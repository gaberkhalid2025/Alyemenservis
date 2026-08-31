@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.screens.bookings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.*
import com.example.viewmodels.AuthViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.*
import com.example.utils.*


@Composable
fun OrdersScreenLayout(authViewModel: AuthViewModel = viewModel(), themeColors: VisualThemePalette, onRequestQuickService: () -> Unit = {}) {
    val orders by viewModel.orders.collectAsState()
    val currentUserPhone by authViewModel.currentUserPhone.collectAsState()
    val context = LocalContext.current

    // Allow user to query by phone if currentUserPhone is empty
    var customPhoneInput by remember { mutableStateOf("") }
    val activePhone = remember(currentUserPhone, customPhoneInput) {
        currentUserPhone.ifEmpty { customPhoneInput }
    }

    val myOrders = remember(orders, activePhone) {
        if (activePhone.isBlank()) {
            emptyList()
        } else {
            orders.filter { 
                it.customerPhone.trim() == activePhone.trim()
            }.sortedByDescending { it.timestamp }
        }
    }

    var selectedOrderForDeletion by remember { mutableStateOf<OrderEntity?>(null) }
    var deletionCodeInput by remember { mutableStateOf("") }
    var showDeleteAllConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(themeColors.background)
            .padding(12.dp)
    ) {
        // Top Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(themeColors.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🛍️", fontSize = 16.sp)
                }
                Column {
                    Text(
                        text = "🛍️ سجل طلبات الشراء والمشتريات",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "متابعة وتتبع طلبات الشراء من المتاجر والمطاعم",
                        fontSize = 10.sp,
                        color = Color.LightGray
                    )
                }
            }

            if (myOrders.isNotEmpty()) {
                Button(
                    onClick = { showDeleteAllConfirm = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "حذف الكل", tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("حذف الكل 🗑️", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Quick Summary Stats
        val newCount = remember(myOrders) { myOrders.count { it.status == "PENDING" || it.status.isEmpty() } }
        val inProgressCount = remember(myOrders) { myOrders.count { it.status == "PROCESSING" } }
        val completedCount = remember(myOrders) { myOrders.count { it.status == "COMPLETED" } }
        val cancelledCount = remember(myOrders) { myOrders.count { it.status == "CANCELLED" } }

        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = themeColors.surface.copy(alpha = 0.8f)),
            border = BorderStroke(0.6.dp, themeColors.accent.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🔴 $newCount", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                    Text("جديدة", fontSize = 9.sp, color = Color.LightGray)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🟡 $inProgressCount", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B))
                    Text("قيد التجهيز", fontSize = 9.sp, color = Color.LightGray)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🟢 $completedCount", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                    Text("مكتملة", fontSize = 9.sp, color = Color.LightGray)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("⚫ $cancelledCount", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Text("ملغية", fontSize = 9.sp, color = Color.LightGray)
                }
            }
        }

        HorizontalDivider(color = themeColors.accent.copy(alpha = 0.15f), thickness = 1.dp, modifier = Modifier.padding(bottom = 6.dp))

        // If the user's phone is empty, prompt them to enter it
        if (currentUserPhone.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.2f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🔐 استعراض فوري لطلبات مشترياتك",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeColors.accent,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "اكتب رقم هاتفك لعرض وتتبع جميع طلبات الشراء من المتاجر والمطاعم:",
                        fontSize = 10.sp,
                        color = Color.LightGray,
                        textAlign = TextAlign.Center,
                        lineHeight = 14.sp
                    )
                    OutlinedTextField(
                        value = customPhoneInput,
                        onValueChange = { customPhoneInput = it },
                        placeholder = { Text("مثال: 777123456", color = Color.Gray, fontSize = 12.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = "الهاتف", tint = themeColors.accent, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = themeColors.accent,
                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                            focusedContainerColor = Color(0xFF1E293B),
                            unfocusedContainerColor = Color(0xFF1E293B)
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
        }

        if (activePhone.isBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🛍️", fontSize = 48.sp)
                    Text("اكتب رقم هاتفك أعلاه لعرض طلباتك", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Gray)
                }
            }
        } else if (myOrders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("📭", fontSize = 48.sp)
                    Text("لا توجد أي طلبات شراء مسجلة لهذا الرقم", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Gray)
                    Text("تأكد من رقم الهاتف أو اطلب سلعاً من المتاجر والمطاعم", fontSize = 11.sp, color = Color.Gray)
                }
            }
        } else {
            var orderPageLimit by remember { mutableIntStateOf(20) }
            val paginatedOrders = remember(myOrders, orderPageLimit) {
                myOrders.take(orderPageLimit)
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(paginatedOrders) { order ->
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
                                            "COMPLETED" -> "مكتمل ومستلم ✅"
                                            "CANCELLED" -> "ملغي ❌"
                                            "PROCESSING" -> "قيد التجهيز والتوصيل 🛵"
                                            else -> "قيد المراجعة ⏳"
                                        },
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (order.status == "PROCESSING" || order.status == "CANCELLED") Color.White else Color.Black,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Product and Order details
                            Text(
                                text = "📦 المنتج: ${order.productName}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "🏪 المتجر: ${order.storeName.ifEmpty { "متجر معتمد" }}",
                                fontSize = 11.sp,
                                color = Color.LightGray
                            )
                            Text(
                                text = "📝 الملاحظات: ${order.notes.ifBlank { "لا توجد" }}",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "الكمية: ${order.quantity} | السعر: ${order.price} ر.ي",
                                    fontSize = 11.sp,
                                    color = Color.LightGray
                                )
                                Text(
                                    text = "💰 الإجمالي: ${order.totalAmount} ر.ي",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = themeColors.accent
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.height(8.dp))

                            // Action buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Deletion Code display
                                Box(
                                    modifier = Modifier
                                        .background(themeColors.primary.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "رمز الحذف: $deleteCode",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = themeColors.accent
                                    )
                                }

                                // Delete Order Button with PIN verification
                                IconButton(
                                    onClick = {
                                        selectedOrderForDeletion = order
                                        deletionCodeInput = ""
                                    },
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

                if (myOrders.size > paginatedOrders.size) {
                    item {
                        Button(
                            onClick = { orderPageLimit += 20 },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.surface)
                        ) {
                            Text("تحميل المزيد من الطلبات (${paginatedOrders.size} من ${myOrders.size})", color = themeColors.accent, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    // 🔒 Confirm Individual Order Deletion Code Dialog
    if (selectedOrderForDeletion != null) {
        val order = selectedOrderForDeletion!!
        val correctCode = (order.id.hashCode().coerceAtLeast(0) % 9000 + 1000).toString()

        Dialog(onDismissRequest = { selectedOrderForDeletion = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(2.dp, themeColors.accent),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🔒 تأكيد رمز الحذف الآمن",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeColors.accent
                    )
                    Text(
                        text = "لحذف هذا الطلب، يرجى كتابة كود الحذف الآمن وهو ($correctCode):",
                        fontSize = 12.sp,
                        color = Color.LightGray,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                    OutlinedTextField(
                        value = deletionCodeInput,
                        onValueChange = { deletionCodeInput = it },
                        placeholder = { Text("اكتب الرمز المكون من 4 أرقام") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 14.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = themeColors.accent
                        ),
                        singleLine = true
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                if (deletionCodeInput.trim() == correctCode) {
                                    viewModel.deleteOrder(order.id)
                                    selectedOrderForDeletion = null
                                    Toast.makeText(context, "✅ تم حذف وإلغاء الطلب بنجاح!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "❌ رمز الحذف غير صحيح! يرجى المحاولة مرة أخرى.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("تأكيد الحذف 🗑️", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        OutlinedButton(
                            onClick = { selectedOrderForDeletion = null },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Text("تراجع", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    // 🗑️ Delete All Confirmation Dialog
    if (showDeleteAllConfirm) {
        Dialog(onDismissRequest = { showDeleteAllConfirm = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(2.dp, Color.Red),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "⚠️ تنبيه هام: حذف جميع الطلبات",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Red
                    )
                    Text(
                        text = "هل أنت متأكد من رغبتك في حذف جميع طلبات الشراء المسجلة برقم هاتفك ($activePhone)؟",
                        fontSize = 12.sp,
                        color = Color.LightGray,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.deleteAllOrders(activePhone)
                                showDeleteAllConfirm = false
                                Toast.makeText(context, "✅ تم حذف جميع الطلبات بنجاح!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("نعم، احذف الكل", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        OutlinedButton(
                            onClick = { showDeleteAllConfirm = false },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Text("تراجع", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
