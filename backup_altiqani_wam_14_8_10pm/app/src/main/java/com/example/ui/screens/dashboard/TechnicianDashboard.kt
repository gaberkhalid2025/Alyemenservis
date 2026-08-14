package com.example.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.MainViewModel
import com.example.utils.VisualThemePalette

@Composable
fun TechnicianDashboard(
    account: UnifiedBusinessAccount,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onBackClick: () -> Unit
) {
    var activeTab by remember { mutableStateOf(0) } // 0: Overview, 1: Services, 2: Ratings, 3: Settings
    val tabsList = listOf(
        Pair("📊", "نظرة عامة فنية"),
        Pair("🛠️", "الخدمات والمهن"),
        Pair("⭐", "تقييمات الفني"),
        Pair("⚙️", "الإعدادات")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(themeColors.background)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(themeColors.secondary)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = account.name.ifBlank { "لوحة تحكم الفني المحترف" },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "🛠️ فني معتمد • ID: ${account.id.take(12)}...",
                    fontSize = 10.sp,
                    color = Color.LightGray
                )
            }
        }

        // Tabs
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(themeColors.surface)
                .padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(tabsList.size) { index ->
                val tab = tabsList[index]
                val isSelected = activeTab == index
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) themeColors.accent else Color.DarkGray.copy(alpha = 0.5f))
                        .clickable { activeTab = index }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${tab.first} ${tab.second}",
                        fontSize = 11.sp,
                        color = if (isSelected) Color.Black else Color.White,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        // Content
        Box(modifier = Modifier.weight(1f).padding(12.dp)) {
            when (activeTab) {
                0 -> UnifiedProfileSection(account, viewModel, themeColors)
                1 -> UnifiedProductsServicesSection(account, viewModel, themeColors)
                2 -> UnifiedRatingsSection(account, viewModel, themeColors)
                3 -> UnifiedSettingsSection(account, viewModel, themeColors)
            }
        }
    }
}
