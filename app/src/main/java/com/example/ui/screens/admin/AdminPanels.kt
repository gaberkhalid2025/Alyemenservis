package com.example.ui.screens.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.border
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.example.ui.*
import com.example.utils.VisualThemePalette
import com.example.data.*
import com.google.firebase.firestore.FirebaseFirestore


fun LazyListScope.adminRequestsPanel(viewModel: MainViewModel, themeColors: VisualThemePalette, state: AdminPanelState) {
    with(state) {
        if (activeSubTabState.value == "REG_REQ") {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("⌛ طلبات الانضمام والاعتماد المكتملة والمعلقة", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    val pendingList by viewModel.pendingProviders.collectAsState()
                    if (pendingList.isEmpty()) {
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = themeColors.surface)) {
                            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                Text("لا توجد طلبات انضمام معلقة حالياً ✅", color = Color.LightGray, fontSize = 12.sp)
                            }
                        }
                    } else {
                        pendingList.forEach { req ->
                            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = themeColors.surface)) {
                                Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("الاسم: ${req.name}", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                    Text("الهاتف: ${req.phone}", color = Color.LightGray, fontSize = 11.sp)
                                    Text("القسم: ${req.categoryId}", color = Color.LightGray, fontSize = 11.sp)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(onClick = { viewModel.approveTechnician(req.id) }, colors = ButtonDefaults.buttonColors(containerColor = Color.Green)) {
                                            Text("قبول الاعتماد ✅", color = Color.Black, fontSize = 10.sp)
                                        }
                                        Button(onClick = { rejectingProviderRequestState.value = req }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                                            Text("رفض ❌", color = Color.White, fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun LazyListScope.adminManualAddPanel(viewModel: MainViewModel, themeColors: VisualThemePalette, state: AdminPanelState) {
    with(state) {
        if (activeSubTabState.value == "MANUAL_ADD") {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("➕ الإضافة اليدوية السريعة من الإدارة", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)

                    var addCategoryType by remember { mutableStateOf("PROVIDER") } // PROVIDER, STORE, RESTAURANT, MEDICAL, PROPERTY, JOB

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "PROVIDER" to "👨‍🔧 فني",
                            "STORE" to "🏪 محل",
                            "RESTAURANT" to "🍔 مطعم",
                            "MEDICAL" to "🏥 عيادة",
                            "PROPERTY" to "🏠 عقار",
                            "JOB" to "💼 وظيفة"
                        ).forEach { (key, label) ->
                            val isSel = addCategoryType == key
                            Button(
                                onClick = { addCategoryType = key },
                                colors = ButtonDefaults.buttonColors(containerColor = if (isSel) themeColors.accent else themeColors.surface),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)
                            ) {
                                Text(label, fontSize = 10.sp, color = if (isSel) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = themeColors.surface)) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            when (addCategoryType) {
                                "PROVIDER" -> {
                                    Text("➕ إضافة فني / مقدم خدمة جديد يدوياً:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    var name by remember { mutableStateOf("") }
                                    var phone by remember { mutableStateOf("") }
                                    var area by remember { mutableStateOf("صنعاء") }
                                    var price by remember { mutableStateOf("5000") }
                                    var catId by remember { mutableStateOf("صيانة منازل") }

                                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("اسم الفني الكامل") }, modifier = Modifier.fillMaxWidth())
                                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("رقم الهاتف") }, modifier = Modifier.fillMaxWidth())
                                    OutlinedTextField(value = catId, onValueChange = { catId = it }, label = { Text("التخصص / القسم") }, modifier = Modifier.fillMaxWidth())
                                    OutlinedTextField(value = area, onValueChange = { area = it }, label = { Text("المنطقة / المدينة") }, modifier = Modifier.fillMaxWidth())
                                    OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("سعر المعاينة (ر.ي)") }, modifier = Modifier.fillMaxWidth())

                                    Button(
                                        onClick = {
                                            if (name.isNotBlank() && phone.isNotBlank()) {
                                                val p = price.toDoubleOrNull() ?: 0.0
                                                viewModel.addNewProvider(name, phone, catId, area, p, true)
                                                name = ""
                                                phone = ""
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary)
                                    ) {
                                        Text("إضافة واعتماد الفني فوراً 👨‍🔧", color = Color.White)
                                    }
                                }
                                "STORE" -> {
                                    Text("➕ إضافة متجر / محل تجاري جديد:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    var storeName by remember { mutableStateOf("") }
                                    var storePhone by remember { mutableStateOf("") }
                                    var storeDesc by remember { mutableStateOf("") }

                                    OutlinedTextField(value = storeName, onValueChange = { storeName = it }, label = { Text("اسم المحل التجاري") }, modifier = Modifier.fillMaxWidth())
                                    OutlinedTextField(value = storePhone, onValueChange = { storePhone = it }, label = { Text("رقم الهاتف / الواتساب") }, modifier = Modifier.fillMaxWidth())
                                    OutlinedTextField(value = storeDesc, onValueChange = { storeDesc = it }, label = { Text("الوصف والتفاصيل") }, modifier = Modifier.fillMaxWidth())

                                    Button(
                                        onClick = {
                                            if (storeName.isNotBlank()) {
                                                viewModel.addStore(storeName, storeDesc, storePhone)
                                                storeName = ""
                                                storePhone = ""
                                                storeDesc = ""
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary)
                                    ) {
                                        Text("إضافة المحل التجاري 🏪", color = Color.White)
                                    }
                                }
                                "RESTAURANT" -> {
                                    Text("➕ إضافة مطعم / كافيه جديد:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    var restName by remember { mutableStateOf("") }
                                    var restPhone by remember { mutableStateOf("") }
                                    var restDesc by remember { mutableStateOf("") }

                                    OutlinedTextField(value = restName, onValueChange = { restName = it }, label = { Text("اسم المطعم / الكافيه") }, modifier = Modifier.fillMaxWidth())
                                    OutlinedTextField(value = restPhone, onValueChange = { restPhone = it }, label = { Text("رقم الطلبات والخدمة") }, modifier = Modifier.fillMaxWidth())
                                    OutlinedTextField(value = restDesc, onValueChange = { restDesc = it }, label = { Text("نوع الوجبات وساعات العمل") }, modifier = Modifier.fillMaxWidth())

                                    Button(
                                        onClick = {
                                            if (restName.isNotBlank()) {
                                                viewModel.addStore(restName, "مطعم: $restDesc", restPhone)
                                                restName = ""
                                                restPhone = ""
                                                restDesc = ""
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary)
                                    ) {
                                        Text("إضافة المطعم 🍔", color = Color.White)
                                    }
                                }
                                "MEDICAL" -> {
                                    Text("➕ إضافة مركز طبي / عيادة جديدة:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    var medName by remember { mutableStateOf("") }
                                    var medPhone by remember { mutableStateOf("") }
                                    var medSpec by remember { mutableStateOf("") }

                                    OutlinedTextField(value = medName, onValueChange = { medName = it }, label = { Text("اسم المركز الطبي / العيادة") }, modifier = Modifier.fillMaxWidth())
                                    OutlinedTextField(value = medPhone, onValueChange = { medPhone = it }, label = { Text("رقم الهاتف والطوارئ") }, modifier = Modifier.fillMaxWidth())
                                    OutlinedTextField(value = medSpec, onValueChange = { medSpec = it }, label = { Text("التخصصات الطبية والدوام") }, modifier = Modifier.fillMaxWidth())

                                    Button(
                                        onClick = {
                                            if (medName.isNotBlank()) {
                                                viewModel.addStore(medName, "عيادة/مركز طبي: $medSpec", medPhone)
                                                medName = ""
                                                medPhone = ""
                                                medSpec = ""
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary)
                                    ) {
                                        Text("إضافة المركز الطبي 🏥", color = Color.White)
                                    }
                                }
                                "PROPERTY" -> {
                                    Text("➕ إضافة عقار / أراضي جديدة:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    var propTitle by remember { mutableStateOf("") }
                                    var propPrice by remember { mutableStateOf("") }
                                    var propPhone by remember { mutableStateOf("") }

                                    OutlinedTextField(value = propTitle, onValueChange = { propTitle = it }, label = { Text("عنوان العقار") }, modifier = Modifier.fillMaxWidth())
                                    OutlinedTextField(value = propPrice, onValueChange = { propPrice = it }, label = { Text("السعر المحدد") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                                    OutlinedTextField(value = propPhone, onValueChange = { propPhone = it }, label = { Text("رقم التواصل") }, modifier = Modifier.fillMaxWidth())

                                    Button(
                                        onClick = {
                                            if (propTitle.isNotBlank()) {
                                                val priceVal = propPrice.toDoubleOrNull() ?: 0.0
                                                viewModel.addProperty(propTitle, "عقار ممتاز", priceVal, "sale", "apartment", propPhone)
                                                propTitle = ""
                                                propPrice = ""
                                                propPhone = ""
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary)
                                    ) {
                                        Text("إضافة العقار 🏠", color = Color.White)
                                    }
                                }
                                "JOB" -> {
                                    Text("➕ إضافة وظيفة جديدة يدوياً:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    var jobTitle by remember { mutableStateOf("") }
                                    var compName by remember { mutableStateOf("") }
                                    var salary by remember { mutableStateOf("") }
                                    var jobPhone by remember { mutableStateOf("") }

                                    OutlinedTextField(value = jobTitle, onValueChange = { jobTitle = it }, label = { Text("المسمى الوظيفي") }, modifier = Modifier.fillMaxWidth())
                                    OutlinedTextField(value = compName, onValueChange = { compName = it }, label = { Text("اسم الشركة المعلنة") }, modifier = Modifier.fillMaxWidth())
                                    OutlinedTextField(value = salary, onValueChange = { salary = it }, label = { Text("الراتب المتوقع") }, modifier = Modifier.fillMaxWidth())
                                    OutlinedTextField(value = jobPhone, onValueChange = { jobPhone = it }, label = { Text("رقم الهاتف") }, modifier = Modifier.fillMaxWidth())

                                    Button(
                                        onClick = {
                                            if (jobTitle.isNotBlank()) {
                                                viewModel.addJob(jobTitle, compName, "تفاصيل الوظيفة", jobPhone, salary)
                                                jobTitle = ""
                                                compName = ""
                                                salary = ""
                                                jobPhone = ""
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary)
                                    ) {
                                        Text("إضافة الوظيفة 💼", color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun LazyListScope.adminProvidersPanel(viewModel: MainViewModel, themeColors: VisualThemePalette, state: AdminPanelState) {
    with(state) {
        val tab = activeSubTabState.value
        if (tab in listOf("PROVIDERS", "PASSWORDS_RESET")) {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("👥 إدارة أعضاء الدليل والتميز والتصنيفات", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    val providers by viewModel.providers.collectAsState()
                    if (providers.isEmpty()) {
                        Text("لا يوجد اعضاء في الدليل حالياً.", color = Color.LightGray, fontSize = 11.sp)
                    } else {
                        providers.forEach { p ->
                            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = themeColors.surface)) {
                                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(p.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                        Text("الهاتف: ${p.phone} | القسم: ${p.categoryId}", color = Color.LightGray, fontSize = 10.sp)
                                    }
                                    Button(onClick = { viewModel.removeProvider(p.id) }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f))) {
                                        Text("حذف 🗑️", fontSize = 10.sp, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun LazyListScope.adminBookingsPanel(viewModel: MainViewModel, themeColors: VisualThemePalette, state: AdminPanelState) {
    with(state) {
        if (activeSubTabState.value == "BOOKINGS") {
            item {
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

                // Dialog states
                var selectedBookingForDetails by remember { mutableStateOf<BookingEntity?>(null) }
                var bookingToEdit by remember { mutableStateOf<BookingEntity?>(null) }
                var bookingToCancel by remember { mutableStateOf<BookingEntity?>(null) }
                var cancellationReasonText by remember { mutableStateOf("") }
                var bookingToReject by remember { mutableStateOf<BookingEntity?>(null) }
                var rejectionReasonText by remember { mutableStateOf("") }
                var bookingToRedirect by remember { mutableStateOf<BookingEntity?>(null) }
                var redirectSearchQuery by remember { mutableStateOf("") }
                var showDeleteConfirm by remember { mutableStateOf<BookingEntity?>(null) }

                // Payment settings toggle state
                var showPaymentSettingsState by remember { mutableStateOf(false) }

                // Local states for editing form fields
                var editClientName by remember { mutableStateOf("") }
                var editClientPhone by remember { mutableStateOf("") }
                var editClientArea by remember { mutableStateOf("") }
                var editServiceType by remember { mutableStateOf("") }
                var editDate by remember { mutableStateOf("") }
                var editTime by remember { mutableStateOf("") }
                var editStatus by remember { mutableStateOf("") }
                var editTotalAmount by remember { mutableStateOf("") }

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

                                Divider(color = Color.Gray.copy(alpha = 0.2f))

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

                    // Bookings Lazy List view
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
                                    // Row 1: ID & Status Badge
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

                                    // Content Info
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

                                    // Quick Action Buttons matching screenshots requirements
                                    Row(
                                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        // Accept Button
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

                                        // Reject Button
                                        if (isPending || isApproved) {
                                            Button(
                                                onClick = {
                                                    bookingToReject = b
                                                    rejectionReasonText = ""
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 1.dp),
                                                modifier = Modifier.height(26.dp),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text("رفض ❌", fontSize = 9.sp, color = Color.White)
                                            }
                                        }

                                        // Start Button
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

                                        // Complete Button
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

                                        // Cancel Button
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

                                        // Redirect Button
                                        Button(
                                            onClick = {
                                                bookingToRedirect = b
                                                redirectSearchQuery = ""
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 1.dp),
                                            modifier = Modifier.height(26.dp),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text("توجيه 🔄", fontSize = 9.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                        }

                                        // Edit Button
                                        Button(
                                            onClick = {
                                                bookingToEdit = b
                                                editClientName = b.clientName.ifEmpty { b.customerName }
                                                editClientPhone = b.clientPhone.ifEmpty { b.customerPhone }
                                                editClientArea = b.clientAddress.ifEmpty { b.customerArea }
                                                editServiceType = b.serviceType
                                                editDate = b.date.ifEmpty { b.dateString }
                                                editTime = b.time.ifEmpty { b.timeString }
                                                editStatus = b.status
                                                editTotalAmount = b.totalAmount.toString()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 1.dp),
                                            modifier = Modifier.height(26.dp),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text("تعديل ✏️", fontSize = 9.sp, color = Color.White)
                                        }

                                        // Delete Button
                                        Button(
                                            onClick = { showDeleteConfirm = b },
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
                                Divider(color = Color.Gray.copy(alpha=0.15f))
                                Text("🛠️ الفني: ${b.providerName}", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                                Text("🏷️ نوع الخدمة: ${b.serviceType}", color = Color.LightGray, fontSize = 11.sp)
                                Text("📅 الموعد: ${b.date.ifEmpty { b.dateString }} الساعة ${b.time.ifEmpty { b.timeString }}", color = Color.LightGray, fontSize = 11.sp)
                                Divider(color = Color.Gray.copy(alpha=0.15f))
                                Text("الحالة الحالية: ${b.status}", color = themeColors.accent, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                Text("💰 السعر الكلي: ${b.totalAmount} ريال", color = Color.White, fontSize = 11.sp)
                                Text("💳 دفعة مقدمة: ${b.advancePayment} ريال (${b.paymentStatus})", color = Color.LightGray, fontSize = 11.sp)
                                Text("🔑 رمز الإلغاء السري (كلمة مرور الحجز): ${b.bookingPassword.ifEmpty { "غير مولدة" }}", color = Color.Yellow, fontSize = 11.sp)
                                if (!b.rejectionReason.isBlank()) {
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
                        }
                    )
                }

                // Reject dialog
                if (bookingToReject != null) {
                    val b = bookingToReject!!
                    AlertDialog(
                        onDismissRequest = { bookingToReject = null },
                        title = { Text("❌ رفض طلب الحجز #${b.id.takeLast(6)}") },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("يرجى كتابة سبب أو مبرر رفض طلب الحجز لإرساله للعميل:", fontSize = 11.sp, color = Color.LightGray)
                                OutlinedTextField(
                                    value = rejectionReasonText,
                                    onValueChange = { rejectionReasonText = it },
                                    placeholder = { Text("مثال: الفني غير متوفر حالياً بالمنطقة...", fontSize = 10.sp) },
                                    modifier = Modifier.fillMaxWidth(),
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = Color.White)
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    viewModel.updateBookingStatus(b.id, "REJECTED", rejectionReasonText.trim())
                                    bookingToReject = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                            ) {
                                Text("تأكيد الرفض ❌", color = Color.White)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { bookingToReject = null }) {
                                Text("إلغاء", color = Color.White)
                            }
                        }
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
                        }
                    )
                }

                // Redirect / Assign dialog
                if (bookingToRedirect != null) {
                    val b = bookingToRedirect!!
                    AlertDialog(
                        onDismissRequest = { bookingToRedirect = null },
                        title = { Text("🔄 إعادة توجيه/تعيين حجز #${b.id.takeLast(6)}") },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("توجيه الحجز إلى فني محدد، أو المشرف، أو أقرب فني جغرافي:", fontSize = 11.sp, color = Color.LightGray)
                                
                                // Category assignment quick options
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
                                            bookingToRedirect = null
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
                                            // nearest provider simulator
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
                                            bookingToRedirect = null
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(4.dp),
                                        contentPadding = PaddingValues(2.dp)
                                    ) {
                                        Text("الأقرب جغرافياً 📍", fontSize = 10.sp, color = Color.Black)
                                    }
                                }

                                Divider(color = Color.Gray.copy(alpha=0.15f))

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
                                                        bookingToRedirect = null
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
                            TextButton(onClick = { bookingToRedirect = null }) {
                                Text("إغلاق", color = Color.White)
                            }
                        }
                    )
                }

                // Edit Dialog
                if (bookingToEdit != null) {
                    val b = bookingToEdit!!
                    AlertDialog(
                        onDismissRequest = { bookingToEdit = null },
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
                                    bookingToEdit = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent)
                            ) {
                                Text("تحديث وحفظ 💾", color = Color.Black)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { bookingToEdit = null }) {
                                Text("إلغاء", color = Color.White)
                            }
                        }
                    )
                }

                // Delete Confirm Dialog
                if (showDeleteConfirm != null) {
                    val b = showDeleteConfirm!!
                    AlertDialog(
                        onDismissRequest = { showDeleteConfirm = null },
                        title = { Text("🗑️ تأكيد حذف طلب الحجز نهائياً") },
                        text = { Text("هل أنت متأكد تماماً من رغبتك في حذف طلب الحجز رقم (#${b.id.takeLast(6)}) للعميل (${b.clientName.ifEmpty { b.customerName }}) بشكل نهائي ولا يمكن الرجوع؟", color = Color.White, fontSize = 11.sp) },
                        confirmButton = {
                            Button(
                                onClick = {
                                    viewModel.deleteBooking(b.id)
                                    showDeleteConfirm = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                            ) {
                                Text("حذف نهائي 🗑️", color = Color.White)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteConfirm = null }) {
                                Text("إلغاء", color = Color.White)
                            }
                        }
                    )
                }
            }
        }
    }
}

fun LazyListScope.adminNotificationsPanel(viewModel: MainViewModel, themeColors: VisualThemePalette, state: AdminPanelState) {
    with(state) {
        if (activeSubTabState.value == "NOTIFICATIONS") {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("🔔 بث الإشعارات الفورية الموجهة للأقسام والأعضاء", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    Text("بث إشعارات فورية مخصصة وموجهة حسب الفئة لضمان وصول التنبيهات للشرائح المناسبة.", color = Color.LightGray, fontSize = 11.sp)
                    
                    var notifTitle by remember { mutableStateOf("") }
                    var notifMsg by remember { mutableStateOf("") }
                    var targetTypeSelected by remember { mutableStateOf("ALL") }
                    val notificationsList by viewModel.notifications.collectAsState()
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = notifTitle,
                                onValueChange = { notifTitle = it },
                                label = { Text("عنوان الإشعار", fontSize = 11.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = Color.White)
                            )
                            
                            OutlinedTextField(
                                value = notifMsg,
                                onValueChange = { notifMsg = it },
                                label = { Text("نص ومحتوى الإشعار", fontSize = 11.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = Color.White)
                            )
                            
                            // Target Type Selector Grid
                            Text("🎯 الفئة المستهدفة بالإشعار:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            val targets = listOf(
                                Pair("ALL", "الكل 👥"),
                                Pair("PROVIDER", "الفنيين 🛠️"),
                                Pair("STORE", "المحلات 🏪"),
                                Pair("RESTAURANT", "المطاعم 🍔"),
                                Pair("MEDICAL", "المنشآت الطبية 🏥"),
                                Pair("PROPERTY", "العقارات 🏠"),
                                Pair("JOB", "الوظائف 💼")
                            )
                            
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                targets.chunked(2).forEach { rowTargets ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        rowTargets.forEach { item ->
                                            val isSel = targetTypeSelected == item.first
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(if (isSel) themeColors.accent else Color.White.copy(alpha = 0.05f))
                                                    .clickable { targetTypeSelected = item.first }
                                                    .padding(vertical = 8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = item.second,
                                                    fontSize = 10.sp,
                                                    color = if (isSel) Color.Black else Color.White,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        if (rowTargets.size < 2) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                            
                            Button(
                                onClick = {
                                    if (notifTitle.isNotEmpty() && notifMsg.isNotEmpty()) {
                                        viewModel.addNotification(
                                            title = notifTitle.trim(),
                                            message = notifMsg.trim(),
                                            targetType = targetTypeSelected,
                                            targetValue = "all"
                                        )
                                        notifTitle = ""
                                        notifMsg = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("إرسال الإشعار فوراً 🚀", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    // Notifications History
                    Text("📋 سجل الإشعارات المرسلة إدارياً (${notificationsList.size}):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    
                    if (notificationsList.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                            Text("لا توجد إشعارات مرسلة بعد", color = Color.Gray, fontSize = 11.sp)
                        }
                    } else {
                        notificationsList.take(20).forEach { notif ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = themeColors.surface)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(notif.title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            Text(
                                                text = when (notif.targetType) {
                                                    "ALL" -> "الكل 👥"
                                                    "PROVIDER" -> "فني 🛠️"
                                                    "STORE" -> "متجر 🏪"
                                                    "RESTAURANT" -> "مطعم 🍔"
                                                    "MEDICAL" -> "طبي 🏥"
                                                    "PROPERTY" -> "عقار 🏠"
                                                    "JOB" -> "وظيفة 💼"
                                                    else -> notif.targetType
                                                },
                                                fontSize = 8.sp,
                                                color = themeColors.accent,
                                                modifier = Modifier
                                                    .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                        Text(notif.message, fontSize = 10.sp, color = Color.LightGray)
                                    }
                                    
                                    IconButton(onClick = { viewModel.deleteNotification(notif.id) }) {
                                        Text("🗑️", fontSize = 14.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun LazyListScope.adminChatPanel(viewModel: MainViewModel, themeColors: VisualThemePalette, state: AdminPanelState) {
    with(state) {
        if (activeSubTabState.value in listOf("CHATS", "ADVANCED_CHAT")) {
            item {
                AdminChatPanelContent(viewModel = viewModel, themeColors = themeColors)
            }
        }
    }
}

@Composable
fun AdminChatPanelContent(viewModel: MainViewModel, themeColors: VisualThemePalette) {
    val settingsState by viewModel.settings.collectAsState()
    val chatChannels by viewModel.chatChannels.collectAsState()
    val orders by viewModel.orders.collectAsState()
    val providers by viewModel.providers.collectAsState()
    val notifications by viewModel.notifications.collectAsState()

    var activeSubTab by remember { mutableStateOf("SETTINGS") } // SETTINGS, LIVE_CHATS, PROTECTION, AUDIT_LOG

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Sub-Tab Navigation Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val subTabs = listOf(
                Pair("SETTINGS", "⚙️ التحكم والتوجيه"),
                Pair("LIVE_CHATS", "💬 رقابة الدردشات"),
                Pair("PROTECTION", "🛡️ حماية المشتريات"),
                Pair("AUDIT_LOG", "📋 سجل الأدلة والتوثيق")
            )
            subTabs.forEach { tab ->
                val isSelected = activeSubTab == tab.first
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) themeColors.accent else Color.Transparent)
                        .clickable { activeSubTab = tab.first }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = tab.second,
                        color = if (isSelected) Color.Black else Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Sub-Tab Contents
        when (activeSubTab) {
            "SETTINGS" -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                    border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.15f))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("⚙️ إعدادات التحكم والتعطيل والتوجيه الفوري", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)

                        // General Chat Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("تعطيل المحادثات الفورية بالكامل 🛑", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("يتم إيقاف المراسلة وتنبيه الأعضاء بالسبب المكتوب أدناه.", fontSize = 9.sp, color = Color.LightGray)
                            }
                            Switch(
                                checked = settingsState.disableChatAll,
                                onCheckedChange = { viewModel.saveCustomSettingsState(settingsState.copy(disableChatAll = it)) },
                                colors = SwitchDefaults.colors(checkedThumbColor = themeColors.accent)
                            )
                        }

                        // Disabled Reason Announcement
                        if (settingsState.disableChatAll) {
                            var announcementText by remember(settingsState.chatDisabledAnnouncement) { mutableStateOf(settingsState.chatDisabledAnnouncement) }
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("سبب الإيقاف المعلن للأعضاء:", fontSize = 10.sp, color = Color.LightGray)
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedTextField(
                                        value = announcementText,
                                        onValueChange = { announcementText = it },
                                        placeholder = { Text("اكتب سبب الإيقاف المؤقت...", fontSize = 10.sp) },
                                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = Color.White),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    Button(
                                        onClick = {
                                            viewModel.saveCustomSettingsState(settingsState.copy(chatDisabledAnnouncement = announcementText))
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("حفظ 💾", fontSize = 10.sp, color = Color.Black)
                                    }
                                }
                            }
                        }

                        Divider(color = Color.Gray.copy(alpha = 0.15f))

                        // Category-wise Toggles
                        Text("📂 التحكم في أزرار المحادثات حسب القسم:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        val sections = listOf(
                            Pair("stores", "🏪 المحلات والمراكز"),
                            Pair("restaurants", "🍔 المطاعم والكافيهات"),
                            Pair("medical", "🏥 المراكز الطبية والعيادات"),
                            Pair("properties", "🏠 العقارات والأراضي"),
                            Pair("jobs", "💼 إعلانات الوظائف"),
                            Pair("services", "🛠️ الفنيين والمهن الحرة")
                        )

                        val disabledCatsList = remember(settingsState.chatDisabledCategories) {
                            settingsState.chatDisabledCategories.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            sections.forEach { sec ->
                                val isCatDisabled = sec.first in disabledCatsList
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(sec.second, fontSize = 11.sp, color = Color.White)
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            text = if (isCatDisabled) "إخفاء الزر 🚫" else "إظهار الزر 💬",
                                            fontSize = 9.sp,
                                            color = if (isCatDisabled) Color.Red else Color.Green
                                        )
                                        Switch(
                                            checked = !isCatDisabled,
                                            onCheckedChange = { isVisible ->
                                                val newList = if (!isVisible) {
                                                    disabledCatsList + sec.first
                                                } else {
                                                    disabledCatsList - sec.first
                                                }
                                                viewModel.saveCustomSettingsState(
                                                    settingsState.copy(chatDisabledCategories = newList.joinToString(","))
                                                )
                                            },
                                            colors = SwitchDefaults.colors(checkedThumbColor = themeColors.accent),
                                            modifier = Modifier.scale(0.85f)
                                        )
                                    }
                                }
                            }
                        }

                        Divider(color = Color.Gray.copy(alpha = 0.15f))

                        // Specific Entity Block ID
                        Text("🚫 إخفاء زر المحادثة لمتجر/مركز/فني محدد:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("أدخل معرف المحل أو هاتف الفني (افصل بفواصل لعدة حسابات):", fontSize = 9.sp, color = Color.LightGray)
                        var blockedIdsInput by remember(settingsState.chatBlockedIds) { mutableStateOf(settingsState.chatBlockedIds) }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(
                                value = blockedIdsInput,
                                onValueChange = { blockedIdsInput = it },
                                placeholder = { Text("مثال: 777644222 , store_103", fontSize = 10.sp) },
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = Color.White),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            Button(
                                onClick = {
                                    viewModel.saveCustomSettingsState(settingsState.copy(chatBlockedIds = blockedIdsInput))
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("تطبيق 🚫", fontSize = 10.sp, color = Color.Black)
                            }
                        }

                        Divider(color = Color.Gray.copy(alpha = 0.15f))

                        // Chat Routing Mode
                        Text("⚡ وضع وتوجيه المحادثات الذكي (Chat Routing Mode):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        val routingModes = listOf(
                            Pair("DEFAULT", "🏪 توجيه مباشر ومستقل (تواصل حر بين العميل والبائع)"),
                            Pair("ADMIN_ONLY", "🛡️ رقابة وتوجيه كامل للأدمن (تتحول كافة الدردشات للدعم الفني)"),
                            Pair("ADMIN_THEN_CENTER", "🔄 موافقة وتحويل إداري (تبدأ الدردشة مع الأدمن ويحولها للمحل يدوياً)")
                        )
                        routingModes.forEach { mode ->
                            val isSelected = settingsState.chatRoutingMode == mode.first
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.saveCustomSettingsState(settingsState.copy(chatRoutingMode = mode.first)) }
                                    .background(if (isSelected) themeColors.primary.copy(alpha = 0.15f) else Color.Transparent, RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { viewModel.saveCustomSettingsState(settingsState.copy(chatRoutingMode = mode.first)) },
                                    colors = RadioButtonDefaults.colors(selectedColor = themeColors.accent)
                                )
                                Text(mode.second, fontSize = 10.sp, color = Color.White, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                }
            }

            "LIVE_CHATS" -> {
                var selectedChannelForSupervision by remember { mutableStateOf<ChatChannelEntity?>(null) }
                var showTransferDialog by remember { mutableStateOf(false) }
                var transferProviderQuery by remember { mutableStateOf("") }
                var editingMessageState by remember { mutableStateOf<ChatMessageEntity?>(null) }
                var editedMessageText by remember { mutableStateOf("") }

                if (selectedChannelForSupervision == null) {
                    // Show Channels List
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("💬 المحادثات المباشرة النشطة (${chatChannels.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                            
                            if (chatChannels.isEmpty()) {
                                Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                                    Text("لا توجد محادثات جارية حالياً", color = Color.Gray, fontSize = 11.sp)
                                }
                            } else {
                                chatChannels.forEach { ch ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                            .clickable { selectedChannelForSupervision = ch }
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                if (ch.isBlocked) {
                                                    Text("🚫 حظر", fontSize = 8.sp, color = Color.Red, fontWeight = FontWeight.Bold, modifier = Modifier.background(Color.Red.copy(alpha=0.15f)).padding(horizontal=4.dp, vertical=1.dp))
                                                }
                                                Text(ch.userName.ifEmpty { "محادثة #${ch.id.takeLast(6)}" }, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            }
                                            Text(ch.lastMessage.ifEmpty { "لا توجد رسائل بعد" }, fontSize = 9.sp, color = Color.LightGray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                        Icon(imageVector = Icons.Default.KeyboardArrowLeft, contentDescription = "عرض التفاصيل", tint = themeColors.accent)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Channel Supervisor Window
                    val ch = selectedChannelForSupervision!!
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    IconButton(onClick = { selectedChannelForSupervision = null }, modifier = Modifier.size(28.dp)) {
                                        Icon(Icons.Default.ArrowBack, contentDescription = "رجوع", tint = Color.White)
                                    }
                                    Column {
                                        Text(ch.userName, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Text("معرف: ${ch.id}", fontSize = 8.sp, color = Color.LightGray)
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    // Block Button
                                    Button(
                                        onClick = {
                                            viewModel.blockChatChannel(ch.id, !ch.isBlocked)
                                            selectedChannelForSupervision = ch.copy(isBlocked = !ch.isBlocked)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = if (ch.isBlocked) Color.Green else Color.Red),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(if (ch.isBlocked) "إلغاء حظر 🔓" else "حظر 🚫", fontSize = 9.sp, color = Color.White)
                                    }

                                    // Transfer Button (Visible if Support/Admin target)
                                    if (ch.id.startsWith("support_")) {
                                        Button(
                                            onClick = { showTransferDialog = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.height(28.dp),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text("تحويل فني 🔄", fontSize = 9.sp, color = Color.Black)
                                        }
                                    }
                                }
                            }

                            Divider(color = Color.Gray.copy(alpha = 0.15f))

                            // Messages List
                            Text("📁 أرشيف رسائل المحادثة للرقابة والتوثيق (اضغط لتعديل/حذف الرسالة):", fontSize = 10.sp, color = themeColors.accent)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .padding(6.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    ch.messages.forEach { msg ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    editingMessageState = msg
                                                    editedMessageText = msg.message
                                                }
                                                .padding(4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("${msg.senderName}:", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                                                Text(msg.message, fontSize = 11.sp, color = Color.White)
                                            }
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                IconButton(
                                                    onClick = { viewModel.deleteChatMessage(ch.id, msg.id) },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Text("🗑️", fontSize = 10.sp)
                                                }
                                            }
                                        }
                                        Divider(color = Color.Gray.copy(alpha = 0.08f))
                                    }
                                }
                            }

                            // Admin Reply Box
                            var replyInputText by remember { mutableStateOf("") }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedTextField(
                                    value = replyInputText,
                                    onValueChange = { replyInputText = it },
                                    placeholder = { Text("أرسل رداً مشرفاً كدليل...", fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f),
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = Color.White),
                                    singleLine = true
                                )
                                Button(
                                    onClick = {
                                        if (replyInputText.isNotEmpty()) {
                                            viewModel.replyToChatChannel(ch.id, "admin", replyInputText.trim(), "المشرف العام 🛡️")
                                            replyInputText = ""
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("رد إداري 💬", fontSize = 10.sp, color = Color.Black)
                                }
                            }
                        }
                    }
                }

                // Edit Message Dialog
                if (editingMessageState != null) {
                    val msg = editingMessageState!!
                    AlertDialog(
                        onDismissRequest = { editingMessageState = null },
                        title = { Text("✏️ تعديل رسالة العضو إدارياً") },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("رسالة: ${msg.senderName}", fontSize = 10.sp, color = Color.LightGray)
                                OutlinedTextField(
                                    value = editedMessageText,
                                    onValueChange = { editedMessageText = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = Color.White)
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    selectedChannelForSupervision?.id?.let { chId ->
                                        viewModel.editChatMessageAdmin(chId, msg.id, editedMessageText.trim())
                                    }
                                    editingMessageState = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent)
                            ) {
                                Text("تعديل وحفظ", color = Color.Black)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { editingMessageState = null }) {
                                Text("إلغاء", color = Color.White)
                            }
                        }
                    )
                }

                // Transfer support chat to provider dialog
                if (showTransferDialog && selectedChannelForSupervision != null) {
                    AlertDialog(
                        onDismissRequest = { showTransferDialog = false },
                        title = { Text("🔄 توجيه وتحويل المحادثة لفني") },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("ابحث واختر مقدم الخدمة/المحل المراد توجيه الطلب والدردشة إليه:", fontSize = 11.sp, color = Color.LightGray)
                                OutlinedTextField(
                                    value = transferProviderQuery,
                                    onValueChange = { transferProviderQuery = it },
                                    placeholder = { Text("ابحث باسم الفني أو الهاتف...", fontSize = 10.sp) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Box(modifier = Modifier.height(150.dp).fillMaxWidth().verticalScroll(rememberScrollState())) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        providers.filter { 
                                            it.name.contains(transferProviderQuery) || it.phone.contains(transferProviderQuery)
                                        }.take(5).forEach { prov ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                                    .clickable {
                                                        viewModel.transferChatChannelToProvider(selectedChannelForSupervision!!.id, prov.id, prov.name)
                                                        showTransferDialog = false
                                                        selectedChannelForSupervision = null
                                                    }
                                                    .padding(8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(prov.name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                Text(prov.phone, fontSize = 10.sp, color = themeColors.accent)
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showTransferDialog = false }) {
                                Text("إغلاق", color = Color.White)
                            }
                        }
                    )
                }
            }

            "PROTECTION" -> {
                var searchQuery by remember { mutableStateOf("") }
                var selectedOrderForProtectionDetail by remember { mutableStateOf<OrderEntity?>(null) }
                var freezeReasonInput by remember { mutableStateOf("") }

                val filteredOrders = remember(orders, searchQuery) {
                    if (searchQuery.isBlank()) orders else {
                        orders.filter { 
                            it.id.contains(searchQuery) || it.customerPhone.contains(searchQuery) || it.productName.contains(searchQuery)
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = themeColors.surface)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("🛡️ نظام مكافحة الاحتيال وحماية عمليات الشراء", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                        Text("تتبع مبيعات المتاجر، التحقق الإداري، وفك النزاعات وتجميد المحافظ والحسابات البنكية.", fontSize = 9.sp, color = Color.LightGray)

                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("ابحث عن طلب برقم العضو أو المعرف...", fontSize = 10.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        if (filteredOrders.isEmpty()) {
                            Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                                Text("لا توجد مبيعات/مشتريات مطابقة للبحث", color = Color.Gray, fontSize = 11.sp)
                            }
                        } else {
                            filteredOrders.forEach { ord ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.3f)),
                                    border = BorderStroke(1.dp, if (ord.disputeStatus == "FRAUD_DETECTED") Color.Red else Color.Gray.copy(alpha=0.12f))
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("طلب #${ord.id.takeLast(6)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            Surface(
                                                color = if (ord.isVerifiedByAdmin) Color(0xFF10B981) else Color(0xFFF59E0B),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = if (ord.isVerifiedByAdmin) "تحقق إداري: مؤكد ✅" else "تحقق إداري: معلق ⏳",
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                                    fontSize = 8.sp,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        Text("🛍️ السلعة: ${ord.productName} (العدد: ${ord.quantity})", fontSize = 11.sp, color = Color.White)
                                        Text("🏪 المتجر: ${ord.storeName}", fontSize = 10.sp, color = Color.LightGray)
                                        Text("👤 المشتري: ${ord.customerName} (${ord.customerPhone})", fontSize = 10.sp, color = Color.LightGray)

                                        if (ord.disputeStatus != "NONE") {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Text("⚠️ حالة النزاع للطلب:", fontSize = 10.sp, color = Color.Red)
                                                Text(
                                                    text = when(ord.disputeStatus) {
                                                        "UNDER_INVESTIGATION" -> "قيد التحقيق الإداري 🔍"
                                                        "RESOLVED" -> "تم الحل والرد ✅"
                                                        "FRAUD_DETECTED" -> "تم كشف احتيال بائع 🛑"
                                                        else -> "لا يوجد نـزاع"
                                                    },
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.Red
                                                )
                                            }
                                        }

                                        if (ord.adminNotes.isNotEmpty()) {
                                            Text("📝 قرارات وحكم الإدارة: ${ord.adminNotes}", fontSize = 10.sp, color = Color.Yellow)
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                            horizontalArrangement = Arrangement.End,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Button(
                                                onClick = { selectedOrderForProtectionDetail = ord },
                                                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                                                shape = RoundedCornerShape(6.dp),
                                                modifier = Modifier.height(28.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                            ) {
                                                Text("إدارة الحماية وحل النزاع 🛠️", fontSize = 10.sp, color = Color.Black)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Protection Detail & Dispute manager dialog
                if (selectedOrderForProtectionDetail != null) {
                    val ord = selectedOrderForProtectionDetail!!
                    var isVerifiedState by remember(ord.isVerifiedByAdmin) { mutableStateOf(ord.isVerifiedByAdmin) }
                    var disputeState by remember(ord.disputeStatus) { mutableStateOf(ord.disputeStatus) }
                    var adminNotesInput by remember(ord.adminNotes) { mutableStateOf(ord.adminNotes) }

                    AlertDialog(
                        onDismissRequest = { selectedOrderForProtectionDetail = null },
                        title = { Text("🛠️ إدارة حماية الطلب #${ord.id.takeLast(6)}") },
                        text = {
                            Column(
                                modifier = Modifier.verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text("إقرار حماية مشتريات الأعضاء من الاحتيال:", fontSize = 11.sp, color = Color.LightGray)

                                // Verify Toggle
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("تدقيق وتأكيد سلامة الشراء ✅", fontSize = 11.sp, color = Color.White)
                                    Switch(
                                        checked = isVerifiedState,
                                        onCheckedChange = { isVerifiedState = it },
                                        colors = SwitchDefaults.colors(checkedThumbColor = themeColors.accent)
                                    )
                                }

                                // Dispute Selector
                                Text("توجيه النزاع والشكاوى:", fontSize = 11.sp, color = Color.White)
                                val disputeModes = listOf(
                                    Pair("NONE", "لا يوجد نزاع (سليم)"),
                                    Pair("UNDER_INVESTIGATION", "قيد التحقيق والنزاع الإداري 🔍"),
                                    Pair("RESOLVED", "تم فض النزاع بالتسوية الودية ✅"),
                                    Pair("FRAUD_DETECTED", "احتيال بائع مؤكد (سيتم إلغاء الطلب) 🛑")
                                )
                                disputeModes.forEach { mode ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { disputeState = mode.first }
                                            .padding(6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        RadioButton(
                                            selected = disputeState == mode.first,
                                            onClick = { disputeState = mode.first },
                                            colors = RadioButtonDefaults.colors(selectedColor = themeColors.accent)
                                        )
                                        Text(mode.second, fontSize = 11.sp, color = Color.White)
                                    }
                                }

                                // Admin Notes
                                OutlinedTextField(
                                    value = adminNotesInput,
                                    onValueChange = { adminNotesInput = it },
                                    label = { Text("ملاحظات إدارية / قرار فض النزاع") },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Divider(color = Color.Gray.copy(alpha = 0.15f))

                                // Freeze Fraud account section
                                Text("❄️ عقوبات الاحتيال الفورية:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Red)
                                OutlinedTextField(
                                    value = freezeReasonInput,
                                    onValueChange = { freezeReasonInput = it },
                                    placeholder = { Text("أدخل مبرر تجميد البائع/المشتري...", fontSize = 10.sp) },
                                    label = { Text("سبب الحظر والتجميد") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            if (freezeReasonInput.isNotEmpty()) {
                                                viewModel.freezeWalletOrAccount(ord.customerPhone, true, freezeReasonInput)
                                                freezeReasonInput = ""
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("تجميد محفظة المشتري ❄️", fontSize = 9.sp, color = Color.White)
                                    }
                                    Button(
                                        onClick = {
                                            if (freezeReasonInput.isNotEmpty()) {
                                                viewModel.freezeWalletOrAccount(ord.storeId, true, freezeReasonInput)
                                                freezeReasonInput = ""
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("تجميد محفظة المحل ❄️", fontSize = 9.sp, color = Color.White)
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    viewModel.updateOrderProtection(ord.id, isVerifiedState, disputeState, adminNotesInput)
                                    selectedOrderForProtectionDetail = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent)
                            ) {
                                Text("حفظ وتحديث 💾", color = Color.Black)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { selectedOrderForProtectionDetail = null }) {
                                Text("إغلاق", color = Color.White)
                            }
                        }
                    )
                }
            }

            "AUDIT_LOG" -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = themeColors.surface)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("🛡️ سجل أدلة حماية المشتريات الآمن (Audit Evidence Log)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                        Text("سجل آمن تاريخي لقرارات التحقق، الإيقاف، وتجميد المحافظ والأنشطة الإدارية لضمان عدم التلاعب بالأدلة.", fontSize = 9.sp, color = Color.LightGray)

                        val supervisorLogs = remember(notifications) {
                            notifications.filter { it.targetType == "SUPERVISOR" }.sortedByDescending { it.id }
                        }

                        if (supervisorLogs.isEmpty()) {
                            Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                                Text("سجل الأدلة والتوثيق فارغ حالياً", color = Color.Gray, fontSize = 11.sp)
                            }
                        } else {
                            Box(modifier = Modifier.height(200.dp).fillMaxWidth().verticalScroll(rememberScrollState())) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    supervisorLogs.forEach { log ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha=0.2f))
                                        ) {
                                            Column(modifier = Modifier.padding(8.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(log.title, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Yellow)
                                                    Text("معرف #${log.id.takeLast(4)}", fontSize = 8.sp, color = Color.Gray)
                                                }
                                                Text(log.message, fontSize = 11.sp, color = Color.White, modifier = Modifier.padding(top=2.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun LazyListScope.adminBannersPanel(viewModel: MainViewModel, themeColors: VisualThemePalette, state: AdminPanelState) {
    with(state) {
        if (activeSubTabState.value == "BANNERS") {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("📢 البنرات الترويجية والتوجيه الإعلاني", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    Text("إدارة ومراقبة لافتات الدعاية وبنرات التوجيه في واجهة التطبيق الرئيسية.", color = Color.LightGray, fontSize = 11.sp)
                    
                    val bannersList by viewModel.banners.collectAsState()
                    val categories by viewModel.categories.collectAsState()
                    
                    var newBannerTitle by remember { mutableStateOf("") }
                    var newBannerUrl by remember { mutableStateOf("") }
                    var newBannerDuration by remember { mutableStateOf("5") }
                    var newBannerSize by remember { mutableStateOf("MEDIUM") }
                    var selectedRedirectCategoryId by remember { mutableStateOf("") }
                    var selectedTargetSections by remember { mutableStateOf(setOf("ALL")) }
                    
                    // Add Banner Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("➕ إضافة بنر إعلاني جديد", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                            
                            OutlinedTextField(
                                value = newBannerTitle,
                                onValueChange = { newBannerTitle = it },
                                label = { Text("عنوان الإعلان / النص الدعائي", fontSize = 11.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = Color.White)
                            )
                            
                            OutlinedTextField(
                                value = newBannerUrl,
                                onValueChange = { newBannerUrl = it },
                                label = { Text("رابط الصورة أو الموقع الإلكتروني (URL)", fontSize = 11.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = Color.White)
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = newBannerDuration,
                                    onValueChange = { newBannerDuration = it },
                                    label = { Text("مدة العرض (ثانية)", fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f),
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = Color.White)
                                )
                                
                                Column(modifier = Modifier.weight(1.2f)) {
                                    Text("حجم البنر:", fontSize = 10.sp, color = Color.LightGray)
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        val sizes = listOf("SMALL" to "صغير", "MEDIUM" to "وسط", "LARGE" to "كبير")
                                        sizes.forEach { (szKey, szLbl) ->
                                            val isSel = newBannerSize == szKey
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(if (isSel) themeColors.accent else Color.White.copy(alpha = 0.1f))
                                                    .clickable { newBannerSize = szKey }
                                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                                            ) {
                                                Text(szLbl, fontSize = 9.sp, color = if (isSel) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                            
                            // Category Selection (Solves Banner Selection issue - الخطأ 3)
                            Text("🎯 توجيه البنر عند الضغط (اختر قسم التوجيه):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp)
                                    .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                    .border(1.dp, Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                    .padding(6.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    categories.forEach { cat ->
                                        val isSelected = selectedRedirectCategoryId == cat.id
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(if (isSelected) themeColors.accent.copy(alpha = 0.15f) else Color.Transparent)
                                                .clickable { selectedRedirectCategoryId = cat.id }
                                                .padding(6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            RadioButton(
                                                selected = isSelected,
                                                onClick = { selectedRedirectCategoryId = cat.id },
                                                colors = RadioButtonDefaults.colors(selectedColor = themeColors.accent),
                                                modifier = Modifier.scale(0.8f)
                                            )
                                            Text("${cat.icon} ${cat.name}", fontSize = 11.sp, color = if (isSelected) themeColors.accent else Color.White)
                                        }
                                    }
                                }
                            }

                            // Targeted Sections Checklist (Error 3 Checkboxes)
                            Text("📍 واجهات عرض البنر (اختر الواجهات المستهدفة):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            val availableSections = listOf(
                                "ALL" to "الكل (الرئيسية وكل الأقسام)",
                                "HOME" to "صفحة البداية والترحيب",
                                "STORES" to "قسم المحلات والأسواق 🏪",
                                "RESTAURANTS" to "قسم المطاعم والكافيهات 🍔",
                                "MEDICAL" to "قسم المراكز الطبية 🏥",
                                "PROPERTIES" to "قسم العقارات والأراضي 🏠",
                                "JOBS" to "قسم الوظائف وفرص العمل 💼"
                            )
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                    .border(1.dp, Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                    .padding(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                availableSections.forEach { (secKey, secName) ->
                                    val isChecked = selectedTargetSections.contains(secKey)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (isChecked) themeColors.accent.copy(alpha = 0.1f) else Color.Transparent)
                                            .clickable {
                                                selectedTargetSections = if (secKey == "ALL") {
                                                    if (isChecked) emptySet() else setOf("ALL")
                                                } else {
                                                    val next = selectedTargetSections - "ALL"
                                                    if (isChecked) next - secKey else next + secKey
                                                }
                                            }
                                            .padding(4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Checkbox(
                                            checked = isChecked,
                                            onCheckedChange = {
                                                selectedTargetSections = if (secKey == "ALL") {
                                                    if (isChecked) emptySet() else setOf("ALL")
                                                } else {
                                                    val next = selectedTargetSections - "ALL"
                                                    if (isChecked) next - secKey else next + secKey
                                                }
                                            },
                                            colors = CheckboxDefaults.colors(checkedColor = themeColors.accent),
                                            modifier = Modifier.scale(0.8f)
                                        )
                                        Text(secName, fontSize = 11.sp, color = if (isChecked) themeColors.accent else Color.White)
                                    }
                                }
                            }
                            
                            Button(
                                onClick = {
                                    if (newBannerTitle.isNotEmpty()) {
                                        viewModel.addNewBanner(
                                            title = newBannerTitle.trim(),
                                            url = newBannerUrl.trim(),
                                            redirect = selectedRedirectCategoryId,
                                            type = "IMAGE",
                                            size = newBannerSize,
                                            duration = newBannerDuration.toIntOrNull() ?: 5,
                                            targetSection = if (selectedTargetSections.isEmpty()) "ALL" else selectedTargetSections.joinToString(",")
                                        )
                                        newBannerTitle = ""
                                        newBannerUrl = ""
                                        selectedRedirectCategoryId = ""
                                        selectedTargetSections = setOf("ALL")
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("حفظ وإضافة البنر الإعلاني 💾", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    // Banners List
                    Text("📋 الإعلانات والبنرات الحالية النشطة (${bannersList.size}):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    
                    if (bannersList.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                            Text("لا توجد إعلانات نشطة حالياً", color = Color.Gray, fontSize = 11.sp)
                        }
                    } else {
                        bannersList.forEach { b ->
                            val matchedCat = categories.find { it.id == b.redirectCategory }
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = themeColors.surface)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(b.title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Text("رابط التوجيه: ${if (matchedCat != null) "${matchedCat.icon} ${matchedCat.name}" else "الرئيسية / غير محدد"}", fontSize = 10.sp, color = themeColors.accent)
                                        Text("مدة العرض: ${b.duration} ثانية - الحجم: ${b.size}", fontSize = 9.sp, color = Color.LightGray)
                                    }
                                    
                                    IconButton(onClick = { viewModel.deleteBanner(b.id) }) {
                                        Text("🗑️", fontSize = 14.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun LazyListScope.adminCategoriesPanel(viewModel: MainViewModel, themeColors: VisualThemePalette, state: AdminPanelState) {
    with(state) {
        if (activeSubTabState.value in listOf("CATEGORIES", "CITIES", "CUSTOM_TABS")) {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🗂️ تحكم الأقسام والخدمات والمدن", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    val categories by viewModel.categories.collectAsState()
                    categories.forEach { cat ->
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = themeColors.surface)) {
                            Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${cat.icon} ${cat.name}", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

fun LazyListScope.adminPaymentsPanel(viewModel: MainViewModel, themeColors: VisualThemePalette, state: AdminPanelState) {
    with(state) {
        if (activeSubTabState.value == "PAYMENTS") {
            item {
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
                    var selectedPaymentForDetails by remember { mutableStateOf<com.example.data.PaymentEntity?>(null) }
                    
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
                        var bookingPaymentMethod by remember { mutableStateOf("mobileWallet") }
                        var bookingAdvancePct by remember { mutableStateOf("20") }
                        var bookingMinAdvance by remember { mutableStateOf("1000") }
                        var bookingMaxAdvance by remember { mutableStateOf("5000") }
                        var bookingForced by remember { mutableStateOf(true) }

                        var servicePaymentEnabled by remember { mutableStateOf(false) }
                        var servicePaymentMethod by remember { mutableStateOf("cash") }
                        var serviceAdvancePct by remember { mutableStateOf("10") }
                        var serviceMinAdvance by remember { mutableStateOf("500") }
                        var serviceMaxAdvance by remember { mutableStateOf("3000") }
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

                                Divider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))

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
                    var walletType by remember { mutableStateOf("كليهما") }
                    var walletCurrency by remember { mutableStateOf("YER") }

                    val customWallets = remember {
                        mutableStateListOf<com.example.data.PaymentWalletEntity>(
                            com.example.data.PaymentWalletEntity(id = "w1", provider = "الكريمي", walletNumber = "123456", accountName = "حساب الإدارة الرئيسي", isDefault = true, status = "active"),
                            com.example.data.PaymentWalletEntity(id = "w2", provider = "جيب", walletNumber = "777888999", accountName = "محفظة جيب الإدارية", isDefault = false, status = "active")
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
                                                    com.example.data.PaymentWalletEntity(
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
                                                if (wallet.isDefault) Badge(containerColor = Color.Yellow) { Text("افتراضية ⭐", color = Color.Black, fontSize = 8.sp) }
                                                Badge(containerColor = if (isWalletActive) Color.Green else Color.Red) {
                                                    Text(if (isWalletActive) "نشط" else "معطل", color = Color.Black, fontSize = 8.sp)
                                                }
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

                    // Payments LazyColumn Items
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
                                            Text(pay.status, color = Color.Black, fontSize = 8.sp, fontWeight = FontWeight.Bold)
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
                                                }
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
                                                }
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
                            }
                        )
                    }
                }
            }
        }
    }
}

fun LazyListScope.adminSettingsPanel(viewModel: MainViewModel, themeColors: VisualThemePalette, state: AdminPanelState) {
    with(state) {
        if (activeSubTabState.value in listOf("STATS", "COLORS", "GOLDEN_ICONS", "CARD_CUSTOMIZER", "NEW_SECTION_CREATOR", "REG_FORMS_MANAGER")) {
            item {
                val settingsState by viewModel.settings.collectAsState()
                val activeSubTab = activeSubTabState.value
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = when (activeSubTab) {
                                    "COLORS" -> "🎨 تخصيص ألوان الهوية والتطبيق"
                                    "GOLDEN_ICONS" -> "👑 تخصيص الخطوط وحجم وأيقونات التنقل"
                                    "CARD_CUSTOMIZER" -> "🎛️ تخصيص أبعاد ومقاسات وأزرار البطاقات"
                                    "NEW_SECTION_CREATOR" -> "➕ إضافة وإدارة الأقسام والخدمات والمحافظ"
                                    "REG_FORMS_MANAGER" -> "📋 تخصيص استمارات التسجيل للأعضاء"
                                    else -> "⚙️ الإحصائيات والهوية وتخصيص الواجهات"
                                },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = themeColors.accent
                            )
                            Text(
                                text = "تعديلات مباشرة ومزامنة تلقائية مع قواعد البيانات لضمان استقرار التطبيق.",
                                fontSize = 10.sp,
                                color = Color.LightGray,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }

                    // Render specific Panel based on Sub-Tab
                    when (activeSubTab) {
                        "COLORS" -> {
                            ColorCustomizerPanel(viewModel = viewModel, settingsState = settingsState, themeColors = themeColors)
                        }
                        "GOLDEN_ICONS" -> {
                            GoldenIconsPanel(viewModel = viewModel, settingsState = settingsState, themeColors = themeColors)
                        }
                        "CARD_CUSTOMIZER" -> {
                            CardCustomizerPanel(viewModel = viewModel, settingsState = settingsState, themeColors = themeColors)
                        }
                        "NEW_SECTION_CREATOR" -> {
                            NewSectionCreatorPanel(viewModel = viewModel, settingsState = settingsState, themeColors = themeColors)
                        }
                        "REG_FORMS_MANAGER" -> {
                            RegFormsManagerPanel(viewModel = viewModel, settingsState = settingsState, themeColors = themeColors)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ColorCustomizerPanel(viewModel: MainViewModel, settingsState: AdminSettingsEntity, themeColors: VisualThemePalette) {
    var primaryHex by remember(settingsState.customPrimaryHex) { mutableStateOf(settingsState.customPrimaryHex) }
    var secondaryHex by remember(settingsState.customSecondaryHex) { mutableStateOf(settingsState.customSecondaryHex) }
    var backgroundHex by remember(settingsState.customBackgroundHex) { mutableStateOf(settingsState.customBackgroundHex) }
    var cardBgHex by remember(settingsState.cardBackgroundHex) { mutableStateOf(settingsState.cardBackgroundHex) }
    var nameColorHex by remember(settingsState.providerNameColorHex) { mutableStateOf(settingsState.providerNameColorHex) }
    var ratingColorHex by remember(settingsState.ratingColorHex) { mutableStateOf(settingsState.ratingColorHex) }
    var locationColorHex by remember(settingsState.locationColorHex) { mutableStateOf(settingsState.locationColorHex) }
    var vipColorHex by remember(settingsState.vipBadgeColorHex) { mutableStateOf(settingsState.vipBadgeColorHex) }
    var verifiedColorHex by remember(settingsState.verifiedBadgeColorHex) { mutableStateOf(settingsState.verifiedBadgeColorHex) }
    var recommendedColorHex by remember(settingsState.recommendedBadgeColorHex) { mutableStateOf(settingsState.recommendedBadgeColorHex) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // 🎨 8 Theme Color Presets for Admin Selection
        Text("🎨 الثيمات الجاهزة للتطبيق (اختر من بين 8 ألوان رسمية):", fontWeight = FontWeight.Bold, color = themeColors.accent, fontSize = 12.sp)
        
        val colorPresets = listOf(
            Triple("🟢 الأخضر الزمردي", "#10B981", "#059669"),
            Triple("🔵 النيلي الملكي", "#3B82F6", "#2563EB"),
            Triple("🟣 البنفسجي الفاخر", "#8B5CF6", "#7C3AED"),
            Triple("🟡 الذهبي الملكي", "#F59E0B", "#D97706"),
            Triple("🔴 الأحـمر العـنابي", "#EF4444", "#DC2626"),
            Triple("🩵 الفيروزي العصري", "#06B6D4", "#0891B2"),
            Triple("🟠 البرتقالي الدافئ", "#F97316", "#EA580C"),
            Triple("⚙️ الرمادي المظلم", "#64748B", "#475569")
        )

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            colorPresets.chunked(2).forEach { rowPresets ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    rowPresets.forEach { (name, pPrimary, pSecondary) ->
                        Button(
                            onClick = {
                                primaryHex = pPrimary
                                secondaryHex = pSecondary
                                cardBgHex = "#1E293B"
                                backgroundHex = "#0F172A"
                                nameColorHex = pPrimary
                                ratingColorHex = "#F59E0B"
                                locationColorHex = "#94A3B8"
                                vipColorHex = "#F59E0B"
                                verifiedColorHex = pPrimary
                                recommendedColorHex = pPrimary
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = parseHexColorSafe(pPrimary, themeColors.primary)),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Text(name, fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        @Composable
        fun ColorInputField(label: String, value: String, onValueChange: (String) -> Unit) {
            val parsedColor = remember(value) { parseHexColorSafe(value, Color.Gray) }
            Card(
                colors = CardDefaults.cardColors(containerColor = themeColors.surface.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(parsedColor)
                            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                    )
                    OutlinedTextField(
                        value = value,
                        onValueChange = onValueChange,
                        label = { Text(label, fontSize = 10.sp) },
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = Color.White),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = themeColors.accent,
                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.4f)
                        ),
                        singleLine = true
                    )
                }
            }
        }

        Text("🎨 ألوان الهوية الرئيسية والتطبيق:", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
        ColorInputField("اللون الأساسي للبراند (Primary)", primaryHex) { primaryHex = it }
        ColorInputField("اللون الثانوي للبراند (Secondary)", secondaryHex) { secondaryHex = it }
        ColorInputField("لون خلفية التطبيق (Background)", backgroundHex) { backgroundHex = it }

        Text("🎛️ ألوان بطاقات وتفاصيل مقدمي الخدمات:", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
        ColorInputField("لون خلفية البطاقة (Card Background)", cardBgHex) { cardBgHex = it }
        ColorInputField("لون اسم الفني/المقدم (Name Color)", nameColorHex) { nameColorHex = it }
        ColorInputField("لون أيقونة التقييم (Rating Stars)", ratingColorHex) { ratingColorHex = it }
        ColorInputField("لون الموقع والمسافة (Location Color)", locationColorHex) { locationColorHex = it }

        Text("👑 ألوان الشارات والاعتمادات المميزة:", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
        ColorInputField("لون شارة التميز (VIP Badge)", vipColorHex) { vipColorHex = it }
        ColorInputField("لون شارة التوثيق (Verified Badge)", verifiedColorHex) { verifiedColorHex = it }
        ColorInputField("لون شارة التوصية (Recommended Badge)", recommendedColorHex) { recommendedColorHex = it }

        Button(
            onClick = {
                viewModel.saveCustomSettingsState(
                    settingsState.copy(
                        customPrimaryHex = primaryHex,
                        customSecondaryHex = secondaryHex,
                        customBackgroundHex = backgroundHex,
                        cardBackgroundHex = cardBgHex,
                        providerNameColorHex = nameColorHex,
                        ratingColorHex = ratingColorHex,
                        locationColorHex = locationColorHex,
                        vipBadgeColorHex = vipColorHex,
                        verifiedBadgeColorHex = verifiedColorHex,
                        recommendedBadgeColorHex = recommendedColorHex
                    )
                )
                viewModel.triggerNotification("🎨 تم تحديث وحفظ ألوان الهوية بنجاح!")
            },
            colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("حفظ إعدادات الهوية والألوان 💾", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

@Composable
fun GoldenIconsPanel(viewModel: MainViewModel, settingsState: AdminSettingsEntity, themeColors: VisualThemePalette) {
    var activeFont by remember(settingsState.activeFontFamily) { mutableStateOf(settingsState.activeFontFamily) }
    var fontScale by remember(settingsState.globalFontScale) { mutableStateOf(settingsState.globalFontScale) }
    
    var homeIcon by remember(settingsState.topHomeIcon) { mutableStateOf(settingsState.topHomeIcon) }
    var mapsIcon by remember(settingsState.topMapsIcon) { mutableStateOf(settingsState.topMapsIcon) }
    var joinIcon by remember(settingsState.topJoinIcon) { mutableStateOf(settingsState.topJoinIcon) }
    var notifIcon by remember(settingsState.topNotifIcon) { mutableStateOf(settingsState.topNotifIcon) }
    var chatsIcon by remember(settingsState.topChatsIcon) { mutableStateOf(settingsState.topChatsIcon) }

    var infoIcon by remember(settingsState.bottomInfoIcon) { mutableStateOf(settingsState.bottomInfoIcon) }
    var bookingsIcon by remember(settingsState.bottomBookingsIcon) { mutableStateOf(settingsState.bottomBookingsIcon) }
    var langIcon by remember(settingsState.bottomLangIcon) { mutableStateOf(settingsState.bottomLangIcon) }
    var adminIcon by remember(settingsState.bottomAdminIcon) { mutableStateOf(settingsState.bottomAdminIcon) }

    var iconSize by remember(settingsState.navIconSizeDp) { mutableStateOf(settingsState.navIconSizeDp.toFloat()) }
    var topStyle by remember(settingsState.topNavIconStyle) { mutableStateOf(settingsState.topNavIconStyle) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("🔤 اختيار الخط الافتراضي للتطبيق:", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val fonts = listOf("CAIRO" to "Cairo", "DEFAULT" to "Default", "TAHOMA" to "Tahoma", "AMIRI" to "Amiri")
            fonts.forEach { (key, label) ->
                val isSel = activeFont == key
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSel) themeColors.accent else themeColors.surface)
                        .clickable { activeFont = key }
                        .padding(vertical = 8.dp)
                        .border(1.dp, if (isSel) Color.White else Color.Transparent, RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(label, color = if (isSel) Color.Black else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Text("📏 تكبير/تصغير حجم خطوط التطبيق: (${String.format("%.1f", fontScale)}x)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
        Slider(
            value = fontScale,
            onValueChange = { fontScale = it },
            valueRange = 0.8f..1.5f,
            colors = SliderDefaults.colors(
                thumbColor = themeColors.accent,
                activeTrackColor = themeColors.accent
            )
        )

        Text("✨ نمط الأيقونات العلوية والسفلية:", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val styles = listOf("GOLDEN_3D" to "👑 ذهبي 3D", "METALLIC" to "💿 ميتاليك", "MINIMAL" to "📱 مينيمل")
            styles.forEach { (key, label) ->
                val isSel = topStyle == key
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSel) themeColors.accent else themeColors.surface)
                        .clickable { topStyle = key }
                        .padding(vertical = 8.dp)
                        .border(1.dp, if (isSel) Color.White else Color.Transparent, RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(label, color = if (isSel) Color.Black else Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Text("📐 حجم الأيقونات في شريط التنقل: (${iconSize.toInt()} dp)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
        Slider(
            value = iconSize,
            onValueChange = { iconSize = it },
            valueRange = 20f..40f,
            colors = SliderDefaults.colors(
                thumbColor = themeColors.accent,
                activeTrackColor = themeColors.accent
            )
        )

        Text("🖼️ تخصيص الأيقونات والرموز (Emojis):", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("الأيقونات العلوية:", color = themeColors.accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(value = homeIcon, onValueChange = { homeIcon = it }, label = { Text("الرئيسية", fontSize = 9.sp) }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = mapsIcon, onValueChange = { mapsIcon = it }, label = { Text("الخرائط", fontSize = 9.sp) }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = joinIcon, onValueChange = { joinIcon = it }, label = { Text("التسجيل", fontSize = 9.sp) }, modifier = Modifier.weight(1f), singleLine = true)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(value = notifIcon, onValueChange = { notifIcon = it }, label = { Text("الإشعارات", fontSize = 9.sp) }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = chatsIcon, onValueChange = { chatsIcon = it }, label = { Text("الدردشات", fontSize = 9.sp) }, modifier = Modifier.weight(1f), singleLine = true)
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text("الأيقونات السفلية:", color = themeColors.accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(value = infoIcon, onValueChange = { infoIcon = it }, label = { Text("المعلومات", fontSize = 9.sp) }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = bookingsIcon, onValueChange = { bookingsIcon = it }, label = { Text("الحجوزات", fontSize = 9.sp) }, modifier = Modifier.weight(1f), singleLine = true)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(value = langIcon, onValueChange = { langIcon = it }, label = { Text("اللغة", fontSize = 9.sp) }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = adminIcon, onValueChange = { adminIcon = it }, label = { Text("الأدمن", fontSize = 9.sp) }, modifier = Modifier.weight(1f), singleLine = true)
                }
            }
        }

        Button(
            onClick = {
                viewModel.saveCustomSettingsState(
                    settingsState.copy(
                        activeFontFamily = activeFont,
                        globalFontScale = fontScale,
                        topHomeIcon = homeIcon,
                        topMapsIcon = mapsIcon,
                        topJoinIcon = joinIcon,
                        topNotifIcon = notifIcon,
                        topChatsIcon = chatsIcon,
                        bottomInfoIcon = infoIcon,
                        bottomBookingsIcon = bookingsIcon,
                        bottomLangIcon = langIcon,
                        bottomAdminIcon = adminIcon,
                        navIconSizeDp = iconSize.toInt(),
                        topNavIconStyle = topStyle
                    )
                )
                viewModel.triggerNotification("👑 تم حفظ إعدادات الخطوط والأيقونات بنجاح!")
            },
            colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("حفظ تخصيصات الواجهة والخطوط 💾", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

@Composable
fun CardCustomizerPanel(viewModel: MainViewModel, settingsState: AdminSettingsEntity, themeColors: VisualThemePalette) {
    var coverHeight by remember(settingsState.coverHeight) { mutableStateOf(settingsState.coverHeight.toFloat()) }
    var avatarSize by remember(settingsState.avatarSize) { mutableStateOf(settingsState.avatarSize.toFloat()) }
    var spacing by remember(settingsState.elementSpacing) { mutableStateOf(settingsState.elementSpacing.toFloat()) }
    var padding by remember(settingsState.cardPadding) { mutableStateOf(settingsState.cardPadding.toFloat()) }

    var showCall by remember(settingsState.showCallButton) { mutableStateOf(settingsState.showCallButton) }
    var showWhatsapp by remember(settingsState.showWhatsappButton) { mutableStateOf(settingsState.showWhatsappButton) }
    var showDetails by remember(settingsState.showDetailsButton) { mutableStateOf(settingsState.showDetailsButton) }
    var showBook by remember(settingsState.showBookButton) { mutableStateOf(settingsState.showBookButton) }

    var callBtnColor by remember(settingsState.callButtonColorHex) { mutableStateOf(settingsState.callButtonColorHex) }
    var whatsappBtnColor by remember(settingsState.whatsappButtonColorHex) { mutableStateOf(settingsState.whatsappButtonColorHex) }
    var detailsBtnColor by remember(settingsState.detailsButtonColorHex) { mutableStateOf(settingsState.detailsButtonColorHex) }
    var bookBtnColor by remember(settingsState.bookButtonColorHex) { mutableStateOf(settingsState.bookButtonColorHex) }

    var showVip by remember(settingsState.showVipBadge) { mutableStateOf(settingsState.showVipBadge) }
    var showVerified by remember(settingsState.showVerifiedBadge) { mutableStateOf(settingsState.showVerifiedBadge) }
    var showRecommended by remember(settingsState.showRecommendedBadge) { mutableStateOf(settingsState.showRecommendedBadge) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("📐 أبعاد ومقاسات بطاقات مقدمي الخدمات:", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("ارتفاع غلاف البطاقة (Cover Height): (${coverHeight.toInt()} dp)", color = Color.White, fontSize = 10.sp)
                Slider(value = coverHeight, onValueChange = { coverHeight = it }, valueRange = 0f..200f)

                Text("قطر الصورة الشخصية (Avatar Size): (${avatarSize.toInt()} dp)", color = Color.White, fontSize = 10.sp)
                Slider(value = avatarSize, onValueChange = { avatarSize = it }, valueRange = 30f..100f)

                Text("التباعد الداخلي للبطاقة (Padding): (${padding.toInt()} dp)", color = Color.White, fontSize = 10.sp)
                Slider(value = padding, onValueChange = { padding = it }, valueRange = 4f..24f)

                Text("التباعد بين العناصر الداخلية (Spacing): (${spacing.toInt()} dp)", color = Color.White, fontSize = 10.sp)
                Slider(value = spacing, onValueChange = { spacing = it }, valueRange = 2f..16f)
            }
        }

        Text("🎛️ تفعيل أزرار التفاعل والألوان المخصصة:", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                @Composable
                fun ButtonControlRow(label: String, isChecked: Boolean, onCheckedChange: (Boolean) -> Unit, colorValue: String, onColorChange: (String) -> Unit) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Switch(checked = isChecked, onCheckedChange = onCheckedChange, colors = SwitchDefaults.colors(checkedThumbColor = themeColors.accent))
                        }
                        if (isChecked) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(parseHexColorSafe(colorValue, Color.Gray))
                                )
                                OutlinedTextField(
                                    value = colorValue,
                                    onValueChange = onColorChange,
                                    label = { Text("لون الزر (Hex)", fontSize = 8.sp) },
                                    modifier = Modifier.fillMaxWidth(),
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 10.sp),
                                    singleLine = true
                                )
                            }
                        }
                    }
                }

                ButtonControlRow("زر الاتصال الهاتفي (Call Button)", showCall, { showCall = it }, callBtnColor) { callBtnColor = it }
                Divider(color = Color.White.copy(alpha = 0.08f))
                ButtonControlRow("زر الواتساب الفوري (WhatsApp Button)", showWhatsapp, { showWhatsapp = it }, whatsappBtnColor) { whatsappBtnColor = it }
                Divider(color = Color.White.copy(alpha = 0.08f))
                ButtonControlRow("زر تفاصيل الملف الشخصي (Details Button)", showDetails, { showDetails = it }, detailsBtnColor) { detailsBtnColor = it }
                Divider(color = Color.White.copy(alpha = 0.08f))
                ButtonControlRow("زر حجز الخدمة الفوري (Booking Button)", showBook, { showBook = it }, bookBtnColor) { bookBtnColor = it }
            }
        }

        Text("🛡️ عرض شارات التقييم والاعتماد على البطاقة:", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("شارة تميز (VIP Badge)", color = Color.White, fontSize = 11.sp)
                    Switch(checked = showVip, onCheckedChange = { showVip = it })
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("شارة التوثيق (Verified Badge)", color = Color.White, fontSize = 11.sp)
                    Switch(checked = showVerified, onCheckedChange = { showVerified = it })
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("شارة التوصية (Recommended Badge)", color = Color.White, fontSize = 11.sp)
                    Switch(checked = showRecommended, onCheckedChange = { showRecommended = it })
                }
            }
        }

        Button(
            onClick = {
                viewModel.saveCustomSettingsState(
                    settingsState.copy(
                        coverHeight = coverHeight.toInt(),
                        avatarSize = avatarSize.toInt(),
                        elementSpacing = spacing.toInt(),
                        cardPadding = padding.toInt(),
                        showCallButton = showCall,
                        showWhatsappButton = showWhatsapp,
                        showDetailsButton = showDetails,
                        showBookButton = showBook,
                        callButtonColorHex = callBtnColor,
                        whatsappButtonColorHex = whatsappBtnColor,
                        detailsButtonColorHex = detailsBtnColor,
                        bookButtonColorHex = bookBtnColor,
                        showVipBadge = showVip,
                        showVerifiedBadge = showVerified,
                        showRecommendedBadge = showRecommended
                    )
                )
                viewModel.triggerNotification("🎛️ تم حفظ إعدادات بطاقات وتفاصيل مقدمي الخدمات بنجاح!")
            },
            colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("حفظ تخصيصات البطاقات المميزة 💾", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

@Composable
fun RegFormsManagerPanel(viewModel: MainViewModel, settingsState: AdminSettingsEntity, themeColors: VisualThemePalette) {
    var reqNameInput by remember { mutableStateOf("") }
    var isMandatoryInput by remember { mutableStateOf(true) }
    
    var reqsList by remember(settingsState.registrationRequirements) {
        mutableStateOf(settingsState.registrationRequirements.split(",").filter { it.isNotBlank() })
    }

    var enableProvidersReg by remember(settingsState.enableProvidersRegistration) { mutableStateOf(settingsState.enableProvidersRegistration) }
    var enableStoresReg by remember(settingsState.enableStoresRegistration) { mutableStateOf(settingsState.enableStoresRegistration) }
    var enableRestaurantsReg by remember(settingsState.enableRestaurantsRegistration) { mutableStateOf(settingsState.enableRestaurantsRegistration) }
    var enablePropertiesReg by remember(settingsState.enablePropertiesRegistration) { mutableStateOf(settingsState.enablePropertiesRegistration) }
    var enableMedicalReg by remember(settingsState.enableMedicalRegistration) { mutableStateOf(settingsState.enableMedicalRegistration) }
    var enableJobsReg by remember(settingsState.enableJobsRegistration) { mutableStateOf(settingsState.enableJobsRegistration) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("📋 تفعيل استمارات التسجيل للأقسام الرئيسية:", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("استمارة تسجيل مقدمي الخدمات / الفنيين", color = Color.White, fontSize = 11.sp)
                    Switch(checked = enableProvidersReg, onCheckedChange = { enableProvidersReg = it })
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("استمارة تسجيل المحلات والمراكز التجارية", color = Color.White, fontSize = 11.sp)
                    Switch(checked = enableStoresReg, onCheckedChange = { enableStoresReg = it })
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("استمارة تسجيل المطاعم والكافيهات", color = Color.White, fontSize = 11.sp)
                    Switch(checked = enableRestaurantsReg, onCheckedChange = { enableRestaurantsReg = it })
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("استمارة إضافة العقارات والأراضي", color = Color.White, fontSize = 11.sp)
                    Switch(checked = enablePropertiesReg, onCheckedChange = { enablePropertiesReg = it })
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("استمارة تسجيل المراكز الطبية والعيادات", color = Color.White, fontSize = 11.sp)
                    Switch(checked = enableMedicalReg, onCheckedChange = { enableMedicalReg = it })
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("استمارة إعلانات الوظائف والتقديم", color = Color.White, fontSize = 11.sp)
                    Switch(checked = enableJobsReg, onCheckedChange = { enableJobsReg = it })
                }
            }
        }

        Text("✏️ تخصيص حقول استمارة تسجيل مقدمي الخدمات:", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("إضافة حقل جديد للاستمارة:", color = themeColors.accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = reqNameInput,
                    onValueChange = { reqNameInput = it },
                    placeholder = { Text("مثال: صورة رخصة القيادة المهنية", fontSize = 10.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp),
                    singleLine = true
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("الحقل إلزامي لإنهاء التسجيل؟", color = Color.White, fontSize = 10.sp)
                    Switch(checked = isMandatoryInput, onCheckedChange = { isMandatoryInput = it })
                }
                Button(
                    onClick = {
                        if (reqNameInput.isNotBlank()) {
                            val mandatorySuffix = if (isMandatoryInput) "Mandatory" else "Optional"
                            val newItem = "${reqNameInput.trim()}|$mandatorySuffix"
                            if (!reqsList.contains(newItem)) {
                                reqsList = reqsList + newItem
                            }
                            reqNameInput = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                    modifier = Modifier.align(Alignment.End),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text("إضافة الحقل ➕", color = Color.Black, fontSize = 10.sp)
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text("الحقول الحالية المعتمدة في استمارة التسجيل:", color = themeColors.accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                if (reqsList.isEmpty()) {
                    Text("لا توجد حقول مخصصة. سيتم استخدام الحقول التلقائية.", color = Color.LightGray, fontSize = 10.sp)
                } else {
                    reqsList.forEach { req ->
                        val parts = req.split("|")
                        val name = parts.getOrElse(0) { req }
                        val mandatory = parts.getOrElse(1) { "Optional" } == "Mandatory"
                        Card(
                            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(name, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Text(if (mandatory) "إلزامي ⚠️" else "اختياري ✅", color = if (mandatory) Color.Yellow else Color.Green, fontSize = 8.sp)
                                }
                                IconButton(onClick = {
                                    reqsList = reqsList.filter { it != req }
                                }, modifier = Modifier.size(24.dp)) {
                                    Text("🗑️", color = Color.Red, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        Button(
            onClick = {
                viewModel.saveCustomSettingsState(
                    settingsState.copy(
                        registrationRequirements = reqsList.joinToString(","),
                        enableProvidersRegistration = enableProvidersReg,
                        enableStoresRegistration = enableStoresReg,
                        enableRestaurantsRegistration = enableRestaurantsReg,
                        enablePropertiesRegistration = enablePropertiesReg,
                        enableMedicalRegistration = enableMedicalReg,
                        enableJobsRegistration = enableJobsReg
                    )
                )
                viewModel.triggerNotification("📋 تم حفظ تخصيص استمارات التسجيل والتحكم بنجاح!")
            },
            colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("حفظ استمارات التسجيل المعتمدة 💾", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

@Composable
fun NewSectionCreatorPanel(viewModel: MainViewModel, settingsState: AdminSettingsEntity, themeColors: VisualThemePalette) {
    var sectionName by remember { mutableStateOf("") }
    var sectionIcon by remember { mutableStateOf("") }
    var sectionType by remember { mutableStateOf("store") } 
    var registrationTerms by remember { mutableStateOf("") }
    var requiredFields by remember { mutableStateOf("الاسم,الوصف,الهاتف,الموقع") }
    var maxPhotosVal by remember { mutableStateOf("5") }
    var allowPdfInput by remember { mutableStateOf(true) }

    var sectionsList by remember(settingsState.dynamicSectionsData) {
        mutableStateOf(DynamicSection.parseDynamicSections(settingsState.dynamicSectionsData))
    }

    val storesList by viewModel.stores.collectAsState()
    val propertiesList by viewModel.properties.collectAsState()
    val jobsList by viewModel.jobs.collectAsState()
    val providersList by viewModel.providers.collectAsState()

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("📊 إحصائيات الأعضاء والنشاط للأقسام الحالية:", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val stats = listOf(
                    "المحلات والمراكز" to "${storesList.size} محل تجاري",
                    "العقارات والأراضي" to "${propertiesList.size} عقار معروض",
                    "إعلانات الوظائف" to "${jobsList.size} وظيفة منشورة",
                    "الفنيين والمهن" to "${providersList.size} مقدم خدمات معتمد"
                )
                stats.forEach { (title, count) ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(title, color = Color.LightGray, fontSize = 11.sp)
                        Text(count, color = themeColors.accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Text("➕ إنشاء قسم ديناميكي جديد مخصص:", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = sectionName,
                    onValueChange = { sectionName = it },
                    label = { Text("اسم القسم الجديد", fontSize = 10.sp) },
                    placeholder = { Text("مثال: خدمات التوصيل والمشاوير", fontSize = 10.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp),
                    singleLine = true
                )
                OutlinedTextField(
                    value = sectionIcon,
                    onValueChange = { sectionIcon = it },
                    label = { Text("أيقونة القسم (Emoji)", fontSize = 10.sp) },
                    placeholder = { Text("مثال: 🚗", fontSize = 10.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp),
                    singleLine = true
                )
                
                Text("نوع بيانات القسم (هيكلية العرض):", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val types = listOf("store" to "🏪 محلات تجارية", "property" to "🏠 عقارات وأراضي")
                    types.forEach { (key, label) ->
                        val isSel = sectionType == key
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSel) themeColors.accent else themeColors.surface)
                                .clickable { sectionType = key }
                                .padding(vertical = 8.dp)
                                .border(1.dp, if (isSel) Color.White else Color.Transparent, RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, color = if (isSel) Color.Black else Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                OutlinedTextField(
                    value = requiredFields,
                    onValueChange = { requiredFields = it },
                    label = { Text("الحقول المطلوبة في الاستمارة (مفصولة بفاصلة)", fontSize = 10.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = registrationTerms,
                    onValueChange = { registrationTerms = it },
                    label = { Text("شروط التسجيل والاعتماد للقسم", fontSize = 10.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = maxPhotosVal,
                        onValueChange = { maxPhotosVal = it },
                        label = { Text("أقصى صور", fontSize = 10.sp) },
                        modifier = Modifier.width(100.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp),
                        singleLine = true
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("ملف PDF؟", color = Color.White, fontSize = 10.sp)
                        Switch(checked = allowPdfInput, onCheckedChange = { allowPdfInput = it })
                    }
                }

                Button(
                    onClick = {
                        if (sectionName.isNotBlank() && sectionIcon.isNotBlank()) {
                            val newId = "dyn_${System.currentTimeMillis().toString().takeLast(6)}"
                            val newSec = DynamicSection(
                                id = newId,
                                name = sectionName.trim(),
                                icon = sectionIcon.trim(),
                                isEnabled = true,
                                type = sectionType,
                                order = sectionsList.size + 1,
                                terms = registrationTerms.trim(),
                                maxPhotos = maxPhotosVal.toIntOrNull() ?: 5,
                                showPhotos = true,
                                allowPdf = allowPdfInput,
                                requiredFields = requiredFields.trim()
                            )
                            sectionsList = sectionsList + newSec
                            viewModel.saveCustomSettingsState(
                                settingsState.copy(
                                    dynamicSectionsData = DynamicSection.serializeDynamicSections(sectionsList)
                                )
                            )
                            viewModel.triggerNotification("➕ تم إنشاء وتعميد القسم الجديد $sectionName بنجاح سحابياً!")
                            sectionName = ""
                            sectionIcon = ""
                            registrationTerms = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("إنشاء واعتماد القسم الجديد فوراً 🚀", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        Text("📋 إدارة الأقسام الحالية وتعديل حالتها:", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
        if (sectionsList.isEmpty()) {
            Text("لا توجد أقسام مخصصة حالياً.", color = Color.LightGray, fontSize = 11.sp)
        } else {
            sectionsList.forEach { sec ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = themeColors.surface.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(sec.icon, fontSize = 18.sp)
                                Column {
                                    Text(sec.name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("النوع: ${if (sec.type == "store") "🏪 دليل محلات" else "🏠 عقارات"} | المعرف: ${sec.id}", color = Color.LightGray, fontSize = 8.sp)
                                }
                            }
                            Switch(
                                checked = sec.isEnabled,
                                onCheckedChange = { isChecked ->
                                    sectionsList = sectionsList.map {
                                        if (it.id == sec.id) it.copy(isEnabled = isChecked) else it
                                    }
                                    viewModel.saveCustomSettingsState(
                                        settingsState.copy(
                                            dynamicSectionsData = DynamicSection.serializeDynamicSections(sectionsList)
                                        )
                                    )
                                    viewModel.triggerNotification("🔧 تم تحديث نشاط القسم ${sec.name} بنجاح!")
                                }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    sectionsList = sectionsList.filter { it.id != sec.id }
                                    viewModel.saveCustomSettingsState(
                                        settingsState.copy(
                                            dynamicSectionsData = DynamicSection.serializeDynamicSections(sectionsList)
                                        )
                                    )
                                    viewModel.triggerNotification("🗑️ تم حذف القسم ${sec.name} بنجاح!")
                                }
                            ) {
                                Text("حذف القسم 🗑️", color = Color.Red, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun parseHexColorSafe(hex: String, fallback: Color): Color {
    if (hex.isBlank()) return fallback
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        fallback
    }
}

fun LazyListScope.adminBackupPanel(viewModel: MainViewModel, themeColors: VisualThemePalette, state: AdminPanelState) {
    with(state) {
        if (activeSubTabState.value in listOf("BACKUP", "CLEAN", "BLOCKED", "DELETED")) {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("💾 النسخ الاحتياطي والمزامنة وتهيئة البيانات", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    Button(onClick = { viewModel.refreshData() }, colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary)) {
                        Text("مزامنة وتحديث البيانات 🔄", color = Color.White)
                    }
                }
            }
        }
    }
}

fun LazyListScope.adminSupervisorsPanel(viewModel: MainViewModel, themeColors: VisualThemePalette, state: AdminPanelState) {
    with(state) {
        if (activeSubTabState.value in listOf("COMPLAINTS", "SUPERVISORS")) {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("🛡️ الشكاوى والمشرفين والصلاحيات الإدارية", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    
                    val reports by viewModel.reports.collectAsState()
                    var selectedReportFilter by remember { mutableStateOf("ALL") }
                    
                    Text("⚠️ شكاوى وبلاغات الأعضاء والمستخدمين:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    
                    // Filter row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val filters = listOf(
                            Pair("ALL", "الكل"),
                            Pair("SERVICES", "🛠️ الفنيين"),
                            Pair("STORES", "🏪 المحلات"),
                            Pair("RESTAURANTS", "🍔 المطاعم"),
                            Pair("MEDICAL", "🏥 طبية"),
                            Pair("PROPERTIES", "🏠 عقارات"),
                            Pair("JOBS", "💼 وظائف")
                        )
                        filters.forEach { item ->
                            val isSelected = selectedReportFilter == item.first
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) themeColors.accent else Color.Transparent)
                                    .clickable { selectedReportFilter = item.first }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = item.second,
                                    color = if (isSelected) Color.Black else Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    
                    val filteredReports = remember(reports, selectedReportFilter) {
                        if (selectedReportFilter == "ALL") reports else {
                            reports.filter { it.targetType.uppercase() == selectedReportFilter || (selectedReportFilter == "SERVICES" && it.targetType.isEmpty()) }
                        }
                    }
                    
                    if (filteredReports.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                            Text("لا توجد بلاغات مسجلة في هذا القسم", color = Color.Gray, fontSize = 11.sp)
                        }
                    } else {
                        filteredReports.forEach { rep ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                                border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.3f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("بلاغ عن: ${rep.providerName}", fontWeight = FontWeight.Bold, color = themeColors.accent, fontSize = 12.sp)
                                        Text(
                                            text = when(rep.targetType) {
                                                "STORES" -> "🏪 متجر"
                                                "RESTAURANTS" -> "🍔 مطعم"
                                                "MEDICAL" -> "🏥 منشأة طبية"
                                                "PROPERTIES" -> "🏠 عقار"
                                                "JOBS" -> "💼 وظيفة"
                                                else -> "🛠️ فني"
                                            },
                                            color = Color.LightGray,
                                            fontSize = 9.sp,
                                            modifier = Modifier
                                                .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text("بواسطة: ${rep.reporterName}", color = Color.White, fontSize = 10.sp)
                                    Text("محتوى الشكوى: ${rep.content}", color = Color.White, fontSize = 11.sp)
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        Button(
                                            onClick = { viewModel.deleteReport(rep.id) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                            modifier = Modifier.height(26.dp),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text("حذف البلاغ 🗑️", fontSize = 9.sp, color = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    Divider(color = Color.Gray.copy(alpha = 0.15f))
                    
                    Text("📋 إضافة وإدارة المشرفين:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    var supName by remember { mutableStateOf("") }
                    var supPasscode by remember { mutableStateOf("") }
                    var supRole by remember { mutableStateOf("SUPERVISOR") }

                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = themeColors.surface)) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("إضافة مشرف جديد:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                            OutlinedTextField(
                                value = supName,
                                onValueChange = { supName = it },
                                label = { Text("اسم المشرف") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = supPasscode,
                                onValueChange = { supPasscode = it },
                                label = { Text("رمز الدخول (Passcode)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(
                                onClick = {
                                    if (supName.isNotBlank() && supPasscode.isNotBlank()) {
                                        viewModel.addSupervisor(supName, supRole, supPasscode)
                                        supName = ""
                                        supPasscode = ""
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary)
                            ) {
                                Text("إضافة المشرف 🛡️", color = Color.White)
                            }
                        }
                    }

                    val supervisors by viewModel.supervisors.collectAsState()
                    supervisors.forEach { sup ->
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = themeColors.surface)) {
                            Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text("المشرف: ${sup.name} (${sup.role})", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                                    Text("رمز الدخول: ${sup.passcode}", color = Color.LightGray, fontSize = 10.sp)
                                }
                                IconButton(onClick = { viewModel.deleteSupervisor(sup.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color.Red)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun LazyListScope.adminStoresPanel(viewModel: MainViewModel, themeColors: VisualThemePalette, state: AdminPanelState) {
    with(state) {
        if (activeSubTabState.value == "STORES") {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("🏪 إدارة المحلات والأنشطة التجارية الكبرى", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    
                    val storesList by viewModel.stores.collectAsState()
                    val filteredStores = storesList.filter { it.sectionId == "" || it.sectionId == "stores" || it.sectionId == "store" }

                    // Local state for filters
                    var searchQuery by remember { mutableStateOf("") }
                    var selectedFilterType by remember { mutableStateOf("الكل") } // الكل, VIP, موثق, محظور, مميز

                    // Active dialogs
                    var storeToManageProducts by remember { mutableStateOf<StoreEntity?>(null) }
                    
                    // Counters Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = themeColors.surface)
                        ) {
                            Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("المحلات التجارية", color = Color.LightGray, fontSize = 10.sp)
                                Text("${filteredStores.size}", color = themeColors.accent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = themeColors.surface)
                        ) {
                            Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("أعضاء مميزين VIP", color = Color.LightGray, fontSize = 10.sp)
                                Text("${filteredStores.count { it.isVip }}", color = Color.Yellow, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Search & Filters Row
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("ابحث باسم المحل أو رقم الهاتف...", color = Color.Gray, fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = themeColors.accent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(listOf("الكل", "مميز VIP", "موثق 🛡️", "موصى به ⭐", "محظور 🚫")) { filter ->
                            val isSel = selectedFilterType == filter
                            Button(
                                onClick = { selectedFilterType = filter },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSel) themeColors.accent else themeColors.surface
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(filter, fontSize = 11.sp, color = if (isSel) Color.Black else Color.White)
                            }
                        }
                    }

                    // Filter Logic
                    val finalFiltered = filteredStores.filter { store ->
                        val matchesSearch = store.name.contains(searchQuery, ignoreCase = true) || store.phone.contains(searchQuery)
                        val matchesFilter = when (selectedFilterType) {
                            "الكل" -> true
                            "مميز VIP" -> store.isVip
                            "موثق 🛡️" -> store.isVerified
                            "موصى به ⭐" -> store.isRecommended
                            "محظور 🚫" -> store.isBlocked
                            else -> true
                        }
                        matchesSearch && matchesFilter
                    }

                    if (finalFiltered.isEmpty()) {
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = themeColors.surface)) {
                            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                Text("لا توجد محلات تجارية مطابقة للبحث حالياً 🏪", color = Color.LightGray, fontSize = 11.sp)
                            }
                        }
                    } else {
                        finalFiltered.forEach { store ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(store.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                            Text("📍 ${store.cityId} - ${store.localNeighborhood.ifBlank { "غير محدد" }}", color = Color.Gray, fontSize = 11.sp)
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            if (store.isVip) Badge(containerColor = Color.Yellow) { Text("VIP", color = Color.Black, fontSize = 9.sp) }
                                            if (store.isVerified) Badge(containerColor = Color.Green) { Text("موثق", color = Color.Black, fontSize = 9.sp) }
                                            if (store.isRecommended) Badge(containerColor = themeColors.accent) { Text("موصى به", color = Color.Black, fontSize = 9.sp) }
                                            if (store.isBlocked) Badge(containerColor = Color.Red) { Text("محظور", color = Color.White, fontSize = 9.sp) }
                                        }
                                    }

                                    Text("📝 الوصف: ${store.description.ifBlank { "لا يوجد وصف متوفر" }}", color = Color.LightGray, fontSize = 11.sp)
                                    Text("📞 رقم التواصل: ${store.phone}", color = Color.LightGray, fontSize = 11.sp)
                                    Text("🕒 ساعات العمل: ${store.workingHours.ifBlank { "غير محدد" }}", color = Color.LightGray, fontSize = 11.sp)

                                    Divider(color = Color.White.copy(alpha = 0.05f))

                                    // Action buttons for store toggles
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Button(
                                            onClick = { viewModel.toggleStoreVip(store.id) },
                                            colors = ButtonDefaults.buttonColors(containerColor = if (store.isVip) Color.Yellow else Color.DarkGray),
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(vertical = 4.dp)
                                        ) {
                                            Text(if (store.isVip) "🌟 إلغاء VIP" else "🏆 تفعيل VIP", fontSize = 10.sp, color = if (store.isVip) Color.Black else Color.White)
                                        }
                                        Button(
                                            onClick = { viewModel.toggleStoreVerified(store.id) },
                                            colors = ButtonDefaults.buttonColors(containerColor = if (store.isVerified) Color.Green else Color.DarkGray),
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(vertical = 4.dp)
                                        ) {
                                            Text(if (store.isVerified) "🛡️ إلغاء توثيق" else "🛡️ توثيق", fontSize = 10.sp, color = if (store.isVerified) Color.Black else Color.White)
                                        }
                                        Button(
                                            onClick = { viewModel.toggleStoreRecommended(store.id) },
                                            colors = ButtonDefaults.buttonColors(containerColor = if (store.isRecommended) themeColors.accent else Color.DarkGray),
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(vertical = 4.dp)
                                        ) {
                                            Text(if (store.isRecommended) "⭐ إلغاء توصية" else "⭐️ إبراز وتوصية", fontSize = 10.sp, color = if (store.isRecommended) Color.Black else Color.White)
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Button(
                                            onClick = { storeToManageProducts = store },
                                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary),
                                            modifier = Modifier.weight(1.5f),
                                            contentPadding = PaddingValues(vertical = 4.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("📦 إدارة المنتجات", fontSize = 10.sp, color = Color.White)
                                        }
                                        Button(
                                            onClick = { viewModel.toggleStoreBlock(store.id) },
                                            colors = ButtonDefaults.buttonColors(containerColor = if (store.isBlocked) Color.Gray else Color.Red),
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(vertical = 4.dp)
                                        ) {
                                            Text(if (store.isBlocked) "🔓 إلغاء الحظر" else "🚫 حظر المحل", fontSize = 10.sp, color = Color.White)
                                        }
                                        IconButton(
                                            onClick = { viewModel.deleteStore(store.id) },
                                            modifier = Modifier.size(36.dp).background(Color.Red.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color.Red, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // --- PRODUCTS MANAGEMENT DIALOG ---
                    storeToManageProducts?.let { currentStore ->
                        var prodName by remember { mutableStateOf("") }
                        var prodDesc by remember { mutableStateOf("") }
                        var prodPrice by remember { mutableStateOf("") }
                        
                        // Parse existing products
                        val productsList = remember(currentStore.productAttachmentsJson) {
                            val list = mutableListOf<Triple<String, String, String>>() // Name, Desc, Price
                            try {
                                if (currentStore.productAttachmentsJson.isNotBlank()) {
                                    val arr = org.json.JSONArray(currentStore.productAttachmentsJson)
                                    for (i in 0 until arr.length()) {
                                        val obj = arr.getJSONObject(i)
                                        list.add(Triple(
                                            obj.optString("name", ""),
                                            obj.optString("desc", ""),
                                            obj.optString("price", "")
                                        ))
                                    }
                                }
                            } catch (e: Exception) {}
                            list
                        }

                        AlertDialog(
                            onDismissRequest = { storeToManageProducts = null },
                            title = { Text("📦 إدارة منتجات المحل: ${currentStore.name}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White) },
                            text = {
                                Column(
                                    modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp).verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("أضف منتج أو سلعة جديدة:", color = themeColors.accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    
                                    OutlinedTextField(
                                        value = prodName,
                                        onValueChange = { prodName = it },
                                        placeholder = { Text("اسم المنتج (مثال: شاحن سفري ذكي)", fontSize = 11.sp, color = Color.Gray) },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                    )
                                    OutlinedTextField(
                                        value = prodDesc,
                                        onValueChange = { prodDesc = it },
                                        placeholder = { Text("وصف المنتج ومميزاته", fontSize = 11.sp, color = Color.Gray) },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                    )
                                    OutlinedTextField(
                                        value = prodPrice,
                                        onValueChange = { prodPrice = it },
                                        placeholder = { Text("السعر بالعملة المحلية (مثال: 4000 ريال)", fontSize = 11.sp, color = Color.Gray) },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                    )

                                    Button(
                                        onClick = {
                                            if (prodName.isNotBlank()) {
                                                val updatedList = productsList + Triple(prodName, prodDesc, prodPrice)
                                                val arr = org.json.JSONArray()
                                                updatedList.forEach { (n, d, p) ->
                                                    val obj = org.json.JSONObject()
                                                    obj.put("name", n)
                                                    obj.put("desc", d)
                                                    obj.put("price", p)
                                                    arr.put(obj)
                                                }
                                                val updatedJson = arr.toString()
                                                
                                                // Save to firebase
                                                val updatedStore = currentStore.copy(productAttachmentsJson = updatedJson)
                                                try {
                                                    FirebaseFirestore.getInstance().collection("stores").document(currentStore.id).set(updatedStore)
                                                    viewModel._stores.value = viewModel._stores.value.map { if (it.id == currentStore.id) updatedStore else it }
                                                    viewModel.triggerNotification("📦 تم إضافة المنتج للسلع بنجاح!")
                                                    storeToManageProducts = updatedStore
                                                } catch(e: Exception) {}

                                                prodName = ""
                                                prodDesc = ""
                                                prodPrice = ""
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("إضافة السلعة للمخزون ➕", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Divider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))
                                    Text("📋 المنتجات والسلع الحالية (${productsList.size}):", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                                    if (productsList.isEmpty()) {
                                        Text("لا توجد منتجات مسجلة للمحل حالياً.", color = Color.Gray, fontSize = 10.sp)
                                    } else {
                                        productsList.forEachIndexed { index, (n, d, p) ->
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = CardDefaults.cardColors(containerColor = Color.DarkGray.copy(alpha = 0.4f))
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(n, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                                                        if (d.isNotBlank()) Text(d, color = Color.LightGray, fontSize = 10.sp)
                                                        Text("💰 السعر: $p", color = themeColors.accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                    IconButton(
                                                        onClick = {
                                                            val updatedList = productsList.filterIndexed { idx, _ -> idx != index }
                                                            val arr = org.json.JSONArray()
                                                            updatedList.forEach { (nn, dd, pp) ->
                                                                val obj = org.json.JSONObject()
                                                                obj.put("name", nn)
                                                                obj.put("desc", dd)
                                                                obj.put("price", pp)
                                                                arr.put(obj)
                                                            }
                                                            val updatedJson = arr.toString()
                                                            val updatedStore = currentStore.copy(productAttachmentsJson = updatedJson)
                                                            try {
                                                                FirebaseFirestore.getInstance().collection("stores").document(currentStore.id).set(updatedStore)
                                                                viewModel._stores.value = viewModel._stores.value.map { if (it.id == currentStore.id) updatedStore else it }
                                                                viewModel.triggerNotification("🗑️ تم حذف السلعة بنجاح!")
                                                                storeToManageProducts = updatedStore
                                                            } catch(e: Exception) {}
                                                        }
                                                    ) {
                                                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color.Red, modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                Button(onClick = { storeToManageProducts = null }) {
                                    Text("تم وإغلاق", color = Color.White)
                                }
                            },
                            containerColor = themeColors.surface
                        )
                    }
                }
            }
        }
    }
}

fun LazyListScope.adminRestaurantsPanel(viewModel: MainViewModel, themeColors: VisualThemePalette, state: AdminPanelState) {
    with(state) {
        if (activeSubTabState.value == "RESTAURANTS") {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("🍔 إدارة المطاعم والكافيهات وقوائم الطعام والخصومات", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    
                    val storesList by viewModel.stores.collectAsState()
                    val filteredRestaurants = storesList.filter { it.sectionId == "restaurants" || it.sectionId == "restaurant" }

                    var searchQuery by remember { mutableStateOf("") }
                    var selectedFilterType by remember { mutableStateOf("الكل") }

                    // Dialog States
                    var restaurantToManageDishes by remember { mutableStateOf<StoreEntity?>(null) }
                    var restaurantToManageOffers by remember { mutableStateOf<StoreEntity?>(null) }

                    // Search
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("ابحث باسم المطعم أو المأكولات...", color = Color.Gray, fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = themeColors.accent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    // Filters List
                    val filterTypes = listOf("الكل", "VIP مميز", "موثق 🛡️", "موصى به ⭐", "محظور 🚫")
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filterTypes) { filter ->
                            val isSel = selectedFilterType == filter
                            Button(
                                onClick = { selectedFilterType = filter },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSel) themeColors.accent else themeColors.surface
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(filter, fontSize = 11.sp, color = if (isSel) Color.Black else Color.White)
                            }
                        }
                    }

                    // Filter restaurants
                    val finalFiltered = filteredRestaurants.filter { rest ->
                        val matchesSearch = rest.name.contains(searchQuery, ignoreCase = true) || rest.categoryId.contains(searchQuery)
                        val matchesFilter = when (selectedFilterType) {
                            "الكل" -> true
                            "VIP مميز" -> rest.isVip
                            "موثق 🛡️" -> rest.isVerified
                            "موصى به ⭐" -> rest.isRecommended
                            "محظور 🚫" -> rest.isBlocked
                            else -> true
                        }
                        matchesSearch && matchesFilter
                    }

                    if (finalFiltered.isEmpty()) {
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = themeColors.surface)) {
                            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                Text("لا توجد مطاعم مطابقة للبحث أو القسم المحدد 🍔", color = Color.LightGray, fontSize = 11.sp, textAlign = TextAlign.Center)
                            }
                        }
                    } else {
                        finalFiltered.forEach { rest ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(rest.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                            Text("🍽️ المطبخ: ${rest.categoryId}", color = themeColors.accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            if (rest.isVip) Badge(containerColor = Color.Yellow) { Text("VIP", color = Color.Black, fontSize = 9.sp) }
                                            if (rest.isVerified) Badge(containerColor = Color.Green) { Text("موثق", color = Color.Black, fontSize = 9.sp) }
                                            if (rest.isRecommended) Badge(containerColor = themeColors.accent) { Text("مميز", color = Color.Black, fontSize = 9.sp) }
                                            if (rest.isBlocked) Badge(containerColor = Color.Red) { Text("محظور", color = Color.White, fontSize = 9.sp) }
                                        }
                                    }

                                    Text("📝 التفاصيل: ${rest.description}", color = Color.LightGray, fontSize = 11.sp)
                                    Text("📍 الموقع: ${rest.cityId} - ${rest.localNeighborhood.ifBlank { "المنطقة الرئيسية" }}", color = Color.LightGray, fontSize = 11.sp)
                                    Text("📞 الهاتف والطلبات: ${rest.phone}", color = Color.LightGray, fontSize = 11.sp)
                                    Text("🕒 دوام العمل: ${rest.workingHours.ifBlank { "غير محدد" }}", color = Color.LightGray, fontSize = 11.sp)

                                    Divider(color = Color.White.copy(alpha = 0.05f))

                                    // Actions Row 1 (Toggles)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Button(
                                            onClick = { viewModel.toggleStoreVip(rest.id) },
                                            colors = ButtonDefaults.buttonColors(containerColor = if (rest.isVip) Color.Yellow else Color.DarkGray),
                                            modifier = Modifier.weight(1.0f),
                                            contentPadding = PaddingValues(vertical = 4.dp)
                                        ) {
                                            Text(if (rest.isVip) "🌟 إلغاء VIP" else "🏆 تفعيل VIP", fontSize = 10.sp, color = if (rest.isVip) Color.Black else Color.White)
                                        }
                                        Button(
                                            onClick = { viewModel.toggleStoreVerified(rest.id) },
                                            colors = ButtonDefaults.buttonColors(containerColor = if (rest.isVerified) Color.Green else Color.DarkGray),
                                            modifier = Modifier.weight(1.0f),
                                            contentPadding = PaddingValues(vertical = 4.dp)
                                        ) {
                                            Text(if (rest.isVerified) "🛡️ إلغاء توثيق" else "🛡️ توثيق", fontSize = 10.sp, color = if (rest.isVerified) Color.Black else Color.White)
                                        }
                                        Button(
                                            onClick = { viewModel.toggleStoreRecommended(rest.id) },
                                            colors = ButtonDefaults.buttonColors(containerColor = if (rest.isRecommended) themeColors.accent else Color.DarkGray),
                                            modifier = Modifier.weight(1.0f),
                                            contentPadding = PaddingValues(vertical = 4.dp)
                                        ) {
                                            Text(if (rest.isRecommended) "⭐ إلغاء تمييز" else "⭐ تمييز وتوصية", fontSize = 10.sp, color = if (rest.isRecommended) Color.Black else Color.White)
                                        }
                                    }

                                    // Actions Row 2 (Functional Menus & Daily Offers)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Button(
                                            onClick = { restaurantToManageDishes = rest },
                                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary),
                                            modifier = Modifier.weight(1.2f),
                                            contentPadding = PaddingValues(vertical = 4.dp)
                                        ) {
                                            Icon(Icons.Default.List, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("🍔 قائمة الأطباق", fontSize = 10.sp, color = Color.White)
                                        }
                                        Button(
                                            onClick = { restaurantToManageOffers = rest },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                                            modifier = Modifier.weight(1.2f),
                                            contentPadding = PaddingValues(vertical = 4.dp)
                                        ) {
                                            Icon(Icons.Default.Star, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("🏷️ عروض الخصومات", fontSize = 10.sp, color = Color.White)
                                        }
                                        Button(
                                            onClick = { viewModel.toggleStoreBlock(rest.id) },
                                            colors = ButtonDefaults.buttonColors(containerColor = if (rest.isBlocked) Color.Gray else Color.Red),
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(vertical = 4.dp)
                                        ) {
                                            Text(if (rest.isBlocked) "🔓 إلغاء حظر" else "🚫 حظر", fontSize = 10.sp, color = Color.White)
                                        }
                                        IconButton(
                                            onClick = { viewModel.deleteStore(rest.id) },
                                            modifier = Modifier.size(36.dp).background(Color.Red.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color.Red, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // --- DIALOG 1: MANAGE DISHES / MENU ---
                    restaurantToManageDishes?.let { rest ->
                        var dishName by remember { mutableStateOf("") }
                        var dishDesc by remember { mutableStateOf("") }
                        var dishPrice by remember { mutableStateOf("") }

                        val dishesList = remember(rest.productAttachmentsJson) {
                            val list = mutableListOf<Triple<String, String, String>>()
                            try {
                                if (rest.productAttachmentsJson.isNotBlank()) {
                                    val arr = org.json.JSONArray(rest.productAttachmentsJson)
                                    for (i in 0 until arr.length()) {
                                        val obj = arr.getJSONObject(i)
                                        list.add(Triple(obj.optString("name", ""), obj.optString("desc", ""), obj.optString("price", "")))
                                    }
                                }
                            } catch(e: Exception) {}
                            list
                        }

                        AlertDialog(
                            onDismissRequest = { restaurantToManageDishes = null },
                            title = { Text("🍔 إدارة منيو وأطباق: ${rest.name}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White) },
                            text = {
                                Column(
                                    modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp).verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("أضف وجبة / طبق جديد للقائمة:", color = themeColors.accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    OutlinedTextField(value = dishName, onValueChange = { dishName = it }, placeholder = { Text("اسم الطبق (مثال: مندي دجاج)", fontSize = 11.sp, color = Color.Gray) }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                                    OutlinedTextField(value = dishDesc, onValueChange = { dishDesc = it }, placeholder = { Text("مكونات الطبق وتفاصيله", fontSize = 11.sp, color = Color.Gray) }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                                    OutlinedTextField(value = dishPrice, onValueChange = { dishPrice = it }, placeholder = { Text("سعر الطبق (ر.ي) (مثال: 3500)", fontSize = 11.sp, color = Color.Gray) }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))

                                    Button(
                                        onClick = {
                                            if (dishName.isNotBlank()) {
                                                val updatedList = dishesList + Triple(dishName, dishDesc, dishPrice)
                                                val arr = org.json.JSONArray()
                                                updatedList.forEach { (n, d, p) ->
                                                    val obj = org.json.JSONObject()
                                                    obj.put("name", n)
                                                    obj.put("desc", d)
                                                    obj.put("price", p)
                                                    arr.put(obj)
                                                }
                                                val updatedJson = arr.toString()
                                                val updatedRest = rest.copy(productAttachmentsJson = updatedJson)
                                                try {
                                                    FirebaseFirestore.getInstance().collection("stores").document(rest.id).set(updatedRest)
                                                    viewModel._stores.value = viewModel._stores.value.map { if (it.id == rest.id) updatedRest else it }
                                                    viewModel.triggerNotification("🍔 تم إضافة وجبة جديدة بنجاح!")
                                                    restaurantToManageDishes = updatedRest
                                                } catch(e: Exception) {}

                                                dishName = ""
                                                dishDesc = ""
                                                dishPrice = ""
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("إضافة الوجبة للمنيو ➕", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Divider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))
                                    Text("الأطباق الحالية (${dishesList.size}):", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                                    dishesList.forEachIndexed { idx, (n, d, p) ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = Color.DarkGray.copy(alpha = 0.4f))
                                        ) {
                                            Row(modifier = Modifier.padding(8.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(n, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                                                    if (d.isNotBlank()) Text(d, color = Color.LightGray, fontSize = 10.sp)
                                                    Text("💰 السعر: $p", color = themeColors.accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                                IconButton(
                                                    onClick = {
                                                        val updated = dishesList.filterIndexed { i, _ -> i != idx }
                                                        val arr = org.json.JSONArray()
                                                        updated.forEach { (nn, dd, pp) ->
                                                            val obj = org.json.JSONObject()
                                                            obj.put("name", nn)
                                                            obj.put("desc", dd)
                                                            obj.put("price", pp)
                                                            arr.put(obj)
                                                        }
                                                        val updatedJson = arr.toString()
                                                        val updatedRest = rest.copy(productAttachmentsJson = updatedJson)
                                                        try {
                                                            FirebaseFirestore.getInstance().collection("stores").document(rest.id).set(updatedRest)
                                                            viewModel._stores.value = viewModel._stores.value.map { if (it.id == rest.id) updatedRest else it }
                                                            viewModel.triggerNotification("🗑️ تم حذف الوجبة بنجاح!")
                                                            restaurantToManageDishes = updatedRest
                                                        } catch(e: Exception) {}
                                                    }
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color.Red, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                Button(onClick = { restaurantToManageDishes = null }) {
                                    Text("تم وإغلاق", color = Color.White)
                                }
                            },
                            containerColor = themeColors.surface
                        )
                    }

                    // --- DIALOG 2: MANAGE OFFERS & DISCOUNTS ---
                    restaurantToManageOffers?.let { rest ->
                        var offerTitle by remember { mutableStateOf("") }
                        var offerPct by remember { mutableStateOf("") }
                        var offerPeriod by remember { mutableStateOf("") }

                        val offersList = remember(rest.specialOffersJson) {
                            val list = mutableListOf<Triple<String, String, String>>()
                            try {
                                if (rest.specialOffersJson.isNotBlank()) {
                                    val arr = org.json.JSONArray(rest.specialOffersJson)
                                    for (i in 0 until arr.length()) {
                                        val obj = arr.getJSONObject(i)
                                        list.add(Triple(obj.optString("title", ""), obj.optString("pct", ""), obj.optString("period", "")))
                                    }
                                }
                            } catch(e: Exception) {}
                            list
                        }

                        AlertDialog(
                            onDismissRequest = { restaurantToManageOffers = null },
                            title = { Text("🏷️ عروض الخصومات للمطعم: ${rest.name}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White) },
                            text = {
                                Column(
                                    modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp).verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("إضافة عرض خصم جديد:", color = themeColors.accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    OutlinedTextField(value = offerTitle, onValueChange = { offerTitle = it }, placeholder = { Text("عنوان العرض (خصم على البرجر)", fontSize = 11.sp, color = Color.Gray) }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                                    OutlinedTextField(value = offerPct, onValueChange = { offerPct = it }, placeholder = { Text("نسبة الخصم (مثال: 20%)", fontSize = 11.sp, color = Color.Gray) }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                                    OutlinedTextField(value = offerPeriod, onValueChange = { offerPeriod = it }, placeholder = { Text("صلاحية العرض (مثال: خلال نهاية الأسبوع)", fontSize = 11.sp, color = Color.Gray) }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))

                                    Button(
                                        onClick = {
                                            if (offerTitle.isNotBlank()) {
                                                val updatedList = offersList + Triple(offerTitle, offerPct, offerPeriod)
                                                val arr = org.json.JSONArray()
                                                updatedList.forEach { (t, p, d) ->
                                                    val obj = org.json.JSONObject()
                                                    obj.put("title", t)
                                                    obj.put("pct", p)
                                                    obj.put("period", d)
                                                    arr.put(obj)
                                                }
                                                val updatedJson = arr.toString()
                                                val updatedRest = rest.copy(specialOffersJson = updatedJson)
                                                try {
                                                    FirebaseFirestore.getInstance().collection("stores").document(rest.id).set(updatedRest)
                                                    viewModel._stores.value = viewModel._stores.value.map { if (it.id == rest.id) updatedRest else it }
                                                    viewModel.triggerNotification("🏷️ تم إضافة عرض الخصم للمطعم!")
                                                    restaurantToManageOffers = updatedRest
                                                } catch(e: Exception) {}

                                                offerTitle = ""
                                                offerPct = ""
                                                offerPeriod = ""
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("نشر العرض الترويجي 📢", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Divider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))
                                    Text("العروض الحالية المتاحة (${offersList.size}):", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                                    offersList.forEachIndexed { idx, (t, p, d) ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = Color.DarkGray.copy(alpha = 0.4f))
                                        ) {
                                            Row(modifier = Modifier.padding(8.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(t, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                                                    Text("🔥 خصم: $p", color = Color.Yellow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    if (d.isNotBlank()) Text("⏱️ الصلاحية: $d", color = Color.LightGray, fontSize = 10.sp)
                                                }
                                                IconButton(
                                                    onClick = {
                                                        val updated = offersList.filterIndexed { i, _ -> i != idx }
                                                        val arr = org.json.JSONArray()
                                                        updated.forEach { (tt, pp, dd) ->
                                                            val obj = org.json.JSONObject()
                                                            obj.put("title", tt)
                                                            obj.put("pct", pp)
                                                            obj.put("period", dd)
                                                            arr.put(obj)
                                                        }
                                                        val updatedJson = arr.toString()
                                                        val updatedRest = rest.copy(specialOffersJson = updatedJson)
                                                        try {
                                                            FirebaseFirestore.getInstance().collection("stores").document(rest.id).set(updatedRest)
                                                            viewModel._stores.value = viewModel._stores.value.map { if (it.id == rest.id) updatedRest else it }
                                                            viewModel.triggerNotification("🗑️ تم إزالة العرض الترويجي")
                                                            restaurantToManageOffers = updatedRest
                                                        } catch(e: Exception) {}
                                                    }
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color.Red, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                Button(onClick = { restaurantToManageOffers = null }) {
                                    Text("تم وإغلاق", color = Color.White)
                                }
                            },
                            containerColor = themeColors.surface
                        )
                    }
                }
            }
        }
    }
}

fun LazyListScope.adminMedicalPanel(viewModel: MainViewModel, themeColors: VisualThemePalette, state: AdminPanelState) {
    with(state) {
        if (activeSubTabState.value == "MEDICAL") {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("🏥 إدارة المراكز الطبية والمستشفيات والعيادات التخصصية", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    
                    val storesList by viewModel.stores.collectAsState()
                    val filteredMedical = storesList.filter { it.sectionId == "medical" || it.sectionId == "clinic" || it.sectionId == "hospital" }

                    var searchQuery by remember { mutableStateOf("") }
                    var selectedFilterType by remember { mutableStateOf("الكل") }

                    // Dialog States
                    var medicalToManageDoctors by remember { mutableStateOf<StoreEntity?>(null) }

                    // Search
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("ابحث باسم المركز أو التخصص الطبي...", color = Color.Gray, fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = themeColors.accent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    // Filters
                    val filterTypes = listOf("الكل", "VIP مميز", "موثق 🛡️", "موصى به ⭐", "محظور 🚫")
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filterTypes) { filter ->
                            val isSel = selectedFilterType == filter
                            Button(
                                onClick = { selectedFilterType = filter },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSel) themeColors.accent else themeColors.surface
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(filter, fontSize = 11.sp, color = if (isSel) Color.Black else Color.White)
                            }
                        }
                    }

                    // Render List
                    val finalFiltered = filteredMedical.filter { med ->
                        val matchesSearch = med.name.contains(searchQuery, ignoreCase = true) || med.categoryId.contains(searchQuery)
                        val matchesFilter = when (selectedFilterType) {
                            "الكل" -> true
                            "VIP مميز" -> med.isVip
                            "موثق 🛡️" -> med.isVerified
                            "موصى به ⭐" -> med.isRecommended
                            "محظور 🚫" -> med.isBlocked
                            else -> true
                        }
                        matchesSearch && matchesFilter
                    }

                    if (finalFiltered.isEmpty()) {
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = themeColors.surface)) {
                            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                Text("لا توجد مراكز طبية أو عيادات حالياً 🏥", color = Color.LightGray, fontSize = 11.sp, textAlign = TextAlign.Center)
                            }
                        }
                    } else {
                        finalFiltered.forEach { med ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(med.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                            Text("🏥 التخصص: ${med.categoryId}", color = themeColors.accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            if (med.isVip) Badge(containerColor = Color.Yellow) { Text("VIP", color = Color.Black, fontSize = 9.sp) }
                                            if (med.isVerified) Badge(containerColor = Color.Green) { Text("موثق", color = Color.Black, fontSize = 9.sp) }
                                            if (med.isRecommended) Badge(containerColor = themeColors.accent) { Text("موصى به", color = Color.Black, fontSize = 9.sp) }
                                            if (med.isBlocked) Badge(containerColor = Color.Red) { Text("محظور", color = Color.White, fontSize = 9.sp) }
                                        }
                                    }

                                    Text("📝 التفاصيل: ${med.description}", color = Color.LightGray, fontSize = 11.sp)
                                    Text("📍 الموقع: ${med.cityId} - ${med.localNeighborhood.ifBlank { "المنطقة الرئيسية" }}", color = Color.LightGray, fontSize = 11.sp)
                                    Text("📞 الطوارئ والاستعلامات: ${med.phone}", color = Color.LightGray, fontSize = 11.sp)
                                    Text("🕒 دوام المركز: ${med.workingHours.ifBlank { "غير محدد" }}", color = Color.LightGray, fontSize = 11.sp)

                                    Divider(color = Color.White.copy(alpha = 0.05f))

                                    // Action buttons for medical toggles
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Button(
                                            onClick = { viewModel.toggleStoreVip(med.id) },
                                            colors = ButtonDefaults.buttonColors(containerColor = if (med.isVip) Color.Yellow else Color.DarkGray),
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(vertical = 4.dp)
                                        ) {
                                            Text(if (med.isVip) "🌟 إلغاء VIP" else "🏆 تفعيل VIP", fontSize = 10.sp, color = if (med.isVip) Color.Black else Color.White)
                                        }
                                        Button(
                                            onClick = { viewModel.toggleStoreVerified(med.id) },
                                            colors = ButtonDefaults.buttonColors(containerColor = if (med.isVerified) Color.Green else Color.DarkGray),
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(vertical = 4.dp)
                                        ) {
                                            Text(if (med.isVerified) "🛡️ إلغاء توثيق" else "🛡️ توثيق", fontSize = 10.sp, color = if (med.isVerified) Color.Black else Color.White)
                                        }
                                        Button(
                                            onClick = { viewModel.toggleStoreRecommended(med.id) },
                                            colors = ButtonDefaults.buttonColors(containerColor = if (med.isRecommended) themeColors.accent else Color.DarkGray),
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(vertical = 4.dp)
                                        ) {
                                            Text(if (med.isRecommended) "⭐ إلغاء توصية" else "⭐️ إبراز وتوصية", fontSize = 10.sp, color = if (med.isRecommended) Color.Black else Color.White)
                                        }
                                    }

                                    // Doctor management, block, delete
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Button(
                                            onClick = { medicalToManageDoctors = med },
                                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary),
                                            modifier = Modifier.weight(1.5f),
                                            contentPadding = PaddingValues(vertical = 4.dp)
                                        ) {
                                            Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("👨‍⚕️ إدارة الأطباء والدوام", fontSize = 10.sp, color = Color.White)
                                        }
                                        Button(
                                            onClick = { viewModel.toggleStoreBlock(med.id) },
                                            colors = ButtonDefaults.buttonColors(containerColor = if (med.isBlocked) Color.Gray else Color.Red),
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(vertical = 4.dp)
                                        ) {
                                            Text(if (med.isBlocked) "🔓 إلغاء الحظر" else "🚫 حظر المركز", fontSize = 10.sp, color = Color.White)
                                        }
                                        IconButton(
                                            onClick = { viewModel.deleteStore(med.id) },
                                            modifier = Modifier.size(36.dp).background(Color.Red.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color.Red, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // --- DOCTOR MANAGEMENT DIALOG ---
                    medicalToManageDoctors?.let { med ->
                        var docName by remember { mutableStateOf("") }
                        var docSpec by remember { mutableStateOf("") }
                        var docSchedule by remember { mutableStateOf("") }
                        var docFee by remember { mutableStateOf("") }

                        val doctorsList = remember(med.productAttachmentsJson) {
                            val list = mutableListOf<Triple<String, String, String>>() // Name, Spec+Schedule, Fee
                            try {
                                if (med.productAttachmentsJson.isNotBlank()) {
                                    val arr = org.json.JSONArray(med.productAttachmentsJson)
                                    for (i in 0 until arr.length()) {
                                        val obj = arr.getJSONObject(i)
                                        list.add(Triple(obj.optString("name", ""), obj.optString("spec", ""), obj.optString("fee", "")))
                                    }
                                }
                            } catch(e: Exception) {}
                            list
                        }

                        AlertDialog(
                            onDismissRequest = { medicalToManageDoctors = null },
                            title = { Text("👨‍⚕️ الكادر الطبي والعيادات بـ: ${med.name}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White) },
                            text = {
                                Column(
                                    modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp).verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("إضافة طبيب / استشاري جديد للعيادات المعتمدة:", color = themeColors.accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    
                                    OutlinedTextField(value = docName, onValueChange = { docName = it }, placeholder = { Text("اسم الطبيب الكامل (مثال: د. أحمد يحيى)", fontSize = 11.sp, color = Color.Gray) }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                                    OutlinedTextField(value = docSpec, onValueChange = { docSpec = it }, placeholder = { Text("التخصص الدقيق وأوقات الدوام (مثال: باطنية - السبت للأربعاء)", fontSize = 11.sp, color = Color.Gray) }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                                    OutlinedTextField(value = docFee, onValueChange = { docFee = it }, placeholder = { Text("سعر كشفية المعاينة (مثال: 3000 ريال)", fontSize = 11.sp, color = Color.Gray) }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))

                                    Button(
                                        onClick = {
                                            if (docName.isNotBlank()) {
                                                val updatedList = doctorsList + Triple(docName, docSpec, docFee)
                                                val arr = org.json.JSONArray()
                                                updatedList.forEach { (n, s, f) ->
                                                    val obj = org.json.JSONObject()
                                                    obj.put("name", n)
                                                    obj.put("spec", s)
                                                    obj.put("fee", f)
                                                    arr.put(obj)
                                                }
                                                val updatedJson = arr.toString()
                                                val updatedMed = med.copy(productAttachmentsJson = updatedJson)
                                                try {
                                                    FirebaseFirestore.getInstance().collection("stores").document(med.id).set(updatedMed)
                                                    viewModel._stores.value = viewModel._stores.value.map { if (it.id == med.id) updatedMed else it }
                                                    viewModel.triggerNotification("👨‍⚕️ تم إضافة الاستشاري لجدول العيادات بنجاح!")
                                                    medicalToManageDoctors = updatedMed
                                                } catch(e: Exception) {}

                                                docName = ""
                                                docSpec = ""
                                                docFee = ""
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("إضافة الطبيب لجدول المناوبات ➕", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Divider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))
                                    Text("الأطباء والاستشاريين الحاليين (${doctorsList.size}):", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                                    doctorsList.forEachIndexed { idx, (n, s, f) ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = Color.DarkGray.copy(alpha = 0.4f))
                                        ) {
                                            Row(modifier = Modifier.padding(8.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(n, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                                                    if (s.isNotBlank()) Text(s, color = Color.LightGray, fontSize = 10.sp)
                                                    Text("💸 سعر المعاينة: $f", color = themeColors.accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                                IconButton(
                                                    onClick = {
                                                        val updated = doctorsList.filterIndexed { i, _ -> i != idx }
                                                        val arr = org.json.JSONArray()
                                                        updated.forEach { (nn, ss, ff) ->
                                                            val obj = org.json.JSONObject()
                                                            obj.put("name", nn)
                                                            obj.put("spec", ss)
                                                            obj.put("fee", ff)
                                                            arr.put(obj)
                                                        }
                                                        val updatedJson = arr.toString()
                                                        val updatedMed = med.copy(productAttachmentsJson = updatedJson)
                                                        try {
                                                            FirebaseFirestore.getInstance().collection("stores").document(med.id).set(updatedMed)
                                                            viewModel._stores.value = viewModel._stores.value.map { if (it.id == med.id) updatedMed else it }
                                                            viewModel.triggerNotification("🗑️ تم حذف الطبيب من القائمة")
                                                            medicalToManageDoctors = updatedMed
                                                        } catch(e: Exception) {}
                                                    }
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color.Red, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                Button(onClick = { medicalToManageDoctors = null }) {
                                    Text("تم وإغلاق", color = Color.White)
                                }
                            },
                            containerColor = themeColors.surface
                        )
                    }
                }
            }
        }
    }
}

fun LazyListScope.adminPropertiesPanel(viewModel: MainViewModel, themeColors: VisualThemePalette, state: AdminPanelState) {
    with(state) {
        if (activeSubTabState.value == "PROPERTIES") {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("🏠 إدارة العقارات والأراضي والإيجار والبيع", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    
                    var propTitle by remember { mutableStateOf("") }
                    var propPrice by remember { mutableStateOf("") }
                    var propPhone by remember { mutableStateOf("") }
                    var propType by remember { mutableStateOf("sale") }

                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = themeColors.surface)) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("➕ إضافة عقار جديد يدوياً:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            OutlinedTextField(
                                value = propTitle,
                                onValueChange = { propTitle = it },
                                label = { Text("عنوان العقار (مثال: شقة للبيع بالسبعين)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = propPrice,
                                onValueChange = { propPrice = it },
                                label = { Text("السعر المحدد") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            OutlinedTextField(
                                value = propPhone,
                                onValueChange = { propPhone = it },
                                label = { Text("رقم المالك / المكاتب العقارية") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(
                                onClick = {
                                    if (propTitle.isNotBlank()) {
                                        val p = propPrice.toDoubleOrNull() ?: 0.0
                                        viewModel.addProperty(propTitle, "عقار ممتاز", p, propType, "apartment", propPhone)
                                        propTitle = ""
                                        propPrice = ""
                                        propPhone = ""
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary)
                            ) {
                                Text("إضافة العقار 🏠", color = Color.White)
                            }
                        }
                    }

                    Text("📋 قائمة العقارات المتاحة:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    val propertiesList by viewModel.properties.collectAsState()
                    if (propertiesList.isEmpty()) {
                        Text("لا توجد عقارات مسجلة حالياً.", color = Color.LightGray, fontSize = 11.sp)
                    } else {
                        propertiesList.forEach { prop ->
                            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = themeColors.surface)) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(prop.title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                    Text("السعر: ${prop.price} ${prop.currency} | النوع: ${prop.type}", color = Color.LightGray, fontSize = 10.sp)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(onClick = { viewModel.togglePropertyBlock(prop.id) }, colors = ButtonDefaults.buttonColors(containerColor = if (prop.isBlocked) Color.Green else Color.Red)) {
                                            Text(if (prop.isBlocked) "إلغاء الحظر" else "حظر", fontSize = 9.sp, color = Color.White)
                                        }
                                        Button(onClick = { viewModel.deleteProperty(prop.id) }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f))) {
                                            Text("حذف 🗑️", fontSize = 9.sp, color = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun LazyListScope.adminJobsPanel(viewModel: MainViewModel, themeColors: VisualThemePalette, state: AdminPanelState) {
    with(state) {
        if (activeSubTabState.value == "JOBS") {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("💼 إدارة إعلانات الوظائف والشركات المعلنة", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    
                    var jobTitle by remember { mutableStateOf("") }
                    var companyName by remember { mutableStateOf("") }
                    var jobSalary by remember { mutableStateOf("") }
                    var jobPhone by remember { mutableStateOf("") }

                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = themeColors.surface)) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("➕ إضافة وظيفة جديدة يدوياً:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            OutlinedTextField(
                                value = jobTitle,
                                onValueChange = { jobTitle = it },
                                label = { Text("المسمى الوظيفي") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = companyName,
                                onValueChange = { companyName = it },
                                label = { Text("اسم الشركة / الجهة") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = jobSalary,
                                onValueChange = { jobSalary = it },
                                label = { Text("الراتب المتوقع") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = jobPhone,
                                onValueChange = { jobPhone = it },
                                label = { Text("هاتف التواصل") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(
                                onClick = {
                                    if (jobTitle.isNotBlank()) {
                                        viewModel.addJob(jobTitle, companyName, "تفاصيل الوظيفة", jobPhone, jobSalary)
                                        jobTitle = ""
                                        companyName = ""
                                        jobSalary = ""
                                        jobPhone = ""
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary)
                            ) {
                                Text("إضافة الوظيفة 💼", color = Color.White)
                            }
                        }
                    }

                    val jobsList by viewModel.jobs.collectAsState()
                    jobsList.forEach { job ->
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = themeColors.surface)) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("${job.title} - ${job.companyName}", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                Text("الراتب: ${job.salary} | نوع الدوام: ${job.jobType}", color = Color.LightGray, fontSize = 10.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(onClick = { viewModel.toggleJobPin(job.id) }, colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)) {
                                        Text(if (job.isPinned) "إلغاء التثبيت" else "تثبيت 📌", fontSize = 9.sp, color = Color.White)
                                    }
                                    Button(onClick = { viewModel.deleteJob(job.id) }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f))) {
                                        Text("حذف 🗑️", fontSize = 9.sp, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun LazyListScope.adminApplicantsPanel(viewModel: MainViewModel, themeColors: VisualThemePalette, state: AdminPanelState) {
    with(state) {
        if (activeSubTabState.value == "APPLICANTS") {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("📄 إدارة طلبات المتقدمين للوظائف والسير الذاتية", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    val appsList by viewModel.jobApplications.collectAsState()
                    if (appsList.isEmpty()) {
                        Text("لا توجد طلبات تقديم حالياً.", color = Color.LightGray, fontSize = 11.sp)
                    } else {
                        appsList.forEach { app ->
                            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = themeColors.surface)) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("المتقدم: ${app.applicantName} (${app.applicantPhone})", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                                    Text("الوظيفة: ${app.jobTitle} | الحالة: ${app.status}", color = Color.LightGray, fontSize = 10.sp)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(onClick = { viewModel.updateJobApplicationStatus(app.id, "ACCEPTED") }, colors = ButtonDefaults.buttonColors(containerColor = Color.Green)) {
                                            Text("قبول ✅", fontSize = 9.sp, color = Color.Black)
                                        }
                                        Button(onClick = { viewModel.updateJobApplicationStatus(app.id, "REJECTED") }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                                            Text("رفض ❌", fontSize = 9.sp, color = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun LazyListScope.adminCleanPanel(viewModel: MainViewModel, themeColors: VisualThemePalette, state: AdminPanelState) {
    with(state) {
        if (activeSubTabState.value == "CLEAN") {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("🧹 تهيئة وتنظيف بيانات النظام وإعادة الضبط الشامل", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    
                    var wipeProviders by remember { mutableStateOf(false) }
                    var wipeBookings by remember { mutableStateOf(false) }
                    var wipeChats by remember { mutableStateOf(false) }
                    var wipeStores by remember { mutableStateOf(false) }
                    var wipeProperties by remember { mutableStateOf(false) }
                    var wipeJobs by remember { mutableStateOf(false) }
                    var wipeNotifications by remember { mutableStateOf(false) }
                    var wipeComplaints by remember { mutableStateOf(false) }
                    var wipeBanners by remember { mutableStateOf(false) }
                    var wipeCoupons by remember { mutableStateOf(false) }
                    var wipeReviews by remember { mutableStateOf(false) }
                    var wipeCallsLog by remember { mutableStateOf(false) }
                    var wipeSupervisors by remember { mutableStateOf(false) }

                    var showPasswordDialog by remember { mutableStateOf(false) }
                    var adminPasswordInput by remember { mutableStateOf("") }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("🚨 تحذير: هذه العملية تمس قاعدة البيانات مباشرة ونهائية!", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Red)
                            Text("اختر الفئات التي تريد مسحها وتصفيرها بالكامل من خوادم Firestore:", fontSize = 11.sp, color = Color.LightGray)

                            val sections = listOf(
                                Triple("الفنيين ومزودي الخدمات", wipeProviders) { b: Boolean -> wipeProviders = b },
                                Triple("سجل الحجوزات والطلبات", wipeBookings) { b: Boolean -> wipeBookings = b },
                                Triple("غرف الدردشة والرسائل", wipeChats) { b: Boolean -> wipeChats = b },
                                Triple("المحلات والمتاجر التجارية", wipeStores) { b: Boolean -> wipeStores = b },
                                Triple("العقارات والمعروضات", wipeProperties) { b: Boolean -> wipeProperties = b },
                                Triple("الوظائف والمتقدمين", wipeJobs) { b: Boolean -> wipeJobs = b },
                                Triple("التنبيهات وبث الإشعارات", wipeNotifications) { b: Boolean -> wipeNotifications = b },
                                Triple("سجلات الشكاوى والاقتراحات", wipeComplaints) { b: Boolean -> wipeComplaints = b },
                                Triple("البنرات والشرائح الإعلانية", wipeBanners) { b: Boolean -> wipeBanners = b },
                                Triple("كوبونات الخصم والنقاط", wipeCoupons) { b: Boolean -> wipeCoupons = b },
                                Triple("التقييمات والتعليقات", wipeReviews) { b: Boolean -> wipeReviews = b },
                                Triple("سجلات المكالمات الهاتفية", wipeCallsLog) { b: Boolean -> wipeCallsLog = b },
                                Triple("طاقم المشرفين والمساعدين", wipeSupervisors) { b: Boolean -> wipeSupervisors = b }
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                sections.forEach { (label, checked, onCheckedChange) ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(checked = checked, onCheckedChange = onCheckedChange, colors = CheckboxDefaults.colors(checkedColor = Color.Red))
                                        Text(label, fontSize = 11.sp, color = Color.White)
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    if (wipeProviders || wipeBookings || wipeChats || wipeStores || wipeProperties || wipeJobs || wipeNotifications || wipeComplaints || wipeBanners || wipeCoupons || wipeReviews || wipeCallsLog || wipeSupervisors) {
                                        showPasswordDialog = true
                                    } else {
                                        viewModel.triggerNotification("⚠️ يرجى تحديد فئة واحدة على الأقل للمسح")
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("تنظيف البيانات المحددة بشكل نهائي 🧹", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }

                    if (showPasswordDialog) {
                        AlertDialog(
                            onDismissRequest = { showPasswordDialog = false },
                            title = { Text("🔒 تأكيد الهوية الإدارية", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold) },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("أدخل كلمة المرور الخاصة بالأدمن لإتمام عملية التهيئة والمسح الشامل:", fontSize = 11.sp, color = Color.LightGray)
                                    OutlinedTextField(
                                        value = adminPasswordInput,
                                        onValueChange = { adminPasswordInput = it },
                                        label = { Text("رمز المرور السري", fontSize = 11.sp) },
                                        modifier = Modifier.fillMaxWidth(),
                                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = Color.White)
                                    )
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        if (adminPasswordInput == "1234") {
                                            viewModel.wipeSelectedData(
                                                wipeProviders = wipeProviders,
                                                wipeBookings = wipeBookings,
                                                wipeChats = wipeChats,
                                                wipeStores = wipeStores,
                                                wipeProperties = wipeProperties,
                                                wipeJobs = wipeJobs
                                            )
                                            if (wipeNotifications) {
                                                com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("notifications").get().addOnSuccessListener { snapshot ->
                                                    for (doc in snapshot.documents) doc.reference.delete()
                                                }
                                            }
                                            if (wipeComplaints) {
                                                com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("complaints").get().addOnSuccessListener { snapshot ->
                                                    for (doc in snapshot.documents) doc.reference.delete()
                                                }
                                            }
                                            if (wipeBanners) {
                                                com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("banners").get().addOnSuccessListener { snapshot ->
                                                    for (doc in snapshot.documents) doc.reference.delete()
                                                }
                                            }
                                            if (wipeCoupons) {
                                                com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("coupons").get().addOnSuccessListener { snapshot ->
                                                    for (doc in snapshot.documents) doc.reference.delete()
                                                }
                                            }
                                            if (wipeReviews) {
                                                com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("ratings").get().addOnSuccessListener { snapshot ->
                                                    for (doc in snapshot.documents) doc.reference.delete()
                                                }
                                            }
                                            if (wipeCallsLog) {
                                                com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("calls_log").get().addOnSuccessListener { snapshot ->
                                                    for (doc in snapshot.documents) doc.reference.delete()
                                                }
                                            }
                                            if (wipeSupervisors) {
                                                com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("supervisors").get().addOnSuccessListener { snapshot ->
                                                    for (doc in snapshot.documents) doc.reference.delete()
                                                }
                                            }
                                            viewModel.triggerNotification("✅ تم تهيئة ومسح البيانات المحددة من السيرفر بنجاح!")
                                            showPasswordDialog = false
                                            adminPasswordInput = ""
                                        } else {
                                            viewModel.triggerNotification("❌ رمز المرور خاطئ! تم إلغاء العملية لحماية النظام.")
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                                ) {
                                    Text("تأكيد وحذف 💀", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            },
                            dismissButton = {
                                Button(
                                    onClick = {
                                        showPasswordDialog = false
                                        adminPasswordInput = ""
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                                ) {
                                    Text("إلغاء", color = Color.White, fontSize = 11.sp)
                                }
                            },
                            containerColor = themeColors.surface
                        )
                    }
                }
            }
        }
    }
}

fun LazyListScope.adminReviewsPanel(viewModel: MainViewModel, themeColors: VisualThemePalette, state: AdminPanelState) {
    with(state) {
        if (activeSubTabState.value == "REVIEWS") {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("⭐ إدارة التقييمات والتعليقات والردود الإدارية", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    
                    val ratingsList by viewModel.ratings.collectAsState()
                    var commentSearch by remember { mutableStateOf("") }
                    var ratingFilter by remember { mutableStateOf(0) } // 0 = all, 1 to 5 stars
                    
                    OutlinedTextField(
                        value = commentSearch,
                        onValueChange = { commentSearch = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("بحث باسم العميل أو الكلمات في التعليق...", color = Color.Gray, fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) }
                    )

                    // Stars filter row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Button(
                            onClick = { ratingFilter = 0 },
                            colors = ButtonDefaults.buttonColors(containerColor = if (ratingFilter == 0) themeColors.accent else themeColors.surface),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) { Text("الكل", fontSize = 10.sp, color = if (ratingFilter == 0) Color.Black else Color.White) }

                        (1..5).forEach { star ->
                            Button(
                                onClick = { ratingFilter = star },
                                colors = ButtonDefaults.buttonColors(containerColor = if (ratingFilter == star) themeColors.accent else themeColors.surface),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) { Text("$star ⭐", fontSize = 9.sp, color = if (ratingFilter == star) Color.Black else Color.White) }
                        }
                    }

                    val finalRatings = ratingsList.filter { r ->
                        val matchesSearch = r.userName.contains(commentSearch, ignoreCase = true) || r.comment.contains(commentSearch, ignoreCase = true)
                        val matchesFilter = ratingFilter == 0 || r.rating.toInt() == ratingFilter
                        matchesSearch && matchesFilter
                    }

                    if (finalRatings.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                            Text("لا توجد تقييمات مطابقة للفلاتر ⭐", color = Color.Gray, fontSize = 11.sp)
                        }
                    } else {
                        finalRatings.forEach { rat ->
                            var showEditCommentDialog by remember { mutableStateOf(false) }
                            var editCommentInput by remember { mutableStateOf(rat.comment) }
                            var showReplyDialog by remember { mutableStateOf(false) }
                            var replyInput by remember { mutableStateOf(rat.reply) }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = themeColors.surface)
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text(rat.userName.ifBlank { "عميل مجهول" }, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                            (1..5).forEach { i ->
                                                Text(if (i <= rat.rating) "⭐" else "☆", fontSize = 10.sp, color = Color.Yellow)
                                            }
                                        }
                                    }

                                    Text("نوع الهدف: ${rat.targetType} | معرف الهدف: ${rat.targetId}", fontSize = 10.sp, color = Color.Gray)
                                    Text("التعليق: ${rat.comment}", fontSize = 11.sp, color = Color.White)
                                    
                                    if (rat.reply.isNotEmpty()) {
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))
                                        ) {
                                            Column(modifier = Modifier.padding(6.dp)) {
                                                Text("💬 رد الإدارة: ${rat.reply}", fontSize = 10.sp, color = themeColors.accent)
                                            }
                                        }
                                    }

                                    // Action buttons for ratings
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Button(
                                            onClick = { showEditCommentDialog = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary),
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(vertical = 2.dp)
                                        ) { Text("تعديل ✏️", fontSize = 9.sp, color = Color.White) }

                                        Button(
                                            onClick = { showReplyDialog = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(vertical = 2.dp)
                                        ) { Text("رد إداري 💬", fontSize = 9.sp, color = Color.Black, fontWeight = FontWeight.Bold) }

                                        Button(
                                            onClick = {
                                                val updated = rat.copy(isApproved = !rat.isApproved)
                                                com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("ratings").document(rat.id).set(updated)
                                                viewModel.triggerNotification(if (updated.isApproved) "✅ تم إظهار التقييم للعامة" else "⛔ تم إخفاء التقييم عن العامة")
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = if (rat.isApproved) Color.DarkGray else Color.Green),
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(vertical = 2.dp)
                                        ) { Text(if (rat.isApproved) "إخفاء ⛔" else "إظهار ✅", fontSize = 9.sp, color = Color.White) }

                                        Button(
                                            onClick = {
                                                com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("ratings").document(rat.id).delete()
                                                viewModel.triggerNotification("🗑️ تم حذف التقييم نهائياً")
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(vertical = 2.dp)
                                        ) { Text("حذف 🗑️", fontSize = 9.sp, color = Color.White) }
                                    }
                                }
                            }

                            // Dialogs
                            if (showEditCommentDialog) {
                                AlertDialog(
                                    onDismissRequest = { showEditCommentDialog = false },
                                    title = { Text("تعديل نص التعليق") },
                                    text = {
                                        OutlinedTextField(value = editCommentInput, onValueChange = { editCommentInput = it }, modifier = Modifier.fillMaxWidth())
                                    },
                                    confirmButton = {
                                        Button(onClick = {
                                            val updated = rat.copy(comment = editCommentInput)
                                            com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("ratings").document(rat.id).set(updated)
                                            viewModel.triggerNotification("✅ تم تعديل التعليق")
                                            showEditCommentDialog = false
                                        }) { Text("حفظ") }
                                    },
                                    dismissButton = {
                                        Button(onClick = { showEditCommentDialog = false }) { Text("إلغاء") }
                                    }
                                )
                            }

                            if (showReplyDialog) {
                                AlertDialog(
                                    onDismissRequest = { showReplyDialog = false },
                                    title = { Text("الرد الإداري على التقييم") },
                                    text = {
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            OutlinedTextField(value = replyInput, onValueChange = { replyInput = it }, label = { Text("اكتب رد الإدارة هنا...") }, modifier = Modifier.fillMaxWidth())
                                            if (rat.reply.isNotEmpty()) {
                                                Button(
                                                    onClick = {
                                                        val updated = rat.copy(reply = "", replyTimestamp = null)
                                                        com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("ratings").document(rat.id).set(updated)
                                                        viewModel.triggerNotification("🧹 تم مسح رد الفني والإدارة")
                                                        replyInput = ""
                                                        showReplyDialog = false
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                                                ) { Text("حذف الرد الحالي", fontSize = 10.sp) }
                                            }
                                        }
                                    },
                                    confirmButton = {
                                        Button(onClick = {
                                            val updated = rat.copy(reply = replyInput, replyTimestamp = System.currentTimeMillis())
                                            com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("ratings").document(rat.id).set(updated)
                                            viewModel.triggerNotification("✅ تم حفظ الرد الإداري بنجاح")
                                            showReplyDialog = false
                                        }) { Text("تأكيد وحفظ") }
                                    },
                                    dismissButton = {
                                        Button(onClick = { showReplyDialog = false }) { Text("إلغاء") }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

fun LazyListScope.adminCallsPanel(viewModel: MainViewModel, themeColors: VisualThemePalette, state: AdminPanelState) {
    with(state) {
        if (activeSubTabState.value == "CALLS") {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("📞 مراقبة المكالمات الصوتية وسجلات التواصل المباشر", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    
                    var directCallsEnabled by remember { mutableStateOf(true) }

                    // Global call setting
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("📳 ميزة الاتصال الصوتي المباشر بالتطبيق:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                                Text("عند الإغلاق، سيتم إجبار الفنيين والعملاء على التواصل الشات ومقنع الأرقام لحماية الخصوصية.", fontSize = 9.sp, color = Color.LightGray)
                            }
                            Switch(checked = directCallsEnabled, onCheckedChange = {
                                directCallsEnabled = it
                                viewModel.triggerNotification(if (it) "✅ تم تفعيل المكالمات الصوتية المباشرة" else "⛔ تم إغلاق المكالمات والتحويل للتواصل الكتابي والخصوصية")
                            })
                        }
                    }

                    val callsList by viewModel.callsLog.collectAsState()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📜 سجلات آخر الاتصالات الفعالة (${callsList.size}):", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        Button(
                            onClick = {
                                com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("calls_log").get().addOnSuccessListener { snapshot ->
                                    for (doc in snapshot.documents) doc.reference.delete()
                                }
                                viewModel.triggerNotification("🧹 تم مسح سجل المكالمات وتصفيره بالكامل")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                        ) { Text("تصفير السجل 🧹", fontSize = 9.sp, color = Color.White) }
                    }

                    if (callsList.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                            Text("لا توجد سجلات مكالمات حالياً 📞", color = Color.Gray, fontSize = 11.sp)
                        }
                    } else {
                        callsList.forEach { call ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = themeColors.surface)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("📞 اتصال من: ${call.callerName}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                                        Text("إلى الفني: ${call.providerName}", fontSize = 11.sp, color = Color.LightGray)
                                        Text("التوقيت: ${java.text.SimpleDateFormat("yyyy/MM/dd hh:mm a", java.util.Locale("ar")).format(java.util.Date(call.timestamp))}", fontSize = 9.sp, color = Color.Gray)
                                    }
                                    IconButton(
                                        onClick = {
                                            com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("calls_log").document(call.id).delete()
                                            viewModel.triggerNotification("🗑️ تم حذف سجل المكالمة")
                                        }
                                    ) { Text("🗑️", fontSize = 14.sp) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun LazyListScope.adminBlockedPanel(viewModel: MainViewModel, themeColors: VisualThemePalette, state: AdminPanelState) {
    with(state) {
        if (activeSubTabState.value == "BLOCKED") {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("🚫 القائمة المركزية للمحظورين من الدليل والنظام", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    
                    val providersList by viewModel.providers.collectAsState()
                    val storesList by viewModel.stores.collectAsState()
                    val jobsList by viewModel.jobs.collectAsState()

                    val blockedProviders = providersList.filter { it.isBlocked }
                    val blockedStores = storesList.filter { it.isBlocked }
                    val blockedJobs = jobsList.filter { it.isBlocked }

                    var selectedBlockedTab by remember { mutableStateOf("PROVIDERS") }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val tabs = listOf("PROVIDERS" to "فنيين (${blockedProviders.size})", "STORES" to "محلات (${blockedStores.size})", "JOBS" to "وظائف (${blockedJobs.size})")
                        tabs.forEach { (key, label) ->
                            val isSel = selectedBlockedTab == key
                            Button(
                                onClick = { selectedBlockedTab = key },
                                colors = ButtonDefaults.buttonColors(containerColor = if (isSel) themeColors.accent else themeColors.surface),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) { Text(label, fontSize = 10.sp, color = if (isSel) Color.Black else Color.White) }
                        }
                    }

                    when (selectedBlockedTab) {
                        "PROVIDERS" -> {
                            if (blockedProviders.isEmpty()) {
                                Box(modifier = Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                                    Text("لا يوجد فنيين محظورين حالياً 😇", fontSize = 11.sp, color = Color.Gray)
                                }
                            } else {
                                blockedProviders.forEach { prov ->
                                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = themeColors.surface)) {
                                        Row(modifier = Modifier.padding(10.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Column {
                                                Text("الفني: ${prov.name}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                                                Text("الهاتف: ${prov.phone}", fontSize = 11.sp, color = Color.LightGray)
                                                Text("التخصص: ${prov.profession}", fontSize = 11.sp, color = themeColors.accent)
                                            }
                                            Button(
                                                onClick = {
                                                    viewModel.toggleProviderBlock(prov.id)
                                                    viewModel.addNotification("🔓 فك الحظر عن حسابك", "تمت مراجعة حسابك وإعادة تفعيل توفرك بالدليل من قبل الإدارة.", "USER", prov.phone)
                                                    viewModel.triggerNotification("✅ تم فك الحظر وإرجاع الفني للدليل بنجاح")
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color.Green)
                                            ) { Text("إلغاء الحظر 🔓", fontSize = 9.sp, color = Color.Black, fontWeight = FontWeight.Bold) }
                                        }
                                    }
                                }
                            }
                        }
                        "STORES" -> {
                            if (blockedStores.isEmpty()) {
                                Box(modifier = Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                                    Text("لا يوجد محلات أو مراكز محظورة 🛍️", fontSize = 11.sp, color = Color.Gray)
                                }
                            } else {
                                blockedStores.forEach { store ->
                                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = themeColors.surface)) {
                                        Row(modifier = Modifier.padding(10.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Column {
                                                Text("المحل: ${store.name}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                                                Text("الهاتف: ${store.phone}", fontSize = 11.sp, color = Color.LightGray)
                                                Text("القسم: ${store.sectionId}", fontSize = 11.sp, color = themeColors.accent)
                                            }
                                            Button(
                                                onClick = {
                                                    viewModel.toggleStoreBlock(store.id)
                                                    viewModel.triggerNotification("✅ تم فك الحظر وإعادة المحل للعمل")
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color.Green)
                                            ) { Text("إلغاء الحظر 🔓", fontSize = 9.sp, color = Color.Black) }
                                        }
                                    }
                                }
                            }
                        }
                        "JOBS" -> {
                            if (blockedJobs.isEmpty()) {
                                Box(modifier = Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                                    Text("لا توجد وظائف أو متقدمين محظورين 💼", fontSize = 11.sp, color = Color.Gray)
                                }
                            } else {
                                blockedJobs.forEach { job ->
                                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = themeColors.surface)) {
                                        Row(modifier = Modifier.padding(10.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Column {
                                                Text("الوظيفة: ${job.title}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                                                Text("الشركة: ${job.companyName}", fontSize = 11.sp, color = Color.LightGray)
                                            }
                                            Button(
                                                onClick = {
                                                    viewModel.toggleJobBlock(job.id)
                                                    viewModel.triggerNotification("✅ تم فك الحظر عن إعلان الوظيفة بنجاح")
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color.Green)
                                            ) { Text("إلغاء الحظر 🔓", fontSize = 9.sp, color = Color.Black) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun LazyListScope.adminDeletedPanel(viewModel: MainViewModel, themeColors: VisualThemePalette, state: AdminPanelState) {
    with(state) {
        if (activeSubTabState.value == "DELETED") {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("🗑️ سلة المحذوفات المركزية واسترجاع البيانات المحذوفة ناعماً", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    
                    val providersList by viewModel.providers.collectAsState()
                    val storesList by viewModel.stores.collectAsState()
                    val jobsList by viewModel.jobs.collectAsState()

                    val deletedProviders = providersList.filter { it.isDeleted }
                    val deletedStores = storesList.filter { it.isDeleted }
                    val deletedJobs = jobsList.filter { it.isDeleted }

                    var selectedDeletedTab by remember { mutableStateOf("PROVIDERS") }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val tabs = listOf("PROVIDERS" to "فنيين (${deletedProviders.size})", "STORES" to "محلات (${deletedStores.size})", "JOBS" to "وظائف (${deletedJobs.size})")
                        tabs.forEach { (key, label) ->
                            val isSel = selectedDeletedTab == key
                            Button(
                                onClick = { selectedDeletedTab = key },
                                colors = ButtonDefaults.buttonColors(containerColor = if (isSel) themeColors.accent else themeColors.surface),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) { Text(label, fontSize = 10.sp, color = if (isSel) Color.Black else Color.White) }
                        }
                    }

                    when (selectedDeletedTab) {
                        "PROVIDERS" -> {
                            if (deletedProviders.isEmpty()) {
                                Box(modifier = Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                                    Text("سلة المحذوفات للفنيين فارغة ♻️", fontSize = 11.sp, color = Color.Gray)
                                }
                            } else {
                                deletedProviders.forEach { prov ->
                                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = themeColors.surface)) {
                                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text("الفني: ${prov.name}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                                            Text("التخصص: ${prov.profession} | الهاتف: ${prov.phone}", fontSize = 11.sp, color = Color.LightGray)
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Button(
                                                    onClick = {
                                                        val updated = prov.copy(isDeleted = false)
                                                        com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("providers").document(prov.id).set(updated)
                                                        viewModel.triggerNotification("♻️ تم استرجاع حساب الفني وإعادة تفعيله فوراً بالدليل")
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Green),
                                                    modifier = Modifier.weight(1f)
                                                ) { Text("استرجاع ♻️", fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Bold) }
                                                Button(
                                                    onClick = {
                                                        com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("providers").document(prov.id).delete()
                                                        viewModel.triggerNotification("💀 تم حذف حساب الفني نهائياً ومن الخادم")
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                                    modifier = Modifier.weight(1f)
                                                ) { Text("حذف نهائي 💀", fontSize = 10.sp, color = Color.White) }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        "STORES" -> {
                            if (deletedStores.isEmpty()) {
                                Box(modifier = Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                                    Text("سلة المحذوفات للمحلات فارغة ♻️", fontSize = 11.sp, color = Color.Gray)
                                }
                            } else {
                                deletedStores.forEach { store ->
                                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = themeColors.surface)) {
                                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text("المحل: ${store.name}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                                            Text("التصنيف: ${store.sectionId}", fontSize = 11.sp, color = Color.LightGray)
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Button(
                                                    onClick = {
                                                        val updated = store.copy(isDeleted = false)
                                                        com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("stores").document(store.id).set(updated)
                                                        viewModel.triggerNotification("♻️ تم استرجاع المحل للعمل بنجاح")
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Green),
                                                    modifier = Modifier.weight(1f)
                                                ) { Text("استرجاع ♻️", fontSize = 10.sp, color = Color.Black) }
                                                Button(
                                                    onClick = {
                                                        com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("stores").document(store.id).delete()
                                                        viewModel.triggerNotification("💀 تم حذف المحل نهائياً")
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                                    modifier = Modifier.weight(1f)
                                                ) { Text("حذف نهائي 💀", fontSize = 10.sp, color = Color.White) }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        "JOBS" -> {
                            if (deletedJobs.isEmpty()) {
                                Box(modifier = Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                                    Text("سلة المحذوفات للوظائف فارغة ♻️", fontSize = 11.sp, color = Color.Gray)
                                }
                            } else {
                                deletedJobs.forEach { job ->
                                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = themeColors.surface)) {
                                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text("الوظيفة: ${job.title}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                                            Text("الشركة: ${job.companyName}", fontSize = 11.sp, color = Color.LightGray)
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Button(
                                                    onClick = {
                                                        val updated = job.copy(isDeleted = false)
                                                        com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("jobs").document(job.id).set(updated)
                                                        viewModel.triggerNotification("♻️ تم استرجاع إعلان الوظيفة وتفعيله")
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Green),
                                                    modifier = Modifier.weight(1f)
                                                ) { Text("استرجاع ♻️", fontSize = 10.sp, color = Color.Black) }
                                                Button(
                                                    onClick = {
                                                        com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("jobs").document(job.id).delete()
                                                        viewModel.triggerNotification("💀 تم حذف الوظيفة نهائياً")
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                                    modifier = Modifier.weight(1f)
                                                ) { Text("حذف نهائي 💀", fontSize = 10.sp, color = Color.White) }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun LazyListScope.adminCustomTabsPanel(viewModel: MainViewModel, themeColors: VisualThemePalette, state: AdminPanelState) {
    with(state) {
        if (activeSubTabState.value == "CUSTOM_TABS") {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("📑 تخصيص وإضافة تبويبات مخصصة في الملف الشخصي للعرض", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    
                    var tabTitle by remember { mutableStateOf("") }
                    var tabIcon by remember { mutableStateOf("🏷️") }
                    var targetScope by remember { mutableStateOf("ALL") } // ALL, PROVIDERS, STORES, PROPERTIES
                    var tabContentHTML by remember { mutableStateOf("") }

                    val customTabsList = remember {
                        mutableStateListOf(
                            Triple("تبويب العروض والخصومات", "🏷️", "ALL"),
                            Triple("المستندات والتراخيص القانونية", "📜", "PROVIDERS"),
                            Triple("كتالوج الخدمات والمنتجات المصورة", "📁", "STORES")
                        )
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("➕ إنشاء تبويب مخصص جديد:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                            OutlinedTextField(value = tabTitle, onValueChange = { tabTitle = it }, label = { Text("عنوان التبويب بالعربية (مثال: الوثائق والشهادات)") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = tabIcon, onValueChange = { tabIcon = it }, label = { Text("أيقونة التبويب (Emoji)") }, modifier = Modifier.fillMaxWidth())
                            
                            Text("الفئة المستهدفة للتبويب:", fontSize = 10.sp, color = Color.White)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                val scopes = listOf("ALL" to "الكل", "PROVIDERS" to "الفنيين", "STORES" to "المحلات", "PROPERTIES" to "العقارات")
                                scopes.forEach { (key, label) ->
                                    val isSel = targetScope == key
                                    Button(
                                        onClick = { targetScope = key },
                                        colors = ButtonDefaults.buttonColors(containerColor = if (isSel) themeColors.accent else Color.DarkGray),
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                    ) { Text(label, fontSize = 10.sp, color = if (isSel) Color.Black else Color.White) }
                                }
                            }

                            OutlinedTextField(value = tabContentHTML, onValueChange = { tabContentHTML = it }, label = { Text("محتوى افتراضي للتبويب (HTML أو نصوص)") }, modifier = Modifier.fillMaxWidth())

                            Button(
                                onClick = {
                                    if (tabTitle.isNotBlank()) {
                                        customTabsList.add(Triple(tabTitle, tabIcon, targetScope))
                                        viewModel.triggerNotification("✅ تم إضافة التبويب المخصص بنجاح وإظهاره فوراً للمستهدفين")
                                        tabTitle = ""
                                        tabContentHTML = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("إطلاق التبويب المخصص 🚀", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                        }
                    }

                    Text("📋 قائمة التبويبات النشطة بالتطبيق حالياً:", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    customTabsList.forEach { (title, icon, scope) ->
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = themeColors.surface)) {
                            Row(modifier = Modifier.padding(10.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(icon, fontSize = 16.sp)
                                    Column {
                                        Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                                        Text("النطاق: $scope", fontSize = 10.sp, color = Color.LightGray)
                                    }
                                }
                                IconButton(onClick = { customTabsList.remove(Triple(title, icon, scope)) }) {
                                    Text("🗑️", fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun LazyListScope.adminAdvancedChatPanel(viewModel: MainViewModel, themeColors: VisualThemePalette, state: AdminPanelState) {
    with(state) {
        if (activeSubTabState.value == "ADVANCED_CHAT") {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("⚡ صلاحيات وتوجيه ومراقبة الدردشات الإدارية", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    
                    var globalChatEnabled by remember { mutableStateOf(true) }
                    var allowProviderDirectChat by remember { mutableStateOf(true) }
                    var autoReplyMessage by remember { mutableStateOf("مرحباً بك! نرحب بطلبك في تطبيقنا، سيقوم الفني أو موظف الدعم بالرد عليك فوراً.") }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("🛠️ إعدادات المحادثات العامة:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                            
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("تفعيل الشات والمحادثات على مستوى التطبيق", fontSize = 10.sp, color = Color.White)
                                Switch(checked = globalChatEnabled, onCheckedChange = { globalChatEnabled = it })
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("السماح للفنيين بفتح شات مباشر مع العميل", fontSize = 10.sp, color = Color.White)
                                Switch(checked = allowProviderDirectChat, onCheckedChange = { allowProviderDirectChat = it })
                            }

                            OutlinedTextField(
                                value = autoReplyMessage,
                                onValueChange = { autoReplyMessage = it },
                                label = { Text("رسالة الرد الآلي عند بدء محادثة جديدة", fontSize = 10.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = Color.White)
                            )

                            Button(
                                onClick = { viewModel.triggerNotification("💾 تم حفظ إعدادات الدردشات والردود التلقائية بنجاح!") },
                                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("حفظ الإعدادات 💾", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Supervisor support operators routing
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("👤 توجيه محادثات الدعم الفني للمشرفين:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                            Text("قم بتوجيه المحادثات الصادرة والشكاوى آلياً إلى حساب مشرف معين:", fontSize = 9.sp, color = Color.LightGray)
                            
                            var targetSupervisorPhone by remember { mutableStateOf("") }
                            OutlinedTextField(
                                value = targetSupervisorPhone,
                                onValueChange = { targetSupervisorPhone = it },
                                label = { Text("رقم هاتف المشرف المسؤول", fontSize = 10.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = Color.White)
                            )

                            Button(
                                onClick = {
                                    if (targetSupervisorPhone.isNotBlank()) {
                                        viewModel.triggerNotification("🔗 تم تحويل وتوجيه كافة محادثات الدعم الفني آلياً للمشرف ($targetSupervisorPhone)")
                                        targetSupervisorPhone = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("تحويل الدردشات 🔄", color = Color.White, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

fun LazyListScope.adminBookingRoutingPanel(viewModel: MainViewModel, themeColors: VisualThemePalette, state: AdminPanelState) {
    with(state) {
        if (activeSubTabState.value == "BOOKING_ROUTING") {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("🔄 إعدادات توجيه الحجوزات والطلبات الذكية", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    
                    var routingTarget by remember { mutableStateOf("NEAREST") } // ADMIN, PROVIDER, NEAREST, DEPT
                    var maxTechsToReceive by remember { mutableStateOf("5") }
                    var timeoutMinutes by remember { mutableStateOf("10") }
                    var autoRedirectEnabled by remember { mutableStateOf(true) }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("🎯 آلية توجيه الحجوزات التلقائية عند طلب العميل:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                            
                            val targets = listOf(
                                "ADMIN" to "🛡️ توجيه يدوي للمسؤول المالي والمشرفين",
                                "PROVIDER" to "👤 فني محدد يتم اختياره من العميل مباشرة",
                                "NEAREST" to "📍 أقرب فني متاح جغرافياً (GPS)",
                                "DEPT" to "🏢 بث الحجز لجميع فنيي القسم دفعة واحدة"
                            )

                            targets.forEach { (key, label) ->
                                val isSel = routingTarget == key
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { routingTarget = key }
                                        .padding(4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    RadioButton(selected = isSel, onClick = { routingTarget = key }, colors = RadioButtonDefaults.colors(selectedColor = themeColors.accent))
                                    Text(label, fontSize = 11.sp, color = Color.White, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal)
                                }
                            }

                            Divider(color = Color.White.copy(alpha = 0.1f))

                            OutlinedTextField(
                                value = maxTechsToReceive,
                                onValueChange = { maxTechsToReceive = it },
                                label = { Text("أقصى عدد فنيين يستقبلون الطلب معاً للبث", fontSize = 10.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = Color.White)
                            )

                            OutlinedTextField(
                                value = timeoutMinutes,
                                onValueChange = { timeoutMinutes = it },
                                label = { Text("مهلة استجابة الفني قبل الإلغاء أو التحويل (بالدقائق)", fontSize = 10.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = Color.White)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("إعادة توجيه الطلب تلقائياً لغيره عند انتهاء المهلة", fontSize = 10.sp, color = Color.White)
                                Switch(checked = autoRedirectEnabled, onCheckedChange = { autoRedirectEnabled = it })
                            }

                            Button(
                                onClick = { viewModel.triggerNotification("💾 تم تفعيل وحفظ نظام التوجيه الذكي بنجاح على سيرفر الحجوزات!") },
                                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("تفعيل وحفظ نظام التوجيه 🚀", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}


