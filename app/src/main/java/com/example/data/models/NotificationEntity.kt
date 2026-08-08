package com.example.data

import androidx.annotation.Keep

@Keep
data class NotificationEntity(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val targetType: String = "ALL", // "ALL", "REGION", "CATEGORY", "USER", "PROVIDER", "SUPERVISOR"
    val targetValue: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val expiryTimestamp: Long = 0L,
    val scheduledTime: Long = 0L,
    val customerPhone: String = "",
    val customerName: String = "",
    val notificationType: String = "NORMAL", // "IMPORTANT", "NORMAL", "REGISTRATION_APPROVED", "BOOKING_CONFIRMED", etc.
    val channel: String = "IN_APP", // "IN_APP", "FCM", "WHATSAPP", "SMS", "TELEGRAM"
    val isRead: Boolean = false
)
