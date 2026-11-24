package com.example.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * 💬 DirectReplyReceiver
 * يستقبل الرد السريع المباشر من الإشعار (RemoteInput) ويرسله إلى قناة المحادثة في Firestore.
 */
class DirectReplyReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_DIRECT_REPLY) return

        val remoteInput = RemoteInput.getResultsFromIntent(intent) ?: return
        val replyText = remoteInput.getCharSequence(ChatNotificationHelper.KEY_TEXT_REPLY)?.toString()?.trim().orEmpty()
        val channelId = intent.getStringExtra(ChatNotificationHelper.EXTRA_CHANNEL_ID).orEmpty()
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, channelId.hashCode())
        if (replyText.isBlank() || channelId.isBlank()) return

        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val auth = FirebaseAuth.getInstance()
                val uid = auth.currentUser?.uid
                val prefs = appContext.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                val senderPhone = prefs.getString("user_phone", "").orEmpty()
                val senderName = prefs.getString("user_name", "مستخدم").orEmpty().ifBlank { "مستخدم" }
                val senderId = uid ?: senderPhone
                if (senderId.isBlank()) return@launch

                val db = FirebaseFirestore.getInstance()
                val msgId = UUID.randomUUID().toString()
                val now = System.currentTimeMillis()
                val msgPayload = hashMapOf(
                    "id" to msgId,
                    "channelId" to channelId,
                    "senderId" to senderId,
                    "senderName" to senderName,
                    "senderPhone" to senderPhone,
                    "text" to replyText,
                    "mediaType" to "TEXT",
                    "timestamp" to now,
                    "isRead" to false
                )
                db.collection("chat_channels").document(channelId)
                    .collection("messages").document(msgId)
                    .set(msgPayload)
                    .await()

                val updates = mutableMapOf<String, Any>(
                    "lastMessage" to replyText,
                    "lastMessageTime" to now,
                    "lastTimestamp" to now,
                    "lastSenderId" to senderId
                )
                val targetId = intent.getStringExtra(ChatNotificationHelper.EXTRA_SENDER_ID).orEmpty()
                if (targetId.isNotBlank()) {
                    updates[FieldPath.of("unreadCounts", targetId).toString()] = FieldValue.increment(1L)
                }
                db.collection("chat_channels").document(channelId)
                    .update(
                        "lastMessage", replyText,
                        "lastMessageTime", now,
                        "lastTimestamp", now,
                        "lastSenderId", senderId
                    )
                    .await()

                val updatedNotification = NotificationCompat.Builder(appContext, ChatNotificationHelper.CHANNEL_MESSAGES)
                    .setSmallIcon(android.R.drawable.stat_notify_chat)
                    .setContentTitle("تم إرسال ردك ✅")
                    .setContentText(replyText)
                    .setAutoCancel(true)
                    .build()
                try {
                    NotificationManagerCompat.from(appContext).notify(notificationId, updatedNotification)
                } catch (_: SecurityException) {
                }
            } catch (_: Exception) {
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_DIRECT_REPLY = "com.alyemenservices.ACTION_CHAT_DIRECT_REPLY"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    }
}
