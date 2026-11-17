package com.example.ui.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.utils.Conflict
import com.example.utils.ConflictResolver
import com.example.utils.FullSyncManager
import com.example.utils.OfflineQueueManager
import com.example.utils.Resolution
import kotlinx.coroutines.launch

internal class SettingsCommsAndSyncUtilsDelegate(private val vm: SettingsViewModel) {

    fun toggleChatParticipant(participantType: SettingsViewModel.ChatParticipantType) {
        val current = vm._blockedChatParticipants.value
        vm._blockedChatParticipants.value = if (participantType in current) {
            current - participantType
        } else {
            current + participantType
        }
        try {
            vm.db.collection("settings").document("chat_participants")
                ?.set(mapOf("blocked" to vm._blockedChatParticipants.value.map { it.name }))
        } catch (_: Exception) {}
    }

    fun isChatBlockedFor(participantType: SettingsViewModel.ChatParticipantType): Boolean {
        return participantType in vm._blockedChatParticipants.value ||
            SettingsViewModel.ChatParticipantType.ALL in vm._blockedChatParticipants.value
    }

    fun canParticipateInChat(participantType: SettingsViewModel.ChatParticipantType): Boolean {
        return !isChatBlockedFor(participantType)
    }

    fun startVoiceCall(name: String, role: String) {
        vm._activeVoiceCall.value = Pair(name, role)
    }

    fun endVoiceCall() {
        vm._activeVoiceCall.value = null
    }

    fun triggerManualSync(context: Context, onComplete: ((Boolean) -> Unit)? = null) {
        vm.viewModelScope.launch {
            try {
                val syncMgr = FullSyncManager(context)
                val success = syncMgr.syncAllSettings()
                if (success) {
                    vm.triggerToast("🔄 تم استكمال المزامنة بنجاح")
                } else {
                    vm.triggerToast("⚠️ تعذرت المزامنة المباشرة مع السحابة")
                }
                onComplete?.invoke(success)
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Error in triggerManualSync", e)
                onComplete?.invoke(false)
            }
        }
    }

    fun resolveConflict(context: Context, conflict: Conflict, resolution: Resolution) {
        try {
            ConflictResolver(context).resolveConflict(conflict, resolution)
            vm.triggerToast("✅ تم حل التعارض بنجاح")
        } catch (e: Exception) {
            Log.e("SettingsViewModel", "Error resolving conflict", e)
        }
    }

    fun retryOfflineQueue(context: Context) {
        try {
            OfflineQueueManager(context).retryFailedRequests()
            vm.triggerToast("🚀 جاري إعادة إرسال العمليات المتبقية...")
        } catch (e: Exception) {
            Log.e("SettingsViewModel", "Error retrying offline queue", e)
        }
    }
}
