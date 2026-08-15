@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.ui.screens.bookings

import com.example.ui.*
import com.example.ui.utils.*


import android.content.Intent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.PathEffect
import okhttp3.MediaType.Companion.toMediaType

import com.example.*

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.*
import com.example.utils.*
import com.example.ui.MainViewModel
import com.example.ui.components.*
import com.example.ui.dialogs.*
import com.example.ui.screens.home.*
import com.example.ui.screens.map.*
import com.example.ui.screens.bookings.*
import com.example.ui.screens.admin.*
import com.example.ui.screens.assistant.*
import com.example.ui.screens.register.*
import com.example.ui.screens.status.*
import com.example.ui.screens.about.*
import com.example.viewmodels.*
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


@Composable
fun BookingsScreenLayout(viewModel: MainViewModel, themeColors: VisualThemePalette) {
    val bookings by viewModel.bookings.collectAsState()
    val currentUserPhone by viewModel.currentUserPhone.collectAsState()
    val providers by viewModel.providers.collectAsState()
    val stores by viewModel.stores.collectAsState()
    val properties by viewModel.properties.collectAsState()
    val chatChannels by viewModel.chatChannels.collectAsState()
    val adminRole by viewModel.adminRole.collectAsState()
    val currentUserId by viewModel.currentUserId.collectAsState()
    val currentUserName by viewModel.currentUserName.collectAsState()
    val paymentsList by viewModel.payments.collectAsState()
    val paymentWallets by viewModel.paymentWallets.collectAsState()

    var payingBookingObj by remember { mutableStateOf<com.example.data.BookingEntity?>(null) }
    var selectedUserWalletObj by remember { mutableStateOf<com.example.data.PaymentWalletEntity?>(null) }
    var userTransferIdInput by remember { mutableStateOf("") }
    var userTransferAccountNameInput by remember { mutableStateOf("") }
    var userTransferPhotoInput by remember { mutableStateOf("") }

    var unlockedBookingIds by remember { mutableStateOf(setOf<String>()) }

    val context = LocalContext.current
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    var phoneInput by remember { mutableStateOf(currentUserPhone) }
    var activeSearchPhone by remember { mutableStateOf(currentUserPhone.ifEmpty { "ALL" }) }

    LaunchedEffect(currentUserPhone) {
        if (currentUserPhone.isNotEmpty()) {
            activeSearchPhone = currentUserPhone
            phoneInput = currentUserPhone
        }
    }

    val matchingProvider = remember(providers, activeSearchPhone) {
        providers.find { it.phone.trim() == activeSearchPhone.trim() && activeSearchPhone.isNotEmpty() && activeSearchPhone != "ALL" }
    }

    val matchingStore = remember(stores, activeSearchPhone) {
        stores.find { it.phone.trim() == activeSearchPhone.trim() && activeSearchPhone.isNotEmpty() && activeSearchPhone != "ALL" }
    }

    val matchingProperty = remember(properties, activeSearchPhone) {
        properties.find { it.phone.trim() == activeSearchPhone.trim() && activeSearchPhone.isNotEmpty() && activeSearchPhone != "ALL" }
    }

    // Filter customer bookings strictly to protect user privacy
    val myCustomerBookings = remember(bookings, activeSearchPhone, currentUserPhone, adminRole) {
        val isAdmin = adminRole != "GUEST" && adminRole != "SUPERVISOR"
        val phoneToMatch = activeSearchPhone.trim()
        val currentPhoneToMatch = currentUserPhone.trim()
        
        if (isAdmin) {
            if (phoneToMatch.isEmpty()) {
                bookings.sortedByDescending { it.id }
            } else {
                bookings.filter { it.customerPhone.trim() == phoneToMatch }.sortedByDescending { it.id }
            }
        } else {
            if (phoneToMatch.isEmpty() && currentPhoneToMatch.isEmpty()) {
                emptyList()
            } else {
                bookings.filter { 
                    (phoneToMatch.isNotEmpty() && it.customerPhone.trim() == phoneToMatch) ||
                    (currentPhoneToMatch.isNotEmpty() && it.customerPhone.trim() == currentPhoneToMatch)
                }.sortedByDescending { it.id }
            }
        }
    }

    // Filter technician / store / property received bookings + matching section-specific urgent requests
    val receivedBookings = remember(bookings, matchingProvider, matchingStore, matchingProperty) {
        val targetIds = mutableSetOf<String>()
        matchingProvider?.let { targetIds.add(it.id); targetIds.add(it.phone) }
        matchingStore?.let { targetIds.add(it.id); targetIds.add(it.phone) }
        matchingProperty?.let { targetIds.add(it.id); targetIds.add(it.phone) }

        if (targetIds.isNotEmpty() || matchingProvider != null || matchingStore != null || matchingProperty != null) {
            bookings.filter { b ->
                // Direct bookings
                if (targetIds.contains(b.providerId) || targetIds.contains(b.providerPhone)) {
                    true
                } else if (matchingProvider != null && (b.providerId == "ALL_SERVICES" || b.providerId == "ALL")) {
                    // Technicians and service providers only receive service urgent requests
                    val provArea = matchingProvider.area.trim()
                    val bArea = b.customerArea.trim()
                    val areaMatches = provArea.isEmpty() || bArea.isEmpty() || bArea.contains(provArea, ignoreCase = true) || provArea.contains(bArea, ignoreCase = true)
                    areaMatches
                } else if (matchingStore != null && (b.providerId == "ALL_STORES" || (matchingStore.categoryId.contains("مطعم") && b.providerId == "ALL_RESTAURANTS") || b.providerId == "ALL")) {
                    // Stores & Restaurants only receive store/restaurant urgent requests
                    val storeArea = matchingStore.cityId.trim()
                    val bArea = b.customerArea.trim()
                    val areaMatches = storeArea.isEmpty() || bArea.isEmpty() || bArea.contains(storeArea, ignoreCase = true) || storeArea.contains(bArea, ignoreCase = true)
                    areaMatches
                } else if (matchingProperty != null && (b.providerId == "ALL_PROPERTIES" || b.providerId == "ALL_PROPERTY" || b.providerId == "ALL")) {
                    // Real estate only receives property urgent requests
                    val propArea = matchingProperty.cityId.trim()
                    val bArea = b.customerArea.trim()
                    val areaMatches = propArea.isEmpty() || bArea.isEmpty() || bArea.contains(propArea, ignoreCase = true) || propArea.contains(bArea, ignoreCase = true)
                    areaMatches
                } else {
                    false
                }
            }.sortedByDescending { it.id }
        } else {
            emptyList()
        }
    }

    var selectedTab by remember { mutableStateOf(if (matchingProvider != null || matchingStore != null || matchingProperty != null) 1 else 0) } // 0 = كعميل, 1 = كفني صيانة/مقدم خدمة
    LaunchedEffect(matchingProvider, matchingStore, matchingProperty) {
        if (matchingProvider != null || matchingStore != null || matchingProperty != null) {
            selectedTab = 1
        } else {
            selectedTab = 0
        }
    }

    // State for sub-tabs status categorization: ACTIVE, COMPLETED, CANCELLED
    var filterStatusTab by remember { mutableStateOf("ACTIVE") }
    var showClearAllBookingsDialog by remember { mutableStateOf(false) }
    var customerSubTab by remember { mutableStateOf("URGENT") } // "URGENT" = طلباتي العاجلة, "DIRECT" = حجوزاتي المباشرة, "ALL" = عرض الكل

    // Dialog & overlay states
    var selectedDetailBooking by remember { mutableStateOf<com.example.data.BookingEntity?>(null) }
    var showCancelPasswordDialogForBooking by remember { mutableStateOf<com.example.data.BookingEntity?>(null) }
    var bookingToDeletePermanently by remember { mutableStateOf<com.example.data.BookingEntity?>(null) }
    var enteredCancelPasswordInput by remember { mutableStateOf("") }
    var cancelDialogErrorText by remember { mutableStateOf("") }

    // State for local chat dialog from booking details
    var activeChatChannelForBookingDetail by remember { mutableStateOf<com.example.data.ChatChannelEntity?>(null) }
    var chatReplyInputText by remember { mutableStateOf("") }

    LaunchedEffect(chatChannels, activeChatChannelForBookingDetail?.id) {
        activeChatChannelForBookingDetail?.id?.let { activeId ->
            val latestCh = chatChannels.find { it.id == activeId }
            if (latestCh != null) {
                activeChatChannelForBookingDetail = latestCh
            }
        }
    }

    // State for Editing Booking
    var editingBooking by remember { mutableStateOf<com.example.data.BookingEntity?>(null) }
    var editDate by remember { mutableStateOf("") }
    var editTime by remember { mutableStateOf("") }
    var editServiceType by remember { mutableStateOf("") }

    val settingsState by viewModel.settings.collectAsState()
    val isBlocked = remember(settingsState.blockedUsersForBookings, currentUserPhone) {
        val blockedList = settingsState.blockedUsersForBookings.split(",").map { it.trim() }
        currentUserPhone.isNotEmpty() && blockedList.contains(currentUserPhone)
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(themeColors.secondary)
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.navigateTo("USER_BROWSE") }) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.ArrowBack,
                        contentDescription = "رجوع",
                        tint = themeColors.accent
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "📋 طلباتي وحجوزاتي العاجلة",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(themeColors.background)
                .padding(16.dp)
        ) {
            if (settingsState.bookingsAccessControl == "DISABLED") {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("🚫 نظام معطل", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Red)
                            Text("عذراً، نظام حجز المواعيد معطل حالياً بالكامل من قبل إدارة التطبيق.", color = Color.White, fontSize = 12.sp, textAlign = TextAlign.Center)
                        }
                    }
                }
            } else if (isBlocked) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("🚫 وصول مقيد", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Red)
                            Text("لقد تم تقييد وحظر وصول رقم هاتفك من استخدام نظام الحجوزات من قبل الإدارة.", color = Color.White, fontSize = 12.sp, textAlign = TextAlign.Center)
                        }
                    }
                }
            } else if (settingsState.bookingsAccessControl == "REGISTERED_ONLY" && currentUserPhone.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("🔒 التسجيل مطلوب", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                            Text("عذراً، تصفح وحجز المواعيد متاح فقط للأعضاء والمستخدمين المسجلين في التطبيق ولديها حسابات.", color = Color.White, fontSize = 12.sp, textAlign = TextAlign.Center)
                            Button(
                                onClick = { viewModel.navigateTo("USER_BROWSE") },
                                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent)
                            ) {
                                Text("العودة للرئيسية والتسجيل", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                if (activeSearchPhone.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                            border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth().padding(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Default.DateRange,
                                    contentDescription = null,
                                    tint = themeColors.accent,
                                    modifier = Modifier.size(48.dp).align(Alignment.CenterHorizontally)
                                )
                                Text(
                                    text = "📅 الاستعلام عن الحجوزات والمواعيد",
                                    fontWeight = FontWeight.Bold,
                                    color = themeColors.accent,
                                    fontSize = 14.sp,
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                                Text(
                                    text = "يرجى إدخال رقم هاتفك المحمول الذي قمت باستخدامه في الحجز لعرض ومتابعة المواعيد وتعديلها أو إلغائها فوراً وبدون أي شروط للتعقيد:",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center
                                )
                                
                                OutlinedTextField(
                                    value = phoneInput,
                                    onValueChange = { phoneInput = it },
                                    label = { Text("أدخل رقم هاتف الحجز (9 أرقام)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                )
                                
                                Button(
                                    onClick = {
                                        val cleanPhone = phoneInput.trim()
                                        if (cleanPhone.length >= 6) {
                                            activeSearchPhone = cleanPhone
                                        } else {
                                            android.widget.Toast.makeText(context, "⚠️ يرجى إدخال رقم هاتف صحيح لمتابعة واستعلام الحجز!", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                                    modifier = Modifier.fillMaxWidth().height(46.dp),
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                                ) {
                                    Text("استعلام وعرض المواعيد 🔍", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }

                                Button(
                                    onClick = {
                                        viewModel.triggerRestoreAccountDialog.value = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary.copy(alpha = 0.3f)),
                                    border = BorderStroke(1.dp, themeColors.accent),
                                    modifier = Modifier.fillMaxWidth().height(42.dp),
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                                ) {
                                    Text("🔓 استرجاع الحساب والبيانات برقم الهاتف", color = themeColors.accent, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                } else {
                    // Show search information bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .background(themeColors.surface, shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("🔍 مستعلم حالياً للرقم:", color = Color.LightGray, fontSize = 9.sp)
                            Text(activeSearchPhone, color = themeColors.accent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (myCustomerBookings.isNotEmpty()) {
                                Button(
                                    onClick = { showClearAllBookingsDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f)),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("حذف الكل 🗑️", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Button(
                                onClick = {
                                    activeSearchPhone = ""
                                    phoneInput = ""
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("رقم آخر 🔁", color = Color.White, fontSize = 10.sp)
                            }
                        }
                    }

                    // If technician is logged in, show tabs
                    if (matchingProvider != null || matchingStore != null || matchingProperty != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp)
                                .background(themeColors.surface, shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                                .padding(4.dp)
                        ) {
                            Button(
                                onClick = { selectedTab = 0 },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selectedTab == 0) themeColors.accent else Color.Transparent
                                ),
                                elevation = null
                            ) {
                                Text(
                                    "حجوزاتي كعميل (${myCustomerBookings.size})",
                                    color = if (selectedTab == 0) Color.Black else themeColors.textSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Button(
                                onClick = { selectedTab = 1 },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selectedTab == 1) themeColors.accent else Color.Transparent
                                ),
                                elevation = null
                            ) {
                                Text(
                                    "الحجوزات المستلمة (${receivedBookings.size})",
                                    color = if (selectedTab == 1) Color.Black else themeColors.textSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        selectedTab = 0
                    }

                    // Apply customer-specific tab filter (Urgent vs Direct vs All)
                    val baseList = remember(selectedTab, myCustomerBookings, receivedBookings, customerSubTab) {
                        if (selectedTab == 0) {
                            when (customerSubTab) {
                                "URGENT" -> myCustomerBookings.filter { it.providerId == "ALL" }
                                "DIRECT" -> myCustomerBookings.filter { it.providerId != "ALL" }
                                else -> myCustomerBookings
                            }
                        } else {
                            receivedBookings
                        }
                    }

                    if (selectedTab == 0) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .background(themeColors.surface, shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val customerTabs = listOf(
                                "URGENT" to "⚡ طلباتي العاجلة",
                                "DIRECT" to "📅 حجوزاتي المباشرة",
                                "ALL" to "📋 عرض الكل"
                            )
                            customerTabs.forEach { (tabId, label) ->
                                val isSel = customerSubTab == tabId
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
                                        .background(if (isSel) themeColors.accent else Color.Transparent)
                                        .clickable { customerSubTab = tabId }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSel) Color.Black else Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Status Categorization Sub-Tabs: ACTIVE, COMPLETED, CANCELLED
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val subTabs = listOf(
                            Triple("ACTIVE", "📅 نشطة", Color(0xFFF59E0B)),
                            Triple("COMPLETED", "✅ مكتملة", Color(0xFF10B981)),
                            Triple("CANCELLED", "❌ ملغية", Color(0xFFEF4444))
                        )
                        subTabs.forEach { (tabId, label, tabColor) ->
                            val isSel = filterStatusTab == tabId
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) tabColor.copy(alpha = 0.2f) else Color.Transparent)
                                    .border(1.dp, if (isSel) tabColor else Color.Transparent, RoundedCornerShape(6.dp))
                                    .clickable { filterStatusTab = tabId }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSel) Color.White else Color.Gray,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Apply status filter on the current list
                    val currentList = remember(baseList, filterStatusTab) {
                        when (filterStatusTab) {
                            "ACTIVE" -> baseList.filter { it.status != "COMPLETED" && it.status != "CANCELLED" && it.status != "REJECTED" }
                            "COMPLETED" -> baseList.filter { it.status == "COMPLETED" }
                            else -> baseList.filter { it.status == "CANCELLED" || it.status == "REJECTED" }
                        }
                    }

                    if (currentList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(androidx.compose.material.icons.Icons.Default.DateRange, contentDescription = null, tint = themeColors.textSecondary, modifier = Modifier.size(48.dp))
                                Text(
                                    text = "لا توجد أي حجوزات في هذا التصنيف حالياً.",
                                    color = themeColors.textSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(currentList, key = { it.id }) { booking ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                                    border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.2f)),
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        selectedDetailBooking = booking
                                    }
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = booking.serviceType,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = themeColors.accent
                                            )
                                            
                                            val statusColor = when (booking.status.uppercase()) {
                                                "PENDING" -> Color(0xFFF59E0B)
                                                "APPROVED" -> Color(0xFF10B981)
                                                "STARTED", "IN_PROGRESS" -> Color(0xFF3B82F6)
                                                "COMPLETED" -> Color(0xFF10B981)
                                                "CANCELLED", "REJECTED" -> Color(0xFFEF4444)
                                                else -> Color.Gray
                                            }
                                            val statusText = when (booking.status.uppercase()) {
                                                "PENDING" -> "قيد الانتظار"
                                                "APPROVED" -> "تم القبول"
                                                "STARTED", "IN_PROGRESS" -> "قيد التنفيذ"
                                                "COMPLETED" -> "مكتمل"
                                                "CANCELLED" -> "ملغي"
                                                "REJECTED" -> "مرفوض"
                                                else -> booking.status
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .background(statusColor.copy(alpha = 0.15f), shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(statusText, color = statusColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        Divider(color = themeColors.accent.copy(alpha = 0.1f), thickness = 0.5.dp)

                                        val isAuthorizedForDetail = adminRole != "GUEST" || (matchingProvider != null && matchingProvider.id == booking.providerId) || unlockedBookingIds.contains(booking.id)

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "رقم الحجز: ${booking.bookingNumber.ifEmpty { booking.id }}",
                                                fontSize = 11.sp,
                                                color = Color.White.copy(alpha = 0.8f),
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (isAuthorizedForDetail) {
                                                Text(
                                                    text = "🔑 رمز المرور: ${booking.bookingPassword.ifEmpty { booking.pinCode.ifEmpty { "1234" } }}",
                                                    fontSize = 11.sp,
                                                    color = themeColors.accent
                                                )
                                            } else {
                                                Text(
                                                    text = "🔑 رمز المرور: مخفي 🔒",
                                                    fontSize = 11.sp,
                                                    color = Color.Gray
                                                )
                                            }
                                        }

                                        if (selectedTab == 0) {
                                            Text(
                                                "الجهة الفنية: ${booking.providerName}",
                                                fontSize = 11.sp,
                                                color = themeColors.textPrimary
                                            )
                                        } else {
                                            if (adminRole != "GUEST" || (matchingProvider != null && matchingProvider.id == booking.providerId)) {
                                                Text(
                                                    "العميل المستفيد: ${booking.customerName} | هاتف: ${booking.customerPhone}",
                                                    fontSize = 11.sp,
                                                    color = themeColors.textPrimary
                                                )
                                            } else {
                                                Text(
                                                    "العميل المستفيد: ${booking.customerName.take(3)}*** | هاتف: ${booking.customerPhone.take(4)}****",
                                                    fontSize = 11.sp,
                                                    color = Color.Gray
                                                )
                                            }
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Text("📅 التاريخ: ${booking.dateString}", fontSize = 11.sp, color = themeColors.textSecondary)
                                            Text("⏰ الوقت: ${booking.timeString}", fontSize = 11.sp, color = themeColors.textSecondary)
                                        }

                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                                .padding(6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("اضغط هنا لعرض التفاصيل الكاملة وبدء الإجراءات 🔍", color = themeColors.accent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
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

    // ==========================================
    // 📱 BOOKING DETAIL SCREEN (MODAL DIALOG)
    // ==========================================
    if (selectedDetailBooking != null) {
        val b = selectedDetailBooking!!
        val isAuthorized = adminRole != "GUEST" || 
                           (matchingProvider != null && (matchingProvider.id == b.providerId || matchingProvider.phone == b.providerPhone)) ||
                           (matchingStore != null && (matchingStore.id == b.providerId || matchingStore.phone == b.providerPhone)) ||
                           (matchingProperty != null && (matchingProperty.id == b.providerId || matchingProperty.phone == b.providerPhone)) ||
                           (currentUserPhone.isNotEmpty() && (b.customerPhone == currentUserPhone || b.clientPhone == currentUserPhone)) ||
                           (activeSearchPhone.isNotEmpty() && (b.customerPhone == activeSearchPhone || b.clientPhone == activeSearchPhone)) ||
                           (currentUserId.isNotEmpty() && b.clientId == currentUserId)
        val isUnlocked = isAuthorized || unlockedBookingIds.contains(b.id)

        Dialog(onDismissRequest = { selectedDetailBooking = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = BorderStroke(1.dp, themeColors.accent),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp)
            ) {
                if (!isUnlocked) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🔐 حماية وتأكيد ملكية الحجز", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                            IconButton(onClick = { selectedDetailBooking = null }) {
                                Icon(androidx.compose.material.icons.Icons.Default.Close, contentDescription = "إغلاق", tint = Color.White)
                            }
                        }

                        Divider(color = themeColors.accent.copy(alpha = 0.2f))

                        Text("🚨 هذا الحجز محمي بكلمة مرور سرية لحماية خصوصية بيانات العميل ومنع التعديل أو الإلغاء العشوائي.", color = Color.White, fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        
                        Text("يرجى إدخال كلمة المرور المكونة من 4 أرقام أو معرف الحجز (ID) الخاص بك لفك القفل:", color = Color.LightGray, fontSize = 11.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)

                        var pinInput by remember { mutableStateOf("") }
                        var pinError by remember { mutableStateOf("") }

                        OutlinedTextField(
                            value = pinInput,
                            onValueChange = { pinInput = it },
                            label = { Text("رمز المرور السري أو معرف الحجز", fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        if (pinError.isNotEmpty()) {
                            Text(pinError, color = Color.Red, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                val bPass = b.bookingPassword.ifEmpty { b.pinCode.ifEmpty { "1234" } }
                                val bId = b.bookingNumber.ifEmpty { b.id }
                                if (pinInput.trim() == bPass || pinInput.trim() == bId || pinInput.trim() == b.id) {
                                    unlockedBookingIds = unlockedBookingIds + b.id
                                    pinError = ""
                                    android.widget.Toast.makeText(context, "🔓 تم التحقق بنجاح وفك قفل الحجز!", android.widget.Toast.LENGTH_SHORT).show()
                                } else {
                                    pinError = "❌ رمز المرور أو معرف الحجز غير صحيح! حاول مرة أخرى."
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("تحقق وفك القفل 🔓", color = Color.Black, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text("💡 إذا كنت قد نسيت رمز المرور أو معرف الحجز، يمكنك التواصل الفوري مع الإدارة لاسترجاعه وتأكيد ملكية الحجز.", color = Color.Gray, fontSize = 10.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)

                        Button(
                            onClick = {
                                val supportChId = "support_" + (currentUserId.ifEmpty { b.customerPhone })
                                viewModel.getOrCreateChatChannel(
                                    providerId = "admin",
                                    providerName = "الإدارة والدعم",
                                    customerId = currentUserId.ifEmpty { b.customerPhone },
                                    customerName = currentUserName.ifEmpty { b.customerName }
                                )
                                viewModel.sendMessageInChat("مرحباً بخصوص الحجز رقم ${b.bookingNumber.ifEmpty { b.id }}، لقد نسيت كلمة المرور الخاصة بحجزي وأود استرجاعها وتأكيد ملكيتي له.")
                                activeChatChannelForBookingDetail = com.example.data.ChatChannelEntity(
                                    id = supportChId,
                                    userName = "الإدارة والدعم الفني",
                                    lastMessage = "جاري فتح المحادثة لاسترجاع كلمة المرور...",
                                    messages = emptyList()
                                )
                                selectedDetailBooking = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.secondary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("💬 تواصل مع الإدارة لاسترجاع البيانات فورياً", color = Color.White)
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📋 تفاصيل طلب الحجز الشاملة", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                        IconButton(onClick = { selectedDetailBooking = null }) {
                            Icon(androidx.compose.material.icons.Icons.Default.Close, contentDescription = "إغلاق", tint = Color.White)
                        }
                    }

                    Divider(color = themeColors.accent.copy(alpha = 0.2f))

                    // 🛠️ PROGRESS BAR STATUS TRACKING
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("🚦 شريط تقدم وحالة الحجز ومتابعته:", color = themeColors.accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        
                        val activeStep = when (b.status.uppercase()) {
                            "PENDING" -> 1
                            "APPROVED" -> 2
                            "STARTED", "IN_PROGRESS" -> 3
                            "COMPLETED" -> 4
                            else -> 0 // CANCELLED or REJECTED
                        }

                        if (activeStep == 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Red.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("❌ الحجز ملغي أو مرفوض من قبل العميل أو الإدارة.", color = Color.Red, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            // Render Progress Bar
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val steps = listOf("مرسل", "مقبول", "مباشر", "مكتمل")
                                steps.forEachIndexed { index, stepName ->
                                    val stepNum = index + 1
                                    val isPassed = stepNum <= activeStep
                                    val stepColor = if (isPassed) themeColors.accent else Color.Gray

                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(if (isPassed) themeColors.accent else Color.DarkGray)
                                                .border(1.dp, Color.White, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (stepNum < activeStep) {
                                                Text("✓", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            } else {
                                                Text(stepNum.toString(), color = if (isPassed) Color.Black else Color.White, fontSize = 9.sp)
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(stepName, color = stepColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }

                                    if (index < steps.size - 1) {
                                        val isLinePassed = stepNum < activeStep
                                        Box(
                                            modifier = Modifier
                                                .weight(0.5f)
                                                .height(2.dp)
                                                .background(if (isLinePassed) themeColors.accent else Color.DarkGray)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 📌 BOOKING CREDENTIALS WITH COPY FUNCTIONALITY
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val bNum = b.bookingNumber.ifEmpty { b.id }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("رقم الحجز الفريد:", color = Color.LightGray, fontSize = 9.sp)
                                Text(bNum, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = {
                                    val annotated = androidx.compose.ui.text.AnnotatedString(bNum)
                                    clipboardManager.setText(annotated)
                                    viewModel.triggerNotification("📋 تم نسخ رقم الحجز للذاكرة بنجاح!")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = themeColors.secondary),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(26.dp)
                            ) {
                                Text("نسخ الرقم 📋", color = Color.White, fontSize = 9.sp)
                            }
                        }

                        Divider(color = Color.Gray.copy(alpha = 0.2f), thickness = 0.5.dp)

                        val bPass = b.bookingPassword.ifEmpty { b.pinCode.ifEmpty { "1234" } }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("كلمة المرور السريّة للإلغاء:", color = Color.LightGray, fontSize = 9.sp)
                                Text(bPass, color = themeColors.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = {
                                    val annotated = androidx.compose.ui.text.AnnotatedString(bPass)
                                    clipboardManager.setText(annotated)
                                    viewModel.triggerNotification("🔑 تم نسخ رمز المرور السري!")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = themeColors.secondary),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(26.dp)
                            ) {
                                Text("نسخ الرمز 🔑", color = Color.White, fontSize = 9.sp)
                            }
                        }
                    }

                    // 📝 DETAILS GRID
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("بيانات الحجز:", color = themeColors.accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        
                        Text("• العميل المستفيد: ${b.customerName}", color = Color.White, fontSize = 11.sp)
                        Text("• رقم هاتف العميل: ${b.customerPhone}", color = Color.White, fontSize = 11.sp)
                        Text("• العنوان والسكن: ${b.customerArea}", color = Color.White, fontSize = 11.sp)
                        Text("• الجهة الفنية: ${b.providerName}", color = Color.White, fontSize = 11.sp)
                        Text("• الخدمة المطلوبة: ${b.serviceType}", color = Color.White, fontSize = 11.sp)
                        Text("• التاريخ والموعد: ${b.dateString} الساعة ${b.timeString}", color = Color.White, fontSize = 11.sp)

                        if (b.rejectionReason.isNotEmpty()) {
                            Text("• سبب الإلغاء أو الرفض: ${b.rejectionReason}", color = Color.Red, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (settingsState.isPaymentEnabled) {
                        val associatedPayment = paymentsList.find { it.bookingId == b.id }
                        
                        Divider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))
                        
                        if (associatedPayment == null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("💳 مستحقات الدفع للمنصة", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                                    Text("لم يتم سداد رسوم أو مقدم حجز هذه الخدمة بعد.", fontSize = 10.sp, color = Color.Gray)
                                }
                                
                                Button(
                                    onClick = {
                                        payingBookingObj = b
                                        selectedUserWalletObj = paymentWallets.firstOrNull { it.status == "active" }
                                        userTransferIdInput = ""
                                        userTransferAccountNameInput = ""
                                        userTransferPhotoInput = ""
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Text("سداد بالمنصة 💳", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("حالة الدفع المالي بالمنصة:", fontSize = 11.sp, color = Color.LightGray)
                                    
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                when (associatedPayment.status) {
                                                    "COMPLETED" -> Color(0xFF10B981).copy(alpha = 0.15f)
                                                    "PROCESSING" -> Color(0xFF3B82F6).copy(alpha = 0.15f)
                                                    "FAILED" -> Color.Red.copy(alpha = 0.15f)
                                                    "REFUNDED" -> Color.Magenta.copy(alpha = 0.15f)
                                                    else -> Color.Gray.copy(alpha = 0.15f)
                                                },
                                                RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        val payStatusLabel = when (associatedPayment.status) {
                                            "COMPLETED" -> "تم تأكيد الدفع بنجاح ✅"
                                            "PROCESSING" -> "بانتظار مراجعة الإدارة ⏳"
                                            "FAILED" -> "مرفوض / غير مكتمل ❌"
                                            "REFUNDED" -> "تم استرداد المبلغ 🔄"
                                            else -> "معلق ⏳"
                                        }
                                        Text(
                                            payStatusLabel,
                                            color = when (associatedPayment.status) {
                                                "COMPLETED" -> Color(0xFF10B981)
                                                "PROCESSING" -> Color(0xFF3B82F6)
                                                "FAILED" -> Color.Red
                                                "REFUNDED" -> Color.Magenta
                                                else -> Color.Gray
                                            },
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                
                                if (associatedPayment.status == "FAILED") {
                                    Text("⚠️ سبب الرفض: ${associatedPayment.verificationNote.ifBlank { "رقم الحوالة غير صحيح" }}", fontSize = 10.sp, color = Color.Red)
                                    Button(
                                        onClick = {
                                            payingBookingObj = b
                                            selectedUserWalletObj = paymentWallets.firstOrNull { it.status == "active" }
                                            userTransferIdInput = ""
                                            userTransferAccountNameInput = ""
                                            userTransferPhotoInput = ""
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("🔄 إعادة تقديم إثبات تحويل جديد", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Text("رقم الحوالة: ${associatedPayment.transferId} | مزود الدفع: ${associatedPayment.walletProvider}", fontSize = 10.sp, color = Color.Gray)
                                }
                            }
                        }
                    }

                    Divider(color = Color.Gray.copy(alpha = 0.2f))

                    // ⚡ ACTION BUTTONS: EDIT, CANCEL, CHAT, CALL, DELETE / STATUS
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (selectedTab == 0) {
                            // --- USER ACTIONS ---
                            val canModify = b.status != "CANCELLED" && b.status != "COMPLETED" && b.status != "REJECTED"

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                // ✏️ Edit Button
                                Button(
                                    onClick = {
                                        editingBooking = b
                                        editDate = b.dateString
                                        editTime = b.timeString
                                        editServiceType = b.serviceType
                                    },
                                    enabled = canModify,
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.secondary)
                                ) {
                                    Text("تعديل الحجز ✏️", fontSize = 10.sp, color = if (canModify) Color.White else Color.Gray, fontWeight = FontWeight.Bold)
                                }

                                // ❌ Cancel Button (Subject to 6-hour rule)
                                if (canModify) {
                                    Button(
                                        onClick = {
                                            showCancelPasswordDialogForBooking = b
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                                    ) {
                                        Text("إلغاء الحجز ❌", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // 🗑️ Permanent Delete Button (Shown after Cancellation/Rejection/Completion)
                            if (b.status == "CANCELLED" || b.status == "REJECTED" || b.status == "COMPLETED") {
                                Button(
                                    onClick = {
                                        bookingToDeletePermanently = b
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                                ) {
                                    Text("🗑️ حذف الحجز نهائياً من السجلات", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            // --- TECHNICIAN STATUS ACTIONS ---
                            Text("⚡ تحديث حالة الطلب الفني:", color = themeColors.accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (b.status == "PENDING") {
                                    Button(
                                        onClick = {
                                            viewModel.updateBookingStatus(b.id, "APPROVED")
                                            selectedDetailBooking = b.copy(status = "APPROVED")
                                            viewModel.triggerNotification("✅ تم قبول طلب الحجز بنجاح")
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                                    ) {
                                        Text("قبول ✅", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = {
                                            viewModel.updateBookingStatus(b.id, "REJECTED", "اعتذار الفني عن تنفيذ الطلب")
                                            selectedDetailBooking = b.copy(status = "REJECTED", rejectionReason = "اعتذار الفني")
                                            viewModel.triggerNotification("❌ تم الاعتذار عن الطلب")
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                                    ) {
                                        Text("اعتذار ❌", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                } else if (b.status == "APPROVED") {
                                    Button(
                                        onClick = {
                                            viewModel.updateBookingStatus(b.id, "IN_PROGRESS")
                                            selectedDetailBooking = b.copy(status = "IN_PROGRESS")
                                            viewModel.triggerNotification("🚀 تم بدء تنفيذ الخدمة")
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                                    ) {
                                        Text("🚀 بدء تنفيذ العمل والخدمة", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                } else if (b.status == "IN_PROGRESS" || b.status == "STARTED") {
                                    Button(
                                        onClick = {
                                            viewModel.updateBookingStatus(b.id, "COMPLETED")
                                            selectedDetailBooking = b.copy(status = "COMPLETED")
                                            viewModel.triggerNotification("🎉 تم إنجاز واكتمال الخدمة بنجاح")
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                                    ) {
                                        Text("🎉 تم إنجاز الخدمة واكتمالها بنجاح", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // 💬 Chat Button
                            Button(
                                onClick = {
                                    val chanId = "chat_p_${b.providerId}_u_${b.customerPhone}"
                                    viewModel.getOrCreateChatChannel(b.providerId, b.providerName, b.customerPhone, b.customerName)
                                    activeChatChannelForBookingDetail = com.example.data.ChatChannelEntity(
                                        id = chanId,
                                        userName = b.providerName,
                                        lastMessage = "مرحباً بخصوص الحجز...",
                                        messages = emptyList()
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent)
                            ) {
                                Text("محادثة 💬", fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                            }

                            // 📞 Call Button
                            Button(
                                onClick = {
                                    val pNum = if (selectedTab == 0) b.providerPhone.ifEmpty { b.customerPhone } else b.customerPhone
                                    try {
                                        val dialIntent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                                            data = android.net.Uri.parse("tel:$pNum")
                                        }
                                        context.startActivity(dialIntent)
                                    } catch (e: Exception) {
                                        viewModel.triggerNotification("❌ لا يمكن بدء الاتصال التلفوني")
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                            ) {
                                Text("اتصال 📞", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }

                            // 🎙️ Voice Call Button
                            Button(
                                onClick = {
                                    val pNum = if (selectedTab == 0) b.providerPhone.ifEmpty { b.customerPhone } else b.customerPhone
                                    viewModel.triggerNotification("🎙️ جاري الاتصال الصوتي...")
                                    try {
                                        val dialIntent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                                            data = android.net.Uri.parse("tel:$pNum")
                                        }
                                        context.startActivity(dialIntent)
                                    } catch (e: Exception) {}
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                            ) {
                                Text("صوتي 🎙️", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                }
            }
        }
    }

    // ==========================================
    // 🔒 BOOKING PASSWORD CANCELLATION DIALOG
    // ==========================================
    if (showCancelPasswordDialogForBooking != null) {
        val booking = showCancelPasswordDialogForBooking!!
        val remainingAttempts = 3 - booking.cancellationAttempts
        AlertDialog(
            onDismissRequest = { 
                showCancelPasswordDialogForBooking = null 
                enteredCancelPasswordInput = ""
                cancelDialogErrorText = ""
            },
            title = {
                Text("❌ حوار تأكيد إلغاء الحجز", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
            },
            containerColor = themeColors.secondary,
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Text("رقم الحجز: ${booking.bookingNumber.ifEmpty { booking.id }}", color = Color.Yellow, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("الخدمة: ${booking.serviceType}", color = Color.White, fontSize = 11.sp)
                    Text("الموعد: ${booking.dateString} - ${booking.timeString}", color = Color.LightGray, fontSize = 11.sp)
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text("يرجى إدخال كلمة مرور الحجز (4 أرقام) أو رقم الحجز لتأكيد الإلغاء وحماية الحجز:", color = Color.White, fontSize = 11.sp)
                    
                    OutlinedTextField(
                        value = enteredCancelPasswordInput,
                        onValueChange = { enteredCancelPasswordInput = it },
                        label = { Text("رمز المرور أو رقم الحجز") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = themeColors.accent
                        )
                    )
                    
                    if (cancelDialogErrorText.isNotEmpty()) {
                        Text(cancelDialogErrorText, color = Color.Red, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    
                    Text("عدد المحاولات المتبقية قبل القفل: $remainingAttempts / 3", color = if (remainingAttempts <= 1) Color.Red else Color.LightGray, fontSize = 10.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (enteredCancelPasswordInput.trim().isEmpty()) {
                            cancelDialogErrorText = "⚠️ يرجى إدخال كلمة المرور أو رقم الحجز!"
                            return@Button
                        }
                        viewModel.attemptCancelBooking(booking.id, enteredCancelPasswordInput.trim()) { success, message ->
                            if (success) {
                                showCancelPasswordDialogForBooking = null
                                selectedDetailBooking = null // Close detail screen too
                                enteredCancelPasswordInput = ""
                                cancelDialogErrorText = ""
                                android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
                            } else {
                                cancelDialogErrorText = message
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("تأكيد الإلغاء ❌", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Button(
                    onClick = { 
                        showCancelPasswordDialogForBooking = null 
                        enteredCancelPasswordInput = ""
                        cancelDialogErrorText = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                ) {
                    Text("تراجع", color = Color.White, fontSize = 11.sp)
                }
            }
        )
    }

    // ==========================================
    // 💬 CHAT DIALOG FOR BOOKINGS
    // ==========================================
    if (activeChatChannelForBookingDetail != null) {
        val ch = activeChatChannelForBookingDetail!!
        Dialog(onDismissRequest = { activeChatChannelForBookingDetail = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = BorderStroke(1.dp, themeColors.accent),
                modifier = Modifier.padding(12.dp).fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("💬 محادثة فورية: ${ch.userName}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(ch.messages) { msg ->
                                val isMe = msg.senderId == currentUserPhone
                                val alignment = if (isMe) Alignment.End else Alignment.Start
                                val bubbleBg = if (isMe) themeColors.primary else Color.Gray.copy(alpha = 0.3f)
                                Column(horizontalAlignment = alignment, modifier = Modifier.fillMaxWidth()) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(bubbleBg)
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(msg.message, fontSize = 10.sp, color = Color.White)
                                    }
                                    Text(msg.senderName, fontSize = 8.sp, color = themeColors.textSecondary)
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = chatReplyInputText,
                        onValueChange = { chatReplyInputText = it },
                        placeholder = { Text("اكتب ردك السريع هنا...") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                if (chatReplyInputText.trim().isNotEmpty()) {
                                    val senderName = if (selectedTab == 0) "العميل" else ch.userName
                                    viewModel.replyToChatChannel(ch.id, currentUserPhone.ifEmpty { "user" }, chatReplyInputText.trim(), senderName)
                                    
                                    val newMsg = com.example.data.ChatMessageEntity(
                                        id = java.util.UUID.randomUUID().toString(),
                                        senderId = currentUserPhone.ifEmpty { "user" },
                                        message = chatReplyInputText.trim(),
                                        timestamp = System.currentTimeMillis(),
                                        senderName = senderName
                                    )
                                    activeChatChannelForBookingDetail = ch.copy(
                                        messages = ch.messages + newMsg,
                                        lastMessage = chatReplyInputText.trim(),
                                        timestamp = System.currentTimeMillis()
                                    )
                                    chatReplyInputText = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("إرسال رد ⚡", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // 🗑️ PERMANENT DELETE BOOKING CONFIRMATION DIALOG
    if (bookingToDeletePermanently != null) {
        val bToDelete = bookingToDeletePermanently!!
        AlertDialog(
            onDismissRequest = { bookingToDeletePermanently = null },
            containerColor = Color(0xFF1E293B),
            title = { Text("🗑️ تأكيد الحذف النهائي للحجز", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "هل أنت متأكد من حذف هذا الحجز نهائياً من السجلات وقاعدة البيانات؟",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "• رقم الحجز: ${bToDelete.id}\n• العميل: ${bToDelete.customerName}\n• الفني: ${bToDelete.providerName}\n\n⚠️ ملاحظة: لا يمكن التراجع عن هذه العملية بعد إتمام الحذف وسيتم إشعار الطرفين بتمام الإغلاق.",
                        color = Color.LightGray,
                        fontSize = 11.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteBooking(bToDelete.id)
                        viewModel.addNotification(
                            title = "🗑️ إغلاق وحذف الحجز",
                            message = "تم حذف الحجز الملغي (${bToDelete.id}) نهائياً من قبل المستخدم/الفني.",
                            targetType = "USER",
                            targetValue = bToDelete.customerPhone
                        )
                        viewModel.addNotification(
                            title = "🗑️ إغلاق وحذف الحجز",
                            message = "تم حذف الحجز الملغي (${bToDelete.id}) نهائياً.",
                            targetType = "PROVIDER",
                            targetValue = bToDelete.providerId
                        )
                        viewModel.triggerNotification("🗑️ تم حذف الحجز نهائياً من السجلات وقاعدة البيانات")
                        bookingToDeletePermanently = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("نعم، حذف نهائياً 🗑️", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            },
            dismissButton = {
                Button(
                    onClick = { bookingToDeletePermanently = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                ) {
                    Text("إلغاء ❌", color = Color.White, fontSize = 11.sp)
                }
            }
        )
    }

    // ==========================================
    // 📝 EDIT BOOKING DATE/TIME DIALOG (FOR USER)
    // ==========================================
    if (editingBooking != null) {
        val b = editingBooking!!
        AlertDialog(
            onDismissRequest = { editingBooking = null },
            title = { Text("📝 تعديل موعد الحجز", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White) },
            containerColor = themeColors.secondary,
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Text("قم بتعديل تفاصيل حجز الخدمة:", color = themeColors.textSecondary, fontSize = 11.sp)
                    
                    OutlinedTextField(
                        value = editServiceType,
                        onValueChange = { editServiceType = it },
                        label = { Text("نوع الخدمة المطلوبة", fontSize = 10.sp) },
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = Color.White),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = editDate,
                        onValueChange = { editDate = it },
                        label = { Text("تاريخ الموعد (مثال: 2026-07-25)", fontSize = 10.sp) },
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = Color.White),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = editTime,
                        onValueChange = { editTime = it },
                        label = { Text("وقت الموعد (مثال: 16:30)", fontSize = 10.sp) },
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = Color.White),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.editBookingByUser(b.id, editDate, editTime, editServiceType)
                        editingBooking = null
                        selectedDetailBooking = null // Refresh detail view
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent)
                ) {
                    Text("حفظ التغييرات", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            },
            dismissButton = {
                Button(
                    onClick = { editingBooking = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                ) {
                    Text("إلغاء", color = Color.White, fontSize = 11.sp)
                }
            }
        )
    }

    if (payingBookingObj != null) {
        UserSubmitPaymentProofDialog(
            booking = payingBookingObj!!,
            viewModel = viewModel,
            paymentWallets = paymentWallets,
            settingsState = settingsState,
            themeColors = themeColors,
            onDismiss = { payingBookingObj = null }
        )
    }

    if (showClearAllBookingsDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllBookingsDialog = false },
            containerColor = themeColors.surface,
            title = { Text("🗑️ تأكيد مسح جميع الحجوزات", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp) },
            text = {
                Text(
                    "هل أنت متأكد تماماً من حذف سجل وتصفية جميع الحجوزات والمواعيد المسجلة برقم هاتفك ($activeSearchPhone) نهائياً من قاعدة البيانات؟ لا يمكن التراجع عن هذه الخطوة.",
                    color = Color.LightGray,
                    fontSize = 12.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAllBookings(activeSearchPhone)
                        showClearAllBookingsDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("نعم، احذف الكل 🗑️", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            },
            dismissButton = {
                Button(
                    onClick = { showClearAllBookingsDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                ) {
                    Text("تراجع", color = Color.White, fontSize = 11.sp)
                }
            }
        )
    }
}
