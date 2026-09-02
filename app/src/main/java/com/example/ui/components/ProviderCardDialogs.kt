package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.ProviderEntity
import com.example.ui.MainViewModel
import com.example.utils.getStarsString
import com.example.utils.VisualThemePalette

@Composable
fun ProviderDetailsDialog(
    provider: ProviderEntity,
    themeColors: VisualThemePalette,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val detailProfessionText = provider.profession.ifEmpty { "صيانة فنية شاملة" }
    val detailSpecializationText = provider.specialization.ifEmpty { "جميع خدمات الصيانة العامة" }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.padding(16.dp).fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("📋 بطاقة بيانات الفني المعتمد", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("👤 الاسم واللقب:", fontSize = 12.sp, color = themeColors.textSecondary)
                        Text(provider.name, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("💼 المهنة والوظيفة:", fontSize = 12.sp, color = themeColors.textSecondary)
                        Text(detailProfessionText, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("🎓 التخصص المهني:", fontSize = 12.sp, color = themeColors.textSecondary)
                        Text(detailSpecializationText, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("📍 المحافظة والمنطقة:", fontSize = 12.sp, color = themeColors.textSecondary)
                        Text(provider.area, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("🏡 الحي / الحارة:", fontSize = 12.sp, color = themeColors.textSecondary)
                        Text(provider.localNeighborhood, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("📞 رقم الاتصال:", fontSize = 12.sp, color = themeColors.textSecondary)
                        Text(provider.phone, fontSize = 12.sp, color = themeColors.accent, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("🔧 حالة التوفر الحالية:", fontSize = 12.sp, color = themeColors.textSecondary)
                        Text(
                            text = if (provider.isAvailable) "متاح للعمل الفوري 🟢" else "مشغول حالياً 🔴",
                            fontSize = 12.sp,
                            color = if (provider.isAvailable) Color.Green else Color.Red,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("⭐ النقاط والتقييمات التراكمية:", fontSize = 12.sp, color = themeColors.textSecondary)
                        Text("${provider.points} نقطة مهنية", fontSize = 12.sp, color = Color.Yellow, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("إغلاق التفاصيل ❌", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ProviderReviewsListDialog(
    provider: ProviderEntity,
    themeColors: VisualThemePalette,
    viewModel: MainViewModel? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val allRatingsState = viewModel?.ratings?.collectAsState()
    val providerRatings = remember(allRatingsState?.value, provider.id) {
        allRatingsState?.value?.filter { it.targetId == provider.id || it.providerId == provider.id } ?: emptyList()
    }

    var selectedRating by remember { mutableStateOf(5) }
    var reviewComment by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.padding(12.dp).fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("💬 الآراء والتجارب لـ ${provider.name}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                Text("التقييم العام: ${String.format("%.1f", provider.rating)} / 5.0 (إجمالي ${providerRatings.size} رأي وتجربة حقيقية)", fontSize = 11.sp, color = Color.LightGray)

                if (providerRatings.isEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("📝 لا توجد آراء أو تجارب مسجلة حالياً.", fontSize = 11.5.sp, color = Color.LightGray, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("كن أول من يشارك رأيه وتجربته مع هذا المقدم!", fontSize = 10.5.sp, color = themeColors.accent)
                        }
                    }
                } else {
                    providerRatings.forEach { rev ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (rev.userName.isNotBlank()) rev.userName else "عميل مجهول",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = themeColors.accent
                                    )
                                    Text(getStarsString(rev.rating), color = Color.Yellow, fontSize = 11.sp)
                                }
                                if (rev.comment.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(rev.comment, fontSize = 11.sp, color = Color.White)
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 4.dp))

                Text("✍️ إضافة رأيك وتجربتك:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    (1..5).forEach { star ->
                        IconButton(
                            onClick = { selectedRating = star },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Text(
                                text = if (star <= selectedRating) "⭐" else "☆",
                                fontSize = 18.sp
                            )
                        }
                    }
                    Text("($selectedRating / 5)", fontSize = 11.sp, color = Color.Yellow, fontWeight = FontWeight.Bold)
                }

                OutlinedTextField(
                    value = reviewComment,
                    onValueChange = { reviewComment = it },
                    placeholder = { Text("اكتب رأيك وتجربتك بالتفصيل هنا...", fontSize = 11.sp, color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = themeColors.accent,
                        unfocusedBorderColor = Color.Gray
                    ),
                    maxLines = 3
                )

                Button(
                    onClick = {
                        if (reviewComment.trim().isBlank()) {
                            Toast.makeText(context, "يرجى كتابة ملاحظتك أو تجربتك أولاً", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isSubmitting = true
                        val newRating = com.example.data.RatingEntity(
                            id = "rev_" + System.currentTimeMillis(),
                            targetId = provider.id,
                            targetType = "PROVIDER",
                            rating = selectedRating.toFloat(),
                            comment = reviewComment.trim(),
                            userName = "عميل تطبيق دليل اليمن",
                            timestamp = System.currentTimeMillis()
                        )
                        viewModel?.addRating(newRating)
                        Toast.makeText(context, "شكرًا لك! تم إرسال رأيك وتجربتك بنجاح.", Toast.LENGTH_LONG).show()
                        reviewComment = ""
                        isSubmitting = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSubmitting,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("إرسال الرأي والتجربة 🚀", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("إغلاق ❌", color = Color.White, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun GenericEntityReviewsDialog(
    title: String,
    rating: Float,
    numReviews: Int,
    themeColors: VisualThemePalette,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.padding(16.dp).fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("💬 الآراء والتجارب لـ $title", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                Text("التقييم العام: ${String.format("%.1f", rating)} / 5.0 (إجمالي $numReviews تقييم)", fontSize = 10.5.sp, color = Color.LightGray)

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("📝 لا توجد آراء أو تجارب مسجلة حالياً لهذا القسم.", fontSize = 11.sp, color = Color.LightGray)
                    }
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("إغلاق ❌", color = Color.White, fontSize = 11.sp)
                }
            }
        }
    }
}
