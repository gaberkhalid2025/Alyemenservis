@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.screens.map

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.example.data.PropertyEntity
import com.example.data.ProviderEntity
import com.example.data.StoreEntity
import com.example.ui.MainViewModel
import com.example.ui.dialogs.BookingDialog
import com.example.ui.screens.map.components.MapBottomSheet
import com.example.ui.screens.map.components.RadarRenderer
import com.example.ui.screens.map.utils.OfflineMapManager
import com.example.utils.VisualThemePalette
import com.example.utils.resolveThemePalette
import kotlinx.coroutines.launch

/**
 * 🗺️ MapScreenContent - Vela Map (Zero Keys, 100% Free & Open Source)
 * - خريطة عالمية مجانية متعددة الطبقات (Satellite, OSM, HOT, Topo)
 * - فتح افتراضي على صور الأقمار الصناعية (Satellite)
 * - تنقل سريع بين المدن اليمنية (Fly-To)
 * - بطاقة سفلية للتواصل المباشر مع الفنيين والمحلات
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

    val adminSettings by viewModel.settings.collectAsState()

    // مزامنة الطبقة الافتراضية المحددة من الإدارة (الافتراضي satellite)
    LaunchedEffect(adminSettings.defaultMapLayer) {
        if (adminSettings.defaultMapLayer.isNotBlank()) {
            state.currentLayer = adminSettings.defaultMapLayer
        }
    }

    // إعداد أشرطة النظام
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

    androidx.activity.compose.BackHandler(enabled = true) {
        if (state.selectedEntity != null) {
            state.resetSelection()
        } else if (state.showLayerSelector) {
            state.showLayerSelector = false
        } else {
            onBackClick()
        }
    }

    // مصادر البيانات
    val providers by viewModel.providers.collectAsState()
    val stores by viewModel.stores.collectAsState()
    val properties by viewModel.properties.collectAsState()

    val userLatState by viewModel.userLatitude.collectAsState()
    val userLngState by viewModel.userLongitude.collectAsState()

    val fallbackCityCoords = remember(state.selectedCity) {
        OfflineMapManager.getCityCoordinates(state.selectedCity)
    }
    val safeUserLat = if (userLatState != 0.0 && !userLatState.isNaN()) userLatState else fallbackCityCoords.latitude
    val safeUserLng = if (userLngState != 0.0 && !userLngState.isNaN()) userLngState else fallbackCityCoords.longitude

    // طلب إذن الـ GPS
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            viewModel.startLocationUpdates()
            coroutineScope.launch {
                snackbarHostState.showSnackbar("تم تحديد موقعك بدقة بنجاح 📍")
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

    DisposableEffect(Unit) {
        onDispose {
            state.clearOffsets()
        }
    }

    // تصفية العناصر جغرافياً وحسب التصنيف
    val filteredProviders = remember(providers, state.selectedCategory, state.selectedCity, state.searchQuery) {
        MapScreenFilters.filterProviders(providers, state.selectedCategory, state.selectedCity, state.searchQuery)
    }

    val filteredStores = remember(stores, state.selectedCategory, state.selectedCity, state.searchQuery) {
        MapScreenFilters.filterStores(stores, state.selectedCategory, state.selectedCity, state.searchQuery)
    }

    val filteredProperties = remember(properties, state.selectedCategory, state.selectedCity, state.searchQuery) {
        MapScreenFilters.filterProperties(properties, state.selectedCategory, state.selectedCity, state.searchQuery)
    }

    val radarPoints = remember(filteredProviders, filteredStores, filteredProperties, safeUserLat, safeUserLng) {
        MapScreenFilters.buildRadarPoints(
            filteredProviders = filteredProviders,
            filteredStores = filteredStores,
            filteredProperties = filteredProperties,
            safeUserLat = safeUserLat,
            safeUserLng = safeUserLng
        )
    }

    // المدن اليمنية الرئيسية للتنقل السريع
    val quickCities = remember {
        listOf("صنعاء", "عدن", "تعز", "الحديدة", "إب", "المكلا", "مأرب")
    }

    // الفئات الرئيسية للفلترة السريعة
    val filterCategories = remember {
        listOf(
            Pair("ALL", "الكل"),
            Pair("spaka", "سباكة 🔧"),
            Pair("kahraba", "كهرباء ⚡"),
            Pair("ac", "تكييف ❄️"),
            Pair("STORES", "محلات ومواد 🏬"),
            Pair("RESTAURANTS", "مطاعم 🍔"),
            Pair("PROPERTIES", "عقارات 🏠"),
            Pair("MEDICAL", "طبي 🏥")
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = Color(0xFF0B111E),
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
                .background(Color(0xFF0B111E))
                .padding(paddingValues)
        ) {
            // 1. عرض الخريطة التفاعلية الكاملة (Vela Map)
            if (!state.isRadarMode) {
                RealLeafletMapView(
                    userCoords = Pair(safeUserLat, safeUserLng),
                    nearbyProviders = filteredProviders,
                    nearbyStores = filteredStores,
                    nearbyProperties = filteredProperties,
                    dynamicOffsets = state.dynamicOffsets,
                    selectedCity = state.selectedCity,
                    currentLayer = state.currentLayer,
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
            } else {
                // شاشة الرادار المحلي
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
            }

            // 2. الهيدر العائم في أعلى الشاشة (Floating Header matching Screenshot 2)
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // شريط العنوان والشارة وزر الإغلاق
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // مساحة توازن فارغة
                    Spacer(modifier = Modifier.size(38.dp))

                    // شارة العنوان المركزية: "خريطة Vela Map • اليمن OSM"
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF0F172A).copy(alpha = 0.92f),
                        border = BorderStroke(1.dp, Color(0xFF334155)),
                        shadowElevation = 8.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color(0xFF10B981), CircleShape)
                            )
                            Text(
                                text = "خريطة Vela Map • اليمن",
                                color = Color.White,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFD97706)
                            ) {
                                Text(
                                    text = "OSM",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    // زر الإغلاق الدائري X
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF0F172A).copy(alpha = 0.88f),
                        border = BorderStroke(1.dp, Color(0xFF334155)),
                        modifier = Modifier.size(38.dp)
                    ) {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier.fillMaxSize().testTag("map_close_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "إغلاق",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // صف المدن اليمنية (Quick City Fly-To)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickCities.forEach { city ->
                        val isSelected = state.selectedCity == city
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) Color(0xFF0284C7) else Color(0xFF0F172A).copy(alpha = 0.85f),
                            border = BorderStroke(1.dp, if (isSelected) Color(0xFF38BDF8) else Color(0xFF334155)),
                            modifier = Modifier
                                .clickable {
                                    state.selectedCity = city
                                    val coords = OfflineMapManager.getCityCoordinates(city)
                                    viewModel.updateUserLocation(coords.latitude, coords.longitude)
                                }
                                .testTag("city_chip_$city")
                        ) {
                            Text(
                                text = city,
                                color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                                fontSize = 11.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                // صف الفئات (Category Filter Chips)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    filterCategories.forEach { (catKey, catLabel) ->
                        val isSelected = state.selectedCategory == catKey
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) Color(0xFF059669) else Color(0xFF0F172A).copy(alpha = 0.85f),
                            border = BorderStroke(1.dp, if (isSelected) Color(0xFF34D399) else Color(0xFF334155)),
                            modifier = Modifier
                                .clickable { state.selectedCategory = catKey }
                                .testTag("category_chip_$catKey")
                        ) {
                            Text(
                                text = catLabel,
                                color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                                fontSize = 11.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            // 3. أزرار التحكم العائمة على اليسار (Matching Screenshots 1 & 2)
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 14.dp, top = 145.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // زر طبقات الخريطة (Layers Selector Button)
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF0F172A).copy(alpha = 0.92f),
                    border = BorderStroke(1.5.dp, Color(0xFF38BDF8)),
                    shadowElevation = 8.dp,
                    modifier = Modifier.size(44.dp)
                ) {
                    IconButton(
                        onClick = { state.showLayerSelector = !state.showLayerSelector },
                        modifier = Modifier.fillMaxSize().testTag("map_layer_toggle_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = "طبقات الخريطة",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // زر معلومات خريطة Vela Map (Amber circle 'i')
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFD97706),
                    shadowElevation = 8.dp,
                    modifier = Modifier.size(44.dp)
                ) {
                    IconButton(
                        onClick = { state.showInfoDialog = true },
                        modifier = Modifier.fillMaxSize().testTag("map_info_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "معلومات الخريطة",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // زر تحديد موقعي (GPS Target)
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF0F172A).copy(alpha = 0.92f),
                    border = BorderStroke(1.5.dp, Color(0xFF00E5FF)),
                    shadowElevation = 8.dp,
                    modifier = Modifier.size(44.dp)
                ) {
                    IconButton(
                        onClick = {
                            state.panOffset = Offset.Zero
                            val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                            if (hasFine) {
                                viewModel.startLocationUpdates()
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("تم التمركز على موقعك الفعلي 📍")
                                }
                            } else {
                                locationPermissionLauncher.launch(
                                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                                )
                            }
                        },
                        modifier = Modifier.fillMaxSize().testTag("recenter_gps_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = "موقعي",
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // زر التبديل إلى الرادار / الخريطة
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF0F172A).copy(alpha = 0.92f),
                    border = BorderStroke(1.5.dp, if (state.isRadarMode) Color(0xFF10B981) else Color(0xFF94A3B8)),
                    shadowElevation = 8.dp,
                    modifier = Modifier.size(44.dp)
                ) {
                    IconButton(
                        onClick = { state.isRadarMode = !state.isRadarMode },
                        modifier = Modifier.fillMaxSize().testTag("toggle_radar_btn")
                    ) {
                        Icon(
                            imageVector = if (state.isRadarMode) Icons.Default.Map else Icons.Default.Sensors,
                            contentDescription = "الرادار",
                            tint = if (state.isRadarMode) Color(0xFF10B981) else Color(0xFF38BDF8),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // أيقونة البوصلة
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF0F172A).copy(alpha = 0.92f),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    shadowElevation = 6.dp,
                    modifier = Modifier.size(38.dp)
                ) {
                    IconButton(
                        onClick = {
                            // إعادة توجيه الكاميرا إلى العاصمة صنعاء
                            state.selectedCity = "صنعاء"
                            viewModel.updateUserLocation(15.3694, 44.1910)
                        },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Explore,
                            contentDescription = "البوصلة",
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // قائمة طبقات الخريطة المنبثقة (Layer Selector Popup matching Screenshot 1)
            if (state.showLayerSelector) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF0B132B).copy(alpha = 0.97f),
                    border = BorderStroke(1.5.dp, Color(0xFF1E3A8A)),
                    shadowElevation = 16.dp,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 68.dp, top = 145.dp)
                        .width(280.dp)
                        .testTag("layer_selector_popup")
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val layerOptions = listOf(
                            Pair("osm", "🌍 OpenStreetMap Standard (الرسمية)"),
                            Pair("hot", "🏙️ (HOT) خريطة الشوارع الحيوية"),
                            Pair("topo", "🗺️ (Topo) خريطة العالم والتضاريس"),
                            Pair("satellite", "🛰️ (Satellite) صور الأقمار الصناعية")
                        )

                        layerOptions.forEach { (key, label) ->
                            val isSelected = state.currentLayer == key
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) Color(0xFF1E293B) else Color.Transparent,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        state.currentLayer = key
                                        state.showLayerSelector = false
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("تم تفعيل: $label")
                                        }
                                    }
                                    .padding(vertical = 4.dp, horizontal = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp, horizontal = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) Color(0xFF00E5FF) else Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color(0xFF00E5FF),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. أزرار التكبير والتصغير على اليمين (Zoom In / Out)
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 14.dp, bottom = if (state.selectedEntity != null) 220.dp else 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Zoom In (+)
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF0F172A).copy(alpha = 0.90f),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    shadowElevation = 6.dp,
                    modifier = Modifier.size(44.dp)
                ) {
                    IconButton(
                        onClick = { state.zoomScale = (state.zoomScale * 1.30f).coerceIn(0.25f, 8.0f) },
                        modifier = Modifier.fillMaxSize().testTag("map_zoom_in_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "تكبير", tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                }

                // Zoom Out (-)
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF0F172A).copy(alpha = 0.90f),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    shadowElevation = 6.dp,
                    modifier = Modifier.size(44.dp)
                ) {
                    IconButton(
                        onClick = { state.zoomScale = (state.zoomScale / 1.30f).coerceIn(0.25f, 8.0f) },
                        modifier = Modifier.fillMaxSize().testTag("map_zoom_out_btn")
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "تصغير", tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                }
            }

            // 5. شارة أسفل الشاشة توضح عدد العناصر المكتشفة ومصدر الخريطة
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF0F172A).copy(alpha = 0.88f),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .navigationBarsPadding()
                    .padding(start = 16.dp, bottom = if (state.selectedEntity != null) 230.dp else 20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color(0xFF10B981), CircleShape)
                    )
                    Text(
                        text = "Vela Map • ${radarPoints.size} خدمة معتمدة",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // 6. البطاقة السفلية للعنصر المحدد (Map Bottom Sheet)
            state.selectedEntity?.let { entity ->
                MapBottomSheet(
                    entity = entity,
                    userLat = safeUserLat,
                    userLng = safeUserLng,
                    onDismiss = { state.selectedEntity = null },
                    onRequestBooking = { target ->
                        state.selectedEntity = null
                        when (target) {
                            is ProviderEntity -> state.bookingProviderTarget = target
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
                    onOpenChat = { ent ->
                        state.selectedEntity = null
                        when (ent) {
                            is ProviderEntity -> {
                                viewModel.openDirectChat(
                                    targetUserId = ent.id.ifBlank { ent.phone },
                                    targetUserName = ent.name,
                                    targetUserPhoto = ent.profileImage,
                                    relatedEntityId = ent.id,
                                    relatedEntityType = "PROVIDER"
                                )
                            }
                            is StoreEntity -> {
                                viewModel.openDirectChat(
                                    targetUserId = ent.id.ifBlank { ent.phone },
                                    targetUserName = ent.name,
                                    targetUserPhoto = ent.logoImage,
                                    relatedEntityId = ent.id,
                                    relatedEntityType = "STORE"
                                )
                            }
                            is PropertyEntity -> {
                                viewModel.openDirectChat(
                                    targetUserId = ent.id.ifBlank { ent.phone },
                                    targetUserName = ent.title,
                                    targetUserPhoto = ent.images.firstOrNull() ?: "",
                                    relatedEntityId = ent.id,
                                    relatedEntityType = "PROPERTY"
                                )
                            }
                        }
                    },
                    themeColors = themeColors,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                )
            }
        }
    }

    // 7. نافذة معلومات خريطة Vela Map (Info Dialog)
    if (state.showInfoDialog) {
        Dialog(onDismissRequest = { state.showInfoDialog = false }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF0F172A),
                border = BorderStroke(1.5.dp, Color(0xFF00E5FF)),
                shadowElevation = 24.dp,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🗺️ خريطة Vela Map الحرة",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E5FF)
                        )
                        IconButton(onClick = { state.showInfoDialog = false }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.Gray)
                        }
                    }

                    HorizontalDivider(color = Color(0xFF1E293B))

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        InfoItemRow(icon = "✨", title = "مجانية 100% وبدون مفاتيح (Zero Keys)", desc = "تعمل مباشرة دون الحاجة لأي مفاتيح أو اشتراكات")
                        InfoItemRow(icon = "🛰️", title = "صور الأقمار الصناعية (Satellite)", desc = "معاينة التضاريس والشوارع بدقة عالية من الفضاء")
                        InfoItemRow(icon = "🌍", title = "خرائط OpenStreetMap العالمية", desc = "تغطية كاملة للمدن والشوارع والأحياء اليمنية")
                        InfoItemRow(icon = "⚡", title = "رادار تفاعلي وتواصل مباشر", desc = "اتصال، محادثة واتساب، ودردشة فورية بنقرة واحدة")
                    }

                    Button(
                        onClick = { state.showInfoDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        Text("حسناً، فهمت", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }

    // 8. حوار الحجز الفوري
    state.bookingProviderTarget?.let { provider ->
        BookingDialog(
            provider = provider,
            viewModel = viewModel,
            themeColors = themeColors,
            onDismiss = { state.bookingProviderTarget = null }
        )
    }
}

@Composable
private fun InfoItemRow(icon: String, title: String, desc: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
        Text(text = icon, fontSize = 16.sp)
        Column {
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = Color.White)
            Text(text = desc, fontSize = 11.sp, color = Color(0xFF94A3B8))
        }
    }
}
