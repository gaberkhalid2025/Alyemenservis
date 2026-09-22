package com.example.utils

import android.content.Context

/**
 * 🔔 Centralized Notification Channels Manager (Delegates to NotificationChannelsRegistry)
 */
object NotificationChannels {
    
    // Approved 10 Channels Mapping
    const val CHAT_MESSAGES = NotificationChannelsRegistry.CHANNEL_CHAT_MESSAGES_ALIAS
    const val CHAT_VOICE = NotificationChannelsRegistry.CHANNEL_CHAT_VOICE_ALIAS
    const val CHAT_MEDIA = NotificationChannelsRegistry.CHANNEL_CHAT_MEDIA_ALIAS
    const val URGENT_ALERTS = NotificationChannelsRegistry.CHANNEL_URGENT_ALERTS_ALIAS
    const val BOOKING_REMINDERS = NotificationChannelsRegistry.CHANNEL_BOOKING_REMINDER_ALIAS
    const val GENERAL = NotificationChannelsRegistry.CHANNEL_YEMEN_SERVICES_FCM
    
    // Admin & Secondary Channels
    const val ADMIN_CRITICAL = NotificationChannelsRegistry.CHANNEL_ADMIN_CRITICAL
    const val ADMIN_NORMAL = NotificationChannelsRegistry.CHANNEL_ADMIN_NORMAL
    const val USER_BOOKINGS = NotificationChannelsRegistry.CHANNEL_BOOKINGS_ALERTS
    const val USER_CHAT = NotificationChannelsRegistry.CHANNEL_CHAT_MESSAGES_ALIAS
    const val USER_GENERAL = NotificationChannelsRegistry.CHANNEL_YEMEN_SERVICES_FCM
    
    // Legacy / Specific Provider Channels
    const val CHAT_MESSAGES_CHANNEL = NotificationChannelsRegistry.CHANNEL_CHAT_MESSAGES_ALIAS
    const val CHAT_VOICE_CHANNEL = NotificationChannelsRegistry.CHANNEL_CHAT_VOICE_ALIAS
    const val CHAT_MEDIA_CHANNEL = NotificationChannelsRegistry.CHANNEL_CHAT_MEDIA_ALIAS
    const val URGENT_ALERTS_CHANNEL = NotificationChannelsRegistry.CHANNEL_URGENT_ALERTS_ALIAS
    const val BOOKING_REMINDER_CHANNEL = NotificationChannelsRegistry.CHANNEL_BOOKING_REMINDER_ALIAS
    
    fun createAll(context: Context) {
        NotificationChannelsRegistry.createAll(context)
    }
}
