package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class AdminCategoryFilterItem(
    val id: String,
    val title: String,
    val icon: String,
    val colorHex: Long
)

val defaultAdminCategoriesList = listOf(
    AdminCategoryFilterItem("ALL", "جميع الأقسام", "🌐", 0xFF0284C7),
    AdminCategoryFilterItem("TECHNICIAN", "الفنيين والمهن", "🔧", 0xFF10B981),
    AdminCategoryFilterItem("STORES", "المراكز التجارية", "🏢", 0xFF8B5CF6),
    AdminCategoryFilterItem("REAL_ESTATE", "العقارات والأراضي", "🏠", 0xFFF59E0B),
    AdminCategoryFilterItem("MEDICAL", "المراكز الطبية", "🏥", 0xFFEF4444),
    AdminCategoryFilterItem("RESTAURANTS", "المطاعم والكافيهات", "🍔", 0xFFEC4899),
    AdminCategoryFilterItem("JOBS", "إعلانات الوظائف", "💼", 0xFF06B6D4)
)

/**
 * 🌐 GlobalCategoryFilterBar:
 * Mandatory fixed horizontal category filter bar for ALL admin control panel screens.
 * Allows filtering bookings, reviews, complaints, calls, chats, and technician logs by category.
 */
@Composable
fun GlobalCategoryFilterBar(
    selectedCategoryId: String,
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    categories: List<AdminCategoryFilterItem> = defaultAdminCategoriesList
) {
    val scrollState = rememberScrollState()

    Surface(
        color = Color(0xFF0F172A),
        tonalElevation = 4.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            categories.forEach { category ->
                val isSelected = selectedCategoryId == category.id
                val badgeBg = if (isSelected) Color(category.colorHex) else Color(0xFF1E293B)
                val textCol = if (isSelected) Color.White else Color(0xFF94A3B8)
                val borderCol = if (isSelected) Color.White else Color(0xFF334155)

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(badgeBg)
                        .border(1.dp, borderCol, RoundedCornerShape(12.dp))
                        .clickable { onCategorySelected(category.id) }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = category.icon,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = category.title,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = textCol
                        )
                    }
                }
            }
        }
    }
}
