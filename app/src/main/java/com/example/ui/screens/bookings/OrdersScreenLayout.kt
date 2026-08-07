@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.screens.bookings

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.*
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
import com.example.ui.MainViewModel

@Composable
fun OrdersScreenLayout(viewModel: MainViewModel, themeColors: VisualThemePalette) {
    val orders by viewModel.orders.collectAsState()
    val currentUserPhone by viewModel.currentUserPhone.collectAsState()
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
            .padding(16.dp)
    ) {
        // Top Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(themeColors.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🛍️", fontSize = 18.sp)
                }
                Column {
                    Text(
                        text = "📦 شاشة طلبات الشراء",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "خاصة بالمنتجات والمأكولات فقط",
                        fontSize = 10.sp,
                        color = Color.LightGray
                    )
                }
            }

            if (myOrders.isNotEmpty()) {
                Button(
                    onClick = { showDeleteAllConfirm = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "حذف الكل", tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("حذف الكل 🗑️", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        Divider(color = themeColors.accent.copy(alpha = 0.15f), thickness = 1.dp, modifier = Modifier.padding(bottom = 12.dp))

        // If the user's phone is empty, prompt them to enter it to view their purchases
        if (currentUserPhone.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.2f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🔐 استعراض فوري للمشتريات والطلبات",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeColors.accent,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "يرجى كتابة رقم هاتفك المحمول لعرض وتتبع جميع طلبات الشراء التي قمت بها من المتاجر والمطاعم بشكل آمن وخاص بك:",
                        fontSize = 11.sp,
                        color = Color.LightGray,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )
                    OutlinedTextField(
                        value = customPhoneInput,
                        onValueChange = { customPhoneInput = it },
                        placeholder = { Text("مثال: 777xxxxxx") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = "الهاتف", tint = themeColors.accent) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 13.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = themeColors.accent,
                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
                        ),
                        singleLine = true
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
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(myOrders) { order ->
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
                            
                            Divider(
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
                        text = "لكي تتمكن من حذف هذا الطلب بنفسك بعد إتمامه، يرجى كتابة كود الحذف الآمن الظاهر على بطاقة الطلب وهو ($correctCode):",
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
                        text = "هل أنت متأكد تماماً من رغبتك في مسح وأرشفة جميع طلبات الشراء المسجلة برقم هاتفك ($activePhone) نهائياً من سجلات هاتفك؟",
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
                                Toast.makeText(context, "✅ تم تصفية وحذف سجلات جميع الطلبات بنجاح!", Toast.LENGTH_SHORT).show()
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
