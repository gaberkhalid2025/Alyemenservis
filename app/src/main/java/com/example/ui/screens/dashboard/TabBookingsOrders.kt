package com.example.ui.screens.dashboard

import androidx.compose.foundation.layout.*
import com.example.ui.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BookingEntity
import com.example.ui.screens.dashboard.components.UnifiedBookingsSection
import com.example.ui.screens.dashboard.components.UnifiedEmptyState
import com.example.utils.VisualThemePalette

@Composable
fun TabBookingsOrders(
    bookings: List<BookingEntity>,
    themeColors: VisualThemePalette,
    onAcceptBooking: (id: String) -> Unit,
    onRejectBooking: (id: String, reason: String) -> Unit,
    onStartProgress: (id: String) -> Unit,
    onCompleteBooking: (id: String) -> Unit,
    onChatWithClient: (clientId: String) -> Unit,
    onUpdateOrderStatus: ((id: String, status: String) -> Unit)? = null,
    showFilters: Boolean = true
) {
    var selectedFilter by remember { mutableStateOf("ALL") }

    val filteredBookings = remember(bookings, selectedFilter) {
        when (selectedFilter) {
            com.example.utils.BookingStatus.PENDING.code -> bookings.filter { it.status.equals(com.example.utils.BookingStatus.PENDING.code, ignoreCase = true) }
            "ACTIVE" -> bookings.filter { it.status.uppercase() in listOf(com.example.utils.BookingStatus.ACCEPTED.code, com.example.utils.BookingStatus.IN_PROGRESS.code, "APPROVED", "IN_PREPARATION", "READY") }
            "COMPLETED" -> bookings.filter { it.status.uppercase() in listOf(com.example.utils.BookingStatus.COMPLETED.code, com.example.utils.BookingStatus.PAID.code, com.example.utils.BookingStatus.CLOSED.code, "DELIVERED") }
            "REJECTED" -> bookings.filter { it.status.uppercase() in listOf(com.example.utils.BookingStatus.REJECTED.code, com.example.utils.BookingStatus.CANCELLED.code) }
            else -> bookings
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "📅 قائمة الحجوزات والطلبات (${filteredBookings.size})", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = themeColors.textPrimary)
        }

        if (showFilters) {
            val filters = listOf(
                "ALL" to "الكل (${bookings.size})",
                "PENDING" to "جديدة ⏳",
                "ACTIVE" to "حالية 📋",
                "COMPLETED" to "مكتملة 🏁",
                "REJECTED" to "مرفوضة/ملغاة ❌"
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(filters) { (key, label) ->
                    FilterChip(
                        selected = selectedFilter == key,
                        onClick = { selectedFilter = key },
                        label = { Text(label, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = themeColors.accent,
                            selectedLabelColor = androidx.compose.ui.graphics.Color.Black
                        )
                    )
                }
            }
        }

        if (filteredBookings.isEmpty()) {
            UnifiedEmptyState(
                title = "لا توجد طلبات أو حجوزات مسجلة",
                description = "تظهر هنا أي طلبات حجز أو شراء موجهة إليك.",
                iconText = "📋",
                themeColors = themeColors
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(filteredBookings, key = { it.id }) { booking ->
                    val resolvedClientId = booking.clientId
                        .ifBlank { booking.customerPhone }
                        .ifBlank { booking.clientPhone }
                        .ifBlank { booking.userPhone }
                    UnifiedBookingsSection(
                        bookingId = booking.id,
                        clientName = booking.customerName.ifBlank { booking.clientName }.ifBlank { booking.userName }.ifBlank { "عميل" },
                        clientPhone = booking.customerPhone.ifBlank { booking.clientPhone }.ifBlank { booking.userPhone },
                        serviceTitle = booking.serviceType.ifBlank { booking.serviceDetails },
                        dateString = booking.date.ifBlank { booking.dateString },
                        timeString = booking.time.ifBlank { booking.timeString },
                        status = booking.status,
                        rejectionReason = booking.rejectionReason,
                        themeColors = themeColors,
                        onAcceptClick = { onAcceptBooking(booking.id) },
                        onRejectClick = { reason -> onRejectBooking(booking.id, reason) },
                        onStartProgressClick = { onStartProgress(booking.id) },
                        onCompleteClick = { onCompleteBooking(booking.id) },
                        onChatClick = { onChatWithClient(resolvedClientId) },
                        onUpdateOrderStatus = onUpdateOrderStatus?.let { func -> { status -> func(booking.id, status) } }
                    )
                }
            }
        }
    }
}
