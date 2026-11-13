@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.example.ui.screens.map
import com.example.ui.*

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import com.example.utils.MapDiagnosticsDialog
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.PropertyEntity
import com.example.data.ProviderEntity
import com.example.data.StoreEntity
import com.example.utils.NavWaypoint
import com.example.ui.MainViewModel
import com.example.ui.dialogs.BookingDialog
import com.example.ui.screens.map.components.*
import com.example.ui.screens.map.utils.MapDistanceCalculator
import com.example.ui.screens.map.utils.OfflineMapManager
import com.example.utils.RealValhallaNavigator
import com.example.utils.OfflineYemenGazetteer
import com.example.utils.SosDispatcher
import com.example.utils.ValhallaTileDownloader
import com.example.utils.VisualThemePalette
import com.example.utils.resolveThemePalette
import com.example.utils.LandmarkAddress
import com.example.utils.LandmarkAddressStore
import com.example.utils.MapPowerDataPrefs
import com.example.utils.PermissionManager
import com.example.ui.components.AddCustomLandmarkDialog
import com.example.ui.components.ManualCoordinateDialog
import androidx.activity.compose.BackHandler
import androidx.compose.ui.zIndex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 🗺️ MapScreenContent
 * Clean, modern, 90% clutter-free Map & Navigation Screen:
 * 1. Single Glassmorphism Top Search Capsule with hidden filter popup (NO rows underneath).
 * 2. Unified Vertical Control Dock (GPS -> Zoom+ -> Zoom- -> Layers -> Compass -> Context).
 * 3. Soft Pulsing Floating AR (VELA) button opposite the dock.
 * 4. Discreet SOS / الفزعة button in calm orange-red.
 * 5. Hidden-by-default Bottom Sheet with Emerald Green [بدء الملاحة] (#10B981).
 * 6. Night Windshield HUD Mode reflection.
 * 7. Smart Companion Bar for intercity travelers.
 * 8. Accurate location engine with road snapping.
 */
@Composable
fun MapScreenContent(
    viewModel: MainViewModel,
    state: MapScreenState = rememberMapScreenState(),
    onBackClick: () -> Unit = {},
    onOpenProviderDetails: (ProviderEntity) -> Unit = {},
    onOpenStoreDetails: (StoreEntity) -> Unit = {},
    onOpenPropertyDetails: (PropertyEntity) -> Unit = {},
    onRequestBooking: (Any) -> Unit = {},
    themeColors: VisualThemePalette = resolveThemePalette(viewModel.settings.collectAsState().value)
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    com.example.utils.CrashReportDialog()

    // Status bar icon color setup
    val view = androidx.compose.ui.platform.LocalView.current
    if (!view.isInEditMode) {
        androidx.compose.runtime.SideEffect {
            val window = (view.context as? android.app.Activity)?.window
            if (window != null) {
                val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = false
            }
        }
    }

    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    var areTilesFailing by remember { mutableStateOf(false) }

    val isSysDark = androidx.compose.foundation.isSystemInDarkTheme()
    LaunchedEffect(isSysDark) {
        state.nightMode = isSysDark
    }

    var isDrivingMode by remember { mutableStateOf(true) }
    var isHudMode by remember { mutableStateOf(false) }
    var isHudMirrored by remember { mutableStateOf(false) }
    var showCompanionBar by remember { mutableStateOf(false) }
    var companionFilter by remember { mutableStateOf<String?>(null) }
    var showSosDialog by remember { mutableStateOf(false) }
    var showDownloadDialog by remember { mutableStateOf(false) }
    var showGnss by remember { mutableStateOf(false) }
    var showZones by remember { mutableStateOf(false) }
    var showServicesSheet by remember { mutableStateOf(false) }
    var showDestinationPicker by remember { mutableStateOf(false) }
    var showMoreLayersSheet by remember { mutableStateOf(false) }
    var showGroupTracking by remember { mutableStateOf(false) }
    var coverageRefreshTick by remember { mutableIntStateOf(0) }
    var quakesJson by remember { mutableStateOf<String?>(null) }
    var rainUrl by remember { mutableStateOf<String?>(null) }
    val groupStore = remember { com.example.utils.GroupStore(context) }
    var groupMembers by remember { mutableStateOf(groupStore.all()) }

    LaunchedEffect(Unit) { MapPowerDataPrefs.init(context) }
    val isEcoMode by MapPowerDataPrefs.ecoMode.collectAsState()
    val isManualLoc by viewModel.isManualLocationSet.collectAsState()
    var isPinPickMode by remember { mutableStateOf(false) }
    var centerCoords by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var pinnedPoint by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var pinnedLandmarkTarget by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var showCoordinateDialog by remember { mutableStateOf(false) }
    var showAddLandmarkForPin by remember { mutableStateOf<Pair<Double, Double>?>(null) }

    LaunchedEffect(state.quakesEnabled) {
        if (!state.quakesEnabled) {
            quakesJson = null
            return@LaunchedEffect
        }
        val res = com.example.utils.OverlayFeeds.quakes()
        res.onSuccess { list ->
            quakesJson = com.example.utils.OverlayFeeds.quakesToJson(list)
            snackbarHostState.showSnackbar("🌋 تم تحميل ${list.size} هزة أرضية من USGS")
        }.onFailure {
            quakesJson = null
            state.quakesEnabled = false
            snackbarHostState.showSnackbar("تعذر جلب بيانات الزلازل من USGS — تحقق من الإنترنت")
        }
    }

    LaunchedEffect(state.rainEnabled) {
        if (!state.rainEnabled) {
            rainUrl = null
            return@LaunchedEffect
        }
        val res = com.example.utils.OverlayFeeds.rainTemplate()
        res.onSuccess { url ->
            rainUrl = url
        }.onFailure { err ->
            rainUrl = null
            state.rainEnabled = false
            snackbarHostState.showSnackbar(err.message ?: "طبقة رادار الأمطار غير متاحة حالياً")
        }
    }

    androidx.activity.compose.BackHandler(enabled = true) {
        if (isHudMode) {
            isHudMode = false
        } else if (isPinPickMode) {
            isPinPickMode = false
        } else if (pinnedPoint != null) {
            pinnedPoint = null
        } else if (state.isNavigating) {
            state.stopNavigation()
        } else if (state.selectedEntity != null) {
            state.resetSelection()
        } else {
            onBackClick()
        }
    }

    // Data streams from ViewModel
    val providers by viewModel.providers.collectAsState()
    val stores by viewModel.stores.collectAsState()
    val properties by viewModel.properties.collectAsState()
    val bookings by viewModel.bookings.collectAsState()

    val userLatState by viewModel.userLatitude.collectAsState()
    val userLngState by viewModel.userLongitude.collectAsState()

    val fallbackCityCoords = remember(state.selectedCity) {
        OfflineMapManager.getCityCoordinates(state.selectedCity)
    }
    val rawUserLat = if (userLatState != 0.0 && !userLatState.isNaN()) userLatState else fallbackCityCoords.latitude
    val rawUserLng = if (userLngState != 0.0 && !userLngState.isNaN()) userLngState else fallbackCityCoords.longitude

    // [إصلاح 17] التحقق من الموقع الحقيقي وعدم الاعتماد على إحداثيات وهمية
    val isUserLocationReal by viewModel.isUserLocationReal.collectAsState()
    val hasRealLocation = isUserLocationReal &&
            userLatState != 0.0 && userLngState != 0.0 && !userLatState.isNaN() && !userLngState.isNaN()
    val safeUserLat = rawUserLat
    val safeUserLng = rawUserLng

    // [إصلاح 18] بوصلة حقيقية باستخدام الحساسات (تتوقف في وضع توفير الطاقة)
    val heading = com.example.utils.rememberHeadingDegrees(
        enabled = !state.isNavigating && !isEcoMode,
        lat = safeUserLat,
        lng = safeUserLng
    )

    var isTilesDownloaded by remember { mutableStateOf(ValhallaTileDownloader.isTilesDownloaded(context)) }
    var searchResults by remember { mutableStateOf<List<RealValhallaNavigator.SearchAddressResult>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var searchMessage by remember { mutableStateOf<String?>(null) }
    var searchPanelOpen by remember { mutableStateOf(false) }
    var searchRetryNonce by remember { mutableIntStateOf(0) }
    var searchedPlace by remember { mutableStateOf<RealValhallaNavigator.SearchAddressResult?>(null) }
    var showDiagnostics by remember { mutableStateOf(false) }
    val accuracyM by viewModel.userLocationAccuracyM.collectAsState()
    val queuedRequests by viewModel.offlineQueueManager.pendingRequests.collectAsState()
    val sosPending = queuedRequests.any { it.type == "SOS" }
    var wasSosPending by remember { mutableStateOf(false) }
    LaunchedEffect(sosPending) {
        if (wasSosPending && !sosPending) {
            snackbarHostState.showSnackbar("✅ وصل نداء الفزعة إلى الخادم بنجاح")
        }
        wasSosPending = sosPending
    }



    // [إصلاح 18] البحث الصوتي باستخدام محرك النظام
    val speechLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        if (res.resultCode == android.app.Activity.RESULT_OK) {
            val text = res.data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!text.isNullOrBlank()) {
                state.searchQuery = text.trim()
                searchPanelOpen = true
            }
        }
    }

    LaunchedEffect(state.searchQuery, searchRetryNonce) {
        val q = state.searchQuery.trim()
        if (q.length < 2) {
            searchResults = emptyList()
            searchMessage = null
            isSearching = false
            searchPanelOpen = false
            return@LaunchedEffect
        }
        delay(300)
        isSearching = true
        searchPanelOpen = true
        searchResults = RealValhallaNavigator.searchAddressAndPlaces(
            query = q,
            nearbyProviders = providers,
            nearbyStores = stores,
            userLat = if (hasRealLocation) safeUserLat else 0.0,
            userLng = if (hasRealLocation) safeUserLng else 0.0,
            context = context
        )
        searchMessage = if (searchResults.isEmpty()) (RealValhallaNavigator.lastSearchError ?: "لا توجد نتائج لـ «$q»") else null
        isSearching = false
    }

    var showLocationDisclosure by remember { mutableStateOf(false) }

    val addressStore = remember { LandmarkAddressStore(context.applicationContext) }
    var savedAddresses by remember { mutableStateOf<List<LandmarkAddress>>(emptyList()) }
    var showSavedAddresses by remember { mutableStateOf(false) }
    var showLandmarkAddress by remember { mutableStateOf(false) }
    LaunchedEffect(showLandmarkAddress, showSavedAddresses) {
        savedAddresses = withContext(Dispatchers.IO) { addressStore.getAll() }
    }
    val accuracyForBanner by viewModel.userLocationAccuracyM.collectAsState()
    val gpsWeak = hasRealLocation && accuracyForBanner > 50f && accuracyForBanner < Float.MAX_VALUE

    var hybridLabels by remember { mutableStateOf(false) }

    val is3DAvailable = remember { Map3D.isAvailable(context) }
    var show3D by remember { mutableStateOf(false) }
    var map3dMessage by remember { mutableStateOf<String?>(null) }

    // Location Permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            viewModel.startLocationUpdates()
            coroutineScope.launch {
                snackbarHostState.showSnackbar("تم تفعيل نظام تحديد المواقع بدقة ✓")
            }
        }
    }

    if (showLocationDisclosure) {
        AlertDialog(
            onDismissRequest = { showLocationDisclosure = false },
            title = { Text("لماذا نطلب موقعك؟", fontWeight = FontWeight.Bold) },
            text = { Text(PermissionManager.getRationaleForPermission(PermissionManager.PERMISSION_FINE_LOCATION)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLocationDisclosure = false
                        locationPermissionLauncher.launch(
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                        )
                    }
                ) { Text("متابعة", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showLocationDisclosure = false }) { Text("ليس الآن") }
            }
        )
    }

    LaunchedEffect(Unit) {
        try {
            OfflineMapManager.purgeCacheIfNeeded(context)
        } catch (e: Exception) {}
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (hasFine || hasCoarse) {
            viewModel.startLocationUpdates()
        } else {
            showLocationDisclosure = true
        }
    }

    LaunchedEffect(viewModel.selectedProvider, viewModel.selectedStore, viewModel.selectedProperty) {
        val targetEntity = viewModel.selectedProvider ?: viewModel.selectedStore ?: viewModel.selectedProperty
        if (targetEntity != null) {
            state.selectedEntity = targetEntity
            state.isRadarMode = false
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            state.clearOffsets()
            viewModel.stopLocationUpdates()
        }
    }

    // Filtered lists
    val filteredProviders = remember(providers, state.selectedCategory, state.selectedCity, state.searchQuery, companionFilter) {
        var list = MapScreenFilters.filterProviders(providers, state.selectedCategory, state.selectedCity, state.searchQuery)
        if (companionFilter != null) {
            list = list.filter { it.specialization.contains(companionFilter!!) || it.profession.contains(companionFilter!!) }
        }
        list
    }

    val filteredStores = remember(stores, state.selectedCategory, state.selectedCity, state.searchQuery, companionFilter) {
        var list = MapScreenFilters.filterStores(stores, state.selectedCategory, state.selectedCity, state.searchQuery)
        if (companionFilter != null) {
            list = list.filter { it.name.contains(companionFilter!!) || it.description.contains(companionFilter!!) }
        }
        list
    }

    val filteredProperties = remember(properties, state.selectedCategory, state.selectedCity, state.searchQuery) {
        if (companionFilter != null) emptyList()
        else MapScreenFilters.filterProperties(properties, state.selectedCategory, state.selectedCity, state.searchQuery)
    }

    // Pulse animation for AR (VELA) button
    val infiniteTransition = rememberInfiniteTransition(label = "ar_glow")
    val arGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ar_alpha"
    )

    // 🏠 معالج «عنواني بالمعلم» (حفظ وتعديل العناوين بالمعالم بدون الاعتماد على GPS)
    if (showLandmarkAddress) {
        LandmarkAddressScreen(
            centerLat = safeUserLat,
            centerLng = safeUserLng,
            hasRealLocation = hasRealLocation,
            selectedCity = state.selectedCity,
            initialPoint = pinnedLandmarkTarget,
            onSaved = { addr ->
                pinnedLandmarkTarget = null
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("تم حفظ عنوانك: ${addr.describe()}")
                }
            },
            onClose = {
                pinnedLandmarkTarget = null
                showLandmarkAddress = false
            }
        )
        return
    }

    // Fullscreen Turn-by-Turn Navigation Screen
    if (state.isNavigating) {
        DisposableEffect(Unit) {
            viewModel.startNavigationTracking()
            onDispose { viewModel.stopNavigationTracking() }
        }
        val hazardZones = remember(filteredProviders) {
            com.example.utils.HazardAlerts.roadDifficultyZones(filteredProviders)
        }
        NavigationScreen(
            targetEntity = null,
            userLat = safeUserLat,
            userLng = safeUserLng,
            hasRealLocation = hasRealLocation,
            isDrivingMode = isDrivingMode,
            initialWaypoints = state.navWaypoints,
            onFinishNavigation = { state.stopNavigation() },
            nearbyProviders = filteredProviders,
            nearbyStores = filteredStores,
            hazardZones = hazardZones
        )
        return
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = Color(0xFF0B0F19),
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = Color(0xFF1E293B),
                    contentColor = Color.White,
                    actionColor = Color(0xFF10B981),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0B0F19))
                .padding(paddingValues)
        ) {
            // 🗺️ 1. Main Map Background / AR View (Occupies 100% of viewport, 90% completely unobstructed)
            
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
            } else {
                RealLeafletMapView(
                    userCoords = if (hasRealLocation) Pair(safeUserLat, safeUserLng) else Pair(0.0, 0.0),
                    nearbyProviders = filteredProviders,
                    nearbyStores = filteredStores,
                    nearbyProperties = filteredProperties,
                    dynamicOffsets = state.dynamicOffsets,
                    currentMapMode = state.currentMapMode,
                    selectedCity = state.selectedCity,
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
                    onTilesStatus = { areTilesFailing = it },
                    hybridLabels = hybridLabels,
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
                    onMapPicked = { lat, lng ->
                        isPinPickMode = false
                        pinnedPoint = lat to lng
                        state.selectedEntity = null
                        searchedPlace = null
                    },
                    onMapLongPressed = { lat, lng ->
                        isPinPickMode = false
                        pinnedPoint = lat to lng
                        state.selectedEntity = null
                        searchedPlace = null
                    },
                    onMapCenterChanged = { lat, lng ->
                        centerCoords = lat to lng
                    },
                    waypointMarkers = remember(groupMembers) {
                        groupMembers.mapNotNull { m ->
                            val lat = m.lastLat
                            val lng = m.lastLng
                            if (!m.expired && lat != null && lng != null && LandmarkAddress.isValid(lat, lng)) {
                                Triple("👥 ${m.label}", lat, lng)
                            } else null
                        }
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
                        bookings
                    ) {
                        val combined = mutableListOf<MapZone>()
                        if (showZones || state.reliabilityEnabled) {
                            combined.addAll(com.example.utils.ReliabilityZones.build(filteredProviders, filteredStores))
                        }
                        if (state.peakEnabled) {
                            val provById = filteredProviders.associateBy { it.id }
                            val stamps = bookings.mapNotNull { b ->
                                val p = provById[b.providerId] ?: return@mapNotNull null
                                val t = if (b.scheduledAt > 0L) b.scheduledAt else b.createdAt
                                if (t > 0L && LandmarkAddress.isValid(p.latitude, p.longitude)) {
                                    com.example.utils.BookingStamp(p.latitude, p.longitude, t)
                                } else null
                            }
                            if (stamps.isNotEmpty()) {
                                val peakZones = com.example.utils.PeakPredictor.predict(stamps, System.currentTimeMillis())
                                combined.addAll(com.example.utils.PeakPredictor.toMapZones(peakZones))
                            }
                        }
                        if (state.coverageEnabled) {
                            combined.addAll(com.example.utils.CoverageLog.deadZones(context))
                        }
                        if (state.hazardEnabled) {
                            combined.addAll(com.example.utils.HazardAlerts.roadDifficultyZones(filteredProviders))
                        }
                        combined
                    },
                    eoxEnabled = state.eoxEnabled,
                    quakesJson = quakesJson,
                    rainUrl = rainUrl,
                    onOverlayStatus = { _, failing, msg ->
                        if (failing && msg.isNotBlank()) {
                            coroutineScope.launch { snackbarHostState.showSnackbar(msg) }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                if (areTilesFailing && !state.isRadarMode) {
                    Surface(
                        color = Color(0xDD7F1D1D),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 95.dp)
                            .padding(horizontal = 16.dp)
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
                                    areTilesFailing = false
                                    state.reloadTilesTrigger += 1
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("إعادة المحاولة", color = Color(0xFF93C5FD), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // 🛰️ لافتة GPS الضعيف (تشويش أو ضعف الإشارة)
                if (gpsWeak) {
                    Surface(
                        color = Color(0xCC7C2D12),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .statusBarsPadding()
                            .padding(top = 150.dp, start = 16.dp, end = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "إشارة GPS ضعيفة (±${accuracyForBanner.toInt()} م). لعنوان دقيق استخدم «عنواني بالمعلم».",
                                color = Color.White,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { showLandmarkAddress = true }) {
                                Text("فتح", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // 🏷️ شريط الشارات السريعة (أسماء الشوارع + وضع التوفير + الموقع اليدوي)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .padding(top = 100.dp, start = 16.dp)
                ) {
                    if (state.currentMapMode == MapMode.SATELLITE || state.currentMapMode == MapMode.VELA_3D) {
                        FilterChip(
                            selected = hybridLabels,
                            onClick = { hybridLabels = !hybridLabels },
                            label = { Text("أسماء الشوارع", fontSize = 11.sp, color = if (hybridLabels) Color(0xFF00E5FF) else Color.White) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Color(0xCC0F172A),
                                selectedContainerColor = Color(0xFF1E293B)
                            )
                        )
                    }
                    if (isEcoMode) {
                        FilterChip(
                            selected = true,
                            onClick = {
                                MapPowerDataPrefs.setEcoMode(context, false)
                                viewModel.startLocationUpdates()
                                coroutineScope.launch { snackbarHostState.showSnackbar("تم إيقاف وضع توفير البطارية والنت") }
                            },
                            label = { Text("🔋 توفير", fontSize = 11.sp, color = Color(0xFF10B981)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xCC064E3B)
                            )
                        )
                    }
                    if (isManualLoc) {
                        FilterChip(
                            selected = true,
                            onClick = {
                                viewModel.clearManualUserLocation()
                                viewModel.startLocationUpdates()
                                coroutineScope.launch { snackbarHostState.showSnackbar("تمت العودة لتحديد الموقع عبر GPS") }
                            },
                            label = { Text("📍 موقع يدوي ×", fontSize = 10.5.sp, color = Color(0xFF00E5FF)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xCC0F172A)
                            )
                        )
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
                                    onClick = {
                                        pinnedPoint = active
                                        isPinPickMode = false
                                        state.selectedEntity = null
                                        searchedPlace = null
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                    modifier = Modifier.weight(1f).height(48.dp)
                                ) {
                                    Text("تثبيت هذه النقطة ✓", fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = { isPinPickMode = false },
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    Text("إلغاء", color = Color(0xFF94A3B8))
                                }
                            }
                        }
                    }
                }

                // 🧊 زر 3D الحقيقي (MapLibre + OpenFreeMap)
                if (is3DAvailable && !state.isNavigating) {
                    SmallFloatingActionButton(
                        onClick = {
                            map3dMessage = null
                            show3D = true
                        },
                        containerColor = Color(0xFF0F172A),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .statusBarsPadding()
                            .padding(top = 150.dp, end = 16.dp)
                    ) {
                        Text("3D", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                // 🏠 زر «عنواني بالمعلم» (حل تشويش GPS)
                SmallFloatingActionButton(
                    onClick = {
                        if (savedAddresses.isEmpty()) showLandmarkAddress = true
                        else showSavedAddresses = true
                    },
                    containerColor = Color(0xFF0F172A),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 190.dp)
                ) {
                    Text("🏠", fontSize = 16.sp)
                }

                // 🗺️ عرض الخريطة ثلاثية الأبعاد (3D View Overlay)
                if (show3D) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .zIndex(10f)
                            .background(Color(0xFF0B0F19))
                    ) {
                        Map3DView(
                            centerLat = safeUserLat,
                            centerLng = safeUserLng,
                            userCoords = if (hasRealLocation) Pair(safeUserLat, safeUserLng) else null,
                            markersJson = com.example.ui.screens.map.utils.MapJsonBuilder.buildMarkersJson(
                                nearbyProviders = filteredProviders.take(com.example.ui.screens.map.utils.MapJsonBuilder.MAX_PER_TYPE),
                                nearbyStores = filteredStores.take(com.example.ui.screens.map.utils.MapJsonBuilder.MAX_PER_TYPE),
                                nearbyProperties = emptyList(),
                                dynamicOffsets = emptyMap(),
                                safeUserLat = safeUserLat,
                                safeUserLng = safeUserLng
                            ),
                            onMarkerClicked = { id, type ->
                                when (type) {
                                    "PROVIDER" -> filteredProviders.find { it.id == id }?.let { state.selectedEntity = it; show3D = false }
                                    "STORE", "RESTAURANT", "MEDICAL" -> filteredStores.find { it.id == id }?.let { state.selectedEntity = it; show3D = false }
                                }
                            },
                            onUnavailable = { reason -> map3dMessage = reason; show3D = false },
                            modifier = Modifier.fillMaxSize()
                        )
                        IconButton(
                            onClick = { show3D = false },
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .statusBarsPadding()
                                .padding(8.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.White)
                        }
                    }
                    BackHandler { show3D = false }
                }
            }

            // 🌙 2. HUD Mode Overlay (Night Windshield Reflection)
            if (isHudMode) {
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
                        Text(
                            text = if (isDrivingMode) "55" else "4.2",
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
                            text = "واصل السير على المسار الملاحي",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Button(
                            onClick = { isHudMirrored = !isHudMirrored },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(if (isHudMirrored) "عكس عادي" else "انعكاس الزجاج 🪞", color = Color(0xFF10B981))
                        }
                        Button(
                            onClick = { isHudMode = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("خروج من وضع HUD", color = Color.White)
                        }
                    }
                }
            }

            // 🔍 3. Top Search Bar: Single Glassmorphism Capsule (NO CLUTTER UNDERNEATH)
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
                    onCitySelected = { city ->
                        state.selectedCity = city
                    },
                    searchQuery = state.searchQuery,
                    onSearchQueryChange = { state.searchQuery = it },
                    themeColors = themeColors,
                    onBackClick = onBackClick,
                    onVoiceSearchClick = {
                        val intent = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, "ar-YE")
                            putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "تحدث باسم الشارع أو الخدمة")
                            putExtra(android.speech.RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                        }
                        try {
                            speechLauncher.launch(intent)
                        } catch (e: android.content.ActivityNotFoundException) {
                            coroutineScope.launch { snackbarHostState.showSnackbar("جهازك لا يدعم التعرف على الصوت") }
                        }
                    },
                    onSearchExecute = { query ->
                        val q = query.trim()
                        if (q.length >= 2) {
                            searchPanelOpen = true
                            searchRetryNonce++
                        }
                        OfflineMapManager.findCity(q)?.let { city -> state.selectedCity = city.nameAr }
                        keyboardController?.hide()
                    }
                )

                // 🇾🇪 Offline Map Status Badge
                Surface(
                    shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
                    color = Color(0xFF0B0F19).copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, if (isTilesDownloaded) Color(0xFF10B981).copy(alpha = 0.4f) else Color(0xFFF59E0B).copy(alpha = 0.4f)),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 64.dp, end = 24.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .combinedClickable(
                                onClick = { if (!isTilesDownloaded) showDownloadDialog = true else showDiagnostics = true },
                                onLongClick = { showDiagnostics = true }
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isTilesDownloaded) Color(0xFF10B981) else Color(0xFFF59E0B))
                        )
                        Text(
                            text = if (isTilesDownloaded) "خرائط اليمن أوفلاين ✓" else "تحميل خرائط الملاحة 🇾🇪",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

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
                                            if (q.length >= 2) {
                                                searchRetryNonce++
                                            }
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
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                searchedPlace = res
                                                searchPanelOpen = false
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
                                    }
                                    HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
                                }
                            }
                        }
                    }
                }
            }

            // 🎮 4. Unified Control Dock (قاعدة التحكم الموحدة) - Single continuous vertical capsule
            MapControls(
                isRadarMode = state.isRadarMode,
                onToggleRadarMode = { state.isRadarMode = !state.isRadarMode },
                isHeatmapActive = state.isHeatmapActive,
                onToggleHeatmap = { state.isHeatmapActive = !state.isHeatmapActive },
                onZoomIn = {
                    state.zoomInTick++
                },
                onZoomOut = {
                    state.zoomOutTick++
                },
                onRecenterLocation = {
                    state.panOffset = Offset.Zero
                    state.zoomScale = 1.0f
                    val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    if (hasFine || hasCoarse) {
                        viewModel.startLocationUpdates()
                    } else {
                        locationPermissionLauncher.launch(
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                        )
                    }
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            if (hasRealLocation) "تمت إعادة التمركز إلى موقعك 📍" else "جارٍ تحديد موقعك… تأكد من تشغيل GPS"
                        )
                    }
                },
                onOpenLayerDialog = { state.showLayerDialog = true },
                isNavigationActive = state.isNavigating,
                onToggleNavigation = {
                    val searchedWp = searchedPlace?.let {
                        NavWaypoint(name = it.shortName, latitude = it.latitude, longitude = it.longitude)
                    }
                    when {
                        !hasRealLocation -> {
                            viewModel.startLocationUpdates()
                            coroutineScope.launch { snackbarHostState.showSnackbar("فعّل GPS وانتظر تحديد موقعك ثم أعد المحاولة") }
                        }
                        state.startNavigation(state.selectedEntity ?: searchedWp) -> {
                            searchedPlace = null
                        }
                        else -> {
                            showDestinationPicker = true
                        }
                    }
                },
                isGpsActive = viewModel.isGpsTrackingActive.collectAsState().value,
                themeColors = themeColors,
                currentMapMode = state.currentMapMode,
                onSelectMapMode = { mode, _ ->
                    state.toggleMapMode(mode)
                    state.isRadarMode = false
                    val label = when (state.currentMapMode) {
                        MapMode.SATELLITE, MapMode.VELA_3D -> "أقمار صناعية"
                        MapMode.HOT -> "شوارع (HOT)"
                        MapMode.TOPO -> "تضاريس"
                        else -> "خريطة قياسية"
                    }
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("النمط: $label")
                    }
                },
                isNightMode = state.nightMode,
                onToggleNight = {
                    state.nightMode = !state.nightMode
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(if (state.nightMode) "🌙 تم تفعيل الوضع الليلي للخريطة" else "☀️ تم تفعيل الوضع النهاري للخريطة")
                    }
                },
                isGibsActive = state.gibsEnabled,
                onToggleGibs = {
                    state.gibsEnabled = !state.gibsEnabled
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            if (state.gibsEnabled) "🛰️ تم تفعيل طبقة ناسا اليومية (${if (state.gibsProduct == "landtemp") "حرارة السطح" else "الألوان الحقيقية"})"
                            else "تم إيقاف طبقة ناسا"
                        )
                    }
                },
                onGibsLongPress = {
                    state.gibsProduct = if (state.gibsProduct == "truecolor") "landtemp" else "truecolor"
                    state.gibsEnabled = true
                    val prodName = if (state.gibsProduct == "landtemp") "حرارة السطح النهاري" else "الألوان الحقيقية (TrueColor)"
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("🛰️ تم التبديل إلى منتج ناسا: $prodName")
                    }
                },
                isDrivingMode = isDrivingMode,
                onToggleDrivingMode = {
                    isDrivingMode = !isDrivingMode
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            if (isDrivingMode) "🚘 تم تفعيل وضع القيادة والملاحة المباشرة"
                            else "🚶 تم تفعيل وضع المشي المريح"
                        )
                    }
                },
                compassBearing = -heading,
                onResetCompass = {
                    coroutineScope.launch {
                        val deg = ((heading % 360f) + 360f) % 360f
                        val names = listOf("شمال", "شمال شرق", "شرق", "جنوب شرق", "جنوب", "جنوب غرب", "غرب", "شمال غرب")
                        val dir = names[(((deg + 22.5f) / 45f).toInt()) % 8]
                        snackbarHostState.showSnackbar("اتجاه الجهاز: ${deg.toInt()}° ($dir)")
                    }
                },
                onShowGnss = { showGnss = true },
                isZonesActive = showZones || state.reliabilityEnabled,
                onToggleZones = {
                    val next = !(showZones || state.reliabilityEnabled)
                    showZones = next
                    state.reliabilityEnabled = next
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(if (next) "🟢 تم تفعيل طبقة توفر وموثوقية الخدمات" else "تم إخفاء طبقة توفر الخدمات")
                    }
                },
                onToggleArVela = {
                    state.currentMapMode = if (state.currentMapMode == MapMode.AR_VPS) MapMode.SATELLITE else MapMode.AR_VPS
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            if (state.currentMapMode == MapMode.AR_VPS) "📷 تم تشغيل كاميرا الواقع المعزز (VELA AR)"
                            else "🗺️ تم العودة لخريطة الأقمار الصناعية"
                        )
                    }
                },
                isPinPickMode = isPinPickMode,
                onTogglePinPickMode = {
                    isPinPickMode = !isPinPickMode
                    if (isPinPickMode) {
                        pinnedPoint = null
                        state.selectedEntity = null
                        searchedPlace = null
                    }
                },
                isEcoMode = isEcoMode,
                onToggleEcoMode = {
                    val next = !isEcoMode
                    MapPowerDataPrefs.setEcoMode(context, next)
                    viewModel.startLocationUpdates()
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            if (next) "🔋 تم تفعيل وضع توفير البطارية والنت (كاش ذكي وتحديث GPS اقتصادي)"
                            else "⚡ تم العودة للوضع القياسي للخرائط والموقع"
                        )
                    }
                },
                onOpenCoordinateEntry = { showCoordinateDialog = true },
                hasMoreLayersActive = state.eoxEnabled || state.quakesEnabled || state.rainEnabled ||
                    state.peakEnabled || state.coverageEnabled || state.hazardEnabled || state.gibsEnabled,
                onOpenMoreLayers = { showMoreLayersSheet = true },
                onOpenGroupTracking = { showGroupTracking = true },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp)
            )


            // 🚨 6. Quiet Discreet SOS / الفزعة Button (Calm orange-red #EF4444)
            Surface(
                onClick = { showSosDialog = true },
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF0B0F19).copy(alpha = 0.85f),
                border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.45f)),
                shadowElevation = 6.dp,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .navigationBarsPadding()
                    .padding(start = 16.dp, bottom = if (state.selectedEntity != null) 240.dp else 24.dp)
                    .testTag("map_sos_btn")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Emergency,
                        contentDescription = "فزعة طوارئ",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "فزعة",
                        color = Color(0xFFFCA5A5),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (sosPending) {
                Surface(
                    color = Color(0xFF7F1D1D),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFEF4444)),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = 110.dp)
                ) {
                    Text(
                        "🚨 نداء الفزعة محفوظ وقيد الإرسال تلقائياً…",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }

            // 🚗 7. Smart Companion Bar (Consolidated into single button)
            if (isDrivingMode && state.selectedEntity == null) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // HUD toggle
                    FloatingActionButton(
                        onClick = { isHudMode = true },
                        containerColor = Color(0xFF0F172A),
                        contentColor = Color(0xFF38BDF8),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Nightlife, contentDescription = "HUD ليلي", modifier = Modifier.size(24.dp))
                    }

                    // Consolidated Services Button
                    ExtendedFloatingActionButton(
                        onClick = { showServicesSheet = true },
                        containerColor = Color(0xFF0F172A),
                        contentColor = Color.White,
                        icon = { Icon(Icons.Default.LocalShipping, contentDescription = null, tint = Color(0xFF10B981)) },
                        text = { Text("خدمات الطريق", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.height(48.dp)
                    )
                }
            }

            // Bottom Sheet for Services
            if (showServicesSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showServicesSheet = false },
                    containerColor = Color(0xFF0F172A),
                    contentColor = Color.White,
                    dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.3f)) }
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 48.dp, start = 24.dp, end = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("خدمات الطريق السريع", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                        
                        val items = listOf(
                            Triple("وقود", "⛽ محطات وقود", "محطة"),
                            Triple("استراحة", "☕ استراحات ومطاعم", "استراحة"),
                            Triple("ميكانيك", "🔧 بنشر وصيانة", "صيانة")
                        )

                        items.forEach { (key, label, filterQuery) ->
                            val isActive = companionFilter == filterQuery
                            Surface(
                                onClick = {
                                    companionFilter = if (isActive) null else filterQuery
                                    showServicesSheet = false
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isActive) Color(0xFF10B981).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f),
                                border = if (isActive) BorderStroke(1.dp, Color(0xFF10B981)) else null,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(label, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                    if (isActive) Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF10B981))
                                }
                            }
                        }
                    }
                }
            }

            // 🗺️ 8. Layer Selection Dialog (نظام الطبقات الخمس)
            if (state.showLayerDialog) {
                MapLayersDialog(
                    currentMapMode = state.currentMapMode,
                    onSelectLayer = { mode, tilt ->
                        state.currentMapMode = mode
                        state.isRadarMode = false
                        state.showLayerDialog = false
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("تم تفعيل الطبقة بنجاح ✓")
                        }
                    },
                    onDismiss = { state.showLayerDialog = false }
                )
            }

            if (showGnss) {
                GnssPanelDialog(
                    accuracyM = accuracyM.takeIf { it < Float.MAX_VALUE },
                    onOpenLandmarkAddress = { showLandmarkAddress = true },
                    onDismiss = { showGnss = false }
                )
            }

            if (showMoreLayersSheet) {
                MoreLayersSheet(
                    nightMode = state.nightMode,
                    onToggleNight = { state.nightMode = it },
                    gibsEnabled = state.gibsEnabled,
                    onToggleGibs = { state.gibsEnabled = it },
                    eoxEnabled = state.eoxEnabled,
                    onToggleEox = { state.eoxEnabled = it },
                    quakesEnabled = state.quakesEnabled,
                    onToggleQuakes = { state.quakesEnabled = it },
                    rainEnabled = state.rainEnabled,
                    onToggleRain = { state.rainEnabled = it },
                    reliabilityEnabled = showZones || state.reliabilityEnabled,
                    onToggleReliability = {
                        state.reliabilityEnabled = it
                        showZones = it
                    },
                    peakEnabled = state.peakEnabled,
                    onTogglePeak = { state.peakEnabled = it },
                    coverageEnabled = state.coverageEnabled,
                    onToggleCoverage = { state.coverageEnabled = it },
                    hazardEnabled = state.hazardEnabled,
                    onToggleHazard = { state.hazardEnabled = it },
                    onCoverageCleared = {
                        coverageRefreshTick++
                        coroutineScope.launch { snackbarHostState.showSnackbar("✓ تم مسح سجل التغطية المحلي") }
                    },
                    onDismiss = { showMoreLayersSheet = false }
                )
            }

            if (showGroupTracking) {
                GroupTrackingDialog(
                    onMembersUpdated = { groupMembers = it },
                    onFocusMember = { member ->
                        val lat = member.lastLat
                        val lng = member.lastLng
                        if (lat != null && lng != null) {
                            searchedPlace = RealValhallaNavigator.SearchAddressResult(
                                displayName = "موقع ${member.label} في المجموعة",
                                shortName = "👥 ${member.label}",
                                latitude = lat,
                                longitude = lng,
                                type = "group",
                                icon = "👥"
                            )
                            showGroupTracking = false
                        }
                    },
                    onDismiss = { showGroupTracking = false }
                )
            }

            // 📍 8.5. Multi-Stop Waypoints Management Bar (Appears when stops are added)
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
                                        val before = com.example.utils.WaypointOptimizer.pathLength(origin, state.selectedWaypoints.toList())
                                        val opt = com.example.utils.WaypointOptimizer.optimize(origin, state.selectedWaypoints.toList())
                                        val after = com.example.utils.WaypointOptimizer.pathLength(origin, opt)
                                        if (after + 1.0 < before) {
                                            state.selectedWaypoints.clear()
                                            state.selectedWaypoints.addAll(opt)
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("تم الترتيب: توفير ≈ ${com.example.ui.screens.map.utils.MapDistanceCalculator.formatDistance(before - after)} (بالخط المستقيم)")
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

            // 📋 9. Bottom Sheet for clicked place (HIDDEN BY DEFAULT - APPEARS ONLY ON PIN TAP)
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

            // 📍 بطاقة المكان المختار من البحث (إصلاح البحث)
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
                                        searchedPlace = null
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("ابدأ الملاحة", fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = {
                                    state.addWaypoint(NavWaypoint(name = place.shortName, latitude = place.latitude, longitude = place.longitude))
                                    coroutineScope.launch { snackbarHostState.showSnackbar("تمت إضافة المحطة للمسار") }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("إضافة كمحطة")
                            }
                            TextButton(onClick = { searchedPlace = null }) {
                                Text("إغلاق", color = Color(0xFF94A3B8))
                            }
                        }
                    }
                }
            }

            // 📌 بطاقة النقطة المحددة يدوياً (بالنقر المطول أو وضع الدبوس أو الإحداثيات)
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
                                        OfflineYemenGazetteer.formatCoordinates(pLat, pLng),
                                        color = Color(0xFF00E5FF),
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                IconButton(
                                    onClick = { pinnedPoint = null },
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
                                            pinnedPoint = null
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
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                item {
                                    AssistChip(
                                        onClick = {
                                            viewModel.setManualUserLocation(pLat, pLng)
                                            pinnedPoint = null
                                            coroutineScope.launch { snackbarHostState.showSnackbar("✓ تم اعتماد هذه النقطة كموقعك اليدوي للانطلاق والتوصيل") }
                                        },
                                        label = { Text("📍 اعتمد كموقعي", fontSize = 11.sp, color = Color(0xFF00E5FF)) }
                                    )
                                }
                                item {
                                    AssistChip(
                                        onClick = { showAddLandmarkForPin = pLat to pLng },
                                        label = { Text("➕ احفظ كمعلم", fontSize = 11.sp, color = Color(0xFFFBBF24)) }
                                    )
                                }
                                item {
                                    AssistChip(
                                        onClick = {
                                            pinnedLandmarkTarget = pLat to pLng
                                            pinnedPoint = null
                                            showLandmarkAddress = true
                                        },
                                        label = { Text("🏠 عنوان بالمعلم", fontSize = 11.sp, color = Color(0xFF10B981)) }
                                    )
                                }
                                item {
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
        }
    }

    // 📥 Download Dialog for Offline Maps
    if (showDownloadDialog) {
        var isDownloading by remember { mutableStateOf(false) }
        var downloadProgress by remember { mutableFloatStateOf(0f) }
        var downloadStatusText by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { if (!isDownloading) showDownloadDialog = false },
            containerColor = Color(0xFF0B0F19),
            titleContentColor = Color.White,
            textContentColor = Color.White,
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null, tint = Color(0xFF38BDF8))
                    Text("تحميل خرائط الملاحة أوفلاين", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "للحصول على أدق النتائج والملاحة في اليمن بدون إنترنت، يرجى تحميل حزمة خرائط اليمن.\nحجم التنزيل: ~${ValhallaTileDownloader.EXPECTED_DOWNLOAD_MB} ميجابايت مضغوطة (تُفكّ إلى ~${ValhallaTileDownloader.EXPECTED_INSTALLED_MB} ميجابايت على الجهاز).",
                        color = Color.White,
                        fontSize = 13.sp
                    )
                    if (isDownloading) {
                        LinearProgressIndicator(
                            progress = { downloadProgress },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = Color(0xFF10B981),
                            trackColor = Color(0xFF1E293B)
                        )
                        Text(downloadStatusText, color = Color(0xFF10B981), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            confirmButton = {
                if (!isDownloading) {
                    Button(
                        onClick = {
                            isDownloading = true
                            coroutineScope.launch {
                                val result = ValhallaTileDownloader.downloadAndExtractTiles(context) { p, status ->
                                    downloadProgress = p
                                    downloadStatusText = status
                                }
                                if (result.isSuccess) {
                                    isTilesDownloaded = true
                                    showDownloadDialog = false
                                    snackbarHostState.showSnackbar("✅ تم تثبيت خرائط اليمن (~${ValhallaTileDownloader.EXPECTED_INSTALLED_MB} MB) بنجاح! استمتع بالملاحة أوفلاين.")
                                } else {
                                    isDownloading = false
                                    snackbarHostState.showSnackbar("❌ فشل التحميل: ${result.exceptionOrNull()?.message ?: "يرجى التحقق من الاتصال."}")
                                }
                            }
                        },
                        modifier = Modifier.heightIn(min = 48.dp).testTag("map_download_offline_confirm_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Text("تحميل الآن (~${ValhallaTileDownloader.EXPECTED_DOWNLOAD_MB} MB)", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                if (!isDownloading) {
                    TextButton(onClick = { showDownloadDialog = false }) {
                        Text("لاحقاً", color = Color(0xFF94A3B8))
                    }
                }
            }
        )
    }

    // 🚨 10. SOS / الفزعة Confirmation Dialog
    if (showSosDialog) {
        val nearest = remember(providers, hasRealLocation, safeUserLat, safeUserLng) {
            if (!hasRealLocation) null
            else providers
                .filter { it.phone.isNotBlank() && it.latitude != 0.0 && it.longitude != 0.0 }
                .minByOrNull { MapDistanceCalculator.calculateDistanceMeters(safeUserLat, safeUserLng, it.latitude, it.longitude) }
        }
        val sosText = SosDispatcher.buildMessage(safeUserLat, safeUserLng, null)

        AlertDialog(
            onDismissRequest = { showSosDialog = false },
            containerColor = Color(0xFF0F172A),
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Emergency, contentDescription = null, tint = Color(0xFFEF4444))
                    Text("🚨 نداء فزعة طوارئ", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (!hasRealLocation) {
                        Text("لم يتم تحديد موقعك الحقيقي بعد، فلن يُرسَل موقع. يمكنك الاتصال مباشرة.", color = Color(0xFFF59E0B), fontSize = 13.sp)
                    } else {
                        Text("سيتم استخدام موقعك الحالي. لا يُعتبر النداء مرسلاً إلا بعد تأكيد الإرسال أدناه.", color = Color(0xFFCBD5E1), fontSize = 13.sp)
                    }
                    if (com.example.utils.NavConfig.EMERGENCY_NUMBER.isNotBlank()) {
                        Button(
                            onClick = { SosDispatcher.safeStart(context, SosDispatcher.dialIntent(com.example.utils.NavConfig.EMERGENCY_NUMBER)) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("📞 اتصال بالطوارئ ${com.example.utils.NavConfig.EMERGENCY_NUMBER}", fontWeight = FontWeight.Bold) }
                    }
                    nearest?.let { p ->
                        Text("أقرب فني: ${p.name}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Button(
                            onClick = { SosDispatcher.safeStart(context, SosDispatcher.dialIntent(p.phone)) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("📞 اتصل بـ ${p.name}")
                        }
                        if (hasRealLocation) {
                            OutlinedButton(
                                onClick = {
                                    if (!SosDispatcher.safeStart(context, SosDispatcher.smsIntent(p.phone, sosText))) {
                                        coroutineScope.launch { snackbarHostState.showSnackbar("تعذر فتح تطبيق الرسائل") }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("✉️ أرسل رسالة SMS بموقعي (تعمل بلا إنترنت)")
                            }
                        }
                    }
                    if (hasRealLocation) {
                        OutlinedButton(
                            onClick = { SosDispatcher.safeStart(context, SosDispatcher.shareIntent(sosText)) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("📤 مشاركة موقعي (واتساب وغيره)")
                        }
                        Button(
                            onClick = {
                                val ok = viewModel.enqueueSos(
                                    safeUserLat,
                                    safeUserLng,
                                    accuracyM.takeIf { it < Float.MAX_VALUE },
                                    sosText
                                )
                                showSosDialog = false
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(
                                        if (ok) "تم حفظ النداء وسيُرسل للخادم فور توفر الإنترنت. للأمان اتصل أيضاً بأقرب فني."
                                        else "تعذر حفظ النداء! اتصل بأقرب فني أو أرسل رسالة SMS الآن."
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("🚨 إرسال نداء عبر التطبيق", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSosDialog = false }) {
                    Text("إغلاق", color = Color(0xFF94A3B8))
                }
            }
        )
    }

    if (showDiagnostics) {
        MapDiagnosticsDialog(
            hasRealLocation = hasRealLocation,
            accuracyM = accuracyM.takeIf { it < Float.MAX_VALUE },
            onTilesUpdated = { isTilesDownloaded = ValhallaTileDownloader.isTilesDownloaded(context) }
        ) {
            showDiagnostics = false
        }
    }

    // Provider Booking Dialog
    state.bookingProviderTarget?.let { provider ->
        BookingDialog(
            provider = provider,
            viewModel = viewModel,
            themeColors = themeColors,
            onDismiss = { state.bookingProviderTarget = null }
        )
    }

    if (showSavedAddresses) {
        SavedLandmarkAddressesDialog(
            addresses = savedAddresses,
            onNavigate = { a ->
                showSavedAddresses = false
                if (!hasRealLocation) {
                    viewModel.startLocationUpdates()
                    coroutineScope.launch { snackbarHostState.showSnackbar("فعّل GPS وانتظر تحديد موقعك") }
                } else {
                    state.startNavigationViaLandmark(a)
                }
            },
            onDelete = { a ->
                coroutineScope.launch {
                    withContext(Dispatchers.IO) { addressStore.delete(a.id) }
                    savedAddresses = withContext(Dispatchers.IO) { addressStore.getAll() }
                }
            },
            onAddNew = {
                showSavedAddresses = false
                showLandmarkAddress = true
            },
            onDismiss = { showSavedAddresses = false }
        )
    }

    if (showDestinationPicker) {
        var pickerQuery by remember { mutableStateOf("") }
        var pickerResults by remember { mutableStateOf<List<RealValhallaNavigator.SearchAddressResult>>(emptyList()) }
        var pickerBusy by remember { mutableStateOf(false) }
        val defaultSuggestions = remember(safeUserLat, safeUserLng, savedAddresses) {
            val saved = savedAddresses.map {
                RealValhallaNavigator.SearchAddressResult(
                    displayName = "عنوان محفوظ • ${it.landmarkName}",
                    shortName = it.label,
                    latitude = it.pointLat,
                    longitude = it.pointLng,
                    type = "saved",
                    icon = "🏠"
                )
            }
            (saved + OfflineYemenGazetteer.suggestionsNear(safeUserLat, safeUserLng, limit = 12))
                .distinctBy { "${it.shortName}_${it.latitude}" }
        }

        LaunchedEffect(pickerQuery) {
            val q = pickerQuery.trim()
            if (q.length < 2) {
                pickerResults = emptyList()
                pickerBusy = false
                return@LaunchedEffect
            }
            delay(250)
            pickerBusy = true
            pickerResults = RealValhallaNavigator.searchAddressAndPlaces(
                query = q,
                nearbyProviders = providers,
                nearbyStores = stores,
                userLat = if (hasRealLocation) safeUserLat else 0.0,
                userLng = if (hasRealLocation) safeUserLng else 0.0,
                context = context
            )
            pickerBusy = false
        }

        val visibleList = if (pickerQuery.trim().length >= 2) pickerResults else defaultSuggestions

        AlertDialog(
            onDismissRequest = { showDestinationPicker = false },
            containerColor = Color(0xFF0F172A),
            titleContentColor = Color.White,
            title = { Text("🧭 اختر وجهتك للملاحة", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = pickerQuery,
                        onValueChange = { pickerQuery = it },
                        singleLine = true,
                        placeholder = { Text("ابحث عن شارع، جولة، فندق، معلم أو الصق إحداثيات…", fontSize = 12.sp, color = Color(0xFF94A3B8)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF00E5FF),
                            unfocusedBorderColor = Color(0xFF334155)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = {
                                showDestinationPicker = false
                                isPinPickMode = true
                            },
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Text("📍 دبوس الخريطة", fontSize = 11.sp, color = Color(0xFF10B981), maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = {
                                showDestinationPicker = false
                                showCoordinateDialog = true
                            },
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Text("🔢 إحداثيات يدوياً", fontSize = 11.sp, color = Color(0xFF00E5FF), maxLines = 1)
                        }
                    }
                    if (pickerBusy) {
                        LinearProgressIndicator(Modifier.fillMaxWidth(), color = Color(0xFF10B981))
                    }
                    if (visibleList.isEmpty() && pickerQuery.trim().length >= 2 && !pickerBusy) {
                        Text(
                            text = "لم يتم العثور على «${pickerQuery.trim()}» — حدد المكان بدبوس الخريطة أو بالإحداثيات أعلاه",
                            color = Color(0xFFF59E0B),
                            fontSize = 12.sp
                        )
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 260.dp)) {
                            items(visibleList) { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val wp = NavWaypoint(
                                                name = item.shortName,
                                                latitude = item.latitude,
                                                longitude = item.longitude
                                            )
                                            showDestinationPicker = false
                                            state.startNavigation(wp)
                                        }
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(item.icon, fontSize = 16.sp)
                                    Column(Modifier.weight(1f)) {
                                        Text(item.shortName, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(item.displayName, color = Color(0xFF94A3B8), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                                HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showDestinationPicker = false }) {
                    Text("إغلاق", color = Color(0xFF94A3B8))
                }
            }
        )
    }

    if (showCoordinateDialog) {
        ManualCoordinateDialog(
            initialLat = pinnedPoint?.first ?: safeUserLat,
            initialLng = pinnedPoint?.second ?: safeUserLng,
            gpsLat = safeUserLat,
            gpsLng = safeUserLng,
            hasGps = hasRealLocation,
            confirmLabel = "📌 عرض على الخريطة",
            onDismiss = { showCoordinateDialog = false },
            onConfirm = { lat, lng, _ ->
                pinnedPoint = lat to lng
                searchedPlace = null
                state.selectedEntity = null
                showCoordinateDialog = false
            },
            onNavigate = { lat, lng, _ ->
                val wp = NavWaypoint(
                    name = "إحداثيات (${OfflineYemenGazetteer.formatCoordinates(lat, lng)})",
                    latitude = lat,
                    longitude = lng
                )
                showCoordinateDialog = false
                if (!hasRealLocation) {
                    coroutineScope.launch { snackbarHostState.showSnackbar("حدد موقع انطلاقك أولاً عبر GPS أو «📍 كموقعي»") }
                } else {
                    state.startNavigation(wp)
                }
            },
            onSetAsMyLocation = { lat, lng ->
                viewModel.setManualUserLocation(lat, lng)
                showCoordinateDialog = false
                coroutineScope.launch { snackbarHostState.showSnackbar("✓ تم اعتماد الإحداثيات كموقع انطلاقك اليدوي") }
            }
        )
    }

    showAddLandmarkForPin?.let { (lat, lng) ->
        AddCustomLandmarkDialog(
            latitude = lat,
            longitude = lng,
            onDismiss = { showAddLandmarkForPin = null },
            onAdded = { saved ->
                showAddLandmarkForPin = null
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("✓ تم حفظ المعلم «${saved.name}» وسيظهر في البحث أوفلاين")
                }
            }
        )
    }

    LaunchedEffect(map3dMessage) {
        map3dMessage?.let {
            snackbarHostState.showSnackbar(it)
            map3dMessage = null
        }
    }
}
