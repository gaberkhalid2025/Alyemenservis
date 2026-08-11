package com.example.ui

import com.example.data.*
import java.util.UUID

fun MainViewModel.openOrCreateChatChannel(targetId: String, targetName: String) {
    _activeChatChannel.value = ChatChannelEntity(
        id = UUID.randomUUID().toString(),
        targetId = targetId,
        targetName = targetName
    )
}

fun MainViewModel.startVoiceCall(callerName: String, callerRole: String) {
    _activeVoiceCallPair.value = Pair(callerName, callerRole)
}

fun MainViewModel.startVoiceCall(targetId: String) {
    _activeVoiceCallPair.value = Pair(targetId, "مستخدم")
}

fun MainViewModel.endVoiceCall() {
    _activeVoiceCallPair.value = null
}
