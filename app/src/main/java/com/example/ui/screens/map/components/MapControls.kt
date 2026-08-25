package com.example.ui.screens.map.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.utils.VisualThemePalette

/**
 * 🎮 MapControls
 * Floating interactive control panel for:
 * - Radar Mode / Leaflet Mode Toggle
 * - Heatmap Density Mode Toggle
 * - Zoom In / Zoom Out
 * - My Location Recenter
 */
@Composable
fun MapControls(
    isRadarMode: Boolean,
    onToggleRadarMode: () -> Unit,
    isHeatmapActive: Boolean,
    onToggleHeatmap: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onRecenterLocation: () -> Unit,
    isGpsActive: Boolean,
    themeColors: VisualThemePalette,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Toggle Mode Button (Radar vs OSM)
        FloatingActionButton(
            onClick = onToggleRadarMode,
            containerColor = if (isRadarMode) Color(0xFF00E5FF) else Color(0xFF1E293B),
            contentColor = if (isRadarMode) Color(0xFF0F172A) else Color(0xFF00E5FF),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .size(48.dp)
                .testTag("toggle_radar_btn")
        ) {
            Icon(
                imageVector = if (isRadarMode) Icons.Default.Place else Icons.Default.Refresh,
                contentDescription = if (isRadarMode) "خريطة OSM" else "رادار"
            )
        }

        // Toggle Heatmap Button
        FloatingActionButton(
            onClick = onToggleHeatmap,
            containerColor = if (isHeatmapActive) Color(0xFFFF5252) else Color(0xFF1E293B),
            contentColor = if (isHeatmapActive) Color.White else Color(0xFFFF5252),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .size(48.dp)
                .testTag("toggle_heatmap_btn")
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "خريطة حرارية"
            )
        }

        // Zoom In & Out Container
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF1E293B),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
            modifier = Modifier.shadow(4.dp, RoundedCornerShape(14.dp))
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = onZoomIn,
                    modifier = Modifier
                        .size(42.dp)
                        .testTag("map_zoom_in_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "تكبير", tint = Color.White)
                }
                HorizontalDivider(modifier = Modifier.width(32.dp), color = Color(0xFF334155))
                IconButton(
                    onClick = onZoomOut,
                    modifier = Modifier
                        .size(42.dp)
                        .testTag("map_zoom_out_btn")
                ) {
                    Icon(Icons.Default.Close, contentDescription = "تصغير", tint = Color.White)
                }
            }
        }

        // Recenter Location Button
        FloatingActionButton(
            onClick = onRecenterLocation,
            containerColor = if (isGpsActive) Color(0xFF10B981) else Color(0xFF1E293B),
            contentColor = if (isGpsActive) Color.White else Color(0xFF10B981),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .size(48.dp)
                .testTag("recenter_location_btn")
        ) {
            Icon(
                imageVector = Icons.Default.Place,
                contentDescription = "موقعي الحالي"
            )
        }
    }
}
