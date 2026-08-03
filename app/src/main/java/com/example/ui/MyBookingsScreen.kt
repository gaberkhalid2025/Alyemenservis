package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BookingEntity

/**
 * 📅 MyBookingsScreen (`my_bookings_screen`):
 * Isolated destination strictly for Direct Scheduled Appointments with Clinics, Centers, & Technicians.
 * Toolbar Title: "حجوزاتي المباشرة"
 * Tabs: [نشطة] | [مكتملة] | [ملغية]
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyBookingsScreen(
    viewModel: MainViewModel,
    onBackClick: () -> Unit,
    onOpenBookingDetails: (String) -> Unit = {}
) {
    val myBookingsList: List<BookingEntity> by viewModel.userBookings.collectAsState(initial = emptyList())
    var selectedTabIdx by remember { mutableIntStateOf(0) }
    val tabs = listOf("نشطة 🟢", "مكتملة 🏁", "ملغية ❌")

    val filteredList = remember(myBookingsList, selectedTabIdx) {
        when (selectedTabIdx) {
            0 -> myBookingsList.filter { it.status == "PENDING" || it.status == "APPROVED" || it.status == "ACTIVE" }
            1 -> myBookingsList.filter { it.status == "COMPLETED" }
            2 -> myBookingsList.filter { it.status == "REJECTED" || it.status == "CANCELLED" }
            else -> myBookingsList
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("حجوزاتي المباشرة 📅", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        containerColor = Color(0xFF0F172A)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tabs Bar
            TabRow(
                selectedTabIndex = selectedTabIdx,
                containerColor = Color(0xFF1E293B),
                contentColor = Color(0xFF10B981)
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIdx == index,
                        onClick = { selectedTabIdx = index },
                        text = { Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📅", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "لا توجد حجوزات مباشرة في هذه القائمة حالياً",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "يمكنك حجز المواعيد المباشرة مع المراكز والعيادات والفنيين عبر زر الحجز المباشر",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredList) { booking ->
                        BookingCardItem(booking = booking, onClick = { onOpenBookingDetails(booking.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun BookingCardItem(
    booking: BookingEntity,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "حجز: ${booking.providerName.ifEmpty { "مركز/فني" }}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White
                )

                Surface(
                    color = when (booking.status) {
                        "APPROVED", "ACTIVE" -> Color(0xFF10B981)
                        "COMPLETED" -> Color(0xFF3B82F6)
                        else -> Color(0xFFEF4444)
                    },
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = booking.status,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "الخدمة: ${booking.serviceType.ifEmpty { "حجز موعد مباشر" }}",
                fontSize = 12.sp,
                color = Color(0xFFCBD5E1)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📆 ${booking.dateString} - ${booking.timeString}",
                    fontSize = 11.sp,
                    color = Color(0xFF38BDF8),
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "🔑 كود الحجز: ${booking.pinCode.ifEmpty { booking.bookingPassword.ifEmpty { "----" } }}",
                    fontSize = 11.sp,
                    color = Color(0xFFFFD700),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
