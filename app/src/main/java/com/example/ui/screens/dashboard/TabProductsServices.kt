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
    var showAddDialog by remember { mutableStateOf(false) }
    var titleInput by remember { mutableStateOf("") }
    var priceInput by remember { mutableStateOf("") }
    var descInput by remember { mutableStateOf("") }
    var imageInput by remember { mutableStateOf("") }

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
                        themeColors = themeColors,
                        onEditClick = { /* Edit */ },
                        onDeleteClick = { onDeleteProduct(item.id) },
                        onToggleAvailability = null
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(addButtonLabel, fontSize = 14.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = titleInput,
                        onValueChange = { titleInput = it },
                        label = { Text("الاسم / العنوان") },
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
                        label = { Text("الوصف / التفاصيل") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    UnifiedImagePicker(
                        currentImageUrl = imageInput,
                        label = "صورة العنصر",
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
                            onAddProduct(titleInput, p, descInput, imageInput)
                            titleInput = ""
                            priceInput = ""
                            descInput = ""
                            imageInput = ""
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent)
                ) {
                    Text("حفظ العنصر", color = Color.Black, fontWeight = FontWeight.Bold)
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
