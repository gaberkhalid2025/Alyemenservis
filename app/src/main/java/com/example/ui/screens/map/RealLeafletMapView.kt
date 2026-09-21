package com.example.ui.screens.map

import android.content.Intent
import android.net.Uri
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.PropertyEntity
import com.example.data.ProviderEntity
import com.example.data.StoreEntity
import com.example.ui.screens.map.components.MapBottomSheet
import com.example.ui.screens.map.components.MapControls
import com.example.data.AdminSettingsEntity
import com.example.utils.VisualThemePalette
import com.example.utils.resolveThemePalette
import org.json.JSONArray
import org.json.JSONObject

/**
 * 🗺️ RealLeafletMapView
 * Real-world interactive Leaflet GIS map with OpenStreetMap & Google Maps tile layers.
 * Supports smooth pan/zoom, live city centering (Sana'a, Taiz, Aden, Ibb, etc.),
 * dynamic pins for providers, stores, properties, and click-to-open detail sheets.
 */
@Composable
fun RealLeafletMapView(
    userCoords: Pair<Double, Double>,
    nearbyProviders: List<ProviderEntity>,
    nearbyStores: List<StoreEntity>,
    nearbyProperties: List<PropertyEntity>,
    dynamicOffsets: Map<String, Pair<Double, Double>>,
    selectedCity: String = "الكل",
    zoomScale: Float = 1.0f,
    onZoomScaleChange: (Float) -> Unit = {},
    panOffset: Offset = Offset.Zero,
    onPanOffsetChange: (Offset) -> Unit = {},
    selectedEntity: Any? = null,
    onProviderSelected: (ProviderEntity) -> Unit,
    onStoreSelected: (StoreEntity) -> Unit,
    onPropertySelected: (PropertyEntity) -> Unit,
    onDeselect: () -> Unit = {},
    onSwitchToRadar: (() -> Unit)? = null,
    themeColors: VisualThemePalette = resolveThemePalette(AdminSettingsEntity()),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var currentSelectedEntity by remember { mutableStateOf<Any?>(selectedEntity) }
    var currentZoom by remember { mutableStateOf(14) }

    // Safe default user coordinates
    val safeUserLat = if (userCoords.first != 0.0) userCoords.first else 15.3694
    val safeUserLng = if (userCoords.second != 0.0) userCoords.second else 44.1910

    // Determine target center coordinates based on selected governorate
    val (targetLat, targetLng) = remember(selectedCity, safeUserLat, safeUserLng) {
        when {
            selectedCity.contains("تعز") -> Pair(13.5789, 44.0195)
            selectedCity.contains("عدن") -> Pair(12.7855, 45.0186)
            selectedCity.contains("إب") -> Pair(13.9667, 44.1833)
            selectedCity.contains("الحديدة") -> Pair(14.7978, 42.9545)
            selectedCity.contains("حضرموت") || selectedCity.contains("المكلا") -> Pair(14.5425, 49.1242)
            selectedCity.contains("ذمار") -> Pair(14.5427, 44.4051)
            selectedCity.contains("مأرب") -> Pair(15.4628, 45.3258)
            else -> Pair(safeUserLat, safeUserLng)
        }
    }

    // Serialize providers, stores, and properties into JSON array for Leaflet markers
    val markersJsonArray = remember(nearbyProviders, nearbyStores, nearbyProperties, targetLat, targetLng) {
        val jsonArray = JSONArray()

        nearbyProviders.forEachIndexed { index, provider ->
            val lat = provider.latitude.takeIf { it != 0.0 } ?: (targetLat + (index % 5 - 2) * 0.012)
            val lng = provider.longitude.takeIf { it != 0.0 } ?: (targetLng + (index % 4 - 2) * 0.012)
            val specText = provider.customCategoryName.ifEmpty { provider.specialization.ifEmpty { provider.profession.ifEmpty { "فني صيانة" } } }
            val obj = JSONObject().apply {
                put("type", "PROVIDER")
                put("id", provider.id)
                put("name", provider.name)
                put("lat", lat)
                put("lng", lng)
                put("spec", specText)
                put("badgeColor", "#00E5FF")
                put("emoji", "🔧")
                put("rating", provider.rating.toString())
                put("status", if (provider.isAvailable) "متوفر الآن" else "مشغول")
                put("phone", provider.phone)
                put("serviceCategory", "فني معتمد")
            }
            jsonArray.put(obj)
        }

        nearbyStores.forEachIndexed { index, store ->
            val lat = store.latitude.takeIf { it != 0.0 } ?: (targetLat + (index % 4 - 1) * 0.015)
            val lng = store.longitude.takeIf { it != 0.0 } ?: (targetLng + (index % 3 - 1) * 0.015)
            val specText = store.description.ifEmpty { store.workingHours }
            val obj = JSONObject().apply {
                put("type", "STORE")
                put("id", store.id)
                put("name", store.name)
                put("lat", lat)
                put("lng", lng)
                put("spec", specText)
                put("badgeColor", "#10B981")
                put("emoji", "🛒")
                put("rating", store.rating.toString())
                put("status", "مفتوح")
                put("phone", store.phone)
                put("serviceCategory", "متجر قطع غيار")
            }
            jsonArray.put(obj)
        }

        nearbyProperties.forEachIndexed { index, prop ->
            val lat = prop.latitude.takeIf { it != 0.0 } ?: (targetLat + (index % 3 - 1) * 0.018)
            val lng = prop.longitude.takeIf { it != 0.0 } ?: (targetLng + (index % 4 - 2) * 0.018)
            val specText = "${prop.type} - ${prop.price} ${prop.currency}"
            val obj = JSONObject().apply {
                put("type", "PROPERTY")
                put("id", prop.id)
                put("name", prop.title)
                put("lat", lat)
                put("lng", lng)
                put("spec", specText)
                put("badgeColor", "#F59E0B")
                put("emoji", "🏢")
                put("rating", prop.rating.toString())
                put("status", "متاح للايجار/البيع")
                put("phone", prop.phone)
                put("serviceCategory", "عقار/مكتب")
            }
            jsonArray.put(obj)
        }

        jsonArray.toString()
    }

    // Push updates to Leaflet map whenever center or markers change
    LaunchedEffect(targetLat, targetLng, markersJsonArray, webViewInstance) {
        webViewInstance?.let { webView ->
            val updateScript = """
                if (window.updateMapCenter) { window.updateMapCenter($targetLat, $targetLng); }
                if (window.updateMapMarkers) { window.updateMapMarkers($markersJsonArray); }
            """.trimIndent()
            webView.evaluateJavascript(updateScript, null)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // 1. High-Performance Leaflet OpenStreetMap WebView
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
                    setBackgroundColor(android.graphics.Color.parseColor("#0F172A"))
                    
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        allowFileAccess = true
                        allowContentAccess = true
                        allowFileAccessFromFileURLs = true
                        allowUniversalAccessFromFileURLs = true
                        javaScriptCanOpenWindowsAutomatically = true
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        cacheMode = WebSettings.LOAD_DEFAULT
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        userAgentString = "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                            return true
                        }
                    }

                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            view?.evaluateJavascript("""
                                if (window.updateMapCenter) { window.updateMapCenter($targetLat, $targetLng); }
                                if (window.updateMapMarkers) { window.updateMapMarkers($markersJsonArray); }
                            """.trimIndent(), null)
                        }

                        override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                            super.onReceivedError(view, request, error)
                        }
                    }

                    addJavascriptInterface(object {
                        @android.webkit.JavascriptInterface
                        fun onMarkerClicked(type: String, id: String) {
                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                when (type) {
                                    "PROVIDER" -> nearbyProviders.find { it.id == id }?.let {
                                        currentSelectedEntity = it
                                        onProviderSelected(it)
                                    }
                                    "STORE" -> nearbyStores.find { it.id == id }?.let {
                                        currentSelectedEntity = it
                                        onStoreSelected(it)
                                    }
                                    "PROPERTY" -> nearbyProperties.find { it.id == id }?.let {
                                        currentSelectedEntity = it
                                        onPropertySelected(it)
                                    }
                                }
                            }
                        }

                        @android.webkit.JavascriptInterface
                        fun onMapReady() {
                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                evaluateJavascript("""
                                    if (window.updateMapCenter) { window.updateMapCenter($targetLat, $targetLng); }
                                    if (window.updateMapMarkers) { window.updateMapMarkers($markersJsonArray); }
                                """.trimIndent(), null)
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
                                    if (mapIntent.resolveActivity(context.packageManager) != null) {
                                        context.startActivity(mapIntent)
                                    } else {
                                        val browserUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$lat,$lng")
                                        val browserIntent = Intent(Intent.ACTION_VIEW, browserUri)
                                        context.startActivity(Intent.createChooser(browserIntent, "فتح الاتجاهات"))
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }

                        @android.webkit.JavascriptInterface
                        fun getUserLat(): Double = safeUserLat

                        @android.webkit.JavascriptInterface
                        fun getUserLng(): Double = safeUserLng

                        @android.webkit.JavascriptInterface
                        fun getMarkersJson(): String = markersJsonArray
                    }, "AndroidBridge")

                    loadUrl("file:///android_asset/map.html")
                    webViewInstance = this
                }
            },
            update = { webView ->
                webViewInstance = webView
            }
        )

        // 2. Map Floating Action Controls (Zoom In, Zoom Out, Recenter Location, Switch to Radar)
        MapControls(
            isRadarMode = false,
            onToggleRadarMode = { onSwitchToRadar?.invoke() },
            isHeatmapActive = false,
            onToggleHeatmap = {},
            onZoomIn = {
                currentZoom = (currentZoom + 1).coerceAtMost(19)
                webViewInstance?.evaluateJavascript("if (map) { map.setZoom($currentZoom); }", null)
            },
            onZoomOut = {
                currentZoom = (currentZoom - 1).coerceAtLeast(6)
                webViewInstance?.evaluateJavascript("if (map) { map.setZoom($currentZoom); }", null)
            },
            onRecenterLocation = {
                webViewInstance?.evaluateJavascript("if (window.updateMapCenter) { window.updateMapCenter($targetLat, $targetLng); }", null)
            },
            isGpsActive = true,
            themeColors = themeColors,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(top = 100.dp)
        )

        // 3. Detail Bottom Sheet when tapping any pin
        currentSelectedEntity?.let { entity ->
            MapBottomSheet(
                entity = entity,
                userLat = safeUserLat,
                userLng = safeUserLng,
                onDismiss = {
                    currentSelectedEntity = null
                    onDeselect()
                },
                onRequestBooking = { selectedItem ->
                    when (selectedItem) {
                        is ProviderEntity -> onProviderSelected(selectedItem)
                        is StoreEntity -> onStoreSelected(selectedItem)
                        is PropertyEntity -> onPropertySelected(selectedItem)
                    }
                },
                onOpenDetails = { selectedItem ->
                    when (selectedItem) {
                        is ProviderEntity -> onProviderSelected(selectedItem)
                        is StoreEntity -> onStoreSelected(selectedItem)
                        is PropertyEntity -> onPropertySelected(selectedItem)
                    }
                },
                themeColors = themeColors,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            )
        }
    }
}
