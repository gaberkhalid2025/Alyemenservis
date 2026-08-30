package com.example.ui.screens.chat

data class ChatMessageUiModel(
    val messageId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val attachmentUrl: String? = null,
    val attachmentType: String? = null // image, audio, file
)

data class ChatChannelUiModel(
    val channelId: String = "",
    val title: String = "",
    val lastMessage: String = "",
    val lastTimestamp: Long = System.currentTimeMillis(),
    val unreadCount: Int = 0,
    val avatarUrl: String = ""
)
