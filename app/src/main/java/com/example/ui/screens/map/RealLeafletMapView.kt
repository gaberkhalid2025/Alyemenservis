@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.screens.map

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.PropertyEntity
import com.example.data.ProviderEntity
import com.example.data.StoreEntity
import com.example.utils.getProviderCoords

/**
 * 🗺️ RealLeafletMapView
 * خريطة تفاعلية متطورة تعتمد على OpenStreetMap و Leaflet JS بدون أي تكاليف
 * تشمل تحديد الموقع المباشر، رادار الفنيين والمحلات، حساب المسافات، وأزرار الملاحة والاتصال السريع
 */
@Composable
fun RealLeafletMapView(
    userCoords: Pair<Double, Double>,
    nearbyProviders: List<ProviderEntity>,
    nearbyStores: List<StoreEntity>,
    nearbyProperties: List<PropertyEntity>,
    dynamicOffsets: Map<String, Pair<Double, Double>>,
    onProviderSelected: (ProviderEntity) -> Unit,
    onStoreSelected: (StoreEntity) -> Unit,
    onPropertySelected: (PropertyEntity) -> Unit
) {
    val context = LocalContext.current

    // Build JSON data for Leaflet
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
            p.categoryId.contains("ac") || p.categoryId.contains("tabreed") -> "❄️"
            p.categoryId.contains("car") || p.categoryId.contains("mechanic") -> "🚗"
            p.categoryId.contains("carpentry") || p.categoryId.contains("najjara") -> "🪚"
            else -> "👷"
        }
        val safeName = p.name.replace("\"", "\\\"").replace("'", "\\'")
        val pSpec = p.specialization.ifEmpty { p.profession }.replace("\"", "\\\"").replace("'", "\\'")
        val cleanSpec = if (pSpec.isEmpty()) "فني صيانة معتمد" else pSpec
        val safePhone = p.phone.replace("\"", "")
        """
        {
            id: "${p.id}",
            type: "PROVIDER",
            name: "$safeName",
            lat: $liveLat,
            lng: $liveLng,
            emoji: "$categoryEmoji",
            spec: "$cleanSpec",
            phone: "$safePhone",
            rating: "${p.rating}",
            status: "${if (p.isAvailable) "متاح لاستقبال الطلبات 🟢" else "مشغول حالياً 🟡"}",
            badgeColor: "#00E5FF"
        }
        """
    }

    val storeMarkers = nearbyStores.map { s ->
        val emoji = when {
            s.sectionId.contains("medical") || s.categoryId.contains("medical") || s.name.contains("طبي") || s.name.contains("مستشفى") || s.name.contains("عيادة") || s.name.contains("صيدلية") -> "🏥"
            s.sectionId.contains("restaurant") || s.categoryId.contains("restaurant") || s.name.contains("مطعم") || s.name.contains("مأكولات") || s.name.contains("كافيه") -> "🍔"
            s.sectionId.contains("supermarket") || s.name.contains("بقالة") || s.name.contains("هايبر") -> "🛒"
            else -> "🏪"
        }
        val safeName = s.name.replace("\"", "\\\"").replace("'", "\\'")
        val safeDesc = s.description.replace("\"", "\\\"").replace("'", "\\'").take(60)
        val cleanDesc = if (safeDesc.isEmpty()) "محل / متجر معتمد" else safeDesc
        val safePhone = s.phone.replace("\"", "")
        val badgeColor = if (emoji == "🏥") "#EC4899" else if (emoji == "🍔") "#F59E0B" else "#10B981"
        """
        {
            id: "${s.id}",
            type: "STORE",
            name: "$safeName",
            lat: ${s.latitude},
            lng: ${s.longitude},
            emoji: "$emoji",
            spec: "$cleanDesc",
            phone: "$safePhone",
            rating: "${s.rating}",
            status: "${if (s.isActive) "مفتوح ويستقبل الطلبات 🟢" else "مغلق حالياً 🔴"}",
            badgeColor: "$badgeColor"
        }
        """
    }

    val propertyMarkers = nearbyProperties.map { pr ->
        val safeTitle = pr.title.replace("\"", "\\\"").replace("'", "\\'")
        val safeDesc = pr.description.replace("\"", "\\\"").replace("'", "\\'").take(60)
        val cleanDesc = if (safeDesc.isEmpty()) "عقار معروض" else safeDesc
        val safePhone = pr.phone.replace("\"", "")
        val priceFormatted = "${pr.price} ${pr.currency}"
        """
        {
            id: "${pr.id}",
            type: "PROPERTY",
            name: "$safeTitle",
            lat: ${pr.latitude},
            lng: ${pr.longitude},
            emoji: "🏠",
            spec: "$cleanDesc ($priceFormatted)",
            phone: "$safePhone",
            rating: "5.0",
            status: "معروض للإيجار / البيع 🟢",
            badgeColor: "#8B5CF6"
        }
        """
    }

    val markersJson = (providerMarkers + storeMarkers + propertyMarkers).joinToString(",")

    val htmlContent = """
        <!DOCTYPE html>
        <html lang="ar" dir="rtl">
        <head>
            <meta charset="utf-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
            <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
            <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
            <style>
                * {
                    box-sizing: border-box;
                }
                html, body, #map {
                    height: 100%;
                    width: 100%;
                    margin: 0;
                    padding: 0;
                    background: #0B0F19;
                    font-family: system-ui, -apple-system, sans-serif;
                    overflow: hidden;
                }
                .leaflet-tile-container img {
                    filter: brightness(0.85) contrast(1.1);
                }
                .leaflet-popup-content-wrapper {
                    background: #1E293B !important;
                    color: #FFFFFF !important;
                    border: 2px solid #00E5FF;
                    border-radius: 16px;
                    padding: 4px;
                    box-shadow: 0 12px 30px rgba(0,0,0,0.7);
                }
                .leaflet-popup-tip {
                    background: #1E293B !important;
                }
                .popup-card {
                    font-family: system-ui, -apple-system, sans-serif;
                    text-align: right;
                    direction: rtl;
                    color: #FFFFFF;
                    padding: 8px 4px;
                }
                .popup-title {
                    font-size: 15px;
                    font-weight: 800;
                    color: #00E5FF;
                    margin-bottom: 4px;
                    line-height: 1.3;
                }
                .popup-spec {
                    font-size: 12px;
                    color: #CBD5E1;
                    margin-bottom: 6px;
                }
                .popup-meta {
                    display: flex;
                    justify-content: space-between;
                    font-size: 11px;
                    margin-bottom: 10px;
                    color: #94A3B8;
                    border-bottom: 1px solid rgba(255,255,255,0.1);
                    padding-bottom: 6px;
                }
                .popup-buttons {
                    display: flex;
                    gap: 6px;
                }
                .btn-details {
                    flex: 2;
                    background: linear-gradient(135deg, #00E5FF, #00B4D8);
                    color: #0F172A;
                    border: none;
                    padding: 8px 10px;
                    border-radius: 10px;
                    font-weight: bold;
                    font-size: 12px;
                    cursor: pointer;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    gap: 4px;
                }
                .btn-call {
                    flex: 1;
                    background: #10B981;
                    color: #FFFFFF;
                    border: none;
                    padding: 8px 6px;
                    border-radius: 10px;
                    font-weight: bold;
                    font-size: 12px;
                    cursor: pointer;
                    text-align: center;
                    text-decoration: none;
                }
                .btn-nav {
                    flex: 1;
                    background: #3B82F6;
                    color: #FFFFFF;
                    border: none;
                    padding: 8px 6px;
                    border-radius: 10px;
                    font-weight: bold;
                    font-size: 12px;
                    cursor: pointer;
                    text-align: center;
                }
                .custom-pin {
                    width: 44px;
                    height: 44px;
                    background: #1E293B;
                    border-width: 3px;
                    border-style: solid;
                    border-radius: 50%;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    font-size: 22px;
                    box-shadow: 0 4px 15px rgba(0,0,0,0.6);
                    transition: transform 0.2s cubic-bezier(0.175, 0.885, 0.32, 1.275);
                }
                .custom-pin:active, .custom-pin:hover {
                    transform: scale(1.25);
                }
                .user-marker-container {
                    position: relative;
                    width: 30px;
                    height: 30px;
                }
                .user-marker {
                    width: 20px;
                    height: 20px;
                    background: #00E5FF;
                    border: 3px solid #FFFFFF;
                    border-radius: 50%;
                    box-shadow: 0 0 20px #00E5FF;
                    position: absolute;
                    top: 5px;
                    left: 5px;
                    z-index: 2;
                }
                .user-pulse {
                    position: absolute;
                    width: 44px;
                    height: 44px;
                    background: rgba(0, 229, 255, 0.4);
                    border-radius: 50%;
                    top: -7px;
                    left: -7px;
                    animation: pulseRing 2s infinite ease-out;
                }
                @keyframes pulseRing {
                    0% { transform: scale(0.4); opacity: 1; }
                    100% { transform: scale(1.8); opacity: 0; }
                }
            </style>
        </head>
        <body>
            <div id="map"></div>
            <script>
                function calculateDistKm(lat1, lon1, lat2, lon2) {
                    var R = 6371;
                    var dLat = (lat2-lat1) * Math.PI / 180;
                    var dLon = (lon2-lon1) * Math.PI / 180;
                    var a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                            Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
                            Math.sin(dLon/2) * Math.sin(dLon/2);
                    var c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
                    return (R * c).toFixed(1);
                }

                var userLat = ${userCoords.first};
                var userLng = ${userCoords.second};

                var map = L.map('map', {
                    zoomControl: false,
                    attributionControl: false
                }).setView([userLat, userLng], 14);

                L.tileLayer('https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png', {
                    maxZoom: 19,
                    subdomains: 'abcd'
                }).addTo(map);

                // User Location
                var userIcon = L.divIcon({
                    className: 'user-marker-container',
                    html: '<div class="user-pulse"></div><div class="user-marker"></div>',
                    iconSize: [30, 30],
                    iconAnchor: [15, 15]
                });
                L.marker([userLat, userLng], {icon: userIcon}).addTo(map)
                    .bindPopup("<div class='popup-card'><b style='color:#00E5FF;'>📍 موقعك الحالي المباشر</b><br/><span style='font-size:11px;color:#94A3B8;'>يتم قياس المسافات من هنا</span></div>");

                var bounds = [ [userLat, userLng] ];
                var markers = [$markersJson];

                markers.forEach(function(item) {
                    var dist = calculateDistKm(userLat, userLng, item.lat, item.lng);
                    var customIcon = L.divIcon({
                        className: 'custom-pin-wrapper',
                        html: '<div class="custom-pin" style="border-color:' + item.badgeColor + ';">' + item.emoji + '</div>',
                        iconSize: [44, 44],
                        iconAnchor: [22, 22]
                    });

                    var m = L.marker([item.lat, item.lng], {icon: customIcon}).addTo(map);
                    bounds.push([item.lat, item.lng]);

                    var callBtnHtml = item.phone ? '<a href="tel:' + item.phone + '" class="btn-call">📞 اتصال</a>' : '';
                    var navBtnHtml = '<button onclick="AndroidBridge.openNavigation(' + item.lat + ',' + item.lng + ',\'' + item.name + '\')" class="btn-nav">🗺️ ملاحة</button>';

                    var popupHtml = "<div class='popup-card'>" +
                        "<div class='popup-title'>" + item.name + "</div>" +
                        "<div class='popup-spec'>" + item.spec + "</div>" +
                        "<div class='popup-meta'>" +
                            "<span>⭐ " + item.rating + "</span>" +
                            "<span>📏 تبعد " + dist + " كم</span>" +
                            "<span>" + item.status + "</span>" +
                        "</div>" +
                        "<div class='popup-buttons'>" +
                            "<button onclick=\"AndroidBridge.onMarkerClicked('" + item.type + "', '" + item.id + "')\" class='btn-details'>عرض التفاصيل والطلب 🔍</button>" +
                            callBtnHtml +
                            navBtnHtml +
                        "</div>" +
                        "</div>";

                    m.bindPopup(popupHtml);
                });

                if (bounds.length > 1) {
                    map.fitBounds(bounds, { padding: [60, 60], maxZoom: 16 });
                }

                window.recenterToUser = function() {
                    map.flyTo([userLat, userLng], 15, { animate: true, duration: 1 });
                };
            </script>
        </body>
        </html>
    """.trimIndent()

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                android.webkit.WebView(ctx).apply {
                    setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
                    isHapticFeedbackEnabled = true
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
                        mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    }
                    webViewClient = object : android.webkit.WebViewClient() {
                        override fun onRenderProcessGone(
                            view: android.webkit.WebView?,
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
                        fun openNavigation(lat: Double, lng: Double, label: String) {
                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                try {
                                    val gmmIntentUri = Uri.parse("geo:$lat,$lng?q=$lat,$lng($label)")
                                    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                    ctx.startActivity(mapIntent)
                                } catch (e: Exception) {
                                    Toast.makeText(ctx, "جاري الملاحة إلى: $label ($lat, $lng)", Toast.LENGTH_SHORT).show()
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
    }
}
