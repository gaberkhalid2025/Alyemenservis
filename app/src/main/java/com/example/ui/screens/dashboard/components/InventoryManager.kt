package com.example.ui.screens.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.utils.VisualThemePalette

data class InventoryItem(
    val id: String,
    val sku: String,
    val name: String,
    val quantity: Int,
    val minThreshold: Int = 5,
    val price: Double,
    val inStock: Boolean = true
)

/**
 * 📦 InventoryManager (إدارة المخزون وتتبع الكميات والتنبيهات)
 * تتبع الكميات المتوفرة، التنبيه بنفاد المخزون (Low Stock Alert)، ورمز الباركود/SKU.
 */
@Composable
fun InventoryManager(
    themeColors: VisualThemePalette,
    modifier: Modifier = Modifier
) {
    var inventoryList by remember {
        mutableStateOf(
            listOf(
                InventoryItem("1", "SKU-1001", "طقم أدوات صيانة متكامل", 12, 5, 25000.0, true),
                InventoryItem("2", "SKU-1002", "مضخة مياه إيطالية 1 حصان", 3, 5, 85000.0, true),
                InventoryItem("3", "SKU-1003", "مفتاح كهربائي ذكي WiFi", 0, 10, 14000.0, false)
            )
        )
    }

    var showAddItemDialog by remember { mutableStateOf(false) }
    var newItemName by remember { mutableStateOf("") }
    var newItemSku by remember { mutableStateOf("") }
    var newItemQty by remember { mutableStateOf("10") }
    var newItemPrice by remember { mutableStateOf("15000") }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = Color(0xFF38BDF8))
                    Text(
                        text = "إدارة المخزون والمنتجات",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Button(
                    onClick = { showAddItemDialog = true },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8), contentColor = Color(0xFF0F172A)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("إضافة صنف", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Summary metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val totalItems = inventoryList.sumOf { it.quantity }
                val lowStockCount = inventoryList.count { it.quantity <= it.minThreshold }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF334155),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("إجمالي القطع", fontSize = 11.sp, color = Color(0xFF94A3B8))
                        Text("$totalItems", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (lowStockCount > 0) Color(0xFFEF4444).copy(alpha = 0.2f) else Color(0xFF334155),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("أصناف منخفضة", fontSize = 11.sp, color = if (lowStockCount > 0) Color(0xFFEF4444) else Color(0xFF94A3B8))
                        Text("$lowStockCount", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = if (lowStockCount > 0) Color(0xFFEF4444) else Color.White)
                    }
                }
            }

            // Items List
            inventoryList.forEach { item ->
                val isLowStock = item.quantity <= item.minThreshold
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF334155),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = item.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                if (isLowStock) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFFEF4444).copy(alpha = 0.25f)
                                    ) {
                                        Text(
                                            text = if (item.quantity == 0) "نفد المخزون" else "مخزون منخفض",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFEF4444),
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "رمز الصنف SKU: ${item.sku} • السعر: ${item.price.toInt()} ريال",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(
                                onClick = {
                                    if (item.quantity > 0) {
                                        inventoryList = inventoryList.map {
                                            if (it.id == item.id) it.copy(quantity = it.quantity - 1, inStock = it.quantity - 1 > 0) else it
                                        }
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Decrease", tint = Color.White)
                            }

                            Text(
                                text = "${item.quantity}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isLowStock) Color(0xFFEF4444) else Color(0xFF38BDF8)
                            )

                            IconButton(
                                onClick = {
                                    inventoryList = inventoryList.map {
                                        if (it.id == item.id) it.copy(quantity = it.quantity + 1, inStock = true) else it
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Increase", tint = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddItemDialog) {
        AlertDialog(
            onDismissRequest = { showAddItemDialog = false },
            title = { Text("إضافة صنف جديد للمخزون") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newItemName,
                        onValueChange = { newItemName = it },
                        label = { Text("اسم الصنف أو المنتج") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newItemSku,
                        onValueChange = { newItemSku = it },
                        label = { Text("رمز التخزين SKU / الباركود") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newItemQty,
                        onValueChange = { newItemQty = it },
                        label = { Text("الكمية المتوفرة") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newItemPrice,
                        onValueChange = { newItemPrice = it },
                        label = { Text("السعر الفردي (ريال)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newItemName.isNotBlank()) {
                            val newItem = InventoryItem(
                                id = System.currentTimeMillis().toString(),
                                sku = if (newItemSku.isBlank()) "SKU-${System.currentTimeMillis() % 10000}" else newItemSku,
                                name = newItemName,
                                quantity = newItemQty.toIntOrNull() ?: 1,
                                minThreshold = 5,
                                price = newItemPrice.toDoubleOrNull() ?: 0.0,
                                inStock = (newItemQty.toIntOrNull() ?: 1) > 0
                            )
                            inventoryList = inventoryList + newItem
                            showAddItemDialog = false
                            newItemName = ""
                            newItemSku = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8), contentColor = Color(0xFF0F172A))
                ) {
                    Text("إضافة")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddItemDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}
