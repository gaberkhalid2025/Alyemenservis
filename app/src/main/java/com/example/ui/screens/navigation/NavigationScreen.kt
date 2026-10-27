@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.screens.navigation

import android.annotation.SuppressLint
import android.speech.tts.TextToSpeech
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.ProviderEntity
import com.example.data.StoreEntity
import com.example.ui.screens.map.utils.MapDistanceCalculator
import com.example.utils.RealValhallaNavigator
import com.example.utils.ValhallaTileDownloader
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.util.Locale

/**
 * 🧭 NavigationScreen
 * شاشة الملاحة اليمنية الذكية الشاملة:
 * 1. ملاحة مباشرة متصلة عبر شبكة OSRM الدقيقة بين المدن والشوارع بدقة 100%.
 * 2. بحث فوري عن أي شارع، حي، منزل، محل، أو معلم يمني (OSM Nominatim + بيانات التطبيق).
 * 3. دعم المحطات ونقاط التوقف المتعددة (Multi-Waypoints).
 * 4. نطق صوتي عربي فوري (TTS) عند الانطلاق، وعند الانعطافات، وعند الوصول لكل نقطة محددة.
 * 5. محاكي سير تفاعلي لتجربة الملاحة والنطق الصوتي خطوة بخطوة.
 * 6. دعم تحميل حزمة الخرائط (25MB) للعمل أوفلاين التام.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun NavigationScreen(
    userLat: Double = 15.3694,
    userLng: Double = 44.1910,
    targetName: String = "الوجهة المحددة",
    targetLat: Double = 15.3500,
    targetLng: Double = 44.2000,
    nearbyProviders: List<ProviderEntity> = emptyList(),
    nearbyStores: List<StoreEntity> = emptyList(),
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // 1. إدارة محطات ونقاط التوقف (Multi-Waypoints)
    val hasInitialDest = targetName.isNotBlank() && targetName != "الوجهة المحددة" && (targetLat != userLat || targetLng != userLng)
    var waypoints by remember {
        mutableStateOf(
            if (hasInitialDest) {
                listOf(
                    RealValhallaNavigator.NavigationWaypoint(
                        id = "origin",
                        name = "موقعي الحالي",
                        latitude = userLat,
                        longitude = userLng
                    ),
                    RealValhallaNavigator.NavigationWaypoint(
                        id = "dest",
                        name = targetName,
                        latitude = targetLat,
                        longitude = targetLng
                    )
                )
            } else {
                listOf(
                    RealValhallaNavigator.NavigationWaypoint(
                        id = "origin",
                        name = "موقعي الحالي",
                        latitude = userLat,
                        longitude = userLng
                    )
                )
            }
        )
    }

    // 2. حالات البحث والوجهات
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<RealValhallaNavigator.SearchAddressResult>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    // 3. مسار الملاحة والخطوات
    var routeResult by remember { mutableStateOf<RealValhallaNavigator.NavigationRouteResult?>(null) }
    var isLoadingRoute by remember { mutableStateOf(hasInitialDest) }
    var currentStepIndex by remember { mutableIntStateOf(0) }

    // 4. محاكاة السير المباشر
    var isSimulating by remember { mutableStateOf(false) }
    var simulatedLat by remember { mutableDoubleStateOf(userLat) }
    var simulatedLng by remember { mutableDoubleStateOf(userLng) }

    // 5. النطق الصوتي (TTS)
    var isVoiceEnabled by remember { mutableStateOf(true) }
    var ttsInstance by remember { mutableStateOf<TextToSpeech?>(null) }

    DisposableEffect(context) {
        var localTts: TextToSpeech? = null
        localTts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = localTts?.setLanguage(Locale("ar"))
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    localTts?.setLanguage(Locale.getDefault())
                }
            }
        }
        ttsInstance = localTts
        onDispose {
            localTts?.stop()
            localTts?.shutdown()
        }
    }

    fun speak(text: String) {
        if (isVoiceEnabled && ttsInstance != null && text.isNotBlank()) {
            ttsInstance?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "NavVoice_${System.currentTimeMillis()}")
        }
    }

    // 6. تحميل حزمة الـ 25MB للأوفلاين (اختياري بالكامل)
    var isTilesAvailable by remember { mutableStateOf(RealValhallaNavigator.isReady(context)) }
    var showDownloadDialog by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var downloadStatusText by remember { mutableStateOf("") }

    // 7. مرجع الخريطة التفاعلية WebView
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isMapReady by remember { mutableStateOf(false) }

    // دالة إرسال ورسم المسار على الخريطة
    fun sendRouteToMap(route: RealValhallaNavigator.NavigationRouteResult?) {
        if (route == null || webViewRef == null || !isMapReady) return
        val coords = route.coordinates
        if (coords.isNotEmpty()) {
            val jsonArray = JSONArray()
            coords.forEach { pt ->
                val p = JSONArray()
                p.put(pt.first)
                p.put(pt.second)
                jsonArray.put(p)
            }
            val script = "if (window.drawRoute) { window.drawRoute('${jsonArray}', '#00E5FF'); }"
            webViewRef?.evaluateJavascript(script, null)
        }
    }

    // حساب المسار فور تغير المحطات أو نقاط التوقف
    fun recalculateRoute() {
        if (waypoints.size < 2) {
            routeResult = null
            isLoadingRoute = false
            return
        }
        coroutineScope.launch {
            isLoadingRoute = true
            val result = RealValhallaNavigator.calculateRouteWithWaypoints(context, waypoints)
            routeResult = result
            isLoadingRoute = false
            currentStepIndex = 0

            // نطق بدء الملاحة
            val destTitle = waypoints.lastOrNull()?.name ?: "الوجهة"
            speak("بدء الملاحة نحو $destTitle. المسافة ${result.formattedDistance} والوقت المقدر ${result.formattedDuration}.")

            // رسم المسار على الخريطة
            sendRouteToMap(result)
        }
    }

    LaunchedEffect(waypoints) {
        recalculateRoute()
    }

    LaunchedEffect(isMapReady, routeResult) {
        if (isMapReady && routeResult != null) {
            sendRouteToMap(routeResult)
        }
    }

    // البحث الحي عند كتابة نص في مربع البحث
    LaunchedEffect(searchQuery) {
        if (searchQuery.trim().length >= 2) {
            isSearching = true
            delay(350) // Debounce
            val results = RealValhallaNavigator.searchAddressAndPlaces(
                query = searchQuery,
                nearbyProviders = nearbyProviders,
                nearbyStores = nearbyStores
            )
            searchResults = results
            isSearching = false
        } else {
            searchResults = emptyList()
            isSearching = false
        }
    }

    // حلقة محاكاة السير المباشر (Simulation Loop)
    LaunchedEffect(isSimulating, routeResult) {
        if (isSimulating && routeResult != null) {
            val steps = routeResult?.steps ?: emptyList()
            while (isSimulating && currentStepIndex < steps.size) {
                val step = steps[currentStepIndex]

                // تحديث موقع المستخدم المتحرك على الخريطة
                val coords = routeResult?.coordinates ?: emptyList()
                if (coords.isNotEmpty()) {
                    val stepFraction = (currentStepIndex.toDouble() / steps.size.toDouble()).coerceIn(0.0, 1.0)
                    val coordIdx = (stepFraction * (coords.size - 1)).toInt().coerceIn(0, coords.size - 1)
                    val pt = coords[coordIdx]
                    simulatedLat = pt.first
                    simulatedLng = pt.second
                    webViewRef?.evaluateJavascript("if (window.updateUserLocation) { window.updateUserLocation(${pt.first}, ${pt.second}, 15, true); }", null)
                }

                // نطق التوجيه الصوتي لكل خطوة
                if (step.iconType == "DESTINATION") {
                    val isFinal = (currentStepIndex >= steps.size - 1)
                    if (isFinal) {
                        speak("لقد وصلت إلى وجهتك النهائية: ${step.targetWaypointName} بنجاح!")
                    } else {
                        speak("لقد وصلت إلى: ${step.targetWaypointName}. جاري التوجيه نحو النقطة التالية.")
                    }
                } else {
                    speak(step.instruction)
                }

                delay(3200L)
                if (currentStepIndex < steps.size - 1) {
                    currentStepIndex++
                } else {
                    isSimulating = false
                    break
                }
            }
        }
    }

    BackHandler {
        if (isSimulating) {
            isSimulating = false
        } else {
            onBack()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = Color(0xFF0B1120)
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFF0B1120))
        ) {
            // 🗺️ 1. خريطة Leaflet التفاعلية الحقيقية في الخلفية (100% بدون شاشة سوداء)
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setBackgroundColor(android.graphics.Color.parseColor("#0B1120"))
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            allowFileAccess = true
                            allowContentAccess = true
                            cacheMode = WebSettings.LOAD_DEFAULT
                            userAgentString = "AlYemenServicesApp/1.0 (Android; OpenStreetMap)"
                            setSupportZoom(true)
                            builtInZoomControls = false
                            displayZoomControls = false
                            loadWithOverviewMode = true
                            useWideViewPort = true
                        }

                        addJavascriptInterface(object {
                            @JavascriptInterface
                            fun onMapReady() {
                                isMapReady = true
                            }
                            @JavascriptInterface
                            fun onMapClicked() {
                                // إخفاء لوحة البحث عند النقر على الخريطة
                                searchQuery = ""
                            }
                        }, "AndroidBridge")

                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isMapReady = true
                                // تمركز الخريطة ورسم المسار
                                evaluateJavascript("if (window.updateUserLocation) { window.updateUserLocation($userLat, $userLng, 25, true); }", null)
                                sendRouteToMap(routeResult)
                            }
                        }

                        loadUrl("file:///android_asset/map.html")
                        webViewRef = this
                    }
                },
                update = { webView ->
                    webViewRef = webView
                    if (isMapReady && routeResult != null) {
                        sendRouteToMap(routeResult)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // 🔍 2. الجزء العلوي: شريط الحالة والبحث والمحطات
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // شريط الأدوات العلوي مع أزرار التحكم
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFF0F172A).copy(alpha = 0.95f),
                    border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.4f)),
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "رجوع",
                                    tint = Color.White
                                )
                            }
                            Column {
                                Text(
                                    text = "🧭 نظام الملاحة الذكي",
                                    color = Color.White,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = routeResult?.source ?: (if (isTilesAvailable) "خرائط أوفلاين 💾" else "شبكة الطرق العالمية 🌐"),
                                    color = if (routeResult?.isOffline == false) Color(0xFF00E5FF) else Color(0xFF10B981),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // زر الصوت
                            IconButton(
                                onClick = {
                                    isVoiceEnabled = !isVoiceEnabled
                                    if (isVoiceEnabled) speak("تم تفعيل التوجيه الصوتي باللغة العربية")
                                    else Toast.makeText(context, "تم كتم الصوت 🔇", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (isVoiceEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                                    contentDescription = "الصوت",
                                    tint = if (isVoiceEnabled) Color(0xFF10B981) else Color(0xFF94A3B8),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // زر تحميل الخرائط للأوفلاين (25MB)
                            Button(
                                onClick = { showDownloadDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isTilesAvailable) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFF0284C7)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                border = if (isTilesAvailable) BorderStroke(1.dp, Color(0xFF10B981)) else null,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (isTilesAvailable) Icons.Default.CheckCircle else Icons.Default.CloudDownload,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isTilesAvailable) "أوفلاين 🇾🇪" else "تحميل (25MB)",
                                    fontSize = 10.5.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // مربع البحث الذكي عن أي عنوان أو شارع أو محل في اليمن
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "ابحث عن شارع، محل، حي، أو وجهة...",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.5.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "مسح", tint = Color.Gray)
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF0F172A).copy(alpha = 0.95f),
                        unfocusedContainerColor = Color(0xFF0F172A).copy(alpha = 0.90f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF00E5FF),
                        unfocusedBorderColor = Color(0xFF334155)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("nav_search_input")
                )

                // نتائج البحث المنسدلة
                if (searchQuery.trim().length >= 2) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.98f)),
                        border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f)),
                        elevation = CardDefaults.cardElevation(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp)
                    ) {
                        if (isSearching) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color(0xFF00E5FF))
                                    Text("جاري البحث في الشوارع والأماكن...", color = Color.White, fontSize = 12.sp)
                                }
                            }
                        } else if (searchResults.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("لم يتم العثور على عنوان مطابق، حاول البحث باسم آخر", color = Color(0xFF94A3B8), fontSize = 12.sp)
                            }
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                                items(searchResults) { item ->
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                modifier = Modifier.weight(1f),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(text = item.icon, fontSize = 18.sp)
                                                Column {
                                                    Text(
                                                        text = item.shortName,
                                                        color = Color.White,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = item.displayName,
                                                        color = Color(0xFF94A3B8),
                                                        fontSize = 10.5.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }

                                            // زرا الاختيار: كوجهة رئيسية أو كمحطة توقف
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                // تعيين كوجهة رئيسية 🎯
                                                Button(
                                                    onClick = {
                                                        val newWp = RealValhallaNavigator.NavigationWaypoint(
                                                            id = "dest_${System.currentTimeMillis()}",
                                                            name = item.shortName,
                                                            latitude = item.latitude,
                                                            longitude = item.longitude
                                                        )
                                                        // تحديث الوجهة النهائية
                                                        waypoints = listOf(waypoints.first()) + newWp
                                                        searchQuery = ""
                                                        speak("تم تحديد الوجهة: ${item.shortName}")
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                    modifier = Modifier.height(30.dp)
                                                ) {
                                                    Text("وجهة 🎯", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }

                                                // إضافة كنقطة توقف ➕
                                                OutlinedButton(
                                                    onClick = {
                                                        val stopWp = RealValhallaNavigator.NavigationWaypoint(
                                                            id = "stop_${System.currentTimeMillis()}",
                                                            name = item.shortName,
                                                            latitude = item.latitude,
                                                            longitude = item.longitude
                                                        )
                                                        val currentList = waypoints.toMutableList()
                                                        // إدراج النقطة قبل الوجهة الأخيرة
                                                        currentList.add(currentList.size - 1, stopWp)
                                                        waypoints = currentList
                                                        searchQuery = ""
                                                        speak("تمت إضافة محطة: ${item.shortName}")
                                                    },
                                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00E5FF)),
                                                    border = BorderStroke(1.dp, Color(0xFF00E5FF)),
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                                    modifier = Modifier.height(30.dp)
                                                ) {
                                                    Text("+ محطة", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                        HorizontalDivider(
                                            modifier = Modifier.padding(top = 8.dp),
                                            color = Color.White.copy(alpha = 0.06f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // شريط عرض المحطات ونقاط التوقف الحالية (Waypoints Chips)
                if (waypoints.size > 2) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        waypoints.forEachIndexed { index, wp ->
                            val isOrigin = index == 0
                            val isFinal = index == waypoints.size - 1
                            val label = when {
                                isOrigin -> "🟢 الانطلاق: ${wp.name}"
                                isFinal -> "🏁 النهاية: ${wp.name}"
                                else -> "📍 محطة $index: ${wp.name}"
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isFinal) Color(0xFF10B981).copy(alpha = 0.25f) else Color(0xFF1E293B).copy(alpha = 0.9f),
                                border = BorderStroke(1.dp, if (isFinal) Color(0xFF10B981) else Color(0xFF00E5FF).copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = label,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    if (!isOrigin && !isFinal) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "حذف المحطة",
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clickable {
                                                    val updated = waypoints.toMutableList()
                                                    updated.removeAt(index)
                                                    waypoints = updated
                                                }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // بطاقة التوجيه الفوري خطوة بخطوة (Turn-by-Turn Card)
                routeResult?.let { result ->
                    val steps = result.steps
                    val currentStep = steps.getOrNull(currentStepIndex) ?: steps.lastOrNull()
                    if (currentStep != null) {
                        val turnIcon = when (currentStep.iconType) {
                            "LEFT" -> Icons.Default.TurnLeft
                            "RIGHT" -> Icons.Default.TurnRight
                            "ROUNDABOUT" -> Icons.Default.RotateRight
                            "HIGHWAY" -> Icons.Default.DirectionsCar
                            "DESTINATION" -> Icons.Default.CheckCircle
                            else -> Icons.Default.Straight
                        }
                        val iconBg = if (currentStep.iconType == "DESTINATION") Color(0xFF10B981) else Color(0xFF0284C7)

                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = Color(0xFF0F172A).copy(alpha = 0.95f),
                            border = BorderStroke(1.5.dp, Color(0xFF00E5FF)),
                            shadowElevation = 10.dp,
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
                                    color = iconBg,
                                    modifier = Modifier.size(46.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = turnIcon,
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
                                        Text(
                                            text = "بعد ${currentStep.distanceText}",
                                            color = Color(0xFF00E5FF),
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (currentStep.streetName.isNotBlank()) {
                                            Text(
                                                text = "· ${currentStep.streetName}",
                                                color = Color(0xFF94A3B8),
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    Text(
                                        text = currentStep.instruction,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                // زر تشغيل صوت التوجيه يدوياً
                                IconButton(
                                    onClick = { speak(currentStep.instruction) },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Campaign,
                                        contentDescription = "استماع",
                                        tint = Color(0xFF10B981)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 🚗 3. اللوحة السفلية: إحصائيات الوقت والمسافة والمحاكاة والإنهاء
            routeResult?.let { result ->
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = Color(0xFF0F172A).copy(alpha = 0.96f),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    shadowElevation = 16.dp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // إحصائيات المسافة والوقت الإجمالي
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = result.formattedDuration,
                                    color = Color(0xFF10B981),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "الوقت المتوقع للوصول",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF1E293B)
                                ) {
                                    Text(
                                        text = if (isSimulating) "🚘 محاكاة السير نشطة" else "📍 ملاحة مباشرة",
                                        color = if (isSimulating) Color(0xFF00E5FF) else Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = result.formattedDistance,
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "المسافة الكلية",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        // شريط التقدم المرئي للخطوات
                        val stepsCount = result.steps.size.coerceAtLeast(1)
                        val progressFraction = ((currentStepIndex + 1).toFloat() / stepsCount.toFloat()).coerceIn(0.05f, 1f)
                        LinearProgressIndicator(
                            progress = { progressFraction },
                            color = Color(0xFF00E5FF),
                            trackColor = Color(0xFF334155),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )

                        // أزرار العمليات: تجربة محاكاة السير + إنهاء الملاحة
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // زر تجربة محاكاة السير (لاختبار الصوت والانتقال بين الشوارع)
                            Button(
                                onClick = {
                                    isSimulating = !isSimulating
                                    if (isSimulating) {
                                        Toast.makeText(context, "🚘 بدأت محاكاة السير والنطق الصوتي التلقائي", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSimulating) Color(0xFFF59E0B) else Color(0xFF0284C7)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1.2f)
                                    .height(44.dp)
                                    .testTag("simulate_nav_btn")
                            ) {
                                Icon(
                                    imageVector = if (isSimulating) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isSimulating) "إيقاف السير" else "محاكاة السير والصوت",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // زر إنهاء الملاحة
                            Button(
                                onClick = onBack,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("exit_nav_button")
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("إنهاء الملاحة", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 💡 إرشاد اختيار الوجهة عندما لا تكون هناك وجهة محددة بعد
            if (waypoints.size < 2) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF0F172A).copy(alpha = 0.95f),
                    border = BorderStroke(1.5.dp, Color(0xFF00E5FF)),
                    shadowElevation = 14.dp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Place, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(24.dp))
                            Text(
                                text = "اختر وجهتك لبدء الملاحة المباشرة",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "اكتب اسم أي شارع، حي، محل، أو مدينة في شريط البحث أعلاه، وستظهر لك الوجهة فوراً مع خيار بدء الملاحة 🎯 أو إضافة محطات ➕",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.5.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 17.sp
                        )
                    }
                }
            }

            // مؤشر التحميل الأولي للمسار
            if (isLoadingRoute && routeResult == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF0F172A),
                        border = BorderStroke(1.dp, Color(0xFF00E5FF)),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(color = Color(0xFF00E5FF))
                            Text(
                                text = "جاري حساب المسار المباشر عبر شبكة الطرق...",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // نافذة حوار تحميل خرائط الأوفلاين (25MB) - اختياري بالكامل
            if (showDownloadDialog) {
                AlertDialog(
                    onDismissRequest = {
                        if (!isDownloading) showDownloadDialog = false
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(36.dp)
                        )
                    },
                    title = {
                        Text(
                            text = "حزمة الخرائط الأوفلاين (25MB) - اختياري",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    },
                    text = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "حزمة الخرائط اختيارية تماماً. في حال عدم تحميلها، تعمل الملاحة بدقة 100% عبر الإنترنت لجميع المدن والطرق والشوارع اليمنية.\n\nتحميل الحزمة يتيح لك تشغيل الملاحة أوفلاين بدون إنترنت نهائياً.",
                                fontSize = 12.5.sp,
                                color = Color(0xFFCBD5E1),
                                lineHeight = 19.sp
                            )

                            if (isDownloading) {
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { downloadProgress },
                                    color = Color(0xFF10B981),
                                    trackColor = Color(0xFF334155),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                )
                                Text(
                                    text = downloadStatusText,
                                    fontSize = 11.sp,
                                    color = Color(0xFF10B981),
                                    fontWeight = FontWeight.SemiBold
                                )
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
                                            onProgress = { p, msg ->
                                                downloadProgress = p
                                                downloadStatusText = msg
                                            }
                                        )
                                        isDownloading = false
                                        if (result.isSuccess) {
                                            isTilesAvailable = true
                                            showDownloadDialog = false
                                            Toast.makeText(context, "✅ تم حفظ وتثبيت خريطة اليمن بنجاح! الملاحة تعمل الآن أوفلاين.", Toast.LENGTH_LONG).show()
                                            recalculateRoute()
                                        } else {
                                            val err = result.exceptionOrNull()?.localizedMessage ?: "خطأ في الشبكة"
                                            Toast.makeText(context, "❌ فشل التحميل: $err", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("بدء التحميل الآن (25MB)", fontWeight = FontWeight.Bold)
                            }
                        }
                    },
                    dismissButton = {
                        if (!isDownloading) {
                            TextButton(onClick = { showDownloadDialog = false }) {
                                Text("إلغاء", color = Color(0xFF94A3B8))
                            }
                        }
                    },
                    containerColor = Color(0xFF0F172A),
                    shape = RoundedCornerShape(18.dp)
                )
            }
        }
    }
}
