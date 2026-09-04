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
 * 🏬 Standalone Dedicated Dashboard for Stores & Commercial Centers (لوحة المتجر والمركز التجاري)
 */
@Composable
fun StoreDashboard(
    account: UnifiedBusinessAccount,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var activeTab by remember { mutableIntStateOf(0) }

    val storeViewModel = remember(account.id) {
        StoreDashboardViewModel(
            storeId = account.id,
            dashboardRepository = DashboardRepositoryImpl(context),
            productsRepository = ProductsRepositoryImpl(context),
            ratingsRepository = RatingsRepositoryImpl(context)
        )
    }

    val storeUiState by storeViewModel.uiState.collectAsState()

    LaunchedEffect(storeViewModel) {
        storeViewModel.eventFlow.collect { event ->
            when (event) {
                is DashboardEvent.ShowToast -> Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                is DashboardEvent.NavigateToDetail -> { }
            }
        }
    }

    val tabsList = listOf(
        Pair("📦", "المنتجات والمخزون"),
        Pair("🛍️", "طلبات الشراء"),
        Pair("🏷️", "العروض والكوبونات"),
        Pair("💬", "تقييمات المتجر"),
        Pair("📝", "الملف التجاري للمتجر"),
        Pair("📊", "الإحصائيات والأداء")
    )

    val stores by viewModel.stores.collectAsState()
    val matchingStore = stores.find { it.id == account.id || it.phone == account.phone }
    val isVerified = account.isVerified || (matchingStore?.isActive == true)
    var isServiceActive by remember { mutableStateOf(account.isActive) }

    val allBookings by viewModel.bookings.collectAsState()
    val storeOrders = remember(allBookings, account.id) {
        allBookings.filter { it.providerId == account.id || it.providerPhone == account.phone }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        // Professional Top Header
        ProfessionalDashboardHeader(
            account = account,
            subtitle = "🏬 متجر تجاري معتمد • ${account.neighborhood.ifBlank { account.city.ifBlank { "اليمن" } }}",
            isVerified = isVerified,
            isServiceActive = isServiceActive,
            onToggleServiceActive = { active ->
                isServiceActive = active
                viewModel.updateBusinessAccountStatus(account.id, active)
            },
            onEditProfileClick = { activeTab = 4 },
            onShareClick = {
                val sendIntent = android.content.Intent().apply {
                    action = android.content.Intent.ACTION_SEND
                    putExtra(android.content.Intent.EXTRA_TEXT, "تسوق من متجر ${account.name} على منصة الخدمات: ${account.phone}")
                    type = "text/plain"
                }
                context.startActivity(android.content.Intent.createChooser(sendIntent, "مشاركة المتجر"))
            },
            onBackClick = onBackClick,
            themeColors = themeColors
        )

        // Quick Performance Stats
        ProfessionalQuickStatsGrid(
            todayOrdersCount = storeOrders.size.coerceAtLeast(1),
            overallRating = account.rating,
            activeOffersCount = 3,
            approxRevenue = "68,000 ر.ي",
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
                1 -> TabBookingsOrders(account, viewModel, themeColors)
                2 -> TabOffersCoupons(account, viewModel, themeColors)
                3 -> TabReviewsFeedback(account, viewModel, themeColors)
                4 -> TabProfileEdit(account, viewModel, themeColors)
                5 -> TabStatisticsGrowth(account, viewModel, themeColors)
            }
        }
    }
}
