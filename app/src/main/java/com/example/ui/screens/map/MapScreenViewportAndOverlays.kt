@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)

package com.example.ui.screens.map

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BookingEntity
import com.example.data.PropertyEntity
import com.example.data.ProviderEntity
import com.example.data.StoreEntity
import com.example.ui.MainViewModel
import com.example.ui.screens.map.components.*
import com.example.ui.screens.map.utils.MapDistanceCalculator
import com.example.ui.screens.map.utils.MapJsonBuilder
import com.example.ui.screens.map.utils.OfflineMapManager
import com.example.utils.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 🗺️ MapScreenViewportAndOverlays
 * عرض الخريطة الرئيسي (AR VPS / Radar / 3D / Leaflet)، شريط الشارات، شريط البحث العلوي ونتائجه،
 * شريط التحديد اليدوي، HUD الليلي، شريط المحطات، وبطاقات التفاصيل السفلية
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
    var osmPoisJson by remember { mutableStateOf<String?>(null) }
    val poiLat = centerCoords?.first ?: safeUserLat
    val poiLng = centerCoords?.second ?: safeUserLng
    val poiGridKey = remember(poiLat, poiLng) {
        if (LandmarkAddress.isValid(poiLat, poiLng)) {
            java.lang.String.format(java.util.Locale.US, "%.3f_%.3f", poiLat, poiLng)
        } else ""
    }
    LaunchedEffect(poiGridKey, hybridLabels, state.mapLayer, state.isRadarMode, state.currentMapMode, state.visibleLandmarkFilters) {
        val showPois = !state.isRadarMode &&
            state.currentMapMode != MapMode.AR_VPS &&
            state.mapLayer != MapLayer.VALHALLA_OFFLINE
        if (!showPois || poiGridKey.isBlank()) {
            osmPoisJson = null
            return@LaunchedEffect
        }

        fun isCategoryAllowed(kind: String): Boolean {
            val filters = state.visibleLandmarkFilters
            return when {
                kind == "شارع" -> filters.contains(MapLandmarkFilter.STREETS)
                kind in listOf("جامع", "مسجد", "دار عبادة") -> filters.contains(MapLandmarkFilter.MOSQUES)
                kind in listOf("مدرسة", "جامعة", "معهد", "كلية") -> filters.contains(MapLandmarkFilter.SCHOOLS)
                kind in listOf("صالة أفراح", "قاعة") -> filters.contains(MapLandmarkFilter.HALLS)
                kind in listOf("مطعم", "كافيه", "بوفيه") -> filters.contains(MapLandmarkFilter.RESTAURANTS)
                kind in listOf("سوق", "مول") -> filters.contains(MapLandmarkFilter.STORES)
                kind in listOf("مستشفى", "عيادة", "صيدلية") -> filters.contains(MapLandmarkFilter.MEDICAL)
                else -> filters.contains(MapLandmarkFilter.PROVIDERS)
            }
        }

        // 1) تحميل المعالم المحلية فوراً (مساجد، صالات، مدارس، مستشفيات، شوارع) لتظهر فوراً على الخريطة حتى بدون إنترنت
        withContext(Dispatchers.IO) {
            val localArr = org.json.JSONArray()
            val seenKeys = mutableSetOf<String>()

            // معالم القاموس الجغرافي اليمني الشامل ضمن النطاق المرئي (حتى 20 كم)
            val nearbyPlaces = OfflineYemenGazetteer.PLACES.filter { p ->
                val dist = HaversinePrecisionNavigator.calculateDistance(poiLat, poiLng, p.latitude, p.longitude)
                dist <= 20_000.0 && isCategoryAllowed(p.category)
            }
            nearbyPlaces.forEach { p ->
                val dedupeKey = "${p.name.trim()}_${java.lang.String.format(java.util.Locale.US, "%.3f_%.3f", p.latitude, p.longitude)}"
                if (seenKeys.add(dedupeKey)) {
                    val (ic, col) = when (p.category) {
                        "جامع", "مسجد" -> "🕌" to "#10B981"
                        "صالة أفراح" -> "💒" to "#EC4899"
                        "مدرسة" -> "🏫" to "#3B82F6"
                        "جامعة", "معهد" -> "🎓" to "#2563EB"
                        "مستشفى", "عيادة" -> "🏥" to "#EF4444"
                        "صيدلية" -> "💊" to "#DC2626"
                        "سوق" -> "🛒" to "#0EA5E9"
                        "صرافة", "بنك" -> "🏦" to "#6366F1"
                        "حي" -> "🏘️" to "#14B8A6"
                        "شارع" -> "🛣️" to "#38BDF8"
                        "جولة" -> "🔄" to "#F59E0B"
                        else -> p.icon to "#64748B"
                    }
                    localArr.put(org.json.JSONObject().apply {
                        put("name", p.name)
                        put("kind", p.category)
                        put("icon", ic)
                        put("color", col)
                        put("lat", p.latitude)
                        put("lng", p.longitude)
                    })
                }
            }

            // المعالم المخصصة المحفوظة محلياً
            val custom = CustomLandmarkStore.nearby(context, poiLat, poiLng, maxDistanceM = 15_000.0).filter { isCategoryAllowed(it.kind) }
            custom.forEach { c ->
                val dedupeKey = "${c.name.trim()}_${java.lang.String.format(java.util.Locale.US, "%.3f_%.3f", c.lat, c.lng)}"
                if (seenKeys.add(dedupeKey)) {
                    localArr.put(org.json.JSONObject().apply {
                        put("name", c.name)
                        put("kind", c.kind)
                        put("icon", "📍")
                        put("color", "#06B6D4")
                        put("lat", c.lat)
                        put("lng", c.lng)
                    })
                }
            }

            if (localArr.length() > 0) {
                withContext(Dispatchers.Main) {
                    osmPoisJson = localArr.toString()
                }
            }

            // 2) إذا توفر الإنترنت، جلب المعالم الإضافية من Overpass في الخلفية ودمجها مع المعالم الحالية
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
            val caps = cm?.getNetworkCapabilities(cm.activeNetwork)
            val hasNet = caps?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
            if (hasNet) {
                try {
                    val remotePois = LandmarkFinder.fetchViewportPois(poiLat, poiLng, radiusM = 2500).filter { isCategoryAllowed(it.kind) }
                    if (remotePois.isNotEmpty()) {
                        val mergedArr = org.json.JSONArray(localArr.toString())
                        val toCache = mutableListOf<RealValhallaNavigator.SearchAddressResult>()
                        remotePois.forEach { p ->
                            val dedupeKey = "${p.name.trim()}_${java.lang.String.format(java.util.Locale.US, "%.3f_%.3f", p.lat, p.lng)}"
                            if (seenKeys.add(dedupeKey)) {
                                val (ic, col) = when (p.kind) {
                                    "مسجد" -> "🕌" to "#10B981"
                                    "مدرسة" -> "🏫" to "#3B82F6"
                                    "جامعة" -> "🎓" to "#2563EB"
                                    "صالة أفراح" -> "💒" to "#EC4899"
                                    "مستشفى", "عيادة" -> "🏥" to "#EF4444"
                                    "صيدلية" -> "💊" to "#DC2626"
                                    "مطعم" -> "🍴" to "#F97316"
                                    "كافيه" -> "☕" to "#D97706"
                                    "سوق" -> "🛒" to "#0EA5E9"
                                    "بنك" -> "🏦" to "#6366F1"
                                    "محطة وقود" -> "⛽" to "#EAB308"
                                    "فندق" -> "🏨" to "#8B5CF6"
                                    "حي" -> "🏘️" to "#14B8A6"
                                    else -> "📍" to "#64748B"
                                }
                                mergedArr.put(org.json.JSONObject().apply {
                                    put("name", p.name)
                                    put("kind", p.kind)
                                    put("icon", ic)
                                    put("color", col)
                                    put("lat", p.lat)
                                    put("lng", p.lng)
                                })
                                toCache.add(
                                    RealValhallaNavigator.SearchAddressResult(
                                        displayName = "${p.name} (${p.kind})",
                                        shortName = p.name,
                                        latitude = p.lat,
                                        longitude = p.lng,
                                        type = "osm_poi",
                                        icon = ic
                                    )
                                )
                            }
                        }
                        if (toCache.isNotEmpty()) {
                            OfflineYemenGazetteer.cacheOnlineResults(context.applicationContext, toCache)
                        }
                        withContext(Dispatchers.Main) {
                            osmPoisJson = mergedArr.toString()
                        }
                    }
                } catch (_: Throwable) {}
            }
        }
    }
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
        Map3DView(
            centerLat = safeUserLat,
            centerLng = safeUserLng,
            userCoords = if (hasRealLocation) Pair(safeUserLat, safeUserLng) else null,
            markersJson = MapJsonBuilder.buildMarkersJson(
                nearbyProviders = filteredProviders.take(MapJsonBuilder.MAX_PER_TYPE),
                nearbyStores = filteredStores.take(MapJsonBuilder.MAX_PER_TYPE),
                nearbyProperties = filteredProperties.take(MapJsonBuilder.MAX_PER_TYPE),
                dynamicOffsets = state.dynamicOffsets,
                safeUserLat = safeUserLat,
                safeUserLng = safeUserLng
            ),
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

        // [نظام خرائط وملاحة عالمي - ملكنا]
        // 🌐 زر كرية زجاجية أنيقة لنمط الخريطة (قياسي / قمر صناعي / تضاريس / أوفلاين) مع قائمة منسدلة
        var showMapTypeDropdown by remember { mutableStateOf(false) }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 74.dp, end = 14.dp)
        ) {
            com.example.ui.theme.GlassIconButton(
                icon = when (state.currentMapMode) {
                    MapMode.SATELLITE, MapMode.SATELLITE_CLARITY -> Icons.Default.SatelliteAlt
                    MapMode.TOPO -> Icons.Default.Terrain
                    else -> Icons.Default.Public
                },
                description = "نمط الخريطة",
                onClick = { showMapTypeDropdown = true },
                active = true,
                tint = when (state.currentMapMode) {
                    MapMode.SATELLITE, MapMode.SATELLITE_CLARITY -> com.example.ui.theme.MapUi.RoyalTop
                    MapMode.TOPO -> Color(0xFFF59E0B)
                    else -> Color(0xFF10B981)
                },
                size = 48.dp,
                modifier = Modifier.testTag("map_type_dropdown_btn")
            )

            DropdownMenu(
                expanded = showMapTypeDropdown,
                onDismissRequest = { showMapTypeDropdown = false },
                containerColor = com.example.ui.theme.MapUi.SurfaceSolid,
                modifier = Modifier
                    .border(1.dp, com.example.ui.theme.MapUi.BorderGlow, RoundedCornerShape(16.dp))
                    .widthIn(min = 210.dp)
            ) {
                val isStandard = (state.currentMapMode == MapMode.OSM || state.currentMapMode == MapMode.VELA) && state.mapLayer != MapLayer.VALHALLA_OFFLINE
                DropdownMenuItem(
                    text = {
                        Text(
                            "خريطة قياسية (شوارع)",
                            fontWeight = if (isStandard) FontWeight.Bold else FontWeight.Normal,
                            color = if (isStandard) Color(0xFF10B981) else Color.White
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Public, contentDescription = null, tint = if (isStandard) Color(0xFF10B981) else Color(0xFF94A3B8))
                    },
                    trailingIcon = if (isStandard) {
                        { Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp)) }
                    } else null,
                    onClick = {
                        state.currentMapMode = MapMode.OSM
                        state.mapLayer = MapLayer.STANDARD
                        showMapTypeDropdown = false
                    }
                )

                val isSat = state.currentMapMode == MapMode.SATELLITE || state.currentMapMode == MapMode.SATELLITE_CLARITY
                DropdownMenuItem(
                    text = {
                        Text(
                            "قمر صناعي (أقمار حقيقية)",
                            fontWeight = if (isSat) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSat) com.example.ui.theme.MapUi.RoyalTop else Color.White
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Default.SatelliteAlt, contentDescription = null, tint = if (isSat) com.example.ui.theme.MapUi.RoyalTop else Color(0xFF94A3B8))
                    },
                    trailingIcon = if (isSat) {
                        { Icon(Icons.Default.Check, contentDescription = null, tint = com.example.ui.theme.MapUi.RoyalTop, modifier = Modifier.size(16.dp)) }
                    } else null,
                    onClick = {
                        state.currentMapMode = MapMode.SATELLITE
                        showMapTypeDropdown = false
                    }
                )

                val isTopo = state.currentMapMode == MapMode.TOPO
                DropdownMenuItem(
                    text = {
                        Text(
                            "خريطة تضاريس (Topo)",
                            fontWeight = if (isTopo) FontWeight.Bold else FontWeight.Normal,
                            color = if (isTopo) Color(0xFFF59E0B) else Color.White
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Terrain, contentDescription = null, tint = if (isTopo) Color(0xFFF59E0B) else Color(0xFF94A3B8))
                    },
                    trailingIcon = if (isTopo) {
                        { Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp)) }
                    } else null,
                    onClick = {
                        state.currentMapMode = MapMode.TOPO
                        showMapTypeDropdown = false
                    }
                )

                val isOffline = state.mapLayer == MapLayer.VALHALLA_OFFLINE
                DropdownMenuItem(
                    text = {
                        Text(
                            "وضع أوفلاين بدون إنترنت",
                            fontWeight = if (isOffline) FontWeight.Bold else FontWeight.Normal,
                            color = if (isOffline) Color(0xFF10B981) else Color.White
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Default.CloudOff, contentDescription = null, tint = if (isOffline) Color(0xFF10B981) else Color(0xFF94A3B8))
                    },
                    trailingIcon = if (isOffline) {
                        { Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp)) }
                    } else null,
                    onClick = {
                        state.mapLayer = if (isOffline) MapLayer.STANDARD else MapLayer.VALHALLA_OFFLINE
                        showMapTypeDropdown = false
                    }
                )

                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                DropdownMenuItem(
                    text = {
                        Text(
                            if (state.showLabelsOnMap) "إخفاء أسماء الشوارع" else "إظهار أسماء الشوارع",
                            color = Color.White
                        )
                    },
                    leadingIcon = {
                        Icon(
                            if (state.showLabelsOnMap) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null,
                            tint = com.example.ui.theme.MapUi.Accent
                        )
                    },
                    onClick = {
                        state.showLabelsOnMap = !state.showLabelsOnMap
                        showMapTypeDropdown = false
                    }
                )

                DropdownMenuItem(
                    text = {
                        Text(
                            "تصفية معالم الخريطة",
                            color = Color.White
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Place, contentDescription = null, tint = com.example.ui.theme.MapUi.Accent)
                    },
                    onClick = {
                        state.showLandmarkFilterSheet = true
                        showMapTypeDropdown = false
                    }
                )
            }
        }

        // ⚠️ حزمة تنبيهات الخريطة المكدسة بانتظام أسفل الشارات لمنع أي تداخل بصري
        val showTilesBanner = areTilesFailing && !state.isRadarMode && state.mapLayer != MapLayer.VALHALLA_OFFLINE
        if (showTilesBanner || gpsWeak || sosPending) {
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 124.dp, start = 16.dp, end = 86.dp)
            ) {
                if (sosPending) {
                    Surface(
                        color = Color(0xFF7F1D1D),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFEF4444)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "🚨 نداء الفزعة محفوظ وقيد الإرسال تلقائياً…",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
                if (showTilesBanner) {
                    Surface(
                        color = Color(0xDD7F1D1D),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CloudOff,
                                contentDescription = null,
                                tint = Color(0xFFFCA5A5),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "تعذر جلب بلاطات الخريطة",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                onClick = {
                                    onTilesStatus(false)
                                    state.reloadTilesTrigger += 1
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("إعادة المحاولة", color = Color(0xFF93C5FD), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                if (gpsWeak) {
                    Surface(
                        color = Color(0xCC7C2D12),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "إشارة GPS ضعيفة (±${accuracyForBanner.toInt()} م). لعنوان دقيق استخدم «عنواني بالمعلم».",
                                color = Color.White,
                                fontSize = 11.5.sp,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = onOpenLandmarkAddress) {
                                Text("فتح", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // 🎯 وضع تحديد نقطة بدبوس المنتصف أو النقر
        if (isPinPickMode) {
            Box(
                modifier = Modifier.align(Alignment.Center),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "مؤشر تحديد النقطة",
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(42.dp).offset(y = (-16).dp)
                )
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00E5FF))
                )
            }

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFF0F172A).copy(alpha = 0.96f),
                border = BorderStroke(1.2.dp, Color(0xFF00E5FF)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 90.dp)
                    .fillMaxWidth()
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val active = centerCoords ?: (safeUserLat to safeUserLng)
                    Text("📌 تحديد نقطة يدوياً على الخريطة", color = Color(0xFF00E5FF), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "حرّك الخريطة لوضع المؤشر على المكان المطلوب أو اضغط مباشرة فوقه (${OfflineYemenGazetteer.formatCoordinates(active.first, active.second)})",
                        color = Color(0xFFCBD5E1),
                        fontSize = 11.5.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { onMapPicked(active.first, active.second) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Text("تثبيت هذه النقطة ✓", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = onCancelPinPick,
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text("إلغاء", color = Color(0xFF94A3B8))
                        }
                    }
                }
            }
        }

        // 🏠 زر كرية زجاجية «عنواني بالمعلم» (في الجانب المقابل لشريط الأدوات لتجنب التداخل)
        com.example.ui.theme.GlassIconButton(
            icon = Icons.Default.HomeWork,
            description = "عنواني بالمعلم",
            onClick = {
                if (savedAddresses.isEmpty()) onOpenLandmarkAddress()
                else onOpenSavedAddresses()
            },
            tint = com.example.ui.theme.MapUi.RoyalTop,
            size = 48.dp,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(start = 16.dp, bottom = 84.dp)
                .testTag("map_landmark_address_orb_btn")
        )
    }
}

@Composable
internal fun BoxScope.MapHudOverlay(
    isDrivingMode: Boolean,
    isHudMirrored: Boolean,
    onToggleMirror: () -> Unit,
    onExitHud: () -> Unit,
    currentSpeedKmh: Int = 0,
    nextDistanceText: String = "",
    nextInstruction: String = "واصل السير على المسار الملاحي"
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.94f))
            .graphicsLayer(
                scaleX = if (isHudMirrored) -1f else 1f,
                scaleY = if (isHudMirrored) -1f else 1f
            )
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Straight,
                contentDescription = null,
                tint = Color(0xFF10B981),
                modifier = Modifier.size(96.dp)
            )
            if (nextDistanceText.isNotBlank()) {
                Text(
                    text = nextDistanceText,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF00E5FF)
                )
            }
            Text(
                text = currentSpeedKmh.coerceAtLeast(0).toString(),
                fontSize = 80.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF10B981)
            )
            Text(
                text = if (isDrivingMode) "كم / ساعة" else "كم / س (مشياً)",
                fontSize = 18.sp,
                color = Color.White
            )
            Text(
                text = nextInstruction,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = onToggleMirror,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (isHudMirrored) "عكس عادي" else "انعكاس الزجاج 🪞", color = Color(0xFF10B981))
            }
            Button(
                onClick = onExitHud,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("خروج من وضع HUD", color = Color.White)
            }
        }
    }
}

@Composable
internal fun BoxScope.MapScreenTopSearchAndResults(
    coroutineScope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
    keyboardController: SoftwareKeyboardController?,
    state: MapScreenState,
    themeColors: VisualThemePalette,
    onBackClick: () -> Unit,
    onVoiceSearchClick: () -> Unit,
    isTilesDownloaded: Boolean,
    onOpenDownloadDialog: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    searchPanelOpen: Boolean,
    onOpenSearchPanel: () -> Unit,
    onCloseSearchPanel: () -> Unit,
    onRetrySearch: () -> Unit,
    isSearching: Boolean,
    searchResults: List<RealValhallaNavigator.SearchAddressResult>,
    searchMessage: String?,
    onSelectSearchResult: (RealValhallaNavigator.SearchAddressResult) -> Unit,
    focusTick: Int = 0,
    safeUserLat: Double = 15.3694,
    safeUserLng: Double = 44.1910,
    hasRealLocation: Boolean = false
) {
    Box(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        MapFilterBar(
            selectedCategory = state.selectedCategory,
            onCategorySelected = { state.selectedCategory = it },
            selectedCity = state.selectedCity,
            onCitySelected = { city -> state.selectedCity = city },
            searchQuery = state.searchQuery,
            onSearchQueryChange = { state.searchQuery = it },
            themeColors = themeColors,
            onBackClick = onBackClick,
            onVoiceSearchClick = onVoiceSearchClick,
            focusTick = focusTick,
            isTilesDownloaded = isTilesDownloaded,
            onDownloadMapsClick = onOpenDownloadDialog,
            onDiagnosticsClick = onOpenDiagnostics,
            onSearchExecute = { query ->
                val q = query.trim()
                if (q.length >= 2) {
                    onOpenSearchPanel()
                    onRetrySearch()
                }
                OfflineMapManager.findCity(q)?.let { city -> state.selectedCity = city.nameAr }
                keyboardController?.hide()
            }
        )

        // 📋 لوحة نتائج البحث
        if (searchPanelOpen && (isSearching || searchResults.isNotEmpty() || searchMessage != null)) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFF0B0F19).copy(alpha = 0.97f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
                modifier = Modifier
                    .padding(top = 66.dp, start = 16.dp, end = 16.dp)
                    .fillMaxWidth()
                    .heightIn(max = 320.dp)
            ) {
                Column {
                    if (isSearching) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFF10B981)
                        )
                    }
                    searchMessage?.let {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(24.dp))
                            Text(
                                text = it,
                                color = Color(0xFFCBD5E1),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = {
                                    val q = state.searchQuery.trim()
                                    if (q.length >= 2) onRetrySearch()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text("إعادة المحاولة", fontSize = 12.sp, color = Color(0xFF00E5FF))
                            }
                        }
                    }
                    LazyColumn {
                        items(searchResults) { res ->
                            val distM = if (hasRealLocation) {
                                HaversinePrecisionNavigator.calculateDistance(safeUserLat, safeUserLng, res.latitude, res.longitude)
                            } else null
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSelectSearchResult(res)
                                        onCloseSearchPanel()
                                        state.searchQuery = ""
                                        keyboardController?.hide()
                                    }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(res.icon, fontSize = 18.sp)
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = res.shortName,
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = res.displayName,
                                        color = Color(0xFF94A3B8),
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (distM != null) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF1E293B),
                                        border = BorderStroke(1.dp, Color(0xFF334155))
                                    ) {
                                        Text(
                                            text = MapDistanceCalculator.formatDistance(distM),
                                            color = Color(0xFF00E5FF),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                            HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun BoxScope.MapWaypointsAndBottomCards(
    context: Context,
    coroutineScope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
    viewModel: MainViewModel,
    state: MapScreenState,
    themeColors: VisualThemePalette,
    hasRealLocation: Boolean,
    safeUserLat: Double,
    safeUserLng: Double,
    isTilesDownloaded: Boolean,
    searchedPlace: RealValhallaNavigator.SearchAddressResult?,
    onDismissSearchedPlace: () -> Unit,
    pinnedPoint: Pair<Double, Double>?,
    onDismissPinnedPoint: () -> Unit,
    onAddLandmarkForPin: (Pair<Double, Double>) -> Unit,
    onOpenLandmarkForPin: (Pair<Double, Double>) -> Unit,
    onOpenProviderDetails: (ProviderEntity) -> Unit,
    onOpenStoreDetails: (StoreEntity) -> Unit,
    onOpenPropertyDetails: (PropertyEntity) -> Unit,
    onRequestBooking: (Any) -> Unit
) {
    // 📍 8.5. Multi-Stop Waypoints Management Bar
    if (state.selectedWaypoints.isNotEmpty()) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF0B0F19).copy(alpha = 0.90f),
            border = BorderStroke(1.2.dp, Color(0xFF38BDF8).copy(alpha = 0.6f)),
            shadowElevation = 10.dp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = if (state.selectedEntity != null) 250.dp else 90.dp)
                .fillMaxWidth(0.92f)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "محطات التوقف المحددة (${state.selectedWaypoints.size})",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (state.selectedWaypoints.size >= 3 && hasRealLocation) {
                            TextButton(onClick = {
                                val origin = safeUserLat to safeUserLng
                                val before = WaypointOptimizer.pathLength(origin, state.selectedWaypoints.toList())
                                val opt = WaypointOptimizer.optimize(origin, state.selectedWaypoints.toList())
                                val after = WaypointOptimizer.pathLength(origin, opt)
                                if (after + 1.0 < before) {
                                    state.selectedWaypoints.clear()
                                    state.selectedWaypoints.addAll(opt)
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("تم الترتيب: توفير ≈ ${MapDistanceCalculator.formatDistance(before - after)} (بالخط المستقيم)")
                                    }
                                } else {
                                    coroutineScope.launch { snackbarHostState.showSnackbar("ترتيبك الحالي هو الأقرب") }
                                }
                            }) {
                                Text("ترتيب أذكى", color = Color(0xFF00E5FF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        TextButton(onClick = { state.clearWaypoints() }) {
                            Text("مسح الكل", color = Color(0xFFEF4444), fontSize = 11.sp)
                        }
                    }
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 4.dp)
                ) {
                    items(state.selectedWaypoints) { wp ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF1E293B),
                            border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(wp.name, color = Color.White, fontSize = 11.sp, maxLines = 1)
                                Spacer(Modifier.width(4.dp))
                                IconButton(
                                    onClick = { state.removeWaypoint(wp) },
                                    modifier = Modifier.size(16.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(12.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 📋 9. Bottom Sheet for clicked place
    state.selectedEntity?.let { entity ->
        MapBottomSheet(
            entity = entity,
            userLat = if (hasRealLocation) safeUserLat else 0.0,
            userLng = if (hasRealLocation) safeUserLng else 0.0,
            onDismiss = { state.selectedEntity = null },
            onAddStop = { target ->
                state.addWaypoint(target)
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("📍 تمت إضافة المحطة للمسار")
                }
            },
            onRequestBooking = { target ->
                state.selectedEntity = null
                when (target) {
                    is ProviderEntity -> state.bookingProviderTarget = target
                    is StoreEntity -> onRequestBooking(target)
                    is PropertyEntity -> onRequestBooking(target)
                    else -> onRequestBooking(target)
                }
            },
            onOpenDetails = { ent ->
                state.selectedEntity = null
                when (ent) {
                    is ProviderEntity -> onOpenProviderDetails(ent)
                    is StoreEntity -> onOpenStoreDetails(ent)
                    is PropertyEntity -> onOpenPropertyDetails(ent)
                }
            },
            themeColors = themeColors,
            onStartNavigation = { target ->
                when {
                    !hasRealLocation -> {
                        viewModel.startLocationUpdates()
                        coroutineScope.launch { snackbarHostState.showSnackbar("فعّل GPS وانتظر تحديد موقعك") }
                    }
                    !state.startNavigation(target) -> {
                        coroutineScope.launch { snackbarHostState.showSnackbar("موقع هذا المكان غير محدد بدقة، لا يمكن الملاحة إليه") }
                    }
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
        )
    }

    // 📍 بطاقة المكان المختار من البحث
    searchedPlace?.let { place ->
        val dist = MapDistanceCalculator.calculateDistanceMeters(safeUserLat, safeUserLng, place.latitude, place.longitude)
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF0B0F19).copy(alpha = 0.96f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(12.dp)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(place.shortName, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(place.displayName, color = Color(0xFF94A3B8), fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    text = if (isTilesDownloaded)
                        "المسافة المباشرة: ${MapDistanceCalculator.formatDistance(dist)} • توجيه أوفلاين جاهز ✓"
                    else
                        "المسافة المباشرة: ${MapDistanceCalculator.formatDistance(dist)} (تقريبي)",
                    color = Color(0xFF10B981),
                    fontSize = 12.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val wp = NavWaypoint(name = place.shortName, latitude = place.latitude, longitude = place.longitude)
                            if (!hasRealLocation) {
                                viewModel.startLocationUpdates()
                                coroutineScope.launch { snackbarHostState.showSnackbar("فعّل GPS وانتظر تحديد موقعك") }
                            } else if (state.startNavigation(wp)) {
                                onDismissSearchedPlace()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = com.example.ui.theme.MapUi.RoyalMid),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("ابدأ الملاحة", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = {
                            state.addWaypoint(NavWaypoint(name = place.shortName, latitude = place.latitude, longitude = place.longitude))
                            coroutineScope.launch { snackbarHostState.showSnackbar("تمت إضافة المحطة للمسار") }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                    ) {
                        Text("إضافة كمحطة")
                    }
                    TextButton(onClick = onDismissSearchedPlace, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text("إغلاق", color = Color(0xFF94A3B8))
                    }
                }
            }
        }
    }

    // 📌 بطاقة النقطة المحددة يدوياً
    pinnedPoint?.let { (pLat, pLng) ->
        if (state.selectedEntity == null && searchedPlace == null) {
            val dist = MapDistanceCalculator.calculateDistanceMeters(safeUserLat, safeUserLng, pLat, pLng)
            var refSummary by remember(pLat, pLng) { mutableStateOf("") }
            LaunchedEffect(pLat, pLng) {
                refSummary = withContext(Dispatchers.IO) {
                    OfflineYemenGazetteer.nearestReferenceSummary(context, pLat, pLng)
                }
            }
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF0B0F19).copy(alpha = 0.97f),
                border = BorderStroke(1.2.dp, Color(0xFF00E5FF).copy(alpha = 0.45f)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(12.dp)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f)) {
                            Text("📌 نقطة محددة يدوياً", color = Color.White, fontSize = 14.5.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = "\u200E${OfflineYemenGazetteer.formatCoordinates(pLat, pLng)}\u200E",
                                color = Color(0xFF00E5FF),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        IconButton(
                            onClick = onDismissPinnedPoint,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق النقطة", tint = Color(0xFF94A3B8))
                        }
                    }
                    if (refSummary.isNotBlank()) {
                        Text("أقرب مرجع: $refSummary", color = Color(0xFF10B981), fontSize = 11.5.sp)
                    }
                    if (hasRealLocation) {
                        Text(
                            "المسافة من موقعك: ${MapDistanceCalculator.formatDistance(dist)}",
                            color = Color(0xFFCBD5E1),
                            fontSize = 11.5.sp
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                val wp = NavWaypoint(name = "نقطة محددة", latitude = pLat, longitude = pLng)
                                if (!hasRealLocation) {
                                    viewModel.startLocationUpdates()
                                    coroutineScope.launch { snackbarHostState.showSnackbar("فعّل GPS أو حدد موقعك يدوياً من «اعتمد كموقعي» للبدء") }
                                } else if (state.startNavigation(wp)) {
                                    onDismissPinnedPoint()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Text("🧭 ابدأ الملاحة", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = {
                                state.addWaypoint(NavWaypoint(name = "نقطة (${OfflineYemenGazetteer.formatCoordinates(pLat, pLng)})", latitude = pLat, longitude = pLng))
                                coroutineScope.launch { snackbarHostState.showSnackbar("📍 تمت إضافة النقطة كمحطة") }
                            },
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Text("➕ كمحطة", fontSize = 12.sp, color = Color.White)
                        }
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        AssistChip(
                            onClick = {
                                viewModel.setManualUserLocation(pLat, pLng)
                                onDismissPinnedPoint()
                                coroutineScope.launch { snackbarHostState.showSnackbar("✓ تم اعتماد هذه النقطة كموقعك اليدوي للانطلاق والتوصيل") }
                            },
                            label = { Text("📍 اعتمد كموقعي", fontSize = 11.sp, color = Color(0xFF00E5FF)) }
                        )
                        AssistChip(
                            onClick = { onAddLandmarkForPin(pLat to pLng) },
                            label = { Text("➕ احفظ كمعلم", fontSize = 11.sp, color = Color(0xFFFBBF24)) }
                        )
                        AssistChip(
                            onClick = {
                                onOpenLandmarkForPin(pLat to pLng)
                                onDismissPinnedPoint()
                            },
                            label = { Text("🏠 عنوان بالمعلم", fontSize = 11.sp, color = Color(0xFF10B981)) }
                        )
                        AssistChip(
                            onClick = {
                                val shareText = "موقعي المحدد:\n$refSummary\n${OfflineYemenGazetteer.formatCoordinates(pLat, pLng)}\n${SosDispatcher.mapLink(pLat, pLng)}"
                                SosDispatcher.safeStart(context, SosDispatcher.shareIntent(shareText))
                            },
                            label = { Text("📤 مشاركة", fontSize = 11.sp, color = Color.White) }
                        )
                    }
                }
            }
        }
    }
}
