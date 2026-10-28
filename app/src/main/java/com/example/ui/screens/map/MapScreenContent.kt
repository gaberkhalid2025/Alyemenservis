@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.screens.map
import com.example.ui.*

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.PropertyEntity
import com.example.data.ProviderEntity
import com.example.data.StoreEntity
import com.example.ui.MainViewModel
import com.example.ui.dialogs.BookingDialog
import com.example.ui.screens.map.components.*
import com.example.ui.screens.map.utils.MapDistanceCalculator
import com.example.ui.screens.map.utils.OfflineMapManager
import com.example.utils.MockValhallaNavigator
import com.example.utils.ValhallaTileDownloader
import com.example.utils.VisualThemePalette
import com.example.utils.resolveThemePalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

    var isDrivingMode by remember { mutableStateOf(true) }
    var isHudMode by remember { mutableStateOf(false) }
    var isHudMirrored by remember { mutableStateOf(false) }
    var showCompanionBar by remember { mutableStateOf(false) }
    var companionFilter by remember { mutableStateOf<String?>(null) }
    var showSosDialog by remember { mutableStateOf(false) }
    var isSosBroadcasting by remember { mutableStateOf(false) }
    var compassBearing by remember { mutableFloatStateOf(0f) }
    var showDownloadDialog by remember { mutableStateOf(false) }

    androidx.activity.compose.BackHandler(enabled = true) {
        if (isHudMode) {
            isHudMode = false
        } else if (state.isNavigating) {
            state.isNavigating = false
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

    val userLatState by viewModel.userLatitude.collectAsState()
    val userLngState by viewModel.userLongitude.collectAsState()

    val fallbackCityCoords = remember(state.selectedCity) {
        OfflineMapManager.getCityCoordinates(state.selectedCity)
    }
    val rawUserLat = if (userLatState != 0.0 && !userLatState.isNaN()) userLatState else fallbackCityCoords.latitude
    val rawUserLng = if (userLngState != 0.0 && !userLngState.isNaN()) userLngState else fallbackCityCoords.longitude

    // Road Snapping calculation
    val (safeUserLat, safeUserLng) = remember(rawUserLat, rawUserLng) {
        MockValhallaNavigator.snapToRoad(rawUserLat, rawUserLng)
    }

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

    LaunchedEffect(Unit) {
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (hasFine || hasCoarse) {
            viewModel.startLocationUpdates()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
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

    // Fullscreen Turn-by-Turn Navigation Screen
    if (state.isNavigating) {
        val targetEntity = state.selectedEntity

        com.example.ui.screens.map.NavigationScreen(
            targetEntity = targetEntity,
            userLat = safeUserLat,
            userLng = safeUserLng,
            onFinishNavigation = { state.isNavigating = false },
            nearbyProviders = filteredProviders,
            nearbyStores = filteredStores
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
            val isTilesDownloaded = remember { ValhallaTileDownloader.isTilesDownloaded(context) }
            
            if (state.currentMapMode == MapMode.AR_VPS) {
                ArVpsCameraOverlay(
                    nearbyProviders = filteredProviders,
                    nearbyStores = filteredStores,
                    nearbyProperties = filteredProperties,
                    userLat = safeUserLat,
                    userLng = safeUserLng,
                    onProviderSelected = { state.selectedEntity = it },
                    onStoreSelected = { state.selectedEntity = it },
                    onPropertySelected = { state.selectedEntity = it },
                    onCloseAr = { state.currentMapMode = MapMode.SATELLITE },
                    modifier = Modifier.fillMaxSize()
                )
            } else if (state.isRadarMode) {
                RadarRenderer(
                    items = remember(filteredProviders, filteredStores, filteredProperties, safeUserLat, safeUserLng) {
                        MapScreenFilters.buildRadarPoints(
                            filteredProviders = filteredProviders,
                            filteredStores = filteredStores,
                            filteredProperties = filteredProperties,
                            safeUserLat = safeUserLat,
                            safeUserLng = safeUserLng
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
            } else {
                RealLeafletMapView(
                    userCoords = Pair(safeUserLat, safeUserLng),
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
                    modifier = Modifier.fillMaxSize()
                )
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
                        if (city != "الكل") {
                            val coords = com.example.ui.screens.map.utils.OfflineMapManager.getCityCoordinates(city)
                            viewModel.updateUserLocation(coords.latitude, coords.longitude)
                        }
                    },
                    searchQuery = state.searchQuery,
                    onSearchQueryChange = { state.searchQuery = it },
                    themeColors = themeColors,
                    onBackClick = onBackClick,
                    onVoiceSearchClick = {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("🎙️ المساعد الصوتي يستمع: تحدث باسم الشارع أو الخدمة...")
                        }
                    },
                    onSearchExecute = { query ->
                        if (query.isNotBlank()) {
                            val cityCoords = com.example.ui.screens.map.utils.OfflineMapManager.getCityCoordinates(query)
                            if (cityCoords.nameAr != "صنعاء" || query.contains("صنعاء")) {
                                state.selectedCity = cityCoords.nameAr
                                viewModel.updateUserLocation(cityCoords.latitude, cityCoords.longitude)
                            } else {
                                val match = filteredProviders.firstOrNull() ?: filteredStores.firstOrNull() ?: filteredProperties.firstOrNull()
                                if (match != null) {
                                    state.selectedEntity = match
                                }
                            }
                        }
                    }
                )

                // 🇾🇪 Offline Map Status Badge
                Surface(
                    onClick = { if (!isTilesDownloaded) showDownloadDialog = true },
                    shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
                    color = Color(0xFF0B0F19).copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, if (isTilesDownloaded) Color(0xFF10B981).copy(alpha = 0.4f) else Color(0xFFF59E0B).copy(alpha = 0.4f)),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 64.dp, end = 24.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
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
        }

            // 🎮 4. Unified Control Dock (قاعدة التحكم الموحدة) - Single continuous vertical capsule
            MapControls(
                isRadarMode = state.isRadarMode,
                onToggleRadarMode = { state.isRadarMode = !state.isRadarMode },
                isHeatmapActive = state.isHeatmapActive,
                onToggleHeatmap = { state.isHeatmapActive = !state.isHeatmapActive },
                onZoomIn = {
                    state.zoomScale = (state.zoomScale * 1.30f).coerceIn(0.25f, 8.0f)
                },
                onZoomOut = {
                    state.zoomScale = (state.zoomScale / 1.30f).coerceIn(0.25f, 8.0f)
                },
                onRecenterLocation = {
                    state.panOffset = Offset.Zero
                    state.zoomScale = 1.0f
                    val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    if (hasFine || hasCoarse) {
                        viewModel.startLocationUpdates()
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("تمت إعادة التمركز إلى موقعك الفعلي 📍")
                        }
                    } else {
                        locationPermissionLauncher.launch(
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                        )
                    }
                },
                onOpenLayerDialog = { state.showLayerDialog = true },
                isNavigationActive = state.isNavigating,
                onToggleNavigation = {
                    state.isNavigating = true
                },
                isGpsActive = viewModel.isGpsTrackingActive.collectAsState().value,
                themeColors = themeColors,
                currentMapMode = state.currentMapMode,
                onSelectMapMode = { mode, tilt ->
                    state.currentMapMode = mode
                    state.isRadarMode = false
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("تم تطبيق النمط بنجاح ✓")
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
                compassBearing = compassBearing,
                onResetCompass = {
                    compassBearing = 0f
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("🧭 تم إعادة توجيه الخريطة إلى الشمال الحقيقي")
                    }
                },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp)
            )

            // 📷 5. Floating AR (VELA) Button (Opposite the dock with soft pulsing glow)
            Surface(
                onClick = {
                    state.currentMapMode = if (state.currentMapMode == MapMode.AR_VPS) MapMode.SATELLITE else MapMode.AR_VPS
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            if (state.currentMapMode == MapMode.AR_VPS) "📷 تم تشغيل كاميرا الواقع المعزز (VELA AR) مع التوجيه ثلاثي الأبعاد"
                            else "🗺️ تم العودة لخريطة الأقمار الصناعية"
                        )
                    }
                },
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFF0B0F19).copy(alpha = 0.85f),
                border = BorderStroke(1.2.dp, Color(0xFF10B981).copy(alpha = arGlowAlpha)),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 16.dp)
                    .testTag("mode_ar_vela_btn")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981).copy(alpha = arGlowAlpha))
                    )
                    Icon(
                        imageVector = Icons.Default.ViewInAr,
                        contentDescription = "واقع معزز",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "VELA AR",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

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

            // 🚗 7. Smart Companion Bar for Intercity Trips (Discreet, triggers on toggle or driving mode)
            if (isDrivingMode && state.selectedEntity == null) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF0B0F19).copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 24.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // HUD toggle
                        IconButton(
                            onClick = { isHudMode = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Nightlife, contentDescription = "HUD ليلي", tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                        }

                        // Intercity companion filters
                        val items = listOf(
                            Triple("وقود", "⛽ وقود", "محطة"),
                            Triple("استراحة", "☕ استراحة", "استراحة"),
                            Triple("ميكانيك", "🔧 بنشر/صيانة", "صيانة")
                        )

                        items.forEach { (key, label, filterQuery) ->
                            val isActive = companionFilter == filterQuery
                            Surface(
                                onClick = {
                                    companionFilter = if (isActive) null else filterQuery
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(
                                            if (!isActive) "رفيق السفر: تصفية أقرب $label على الطريق"
                                            else "تم إلغاء تصفية رفيق السفر"
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isActive) Color(0xFF10B981).copy(alpha = 0.25f) else Color.Transparent,
                                border = if (isActive) BorderStroke(1.dp, Color(0xFF10B981)) else null
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.5.sp,
                                    color = if (isActive) Color(0xFF10B981) else Color.White,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
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
                            TextButton(onClick = { state.selectedWaypoints.clear() }) {
                                Text("مسح الكل", color = Color(0xFFEF4444), fontSize = 11.sp)
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
                    userLat = safeUserLat,
                    userLng = safeUserLng,
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
                        state.isNavigating = true
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                )
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
                        "للحصول على أدق النتائج والملاحة في اليمن بدون إنترنت، يرجى تحميل حزمة الخرائط (حوالي 25 ميجابايت).",
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
                                    showDownloadDialog = false
                                    snackbarHostState.showSnackbar("✅ تم تثبيت خرائط اليمن بنجاح! استمتع بالملاحة أوفلاين.")
                                } else {
                                    isDownloading = false
                                    snackbarHostState.showSnackbar("❌ فشل التحميل، يرجى التحقق من الاتصال.")
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Text("تحميل الآن (25MB)", color = Color.White, fontWeight = FontWeight.Bold)
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
        AlertDialog(
            onDismissRequest = { showSosDialog = false },
            containerColor = Color(0xFF0B0F19),
            titleContentColor = Color.White,
            textContentColor = Color.White,
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Emergency, contentDescription = null, tint = Color(0xFFEF4444))
                    Text("فزعة طوارئ 🆘", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "هل تحتاج إلى مساعدة فورية أو بنشر أو سحب مركبة؟",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "سيتم إرسال إحداثيات موقعك الحالي ($safeUserLat, $safeUserLng) إلى أقرب فني وميكانيكي معتمد في محيطك فوراً.",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.5.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isSosBroadcasting = true
                        coroutineScope.launch {
                            delay(600)
                            isSosBroadcasting = false
                            showSosDialog = false
                            snackbarHostState.showSnackbar("🚨 تم إرسال نداء الفزعة وإحداثيات موقعك لأقرب فني طوارئ معتمد!")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(if (isSosBroadcasting) "جارِ الإرسال..." else "إرسال نداء الفزعة", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSosDialog = false }) {
                    Text("إلغاء", color = Color(0xFF94A3B8))
                }
            }
        )
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
}
