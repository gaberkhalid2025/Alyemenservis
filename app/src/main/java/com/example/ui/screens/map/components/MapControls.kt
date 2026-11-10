package com.example.ui.screens.map.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.map.MapMode
import com.example.utils.VisualThemePalette

/**
 * 🎮 MapControls (Floating Action Dock - الشريط العائم الرأسي الموحد)
 * Capsule design: 26dp rounded corner, dark navy #0F172A at 94% opacity, elevation 14dp, width 58dp with vertical scroll.
 * Features DockButton components with icons, clear micro-labels, active indicators, and TalkBack support.
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
    onResetCompass: () -> Unit = {},
    isNightMode: Boolean = false,
    onToggleNight: () -> Unit = {},
    isGibsActive: Boolean = false,
    onToggleGibs: () -> Unit = {},
    onGibsLongPress: () -> Unit = {},
    onShowGnss: (() -> Unit)? = null
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
        modifier = modifier.width(58.dp).heightIn(max = 580.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 6.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // 1. 🧭 سهم اتجاه الملاحة
            DockButton(
                icon = Icons.Default.Navigation,
                label = "ملاحة",
                active = isNavigationActive,
                activeColor = Color(0xFF38BDF8),
                onClick = onToggleNavigation,
                testTag = "map_nav_toggle_btn",
                contentDescription = "سهم اتجاه الملاحة"
            )

            HorizontalDivider(modifier = Modifier.width(34.dp), color = Color.White.copy(alpha = 0.08f))

            // 2. 🎯 رادار المسح والاستكشاف الحي
            DockButton(
                icon = Icons.Default.TrackChanges,
                label = "رادار",
                active = isRadarMode,
                activeColor = Color(0xFF10B981),
                onClick = onToggleRadarMode,
                testTag = "map_radar_btn",
                contentDescription = "رادار المسح والاستكشاف الحي"
            )

            HorizontalDivider(modifier = Modifier.width(34.dp), color = Color.White.copy(alpha = 0.08f))

            // 3. 🛰️ التبديل السريع للأقمار الصناعية
            DockButton(
                icon = Icons.Default.Satellite,
                label = "أقمار",
                active = currentMapMode == MapMode.SATELLITE,
                activeColor = Color(0xFF10B981),
                onClick = { onSelectMapMode(MapMode.SATELLITE, 0f) },
                testTag = "map_satellite_btn",
                contentDescription = "الأقمار الصناعية"
            )

            HorizontalDivider(modifier = Modifier.width(34.dp), color = Color.White.copy(alpha = 0.08f))

            // 4. 🗺️ خريطة الشوارع (HOT)
            DockButton(
                icon = Icons.Default.Map,
                label = "شوارع",
                active = currentMapMode == MapMode.HOT,
                activeColor = Color(0xFF10B981),
                onClick = { onSelectMapMode(MapMode.HOT, 0f) },
                testTag = "map_pano_btn",
                contentDescription = "خريطة الشوارع (HOT)"
            )

            HorizontalDivider(modifier = Modifier.width(34.dp), color = Color.White.copy(alpha = 0.08f))

            // 5. ☁️ الخريطة الحرارية
            DockButton(
                icon = if (isHeatmapActive) Icons.Default.CloudDone else Icons.Default.CloudQueue,
                label = "حراري",
                active = isHeatmapActive,
                activeColor = Color(0xFF10B981),
                onClick = onToggleHeatmap,
                testTag = "map_heatmap_sync_btn",
                contentDescription = "الخريطة الحرارية"
            )

            HorizontalDivider(modifier = Modifier.width(34.dp), color = Color.White.copy(alpha = 0.08f))

            // 6. Map Layers (طبقات الخريطة)
            DockButton(
                icon = Icons.Default.Layers,
                label = "طبقات",
                active = false,
                activeColor = Color(0xFFFBBF24),
                inactiveColor = Color(0xFFFBBF24),
                onClick = onOpenLayerDialog,
                testTag = "map_layers_btn",
                contentDescription = "طبقات الخريطة"
            )

            HorizontalDivider(modifier = Modifier.width(34.dp), color = Color.White.copy(alpha = 0.08f))

            // 7. GPS Button (موقعي الحالي)
            Surface(
                onClick = onRecenterLocation,
                shape = RoundedCornerShape(10.dp),
                color = Color.Transparent,
                modifier = Modifier
                    .size(width = 52.dp, height = 48.dp)
                    .testTag("recenter_location_btn")
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 2.dp, vertical = 3.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                        if (isGpsActive) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
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
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "موقعي",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isGpsActive) Color(0xFF10B981) else Color.White.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.width(34.dp), color = Color.White.copy(alpha = 0.08f))

            // 8. ➕ تكبير مباشر
            DockButton(
                icon = Icons.Default.Add,
                label = "تكبير",
                active = false,
                onClick = onZoomIn,
                testTag = "map_zoom_in_btn",
                contentDescription = "تكبير الخريطة"
            )

            // 9. ➖ تصغير مباشر
            DockButton(
                icon = Icons.Default.Remove,
                label = "تصغير",
                active = false,
                onClick = onZoomOut,
                testTag = "map_zoom_out_btn",
                contentDescription = "تصغير الخريطة"
            )

            HorizontalDivider(modifier = Modifier.width(34.dp), color = Color.White.copy(alpha = 0.08f))

            // 10. 🌙 الوضع الليلي للخريطة
            DockButton(
                icon = if (isNightMode) Icons.Default.Bedtime else Icons.Default.WbSunny,
                label = if (isNightMode) "ليلي" else "نهاري",
                active = isNightMode,
                activeColor = Color(0xFFA78BFA),
                onClick = onToggleNight,
                testTag = "map_night_mode_btn",
                contentDescription = "تبديل الوضع الليلي للخريطة"
            )

            HorizontalDivider(modifier = Modifier.width(34.dp), color = Color.White.copy(alpha = 0.08f))

            // 11. 🛰️ طبقة NASA GIBS اليومية المباشرة
            DockButton(
                icon = Icons.Default.Explore,
                label = "ناسا",
                active = isGibsActive,
                activeColor = Color(0xFF38BDF8),
                onClick = onToggleGibs,
                onLongClick = onGibsLongPress,
                testTag = "map_gibs_btn",
                contentDescription = "طبقة ناسا اليومية (اضغط مطولاً لتبديل النوع)"
            )

            // 12. 📡 لوحة تشخيص إشارة GNSS الحية
            onShowGnss?.let { showGnssAction ->
                HorizontalDivider(modifier = Modifier.width(34.dp), color = Color.White.copy(alpha = 0.08f))
                DockButton(
                    icon = Icons.Default.SatelliteAlt,
                    label = "إشارة",
                    active = false,
                    activeColor = Color(0xFF00E5FF),
                    onClick = showGnssAction,
                    testTag = "map_gnss_btn",
                    contentDescription = "حالة إشارة GPS والأقمار الصناعية"
                )
            }
        }
    }
}

/**
 * 🔘 DockButton: زر مخصص لشريط الخريطة بمقاس مريح (52×48dp) وأيقونة ونص صغير وسمات إمكانية الوصول
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DockButton(
    icon: ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    activeColor: Color = Color(0xFF10B981),
    inactiveColor: Color = Color.White,
    contentDescription: String = label,
    testTag: String = ""
) {
    val animatedBg by animateColorAsState(
        targetValue = if (active) activeColor.copy(alpha = 0.18f) else Color.Transparent,
        label = "dock_btn_bg"
    )
    val animatedTint by animateColorAsState(
        targetValue = if (active) activeColor else inactiveColor,
        label = "dock_btn_tint"
    )

    Box(
        modifier = modifier
            .size(width = 52.dp, height = 48.dp)
            .testTag(testTag)
            .semantics(mergeDescendants = true) {
                this.contentDescription = contentDescription
                this.role = Role.Button
                this.stateDescription = if (active) "مفعّل" else "متوقف"
            }
            .clip(RoundedCornerShape(10.dp))
            .background(animatedBg)
            .then(
                if (active) Modifier.border(1.dp, activeColor.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                else Modifier
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 2.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = animatedTint,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 9.sp,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                color = if (active) activeColor else Color.White.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}
