package com.example.ui.screens.dashboard.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repositories.IDashboardRepository
import com.example.data.repositories.IProductsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 🧠 RestaurantDashboardViewModel - إدارة بيانات لوحة تحكم المطاعم والكافيهات
 */
class RestaurantDashboardViewModel(
    private val restaurantId: String,
    private val dashboardRepository: IDashboardRepository,
    private val productsRepository: IProductsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            launch {
                dashboardRepository.getDashboardStats(restaurantId, "RESTAURANT").collect { stats ->
                    _uiState.value = _uiState.value.copy(stats = stats, isLoading = false)
                }
            }
            launch {
                productsRepository.getOwnerProducts(restaurantId).collect { menu ->
                    _uiState.value = _uiState.value.copy(products = menu)
                }
            }
        }
    }
}
