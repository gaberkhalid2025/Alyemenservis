package com.example.ui.screens.map.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.utils.VisualThemePalette

/**
 * 🎮 MapControls (مطابقة للشاشة والضوابط العائمة للخريطة)
 * أزرار تحكم عائمة بتصميم دائري داكن ومريح مطابق للشاشة المرفقة:
 * 1. زر تحديد وتوسيط موقع المستخدم (GPS Target)
 * 2. أزرار التكبير (+) والتصغير (-) التدريجي
 * 3. زر تخزين وتأكيد الأوفلاين (Cloud Offline Cache)
 * 4. زر التبديل بين الرادار والخريطة
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
    isGpsActive: Boolean,
    themeColors: VisualThemePalette,
    modifier: Modifier = Modifier,
    currentMapMode: com.example.ui.screens.map.MapMode = com.example.ui.screens.map.MapMode.SATELLITE,
    isOfflineCached: Boolean = true,
    onToggleOfflineCache: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Recenter Location Button (GPS Target - أعلى المجموعة كالصورة)
        Surface(
            shape = CircleShape,
            color = Color(0xFF0F242A),
            border = BorderStroke(1.5.dp, Color(0xFF10B981)),
            shadowElevation = 6.dp,
            modifier = Modifier.size(46.dp)
        ) {
            IconButton(
                onClick = onRecenterLocation,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("recenter_location_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "موقعي الحالي",
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // 2. Zoom In Button (+)
        Surface(
            shape = CircleShape,
            color = Color(0xFF1E293B),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            shadowElevation = 6.dp,
            modifier = Modifier.size(46.dp)
        ) {
            IconButton(
                onClick = onZoomIn,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("map_zoom_in_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "تكبير",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // 3. Zoom Out Button (-)
        Surface(
            shape = CircleShape,
            color = Color(0xFF1E293B),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            shadowElevation = 6.dp,
            modifier = Modifier.size(46.dp)
        ) {
            IconButton(
                onClick = onZoomOut,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("map_zoom_out_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "تصغير",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // 4. Layer Selection Button (اختيار طبقات الخريطة الأربع: OSM, HOT, Topo, Satellite)
        val layerBadge = when (currentMapMode) {
            com.example.ui.screens.map.MapMode.OSM -> "🌐"
            com.example.ui.screens.map.MapMode.HOT -> "🗺️"
            com.example.ui.screens.map.MapMode.TOPO -> "⛰️"
            com.example.ui.screens.map.MapMode.SATELLITE -> "🛰️"
            else -> "🛰️"
        }
        Surface(
            shape = CircleShape,
            color = Color(0xFF1E293B),
            border = BorderStroke(1.5.dp, Color(0xFF00E5FF)),
            shadowElevation = 6.dp,
            modifier = Modifier.size(46.dp)
        ) {
            IconButton(
                onClick = onOpenLayerDialog,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("map_layers_btn")
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = "اختر طبقة الخريطة",
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = layerBadge,
                        fontSize = 11.sp,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 2.dp, bottom = 2.dp)
                    )
                }
            }
        }

        // 5. Cloud Offline Cache Indicator Button (حفظ الأوفلاين الفعلي)
        Surface(
            shape = CircleShape,
            color = Color(0xFF1E293B),
            border = BorderStroke(1.dp, if (isOfflineCached) Color(0xFF00E5FF) else Color(0xFF334155)),
            shadowElevation = 6.dp,
            modifier = Modifier.size(46.dp)
        ) {
            IconButton(
                onClick = onToggleOfflineCache,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("map_offline_cache_btn")
            ) {
                Icon(
                    imageVector = if (isOfflineCached) Icons.Default.CloudDone else Icons.Default.CloudDownload,
                    contentDescription = "التخزين المحلي للأوفلاين",
                    tint = if (isOfflineCached) Color(0xFF00E5FF) else Color(0xFF94A3B8),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // 6. Heatmap Density Indicator Button (الخريطة الحرارية للكثافة)
        Surface(
            shape = CircleShape,
            color = Color(0xFF1E293B),
            border = BorderStroke(1.dp, if (isHeatmapActive) Color(0xFFF97316) else Color(0xFF334155)),
            shadowElevation = 6.dp,
            modifier = Modifier.size(46.dp)
        ) {
            IconButton(
                onClick = onToggleHeatmap,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("map_heatmap_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Whatshot,
                    contentDescription = "الخريطة الحرارية",
                    tint = if (isHeatmapActive) Color(0xFFF97316) else Color(0xFF94A3B8),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
