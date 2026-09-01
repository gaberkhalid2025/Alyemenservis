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
import com.example.ui.utils.getStarsString
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
                Text("⭐ آرائ وتقييمات العملاء لـ ${provider.name}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                Text("متوسط التقييم: ${provider.rating} / 5.0 (إجمالي ${provider.numReviews} تقييم)", fontSize = 11.sp, color = Color.LightGray)

                val reviewsList = listOf("خدمة ممتازة وسريعة جداً!", "فني محترف ويلتزم بالمواعيد دائمًا.")

                reviewsList.forEach { rev ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(getStarsString(provider.rating), color = Color.Yellow, fontSize = 12.sp)
                            Text(rev, fontSize = 11.sp, color = Color.White)
                        }
                    }
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("إغلاق", color = Color.White, fontSize = 11.sp)
                }
            }
        }
    }
}
