package com.example.ui.screens.map

import androidx.compose.runtime.Composable
import com.example.data.PropertyEntity
import com.example.data.ProviderEntity
import com.example.data.StoreEntity

import com.example.ui.MapScreen

/**
 * 🗺️ MapScreenLayout
 * واجهة تخطيط الخريطة التفاعلية التي تدمج شاشة الخريطة ونظام الرادار والـ Leaflet المباشر
 */
@Composable
fun MapScreenLayout(
    viewModel: AuthViewModel,
    onBackClick: () -> Unit = {},
    onOpenProviderDetails: (ProviderEntity) -> Unit = {},
    onOpenStoreDetails: (StoreEntity) -> Unit = {},
    onOpenPropertyDetails: (PropertyEntity) -> Unit = {},
    onRequestBooking: (ProviderEntity) -> Unit = {}
) {
    MapScreen(
        viewModel = viewModel,
        onBackClick = onBackClick,
        onOpenProviderDetails = onOpenProviderDetails,
        onOpenStoreDetails = onOpenStoreDetails,
        onOpenPropertyDetails = onOpenPropertyDetails,
        onRequestBooking = onRequestBooking
    )
}
