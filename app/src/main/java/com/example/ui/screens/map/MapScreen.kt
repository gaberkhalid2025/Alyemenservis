@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.screens.map

import androidx.compose.runtime.*
import com.example.viewmodels.AdminViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.PropertyEntity
import com.example.data.ProviderEntity
import com.example.data.StoreEntity

import com.example.utils.VisualThemePalette
import com.example.utils.resolveThemePalette

/**
 * 🗺️ MapScreen
 * Modern, modular, high-performance Map Screen delegating to dedicated sub-modules:
 * - MapScreenContent (Primary presentation layout)
 * - MapScreenState (State container)
 * - MapScreenFilters (City & category filter logic)
 * - MapScreenUiState & MapScreenEvents (MVI architecture)
 * - RadarRenderer (Canvas radar with dual pulses & sweep animation)
 * - RealLeafletMapView (OSM Tiles + Clustering + Offline Cache)
 */
@Composable
fun MapScreen(
    adminViewModel: AdminViewModel = viewModel(),
    onBackClick: () -> Unit = {},
    onOpenProviderDetails: (ProviderEntity) -> Unit = {},
    onOpenStoreDetails: (StoreEntity) -> Unit = {},
    onOpenPropertyDetails: (PropertyEntity) -> Unit = {},
    onRequestBooking: (ProviderEntity) -> Unit = {},
    themeColors: VisualThemePalette = resolveThemePalette(adminViewModel.settings.collectAsState().value)
) {
    val state = rememberMapScreenState()

    MapScreenContent(
        
        state = state,
        onBackClick = onBackClick,
        onOpenProviderDetails = onOpenProviderDetails,
        onOpenStoreDetails = onOpenStoreDetails,
        onOpenPropertyDetails = onOpenPropertyDetails,
        onRequestBooking = onRequestBooking,
        themeColors = themeColors
    )
}
