package com.example.ui.screens.map.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PropertyEntity
import com.example.data.ProviderEntity
import com.example.data.StoreEntity
import com.example.ui.screens.map.utils.OfflineMapManager
import com.example.utils.NavRouteResult
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * 🗺️ OfflineInteractiveMap (Full Entity Overload)
 * خريطة احتياطية مبسّطة تعرض الأماكن والشوارع والمعالم بلا إنترنت باستخدام Canvas.
 * منسّق العرض وإيماءات اللمس والتكبير، ويفوّض حساب النقاط إلى [buildInteractiveMapPoints]
 * والرسم إلى [renderOfflineMapScene].
 */
@Composable
fun OfflineInteractiveMap(
    userCoords: Pair<Double, Double>,
    hasUserLocation: Boolean = true,
    nearbyProviders: List<ProviderEntity>,
    nearbyStores: List<StoreEntity>,
    nearbyProperties: List<PropertyEntity>,
    dynamicOffsets: Map<String, Pair<Double, Double>>,
    selectedCity: String = "الكل",
    zoomScale: Float,
    onZoomScaleChange: (Float) -> Unit,
    panOffset: Offset,
    onPanOffsetChange: (Offset) -> Unit,
    selectedEntity: Any?,
    onProviderSelected: (ProviderEntity) -> Unit,
    onStoreSelected: (StoreEntity) -> Unit,
    onPropertySelected: (PropertyEntity) -> Unit,
    onDeselect: () -> Unit,
    onSwitchToRadar: (() -> Unit)? = null,
    route: NavRouteResult? = null,
    modifier: Modifier = Modifier
) {
    // [تعديل] استخدام إحداثيات GPS الحقيقية أو إحداثيات المدينة كبديل آمن ودقيق
    val defaultCityCoords = remember(selectedCity) {
        OfflineMapManager.getCityCoordinates(selectedCity)
    }
    val safeUserLat = if (userCoords.first != 0.0 && !userCoords.first.isNaN()) userCoords.first else defaultCityCoords.latitude
    val safeUserLng = if (userCoords.second != 0.0 && !userCoords.second.isNaN()) userCoords.second else defaultCityCoords.longitude

    val cityCenterCoords = remember(selectedCity, safeUserLat, safeUserLng) {
        if (selectedCity == "الكل" || selectedCity.isBlank()) {
            Pair(safeUserLat, safeUserLng)
        } else {
            val c = OfflineMapManager.getCityCoordinates(selectedCity)
            Pair(c.latitude, c.longitude)
        }
    }

    val originLat = cityCenterCoords.first
    val originLng = cityCenterCoords.second

    val metersPerDegreeLat = 110540.0
    val metersPerDegreeLng = 111320.0 * cos(Math.toRadians(originLat))

    val infiniteTransition = rememberInfiniteTransition(label = "user_pulse")
    val pulseProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_progress"
    )

    val paints = remember { OfflineMapPaintCache() }

    val mapPoints = remember(
        nearbyProviders,
        nearbyStores,
        nearbyProperties,
        originLat,
        originLng,
        selectedCity
    ) {
        buildInteractiveMapPoints(
            nearbyProviders = nearbyProviders,
            nearbyStores = nearbyStores,
            nearbyProperties = nearbyProperties,
            originLat = originLat,
            originLng = originLng,
            metersPerDegreeLat = metersPerDegreeLat,
            metersPerDegreeLng = metersPerDegreeLng,
            selectedCity = selectedCity
        )
    }

    // Reset pan & zoom smoothly when city changes
    LaunchedEffect(selectedCity) {
        onPanOffsetChange(Offset.Zero)
        onZoomScaleChange(1.0f)
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()
        val centerX = widthPx / 2f + panOffset.x
        val centerY = heightPx / 2f + panOffset.y

        val clampedZoom = zoomScale.coerceIn(0.25f, 8.0f)
        val baseMetersPerPx = 4.0f
        val metersPerPx = (baseMetersPerPx / clampedZoom).coerceIn(0.4f, 25.0f)

        val screenPoints = remember(mapPoints, centerX, centerY, metersPerPx) {
            mapPoints.map { pt ->
                pt.copy(
                    screenX = centerX + (pt.xMeters / metersPerPx),
                    screenY = centerY + (pt.yMeters / metersPerPx)
                )
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val nextZoom = (zoomScale * zoom).coerceIn(0.25f, 8.0f)
                        onZoomScaleChange(nextZoom)
                        onPanOffsetChange(panOffset + pan)
                    }
                }
                .pointerInput(screenPoints) {
                    detectTapGestures(
                        onDoubleTap = {
                            val nextZoom = (zoomScale * 1.4f).coerceIn(0.25f, 8.0f)
                            onZoomScaleChange(nextZoom)
                        },
                        onTap = { tapOffset ->
                            val clicked = screenPoints.minByOrNull { pt ->
                                val dx = tapOffset.x - pt.screenX
                                val dy = tapOffset.y - pt.screenY
                                dx * dx + dy * dy
                            }
                            if (clicked != null) {
                                val distPx = sqrt(
                                    (tapOffset.x - clicked.screenX).pow(2) +
                                        (tapOffset.y - clicked.screenY).pow(2)
                                )
                                if (distPx < 54f) {
                                    when (val ent = clicked.originalEntity) {
                                        is ProviderEntity -> onProviderSelected(ent)
                                        is StoreEntity -> onStoreSelected(ent)
                                        is PropertyEntity -> onPropertySelected(ent)
                                    }
                                } else {
                                    onDeselect()
                                }
                            } else {
                                onDeselect()
                            }
                        }
                    )
                }
        ) {
            val safeCX = if (centerX.isNaN() || centerX.isInfinite()) size.width / 2f else centerX
            val safeCY = if (centerY.isNaN() || centerY.isInfinite()) size.height / 2f else centerY
            val safeMeters = if (metersPerPx.isNaN() || metersPerPx.isInfinite() || metersPerPx <= 0f) 4.0f else metersPerPx
            val safePan = if (panOffset.x.isNaN() || panOffset.y.isNaN()) Offset.Zero else panOffset
            val safeZoom = if (clampedZoom.isNaN() || clampedZoom.isInfinite() || clampedZoom <= 0f) 1.0f else clampedZoom

            try {
                renderOfflineMapScene(
                    safeCX = safeCX,
                    safeCY = safeCY,
                    safeMeters = safeMeters,
                    safePan = safePan,
                    safeZoom = safeZoom,
                    selectedCity = selectedCity,
                    originLat = originLat,
                    originLng = originLng,
                    safeUserLat = safeUserLat,
                    safeUserLng = safeUserLng,
                    hasUserLocation = hasUserLocation,
                    metersPerDegreeLat = metersPerDegreeLat,
                    metersPerDegreeLng = metersPerDegreeLng,
                    route = route,
                    screenPoints = screenPoints,
                    selectedEntity = selectedEntity,
                    pulseProgress = pulseProgress,
                    paints = paints
                )
            } catch (e: Exception) {
                drawRect(Color(0xFF0F172A))
                drawCircle(Color(0xFF00E5FF).copy(alpha = 0.3f), 40f, Offset(size.width / 2, size.height / 2))
            }
        }

        // Top-left city and offline map indicator badge
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF1E293B).copy(alpha = 0.92f),
            border = ButtonDefaults.outlinedButtonBorder,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 16.dp, top = 80.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.Place, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(14.dp))
                Text(
                    text = if (selectedCity == "الكل") "خريطة اليمن التفاعلية 🗺️" else "خريطة $selectedCity 📍",
                    fontSize = 11.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Top-right Interactive North Compass Needle
        Surface(
            shape = CircleShape,
            color = Color(0xFF1E293B).copy(alpha = 0.92f),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 16.dp, top = 80.dp)
                .size(38.dp)
                .clickable {
                    onPanOffsetChange(Offset.Zero)
                }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Navigation,
                    contentDescription = "محاذاة للشمال",
                    tint = Color(0xFFFF5252),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Center friendly notice when no points match selected filter
        if (mapPoints.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF1E293B).copy(alpha = 0.95f),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(32.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(18.dp))
                    Text(
                        text = "لا توجد خدمات في هذه المنطقة حالياً",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Bottom-left Dynamic Cartographic Scale Ruler & Zoom Multiplier
        val rawScaleMeters = 75f * metersPerPx
        val scaleDistanceMeters = when {
            rawScaleMeters <= 75f -> 50
            rawScaleMeters <= 150f -> 100
            rawScaleMeters <= 350f -> 200
            rawScaleMeters <= 750f -> 500
            rawScaleMeters <= 1500f -> 1000
            rawScaleMeters <= 3500f -> 2000
            rawScaleMeters <= 7500f -> 5000
            rawScaleMeters <= 15000f -> 10000
            else -> 20000
        }
        val scaleBarWidthDp = ((scaleDistanceMeters / metersPerPx) * 0.85f).coerceIn(40f, 130f).dp
        val scaleLabelText = if (scaleDistanceMeters < 1000) "$scaleDistanceMeters م" else "${scaleDistanceMeters / 1000} كم"

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF1E293B).copy(alpha = 0.90f),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(start = 16.dp, bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = scaleLabelText,
                        color = Color(0xFFE2E8F0),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .width(scaleBarWidthDp)
                            .height(3.dp)
                            .background(Color(0xFF00E5FF), RoundedCornerShape(1.dp))
                    )
                }
                Text(
                    text = "${String.format(java.util.Locale.US, "%.1f", clampedZoom)}x",
                    color = Color(0xFF00E5FF),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

data class MapLabel(val text: String, val x: Float, val y: Float, val paint: android.graphics.Paint)

data class InteractiveMapPoint(
    val id: String,
    val name: String,
    val category: String,
    val type: String,
    val emoji: String,
    val color: Color,
    val xMeters: Float,
    val yMeters: Float,
    val rating: Double,
    val phone: String,
    val originalEntity: Any,
    val isApprox: Boolean = false,
    val screenX: Float = 0f,
    val screenY: Float = 0f
)
