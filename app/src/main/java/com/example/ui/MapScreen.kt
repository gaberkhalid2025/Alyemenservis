package com.example.ui

import com.example.utils.*

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.data.PropertyEntity
import com.example.data.ProviderEntity
import com.example.data.StoreEntity
import com.example.utils.ConnectionManager
import com.example.utils.calculateDistanceInMeters
import com.example.utils.formatDistance
import com.example.utils.getCityCenterCoords
import com.example.viewmodels.MapViewModel
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: MainViewModel,
    onBackClick: () -> Unit,
    onOpenProviderDetails: (ProviderEntity) -> Unit = {},
    onOpenStoreDetails: (StoreEntity) -> Unit = {},
    onOpenPropertyDetails: (PropertyEntity) -> Unit = {},
    onRequestBooking: (ProviderEntity) -> Unit = {}
) {
    val context = LocalContext.current
    val settingsState by viewModel.settings.collectAsState()

    // Connection Manager
    val connectionManager = remember { ConnectionManager(context) }
    val isOnline by connectionManager.isOnline.collectAsState()

    // Data Sources
    val providers by viewModel.providers.collectAsState(initial = emptyList())
    val stores by viewModel.stores.collectAsState(initial = emptyList())
    val properties by viewModel.properties.collectAsState(initial = emptyList())

    // User Location State - Default Sana'a (15.3694, 44.1910)
    var userLat by remember { mutableDoubleStateOf(15.3694) }
    var userLng by remember { mutableDoubleStateOf(44.1910) }

    // Map Controls
    var mapZoom by remember { mutableIntStateOf(12) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("ALL") } // ALL, PROVIDERS, STORES, RESTAURANTS, PROPERTIES
    var maxDistanceKm by remember { mutableFloatStateOf(20.0f) }
    var minRatingFilter by remember { mutableFloatStateOf(0.0f) }
    var onlyAvailableFilter by remember { mutableStateOf(false) }

    // Selected Entities
    var selectedProvider by remember { mutableStateOf<ProviderEntity?>(null) }
    var selectedStore by remember { mutableStateOf<StoreEntity?>(null) }
    var selectedProperty by remember { mutableStateOf<PropertyEntity?>(null) }

    // Extra UI Panels
    var showClosestSuggestions by remember { mutableStateOf(false) }
    var realtimeNoticeBanner by remember { mutableStateOf<String?>(null) }

    // Location Permission Handler
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fine = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarse = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        hasLocationPermission = fine || coarse
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            locationPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    // Pulse Effect & User Location Simulation/Tracker (5-second radar pulse)
    LaunchedEffect(Unit) {
        while (true) {
            delay(5000L)
            if (userLat != 0.0) {
                userLat += (Math.random() - 0.5) * 0.0001
                userLng += (Math.random() - 0.5) * 0.0001
            }
        }
    }

    // Real-time detection tracker for new items
    var lastProvidersCount by remember { mutableIntStateOf(providers.size) }
    var lastStoresCount by remember { mutableIntStateOf(stores.size) }
    var lastPropertiesCount by remember { mutableIntStateOf(properties.size) }

    LaunchedEffect(providers.size, stores.size, properties.size) {
        if (lastProvidersCount > 0 && providers.size > lastProvidersCount) {
            realtimeNoticeBanner = "🆕 تم إضافة فني جديد في خريطة خدمات اليمن!"
        } else if (lastStoresCount > 0 && stores.size > lastStoresCount) {
            realtimeNoticeBanner = "🆕 تم إضافة محل/مطعم جديد بالموقع!"
        } else if (lastPropertiesCount > 0 && properties.size > lastPropertiesCount) {
            realtimeNoticeBanner = "🆕 تم إضافة عقار جديد في المنطقة!"
        }
        lastProvidersCount = providers.size
        lastStoresCount = stores.size
        lastPropertiesCount = properties.size
    }

    // Categorized Filtering Logic
    val filteredProviders = remember(providers, userLat, userLng, searchQuery, selectedCategoryFilter, maxDistanceKm, minRatingFilter, onlyAvailableFilter) {
        if (selectedCategoryFilter != "ALL" && selectedCategoryFilter != "PROVIDERS") emptyList()
        else providers.filter { p ->
            val matchSearch = searchQuery.isEmpty() || p.name.contains(searchQuery, ignoreCase = true) || p.customCategoryName.contains(searchQuery, ignoreCase = true)
            val distMeters = calculateDistanceInMeters(userLat, userLng, p.latitude, p.longitude)
            val matchDist = (distMeters / 1000.0) <= maxDistanceKm
            val matchRating = p.rating >= minRatingFilter
            val matchAvail = !onlyAvailableFilter || p.isAvailable
            matchSearch && matchDist && matchRating && matchAvail
        }
    }

    val restaurantsList = remember(stores) {
        stores.filter { s ->
            s.sectionId.contains("restaurant", ignoreCase = true) ||
            s.name.contains("مطعم") || s.name.contains("كافيه") || s.name.contains("وجبات")
        }
    }

    val generalStoresList = remember(stores) {
        stores.filter { s ->
            !s.sectionId.contains("restaurant", ignoreCase = true) &&
            !s.name.contains("مطعم") && !s.name.contains("كافيه")
        }
    }

    val filteredStores = remember(generalStoresList, userLat, userLng, searchQuery, selectedCategoryFilter, maxDistanceKm, minRatingFilter) {
        if (selectedCategoryFilter != "ALL" && selectedCategoryFilter != "STORES") emptyList()
        else generalStoresList.filter { s ->
            val matchSearch = searchQuery.isEmpty() || s.name.contains(searchQuery, ignoreCase = true) || s.categoryId.contains(searchQuery, ignoreCase = true)
            val distMeters = calculateDistanceInMeters(userLat, userLng, s.latitude, s.longitude)
            val matchDist = (distMeters / 1000.0) <= maxDistanceKm
            val matchRating = s.rating >= minRatingFilter
            matchSearch && matchDist && matchRating
        }
    }

    val filteredRestaurants = remember(restaurantsList, userLat, userLng, searchQuery, selectedCategoryFilter, maxDistanceKm, minRatingFilter) {
        if (selectedCategoryFilter != "ALL" && selectedCategoryFilter != "RESTAURANTS") emptyList()
        else restaurantsList.filter { r ->
            val matchSearch = searchQuery.isEmpty() || r.name.contains(searchQuery, ignoreCase = true) || r.description.contains(searchQuery, ignoreCase = true)
            val distMeters = calculateDistanceInMeters(userLat, userLng, r.latitude, r.longitude)
            val matchDist = (distMeters / 1000.0) <= maxDistanceKm
            val matchRating = r.rating >= minRatingFilter
            matchSearch && matchDist && matchRating
        }
    }

    val filteredProperties = remember(properties, userLat, userLng, searchQuery, selectedCategoryFilter, maxDistanceKm, minRatingFilter) {
        if (selectedCategoryFilter != "ALL" && selectedCategoryFilter != "PROPERTIES") emptyList()
        else properties.filter { pr ->
            val matchSearch = searchQuery.isEmpty() || pr.title.contains(searchQuery, ignoreCase = true) || pr.propertyType.contains(searchQuery, ignoreCase = true)
            val distMeters = calculateDistanceInMeters(userLat, userLng, pr.latitude, pr.longitude)
            val matchDist = (distMeters / 1000.0) <= maxDistanceKm
            val matchRating = pr.rating >= minRatingFilter
            matchSearch && matchDist && matchRating
        }
    }

    val totalVisibleCount = filteredProviders.size + filteredStores.size + filteredRestaurants.size + filteredProperties.size

    // Service Density calculation for Circle Color
    val circleColorHex = when {
        totalVisibleCount > 10 -> "#ef4444" // Red
        totalVisibleCount in 3..10 -> "#eab308" // Yellow
        else -> "#22c55e" // Green
    }

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. FULL SCREEN MAP RENDERER (Background)
            LeafletRadarMapRenderer(
                userLat = userLat,
                userLng = userLng,
                zoom = mapZoom,
                maxDistanceKm = maxDistanceKm,
                circleColorHex = circleColorHex,
                providers = filteredProviders,
                stores = filteredStores,
                restaurants = filteredRestaurants,
                properties = filteredProperties,
                onSelectProvider = {
                    selectedProvider = it
                    selectedStore = null
                    selectedProperty = null
                },
                onSelectStore = {
                    selectedStore = it
                    selectedProvider = null
                    selectedProperty = null
                },
                onSelectProperty = {
                    selectedProperty = it
                    selectedProvider = null
                    selectedStore = null
                }
            )

            // 2. CONNECTION STATE BANNER (Top Overlay)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            ) {
                if (!isOnline) {
                    Surface(
                        color = Color(0xFFDC2626),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp, horizontal = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "أنت في وضع عدم الاتصال - يتم عرض البيانات المخزنة محلياً",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                realtimeNoticeBanner?.let { notice ->
                    Surface(
                        color = Color(0xFF059669),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { realtimeNoticeBanner = null }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp, horizontal = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(notice, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                // 3. SEARCH & TOP HEADER OVERLAY
                Surface(
                    shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
                    color = Color(0xFF0F172A).copy(alpha = 0.94f),
                    shadowElevation = 10.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        // Title & Back & Quick City Centers
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = onBackClick,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color(0xFF1E293B), CircleShape)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = Color.White)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("🗺️ خريطة خدمات اليمن", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .background(Color(0xFFF59E0B), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("⚡ رادار حي", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                        }
                                    }
                                    Text(
                                        text = "الموقع: اليمن (${String.format("%.3f, %.3f", userLat, userLng)})",
                                        fontSize = 10.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                // Weather Badge
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("☀️ صنعاء 26°C", fontSize = 10.sp, color = Color(0xFFFFD700), fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Search Bar
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("ابحث عن فني، محل، مطعم، عقار...", fontSize = 12.sp, color = Color.Gray) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFFF59E0B)) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "مسح", tint = Color.Gray)
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF1E293B),
                                unfocusedContainerColor = Color(0xFF1E293B),
                                focusedBorderColor = Color(0xFFF59E0B),
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Filter Chips Row
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            item {
                                FilterTypeChip(
                                    label = "الكل ($totalVisibleCount)",
                                    icon = "🌐",
                                    selected = selectedCategoryFilter == "ALL",
                                    onClick = { selectedCategoryFilter = "ALL" }
                                )
                            }
                            item {
                                FilterTypeChip(
                                    label = "الفنيون (${filteredProviders.size})",
                                    icon = "🛠️",
                                    selected = selectedCategoryFilter == "PROVIDERS",
                                    onClick = { selectedCategoryFilter = "PROVIDERS" }
                                )
                            }
                            item {
                                FilterTypeChip(
                                    label = "المحلات (${filteredStores.size})",
                                    icon = "🏪",
                                    selected = selectedCategoryFilter == "STORES",
                                    onClick = { selectedCategoryFilter = "STORES" }
                                )
                            }
                            item {
                                FilterTypeChip(
                                    label = "المطاعم (${filteredRestaurants.size})",
                                    icon = "🍔",
                                    selected = selectedCategoryFilter == "RESTAURANTS",
                                    onClick = { selectedCategoryFilter = "RESTAURANTS" }
                                )
                            }
                            item {
                                FilterTypeChip(
                                    label = "العقارات (${filteredProperties.size})",
                                    icon = "🏠",
                                    selected = selectedCategoryFilter == "PROPERTIES",
                                    onClick = { selectedCategoryFilter = "PROPERTIES" }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Distance Radius Slider & Quick Toggles
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "نطاق الرادار: ${maxDistanceKm.toInt()} كم",
                                fontSize = 11.sp,
                                color = Color(0xFFF59E0B),
                                fontWeight = FontWeight.Bold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (minRatingFilter >= 4.0f) Color(0xFFD97706) else Color(0xFF1E293B))
                                        .clickable { minRatingFilter = if (minRatingFilter >= 4.0f) 0.0f else 4.0f }
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("4.0+ ", fontSize = 10.sp, color = Color.White)
                                }

                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (onlyAvailableFilter) Color(0xFF10B981) else Color(0xFF1E293B))
                                        .clickable { onlyAvailableFilter = !onlyAvailableFilter }
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("متاح الآن ⚡", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Slider(
                            value = maxDistanceKm,
                            onValueChange = { maxDistanceKm = it },
                            valueRange = 1f..100f,
                            steps = 19,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFFF59E0B),
                                activeTrackColor = Color(0xFFF59E0B),
                                inactiveTrackColor = Color(0xFF334155)
                            ),
                            modifier = Modifier.height(24.dp)
                        )
                    }
                }
            }

            // 4. FLOATING ACTION CONTROLS (Right Side Overlay)
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp, top = 220.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Re-center Button
                FloatingActionButton(
                    onClick = {
                        val sanaa = getCityCenterCoords("ye_san")
                        userLat = sanaa.first
                        userLng = sanaa.second
                        Toast.makeText(context, "📍 تم إعادة التمركز في صنعاء", Toast.LENGTH_SHORT).show()
                    },
                    containerColor = Color(0xFF0F172A),
                    contentColor = Color(0xFFF59E0B),
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = "تمركز")
                }

                // Zoom In
                FloatingActionButton(
                    onClick = { mapZoom = (mapZoom + 1).coerceAtMost(18) },
                    containerColor = Color(0xFF0F172A),
                    contentColor = Color.White,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "تكبير")
                }

                // Zoom Out
                FloatingActionButton(
                    onClick = { mapZoom = (mapZoom - 1).coerceAtLeast(5) },
                    containerColor = Color(0xFF0F172A),
                    contentColor = Color.White,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Default.Clear, contentDescription = "تصغير")
                }

                // Closest Suggestions Button
                FloatingActionButton(
                    onClick = { showClosestSuggestions = !showClosestSuggestions },
                    containerColor = Color(0xFFF59E0B),
                    contentColor = Color.Black,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(Icons.Default.Star, contentDescription = "مقترحات")
                }

                // Share Location Button
                FloatingActionButton(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "📍 موقعي الحالي على خريطة دليل خدمات اليمن: https://maps.google.com/?q=$userLat,$userLng")
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "مشاركة موقعي"))
                    },
                    containerColor = Color(0xFF059669),
                    contentColor = Color.White,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "مشاركة")
                }
            }

            // 5. QUICK CITY SELECTOR (Bottom Left Floating)
            LazyRow(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 12.dp, bottom = if (selectedProvider != null || selectedStore != null || selectedProperty != null) 210.dp else 24.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val cities = listOf(
                    "صنعاء" to Pair(15.3694, 44.1910),
                    "عدن" to Pair(12.7855, 45.0186),
                    "تعز" to Pair(13.5794, 44.0205),
                    "الحديدة" to Pair(14.7979, 42.9530),
                    "حضرموت" to Pair(14.5424, 49.1242)
                )
                items(cities) { (cityName, coords) ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0F172A).copy(alpha = 0.9f))
                            .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
                            .clickable {
                                userLat = coords.first
                                userLng = coords.second
                                Toast.makeText(context, "📍 الانتقال إلى $cityName", Toast.LENGTH_SHORT).show()
                            }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Text("📍 $cityName", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 6. POPUP OVERLAY CARDS
            selectedProvider?.let { provider ->
                val distMeters = calculateDistanceInMeters(userLat, userLng, provider.latitude, provider.longitude)
                val formattedDist = formatDistance(distMeters)
                val etaText = calculateEtaText(distMeters)

                DetailedMapCardOverlay(
                    title = provider.name,
                    categoryBadge = "🛠️ ${provider.customCategoryName.ifEmpty { "فني متخصص" }}",
                    phone = provider.phone,
                    rating = provider.rating.toDouble(),
                    numReviews = provider.numReviews,
                    distanceText = formattedDist,
                    etaText = etaText,
                    onDismiss = { selectedProvider = null },
                    onOpenDetails = { onOpenProviderDetails(provider) },
                    onAction = { onRequestBooking(provider) },
                    actionLabel = "حجز الخدمة ⚡",
                    onDirections = {
                        openExternalDirections(context, provider.latitude, provider.longitude, provider.name)
                    },
                    onWhatsapp = {
                        val url = "https://api.whatsapp.com/send?phone=967${provider.phone.removePrefix("0")}"
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    }
                )
            }

            selectedStore?.let { store ->
                val distMeters = calculateDistanceInMeters(userLat, userLng, store.latitude, store.longitude)
                val formattedDist = formatDistance(distMeters)
                val etaText = calculateEtaText(distMeters)
                val isRest = store.sectionId.contains("restaurant", ignoreCase = true) ||
                        store.name.contains("مطعم") || store.name.contains("كافيه")

                DetailedMapCardOverlay(
                    title = store.name,
                    categoryBadge = if (isRest) "🍔 مطعم / كافيه" else "🏪 متجر تجاري",
                    phone = store.phone,
                    rating = store.rating.toDouble(),
                    numReviews = store.numReviews,
                    distanceText = formattedDist,
                    etaText = etaText,
                    onDismiss = { selectedStore = null },
                    onOpenDetails = { onOpenStoreDetails(store) },
                    onAction = {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${store.phone}"))
                        context.startActivity(intent)
                    },
                    actionLabel = if (isRest) "طلب الوجبة 🍔" else "اتصال بالمتجر 📞",
                    onDirections = {
                        openExternalDirections(context, store.latitude, store.longitude, store.name)
                    },
                    onWhatsapp = {
                        val url = "https://api.whatsapp.com/send?phone=967${store.phone.removePrefix("0")}"
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    }
                )
            }

            selectedProperty?.let { prop ->
                val distMeters = calculateDistanceInMeters(userLat, userLng, prop.latitude, prop.longitude)
                val formattedDist = formatDistance(distMeters)
                val etaText = calculateEtaText(distMeters)

                DetailedMapCardOverlay(
                    title = prop.title,
                    categoryBadge = "🏠 عقار - ${prop.type} (${prop.price} YER)",
                    phone = prop.phone,
                    rating = prop.rating.toDouble(),
                    numReviews = prop.numReviews,
                    distanceText = formattedDist,
                    etaText = etaText,
                    onDismiss = { selectedProperty = null },
                    onOpenDetails = { onOpenPropertyDetails(prop) },
                    onAction = {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${prop.phone}"))
                        context.startActivity(intent)
                    },
                    actionLabel = "اتصال بالمالك 📞",
                    onDirections = {
                        openExternalDirections(context, prop.latitude, prop.longitude, prop.title)
                    },
                    onWhatsapp = {
                        val url = "https://api.whatsapp.com/send?phone=967${prop.phone.removePrefix("0")}"
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    }
                )
            }

            // 7. CLOSEST SUGGESTIONS DIALOG PANEL
            if (showClosestSuggestions) {
                AlertDialog(
                    onDismissRequest = { showClosestSuggestions = false },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFF59E0B))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("✨ أقرب 3 خدمات متوفرة إليك", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    },
                    text = {
                        val allCombined = mutableListOf<MapItemSuggestion>()
                        filteredProviders.forEach { p ->
                            val dist = calculateDistanceInMeters(userLat, userLng, p.latitude, p.longitude)
                            allCombined.add(MapItemSuggestion(p.name, "🛠️ فني", dist, p.phone, p.rating.toDouble()))
                        }
                        filteredStores.forEach { s ->
                            val dist = calculateDistanceInMeters(userLat, userLng, s.latitude, s.longitude)
                            allCombined.add(MapItemSuggestion(s.name, "🏪 محل", dist, s.phone, s.rating.toDouble()))
                        }
                        filteredRestaurants.forEach { r ->
                            val dist = calculateDistanceInMeters(userLat, userLng, r.latitude, r.longitude)
                            allCombined.add(MapItemSuggestion(r.name, "🍔 مطعم", dist, r.phone, r.rating.toDouble()))
                        }

                        val top3 = allCombined.sortedBy { it.distanceMeters }.take(3)

                        Column(modifier = Modifier.fillMaxWidth()) {
                            if (top3.isEmpty()) {
                                Text("لا يوجد خدمات متاحة بالقرب من هذا النطاق حالياً.", fontSize = 12.sp, color = Color.Gray)
                            } else {
                                top3.forEach { item ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(item.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                                                Text("${item.type} • المسافة: ${formatDistance(item.distanceMeters)}", fontSize = 11.sp, color = Color(0xFFF59E0B))
                                            }
                                            IconButton(onClick = {
                                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${item.phone}"))
                                                context.startActivity(intent)
                                            }) {
                                                Icon(Icons.Default.Call, contentDescription = "اتصال", tint = Color(0xFF10B981))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { showClosestSuggestions = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B))
                        ) {
                            Text("إغلاق", color = Color.Black)
                        }
                    },
                    containerColor = Color(0xFF0F172A)
                )
            }
        }
    }
}

private data class MapItemSuggestion(
    val name: String,
    val type: String,
    val distanceMeters: Float,
    val phone: String,
    val rating: Double
)

@Composable
private fun FilterTypeChip(
    label: String,
    icon: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) Color(0xFFF59E0B) else Color(0xFF1E293B))
            .border(1.dp, if (selected) Color.White else Color(0xFF334155), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(icon, fontSize = 12.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) Color.Black else Color.White
            )
        }
    }
}

@Composable
private fun DetailedMapCardOverlay(
    title: String,
    categoryBadge: String,
    phone: String,
    rating: Double,
    numReviews: Int,
    distanceText: String,
    etaText: String,
    onDismiss: () -> Unit,
    onOpenDetails: () -> Unit,
    onAction: () -> Unit,
    actionLabel: String,
    onDirections: () -> Unit,
    onWhatsapp: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = BorderStroke(1.5.dp, Color(0xFFF59E0B)),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(categoryBadge, fontSize = 11.sp, color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Stats & ETA Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("${String.format("%.1f", rating)} ($numReviews)", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Box(modifier = Modifier.width(1.dp).height(16.dp).background(Color(0xFF334155)))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Place, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(distanceText, fontSize = 11.sp, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                    }

                    Box(modifier = Modifier.width(1.dp).height(16.dp).background(Color(0xFF334155)))

                    Text(etaText, fontSize = 11.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Action Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = onOpenDetails,
                        border = BorderStroke(1.dp, Color(0xFF334155)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("التفاصيل", fontSize = 11.sp, color = Color.White)
                    }

                    Button(
                        onClick = onDirections,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1.1f)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("اتجاهات", fontSize = 11.sp, color = Color.White)
                        }
                    }

                    Button(
                        onClick = onWhatsapp,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("واتساب 💬", fontSize = 11.sp, color = Color.White)
                    }

                    Button(
                        onClick = onAction,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Text(actionLabel, fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun LeafletRadarMapRenderer(
    userLat: Double,
    userLng: Double,
    zoom: Int,
    maxDistanceKm: Float,
    circleColorHex: String,
    providers: List<ProviderEntity>,
    stores: List<StoreEntity>,
    restaurants: List<StoreEntity>,
    properties: List<PropertyEntity>,
    onSelectProvider: (ProviderEntity) -> Unit,
    onSelectStore: (StoreEntity) -> Unit,
    onSelectProperty: (PropertyEntity) -> Unit
) {
    val htmlContent = remember(userLat, userLng, zoom, maxDistanceKm, circleColorHex, providers, stores, restaurants, properties) {
        buildLeafletHtml(userLat, userLng, zoom, maxDistanceKm, circleColorHex, providers, stores, restaurants, properties)
    }

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.cacheMode = WebSettings.LOAD_DEFAULT
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                        if (url != null) {
                            when {
                                url.startsWith("app://provider/") -> {
                                    val id = url.removePrefix("app://provider/")
                                    providers.find { it.id == id }?.let { onSelectProvider(it) }
                                    return true
                                }
                                url.startsWith("app://store/") -> {
                                    val id = url.removePrefix("app://store/")
                                    (stores + restaurants).find { it.id == id }?.let { onSelectStore(it) }
                                    return true
                                }
                                url.startsWith("app://property/") -> {
                                    val id = url.removePrefix("app://property/")
                                    properties.find { it.id == id }?.let { onSelectProperty(it) }
                                    return true
                                }
                            }
                        }
                        return false
                    }
                }
                loadDataWithBaseURL("https://openstreetmap.org", htmlContent, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL("https://openstreetmap.org", htmlContent, "text/html", "UTF-8", null)
        },
        modifier = Modifier.fillMaxSize()
    )
}

private fun buildLeafletHtml(
    userLat: Double,
    userLng: Double,
    zoom: Int,
    maxDistanceKm: Float,
    circleColorHex: String,
    providers: List<ProviderEntity>,
    stores: List<StoreEntity>,
    restaurants: List<StoreEntity>,
    properties: List<PropertyEntity>
): String {
    val centerLat = if (userLat != 0.0) userLat else 15.3694
    val centerLng = if (userLng != 0.0) userLng else 44.1910

    val tileLayerUrl = "https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png"

    val markersJs = StringBuilder()

    // Add User pulse radar icon
    markersJs.append("""
        var userIcon = L.divIcon({
            className: 'pulse-user-icon',
            html: '<div class="user-pulse-ring"></div><div class="user-core-dot">📍</div>',
            iconSize: [40, 40],
            iconAnchor: [20, 20]
        });
        L.marker([$centerLat, $centerLng], {icon: userIcon}).addTo(map)
            .bindPopup("<div style='text-align:center;'><b>📍 موقعك الحقيقي (اليمن)</b><br><span style='color:#38bdf8;font-size:11px;'>رادار الخدمات متصل ⚡</span></div>");

        L.circle([$centerLat, $centerLng], {
            color: '$circleColorHex',
            fillColor: '$circleColorHex',
            fillOpacity: 0.12,
            radius: ${maxDistanceKm * 1000}
        }).addTo(map);
    """.trimIndent())

    // 🛠️ Technicians
    providers.forEach { p ->
        val lat = if (p.latitude != 0.0) p.latitude else centerLat + (Math.random() - 0.5) * 0.04
        val lng = if (p.longitude != 0.0) p.longitude else centerLng + (Math.random() - 0.5) * 0.04
        markersJs.append("""
            var pIcon = L.divIcon({
                className: 'tech-marker-icon',
                html: '<div class="pulse-marker tech-pin">🛠️</div>',
                iconSize: [36, 36],
                iconAnchor: [18, 18]
            });
            L.marker([$lat, $lng], {icon: pIcon}).addTo(map)
                .bindPopup("<b>🛠️ ${p.name}</b><br><span style='color:#f59e0b;'>${p.customCategoryName.ifEmpty { "فني متخصص" }}</span><br><a href='app://provider/${p.id}' style='color:#38bdf8;font-weight:bold;'>عرض التفاصيل والطلب</a>");
        """.trimIndent())
    }

    // 🏪 Stores
    stores.forEach { s ->
        val lat = if (s.latitude != 0.0) s.latitude else centerLat + (Math.random() - 0.5) * 0.04
        val lng = if (s.longitude != 0.0) s.longitude else centerLng + (Math.random() - 0.5) * 0.04
        markersJs.append("""
            var sIcon = L.divIcon({
                className: 'store-marker-icon',
                html: '<div class="pulse-marker store-pin">🏪</div>',
                iconSize: [36, 36],
                iconAnchor: [18, 18]
            });
            L.marker([$lat, $lng], {icon: sIcon}).addTo(map)
                .bindPopup("<b>🏪 ${s.name}</b><br><span style='color:#10b981;'>${s.categoryId.ifEmpty { "متجر" }}</span><br><a href='app://store/${s.id}' style='color:#38bdf8;font-weight:bold;'>عرض التفاصيل والاتصال</a>");
        """.trimIndent())
    }

    // 🍔 Restaurants
    restaurants.forEach { r ->
        val lat = if (r.latitude != 0.0) r.latitude else centerLat + (Math.random() - 0.5) * 0.04
        val lng = if (r.longitude != 0.0) r.longitude else centerLng + (Math.random() - 0.5) * 0.04
        markersJs.append("""
            var rIcon = L.divIcon({
                className: 'rest-marker-icon',
                html: '<div class="pulse-marker rest-pin">🍔</div>',
                iconSize: [36, 36],
                iconAnchor: [18, 18]
            });
            L.marker([$lat, $lng], {icon: rIcon}).addTo(map)
                .bindPopup("<b>🍔 ${r.name}</b><br><span style='color:#ef4444;'>مطعم / كافيه</span><br><a href='app://store/${r.id}' style='color:#38bdf8;font-weight:bold;'>عرض القائمة والطلب</a>");
        """.trimIndent())
    }

    // 🏠 Properties
    properties.forEach { pr ->
        val lat = if (pr.latitude != 0.0) pr.latitude else centerLat + (Math.random() - 0.5) * 0.04
        val lng = if (pr.longitude != 0.0) pr.longitude else centerLng + (Math.random() - 0.5) * 0.04
        markersJs.append("""
            var prIcon = L.divIcon({
                className: 'prop-marker-icon',
                html: '<div class="pulse-marker prop-pin">🏠</div>',
                iconSize: [36, 36],
                iconAnchor: [18, 18]
            });
            L.marker([$lat, $lng], {icon: prIcon}).addTo(map)
                .bindPopup("<b>🏠 ${pr.title}</b><br><span style='color:#8b5cf6;'>${pr.price} YER</span><br><a href='app://property/${pr.id}' style='color:#38bdf8;font-weight:bold;'>التفاصيل المباشرة</a>");
        """.trimIndent())
    }

    return """
        <!DOCTYPE html>
        <html dir="rtl" lang="ar">
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
            <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
            <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
            <style>
                * { margin: 0; padding: 0; box-sizing: border-box; font-family: system-ui, -apple-system, sans-serif; }
                body, html { width: 100%; height: 100%; overflow: hidden; background: #0b0f19; }
                #map { width: 100vw; height: 100vh; background: #0b0f19; }

                .pulse-user-icon {
                    position: relative;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                }
                .user-pulse-ring {
                    position: absolute;
                    width: 50px;
                    height: 50px;
                    border-radius: 50%;
                    border: 2px solid #38bdf8;
                    background: rgba(56, 189, 248, 0.25);
                    animation: radarPulse 2s infinite ease-out;
                }
                .user-core-dot {
                    font-size: 24px;
                    z-index: 10;
                }

                .pulse-marker {
                    width: 36px;
                    height: 36px;
                    border-radius: 50%;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    font-size: 18px;
                    box-shadow: 0 0 12px rgba(0,0,0,0.6);
                    border: 2px solid #ffffff;
                    animation: floatMarker 3s ease-in-out infinite alternate;
                }
                .tech-pin { background: #f59e0b; }
                .store-pin { background: #10b981; }
                .rest-pin { background: #ef4444; }
                .prop-pin { background: #8b5cf6; }

                @keyframes radarPulse {
                    0% { transform: scale(0.3); opacity: 1; }
                    100% { transform: scale(2.2); opacity: 0; }
                }
                @keyframes floatMarker {
                    0% { transform: translateY(0); }
                    100% { transform: translateY(-5px); }
                }
                .leaflet-popup-content-wrapper {
                    background: #1e293b;
                    color: #f8fafc;
                    border-radius: 12px;
                    padding: 8px;
                    font-family: inherit;
                    box-shadow: 0 10px 25px rgba(0,0,0,0.5);
                }
                .leaflet-popup-tip { background: #1e293b; }
            </style>
        </head>
        <body>
            <div id="map"></div>
            <script>
                var map = L.map('map', { zoomControl: false }).setView([$centerLat, $centerLng], $zoom);
                L.tileLayer('$tileLayerUrl', {
                    maxZoom: 19,
                    attribution: 'دليل خدمات اليمن'
                }).addTo(map);
                $markersJs
            </script>
        </body>
        </html>
    """.trimIndent()
}

private fun calculateEtaText(distMeters: Float): String {
    val km = distMeters / 1000.0
    return if (km < 1.0) {
        val mins = (km / 5.0 * 60.0).roundToInt().coerceAtLeast(1)
        "⏱️ ~ $mins دقيقة سيراً"
    } else {
        val mins = (km / 40.0 * 60.0).roundToInt().coerceAtLeast(2)
        "🚘 ~ $mins دقيقة بالسيارة"
    }
}

private fun openExternalDirections(context: Context, destLat: Double, destLng: Double, label: String) {
    try {
        val gmapsUri = Uri.parse("google.navigation:q=$destLat,$destLng")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmapsUri).apply {
            setPackage("com.google.android.apps.maps")
        }
        if (mapIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(mapIntent)
        } else {
            val browserUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$destLat,$destLng")
            context.startActivity(Intent(Intent.ACTION_VIEW, browserUri))
        }
    } catch (e: Exception) {
        val fallbackUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$destLat,$destLng")
        context.startActivity(Intent(Intent.ACTION_VIEW, fallbackUri))
    }
}
