package com.example.ui.screens.map.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.screens.map.MapMode
import com.example.utils.VisualThemePalette

/**
 * 🎮 MapControls (Floating Action Dock - الشريط العائم الرأسي الموحد)
 * Capsule design: 26dp rounded corner, dark navy #0F172A at 94% opacity, elevation 14dp, width 52dp with vertical scroll.
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
    onOpenLayerDialog: () -> Unit,
    isNavigationActive: Boolean,
    onToggleNavigation: () -> Unit,
    isGpsActive: Boolean,
    themeColors: VisualThemePalette,
    modifier: Modifier = Modifier,
    currentMapMode: MapMode = MapMode.SATELLITE,
    onSelectMapMode: (MapMode, Float) -> Unit = { _, _ -> },
    isOfflineCached: Boolean = true,
    onToggleOfflineCache: () -> Unit = {},
    isDrivingMode: Boolean = true,
    onToggleDrivingMode: () -> Unit = {},
    compassBearing: Float = 0f,
    onResetCompass: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "gps_pulse")
    val gpsPulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gps_pulse_alpha"
    )

    Surface(
        shape = RoundedCornerShape(26.dp),
        color = Color(0xFF0F172A).copy(alpha = 0.94f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
        shadowElevation = 14.dp,
        modifier = modifier.width(52.dp).heightIn(max = 560.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 6.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 1. 🧭 سهم اتجاه الملاحة
            IconButton(
                onClick = onToggleNavigation,
                modifier = Modifier.size(40.dp).testTag("map_nav_toggle_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Navigation,
                    contentDescription = "سهم اتجاه الملاحة",
                    tint = if (isNavigationActive) Color(0xFF38BDF8) else Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            HorizontalDivider(modifier = Modifier.width(30.dp), color = Color.White.copy(alpha = 0.08f))

            // 2. 🎯 رادار المسح والاستكشاف الحي
            IconButton(
                onClick = onToggleRadarMode,
                modifier = Modifier.size(40.dp).testTag("map_radar_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.TrackChanges,
                    contentDescription = "رادار المسح والاستكشاف الحي",
                    tint = if (isRadarMode) Color(0xFF10B981) else Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            HorizontalDivider(modifier = Modifier.width(30.dp), color = Color.White.copy(alpha = 0.08f))

            // 3. 🛰️ التبديل السريع للأقمار الصناعية
            IconButton(
                onClick = { onSelectMapMode(MapMode.SATELLITE, 0f) },
                modifier = Modifier.size(40.dp).testTag("map_satellite_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Satellite,
                    contentDescription = "الأقمار الصناعية",
                    tint = if (currentMapMode == MapMode.SATELLITE) Color(0xFF10B981) else Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            HorizontalDivider(modifier = Modifier.width(30.dp), color = Color.White.copy(alpha = 0.08f))

            // 4. 🗺️ خريطة الشوارع (HOT)
            IconButton(
                onClick = { onSelectMapMode(MapMode.HOT, 0f) },
                modifier = Modifier.size(40.dp).testTag("map_pano_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Map,
                    contentDescription = "خريطة الشوارع (HOT)",
                    tint = if (currentMapMode == MapMode.HOT) Color(0xFF10B981) else Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            HorizontalDivider(modifier = Modifier.width(30.dp), color = Color.White.copy(alpha = 0.08f))

            // 5. ☁️ الخريطة الحرارية
            IconButton(
                onClick = onToggleHeatmap,
                modifier = Modifier.size(40.dp).testTag("map_heatmap_sync_btn")
            ) {
                Icon(
                    imageVector = if (isHeatmapActive) Icons.Default.CloudDone else Icons.Default.CloudQueue,
                    contentDescription = "الخريطة الحرارية",
                    tint = if (isHeatmapActive) Color(0xFF10B981) else Color(0xFF38BDF8),
                    modifier = Modifier.size(22.dp)
                )
            }

            HorizontalDivider(modifier = Modifier.width(30.dp), color = Color.White.copy(alpha = 0.08f))

            // 6. Map Layers (🗺️ بلون ذهبي #FBBF24)
            IconButton(
                onClick = onOpenLayerDialog,
                modifier = Modifier.size(40.dp).testTag("map_layers_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Layers,
                    contentDescription = "طبقات الخريطة",
                    tint = Color(0xFFFBBF24), // Golden #FBBF24
                    modifier = Modifier.size(22.dp)
                )
            }

            HorizontalDivider(modifier = Modifier.width(30.dp), color = Color.White.copy(alpha = 0.08f))

            // 7. GPS Button
            IconButton(
                onClick = onRecenterLocation,
                modifier = Modifier.size(40.dp).testTag("recenter_location_btn")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isGpsActive) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981).copy(alpha = gpsPulseAlpha))
                        )
                    }
                    Icon(
                        imageVector = if (isGpsActive) Icons.Default.GpsFixed else Icons.Default.MyLocation,
                        contentDescription = "موقعي الحالي (GPS)",
                        tint = if (isGpsActive) Color(0xFF10B981) else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
