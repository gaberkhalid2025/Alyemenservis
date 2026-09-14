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

typealias ChatMessageEntity = com.example.data.models.ChatMessage
typealias ChatChannelEntity = com.example.data.models.ChatChannel

@Keep
data class CallEntity(
    val id: String = "",
    val providerId: String = "",
    val providerName: String = "",
    val callerName: String = "",
    val timestamp: Long = 0L
)
