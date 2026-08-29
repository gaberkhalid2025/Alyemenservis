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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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

/**
 * 🗺️ RealLeafletMapView
 * Production-ready OpenStreetMap Leaflet implementation cleanly modularized.
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

    val safeUserLat = if (userCoords.first != 0.0) userCoords.first else 15.3694
    val safeUserLng = if (userCoords.second != 0.0) userCoords.second else 44.1910

    LaunchedEffect(Unit) {
        OfflineMapManager.purgeCacheIfNeeded(context)
    }

    val markersJsonArray = remember(nearbyProviders, nearbyStores, nearbyProperties, dynamicOffsets, safeUserLat, safeUserLng) {
        MapJsonBuilder.buildMarkersJsonArray(
            nearbyProviders = nearbyProviders,
            nearbyStores = nearbyStores,
            nearbyProperties = nearbyProperties,
            dynamicOffsets = dynamicOffsets,
            safeUserLat = safeUserLat,
            safeUserLng = safeUserLng
        )
    }

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

                        override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                            super.onReceivedError(view, request, error)
                            if (request?.isForMainFrame == true) {
                                hasLoadError = true
                                isMapLoading = false
                            }
                        }

                        override fun onRenderProcessGone(view: WebView?, detail: android.webkit.RenderProcessGoneDetail?): Boolean = true
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
                            android.os.Handler(android.os.Looper.getMainLooper()).post { isMapLoading = false }
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
                                    Log.e("RealLeafletMapView", "Navigation error: ${e.message}")
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

        if (hasLoadError) {
            MapErrorOverlay(
                onSwitchToRadar = onSwitchToRadar,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}
