package com.example.ui.screens.dashboard.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repositories.IDashboardRepository
import com.example.data.repositories.IProductsRepository
import com.example.data.repositories.IRatingsRepository
import com.example.domain.entities.ProductItemEntity
import com.example.ui.screens.dashboard.DashboardEvent
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class DoctorItem(
    val id: String = "",
    val name: String = "",
    val specialty: String = "",
    val workingHours: String = ""
)

class MedicalDashboardViewModel(
    private val ownerId: String,
    private val dashboardRepository: IDashboardRepository,
    private val productsRepository: IProductsRepository,
    private val ratingsRepository: IRatingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val _doctors = MutableStateFlow<List<DoctorItem>>(emptyList())
    val doctors: StateFlow<List<DoctorItem>> = _doctors.asStateFlow()

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
                dashboardRepository.getDashboardStats(ownerId, "MEDICAL").collect { stats ->
                    _uiState.value = _uiState.value.copy(stats = stats, isLoading = false)
                }
            }

            launch {
                productsRepository.getOwnerProducts(ownerId).collect { prods ->
                    _uiState.value = _uiState.value.copy(products = prods)
                }
            }

            launch {
                ratingsRepository.getTargetRatings(ownerId).collect { revs ->
                    _uiState.value = _uiState.value.copy(reviews = revs)
                }
            }
        }
    }

    fun addDoctor(name: String, specialty: String, workingHours: String) {
        if (name.isBlank()) return
        val doc = DoctorItem(
            id = System.currentTimeMillis().toString(),
            name = name,
            specialty = specialty,
            workingHours = workingHours
        )
        _doctors.value = _doctors.value + doc
        viewModelScope.launch {
            _eventFlow.emit(DashboardEvent.ShowToast("تمت إضافة الطبيب بنجاح 🩺"))
        }
    }

    fun deleteDoctor(id: String) {
        _doctors.value = _doctors.value.filter { it.id != id }
        viewModelScope.launch {
            _eventFlow.emit(DashboardEvent.ShowToast("تم حذف الطبيب 🗑️"))
        }
    }

    fun addMedicalService(title: String, priceYer: Double, description: String = "") {
        viewModelScope.launch {
            if (title.isBlank()) return@launch
            val service = ProductItemEntity(
                ownerId = ownerId,
                title = title,
                priceYer = priceYer,
                description = description,
                category = "MEDICAL"
            )
            productsRepository.addProduct(service).onSuccess {
                _eventFlow.emit(DashboardEvent.ShowToast("تمت إضافة الخدمة الطبية 💊"))
            }
        }
    }
}
