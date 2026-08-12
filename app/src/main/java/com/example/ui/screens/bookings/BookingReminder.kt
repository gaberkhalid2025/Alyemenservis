package com.example.ui.screens.bookings

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.utils.VisualThemePalette

@Composable
fun BookingReminder(
    themeColors: VisualThemePalette,
    bookingId: String,
    onReminderSet: (String) -> Unit
) {
    val context = LocalContext.current
    var is24hEnabled by remember { mutableStateOf(true) }
    var is1hEnabled by remember { mutableStateOf(true) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "🔔 نظام التذكير الذكي للحجوزات",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = themeColors.textPrimary
            )

            Text(
                text = "اضبط منبهات مخصصة لضمان عدم نسيان موعد الحجز الخاص بك:",
                fontSize = 11.sp,
                color = themeColors.textSecondary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("تنبيه وتذكير قبل الموعد بـ ٢٤ ساعة", fontSize = 12.sp, color = themeColors.textPrimary)
                Switch(
                    checked = is24hEnabled,
                    onCheckedChange = {
                        is24hEnabled = it
                        Toast.makeText(context, if (it) "تم تفعيل التذكير قبل 24 ساعة" else "تم إلغاء التذكير", Toast.LENGTH_SHORT).show()
                        onReminderSet("24h_before")
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("تنبيه وتذكير قبل الموعد بساعة واحدة", fontSize = 12.sp, color = themeColors.textPrimary)
                Switch(
                    checked = is1hEnabled,
                    onCheckedChange = {
                        is1hEnabled = it
                        Toast.makeText(context, if (it) "تم تفعيل التذكير قبل ساعة واحدة" else "تم إلغاء التذكير", Toast.LENGTH_SHORT).show()
                        onReminderSet("1h_before")
                    }
                )
            }
        }
    }
}
