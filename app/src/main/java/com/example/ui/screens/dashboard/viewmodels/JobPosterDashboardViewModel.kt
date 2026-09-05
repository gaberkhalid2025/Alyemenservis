package com.example.ui.screens.dashboard.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repositories.IDashboardRepository
import com.example.data.repositories.IProductsRepository
import com.example.domain.entities.ProductItemEntity
import com.example.ui.screens.dashboard.DashboardEvent
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class JobPostItem(
    val id: String = "",
    val title: String = "",
    val companyName: String = "",
    val salary: String = "",
    val requirements: String = "",
    val applicantsCount: Int = 0
)

class JobPosterDashboardViewModel(
    private val ownerId: String,
    private val dashboardRepository: IDashboardRepository,
    private val productsRepository: IProductsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val _jobs = MutableStateFlow<List<JobPostItem>>(emptyList())
    val jobs: StateFlow<List<JobPostItem>> = _jobs.asStateFlow()

    private val _eventFlow = MutableSharedFlow<DashboardEvent>()
    val eventFlow: SharedFlow<DashboardEvent> = _eventFlow.asSharedFlow()

    init {
        loadDashboardData()
    }

    fun selectTab(tabIndex: Int) {
        _uiState.value = _uiState.value.copy(activeTab = tabIndex)
    }

    fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            launch {
                dashboardRepository.getDashboardStats(ownerId, "JOB").collect { stats ->
                    _uiState.value = _uiState.value.copy(stats = stats, isLoading = false)
                }
            }

            launch {
                productsRepository.getOwnerProducts(ownerId).collect { prods ->
                    _uiState.value = _uiState.value.copy(products = prods)
                }
            }
        }
    }

    fun postJob(title: String, company: String, salary: String, requirements: String) {
        if (title.isBlank()) return
        val item = JobPostItem(
            id = System.currentTimeMillis().toString(),
            title = title,
            companyName = company,
            salary = salary,
            requirements = requirements,
            applicantsCount = 0
        )
        _jobs.value = _jobs.value + item
        viewModelScope.launch {
            _eventFlow.emit(DashboardEvent.ShowToast("تم نشر الشاغر الوظيفي بنجاح 💼"))
        }
    }

    fun deleteJob(id: String) {
        _jobs.value = _jobs.value.filter { it.id != id }
        viewModelScope.launch {
            _eventFlow.emit(DashboardEvent.ShowToast("تم حذف إعلان الوظيفة 🗑️"))
        }
    }
}
