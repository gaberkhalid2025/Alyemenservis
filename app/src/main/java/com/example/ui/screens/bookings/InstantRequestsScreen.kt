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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.data.models.InstantRequestEntity
import com.example.data.models.RequestOfferEntity
import com.example.ui.MainViewModel
import com.example.utils.VisualThemePalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstantRequestsScreen(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onBackClick: () -> Unit = {}
) {
    val instantRequests by viewModel.instantRequests.collectAsState()
    val requestOffers by viewModel.requestOffers.collectAsState()
    val providers by viewModel.providers.collectAsState()
    val currentUserPhone by viewModel.currentUserPhone.collectAsState()
    val adminRole by viewModel.adminRole.collectAsState()
    val currentUserId by viewModel.currentUserId.collectAsState()
    val currentUserName by viewModel.currentUserName.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: طلباتي الكلي, 1: الفرص والمزايدات للفنيين
    var searchQuery by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedRequestForOffers by remember { mutableStateOf<InstantRequestEntity?>(null) }
    var selectedRequestForSubmitOffer by remember { mutableStateOf<InstantRequestEntity?>(null) }

    val filteredList = remember(instantRequests, selectedTab, searchQuery, currentUserPhone, currentUserId) {
        instantRequests.filter { req ->
            val matchesTab = if (selectedTab == 0) {
                // User's own requests or all if phone matches
                currentUserPhone.isBlank() || req.userPhone == currentUserPhone || req.userId == currentUserId || adminRole.isNotBlank()
            } else {
                // Technician marketplace view - requests open for bidding
                req.status == "WAITING_FOR_OFFERS" || req.status == "REVIEWING_OFFERS"
            }
            val matchesQuery = searchQuery.isBlank() ||
                    req.requestCode.contains(searchQuery, ignoreCase = true) ||
                    req.serviceTitle.contains(searchQuery, ignoreCase = true) ||
                    req.userCity.contains(searchQuery, ignoreCase = true)
            matchesTab && matchesQuery
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("🚨 اطلب خدمتك الآن (مزايدة فورية)", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                        Text("عروض أسعار تنافسية ومباشرة خلال ساعتين ⏱️", fontSize = 11.sp, color = Color(0xFFA7F3D0))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = Color(0xFF10B981),
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("اطلب خدمتك الآن ⚡", fontWeight = FontWeight.Bold) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF8FAFC))
        ) {
            // Tab Header (Customer Requests vs Technician Marketplace Bids)
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = Color(0xFF0F172A)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("طلباتي الفورية 📋", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("سوق المزايدات (للفنيين) 💰", fontWeight = FontWeight.Bold) }
                )
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("بحث بكود الطلب (R-XXXXXX)، المدينة، أو نوع الخدمة...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            )

            // Content List
            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🚨", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (selectedTab == 0) "لا توجد طلبات فورية خاصة بك حالياً.\nاضغط على 'اطلب خدمتك الآن' لإطلاق طلب مزايدة جديد!"
                            else "لا توجد طلبات مفتوحة للمزايدة حالياً من العملاء.",
                            textAlign = TextAlign.Center,
                            color = Color.Gray,
                            fontSize = 13.sp
                        )
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
            viewModel = viewModel,
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
            onSubmit = { price, arrivalTime, duration, notes ->
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
                    estimatedArrivalTime = arrivalTime,
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
        "REVIEWING_OFFERS" -> "متاحة ومراجعة العروض (${offers.size}) 💰"
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
                Text("القسم/التخصص: ${req.categoryName}", fontSize = 11.sp, color = Color.Gray)
            }
            Text("المنطقة/الموقع: ${req.userCity} - ${req.userNeighborhood}", fontSize = 11.sp, color = Color(0xFF475569))

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
                        Text("مقارنة العروض المقدمة (${offers.size}) 💰", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                        Text("تقديم عرض سعر ومزايدة 💰", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
    var timeLeftMs by remember(expiresAt) { mutableLongStateOf(maxOf(0L, expiresAt - System.currentTimeMillis())) }

    LaunchedEffect(expiresAt) {
        while (isActive && timeLeftMs > 0) {
            delay(1000)
            timeLeftMs = maxOf(0L, expiresAt - System.currentTimeMillis())
        }
    }

    DisposableEffect(expiresAt) {
        onDispose {
            // Safety cleanup on leave/dispose
        }
    }

    val totalSeconds = timeLeftMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    val formattedTime = String.format("%02d:%02d:%02d", hours, minutes, seconds)

    val timerColor = when {
        hours >= 1 || minutes >= 30 -> Color(0xFF10B981)
        minutes >= 10 -> Color(0xFFF59E0B)
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
            Text("المهلة المتبقية للمزايدة (ساعتان):", fontSize = 11.sp, color = Color(0xFF334155))
        }
        Text(
            if (timeLeftMs > 0) "⏱️ $formattedTime" else "⏰ انتهت مهلة المزايدة",
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = timerColor
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateInstantRequestDialog(
    userId: String,
    userName: String,
    userPhone: String,
    onDismiss: () -> Unit,
    onSubmit: (catId: String, catName: String, title: String, desc: String, city: String, area: String) -> Unit
) {
    // Auto-bind name and phone directly from profile
    var nameInput by remember(userName) { mutableStateOf(userName.ifBlank { "عميل جديد" }) }
    var phoneInput by remember(userPhone) { mutableStateOf(userPhone) }

    // Category dropdown options
    val categoryList = listOf(
        "كهرباء وصيانة منزلية ⚡",
        "سباكة وتمديدات مياه 🚰",
        "تكييف وتبريد ❄️",
        "صيانة أجهزة الكترونية 💻",
        "خدمات سيارات وميكانيك 🚗",
        "مستلزمات ومتاجر تجارية 🏪",
        "مطاعم ووجبات غذائية 🍔",
        "خدمات عامة ونقل 📦"
    )
    var selectedCategory by remember { mutableStateOf(categoryList[0]) }
    var isCategoryDropdownExpanded by remember { mutableStateOf(false) }

    var selectedSection by remember { mutableStateOf("SERVICES") }
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("صنعاء") }
    var area by remember { mutableStateOf("شارع الستين") }
    var urgencyTime by remember { mutableStateOf("فوراً (خلال ساعة)") }
    var errorMessage by remember { mutableStateOf("") }

    val isGuest = userId == "guest" || phoneInput.isBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("🚨 اطلب خدمتك الآن (مزايدة فورية)", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A))
                Text("⚡ سيتم إرسال الطلب للمتخصصين المتاحين بمنطقتك فقط", fontSize = 11.sp, color = Color(0xFF10B981))
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isGuest) {
                    Surface(
                        color = Color(0xFFFEF2F2),
                        border = BorderStroke(1.dp, Color(0xFFEF4444)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "🔒 حماية الخصوصية (AuthGuard): الزوار لا يمكنهم إرسال طلبات فورية. يرجى تسجيل الدخول بحسابك أولاً.",
                            color = Color(0xFF991B1B),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Text("1️⃣ اختيار القسم والتخصص المطلوب (قائمة منسدلة):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                
                ExposedDropdownMenuBox(
                    expanded = isCategoryDropdownExpanded,
                    onExpandedChange = { isCategoryDropdownExpanded = !isCategoryDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("اختر القسم أو التخصص المطلوب 📋") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCategoryDropdownExpanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = isCategoryDropdownExpanded,
                        onDismissRequest = { isCategoryDropdownExpanded = false }
                    ) {
                        categoryList.forEach { categoryOption ->
                            DropdownMenuItem(
                                text = { Text(categoryOption, fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                                onClick = {
                                    selectedCategory = categoryOption
                                    isCategoryDropdownExpanded = false
                                    selectedSection = when {
                                        categoryOption.contains("متجر") || categoryOption.contains("مستلزمات") -> "STORES"
                                        categoryOption.contains("مطاعم") || categoryOption.contains("وجبات") -> "RESTAURANTS"
                                        else -> "SERVICES"
                                    }
                                }
                            )
                        }
                    }
                }

                Text("2️⃣ بياناتك الشخصية والموقع (ربط تلقائي من البروفايل):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("الاسم") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = phoneInput,
                        onValueChange = { phoneInput = it },
                        label = { Text("رقم الهاتف") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

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
                        label = { Text("المنطقة / الشارع") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Text("3️⃣ تفاصيل وصف الخدمة:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان الخدمة (مثال: صيانة تسريب مياه أو أعطال أسلاك)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("اكتب تفاصيل ما تحتاجه بالضبط...") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("4️⃣ الوقت المطلوب للتنفيذ:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("فوراً (خلال ساعة)", "خلال ساعتين", "خلال 4 ساعات").forEach { time ->
                        FilterChip(
                            selected = urgencyTime == time,
                            onClick = { urgencyTime = time },
                            label = { Text(time, fontSize = 9.sp) }
                        )
                    }
                }

                Surface(
                    color = Color(0xFFF0FDF4),
                    border = BorderStroke(1.dp, Color(0xFF10B981)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "🔒 حماية الخصوصية: سيتم توجيه طلبك حصرياً للمتخصصين المتاحين بـ $city ولن يظهر رقم هاتفك الحقيقي إلا بعد قبول العرض.",
                        color = Color(0xFF166534),
                        fontSize = 10.sp,
                        modifier = Modifier.padding(8.dp)
                    )
                }

                if (errorMessage.isNotEmpty()) {
                    Text(errorMessage, color = Color.Red, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isGuest) {
                        errorMessage = "🔒 تم تفعيل AuthGuard: يرجى تسجيل الدخول بحسابك أولاً لإرسال الطلب."
                        return@Button
                    }
                    if (title.isBlank()) {
                        errorMessage = "⚠️ يرجى كتابة عنوان الخدمة المطلوبة."
                        return@Button
                    }
                    val fullDesc = "$desc | الوقت المطلوب: $urgencyTime"
                    onSubmit(selectedSection, selectedCategory, title, fullDesc, city, area)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("🚀 إرسال الطلب للمتخصصين فوراً", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { 
                Text("إلغاء", color = Color.Gray) 
            }
        }
    )
}

@Composable
fun ReviewOffersDialog(
    req: InstantRequestEntity,
    offers: List<RequestOfferEntity>,
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onAcceptOffer: (RequestOfferEntity) -> Unit
) {
    var selectedOfferForMap by remember { mutableStateOf<RequestOfferEntity?>(null) }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("💰 عروض الأسعار للطلب ${req.requestCode}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("قارن بين العروض، تفقد الموقع على الخريطة، واختر العرض الأنسب لك.", fontSize = 11.sp, color = Color.Gray)
            }
        },
        text = {
            if (offers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("⏱️", fontSize = 36.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "تم توجيه طلبك للفنيين والمتاجر المتخصصين بـ ${req.userCity}.\nفي انتظار تقديم العروض الأولى خلال لحظات...",
                            textAlign = TextAlign.Center,
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(offers) { offer ->
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                // Provider Info Header
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFF1E293B),
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(offer.technicianName.take(1), color = Color.White, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        Column {
                                            Text(offer.technicianName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                                            Text("⭐ ${offer.technicianRating} | 📍 على بعد ${offer.distanceKm} كم", fontSize = 11.sp, color = Color(0xFF475569))
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFF10B981).copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "${offer.price} ر.ي",
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF059669),
                                            fontSize = 15.sp,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("⏱️ وقت الوصول: ${offer.estimatedArrivalTime}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                                    Text("⏳ مدة التنفيذ: ${offer.estimatedDuration}", fontSize = 11.sp, color = Color(0xFF64748B))
                                }

                                if (offer.notes.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("📝 ملاحظات الضمان: ${offer.notes}", fontSize = 11.sp, color = Color(0xFF334155))
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Quick Actions: Map Location, Chat, Select Offer
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { selectedOfferForMap = offer },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("📍 موقع الفني", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            viewModel.getOrCreateChatChannel(offer.technicianId, offer.technicianName, req.userPhone, req.userName)
                                            Toast.makeText(context, "تم فتح المحادثة الفورية مع ${offer.technicianName}", Toast.LENGTH_SHORT).show()
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("💬 محادثة", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = { onAcceptOffer(offer) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.weight(1.2f)
                                    ) {
                                        Text("✅ اختيار العرض", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
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

    // Map Location Popup
    selectedOfferForMap?.let { offer ->
        ProviderMapLocationDialog(
            offer = offer,
            req = req,
            onDismiss = { selectedOfferForMap = null }
        )
    }
}

@Composable
fun ProviderMapLocationDialog(
    offer: RequestOfferEntity,
    req: InstantRequestEntity,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("📍 موقع الفني ومسافة الوصول", fontWeight = FontWeight.Bold, fontSize = 15.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    color = Color(0xFF0F172A),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🗺️ خريطة الموقع المباشرة", fontSize = 18.sp, color = Color.White)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("موقعك: ${req.userCity} - ${req.userNeighborhood}", fontSize = 11.sp, color = Color.LightGray)
                            Text("موقع الفني: على بعد ${offer.distanceKm} كم تقريباً", fontSize = 11.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Text("• الفني: ${offer.technicianName}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("• وقت الوصول المتوقع: ${offer.estimatedArrivalTime}", fontSize = 12.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                Text("• السعر المعروض: ${offer.price} ريال يمني", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
            ) {
                Text("فهمت 🤝", color = Color.White)
            }
        }
    )
}

@Composable
fun SubmitOfferDialog(
    req: InstantRequestEntity,
    onDismiss: () -> Unit,
    onSubmit: (price: Double, arrivalTime: String, duration: String, notes: String) -> Unit
) {
    var priceText by remember { mutableStateOf("") }
    var arrivalTime by remember { mutableStateOf("خلال 30 دقيقة") }
    var duration by remember { mutableStateOf("ساعتان") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تقديم عرض سعر للطلب ${req.requestCode}", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("خدمة: ${req.serviceTitle}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("مدينة/منطقة: ${req.userCity} - ${req.userNeighborhood}", fontSize = 11.sp, color = Color.Gray)
                Text("التفاصيل المطلوبة: ${req.description}", fontSize = 11.sp, color = Color(0xFF334155))
                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("المبلغ المقترح بالريال اليمني (ر.ي)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = arrivalTime,
                    onValueChange = { arrivalTime = it },
                    label = { Text("وقت الوصول المتوقع (مثال: خلال 20 دقيقة)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = duration,
                    onValueChange = { duration = it },
                    label = { Text("المدة المتوقعة للتنفيذ (مثال: ساعتان)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("تفاصيل العرض وضمان الخدمة") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val p = priceText.toDoubleOrNull() ?: 0.0
                    if (p <= 0.0) return@Button
                    onSubmit(p, arrivalTime, duration, notes)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
            ) {
                Text("إرسال العرض للمستخدم 📤", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
