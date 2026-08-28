@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.screens.map

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.PropertyEntity
import com.example.data.ProviderEntity
import com.example.data.StoreEntity
import com.example.ui.screens.map.utils.OfflineMapManager
import com.example.utils.getProviderCoords
import com.example.utils.getStoreCoords
import com.example.utils.getPropertyCoords

/**
 * 🗺️ RealLeafletMapView
 * Production-ready OpenStreetMap Leaflet implementation with:
 * - Dynamic marker updates without full page reloads
 * - WebChromeClient for JavaScript console & error monitoring in Logcat
 * - Offline tile caching via OfflineMapManager
 * - Approximate fallback coordinates for zero-coordinate entities
 * - Smooth CircularProgressIndicator during map initialization
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun RealLeafletMapView(
    userCoords: Pair<Double, Double>,
    nearbyProviders: List<ProviderEntity>,
    nearbyStores: List<StoreEntity>,
    nearbyProperties: List<PropertyEntity>,
    dynamicOffsets: Map<String, Pair<Double, Double>>,
    onProviderSelected: (ProviderEntity) -> Unit,
    onStoreSelected: (StoreEntity) -> Unit,
    onPropertySelected: (PropertyEntity) -> Unit,
    onSwitchToRadar: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isMapLoading by remember { mutableStateOf(true) }
    var hasLoadError by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    // Run cache purge in background
    LaunchedEffect(Unit) {
        OfflineMapManager.purgeCacheIfNeeded(context)
    }

    // Build JSON data with approximate fallback coordinates for items with lat/lng = 0
    val providerMarkers = nearbyProviders.mapIndexed { idx, p ->
        val baseCoords = getProviderCoords(p)
        val walkOffset = dynamicOffsets[p.id] ?: Pair(0.0, 0.0)
        val rawLat = baseCoords.first + walkOffset.first
        val rawLng = baseCoords.second + walkOffset.second

        // Fallback for (0,0) coordinates
        val isApproximate = rawLat == 0.0 && rawLng == 0.0
        val liveLat = if (isApproximate) userCoords.first + ((idx % 5) * 0.003 - 0.006) else rawLat
        val liveLng = if (isApproximate) userCoords.second + (((idx / 5) % 5) * 0.003 - 0.006) else rawLng

        val categoryEmoji = when {
            p.categoryId.contains("spaka") || p.profession.contains("سباك") -> "🔧"
            p.categoryId.contains("kahraba") || p.profession.contains("كهربا") -> "⚡"
            p.categoryId.contains("solar") || p.profession.contains("طاقة") -> "☀️"
            p.categoryId.contains("dehan") || p.profession.contains("دهان") -> "🎨"
            p.categoryId.contains("hadada") || p.profession.contains("حداد") -> "🔨"
            p.categoryId.contains("ac") || p.categoryId.contains("tabreed") || p.profession.contains("تكييف") -> "❄️"
            p.categoryId.contains("car") || p.categoryId.contains("mechanic") || p.profession.contains("ميكانيك") -> "🚗"
            p.categoryId.contains("carpentry") || p.categoryId.contains("najjara") || p.profession.contains("نجار") -> "🪚"
            else -> "👷"
        }
        val safeName = p.name.replace("\"", "\\\"").replace("'", "\\'")
        val pSpec = (p.customCategoryName.ifEmpty { p.specialization.ifEmpty { p.profession } }).replace("\"", "\\\"").replace("'", "\\'")
        val cleanSpec = if (pSpec.isEmpty()) "فني صيانة معتمد" else pSpec
        val safePhone = p.phone.replace("\"", "")
        """
        {
            id: "${p.id}",
            type: "PROVIDER",
            name: "$safeName",
            lat: $liveLat,
            lng: $liveLng,
            isApprox: $isApproximate,
            emoji: "$categoryEmoji",
            spec: "$cleanSpec",
            phone: "$safePhone",
            rating: "${p.rating}",
            status: "${if (p.isAvailable) "متاح لاستقبال الطلبات 🟢" else "مشغول حالياً 🟡"}",
            badgeColor: "#00E5FF",
            serviceCategory: "فنيون صيانة"
        }
        """
    }

    val storeMarkers = nearbyStores.mapIndexed { idx, s ->
        val coords = getStoreCoords(s)
        val isMedical = s.sectionId.contains("medical") || s.categoryId.contains("medical") || s.categoryId.contains("pharmacy") || s.medicalLicenseNo.isNotBlank() || s.name.contains("طبي") || s.name.contains("مستشفى") || s.name.contains("عيادة") || s.name.contains("صيدلية")
        val isRestaurant = !isMedical && (s.sectionId.contains("restaurant") || s.categoryId.contains("restaurant") || s.name.contains("مطعم") || s.name.contains("مأكولات") || s.name.contains("كافيه") || s.name.contains("شاورما"))
        
        val emoji = when {
            isMedical -> "🏥"
            isRestaurant -> "🍔"
            s.sectionId.contains("supermarket") || s.name.contains("بقالة") || s.name.contains("هايبر") -> "🛒"
            else -> "🏪"
        }
        val safeName = s.name.replace("\"", "\\\"").replace("'", "\\'")
        val safeDesc = s.description.replace("\"", "\\\"").replace("'", "\\'").take(60)
        val cleanDesc = if (safeDesc.isEmpty()) (if (isMedical) "مركز طبي / صيدلية معتمدة" else "محل / متجر معتمد") else safeDesc
        val safePhone = s.phone.replace("\"", "")
        
        val isApproximate = coords.first == 0.0 && coords.second == 0.0
        val liveLat = if (isApproximate) userCoords.first + (((idx + 2) % 6) * 0.0035 - 0.007) else coords.first
        val liveLng = if (isApproximate) userCoords.second + ((((idx + 2) / 6) % 6) * 0.0035 - 0.007) else coords.second

        val (badgeColor, categoryLabel) = when {
            isMedical -> Pair("#EC4899", "مراكز طبية وصيدليات")
            isRestaurant -> Pair("#F59E0B", "مطاعم وكافيهات")
            else -> Pair("#10B981", "متاجر وأسواق")
        }

        """
        {
            id: "${s.id}",
            type: "STORE",
            name: "$safeName",
            lat: $liveLat,
            lng: $liveLng,
            isApprox: $isApproximate,
            emoji: "$emoji",
            spec: "$cleanDesc",
            phone: "$safePhone",
            rating: "${s.rating}",
            status: "${if (s.isActive) "مفتوح ويستقبل الطلبات 🟢" else "مغلق حالياً 🔴"}",
            badgeColor: "$badgeColor",
            serviceCategory: "$categoryLabel"
        }
        """
    }

    val propertyMarkers = nearbyProperties.mapIndexed { idx, pr ->
        val coords = getPropertyCoords(pr)
        val safeTitle = pr.title.replace("\"", "\\\"").replace("'", "\\'")
        val safeDesc = pr.description.replace("\"", "\\\"").replace("'", "\\'").take(60)
        val cleanDesc = if (safeDesc.isEmpty()) "عقار معروض" else safeDesc
        val safePhone = pr.phone.replace("\"", "")
        val priceFormatted = "${pr.price} ${pr.currency}"

        val isApproximate = coords.first == 0.0 && coords.second == 0.0
        val liveLat = if (isApproximate) userCoords.first + (((idx + 4) % 5) * 0.004 - 0.008) else coords.first
        val liveLng = if (isApproximate) userCoords.second + ((((idx + 4) / 5) % 5) * 0.004 - 0.008) else coords.second

        """
        {
            id: "${pr.id}",
            type: "PROPERTY",
            name: "$safeTitle",
            lat: $liveLat,
            lng: $liveLng,
            isApprox: $isApproximate,
            emoji: "🏠",
            spec: "$cleanDesc ($priceFormatted)",
            phone: "$safePhone",
            rating: "5.0",
            status: "معروض للإيجار / البيع 🟢",
            badgeColor: "#8B5CF6",
            serviceCategory: "عقارات وشقق"
        }
        """
    }

    val markersJson = (providerMarkers + storeMarkers + propertyMarkers).joinToString(",")

    // Hot-update markers in WebView if already initialized
    LaunchedEffect(markersJson) {
        webViewRef?.evaluateJavascript("if (window.updateMapMarkers) { window.updateMapMarkers([$markersJson]); }", null)
    }

    val htmlContent = remember(userCoords, markersJson) {
        try {
            val template = context.assets.open("map.html").bufferedReader().use { it.readText() }
            template
                .replace("_USER_LAT_", userCoords.first.toString())
                .replace("_USER_LNG_", userCoords.second.toString())
                .replace("_MARKERS_JSON_", markersJson)
        } catch (e: Exception) {
            Log.e("RealLeafletMapView", "Error reading map.html asset", e)
            ""
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    webViewRef = this
                    setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
                    isHapticFeedbackEnabled = true
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        cacheMode = WebSettings.LOAD_DEFAULT
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    }
                    webChromeClient = object : WebChromeClient() {
                        override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                            Log.d("RealLeafletMapView", "${consoleMessage?.message()} -- From line ${consoleMessage?.lineNumber()} of ${consoleMessage?.sourceId()}")
                            return true
                        }
                    }
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isMapLoading = false
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?
                        ) {
                            super.onReceivedError(view, request, error)
                            if (request?.isForMainFrame == true) {
                                hasLoadError = true
                                isMapLoading = false
                            }
                        }

                        override fun onRenderProcessGone(
                            view: WebView?,
                            detail: android.webkit.RenderProcessGoneDetail?
                        ): Boolean = true
                    }
                    addJavascriptInterface(object {
                        @android.webkit.JavascriptInterface
                        fun onMarkerClicked(type: String, id: String) {
                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                try {
                                    when (type) {
                                        "PROVIDER" -> nearbyProviders.find { it.id == id }?.let(onProviderSelected)
                                        "STORE" -> nearbyStores.find { it.id == id }?.let(onStoreSelected)
                                        "PROPERTY" -> nearbyProperties.find { it.id == id }?.let(onPropertySelected)
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }

                        @android.webkit.JavascriptInterface
                        fun onMapReady() {
                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                isMapLoading = false
                            }
                        }

                        @android.webkit.JavascriptInterface
                        fun onMapLoadFailed() {
                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                hasLoadError = true
                                isMapLoading = false
                            }
                        }

                        @android.webkit.JavascriptInterface
                        fun openNavigation(lat: Double, lng: Double, label: String) {
                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                try {
                                    val gmmIntentUri = Uri.parse("google.navigation:q=$lat,$lng")
                                    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                                        setPackage("com.google.android.apps.maps")
                                    }
                                    if (mapIntent.resolveActivity(ctx.packageManager) != null) {
                                        ctx.startActivity(mapIntent)
                                    } else {
                                        val browserUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$lat,$lng")
                                        val browserIntent = Intent(Intent.ACTION_VIEW, browserUri)
                                        ctx.startActivity(Intent.createChooser(browserIntent, "فتح الاتجاهات"))
                                    }
                                } catch (e: Exception) {
                                    try {
                                        val fallbackUri = Uri.parse("geo:$lat,$lng?q=$lat,$lng($label)")
                                        val fallbackIntent = Intent(Intent.ACTION_VIEW, fallbackUri)
                                        ctx.startActivity(Intent.createChooser(fallbackIntent, "تطبيق الخرائط"))
                                    } catch (ex: Exception) {
                                        Log.e("RealLeafletMapView", "Navigation error: ${ex.message}")
                                    }
                                }
                            }
                        }
                    }, "AndroidBridge")

                    tag = htmlContent
                    loadDataWithBaseURL("https://openstreetmap.org", htmlContent, "text/html", "UTF-8", null)
                }
            },
            update = { webView ->
                if (webView.tag != htmlContent) {
                    webView.tag = htmlContent
                    webView.loadDataWithBaseURL("https://openstreetmap.org", htmlContent, "text/html", "UTF-8", null)
                }
            }
        )

        // Loading Indicator
        AnimatedVisibility(
            visible = isMapLoading,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF0F172A).copy(alpha = 0.9f),
                modifier = Modifier.padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color(0xFF00E5FF),
                        strokeWidth = 2.5.dp
                    )
                    Text("جاري تحميل الخريطة المباشرة...", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Fallback UI overlay if network fails
        if (hasLoadError) {
            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "📡 تعذر الاتصال بخرائط الإنترنت",
                            color = Color(0xFFFFD700),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "يمكنك التبديل إلى الرادار المحلي التفاعلي لاستعراض الخدمات بدون إنترنت.",
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp
                        )
                    }
                    if (onSwitchToRadar != null) {
                        Button(
                            onClick = onSwitchToRadar,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("الرادار المحلي", color = Color(0xFF0F172A), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
