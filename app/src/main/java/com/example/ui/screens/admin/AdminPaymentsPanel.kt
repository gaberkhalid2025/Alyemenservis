package com.example.ui.screens.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.MainViewModel
import com.example.utils.VisualThemePalette
import com.example.util.PermissionGuard
import com.example.util.RoleManager
import com.example.util.UserRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPaymentsPanel(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    state: AdminPanelState
) {
    val currentRole = RoleManager.fromRoleString(viewModel.adminRole.value)
    if (!PermissionGuard.hasPermission(currentRole, "MANAGE_SETTINGS")) { // Payments can share MANAGE_SETTINGS permissions
        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Red, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("🔒 ليس لديك صلاحية للوصول إلى هذه اللوحة", color = Color.White, fontSize = 14.sp)
                Text("يرجى التواصل مع المالك أو المدير الرئيسي", color = Color.Gray, fontSize = 12.sp)
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("💳 نظام الدفع والتحقق والمحافظ الإلكترونية والإيرادات", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
        
        val paymentsList by viewModel.payments.collectAsState()
        var searchQuery by remember { mutableStateOf("") }
        var statusFilter by remember { mutableStateOf("ALL") }
        var selectedPaymentForDetails by remember { mutableStateOf<PaymentEntity?>(null) }
        
        // Stats Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val stats = listOf(
                Triple("اليوم", "YER 150,000", Color.Green),
                Triple("الأسبوع", "YER 980,000", Color.Yellow),
                Triple("الشهر", "YER 3,450,000", themeColors.accent)
            )
            stats.forEach { (title, amt, col) ->
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = themeColors.surface)
                ) {
                    Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(title, fontSize = 9.sp, color = Color.LightGray)
                        Text(amt, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = col)
                    }
                }
            }
        }

        // Revenue by entity Type
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = themeColors.surface.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("📊 تفصيل إيرادات مزودي الخدمات:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("🔧 إيرادات الفنيين:", fontSize = 10.sp, color = Color.LightGray)
                    Text("YER 1,850,000", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("🏪 إيرادات المحلات والمتاجر:", fontSize = 10.sp, color = Color.LightGray)
                    Text("YER 1,600,000", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                }
            }
        }

        // Actions Bar (Export)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Button(
                onClick = { viewModel.triggerNotification("📥 تم تصدير سجل المدفوعات بصيغة PDF بنجاح!") },
                colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                Text("تصدير PDF 📄", fontSize = 10.sp, color = Color.White)
            }
            Button(
                onClick = { viewModel.triggerNotification("📥 تم تصدير سجل المدفوعات بصيغة CSV بنجاح!") },
                colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                Text("تصدير CSV 📊", fontSize = 10.sp, color = Color.White)
            }
        }

        // Settings for Booking Payment and "Order service now"
        var showPaymentSettings by remember { mutableStateOf(false) }
        Button(
            onClick = { showPaymentSettings = !showPaymentSettings },
            colors = ButtonDefaults.buttonColors(containerColor = themeColors.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (showPaymentSettings) "🙈 إخفاء خيارات ربط الاستمارات بالدفع" else "👁️ إظهار خيارات ربط الاستمارات بنظام الدفع 💳", fontSize = 11.sp, color = themeColors.accent)
        }

        if (showPaymentSettings) {
            var bookingPaymentEnabled by remember { mutableStateOf(true) }
            var bookingAdvancePct by remember { mutableStateOf("20") }
            var bookingMinAdvance by remember { mutableStateOf("1000") }
            var bookingMaxAdvance by remember { mutableStateOf("5000") }
            var bookingForced by remember { mutableStateOf(true) }

            var servicePaymentEnabled by remember { mutableStateOf(false) }
            var serviceAdvancePct by remember { mutableStateOf("10") }
            var serviceForced by remember { mutableStateOf(false) }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("📅 صلاحية ربط استمارة الحجز بنظام الدفع:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = themeColors.accent)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("تفعيل الدفع للحجز", fontSize = 10.sp, color = Color.White)
                        Switch(checked = bookingPaymentEnabled, onCheckedChange = { bookingPaymentEnabled = it })
                    }
                    if (bookingPaymentEnabled) {
                        OutlinedTextField(
                            value = bookingAdvancePct,
                            onValueChange = { bookingAdvancePct = it },
                            label = { Text("نسبة الدفع المقدم (%)", fontSize = 9.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = Color.White)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(
                                value = bookingMinAdvance,
                                onValueChange = { bookingMinAdvance = it },
                                label = { Text("الحد الأدنى للمقدم", fontSize = 9.sp) },
                                modifier = Modifier.weight(1f),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = Color.White)
                            )
                            OutlinedTextField(
                                value = bookingMaxAdvance,
                                onValueChange = { bookingMaxAdvance = it },
                                label = { Text("الحد الأقصى للمقدم", fontSize = 9.sp) },
                                modifier = Modifier.weight(1f),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = Color.White)
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("إجبارية الدفع للاعتماد (Switch)", fontSize = 10.sp, color = Color.White)
                            Switch(checked = bookingForced, onCheckedChange = { bookingForced = it })
                        }
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))

                    Text("⚡ صلاحية ربط استمارة \"اطلب خدمتك الآن\" بالدفع:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = themeColors.accent)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("تفعيل الدفع للطلب الفوري", fontSize = 10.sp, color = Color.White)
                        Switch(checked = servicePaymentEnabled, onCheckedChange = { servicePaymentEnabled = it })
                    }
                    if (servicePaymentEnabled) {
                        OutlinedTextField(
                            value = serviceAdvancePct,
                            onValueChange = { serviceAdvancePct = it },
                            label = { Text("نسبة الدفع المقدم (%)", fontSize = 9.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = Color.White)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("إجبارية الدفع", fontSize = 10.sp, color = Color.White)
                            Switch(checked = serviceForced, onCheckedChange = { serviceForced = it })
                        }
                    }

                    Button(
                        onClick = { viewModel.triggerNotification("💾 تم حفظ إعدادات دفع الاستمارات بنجاح!") },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("حفظ إعدادات الدفع 💾", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Add and Manage Electronic Wallets
        var showAddWalletForm by remember { mutableStateOf(false) }
        var walletProvider by remember { mutableStateOf("جيب") }
        var walletNumber by remember { mutableStateOf("") }
        var walletAccountName by remember { mutableStateOf("") }
        var walletDescription by remember { mutableStateOf("") }
        val walletType by remember { mutableStateOf("كليهما") }
        var walletCurrency by remember { mutableStateOf("YER") }

        val customWallets = remember {
            mutableStateListOf(
                PaymentWalletEntity(id = "w1", provider = "الكريمي", walletNumber = "123456", accountName = "حساب الإدارة الرئيسي", isDefault = true, status = "active"),
                PaymentWalletEntity(id = "w2", provider = "جيب", walletNumber = "777888999", accountName = "محفظة جيب الإدارية", isDefault = false, status = "active")
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = themeColors.surface)
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("📱 المحافظ الإلكترونية المتاحة للتطبيق (${customWallets.size}):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Button(
                        onClick = { showAddWalletForm = !showAddWalletForm },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(if (showAddWalletForm) "إغلاق" else "إضافة محفظة ➕", fontSize = 9.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }

                if (showAddWalletForm) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(value = walletProvider, onValueChange = { walletProvider = it }, label = { Text("المزود (جيب، الكريمي، جوالي، تحويل بنكي)") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = walletNumber, onValueChange = { walletNumber = it }, label = { Text("رقم المحفظة / رقم الحساب") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = walletAccountName, onValueChange = { walletAccountName = it }, label = { Text("اسم الحساب") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = walletDescription, onValueChange = { walletDescription = it }, label = { Text("الوصف") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = walletCurrency, onValueChange = { walletCurrency = it }, label = { Text("العملة (YER, USD, SAR)") }, modifier = Modifier.fillMaxWidth())
                        
                        Button(
                            onClick = {
                                if (walletNumber.isNotBlank() && walletAccountName.isNotBlank()) {
                                    customWallets.add(
                                        PaymentWalletEntity(
                                            id = "w_" + System.currentTimeMillis().toString().takeLast(4),
                                            provider = walletProvider,
                                            walletNumber = walletNumber,
                                            accountName = walletAccountName,
                                            status = "active",
                                            currency = walletCurrency,
                                            walletType = walletType,
                                            description = walletDescription
                                        )
                                    )
                                    walletNumber = ""
                                    walletAccountName = ""
                                    walletDescription = ""
                                    showAddWalletForm = false
                                    viewModel.triggerNotification("✅ تم إضافة المحفظة بنجاح")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("حفظ المحفظة الجديدة 💾", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                customWallets.forEach { wallet ->
                    val isWalletActive = wallet.status == "active"
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))
                    ) {
                        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(wallet.provider, fontWeight = FontWeight.Bold, color = themeColors.accent, fontSize = 11.sp)
                                    if (wallet.isDefault) {
                                        SuggestionChip(
                                            onClick = {},
                                            label = { Text("افتراضية ⭐", fontSize = 8.sp, color = Color.Black) },
                                            colors = SuggestionChipDefaults.suggestionChipColors(containerColor = Color.Yellow)
                                        )
                                    }
                                    SuggestionChip(
                                        onClick = {},
                                        label = { Text(if (isWalletActive) "نشط" else "معطل", fontSize = 8.sp, color = Color.Black) },
                                        colors = SuggestionChipDefaults.suggestionChipColors(containerColor = if (isWalletActive) Color.Green else Color.Red)
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(onClick = {
                                        val idx = customWallets.indexOf(wallet)
                                        if (idx != -1) {
                                            customWallets[idx] = wallet.copy(isDefault = true)
                                            customWallets.forEachIndexed { i, w ->
                                                if (i != idx) customWallets[i] = w.copy(isDefault = false)
                                            }
                                            viewModel.triggerNotification("⭐ تم تعيين المحفظة كافتراضية")
                                        }
                                    }, modifier = Modifier.size(24.dp)) {
                                        Text("⭐", fontSize = 11.sp)
                                    }
                                    IconButton(onClick = {
                                        val idx = customWallets.indexOf(wallet)
                                        if (idx != -1) {
                                            val nextStatus = if (isWalletActive) "inactive" else "active"
                                            customWallets[idx] = wallet.copy(status = nextStatus)
                                        }
                                    }, modifier = Modifier.size(24.dp)) {
                                        Text(if (isWalletActive) "⛔" else "✅", fontSize = 11.sp)
                                    }
                                    IconButton(onClick = { customWallets.remove(wallet) }, modifier = Modifier.size(24.dp)) {
                                        Text("🗑️", fontSize = 11.sp)
                                    }
                                }
                            }
                            Text("رقم الحساب: ${wallet.walletNumber}", fontSize = 10.sp, color = Color.White)
                            Text("الاسم: ${wallet.accountName}", fontSize = 10.sp, color = Color.LightGray)
                        }
                    }
                }
            }
        }

        // Search and Filter Payments List
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("بحث برقم المعاملة، اسم العميل، اسم الفني...", color = Color.Gray, fontSize = 11.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) }
        )

        // Filters
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val filters = listOf("ALL" to "الكل", "PENDING" to "انتظار ⏳", "COMPLETED" to "مكتمل ✅", "REJECTED" to "مرفوض ❌", "REFUNDED" to "مسترد 🔄")
            filters.forEach { (key, label) ->
                val isSel = statusFilter == key
                Button(
                    onClick = { statusFilter = key },
                    colors = ButtonDefaults.buttonColors(containerColor = if (isSel) themeColors.accent else themeColors.surface),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(label, fontSize = 9.sp, color = if (isSel) Color.Black else Color.White)
                }
            }
        }

        // Payments Lazy List Items
        val finalPayments = paymentsList.filter { p ->
            val matchesSearch = p.id.contains(searchQuery, ignoreCase = true) || p.userId.contains(searchQuery) || p.providerId.contains(searchQuery)
            val matchesFilter = statusFilter == "ALL" || p.status == statusFilter
            matchesSearch && matchesFilter
        }

        if (finalPayments.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                Text("لا توجد معاملات مالية مطابقة لفلاتر البحث 💳", color = Color.Gray, fontSize = 11.sp)
            }
        } else {
            finalPayments.forEach { pay ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedPaymentForDetails = pay },
                    colors = CardDefaults.cardColors(containerColor = themeColors.surface)
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("معاملة: #${pay.id.take(8)}", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                            Badge(
                                containerColor = when (pay.status) {
                                    "COMPLETED" -> Color.Green
                                    "PENDING" -> Color.Yellow
                                    "REJECTED" -> Color.Red
                                    else -> Color.Gray
                                }
                            ) {
                                Text(pay.status, color = Color.Black, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                            }
                        }

                        Text("العميل: ${pay.userId.ifBlank { "عميل عام" }}", fontSize = 11.sp, color = Color.LightGray)
                        Text("الفني: ${pay.providerId.ifBlank { "غير محدد" }}", fontSize = 11.sp, color = Color.LightGray)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("المبلغ: ${pay.amount} ${pay.currency}", fontWeight = FontWeight.Bold, color = themeColors.accent, fontSize = 12.sp)
                            Text("التاريخ: ${java.text.SimpleDateFormat("yyyy/MM/dd hh:mm a", java.util.Locale("ar")).format(java.util.Date(pay.createdAt))}", fontSize = 9.sp, color = Color.Gray)
                        }

                        // Action buttons in item
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Button(
                                onClick = {
                                    val updated = pay.copy(status = "COMPLETED", verifiedAt = System.currentTimeMillis())
                                    com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("payments").document(pay.id).set(updated)
                                    viewModel.addNotification("💳 تأكيد الدفع", "تم تأكيد عملية الدفع الخاصة بك بنجاح بمبلغ ${pay.amount} ${pay.currency}", "USER", pay.userId)
                                    viewModel.triggerNotification("✅ تم تأكيد الدفع بنجاح وإرسال الإشعار للعميل")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Green),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(vertical = 2.dp)
                            ) {
                                Text("موافقة ✅", fontSize = 9.sp, color = Color.Black)
                            }

                            var showRejectDialog by remember { mutableStateOf(false) }
                            var rejectReason by remember { mutableStateOf("") }
                            if (showRejectDialog) {
                                AlertDialog(
                                    onDismissRequest = { showRejectDialog = false },
                                    title = { Text("سبب الرفض") },
                                    text = {
                                        OutlinedTextField(value = rejectReason, onValueChange = { rejectReason = it }, label = { Text("أدخل سبب الرفض") })
                                    },
                                    confirmButton = {
                                        Button(onClick = {
                                            val updated = pay.copy(status = "REJECTED", verificationNote = rejectReason)
                                            com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("payments").document(pay.id).set(updated)
                                            viewModel.triggerNotification("❌ تم رفض المعاملة")
                                            showRejectDialog = false
                                        }) { Text("تأكيد الرفض") }
                                    },
                                    containerColor = themeColors.surface
                                )
                            }

                            Button(
                                onClick = { showRejectDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(vertical = 2.dp)
                            ) {
                                Text("رفض ❌", fontSize = 9.sp, color = Color.White)
                            }

                            var showNotesDialog by remember { mutableStateOf(false) }
                            var notesInput by remember { mutableStateOf(pay.adminNote) }
                            if (showNotesDialog) {
                                AlertDialog(
                                    onDismissRequest = { showNotesDialog = false },
                                    title = { Text("ملاحظات إدارية") },
                                    text = {
                                        OutlinedTextField(value = notesInput, onValueChange = { notesInput = it }, label = { Text("ملاحظات الإدارة") })
                                    },
                                    confirmButton = {
                                        Button(onClick = {
                                            val updated = pay.copy(adminNote = notesInput)
                                            com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("payments").document(pay.id).set(updated)
                                            viewModel.triggerNotification("📝 تم حفظ الملاحظة")
                                            showNotesDialog = false
                                        }) { Text("حفظ") }
                                    },
                                    containerColor = themeColors.surface
                                )
                            }

                            Button(
                                onClick = { showNotesDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(vertical = 2.dp)
                            ) {
                                Text("ملاحظات 📝", fontSize = 9.sp, color = Color.White)
                            }

                            Button(
                                onClick = {
                                    viewModel.addNotification("🔔 تذكير بالدفع", "نرجو تزويد الإدارة بإثبات الدفع لاستكمال اعتماد طلبك.", "USER", pay.userId)
                                    viewModel.triggerNotification("🔔 تم إرسال إشعار تذكير للعميل بنجاح")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(vertical = 2.dp)
                            ) {
                                Text("إشعار 🔔", fontSize = 9.sp, color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // Details Dialog on click
        selectedPaymentForDetails?.let { pay ->
            AlertDialog(
                onDismissRequest = { selectedPaymentForDetails = null },
                title = { Text("تفاصيل المعاملة #${pay.id.take(8)}") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("معرف العميل: ${pay.userId}")
                        Text("معرف الفني: ${pay.providerId}")
                        Text("طريقة الدفع: ${pay.method}")
                        Text("المبلغ: ${pay.amount} ${pay.currency}")
                        Text("حالة المعاملة: ${pay.status}")
                        Text("التحويل الإلكتروني: ${pay.walletProvider} - ${pay.walletNumber}")
                        Text("ملاحظة المسؤول: ${pay.verificationNote}")
                        Text("ملاحظات إدارية: ${pay.adminNote}")
                        if (pay.transferPhoto.isNotEmpty()) {
                            Text("📷 صورة إثبات الدفع / سند التحويل موجودة.")
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { selectedPaymentForDetails = null }) {
                        Text("إغلاق")
                    }
                },
                containerColor = themeColors.surface
            )
        }
    }
}
