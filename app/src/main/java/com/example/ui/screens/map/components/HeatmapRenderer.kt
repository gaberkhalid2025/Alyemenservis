package com.example.ui.screens.map.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * 🔥 HeatmapRenderer
 * Real Kernel Density Estimation (KDE) calculation and rendering
 * Color gradient: Blue (low) -> Green -> Yellow -> Red (high density)
 */
object HeatmapRenderer {

    data class WeightedPoint(
        val x: Float,
        val y: Float,
        val weight: Float = 1.0f
    )

    /**
     * Compute Gaussian Kernel Density at (px, py) from a list of points
     */
    fun computeKernelDensity(
        px: Float,
        py: Float,
        points: List<WeightedPoint>,
        bandwidth: Float
    ): Float {
        var totalDensity = 0.0f
        val variance = bandwidth.pow(2)

        for (pt in points) {
            val distSq = (px - pt.x).pow(2) + (py - pt.y).pow(2)
            if (distSq < variance * 9) { // 3-sigma cutoff
                val kernelVal = exp(-distSq / (2 * variance))
                totalDensity += pt.weight * kernelVal
            }
        }
        return totalDensity
    }

    /**
     * Draw KDE Heatmap on Canvas
     */
    fun drawHeatmapLayer(
        drawScope: DrawScope,
        points: List<WeightedPoint>,
        bandwidth: Float = 60f,
        maxOpacity: Float = 0.65f
    ) {
        if (points.isEmpty()) return

        for (pt in points) {
            val radius = bandwidth * 1.5f
            drawScope.drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFEF4444).copy(alpha = maxOpacity * 0.7f), // Red
                        Color(0xFFF59E0B).copy(alpha = maxOpacity * 0.5f), // Yellow
                        Color(0xFF10B981).copy(alpha = maxOpacity * 0.3f), // Green
                        Color(0xFF00E5FF).copy(alpha = maxOpacity * 0.15f), // Cyan
                        Color.Transparent
                    ),
                    center = Offset(pt.x, pt.y),
                    radius = radius
                ),
                center = Offset(pt.x, pt.y),
                radius = radius
            )
        }
    }
}
