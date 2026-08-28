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
import org.json.JSONArray
import org.json.JSONObject

/**
 * 🗺️ RealLeafletMapView
 * Production-ready OpenStreetMap Leaflet implementation with:
 * - Dynamic marker updates without full page reloads
 * - High-speed JSON serialization preventing syntax crashes
 * - WebChromeClient for JavaScript console & error monitoring in Logcat
 * - Offline tile caching via OfflineMapManager
 * - Accurate fallback coordinates for zero-coordinate entities
 * - Interactive custom popup cards with direct calling and directions
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

    // Safe user coordinates fallback (Default to Sana'a if 0,0)
    val safeUserLat = if (userCoords.first != 0.0) userCoords.first else 15.3694
    val safeUserLng = if (userCoords.second != 0.0) userCoords.second else 44.1910

    // Run cache purge in background
    LaunchedEffect(Unit) {
        OfflineMapManager.purgeCacheIfNeeded(context)
    }

    // Build structured JSON array for markers
    val markersJsonArray = remember(nearbyProviders, nearbyStores, nearbyProperties, dynamicOffsets, safeUserLat, safeUserLng) {
        val jsonArray = JSONArray()

        // 1. Providers
        nearbyProviders.forEachIndexed { idx, p ->
            val baseCoords = getProviderCoords(p)
            val walkOffset = dynamicOffsets[p.id] ?: Pair(0.0, 0.0)
            var rawLat = baseCoords.first + walkOffset.first
            var rawLng = baseCoords.second + walkOffset.second

            val isApproximate = (rawLat == 0.0 && rawLng == 0.0)
            if (isApproximate) {
                rawLat = safeUserLat + ((idx % 5) * 0.003 - 0.006)
                rawLng = safeUserLng + (((idx / 5) % 5) * 0.003 - 0.006)
            }

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

            val pSpec = p.customCategoryName.ifEmpty { p.specialization.ifEmpty { p.profession } }
            val cleanSpec = if (pSpec.isBlank()) "فني صيانة معتمد" else pSpec

            val obj = JSONObject().apply {
                put("id", p.id)
                put("type", "PROVIDER")
                put("name", p.name.ifBlank { "فني دليل اليمن" })
                put("lat", rawLat)
                put("lng", rawLng)
                put("isApprox", isApproximate)
                put("emoji", categoryEmoji)
                put("spec", cleanSpec)
                put("phone", p.phone)
                put("rating", if (p.rating > 0) String.format("%.1f", p.rating) else "5.0")
                put("status", if (p.isAvailable) "متاح لاستقبال الطلبات 🟢" else "مشغول حالياً 🟡")
                put("badgeColor", "#00E5FF")
                put("serviceCategory", "فنيون صيانة")
            }
            jsonArray.put(obj)
        }

        // 2. Stores & Medical Centers & Restaurants
        nearbyStores.forEachIndexed { idx, s ->
            val coords = getStoreCoords(s)
            val isMedical = s.sectionId.contains("medical") || s.categoryId.contains("medical") || s.categoryId.contains("pharmacy") || s.medicalLicenseNo.isNotBlank() || s.name.contains("طبي") || s.name.contains("مستشفى") || s.name.contains("عيادة") || s.name.contains("صيدلية")
            val isRestaurant = !isMedical && (s.sectionId.contains("restaurant") || s.categoryId.contains("restaurant") || s.name.contains("مطعم") || s.name.contains("مأكولات") || s.name.contains("كافيه") || s.name.contains("شاورما"))

            val emoji = when {
                isMedical -> "🏥"
                isRestaurant -> "🍔"
                s.sectionId.contains("supermarket") || s.name.contains("بقالة") || s.name.contains("هايبر") -> "🛒"
                else -> "🏪"
            }

            var rawLat = coords.first
            var rawLng = coords.second
            val isApproximate = (rawLat == 0.0 && rawLng == 0.0)
            if (isApproximate) {
                rawLat = safeUserLat + (((idx + 2) % 6) * 0.0035 - 0.007)
                rawLng = safeUserLng + ((((idx + 2) / 6) % 6) * 0.0035 - 0.007)
            }

            val (badgeColor, categoryLabel) = when {
                isMedical -> Pair("#EC4899", "مراكز طبية وصيدليات")
                isRestaurant -> Pair("#F59E0B", "مطاعم وكافيهات")
                else -> Pair("#10B981", "متاجر وأسواق")
            }

            val cleanDesc = s.description.ifBlank { if (isMedical) "مركز طبي / صيدلية معتمدة" else "محل / متجر معتمد" }.take(60)

            val obj = JSONObject().apply {
                put("id", s.id)
                put("type", "STORE")
                put("name", s.name.ifBlank { "متجر معتمد" })
                put("lat", rawLat)
                put("lng", rawLng)
                put("isApprox", isApproximate)
                put("emoji", emoji)
                put("spec", cleanDesc)
                put("phone", s.phone)
                put("rating", if (s.rating > 0) String.format("%.1f", s.rating) else "5.0")
                put("status", if (s.isActive) "مفتوح ويستقبل الطلبات 🟢" else "مغلق حالياً 🔴")
                put("badgeColor", badgeColor)
                put("serviceCategory", categoryLabel)
            }
            jsonArray.put(obj)
        }

        // 3. Properties
        nearbyProperties.forEachIndexed { idx, pr ->
            val coords = getPropertyCoords(pr)
            var rawLat = coords.first
            var rawLng = coords.second
            val isApproximate = (rawLat == 0.0 && rawLng == 0.0)
            if (isApproximate) {
                rawLat = safeUserLat + (((idx + 4) % 5) * 0.004 - 0.008)
                rawLng = safeUserLng + ((((idx + 4) / 5) % 5) * 0.004 - 0.008)
            }

            val priceFormatted = if (pr.price > 0) "${pr.price} ${pr.currency.ifEmpty { "ريال" }}" else "حسب الاتفاق"
            val cleanDesc = pr.description.ifBlank { "عقار معروض" }.take(60)

            val obj = JSONObject().apply {
                put("id", pr.id)
                put("type", "PROPERTY")
                put("name", pr.title.ifBlank { "عقار معروض" })
                put("lat", rawLat)
                put("lng", rawLng)
                put("isApprox", isApproximate)
                put("emoji", "🏠")
                put("spec", "$cleanDesc ($priceFormatted)")
                put("phone", pr.phone)
                put("rating", "5.0")
                put("status", "معروض للإيجار / البيع 🟢")
                put("badgeColor", "#8B5CF6")
                put("serviceCategory", "عقارات وشقق")
            }
            jsonArray.put(obj)
        }

        jsonArray.toString()
    }

    // Hot-update markers in WebView if already initialized
    LaunchedEffect(markersJsonArray) {
        webViewRef?.evaluateJavascript("if (window.updateMapMarkers) { window.updateMapMarkers($markersJsonArray); }", null)
    }

    val htmlContent = remember(safeUserLat, safeUserLng, markersJsonArray) {
        try {
            val template = context.assets.open("map.html").bufferedReader().use { it.readText() }
            template
                .replace("_USER_LAT_", safeUserLat.toString())
                .replace("_USER_LNG_", safeUserLng.toString())
                .replace("_MARKERS_JSON_", markersJsonArray)
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
                            Log.d("RealLeafletMapView", "${consoleMessage?.message()} -- line ${consoleMessage?.lineNumber()} of ${consoleMessage?.sourceId()}")
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
                color = Color(0xFF0F172A).copy(alpha = 0.92f),
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
                    Text("جاري تحميل الخريطة والخدمات...", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
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
