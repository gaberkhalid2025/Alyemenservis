package com.example.ui.screens.map.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.map.MapMode

/**
 * 🗺️ MapLayersDialog (نظام طبقات الخرائط المجاني الشامل — بدون أي مفاتيح API)
 */
@Composable
fun MapLayersDialog(
    currentMapMode: MapMode,
    onSelectLayer: (MapMode, Float) -> Unit,
    is3DAvailable: Boolean = true,
    is3DActive: Boolean = false,
    onOpen3D: () -> Unit = {},
    gibsEnabled: Boolean = false,
    gibsProduct: String = "truecolor",
    onToggleGibs: (Boolean) -> Unit = {},
    onSelectGibsProduct: (String) -> Unit = {},
    eoxEnabled: Boolean = false,
    onToggleEox: (Boolean) -> Unit = {},
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        titleContentColor = Color.White,
        textContentColor = Color.White,
        shape = RoundedCornerShape(26.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Layers,
                    contentDescription = null,
                    tint = Color(0xFFFBBF24)
                )
                Column {
                    Text("🗺️ نظام طبقات الخرائط", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "جميع الخرائط والأقمار مجانية 100% بدون مفاتيح API",
                        fontSize = 10.5.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Text(
                    text = "خرائط الأساس والخرائط المجسمة",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF38BDF8)
                )

                // ✨ خرائط VELA المجانية
                BaseLayerCard(
                    title = "✨ خرائط VELA المجانية",
                    desc = "تصميم عصري نظيف وسريع للشوارع والمعالم والحدود (Carto Voyager / OSM — مجانية بلا قيود)",
                    badge = "جديد • مجاني",
                    badgeColor = Color(0xFF00E5FF),
                    isSelected = currentMapMode == MapMode.VELA && !is3DActive,
                    onClick = { onSelectLayer(MapMode.VELA, 0f) },
                    testTag = "layer_dialog_vela_btn"
                )

                // 🧊 خرائط 3D التفاعلية
                BaseLayerCard(
                    title = "🧊 خرائط 3D التفاعلية",
                    desc = if (is3DAvailable) {
                        "عرض مجسم ثلاثي الأبعاد للمباني والتضاريس والطرق (OpenFreeMap + MapLibre GL مجاناً)"
                    } else {
                        "يتطلب دعم WebGL2 على الجهاز لعرض المباني ثلاثية الأبعاد"
                    },
                    badge = "3D مباشر",
                    badgeColor = Color(0xFFA78BFA),
                    isSelected = is3DActive,
                    enabled = is3DAvailable,
                    onClick = onOpen3D,
                    testTag = "layer_dialog_3d_btn"
                )

                val baseLayers = listOf(
                    LayerInfo(MapMode.OSM, "🌐 خريطة OpenStreetMap القياسية", "خريطة OSM القياسية بجميع الأسماء والطرق التفصيلية", 0f),
                    LayerInfo(MapMode.HOT, "🗺️ شوارع HOT", "خريطة شوارع مخصصة للأعمال الميدانية بأسماء المعالم من OSM", 0f),
                    LayerInfo(MapMode.TOPO, "⛰️ تضاريس OpenTopoMap", "خريطة جغرافية بتدرجات التضاريس وظلال الجبال وخطوط الكنتور", 0f)
                )

                baseLayers.forEach { layer ->
                    BaseLayerCard(
                        title = layer.title,
                        desc = layer.desc,
                        isSelected = currentMapMode == layer.mode && !is3DActive,
                        onClick = { onSelectLayer(layer.mode, layer.tilt) },
                        testTag = "layer_dialog_${layer.mode.name.lowercase()}_btn"
                    )
                }

                HorizontalDivider(
                    color = Color.White.copy(alpha = 0.1f),
                    modifier = Modifier.padding(vertical = 2.dp)
                )

                Text(
                    text = "🛰️ الأقمار الصناعية الحية والمباشرة (مجانية)",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF10B981)
                )

                // 🛰️ أقمار صناعية Esri القياسية
                BaseLayerCard(
                    title = "🛰️ أقمار صناعية (Esri القياسية)",
                    desc = "صور جوية فضائية عالية الدقة من Esri World Imagery (مجانية بدون مفتاح API)",
                    isSelected = (currentMapMode == MapMode.SATELLITE || currentMapMode == MapMode.VELA_3D) && !is3DActive,
                    onClick = { onSelectLayer(MapMode.SATELLITE, 0f) },
                    testTag = "layer_dialog_satellite_btn"
                )

                // 💎 أقمار صناعية عالية الوضوح (Esri Clarity)
                BaseLayerCard(
                    title = "💎 أقمار صناعية فائقة الوضوح (Clarity)",
                    desc = "صور فضائية أكثر حدة ووضوحاً للمدن والطرق الجبلية من أرشيف Esri Clarity المجاني",
                    badge = "عالية الدقة",
                    badgeColor = Color(0xFF10B981),
                    isSelected = currentMapMode == MapMode.SATELLITE_CLARITY && !is3DActive,
                    onClick = { onSelectLayer(MapMode.SATELLITE_CLARITY, 0f) },
                    testTag = "layer_dialog_clarity_btn"
                )

                // 🌍 صور EOX Sentinel-2 (منقولة من الطبقات الإضافية)
                OverlayLayerSwitchCard(
                    icon = Icons.Default.Public,
                    title = "🌍 صور EOX Sentinel-2 (2021 بدون سحب)",
                    subtitle = "خريطة أقمار أوروبية صافية خالية تماماً من السحب (CC BY 4.0 — مجانية)",
                    checked = eoxEnabled,
                    onCheckedChange = onToggleEox,
                    accentColor = Color(0xFF38BDF8),
                    testTag = "layer_dialog_eox_switch"
                )

                // 📡 صور ناسا الفضائية الحية والمباشرة (منقولة من الإعدادات والطبقات الإضافية)
                OverlayLayerSwitchCard(
                    icon = Icons.Default.SatelliteAlt,
                    title = "📡 بث ناسا اليومي المباشر (NASA GIBS)",
                    subtitle = "صور أقمار صناعية حية ومباشرة يومياً من NASA EOSDIS للسحب والطقس والأرض مجاناً",
                    checked = gibsEnabled,
                    onCheckedChange = onToggleGibs,
                    accentColor = Color(0xFF00E5FF),
                    testTag = "layer_dialog_gibs_switch"
                ) {
                    if (gibsEnabled) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                "truecolor" to "VIIRS مباشر",
                                "modis" to "MODIS ألوان",
                                "landtemp" to "حرارة السطح"
                            ).forEach { (key, label) ->
                                val selected = gibsProduct == key
                                FilterChip(
                                    selected = selected,
                                    onClick = { onSelectGibsProduct(key) },
                                    label = {
                                        Text(
                                            text = label,
                                            fontSize = 10.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = Color(0xFF0F172A),
                                        labelColor = Color(0xFFCBD5E1),
                                        selectedContainerColor = Color(0xFF00E5FF).copy(alpha = 0.2f),
                                        selectedLabelColor = Color(0xFF00E5FF)
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.heightIn(min = 48.dp).testTag("layer_dialog_close_btn")
            ) {
                Text("إغلاق", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun BaseLayerCard(
    title: String,
    desc: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    badge: String? = null,
    badgeColor: Color = Color(0xFF10B981),
    enabled: Boolean = true
) {
    Surface(
        onClick = { if (enabled) onClick() },
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFF1E293B),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) Color(0xFF10B981) else Color(0xFF334155)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .testTag(testTag)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    color = if (!enabled) Color(0xFF64748B) else if (isSelected) Color(0xFF10B981) else Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (badge != null) {
                    Surface(
                        color = badgeColor.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(0.8.dp, badgeColor.copy(alpha = 0.45f))
                    ) {
                        Text(
                            text = badge,
                            color = badgeColor,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = desc,
                color = if (enabled) Color(0xFF94A3B8) else Color(0xFF64748B),
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun OverlayLayerSwitchCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    accentColor: Color,
    testTag: String,
    extraContent: @Composable ColumnScope.() -> Unit = {}
) {
    Surface(
        onClick = { onCheckedChange(!checked) },
        shape = RoundedCornerShape(14.dp),
        color = if (checked) accentColor.copy(alpha = 0.14f) else Color(0xFF1E293B),
        border = BorderStroke(
            width = if (checked) 1.5.dp else 1.dp,
            color = if (checked) accentColor else Color(0xFF334155)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
            .testTag(testTag)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (checked) accentColor else Color(0xFFCBD5E1),
                    modifier = Modifier.size(22.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = if (checked) accentColor else Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                }
                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange
                )
            }
            extraContent()
        }
    }
}

data class LayerInfo(val mode: MapMode, val title: String, val desc: String, val tilt: Float)
