package com.example.ui

import android.annotation.SuppressLint
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.ProviderEntity

/**
 * 🗺️ FreeMapsAndLiveRadarView:
 * Implements OpenStreetMap (Osmdroid free open source engine) + Canvas-based pulse radar ripple animation.
 */
@Composable
fun FreeMapsAndLiveRadarView(
    userLat: Double,
    userLng: Double,
    nearbyProviders: List<ProviderEntity>,
    onSelectProvider: (ProviderEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        // 1. OpenStreetMap Web Engine Layer
        OsmMapViewLayer(
            userLat = userLat,
            userLng = userLng,
            providers = nearbyProviders,
            onSelectProvider = onSelectProvider
        )

        // 2. Canvas-based Live Pulse Radar Overlay in Center
        CanvasRadarRippleOverlay(
            modifier = Modifier.fillMaxSize()
        )

        // Top Status Header
        Surface(
            color = Color(0xFF0F172A).copy(alpha = 0.85f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("📡 رادار البحث المباشر: ", fontSize = 12.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                Text("جاري مسح المزودين القريبين (${nearbyProviders.size})", fontSize = 12.sp, color = Color.White)
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun OsmMapViewLayer(
    userLat: Double,
    userLng: Double,
    providers: List<ProviderEntity>,
    onSelectProvider: (ProviderEntity) -> Unit
) {
    val centerLat = if (userLat != 0.0) userLat else 15.3694
    val centerLng = if (userLng != 0.0) userLng else 44.1910

    val markersJs = StringBuilder()
    providers.forEach { p ->
        val pLat = if (p.latitude != 0.0) p.latitude else centerLat + (Math.random() - 0.5) * 0.03
        val pLng = if (p.longitude != 0.0) p.longitude else centerLng + (Math.random() - 0.5) * 0.03
        markersJs.append("""
            L.marker([$pLat, $pLng]).addTo(map)
                .bindPopup("<b>🔧 ${p.name}</b><br>${p.customCategoryName}<br><a href='app://provider/${p.id}'>اختر الفني</a>");
        """.trimIndent())
    }

    val htmlContent = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0" />
            <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
            <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
            <style>
                body { margin: 0; padding: 0; background: #0f172a; }
                #map { width: 100vw; height: 100vh; }
            </style>
        </head>
        <body>
            <div id="map"></div>
            <script>
                var map = L.map('map').setView([$centerLat, $centerLng], 14);
                L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                    maxZoom: 19,
                    attribution: '© OpenStreetMap'
                }).addTo(map);
                L.circle([$centerLat, $centerLng], {
                    color: '#10B981',
                    fillColor: '#10B981',
                    fillOpacity: 0.25,
                    radius: 1200
                }).addTo(map);
                $markersJs
            </script>
        </body>
        </html>
    """.trimIndent()

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                        if (url != null && url.startsWith("app://provider/")) {
                            val id = url.removePrefix("app://provider/")
                            providers.find { it.id == id }?.let { onSelectProvider(it) }
                            return true
                        }
                        return false
                    }
                }
                loadDataWithBaseURL("https://openstreetmap.org", htmlContent, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL("https://openstreetmap.org", htmlContent, "text/html", "UTF-8", null)
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun CanvasRadarRippleOverlay(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "RadarRipple")
    
    val radiusRatio by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RippleAnimation"
    )

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxRadius = size.width.coerceAtMost(size.height) * 0.4f

        val currentRadius = maxRadius * radiusRatio
        val alpha = (1f - radiusRatio).coerceIn(0f, 1f)

        // Draw animated pulse ripple circle
        drawCircle(
            color = Color(0xFF10B981).copy(alpha = alpha * 0.5f),
            radius = currentRadius,
            center = center,
            style = Stroke(width = 3.dp.toPx())
        )

        // Outer static boundary radar circle
        drawCircle(
            color = Color(0xFF10B981).copy(alpha = 0.2f),
            radius = maxRadius,
            center = center,
            style = Stroke(width = 1.dp.toPx())
        )

        // Center location blip dot
        drawCircle(
            color = Color(0xFF10B981),
            radius = 6.dp.toPx(),
            center = center
        )
    }
}
