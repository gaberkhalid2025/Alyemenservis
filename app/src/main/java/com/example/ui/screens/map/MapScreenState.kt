package com.example.ui.screens.map

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
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

    companion object {
        val Saver: Saver<MapScreenState, *> = listSaver(
            save = { listOf(it.isRadarMode, it.isHeatmapActive, it.selectedCategory, it.selectedCity, it.searchQuery, it.maxRangeKm) },
            restore = {
                MapScreenState(
                    isRadarMode = it[0] as Boolean,
                    isHeatmapActive = it[1] as Boolean,
                    selectedCategory = it[2] as String,
                    selectedCity = it[3] as String,
                    searchQuery = it[4] as String,
                    maxRangeKm = (it[5] as Number).toFloat()
                )
            }
        )
    }
}

@Composable
fun rememberMapScreenState(): MapScreenState {
    return rememberSaveable(saver = MapScreenState.Saver) {
        MapScreenState()
    }
}

