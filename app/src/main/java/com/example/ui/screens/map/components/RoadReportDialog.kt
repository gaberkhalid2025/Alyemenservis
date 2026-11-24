package com.example.ui.screens.map.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.utils.HaversinePrecisionNavigator
import com.example.utils.LandmarkAddress
import com.example.utils.RoadReportEntity
import com.example.utils.RoadReportRepository
import com.example.utils.RoadReportType
import kotlinx.coroutines.launch

/**
 * 🚧 RoadReportDialog
 * واجهة السحابة التجميعية لبلاغات الطريق (أسلوب Waze) الموفرة لحصة Firebase Spark.
 */
@Composable
fun RoadReportDialog(
    targetLat: Double,
    targetLng: Double,
    hasValidLocation: Boolean,
    reports: List<RoadReportEntity>,
    onReportsUpdated: (List<RoadReportEntity>) -> Unit,
    onFeedback: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedType by remember { mutableStateOf(RoadReportType.HAZARD) }
    var description by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF0F172A),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🚧 بلاغات الطريق التشاركية",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                if (!isRefreshing && LandmarkAddress.isValid(targetLat, targetLng)) {
                                    isRefreshing = true
                                    scope.launch {
                                        val res = RoadReportRepository.fetchNearbyReports(targetLat, targetLng, forceRefresh = true)
                                        isRefreshing = false
                                        res.onSuccess {
                                            onReportsUpdated(it)
                                            onFeedback("تم تحديث بلاغات المنطقة المحيطة")
                                        }.onFailure {
                                            onFeedback("تعذر التحديث الآن")
                                        }
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "تحديث البلاغات", tint = Color(0xFF38BDF8))
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color(0xFF94A3B8))
                        }
                    }
                }

                Text(
                    text = "بلاغات قريبة (${reports.size}) — تُحدّث بالكاش الذكي لحماية الباقة والحصة المجانية",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 170.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (reports.isEmpty()) {
                        item {
                            Surface(
                                color = Color(0xFF1E293B),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "الطريق حولك سالك حالياً — لا توجد بلاغات نشطة في هذا النطاق.",
                                    color = Color(0xFF10B981),
                                    fontSize = 11.5.sp,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    } else {
                        items(reports, key = { it.id }) { report ->
                            val distM = if (LandmarkAddress.isValid(targetLat, targetLng)) {
                                HaversinePrecisionNavigator.calculateDistance(targetLat, targetLng, report.latitude, report.longitude).toInt()
                            } else 0
                            Surface(
                                color = Color(0xFF1E293B),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFF334155)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = report.title.ifBlank { RoadReportType.fromCode(report.type).labelAr },
                                            color = Color.White,
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (report.description.isNotBlank()) {
                                            Text(report.description, color = Color(0xFFCBD5E1), fontSize = 11.sp)
                                        }
                                        Text(
                                            text = "يبعد ~${distM}م • تأكيدات: ${report.confirmCount}",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 10.5.sp
                                        )
                                    }
                                    TextButton(
                                        onClick = {
                                            scope.launch {
                                                RoadReportRepository.confirmReport(context, report).onSuccess {
                                                    val refreshed = RoadReportRepository.getCachedNearby(targetLat, targetLng)
                                                    if (refreshed.isNotEmpty()) onReportsUpdated(refreshed)
                                                    onFeedback("شكراً! تم تأكيد البلاغ ✅")
                                                }.onFailure { e ->
                                                    onFeedback(e.message ?: "تعذر التأكيد")
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.ThumbUp, contentDescription = "تأكيد البلاغ", tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("تأكيد", color = Color(0xFF10B981), fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFF334155))

                Text(
                    text = "أضف بلاغ طريق في موقعك الحالي:",
                    color = Color(0xFFFBBF24),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(RoadReportType.entries) { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text("${type.emoji} ${type.labelAr}", fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { if (it.length <= 160) description = it },
                    label = { Text("وصف مختصر (اختياري)") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = Color(0xFF38BDF8),
                        unfocusedLabelColor = Color(0xFF94A3B8)
                    )
                )

                Button(
                    onClick = {
                        if (!hasValidLocation) {
                            onFeedback("فعّل GPS أو حدد نقطة على الخريطة لإرسال البلاغ")
                            return@Button
                        }
                        if (!isSubmitting) {
                            isSubmitting = true
                            scope.launch {
                                val res = RoadReportRepository.submitReport(
                                    context = context,
                                    type = selectedType,
                                    lat = targetLat,
                                    lng = targetLng,
                                    description = description
                                )
                                isSubmitting = false
                                res.onSuccess { created ->
                                    onReportsUpdated((listOf(created) + reports).distinctBy { it.id })
                                    onFeedback("تم نشر بلاغ الطريق وتنبيه السائقين 🚧")
                                    onDismiss()
                                }.onFailure { e ->
                                    onFeedback(e.message ?: "تعذر إرسال البلاغ")
                                }
                            }
                        }
                    },
                    enabled = !isSubmitting,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("submit_road_report_btn")
                ) {
                    Text(
                        text = if (isSubmitting) "جاري الإرسال..." else "نشر البلاغ الآن 🚧",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
