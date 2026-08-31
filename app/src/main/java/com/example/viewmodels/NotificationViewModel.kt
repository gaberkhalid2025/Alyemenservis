package com.example.viewmodels

import androidx.annotation.Keep
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.NotificationEntity
import com.example.data.repositories.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class NotificationUiState {
    object Idle : NotificationUiState()
    object Loading : NotificationUiState()
    data class Success(val message: String) : NotificationUiState()
    data class Error(val message: String) : NotificationUiState()
}

/**
 * 🔔 NotificationViewModel
 * إدارة كاملة لمنطق الإشعارات الذكية، التدفق الحي والفلترة المتقدمة.
 */
@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<NotificationUiState>(NotificationUiState.Idle)
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    private val _notifications = MutableStateFlow<List<NotificationEntity>>(emptyList())
    val notifications: StateFlow<List<NotificationEntity>> = _notifications.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun observeUserNotifications(userId: String, phone: String = "", role: String = "CLIENT") {
        viewModelScope.launch {
            _isLoading.value = true
            notificationRepository.observeUserNotifications(userId, phone, role).collect { list ->
                _isLoading.value = false
                _notifications.value = list
                _unreadCount.value = list.count { !it.isRead }
            }
        }
    }

    fun observeAdminNotifications() {
        viewModelScope.launch {
            _isLoading.value = true
            notificationRepository.observeAdminNotifications().collect { list ->
                _isLoading.value = false
                _notifications.value = list
                _unreadCount.value = list.count { !it.isRead }
            }
        }
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            notificationRepository.markAsRead(notificationId)
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            val unreadIds = _notifications.value.filter { !it.isRead }.map { it.id }
            if (unreadIds.isNotEmpty()) {
                notificationRepository.markAllAsRead(unreadIds)
            }
        }
    }

    fun deleteNotification(notificationId: String) {
        viewModelScope.launch {
            notificationRepository.deleteNotification(notificationId)
        }
    }

    fun sendNotification(notification: NotificationEntity, onResult: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            val result = notificationRepository.sendNotification(notification)
            onResult?.invoke(result.isSuccess)
        }
    }

    override fun onCleared() {
        super.onCleared()
        notificationRepository.clearListeners()
    }
}
