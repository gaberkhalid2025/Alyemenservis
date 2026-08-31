package com.example.ui.screens.dashboard
import com.example.ui.MainViewModel

import android.widget.Toast
import androidx.compose.foundation.*
import com.example.viewmodels.StoreViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
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

import com.example.utils.VisualThemePalette

import com.example.data.repositories.*

/**
 * 🏬 Standalone Dedicated Dashboard for Stores & Commercial Centers (لوحة المتجر والمركز التجاري)
 */
@Composable
fun StoreDashboard(
    account: UnifiedBusinessAccount,
    viewModel: MainViewModel = viewModel(),
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
        Pair("💬", "تقييمات المتجر"),
        Pair("📝", "الملف التجاري للمتجر"),
        Pair("📊", "الإحصائيات والأداء")
    )

    val stores by viewModel.stores.collectAsState()
    val matchingStore = stores.find { it.id == account.id || it.phone == account.phone }
    val isVerified = account.isVerified || (matchingStore?.isActive == true)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        // Top Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E293B))
                .border(1.dp, Color.White.copy(alpha = 0.08f))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .size(36.dp)
                    .background(Color.White.copy(alpha = 0.08f), CircleShape)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "رجوع", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = account.name.ifBlank { "لوحة تحكم المتجر" },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "🏬 متجر تجاري • ${account.neighborhood.ifBlank { account.city }}",
                    fontSize = 11.sp,
                    color = themeColors.accent
                )
            }

            Surface(
                color = if (isVerified) Color(0xFF10B981) else Color(0xFFF59E0B),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (isVerified) "موثق ✓" else "قيد التوثيق ⏳",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    fontSize = 10.sp,
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Horizontal Tabs Bar
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F172A))
                .padding(vertical = 10.dp, horizontal = 8.dp),
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
