@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.screens.map

import com.example.ui.screens.dashboard.*
import android.content.Intent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.PathEffect
import okhttp3.MediaType.Companion.toMediaType

import com.example.*

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.*
import com.example.utils.*
import com.example.ui.MainViewModel
import com.example.ui.components.*
import com.example.ui.dialogs.*
import com.example.ui.screens.home.*
import com.example.ui.screens.map.*
import com.example.ui.screens.bookings.*
import com.example.ui.screens.admin.*
import com.example.ui.screens.assistant.*
import com.example.ui.screens.register.*
import com.example.ui.screens.status.*
import com.example.ui.screens.about.*
import com.example.ui.screens.chat.*
import com.example.ui.*
import com.example.ui.utils.*
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


@Composable
fun RealLeafletMapView(
    userCoords: Pair<Double, Double>,
    nearbyProviders: List<com.example.data.ProviderEntity>,
    nearbyStores: List<com.example.data.StoreEntity>,
    nearbyProperties: List<com.example.data.PropertyEntity>,
    dynamicOffsets: Map<String, Pair<Double, Double>>,
    onProviderSelected: (com.example.data.ProviderEntity) -> Unit,
    onStoreSelected: (com.example.data.StoreEntity) -> Unit,
    onPropertySelected: (com.example.data.PropertyEntity) -> Unit
) {
    // Build HTML string
    val providerMarkers = nearbyProviders.map { p ->
        val baseCoords = getProviderCoords(p)
        val walkOffset = dynamicOffsets[p.id] ?: Pair(0.0, 0.0)
        val liveLat = baseCoords.first + walkOffset.first
        val liveLng = baseCoords.second + walkOffset.second
        val categoryEmoji = when {
            p.categoryId.contains("spaka") -> "🔧"
            p.categoryId.contains("kahraba") -> "⚡"
            p.categoryId.contains("dehan") -> "🎨"
            p.categoryId.contains("hadada") -> "🔨"
            p.categoryId.contains("store") -> "🏪"
            p.categoryId.contains("property") -> "🏠"
            p.categoryId.contains("restaurant") -> "🍔"
            p.categoryId.contains("job") -> "💼"
            else -> "👷"
        }
        val safeName = p.name.replace("\"", "\\\"").replace("\'", "\\'")
        val pSpec = p.specialization.ifEmpty { p.profession }.replace("\"", "\\\"").replace("\'", "\\'")
        val cleanSpec = if (pSpec.isEmpty()) "مقدم خدمة" else pSpec
        """
        {
            id: "${p.id}",
            type: "PROVIDER",
            name: "$safeName",
            lat: $liveLat,
            lng: $liveLng,
            emoji: "$categoryEmoji",
            spec: "$cleanSpec",
            status: "${if (p.isAvailable) "متاح 🟢" else "مشغول 🟡"}"
        }
        """
    }

    val storeMarkers = nearbyStores.map { s ->
        val emoji = when {
            s.sectionId.contains("medical") || s.categoryId.contains("medical") || s.name.contains("طبي") || s.name.contains("مستشفى") || s.name.contains("عيادة") -> "🏥"
            s.sectionId.contains("restaurant") || s.categoryId.contains("restaurant") || s.name.contains("مطعم") || s.name.contains("مأكولات") -> "🍔"
            else -> "🏪"
        }
        val safeName = s.name.replace("\"", "\\\"").replace("\'", "\\'")
        val safeDesc = s.description.replace("\"", "\\\"").replace("\'", "\\'").take(50)
        val cleanDesc = if (safeDesc.isEmpty()) "محل تجاري" else safeDesc
        """
        {
            id: "${s.id}",
            type: "STORE",
            name: "$safeName",
            lat: ${s.latitude},
            lng: ${s.longitude},
            emoji: "$emoji",
            spec: "$cleanDesc",
            status: "${if (s.isActive) "مفتوح 🟢" else "مغلق 🔴"}"
        }
        """
    }

    val propertyMarkers = nearbyProperties.map { pr ->
        val safeTitle = pr.title.replace("\"", "\\\"").replace("\'", "\\'")
        val safeDesc = pr.description.replace("\"", "\\\"").replace("\'", "\\'").take(50)
        val cleanDesc = if (safeDesc.isEmpty()) "عقار معروض" else safeDesc
        """
        {
            id: "${pr.id}",
            type: "PROPERTY",
            name: "$safeTitle",
            lat: ${pr.latitude},
            lng: ${pr.longitude},
            emoji: "🏠",
            spec: "$cleanDesc",
            status: "نشط 🟢"
        }
        """
    }

    val markersJson = (providerMarkers + storeMarkers + propertyMarkers).joinToString(",")

    val htmlContent = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no" />
            <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
            <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
            <style>
                html, body, #map {
                    height: 100%;
                    margin: 0;
                    padding: 0;
                    background: #0B0F19;
                    font-family: system-ui, -apple-system, sans-serif;
                }
                .leaflet-tile-container img {
                    filter: brightness(0.85) contrast(1.1);
                }
                .leaflet-popup-content-value {
                    font-family: sans-serif;
                    text-align: right;
                    direction: rtl;
                    color: #FFFFFF;
                }
                .leaflet-popup-content-wrapper {
                    background: #1E293B !important;
                    color: #FFFFFF !important;
                    border: 2px solid #00E5FF;
                    border-radius: 14px;
                    box-shadow: 0 10px 25px rgba(0,0,0,0.6);
                }
                .leaflet-popup-tip {
                    background: #1E293B !important;
                }
                .custom-pin {
                    width: 40px;
                    height: 40px;
                    background: #1E293B;
                    border: 2px solid #00E5FF;
                    border-radius: 50%;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    font-size: 20px;
                    box-shadow: 0 4px 12px rgba(0,229,255,0.4);
                    transition: transform 0.2s ease;
                }
                .custom-pin:hover {
                    transform: scale(1.15);
                }
                .user-marker-container {
                    position: relative;
                    width: 24px;
                    height: 24px;
                }
                .user-marker {
                    width: 18px;
                    height: 18px;
                    background: #00E5FF;
                    border: 3px solid #FFFFFF;
                    border-radius: 50%;
                    box-shadow: 0 0 16px rgba(0, 229, 255, 1);
                    position: absolute;
                    top: 3px;
                    left: 3px;
                    z-index: 2;
                }
                .user-pulse {
                    position: absolute;
                    width: 36px;
                    height: 36px;
                    background: rgba(0, 229, 255, 0.35);
                    border-radius: 50%;
                    top: -6px;
                    left: -6px;
                    animation: pulseRing 2s infinite ease-out;
                }
                @keyframes pulseRing {
                    0% { transform: scale(0.5); opacity: 1; }
                    100% { transform: scale(1.6); opacity: 0; }
                }
            </style>
        </head>
        <body>
            <div id="map"></div>
            <script>
                var map = L.map('map', {
                    zoomControl: false,
                    attributionControl: false
                }).setView([${userCoords.first}, ${userCoords.second}], 14);

                L.tileLayer('https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png', {
                    maxZoom: 19,
                    subdomains: 'abcd'
                }).addTo(map);

                // User location marker with pulse
                var userIcon = L.divIcon({
                    className: 'user-marker-container',
                    html: '<div class="user-pulse"></div><div class="user-marker"></div>',
                    iconSize: [24, 24],
                    iconAnchor: [12, 12]
                });
                var userMarker = L.marker([${userCoords.first}, ${userCoords.second}], {icon: userIcon}).addTo(map)
                    .bindPopup("<div class='leaflet-popup-content-value'><b style='color:#00E5FF;'>📍 موقعك الحالي المباشر</b></div>");

                var bounds = [ [${userCoords.first}, ${userCoords.second}] ];
                var markers = [$markersJson];
                markers.forEach(function(p) {
                    var pinColor = p.type === 'PROVIDER' ? '#00E5FF' : (p.type === 'STORE' ? '#F59E0B' : '#10B981');
                    var customIcon = L.divIcon({
                        className: 'custom-pin-wrapper',
                        html: '<div class="custom-pin" style="border-color:' + pinColor + ';">' + p.emoji + '</div>',
                        iconSize: [40, 40],
                        iconAnchor: [20, 20]
                    });
                    var m = L.marker([p.lat, p.lng], {icon: customIcon}).addTo(map);
                    bounds.push([p.lat, p.lng]);
                    var popupHtml = "<div class='leaflet-popup-content-value'>" +
                        "<b style='color:#00E5FF; font-size:14px;'>" + p.name + "</b><br/>" +
                        "<span style='color:#CBD5E1; font-size:11px;'>" + p.spec + "</span><br/>" +
                        "<span style='font-size:11px; font-weight:bold;'>" + p.status + "</span><br/>" +
                        "<button onclick=\"AndroidBridge.onMarkerClicked('" + p.type + "', '" + p.id + "')\" style='margin-top:8px;background:#00E5FF;color:#0F172A;border:none;padding:7px 12px;border-radius:8px;width:100%;font-weight:bold;font-size:12px;cursor:pointer;box-shadow:0 2px 8px rgba(0,229,255,0.4);'>عرض التفاصيل والطلب 🔍</button>" +
                        "</div>";
                    m.bindPopup(popupHtml);
                });

                if (bounds.length > 1) {
                    map.fitBounds(bounds, { padding: [50, 50], maxZoom: 16 });
                }
            </script>
        </body>
        </html>
    """.trimIndent()

    androidx.compose.ui.viewinterop.AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            android.webkit.WebView(ctx).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                }
                webViewClient = object : android.webkit.WebViewClient() {
                    override fun onRenderProcessGone(
                        view: android.webkit.WebView?,
                        detail: android.webkit.RenderProcessGoneDetail?
                    ): Boolean {
                        // Crucial: Prevent system crash if renderer gets terminated/low RAM
                        return true
                    }
                }
                addJavascriptInterface(object {
                    @android.webkit.JavascriptInterface
                    fun onMarkerClicked(type: String, id: String) {
                        try {
                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                try {
                                    when (type) {
                                        "PROVIDER" -> {
                                            val matched = nearbyProviders.find { it.id == id }
                                            if (matched != null) {
                                                onProviderSelected(matched)
                                            }
                                        }
                                        "STORE" -> {
                                            val matched = nearbyStores.find { it.id == id }
                                            if (matched != null) {
                                                onStoreSelected(matched)
                                            }
                                        }
                                        "PROPERTY" -> {
                                            val matched = nearbyProperties.find { it.id == id }
                                            if (matched != null) {
                                                onPropertySelected(matched)
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }, "AndroidBridge")
                loadDataWithBaseURL("https://openstreetmap.org", htmlContent, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            // Re-creation of WebView when data changes is managed via Compose keys in parent caller
        }
    )
}