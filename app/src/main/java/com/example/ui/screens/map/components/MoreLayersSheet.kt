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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.utils.CoverageLog
import com.example.utils.NavConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreLayersSheet(
    nightMode: Boolean,
    onToggleNight: (Boolean) -> Unit,
    gibsEnabled: Boolean,
    onToggleGibs: (Boolean) -> Unit,
    eoxEnabled: Boolean,
    onToggleEox: (Boolean) -> Unit,
    quakesEnabled: Boolean,
    onToggleQuakes: (Boolean) -> Unit,
    rainEnabled: Boolean,
    onToggleRain: (Boolean) -> Unit,
    reliabilityEnabled: Boolean,
    onToggleReliability: (Boolean) -> Unit,
    peakEnabled: Boolean,
    onTogglePeak: (Boolean) -> Unit,
    coverageEnabled: Boolean,
    onToggleCoverage: (Boolean) -> Unit,
    hazardEnabled: Boolean,
    onToggleHazard: (Boolean) -> Unit,
    onCoverageCleared: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var showClearConfirm by remember { mutableStateOf(false) }
    var cellCount by remember { mutableIntStateOf(CoverageLog.cellCount(context)) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        contentColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.3f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Layers, contentDescription = null, tint = Color(0xFF00E5FF))
                Text(
                    "طبقات إضافية مجانية (بدون مفاتيح API)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            LayerToggleRow(
                icon = Icons.Default.Bedtime,
                title = "الوضع الليلي للخريطة",
                subtitle = "حماية العين ليلاً مع عكس مريح لخرائط الشوارع",
                checked = nightMode,
                onCheckedChange = onToggleNight,
                testTag = "layer_night_switch"
            )
            LayerToggleRow(
                icon = Icons.Default.SatelliteAlt,
                title = "صور ناسا الفضائية (NASA GIBS)",
                subtitle = "صور أقمار صناعية يومية مجانية من NASA EOSDIS",
                checked = gibsEnabled,
                onCheckedChange = onToggleGibs,
                testTag = "layer_gibs_switch"
            )
            LayerToggleRow(
                icon = Icons.Default.Public,
                title = "صور EOX Sentinel-2 (2016 بدون سحب)",
                subtitle = "خريطة فضائية نظيفة CC-BY-4.0 بلا قيود أو مفاتيح",
                checked = eoxEnabled,
                onCheckedChange = onToggleEox,
                testTag = "layer_eox_switch"
            )
            LayerToggleRow(
                icon = Icons.Default.WarningAmber,
                title = "زلازل USGS (≥ 2.5 آخر أسبوع)",
                subtitle = "عرض الهزات المسجلة عالمياً مجاناً مع تفاصيل كل نقطة",
                checked = quakesEnabled,
                onCheckedChange = onToggleQuakes,
                testTag = "layer_quakes_switch"
            )
            LayerToggleRow(
                icon = Icons.Default.Cloud,
                title = "رادار الأمطار (RainViewer)",
                subtitle = if (NavConfig.ENABLE_RAINVIEWER) {
                    "تغطية السحب والأمطار الأخيرة"
                } else {
                    "معطّلة افتراضياً (بدون مفاتيح مدفوعة)"
                },
                checked = rainEnabled,
                enabled = NavConfig.ENABLE_RAINVIEWER,
                onCheckedChange = onToggleRain,
                testTag = "layer_rain_switch"
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
            Text("طبقات التحليل الأوفلاين:", fontSize = 12.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)

            LayerToggleRow(
                icon = Icons.Default.PieChart,
                title = "مناطق توفر وموثوقية الخدمات",
                subtitle = "توزيع الفنيين والمتاجر المتاحين حولك",
                checked = reliabilityEnabled,
                onCheckedChange = onToggleReliability,
                testTag = "layer_reliability_switch"
            )
            LayerToggleRow(
                icon = Icons.Default.TrendingUp,
                title = "التنبؤ بساعات الذروة والطلب",
                subtitle = "تحليل محلي لسجل الحجوزات حسب الوقت",
                checked = peakEnabled,
                onCheckedChange = onTogglePeak,
                testTag = "layer_peak_switch"
            )
            LayerToggleRow(
                icon = Icons.Default.SignalCellularOff,
                title = "مناطق ضعف الشبكة المتعلمة محلياً",
                subtitle = "نقاط الانقطاع المرصودة ذاتياً على جهازك ($cellCount خلية)",
                checked = coverageEnabled,
                onCheckedChange = onToggleCoverage,
                testTag = "layer_coverage_switch"
            )
            LayerToggleRow(
                icon = Icons.Default.Terrain,
                title = "مناطق وعورة الطريق والمنحدرات",
                subtitle = "إبراز العقبات والنقائل الصعبة مع تنبيه صوتي مسبق",
                checked = hazardEnabled,
                onCheckedChange = onToggleHazard,
                testTag = "layer_hazard_switch"
            )

            OutlinedButton(
                onClick = { showClearConfirm = true },
                enabled = cellCount > 0,
                border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("clear_coverage_data_btn")
            ) {
                Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = Color(0xFFFCA5A5), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("مسح بيانات التغطية ($cellCount)", color = Color(0xFFFCA5A5), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(12.dp))
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            containerColor = Color(0xFF0F172A),
            title = { Text("مسح بيانات التغطية؟", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("سيتم حذف سجل مناطق ضعف الشبكة المحفوظ محلياً على جهازك.", color = Color(0xFFCBD5E1)) },
            confirmButton = {
                TextButton(onClick = {
                    CoverageLog.clear(context)
                    cellCount = 0
                    showClearConfirm = false
                    onCoverageCleared()
                }) { Text("مسح", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("إلغاء", color = Color(0xFF94A3B8)) }
            }
        )
    }
}

@Composable
private fun LayerToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String,
    enabled: Boolean = true
) {
    Surface(
        onClick = { if (enabled) onCheckedChange(!checked) },
        shape = RoundedCornerShape(14.dp),
        color = if (checked) Color(0xFF1E293B) else Color(0xFF1E293B).copy(alpha = 0.45f),
        border = if (checked) BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.45f)) else null,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (!enabled) Color(0xFF64748B) else if (checked) Color(0xFF00E5FF) else Color(0xFFCBD5E1),
                modifier = Modifier.size(22.dp)
            )
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    color = if (enabled) Color.White else Color(0xFF64748B),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    subtitle,
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = if (enabled) onCheckedChange else null,
                enabled = enabled
            )
        }
    }
}
