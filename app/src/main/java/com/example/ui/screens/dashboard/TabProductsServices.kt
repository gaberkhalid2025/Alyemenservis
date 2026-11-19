package com.example.ui.screens.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
    onDeleteProduct: (id: String) -> Unit,
    onUpdateProduct: ((ProductItemEntity) -> Unit)? = null,
    onAddProductRich: ((title: String, priceYer: Double, description: String, imageUrl: String, category: String, isAvailable: Boolean, isOffer: Boolean, discountPercent: Int) -> Unit)? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<ProductItemEntity?>(null) }
    var itemToDelete by remember { mutableStateOf<ProductItemEntity?>(null) }
    var titleInput by remember { mutableStateOf("") }
    var subCategoryInput by remember { mutableStateOf("") }
    var priceInput by remember { mutableStateOf("") }
    var descInput by remember { mutableStateOf("") }
    var imageInput by remember { mutableStateOf("") }
    var isAvailableInput by remember { mutableStateOf(true) }
    var isOfferInput by remember { mutableStateOf(false) }
    var discountInput by remember { mutableStateOf("") }
    var productStorageStamp by remember { mutableStateOf(System.currentTimeMillis()) }

    fun openNewItemDialog() {
        editingItem = null
        titleInput = ""
        subCategoryInput = ""
        priceInput = ""
        descInput = ""
        imageInput = ""
        isAvailableInput = true
        isOfferInput = false
        discountInput = ""
        productStorageStamp = System.currentTimeMillis()
        showAddDialog = true
    }

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
                onClick = { openNewItemDialog() },
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
                onActionClick = { openNewItemDialog() },
                themeColors = themeColors
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(products, key = { it.id }) { item ->
                    val subTitleText = buildString {
                        if (item.category.isNotBlank() && item.category !in listOf("STORE", "RESTAURANT", "MEDICAL", "PROPERTY", "TECHNICIAN")) {
                            append("[${item.category}] ")
                        }
                        append(item.description)
                        if (item.isOffer && item.discountPercent > 0) {
                            append(" • خصم ${item.discountPercent}%")
                        }
                    }.trim()
                    UnifiedProductsServicesSection(
                        title = item.title,
                        description = subTitleText,
                        price = item.priceYer.toInt().toString(),
                        imageUrl = item.imageUrl,
                        isAvailable = item.isAvailable,
                        isOffer = item.isOffer || item.discountPercent > 0,
                        themeColors = themeColors,
                        onEditClick = {
                            editingItem = item
                            titleInput = item.title
                            subCategoryInput = item.category.takeIf { it !in listOf("STORE", "RESTAURANT", "MEDICAL", "PROPERTY", "TECHNICIAN") } ?: ""
                            priceInput = if (item.priceYer > 0.0) item.priceYer.toInt().toString() else ""
                            descInput = item.description
                            imageInput = item.imageUrl
                            isAvailableInput = item.isAvailable
                            isOfferInput = item.isOffer || item.discountPercent > 0
                            discountInput = if (item.discountPercent > 0) item.discountPercent.toString() else ""
                            productStorageStamp = System.currentTimeMillis()
                            showAddDialog = true
                        },
                        onDeleteClick = { itemToDelete = item },
                        onToggleAvailability = { newStatus ->
                            if (onUpdateProduct != null) {
                                onUpdateProduct(item.copy(isAvailable = newStatus))
                            } else {
                                android.widget.Toast.makeText(
                                    context,
                                    if (newStatus) "تم التعيين: متوفر ✓" else "تم التعيين: غير متوفر ✕",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        onQuickPriceUpdate = { newPrice ->
                            if (newPrice <= 0.0) {
                                android.widget.Toast.makeText(context, "يرجى إدخال سعر صحيح أكبر من صفر", android.widget.Toast.LENGTH_SHORT).show()
                            } else if (onUpdateProduct != null) {
                                onUpdateProduct(item.copy(priceYer = newPrice))
                            } else {
                                onAddProduct(item.title, newPrice, item.description, item.imageUrl)
                            }
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
            title = { Text("تأكيد الحذف 🗑️", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.textPrimary) },
            text = {
                Text(
                    "هل أنت متأكد من رغبتك في حذف '${target.title}'؟ لن تتمكن من التراجع عن هذه الخطوة.",
                    fontSize = 12.sp,
                    color = themeColors.textSecondary
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
                    Text("إلغاء", color = themeColors.textSecondary)
                }
            },
            containerColor = themeColors.surface
        )
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                editingItem = null
            },
            title = {
                Text(
                    if (editingItem == null) "إضافة عنصر جديد للقائمة 🛍️" else "تعديل بيانات: ${editingItem?.title} ✏️",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = themeColors.textPrimary
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(
                        value = titleInput,
                        onValueChange = { titleInput = it },
                        label = { Text("اسم المنتج / الخدمة") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = subCategoryInput,
                        onValueChange = { subCategoryInput = it },
                        label = { Text("القسم الفرعي (مثال: وجبات، صيانة، إلكترونيات)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = priceInput,
                        onValueChange = { priceInput = it },
                        label = { Text("السعر (بالريال اليمني)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = descInput,
                        onValueChange = { descInput = it },
                        label = { Text("الوصف / المواصفات / المدة") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isAvailableInput) "متوفر حالياً 🟢" else "غير متوفر / نفد 🔴",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isAvailableInput) Color(0xFF10B981) else Color(0xFFEF4444)
                        )
                        Switch(
                            checked = isAvailableInput,
                            onCheckedChange = { isAvailableInput = it }
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("عرض خاص وتخفيض 🎁", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = themeColors.textPrimary)
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
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    UnifiedImagePicker(
                        currentImageUrl = imageInput,
                        storagePath = "products/${productStorageStamp}_item.webp",
                        label = "صورة العنصر (WebP 60KB)",
                        themeColors = themeColors,
                        onImageSelected = { imageInput = it }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val p = priceInput.trim().toDoubleOrNull()
                        val disc = if (isOfferInput) discountInput.trim().toIntOrNull() else 0
                        if (titleInput.isBlank()) {
                            android.widget.Toast.makeText(context, "يرجى إدخال اسم أو عنوان العنصر أولاً", android.widget.Toast.LENGTH_SHORT).show()
                        } else if (p == null || p <= 0.0) {
                            android.widget.Toast.makeText(context, "يرجى إدخال سعر رقمي صحيح أكبر من الصفر", android.widget.Toast.LENGTH_SHORT).show()
                        } else if (isOfferInput && (disc == null || disc !in 1..99)) {
                            android.widget.Toast.makeText(context, "نسبة الخصم يجب أن تكون بين 1 و 99%", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            val targetEdit = editingItem
                            val cleanCategory = subCategoryInput.trim()
                            val cleanDesc = descInput.trim()
                            val safeDisc = disc ?: 0
                            if (targetEdit != null && onUpdateProduct != null) {
                                onUpdateProduct(
                                    targetEdit.copy(
                                        title = titleInput.trim(),
                                        priceYer = p,
                                        description = cleanDesc,
                                        category = cleanCategory.ifBlank { targetEdit.category },
                                        imageUrl = imageInput.trim(),
                                        isAvailable = isAvailableInput,
                                        isOffer = isOfferInput,
                                        discountPercent = safeDisc
                                    )
                                )
                            } else if (onAddProductRich != null) {
                                onAddProductRich(
                                    titleInput.trim(),
                                    p,
                                    cleanDesc,
                                    imageInput.trim(),
                                    cleanCategory,
                                    isAvailableInput,
                                    isOfferInput,
                                    safeDisc
                                )
                            } else {
                                onAddProduct(titleInput.trim(), p, cleanDesc, imageInput.trim())
                            }
                            editingItem = null
                            titleInput = ""
                            subCategoryInput = ""
                            priceInput = ""
                            descInput = ""
                            imageInput = ""
                            isAvailableInput = true
                            isOfferInput = false
                            discountInput = ""
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent)
                ) {
                    Text(if (editingItem == null) "حفظ العنصر 💾" else "حفظ التعديلات 💾", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddDialog = false
                    editingItem = null
                }) {
                    Text("إلغاء", color = themeColors.textSecondary)
                }
            },
            containerColor = themeColors.surface
        )
    }
}
