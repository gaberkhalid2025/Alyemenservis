package com.example.ui.screens.map

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.AdminSettingsEntity
import com.example.data.PropertyEntity
import com.example.data.ProviderEntity
import com.example.data.StoreEntity
import com.example.ui.screens.map.components.OfflineInteractiveMap
import com.example.ui.screens.map.utils.MapJsonBuilder
import com.example.ui.screens.map.utils.OfflineMapManager
import com.example.ui.screens.map.utils.OfflineTileInterceptor
import com.example.utils.VisualThemePalette
import com.example.utils.resolveThemePalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class MapZone(val lat: Double, val lng: Double, val radiusM: Int, val colorHex: String)

/**
 * 🗺️ RealLeafletMapView (Vela 3D + OpenAerialMap Satellite Engine)
 * - تحميل خريطة الأقمار الاصطناعية الافتراضية عالية الدقة بدون أي مفتاح API
 * - دعم وضع Vela 3D التفاعلي مع المباني ثلاثية الأبعاد وإمالة الكاميرا
 * - دعم كامل للمس المتعدد والتكبير والتصغير السلس (Pinch / Multi-Touch Zoom)
 * - ربط العلامات الملونة حسب نوع الكيان مع Bottom Sheet تفاعلي
 * - مؤشر نابض أزرق لموقع المستخدم الفعلي
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun RealLeafletMapView(
    userCoords: Pair<Double, Double>,
    nearbyProviders: List<ProviderEntity>,
    nearbyStores: List<StoreEntity>,
    nearbyProperties: List<PropertyEntity>,
    dynamicOffsets: Map<String, Pair<Double, Double>>,
    currentMapMode: MapMode = MapMode.SATELLITE,
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
    onMapLoadFailed: (() -> Unit)? = null,
    onTilesStatus: ((failing: Boolean) -> Unit)? = null,
    reloadTilesKey: Int = 0,
    tilt: Float = 35f,
    rotation: Float = 0f,
    routeCoordinates: List<Pair<Double, Double>> = emptyList(),
    routeKey: Int = 0,
    waypointMarkers: List<Triple<String, Double, Double>> = emptyList(),
    followUser: Boolean = false,
    onUserPanned: (() -> Unit)? = null,
    focusPoint: Triple<String, Double, Double>? = null,
    pickMode: Boolean = false,
    onMapPicked: ((lat: Double, lng: Double) -> Unit)? = null,
    onMapLongPressed: ((lat: Double, lng: Double) -> Unit)? = null,
    onMapCenterChanged: ((lat: Double, lng: Double) -> Unit)? = null,
    ecoMode: Boolean = false,
    hybridLabels: Boolean = false,
    nightMode: Boolean = false,
    gibsEnabled: Boolean = false,
    gibsProduct: String = "truecolor",
    onGibsStatus: ((Boolean) -> Unit)? = null,
    zoomInTick: Int = 0,
    zoomOutTick: Int = 0,
    useZoomTicks: Boolean = false,
    navZoom: Int? = null,
    zones: List<MapZone> = emptyList(),
    eoxEnabled: Boolean = false,
    quakesJson: String? = null,
    rainUrl: String? = null,
    onOverlayStatus: ((name: String, failing: Boolean, msg: String) -> Unit)? = null,
    themeColors: VisualThemePalette = resolveThemePalette(AdminSettingsEntity()),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isMapReady by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var hasLoadError by remember { mutableStateOf(false) }
    var didInitialCenter by remember { mutableStateOf(false) }

    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val providersState by rememberUpdatedState(nearbyProviders)
    val storesState by rememberUpdatedState(nearbyStores)
    val propertiesState by rememberUpdatedState(nearbyProperties)
    val onProviderSelectedState by rememberUpdatedState(onProviderSelected)
    val onStoreSelectedState by rememberUpdatedState(onStoreSelected)
    val onPropertySelectedState by rememberUpdatedState(onPropertySelected)
    val onDeselectState by rememberUpdatedState(onDeselect)
    val onUserPannedState by rememberUpdatedState(onUserPanned)
    val onTilesStatusState by rememberUpdatedState(onTilesStatus)
    val onMapPickedState by rememberUpdatedState(onMapPicked)
    val onMapLongPressedState by rememberUpdatedState(onMapLongPressed)
    val onMapCenterChangedState by rememberUpdatedState(onMapCenterChanged)
    val onGibsStatusState by rememberUpdatedState(onGibsStatus)
    val onOverlayStatusState by rememberUpdatedState(onOverlayStatus)

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, webViewRef) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> {
                    webViewRef?.onPause()
                    webViewRef?.pauseTimers()
                }
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> {
                    webViewRef?.onResume()
                    webViewRef?.resumeTimers()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // [المحور الثالث] حماية الشاشة السوداء: مؤقت 6 ثوانٍ. إذا لم تصل إشارة onMapReady يتم التحويل تلقائياً لـ OfflineInteractiveMap
    LaunchedEffect(isMapReady) {
        if (!isMapReady) {
            kotlinx.coroutines.delay(6000L)
            if (!isMapReady) {
                hasLoadError = true
                onMapLoadFailed?.invoke()
            }
        }
    }

    // تحويل البيانات إلى JSON مهيأ للبلاطات والعلامات على خيط الخلفية لمنع أي تقطيع أثناء التمرير والبحث
    val offsetsSnapshot = dynamicOffsets.toMap()
    val markersJson by produceState(
        initialValue = "",
        nearbyProviders, nearbyStores, nearbyProperties, selectedCity, offsetsSnapshot
    ) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            MapJsonBuilder.buildMarkersJson(
                nearbyProviders = nearbyProviders.take(MapJsonBuilder.MAX_PER_TYPE),
                nearbyStores = nearbyStores.take(MapJsonBuilder.MAX_PER_TYPE),
                nearbyProperties = nearbyProperties.take(MapJsonBuilder.MAX_PER_TYPE),
                dynamicOffsets = offsetsSnapshot,
                safeUserLat = userCoords.first,
                safeUserLng = userCoords.second
            )
        }
    }

    // إرسال العلامات للخريطة عند اكتمال الجاهزية أو تغير البيانات بأمان تام عبر Base64
    LaunchedEffect(isMapReady, markersJson) {
        if (isMapReady && webViewRef != null && markersJson.isNotBlank()) {
            val b64 = Base64.encodeToString(markersJson.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            val script = "if (window.setMarkersBase64) { window.setMarkersBase64('$b64'); }"
            webViewRef?.evaluateJavascript(script, null)
        }
    }

    // تبديل أوضاع الخريطة
    LaunchedEffect(isMapReady, currentMapMode) {
        if (isMapReady && webViewRef != null) {
            val modeKey = when (currentMapMode) {
                MapMode.VELA_3D -> "VELA_3D"
                MapMode.VELA -> "VELA"
                MapMode.OSM -> "OSM"
                MapMode.HOT -> "HOT"
                MapMode.TOPO -> "TOPO"
                MapMode.SATELLITE -> "SATELLITE"
                MapMode.SATELLITE_CLARITY -> "SATELLITE_CLARITY"
                else -> "SATELLITE"
            }
            val script = "if (window.switchMapMode) { window.switchMapMode('$modeKey'); }"
            webViewRef?.evaluateJavascript(script, null)
        }
    }

    // تحديث موقع المستخدم الفعلي على الخريطة
    LaunchedEffect(isMapReady, userCoords, followUser) {
        if (isMapReady && webViewRef != null && userCoords.first != 0.0 && userCoords.second != 0.0) {
            val pan = followUser || !didInitialCenter
            didInitialCenter = true
            val script = "if (window.updateUserLocation) { window.updateUserLocation(${userCoords.first}, ${userCoords.second}, 30, $pan); }"
            webViewRef?.evaluateJavascript(script, null)
        }
    }

    // تمركز الخريطة عند تغيير المدينة (عدم التحويل لمركز صنعاء عند اختيار "الكل")
    LaunchedEffect(isMapReady, selectedCity) {
        if (!isMapReady || webViewRef == null) return@LaunchedEffect
        if (selectedCity == "الكل") return@LaunchedEffect
        val cityCoords = OfflineMapManager.getCityCoordinates(selectedCity)
        val script = "if (window.setCity) { window.setCity('${cityCoords.nameAr}', ${cityCoords.latitude}, ${cityCoords.longitude}, ${cityCoords.defaultZoom}); }"
        webViewRef?.evaluateJavascript(script, null)
    }

    // التحكم في التكبير والتصغير التفاعلي
    var lastZoomScale by remember { mutableFloatStateOf(zoomScale) }
    LaunchedEffect(zoomScale) {
        if (useZoomTicks) return@LaunchedEffect
        if (isMapReady && webViewRef != null) {
            if (zoomScale > lastZoomScale) {
                webViewRef?.evaluateJavascript("if (window.zoomIn) { window.zoomIn(); }", null)
            } else if (zoomScale < lastZoomScale) {
                webViewRef?.evaluateJavascript("if (window.zoomOut) { window.zoomOut(); }", null)
            }
            lastZoomScale = zoomScale
        }
    }

    LaunchedEffect(zoomInTick) {
        if (isMapReady && webViewRef != null && zoomInTick > 0) {
            webViewRef?.evaluateJavascript("if (window.zoomIn) { window.zoomIn(); }", null)
        }
    }

    LaunchedEffect(zoomOutTick) {
        if (isMapReady && webViewRef != null && zoomOutTick > 0) {
            webViewRef?.evaluateJavascript("if (window.zoomOut) { window.zoomOut(); }", null)
        }
    }

    LaunchedEffect(isMapReady, navZoom, followUser) {
        val z = navZoom
        if (isMapReady && z != null && followUser) {
            webViewRef?.evaluateJavascript("if (window.setNavZoom) { window.setNavZoom($z); }", null)
        }
    }

    LaunchedEffect(isMapReady, zones) {
        if (!isMapReady) return@LaunchedEffect
        val b64 = withContext(Dispatchers.Default) {
            val arr = JSONArray()
            zones.take(400).forEach {
                arr.put(JSONObject().put("lat", it.lat).put("lng", it.lng).put("radiusM", it.radiusM).put("color", it.colorHex))
            }
            Base64.encodeToString(arr.toString().toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        }
        webViewRef?.evaluateJavascript("if (window.setZonesBase64) { window.setZonesBase64('$b64'); }", null)
    }

    // إعادة التمركز إلى موقع المستخدم
    LaunchedEffect(panOffset) {
        if (isMapReady && webViewRef != null && panOffset == Offset.Zero) {
            webViewRef?.evaluateJavascript("if (window.recenterUser) { window.recenterUser(); }", null)
        }
    }

    // إعادة تحميل البلاطات الأساسية عند طلب المستخدم
    LaunchedEffect(reloadTilesKey) {
        if (isMapReady && webViewRef != null && reloadTilesKey > 0) {
            webViewRef?.evaluateJavascript("if (window.reloadBaseLayer) { window.reloadBaseLayer(); }", null)
        }
    }

    // رسم ومسح مسار الملاحة على خريطة Leaflet
    LaunchedEffect(isMapReady, routeCoordinates, routeKey) {
        if (!isMapReady || webViewRef == null) return@LaunchedEffect
        if (routeCoordinates.size < 2) {
            webViewRef?.evaluateJavascript("if (window.clearRoute) { window.clearRoute(); }", null)
            return@LaunchedEffect
        }
        val b64 = withContext(Dispatchers.Default) {
            val step = maxOf(1, routeCoordinates.size / 2000)
            val arr = JSONArray()
            var i = 0
            while (i < routeCoordinates.size) {
                arr.put(JSONArray().put(routeCoordinates[i].first).put(routeCoordinates[i].second))
                i += step
            }
            val last = routeCoordinates.lastOrNull() ?: return@withContext ""
            arr.put(JSONArray().put(last.first).put(last.second))
            Base64.encodeToString(arr.toString().toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        }
        if (b64.isNotBlank()) {
            webViewRef?.evaluateJavascript("if (window.setRouteBase64) { window.setRouteBase64('$b64', ${!followUser}); }", null)
        }
    }

    // محطات المسار المرقمة
    LaunchedEffect(isMapReady, waypointMarkers) {
        if (!isMapReady || webViewRef == null) return@LaunchedEffect
        val arr = JSONArray()
        waypointMarkers.forEach { (label, lat, lng) ->
            arr.put(JSONObject().put("label", label).put("lat", lat).put("lng", lng))
        }
        val b64 = Base64.encodeToString(arr.toString().toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        webViewRef?.evaluateJavascript("if (window.setWaypointMarkersBase64) { window.setWaypointMarkersBase64('$b64'); }", null)
    }

    // دبوس نتيجة البحث
    LaunchedEffect(isMapReady, focusPoint) {
        if (!isMapReady || webViewRef == null) return@LaunchedEffect
        if (focusPoint == null) {
            webViewRef?.evaluateJavascript("if (window.clearSearchPin) { window.clearSearchPin(); }", null)
        } else {
            val o = JSONObject().put("name", focusPoint.first).put("lat", focusPoint.second).put("lng", focusPoint.third)
            val b64 = Base64.encodeToString(o.toString().toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            webViewRef?.evaluateJavascript("if (window.showSearchPinBase64) { window.showSearchPinBase64('$b64'); }", null)
        }
    }

    LaunchedEffect(isMapReady, pickMode) {
        if (isMapReady && webViewRef != null) {
            webViewRef?.evaluateJavascript("if (window.setPickMode) { window.setPickMode($pickMode); }", null)
        }
    }

    LaunchedEffect(isMapReady, ecoMode) {
        if (isMapReady && webViewRef != null) {
            webViewRef?.evaluateJavascript("if (window.setEcoMode) { window.setEcoMode($ecoMode); }", null)
        }
    }

    LaunchedEffect(isMapReady, hybridLabels, currentMapMode) {
        if (isMapReady && webViewRef != null) {
            val on = hybridLabels && (currentMapMode == MapMode.SATELLITE || currentMapMode == MapMode.SATELLITE_CLARITY || currentMapMode == MapMode.VELA_3D)
            webViewRef?.evaluateJavascript("if (window.setHybridLabels) { window.setHybridLabels($on); }", null)
        }
    }

    LaunchedEffect(isMapReady, nightMode) {
        if (isMapReady && webViewRef != null) {
            webViewRef?.evaluateJavascript("if (window.setNightMode) { window.setNightMode($nightMode); }", null)
        }
    }

    LaunchedEffect(isMapReady, gibsEnabled, gibsProduct) {
        if (isMapReady && webViewRef != null) {
            val key = gibsProduct.replace("'", "").replace("\"", "")
            webViewRef?.evaluateJavascript("if (window.setGibs) { window.setGibs($gibsEnabled, '$key'); }", null)
        }
    }

    LaunchedEffect(isMapReady, eoxEnabled) {
        if (isMapReady && webViewRef != null) {
            webViewRef?.evaluateJavascript("if (window.setEox) { window.setEox($eoxEnabled); }", null)
        }
    }

    LaunchedEffect(isMapReady, quakesJson) {
        if (isMapReady && webViewRef != null) {
            val json = quakesJson
            if (json.isNullOrBlank()) {
                webViewRef?.evaluateJavascript("if (window.setQuakesBase64) { window.setQuakesBase64(''); }", null)
            } else {
                val b64 = Base64.encodeToString(json.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
                webViewRef?.evaluateJavascript("if (window.setQuakesBase64) { window.setQuakesBase64('$b64'); }", null)
            }
        }
    }

    LaunchedEffect(isMapReady, rainUrl) {
        if (isMapReady && webViewRef != null) {
            val safeUrl = (rainUrl ?: "").replace("'", "").replace("\"", "").trim()
            webViewRef?.evaluateJavascript("if (window.setRainUrl) { window.setRainUrl('$safeUrl'); }", null)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        if (!hasLoadError) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setBackgroundColor(android.graphics.Color.parseColor("#0F172A"))
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            cacheMode = WebSettings.LOAD_DEFAULT
                            userAgentString = "AlYemenServicesApp/1.0 (Android; OpenStreetMap)"
                            setSupportZoom(true)
                            builtInZoomControls = false
                            displayZoomControls = false
                            loadWithOverviewMode = true
                            useWideViewPort = true
                            allowFileAccess = false
                            allowContentAccess = false
                            javaScriptCanOpenWindowsAutomatically = false
                        }

                        addJavascriptInterface(object {
                            @JavascriptInterface
                            fun onMapReady() {
                                mainHandler.post { isMapReady = true }
                            }

                            @JavascriptInterface
                            fun onMapClicked() {
                                mainHandler.post { onDeselectState() }
                            }

                            @JavascriptInterface
                            fun onUserPanned() {
                                mainHandler.post { onUserPannedState?.invoke() }
                            }

                            @JavascriptInterface
                            fun onMarkerSelected(id: String, type: String) {
                                handleMarkerClick(id, type)
                            }

                            @JavascriptInterface
                            fun onMarkerClicked(id: String, type: String) {
                                handleMarkerClick(id, type)
                            }

                            private fun handleMarkerClick(id: String, type: String) {
                                mainHandler.post {
                                    when (type.uppercase()) {
                                        "PROVIDER" -> {
                                            providersState.find { it.id == id }?.let { onProviderSelectedState(it) }
                                        }
                                        "STORE", "RESTAURANT", "MEDICAL" -> {
                                            storesState.find { it.id == id }?.let { onStoreSelectedState(it) }
                                        }
                                        "PROPERTY" -> {
                                            propertiesState.find { it.id == id }?.let { onPropertySelectedState(it) }
                                        }
                                    }
                                }
                            }

                            @JavascriptInterface
                            fun onTilesFailing(failing: Boolean) {
                                mainHandler.post { onTilesStatusState?.invoke(failing) }
                            }

                            @JavascriptInterface
                            fun onGibsStatus(ok: Boolean) {
                                mainHandler.post { onGibsStatusState?.invoke(ok) }
                            }

                            @JavascriptInterface
                            fun onOverlayStatus(name: String, failing: Boolean, msg: String) {
                                mainHandler.post { onOverlayStatusState?.invoke(name, failing, msg) }
                            }

                            @JavascriptInterface
                            fun onMapPicked(lat: Double, lng: Double) {
                                mainHandler.post { onMapPickedState?.invoke(lat, lng) }
                            }

                            @JavascriptInterface
                            fun onMapLongPressed(lat: Double, lng: Double) {
                                mainHandler.post { onMapLongPressedState?.invoke(lat, lng) }
                            }

                            @JavascriptInterface
                            fun onMapCenterChanged(lat: Double, lng: Double) {
                                mainHandler.post { onMapCenterChangedState?.invoke(lat, lng) }
                            }

                            @JavascriptInterface
                            fun onMapModeChanged(newMode: String) {
                                android.util.Log.d("LeafletBridge", "Map mode: $newMode")
                            }

                            @JavascriptInterface
                            fun getAvailableLayers(): String {
                                return "[\"OSM\",\"HOT\",\"TOPO\",\"VELA\",\"SATELLITE\",\"SATELLITE_CLARITY\",\"VELA_3D\"]"
                            }
                        }, "AndroidBridge")

                        webViewClient = object : WebViewClient() {
                            private val ALLOWED_HOSTS = setOf(
                                "www.openstreetmap.org",
                                "www.hotosm.org",
                                "opentopomap.org",
                                "www.esri.com",
                                "carto.com",
                                "earthdata.nasa.gov",
                                "s2maps.eu",
                                "creativecommons.org",
                                "www.rainviewer.com"
                            )

                            override fun shouldInterceptRequest(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): WebResourceResponse? {
                                val url = request?.url?.toString() ?: return null
                                return try {
                                    OfflineTileInterceptor.intercept(context, url)
                                } catch (_: Throwable) {
                                    null
                                }
                            }

                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                val uri = request?.url ?: return true
                                if (uri.scheme == "file") return false
                                if (uri.scheme == "https" && uri.host in ALLOWED_HOSTS) {
                                    try { context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, uri).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (_: Exception) {}
                                }
                                return true
                            }

                            @Deprecated("Deprecated in Java")
                            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                                if (url == null) return true
                                val uri = android.net.Uri.parse(url)
                                if (uri.scheme == "file") return false
                                if (uri.scheme == "https" && uri.host in ALLOWED_HOSTS) {
                                    try { context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, uri).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (_: Exception) {}
                                }
                                return true
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isMapReady = true
                            }

                            override fun onReceivedError(
                                view: WebView?,
                                errorCode: Int,
                                description: String?,
                                failingUrl: String?
                            ) {
                                super.onReceivedError(view, errorCode, description, failingUrl)
                                if (failingUrl?.contains("map.html") == true) {
                                    hasLoadError = true
                                    onMapLoadFailed?.invoke()
                                }
                            }

                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                error: android.webkit.WebResourceError?
                            ) {
                                super.onReceivedError(view, request, error)
                                if (request?.isForMainFrame == true) {
                                    hasLoadError = true
                                    onMapLoadFailed?.invoke()
                                }
                            }
                        }

                        loadUrl("file:///android_asset/map.html")
                        webViewRef = this
                    }
                },
                update = { webView ->
                    webViewRef = webView
                },
                onRelease = { webView ->
                    webView.stopLoading()
                    webView.removeJavascriptInterface("AndroidBridge")
                    webView.destroy()
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // الخريطة التفاعلية المحلية البديلة في حالة الطوارئ (100% بدون شاشة سوداء)
            OfflineInteractiveMap(
                userCoords = userCoords,
                hasUserLocation = userCoords.first != 0.0 && userCoords.second != 0.0,
                nearbyProviders = nearbyProviders,
                nearbyStores = nearbyStores,
                nearbyProperties = nearbyProperties,
                dynamicOffsets = dynamicOffsets,
                selectedCity = selectedCity,
                zoomScale = zoomScale,
                onZoomScaleChange = onZoomScaleChange,
                panOffset = panOffset,
                onPanOffsetChange = onPanOffsetChange,
                selectedEntity = selectedEntity,
                onProviderSelected = onProviderSelected,
                onStoreSelected = onStoreSelected,
                onPropertySelected = onPropertySelected,
                onDeselect = onDeselect,
                onSwitchToRadar = onSwitchToRadar,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
