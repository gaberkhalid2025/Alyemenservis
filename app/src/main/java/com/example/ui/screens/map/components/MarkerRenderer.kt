package com.example.ui.screens.map.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.*

/**
 * 📍 MarkerRenderer
 * Custom Canvas & Radar marker rendering with 4-level clustering:
 * - Level 1: < 5 items (Small Cyan/Green)
 * - Level 2: 5 - 20 items (Medium Orange)
 * - Level 3: 20 - 50 items (Large Pink)
 * - Level 4: > 50 items (Extra Large Red/Purple)
 */
object MarkerRenderer {

    data class MapItemPoint(
        val id: String,
        val title: String,
        val type: String, // "PROVIDER", "STORE", "RESTAURANT", "MEDICAL", "PROPERTY", "JOB"
        val x: Float,
        val y: Float,
        val rating: Double = 5.0,
        val isAvailable: Boolean = true,
        val originalItem: Any
    )

    data class ClusterGroup(
        val centerX: Float,
        val centerY: Float,
        val items: List<MapItemPoint>
    )

    fun getColorForType(type: String): Color {
        return when (type.uppercase()) {
            "PROVIDER", "TECHNICIAN" -> Color(0xFF00E5FF)
            "STORE" -> Color(0xFF10B981)
            "RESTAURANT" -> Color(0xFFF59E0B)
            "MEDICAL" -> Color(0xFFEC4899)
            "PROPERTY", "REAL_ESTATE" -> Color(0xFF8B5CF6)
            "JOB", "JOBS" -> Color(0xFF6366F1)
            else -> Color(0xFF00E5FF)
        }
    }

    fun getEmojiForType(type: String): String {
        return when (type.uppercase()) {
            "PROVIDER", "TECHNICIAN" -> "👷"
            "STORE" -> "🏪"
            "RESTAURANT" -> "🍔"
            "MEDICAL" -> "🏥"
            "PROPERTY", "REAL_ESTATE" -> "🏠"
            "JOB", "JOBS" -> "💼"
            else -> "📍"
        }
    }

    /**
     * Compute clusters based on screen distance threshold (e.g. 40 pixels)
     */
    fun clusterPoints(points: List<MapItemPoint>, thresholdPx: Float = 50f): List<ClusterGroup> {
        val clusters = mutableListOf<ClusterGroup>()
        val visited = BooleanArray(points.size)

        for (i in points.indices) {
            if (visited[i]) continue
            visited[i] = true

            val group = mutableListOf(points[i])
            var sumX = points[i].x
            var sumY = points[i].y

            for (j in (i + 1) until points.size) {
                if (visited[j]) continue
                val dist = sqrt((points[i].x - points[j].x).pow(2) + (points[i].y - points[j].y).pow(2))
                if (dist <= thresholdPx) {
                    visited[j] = true
                    group.add(points[j])
                    sumX += points[j].x
                    sumY += points[j].y
                }
            }

            clusters.add(
                ClusterGroup(
                    centerX = sumX / group.size,
                    centerY = sumY / group.size,
                    items = group
                )
            )
        }
        return clusters
    }

    /**
     * Draw individual item marker or cluster badge
     */
    fun drawCluster(
        drawScope: DrawScope,
        cluster: ClusterGroup,
        isSelected: Boolean = false
    ) {
        val count = cluster.items.size
        val center = Offset(cluster.centerX, cluster.centerY)

        if (count == 1) {
            val item = cluster.items.first()
            val color = getColorForType(item.type)

            // Outer glow if selected
            if (isSelected) {
                drawScope.drawCircle(
                    color = color.copy(alpha = 0.35f),
                    radius = 24f,
                    center = center
                )
            }

            // Outer ring
            drawScope.drawCircle(
                color = if (item.isAvailable) color else Color(0xFF64748B),
                radius = 14f,
                center = center
            )
            // Center core
            drawScope.drawCircle(
                color = Color(0xFF0F172A),
                radius = 10f,
                center = center
            )
            drawScope.drawCircle(
                color = color,
                radius = 6f,
                center = center
            )
        } else {
            // Cluster level 1-4
            val (clusterColor, radius) = when {
                count < 5 -> Color(0xFF00E5FF) to 18f
                count in 5..20 -> Color(0xFFF59E0B) to 22f
                count in 21..50 -> Color(0xFFEC4899) to 26f
                else -> Color(0xFFEF4444) to 30f
            }

            // Ripple ring
            drawScope.drawCircle(
                color = clusterColor.copy(alpha = 0.25f),
                radius = radius + 8f,
                center = center
            )
            // Background circle
            drawScope.drawCircle(
                color = clusterColor,
                radius = radius,
                center = center
            )
            // Border
            drawScope.drawCircle(
                color = Color.White,
                radius = radius,
                center = center,
                style = Stroke(width = 2f)
            )
        }
    }
}
