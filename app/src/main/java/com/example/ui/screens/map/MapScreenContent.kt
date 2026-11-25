@file:OptIn(
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class
)

package com.example.ui.screens.map

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.example.data.PropertyEntity
import com.example.data.ProviderEntity
import com.example.data.StoreEntity
import com.example.ui.MainViewModel
import com.example.ui.screens.map.components.RoadReportDialog
import com.example.ui.screens.map.utils.OfflineMapManager
import com.example.ui.theme.*
import com.example.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 🗺️ MapScreenContent (المنسّق الرئيسي لشاشة الخريطة)
 * يدير الحالة العامة، الأذونات، حساسات الموقع والبوصلة، البحث الصوتي والنصي،
 * ويوجه العرض بين شاشة الخريطة، الملاحة المباشرة، عنوان المعلم، الطبقات، والحوارات.
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

    CrashReportDialog()

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? android.app.Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            }
        }
    }

    val keyboardController = LocalSoftwareKeyboardController.current
    var areTilesFailing by remember { mutableStateOf(false) }

    val isSysDark = isSystemInDarkTheme()
    LaunchedEffect(isSysDark) {
        state.nightMode = isSysDark
    }

    var isDrivingMode by remember { mutableStateOf(true) }
    var companionFilter by remember { mutableStateOf<String?>(null) }
    var showSosDialog by remember { mutableStateOf(false) }
    var showDownloadDialog by remember { mutableStateOf(false) }
    var showGnss by remember { mutableStateOf(false) }
    var showZones by remember { mutableStateOf(false) }
    var showServicesSheet by remember { mutableStateOf(false) }
    var showDestinationPicker by remember { mutableStateOf(false) }
    var showMoreLayersSheet by remember { mutableStateOf(false) }
    var showGroupTracking by remember { mutableStateOf(false) }
    var showRoadReportDialog by remember { mutableStateOf(false) }
    var roadReports by remember { mutableStateOf<List<RoadReportEntity>>(emptyList()) }
    var coverageRefreshTick by remember { mutableIntStateOf(0) }
    var quakesJson by remember { mutableStateOf<String?>(null) }
    var rainUrl by remember { mutableStateOf<String?>(null) }
    val groupStore = remember { GroupStore(context) }
    var groupMembers by remember { mutableStateOf(groupStore.all()) }

    val settingsState by viewModel.settings.collectAsState()
    val adminRole by viewModel.adminRole.collectAsState()
    val currentUserId by viewModel.currentUserId.collectAsState()
    val currentUserPhone by viewModel.currentUserPhone.collectAsState()
    val isAdminOrStaff = adminRole.trim().uppercase() in setOf("OWNER", "ADMIN", "SUPERVISOR", "SUPER_ADMIN", "MAIN_ADMIN")

    if (!settingsState.isMapFeatureEnabled && !isAdminOrStaff) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0B0F19))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = Color(0xFF0F172A),
                border = BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("🗺️", fontSize = 42.sp)
                    Text("خدمة الخرائط متوقفة مؤقتاً", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "أوقفت الإدارة ميزة الخرائط والملاحة مؤقتاً للصيانة أو التحديث.",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Button(
                        onClick = onBackClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Text("رجوع", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        return
    }

    LaunchedEffect(Unit) {
        MapPowerDataPrefs.init(context)
        if (settingsState.isEcoMapDefault) {
            MapPowerDataPrefs.setEcoMode(context, true)
        }
    }
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
        OverlayFeeds.quakes().onSuccess { list ->
            quakesJson = OverlayFeeds.quakesToJson(list)
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
        OverlayFeeds.rainTemplate().onSuccess { url ->
            rainUrl = url
        }.onFailure { err ->
            rainUrl = null
            state.rainEnabled = false
            snackbarHostState.showSnackbar(err.message ?: "طبقة رادار الأمطار غير متاحة حالياً")
        }
    }

    BackHandler(enabled = true) {
        when {
            isPinPickMode -> isPinPickMode = false
            pinnedPoint != null -> pinnedPoint = null
            state.isNavigating -> state.stopNavigation()
            state.selectedEntity != null -> state.resetSelection()
            else -> onBackClick()
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

    val isUserLocationReal by viewModel.isUserLocationReal.collectAsState()
    val hasRealLocation = isUserLocationReal &&
        userLatState != 0.0 && userLngState != 0.0 && !userLatState.isNaN() && !userLngState.isNaN()
    val safeUserLat = rawUserLat
    val safeUserLng = rawUserLng

    val heading = rememberHeadingDegrees(
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

    var voiceState by remember { mutableStateOf(VoiceState()) }
    var searchFocusTick by remember { mutableIntStateOf(0) }
    val voiceHelper = remember(context) {
        VoiceSearchHelper(
            context = context,
            onFinal = { text ->
                state.searchQuery = text.trim()
                searchPanelOpen = true
            },
            onState = { s -> voiceState = s }
        )
    }
    DisposableEffect(Unit) {
        onDispose { voiceHelper.destroy() }
    }
    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) voiceHelper.start()
        else coroutineScope.launch { snackbarHostState.showSnackbar("مطلوب إذن الميكروفون للبحث الصوتي") }
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
            context = context,
            nearbyProperties = properties,
            biasCenter = centerCoords
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

    var hybridLabels by remember { mutableStateOf(true) }
    val is3DAvailable = remember { Map3D.isAvailable(context) }
    var map3dMessage by remember { mutableStateOf<String?>(null) }
    var use3DCssFallback by remember { mutableStateOf(false) }
    val valhallaSizeMb = remember(isTilesDownloaded) {
        if (isTilesDownloaded) ValhallaTileDownloader.getInstalledSizeMb(context) else 0
    }

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

    LaunchedEffect(Unit) {
        try {
            OfflineMapManager.purgeCacheIfNeeded(context)
        } catch (_: Exception) {
        }
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

    LaunchedEffect(viewModel.pendingMapFocusPoint, viewModel.pendingMapAutoNavigate) {
        val wp = viewModel.pendingMapFocusPoint ?: return@LaunchedEffect
        val autoNav = viewModel.pendingMapAutoNavigate
        viewModel.pendingMapFocusPoint = null
        viewModel.pendingMapAutoNavigate = false
        state.isRadarMode = false
        state.selectedEntity = null
        pinnedPoint = wp.latitude to wp.longitude
        searchedPlace = RealValhallaNavigator.SearchAddressResult(
            displayName = wp.name,
            shortName = wp.name,
            latitude = wp.latitude,
            longitude = wp.longitude,
            type = "chat_shared",
            icon = "📍"
        )
        if (autoNav) {
            state.startNavigation(wp)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            state.clearOffsets()
            viewModel.stopLocationUpdates()
        }
    }

    val isRegisteredUser = (currentUserId.isNotBlank() && currentUserId != "guest") || currentUserPhone.isNotBlank()
    val isProviderUser = remember(isRegisteredUser, currentUserId, currentUserPhone, providers, stores, properties) {
        isRegisteredUser && (
            providers.any { it.id == currentUserId || (currentUserPhone.isNotBlank() && it.phone == currentUserPhone) } ||
                stores.any { it.id == currentUserId || (currentUserPhone.isNotBlank() && it.phone == currentUserPhone) } ||
                properties.any { it.ownerId == currentUserId || (currentUserPhone.isNotBlank() && it.phone == currentUserPhone) }
            )
    }
    val selectedFilterLabel = ""

    val filteredProviders = remember(providers, state.selectedCategory, state.selectedCity, state.searchQuery, companionFilter, selectedFilterLabel) {
        var list = MapScreenFilters.filterProviders(providers, state.selectedCategory, state.selectedCity, state.searchQuery, selectedFilterLabel)
        val filter = companionFilter
        if (!filter.isNullOrBlank()) {
            list = list.filter { it.specialization.contains(filter) || it.profession.contains(filter) }
        }
        list
    }

    val filteredStores = remember(stores, state.selectedCategory, state.selectedCity, state.searchQuery, companionFilter, selectedFilterLabel) {
        var list = MapScreenFilters.filterStores(stores, state.selectedCategory, state.selectedCity, state.searchQuery, selectedFilterLabel)
        val filter = companionFilter
        if (!filter.isNullOrBlank()) {
            list = list.filter { it.name.contains(filter) || it.description.contains(filter) }
        }
        list
    }

    val filteredProperties = remember(properties, state.selectedCategory, state.selectedCity, state.searchQuery, companionFilter, selectedFilterLabel) {
        if (companionFilter != null) emptyList()
        else MapScreenFilters.filterProperties(properties, state.selectedCategory, state.selectedCity, state.searchQuery, selectedFilterLabel)
    }

    // 🏠 معالج «عنواني بالمعلم»
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

    // 🧭 شاشة الملاحة الكاملة
    if (state.isNavigating) {
        DisposableEffect(Unit) {
            viewModel.startNavigationTracking()
            onDispose { viewModel.stopNavigationTracking() }
        }
        val hazardZones = remember(filteredProviders, roadReports) {
            HazardAlerts.roadDifficultyZones(filteredProviders) + roadReports.map { it.toMapZone() }
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

    val roadReportCellKey = remember(safeUserLat, safeUserLng, hasRealLocation) {
        if (hasRealLocation && LandmarkAddress.isValid(safeUserLat, safeUserLng)) {
            RoadReportRepository.computeCellId(safeUserLat, safeUserLng)
        } else ""
    }
    LaunchedEffect(roadReportCellKey, settingsState.enableMapLiveTrafficReports) {
        if (settingsState.enableMapLiveTrafficReports && roadReportCellKey.isNotBlank()) {
            RoadReportRepository.fetchNearbyReports(safeUserLat, safeUserLng)
                .onSuccess { roadReports = it }
        }
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
            MapScreenViewport(
                context = context,
                coroutineScope = coroutineScope,
                snackbarHostState = snackbarHostState,
                viewModel = viewModel,
                state = state,
                filteredProviders = filteredProviders,
                filteredStores = filteredStores,
                filteredProperties = filteredProperties,
                bookings = bookings,
                safeUserLat = safeUserLat,
                safeUserLng = safeUserLng,
                hasRealLocation = hasRealLocation,
                is3DAvailable = is3DAvailable,
                use3DCssFallback = use3DCssFallback,
                onFallback3DCss = { reason, isUnavailable ->
                    map3dMessage = reason
                    use3DCssFallback = true
                    if (isUnavailable) {
                        coroutineScope.launch { snackbarHostState.showSnackbar(reason) }
                    }
                },
                hybridLabels = hybridLabels,
                onToggleHybridLabels = { hybridLabels = !hybridLabels },
                isPinPickMode = isPinPickMode,
                onMapPicked = { lat, lng ->
                    isPinPickMode = false
                    pinnedPoint = lat to lng
                    state.selectedEntity = null
                    searchedPlace = null
                },
                onCancelPinPick = { isPinPickMode = false },
                centerCoords = centerCoords,
                onCenterChanged = { lat, lng -> centerCoords = lat to lng },
                searchedPlace = searchedPlace,
                pinnedPoint = pinnedPoint,
                areTilesFailing = areTilesFailing,
                onTilesStatus = { areTilesFailing = it },
                gpsWeak = gpsWeak,
                accuracyForBanner = accuracyForBanner,
                isEcoMode = isEcoMode,
                isManualLoc = isManualLoc,
                isTilesDownloaded = isTilesDownloaded,
                valhallaSizeMb = valhallaSizeMb,
                groupMembers = groupMembers,
                showZones = showZones,
                coverageRefreshTick = coverageRefreshTick,
                quakesJson = quakesJson,
                rainUrl = rainUrl,
                savedAddresses = savedAddresses,
                sosPending = sosPending,
                roadReports = roadReports,
                onSelectOsmPoi = { res ->
                    searchedPlace = res
                    state.selectedEntity = null
                },
                onOpenLandmarkAddress = { showLandmarkAddress = true },
                onOpenSavedAddresses = { showSavedAddresses = true }
            )

            MapScreenTopSearchAndResults(
                coroutineScope = coroutineScope,
                snackbarHostState = snackbarHostState,
                keyboardController = keyboardController,
                state = state,
                themeColors = themeColors,
                onBackClick = onBackClick,
                onVoiceSearchClick = {
                    if (voiceHelper.hasMicPermission()) {
                        voiceHelper.start()
                    } else {
                        micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                isTilesDownloaded = isTilesDownloaded,
                onOpenDownloadDialog = { showDownloadDialog = true },
                onOpenDiagnostics = { showDiagnostics = true },
                searchPanelOpen = searchPanelOpen,
                onOpenSearchPanel = { searchPanelOpen = true },
                onCloseSearchPanel = { searchPanelOpen = false },
                onRetrySearch = { searchRetryNonce++ },
                isSearching = isSearching,
                searchResults = searchResults,
                searchMessage = searchMessage,
                onSelectSearchResult = { res ->
                    searchedPlace = res
                    state.selectedEntity = null
                },
                focusTick = searchFocusTick,
                safeUserLat = safeUserLat,
                safeUserLng = safeUserLng,
                hasRealLocation = hasRealLocation
            )

            var showToolsMenu by remember { mutableStateOf(false) }

            MapFabCluster(
                isNavigating = state.isNavigating,
                locateActive = hasRealLocation,
                onNavigate = {
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
                onLayers = { state.showLayerDialog = true },
                onZoomIn = { state.zoomInTick++ },
                onZoomOut = { state.zoomOutTick++ },
                onLocate = {
                    state.recenterTick++
                    viewModel.startLocationUpdates()
                    coroutineScope.launch { snackbarHostState.showSnackbar("تمت إعادة التمركز إلى موقعك 📍") }
                },
                onMore = { showToolsMenu = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 12.dp, bottom = 90.dp)
            )

            if (showToolsMenu) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 72.dp, bottom = 140.dp)
                ) {
                    // [نظام خرائط وملاحة عالمي - ملكنا]
                    DropdownMenu(
                        expanded = showToolsMenu,
                        onDismissRequest = { showToolsMenu = false },
                        containerColor = MapUi.SurfaceSolid,
                        modifier = Modifier.border(1.dp, MapUi.Border, RoundedCornerShape(MapUi.RadiusS))
                    ) {
                        DropdownMenuItem(
                            text = { Text("فزعة طوارئ", color = MapUi.Danger, fontWeight = FontWeight.Bold) },
                            leadingIcon = { Icon(Icons.Default.Emergency, null, tint = MapUi.Danger) },
                            onClick = { showToolsMenu = false; showSosDialog = true }
                        )
                        DropdownMenuItem(
                            text = { Text("خدمات الطريق", color = MapUi.Ok) },
                            leadingIcon = { Icon(Icons.Default.LocalShipping, null, tint = MapUi.Ok) },
                            onClick = { showToolsMenu = false; showServicesSheet = true }
                        )
                        DropdownMenuItem(
                            text = { Text("بلاغات الطرق", color = MapUi.Warn) },
                            leadingIcon = { Icon(Icons.Default.Warning, null, tint = MapUi.Warn) },
                            onClick = { showToolsMenu = false; showRoadReportDialog = true }
                        )
                        HorizontalDivider(color = MapUi.Border)
                        DropdownMenuItem(
                            text = { Text(if (state.nightMode) "وضع النهار" else "الوضع الليلي", color = MapUi.TextPrimary) },
                            leadingIcon = { Icon(if (state.nightMode) Icons.Default.LightMode else Icons.Default.Bedtime, null, tint = MapUi.RoyalTop) },
                            onClick = { showToolsMenu = false; state.nightMode = !state.nightMode }
                        )
                        DropdownMenuItem(
                            text = { Text("إدخال إحداثيات", color = MapUi.TextPrimary) },
                            leadingIcon = { Icon(Icons.Default.EditLocationAlt, null, tint = MapUi.RoyalTop) },
                            onClick = { showToolsMenu = false; showCoordinateDialog = true }
                        )
                        DropdownMenuItem(
                            text = { Text("تشخيص GPS", color = MapUi.TextPrimary) },
                            leadingIcon = { Icon(Icons.Default.SatelliteAlt, null, tint = MapUi.Accent) },
                            onClick = { showToolsMenu = false; showGnss = true }
                        )
                        DropdownMenuItem(
                            text = { Text("تتبع المجموعة", color = MapUi.TextPrimary) },
                            leadingIcon = { Icon(Icons.Default.Group, null, tint = MapUi.Ok) },
                            onClick = { showToolsMenu = false; showGroupTracking = true }
                        )
                    }
                }
            }

            val isGpsHardwareEnabled = remember(context, hasRealLocation) {
                val lm = context.getSystemService(android.content.Context.LOCATION_SERVICE) as? android.location.LocationManager
                lm != null && (
                    lm.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) ||
                    lm.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)
                )
            }

            MapContextBanner(
                text = when {
                    areTilesFailing -> "الخريطة تحتاج إنترنت أو حزمة عرض"
                    !isGpsHardwareEnabled -> "الموقع متوقف — يرجى تفعيل GPS"
                    !hasRealLocation -> "جارٍ البحث عن الأقمار الصناعية (GPS)…"
                    accuracyForBanner in 61f..1500f -> "دقة الموقع تقريبية (±${accuracyForBanner.toInt()} م)"
                    else -> null
                },
                color = when {
                    areTilesFailing -> MapUi.Warn
                    !isGpsHardwareEnabled -> MapUi.Danger
                    !hasRealLocation -> MapUi.Warn
                    else -> MapUi.RoyalTop
                },
                icon = when {
                    areTilesFailing -> Icons.Default.CloudOff
                    !isGpsHardwareEnabled -> Icons.Default.LocationDisabled
                    !hasRealLocation -> Icons.Default.GpsNotFixed
                    else -> Icons.Default.LocationSearching
                },
                onClick = {
                    if (areTilesFailing) showDownloadDialog = true
                    else if (!isGpsHardwareEnabled) {
                        try { context.startActivity(android.content.Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)) } catch (_: Exception) {}
                    }
                },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 78.dp, start = 72.dp, end = 72.dp)
            )

            MapWaypointsAndBottomCards(
                context = context,
                coroutineScope = coroutineScope,
                snackbarHostState = snackbarHostState,
                viewModel = viewModel,
                state = state,
                themeColors = themeColors,
                hasRealLocation = hasRealLocation,
                safeUserLat = safeUserLat,
                safeUserLng = safeUserLng,
                isTilesDownloaded = isTilesDownloaded,
                searchedPlace = searchedPlace,
                onDismissSearchedPlace = { searchedPlace = null },
                pinnedPoint = pinnedPoint,
                onDismissPinnedPoint = { pinnedPoint = null },
                onAddLandmarkForPin = { showAddLandmarkForPin = it },
                onOpenLandmarkForPin = {
                    pinnedLandmarkTarget = it
                    showLandmarkAddress = true
                },
                onOpenProviderDetails = onOpenProviderDetails,
                onOpenStoreDetails = onOpenStoreDetails,
                onOpenPropertyDetails = onOpenPropertyDetails,
                onRequestBooking = onRequestBooking
            )

            MapLayersAndServicesSheetsHost(
                coroutineScope = coroutineScope,
                snackbarHostState = snackbarHostState,
                state = state,
                showServicesSheet = showServicesSheet,
                onDismissServicesSheet = { showServicesSheet = false },
                companionFilter = companionFilter,
                onSelectCompanionFilter = { companionFilter = it },
                isTilesDownloaded = isTilesDownloaded,
                valhallaSizeMb = valhallaSizeMb,
                onClearTilesFailing = { areTilesFailing = false },
                onOpenDownloadDialog = { showDownloadDialog = true },
                is3DAvailable = is3DAvailable,
                onPrepare3DToggle = { enabled ->
                    map3dMessage = null
                    if (enabled) use3DCssFallback = false
                },
                showMoreLayersSheet = showMoreLayersSheet,
                onDismissMoreLayersSheet = { showMoreLayersSheet = false },
                showZones = showZones,
                onSetShowZones = { showZones = it },
                onCoverageCleared = { coverageRefreshTick++ },
                adminRole = adminRole,
                isRegisteredUser = isRegisteredUser,
                isProvider = isProviderUser,
                enableLiveTrafficByAdmin = settingsState.enableMapLiveTrafficReports
            )
        }
    }

    if (state.showLandmarkFilterSheet) {
        com.example.ui.screens.map.components.MapLandmarkFilterSheet(
            showLabelsOnMap = state.showLabelsOnMap,
            onToggleLabelsOnMap = { enabled ->
                state.showLabelsOnMap = enabled
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        if (enabled) "👁️ تم إظهار أسماء الشوارع والمعالم"
                        else "🙈 تم إخفاء أسماء الشوارع والمعالم لتخفيف الازدحام"
                    )
                }
            },
            visibleFilters = state.visibleLandmarkFilters,
            onToggleFilter = { filter ->
                val current = state.visibleLandmarkFilters.toMutableSet()
                if (current.contains(filter)) current.remove(filter) else current.add(filter)
                state.visibleLandmarkFilters = current
            },
            onSelectAll = {
                state.visibleLandmarkFilters = com.example.ui.screens.map.components.MapLandmarkFilter.entries.toSet()
            },
            onClearAll = {
                state.visibleLandmarkFilters = emptySet()
            },
            onDismiss = { state.showLandmarkFilterSheet = false }
        )
    }

    MapScreenDialogsHost(
        context = context,
        coroutineScope = coroutineScope,
        snackbarHostState = snackbarHostState,
        viewModel = viewModel,
        state = state,
        themeColors = themeColors,
        providers = providers,
        stores = stores,
        hasRealLocation = hasRealLocation,
        safeUserLat = safeUserLat,
        safeUserLng = safeUserLng,
        accuracyM = accuracyM,
        showLocationDisclosure = showLocationDisclosure,
        onDismissLocationDisclosure = { showLocationDisclosure = false },
        locationPermissionLauncher = locationPermissionLauncher,
        showGnss = showGnss,
        onDismissGnss = { showGnss = false },
        onOpenLandmarkAddress = { showLandmarkAddress = true },
        showGroupTracking = showGroupTracking,
        onDismissGroupTracking = { showGroupTracking = false },
        onGroupMembersUpdated = { groupMembers = it },
        onFocusGroupMember = { res ->
            searchedPlace = res
            state.selectedEntity = null
        },
        showDownloadDialog = showDownloadDialog,
        onDismissDownloadDialog = { showDownloadDialog = false },
        onTilesDownloaded = { isTilesDownloaded = true },
        showSosDialog = showSosDialog,
        onDismissSosDialog = { showSosDialog = false },
        showDiagnostics = showDiagnostics,
        onDismissDiagnostics = { showDiagnostics = false },
        onDiagnosticsTilesUpdated = { isTilesDownloaded = ValhallaTileDownloader.isTilesDownloaded(context) },
        showSavedAddresses = showSavedAddresses,
        savedAddresses = savedAddresses,
        onSavedAddressesUpdated = { savedAddresses = it },
        addressStore = addressStore,
        onDismissSavedAddresses = { showSavedAddresses = false },
        showDestinationPicker = showDestinationPicker,
        onDismissDestinationPicker = { showDestinationPicker = false },
        onEnablePinPickMode = {
            isPinPickMode = true
            pinnedPoint = null
            state.selectedEntity = null
            searchedPlace = null
        },
        onOpenCoordinateDialog = { showCoordinateDialog = true },
        showCoordinateDialog = showCoordinateDialog,
        onDismissCoordinateDialog = { showCoordinateDialog = false },
        pinnedPoint = pinnedPoint,
        onPinConfirmedFromCoords = { lat, lng ->
            pinnedPoint = lat to lng
            searchedPlace = null
            state.selectedEntity = null
        },
        showAddLandmarkForPin = showAddLandmarkForPin,
        onDismissAddLandmarkForPin = { showAddLandmarkForPin = null }
    )

    LaunchedEffect(map3dMessage) {
        map3dMessage?.let {
            snackbarHostState.showSnackbar(it)
            map3dMessage = null
        }
    }

    if (voiceState.status != VoiceStatus.IDLE) {
        VoiceSearchDialog(
            state = voiceState,
            onCancel = { voiceHelper.cancel() },
            onRetry = { voiceHelper.start() },
            onUseKeyboard = {
                voiceHelper.cancel()
                searchFocusTick++
            },
            onOpenSettings = {
                VoiceSearchHelper.openVoiceSettings(context)
            }
        )
    }

    if (showRoadReportDialog) {
        val reportLat = pinnedPoint?.first ?: centerCoords?.first ?: safeUserLat
        val reportLng = pinnedPoint?.second ?: centerCoords?.second ?: safeUserLng
        RoadReportDialog(
            targetLat = reportLat,
            targetLng = reportLng,
            hasValidLocation = LandmarkAddress.isValid(reportLat, reportLng),
            reports = roadReports,
            onReportsUpdated = { roadReports = it },
            onFeedback = { msg -> coroutineScope.launch { snackbarHostState.showSnackbar(msg) } },
            onDismiss = { showRoadReportDialog = false }
        )
    }
}
