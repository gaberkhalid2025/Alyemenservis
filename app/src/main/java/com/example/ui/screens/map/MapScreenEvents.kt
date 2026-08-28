package com.example.ui.screens.map

import com.example.data.PropertyEntity
import com.example.data.ProviderEntity
import com.example.data.StoreEntity

/**
 * 🗺️ MapScreenEvents
 * User events occurring on the Map and Radar interface.
 */
sealed class MapScreenEvents {
    data class OnEntitySelected(val entity: Any?) : MapScreenEvents()
    data class OnCategorySelected(val category: String) : MapScreenEvents()
    data class OnCitySelected(val city: String) : MapScreenEvents()
    data class OnSearchQueryChanged(val query: String) : MapScreenEvents()
    data class OnToggleRadarMode(val isRadar: Boolean) : MapScreenEvents()
    data class OnToggleHeatmap(val isHeatmap: Boolean) : MapScreenEvents()
    data class OnRequestBooking(val provider: ProviderEntity) : MapScreenEvents()
    object OnDismissBottomSheet : MapScreenEvents()
    object OnDismissBookingDialog : MapScreenEvents()
    data class OnConfirmBooking(val notes: String) : MapScreenEvents()
}
