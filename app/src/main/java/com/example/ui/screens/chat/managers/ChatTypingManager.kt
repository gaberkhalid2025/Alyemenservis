package com.example.ui.screens.chat.managers

import com.example.data.repositories.ChatRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatTypingManager(
    private val repository: ChatRepository,
    private val scope: CoroutineScope
) {
    private val _isTypingOther = MutableStateFlow(false)
    val isTypingOther: StateFlow<Boolean> = _isTypingOther.asStateFlow()

    private var typingJob: Job? = null

    fun setOtherUserTyping(isTyping: Boolean) {
        _isTypingOther.value = isTyping
    }

    fun onUserTyping(channelId: String, senderId: String, text: String) {
        sendTypingStatus(channelId, senderId, true)
        typingJob?.cancel()
        typingJob = scope.launch {
            delay(3000)
            sendTypingStatus(channelId, senderId, false)
        }
    }

    private fun sendTypingStatus(channelId: String, senderId: String, isTyping: Boolean) {
        scope.launch {
            try {
                repository.setTyping(channelId, senderId, isTyping)
            } catch (_: Exception) {}
        }
    }

    fun clear() {
        typingJob?.cancel()
        _isTypingOther.value = false
    }
}
