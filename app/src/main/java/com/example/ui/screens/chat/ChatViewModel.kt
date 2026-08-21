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
import java.util.UUID

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

    private val _replyingTo = MutableStateFlow<ChatMessageEntity?>(null)
    val replyingTo: StateFlow<ChatMessageEntity?> = _replyingTo.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive: StateFlow<Boolean> = _isSearchActive.asStateFlow()

    private val _isChannelBlocked = MutableStateFlow(false)
    val isChannelBlocked: StateFlow<Boolean> = _isChannelBlocked.asStateFlow()

    private var messagesJob: Job? = null
    private var typingJob: Job? = null
    private var typingTimeoutJob: Job? = null
    private var currentChannelId: String = ""

    /**
     * Start listening to messages and typing status for a specific channel
     */
    fun startListeningToChannel(channelId: String, currentUserId: String = "", otherUserId: String = "") {
        currentChannelId = channelId
        messagesJob?.cancel()
        typingJob?.cancel()
        _isLoading.value = true

        messagesJob = viewModelScope.launch {
            chatRepository.listenToMessages(channelId).collect { msgs ->
                _messages.value = msgs
                _isLoading.value = false
                // Auto mark incoming messages as read
                if (currentUserId.isNotBlank()) {
                    msgs.filter { it.recipientId == currentUserId && it.status != "READ" }.forEach { unread ->
                        chatRepository.markMessageAsRead(channelId, unread.id)
                    }
                }
            }
        }

        if (otherUserId.isNotBlank()) {
            typingJob = viewModelScope.launch {
                chatRepository.listenToTypingStatus(channelId, otherUserId).collect { typing ->
                    _otherUserTyping.value = typing
                }
            }
        }
    }

    /**
     * Stop listening to the active channel
     */
    fun stopListening() {
        if (currentChannelId.isNotBlank()) {
            setTypingState(currentChannelId, "", false)
        }
        messagesJob?.cancel()
        typingJob?.cancel()
        messagesJob = null
        typingJob = null
    }

    fun setReplyingTo(message: ChatMessageEntity?) {
        _replyingTo.value = message
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleSearchActive(active: Boolean) {
        _isSearchActive.value = active
        if (!active) _searchQuery.value = ""
    }

    /**
     * Update text input field state and trigger typing indicator timeout
     */
    fun updateInputText(text: String, channelId: String, currentUserId: String) {
        _inputText.value = text
        
        if (text.isNotBlank() && !_isTyping.value) {
            setTypingState(channelId, currentUserId, true)
        }

        typingTimeoutJob?.cancel()
        typingTimeoutJob = viewModelScope.launch {
            delay(3000)
            setTypingState(channelId, currentUserId, false)
        }
    }

    private fun setTypingState(channelId: String, userId: String, isTyping: Boolean) {
        _isTyping.value = isTyping
        if (channelId.isNotBlank() && userId.isNotBlank()) {
            chatRepository.setTypingStatus(channelId, userId, isTyping)
        }
    }

    /**
     * Send message inside the active channel
     */
    fun sendMessage(
        channelId: String,
        senderId: String,
        senderName: String,
        recipientId: String,
        msgText: String,
        mediaType: String = "TEXT",
        mediaUrl: String = "",
        fileName: String = "",
        fileSize: Long = 0L,
        forwardedFrom: String = ""
    ) {
        if (msgText.isBlank() && mediaUrl.isBlank() && fileName.isBlank()) return

        val reply = _replyingTo.value
        val msg = ChatMessageEntity(
            id = UUID.randomUUID().toString(),
            senderId = senderId,
            senderName = senderName,
            recipientId = recipientId,
            message = msgText,
            timestamp = System.currentTimeMillis(),
            mediaType = mediaType,
            mediaUrl = mediaUrl,
            imageUrl = if (mediaType == "IMAGE") mediaUrl else "",
            replyToId = reply?.id ?: "",
            replyToText = reply?.message ?: "",
            replyToSender = reply?.senderName ?: "",
            fileName = fileName,
            fileSize = fileSize,
            forwardedFrom = forwardedFrom,
            status = "SENT"
        )

        chatRepository.sendMessage(channelId, msg) { success, _ ->
            if (success) {
                _inputText.value = ""
                _replyingTo.value = null
                setTypingState(channelId, senderId, false)
            }
        }
    }

    fun deleteMessage(channelId: String, messageId: String, deletedBy: String, deleteForEveryone: Boolean = true) {
        chatRepository.deleteMessage(channelId, messageId, deletedBy, deleteForEveryone)
    }

    fun toggleReaction(channelId: String, messageId: String, emoji: String, currentReactions: String) {
        chatRepository.toggleReaction(channelId, messageId, emoji, currentReactions)
    }

    fun toggleBlockChannel(channelId: String, isBlocked: Boolean) {
        chatRepository.setChannelBlocked(channelId, isBlocked) { success ->
            if (success) _isChannelBlocked.value = isBlocked
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopListening()
    }
}
