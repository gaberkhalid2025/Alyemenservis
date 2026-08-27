package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/**
 * 🌟 Premium Multi-Dimensional Rating & Review Input (10/10 UX)
 * Allows users to rate multiple dimensions of the service provider:
 * 1. Speed & Execution (السرعة والالتزام)
 * 2. Quality of Service (جودة العمل)
 * 3. Fair Price (السعر المناسب)
 * Computes the aggregate rating automatically while maintaining full backward-compatibility.
 */
@Composable
fun ReviewInput(
    modifier: Modifier = Modifier,
    onSubmit: (rating: Int, comment: String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    
    // Multi-dimensional ratings
    var ratingSpeed by remember { mutableIntStateOf(5) }
    var ratingQuality by remember { mutableIntStateOf(5) }
    var ratingPrice by remember { mutableIntStateOf(5) }
    
    var comment by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }

    if (!isExpanded) {
        OutlinedButton(
            onClick = { isExpanded = true },
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .height(48.dp)
                .testTag("expand_review_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text("⭐ ", fontSize = 16.sp)
                Text(
                    text = "أضف تقييمك ورأيك في الخدمة (تقييم ثلاثي الأبعاد)",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                )
            }
        }
    } else {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(12.dp)
                .testTag("review_input_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with Title and Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "شاركنا تقييمك التفصيلي للخدمة",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(
                        onClick = { isExpanded = false },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Dimension 1: Speed
                RatingDimensionRow(
                    title = "⚡ السرعة والالتزام بالوقت",
                    rating = ratingSpeed,
                    onRatingChanged = { ratingSpeed = it },
                    tagPrefix = "speed"
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Dimension 2: Quality
                RatingDimensionRow(
                    title = "🛠️ جودة الخدمة والعمل",
                    rating = ratingQuality,
                    onRatingChanged = { ratingQuality = it },
                    tagPrefix = "quality"
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Dimension 3: Price
                RatingDimensionRow(
                    title = "💰 مناسبة السعر والاتفاق",
                    rating = ratingPrice,
                    onRatingChanged = { ratingPrice = it },
                    tagPrefix = "price"
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Feedback TextField
                OutlinedTextField(
                    value = comment,
                    onValueChange = { 
                        comment = it
                        if (it.isNotBlank()) errorMsg = ""
                    },
                    label = { Text("اكتب تعليقك هنا...") },
                    placeholder = { Text("كيف كانت تجربتك مع هذه الخدمة؟") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .testTag("review_comment_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    maxLines = 4
                )

                if (errorMsg.isNotBlank()) {
                    Text(
                        text = errorMsg,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .align(Alignment.Start)
                            .padding(top = 4.dp, start = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Submit Button
                Button(
                    onClick = {
                        if (comment.isBlank()) {
                            errorMsg = "الرجاء كتابة تعليق قبل الإرسال"
                        } else {
                            // Compute mathematically rounded average for backward compatibility
                            val aggregateRating = ((ratingSpeed + ratingQuality + ratingPrice) / 3.0f).roundToInt()
                            onSubmit(aggregateRating.coerceIn(1, 5), comment)
                            comment = ""
                            ratingSpeed = 5
                            ratingQuality = 5
                            ratingPrice = 5
                            errorMsg = ""
                            isExpanded = false
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("submit_review_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(
                        text = "إرسال التقييم",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun RatingDimensionRow(
    title: String,
    rating: Int,
    onRatingChanged: (Int) -> Unit,
    tagPrefix: String
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.testTag("rating_row_$tagPrefix")
        ) {
            for (i in 1..5) {
                val isSelected = i <= rating
                Icon(
                    imageVector = if (isSelected) Icons.Filled.Star else Icons.Outlined.Star,
                    contentDescription = "$title Star $i",
                    tint = if (isSelected) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onRatingChanged(i) }
                        .testTag("star_${tagPrefix}_$i")
                )
            }
        }
    }
}
