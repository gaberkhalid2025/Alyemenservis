package com.example.data.models

import androidx.annotation.Keep
import java.io.Serializable

@Keep
enum class ChannelType {
    PRIVATE, SUPPORT, GROUP
}

@Keep
enum class MediaType {
    TEXT, IMAGE, VIDEO, AUDIO, FILE
}

@Keep
enum class MessageStatus {
    SENT, DELIVERED, READ
}

@Keep
enum class ChatFilterCategory {
    ALL, UNREAD, TECHNICIANS, STORES, RESTAURANTS, SUPPORT
}

@Keep
data class ChatChannel(
    val id: String = "",
    val participants: List<String> = emptyList(),
    val participantNames: Map<String, String> = emptyMap(),
    val participantPhotos: Map<String, String> = emptyMap(),
    val participantRoles: Map<String, String> = emptyMap(),
    val type: ChannelType = ChannelType.PRIVATE,
    val relatedEntityId: String? = null,
    val relatedEntityType: String? = null, // BOOKING, URGENT_REQUEST, SUPPORT, STORE, RESTAURANT, TECHNICIAN
    val lastMessage: String = "",
    val lastMessageTime: Long = 0L,
    val lastMessageSenderId: String = "",
    val unreadCount: Map<String, Int> = emptyMap(),
    val isBlocked: Map<String, Boolean> = emptyMap(),
    val isTyping: Map<String, Boolean> = emptyMap(),
    val deletedForUsers: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) : Serializable

@Keep
data class ChatMessage(
    val id: String = "",
    val channelId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val message: String = "",
    val mediaType: MediaType = MediaType.TEXT,
    val mediaUrl: String = "",
    val mediaDurationSeconds: Int = 0,
    val replyToId: String? = null,
    val replyToText: String? = null,
    val status: MessageStatus = MessageStatus.SENT,
    val timestamp: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val deletedForUsers: List<String> = emptyList()
) : Serializable

@Keep
data class UserPresence(
    val userId: String = "",
    val isOnline: Boolean = false,
    val lastSeen: Long = System.currentTimeMillis()
) : Serializable

