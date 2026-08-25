package com.example.ui.screens.entities

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.SmartAsyncImage
import com.example.utils.VisualThemePalette

@Composable
fun ProfileHeader(
    entityName: String,
    entityType: ProfileEntityType,
    entityAddress: String,
    ratingValue: Float,
    reviewsCount: Int,
    profileCover: String,
    entityDescription: String,
    isVerified: Boolean,
    isVip: Boolean,
    isOwner: Boolean,
    bookingsCount: Int,
    completedRevenue: Double,
    themeColors: VisualThemePalette
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.25f))
    ) {
        Column {
            // Cover & Hero image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(themeColors.primary, themeColors.secondary)
                        )
                    )
            ) {
                if (profileCover.isNotBlank()) {
                    SmartAsyncImage(
                        model = profileCover,
                        contentDescription = entityName,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                // Type Tag
                Box(
                    modifier = Modifier
                        .padding(12.dp)
                        .align(Alignment.TopEnd)
                        .background(entityType.badgeColor, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = entityType.labelAr,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Info Body
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = entityName,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            if (isVerified) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "موثق رسميًا",
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            if (isVip) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFFFA000), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text("VIP", color = Color.Black, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Text(
                            text = "📍 $entityAddress",
                            fontSize = 12.sp,
                            color = Color.LightGray
                        )
                    }

                    // Rating Badge
                    Surface(
                        color = Color(0xFFFFA000).copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, Color(0xFFFFA000)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(String.format("%.1f", ratingValue), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(" ($reviewsCount)", color = Color.LightGray, fontSize = 10.sp)
                        }
                    }
                }

                Text(
                    text = entityDescription,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    lineHeight = 18.sp
                )

                // Owner Business Statistics Section
                if (isOwner) {
                    Divider(color = themeColors.accent.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        text = "📊 إحصائيات الأداء (خاصة بك كمالك):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeColors.accent
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatMetricCard("الحجوزات", "$bookingsCount حجز", Modifier.weight(1f), themeColors)
                        StatMetricCard("التقييمات", "$reviewsCount تقييم", Modifier.weight(1f), themeColors)
                        if (entityType == ProfileEntityType.TECHNICIAN || entityType == ProfileEntityType.STORE || entityType == ProfileEntityType.RESTAURANT) {
                            StatMetricCard("الإيرادات", "${completedRevenue.toInt()} ر.ي", Modifier.weight(1.2f), themeColors, Color(0xFF10B981))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatMetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    themeColors: VisualThemePalette,
    valueColor: Color = Color.White
) {
    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        Column {
            Text(label, fontSize = 10.sp, color = Color.Gray)
            Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = valueColor)
        }
    }
}
