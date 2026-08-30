package com.example.chat.domain

enum class MessageStatus { SENDING, SENT, DELIVERED, READ, FAILED }
enum class MessageType { TEXT, IMAGE, AUDIO, DOCUMENT }

data class ChatMessage(
    val id: String = "",
    val roomId: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val content: String = "",
    val type: MessageType = MessageType.TEXT,
    val mediaUrl: String? = null,
    val thumbnailUrl: String? = null,
    val durationMillis: Long? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val status: MessageStatus = MessageStatus.SENDING
)

data class ChatRoom(
    val id: String = "",
    val participantIds: List<String> = emptyList(),
    val lastMessage: ChatMessage? = null,
    val unreadCount: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
)
