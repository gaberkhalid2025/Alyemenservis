package com.example.data.repositories

import android.content.Context
import com.example.data.local.ChatLocalDataSource
import com.example.data.models.*
import com.example.utils.AppResult
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow

/**
 * 🚀 ChatRepository
 * الواجهة الموحدة (Facade) لمستودع المحادثات بنظام (Offline-First) والمزامنة الحية
 * تفوّض العمليات إلى:
 * - [ChatChannelDelegate] لإدارة القنوات
 * - [ChatMessageDelegate] لإدارة الرسائل والمزامنة التفاضلية
 * - [ChatPresenceDelegate] لحالة الاتصال والكتابة والحظر
 */
class ChatRepository(
    private val context: Context? = null,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val localDataSource: ChatLocalDataSource? = null
) : IChatRepository {

    private val local: ChatLocalDataSource? by lazy {
        localDataSource ?: context?.let { ChatLocalDataSource.getInstance(it) }
    }

    private val channelsCollection = firestore.collection("chat_channels")
    private val presenceCollection = firestore.collection("user_presence")
    private val realtimeDb: FirebaseDatabase? by lazy {
        try {
            FirebaseDatabase.getInstance()
        } catch (_: Exception) {
            null
        }
    }

    private val channelDelegate = ChatChannelDelegate(
        context = context,
        firestore = firestore,
        channelsCollection = channelsCollection,
        localProvider = { local }
    )

    private val messageDelegate = ChatMessageDelegate(
        firestore = firestore,
        channelsCollection = channelsCollection,
        localProvider = { local }
    )

    private val presenceDelegate = ChatPresenceDelegate(
        channelsCollection = channelsCollection,
        presenceCollection = presenceCollection,
        realtimeDbProvider = { realtimeDb },
        localProvider = { local }
    )

    companion object {
        const val SUPPORT_ADMIN_ID = "ADMIN"
        const val SUPPORT_ADMIN_NAME = "الدعم الفني"
    }

    // 1. Channels
    override suspend fun getOrCreateChannel(
        currentUserId: String,
        currentUserName: String,
        currentUserPhoto: String,
        otherUserId: String,
        otherUserName: String,
        otherUserPhoto: String,
        type: ChannelType,
        relatedEntityId: String?,
        relatedEntityType: String?
    ): AppResult<ChatChannel> = channelDelegate.getOrCreateChannel(
        currentUserId = currentUserId,
        currentUserName = currentUserName,
        currentUserPhoto = currentUserPhoto,
        otherUserId = otherUserId,
        otherUserName = otherUserName,
        otherUserPhoto = otherUserPhoto,
        type = type,
        relatedEntityId = relatedEntityId,
        relatedEntityType = relatedEntityType
    )

    override suspend fun getChannelById(channelId: String): AppResult<ChatChannel?> =
        channelDelegate.getChannelById(channelId)

    override fun getUserChannels(userId: String): Flow<List<ChatChannel>> =
        channelDelegate.getUserChannels(userId)

    override suspend fun deleteChannel(channelId: String): AppResult<Unit> =
        channelDelegate.deleteChannel(channelId)

    override suspend fun deleteAllChannels(channelsList: List<ChatChannel>): AppResult<Unit> =
        channelDelegate.deleteAllChannels(channelsList)

    // 2. Messages & Delta Sync
    override fun getChannelMessages(channelId: String, currentUserId: String, limit: Int): Flow<List<ChatMessage>> =
        messageDelegate.getChannelMessages(channelId, currentUserId, limit)

    override suspend fun sendMessage(
        channelId: String,
        senderId: String,
        senderName: String,
        messageText: String,
        mediaType: MediaType,
        mediaUrl: String,
        replyToId: String?,
        replyToText: String?,
        attachment: ChatAttachment?,
        audioWaveform: List<Int>,
        existingMessageId: String?
    ): AppResult<ChatMessage> = messageDelegate.sendMessage(
        channelId = channelId,
        senderId = senderId,
        senderName = senderName,
        messageText = messageText,
        mediaType = mediaType,
        mediaUrl = mediaUrl,
        replyToId = replyToId,
        replyToText = replyToText,
        attachment = attachment,
        audioWaveform = audioWaveform,
        existingMessageId = existingMessageId
    )

    override suspend fun updateMessageMediaUrl(
        channelId: String,
        messageId: String,
        downloadUrl: String,
        newStatus: MessageStatus
    ): AppResult<Unit> = messageDelegate.updateMessageMediaUrl(channelId, messageId, downloadUrl, newStatus)

    override suspend fun updateMessageStatus(
        channelId: String,
        messageId: String,
        status: MessageStatus
    ): AppResult<Unit> = messageDelegate.updateMessageStatus(channelId, messageId, status)

    override suspend fun retryPendingMessages(currentUserId: String): AppResult<Int> =
        messageDelegate.retryPendingMessages(currentUserId)

    override suspend fun markChannelAsRead(channelId: String, currentUserId: String): AppResult<Unit> =
        messageDelegate.markChannelAsRead(channelId, currentUserId)

    override suspend fun editMessage(
        channelId: String,
        messageId: String,
        newText: String,
        currentUserId: String
    ): AppResult<Unit> = messageDelegate.editMessage(channelId, messageId, newText, currentUserId)

    override suspend fun deleteMessage(
        channelId: String,
        messageId: String,
        forEveryone: Boolean,
        currentUserId: String
    ): AppResult<Unit> = messageDelegate.deleteMessage(channelId, messageId, forEveryone, currentUserId)

    override suspend fun toggleReaction(
        channelId: String,
        messageId: String,
        userId: String,
        emoji: String
    ): AppResult<Unit> = messageDelegate.toggleReaction(channelId, messageId, userId, emoji)

    override suspend fun togglePinMessage(
        channelId: String,
        messageId: String,
        isPinned: Boolean
    ): AppResult<Unit> = messageDelegate.togglePinMessage(channelId, messageId, isPinned)

    override suspend fun syncChannelDelta(channelId: String): AppResult<Int> =
        messageDelegate.syncChannelDelta(channelId)

    // 3. Presence, Typing & Blocking
    override suspend fun setTyping(channelId: String, userId: String, isTyping: Boolean): AppResult<Unit> =
        presenceDelegate.setTyping(channelId, userId, isTyping)

    override suspend fun toggleBlockUser(channelId: String, userIdToBlock: String, isBlocked: Boolean): AppResult<Unit> =
        presenceDelegate.toggleBlockUser(channelId, userIdToBlock, isBlocked)

    override suspend fun setUserPresence(userId: String, isOnline: Boolean): AppResult<Unit> =
        presenceDelegate.setUserPresence(userId, isOnline)

    override fun getUserPresence(userId: String): Flow<UserPresence?> =
        presenceDelegate.getUserPresence(userId)

    override fun getTypingStatus(channelId: String, userId: String): Flow<Boolean> =
        presenceDelegate.getTypingStatus(channelId, userId)
}
