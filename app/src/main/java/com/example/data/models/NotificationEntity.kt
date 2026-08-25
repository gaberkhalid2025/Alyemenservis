package com.example.data

import androidx.annotation.Keep

@Keep
data class NotificationEntity(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val targetType: String = "ALL", // "ALL", "REGION", "CATEGORY", "USER", "PROVIDER", "SUPERVISOR", "ROLE", "SPECIFIC_USER"
    val targetValue: String = "",
    val targetAudience: String = "ADMIN_ONLY", // "ADMIN_ONLY", "SPECIFIC_ROLES", "SPECIFIC_USERS", "ALL_REGISTERED_USERS", "ALL"
    val targetRoles: List<String> = emptyList(), // "TECHNICIAN", "STORE", "MEDICAL", "RESTAURANT", "REAL_ESTATE", "ADMIN", "USER"
    val targetUserIds: List<String> = emptyList(),
    val senderId: String = "SYSTEM",
    val senderName: String = "النظام",
    val dedupKey: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val expiryTimestamp: Long = 0L,
    val scheduledTime: Long = 0L,
    val customerPhone: String = "",
    val customerName: String = "",
    val notificationType: String = "NORMAL", // "BOOKING", "MESSAGE", "SYSTEM", "ADMIN", "REGISTRATION_APPROVED", "SPECIAL_OFFER", "NORMAL"
    val channel: String = "IN_APP", // "IN_APP", "FCM", "WHATSAPP", "SMS", "TELEGRAM"
    val isRead: Boolean = false
) {
    // Validation helper to reject dummy, incomplete or corrupted notifications
    fun isValid(): Boolean {
        return id.isNotBlank() &&
               title.trim().isNotEmpty() &&
               message.trim().isNotEmpty() &&
               senderId.trim().isNotEmpty() &&
               notificationType.trim().isNotEmpty()
    }
}

