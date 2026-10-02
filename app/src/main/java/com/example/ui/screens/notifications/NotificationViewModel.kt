package com.example.ui.screens.notifications

import android.content.Context
import com.example.ui.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.NotificationEntity
import com.example.ui.MainViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for managing user notifications, state-based filtering, deduplication,
 * and high-performance, real-time read/unread status updates.
 */
class NotificationViewModel(
    private val mainViewModel: MainViewModel
) : ViewModel() {

    // Filtering inputs
    private val _activeTab = MutableStateFlow("ALL") // "ALL", "UNREAD", "IMPORTANT", "READ"
    val activeTab: StateFlow<String> = _activeTab.asStateFlow()

    private val _selectedTypeFilter = MutableStateFlow("ALL") // "ALL", "BOOKING", "MESSAGE", "SPECIAL_OFFER", "SYSTEM"
    val selectedTypeFilter: StateFlow<String> = _selectedTypeFilter.asStateFlow()

    // Expose flows from MainViewModel
    val notifications = mainViewModel.notifications
    val currentUserPhone = mainViewModel.currentUserPhone
    val currentUserId = mainViewModel.currentUserId
    val adminRole = mainViewModel.adminRole
    val readNotificationIds = mainViewModel.readNotificationIds
    val currentUserResidence = mainViewModel.currentUserResidence
    val isProviderUser = mainViewModel.isProviderUser

    fun setActiveTab(tab: String) {
        _activeTab.value = tab
    }

    fun setSelectedTypeFilter(filter: String) {
        _selectedTypeFilter.value = filter
    }

    fun loadReadNotifications(context: Context) {
        mainViewModel.loadReadNotifications(context)
    }

    fun markNotificationAsRead(context: Context, notifId: String) {
        mainViewModel.markNotificationAsRead(context, notifId)
    }

    fun markAllAsRead(context: Context, notificationsList: List<NotificationEntity>) {
        val idsToMark = notificationsList.map { it.id }.filter { it.isNotBlank() }.toSet()
        if (idsToMark.isNotEmpty()) {
            mainViewModel.markNotificationsAsRead(context, idsToMark)
        }
    }

    fun deleteNotification(notifId: String) {
        mainViewModel.deleteNotification(notifId)
    }

    fun deleteAllNotifications() {
        mainViewModel.deleteAllNotifications()
    }

    fun listenForNotifications(userId: String) {
        // Live listener handled directly via mainViewModel.notifications Flow
    }

    fun markAsRead(context: Context, notificationId: String) {
        markNotificationAsRead(context, notificationId)
    }

    fun clearAll() {
        deleteAllNotifications()
    }

    fun filterAudienceNotifications(
        allList: List<NotificationEntity>,
        phone: String,
        uid: String,
        role: String
    ): List<NotificationEntity> {
        val cleanPhone = phone.trim().replace(" ", "").replace("+", "")
        val cleanUserId = uid.trim()
        val isAdmin = role == "OWNER" || role == "SUPER_ADMIN" || role == "ADMIN" || role == "SUPERVISOR"
        val isProv = isProviderUser
        val isReg = cleanPhone.isNotEmpty() || cleanUserId.isNotEmpty()
        val currentRes = currentUserResidence.value
        val seenKeys = mutableSetOf<String>()

        return allList.filter { notif ->
            if (!notif.isValid()) return@filter false

            val dKey = if (notif.dedupKey.isNotBlank()) notif.dedupKey else "${notif.notificationType}_${notif.title}_${notif.timestamp / (30 * 1000L)}"
            if (!seenKeys.add(dKey) && notif.id.isBlank()) return@filter false

            NotificationAudienceFilter.isAudienceMatched(
                notif = notif,
                cleanPhone = cleanPhone,
                cleanUserId = cleanUserId,
                isAdmin = isAdmin,
                isRegistered = isReg,
                isProvider = isProv,
                currentResidence = currentRes
            )
        }.distinctBy { it.id.ifBlank { "${it.title}_${it.timestamp}" } }
    }

    fun addNotification(
        title: String,
        message: String,
        targetType: String = "ALL",
        targetValue: String = "",
        targetAudience: String = "ALL",
        targetRoles: List<String> = emptyList(),
        targetUserIds: List<String> = emptyList(),
        notificationType: String = "NORMAL"
    ) {
        mainViewModel.addNotification(
            title = title,
            message = message,
            targetType = targetType,
            targetValue = targetValue,
            targetAudience = targetAudience,
            targetRoles = targetRoles,
            targetUserIds = targetUserIds,
            notificationType = notificationType
        )
    }

    fun addNotification(request: com.example.data.models.NotificationRequest) {
        addNotification(
            title = request.title,
            message = request.message,
            targetType = request.targetType,
            targetValue = request.targetValue,
            targetAudience = request.targetAudience,
            targetRoles = request.targetRoles,
            targetUserIds = request.targetUserIds,
            notificationType = request.notificationType
        )
    }
}
