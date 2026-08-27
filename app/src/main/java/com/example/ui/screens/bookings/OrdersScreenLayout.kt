@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.screens.bookings

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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MainViewModel
import com.example.utils.VisualThemePalette

@Composable
fun OrdersScreenLayout(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onRequestQuickService: () -> Unit = {},
    ordersViewModel: OrdersViewModel = viewModel()
) {
    val orders by viewModel.orders.collectAsState()
    val bookings by viewModel.bookings.collectAsState()
    val currentUserPhone by viewModel.currentUserPhone.collectAsState()
    val context = LocalContext.current

    val selectedOrderForDeletion by ordersViewModel.selectedOrderForDeletion.collectAsState()
    val deletionCodeInput by ordersViewModel.deletionCodeInput.collectAsState()
    val showDeleteAllConfirm by ordersViewModel.showDeleteAllConfirm.collectAsState()

    var selectedOrderTypeTab by remember { mutableStateOf("URGENT_SERVICES") }
    var customPhoneInput by remember { mutableStateOf("") }
    val activePhone = remember(currentUserPhone, customPhoneInput) {
        currentUserPhone.ifEmpty { customPhoneInput }
    }

    val myOrders = remember(orders, activePhone) {
        if (activePhone.isBlank()) emptyList()
        else orders.filter { it.customerPhone.trim() == activePhone.trim() }.sortedByDescending { it.timestamp }
    }

    val myUrgentRequests = remember(bookings, activePhone) {
        if (activePhone.isBlank()) emptyList()
        else bookings.filter {
            (it.customerPhone.trim() == activePhone.trim() || activePhone == "ALL") &&
            (it.timeString.contains("المزاد") || it.providerId == "ALL" || it.dateString.contains("عاجل") || it.serviceType.contains("طلب"))
        }.sortedByDescending { it.id }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = themeColors.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(12.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier.size(26.dp).clip(CircleShape).background(themeColors.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) { Text("📦", fontSize = 13.sp) }
                    Column {
                        Text("📦 شاشة طلباتي والشراء والمزاد", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("متابعة طلبات الخدمات العاجلة والمزاد العكسي والمشتريات", fontSize = 8.sp, color = Color.LightGray)
                    }
                }

                if (selectedOrderTypeTab == "PURCHASES" && myOrders.isNotEmpty()) {
                    Button(
                        onClick = { ordersViewModel.setShowDeleteAllConfirm(true) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier.height(26.dp),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "حذف الكل", tint = Color.White, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("حذف الكل 🗑️", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Tab Selector
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = { selectedOrderTypeTab = "URGENT_SERVICES" },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedOrderTypeTab == "URGENT_SERVICES") themeColors.accent else themeColors.surface
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).height(34.dp)
                ) {
                    Text(
                        "⚡ خدمات ومزاد (${myUrgentRequests.size})",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedOrderTypeTab == "URGENT_SERVICES") Color.Black else Color.White
                    )
                }

                Button(
                    onClick = { selectedOrderTypeTab = "PURCHASES" },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedOrderTypeTab == "PURCHASES") themeColors.accent else themeColors.surface
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).height(34.dp)
                ) {
                    Text(
                        "🛍️ مشتريات ومأكولات (${myOrders.size})",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedOrderTypeTab == "PURCHASES") Color.Black else Color.White
                    )
                }
            }

            // Phone Prompt Card
            if (currentUserPhone.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🔐 استعراض فوري لطلباتك ومشترياتك", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                        OutlinedTextField(
                            value = customPhoneInput,
                            onValueChange = { customPhoneInput = it },
                            placeholder = { Text("مثال: 777123456", color = Color.Gray, fontSize = 12.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = themeColors.accent, modifier = Modifier.size(18.dp)) },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }

            if (selectedOrderTypeTab == "URGENT_SERVICES") {
                Button(
                    onClick = onRequestQuickService,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    modifier = Modifier.fillMaxWidth().height(40.dp).padding(bottom = 6.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("⚡ + إضافة طلب خدمة عاجلة جديدة (المزاد العكسي)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                if (myUrgentRequests.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text("لا توجد طلبات خدمات عاجلة مسجلة حالياً ⚡", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Gray)
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(myUrgentRequests) { req ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(0.8.dp, themeColors.accent.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("⚡ طلب #${req.id.takeLast(6)} • ${req.serviceType}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                                        Text(if (req.status == "ACCEPTED" || req.status == "COMPLETED") "مكتمل ✅" else "جاري العروض ⏳", fontSize = 9.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Button(
                                            onClick = {
                                                viewModel.triggerOpenChatForRequest(req.id, req.customerPhone, req.serviceType)
                                                Toast.makeText(context, "💬 جاري فتح المحادثات...", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                            modifier = Modifier.weight(1f).height(28.dp),
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(0.dp)
                                        ) { Text("العروض والمحادثة", fontSize = 9.5.sp, color = Color.White, fontWeight = FontWeight.Bold) }

                                        Button(
                                            onClick = { viewModel.deleteBooking(req.id) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.7f)),
                                            modifier = Modifier.weight(0.5f).height(28.dp),
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(0.dp)
                                        ) { Text("إلغاء 🗑️", fontSize = 9.sp, color = Color.White) }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                if (myOrders.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text("لا توجد طلبات شراء مسجلة 🛍️", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Gray)
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(myOrders) { order ->
                            OrderCardItem(
                                order = order,
                                themeColors = themeColors,
                                onDeleteClick = { ordersViewModel.selectOrderForDeletion(it) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    OrderDeleteDialog(
        order = selectedOrderForDeletion,
        inputCode = deletionCodeInput,
        themeColors = themeColors,
        onInputChange = { ordersViewModel.updateDeletionCodeInput(it) },
        onConfirmDelete = {
            selectedOrderForDeletion?.let { order ->
                ordersViewModel.verifyAndDeleteOrder(
                    order = order,
                    inputCode = deletionCodeInput,
                    mainViewModel = viewModel,
                    onSuccess = { Toast.makeText(context, "✅ تم حذف وإلغاء الطلب بنجاح", Toast.LENGTH_SHORT).show() },
                    onError = { err -> Toast.makeText(context, err, Toast.LENGTH_SHORT).show() }
                )
            }
        },
        onDismiss = { ordersViewModel.selectOrderForDeletion(null) }
    )

    if (showDeleteAllConfirm) {
        OrderDeleteAllDialog(
            activePhone = activePhone,
            themeColors = themeColors,
            onConfirmDeleteAll = {
                ordersViewModel.confirmDeleteAllOrders(activePhone, viewModel) {
                    Toast.makeText(context, "✅ تم حذف جميع الطلبات بنجاح", Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { ordersViewModel.setShowDeleteAllConfirm(false) }
        )
    }
}
