@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.screens.dashboard
import com.example.ui.MainViewModel

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*

import com.example.utils.VisualThemePalette

/**
 * 👑 UnifiedOwnerDashboardLayout - لوحة تحكم أصحاب الأنشطة التجارية والمتاجر والعقارات الموحدة
 * تدمج كافة لوحات التحكم تحت سقف معمارية واحدة مرنة وسريعة الاستجابة (<250 سطر)
 */
@Composable
fun UnifiedOwnerDashboardLayout(
    account: UnifiedBusinessAccount,
    viewModel: MainViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    themeColors: VisualThemePalette,
    onBackClick: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = remember(account.businessType) {
        when (account.businessType) {
            BusinessType.REAL_ESTATE -> listOf(
                "العقارات المعروضة" to Icons.Default.Home,
                "الطلبات والاستفسارات" to Icons.Default.Notifications,
                "إحصائيات المشاهدات" to Icons.Default.Star,
                "تعديل البيانات" to Icons.Default.Edit
            )
            else -> listOf(
                "المنتجات والخدمات" to Icons.Default.ShoppingCart,
                "الطلبات المباشرة" to Icons.Default.Notifications,
                "معرض الصور" to Icons.Default.Menu,
                "التقييمات والآراء" to Icons.Default.Star,
                "تعديل الملف التجاري" to Icons.Default.Edit
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "${account.businessType.icon} ${account.name}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "لوحة إدارة ${account.businessType.titleArabic}",
                            fontSize = 10.5.sp,
                            color = themeColors.accent
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        containerColor = Color(0xFF0F172A)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Dashboard Navigation Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF1E293B),
                contentColor = themeColors.accent,
                edgePadding = 8.dp
            ) {
                tabs.forEachIndexed { index, (title, icon) ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text(
                                    text = title,
                                    fontSize = 11.sp,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    )
                }
            }

            // Tab Content Dispatcher
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp)
            ) {
                when (selectedTab) {
                    0 -> TabProductsServices(account = account, viewModel = viewModel, themeColors = themeColors)
                    1 -> TabBookingsOrders(account = account, viewModel = viewModel, themeColors = themeColors)
                    2 -> if (account.businessType == BusinessType.REAL_ESTATE) {
                        TabStatisticsGrowth(account = account, viewModel = viewModel, themeColors = themeColors)
                    } else {
                        TabGalleryAlbums(account = account, viewModel = viewModel, themeColors = themeColors)
                    }
                    3 -> if (account.businessType == BusinessType.REAL_ESTATE) {
                        TabProfileEdit(account = account, viewModel = viewModel, themeColors = themeColors)
                    } else {
                        TabReviewsFeedback(account = account, viewModel = viewModel, themeColors = themeColors)
                    }
                    4 -> TabProfileEdit(account = account, viewModel = viewModel, themeColors = themeColors)
                }
            }
        }
    }
}
