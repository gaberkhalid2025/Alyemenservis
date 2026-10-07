package com.example.ui.screens.map

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.AdminSettingsEntity
import com.example.data.PropertyEntity
import com.example.data.ProviderEntity
import com.example.data.StoreEntity
import com.example.ui.screens.map.components.MapBottomSheet
import com.example.ui.screens.map.components.OfflineInteractiveMap
import com.example.utils.VisualThemePalette
import com.example.utils.resolveThemePalette

/**
 * 🗺️ RealLeafletMapView (Native High-Performance Offline Map Engine)
 * Replaced heavy WebViews & Leaflet JS with 100% Native Jetpack Compose Canvas vector map.
 * Zero black screens, instant load, ultra-low memory, free & completely offline.
 */
@Composable
fun RealLeafletMapView(
    userCoords: Pair<Double, Double>,
    nearbyProviders: List<ProviderEntity>,
    nearbyStores: List<StoreEntity>,
    nearbyProperties: List<PropertyEntity>,
    dynamicOffsets: Map<String, Pair<Double, Double>>,
    selectedCity: String = "الكل",
    zoomScale: Float = 1.0f,
    onZoomScaleChange: (Float) -> Unit = {},
    panOffset: Offset = Offset.Zero,
    onPanOffsetChange: (Offset) -> Unit = {},
    selectedEntity: Any? = null,
    onProviderSelected: (ProviderEntity) -> Unit,
    onStoreSelected: (StoreEntity) -> Unit,
    onPropertySelected: (PropertyEntity) -> Unit,
    onDeselect: () -> Unit = {},
    onSwitchToRadar: (() -> Unit)? = null,
    onMapLoadFailed: (() -> Unit)? = null,
    themeColors: VisualThemePalette = resolveThemePalette(AdminSettingsEntity()),
    modifier: Modifier = Modifier
) {
    var currentSelectedEntity by remember(selectedEntity) { mutableStateOf<Any?>(selectedEntity) }

    val safeUserLat = if (userCoords.first != 0.0 && !userCoords.first.isNaN()) userCoords.first else 15.3694
    val safeUserLng = if (userCoords.second != 0.0 && !userCoords.second.isNaN()) userCoords.second else 44.1910

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        // الخريطة التفاعلية الأصلية بـ Jetpack Compose الخفيفة والسريعة
        OfflineInteractiveMap(
            userCoords = Pair(safeUserLat, safeUserLng),
            nearbyProviders = nearbyProviders,
            nearbyStores = nearbyStores,
            nearbyProperties = nearbyProperties,
            dynamicOffsets = dynamicOffsets,
            selectedCity = selectedCity,
            zoomScale = zoomScale,
            onZoomScaleChange = onZoomScaleChange,
            panOffset = panOffset,
            onPanOffsetChange = onPanOffsetChange,
            selectedEntity = currentSelectedEntity,
            onProviderSelected = {
                currentSelectedEntity = it
                onProviderSelected(it)
            },
            onStoreSelected = {
                currentSelectedEntity = it
                onStoreSelected(it)
            },
            onPropertySelected = {
                currentSelectedEntity = it
                onPropertySelected(it)
            },
            onDeselect = {
                currentSelectedEntity = null
                onDeselect()
            },
            onSwitchToRadar = onSwitchToRadar,
            modifier = Modifier.fillMaxSize()
        )

        // تفاصيل العنصر المحدد في الأسفل
        currentSelectedEntity?.let { entity ->
            MapBottomSheet(
                entity = entity,
                userLat = safeUserLat,
                userLng = safeUserLng,
                onDismiss = {
                    currentSelectedEntity = null
                    onDeselect()
                },
                onRequestBooking = { selectedItem ->
                    when (selectedItem) {
                        is ProviderEntity -> onProviderSelected(selectedItem)
                        is StoreEntity -> onStoreSelected(selectedItem)
                        is PropertyEntity -> onPropertySelected(selectedItem)
                    }
                },
                onOpenDetails = { selectedItem ->
                    when (selectedItem) {
                        is ProviderEntity -> onProviderSelected(selectedItem)
                        is StoreEntity -> onStoreSelected(selectedItem)
                        is PropertyEntity -> onPropertySelected(selectedItem)
                    }
                },
                themeColors = themeColors,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            )
        }
    }
}

object MapAssetMemoryCache {
    fun isCached(): Boolean = true
    fun invalidate() {}
    fun getOrLoadSync(context: Context): String = ""
    suspend fun getOrLoadAsync(context: Context): String = ""
}
