package com.example.ui.screens.entities

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CategoryEntity
import com.example.ui.MainViewModel
import com.example.utils.VisualThemePalette

data class StaticCategoryItem(
    val id: String,
    val name: String,
    val icon: String,
    val countLabel: String
)

@Composable
fun CategoriesScreen(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onCategoryClick: (String) -> Unit
) {
    val categoriesFromVm by viewModel.categories.collectAsState()

    val defaultCategories = listOf(
        StaticCategoryItem("stores", "المحلات والأنشطة التجارية", "🏪", "أكثر من 500 محل"),
        StaticCategoryItem("medical", "المراكز الطبية والعيادات", "🏥", "مستشفيات وصيدليات"),
        StaticCategoryItem("restaurants", "المطاعم والكافيهات", "🍔", "أشهر الوجبات والمقاهي"),
        StaticCategoryItem("properties", "العقارات والأراضي", "🏠", "بيوت، شقق، أراضي"),
        StaticCategoryItem("plumbing", "سباكة وكهرباء", "🔧", "فنيون متعددو المهارات"),
        StaticCategoryItem("car_maintenance", "صيانة السيارات", "🚗", "ورش وميكانيك"),
        StaticCategoryItem("appliances", "صيانة الأجهزة الإلكترونية", "📱", "موبايل، تكييف، غسالات"),
        StaticCategoryItem("cleaning", "خدمات التنظيف والتعقيم", "🧹", "منازل ومكاتب")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(themeColors.background)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🗂️ فئات دليل الخدمات اليمني المتاحة:",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = themeColors.accent
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(defaultCategories, key = { it.id }) { cat ->
                StaticCategoryCard(
                    cat = cat,
                    themeColors = themeColors,
                    onClick = { onCategoryClick(cat.id) }
                )
            }

            if (categoriesFromVm.isNotEmpty()) {
                items(categoriesFromVm, key = { it.id }) { cat ->
                    DynamicCategoryCard(
                        category = cat,
                        themeColors = themeColors,
                        onClick = { onCategoryClick(cat.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun StaticCategoryCard(
    cat: StaticCategoryItem,
    themeColors: VisualThemePalette,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                color = themeColors.accent.copy(alpha = 0.15f),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    text = cat.icon,
                    fontSize = 28.sp,
                    modifier = Modifier.padding(10.dp)
                )
            }

            Text(
                text = cat.name,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = cat.countLabel,
                fontSize = 10.sp,
                color = Color.LightGray,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun DynamicCategoryCard(
    category: CategoryEntity,
    themeColors: VisualThemePalette,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
        border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = category.icon.ifBlank { "✨" },
                fontSize = 28.sp
            )

            Text(
                text = category.name,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
