@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.screens.map

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.ui.createBookingDirectly
import com.example.ui.screens.map.components.*
import com.example.ui.screens.map.utils.MapDistanceCalculator
import com.example.ui.screens.map.utils.OfflineMapManager
import com.example.utils.VisualThemePalette
import com.example.utils.resolveThemePalette
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

/**
 * 🗺️ MapScreen
 * Modern, modular, high-performance Map Screen integrating:
 * - RealLeafletMapView (OSM Tiles + Clustering + Offline Cache)
 * - RadarRenderer (Canvas radar with pulses & sweep animation)
 * - MapFilterBar (City, Search, and Category Chips)
 * - MapControls (Mode toggle, Heatmap, Zoom, Location)
 * - MapBottomSheet & MapBookingDialog
 * - Strict Memory Management via DisposableEffect
 * - Material 3 Custom SnackBar Host (Green, Red, Amber)
 */
@Composable
fun MapScreen(
    viewModel: MainViewModel,
    onBackClick: () -> Unit = {},
    onOpenProviderDetails: (ProviderEntity) -> Unit = {},
    onOpenStoreDetails: (StoreEntity) -> Unit = {},
    onOpenPropertyDetails: (PropertyEntity) -> Unit = {},
    onRequestBooking: (ProviderEntity) -> Unit = {},
    themeColors: VisualThemePalette = resolveThemePalette(viewModel.settings.collectAsState().value)
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Data streams from ViewModel
    val providers by viewModel.providers.collectAsState()
    val stores by viewModel.stores.collectAsState()
    val properties by viewModel.properties.collectAsState()

    val userLatState by viewModel.userLatitude.collectAsState()
    val userLngState by viewModel.userLongitude.collectAsState()

    // Persistent State
    var isRadarMode by rememberSaveable { mutableStateOf(false) }
    var isHeatmapActive by rememberSaveable { mutableStateOf(false) }
    var selectedCategory by rememberSaveable { mutableStateOf("ALL") }
    var selectedCity by rememberSaveable { mutableStateOf("الكل") }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var maxRangeKm by rememberSaveable { mutableFloatStateOf(25.0f) }

    // Dynamic technician offsets with safe cleanup
    val dynamicOffsets = remember { mutableStateMapOf<String, Pair<Double, Double>>() }

    // Selected Entity for Bottom Sheet / Booking
    var selectedEntity by remember { mutableStateOf<Any?>(null) }
    var bookingProviderTarget by remember { mutableStateOf<ProviderEntity?>(null) }

    // Permission launcher for Location
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

    // Clean up dynamic offsets on exit to prevent memory leaks
    DisposableEffect(Unit) {
        onDispose {
            dynamicOffsets.clear()
        }
    }

    // Filtered lists
    val filteredProviders = remember(providers, selectedCategory, selectedCity, searchQuery) {
        providers.filter { p ->
            val matchesCat = selectedCategory == "ALL" || selectedCategory == "PROVIDERS"
            val matchesCity = matchesCityFilter(selectedCity, p.cityId, p.area, p.localNeighborhood)
            val matchesQuery = searchQuery.isEmpty() || p.name.contains(searchQuery, ignoreCase = true) ||
                    p.profession.contains(searchQuery, ignoreCase = true) || p.customCategoryName.contains(searchQuery, ignoreCase = true) ||
                    p.specialization.contains(searchQuery, ignoreCase = true)
            matchesCat && matchesCity && matchesQuery
        }
    }

    val filteredStores = remember(stores, selectedCategory, selectedCity, searchQuery) {
        stores.filter { s ->
            val isMedical = s.sectionId.contains("medical") || s.categoryId.contains("medical") || s.categoryId.contains("pharmacy") || s.medicalLicenseNo.isNotBlank() || s.name.contains("طبي") || s.name.contains("مستشفى") || s.name.contains("صيدلية") || s.name.contains("مركز")
            val isRestaurant = !isMedical && (s.sectionId.contains("restaurant") || s.categoryId.contains("restaurant") || s.name.contains("مطعم") || s.name.contains("كافيه") || s.name.contains("مأكولات"))
            
            val matchesCat = when (selectedCategory) {
                "ALL" -> true
                "STORES" -> !isMedical && !isRestaurant
                "RESTAURANTS" -> isRestaurant
                "MEDICAL" -> isMedical
                else -> false
            }
            val matchesCity = matchesCityFilter(selectedCity, s.cityId, s.localNeighborhood, "")
            val matchesQuery = searchQuery.isEmpty() || s.name.contains(searchQuery, ignoreCase = true) || s.description.contains(searchQuery, ignoreCase = true)
            matchesCat && matchesCity && matchesQuery
        }
    }

    val filteredProperties = remember(properties, selectedCategory, selectedCity, searchQuery) {
        properties.filter { pr ->
            val matchesCat = selectedCategory == "ALL" || selectedCategory == "PROPERTIES"
            val matchesCity = matchesCityFilter(selectedCity, pr.cityId, pr.localNeighborhood, "")
            val matchesQuery = searchQuery.isEmpty() || pr.title.contains(searchQuery, ignoreCase = true) || pr.description.contains(searchQuery, ignoreCase = true)
            matchesCat && matchesCity && matchesQuery
        }
    }

    // Convert items to MapItemPoints for RadarRenderer
    val radarPoints = remember(filteredProviders, filteredStores, filteredProperties, userLatState, userLngState, maxRangeKm) {
        val list = mutableListOf<MarkerRenderer.MapItemPoint>()
        val radarMaxPx = 280.0f

        filteredProviders.forEach { p ->
            val coords = com.example.utils.getProviderCoords(p)
            val itemLat = if (coords.first != 0.0) coords.first else userLatState
            val itemLng = if (coords.second != 0.0) coords.second else userLngState

            val distanceMeters = MapDistanceCalculator.calculateDistanceMeters(userLatState, userLngState, itemLat, itemLng)
            val distanceKm = (distanceMeters / 1000.0).coerceAtLeast(0.1)

            val bearingDeg = calculateBearingDegrees(userLatState, userLngState, itemLat, itemLng)
            val mathAngleRad = Math.toRadians(bearingDeg - 90.0)
            val normalizedRadius = ((distanceKm / maxRangeKm.toDouble()).coerceIn(0.08, 1.0) * radarMaxPx).toFloat()

            val relX = (normalizedRadius * cos(mathAngleRad)).toFloat()
            val relY = (normalizedRadius * sin(mathAngleRad)).toFloat()

            list.add(
                MarkerRenderer.MapItemPoint(
                    id = p.id,
                    title = p.name,
                    type = "PROVIDER",
                    x = relX,
                    y = relY,
                    rating = p.rating.toDouble(),
                    isAvailable = p.isAvailable,
                    originalItem = p
                )
            )
        }

        filteredStores.forEach { s ->
            val coords = com.example.utils.getStoreCoords(s)
            val itemLat = if (coords.first != 0.0) coords.first else userLatState
            val itemLng = if (coords.second != 0.0) coords.second else userLngState

            val isMedical = s.sectionId.contains("medical") || s.categoryId.contains("medical") || s.categoryId.contains("pharmacy") || s.medicalLicenseNo.isNotBlank() || s.name.contains("طبي") || s.name.contains("مستشفى") || s.name.contains("صيدلية") || s.name.contains("مركز")
            val isRestaurant = !isMedical && (s.sectionId.contains("restaurant") || s.categoryId.contains("restaurant") || s.name.contains("مطعم") || s.name.contains("كافيه") || s.name.contains("مأكولات"))
            val typeStr = if (isMedical) "MEDICAL" else if (isRestaurant) "RESTAURANT" else "STORE"

            val distanceMeters = MapDistanceCalculator.calculateDistanceMeters(userLatState, userLngState, itemLat, itemLng)
            val distanceKm = (distanceMeters / 1000.0).coerceAtLeast(0.1)

            val bearingDeg = calculateBearingDegrees(userLatState, userLngState, itemLat, itemLng)
            val mathAngleRad = Math.toRadians(bearingDeg - 90.0)
            val normalizedRadius = ((distanceKm / maxRangeKm.toDouble()).coerceIn(0.08, 1.0) * radarMaxPx).toFloat()

            val relX = (normalizedRadius * cos(mathAngleRad)).toFloat()
            val relY = (normalizedRadius * sin(mathAngleRad)).toFloat()

            list.add(
                MarkerRenderer.MapItemPoint(
                    id = s.id,
                    title = s.name,
                    type = typeStr,
                    x = relX,
                    y = relY,
                    rating = s.rating.toDouble(),
                    isAvailable = s.isActive,
                    originalItem = s
                )
            )
        }

        filteredProperties.forEach { pr ->
            val coords = com.example.utils.getPropertyCoords(pr)
            val itemLat = if (coords.first != 0.0) coords.first else userLatState
            val itemLng = if (coords.second != 0.0) coords.second else userLngState

            val distanceMeters = MapDistanceCalculator.calculateDistanceMeters(userLatState, userLngState, itemLat, itemLng)
            val distanceKm = (distanceMeters / 1000.0).coerceAtLeast(0.1)

            val bearingDeg = calculateBearingDegrees(userLatState, userLngState, itemLat, itemLng)
            val mathAngleRad = Math.toRadians(bearingDeg - 90.0)
            val normalizedRadius = ((distanceKm / maxRangeKm.toDouble()).coerceIn(0.08, 1.0) * radarMaxPx).toFloat()

            val relX = (normalizedRadius * cos(mathAngleRad)).toFloat()
            val relY = (normalizedRadius * sin(mathAngleRad)).toFloat()

            list.add(
                MarkerRenderer.MapItemPoint(
                    id = pr.id,
                    title = pr.title,
                    type = "PROPERTY",
                    x = relX,
                    y = relY,
                    rating = 5.0,
                    isAvailable = pr.isActive,
                    originalItem = pr
                )
            )
        }

        list
    }

    Scaffold(
        containerColor = Color(0xFF020617),
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
                .padding(paddingValues)
        ) {
            // Main Map View (OSM Leaflet or Radar Canvas)
            if (isRadarMode) {
                RadarRenderer(
                    items = radarPoints,
                    selectedItemId = when (val ent = selectedEntity) {
                        is ProviderEntity -> ent.id
                        is StoreEntity -> ent.id
                        is PropertyEntity -> ent.id
                        else -> null
                    },
                    onItemSelected = { item ->
                        selectedEntity = item.originalItem
                    },
                    isHeatmapActive = isHeatmapActive,
                    maxRangeKm = maxRangeKm,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                RealLeafletMapView(
                    userCoords = Pair(userLatState, userLngState),
                    nearbyProviders = filteredProviders,
                    nearbyStores = filteredStores,
                    nearbyProperties = filteredProperties,
                    dynamicOffsets = dynamicOffsets,
                    onProviderSelected = { selectedEntity = it },
                    onStoreSelected = { selectedEntity = it },
                    onPropertySelected = { selectedEntity = it },
                    onSwitchToRadar = { isRadarMode = true },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Top Header & Filter Bar
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
            ) {
                // Top Bar with Back Button
                Surface(
                    color = Color(0xFF0F172A).copy(alpha = 0.95f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(
                                onClick = onBackClick,
                                modifier = Modifier.testTag("map_back_btn")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = Color.White)
                            }
                            Text(
                                if (isRadarMode) "📡 رادار الخدمات الذكي" else "🗺️ خريطة دليل اليمن المباشرة",
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
                                text = "${radarPoints.size} خدمة متوفرة",
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
                    selectedCategory = selectedCategory,
                    onCategorySelected = { selectedCategory = it },
                    selectedCity = selectedCity,
                    onCitySelected = { city ->
                        selectedCity = city
                        val coords = OfflineMapManager.getCityCoordinates(city)
                        viewModel.updateUserLocation(coords.latitude, coords.longitude)
                    },
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    themeColors = themeColors
                )
            }

            // Map Controls (Right / Floating)
            MapControls(
                isRadarMode = isRadarMode,
                onToggleRadarMode = { isRadarMode = !isRadarMode },
                isHeatmapActive = isHeatmapActive,
                onToggleHeatmap = {
                    isHeatmapActive = !isHeatmapActive
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            if (isHeatmapActive) "تم تفعيل الخريطة الحرارية لكثافة الخدمات 🔥" else "تم إيقاف الخريطة الحرارية"
                        )
                    }
                },
                onZoomIn = { /* Zoom handled on map */ },
                onZoomOut = { /* Zoom handled on map */ },
                onRecenterLocation = {
                    val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    if (hasFine) {
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
                isGpsActive = viewModel.isGpsTrackingActive.collectAsState().value,
                themeColors = themeColors,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(bottom = if (selectedEntity != null) 200.dp else 20.dp)
            )

            // Bottom Sheet for selected item
            selectedEntity?.let { entity ->
                MapBottomSheet(
                    entity = entity,
                    userLat = userLatState,
                    userLng = userLngState,
                    onDismiss = { selectedEntity = null },
                    onRequestBooking = { p ->
                        bookingProviderTarget = p
                    },
                    onOpenDetails = { ent ->
                        when (ent) {
                            is ProviderEntity -> onOpenProviderDetails(ent)
                            is StoreEntity -> onOpenStoreDetails(ent)
                            is PropertyEntity -> onOpenPropertyDetails(ent)
                        }
                    },
                    themeColors = themeColors,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }

    // Direct Map Booking Dialog
    bookingProviderTarget?.let { provider ->
        MapBookingDialog(
            provider = provider,
            userLat = userLatState,
            userLng = userLngState,
            onDismiss = { bookingProviderTarget = null },
            onConfirmBooking = { notes ->
                viewModel.createBookingDirectly(
                    provider = provider,
                    notes = notes,
                    onSuccess = {
                        bookingProviderTarget = null
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("تم إرسال طلب الحجز بنجاح إلى ${provider.name} ✓")
                        }
                    },
                    onError = { err: String ->
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("فشل إرسال طلب الحجز: $err ⚠️")
                        }
                    }
                )
            },
            themeColors = themeColors
        )
    }
}

fun calculateBearingDegrees(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    if (lat1 == lat2 && lng1 == lng2) return 0.0
    val dLng = Math.toRadians(lng2 - lng1)
    val phi1 = Math.toRadians(lat1)
    val phi2 = Math.toRadians(lat2)
    val y = kotlin.math.sin(dLng) * kotlin.math.cos(phi2)
    val x = kotlin.math.cos(phi1) * kotlin.math.sin(phi2) - kotlin.math.sin(phi1) * kotlin.math.cos(phi2) * kotlin.math.cos(dLng)
    val bearing = Math.atan2(y, x)
    return (Math.toDegrees(bearing) + 360.0) % 360.0
}

fun matchesCityFilter(
    selectedCity: String,
    cityId: String,
    area: String,
    neighborhood: String
): Boolean {
    if (selectedCity.isEmpty() || selectedCity == "الكل" || selectedCity == "جميع المدن") {
        return true
    }
    val targetNorm = selectedCity.trim().lowercase()
    val combinedField = "$cityId $area $neighborhood".lowercase()

    val aliases = when {
        targetNorm.contains("صنعاء") || targetNorm.contains("sanaa") -> listOf("صنعاء", "san", "sanaa", "ye_san", "ye_sana_cap", "أمانة العاصمة", "العاصمة", "حدة", "السبعين", "شعوب", "معين", "التحرير")
        targetNorm.contains("عدن") || targetNorm.contains("aden") -> listOf("عدن", "ade", "aden", "ye_ade", "خور مكسر", "المنصورة", "كريتر", "المعلا", "الشيخ عثمان")
        targetNorm.contains("تعز") || targetNorm.contains("taiz") -> listOf("تعز", "tai", "taiz", "ye_tai")
        targetNorm.contains("الحديدة") || targetNorm.contains("hodeidah") || targetNorm.contains("hud") -> listOf("الحديدة", "hod", "hud", "hodeidah", "ye_hod")
        targetNorm.contains("إب") || targetNorm.contains("ibb") -> listOf("إب", "ibb", "ye_ibb")
        targetNorm.contains("حضرموت") || targetNorm.contains("المكلا") || targetNorm.contains("mukalla") -> listOf("حضرموت", "المكلا", "had", "mukalla", "ye_had")
        targetNorm.contains("مأرب") || targetNorm.contains("marib") -> listOf("مأرب", "mar", "marib", "ye_mar")
        else -> listOf(targetNorm)
    }

    return aliases.any { alias -> combinedField.contains(alias) } || combinedField.isBlank()
}
