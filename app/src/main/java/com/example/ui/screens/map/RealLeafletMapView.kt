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
 * خريطة تفاعلية متطورة تعتمد على OpenStreetMap مع دعم Marker Clustering الكامل
 * وألوان تمييزية حسب نوع الخدمة (فنيون، متاجر، مطاعم، مراكز طبية، عقارات)
 * وحساب وقت الوصول التقديري (ETA) وأزرار توجيه عبر Google Maps
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

    // Build JSON data for Leaflet with distinct categories & colors
    val providerMarkers = nearbyProviders.filter { it.latitude != 0.0 || it.longitude != 0.0 || it.cityId.isNotBlank() || it.area.isNotBlank() }.map { p ->
        val baseCoords = getProviderCoords(p)
        val walkOffset = dynamicOffsets[p.id] ?: Pair(0.0, 0.0)
        val liveLat = baseCoords.first + walkOffset.first
        val liveLng = baseCoords.second + walkOffset.second
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
            badgeColor: "#00E5FF",
            serviceCategory: "فنيون صيانة"
        }
        """
    }

    val storeMarkers = nearbyStores.filter { it.latitude != 0.0 && it.longitude != 0.0 }.map { s ->
        val isMedical = s.sectionId.contains("medical") || s.categoryId.contains("medical") || s.name.contains("طبي") || s.name.contains("مستشفى") || s.name.contains("عيادة") || s.name.contains("صيدلية")
        val isRestaurant = s.sectionId.contains("restaurant") || s.categoryId.contains("restaurant") || s.name.contains("مطعم") || s.name.contains("مأكولات") || s.name.contains("كافيه") || s.name.contains("شاورما")
        
        val emoji = when {
            isMedical -> "🏥"
            isRestaurant -> "🍔"
            s.sectionId.contains("supermarket") || s.name.contains("بقالة") || s.name.contains("هايبر") -> "🛒"
            else -> "🏪"
        }
        val safeName = s.name.replace("\"", "\\\"").replace("'", "\\'")
        val safeDesc = s.description.replace("\"", "\\\"").replace("'", "\\'").take(60)
        val cleanDesc = if (safeDesc.isEmpty()) "محل / متجر معتمد" else safeDesc
        val safePhone = s.phone.replace("\"", "")
        
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
            lat: ${s.latitude},
            lng: ${s.longitude},
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

    val propertyMarkers = nearbyProperties.filter { it.latitude != 0.0 && it.longitude != 0.0 }.map { pr ->
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
            badgeColor: "#8B5CF6",
            serviceCategory: "عقارات وشقق"
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
            <link rel="stylesheet" href="https://unpkg.com/leaflet.markercluster@1.5.3/dist/MarkerCluster.css" />
            <link rel="stylesheet" href="https://unpkg.com/leaflet.markercluster@1.5.3/dist/MarkerCluster.Default.css" />
            <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
            <script src="https://unpkg.com/leaflet.markercluster@1.5.3/dist/leaflet.markercluster.js"></script>
            <style>
                * {
                    box-sizing: border-box;
                    margin: 0;
                    padding: 0;
                }
                html, body, #map {
                    height: 100%;
                    width: 100%;
                    background: #0B0F19;
                    font-family: system-ui, -apple-system, sans-serif;
                    overflow: hidden;
                }
                .leaflet-tile-container img {
                    filter: brightness(0.88) contrast(1.08);
                }
                .leaflet-popup-content-wrapper {
                    background: #1E293B !important;
                    color: #FFFFFF !important;
                    border: 2px solid #00E5FF;
                    border-radius: 16px;
                    padding: 4px;
                    box-shadow: 0 12px 30px rgba(0,0,0,0.8);
                }
                .leaflet-popup-tip {
                    background: #1E293B !important;
                }
                .popup-card {
                    font-family: system-ui, -apple-system, sans-serif;
                    text-align: right;
                    direction: rtl;
                    color: #FFFFFF;
                    padding: 8px 6px;
                    min-width: 210px;
                }
                .popup-cat-badge {
                    display: inline-block;
                    font-size: 10px;
                    padding: 2px 8px;
                    border-radius: 12px;
                    background: rgba(255,255,255,0.1);
                    margin-bottom: 4px;
                    font-weight: 600;
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
                    flex-direction: column;
                    gap: 3px;
                    font-size: 11px;
                    margin-bottom: 10px;
                    color: #94A3B8;
                    border-top: 1px solid rgba(255,255,255,0.1);
                    border-bottom: 1px solid rgba(255,255,255,0.1);
                    padding: 6px 0;
                }
                .popup-meta-row {
                    display: flex;
                    justify-content: space-between;
                }
                .popup-eta {
                    color: #38BDF8;
                    font-weight: bold;
                }
                .popup-buttons {
                    display: flex;
                    flex-direction: column;
                    gap: 6px;
                }
                .popup-buttons-row {
                    display: flex;
                    gap: 6px;
                }
                .btn-details {
                    width: 100%;
                    background: linear-gradient(135deg, #00E5FF, #00B4D8);
                    color: #0F172A;
                    border: none;
                    padding: 9px 10px;
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
                    display: inline-block;
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
                    transform: scale(1.22);
                }
                .user-marker-container {
                    position: relative;
                    width: 32px;
                    height: 32px;
                }
                .user-marker {
                    width: 22px;
                    height: 22px;
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
                    width: 46px;
                    height: 46px;
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
                .marker-cluster-small {
                    background-color: rgba(0, 229, 255, 0.4);
                }
                .marker-cluster-small div {
                    background-color: #00E5FF;
                    color: #0F172A;
                    font-weight: 800;
                }
                .marker-cluster-medium {
                    background-color: rgba(245, 158, 11, 0.4);
                }
                .marker-cluster-medium div {
                    background-color: #F59E0B;
                    color: #0F172A;
                    font-weight: 800;
                }
                .marker-cluster-large {
                    background-color: rgba(236, 72, 153, 0.4);
                }
                .marker-cluster-large div {
                    background-color: #EC4899;
                    color: #FFFFFF;
                    font-weight: 800;
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
                    return (R * c);
                }

                function computeEtaText(distKm) {
                    if (distKm < 1.0) {
                        var mins = Math.max(1, Math.round(distKm / 4.5 * 60));
                        return '⏱️ ~ ' + mins + ' دقيقة سيراً (' + Math.round(distKm * 1000) + ' م)';
                    } else {
                        var mins = Math.max(2, Math.round(distKm / 35.0 * 60));
                        return '🚘 ~ ' + mins + ' دقيقة بالسيارة (' + distKm.toFixed(1) + ' كم)';
                    }
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

                // User Location Marker
                var userIcon = L.divIcon({
                    className: 'user-marker-container',
                    html: '<div class="user-pulse"></div><div class="user-marker"></div>',
                    iconSize: [32, 32],
                    iconAnchor: [16, 16]
                });
                L.marker([userLat, userLng], {icon: userIcon, zIndexOffset: 1000}).addTo(map)
                    .bindPopup("<div class='popup-card'><b style='color:#00E5FF;'>📍 موقعك الحالي المباشر</b><br/><span style='font-size:11px;color:#94A3B8;'>نقطة ارتكاز البحث وحساب المسافات</span></div>");

                // Initialize Marker Cluster Group
                var clusterGroup = L.markerClusterGroup ? L.markerClusterGroup({
                    chunkedLoading: true,
                    maxClusterRadius: 40,
                    spiderfyOnMaxZoom: true,
                    showCoverageOnHover: false
                }) : null;

                var bounds = [ [userLat, userLng] ];
                var rawMarkers = [$markersJson];

                rawMarkers.forEach(function(item) {
                    if (!item.lat || !item.lng) return;
                    var distNum = calculateDistKm(userLat, userLng, item.lat, item.lng);
                    var etaString = computeEtaText(distNum);

                    var customIcon = L.divIcon({
                        className: 'custom-pin-wrapper',
                        html: '<div class="custom-pin" style="border-color:' + item.badgeColor + '; box-shadow: 0 0 12px ' + item.badgeColor + '66;">' + item.emoji + '</div>',
                        iconSize: [44, 44],
                        iconAnchor: [22, 22]
                    });

                    var m = L.marker([item.lat, item.lng], {icon: customIcon});
                    bounds.push([item.lat, item.lng]);

                    var callBtnHtml = item.phone ? '<a href="tel:' + item.phone + '" class="btn-call">📞 اتصال</a>' : '';
                    var navBtnHtml = '<button onclick="AndroidBridge.openNavigation(' + item.lat + ',' + item.lng + ',\'' + item.name + '\')" class="btn-nav">🗺️ اتجاهات</button>';

                    var popupHtml = "<div class='popup-card'>" +
                        "<div class='popup-cat-badge' style='color:" + item.badgeColor + "; border: 1px solid " + item.badgeColor + "44;'>" + item.serviceCategory + "</div>" +
                        "<div class='popup-title'>" + item.name + "</div>" +
                        "<div class='popup-spec'>" + item.spec + "</div>" +
                        "<div class='popup-meta'>" +
                            "<div class='popup-meta-row'><span>⭐ تقييم: " + item.rating + "</span><span>" + item.status + "</span></div>" +
                            "<div class='popup-eta'>" + etaString + "</div>" +
                        "</div>" +
                        "<div class='popup-buttons'>" +
                            "<button onclick=\"AndroidBridge.onMarkerClicked('" + item.type + "', '" + item.id + "')\" class='btn-details'>عرض التفاصيل والطلب 🔍</button>" +
                            "<div class='popup-buttons-row'>" +
                                callBtnHtml +
                                navBtnHtml +
                            "</div>" +
                        "</div>" +
                        "</div>";

                    m.bindPopup(popupHtml);

                    if (clusterGroup) {
                        clusterGroup.addLayer(m);
                    } else {
                        m.addTo(map);
                    }
                });

                if (clusterGroup) {
                    map.addLayer(clusterGroup);
                }

                if (bounds.length > 1) {
                    map.fitBounds(bounds, { padding: [50, 50], maxZoom: 15 });
                }

                window.recenterToUser = function() {
                    map.flyTo([userLat, userLng], 15, { animate: true, duration: 0.8 });
                };

                window.setMapLocation = function(newLat, newLng) {
                    userLat = newLat;
                    userLng = newLng;
                    map.flyTo([newLat, newLng], 15, { animate: true, duration: 0.8 });
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
                                    val gmmIntentUri = Uri.parse("google.navigation:q=$lat,$lng")
                                    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                                        setPackage("com.google.android.apps.maps")
                                    }
                                    if (mapIntent.resolveActivity(ctx.packageManager) != null) {
                                        ctx.startActivity(mapIntent)
                                    } else {
                                        val browserUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$lat,$lng")
                                        ctx.startActivity(Intent(Intent.ACTION_VIEW, browserUri))
                                    }
                                } catch (e: Exception) {
                                    try {
                                        val fallbackUri = Uri.parse("geo:$lat,$lng?q=$lat,$lng($label)")
                                        ctx.startActivity(Intent(Intent.ACTION_VIEW, fallbackUri))
                                    } catch (ex: Exception) {
                                        Toast.makeText(ctx, "جاري فتح الخريطة: $label", Toast.LENGTH_SHORT).show()
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
    }
}
