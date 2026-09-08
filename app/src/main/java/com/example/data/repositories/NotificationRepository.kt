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
}
