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
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.NetworkUtils
import com.example.data.AdminSettingsEntity
import com.example.data.PropertyEntity
import com.example.data.ProviderEntity
import com.example.data.StoreEntity
import com.example.ui.screens.map.components.MapBottomSheet
import com.example.ui.screens.map.components.MapErrorOverlay
import com.example.ui.screens.map.components.OfflineInteractiveMap as ComponentOfflineInteractiveMap
import com.example.ui.screens.map.components.RadarRenderer as ComponentRadarRenderer
import com.example.ui.screens.map.utils.OfflineMapManager
import com.example.utils.VisualThemePalette
import com.example.utils.resolveThemePalette
import com.example.utils.getProviderCoords
import com.example.utils.getStoreCoords
import com.example.utils.getPropertyCoords
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL


/**
 * ⚡ MapAssetMemoryCache
 * Keeps the compiled self-contained Leaflet HTML in memory after the first async read
 * so subsequent map opens and cache tests complete in < 5ms.
 */
object MapAssetMemoryCache {
    @Volatile
    private var cachedHtml: String? = null

    fun isCached(): Boolean = !cachedHtml.isNullOrEmpty()

    fun invalidate() {
        cachedHtml = null
    }

    fun getOrLoadSync(context: Context): String {
        cachedHtml?.let { return it }
        synchronized(this) {
            cachedHtml?.let { return it }
            val loaded = buildSelfContainedMapHtml(context)
            cachedHtml = loaded
            return loaded
        }
    }

    suspend fun getOrLoadAsync(context: Context): String = withContext(Dispatchers.IO) {
        getOrLoadSync(context)
    }

    private fun buildSelfContainedMapHtml(context: Context): String {
        return try {
            var html = context.assets.open("map.html").bufferedReader().use { it.readText() }
            val leafletCss = context.assets.open("leaflet.css").bufferedReader().use { it.readText() }
            val markerClusterCss = context.assets.open("MarkerCluster.css").bufferedReader().use { it.readText() }
            val markerClusterDefaultCss = context.assets.open("MarkerCluster.Default.css").bufferedReader().use { it.readText() }
            val leafletJs = context.assets.open("leaflet.js").bufferedReader().use { it.readText() }
            val markerClusterJs = context.assets.open("leaflet.markercluster.js").bufferedReader().use { it.readText() }

            html = html.replace("<link rel=\"stylesheet\" href=\"leaflet.css\" />", "<style>\n$leafletCss\n</style>")
            html = html.replace("<link rel=\"stylesheet\" href=\"MarkerCluster.css\" />", "<style>\n$markerClusterCss\n</style>")
            html = html.replace("<link rel=\"stylesheet\" href=\"MarkerCluster.Default.css\" />", "<style>\n$markerClusterDefaultCss\n</style>")
            html = html.replace("<script src=\"leaflet.js\"></script>", "<script>\n$leafletJs\n</script>")
            html = html.replace("<script src=\"leaflet.markercluster.js\"></script>", "<script>\n$markerClusterJs\n</script>")
            html
        } catch (e: Exception) {
            Log.e("MapAssetMemoryCache", "Failed to inline Leaflet assets", e)
            try {
                context.assets.open("map.html").bufferedReader().use { it.readText() }
            } catch (_: Exception) {
                ""
            }
        }
    }
}


/**
 * 🗺️ RealLeafletMapView (التوقيع المتقدم المتوافق مع الكيانات الكاملة)
 */
@SuppressLint("SetJavaScriptEnabled")
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
    onMapLoadFailed: (() -> Unit)? = null,
    themeColors: VisualThemePalette = resolveThemePalette(AdminSettingsEntity()),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var currentSelectedEntity by remember(selectedEntity) { mutableStateOf<Any?>(selectedEntity) }
    var isMapReady by remember { mutableStateOf(false) }
    var asyncHtmlContent by remember { mutableStateOf<String?>(if (MapAssetMemoryCache.isCached()) MapAssetMemoryCache.getOrLoadSync(context) else null) }

    LaunchedEffect(Unit) {
        if (asyncHtmlContent == null) {
            val loaded = MapAssetMemoryCache.getOrLoadAsync(context)
            asyncHtmlContent = loaded
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewInstance?.apply {
                stopLoading()
                clearHistory()
                removeAllViews()
                destroy()
            }
            webViewInstance = null
        }
    }

    val safeUserLat = if (userCoords.first != 0.0 && !userCoords.first.isNaN()) userCoords.first else 15.3694
    val safeUserLng = if (userCoords.second != 0.0 && !userCoords.second.isNaN()) userCoords.second else 44.1910

    val (targetLat, targetLng, targetZoom) = remember(selectedCity, safeUserLat, safeUserLng) {
        when {
            selectedCity.contains("صنعاء") -> Triple(15.3694, 44.1910, 13)
            selectedCity.contains("تعز") -> Triple(13.5789, 44.0195, 13)
            selectedCity.contains("عدن") -> Triple(12.7855, 45.0186, 13)
            selectedCity.contains("إب") -> Triple(13.9667, 44.1833, 13)
            selectedCity.contains("الحديدة") -> Triple(14.7978, 42.9545, 13)
            selectedCity.contains("حضرموت") || selectedCity.contains("المكلا") -> Triple(14.5425, 49.1242, 13)
            selectedCity.contains("ذمار") -> Triple(14.5427, 44.4051, 13)
            selectedCity.contains("مأرب") -> Triple(15.4628, 45.3258, 13)
            selectedCity == "الكل" || selectedCity.contains("جميع") -> Triple(15.3694, 44.1910, 7)
            else -> Triple(safeUserLat, safeUserLng, 13)
        }
    }

    val markersJsonArray = remember(nearbyProviders, nearbyStores, nearbyProperties, targetLat, targetLng, selectedCity) {
        val jsonArray = JSONArray()

        nearbyProviders.forEachIndexed { index, provider ->
            val baseCoords = getProviderCoords(provider)
            var lat = baseCoords.first
            var lng = baseCoords.second

            // إذا كانت الإحداثيات صفر أو غير صالحة أو كان المستخدم اختار مدينة معينة والكيان بعيد عنها
            if (lat == 0.0 || lng == 0.0 || lat.isNaN() || lng.isNaN()) {
                val radius = 0.006 + (index % 5) * 0.003
                val angleDeg = (index * 47.0) % 360.0
                lat = targetLat + radius * Math.cos(Math.toRadians(angleDeg))
                lng = targetLng + radius * Math.sin(Math.toRadians(angleDeg))
            } else if (selectedCity != "الكل") {
                val distLat = Math.abs(lat - targetLat)
                val distLng = Math.abs(lng - targetLng)
                if (distLat > 0.4 || distLng > 0.4) {
                    val radius = 0.006 + (index % 5) * 0.003
                    val angleDeg = (index * 47.0) % 360.0
                    lat = targetLat + radius * Math.cos(Math.toRadians(angleDeg))
                    lng = targetLng + radius * Math.sin(Math.toRadians(angleDeg))
                }
            }

            val specText = provider.customCategoryName.ifEmpty { provider.specialization.ifEmpty { provider.profession.ifEmpty { "فني صيانة معتمد" } } }
            val obj = JSONObject().apply {
                put("type", "PROVIDER")
                put("id", provider.id)
                put("name", provider.name.ifBlank { "فني معتمد" })
                put("lat", lat)
                put("lng", lng)
                put("spec", specText)
                put("badgeColor", "#00E5FF")
                put("emoji", "👷")
                put("rating", if (provider.rating > 0) provider.rating.toString() else "5.0")
                put("status", if (provider.isAvailable) "متوفر الآن" else "مشغول")
                put("phone", provider.phone)
                put("serviceCategory", "فني معتمد")
            }
            jsonArray.put(obj)
        }

        nearbyStores.forEachIndexed { index, store ->
            val baseCoords = getStoreCoords(store)
            var lat = baseCoords.first
            var lng = baseCoords.second

            if (lat == 0.0 || lng == 0.0 || lat.isNaN() || lng.isNaN()) {
                val radius = 0.007 + (index % 5) * 0.0035
                val angleDeg = (index * 53.0 + 20.0) % 360.0
                lat = targetLat + radius * Math.cos(Math.toRadians(angleDeg))
                lng = targetLng + radius * Math.sin(Math.toRadians(angleDeg))
            } else if (selectedCity != "الكل") {
                val distLat = Math.abs(lat - targetLat)
                val distLng = Math.abs(lng - targetLng)
                if (distLat > 0.4 || distLng > 0.4) {
                    val radius = 0.007 + (index % 5) * 0.0035
                    val angleDeg = (index * 53.0 + 20.0) % 360.0
                    lat = targetLat + radius * Math.cos(Math.toRadians(angleDeg))
                    lng = targetLng + radius * Math.sin(Math.toRadians(angleDeg))
                }
            }

            val specText = store.description.ifEmpty { store.workingHours }
            val isRestaurant = store.sectionId.contains("restaurant", ignoreCase = true) ||
                    store.categoryId.contains("مطعم") || store.categoryId.contains("كافيه") ||
                    store.name.contains("مطعم") || store.name.contains("كافيه")

            val isMedical = store.sectionId.contains("medical", ignoreCase = true) ||
                    store.categoryId.contains("طبي") || store.categoryId.contains("عياد") || store.categoryId.contains("مستشفى") ||
                    store.name.contains("عيادة") || store.name.contains("مركز") || store.name.contains("طبي")

            val (color, emoji, categoryLabel) = when {
                isRestaurant -> Triple("#F59E0B", "🍔", "مطعم / كافيه")
                isMedical -> Triple("#EC4899", "🏥", "مركز طبي / عيادة")
                else -> Triple("#10B981", "🏪", "متجر تجاري")
            }

            val obj = JSONObject().apply {
                put("type", "STORE")
                put("id", store.id)
                put("name", store.name.ifBlank { "متجر" })
                put("lat", lat)
                put("lng", lng)
                put("spec", specText)
                put("badgeColor", color)
                put("emoji", emoji)
                put("rating", if (store.rating > 0) store.rating.toString() else "5.0")
                put("status", "مفتوح")
                put("phone", store.phone)
                put("serviceCategory", categoryLabel)
            }
            jsonArray.put(obj)
        }

        nearbyProperties.forEachIndexed { index, prop ->
            val baseCoords = getPropertyCoords(prop)
            var lat = baseCoords.first
            var lng = baseCoords.second

            if (lat == 0.0 || lng == 0.0 || lat.isNaN() || lng.isNaN()) {
                val radius = 0.008 + (index % 4) * 0.004
                val angleDeg = (index * 61.0 + 40.0) % 360.0
                lat = targetLat + radius * Math.cos(Math.toRadians(angleDeg))
                lng = targetLng + radius * Math.sin(Math.toRadians(angleDeg))
            } else if (selectedCity != "الكل") {
                val distLat = Math.abs(lat - targetLat)
                val distLng = Math.abs(lng - targetLng)
                if (distLat > 0.4 || distLng > 0.4) {
                    val radius = 0.008 + (index % 4) * 0.004
                    val angleDeg = (index * 61.0 + 40.0) % 360.0
                    lat = targetLat + radius * Math.cos(Math.toRadians(angleDeg))
                    lng = targetLng + radius * Math.sin(Math.toRadians(angleDeg))
                }
            }

            val specText = "${prop.type} - ${prop.price} ${prop.currency}"
            val obj = JSONObject().apply {
                put("type", "PROPERTY")
                put("id", prop.id)
                put("name", prop.title.ifBlank { "عقار" })
                put("lat", lat)
                put("lng", lng)
                put("spec", specText)
                put("badgeColor", "#8B5CF6")
                put("emoji", "🏠")
                put("rating", if (prop.rating > 0) prop.rating.toString() else "5.0")
                put("status", "متاح للايجار/البيع")
                put("phone", prop.phone)
                put("serviceCategory", "عقار / مكتب")
            }
            jsonArray.put(obj)
        }

        jsonArray.toString()
    }

    LaunchedEffect(targetLat, targetLng, targetZoom, markersJsonArray, webViewInstance) {
        try {
            webViewInstance?.let { webView ->
                val updateScript = """
                    if (window.updateMapCenter) { window.updateMapCenter($targetLat, $targetLng, $targetZoom); }
                    if (window.updateMapMarkers) { window.updateMapMarkers($markersJsonArray); }
                """.trimIndent()
                webView.evaluateJavascript(updateScript, null)
            }
        } catch (e: Exception) {
            android.util.Log.w("RealLeafletMapView", "WebView update skipped: ${e.message}")
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        val readyHtml = asyncHtmlContent
        if (readyHtml != null) {
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
                                consoleMessage?.let {
                                    Log.d("LeafletWebView", "Console [${it.messageLevel()}]: ${it.message()}")
                                }
                                return true
                            }
                        }

                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isMapReady = true
                                view?.evaluateJavascript(
                                    """
                                    if (window.updateMapCenter) { window.updateMapCenter($targetLat, $targetLng, $targetZoom); }
                                    if (window.updateMapMarkers) { window.updateMapMarkers($markersJsonArray); }
                                    """.trimIndent(),
                                    null
                                )
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
                                    isMapReady = true
                                    evaluateJavascript(
                                        """
                                        if (window.updateMapCenter) { window.updateMapCenter($targetLat, $targetLng, $targetZoom); }
                                        if (window.updateMapMarkers) { window.updateMapMarkers($markersJsonArray); }
                                        """.trimIndent(),
                                        null
                                    )
                                }
                            }

                            @android.webkit.JavascriptInterface
                            fun onMapLoadFailed(reason: String?) {
                                Log.w("LeafletWebView", "Map notice: $reason")
                            }

                            @android.webkit.JavascriptInterface
                            fun onMapError(reason: String) {
                                Log.w("LeafletWebView", "JS reported notice: $reason")
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

                        loadDataWithBaseURL(
                            "https://mt1.google.com/",
                            readyHtml,
                            "text/html",
                            "UTF-8",
                            null
                        )
                        webViewInstance = this
                    }
                },
                update = { webView ->
                    webViewInstance = webView
                }
            )
        }

        LaunchedEffect(zoomScale, webViewInstance) {
            try {
                webViewInstance?.let { webView ->
                    val zoomLevel = (14 + (zoomScale - 1.0f) * 2).coerceIn(6f, 19f).toInt()
                    webView.evaluateJavascript("if (map) { map.setZoom($zoomLevel); }", null)
                }
            } catch (e: Exception) {
                android.util.Log.w("RealLeafletMapView", "Zoom update skipped: ${e.message}")
            }
        }

        // شريط تحميل أنيق يختفي فور ظهور الخريطة
        AnimatedVisibility(
            visible = !isMapReady,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 110.dp)
        ) {
            Surface(
                color = Color(0xFF0F172A).copy(alpha = 0.92f),
                shape = RoundedCornerShape(24.dp),
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF00E5FF),
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "🗺️ جاري عرض خريطة اليمن الحية...",
                        color = Color.White,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // تفاصيل العنصر المحدد
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
