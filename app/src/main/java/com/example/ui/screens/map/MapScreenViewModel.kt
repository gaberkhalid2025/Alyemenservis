package com.example.ui.screens.map

import androidx.lifecycle.ViewModel
import com.example.ui.*
import androidx.lifecycle.viewModelScope
import com.example.data.PropertyEntity
import com.example.data.ProviderEntity
import com.example.data.StoreEntity
import kotlinx.coroutines.flow.*

/**
 * 🗺️ MapScreenViewModel
 * Specialized ViewModel handling map entities, filtering, and live GPS state.
 */
class MapScreenViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<MapScreenUiState>(MapScreenUiState.Loading)
    val uiState: StateFlow<MapScreenUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCity = MutableStateFlow("الكل")
    val selectedCity: StateFlow<String> = _selectedCity.asStateFlow()

    private val _selectedCategory = MutableStateFlow("ALL")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateCity(city: String) {
        _selectedCity.value = city
    }

    fun updateCategory(category: String) {
        _selectedCategory.value = category
    }

    /**
     * تصفية المزودين بناءً على حالة ViewModel الحالية (المدينة، التصنيف، والبحث)
     */
    fun filterProviders(providers: List<ProviderEntity>): List<ProviderEntity> {
        return MapScreenFilters.filterProviders(
            providers = providers,
            selectedCategory = _selectedCategory.value,
            selectedCity = _selectedCity.value,
            searchQuery = _searchQuery.value
        )
    }

    /**
     * تصفية المتاجر والمطاعم والمراكز الطبية
     */
    fun filterStores(stores: List<StoreEntity>): List<StoreEntity> {
        return MapScreenFilters.filterStores(
            stores = stores,
            selectedCategory = _selectedCategory.value,
            selectedCity = _selectedCity.value,
            searchQuery = _searchQuery.value
        )
    }

    /**
     * تصفية العقارات
     */
    fun filterProperties(properties: List<PropertyEntity>): List<PropertyEntity> {
        return MapScreenFilters.filterProperties(
            properties = properties,
            selectedCategory = _selectedCategory.value,
            selectedCity = _selectedCity.value,
            searchQuery = _searchQuery.value
        )
    }

    fun resetFilters() {
        _searchQuery.value = ""
        _selectedCity.value = "الكل"
        _selectedCategory.value = "ALL"
    }

    fun onEvent(event: MapScreenEvents) {
        when (event) {
            is MapScreenEvents.OnSearchQueryChanged -> updateSearchQuery(event.query)
            is MapScreenEvents.OnCitySelected -> updateCity(event.city)
            is MapScreenEvents.OnCategorySelected -> updateCategory(event.category)
            else -> { /* Delegated to UI or Repository */ }
        }
    }
}
