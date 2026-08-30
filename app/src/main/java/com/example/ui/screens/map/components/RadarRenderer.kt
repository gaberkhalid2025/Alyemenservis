package com.example.ui.screens.map.components

import androidx.compose.animation.core.*
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.*

/**
 * 📡 RadarRenderer
 * High-performance radar scanner Canvas with concentric distance rings, sweep line,
 * dual pulse ripples with 800ms offset, fade-out alpha, and cached clustering.
 */
@Composable
fun RadarRenderer(
    items: List<MarkerRenderer.MapItemPoint>,
    selectedItemId: String?,
    onItemSelected: (MarkerRenderer.MapItemPoint) -> Unit,
    isHeatmapActive: Boolean,
    maxRangeKm: Float,
    pulseColor: Color = Color(0xFF00E5FF),
    pulseCycleDurationMs: Int = 2500,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "radar_anim")
    
    // Rotating sweep line (3.5s cycle)
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep_angle"
    )

    // Pulse 1: Primary expanding wave (2.5s cycle with FastOutSlowInEasing)
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(pulseCycleDurationMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_radius_1"
    )

    // Pulse 2: Secondary wave with exactly 800ms offset
    val pulseRadius2 by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(pulseCycleDurationMs, delayMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_radius_2"
    )

    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()

        val centerX = widthPx / 2f + panOffset.x
        val centerY = heightPx / 2f + panOffset.y
        val maxRadius = min(widthPx, heightPx) * 0.44f * zoomScale

        // Performance Optimization: Cache screen items and clusters so they are NOT recalculated in every 60fps animation frame
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
                        zoomScale = (zoomScale * zoom).coerceIn(0.5f, 3.5f)
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
            // 1. Dark Background Gradient
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF0F172A), Color(0xFF060B18), Color(0xFF020617)),
                    center = Offset(centerX, centerY),
                    radius = maxRadius * 1.3f
                ),
                center = Offset(centerX, centerY),
                radius = maxRadius * 1.25f
            )

            // 2. Concentric Radar Rings
            val rings = 4
            for (i in 1..rings) {
                val ringRadius = maxRadius * (i.toFloat() / rings)
                drawCircle(
                    color = pulseColor.copy(alpha = 0.18f),
                    radius = ringRadius,
                    center = Offset(centerX, centerY),
                    style = Stroke(width = 1.2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f)))
                )
            }

            // 3. Crosshairs
            drawLine(
                color = pulseColor.copy(alpha = 0.22f),
                start = Offset(centerX - maxRadius, centerY),
                end = Offset(centerX + maxRadius, centerY),
                strokeWidth = 1f
            )
            drawLine(
                color = pulseColor.copy(alpha = 0.22f),
                start = Offset(centerX, centerY - maxRadius),
                end = Offset(centerX, centerY + maxRadius),
                strokeWidth = 1f
            )

            // 4. Expanding Radar Pulses with Alpha Fade-Out
            val alpha1 = ((1.0f - pulseRadius) * 0.35f).coerceIn(0f, 1f)
            drawCircle(
                color = pulseColor.copy(alpha = alpha1),
                radius = maxRadius * pulseRadius,
                center = Offset(centerX, centerY),
                style = Stroke(width = 2.2f)
            )

            val alpha2 = ((1.0f - pulseRadius2) * 0.28f).coerceIn(0f, 1f)
            drawCircle(
                color = pulseColor.copy(alpha = alpha2),
                radius = maxRadius * pulseRadius2,
                center = Offset(centerX, centerY),
                style = Stroke(width = 1.8f)
            )

            // 5. Rotating Radar Sweep Line
            val rad = Math.toRadians(sweepAngle.toDouble())
            val sweepEndX = (centerX + maxRadius * cos(rad)).toFloat()
            val sweepEndY = (centerY + maxRadius * sin(rad)).toFloat()
            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(pulseColor.copy(alpha = 0.9f), pulseColor.copy(alpha = 0.15f), Color.Transparent),
                    start = Offset(centerX, centerY),
                    end = Offset(sweepEndX, sweepEndY)
                ),
                start = Offset(centerX, centerY),
                end = Offset(sweepEndX, sweepEndY),
                strokeWidth = 3f
            )

            // 6. Heatmap Layer (if enabled)
            if (isHeatmapActive && weightedPoints.isNotEmpty()) {
                HeatmapRenderer.drawHeatmapLayer(
                    drawScope = this,
                    points = weightedPoints,
                    bandwidth = 45f * zoomScale
                )
            }

            // 7. Cluster & Draw Items (Optimized using cached clusters)
            val paint = android.graphics.Paint().apply {
                textSize = 28f
                textAlign = android.graphics.Paint.Align.CENTER
            }
            for (cluster in clusters) {
                val isSelected = cluster.items.any { it.id == selectedItemId }
                val count = cluster.items.size
                val center = Offset(cluster.centerX, cluster.centerY)
                
                if (count == 1) {
                    val item = cluster.items.first()
                    val emoji = MarkerRenderer.getEmojiForType(item.type)
                    val color = MarkerRenderer.getColorForType(item.type)
                    
                    if (isSelected) {
                        drawCircle(color = color.copy(alpha = 0.4f), radius = 24f, center = center)
                    }
                    
                    drawContext.canvas.nativeCanvas.drawText(
                        emoji,
                        center.x,
                        center.y + (paint.textSize / 3),
                        paint
                    )
                } else {
                    MarkerRenderer.drawCluster(
                        drawScope = this,
                        cluster = cluster,
                        isSelected = isSelected
                    )
                }
            }

            // 8. User Center Point (Glowing Green Beacon)
            drawCircle(
                color = Color(0xFF10B981).copy(alpha = 0.25f),
                radius = 20f,
                center = Offset(centerX, centerY)
            )
            drawCircle(
                color = Color(0xFF10B981),
                radius = 8f,
                center = Offset(centerX, centerY)
            )
            drawCircle(
                color = Color.White,
                radius = 3.5f,
                center = Offset(centerX, centerY)
            )
        }
    }
}
