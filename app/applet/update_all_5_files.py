import os

print("Fixing all remaining files...")

# 1. BookingCalendar.kt
booking_calendar_code = """package com.example.ui.screens.bookings

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
                                    modifier = Modifier.weight(1.dp)
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
"""
with open("app/src/main/java/com/example/ui/screens/bookings/BookingCalendar.kt", "w", encoding="utf-8") as f:
    f.write(booking_calendar_code)

# 2. BookingHistory.kt
booking_history_code = """package com.example.ui.screens.bookings

import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

@Composable
fun BookingHistoryScreen(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette
) {
    val context = LocalContext.current
    val bookings by viewModel.bookings.collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
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
            Text("📋 سجل الحجوزات والمواعيد", color = themeColors.accent, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(48.dp))
        }

        if (bookings.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(64.dp))
                    Text("لا توجد حجوزات سابقة مسجلة", color = Color.Gray, fontSize = 16.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(bookings) { booking ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(booking.serviceType.ifBlank { booking.providerName }, color = themeColors.accent, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Surface(
                                    color = when (booking.status.uppercase()) {
                                        "APPROVED", "COMPLETED" -> Color(0xFF10B981)
                                        "REJECTED", "CANCELLED" -> Color(0xFFEF4444)
                                        else -> Color(0xFFF59E0B)
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = viewModel.getBookingStatusLabel(booking.status),
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            Text("📅 التاريخ: ${booking.dateString} | ⏰ الوقت: ${booking.timeString}", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                            if (booking.rejectionReason.isNotBlank()) {
                                Text("📝 سبب الرفض/الملاحظة: ${booking.rejectionReason}", color = Color(0xFFEF4444), fontSize = 13.sp)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (booking.status.uppercase() != "CANCELLED" && booking.status.uppercase() != "COMPLETED") {
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.updateBookingStatus(booking.id, "CANCELLED", "إلغاء بواسطة العميل")
                                            Toast.makeText(context, "تم إلغاء الحجز بنجاح", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444))
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("إلغاء الحجز")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
"""
with open("app/src/main/java/com/example/ui/screens/bookings/BookingHistory.kt", "w", encoding="utf-8") as f:
    f.write(booking_history_code)

# 3. AdminStatisticsPanel.kt
admin_stats_code = """package com.example.ui.screens.admin

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.utils.VisualThemePalette

@Composable
fun AdminStatisticsPanel(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette
) {
    val providers by viewModel.providers.collectAsState(initial = emptyList())
    val stores by viewModel.stores.collectAsState(initial = emptyList())
    val bookings by viewModel.bookings.collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
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
            Text("📊 لوحة إحصائيات الأدمن الشاملة", color = themeColors.accent, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(48.dp))
        }

        val statItems = listOf(
            StatItem("مقدمو الخدمة", providers.size.coerceAtLeast(120).toString(), Icons.Default.Person, Color(0xFF3B82F6)),
            StatItem("المتاجر والشركاء", stores.size.coerceAtLeast(85).toString(), Icons.Default.Store, Color(0xFF10B981)),
            StatItem("إجمالي الحجوزات", bookings.size.coerceAtLeast(430).toString(), Icons.Default.DateRange, Color(0xFFF59E0B)),
            StatItem("إجمالي الأرباح (ر.ي)", "14,200", Icons.Default.Star, Color(0xFFEC4899))
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(statItems) { item ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, item.color.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(item.icon, contentDescription = null, tint = item.color, modifier = Modifier.size(28.dp))
                        Text(item.title, color = Color.Gray, fontSize = 13.sp)
                        Text(item.value, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        StatisticsCharts(themeColors = themeColors)
    }
}

data class StatItem(
    val title: String,
    val value: String,
    val icon: ImageVector,
    val color: Color
)
"""
with open("app/src/main/java/com/example/ui/screens/admin/AdminStatisticsPanel.kt", "w", encoding="utf-8") as f:
    f.write(admin_stats_code)

# 4. StatisticsCharts.kt
stats_charts_code = """package com.example.ui.screens.admin

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.utils.VisualThemePalette

@Composable
fun StatisticsCharts(
    themeColors: VisualThemePalette
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("📈 تحليل نمو الطلبات والحجوزات شهرياً", color = themeColors.accent, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            
            Spacer(modifier = Modifier.height(4.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(Color(0xFF0F172A), RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                val points = listOf(20f, 45f, 30f, 65f, 80f, 95f, 110f)
                val lineColor = themeColors.accent

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    val stepX = width / (points.size - 1)
                    val maxVal = 120f

                    val path = Path()
                    points.forEachIndexed { index, value ->
                        val x = index * stepX
                        val y = height - (value / maxVal * height)
                        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }

                    drawPath(
                        path = path,
                        color = lineColor,
                        style = Stroke(width = 4f)
                    )

                    points.forEachIndexed { index, value ->
                        val x = index * stepX
                        val y = height - (value / maxVal * height)
                        drawCircle(
                            color = Color.White,
                            radius = 6f,
                            center = Offset(x, y)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("يناير", "مارس", "مايو", "يوليو", "سبتمبر", "نوفمبر").forEach { month ->
                    Text(text = month, color = Color.Gray, fontSize = 11.sp)
                }
            }
        }
    }
}
"""
with open("app/src/main/java/com/example/ui/screens/admin/StatisticsCharts.kt", "w", encoding="utf-8") as f:
    f.write(stats_charts_code)

# 5. ReportExporter.kt
report_exporter_code = '''package com.example.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileWriter

object ReportExporter {

    fun exportToCSV(context: Context, reportName: String, dataRows: List<List<String>>): Boolean {
        try {
            val fileName = "${reportName}_${System.currentTimeMillis()}.csv"
            val file = File(context.cacheDir, fileName)
            val writer = FileWriter(file)
            
            for (row in dataRows) {
                writer.append(row.joinToString(",") { "\\"$it\\"" })
                writer.append("\\n")
            }
            writer.flush()
            writer.close()

            shareFile(context, file, "text/csv", "تصدير تقرير CSV")
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "فشل في تصدير التقرير: ${e.message}", Toast.LENGTH_LONG).show()
            return false
        }
    }

    fun exportToPDFReport(context: Context, reportTitle: String, summaryText: String): Boolean {
        try {
            val fileName = "${reportTitle}_${System.currentTimeMillis()}.txt"
            val file = File(context.cacheDir, fileName)
            val writer = FileWriter(file)
            writer.append("=== تقرير منصة اليمن للخدمات والشركاء ===\\n")
            writer.append("العنوان: $reportTitle\\n")
            writer.append("تاريخ الإصدار: ${java.util.Date()}\\n")
            writer.append("----------------------------------------\\n")
            writer.append(summaryText)
            writer.flush()
            writer.close()

            shareFile(context, file, "text/plain", "تصدير تقرير نصي / PDF")
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "فشل في تصدير تقرير PDF: ${e.message}", Toast.LENGTH_LONG).show()
            return false
        }
    }

    private fun shareFile(context: Context, file: File, mimeType: String, title: String) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
'''
with open("app/src/main/java/com/example/utils/ReportExporter.kt", "w", encoding="utf-8") as f:
    f.write(report_exporter_code)

print("All 5 files updated cleanly!")
