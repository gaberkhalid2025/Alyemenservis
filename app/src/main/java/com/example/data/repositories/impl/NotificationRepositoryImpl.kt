package com.example.data.repositories.impl

import android.content.Context
import android.util.Log
import com.example.data.NotificationEntity
import com.example.data.repositories.contracts.INotificationRepository
import com.example.data.utils.AppError
import com.example.data.utils.AppResult
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

class NotificationRepositoryImpl(
    private val context: Context?,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : INotificationRepository {

    private val listeners = mutableListOf<ListenerRegistration>()
    private val notificationsCollection = firestore.collection("notifications")
    private val deduplicator = context?.let { NotificationDeduplicator(it) }

    companion object {
        private const val TAG = "NotificationRepositoryImpl"
    }

    override fun clearListeners() {
        try {
            listeners.forEach { it.remove() }
            listeners.clear()
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing listeners", e)
        }
    }

    override suspend fun sendNotification(notification: NotificationEntity): AppResult<String> = withContext(Dispatchers.IO) {
        try {
            val docId = notification.id.ifBlank { UUID.randomUUID().toString() }
            val finalNotification = notification.copy(
                id = docId,
                createdAt = if (notification.createdAt > 0) notification.createdAt else System.currentTimeMillis()
            )

            if (deduplicator != null) {
                if (deduplicator.isDuplicate(finalNotification)) {
                    return@withContext Result.success(docId)
                }
            }

            notificationsCollection.document(docId).set(finalNotification, SetOptions.merge()).await()
            Result.success(docId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send notification", e)
            Result.failure(AppError.DatabaseError(e.localizedMessage ?: "فشل إرسال الإشعار"))
        }
    }

    override fun observeUserNotifications(userId: String, phone: String, role: String): Flow<List<NotificationEntity>> = callbackFlow {
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
        }
    }.flowOn(Dispatchers.IO)

    override fun observeAdminNotifications(): Flow<List<NotificationEntity>> = callbackFlow {
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
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun markAsRead(notificationId: String): AppResult<Unit> = withContext(Dispatchers.IO) {
        try {
            if (notificationId.isBlank()) return@withContext Result.success(Unit)
            notificationsCollection.document(notificationId).update("isRead", true).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error marking notification $notificationId as read", e)
            Result.failure(AppError.DatabaseError(e.localizedMessage ?: "فشل تعليم الإشعار كمقروء"))
        }
    }

    override suspend fun markAllAsRead(notificationIds: List<String>): AppResult<Unit> = withContext(Dispatchers.IO) {
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
            Result.failure(AppError.DatabaseError(e.localizedMessage ?: "فشل تعليم جميع الإشعارات كمقروءة"))
        }
    }

    override suspend fun deleteNotification(notificationId: String): AppResult<Unit> = withContext(Dispatchers.IO) {
        try {
            if (notificationId.isBlank()) return@withContext Result.success(Unit)
            notificationsCollection.document(notificationId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting notification $notificationId", e)
            Result.failure(AppError.DatabaseError(e.localizedMessage ?: "فشل حذف الإشعار"))
        }
    }

    override suspend fun deleteAllNotifications(): AppResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val snapshot = notificationsCollection.get().await()
            val batch = firestore.batch()
            for (doc in snapshot.documents) {
                batch.delete(doc.reference)
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting all notifications", e)
            Result.failure(AppError.DatabaseError(e.localizedMessage ?: "فشل حذف جميع الإشعارات"))
        }
    }
}
