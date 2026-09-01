package com.example.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
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
 * إدارة الإشعارات الفورية وسجل التنبيهات الخاصة بالمستخدم والفنيين.
 */
class NotificationViewModel(application: Application) : AndroidViewModel(application) {

    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    private val _notifications = MutableStateFlow<List<AppNotification>>(emptyList())
    val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    fun listenForNotifications(userId: String) {
        if (userId.isBlank()) return
        firestore.collection("notifications")
            .whereEqualTo("targetUserId", userId)
            .addSnapshotListener { snapshot, error ->
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { it.toObject(AppNotification::class.java) }
                    _notifications.value = list.sortedByDescending { it.createdAt }
                    _unreadCount.value = if (list.isEmpty()) 0 else list.count { !it.isRead }
                }
            }
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            firestore.collection("notifications").document(notificationId).update("isRead", true)
            _notifications.value = _notifications.value.map {
                if (it.id == notificationId) it.copy(isRead = true) else it
            }
            _unreadCount.value = _notifications.value.count { !it.isRead }
        }
    }

    fun clearAll() {
        _notifications.value = emptyList()
        _unreadCount.value = 0
    }
}
