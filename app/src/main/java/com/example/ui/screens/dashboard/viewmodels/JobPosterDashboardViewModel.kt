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

data class JobPostItem(
    val id: String = "",
    val title: String,
    val salaryYer: Double,
    val location: String,
    val applicantsCount: Int = 0
)

/**
 * 🧠 JobPosterDashboardViewModel - إدارة الشواغر الوظيفية والمتقدمين
 */
class JobPosterDashboardViewModel(
    private val posterId: String,
    private val dashboardRepository: IDashboardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<DashboardEvent>()
    val eventFlow: SharedFlow<DashboardEvent> = _eventFlow.asSharedFlow()

    private val _jobs = MutableStateFlow<List<JobPostItem>>(
        listOf(
            JobPostItem("1", "محاسب مالي خبرة سنتين", 250000.0, "صنعاء - حدة", 8),
            JobPostItem("2", "مهندس صيانة شبكات وهواتف", 300000.0, "عدن - المعلا", 5)
        )
    )
    val jobs: StateFlow<List<JobPostItem>> = _jobs.asStateFlow()

    init {
        viewModelScope.launch {
            dashboardRepository.getDashboardStats(posterId, "JOB").collect { stats ->
                _uiState.value = _uiState.value.copy(stats = stats, isLoading = false)
            }
        }
    }

    fun addJob(title: String, salaryYer: Double, location: String) {
        if (title.isBlank()) return
        val newJob = JobPostItem(
            id = System.currentTimeMillis().toString(),
            title = title,
            salaryYer = salaryYer,
            location = location,
            applicantsCount = 0
        )
        _jobs.value = _jobs.value + newJob
        viewModelScope.launch {
            _eventFlow.emit(DashboardEvent.ShowToast("✅ تم نشر الشاغر الوظيفي بنجاح!"))
        }
    }

    fun deleteJob(id: String) {
        _jobs.value = _jobs.value.filter { it.id != id }
        viewModelScope.launch {
            _eventFlow.emit(DashboardEvent.ShowToast("🗑️ تم حذف الشاغر الوظيفي"))
        }
    }
}
