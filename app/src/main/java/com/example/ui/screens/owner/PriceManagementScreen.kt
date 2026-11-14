package com.example.ui.screens.owner

import android.widget.Toast
import com.example.ui.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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

@Composable
fun PriceManagementScreen(
    account: UnifiedBusinessAccount,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette
) {
    val context = LocalContext.current
    val productsList by viewModel.products.collectAsState()

    var selectedCategoryFilter by remember { mutableStateOf("الكل") }

    val myProducts = remember(productsList, account.id) {
        productsList.filter { it.storeId == account.id }
    }
    val categories = remember(myProducts) {
        listOf("الكل") + myProducts.mapNotNull { it.category.takeIf { c -> c.isNotBlank() } }.distinct()
    }
    val filteredProducts = remember(myProducts, selectedCategoryFilter) {
        if (selectedCategoryFilter == "الكل") myProducts
        else myProducts.filter { it.category == selectedCategoryFilter }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(themeColors.background)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("🏷️ إدارة وتحديث الأسعار السريعة:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)

        if (categories.size > 1) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(categories.size) { idx ->
                    val cat = categories[idx]
                    val isSel = selectedCategoryFilter == cat
                    Surface(
                        color = if (isSel) themeColors.accent else themeColors.surface,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, if (isSel) themeColors.textPrimary else Color.Gray.copy(alpha = 0.3f)),
                        modifier = Modifier.clickable { selectedCategoryFilter = cat }
                    ) {
                        Text(
                            cat,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSel) Color.Black else themeColors.textPrimary
                        )
                    }
                }
            }
        }

        if (filteredProducts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("لا توجد منتجات مسجلة لتعديل أسعارها", color = themeColors.textSecondary, fontSize = 12.sp)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredProducts, key = { it.id }) { product ->
                    PriceItemRow(
                        product = product,
                        themeColors = themeColors,
                        onSavePrice = { newPrice ->
                            if (newPrice <= 0.0) {
                                Toast.makeText(context, "يرجى إدخال سعر صحيح أكبر من صفر", Toast.LENGTH_SHORT).show()
                            } else {
                                val updated = product.copy(price = newPrice)
                                viewModel.saveProduct(updated)
                                Toast.makeText(context, "تم تحديث سعر ${product.name} إلى $newPrice YER", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PriceItemRow(
    product: ProductEntity,
    themeColors: VisualThemePalette,
    onSavePrice: (Double) -> Unit
) {
    var editedPriceText by remember(product.id, product.price) { mutableStateOf(product.price.toString()) }

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
        border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(product.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = themeColors.textPrimary)
                Text("السعر الحالي: ${product.price} YER", fontSize = 10.sp, color = themeColors.textSecondary)
            }

            OutlinedTextField(
                value = editedPriceText,
                onValueChange = { editedPriceText = it },
                modifier = Modifier.width(100.dp),
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = themeColors.accent,
                    unfocusedBorderColor = Color.Gray,
                    focusedTextColor = themeColors.textPrimary,
                    unfocusedTextColor = themeColors.textPrimary
                )
            )

            Button(
                onClick = {
                    val parsed = editedPriceText.trim().toDoubleOrNull() ?: -1.0
                    onSavePrice(parsed)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Text("حفظ 💾", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}
