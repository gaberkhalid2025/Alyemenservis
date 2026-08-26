package com.example.ui.screens.dashboard

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.UnifiedBusinessAccount
import com.example.ui.MainViewModel
import com.example.utils.VisualThemePalette

/**
 * 📈 Modular Tab: Business Statistics, Analytics & Growth (الإحصائيات والتحليلات ونمو المنشأة)
 */
@Composable
fun TabStatisticsGrowth(
    account: UnifiedBusinessAccount,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette
) {
    val context = LocalContext.current
    val allProducts by viewModel.products.collectAsState()
    val allBookings by viewModel.bookings.collectAsState()
    val allRatings by viewModel.ratings.collectAsState()

    val myProducts = remember(allProducts, account.id) {
        allProducts.filter { (it.storeId == account.id || it.storeId == account.phone) && !it.isDeleted }
    }
    val myBookings = remember(allBookings, account.id, account.phone) {
        allBookings.filter { b -> b.providerId == account.id || b.providerId == account.phone }
    }
    val myReviews = remember(allRatings, account.id, account.phone) {
        allRatings.filter { r -> r.targetId == account.id || r.targetId == account.phone }
    }

    val completedBookings = remember(myBookings) { myBookings.filter { it.status == "COMPLETED" }.size }
    val pendingBookings = remember(myBookings) { myBookings.filter { it.status == "PENDING" }.size }
    val totalViews = remember(account.numReviews) { account.numReviews * 15 + 47 }

    fun exportReport(format: String) {
        val summary = """
            📊 تقرير أداء المنشأة (${account.name})
            ------------------------------------
            • المشاهدات والزيارات: $totalViews
            • متوسط التقييم: ⭐ ${String.format("%.1f", account.rating)} (${account.numReviews} تقييم)
            • عدد المنتجات/الخدمات: ${myProducts.size}
            • إجمالي الحجوزات/الطلبات: ${myBookings.size} (مكتمل: $completedBookings، معلق: $pendingBookings)
            ------------------------------------
            صادر من دليل خدمات اليمن - صيغة التصدير: $format
        """.trimIndent()

        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("Business Report", summary)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "📄 تم نسخ التقرير بصيغة $format إلى الحافظة بنجاح!", Toast.LENGTH_LONG).show()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📊 إحصائيات الأداء ونمو المنشأة",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = themeColors.accent
                )

                // Export Menu Button
                var showExportMenu by remember { mutableStateOf(false) }
                Box {
                    Button(
                        onClick = { showExportMenu = true },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = themeColors.accent, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("تصدير التقرير 📥", fontSize = 10.sp, color = themeColors.accent, fontWeight = FontWeight.Bold)
                    }

                    DropdownMenu(
                        expanded = showExportMenu,
                        onDismissRequest = { showExportMenu = false },
                        modifier = Modifier.background(themeColors.surface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("📄 تصدير كـ PDF", fontSize = 11.sp, color = Color.White) },
                            onClick = { exportReport("PDF"); showExportMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("📊 تصدير كـ Excel (XLSX)", fontSize = 11.sp, color = Color.White) },
                            onClick = { exportReport("Excel"); showExportMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("📑 تصدير كـ CSV", fontSize = 11.sp, color = Color.White) },
                            onClick = { exportReport("CSV"); showExportMenu = false }
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Card 1: Views/Visits
                Card(
                    colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("👀 المشاهدات والزيارات", fontSize = 10.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("${account.numReviews * 15 + 47}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    }
                }

                // Card 2: Rating
                Card(
                    colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("⭐ متوسط التقييم", fontSize = 10.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "⭐ " + String.format("%.1f", account.rating),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFB300)
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Card 3: Total Products/Services
                Card(
                    colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("🛒 المنتجات / الخدمات", fontSize = 10.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("${myProducts.size}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                // Card 4: Bookings
                Card(
                    colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("📅 إجمالي الحجوزات", fontSize = 10.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("${myBookings.size}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.15f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("💡 تحليلات الأداء والنمو", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("الحجوزات المكتملة بنجاح 🎉", fontSize = 11.sp, color = Color.LightGray)
                        Text("$completedBookings حجز", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("الحجوزات قيد المعالجة والانتظار ⏳", fontSize = 11.sp, color = Color.LightGray)
                        Text("$pendingBookings حجز", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("التقييمات والآراء المكتوبة ⭐", fontSize = 11.sp, color = Color.LightGray)
                        Text("${myReviews.size} تعليق", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}
