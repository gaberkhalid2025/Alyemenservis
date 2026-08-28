package com.example.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.models.*
import com.example.data.repositories.ChatRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: ChatRepository = ChatRepository()
) : ViewModel() {

    private val _currentChannel = MutableStateFlow<ChatChannel?>(null)
    val currentChannel: StateFlow<ChatChannel?> = _currentChannel.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _otherUserPresence = MutableStateFlow<UserPresence?>(null)
    val otherUserPresence: StateFlow<UserPresence?> = _otherUserPresence.asStateFlow()

    private val _isTypingOther = MutableStateFlow(false)
    val isTypingOther: StateFlow<Boolean> = _isTypingOther.asStateFlow()

    private val _replyingToMessage = MutableStateFlow<ChatMessage?>(null)
    val replyingToMessage: StateFlow<ChatMessage?> = _replyingToMessage.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private var messagesJob: Job? = null
    private var presenceJob: Job? = null
    private var typingJob: Job? = null

    /**
     * Initialize conversation with channel.
     */
    fun openChannel(channel: ChatChannel, currentUserId: String) {
        _currentChannel.value = channel
        markAsRead(channel.id, currentUserId)
        listenToMessages(channel.id, currentUserId)

        val otherUserId = channel.participants.firstOrNull { it != currentUserId } ?: ""
        if (otherUserId.isNotBlank()) {
            listenToPresence(otherUserId)
        }
    }

    /**
     * Open channel by channel ID or fallback to target user ID.
     */
    fun openChannelById(
        channelId: String,
        currentUserId: String,
        currentUserName: String = "",
        fallbackTargetUserId: String? = null,
        fallbackUserName: String? = null
    ) {
        viewModelScope.launch {
            val channelResult = repository.getChannelById(channelId)
            val channel = channelResult.getOrNull()
            if (channel != null) {
                openChannel(channel, currentUserId)
            } else {
                val targetId = fallbackTargetUserId ?: channelId
                if (targetId.isNotBlank()) {
                    startDirectChat(
                        currentUserId = currentUserId,
                        currentUserName = currentUserName,
                        currentUserPhoto = "",
                        otherUserId = targetId,
                        otherUserName = fallbackUserName ?: "مستخدم",
                        otherUserPhoto = ""
                    )
                }
            }
        }
    }

    /**
     * Initialize or find channel between two users.
     */
    fun startDirectChat(
        currentUserId: String,
        currentUserName: String,
        currentUserPhoto: String,
        otherUserId: String,
        otherUserName: String,
        otherUserPhoto: String,
        relatedEntityId: String? = null,
        relatedEntityType: String? = null
    ) {
        viewModelScope.launch {
            val channelResult = repository.getOrCreateChannel(
                currentUserId = currentUserId,
                currentUserName = currentUserName,
                currentUserPhoto = currentUserPhoto,
                otherUserId = otherUserId,
                otherUserName = otherUserName,
                otherUserPhoto = otherUserPhoto,
                type = ChannelType.PRIVATE,
                relatedEntityId = relatedEntityId,
                relatedEntityType = relatedEntityType
            )
            val channel = channelResult.getOrNull()
            if (channel != null) {
                openChannel(channel, currentUserId)
            }
        }
    }

    private fun listenToMessages(channelId: String, currentUserId: String) {
        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            repository.getChannelMessages(channelId, currentUserId).collect { msgList ->
                _messages.value = msgList
            }
        }
    }

    private fun listenToPresence(otherUserId: String) {
        presenceJob?.cancel()
        presenceJob = viewModelScope.launch {
            repository.getUserPresence(otherUserId).collect { presence ->
                _otherUserPresence.value = presence
            }
        }
    }

    fun sendMessage(
        senderId: String,
        senderName: String,
        text: String,
        mediaType: MediaType = MediaType.TEXT,
        mediaUrl: String = ""
    ) {
        val channel = _currentChannel.value ?: return
        if (text.isBlank() && mediaUrl.isBlank()) return

        val replyTo = _replyingToMessage.value
        _isSending.value = true

        viewModelScope.launch {
            val result = repository.sendMessage(
                channelId = channel.id,
                senderId = senderId,
                senderName = senderName,
                messageText = text.trim(),
                mediaType = mediaType,
                mediaUrl = mediaUrl,
                replyToId = replyTo?.id,
                replyToText = replyTo?.message
            )
            _isSending.value = false
            if (result.isSuccess) {
                _replyingToMessage.value = null
                sendTypingStatus(senderId, false)
            }
        }
    }

    fun setReplyingTo(message: ChatMessage?) {
        _replyingToMessage.value = message
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun onUserTyping(senderId: String, text: String) {
        val channel = _currentChannel.value ?: return
        typingJob?.cancel()
        typingJob = viewModelScope.launch {
            repository.setTyping(channel.id, senderId, text.isNotBlank())
            if (text.isNotBlank()) {
                delay(3000)
                repository.setTyping(channel.id, senderId, false)
            }
        }
    }

    private fun sendTypingStatus(senderId: String, isTyping: Boolean) {
        val channel = _currentChannel.value ?: return
        viewModelScope.launch {
            repository.setTyping(channel.id, senderId, isTyping)
        }
    }

    fun markAsRead(channelId: String, currentUserId: String) {
        viewModelScope.launch {
            repository.markChannelAsRead(channelId, currentUserId)
        }
    }

    fun deleteMessage(messageId: String, forEveryone: Boolean, currentUserId: String) {
        val channel = _currentChannel.value ?: return
        viewModelScope.launch {
            repository.deleteMessage(channel.id, messageId, forEveryone, currentUserId)
        }
    }

    fun toggleBlock(otherUserId: String, block: Boolean) {
        val channel = _currentChannel.value ?: return
        viewModelScope.launch {
            repository.toggleBlockUser(channel.id, otherUserId, block)
            _currentChannel.value = _currentChannel.value?.copy(
                isBlocked = _currentChannel.value?.isBlocked?.toMutableMap()?.apply {
                    put(otherUserId, block)
                } ?: mapOf(otherUserId to block)
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        messagesJob?.cancel()
        presenceJob?.cancel()
        typingJob?.cancel()
    }
}
