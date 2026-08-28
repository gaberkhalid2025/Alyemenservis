package com.example.ui.screens.map

import com.example.data.PropertyEntity
import com.example.data.ProviderEntity
import com.example.data.StoreEntity
import com.example.ui.screens.map.components.MarkerRenderer

/**
 * 🗺️ MapScreenUiState
 * Comprehensive UI State model for Map and Radar screens.
 */
sealed class MapScreenUiState {
    object Loading : MapScreenUiState()
    
    data class Success(
        val providers: List<ProviderEntity> = emptyList(),
        val stores: List<StoreEntity> = emptyList(),
        val properties: List<PropertyEntity> = emptyList(),
        val radarPoints: List<MarkerRenderer.MapItemPoint> = emptyList(),
        val userLat: Double = 15.3694,
        val userLng: Double = 44.1910,
        val isRadarMode: Boolean = false,
        val isHeatmapActive: Boolean = false,
        val selectedCategory: String = "ALL",
        val selectedCity: String = "الكل",
        val searchQuery: String = "",
        val selectedEntity: Any? = null,
        val bookingProviderTarget: ProviderEntity? = null
    ) : MapScreenUiState()

    data class Error(val message: String) : MapScreenUiState()
    
    object Empty : MapScreenUiState()
}
