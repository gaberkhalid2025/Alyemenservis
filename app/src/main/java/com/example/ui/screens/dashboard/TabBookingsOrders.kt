package com.example.ui.screens.dashboard

import android.widget.Toast
import androidx.compose.foundation.background
import com.example.viewmodels.BookingViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BookingEntity
import com.example.data.UnifiedBusinessAccount

import com.example.utils.VisualThemePalette

/**
 * 📅 Modular Tab: Bookings, Appointments, & Custom Client Orders (إدارة الحجوزات والطلبات والمواعيد)
 */
@Composable
fun TabBookingsOrders(
    account: UnifiedBusinessAccount,
    bookingViewModel: BookingViewModel = viewModel(),
    themeColors: VisualThemePalette
) {
    val context = LocalContext.current
    var selectedFilter by remember { mutableStateOf("ALL") }
    
    val allBookings by bookingViewModel.bookings.collectAsState()
    
    val myBookings = remember(allBookings, account.id, account.phone) {
        allBookings.filter { b ->
            b.providerId == account.id || b.providerId == account.phone
        }
    }

    val filteredBookings = remember(myBookings, selectedFilter) {
        if (selectedFilter == "ALL") myBookings
        else myBookings.filter { it.status == selectedFilter }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "📅 إدارة طلبات الحجوزات والمواعيد",
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold,
            color = themeColors.accent
        )

        // Filter chips list
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val filters = listOf(
                Pair("ALL", "الكل (${myBookings.size})"),
                Pair("PENDING", "قيد الانتظار ⏳"),
                Pair("APPROVED", "مقبولة ✅"),
                Pair("COMPLETED", "مكتملة 🎉"),
                Pair("REJECTED", "ملغاة ❌")
            )
            items(filters) { item ->
                val isSel = selectedFilter == item.first
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSel) themeColors.accent else Color.DarkGray)
                        .clickable { selectedFilter = item.first }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = item.second,
                        fontSize = 10.sp,
                        color = if (isSel) Color.Black else Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (filteredBookings.isEmpty()) {
            UnifiedEmptyState(
                icon = "📅",
                title = "لا توجد حجوزات مسجلة",
                description = "لا توجد أي مواعيد أو حجوزات مطابقة للفلتر المحدد حالياً.",
                themeColors = themeColors,
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(filteredBookings) { booking ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = booking.clientName.ifBlank { "عميل مجهول" },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = when (booking.status) {
                                        "PENDING" -> "قيد الانتظار ⏳"
                                        "APPROVED" -> "تم قبول الطلب ✅"
                                        "COMPLETED" -> "مكتمل 🎉"
                                        else -> "ملغي ❌"
                                    },
                                    fontSize = 10.sp,
                                    color = when (booking.status) {
                                        "PENDING" -> Color(0xFFFFB300)
                                        "APPROVED" -> themeColors.accent
                                        "COMPLETED" -> Color(0xFF10B981)
                                        else -> Color(0xFFEF5350)
                                    },
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Text("📱 رقم الهاتف: ${booking.clientPhone}", fontSize = 10.5.sp, color = Color.LightGray)
                            Text("🗓️ موعد الحجز: ${booking.date} - ${booking.time}", fontSize = 10.5.sp, color = Color.LightGray)
                            if (booking.serviceDetails.isNotBlank()) {
                                Text("💬 التفاصيل: ${booking.serviceDetails}", fontSize = 10.5.sp, color = Color.Gray)
                            }

                            if (booking.status == "PENDING") {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            bookingViewModel.updateBookingStatus(booking.id, "APPROVED")
                                            Toast.makeText(context, "✅ تم قبول الحجز بنجاح ومزامنة الموعد!", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("قبول الموعد ✓", fontSize = 10.sp, color = Color.Black)
                                    }

                                    Button(
                                        onClick = {
                                            bookingViewModel.updateBookingStatus(booking.id, "REJECTED")
                                            Toast.makeText(context, "❌ تم رفض الحجز!", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("رفض الحجز ❌", fontSize = 10.sp, color = Color.White)
                                    }
                                }
                            } else if (booking.status == "APPROVED") {
                                Button(
                                    onClick = {
                                        bookingViewModel.updateBookingStatus(booking.id, "COMPLETED")
                                        Toast.makeText(context, "🎉 تهانينا! تم إكمال الخدمة بنجاح.", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("تعليم كخدمة مكتملة 🎉", fontSize = 10.sp, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
