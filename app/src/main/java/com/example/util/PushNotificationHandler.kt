package com.example.util

import android.content.Context
import android.content.Intent
import androidx.annotation.Keep
import com.example.MainActivity
import com.google.firebase.messaging.RemoteMessage
import java.util.UUID

@Keep
data class PushNotificationItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val message: String = "",
    val targetScreen: String = "home",
    val data: Map<String, String> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

/**
 * 📲 PushNotificationHandler
 * 
 * المساعد المخصص لمعالجة إشعارات Firebase Cloud Messaging (FCM) الواردة، 
 * وتمريرها مباشرة لمساعد `NotificationManager` الموحد لحفظها وتنبيه المستخدم.
 */
class PushNotificationHandler(private val context: Context) {

    private val unifiedNotificationManager by lazy { NotificationManager(context) }

    /**
     * معالجة رسالة FCM فور وصولها
     */
    fun handlePushNotification(remoteMessage: RemoteMessage) {
        val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "إشعار جديد"
        val body = remoteMessage.notification?.body ?: remoteMessage.data["message"] ?: ""

        unifiedNotificationManager.handlePushNotification(title, body, remoteMessage.data)
    }

    /**
     * عرض الإشعار للشاشة
     */
    fun displayNotification(ctx: Context, notification: PushNotificationItem) {
        unifiedNotificationManager.showLocalNotification(
            title = notification.title,
            body = notification.message
        )
    }

    /**
     * التوجيه نحو شاشة محددة
     */
    fun navigateToScreen(ctx: Context, screen: String, data: Map<String, String>) {
        val intent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("target_screen", screen)
            for ((k, v) in data) {
                putExtra(k, v)
            }
        }
        ctx.startActivity(intent)
    }
}
