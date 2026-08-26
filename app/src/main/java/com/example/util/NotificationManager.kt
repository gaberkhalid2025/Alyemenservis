package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager as AndroidNotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.data.local.ScheduledNotificationDatabase
import com.example.data.local.ScheduledNotificationEntity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * 🔔 NotificationManager - مدير الإشعارات الموحد لمنصة دليل خدمات اليمن
 * 
 * الميزات:
 * 1. توحيد منطق الإشعارات الفورية (Push Notifications) والإشعارات المحلية (Local Notifications).
 * 2. جدولة الإشعارات التذكيرية وحفظها في Room Database (`ScheduledNotificationDatabase`).
 * 3. معالجة وتصفية الإشعارات وتخزين السجل في Firebase Firestore.
 */
class NotificationManager(private val context: Context) {

    private val androidNotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as? AndroidNotificationManager

    private val scheduledDb by lazy { ScheduledNotificationDatabase.getInstance(context) }
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val firestore by lazy { FirebaseFirestore.getInstance() }

    init {
        createChannels()
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                AppSetup.DEFAULT_NOTIFICATION_CHANNEL_ID,
                "تنبيهات الخدمات والطلبات",
                AndroidNotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "إشعارات الطلبات المباشرة والحجوزات"
                enableVibration(true)
            }

            val chatChannel = NotificationChannel(
                AppSetup.CHAT_NOTIFICATION_CHANNEL_ID,
                "المحادثات المباشرة",
                AndroidNotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "إشعارات الرسائل بين العملاء والفنيين"
                enableVibration(true)
            }

            androidNotificationManager?.createNotificationChannel(serviceChannel)
            androidNotificationManager?.createNotificationChannel(chatChannel)
        }
    }

    /**
     * إظهار إشعار محلي فوراً على الجهاز
     */
    fun showLocalNotification(
        title: String,
        body: String,
        channelId: String = AppSetup.DEFAULT_NOTIFICATION_CHANNEL_ID,
        notificationId: Int = (System.currentTimeMillis() % 100000).toInt()
    ) {
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        androidNotificationManager?.notify(notificationId, builder.build())
    }

    /**
     * جدولة إشعار تذكيري وحفظه في Room Database
     */
    suspend fun scheduleNotification(
        id: String = UUID.randomUUID().toString(),
        title: String,
        message: String,
        scheduledTime: Long,
        type: String = "BOOKING_REMINDER",
        bookingId: String = ""
    ) {
        val entity = ScheduledNotificationEntity(
            id = id,
            title = title,
            message = message,
            scheduledTime = scheduledTime,
            isActive = true,
            type = type,
            bookingId = bookingId
        )
        scheduledDb.scheduledNotificationDao().insert(entity)
    }

    /**
     * إلغاء إشعار مجدول
     */
    suspend fun cancelNotification(notificationId: String) {
        scheduledDb.scheduledNotificationDao().deleteById(notificationId)
    }

    /**
     * الحصول على تدفق الإشعارات المجدولة النشطة
     */
    fun getActiveScheduledNotifications(): Flow<List<ScheduledNotificationEntity>> {
        return scheduledDb.scheduledNotificationDao().getActiveNotificationsFlow()
    }

    /**
     * معالجة الإشعارات الفورية القادمة من FCM (Push Notifications)
     */
    fun handlePushNotification(title: String?, body: String?, dataMap: Map<String, String> = emptyMap()) {
        val cleanTitle = title ?: dataMap["title"] ?: "إشعار جديد 🔔"
        val cleanBody = body ?: dataMap["body"] ?: "لديك تحديث جديد في منصة دليل خدمات اليمن"
        val type = dataMap["type"] ?: "GENERAL"

        showLocalNotification(cleanTitle, cleanBody)

        scope.launch {
            saveNotificationToCloud(
                id = dataMap["id"] ?: UUID.randomUUID().toString(),
                title = cleanTitle,
                body = cleanBody,
                type = type
            )
        }
    }

    /**
     * حفظ الإشعار في سجل Firestore
     */
    suspend fun saveNotificationToCloud(
        id: String = UUID.randomUUID().toString(),
        title: String,
        body: String,
        type: String = "GENERAL",
        userId: String = ""
    ) {
        try {
            val payload = hashMapOf<String, Any?>(
                "id" to id,
                "title" to title,
                "body" to body,
                "type" to type,
                "userId" to userId,
                "timestamp" to System.currentTimeMillis(),
                "isRead" to false
            )
            firestore.collection("user_notifications").document(id).set(payload)
        } catch (e: Exception) {
            // صامت
        }
    }
}
