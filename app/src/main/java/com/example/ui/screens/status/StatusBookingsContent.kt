package com.example.ui.screens.status

import androidx.compose.foundation.background
import com.example.ui.*
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BookingEntity
import com.example.data.models.InstantRequestEntity
import com.example.utils.VisualThemePalette

/**
 * 📋 StatusBookingsContent
 * Displays active system bookings and urgent instant requests status.
 */
@Composable
fun StatusBookingsContent(
    bookings: List<BookingEntity>,
    instantRequests: List<InstantRequestEntity>,
    themeColors: VisualThemePalette,
    modifier: Modifier = Modifier
) {
    var selectedBookingFilter by remember { mutableStateOf("ALL") }

    val filteredBookings = remember(bookings, selectedBookingFilter) {
        when (selectedBookingFilter) {
            "PENDING" -> bookings.filter { it.status.uppercase() == "PENDING" }
            "APPROVED" -> bookings.filter { it.status.uppercase() == "APPROVED" || it.status.uppercase() == "STARTED" }
            "COMPLETED" -> bookings.filter { it.status.uppercase() == "COMPLETED" || it.status.uppercase() == "FINISHED" }
            "CANCELLED" -> bookings.filter { it.status.uppercase() == "CANCELLED" || it.status.uppercase() == "REJECTED" }
            else -> bookings
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "⚡ الطلبات العاجلة في النظام (${instantRequests.size})",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = themeColors.textPrimary
            )
        }

        if (instantRequests.isEmpty()) {
            item {
                Text(
                    text = "لا توجد طلبات عاجلة حالية",
                    fontSize = 13.sp,
                    color = themeColors.textSecondary
                )
            }
        } else {
            items(instantRequests, key = { "instant_${it.id}" }) { req ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = themeColors.surface,
                    shadowElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, themeColors.border.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${req.requestCode} - ${req.serviceTitle}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = themeColors.textPrimary
                            )
                            Text(
                                text = "📍 ${req.userCity} - ${req.userNeighborhood}",
                                fontSize = 12.sp,
                                color = themeColors.textSecondary
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFEE2E2))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = req.status,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFDC2626)
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📋 الحجوزات في النظام (${filteredBookings.size})",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = themeColors.textPrimary
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    "ALL" to "الكل",
                    "PENDING" to "معلق",
                    "APPROVED" to "جاري",
                    "COMPLETED" to "مكتمل",
                    "CANCELLED" to "ملغي"
                ).forEach { (key, label) ->
                    FilterChip(
                        selected = selectedBookingFilter == key,
                        onClick = { selectedBookingFilter = key },
                        label = { Text(label, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = themeColors.primary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        if (filteredBookings.isEmpty()) {
            item {
                Text(
                    text = "لا توجد حجوزات مطابقة للفلتر المحدد",
                    fontSize = 13.sp,
                    color = themeColors.textSecondary
                )
            }
        } else {
            items(filteredBookings, key = { "booking_${it.id}" }) { booking ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = themeColors.surface,
                    shadowElevation = 1.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, themeColors.border.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = booking.serviceType.ifBlank { "خدمة عامة" },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = themeColors.textPrimary
                            )
                            Text(
                                text = "${booking.totalAmount.toInt()} ر.ي",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981)
                            )
                        }
                        Text(
                            text = "العميل: ${booking.clientName.ifBlank { booking.customerName }} | الفني: ${booking.providerName}",
                            fontSize = 12.sp,
                            color = themeColors.textSecondary
                        )
                    }
                }
            }
        }
    }
}
