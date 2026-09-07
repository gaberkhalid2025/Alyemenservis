package com.example.ui.viewmodels.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ChatChannelEntity
import com.example.data.ChatMessageEntity
import com.example.data.models.ChannelType
import com.example.data.models.ChatChannel
import com.example.data.models.ChatMessage
import com.example.data.models.MediaType
import com.example.data.repositories.ChatRepository
import com.example.utils.AppResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 💬 ChatCoordinator
 * إدارة المحادثات والقنوات والرسائل بشكل مركزي
 */
@HiltViewModel
class ChatCoordinator @Inject constructor(
    private val chatRepository: ChatRepository
) : ViewModel() {

    constructor() : this(ChatRepository())

    data class ChatState(
        val channels: List<ChatChannelEntity> = emptyList(),
        val messages: List<ChatMessageEntity> = emptyList(),
        val activeChannel: ChatChannelEntity? = null,
        val isLoading: Boolean = false,
        val isRefreshing: Boolean = false,
        val error: String? = null,
        val unreadCount: Int = 0
    )

    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state.asStateFlow()

    // ===== STATE FLOWS FOR BACKWARDS COMPATIBILITY =====
    val _chatMessages = MutableStateFlow<List<ChatMessageEntity>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessageEntity>> = _chatMessages.asStateFlow()

    val _chatChannels = MutableStateFlow<List<ChatChannelEntity>>(emptyList())
    val chatChannels: StateFlow<List<ChatChannelEntity>> = _chatChannels.asStateFlow()

    val _activeChatChannel = MutableStateFlow<ChatChannelEntity?>(null)
    val activeChatChannel: StateFlow<ChatChannelEntity?> = _activeChatChannel.asStateFlow()

    val _isChatChannelsLoading = MutableStateFlow(true)
    val isChatChannelsLoading: StateFlow<Boolean> = _isChatChannelsLoading.asStateFlow()

    // ===== CHAT FUNCTIONS =====

    fun loadUserChannels(userId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            chatRepository.getUserChannels(userId).collect { channelList ->
                val entities = channelList.map { mapToChatChannelEntity(it) }
                _chatChannels.value = entities
                _isChatChannelsLoading.value = false
                val unread = channelList.sumOf { it.unreadCount[userId] ?: 0 }
                _state.update {
                    it.copy(
                        channels = entities,
                        isLoading = false,
                        unreadCount = unread
                    )
                }
            }
        }
    }

    fun loadChannelMessages(channelId: String, currentUserId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            chatRepository.getChannelMessages(channelId, currentUserId).collect { messageList ->
                val entities = messageList.map { mapToChatMessageEntity(it) }
                _chatMessages.value = entities
                _state.update {
                    it.copy(
                        messages = entities,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun sendMessage(
        channelId: String,
        senderId: String,
        senderName: String,
        message: String,
        mediaType: MediaType = MediaType.TEXT,
        mediaUrl: String = "",
        replyToId: String? = null,
        replyToText: String? = null,
        onSuccess: (ChatMessageEntity?) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = chatRepository.sendMessage(
                channelId = channelId,
                senderId = senderId,
                senderName = senderName,
                messageText = message,
                mediaType = mediaType,
                mediaUrl = mediaUrl,
                replyToId = replyToId,
                replyToText = replyToText
            )

            result.onSuccess { sentMessage ->
                val newMessage = ChatMessageEntity(
                    id = sentMessage.id,
                    senderId = sentMessage.senderId,
                    senderName = sentMessage.senderName,
                    message = sentMessage.message,
                    timestamp = sentMessage.timestamp,
                    mediaType = sentMessage.mediaType.name,
                    mediaUrl = sentMessage.mediaUrl,
                    status = "SENT"
                )
                _chatMessages.value = _chatMessages.value + newMessage
                _state.update { it.copy(messages = _chatMessages.value) }
                onSuccess(newMessage)
            }.onError { error ->
                onError(error.messageArabic)
            }
        }
    }

    fun openChannel(channelId: String, currentUserId: String) {
        viewModelScope.launch {
            val result = chatRepository.getChannelById(channelId)
            result.onSuccess { channel ->
                if (channel != null) {
                    val channelEntity = mapToChatChannelEntity(channel)
                    _activeChatChannel.value = channelEntity
                    _state.update { it.copy(activeChannel = channelEntity) }
                    loadChannelMessages(channelId, currentUserId)
                    markChannelAsRead(channelId, currentUserId)
                }
            }
        }
    }

    fun openOrCreateChannel(
        targetId: String,
        targetName: String,
        targetType: String = "PROVIDER",
        currentUserId: String,
        currentUserName: String,
        currentUserPhone: String = "",
        relatedEntityId: String? = null,
        relatedEntityType: String? = null,
        onCreated: (ChatChannelEntity?) -> Unit = {}
    ) {
        viewModelScope.launch {
            val channelType = when (targetType.uppercase()) {
                "SUPPORT" -> ChannelType.SUPPORT
                "STORE" -> ChannelType.STORE
                "PROVIDER" -> ChannelType.PROVIDER
                "GROUP" -> ChannelType.GROUP
                else -> ChannelType.PRIVATE
            }

            val result = chatRepository.getOrCreateChannel(
                currentUserId = currentUserId,
                currentUserName = currentUserName,
                currentUserPhoto = "",
                otherUserId = targetId,
                otherUserName = targetName,
                otherUserPhoto = "",
                type = channelType,
                relatedEntityId = relatedEntityId,
                relatedEntityType = relatedEntityType
            )

            result.onSuccess { channel ->
                val channelEntity = mapToChatChannelEntity(channel)
                _activeChatChannel.value = channelEntity
                _state.update { it.copy(activeChannel = channelEntity) }
                if (!_chatChannels.value.any { it.id == channel.id }) {
                    _chatChannels.value = _chatChannels.value + channelEntity
                }
                onCreated(channelEntity)
            }.onError {
                onCreated(null)
            }
        }
    }

    fun markChannelAsRead(channelId: String, currentUserId: String) {
        viewModelScope.launch {
            chatRepository.markChannelAsRead(channelId, currentUserId)
            _chatChannels.value = _chatChannels.value.map { ch ->
                if (ch.id == channelId) {
                    ch.copy(unreadCountUser = 0)
                } else ch
            }
            _state.update { it.copy(channels = _chatChannels.value) }
        }
    }

    fun deleteChannel(channelId: String) {
        viewModelScope.launch {
            chatRepository.deleteChannel(channelId)
            _chatChannels.value = _chatChannels.value.filter { it.id != channelId }
            _state.update { it.copy(channels = _chatChannels.value) }
            if (_activeChatChannel.value?.id == channelId) {
                _activeChatChannel.value = null
                _state.update { it.copy(activeChannel = null) }
            }
        }
    }

    fun deleteAllChannels() {
        viewModelScope.launch {
            val channels = _chatChannels.value
            chatRepository.deleteAllChannels(channels.map {
                ChatChannel(id = it.id)
            })
            _chatChannels.value = emptyList()
            _state.update { it.copy(channels = emptyList()) }
            _activeChatChannel.value = null
            _state.update { it.copy(activeChannel = null) }
        }
    }

    fun blockChannel(channelId: String, isBlocked: Boolean) {
        viewModelScope.launch {
            _chatChannels.value = _chatChannels.value.map { ch ->
                if (ch.id == channelId) {
                    ch.copy(isBlocked = isBlocked)
                } else ch
            }
            _state.update { it.copy(channels = _chatChannels.value) }
            if (_activeChatChannel.value?.id == channelId) {
                _activeChatChannel.value = _activeChatChannel.value?.copy(isBlocked = isBlocked)
                _state.update { it.copy(activeChannel = _activeChatChannel.value) }
            }
        }
    }

    fun getUnreadCount(): Int {
        return _chatChannels.value.sumOf { it.unreadCountUser }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }
            _state.update { it.copy(isRefreshing = false) }
        }
    }

    // ===== HELPER FUNCTIONS =====

    private fun mapToChatChannelEntity(channel: ChatChannel): ChatChannelEntity {
        val firstParticipant = channel.participants.firstOrNull() ?: ""
        val otherUserId = channel.participants.firstOrNull { it != firstParticipant } ?: firstParticipant

        return ChatChannelEntity(
            id = channel.id,
            channelType = channel.type.name,
            targetId = otherUserId,
            targetName = channel.participantNames[otherUserId] ?: "",
            userName = channel.participantNames[firstParticipant] ?: "",
            customerName = channel.participantNames.values.firstOrNull() ?: "",
            customerId = firstParticipant,
            lastMessage = channel.lastMessage,
            lastMessageTime = channel.lastMessageTime,
            isBlocked = channel.isBlocked[otherUserId] == true,
            unreadCountUser = channel.unreadCount[firstParticipant] ?: 0,
            unreadCountTarget = channel.unreadCount[otherUserId] ?: 0,
            relatedEntityId = channel.relatedEntityId ?: "",
            relatedEntityType = channel.relatedEntityType ?: "",
            timestamp = channel.createdAt
        )
    }

    private fun mapToChatMessageEntity(msg: ChatMessage): ChatMessageEntity {
        return ChatMessageEntity(
            id = msg.id,
            senderId = msg.senderId,
            senderName = msg.senderName,
            senderPhone = "",
            message = msg.message,
            timestamp = msg.timestamp,
            mediaType = msg.mediaType.name,
            mediaUrl = msg.mediaUrl,
            status = msg.status.name,
            imageUrl = if (msg.mediaType == MediaType.IMAGE) msg.mediaUrl else "",
            replyToId = msg.replyToId ?: "",
            replyToText = msg.replyToText ?: "",
            replyToSender = msg.replyToSender ?: ""
        )
    }
}
