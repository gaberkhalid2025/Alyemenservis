package com.example.util

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class Conflict(
    val entityId: String,
    val entityType: String, // "ADMIN_SETTINGS", "PROVIDER", "BOOKING", "FORM_CONFIG"
    val localVersion: Int,
    val cloudVersion: Int,
    val localData: Map<String, Any?>,
    val cloudData: Map<String, Any?>,
    val timestamp: Long = System.currentTimeMillis()
)

enum class Resolution {
    USE_CLOUD,
    USE_LOCAL,
    MERGE
}

data class ConflictAuditEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val entityId: String,
    val entityType: String,
    val resolution: Resolution,
    val resolvedAt: Long = System.currentTimeMillis(),
    val notes: String = ""
)

/**
 * ⚖️ ConflictResolver
 * كشف وإدارة وحل التعارضات الناتجة عن التعديل المتزامن محلياً وسحابياً
 */
class ConflictResolver(private val context: Context) {

    private val _pendingConflicts = MutableStateFlow<List<Conflict>>(emptyList())
    val pendingConflicts: StateFlow<List<Conflict>> = _pendingConflicts.asStateFlow()

    private val _auditLogs = MutableStateFlow<List<ConflictAuditEntry>>(emptyList())
    val auditLogs: StateFlow<List<ConflictAuditEntry>> = _auditLogs.asStateFlow()

    companion object {
        private const val TAG = "ConflictResolver"
    }

    /**
     * كشف التعارضات بين النسخة المحلية والسحابية بناء على الإصدار أو التوقيت
     */
    fun detectConflicts(
        entityId: String,
        entityType: String,
        localData: Map<String, Any?>,
        cloudData: Map<String, Any?>,
        localVer: Int = 1,
        cloudVer: Int = 1
    ): Conflict? {
        val hasDifference = localData.entries.any { (k, v) ->
            cloudData.containsKey(k) && cloudData[k] != v
        }

        if (hasDifference && localVer != cloudVer) {
            val conflict = Conflict(
                entityId = entityId,
                entityType = entityType,
                localVersion = localVer,
                cloudVersion = cloudVer,
                localData = localData,
                cloudData = cloudData
            )
            _pendingConflicts.value = _pendingConflicts.value + conflict
            return conflict
        }
        return null
    }

    fun getPendingConflicts(): List<Conflict> = _pendingConflicts.value

    /**
     * حل تعارض معين وتطبيق القرار
     */
    fun resolveConflict(
        conflict: Conflict,
        resolution: Resolution,
        onResolved: ((Map<String, Any?>) -> Unit)? = null
    ): Map<String, Any?> {
        val resultData = when (resolution) {
            Resolution.USE_CLOUD -> conflict.cloudData
            Resolution.USE_LOCAL -> conflict.localData
            Resolution.MERGE -> mergeChanges(conflict)
        }

        // إزالة التعارض من القائمة المعلقة
        _pendingConflicts.value = _pendingConflicts.value.filterNot { it.entityId == conflict.entityId }

        // تسجيل في سجل التدقيق
        _auditLogs.value = _auditLogs.value + ConflictAuditEntry(
            entityId = conflict.entityId,
            entityType = conflict.entityType,
            resolution = resolution,
            notes = "Resolved with $resolution successfully"
        )

        Log.d(TAG, "Conflict resolved for ${conflict.entityId} using $resolution")
        onResolved?.invoke(resultData)
        return resultData
    }

    /**
     * دمج التغييرات المحلية والسحابية (دمج الحقول المحدثة)
     */
    fun mergeChanges(conflict: Conflict): Map<String, Any?> {
        val merged = HashMap<String, Any?>()
        merged.putAll(conflict.cloudData)
        // دمج الحقول المحلية غير الفارغة أو الأحدث
        conflict.localData.forEach { (key, localVal) ->
            if (localVal != null) {
                if (localVal is String && localVal.isNotBlank()) {
                    merged[key] = localVal
                } else if (localVal !is String) {
                    merged[key] = localVal
                }
            }
        }
        merged["lastMergedAt"] = System.currentTimeMillis()
        return merged
    }

    /**
     * حل جميع التعارضات باستخدام السحابة
     */
    fun resolveAllWithCloud(onResolved: (List<Pair<Conflict, Map<String, Any?>>>) -> Unit) {
        val list = _pendingConflicts.value.map { c ->
            Pair(c, resolveConflict(c, Resolution.USE_CLOUD))
        }
        onResolved(list)
    }

    /**
     * حل جميع التعارضات باستخدام المحلي
     */
    fun resolveAllWithLocal(onResolved: (List<Pair<Conflict, Map<String, Any?>>>) -> Unit) {
        val list = _pendingConflicts.value.map { c ->
            Pair(c, resolveConflict(c, Resolution.USE_LOCAL))
        }
        onResolved(list)
    }
}
