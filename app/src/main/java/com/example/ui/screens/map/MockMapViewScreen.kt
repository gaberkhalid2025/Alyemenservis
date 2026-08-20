@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.ui.screens.map

import com.example.ui.*
import com.example.ui.utils.*


import android.content.Intent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.PathEffect
import okhttp3.MediaType.Companion.toMediaType

import com.example.*

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.*
import com.example.utils.*
import com.example.ui.MainViewModel
import com.example.ui.components.*
import com.example.ui.dialogs.*
import com.example.ui.screens.home.*
import com.example.ui.screens.map.*
import com.example.ui.screens.bookings.*
import com.example.ui.screens.admin.*
import com.example.ui.screens.assistant.*
import com.example.ui.screens.register.*
import com.example.ui.screens.status.*
import com.example.ui.screens.about.*
import com.example.viewmodels.*
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


@Composable
fun MockMapViewScreen(viewModel: MainViewModel, themeColors: VisualThemePalette, onRequestLocationPermission: () -> Unit) {
    val density = LocalDensity.current.density
    val providers by viewModel.providers.collectAsState()
    val stores by viewModel.stores.collectAsState()
    val properties by viewModel.properties.collectAsState()
    val citiesList by viewModel.cities.collectAsState()
    val settingsState by viewModel.settings.collectAsState()
    val radiusKm by viewModel.maxKmRadius.collectAsState()
    val currentUserName by viewModel.currentUserName.collectAsState()
    val currentUserPhone by viewModel.currentUserPhone.collectAsState()
    val categories by viewModel.categories.collectAsState()

    val context = LocalContext.current
    val isOnline = com.example.NetworkUtils.isNetworkAvailable(context)

    // Interactive State Managers
    var selectedUserCityId by remember { mutableStateOf("ye_san") }
    var selectedProviderForMap by remember { mutableStateOf<com.example.data.ProviderEntity?>(null) }
    var selectedStoreForMap by remember { mutableStateOf<com.example.data.StoreEntity?>(null) }
    var selectedPropertyForMap by remember { mutableStateOf<com.example.data.PropertyEntity?>(null) }
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    var selectedAvailability by remember { mutableStateOf("ALL") } // ALL, AVAILABLE, BUSY
    var selectedMinRating by remember { mutableStateOf(0.0f) } // 0f, 4.0f, 4.5f
    var heatmapEnabled by remember { mutableStateOf(false) }
    var activeMapType by remember { mutableStateOf("OSM") } // "OSM" or "RADAR"
    
    // Transform / Interactive Gestures
    var mapOffset by remember { mutableStateOf(Offset.Zero) }
    var mapZoom by remember { mutableStateOf(1.2f) }

    // Real-time tracking offsets (5s updates)
    var trackingCounter by remember { mutableStateOf(0) }
    val dynamicOffsets = remember { mutableStateMapOf<String, Pair<Double, Double>>() }

    // Virtual appointment reservation target on map
    var bookingProviderTargetOnMap by remember { mutableStateOf<com.example.data.ProviderEntity?>(null) }

    val userCoords = getCityCenterCoords(selectedUserCityId)

    // Real-time Tracking Trigger (every 5 seconds)
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(5000L)
            trackingCounter++
            // Add tiny random walk delta to simulate technician movement in streets
            providers.forEach { p ->
                val randomLat = (Math.random() - 0.5) * 0.0006
                val randomLng = (Math.random() - 0.5) * 0.0006
                val current = dynamicOffsets[p.id] ?: Pair(0.0, 0.0)
                dynamicOffsets[p.id] = Pair(current.first + randomLat, current.second + randomLng)
            }
        }
    }

    // Side effect to update position in viewmodel
    LaunchedEffect(selectedUserCityId) {
        viewModel.updateUserLocation(userCoords.first, userCoords.second)
        viewModel.setCityFilter(selectedUserCityId)
        // Reset offset on city switch so center user is always visible
        mapOffset = Offset.Zero
        mapZoom = 1.2f
    }

    var isFiltersExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)) // Modern slate background
    ) {
        // --- OFFLINE/PERMISSIONS BANNER ---
        if (!isOnline) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF854D0E)) // Warm Amber
                    .padding(vertical = 4.dp, horizontal = 12.dp)
            ) {
                Text(
                    text = "⚠️ يعمل رادار الخرائط الآن في الوضع المحلي الذكي (بدون اتصال بالشبكة).",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        // --- FILTER BAR CONTROLLER ---
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                // Main Header Row: City Dropdown, Map Mode, & Filter Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // City Dropdown
                    Box(modifier = Modifier.weight(1f)) {
                        var dropdownExpanded by remember { mutableStateOf(false) }
                        val activeLabel = citiesList.find { it.id == selectedUserCityId }?.nameAr ?: "صنعاء 🇾🇪"
                        
                        Button(
                            onClick = { dropdownExpanded = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().height(32.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(activeLabel, color = Color.White, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(14.dp))
                            }
                        }
                        
                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false },
                            modifier = Modifier.background(Color(0xFF1E293B))
                        ) {
                            citiesList.forEach { city ->
                                DropdownMenuItem(
                                    text = { Text(city.nameAr, color = Color.White, fontSize = 11.sp) },
                                    onClick = {
                                        selectedUserCityId = city.id
                                        selectedProviderForMap = null
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Map Mode Pill Switch (OSM vs RADAR)
                    Row(
                        modifier = Modifier
                            .weight(1.3f)
                            .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                            .padding(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (activeMapType == "OSM") themeColors.primary else Color.Transparent)
                                .clickable { activeMapType = "OSM" }
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🗺️ حقيقية OSM", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (activeMapType == "RADAR") themeColors.primary else Color.Transparent)
                                .clickable { activeMapType = "RADAR" }
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("📡 رادار", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Filter Toggle Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isFiltersExpanded) Color(0xFFFFD700) else Color(0xFF334155))
                            .clickable { isFiltersExpanded = !isFiltersExpanded }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                            .height(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isFiltersExpanded) "🔍 إخفاء الفلاتر 🔼" else "🔍 خيارات 🔽",
                            color = if (isFiltersExpanded) Color.Black else Color.White,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Expanded Filters Body
                if (isFiltersExpanded) {
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Proximity Distance Range Slider
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "نطاق الرادار: ${radiusKm} كم",
                                color = Color.LightGray,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Slider(
                                value = radiusKm.toFloat(),
                                onValueChange = { viewModel.setRadiusKm(it.toInt()) },
                                valueRange = 2f..50f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFFFFD700),
                                    activeTrackColor = themeColors.primary,
                                    inactiveTrackColor = Color(0xFF334155)
                                ),
                                modifier = Modifier.height(18.dp)
                            )
                        }

                        // Heatmap Toggle Button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (heatmapEnabled) Color(0xFFEF4444) else Color(0xFF334155))
                                .clickable { heatmapEnabled = !heatmapEnabled }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                .height(28.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (heatmapEnabled) "🔥 حراري نشط" else "🔥 خريطة الطلب",
                                color = Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Row 2: Specialty / Category Filter List
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            CustomFilterChip(
                                label = "كل الأقسام 🌟",
                                selected = selectedCategoryId == null,
                                onClick = { selectedCategoryId = null }
                            )
                        }
                        val listToShow = if (categories.isEmpty()) {
                            listOf(
                                com.example.data.CategoryEntity("ye_sub_spaka", "سباكة 🔧", "", 1),
                                com.example.data.CategoryEntity("ye_sub_kahraba", "كهرباء ⚡", "", 2),
                                com.example.data.CategoryEntity("ye_sub_dehan", "دهان وصباغة 🎨", "", 3),
                                com.example.data.CategoryEntity("ye_sub_hadada", "حدادة ألمنيوم 🔨", "", 4)
                            )
                        } else categories

                        items(listToShow) { cat ->
                            CustomFilterChip(
                                label = cat.name,
                                selected = selectedCategoryId == cat.id,
                                onClick = { selectedCategoryId = if (selectedCategoryId == cat.id) null else cat.id }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Row 3: Availability & Rating filters row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("التوفر:", color = Color.Gray, fontSize = 9.sp)
                        listOf("ALL" to "الكل", "AVAILABLE" to "متاح 🟢", "BUSY" to "مشغول 🟡").forEach { (id, name) ->
                            val isSel = selectedAvailability == id
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) themeColors.primary else Color(0xFF0F172A))
                                    .clickable { selectedAvailability = id }
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Text(name, color = Color.White, fontSize = 8.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal)
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        Text("التقييم:", color = Color.Gray, fontSize = 9.sp)
                        listOf(0.0f to "الكل", 4.0f to "4.0+ ⭐", 4.5f to "4.5+ ⭐").forEach { (valRate, name) ->
                            val isSel = selectedMinRating == valRate
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) Color(0xFFFFD700) else Color(0xFF0F172A))
                                    .clickable { selectedMinRating = valRate }
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Text(name, color = if (isSel) Color.Black else Color.White, fontSize = 8.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                }
            }
        }

        // Apply coordinates filter to get nearby providers
        val nearbyProviders = remember(providers, userCoords, radiusKm, selectedUserCityId, selectedCategoryId, selectedAvailability, selectedMinRating, dynamicOffsets) {
            providers.filter { p ->
                val baseCoords = getProviderCoords(p)
                val walkOffset = dynamicOffsets[p.id] ?: Pair(0.0, 0.0)
                val liveLat = baseCoords.first + walkOffset.first
                val liveLng = baseCoords.second + walkOffset.second

                val distMeters = calculateDistanceInMeters(userCoords.first, userCoords.second, liveLat, liveLng)
                val distKm = (distMeters / 1000f).toDouble()
                
                val matchesCategory = selectedCategoryId == null || p.categoryId == selectedCategoryId
                val matchesAvailability = when (selectedAvailability) {
                    "AVAILABLE" -> p.isAvailable
                    "BUSY" -> !p.isAvailable
                    else -> true
                }
                val matchesRating = p.rating >= selectedMinRating.toDouble()
                
                distKm <= radiusKm.toDouble() && p.cityId == selectedUserCityId && (p.isVip || p.subscriptionStatus == "APPROVED") && matchesCategory && matchesAvailability && matchesRating && !p.isBlocked
            }
        }

        val nearbyStores = remember(stores, userCoords, radiusKm, selectedUserCityId, selectedCategoryId) {
            stores.filter { s ->
                val distMeters = calculateDistanceInMeters(userCoords.first, userCoords.second, s.latitude, s.longitude)
                val distKm = distMeters / 1000.0
                val matchesCategory = selectedCategoryId == null || s.categoryId == selectedCategoryId
                distKm <= radiusKm.toDouble() && s.cityId == selectedUserCityId && matchesCategory && !s.isDeleted
            }
        }

        val nearbyProperties = remember(properties, userCoords, radiusKm, selectedUserCityId) {
            properties.filter { pr ->
                val distMeters = calculateDistanceInMeters(userCoords.first, userCoords.second, pr.latitude, pr.longitude)
                val distKm = distMeters / 1000.0
                distKm <= radiusKm.toDouble() && pr.cityId == selectedUserCityId && !pr.isDeleted
            }
        }

        // --- MAP CONTAINER OVERLAY ---
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clipToBounds()
                .background(Color(0xFF0B0F19)) // Space dark color
        ) {
            if (activeMapType == "OSM") {
                RealLeafletMapView(
                    userCoords = userCoords,
                    nearbyProviders = nearbyProviders,
                    nearbyStores = nearbyStores,
                    nearbyProperties = nearbyProperties,
                    dynamicOffsets = dynamicOffsets,
                    onProviderSelected = { 
                        selectedProviderForMap = it
                        selectedStoreForMap = null
                        selectedPropertyForMap = null
                    },
                    onStoreSelected = {
                        selectedStoreForMap = it
                        selectedProviderForMap = null
                        selectedPropertyForMap = null
                    },
                    onPropertySelected = {
                        selectedPropertyForMap = it
                        selectedProviderForMap = null
                        selectedStoreForMap = null
                    }
                )
            } else {
                // Radar Vector Canvas Mode
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                mapZoom = (mapZoom * zoom).coerceIn(0.5f, 3.5f)
                                mapOffset += pan
                            }
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        
                        // Draw decorative streets and city blocks
                        val streetColor = Color(0xFF1E293B)
                        val avenueColor = Color(0xFF334155).copy(alpha = 0.5f)
                        val blockColor = Color(0xFF151D2A)
                        
                        // Draw standard grid blocks
                        for (x in -5..5) {
                            for (y in -5..5) {
                                val bx = cx + (x * 140f * mapZoom) + mapOffset.x
                                val by = cy + (y * 140f * mapZoom) + mapOffset.y
                                drawRoundRect(
                                    color = blockColor,
                                    topLeft = Offset(bx - 50f * mapZoom, by - 50f * mapZoom),
                                    size = Size(100f * mapZoom, 100f * mapZoom),
                                    cornerRadius = CornerRadius(8f * mapZoom, 8f * mapZoom)
                                )
                            }
                        }

                        // Draw secondary streets
                        for (i in -4..4) {
                            val lineX = cx + (i * 140f * mapZoom) + mapOffset.x
                            drawLine(color = streetColor, start = Offset(lineX, 0f), end = Offset(lineX, size.height), strokeWidth = 3f * mapZoom)
                            
                            val lineY = cy + (i * 140f * mapZoom) + mapOffset.y
                            drawLine(color = streetColor, start = Offset(0f, lineY), end = Offset(size.width, lineY), strokeWidth = 3f * mapZoom)
                        }

                        // Draw main avenues (Highways) crossing Yemen cities
                        drawLine(color = avenueColor, start = Offset(cx + mapOffset.x, 0f), end = Offset(cx + mapOffset.x, size.height), strokeWidth = 10f * mapZoom)
                        drawLine(color = avenueColor, start = Offset(0f, cy + mapOffset.y), end = Offset(size.width, cy + mapOffset.y), strokeWidth = 10f * mapZoom)

                        // Draw green parks
                        drawRoundRect(
                            color = Color(0xFF065F46).copy(alpha = 0.15f),
                            topLeft = Offset(cx - 220f * mapZoom + mapOffset.x, cy + 120f * mapZoom + mapOffset.y),
                            size = Size(150f * mapZoom, 100f * mapZoom),
                            cornerRadius = CornerRadius(12f, 12f)
                        )

                        // Draw radar circle grids (pulse guide rings)
                        for (r in listOf(0.18f, 0.36f, 0.55f, 0.72f, 0.9f)) {
                            drawCircle(
                                color = Color(0xFF1E293B).copy(alpha = 0.6f),
                                radius = size.width * r * mapZoom,
                                center = Offset(cx + mapOffset.x, cy + mapOffset.y),
                                style = Stroke(width = 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
                            )
                        }

                        // --- HEATMAP OVERLAY ---
                        if (heatmapEnabled) {
                            nearbyProviders.forEach { p ->
                                val baseCoords = getProviderCoords(p)
                                val walkOffset = dynamicOffsets[p.id] ?: Pair(0.0, 0.0)
                                val liveLat = baseCoords.first + walkOffset.first
                                val liveLng = baseCoords.second + walkOffset.second

                                val scaleFactorRange = (radiusKm.toDouble() / 111.0).coerceAtLeast(0.01)
                                val relX = ((liveLng - userCoords.second) / scaleFactorRange).toFloat()
                                val relY = ((liveLat - userCoords.first) / scaleFactorRange).toFloat()
                                
                                val posX = cx + (cx * relX * 0.85f * mapZoom) + mapOffset.x
                                val posY = cy - (cy * relY * 0.85f * mapZoom) + mapOffset.y

                                // Draw radial demand density glow
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(Color(0xFFEF4444).copy(alpha = 0.35f), Color(0xFFFF9F0A).copy(alpha = 0.15f), Color.Transparent),
                                        center = Offset(posX, posY),
                                        radius = 70f * mapZoom
                                    ),
                                    radius = 70f * mapZoom,
                                    center = Offset(posX, posY)
                                )
                            }
                        }

                        // --- DRAW ROUTING PATH & NAV SYMBOL ---
                        selectedProviderForMap?.let { p ->
                            val baseCoords = getProviderCoords(p)
                            val walkOffset = dynamicOffsets[p.id] ?: Pair(0.0, 0.0)
                            val liveLat = baseCoords.first + walkOffset.first
                            val liveLng = baseCoords.second + walkOffset.second

                            val scaleFactorRange = (radiusKm.toDouble() / 111.0).coerceAtLeast(0.01)
                            val relX = ((liveLng - userCoords.second) / scaleFactorRange).toFloat()
                            val relY = ((liveLat - userCoords.first) / scaleFactorRange).toFloat()
                            
                            val posX = cx + (cx * relX * 0.85f * mapZoom) + mapOffset.x
                            val posY = cy - (cy * relY * 0.85f * mapZoom) + mapOffset.y

                            // Draw glowing dashed line between user and provider
                            drawLine(
                                color = Color(0xFF00E5FF),
                                start = Offset(cx + mapOffset.x, cy + mapOffset.y),
                                end = Offset(posX, posY),
                                strokeWidth = 3f * mapZoom,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), trackingCounter * 10f)
                            )

                            // Draw pulsing target navigation pointer
                            drawCircle(
                                color = Color(0xFF00E5FF).copy(alpha = 0.2f),
                                radius = 20f * mapZoom,
                                center = Offset(posX, posY)
                            )
                        }
                    }

                    // --- CONCENTRIC PULSING BLUE DOT (USER CENTER LOCATION) ---
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val cxDp = maxWidth / 2f - 20.dp + (mapOffset.x / density).dp
                        val cyDp = maxHeight / 2f - 24.dp + (mapOffset.y / density).dp
                        
                        Column(
                            modifier = Modifier.absoluteOffset(x = cxDp, y = cyDp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(Color(0x330088FF), CircleShape)
                                    .border(2.dp, Color(0xFF0088FF), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(Color(0xFF00E5FF), CircleShape)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF0088FF).copy(alpha = 0.85f))
                                    .testTag("user_dot")
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text("موقعك الحالي", color = Color.White, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // --- RENDER DYNAMIC MARKERS / SMART CLUSTERING ---
                    val isClusteredMode = mapZoom < 0.8f
                    
                    if (isClusteredMode) {
                        // Draw a simple aggregated cluster marker
                        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                            val cxDp = maxWidth / 2f - 25.dp + (mapOffset.x / density).dp
                            val cyDp = maxHeight / 2f - 40.dp + (mapOffset.y / density).dp
                            
                            if (nearbyProviders.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .absoluteOffset(x = cxDp, y = cyDp)
                                        .size(48.dp)
                                        .background(Color(0xFF7C3AED).copy(alpha = 0.35f), CircleShape)
                                        .border(2.dp, Color(0xFF8B5CF6), CircleShape)
                                        .clickable { mapZoom = 1.6f },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("+${nearbyProviders.size}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        // Plot individual custom markers
                        nearbyProviders.forEachIndexed { idx, provider ->
                            val baseCoords = getProviderCoords(provider)
                            val walkOffset = dynamicOffsets[provider.id] ?: Pair(0.0, 0.0)
                            val liveLat = baseCoords.first + walkOffset.first
                            val liveLng = baseCoords.second + walkOffset.second

                            val scaleFactorRange = (radiusKm.toDouble() / 111.0).coerceAtLeast(0.01)
                            val relX = ((liveLng - userCoords.second) / scaleFactorRange).toFloat()
                            val relY = ((liveLat - userCoords.first) / scaleFactorRange).toFloat()

                            val distanceMeters = calculateDistanceInMeters(userCoords.first, userCoords.second, liveLat, liveLng)
                            val distKm = distanceMeters / 1000.0

                            val categoryEmoji = when {
                                provider.categoryId.contains("spaka") -> "🔧"
                                provider.categoryId.contains("kahraba") -> "⚡"
                                provider.categoryId.contains("dehan") -> "🎨"
                                provider.categoryId.contains("hadada") -> "🔨"
                                else -> "👷"
                            }

                            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                                val posX = (maxWidth / 2f) + ((maxWidth / 2f) * relX * 0.85f * mapZoom) - 16.dp + (mapOffset.x / density).dp
                                val posY = (maxHeight / 2f) - ((maxHeight / 2f) * relY * 0.85f * mapZoom) - 24.dp + (mapOffset.y / density).dp

                                val availabilityColor = if (provider.isAvailable) Color(0xFF10B981) else Color(0xFFEF4444)

                                Column(
                                    modifier = Modifier
                                        .absoluteOffset(x = posX, y = posY)
                                        .clickable { 
                                            selectedProviderForMap = provider 
                                            selectedStoreForMap = null
                                            selectedPropertyForMap = null
                                        }
                                        .testTag("geo_marker_$idx"),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // Custom Marker with Availability Halo & Specialty Symbol
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(Color.Black.copy(alpha = 0.85f), CircleShape)
                                            .border(2.dp, availabilityColor, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(categoryEmoji, fontSize = 13.sp)
                                    }
                                    
                                    // Glowing indicator
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(availabilityColor, CircleShape)
                                    )

                                    // Title details label
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFF1E293B).copy(alpha = 0.95f))
                                            .border(0.5.dp, Color(0xFF334155), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 3.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "${provider.name.split(" ").lastOrNull() ?: provider.name} (${String.format(java.util.Locale.US, "%.1f", distKm)} كم)",
                                            color = Color.White,
                                            fontSize = 7.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        // Plot individual custom stores
                        nearbyStores.forEachIndexed { idx, store ->
                            val scaleFactorRange = (radiusKm.toDouble() / 111.0).coerceAtLeast(0.01)
                            val relX = ((store.longitude - userCoords.second) / scaleFactorRange).toFloat()
                            val relY = ((store.latitude - userCoords.first) / scaleFactorRange).toFloat()

                            val distanceMeters = calculateDistanceInMeters(userCoords.first, userCoords.second, store.latitude, store.longitude)
                            val distKm = distanceMeters / 1000.0

                            val emoji = when {
                                store.sectionId.contains("medical") || store.categoryId.contains("medical") || store.name.contains("طبي") || store.name.contains("مستشفى") || store.name.contains("عيادة") -> "🏥"
                                store.sectionId.contains("restaurant") || store.categoryId.contains("restaurant") || store.name.contains("مطعم") || store.name.contains("مأكولات") -> "🍔"
                                else -> "🏪"
                            }

                            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                                val posX = (maxWidth / 2f) + ((maxWidth / 2f) * relX * 0.85f * mapZoom) - 16.dp + (mapOffset.x / density).dp
                                val posY = (maxHeight / 2f) - ((maxHeight / 2f) * relY * 0.85f * mapZoom) - 24.dp + (mapOffset.y / density).dp

                                val statusColor = if (store.isActive) Color(0xFF10B981) else Color.Red

                                Column(
                                    modifier = Modifier
                                        .absoluteOffset(x = posX, y = posY)
                                        .clickable { 
                                            selectedStoreForMap = store
                                            selectedProviderForMap = null
                                            selectedPropertyForMap = null
                                        },
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(Color.Black.copy(alpha = 0.85f), CircleShape)
                                            .border(2.dp, statusColor, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(emoji, fontSize = 13.sp)
                                    }
                                    Box(modifier = Modifier.size(6.dp).background(statusColor, CircleShape))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFF1E293B).copy(alpha = 0.95f))
                                            .border(0.5.dp, Color(0xFF334155), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 3.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "${store.name.split(" ").lastOrNull() ?: store.name} (${String.format(java.util.Locale.US, "%.1f", distKm)} كم)",
                                            color = Color.White,
                                            fontSize = 7.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        // Plot individual custom properties
                        nearbyProperties.forEachIndexed { idx, prop ->
                            val scaleFactorRange = (radiusKm.toDouble() / 111.0).coerceAtLeast(0.01)
                            val relX = ((prop.longitude - userCoords.second) / scaleFactorRange).toFloat()
                            val relY = ((prop.latitude - userCoords.first) / scaleFactorRange).toFloat()

                            val distanceMeters = calculateDistanceInMeters(userCoords.first, userCoords.second, prop.latitude, prop.longitude)
                            val distKm = distanceMeters / 1000.0

                            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                                val posX = (maxWidth / 2f) + ((maxWidth / 2f) * relX * 0.85f * mapZoom) - 16.dp + (mapOffset.x / density).dp
                                val posY = (maxHeight / 2f) - ((maxHeight / 2f) * relY * 0.85f * mapZoom) - 24.dp + (mapOffset.y / density).dp

                                val statusColor = Color(0xFF10B981)

                                Column(
                                    modifier = Modifier
                                        .absoluteOffset(x = posX, y = posY)
                                        .clickable { 
                                            selectedPropertyForMap = prop
                                            selectedProviderForMap = null
                                            selectedStoreForMap = null
                                        },
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(Color.Black.copy(alpha = 0.85f), CircleShape)
                                            .border(2.dp, statusColor, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("🏠", fontSize = 13.sp)
                                    }
                                    Box(modifier = Modifier.size(6.dp).background(statusColor, CircleShape))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFF1E293B).copy(alpha = 0.95f))
                                            .border(0.5.dp, Color(0xFF334155), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 3.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "${prop.title.split(" ").lastOrNull() ?: prop.title} (${String.format(java.util.Locale.US, "%.1f", distKm)} كم)",
                                            color = Color.White,
                                            fontSize = 7.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // --- FLOATING ZOOM CONTROL DIAL & RADAR CONTROLS ---
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // GPS Center Location Button
                        FloatingActionButton(
                            onClick = { onRequestLocationPermission() },
                            containerColor = themeColors.accent,
                            contentColor = Color.Black,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Text("📍", fontSize = 16.sp)
                        }

                        // Zoom In
                        FloatingActionButton(
                            onClick = { mapZoom = (mapZoom + 0.3f).coerceAtLeast(0.5f) },
                            containerColor = Color(0xFF1E293B),
                            contentColor = Color.White,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }

                        // Zoom Out
                        FloatingActionButton(
                            onClick = { mapZoom = (mapZoom - 0.3f).coerceAtLeast(0.5f) },
                            containerColor = Color(0xFF1E293B),
                            contentColor = Color.White,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Text("-", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // --- DETAILS BOTTOM SHEET FOR SELECTED TECHNICIAN ---
        selectedProviderForMap?.let { p ->
            val baseCoords = getProviderCoords(p)
            val walkOffset = dynamicOffsets[p.id] ?: Pair(0.0, 0.0)
            val liveLat = baseCoords.first + walkOffset.first
            val liveLng = baseCoords.second + walkOffset.second

            val distanceMeters = calculateDistanceInMeters(userCoords.first, userCoords.second, liveLat, liveLng)
            val distKm = (distanceMeters / 1000f).toDouble()
            
            // Assume 35 km/h driving speed in Yemen towns
            val travelTimeMin = ((distKm * 60.0) / 35.0).toInt().coerceAtLeast(3)

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                border = BorderStroke(1.5.dp, Color(0xFFFFD700)), // Glowing Gold
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Header Section with Profile Details
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Avatar Placeholder
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF334155)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("👨‍🔧", fontSize = 22.sp)
                            }
                            
                            Spacer(modifier = Modifier.width(8.dp))

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(p.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    if (p.isVip) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Badge(containerColor = Color(0xFFFFD700)) {
                                            Text("VIP", color = Color.Black, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    if (p.subscriptionStatus == "APPROVED") {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("✔️", fontSize = 10.sp, color = Color.Cyan)
                                    }
                                }
                                val pSpec = p.specialization.ifEmpty { p.profession }
                                Text(pSpec, color = Color.LightGray, fontSize = 11.sp)
                            }
                        }
                        IconButton(
                            onClick = { selectedProviderForMap = null },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray, modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Geographic Specs (Distance + Travel Time estimate)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📍 مسافة الرادار", color = Color.Gray, fontSize = 9.sp)
                            Text("${String.format(java.util.Locale.US, "%.1f", distKm)} كم", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color(0xFF334155)))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🚗 زمن القيادة المقدر", color = Color.Gray, fontSize = 9.sp)
                            Text("حوالي ${travelTimeMin} دقيقة", color = Color(0xFFFFD700), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color(0xFF334155)))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("⭐️ متوسط التقييم", color = Color.Gray, fontSize = 9.sp)
                            Text("${String.format(java.util.Locale.US, "%.1f", p.rating)} / 5.0", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Action 1: Dial
                        Button(
                            onClick = {
                                viewModel.logCall(p.id, p.name)
                                val uri = Uri.parse("tel:${p.phone}")
                                val intent = Intent(Intent.ACTION_DIAL, uri)
                                try { context.startActivity(intent) } catch(e: Exception) {}
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary),
                            modifier = Modifier.weight(1f).height(38.dp),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.Call, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("اتصال مباشر", fontSize = 10.sp)
                        }

                        // Action 2: Booking
                        Button(
                            onClick = { bookingProviderTargetOnMap = p },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
                            modifier = Modifier.weight(1.1f).height(38.dp),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("حجز موعد فوري", fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                        }

                        // Action 3: Directions Map Routing
                        Button(
                            onClick = {
                                val gmmIntentUri = Uri.parse("google.navigation:q=$liveLat,$liveLng")
                                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                mapIntent.setPackage("com.google.android.apps.maps")
                                try {
                                    context.startActivity(mapIntent)
                                } catch (e: Exception) {
                                    val webUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$liveLat,$liveLng")
                                    val webIntent = Intent(Intent.ACTION_VIEW, webUri)
                                    context.startActivity(webIntent)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                            modifier = Modifier.weight(1f).height(38.dp),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("خرائط قوقل 🗺️", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // --- DETAILS BOTTOM SHEET FOR SELECTED STORE/SHOP/MEDICAL CENTER/RESTAURANT ---
    selectedStoreForMap?.let { s ->
        val distMeters = calculateDistanceInMeters(userCoords.first, userCoords.second, s.latitude, s.longitude)
        val distKm = distMeters / 1000.0
        
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            border = BorderStroke(1.5.dp, themeColors.accent),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF334155)),
                            contentAlignment = Alignment.Center
                        ) {
                            val emoji = when {
                                s.sectionId.contains("medical") || s.categoryId.contains("medical") || s.name.contains("طبي") || s.name.contains("مستشفى") || s.name.contains("عيادة") -> "🏥"
                                s.sectionId.contains("restaurant") || s.categoryId.contains("restaurant") || s.name.contains("مطعم") || s.name.contains("مأكولات") -> "🍔"
                                else -> "🏪"
                            }
                            Text(emoji, fontSize = 22.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(s.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(s.description.ifEmpty { "محل ومجمع تجاري متميز" }, color = Color.LightGray, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    IconButton(
                        onClick = { selectedStoreForMap = null },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📍 مسافة الرادار", color = Color.Gray, fontSize = 9.sp)
                        Text("${String.format(java.util.Locale.US, "%.1f", distKm)} كم", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color(0xFF334155)))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🟢 الحالة", color = Color.Gray, fontSize = 9.sp)
                        Text(if (s.isActive) "مفتوح حالياً" else "مغلق", color = if (s.isActive) Color(0xFF10B981) else Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color(0xFF334155)))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📞 الهاتف", color = Color.Gray, fontSize = 9.sp)
                        Text(s.phone.ifEmpty { "غير متوفر" }, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (s.phone.isNotEmpty()) {
                        Button(
                            onClick = {
                                val uri = Uri.parse("tel:${s.phone}")
                                val intent = Intent(Intent.ACTION_DIAL, uri)
                                try { context.startActivity(intent) } catch(e: Exception) {}
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary),
                            modifier = Modifier.weight(1f).height(38.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Call, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("اتصال مباشر", fontSize = 10.sp)
                        }
                    }

                    Button(
                        onClick = {
                            val gmmIntentUri = Uri.parse("google.navigation:q=${s.latitude},${s.longitude}")
                            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                            mapIntent.setPackage("com.google.android.apps.maps")
                            try {
                                context.startActivity(mapIntent)
                            } catch (e: Exception) {
                                val webUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${s.latitude},${s.longitude}")
                                val webIntent = Intent(Intent.ACTION_VIEW, webUri)
                                context.startActivity(webIntent)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                        modifier = Modifier.weight(1f).height(38.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("خرائط قوقل 🗺️", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // --- DETAILS BOTTOM SHEET FOR SELECTED PROPERTY ---
    selectedPropertyForMap?.let { pr ->
        val distMeters = calculateDistanceInMeters(userCoords.first, userCoords.second, pr.latitude, pr.longitude)
        val distKm = distMeters / 1000.0
        
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            border = BorderStroke(1.5.dp, Color(0xFF10B981)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF334155)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🏠", fontSize = 22.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(pr.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(pr.type.ifEmpty { "عقار سكني أو تجاري" }, color = Color.LightGray, fontSize = 11.sp)
                        }
                    }
                    IconButton(
                        onClick = { selectedPropertyForMap = null },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📍 مسافة الرادار", color = Color.Gray, fontSize = 9.sp)
                        Text("${String.format(java.util.Locale.US, "%.1f", distKm)} كم", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color(0xFF334155)))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("💰 السعر", color = Color.Gray, fontSize = 9.sp)
                        Text("${pr.price} ريال", color = Color(0xFFFFD700), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color(0xFF334155)))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🏠 النوع", color = Color.Gray, fontSize = 9.sp)
                        Text(
                            text = when(pr.propertyType) {
                                "apartment" -> "شقة"
                                "house" -> "منزل"
                                "land" -> "أرض"
                                "shop" -> "محل"
                                else -> pr.propertyType
                            },
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (pr.phone.isNotEmpty()) {
                        Button(
                            onClick = {
                                val uri = Uri.parse("tel:${pr.phone}")
                                val intent = Intent(Intent.ACTION_DIAL, uri)
                                try { context.startActivity(intent) } catch(e: Exception) {}
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary),
                            modifier = Modifier.weight(1f).height(38.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Call, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("اتصال المالك", fontSize = 10.sp)
                        }
                    }

                    Button(
                        onClick = {
                            val gmmIntentUri = Uri.parse("google.navigation:q=${pr.latitude},${pr.longitude}")
                            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                            mapIntent.setPackage("com.google.android.apps.maps")
                            try {
                                context.startActivity(mapIntent)
                            } catch (e: Exception) {
                                val webUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${pr.latitude},${pr.longitude}")
                                val webIntent = Intent(Intent.ACTION_VIEW, webUri)
                                context.startActivity(webIntent)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                        modifier = Modifier.weight(1f).height(38.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("خرائط قوقل 🗺️", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Dynamic booking target dialog shown when clicked from map card
    bookingProviderTargetOnMap?.let { p ->
        val currentUserName by viewModel.currentUserName.collectAsState()
        val currentUserPhone by viewModel.currentUserPhone.collectAsState()
        val currentUserResidence by viewModel.currentUserResidence.collectAsState()

        var clientNameInput by remember { mutableStateOf(currentUserName) }
        var clientPhoneInput by remember { mutableStateOf(currentUserPhone) }
        var clientResidenceInput by remember { mutableStateOf(currentUserResidence) }
        var detailsInput by remember { mutableStateOf("") }
        var preferredTimeInput by remember { mutableStateOf("غداً الساعة 4:00 مساءً") }
        var pinCodeInput by remember { mutableStateOf("") }
        
        var formSubmittedOnce by remember { mutableStateOf(false) }
        var missingFields by remember { mutableStateOf<List<String>>(emptyList()) }
        
        AlertDialog(
            onDismissRequest = { bookingProviderTargetOnMap = null },
            containerColor = Color(0xFF1E293B),
            title = { Text("🗓️ طلب موعد حجز فوري: ${p.name}", color = Color(0xFFFFD700), fontWeight = FontWeight.Bold, fontSize = 14.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                    Text("الرجاء إدخال كافة بياناتك السكنية والاتصال لإرسال الطلب وحفظ حسابك:", color = Color.White, fontSize = 11.sp)
                    
                    if (formSubmittedOnce && missingFields.isNotEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFEF4444).copy(alpha = 0.15f)),
                            border = BorderStroke(1.dp, Color(0xFFEF4444)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("⚠️", fontSize = 14.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("يرجى إكمال وتصحيح الحقول المطلوبة:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                missingFields.forEach { field ->
                                    Text("• $field", fontSize = 10.sp, color = Color.White)
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = clientNameInput,
                        onValueChange = { clientNameInput = it },
                        label = { Text("الاسم الثلاثي بالكامل (مطلوب) *") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = formSubmittedOnce && clientNameInput.trim().isEmpty(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    OutlinedTextField(
                        value = clientPhoneInput,
                        onValueChange = { clientPhoneInput = it },
                        label = { Text("رقم الهاتف اليمني المكون من 9 أرقام (مطلوب) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        isError = formSubmittedOnce && (clientPhoneInput.trim().isEmpty() || !clientPhoneInput.trim().replace(" ", "").replace("+", "").let { p ->
                            p.length == 9 && (p.startsWith("77") || p.startsWith("73") || p.startsWith("71") || p.startsWith("70") || p.startsWith("78"))
                        }),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    OutlinedTextField(
                        value = clientResidenceInput,
                        onValueChange = { clientResidenceInput = it },
                        label = { Text("الحي والشارع ومنطقة السكن (مطلوب) *") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = formSubmittedOnce && clientResidenceInput.trim().isEmpty(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    OutlinedTextField(
                        value = detailsInput,
                        onValueChange = { detailsInput = it },
                        label = { Text("تفاصيل ومعلومات مشكلة الصيانة (مطلوب) *") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = formSubmittedOnce && detailsInput.trim().isEmpty(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    OutlinedTextField(
                        value = preferredTimeInput,
                        onValueChange = { preferredTimeInput = it },
                        label = { Text("الوقت المقترح") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    OutlinedTextField(
                        value = pinCodeInput,
                        onValueChange = { pinCodeInput = it },
                        label = { Text("🔑 كلمة مرور سرية لحفظ وتأمين الحجز (مطلوب) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        isError = formSubmittedOnce && pinCodeInput.trim().isEmpty(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleanName = clientNameInput.trim()
                        val cleanPhone = clientPhoneInput.trim().replace(" ", "").replace("+", "")
                        val cleanResidence = clientResidenceInput.trim()
                        val cleanDetails = detailsInput.trim()
                        val cleanPin = pinCodeInput.trim()

                        // Validations and detailed toast error reporting
                        val isValidYemeniPhone = cleanPhone.length == 9 && (
                            cleanPhone.startsWith("77") || 
                            cleanPhone.startsWith("73") || 
                            cleanPhone.startsWith("71") || 
                            cleanPhone.startsWith("70") || 
                            cleanPhone.startsWith("78")
                        )

                        val missing = mutableListOf<String>()
                        if (cleanName.isEmpty()) missing.add("الاسم الثلاثي بالكامل")
                        if (cleanPhone.isEmpty()) {
                            missing.add("رقم الهاتف")
                        } else if (!isValidYemeniPhone) {
                            missing.add("رقم الهاتف اليمني غير صحيح (يجب أن يتكون من 9 أرقام ويبدأ بـ 77، 73، 71، 70، 78)")
                        }
                        if (cleanResidence.isEmpty()) missing.add("الحي والشارع ومكان السكن")
                        if (cleanDetails.isEmpty()) missing.add("تفاصيل ومعلومات المشكلة")
                        if (cleanPin.isEmpty()) missing.add("كلمة المرور السرية للحجز")

                        if (missing.isNotEmpty()) {
                            formSubmittedOnce = true
                            missingFields = missing
                            Toast.makeText(context, "⚠️ هناك حقول مطلوبة أو غير صحيحة!", Toast.LENGTH_LONG).show()
                        } else {
                            formSubmittedOnce = false
                            missingFields = emptyList()
                            viewModel.addBooking(
                                name = cleanName,
                                phone = cleanPhone,
                                area = cleanResidence,
                                serviceType = "رادار الخريطة - $cleanDetails",
                                providerId = p.id,
                                providerName = p.name,
                                dateString = "2026-06-21",
                                timeString = preferredTimeInput.trim(),
                                customPassword = cleanPin,
                                pinCode = cleanPin
                            )
                            viewModel.registerGuestUser(
                                context = context,
                                name = cleanName,
                                phone = cleanPhone,
                                residence = cleanResidence,
                                password = cleanPin
                            )
                            bookingProviderTargetOnMap = null
                            selectedProviderForMap = null
                            Toast.makeText(context, "تم إرسال طلب الحجز بنجاح بالرقم المرجعي الموحد! 🚀", Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary)
                ) {
                    Text("تأكيد وتأمين الطلب")
                }
            },
            dismissButton = {
                TextButton(onClick = { bookingProviderTargetOnMap = null }) {
                    Text("إلغاء", color = Color.White)
                }
            }
        )
    }
}
