package com.example.ui.screens.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.SmartAsyncImage
import com.example.utils.VisualThemePalette

/**
 * 📦 بطاقة إدارة السلعة/الخدمة للتاجر مع ميزة:
 * - تعديل الأسعار السريع (In-line Quick Edit) بلمسة واحدة
 * - تبديل حالة التوفر (متوفر بالمخزون / نفد)
 * - شارة العرض الخاص (عرض خاص 🎁)
 */
@Composable
fun UnifiedProductsServicesSection(
    title: String,
    description: String = "",
    price: String = "",
    imageUrl: String = "",
    isAvailable: Boolean = true,
    isOffer: Boolean = false,
    themeColors: VisualThemePalette,
    onEditClick: (() -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null,
    onToggleAvailability: ((Boolean) -> Unit)? = null,
    onQuickPriceUpdate: ((Double) -> Unit)? = null
) {
    var showQuickPriceDialog by remember { mutableStateOf(false) }
    var editPriceInput by remember(price) { mutableStateOf(price) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.2f))
                    .border(1.dp, themeColors.accent.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (imageUrl.isNotBlank()) {
                    SmartAsyncImage(
                        model = imageUrl,
                        contentDescription = title,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(text = "📦", fontSize = 24.sp)
                }

                if (isOffer) {
                    Surface(
                        color = Color(0xFFEF4444),
                        shape = RoundedCornerShape(bottomStart = 6.dp),
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Text(
                            text = "عرض 🎁",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = themeColors.textPrimary
                )
                if (description.isNotBlank()) {
                    Text(
                        text = description,
                        fontSize = 10.5.sp,
                        color = themeColors.textSecondary,
                        maxLines = 1
                    )
                }

                // السعر مع ميزة التعديل السريع المباشر (In-line Quick Edit)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        onClick = { showQuickPriceDialog = true },
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(0.6.dp, Color(0xFF10B981))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "$price ريال يمني",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981)
                            )
                            Text("✏️", fontSize = 10.sp)
                        }
                    }

                    // شارة التوفر / نفد بالمخزون
                    if (onToggleAvailability != null) {
                        Surface(
                            onClick = { onToggleAvailability(!isAvailable) },
                            shape = RoundedCornerShape(6.dp),
                            color = if (isAvailable) Color(0xFF065F46).copy(alpha = 0.2f) else Color(0xFFEF4444).copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = if (isAvailable) "متوفر بالمخزون ✓" else "نفد من المخزون ✕",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isAvailable) Color(0xFF34D399) else Color(0xFFF87171),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                if (onEditClick != null) {
                    IconButton(onClick = onEditClick, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = Color.LightGray, modifier = Modifier.size(16.dp))
                    }
                }
                if (onDeleteClick != null) {
                    IconButton(onClick = onDeleteClick, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }

    // نافذة التعديل السريع للسعر (In-line Quick Edit Dialog)
    if (showQuickPriceDialog) {
        AlertDialog(
            onDismissRequest = { showQuickPriceDialog = false },
            title = {
                Text(
                    text = "تعديل سعر: $title 💰",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "أدخل السعر الجديد بالريال اليمني:",
                        fontSize = 11.sp,
                        color = Color.LightGray
                    )
                    OutlinedTextField(
                        value = editPriceInput,
                        onValueChange = { editPriceInput = it },
                        label = { Text("السعر (ريال يمني)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newP = editPriceInput.toDoubleOrNull() ?: 0.0
                        onQuickPriceUpdate?.invoke(newP)
                        showQuickPriceDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent)
                ) {
                    Text("حفظ السعر 💾", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuickPriceDialog = false }) {
                    Text("إلغاء")
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }
}
