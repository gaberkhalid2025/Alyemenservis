package com.example.chat.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import com.example.MainActivity

/**
 * 🔔 ChatNotificationHelper
 * Production-ready notification manager for Chat messages supporting:
 * - High-priority NotificationChannels on Android 8.0+ (Oreo)
 * - Android Direct Reply (RemoteInput) right from notification shade
 * - Notification Stacking / Summary grouping
 * - Deep linking to active chat room
 */
class ChatNotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID_MESSAGES = "yemen_chat_messages_channel"
        const val CHANNEL_ID_VOICE = "yemen_chat_voice_channel"
        const val KEY_TEXT_REPLY = "key_text_reply"
        const val ACTION_DIRECT_REPLY = "com.example.chat.ACTION_DIRECT_REPLY"
        const val NOTIFICATION_GROUP_KEY = "com.example.chat.MESSAGES_GROUP"
        const val SUMMARY_NOTIFICATION_ID = 1001
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Text Messages Channel
            val messagesChannel = NotificationChannel(
                CHANNEL_ID_MESSAGES,
                "رسائل المحادثات الفورية",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "إشعارات الرسائل والمحادثات المباشرة بين العملاء والفنيين"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }

            // Voice Notes Channel
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_INSTANT)
                .build()

            val voiceChannel = NotificationChannel(
                CHANNEL_ID_VOICE,
                "رسائل صوتية",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "إشعارات الرسائل الصوتية المستلمة"
                setSound(soundUri, audioAttributes)
                enableVibration(true)
            }

            notificationManager.createNotificationChannels(listOf(messagesChannel, voiceChannel))
        }
    }

    /**
     * Displays a rich chat notification with Direct Reply action.
     */
    fun showMessageNotification(
        messageId: String,
        roomId: String,
        senderId: String,
        senderName: String,
        messageContent: String,
        isVoiceNote: Boolean = false
    ) {
        val channelId = if (isVoiceNote) CHANNEL_ID_VOICE else CHANNEL_ID_MESSAGES

        // 1. PendingIntent to open Chat Room
        val openChatIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("EXTRA_NAVIGATE_TO", "CHAT_DIRECT")
            putExtra("EXTRA_CHAT_ROOM_ID", roomId)
            putExtra("EXTRA_CHAT_TARGET_ID", senderId)
            putExtra("EXTRA_CHAT_TARGET_NAME", senderName)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            roomId.hashCode(),
            openChatIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 2. Direct Reply RemoteInput Action
        val remoteInput = RemoteInput.Builder(KEY_TEXT_REPLY)
            .setLabel("اكتب رداً سريعاً...")
            .build()

        val replyIntent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_DIRECT_REPLY
            putExtra("EXTRA_CHAT_ROOM_ID", roomId)
            putExtra("EXTRA_CHAT_TARGET_ID", senderId)
        }
        val replyPendingIntent = PendingIntent.getActivity(
            context,
            roomId.hashCode() + 1,
            replyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        val replyAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_send,
            "رد سريع 💬",
            replyPendingIntent
        ).addRemoteInput(remoteInput).build()

        // 3. Build Notification
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle(senderName)
            .setContentText(if (isVoiceNote) "🎙️ أرسل رسالة صوتية" else messageContent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(messageContent))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .addAction(replyAction)
            .setGroup(NOTIFICATION_GROUP_KEY)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .build()

        val notificationManager = NotificationManagerCompat.from(context)
        try {
            notificationManager.notify(messageId.hashCode(), notification)

            // Group Summary Notification for Android 7.0+
            val summaryNotification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.stat_notify_chat)
                .setStyle(NotificationCompat.InboxStyle().setSummaryText("رسائل جديدة"))
                .setGroup(NOTIFICATION_GROUP_KEY)
                .setGroupSummary(true)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(SUMMARY_NOTIFICATION_ID, summaryNotification)
        } catch (e: SecurityException) {
            // In case Notification permission is not yet granted on Android 13+
        }
    }
}
