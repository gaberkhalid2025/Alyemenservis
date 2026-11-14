package com.example.ui.screens.dashboard.components

import androidx.compose.animation.core.animateFloatAsState
import com.example.ui.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import com.example.utils.ReportExporter
import com.example.utils.VisualThemePalette
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * 📊 AnalyticsCharts (لوحة تحليلات وإحصاءات الأداء والنمو)
 * رسوم بيانية ومؤشرات تفاعلية للأرباح، دعم تعدد العملات (YER, SAR, USD)، وتصدير التقارير المالية CSV.
 */
@Composable
fun AnalyticsCharts(
    themeColors: VisualThemePalette,
    businessName: String = "حساب الأعمال",
    stats: com.example.domain.entities.DashboardStatsEntity? = null,
    totalRevenueYer: Double = 0.0,
    bookings: List<com.example.data.BookingEntity> = emptyList(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var timePeriod by remember { mutableStateOf("أسبوعي") } // أسبوعي / شهري
    var selectedCurrency by remember { mutableStateOf("YER") } // YER, SAR, USD

    val baseIncomeYer = if (totalRevenueYer > 0.0) totalRevenueYer else (stats?.totalRevenueYer ?: 0.0)
    val numberFormat = remember { DecimalFormat("#,###.##", DecimalFormatSymbols(Locale.US)) }

    val (convertedIncome, currencySymbol) = remember(selectedCurrency, baseIncomeYer) {
        when (selectedCurrency) {
            "SAR" -> (baseIncomeYer / 420.0) to "ر.س"
            "USD" -> (baseIncomeYer / 1600.0) to "$"
            else -> baseIncomeYer to "ر.ي"
        }
    }

    val totalBookingsCount = remember(bookings, stats) {
        if (bookings.isNotEmpty()) bookings.size
        else ((stats?.activeBookingsCount ?: 0) + (stats?.completedBookingsCount ?: 0))
    }
    val completedCount = remember(bookings, stats) {
        if (bookings.isNotEmpty()) bookings.count { it.status.equals("COMPLETED", ignoreCase = true) }
        else (stats?.completedBookingsCount ?: 0)
    }
    val completionRate = if (totalBookingsCount > 0) (completedCount * 100.0 / totalBookingsCount) else 0.0
    val reviewCount = stats?.totalReviewsCount ?: 0
    val averageRating = if (reviewCount > 0) (stats?.averageRating ?: 0.0) else 0.0

    val activityData = remember(bookings, stats, timePeriod) {
        val days = listOf(
            java.util.Calendar.SATURDAY to "السبت",
            java.util.Calendar.SUNDAY to "الأحد",
            java.util.Calendar.MONDAY to "الإثنين",
            java.util.Calendar.TUESDAY to "الثلاثاء",
            java.util.Calendar.WEDNESDAY to "الأربعاء",
            java.util.Calendar.THURSDAY to "الخميس",
            java.util.Calendar.FRIDAY to "الجمعة"
        )
        val dayMs = 24L * 60L * 60L * 1000L
        val cutoff = System.currentTimeMillis() - (if (timePeriod == "أسبوعي") 7L else 30L) * dayMs
        val relevant = bookings.filter { it.createdAt == 0L || it.createdAt >= cutoff }
        val countsByDay = days.map { (calDay, label) ->
            val count = relevant.count { b ->
                val ts = if (b.createdAt > 0L) b.createdAt else b.scheduledAt
                if (ts <= 0L) false else {
                    val cal = java.util.Calendar.getInstance().apply { timeInMillis = ts }
                    cal.get(java.util.Calendar.DAY_OF_WEEK) == calDay
                }
            }
            label to count
        }
        val maxCount = countsByDay.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1
        val hasTimestamps = countsByDay.any { it.second > 0 }
        if (hasTimestamps) {
            countsByDay.map { (label, count) -> label to (count.toFloat() / maxCount.toFloat()) }
        } else {
            val fallbackTotal = totalBookingsCount
            val baseRatio = if (fallbackTotal > 0) (fallbackTotal.coerceAtMost(10) / 10f) else 0.05f
            days.map { (_, label) -> label to baseRatio }
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header & Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFF10B981))
                    Text(
                        text = "التحليلات ومؤشرات الأداء",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeColors.textPrimary
                    )
                }

                IconButton(
                    onClick = {
                        val exportItems = listOf(
                            "إجمالي الإيرادات المسجلة" to convertedIncome,
                            "إجمالي الطلبات ($totalBookingsCount)" to totalBookingsCount.toDouble(),
                            "الطلبات المكتملة ($completedCount)" to completedCount.toDouble()
                        )
                        val result = ReportExporter.exportFinancialReportToCsv(
                            context = context,
                            accountName = businessName,
                            totalIncome = convertedIncome,
                            currency = currencySymbol,
                            items = exportItems
                        )
                        result.onSuccess { file ->
                            ReportExporter.shareExportedFile(context, file, "تقرير الأداء المالي - $businessName")
                        }
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = "تصدير CSV", tint = Color(0xFF00E5FF))
                }
            }

            // Currency & Time Period Switchers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Currency Switcher
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("YER" to "ريال يمني", "SAR" to "سعودي", "USD" to "دولار").forEach { (code, label) ->
                        FilterChip(
                            selected = selectedCurrency == code,
                            onClick = { selectedCurrency = code },
                            label = { Text(label, fontSize = 10.sp) },
                            modifier = Modifier.height(40.dp)
                        )
                    }
                }

                // Time Period Filter
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("أسبوعي", "شهري").forEach { period ->
                        FilterChip(
                            selected = timePeriod == period,
                            onClick = { timePeriod = period },
                            label = { Text(period, fontSize = 10.sp) },
                            modifier = Modifier.height(40.dp)
                        )
                    }
                }
            }

            // Key KPI Metric Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = themeColors.surface.copy(alpha = 0.85f),
                    tonalElevation = 2.dp,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("إجمالي الإيرادات", fontSize = 11.sp, color = themeColors.textSecondary)
                        Text("${numberFormat.format(convertedIncome)} $currencySymbol", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                        Text("$completedCount طلب مكتمل", fontSize = 10.sp, color = Color(0xFF10B981))
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = themeColors.surface.copy(alpha = 0.85f),
                    tonalElevation = 2.dp,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("الطلبات والحجوزات", fontSize = 11.sp, color = themeColors.textSecondary)
                        Text("$totalBookingsCount طلب", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00E5FF))
                        Text("${String.format(Locale.US, "%.1f", completionRate)}% اكتمال", fontSize = 10.sp, color = Color(0xFF00E5FF))
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = themeColors.surface.copy(alpha = 0.85f),
                    tonalElevation = 2.dp,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("تقييم العملاء", fontSize = 11.sp, color = themeColors.textSecondary)
                        Text("★ ${String.format(Locale.US, "%.1f", averageRating)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B))
                        Text("$reviewCount مراجعة", fontSize = 10.sp, color = Color(0xFFF59E0B))
                    }
                }
            }

            // Interactive Bar Chart
            Text(
                text = "حجم النشاط والطلبات (${timePeriod}):",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = themeColors.textPrimary
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(themeColors.background, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                activityData.forEach { (day, ratio) ->
                    val animatedHeight by animateFloatAsState(
                        targetValue = ratio.coerceIn(0.05f, 1.0f),
                        animationSpec = tween(durationMillis = 800),
                        label = "bar_anim"
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp),
                            color = if (ratio >= 0.85f) Color(0xFF00E5FF) else Color(0xFF38BDF8),
                            modifier = Modifier
                                .width(18.dp)
                                .height((80 * animatedHeight).dp)
                        ) {}
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = day,
                            fontSize = 9.sp,
                            color = themeColors.textSecondary
                        )
                    }
                }
            }
        }
    }
}

