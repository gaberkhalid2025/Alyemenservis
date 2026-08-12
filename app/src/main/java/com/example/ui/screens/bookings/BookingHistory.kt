package com.example.ui.screens.bookings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.utils.VisualThemePalette

data class BookingLog(
    val id: String,
    val date: String,
    val status: String,
    val description: String,
    val reason: String = ""
)

@Composable
fun BookingHistory(
    themeColors: VisualThemePalette,
    logs: List<BookingLog> = listOf(
        BookingLog("1", "2026-08-11 10:00", "تعديل الموعد", "تم تأجيل موعد الخدمة تلبية لطلب العميل"),
        BookingLog("2", "2026-08-10 14:30", "إلغاء حجز", "إلغاء الحجز بسبب عدم توفر فني بديل في الموعد المختار", "ظرف فني طارئ لدى مقدم الخدمة")
    )
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "📋 سجل العمليات وتعديلات الحجز",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = themeColors.textPrimary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (logs.isEmpty()) {
                Text(
                    text = "لا توجد تعديلات سابقة مسجلة لهذا الحجز.",
                    fontSize = 11.sp,
                    color = themeColors.textSecondary,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                logs.forEach { log ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .background(color = themeColors.background, shape = RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⚡ ${log.status}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (log.status.contains("إلغاء")) Color.Red else themeColors.accent
                            )
                            Text(
                                text = log.date,
                                fontSize = 10.sp,
                                color = themeColors.textSecondary
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = log.description,
                            fontSize = 11.sp,
                            color = themeColors.textPrimary
                        )

                        if (log.reason.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 4.dp)
                            ) {
                                Icon(Icons.Default.Info, null, tint = Color.Red, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "السبب: ${log.reason}",
                                    fontSize = 10.sp,
                                    color = Color.Red,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
