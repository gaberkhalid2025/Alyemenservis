package com.example.ui.screens.dashboard.viewmodels

import androidx.lifecycle.ViewModel
import com.example.ui.*
import androidx.lifecycle.viewModelScope
import com.example.data.repositories.IDashboardRepository
import com.example.data.repositories.IProductsRepository
import com.example.domain.entities.ProductItemEntity
import com.example.ui.screens.dashboard.DashboardEvent
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.flow.catch

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
    private val productsRepository: IProductsRepository,
    private val jobRepository: com.example.data.repositories.JobRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val _jobs = MutableStateFlow<List<JobPostItem>>(emptyList())
    val jobs: StateFlow<List<JobPostItem>> = _jobs.asStateFlow()

    private val _eventFlow = MutableSharedFlow<DashboardEvent>()
    val eventFlow: SharedFlow<DashboardEvent> = _eventFlow.asSharedFlow()

    init {
        loadDashboardData()
        viewModelScope.launch {
            jobRepository.getJobs(ownerId).collect {
                _jobs.value = it
            }
        }
    }

    fun selectTab(tabIndex: Int) {
        _uiState.value = _uiState.value.copy(activeTab = tabIndex)
    }

    fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            supervisorScope {
                launch {
                    dashboardRepository.getDashboardStats(ownerId, "JOB")
                        .catch { e -> 
                            _eventFlow.emit(DashboardEvent.ShowToast(e.message ?: "خطأ في الإحصائيات")) 
                            _uiState.value = _uiState.value.copy(isLoading = false)
                        }
                        .collect { stats ->
                            _uiState.value = _uiState.value.copy(stats = stats, isLoading = false)
                        }
                }
                launch {
                    productsRepository.getOwnerProducts(ownerId)
                        .catch { e -> _eventFlow.emit(DashboardEvent.ShowToast(e.message ?: "خطأ في المنتجات")) }
                        .collect { prods ->
                            _uiState.value = _uiState.value.copy(products = prods)
                        }
                }
            }
        }
    }

    fun postJob(title: String, company: String, salary: String, requirements: String) {
        if (title.isBlank()) return
        
        val jobId = java.util.UUID.randomUUID().toString()
        val existing = _jobs.value.find { it.title == title && it.companyName == company }
        if (existing != null) {
            viewModelScope.launch {
                _eventFlow.emit(DashboardEvent.ShowToast("الوظيفة موجودة مسبقاً ⚠️"))
            }
            return
        }

        val item = JobPostItem(
            id = jobId,
            title = title,
            companyName = company,
            salary = salary,
            requirements = requirements,
            applicantsCount = 0
        )
        viewModelScope.launch {
            jobRepository.postJob(ownerId, item).onSuccess {
                _eventFlow.emit(DashboardEvent.ShowToast("تم نشر الشاغر الوظيفي بنجاح 💼"))
            }.onFailure {
                _eventFlow.emit(DashboardEvent.ShowToast("حدث خطأ أثناء النشر"))
            }
        }
    }

    fun deleteJob(id: String) {
        viewModelScope.launch {
            jobRepository.deleteJob(id).onSuccess {
                _eventFlow.emit(DashboardEvent.ShowToast("تم حذف إعلان الوظيفة 🗑️"))
            }.onFailure {
                _eventFlow.emit(DashboardEvent.ShowToast("حدث خطأ أثناء الحذف"))
            }
        }
    }
}
