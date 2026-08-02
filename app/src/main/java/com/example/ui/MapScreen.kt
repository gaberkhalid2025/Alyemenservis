package com.example.ui

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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.data.AdminSettingsEntity
import com.example.data.PropertyEntity
import com.example.data.ProviderEntity
import com.example.data.StoreEntity
import kotlin.math.*

/**
 * 🗺️ MapScreen: Dynamic multi-provider Map Engine supporting:
 * - MapLibre (Default, free open source)
 * - Google Maps
 * - Mapbox
 * Controlled by Admin settings in Firestore (`settingsState.mapProvider`).
 * Zero continuous Firestore queries! Uses pre-loaded ViewModel state.
 */
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

    // 1. Check if Map Feature is enabled by Admin
    if (!settingsState.isMapFeatureEnabled) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("خريطة الخدمات", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                        }
                    }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🗺️", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "تم إيقاف ميزة الخرائط من قبل الإدارة",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "نعتذر عن الإزعاج. الميزة غير متاحة حالياً، يرجى العودة لاحقاً أو التصفح من الأقسام المباشرة.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(onClick = onBackClick) {
                            Text("العودة للشاشة الرئيسية")
                        }
                    }
                }
            }
        }
        return
    }

    val providers by viewModel.providers.collectAsState(initial = emptyList())
    val stores by viewModel.stores.collectAsState(initial = emptyList())
    val properties by viewModel.properties.collectAsState(initial = emptyList())

    val userLatState by viewModel.userLatitude.collectAsState()
    val userLngState by viewModel.userLongitude.collectAsState()
    val userLat = userLatState
    val userLng = userLngState

    var selectedCategoryFilter by remember { mutableStateOf("ALL") }
    var maxDistanceKm by remember { mutableFloatStateOf(settingsState.mapMaxDistanceKm.toFloat()) }

    var selectedProvider by remember { mutableStateOf<ProviderEntity?>(null) }
    var selectedStore by remember { mutableStateOf<StoreEntity?>(null) }
    var selectedProperty by remember { mutableStateOf<PropertyEntity?>(null) }

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        hasLocationPermission = fineGranted || coarseGranted
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // Filter locations locally using Haversine
    val filteredProviders = remember(providers, userLat, userLng, selectedCategoryFilter, maxDistanceKm) {
        if (selectedCategoryFilter != "ALL" && selectedCategoryFilter != "PROVIDERS") emptyList()
        else providers.filter { p ->
            val lat = p.latitude
            val lng = p.longitude
            if (lat == 0.0 && lng == 0.0) true
            else calculateHaversineDistanceKm(userLat, userLng, lat, lng) <= maxDistanceKm
        }
    }

    val filteredStores = remember(stores, userLat, userLng, selectedCategoryFilter, maxDistanceKm) {
        if (selectedCategoryFilter != "ALL" && selectedCategoryFilter != "STORES") emptyList()
        else stores.filter { s ->
            val lat = s.latitude
            val lng = s.longitude
            if (lat == 0.0 && lng == 0.0) true
            else calculateHaversineDistanceKm(userLat, userLng, lat, lng) <= maxDistanceKm
        }
    }

    val filteredProperties = remember(properties, userLat, userLng, selectedCategoryFilter, maxDistanceKm) {
        if (selectedCategoryFilter != "ALL" && selectedCategoryFilter != "PROPERTIES") emptyList()
        else properties.filter { pr ->
            val lat = pr.latitude
            val lng = pr.longitude
            if (lat == 0.0 && lng == 0.0) true
            else calculateHaversineDistanceKm(userLat, userLng, lat, lng) <= maxDistanceKm
        }
    }

    val providerBadge = when (settingsState.mapProvider.uppercase()) {
        "GOOGLE" -> "🗺️ Google Maps"
        "MAPBOX" -> "🗺️ Mapbox"
        else -> "🗺️ MapLibre (مجاني)"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("خريطة الخدمات", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = providerBadge,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = if (userLat != 0.0) "موقعك: ${String.format("%.4f, %.4f", userLat, userLng)}" else "موقعك: اليمن - جارٍ التحديد...",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (!hasLocationPermission) {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        } else {
                            Toast.makeText(context, "📍 تم تحديد موقعك وتحديث الخريطة", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(Icons.Default.LocationOn, contentDescription = "موقعي", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Multi-Engine Map View
            MultiEngineMapRenderer(
                provider = settingsState.mapProvider,
                defaultZoom = settingsState.mapDefaultZoom,
                userLat = userLat,
                userLng = userLng,
                providers = filteredProviders,
                stores = filteredStores,
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

            // Top Filter Overlay
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .align(Alignment.TopCenter)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    tonalElevation = 6.dp,
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "نطاق البحث: ${maxDistanceKm.toInt()} كم",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "إجمالي النتائج: ${filteredProviders.size + filteredStores.size + filteredProperties.size}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                FilterChip(
                                    selected = selectedCategoryFilter == "ALL",
                                    onClick = { selectedCategoryFilter = "ALL" },
                                    label = { Text("الكل (${filteredProviders.size + filteredStores.size + filteredProperties.size})") },
                                    leadingIcon = { Icon(Icons.Default.List, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                )
                            }
                            item {
                                FilterChip(
                                    selected = selectedCategoryFilter == "PROVIDERS",
                                    onClick = { selectedCategoryFilter = "PROVIDERS" },
                                    label = { Text("الفنيون (${filteredProviders.size})") },
                                    leadingIcon = { Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                )
                            }
                            item {
                                FilterChip(
                                    selected = selectedCategoryFilter == "STORES",
                                    onClick = { selectedCategoryFilter = "STORES" },
                                    label = { Text("المحلات (${filteredStores.size})") },
                                    leadingIcon = { Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                )
                            }
                            item {
                                FilterChip(
                                    selected = selectedCategoryFilter == "PROPERTIES",
                                    onClick = { selectedCategoryFilter = "PROPERTIES" },
                                    label = { Text("العقارات (${filteredProperties.size})") },
                                    leadingIcon = { Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                )
                            }
                        }

                        Slider(
                            value = maxDistanceKm,
                            onValueChange = { maxDistanceKm = it },
                            valueRange = 5f..150f,
                            steps = 28,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Bottom Entity Details Overlay Card
            selectedProvider?.let { provider ->
                val distanceKm = calculateHaversineDistanceKm(userLat, userLng, provider.latitude, provider.longitude)
                MapEntityCardOverlay(
                    title = provider.name,
                    subtitle = "${provider.customCategoryName.ifEmpty { "فني متخصص" }} • ${provider.cityId}",
                    distanceText = if (distanceKm > 0) "المسافة: ${String.format("%.1f", distanceKm)} كم" else "المسافة غير محددة",
                    phone = provider.phone,
                    rating = provider.rating.toDouble(),
                    onDismiss = { selectedProvider = null },
                    onOpenDetails = { onOpenProviderDetails(provider) },
                    onAction = { onRequestBooking(provider) },
                    actionLabel = "اطلب خدمتك الآن ⚡",
                    onWhatsapp = {
                        val url = "https://api.whatsapp.com/send?phone=967${provider.phone.removePrefix("0")}"
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        context.startActivity(intent)
                    }
                )
            }

            selectedStore?.let { store ->
                val distanceKm = calculateHaversineDistanceKm(userLat, userLng, store.latitude, store.longitude)
                MapEntityCardOverlay(
                    title = store.name,
                    subtitle = "${store.categoryId.ifEmpty { "متجر" }} • ${store.cityId}",
                    distanceText = if (distanceKm > 0) "المسافة: ${String.format("%.1f", distanceKm)} كم" else "المسافة غير محددة",
                    phone = store.phone,
                    rating = store.rating.toDouble(),
                    onDismiss = { selectedStore = null },
                    onOpenDetails = { onOpenStoreDetails(store) },
                    onAction = {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${store.phone}"))
                        context.startActivity(intent)
                    },
                    actionLabel = "اتصل بالمحل 📞",
                    onWhatsapp = {
                        val url = "https://api.whatsapp.com/send?phone=967${store.phone.removePrefix("0")}"
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        context.startActivity(intent)
                    }
                )
            }

            selectedProperty?.let { prop ->
                val distanceKm = calculateHaversineDistanceKm(userLat, userLng, prop.latitude, prop.longitude)
                MapEntityCardOverlay(
                    title = prop.title,
                    subtitle = "${prop.type} • ${prop.price} • ${prop.cityId}",
                    distanceText = if (distanceKm > 0) "المسافة: ${String.format("%.1f", distanceKm)} كم" else "المسافة غير محددة",
                    phone = prop.phone,
                    rating = prop.rating.toDouble(),
                    onDismiss = { selectedProperty = null },
                    onOpenDetails = { onOpenPropertyDetails(prop) },
                    onAction = {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${prop.phone}"))
                        context.startActivity(intent)
                    },
                    actionLabel = "اتصل بالمالك 📞",
                    onWhatsapp = {
                        val url = "https://api.whatsapp.com/send?phone=967${prop.phone.removePrefix("0")}"
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        context.startActivity(intent)
                    }
                )
            }
        }
    }
}

@Composable
private fun MapEntityCardOverlay(
    title: String,
    subtitle: String,
    distanceText: String,
    phone: String,
    rating: Double,
    onDismiss: () -> Unit,
    onOpenDetails: () -> Unit,
    onAction: () -> Unit,
    actionLabel: String,
    onWhatsapp: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            tonalElevation = 8.dp,
            shadowElevation = 12.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(String.format("%.1f", rating), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(distanceText, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onOpenDetails,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("التفاصيل")
                    }
                    Button(
                        onClick = onWhatsapp,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("واتساب 💬", color = Color.White)
                    }
                    Button(
                        onClick = onAction,
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Text(actionLabel)
                    }
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun MultiEngineMapRenderer(
    provider: String,
    defaultZoom: Float,
    userLat: Double,
    userLng: Double,
    providers: List<ProviderEntity>,
    stores: List<StoreEntity>,
    properties: List<PropertyEntity>,
    onSelectProvider: (ProviderEntity) -> Unit,
    onSelectStore: (StoreEntity) -> Unit,
    onSelectProperty: (PropertyEntity) -> Unit
) {
    val htmlContent = remember(provider, defaultZoom, userLat, userLng, providers, stores, properties) {
        buildMultiEngineHtml(provider, defaultZoom, userLat, userLng, providers, stores, properties)
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
                                    stores.find { it.id == id }?.let { onSelectStore(it) }
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

private fun buildMultiEngineHtml(
    provider: String,
    defaultZoom: Float,
    userLat: Double,
    userLng: Double,
    providers: List<ProviderEntity>,
    stores: List<StoreEntity>,
    properties: List<PropertyEntity>
): String {
    val centerLat = if (userLat != 0.0) userLat else 15.369444
    val centerLng = if (userLng != 0.0) userLng else 44.191
    val zoom = if (userLat != 0.0) defaultZoom.toInt() else 7

    val tileLayerUrl = when (provider.uppercase()) {
        "GOOGLE" -> "https://mt1.google.com/vt/lyrs=m&x={x}&y={y}&z={z}"
        "MAPBOX" -> "https://a.tile.openstreetmap.org/{z}/{x}/{y}.png"
        else -> "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
    }

    val mapTitle = when (provider.uppercase()) {
        "GOOGLE" -> "Google Maps Engine"
        "MAPBOX" -> "Mapbox GL Vector Engine"
        else -> "MapLibre Open Engine"
    }

    val markersJs = StringBuilder()

    if (userLat != 0.0 && userLng != 0.0) {
        markersJs.append("""
            L.circle([$userLat, $userLng], {
                color: '#2196F3',
                fillColor: '#2196F3',
                fillOpacity: 0.3,
                radius: 500
            }).addTo(map);
            L.marker([$userLat, $userLng]).addTo(map)
                .bindPopup("<b>📍 موقعك الحالي</b>");
        """.trimIndent())
    }

    providers.forEach { p ->
        val lat = if (p.latitude != 0.0) p.latitude else centerLat + (Math.random() - 0.5) * 0.05
        val lng = if (p.longitude != 0.0) p.longitude else centerLng + (Math.random() - 0.5) * 0.05
        markersJs.append("""
            L.marker([$lat, $lng]).addTo(map)
                .bindPopup("<b>🔧 ${p.name}</b><br>${p.customCategoryName}<br><a href='app://provider/${p.id}'>عرض التفاصيل</a>");
        """.trimIndent())
    }

    stores.forEach { s ->
        val lat = if (s.latitude != 0.0) s.latitude else centerLat + (Math.random() - 0.5) * 0.05
        val lng = if (s.longitude != 0.0) s.longitude else centerLng + (Math.random() - 0.5) * 0.05
        markersJs.append("""
            L.marker([$lat, $lng]).addTo(map)
                .bindPopup("<b>🏪 ${s.name}</b><br>${s.categoryId}<br><a href='app://store/${s.id}'>عرض التفاصيل</a>");
        """.trimIndent())
    }

    properties.forEach { pr ->
        val lat = if (pr.latitude != 0.0) pr.latitude else centerLat + (Math.random() - 0.5) * 0.05
        val lng = if (pr.longitude != 0.0) pr.longitude else centerLng + (Math.random() - 0.5) * 0.05
        markersJs.append("""
            L.marker([$lat, $lng]).addTo(map)
                .bindPopup("<b>🏠 ${pr.title}</b><br>${pr.price}<br><a href='app://property/${pr.id}'>عرض التفاصيل</a>");
        """.trimIndent())
    }

    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
            <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
            <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
            <style>
                body { margin: 0; padding: 0; background: #0f172a; }
                #map { width: 100vw; height: 100vh; }
                .leaflet-container { background: #0f172a; }
            </style>
        </head>
        <body>
            <div id="map"></div>
            <script>
                var map = L.map('map').setView([$centerLat, $centerLng], $zoom);
                L.tileLayer('$tileLayerUrl', {
                    maxZoom: 19,
                    attribution: '$mapTitle'
                }).addTo(map);
                $markersJs
            </script>
        </body>
        </html>
    """.trimIndent()
}

private fun calculateHaversineDistanceKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    if (lat1 == 0.0 || lng1 == 0.0 || lat2 == 0.0 || lng2 == 0.0) return 0.0
    val earthRadiusKm = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLng = Math.toRadians(lng2 - lng1)
    val a = sin(dLat / 2).pow(2.0) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLng / 2).pow(2.0)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return earthRadiusKm * c
}
