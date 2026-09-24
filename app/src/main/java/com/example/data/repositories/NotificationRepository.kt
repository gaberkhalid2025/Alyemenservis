package com.example.data.repositories

import com.example.data.NotificationEntity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class NotificationRepository {
    private val db = FirebaseFirestore.getInstance()

    suspend fun saveNotification(notification: NotificationEntity) {
        db.collection("notifications").document(notification.id).set(notification).await()
    }

    suspend fun deleteNotification(notifId: String) {
        db.collection("notifications").document(notifId).delete().await()
    }

    suspend fun cleanupOldNotifications(userPhone: String) {
        try {
            val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
            val oldNotifs = db.collection("notifications")
                .whereEqualTo("targetValue", userPhone)
                .whereLessThan("createdAt", thirtyDaysAgo)
                .get()
                .await()

            val batch = db.batch()
            for (doc in oldNotifs.documents) {
                batch.delete(doc.reference)
            }
            batch.commit().await()
        } catch (e: Exception) {
            android.util.Log.e("NotificationRepository", "Error cleaning up old notifications: ${e.message}")
        }
    }
}
