package com.example.ui.screens.notifications

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.NotificationEntity
import com.example.ui.MainViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Representing the UI State for Notifications.
 */
sealed class NotificationUiState {
    object Loading : NotificationUiState()
    data class Success(val notifications: List<NotificationEntity>) : NotificationUiState()
    data class Error(val message: String) : NotificationUiState()
}

/**
 * NotificationViewModel is responsible for offloading notification processing, security audits,
 * and filtering logic from the UI.
 *
 * @param mainViewModel The shared global MainViewModel.
 */
class UserNotificationsViewModel(
    private val mainViewModel: MainViewModel
) : ViewModel() {

    private val _uiState = MutableStateFlow<NotificationUiState>(NotificationUiState.Loading)
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    private val _activeTab = MutableStateFlow("ALL")
    val activeTab: StateFlow<String> = _activeTab.asStateFlow()

    private val _selectedTypeFilter = MutableStateFlow("ALL")
    val selectedTypeFilter: StateFlow<String> = _selectedTypeFilter.asStateFlow()

    init {
        observeNotifications()
    }

    private fun observeNotifications() {
        viewModelScope.launch {
            try {
                mainViewModel.notifications.collect { notifs ->
                    _uiState.value = NotificationUiState.Success(notifs)
                }
            } catch (e: Exception) {
                _uiState.value = NotificationUiState.Error(e.localizedMessage ?: "فشل في تحميل الإشعارات")
            }
        }
    }

    /**
     * Set active filter tab ("ALL", "UNREAD", "IMPORTANT", "READ").
     */
    fun setActiveTab(tab: String) {
        _activeTab.value = tab
    }

    /**
     * Set active notification category filter.
     */
    fun setSelectedTypeFilter(filter: String) {
        _selectedTypeFilter.value = filter
    }

    /**
     * Loads the read notification IDs.
     */
    fun loadReadNotifications(context: Context) {
        mainViewModel.loadReadNotifications(context)
    }

    /**
     * Marks a specific notification as read.
     */
    fun markNotificationAsRead(context: Context, notifId: String) {
        mainViewModel.markNotificationAsRead(context, notifId)
    }

    /**
     * Deletes a specific notification.
     */
    fun deleteNotification(notifId: String) {
        mainViewModel.deleteNotification(notifId)
    }

    /**
     * Re-adds or pushes a new notification (for Undo operations).
     */
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

    /**
     * Clears all notifications.
     */
    fun deleteAllNotifications() {
        mainViewModel.deleteAllNotifications()
    }

    /**
     * Live computed and secured notifications list.
     */
    val validAndFilteredNotifications: StateFlow<List<NotificationEntity>> = combine(
        mainViewModel.notifications,
        mainViewModel.currentUserPhone,
        mainViewModel.currentUserId,
        mainViewModel.adminRole
    ) { allNotifications, userPhone, userId, adminRole ->
        val cleanPhone = userPhone.trim().replace(" ", "").replace("+", "")
        val cleanUserId = userId.trim()
        val isAdmin = adminRole == "OWNER" || adminRole == "SUPER_ADMIN" || adminRole == "ADMIN" || adminRole == "SUPERVISOR"
        val seenKeys = mutableSetOf<String>()

        allNotifications.filter { notif ->
            // 1. Strict Validation Check
            if (!notif.isValid()) return@filter false

            // 2. Deduplication check
            val dKey = if (notif.dedupKey.isNotBlank()) notif.dedupKey else "${notif.notificationType}_${notif.title}_${notif.timestamp / (30 * 1000L)}"
            if (!seenKeys.add(dKey) && notif.id.isBlank()) return@filter false

            // 3. Security & Sensitivity Check
            val isSensitive = notif.title.contains("كلمة مرور") || notif.message.contains("كلمة المرور") || 
                              notif.title.contains("استعادة") || notif.title.contains("رمز التحقق")
            if (isSensitive) {
                val isMyTarget = (cleanPhone.isNotEmpty() && notif.targetValue.contains(cleanPhone)) ||
                                 (cleanUserId.isNotEmpty() && notif.targetUserIds.contains(cleanUserId))
                if (!isAdmin && !isMyTarget) return@filter false
            }

            // 4. Role & Audience Targeting Logic
            if (isAdmin) return@filter true

            val isRegistered = cleanPhone.isNotEmpty() || cleanUserId.isNotEmpty()
            if (!isRegistered && notif.targetAudience != "ALL") return@filter false

            when (notif.targetAudience.uppercase()) {
                "ADMIN_ONLY" -> false
                "ALL_REGISTERED_USERS" -> isRegistered
                "SPECIFIC_ROLES", "ROLE" -> {
                    val isProvider = mainViewModel.isProviderUser
                    notif.targetRoles.any { r ->
                        when (r.uppercase()) {
                            "TECHNICIAN", "PROVIDER" -> isProvider
                            "STORE" -> isProvider && mainViewModel.selectedStore != null
                            "MEDICAL" -> isProvider && mainViewModel.selectedStore?.sectionId?.contains("medical") == true
                            "RESTAURANT" -> isProvider && mainViewModel.selectedStore?.sectionId?.contains("restaurant") == true
                            "REAL_ESTATE" -> isProvider && mainViewModel.selectedProperty != null
                            "USER" -> isRegistered
                            else -> false
                        }
                    }
                }
                "SPECIFIC_USERS", "SPECIFIC_USER" -> {
                    (cleanPhone.isNotEmpty() && (notif.targetValue.contains(cleanPhone) || notif.targetUserIds.contains(cleanPhone))) ||
                    (cleanUserId.isNotEmpty() && notif.targetUserIds.contains(cleanUserId))
                }
                "REGION" -> {
                    val currentRes = mainViewModel.currentUserResidence.value
                    notif.targetValue.isEmpty() || currentRes.contains(notif.targetValue)
                }
                "CATEGORY" -> true
                "ALL" -> {
                    when (notif.targetType) {
                        "ALL" -> true
                        "USER" -> notif.targetValue.isEmpty() || (cleanPhone.isNotEmpty() && notif.targetValue.contains(cleanPhone))
                        "PROVIDER" -> cleanPhone.isNotEmpty() && notif.targetValue.contains(cleanPhone)
                        "SUPERVISOR" -> false
                        else -> true
                    }
                }
                else -> false
            }
        }.distinctBy { it.id.ifBlank { "${it.title}_${it.timestamp}" } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Reactive count of unread notifications.
     */
    val unreadCount: StateFlow<Int> = combine(
        validAndFilteredNotifications,
        mainViewModel.readNotificationIds
    ) { validList, readIds ->
        validList.count { !readIds.contains(it.id) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    /**
     * finalNotifs combined filter flow for the UI layout.
     */
    val finalNotifications: StateFlow<List<NotificationEntity>> = combine(
        validAndFilteredNotifications,
        mainViewModel.readNotificationIds,
        _activeTab,
        _selectedTypeFilter
    ) { list, readIds, tab, type ->
        list.filter { notif ->
            val matchesTab = when (tab) {
                "READ" -> readIds.contains(notif.id)
                "UNREAD" -> !readIds.contains(notif.id)
                "IMPORTANT" -> notif.notificationType == "BOOKING" || notif.notificationType == "ADMIN" || notif.title.contains("عاجل") || notif.title.contains("مهم")
                else -> true
            }
            val matchesType = when (type) {
                "BOOKING" -> notif.notificationType == "BOOKING" || notif.title.contains("حجز")
                "MESSAGE" -> notif.notificationType == "MESSAGE" || notif.title.contains("دردشة") || notif.title.contains("رسالة")
                "SPECIAL_OFFER" -> notif.notificationType == "SPECIAL_OFFER" || notif.title.contains("عرض")
                "SYSTEM" -> notif.notificationType == "SYSTEM" || notif.notificationType == "ADMIN"
                else -> true
            }
            matchesTab && matchesType
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
