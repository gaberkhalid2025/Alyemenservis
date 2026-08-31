package com.example.ui.screens.notifications

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.NotificationEntity

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
    private val mainViewModel: AuthViewModel
) : ViewModel() {

    // Filtering inputs
    private val _activeTab = MutableStateFlow("ALL") // "ALL", "UNREAD", "IMPORTANT", "READ"
    val activeTab: StateFlow<String> = _activeTab.asStateFlow()

    private val _selectedTypeFilter = MutableStateFlow("ALL") // "ALL", "BOOKING", "MESSAGE", "SPECIAL_OFFER", "SYSTEM"
    val selectedTypeFilter: StateFlow<String> = _selectedTypeFilter.asStateFlow()

    // Expose flows from AuthViewModel
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
        viewModelScope.launch {
            notificationsList.forEach { notif ->
                mainViewModel.markNotificationAsRead(context, notif.id)
            }
        }
    }

    fun deleteNotification(notifId: String) {
        mainViewModel.deleteNotification(notifId)
    }

    fun deleteAllNotifications() {
        mainViewModel.deleteAllNotifications()
    }

    fun addNotification(
        title: String,
        message: String,
        targetType: String,
        targetValue: String,
        targetAudience: String,
        targetRoles: List<String>,
        targetUserIds: List<String>,
        notificationType: String
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
}
