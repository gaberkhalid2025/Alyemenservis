package com.example

import com.example.utils.*

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
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
        
        // الخطوة 1: حفظ التوكن في SharedPreferences فوراً (آمن 100%)
        try {
            val sp = getSharedPreferences("yemen_service_prefs", Context.MODE_PRIVATE)
            sp.edit().putString("fcm_token_backup", token).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        // الخطوة 2: مزامنة مع Firestore بعد تأخير (لضمان تهيئة Firebase)
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                // التحقق من تهيئة Firebase
                if (FirebaseApp.getApps(this).isEmpty()) {
                    try {
                        FirebaseApp.initializeApp(this)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        return@postDelayed
                    }
                }
                
                val db = FirebaseFirestore.getInstance()
                val sp = getSharedPreferences("yemen_service_prefs", Context.MODE_PRIVATE)
                
                // جلب بيانات المستخدم
                val rawUserId = sp.getString("user_id", "") ?: ""
                val userId = if (rawUserId.isNotEmpty() && rawUserId != "guest") {
                    try { SecurityCryptoUtils.decrypt(rawUserId) } catch (e: Exception) { rawUserId }
                } else rawUserId
                
                if (userId.isEmpty() || userId == "guest") {
                    return@postDelayed
                }
                
                val rawPhone = sp.getString("user_phone", "") ?: ""
                val phone = if (rawPhone.isNotEmpty()) {
                    try { SecurityCryptoUtils.decrypt(rawPhone) } catch (e: Exception) { rawPhone }
                } else rawPhone
                val cleanPhone = phone.trim().replace(" ", "").replace("+", "")
                
                // إنشاء المعاملة
                val batch = db.batch()
                val now = System.currentTimeMillis()
                
                // 1. تحديث fcm_tokens
                val tokenData = mapOf(
                    "token" to token,
                    "phone" to cleanPhone,
                    "role" to "CLIENT",
                    "updatedAt" to now
                )
                val tokenRef = db.collection("fcm_tokens").document(userId)
                batch.set(tokenRef, tokenData)
                
                // 2. تحديث registered_users
                try {
                    val userRef = db.collection("registered_users").document(userId)
                    batch.update(userRef, "fcmToken", token)
                } catch (e: Exception) {
                    // قد لا يكون المستند موجوداً، نتجاوز
                }
                
                // 3. تحديث providers, stores, properties (إذا كان الرقم موجوداً)
                if (cleanPhone.isNotEmpty() && cleanPhone.length >= 7) {
                    try {
                        val providerRef = db.collection("providers").document(cleanPhone)
                        batch.update(providerRef, "fcmToken", token)
                    } catch (e: Exception) { /* تجاهل */ }
                    
                    try {
                        val storeRef = db.collection("stores").document(cleanPhone)
                        batch.update(storeRef, "fcmToken", token)
                    } catch (e: Exception) { /* تجاهل */ }
                    
                    try {
                        val propRef = db.collection("properties").document(cleanPhone)
                        batch.update(propRef, "fcmToken", token)
                    } catch (e: Exception) { /* تجاهل */ }
                }
                
                // تنفيذ المعاملة مع معالجة الأخطاء
                batch.commit()
                    .addOnSuccessListener {
                        // نجاح المزامنة
                    }
                    .addOnFailureListener { e ->
                        // فشل المزامنة - سنحاول مرة أخرى في المرة القادمة
                        e.printStackTrace()
                    }
                    
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, 3000) // تأخير 3 ثواني لضمان تهيئة Firebase بالكامل
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
