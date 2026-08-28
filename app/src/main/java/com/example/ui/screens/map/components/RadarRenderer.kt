package com.example.ui.screens.map.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep_angle"
    )

    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_radius"
    )

    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    zoomScale = (zoomScale * zoom).coerceIn(0.5f, 4.0f)
                    panOffset += pan
                }
            }
            .pointerInput(items) {
                detectTapGestures { tapOffset ->
                    // Find closest item within 40px
                    val clicked = items.minByOrNull { item ->
                        val screenX = item.x * zoomScale + panOffset.x
                        val screenY = item.y * zoomScale + panOffset.y
                        sqrt((tapOffset.x - screenX).pow(2) + (tapOffset.y - screenY).pow(2))
                    }
                    if (clicked != null) {
                        val screenX = clicked.x * zoomScale + panOffset.x
                        val screenY = clicked.y * zoomScale + panOffset.y
                        val dist = sqrt((tapOffset.x - screenX).pow(2) + (tapOffset.y - screenY).pow(2))
                        if (dist < 50f) {
                            onItemSelected(clicked)
                        }
                    }
                }
            }
    ) {
        val centerX = size.width / 2f + panOffset.x
        val centerY = size.height / 2f + panOffset.y
        val maxRadius = min(size.width, size.height) * 0.44f * zoomScale

        // 1. Dark Background Circles
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF0F172A), Color(0xFF020617)),
                center = Offset(centerX, centerY),
                radius = maxRadius * 1.2f
            ),
            center = Offset(centerX, centerY),
            radius = maxRadius * 1.15f
        )

        // 2. Concentric Radar Rings
        val rings = 4
        for (i in 1..rings) {
            val ringRadius = maxRadius * (i.toFloat() / rings)
            drawCircle(
                color = Color(0xFF00E5FF).copy(alpha = 0.18f),
                radius = ringRadius,
                center = Offset(centerX, centerY),
                style = Stroke(width = 1.2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)))
            )
        }

        // 3. Crosshairs
        drawLine(
            color = Color(0xFF00E5FF).copy(alpha = 0.25f),
            start = Offset(centerX - maxRadius, centerY),
            end = Offset(centerX + maxRadius, centerY),
            strokeWidth = 1f
        )
        drawLine(
            color = Color(0xFF00E5FF).copy(alpha = 0.25f),
            start = Offset(centerX, centerY - maxRadius),
            end = Offset(centerX, centerY + maxRadius),
            strokeWidth = 1f
        )

        // 4. Expanding Radar Pulse
        drawCircle(
            color = Color(0xFF00E5FF).copy(alpha = (1.0f - pulseRadius) * 0.35f),
            radius = maxRadius * pulseRadius,
            center = Offset(centerX, centerY),
            style = Stroke(width = 2f)
        )

        // 5. Rotating Radar Sweep Line
        val rad = Math.toRadians(sweepAngle.toDouble())
        val sweepEndX = (centerX + maxRadius * cos(rad)).toFloat()
        val sweepEndY = (centerY + maxRadius * sin(rad)).toFloat()
        drawLine(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF00E5FF).copy(alpha = 0.8f), Color.Transparent),
                start = Offset(centerX, centerY),
                end = Offset(sweepEndX, sweepEndY)
            ),
            start = Offset(centerX, centerY),
            end = Offset(sweepEndX, sweepEndY),
            strokeWidth = 2.5f
        )

        // 6. Heatmap Layer (if enabled)
        if (isHeatmapActive) {
            val weightedPoints = items.map { item ->
                HeatmapRenderer.WeightedPoint(
                    x = item.x * zoomScale + panOffset.x,
                    y = item.y * zoomScale + panOffset.y,
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
                x = item.x * zoomScale + panOffset.x,
                y = item.y * zoomScale + panOffset.y
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

        // 8. User Center Point
        drawCircle(
            color = Color(0xFF10B981).copy(alpha = 0.3f),
            radius = 16f,
            center = Offset(centerX, centerY)
        )
        drawCircle(
            color = Color(0xFF10B981),
            radius = 7f,
            center = Offset(centerX, centerY)
        )
        drawCircle(
            color = Color.White,
            radius = 3f,
            center = Offset(centerX, centerY)
        )
    }
}
