package com.example.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

/**
 * 🔔 Centralized Notification Channels Manager
 */
object NotificationChannels {
    
    const val CHAT_MESSAGES = "chat_messages"
    const val CHAT_VOICE = "chat_voice"
    const val CHAT_MEDIA = "chat_media"
    const val URGENT_ALERTS = "urgent_alerts"
    const val BOOKING_REMINDERS = "booking_reminders"
    const val GENERAL = "general_notifications"
    
    fun createAll(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        
        val channels = listOf(
            NotificationChannel(CHAT_MESSAGES, "رسائل المحادثات", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "إشعارات الرسائل الفورية في المحادثات"
            },
            NotificationChannel(CHAT_VOICE, "الرسائل الصوتية", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "إشعارات المكالمات والرسائل الصوتية"
            },
            NotificationChannel(CHAT_MEDIA, "الوسائط والمستندات", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "إشعارات الصور والمستندات المرسلة"
            },
            NotificationChannel(URGENT_ALERTS, "الطلبات العاجلة", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "تنبيهات طلبات الخدمة الفورية والمزادات"
            },
            NotificationChannel(BOOKING_REMINDERS, "تذكيرات الحجوزات", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "تذكيرات بمواعيد الحجوزات وتغيير الحالات"
            },
            NotificationChannel(GENERAL, "إشعارات عامة", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "إشعارات التحديثات والعروض العامة"
            }
        )
        
        manager.createNotificationChannels(channels)
    }
}
