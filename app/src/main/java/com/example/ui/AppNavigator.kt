package com.example.ui

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
import com.example.utils.*
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
import com.example.ui.screens.chat.*
import com.example.ui.screens.dashboard.*
import com.example.ui.screens.notifications.*
import com.example.viewmodels.*
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ------ App Main Navigator ------
@Composable
fun AppNavigator(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    permissionLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>,
    locationPermissions: Array<String>
) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val currentLang by viewModel.currentLanguage.collectAsState()
    val toastMessage by viewModel.toastFlow.collectAsState()
    val settingsState by viewModel.settings.collectAsState()
    val adminRole by viewModel.adminRole.collectAsState()
    val showBackdoorDialogState by viewModel.showBackdoorDialog.collectAsState()
    val context = LocalContext.current

    val currentUserIdState by viewModel.currentUserId.collectAsState()
    var showGuestRegisterDialogForAction by remember { mutableStateOf<String?>(null) } // null, "CHAT"
    var activeSectionIdForCreation by remember { mutableStateOf("") }
    var preselectedRegistrationType by remember { mutableStateOf("TECHNICIAN") }

    LaunchedEffect(Unit) {
        viewModel.initializeUserIdentity(context)
    }

    LaunchedEffect(currentUserIdState) {
        if (currentUserIdState.isNotEmpty() && currentUserIdState != "guest") {
            try {
                com.google.firebase.messaging.FirebaseMessaging.getInstance().token
                    .addOnCompleteListener { task ->
                        try {
                            if (task.isSuccessful && task.result != null) {
                                val token = task.result
                                if (!token.isNullOrEmpty()) {
                                    viewModel.updateUserFcmToken(currentUserIdState, token)
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.w("FCM", "FCM token processing skipped: ${e.message}")
                        }
                    }
                    .addOnFailureListener { e ->
                        android.util.Log.w("FCM", "FCM token retrieval failed: ${e.message}")
                    }
            } catch (e: Exception) {
                android.util.Log.w("FCM", "FCM initialization error: ${e.message}")
            }
        }
    }

    // Modal dialog trigger states
    var showInfoDialog by remember { mutableStateOf(false) }
    var showAssistantDialog by remember { mutableStateOf(false) }
    var showRequestServiceModal by remember { mutableStateOf(false) }
    var showChatDialog by remember { mutableStateOf(false) }
    var showNotificationsDialog by remember { mutableStateOf(false) }
    var showAllConversationsDialog by remember { mutableStateOf(false) }
    var preSelectedChannelId by remember { mutableStateOf<String?>(null) }
    var chatReadTrigger by remember { mutableStateOf(0) }

    val providers by viewModel.providers.collectAsState()
    val pendingProviders by viewModel.pendingProviders.collectAsState()
    val stores by viewModel.stores.collectAsState()
    val properties by viewModel.properties.collectAsState()

    var showRestoreAccountDialog by remember { mutableStateOf(false) }
    var restorePhoneInput by remember { mutableStateOf("") }
    var restoreStep by remember { mutableStateOf(1) }
    var matchedProvider by remember { mutableStateOf<com.example.data.ProviderEntity?>(null) }
    var matchedPending by remember { mutableStateOf<com.example.data.PendingProviderEntity?>(null) }
    var matchedStore by remember { mutableStateOf<com.example.data.StoreEntity?>(null) }
    var matchedProperty by remember { mutableStateOf<com.example.data.PropertyEntity?>(null) }
    var matchedUserDoc by remember { mutableStateOf<Map<String, Any>?>(null) }
    var isSearchingAccount by remember { mutableStateOf(false) }
    var restorePasswordInput by remember { mutableStateOf("") }

    val triggerRestore by viewModel.triggerRestoreAccountDialog.collectAsState()
    if (triggerRestore) {
        showRestoreAccountDialog = true
        viewModel.triggerRestoreAccountDialog.value = false
    }

    val activeChatChannel by viewModel.activeChatChannel.collectAsState()
    LaunchedEffect(activeChatChannel) {
        activeChatChannel?.let { channel ->
            preSelectedChannelId = channel.id
            showAllConversationsDialog = true
        }
    }

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearNotification()
        }
    }

    CompositionLocalProvider(
        androidx.compose.ui.platform.LocalLayoutDirection provides (if (currentLang == "en") androidx.compose.ui.unit.LayoutDirection.Ltr else androidx.compose.ui.unit.LayoutDirection.Rtl)
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                AppHeaderBar(
                    viewModel = viewModel,
                    themeColors = themeColors,
                    onNotificationsClick = { showNotificationsDialog = true },
                    onChatsClick = { showAllConversationsDialog = true },
                    chatReadTrigger = chatReadTrigger,
                    onMenuClick = {}
                )
            },
            bottomBar = {
                AppFooterBar(
                    viewModel = viewModel,
                    themeColors = themeColors,
                    onInfoClick = { showInfoDialog = true }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(themeColors.background)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    val isOnline by viewModel.isOnline.collectAsState()
                    val uiErrorMessage by viewModel.uiErrorMessage.collectAsState()
                    val isRefreshing by viewModel.isRefreshing.collectAsState()

                    if (!isOnline) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(androidx.compose.ui.graphics.Color(0xFFE57373))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "⚠️ عذراً، لا يوجد اتصال بالإنترنت. يتم عرض البيانات المخزنة مؤقتاً.",
                                color = androidx.compose.ui.graphics.Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = {
                                    viewModel.retryConnection(context)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color.White),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier = Modifier.height(26.dp)
                            ) {
                                Text("إعادة المحاولة", color = androidx.compose.ui.graphics.Color(0xFFC62828), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (uiErrorMessage != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(androidx.compose.ui.graphics.Color(0xFFD32F2F))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = androidx.compose.ui.graphics.Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = uiErrorMessage ?: "",
                                    color = androidx.compose.ui.graphics.Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Row {
                                TextButton(
                                    onClick = { viewModel.refreshData() },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                ) {
                                    Text("تحديث 🔄", color = androidx.compose.ui.graphics.Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                IconButton(
                                    onClick = { viewModel.clearUiError() },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = androidx.compose.material.icons.Icons.Default.Close,
                                        contentDescription = "إغلاق",
                                        tint = androidx.compose.ui.graphics.Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (isRefreshing) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().height(3.dp),
                            color = themeColors.accent,
                            trackColor = themeColors.accent.copy(alpha = 0.2f)
                        )
                    }

                    Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                        if (settingsState.isMaintenanceActive && adminRole == "GUEST") {
                            MaintenanceSplashView(settingsState = settingsState, themeColors = themeColors, viewModel = viewModel)
                        } else {
                            when (currentScreen) {
                                "OWNER_PANEL" -> OwnerBackdoorPanelLayout(viewModel = viewModel, themeColors = themeColors)
                                "ADMIN_PANEL" -> AdminPanelLayout(viewModel = viewModel, themeColors = themeColors)
                                "REGISTER_FORM" -> ProviderRegisterFormLayout(
                                    viewModel = viewModel,
                                    themeColors = themeColors,
                                    regType = preselectedRegistrationType,
                                    sectionId = activeSectionIdForCreation,
                                    onRegTypeChange = { preselectedRegistrationType = it }
                                )
                                "JOIN_REQUEST_STATUS" -> JoinRequestStatusScreen(viewModel = viewModel, themeColors = themeColors)
                                "ABOUT_APP" -> AboutAppScreenContent(viewModel = viewModel, themeColors = themeColors)
                                "BOOKINGS_VIEW" -> BookingsScreenLayout(viewModel = viewModel, themeColors = themeColors)
                                "ORDERS_VIEW" -> OrdersScreenLayout(viewModel = viewModel, themeColors = themeColors)
                                "MAP_VIEW" -> com.example.ui.MapScreen(
                                    viewModel = viewModel,
                                    onBackClick = { viewModel.navigateTo("HOME") },
                                    onOpenProviderDetails = { provider ->
                                        viewModel.selectedProvider = provider
                                        viewModel.navigateTo("PROVIDER_DETAILS")
                                    },
                                    onOpenStoreDetails = { store ->
                                        viewModel.selectedStore = store
                                        viewModel.navigateTo("STORE_DETAILS")
                                    },
                                    onOpenPropertyDetails = { property ->
                                        viewModel.selectedProperty = property
                                        viewModel.navigateTo("PROPERTY_DETAILS")
                                    },
                                    onRequestBooking = { provider ->
                                        viewModel.selectedProvider = provider
                                        viewModel.navigateTo("CREATE_BOOKING")
                                    }
                                )
                                else -> ServicesBrowserLayout(
                                    viewModel = viewModel,
                                    themeColors = themeColors,
                                    activeSectionIdForCreation = activeSectionIdForCreation,
                                    onActiveSectionIdForCreationChange = { activeSectionIdForCreation = it },
                                    preselectedRegistrationType = preselectedRegistrationType,
                                    onPreselectedRegistrationTypeChange = { preselectedRegistrationType = it },
                                    onChatOpen = { channelId ->
                                        preSelectedChannelId = channelId
                                        showAllConversationsDialog = true
                                    }
                                )
                            }

                            val isRegistrationOrFormOpen = currentScreen in setOf(
                                "REGISTER_FORM", "JOIN_REQUEST_STATUS", "LOGIN", 
                                "PROVIDER_REGISTRATION", "STORE_CREATION", "PROPERTY_CREATION", 
                                "JOB_CREATION", "CREATE_BOOKING", "REGISTER",
                                "MAP_VIEW", "ADMIN_PANEL", "ADMIN_LOGIN", "OWNER_PANEL"
                            ) || showGuestRegisterDialogForAction != null || 
                              showAssistantDialog || showRequestServiceModal

                            if (!isRegistrationOrFormOpen) {
                                FloatingIconsOverlay(
                                    settings = settingsState,
                                    themeColors = themeColors,
                                    onAssistantClick = { showAssistantDialog = true },
                                    onRequestServiceClick = { showRequestServiceModal = true }
                                )
                            }
                        }
                    }
                }
            }
        }

    if (showGuestRegisterDialogForAction == "CHAT") {
        GuestRegistrationDialog(
            viewModel = viewModel,
            themeColors = themeColors,
            onDismiss = { showGuestRegisterDialogForAction = null },
            onRegisterCompleted = { name, phone, residence, password ->
                viewModel.registerGuestUser(context, name, phone, residence, password)
                showGuestRegisterDialogForAction = null
                showChatDialog = true
            }
        )
    }

    if (showInfoDialog) {
        AboutAppDialogView(viewModel = viewModel, themeColors = themeColors, onDismiss = { showInfoDialog = false })
    }

    if (showRequestServiceModal) {
        QuickServiceRequestDialog(
            viewModel = viewModel,
            themeColors = themeColors,
            onDismiss = { showRequestServiceModal = false },
            onRequestCreated = {
                showRequestServiceModal = false
                viewModel.navigateTo("BOOKINGS_VIEW")
            }
        )
    }

    if (showAssistantDialog) {
        SmartAssistantDialogView(
            viewModel = viewModel,
            settings = settingsState,
            themeColors = themeColors,
            onDismiss = { showAssistantDialog = false },
            onChatOpen = { channelId ->
                preSelectedChannelId = channelId
                showAllConversationsDialog = true
                showAssistantDialog = false
            }
        )
    }

    if (showChatDialog) {
        ChatPanelDialogView(viewModel = viewModel, themeColors = themeColors, onDismiss = { showChatDialog = false })
    }

    if (showNotificationsDialog) {
        UserNotificationsDialogView(viewModel = viewModel, themeColors = themeColors, onDismiss = { showNotificationsDialog = false })
    }

    if (showAllConversationsDialog) {
        AllConversationsDialogView(
            viewModel = viewModel,
            themeColors = themeColors,
            initialSelectedChannelId = preSelectedChannelId,
            onReadTrigger = { chatReadTrigger++ },
            onDismiss = { 
                showAllConversationsDialog = false
                preSelectedChannelId = null
                viewModel.closeActiveChatChannel()
            }
        )
    }

    if (showRestoreAccountDialog) {
        val bookings by viewModel.bookings.collectAsState()
        val chatChannels by viewModel.chatChannels.collectAsState()

        Dialog(onDismissRequest = { 
            showRestoreAccountDialog = false 
            restoreStep = 1
            restorePhoneInput = ""
            restorePasswordInput = ""
            matchedProvider = null
            matchedPending = null
            matchedStore = null
            matchedProperty = null
            matchedUserDoc = null
            isSearchingAccount = false
        }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, themeColors.accent),
                modifier = Modifier.padding(12.dp).fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("🔄 استرجاع الحساب والبيانات من أي قسم", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    
                    if (restoreStep == 1) {
                        Text("يرجى إدخال رقم الهاتف المسجل به سابقاً (عميل، فني، صاحب متجر/مطعم/مركز، أو عقار) للمتابعة واسترجاع الحساب فوراً:", color = Color.LightGray, fontSize = 11.sp)
                        
                        OutlinedTextField(
                            value = restorePhoneInput,
                            onValueChange = { restorePhoneInput = it },
                            placeholder = { Text("مثال: 771234567") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    val raw = restorePhoneInput.trim().replace(" ", "")
                                    val cleanPhone = when {
                                        raw.startsWith("+967") -> raw.substring(4)
                                        raw.startsWith("00967") -> raw.substring(5)
                                        raw.startsWith("0") -> raw.substring(1)
                                        else -> raw
                                    }

                                    fun normalizePhone(ph: String): String {
                                        return ph.trim()
                                            .replace(" ", "")
                                            .replace("+", "")
                                            .removePrefix("967")
                                            .removePrefix("00967")
                                            .removePrefix("0")
                                    }
                                    val normInput = normalizePhone(restorePhoneInput)

                                    if (cleanPhone.length >= 6) {
                                        val matchingPending = pendingProviders.find { 
                                            normalizePhone(it.phone) == normInput
                                        }
                                        val matchingApproved = providers.find { 
                                            normalizePhone(it.phone) == normInput
                                        }
                                        val matchingStore = stores.find { 
                                            normalizePhone(it.phone) == normInput || normalizePhone(it.ownerId) == normInput
                                        }
                                        val matchingProperty = properties.find { 
                                            normalizePhone(it.phone) == normInput || normalizePhone(it.ownerId) == normInput
                                        }
                                        
                                        val matchingBooking = bookings.find { b ->
                                            normalizePhone(b.clientPhone) == normInput
                                        }
                                        val matchingChatChannel = chatChannels.find { c ->
                                            normalizePhone(c.customerPhone) == normInput
                                        }

                                        if (matchingPending != null || matchingApproved != null || matchingStore != null || matchingProperty != null) {
                                            matchedProvider = matchingApproved
                                            matchedPending = matchingPending
                                            matchedStore = matchingStore
                                            matchedProperty = matchingProperty
                                            restoreStep = 2
                                        } else if (matchingBooking != null || matchingChatChannel != null) {
                                            matchedUserDoc = mapOf(
                                                "name" to (matchingBooking?.clientName.orEmpty().ifEmpty { matchingChatChannel?.customerName.orEmpty().ifEmpty { "عميل" } }),
                                                "phone" to cleanPhone,
                                                "password" to (matchingBooking?.bookingPassword.orEmpty().ifEmpty { matchingBooking?.pinCode.orEmpty() }),
                                                "residence" to "اليمن"
                                            )
                                            restoreStep = 2
                                        } else {
                                            // Search Firestore registered_users
                                            isSearchingAccount = true
                                            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                                .collection("registered_users")
                                                .whereEqualTo("phone", cleanPhone)
                                                .get()
                                                .addOnSuccessListener { qs ->
                                                    isSearchingAccount = false
                                                    if (qs != null && !qs.isEmpty) {
                                                        matchedUserDoc = qs.documents.first().data
                                                        restoreStep = 2
                                                    } else {
                                                        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                                            .collection("registered_users")
                                                            .whereEqualTo("phone", raw)
                                                            .get()
                                                            .addOnSuccessListener { qs2 ->
                                                                if (qs2 != null && !qs2.isEmpty) {
                                                                    matchedUserDoc = qs2.documents.first().data
                                                                    restoreStep = 2
                                                                } else {
                                                                    android.widget.Toast.makeText(context, "❌ لا يوجد حساب مسجل بهذا الرقم! يمكنك إنشاء حساب جديد مجاناً.", android.widget.Toast.LENGTH_LONG).show()
                                                                }
                                                            }.addOnFailureListener {
                                                                android.widget.Toast.makeText(context, "❌ لا يوجد حساب مسجل بهذا الرقم!", android.widget.Toast.LENGTH_LONG).show()
                                                            }
                                                    }
                                                }.addOnFailureListener {
                                                    isSearchingAccount = false
                                                    android.widget.Toast.makeText(context, "❌ خطأ في الاتصال بالشبكة!", android.widget.Toast.LENGTH_SHORT).show()
                                                }
                                        }
                                    } else {
                                        android.widget.Toast.makeText(context, "❌ يرجى إدخال رقم هاتف صحيح مكون من 9 أرقام!", android.widget.Toast.LENGTH_LONG).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                                modifier = Modifier.weight(1f),
                                enabled = !isSearchingAccount
                            ) {
                                Text(if (isSearchingAccount) "جاري البحث..." else "التالي ➡️", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                            Button(
                                onClick = { 
                                    showRestoreAccountDialog = false 
                                    restoreStep = 1
                                    restorePhoneInput = ""
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("إلغاء", color = Color.White, fontSize = 11.sp)
                            }
                        }
                    } else {
                        // Step 2: Password
                        val accountName = matchedProvider?.name ?: matchedPending?.name ?: matchedStore?.name ?: matchedProperty?.title ?: matchedUserDoc?.get("name")?.toString() ?: "مستخدم"
                        val accountType = when {
                            matchedProvider != null -> "فني معتمد"
                            matchedPending != null -> "طلب فني معلق"
                            matchedStore != null -> "محل / مركز / مطعم"
                            matchedProperty != null -> "عقار / بيت"
                            matchedUserDoc != null -> "حساب عميل (حجوزات ومحادثات)"
                            else -> "حساب مسجل"
                        }
                        Text("👤 تم العثور على حساب ($accountType) لـ: $accountName", color = themeColors.accent, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Text("الرجاء إدخال كلمة المرور الخاصة بحسابك للتحقق المباشر واسترجاع بياناتك:", color = Color.LightGray, fontSize = 10.sp)
                        
                        var passVisible by remember { mutableStateOf(false) }
                        OutlinedTextField(
                            value = restorePasswordInput,
                            onValueChange = { restorePasswordInput = it },
                            placeholder = { Text("أدخل كلمة المرور") },
                            visualTransformation = if (passVisible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                            trailingIcon = {
                                IconButton(onClick = { passVisible = !passVisible }) {
                                    Text(if (passVisible) "👁️" else "🙈", fontSize = 16.sp)
                                }
                            }
                        )
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    val raw = restorePhoneInput.trim().replace(" ", "")
                                    val cleanPhone = when {
                                        raw.startsWith("+967") -> raw.substring(4)
                                        raw.startsWith("00967") -> raw.substring(5)
                                        raw.startsWith("0") -> raw.substring(1)
                                        else -> raw
                                    }

                                    val savedPass = matchedProvider?.password 
                                        ?: matchedPending?.password 
                                        ?: matchedStore?.password 
                                        ?: matchedProperty?.password 
                                        ?: matchedUserDoc?.get("password")?.toString() 
                                        ?: ""

                                    val isPassValid = com.example.util.PasswordHasher.verifyPassword(restorePasswordInput, savedPass) || 
                                                      com.example.util.SecurityCryptoUtils.verifyAdminPassword(restorePasswordInput, savedPass) ||
                                                      viewModel.verifyAdminOrOwnerPassword(restorePasswordInput)

                                    if (isPassValid) {
                                        val rName = accountName
                                        viewModel.setUserSessionDetails(context, rName, cleanPhone, "اليمن")

                                        if (matchedProvider != null) {
                                            if (matchedProvider!!.isDeleted) {
                                                viewModel.restoreProvider(matchedProvider!!.id)
                                            }
                                            viewModel.setJoinRequestPhone(context, cleanPhone)
                                            viewModel.navigateTo("REGISTER_FORM")
                                        } else if (matchedPending != null) {
                                            viewModel.setJoinRequestPhone(context, cleanPhone)
                                            viewModel.navigateTo("JOIN_REQUEST_STATUS")
                                        } else if (matchedStore != null) {
                                            if (matchedStore!!.isDeleted) {
                                                viewModel.restoreStore(matchedStore!!.id)
                                            }
                                            viewModel.setJoinRequestPhone(context, cleanPhone)
                                            viewModel.navigateTo("REGISTER_FORM")
                                        } else if (matchedProperty != null) {
                                            if (matchedProperty!!.isDeleted) {
                                                viewModel.restoreProperty(matchedProperty!!.id)
                                            }
                                            viewModel.setJoinRequestPhone(context, cleanPhone)
                                            viewModel.navigateTo("REGISTER_FORM")
                                        } else {
                                            viewModel.navigateTo("REGISTER_FORM")
                                        }

                                        android.widget.Toast.makeText(context, "🔓 تم استعادة حسابك وبيناتك وحجوزاتك بنجاح! مرحباً بك $rName", android.widget.Toast.LENGTH_LONG).show()
                                        showRestoreAccountDialog = false
                                        restoreStep = 1
                                        restorePhoneInput = ""
                                        restorePasswordInput = ""
                                        matchedProvider = null
                                        matchedPending = null
                                        matchedStore = null
                                        matchedProperty = null
                                        matchedUserDoc = null
                                    } else {
                                        android.widget.Toast.makeText(context, "❌ كلمة المرور غير صحيحة!", android.widget.Toast.LENGTH_LONG).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("تأكيد واسترجاع 🔓", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                            Button(
                                onClick = {
                                    restoreStep = 1
                                    restorePasswordInput = ""
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("رجوع", color = Color.White, fontSize = 11.sp)
                            }
                        }

                        Button(
                            onClick = {
                                val raw = restorePhoneInput.trim().replace(" ", "")
                                val cleanPhone = when {
                                    raw.startsWith("+967") -> raw.substring(4)
                                    raw.startsWith("00967") -> raw.substring(5)
                                    raw.startsWith("0") -> raw.substring(1)
                                    else -> raw
                                }
                                val savedPass = matchedProvider?.password 
                                    ?: matchedPending?.password 
                                    ?: matchedStore?.password 
                                    ?: matchedProperty?.password 
                                    ?: matchedUserDoc?.get("password")?.toString() 
                                    ?: "مخفية/مشفرة"

                                // Dispatch recovery notification directly to supervisor/admin collection so it is guaranteed to reach the admin panel
                                viewModel.requestPasswordRecoveryGeneral(
                                    accountName = accountName,
                                    phone = cleanPhone,
                                    accountType = accountType,
                                    currentPassword = savedPass
                                )

                                val supportChId = "support_" + cleanPhone.ifEmpty { "user" }
                                viewModel.getOrCreateChatChannel(
                                    providerId = "admin",
                                    providerName = "الإدارة والدعم",
                                    customerId = cleanPhone,
                                    customerName = accountName
                                )
                                android.widget.Toast.makeText(context, "💬 تم إرسال طلب إعادة التعيين للإدارة بنجاح!", android.widget.Toast.LENGTH_LONG).show()
                                showRestoreAccountDialog = false
                                restoreStep = 1
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.secondary),
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        ) {
                            Text("💬 نسيت كلمة المرور؟ طلب الاستعادة من الأدمن", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showBackdoorDialogState) {
        var bdPasswordInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { viewModel.dismissBackdoorDialog() },
            containerColor = Color(0xFF0F172A),
            title = { Text("🔓 بوابة تسجيل الدخول الخلفي للمنفذ العظيم", color = themeColors.accent, fontWeight = FontWeight.Bold, fontSize = 14.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("الرجاء إدخال كلمة المرور للتحكم الخلفي والمالك:", color = Color.White, fontSize = 11.sp)
                    OutlinedTextField(
                        value = bdPasswordInput,
                        onValueChange = { bdPasswordInput = it },
                        label = { Text("كلمة المرور") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (viewModel.verifyAdminOrOwnerPassword(bdPasswordInput.trim())) {
                            viewModel.authenticateAdmin("OWNER")
                            viewModel.navigateTo("OWNER_PANEL")
                            viewModel.dismissBackdoorDialog()
                            viewModel.triggerNotification("🔓 تم تفعيل البوابة الخلفية والتحكم الشامل بنجاح!")
                        } else {
                            viewModel.triggerNotification("❌ الرمز السري للمنفذ الخلفي غير صحيح!")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent)
                ) {
                    Text("دخول", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Button(
                    onClick = { viewModel.dismissBackdoorDialog() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                ) {
                    Text("إلغاء الإجراء")
                }
            }
        )
    }
}
}

// ------ Luxury 3D Navigation Icon Component ------
@Composable
fun Luxury3DNavIcon(
    emojiIcon: String,
    vectorIcon: ImageVector?,
    label: String,
    isSelected: Boolean,
    badgeCount: Int = 0,
    iconSizeDp: Int = 20,
    iconStyle: String = "GOLDEN_3D",
    onClick: () -> Unit
) {
    val sizeDp = (iconSizeDp * 0.82f).dp
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 3.dp, vertical = 1.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(sizeDp + 8.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFF59E0B).copy(alpha = 0.45f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )
            }

            Surface(
                shape = CircleShape,
                color = if (isSelected) Color(0xFF1E293B) else Color(0xFF0F172A),
                border = BorderStroke(
                    width = if (isSelected) 1.5.dp else 0.8.dp,
                    brush = Brush.linearGradient(
                        colors = if (isSelected) listOf(
                            Color(0xFFFFFAED),
                            Color(0xFFF59E0B),
                            Color(0xFFD97706),
                            Color(0xFFFEF3C7)
                        ) else listOf(
                            Color(0xFFCBD5E1).copy(alpha = 0.6f),
                            Color(0xFF475569).copy(alpha = 0.3f)
                        )
                    )
                ),
                shadowElevation = if (isSelected) 5.dp else 1.dp,
                modifier = Modifier.size(sizeDp + 5.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    if (vectorIcon != null && iconStyle == "MINIMAL") {
                        Icon(
                            imageVector = vectorIcon,
                            contentDescription = label,
                            tint = if (isSelected) Color(0xFFF59E0B) else Color.White,
                            modifier = Modifier.size(sizeDp)
                        )
                    } else {
                        Text(
                            text = emojiIcon,
                            fontSize = (sizeDp.value * 0.68f).sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            if (badgeCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-2).dp)
                        .size(13.dp)
                        .background(Color.Red, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                        color = Color.White,
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(1.dp))
        Text(
            text = label,
            fontSize = 8.5.sp,
            color = if (isSelected) Color(0xFFFBBF24) else Color.White,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1
        )
    }
}

// ------ Custom Top App Bar ------
@Composable
fun AppHeaderBar(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onNotificationsClick: () -> Unit,
    onChatsClick: () -> Unit,
    chatReadTrigger: Int = 0,
    onMenuClick: () -> Unit
) {
    val settingsState by viewModel.settings.collectAsState()
    val currentScreen by viewModel.currentScreen.collectAsState()
    val allNotifications by viewModel.notifications.collectAsState()
    val userPhoneState by viewModel.currentUserPhone.collectAsState()
    val adminRoleState by viewModel.adminRole.collectAsState()
    val currentUserId by viewModel.currentUserId.collectAsState()
    val currentUserPhone by viewModel.currentUserPhone.collectAsState()
    val chatChannels by viewModel.chatChannels.collectAsState()
    val providers by viewModel.providers.collectAsState()
    val currentLang by viewModel.currentLanguage.collectAsState()
    val isEn = currentLang == "en"

    val myProvider = providers.find { it.phone == currentUserPhone }

    val myChannels = remember(chatChannels, currentUserId, currentUserPhone, myProvider) {
        chatChannels.filter { ch ->
            ch.id == "support_$currentUserId" ||
            ch.id.contains(currentUserId) ||
            (currentUserPhone.isNotEmpty() && ch.id.contains(currentUserPhone)) ||
            (myProvider != null && (ch.id.contains("chat_p_${myProvider.id}_") || ch.id.contains("_u_${myProvider.id}"))) ||
            currentUserId == "admin" || currentUserId.startsWith("super_")
        }
    }

    val headerContext = LocalContext.current
    val headerSp = remember(headerContext) { headerContext.getSharedPreferences("yemen_service_prefs", android.content.Context.MODE_PRIVATE) }
    var headerReadIds by remember { mutableStateOf(headerSp.getStringSet("read_notif_ids", emptySet()) ?: emptySet()) }
    
    // Calculate unread notifications count
    val filteredNotifs = remember(allNotifications, userPhoneState, adminRoleState) {
        allNotifications.filter { notif ->
            when (notif.targetType) {
                "ALL" -> true
                "USER" -> notif.targetValue == userPhoneState
                "PROVIDER" -> notif.targetValue == userPhoneState
                "SUPERVISOR" -> adminRoleState != "GUEST"
                else -> true
            }
        }
    }
    val unreadNotifCount = remember(filteredNotifs, headerReadIds) {
        filteredNotifs.count { it.id !in headerReadIds }
    }

    // Calculate unread chats count
    val unreadChatsCount = remember(myChannels, chatChannels, headerSp, chatReadTrigger) {
        myChannels.count { ch ->
            val lastMsg = ch.messages.lastOrNull()
            if (lastMsg == null) {
                false
            } else {
                val isMe = lastMsg.senderId == currentUserId || (myProvider != null && lastMsg.senderId == myProvider.id)
                val readTime = headerSp.getLong("chat_read_${ch.id}", 0L)
                !isMe && lastMsg.timestamp > readTime
            }
        }
    }

    val screenBackStack by viewModel.screenBackStack.collectAsState()
    val showBackButton = screenBackStack.size > 1 || currentScreen != "USER_BROWSE"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(themeColors.primary)
            .statusBarsPadding()
            .testTag("app_header_bar")
    ) {
        // Row 1: Unified TopBar Header with Back Button (Controlled by Admin hideTopHeaderBar)
        if (!settingsState.hideTopHeaderBar) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showBackButton) {
                    IconButton(
                        onClick = { viewModel.goBack() },
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                            .size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (isEn) "Back" else "رجوع",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                val defaultTitle = if (isEn) "Yemen Services Directory" else "دليل خدمات اليمن"
                val customTitle = settingsState.customAppName.ifEmpty { settingsState.appName.ifEmpty { defaultTitle } }

                val titleText = when (currentScreen) {
                    "REGISTER_FORM" -> if (isEn) "Join & Register" else "الانضمام والتسجيل"
                    "JOIN_REQUEST_STATUS" -> if (isEn) "Request Status" else "حالة طلب الانضمام"
                    "ADMIN_PANEL" -> if (isEn) "Admin Panel" else "لوحة التحكم والإدارة"
                    "OWNER_PANEL" -> if (isEn) "Owner Backdoor" else "البوابة الخلفية"
                    "ABOUT_APP" -> if (isEn) "About App" else "عن التطبيق"
                    "BOOKINGS_VIEW" -> if (isEn) "Bookings & Orders" else "الحجوزات والطلبات"
                    "MAP_VIEW" -> if (isEn) "Services Map" else "خريطة الخدمات"
                    else -> customTitle
                }

                Text(
                    text = titleText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                Spacer(modifier = Modifier.weight(1f))

                // [ طلباتي ] Icon Button in Header
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(themeColors.accent)
                        .clickable { viewModel.navigateTo("ORDERS_VIEW") }
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("📋", fontSize = 12.sp)
                        val isProvider = viewModel.selectedProvider != null || viewModel.selectedStore != null || viewModel.selectedProperty != null
                        Text(
                            text = if (isEn) (if (isProvider) "Requests" else "My Requests") else (if (isProvider) "الطلبات" else "طلباتي"),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                val isRefreshingHeader by viewModel.isRefreshing.collectAsState()
                IconButton(
                    onClick = { viewModel.refreshData() },
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.2f), CircleShape)
                        .size(32.dp)
                ) {
                    if (isRefreshingHeader) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = if (isEn) "Refresh" else "تحديث البيانات",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Row 2: Navigation Items (5 luxury 3D golden icons)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // 1. الرئيسية
            val isBrowse = currentScreen == "USER_BROWSE" || currentScreen == "MAIN_DASHBOARD"
            Luxury3DNavIcon(
                emojiIcon = settingsState.topHomeIcon.ifEmpty { "🏠" },
                vectorIcon = Icons.Default.Home,
                label = if (isEn) "Home" else "الرئيسية",
                isSelected = isBrowse,
                iconSizeDp = settingsState.navIconSizeDp,
                iconStyle = settingsState.topNavIconStyle,
                onClick = {
                    viewModel.navigateTo("USER_BROWSE")
                    viewModel.registerBackdoorInteraction()
                }
            )

            // 2. الخرائط
            if (settingsState.isMapFeatureEnabled) {
                val isMap = currentScreen == "MAP_VIEW"
                Luxury3DNavIcon(
                    emojiIcon = settingsState.topMapsIcon.ifEmpty { "🗺️" },
                    vectorIcon = Icons.Default.Place,
                    label = if (isEn) "Maps" else "الخرائط",
                    isSelected = isMap,
                    iconSizeDp = settingsState.navIconSizeDp,
                    iconStyle = settingsState.topNavIconStyle,
                    onClick = { viewModel.navigateTo("MAP_VIEW") }
                )
            }

            // 3. الانضمام
            val isJoin = currentScreen == "REGISTER_FORM" || currentScreen == "JOIN_REQUEST_STATUS"
            Luxury3DNavIcon(
                emojiIcon = settingsState.topJoinIcon.ifEmpty { "👤" },
                vectorIcon = Icons.Default.Person,
                label = if (isEn) "Join" else "الانضمام",
                isSelected = isJoin,
                iconSizeDp = settingsState.navIconSizeDp,
                iconStyle = settingsState.topNavIconStyle,
                onClick = { viewModel.navigateTo("REGISTER_FORM") }
            )

            // 4. الإشعارات
            Luxury3DNavIcon(
                emojiIcon = settingsState.topNotifIcon.ifEmpty { "🔔" },
                vectorIcon = Icons.Default.Notifications,
                label = if (isEn) "Alerts" else "الإشعارات",
                isSelected = unreadNotifCount > 0,
                badgeCount = unreadNotifCount,
                iconSizeDp = settingsState.navIconSizeDp,
                iconStyle = settingsState.topNavIconStyle,
                onClick = {
                    val allIds = filteredNotifs.map { it.id }.toSet()
                    headerSp.edit().putStringSet("read_notif_ids", allIds).apply()
                    headerReadIds = allIds
                    onNotificationsClick()
                }
            )

            // 5. المحادثات
            val hasUnreadChats = unreadChatsCount > 0
            Luxury3DNavIcon(
                emojiIcon = settingsState.topChatsIcon.ifEmpty { "✉️" },
                vectorIcon = androidx.compose.material.icons.Icons.Default.Email,
                label = if (isEn) "Chats" else "المحادثات",
                isSelected = hasUnreadChats,
                badgeCount = unreadChatsCount,
                iconSizeDp = settingsState.navIconSizeDp,
                iconStyle = settingsState.topNavIconStyle,
                onClick = { onChatsClick() }
            )
        }
    }
}



// ------ Custom Dynamic Footer with Language Switcher and Admin Control ------
@Composable
fun AppFooterBar(viewModel: MainViewModel, themeColors: VisualThemePalette, onInfoClick: () -> Unit) {
    val settingsState by viewModel.settings.collectAsState()
    val currentLang by viewModel.currentLanguage.collectAsState()
    val isEn = currentLang == "en"

    val footerBg = remember(settingsState.footerBgColorHex, themeColors.secondary) {
        try {
            Color(android.graphics.Color.parseColor(settingsState.footerBgColorHex))
        } catch (e: Exception) {
            Color(0xFF0D332D) // Dark teal metallic container like image 2
        }
    }

    Surface(
        color = footerBg,
        shadowElevation = 10.dp,
        border = BorderStroke(1.dp, Brush.horizontalGradient(listOf(Color(0xFF0F5243), Color(0xFF1B8A72), Color(0xFF0F5243)))),
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .testTag("app_footer_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            val bookings by viewModel.bookings.collectAsState()
            val currentUserPhone by viewModel.currentUserPhone.collectAsState()
            val providers by viewModel.providers.collectAsState()

            val matchingProvider = remember(providers, currentUserPhone) {
                providers.find { it.phone.trim() == currentUserPhone.trim() && currentUserPhone.isNotEmpty() }
            }

            val unreadCount = remember(bookings, currentUserPhone, matchingProvider) {
                val custCount = bookings.count { b ->
                    b.customerPhone.trim() == currentUserPhone.trim() && currentUserPhone.isNotEmpty() && (b.status == "PENDING" || b.status == "APPROVED" || b.status == "STARTED")
                }
                val provCount = if (matchingProvider != null) {
                    bookings.count { b ->
                        b.providerId == matchingProvider.id && (b.status == "PENDING" || b.status == "APPROVED" || b.status == "STARTED")
                    }
                } else 0
                custCount + provCount
            }

            // 1. Info Icon ("عن التطبيق")
            if (settingsState.showInfoIcon) {
                Luxury3DNavIcon(
                    emojiIcon = settingsState.bottomInfoIcon.ifEmpty { "ℹ️" },
                    vectorIcon = Icons.Default.Info,
                    label = if (isEn) "About" else "عن التطبيق",
                    isSelected = false,
                    iconSizeDp = settingsState.navIconSizeDp,
                    iconStyle = settingsState.topNavIconStyle,
                    onClick = { onInfoClick() }
                )
            }

            // 2. Bookings Icon ("الحجوزات")
            if (settingsState.showBookingsIcon) {
                Luxury3DNavIcon(
                    emojiIcon = settingsState.bottomBookingsIcon.ifEmpty { "📅" },
                    vectorIcon = Icons.Default.DateRange,
                    label = if (isEn) "Bookings" else "الحجوزات",
                    isSelected = false,
                    badgeCount = unreadCount,
                    iconSizeDp = settingsState.navIconSizeDp,
                    iconStyle = settingsState.topNavIconStyle,
                    onClick = { viewModel.navigateTo("BOOKINGS_VIEW") }
                )
            }

            // 3. Center Brand Text ("WAM2026")
            if (settingsState.showFooterText) {
                Box(
                    modifier = Modifier.padding(horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = settingsState.footerMessage.ifBlank { "WAM2026" },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFE2E8F0),
                        letterSpacing = 1.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 4. Single Clean Language Switcher Icon ("EN" / "🌐")
            if (settingsState.showLangIcon) {
                Luxury3DNavIcon(
                    emojiIcon = settingsState.bottomLangIcon.ifEmpty { if (isEn) "🌐" else "EN" },
                    vectorIcon = null,
                    label = if (isEn) "العربية" else "English",
                    isSelected = false,
                    iconSizeDp = settingsState.navIconSizeDp,
                    iconStyle = settingsState.topNavIconStyle,
                    onClick = {
                        viewModel.switchLanguage()
                        viewModel.triggerNotification(
                            if (isEn) "تم التحويل إلى اللغة العربية 🇾🇪" else "Language switched to English 🌐"
                        )
                    }
                )
            }

            // 5. Admin Lock Icon ("الإدارة")
            if (settingsState.showAdminIcon) {
                Luxury3DNavIcon(
                    emojiIcon = settingsState.bottomAdminIcon.ifEmpty { "🔒" },
                    vectorIcon = Icons.Default.Lock,
                    label = if (isEn) "Admin" else "الإدارة",
                    isSelected = false,
                    iconSizeDp = settingsState.navIconSizeDp,
                    iconStyle = settingsState.topNavIconStyle,
                    onClick = { viewModel.navigateTo("ADMIN_PANEL") }
                )
            }
        }
    }
}

// ------ Floating Icons Overlay Container ------
@Composable
fun BoxScope.FloatingIconsOverlay(
    settings: AdminSettingsEntity,
    themeColors: VisualThemePalette,
    onAssistantClick: () -> Unit,
    onRequestServiceClick: () -> Unit
) {
    // 1. Primary Action FAB: "اطلب خدمتك الآن" (Instant Request Service / Reverse Marketplace FAB)
    Box(
        modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(start = 12.dp, bottom = 14.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFF10B981), Color(0xFF059669))
                )
            )
            .clickable { onRequestServiceClick() }
            .border(1.dp, Color.White, RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Send,
                contentDescription = "اطلب خدمتك الآن",
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = "اطلب خدمتك الآن ⚡",
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }

    // 2. Secondary FAB: "المساعد الذكي" (Offline Local AI Assistant FAB)
    if (!settings.assistantHidden) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 14.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(themeColors.accent)
                .clickable { onAssistantClick() }
                .border(1.dp, Color.White, RoundedCornerShape(20.dp))
                .padding(horizontal = 11.dp, vertical = 7.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("🤖", fontSize = 13.sp)
                Text(
                    text = "المساعد الذكي",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }
    }
}

// ------ Maintenance Banner view ------
@Composable
fun MaintenanceSplashView(settingsState: AdminSettingsEntity, themeColors: VisualThemePalette, viewModel: MainViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(imageVector = Icons.Default.Warning, contentDescription = "تحت الصيانة", tint = themeColors.accent, modifier = Modifier.size(72.dp))
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = "التطبيق في وضع الصيانة والتحديث",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = themeColors.textPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "رسالة الإدارة: ${settingsState.welcomeMessage}",
            fontSize = 13.sp,
            color = themeColors.textSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { viewModel.navigateTo("ADMIN_PANEL") },
            colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary)
        ) {
            Text("تسجيل دخول المالك", color = Color.White)
        }
    }
}

@Composable
fun AdminCustomBannerView(settingsState: com.example.data.AdminSettingsEntity, themeColors: VisualThemePalette) {
    var isVisible by remember(settingsState.bannerContent, settingsState.bannerEnabled) { mutableStateOf(true) }
    
    if (settingsState.bannerDurationSeconds > 0) {
        LaunchedEffect(settingsState.bannerContent, settingsState.bannerEnabled) {
            kotlinx.coroutines.delay(settingsState.bannerDurationSeconds * 1000L)
            isVisible = false
        }
    }

    if (!isVisible || !settingsState.bannerEnabled) return

    val imageBitmap = remember(settingsState.bannerBase64) {
        if (!settingsState.bannerBase64.isNullOrEmpty()) {
            try {
                val bytes = android.util.Base64.decode(settingsState.bannerBase64, android.util.Base64.DEFAULT)
                android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            } catch(e: Exception) { null }
        } else null
    }

    var startAnimation by remember { mutableStateOf(false) }
    LaunchedEffect(isVisible) {
        if (isVisible) {
            startAnimation = true
        }
    }

    val style = settingsState.bannerDisplayStyle ?: "SLIDE"

    // Animations corresponding to display styles
    val slideOffset by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (startAnimation && style == "SLIDE") 0.dp else if (style == "SLIDE") (-300).dp else 0.dp,
        animationSpec = androidx.compose.animation.core.tween(800)
    )

    val fadeAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(1000)
    )

    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition()
    val blinkAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(800, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        )
    )

    val scrollOffset by infiniteTransition.animateFloat(
        initialValue = 50f,
        targetValue = -50f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(4000, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        )
    )

    val appliedModifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp)
        .let { modifier ->
            when (style) {
                "SLIDE" -> modifier.offset(x = slideOffset)
                "FADE" -> modifier.alpha(fadeAlpha)
                "BLINK" -> modifier.alpha(blinkAlpha)
                "SCROLL" -> modifier.offset(x = scrollOffset.dp)
                else -> modifier
            }
        }

    Card(
        modifier = appliedModifier,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.5.dp, themeColors.accent.copy(alpha = 0.6f)),
        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                // Header row with title and type icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val icon = when (settingsState.bannerType) {
                            "IMAGE" -> "🖼️"
                            "VIDEO" -> "📹"
                            else -> "📢"
                        }
                        Text(icon, fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "إعلان رسمي من إدارة المنصة",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = themeColors.accent
                        )
                    }
                    
                    // Dismiss button
                    IconButton(
                        onClick = { isVisible = false },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Text("✕", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))

                when (settingsState.bannerType) {
                    "IMAGE" -> {
                        if (imageBitmap != null) {
                            Image(
                                bitmap = imageBitmap,
                                contentDescription = "إعلان بنر",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } else {
                            // Load from remote URL or show decorative placeholder
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                                    .background(Color.DarkGray, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("📢 إعلان مرئي", fontSize = 16.sp, color = themeColors.accent)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(settingsState.bannerContent, fontSize = 11.sp, color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 8.dp))
                                }
                            }
                        }
                    }
                    "VIDEO" -> {
                        // Simulated high-fidelity Video Player card as requested
                        var isPlaying by remember { mutableStateOf(true) }
                        var simulatedProgress by remember { mutableStateOf(0.4f) }
                        
                        LaunchedEffect(isPlaying) {
                            if (isPlaying) {
                                while (true) {
                                    kotlinx.coroutines.delay(1000L)
                                    simulatedProgress = (simulatedProgress + 0.05f)
                                    if (simulatedProgress >= 1.0f) simulatedProgress = 0.0f
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            if (imageBitmap != null) {
                                Image(
                                    bitmap = imageBitmap,
                                    contentDescription = "فيديو البنر",
                                    modifier = Modifier.fillMaxSize().alpha(0.6f),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            } else {
                                Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0F172A)).alpha(0.5f))
                            }
                            
                            // REC Indicator
                            Row(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(Color.Red, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("LIVE", fontSize = 9.sp, color = Color.Red, fontWeight = FontWeight.Bold)
                            }

                            // Play/Pause Overlay Button
                            IconButton(
                                onClick = { isPlaying = !isPlaying },
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            ) {
                                Text(if (isPlaying) "⏸️" else "▶️", fontSize = 18.sp)
                            }

                            // Video Controls Overlay at Bottom
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .background(Color.Black.copy(alpha = 0.6f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (settingsState.bannerContent.length > 30) settingsState.bannerContent.take(30) + "..." else settingsState.bannerContent,
                                        fontSize = 10.sp,
                                        color = Color.White
                                    )
                                    Text("00:0${(simulatedProgress * 15).toInt()} / 00:15", fontSize = 9.sp, color = Color.LightGray)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                // Custom progress bar
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(3.dp)
                                        .background(Color.Gray)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(simulatedProgress)
                                            .fillMaxHeight()
                                            .background(themeColors.accent)
                                    )
                                }
                            }
                        }
                    }
                    else -> {
                        // Text Banner
                        val welcomeMsg = "أهلاً بكم في دليل خدمات اليمن! المنصة الأولى لربط مقدمي الخدمات والمهنيين والمراكز التجارية مع المستخدمين، وانتظروا الإضافات القادمة! ✨"
                        val displayText = if (settingsState.bannerContent.contains("خصومات") || settingsState.bannerContent.contains("الصيانة الكهربائية") || settingsState.bannerContent.isBlank()) {
                            welcomeMsg
                        } else {
                            settingsState.bannerContent
                        }
                        Card(
                            colors = CardDefaults.cardColors(containerColor = themeColors.accent.copy(alpha = 0.15f)),
                            border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("📢", fontSize = 24.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = displayText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ------ Main Category and Service Directory Browser Layout ------
/* ServicesBrowserLayout has been moved to com.example.ui.screens.home.ServicesBrowserLayout */
// ------ Horizontal Advertisement Banner Composable ------
@Composable
fun BannerSliderView(banners: List<BannerEntity>, themeColors: VisualThemePalette, onBannerClick: (String) -> Unit) {
    var currentIndex by remember { mutableStateOf(0) }
    
    val activeBanner = if (banners.isNotEmpty()) banners.getOrNull(currentIndex) else null
    
    LaunchedEffect(currentIndex, banners) {
        if (banners.isNotEmpty()) {
            val active = banners.getOrNull(currentIndex)
            val durationSec = if (active != null && active.duration > 0) active.duration else 5
            kotlinx.coroutines.delay(durationSec * 1000L)
            currentIndex = (currentIndex + 1) % banners.size
        }
    }

    if (activeBanner != null) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .clickable { onBannerClick(activeBanner.redirectCategory) },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = themeColors.secondary)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                when (activeBanner.type.uppercase()) {
                    "IMAGE" -> {
                        if (activeBanner.url.isNotEmpty()) {
                            if (activeBanner.url.startsWith("data:image") || activeBanner.url.length > 200) {
                                val bitmap = remember(activeBanner.url) {
                                    try {
                                        val base64Data = if (activeBanner.url.contains(",")) activeBanner.url.substringAfter(",") else activeBanner.url
                                        val decodedBytes = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)
                                        BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                                    } catch (e: Exception) {
                                        null
                                    }
                                }
                                if (bitmap != null) {
                                    Image(
                                        painter = BitmapPainter(bitmap.asImageBitmap()),
                                        contentDescription = activeBanner.title,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                    )
                                } else {
                                    AsyncImage(
                                        model = activeBanner.url,
                                        contentDescription = activeBanner.title,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                    )
                                }
                            } else {
                                AsyncImage(
                                    model = activeBanner.url,
                                    contentDescription = activeBanner.title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            }
                            // Overlay gradient for text legibility
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                                        )
                                    )
                            )
                            // Title & Label at bottom
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = activeBanner.title,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                if (activeBanner.redirectCategory.isNotEmpty()) {
                                    Text(
                                        text = "اضغط للانتقال إلى قسم: ${activeBanner.redirectCategory}",
                                        fontSize = 9.sp,
                                        color = themeColors.accent
                                    )
                                }
                            }
                        } else {
                            // Text Fallback if URL is empty
                            BannerTextFallback(activeBanner = activeBanner, themeColors = themeColors)
                        }
                    }
                    "VIDEO" -> {
                        if (activeBanner.url.isNotEmpty()) {
                            val context = LocalContext.current
                            androidx.compose.ui.viewinterop.AndroidView(
                                factory = { ctx ->
                                    android.widget.VideoView(ctx).apply {
                                        setVideoURI(android.net.Uri.parse(activeBanner.url))
                                        setOnPreparedListener { mp ->
                                            mp.isLooping = true
                                            mp.setVolume(0f, 0f) // Silent looping banner
                                            start()
                                        }
                                        setOnErrorListener { _, _, _ -> true } // silent failure
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                            // Overlay gradient
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                                        )
                                    )
                            )
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("🎬 فيديو مميز", fontSize = 9.sp, color = themeColors.accent, fontWeight = FontWeight.Bold)
                                }
                                Text(
                                    text = activeBanner.title,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        } else {
                            BannerTextFallback(activeBanner = activeBanner, themeColors = themeColors)
                        }
                    }
                    else -> { // TEXT banner
                        BannerTextFallback(activeBanner = activeBanner, themeColors = themeColors)
                    }
                }
            }
        }
    }
}

@Composable
fun BannerTextFallback(activeBanner: BannerEntity, themeColors: VisualThemePalette) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.horizontalGradient(
                    listOf(
                        themeColors.primary,
                        themeColors.secondary.copy(alpha = 0.8f)
                    )
                )
            )
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Info, contentDescription = "إعلان ممتاز", tint = themeColors.accent, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "إعلان رسمي دليل خدمات اليمن",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = themeColors.accent
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = activeBanner.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (activeBanner.redirectCategory.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "🔗 الانتقال لقسم: ${activeBanner.redirectCategory}",
                    fontSize = 9.sp,
                    color = themeColors.accent.copy(alpha = 0.9f),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ------ Category section icon decorator helper ------
@Composable
fun CategorySectionIconView(iconStr: String, modifier: Modifier = Modifier, size: androidx.compose.ui.unit.Dp = 20.dp) {
    if (iconStr.length > 15) {
        if (iconStr.startsWith("http://") || iconStr.startsWith("https://")) {
            AsyncImage(
                model = iconStr,
                contentDescription = null,
                modifier = modifier.size(size)
            )
        } else {
            val bitmap = remember(iconStr) {
                try {
                    val base64Data = if (iconStr.contains(",")) iconStr.substringAfter(",") else iconStr
                    val decodedBytes = Base64.decode(base64Data, Base64.DEFAULT)
                    BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                } catch (e: Exception) {
                    null
                }
            }
            if (bitmap != null) {
                Image(
                    painter = BitmapPainter(bitmap.asImageBitmap()),
                    contentDescription = null,
                    modifier = modifier.size(size)
                )
            } else {
                Text(text = "📁", fontSize = 14.sp)
            }
        }
    } else {
        Text(text = iconStr.ifEmpty { "📁" }, fontSize = 14.sp, modifier = modifier)
    }
}

// ------ Category selection chip ------
@Composable
fun CategoryChip(name: String, icon: String, isSelected: Boolean, themeColors: VisualThemePalette, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(horizontal = 4.dp, vertical = 4.dp)
            .width(76.dp)
    ) {
        val luxuryGoldBrush = Brush.linearGradient(
            colors = listOf(
                Color(0xFFFFE259), // Light Gold
                Color(0xFFFFA751), // Deep Orange Gold
                Color(0xFFFFE259)  // Light Gold
            )
        )
        
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(if (isSelected) themeColors.accent.copy(alpha = 0.25f) else themeColors.surface)
                .border(
                    width = if (isSelected) 2.5.dp else 1.dp,
                    brush = if (isSelected) luxuryGoldBrush else Brush.linearGradient(listOf(themeColors.accent.copy(alpha = 0.3f), themeColors.accent.copy(alpha = 0.1f))),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            CategorySectionIconView(iconStr = icon, size = 26.dp)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = name,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) themeColors.accent else Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ------ Detailed Provider Placeholder Card ------
@Composable
fun DetailedProviderPlaceholderCard(themeColors: VisualThemePalette) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .testTag("provider_detail_placeholder_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
        border = BorderStroke(2.dp, themeColors.accent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header with VIP badge and Rating
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(themeColors.accent, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "👑 نموذجي معتمد",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                    Text(
                        text = "صيانة منزلية",
                        fontSize = 10.sp,
                        color = themeColors.accent,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Rating Star Indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Rating",
                        tint = themeColors.accent,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "4.9 (نموذج)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Provider Name & Call Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "امين الغرباني (صيانة عامة)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Location",
                            tint = themeColors.textSecondary,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "صنعاء، منطقة الدائري جوار مدرسة اسماء للبنات",
                            fontSize = 11.sp,
                            color = themeColors.textSecondary
                        )
                    }
                }

                // Call Button
                IconButton(
                    onClick = {
                        val callIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:777703195"))
                        context.startActivity(callIntent)
                    },
                    modifier = Modifier
                        .background(Color.Green.copy(alpha = 0.2f), CircleShape)
                        .size(40.dp)
                        .testTag("provider_placeholder_call_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "اتصال بالفني",
                        tint = Color.Green,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            HorizontalDivider(
                color = themeColors.accent.copy(alpha = 0.2f),
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // Service Description
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "وصف الخدمة النموذجية:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = themeColors.accent
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "مختص صيانة وتمديد كهربائي، صيانة المكيفات والأجهزة المنزلية بدقة وأمان تام. تتوفر لدينا أحدث أجهزة الفحص وبأسعار مناسبة ومعتمدة مع ضمان الخدمة.",
                    fontSize = 11.sp,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun ProviderCard(
    provider: ProviderEntity,
    themeColors: VisualThemePalette,
    viewModel: MainViewModel,
    onChatOpen: (String) -> Unit
) {
    val context = LocalContext.current
    val settingsState by viewModel.settings.collectAsState()
    val currentUserIdState by viewModel.currentUserId.collectAsState()
    val currentUserNameState by viewModel.currentUserName.collectAsState()
    val currentUserPhoneState by viewModel.currentUserPhone.collectAsState()
    val currentUserResidenceState by viewModel.currentUserResidence.collectAsState()
    val categories by viewModel.categories.collectAsState()

    var showDetailsDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var selectedReportReason by remember { mutableStateOf("سلوك غير لائق") }
    var showGuestRegisterDialogForBooking by remember { mutableStateOf(false) }

    // --- NEW Custom States for Interactive Dialogs & Popups ---
    var simulateAdminMode by remember { mutableStateOf(false) }
    var showReviewsListDialog by remember { mutableStateOf(false) }
    var activeVoiceCallForProvider by remember { mutableStateOf<Pair<String, String>?>(null) }
    val activeCallFromVm by viewModel.activeVoiceCall.collectAsState()

    if (activeVoiceCallForProvider != null || activeCallFromVm != null) {
        val currentCall = activeCallFromVm ?: activeVoiceCallForProvider
        val callerName = currentCall?.first ?: ""
        val callerRole = currentCall?.second ?: ""
        InAppVoiceCallDialog(
            callerName = callerName,
            callerRole = callerRole,
            onDismiss = {
                activeVoiceCallForProvider = null
                viewModel.endVoiceCall()
            },
            themeColors = themeColors
        )
    }
    var showInstantChatDialog by remember { mutableStateOf(false) }
    var chatInputText by remember { mutableStateOf("") }
    var chatHistoryList by remember { mutableStateOf(listOf<Pair<String, Boolean>>()) }
    var newCommentText by remember { mutableStateOf("") }
    var newCommentAuthor by remember { mutableStateOf("") }

    // User-side Payment Dialog States
    var payingBookingObj by remember { mutableStateOf<BookingEntity?>(null) }
    var selectedUserWalletObj by remember { mutableStateOf<PaymentWalletEntity?>(null) }
    var userTransferIdInput by remember { mutableStateOf("") }
    var userTransferAccountNameInput by remember { mutableStateOf("") }
    var userTransferPhotoInput by remember { mutableStateOf("") }

    // Booking form inputs
    var customerNameInput by remember { mutableStateOf("") }
    var customerPhoneInput by remember { mutableStateOf("") }
    var customerAreaInput by remember { mutableStateOf("") }
    var customerServiceInput by remember { mutableStateOf("") }
    var bookingDateInput by remember { mutableStateOf("") }
    var bookingTimeInput by remember { mutableStateOf("") }
    var bookingCouponCodeInput by remember { mutableStateOf("") }
    var bookingPinCodeInput by remember { mutableStateOf("") }
    var bookingCustomIdInput by remember { mutableStateOf("") }
    var unlockedBookingIds by remember { mutableStateOf(setOf<String>()) }
    var selectedServiceDropdown by remember { mutableStateOf("صيانة أعطال عامة") }
    var serviceDropdownExpanded by remember { mutableStateOf(false) }
    var showBookingDialog by remember { mutableStateOf(false) }
    var showBookingConfirmDialog by remember { mutableStateOf(false) }
    var bookingFormSubmittedOnce by remember { mutableStateOf(false) }
    var bookingFormMissingFields by remember { mutableStateOf<List<String>>(emptyList()) }

    // Admin Control Dialog States
    var showAdminEditName by remember { mutableStateOf(false) }
    var showAdminEditRating by remember { mutableStateOf(false) }
    var showAdminEditLocation by remember { mutableStateOf(false) }
    var showAdminEditImage by remember { mutableStateOf(false) }
    var showAdminEditButtons by remember { mutableStateOf(false) }
    var showAdminEditTexts by remember { mutableStateOf(false) }
    var showAdminEditDesign by remember { mutableStateOf(false) }

    // Admin inputs
    var adminNameInput by remember { mutableStateOf(provider.name) }
    var adminRatingInput by remember { mutableStateOf(provider.rating) }
    var adminReviewsCountInput by remember { mutableStateOf(provider.numReviews) }
    var adminAreaInput by remember { mutableStateOf(provider.area) }
    var adminNeighborhoodInput by remember { mutableStateOf(provider.localNeighborhood) }
    var adminProfileImageInput by remember { mutableStateOf(provider.profileImage) }
    var adminPhoneInput by remember { mutableStateOf(provider.phone) }
    var adminPreviewPriceInput by remember { mutableStateOf(provider.previewPrice) }

    val coroutineScope = rememberCoroutineScope()
    val adminRoleState by viewModel.adminRole.collectAsState()
    val isAdminActive = adminRoleState != "GUEST" || simulateAdminMode

    LaunchedEffect(provider) {
        adminNameInput = provider.name
        adminRatingInput = provider.rating
        adminReviewsCountInput = provider.numReviews
        adminAreaInput = provider.area
        adminNeighborhoodInput = provider.localNeighborhood
        adminProfileImageInput = provider.profileImage
        adminPhoneInput = provider.phone
        adminPreviewPriceInput = provider.previewPrice
    }

    LaunchedEffect(showBookingDialog) {
        if (showBookingDialog) {
            customerNameInput = currentUserNameState
            customerPhoneInput = currentUserPhoneState
            customerAreaInput = currentUserResidenceState
            
            val currentCalendar = java.util.Calendar.getInstance()
            val year = currentCalendar.get(java.util.Calendar.YEAR)
            val month = currentCalendar.get(java.util.Calendar.MONTH) + 1
            val day = currentCalendar.get(java.util.Calendar.DAY_OF_MONTH)
            bookingDateInput = "$year/$month/$day"
            
            val hourOfDay = currentCalendar.get(java.util.Calendar.HOUR_OF_DAY)
            val minute = currentCalendar.get(java.util.Calendar.MINUTE)
            val amPm = if (hourOfDay < 12) "ص" else "م"
            val hour = if (hourOfDay % 12 == 0) 12 else hourOfDay % 12
            val formattedMin = String.format("%02d", minute)
            bookingTimeInput = "$hour:$formattedMin $amPm"
            
            selectedServiceDropdown = "صيانة أعطال عامة"
            customerServiceInput = ""
        }
    }

    var showAddCommentDialog by remember { mutableStateOf(false) }

    // Parse customized styles with absolute safety (try/catch default values fallback)
    val cardBg = remember(settingsState.cardBackgroundHex, themeColors.surface) {
        try { Color(android.graphics.Color.parseColor(settingsState.cardBackgroundHex)) } catch (e: Exception) { themeColors.surface }
    }
    val nameColor = remember(settingsState.providerNameColorHex) {
        try { Color(android.graphics.Color.parseColor(settingsState.providerNameColorHex)) } catch (e: Exception) { Color.White }
    }
    val locationColor = remember(settingsState.locationColorHex, themeColors.textSecondary) {
        try { Color(android.graphics.Color.parseColor(settingsState.locationColorHex)) } catch (e: Exception) { themeColors.textSecondary }
    }
    val ratingColor = remember(settingsState.ratingColorHex, themeColors.accent) {
        try { Color(android.graphics.Color.parseColor(settingsState.ratingColorHex)) } catch (e: Exception) { themeColors.accent }
    }
    val priceColor = remember(settingsState.previewPriceColorHex) {
        try { Color(android.graphics.Color.parseColor(settingsState.previewPriceColorHex)) } catch (e: Exception) { Color(0xFF10B981) }
    }

    // Interactive scale animation
    var isPressed by remember { mutableStateOf(false) }
    val scaleFactor by animateFloatAsState(
        targetValue = if (settingsState.enableScaleAnimation && isPressed) settingsState.clickScaleRatio else 1.0f,
        label = "click_scale"
    )

    val luxuryGoldBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFFFFD700), // Pure Gold
            Color(0xFFFFA500), // Orange-Gold
            Color(0xFFB8860B), // Dark Goldenrod
            Color(0xFFFFD700)  // Gold shine repeat
        )
    )

    val metallicGlassBrush = Brush.verticalGradient(
        colors = listOf(
            cardBg.copy(alpha = 0.88f),
            cardBg.copy(alpha = 0.98f)
        )
    )

    // Helper to generate Star string representation dynamically (e.g. ★★★★☆)
    fun getStarsString(r: Float): String {
        val filled = r.toInt().coerceIn(0, 5)
        val empty = (5 - filled).coerceIn(0, 5)
        return "★".repeat(filled) + "☆".repeat(empty)
    }

    // --- Card View Body (Miniature & Edge-to-Edge with zero margins & 70% of original layout - 30% smaller) ---
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp, horizontal = 0.dp)
            .scale(scaleFactor)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    }
                )
            },
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Brush.linearGradient(listOf(themeColors.accent.copy(alpha = 0.3f), themeColors.accent.copy(alpha = 0.05f))))
    ) {
        Box(
            modifier = Modifier
                .background(metallicGlassBrush)
                .padding((if (settingsState.cardPadding > 0) (settingsState.cardPadding * 0.35f).toInt().coerceAtLeast(3) else 4).dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy((if (settingsState.elementSpacing > 0) (settingsState.elementSpacing * 0.35f).toInt().coerceAtLeast(2) else 3).dp)) {
                
                // Top Cover Banner if enabled and present
                if (settingsState.coverHeight > 0 && provider.coverImage.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((settingsState.coverHeight * 0.42f).dp)
                            .clip(RoundedCornerShape(4.dp))
                    ) {
                        AsyncImage(
                            model = provider.coverImage,
                            contentDescription = "صورة غلاف الفني",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    }
                }

                // 1. Core Profile Row (Compact Circular Avatar + Dynamic Name & Details + Rating + Location)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // [Compact] Circular Avatar Image (30% smaller)
                    Box(
                        modifier = Modifier
                            .size((if (settingsState.avatarSize > 0) (settingsState.avatarSize * 0.45f) else 29f).dp)
                            .clip(if (settingsState.avatarShape == "ROUNDED") RoundedCornerShape(6.dp) else CircleShape)
                            .background(Color.Black)
                            .border(1.dp, themeColors.accent, if (settingsState.avatarShape == "ROUNDED") RoundedCornerShape(6.dp) else CircleShape)
                    ) {
                        val isValidUrl = provider.profileImage.startsWith("http://") || provider.profileImage.startsWith("https://") || provider.profileImage.startsWith("content://") || provider.profileImage.startsWith("file://")
                        val isBase64 = provider.profileImage.length > 20 && !isValidUrl

                        val base64Bitmap = remember(provider.profileImage) {
                            if (isBase64) {
                                try {
                                    val cleanBase64 = if (provider.profileImage.contains(",")) provider.profileImage.substringAfter(",") else provider.profileImage
                                    val bytes = android.util.Base64.decode(cleanBase64, android.util.Base64.DEFAULT)
                                    android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                } catch (e: Exception) {
                                    null
                                }
                            } else {
                                null
                            }
                        }

                        if (base64Bitmap != null) {
                            androidx.compose.foundation.Image(
                                bitmap = base64Bitmap.asImageBitmap(),
                                contentDescription = "الصورة الشخصية",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } else if (isValidUrl) {
                            AsyncImage(
                                model = provider.profileImage,
                                contentDescription = "الصورة الشخصية",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Brush.linearGradient(listOf(themeColors.primary, themeColors.accent.copy(alpha = 0.8f)))),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = provider.name.trim().take(1).ifEmpty { "👤" },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = Color.Black
                                )
                            }
                        }
                    }

                    // Provider Information Details Column (Compact)
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        // Name and Profile Popup Trigger
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = provider.name,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = nameColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            
                            // 👤 عرض الملف الشخصي (شاشة منبثقة)
                            Text(
                                text = "👤 عرض الملف",
                                fontSize = 7.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = themeColors.accent,
                                modifier = Modifier
                                    .background(themeColors.accent.copy(alpha = 0.15f), RoundedCornerShape(3.dp))
                                    .padding(horizontal = 3.dp, vertical = 1.dp)
                                    .clickable { showDetailsDialog = true }
                            )
                        }

                        // Profession and Specialization badges
                        val cardCatName = if (provider.categoryId == "other" && provider.customCategoryName.isNotEmpty()) provider.customCategoryName else (categories.find { it.id == provider.categoryId }?.name ?: "صيانة فنية")
                        val cardProfessionText = provider.profession.ifEmpty { cardCatName }
                        val cardSpecializationText = provider.specialization.ifEmpty { "متخصص معتمد" }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            modifier = Modifier.padding(vertical = 1.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(themeColors.primary.copy(alpha = 0.2f))
                                    .padding(horizontal = 3.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "💼 $cardProfessionText",
                                    fontSize = 7.5.sp,
                                    color = themeColors.accent,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(themeColors.accent.copy(alpha = 0.15f))
                                    .padding(horizontal = 3.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "🎓 $cardSpecializationText",
                                    fontSize = 7.5.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Rating: ★★★★☆ 4.8 (0 تقييمات)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = getStarsString(provider.rating),
                                fontSize = 8.sp,
                                color = Color(0xFFFFD700),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${provider.rating} (${provider.numReviews} تقييم)",
                                fontSize = 7.5.sp,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }

                        // Location: 📍 صنعاء ✏️
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "📍 ${provider.area}، ${provider.localNeighborhood}",
                                fontSize = 8.sp,
                                color = locationColor,
                                fontWeight = FontWeight.Medium
                            )
                            if (isAdminActive) {
                                Text(
                                    text = "✏️",
                                    fontSize = 8.sp,
                                    modifier = Modifier
                                        .clickable { showAdminEditLocation = true }
                                        .padding(horizontal = 1.dp)
                                )
                            }
                        }

                        // Profession & Specialization
                        if (provider.profession.isNotEmpty() || provider.specialization.isNotEmpty()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                modifier = Modifier.padding(top = 1.dp)
                            ) {
                                val profText = listOfNotNull(
                                    if (provider.profession.isNotEmpty()) "المهنة: ${provider.profession}" else null,
                                    if (provider.specialization.isNotEmpty()) "التخصص: ${provider.specialization}" else null
                                ).joinToString(" | ")
                                Icon(
                                    imageVector = Icons.Default.Build,
                                    contentDescription = null,
                                    tint = themeColors.accent,
                                    modifier = Modifier.size(8.dp)
                                )
                                Text(
                                    text = profText,
                                    fontSize = 7.5.sp,
                                    color = themeColors.accent,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = themeColors.accent.copy(alpha = 0.15f))

                // 2. Compact Communication Methods
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    // 📩 مراسلة فورية
                    if (settingsState.showInstantChatButton) {
                        Button(
                            onClick = {
                                if (provider.isChatDisabled) {
                                    Toast.makeText(context, "⚠️ عذراً، لقد تم إيقاف خدمة الدردشة مع هذا الفني مؤقتاً بواسطة الإدارة.", Toast.LENGTH_LONG).show()
                                } else if (currentUserIdState == "guest" && !settingsState.bypassVisitorRegistration && !settingsState.disableChatFirewall) {
                                    showGuestRegisterDialogForBooking = true
                                } else {
                                    val targetId = if (provider.chatRecipientId.isNotEmpty()) provider.chatRecipientId else provider.id
                                    val customerIdForChat = if (currentUserPhoneState.isNotEmpty()) currentUserPhoneState else currentUserIdState
                                    val chatRoomId = "chat_p_${targetId}_u_${customerIdForChat}"
                                    viewModel.getOrCreateChatChannel(
                                        targetId,
                                        provider.name,
                                        customerIdForChat,
                                        currentUserNameState
                                    )
                                    onChatOpen(chatRoomId)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary.copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 2.dp, horizontal = 2.dp),
                            border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f))
                        ) {
                            Text("📩 مراسلة", fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    // 📞 اتصال مباشر
                    if (settingsState.showCallButton) {
                        Button(
                            onClick = {
                                viewModel.logCall(provider.id, provider.name)
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${provider.phone}"))
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 2.dp, horizontal = 2.dp)
                        ) {
                            Text("📞 اتصال", fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    // 🎙️ مكالمة صوتية
                    if (settingsState.showVoiceCallButton && !settingsState.disableVoiceCalls) {
                        Button(
                            onClick = {
                                viewModel.logCall(provider.id, provider.name)
                                activeVoiceCallForProvider = Pair(provider.name, "فني / مقدم خدمة")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 2.dp, horizontal = 2.dp)
                        ) {
                            Text("🎙️ مكالمة", fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // 3. Compact Extra Options ("💬 آراء وتجارب | ✍️ أضف تعليق")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    // 💬 آراء وتجارب
                    Button(
                        onClick = { showReviewsListDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 1.5.dp, horizontal = 2.dp),
                        border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f))
                    ) {
                        Text("💬 آراء العملاء", fontSize = 7.5.sp, color = themeColors.textSecondary)
                    }

                    // ✍️ أضف تعليق
                    Button(
                        onClick = { showAddCommentDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 1.5.dp, horizontal = 2.dp),
                        border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f))
                    ) {
                        Text("✍️ تعليق جديد", fontSize = 7.5.sp, color = themeColors.textSecondary)
                    }
                }

                // 4. Compact Main Service Button ("📅 حجز موعد خدمة فورية")
                Button(
                    onClick = {
                        if (currentUserIdState == "guest" && !settingsState.bypassVisitorRegistration && !settingsState.disableBookingFirewall) {
                            showGuestRegisterDialogForBooking = true
                        } else {
                            showBookingDialog = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 4.dp, horizontal = 6.dp)
                ) {
                    Icon(imageVector = Icons.Default.DateRange, contentDescription = null, tint = Color.Black, modifier = Modifier.size(10.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("📅 حجز موعد خدمة فورية ومباشرة", fontSize = 8.5.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                }

                // Availability Status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (provider.isAvailable) "🟢 متاح للعمل الآن" else "🔴 مشغول حالياً",
                        fontSize = 8.sp,
                        color = if (provider.isAvailable) Color.Green else Color.Red,
                        fontWeight = FontWeight.Bold
                    )
                }

                // 5. Admin Panel Section ("🔧 صلاحيات الأدمن")
                if (isAdminActive) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🔧 لوحة صلاحيات الأدمن:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                                Spacer(modifier = Modifier.weight(1f))
                                Box(
                                    modifier = Modifier
                                        .background(Color.Green.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("نشط", fontSize = 8.sp, color = Color.Green, fontWeight = FontWeight.Bold)
                                }
                            }

                            // 2-column Admin Button Grid
                            val adminOptionsList = listOf(
                                "📝 تعديل الاسم" to { showAdminEditName = true },
                                "⭐ تغيير التقييم" to { showAdminEditRating = true },
                                "📍 تحديث الموقع" to { showAdminEditLocation = true },
                                "🖼️ تبديل الصورة" to { showAdminEditImage = true },
                                "🔘 تخصيص الأزرار" to { showAdminEditButtons = true },
                                "✍️ تعديل العناوين" to { showAdminEditTexts = true },
                                "🎨 مظهر وألوان البطاقة" to { showAdminEditDesign = true }
                            )

                            adminOptionsList.chunked(2).forEach { rowItems ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    rowItems.forEach { (label, action) ->
                                        Button(
                                            onClick = action,
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f)),
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(text = label, fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    if (rowItems.size < 2) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- POPUP DIALOGS & SHEET SIMULATIONS ---

    // Instant Chat Dialog with Provider
    if (showInstantChatDialog) {
        Dialog(onDismissRequest = { showInstantChatDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, themeColors.accent),
                modifier = Modifier.padding(12.dp).fillMaxWidth().height(450.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.size(8.dp).background(Color.Green, CircleShape))
                            Text(text = "محادثة فورية: ${provider.name}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        IconButton(onClick = { showInstantChatDialog = false }, modifier = Modifier.size(24.dp)) {
                            Text("❌", color = Color.White, fontSize = 12.sp)
                        }
                    }
                    
                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 8.dp))
                    
                    // Messages Column
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Box(
                                modifier = Modifier
                                    .background(Color.Gray.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = "مرحباً بك! أنا ${provider.name} متواجد لخدمتك في ${provider.area}. كيف يمكنني تلبية طلبك اليوم؟ يمكنك حجز موعد مباشر أو الاستفسار.",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                        }

                        items(chatHistoryList) { (msg, isUser) ->
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (isUser) themeColors.accent.copy(alpha = 0.25f) else Color.Gray.copy(alpha = 0.15f),
                                            RoundedCornerShape(10.dp)
                                        )
                                        .padding(8.dp)
                                ) {
                                    Text(text = msg, color = Color.White, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    // Input text row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = chatInputText,
                            onValueChange = { chatInputText = it },
                            placeholder = { Text("اكتب رسالتك...", fontSize = 11.sp, color = Color.LightGray) },
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                        IconButton(
                            onClick = {
                                if (chatInputText.trim().isNotEmpty()) {
                                    val userMsg = chatInputText
                                    chatHistoryList = chatHistoryList + (userMsg to true)
                                    chatInputText = ""
                                    
                                    val reply = when {
                                        userMsg.contains("سعر") || userMsg.contains("بكم") || userMsg.contains("تكلفة") -> 
                                            "أهلاً بك. يمكنك مناقشة التكاليف والاتفاق عليها بدقة بعد المعاينة الميدانية والوقوف على طبيعة الخدمة المطلوبة."
                                        userMsg.contains("حجز") || userMsg.contains("موعد") || userMsg.contains("متى") ->
                                            "يمكنك استخدام زر '📅 حجز موعد خدمة فورية ومباشرة' المتواجد على بطاقتي لإرسال طلب حجز رسمي، وسأقوم بالاتصال بك فوراً لتأكيده."
                                        userMsg.contains("رقم") || userMsg.contains("هاتف") ->
                                            "يمكنك الاتصال بي مباشرة على الرقم: ${provider.phone} أو عبر واتساب بالضغط على أيقونة الاتصال."
                                        else -> "أهلاً بك، يسعدني جداً تواصلك معي. تفضل بطرح تفاصيل مشكلتك وسأقوم بالرد عليك أو الاتصال بك لمساعدتك بأسرع وقت ممكن!"
                                    }
                                    
                                    coroutineScope.launch {
                                        delay(1000)
                                        chatHistoryList = chatHistoryList + (reply to false)
                                    }
                                }
                            },
                            modifier = Modifier.background(themeColors.accent, CircleShape).size(36.dp)
                        ) {
                            Text("↩️", fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }

    // Reviews List Dialog
    if (showReviewsListDialog) {
        Dialog(onDismissRequest = { showReviewsListDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, themeColors.accent),
                modifier = Modifier.padding(12.dp).fillMaxWidth().height(400.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "💬 آراء وتجارب العملاء لـ ${provider.name}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        IconButton(onClick = { showReviewsListDialog = false }, modifier = Modifier.size(24.dp)) {
                            Text("❌", color = Color.White, fontSize = 12.sp)
                        }
                    }
                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 8.dp))
                    
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val sampleReviews = emptyList<Pair<String, String>>()
                        if (sampleReviews.isEmpty()) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                    Text("لا توجد تجارب معتمدة مسجلة حالياً لهذا الفني. كن أول من يضيف تجربة!", color = Color.Gray, fontSize = 11.sp, textAlign = TextAlign.Center)
                                }
                            }
                        }
                        items(sampleReviews) { (text, author) ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.2f)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(text = author, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = text, fontSize = 11.sp, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Review Dialog
    if (showAddCommentDialog) {
        Dialog(onDismissRequest = { showAddCommentDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, themeColors.accent),
                modifier = Modifier.padding(12.dp).fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "✍️ أضف تعليق وتقييم جديد", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        IconButton(onClick = { showAddCommentDialog = false }, modifier = Modifier.size(24.dp)) {
                            Text("❌", color = Color.White, fontSize = 12.sp)
                        }
                    }
                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
                    
                    OutlinedTextField(
                        value = newCommentAuthor,
                        onValueChange = { newCommentAuthor = it },
                        label = { Text("اسمك الكريم", color = Color.LightGray, fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    
                    OutlinedTextField(
                        value = newCommentText,
                        onValueChange = { newCommentText = it },
                        label = { Text("اكتب تعليقك وتجربتك هنا بكل أمانة...", color = Color.LightGray, fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth().height(80.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    
                    Button(
                        onClick = {
                            if (newCommentAuthor.trim().isEmpty() || newCommentText.trim().isEmpty()) {
                                Toast.makeText(context, "الرجاء كتابة الاسم والتعليق أولاً", Toast.LENGTH_SHORT).show()
                            } else {
                                val updated = provider.copy(numReviews = provider.numReviews + 1)
                                viewModel.updateProviderEntity(updated)
                                Toast.makeText(context, "💖 شكراً لك! تم إرسال تعليقك وسيقوم المسؤول بمراجعته وتفعيله فوراً.", Toast.LENGTH_LONG).show()
                                showAddCommentDialog = false
                                newCommentText = ""
                                newCommentAuthor = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = "إرسال التعليق والتقييم ✍️", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }

    // --- ADMIN PERMISSIONS CONTROL DIALOGS ---

    // 1. Edit Name
    if (showAdminEditName) {
        Dialog(onDismissRequest = { showAdminEditName = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, themeColors.accent),
                modifier = Modifier.padding(12.dp).fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("📝 تعديل اسم مقدم الخدمة", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    OutlinedTextField(
                        value = adminNameInput,
                        onValueChange = { adminNameInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    Button(
                        onClick = {
                            viewModel.updateProviderEntity(provider.copy(name = adminNameInput))
                            showAdminEditName = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("حفظ الاسم الجديد", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // 2. Change Rating
    if (showAdminEditRating) {
        Dialog(onDismissRequest = { showAdminEditRating = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, themeColors.accent),
                modifier = Modifier.padding(12.dp).fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("⭐ تعديل التقييم وعدد المراجعات", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    
                    Text("التقييم الحالي: ${String.format("%.1f", adminRatingInput)}", color = Color.LightGray, fontSize = 11.sp)
                    Slider(
                        value = adminRatingInput,
                        onValueChange = { adminRatingInput = it },
                        valueRange = 1.0f..5.0f,
                        steps = 40
                    )
                    
                    OutlinedTextField(
                        value = adminReviewsCountInput.toString(),
                        onValueChange = { adminReviewsCountInput = it.toIntOrNull() ?: 0 },
                        label = { Text("عدد التقييمات", color = Color.LightGray, fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    
                    Button(
                        onClick = {
                            viewModel.updateProviderEntity(provider.copy(rating = adminRatingInput, numReviews = adminReviewsCountInput))
                            showAdminEditRating = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("حفظ التقييم الجديد", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // 3. Update Location
    if (showAdminEditLocation) {
        Dialog(onDismissRequest = { showAdminEditLocation = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, themeColors.accent),
                modifier = Modifier.padding(12.dp).fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("📍 تعديل موقع مقدم الخدمة", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    
                    OutlinedTextField(
                        value = adminAreaInput,
                        onValueChange = { adminAreaInput = it },
                        label = { Text("المحافظة/المدينة", color = Color.LightGray, fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    
                    OutlinedTextField(
                        value = adminNeighborhoodInput,
                        onValueChange = { adminNeighborhoodInput = it },
                        label = { Text("الحي/الحارة/الشارع", color = Color.LightGray, fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    
                    Button(
                        onClick = {
                            viewModel.updateProviderEntity(provider.copy(area = adminAreaInput, localNeighborhood = adminNeighborhoodInput))
                            showAdminEditLocation = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("حفظ الموقع الجديد", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // 4. Switch/Update Image
    // 4. Change Images (Profile Image & Cover Image)
    if (showAdminEditImage) {
        var adminCoverImageInput by remember { mutableStateOf(provider.coverImage) }
        Dialog(onDismissRequest = { showAdminEditImage = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, themeColors.accent),
                modifier = Modifier.padding(12.dp).fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("🖼️ إدارة الصور (الشخصية والغلاف)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    
                    OutlinedTextField(
                        value = adminProfileImageInput,
                        onValueChange = { adminProfileImageInput = it },
                        label = { Text("رابط الصورة الشخصية / الشعار (URL أو Base64)", color = Color.LightGray, fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    OutlinedTextField(
                        value = adminCoverImageInput,
                        onValueChange = { adminCoverImageInput = it },
                        label = { Text("رابط صورة الغلاف العلوي (URL أو Base64)", color = Color.LightGray, fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    
                    Text("صوَر شخصية نموذجية سريعة:", color = Color.White, fontSize = 11.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf(
                            "https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?w=150" to "نسائي",
                            "https://images.unsplash.com/photo-1560250097-0b93528c311a?w=150" to "رجالي"
                        ).forEach { (url, label) ->
                            Button(
                                onClick = { adminProfileImageInput = url },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(label, fontSize = 9.sp, color = Color.White)
                            }
                        }
                    }
                    
                    Button(
                        onClick = {
                            viewModel.updateProviderEntity(provider.copy(
                                profileImage = adminProfileImageInput,
                                coverImage = adminCoverImageInput
                            ))
                            showAdminEditImage = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("حفظ الصور المحدثة", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // 5. Customize Buttons
    if (showAdminEditButtons) {
        Dialog(onDismissRequest = { showAdminEditButtons = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, themeColors.accent),
                modifier = Modifier.padding(12.dp).fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("🔘 صلاحيات وتخصيص أزرار التفاعل بالبطاقة", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📩 زر المراسلة والدردشة الفورية", color = Color.White, fontSize = 11.sp)
                        Switch(
                            checked = settingsState.showInstantChatButton,
                            onCheckedChange = { viewModel.saveCustomSettingsState(settingsState.copy(showInstantChatButton = it)) }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📞 زر الاتصال المباشر", color = Color.White, fontSize = 11.sp)
                        Switch(
                            checked = settingsState.showCallButton,
                            onCheckedChange = { viewModel.saveCustomSettingsState(settingsState.copy(showCallButton = it)) }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🎙️ زر المكالمة الصوتية داخل التطبيق", color = Color.White, fontSize = 11.sp)
                        Switch(
                            checked = settingsState.showVoiceCallButton,
                            onCheckedChange = { viewModel.saveCustomSettingsState(settingsState.copy(showVoiceCallButton = it)) }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("💬 زر الآراء وتقييمات العملاء", color = Color.White, fontSize = 11.sp)
                        Switch(
                            checked = settingsState.showReviewButton,
                            onCheckedChange = { viewModel.saveCustomSettingsState(settingsState.copy(showReviewButton = it)) }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🚨 زر تقديم البلاغات والشكاوى", color = Color.White, fontSize = 11.sp)
                        Switch(
                            checked = settingsState.showReportButton,
                            onCheckedChange = { viewModel.saveCustomSettingsState(settingsState.copy(showReportButton = it)) }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📅 زر حجز المواعيد والخدمات", color = Color.White, fontSize = 11.sp)
                        Switch(
                            checked = settingsState.showBookButton,
                            onCheckedChange = { viewModel.saveCustomSettingsState(settingsState.copy(showBookButton = it)) }
                        )
                    }
                    
                    Button(
                        onClick = { showAdminEditButtons = false },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("حفظ وتأكيد الأزرار", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // 6. Edit Texts & Headings
    if (showAdminEditTexts) {
        Dialog(onDismissRequest = { showAdminEditTexts = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, themeColors.accent),
                modifier = Modifier.padding(12.dp).fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("✍️ تعديل النصوص والعناوين العامة", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    
                    var labelNameInput by remember { mutableStateOf(settingsState.bookingLabelName) }
                    var termsInput by remember { mutableStateOf(settingsState.bookingTerms) }
                    
                    OutlinedTextField(
                        value = labelNameInput,
                        onValueChange = { labelNameInput = it },
                        label = { Text("عنوان حقل الاسم بالاستمارة", color = Color.LightGray, fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    OutlinedTextField(
                        value = termsInput,
                        onValueChange = { termsInput = it },
                        label = { Text("شروط الحجز العامة للأدمن", color = Color.LightGray, fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth().height(80.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    
                    Button(
                        onClick = {
                            viewModel.saveCustomSettingsState(settingsState.copy(
                                bookingLabelName = labelNameInput,
                                bookingTerms = termsInput
                            ))
                            showAdminEditTexts = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("حفظ العناوين والنصوص", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // 7. Change Card Colors & Design
    if (showAdminEditDesign) {
        Dialog(onDismissRequest = { showAdminEditDesign = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, themeColors.accent),
                modifier = Modifier.padding(12.dp).fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("🎨 تخصيص مظهر وتصميم بطاقات الحرفيين", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    
                    var cardBgHexInput by remember { mutableStateOf(settingsState.cardBackgroundHex) }
                    var nameHexInput by remember { mutableStateOf(settingsState.providerNameColorHex) }
                    
                    OutlinedTextField(
                        value = cardBgHexInput,
                        onValueChange = { cardBgHexInput = it },
                        label = { Text("خلفية البطاقة (Hex)", color = Color.LightGray, fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    
                    OutlinedTextField(
                        value = nameHexInput,
                        onValueChange = { nameHexInput = it },
                        label = { Text("لون خط الاسم (Hex)", color = Color.LightGray, fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    Text("اختر نمط سريع من الألوان الجذابة:", color = Color.White, fontSize = 11.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf(
                            ("فحمي داكن" to "#1E293B") to "#FFFFFF",
                            ("ذهبي ملكي" to "#1A1A1A") to "#FFD700",
                            ("أزرق زجاجي" to "#0F172A") to "#60A5FA"
                        ).forEach { (style, nameCol) ->
                            Button(
                                onClick = {
                                    cardBgHexInput = style.second
                                    nameHexInput = nameCol
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(style.first, fontSize = 9.sp, color = Color.White)
                            }
                        }
                    }
                    
                    Button(
                        onClick = {
                            viewModel.saveCustomSettingsState(settingsState.copy(
                                cardBackgroundHex = cardBgHexInput,
                                providerNameColorHex = nameHexInput
                            ))
                            showAdminEditDesign = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("حفظ التصميم والألوان", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showReportDialog) {
        Dialog(onDismissRequest = { showReportDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color.Red),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("📢 تقديم بلاغ أو شكوى ضد الفني", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("الرجاء كتابة سبب الإنزعاج أو الشكوى بالتفصيل وسنقوم بمراجعتها فوراً:", fontSize = 11.sp, color = themeColors.textSecondary)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = selectedReportReason,
                        onValueChange = { selectedReportReason = it },
                        placeholder = { Text("مثال: سعر مرتفع، تأخير في الموعد، سوء معاملة...", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { showReportDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                        ) {
                            Text("إلغاء", color = Color.White)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                viewModel.sendReport(provider.id, provider.name, "مستخدم مجهول", selectedReportReason)
                                showReportDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                        ) {
                            Text("إرسال الشكوى", color = Color.White)
                        }
                    }
                }
            }
        }
    }

    if (showDetailsDialog) {
        Dialog(onDismissRequest = { showDetailsDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.5f)),
                modifier = Modifier.padding(12.dp).fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("🔍 تفاصيل وملف الفني المهني", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    
                    // Profile/Cover header representation
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(Color.Black)
                                .border(2.dp, themeColors.accent, CircleShape)
                        ) {
                            val base64Bitmap = remember(provider.profileImage) {
                                if (provider.profileImage.isNotEmpty() && !provider.profileImage.startsWith("http") && !provider.profileImage.startsWith("content")) {
                                    try {
                                        val cleanBase64 = if (provider.profileImage.contains(",")) provider.profileImage.substringAfter(",") else provider.profileImage
                                        val bytes = android.util.Base64.decode(cleanBase64, android.util.Base64.DEFAULT)
                                        android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                    } catch (e: Exception) {
                                        null
                                    }
                                } else {
                                    null
                                }
                            }

                            if (base64Bitmap != null) {
                                androidx.compose.foundation.Image(
                                    bitmap = base64Bitmap.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            } else if (provider.profileImage.isNotEmpty()) {
                                AsyncImage(
                                    model = provider.profileImage,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxSize().background(themeColors.accent),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = provider.name.take(1),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 24.sp,
                                        color = Color.Black
                                    )
                                }
                            }
                        }
                        
                        Column {
                            Text(provider.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = themeColors.accent, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("${provider.rating} (${provider.numReviews} تقييم)", fontSize = 11.sp, color = Color.White)
                            }
                        }
                    }

                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))

                    // Badges section
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (provider.isVip) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFD97706))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text("👑 VIP ذهبي", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                        }
                        if (provider.isVerified) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF3B82F6))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text("🔵 معتمد وموثق", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                        if (provider.isRecommended) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF10B981))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text("🟢 موصى به", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }

                    // Structured Details Cards
                    val detailCatName = if (provider.categoryId == "other" && provider.customCategoryName.isNotEmpty()) provider.customCategoryName else (categories.find { it.id == provider.categoryId }?.name ?: "صيانة فنية")
                    val detailProfessionText = provider.profession.ifEmpty { detailCatName }
                    val detailSpecializationText = provider.specialization.ifEmpty { "متخصص معتمد" }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("💼 المهنة والوظيفة:", fontSize = 12.sp, color = themeColors.textSecondary)
                            Text(detailProfessionText, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("🎓 التخصص المهني:", fontSize = 12.sp, color = themeColors.textSecondary)
                            Text(detailSpecializationText, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("📍 المحافظة والمنطقة:", fontSize = 12.sp, color = themeColors.textSecondary)
                            Text(provider.area, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("🏡 الحي / الحارة:", fontSize = 12.sp, color = themeColors.textSecondary)
                            Text(provider.localNeighborhood, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("📞 رقم الاتصال:", fontSize = 12.sp, color = themeColors.textSecondary)
                            Text(provider.phone, fontSize = 12.sp, color = themeColors.accent, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("🔧 حالة التوفر الحالية:", fontSize = 12.sp, color = themeColors.textSecondary)
                            Text(
                                text = if (provider.isAvailable) "متاح للعمل الفوري 🟢" else "مشغول حالياً 🔴",
                                fontSize = 12.sp,
                                color = if (provider.isAvailable) Color.Green else Color.Red,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("⭐ النقاط والتقييمات التراكمية:", fontSize = 12.sp, color = themeColors.textSecondary)
                            Text("${provider.points} نقطة مهنية", fontSize = 12.sp, color = Color.Yellow, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Dynamic Custom Profile Tabs
                    val customTabsList by viewModel.customProfileTabs.collectAsState()
                    val activeProviderTabs = remember(customTabsList) {
                        customTabsList.filter { it.isEnabled && (it.targetType == "ALL" || it.targetType == "PROVIDERS") }
                    }
                    if (activeProviderTabs.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("📑 التبويبات المخصصة للملف:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                        activeProviderTabs.forEach { tab ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                                border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                            ) {
                                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("${tab.icon} ${tab.title}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    if (tab.contentHtmlOrText.isNotEmpty()) {
                                        Text(tab.contentHtmlOrText, fontSize = 10.sp, color = themeColors.textSecondary)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { showDetailsDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("إغلاق التفاصيل ❌", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showGuestRegisterDialogForBooking) {
        GuestRegistrationDialog(
            viewModel = viewModel,
            themeColors = themeColors,
            onDismiss = { showGuestRegisterDialogForBooking = false },
            onRegisterCompleted = { name, phone, residence, password ->
                viewModel.registerGuestUser(context, name, phone, residence, password)
                showGuestRegisterDialogForBooking = false
                showBookingDialog = true
            }
        )
    }

    // ------ Dynamic Booking Form Layout Dialogs ------
    if (showBookingDialog) {
        Dialog(onDismissRequest = { showBookingDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, themeColors.accent),
                modifier = Modifier.padding(12.dp).fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("📅 استمارة حجز فني: ${provider.name}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    
                    if (bookingFormSubmittedOnce && bookingFormMissingFields.isNotEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFEF4444).copy(alpha = 0.15f)),
                            border = BorderStroke(1.dp, Color(0xFFEF4444)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("⚠️", fontSize = 14.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("يرجى إكمال وتصحيح الحقول المطلوبة لتأكيد حجزك:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                bookingFormMissingFields.forEach { field ->
                                    Text("• $field", fontSize = 10.sp, color = Color.White)
                                }
                            }
                        }
                    }

                    // Display admin customizable booking terms/shuroot
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.Yellow.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("⚠️ شروط وطريقة الحجز الموثقة:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Yellow)
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = settingsState.bookingTerms,
                                fontSize = 10.sp,
                                color = Color.LightGray,
                                lineHeight = 14.sp
                            )
                        }
                    }

                    OutlinedTextField(
                        value = customerNameInput,
                        onValueChange = { customerNameInput = it },
                        label = { Text("${settingsState.bookingLabelName} *", color = themeColors.textSecondary, fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        isError = bookingFormSubmittedOnce && customerNameInput.trim().isEmpty(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        leadingIcon = if (settingsState.allowTextToSpeechBookingForm) {
                            {
                                IconButton(onClick = { VoiceManager.onSpeak?.invoke(customerNameInput.ifBlank { "الاسم" }) }) {
                                    Text("🔊", fontSize = 16.sp)
                                }
                            }
                        } else null,
                        trailingIcon = if (settingsState.allowVoiceInputBookingForm) {
                            {
                                IconButton(onClick = {
                                    VoiceManager.onHear?.invoke { spokenText -> customerNameInput = spokenText }
                                }) {
                                    Text("🎙️", fontSize = 16.sp)
                                }
                            }
                        } else null
                    )

                    OutlinedTextField(
                        value = customerPhoneInput,
                        onValueChange = { customerPhoneInput = it },
                        label = { Text("${settingsState.bookingLabelPhone} *", color = themeColors.textSecondary, fontSize = 11.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        isError = bookingFormSubmittedOnce && (customerPhoneInput.trim().isEmpty() || !customerPhoneInput.trim().replace(" ", "").replace("+", "").let { p ->
                            p.length == 9 && (p.startsWith("77") || p.startsWith("73") || p.startsWith("71") || p.startsWith("70") || p.startsWith("78"))
                        }),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        leadingIcon = if (settingsState.allowTextToSpeechBookingForm) {
                            {
                                IconButton(onClick = { VoiceManager.onSpeak?.invoke(customerPhoneInput.ifBlank { "رقم الهاتف" }) }) {
                                    Text("🔊", fontSize = 16.sp)
                                }
                            }
                        } else null,
                        trailingIcon = if (settingsState.allowVoiceInputBookingForm) {
                            {
                                IconButton(onClick = {
                                    VoiceManager.onHear?.invoke { spokenText -> customerPhoneInput = spokenText }
                                }) {
                                    Text("🎙️", fontSize = 16.sp)
                                }
                            }
                        } else null
                    )

                    OutlinedTextField(
                        value = customerAreaInput,
                        onValueChange = { customerAreaInput = it },
                        label = { Text("${settingsState.bookingLabelArea} *", color = themeColors.textSecondary, fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        isError = bookingFormSubmittedOnce && customerAreaInput.trim().isEmpty(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        leadingIcon = if (settingsState.allowTextToSpeechBookingForm) {
                            {
                                IconButton(onClick = { VoiceManager.onSpeak?.invoke(customerAreaInput.ifBlank { "المنطقة" }) }) {
                                    Text("🔊", fontSize = 16.sp)
                                }
                            }
                        } else null,
                        trailingIcon = if (settingsState.allowVoiceInputBookingForm) {
                            {
                                IconButton(onClick = {
                                    VoiceManager.onHear?.invoke { spokenText -> customerAreaInput = spokenText }
                                }) {
                                    Text("🎙️", fontSize = 16.sp)
                                }
                            }
                        } else null
                    )

                    // Dropdown for selecting service type
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedServiceDropdown,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("نوع الخدمة المطلوبة", color = themeColors.textSecondary, fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth().clickable { serviceDropdownExpanded = true },
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                            trailingIcon = {
                                IconButton(onClick = { serviceDropdownExpanded = true }) {
                                    Text("▼", color = Color.White, fontSize = 12.sp)
                                }
                            }
                        )
                        DropdownMenu(
                            expanded = serviceDropdownExpanded,
                            onDismissRequest = { serviceDropdownExpanded = false },
                            modifier = Modifier.background(Color(0xFF1E293B)).fillMaxWidth(0.8f)
                        ) {
                            val services = listOf(
                                "صيانة أعطال عامة",
                                "تركيب وتهيئة أجهزة جديدة",
                                "فحص دوري ومعاينة فنية",
                                "إصلاح عاجل وطوارئ",
                                "تأسيس وتشطيب متكامل",
                                "أخرى (اكتب في الوصف أدناه)"
                            )
                            services.forEach { s ->
                                DropdownMenuItem(
                                    text = { Text(s, color = Color.White, fontSize = 12.sp) },
                                    onClick = {
                                        selectedServiceDropdown = s
                                        serviceDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Date Picker Input
                    val context = LocalContext.current
                    val calendar = java.util.Calendar.getInstance()
                    val datePickerDialog = remember(context) {
                        android.app.DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                bookingDateInput = "$year/${month + 1}/$dayOfMonth"
                            },
                            calendar.get(java.util.Calendar.YEAR),
                            calendar.get(java.util.Calendar.MONTH),
                            calendar.get(java.util.Calendar.DAY_OF_MONTH)
                        )
                    }

                    OutlinedTextField(
                        value = bookingDateInput,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("تاريخ الحجز المفضل", color = themeColors.textSecondary, fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth().clickable { datePickerDialog.show() },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        trailingIcon = {
                            IconButton(onClick = { datePickerDialog.show() }) {
                                Text("📅", fontSize = 16.sp)
                            }
                        }
                    )

                    // Time Picker Input
                    val timePickerDialog = remember(context) {
                        android.app.TimePickerDialog(
                            context,
                            { _, hourOfDay, minute ->
                                val amPm = if (hourOfDay < 12) "ص" else "م"
                                val hour = if (hourOfDay % 12 == 0) 12 else hourOfDay % 12
                                val formattedMin = String.format("%02d", minute)
                                bookingTimeInput = "$hour:$formattedMin $amPm"
                            },
                            calendar.get(java.util.Calendar.HOUR_OF_DAY),
                            calendar.get(java.util.Calendar.MINUTE),
                            false
                        )
                    }

                    OutlinedTextField(
                        value = bookingTimeInput,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("وقت الحجز المفضل", color = themeColors.textSecondary, fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth().clickable { timePickerDialog.show() },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        trailingIcon = {
                            IconButton(onClick = { timePickerDialog.show() }) {
                                Text("🕒", fontSize = 16.sp)
                            }
                        }
                    )

                    OutlinedTextField(
                        value = customerServiceInput,
                        onValueChange = { customerServiceInput = it },
                        label = { Text("وصف المشكلة بالتفصيل وملاحظاتك *", color = themeColors.textSecondary, fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        isError = bookingFormSubmittedOnce && customerServiceInput.trim().isEmpty(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        leadingIcon = if (settingsState.allowTextToSpeechBookingForm) {
                            {
                                IconButton(onClick = { VoiceManager.onSpeak?.invoke(customerServiceInput.ifBlank { "الوصف" }) }) {
                                    Text("🔊", fontSize = 16.sp)
                                }
                            }
                        } else null,
                        trailingIcon = if (settingsState.allowVoiceInputBookingForm) {
                            {
                                IconButton(onClick = {
                                    VoiceManager.onHear?.invoke { spokenText -> customerServiceInput = spokenText }
                                }) {
                                    Text("🎙️", fontSize = 16.sp)
                                }
                            }
                        } else null
                    )

                    OutlinedTextField(
                        value = bookingCouponCodeInput,
                        onValueChange = { bookingCouponCodeInput = it },
                        label = { Text("رمز الكوبون للتخفيض أو الشحن (اختياري)", color = themeColors.textSecondary, fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        leadingIcon = {
                            Text("🎫", fontSize = 16.sp)
                        }
                    )

                    OutlinedTextField(
                        value = bookingCustomIdInput,
                        onValueChange = { bookingCustomIdInput = it },
                        label = { Text("🆔 معرف/رقم الحجز المخصص (اختياري - اتركه فارغاً للتوليد التلقائي)", color = themeColors.textSecondary, fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        leadingIcon = {
                            Text("🆔", fontSize = 16.sp)
                        }
                    )

                    OutlinedTextField(
                        value = bookingPinCodeInput,
                        onValueChange = { bookingPinCodeInput = it },
                        label = { Text("🔑 كلمة مرور سرية لحفظ وتأمين الحجز (مطلوب) *", color = themeColors.textSecondary, fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        isError = bookingFormSubmittedOnce && bookingPinCodeInput.trim().isEmpty(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        leadingIcon = {
                            Text("🔑", fontSize = 16.sp)
                        }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val cleanName = customerNameInput.trim()
                                val cleanPhone = customerPhoneInput.trim().replace(" ", "").replace("+", "")
                                val cleanArea = customerAreaInput.trim()
                                val cleanService = customerServiceInput.trim()
                                val cleanPin = bookingPinCodeInput.trim()

                                val isValidYemeniPhone = cleanPhone.length == 9 && (
                                    cleanPhone.startsWith("77") || 
                                    cleanPhone.startsWith("73") || 
                                    cleanPhone.startsWith("71") || 
                                    cleanPhone.startsWith("70") || 
                                    cleanPhone.startsWith("78")
                                )

                                val missing = mutableListOf<String>()
                                if (cleanName.isEmpty()) missing.add("الاسم الثلاثي بالكامل")
                                if (cleanPhone.isEmpty()) {
                                    missing.add("رقم الهاتف")
                                } else if (!isValidYemeniPhone) {
                                    missing.add("رقم الهاتف اليمني غير صالح (يجب أن يتكون من 9 أرقام ويبدأ بـ 77، 73، 71، 70، 78)")
                                }
                                if (cleanArea.isEmpty()) missing.add("الحي والشارع ومكان السكن (العنوان)")
                                if (cleanService.isEmpty()) missing.add("تفاصيل ومعلومات المشكلة (الوصف)")
                                if (cleanPin.isEmpty()) missing.add("كلمة المرور السرية للحجز")

                                if (missing.isNotEmpty()) {
                                    bookingFormSubmittedOnce = true
                                    bookingFormMissingFields = missing
                                    Toast.makeText(context, "⚠️ هناك حقول مطلوبة أو غير صحيحة!", Toast.LENGTH_LONG).show()
                                } else {
                                    bookingFormSubmittedOnce = false
                                    bookingFormMissingFields = emptyList()
                                    showBookingDialog = false
                                    showBookingConfirmDialog = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("تأكيد الحجز", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        Button(
                            onClick = {
                                bookingFormSubmittedOnce = false
                                bookingFormMissingFields = emptyList()
                                showBookingDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("إلغاء الحجز", color = Color.White, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }

    if (showBookingConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showBookingConfirmDialog = false },
            containerColor = Color(0xFF1E293B),
            title = { Text("📋 هل كافة مدخلات الحجز صحيحة ودقيقة؟", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("📍 تفاصيل طلب الحجز لمراجعتها قبل الإرسال للشبكة:", fontSize = 11.sp, color = themeColors.accent)
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("• الاسم: $customerNameInput", color = Color.White, fontSize = 11.sp)
                        Text("• رقم الهاتف: $customerPhoneInput", color = Color.White, fontSize = 11.sp)
                        Text("• منطقة السكن والحي: $customerAreaInput", color = Color.White, fontSize = 11.sp)
                        Text("• نوع الخدمة: $selectedServiceDropdown", color = Color.Yellow, fontSize = 11.sp)
                        Text("• تاريخ الحجز: $bookingDateInput", color = Color.White, fontSize = 11.sp)
                        Text("• وقت الحجز: $bookingTimeInput", color = Color.White, fontSize = 11.sp)
                        Text("• تفاصيل المشكلة: $customerServiceInput", color = Color.LightGray, fontSize = 11.sp)
                    }

                    Text("• الفني المسؤول: ${provider.name}", color = Color.White, fontSize = 11.sp)
                    Text("• التكاليف المتوقعة: يتم الاتفاق المباشر عليها مع الفني المعتمد بعد المعاينة الميدانية.", color = Color.Green, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addBooking(
                            name = customerNameInput,
                            phone = customerPhoneInput,
                            area = customerAreaInput,
                            serviceType = "$selectedServiceDropdown - $customerServiceInput",
                            providerId = provider.id,
                            providerName = provider.name,
                            dateString = bookingDateInput,
                            timeString = bookingTimeInput,
                            couponCode = bookingCouponCodeInput,
                            pinCode = bookingPinCodeInput,
                            customBookingId = bookingCustomIdInput,
                            customPassword = bookingPinCodeInput
                        )
                        bookingCouponCodeInput = ""
                        bookingPinCodeInput = ""
                        bookingCustomIdInput = ""
                        showBookingConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("تأكيد وإرسال طلب الحجز", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Button(
                    onClick = { showBookingConfirmDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("تعديل الاستمارة", color = Color.White, fontSize = 11.sp)
                }
            }
        )
    }
}

fun convertGenericUriToBase64(context: android.content.Context, uri: android.net.Uri): String {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return ""
        val bytes = inputStream.readBytes()
        inputStream.close()
        android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT)
    } catch (e: Exception) { "" }
}

fun convertBitmapToBase64(bitmap: android.graphics.Bitmap): String {
    return try {
        val reqWidth = 220
        val reqHeight = 220
        val scaledBitmap = if (bitmap.width > reqWidth || bitmap.height > reqHeight) {
            val ratio = Math.min(reqWidth.toFloat() / bitmap.width, reqHeight.toFloat() / bitmap.height)
            android.graphics.Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * ratio).toInt(),
                (bitmap.height * ratio).toInt(),
                true
            )
        } else {
            bitmap
        }
        val outputStream = java.io.ByteArrayOutputStream()
        scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 55, outputStream)
        val bytes = outputStream.toByteArray()
        if (scaledBitmap != bitmap) {
            scaledBitmap.recycle()
        }
        android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT)
    } catch (e: Exception) { "" }
}

fun compressAndResizeImageUri(context: android.content.Context, uri: android.net.Uri, maxDimension: Int = 800, quality: Int = 70): String {
    return try {
        val contentResolver = context.contentResolver
        val inputStreamForBounds = contentResolver.openInputStream(uri) ?: return ""
        val options = android.graphics.BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        android.graphics.BitmapFactory.decodeStream(inputStreamForBounds, null, options)
        inputStreamForBounds.close()

        var inSampleSize = 1
        if (options.outHeight > maxDimension || options.outWidth > maxDimension) {
            val halfHeight = options.outHeight / 2
            val halfWidth = options.outWidth / 2
            while ((halfHeight / inSampleSize) >= maxDimension && (halfWidth / inSampleSize) >= maxDimension) {
                inSampleSize *= 2
            }
        }

        val finalOptions = android.graphics.BitmapFactory.Options().apply {
            inSampleSize = inSampleSize
        }
        val inputStreamForDecode = contentResolver.openInputStream(uri) ?: return ""
        val decodedBitmap = android.graphics.BitmapFactory.decodeStream(inputStreamForDecode, null, finalOptions)
        inputStreamForDecode.close()

        if (decodedBitmap != null) {
            val scaledBitmap = if (decodedBitmap.width > maxDimension || decodedBitmap.height > maxDimension) {
                val ratio = Math.min(maxDimension.toFloat() / decodedBitmap.width, maxDimension.toFloat() / decodedBitmap.height)
                android.graphics.Bitmap.createScaledBitmap(
                    decodedBitmap,
                    (decodedBitmap.width * ratio).toInt(),
                    (decodedBitmap.height * ratio).toInt(),
                    true
                )
            } else {
                decodedBitmap
            }

            val outputStream = java.io.ByteArrayOutputStream()
            scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, outputStream)
            val bytes = outputStream.toByteArray()
            if (scaledBitmap != decodedBitmap) {
                scaledBitmap.recycle()
            }
            decodedBitmap.recycle()
            android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        } else ""
    } catch (e: Exception) {
        e.printStackTrace()
        ""
    }
}

/* ClientPersonalAccountDashboard has been moved to com.example.ui.screens.dashboard.ClientPersonalAccountDashboard */
// ------ Provider Registration Form Layout ------
fun Color.luminance() : Float {
    return (0.2126f * this.red + 0.7152f * this.green + 0.0722f * this.blue)
}

/* ProviderRegisterFormLayout has been moved to com.example.ui.screens.register.ProviderRegisterFormLayout */
// ------ Option Checkbox Card Component ------
@Composable
fun OptionCheckboxCard(
    title: String,
    subtitle: String? = null,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    themeColors: VisualThemePalette
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isChecked) themeColors.accent.copy(alpha = 0.15f) else themeColors.surface
        ),
        border = BorderStroke(
            1.dp,
            if (isChecked) themeColors.accent else Color.Gray.copy(alpha = 0.25f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!isChecked) }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                if (!subtitle.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(subtitle, fontSize = 10.sp, color = Color.LightGray)
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(
                        if (isChecked) themeColors.accent else Color.Transparent,
                        RoundedCornerShape(6.dp)
                    )
                    .border(
                        1.5.dp,
                        if (isChecked) themeColors.accent else Color.Gray,
                        RoundedCornerShape(6.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isChecked) {
                    Text("☑", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                } else {
                    Text("☐", color = Color.Gray, fontSize = 14.sp)
                }
            }
        }
    }
}

// ------ Admin Panel Layout ------
/* OwnerBackdoorPanelLayout has been moved to com.example.ui.screens.admin.OwnerBackdoorPanelLayout */
// ------ Quick Reverse Marketplace Request Creation Dialog ------
@Composable
/* QuickServiceRequestDialog has been moved to com.example.ui.dialogs.QuickServiceRequestDialog */
/* AboutAppDialogView has been moved to com.example.ui.screens.about.AboutAppDialogView */
/* SmartAssistantDialogView has been moved to com.example.ui.screens.assistant.SmartAssistantDialogView */
/* ChatPanelDialogView and AboutAppScreenContent have been moved to com.example.ui.screens.chat.ChatPanelDialogView and com.example.ui.screens.about.AboutAppDialogView */
/* RealLeafletMapView has been moved to com.example.ui.screens.map.RealLeafletMapView */

/* JoinRequestStatusScreen has been moved to com.example.ui.screens.register.JoinRequestStatusScreen */
/* InAppVoiceCallDialog has been moved to com.example.ui.dialogs.InAppVoiceCallDialog */
/* GuestRegistrationDialog has been moved to com.example.ui.screens.register.GuestRegistrationDialog */
/* UserNotificationsDialogView has been moved to com.example.ui.screens.notifications.UserNotificationsDialogView */
fun isMoreThan8HoursBefore(dateStr: String, timeStr: String): Boolean {
    return try {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US)
        val bookingDate = sdf.parse("$dateStr $timeStr")
        if (bookingDate != null) {
            val diffMs = bookingDate.time - System.currentTimeMillis()
            val diffHours = diffMs / (1000 * 60 * 60)
            diffHours >= 8
        } else {
            true
        }
    } catch (e: Exception) {
        true
    }
}

/* UserSubmitPaymentProofDialog has been moved to com.example.ui.dialogs.UserSubmitPaymentProofDialog */
/* OrdersScreenLayout has been moved to com.example.ui.screens.bookings.OrdersScreenLayout */
/* StoreOwnerDashboardLayout has been moved to com.example.ui.screens.dashboard.StoreOwnerDashboardLayout */
/* PropertyOwnerDashboardLayout has been moved to com.example.ui.screens.dashboard.PropertyOwnerDashboardLayout */
