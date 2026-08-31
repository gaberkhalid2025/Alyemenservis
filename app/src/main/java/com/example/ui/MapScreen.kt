package com.example.ui

import androidx.compose.runtime.Composable
import com.example.data.PropertyEntity
import com.example.data.ProviderEntity
import com.example.data.StoreEntity
import com.example.ui.screens.map.MapScreen as ModularMapScreen
import com.example.utils.VisualThemePalette
import com.example.utils.resolveThemePalette

/**
 * 🗺️ MapScreen (Root UI delegator to modular MapScreen)
 */
@Composable
fun MapScreen(
    viewModel: MainViewModel,
    onBackClick: () -> Unit = {},
    onOpenProviderDetails: (ProviderEntity) -> Unit = {},
    onOpenStoreDetails: (StoreEntity) -> Unit = {},
    onOpenPropertyDetails: (PropertyEntity) -> Unit = {},
    onRequestBooking: (ProviderEntity) -> Unit = {}
) {
    ModularMapScreen(
        viewModel = viewModel,
        onBackClick = onBackClick,
        onOpenProviderDetails = onOpenProviderDetails,
        onOpenStoreDetails = onOpenStoreDetails,
        onOpenPropertyDetails = onOpenPropertyDetails,
        onRequestBooking = onRequestBooking
    )
}
