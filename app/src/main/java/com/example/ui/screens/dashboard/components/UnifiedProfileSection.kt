package com.example.ui.screens.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.SmartAsyncImage
import com.example.utils.VisualThemePalette

@Composable
fun UnifiedProfileSection(
    title: String,
    subtitle: String = "",
    phone: String = "",
    cityArea: String = "",
    photoUrl: String = "",
    coverUrl: String = "",
    rating: Double = 5.0,
    reviewCount: Int = 0,
    isAvailable: Boolean = true,
    themeColors: VisualThemePalette
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(Color.Black.copy(alpha = 0.3f))
            ) {
                if (coverUrl.isNotBlank()) {
                    SmartAsyncImage(
                        model = coverUrl,
                        contentDescription = "Cover",
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 16.dp, bottom = 8.dp)
                        .size(64.dp)
                        .clip(CircleShape)
                        .border(2.dp, themeColors.accent, CircleShape)
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    if (photoUrl.isNotBlank()) {
                        SmartAsyncImage(
                            model = photoUrl,
                            contentDescription = "Profile Photo",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(text = "👤", fontSize = 28.sp)
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeColors.textPrimary
                    )

                    Surface(
                        color = if (isAvailable) Color(0xFF10B981) else Color(0xFFEF4444),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (isAvailable) "متاح الآن 🟢" else "مشغول / مغلق 🔴",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                if (subtitle.isNotBlank()) {
                    Text(text = subtitle, fontSize = 12.sp, color = themeColors.textSecondary)
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (phone.isNotBlank()) {
                        Text(text = "📞 $phone", fontSize = 11.sp, color = themeColors.accent)
                    }
                    if (cityArea.isNotBlank()) {
                        Text(text = "📍 $cityArea", fontSize = 11.sp, color = themeColors.textSecondary)
                    }
                    Text(text = "⭐ %.1f ($reviewCount تقييم)".format(rating, reviewCount), fontSize = 11.sp, color = Color(0xFFFFA000))
                }
            }
        }
    }
}
