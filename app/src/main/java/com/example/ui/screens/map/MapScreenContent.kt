@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.screens.map
import com.example.ui.*

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.PropertyEntity
import com.example.data.ProviderEntity
import com.example.data.StoreEntity
import com.example.ui.MainViewModel
import com.example.ui.dialogs.BookingDialog
import com.example.ui.screens.map.components.*
import com.example.ui.screens.map.utils.OfflineMapManager
import com.example.utils.VisualThemePalette
import com.example.utils.resolveThemePalette
import kotlinx.coroutines.launch


/**
 * 🗺️ MapScreenContent
 * Primary presentation composable for the Map & Radar screen (Radar-First 100%).
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

    // ضبط أيقونات شريط الحالة العلوي للهاتف لتكون واضحة وناصعة البياض على الخلفية الداكنة
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

    var showMapErrorOverlay by remember { mutableStateOf(false) }
    var mapRetryCount by remember { mutableIntStateOf(0) }

    androidx.activity.compose.BackHandler(enabled = true) {
        if (state.selectedEntity != null) {
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

    // إصلاح 1.1: عند عدم توفر GPS حقيقي نستخدم مركز المدينة المحددة حالياً بدلاً من إحداثيات ثابتة قديمة
    val fallbackCityCoords = remember(state.selectedCity) {
        OfflineMapManager.getCityCoordinates(state.selectedCity)
    }
    val safeUserLat = if (userLatState != 0.0 && !userLatState.isNaN()) userLatState else fallbackCityCoords.latitude
    val safeUserLng = if (userLngState != 0.0 && !userLngState.isNaN()) userLngState else fallbackCityCoords.longitude

    // Location Permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            viewModel.startLocationUpdates()
            coroutineScope.launch {
                snackbarHostState.showSnackbar("تم تفعيل نظام تحديد المواقع بنجاح ✓")
            }
        }
    }

    // Auto-request location updates on screen launch to acquire user's real GPS immediately
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

    // [تحسين مدمج] ربط الكيان المختار من المساعد الذكي أو القوائم مباشرة بالخريطة وفتح بطاقة التفاصيل
    LaunchedEffect(Unit) {
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

    // Filtered lists using MapScreenFilters module
    val filteredProviders = remember(providers, state.selectedCategory, state.selectedCity, state.searchQuery) {
        MapScreenFilters.filterProviders(providers, state.selectedCategory, state.selectedCity, state.searchQuery)
    }

    val filteredStores = remember(stores, state.selectedCategory, state.selectedCity, state.searchQuery) {
        MapScreenFilters.filterStores(stores, state.selectedCategory, state.selectedCity, state.searchQuery)
    }

    val filteredProperties = remember(properties, state.selectedCategory, state.selectedCity, state.searchQuery) {
        MapScreenFilters.filterProperties(properties, state.selectedCategory, state.selectedCity, state.searchQuery)
    }

    // Radar Points calculated via MapScreenFilters
    val radarPoints = remember(filteredProviders, filteredStores, filteredProperties, safeUserLat, safeUserLng) {
        MapScreenFilters.buildRadarPoints(
            filteredProviders = filteredProviders,
            filteredStores = filteredStores,
            filteredProperties = filteredProperties,
            safeUserLat = safeUserLat,
            safeUserLng = safeUserLng
        )
    }

    // Reset map pan & zoom when governorate changes
    LaunchedEffect(state.selectedCity) {
        state.panOffset = Offset.Zero
        state.zoomScale = 1.0f
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = Color(0xFF0F172A),
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = Color(0xFF1E293B),
                    contentColor = Color.White,
                    actionColor = Color(0xFF00E5FF),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F172A))
                .padding(paddingValues)
        ) {
            // Main Map / AR View
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
                Box(modifier = Modifier.fillMaxSize()) {
                    RadarRenderer(
                        items = radarPoints,
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

                    // زر التبديل الاختياري إلى الخريطة الكاملة في الأسفل عند عدم وجود عنصر محدد
                    if (state.selectedEntity == null) {
                        Button(
                            onClick = {
                                showMapErrorOverlay = false
                                state.isRadarMode = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .navigationBarsPadding()
                                .padding(bottom = 24.dp)
                        ) {
                            Text("🗺️ تبديل إلى خريطة اليمن الكاملة الحية", color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    RealLeafletMapView(
                        userCoords = Pair(safeUserLat, safeUserLng),
                        currentMapMode = state.currentMapMode,
                        nearbyProviders = filteredProviders,
                        nearbyStores = filteredStores,
                        nearbyProperties = filteredProperties,
                        dynamicOffsets = state.dynamicOffsets,
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
                        onSwitchToRadar = {
                            showMapErrorOverlay = false
                            state.isRadarMode = true
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // [تعديل] شارة مصدر الخريطة والنقاط المكتشفة كالصورة المرفقة تماماً
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF0F172A).copy(alpha = 0.88f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .navigationBarsPadding()
                    .padding(start = 16.dp, bottom = if (state.selectedEntity != null) 220.dp else 24.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color(0xFF10B981), androidx.compose.foundation.shape.CircleShape)
                    )
                    Text(
                        text = if (state.isRadarMode) "رادار كشف الخدمات · ${radarPoints.size} نقطة مكتشفة" else "OpenStreetMap · ${radarPoints.size} نقطة مكتشفة",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // [تعديل] الزر الدائري الأحمر العائم في الأسفل يميناً كالصورة المرفقة (تبديل الرادار والخريطة الحية)
            Surface(
                shape = androidx.compose.foundation.shape.CircleShape,
                color = Color(0xFFDC2626), // Red circle as in screenshot
                shadowElevation = 8.dp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 16.dp, bottom = if (state.selectedEntity != null) 220.dp else 24.dp)
                    .size(54.dp)
            ) {
                IconButton(
                    onClick = {
                        showMapErrorOverlay = false
                        state.isRadarMode = !state.isRadarMode
                    },
                    modifier = Modifier.fillMaxSize().testTag("floating_radar_fab")
                ) {
                    Icon(
                        imageVector = if (state.isRadarMode) Icons.Default.Place else Icons.Default.Navigation,
                        contentDescription = "رادار الخدمات",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            // 🎛️ MapControls (GPS, Zoom In, Zoom Out, Layers, Offline) على الجانب الأيسر كالصورة المرفقة تماماً
            MapControls(
                isRadarMode = state.isRadarMode,
                onToggleRadarMode = {
                    showMapErrorOverlay = false
                    state.isRadarMode = !state.isRadarMode
                },
                isHeatmapActive = state.isHeatmapActive,
                onToggleHeatmap = {
                    state.isHeatmapActive = !state.isHeatmapActive
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            if (state.isHeatmapActive) "تم تفعيل التخزين المؤقت للأوفلاين 💾" else "تم إيقاف التخزين المؤقت"
                        )
                    }
                },
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
                onOpenLayerDialog = {
                    state.showLayerDialog = true
                },
                isGpsActive = viewModel.isGpsTrackingActive.collectAsState().value,
                themeColors = themeColors,
                currentMapMode = state.currentMapMode,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .navigationBarsPadding()
                    .padding(bottom = if (state.selectedEntity != null) 200.dp else 20.dp)
            )

            // 🗺️ Layer Selection Dialog (الخيارات الأربعة كالصورة الثانية)
            if (state.showLayerDialog) {
                AlertDialog(
                    onDismissRequest = { state.showLayerDialog = false },
                    containerColor = Color(0xFF0F172A),
                    titleContentColor = Color.White,
                    textContentColor = Color.White,
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("🗺️ اختيار طبقة الخريطة", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            val layers = listOf(
                                Triple(MapMode.OSM, "🌐 OpenStreetMap Standard (الرسمية)", "الخريطة القياسية المفصلة"),
                                Triple(MapMode.HOT, "🗺️ خريطة الشوارع الحيوية (HOT)", "مثالية للملاحة والشوارع الفرعية"),
                                Triple(MapMode.TOPO, "⛰️ خريطة العالم والتضاريس (Topo)", "إظهار المعالم والارتفاعات الجغرافية"),
                                Triple(MapMode.SATELLITE, "🛰️ صور الأقمار الصناعية (Satellite)", "صور جوية عالية الدقة للمدن اليمنية")
                            )

                            layers.forEach { (mode, title, desc) ->
                                val isSelected = state.currentMapMode == mode
                                Surface(
                                    onClick = {
                                        state.currentMapMode = mode
                                        state.isRadarMode = false
                                        state.showLayerDialog = false
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("تم تفعيل طبقة: $title")
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) Color(0xFF00E5FF).copy(alpha = 0.2f) else Color(0xFF1E293B),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) Color(0xFF00E5FF) else Color(0xFF334155)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(text = title, color = if (isSelected) Color(0xFF00E5FF) else Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(text = desc, color = Color(0xFF94A3B8), fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { state.showLayerDialog = false }) {
                            Text("إغلاق", color = Color(0xFF00E5FF))
                        }
                    }
                )
            }

            // Top Header & Filter Bar
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .background(Color(0xFF0F172A))
                    .statusBarsPadding()
            ) {
                // Top Bar with Back Button
                Surface(
                    color = Color(0xFF0F172A),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = onBackClick,
                                modifier = Modifier.testTag("map_back_btn")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = Color.White)
                            }
                            Text(
                                if (state.isRadarMode) "🛰️ رادار الخدمات المحلي (أوفلاين)" else "🗺️ خريطة دليل اليمن المباشرة",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Entity Count Badge
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF00E5FF).copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "${radarPoints.size} متوفر",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00E5FF),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Filter Bar
                MapFilterBar(
                    selectedCategory = state.selectedCategory,
                    onCategorySelected = { state.selectedCategory = it },
                    selectedCity = state.selectedCity,
                    onCitySelected = { city ->
                        state.selectedCity = city
                        if (city != "الكل") {
                            val coords = OfflineMapManager.getCityCoordinates(city)
                            viewModel.updateUserLocation(coords.latitude, coords.longitude)
                        }
                    },
                    searchQuery = state.searchQuery,
                    onSearchQueryChange = { state.searchQuery = it },
                    themeColors = themeColors
                )
            }

            // 🎛️ Special Mode Buttons (Vela 3D, AR, Aleph) على الجانب الأيمن
            if (state.currentMapMode != MapMode.AR_VPS) {
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 14.dp)
                        .navigationBarsPadding(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1. Vela 3D (الخرائط ثلاثية الأبعاد) // [تحسين مستقبلي: MapLibre FillExtrusion للمباني ثلاثية الأبعاد]
                    FloatingActionButton(
                        onClick = {
                            state.currentMapMode = MapMode.VELA_3D
                            state.isRadarMode = false
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("تم تفعيل خريطة Vela 3D ثلاثية الأبعاد 🏙️")
                            }
                        },
                        containerColor = if (state.currentMapMode == MapMode.VELA_3D && !state.isRadarMode) Color(0xFF00E5FF) else Color(0xFF0F172A).copy(alpha = 0.90f),
                        contentColor = if (state.currentMapMode == MapMode.VELA_3D && !state.isRadarMode) Color(0xFF0F172A) else Color.White,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .size(46.dp)
                            .testTag("mode_vela3d_fab")
                    ) {
                        Text("🏙️", fontSize = 18.sp)
                    }

                    // 2. VPS AR Mode (كاميرا الواقع المعزز)
                    FloatingActionButton(
                        onClick = {
                            state.currentMapMode = MapMode.AR_VPS
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("جاري تشغيل كاميرا الواقع المعزز (VPS) 📷")
                            }
                        },
                        containerColor = if (state.currentMapMode == MapMode.AR_VPS) Color(0xFF00E5FF) else Color(0xFF0F172A).copy(alpha = 0.90f),
                        contentColor = if (state.currentMapMode == MapMode.AR_VPS) Color(0xFF0F172A) else Color.White,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .size(46.dp)
                            .testTag("mode_ar_vps_fab")
                    ) {
                        Text("📷", fontSize = 18.sp)
                    }

                    // 3. المساعد البصري الذكي (Aleph)
                    FloatingActionButton(
                        onClick = {
                            state.isAlephAssistantOpen = true
                        },
                        containerColor = if (state.isAlephAssistantOpen) Color(0xFF10B981) else Color(0xFF0F172A).copy(alpha = 0.90f),
                        contentColor = Color.White,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .size(46.dp)
                            .testTag("mode_aleph_fab")
                    ) {
                        Text("👁️", fontSize = 18.sp)
                    }
                }
            }

            // Bottom Sheet for selected item
            state.selectedEntity?.let { entity ->
                MapBottomSheet(
                    entity = entity,
                    userLat = safeUserLat,
                    userLng = safeUserLng,
                    onDismiss = { state.selectedEntity = null },
                    onRequestBooking = { target ->
                        state.selectedEntity = null
                        when (target) {
                            is ProviderEntity -> {
                                state.bookingProviderTarget = target
                            }
                            is StoreEntity -> {
                                onRequestBooking(target)
                            }
                            is PropertyEntity -> {
                                onRequestBooking(target)
                            }
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
                    onAskAleph = { state.isAlephAssistantOpen = true },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                )
            }

            // 👁️ الوضع 4: المساعد البصري الذكي Aleph
            if (state.isAlephAssistantOpen) {
                AlephVisualAssistantDialog(
                    userLat = safeUserLat,
                    userLng = safeUserLng,
                    selectedCity = state.selectedCity,
                    nearbyProviders = filteredProviders,
                    nearbyStores = filteredStores,
                    targetEntity = state.selectedEntity,
                    onDismiss = { state.isAlephAssistantOpen = false }
                )
            }
        }
    }

    // Unified Direct Booking Dialog
    state.bookingProviderTarget?.let { provider ->
        BookingDialog(
            provider = provider,
            viewModel = viewModel,
            themeColors = themeColors,
            onDismiss = { state.bookingProviderTarget = null }
        )
    }
}
