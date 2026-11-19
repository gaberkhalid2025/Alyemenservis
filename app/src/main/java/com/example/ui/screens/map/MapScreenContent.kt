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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Nightlife
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
import com.example.ui.screens.map.utils.OfflineMapManager
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
    var coverageRefreshTick by remember { mutableIntStateOf(0) }
    var quakesJson by remember { mutableStateOf<String?>(null) }
    var rainUrl by remember { mutableStateOf<String?>(null) }
    val groupStore = remember { GroupStore(context) }
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

    DisposableEffect(Unit) {
        onDispose {
            state.clearOffsets()
            viewModel.stopLocationUpdates()
        }
    }

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

    val filteredProperties = remember(properties, state.selectedCategory, state.selectedCity, state.searchQuery, companionFilter) {
        if (companionFilter != null) emptyList()
        else MapScreenFilters.filterProperties(properties, state.selectedCategory, state.selectedCity, state.searchQuery)
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
        val hazardZones = remember(filteredProviders) {
            HazardAlerts.roadDifficultyZones(filteredProviders)
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
                focusTick = searchFocusTick
            )

            MapControlsDockHost(
                context = context,
                coroutineScope = coroutineScope,
                snackbarHostState = snackbarHostState,
                viewModel = viewModel,
                state = state,
                themeColors = themeColors,
                hasRealLocation = hasRealLocation,
                heading = heading,
                isDrivingMode = isDrivingMode,
                onToggleDrivingMode = { isDrivingMode = !isDrivingMode },
                isEcoMode = isEcoMode,
                isPinPickMode = isPinPickMode,
                onSetPinPickMode = { isPinPickMode = it },
                onClearPinnedAndSearched = {
                    pinnedPoint = null
                    state.selectedEntity = null
                    searchedPlace = null
                },
                searchedPlace = searchedPlace,
                onClearSearchedOnly = { searchedPlace = null },
                showZones = showZones,
                onSetShowZones = { showZones = it },
                locationPermissionLauncher = locationPermissionLauncher,
                onShowDestinationPicker = { showDestinationPicker = true },
                onShowGnss = { showGnss = true },
                onShowCoordinateDialog = { showCoordinateDialog = true },
                onOpenMoreLayersSheet = { showMoreLayersSheet = true },
                onCloseMoreLayersSheet = { showMoreLayersSheet = false },
                onShowGroupTracking = { showGroupTracking = true }
            )

            // 🚨 لافتة انتظار نداء الفزعة إن وجد طلب معلّق
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

            // 🌟 الشريط العائم الموحد (Glassmorphic Floating Bar)
            // يجمع: 1. الفزعة (الأيقونة الحمراء) + 2. خدمات الطريق + 3. زر الكأس (المكافآت) + 4. HUD ليلي
            if (state.selectedEntity == null) {
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = Color(0xFF0F172A).copy(alpha = 0.92f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                    shadowElevation = 14.dp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 20.dp, start = 16.dp, end = 16.dp)
                        .testTag("map_bottom_floating_bar")
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // 1. 🚨 الأيقونة الحمراء (فزعة طوارئ)
                        Surface(
                            onClick = { showSosDialog = true },
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFEF4444).copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.35f)),
                            modifier = Modifier
                                .height(42.dp)
                                .testTag("map_sos_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Emergency,
                                    contentDescription = "فزعة طوارئ",
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "فزعة",
                                    color = Color(0xFFFCA5A5),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        VerticalDivider(
                            modifier = Modifier.height(26.dp),
                            color = Color.White.copy(alpha = 0.10f)
                        )

                        // 2. 🚚 خدمات الطريق
                        Surface(
                            onClick = { showServicesSheet = true },
                            shape = RoundedCornerShape(14.dp),
                            color = if (companionFilter != null) Color(0xFF10B981).copy(alpha = 0.16f) else Color.Transparent,
                            border = if (companionFilter != null) BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.40f)) else null,
                            modifier = Modifier
                                .height(42.dp)
                                .testTag("map_services_sheet_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalShipping,
                                    contentDescription = "خدمات الطريق",
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "خدمات الطريق",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

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
                onCoverageCleared = { coverageRefreshTick++ }
            )
        }
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
}
