package com.example.data.repositories

import android.content.Context
import android.util.Log
import com.example.data.models.*
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ChatRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val channelsCollection = firestore.collection("chat_channels")
    private val presenceCollection = firestore.collection("user_presence")

    /**
     * Get or create a secure chat channel between two participants.
     */
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
    ): ChatChannel {
        val sortedParticipants = listOf(currentUserId.trim(), otherUserId.trim()).sorted()
        val customChannelId = if (type == ChannelType.PRIVATE) {
            "channel_${sortedParticipants[0]}_${sortedParticipants[1]}"
        } else if (relatedEntityId != null) {
            "channel_${type.name.lowercase()}_${relatedEntityId.trim()}"
        } else {
            channelsCollection.document().id
        }

        val docRef = channelsCollection.document(customChannelId)
        val snapshot = docRef.get().await()

        if (snapshot.exists()) {
            val existing = snapshot.toObject(ChatChannel::class.java)
            if (existing != null) {
                // Ensure names and photos are updated
                val updatedNames = existing.participantNames.toMutableMap().apply {
                    put(currentUserId, currentUserName)
                    if (otherUserName.isNotBlank()) put(otherUserId, otherUserName)
                }
                val updatedPhotos = existing.participantPhotos.toMutableMap().apply {
                    put(currentUserId, currentUserPhoto)
                    if (otherUserPhoto.isNotBlank()) put(otherUserId, otherUserPhoto)
                }
                docRef.update(
                    mapOf(
                        "participantNames" to updatedNames,
                        "participantPhotos" to updatedPhotos
                    )
                )
                return existing.copy(
                    id = customChannelId,
                    participantNames = updatedNames,
                    participantPhotos = updatedPhotos
                )
            }
        }

        // Create new channel
        val newChannel = ChatChannel(
            id = customChannelId,
            participants = sortedParticipants,
            participantNames = mapOf(
                currentUserId to currentUserName,
                otherUserId to otherUserName
            ),
            participantPhotos = mapOf(
                currentUserId to currentUserPhoto,
                otherUserId to otherUserPhoto
            ),
            type = type,
            relatedEntityId = relatedEntityId,
            relatedEntityType = relatedEntityType,
            lastMessage = "محادثة جديدة",
            lastMessageTime = System.currentTimeMillis(),
            lastMessageSenderId = currentUserId,
            unreadCount = mapOf(
                currentUserId to 0,
                otherUserId to 0
            ),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        docRef.set(newChannel).await()
        return newChannel
    }

    /**
     * Fetch a single channel by its ID.
     */
    suspend fun getChannelById(channelId: String): ChatChannel? {
        if (channelId.isBlank()) return null
        return try {
            val snapshot = channelsCollection.document(channelId).get().await()
            if (snapshot.exists()) {
                snapshot.toObject(ChatChannel::class.java)?.copy(id = snapshot.id)
            } else null
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error getting channel $channelId: ${e.message}")
            null
        }
    }

    /**
     * Get channels for a user using strict participants security filter.
     */
    fun getUserChannels(userId: String): Flow<List<ChatChannel>> = callbackFlow {
        val cleanUserId = userId.trim()
        if (cleanUserId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener: ListenerRegistration = channelsCollection
            .whereArrayContains("participants", cleanUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ChatRepository", "Error fetching channels: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(ChatChannel::class.java)?.copy(id = doc.id)
                }?.sortedByDescending { it.lastMessageTime } ?: emptyList()

                trySend(list)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Real-time stream of messages in a channel.
     */
    fun getChannelMessages(channelId: String, currentUserId: String): Flow<List<ChatMessage>> = callbackFlow {
        if (channelId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val messagesRef = channelsCollection.document(channelId).collection("messages")
        val listener = messagesRef
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ChatRepository", "Error fetching messages: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val messages = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(ChatMessage::class.java)?.copy(id = doc.id)
                }?.filter { msg ->
                    !msg.deletedForUsers.contains(currentUserId)
                } ?: emptyList()

                trySend(messages)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Send a new message.
     */
    suspend fun sendMessage(
        channelId: String,
        senderId: String,
        senderName: String,
        messageText: String,
        mediaType: MediaType = MediaType.TEXT,
        mediaUrl: String = "",
        replyToId: String? = null,
        replyToText: String? = null
    ): Boolean {
        return try {
            val channelRef = channelsCollection.document(channelId)
            val channelSnapshot = channelRef.get().await()
            val channel = channelSnapshot.toObject(ChatChannel::class.java) ?: return false

            // Check if sender is blocked
            val isSenderBlocked = channel.isBlocked[senderId] ?: false
            if (isSenderBlocked) return false

            val messageId = channelRef.collection("messages").document().id
            val now = System.currentTimeMillis()

            val chatMsg = ChatMessage(
                id = messageId,
                channelId = channelId,
                senderId = senderId,
                senderName = senderName,
                message = messageText,
                mediaType = mediaType,
                mediaUrl = mediaUrl,
                replyToId = replyToId,
                replyToText = replyToText,
                status = MessageStatus.SENT,
                timestamp = now,
                isDeleted = false
            )

            // Save message
            channelRef.collection("messages").document(messageId).set(chatMsg).await()

            // Update channel last message and increment unread for other participants
            val updatedUnread = channel.unreadCount.toMutableMap()
            channel.participants.forEach { pId ->
                if (pId != senderId) {
                    val currentUnread = updatedUnread[pId] ?: 0
                    updatedUnread[pId] = currentUnread + 1
                }
            }

            val displayLastMessage = when (mediaType) {
                MediaType.IMAGE -> "📷 صورة"
                MediaType.VIDEO -> "🎥 فيديو"
                MediaType.AUDIO -> "🎤 تسجيل صوتي"
                MediaType.FILE -> "📎 ملف مرفق"
                MediaType.TEXT -> messageText
            }

            channelRef.update(
                mapOf(
                    "lastMessage" to displayLastMessage,
                    "lastMessageTime" to now,
                    "lastMessageSenderId" to senderId,
                    "unreadCount" to updatedUnread,
                    "updatedAt" to now
                )
            ).await()

            true
        } catch (e: Exception) {
            Log.e("ChatRepository", "Failed to send message: ${e.message}")
            false
        }
    }

    /**
     * Mark channel messages as read for current user efficiently.
     */
    suspend fun markChannelAsRead(channelId: String, currentUserId: String) {
        if (channelId.isBlank()) return
        try {
            val channelRef = channelsCollection.document(channelId)
            channelRef.update("unreadCount.$currentUserId", 0).await()

            // Update status only for messages that are not sent by current user and not yet READ
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
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error marking as read: ${e.message}")
        }
    }

    /**
     * Set typing status for a participant.
     */
    suspend fun setTyping(channelId: String, userId: String, isTyping: Boolean) {
        try {
            channelsCollection.document(channelId).update("isTyping.$userId", isTyping).await()
        } catch (e: Exception) {
            // silent fail
        }
    }

    /**
     * Block/Unblock user in channel.
     */
    suspend fun toggleBlockUser(channelId: String, userIdToBlock: String, isBlocked: Boolean) {
        try {
            channelsCollection.document(channelId).update("isBlocked.$userIdToBlock", isBlocked).await()
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error toggling block: ${e.message}")
        }
    }

    /**
     * Delete message (for everyone or for current user).
     */
    suspend fun deleteMessage(channelId: String, messageId: String, forEveryone: Boolean, currentUserId: String) {
        try {
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
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error deleting message: ${e.message}")
        }
    }

    /**
     * Set user online presence.
     */
    suspend fun setUserPresence(userId: String, isOnline: Boolean) {
        if (userId.isBlank()) return
        try {
            val presence = UserPresence(
                userId = userId,
                isOnline = isOnline,
                lastSeen = System.currentTimeMillis()
            )
            presenceCollection.document(userId).set(presence, SetOptions.merge()).await()
        } catch (e: Exception) {
            // silent
        }
    }

    /**
     * Stream other user presence.
     */
    fun getUserPresence(userId: String): Flow<UserPresence?> = callbackFlow {
        if (userId.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val listener = presenceCollection.document(userId).addSnapshotListener { snapshot, _ ->
            val presence = snapshot?.toObject(UserPresence::class.java)
            trySend(presence)
        }

        awaitClose { listener.remove() }
    }
}
