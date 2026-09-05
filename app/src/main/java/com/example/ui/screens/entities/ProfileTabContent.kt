package com.example.ui.screens.entities

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import com.example.data.*
import com.example.ui.components.SmartAsyncImage
import com.example.utils.VisualThemePalette

@Composable
fun EmptyStateBox(msg: String, themeColors: VisualThemePalette) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
            .background(Color.Black.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = msg, color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center)
    }
}

@Composable
fun ProfileTabContent(
    selectedTab: Int,
    entityType: ProfileEntityType,
    provider: ProviderEntity?,
    store: StoreEntity?,
    property: PropertyEntity?,
    job: JobEntity?,
    products: List<ProductEntity>,
    entityReviews: List<RatingEntity>,
    entityDescription: String,
    themeColors: VisualThemePalette,
    isOwner: Boolean = false,
    onAddReviewClick: () -> Unit
) {
    when (selectedTab) {
        0 -> {
            // First Tab (Photos / Products / Menu / Listings)
            when (entityType) {
                ProfileEntityType.TECHNICIAN -> {
                    val photos = provider?.workPhotosBase64 ?: emptyList()
                    if (photos.isEmpty()) {
                        EmptyStateBox("لا توجد صور سابقة أعمال مرفوعة حالياً.", themeColors)
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(photos) { photo ->
                                Card(
                                    modifier = Modifier.size(120.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    SmartAsyncImage(model = photo, contentDescription = "عمل سابق", modifier = Modifier.fillMaxSize())
                                }
                            }
                        }
                    }
                }
                ProfileEntityType.STORE, ProfileEntityType.RESTAURANT, ProfileEntityType.MEDICAL -> {
                    val storeProducts = products.filter { it.storeId == (store?.id ?: "") }
                    if (storeProducts.isEmpty()) {
                        EmptyStateBox("لا توجد أصناف مدرجة في القائمة حالياً.", themeColors)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            storeProducts.forEach { prod ->
                                ProfileProductCard(product = prod, themeColors = themeColors)
                            }
                        }
                    }
                }
                ProfileEntityType.REAL_ESTATE -> {
                    val images = property?.images ?: emptyList()
                    if (images.isEmpty()) {
                        EmptyStateBox("لا توجد صور إضافية للعقار.", themeColors)
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(images) { img ->
                                Card(
                                    modifier = Modifier.size(140.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    SmartAsyncImage(model = img, contentDescription = "صورة العقار", modifier = Modifier.fillMaxSize())
                                }
                            }
                        }
                    }
                }
                else -> {
                    Text(
                        text = entityDescription,
                        color = Color.White,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
        1 -> {
            // Reviews / Secondary info
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("التقييمات وآراء العملاء ⭐", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    if (!isOwner) {
                        Button(
                            onClick = onAddReviewClick,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA000)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("أضف تقييمك ⭐", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (entityReviews.isEmpty()) {
                    EmptyStateBox("لا توجد تقييمات مسجلة بعد. كن أول من يكتب تقييماً!", themeColors)
                } else {
                    entityReviews.forEach { review ->
                        ProfileReviewCard(review = review, themeColors = themeColors)
                    }
                }
            }
        }
        2 -> {
            // Warranty / Working Hours / Policy
            Card(
                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("📜 الشروط والضمان المعتمد بالمنصة", fontWeight = FontWeight.Bold, color = themeColors.accent, fontSize = 13.sp)
                    Text("• جميع التعاملات تخضع لميثاق الجودة والحماية في دليل خدمات اليمن.", color = Color.White, fontSize = 11.sp)
                    Text("• إمكانية استرجاع الرسوم أو رفع شكوى مباشرة لإدارة المنصة في حال الإخلال بالمواصفات.", color = Color.LightGray, fontSize = 11.sp)
                    Text("• الدفع المباشر عبر المحافظ الإلكترونية المعتمدة (الكريمي، جيب، جوالي، ون كاش).", color = Color.LightGray, fontSize = 11.sp)
                }
            }
        }
    }
}
