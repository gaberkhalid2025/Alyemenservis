package com.example.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import com.example.MainActivity
import com.example.ui.AppScreens
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * 💬 ChatNotificationHelper
 * إدارة إشعارات FCM والمحادثات الغنية مع دعم الرد المباشر (Direct Reply)
 * والقنوات المنفصلة للرسائل النصية، الصوتية، الصور، والطلبات العاجلة.
 */
object ChatNotificationHelper {

    const val CHANNEL_MESSAGES = "chat_messages_channel"
    const val CHANNEL_VOICE = "chat_voice_channel"
    const val CHANNEL_MEDIA = "chat_media_channel"
    const val CHANNEL_URGENT = "urgent_alerts_channel"

    const val KEY_TEXT_REPLY = "key_text_reply"
    const val EXTRA_CHANNEL_ID = "extra_channel_id"
    const val EXTRA_SENDER_ID = "extra_sender_id"
    const val EXTRA_SENDER_NAME = "extra_sender_name"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val messagesChannel = NotificationChannel(
                CHANNEL_MESSAGES,
                "رسائل المحادثات",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "إشعارات الرسائل النصية المباشرة"
                enableLights(true)
                enableVibration(true)
            }

            val voiceChannel = NotificationChannel(
                CHANNEL_VOICE,
                "الرسائل الصوتية والمكالمات",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "إشعارات الرسائل الصوتية والاتصالات"
                enableLights(true)
                enableVibration(true)
            }

            val mediaChannel = NotificationChannel(
                CHANNEL_MEDIA,
                "الوسائط والمستندات",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "إشعارات الصور ومقاطع الفيديو والمستندات المرفقة"
            }

            val urgentChannel = NotificationChannel(
                CHANNEL_URGENT,
                "الطلبات العاجلة والطوارئ 🚨",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "إشعارات فورية فائقة الأولوية للطلبات العاجلة خلال 30 دقيقة"
                enableLights(true)
                enableVibration(true)
            }

            notificationManager.createNotificationChannels(listOf(messagesChannel, voiceChannel, mediaChannel, urgentChannel))
        }
    }

    fun showChatMessageNotification(
        context: Context,
        notificationId: Int,
        channelId: String,
        senderId: String,
        senderName: String,
        messageText: String,
        mediaType: String = "TEXT",
        mediaUrl: String? = null,
        timestamp: Long = System.currentTimeMillis()
    ) {
        createNotificationChannels(context)

        val targetChannel = when (mediaType.uppercase()) {
            "AUDIO", "VOICE", "CALL" -> CHANNEL_VOICE
            "IMAGE", "VIDEO", "FILE", "DOCUMENT" -> CHANNEL_MEDIA
            "URGENT" -> CHANNEL_URGENT
            else -> CHANNEL_MESSAGES
        }

        // 1. Intent for opening direct chat screen in MainActivity
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_CHANNEL_ID, channelId)
            putExtra(EXTRA_SENDER_ID, senderId)
            putExtra(EXTRA_SENDER_NAME, senderName)
            if (channelId.isNotBlank()) {
                putExtra("openChatChannelId", channelId)
            }
        }

        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 2. Person and MessagingStyle
        val sender = Person.Builder()
            .setName(senderName.ifBlank { "مستخدم" })
            .setKey(senderId)
            .build()

        val messagingStyle = NotificationCompat.MessagingStyle(Person.Builder().setName("أنا").build())
            .setConversationTitle(senderName)
            .addMessage(messageText, timestamp, sender)

        val replyAction = if (channelId.isNotBlank()) {
            val remoteInput = RemoteInput.Builder(KEY_TEXT_REPLY)
                .setLabel("اكتب ردك هنا...")
                .build()
            val replyIntent = Intent(context, DirectReplyReceiver::class.java).apply {
                action = DirectReplyReceiver.ACTION_DIRECT_REPLY
                putExtra(EXTRA_CHANNEL_ID, channelId)
                putExtra(EXTRA_SENDER_ID, senderId)
                putExtra(EXTRA_SENDER_NAME, senderName)
                putExtra(DirectReplyReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            }
            val replyFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val replyPendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId + 1000,
                replyIntent,
                replyFlags
            )
            NotificationCompat.Action.Builder(
                android.R.drawable.ic_menu_send,
                "رد سريع",
                replyPendingIntent
            ).addRemoteInput(remoteInput)
                .setAllowGeneratedReplies(true)
                .build()
        } else null

        val builder = NotificationCompat.Builder(context, targetChannel)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle(senderName)
            .setContentText(messageText)
            .setStyle(messagingStyle)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .apply {
                if (replyAction != null) addAction(replyAction)
            }

        val appContext = context.applicationContext
        if (mediaType.equals("IMAGE", ignoreCase = true) && !mediaUrl.isNullOrBlank()) {
            CoroutineScope(Dispatchers.IO).launch {
                val bitmap = downloadBitmap(mediaUrl)
                if (bitmap != null) {
                    builder.setLargeIcon(bitmap)
                        .setStyle(
                            NotificationCompat.BigPictureStyle()
                                .bigPicture(bitmap)
                                .setSummaryText(messageText)
                        )
                }
                try {
                    NotificationManagerCompat.from(appContext).notify(notificationId, builder.build())
                } catch (_: SecurityException) {
                }
            }
        } else {
            try {
                NotificationManagerCompat.from(appContext).notify(notificationId, builder.build())
            } catch (_: SecurityException) {
            }
        }
    }

    fun showUrgentRequestNotification(
        context: Context,
        notificationId: Int,
        requestCode: String,
        title: String,
        description: String,
        city: String,
        priceHint: String = ""
    ) {
        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("target_screen", AppScreens.INSTANT_REQUESTS_VIEW)
            if (requestCode.isNotBlank()) {
                putExtra("openUrgentRequestCode", requestCode)
            }
            putExtra("EXTRA_NAV_TARGET", "instant_requests")
            putExtra("EXTRA_REQUEST_CODE", requestCode)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_URGENT)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle("🚨 طلب طوارئ جديد: $title ($requestCode)")
            .setContentText("$city - $description")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("📍 المدينة/الحي: $city\n📝 التفاصيل: $description\n⏱️ مهلة الاستجابة: 30 دقيقة\n$priceHint")
            )
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (ignored: Exception) {}
    }

    private fun downloadBitmap(urlStr: String): Bitmap? {
        return try {
            val url = URL(urlStr)
            val connection = url.openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connectTimeout = 3000
            connection.readTimeout = 3000
            connection.connect()
            val input: InputStream = connection.inputStream
            BitmapFactory.decodeStream(input)
        } catch (e: Exception) {
            null
        }
    }
}
