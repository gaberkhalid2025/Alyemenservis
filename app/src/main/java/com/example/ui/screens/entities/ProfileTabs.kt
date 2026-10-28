package com.example.ui.screens.entities

import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.ui.*
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
        ProfileEntityType.TECHNICIAN -> listOf("الخدمات 🛠️", "التقييمات والآراء ⭐", "المعرض 📸", "عن الفني 👤", "أوقات العمل ⏰")
        ProfileEntityType.STORE -> listOf("الكتالوج والمنتجات 📦", "العروض والخصومات 🏷️", "التقييمات ⭐", "سياسة التوصيل 🚚", "طرق الدفع 💳", "عن المتجر 🏬")
        ProfileEntityType.RESTAURANT -> listOf("القائمة والأصناف 🍽️", "العروض والخصومات 🔥", "التقييمات ⭐", "التوصيل والمناطق 🛵", "أوقات العمل ⏰", "عن المطعم 🏪")
        ProfileEntityType.MEDICAL -> listOf("العيادات والتخصصات 🩺", "الأطباء 👨‍⚕️", "حجز موعد 📅", "المختبر والأشعة 🔬", "الطوارئ وأوقات العمل 🚨", "التقييمات والآراء ⭐", "عن المركز 🏥")
        ProfileEntityType.REAL_ESTATE -> listOf("التفاصيل والمواصفات 📐", "الصور والمعرض 🖼️", "الموقع على الخريطة 🗺️", "المرافق والخدمات 🏢", "التواصل مع المعلن 📞")
        ProfileEntityType.JOB -> listOf("وصف الوظيفة 📋", "المتطلبات والمؤهلات 🎓", "المزايا والراتب 💼", "معلومات الشركة 🏢", "طريقة التقديم ⚡")
        ProfileEntityType.GENERAL -> listOf("الخدمات 🛠️", "التقييمات ⭐", "عن المنشأة ℹ️", "أوقات العمل ⏰")
    }

    androidx.compose.material3.ScrollableTabRow(
        selectedTabIndex = selectedTab.coerceIn(0, tabTitles.size - 1),
        containerColor = themeColors.surface,
        contentColor = themeColors.accent,
        edgePadding = 8.dp,
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
