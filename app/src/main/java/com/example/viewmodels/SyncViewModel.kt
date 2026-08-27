package com.example.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.util.Conflict
import com.example.util.ConflictResolver
import com.example.util.OfflineQueueManager
import com.example.util.Resolution
import com.example.util.SyncError
import com.example.util.SyncManager
import com.example.util.SyncStatus
import com.example.util.SyncStatusTracker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 🔄 SyncViewModel
 * إدارة واجهات وتفاعل عمليات المزامنة المركزية وحل التعارضات وإدارة طابور الأوفلاين.
 */
class SyncViewModel(application: Application) : AndroidViewModel(application) {

    val syncManager = SyncManager(application)
    val statusTracker = SyncStatusTracker(application)
    val conflictResolver = ConflictResolver(application)
    val offlineQueue = OfflineQueueManager(application)

    val syncStatus: StateFlow<SyncStatus> = statusTracker.syncStatus
    val syncProgress: StateFlow<Float> = statusTracker.syncProgress
    val pendingConflicts: StateFlow<List<Conflict>> = conflictResolver.pendingConflicts
    val errors: StateFlow<List<SyncError>> = statusTracker.errors

    private val _lastSyncFormatted = MutableStateFlow(syncManager.getLastSyncTime())
    val lastSyncFormatted: StateFlow<String> = _lastSyncFormatted.asStateFlow()

    fun triggerManualSync(onComplete: ((Boolean) -> Unit)? = null) {
        statusTracker.manualSync {
            viewModelScope.launch {
                val success = syncManager.syncAllSettings()
                if (success) {
                    statusTracker.recordSuccessfulSync(10)
                    _lastSyncFormatted.value = syncManager.getLastSyncTime()
                } else {
                    statusTracker.recordError("فشلت عملية المزامنة مع السحابة")
                }
                onComplete?.invoke(success)
            }
        }
    }

    fun resolveConflict(conflict: Conflict, resolution: Resolution) {
        conflictResolver.resolveConflict(conflict, resolution)
    }

    fun retryOfflineQueue() {
        offlineQueue.retryFailedRequests()
    }
}
