package com.example.util

import android.content.Context
import android.util.Log
import com.example.data.AdminSettingsEntity
import com.example.data.local.SyncDataDatabase
import com.example.data.local.SyncDataEntity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 🔄 SyncManager
 * 
 * المركز الرئيسي لإدارة المزامنة الشاملة والجزئية وحفظ واسترجاع إعدادات الأدمن، الثيمات،
 * واستمارات التسجيل وحقولها من وإلى Firebase Firestore.
 * 
 * يعتمد على **Room Database (`SyncDataDatabase`)** لتخزين حالات وبصمات المزامنة.
 */
class SyncManager(private val context: Context) {

    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val syncDb by lazy { SyncDataDatabase.getInstance(context) }
    private val dao by lazy { syncDb.syncDataDao() }
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _lastSyncTimestamp = MutableStateFlow(0L)
    val lastSyncTimestamp: StateFlow<Long> = _lastSyncTimestamp.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private var autoSyncJob: Job? = null

    companion object {
        private const val TAG = "SyncManager"
        private const val KEY_LAST_SYNC_TS = "key_last_sync_timestamp"
        private const val COLLECTION_ADMIN_SETTINGS = "admin_settings"
        private const val DOC_MAIN_CONFIG = "main_config"
        private const val DOC_FORMS_CONFIG = "forms_config"
        private const val DOC_THEME_CONFIG = "theme_config"
        private const val SYNC_INTERVAL_MS = 5 * 60 * 1000L // 5 minutes
    }

    init {
        loadLastSyncTimestamp()
        startAutoSync()
    }

    private fun loadLastSyncTimestamp() {
        scope.launch {
            try {
                val entity = dao.get(KEY_LAST_SYNC_TS)
                if (entity != null) {
                    _lastSyncTimestamp.value = entity.timestamp
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading sync timestamp from Room: ${e.message}")
            }
        }
    }

    fun startAutoSync() {
        autoSyncJob?.cancel()
        autoSyncJob = scope.launch {
            while (isActive) {
                if (isSyncRequired()) {
                    syncAllSettings()
                }
                delay(SYNC_INTERVAL_MS)
            }
        }
    }

    fun stopAutoSync() {
        autoSyncJob?.cancel()
        autoSyncJob = null
    }

    suspend fun syncAllSettings(): Boolean = withContext(Dispatchers.IO) {
        _isSyncing.value = true
        try {
            val adminSync = syncAdminSettingsInternal()
            val themeSync = syncThemeSettingsInternal()
            val formSync = syncFormSettingsInternal()

            val success = adminSync && themeSync && formSync
            if (success) {
                val now = System.currentTimeMillis()
                dao.insert(SyncDataEntity(KEY_LAST_SYNC_TS, now.toString(), now))
                _lastSyncTimestamp.value = now
                Log.d(TAG, "All settings synchronized successfully to Firestore & Room.")
            }
            _isSyncing.value = false
            success
        } catch (e: Exception) {
            Log.e(TAG, "Error in syncAllSettings: ${e.message}", e)
            _isSyncing.value = false
            false
        }
    }

    suspend fun syncPartialData(key: String, payloadMap: Map<String, Any?>): Boolean = withContext(Dispatchers.IO) {
        try {
            val jsonObj = JSONObject(payloadMap)
            val now = System.currentTimeMillis()
            dao.insert(SyncDataEntity(key, jsonObj.toString(), now))

            firestore.collection("sync_partial")
                .document(key)
                .set(payloadMap, SetOptions.merge())
                .await()

            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed partial sync for key $key: ${e.message}")
            false
        }
    }

    suspend fun syncAdminSettings(settings: AdminSettingsEntity? = null): Boolean = withContext(Dispatchers.IO) {
        try {
            syncAdminSettingsInternal(settings)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing admin settings: ${e.message}", e)
            false
        }
    }

    private suspend fun syncAdminSettingsInternal(settings: AdminSettingsEntity? = null): Boolean = suspendCancellableCoroutine { continuation ->
        val dataToSync = HashMap<String, Any>()
        if (settings != null) {
            dataToSync["appName"] = settings.appName
            dataToSync["welcomeMessage"] = settings.welcomeMessage
            dataToSync["footerMessage"] = settings.footerMessage
            dataToSync["footerBgColorHex"] = settings.footerBgColorHex
            dataToSync["footerItemsOrder"] = settings.footerItemsOrder
            dataToSync["showInfoIcon"] = settings.showInfoIcon
            dataToSync["showBookingsIcon"] = settings.showBookingsIcon
            dataToSync["showLangIcon"] = settings.showLangIcon
            dataToSync["showAdminIcon"] = settings.showAdminIcon
            dataToSync["showFooterText"] = settings.showFooterText
            dataToSync["activeThemeId"] = settings.activeThemeId
            dataToSync["customPrimaryHex"] = settings.customPrimaryHex
            dataToSync["customSecondaryHex"] = settings.customSecondaryHex
            dataToSync["customBackgroundHex"] = settings.customBackgroundHex
            dataToSync["customSurfaceHex"] = settings.customSurfaceHex
            dataToSync["isMaintenanceActive"] = settings.isMaintenanceActive
            dataToSync["supportPhone"] = settings.supportPhone
            dataToSync["supportWhatsapp"] = settings.supportWhatsapp
            dataToSync["supportEmail"] = settings.supportEmail
            dataToSync["adminUsername"] = settings.adminUsername
            dataToSync["adminPassword"] = settings.adminPassword
            dataToSync["allowVoiceInput"] = settings.allowVoiceInput
            dataToSync["allowTextToSpeech"] = settings.allowTextToSpeech
            dataToSync["isAssistantEnabled"] = settings.isAssistantEnabled
            dataToSync["isMapFeatureEnabled"] = settings.isMapFeatureEnabled
            dataToSync["registrationRequirements"] = settings.registrationRequirements
            dataToSync["updatedAt"] = System.currentTimeMillis()
        } else {
            dataToSync["updatedAt"] = System.currentTimeMillis()
            dataToSync["status"] = "PING"
        }

        firestore.collection(COLLECTION_ADMIN_SETTINGS)
            .document(DOC_MAIN_CONFIG)
            .set(dataToSync, SetOptions.merge())
            .addOnSuccessListener {
                if (continuation.isActive) continuation.resume(true) {}
            }
            .addOnFailureListener { err ->
                Log.w(TAG, "Failed admin settings sync: ${err.message}")
                if (continuation.isActive) continuation.resume(false) {}
            }
    }

    suspend fun syncThemeSettings(themeId: String? = null, primaryHex: String? = null, secondaryHex: String? = null): Boolean = withContext(Dispatchers.IO) {
        try {
            syncThemeSettingsInternal(themeId, primaryHex, secondaryHex)
        } catch (e: Exception) {
            Log.e(TAG, "Error in syncThemeSettings: ${e.message}", e)
            false
        }
    }

    private suspend fun syncThemeSettingsInternal(themeId: String? = null, primaryHex: String? = null, secondaryHex: String? = null): Boolean = suspendCancellableCoroutine { continuation ->
        val themeMap = hashMapOf<String, Any>(
            "activeThemeId" to (themeId ?: "EMERALD_YEMEN"),
            "customPrimaryHex" to (primaryHex ?: "#059669"),
            "customSecondaryHex" to (secondaryHex ?: "#115E59"),
            "updatedAt" to System.currentTimeMillis()
        )

        firestore.collection(COLLECTION_ADMIN_SETTINGS)
            .document(DOC_THEME_CONFIG)
            .set(themeMap, SetOptions.merge())
            .addOnSuccessListener {
                if (continuation.isActive) continuation.resume(true) {}
            }
            .addOnFailureListener {
                if (continuation.isActive) continuation.resume(false) {}
            }
    }

    suspend fun syncFormSettings(formsData: Map<String, Any>? = null): Boolean = withContext(Dispatchers.IO) {
        try {
            syncFormSettingsInternal(formsData)
        } catch (e: Exception) {
            Log.e(TAG, "Error in syncFormSettings: ${e.message}", e)
            false
        }
    }

    private suspend fun syncFormSettingsInternal(formsData: Map<String, Any>? = null): Boolean = suspendCancellableCoroutine { continuation ->
        val data = formsData ?: mapOf(
            "services_form" to "الاسم الثلاثي|Mandatory,رقم الهاتف|Mandatory,القسم|Mandatory,المدينة|Mandatory,الحي|Optional",
            "restaurants_form" to "اسم المطعم|Mandatory,رقم الهاتف|Mandatory,النوع|Mandatory,المدينة|Mandatory",
            "stores_form" to "اسم المتجر|Mandatory,رقم الهاتف|Mandatory,النشاط|Mandatory,المدينة|Mandatory",
            "medical_form" to "اسم المركز الطبي|Mandatory,رقم الهاتف|Mandatory,التخصص|Mandatory,المدينة|Mandatory",
            "updatedAt" to System.currentTimeMillis()
        )

        firestore.collection(COLLECTION_ADMIN_SETTINGS)
            .document(DOC_FORMS_CONFIG)
            .set(data, SetOptions.merge())
            .addOnSuccessListener {
                if (continuation.isActive) continuation.resume(true) {}
            }
            .addOnFailureListener {
                if (continuation.isActive) continuation.resume(false) {}
            }
    }

    suspend fun restoreAllSettings(): Map<String, Any?>? = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            firestore.collection(COLLECTION_ADMIN_SETTINGS)
                .document(DOC_MAIN_CONFIG)
                .get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot.exists()) {
                        continuation.resume(snapshot.data) {}
                    } else {
                        continuation.resume(null) {}
                    }
                }
                .addOnFailureListener { err ->
                    Log.e(TAG, "Failed to restore settings: ${err.message}")
                    continuation.resume(null) {}
                }
        }
    }

    fun getLastSyncTime(): String {
        val ts = _lastSyncTimestamp.value
        if (ts == 0L) return "لم تتم المزامنة بعد"
        val sdf = SimpleDateFormat("yyyy/MM/dd - hh:mm a", Locale("ar"))
        return sdf.format(Date(ts))
    }

    fun isSyncRequired(): Boolean {
        val lastTs = _lastSyncTimestamp.value
        return (System.currentTimeMillis() - lastTs) >= SYNC_INTERVAL_MS
    }

    fun forceSync(onComplete: (Boolean) -> Unit = {}) {
        scope.launch {
            val res = syncAllSettings()
            withContext(Dispatchers.Main) {
                onComplete(res)
            }
        }
    }

    fun clearLocalCache() {
        scope.launch {
            dao.clearAll()
            _lastSyncTimestamp.value = 0L
        }
    }
}
