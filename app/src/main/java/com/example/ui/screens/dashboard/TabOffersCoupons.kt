package com.example.ui.screens.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.dashboard.components.UnifiedEmptyState
import com.example.utils.VisualThemePalette

data class OfferItem(
    val id: String = System.currentTimeMillis().toString(),
    val title: String,
    val description: String,
    val discountPercent: Int,
    val couponCode: String = ""
)

@Composable
fun TabOffersCoupons(
    themeColors: VisualThemePalette
) {
    var offersList by remember { mutableStateOf(listOf<OfferItem>()) }
    var showAddDialog by remember { mutableStateOf(false) }

    var titleInput by remember { mutableStateOf("") }
    var descInput by remember { mutableStateOf("") }
    var discountInput by remember { mutableStateOf("") }
    var codeInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "🏷️ العروض والتخفيضات والكوبونات (${offersList.size})", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = themeColors.textPrimary)
            Button(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("إضافة عرض ➕", fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }

        if (offersList.isEmpty()) {
            UnifiedEmptyState(
                title = "لا توجد عروض ترويجية نشطة حالياً",
                description = "يمكنك إضافة عروض خصم أو كود خصم لجذب المزيد من العملاء.",
                iconText = "🏷️",
                actionLabel = "إضافة عرض جديد ➕",
                onActionClick = { showAddDialog = true },
                themeColors = themeColors
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(offersList, key = { it.id }) { offer ->
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
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(text = offer.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.textPrimary)
                                if (offer.description.isNotBlank()) {
                                    Text(text = offer.description, fontSize = 11.sp, color = themeColors.textSecondary)
                                }
                                if (offer.couponCode.isNotBlank()) {
                                    Text(text = "كود الخصم: ${offer.couponCode}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                                }
                            }
                            Surface(
                                color = Color(0xFFEF4444),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "خصم %${offer.discountPercent}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("إضافة عرض أو كوبون خصم جديد", fontSize = 14.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = titleInput,
                        onValueChange = { titleInput = it },
                        label = { Text("عنوان العرض (مثال: خصم الصيف)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = descInput,
                        onValueChange = { descInput = it },
                        label = { Text("وصف العرض / الشروط") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = discountInput,
                        onValueChange = { discountInput = it },
                        label = { Text("نسبة الخصم %") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = codeInput,
                        onValueChange = { codeInput = it },
                        label = { Text("كود الكوبون (اختياري)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val disc = discountInput.toIntOrNull() ?: 10
                        if (titleInput.isNotBlank()) {
                            offersList = offersList + OfferItem(
                                title = titleInput,
                                description = descInput,
                                discountPercent = disc,
                                couponCode = codeInput
                            )
                            titleInput = ""
                            descInput = ""
                            discountInput = ""
                            codeInput = ""
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent)
                ) {
                    Text("إضافة العرض", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}
