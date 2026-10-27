package com.example.ui.screens.map

import android.content.Context
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PropertyEntity
import com.example.data.ProviderEntity
import com.example.data.StoreEntity
import com.example.ui.screens.map.components.OfflineInteractiveMap
import com.example.ui.screens.map.utils.MapDistanceCalculator
import com.example.utils.MockValhallaNavigator
import com.example.utils.RealValhallaNavigator
import com.example.utils.ValhallaTileDownloader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * 🧭 NavigationScreen
 * شاشة الملاحة اليمنية التفاعلية الذكية مع دعم محطات التوقف المتعددة (Multi-Stop Navigation)
 * والنطق الصوتي (TextToSpeech) والتخطيط الهجين (RealValhallaNavigator vs MockValhallaNavigator).
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

    // Initialize TextToSpeech for voice announcements upon reaching stops
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(context) {
        var textToSpeechInstance: TextToSpeech? = null
        textToSpeechInstance = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeechInstance?.language = Locale("ar")
            }
        }
        tts = textToSpeechInstance
        onDispose {
            textToSpeechInstance?.stop()
            textToSpeechInstance?.shutdown()
        }
    }

    // Initial destination setup
    val initialName = when (targetEntity) {
        is ProviderEntity -> targetEntity.name
        is StoreEntity -> targetEntity.name
        is PropertyEntity -> targetEntity.title
        else -> "الوجهة المحددة"
    }
    val initialLat = when (targetEntity) {
        is ProviderEntity -> targetEntity.latitude
        is StoreEntity -> targetEntity.latitude
        is PropertyEntity -> targetEntity.latitude
        else -> 15.3694
    }
    val initialLng = when (targetEntity) {
        is ProviderEntity -> targetEntity.longitude
        is StoreEntity -> targetEntity.longitude
        is PropertyEntity -> targetEntity.longitude
        else -> 44.1910
    }

    // Multi-stop waypoints list state
    val waypoints = remember {
        mutableStateListOf(
            RealValhallaNavigator.NavigationWaypoint(
                name = initialName,
                latitude = initialLat,
                longitude = initialLng
            )
        )
    }

    // Add extra stop dialog state
    var showAddStopDialog by remember { mutableStateOf(false) }
    var stopSearchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<RealValhallaNavigator.SearchAddressResult>>(emptyList()) }

    var isTilesDownloaded by remember {
        mutableStateOf(ValhallaTileDownloader.isTilesDownloaded(context))
    }

    // Hybrid Route Result State (Real vs Mock)
    var routeResult by remember {
        mutableStateOf<RealValhallaNavigator.NavigationRouteResult?>(null)
    }
    var mockRouteResult by remember {
        mutableStateOf<MockValhallaNavigator.RouteResult?>(null)
    }
    var isLoadingRoute by remember { mutableStateOf(true) }

    // Fetch route asynchronously
    LaunchedEffect(waypoints.toList(), isTilesDownloaded, userLat, userLng) {
        isLoadingRoute = true
        if (RealValhallaNavigator.isReady(context) || isTilesDownloaded) {
            val realWps = waypoints.map { RealValhallaNavigator.NavigationWaypoint(it.id, it.name, it.latitude, it.longitude, it.isReached) }
            val originWp = RealValhallaNavigator.Waypoint("موقعي الحالي", userLat, userLng)
            val restWps = realWps.map { RealValhallaNavigator.Waypoint(it.name, it.latitude, it.longitude, it.id) }
            routeResult = RealValhallaNavigator.calculateRoute(context, originWp, restWps)
        } else {
            val mockWps = waypoints.map { MockValhallaNavigator.Waypoint(it.name, it.latitude, it.longitude, it.id) }
            val originMock = MockValhallaNavigator.Waypoint("موقعي الحالي", userLat, userLng)
            mockRouteResult = MockValhallaNavigator.calculateRoute(context, originMock, mockWps)
        }
        isLoadingRoute = false
    }

    val totalDistanceMeters = routeResult?.totalDistanceMeters ?: mockRouteResult?.totalDistanceMeters ?: 1000.0
    var remainingMeters by remember(totalDistanceMeters) { mutableDoubleStateOf(totalDistanceMeters) }
    var currentSpeedKmh by remember { mutableIntStateOf(50) }
    var currentStepIndex by remember { mutableIntStateOf(0) }

    // Download dialog state
    var showDownloadDialog by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var downloadStatusText by remember { mutableStateOf("") }

    // Navigation simulation & voice announcements
    LaunchedEffect(totalDistanceMeters) {
        while (remainingMeters > 30) {
            delay(1500L)
            remainingMeters = (remainingMeters - 150).coerceAtLeast(10.0)
            val currentStepsList = routeResult?.steps ?: emptyList()
            if (currentStepsList.isNotEmpty()) {
                val stepFrac = 1.0 - (remainingMeters / totalDistanceMeters)
                val newIdx = (stepFrac * currentStepsList.size).toInt().coerceIn(0, currentStepsList.size - 1)
                if (newIdx != currentStepIndex) {
                    currentStepIndex = newIdx
                    val instruction = currentStepsList[currentStepIndex].instruction
                    tts?.speak(instruction, TextToSpeech.QUEUE_FLUSH, null, null)
                }
            }
        }
        // Reached destination voice announcement
        tts?.speak("لقد وصلت إلى وجهتك النهائية بنجاح", TextToSpeech.QUEUE_FLUSH, null, null)
    }

    val progress = (1.0 - (remainingMeters / totalDistanceMeters)).toFloat().coerceIn(0.05f, 1.0f)
    val remainingDistanceText = MapDistanceCalculator.formatDistance(remainingMeters)
    val remainingEtaText = MapDistanceCalculator.computeEta(remainingMeters)

    val currentStepText = routeResult?.steps?.getOrNull(currentStepIndex)?.instruction 
        ?: mockRouteResult?.steps?.getOrNull(currentStepIndex)?.instruction 
        ?: "اتجه نحو الوجهة المحددة"

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0B1120))
    ) {
        var zoomScale by remember { mutableFloatStateOf(1.2f) }
        var panOffset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

        // Interactive Map
        OfflineInteractiveMap(
            userCoords = Pair(userLat, userLng),
            nearbyProviders = nearbyProviders,
            nearbyStores = nearbyStores,
            nearbyProperties = emptyList(),
            dynamicOffsets = emptyMap(),
            selectedCity = "الكل",
            zoomScale = zoomScale,
            onZoomScaleChange = { zoomScale = it },
            panOffset = panOffset,
            onPanOffsetChange = { panOffset = it },
            selectedEntity = targetEntity,
            onProviderSelected = { p ->
                waypoints.add(RealValhallaNavigator.NavigationWaypoint(name = p.name, latitude = p.latitude, longitude = p.longitude))
                Toast.makeText(context, "➕ تمت إضافة المحطة: ${p.name}", Toast.LENGTH_SHORT).show()
            },
            onStoreSelected = { s ->
                waypoints.add(RealValhallaNavigator.NavigationWaypoint(name = s.name, latitude = s.latitude, longitude = s.longitude))
                Toast.makeText(context, "➕ تمت إضافة المحطة: ${s.name}", Toast.LENGTH_SHORT).show()
            },
            onPropertySelected = {},
            onDeselect = {},
            modifier = Modifier.fillMaxSize()
        )

        // Top Header & Waypoints bar
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Status & Download Banner
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF0F172A).copy(alpha = 0.95f),
                border = BorderStroke(1.dp, if (isTilesDownloaded) Color(0xFF10B981) else Color(0xFFF59E0B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isTilesDownloaded) Color(0xFF10B981) else Color(0xFFF59E0B),
                            modifier = Modifier.size(8.dp)
                        ) {}
                        Text(
                            text = if (isTilesDownloaded) "🌟 ملاحة أوفلاين متعددة المحطات (Valhalla 🇾🇪)" else "⚡ ملاحة هجينة ذكية متعددة المحطات",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = { showAddStopDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("إضافة محطة", fontSize = 10.sp, color = Color.White)
                        }

                        if (!isTilesDownloaded) {
                            Button(
                                onClick = { showDownloadDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("تحميل (25MB)", fontSize = 10.sp, color = Color.White)
                            }
                        }
                    }
                }
            }

            // Horizontal Waypoints Chips Row
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(waypoints) { wp ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1E293B).copy(alpha = 0.95f),
                        border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Place, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(14.dp))
                            Text(wp.name, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            if (waypoints.size > 1) {
                                IconButton(
                                    onClick = { waypoints.remove(wp) },
                                    modifier = Modifier.size(16.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "حذف", tint = Color(0xFFEF4444), modifier = Modifier.size(12.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Turn-by-Turn Instruction Card
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF0F172A).copy(alpha = 0.95f),
                border = BorderStroke(1.5.dp, Color(0xFF00E5FF)),
                shadowElevation = 12.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF0284C7),
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Navigation,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "توجيه صوتي مفعل 🔊",
                                color = Color(0xFF00E5FF),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "· $currentSpeedKmh كم/س",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = currentStepText,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "عدد المحطات: ${waypoints.size} | المسافة الكلية: ${routeResult?.formattedDistance ?: mockRouteResult?.formattedDistance ?: "0 م"}",
                            color = Color(0xFFCBD5E1),
                            fontSize = 11.sp
                        )
                    }

                    IconButton(
                        onClick = onFinishNavigation,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("exit_navigation_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إنهاء",
                            tint = Color(0xFF94A3B8)
                        )
                    }
                }
            }
        }

        // Bottom Dashboard Bar
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF0F172A).copy(alpha = 0.95f),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                shadowElevation = 14.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = remainingEtaText,
                                color = Color(0xFF10B981),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "الوقت المتوقع للوصول",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = remainingDistanceText,
                                color = Color.White,
                                fontSize = 18.sp,
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
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = Color(0xFF00E5FF),
                        trackColor = Color(0xFF334155)
                    )

                    Button(
                        onClick = onFinishNavigation,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFDC2626),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("end_navigation_btn")
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("إنهاء الملاحة متعددة المحطات", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Add Stop Search Dialog
        if (showAddStopDialog) {
            AlertDialog(
                onDismissRequest = { showAddStopDialog = false },
                containerColor = Color(0xFF0F172A),
                title = { Text("إضافة محطة توقف جديدة", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = stopSearchQuery,
                            onValueChange = {
                                stopSearchQuery = it
                                coroutineScope.launch {
                                    searchResults = RealValhallaNavigator.searchAddressAndPlaces(it, nearbyProviders, nearbyStores)
                                }
                            },
                            placeholder = { Text("ابحث عن شارع، محل، أو فني...", color = Color(0xFF94A3B8)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF00E5FF),
                                unfocusedBorderColor = Color(0xFF334155)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(searchResults) { res ->
                                Surface(
                                    onClick = {
                                        waypoints.add(
                                            RealValhallaNavigator.NavigationWaypoint(
                                                name = res.shortName,
                                                latitude = res.latitude,
                                                longitude = res.longitude
                                            )
                                        )
                                        showAddStopDialog = false
                                        stopSearchQuery = ""
                                        Toast.makeText(context, "✅ تمت إضافة المحطة: ${res.shortName}", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF1E293B),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(res.icon, fontSize = 16.sp)
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(res.shortName, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Text(res.displayName, color = Color(0xFF94A3B8), fontSize = 10.sp, maxLines = 1)
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAddStopDialog = false }) {
                        Text("إغلاق", color = Color(0xFF00E5FF))
                    }
                },
                shape = RoundedCornerShape(18.dp)
            )
        }

        // Download Dialog
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
