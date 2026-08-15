package com.example.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ChatChannelEntity
import com.example.data.ChatMessageEntity
import com.example.data.repositories.ChatRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatViewModel(
    private val chatRepository: ChatRepository = ChatRepository()
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessageEntity>>(emptyList())
    val messages: StateFlow<List<ChatMessageEntity>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping.asStateFlow()

    private val _otherUserTyping = MutableStateFlow(false)
    val otherUserTyping: StateFlow<Boolean> = _otherUserTyping.asStateFlow()

    private var messagesJob: Job? = null
    private var typingTimeoutJob: Job? = null

    /**
     * Start listening to messages for a specific channel
     */
    fun startListeningToChannel(channelId: String) {
        messagesJob?.cancel()
        _isLoading.value = true
        messagesJob = viewModelScope.launch {
            chatRepository.listenToMessages(channelId).collect { msgs ->
                _messages.value = msgs
                _isLoading.value = false
            }
        }
    }

    /**
     * Stop listening to the active channel
     */
    fun stopListening() {
        messagesJob?.cancel()
        messagesJob = null
    }

    /**
     * Update text input field state and trigger typing indicator timeout
     */
    fun updateInputText(text: String, channelId: String, currentUserId: String) {
        _inputText.value = text
        
        if (text.isNotBlank() && !_isTyping.value) {
            setTypingState(channelId, currentUserId, true)
        }

        // Debounce/Timeout for typing indicator (disappear after 3 seconds of inactivity)
        typingTimeoutJob?.cancel()
        typingTimeoutJob = viewModelScope.launch {
            delay(3000)
            setTypingState(channelId, currentUserId, false)
        }
    }

    /**
     * Update typing status in Firestore / Local state
     */
    private fun setTypingState(channelId: String, userId: String, isTyping: Boolean) {
        _isTyping.value = isTyping
        // This can be synced to Firestore collection "chat_typing" if desired
    }

    /**
     * Send message inside the active channel
     */
    fun sendMessage(channelId: String, senderId: String, senderName: String, recipientId: String, msgText: String) {
        if (msgText.isBlank()) return

        val msg = ChatMessageEntity(
            id = java.util.UUID.randomUUID().toString(),
            senderId = senderId,
            senderName = senderName,
            recipientId = recipientId,
            message = msgText,
            timestamp = System.currentTimeMillis(),
            mediaType = "TEXT"
        )

        chatRepository.sendMessage(channelId, msg) { success, _ ->
            if (success) {
                _inputText.value = ""
                _isTyping.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopListening()
    }
}
