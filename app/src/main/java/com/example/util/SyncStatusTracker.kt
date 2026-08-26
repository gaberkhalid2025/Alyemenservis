package com.example.util

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * 🔄 SyncStatus - حالات المزامنة
 */
enum class SyncStatus {
    SYNCED,        // متزامن بالكامل
    SYNCING,       // جاري المزامنة
    CONFLICT,      // تعارض
    NOT_SYNCED,    // غير متزامن
    ERROR          // خطأ
}

/**
 * ❌ SyncError - كائن خطأ المزامنة
 */
data class SyncError(
    val id: String = UUID.randomUUID().toString(),
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val module: String = "GENERAL",
    val exception: Throwable? = null
)

/**
 * 📊 SyncStatusTracker
 * 
 * تتبع وإدارة حالة المزامنة وعرض الإحصائيات والأخطاء والتقدم في واجهة المستخدم لحظياً.
 * يوفر StateFlow لكافة تفاصيل التقدم ومؤشرات الخطأ وحالات المزامنة مع كليبرات مستقرة.
 */
class SyncStatusTracker(private val context: Context) {

    private val _syncStatus = MutableStateFlow(SyncStatus.NOT_SYNCED)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val _lastSyncTime = MutableStateFlow(0L)
    val lastSyncTime: StateFlow<Long> = _lastSyncTime.asStateFlow()

    private val _syncedItemsCount = MutableStateFlow(0)
    val syncedItemsCount: StateFlow<Int> = _syncedItemsCount.asStateFlow()

    private val _pendingItemsCount = MutableStateFlow(0)
    val pendingItemsCount: StateFlow<Int> = _pendingItemsCount.asStateFlow()

    private val _syncProgress = MutableStateFlow(0f)
    val syncProgress: StateFlow<Float> = _syncProgress.asStateFlow()

    private val _errors = MutableStateFlow<List<SyncError>>(emptyList())
    val errors: StateFlow<List<SyncError>> = _errors.asStateFlow()

    /**
     * الحصول على حالة المزامنة الحالية
     */
    fun getSyncStatus(): SyncStatus = _syncStatus.value

    /**
     * الحصول على توقيت آخر مزامنة
     */
    fun getLastSyncTime(): Long = _lastSyncTime.value

    /**
     * الحصول على عدد العناصر المتزامنة
     */
    fun getSyncedItemsCount(): Int = _syncedItemsCount.value

    /**
     * الحصول على عدد العناصر المعلقة
     */
    fun getPendingItemsCount(): Int = _pendingItemsCount.value

    /**
     * الحصول على نسبة تقدم المزامنة (من 0 إلى 1)
     */
    fun getSyncProgress(): Float = _syncProgress.value

    /**
     * الحصول على قائمة أخطاء المزامنة
     */
    fun getErrors(): List<SyncError> = _errors.value

    /**
     * تحديث حالة المزامنة
     */
    fun updateStatus(status: SyncStatus) {
        _syncStatus.value = status
    }

    /**
     * تحديث التقدم وعدد العناصر
     */
    fun updateProgress(progress: Float, syncedCount: Int, pendingCount: Int) {
        _syncProgress.value = progress.coerceIn(0f, 1f)
        _syncedItemsCount.value = syncedCount
        _pendingItemsCount.value = pendingCount
    }

    /**
     * تسجيل نجاح عملية مزامنة
     */
    fun recordSuccessfulSync(syncedCount: Int) {
        _syncStatus.value = SyncStatus.SYNCED
        _lastSyncTime.value = System.currentTimeMillis()
        _syncedItemsCount.value = syncedCount
        _pendingItemsCount.value = 0
        _syncProgress.value = 1f
    }

    /**
     * تسجيل خطأ في عملية المزامنة
     */
    fun recordError(message: String, module: String = "SYNC", throwable: Throwable? = null) {
        _syncStatus.value = SyncStatus.ERROR
        val error = SyncError(message = message, module = module, exception = throwable)
        _errors.value = _errors.value + error
    }

    /**
     * مسح قائمة الأخطاء
     */
    fun clearErrors() {
        _errors.value = emptyList()
        if (_syncStatus.value == SyncStatus.ERROR) {
            _syncStatus.value = SyncStatus.NOT_SYNCED
        }
    }

    /**
     * إعادة محاولة المزامنة الفاشلة
     */
    fun retryFailedSync(action: () -> Unit) {
        clearErrors()
        _syncStatus.value = SyncStatus.SYNCING
        action()
    }

    /**
     * بدء مزامنة يدوية
     */
    fun manualSync(action: () -> Unit) {
        _syncStatus.value = SyncStatus.SYNCING
        _syncProgress.value = 0.1f
        action()
    }
}
