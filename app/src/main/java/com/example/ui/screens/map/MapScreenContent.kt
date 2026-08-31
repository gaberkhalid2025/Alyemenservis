@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.screens.map

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.ui.screens.map.utils.OfflineMapManager
import com.example.utils.VisualThemePalette
import com.example.utils.resolveThemePalette
import kotlinx.coroutines.launch

/**
 * 🗺️ MapScreenContent
 * Primary presentation composable for the Map & Radar screen.
 */
@Composable
fun MapScreenContent(
    viewModel: MainViewModel,
    state: MapScreenState = rememberMapScreenState(),
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

    val safeUserLat = if (userLatState != 0.0) userLatState else 15.3694
    val safeUserLng = if (userLngState != 0.0) userLngState else 44.1910

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
            if (state.isRadarMode) {
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
            } else {
                RealLeafletMapView(
                    userCoords = Pair(safeUserLat, safeUserLng),
                    nearbyProviders = filteredProviders,
                    nearbyStores = filteredStores,
                    nearbyProperties = filteredProperties,
                    dynamicOffsets = state.dynamicOffsets,
                    onProviderSelected = { state.selectedEntity = it },
                    onStoreSelected = { state.selectedEntity = it },
                    onPropertySelected = { state.selectedEntity = it },
                    onSwitchToRadar = { state.isRadarMode = true },
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
                                if (state.isRadarMode) "📡 رادار الخدمات الذكي" else "🗺️ خريطة دليل اليمن المباشرة",
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

            // Map Controls (Right / Floating)
            MapControls(
                isRadarMode = state.isRadarMode,
                onToggleRadarMode = { state.isRadarMode = !state.isRadarMode },
                isHeatmapActive = state.isHeatmapActive,
                onToggleHeatmap = {
                    state.isHeatmapActive = !state.isHeatmapActive
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            if (state.isHeatmapActive) "تم تفعيل الخريطة الحرارية لكثافة الخدمات 🔥" else "تم إيقاف الخريطة الحرارية"
                        )
                    }
                },
                onZoomIn = { /* Handled natively on map */ },
                onZoomOut = { /* Handled natively on map */ },
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
                    .padding(bottom = if (state.selectedEntity != null) 200.dp else 20.dp)
            )

            // Bottom Sheet for selected item
            state.selectedEntity?.let { entity ->
                MapBottomSheet(
                    entity = entity,
                    userLat = safeUserLat,
                    userLng = safeUserLng,
                    onDismiss = { state.selectedEntity = null },
                    onRequestBooking = { p ->
                        state.bookingProviderTarget = p
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
    state.bookingProviderTarget?.let { provider ->
        MapBookingDialog(
            provider = provider,
            userLat = safeUserLat,
            userLng = safeUserLng,
            onDismiss = { state.bookingProviderTarget = null },
            onConfirmBooking = { notes ->
                viewModel.createBookingDirectly(
                    provider = provider,
                    notes = notes,
                    onSuccess = {
                        state.bookingProviderTarget = null
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
