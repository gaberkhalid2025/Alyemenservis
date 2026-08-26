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
    var selectedCity by rememberSaveable { mutableStateOf("صنعاء") }
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

    // Base data with fallbacks to high-quality Yemeni mock data if empty
    val safeProviders = remember(providers, userLatState, userLngState, selectedCity) {
        if (providers.isNotEmpty()) {
            providers
        } else {
            listOf(
                ProviderEntity(
                    id = "mock_p_1",
                    name = "م. محمد الحرازي (كهربائي تمديدات)",
                    phone = "777123456",
                    categoryId = "kahraba",
                    profession = "كهربائي",
                    specialization = "تمديدات وصيانة منزلية",
                    area = "$selectedCity - الشارع الرئيسي",
                    cityId = selectedCity,
                    localNeighborhood = "الشارع الرئيسي",
                    rating = 4.9f,
                    numReviews = 24,
                    isAvailable = true,
                    latitude = userLatState + 0.003,
                    longitude = userLngState + 0.002,
                    isVip = true,
                    isVerified = true
                ),
                ProviderEntity(
                    id = "mock_p_2",
                    name = "المهندس علي السباك (صيانة صحية)",
                    phone = "771987654",
                    categoryId = "spaka",
                    profession = "سباك",
                    specialization = "تركيب وصيانة شبكات المياه والترميم",
                    area = "$selectedCity - الحي التجاري",
                    cityId = selectedCity,
                    localNeighborhood = "الحي التجاري",
                    rating = 4.7f,
                    numReviews = 18,
                    isAvailable = true,
                    latitude = userLatState - 0.005,
                    longitude = userLngState - 0.006,
                    isVip = false,
                    isVerified = true
                ),
                ProviderEntity(
                    id = "mock_p_3",
                    name = "ياسر لتبريد وتكييف الهواء",
                    phone = "775443322",
                    categoryId = "ac",
                    profession = "فني تكييف",
                    specialization = "صيانة مكيفات مركزي وسبليت",
                    area = "$selectedCity - شارع الجزائر",
                    cityId = selectedCity,
                    localNeighborhood = "شارع الجزائر",
                    rating = 4.8f,
                    numReviews = 15,
                    isAvailable = true,
                    latitude = userLatState + 0.006,
                    longitude = userLngState + 0.007,
                    isVip = true,
                    isVerified = true
                )
            )
        }
    }

    val safeStores = remember(stores, userLatState, userLngState, selectedCity) {
        if (stores.isNotEmpty()) {
            stores
        } else {
            listOf(
                StoreEntity(
                    id = "mock_s_1",
                    sectionId = "stores",
                    name = "سوبرماركت الأمانة والوفاء",
                    description = "أفضل المواد الغذائية والاستهلاكية بأسعار منافسة خدمة توصيل سريعة",
                    phone = "770112233",
                    categoryId = "supermarket",
                    cityId = selectedCity,
                    localNeighborhood = "شارع الخمسين",
                    rating = 4.5f,
                    numReviews = 42,
                    isActive = true,
                    latitude = userLatState - 0.004,
                    longitude = userLngState + 0.009,
                    isVerified = true,
                    workingHours = "7:00 AM - 12:00 PM"
                ),
                StoreEntity(
                    id = "mock_s_2",
                    sectionId = "medical",
                    categoryId = "pharmacy",
                    name = "صيدلية اليمن السعيد الكبرى",
                    description = "توفير كافة الأدوية والمستلزمات الطبية على مدار 24 ساعة",
                    phone = "773445566",
                    cityId = selectedCity,
                    localNeighborhood = "شارع الزبيري",
                    rating = 4.9f,
                    numReviews = 31,
                    isActive = true,
                    latitude = userLatState + 0.001,
                    longitude = userLngState - 0.003,
                    isVerified = true,
                    workingHours = "24/7 مفتوح دائماً"
                ),
                StoreEntity(
                    id = "mock_s_3",
                    sectionId = "restaurant",
                    categoryId = "restaurant",
                    name = "مطعم الشيباني الفاخر والحديث",
                    description = "أشهى المأكولات اليمنية والشعبية والسفري واللحم المندي والسلته والمقلقل",
                    phone = "775667788",
                    cityId = selectedCity,
                    localNeighborhood = "حدة المدينة",
                    rating = 4.8f,
                    numReviews = 112,
                    isActive = true,
                    latitude = userLatState - 0.002,
                    longitude = userLngState + 0.012,
                    isVerified = true,
                    workingHours = "6:00 AM - 11:30 PM"
                )
            )
        }
    }

    val safeProperties = remember(properties, userLatState, userLngState, selectedCity) {
        if (properties.isNotEmpty()) {
            properties
        } else {
            listOf(
                PropertyEntity(
                    id = "mock_pr_1",
                    sectionId = "properties",
                    title = "شقة سكنية عائلية مفروشة للإيجار",
                    description = "شقة راقية تتكون من 4 غرف وصالة واسعة وحمامين ومطبخ جاهز، قريبة من الخدمات والمدارس والمواصلات.",
                    price = 120000.0,
                    currency = "YER",
                    type = "rent",
                    propertyType = "apartment",
                    phone = "774998877",
                    cityId = selectedCity,
                    localNeighborhood = "بيت بوس",
                    rating = 4.7f,
                    numReviews = 5,
                    isActive = true,
                    latitude = userLatState - 0.008,
                    longitude = userLngState + 0.005
                ),
                PropertyEntity(
                    id = "mock_pr_2",
                    sectionId = "properties",
                    title = "بيت مستقل للبيع دورين حجر ديوان",
                    description = "منزل حجر صمم بأعلى المواصفات، دورين جاهزين للسكن مباشرة، يقع على شارعين واسعين وقريب من السوق الرئيسي.",
                    price = 45000000.0,
                    currency = "YER",
                    type = "sale",
                    propertyType = "house",
                    phone = "770889900",
                    cityId = selectedCity,
                    localNeighborhood = "شارع تعز",
                    rating = 5.0f,
                    numReviews = 3,
                    isActive = true,
                    latitude = userLatState + 0.009,
                    longitude = userLngState - 0.002
                )
            )
        }
    }

    // Filtered lists
    val filteredProviders = remember(safeProviders, selectedCategory, selectedCity, searchQuery) {
        safeProviders.filter { p ->
            val matchesCat = selectedCategory == "ALL" || selectedCategory == "PROVIDERS"
            val matchesCity = selectedCity.isEmpty() || p.area.contains(selectedCity) || p.cityId.contains(selectedCity) || p.localNeighborhood.contains(selectedCity)
            val matchesQuery = searchQuery.isEmpty() || p.name.contains(searchQuery, ignoreCase = true) ||
                    p.profession.contains(searchQuery, ignoreCase = true) || p.customCategoryName.contains(searchQuery, ignoreCase = true)
            matchesCat && matchesCity && matchesQuery
        }
    }

    val filteredStores = remember(safeStores, selectedCategory, selectedCity, searchQuery) {
        safeStores.filter { s ->
            val isMedical = s.sectionId.contains("medical") || s.categoryId.contains("medical") || s.name.contains("طبي") || s.name.contains("صيدلية")
            val isRestaurant = !isMedical && (s.sectionId.contains("restaurant") || s.categoryId.contains("restaurant") || s.name.contains("مطعم") || s.name.contains("كافيه"))
            
            val matchesCat = when (selectedCategory) {
                "ALL" -> true
                "STORES" -> !isMedical && !isRestaurant
                "RESTAURANTS" -> isRestaurant
                "MEDICAL" -> isMedical
                else -> false
            }
            val matchesCity = selectedCity.isEmpty() || s.cityId.contains(selectedCity) || s.localNeighborhood.contains(selectedCity)
            val matchesQuery = searchQuery.isEmpty() || s.name.contains(searchQuery, ignoreCase = true) || s.description.contains(searchQuery, ignoreCase = true)
            matchesCat && matchesCity && matchesQuery
        }
    }

    val filteredProperties = remember(safeProperties, selectedCategory, selectedCity, searchQuery) {
        safeProperties.filter { pr ->
            val matchesCat = selectedCategory == "ALL" || selectedCategory == "PROPERTIES"
            val matchesCity = selectedCity.isEmpty() || pr.cityId.contains(selectedCity) || pr.localNeighborhood.contains(selectedCity)
            val matchesQuery = searchQuery.isEmpty() || pr.title.contains(searchQuery, ignoreCase = true) || pr.description.contains(searchQuery, ignoreCase = true)
            matchesCat && matchesCity && matchesQuery
        }
    }

    // Convert items to MapItemPoints for RadarRenderer
    val radarPoints = remember(filteredProviders, filteredStores, filteredProperties, userLatState, userLngState) {
        val list = mutableListOf<MarkerRenderer.MapItemPoint>()
        var angle = 0.0

        filteredProviders.forEachIndexed { i, p ->
            val d = MapDistanceCalculator.calculateDistanceMeters(userLatState, userLngState, p.latitude, p.longitude)
            val r = (d / 1000.0).coerceIn(1.0, 30.0).toFloat() * 15f
            val rad = Math.toRadians(angle)
            list.add(
                MarkerRenderer.MapItemPoint(
                    id = p.id,
                    title = p.name,
                    type = "PROVIDER",
                    x = (r * cos(rad)).toFloat(),
                    y = (r * sin(rad)).toFloat(),
                    rating = p.rating.toDouble(),
                    isAvailable = p.isAvailable,
                    originalItem = p
                )
            )
            angle += 45.0
        }

        filteredStores.forEachIndexed { i, s ->
            val isMedical = s.sectionId.contains("medical") || s.categoryId.contains("medical") || s.name.contains("طبي") || s.name.contains("صيدلية")
            val isRestaurant = !isMedical && (s.sectionId.contains("restaurant") || s.categoryId.contains("restaurant") || s.name.contains("مطعم") || s.name.contains("كافيه"))
            val typeStr = if (isMedical) "MEDICAL" else if (isRestaurant) "RESTAURANT" else "STORE"
            val d = MapDistanceCalculator.calculateDistanceMeters(userLatState, userLngState, s.latitude, s.longitude)
            val r = (d / 1000.0).coerceIn(1.0, 30.0).toFloat() * 15f
            val rad = Math.toRadians(angle)
            list.add(
                MarkerRenderer.MapItemPoint(
                    id = s.id,
                    title = s.name,
                    type = typeStr,
                    x = (r * cos(rad)).toFloat(),
                    y = (r * sin(rad)).toFloat(),
                    rating = s.rating.toDouble(),
                    isAvailable = s.isActive,
                    originalItem = s
                )
            )
            angle += 35.0
        }

        filteredProperties.forEachIndexed { i, pr ->
            val d = MapDistanceCalculator.calculateDistanceMeters(userLatState, userLngState, pr.latitude, pr.longitude)
            val r = (d / 1000.0).coerceIn(1.0, 30.0).toFloat() * 15f
            val rad = Math.toRadians(angle)
            list.add(
                MarkerRenderer.MapItemPoint(
                    id = pr.id,
                    title = pr.title,
                    type = "PROPERTY",
                    x = (r * cos(rad)).toFloat(),
                    y = (r * sin(rad)).toFloat(),
                    rating = 5.0,
                    isAvailable = pr.isActive,
                    originalItem = pr
                )
            )
            angle += 50.0
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
