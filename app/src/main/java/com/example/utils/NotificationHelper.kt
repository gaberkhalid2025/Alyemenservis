// utils/NotificationHelper.kt
package com.example.utils

import android.app.NotificationManager
import android.content.Context
import android.graphics.Color

/**
 * 🔔 مساعد الإشعارات الموحد (مربوط بالـ NotificationChannelsRegistry)
 */
object NotificationHelper {
    
    // ثوابت القنوات الموحدة من السجل الرئيسي (الموجهة للقنوات الـ 10 المعتمدة)
    const val CHANNEL_ADMIN_CRITICAL = NotificationChannelsRegistry.CHANNEL_ADMIN_CRITICAL
    const val CHANNEL_ADMIN_NORMAL = NotificationChannelsRegistry.CHANNEL_ADMIN_NORMAL
    const val CHANNEL_USER_BOOKINGS = NotificationChannelsRegistry.CHANNEL_BOOKINGS_ALERTS
    const val CHANNEL_USER_CHAT = NotificationChannelsRegistry.CHANNEL_CHAT_MESSAGES_ALIAS
    const val CHANNEL_USER_GENERAL = NotificationChannelsRegistry.CHANNEL_YEMEN_SERVICES_FCM
    
    /**
     * 🎯 تفويض إنشاء القنوات للمصدر المركزي الموحد
     */
    fun createNotificationChannels(context: Context) {
        NotificationChannelsRegistry.createAll(context)
    }

    /**
     * 📲 إرسال إشعار محلي فوري على جهاز المستخدم
     */
    fun showLocalNotification(
        context: Context,
        title: String,
        message: String,
        channelId: String = CHANNEL_USER_BOOKINGS,
        notificationId: Int = (System.currentTimeMillis() % 100000).toInt()
    ) {
        try {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            val builder = androidx.core.app.NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(androidx.core.app.NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setDefaults(androidx.core.app.NotificationCompat.DEFAULT_ALL)
                .setColor(Color.parseColor("#00E5FF"))

            manager.notify(notificationId, builder.build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
