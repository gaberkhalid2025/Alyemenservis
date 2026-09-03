package com.example.ui.screens.dashboard.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repositories.IDashboardRepository
import com.example.ui.screens.dashboard.DashboardEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DoctorItem(
    val id: String = "",
    val name: String,
    val specialty: String,
    val hours: String
)

/**
 * 🧠 MedicalDashboardViewModel - إدارة الأطباء والعيادات بالمراكز الطبية
 */
class MedicalDashboardViewModel(
    private val centerId: String,
    private val dashboardRepository: IDashboardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<DashboardEvent>()
    val eventFlow: SharedFlow<DashboardEvent> = _eventFlow.asSharedFlow()

    private val _doctors = MutableStateFlow<List<DoctorItem>>(emptyList())
    val doctors: StateFlow<List<DoctorItem>> = _doctors.asStateFlow()

    init {
        viewModelScope.launch {
            dashboardRepository.getDashboardStats(centerId, "MEDICAL").collect { stats ->
                _uiState.value = _uiState.value.copy(stats = stats, isLoading = false)
            }
        }
    }

    fun addDoctor(name: String, specialty: String, hours: String) {
        if (name.isBlank() || specialty.isBlank()) return
        val newDoc = DoctorItem(
            id = System.currentTimeMillis().toString(),
            name = name,
            specialty = specialty,
            hours = if (hours.startsWith("🕒")) hours else "🕒 الدوام: $hours"
        )
        _doctors.value = _doctors.value + newDoc
        viewModelScope.launch {
            _eventFlow.emit(DashboardEvent.ShowToast("✅ تم تسجيل الطبيب بالعيادة بنجاح!"))
        }
    }

    fun deleteDoctor(id: String) {
        _doctors.value = _doctors.value.filter { it.id != id }
        viewModelScope.launch {
            _eventFlow.emit(DashboardEvent.ShowToast("🗑️ تم إزالة الطبيب بنجاح"))
        }
    }
}
