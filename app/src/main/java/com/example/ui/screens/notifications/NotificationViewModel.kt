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
            val currentRead = mainViewModel.readNotificationIds.value.toMutableSet()
            currentRead.addAll(idsToMark)
            mainViewModel.preferenceHelper.markAllNotificationsAsRead(context, currentRead)
            mainViewModel._readNotificationIds.value = currentRead
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
        val provPhone = mainViewModel.selectedProvider?.phone?.trim()?.replace(" ", "")?.replace("+", "") ?: ""
        val provId = mainViewModel.selectedProvider?.id ?: ""
        val isAdmin = role == "OWNER" || role == "SUPER_ADMIN" || role == "ADMIN" || role == "SUPERVISOR"
        val seenKeys = mutableSetOf<String>()

        return allList.filter { notif ->
            if (!notif.isValid()) return@filter false

            val dKey = if (notif.dedupKey.isNotBlank()) notif.dedupKey else "${notif.notificationType}_${notif.title}_${notif.timestamp / (30 * 1000L)}"
            if (!seenKeys.add(dKey) && notif.id.isBlank()) return@filter false

            val isSensitive = notif.title.contains("كلمة مرور") || notif.message.contains("كلمة المرور") || 
                              notif.title.contains("استعادة") || notif.title.contains("رمز التحقق")
            if (isSensitive) {
                val isMyTarget = (cleanPhone.isNotEmpty() && notif.targetValue.contains(cleanPhone)) ||
                                 (cleanUserId.isNotEmpty() && notif.targetUserIds.contains(cleanUserId)) ||
                                 (provPhone.isNotEmpty() && notif.targetValue.contains(provPhone)) ||
                                 (provId.isNotEmpty() && notif.targetValue.contains(provId))
                if (!isAdmin && !isMyTarget) return@filter false
            }

            if (isAdmin) return@filter true

            val isRegistered = cleanPhone.isNotEmpty() || cleanUserId.isNotEmpty()
            if (!isRegistered && notif.targetAudience != "ALL") return@filter false

            when (notif.targetAudience.uppercase()) {
                "ADMIN_ONLY" -> false
                "ALL_REGISTERED_USERS" -> isRegistered
                "SPECIFIC_ROLES", "ROLE" -> {
                    val isProv = isProviderUser
                    notif.targetRoles.any { r ->
                        when (r.uppercase()) {
                            "TECHNICIAN", "PROVIDER" -> isProv
                            "STORE" -> isProv && mainViewModel.selectedStore != null
                            "MEDICAL" -> isProv && mainViewModel.selectedStore?.sectionId?.contains("medical") == true
                            "RESTAURANT" -> isProv && mainViewModel.selectedStore?.sectionId?.contains("restaurant") == true
                            "REAL_ESTATE" -> isProv && mainViewModel.selectedProperty != null
                            "USER" -> isRegistered
                            else -> false
                        }
                    }
                }
                "SPECIFIC_USERS", "SPECIFIC_USER" -> {
                    (cleanPhone.isNotEmpty() && (notif.targetValue.contains(cleanPhone) || notif.targetUserIds.contains(cleanPhone))) ||
                    (cleanUserId.isNotEmpty() && notif.targetUserIds.contains(cleanUserId)) ||
                    (provPhone.isNotEmpty() && notif.targetValue.contains(provPhone)) ||
                    (provId.isNotEmpty() && notif.targetValue.contains(provId))
                }
                "REGION" -> {
                    val currentRes = currentUserResidence.value
                    notif.targetValue.isEmpty() || currentRes.contains(notif.targetValue)
                }
                "CATEGORY" -> true
                "ALL" -> {
                    when (notif.targetType) {
                        "ALL" -> notif.targetAudience != "ADMIN_ONLY"
                        "USER" -> notif.targetValue.isEmpty() || (cleanPhone.isNotEmpty() && notif.targetValue.contains(cleanPhone))
                        "PROVIDER" -> (cleanPhone.isNotEmpty() && (notif.targetValue.contains(cleanPhone) || notif.targetUserIds.contains(cleanPhone))) ||
                                      (cleanUserId.isNotEmpty() && (notif.targetValue.contains(cleanUserId) || notif.targetUserIds.contains(cleanUserId))) ||
                                      (provPhone.isNotEmpty() && (notif.targetValue.contains(provPhone) || notif.targetUserIds.contains(provPhone))) ||
                                      (provId.isNotEmpty() && (notif.targetValue.contains(provId) || notif.targetUserIds.contains(provId)))
                        "SUPERVISOR", "ADMIN_ONLY" -> false
                        else -> true
                    }
                }
                else -> false
            }
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
}
