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
                Column(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("📅 الحجوزات والطلبات الميدانية", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    val bookings by viewModel.bookings.collectAsState()
                    if (bookings.isEmpty()) {
                        Text("لا توجد حجوزات مسجلة حالياً.", color = Color.LightGray, fontSize = 11.sp)
                    } else {
                        bookings.forEach { b ->
                            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = themeColors.surface)) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("حجز للعميل: ${b.clientName} (${b.clientPhone})", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                                    Text("الحالة: ${b.status} | الموعد: ${b.date}", color = Color.LightGray, fontSize = 10.sp)
                                }
                            }
                        }
                    }
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
        if (activeSubTabState.value in listOf("PAYMENTS", "VIP", "COUPONS")) {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("💳 نظام الدفع والتحقق والمحافظ وترقيات VIP", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    Text("إدارة المحافظ الإلكترونية (جيب، جوال، وغيرها) وترقيات التميز وربط الحسابات.", color = Color.LightGray, fontSize = 11.sp)
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
                    Text("🏪 إدارة المحلات التجارية والمراكز كاملة", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    
                    var newStoreName by remember { mutableStateOf("") }
                    var newStorePhone by remember { mutableStateOf("") }
                    var newStoreDesc by remember { mutableStateOf("") }

                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = themeColors.surface)) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("➕ إضافة محل تجاري جديد يدوياً:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            OutlinedTextField(
                                value = newStoreName,
                                onValueChange = { newStoreName = it },
                                label = { Text("الاسم التجاري للمحل") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = newStorePhone,
                                onValueChange = { newStorePhone = it },
                                label = { Text("رقم الهاتف / الواتساب") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = newStoreDesc,
                                onValueChange = { newStoreDesc = it },
                                label = { Text("وصف المحلات والتفاصيل") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(
                                onClick = {
                                    if (newStoreName.isNotBlank()) {
                                        viewModel.addStore(newStoreName, newStoreDesc, newStorePhone)
                                        newStoreName = ""
                                        newStorePhone = ""
                                        newStoreDesc = ""
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary)
                            ) {
                                Text("حفظ وإضافة المحل 🏪", color = Color.White)
                            }
                        }
                    }

                    Text("📋 المحلات التجارية المسجلة:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    val storesList by viewModel.stores.collectAsState()
                    if (storesList.isEmpty()) {
                        Text("لا توجد محلات مسجلة حالياً.", color = Color.LightGray, fontSize = 11.sp)
                    } else {
                        storesList.forEach { store ->
                            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = themeColors.surface)) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text(store.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            if (store.isVip) Text("🏆 VIP", color = Color.Yellow, fontSize = 10.sp)
                                            if (store.isBlocked) Text("🚫 محظور", color = Color.Red, fontSize = 10.sp)
                                        }
                                    }
                                    Text("الوصف: ${store.description}", color = Color.LightGray, fontSize = 10.sp)
                                    Text("📞 الهاتف: ${store.phone}", color = Color.LightGray, fontSize = 10.sp)

                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Button(onClick = { viewModel.toggleStoreVip(store.id) }, colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)) {
                                            Text(if (store.isVip) "إلغاء VIP" else "تفعيل VIP", fontSize = 9.sp, color = Color.White)
                                        }
                                        Button(onClick = { viewModel.toggleStoreBlock(store.id) }, colors = ButtonDefaults.buttonColors(containerColor = if (store.isBlocked) Color.Green else Color.Red)) {
                                            Text(if (store.isBlocked) "إلغاء الحظر" else "حظر", fontSize = 9.sp, color = Color.White)
                                        }
                                        Button(onClick = { viewModel.deleteStore(store.id) }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f))) {
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

fun LazyListScope.adminRestaurantsPanel(viewModel: MainViewModel, themeColors: VisualThemePalette, state: AdminPanelState) {
    with(state) {
        if (activeSubTabState.value == "RESTAURANTS") {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("🍔 إدارة المطاعم والكافيهات وقوائم الطعام", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    
                    var restName by remember { mutableStateOf("") }
                    var restPhone by remember { mutableStateOf("") }
                    var restDesc by remember { mutableStateOf("") }

                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = themeColors.surface)) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("➕ إضافة مطعم / كافيه جديد يدوياً:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            OutlinedTextField(
                                value = restName,
                                onValueChange = { restName = it },
                                label = { Text("اسم المطعم / الكافيه") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = restPhone,
                                onValueChange = { restPhone = it },
                                label = { Text("رقم الهاتف والطلبات") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = restDesc,
                                onValueChange = { restDesc = it },
                                label = { Text("نوع المأكولات والساعات") },
                                modifier = Modifier.fillMaxWidth()
                            )
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
                    Text("🏥 إدارة المراكز الطبية والعيادات والأطباء", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    
                    var medName by remember { mutableStateOf("") }
                    var medPhone by remember { mutableStateOf("") }
                    var medSpec by remember { mutableStateOf("") }

                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = themeColors.surface)) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("➕ إضافة مركز طبي / عيادة جديدة يدوياً:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            OutlinedTextField(
                                value = medName,
                                onValueChange = { medName = it },
                                label = { Text("اسم المركز الطبي / العيادة") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = medPhone,
                                onValueChange = { medPhone = it },
                                label = { Text("رقم الطوارئ والاستعلامات") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = medSpec,
                                onValueChange = { medSpec = it },
                                label = { Text("التخصصات الطبية وساعات العمل") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(
                                onClick = {
                                    if (medName.isNotBlank()) {
                                        viewModel.addStore(medName, "مركز طبي: $medSpec", medPhone)
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
                Column(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("🧹 تهيئة وتنظيف بيانات النظام وإعادة الضبط", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    
                    var wipeProviders by remember { mutableStateOf(false) }
                    var wipeBookings by remember { mutableStateOf(false) }
                    var wipeChats by remember { mutableStateOf(false) }
                    var wipeStores by remember { mutableStateOf(false) }

                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = themeColors.surface)) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("اختر البيانات المراد تهيئتها وإفراغها:", fontSize = 11.sp, color = Color.White)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = wipeProviders, onCheckedChange = { wipeProviders = it })
                                Text("إفراغ الفنيين وأعضاء الدليل", fontSize = 11.sp, color = Color.White)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = wipeBookings, onCheckedChange = { wipeBookings = it })
                                Text("إفراغ جميع الحجوزات", fontSize = 11.sp, color = Color.White)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = wipeChats, onCheckedChange = { wipeChats = it })
                                Text("إفراغ سلة المحادثات والرسائل", fontSize = 11.sp, color = Color.White)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = wipeStores, onCheckedChange = { wipeStores = it })
                                Text("إفراغ المحلات والعقارات والوظائف", fontSize = 11.sp, color = Color.White)
                            }
                            Button(
                                onClick = {
                                    viewModel.wipeSelectedData(
                                        wipeProviders = wipeProviders,
                                        wipeBookings = wipeBookings,
                                        wipeChats = wipeChats,
                                        wipeStores = wipeStores,
                                        wipeProperties = wipeStores,
                                        wipeJobs = wipeStores
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                            ) {
                                Text("تأكيد تنظيف البيانات المحددة 🧹", color = Color.White)
                            }
                        }
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
                Column(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("⭐ إدارة التقييمات والتعليقات والردود", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    Text("مراقبة التقييمات والموافقة عليها وحذف التقييمات غير اللائقة مع إمكانية رد الإدارة.", color = Color.LightGray, fontSize = 11.sp)
                }
            }
        }
    }
}

fun LazyListScope.adminCallsPanel(viewModel: MainViewModel, themeColors: VisualThemePalette, state: AdminPanelState) {
    with(state) {
        if (activeSubTabState.value == "CALLS") {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("📞 مراقبة المكالمات الصوتية وسجلات التواصل", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    Text("عرض وتتبع سجل مكالمات التطبيق وإمكانية تفعيل أو تعطيل الاتصال المباشر.", color = Color.LightGray, fontSize = 11.sp)
                }
            }
        }
    }
}

fun LazyListScope.adminBlockedPanel(viewModel: MainViewModel, themeColors: VisualThemePalette, state: AdminPanelState) {
    with(state) {
        if (activeSubTabState.value == "BLOCKED") {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("🚫 القائمة المحظورة المركزية لجميع الفئات", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    Text("قائمة موحدة بجميع الحسابات والفنيين والمحلات والعقارات المحظورة مع إمكانية فك الحظر السريع.", color = Color.LightGray, fontSize = 11.sp)
                }
            }
        }
    }
}

fun LazyListScope.adminDeletedPanel(viewModel: MainViewModel, themeColors: VisualThemePalette, state: AdminPanelState) {
    with(state) {
        if (activeSubTabState.value == "DELETED") {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("🗑️ سلة المحذوفات المركزية واستعادة البيانات", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    Text("عرض العناصر المحذوفة ناعماً وإمكانية استعادتها فوراً إلى قاعدة البيانات.", color = Color.LightGray, fontSize = 11.sp)
                }
            }
        }
    }
}

fun LazyListScope.adminCustomTabsPanel(viewModel: MainViewModel, themeColors: VisualThemePalette, state: AdminPanelState) {
    with(state) {
        if (activeSubTabState.value == "CUSTOM_TABS") {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("📑 تخصيص تبويبات الملفات الشخصية والعرض", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    Text("إضافة وإدارة التبويبات المخصصة لجميع مقدمي الخدمات والمستندات المرفقة.", color = Color.LightGray, fontSize = 11.sp)
                }
            }
        }
    }
}

