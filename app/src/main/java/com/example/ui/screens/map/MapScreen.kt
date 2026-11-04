@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.screens.map
import com.example.ui.*

import androidx.compose.runtime.*
import com.example.data.PropertyEntity
import com.example.data.ProviderEntity
import com.example.data.StoreEntity
import com.example.ui.MainViewModel
import com.example.utils.VisualThemePalette
import com.example.utils.resolveThemePalette

/**
 * 🗺️ MapScreen
 * Modern, modular, high-performance Map Screen delegating to dedicated sub-modules:
 * - MapScreenContent (Primary presentation layout)
 * - MapScreenState (State container)
 * - MapScreenFilters (City & category filter logic)
 * - RadarRenderer (Canvas radar with dual pulses & sweep animation)
 * - RealLeafletMapView (OSM Tiles + Clustering + Offline Cache)
 * - LandmarkAddressScreen (Visual Landmark Addresses for GPS jamming resilience)
 * - Map3DView (MapLibre GL 3D vector map)
 */
@Composable
fun MapScreen(
    viewModel: MainViewModel,
    onBackClick: () -> Unit = {},
    onOpenProviderDetails: (ProviderEntity) -> Unit = {},
    onOpenStoreDetails: (StoreEntity) -> Unit = {},
    onOpenPropertyDetails: (PropertyEntity) -> Unit = {},
    onRequestBooking: (Any) -> Unit = {},
    themeColors: VisualThemePalette = resolveThemePalette(viewModel.settings.collectAsState().value)
) {
    val state = rememberMapScreenState()

    MapScreenContent(
        viewModel = viewModel,
        state = state,
        onBackClick = onBackClick,
        onOpenProviderDetails = onOpenProviderDetails,
        onOpenStoreDetails = onOpenStoreDetails,
        onOpenPropertyDetails = onOpenPropertyDetails,
        onRequestBooking = onRequestBooking,
        themeColors = themeColors
    )
}
