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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.*

/**
 * 📡 RadarRenderer
 * High-performance radar scanner Canvas with concentric distance rings, sweep line,
 * pulse ripples, and interactive touch navigation.
 */
@Composable
fun RadarRenderer(
    items: List<MarkerRenderer.MapItemPoint>,
    selectedItemId: String?,
    onItemSelected: (MarkerRenderer.MapItemPoint) -> Unit,
    isHeatmapActive: Boolean,
    maxRangeKm: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "radar_anim")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep_angle"
    )

    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_radius"
    )

    val pulseRadius2 by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, delayMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_radius_2"
    )

    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        zoomScale = (zoomScale * zoom).coerceIn(0.5f, 3.5f)
                        panOffset += pan
                    }
                }
                .pointerInput(items, zoomScale, panOffset, widthPx, heightPx) {
                    detectTapGestures { tapOffset ->
                        val cX = widthPx / 2f + panOffset.x
                        val cY = heightPx / 2f + panOffset.y
                        // Find closest item within 50px of tap
                        val clicked = items.minByOrNull { item ->
                            val screenX = cX + item.x * zoomScale
                            val screenY = cY + item.y * zoomScale
                            sqrt((tapOffset.x - screenX).pow(2) + (tapOffset.y - screenY).pow(2))
                        }
                        if (clicked != null) {
                            val screenX = cX + clicked.x * zoomScale
                            val screenY = cY + clicked.y * zoomScale
                            val dist = sqrt((tapOffset.x - screenX).pow(2) + (tapOffset.y - screenY).pow(2))
                            if (dist < 60f) {
                                onItemSelected(clicked)
                            }
                        }
                    }
                }
        ) {
            val centerX = size.width / 2f + panOffset.x
            val centerY = size.height / 2f + panOffset.y
            val maxRadius = min(size.width, size.height) * 0.44f * zoomScale

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
                    color = Color(0xFF00E5FF).copy(alpha = 0.18f),
                    radius = ringRadius,
                    center = Offset(centerX, centerY),
                    style = Stroke(width = 1.2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f)))
                )
            }

            // 3. Crosshairs
            drawLine(
                color = Color(0xFF00E5FF).copy(alpha = 0.22f),
                start = Offset(centerX - maxRadius, centerY),
                end = Offset(centerX + maxRadius, centerY),
                strokeWidth = 1f
            )
            drawLine(
                color = Color(0xFF00E5FF).copy(alpha = 0.22f),
                start = Offset(centerX, centerY - maxRadius),
                end = Offset(centerX, centerY + maxRadius),
                strokeWidth = 1f
            )

            // 4. Expanding Radar Pulses
            drawCircle(
                color = Color(0xFF00E5FF).copy(alpha = ((1.0f - pulseRadius) * 0.35f).coerceIn(0f, 1f)),
                radius = maxRadius * pulseRadius,
                center = Offset(centerX, centerY),
                style = Stroke(width = 2.2f)
            )
            drawCircle(
                color = Color(0xFF00E5FF).copy(alpha = ((1.0f - pulseRadius2) * 0.25f).coerceIn(0f, 1f)),
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
                    colors = listOf(Color(0xFF00E5FF).copy(alpha = 0.9f), Color(0xFF00E5FF).copy(alpha = 0.15f), Color.Transparent),
                    start = Offset(centerX, centerY),
                    end = Offset(sweepEndX, sweepEndY)
                ),
                start = Offset(centerX, centerY),
                end = Offset(sweepEndX, sweepEndY),
                strokeWidth = 3f
            )

            // 6. Heatmap Layer (if enabled)
            if (isHeatmapActive) {
                val weightedPoints = items.map { item ->
                    HeatmapRenderer.WeightedPoint(
                        x = centerX + item.x * zoomScale,
                        y = centerY + item.y * zoomScale,
                        weight = 1.2f
                    )
                }
                HeatmapRenderer.drawHeatmapLayer(
                    drawScope = this,
                    points = weightedPoints,
                    bandwidth = 45f * zoomScale
                )
            }

            // 7. Cluster & Draw Items
            val screenItems = items.map { item ->
                item.copy(
                    x = centerX + item.x * zoomScale,
                    y = centerY + item.y * zoomScale
                )
            }
            val clusters = MarkerRenderer.clusterPoints(screenItems, thresholdPx = 45f * zoomScale)
            for (cluster in clusters) {
                val isSelected = cluster.items.any { it.id == selectedItemId }
                MarkerRenderer.drawCluster(
                    drawScope = this,
                    cluster = cluster,
                    isSelected = isSelected
                )
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
