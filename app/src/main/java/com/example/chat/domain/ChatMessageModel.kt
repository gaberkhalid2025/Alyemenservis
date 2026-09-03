package com.example.chat.domain

import com.example.data.local.ChatMessageRoomEntity

/**
 * 📦 MessageType
 * Defines the comprehensive supported media and content types for chat messages.
 */
enum class MessageType {
    TEXT,
    IMAGE,
    AUDIO,
    VIDEO,
    PDF,
    DOCUMENT,
    CONTACT,
    LOCATION
}

/**
 * 📊 MessageStatus
 * Lifecycle delivery status of a chat message.
 */
enum class MessageStatus {
    SENDING,
    SENT,
    DELIVERED,
    READ,
    FAILED
}

/**
 * 💬 ChatMessageModel
 * Production-grade immutable domain model for chat messages.
 * Contains rich metadata for multimedia, encryption, quotes/replies, and bidirectional syncing.
 */
data class ChatMessageModel(
    val id: String = "",
    val roomId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderPhoto: String = "",
    val receiverId: String = "",
    val content: String = "",
    val type: MessageType = MessageType.TEXT,
    val status: MessageStatus = MessageStatus.SENDING,
    val mediaUrl: String? = null,
    val thumbnailUrl: String? = null,
    val localFilePath: String? = null,
    val durationMillis: Long? = null,
    val fileSize: Long = 0L,
    val fileName: String = "",
    val mimeType: String = "",
    val contactName: String? = null,
    val contactPhone: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val isEncrypted: Boolean = false,
    val replyToMessageId: String? = null,
    val replyToText: String? = null,
    val replyToSenderName: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val syncStatus: String = "SYNCED"
) {
    /**
     * Converts domain model to Room Database Entity for local SQLite persistence.
     */
    fun toRoomEntity(): ChatMessageRoomEntity {
        return ChatMessageRoomEntity(
            id = id,
            channelId = roomId,
            senderId = senderId,
            senderName = senderName,
            senderPhoto = senderPhoto,
            message = content,
            mediaType = type.name,
            mediaUrl = mediaUrl ?: localFilePath ?: "",
            status = status.name,
            isEncrypted = isEncrypted,
            timestamp = timestamp,
            syncStatus = syncStatus
        )
    }

    /**
     * Converts domain model to Map for Firebase Firestore storage.
     */
    fun toFirestoreMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "roomId" to roomId,
            "senderId" to senderId,
            "senderName" to senderName,
            "senderPhoto" to senderPhoto,
            "receiverId" to receiverId,
            "content" to content,
            "type" to type.name,
            "status" to status.name,
            "mediaUrl" to mediaUrl,
            "thumbnailUrl" to thumbnailUrl,
            "durationMillis" to durationMillis,
            "fileSize" to fileSize,
            "fileName" to fileName,
            "mimeType" to mimeType,
            "contactName" to contactName,
            "contactPhone" to contactPhone,
            "latitude" to latitude,
            "longitude" to longitude,
            "isEncrypted" to isEncrypted,
            "replyToMessageId" to replyToMessageId,
            "replyToText" to replyToText,
            "replyToSenderName" to replyToSenderName,
            "timestamp" to timestamp
        )
    }

    companion object {
        /**
         * Parses Firestore document snapshot map into ChatMessageModel safely.
         */
        @Suppress("UNCHECKED_CAST")
        fun fromFirestore(data: Map<String, Any?>): ChatMessageModel {
            return ChatMessageModel(
                id = data["id"] as? String ?: "",
                roomId = data["roomId"] as? String ?: "",
                senderId = data["senderId"] as? String ?: "",
                senderName = data["senderName"] as? String ?: "",
                senderPhoto = data["senderPhoto"] as? String ?: "",
                receiverId = data["receiverId"] as? String ?: "",
                content = data["content"] as? String ?: "",
                type = try {
                    MessageType.valueOf((data["type"] as? String) ?: "TEXT")
                } catch (e: Exception) {
                    MessageType.TEXT
                },
                status = try {
                    MessageStatus.valueOf((data["status"] as? String) ?: "SENT")
                } catch (e: Exception) {
                    MessageStatus.SENT
                },
                mediaUrl = data["mediaUrl"] as? String,
                thumbnailUrl = data["thumbnailUrl"] as? String,
                durationMillis = (data["durationMillis"] as? Number)?.toLong(),
                fileSize = (data["fileSize"] as? Number)?.toLong() ?: 0L,
                fileName = data["fileName"] as? String ?: "",
                mimeType = data["mimeType"] as? String ?: "",
                contactName = data["contactName"] as? String,
                contactPhone = data["contactPhone"] as? String,
                latitude = (data["latitude"] as? Number)?.toDouble(),
                longitude = (data["longitude"] as? Number)?.toDouble(),
                isEncrypted = data["isEncrypted"] as? Boolean ?: false,
                replyToMessageId = data["replyToMessageId"] as? String,
                replyToText = data["replyToText"] as? String,
                replyToSenderName = data["replyToSenderName"] as? String,
                timestamp = (data["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }

        /**
         * Converts Room Entity to ChatMessageModel.
         */
        fun fromRoomEntity(entity: ChatMessageRoomEntity): ChatMessageModel {
            val msgType = try {
                MessageType.valueOf(entity.mediaType)
            } catch (e: Exception) {
                MessageType.TEXT
            }
            val msgStatus = try {
                MessageStatus.valueOf(entity.status)
            } catch (e: Exception) {
                MessageStatus.SENT
            }
            return ChatMessageModel(
                id = entity.id,
                roomId = entity.channelId,
                senderId = entity.senderId,
                senderName = entity.senderName,
                senderPhoto = entity.senderPhoto,
                content = entity.message,
                type = msgType,
                status = msgStatus,
                mediaUrl = entity.mediaUrl.ifEmpty { null },
                isEncrypted = entity.isEncrypted,
                timestamp = entity.timestamp,
                syncStatus = entity.syncStatus
            )
        }
    }
}

/**
 * 🏷️ ChatRoomModel
 * Represents a conversation channel between participants.
 */
data class ChatRoomModel(
    val id: String = "",
    val participantIds: List<String> = emptyList(),
    val participantNames: Map<String, String> = emptyMap(),
    val participantPhotos: Map<String, String> = emptyMap(),
    val lastMessage: ChatMessageModel? = null,
    val unreadCount: Int = 0,
    val isTypingMap: Map<String, Boolean> = emptyMap(),
    val updatedAt: Long = System.currentTimeMillis()
)

// Backward-compatibility TypeAliases
typealias ChatMessage = ChatMessageModel
typealias ChatRoom = ChatRoomModel
