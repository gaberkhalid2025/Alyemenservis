package com.example.data.repositories.impl

import android.content.Context
import android.util.Log
import com.example.data.local.ChatLocalDataSource
import com.example.data.models.*
import com.example.data.repositories.contracts.IChatRepository
import com.example.data.repositories.FirebaseOptimizationManager
import com.example.data.utils.AppError
import com.example.data.utils.AppResult
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await

class ChatRepositoryImpl(
    private val context: Context?,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val localDataSource: ChatLocalDataSource? = null
) : IChatRepository {

    private val local: ChatLocalDataSource? by lazy {
        localDataSource ?: context?.let { ChatLocalDataSource.getInstance(it) }
    }

    private val channelsCollection = firestore.collection("chat_channels")
    private val presenceCollection = firestore.collection("user_presence")
    private val activeListeners = mutableListOf<ListenerRegistration>()

    override fun clearListeners() {
        try {
            activeListeners.forEach { it.remove() }
            activeListeners.clear()
        } catch (e: Exception) {
            Log.e("ChatRepositoryImpl", "Error clearing listeners", e)
        }
    }

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
    ): AppResult<ChatChannel> = withContext(Dispatchers.IO) {
        try {
            val cleanCurrent = currentUserId.trim()
            val cleanOther = otherUserId.trim()
            if (cleanCurrent.isBlank()) {
                return@withContext Result.failure(AppError.ValidationError("currentUserId", "معرف المستخدم الحالي فارغ"))
            }

            val sortedParticipants = listOf(cleanCurrent, cleanOther).filter { it.isNotBlank() }.sorted()
            val customChannelId = when {
                type == ChannelType.PRIVATE && sortedParticipants.size == 2 -> "channel_${sortedParticipants[0]}_${sortedParticipants[1]}"
                relatedEntityId != null -> "channel_${type.name.lowercase()}_${relatedEntityId.trim()}"
                else -> channelsCollection.document().id
            }

            val cached = local?.getChannelById(customChannelId)
            if (cached != null) {
                local?.saveOrUpdateChannel(cached)
            }

            val docRef = channelsCollection.document(customChannelId)
            val snapshot = docRef.get().await()

            val channelToReturn = if (snapshot.exists()) {
                val existing = snapshot.toObject(ChatChannel::class.java)?.copy(id = snapshot.id) ?: cached ?: ChatChannel(id = customChannelId)
                val updatedNames = existing.participantNames.toMutableMap().apply {
                    put(cleanCurrent, currentUserName)
                    if (cleanOther.isNotBlank() && otherUserName.isNotBlank()) put(cleanOther, otherUserName)
                }
                val updatedPhotos = existing.participantPhotos.toMutableMap().apply {
                    put(cleanCurrent, currentUserPhoto)
                    if (cleanOther.isNotBlank() && otherUserPhoto.isNotBlank()) put(cleanOther, otherUserPhoto)
                }
                val updated = existing.copy(
                    participantNames = updatedNames,
                    participantPhotos = updatedPhotos,
                    updatedAt = System.currentTimeMillis()
                )
                docRef.update(
                    mapOf(
                        "participantNames" to updatedNames,
                        "participantPhotos" to updatedPhotos,
                        "updatedAt" to updated.updatedAt
                    )
                )
                updated
            } else {
                val newChannel = ChatChannel(
                    id = customChannelId,
                    participants = sortedParticipants,
                    participantNames = mapOf(
                        cleanCurrent to currentUserName,
                        cleanOther to otherUserName
                    ),
                    participantPhotos = mapOf(
                        cleanCurrent to currentUserPhoto,
                        cleanOther to otherUserPhoto
                    ),
                    type = type,
                    relatedEntityId = relatedEntityId,
                    relatedEntityType = relatedEntityType,
                    lastMessage = "محادثة جديدة",
                    lastMessageTime = System.currentTimeMillis(),
                    lastMessageSenderId = cleanCurrent,
                    unreadCount = mapOf(
                        cleanCurrent to 0,
                        cleanOther to 0
                    ),
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                docRef.set(newChannel).await()
                newChannel
            }

            local?.saveOrUpdateChannel(channelToReturn)
            Result.success(channelToReturn)
        } catch (e: Exception) {
            Log.e("ChatRepositoryImpl", "getOrCreateChannel failed: ${e.message}", e)
            val fallback = local?.getChannelById("channel_${listOf(currentUserId, otherUserId).sorted().joinToString("_")}")
            if (fallback != null) {
                Result.success(fallback)
            } else {
                Result.failure(AppError.NetworkError(e.localizedMessage ?: "خطأ في الاتصال"))
            }
        }
    }

    override suspend fun getChannelById(channelId: String): AppResult<ChatChannel?> = withContext(Dispatchers.IO) {
        if (channelId.isBlank()) return@withContext Result.success(null)
        try {
            val cached = local?.getChannelById(channelId)
            if (cached != null) {
                return@withContext Result.success(cached)
            }

            val snapshot = channelsCollection.document(channelId).get().await()
            if (snapshot.exists()) {
                val channel = snapshot.toObject(ChatChannel::class.java)?.copy(id = snapshot.id)
                if (channel != null) {
                    local?.saveOrUpdateChannel(channel)
                }
                Result.success(channel)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            val cached = local?.getChannelById(channelId)
            if (cached != null) Result.success(cached)
            else Result.failure(AppError.NetworkError(e.localizedMessage ?: "خطأ في الاتصال"))
        }
    }

    override fun getUserChannels(userId: String): Flow<List<ChatChannel>> = callbackFlow {
        val cleanUserId = userId.trim()
        if (cleanUserId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        local?.getChannels()?.let { cached ->
            if (cached.isNotEmpty()) {
                trySend(cached)
            }
        }

        val listener = channelsCollection
            .whereArrayContains("participants", cleanUserId)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ChatRepositoryImpl", "Firestore channels listener error: ${error.message}")
                    return@addSnapshotListener
                }

                val remoteChannels = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(ChatChannel::class.java)?.copy(id = doc.id)
                }?.sortedByDescending { it.lastMessageTime } ?: emptyList()

                CoroutineScope(Dispatchers.IO).launch {
                    local?.saveChannels(remoteChannels)
                }
                trySend(remoteChannels)
            }
        activeListeners.add(listener)

        awaitClose {
            listener.remove()
        }
    }.flowOn(Dispatchers.IO)

    override fun getChannelMessages(channelId: String, currentUserId: String): Flow<List<ChatMessage>> = callbackFlow {
        if (channelId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val cached = local?.getMessages(channelId) ?: emptyList()
        val visibleCached = cached.filter { !it.isHiddenFor(currentUserId) }
        trySend(visibleCached)

        val messagesRef = channelsCollection.document(channelId).collection("messages")
        val listener = messagesRef
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ChatRepositoryImpl", "Messages listener error: ${error.message}")
                    return@addSnapshotListener
                }

                val remoteMessages = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(ChatMessage::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                CoroutineScope(Dispatchers.IO).launch {
                    val currentLocal = local?.getMessages(channelId) ?: emptyList()
                    val pendingLocal = currentLocal.filter { it.status == MessageStatus.PENDING || it.status == MessageStatus.SENDING }

                    val mergedMap = LinkedHashMap<String, ChatMessage>()
                    for (remote in remoteMessages) {
                        mergedMap[remote.id] = remote
                    }
                    for (pending in pendingLocal) {
                        if (!mergedMap.containsKey(pending.id)) {
                            mergedMap[pending.id] = pending
                        }
                    }

                    val mergedList = mergedMap.values.sortedBy { it.timestamp }
                    local?.saveMessages(channelId, mergedList)
                    local?.setLastSyncTimestamp(channelId, System.currentTimeMillis())

                    val visible = mergedList.filter { !it.isHiddenFor(currentUserId) }
                    trySend(visible)
                }
            }
        activeListeners.add(listener)

        awaitClose {
            listener.remove()
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun sendMessage(
        channelId: String,
        senderId: String,
        senderName: String,
        messageText: String,
        mediaType: MediaType,
        mediaUrl: String,
        replyToId: String?,
        replyToText: String?,
        attachment: ChatAttachment?
    ): AppResult<ChatMessage> = withContext(Dispatchers.IO) {
        try {
            if (channelId.isBlank() || senderId.isBlank()) {
                return@withContext Result.failure(AppError.ValidationError("message", "بيانات الرسالة غير مكتملة"))
            }

            val messageId = channelsCollection.document(channelId).collection("messages").document().id
            val now = System.currentTimeMillis()

            val initialMsg = ChatMessage(
                id = messageId,
                channelId = channelId,
                senderId = senderId,
                senderName = senderName,
                message = messageText,
                mediaType = mediaType,
                mediaUrl = mediaUrl,
                attachment = attachment,
                replyToId = replyToId,
                replyToText = replyToText,
                status = MessageStatus.SENDING,
                timestamp = now,
                syncStatus = SyncStatus.PENDING_UPLOAD
            )

            local?.insertOrUpdateMessage(initialMsg)

            val channelRef = channelsCollection.document(channelId)
            val channelSnapshot = channelRef.get().await()
            val channel = channelSnapshot.toObject(ChatChannel::class.java)

            if (channel != null && channel.isUserBlocked(senderId)) {
                local?.updateMessageStatus(channelId, messageId, MessageStatus.FAILED)
                return@withContext Result.failure(AppError.UnauthorizedError("لا يمكنك إرسال الرسالة لأنك محظور في هذه المحادثة."))
            }

            val confirmedMsg = initialMsg.copy(status = MessageStatus.SENT, syncStatus = SyncStatus.SYNCED)
            channelRef.collection("messages").document(messageId).set(confirmedMsg).await()

            val updatedUnread = (channel?.unreadCount ?: emptyMap()).toMutableMap()
            channel?.participants?.forEach { pId ->
                if (pId != senderId) {
                    val count = updatedUnread[pId] ?: 0
                    updatedUnread[pId] = count + 1
                }
            }

            val displayLast = when (mediaType) {
                MediaType.IMAGE -> "📷 صورة"
                MediaType.VIDEO -> "🎥 فيديو"
                MediaType.AUDIO -> "🎤 تسجيل صوتي"
                MediaType.FILE -> "📎 ${attachment?.fileName.orEmpty().ifBlank { "ملف مرفق" }}"
                MediaType.LOCATION -> "📍 موقع جغرافي"
                MediaType.CALL -> "📞 مكالمة صوتية"
                MediaType.TEXT -> messageText
            }

            channelRef.update(
                mapOf(
                    "lastMessage" to displayLast,
                    "lastMessageTime" to now,
                    "lastMessageSenderId" to senderId,
                    "unreadCount" to updatedUnread,
                    "updatedAt" to now
                )
            ).await()

            local?.insertOrUpdateMessage(confirmedMsg)
            Result.success(confirmedMsg)
        } catch (e: Exception) {
            Log.e("ChatRepositoryImpl", "sendMessage failed, saving to offline queue: ${e.message}")
            val offlineMsg = ChatMessage(
                id = java.util.UUID.randomUUID().toString(),
                channelId = channelId,
                senderId = senderId,
                senderName = senderName,
                message = messageText,
                mediaType = mediaType,
                mediaUrl = mediaUrl,
                attachment = attachment,
                replyToId = replyToId,
                replyToText = replyToText,
                status = MessageStatus.PENDING,
                timestamp = System.currentTimeMillis(),
                syncStatus = SyncStatus.PENDING_UPLOAD
            )
            local?.queuePendingMessage(offlineMsg)
            Result.success(offlineMsg)
        }
    }

    override suspend fun retryPendingMessages(currentUserId: String): AppResult<Int> = withContext(Dispatchers.IO) {
        try {
            val pending = local?.getPendingMessages() ?: emptyList()
            var successCount = 0

            for (msg in pending) {
                val sendResult = sendMessage(
                    channelId = msg.channelId,
                    senderId = msg.senderId,
                    senderName = msg.senderName,
                    messageText = msg.message,
                    mediaType = msg.mediaType,
                    mediaUrl = msg.mediaUrl,
                    replyToId = msg.replyToId,
                    replyToText = msg.replyToText,
                    attachment = msg.attachment
                )
                if (sendResult.isSuccess) {
                    local?.removePendingMessage(msg.id)
                    successCount++
                }
            }
            Result.success(successCount)
        } catch (e: Exception) {
            Result.failure(AppError.NetworkError(e.localizedMessage ?: "خطأ في الاتصال"))
        }
    }

    override suspend fun markChannelAsRead(channelId: String, currentUserId: String): AppResult<Unit> = withContext(Dispatchers.IO) {
        if (channelId.isBlank() || currentUserId.isBlank()) return@withContext Result.success(Unit)
        try {
            local?.markMessagesAsRead(channelId, currentUserId)

            val channelRef = channelsCollection.document(channelId)
            channelRef.update("unreadCount.$currentUserId", 0).await()

            val unreadSnapshot = channelRef.collection("messages")
                .whereNotEqualTo("senderId", currentUserId)
                .get().await()

            val unreadDocs = unreadSnapshot.documents.filter { doc ->
                doc.getString("status") != MessageStatus.READ.name
            }

            if (unreadDocs.isNotEmpty()) {
                val batch = firestore.batch()
                for (doc in unreadDocs) {
                    batch.update(doc.reference, "status", MessageStatus.READ.name)
                }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ChatRepositoryImpl", "markChannelAsRead error: ${e.message}")
            Result.failure(AppError.NetworkError(e.localizedMessage ?: "خطأ في الاتصال"))
        }
    }

    override suspend fun setTyping(channelId: String, userId: String, isTyping: Boolean): AppResult<Unit> = withContext(Dispatchers.IO) {
        if (channelId.isBlank() || userId.isBlank()) return@withContext Result.success(Unit)
        try {
            channelsCollection.document(channelId).update("isTyping.$userId", isTyping).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(AppError.NetworkError(e.localizedMessage ?: "خطأ في الاتصال"))
        }
    }

    override suspend fun toggleBlockUser(channelId: String, userIdToBlock: String, isBlocked: Boolean): AppResult<Unit> = withContext(Dispatchers.IO) {
        if (channelId.isBlank() || userIdToBlock.isBlank()) return@withContext Result.success(Unit)
        try {
            channelsCollection.document(channelId).update("isBlocked.$userIdToBlock", isBlocked).await()
            val cached = local?.getChannelById(channelId)
            if (cached != null) {
                val updatedBlocked = cached.isBlocked.toMutableMap().apply { put(userIdToBlock, isBlocked) }
                local?.saveOrUpdateChannel(cached.copy(isBlocked = updatedBlocked))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(AppError.NetworkError(e.localizedMessage ?: "خطأ في الاتصال"))
        }
    }

    override suspend fun deleteMessage(
        channelId: String,
        messageId: String,
        forEveryone: Boolean,
        currentUserId: String
    ): AppResult<Unit> = withContext(Dispatchers.IO) {
        if (channelId.isBlank() || messageId.isBlank()) return@withContext Result.success(Unit)
        try {
            if (forEveryone) {
                local?.deleteMessage(channelId, messageId)
            }

            val msgRef = channelsCollection.document(channelId).collection("messages").document(messageId)
            if (forEveryone) {
                msgRef.update(
                    mapOf(
                        "isDeleted" to true,
                        "message" to "تم حذف هذه الرسالة"
                    )
                ).await()
            } else {
                msgRef.update("deletedForUsers", FieldValue.arrayUnion(currentUserId)).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(AppError.NetworkError(e.localizedMessage ?: "خطأ في الاتصال"))
        }
    }

    override suspend fun toggleReaction(
        channelId: String,
        messageId: String,
        userId: String,
        emoji: String
    ): AppResult<Unit> = withContext(Dispatchers.IO) {
        if (channelId.isBlank() || messageId.isBlank() || userId.isBlank()) return@withContext Result.success(Unit)
        try {
            val msgRef = channelsCollection.document(channelId).collection("messages").document(messageId)
            val snapshot = msgRef.get().await()
            val currentReactions = snapshot.toObject(ChatMessage::class.java)?.reactions?.toMutableMap() ?: mutableMapOf()

            if (currentReactions[userId] == emoji) {
                currentReactions.remove(userId)
            } else {
                currentReactions[userId] = emoji
            }

            msgRef.update("reactions", currentReactions).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(AppError.NetworkError(e.localizedMessage ?: "خطأ في الاتصال"))
        }
    }

    override suspend fun deleteChannel(channelId: String): AppResult<Unit> = withContext(Dispatchers.IO) {
        if (channelId.isBlank()) return@withContext Result.success(Unit)
        try {
            local?.deleteChannel(channelId)
            channelsCollection.document(channelId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(AppError.NetworkError(e.localizedMessage ?: "خطأ في الاتصال"))
        }
    }

    override suspend fun deleteAllChannels(channelsList: List<ChatChannel>): AppResult<Unit> = withContext(Dispatchers.IO) {
        if (channelsList.isEmpty()) return@withContext Result.success(Unit)
        try {
            local?.clearAllChannels()
            val batch = firestore.batch()
            channelsList.forEach { ch ->
                batch.delete(channelsCollection.document(ch.id))
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(AppError.NetworkError(e.localizedMessage ?: "خطأ في الاتصال"))
        }
    }

    override suspend fun setUserPresence(userId: String, isOnline: Boolean): AppResult<Unit> = withContext(Dispatchers.IO) {
        if (userId.isBlank()) return@withContext Result.success(Unit)
        try {
            val presence = UserPresence(
                userId = userId,
                isOnline = isOnline,
                lastSeen = System.currentTimeMillis()
            )
            local?.saveUserPresence(presence)
            presenceCollection.document(userId).set(presence, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(AppError.NetworkError(e.localizedMessage ?: "خطأ في الاتصال"))
        }
    }

    override fun getUserPresence(userId: String): Flow<UserPresence?> = callbackFlow {
        if (userId.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }

        local?.observeUserPresence(userId)?.let { localFlow ->
            CoroutineScope(Dispatchers.IO).launch {
                localFlow.collect { cached ->
                    trySend(cached)
                }
            }
        }

        val listener = presenceCollection.document(userId).addSnapshotListener { snapshot, _ ->
            val presence = snapshot?.toObject(UserPresence::class.java)
            if (presence != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    local?.saveUserPresence(presence)
                }
            }
            trySend(presence)
        }
        activeListeners.add(listener)

        awaitClose { listener.remove() }
    }.flowOn(Dispatchers.IO)

    override suspend fun syncChannelDelta(channelId: String): AppResult<Int> = withContext(Dispatchers.IO) {
        if (channelId.isBlank()) return@withContext Result.success(0)
        try {
            val lastSync = local?.getLastSyncTimestamp(channelId) ?: 0L
            val messagesRef = channelsCollection.document(channelId).collection("messages")

            val snapshot = if (lastSync > 0) {
                messagesRef.whereGreaterThan("timestamp", lastSync).get().await()
            } else {
                messagesRef.orderBy("timestamp", Query.Direction.DESCENDING).limit(50).get().await()
            }

            val newMessages = snapshot.documents.mapNotNull { doc ->
                doc.toObject(ChatMessage::class.java)?.copy(id = doc.id)
            }

            if (newMessages.isNotEmpty()) {
                val current = local?.getMessages(channelId) ?: emptyList()
                val merged = (current + newMessages).distinctBy { it.id }.sortedBy { it.timestamp }
                local?.saveMessages(channelId, merged)
                local?.setLastSyncTimestamp(channelId, System.currentTimeMillis())
            }

            Result.success(newMessages.size)
        } catch (e: Exception) {
            Result.failure(AppError.NetworkError(e.localizedMessage ?: "خطأ في الاتصال"))
        }
    }
}
