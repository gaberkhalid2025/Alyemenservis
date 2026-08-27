package com.example

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * 🔔 FCMService
 * خدمة استقبال وإدارة الإشعارات السحابية الفورية من Firebase لأنواع الحجوزات، العروض، الرسائل والموافقات.
 */
class FCMService : FirebaseMessagingService() {

    enum class NotificationType(val channelId: String, val channelName: String) {
        BOOKING("yemen_services_bookings", "إشعارات وتحديثات الحجوزات"),
        CHAT("yemen_services_chat", "رسائل المحادثات الفورية"),
        OFFER("yemen_services_offers", "عروض الأسعار والطلبات"),
        APPROVAL("yemen_services_approvals", "موافقات الحسابات والطلبات"),
        GENERAL("yemen_services_general", "التنبيهات العامة والإعلانات")
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "تحديث جديد 🔔"
        val body = remoteMessage.notification?.body ?: remoteMessage.data["body"] ?: "لديك إشعار جديد في تطبيق دليل خدمات اليمن."
        val rawType = remoteMessage.data["type"] ?: "GENERAL"
        val targetScreen = remoteMessage.data["targetScreen"] ?: when (rawType.uppercase()) {
            "BOOKING" -> "BOOKINGS"
            "CHAT" -> "CHAT"
            "OFFER" -> "REQUESTS"
            "APPROVAL" -> "STATUS"
            else -> "MAIN"
        }

        val notifType = try {
            NotificationType.valueOf(rawType.uppercase())
        } catch (e: Exception) {
            NotificationType.GENERAL
        }

        sendLocalNotification(title, body, notifType, targetScreen)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        try {
            val sp = getSharedPreferences("yemen_service_prefs", Context.MODE_PRIVATE)
            val rawUserId = sp.getString("user_id", "") ?: ""
            val userId = if (rawUserId.isNotEmpty() && rawUserId != "guest") com.example.util.SecurityCryptoUtils.decrypt(rawUserId) else rawUserId
            val rawPhone = sp.getString("user_phone", "") ?: ""
            val phone = if (rawPhone.isNotEmpty()) com.example.util.SecurityCryptoUtils.decrypt(rawPhone) else ""
            if (userId.isNotEmpty() && userId != "guest") {
                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                db.collection("registered_users").document(userId).update("fcmToken", token)
                val cleanPhone = phone.trim().replace(" ", "").replace("+", "")
                if (cleanPhone.isNotEmpty()) {
                    db.collection("providers").document(cleanPhone).update("fcmToken", token)
                    db.collection("stores").document(cleanPhone).update("fcmToken", token)
                    db.collection("properties").document(cleanPhone).update("fcmToken", token)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun sendLocalNotification(
        title: String,
        body: String,
        type: NotificationType,
        targetScreen: String = "MAIN"
    ) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                type.channelId,
                type.channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "قناة مخصصة لـ ${type.channelName}"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("target_screen", targetScreen)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, (System.currentTimeMillis() % 10000).toInt(), intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val builder = NotificationCompat.Builder(this, type.channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        notificationManager.notify((System.currentTimeMillis() % 100000).toInt(), builder.build())
    }
}
