package com.example

import com.example.utils.*

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FCMService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val data = remoteMessage.data
        val type = data["type"] ?: data["notificationType"] ?: ""

        if (type.equals("CHAT", ignoreCase = true) || data.containsKey("channelId")) {
            val channelId = data["channelId"] ?: ""
            val senderId = data["senderId"] ?: ""
            val senderName = data["senderName"] ?: "رسالة جديدة"
            val messageText = data["message"] ?: remoteMessage.notification?.body ?: "لديك رسالة جديدة"
            val mediaType = data["mediaType"] ?: "TEXT"
            val mediaUrl = data["mediaUrl"]

            ChatNotificationHelper.showChatMessageNotification(
                context = this,
                notificationId = (System.currentTimeMillis() % 100000).toInt(),
                channelId = channelId,
                senderId = senderId,
                senderName = senderName,
                messageText = messageText,
                mediaType = mediaType,
                mediaUrl = mediaUrl
            )
            return
        }

        if (type.equals("URGENT", ignoreCase = true) || data.containsKey("requestCode")) {
            val requestCode = data["requestCode"] ?: ""
            val title = data["title"] ?: remoteMessage.notification?.title ?: "طلب طوارئ عاجل"
            val description = data["description"] ?: remoteMessage.notification?.body ?: ""
            val city = data["city"] ?: ""

            ChatNotificationHelper.showUrgentRequestNotification(
                context = this,
                notificationId = (System.currentTimeMillis() % 100000).toInt(),
                requestCode = requestCode,
                title = title,
                description = description,
                city = city
            )
            return
        }

        val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "تحديث جديد 🔔"
        val body = remoteMessage.notification?.body ?: remoteMessage.data["body"] ?: "لديك إشعار جديد في تطبيق خدمات اليمن."
        val targetScreen = remoteMessage.data["targetScreen"] ?: "MAIN"

        sendLocalNotification(title, body, targetScreen)
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
                val tokenData = mapOf(
                    "token" to token,
                    "phone" to phone,
                    "role" to "CLIENT",
                    "updatedAt" to System.currentTimeMillis()
                )
                db.collection("fcm_tokens").document(userId).set(tokenData)
                db.collection("registered_users").document(userId).update("fcmToken", token)
                val cleanPhone = phone.trim().replace(" ", "").replace("+", "")
                if (cleanPhone.isNotEmpty()) {
                    db.collection("fcm_tokens").document(cleanPhone).set(tokenData)
                    db.collection("providers").document(cleanPhone).update("fcmToken", token)
                    db.collection("stores").document(cleanPhone).update("fcmToken", token)
                    db.collection("properties").document(cleanPhone).update("fcmToken", token)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun sendLocalNotification(title: String, body: String, targetScreen: String = "MAIN") {
        val channelId = "yemen_services_fcm_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "إشعارات الخدمة والحجوزات والمحادثات",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "قناة مخصصة للتنبيهات الفورية بتحديثات الحالة والرسائل والطلبات"
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
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        notificationManager.notify((System.currentTimeMillis() % 100000).toInt(), builder.build())
    }
}
