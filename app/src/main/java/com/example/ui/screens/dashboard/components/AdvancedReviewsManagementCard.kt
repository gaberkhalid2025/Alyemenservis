package com.example.ui.screens.dashboard.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.utils.VisualThemePalette

data class ReviewUiModel(
    val id: String = System.currentTimeMillis().toString(),
    val authorName: String,
    val rating: Int,
    val comment: String,
    var replyText: String = ""
)

@Composable
fun AdvancedReviewsManagementCard(
    themeColors: VisualThemePalette,
    modifier: Modifier = Modifier
) {
    var reviews by remember {
        mutableStateOf<List<ReviewUiModel>>(emptyList())
    }

    Card(
        modifier = modifier.fillMaxWidth().padding(8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            Text("إدارة التعليقات والردود ⭐", fontSize = 16.sp, color = themeColors.primary)
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(modifier = Modifier.height(200.dp).fillMaxWidth()) {
                items(reviews) { review ->
                    var replyInput by remember { mutableStateOf(review.replyText) }
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(10.dp).fillMaxWidth()) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(review.authorName, fontSize = 13.sp, color = themeColors.primary)
                                Row {
                                    repeat(review.rating) {
                                        Icon(Icons.Default.Star, contentDescription = null, tint = androidx.compose.ui.graphics.Color(0xFFFFC107), modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                            Text(review.comment, fontSize = 12.sp, modifier = Modifier.padding(vertical = 4.dp))
                            
                            if (review.replyText.isNotBlank()) {
                                Text("ردك: ${review.replyText}", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                            } else {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    OutlinedTextField(
                                        value = replyInput,
                                        onValueChange = { replyInput = it },
                                        placeholder = { Text("اكتب ردك هنا...", fontSize = 11.sp) },
                                        modifier = Modifier.weight(1f).height(48.dp)
                                    )
                                    Button(
                                        onClick = {
                                            reviews = reviews.map { if (it.id == review.id) it.copy(replyText = replyInput) else it }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary),
                                        modifier = Modifier.height(48.dp)
                                    ) {
                                        Text("إرسال", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
