package com.example.ui.screens.map

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.utils.GnssMonitor
import com.example.utils.GnssQuality

@Composable
fun GnssPanelDialog(accuracyM: Float?, onOpenLandmarkAddress: () -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val monitor = remember { GnssMonitor(context) }
    val snap by monitor.state.collectAsState()
    var started by remember { mutableStateOf(true) }
    DisposableEffect(Unit) { started = monitor.start(); onDispose { monitor.stop() } }

    val (color, label) = when (snap.quality) {
        GnssQuality.GOOD -> Color(0xFF10B981) to "إشارة ممتازة"
        GnssQuality.FAIR -> Color(0xFFF59E0B) to "إشارة متوسطة"
        GnssQuality.POOR -> Color(0xFFEF4444) to "إشارة ضعيفة"
        GnssQuality.NONE -> Color(0xFF64748B) to "لا إشارة أقمار"
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        title = { Text("حالة إشارة GPS", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!started) Text("امنح إذن الموقع الدقيق لعرض حالة الأقمار.", color = Color(0xFFF59E0B), fontSize = 13.sp)
                else if (!monitor.isGpsProviderEnabled) Text("خدمة GPS مطفأة في الجهاز. فعّلها من الإعدادات.", color = Color(0xFFF59E0B), fontSize = 13.sp)
                Text(label, color = color, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                Text("الأقمار: ${snap.used} مستعملة من ${snap.visible} مرئية", color = Color.White, fontSize = 14.sp)
                LinearProgressIndicator(
                    progress = { (snap.avgCn0 / 45f).coerceIn(0f, 1f) },
                    color = color, trackColor = Color(0xFF334155), modifier = Modifier.fillMaxWidth().height(8.dp)
                )
                Text("قوة الإشارة (متوسط): ${"%.0f".format(snap.avgCn0)} dB-Hz", color = Color(0xFFCBD5E1), fontSize = 12.sp)
                Text(
                    accuracyM?.takeIf { it < Float.MAX_VALUE }?.let { "دقة الموقع الحالية: ±${it.toInt()} م" } ?: "دقة الموقع: غير متاحة بعد",
                    color = Color(0xFFCBD5E1), fontSize = 12.sp
                )
                Text(
                    when (snap.quality) {
                        GnssQuality.GOOD -> "الموقع موثوق للملاحة."
                        GnssQuality.FAIR -> "قد تتأخر المنعطفات قليلاً. ابتعد عن المباني العالية."
                        GnssQuality.POOR, GnssQuality.NONE -> "انتقل لمكان مفتوح أو استعمل «عنواني بالمعلم» لتحديد موقعك بدقة."
                    },
                    color = Color(0xFF94A3B8), fontSize = 12.sp
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("إغلاق", color = Color(0xFF00E5FF)) } },
        dismissButton = { TextButton(onClick = { onDismiss(); onOpenLandmarkAddress() }) { Text("عنواني بالمعلم", color = Color(0xFF10B981)) } }
    )
}
