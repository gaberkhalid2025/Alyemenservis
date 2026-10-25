package com.example.ui.screens.map

import android.annotation.SuppressLint
import android.content.Context
import android.view.ViewGroup
import android.webkit.JavascriptInterface
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
import com.example.utils.VisualThemePalette
import com.example.utils.resolveThemePalette

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
    themeColors: VisualThemePalette = resolveThemePalette(AdminSettingsEntity()),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isMapReady by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var hasLoadError by remember { mutableStateOf(false) }

    // [المحور الثالث] حماية الشاشة السوداء: مؤقت 15 ثانية. إذا لم تصل إشارة onMapReady يتم التحويل تلقائياً لـ OfflineInteractiveMap
    LaunchedEffect(isMapReady) {
        if (!isMapReady) {
            kotlinx.coroutines.delay(15000L)
            if (!isMapReady) {
                hasLoadError = true
                onMapLoadFailed?.invoke()
            }
        }
    }

    // تحويل البيانات إلى JSON مهيأ للبلاطات والعلامات
    val markersJson = remember(nearbyProviders, nearbyStores, nearbyProperties, selectedCity) {
        MapJsonBuilder.buildMarkersJson(
            nearbyProviders = nearbyProviders,
            nearbyStores = nearbyStores,
            nearbyProperties = nearbyProperties,
            dynamicOffsets = dynamicOffsets,
            safeUserLat = userCoords.first,
            safeUserLng = userCoords.second
        )
    }

    // إرسال العلامات للخريطة عند اكتمال الجاهزية أو تغير البيانات بأمان تام عبر Base64
    LaunchedEffect(isMapReady, markersJson) {
        if (isMapReady && webViewRef != null && markersJson.isNotBlank()) {
            val b64 = android.util.Base64.encodeToString(markersJson.toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP)
            val script = "if (window.setMarkersBase64) { window.setMarkersBase64('$b64'); } else if (window.setMarkers) { window.setMarkers('$markersJson'); }"
            webViewRef?.evaluateJavascript(script, null)
        }
    }

    // تبديل أوضاع الخريطة
    LaunchedEffect(isMapReady, currentMapMode) {
        if (isMapReady && webViewRef != null) {
            val modeKey = when (currentMapMode) {
                MapMode.VELA_3D -> "VELA_3D"
                MapMode.OSM -> "OSM"
                MapMode.HOT -> "HOT"
                MapMode.TOPO -> "TOPO"
                MapMode.SATELLITE -> "SATELLITE"
                else -> "SATELLITE"
            }
            val script = "if (window.switchMapMode) { window.switchMapMode('$modeKey'); }"
            webViewRef?.evaluateJavascript(script, null)
        }
    }

    // تحديث موقع المستخدم الفعلي على الخريطة
    LaunchedEffect(isMapReady, userCoords) {
        if (isMapReady && webViewRef != null && userCoords.first != 0.0 && userCoords.second != 0.0) {
            val script = "if (window.updateUserLocation) { window.updateUserLocation(${userCoords.first}, ${userCoords.second}, 30, false); }"
            webViewRef?.evaluateJavascript(script, null)
        }
    }

    // تمركز الخريطة عند تغيير المدينة
    LaunchedEffect(isMapReady, selectedCity) {
        if (isMapReady && webViewRef != null) {
            val cityCoords = OfflineMapManager.getCityCoordinates(selectedCity)
            val script = "if (window.setCity) { window.setCity('${cityCoords.nameAr}', ${cityCoords.latitude}, ${cityCoords.longitude}, ${cityCoords.defaultZoom}); }"
            webViewRef?.evaluateJavascript(script, null)
        }
    }

    // التحكم في التكبير والتصغير التفاعلي
    var lastZoomScale by remember { mutableFloatStateOf(zoomScale) }
    LaunchedEffect(zoomScale) {
        if (isMapReady && webViewRef != null) {
            if (zoomScale > lastZoomScale) {
                webViewRef?.evaluateJavascript("if (window.zoomIn) { window.zoomIn(); }", null)
            } else if (zoomScale < lastZoomScale) {
                webViewRef?.evaluateJavascript("if (window.zoomOut) { window.zoomOut(); }", null)
            }
            lastZoomScale = zoomScale
        }
    }

    // إعادة التمركز إلى موقع المستخدم
    LaunchedEffect(panOffset) {
        if (isMapReady && webViewRef != null && panOffset == Offset.Zero) {
            webViewRef?.evaluateJavascript("if (window.recenterUser) { window.recenterUser(); }", null)
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
                            allowFileAccess = true
                            allowContentAccess = true
                        }
                        // جسر التواصل بين JavaScript في Leaflet وكود Kotlin الأصلي
                        addJavascriptInterface(object {
                            @JavascriptInterface
                            fun onMapReady() {
                                isMapReady = true
                            }

                            @JavascriptInterface
                            fun onMapClicked() {
                                onDeselect()
                            }

                            @JavascriptInterface
                            fun onMarkerClicked(id: String, type: String) {
                                when (type.uppercase()) {
                                    "PROVIDER" -> {
                                        nearbyProviders.find { it.id == id }?.let { onProviderSelected(it) }
                                    }
                                    "STORE", "RESTAURANT", "MEDICAL" -> {
                                        nearbyStores.find { it.id == id }?.let { onStoreSelected(it) }
                                    }
                                    "PROPERTY" -> {
                                        nearbyProperties.find { it.id == id }?.let { onPropertySelected(it) }
                                    }
                                }
                            }

                            // [إصلاح نهائي] استعلام وتغيير طبقات الخريطة عبر الجسر
                            @JavascriptInterface
                            fun onMapModeChanged(newMode: String) {
                                android.util.Log.d("LeafletBridge", "Map mode switched to: $newMode")
                            }

                            @JavascriptInterface
                            fun getAvailableLayers(): String {
                                return "[\"OSM\",\"HOT\",\"TOPO\",\"SATELLITE\",\"VELA_3D\"]"
                            }
                        }, "AndroidBridge")

                        webViewClient = object : WebViewClient() {
                            // [إصلاح نهائي] منع الانتقال إلى أي موقع أو صفحة خارجية نهائياً عند النقر على نص الإسناد
                            override fun shouldOverrideUrlLoading(view: WebView?, request: android.webkit.WebResourceRequest?): Boolean {
                                return true
                            }

                            @Deprecated("Deprecated in Java")
                            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
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
                                request: android.webkit.WebResourceRequest?,
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
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // الخريطة التفاعلية المحلية البديلة في حالة الطوارئ (100% بدون شاشة سوداء)
            OfflineInteractiveMap(
                userCoords = userCoords,
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

object MapAssetMemoryCache {
    fun isCached(): Boolean = true
    fun invalidate() {}
    fun getOrLoadSync(context: Context): String = ""
    suspend fun getOrLoadAsync(context: Context): String = ""
}
