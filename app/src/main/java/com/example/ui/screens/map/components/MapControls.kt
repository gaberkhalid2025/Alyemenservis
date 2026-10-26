package com.example.ui.screens.map.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.utils.VisualThemePalette

/**
 * 🎮 MapControls (Unified Control Dock - قاعدة التحكم الموحدة)
 * Single continuous vertical Glassmorphism capsule on the screen's side:
 * Ordered strictly top-to-bottom:
 * 1. GPS (Glows vibrant emerald green #10B981 with breathing pulse when active)
 * 2. Zoom In (+)
 * 3. Zoom Out (-)
 * 4. Map Layers
 * 5. Compass (North orientation / bearing)
 * 6. Context Mode (Driving 🚘 vs Walking 🚶)
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
    currentMapMode: com.example.ui.screens.map.MapMode = com.example.ui.screens.map.MapMode.SATELLITE,
    isOfflineCached: Boolean = true,
    onToggleOfflineCache: () -> Unit = {},
    isDrivingMode: Boolean = true,
    onToggleDrivingMode: () -> Unit = {},
    compassBearing: Float = 0f,
    onResetCompass: () -> Unit = {}
) {
    // Breathing pulse animation for active GPS glow
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

    // Unified Continuous Vertical Capsule Dock
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color(0xFF0B0F19).copy(alpha = 0.85f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
        shadowElevation = 10.dp,
        modifier = modifier.width(48.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // 0. Navigation Toggle (Top-most)
            IconButton(
                onClick = onToggleNavigation,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("map_nav_toggle_btn")
            ) {
                Icon(
                    imageVector = if (isNavigationActive) Icons.Default.Navigation else Icons.Default.Map,
                    contentDescription = "بدء الملاحة",
                    tint = if (isNavigationActive) Color(0xFF38BDF8) else Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            HorizontalDivider(
                modifier = Modifier.width(26.dp),
                color = Color.White.copy(alpha = 0.08f)
            )

            // 1. GPS Button (Glows green #10B981 when active)
            IconButton(
                onClick = onRecenterLocation,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("recenter_location_btn")
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

            HorizontalDivider(
                modifier = Modifier.width(26.dp),
                color = Color.White.copy(alpha = 0.08f)
            )

            // 2. Zoom In (+)
            IconButton(
                onClick = onZoomIn,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("map_zoom_in_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "تكبير الخريطة",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            HorizontalDivider(
                modifier = Modifier.width(26.dp),
                color = Color.White.copy(alpha = 0.08f)
            )

            // 3. Zoom Out (-)
            IconButton(
                onClick = onZoomOut,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("map_zoom_out_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "تصغير الخريطة",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            HorizontalDivider(
                modifier = Modifier.width(26.dp),
                color = Color.White.copy(alpha = 0.08f)
            )

            // 4. Map Layers
            val isLayerNonDefault = currentMapMode != com.example.ui.screens.map.MapMode.SATELLITE
            IconButton(
                onClick = onOpenLayerDialog,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("map_layers_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Layers,
                    contentDescription = "طبقات الخريطة",
                    tint = if (isLayerNonDefault) Color(0xFF10B981) else Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            HorizontalDivider(
                modifier = Modifier.width(26.dp),
                color = Color.White.copy(alpha = 0.08f)
            )

            // 5. Compass (North orientation)
            IconButton(
                onClick = onResetCompass,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("map_compass_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Explore,
                    contentDescription = "البوصلة وتوجيه الشمال",
                    tint = Color(0xFF10B981),
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(compassBearing)
                )
            }

            HorizontalDivider(
                modifier = Modifier.width(26.dp),
                color = Color.White.copy(alpha = 0.08f)
            )

            // 6. Context Mode (Driving 🚘 vs Walking 🚶)
            IconButton(
                onClick = onToggleDrivingMode,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("map_context_mode_btn")
            ) {
                Icon(
                    imageVector = if (isDrivingMode) Icons.Default.DirectionsCar else Icons.Default.DirectionsWalk,
                    contentDescription = if (isDrivingMode) "وضع القيادة" else "وضع المشي",
                    tint = if (isDrivingMode) Color(0xFF38BDF8) else Color(0xFFF59E0B),
                    modifier = Modifier.size(19.dp)
                )
            }
        }
    }
}
