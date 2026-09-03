package com.example.data.repositories.contracts

import com.example.data.models.*
import com.example.data.utils.AppResult
import kotlinx.coroutines.flow.Flow

interface IChatRepository {
    suspend fun getOrCreateChannel(
        currentUserId: String,
        currentUserName: String,
        currentUserPhoto: String,
        otherUserId: String,
        otherUserName: String,
        otherUserPhoto: String,
        type: ChannelType = ChannelType.PRIVATE,
        relatedEntityId: String? = null,
        relatedEntityType: String? = null
    ): AppResult<ChatChannel>

    suspend fun getChannelById(channelId: String): AppResult<ChatChannel?>
    fun getUserChannels(userId: String): Flow<List<ChatChannel>>
    fun getChannelMessages(channelId: String, currentUserId: String): Flow<List<ChatMessage>>
    suspend fun sendMessage(
        channelId: String,
        senderId: String,
        senderName: String,
        messageText: String,
        mediaType: MediaType = MediaType.TEXT,
        mediaUrl: String = "",
        replyToId: String? = null,
        replyToText: String? = null,
        attachment: ChatAttachment? = null
    ): AppResult<ChatMessage>

    suspend fun retryPendingMessages(currentUserId: String): AppResult<Int>
    suspend fun markChannelAsRead(channelId: String, currentUserId: String): AppResult<Unit>
    suspend fun setTyping(channelId: String, userId: String, isTyping: Boolean): AppResult<Unit>
    suspend fun toggleBlockUser(channelId: String, userIdToBlock: String, isBlocked: Boolean): AppResult<Unit>
    suspend fun deleteMessage(channelId: String, messageId: String, forEveryone: Boolean, currentUserId: String): AppResult<Unit>
    suspend fun toggleReaction(channelId: String, messageId: String, userId: String, emoji: String): AppResult<Unit>
    suspend fun deleteChannel(channelId: String): AppResult<Unit>
    suspend fun deleteAllChannels(channelsList: List<ChatChannel>): AppResult<Unit>
    suspend fun setUserPresence(userId: String, isOnline: Boolean): AppResult<Unit>
    fun getUserPresence(userId: String): Flow<UserPresence?>
    suspend fun syncChannelDelta(channelId: String): AppResult<Int>
    fun clearListeners()
}
