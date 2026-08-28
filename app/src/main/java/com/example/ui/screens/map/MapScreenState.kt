package com.example.ui.screens.map

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.data.PropertyEntity
import com.example.data.ProviderEntity
import com.example.data.StoreEntity

/**
 * 🗺️ MapScreenState
 * Encapsulates the UI and interaction states for Map and Radar views.
 */
@Stable
class MapScreenState(
    isRadarMode: Boolean = false,
    isHeatmapActive: Boolean = false,
    selectedCategory: String = "ALL",
    selectedCity: String = "الكل",
    searchQuery: String = "",
    maxRangeKm: Float = 25.0f
) {
    var isRadarMode by mutableStateOf(isRadarMode)
    var isHeatmapActive by mutableStateOf(isHeatmapActive)
    var selectedCategory by mutableStateOf(selectedCategory)
    var selectedCity by mutableStateOf(selectedCity)
    var searchQuery by mutableStateOf(searchQuery)
    var maxRangeKm by mutableFloatStateOf(maxRangeKm)

    var selectedEntity by mutableStateOf<Any?>(null)
    var bookingProviderTarget by mutableStateOf<ProviderEntity?>(null)

    val dynamicOffsets = mutableStateMapOf<String, Pair<Double, Double>>()

    fun resetSelection() {
        selectedEntity = null
        bookingProviderTarget = null
    }

    fun clearOffsets() {
        dynamicOffsets.clear()
    }
}

@Composable
fun rememberMapScreenState(): MapScreenState {
    val isRadarMode = rememberSaveable { mutableStateOf(false) }
    val isHeatmapActive = rememberSaveable { mutableStateOf(false) }
    val selectedCategory = rememberSaveable { mutableStateOf("ALL") }
    val selectedCity = rememberSaveable { mutableStateOf("الكل") }
    val searchQuery = rememberSaveable { mutableStateOf("") }
    val maxRangeKm = rememberSaveable { mutableFloatStateOf(25.0f) }

    return remember {
        MapScreenState(
            isRadarMode = isRadarMode.value,
            isHeatmapActive = isHeatmapActive.value,
            selectedCategory = selectedCategory.value,
            selectedCity = selectedCity.value,
            searchQuery = searchQuery.value,
            maxRangeKm = maxRangeKm.floatValue
        )
    }
}
