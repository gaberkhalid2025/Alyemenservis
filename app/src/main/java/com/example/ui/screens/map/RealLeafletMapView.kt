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
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
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
 * - Smooth custom Shimmer Skeleton loader during map initialization
 * - Lifecycle-aware onResume/onPause handling to save CPU and RAM resources
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

    // Lifecycle-aware WebView observer to pause timers & rendering when screen backgrounded
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, webViewRef) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    webViewRef?.onResume()
                    webViewRef?.resumeTimers()
                }
                Lifecycle.Event.ON_PAUSE -> {
                    webViewRef?.onPause()
                    webViewRef?.pauseTimers()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val htmlContent = """
        <!DOCTYPE html>
        <html lang="ar" dir="rtl">
        <head>
            <meta charset="utf-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
            <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
            <link rel="stylesheet" href="https://unpkg.com/leaflet.markercluster@1.5.3/dist/MarkerCluster.css" />
            <link rel="stylesheet" href="https://unpkg.com/leaflet.markercluster@1.5.3/dist/MarkerCluster.Default.css" />
            <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
            <script src="https://unpkg.com/leaflet.markercluster@1.5.3/dist/leaflet.markercluster.js"></script>
            <style>
                * { box-sizing: border-box; margin: 0; padding: 0; }
                html, body, #map { height: 100%; width: 100%; background: #0B0F19; font-family: system-ui, -apple-system, sans-serif; overflow: hidden; }
                .leaflet-tile-container img { filter: brightness(0.88) contrast(1.08); }
                .leaflet-popup-content-wrapper { background: #1E293B !important; color: #FFFFFF !important; border: 2px solid #00E5FF; border-radius: 16px; padding: 4px; box-shadow: 0 12px 30px rgba(0,0,0,0.8); }
                .leaflet-popup-tip { background: #1E293B !important; }
                .popup-card { font-family: system-ui, -apple-system, sans-serif; text-align: right; direction: rtl; color: #FFFFFF; padding: 8px 6px; min-width: 210px; }
                .popup-cat-badge { display: inline-block; font-size: 10px; padding: 2px 8px; border-radius: 12px; background: rgba(255,255,255,0.1); margin-bottom: 4px; font-weight: 600; }
                .popup-approx-badge { display: inline-block; font-size: 9.5px; padding: 1px 6px; border-radius: 8px; background: rgba(245,158,11,0.2); color: #F59E0B; margin-bottom: 4px; }
                .popup-title { font-size: 15px; font-weight: 800; color: #00E5FF; margin-bottom: 4px; line-height: 1.3; }
                .popup-spec { font-size: 12px; color: #CBD5E1; margin-bottom: 6px; }
                .popup-meta { display: flex; flex-direction: column; gap: 3px; font-size: 11px; margin-bottom: 10px; color: #94A3B8; border-top: 1px solid rgba(255,255,255,0.1); border-bottom: 1px solid rgba(255,255,255,0.1); padding: 6px 0; }
                .popup-meta-row { display: flex; justify-content: space-between; }
                .popup-eta { color: #38BDF8; font-weight: bold; }
                .popup-buttons { display: flex; flex-direction: column; gap: 6px; }
                .popup-buttons-row { display: flex; gap: 6px; }
                .btn-details { width: 100%; background: linear-gradient(135deg, #00E5FF, #00B4D8); color: #0F172A; border: none; padding: 9px 10px; border-radius: 10px; font-weight: bold; font-size: 12px; cursor: pointer; display: flex; align-items: center; justify-content: center; gap: 4px; }
                .btn-call { flex: 1; background: #10B981; color: #FFFFFF; border: none; padding: 8px 6px; border-radius: 10px; font-weight: bold; font-size: 12px; cursor: pointer; text-align: center; text-decoration: none; display: inline-block; }
                .btn-nav { flex: 1; background: #3B82F6; color: #FFFFFF; border: none; padding: 8px 6px; border-radius: 10px; font-weight: bold; font-size: 12px; cursor: pointer; text-align: center; }
                .custom-pin { width: 44px; height: 44px; background: #1E293B; border-width: 3px; border-style: solid; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 22px; box-shadow: 0 4px 15px rgba(0,0,0,0.6); transition: transform 0.2s cubic-bezier(0.175, 0.885, 0.32, 1.275); }
                .custom-pin:active, .custom-pin:hover { transform: scale(1.22); }
                .user-marker-container { position: relative; width: 32px; height: 32px; }
                .user-marker { width: 22px; height: 22px; background: #00E5FF; border: 3px solid #FFFFFF; border-radius: 50%; box-shadow: 0 0 20px #00E5FF; position: absolute; top: 5px; left: 5px; z-index: 2; }
                .user-pulse { position: absolute; width: 46px; height: 46px; background: rgba(0, 229, 255, 0.4); border-radius: 50%; top: -7px; left: -7px; animation: pulseRing 2s infinite ease-out; }
                @keyframes pulseRing { 0% { transform: scale(0.4); opacity: 1; } 100% { transform: scale(1.8); opacity: 0; } }
                .marker-cluster-small { background-color: rgba(0, 229, 255, 0.4); }
                .marker-cluster-small div { background-color: #00E5FF; color: #0F172A; font-weight: 800; }
                .marker-cluster-medium { background-color: rgba(245, 158, 11, 0.4); }
                .marker-cluster-medium div { background-color: #F59E0B; color: #0F172A; font-weight: 800; }
                .marker-cluster-large { background-color: rgba(236, 72, 153, 0.4); }
                .marker-cluster-large div { background-color: #EC4899; color: #FFFFFF; font-weight: 800; }
            </style>
        </head>
        <body>
            <div id="map"></div>
            <script>
                var userLat = ${userCoords.first};
                var userLng = ${userCoords.second};
                var map = null;
                var clusterGroup = null;

                function calculateDistKm(lat1, lon1, lat2, lon2) {
                    var R = 6371;
                    var dLat = (lat2-lat1) * Math.PI / 180;
                    var dLon = (lon2-lon1) * Math.PI / 180;
                    var a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                            Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
                            Math.sin(dLon/2) * Math.sin(dLon/2);
                    var c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
                    return R * c;
                }

                function formatDistance(distKm) {
                    if (distKm < 1.0) {
                        return Math.round(distKm * 1000) + " م";
                    } else {
                        return distKm.toFixed(1) + " كم";
                    }
                }

                function getEta(distKm) {
                    if (distKm < 1.0) {
                        return Math.max(1, Math.round((distKm / 5.0) * 60)) + " دقيقة سيراً 🏃";
                    } else {
                        return Math.max(1, Math.round((distKm / 35.0) * 60)) + " دقيقة بالسيارة 🚗";
                    }
                }

                function initMap() {
                    map = L.map('map', {
                        zoomControl: false,
                        attributionControl: false
                    }).setView([userLat, userLng], 14);

                    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                        maxZoom: 19
                    }).addTo(map);

                    clusterGroup = L.markerClusterGroup({
                        maxClusterRadius: 40,
                        showCoverageOnHover: false
                    });
                    map.addLayer(clusterGroup);

                    var userIcon = L.divIcon({
                        className: 'user-marker-container',
                        html: '<div class="user-pulse"></div><div class="user-marker"></div>',
                        iconSize: [32, 32],
                        iconAnchor: [16, 16]
                    });
                    L.marker([userLat, userLng], { icon: userIcon }).addTo(map);

                    window.updateMapMarkers([$markersJson]);

                    if (window.AndroidBridge && window.AndroidBridge.onMapReady) {
                        window.AndroidBridge.onMapReady();
                    }
                }

                window.updateMapMarkers = function(rawMarkers) {
                    if (!map || !clusterGroup) return;
                    clusterGroup.clearLayers();

                    rawMarkers.forEach(function(item) {
                        var dist = calculateDistKm(userLat, userLng, item.lat, item.lng);
                        var distStr = formatDistance(dist);
                        var etaStr = getEta(dist);

                        var pinIcon = L.divIcon({
                            className: 'custom-pin-container',
                            html: '<div class="custom-pin" style="border-color: ' + item.badgeColor + ';">' + item.emoji + '</div>',
                            iconSize: [44, 44],
                            iconAnchor: [22, 22]
                        });

                        var popupHtml = '<div class="popup-card">' +
                            '<div class="popup-cat-badge" style="color: ' + item.badgeColor + '; border-color: ' + item.badgeColor + '; border: 1px solid;">' + item.serviceCategory + '</div>' +
                            (item.isApprox ? '<br/><div class="popup-approx-badge">📍 موقع تقريبي لتسهيل الوصول</div>' : '') +
                            '<div class="popup-title">' + item.name + '</div>' +
                            '<div class="popup-spec">' + item.spec + '</div>' +
                            '<div class="popup-meta">' +
                                '<div class="popup-meta-row"><span>التقييم:</span><span>⭐ ' + item.rating + '</span></div>' +
                                '<div class="popup-meta-row"><span>المسافة:</span><span class="popup-eta">' + distStr + '</span></div>' +
                                '<div class="popup-meta-row"><span>الوقت المتوقع:</span><span class="popup-eta">' + etaStr + '</span></div>' +
                                '<div class="popup-meta-row"><span>الحالة:</span><span>' + item.status + '</span></div>' +
                            '</div>' +
                            '<div class="popup-buttons">' +
                                '<button class="btn-details" onclick="AndroidBridge.onMarkerClicked(\'' + item.type + '\', \'' + item.id + '\')">📱 تفاصيل الخدمة والطلب</button>' +
                                '<div class="popup-buttons-row">' +
                                    '<a class="btn-call" href="tel:' + item.phone + '">📞 اتصال</a>' +
                                    '<button class="btn-nav" onclick="AndroidBridge.openNavigation(' + item.lat + ',' + item.lng + ', \'' + item.name.replace(/'/g, "\\'") + '\')">🗺️ وجهني</button>' +
                                '</div>' +
                            '</div>' +
                        '</div>';

                        var marker = L.marker([item.lat, item.lng], { icon: pinIcon }).bindPopup(popupHtml, {
                            maxWidth: 260,
                            minWidth: 220,
                            closeButton: false
                        });
                        clusterGroup.addLayer(marker);
                    });
                };

                window.onload = function() {
                    try {
                        initMap();
                    } catch(e) {
                        if (window.AndroidBridge && window.AndroidBridge.onMapLoadFailed) {
                            window.AndroidBridge.onMapLoadFailed();
                        }
                    }
                };
            </script>
        </body>
        </html>
    """.trimIndent()

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    webViewRef = this
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        cacheMode = WebSettings.LOAD_DEFAULT
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

        // Custom High-Quality Shimmer Loading Skeleton
        AnimatedVisibility(
            visible = isMapLoading,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            MapLoadingSkeleton()
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

/**
 * 🎨 Shimmer brush builder
 */
@Composable
fun ShimmerBrush(showShimmer: Boolean = true, targetValue: Float = 1000f): Brush {
    return if (showShimmer) {
        val transition = rememberInfiniteTransition(label = "shimmer_trans")
        val translateAnimation by transition.animateFloat(
            initialValue = 0f,
            targetValue = targetValue,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "shimmer_anim"
        )
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF0F172A),
                Color(0xFF1E293B),
                Color(0xFF0F172A)
            ),
            start = Offset.Zero,
            end = Offset(x = translateAnimation, y = translateAnimation)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(Color(0xFF0F172A), Color(0xFF0F172A)),
            start = Offset.Zero,
            end = Offset.Zero
        )
    }
}

/**
 * 🗺️ Map Loading Skeleton
 * Animated, rich visual representation of the map dashboard loading
 */
@Composable
fun MapLoadingSkeleton() {
    val brush = ShimmerBrush()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF020617))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Bar Skeleton
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(brush)
            )

            // Main Map Skeleton with visual ripples representing radar/mapping
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(brush),
                contentAlignment = Alignment.Center
            ) {
                // Interactive Radar Scanning Ripple inside skeleton
                val transition = rememberInfiniteTransition(label = "radar_skeleton_trans")
                val pulseScale by transition.animateFloat(
                    initialValue = 0.2f,
                    targetValue = 1.0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1500, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "radar_pulse_scale"
                )
                val pulseAlpha by transition.animateFloat(
                    initialValue = 0.6f,
                    targetValue = 0.0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1500, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "radar_pulse_alpha"
                )

                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF00E5FF).copy(alpha = pulseAlpha),
                                    Color.Transparent
                                )
                            )
                        )
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = Color(0xFF00E5FF),
                        strokeWidth = 3.dp
                    )
                    Text(
                        "تحميل دليل اليمن التفاعلي...",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Bottom Items Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                repeat(2) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(14.dp))
                            .background(brush)
                    )
                }
            }
        }
    }
}
