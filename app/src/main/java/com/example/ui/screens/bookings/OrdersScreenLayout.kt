@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.screens.bookings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.OrderEntity
import com.example.data.models.OrderStatus
import com.example.ui.*
import com.example.ui.helpers.AppPreferenceHelper
import com.example.utils.VisualThemePalette

@Composable
fun OrdersScreenLayout(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onRequestQuickService: () -> Unit = {}
) {
    val orders by viewModel.orders.collectAsState()
    val instantRequests by viewModel.instantRequests.collectAsState()
    val currentUserPhone by viewModel.currentUserPhone.collectAsState()
    val currentUserId by viewModel.currentUserId.collectAsState()

    var customPhoneInput by remember { mutableStateOf("") }
    val activePhone = remember(currentUserPhone, customPhoneInput) {
        currentUserPhone.ifEmpty { customPhoneInput }
    }
    val cleanActivePhone = remember(activePhone) {
        AppPreferenceHelper.normalizePhoneNumber(activePhone)
    }

    var selectedTab by remember { mutableIntStateOf(0) }

    val myInstantRequests = remember(instantRequests, cleanActivePhone, currentUserId) {
        instantRequests.filter { req ->
            val reqPhoneClean = AppPreferenceHelper.normalizePhoneNumber(req.userPhone)
            cleanActivePhone.isBlank() || reqPhoneClean == cleanActivePhone || (currentUserId.isNotBlank() && req.userId == currentUserId)
        }.sortedByDescending { it.createdAt }
    }

    val myOrders = remember(orders, cleanActivePhone, activePhone) {
        if (activePhone.isBlank()) {
            orders.sortedByDescending { it.timestamp }
        } else {
            orders.filter {
                val orderPhoneClean = AppPreferenceHelper.normalizePhoneNumber(it.customerPhone)
                (cleanActivePhone.isNotBlank() && orderPhoneClean == cleanActivePhone) || it.customerPhone.trim() == activePhone.trim()
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
                    Text(if (selectedTab == 0) "⚡" else "🛍️", fontSize = 16.sp)
                }
                Column {
                    Text(
                        text = "📋 مركز متابعة طلباتي وضمان الخدمة",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "تتبع الطلبات الفورية العاجلة ومشتريات المتاجر والمطاعم",
                        fontSize = 10.sp,
                        color = Color.LightGray
                    )
                }
            }

            if (selectedTab == 1 && myOrders.isNotEmpty()) {
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

        // Navigation Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = themeColors.surface,
            contentColor = themeColors.accent,
            modifier = Modifier.padding(bottom = 8.dp).clip(RoundedCornerShape(10.dp))
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Text(
                        "⚡ طلبات الخدمات العاجلة (${myInstantRequests.size})",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedTab == 0) themeColors.accent else Color.LightGray
                    )
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Text(
                        "🛍️ مشتريات المتاجر (${myOrders.size})",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedTab == 1) themeColors.accent else Color.LightGray
                    )
                }
            )
        }

        // Prompt for phone entry if empty
        if (currentUserPhone.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.2f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🔐 استعراض فوري لطلباتك",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeColors.accent,
                        textAlign = TextAlign.Center
                    )
                    OutlinedTextField(
                        value = customPhoneInput,
                        onValueChange = { customPhoneInput = it },
                        placeholder = { Text("أدخل رقم هاتفك لتصفح جميع طلباتك (مثال: 777123456)", color = Color.Gray, fontSize = 11.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = "الهاتف", tint = themeColors.accent, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold),
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

        if (selectedTab == 0) {
            if (myInstantRequests.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text("⚡", fontSize = 52.sp)
                        Text(
                            text = if (activePhone.isBlank()) "يرجى كتابة رقم هاتفك أعلى الشاشة لعرض طلباتك" else "لا توجد طلبات خدمات عاجلة مسجلة لهذا الرقم",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "يمكنك إطلاق طلب فوري للحصول على عروض أسعار ومزايدات مباشرة من الفنيين والمزودين خلال دقائق",
                            fontSize = 11.sp,
                            color = Color.LightGray,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = onRequestQuickService,
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent, contentColor = Color.Black),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("⚡ اطلب خدمتك الآن (المزاد العكسي)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(myInstantRequests, key = { it.id }) { req ->
                        InstantRequestOrderCard(req = req, viewModel = viewModel, themeColors = themeColors)
                    }
                }
            }
        } else {
            val newCount = remember(myOrders) { myOrders.count { it.status == OrderStatus.PENDING.code || it.status.isEmpty() } }
            val inProgressCount = remember(myOrders) { myOrders.count { it.status == OrderStatus.PROCESSING.code } }
            val completedCount = remember(myOrders) { myOrders.count { it.status == OrderStatus.COMPLETED.code } }
            val cancelledCount = remember(myOrders) { myOrders.count { it.status == OrderStatus.CANCELLED.code } }

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
                    items(paginatedOrders, key = { it.id }) { order ->
                        StoreOrderCard(
                            order = order,
                            themeColors = themeColors,
                            onRequestDelete = {
                                selectedOrderForDeletion = it
                                deletionCodeInput = ""
                            }
                        )
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
    }

    val orderToDelete = selectedOrderForDeletion
    if (orderToDelete != null) {
        OrderDeletionDialog(
            order = orderToDelete,
            deletionCodeInput = deletionCodeInput,
            onDeletionCodeChange = { deletionCodeInput = it },
            themeColors = themeColors,
            onConfirmDelete = { orderId ->
                viewModel.deleteOrder(orderId)
                selectedOrderForDeletion = null
            },
            onDismiss = { selectedOrderForDeletion = null }
        )
    }

    if (showDeleteAllConfirm) {
        DeleteAllOrdersDialog(
            activePhone = activePhone,
            themeColors = themeColors,
            onConfirmDeleteAll = {
                viewModel.deleteAllOrders(activePhone)
                showDeleteAllConfirm = false
            },
            onDismiss = { showDeleteAllConfirm = false }
        )
    }
}
