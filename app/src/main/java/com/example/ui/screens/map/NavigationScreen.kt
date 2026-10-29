package com.example.ui.screens.map

import android.content.Context
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PropertyEntity
import com.example.data.ProviderEntity
import com.example.data.StoreEntity
import com.example.ui.screens.map.utils.MapDistanceCalculator
import com.example.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.*

/**
 * 🧭 NavigationScreen
 * [إصلاح ملاحة نهائي] شاشة الملاحة اليمنية المتطورة مع دعم تتبع GPS الحقيقي،
 * ورسم مسار Polyline دقيق، وبحث فوري عن الأماكن والشوارع، وتوجيه صوتي عربي ناطق،
 * وإعادة الحساب التلقائي عند الخروج عن المسار.
 */
@Composable
fun NavigationScreen(
    targetEntity: Any?,
    userLat: Double,
    userLng: Double,
    onFinishNavigation: () -> Unit,
    downloadUrl: String = ValhallaTileDownloader.DEFAULT_DOWNLOAD_URL,
    nearbyProviders: List<ProviderEntity> = emptyList(),
    nearbyStores: List<StoreEntity> = emptyList(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // 🔊 [إصلاح ملاحة نهائي] محرك النطق الصوتي العربي
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var isVoiceEnabled by remember { mutableStateOf(true) }
    var lastSpokenInstruction by remember { mutableStateOf("") }

    DisposableEffect(context) {
        var textToSpeechInstance: TextToSpeech? = null
        textToSpeechInstance = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val res = textToSpeechInstance?.isLanguageAvailable(Locale("ar"))
                if (res == TextToSpeech.LANG_AVAILABLE || res == TextToSpeech.LANG_COUNTRY_AVAILABLE) {
                    textToSpeechInstance?.language = Locale("ar")
                } else {
                    textToSpeechInstance?.language = Locale.ENGLISH
                }
            }
        }
        tts = textToSpeechInstance
        onDispose {
            textToSpeechInstance?.stop()
            textToSpeechInstance?.shutdown()
        }
    }

    fun speak(text: String) {
        if (!isVoiceEnabled || text.isBlank() || text == lastSpokenInstruction) return
        lastSpokenInstruction = text
        try {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        } catch (e: Exception) {
            android.util.Log.e("NavigationScreen", "TTS error: ${e.message}")
        }
    }

    // 📍 [إصلاح ملاحة نهائي] تحديد الوجهة الأولية بأمان تام
    val hasInitialTarget = targetEntity != null
    val initialName = when (targetEntity) {
        is ProviderEntity -> targetEntity.name
        is StoreEntity -> targetEntity.name
        is PropertyEntity -> targetEntity.title
        else -> ""
    }
    val initialLat = when (targetEntity) {
        is ProviderEntity -> targetEntity.latitude
        is StoreEntity -> targetEntity.latitude
        is PropertyEntity -> targetEntity.latitude
        else -> 0.0
    }
    val initialLng = when (targetEntity) {
        is ProviderEntity -> targetEntity.longitude
        is StoreEntity -> targetEntity.longitude
        is PropertyEntity -> targetEntity.longitude
        else -> 0.0
    }

    // قائمة محطات التوقف والوجهات
    val waypoints = remember {
        mutableStateListOf<NavWaypoint>().apply {
            if (hasInitialTarget && initialLat != 0.0 && initialLng != 0.0) {
                add(
                    NavWaypoint(
                        name = initialName.ifBlank { "الوجهة المختارة" },
                        latitude = initialLat,
                        longitude = initialLng
                    )
                )
            }
        }
    }

    // 🔍 [إصلاح ملاحة نهائي] شريط البحث العلوي السريع
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<RealValhallaNavigator.SearchAddressResult>>(emptyList()) }
    var searchJob by remember { mutableStateOf<Job?>(null) }
    var showSearchResultsDropdown by remember { mutableStateOf(false) }

    // حالة حزمة الخرائط الأوفلاين
    var isTilesDownloaded by remember {
        mutableStateOf(ValhallaTileDownloader.isTilesDownloaded(context))
    }

    // 🛣️ [إصلاح ملاحة نهائي] نتيجة حساب المسار الحقيقي
    var routeResult by remember { mutableStateOf<NavRouteResult?>(null) }
    var isLoadingRoute by remember { mutableStateOf(false) }
    var lastRecalculateTimeMs by remember { mutableLongStateOf(0L) }

    // دالة حساب المسار المركزية الآمنة
    fun triggerRouteCalculation(reason: String = "") {
        if (waypoints.isEmpty()) {
            routeResult = null
            return
        }
        val safeLat = if (userLat != 0.0) userLat else 15.3694
        val safeLng = if (userLng != 0.0) userLng else 44.1910

        isLoadingRoute = true
        coroutineScope.launch {
            try {
                val originWp = NavWaypoint(
                    name = "موقعي الحالي",
                    latitude = safeLat,
                    longitude = safeLng,
                    type = WaypointType.START
                )
                val result = withContext(Dispatchers.IO) {
                    RealValhallaNavigator.calculateRoute(context, originWp, waypoints.toList())
                }
                routeResult = result
                lastRecalculateTimeMs = System.currentTimeMillis()

                if (reason == "off_route") {
                    speak("تم تحديث المسار نحو وجهتك")
                } else if (result.legs.isNotEmpty()) {
                    val firstStep = result.getAllSteps().firstOrNull()?.instruction
                    if (!firstStep.isNullOrBlank()) {
                        speak(firstStep)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("NavigationScreen", "Route calc error: ${e.message}", e)
            } finally {
                isLoadingRoute = false
            }
        }
    }

    // حساب المسار عند تغيير الوجهات أو حالة التحميل
    LaunchedEffect(waypoints.toList(), isTilesDownloaded) {
        if (waypoints.isNotEmpty()) {
            triggerRouteCalculation("init")
        }
    }

    // 🎯 [إصلاح ملاحة نهائي] حساب المسافة الحقيقية للوجهة النشطة
    val activeDest = waypoints.lastOrNull()
    val remainingMeters = remember(userLat, userLng, activeDest, routeResult) {
        if (activeDest != null && userLat != 0.0 && userLng != 0.0) {
            HaversinePrecisionNavigator.calculateDistance(userLat, userLng, activeDest.latitude, activeDest.longitude)
        } else {
            routeResult?.totalDistanceMeters ?: 0.0
        }
    }

    // 🏁 [إصلاح ملاحة نهائي] منطق الوصول الحقيقي ومنع الوصول الوهمي
    var initialDistanceMeters by remember { mutableDoubleStateOf(0.0) }
    var hasAnnouncedArrival by remember { mutableStateOf(false) }

    LaunchedEffect(activeDest) {
        if (activeDest != null && userLat != 0.0 && userLng != 0.0) {
            val d = HaversinePrecisionNavigator.calculateDistance(userLat, userLng, activeDest.latitude, activeDest.longitude)
            initialDistanceMeters = d
            hasAnnouncedArrival = false
        }
    }

    LaunchedEffect(remainingMeters) {
        if (activeDest != null && initialDistanceMeters > 50.0 && remainingMeters < 25.0 && !hasAnnouncedArrival) {
            hasAnnouncedArrival = true
            speak("لقد وصلت إلى وجهتك بنجاح، حمداً لله على سلامتكم")
            Toast.makeText(context, "🎉 لقد وصلت إلى وجهتك بنجاح!", Toast.LENGTH_LONG).show()
        }
    }

    // 🔄 [إصلاح ملاحة نهائي] كشف الخروج عن المسار وإعادة الحساب التلقائي (> 85 متراً)
    LaunchedEffect(userLat, userLng, routeResult) {
        val coords = routeResult?.coordinates
        if (coords != null && coords.isNotEmpty() && userLat != 0.0 && userLng != 0.0) {
            val now = System.currentTimeMillis()
            if (now - lastRecalculateTimeMs > 12000L && remainingMeters > 50.0) {
                // حساب أقرب مسافة بين موقع المستخدم وأي نقطة في المسار
                var minDistanceToPolyline = Double.MAX_VALUE
                for (pt in coords) {
                    val d = HaversinePrecisionNavigator.calculateDistance(userLat, userLng, pt.first, pt.second)
                    if (d < minDistanceToPolyline) {
                        minDistanceToPolyline = d
                    }
                }
                if (minDistanceToPolyline > 85.0) {
                    android.util.Log.d("NavigationScreen", "Off route detected: $minDistanceToPolyline m. Recalculating...")
                    triggerRouteCalculation("off_route")
                }
            }
        }
    }

    // حساب نسبة التقدم والزمن المتوقع
    val totalDistanceMeters = (routeResult?.totalDistanceMeters ?: (remainingMeters + 50.0)).coerceAtLeast(1.0)
    val progress = if (initialDistanceMeters > 0.0) {
        (1.0 - (remainingMeters / initialDistanceMeters.coerceAtLeast(1.0))).toFloat().coerceIn(0.05f, 1.0f)
    } else {
        (1.0 - (remainingMeters / totalDistanceMeters)).toFloat().coerceIn(0.05f, 1.0f)
    }
    val remainingDistanceText = MapDistanceCalculator.formatDistance(remainingMeters)
    val remainingEtaText = MapDistanceCalculator.computeEta(remainingMeters)

    // الخطوة الإرشادية الحالية
    val allSteps = routeResult?.getAllSteps() ?: emptyList()
    val currentStepText = allSteps.firstOrNull()?.instruction ?: if (waypoints.isEmpty()) "ابحث عن وجهة لبدء الملاحة" else "اتجه نحو الوجهة المحددة"

    // نافذة تنزيل الحزمة
    var showDownloadDialog by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var downloadStatusText by remember { mutableStateOf("") }

    // زوايا العرض والخريطة
    var zoomScale by remember { mutableFloatStateOf(1.3f) }
    var panOffset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    var tilt by remember { mutableFloatStateOf(35f) }
    var rotation by remember { mutableFloatStateOf(0f) }
    var is3DMode by remember { mutableStateOf(true) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0B1120))
    ) {
        // 🗺️ [إصلاح ملاحة نهائي] خريطة الملاحة الحية مع تمرير إحداثيات المسار الكاملة (routeCoordinates)
        RealLeafletMapView(
            userCoords = Pair(if (userLat != 0.0) userLat else 15.3694, if (userLng != 0.0) userLng else 44.1910),
            routeCoordinates = routeResult?.coordinates ?: emptyList(),
            nearbyProviders = nearbyProviders,
            nearbyStores = nearbyStores,
            nearbyProperties = emptyList(),
            dynamicOffsets = emptyMap(),
            currentMapMode = if (is3DMode) MapMode.VELA_3D else MapMode.HOT,
            selectedCity = "الكل",
            zoomScale = zoomScale,
            onZoomScaleChange = { zoomScale = it },
            panOffset = panOffset,
            onPanOffsetChange = { panOffset = it },
            selectedEntity = targetEntity,
            onProviderSelected = { p ->
                waypoints.add(NavWaypoint(name = p.name, latitude = p.latitude, longitude = p.longitude))
                Toast.makeText(context, "➕ تمت إضافة المحطة: ${p.name}", Toast.LENGTH_SHORT).show()
            },
            onStoreSelected = { s ->
                waypoints.add(NavWaypoint(name = s.name, latitude = s.latitude, longitude = s.longitude))
                Toast.makeText(context, "➕ تمت إضافة المحطة: ${s.name}", Toast.LENGTH_SHORT).show()
            },
            onPropertySelected = {},
            onDeselect = {},
            tilt = tilt,
            rotation = rotation,
            modifier = Modifier.fillMaxSize()
        )

        // 🌟 [إصلاح ملاحة نهائي] الجزء العلوي: شريط البحث + المحطات + بطاقة التوجيه
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 🔎 شريط البحث المباشر عن الوجهات والشوارع
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF0F172A).copy(alpha = 0.96f),
                border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.6f)),
                shadowElevation = 10.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onFinishNavigation,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = Color.White
                        )
                    }

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { query ->
                            searchQuery = query
                            searchJob?.cancel()
                            if (query.trim().length >= 2) {
                                isSearching = true
                                showSearchResultsDropdown = true
                                searchJob = coroutineScope.launch {
                                    delay(300L)
                                    searchResults = RealValhallaNavigator.searchAddressAndPlaces(
                                        query = query,
                                        nearbyProviders = nearbyProviders,
                                        nearbyStores = nearbyStores
                                    )
                                    isSearching = false
                                }
                            } else {
                                searchResults = emptyList()
                                showSearchResultsDropdown = false
                                isSearching = false
                            }
                        },
                        placeholder = {
                            Text(
                                "ابحث عن شارع، محل، فني، أو وجهة...",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp
                            )
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            cursorColor = Color(0xFF00E5FF)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("nav_search_input")
                    )

                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                searchQuery = ""
                                showSearchResultsDropdown = false
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = "مسح", tint = Color.Gray, modifier = Modifier.size(18.dp))
                        }
                    }

                    // زر كتم / تفعيل الصوت
                    IconButton(
                        onClick = {
                            isVoiceEnabled = !isVoiceEnabled
                            Toast.makeText(
                                context,
                                if (isVoiceEnabled) "🔊 تم تفعيل التوجيه الصوتي" else "🔇 تم كتم الصوت",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isVoiceEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = "الصوت",
                            tint = if (isVoiceEnabled) Color(0xFF00E5FF) else Color(0xFF64748B),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // زر تبديل المنظور 3D / 2D
                    IconButton(
                        onClick = {
                            is3DMode = !is3DMode
                            tilt = if (is3DMode) 35f else 0f
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ViewInAr,
                            contentDescription = "3D",
                            tint = if (is3DMode) Color(0xFF10B981) else Color(0xFF94A3B8),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // 📋 قائمة نتائج البحث المنسدلة
            if (showSearchResultsDropdown && (searchResults.isNotEmpty() || isSearching)) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF0F172A).copy(alpha = 0.98f),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    shadowElevation = 14.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                ) {
                    if (isSearching) {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color(0xFF00E5FF), modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                            items(searchResults) { item ->
                                Surface(
                                    onClick = {
                                        waypoints.clear()
                                        waypoints.add(
                                            NavWaypoint(
                                                name = item.shortName,
                                                latitude = item.latitude,
                                                longitude = item.longitude
                                            )
                                        )
                                        searchQuery = ""
                                        showSearchResultsDropdown = false
                                        triggerRouteCalculation("user_selected")
                                        Toast.makeText(context, "🎯 تم تحديد الوجهة: ${item.shortName}", Toast.LENGTH_SHORT).show()
                                    },
                                    color = Color.Transparent,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(item.icon, fontSize = 18.sp)
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(item.shortName, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            Text(item.displayName, color = Color(0xFF94A3B8), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                        Icon(Icons.Default.Directions, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(18.dp))
                                    }
                                }
                                HorizontalDivider(color = Color(0xFF1E293B), thickness = 0.5.dp)
                            }
                        }
                    }
                }
            }

            // 🔖 شريط محطات المسار الأفقية
            if (waypoints.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF1E293B).copy(alpha = 0.9f),
                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Surface(shape = CircleShape, color = Color(0xFF10B981), modifier = Modifier.size(6.dp)) {}
                                Text("موقعي الحالي", color = Color(0xFFE2E8F0), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    items(waypoints) { wp ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF1E293B).copy(alpha = 0.95f),
                            border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Place, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(12.dp))
                                Text(wp.name, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                if (waypoints.size > 1) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "حذف",
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clickable { waypoints.remove(wp) }
                                    )
                                }
                            }
                        }
                    }

                    item {
                        if (!isTilesDownloaded) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF0284C7).copy(alpha = 0.9f),
                                modifier = Modifier.clickable { showDownloadDialog = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.CloudDownload, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                    Text("أوفلاين (25MB)", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // 🧭 [إصلاح ملاحة نهائي] بطاقة التوجيه خطوة بخطوة (Turn-by-Turn Maneuver)
            if (waypoints.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFF0F172A).copy(alpha = 0.96f),
                    border = BorderStroke(1.5.dp, Color(0xFF00E5FF)),
                    shadowElevation = 12.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF0284C7),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Navigation,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (routeResult?.isOffline == true) Color(0xFF10B981) else Color(0xFF00E5FF),
                                    modifier = Modifier.size(6.dp)
                                ) {}
                                Text(
                                    text = routeResult?.engineSource ?: "جاري التجهيز...",
                                    color = Color(0xFF00E5FF),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (isLoadingRoute) {
                                    Text("· تحديث...", color = Color(0xFFF59E0B), fontSize = 10.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = currentStepText,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // زر إعادة التوسيط على موقع المستخدم
                        IconButton(
                            onClick = {
                                panOffset = androidx.compose.ui.geometry.Offset.Zero
                                zoomScale = 1.3f
                                Toast.makeText(context, "🎯 تم التوسيط على موقعك", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.MyLocation, contentDescription = "موقعي", tint = Color(0xFF00E5FF))
                        }
                    }
                }
            }
        }

        // 🏁 [إصلاح ملاحة نهائي] لوحة المعلومات السفلية (Bottom Dashboard)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF0F172A).copy(alpha = 0.96f),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                shadowElevation = 14.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (waypoints.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = remainingEtaText,
                                    color = Color(0xFF10B981),
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "الوقت المقدر للوصول",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = remainingDistanceText,
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "المسافة المتبقية",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = Color(0xFF00E5FF),
                            trackColor = Color(0xFF334155)
                        )
                    } else {
                        Text(
                            text = "👈 اختر وجهة من شريط البحث بالأعلى لبدء الإرشاد الملاحي",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        )
                    }

                    Button(
                        onClick = onFinishNavigation,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFDC2626),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("end_navigation_btn")
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إنهاء الملاحة", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 📥 حوار تنزيل حزمة الخرائط الأوفلاين
        if (showDownloadDialog) {
            AlertDialog(
                onDismissRequest = { if (!isDownloading) showDownloadDialog = false },
                icon = { Icon(Icons.Default.CloudDownload, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(36.dp)) },
                title = { Text("تحميل حزمة الملاحة الكاملة (أوفلاين)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center) },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("لتفعيل الملاحة الدقيقة خطوة بخطوة بدون إنترنت، يحتاج التطبيق إلى تحميل ملف الخرائط (25 ميجابايت).", fontSize = 13.sp, color = Color(0xFFCBD5E1))
                        if (isDownloading) {
                            LinearProgressIndicator(
                                progress = { downloadProgress },
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                color = Color(0xFF10B981),
                                trackColor = Color(0xFF334155)
                            )
                            Text(downloadStatusText, fontSize = 11.5.sp, color = Color(0xFF10B981), fontWeight = FontWeight.SemiBold)
                        }
                    }
                },
                confirmButton = {
                    if (!isDownloading) {
                        Button(
                            onClick = {
                                isDownloading = true
                                coroutineScope.launch {
                                    val result = ValhallaTileDownloader.downloadAndExtractTiles(
                                        context = context,
                                        downloadUrl = downloadUrl,
                                        onProgress = { p, status ->
                                            downloadProgress = p
                                            downloadStatusText = status
                                        }
                                    )
                                    isDownloading = false
                                    if (result.isSuccess) {
                                        isTilesDownloaded = true
                                        showDownloadDialog = false
                                        triggerRouteCalculation("downloaded")
                                        Toast.makeText(context, "✅ تم تثبيت حزمة الملاحة الأوفلاين بنجاح! 🇾🇪", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, "❌ فشل التحميل", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("تحميل الآن (25 MB)", fontWeight = FontWeight.Bold)
                        }
                    }
                },
                dismissButton = {
                    if (!isDownloading) {
                        TextButton(onClick = { showDownloadDialog = false }) {
                            Text("لاحقاً", color = Color(0xFF94A3B8))
                        }
                    }
                },
                containerColor = Color(0xFF0F172A),
                shape = RoundedCornerShape(18.dp)
            )
        }
    }
}
