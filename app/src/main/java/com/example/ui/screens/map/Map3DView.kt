package com.example.ui.screens.map

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.view.View
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.screens.map.utils.OfflineMapManager
import com.example.ui.screens.map.utils.OfflineTileInterceptor
import org.json.JSONObject

object Map3D {
    /** الزر لا يظهر إلا إن وُجدت ملفات المكتبة في assets. */
    fun isAvailable(context: Context): Boolean = try {
        val names = context.assets.list("")?.toSet().orEmpty()
        "map3d.html" in names && "maplibre-gl.js" in names && "maplibre-gl.css" in names
    } catch (e: Exception) { false }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun Map3DView(
    centerLat: Double,
    centerLng: Double,
    userCoords: Pair<Double, Double>?,
    markersJson: String,
    currentMapMode: MapMode = MapMode.SATELLITE,
    selectedCity: String = "الكل",
    hybridLabels: Boolean = false,
    gibsEnabled: Boolean = false,
    gibsProduct: String = "truecolor",
    eoxEnabled: Boolean = false,
    realTimeSatEnabled: Boolean = false,
    realTimeSatFeed: String = "globalvis",
    offlineOnly: Boolean = false,
    pickMode: Boolean = false,
    focusPoint: Triple<String, Double, Double>? = null,
    zoomInTick: Int = 0,
    zoomOutTick: Int = 0,
    recenterTick: Int = 0,
    routeCoordinates: List<Pair<Double, Double>> = emptyList(),
    navigating: Boolean = false,
    onMarkerClicked: (id: String, type: String) -> Unit,
    onDeselect: () -> Unit = {},
    onUserPanned: (() -> Unit)? = null,
    onMapPicked: ((lat: Double, lng: Double) -> Unit)? = null,
    onMapLongPressed: ((lat: Double, lng: Double) -> Unit)? = null,
    onMapCenterChanged: ((lat: Double, lng: Double) -> Unit)? = null,
    onFallbackCss3D: ((String) -> Unit)? = null,
    onUnavailable: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val markerCb by rememberUpdatedState(onMarkerClicked)
    val deselectCb by rememberUpdatedState(onDeselect)
    val pannedCb by rememberUpdatedState(onUserPanned)
    val pickedCb by rememberUpdatedState(onMapPicked)
    val longPressCb by rememberUpdatedState(onMapLongPressed)
    val centerCb by rememberUpdatedState(onMapCenterChanged)
    val fallbackCb by rememberUpdatedState(onFallbackCss3D)
    val unavailableCb by rememberUpdatedState(onUnavailable)
    val offlineOnlyState by rememberUpdatedState(offlineOnly)
    var webView by remember { mutableStateOf<WebView?>(null) }
    var ready by remember { mutableStateOf(false) }
    var fallbackFired by remember { mutableStateOf(false) }

    val modeKey = when (currentMapMode) {
        MapMode.VELA -> "VELA"
        MapMode.OSM -> "OSM"
        MapMode.HOT -> "HOT"
        MapMode.TOPO -> "TOPO"
        MapMode.SATELLITE_CLARITY -> "SATELLITE_CLARITY"
        else -> "SATELLITE"
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, webView) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> {
                    webView?.onPause()
                    webView?.pauseTimers()
                }
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> {
                    webView?.onResume()
                    webView?.resumeTimers()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // حماية الشاشة السوداء: إذا لم يكتمل تحميل العرض ثلاثي الأبعاد خلال 5 ثوانٍ يتم التحويل تلقائياً لوضع 3D التوافقي
    LaunchedEffect(ready) {
        if (!ready) {
            kotlinx.coroutines.delay(5000L)
            if (!ready && !fallbackFired) {
                fallbackFired = true
                val msg = "تفعيل عرض 3D التوافقي لضمان وضوح الخريطة"
                fallbackCb?.invoke(msg) ?: unavailableCb(msg)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setBackgroundColor(android.graphics.Color.parseColor("#0F172A"))
                    setLayerType(View.LAYER_TYPE_HARDWARE, null)
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        cacheMode = WebSettings.LOAD_DEFAULT
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        allowFileAccess = false
                        allowContentAccess = false
                        userAgentString = "AlYemenServicesApp/1.0 (Android; OpenStreetMap)"
                    }
                    addJavascriptInterface(object {
                        @JavascriptInterface fun onMapReady() { mainHandler.post { ready = true } }
                        @JavascriptInterface fun onMarkerClicked(id: String, type: String) { mainHandler.post { markerCb(id, type) } }
                        @JavascriptInterface fun onMapClicked() { mainHandler.post { deselectCb() } }
                        @JavascriptInterface fun onUserPanned() { mainHandler.post { pannedCb?.invoke() } }
                        @JavascriptInterface fun onMapPicked(lat: Double, lng: Double) { mainHandler.post { pickedCb?.invoke(lat, lng) } }
                        @JavascriptInterface fun onMapLongPressed(lat: Double, lng: Double) { mainHandler.post { longPressCb?.invoke(lat, lng) } }
                        @JavascriptInterface fun onMapCenterChanged(lat: Double, lng: Double) { mainHandler.post { centerCb?.invoke(lat, lng) } }
                        @JavascriptInterface fun onFallbackCss3D(reason: String) {
                            mainHandler.post {
                                if (!fallbackFired) {
                                    fallbackFired = true
                                    fallbackCb?.invoke(reason) ?: unavailableCb(reason)
                                }
                            }
                        }
                        @JavascriptInterface fun onUnavailable(reason: String) {
                            mainHandler.post {
                                if (!fallbackFired) {
                                    fallbackFired = true
                                    unavailableCb(reason)
                                }
                            }
                        }
                    }, "AndroidBridge")
                    webViewClient = object : WebViewClient() {
                        private val allowedLocalAssets = mapOf(
                            "/assets/map3d.html" to ("text/html" to "map3d.html"),
                            "/assets/maplibre-gl.js" to ("application/javascript" to "maplibre-gl.js"),
                            "/assets/maplibre-gl.css" to ("text/css" to "maplibre-gl.css")
                        )

                        override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                            val uri = request?.url ?: return null
                            if (uri.scheme == "https" && uri.host == "appassets.local") {
                                val assetSpec = allowedLocalAssets[uri.path] ?: return null
                                return try {
                                    val headers = mapOf("Access-Control-Allow-Origin" to "*")
                                    WebResourceResponse(
                                        assetSpec.first,
                                        "UTF-8",
                                        200,
                                        "OK",
                                        headers,
                                        context.assets.open(assetSpec.second)
                                    )
                                } catch (_: Throwable) {
                                    null
                                }
                            }
                            val url = uri.toString()
                            return try {
                                OfflineTileInterceptor.intercept(context, url, offlineOnly = offlineOnlyState)
                            } catch (_: Throwable) {
                                null
                            }
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            view?.evaluateJavascript(
                                "if (window.init3D) { window.init3D($centerLat, $centerLng, '$modeKey', $offlineOnlyState); }",
                                null
                            )
                        }
                    }
                    // استخدام نطاق HTTPS محلي معزول لحل قيود WebGL2 والـ Workers على file://
                    loadUrl("https://appassets.local/assets/map3d.html")
                    webView = this
                }
            },
            onRelease = { w -> w.stopLoading(); w.removeJavascriptInterface("AndroidBridge"); w.destroy() }
        )
    }

    LaunchedEffect(ready, modeKey) {
        if (ready) webView?.evaluateJavascript("if (window.setBaseMode3D) { window.setBaseMode3D('$modeKey'); }", null)
    }
    LaunchedEffect(ready, offlineOnly) {
        if (ready) webView?.evaluateJavascript("if (window.setOfflineOnly3D) { window.setOfflineOnly3D($offlineOnly); }", null)
    }
    LaunchedEffect(ready, hybridLabels, currentMapMode) {
        if (ready) {
            val on = hybridLabels && (currentMapMode == MapMode.SATELLITE || currentMapMode == MapMode.SATELLITE_CLARITY)
            webView?.evaluateJavascript("if (window.setHybridLabels3D) { window.setHybridLabels3D($on); }", null)
        }
    }
    LaunchedEffect(ready, eoxEnabled) {
        if (ready) webView?.evaluateJavascript("if (window.setEox3D) { window.setEox3D($eoxEnabled); }", null)
    }
    LaunchedEffect(ready, gibsEnabled, gibsProduct) {
        if (ready) {
            val safeProd = gibsProduct.replace("'", "").replace("\"", "")
            webView?.evaluateJavascript("if (window.setGibs3D) { window.setGibs3D($gibsEnabled, '$safeProd'); }", null)
        }
    }
    LaunchedEffect(ready, realTimeSatEnabled, realTimeSatFeed) {
        if (ready) {
            val safeFeed = realTimeSatFeed.replace("'", "").replace("\"", "")
            webView?.evaluateJavascript("if (window.setRealTimeSat3D) { window.setRealTimeSat3D($realTimeSatEnabled, '$safeFeed'); }", null)
        }
    }
    LaunchedEffect(ready, pickMode) {
        if (ready) webView?.evaluateJavascript("if (window.setPickMode3D) { window.setPickMode3D($pickMode); }", null)
    }
    LaunchedEffect(ready, focusPoint) {
        if (!ready) return@LaunchedEffect
        if (focusPoint == null) {
            webView?.evaluateJavascript("if (window.clearSearchPin) { window.clearSearchPin(); }", null)
        } else {
            val o = JSONObject().put("name", focusPoint.first).put("lat", focusPoint.second).put("lng", focusPoint.third)
            val b64 = Base64.encodeToString(o.toString().toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            webView?.evaluateJavascript("if (window.showSearchPinBase64) { window.showSearchPinBase64('$b64'); }", null)
        }
    }
    LaunchedEffect(ready, selectedCity) {
        if (!ready || selectedCity == "الكل") return@LaunchedEffect
        val city = OfflineMapManager.getCityCoordinates(selectedCity)
        webView?.evaluateJavascript("if (window.setCity3D) { window.setCity3D(${city.latitude}, ${city.longitude}, ${city.defaultZoom}); }", null)
    }
    LaunchedEffect(zoomInTick) {
        if (ready && zoomInTick > 0) webView?.evaluateJavascript("if (window.zoomIn3D) { window.zoomIn3D(); }", null)
    }
    LaunchedEffect(zoomOutTick) {
        if (ready && zoomOutTick > 0) webView?.evaluateJavascript("if (window.zoomOut3D) { window.zoomOut3D(); }", null)
    }
    LaunchedEffect(recenterTick) {
        if (ready && recenterTick > 0 && userCoords != null && userCoords.first != 0.0) {
            webView?.evaluateJavascript("if (window.recenter3D) { window.recenter3D(${userCoords.first}, ${userCoords.second}); }", null)
        }
    }
    LaunchedEffect(ready, markersJson) {
        if (!ready || markersJson.isBlank()) return@LaunchedEffect
        val b64 = Base64.encodeToString(markersJson.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        webView?.evaluateJavascript("if (window.setMarkersBase64) { window.setMarkersBase64('$b64'); }", null)
    }
    LaunchedEffect(ready, userCoords) {
        val u = userCoords ?: return@LaunchedEffect
        if (ready && (u.first != 0.0 || u.second != 0.0)) webView?.evaluateJavascript("if (window.setUser) { window.setUser(${u.first}, ${u.second}, false); }", null)
    }
    LaunchedEffect(ready, routeCoordinates) {
        if (!ready || routeCoordinates.size < 2) return@LaunchedEffect
        val arr = org.json.JSONArray()
        val step = maxOf(1, routeCoordinates.size / 2000)
        var i = 0
        while (i < routeCoordinates.size) { arr.put(org.json.JSONArray().put(routeCoordinates[i].first).put(routeCoordinates[i].second)); i += step }
        val b64 = Base64.encodeToString(arr.toString().toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        webView?.evaluateJavascript("if (window.setRouteBase64) { window.setRouteBase64('$b64'); }", null)
    }
}
