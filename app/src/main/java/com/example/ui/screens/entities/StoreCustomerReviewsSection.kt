package com.example.ui.screens.entities

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.RatingEntity
import com.example.data.StoreEntity
import com.example.ui.MainViewModel
import com.example.utils.EntityIdGenerator
import com.example.utils.VisualThemePalette

@Composable
internal fun StoreCustomerReviewsSection(
    storeReviews: List<RatingEntity>,
    themeColors: VisualThemePalette,
    onAddReviewClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("التقييمات وآراء العملاء ⭐ (${storeReviews.size})", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
            Button(
                onClick = onAddReviewClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA000)),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text("أضف تقييمك ⭐", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
        if (storeReviews.isEmpty()) {
            EmptyStateBox("لا توجد تقييمات مسجلة بعد. كن أول من يشارك رأيه!", themeColors)
        } else {
            storeReviews.forEach { review ->
                ProfileReviewCard(review = review, themeColors = themeColors)
            }
        }
    }
}

@Composable
internal fun StoreAddRatingDialog(
    store: StoreEntity,
    isMedical: Boolean,
    isRestaurant: Boolean,
    currentUserId: String,
    currentUserName: String,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var ratingStars by remember { mutableIntStateOf(5) }
    var reviewComment by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = themeColors.surface,
        title = { Text("تقييم ${store.name} ⭐", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = themeColors.textPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    (1..5).forEach { star ->
                        IconButton(onClick = { ratingStars = star }) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "$star نجوم",
                                tint = if (star <= ratingStars) Color(0xFFFFD700) else themeColors.textSecondary.copy(alpha = 0.4f),
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = reviewComment,
                    onValueChange = { reviewComment = it },
                    label = { Text("اكتب تجربتك (اختياري)", color = themeColors.textSecondary) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val ratingObj = RatingEntity(
                        id = EntityIdGenerator.generate("REV"),
                        targetId = store.id,
                        targetType = if (isMedical) "MEDICAL" else if (isRestaurant) "RESTAURANT" else "STORE",
                        userId = currentUserId.ifBlank { "guest" },
                        userName = currentUserName.ifBlank { "عميل" },
                        rating = ratingStars.toFloat(),
                        comment = reviewComment.trim(),
                        timestamp = System.currentTimeMillis()
                    )
                    viewModel.adminViewModel.submitRating(ratingObj) {
                        Toast.makeText(context, "شكراً لك! تم حفظ تقييمك بنجاح ✅", Toast.LENGTH_SHORT).show()
                    }
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent)
            ) {
                Text("إرسال التقييم ⭐", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", color = themeColors.textSecondary)
            }
        }
    )
}
