package com.example.ui.screens.map.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.utils.NavConfig

@Composable
internal fun UnifiedAnalysisRadarTabContent(
    enableLiveTrafficByAdmin: Boolean,
    liveTrafficEnabled: Boolean,
    onToggleLiveTraffic: (Boolean) -> Unit,
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
    cellCount: Int,
    onRequestClearCoverage: () -> Unit,
    isVisible: (String) -> Boolean,
    orderOf: (String, Int) -> Int,
    labelOf: (String, String) -> String,
    onDismiss: () -> Unit
) {
    SectionTitle("📊 أدوات المراقبة وتحليل الطريق", Color(0xFF10B981))

    val analysisRows = listOf("LIVE_TRAFFIC", "RAIN_RADAR", "RELIABILITY", "PEAK_HOURS", "COVERAGE_GAPS", "ROAD_HAZARDS")
        .filter { isVisible(it) }
        .sortedBy { orderOf(it, 99) }

    analysisRows.forEach { layerId ->
        when (layerId) {
            "LIVE_TRAFFIC" -> if (enableLiveTrafficByAdmin) {
                LayerRow(
                    title = labelOf("LIVE_TRAFFIC", "🚦 بلاغات الطرق والازدحام والوقود الحية"),
                    subtitle = "تنبيهات مجتمعية فورية للسيول والازدحام ومحطات الوقود",
                    checked = liveTrafficEnabled,
                    onChange = { onToggleLiveTraffic(it); onDismiss() },
                    testTag = "unified_live_traffic_switch"
                )
            }
            "RAIN_RADAR" -> if (NavConfig.ENABLE_RAINVIEWER) {
                LayerRow(
                    title = labelOf("RAIN_RADAR", "رادار الأمطار (RainViewer)"),
                    subtitle = "تغطية السحب والأمطار الأخيرة",
                    checked = rainEnabled,
                    enabled = true,
                    onChange = { onToggleRain(it); onDismiss() },
                    testTag = "unified_rain_switch"
                )
            }
            "RELIABILITY" -> {
                LayerRow(
                    title = labelOf("RELIABILITY", "مناطق توفر وموثوقية الخدمات"),
                    subtitle = "توزيع الفنيين والمتاجر المتاحين حولك",
                    checked = reliabilityEnabled,
                    onChange = { onToggleReliability(it); onDismiss() },
                    testTag = "unified_reliability_switch"
                )
            }
            "PEAK_HOURS" -> {
                LayerRow(
                    title = labelOf("PEAK_HOURS", "التنبؤ بساعات الذروة والطلب"),
                    subtitle = "تحليل محلي لسجل الحجوزات حسب الوقت",
                    checked = peakEnabled,
                    onChange = { onTogglePeak(it); onDismiss() },
                    testTag = "unified_peak_switch"
                )
            }
            "COVERAGE_GAPS" -> {
                LayerRow(
                    title = labelOf("COVERAGE_GAPS", "مناطق ضعف الشبكة المتعلمة محلياً"),
                    subtitle = "نقاط الانقطاع المرصودة على جهازك ($cellCount خلية)",
                    checked = coverageEnabled,
                    onChange = { onToggleCoverage(it); onDismiss() },
                    testTag = "unified_coverage_switch"
                )
            }
            "ROAD_HAZARDS" -> {
                LayerRow(
                    title = labelOf("ROAD_HAZARDS", "مناطق وعورة الطريق والمنحدرات"),
                    subtitle = "إبراز العقبات والنقائل الصعبة مع تنبيه مسبق",
                    checked = hazardEnabled,
                    onChange = { onToggleHazard(it); onDismiss() },
                    testTag = "unified_hazard_switch"
                )
            }
        }
    }

    Spacer(Modifier.height(6.dp))
    OutlinedButton(
        onClick = onRequestClearCoverage,
        enabled = cellCount > 0,
        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth().height(46.dp)
    ) {
        Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = Color(0xFFFCA5A5), modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("مسح بيانات التغطية ($cellCount)", color = Color(0xFFFCA5A5), fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
internal fun CoverageClearConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        title = { Text("مسح بيانات التغطية؟", color = Color.White, fontWeight = FontWeight.Bold) },
        text = { Text("سيتم حذف سجل مناطق ضعف الشبكة المحفوظ محلياً على جهازك.", color = Color(0xFFCBD5E1)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("مسح", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", color = Color(0xFF94A3B8))
            }
        }
    )
}
