package com.example.ui.screens.dashboard

import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.MainViewModel
import com.example.utils.VisualThemePalette

import com.example.data.repositories.*

/**
 * 🍔 Standalone Dedicated Dashboard for Restaurants & Cafes (لوحة المطعم والكافيه)
 */
@Composable
fun RestaurantDashboard(
    account: UnifiedBusinessAccount,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var activeTab by remember { mutableIntStateOf(0) }

    val restaurantViewModel = remember(account.id) {
        RestaurantDashboardViewModel(
            restaurantId = account.id,
            dashboardRepository = DashboardRepositoryImpl(context),
            productsRepository = ProductsRepositoryImpl(context)
        )
    }

    val restaurantUiState by restaurantViewModel.uiState.collectAsState()

    val stores by viewModel.stores.collectAsState()
    val matchingStore = stores.find { it.id == account.id || it.phone == account.phone }
    val isVerified = account.isVerified || (matchingStore?.isActive == true)
    var isServiceActive by remember { mutableStateOf(account.isActive) }
    var isDeliveryActive by remember { mutableStateOf(true) }
    var isPeakHoursMode by remember { mutableStateOf(true) }

    val allBookings by viewModel.bookings.collectAsState()
    val restaurantBookings = remember(allBookings, account.id) {
        allBookings.filter { it.providerId == account.id || it.providerPhone == account.phone }
    }

    val tabsList = listOf(
        Pair("🍔", "قائمة الطعام والمنيو"),
        Pair("🍳", "طلبات الوجبات والتجهيز"),
        Pair("📅", "الحجوزات والمحادثات"),
        Pair("🏷️", "العروض اليومية والذروة"),
        Pair("💬", "التقييمات والردود"),
        Pair("📝", "الملف التعريفي للمطعم"),
        Pair("📊", "الإحصائيات والأداء")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        // Professional Top Header
        ProfessionalDashboardHeader(
            account = account,
            subtitle = "🍔 مطعم وكافيه معتمد • ${account.neighborhood.ifBlank { account.city.ifBlank { "اليمن" } }}",
            isVerified = isVerified,
            isServiceActive = isServiceActive,
            onToggleServiceActive = { active ->
                isServiceActive = active
                viewModel.updateBusinessAccountStatus(account.id, active)
            },
            onEditProfileClick = { activeTab = 5 },
            onShareClick = {
                val sendIntent = android.content.Intent().apply {
                    action = android.content.Intent.ACTION_SEND
                    putExtra(android.content.Intent.EXTRA_TEXT, "اطلب وجبتك اللذيذة من ${account.name}: ${account.phone}")
                    type = "text/plain"
                }
                context.startActivity(android.content.Intent.createChooser(sendIntent, "مشاركة المطعم"))
            },
            onBackClick = onBackClick,
            themeColors = themeColors
        )

        // Restaurant Fast Controls: Delivery + Peak Hours
        Surface(
            color = Color(0xFF1E293B),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Delivery Toggle
                Surface(
                    color = if (isDeliveryActive) Color(0xFF10B981).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (isDeliveryActive) Color(0xFF10B981) else Color.Gray),
                    modifier = Modifier.clickable {
                        isDeliveryActive = !isDeliveryActive
                        Toast.makeText(context, if (isDeliveryActive) "🛵 تم تفعيل خدمة التوصيل" else "🛑 تم إيقاف خدمة التوصيل", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text(
                        text = if (isDeliveryActive) "🛵 التوصيل: متاح للزبائن" else "🛵 التوصيل: مغلق",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDeliveryActive) Color(0xFF10B981) else Color.Gray,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                // Peak Hours Toggle
                Surface(
                    color = if (isPeakHoursMode) Color(0xFFF59E0B).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (isPeakHoursMode) Color(0xFFF59E0B) else Color.Gray),
                    modifier = Modifier.clickable {
                        isPeakHoursMode = !isPeakHoursMode
                        Toast.makeText(context, if (isPeakHoursMode) "⚡ تفعيل نمط الذروة (خصومات سريعة)" else "نمط العمل الطبيعي", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text(
                        text = if (isPeakHoursMode) "⚡ أوقات الذروة نشطة" else "أوقات العمل العادية",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isPeakHoursMode) Color(0xFFF59E0B) else Color.Gray,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Quick Stats Strip
        ProfessionalQuickStatsGrid(
            todayOrdersCount = restaurantBookings.size.coerceAtLeast(4),
            overallRating = account.rating,
            activeOffersCount = 2,
            approxRevenue = "42,000 ر.ي",
            themeColors = themeColors,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )

        // Horizontal Tabs Bar
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F172A))
                .padding(vertical = 6.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tabsList.size) { index ->
                val tab = tabsList[index]
                val isSelected = activeTab == index
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) themeColors.accent else Color(0xFF1E293B))
                        .border(
                            1.dp,
                            if (isSelected) themeColors.accent else Color.White.copy(alpha = 0.08f),
                            RoundedCornerShape(16.dp)
                        )
                        .clickable { activeTab = index }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(tab.first, fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = tab.second,
                            fontSize = 11.5.sp,
                            color = if (isSelected) Color.Black else Color.White,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)

        // Dynamic Tab Content
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(12.dp)
        ) {
            when (activeTab) {
                0 -> TabProductsServices(account, viewModel, themeColors)
                1 -> RestaurantOrdersPreparationSection(account, viewModel, themeColors)
                2 -> RestaurantBookingsAndChatsSection(account, viewModel, themeColors)
                3 -> TabOffersCoupons(account, viewModel, themeColors)
                4 -> TabReviewsFeedback(account, viewModel, themeColors)
                5 -> TabProfileEdit(account, viewModel, themeColors)
                6 -> TabStatisticsGrowth(account, viewModel, themeColors)
            }
        }
    }
}

/**
 * 🍳 قسم طلبات الوجبات وحالة التجهيز والتوصيل
 */
@Composable
private fun RestaurantOrdersPreparationSection(
    account: UnifiedBusinessAccount,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette
) {
    val context = LocalContext.current
    val allBookings by viewModel.bookings.collectAsState()
    val orders = remember(allBookings, account.id) {
        allBookings.filter { it.providerId == account.id || it.providerPhone == account.phone }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("🍳 طلبات الوجبات الواردة وحالة التجهيز (${orders.size}):", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)

        if (orders.isEmpty()) {
            UnifiedEmptyState(
                icon = "🍔",
                title = "لا توجد طلبات وجبات حالياً",
                description = "ستظهر هنا كافة طلبات الوجبات وساندوتشات الزبائن مع خيارات تغيير حالة التحضير والتسليم مباشرة.",
                themeColors = themeColors
            )
        } else {
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(orders) { order ->
                    var prepStatus by remember(order.id) { mutableStateOf("PREPARING") }
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("طلب وجبة #${order.id.takeLast(5)}", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Surface(
                                    color = when (prepStatus) {
                                        "READY" -> Color(0xFF10B981).copy(alpha = 0.2f)
                                        "DELIVERED" -> Color(0xFF3B82F6).copy(alpha = 0.2f)
                                        else -> Color(0xFFF59E0B).copy(alpha = 0.2f)
                                    },
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = when (prepStatus) {
                                            "READY" -> "✅ جاهز للتسليم"
                                            "DELIVERED" -> "🛵 تم التوصيل"
                                            else -> "🍳 قيد التحضير والتجهيز"
                                        },
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when (prepStatus) {
                                            "READY" -> Color(0xFF10B981)
                                            "DELIVERED" -> Color(0xFF3B82F6)
                                            else -> Color(0xFFF59E0B)
                                        },
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Text("👤 العميل: ${order.clientName.ifBlank { "عميل معتمد" }} • 📱 ${order.clientPhone}", fontSize = 10.5.sp, color = Color.LightGray)
                            Text("🍔 الأصناف: ${order.serviceName.ifBlank { "وجبات مشكلة" }}", fontSize = 11.sp, color = themeColors.accent)
                            Text("📍 العنوان: ${order.clientAddress.ifBlank { "استلام محلي / توصيل" }}", fontSize = 10.sp, color = Color.Gray)

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(
                                    onClick = {
                                        prepStatus = "READY"
                                        Toast.makeText(context, "✅ تم تحديث الطلب: جاهز للتسليم", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("جاهز 🍳", fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = {
                                        prepStatus = "DELIVERED"
                                        Toast.makeText(context, "🛵 تم تحديث الطلب: تم التسليم بنجاح", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("تم التوصيل 🛵", fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 📅 قسم الحجوزات والمحادثات المباشرة مع زبائن المطعم
 */
@Composable
private fun RestaurantBookingsAndChatsSection(
    account: UnifiedBusinessAccount,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette
) {
    val allBookings by viewModel.bookings.collectAsState()
    val bookings = remember(allBookings, account.id) {
        allBookings.filter { it.providerId == account.id || it.providerPhone == account.phone }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("📅 حجوزات الطاولات والتواصل (${bookings.size}):", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)

        if (bookings.isEmpty()) {
            UnifiedEmptyState(
                icon = "📅",
                title = "لا توجد حجوزات طاولات حالياً",
                description = "حجوزات الطاولات والمناسبات الواردة من الزبائن ستظهر هنا مع إمكانية فتح محادثة فورية للتنسيق.",
                themeColors = themeColors
            )
        } else {
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(bookings) { b ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("حجز طاولة #${b.id.takeLast(4)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("📅 ${b.date.ifBlank { b.dateString }}", fontSize = 10.sp, color = themeColors.accent)
                            }
                            Text("👤 العميل: ${b.clientName.ifBlank { b.customerName }} (${b.clientPhone.ifBlank { b.customerPhone }})", fontSize = 10.5.sp, color = Color.LightGray)
                            Text("🕒 الوقت: ${b.time.ifBlank { b.timeString }} • الأفراد: ${b.status}", fontSize = 10.sp, color = Color.Gray)

                            Button(
                                onClick = {
                                    viewModel.openOrCreateChatChannel(
                                        targetId = b.clientId.ifBlank { b.customerPhone },
                                        targetType = "CUSTOMER",
                                        targetName = b.clientName.ifBlank { b.customerName },
                                        targetPhone = b.customerPhone
                                    ) {
                                        viewModel.navigateToScreen(com.example.ui.navigation.AppScreens.CHAT_DIRECT)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("فتح محادثة فورية مع العميل 💬", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                            }

                        }
                    }
                }
            }
        }
    }
}

