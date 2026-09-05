package com.example.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

open class BaseViewModel : ViewModel() {

    open var appContext: android.content.Context? = null

    open val db: FirebaseFirestore by lazy {
        val firestore = FirebaseFirestore.getInstance()
        try {
            val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .setCacheSizeBytes(104857600L) // 100 MB cache size for ultra-fast local offline caching
                .build()
            firestore.firestoreSettings = settings
        } catch (e: Exception) {
            e.printStackTrace()
        }
        firestore
    }

    open val firestoreListeners = mutableListOf<ListenerRegistration>()

    override fun onCleared() {
        super.onCleared()
        try {
            firestoreListeners.forEach { it.remove() }
            firestoreListeners.clear()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    
    open fun com.google.firebase.firestore.Query.addSnapshotListenerReg(listener: (com.google.firebase.firestore.QuerySnapshot?, com.google.firebase.firestore.FirebaseFirestoreException?) -> Unit) {
        reg(this.addSnapshotListener(listener))
    }
    open fun com.google.firebase.firestore.DocumentReference.addSnapshotListenerReg(listener: (com.google.firebase.firestore.DocumentSnapshot?, com.google.firebase.firestore.FirebaseFirestoreException?) -> Unit) {
        reg(this.addSnapshotListener(listener))
    }

    open fun reg(listener: ListenerRegistration) {
        firestoreListeners.add(listener)
    }

    open val _toastMessage = MutableStateFlow<String?>(null)
    open val toastFlow: StateFlow<String?> = _toastMessage.asStateFlow()

    open fun triggerToast(msg: String) {
        _toastMessage.value = msg
    }

    open fun triggerNotification(msg: String) {
        _toastMessage.value = msg
    }

    open fun clearToast() {
        _toastMessage.value = null
    }

    // دالة آمنة لعمليات Firestore مع معالجة الأخطاء
    protected open suspend fun <T> safeFirestoreCall(
        operation: suspend () -> T,
        onSuccess: (T) -> Unit = {},
        onError: (Exception) -> Unit = {},
        errorMessage: String = "حدث خطأ أثناء تنفيذ العملية"
    ) {
        try {
            val result = operation()
            onSuccess(result)
        } catch (e: Exception) {
            e.printStackTrace()
            com.example.utils.AppErrorLogManager.logFirestoreError("FirestoreCall", errorMessage, e)
            triggerNotification("❌ $errorMessage: ${e.localizedMessage}")
            onError(e)
        }
    }

    // دالة آمنة لعمليات Firestore مع callback
    protected open fun safeFirestoreCallWithCallback(
        operation: (onSuccess: () -> Unit, onFailure: (Exception) -> Unit) -> Unit,
        onSuccess: () -> Unit = {},
        onError: (Exception) -> Unit = {},
        errorMessage: String = "حدث خطأ أثناء تنفيذ العملية"
    ) {
        try {
            operation(
                {
                    onSuccess()
                },
                { exception ->
                    com.example.utils.AppErrorLogManager.logFirestoreError("FirestoreCallback", errorMessage, exception)
                    triggerNotification("❌ $errorMessage: ${exception.localizedMessage}")
                    onError(exception)
                }
            )
        } catch (e: Exception) {
            e.printStackTrace()
            com.example.utils.AppErrorLogManager.logFirestoreError("FirestoreException", errorMessage, e)
            triggerNotification("❌ $errorMessage: ${e.localizedMessage}")
            onError(e)
        }
    }

    // دالة لتحديث الحالة المحلية بأمان
    protected open fun <T> safeUpdateState(
        currentState: List<T>,
        newItem: T,
        filterCondition: (T) -> Boolean = { it == newItem }
    ): List<T> {
        return currentState.filter { !filterCondition(it) } + newItem
    }

    // دالة لعرض رسائل المزامنة
    protected open fun showSyncMessage(success: Boolean, action: String) {
        if (success) {
            triggerNotification("✅ تم $action بنجاح")
        } else {
            triggerNotification("⚠️ تم حفظ $action محلياً وسيتم المزامنة تلقائياً")
        }
    }

    open val _isRefreshing = MutableStateFlow(false)
    open val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    open fun setRefreshing(refreshing: Boolean) {
        _isRefreshing.value = refreshing
    }

    open val _uiErrorMessage = MutableStateFlow<String?>(null)
    open val uiErrorMessage: StateFlow<String?> = _uiErrorMessage.asStateFlow()

    open fun setUiError(message: String) {
        _uiErrorMessage.value = message
    }

    open fun clearUiError() {
        _uiErrorMessage.value = null
    }

    open suspend fun uploadImageStringOrUri(
        context: android.content.Context,
        input: String,
        storagePath: String,
        maxSizeBytes: Long = 300 * 1024L
    ): String {
        if (input.isBlank()) return ""
        if (input.startsWith("http://") || input.startsWith("https://")) return input
        return try {
            if (input.startsWith("content://") || input.startsWith("file://")) {
                val uri = android.net.Uri.parse(input)
                val res = com.example.utils.FirebaseStorageUploader.uploadImageUri(
                    context, uri, storagePath, maxDimension = 800, maxSizeBytes = maxSizeBytes
                )
                res.getOrDefault(input)
            } else {
                val cleanBase64 = if (input.contains(",")) input.substringAfter(",") else input
                val bytes = android.util.Base64.decode(cleanBase64, android.util.Base64.DEFAULT)
                val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (bitmap != null) {
                    val res = com.example.utils.FirebaseStorageUploader.uploadBitmap(
                        bitmap, storagePath, maxDimension = 800, maxSizeBytes = maxSizeBytes
                    )
                    res.getOrDefault(input)
                } else {
                    input
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            input
        }
    }

    open fun getAuthEmailForPhone(phone: String): String {
        val cleanPhone = phone.replace("+", "").replace(" ", "")
        return "user_$cleanPhone@yemen-services.app"
    }

    open fun getDefaultStoresList(): List<com.example.data.StoreEntity> = emptyList()
    open fun getDefaultPropertiesList(): List<com.example.data.PropertyEntity> = emptyList()
}
