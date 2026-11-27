package com.example.ui.screens.map.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.map.MapLayer
import com.example.ui.screens.map.MapMode
import com.example.utils.ValhallaTileDownloader

/**
 * 🗺️ PrimaryLayersBottomSheet
 * قائمة مخصصة وحصرية للطبقات الأساسية فقط (بدون أي تبويبات أو طبقات ثانوية):
 * - وضع أوفلاين Valhalla
 * - عرض التضاريس ثلاثية الأبعاد (3D Terrain)
 * - خرائط الأساس (VELA المجانية، Esri Clarity، Esri القياسية، OSM، HOT، Topo)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrimaryLayersBottomSheet(
    mapLayer: MapLayer,
    onToggleMapLayer: (MapLayer) -> Unit,
    isValhallaDownloaded: Boolean,
    valhallaSizeMb: Int,
    onOpenValhallaDownload: () -> Unit,
    currentMapMode: MapMode,
    onSelectMapMode: (MapMode) -> Unit,
    is3DAvailable: Boolean,
    is3DTerrainEnabled: Boolean,
    onToggle3DTerrain: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val valhallaMb = if (valhallaSizeMb > 0) valhallaSizeMb else ValhallaTileDownloader.EXPECTED_INSTALLED_MB

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        contentColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.3f)) },
        modifier = Modifier.testTag("primary_layers_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Layers,
                    contentDescription = null,
                    tint = Color(0xFFFBBF24),
                    modifier = Modifier.size(26.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "الطبقات الأساسية للخريطة",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "خرائط الأساس، وضع أوفلاين، والتضاريس 3D",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
                Surface(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E293B),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("primary_layers_minimize_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "تصغير وإغلاق",
                            tint = Color(0xFFCBD5E1),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "تصغير",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFCBD5E1)
                        )
                    }
                }
            }

            SectionTitle("وضع التشغيل والمنظور ثلاثي الأبعاد", Color(0xFF10B981))

            SheetSwitchCard(
                icon = Icons.Default.OfflinePin,
                title = "خرائط بلا إنترنت",
                subtitle = if (isValhallaDownloaded) {
                    "حزمة اليمن جاهزة محلياً (~$valhallaMb ميجابايت) — توفير 100% للبيانات"
                } else {
                    "استعمل الخريطة المحفوظة على الجهاز (حزم المناطق)"
                },
                checked = mapLayer == MapLayer.VALHALLA_OFFLINE,
                onCheckedChange = { checked ->
                    onToggleMapLayer(if (checked) MapLayer.VALHALLA_OFFLINE else MapLayer.STANDARD)
                    onDismiss()
                },
                accentColor = Color(0xFF10B981),
                testTag = "sheet_valhalla_offline_switch"
            ) {
                if (!isValhallaDownloaded) {
                    Spacer(Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onOpenValhallaDownload()
                        },
                        border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("sheet_download_valhalla_btn")
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "حزمة التوجيه بلا إنترنت (${ValhallaTileDownloader.EXPECTED_DOWNLOAD_MB} ميجابايت)",
                            color = Color(0xFFFBBF24),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            SheetSwitchCard(
                icon = Icons.Default.Terrain,
                title = "عرض التضاريس ثلاثية الأبعاد (3D Terrain)",
                subtitle = if (is3DAvailable) {
                    "ارتفاعات الجبال وإمالة الكاميرا (62°) مع حماية تلقائية ضد الشاشة السوداء"
                } else {
                    "وضع المنظور المائل 3D التوافقي للخرائط"
                },
                checked = is3DTerrainEnabled,
                enabled = true,
                onCheckedChange = { enabled ->
                    onToggle3DTerrain(enabled)
                    onDismiss()
                },
                accentColor = Color(0xFFA78BFA),
                testTag = "sheet_3d_terrain_switch"
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

            SectionTitle("🗺️ خرائط الأساس والأقمار عالية الدقة", Color(0xFFFBBF24))

            val baseModes = listOf(
                Triple(MapMode.VELA, "✨ خرائط VELA", "خريطة شوارع حديثة وسريعة بتفاصيل واضحة"),
                Triple(MapMode.SATELLITE_CLARITY, "💎 أقمار فائقة الوضوح (Esri Clarity)", "صور فضائية عالية الحدة للمدن والطرق الجبلية"),
                Triple(MapMode.SATELLITE, "🛰️ أقمار صناعية (Esri القياسية)", "صور جوية عالمية عالية الدقة من Esri"),
                Triple(MapMode.OSM, "🌐 OpenStreetMap القياسية", "خريطة OSM الكاملة بأسماء الشوارع والمعالم"),
                Triple(MapMode.HOT, "🗺️ شوارع HOT الميدانية", "نمط ميداني واضح للطرق والأحياء اليمنية"),
                Triple(MapMode.TOPO, "⛰️ تضاريس OpenTopoMap", "خريطة جغرافية بخطوط الكنتور والارتفاعات")
            )

            baseModes.forEach { (mode, title, desc) ->
                val selected = currentMapMode == mode || (currentMapMode == MapMode.VELA_3D && mode == MapMode.SATELLITE)
                Surface(
                    onClick = {
                        onSelectMapMode(mode)
                        onDismiss()
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = if (selected) Color(0xFF10B981).copy(alpha = 0.18f) else Color(0xFF1E293B).copy(alpha = 0.55f),
                    border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) Color(0xFF10B981) else Color(0xFF334155)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 54.dp)
                        .testTag("sheet_base_${mode.name.lowercase()}_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                title,
                                color = if (selected) Color(0xFF10B981) else Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(desc, color = Color(0xFF94A3B8), fontSize = 11.sp)
                        }
                        if (selected) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "مفعّل", tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun UnifiedMapBaseTabContent(
    currentMapMode: MapMode,
    onSelectMapMode: (MapMode) -> Unit,
    mapLayer: MapLayer,
    onToggleMapLayer: (MapLayer) -> Unit,
    isValhallaDownloaded: Boolean,
    valhallaMb: Int,
    onOpenValhallaDownload: () -> Unit,
    is3DAvailable: Boolean,
    is3DTerrainEnabled: Boolean,
    onToggle3DTerrain: (Boolean) -> Unit,
    nightMode: Boolean,
    onToggleNight: (Boolean) -> Unit,
    adminRole: String,
    isRegisteredUser: Boolean,
    isProvider: Boolean,
    isVisible: (String) -> Boolean,
    orderOf: (String, Int) -> Int,
    labelOf: (String, String) -> String,
    onDismiss: () -> Unit
) {
    SectionTitle("خريطة الأساس", Color(0xFF10B981))

    val allBaseModes = listOf(
        Triple(MapMode.HOT, "HOT", "🗺️ شوارع"),
        Triple(MapMode.SATELLITE, "SATELLITE", "🛰️ أقمار"),
        Triple(MapMode.SATELLITE_CLARITY, "SATELLITE_CLARITY", "💎 وضوح"),
        Triple(MapMode.VELA, "VELA", "✨ VELA"),
        Triple(MapMode.OSM, "OSM", "🌐 قياسي"),
        Triple(MapMode.TOPO, "TOPO", "⛰️ تضاريس")
    )
    val visibleBaseModes = remember(adminRole, isRegisteredUser, isProvider) {
        allBaseModes
            .filter { (_, id, _) -> isVisible(id) }
            .sortedBy { (_, id, _) -> orderOf(id, 99) }
    }

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        visibleBaseModes.forEach { (mode, _, defaultLabel) ->
            val selected = currentMapMode == mode
            FilterChip(
                selected = selected,
                onClick = { onSelectMapMode(mode); onDismiss() },
                label = {
                    Text(
                        defaultLabel,
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color(0xFF1E293B),
                    labelColor = Color(0xFFCBD5E1),
                    selectedContainerColor = Color(0xFF10B981).copy(alpha = 0.22f),
                    selectedLabelColor = Color(0xFF10B981)
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selected,
                    borderColor = Color(0xFF334155),
                    selectedBorderColor = Color(0xFF10B981)
                )
            )
        }
    }

    HorizontalDivider(color = Color.White.copy(alpha = 0.06f))

    if (isVisible("VALHALLA_OFFLINE")) {
        LayerRow(
            title = labelOf("VALHALLA_OFFLINE", "خرائط بلا إنترنت"),
            subtitle = if (isValhallaDownloaded) "حزمة اليمن جاهزة محلياً (~$valhallaMb ميجابايت)" else "استعمل الخريطة المحفوظة على الجهاز (حزم المناطق)",
            checked = mapLayer == MapLayer.VALHALLA_OFFLINE,
            onChange = { checked ->
                onToggleMapLayer(if (checked) MapLayer.VALHALLA_OFFLINE else MapLayer.STANDARD)
                onDismiss()
            },
            testTag = "unified_valhalla_switch"
        )

        if (!isValhallaDownloaded) {
            OutlinedButton(
                onClick = { onDismiss(); onOpenValhallaDownload() },
                border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.6f)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).heightIn(min = 44.dp)
            ) {
                Icon(Icons.Default.CloudDownload, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "حزمة التوجيه بلا إنترنت (${ValhallaTileDownloader.EXPECTED_DOWNLOAD_MB} ميجابايت)",
                    color = Color(0xFFFBBF24),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    if (isVisible("TERRAIN_3D")) {
        LayerRow(
            title = labelOf("TERRAIN_3D", "التضاريس ثلاثية الأبعاد (3D)"),
            subtitle = if (is3DAvailable) "ارتفاعات الجبال وإمالة الكاميرا (62°)" else "وضع المنظور المائل 3D للخرائط",
            checked = is3DTerrainEnabled,
            onChange = { enabled ->
                onToggle3DTerrain(enabled)
                onDismiss()
            },
            testTag = "unified_3d_terrain_switch"
        )
    }

    if (isVisible("NIGHT_MODE")) {
        LayerRow(
            title = labelOf("NIGHT_MODE", "الوضع الليلي للخريطة"),
            subtitle = "تعتيم مريح للعين أثناء القيادة ليلاً",
            checked = nightMode,
            onChange = { onToggleNight(it); onDismiss() },
            testTag = "unified_night_switch"
        )
    }
}
