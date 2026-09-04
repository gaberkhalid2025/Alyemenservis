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
 * 🏢 Standalone Dedicated Dashboard for Real Estate Offices & Agencies (لوحة العقارات المستقلة)
 */
@Composable
fun PropertyDashboard(
    account: UnifiedBusinessAccount,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var activeTab by remember { mutableIntStateOf(0) }

    val propertyViewModel = remember(account.id) {
        PropertyDashboardViewModel(
            ownerId = account.id,
            dashboardRepository = DashboardRepositoryImpl(context)
        )
    }

    val propertyUiState by propertyViewModel.uiState.collectAsState()

    val tabsList = listOf(
        Pair("🏠", "العقارات والوحدات"),
        Pair("📅", "طلبات المعاينة"),
        Pair("💬", "تقييمات العملاء"),
        Pair("📝", "ملف المكتب العقاري"),
        Pair("📊", "الإحصائيات والنمو")
    )

    val stores by viewModel.stores.collectAsState()
    val matchingStore = stores.find { it.id == account.id || it.phone == account.phone }
    val isVerified = account.isVerified || (matchingStore?.isActive == true)
    var isServiceActive by remember { mutableStateOf(account.isActive) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        // Professional Top Header
        ProfessionalDashboardHeader(
            account = account,
            subtitle = "🏢 مكتب عقارات واستثمار • ${account.neighborhood.ifBlank { account.city.ifBlank { "اليمن" } }}",
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
                    putExtra(android.content.Intent.EXTRA_TEXT, "عقارات وفرص استثمارية من ${account.name}: ${account.phone}")
                    type = "text/plain"
                }
                context.startActivity(android.content.Intent.createChooser(sendIntent, "مشاركة المكتب العقاري"))
            },
            onBackClick = onBackClick,
            themeColors = themeColors
        )

        // Quick Stats Strip
        ProfessionalQuickStatsGrid(
            todayOrdersCount = 5,
            overallRating = account.rating,
            activeOffersCount = 6,
            approxRevenue = "فرص نشطة",
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
                2 -> TabReviewsFeedback(account, viewModel, themeColors)
                3 -> TabProfileEdit(account, viewModel, themeColors)
                4 -> TabStatisticsGrowth(account, viewModel, themeColors)
            }
        }
    }
}
