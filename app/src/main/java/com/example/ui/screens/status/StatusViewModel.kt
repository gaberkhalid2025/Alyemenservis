package com.example.ui.screens.status

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BookingEntity
import com.example.data.NotificationEntity
import com.example.data.PendingProviderEntity
import com.example.data.models.InstantRequestEntity
import com.example.data.repositories.IStatusRepository
import com.example.data.repositories.SystemStatusMetrics
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 📊 StatusUiState
 */
data class StatusUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val metrics: SystemStatusMetrics = SystemStatusMetrics(),
    val pendingJoinRequests: List<PendingProviderEntity> = emptyList(),
    val systemBookings: List<BookingEntity> = emptyList(),
    val instantRequests: List<InstantRequestEntity> = emptyList(),
    val notifications: List<NotificationEntity> = emptyList(),
    val selectedTab: StatusTab = StatusTab.OVERVIEW,
    val errorMessage: String? = null
)

/**
 * ⚡ StatusEvent (SharedFlow for one-time events)
 */
sealed class StatusEvent {
    data class ShowToast(val message: String) : StatusEvent()
    data class ShowSnackbar(val message: String) : StatusEvent()
}

/**
 * 🧠 StatusViewModel
 * ViewModel for Platform Status Center, metrics dashboard, pending join requests, and platform notifications.
 * Implements auto-refresh every 30 seconds and offline-first state handling.
 */
class StatusViewModel(
    private val statusRepository: IStatusRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatusUiState())
    val uiState: StateFlow<StatusUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<StatusEvent>()
    val eventFlow: SharedFlow<StatusEvent> = _eventFlow.asSharedFlow()

    private var autoRefreshJob: Job? = null

    init {
        loadStatusData()
        startAutoRefresh()
    }

    fun selectTab(tab: StatusTab) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }

    fun loadStatusData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            launch {
                statusRepository.getSystemMetricsFlow().collect { metrics ->
                    _uiState.value = _uiState.value.copy(metrics = metrics, isLoading = false)
                }
            }

            launch {
                statusRepository.getPendingJoinRequestsFlow().collect { requests ->
                    _uiState.value = _uiState.value.copy(pendingJoinRequests = requests)
                }
            }

            launch {
                statusRepository.getSystemBookingsFlow().collect { bookings ->
                    _uiState.value = _uiState.value.copy(systemBookings = bookings)
                }
            }

            launch {
                statusRepository.getInstantRequestsFlow().collect { instantReqs ->
                    _uiState.value = _uiState.value.copy(instantRequests = instantReqs)
                }
            }

            launch {
                statusRepository.getNotificationsFlow().collect { notifs ->
                    _uiState.value = _uiState.value.copy(notifications = notifs)
                }
            }
        }
    }

    fun refreshData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            val result = statusRepository.refreshSystemStatus()
            _uiState.value = _uiState.value.copy(isRefreshing = false)
            if (result.isSuccess) {
                _eventFlow.emit(StatusEvent.ShowSnackbar("🔄 تم تحديث حالات وبيانات المنصة"))
            } else {
                _eventFlow.emit(StatusEvent.ShowToast("تعذر التحديث: ${result.exceptionOrNull()?.localizedMessage}"))
            }
        }
    }

    fun approveJoinRequest(request: PendingProviderEntity) {
        viewModelScope.launch {
            val result = statusRepository.approveJoinRequest(request)
            result.onSuccess {
                _eventFlow.emit(StatusEvent.ShowSnackbar("✅ تم قبول طلب انضمام: ${request.name}"))
            }.onFailure { e ->
                _eventFlow.emit(StatusEvent.ShowToast("❌ فشل إجراء القبول: ${e.localizedMessage}"))
            }
        }
    }

    fun rejectJoinRequest(request: PendingProviderEntity, reason: String = "") {
        viewModelScope.launch {
            val result = statusRepository.rejectJoinRequest(request, reason)
            result.onSuccess {
                _eventFlow.emit(StatusEvent.ShowSnackbar("❌ تم رفض طلب انضمام: ${request.name}"))
            }.onFailure { e ->
                _eventFlow.emit(StatusEvent.ShowToast("فشل إجراء الرفض: ${e.localizedMessage}"))
            }
        }
    }

    fun clearAllNotifications() {
        viewModelScope.launch {
            val result = statusRepository.clearNotifications()
            result.onSuccess {
                _eventFlow.emit(StatusEvent.ShowSnackbar("🧹 تم مسح كافة الإشعارات"))
            }
        }
    }

    private fun startAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = viewModelScope.launch {
            while (true) {
                delay(30_000L) // 30 seconds auto refresh loop
                statusRepository.refreshSystemStatus()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        autoRefreshJob?.cancel()
    }
}
