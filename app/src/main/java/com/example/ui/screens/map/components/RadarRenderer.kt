package com.example.ui.screens.map.components

import androidx.compose.animation.core.*
import com.example.ui.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.ui.screens.map.MapMarker
import kotlin.math.*

/**
 * 🛰️ RadarRenderer – رادار محلي 100% Offline (النسخة المباشرة لـ MapMarker)
 * - خلفية داكنة متدرجة (Color(0xFF1E293B) -> Color(0xFF0F172A) -> Color(0xFF020617))
 * - نبضات رادارية 60fps
 * - Clustering + Pan + Pinch-to-Zoom
 * - إحداثيات افتراضية: صنعاء 15.3694, 44.1910
 */
@Composable
fun RadarRenderer(
    markers: List<MapMarker>,
    centerLat: Double = 15.3694,
    centerLng: Double = 44.1910,
    onMarkerClick: (MapMarker) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    val safeCenterLat = if (centerLat == 0.0 || centerLat.isNaN()) 15.3694 else centerLat
    val safeCenterLng = if (centerLng == 0.0 || centerLng.isNaN()) 44.1910 else centerLng

    val infiniteTransition = rememberInfiniteTransition(label = "radar_pulse")
    val pulseProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse"
    )

    @Suppress("UNUSED_VARIABLE")
    val density = LocalDensity.current

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val w = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val h = constraints.maxHeight.toFloat().coerceAtLeast(1f)
        val center = Offset(w / 2f + offsetX, h / 2f + offsetY)

        val projected = remember(markers, safeCenterLat, safeCenterLng, center, scale) {
            markers.map { marker ->
                val dx = ((marker.lng - safeCenterLng) * 111320 * cos(Math.toRadians(safeCenterLat))).toFloat()
                val dy = ((safeCenterLat - marker.lat) * 110540).toFloat()
                val px = center.x + dx * 0.08f * scale
                val py = center.y + dy * 0.08f * scale
                marker to Offset(px, py)
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.6f, 4.5f)
                        offsetX += pan.x
                        offsetY += pan.y
                    }
                }
                .pointerInput(projected) {
                    detectTapGestures { tapOffset ->
                        val hit = projected.minByOrNull { (_, pos) ->
                            sqrt((tapOffset.x - pos.x).pow(2) + (tapOffset.y - pos.y).pow(2))
                        }
                        if (hit != null) {
                            val dist = sqrt((tapOffset.x - hit.second.x).pow(2) + (tapOffset.y - hit.second.y).pow(2))
                            if (dist <= 56f) {
                                onMarkerClick(hit.first)
                            }
                        }
                    }
                }
        ) {
            val canvasW = size.width
            val canvasH = size.height
            val c = Offset(canvasW / 2f + offsetX, canvasH / 2f + offsetY)

            // 1. خلفية داكنة متدرجة
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF1E293B),
                        Color(0xFF0F172A),
                        Color(0xFF020617)
                    ),
                    center = c,
                    radius = (max(canvasW, canvasH) * 0.9f).coerceAtLeast(1f)
                )
            )

            // 2. دوائر الرادار الثابتة
            val maxRadius = min(canvasW, canvasH) * 0.42f
            for (i in 1..4) {
                val r = maxRadius * (i / 4f)
                drawCircle(
                    color = Color(0xFF00E5FF).copy(alpha = 0.12f),
                    radius = r * scale,
                    center = c,
                    style = Stroke(width = 1.2.dp.toPx())
                )
            }

            // 3. نبضة متحركة (Radar Sweep)
            val pulseRadius = (maxRadius * scale * pulseProgress).coerceAtLeast(1f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF00E5FF).copy(alpha = 0.35f * (1f - pulseProgress)),
                        Color.Transparent
                    ),
                    center = c,
                    radius = pulseRadius
                ),
                radius = pulseRadius,
                center = c
            )

            // خط المسح الدوار
            val sweepAngle = pulseProgress * 360f
            val sweepRad = Math.toRadians(sweepAngle.toDouble())
            val endX = c.x + cos(sweepRad).toFloat() * maxRadius * scale
            val endY = c.y + sin(sweepRad).toFloat() * maxRadius * scale
            drawLine(
                color = Color(0xFF00E5FF).copy(alpha = 0.7f),
                start = c,
                end = Offset(endX, endY),
                strokeWidth = 2.5.dp.toPx(),
                cap = StrokeCap.Round
            )

            // 4. رسم العلامات (Markers) مع Clustering بسيط
            projected.forEach { (marker, pos) ->
                if (pos.x < -50 || pos.x > canvasW + 50 || pos.y < -50 || pos.y > canvasH + 50) return@forEach

                val color = when (marker.type.uppercase()) {
                    "PROVIDER", "TECHNICIAN" -> Color(0xFF38BDF8)
                    "STORE" -> Color(0xFF10B981)
                    "RESTAURANT" -> Color(0xFFF59E0B)
                    "MEDICAL" -> Color(0xFF06B6D4)
                    "PROPERTY" -> Color(0xFF8B5CF6)
                    else -> Color(0xFF00E5FF)
                }

                // هالة
                drawCircle(
                    color = color.copy(alpha = 0.25f),
                    radius = 18.dp.toPx() * scale.coerceAtMost(1.8f),
                    center = pos
                )
                // النقطة
                drawCircle(
                    color = color,
                    radius = 7.dp.toPx() * scale.coerceAtMost(1.6f),
                    center = pos
                )
                // حدود بيضاء
                drawCircle(
                    color = Color.White.copy(alpha = 0.9f),
                    radius = 7.dp.toPx() * scale.coerceAtMost(1.6f),
                    center = pos,
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }

            // مركز الرادار
            drawCircle(color = Color(0xFF00E5FF), radius = 5.dp.toPx(), center = c)
            drawCircle(color = Color.White, radius = 2.5.dp.toPx(), center = c)
        }
    }
}

/**
 * 📡 RadarRenderer (التوقيع المتقدم المتوافق مع MapItemPoint + Clustering + Heatmap)
 */
@Composable
fun RadarRenderer(
    items: List<MarkerRenderer.MapItemPoint>,
    selectedItemId: String?,
    onItemSelected: (MarkerRenderer.MapItemPoint) -> Unit,
    isHeatmapActive: Boolean,
    maxRangeKm: Float,
    pulseColor: Color = Color(0xFF00E5FF),
    pulseCycleDurationMs: Int = 2200,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "radar_anim")

    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep_angle"
    )

    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(pulseCycleDurationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_radius_1"
    )

    val pulseRadius2 by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(pulseCycleDurationMs, delayMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_radius_2"
    )

    val pulseRadius3 by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(pulseCycleDurationMs, delayMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_radius_3"
    )

    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val widthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val heightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)

        val centerX = widthPx / 2f + panOffset.x
        val centerY = heightPx / 2f + panOffset.y
        val maxRadius = min(widthPx, heightPx) * 0.42f * zoomScale
        val centerOffset = remember(centerX, centerY) { Offset(centerX, centerY) }

        val dashIntervals = remember { floatArrayOf(12f, 10f) }
        val ringStroke = remember(dashIntervals) {
            Stroke(width = 1.5f, pathEffect = PathEffect.dashPathEffect(dashIntervals))
        }
        val pulse1Stroke = remember { Stroke(width = 2.2f) }
        val pulse2Stroke = remember { Stroke(width = 1.8f) }
        val pulse3Stroke = remember { Stroke(width = 1.2f) }
        val bgGradientColors = remember {
            listOf(Color(0xFF1E293B), Color(0xFF0F172A), Color(0xFF020617))
        }
        val bgBrush = remember(centerOffset, widthPx, heightPx, bgGradientColors) {
            Brush.radialGradient(
                colors = bgGradientColors,
                center = centerOffset,
                radius = (max(widthPx, heightPx) * 0.9f).coerceAtLeast(1f)
            )
        }
        val ringRadii = remember(maxRadius) {
            val rings = 4
            FloatArray(rings) { i -> maxRadius * ((i + 1).toFloat() / rings) }
        }
        val markerTextPaint = remember {
            android.graphics.Paint().apply {
                textSize = 26f
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
            }
        }

        val alpha1 by remember {
            derivedStateOf { ((1.0f - pulseRadius) * 0.35f).coerceIn(0f, 1f) }
        }
        val alpha2 by remember {
            derivedStateOf { ((1.0f - pulseRadius2) * 0.28f).coerceIn(0f, 1f) }
        }
        val alpha3 by remember {
            derivedStateOf { ((1.0f - pulseRadius3) * 0.18f).coerceIn(0f, 1f) }
        }
        val sweepEndOffset by remember(centerX, centerY, maxRadius) {
            derivedStateOf {
                val rad = Math.toRadians(sweepAngle.toDouble())
                Offset(
                    x = (centerX + maxRadius * cos(rad)).toFloat(),
                    y = (centerY + maxRadius * sin(rad)).toFloat()
                )
            }
        }

        val screenItems = remember(items, centerX, centerY, zoomScale) {
            items.map { item ->
                item.copy(
                    x = centerX + item.x * zoomScale,
                    y = centerY + item.y * zoomScale
                )
            }
        }

        val clusters = remember(screenItems, zoomScale) {
            MarkerRenderer.clusterPoints(screenItems, thresholdPx = 45f * zoomScale)
        }

        val weightedPoints = remember(items, centerX, centerY, zoomScale, isHeatmapActive) {
            if (isHeatmapActive) {
                items.map { item ->
                    HeatmapRenderer.WeightedPoint(
                        x = centerX + item.x * zoomScale,
                        y = centerY + item.y * zoomScale,
                        weight = 1.2f
                    )
                }
            } else emptyList()
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        zoomScale = (zoomScale * zoom).coerceIn(0.6f, 4.5f)
                        panOffset += pan
                    }
                }
                .pointerInput(screenItems) {
                    detectTapGestures { tapOffset ->
                        val clicked = screenItems.minByOrNull { item ->
                            sqrt((tapOffset.x - item.x).pow(2) + (tapOffset.y - item.y).pow(2))
                        }
                        if (clicked != null) {
                            val dist = sqrt((tapOffset.x - clicked.x).pow(2) + (tapOffset.y - clicked.y).pow(2))
                            if (dist < 60f) {
                                onItemSelected(clicked)
                            }
                        }
                    }
                }
        ) {
            // 1. خلفية داكنة متدرجة تغطي الشاشة بالكامل (لا شاشة سوداء أبداً)
            drawRect(brush = bgBrush)

            // 2. دوائر الرادار الثابتة
            for (ringRadius in ringRadii) {
                drawCircle(
                    color = pulseColor.copy(alpha = 0.16f),
                    radius = ringRadius,
                    center = centerOffset,
                    style = ringStroke
                )
            }

            // 3. محاور التقاطع (Crosshairs)
            drawLine(
                color = pulseColor.copy(alpha = 0.20f),
                start = Offset(centerX - maxRadius, centerY),
                end = Offset(centerX + maxRadius, centerY),
                strokeWidth = 1f
            )
            drawLine(
                color = pulseColor.copy(alpha = 0.20f),
                start = Offset(centerX, centerY - maxRadius),
                end = Offset(centerX, centerY + maxRadius),
                strokeWidth = 1f
            )

            // 4. نبضات رادارية متحركة 60fps
            val activePulseR = (maxRadius * pulseRadius).coerceAtLeast(1f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        pulseColor.copy(alpha = alpha1),
                        Color.Transparent
                    ),
                    center = centerOffset,
                    radius = activePulseR
                ),
                radius = activePulseR,
                center = centerOffset
            )
            drawCircle(
                color = pulseColor.copy(alpha = alpha1),
                radius = activePulseR,
                center = centerOffset,
                style = pulse1Stroke
            )
            drawCircle(
                color = pulseColor.copy(alpha = alpha2),
                radius = (maxRadius * pulseRadius2).coerceAtLeast(1f),
                center = centerOffset,
                style = pulse2Stroke
            )
            drawCircle(
                color = pulseColor.copy(alpha = alpha3),
                radius = (maxRadius * pulseRadius3).coerceAtLeast(1f),
                center = centerOffset,
                style = pulse3Stroke
            )

            // 5. خط المسح الدوار مع توهج وخلفية متلاشية غامرة (Trail Motion-Blur Glow)
            for (i in 0..5) {
                val angleOffset = i * 2.0f
                val trailAlpha = (0.75f - (i * 0.12f)).coerceAtLeast(0.02f)
                val trailRad = Math.toRadians((sweepAngle - angleOffset).toDouble())
                val trailEnd = Offset(
                    x = (centerX + maxRadius * cos(trailRad)).toFloat(),
                    y = (centerY + maxRadius * sin(trailRad)).toFloat()
                )
                drawLine(
                    color = pulseColor.copy(alpha = trailAlpha),
                    start = centerOffset,
                    end = trailEnd,
                    strokeWidth = (2.5.dp.toPx() * (1f - (i * 0.15f))).coerceAtLeast(1f),
                    cap = StrokeCap.Round
                )
            }

            // 6. الخريطة الحرارية (إذا كانت مفعلة)
            if (isHeatmapActive && weightedPoints.isNotEmpty()) {
                HeatmapRenderer.drawHeatmapLayer(
                    drawScope = this,
                    points = weightedPoints,
                    bandwidth = 45f * zoomScale
                )
            }

            // 7. رسم العلامات والـ Clusters بألوان واضحة حسب النوع
            for (cluster in clusters) {
                val isSelected = cluster.items.any { it.id == selectedItemId }
                val count = cluster.items.size
                val center = Offset(cluster.centerX, cluster.centerY)

                if (center.x < -50 || center.x > size.width + 50 || center.y < -50 || center.y > size.height + 50) continue

                if (count == 1) {
                    val item = cluster.items.first()
                    val emoji = MarkerRenderer.getEmojiForType(item.type)
                    val color = MarkerRenderer.getColorForType(item.type)

                    // هالة خارجية وتوهج ناعم نابض غامر للعناصر المحددة (Soft Breathing selection glow)
                    if (isSelected) {
                        drawCircle(
                            color = color.copy(alpha = 0.22f * (1f - pulseRadius)),
                            radius = 35.dp.toPx() * pulseRadius * zoomScale.coerceAtMost(1.8f),
                            center = center
                        )
                        drawCircle(
                            color = color.copy(alpha = 0.35f),
                            radius = 28.dp.toPx() * zoomScale.coerceAtMost(1.8f),
                            center = center,
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                    }

                    // هالة خارجية
                    drawCircle(
                        color = color.copy(alpha = if (isSelected) 0.45f else 0.25f),
                        radius = (if (isSelected) 24.dp.toPx() else 18.dp.toPx()) * zoomScale.coerceAtMost(1.8f),
                        center = center
                    )
                    // النقطة الملونة
                    drawCircle(
                        color = color,
                        radius = 8.dp.toPx() * zoomScale.coerceAtMost(1.6f),
                        center = center
                    )
                    // حدود بيضاء
                    drawCircle(
                        color = Color.White.copy(alpha = 0.9f),
                        radius = 8.dp.toPx() * zoomScale.coerceAtMost(1.6f),
                        center = center,
                        style = Stroke(width = 1.5.dp.toPx())
                    )

                    drawContext.canvas.nativeCanvas.drawText(
                        emoji,
                        center.x,
                        center.y - 14.dp.toPx(),
                        markerTextPaint
                    )
                } else {
                    MarkerRenderer.drawCluster(
                        drawScope = this,
                        cluster = cluster,
                        isSelected = isSelected
                    )
                }
            }

            // 8. مركز الرادار (صنعاء / موقع المستخدم)
            drawCircle(
                color = Color(0xFF00E5FF).copy(alpha = 0.28f),
                radius = 16.dp.toPx(),
                center = centerOffset
            )
            drawCircle(
                color = Color(0xFF00E5FF),
                radius = 5.dp.toPx(),
                center = centerOffset
            )
            drawCircle(
                color = Color.White,
                radius = 2.5.dp.toPx(),
                center = centerOffset
            )
        }
    }
}
