package com.example.ui.screens.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.*
import com.example.utils.DateFormatter
import com.example.utils.VisualThemePalette

internal fun LazyListScope.adminEngagementSubTabs(
    activeSubTab: String,
    state: AdminPanelState,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette
) {
    val activatedProviders = state.activatedProviders
    val callsLog = state.callsLog
    val couponsList = state.couponsList

    when (activeSubTab) {
        "REVIEWS" -> {
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Text("⭐ إدارة التقييمات والمراجعات للأعضاء", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                Text("قم بالبحث وتعديل تقييمات وعدد مراجعات الفنيين مباشرة:", fontSize = 11.sp, color = themeColors.textSecondary)
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                    border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        activatedProviders.forEach { provider ->
                            var editRatingText by remember(provider.id) { mutableStateOf(provider.rating.toString()) }
                            var editReviewsText by remember(provider.id) { mutableStateOf(provider.numReviews.toString()) }

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(provider.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("${provider.profession} | 📍 ${provider.area}", fontSize = 11.sp, color = Color.LightGray)
                                    Text("التقييم الحالي: ⭐ ${provider.rating} (${provider.numReviews} تقييم)", fontSize = 10.sp, color = themeColors.accent)
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    OutlinedTextField(
                                        value = editRatingText,
                                        onValueChange = { editRatingText = it },
                                        label = { Text("التقييم", fontSize = 9.sp) },
                                        singleLine = true,
                                        modifier = Modifier.width(60.dp),
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                    )

                                    OutlinedTextField(
                                        value = editReviewsText,
                                        onValueChange = { editReviewsText = it },
                                        label = { Text("العدد", fontSize = 9.sp) },
                                        singleLine = true,
                                        modifier = Modifier.width(60.dp),
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                    )

                                    Button(
                                        onClick = {
                                            val r = editRatingText.toFloatOrNull() ?: provider.rating
                                            val n = editReviewsText.toIntOrNull() ?: provider.numReviews
                                            val updated = provider.copy(rating = r, numReviews = n)
                                            viewModel.updateProviderEntity(updated)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary),
                                        contentPadding = PaddingValues(horizontal = 8.dp),
                                        modifier = Modifier.height(36.dp)
                                    ) {
                                        Text("حفظ", fontSize = 10.sp, color = Color.White)
                                    }
                                }
                            }
                            Divider(color = Color.White.copy(alpha = 0.05f))
                        }
                    }
                }
            }
        }

        "CALLS" -> {
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Text("📞 سجلات ومراقبة المكالمات الجارية والمباشرة", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                Text("مراقبة وتتبع اتصالات المواطنين بالفنيين ومزودي الخدمات مباشرة:", fontSize = 11.sp, color = themeColors.textSecondary)
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (callsLog.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("📭 لا توجد سجلات مكالمات محفوظة حالياً.", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            } else {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            callsLog.sortedByDescending { it.timestamp }.forEach { call ->
                                val dateStr = DateFormatter.formatDateTime(call.timestamp)
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("📱 المكالمة لـ: ${call.providerName}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Text("👤 المتصل: ${call.callerName}", fontSize = 11.sp, color = Color.LightGray)
                                        Text("⏰ التاريخ: $dateStr", fontSize = 10.sp, color = themeColors.textSecondary)
                                    }

                                    IconButton(onClick = {
                                        viewModel.db.collection("calls").document(call.id).delete()
                                        viewModel.triggerNotification("🗑️ تم حذف سجل المكالمة")
                                    }) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف السجل", tint = Color.Red.copy(alpha = 0.8f))
                                    }
                                }
                                Divider(color = Color.White.copy(alpha = 0.05f))
                            }
                        }
                    }
                }
            }
        }

        "COUPONS" -> {
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Text("🎫 إدارة وتوليد الكوبونات الحصرية", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                Text("توليد وتوزيع الكوبونات الترويجية لشحن نقاط المواطنين مجاناً:", fontSize = 11.sp, color = themeColors.textSecondary)
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                    border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("➕ إنشاء كوبون جديد", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)

                        OutlinedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = state.couponCodeInput,
                            onValueChange = { state.couponCodeInput = it },
                            label = { Text("رمز الكوبون (مثال: YEMEN2026)") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                modifier = Modifier.weight(1f),
                                value = state.couponPointsInput,
                                onValueChange = { state.couponPointsInput = it },
                                label = { Text("قيمة النقاط") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )

                            OutlinedTextField(
                                modifier = Modifier.weight(1f),
                                value = state.couponExpiryDaysInput,
                                onValueChange = { state.couponExpiryDaysInput = it },
                                label = { Text("صلاحية الكوبون (أيام)") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                modifier = Modifier.weight(1f),
                                value = state.couponDiscountInput,
                                onValueChange = { state.couponDiscountInput = it },
                                label = { Text("نسبة الخصم %") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )

                            OutlinedTextField(
                                modifier = Modifier.weight(1f),
                                value = state.couponMaxUsageInput,
                                onValueChange = { state.couponMaxUsageInput = it },
                                label = { Text("مرات الاستخدام القصوى") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )
                        }

                        Button(
                            onClick = {
                                val code = state.couponCodeInput.trim().uppercase()
                                val points = state.couponPointsInput.toIntOrNull() ?: 100
                                val days = state.couponExpiryDaysInput.toLongOrNull() ?: 30L
                                val discount = state.couponDiscountInput.toIntOrNull() ?: 15
                                val maxUsage = state.couponMaxUsageInput.toIntOrNull() ?: 50
                                if (code.isNotBlank()) {
                                    viewModel.addCoupon(code, points, days * 24 * 60 * 60 * 1000L, discount, maxUsage)
                                    state.couponCodeInput = ""
                                } else {
                                    viewModel.triggerNotification("❌ يرجى إدخال رمز كوبون صالح!")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("إنشاء الكوبون وحفظه 🎫", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(10.dp))
                Text("📋 الكوبونات المتاحة بالمنصة", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
            }

            if (couponsList.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("📭 لا توجد كوبونات مسجلة حالياً.", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            } else {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            couponsList.forEach { coupon ->
                                val expiryStr = DateFormatter.formatDateDash(coupon.expiryTimestamp)
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("🎫 الكود: ${coupon.code}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Text("🎁 النقاط: ${coupon.pointsValue} نقطة | خصم: ${coupon.discountPercentage}% | الاستخدام: ${coupon.usedCount}/${coupon.maxUsageCount}", fontSize = 11.sp, color = Color.LightGray)
                                        Text("الصلاحية لغاية: $expiryStr | الحالة: ${coupon.status}", fontSize = 10.sp, color = themeColors.textSecondary)
                                    }

                                    IconButton(onClick = { viewModel.deleteCoupon(coupon.id) }) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف الكوبون", tint = Color.Red.copy(alpha = 0.8f))
                                    }
                                }
                                Divider(color = Color.White.copy(alpha = 0.05f))
                            }
                        }
                    }
                }
            }
        }
    }
}
