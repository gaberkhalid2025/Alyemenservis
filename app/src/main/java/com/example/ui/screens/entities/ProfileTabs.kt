package com.example.ui.screens.entities

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.utils.VisualThemePalette

@Composable
fun ProfileTabs(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    entityType: ProfileEntityType,
    themeColors: VisualThemePalette
) {
    val tabTitles = when (entityType) {
        ProfileEntityType.TECHNICIAN -> listOf("سابقة الأعمال 📸", "التقييمات والآراء ⭐", "شروط الضمان 🛡️")
        ProfileEntityType.STORE -> listOf("الكتالوج والبضائع 📦", "العروض والخصومات 🏷️", "تقييمات المتجر ⭐")
        ProfileEntityType.RESTAURANT -> listOf("منيو الأكلات 🍔", "العروض الخاصة 🎁", "آراء الذواقة ⭐")
        ProfileEntityType.MEDICAL -> listOf("العيادات والتخصصات 🩺", "أوقات الدوام والطوارئ ⏰", "آراء المراجعين ⭐")
        ProfileEntityType.REAL_ESTATE -> listOf("صور العقار والمخطط 📐", "المواصفات والخدمات 🏢", "معاينة وخريطة 🗺️")
        ProfileEntityType.JOB -> listOf("شروط ومؤهلات الوظيفة 📋", "المزايا والحوافز 💰", "عن جهة العمل 🏢")
        ProfileEntityType.GENERAL -> listOf("الخدمات 🛠️", "التقييمات ⭐", "معلومات التواصل 📞")
    }

    TabRow(
        selectedTabIndex = selectedTab,
        containerColor = themeColors.surface,
        contentColor = themeColors.accent,
        modifier = Modifier.clip(RoundedCornerShape(12.dp))
    ) {
        tabTitles.forEachIndexed { index, title ->
            Tab(
                selected = selectedTab == index,
                onClick = { onTabSelected(index) },
                text = {
                    Text(
                        title,
                        fontSize = 11.sp,
                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedTab == index) themeColors.accent else Color.LightGray
                    )
                }
            )
        }
    }
}
