package com.example.ui.screens.dashboard

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ProductEntity
import com.example.data.UnifiedBusinessAccount
import com.example.ui.MainViewModel
import com.example.utils.VisualThemePalette

/**
 * 🛒 Modular Tab: Products, Services, & Medical Procedures Management
 */
@Composable
fun TabProductsServices(
    account: UnifiedBusinessAccount,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette
) {
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    var serviceToEdit by remember { mutableStateOf<ProductEntity?>(null) }
    var prodName by remember { mutableStateOf("") }
    var prodPrice by remember { mutableStateOf("") }
    var prodDesc by remember { mutableStateOf("") }
    var prodImage by remember { mutableStateOf("") }

    val allProducts by viewModel.products.collectAsState()
    val myProducts = remember(allProducts, account.id) {
        allProducts.filter { (it.storeId == account.id || it.storeId == account.phone) && !it.isDeleted }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🛒 المنتجات والخدمات المعروضة (${myProducts.size})",
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = themeColors.accent
            )
            Button(
                onClick = {
                    serviceToEdit = null
                    prodName = ""
                    prodPrice = ""
                    prodDesc = ""
                    prodImage = ""
                    showAddDialog = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("إضافة عنصر ➕", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }

        if (myProducts.isEmpty()) {
            UnifiedEmptyState(
                icon = "🛒",
                title = "لا توجد عناصر معروضة حالياً",
                description = "اضغط على زر (إضافة عنصر) للبدء في عرض خدماتك أو منتجاتك بأسعار مخصصة للعملاء.",
                themeColors = themeColors,
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(myProducts) { prod ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(prod.name, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(prod.description.ifBlank { "لا يوجد وصف" }, fontSize = 10.5.sp, color = Color.Gray, maxLines = 1)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${prod.price.toInt()} ر.ي",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = themeColors.accent
                                )
                            }
                            
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                IconButton(
                                    onClick = {
                                        serviceToEdit = prod
                                        prodName = prod.name
                                        prodPrice = prod.price.toString()
                                        prodDesc = prod.description
                                        prodImage = prod.imageUrl
                                        showAddDialog = true
                                    }
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = Color.LightGray)
                                }
                                IconButton(
                                    onClick = {
                                        viewModel.deleteProduct(prod.id)
                                        Toast.makeText(context, "🗑️ تم حذف العنصر بنجاح!", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color(0xFFEF5350))
                                }
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
            title = {
                Text(
                    text = if (serviceToEdit == null) "إضافة عنصر جديد 🛒" else "تعديل بيانات العنصر 📝",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    UnifiedImagePicker(
                        label = "📸 صورة العنصر",
                        imageUrl = prodImage,
                        onImageSelected = { uri -> prodImage = uri.toString() },
                        themeColors = themeColors
                    )

                    OutlinedTextField(
                        value = prodName,
                        onValueChange = { prodName = it },
                        label = { Text("الاسم / المسمى التجاري", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = themeColors.accent)
                    )

                    OutlinedTextField(
                        value = prodPrice,
                        onValueChange = { prodPrice = it },
                        label = { Text("السعر بالريال اليمني YER", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = themeColors.accent)
                    )

                    OutlinedTextField(
                        value = prodDesc,
                        onValueChange = { prodDesc = it },
                        label = { Text("الوصف والتفاصيل الإضافية", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = themeColors.accent),
                        minLines = 2
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (prodName.isNotBlank() && prodPrice.isNotBlank()) {
                            val priceVal = prodPrice.toDoubleOrNull() ?: 0.0
                            val targetId = serviceToEdit?.id ?: ""
                            val prodToSave = ProductEntity(
                                id = targetId,
                                name = prodName,
                                price = priceVal,
                                description = prodDesc,
                                imageUrl = prodImage,
                                storeId = account.id,
                                isDeleted = false
                            )
                            viewModel.saveProduct(prodToSave)
                            showAddDialog = false
                        } else {
                            Toast.makeText(context, "⚠️ يرجى إدخال المسمى والسعر بشكل صحيح!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent)
                ) {
                    Text("حفظ البيانات 💾", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("إلغاء", fontSize = 11.sp, color = Color.LightGray)
                }
            }
        )
    }
}
