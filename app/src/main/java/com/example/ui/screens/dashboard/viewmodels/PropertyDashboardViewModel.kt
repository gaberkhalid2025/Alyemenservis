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

class PropertyDashboardViewModel(
    private val ownerId: String,
    private val dashboardRepository: IDashboardRepository,
    private val productsRepository: IProductsRepository,
    private val ratingsRepository: IRatingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

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
                dashboardRepository.getDashboardStats(ownerId, "PROPERTY").collect { stats ->
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

    fun addProperty(title: String, priceYer: Double, description: String = "", imageUrl: String = "") {
        viewModelScope.launch {
            if (title.isBlank()) return@launch
            val prop = ProductItemEntity(
                ownerId = ownerId,
                title = title,
                priceYer = priceYer,
                description = description,
                imageUrl = imageUrl,
                category = "PROPERTY"
            )
            productsRepository.addProduct(prop).onSuccess {
                _eventFlow.emit(DashboardEvent.ShowToast("تم إدراج العقار بنجاح 🏠"))
            }
        }
    }

    fun deleteProperty(id: String) {
        viewModelScope.launch {
            productsRepository.deleteProduct(id).onSuccess {
                _eventFlow.emit(DashboardEvent.ShowToast("تم حذف العقار 🗑️"))
            }
        }
    }
}
