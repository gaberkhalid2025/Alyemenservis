package com.example.ui.screens.bookings

import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun BookingCalendarScreen(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    serviceId: String = "",
    serviceName: String = "خدمة عامة"
) {
    val context = LocalContext.current
    var selectedDate by remember { mutableStateOf(Calendar.getInstance()) }
    var selectedTimeSlot by remember { mutableStateOf("09:00 صباحاً") }
    var note by remember { mutableStateOf("") }

    val timeSlots = listOf(
        "09:00 صباحاً", "10:30 صباحاً", "12:00 ظهراً",
        "02:00 عصراً", "04:00 عصراً", "06:00 مساءً", "08:00 مساءً"
    )

    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale("ar"))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.goBack() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "رجوع", tint = Color.White)
            }
            Text("📅 حجز موعد جديد", color = themeColors.accent, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(48.dp))
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, themeColors.accent),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("الخدمة المطلوبة: $serviceName", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                
                Divider(color = Color.Gray.copy(alpha = 0.3f))

                Text("اختر تاريخ الموعد:", color = themeColors.accent, fontWeight = FontWeight.Bold)
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        val cal = selectedDate.clone() as Calendar
                        cal.add(Calendar.DAY_OF_MONTH, -1)
                        selectedDate = cal
                    }) {
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = "السابق", tint = Color.White)
                    }
                    Text(
                        text = dateFormat.format(selectedDate.time),
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = {
                        val cal = selectedDate.clone() as Calendar
                        cal.add(Calendar.DAY_OF_MONTH, 1)
                        selectedDate = cal
                    }) {
                        Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "التالي", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text("اختر الوقت المناسب:", color = themeColors.accent, fontWeight = FontWeight.Bold)

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    timeSlots.chunked(3).forEach { rowSlots ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowSlots.forEach { slot ->
                                val isSelected = (selectedTimeSlot == slot)
                                Button(
                                    onClick = { selectedTimeSlot = slot },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) themeColors.accent else Color(0xFF334155)
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = slot,
                                        color = if (isSelected) Color.Black else Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("ملاحظات إضافية للفني / مزود الخدمة") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Button(
                    onClick = {
                        val dateStr = dateFormat.format(selectedDate.time)
                        viewModel.addBooking(
                            name = viewModel.currentUserName.value.ifBlank { "عميل زائر" },
                            phone = viewModel.currentUserPhone.value.ifBlank { "770000000" },
                            area = viewModel.currentUserResidence.value.ifBlank { "صنعاء" },
                            serviceType = serviceName,
                            providerId = serviceId.ifEmpty { "p_general" },
                            providerName = serviceName,
                            dateString = dateStr,
                            timeString = selectedTimeSlot
                        )
                        Toast.makeText(context, "تم تأكيد حجز الموعد بنجاح! 🚀", Toast.LENGTH_LONG).show()
                        viewModel.goBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("تأكيد وحجز الموعد الآن ✨", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
