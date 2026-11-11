package com.example.ui.screens.map

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

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
    routeCoordinates: List<Pair<Double, Double>> = emptyList(),
    navigating: Boolean = false,
    onMarkerClicked: (id: String, type: String) -> Unit,
    onUnavailable: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val markerCb by rememberUpdatedState(onMarkerClicked)
    val unavailableCb by rememberUpdatedState(onUnavailable)
    var webView by remember { mutableStateOf<WebView?>(null) }
    var ready by remember { mutableStateOf(false) }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    cacheMode = WebSettings.LOAD_DEFAULT
                    allowFileAccess = false              // أصول assets لا تتأثر بهذا الإعداد
                    allowContentAccess = false
                    userAgentString = "AlYemenServicesApp/1.0 (Android; OpenStreetMap)"
                }
                addJavascriptInterface(object {
                    @JavascriptInterface fun onMapReady() { mainHandler.post { ready = true } }
                    @JavascriptInterface fun onMarkerClicked(id: String, type: String) { mainHandler.post { markerCb(id, type) } }
                    @JavascriptInterface fun onUnavailable(reason: String) { mainHandler.post { unavailableCb(reason) } }
                }, "AndroidBridge")
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        view?.evaluateJavascript("if (window.init3D) { window.init3D($centerLat, $centerLng); }", null)
                    }
                }
                loadUrl("file:///android_asset/map3d.html")
                webView = this
            }
        },
        onRelease = { w -> w.stopLoading(); w.removeJavascriptInterface("AndroidBridge"); w.destroy() }
    )

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
    LaunchedEffect(ready, navigating) {
        if (ready) webView?.evaluateJavascript("if (window.setNavigatingBuildings) { window.setNavigatingBuildings($navigating); }", null)
    }
}
