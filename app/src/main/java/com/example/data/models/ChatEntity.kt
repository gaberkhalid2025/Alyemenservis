package com.example.data

import androidx.annotation.Keep

@Keep
enum class AttachmentType {
    EXCEL, CSV, PDF, IMAGE, JSON
}

@Keep
data class ProductAttachment(
    val id: String = java.util.UUID.randomUUID().toString(),
    val userId: String = "",
    val type: String = "PDF", // "EXCEL", "CSV", "PDF", "IMAGE", "JSON"
    val url: String = "",
    val fileName: String = "",
    val size: Long = 0,
    val mimeType: String = "",
    val uploadedAt: Long = System.currentTimeMillis(),
    val isPublic: Boolean = true
) {
    companion object {
        fun parseList(jsonStr: String): List<ProductAttachment> {
            if (jsonStr.isBlank()) return emptyList()
            return try {
                jsonStr.split(";;").filter { it.isNotBlank() }.map { chunk ->
                    val parts = chunk.split("||")
                    ProductAttachment(
                        id = parts.getOrElse(0) { java.util.UUID.randomUUID().toString() },
                        userId = parts.getOrElse(1) { "" },
                        type = parts.getOrElse(2) { "PDF" },
                        url = parts.getOrElse(3) { "" },
                        fileName = parts.getOrElse(4) { "" },
                        size = parts.getOrElse(5) { "0" }.toLongOrNull() ?: 0L,
                        mimeType = parts.getOrElse(6) { "" },
                        uploadedAt = parts.getOrElse(7) { "0" }.toLongOrNull() ?: System.currentTimeMillis(),
                        isPublic = parts.getOrElse(8) { "true" }.toBoolean()
                    )
                }
            } catch (e: Exception) {
                emptyList()
            }
        }

        fun serializeList(list: List<ProductAttachment>): String {
            return list.joinToString(";;") { item ->
                listOf(
                    item.id,
                    item.userId,
                    item.type,
                    item.url,
                    item.fileName,
                    item.size.toString(),
                    item.mimeType,
                    item.uploadedAt.toString(),
                    item.isPublic.toString()
                ).joinToString("||")
            }
        }
    }
}

@Keep
data class ChatMessageEntity(
    val id: String = "",
    val senderId: String = "guest",
    val senderName: String = "",
    val senderPhone: String = "",
    val recipientId: String = "",
    val message: String = "",
    val timestamp: Long = 0L,
    val mediaType: String = "TEXT", // "TEXT", "AUDIO", "IMAGE", "VIDEO", "FILE", "CALL"
    val mediaUrl: String = "",
    val audioDurationSec: Int = 0,
    val status: String = "SENT", // "SENT", "DELIVERED", "READ"
    val statusTime: Long = 0L,
    val imageUrl: String = "",
    val replyToId: String = "",
    val replyToText: String = "",
    val replyToSender: String = "",
    val reactions: String = "", // e.g. "👍,❤️"
    val isDeleted: Boolean = false,
    val deletedBy: String = "",
    val fileName: String = "",
    val fileSize: Long = 0L,
    val fileType: String = "",
    val forwardedFrom: String = "",
    val readAt: Long = 0L
)

@Keep
data class ChatChannelEntity(
    val id: String = "",
    val channelType: String = "PROVIDER", // "PROVIDER", "STORE", "PROPERTY", "RESTAURANT", "ADMIN", "SUPERVISOR", "CATEGORY"
    val targetId: String = "",
    val targetName: String = "",
    val targetPhone: String = "",
    val targetCategory: String = "",
    val userName: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val customerId: String = "",
    val lastMessage: String = "",
    val lastMessageTime: Long = 0L,
    val isBlocked: Boolean = false,
    val isProvider: Boolean = false,
    val timestamp: Long = 0L,
    val unreadCountUser: Int = 0,
    val unreadCountTarget: Int = 0,
    val providerId: String = "",
    val providerName: String = "",
    val providerPhoto: String = "",
    val clientId: String = "",
    val clientName: String = "",
    val clientPhoto: String = "",
    val unreadCount: Int = 0,
    val messages: List<ChatMessageEntity> = emptyList()
)

@Keep
data class CallEntity(
    val id: String = "",
    val providerId: String = "",
    val providerName: String = "",
    val callerName: String = "",
    val timestamp: Long = 0L
)
