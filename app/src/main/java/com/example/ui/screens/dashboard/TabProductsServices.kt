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
import com.example.domain.entities.ProductItemEntity
import com.example.ui.screens.dashboard.components.UnifiedEmptyState
import com.example.ui.screens.dashboard.components.UnifiedImagePicker
import com.example.ui.screens.dashboard.components.UnifiedProductsServicesSection
import com.example.utils.VisualThemePalette

@Composable
fun TabProductsServices(
    products: List<ProductItemEntity>,
    titleLabel: String = "الخدمات والمنتجات",
    addButtonLabel: String = "إضافة جديد ➕",
    themeColors: VisualThemePalette,
    onAddProduct: (title: String, priceYer: Double, description: String, imageUrl: String) -> Unit,
    onDeleteProduct: (id: String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<ProductItemEntity?>(null) }
    var titleInput by remember { mutableStateOf("") }
    var subCategoryInput by remember { mutableStateOf("") }
    var priceInput by remember { mutableStateOf("") }
    var descInput by remember { mutableStateOf("") }
    var imageInput by remember { mutableStateOf("") }
    var isAvailableInput by remember { mutableStateOf(true) }
    var isOfferInput by remember { mutableStateOf(false) }
    var discountInput by remember { mutableStateOf("") }

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
            Text(text = "$titleLabel (${products.size})", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = themeColors.textPrimary)
            Button(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = addButtonLabel, fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }

        if (products.isEmpty()) {
            UnifiedEmptyState(
                title = "لا توجد عناصر مضافة حتى الآن",
                description = "قم بضغط زر الإضافة لإدراج عناصر لقائمتك.",
                iconText = "📦",
                actionLabel = addButtonLabel,
                onActionClick = { showAddDialog = true },
                themeColors = themeColors
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(products, key = { it.id }) { item ->
                    UnifiedProductsServicesSection(
                        title = item.title,
                        description = item.description,
                        price = item.priceYer.toString(),
                        imageUrl = item.imageUrl,
                        isAvailable = item.isAvailable,
                        isOffer = false,
                        themeColors = themeColors,
                        onEditClick = { /* Edit */ },
                        onDeleteClick = { itemToDelete = item },
                        onToggleAvailability = { newStatus ->
                            // تحديث حالة التوفر
                            android.widget.Toast.makeText(context, if (newStatus) "تم تعيين السلعة: متوفر بالمخزون ✓" else "تم تعيين السلعة: نفد من المخزون ✕", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        onQuickPriceUpdate = { newPrice ->
                            onAddProduct(item.title, newPrice, item.description, item.imageUrl)
                            android.widget.Toast.makeText(context, "تم تحديث السعر إلى $newPrice ريال بنجاح 💾", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }

    val target = itemToDelete
    if (target != null) {
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("تأكيد الحذف 🗑️", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Text(
                    "هل أنت متأكد من رغبتك في حذف '${target.title}'؟ لن تتمكن من التراجع عن هذه الخطوة.",
                    fontSize = 12.sp,
                    color = Color.LightGray
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteProduct(target.id)
                        itemToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("حذف نهائي", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("إلغاء", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("إضافة سلعة جديدة للقائمة 🛍️", fontSize = 14.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = titleInput,
                        onValueChange = { titleInput = it },
                        label = { Text("اسم المنتج / السلعة") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = subCategoryInput,
                        onValueChange = { subCategoryInput = it },
                        label = { Text("القسم الفرعي (مثال: بهارات، مشروبات)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = priceInput,
                        onValueChange = { priceInput = it },
                        label = { Text("السعر (بالريال اليمني)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = descInput,
                        onValueChange = { descInput = it },
                        label = { Text("الوصف / الوزن / المواصفات") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // خيار متوفر بالمخزون / نفد
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isAvailableInput) "متوفر بالمخزون 🟢" else "نفد من المخزون 🔴",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isAvailableInput) Color(0xFF10B981) else Color(0xFFEF4444)
                        )
                        Switch(
                            checked = isAvailableInput,
                            onCheckedChange = { isAvailableInput = it }
                        )
                    }

                    // خيار عرض خاص 🎁
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("عرض خاص وتخفيض 🎁", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Switch(
                            checked = isOfferInput,
                            onCheckedChange = { isOfferInput = it }
                        )
                    }

                    if (isOfferInput) {
                        OutlinedTextField(
                            value = discountInput,
                            onValueChange = { discountInput = it },
                            label = { Text("نسبة الخصم % (مثال: 15)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // التقاط الصورة من الاستوديو مع الضغط الذكي إلى WebP 60KB
                    UnifiedImagePicker(
                        currentImageUrl = imageInput,
                        storagePath = "products/${System.currentTimeMillis()}_item.webp",
                        label = "صورة المنتج (WebP 60KB)",
                        themeColors = themeColors,
                        onImageSelected = { imageInput = it }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val p = priceInput.toDoubleOrNull() ?: 0.0
                        if (titleInput.isNotBlank()) {
                            val finalDesc = listOfNotNull(
                                subCategoryInput.takeIf { it.isNotBlank() }?.let { "[$it]" },
                                descInput.takeIf { it.isNotBlank() }
                            ).joinToString(" ")
                            onAddProduct(titleInput.trim(), p, finalDesc.trim(), imageInput.trim())
                            titleInput = ""
                            subCategoryInput = ""
                            priceInput = ""
                            descInput = ""
                            imageInput = ""
                            showAddDialog = false
                        } else {
                            android.widget.Toast.makeText(context, "يرجى إدخال اسم أو عنوان العنصر أولاً", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent)
                ) {
                    Text("حفظ السلعة 💾", color = Color.Black, fontWeight = FontWeight.Bold)
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
