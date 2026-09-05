package com.example.ui.screens.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
    onUpdateOrderStatus: ((id: String, status: String) -> Unit)? = null
) {
    var selectedFilter by remember { mutableStateOf("ALL") }

    val filteredBookings = remember(bookings, selectedFilter) {
        when (selectedFilter) {
            "PENDING" -> bookings.filter { it.status == "PENDING" }
            "ACTIVE" -> bookings.filter { it.status in listOf("APPROVED", "IN_PROGRESS", "IN_PREPARATION", "READY") }
            "COMPLETED" -> bookings.filter { it.status in listOf("COMPLETED", "DELIVERED") }
            "REJECTED" -> bookings.filter { it.status in listOf("REJECTED", "CANCELLED") }
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

        // Filter chips
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val filters = listOf("ALL" to "الكل", "PENDING" to "جديدة ⏳", "ACTIVE" to "حالية 📋", "COMPLETED" to "مكتملة 🏁")
            filters.forEach { (key, label) ->
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
                    UnifiedBookingsSection(
                        bookingId = booking.id,
                        clientName = booking.customerName.ifBlank { booking.clientName },
                        clientPhone = booking.customerPhone.ifBlank { booking.clientPhone },
                        serviceTitle = booking.serviceType.ifBlank { booking.serviceDetails },
                        dateString = booking.dateString.ifBlank { booking.date },
                        timeString = booking.timeString.ifBlank { booking.time },
                        status = booking.status,
                        rejectionReason = booking.rejectionReason,
                        themeColors = themeColors,
                        onAcceptClick = { onAcceptBooking(booking.id) },
                        onRejectClick = { reason -> onRejectBooking(booking.id, reason) },
                        onStartProgressClick = { onStartProgress(booking.id) },
                        onCompleteClick = { onCompleteBooking(booking.id) },
                        onChatClick = { onChatWithClient(booking.clientId.ifBlank { booking.customerPhone }) },
                        onUpdateOrderStatus = onUpdateOrderStatus?.let { func -> { status -> func(booking.id, status) } }
                    )
                }
            }
        }
    }
}
