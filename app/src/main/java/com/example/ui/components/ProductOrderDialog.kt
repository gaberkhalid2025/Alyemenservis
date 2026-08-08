package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.*
import com.example.utils.VisualThemePalette

@Composable
fun StoreProductOrderDialog(
    product: ProductEntity,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onDismiss: () -> Unit
) {
    val currentUserId by viewModel.currentUserId.collectAsState()
    val currentUserName by viewModel.currentUserName.collectAsState()
    val currentUserPhone by viewModel.currentUserPhone.collectAsState()
    val paymentWallets by viewModel.paymentWallets.collectAsState()

    var customerName by remember { mutableStateOf(currentUserName) }
    var customerPhone by remember { mutableStateOf(currentUserPhone) }
    var customerArea by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf(1) }
    var notes by remember { mutableStateOf("") }

    val activeWallets = remember(paymentWallets) { paymentWallets.filter { it.status == "active" } }
    var selectedWallet by remember { mutableStateOf<PaymentWalletEntity?>(null) }
    var transferIdInput by remember { mutableStateOf("") }
    var transferPhotoInput by remember { mutableStateOf("") }

    val totalAmount = product.price * quantity

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.background),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .border(2.dp, themeColors.accent, RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🛒 طلب شراء السلعة ودفعها إلكترونياً", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, null, tint = Color.Red)
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = themeColors.surface)
                ) {
                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("📦", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(product.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("السعر الفردي: ${product.price} YER", fontSize = 11.sp, color = themeColors.accent)
                        }
                    }
                }

                OutlinedTextField(
                    value = customerName,
                    onValueChange = { customerName = it },
                    label = { Text("الاسم الكامل للمشتري", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth().testTag("order_name_field"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                OutlinedTextField(
                    value = customerPhone,
                    onValueChange = { customerPhone = it },
                    label = { Text("رقم هاتف المشتري للتواصل والتأكيد", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                OutlinedTextField(
                    value = customerArea,
                    onValueChange = { customerArea = it },
                    label = { Text("حي التوصيل أو المنطقة بالكامل بالتفصيل", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("الكمية المطلوبة للطلب:", fontSize = 11.sp, color = Color.White)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = { if (quantity > 1) quantity-- },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.surface),
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.size(36.dp)
                        ) { Text("-", color = Color.White) }
                        Text("$quantity", modifier = Modifier.padding(horizontal = 14.dp), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Button(
                            onClick = { quantity++ },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.surface),
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.size(36.dp)
                        ) { Text("+", color = Color.White) }
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = themeColors.surface)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("إجمالي المبلغ المستحق:", fontSize = 11.sp, color = Color.White)
                        Text("$totalAmount YER", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    }
                }

                // Payment Wallets Selection (Yemeni Methods)
                Text("🏦 اختر وسيلة الدفع الإلكترونية اليمنية المتاحة:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                if (activeWallets.isEmpty()) {
                    Text("لا يوجد محافظ دفع مفعلة حالياً من قبل الأدمن.", color = Color.Red, fontSize = 10.sp)
                } else {
                    activeWallets.forEach { wallet ->
                        val isSelected = selectedWallet?.id == wallet.id
                        val walletIcon = when (wallet.provider) {
                            "jeep" -> "📱 محفظة جيب"
                            "jawali" -> "📲 محفظة جوالي"
                            "kuraimi" -> "🏦 الكريمي موني"
                            else -> "💳 تحويل مباشر"
                        }
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) themeColors.accent.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedWallet = wallet }
                                .border(
                                    1.dp,
                                    if (isSelected) themeColors.accent else Color.Gray.copy(alpha = 0.2f),
                                    RoundedCornerShape(8.dp)
                                )
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(walletIcon, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("اسم الحساب المالي: ${wallet.accountName}", fontSize = 10.sp, color = themeColors.textSecondary)
                                Text("رقم الحساب / رقم المحفظة: ${wallet.walletNumber}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                            }
                        }
                    }
                }

                if (selectedWallet != null) {
                    OutlinedTextField(
                        value = transferIdInput,
                        onValueChange = { transferIdInput = it },
                        label = { Text("رقم مرجع الحوالة / العملية المالية الموثق", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth().testTag("transfer_id_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    OutlinedTextField(
                        value = transferPhotoInput,
                        onValueChange = { transferPhotoInput = it },
                        label = { Text("أدخل رابط إثبات التحويل أو السند المالي", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }

                Button(
                    onClick = {
                        if (customerName.isNotEmpty() && customerPhone.isNotEmpty() && customerArea.isNotEmpty()) {
                            val newOrder = OrderEntity(
                                id = "",
                                storeId = product.storeId,
                                productId = product.id,
                                productName = product.name,
                                customerPhone = customerPhone,
                                customerName = customerName,
                                customerArea = customerArea,
                                price = product.price,
                                quantity = quantity,
                                totalAmount = totalAmount,
                                paymentId = transferIdInput,
                                paymentStatus = if (selectedWallet != null) "PROCESSING" else "PENDING",
                                status = "PENDING"
                            )
                            viewModel.placeOrder(newOrder)
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("تأكيد وحفظ طلب الشراء الفوري", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
