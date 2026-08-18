package com.example.ui.screens.bookings

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.data.models.InstantRequestEntity
import com.example.data.models.RequestOfferEntity
import com.example.ui.MainViewModel
import com.example.utils.VisualThemePalette
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstantRequestsScreen(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val currentUserId by viewModel.currentUserId.collectAsState()
    val currentUserPhone by viewModel.currentUserPhone.collectAsState()
    val currentUserName by viewModel.currentUserName.collectAsState()
    val instantRequests by viewModel.instantRequests.collectAsState()
    val requestOffers by viewModel.requestOffers.collectAsState()
    val providers by viewModel.providers.collectAsState()

    // Determine role: Customer, Technician, or Admin
    val isTechnician = viewModel.isProviderUser || providers.any { it.phone == currentUserPhone || it.id == currentUserId }
    var selectedTab by remember { mutableIntStateOf(if (isTechnician) 1 else 0) } // 0: My Requests (Customer), 1: Available Requests (Tech), 2: Admin View

    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedRequestForOffers by remember { mutableStateOf<InstantRequestEntity?>(null) }
    var selectedRequestForSubmitOffer by remember { mutableStateOf<InstantRequestEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("الطلبات الفورية والمزايدة (30 دقيقة)", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                        Text("نظام المزايدة الفوري الموحد للخدمات", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Default.AddCircle, contentDescription = "طلب جديد", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1E293B))
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = Color(0xFF10B981),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("طلب فوري جديد ⚡", fontWeight = FontWeight.Bold)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF8FAFC))
        ) {
            // Tabs Bar
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = Color(0xFF1E293B)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("طلباتي الفورية 🙋‍♂️", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("الطلبات المتاحة 🛠️", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("سجل الكل 📊", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            val filteredList = when (selectedTab) {
                0 -> instantRequests.filter { it.userId == currentUserId || it.userPhone == currentUserPhone }
                1 -> instantRequests.filter { it.status == "WAITING_FOR_OFFERS" || it.status == "REVIEWING_OFFERS" }
                else -> instantRequests
            }

            if (filteredList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("⚡", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            when (selectedTab) {
                                0 -> "لا توجد طلبات فورية قائمة حالياً"
                                1 -> "لا توجد طلبات فورية متاحة للمزايدة حالياً"
                                else -> "سجل الطلبات الفورية فارغ"
                            },
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { showCreateDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                        ) {
                            Text("إنشاء طلب فوري الآن ⚡", color = Color.White)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredList) { req ->
                        InstantRequestCard(
                            req = req,
                            isCustomerView = selectedTab == 0 || req.userId == currentUserId,
                            offers = requestOffers.filter { it.requestId == req.id },
                            onViewOffers = { selectedRequestForOffers = req },
                            onSubmitOffer = { selectedRequestForSubmitOffer = req },
                            onCancel = { pass -> viewModel.cancelInstantRequest(req.id, pass, true, req.cancellationPassword) },
                            onComplete = { viewModel.completeInstantRequest(req.id) }
                        )
                    }
                }
            }
        }
    }

    // Modal: Create New Instant Request
    if (showCreateDialog) {
        CreateInstantRequestDialog(
            userId = currentUserId,
            userName = currentUserName,
            userPhone = currentUserPhone,
            onDismiss = { showCreateDialog = false },
            onSubmit = { catId, catName, title, desc, city, area ->
                viewModel.createInstantRequest(
                    userId = currentUserId.ifBlank { "user_123" },
                    userName = currentUserName.ifBlank { "عميل دليل اليمن" },
                    userPhone = currentUserPhone.ifBlank { "770000000" },
                    userCity = city,
                    userNeighborhood = area,
                    categoryId = catId,
                    categoryName = catName,
                    serviceTitle = title,
                    description = desc
                )
                showCreateDialog = false
            }
        )
    }

    // Modal: Customer Review Offers
    selectedRequestForOffers?.let { req ->
        val offers = requestOffers.filter { it.requestId == req.id }
        ReviewOffersDialog(
            req = req,
            offers = offers,
            onDismiss = { selectedRequestForOffers = null },
            onAcceptOffer = { offer ->
                viewModel.acceptRequestOffer(req, offer)
                selectedRequestForOffers = null
            }
        )
    }

    // Modal: Technician Submit Offer
    selectedRequestForSubmitOffer?.let { req ->
        SubmitOfferDialog(
            req = req,
            onDismiss = { selectedRequestForSubmitOffer = null },
            onSubmit = { price, duration, notes ->
                val myProv = providers.find { it.phone == currentUserPhone }
                viewModel.submitOfferForRequest(
                    requestId = req.id,
                    requestCode = req.requestCode,
                    technicianId = myProv?.id ?: currentUserId,
                    technicianName = myProv?.name ?: currentUserName.ifBlank { "فني معتمد" },
                    technicianPhone = myProv?.phone ?: currentUserPhone,
                    technicianAvatar = myProv?.profileImage ?: "",
                    technicianRating = myProv?.rating ?: 5.0f,
                    price = price,
                    estimatedDuration = duration,
                    notes = notes
                )
                selectedRequestForSubmitOffer = null
            }
        )
    }
}

@Composable
fun InstantRequestCard(
    req: InstantRequestEntity,
    isCustomerView: Boolean,
    offers: List<RequestOfferEntity>,
    onViewOffers: () -> Unit,
    onSubmitOffer: () -> Unit,
    onCancel: (String) -> Unit,
    onComplete: () -> Unit
) {
    var showCancelDialog by remember { mutableStateOf(false) }
    var cancelPassInput by remember { mutableStateOf("") }

    val statusColor = when (req.status) {
        "WAITING_FOR_OFFERS" -> Color(0xFFF59E0B)
        "REVIEWING_OFFERS" -> Color(0xFF3B82F6)
        "ACCEPTED" -> Color(0xFF10B981)
        "COMPLETED" -> Color(0xFF059669)
        "EXPIRED" -> Color(0xFF64748B)
        else -> Color(0xFFEF4444)
    }

    val statusText = when (req.status) {
        "WAITING_FOR_OFFERS" -> "في انتظار العروض ⏳"
        "REVIEWING_OFFERS" -> "يتم مراجعة العروض (${offers.size}) 💰"
        "ACCEPTED" -> "تم قبول العرض 🤝"
        "COMPLETED" -> "مكتمل ومُنفّذ ✅"
        "EXPIRED" -> "منتهي الصلاحية ⏰"
        else -> "ملغي 🚫"
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF1E293B)
                ) {
                    Text(
                        text = req.requestCode,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = statusColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Service Title & Category
            Text(req.serviceTitle, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A))
            if (req.categoryName.isNotEmpty()) {
                Text(req.categoryName, fontSize = 11.sp, color = Color.Gray)
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(req.description, fontSize = 13.sp, color = Color(0xFF334155), maxLines = 3)

            Spacer(modifier = Modifier.height(10.dp))

            // Countdown Timer
            if (req.status == "WAITING_FOR_OFFERS" || req.status == "REVIEWING_OFFERS") {
                BiddingCountdownTimer(expiresAt = req.expiresAt)
                Spacer(modifier = Modifier.height(10.dp))
            }

            // PRIVACY ENFORCEMENT: Customer secret PIN & Cancellation Pass
            if (isCustomerView) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFEF3C7), RoundedCornerShape(10.dp))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("الرمز السري للعميل", fontSize = 10.sp, color = Color(0xFF92400E))
                        Text("🔐 ${req.secretPin}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFFB45309))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("رمز إلغاء الطلب", fontSize = 10.sp, color = Color(0xFF92400E))
                        Text("🔑 ${req.cancellationPassword}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFFB45309))
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isCustomerView) {
                    Button(
                        onClick = onViewOffers,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.List, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("العروض المقدمة (${offers.size})", fontSize = 11.sp)
                    }

                    if (req.status != "COMPLETED" && req.status != "CANCELLED") {
                        OutlinedButton(
                            onClick = { showCancelDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("إلغاء", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // Technician View
                    Button(
                        onClick = onSubmitOffer,
                        enabled = req.status == "WAITING_FOR_OFFERS" || req.status == "REVIEWING_OFFERS",
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("تقديم عرض سعر 💰", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("إلغاء الطلب الفوري", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("أدخل رمز إلغاء الطلب (4 أرقام) للتأكيد:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = cancelPassInput,
                        onValueChange = { cancelPassInput = it },
                        label = { Text("رمز الإلغاء") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCancel(cancelPassInput)
                        showCancelDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("تأكيد الإلغاء", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
fun BiddingCountdownTimer(expiresAt: Long) {
    var timeLeftMs by remember { mutableLongStateOf(maxOf(0L, expiresAt - System.currentTimeMillis())) }

    LaunchedEffect(expiresAt) {
        while (timeLeftMs > 0) {
            delay(1000)
            timeLeftMs = maxOf(0L, expiresAt - System.currentTimeMillis())
        }
    }

    val totalSeconds = timeLeftMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val formattedTime = String.format("%02d:%02d", minutes, seconds)

    val timerColor = when {
        minutes >= 15 -> Color(0xFF10B981)
        minutes >= 5 -> Color(0xFFF59E0B)
        minutes > 0 || seconds > 0 -> Color(0xFFEF4444)
        else -> Color.Gray
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(timerColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .border(1.dp, timerColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(vertical = 6.dp, horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.DateRange, contentDescription = null, tint = timerColor, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("الوقت المتبقي للمزايدة (30 دقيقة):", fontSize = 11.sp, color = Color(0xFF334155))
        }
        Text(
            if (timeLeftMs > 0) "⏱️ $formattedTime" else "⏰ انتهت مهلة الـ 30 دقيقة",
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = timerColor
        )
    }
}

@Composable
fun CreateInstantRequestDialog(
    userId: String,
    userName: String,
    userPhone: String,
    onDismiss: () -> Unit,
    onSubmit: (catId: String, catName: String, title: String, desc: String, city: String, area: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("صنعاء") }
    var area by remember { mutableStateOf("الحدة") }
    var categoryName by remember { mutableStateOf("صيانة منازل وسباكة") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("⚡ طلب فوري جديد (30 دقيقة)", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان الخدمة المطلوب (مثال: إصلاح تسريب المياه)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("تفاصيل المشكلة والخدمة المطلوب تنفيذها") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it },
                        label = { Text("المدينة") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = area,
                        onValueChange = { area = it },
                        label = { Text("المنطقة / الحي") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) return@Button
                    onSubmit("services", categoryName, title, desc, city, area)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
            ) {
                Text("إرسال الطلب وإطلاق المزايدة ⚡", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@Composable
fun ReviewOffersDialog(
    req: InstantRequestEntity,
    offers: List<RequestOfferEntity>,
    onDismiss: () -> Unit,
    onAcceptOffer: (RequestOfferEntity) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("عروض الأسعار المقدمة لـ ${req.requestCode}", fontWeight = FontWeight.Bold) },
        text = {
            if (offers.isEmpty()) {
                Text("لا توجد عروض مقدمة من الفنيين حتى الآن. انتظر العروض خلال مهلة الـ 30 دقيقة.")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(offers) { offer ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(offer.technicianName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("${offer.price} ر.ي", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF10B981))
                                }
                                if (offer.estimatedDuration.isNotEmpty()) {
                                    Text("مدة التنفيذ: ${offer.estimatedDuration}", fontSize = 11.sp, color = Color.Gray)
                                }
                                if (offer.notes.isNotEmpty()) {
                                    Text("ملاحظات: ${offer.notes}", fontSize = 12.sp, color = Color(0xFF334155))
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Button(
                                    onClick = { onAcceptOffer(offer) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("اختيار هذا العرض 🤝", fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("إغلاق") }
        }
    )
}

@Composable
fun SubmitOfferDialog(
    req: InstantRequestEntity,
    onDismiss: () -> Unit,
    onSubmit: (price: Double, duration: String, notes: String) -> Unit
) {
    var priceText by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("ساعة واحدة") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تقديم عرض سعر للطلب ${req.requestCode}", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("خدمة: ${req.serviceTitle}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("مدينة/منطقة: ${req.userCity} - ${req.userNeighborhood}", fontSize = 11.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("المبلغ التقديري بالريال اليمني (ر.ي)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = duration,
                    onValueChange = { duration = it },
                    label = { Text("المدة المتوقعة للتنفيذ") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظاتك وضمان الخدمة") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val p = priceText.toDoubleOrNull() ?: 0.0
                    if (p <= 0.0) return@Button
                    onSubmit(p, duration, notes)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
            ) {
                Text("إرسال العرض 💰", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
