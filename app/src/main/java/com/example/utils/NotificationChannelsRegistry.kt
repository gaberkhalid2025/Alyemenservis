package com.example.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.util.Log

/**
 * 🔔 المركز الرئيسي الموحد لإدارة قنوات الإشعارات العشر المعتمدة في التطبيق
 */
object NotificationChannelsRegistry {

    private const val TAG = "NotificationChannelsReg"

    // 🎯 القنوات العشر المعتمدة فقط
    const val CHANNEL_ADMIN_CRITICAL = "admin_critical"
    const val CHANNEL_ADMIN_NORMAL = "admin_normal"
    const val CHANNEL_CHAT_MESSAGES_ALIAS = "chat_messages_channel"
    const val CHANNEL_CHAT_VOICE_ALIAS = "chat_voice_channel"
    const val CHANNEL_CHAT_MEDIA_ALIAS = "chat_media_channel"
    const val CHANNEL_URGENT_ALERTS_ALIAS = "urgent_alerts_channel"
    const val CHANNEL_BOOKING_REMINDER_ALIAS = "booking_reminder_channel"
    const val CHANNEL_BOOKINGS_ALERTS = "channel_bookings_alerts"
    const val CHANNEL_INSTANT_SERVICES_PUSH = "instant_services_push_channel"
    const val CHANNEL_YEMEN_SERVICES_FCM = "yemen_services_fcm_channel"

    /**
     * 🎯 إنشاء وتسجيل القنوات العشر المعتمدة دفعة واحدة بأمان عند تشغيل التطبيق
     */
    fun createAll(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        try {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            val channels = listOf(
                // 1. الأدمن الحرجة (مربوطة بالسيرفر Firebase Cloud Functions)
                NotificationChannel(
                    CHANNEL_ADMIN_CRITICAL,
                    "إشعارات الأدمن الحرجة",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "طلبات استعادة كلمة المرور، البلاغات العاجلة، والمدفوعات"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 500, 200, 500)
                    enableLights(true)
                    lightColor = Color.RED
                    setShowBadge(true)
                    lockscreenVisibility = android.app.Notification.VISIBILITY_PRIVATE
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        setBypassDnd(true)
                    }
                },
                // 2. الأدمن العادية
                NotificationChannel(
                    CHANNEL_ADMIN_NORMAL,
                    "إشعارات الأدمن العادية",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "تحديثات، إحصائيات، تنبيهات عامة للإدارة"
                    enableVibration(true)
                    setShowBadge(true)
                },
                // 3. المحادثات المباشرة
                NotificationChannel(
                    CHANNEL_CHAT_MESSAGES_ALIAS,
                    "رسائل المحادثات المباشرة",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "إشعارات الرسائل النصية المباشرة"
                    enableLights(true)
                    enableVibration(true)
                },
                // 4. الرسائل الصوتية
                NotificationChannel(
                    CHANNEL_CHAT_VOICE_ALIAS,
                    "الرسائل الصوتية والمكالمات",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "إشعارات الرسائل الصوتية والاتصالات"
                    enableLights(true)
                    enableVibration(true)
                },
                // 5. الوسائط والمستندات
                NotificationChannel(
                    CHANNEL_CHAT_MEDIA_ALIAS,
                    "الوسائط والمستندات المرفقة",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "إشعارات الصور ومقاطع الفيديو والمستندات المرفقة"
                },
                // 6. الطلبات العاجلة وطوارئ 30 دقيقة
                NotificationChannel(
                    CHANNEL_URGENT_ALERTS_ALIAS,
                    "الطلبات العاجلة والطوارئ 🚨",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "إشعارات فورية فائقة الأولوية للطلبات العاجلة خلال 30 دقيقة"
                    enableLights(true)
                    enableVibration(true)
                },
                // 7. تذكيرات المواعيد والحجوزات
                NotificationChannel(
                    CHANNEL_BOOKING_REMINDER_ALIAS,
                    "تنبيهات المواعيد والحجوزات",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "إشعارات تذكير المستخدمين بمواعيد الحجوزات القادمة"
                    enableVibration(true)
                },
                // 8. إشعارات حالة الحجوزات والخدمات الشاملة
                NotificationChannel(
                    CHANNEL_BOOKINGS_ALERTS,
                    "إشعارات الحجوزات والخدمات الشاملة",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "تنبيهات حالة الحجوزات والمواعيد والخدمات"
                    enableVibration(true)
                    setShowBadge(true)
                },
                // 9. الطلبات والعروض الفورية
                NotificationChannel(
                    CHANNEL_INSTANT_SERVICES_PUSH,
                    "إشعارات الطلبات والعروض الفورية",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "قناة التنبيهات الفورية للعروض والطلبات"
                    enableVibration(true)
                    enableLights(true)
                },
                // 10. إشعارات الخدمة العامة و FCM
                NotificationChannel(
                    CHANNEL_YEMEN_SERVICES_FCM,
                    "إشعارات الخدمة والحجوزات والمحادثات",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "قناة مخصصة للتنبيهات الفورية بتحديثات الحالة والرسائل والطلبات"
                    enableLights(true)
                    enableVibration(true)
                }
            )

            manager.createNotificationChannels(channels)
            Log.d(TAG, "Successfully registered exactly ${channels.size} approved notification channels.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register notification channels: ${e.message}", e)
        }
    }
}
