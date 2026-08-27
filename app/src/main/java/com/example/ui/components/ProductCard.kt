package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ProductEntity
import com.example.ui.components.SnackbarManager
import com.example.ui.components.SnackbarType
import com.example.utils.VisualThemePalette

/**
 * 📦 ProductListItemCard Component (10/10 UX & Safety)
 * Shows a service catalog item/product with support for:
 * 1. Safe inline price adjustment with dynamic validation and confirmation dialogs
 * 2. Adaptable layouts for restaurants, medical, and commercial entities
 * 3. Shimmer-backed loading indicators
 */
@Composable
fun ProductListItemCard(
    product: ProductEntity,
    isOwnerOrAdmin: Boolean,
    themeColors: VisualThemePalette,
    isMedical: Boolean = false,
    isRestaurant: Boolean = false,
    onSaveProduct: ((ProductEntity) -> Unit)? = null,
    viewModel: Any? = null,
    onOrderClick: () -> Unit
) {
    var editingPrice by remember(product.price) { mutableStateOf(product.price.toString()) }
    var isPriceEditing by remember { mutableStateOf(false) }
    
    // Safety confirmation dialog state
    var showConfirmDialog by remember { mutableStateOf(false) }
    var targetNewPrice by remember { mutableDoubleStateOf(0.0) }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("تأكيد تعديل السعر ⚠️", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Text(
                    text = "هل أنت متأكد من تغيير سعر السلعة \"${product.name}\" من (${product.price} ريال) إلى ($targetNewPrice ريال)؟",
                    fontSize = 13.sp,
                    color = Color.LightGray
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveProduct?.invoke(product.copy(price = targetNewPrice))
                        isPriceEditing = false
                        showConfirmDialog = false
                        SnackbarManager.showSnackbar("✅ تم تحديث السعر بنجاح إلى $targetNewPrice ريال", SnackbarType.SUCCESS)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("نعم، تأكيد التعديل", color = Color.White, fontSize = 12.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("إلغاء", color = Color.Gray, fontSize = 12.sp)
                }
            },
            containerColor = Color(0xFF1E293B),
            shape = RoundedCornerShape(14.dp)
        )
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth().testTag("product_item_card_${product.id}")
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // صورة المنتج مع Shimmer
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.DarkGray),
                contentAlignment = Alignment.Center
            ) {
                val fallbackEmoji = when {
                    isMedical -> "🏥"
                    isRestaurant -> "🍔"
                    else -> "📦"
                }
                com.example.ui.components.SmartAsyncImage(
                    model = product.imageUrl,
                    contentDescription = product.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    fallbackEmoji = fallbackEmoji
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = product.name,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (product.isOffer || product.discountPercent > 0) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFEF4444), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text("🔥 خصم", fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (product.isOffer || (product.price > 0 && product.price <= 2000)) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFFFD700), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text("🏷️ أرخص سعر", fontSize = 8.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (product.description.isNotEmpty()) {
                    Text(
                        text = product.description,
                        fontSize = 10.sp,
                        color = themeColors.textSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // عرض السعر أو محرر السعر المباشر
                if (isOwnerOrAdmin && isPriceEditing) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        OutlinedTextField(
                            value = editingPrice,
                            onValueChange = { editingPrice = it },
                            modifier = Modifier.width(90.dp).height(38.dp).testTag("edit_price_input"),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                        Button(
                            onClick = {
                                val pVal = editingPrice.toDoubleOrNull() ?: product.price
                                if (pVal != product.price) {
                                    targetNewPrice = pVal
                                    showConfirmDialog = true
                                } else {
                                    isPriceEditing = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.height(34.dp).testTag("save_price_button")
                        ) {
                            Text("💾 حفظ", fontSize = 9.sp, color = Color.White)
                        }
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "🔽 ${if (product.price % 1.0 == 0.0) product.price.toLong() else product.price} ريال",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981)
                        )
                        if (isOwnerOrAdmin) {
                            Text(
                                text = "✏️ تعديل",
                                fontSize = 9.sp,
                                color = Color.Cyan,
                                modifier = Modifier
                                    .clickable { isPriceEditing = true }
                                    .padding(horizontal = 2.dp)
                                    .testTag("edit_price_trigger")
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // زر طلب أو شراء
            Button(
                onClick = onOrderClick,
                colors = ButtonDefaults.buttonColors(containerColor = if (isMedical) Color(0xFF0284C7) else themeColors.accent),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.testTag("order_button")
            ) {
                val btnText = when {
                    isMedical -> "📅 حجز موعد"
                    isRestaurant -> "🍽️ طلب الوجبة"
                    else -> "🛒 شراء"
                }
                Text(btnText, color = if (isMedical) Color.White else Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
