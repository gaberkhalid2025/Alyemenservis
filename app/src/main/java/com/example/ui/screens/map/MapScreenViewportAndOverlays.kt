package com.example.ui.screens.map

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BookingEntity
import com.example.data.PropertyEntity
import com.example.data.ProviderEntity
import com.example.data.StoreEntity
import com.example.ui.MainViewModel
import com.example.ui.screens.map.components.ArVpsCameraOverlay
import com.example.ui.screens.map.components.RadarRenderer
import com.example.ui.screens.map.utils.MapJsonBuilder
import com.example.utils.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 🗺️ MapScreenViewportAndOverlays
 * عرض الخريطة الرئيسي (AR VPS / Radar / 3D / Leaflet)، وتفويض عناصر التحكم العائمة
 * والشريط العلوي وبطاقات المحطات إلى MapViewportFloatingControls وMapScreenHudAndSearchOverlays وMapWaypointsAndBottomCards
 */
@Composable
internal fun BoxScope.MapScreenViewport(
    context: Context,
    coroutineScope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
    viewModel: MainViewModel,
    state: MapScreenState,
    filteredProviders: List<ProviderEntity>,
    filteredStores: List<StoreEntity>,
    filteredProperties: List<PropertyEntity>,
    bookings: List<BookingEntity>,
    safeUserLat: Double,
    safeUserLng: Double,
    hasRealLocation: Boolean,
    is3DAvailable: Boolean,
    use3DCssFallback: Boolean,
    onFallback3DCss: (String, Boolean) -> Unit,
    hybridLabels: Boolean,
    onToggleHybridLabels: () -> Unit,
    isPinPickMode: Boolean,
    onMapPicked: (Double, Double) -> Unit,
    onCancelPinPick: () -> Unit,
    centerCoords: Pair<Double, Double>?,
    onCenterChanged: (Double, Double) -> Unit,
    searchedPlace: RealValhallaNavigator.SearchAddressResult?,
    pinnedPoint: Pair<Double, Double>?,
    areTilesFailing: Boolean,
    onTilesStatus: (Boolean) -> Unit,
    gpsWeak: Boolean,
    accuracyForBanner: Float,
    isEcoMode: Boolean,
    isManualLoc: Boolean,
    isTilesDownloaded: Boolean,
    valhallaSizeMb: Int,
    groupMembers: List<GroupMember>,
    showZones: Boolean,
    coverageRefreshTick: Int,
    quakesJson: String?,
    rainUrl: String?,
    savedAddresses: List<LandmarkAddress>,
    sosPending: Boolean = false,
    roadReports: List<RoadReportEntity> = emptyList(),
    onSelectOsmPoi: (RealValhallaNavigator.SearchAddressResult) -> Unit = {},
    onOpenLandmarkAddress: () -> Unit,
    onOpenSavedAddresses: () -> Unit
) {
    val osmPoisJson = rememberViewportOsmPoisJson(
        context = context,
        poiLat = centerCoords?.first ?: safeUserLat,
        poiLng = centerCoords?.second ?: safeUserLng,
        hybridLabels = hybridLabels,
        state = state
    )

    if (state.currentMapMode == MapMode.AR_VPS) {
        ArVpsCameraOverlay(
            nearbyProviders = filteredProviders,
            userLat = safeUserLat,
            userLng = safeUserLng,
            hasUserLocation = hasRealLocation,
            onProviderSelected = { state.selectedEntity = it },
            onClose = { state.currentMapMode = MapMode.SATELLITE },
            modifier = Modifier.fillMaxSize()
        )
    } else if (state.isRadarMode) {
        RadarRenderer(
            items = remember(filteredProviders, filteredStores, filteredProperties, safeUserLat, safeUserLng, state.maxRangeKm, hasRealLocation) {
                MapScreenFilters.buildRadarPoints(
                    filteredProviders = filteredProviders,
                    filteredStores = filteredStores,
                    filteredProperties = filteredProperties,
                    safeUserLat = safeUserLat,
                    safeUserLng = safeUserLng,
                    maxRangeKm = state.maxRangeKm,
                    hasUser = hasRealLocation
                )
            },
            selectedItemId = when (val ent = state.selectedEntity) {
                is ProviderEntity -> ent.id
                is StoreEntity -> ent.id
                is PropertyEntity -> ent.id
                else -> null
            },
            onItemSelected = { item ->
                state.selectedEntity = item.originalItem
            },
            isHeatmapActive = state.isHeatmapActive,
            maxRangeKm = state.maxRangeKm,
            modifier = Modifier.fillMaxSize()
        )
        if (!hasRealLocation) {
            Text(
                "الرادار يحتاج موقعك الحقيقي — فعّل GPS",
                color = Color(0xFFF59E0B),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color(0xCC0F172A), RoundedCornerShape(12.dp))
                    .padding(16.dp)
            )
        }
    } else if (state.is3DTerrainEnabled && is3DAvailable && !use3DCssFallback) {
        val markersJson3D = remember(
            filteredProviders,
            filteredStores,
            filteredProperties,
            state.dynamicOffsets.toMap(),
            safeUserLat,
            safeUserLng
        ) {
            MapJsonBuilder.buildMarkersJson(
                nearbyProviders = filteredProviders.take(MapJsonBuilder.MAX_PER_TYPE),
                nearbyStores = filteredStores.take(MapJsonBuilder.MAX_PER_TYPE),
                nearbyProperties = filteredProperties.take(MapJsonBuilder.MAX_PER_TYPE),
                dynamicOffsets = state.dynamicOffsets,
                safeUserLat = safeUserLat,
                safeUserLng = safeUserLng
            )
        }
        Map3DView(
            centerLat = safeUserLat,
            centerLng = safeUserLng,
            userCoords = if (hasRealLocation) Pair(safeUserLat, safeUserLng) else null,
            markersJson = markersJson3D,
            currentMapMode = state.currentMapMode,
            selectedCity = state.selectedCity,
            hybridLabels = hybridLabels,
            gibsEnabled = state.gibsEnabled,
            gibsProduct = state.gibsProduct,
            eoxEnabled = state.eoxEnabled,
            realTimeSatEnabled = state.realTimeSatEnabled,
            realTimeSatFeed = state.realTimeSatFeed,
            offlineOnly = state.mapLayer == MapLayer.VALHALLA_OFFLINE,
            pickMode = isPinPickMode,
            focusPoint = searchedPlace?.let { Triple(it.shortName, it.latitude, it.longitude) }
                ?: pinnedPoint?.let { Triple("📌 نقطة محددة", it.first, it.second) },
            zoomInTick = state.zoomInTick,
            zoomOutTick = state.zoomOutTick,
            recenterTick = state.recenterTick,
            onMarkerClicked = { id, type ->
                when (type) {
                    "PROVIDER" -> filteredProviders.find { it.id == id }?.let { state.selectedEntity = it }
                    "STORE", "RESTAURANT", "MEDICAL" -> filteredStores.find { it.id == id }?.let { state.selectedEntity = it }
                    "PROPERTY" -> filteredProperties.find { it.id == id }?.let { state.selectedEntity = it }
                }
            },
            onDeselect = { state.selectedEntity = null },
            onMapPicked = onMapPicked,
            onMapLongPressed = onMapPicked,
            onMapCenterChanged = onCenterChanged,
            onFallbackCss3D = { reason -> onFallback3DCss(reason, false) },
            onUnavailable = { reason -> onFallback3DCss(reason, true) },
            modifier = Modifier.fillMaxSize()
        )
    } else {
        RealLeafletMapView(
            userCoords = Pair(safeUserLat, safeUserLng),
            nearbyProviders = filteredProviders,
            nearbyStores = filteredStores,
            nearbyProperties = filteredProperties,
            dynamicOffsets = state.dynamicOffsets,
            currentMapMode = state.currentMapMode,
            selectedCity = state.selectedCity,
            tilt = if (state.is3DTerrainEnabled) 55f else 0f,
            zoomScale = state.zoomScale,
            onZoomScaleChange = { state.zoomScale = it },
            panOffset = state.panOffset,
            onPanOffsetChange = { state.panOffset = it },
            selectedEntity = state.selectedEntity,
            onProviderSelected = { state.selectedEntity = it },
            onStoreSelected = { state.selectedEntity = it },
            onPropertySelected = { state.selectedEntity = it },
            onDeselect = { state.selectedEntity = null },
            onSwitchToRadar = { state.isRadarMode = true },
            onTilesStatus = onTilesStatus,
            hybridLabels = hybridLabels,
            osmPoisJson = osmPoisJson,
            onOsmPoiSelected = { name, kind, lat, lng ->
                val ic = when (kind) {
                    "مسجد" -> "🕌"
                    "مدرسة" -> "🏫"
                    "جامعة" -> "🎓"
                    "صالة أفراح" -> "💒"
                    "مستشفى", "عيادة" -> "🏥"
                    "صيدلية" -> "💊"
                    "مطعم" -> "🍴"
                    "كافيه" -> "☕"
                    "سوق" -> "🛒"
                    "بنك" -> "🏦"
                    "محطة وقود" -> "⛽"
                    "فندق" -> "🏨"
                    "حي" -> "🏘️"
                    else -> "📍"
                }
                onSelectOsmPoi(
                    RealValhallaNavigator.SearchAddressResult(
                        displayName = if (kind.isNotBlank()) "$name ($kind)" else name,
                        shortName = name,
                        latitude = lat,
                        longitude = lng,
                        type = "osm_poi",
                        icon = ic
                    )
                )
            },
            nightMode = state.nightMode,
            gibsEnabled = state.gibsEnabled,
            gibsProduct = state.gibsProduct,
            onGibsStatus = { ok ->
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        if (ok) "🛰️ تم تحميل صور ناسا الفضائية لليوم بنجاح"
                        else "تعذر تحميل بيانات ناسا لليوم (جاري المحاولة بالتاريخ السابق)"
                    )
                }
            },
            zoomInTick = state.zoomInTick,
            zoomOutTick = state.zoomOutTick,
            useZoomTicks = true,
            focusPoint = searchedPlace?.let { Triple(it.shortName, it.latitude, it.longitude) }
                ?: pinnedPoint?.let { Triple("📌 نقطة محددة", it.first, it.second) },
            pickMode = isPinPickMode,
            ecoMode = isEcoMode,
            onMapPicked = onMapPicked,
            onMapLongPressed = onMapPicked,
            onMapCenterChanged = onCenterChanged,
            waypointMarkers = remember(groupMembers, roadReports) {
                val memberMarkers = groupMembers.mapNotNull { m ->
                    val lat = m.lastLat
                    val lng = m.lastLng
                    if (!m.expired && lat != null && lng != null && LandmarkAddress.isValid(lat, lng)) {
                        Triple("👥 ${m.label}", lat, lng)
                    } else null
                }
                val reportMarkers = roadReports.map { r ->
                    Triple(r.title.ifBlank { "🚧 بلاغ طريق" }, r.latitude, r.longitude)
                }
                memberMarkers + reportMarkers
            },
            zones = remember(
                showZones,
                state.reliabilityEnabled,
                state.peakEnabled,
                state.coverageEnabled,
                state.hazardEnabled,
                coverageRefreshTick,
                filteredProviders,
                filteredStores,
                bookings,
                roadReports
            ) {
                val combined = mutableListOf<MapZone>()
                if (showZones || state.reliabilityEnabled) {
                    combined.addAll(ReliabilityZones.build(filteredProviders, filteredStores))
                }
                if (state.peakEnabled) {
                    val provById = filteredProviders.associateBy { it.id }
                    val stamps = bookings.mapNotNull { b ->
                        val p = provById[b.providerId] ?: return@mapNotNull null
                        val t = if (b.scheduledAt > 0L) b.scheduledAt else b.createdAt
                        if (t > 0L && LandmarkAddress.isValid(p.latitude, p.longitude)) {
                            BookingStamp(p.latitude, p.longitude, t)
                        } else null
                    }
                    if (stamps.isNotEmpty()) {
                        val peakZones = PeakPredictor.predict(stamps, System.currentTimeMillis())
                        combined.addAll(PeakPredictor.toMapZones(peakZones))
                    }
                }
                if (state.coverageEnabled) {
                    combined.addAll(CoverageLog.deadZones(context))
                }
                if (state.hazardEnabled) {
                    combined.addAll(HazardAlerts.roadDifficultyZones(filteredProviders))
                }
                if (roadReports.isNotEmpty()) {
                    combined.addAll(roadReports.map { it.toMapZone() })
                }
                combined
            },
            eoxEnabled = state.eoxEnabled,
            quakesJson = quakesJson,
            rainUrl = rainUrl,
            eumetEnabled = state.eumetEnabled,
            eumetLayer = state.eumetLayerName,
            onOverlayStatus = { name, failing, msg ->
                if (failing && msg.isNotBlank()) {
                    coroutineScope.launch { snackbarHostState.showSnackbar(msg) }
                }
                if (failing && name == "eumet") {
                    state.eumetEnabled = false
                }
            },
            offlineOnly = state.mapLayer == MapLayer.VALHALLA_OFFLINE,
            isValhallaDownloaded = isTilesDownloaded,
            valhallaSizeMb = valhallaSizeMb,
            realTimeSatEnabled = state.realTimeSatEnabled,
            realTimeSatFeed = state.realTimeSatFeed,
            showLabelsOnMap = state.showLabelsOnMap,
            recenterTick = state.recenterTick,
            bottomInsetDp = 24,
            topInsetDp = 0,
            modifier = Modifier.fillMaxSize()
        )

        MapViewportFloatingControls(
            state = state,
            safeUserLat = safeUserLat,
            safeUserLng = safeUserLng,
            isPinPickMode = isPinPickMode,
            centerCoords = centerCoords,
            areTilesFailing = areTilesFailing,
            onTilesStatus = onTilesStatus,
            gpsWeak = gpsWeak,
            accuracyForBanner = accuracyForBanner,
            sosPending = sosPending,
            savedAddresses = savedAddresses,
            onMapPicked = onMapPicked,
            onCancelPinPick = onCancelPinPick,
            onOpenLandmarkAddress = onOpenLandmarkAddress,
            onOpenSavedAddresses = onOpenSavedAddresses
        )
    }
}
