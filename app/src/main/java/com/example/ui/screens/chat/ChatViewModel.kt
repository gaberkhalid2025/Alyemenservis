package com.example.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.models.*
import com.example.data.repositories.ChatRepository
import com.example.ui.screens.chat.managers.ChatEditManager
import com.example.ui.screens.chat.managers.ChatMessagesManager
import com.example.ui.screens.chat.managers.ChatPresenceManager
import com.example.ui.screens.chat.managers.ChatTypingManager
import com.example.utils.AppResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ChatError(open val messageArabic: String) {
    data class NetworkError(override val messageArabic: String = "خطأ في الاتصال بالشبكة") : ChatError(messageArabic)
    data class PermissionDenied(override val messageArabic: String = "تم رفض الإذن المطلوب") : ChatError(messageArabic)
    data class UnknownError(val details: String) : ChatError(details)
}

sealed class ChatEvent {
    data class ShowError(val message: String) : ChatEvent()
    data class MessageSent(val messageId: String) : ChatEvent()
}

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: ChatRepository
) : ViewModel() {

    // --- Specialized Sub-Managers ---
    val messagesManager = ChatMessagesManager(repository, viewModelScope)
    val typingManager = ChatTypingManager(repository, viewModelScope)
    val presenceManager = ChatPresenceManager(repository, viewModelScope)
    val editManager = ChatEditManager(repository, viewModelScope)

    // --- Events & Global States ---
    private val _eventFlow = MutableSharedFlow<ChatEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    private val _currentChannel = MutableStateFlow<ChatChannel?>(null)
    val currentChannel: StateFlow<ChatChannel?> = _currentChannel.asStateFlow()

    val messages: StateFlow<List<ChatMessage>> = messagesManager.messages
    val isSending: StateFlow<Boolean> = messagesManager.isSending

    val otherUserPresence: StateFlow<UserPresence?> = presenceManager.otherUserPresence
    val isTypingOther: StateFlow<Boolean> = typingManager.isTypingOther
    val replyingToMessage: StateFlow<ChatMessage?> = editManager.replyingToMessage

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private var messagesJob: Job? = null
    private var markAsReadJob: Job? = null
    private var activeUserId: String = ""
    private var currentLimit = 25

    fun openChannel(channel: ChatChannel, currentUserId: String) {
        resetState()
        _currentChannel.value = channel
        activeUserId = currentUserId
        currentLimit = 25

        val otherUserId = channel.participants.firstOrNull { it != currentUserId } ?: ""
        listenToMessages(channel.id, currentUserId)
        presenceManager.listenToPresence(otherUserId)
        markAsRead(channel.id, currentUserId)
    }

    fun openChannelById(
        channelId: String,
        currentUserId: String,
        currentUserName: String = "المستخدم",
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
                val targetName = fallbackUserName ?: "المستخدم"
                val res = repository.getOrCreateChannel(
                    currentUserId = currentUserId,
                    currentUserName = currentUserName,
                    currentUserPhoto = "",
                    otherUserId = targetId,
                    otherUserName = targetName,
                    otherUserPhoto = "",
                    type = ChannelType.PRIVATE,
                    relatedEntityId = null,
                    relatedEntityType = null
                )
                when (res) {
                    is AppResult.Success -> openChannel(res.data, currentUserId)
                    is AppResult.Error -> _eventFlow.emit(ChatEvent.ShowError(res.error.messageArabic))
                }
            }
        }
    }

    fun startDirectChat(
        currentUserId: String,
        currentUserName: String,
        currentUserPhoto: String = "",
        otherUserId: String,
        otherUserName: String,
        otherUserPhoto: String = "",
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
            when (channelResult) {
                is AppResult.Success -> openChannel(channelResult.data, currentUserId)
                is AppResult.Error -> _eventFlow.emit(ChatEvent.ShowError(channelResult.error.messageArabic))
            }
        }
    }

    private fun listenToMessages(channelId: String, currentUserId: String) {
        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            repository.getChannelMessages(channelId, currentUserId, limit = currentLimit).collect { msgs ->
                messagesManager.updateMessagesList(msgs)
                markAsRead(channelId, currentUserId)
            }
        }
    }

    fun loadMoreMessages() {
        val channel = _currentChannel.value ?: return
        currentLimit += 25
        listenToMessages(channel.id, activeUserId)
    }

    fun sendMessage(
        senderId: String = activeUserId,
        senderName: String = "أنا",
        text: String,
        mediaType: MediaType = MediaType.TEXT,
        mediaUrl: String = "",
        attachment: ChatAttachment? = null
    ) {
        val channel = _currentChannel.value ?: return
        if (text.isBlank() && mediaUrl.isBlank() && attachment == null) return

        val replyTo = editManager.replyingToMessage.value
        editManager.setReplyingTo(null)

        messagesManager.sendMessage(
            channelId = channel.id,
            senderId = senderId.ifBlank { activeUserId },
            senderName = senderName,
            text = text.trim(),
            mediaType = mediaType,
            mediaUrl = mediaUrl,
            replyTo = replyTo,
            attachment = attachment,
            onSuccess = { sentMsg ->
                viewModelScope.launch {
                    _eventFlow.emit(ChatEvent.MessageSent(sentMsg.id))
                }
            },
            onError = { errMsg ->
                viewModelScope.launch {
                    _eventFlow.emit(ChatEvent.ShowError(errMsg))
                }
            }
        )
    }

    fun updateMessageStatus(messageId: String, status: MessageStatus) {
        messagesManager.updateMessageStatus(messageId, status)
    }

    fun resendMessage(messageId: String) {
        val channel = _currentChannel.value ?: return
        val targetMsg = messages.value.find { it.id == messageId } ?: return

        messagesManager.resendMessage(
            channelId = channel.id,
            currentUserId = activeUserId,
            targetMsg = targetMsg,
            onSuccess = { sentMsg ->
                viewModelScope.launch {
                    _eventFlow.emit(ChatEvent.MessageSent(sentMsg.id))
                }
            },
            onError = { errMsg ->
                viewModelScope.launch {
                    _eventFlow.emit(ChatEvent.ShowError(errMsg))
                }
            }
        )
    }

    fun resendMessage(messageId: String, senderId: String, senderName: String) {
        resendMessage(messageId)
    }

    fun setReplyingTo(message: ChatMessage?) {
        editManager.setReplyingTo(message)
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun onUserTyping(senderId: String, text: String) {
        val channel = _currentChannel.value ?: return
        typingManager.onUserTyping(channel.id, senderId, text)
    }

    fun markAsRead(channelId: String, currentUserId: String) {
        markAsReadJob?.cancel()
        markAsReadJob = viewModelScope.launch {
            try {
                repository.markChannelAsRead(channelId, currentUserId)
            } catch (_: Exception) {}
        }
    }

    fun editMessage(channelId: String, messageId: String, newText: String) {
        editManager.editMessage(channelId, messageId, newText,
            onError = { viewModelScope.launch { _eventFlow.emit(ChatEvent.ShowError(it)) } }
        )
    }

    fun editMessage(messageId: String, newText: String) {
        val channel = _currentChannel.value ?: return
        editMessage(channel.id, messageId, newText)
    }

    fun deleteMessage(channelId: String, messageId: String) {
        editManager.deleteMessage(channelId, messageId, forEveryone = true, currentUserId = activeUserId,
            onError = { viewModelScope.launch { _eventFlow.emit(ChatEvent.ShowError(it)) } }
        )
    }

    fun deleteMessage(messageId: String, forEveryone: Boolean, currentUserId: String) {
        val channel = _currentChannel.value ?: return
        editManager.deleteMessage(channel.id, messageId, forEveryone, currentUserId,
            onError = { viewModelScope.launch { _eventFlow.emit(ChatEvent.ShowError(it)) } }
        )
    }

    fun toggleBlock(otherUserId: String, block: Boolean) {
        val channel = _currentChannel.value ?: return
        viewModelScope.launch {
            try {
                val res = repository.toggleBlockUser(channel.id, otherUserId, block)
                if (res is AppResult.Error) {
                    _eventFlow.emit(ChatEvent.ShowError(res.error.messageArabic))
                }
            } catch (e: Exception) {
                _eventFlow.emit(ChatEvent.ShowError(e.message ?: "فشل تغيير حالة الحظر"))
            }
        }
    }

    fun deleteCurrentChannel(onDeleted: () -> Unit) {
        val channel = _currentChannel.value ?: return
        viewModelScope.launch {
            try {
                repository.deleteChannel(channel.id)
                onDeleted()
            } catch (e: Exception) {
                _eventFlow.emit(ChatEvent.ShowError(e.message ?: "فشل حذف المحادثة"))
            }
        }
    }

    fun resetState() {
        messagesJob?.cancel()
        presenceManager.clear()
        typingManager.clear()
        editManager.clear()
        _currentChannel.value = null
        messagesManager.updateMessagesList(emptyList())
        _searchQuery.value = ""
    }

    override fun onCleared() {
        super.onCleared()
        resetState()
    }
}
