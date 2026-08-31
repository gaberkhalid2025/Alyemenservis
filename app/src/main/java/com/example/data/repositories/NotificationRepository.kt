package com.example.data.repositories

import android.content.Context
import android.util.Log
import com.example.data.NotificationEntity
import com.example.util.NotificationDeduplicator
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * 🔔 NotificationRepository
 * مستودع إدارة الإشعارات، قراءة، كتابة، حذف، التحقق من التكرار، وتحديث الحالة
 */
class NotificationRepository(
    private val context: Context? = null,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val listeners = mutableListOf<ListenerRegistration>()
    private val notificationsCollection = firestore.collection("notifications")
    private val deduplicator = context?.let { NotificationDeduplicator(it) }

    companion object {
        private const val TAG = "NotificationRepository"
    }

    fun clearListeners() {
        try {
            listeners.forEach { it.remove() }
            listeners.clear()
            Log.d(TAG, "All NotificationRepository listeners cleared safely")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing listeners", e)
        }
    }

    /**
     * إرسال إشعار جديد مع التحقق من عدم التكرار
     */
    suspend fun sendNotification(notification: NotificationEntity): Result<String> = withContext(Dispatchers.IO) {
        try {
            val docId = if (notification.id.isNotBlank()) notification.id else UUID.randomUUID().toString()
            val finalNotification = notification.copy(
                id = docId,
                createdAt = if (notification.createdAt > 0) notification.createdAt else System.currentTimeMillis()
            )

            // Check deduplication
            if (deduplicator != null) {
                if (deduplicator.isDuplicate(finalNotification)) {
                    Log.d(TAG, "Notification deduplicated: ${finalNotification.id}")
                    return@withContext Result.success(docId)
                }
            }

            notificationsCollection.document(docId).set(finalNotification, SetOptions.merge()).await()
            Result.success(docId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send notification", e)
            Result.failure(e)
        }
    }

    /**
     * مراقبة إشعارات مستخدم محدد (بما فيها الإشعارات العامة وإشعارات الدور)
     */
    fun observeUserNotifications(userId: String, phone: String, role: String = "CLIENT"): Flow<List<NotificationEntity>> = callbackFlow {
        val cleanPhone = phone.trim().replace(" ", "").replace("+", "")

        val listener = notificationsCollection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(100)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error observing notifications for user $userId / $phone", error)
                    return@addSnapshotListener
                }

                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(NotificationEntity::class.java)?.copy(id = doc.id)
                }?.filter { notif ->
                    // Filter matching notifications for this user
                    when {
                        notif.targetType == "ALL" || notif.targetAudience == "ALL" -> true
                        notif.targetType == "USER" && (notif.targetValue == userId || notif.targetValue == cleanPhone || notif.customerPhone == cleanPhone) -> true
                        notif.targetUserIds.contains(userId) || notif.targetUserIds.contains(cleanPhone) -> true
                        notif.targetRoles.contains(role) -> true
                        notif.targetType == role -> true
                        else -> false
                    }
                } ?: emptyList()

                trySend(list)
            }
        listeners.add(listener)

        awaitClose {
            listener.remove()
            listeners.remove(listener)
        }
    }.flowOn(Dispatchers.IO)

    /**
     * مراقبة جميع الإشعارات للإدارة
     */
    fun observeAdminNotifications(): Flow<List<NotificationEntity>> = callbackFlow {
        val listener = notificationsCollection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(150)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error observing admin notifications", error)
                    return@addSnapshotListener
                }

                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(NotificationEntity::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                trySend(list)
            }
        listeners.add(listener)

        awaitClose {
            listener.remove()
            listeners.remove(listener)
        }
    }.flowOn(Dispatchers.IO)

    /**
     * تعليم إشعار كمقروء
     */
    suspend fun markAsRead(notificationId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (notificationId.isBlank()) return@withContext Result.success(Unit)
            notificationsCollection.document(notificationId).update("isRead", true).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error marking notification $notificationId as read", e)
            Result.failure(e)
        }
    }

    /**
     * تعليم جميع إشعارات المستخدم كمقروءة
     */
    suspend fun markAllAsRead(notificationIds: List<String>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (notificationIds.isEmpty()) return@withContext Result.success(Unit)
            val batch = firestore.batch()
            notificationIds.forEach { id ->
                val ref = notificationsCollection.document(id)
                batch.update(ref, "isRead", true)
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error marking batch notifications as read", e)
            Result.failure(e)
        }
    }

    /**
     * حذف إشعار
     */
    suspend fun deleteNotification(notificationId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (notificationId.isBlank()) return@withContext Result.success(Unit)
            notificationsCollection.document(notificationId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting notification $notificationId", e)
            Result.failure(e)
        }
    }
}
