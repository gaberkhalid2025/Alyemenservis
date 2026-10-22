package com.example.ui.screens.map

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.data.PropertyEntity
import com.example.data.ProviderEntity
import com.example.data.StoreEntity

/**
 * 🗺️ MapMode
 * الأوضاع الأربعة لمركز التحكم في خرائط Vela:
 * 1. AERIAL_OAM: صور الأقمار الاصطناعية والصور الجوية المجانية بدون مفاتيح (الوضع الافتراضي)
 * 2. VELA_3D: خرائط Vela ثلاثية الأبعاد التفاعلية مع المباني وإمالة الكاميرا
 * 3. AR_VPS: الرؤية الحاسوبية وكاميرا الواقع المعزز (VPS HUD)
 * 4. ALEPH_ASSISTANT: المساعد البصري الذكي "اسأل عن هذا المكان"
 */
enum class MapMode {
    OSM,
    HOT,
    TOPO,
    SATELLITE,
    VELA_3D,
    AR_VPS,
    ALEPH_ASSISTANT
}

/**
 * 🗺️ MapScreenState
 * Encapsulates the UI and interaction states for Map, Vela 3D, Satellite, and AR views.
 */
@Stable
class MapScreenState(
    isRadarMode: Boolean = false,
    isHeatmapActive: Boolean = false,
    selectedCategory: String = "ALL",
    selectedCity: String = "الكل",
    searchQuery: String = "",
    maxRangeKm: Float = 25.0f,
    initialMapMode: MapMode = MapMode.SATELLITE
) {
    var isRadarMode by mutableStateOf(isRadarMode)
    var isHeatmapActive by mutableStateOf(isHeatmapActive)
    var currentMapMode by mutableStateOf(initialMapMode)
    var isAlephAssistantOpen by mutableStateOf(false)
    var showLayerDialog by mutableStateOf(false)
    var selectedCategory by mutableStateOf(selectedCategory)
    var selectedCity by mutableStateOf(selectedCity)
    var searchQuery by mutableStateOf(searchQuery)
    var maxRangeKm by mutableFloatStateOf(maxRangeKm)
    var zoomScale by mutableFloatStateOf(1.0f)
    var panOffset by mutableStateOf(androidx.compose.ui.geometry.Offset.Zero)
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
            save = { 
                listOf(
                    it.isRadarMode, 
                    it.isHeatmapActive, 
                    it.selectedCategory, 
                    it.selectedCity, 
                    it.searchQuery, 
                    it.maxRangeKm,
                    it.currentMapMode.name
                ) 
            },
            restore = {
                val mode = try {
                    MapMode.valueOf(it.getOrNull(6) as? String ?: "SATELLITE")
                } catch (e: Exception) {
                    MapMode.SATELLITE
                }
                MapScreenState(
                    isRadarMode = it[0] as Boolean,
                    isHeatmapActive = it[1] as Boolean,
                    selectedCategory = it[2] as String,
                    selectedCity = it[3] as String,
                    searchQuery = it[4] as String,
                    maxRangeKm = (it[5] as Number).toFloat(),
                    initialMapMode = mode
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
