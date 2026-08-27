package com.example.ui.screens.bookings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.OrderEntity
import com.example.utils.VisualThemePalette

@Composable
fun OrderCardItem(
    order: OrderEntity,
    themeColors: VisualThemePalette,
    onDeleteClick: (OrderEntity) -> Unit
) {
    val deleteCode = remember(order.id) {
        (order.id.hashCode().coerceAtLeast(0) % 9000 + 1000).toString()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
        border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Order ID & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "طلب شراء #${order.id.takeLast(6)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = themeColors.accent
                )
                Surface(
                    color = when (order.status) {
                        "COMPLETED" -> Color(0xFF10B981)
                        "CANCELLED" -> Color(0xFFEF4444)
                        "PROCESSING" -> Color(0xFF3B82F6)
                        else -> Color(0xFFF59E0B)
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = when (order.status) {
                            "COMPLETED" -> "تم التوصيل ✅"
                            "CANCELLED" -> "ملغي ❌"
                            "PROCESSING" -> "قيد التجهيز ⏳"
                            else -> "قيد الانتظار ⚡"
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontSize = 10.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Product details
            Text(
                text = "📦 المادة: ${order.productName}",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "🏪 المتجر: ${order.storeName}",
                fontSize = 11.sp,
                color = Color.LightGray
            )
            Text(
                text = "📝 الملاحظات: ${order.notes.ifBlank { "لا توجد" }}",
                fontSize = 11.sp,
                color = Color.Gray
            )

            HorizontalDivider(
                color = Color.Gray.copy(alpha = 0.1f),
                thickness = 0.5.dp,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // Amount and Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "الكمية: ${order.quantity} | السعر: ${order.price} ر.ي",
                        fontSize = 11.sp,
                        color = Color.LightGray
                    )
                    Text(
                        text = "الإجمالي: ${order.totalAmount} ريال يمني",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeColors.primary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Deletion Code display
                    Box(
                        modifier = Modifier
                            .background(themeColors.primary.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "كود الحذف: $deleteCode",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = themeColors.accent
                        )
                    }

                    // Delete Order Button
                    IconButton(
                        onClick = { onDeleteClick(order) },
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color.Red.copy(alpha = 0.1f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "حذف",
                            tint = Color.Red,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
