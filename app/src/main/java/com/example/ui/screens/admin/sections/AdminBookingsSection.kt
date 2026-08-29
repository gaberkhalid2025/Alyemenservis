package com.example.ui.screens.admin.sections

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.utils.VisualThemePalette

@Composable
fun AdminBookingsSection(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    modifier: Modifier = Modifier
) {
    val bookings by viewModel.bookings.collectAsState()
    val context = LocalContext.current
    var deleteBookingId by remember { mutableStateOf<String?>(null) }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "📅 إدارة الحجوزات والمواعيد المركزية (${bookings.size})",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (bookings.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "لا توجد حجوزات مسجلة حالياً 📋",
                    color = Color.Gray,
                    fontSize = 13.sp
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp)
            ) {
                items(bookings, key = { it.id }) { booking ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "حجز: ${booking.customerName.ifEmpty { booking.clientName }}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.5.sp,
                                    color = Color.White
                                )
                                Surface(
                                    color = when (booking.status) {
                                        "CONFIRMED", "APPROVED" -> Color(0xFF10B981).copy(alpha = 0.2f)
                                        "PENDING" -> Color(0xFFFFB300).copy(alpha = 0.2f)
                                        else -> Color(0xFFEF5350).copy(alpha = 0.2f)
                                    },
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = booking.status,
                                        color = when (booking.status) {
                                            "CONFIRMED", "APPROVED" -> Color(0xFF10B981)
                                            "PENDING" -> Color(0xFFFFB300)
                                            else -> Color(0xFFEF5350)
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = "👨‍🔧 الفني/المركز: ${booking.providerName}", fontSize = 12.sp, color = Color.LightGray)
                            Text(text = "📆 الموعد: ${booking.date} ${booking.time}", fontSize = 12.sp, color = Color.LightGray)
                            Text(text = "💰 المبلغ: ${booking.totalAmount} ر.ي", fontSize = 12.sp, color = Color(0xFF64FFDA))

                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                IconButton(onClick = { deleteBookingId = booking.id }) {
                                    Icon(Icons.Default.Delete, contentDescription = "حذف الحجز", tint = Color(0xFFEF5350))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    deleteBookingId?.let { id ->
        AlertDialog(
            onDismissRequest = { deleteBookingId = null },
            title = { Text("تأكيد حذف الحجز", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold) },
            text = { Text("هل أنت تأكد من حذف هذا الحجز نهائياً من النظام؟", color = Color.LightGray, fontSize = 13.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteBooking(id)
                        Toast.makeText(context, "تم حذف الحجز بنجاح", Toast.LENGTH_SHORT).show()
                        deleteBookingId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350))
                ) {
                    Text("حذف الآن", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteBookingId = null }) {
                    Text("إلغاء", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }
}
