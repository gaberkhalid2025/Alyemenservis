package com.example.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.NotificationEntity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class AppNotification(
    val id: String = UUID.randomUUID().toString(),
    val targetUserId: String = "",
    val title: String = "",
    val body: String = "",
    val bookingId: String = "",
    val type: String = "GENERAL",
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 🔔 NotificationViewModel
 * إدارة الإشعارات الفورية وسجل التنبيهات الخاصة بالمستخدم والفنيين والربط مع Firestore و FCM.
 */
class NotificationViewModel(application: Application) : AndroidViewModel(application) {

    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    private val _notifications = MutableStateFlow<List<NotificationEntity>>(emptyList())
    val notifications: StateFlow<List<NotificationEntity>> = _notifications.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    private var notificationsListener: ListenerRegistration? = null

    /**
     * الاستماع لإشعارات المستخدم في الوقت الفعلي
     */
    fun listenForNotifications(userId: String) {
        if (userId.isBlank()) return
        val clean = userId.trim().replace(" ", "").replace("+", "")
        notificationsListener?.remove()
        notificationsListener = firestore.collection("notifications")
            .whereIn("customerPhone", listOf(clean, userId, "ALL"))
            .addSnapshotListener { snapshot, error ->
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { it.toObject(NotificationEntity::class.java) }
                        .sortedByDescending { it.timestamp }
                    _notifications.value = list
                    _unreadCount.value = list.count { !it.isRead }
                }
            }
    }

    /**
     * تعيين إشعار كمقروء
     */
    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            firestore.collection("notifications").document(notificationId).update("isRead", true)
            _notifications.value = _notifications.value.map {
                if (it.id == notificationId) it.copy(isRead = true) else it
            }
            _unreadCount.value = _notifications.value.count { !it.isRead }
        }
    }

    /**
     * تعيين جميع الإشعارات كمقروءة
     */
    fun markAllAsRead(userId: String) {
        viewModelScope.launch {
            val batch = firestore.batch()
            _notifications.value.filter { !it.isRead }.forEach { notif ->
                val ref = firestore.collection("notifications").document(notif.id)
                batch.update(ref, "isRead", true)
            }
            batch.commit().addOnSuccessListener {
                _notifications.value = _notifications.value.map { it.copy(isRead = true) }
                _unreadCount.value = 0
            }
        }
    }

    /**
     * حذف إشعار
     */
    fun deleteNotification(notificationId: String) {
        viewModelScope.launch {
            firestore.collection("notifications").document(notificationId).delete()
            _notifications.value = _notifications.value.filterNot { it.id == notificationId }
            _unreadCount.value = _notifications.value.count { !it.isRead }
        }
    }

    /**
     * إرسال إشعار جديد
     */
    fun sendNotification(targetPhone: String, title: String, message: String, type: String = "INFO", targetType: String = "USER") {
        viewModelScope.launch {
            val id = UUID.randomUUID().toString()
            val notif = NotificationEntity(
                id = id,
                title = title,
                message = message,
                customerPhone = targetPhone,
                targetType = targetType,
                targetValue = targetPhone,
                notificationType = type,
                isRead = false,
                timestamp = System.currentTimeMillis()
            )
            firestore.collection("notifications").document(id).set(notif)
        }
    }

    fun clearAll() {
        _notifications.value = emptyList()
        _unreadCount.value = 0
    }

    override fun onCleared() {
        super.onCleared()
        notificationsListener?.remove()
    }
}
