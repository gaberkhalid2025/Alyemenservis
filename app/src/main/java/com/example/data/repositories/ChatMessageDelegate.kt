package com.example.data.repositories

import android.util.Log
import com.example.data.local.ChatLocalDataSource
import com.example.data.models.*
import com.example.utils.AppError
import com.example.utils.AppResult
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * 💬 ChatMessageDelegate
 * مسؤول عن بث الرسائل، الإرسال، التعديل، الحذف، التفاعل، التثبيت، قراءة الرسائل، والمزامنة التفاضلية
 */
internal class ChatMessageDelegate(
    private val firestore: FirebaseFirestore,
    private val channelsCollection: CollectionReference,
    private val localProvider: () -> ChatLocalDataSource?
) {

    fun getChannelMessages(channelId: String, currentUserId: String, limit: Int): Flow<List<ChatMessage>> = callbackFlow {
        if (channelId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val local = localProvider()

        // 1. Emit local cache immediately from Room / SQLite
        val cached = local?.getMessages(channelId) ?: emptyList()
        val visibleCached = cached.filter { !it.isHiddenFor(currentUserId) }
        trySend(visibleCached)

        val lastLocalSyncWatermark = cached.maxOfOrNull {
            maxOf(it.updatedAt, it.editTimestamp, it.readAt, it.timestamp)
        } ?: 0L

        // 2. Real-time Firebase Listener with Delta Sync optimization (captures new & mutated messages)
        val messagesRef = channelsCollection.document(channelId).collection("messages")
        val query = if (lastLocalSyncWatermark > 0L) {
            messagesRef.whereGreaterThan("updatedAt", lastLocalSyncWatermark).orderBy("updatedAt", Query.Direction.ASCENDING)
        } else {
            messagesRef.orderBy("timestamp", Query.Direction.ASCENDING).limitToLast(limit.coerceAtLeast(25).toLong())
        }

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("ChatRepository", "Messages listener error: ${error.message}")
                return@addSnapshotListener
            }

            val remoteMessages = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(ChatMessage::class.java)?.copy(id = doc.id)
            } ?: emptyList()

            launch(Dispatchers.IO) {
                val currentLocal = local?.getMessages(channelId) ?: emptyList()
                val pendingLocal = currentLocal.filter { it.status == MessageStatus.PENDING || it.status == MessageStatus.SENDING }

                val mergedMap = LinkedHashMap<String, ChatMessage>()
                for (localMsg in currentLocal) {
                    mergedMap[localMsg.id] = localMsg
                }
                for (remote in remoteMessages) {
                    mergedMap[remote.id] = remote
                }
                for (pending in pendingLocal) {
                    if (!mergedMap.containsKey(pending.id)) {
                        mergedMap[pending.id] = pending
                    }
                }

                // Bound memory & storage to at most 150 messages
                val mergedList = mergedMap.values.sortedBy { it.timestamp }.takeLast(150)
                local?.saveMessages(channelId, mergedList)
                local?.setLastSyncTimestamp(channelId, System.currentTimeMillis())

                val visible = mergedList.filter { !it.isHiddenFor(currentUserId) }
                trySend(visible)
            }
        }

        awaitClose {
            try {
                listener.remove()
            } catch (e: Exception) {
                // Ignore cleanup error
            }
        }
    }.flowOn(Dispatchers.IO)

    suspend fun sendMessage(
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
        existingMessageId: String? = null
    ): AppResult<ChatMessage> = sendMessageInternal(
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
        existingMessageId = existingMessageId,
        existingTimestamp = null
    )

    private suspend fun sendMessageInternal(
        channelId: String,
        senderId: String,
        senderName: String,
        messageText: String,
        mediaType: MediaType,
        mediaUrl: String,
        replyToId: String?,
        replyToText: String?,
        attachment: ChatAttachment?,
        audioWaveform: List<Int> = emptyList(),
        existingMessageId: String? = null,
        existingTimestamp: Long? = null
    ): AppResult<ChatMessage> = withContext(Dispatchers.IO) {
        if (channelId.isBlank() || senderId.isBlank()) {
            return@withContext AppResult.Error(AppError.ValidationError("message", "بيانات الرسالة غير مكتملة"))
        }
        val local = localProvider()

        val messageId = existingMessageId?.takeIf { it.isNotBlank() }
            ?: channelsCollection.document(channelId).collection("messages").document().id
        val now = existingTimestamp?.takeIf { it > 0L } ?: System.currentTimeMillis()

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
            syncStatus = SyncStatus.PENDING_UPLOAD,
            audioWaveform = audioWaveform,
            updatedAt = now
        )

        try {
            // 1. Immediately store in local cache with SENDING state for instant UI
            local?.insertOrUpdateMessage(initialMsg)

            // 2. Attempt remote send
            val channelRef = channelsCollection.document(channelId)
            val channelSnapshot = channelRef.get().await()
            val channel = channelSnapshot.toObject(ChatChannel::class.java)

            if (channel != null && channel.isUserBlocked(senderId)) {
                local?.updateMessageStatus(channelId, messageId, MessageStatus.FAILED)
                local?.removePendingMessage(messageId)
                return@withContext AppResult.Error(AppError.UnauthorizedError("لا يمكنك إرسال الرسالة لأنك محظور في هذه المحادثة."))
            }

            val confirmedMsg = initialMsg.copy(
                status = MessageStatus.SENT,
                syncStatus = SyncStatus.SYNCED,
                updatedAt = System.currentTimeMillis()
            )

            val displayLast = when (mediaType) {
                MediaType.IMAGE -> "📷 صورة"
                MediaType.VIDEO -> "🎥 فيديو"
                MediaType.AUDIO -> "🎤 تسجيل صوتي"
                MediaType.FILE -> "📎 ${attachment?.fileName.orEmpty().ifBlank { "ملف مرفق" }}"
                MediaType.LOCATION -> "📍 موقع جغرافي"
                MediaType.CALL -> "📞 مكالمة صوتية"
                MediaType.TEXT -> messageText
            }

            val updates = mutableMapOf<String, Any>(
                "lastMessage" to displayLast,
                "lastMessageTime" to now,
                "lastMessageSenderId" to senderId,
                "updatedAt" to System.currentTimeMillis()
            )
            val currentAuthUid = try {
                com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid?.takeIf { it.isNotBlank() }
            } catch (_: Exception) {
                null
            }
            if (currentAuthUid != null) {
                updates["participantUids"] = FieldValue.arrayUnion(currentAuthUid)
            }

            // Write message and update channel atomically in a single batch
            val batch = firestore.batch()
            batch.set(channelRef.collection("messages").document(messageId), confirmedMsg)
            batch.update(channelRef, updates)
            channel?.participants?.forEach { pId ->
                if (pId != senderId && pId.isNotBlank()) {
                    batch.update(channelRef, FieldPath.of("unreadCount", pId), FieldValue.increment(1))
                }
            }
            batch.commit().await()

            // Update local to SENT and remove from pending queue if present
            local?.insertOrUpdateMessage(confirmedMsg)
            local?.removePendingMessage(messageId)
            AppResult.Success(confirmedMsg)
        } catch (e: Exception) {
            Log.e("ChatRepository", "sendMessage failed, saving to offline queue: ${e.message}")
            // Fallback: Queue the same message ID offline without creating a duplicate record
            val offlineMsg = initialMsg.copy(
                status = MessageStatus.PENDING,
                syncStatus = SyncStatus.PENDING_UPLOAD
            )
            local?.queuePendingMessage(offlineMsg)
            AppResult.Success(offlineMsg)
        }
    }

    suspend fun updateMessageMediaUrl(
        channelId: String,
        messageId: String,
        downloadUrl: String,
        newStatus: MessageStatus
    ): AppResult<Unit> = withContext(Dispatchers.IO) {
        if (channelId.isBlank() || messageId.isBlank()) {
            return@withContext AppResult.Error(AppError.ValidationError("message", "بيانات الرسالة غير صالحة"))
        }
        val local = localProvider()
        try {
            // 1. تحديث الكاش المحلي و Room فوراً
            local?.updateMessageMediaUrl(channelId, messageId, downloadUrl, newStatus)

            // 2. تحديث السيرفر في Firestore
            val msgRef = channelsCollection.document(channelId).collection("messages").document(messageId)
            msgRef.update(
                mapOf(
                    "mediaUrl" to downloadUrl,
                    "status" to newStatus.name,
                    "syncStatus" to SyncStatus.SYNCED.name,
                    "updatedAt" to System.currentTimeMillis()
                )
            ).await()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            Log.e("ChatRepository", "updateMessageMediaUrl failed: ${e.message}")
            AppResult.Error(AppError.NetworkError(e))
        }
    }

    suspend fun updateMessageStatus(
        channelId: String,
        messageId: String,
        status: MessageStatus
    ): AppResult<Unit> = withContext(Dispatchers.IO) {
        val local = localProvider()
        try {
            local?.updateMessageStatus(channelId, messageId, status)
            val msgRef = channelsCollection.document(channelId).collection("messages").document(messageId)
            msgRef.update(
                mapOf(
                    "status" to status.name,
                    "updatedAt" to System.currentTimeMillis()
                )
            ).await()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            Log.e("ChatRepository", "updateMessageStatus failed: ${e.message}")
            AppResult.Error(AppError.NetworkError(e))
        }
    }

    suspend fun retryPendingMessages(currentUserId: String): AppResult<Int> = withContext(Dispatchers.IO) {
        val local = localProvider()
        try {
            val pending = local?.getPendingMessages() ?: emptyList()
            var successCount = 0

            for (msg in pending) {
                // Remove from pending queue first; sendMessageInternal will re-queue if it fails again
                local?.removePendingMessage(msg.id)
                val sendResult = sendMessageInternal(
                    channelId = msg.channelId,
                    senderId = msg.senderId,
                    senderName = msg.senderName,
                    messageText = msg.message,
                    mediaType = msg.mediaType,
                    mediaUrl = msg.mediaUrl,
                    replyToId = msg.replyToId,
                    replyToText = msg.replyToText,
                    attachment = msg.attachment,
                    existingMessageId = msg.id,
                    existingTimestamp = msg.timestamp
                )
                if (sendResult is AppResult.Success && sendResult.data.status == MessageStatus.SENT) {
                    successCount++
                }
            }
            AppResult.Success(successCount)
        } catch (e: Exception) {
            AppResult.Error(AppError.NetworkError(e))
        }
    }

    suspend fun markChannelAsRead(channelId: String, currentUserId: String): AppResult<Unit> = withContext(Dispatchers.IO) {
        if (channelId.isBlank() || currentUserId.isBlank()) return@withContext AppResult.Success(Unit)
        val local = localProvider()
        try {
            // Local update first
            local?.markMessagesAsRead(channelId, currentUserId)

            // Remote update
            val channelRef = channelsCollection.document(channelId)
            try {
                val currentAuthUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid?.takeIf { it.isNotBlank() }
                if (currentAuthUid != null) {
                    channelRef.update(
                        FieldPath.of("unreadCount", currentUserId), 0,
                        FieldPath.of("participantUids"), FieldValue.arrayUnion(currentAuthUid)
                    ).await()
                } else {
                    channelRef.update(FieldPath.of("unreadCount", currentUserId), 0).await()
                }
            } catch (e: Exception) {
                // If field doesn't exist yet, ignore
            }

            // Update status of incoming messages to READ (query unread only with batch limit)
            val unreadSnapshot = channelRef.collection("messages")
                .whereEqualTo("isRead", false)
                .limit(30)
                .get().await()
            val unreadDocs = unreadSnapshot.documents.filter { doc ->
                val senderId = doc.getString("senderId") ?: ""
                senderId != currentUserId
            }

            if (unreadDocs.isNotEmpty()) {
                val now = System.currentTimeMillis()
                val batch = firestore.batch()
                for (doc in unreadDocs) {
                    batch.update(
                        doc.reference,
                        mapOf(
                            "status" to MessageStatus.READ.name,
                            "isRead" to true,
                            "readAt" to now,
                            "updatedAt" to now
                        )
                    )
                }
                batch.commit().await()
            }
            AppResult.Success(Unit)
        } catch (e: Exception) {
            Log.e("ChatRepository", "markChannelAsRead error: ${e.message}")
            AppResult.Error(AppError.NetworkError(e))
        }
    }

    suspend fun editMessage(
        channelId: String,
        messageId: String,
        newText: String,
        currentUserId: String
    ): AppResult<Unit> = withContext(Dispatchers.IO) {
        if (channelId.isBlank() || messageId.isBlank() || newText.isBlank()) return@withContext AppResult.Success(Unit)
        val local = localProvider()
        try {
            val resolvedUserId = currentUserId.trim().ifBlank {
                try {
                    com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
                } catch (_: Exception) {
                    ""
                }
            }
            if (resolvedUserId.isBlank()) {
                return@withContext AppResult.Error(AppError.ValidationError("currentUserId", "غير مصرح بتعديل الرسالة بدون معرف المستخدم"))
            }
            val msgRef = channelsCollection.document(channelId).collection("messages").document(messageId)
            val snapshot = msgRef.get().await()
            val message = snapshot.toObject(ChatMessage::class.java)
            if (message == null || message.senderId != resolvedUserId) {
                return@withContext AppResult.Error(AppError.ValidationError("senderId", "غير مصرح لك بتعديل هذه الرسالة (ليست من إرسالك)"))
            }
            val cleanText = newText.trim()
            val now = System.currentTimeMillis()
            msgRef.update(
                mapOf(
                    "message" to cleanText,
                    "isEdited" to true,
                    "editTimestamp" to now,
                    "updatedAt" to now
                )
            ).await()
            local?.insertOrUpdateMessage(
                message.copy(
                    id = messageId,
                    message = cleanText,
                    isEdited = true,
                    editTimestamp = now,
                    updatedAt = now
                )
            )
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.NetworkError(e))
        }
    }

    suspend fun deleteMessage(
        channelId: String,
        messageId: String,
        forEveryone: Boolean,
        currentUserId: String
    ): AppResult<Unit> = withContext(Dispatchers.IO) {
        if (channelId.isBlank() || messageId.isBlank()) return@withContext AppResult.Success(Unit)
        val local = localProvider()
        try {
            // Local update for immediate UI feedback
            local?.deleteMessage(channelId, messageId)

            // Remote update
            val msgRef = channelsCollection.document(channelId).collection("messages").document(messageId)
            val now = System.currentTimeMillis()
            if (forEveryone) {
                msgRef.update(
                    mapOf(
                        "isDeleted" to true,
                        "message" to "تم حذف هذه الرسالة",
                        "updatedAt" to now
                    )
                ).await()
            } else {
                msgRef.update(
                    mapOf(
                        "deletedForUsers" to FieldValue.arrayUnion(currentUserId),
                        "updatedAt" to now
                    )
                ).await()
            }
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.NetworkError(e))
        }
    }

    suspend fun toggleReaction(
        channelId: String,
        messageId: String,
        userId: String,
        emoji: String
    ): AppResult<Unit> = withContext(Dispatchers.IO) {
        val cleanChannelId = channelId.trim()
        val cleanMessageId = messageId.trim()
        val cleanUserId = userId.trim()
        if (cleanChannelId.isBlank() || cleanMessageId.isBlank() || cleanUserId.isBlank()) {
            return@withContext AppResult.Success(Unit)
        }
        try {
            val msgRef = channelsCollection.document(cleanChannelId).collection("messages").document(cleanMessageId)
            val snapshot = msgRef.get().await()
            val currentReactions = snapshot.toObject(ChatMessage::class.java)?.reactions ?: emptyMap()

            val newValue: Any = if (currentReactions[cleanUserId] == emoji || emoji.isBlank()) {
                FieldValue.delete()
            } else {
                emoji
            }

            msgRef.update(
                FieldPath.of("reactions", cleanUserId), newValue,
                FieldPath.of("updatedAt"), System.currentTimeMillis()
            ).await()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.NetworkError(e))
        }
    }

    suspend fun togglePinMessage(
        channelId: String,
        messageId: String,
        isPinned: Boolean
    ): AppResult<Unit> = withContext(Dispatchers.IO) {
        val cleanChannelId = channelId.trim()
        val cleanMessageId = messageId.trim()
        if (cleanChannelId.isBlank() || cleanMessageId.isBlank()) return@withContext AppResult.Success(Unit)
        val local = localProvider()
        try {
            val now = System.currentTimeMillis()
            val msgRef = channelsCollection.document(cleanChannelId).collection("messages").document(cleanMessageId)
            msgRef.update(
                mapOf(
                    "isPinned" to isPinned,
                    "updatedAt" to now
                )
            ).await()
            val cachedList = local?.getMessages(cleanChannelId)
            if (cachedList != null) {
                val updated = cachedList.map { if (it.id == cleanMessageId) it.copy(isPinned = isPinned, updatedAt = now) else it }
                local?.saveMessages(cleanChannelId, updated)
            }
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.NetworkError(e))
        }
    }

    suspend fun syncChannelDelta(channelId: String): AppResult<Int> = withContext(Dispatchers.IO) {
        if (channelId.isBlank()) return@withContext AppResult.Success(0)
        val local = localProvider()
        try {
            val lastSync = local?.getLastSyncTimestamp(channelId) ?: 0L
            val messagesRef = channelsCollection.document(channelId).collection("messages")

            val snapshot = if (lastSync > 0) {
                messagesRef.whereGreaterThan("updatedAt", lastSync).limit(100).get().await()
            } else {
                messagesRef.orderBy("timestamp", Query.Direction.DESCENDING).limit(50).get().await()
            }

            val newMessages = snapshot.documents.mapNotNull { doc ->
                doc.toObject(ChatMessage::class.java)?.copy(id = doc.id)
            }

            if (newMessages.isNotEmpty()) {
                val current = local?.getMessages(channelId) ?: emptyList()
                val mergedMap = LinkedHashMap<String, ChatMessage>()
                current.forEach { mergedMap[it.id] = it }
                newMessages.forEach { mergedMap[it.id] = it }
                val merged = mergedMap.values.sortedBy { it.timestamp }
                local?.saveMessages(channelId, merged)
                local?.setLastSyncTimestamp(channelId, System.currentTimeMillis())
            }

            AppResult.Success(newMessages.size)
        } catch (e: Exception) {
            AppResult.Error(AppError.NetworkError(e))
        }
    }
}
