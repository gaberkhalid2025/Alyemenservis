package com.example.ui.screens.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.MainViewModel
import com.example.utils.VisualThemePalette
import com.example.util.PermissionGuard
import com.example.util.RoleManager
import com.example.util.UserRole
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun AdminBookingsPanel(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    state: AdminPanelState
) {
    val currentRole = RoleManager.fromRoleString(viewModel.adminRole.value)
    if (!PermissionGuard.hasPermission(currentRole, "MANAGE_BOOKINGS")) {
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

    val context = LocalContext.current
    val bookings by viewModel.bookings.collectAsState()
    val providers by viewModel.providers.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val settingsState by viewModel.settings.collectAsState()

    // State variables for filtering and search
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf("ALL") }
    var selectedCategoryIdFilter by remember { mutableStateOf("ALL") }
    var dateFilterText by remember { mutableStateOf("") }

    // Dialog states mapped to state properties
    var selectedBookingForDetails by remember { mutableStateOf<BookingEntity?>(null) }
    val bookingToEdit = state.editingBookingObjState.value
    val bookingToRejectId = state.showRejectionReasonDialogIdState.value
    val rejectionReasonText = state.bookingRejectionReasonInputState.value
    val bookingToRedirect = state.redirectingBookingObjState.value
    val showDeleteConfirmId = state.showDeleteBookingConfirmIdState.value
    
    var cancellationReasonText by remember { mutableStateOf("") }
    var bookingToCancel by remember { mutableStateOf<BookingEntity?>(null) }
    var redirectSearchQuery by remember { mutableStateOf("") }

    // Payment settings toggle state
    var showPaymentSettingsState by remember { mutableStateOf(false) }

    // Local states for editing form fields
    var editClientName by remember { mutableStateOf("") }
    var editClientPhone by remember { mutableStateOf("") }
    var editClientArea by remember { mutableStateOf("") }
    var editServiceType by remember { mutableStateOf("") }
    var editDate by remember { mutableStateOf("") }
    var editTime by remember { mutableStateOf("") }
    var editTotalAmount by remember { mutableStateOf("") }

    // Sync editing fields when bookingToEdit changes
    LaunchedEffect(bookingToEdit) {
        bookingToEdit?.let { b ->
            editClientName = b.clientName.ifEmpty { b.customerName }
            editClientPhone = b.clientPhone.ifEmpty { b.customerPhone }
            editClientArea = b.clientAddress.ifEmpty { b.customerArea }
            editServiceType = b.serviceType
            editDate = b.date.ifEmpty { b.dateString }
            editTime = b.time.ifEmpty { b.timeString }
            editTotalAmount = b.totalAmount.toString()
        }
    }

    // Compute statistics
    val totalCount = bookings.size
    val pendingCount = bookings.count { it.status == "PENDING" }
    val approvedCount = bookings.count { it.status == "APPROVED" }
    val inProgressCount = bookings.count { it.status == "IN_PROGRESS" }
    val completedCount = bookings.count { it.status == "COMPLETED" }
    val cancelledCount = bookings.count { it.status == "CANCELLED" }
    val totalRevenue = bookings.filter { it.status == "COMPLETED" }.sumOf { it.totalAmount }

    // Filtered bookings
    val filteredBookings = remember(bookings, searchQuery, selectedStatusFilter, selectedCategoryIdFilter, dateFilterText) {
        bookings.filter { b ->
            val matchesSearch = b.id.contains(searchQuery, ignoreCase = true) ||
                    b.clientName.contains(searchQuery, ignoreCase = true) ||
                    b.providerName.contains(searchQuery, ignoreCase = true) ||
                    b.customerPhone.contains(searchQuery, ignoreCase = true)
            
            val matchesStatus = selectedStatusFilter == "ALL" || b.status == selectedStatusFilter
            val matchesCategory = selectedCategoryIdFilter == "ALL" || b.category == selectedCategoryIdFilter
            val matchesDate = dateFilterText.isBlank() || b.date.contains(dateFilterText)
            
            matchesSearch && matchesStatus && matchesCategory && matchesDate
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = themeColors.surface)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("📅 الحجوزات والطلبات الميدانية الشاملة", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    Button(
                        onClick = { showPaymentSettingsState = !showPaymentSettingsState },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text(
                            text = if (showPaymentSettingsState) "إخفاء خيارات الدفع 🙈" else "ربط الحجز بالدفع 💳",
                            fontSize = 10.sp,
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text("إدارة وجدولة كافة الحجوزات، والربط الفوري بنظام الدفع والاعتماد والتحويل.", color = Color.LightGray, fontSize = 10.sp)
            }
        }

        // Payment Settings Overlay/Panel
        if (showPaymentSettingsState) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("💳 ربط الحجوزات بنظام المدفوعات والضمان", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    
                    // Enable payment on bookings switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("تفعيل الدفع الإجباري للحجوزات", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            Text("لا يمكن إتمام عملية الحجز والاعتماد دون استكمال تحصيل الرسوم.", fontSize = 9.sp, color = Color.LightGray)
                        }
                        Switch(
                            checked = settingsState.isBookingPaymentRequired,
                            onCheckedChange = { viewModel.saveCustomSettingsState(settingsState.copy(isBookingPaymentRequired = it)) },
                            colors = SwitchDefaults.colors(checkedThumbColor = themeColors.accent)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("مطالبة بدفعة مقدمة تأكيدية (Advance Payment)", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            Text("يفرض على العميل دفع نسبة مئوية مقدمة من السعر التقديري لضمان الجدية.", fontSize = 9.sp, color = Color.LightGray)
                        }
                        Switch(
                            checked = settingsState.requireAdvancePayment,
                            onCheckedChange = { viewModel.saveCustomSettingsState(settingsState.copy(requireAdvancePayment = it)) },
                            colors = SwitchDefaults.colors(checkedThumbColor = themeColors.accent)
                        )
                    }

                    if (settingsState.requireAdvancePayment) {
                        var advPercent by remember(settingsState.advancePaymentPercent) { mutableStateOf(settingsState.advancePaymentPercent.toString()) }
                        var minAdvAmount by remember(settingsState.minAdvanceAmount) { mutableStateOf(settingsState.minAdvanceAmount.toString()) }
                        var maxAdvAmount by remember(settingsState.maxAdvanceAmount) { mutableStateOf(settingsState.maxAdvanceAmount.toString()) }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(
                                value = advPercent,
                                onValueChange = { advPercent = it },
                                label = { Text("نسبة الدفعة %", fontSize = 9.sp) },
                                modifier = Modifier.weight(1f),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = Color.White),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            OutlinedTextField(
                                value = minAdvAmount,
                                onValueChange = { minAdvAmount = it },
                                label = { Text("الحد الأدنى (ريال)", fontSize = 9.sp) },
                                modifier = Modifier.weight(1f),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = Color.White),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            OutlinedTextField(
                                value = maxAdvAmount,
                                onValueChange = { maxAdvAmount = it },
                                label = { Text("الحد الأقصى (ريال)", fontSize = 9.sp) },
                                modifier = Modifier.weight(1f),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = Color.White),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }

                        Button(
                            onClick = {
                                viewModel.saveCustomSettingsState(
                                    settingsState.copy(
                                        advancePaymentPercent = advPercent.toFloatOrNull() ?: 15.0f,
                                        minAdvanceAmount = minAdvAmount.toDoubleOrNull() ?: 500.0,
                                        maxAdvanceAmount = maxAdvAmount.toDoubleOrNull() ?: 10000.0
                                    )
                                )
                                viewModel.triggerNotification("💾 تم حفظ وتثبيت إعدادات الدفعة المقدمة للحجوزات")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("حفظ وتثبيت إعدادات الدفعة 💾", fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }

                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))

                    Text("طريقة تحصيل الحجوزات المعتمدة بالموقع:", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val paymentMethods = listOf("WALLET" to "محفظة جوال", "BANK" to "تحويل بنكي", "CASH" to "كاش عند الخدمة", "CREDIT" to "بطاقات سداد")
                        paymentMethods.forEach { method ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (settingsState.isPaymentEnabled) themeColors.primary.copy(alpha = 0.2f) else Color.DarkGray)
                                    .clickable {
                                        viewModel.saveCustomSettingsState(settingsState.copy(isPaymentEnabled = !settingsState.isPaymentEnabled))
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                  Text(method.second, fontSize = 10.sp, color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // Stats Dashboard Grid
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.25f))
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("📊 لوحة مؤشرات الطلبات والحجوزات ومجمل الإيرادات المكتملة", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Column(modifier = Modifier.weight(1f).background(Color.Black.copy(alpha=0.3f), RoundedCornerShape(6.dp)).padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("الكل", fontSize = 9.sp, color = Color.LightGray)
                        Text("$totalCount", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Column(modifier = Modifier.weight(1f).background(Color.Black.copy(alpha=0.3f), RoundedCornerShape(6.dp)).padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("معلق", fontSize = 9.sp, color = Color.Yellow)
                        Text("$pendingCount", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Yellow)
                    }
                    Column(modifier = Modifier.weight(1f).background(Color.Black.copy(alpha=0.3f), RoundedCornerShape(6.dp)).padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("مقبول", fontSize = 9.sp, color = Color.Green)
                        Text("$approvedCount", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Green)
                    }
                    Column(modifier = Modifier.weight(1f).background(Color.Black.copy(alpha=0.3f), RoundedCornerShape(6.dp)).padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("تنفيذ", fontSize = 9.sp, color = Color.Cyan)
                        Text("$inProgressCount", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Cyan)
                    }
                    Column(modifier = Modifier.weight(1.2f).background(Color.Black.copy(alpha=0.3f), RoundedCornerShape(6.dp)).padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("إيرادات المنجزة", fontSize = 9.sp, color = themeColors.accent)
                        Text("${totalRevenue.toInt()} ريال", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = themeColors.accent, maxLines = 1)
                    }
                }
            }
        }

        // Search, Filters & Export Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("بحث برقم الحجز، العميل، الفني...", fontSize = 10.sp) },
                modifier = Modifier.weight(1f),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = Color.White),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "بحث", tint = Color.LightGray, modifier = Modifier.size(16.dp)) }
            )

            // PDF & CSV Export buttons
            Button(
                onClick = {
                    val csvRows = mutableListOf(listOf("رقم الحجز", "العميل", "الهاتف", "الفني", "الخدمة", "التاريخ", "الحالة", "السعر"))
                    filteredBookings.forEach { b ->
                        csvRows.add(listOf(b.id, b.clientName.ifEmpty { b.customerName }, b.clientPhone.ifEmpty { b.customerPhone }, b.providerName, b.serviceType, b.date, b.status, b.totalAmount.toString()))
                    }
                    com.example.utils.ReportExporter.exportToCSV(context, "bookings_report", csvRows)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text("CSV 📥", fontSize = 10.sp, color = Color.White)
            }

            Button(
                onClick = {
                    val pdfSummary = buildString {
                        append("تقرير إجمالي الحجوزات الميدانية\n")
                        append("عدد الحجوزات: ${filteredBookings.size}\n")
                        append("إيرادات العمليات المنجزة: $totalRevenue ريال يمني\n")
                        append("========================================\n\n")
                        filteredBookings.forEach { b ->
                            append("- حجز #${b.id.takeLast(6)}: العميل: ${b.clientName} | الفني: ${b.providerName} | الخدمة: ${b.serviceType} | السعر: ${b.totalAmount} YER | الحالة: ${b.status}\n")
                        }
                    }
                    com.example.utils.ReportExporter.exportToPDFReport(context, "تقرير_الحجوزات_اليمن", pdfSummary)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text("PDF 📄", fontSize = 10.sp, color = Color.White)
            }
        }

        // Filtering Controls (Category & Status Chips)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val statusFilters = listOf("ALL" to "الكل", "PENDING" to "⏳ معلق", "APPROVED" to "✅ مقبول", "IN_PROGRESS" to "⚡ قيد التنفيذ", "COMPLETED" to "🎉 مكتمل", "CANCELLED" to "❌ ملغي")
            statusFilters.forEach { (stKey, stLbl) ->
                val isSel = selectedStatusFilter == stKey
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSel) themeColors.accent else Color.White.copy(alpha = 0.08f))
                        .clickable { selectedStatusFilter = stKey }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(stLbl, fontSize = 10.sp, color = if (isSel) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Bookings List
        if (filteredBookings.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                Text("لا توجد حجوزات مطابقة لمعايير التصفية والبحث 🔍", color = Color.Gray, fontSize = 11.sp)
            }
        } else {
            filteredBookings.forEach { b ->
                val isPending = b.status == "PENDING"
                val isApproved = b.status == "APPROVED"
                val isInProgress = b.status == "IN_PROGRESS"
                val isCompleted = b.status == "COMPLETED"
                val isCancelled = b.status == "CANCELLED"

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedBookingForDetails = b },
                    colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                    border = BorderStroke(
                        1.dp,
                        when {
                            isPending -> Color.Yellow.copy(alpha = 0.3f)
                            isApproved -> Color.Green.copy(alpha = 0.3f)
                            isInProgress -> Color.Cyan.copy(alpha = 0.3f)
                            isCompleted -> Color.LightGray.copy(alpha = 0.15f)
                            else -> Color.Red.copy(alpha = 0.3f)
                        }
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("رقم الحجز: #${b.id.takeLast(6)}", fontWeight = FontWeight.Bold, color = themeColors.accent, fontSize = 11.sp)
                            Surface(
                                color = when {
                                    isPending -> Color.Yellow
                                    isApproved -> Color.Green
                                    isInProgress -> Color.Cyan
                                    isCompleted -> Color.LightGray
                                    else -> Color.Red
                                },
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = when(b.status) {
                                        "PENDING" -> "⏳ قيد الانتظار"
                                        "APPROVED" -> "✅ مقبول"
                                        "IN_PROGRESS" -> "⚡ قيد التنفيذ"
                                        "COMPLETED" -> "🎉 مكتمل"
                                        "CANCELLED" -> "❌ ملغي"
                                        else -> b.status
                                    },
                                    color = Color.Black,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text("👤 العميل: ${b.clientName.ifEmpty { b.customerName }} (${b.clientPhone.ifEmpty { b.customerPhone }})", fontSize = 11.sp, color = Color.White)
                        Text("🛠️ الفني: ${b.providerName.ifEmpty { "غير محدد" }} | الخدمة: ${b.serviceType}", fontSize = 11.sp, color = Color.LightGray)
                        Text("📅 الموعد: ${b.date.ifEmpty { b.dateString }} الساعة ${b.time.ifEmpty { b.timeString }}", fontSize = 10.sp, color = Color.LightGray)
                        Text("💰 السعر التقديري: ${b.totalAmount} YER", fontSize = 11.sp, color = themeColors.accent, fontWeight = FontWeight.Bold)

                        if (b.rejectionReason.isNotBlank()) {
                            Text("⚠️ سبب الرفض: ${b.rejectionReason}", color = Color.Red, fontSize = 10.sp)
                        }
                        if (!b.cancellationReason.isNullOrBlank()) {
                            Text("⚠️ سبب الإلغاء: ${b.cancellationReason}", color = Color.Red, fontSize = 10.sp)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (isPending) {
                                Button(
                                    onClick = { viewModel.updateBookingStatus(b.id, "APPROVED") },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 1.dp),
                                    modifier = Modifier.height(26.dp),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text("قبول ✅", fontSize = 9.sp, color = Color.White)
                                }
                            }

                            if (isPending || isApproved) {
                                Button(
                                    onClick = {
                                        state.showRejectionReasonDialogIdState.value = b.id
                                        state.bookingRejectionReasonInputState.value = ""
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 1.dp),
                                    modifier = Modifier.height(26.dp),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text("رفض ❌", fontSize = 9.sp, color = Color.White)
                                }
                            }

                            if (isApproved) {
                                Button(
                                    onClick = { viewModel.updateBookingStatus(b.id, "IN_PROGRESS") },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF06B6D4)),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 1.dp),
                                    modifier = Modifier.height(26.dp),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text("بدء تنفيذ ⚡", fontSize = 9.sp, color = Color.White)
                                }
                            }

                            if (isInProgress) {
                                Button(
                                    onClick = { viewModel.updateBookingStatus(b.id, "COMPLETED") },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 1.dp),
                                    modifier = Modifier.height(26.dp),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text("تأكيد إكمال 🎉", fontSize = 9.sp, color = Color.White)
                                }
                            }

                            if (!isCancelled && !isCompleted) {
                                Button(
                                    onClick = {
                                        bookingToCancel = b
                                        cancellationReasonText = ""
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 1.dp),
                                    modifier = Modifier.height(26.dp),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text("إلغاء ❌", fontSize = 9.sp, color = Color.White)
                                }
                            }

                            Button(
                                onClick = {
                                    state.redirectingBookingObjState.value = b
                                    redirectSearchQuery = ""
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 1.dp),
                                modifier = Modifier.height(26.dp),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text("توجيه 🔄", fontSize = 9.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { state.editingBookingObjState.value = b },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 1.dp),
                                modifier = Modifier.height(26.dp),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text("تعديل ✏️", fontSize = 9.sp, color = Color.White)
                            }

                            Button(
                                onClick = { state.showDeleteBookingConfirmIdState.value = b.id },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha=0.8f)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 1.dp),
                                modifier = Modifier.height(26.dp),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text("حذف 🗑️", fontSize = 9.sp, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }

    // --- DIALOGS IMPLEMENTATION ---

    // Detail view dialog
    if (selectedBookingForDetails != null) {
        val b = selectedBookingForDetails!!
        AlertDialog(
            onDismissRequest = { selectedBookingForDetails = null },
            title = { Text("🔎 تفاصيل استمارة الحجز #${b.id.takeLast(6)}") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Text("👤 العميل: ${b.clientName.ifEmpty { b.customerName }}", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                    Text("📞 هاتف العميل: ${b.clientPhone.ifEmpty { b.customerPhone }}", color = Color.LightGray, fontSize = 11.sp)
                    Text("📍 العنوان: ${b.clientAddress.ifEmpty { b.customerArea }}", color = Color.LightGray, fontSize = 11.sp)
                    HorizontalDivider(color = Color.Gray.copy(alpha=0.15f))
                    Text("🛠️ الفني: ${b.providerName}", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                    Text("🏷️ نوع الخدمة: ${b.serviceType}", color = Color.LightGray, fontSize = 11.sp)
                    Text("📅 الموعد: ${b.date.ifEmpty { b.dateString }} الساعة ${b.time.ifEmpty { b.timeString }}", color = Color.LightGray, fontSize = 11.sp)
                    HorizontalDivider(color = Color.Gray.copy(alpha=0.15f))
                    Text("الحالة الحالية: ${b.status}", color = themeColors.accent, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Text("💰 السعر الكلي: ${b.totalAmount} ريال", color = Color.White, fontSize = 11.sp)
                    Text("💳 دفعة مقدمة: ${b.advancePayment} ريال (${b.paymentStatus})", color = Color.LightGray, fontSize = 11.sp)
                    Text("🔑 رمز الإلغاء السري (كلمة مرور الحجز): ${b.bookingPassword.ifEmpty { "غير مولدة" }}", color = Color.Yellow, fontSize = 11.sp)
                    if (b.rejectionReason.isNotBlank()) {
                        Text("❌ مبرر الرفض: ${b.rejectionReason}", color = Color.Red, fontSize = 11.sp)
                    }
                    if (!b.cancellationReason.isNullOrBlank()) {
                        Text("❌ مبرر الإلغاء: ${b.cancellationReason}", color = Color.Red, fontSize = 11.sp)
                    }
                }
            },
            confirmButton = {
                Button(onClick = { selectedBookingForDetails = null }, colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent)) {
                    Text("إغلاق", color = Color.Black)
                }
            },
            containerColor = themeColors.surface
        )
    }

    // Reject dialog
    if (bookingToRejectId != null) {
        AlertDialog(
            onDismissRequest = { state.showRejectionReasonDialogIdState.value = null },
            title = { Text("❌ رفض طلب الحجز #${bookingToRejectId.takeLast(6)}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("يرجى كتابة سبب أو مبرر رفض طلب الحجز لإرساله للعميل:", fontSize = 11.sp, color = Color.LightGray)
                    OutlinedTextField(
                        value = rejectionReasonText,
                        onValueChange = { state.bookingRejectionReasonInputState.value = it },
                        placeholder = { Text("مثال: الفني غير متوفر حالياً بالمنطقة...", fontSize = 10.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = Color.White)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateBookingStatus(bookingToRejectId, "REJECTED", rejectionReasonText.trim())
                        state.showRejectionReasonDialogIdState.value = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("تأكيد الرفض ❌", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { state.showRejectionReasonDialogIdState.value = null }) {
                    Text("إلغاء", color = Color.White)
                }
            },
            containerColor = themeColors.surface
        )
    }

    // Cancel dialog
    if (bookingToCancel != null) {
        val b = bookingToCancel!!
        AlertDialog(
            onDismissRequest = { bookingToCancel = null },
            title = { Text("❌ إلغاء طلب الحجز #${b.id.takeLast(6)}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("يرجى كتابة سبب إلغاء الحجز لتسجيله وإعلام الأطراف:", fontSize = 11.sp, color = Color.LightGray)
                    OutlinedTextField(
                        value = cancellationReasonText,
                        onValueChange = { cancellationReasonText = it },
                        placeholder = { Text("مثال: بطلب من العميل...", fontSize = 10.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = Color.White)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val dbObj = FirebaseFirestore.getInstance()
                        dbObj.collection("bookings").document(b.id).update(
                            "status", "CANCELLED",
                            "cancellationReason", cancellationReasonText.trim(),
                            "cancelledBy", "ADMIN",
                            "cancelledAt", System.currentTimeMillis()
                        ).addOnSuccessListener {
                            viewModel.triggerNotification("❌ تم إلغاء الحجز #${b.id.takeLast(6)} بنجاح")
                        }
                        bookingToCancel = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("تأكيد الإلغاء ❌", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { bookingToCancel = null }) {
                    Text("إلغاء", color = Color.White)
                }
            },
            containerColor = themeColors.surface
        )
    }

    // Redirect / Assign dialog
    if (bookingToRedirect != null) {
        val b = bookingToRedirect!!
        AlertDialog(
            onDismissRequest = { state.redirectingBookingObjState.value = null },
            title = { Text("🔄 إعادة توجيه/تعيين حجز #${b.id.takeLast(6)}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("توجيه الحجز إلى فني محدد، أو المشرف، أو أقرب فني جغرافي:", fontSize = 11.sp, color = Color.LightGray)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Button(
                            onClick = {
                                val dbObj = FirebaseFirestore.getInstance()
                                dbObj.collection("bookings").document(b.id).update(
                                    "providerId", "admin",
                                    "providerName", "الإدارة والدعم الفني 🛡️"
                                ).addOnSuccessListener {
                                    viewModel.triggerNotification("🔄 تم توجيه الحجز إلى الإدارة")
                                }
                                state.redirectingBookingObjState.value = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(2.dp)
                        ) {
                            Text("للإدارة 🛡️", fontSize = 10.sp, color = Color.White)
                        }

                        Button(
                            onClick = {
                                val nearest = providers.filter { it.categoryId == b.category }.minByOrNull { it.rating }
                                if (nearest != null) {
                                    val dbObj = FirebaseFirestore.getInstance()
                                    dbObj.collection("bookings").document(b.id).update(
                                        "providerId", nearest.id,
                                        "providerName", nearest.name
                                    ).addOnSuccessListener {
                                        viewModel.triggerNotification("📍 تم توجيه الحجز لأقرب فني: ${nearest.name}")
                                    }
                                } else {
                                    viewModel.triggerNotification("⚠️ لا يوجد فني متاح في هذا القسم حالياً")
                                }
                                state.redirectingBookingObjState.value = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(2.dp)
                        ) {
                            Text("الأقرب جغرافياً 📍", fontSize = 10.sp, color = Color.Black)
                        }
                    }

                    HorizontalDivider(color = Color.Gray.copy(alpha=0.15f))

                    OutlinedTextField(
                        value = redirectSearchQuery,
                        onValueChange = { redirectSearchQuery = it },
                        placeholder = { Text("ابحث باسم الفني أو تليفونه...", fontSize = 10.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = Color.White)
                    )

                    Box(modifier = Modifier.height(130.dp).fillMaxWidth().verticalScroll(rememberScrollState())) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            providers.filter { 
                                it.name.contains(redirectSearchQuery, ignoreCase=true) || it.phone.contains(redirectSearchQuery)
                            }.take(5).forEach { prov ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                        .clickable {
                                            val dbObj = FirebaseFirestore.getInstance()
                                            dbObj.collection("bookings").document(b.id).update(
                                                "providerId", prov.id,
                                                "providerName", prov.name,
                                                "providerPhone", prov.phone
                                            ).addOnSuccessListener {
                                                viewModel.triggerNotification("🔄 تم توجيه الحجز بنجاح للفني: ${prov.name}")
                                            }
                                            state.redirectingBookingObjState.value = null
                                        }
                                        .padding(6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(prov.name, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    Text(prov.phone, fontSize = 10.sp, color = themeColors.accent)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { state.redirectingBookingObjState.value = null }) {
                    Text("إغلاق", color = Color.White)
                }
            },
            containerColor = themeColors.surface
        )
    }

    // Edit Dialog
    if (bookingToEdit != null) {
        val b = bookingToEdit
        AlertDialog(
            onDismissRequest = { state.editingBookingObjState.value = null },
            title = { Text("✏️ تعديل كافة بيانات الحجز #${b.id.takeLast(6)}") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(
                        value = editClientName,
                        onValueChange = { editClientName = it },
                        label = { Text("اسم العميل", fontSize = 10.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = Color.White)
                    )
                    OutlinedTextField(
                        value = editClientPhone,
                        onValueChange = { editClientPhone = it },
                        label = { Text("رقم هاتف العميل", fontSize = 10.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = Color.White)
                    )
                    OutlinedTextField(
                        value = editClientArea,
                        onValueChange = { editClientArea = it },
                        label = { Text("المنطقة / الحي السكني", fontSize = 10.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = Color.White)
                    )
                    OutlinedTextField(
                        value = editServiceType,
                        onValueChange = { editServiceType = it },
                        label = { Text("الخدمة المطلوبة", fontSize = 10.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = Color.White)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = editDate,
                            onValueChange = { editDate = it },
                            label = { Text("التاريخ", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = Color.White)
                        )
                        OutlinedTextField(
                            value = editTime,
                            onValueChange = { editTime = it },
                            label = { Text("الوقت", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = Color.White)
                        )
                    }
                    OutlinedTextField(
                        value = editTotalAmount,
                        onValueChange = { editTotalAmount = it },
                        label = { Text("المبلغ الكلي (YER)", fontSize = 10.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = Color.White),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val updatedObj = b.copy(
                            clientName = editClientName.trim(),
                            customerName = editClientName.trim(),
                            clientPhone = editClientPhone.trim(),
                            customerPhone = editClientPhone.trim(),
                            clientAddress = editClientArea.trim(),
                            customerArea = editClientArea.trim(),
                            serviceType = editServiceType.trim(),
                            date = editDate.trim(),
                            dateString = editDate.trim(),
                            time = editTime.trim(),
                            timeString = editTime.trim(),
                            totalAmount = editTotalAmount.toDoubleOrNull() ?: b.totalAmount
                        )
                        viewModel.updateBooking(updatedObj)
                        state.editingBookingObjState.value = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent)
                ) {
                    Text("تحديث وحفظ 💾", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { state.editingBookingObjState.value = null }) {
                    Text("إلغاء", color = Color.White)
                }
            },
            containerColor = themeColors.surface
        )
    }

    // Delete Confirm Dialog
    if (showDeleteConfirmId != null) {
        val b = bookings.find { it.id == showDeleteConfirmId }
        AlertDialog(
            onDismissRequest = { state.showDeleteBookingConfirmIdState.value = null },
            title = { Text("🗑️ تأكيد حذف طلب الحجز نهائياً") },
            text = { Text("هل أنت متأكد تماماً من رغبتك في حذف طلب الحجز رقم (#${showDeleteConfirmId.takeLast(6)}) للعميل (${b?.clientName ?: ""}) بشكل نهائي ولا يمكن الرجوع؟", color = Color.White, fontSize = 11.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteBooking(showDeleteConfirmId)
                        state.showDeleteBookingConfirmIdState.value = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("حذف نهائي 🗑️", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { state.showDeleteBookingConfirmIdState.value = null }) {
                    Text("إلغاء", color = Color.White)
                }
            },
            containerColor = themeColors.surface
        )
    }
}
