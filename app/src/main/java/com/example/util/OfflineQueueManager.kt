package com.example.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.data.local.OfflineRequestDatabase
import com.example.data.local.OfflineRequestEntity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.util.UUID

/**
 * 📦 OfflineRequest
 * نموذج طلب الأوفلاين للتعامل في الطبقة العليا.
 */
data class OfflineRequest(
    val id: String = UUID.randomUUID().toString(),
    val type: String, // "BOOKING", "REQUEST", "MESSAGE", "OFFER", "ADMIN_UPDATE"
    val data: Map<String, Any?>,
    val timestamp: Long = System.currentTimeMillis(),
    val priority: Int = 3, // 1 (Highest) to 5 (Lowest)
    val retryCount: Int = 0,
    val status: String = "PENDING" // "PENDING", "PROCESSING", "COMPLETED", "FAILED"
)

/**
 * 📦 OfflineQueueManager
 * 
 * إدارة وتخزين الطلبات في وضع الأوفلاين وإعادة جدولتها ومعالجتها تلقائياً عند استعادة الاتصال بالإنترنت.
 * تم تحديثه ليعتمد بالكامل على **Room Database (`OfflineRequestDatabase`)** بدلاً من `SharedPreferences`
 * لتحسين الأداء واستيعاب كميات كبرى من العمليات المعلقة وسرعة الاستعلام والفرز.
 */
class OfflineQueueManager(private val context: Context) {

    private val db = OfflineRequestDatabase.getInstance(context)
    private val dao = db.offlineRequestDao()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _pendingRequests = MutableStateFlow<List<OfflineRequest>>(emptyList())
    val pendingRequests: StateFlow<List<OfflineRequest>> = _pendingRequests.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    companion object {
        private const val TAG = "OfflineQueueManager"
        private const val MAX_RETRIES = 5
    }

    init {
        observeAndLoadQueue()
    }

    private fun observeAndLoadQueue() {
        scope.launch {
            try {
                dao.getPendingRequests().collect { entities ->
                    val domainList = entities.map { entityToDomain(it) }
                    _pendingRequests.value = domainList
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error observing offline queue from Room: ${e.message}")
            }
        }
    }

    private fun domainToEntity(req: OfflineRequest): OfflineRequestEntity {
        val jsonObj = JSONObject()
        req.data.forEach { (k, v) ->
            jsonObj.put(k, v ?: JSONObject.NULL)
        }
        return OfflineRequestEntity(
            id = req.id,
            type = req.type,
            data = jsonObj.toString(),
            timestamp = req.timestamp,
            priority = req.priority,
            retryCount = req.retryCount,
            status = req.status
        )
    }

    private fun entityToDomain(entity: OfflineRequestEntity): OfflineRequest {
        val dataMap = mutableMapOf<String, Any?>()
        try {
            val jsonObj = JSONObject(entity.data)
            val keys = jsonObj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                if (!jsonObj.isNull(key)) {
                    dataMap[key] = jsonObj.get(key)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing json data for entity ${entity.id}: ${e.message}")
        }
        return OfflineRequest(
            id = entity.id,
            type = entity.type,
            data = dataMap,
            timestamp = entity.timestamp,
            priority = entity.priority,
            retryCount = entity.retryCount,
            status = entity.status
        )
    }

    /**
     * 1. إضافة طلب إلى قائمة الانتظار الحافظة في Room
     */
    fun addToQueue(request: OfflineRequest) {
        scope.launch {
            try {
                val entity = domainToEntity(request)
                dao.insert(entity)
                Log.d(TAG, "Request inserted into Room database: ${request.id} (Type: ${request.type})")

                if (isOnline()) {
                    processQueue()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed inserting request to Room: ${e.message}")
            }
        }
    }

    /**
     * 2. الحصول على الطلبات المعلقة محلياً
     */
    fun getPendingRequests(): List<OfflineRequest> {
        return _pendingRequests.value.filter { it.status == "PENDING" || it.status == "FAILED" }
    }

    /**
     * 3. معالجة قائمة الانتظار وإرسال الطلبات للسيرفر عبر Firestore
     */
    fun processQueue(onItemProcessed: ((OfflineRequest, Boolean) -> Unit)? = null) {
        if (!isOnline() || _isProcessing.value) return

        scope.launch {
            _isProcessing.value = true
            val currentList = dao.getPendingOrFailedRequestsList().map { entityToDomain(it) }

            for (req in currentList) {
                if (!isOnline()) break

                var success = false
                try {
                    success = executeRequest(req)
                } catch (e: Exception) {
                    Log.e(TAG, "Execution error for request ${req.id}: ${e.message}")
                }

                if (success) {
                    dao.updateStatus(req.id, "COMPLETED")
                    dao.deleteById(req.id)
                    onItemProcessed?.invoke(req, true)
                } else {
                    val nextRetry = req.retryCount + 1
                    if (nextRetry < MAX_RETRIES) {
                        dao.updateStatus(req.id, "FAILED", nextRetry)
                    } else {
                        Log.w(TAG, "Request ${req.id} exceeded max retries and will be dropped.")
                        dao.deleteById(req.id)
                    }
                    onItemProcessed?.invoke(req, false)
                }
            }
            _isProcessing.value = false
        }
    }

    private suspend fun executeRequest(req: OfflineRequest): Boolean = withContext(Dispatchers.IO) {
        try {
            val firestore = FirebaseFirestore.getInstance()
            val collection = when (req.type) {
                "BOOKING" -> "bookings"
                "REQUEST" -> "instant_requests"
                "MESSAGE" -> "chat_messages"
                "OFFER" -> "offers"
                else -> "offline_sync_logs"
            }
            val targetDoc = if (req.data.containsKey("id") && req.data["id"] is String) {
                req.data["id"] as String
            } else {
                req.id
            }

            var finished = false
            var isOk = false
            firestore.collection(collection).document(targetDoc).set(req.data, SetOptions.merge())
                .addOnSuccessListener {
                    isOk = true
                    finished = true
                }
                .addOnFailureListener {
                    isOk = false
                    finished = true
                }

            var waitMs = 0
            while (!finished && waitMs < 4000) {
                delay(100)
                waitMs += 100
            }
            isOk
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 4. إلغاء طلب من القائمة في Room
     */
    fun cancelRequest(requestId: String) {
        scope.launch {
            dao.deleteById(requestId)
        }
    }

    /**
     * 5. إعادة محاولة الطلبات الفاشلة
     */
    fun retryFailedRequests() {
        scope.launch {
            val list = dao.getPendingOrFailedRequestsList()
            list.forEach { entity ->
                if (entity.status == "FAILED") {
                    dao.updateStatus(entity.id, "PENDING", 0)
                }
            }
            if (isOnline()) {
                processQueue()
            }
        }
    }

    /**
     * 6. مسح كامل طابور العمليات المعلقة
     */
    fun clearQueue() {
        scope.launch {
            dao.clearAll()
        }
    }

    /**
     * 7. حجم قائمة الانتظار الحالية
     */
    fun getQueueSize(): Int = _pendingRequests.value.size

    /**
     * 8. التحقق من الاتصال بالإنترنت
     */
    fun isOnline(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
