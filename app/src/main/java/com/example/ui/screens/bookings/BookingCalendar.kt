package com.example.ui.screens.bookings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.utils.VisualThemePalette

@Composable
fun BookingCalendar(
    themeColors: VisualThemePalette,
    onDateSelected: (String) -> Unit
) {
    var selectedDay by remember { mutableIntStateOf(12) }
    val daysInMonth = (1..30).toList()
    val bookedDays = listOf(3, 7, 15, 22)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "📅 اختر تاريخ الحجز المناسب",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = themeColors.textPrimary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("شعبان / رمضان ١٤٤٧ هـ", fontSize = 12.sp, color = themeColors.textSecondary)
                Text("أغسطس ٢٠٢٦ م", fontSize = 12.sp, color = themeColors.accent, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Calendar Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier.height(180.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val weekDays = listOf("ح", "ن", "ث", "ر", "خ", "ج", "س")
                items(weekDays) { day ->
                    Text(
                        text = day,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeColors.textSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                items(daysInMonth) { day ->
                    val isBooked = bookedDays.contains(day)
                    val isSelected = selectedDay == day

                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(CircleShape)
                            .background(
                                color = when {
                                    isSelected -> themeColors.accent
                                    isBooked -> Color.Red.copy(alpha = 0.15f)
                                    else -> Color.Transparent
                                }
                            )
                            .clickable {
                                if (!isBooked) {
                                    selectedDay = day
                                    onDateSelected("2026-08-$day")
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = day.toString(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = when {
                                isSelected -> Color.Black
                                isBooked -> Color.Red
                                else -> themeColors.textPrimary
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Calendar Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
            ) {
                LegendItem(label = "متاح", color = themeColors.textSecondary)
                LegendItem(label = "محدد", color = themeColors.accent)
                LegendItem(label = "محجوز مسبقاً", color = Color.Red)
            }
        }
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color = color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, fontSize = 10.sp, color = color)
    }
}
