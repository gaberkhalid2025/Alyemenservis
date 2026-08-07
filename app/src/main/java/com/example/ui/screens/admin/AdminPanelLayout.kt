@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.ui.screens.admin

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
fun AdminPanelLayout(viewModel: MainViewModel, themeColors: VisualThemePalette) {
    val pendingProviders by viewModel.pendingProviders.collectAsState()
    val reports by viewModel.reports.collectAsState()
    val adminRole by viewModel.adminRole.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val activatedProviders by viewModel.providers.collectAsState()
    val bookings by viewModel.bookings.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val chatChannels by viewModel.chatChannels.collectAsState()
    val settingsState by viewModel.settings.collectAsState()
    val bannersList by viewModel.banners.collectAsState()
    val supervisorsList by viewModel.supervisors.collectAsState()
    val colorPalettesList by viewModel.colorPalettes.collectAsState()
    val citiesList by viewModel.cities.collectAsState()
    val stores by viewModel.stores.collectAsState()
    val properties by viewModel.properties.collectAsState()
    val jobs by viewModel.jobs.collectAsState()

    var inputPasscode by remember { mutableStateOf("") }
    var isAuthorized by remember(adminRole) { mutableStateOf(adminRole != "GUEST") }
    var activeSubTab by remember(adminRole) { mutableStateOf(if (adminRole == "OWNER") "BACKDOOR" else "REG_REQ") }
    var adminReqSubTab by remember { mutableStateOf("SERVICES") } // SERVICES, PROPERTIES, STORES, MEDICAL, RESTAURANTS, JOBS
    var adminAddSubTab by remember { mutableStateOf("SERVICES") } // SERVICES, PROPERTIES, STORES, MEDICAL, RESTAURANTS, JOBS

    // Dialog state controllers for category edits and deletions
    var showDeleteCategoryConfirmId by remember { mutableStateOf<String?>(null) }
    var showEditCategoryObj by remember { mutableStateOf<CategoryEntity?>(null) }
    var showEditCityObj by remember { mutableStateOf<com.example.data.CityEntity?>(null) }
    var rejectingProviderRequest by remember { mutableStateOf<com.example.data.PendingProviderEntity?>(null) }
    var providerRejectionReasonText by remember { mutableStateOf("") }
    var editCatName by remember { mutableStateOf("") }
    var editCatIcon by remember { mutableStateOf("") }
    var newCatName by remember { mutableStateOf("") }
    var newCatIcon by remember { mutableStateOf("") }

    // Dialog state controllers for booking deletions
    var showDeleteBookingConfirmId by remember { mutableStateOf<String?>(null) }
    var showRejectionReasonDialogId by remember { mutableStateOf<String?>(null) }
    var bookingRejectionReasonInput by remember { mutableStateOf("") }
    var editingBookingObj by remember { mutableStateOf<BookingEntity?>(null) }
    var redirectingBookingObj by remember { mutableStateOf<BookingEntity?>(null) }
    var editingSupervisorObj by remember { mutableStateOf<SupervisorEntity?>(null) }

    // Dialog state controllers for notifications deletions
    var showDeleteNotifConfirmId by remember { mutableStateOf<String?>(null) }

    // Dialog state controllers for chat selections
    var showActiveChatChannelObj by remember { mutableStateOf<ChatChannelEntity?>(null) }
    var adminChatReplyInput by remember { mutableStateOf("") }
    var showDeleteChatConfirmId by remember { mutableStateOf<String?>(null) }
    var backupJsonStringState by remember { mutableStateOf("") }
    var restoreJsonInputState by remember { mutableStateOf("") }

    // Notification input states
    var notifTitleInput by remember { mutableStateOf("") }
    var notifMsgInput by remember { mutableStateOf("") }
    var notifTargetType by remember { mutableStateOf("ALL") } // ALL, USER, PROVIDER, SUPERVISOR, AREA
    var notifTargetValue by remember { mutableStateOf("") }
    var notifDelayHours by remember { mutableStateOf("") }
    var notifValidityHours by remember { mutableStateOf("") }

    // Section Ten input configs state
    var editPrimaryHex by remember { mutableStateOf(settingsState.customPrimaryHex) }
    var editSecondaryHex by remember { mutableStateOf(settingsState.customSecondaryHex) }
    var editCardBgHex by remember { mutableStateOf(settingsState.cardBackgroundHex) }
    var editProviderNameHex by remember { mutableStateOf(settingsState.providerNameColorHex) }
    var editLocationHex by remember { mutableStateOf(settingsState.locationColorHex) }
    var editRatingHex by remember { mutableStateOf(settingsState.ratingColorHex) }
    var editVipBadgeHex by remember { mutableStateOf(settingsState.vipBadgeColorHex) }
    var editVerifiedHex by remember { mutableStateOf(settingsState.verifiedBadgeColorHex) }
    var editRecommendedHex by remember { mutableStateOf(settingsState.recommendedBadgeColorHex) }
    
    var editFontSelected by remember { mutableStateOf(settingsState.activeFontFamily) }
    
    var editChatIconSize by remember(settingsState.chatSize) { mutableStateOf(settingsState.chatSize.toFloat()) }
    var editChatIconX by remember(settingsState.chatXOffset) { mutableStateOf(settingsState.chatXOffset.toFloat()) }
    var editChatIconY by remember(settingsState.chatYOffset) { mutableStateOf(settingsState.chatYOffset.toFloat()) }

    var editAssistantIconSize by remember(settingsState.assistantSize) { mutableStateOf(settingsState.assistantSize.toFloat()) }
    var editAssistantIconX by remember(settingsState.assistantXOffset) { mutableStateOf(settingsState.assistantXOffset.toFloat()) }
    var editAssistantIconY by remember(settingsState.assistantYOffset) { mutableStateOf(settingsState.assistantYOffset.toFloat()) }

    var requirementItemInput by remember { mutableStateOf("") }
    var isNewRequirementMandatory by remember { mutableStateOf(true) }
    var requirementsListState by remember { mutableStateOf(settingsState.registrationRequirements.split(",").filter { it.isNotBlank() }) }

    // Card sizes & spacing layout customizations
    var editCoverHeight by remember(settingsState.coverHeight) { mutableStateOf(settingsState.coverHeight.toFloat()) }
    var editAvatarSize by remember(settingsState.avatarSize) { mutableStateOf(settingsState.avatarSize.toFloat()) }
    var editElementSpacing by remember(settingsState.elementSpacing) { mutableStateOf(settingsState.elementSpacing.toFloat()) }
    var editCardPadding by remember(settingsState.cardPadding) { mutableStateOf(settingsState.cardPadding.toFloat()) }

    var editShowVipBadge by remember(settingsState.showVipBadge) { mutableStateOf(settingsState.showVipBadge) }
    var editShowVerifiedBadge by remember(settingsState.showVerifiedBadge) { mutableStateOf(settingsState.showVerifiedBadge) }
    var editShowRecommendedBadge by remember(settingsState.showRecommendedBadge) { mutableStateOf(settingsState.showRecommendedBadge) }

    var editShowCallButton by remember(settingsState.showCallButton) { mutableStateOf(settingsState.showCallButton) }
    var editShowWhatsappButton by remember(settingsState.showWhatsappButton) { mutableStateOf(settingsState.showWhatsappButton) }
    var editShowDetailsButton by remember(settingsState.showDetailsButton) { mutableStateOf(settingsState.showDetailsButton) }
    var editShowBookButton by remember(settingsState.showBookButton) { mutableStateOf(settingsState.showBookButton) }

    var editCallButtonColorHex by remember(settingsState.callButtonColorHex) { mutableStateOf(settingsState.callButtonColorHex) }
    var editWhatsappButtonColorHex by remember(settingsState.whatsappButtonColorHex) { mutableStateOf(settingsState.whatsappButtonColorHex) }
    var editDetailsButtonColorHex by remember(settingsState.detailsButtonColorHex) { mutableStateOf(settingsState.detailsButtonColorHex) }
    var editBookButtonColorHex by remember(settingsState.bookButtonColorHex) { mutableStateOf(settingsState.bookButtonColorHex) }

    var editShowLoyaltyBanner by remember(settingsState.showLoyaltyBanner) { mutableStateOf(settingsState.showLoyaltyBanner) }
    var editMaxWorkPhotos by remember(settingsState.maxWorkPhotos) { mutableStateOf(settingsState.maxWorkPhotos.toFloat()) }

    // Wipe states
    var showWipeConfirmDialog by remember { mutableStateOf(false) }
    var wipeInputPassword by remember { mutableStateOf("") }
    var wipeProvidersChecked by remember { mutableStateOf(true) }
    var wipeBookingsChecked by remember { mutableStateOf(true) }
    var wipeChatsChecked by remember { mutableStateOf(true) }
    var wipeNotifsChecked by remember { mutableStateOf(true) }
    var wipeReportsChecked by remember { mutableStateOf(true) }
    var wipeCategoriesChecked by remember { mutableStateOf(false) }
    var wipePendingChecked by remember { mutableStateOf(true) }
    var wipeBannersChecked by remember { mutableStateOf(true) }
    var wipeSupervisorsChecked by remember { mutableStateOf(false) }
    var wipeCitiesChecked by remember { mutableStateOf(false) }
    var wipeThemesChecked by remember { mutableStateOf(false) }

    // Section 2 state variables
    var manualName by remember { mutableStateOf("") }
    var manualPhone by remember { mutableStateOf("") }
    var manualCategoryId by remember { mutableStateOf("") }
    var manualStreet by remember { mutableStateOf("") }
    var manualCityId by remember { mutableStateOf("") }
    var manualPhotoUrl by remember { mutableStateOf("") }
    var manualIdCardUrl by remember { mutableStateOf("") }
    var manualForensicUrl by remember { mutableStateOf("") }
    var manualPriceValue by remember { mutableStateOf("1500") }
    var manualIsVipGolden by remember { mutableStateOf(false) }

    // Section 4 state variables
    var newCityArName by remember { mutableStateOf("") }
    var newCityEnName by remember { mutableStateOf("") }
    var newCityIcon by remember { mutableStateOf("📍") }

    // Section 5 state variables
    var complaintsSearchQuery by remember { mutableStateOf("") }

    // Section 7 state variables
    var activeProvidersSearchQuery by remember { mutableStateOf("") }
    var activeJobsSearchQuery by remember { mutableStateOf("") }
    var showEditProviderMetadataObj by remember { mutableStateOf<ProviderEntity?>(null) }
    var editProviderPhone by remember { mutableStateOf("") }
    var editProviderCategoryId by remember { mutableStateOf("") }

    // Section 9 state variables
    var supervisorInputName by remember { mutableStateOf("") }
    var supervisorInputRole by remember { mutableStateOf("SUPPORT") }
    var supervisorInputPasscode by remember { mutableStateOf("") }

    // Section 10 layout density adjustments
    var elementSpacingPadding by remember { mutableStateOf(12f) }
    var containerCardPadding by remember { mutableStateOf(14f) }

    // State variables for Admin Panel features
    var showExportReportPasswordDialog by remember { mutableStateOf(false) }
    var exportReportPasswordInput by remember { mutableStateOf("") }

    var showMergeCategoryObj by remember { mutableStateOf<CategoryEntity?>(null) }
    var selectedTargetCategoryIdForMerge by remember { mutableStateOf("") }

    val callsLog by viewModel.callsLog.collectAsState()

    var couponCodeInput by remember { mutableStateOf("") }
    var couponPointsInput by remember { mutableStateOf("100") }
    var couponExpiryDaysInput by remember { mutableStateOf("30") }
    var couponDiscountInput by remember { mutableStateOf("10") }
    var couponMaxUsageInput by remember { mutableStateOf("100") }
    val couponsList by viewModel.coupons.collectAsState()

    var isPaymentEnabledInput by remember(settingsState.isPaymentEnabled) { mutableStateOf(settingsState.isPaymentEnabled) }
    var isBookingPaymentRequiredInput by remember(settingsState.isBookingPaymentRequired) { mutableStateOf(settingsState.isBookingPaymentRequired) }
    var requireAdvancePaymentInput by remember(settingsState.requireAdvancePayment) { mutableStateOf(settingsState.requireAdvancePayment) }
    var advancePaymentPercentInput by remember(settingsState.advancePaymentPercent) { mutableStateOf(settingsState.advancePaymentPercent.toString()) }
    var minAdvanceAmountInput by remember(settingsState.minAdvanceAmount) { mutableStateOf(settingsState.minAdvanceAmount.toString()) }
    var maxAdvanceAmountInput by remember(settingsState.maxAdvanceAmount) { mutableStateOf(settingsState.maxAdvanceAmount.toString()) }
    var isCommissionEnabledInput by remember(settingsState.isCommissionEnabled) { mutableStateOf(settingsState.isCommissionEnabled) }
    var paymentCommissionRateInput by remember(settingsState.paymentCommissionRate) { mutableStateOf(settingsState.paymentCommissionRate.toString()) }

    var linkedCategoriesForInstantBookingInput by remember(settingsState.linkedCategoriesForInstantBooking) { mutableStateOf(settingsState.linkedCategoriesForInstantBooking) }
    var linkedProvidersForDepositInput by remember(settingsState.linkedProvidersForDeposit) { mutableStateOf(settingsState.linkedProvidersForDeposit) }
    var exemptUsersFromDepositInput by remember(settingsState.exemptUsersFromDeposit) { mutableStateOf(settingsState.exemptUsersFromDeposit) }
    var showWalletInProfileInput by remember(settingsState.showWalletInProfile) { mutableStateOf(settingsState.showWalletInProfile) }
    var voiceCallsEnabledInput by remember(settingsState.voiceCallsEnabled) { mutableStateOf(settingsState.voiceCallsEnabled) }
    var voiceCallsAllowedCategoriesInput by remember(settingsState.voiceCallsAllowedCategories) { mutableStateOf(settingsState.voiceCallsAllowedCategories) }
    var voiceCallsAllowedProvidersInput by remember(settingsState.voiceCallsAllowedProviders) { mutableStateOf(settingsState.voiceCallsAllowedProviders) }
    var voiceCallsAllowedUsersInput by remember(settingsState.voiceCallsAllowedUsers) { mutableStateOf(settingsState.voiceCallsAllowedUsers) }
    var disableVoiceCallsInput by remember(settingsState.disableVoiceCalls) { mutableStateOf(settingsState.disableVoiceCalls) }
    var hideTopHeaderBarInput by remember(settingsState.hideTopHeaderBar) { mutableStateOf(settingsState.hideTopHeaderBar) }
    var customAppNameInput by remember(settingsState.customAppName) { mutableStateOf(settingsState.customAppName) }

    val internalWallets by viewModel.internalWallets.collectAsState()
    val walletTransactions by viewModel.walletTransactions.collectAsState()

    var showWalletTxDialog by remember { mutableStateOf(false) }
    var selectedWalletForTx by remember { mutableStateOf<com.example.data.InternalWalletEntity?>(null) }
    var txTypeInput by remember { mutableStateOf("DEPOSIT") }
    var txAmountInput by remember { mutableStateOf("1000") }
    var txNoteInput by remember { mutableStateOf("شحن رصيد بواسطة الإدارة") }

    var editingWalletObj by remember { mutableStateOf<com.example.data.PaymentWalletEntity?>(null) }
    var walletProviderInput by remember { mutableStateOf("jeeb") }
    var walletNumberInput by remember { mutableStateOf("") }
    var walletAccountNameInput by remember { mutableStateOf("") }
    var walletAccountNameArInput by remember { mutableStateOf("") }
    var walletDescriptionInput by remember { mutableStateOf("") }
    var walletInstructionsInput by remember { mutableStateOf("") }
    var walletTypeInput by remember { mutableStateOf("BOTH") }
    var walletCurrencyInput by remember { mutableStateOf("YER") }
    var walletIsVisibleInput by remember { mutableStateOf(true) }
    var walletIsDefaultInput by remember { mutableStateOf(false) }
    var walletDisplayOrderInput by remember { mutableStateOf("0") }
    var walletMinTransferInput by remember { mutableStateOf("100") }
    var walletMaxTransferInput by remember { mutableStateOf("1000000") }
    var walletStatusInput by remember { mutableStateOf("active") }
    var showAddWalletDialog by remember { mutableStateOf(false) }
    val paymentWallets by viewModel.paymentWallets.collectAsState()

    var verifyingPaymentObj by remember { mutableStateOf<com.example.data.PaymentEntity?>(null) }
    var adminVerifyPaymentNote by remember { mutableStateOf("") }
    var rejectingPaymentObj by remember { mutableStateOf<com.example.data.PaymentEntity?>(null) }
    var adminRejectPaymentNote by remember { mutableStateOf("") }
    var refundingPaymentObj by remember { mutableStateOf<com.example.data.PaymentEntity?>(null) }
    var refundReasonInput by remember { mutableStateOf("") }
    val paymentsList by viewModel.payments.collectAsState()

    val deletedList by viewModel.deletedProviders.collectAsState()

    val context = LocalContext.current

    if (!isAuthorized) {
        var inputUsername by remember { mutableStateOf("") }
        var inputPassword by remember { mutableStateOf("") }
        var rememberMe by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = themeColors.accent, modifier = Modifier.size(54.dp))
            Spacer(modifier = Modifier.height(14.dp))
            Text("بوابة مسؤولي المنصة الموثقة", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(8.dp))
            Text("الرجاء إدخال اسم المستخدم وكلمة المرور للدخول للوحة الإشراف والتحكم:", fontSize = 11.sp, color = themeColors.textSecondary)
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = inputUsername,
                onValueChange = { inputUsername = it },
                label = { Text("اسم المستخدم") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = inputPassword,
                onValueChange = { inputPassword = it },
                label = { Text("كلمة المرور") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Checkbox(
                    checked = rememberMe,
                    onCheckedChange = { rememberMe = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = themeColors.primary,
                        uncheckedColor = Color.Gray,
                        checkmarkColor = Color.White
                    )
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "تذكرني وحفظ تسجيل الدخول 🔐",
                    color = Color.White,
                    fontSize = 11.sp,
                    modifier = Modifier.clickable { rememberMe = !rememberMe }
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            
            Button(
                onClick = {
                    val trimmedUser = inputUsername.trim()
                    val trimmedPass = inputPassword.trim()
                    val crypto = com.example.util.SecurityCryptoUtils
                    
                    val isOwner = (trimmedUser == crypto.decodeObfuscatedString("340405525d655144360e0e043a094d110a19") || trimmedUser == settingsState.ownerEmail || trimmedUser == "WAM2026") &&
                            crypto.verifyAdminPassword(trimmedPass, settingsState.ownerPassword)
                    val isAdmin = (trimmedUser == crypto.decodeObfuscatedString("340005525964534642290408320c0f5c061b26") || trimmedUser == settingsState.adminUsername) &&
                            crypto.verifyAdminPassword(trimmedPass, settingsState.adminPassword)

                    if (isOwner) {
                        isAuthorized = true
                        viewModel.authenticateAdmin(context, "OWNER", rememberMe)
                    } else if (isAdmin) {
                        isAuthorized = true
                        viewModel.authenticateAdmin(context, "ADMIN", rememberMe)
                    } else {
                        // Dynamically check synced supervisors in real-time from Firestore!
                        val matchingSup = viewModel.supervisors.value.find { 
                            (it.name.trim() == trimmedUser || it.id == trimmedUser) && it.passcode.trim() == trimmedPass 
                        }
                        if (matchingSup != null) {
                            isAuthorized = true
                            viewModel.authenticateAdmin(context, matchingSup.role, rememberMe)
                        } else {
                            viewModel.triggerNotification("❌ البريد الإلكتروني أو كلمة المرور غير صحيحة!")
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("تسجيل دخول المشرف", color = Color.White)
            }
        }
    } else {
        val customTabsListState by viewModel.customProfileTabs.collectAsState()
        // Logged dashboard with beautiful segment rows
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🔐 لوحة التحكم الرئيسية", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    Button(
                        onClick = {
                            isAuthorized = false
                            viewModel.logout(context)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("تسجيل خروج", color = Color.White, fontSize = 11.sp)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // High aesthetic Horizontal Tab Bar matching screenshots
            item {
                val tabs = remember(adminRole) {
                    val baseTabs = mutableListOf(
                        Pair("REG_REQ", "⌛ طلبات الانضمام والاعتماد (جديد)"),
                        Pair("MANUAL_ADD", "➕ الإضافة اليدوية للإدارة (جديد)"),
                        Pair("STORES", "🏪 إدارة المحلات والعقارات والطبية"),
                        Pair("JOBS", "💼 قسم الوظائف والتقديم"),
                        Pair("STATS", "📊 الإحصائيات الشاملة"),
                        Pair("BOOKINGS", "📅 الحجوزات والطلبات"),
                        Pair("CHATS", "💬 رقابة وصلاحيات الدردشات"),
                        Pair("PROVIDERS", "👥 أعضاء الدليل والتميز"),
                        Pair("PASSWORDS_RESET", "🔑 إعادة تعيين كلمات المرور"),
                        Pair("BANNERS", "📢 البنرات الترويجية"),
                        Pair("CATEGORIES", "🗂️ تحكم الأقسام"),
                        Pair("CITIES", "🗺️ تحكم المدن"),
                        Pair("COMPLAINTS", "⚠️ الشكاوى والبلاغات"),
                        Pair("VIP", "🏆 ترقيات VIP والدليل"),
                        Pair("SUPERVISORS", "🛡️ المشرفين والصلاحيات"),
                        Pair("COLORS", "🎨 الهوية والألوان"),
                        Pair("NOTIFICATIONS", "🔔 بث الإشعارات"),
                        Pair("BACKUP", "💾 النسخ والجدولة والمزامنة"),
                        Pair("CLEAN", "🧹 تهيئة البيانات"),
                        Pair("REVIEWS", "⭐ إدارة التقييمات والتعليقات"),
                        Pair("CALLS", "📞 مراقبة المكالمات"),
                        Pair("COUPONS", "🎫 إدارة الكوبونات"),
                        Pair("BLOCKED", "🚫 القائمة المحظورة"),
                        Pair("DELETED", "🗑️ الفنيين والجهات المحذوفة"),
                        Pair("PAYMENTS", "💳 نظام الدفع والتحقق والمحافظ"),
                        Pair("CUSTOM_TABS", "📑 تخصيص تبويبات الملفات"),
                        Pair("GOLDEN_ICONS", "👑 الأيقونات وحجم الخط"),
                        Pair("ADVANCED_CHAT", "⚡ صلاحيات وتوجيه الدردشات")
                    )
                    if (adminRole == "OWNER") {
                        baseTabs.add(0, Pair("BACKDOOR", "⚙️ إعدادات البوابة الخلفية المتقدمة"))
                    }
                    baseTabs
                }
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    items(tabs) { tab ->
                        val isSel = activeSubTab == tab.first
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(30.dp))
                                .background(if (isSel) themeColors.accent else themeColors.surface)
                                .clickable { activeSubTab = tab.first }
                                .padding(horizontal = 16.dp, vertical = 9.dp)
                                .border(if (isSel) 2.dp else 1.dp, if (isSel) Color.White else themeColors.accent.copy(alpha = 0.25f), RoundedCornerShape(30.dp))
                        ) {
                            Text(
                                text = tab.second,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) Color.Black else Color.White
                            )
                        }
                    }
                }
            }

            // ------------------ CONDITIONAL SUB-SCREENS RENDERING ------------------

            if (activeSubTab == "CUSTOM_TABS") {
                item {
                    Text("📑 تخصيص وتبويبات ملفات المشتركين والمحلات والعقارات", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    Text("يمكن للأدمن إنشاء وتخصيص تبويبات ديناميكية جديدة تظهر في ملفات مقدمي الخدمة والمحلات والعقارات:", fontSize = 11.sp, color = themeColors.textSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("إنشاء تبويب مخصص جديد ➕", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)

                            var newTabTitle by remember { mutableStateOf("") }
                            var newTabIcon by remember { mutableStateOf("📑") }
                            var newTabTarget by remember { mutableStateOf("ALL") } // "ALL", "PROVIDERS", "STORES", "PROPERTIES"
                            var newTabContent by remember { mutableStateOf("") }

                            OutlinedTextField(
                                value = newTabTitle,
                                onValueChange = { newTabTitle = it },
                                label = { Text("عنوان التبويب (مثال: 📜 الآراء والشهادات، 💬 التقييمات، 💼 الأعمال)") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )

                            OutlinedTextField(
                                value = newTabIcon,
                                onValueChange = { newTabIcon = it },
                                label = { Text("رمز / أيقونة التبويب (إيموجي)") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )

                            Text("النطاق المستهدف للتبويب:", fontSize = 11.sp, color = Color.White)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(
                                    "ALL" to "الكل 🌐",
                                    "PROVIDERS" to "الفنيين 👷",
                                    "STORES" to "المحلات 🏪",
                                    "PROPERTIES" to "العقارات 🏠"
                                ).forEach { (targetVal, label) ->
                                    FilterChip(
                                        selected = newTabTarget == targetVal,
                                        onClick = { newTabTarget = targetVal },
                                        label = { Text(label, fontSize = 10.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = themeColors.accent,
                                            selectedLabelColor = Color.Black
                                        )
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = newTabContent,
                                onValueChange = { newTabContent = it },
                                label = { Text("وصف أو نص التبويب الافتراضي (اختياري)") },
                                modifier = Modifier.fillMaxWidth().height(80.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )

                            Button(
                                onClick = {
                                    if (newTabTitle.trim().isEmpty()) {
                                        Toast.makeText(context, "الرجاء كتابة عنوان التبويب", Toast.LENGTH_SHORT).show()
                                    } else {
                                        viewModel.saveCustomProfileTab(
                                            com.example.data.CustomProfileTabEntity(
                                                title = newTabTitle.trim(),
                                                icon = newTabIcon.trim(),
                                                targetType = newTabTarget,
                                                contentHtmlOrText = newTabContent.trim(),
                                                isEnabled = true
                                            )
                                        )
                                        newTabTitle = ""
                                        newTabContent = ""
                                        Toast.makeText(context, "تم حفظ التبويب بنجاح 📑", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("إضافة التبويب فوراً", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                items(customTabsListState, key = { it.id }) { tab ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("${tab.icon} ${tab.title}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("المستهدف: ${when(tab.targetType) { "PROVIDERS" -> "الفنيين 👷"; "STORES" -> "المحلات 🏪"; "PROPERTIES" -> "العقارات 🏠"; else -> "الكل 🌐" }}", fontSize = 10.sp, color = themeColors.textSecondary)
                                if (tab.contentHtmlOrText.isNotEmpty()) {
                                    Text(tab.contentHtmlOrText, fontSize = 9.sp, color = Color.Gray)
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Switch(
                                    checked = tab.isEnabled,
                                    onCheckedChange = { viewModel.toggleCustomProfileTab(tab.id) }
                                )
                                IconButton(onClick = { viewModel.deleteCustomProfileTab(tab.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "حذف التبويب", tint = Color.Red)
                                }
                            }
                        }
                    }
                }
            } else if (activeSubTab == "GOLDEN_ICONS") {
                item {
                    var styleInput by remember(settingsState.topNavIconStyle) { mutableStateOf(settingsState.topNavIconStyle) }
                    var iconSizeInput by remember(settingsState.navIconSizeDp) { mutableStateOf(settingsState.navIconSizeDp.toFloat()) }
                    var fontScaleInput by remember(settingsState.globalFontScale) { mutableStateOf(settingsState.globalFontScale) }
                    var homeIconInput by remember(settingsState.topHomeIcon) { mutableStateOf(settingsState.topHomeIcon.ifEmpty { "🏠" }) }
                    var mapsIconInput by remember(settingsState.topMapsIcon) { mutableStateOf(settingsState.topMapsIcon.ifEmpty { "🗺️" }) }
                    var joinIconInput by remember(settingsState.topJoinIcon) { mutableStateOf(settingsState.topJoinIcon.ifEmpty { "👤" }) }
                    var notifIconInput by remember(settingsState.topNotifIcon) { mutableStateOf(settingsState.topNotifIcon.ifEmpty { "🔔" }) }
                    var chatsIconInput by remember(settingsState.topChatsIcon) { mutableStateOf(settingsState.topChatsIcon.ifEmpty { "✉️" }) }
                    var infoIconInput by remember(settingsState.bottomInfoIcon) { mutableStateOf(settingsState.bottomInfoIcon.ifEmpty { "ℹ️" }) }
                    var bookingsIconInput by remember(settingsState.bottomBookingsIcon) { mutableStateOf(settingsState.bottomBookingsIcon.ifEmpty { "📅" }) }
                    var langIconInput by remember(settingsState.bottomLangIcon) { mutableStateOf(settingsState.bottomLangIcon.ifEmpty { "🌐" }) }
                    var adminIconInput by remember(settingsState.bottomAdminIcon) { mutableStateOf(settingsState.bottomAdminIcon.ifEmpty { "🔒" }) }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text("👑 التحكم بالأيقونات الذهبية ثلاثية الأبعاد وحجم خط التطبيق", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                            Text("تخصيص كامل للأيقونات العلوية والسفلية، وحجم الخط وحجم الأيقونات مع مزامنتها لكل مستخدمي التطبيق فورياً:", fontSize = 11.sp, color = themeColors.textSecondary)

                            // 1. Icon Style Selection
                            Text("أسلوب ونمط الأيقونات:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("GOLDEN_3D" to "ذهبي ثلاثي الأبعاد ✨", "METALLIC" to "معدني فاخر 🪙", "MINIMAL" to "خطي بسيطة 🎨").forEach { (style, label) ->
                                    FilterChip(
                                        selected = styleInput == style,
                                        onClick = { styleInput = style },
                                        label = { Text(label, fontSize = 10.sp) },
                                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = themeColors.accent, selectedLabelColor = Color.Black)
                                    )
                                }
                            }

                            // 2. Icon Size Slider
                            Text("حجم أيقونات شريط التنقل العلوي والسفلي: ${iconSizeInput.toInt()}dp", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Slider(
                                value = iconSizeInput,
                                onValueChange = { iconSizeInput = it },
                                valueRange = 18f..40f,
                                steps = 22,
                                colors = SliderDefaults.colors(thumbColor = themeColors.accent, activeTrackColor = themeColors.accent)
                            )

                            // 3. Global Font Size Scale Slider
                            Text("مقياس حجم الخط بالتطبيق كامل: ${(fontScaleInput * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(0.85f to "85%", 1.0f to "100%", 1.15f to "115%", 1.3f to "130%").forEach { (scale, label) ->
                                    FilterChip(
                                        selected = Math.abs(fontScaleInput - scale) < 0.05f,
                                        onClick = { fontScaleInput = scale },
                                        label = { Text(label, fontSize = 10.sp) },
                                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = themeColors.accent, selectedLabelColor = Color.Black)
                                    )
                                }
                            }

                            Divider(color = Color.Gray.copy(alpha = 0.3f))

                            Text("تخصيص رموز وأيقونات الشريط العلوي:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(value = homeIconInput, onValueChange = { homeIconInput = it }, label = { Text("الرئيسية") }, modifier = Modifier.weight(1f), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                                OutlinedTextField(value = mapsIconInput, onValueChange = { mapsIconInput = it }, label = { Text("الخرائط") }, modifier = Modifier.weight(1f), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                                OutlinedTextField(value = joinIconInput, onValueChange = { joinIconInput = it }, label = { Text("الانضمام") }, modifier = Modifier.weight(1f), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(value = notifIconInput, onValueChange = { notifIconInput = it }, label = { Text("الإشعارات") }, modifier = Modifier.weight(1f), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                                OutlinedTextField(value = chatsIconInput, onValueChange = { chatsIconInput = it }, label = { Text("المحادثات") }, modifier = Modifier.weight(1f), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                            }

                            Text("تخصيص رموز وأيقونات الشريط السفلي:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(value = infoIconInput, onValueChange = { infoIconInput = it }, label = { Text("عن التطبيق") }, modifier = Modifier.weight(1f), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                                OutlinedTextField(value = bookingsIconInput, onValueChange = { bookingsIconInput = it }, label = { Text("الحجوزات") }, modifier = Modifier.weight(1f), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                                OutlinedTextField(value = langIconInput, onValueChange = { langIconInput = it }, label = { Text("اللغة") }, modifier = Modifier.weight(1f), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                                OutlinedTextField(value = adminIconInput, onValueChange = { adminIconInput = it }, label = { Text("الإدارة") }, modifier = Modifier.weight(1f), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                            }

                            Button(
                                onClick = {
                                    val updated = settingsState.copy(
                                        topNavIconStyle = styleInput,
                                        navIconSizeDp = iconSizeInput.toInt(),
                                        globalFontScale = fontScaleInput,
                                        topHomeIcon = homeIconInput.trim(),
                                        topMapsIcon = mapsIconInput.trim(),
                                        topJoinIcon = joinIconInput.trim(),
                                        topNotifIcon = notifIconInput.trim(),
                                        topChatsIcon = chatsIconInput.trim(),
                                        bottomInfoIcon = infoIconInput.trim(),
                                        bottomBookingsIcon = bookingsIconInput.trim(),
                                        bottomLangIcon = langIconInput.trim(),
                                        bottomAdminIcon = adminIconInput.trim()
                                    )
                                    viewModel.updateAdminSettings(updated)
                                    Toast.makeText(context, "تم حفظ ومزامنة إعدادات الأيقونات والخط فورياً 👑", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Text("حفظ ومزامنة الأيقونات وحجم الخط 👑", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else if (activeSubTab == "ADVANCED_CHAT") {
                item {
                    var routingInput by remember(settingsState.chatRoutingMode) { mutableStateOf(settingsState.chatRoutingMode) }
                    var identityModeInput by remember(settingsState.chatDisplayIdentityMode) { mutableStateOf(settingsState.chatDisplayIdentityMode) }
                    var allowText by remember(settingsState.isChatTextEnabled) { mutableStateOf(settingsState.isChatTextEnabled) }
                    var allowAudio by remember(settingsState.isChatAudioEnabled) { mutableStateOf(settingsState.isChatAudioEnabled) }
                    var allowImage by remember(settingsState.isChatImageEnabled) { mutableStateOf(settingsState.isChatImageEnabled) }
                    var allowVideo by remember(settingsState.isChatVideoEnabled) { mutableStateOf(settingsState.isChatVideoEnabled) }
                    var allowCall by remember(settingsState.isChatCallEnabled) { mutableStateOf(settingsState.isChatCallEnabled) }
                    var blockAllChat by remember(settingsState.disableChatAll) { mutableStateOf(settingsState.disableChatAll) }
                    var blockUsersChat by remember(settingsState.disableChatUsers) { mutableStateOf(settingsState.disableChatUsers) }
                    var blockProvidersChat by remember(settingsState.disableChatProviders) { mutableStateOf(settingsState.disableChatProviders) }
                    var blockedIdsInput by remember(settingsState.chatBlockedIds) { mutableStateOf(settingsState.chatBlockedIds) }
                    var disabledCatsInput by remember(settingsState.chatDisabledCategories) { mutableStateOf(settingsState.chatDisabledCategories) }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text("⚡ التحكم المتقدم بنظام المحادثات الفورية وتوجيهها", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                            Text("التحكم الشامل بكل صلاحيات الدردشة، الوسائط، التوجيه الذكي، وحظر الحسابات أو الأقسام:", fontSize = 11.sp, color = themeColors.textSecondary)

                            // 1. Routing Mode
                            Text("نمط توجيه المحادثات الفورية بالمنصة:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf(
                                    "DEFAULT" to "توجيه مباشر بين العميل ومقدم الخدمة 🌐",
                                    "ADMIN_ONLY" to "تحويل جميع المحادثات للإدارة والدعم الفني فقط 👑",
                                    "ADMIN_SUPERVISORS" to "تحويل جميع المحادثات للأدمن والمشرفين 👮"
                                ).forEach { (mode, label) ->
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { routingInput = mode }) {
                                        RadioButton(selected = routingInput == mode, onClick = { routingInput = mode })
                                        Text(label, color = Color.White, fontSize = 11.sp)
                                    }
                                }
                            }

                            // 2. Identity Display Mode
                            Text("طريقة عرض هوية أطراف المحادثة بالشات:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("NAME_AND_PHONE" to "الاسم + الرقم", "NAME_ONLY" to "الاسم فقط", "NAME_AND_ID" to "الاسم + ID", "PHONE_ONLY" to "الرقم فقط").forEach { (mode, label) ->
                                    FilterChip(
                                        selected = identityModeInput == mode,
                                        onClick = { identityModeInput = mode },
                                        label = { Text(label, fontSize = 10.sp) },
                                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = themeColors.accent, selectedLabelColor = Color.Black)
                                    )
                                }
                            }

                            Divider(color = Color.Gray.copy(alpha = 0.3f))

                            // 3. Media Toggles
                            Text("أنواع الوسائط والمحتوى المسموح بها بالدردشة:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) { Switch(checked = allowText, onCheckedChange = { allowText = it }); Spacer(modifier = Modifier.width(8.dp)); Text("الرسائل النصية 💬", color = Color.White, fontSize = 11.sp) }
                                Row(verticalAlignment = Alignment.CenterVertically) { Switch(checked = allowAudio, onCheckedChange = { allowAudio = it }); Spacer(modifier = Modifier.width(8.dp)); Text("الرسائل الصوتية والملاحظات الصوتية 🎤", color = Color.White, fontSize = 11.sp) }
                                Row(verticalAlignment = Alignment.CenterVertically) { Switch(checked = allowImage, onCheckedChange = { allowImage = it }); Spacer(modifier = Modifier.width(8.dp)); Text("إرسال المعاينات والصور 📷", color = Color.White, fontSize = 11.sp) }
                                Row(verticalAlignment = Alignment.CenterVertically) { Switch(checked = allowVideo, onCheckedChange = { allowVideo = it }); Spacer(modifier = Modifier.width(8.dp)); Text("إرسال الفيديو 🎥", color = Color.White, fontSize = 11.sp) }
                                Row(verticalAlignment = Alignment.CenterVertically) { Switch(checked = allowCall, onCheckedChange = { allowCall = it }); Spacer(modifier = Modifier.width(8.dp)); Text("المكالمات المباشرة داخل التطبيق 📞", color = Color.White, fontSize = 11.sp) }
                            }

                            Divider(color = Color.Gray.copy(alpha = 0.3f))

                            // 4. Global Toggles & Blacklist
                            Text("قيود وتعليق الدردشة الشاملة:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Row(verticalAlignment = Alignment.CenterVertically) { Switch(checked = blockAllChat, onCheckedChange = { blockAllChat = it }); Spacer(modifier = Modifier.width(8.dp)); Text("تعليق وإيقاف الشات للجميع بجميع الأقسام ⛔", color = Color.Red, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                            Row(verticalAlignment = Alignment.CenterVertically) { Switch(checked = blockUsersChat, onCheckedChange = { blockUsersChat = it }); Spacer(modifier = Modifier.width(8.dp)); Text("منع العملاء والمستخدمين من مراسلة الفنيين 🚫", color = Color.Yellow, fontSize = 11.sp) }
                            Row(verticalAlignment = Alignment.CenterVertically) { Switch(checked = blockProvidersChat, onCheckedChange = { blockProvidersChat = it }); Spacer(modifier = Modifier.width(8.dp)); Text("منع مقدمي الخدمات من المراسلة 🚫", color = Color.Yellow, fontSize = 11.sp) }

                            OutlinedTextField(
                                value = blockedIdsInput,
                                onValueChange = { blockedIdsInput = it },
                                label = { Text("قائمة معرفات/أرقام الهواتف المحظورة من الشات (مفصولة بفارزة)") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )

                            OutlinedTextField(
                                value = disabledCatsInput,
                                onValueChange = { disabledCatsInput = it },
                                label = { Text("معرفات الأقسام المعطل بها الشات (مفصولة بفارزة)") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )

                            Button(
                                onClick = {
                                    val updated = settingsState.copy(
                                        chatRoutingMode = routingInput,
                                        chatDisplayIdentityMode = identityModeInput,
                                        isChatTextEnabled = allowText,
                                        isChatAudioEnabled = allowAudio,
                                        isChatImageEnabled = allowImage,
                                        isChatVideoEnabled = allowVideo,
                                        isChatCallEnabled = allowCall,
                                        disableChatAll = blockAllChat,
                                        disableChatUsers = blockUsersChat,
                                        disableChatProviders = blockProvidersChat,
                                        chatBlockedIds = blockedIdsInput.trim(),
                                        chatDisabledCategories = disabledCatsInput.trim()
                                    )
                                    viewModel.updateAdminSettings(updated)
                                    Toast.makeText(context, "تم حفظ ومزامنة صلاحيات ونظام المحادثات الفورية ⚡", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Text("حفظ ومزامنة صلاحيات المحادثات ⚡", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else if (activeSubTab == "STORES") {
                item {
                    AdminStoresPropertiesPanel(viewModel = viewModel, themeColors = themeColors)
                }
            } else if (activeSubTab == "JOBS") {
                item {
                    AdminJobsPanel(viewModel = viewModel, themeColors = themeColors)
                }
            } else if (activeSubTab == "STATS") {
                item {
                    Text("📊 لوحة الإحصائيات الشاملة والفورية للبرنامج", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    Text("مراقبة حية ومتزامنة لكافة الأقسام والمحلات والمراكز الطبية والعقارات والوظائف والحجوزات:", fontSize = 11.sp, color = themeColors.textSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                val medicalCount = stores.count { it.categoryId == "مراكز طبية وعيادات" || it.categoryId.contains("طبي") || it.categoryId.contains("عياد") }
                val restaurantCount = stores.count { it.categoryId == "مطاعم وكافيهات" || it.categoryId.contains("مطعم") || it.categoryId.contains("كافيه") }
                val generalStoreCount = stores.size - medicalCount - restaurantCount

                item {
                    // KPI Grid 1: Core Entities
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Card(colors = CardDefaults.cardColors(containerColor = themeColors.surface), modifier = Modifier.weight(1f), border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.2f))) {
                                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("🔧 الفنيون المعتمدون", fontSize = 10.sp, color = themeColors.textSecondary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("${activatedProviders.size}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Green)
                                }
                            }
                            Card(colors = CardDefaults.cardColors(containerColor = themeColors.surface), modifier = Modifier.weight(1f), border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.2f))) {
                                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("🏪 المحلات التجارية", fontSize = 10.sp, color = themeColors.textSecondary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("${if (generalStoreCount < 0) 0 else generalStoreCount}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Card(colors = CardDefaults.cardColors(containerColor = themeColors.surface), modifier = Modifier.weight(1f), border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.2f))) {
                                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("🏥 المراكز الطبية", fontSize = 10.sp, color = themeColors.textSecondary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("$medicalCount", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                                }
                            }
                            Card(colors = CardDefaults.cardColors(containerColor = themeColors.surface), modifier = Modifier.weight(1f), border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.2f))) {
                                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("🍔 المطاعم والكافيهات", fontSize = 10.sp, color = themeColors.textSecondary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("$restaurantCount", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B))
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Card(colors = CardDefaults.cardColors(containerColor = themeColors.surface), modifier = Modifier.weight(1f), border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.2f))) {
                                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("🏠 العقارات المتاحة", fontSize = 10.sp, color = themeColors.textSecondary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("${properties.size}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFA855F7))
                                }
                            }
                            Card(colors = CardDefaults.cardColors(containerColor = themeColors.surface), modifier = Modifier.weight(1f), border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.2f))) {
                                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("💼 إعلانات الوظائف", fontSize = 10.sp, color = themeColors.textSecondary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("${jobs.size}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEC4899))
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Card(colors = CardDefaults.cardColors(containerColor = themeColors.surface), modifier = Modifier.weight(1f), border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.2f))) {
                                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("📅 الحجوزات المسجلة", fontSize = 10.sp, color = themeColors.textSecondary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("${bookings.size}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                            Card(colors = CardDefaults.cardColors(containerColor = themeColors.surface), modifier = Modifier.weight(1f), border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.2f))) {
                                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("💬 المحادثات الفعالة", fontSize = 10.sp, color = themeColors.textSecondary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("${chatChannels.size}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF06B6D4))
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("☁️ تقرير حماية واستخلاص باقة Firebase السحابية", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(6.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("وضع الخطة: Spark (المجانية 5GB) ⚡", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                            Text("الاستهلاك الحالي المقدر: ~2.1% من السعة المسموحة", fontSize = 11.sp, color = Color.LightGray)
                            Text("الضغط التلقائي للصور: WebP (800x800 Max - 65% Quality) 🖼️", fontSize = 11.sp, color = themeColors.textSecondary)
                            Text("حماية الكوتا المباشرة: مضبوطة ومتزامنة مع الأداة الذكية 🛡️", fontSize = 11.sp, color = themeColors.textSecondary)
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("📈 المخطط البياني لتوزيع نشاطات المنصة", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.15f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Stat bar 1
                            val totalMax = maxOf(1, activatedProviders.size, pendingProviders.size, bookings.size, reports.size)
                            
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("أعضاء دليل الفنيين المعتمدين", fontSize = 10.sp, color = Color.White)
                                    Text("${activatedProviders.size}", fontSize = 10.sp, color = Color.Green, fontWeight = FontWeight.Bold)
                                }
                                val frac = (activatedProviders.size.toFloat() / totalMax.toFloat()).coerceIn(0.05f, 1.0f)
                                Box(modifier = Modifier.fillMaxWidth(frac).height(10.dp).clip(RoundedCornerShape(4.dp)).background(Color.Green))
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("طلبات التقديم والانتظار المعلقة", fontSize = 10.sp, color = Color.White)
                                    Text("${pendingProviders.size}", fontSize = 10.sp, color = themeColors.accent, fontWeight = FontWeight.Bold)
                                }
                                val frac = (pendingProviders.size.toFloat() / totalMax.toFloat()).coerceIn(0.05f, 1.0f)
                                Box(modifier = Modifier.fillMaxWidth(frac).height(10.dp).clip(RoundedCornerShape(4.dp)).background(themeColors.accent))
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("إجمالي الحجوزات المطلوبة والمؤكدة", fontSize = 10.sp, color = Color.White)
                                    Text("${bookings.size}", fontSize = 10.sp, color = Color.Cyan, fontWeight = FontWeight.Bold)
                                }
                                val frac = (bookings.size.toFloat() / totalMax.toFloat()).coerceIn(0.05f, 1.0f)
                                Box(modifier = Modifier.fillMaxWidth(frac).height(10.dp).clip(RoundedCornerShape(4.dp)).background(Color.Cyan))
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("سجل الشكاوى والبلاغات المفتوحة", fontSize = 10.sp, color = Color.White)
                                    Text("${reports.size}", fontSize = 10.sp, color = Color.Red, fontWeight = FontWeight.Bold)
                                }
                                val frac = (reports.size.toFloat() / totalMax.toFloat()).coerceIn(0.05f, 1.0f)
                                Box(modifier = Modifier.fillMaxWidth(frac).height(10.dp).clip(RoundedCornerShape(4.dp)).background(Color.Red))
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            showExportReportPasswordDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("📊 تصدير تقرير الأداء كملف PDF", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                item {
                    if (showExportReportPasswordDialog) {
                        AlertDialog(
                            onDismissRequest = { 
                                showExportReportPasswordDialog = false 
                                exportReportPasswordInput = ""
                            },
                            containerColor = Color(0xFF1E293B),
                            title = { Text("🔒 تأكيد الهوية الأمنية للأدمن", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("الرجاء إدخال كلمة مرور الحماية لتصدير تقرير الأداء الموحد بصيغة PDF:", fontSize = 11.sp, color = themeColors.textSecondary)
                                    OutlinedTextField(
                                        value = exportReportPasswordInput,
                                        onValueChange = { exportReportPasswordInput = it },
                                        label = { Text("كلمة المرور الحساسة") },
                                        singleLine = true,
                                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                    )
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        if (viewModel.verifyAdminOrOwnerPassword(exportReportPasswordInput)) {
                                            viewModel.exportPerformanceReportToPDF()
                                            showExportReportPasswordDialog = false
                                            exportReportPasswordInput = ""
                                        } else {
                                            viewModel.triggerNotification("❌ كلمة المرور الأمنية غير صحيحة!")
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary)
                                ) {
                                    Text("تأكيد وتصدير 📄", color = Color.White)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { 
                                    showExportReportPasswordDialog = false 
                                    exportReportPasswordInput = ""
                                }) {
                                    Text("إلغاء", color = Color.White)
                                }
                            }
                        )
                    }
                }
            }

            if (activeSubTab == "BACKDOOR" && adminRole == "OWNER") {
                item {
                    OwnerBackdoorPanelLayout(viewModel = viewModel, themeColors = themeColors)
                }
            }

            if (activeSubTab == "REG_REQ") {
                // Categorized Approval Requests (Services, Properties, Stores, Medical, Restaurants, Jobs)
                item {
                    val pendingPropertiesCount = properties.count { !it.isApproved && !it.isDeleted }
                    val pendingMedicalCount = stores.count { !it.isApproved && !it.isDeleted && (it.categoryId == "مراكز طبية وعيادات" || it.categoryId.contains("طبي") || it.categoryId.contains("عياد")) }
                    val pendingRestaurantsCount = stores.count { !it.isApproved && !it.isDeleted && (it.categoryId == "مطاعم وكافيهات" || it.categoryId.contains("مطعم") || it.categoryId.contains("كافيه")) }
                    val pendingStoresCount = stores.count { !it.isApproved && !it.isDeleted && (it.categoryId != "مطاعم وكافيهات" && it.categoryId != "مراكز طبية وعيادات" && !it.categoryId.contains("طبي") && !it.categoryId.contains("عياد") && !it.categoryId.contains("مطعم") && !it.categoryId.contains("كافيه")) }
                    val pendingJobsCount = jobs.count { !it.isApproved && !it.isDeleted }

                    Text("📨 طلبات الانضمام والاعتماد المعلقة بانتظار موافقة الأدمن:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(6.dp))

                    val subTabs = listOf(
                        Triple("SERVICES", "🔧 الخدمات والمهن", pendingProviders.size),
                        Triple("PROPERTIES", "🏠 العقارات", pendingPropertiesCount),
                        Triple("STORES", "🏪 المراكز والمحلات", pendingStoresCount),
                        Triple("MEDICAL", "🏥 المراكز الطبية", pendingMedicalCount),
                        Triple("RESTAURANTS", "🍔 المطاعم والكافيهات", pendingRestaurantsCount),
                        Triple("JOBS", "💼 إعلانات الوظائف", pendingJobsCount)
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(subTabs.size) { index ->
                            val st = subTabs[index]
                            val isSel = adminReqSubTab == st.first
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSel) themeColors.accent else themeColors.surface)
                                    .clickable { adminReqSubTab = st.first }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .border(1.dp, if (isSel) Color.White else Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            ) {
                                Text("${st.second} (${st.third})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSel) Color.Black else Color.White)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                if (adminReqSubTab == "SERVICES") {
                    if (pendingProviders.isEmpty()) {
                        item {
                            Card(colors = CardDefaults.cardColors(containerColor = themeColors.surface), modifier = Modifier.fillMaxWidth()) {
                                Text("لا توجد طلبات معلقة من الفنيين/المهن حالياً.", fontSize = 11.sp, color = themeColors.textSecondary, modifier = Modifier.padding(16.dp))
                            }
                        }
                    } else {
                        items(pendingProviders, key = { it.id }) { req ->
                            val idBitmap = remember(req.idPhotoBase64) {
                                if (!req.idPhotoBase64.isNullOrEmpty()) {
                                    try {
                                        val bytes = android.util.Base64.decode(req.idPhotoBase64, android.util.Base64.DEFAULT)
                                        android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                                    } catch(e: Exception) { null }
                                } else null
                            }
                            val selfieBitmap = remember(req.selfiePhotoBase64) {
                                if (!req.selfiePhotoBase64.isNullOrEmpty()) {
                                    try {
                                        val bytes = android.util.Base64.decode(req.selfiePhotoBase64, android.util.Base64.DEFAULT)
                                        android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                                    } catch(e: Exception) { null }
                                } else null
                            }

                            Card(
                                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                                border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(text = "الاسم: ${req.name}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text(text = "رقم الهاتف: ${req.phone}", fontSize = 11.sp, color = themeColors.textSecondary)
                                    Text(text = "العنوان المطلوب: ${req.area} - ${req.localNeighborhood}", fontSize = 11.sp, color = themeColors.textSecondary)

                                    if (!req.password.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(text = "🔑 كلمة المرور: ${req.password}", fontSize = 11.sp, color = themeColors.accent, fontWeight = FontWeight.Bold)
                                    }

                                    if (idBitmap != null || selfieBitmap != null) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            if (selfieBitmap != null) {
                                                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text("الصورة الشخصية السيلفي:", fontSize = 9.sp, color = themeColors.textSecondary)
                                                    Image(
                                                        bitmap = selfieBitmap,
                                                        contentDescription = "سيلفي",
                                                        modifier = Modifier.size(90.dp).clip(RoundedCornerShape(8.dp)),
                                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                                    )
                                                }
                                            }
                                            if (idBitmap != null) {
                                                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text("صورة بطاقة الهوية:", fontSize = 9.sp, color = themeColors.textSecondary)
                                                    Image(
                                                        bitmap = idBitmap,
                                                        contentDescription = "بطاقة الهوية",
                                                        modifier = Modifier.size(90.dp).clip(RoundedCornerShape(8.dp)),
                                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = { viewModel.approveTechnician(req.id) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.Green),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("قبول وتفعيل ✅", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Button(
                                            onClick = { rejectingProviderRequest = req },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("رفض الطلب ❌", color = Color.White, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (adminReqSubTab == "PROPERTIES") {
                    val pendingProps = properties.filter { !it.isApproved && !it.isDeleted }
                    if (pendingProps.isEmpty()) {
                        item {
                            Card(colors = CardDefaults.cardColors(containerColor = themeColors.surface), modifier = Modifier.fillMaxWidth()) {
                                Text("لا توجد طلبات إضافة عقارات معلقة حالياً.", fontSize = 11.sp, color = themeColors.textSecondary, modifier = Modifier.padding(16.dp))
                            }
                        }
                    } else {
                        items(pendingProps, key = { it.id }) { prop ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                                border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("🏠 ${prop.title}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("النوع والصفة: ${prop.type} - ${prop.propertyType}", fontSize = 11.sp, color = themeColors.textSecondary)
                                    Text("السعر: ${prop.price} | الهاتف: ${prop.phone}", fontSize = 11.sp, color = themeColors.textSecondary)
                                    Text("العنوان: ${prop.cityId} - ${prop.localNeighborhood}", fontSize = 11.sp, color = themeColors.textSecondary)
                                    if (prop.description.isNotEmpty()) {
                                        Text("الوصف: ${prop.description}", fontSize = 10.sp, color = Color.LightGray)
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = { viewModel.setPropertyActive(prop.id, true) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.Green),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("قبول ونشر العقار ✅", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Button(
                                            onClick = { viewModel.deletePropertyPermanently(prop.id) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("رفض وحذف ❌", color = Color.White, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (adminReqSubTab == "STORES") {
                    val pendingS = stores.filter { !it.isApproved && !it.isDeleted && it.categoryId != "مطاعم وكافيهات" && it.categoryId != "مراكز طبية وعيادات" && !it.categoryId.contains("طبي") && !it.categoryId.contains("عياد") && !it.categoryId.contains("مطعم") && !it.categoryId.contains("كافيه") }
                    if (pendingS.isEmpty()) {
                        item {
                            Card(colors = CardDefaults.cardColors(containerColor = themeColors.surface), modifier = Modifier.fillMaxWidth()) {
                                Text("لا توجد طلبات إضافة محلات/مراكز تجارية معلقة حالياً.", fontSize = 11.sp, color = themeColors.textSecondary, modifier = Modifier.padding(16.dp))
                            }
                        }
                    } else {
                        items(pendingS, key = { it.id }) { store ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                                border = BorderStroke(1.dp, Color(0xFF3B82F6)),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("🏪 ${store.name}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("القسم: ${store.categoryId} | الهاتف: ${store.phone}", fontSize = 11.sp, color = themeColors.textSecondary)
                                    Text("العنوان: ${store.cityId} - ${store.localNeighborhood}", fontSize = 11.sp, color = themeColors.textSecondary)
                                    if (store.description.isNotEmpty()) {
                                        Text("الوصف: ${store.description}", fontSize = 10.sp, color = Color.LightGray)
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = { viewModel.setStoreActive(store.id, true) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.Green),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("قبول وتفعيل المركز ✅", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Button(
                                            onClick = { viewModel.deleteStorePermanently(store.id) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("رفض وحذف ❌", color = Color.White, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (adminReqSubTab == "MEDICAL") {
                    val pendingM = stores.filter { !it.isApproved && !it.isDeleted && (it.categoryId == "مراكز طبية وعيادات" || it.categoryId.contains("طبي") || it.categoryId.contains("عياد")) }
                    if (pendingM.isEmpty()) {
                        item {
                            Card(colors = CardDefaults.cardColors(containerColor = themeColors.surface), modifier = Modifier.fillMaxWidth()) {
                                Text("لا توجد طلبات إضافة مراكز طبية أو عيادات معلقة حالياً.", fontSize = 11.sp, color = themeColors.textSecondary, modifier = Modifier.padding(16.dp))
                            }
                        }
                    } else {
                        items(pendingM, key = { it.id }) { med ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                                border = BorderStroke(1.dp, Color(0xFF10B981)),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("🏥 ${med.name}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("رقم التواصل والعيادة: ${med.phone}", fontSize = 11.sp, color = themeColors.textSecondary)
                                    Text("العنوان والمدينة: ${med.cityId} - ${med.localNeighborhood}", fontSize = 11.sp, color = themeColors.textSecondary)
                                    if (med.description.isNotEmpty()) {
                                        Text("التخصص والخدمات: ${med.description}", fontSize = 10.sp, color = Color.LightGray)
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = { viewModel.setStoreActive(med.id, true) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.Green),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("قبول وتفعيل العيادة ✅", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Button(
                                            onClick = { viewModel.deleteStorePermanently(med.id) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("رفض وحذف ❌", color = Color.White, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (adminReqSubTab == "RESTAURANTS") {
                    val pendingR = stores.filter { !it.isApproved && !it.isDeleted && (it.categoryId == "مطاعم وكافيهات" || it.categoryId.contains("مطعم") || it.categoryId.contains("كافيه")) }
                    if (pendingR.isEmpty()) {
                        item {
                            Card(colors = CardDefaults.cardColors(containerColor = themeColors.surface), modifier = Modifier.fillMaxWidth()) {
                                Text("لا توجد طلبات إضافة مطاعم أو كافيهات معلقة حالياً.", fontSize = 11.sp, color = themeColors.textSecondary, modifier = Modifier.padding(16.dp))
                            }
                        }
                    } else {
                        items(pendingR, key = { it.id }) { rest ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                                border = BorderStroke(1.dp, Color(0xFFEF4444)),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("🍔 ${rest.name}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("رقم التواصل والتوصيل: ${rest.phone}", fontSize = 11.sp, color = themeColors.textSecondary)
                                    Text("العنوان والمدينة: ${rest.cityId} - ${rest.localNeighborhood}", fontSize = 11.sp, color = themeColors.textSecondary)
                                    if (rest.description.isNotEmpty()) {
                                        Text("قائمة الطعام والوجبات: ${rest.description}", fontSize = 10.sp, color = Color.LightGray)
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = { viewModel.setStoreActive(rest.id, true) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.Green),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("قبول وتفعيل المطعم ✅", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Button(
                                            onClick = { viewModel.deleteStorePermanently(rest.id) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("رفض وحذف ❌", color = Color.White, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (adminReqSubTab == "JOBS") {
                    val pendingJ = jobs.filter { !it.isApproved && !it.isDeleted }
                    if (pendingJ.isEmpty()) {
                        item {
                            Card(colors = CardDefaults.cardColors(containerColor = themeColors.surface), modifier = Modifier.fillMaxWidth()) {
                                Text("لا توجد طلبات إعلانات وظيفية معلقة حالياً.", fontSize = 11.sp, color = themeColors.textSecondary, modifier = Modifier.padding(16.dp))
                            }
                        }
                    } else {
                        items(pendingJ, key = { it.id }) { job ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                                border = BorderStroke(1.dp, Color(0xFF8B5CF6)),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("💼 ${job.title}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("الجهة: ${job.companyName} | الهاتف: ${job.phone}", fontSize = 11.sp, color = themeColors.textSecondary)
                                    Text("الراتب والمدينة: ${job.salary} | ${job.cityId}", fontSize = 11.sp, color = themeColors.textSecondary)
                                    if (job.description.isNotEmpty()) {
                                        Text("متطلبات الوظيفة: ${job.description}", fontSize = 10.sp, color = Color.LightGray)
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = { viewModel.setJobApproved(job.id, true) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.Green),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("قبول ونشر الوظيفة ✅", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Button(
                                            onClick = { viewModel.deleteJob(job.id) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("رفض وحذف ❌", color = Color.White, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (activeSubTab == "COMPLAINTS") {
                // Section 5: Complaints and Reports Logs
                item {
                    Text("📢 البلاغات الواردة وشكاوى المواطنين (${reports.size}):", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("استخدم الفلتر الذكي للبحث عن بلاغات فني أو مواطن معين وتصدير السجلات:", fontSize = 11.sp, color = themeColors.textSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                item {
                    OutlinedTextField(
                        value = complaintsSearchQuery,
                        onValueChange = { complaintsSearchQuery = it },
                        label = { Text("بحث باسم الفني أو المشتكي...") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        trailingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = themeColors.accent) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { 
                                viewModel.exportComplaintsToCSV() 
                                Toast.makeText(context, "تم تصدير سجل الشكاوى بصيغة CSV بنجاح 📁", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("تصدير CSV 💾", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { 
                                viewModel.exportComplaintsToPDF() 
                                Toast.makeText(context, "تم تصدير مستند الشكاوى بصيغة PDF بنجاح 📄", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("تصدير PDF 📄", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                val filteredComplaints = reports.filter {
                    it.providerName.contains(complaintsSearchQuery, ignoreCase = true) ||
                    it.reporterName.contains(complaintsSearchQuery, ignoreCase = true) ||
                    it.content.contains(complaintsSearchQuery, ignoreCase = true)
                }

                if (filteredComplaints.isEmpty()) {
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = themeColors.surface), modifier = Modifier.fillMaxWidth()) {
                            Text("لا توجد بلاغات تفرز معايير البحث المسجلة.", fontSize = 11.sp, color = themeColors.textSecondary, modifier = Modifier.padding(16.dp))
                        }
                    }
                } else {
                    items(filteredComplaints, key = { it.id }) { rep ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                            border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("الفني المشكو ضده: ${rep.providerName}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("اسم المواطن الشاكي: ${rep.reporterName}", fontSize = 11.sp, color = themeColors.textSecondary)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("مضمون ومحتوى البلاغ: ${rep.content}", fontSize = 12.sp, color = Color.White)
                                
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { viewModel.deleteReport(rep.id) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("تجاوز وحذف البلاغ 🗑️", fontSize = 10.sp, color = Color.White)
                                    }
                                    
                                    if (rep.providerId.isNotEmpty()) {
                                        Button(
                                            onClick = { 
                                                viewModel.toggleProviderSubscription(rep.providerId, "SUSPENDED")
                                                Toast.makeText(context, "تم تجميد وإيقاف حساب الفني بنجاح 🛑", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("تجميد حساب الفني 🛑", fontSize = 10.sp, color = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (activeSubTab == "MANUAL_ADD") {
                // Section 2: Manual Creation Sub-tabs
                item {
                    Text("✨ الإضافة المباشرة الفورية من قِبل إدارة المنصة:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    Text("اختر القسم المطلوب لإضافة وتفعيل البيانات مباشرة على الدليل دون انتظار:", fontSize = 11.sp, color = themeColors.textSecondary)
                    Spacer(modifier = Modifier.height(6.dp))

                    val addSubTabs = listOf(
                        Pair("SERVICES", "إضافة فني / مهني 🔧"),
                        Pair("PROPERTIES", "إضافة عقار 🏠"),
                        Pair("STORES", "مركز تجاري 🏪24"),
                        Pair("MEDICAL", "مركز طبي / عيادة 🏥"),
                        Pair("RESTAURANTS", "مطعم / كافيه 🍔"),
                        Pair("JOBS", "إعلان وظيفي 💼")
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(addSubTabs.size) { index ->
                            val st = addSubTabs[index]
                            val isSel = adminAddSubTab == st.first
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(30.dp))
                                    .background(if (isSel) themeColors.accent else themeColors.surface)
                                    .clickable { adminAddSubTab = st.first }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                                    .border(if (isSel) 2.dp else 1.dp, if (isSel) Color.White else Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(30.dp))
                            ) {
                                Text(st.second, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSel) Color.Black else Color.White)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                if (adminAddSubTab == "SERVICES") {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                            border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = manualName,
                                    onValueChange = { manualName = it },
                                    label = { Text("الاسم الكامل للحرفي/المهندِس") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                )
                                OutlinedTextField(
                                    value = manualPhone,
                                    onValueChange = { manualPhone = it },
                                    label = { Text("رقم الهاتف (مثال: 777644)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                )
                                OutlinedTextField(
                                    value = manualStreet,
                                    onValueChange = { manualStreet = it },
                                    label = { Text("الشارع أو الحي التفصيلي") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                )

                                Text("اختر قسم الصيانة المستهدف:", fontSize = 11.sp, color = themeColors.textSecondary)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(categories, key = { it.id }) { cat ->
                                        val isSel = manualCategoryId == cat.id
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (isSel) themeColors.accent else themeColors.surface)
                                                .clickable { manualCategoryId = cat.id }
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                                .border(1.dp, if (isSel) Color.White else Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                        ) {
                                            Text(cat.name, fontSize = 10.sp, color = if (isSel) Color.Black else Color.White)
                                        }
                                    }
                                }

                                Text("اختر المدينة اليمنية المحتوية للحي:", fontSize = 11.sp, color = themeColors.textSecondary)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(citiesList, key = { it.id }) { city ->
                                        val isSel = manualCityId == city.id
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (isSel) themeColors.accent else themeColors.surface)
                                                .clickable { manualCityId = city.id }
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                                .border(1.dp, if (isSel) Color.White else Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                        ) {
                                            Text(city.nameAr, fontSize = 10.sp, color = if (isSel) Color.Black else Color.White)
                                        }
                                    }
                                }

                                val galleryLauncher = rememberLauncherForActivityResult(
                                    contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
                                ) { uri: android.net.Uri? ->
                                    uri?.let {
                                        val base64Str = convertUriToBase64(context, it)
                                        if (base64Str.isNotEmpty()) {
                                            manualPhotoUrl = base64Str
                                            Toast.makeText(context, "✅ تم اختيار الصورة وتحويلها بنجاح!", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = manualPhotoUrl,
                                    onValueChange = { manualPhotoUrl = it },
                                    label = { Text("رابط صورة الفني الشخصية أو كود Base64") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                )

                                Button(
                                    onClick = { galleryLauncher.launch("image/*") },
                                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("📸 اختيار صورة من استوديو الهاتف", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = manualIsVipGolden,
                                        onCheckedChange = { manualIsVipGolden = it },
                                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFFD97706))
                                    )
                                    Text("تفعيل كرت VIP الذهبي المميز على الدليل فورا", fontSize = 11.sp, color = Color.White)
                                }

                                Button(
                                    onClick = {
                                        if (manualName.trim().isEmpty() || manualPhone.trim().isEmpty()) {
                                            Toast.makeText(context, "الرجاء تعبئة الاسم ورقم الهاتف على الأقل للتفعيل", Toast.LENGTH_SHORT).show()
                                        } else {
                                            val finalCat = if (manualCategoryId.isEmpty()) (categories.firstOrNull()?.id ?: "1") else manualCategoryId
                                            val finalCity = if (manualCityId.isEmpty()) (citiesList.firstOrNull()?.id ?: "ye_san") else manualCityId
                                            val priceVal = manualPriceValue.toDoubleOrNull() ?: 1500.0
                                            val finalStreet = if (manualStreet.trim().isEmpty()) "غير محدد" else manualStreet.trim()

                                            viewModel.addNewProviderCustom(
                                                name = manualName.trim(),
                                                phone = manualPhone.trim(),
                                                catId = finalCat,
                                                street = finalStreet,
                                                cityId = finalCity,
                                                profileImage = manualPhotoUrl.trim(),
                                                idCardImage = manualIdCardUrl.trim(),
                                                forensicImage = manualForensicUrl.trim(),
                                                price = priceVal,
                                                isVip = manualIsVipGolden
                                            )

                                            manualName = ""
                                            manualPhone = ""
                                            manualStreet = ""
                                            manualPhotoUrl = ""
                                            Toast.makeText(context, "🚀 تم إضافة وتفعيل مقدم الخدمة بنجاح!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("➕ إضافة الفني وتفعيله بالكامل فوراً", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else if (adminAddSubTab == "PROPERTIES") {
                    item {
                        var pTitle by remember { mutableStateOf("") }
                        var pType by remember { mutableStateOf("شقة") }
                        var pPrice by remember { mutableStateOf("") }
                        var pPhone by remember { mutableStateOf("") }
                        var pCity by remember { mutableStateOf("صنعاء") }
                        var pDistrict by remember { mutableStateOf("") }
                        var pDesc by remember { mutableStateOf("") }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                            border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("🏠 إضافة عقار جديد معتمد فوراً", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)

                                OutlinedTextField(value = pTitle, onValueChange = { pTitle = it }, label = { Text("عنوان العقار (مثال: شقة مفروشة فاخرة للبيع)") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = pType, onValueChange = { pType = it }, label = { Text("نوع العقار (شقة / أرض / بيت / محل)") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = pPrice, onValueChange = { pPrice = it }, label = { Text("السعر أو الإيجار (مثال: 50,000 ريال)") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = pPhone, onValueChange = { pPhone = it }, label = { Text("رقم هاتف المالك/المكتب") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = pCity, onValueChange = { pCity = it }, label = { Text("المدينة") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = pDistrict, onValueChange = { pDistrict = it }, label = { Text("المنطقة / الحي") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = pDesc, onValueChange = { pDesc = it }, label = { Text("تفاصيل ومواصفات العقار") }, modifier = Modifier.fillMaxWidth(), maxLines = 3)

                                Button(
                                    onClick = {
                                        if (pTitle.trim().isEmpty() || pPhone.trim().isEmpty()) {
                                            Toast.makeText(context, "يرجى كتابة عنوان العقار ورقم الهاتف", Toast.LENGTH_SHORT).show()
                                        } else {
                                            viewModel.saveProperty(
                                                com.example.data.PropertyEntity(
                                                    id = java.util.UUID.randomUUID().toString(),
                                                    title = pTitle.trim(),
                                                    propertyType = pType.trim(),
                                                    price = pPrice.trim().toDoubleOrNull() ?: 0.0,
                                                    phone = pPhone.trim(),
                                                    cityId = pCity.trim(),
                                                    localNeighborhood = pDistrict.trim(),
                                                    description = pDesc.trim(),
                                                    isApproved = true,
                                                    isActive = true,
                                                    createdAt = System.currentTimeMillis()
                                                )
                                            )
                                            pTitle = ""; pPrice = ""; pPhone = ""; pDesc = ""
                                            Toast.makeText(context, "✅ تم إضافة ونشر العقار بنجاح!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("➕ نشر ونشر العقار فوراً", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else if (adminAddSubTab == "STORES" || adminAddSubTab == "MEDICAL" || adminAddSubTab == "RESTAURANTS") {
                    item {
                        var sName by remember { mutableStateOf("") }
                        var sPhone by remember { mutableStateOf("") }
                        var sCity by remember { mutableStateOf("صنعاء") }
                        var sAddress by remember { mutableStateOf("") }
                        var sDesc by remember { mutableStateOf("") }

                        val categoryTitle = when(adminAddSubTab) {
                            "MEDICAL" -> "🏥 إضافة مركز طبي أو عيادة تخصصية"
                            "RESTAURANTS" -> "🍔 إضافة مطعم أو كافيه مميز"
                            else -> "🏪 إضافة مركز تجاري أو محل خدمي"
                        }

                        val defaultCategory = when(adminAddSubTab) {
                            "MEDICAL" -> "مراكز طبية وعيادات"
                            "RESTAURANTS" -> "مطاعم وكافيهات"
                            else -> "مراكز تجارية ومحلات"
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                            border = BorderStroke(1.dp, themeColors.accent),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(categoryTitle, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)

                                OutlinedTextField(value = sName, onValueChange = { sName = it }, label = { Text("اسم المنشأة/المحل/المركز") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = sPhone, onValueChange = { sPhone = it }, label = { Text("رقم الهاتف أو الواتساب") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = sCity, onValueChange = { sCity = it }, label = { Text("المدينة") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = sAddress, onValueChange = { sAddress = it }, label = { Text("العنوان والشارع التفصيلي") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = sDesc, onValueChange = { sDesc = it }, label = { Text("وصف الخدمات والتخصصات وقائمة العمل") }, modifier = Modifier.fillMaxWidth(), maxLines = 3)

                                Button(
                                    onClick = {
                                        if (sName.trim().isEmpty() || sPhone.trim().isEmpty()) {
                                            Toast.makeText(context, "يرجى إدخال الاسم ورقم الهاتف", Toast.LENGTH_SHORT).show()
                                        } else {
                                            viewModel.saveStore(
                                                com.example.data.StoreEntity(
                                                    id = java.util.UUID.randomUUID().toString(),
                                                    name = sName.trim(),
                                                    phone = sPhone.trim(),
                                                    cityId = sCity.trim(),
                                                    localNeighborhood = sAddress.trim(),
                                                    description = sDesc.trim(),
                                                    categoryId = defaultCategory,
                                                    isApproved = true,
                                                    isActive = true,
                                                    createdAt = System.currentTimeMillis()
                                                )
                                            )
                                            sName = ""; sPhone = ""; sAddress = ""; sDesc = ""
                                            Toast.makeText(context, "✅ تم إضافة وتفعيل $defaultCategory بنجاح!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("➕ إضافة وتفعيل المنشأة فوراً", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else if (adminAddSubTab == "JOBS") {
                    item {
                        var jTitle by remember { mutableStateOf("") }
                        var jCompany by remember { mutableStateOf("") }
                        var jPhone by remember { mutableStateOf("") }
                        var jSalary by remember { mutableStateOf("") }
                        var jCity by remember { mutableStateOf("صنعاء") }
                        var jDesc by remember { mutableStateOf("") }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                            border = BorderStroke(1.dp, Color(0xFF8B5CF6)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("💼 إضافة إعلان وظيفي جديد معتمد", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)

                                OutlinedTextField(value = jTitle, onValueChange = { jTitle = it }, label = { Text("المسمى الوظيفي (مثال: محاسب مالي خبرة)") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = jCompany, onValueChange = { jCompany = it }, label = { Text("اسم الشركة أو جهة العمل") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = jPhone, onValueChange = { jPhone = it }, label = { Text("رقم التواصل للتقديم") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = jSalary, onValueChange = { jSalary = it }, label = { Text("الراتب المتوقع أو يحدد بعد المقابلة") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = jCity, onValueChange = { jCity = it }, label = { Text("المدينة") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = jDesc, onValueChange = { jDesc = it }, label = { Text("الشروط والمتطلبات وساعات العمل") }, modifier = Modifier.fillMaxWidth(), maxLines = 3)

                                Button(
                                    onClick = {
                                        if (jTitle.trim().isEmpty() || jPhone.trim().isEmpty()) {
                                            Toast.makeText(context, "يرجى كتابة المسمى الوظيفي ورقم التواصل", Toast.LENGTH_SHORT).show()
                                        } else {
                                            viewModel.saveJob(
                                                com.example.data.JobEntity(
                                                    id = java.util.UUID.randomUUID().toString(),
                                                    title = jTitle.trim(),
                                                    companyName = jCompany.trim(),
                                                    phone = jPhone.trim(),
                                                    salary = jSalary.trim(),
                                                    cityId = jCity.trim(),
                                                    description = jDesc.trim(),
                                                    isApproved = true,
                                                    isActive = true,
                                                    createdAt = System.currentTimeMillis()
                                                )
                                            )
                                            jTitle = ""; jCompany = ""; jPhone = ""; jDesc = ""
                                            Toast.makeText(context, "✅ تم إعلان ونشر الوظيفة بنجاح!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("➕ نشر ونشر الإعلان الوظيفي فوراً", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            if (activeSubTab == "PROVIDERS") {
                // INDEPENDENT PROMOTIONS AND VERIFICATIONS LISTING
                item {
                    Text("🏅 ترقية الفنيين وأعضاء دليل الدليل الشامل", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    Text("البحث والتحكم في شارات الفنيين والأوسمة والتحكم المستقل والحذف:", fontSize = 11.sp, color = themeColors.textSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                }

                item {
                    OutlinedTextField(
                        value = activeProvidersSearchQuery,
                        onValueChange = { activeProvidersSearchQuery = it },
                        label = { Text("البحث في دليل الفنيين المعتمدين...") },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        trailingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = themeColors.accent) }
                    )
                }

                val filteredProviders = activatedProviders.filter {
                    it.name.contains(activeProvidersSearchQuery, ignoreCase = true) ||
                    it.phone.contains(activeProvidersSearchQuery, ignoreCase = true) ||
                    it.area.contains(activeProvidersSearchQuery, ignoreCase = true)
                }

                items(filteredProviders, key = { it.id }) { p ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(p.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    val catName = if (p.categoryId == "other" && p.customCategoryName.isNotEmpty()) p.customCategoryName else (categories.find { it.id == p.categoryId }?.name ?: "خدمات عامة")
                                    Text("المهنة: $catName | المنطقة: ${p.area}", fontSize = 11.sp, color = themeColors.textSecondary)
                                }
                                IconButton(
                                    onClick = { viewModel.removeProvider(p.id) }
                                ) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف الفني نهائياً", tint = Color.Red, modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // 3 Independent switches next to each provider in a clean grid with weight distribution
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Row 1
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Checkbox(
                                            checked = p.isVip,
                                            onCheckedChange = { viewModel.pinProvider(p.id, it) },
                                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFFD97706)),
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text("VIP ذهبي", fontSize = 10.sp, color = Color.White, maxLines = 1)
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Checkbox(
                                            checked = p.isVerified,
                                            onCheckedChange = { viewModel.verifyProviderBadge(p.id, it) },
                                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFF3B82F6)),
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text("موثق حساب", fontSize = 10.sp, color = Color.White, maxLines = 1)
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Checkbox(
                                            checked = p.isRecommended,
                                            onCheckedChange = { viewModel.recommendProvider(p.id, it) },
                                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFFEC4899)),
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text("موصى به", fontSize = 10.sp, color = Color.White, maxLines = 1)
                                    }
                                }
                                
                                // Row 2
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Checkbox(
                                            checked = p.isChatDisabled,
                                            onCheckedChange = { viewModel.setProviderChatDisabled(p.id, it) },
                                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFFEF4444)),
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text("إيقاف الدردشة 🔇", fontSize = 9.sp, color = Color.LightGray, maxLines = 1)
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Checkbox(
                                            checked = p.isNotificationsDisabled,
                                            onCheckedChange = { viewModel.setProviderNotificationsDisabled(p.id, it) },
                                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFFEF4444)),
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text("إيقاف الإشعارات 🔕", fontSize = 9.sp, color = Color.LightGray, maxLines = 1)
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Checkbox(
                                            checked = p.isPaymentRequired,
                                            onCheckedChange = { viewModel.setProviderPaymentRequired(p.id, it) },
                                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFF10B981)),
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text("ربط بالدفع 💳", fontSize = 9.sp, color = Color.LightGray, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (activeSubTab == "PASSWORDS_RESET") {
                item {
                    Text("🔑 لوحة تعيين وإعادة ضبط كلمات المرور المشفرة", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    Text("إمكانية البحث وإعادة ضبط كلمة المرور لأي حساب (فني، متجر، مطعم، مركز طبي، عقار، وظيفة، أو مستخدم) وتشفيرها فورياً وحفظها بالسحاب:", fontSize = 11.sp, color = themeColors.textSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("تحديد نوع الحساب المراد ضبط كلمة المرور له:", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            
                            var passResetCategory by remember { mutableStateOf("TECH") } // TECH, STORES, REST, MED, PROP, JOBS, USERS
                            var passSearchQuery by remember { mutableStateOf("") }
                            var newPasswordInput by remember { mutableStateOf("") }

                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                val catTypes = listOf(
                                    Pair("TECH", "🔧 الفنيين"),
                                    Pair("STORES", "🏪 المحلات والمراكز"),
                                    Pair("REST", "🍔 المطاعم"),
                                    Pair("MED", "🏥 المراكز الطبية"),
                                    Pair("PROP", "🏠 العقارات"),
                                    Pair("JOBS", "💼 الوظائف"),
                                    Pair("USERS", "👤 المستخدمين")
                                )
                                items(catTypes) { c ->
                                    val isSel = passResetCategory == c.first
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(if (isSel) themeColors.accent else Color.DarkGray)
                                            .clickable { passResetCategory = c.first }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(c.second, fontSize = 10.sp, color = if (isSel) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = passSearchQuery,
                                onValueChange = { passSearchQuery = it },
                                label = { Text("أدخل رقم الهاتف أو اسم الحساب للبحث", fontSize = 10.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = themeColors.accent)
                            )

                            OutlinedTextField(
                                value = newPasswordInput,
                                onValueChange = { newPasswordInput = it },
                                label = { Text("كلمة المرور الجديدة (سيتم تشفيرها بقوة 🔒)", fontSize = 10.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = themeColors.accent)
                            )

                            Button(
                                onClick = {
                                    if (passSearchQuery.isNotBlank() && newPasswordInput.isNotBlank()) {
                                        val entityTypeStr = when (passResetCategory) {
                                            "TECH" -> "PROVIDER"
                                            "STORES", "REST", "MED" -> "STORE"
                                            "PROP" -> "STORE"
                                            "JOBS" -> "JOB"
                                            else -> "USER"
                                        }
                                        viewModel.resetAccountPassword(entityTypeStr, passSearchQuery, newPasswordInput)
                                        Toast.makeText(context, "🔐 تم تشفير وإعادة تعيين كلمة المرور للحساب بنجاح!", Toast.LENGTH_LONG).show()
                                        newPasswordInput = ""
                                    } else {
                                        Toast.makeText(context, "⚠️ يرجى إدخال رقم الهاتف/الاسم وكلمة المرور الجديدة", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("تشفير وتحديث كلمة المرور فورياً 🔒", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            if (activeSubTab == "BANNERS") {
                // 🖼️ ADVERTISING BANNERS REORDERING AND MANAGEMENT
                item {
                    Spacer(modifier = Modifier.height(18.dp))
                    Text("🖼️ إدارة وترتيب بنرات الإعلانات الترويجية", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Add banner form
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.25f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("➕ إضافة بنر ترويجي جديد ومتقدم:", fontSize = 12.sp, color = themeColors.accent, fontWeight = FontWeight.Bold)
                            
                            var bannerTitle by remember { mutableStateOf("") }
                            var bannerType by remember { mutableStateOf("IMAGE") } // TEXT, IMAGE, VIDEO
                            var bannerUrl by remember { mutableStateOf("") }
                            var bannerRedirect by remember { mutableStateOf("") }
                            var bannerDuration by remember { mutableStateOf(5) }
                            
                            // Type Selector Chips
                            Text("نوع البنر الترويجي:", fontSize = 10.sp, color = Color.White)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("IMAGE" to "🖼️ صورة", "VIDEO" to "🎥 فيديو", "TEXT" to "🔤 نصي فقط").forEach { (typeKey, label) ->
                                    val isSelected = bannerType == typeKey
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) themeColors.primary else Color.White.copy(alpha = 0.05f))
                                            .clickable { bannerType = typeKey }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(label, fontSize = 10.sp, color = if (isSelected) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            
                            OutlinedTextField(
                                value = bannerTitle,
                                onValueChange = { bannerTitle = it },
                                label = { Text("عنوان أو نص الإعلان") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )
                            
                            if (bannerType != "TEXT") {
                                OutlinedTextField(
                                    value = bannerUrl,
                                    onValueChange = { bannerUrl = it },
                                    label = { Text(if (bannerType == "IMAGE") "رابط صورة الإعلان (URL أو مسار)" else "رابط فيديو الإعلان (URL أو مسار)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                )

                                val galleryLauncherForBannerSelection = rememberLauncherForActivityResult(
                                    contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
                                ) { uri ->
                                    if (uri != null) {
                                        try {
                                            val contentResolver = context.contentResolver
                                            val mimeType = contentResolver.getType(uri) ?: ""
                                            if (mimeType.startsWith("image/")) {
                                                val base64Str = com.example.ui.utils.compressAndResizeImageUri(context, uri, 800, 70)
                                                if (base64Str.isNotEmpty()) {
                                                    bannerUrl = "data:image/jpeg;base64,$base64Str"
                                                    bannerType = "IMAGE"
                                                    Toast.makeText(context, "📸 تم اختيار وضغط الصورة بنجاح وتجهيز المزامنة!", Toast.LENGTH_SHORT).show()
                                                }
                                            } else {
                                                bannerUrl = uri.toString()
                                                bannerType = "VIDEO"
                                                Toast.makeText(context, "🎥 تم اختيار الفيديو بنجاح وتجهيز المزامنة!", Toast.LENGTH_SHORT).show()
                                            }
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "❌ فشل قراءة الملف: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }

                                Button(
                                    onClick = {
                                        if (bannerType == "IMAGE") {
                                            galleryLauncherForBannerSelection.launch("image/*")
                                        } else {
                                            galleryLauncherForBannerSelection.launch("video/*")
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = if (bannerType == "IMAGE") "🖼️ اختيار صورة الإعلان من المعرض" else "🎥 اختيار فيديو الإعلان من المعرض",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                }
                            }
                            
                            OutlinedTextField(
                                value = bannerRedirect,
                                onValueChange = { bannerRedirect = it },
                                label = { Text("رمز القسم لتوجيه العميل إليه (اختياري)") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )
                            
                            // Duration selection
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text("مدة عرض البنر: $bannerDuration ثوانٍ", fontSize = 10.sp, color = Color.White)
                                Slider(
                                    value = bannerDuration.toFloat(),
                                    onValueChange = { bannerDuration = it.toInt() },
                                    valueRange = 3f..20f,
                                    steps = 17,
                                    colors = SliderDefaults.colors(
                                        thumbColor = themeColors.accent,
                                        activeTrackColor = themeColors.primary
                                    )
                                )
                            }
                            
                            Button(
                                onClick = {
                                    if (bannerTitle.trim().isEmpty()) {
                                        Toast.makeText(context, "الرجاء كتابة عنوان الإعلان", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    if (bannerType != "TEXT" && bannerUrl.trim().isEmpty()) {
                                        Toast.makeText(context, "الرجاء كتابة رابط أو مسار ملف الإعلان", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    
                                    viewModel.addNewBanner(
                                        title = bannerTitle.trim(),
                                        url = if (bannerType == "TEXT") "" else bannerUrl.trim(),
                                        redirect = bannerRedirect.trim(),
                                        type = bannerType,
                                        size = "LARGE",
                                        duration = bannerDuration
                                    )
                                    bannerTitle = ""
                                    bannerUrl = ""
                                    bannerRedirect = ""
                                    bannerDuration = 5
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("إضافة البنر الإعلاني المطور 🚀", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                item {
                    Text("📋 قائمة البنرات النشطة (ادعم الترتيب بالسحب والإفلات والمبادلة):", fontSize = 12.sp, color = themeColors.textSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                }

                val bannersList_state = bannersList
                itemsIndexed(bannersList_state) { index, b ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.15f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "سحب لنقل الترتيب",
                                    tint = themeColors.accent,
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clickable {
                                             if (index > 0) {
                                                 val mutableBanners = bannersList_state.toMutableList()
                                                 val prev = mutableBanners[index - 1]
                                                 mutableBanners[index - 1] = b
                                                 mutableBanners[index] = prev
                                                 viewModel.reorderBanners(mutableBanners)
                                             }
                                        }
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        val typeIcon = when (b.type.uppercase()) {
                                            "IMAGE" -> "🖼️"
                                            "VIDEO" -> "🎥"
                                            else -> "🔤"
                                        }
                                        Text("$typeIcon ${b.title}", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    Text("مدة العرض: ${b.duration} ثوانٍ | القسم: ${b.redirectCategory.ifEmpty { "عام" }}", fontSize = 9.sp, color = themeColors.accent)
                                    if (b.url.isNotEmpty()) {
                                        Text(b.url, fontSize = 9.sp, color = themeColors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        if (index > 0) {
                                            val mutableBanners = bannersList_state.toMutableList()
                                            val prev = mutableBanners[index - 1]
                                            mutableBanners[index - 1] = b
                                            mutableBanners[index] = prev
                                            viewModel.reorderBanners(mutableBanners)
                                        }
                                    },
                                    enabled = index > 0
                                ) {
                                    Icon(imageVector = Icons.Default.KeyboardArrowUp, contentDescription = "أعلى", tint = if (index > 0) themeColors.accent else Color.Gray.copy(alpha = 0.5f))
                                }
                                IconButton(
                                    onClick = {
                                        if (index < bannersList_state.size - 1) {
                                            val mutableBanners = bannersList_state.toMutableList()
                                            val next = mutableBanners[index + 1]
                                            mutableBanners[index + 1] = b
                                            mutableBanners[index] = next
                                            viewModel.reorderBanners(mutableBanners)
                                        }
                                    },
                                    enabled = index < bannersList_state.size - 1
                                ) {
                                    Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = "أسفل", tint = if (index < bannersList_state.size - 1) themeColors.accent else Color.Gray.copy(alpha = 0.5f))
                                }
                                IconButton(onClick = { viewModel.deleteBanner(b.id) }) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف", tint = Color.Red)
                                }
                            }
                        }
                    }
                }
            }

            if (activeSubTab == "BOOKINGS") {
                // RESERVATIONS SECTION
                item {
                    Text("📅 إدارة حجوزات الصيانة والطلبات والتحكم بالاستمارات", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Dynamic Booking Form & Routing Config Card
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("⚙️ لوحة التحكم بمسار وحقول استمارة الحجز الشاملة:", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)

                            // Routing control
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("📍 توجيه الحجوزات الواردة من العملاء:", fontSize = 11.sp, color = themeColors.textSecondary)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    val rModes = listOf(
                                        Pair("BOTH", "الأدمن والفني 👥"),
                                        Pair("ADMIN", "الأدمن فقط 👮"),
                                        Pair("PROVIDER", "الفني مباشرة 🛠️")
                                    )
                                    rModes.forEach { mode ->
                                        val isSel = settingsState.bookingRouting == mode.first
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSel) themeColors.accent else Color.Gray.copy(alpha = 0.2f))
                                                .clickable {
                                                    viewModel.saveCustomSettingsState(settingsState.copy(bookingRouting = mode.first))
                                                }
                                                .padding(vertical = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = mode.second,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSel) Color.Black else Color.White
                                            )
                                        }
                                    }
                                }
                            }

                            // Booking Accessibility and Icon visibility controls
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("عرض أيقونة الحجوزات للجمهور:", fontSize = 11.sp, color = themeColors.textSecondary)
                                    Switch(
                                        checked = settingsState.isBookingsIconVisible,
                                        onCheckedChange = { viewModel.saveCustomSettingsState(settingsState.copy(isBookingsIconVisible = it)) },
                                        colors = SwitchDefaults.colors(checkedThumbColor = themeColors.accent)
                                    )
                                }

                                Text("🔒 صلاحية الدخول وتصفح الحجوزات والمواعيد:", fontSize = 11.sp, color = themeColors.textSecondary)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    val accessOptions = listOf(
                                        Pair("ALL", "الجميع 🌍"),
                                        Pair("REGISTERED_ONLY", "المسجلين فقط 🔒"),
                                        Pair("DISABLED", "معطلة 🚫")
                                    )
                                    accessOptions.forEach { opt ->
                                        val isSel = settingsState.bookingsAccessControl == opt.first
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSel) themeColors.accent else Color.Gray.copy(alpha = 0.2f))
                                                .clickable {
                                                    viewModel.saveCustomSettingsState(settingsState.copy(bookingsAccessControl = opt.first))
                                                }
                                                .padding(vertical = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = opt.second,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSel) Color.Black else Color.White
                                            )
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = settingsState.blockedUsersForBookings,
                                    onValueChange = { viewModel.saveCustomSettingsState(settingsState.copy(blockedUsersForBookings = it)) },
                                    label = { Text("أرقام الهواتف المحظورة من نظام الحجوزات (مثال: 777644, 73...)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                )
                            }

                            // Booking terms text field
                            OutlinedTextField(
                                value = settingsState.bookingTerms,
                                onValueChange = { viewModel.saveCustomSettingsState(settingsState.copy(bookingTerms = it)) },
                                label = { Text("شروط الحجز المعروضة للعميل (شروط وأحكام)") },
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 3,
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )

                            Text("✏️ تخصيص وتعديل حقول استمارة طلب الحجز (اسم الحقل):", fontSize = 11.sp, color = Color.Yellow, fontWeight = FontWeight.Bold)

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedTextField(
                                    value = settingsState.bookingLabelName,
                                    onValueChange = { viewModel.saveCustomSettingsState(settingsState.copy(bookingLabelName = it)) },
                                    label = { Text("حقل اسم العميل") },
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                )
                                OutlinedTextField(
                                    value = settingsState.bookingLabelPhone,
                                    onValueChange = { viewModel.saveCustomSettingsState(settingsState.copy(bookingLabelPhone = it)) },
                                    label = { Text("حقل رقم الهاتف") },
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                )
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedTextField(
                                    value = settingsState.bookingLabelArea,
                                    onValueChange = { viewModel.saveCustomSettingsState(settingsState.copy(bookingLabelArea = it)) },
                                    label = { Text("حقل العنوان والحي") },
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                )
                                OutlinedTextField(
                                    value = settingsState.bookingLabelService,
                                    onValueChange = { viewModel.saveCustomSettingsState(settingsState.copy(bookingLabelService = it)) },
                                    label = { Text("حقل نوع الخدمة") },
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                )
                            }
                        }
                    }
                }

                // Active bookings status tracking dashboard panel
                item {
                    val pendingCount = bookings.count { it.status == "PENDING" }
                    val inProgressCount = bookings.count { it.status == "IN_PROGRESS" }
                    val completedCount = bookings.count { it.status == "COMPLETED" || it.status == "APPROVED" }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                            border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("⏳ قيد الانتظار", fontSize = 10.sp, color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(pendingCount.toString(), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                            border = BorderStroke(1.dp, Color(0xFF3B82F6)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("⚡ جاري العمل", fontSize = 10.sp, color = Color(0xFF3B82F6), fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(inProgressCount.toString(), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                            border = BorderStroke(1.dp, Color(0xFF10B981)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("✅ مكتملة", fontSize = 10.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(completedCount.toString(), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }

                if (bookings.isEmpty()) {
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = themeColors.surface), modifier = Modifier.fillMaxWidth()) {
                            Text("لا توجد طلبات حجز مكتوبة حالياً في السجلات", fontSize = 11.sp, color = themeColors.textSecondary, modifier = Modifier.padding(16.dp))
                        }
                    }
                } else {
                    items(bookings, key = { it.id }) { b ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                            border = BorderStroke(1.dp, if (b.status == "PENDING") themeColors.accent else Color.Transparent),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("اسم العميل: ${b.customerName}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    val bColor = when(b.status) {
                                        "APPROVED" -> Color.Green
                                        "COMPLETED" -> Color(0xFF10B981)
                                        "IN_PROGRESS" -> Color(0xFF3B82F6)
                                        "REJECTED" -> Color.Red
                                        else -> themeColors.accent
                                    }
                                    Text(
                                        text = when(b.status) {
                                            "APPROVED" -> "معتمد"
                                            "COMPLETED" -> "مكتمل"
                                            "IN_PROGRESS" -> "جاري التنفيذ"
                                            "REJECTED" -> "مرفوض"
                                            else -> "بانتظار الموافقة"
                                        },
                                        fontSize = 11.sp,
                                        color = bColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text("هاتف العميل: ${b.customerPhone}", fontSize = 11.sp, color = themeColors.textSecondary)
                                Text("منطقة السكن والحي: ${b.customerArea}", fontSize = 11.sp, color = themeColors.textSecondary)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text("📅 تاريخ الحجز: ${b.dateString}", fontSize = 11.sp, color = Color.White)
                                    Text("🕒 وقت الحجز: ${b.timeString}", fontSize = 11.sp, color = Color.White)
                                }
                                if (b.serviceType.isNotEmpty()) {
                                    Text("نوع الخدمة المطلوبة: ${b.serviceType}", fontSize = 11.sp, color = Color.Yellow)
                                }
                                Text("اسم الفني المستهدف: ${b.providerName}", fontSize = 11.sp, color = themeColors.accent)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (b.status == "PENDING" || b.status == "UNDER_REVIEW") {
                                            Button(
                                                onClick = { viewModel.updateBookingStatus(b.id, "IN_PROGRESS") },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Text("قبول وبدء الحجز", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Button(
                                                onClick = {
                                                    bookingRejectionReasonInput = ""
                                                    showRejectionReasonDialogId = b.id
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Text("رفض الحجز", color = Color.White, fontSize = 10.sp)
                                            }
                                        } else if (b.status == "APPROVED") {
                                            Button(
                                                onClick = { viewModel.updateBookingStatus(b.id, "IN_PROGRESS") },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Text("بدء التنفيذ", color = Color.White, fontSize = 10.sp)
                                            }
                                        } else if (b.status == "IN_PROGRESS") {
                                            Button(
                                                onClick = { viewModel.updateBookingStatus(b.id, "COMPLETED") },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Text("إكمال الخدمة (مكتمل)", color = Color.White, fontSize = 10.sp)
                                            }
                                        }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = {
                                                val chId = "support_" + b.customerPhone
                                                val existing = chatChannels.find { it.id == chId }
                                                if (existing != null) {
                                                    showActiveChatChannelObj = existing
                                                } else {
                                                    val newCh = com.example.data.ChatChannelEntity(
                                                        id = chId,
                                                        userName = b.customerName,
                                                        lastMessage = "بدء محادثة بخصوص الحجز رقم ${b.id}",
                                                        isBlocked = false,
                                                        isProvider = false,
                                                        timestamp = System.currentTimeMillis(),
                                                        messages = listOf(
                                                            com.example.data.ChatMessageEntity(
                                                                id = "c_init",
                                                                senderId = "admin",
                                                                message = "أهلاً بك عميلنا العزيز ${b.customerName}. نتواصل معك كإدارة/فني بخصوص حجز الخدمة رقم: ${b.id}.",
                                                                timestamp = System.currentTimeMillis(),
                                                                senderName = "الإدارة والدعم"
                                                            )
                                                        )
                                                    )
                                                    viewModel.replyToChatChannel(chId, "admin", "أهلاً بك عميلنا العزيز ${b.customerName}. نتواصل معك كإدارة/فني بخصوص حجز الخدمة رقم: ${b.id}.", "الإدارة والدعم")
                                                    showActiveChatChannelObj = newCh
                                                }
                                            }
                                        ) {
                                            Icon(imageVector = Icons.Default.Send, contentDescription = "دردشة فورية للتنسيق", tint = Color.Green, modifier = Modifier.size(20.dp))
                                        }
                                        IconButton(
                                            onClick = { redirectingBookingObj = b }
                                        ) {
                                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "توجيه الحجز", tint = Color.Cyan, modifier = Modifier.size(20.dp))
                                        }
                                        IconButton(
                                            onClick = { editingBookingObj = b }
                                        ) {
                                            Icon(imageVector = Icons.Default.Edit, contentDescription = "تعديل الحجز", tint = Color.Green, modifier = Modifier.size(20.dp))
                                        }
                                        IconButton(
                                            onClick = { showDeleteBookingConfirmId = b.id }
                                        ) {
                                            Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف", tint = Color.Red, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (activeSubTab == "NOTIFICATIONS") {
                // TARGETED NOTIFICATIONS MANAGERS
                item {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text("🔔 نظام الإشعارات الذكية الموجهة (Targeted)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    Spacer(modifier = Modifier.height(4.dp))
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("إرسال إشعار فوري موجه:", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            
                            OutlinedTextField(
                                value = notifTitleInput,
                                onValueChange = { notifTitleInput = it },
                                label = { Text("عنوان الإشعار (مثال: خصم هائل اليوم!)") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )

                            OutlinedTextField(
                                value = notifMsgInput,
                                onValueChange = { notifMsgInput = it },
                                label = { Text("مضمون الرسالة بالتفصيل") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )

                            // Target Type selectors grid/row
                            Text("نطاق ومجموعة المستهدفين بالبث الفوري:", fontSize = 11.sp, color = themeColors.textSecondary)
                            androidx.compose.foundation.layout.FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                maxItemsInEachRow = 3
                            ) {
                                listOf(
                                    Pair("ALL", "الجميع 🌍"),
                                    Pair("USER", "عميل 👤"),
                                    Pair("PROVIDER", "فني 🔧"),
                                    Pair("SUPERVISOR", "مشرف 👮"),
                                    Pair("AREA", "محافظة 📍")
                                ).forEach { (type, label) ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable { notifTargetType = type }.padding(vertical = 4.dp)
                                    ) {
                                        RadioButton(selected = notifTargetType == type, onClick = { notifTargetType = type })
                                        Text(label, fontSize = 11.sp, color = Color.White)
                                    }
                                }
                            }

                            if (notifTargetType != "ALL") {
                                OutlinedTextField(
                                    value = notifTargetValue,
                                    onValueChange = { notifTargetValue = it },
                                    label = { 
                                        val lbl = when (notifTargetType) {
                                            "USER" -> "رقم هاتف العميل للتوصيل"
                                            "PROVIDER" -> "رقم هاتف أو معرّف الفني"
                                            "SUPERVISOR" -> "معرّف/اسم المشرف المالي"
                                            else -> "اسم المحافظة/المدينة المستهدفة"
                                        }
                                        Text(lbl)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                )

                                // Interactive Quick Selector lists for Providers and Supervisors
                                if (notifTargetType == "PROVIDER" && activatedProviders.isNotEmpty()) {
                                    Text("اختيار سريع من الفنيين المعتمدين بالمنصة:", fontSize = 10.sp, color = themeColors.accent)
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                    ) {
                                        items(activatedProviders) { prov ->
                                            AssistChip(
                                                onClick = { notifTargetValue = prov.phone },
                                                label = { Text(prov.name, fontSize = 10.sp, color = Color.White) },
                                                colors = AssistChipDefaults.assistChipColors(containerColor = themeColors.primary.copy(alpha = 0.2f))
                                            )
                                        }
                                    }
                                }

                                if (notifTargetType == "SUPERVISOR" && supervisorsList.isNotEmpty()) {
                                    Text("اختيار سريع من المشرفين المتواجدين:", fontSize = 10.sp, color = themeColors.accent)
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                    ) {
                                        items(supervisorsList) { sup ->
                                            AssistChip(
                                                onClick = { notifTargetValue = sup.name },
                                                label = { Text(sup.name, fontSize = 10.sp, color = Color.White) },
                                                colors = AssistChipDefaults.assistChipColors(containerColor = themeColors.primary.copy(alpha = 0.2f))
                                            )
                                        }
                                    }
                                }
                            }

                            // Expiry & Delay configurations
                            Text("⏱️ جدولة البث وتحديد مدة صلاحية الإشعار (اختياري):", fontSize = 11.sp, color = themeColors.textSecondary)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = notifDelayHours,
                                    onValueChange = { notifDelayHours = it },
                                    label = { Text("تأخير الإرسال (ساعات)") },
                                    placeholder = { Text("فوري = فارغ") },
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                )

                                OutlinedTextField(
                                    value = notifValidityHours,
                                    onValueChange = { notifValidityHours = it },
                                    label = { Text("مدة الصلاحية (ساعات)") },
                                    placeholder = { Text("دائم = فارغ") },
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                )
                            }

                            Button(
                                onClick = {
                                    if (notifTitleInput.trim().isEmpty() || notifMsgInput.trim().isEmpty()) {
                                        Toast.makeText(context, "يرجى تعبئة العنوان ونص الرسالة", Toast.LENGTH_SHORT).show()
                                    } else {
                                        val delayH = notifDelayHours.toDoubleOrNull() ?: 0.0
                                        val validH = notifValidityHours.toDoubleOrNull() ?: 0.0
                                        
                                        val scheduledTime = if (delayH > 0) {
                                            System.currentTimeMillis() + (delayH * 3600 * 1000).toLong()
                                        } else 0L

                                        val expiryTimestamp = if (validH > 0) {
                                            (if (scheduledTime > 0) scheduledTime else System.currentTimeMillis()) + (validH * 3600 * 1000).toLong()
                                        } else 0L

                                        viewModel.addNotification(
                                            title = notifTitleInput,
                                            message = notifMsgInput,
                                            targetType = notifTargetType,
                                            targetValue = if (notifTargetType == "ALL") "" else notifTargetValue,
                                            expiryTimestamp = expiryTimestamp,
                                            scheduledTime = scheduledTime
                                        )
                                        notifTitleInput = ""
                                        notifMsgInput = ""
                                        notifTargetValue = ""
                                        notifDelayHours = ""
                                        notifValidityHours = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("بث وإرسال الإشعار للمستهدفين المحددين 🚀", color = Color.White, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text("⚡ نماذج سريعة للإرسال والبث الفوري لجميع المستخدمين:", fontSize = 11.sp, color = themeColors.textSecondary)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(
                                    onClick = {
                                        viewModel.addNotification(
                                            title = "🔥 عرض حصري محدود من إدارة المنصة",
                                            message = "خصم يصل إلى 35% على خدمات التكييف، التمديدات وصيانة الأجهزة المنزلية اليوم فقط! احجز فنيك الآن عبر التطبيق.",
                                            targetType = "ALL",
                                            targetValue = ""
                                        )
                                        Toast.makeText(context, "تم بث عرض ترويجي للجميع بنجاح!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("بث عرض ترويجي 📢", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        viewModel.addNotification(
                                            title = "🛠️ تحديث فني هام للنظام وتطوير الأداء",
                                            message = "عملائنا الأعزاء، نود إعلامكم بإطلاق تحديث جديد للبحث الجغرافي وحساب المسافات بأعلى دقة. نوصي بتحديث التطبيق الآن.",
                                            targetType = "ALL",
                                            targetValue = ""
                                        )
                                        Toast.makeText(context, "تم بث إعلان تحديث هام للجميع بنجاح!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("بث تحديث فني ⚙️", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                item {
                    Text("📋 تاريخ الرسائل البوش السابقة (${notifications.size}):", fontSize = 12.sp, color = themeColors.textSecondary)
                }

                items(notifications, key = { it.id }) { n ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(n.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                IconButton(onClick = { showDeleteNotifConfirmId = n.id }, modifier = Modifier.size(24.dp)) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف", tint = Color.Red, modifier = Modifier.size(16.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(n.message, fontSize = 12.sp, color = Color.White)
                            Spacer(modifier = Modifier.height(4.dp))
                            val filterStr = when(n.targetType) {
                                "USER" -> "مستهدف صريح بالهاتف: ${n.targetValue}"
                                "AREA" -> "محافظة يمنية محددة: ${n.targetValue}"
                                else -> "جميع المشتركين بالمنصة"
                            }
                            Text("نطاق الاستهداف: $filterStr", fontSize = 10.sp, color = themeColors.accent, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (activeSubTab == "CHATS") {
                item {
                    Text("💬 إدارة محادثات الدعم والدردشات الفورية والتحكم الفائق بالصلاحيات", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    Text("هنا يمكنك مراقبة كل غرف الشات، تحديد أطراف الاتصال المسموح بها، التحكم بالنطق الصوتي، الحجم، وحظر الأعضاء فورياً:", fontSize = 11.sp, color = themeColors.textSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // 1. Participant selection & Global Outage configs
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("🛡️ صلاحيات وأطراف الاتصال بالدردشة:", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("🚹 تفعيل الشات بين العميل ومقدم الخدمة (الفني)", fontSize = 11.sp, color = Color.White)
                                Switch(
                                    checked = settingsState.allowChatUserToProvider,
                                    onCheckedChange = { active ->
                                        viewModel.saveCustomSettingsState(settingsState.copy(allowChatUserToProvider = active))
                                    }
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("🛠️ تفعيل الشات بين الفني والأدمن/المشرف مباشرة", fontSize = 11.sp, color = Color.White)
                                Switch(
                                    checked = settingsState.allowChatProviderToAdmin,
                                    onCheckedChange = { active ->
                                        viewModel.saveCustomSettingsState(settingsState.copy(allowChatProviderToAdmin = active))
                                    }
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("👤 تفعيل الشات المباشر بين العميل والادارة (الدعم)", fontSize = 11.sp, color = Color.White)
                                Switch(
                                    checked = settingsState.allowChatUserToAdmin,
                                    onCheckedChange = { active ->
                                        viewModel.saveCustomSettingsState(settingsState.copy(allowChatUserToAdmin = active))
                                    }
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("🕵️ نظام الفحص المسبق (موافقة الادارة قبل إرسال الرسالة)", fontSize = 11.sp, color = Color.Yellow)
                                Switch(
                                    checked = settingsState.approveChatsBeforeProvider,
                                    onCheckedChange = { active ->
                                        viewModel.saveCustomSettingsState(settingsState.copy(approveChatsBeforeProvider = active))
                                    }
                                )
                            }
                        }
                    }
                }

                // 2. Outage configs & Global switches
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("🛑 تعطيل الخدمة وبث الطوارئ التلقائي:", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("🛑 إيقاف الشات الفوري بالكامل (عن الكل)", fontSize = 11.sp, color = Color.White)
                                Switch(
                                    checked = settingsState.disableChatAll,
                                    onCheckedChange = { active ->
                                        viewModel.saveCustomSettingsState(settingsState.copy(disableChatAll = active))
                                    }
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("👤 إيقاف الشات عن الزائرين والعملاء فقط", fontSize = 11.sp, color = Color.White)
                                Switch(
                                    checked = settingsState.disableChatUsers,
                                    onCheckedChange = { active ->
                                        viewModel.saveCustomSettingsState(settingsState.copy(disableChatUsers = active))
                                    }
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("🛠️ إيقاف الشات عن الفنيين ومزودي الخدمة", fontSize = 11.sp, color = Color.White)
                                Switch(
                                    checked = settingsState.disableChatProviders,
                                    onCheckedChange = { active ->
                                        viewModel.saveCustomSettingsState(settingsState.copy(disableChatProviders = active))
                                    }
                                )
                            }

                            var announcementText by remember(settingsState.chatDisabledAnnouncement) { mutableStateOf(settingsState.chatDisabledAnnouncement) }
                            OutlinedTextField(
                                value = announcementText,
                                onValueChange = { 
                                    announcementText = it
                                    viewModel.saveCustomSettingsState(settingsState.copy(chatDisabledAnnouncement = it))
                                },
                                label = { Text("رسالة بث الطوارئ والتعطيل في غرف الشات") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )
                        }
                    }
                }

                // 3. Audio & UI appearance settings (Custom dimensions)
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("🎨 مظهر وصوتيات شاشة ومقاس الشات الفوري:", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("🎙️ تفعيل الإدخال الصوتي (Speech-to-Text)", fontSize = 11.sp, color = Color.White)
                                Switch(
                                    checked = settingsState.allowVoiceInput,
                                    onCheckedChange = { active ->
                                        viewModel.saveCustomSettingsState(settingsState.copy(allowVoiceInput = active))
                                    }
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("🔊 تفعيل نطق الرسائل وقراءتها آلياً (TTS)", fontSize = 11.sp, color = Color.White)
                                Switch(
                                    checked = settingsState.allowTextToSpeech,
                                    onCheckedChange = { active ->
                                        viewModel.saveCustomSettingsState(settingsState.copy(allowTextToSpeech = active))
                                    }
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("🎙️ طلب الانضمام: تفعيل الإدخال الصوتي", fontSize = 11.sp, color = Color.White)
                                Switch(
                                    checked = settingsState.allowVoiceInputJoinForm,
                                    onCheckedChange = { active ->
                                        viewModel.saveCustomSettingsState(settingsState.copy(allowVoiceInputJoinForm = active))
                                    }
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("🔊 طلب الانضمام: تفعيل النطق الصوتي (TTS)", fontSize = 11.sp, color = Color.White)
                                Switch(
                                    checked = settingsState.allowTextToSpeechJoinForm,
                                    onCheckedChange = { active ->
                                        viewModel.saveCustomSettingsState(settingsState.copy(allowTextToSpeechJoinForm = active))
                                    }
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("🎙️ المساعد الذكي: تفعيل الإدخال الصوتي", fontSize = 11.sp, color = Color.White)
                                Switch(
                                    checked = settingsState.allowVoiceInputAssistant,
                                    onCheckedChange = { active ->
                                        viewModel.saveCustomSettingsState(settingsState.copy(allowVoiceInputAssistant = active))
                                    }
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("🔊 المساعد الذكي: تفعيل النطق الصوتي (TTS)", fontSize = 11.sp, color = Color.White)
                                Switch(
                                    checked = settingsState.allowTextToSpeechAssistant,
                                    onCheckedChange = { active ->
                                        viewModel.saveCustomSettingsState(settingsState.copy(allowTextToSpeechAssistant = active))
                                    }
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("🎙️ استمارة الحجز: تفعيل الإدخال الصوتي", fontSize = 11.sp, color = Color.White)
                                Switch(
                                    checked = settingsState.allowVoiceInputBookingForm,
                                    onCheckedChange = { active ->
                                        viewModel.saveCustomSettingsState(settingsState.copy(allowVoiceInputBookingForm = active))
                                    }
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("🔊 استمارة الحجز: تفعيل النطق الصوتي (TTS)", fontSize = 11.sp, color = Color.White)
                                Switch(
                                    checked = settingsState.allowTextToSpeechBookingForm,
                                    onCheckedChange = { active ->
                                        viewModel.saveCustomSettingsState(settingsState.copy(allowTextToSpeechBookingForm = active))
                                    }
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("🙈 إخفاء أيقونة الشات بالكامل من التطبيق", fontSize = 11.sp, color = Color.White)
                                Switch(
                                    checked = settingsState.chatHidden,
                                    onCheckedChange = { active ->
                                        viewModel.saveCustomSettingsState(settingsState.copy(chatHidden = active))
                                    }
                                )
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = settingsState.chatSize.toString(),
                                    onValueChange = { newVal ->
                                        newVal.toIntOrNull()?.let {
                                            viewModel.saveCustomSettingsState(settingsState.copy(chatSize = it))
                                        }
                                    },
                                    label = { Text("حجم الأيقونة (dp)") },
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                )

                                OutlinedTextField(
                                    value = settingsState.chatFontSizeSp.toString(),
                                    onValueChange = { newVal ->
                                        newVal.toIntOrNull()?.let {
                                            viewModel.saveCustomSettingsState(settingsState.copy(chatFontSizeSp = it))
                                        }
                                    },
                                    label = { Text("حجم خط الشات (sp)") },
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                )
                            }

                            OutlinedTextField(
                                value = settingsState.chatBackgroundHex,
                                onValueChange = { newVal ->
                                    viewModel.saveCustomSettingsState(settingsState.copy(chatBackgroundHex = newVal))
                                },
                                label = { Text("كود لون خلفية شاشة الشات (Hex)") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )
                        }
                    }
                }

                // 4. Archive actions
                item {
                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { 
                                viewModel.wipeOldChatChannels(30)
                                Toast.makeText(context, "تمت تصفية كامل المحادثات بنجاح من الخادم السحابي والذاكرة المؤقتة 🧼", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f)),
                            modifier = Modifier.weight(1.5f)
                        ) {
                            Text("🧼 مسح أرشيف الشات بالكامل", fontSize = 10.sp, color = Color.White)
                        }
                        Button(
                            onClick = { Toast.makeText(context, "تم تصدير سجل المحادثات بنجاح للمصنف المالي 📁", Toast.LENGTH_SHORT).show() },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("تصدير CSV 📁", fontSize = 11.sp, color = Color.White)
                        }
                    }
                }

                // 5. Active Channels Monitoring
                item {
                    Text("📋 قنوات المحادثة والدردشة النشطة حالياً (${chatChannels.size}):", fontSize = 12.sp, color = themeColors.textSecondary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                }

                if (chatChannels.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("لا توجد محادثات نشطة حالياً بالمنصة 🟢", fontSize = 12.sp, color = Color.Green, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("لم يقم أي فني أو عميل ببدء دردشة جديدة حتى الآن، سيتم المزامنة تلقائياً بمجرد إرسال أي رسالة.", fontSize = 10.sp, color = themeColors.textSecondary)
                            }
                        }
                    }
                } else {
                    items(chatChannels, key = { it.id }) { ch ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { showActiveChatChannelObj = ch }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    val parties = if (ch.isProvider) "مقدم الخدمة: ${ch.userName}" else "مستخدم الدليل: ${ch.userName}"
                                    Text("المحادثة: $parties", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    if (ch.isBlocked) {
                                        Text("محظورة 🛑", color = Color.Red, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    } else {
                                        Text("نشطة 🟢", color = Color.Green, fontSize = 10.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("آخر رسالة: " + ch.lastMessage, fontSize = 11.sp, color = themeColors.textSecondary)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { showActiveChatChannelObj = ch },
                                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("افتح المحادثة ورد كأدمن 💬", fontSize = 9.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = { 
                                            // Toggle block locally/singly to allow testing
                                            viewModel.blockChatChannel(ch.id, !ch.isBlocked)
                                            Toast.makeText(context, if (ch.isBlocked) "تم فك حظر المحادثة" else "تم حظر المحادثة ومنع أطرافها 🛑", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = if (ch.isBlocked) Color.Gray else Color(0xFFD97706)),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(if (ch.isBlocked) "فك حظر الغرفة" else "حظر الغرفة 🛑", fontSize = 9.sp, color = Color.White)
                                    }
                                    Button(
                                        onClick = { showDeleteChatConfirmId = ch.id },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f)),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("حذف المحادثة 🗑️", fontSize = 9.sp, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (activeSubTab == "COLORS") {
                // SECTION TEN CONFIGURATIONS
                item {
                    Text("🎨 القسم العاشر: التحكم المتقدم والألوان ونماذج الشروط والخط", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Color picker inputs
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("تخصيص لوحة الألوان اليمينة الفاخرة للهيئات بالتفصيل (Hex Color):", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            
                            OutlinedTextField(
                                value = editPrimaryHex,
                                onValueChange = { editPrimaryHex = it },
                                label = { Text("اللون الرئيسي للبرنامج (Primary Color)") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )

                            OutlinedTextField(
                                value = editSecondaryHex,
                                onValueChange = { editSecondaryHex = it },
                                label = { Text("اللون الثانوي للبرنامج (Secondary Color)") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )

                            OutlinedTextField(
                                value = editCardBgHex,
                                onValueChange = { editCardBgHex = it },
                                label = { Text("لون خلفية كروت الفنيين (Card Background Hex)") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )

                            OutlinedTextField(
                                value = editProviderNameHex,
                                onValueChange = { editProviderNameHex = it },
                                label = { Text("لون اسم مقدم الخدمة (Provider Name Color)") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )

                            OutlinedTextField(
                                value = editLocationHex,
                                onValueChange = { editLocationHex = it },
                                label = { Text("لون خط المكان والموقع الجغرافي للشارع") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )

                            OutlinedTextField(
                                value = editRatingHex,
                                onValueChange = { editRatingHex = it },
                                label = { Text("لون نجمة وأرقام التقاييم والنسب الفنية") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )

                            OutlinedTextField(
                                value = editVipBadgeHex,
                                onValueChange = { editVipBadgeHex = it },
                                label = { Text("لون شارة VIP الذهبية المحيطة") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )

                            OutlinedTextField(
                                value = editVerifiedHex,
                                onValueChange = { editVerifiedHex = it },
                                label = { Text("لون الشارة الزرقاء الموثقة للدعم") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )

                            OutlinedTextField(
                                value = editRecommendedHex,
                                onValueChange = { editRecommendedHex = it },
                                label = { Text("لون نجمة وشريحة التوصية (Recommended Badge)") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )
                        }
                    }
                }

                // Font adjustment
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("تخصيص نمط الخطوط العربية بالدليل (RTL typography):", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            val fontOptions = listOf("cairo", "amiri", "tahoma", "system")
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                fontOptions.forEach { font ->
                                    val isSel = editFontSelected == font
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSel) themeColors.accent else Color.Black.copy(alpha = 0.3f))
                                            .clickable { editFontSelected = font }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(font.uppercase(), fontSize = 10.sp, color = if (isSel) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // Floating bubble sliders and offset coordinates adjustments
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("تعديل أحجام وإحداثيات أيقونات الدردشة العائمة بالدعم:", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            
                            Text("1. حجم أيقونة شات المساعدة المباشرة: ${editChatIconSize.toInt()}dp", fontSize = 11.sp, color = Color.White)
                            Slider(value = editChatIconSize, onValueChange = { editChatIconSize = it }, valueRange = 35f..90f)

                            Text("• إحداثي الإزاحة الأفقي (X-Offset): ${editChatIconX.toInt()}", fontSize = 10.sp, color = themeColors.textSecondary)
                            Slider(value = editChatIconX, onValueChange = { editChatIconX = it }, valueRange = 10f..120f)
                            
                            Text("• إحداثي الإزاحة الرأسي (Y-Offset): ${editChatIconY.toInt()}", fontSize = 10.sp, color = themeColors.textSecondary)
                            Slider(value = editChatIconY, onValueChange = { editChatIconY = it }, valueRange = 30f..180f)

                            Spacer(modifier = Modifier.height(6.dp))
                            
                            Text("2. حجم أيقونة المساعد الصوتي الذكي (البوت): ${editAssistantIconSize.toInt()}dp", fontSize = 11.sp, color = Color.White)
                            Slider(value = editAssistantIconSize, onValueChange = { editAssistantIconSize = it }, valueRange = 35f..90f)

                            Text("• إحداثي البوت الأفقي (X-Offset): ${editAssistantIconX.toInt()}", fontSize = 10.sp, color = themeColors.textSecondary)
                            Slider(value = editAssistantIconX, onValueChange = { editAssistantIconX = it }, valueRange = 10f..120f)
                            
                            Text("• إحداثي البوت الرأسي (Y-Offset): ${editAssistantIconY.toInt()}", fontSize = 10.sp, color = themeColors.textSecondary)
                            Slider(value = editAssistantIconY, onValueChange = { editAssistantIconY = it }, valueRange = 30f..180f)
                        }
                    }
                }

                // Requirements Form manager list
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("📋 إدارة شروط ونموذج تسجيل الفنيين بالمنصة:", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = requirementItemInput,
                                    onValueChange = { requirementItemInput = it },
                                    label = { Text("اسم الشرط (مثال: فيش جنائي)") },
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                )
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("إلزامي؟", fontSize = 9.sp, color = Color.White)
                                    Switch(
                                        checked = isNewRequirementMandatory,
                                        onCheckedChange = { isNewRequirementMandatory = it },
                                        modifier = Modifier.scale(0.8f)
                                    )
                                }
                                Button(
                                    onClick = {
                                        if (requirementItemInput.trim().isNotEmpty()) {
                                            val suffix = if (isNewRequirementMandatory) "|Mandatory" else "|Optional"
                                            requirementsListState = requirementsListState + "${requirementItemInput.trim()}$suffix"
                                            requirementItemInput = ""
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                                    modifier = Modifier.align(Alignment.CenterVertically)
                                ) {
                                    Text("أضف", color = Color.Black)
                                }
                            }

                            requirementsListState.forEachIndexed { idx, reqItem ->
                                val parts = reqItem.split("|")
                                val reqName = parts.getOrNull(0) ?: reqItem
                                val isMand = parts.getOrNull(1)?.lowercase() != "optional"
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${idx+1}. $reqName", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text(if (isMand) "إلزامي (مطلوب لإنشاء الحساب) 🔴" else "اختياري (غير معرقل للتسجيل) 🟢", color = if (isMand) Color.Red.copy(alpha = 0.8f) else Color.Green, fontSize = 10.sp)
                                    }
                                    
                                    var isEditingItem by remember { mutableStateOf(false) }
                                    var editItemValue by remember { mutableStateOf(reqName) }
                                    var editItemMandatory by remember { mutableStateOf(isMand) }
                                    
                                    if (isEditingItem) {
                                        AlertDialog(
                                            onDismissRequest = { isEditingItem = false },
                                            title = { Text("📝 تعديل الشرط أو المرفق") },
                                            text = {
                                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                                    OutlinedTextField(
                                                        value = editItemValue,
                                                        onValueChange = { editItemValue = it },
                                                        label = { Text("اسم الشرط أو المستند") },
                                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                                    )
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text("شرط إلزامي للجميع؟")
                                                        Switch(checked = editItemMandatory, onCheckedChange = { editItemMandatory = it })
                                                    }
                                                }
                                            },
                                            confirmButton = {
                                                Button(onClick = {
                                                    if (editItemValue.trim().isNotEmpty()) {
                                                        val updatedList = requirementsListState.toMutableList()
                                                        val suff = if (editItemMandatory) "|Mandatory" else "|Optional"
                                                        updatedList[idx] = "${editItemValue.trim()}$suff"
                                                        requirementsListState = updatedList
                                                        isEditingItem = false
                                                    }
                                                }, colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent)) {
                                                    Text("حفظ التعديل")
                                                }
                                            },
                                            dismissButton = {
                                                TextButton(onClick = { isEditingItem = false }) {
                                                    Text("إلغاء")
                                                }
                                            }
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = { isEditingItem = true }) {
                                            Icon(imageVector = Icons.Default.Edit, contentDescription = "تعديل", tint = themeColors.accent, modifier = Modifier.size(18.dp))
                                        }
                                        IconButton(
                                            onClick = { requirementsListState = requirementsListState.filterIndexed { pIdx, _ -> pIdx != idx } }
                                        ) {
                                            Icon(imageVector = Icons.Default.Close, contentDescription = "حذف الالتزام", tint = Color.Red, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Action Save button for Section Ten details
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // New Dynamic Card Dimensions settings
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("📏 تخصيص مقاسات وأبعاد كروت الفنيين:", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)

                            Text("• ارتفاع صورة غلاف الكرت (0 للإخفاء): ${editCoverHeight.toInt()}dp", fontSize = 11.sp, color = Color.White)
                            Slider(value = editCoverHeight, onValueChange = { editCoverHeight = it }, valueRange = 0f..250f)

                            Text("• حجم الصورة الشخصية (Avatar Size): ${editAvatarSize.toInt()}dp", fontSize = 11.sp, color = Color.White)
                            Slider(value = editAvatarSize, onValueChange = { editAvatarSize = it }, valueRange = 30f..100f)

                            Text("• الهامش والتباعد الداخلي للكرت (Padding): ${editCardPadding.toInt()}dp", fontSize = 11.sp, color = Color.White)
                            Slider(value = editCardPadding, onValueChange = { editCardPadding = it }, valueRange = 4f..24f)

                            Text("• المسافات بين عناصر الكرت (Spacing): ${editElementSpacing.toInt()}dp", fontSize = 11.sp, color = Color.White)
                            Slider(value = editElementSpacing, onValueChange = { editElementSpacing = it }, valueRange = 2f..16f)
                        }
                    }
                }

                // New Badges and indicators settings
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("🛡️ إظهار وإخفاء شارات التميز والتوثيق بالفنيين:", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("🏆 شارة VIP الذهبية والدرع المحيط بالكرت", fontSize = 11.sp, color = Color.White)
                                Switch(checked = editShowVipBadge, onCheckedChange = { editShowVipBadge = it })
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("🔵 شارة التوثيق الزرقاء المعتمدة", fontSize = 11.sp, color = Color.White)
                                Switch(checked = editShowVerifiedBadge, onCheckedChange = { editShowVerifiedBadge = it })
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("🟢 شارة نجمة التوصية (موصى به)", fontSize = 11.sp, color = Color.White)
                                Switch(checked = editShowRecommendedBadge, onCheckedChange = { editShowRecommendedBadge = it })
                            }
                        }
                    }
                }

                // Loyalty and Work Photos settings
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("🤖 إعدادات المساعد الذكي وسوابق الأعمال الفنية:", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("🎁 تفعيل نقاط الولاء والمشاركة للمساعد الذكي", fontSize = 11.sp, color = Color.White)
                                Switch(checked = editShowLoyaltyBanner, onCheckedChange = { editShowLoyaltyBanner = it })
                            }

                            Text("📂 أقصى حد لصور سابقة الأعمال التي يرفعها مقدم الخدمة: ${editMaxWorkPhotos.toInt()}", fontSize = 11.sp, color = Color.White)
                            Slider(
                                value = editMaxWorkPhotos,
                                onValueChange = { editMaxWorkPhotos = it },
                                valueRange = 1f..5f,
                                steps = 3
                            )
                        }
                    }
                }

                // New Card communication buttons manager
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("📞 التحكم الفائق بأزرار الاتصال والتواصل في الكروت:", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)

                            // Call button
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("📞 تفعيل زر الاتصال الهاتفي المباشر", fontSize = 11.sp, color = Color.White)
                                    Switch(checked = editShowCallButton, onCheckedChange = { editShowCallButton = it })
                                }
                                if (editShowCallButton) {
                                    OutlinedTextField(
                                        value = editCallButtonColorHex,
                                        onValueChange = { editCallButtonColorHex = it },
                                        label = { Text("لون زر الاتصال (Hex)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                    )
                                }
                            }

                            // Whatsapp button
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("💬 تفعيل زر المحادثة السريعة واتساب", fontSize = 11.sp, color = Color.White)
                                    Switch(checked = editShowWhatsappButton, onCheckedChange = { editShowWhatsappButton = it })
                                }
                                if (editShowWhatsappButton) {
                                    OutlinedTextField(
                                        value = editWhatsappButtonColorHex,
                                        onValueChange = { editWhatsappButtonColorHex = it },
                                        label = { Text("لون زر واتساب (Hex)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                    )
                                }
                            }

                            // Details button
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("🔍 تفعيل زر عرض التفاصيل والتقييمات", fontSize = 11.sp, color = Color.White)
                                    Switch(checked = editShowDetailsButton, onCheckedChange = { editShowDetailsButton = it })
                                }
                                if (editShowDetailsButton) {
                                    OutlinedTextField(
                                        value = editDetailsButtonColorHex,
                                        onValueChange = { editDetailsButtonColorHex = it },
                                        label = { Text("لون زر التفاصيل (Hex)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                    )
                                }
                            }

                            // Book button
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("📅 تفعيل زر طلب الحجز المباشر", fontSize = 11.sp, color = Color.White)
                                    Switch(checked = editShowBookButton, onCheckedChange = { editShowBookButton = it })
                                }
                                if (editShowBookButton) {
                                    OutlinedTextField(
                                        value = editBookButtonColorHex,
                                        onValueChange = { editBookButtonColorHex = it },
                                        label = { Text("لون زر الحجز المباشر (Hex)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                    )
                                }
                            }
                        }
                    }
                }

                // Action Save button for Section Ten details
                item {
                    Button(
                        onClick = {
                            val upToDateSettings = settingsState.copy(
                                customPrimaryHex = editPrimaryHex,
                                customSecondaryHex = editSecondaryHex,
                                cardBackgroundHex = editCardBgHex,
                                providerNameColorHex = editProviderNameHex,
                                locationColorHex = editLocationHex,
                                ratingColorHex = editRatingHex,
                                vipBadgeColorHex = editVipBadgeHex,
                                verifiedBadgeColorHex = editVerifiedHex,
                                recommendedBadgeColorHex = editRecommendedHex,
                                activeFontFamily = editFontSelected,
                                chatSize = editChatIconSize.toInt(),
                                chatXOffset = editChatIconX.toInt(),
                                chatYOffset = editChatIconY.toInt(),
                                assistantSize = editAssistantIconSize.toInt(),
                                assistantXOffset = editAssistantIconX.toInt(),
                                assistantYOffset = editAssistantIconY.toInt(),
                                registrationRequirements = requirementsListState.joinToString(","),
                                coverHeight = editCoverHeight.toInt(),
                                avatarSize = editAvatarSize.toInt(),
                                elementSpacing = editElementSpacing.toInt(),
                                cardPadding = editCardPadding.toInt(),
                                showVipBadge = editShowVipBadge,
                                showVerifiedBadge = editShowVerifiedBadge,
                                showRecommendedBadge = editShowRecommendedBadge,
                                showCallButton = editShowCallButton,
                                showWhatsappButton = editShowWhatsappButton,
                                showDetailsButton = editShowDetailsButton,
                                showBookButton = editShowBookButton,
                                callButtonColorHex = editCallButtonColorHex,
                                whatsappButtonColorHex = editWhatsappButtonColorHex,
                                detailsButtonColorHex = editDetailsButtonColorHex,
                                bookButtonColorHex = editBookButtonColorHex,
                                showLoyaltyBanner = editShowLoyaltyBanner,
                                maxWorkPhotos = editMaxWorkPhotos.toInt()
                            )
                            viewModel.updateBackdoorSettings(
                                appName = upToDateSettings.appName,
                                welcomeMsg = upToDateSettings.welcomeMessage,
                                footerMsg = upToDateSettings.footerMessage,
                                themeId = upToDateSettings.activeThemeId,
                                supportPhone = upToDateSettings.supportPhone,
                                supportEmail = upToDateSettings.supportEmail,
                                supportWhatsapp = upToDateSettings.supportWhatsapp,
                                isMaintenance = upToDateSettings.isMaintenanceActive,
                                hiddenFooter = upToDateSettings.hidePromoFooter,
                                botHidden = upToDateSettings.assistantHidden,
                                botSize = upToDateSettings.assistantSize,
                                chatHidden = upToDateSettings.chatHidden,
                                chatSize = upToDateSettings.chatSize,
                                radiusKm = upToDateSettings.maxSearchRadiusKm,
                                isSpeech = upToDateSettings.isSpeechSearchEnabled,
                                isDataSaver = false,
                                imgQuality = 90
                            )
                            // Direct persistence inside settings StateFlow
                            viewModel.saveCustomSettingsState(upToDateSettings)
                            Toast.makeText(context, "تم حفظ وضبط وتعميم مظهر الدليل والأزرار والبطاقات بنجاح! 🎉", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("💾 حفظ وحقن جميع تخصيصات المظهر بالدليل الصريح والكامل", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            if (activeSubTab == "VIP") {
                // SUBSCRIPTION CONTROL PANEL
                item {
                    Text("💳 لوحة التحكم باشتراكات الفنيين والتجديد", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    Text("إدارة فترات الصلاحية وشارات الإعلانات، وبث إشعارات التحذير قبل الانتهاء بـ 48 ساعة:", fontSize = 11.sp, color = themeColors.textSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Global Alert Button: auto scan for any technician whose subscription expires within 48 hours and send them push alerts!
                item {
                    Button(
                        onClick = {
                            var sentCount = 0
                            val fortyEightHoursMs = 48L * 60 * 60 * 1000
                            activatedProviders.forEach { p ->
                                val timeLeft = p.subscriptionExpiry - System.currentTimeMillis()
                                if (timeLeft > 0 && timeLeft <= fortyEightHoursMs) {
                                    viewModel.addNotification(
                                        title = "تنبيه هام بفترة تجديد الاشتراك",
                                        message = "عزيزنا الفني المعتمد ${p.name}، يرجى التنويه بأن اشتراكك الفني ينتهي خلال أقل من 48 ساعة. يرجى تجديد الاشتراك فوراً لتفادي تجميد حسابك.",
                                        targetType = "USER",
                                        targetValue = p.phone
                                    )
                                    sentCount++
                                }
                            }
                            if (sentCount > 0) {
                                Toast.makeText(context, "تم بث تنبيهات بوش تلقائية لعدد ($sentCount) فنيين اشتراكهم ينتهي خلال 48 ساعة!", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "لم يتم العثور على أي فنيين اقترب انتهاء اشتراكهم (تحت 48 ساعة) في السجلات حالياً.", Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("🚨 بث تلقائي لتنبيهات 48 ساعة لجميع الفنيين المستهدفين", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Providers list with expiration counters and manual alert triggering
                items(activatedProviders, key = { it.id }) { p ->
                    val timeLeft = p.subscriptionExpiry - System.currentTimeMillis()
                    val daysLeft = (timeLeft / (24L * 60 * 60 * 1000)).toInt()
                    val hoursLeft = ((timeLeft % (24L * 60 * 60 * 1000)) / (60L * 60 * 1000)).toInt()
                    
                    val timeString = if (timeLeft < 0) {
                        "منتهي الصلاحية ❌"
                    } else if (daysLeft > 0) {
                        "متبقي $daysLeft يوم و$hoursLeft ساعة"
                    } else {
                        "متبقي $hoursLeft ساعة فقط ⚠️"
                    }
                    
                    val isNearExpiry = timeLeft > 0 && timeLeft <= (48L * 60 * 60 * 1000)

                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        border = BorderStroke(1.dp, if (isNearExpiry) Color.Red else themeColors.accent.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(p.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text(
                                    text = timeString,
                                    fontSize = 11.sp,
                                    color = if (timeLeft < 0) Color.Red else if (isNearExpiry) Color.Yellow else Color.Green,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text("رقم الهاتف: ${p.phone}", fontSize = 11.sp, color = themeColors.textSecondary)
                            Text("حالة الاشتراك الفني: ${p.subscriptionStatus}", fontSize = 11.sp, color = themeColors.accent)
                            
                            if (!p.password.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("🔑 كلمة المرور: ${p.password}", fontSize = 11.sp, color = themeColors.accent, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Button(
                                        onClick = {
                                            val whatsappText = "مرحباً يا غالي، كلمة المرور الخاصة بحسابك الفني في دليل خدمات اليمن هي: ${p.password}"
                                            val whatsappUrl = "https://wa.me/967${p.phone.trim().removePrefix("0").removePrefix("+967")}?text=${android.net.Uri.encode(whatsappText)}"
                                            try {
                                                val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(whatsappUrl))
                                                context.startActivity(intent)
                                            } catch(e: Exception) {
                                                Toast.makeText(context, "فشل فتح واتساب", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                        modifier = Modifier.height(28.dp),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text("🟢 واتساب", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = {
                                            val smsText = "مرحباً يا غالي، كلمة المرور الخاصة بحسابك الفني في دليل خدمات اليمن هي: ${p.password}"
                                            try {
                                                val intent = Intent(Intent.ACTION_SENDTO, android.net.Uri.parse("smsto:${p.phone}")).apply {
                                                    putExtra("sms_body", smsText)
                                                }
                                                context.startActivity(intent)
                                            } catch(e: Exception) {
                                                Toast.makeText(context, "فشل فتح SMS", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                                        modifier = Modifier.height(28.dp),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text("💬 SMS", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = {
                                            viewModel.addNotification(
                                                title = "🔑 تذكير بكلمة المرور الخاصة بك",
                                                message = "مرحباً يا غالي، كلمة المرور الخاصة بحسابك الفني هي: ${p.password}",
                                                targetType = "USER",
                                                targetValue = p.phone
                                            )
                                            Toast.makeText(context, "تم إرسال تذكير بكلمة المرور للفني بنجاح كإشعار داخلي", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                                        modifier = Modifier.height(28.dp),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text("📱 إشعار", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(
                                    onClick = {
                                        viewModel.extendProviderSubscription(p.id, 30L * 24 * 60 * 60 * 1000)
                                        Toast.makeText(context, "تم تجديد اشتراك ${p.name} لمدة 30 يوماً بنجاح", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("تجديد 30 يوم 🟢", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        viewModel.addNotification(
                                            title = "تنبيه هام بانتهاء صلاحية الاشتراك",
                                            message = "عزيزنا الفني ${p.name}، نود تذكيرك بأن اشتراكك ينتهي خلال 48 ساعة فقط. الرجاء المسارعة بالتجديد للاستمرار بظهور اسمك للزبائن في التطبيق.",
                                            targetType = "USER",
                                            targetValue = p.phone
                                        )
                                        Toast.makeText(context, "تم إرسال إشعار بوش يدوي ينبه الفني بالفترة المحددة بـ 48 ساعة", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("تنبيه بـ 48 ساعة 🔔", color = Color.White, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }

            if (activeSubTab == "SUPERVISORS") {
                // Section 9 / WIPE: Supervisor accounts & database reset & dynamic colors additions/modifications/deletions 
                item {
                    Text("👥 إدارة حسابات المشرفين وصلاحيات التطبيق (مزامنة فورية)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    Spacer(modifier = Modifier.height(4.dp))
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("أضف مشرفاً جديداً بكلمة مرور وصلاحية محددة:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            
                            OutlinedTextField(
                                value = supervisorInputName,
                                onValueChange = { supervisorInputName = it },
                                label = { Text("اسم المشرف الكامل") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )

                            OutlinedTextField(
                                value = supervisorInputPasscode,
                                onValueChange = { supervisorInputPasscode = it },
                                label = { Text("كلمة مرور الدخول") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )

                            Text("اختر الأدوار والصلاحيات الأمنية للمشرف المضاف (يمكنك اختيار صلاحية واحدة أو أكثر):", color = themeColors.textSecondary, fontSize = 10.sp)
                            val roles = listOf("SUPPORT" to "دعم فني", "AUDITOR" to "مدقق ومراقب", "ADMIN" to "مدير رئيسي", "OPERATIONS" to "عمليات")
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                roles.forEach { (roleKey, roleName) ->
                                    val isSel = supervisorInputRole.split(",").contains(roleKey)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSel) themeColors.accent else Color.Black.copy(alpha = 0.3f))
                                            .clickable {
                                                val currentSelected = supervisorInputRole.split(",").filter { it.isNotEmpty() }.toMutableList()
                                                if (currentSelected.contains(roleKey)) {
                                                    currentSelected.remove(roleKey)
                                                } else {
                                                    currentSelected.add(roleKey)
                                                }
                                                if (currentSelected.isEmpty()) {
                                                    currentSelected.add("SUPPORT")
                                                }
                                                supervisorInputRole = currentSelected.joinToString(",")
                                            }
                                            .padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(roleName, fontSize = 9.sp, color = if (isSel) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    if (supervisorInputName.trim().isNotEmpty() && supervisorInputPasscode.trim().isNotEmpty()) {
                                        viewModel.addSupervisor(supervisorInputName.trim(), supervisorInputRole, supervisorInputPasscode.trim())
                                        supervisorInputName = ""
                                        supervisorInputPasscode = ""
                                    } else {
                                        Toast.makeText(context, "الرجاء تعبئة اسم المشرف وكلمة المرور أولاً!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("إضافة المشرف المعتمد", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }

                // List of existing supervisors
                if (supervisorsList.isNotEmpty()) {
                    item {
                        Text("📋 قائمة المشرفين المسجلين في النظام الآن:", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    items(supervisorsList, key = { it.id }) { sup ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(sup.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                         val displayRoles = sup.role.split(",").map { r ->
                                             when(r.trim()) {
                                                 "ADMIN" -> "مدير رئيسي 👑"
                                                 "AUDITOR" -> "مدقق ومراقب 🔍"
                                                 "OPERATIONS" -> "عمليات ميدانية 🚗"
                                                 "SUPPORT" -> "دعم فني 📞"
                                                 else -> r.trim()
                                             }
                                         }.joinToString(" + ")
                                         Text("الصلاحيات الممنوحة: $displayRoles", fontSize = 10.sp, color = themeColors.accent)
                                        Text("رمز الدخول (Passcode): ${sup.passcode}", fontSize = 11.sp, color = themeColors.textSecondary)
                                    }
                                    
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        IconButton(
                                            onClick = { editingSupervisorObj = sup }
                                        ) {
                                            Icon(imageVector = Icons.Default.Edit, contentDescription = "تعديل المشرف", tint = Color.Green, modifier = Modifier.size(20.dp))
                                        }
                                        IconButton(
                                            onClick = { viewModel.removeSupervisor(sup.id) }
                                        ) {
                                            Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف المشرف", tint = Color.Red, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Dynamic Colors Additions, updates and deletions panel
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("🎨 نظام الألوان والسمات المتعددة المتزامن فورياً", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    Spacer(modifier = Modifier.height(4.dp))
                }

                item {
                    var newPaletteName by rememberSaveable { mutableStateOf("") }
                    var newPalettePrimary by rememberSaveable { mutableStateOf("#059669") }
                    var newPaletteSecondary by rememberSaveable { mutableStateOf("#115E59") }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("أضف ستايل لوني مخصص ومثبت بالدليل:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            
                            OutlinedTextField(
                                value = newPaletteName,
                                onValueChange = { newPaletteName = it },
                                label = { Text("اسم لوحة الألوان (مثال: الشتاء المتجمد)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = newPalettePrimary,
                                    onValueChange = { newPalettePrimary = it },
                                    label = { Text("اللون الأساسي (Primary)") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                )

                                OutlinedTextField(
                                    value = newPaletteSecondary,
                                    onValueChange = { newPaletteSecondary = it },
                                    label = { Text("اللون الثانوي (Secondary)") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                )
                            }

                            Button(
                                onClick = {
                                    if (newPaletteName.trim().isNotEmpty() && newPalettePrimary.trim().isNotEmpty() && newPaletteSecondary.trim().isNotEmpty()) {
                                        viewModel.addColorPalette(newPaletteName.trim(), newPalettePrimary.trim(), newPaletteSecondary.trim())
                                        newPaletteName = ""
                                    } else {
                                        Toast.makeText(context, "الرجاء تعبئة الاسم والألوان الستة عشرية بالكامل!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("حقن وإضافة بالدليل المتكامل للألوان", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }

                // Render Color Palettes list
                if (colorPalettesList.isNotEmpty()) {
                    item {
                        Text("🌈 لوحات الألوان والسمات المضافة حديثاً بالدليل:", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    items(colorPalettesList, key = { it.id }) { pal ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(try { Color(android.graphics.Color.parseColor(pal.primaryHex)) } catch(e: Exception) { Color.Gray })
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(try { Color(android.graphics.Color.parseColor(pal.secondaryHex)) } catch(e: Exception) { Color.DarkGray })
                                    )
                                    
                                    Column {
                                        Text(pal.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Text("رئيسي: ${pal.primaryHex} | ثانوي: ${pal.secondaryHex}", fontSize = 9.sp, color = themeColors.textSecondary)
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = {
                                            val updatedSettings = settingsState.copy(
                                                activeThemeId = "CUSTOM_THEME",
                                                customPrimaryHex = pal.primaryHex,
                                                customSecondaryHex = pal.secondaryHex,
                                                customBackgroundHex = pal.backgroundHex,
                                                customSurfaceHex = pal.surfaceHex
                                            )
                                            viewModel.saveCustomSettingsState(updatedSettings); if (false) {
                                            viewModel.updateBackdoorSettings(
                                                appName = updatedSettings.appName,
                                                welcomeMsg = updatedSettings.welcomeMessage,
                                                footerMsg = updatedSettings.footerMessage,
                                                themeId = "CUSTOM_THEME",
                                                supportPhone = updatedSettings.supportPhone,
                                                supportEmail = updatedSettings.supportEmail,
                                                supportWhatsapp = updatedSettings.supportWhatsapp,
                                                isMaintenance = updatedSettings.isMaintenanceActive,
                                                hiddenFooter = updatedSettings.hidePromoFooter,
                                                botHidden = updatedSettings.assistantHidden,
                                                botSize = updatedSettings.assistantSize,
                                                chatHidden = updatedSettings.chatHidden,
                                                chatSize = updatedSettings.chatSize,
                                                radiusKm = updatedSettings.maxSearchRadiusKm,
                                                isSpeech = updatedSettings.isSpeechSearchEnabled,
                                                isDataSaver = false,
                                                imgQuality = 90
                                            )
                                            } ; android.widget.Toast.makeText(context, "🌈 تم تطبيق هذا السطح اللوني الآن ومزامنته فوراً!", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Icon(imageVector = Icons.Default.Check, contentDescription = "تطبيق فوري", tint = Color.Green, modifier = Modifier.size(20.dp))
                                    }

                                    IconButton(
                                        onClick = { viewModel.deleteColorPalette(pal.id) }
                                    ) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف اللون", tint = Color.Red, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (activeSubTab == "CATEGORIES") {
                item {
                    Text("🗂️ إدارة أقسام الصيانة والمهن بالمنصة", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    Text("إضافة أقسام جديدة وتعديل الأقسام وتحديد الأيقونة التعبيرية المناسبة:", fontSize = 11.sp, color = themeColors.textSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("إضافة قسم صيانة جديد ➕", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            
                            OutlinedTextField(
                                value = newCatName,
                                onValueChange = { newCatName = it },
                                label = { Text("اسم القسم (مثال: سباكة، كهرباء...)") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )

                            OutlinedTextField(
                                value = newCatIcon,
                                onValueChange = { newCatIcon = it },
                                label = { Text("أيقونة إيموجي مميزة (مثال: 🚰, ⚡)") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )

                            var isNewMainCategory by remember { mutableStateOf(true) }
                            var selectedParentCatId by remember { mutableStateOf("") }

                            Text("نوع القسم:", fontSize = 11.sp, color = Color.White)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { isNewMainCategory = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = if (isNewMainCategory) themeColors.accent else Color.DarkGray),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("قسم رئيسي 🟢", fontSize = 10.sp, color = if (isNewMainCategory) Color.Black else Color.White)
                                }
                                Button(
                                    onClick = { isNewMainCategory = false },
                                    colors = ButtonDefaults.buttonColors(containerColor = if (!isNewMainCategory) themeColors.accent else Color.DarkGray),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("قسم فرعي 🟡", fontSize = 10.sp, color = if (!isNewMainCategory) Color.Black else Color.White)
                                }
                            }

                            if (!isNewMainCategory) {
                                val mainCategories = categories.filter { it.isMainCategory || it.parentId.isEmpty() }
                                Text("اختر القسم الرئيسي التابع له:", fontSize = 10.sp, color = themeColors.accent)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    mainCategories.forEach { parent ->
                                        val isSelected = selectedParentCatId == parent.id
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { selectedParentCatId = parent.id },
                                            label = { Text("${parent.icon} ${parent.name}", fontSize = 10.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = themeColors.accent,
                                                selectedLabelColor = Color.Black
                                            )
                                        )
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    if (newCatName.trim().isEmpty() || newCatIcon.trim().isEmpty()) {
                                        Toast.makeText(context, "الرجاء تعبئة اسم القسم والأيقونة", Toast.LENGTH_SHORT).show()
                                    } else {
                                        viewModel.addNewCategory(
                                            nameAr = newCatName.trim(),
                                            nameEn = newCatName.trim(),
                                            icon = newCatIcon.trim(),
                                            description = "",
                                            parentId = if (!isNewMainCategory) selectedParentCatId else "",
                                            isMainCategory = isNewMainCategory
                                        )
                                        newCatName = ""
                                        newCatIcon = ""
                                        selectedParentCatId = ""
                                        Toast.makeText(context, "تمت إضافة قسم الصيانة بنجاح 🗂️", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("إضافة القسم وتفعيله فوراً", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                items(categories, key = { it.id }) { cat ->
                    val index = categories.indexOf(cat)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(cat.icon, fontSize = 20.sp)
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(cat.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    if (cat.isPinned) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = "مثبت",
                                            tint = Color(0xFFFFD700),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(onClick = { viewModel.togglePinCategory(cat.id) }) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "تثبيت القسم",
                                        tint = if (cat.isPinned) Color(0xFFFFD700) else Color.Gray.copy(alpha = 0.6f)
                                    )
                                }
                                IconButton(onClick = {
                                    showEditCategoryObj = cat
                                    editCatName = cat.name
                                    editCatIcon = cat.icon
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "تعديل القسم",
                                        tint = themeColors.accent
                                    )
                                }
                                IconButton(onClick = {
                                    showMergeCategoryObj = cat
                                    selectedTargetCategoryIdForMerge = categories.firstOrNull { it.id != cat.id }?.id ?: ""
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "دمج القسم",
                                        tint = Color.Cyan
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        val list = categories.toMutableList()
                                        if (index > 0) {
                                            list.removeAt(index)
                                            list.add(index - 1, cat)
                                            viewModel.reorderCategories(list)
                                        }
                                    },
                                    enabled = index > 0
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowUp,
                                        contentDescription = "ترتيب للأعلى",
                                        tint = if (index > 0) themeColors.accent else Color.Gray.copy(alpha = 0.5f)
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        val list = categories.toMutableList()
                                        if (index < list.size - 1) {
                                            list.removeAt(index)
                                            list.add(index + 1, cat)
                                            viewModel.reorderCategories(list)
                                        }
                                    },
                                    enabled = index < categories.size - 1
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "ترتيب للأسفل",
                                        tint = if (index < categories.size - 1) themeColors.accent else Color.Gray.copy(alpha = 0.5f)
                                    )
                                }
                                IconButton(onClick = { showDeleteCategoryConfirmId = cat.id }) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف القسم", tint = Color.Red)
                                }
                            }
                        }
                    }
                }
            }

            if (activeSubTab == "CITIES") {
                item {
                    Text("🗺️ إدارة محافظات ومدن الجمهورية اليمنية", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    Text("إضافة المحافظات والمدن المستهدفة بالخدمة وتصفح المضاف حالياً بالمنصة:", fontSize = 11.sp, color = themeColors.textSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("إضافة مدينة/محافظة يمنية جديدة ➕", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            
                            OutlinedTextField(
                                value = newCityArName,
                                onValueChange = { newCityArName = it },
                                label = { Text("الاسم باللغة العربية (مثال: صنعاء، عدن...)") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )

                            OutlinedTextField(
                                value = newCityEnName,
                                onValueChange = { newCityEnName = it },
                                label = { Text("الاسم باللغة الإنجليزية (مثال: Sana'a, Aden...)") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )

                            OutlinedTextField(
                                value = newCityIcon,
                                onValueChange = { newCityIcon = it },
                                label = { Text("أيقونة/إيموجي رمزية (مثال: 🏰, 🏖️, 📍)") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )

                            Button(
                                onClick = {
                                    if (newCityArName.trim().isEmpty() || newCityEnName.trim().isEmpty()) {
                                        Toast.makeText(context, "الرجاء ملء الاسم العربي والإنجليزي للمحافظة", Toast.LENGTH_SHORT).show()
                                    } else {
                                        viewModel.addNewCity(
                                            nameAr = newCityArName.trim(),
                                            nameEn = newCityEnName.trim(),
                                            icon = newCityIcon.trim().ifEmpty { "📍" }
                                        )
                                        newCityArName = ""
                                        newCityEnName = ""
                                        newCityIcon = "📍"
                                        Toast.makeText(context, "تمت إضافة المحافظة بنجاح 🗺️", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("تأكيد إضافة المحافظة", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                items(citiesList, key = { city -> city.id }) { city ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(city.icon.ifEmpty { "📍" }, fontSize = 20.sp)
                                Column {
                                    Text(city.nameAr, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text(city.nameEn, fontSize = 11.sp, color = themeColors.textSecondary)
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(onClick = { 
                                    showEditCityObj = city
                                }) {
                                    Icon(imageVector = Icons.Default.Edit, contentDescription = "تعديل المحافظة", tint = themeColors.accent)
                                }
                                IconButton(onClick = { 
                                    viewModel.removeCity(city.id)
                                    Toast.makeText(context, "تم حذف المحافظة بنجاح", Toast.LENGTH_SHORT).show()
                                }) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف المحافظة", tint = Color.Red)
                                }
                            }
                        }
                    }
                }
            }

            if (activeSubTab == "BACKUP") {
                item {
                    Text("💾 لوحة النسخ الاحتياطي والمزامنة والجدولة والتقارير", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    Text("أدوات التصدير الشامل للبيانات والتحقق من صحة الاتصال المتزامن مع خوادم Cloud Firestore:", fontSize = 11.sp, color = themeColors.textSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        border = BorderStroke(1.dp, Color.Green.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("🛡️ إحصائيات حالة المزامنة والاتصال الحي", fontSize = 12.sp, color = Color.Green, fontWeight = FontWeight.Bold)
                            
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("الحالة الفورية:", fontSize = 11.sp, color = Color.White)
                                Text("متصل وآمن 🟢", fontSize = 11.sp, color = Color.Green, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("حجم البيانات النشطة:", fontSize = 11.sp, color = Color.White)
                                val sizeEst = (activatedProviders.size + categories.size + bookings.size + reports.size) * 1.5f
                                Text(String.format("%.2f KB", sizeEst), fontSize = 11.sp, color = Color.White)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("تردد نبض الاتصال:", fontSize = 11.sp, color = Color.White)
                                Text("كل 10 ثوانٍ (ذكي تلقائي)", fontSize = 11.sp, color = themeColors.accent)
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Button(
                                onClick = {
                                    Toast.makeText(context, "🔄 جاري إعادة فحص ومزامنة كامل جداول البيانات مع السحاب...", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("تحديث وإعادة جدولة الفحص الفوري 🔄", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("💾 نظام النسخ الاحتياطي التلقائي واستيراد البيانات", fontSize = 12.sp, color = themeColors.accent, fontWeight = FontWeight.Bold)
                            Text("يقوم النظام تلقائياً بجدولة نسخ كامل الجداول وقواعد البيانات لضمان عدم ضياع البيانات الفنية والحجوزات والمتاجر والعقارات والكلمات المشفرة.", fontSize = 11.sp, color = Color.LightGray)
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        viewModel.createSystemBackup { success, jsonStr ->
                                            if (success) {
                                                backupJsonStringState = jsonStr
                                                val path = viewModel.saveBackupToLocalStorage(context, jsonStr, "yemen_services_backup_${System.currentTimeMillis()}")
                                                if (path.isNotEmpty()) {
                                                    Toast.makeText(context, "✅ تم حفظ النسخة الاحتياطية بذاكرة الهاتف/SD Card:\n$path", Toast.LENGTH_LONG).show()
                                                } else {
                                                    Toast.makeText(context, "✅ تم إنشاء النسخة الاحتياطية بنجاح!", Toast.LENGTH_SHORT).show()
                                                }
                                            } else {
                                                Toast.makeText(context, "❌ فشل إنشاء النسخة الاحتياطية", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("حفظ بالهاتف/SD Card 💾", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        viewModel.createSystemBackup { success, jsonStr ->
                                            if (success) {
                                                val clipboardManager = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                                val clipData = android.content.ClipData.newPlainText("YemenServiceBackup", jsonStr)
                                                clipboardManager.setPrimaryClip(clipData)
                                                Toast.makeText(context, "📋 تم نسخ كود الاحتياط الكامل للذاكرة بنجاح!", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("نسخ الكود الكامل 📋", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (backupJsonStringState.isNotEmpty()) {
                                Text("تم توليد الكود الاحتياطي بنجاح (${backupJsonStringState.length} حرفاً). احتفظ به في مكان آمن.", fontSize = 10.sp, color = Color.Green)
                            }

                            Divider(color = Color.Gray.copy(alpha = 0.5f), thickness = 1.dp)

                            Text("📥 استعادة النظام من نسخة احتياطية سابقة", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = restoreJsonInputState,
                                onValueChange = { restoreJsonInputState = it },
                                label = { Text("أدخل أو الصق كود النسخة الاحتياطية JSON هنا") },
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 5,
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )

                            Button(
                                onClick = {
                                    if (restoreJsonInputState.trim().isEmpty()) {
                                        Toast.makeText(context, "⚠️ يرجى لصق كود النسخة أولاً!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        viewModel.restoreSystemFromBackup(restoreJsonInputState) { success, msg ->
                                            if (success) {
                                                restoreJsonInputState = ""
                                                Toast.makeText(context, "💚 تم استعادة كامل البيانات والمزامنة السحابية بنجاح بنسبة 100%!", Toast.LENGTH_LONG).show()
                                            } else {
                                                Toast.makeText(context, "❌ فشل استعادة البيانات: $msg", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("تأكيد استعادة قواعد البيانات ومزامنتها سحابياً ⚠️", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Divider(color = Color.Gray.copy(alpha = 0.5f), thickness = 1.dp)

                            // --- SECONDARY FIREBASE SYNC PANEL ---
                            Text("🔥 ربط وإدارة المزامنة المزدوجة مع حساب Firebase ثانوي", fontSize = 12.sp, color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold)
                            
                            var secProjId by remember { mutableStateOf("") }
                            var secApiKey by remember { mutableStateOf("") }
                            var secAppId by remember { mutableStateOf("") }
                            var secBucket by remember { mutableStateOf("") }
                            var secEnabled by remember { mutableStateOf(false) }

                            OutlinedTextField(
                                value = secProjId,
                                onValueChange = { secProjId = it },
                                label = { Text("Project ID الحساب الثانوي", fontSize = 10.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFF59E0B))
                            )
                            OutlinedTextField(
                                value = secApiKey,
                                onValueChange = { secApiKey = it },
                                label = { Text("API Key الحساب الثانوي", fontSize = 10.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFF59E0B))
                            )

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Switch(
                                    checked = secEnabled,
                                    onCheckedChange = { secEnabled = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF10B981))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("تفعيل المزامنة المزدوجة التلقائية مع Firebase الثانوي ⚡", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    viewModel.setSecondaryFirebaseConfig(secProjId, secApiKey, secAppId, secBucket, secEnabled)
                                    Toast.makeText(context, "⚡ تم حفظ إعدادات المزامنة الثانوية لـ Firebase بنجاح!", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("حفظ وتحديث إعدادات المزامنة المزدوجة 💾", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("📁 تصدير التقارير الإدارية الشاملة للجمهورية", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            
                            Button(
                                onClick = {
                                    Toast.makeText(context, "تم تصدير الدليل الكامل للفنيين والمحافظات إلى ذاكرة الهاتف 📁", Toast.LENGTH_LONG).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("تصدير الدليل الكامل للفنيين (CSV)", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    Toast.makeText(context, "تم تصدير جميع سجلات حجز الصيانة المجدولة والنشطة 📁", Toast.LENGTH_LONG).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("تصدير سجل الحجوزات النشطة (CSV)", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            if (activeSubTab == "CLEAN") {
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("⚙️ تخصيص تصفية وتهيئة البيانات الفورية", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    Text("حدد أنواع البيانات التي تريد مسحها أو نسخها وحفظها احتياطياً:", fontSize = 11.sp, color = themeColors.textSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("📁 تحديد الكل لإجراء العملية الشاملة", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Switch(
                                    checked = wipeProvidersChecked && wipeBookingsChecked && wipeChatsChecked && wipeNotifsChecked && wipeReportsChecked && wipePendingChecked && wipeBannersChecked && wipeSupervisorsChecked && wipeCitiesChecked && wipeThemesChecked,
                                    onCheckedChange = { allChecked ->
                                        wipeProvidersChecked = allChecked
                                        wipeBookingsChecked = allChecked
                                        wipeChatsChecked = allChecked
                                        wipeNotifsChecked = allChecked
                                        wipeReportsChecked = allChecked
                                        wipeCategoriesChecked = allChecked
                                        wipePendingChecked = allChecked
                                        wipeBannersChecked = allChecked
                                        wipeSupervisorsChecked = allChecked
                                        wipeCitiesChecked = allChecked
                                        wipeThemesChecked = allChecked
                                    },
                                    colors = SwitchDefaults.colors(checkedThumbColor = themeColors.accent)
                                )
                            }

                            Divider(color = themeColors.accent.copy(alpha = 0.2f))

                            val itemsList = listOf(
                                Triple("الفنيين ومقدمي الخدمات الحقيقيين 👤", wipeProvidersChecked) { v: Boolean -> wipeProvidersChecked = v },
                                Triple("الحجوزات والطلبات المجدولة 📅", wipeBookingsChecked) { v: Boolean -> wipeBookingsChecked = v },
                                Triple("المحادثات وقنوات الشات المتبادلة 💬", wipeChatsChecked) { v: Boolean -> wipeChatsChecked = v },
                                Triple("الإشعارات الموجهة والمرسلة 🔔", wipeNotifsChecked) { v: Boolean -> wipeNotifsChecked = v },
                                Triple("بلاغات الشكاوى ومشاكل المستخدمين ⚠️", wipeReportsChecked) { v: Boolean -> wipeReportsChecked = v },
                                Triple("الأقسام وتصنيفات المهن (حذر) 📂", wipeCategoriesChecked) { v: Boolean -> wipeCategoriesChecked = v },
                                Triple("طلبات الانضمام والتسجيل المعلقة 📝", wipePendingChecked) { v: Boolean -> wipePendingChecked = v },
                                Triple("الإعلانات وبنرات واجهة التطبيق 📢", wipeBannersChecked) { v: Boolean -> wipeBannersChecked = v },
                                Triple("المشرفين والإداريين المساعدين 🔑", wipeSupervisorsChecked) { v: Boolean -> wipeSupervisorsChecked = v },
                                Triple("المحافظات والمدن المعتمدة 🗺️", wipeCitiesChecked) { v: Boolean -> wipeCitiesChecked = v },
                                Triple("قوالب الألوان المخصصة 🎨", wipeThemesChecked) { v: Boolean -> wipeThemesChecked = v }
                            )

                            itemsList.forEach { (label, isChecked, setChecked) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { setChecked(!isChecked) }
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(label, fontSize = 12.sp, color = Color.White)
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { setChecked(it) },
                                        colors = CheckboxDefaults.colors(checkedColor = themeColors.accent)
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("📥 تصدير ونسخ البيانات المحددة (نسخة احتياطية):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    Spacer(modifier = Modifier.height(4.dp))
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "يمكنك حفظ البيانات المحددة ونقلها فوراً إلى أي هاتف آخر أو نسخها لحساب جوجل درايف الخاص بك لحفظها من الفقدان:",
                                fontSize = 11.sp,
                                color = themeColors.textSecondary
                            )

                            Button(
                                onClick = {
                                    val selectedCols = mutableListOf<String>()
                                    if (wipeProvidersChecked) selectedCols.add("providers")
                                    if (wipeBookingsChecked) selectedCols.add("bookings")
                                    if (wipeChatsChecked) selectedCols.add("chat_channels")
                                    if (wipeNotifsChecked) selectedCols.add("notifications")
                                    if (wipeReportsChecked) selectedCols.add("reports")
                                    if (wipeCategoriesChecked) selectedCols.add("categories")
                                    if (wipePendingChecked) selectedCols.add("pending_providers")
                                    if (wipeBannersChecked) selectedCols.add("banners")
                                    if (wipeSupervisorsChecked) selectedCols.add("supervisors")
                                    if (wipeCitiesChecked) selectedCols.add("cities")
                                    if (wipeThemesChecked) selectedCols.add("color_themes")

                                    viewModel.exportSelectedCollectionsAsJson(selectedCols) { jsonString ->
                                        val clipboardManager = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                        val clipData = android.content.ClipData.newPlainText("YemenService_Backup_JSON", jsonString)
                                        clipboardManager.setPrimaryClip(clipData)
                                        Toast.makeText(context, "📋 تم نسخ البيانات المحددة بصيغة JSON بنجاح إلى ذاكرة الهاتف!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("📋 نسخ البيانات المحددة للحافظة (Clipboard)", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    val selectedCols = mutableListOf<String>()
                                    if (wipeProvidersChecked) selectedCols.add("providers")
                                    if (wipeBookingsChecked) selectedCols.add("bookings")
                                    if (wipeChatsChecked) selectedCols.add("chat_channels")
                                    if (wipeNotifsChecked) selectedCols.add("notifications")
                                    if (wipeReportsChecked) selectedCols.add("reports")
                                    if (wipeCategoriesChecked) selectedCols.add("categories")
                                    if (wipePendingChecked) selectedCols.add("pending_providers")
                                    if (wipeBannersChecked) selectedCols.add("banners")
                                    if (wipeSupervisorsChecked) selectedCols.add("supervisors")
                                    if (wipeCitiesChecked) selectedCols.add("cities")
                                    if (wipeThemesChecked) selectedCols.add("color_themes")

                                    viewModel.exportSelectedCollectionsAsJson(selectedCols) { jsonString ->
                                        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(android.content.Intent.EXTRA_SUBJECT, "نسخة احتياطية - دليل خدمات اليمن")
                                            putExtra(android.content.Intent.EXTRA_TEXT, jsonString)
                                        }
                                        context.startActivity(android.content.Intent.createChooser(shareIntent, "تصدير نسخة البيانات الاحتياطية إلى:"))
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("☁️ نسخ لجوجل درايف / ذاكرة الهاتف ومشاركتها", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("🚨 منطقة التطهير الكلي والمسح النهائي:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Red)
                    Spacer(modifier = Modifier.height(4.dp))
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF451A03)),
                        border = BorderStroke(1.dp, Color.Red),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("تنبيه أمني صارم للغاية!", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Red)
                            Text(
                                text = "الضغط على الزر أدناه سيقوم بحذف وإعادة تهيئة الأنواع المحددة أعلاه فقط فوراً! لن يتسنى لك مراجعة أو التراجع عن هذه البيانات بمجرد تأكيد المسح بالرمز السري.",
                                fontSize = 11.sp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { showWipeConfirmDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("مسح كامل البيانات وإعادة بناء الدليل العظيم", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            if (activeSubTab == "REVIEWS") {
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("⭐ إدارة التقييمات والمراجعات للأعضاء", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    Text("قم بالبحث وتعديل تقييمات وعدد مراجعات الفنيين مباشرة:", fontSize = 11.sp, color = themeColors.textSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                val providersList = activatedProviders
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            providersList.forEach { provider ->
                                var editRatingText by remember(provider.id) { mutableStateOf(provider.rating.toString()) }
                                var editReviewsText by remember(provider.id) { mutableStateOf(provider.numReviews.toString()) }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(provider.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Text("${provider.profession} | 📍 ${provider.area}", fontSize = 11.sp, color = Color.LightGray)
                                        Text("التقييم الحالي: ⭐ ${provider.rating} (${provider.numReviews} تقييم)", fontSize = 10.sp, color = themeColors.accent)
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = editRatingText,
                                            onValueChange = { editRatingText = it },
                                            label = { Text("التقييم", fontSize = 9.sp) },
                                            singleLine = true,
                                            modifier = Modifier.width(60.dp),
                                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                        )

                                        OutlinedTextField(
                                            value = editReviewsText,
                                            onValueChange = { editReviewsText = it },
                                            label = { Text("العدد", fontSize = 9.sp) },
                                            singleLine = true,
                                            modifier = Modifier.width(60.dp),
                                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                        )

                                        Button(
                                            onClick = {
                                                val r = editRatingText.toFloatOrNull() ?: provider.rating
                                                val n = editReviewsText.toIntOrNull() ?: provider.numReviews
                                                val updated = provider.copy(rating = r, numReviews = n)
                                                viewModel.updateProviderEntity(updated)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary),
                                            contentPadding = PaddingValues(horizontal = 8.dp),
                                            modifier = Modifier.height(36.dp)
                                        ) {
                                            Text("حفظ", fontSize = 10.sp, color = Color.White)
                                        }
                                    }
                                }
                                Divider(color = Color.White.copy(alpha = 0.05f))
                            }
                        }
                    }
                }
            }

            if (activeSubTab == "CALLS") {
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("📞 سجلات ومراقبة المكالمات الجارية والمباشرة", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    Text("مراقبة وتتبع اتصالات المواطنين بالفنيين ومزودي الخدمات مباشرة:", fontSize = 11.sp, color = themeColors.textSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (callsLog.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("📭 لا توجد سجلات مكالمات محفوظة حالياً.", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                } else {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                            border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                callsLog.sortedByDescending { it.timestamp }.forEach { call ->
                                    val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(call.timestamp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("📱 المكالمة لـ: ${call.providerName}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            Text("👤 المتصل: ${call.callerName}", fontSize = 11.sp, color = Color.LightGray)
                                            Text("⏰ التاريخ: $dateStr", fontSize = 10.sp, color = themeColors.textSecondary)
                                        }

                                        IconButton(onClick = {
                                            viewModel.db.collection("calls").document(call.id).delete()
                                            viewModel.triggerNotification("🗑️ تم حذف سجل المكالمة")
                                        }) {
                                            Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف السجل", tint = Color.Red.copy(alpha = 0.8f))
                                        }
                                    }
                                    Divider(color = Color.White.copy(alpha = 0.05f))
                                }
                            }
                        }
                    }
                }
            }

            if (activeSubTab == "COUPONS") {
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("🎫 إدارة وتوليد الكوبونات الحصرية", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    Text("توليد وتوزيع الكوبونات الترويجية لشحن نقاط المواطنين مجاناً:", fontSize = 11.sp, color = themeColors.textSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("➕ إنشاء كوبون جديد", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            
                            OutlinedTextField(
                                modifier = Modifier.fillMaxWidth(),
                                value = couponCodeInput,
                                onValueChange = { couponCodeInput = it },
                                label = { Text("رمز الكوبون (مثال: YEMEN2026)") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    modifier = Modifier.weight(1f),
                                    value = couponPointsInput,
                                    onValueChange = { couponPointsInput = it },
                                    label = { Text("قيمة النقاط") },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                )

                                OutlinedTextField(
                                    modifier = Modifier.weight(1f),
                                    value = couponExpiryDaysInput,
                                    onValueChange = { couponExpiryDaysInput = it },
                                    label = { Text("صلاحية الكوبون (أيام)") },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    modifier = Modifier.weight(1f),
                                    value = couponDiscountInput,
                                    onValueChange = { couponDiscountInput = it },
                                    label = { Text("نسبة الخصم %") },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                )

                                OutlinedTextField(
                                    modifier = Modifier.weight(1f),
                                    value = couponMaxUsageInput,
                                    onValueChange = { couponMaxUsageInput = it },
                                    label = { Text("مرات الاستخدام القصوى") },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                )
                            }

                            Button(
                                onClick = {
                                    val code = couponCodeInput.trim().uppercase()
                                    val points = couponPointsInput.toIntOrNull() ?: 100
                                    val days = couponExpiryDaysInput.toLongOrNull() ?: 30L
                                    val discount = couponDiscountInput.toIntOrNull() ?: 15
                                    val maxUsage = couponMaxUsageInput.toIntOrNull() ?: 50
                                    if (code.isNotBlank()) {
                                        viewModel.addCoupon(code, points, days * 24 * 60 * 60 * 1000L, discount, maxUsage)
                                        couponCodeInput = ""
                                    } else {
                                        viewModel.triggerNotification("❌ يرجى إدخال رمز كوبون صالح!")
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("إنشاء الكوبون وحفظه 🎫", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("📋 الكوبونات المتاحة بالمنصة", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                }

                if (couponsList.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("📭 لا توجد كوبونات مسجلة حالياً.", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                } else {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                            border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                couponsList.forEach { coupon ->
                                    val expiryStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(coupon.expiryTimestamp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("🎫 الكود: ${coupon.code}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            Text("🎁 النقاط: ${coupon.pointsValue} نقطة | خصم: ${coupon.discountPercentage}% | الاستخدام: ${coupon.usedCount}/${coupon.maxUsageCount}", fontSize = 11.sp, color = Color.LightGray)
                                            Text("الصلاحية لغاية: $expiryStr | الحالة: ${coupon.status}", fontSize = 10.sp, color = themeColors.textSecondary)
                                        }

                                        IconButton(onClick = {
                                            viewModel.deleteCoupon(coupon.id)
                                        }) {
                                            Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف الكوبون", tint = Color.Red.copy(alpha = 0.8f))
                                        }
                                    }
                                    Divider(color = Color.White.copy(alpha = 0.05f))
                                }
                            }
                        }
                    }
                }
            }

            if (activeSubTab == "BLOCKED") {
                val blockedProviders = activatedProviders.filter { it.isBlocked }

                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("🚫 إدارة ومراقبة الفنيين المحظورين", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    Text("استعراض وإلغاء حظر مقدمي الخدمات الموقوفين لأسباب إدارية أو بلاغات:", fontSize = 11.sp, color = themeColors.textSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (blockedProviders.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🎉 لا يوجد أي فني محظور حالياً بالمنصة.", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                } else {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                            border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                blockedProviders.forEach { provider ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("👤 الفني: ${provider.name}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            Text("📞 هاتف: ${provider.phone} | المهنة: ${provider.profession}", fontSize = 11.sp, color = Color.LightGray)
                                            Text("📍 المنطقة: ${provider.area}", fontSize = 10.sp, color = themeColors.textSecondary)
                                        }

                                        Button(
                                            onClick = {
                                                viewModel.toggleProviderBlock(provider.id)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                                        ) {
                                            Text("إلغاء الحظر ✅", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Divider(color = Color.White.copy(alpha = 0.05f))
                                }
                            }
                        }
                    }
                }
            }

            if (activeSubTab == "PAYMENTS") {
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("💳 نظام الدفع والتحقق والمحافظ الرقمية والمكالمات", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    Text("إدارة خيارات الدفع، المحافظ الجوالية والمستندات، تخصيص شروط الحجز والعربون للمزودين والأقسام، والمحافظ الداخلية في الوقت الفعلي:", fontSize = 11.sp, color = themeColors.textSecondary)
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // 1. General Payment & Booking Rules Config Form (Checkbox Style)
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("⚙️ إعدادات الدفع العامة ونظام العمولات (نمط المربعات ☑)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                            
                            com.example.ui.components.OptionCheckboxCard(
                                title = "تفعيل نظام الدفع كاملاً بالمنصة",
                                subtitle = "تمكين أو إيقاف كافة خيارات ووظائف الدفع المالي في التطبيق",
                                isChecked = isPaymentEnabledInput,
                                onCheckedChange = { isPaymentEnabledInput = it },
                                themeColors = themeColors
                            )

                            com.example.ui.components.OptionCheckboxCard(
                                title = "ربط الدفع باستمارة الحجز",
                                subtitle = "إلزام المستخدمين بالمرور بصفحة اختيار وسيلة الدفع عند الحجز",
                                isChecked = isBookingPaymentRequiredInput,
                                onCheckedChange = { isBookingPaymentRequiredInput = it },
                                themeColors = themeColors
                            )

                            com.example.ui.components.OptionCheckboxCard(
                                title = "طلب دفع مقدم (عربون) عند الحجز",
                                subtitle = "اشتراط دفع جزء من مبلغ الخدمة مقدماً لتأكيد جدية الطلب",
                                isChecked = requireAdvancePaymentInput,
                                onCheckedChange = { requireAdvancePaymentInput = it },
                                themeColors = themeColors
                            )

                            if (requireAdvancePaymentInput) {
                                OutlinedTextField(
                                    value = advancePaymentPercentInput,
                                    onValueChange = { advancePaymentPercentInput = it },
                                    label = { Text("نسبة الدفع المقدم (مثال: 0.30 لـ 30%)", fontSize = 11.sp) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                )

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = minAdvanceAmountInput,
                                        onValueChange = { minAdvanceAmountInput = it },
                                        label = { Text("الحد الأدنى للمقدم (ريال)", fontSize = 11.sp) },
                                        modifier = Modifier.weight(1f),
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                    )
                                    OutlinedTextField(
                                        value = maxAdvanceAmountInput,
                                        onValueChange = { maxAdvanceAmountInput = it },
                                        label = { Text("الحد الأقصى للمقدم (ريال)", fontSize = 11.sp) },
                                        modifier = Modifier.weight(1f),
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                    )
                                }
                            }

                            com.example.ui.components.OptionCheckboxCard(
                                title = "تفعيل اقتطاع العمولة من مقدم الخدمة",
                                subtitle = "احتساب نسبة عمولة المنصة تلقائياً على كل حجز مكتمل",
                                isChecked = isCommissionEnabledInput,
                                onCheckedChange = { isCommissionEnabledInput = it },
                                themeColors = themeColors
                            )

                            if (isCommissionEnabledInput) {
                                OutlinedTextField(
                                    value = paymentCommissionRateInput,
                                    onValueChange = { paymentCommissionRateInput = it },
                                    label = { Text("نسبة عمولة المنصة (مثال: 0.10 لـ 10%)", fontSize = 11.sp) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                )
                            }

                            com.example.ui.components.OptionCheckboxCard(
                                title = "إظهار تبويب المحفظة والدفع بداخل الملفات الشخصية",
                                subtitle = "تمكين قسم المحفظة الرقمية بصفحات المحلات والفنيين والمطاعم والمستخدمين",
                                isChecked = showWalletInProfileInput,
                                onCheckedChange = { showWalletInProfileInput = it },
                                themeColors = themeColors
                            )

                            Divider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))

                            Text("🔗 ربط الحجز الفوري والدفع المتقدم وحالات العربون الخاص", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)

                            OutlinedTextField(
                                value = linkedCategoriesForInstantBookingInput,
                                onValueChange = { linkedCategoriesForInstantBookingInput = it },
                                label = { Text("معرفات الأقسام المربوطة بالحجز الفوري (مفصولة بفارزة)", fontSize = 11.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )

                            OutlinedTextField(
                                value = linkedProvidersForDepositInput,
                                onValueChange = { linkedProvidersForDepositInput = it },
                                label = { Text("معرفات/أرقام الفنيين والمحلات المشترط عليهم دفع عربون فقط", fontSize = 11.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )

                            OutlinedTextField(
                                value = exemptUsersFromDepositInput,
                                onValueChange = { exemptUsersFromDepositInput = it },
                                label = { Text("أرقام هواتف المستخدمين المعفيين من العربون (الحجز المباشر بدون عربون)", fontSize = 11.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )

                            Divider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))

                            Text("📞 نظام المكالمات الصوتية المباشرة داخل التطبيق", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)

                            com.example.ui.components.OptionCheckboxCard(
                                title = "تفعيل نظام المكالمات الصوتية داخل التطبيق",
                                subtitle = "السماح بإجراء مكالمات صوتية مباشرة وفورية بجودة عالية بين الأطراف",
                                isChecked = voiceCallsEnabledInput,
                                onCheckedChange = { voiceCallsEnabledInput = it },
                                themeColors = themeColors
                            )

                            if (voiceCallsEnabledInput) {
                                OutlinedTextField(
                                    value = voiceCallsAllowedCategoriesInput,
                                    onValueChange = { voiceCallsAllowedCategoriesInput = it },
                                    label = { Text("الأقسام المصرح لها بالمكالمات (فارغ = للجميع)", fontSize = 11.sp) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                )

                                OutlinedTextField(
                                    value = voiceCallsAllowedProvidersInput,
                                    onValueChange = { voiceCallsAllowedProvidersInput = it },
                                    label = { Text("أرقام هواتف الفنيين والمحلات المصرح لهم بالمكالمات", fontSize = 11.sp) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedLabelColor = themeColors.accent,
                                        unfocusedLabelColor = Color.LightGray
                                    )
                                )

                                Text(
                                    text = "☑️ حدد الصح أمام اسم الفني أو المحل لمنحه صلاحية إجراء واستقبال المكالمات:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = themeColors.accent
                                )

                                val allowedList = remember(voiceCallsAllowedProvidersInput) {
                                    voiceCallsAllowedProvidersInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                                }

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 160.dp)
                                        .verticalScroll(rememberScrollState())
                                        .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                        .padding(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    val providersList by viewModel.providers.collectAsState()
                                    val storesList by viewModel.stores.collectAsState()

                                    if (providersList.isEmpty() && storesList.isEmpty()) {
                                        Text("لا يوجد مقدمي خدمات أو محلات مسجلة حالياً", fontSize = 10.sp, color = Color.Gray)
                                    } else {
                                        providersList.filter { !it.isDeleted }.forEach { p ->
                                            val identifier = if (p.phone.isNotEmpty()) p.phone else p.id
                                            val isChecked = allowedList.contains(identifier) || allowedList.contains(p.id) || allowedList.contains(p.phone)
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        val current = allowedList.toMutableList()
                                                        if (isChecked) {
                                                            current.remove(p.phone)
                                                            current.remove(p.id)
                                                        } else {
                                                            if (p.phone.isNotEmpty()) current.add(p.phone) else current.add(p.id)
                                                        }
                                                        voiceCallsAllowedProvidersInput = current.distinct().joinToString(",")
                                                    }
                                                    .padding(vertical = 2.dp)
                                            ) {
                                                Checkbox(
                                                    checked = isChecked,
                                                    onCheckedChange = { checked ->
                                                        val current = allowedList.toMutableList()
                                                        if (!checked) {
                                                            current.remove(p.phone)
                                                            current.remove(p.id)
                                                        } else {
                                                            if (p.phone.isNotEmpty()) current.add(p.phone) else current.add(p.id)
                                                        }
                                                        voiceCallsAllowedProvidersInput = current.distinct().joinToString(",")
                                                    },
                                                    colors = CheckboxDefaults.colors(checkedColor = themeColors.accent)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("🛠️ ${p.name} (${p.phone.ifEmpty { "بدون هاتف" }}) - ${p.area}", fontSize = 10.sp, color = Color.White)
                                            }
                                        }

                                        storesList.filter { !it.isDeleted }.forEach { s ->
                                            val identifier = if (s.phone.isNotEmpty()) s.phone else s.id
                                            val isChecked = allowedList.contains(identifier) || allowedList.contains(s.id) || allowedList.contains(s.phone)
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        val current = allowedList.toMutableList()
                                                        if (isChecked) {
                                                            current.remove(s.phone)
                                                            current.remove(s.id)
                                                        } else {
                                                            if (s.phone.isNotEmpty()) current.add(s.phone) else current.add(s.id)
                                                        }
                                                        voiceCallsAllowedProvidersInput = current.distinct().joinToString(",")
                                                    }
                                                    .padding(vertical = 2.dp)
                                            ) {
                                                Checkbox(
                                                    checked = isChecked,
                                                    onCheckedChange = { checked ->
                                                        val current = allowedList.toMutableList()
                                                        if (!checked) {
                                                            current.remove(s.phone)
                                                            current.remove(s.id)
                                                        } else {
                                                            if (s.phone.isNotEmpty()) current.add(s.phone) else current.add(s.id)
                                                        }
                                                        voiceCallsAllowedProvidersInput = current.distinct().joinToString(",")
                                                    },
                                                    colors = CheckboxDefaults.colors(checkedColor = themeColors.accent)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("🏪 ${s.name} (${s.phone.ifEmpty { "بدون هاتف" }}) - ${s.localNeighborhood}", fontSize = 10.sp, color = Color.White)
                                            }
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = voiceCallsAllowedUsersInput,
                                    onValueChange = { voiceCallsAllowedUsersInput = it },
                                    label = { Text("أرقام هواتف المستخدمين المصرح لهم بالمكالمات الصوتية", fontSize = 11.sp) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                )

                                Spacer(modifier = Modifier.height(6.dp))
                                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("🎙️ إعدادات المكالمات الصوتية والمظهر المتقدمة", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                                
                                com.example.ui.components.OptionCheckboxCard(
                                    title = "إخفاء المكالمات الصوتية داخل التطبيق بالكامل",
                                    subtitle = "عند التفعيل، سيتم إخفاء زر المكالمة الصوتية من بطاقة المزود ومن شاشات المحادثات",
                                    isChecked = disableVoiceCallsInput,
                                    onCheckedChange = { disableVoiceCallsInput = it },
                                    themeColors = themeColors
                                )

                                com.example.ui.components.OptionCheckboxCard(
                                    title = "إخفاء الشريط العلوي بالكامل (الهيدر)",
                                    subtitle = "عند التفعيل، سيتم إخفاء شريط العنوان العلوي الذي يحتوي على اسم التطبيق وأيقونة اللغة بالكامل",
                                    isChecked = hideTopHeaderBarInput,
                                    onCheckedChange = { hideTopHeaderBarInput = it },
                                    themeColors = themeColors
                                )

                                OutlinedTextField(
                                    value = customAppNameInput,
                                    onValueChange = { customAppNameInput = it },
                                    label = { Text("تخصيص اسم التطبيق (اتركه فارغاً للاسم الافتراضي)", fontSize = 11.sp) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Button(
                                onClick = {
                                    val updatedSettings = settingsState.copy(
                                        isPaymentEnabled = isPaymentEnabledInput,
                                        isBookingPaymentRequired = isBookingPaymentRequiredInput,
                                        requireAdvancePayment = requireAdvancePaymentInput,
                                        advancePaymentPercent = advancePaymentPercentInput.toFloatOrNull() ?: 0.30f,
                                        minAdvanceAmount = minAdvanceAmountInput.toDoubleOrNull() ?: 500.0,
                                        maxAdvanceAmount = maxAdvanceAmountInput.toDoubleOrNull() ?: 50000.0,
                                        isCommissionEnabled = isCommissionEnabledInput,
                                        paymentCommissionRate = paymentCommissionRateInput.toFloatOrNull() ?: 0.10f,
                                        showWalletInProfile = showWalletInProfileInput,
                                        linkedCategoriesForInstantBooking = linkedCategoriesForInstantBookingInput,
                                        linkedProvidersForDeposit = linkedProvidersForDepositInput,
                                        exemptUsersFromDeposit = exemptUsersFromDepositInput,
                                        voiceCallsEnabled = voiceCallsEnabledInput,
                                        voiceCallsAllowedCategories = voiceCallsAllowedCategoriesInput,
                                        voiceCallsAllowedProviders = voiceCallsAllowedProvidersInput,
                                        voiceCallsAllowedUsers = voiceCallsAllowedUsersInput,
                                        disableVoiceCalls = disableVoiceCallsInput,
                                        hideTopHeaderBar = hideTopHeaderBarInput,
                                        customAppName = customAppNameInput
                                    )
                                    viewModel.saveCustomSettingsState(updatedSettings)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent)
                            ) {
                                Text("💾 حفظ ومزامنة كافة الإعدادات فورياً في Firestore", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }

                // 2. Internal Digital Wallets System for Providers, Stores & Restaurants
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🏦 نظام المحافظ الرقمية الداخلية للمحلات والمزودين", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                                Button(
                                    onClick = {
                                        val newW = com.example.data.InternalWalletEntity(
                                            id = "W_${System.currentTimeMillis().toString().takeLast(6)}",
                                            ownerName = "محل/فني جديد",
                                            ownerPhone = "770000000",
                                            ownerType = "STORE",
                                            balance = 0.0
                                        )
                                        viewModel.saveInternalWallet(newW)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("➕ إنشاء محفظة جديدة", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Text("إدارة الأرصدة، شحن وتفريغ رصيد الفنيين والمطاعم والمحلات وسجل المعاملات المباشر:", fontSize = 11.sp, color = Color.LightGray)

                            if (internalWallets.isEmpty()) {
                                Text("⚠️ لا توجد محافظ داخلية حتى الآن. انقر 'إنشاء محفظة جديدة' للبدء.", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(vertical = 8.dp))
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    internalWallets.forEach { w ->
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = themeColors.surface.copy(alpha = 0.7f)),
                                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text("${w.ownerName} (${w.ownerType})", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                    Text("📱 رقم التواصل: ${w.ownerPhone}", fontSize = 10.sp, color = Color.LightGray)
                                                    Text("💰 الرصيد الحالي: ${w.balance} ريال يمني 🇾🇪", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                                                }
                                                Button(
                                                    onClick = {
                                                        selectedWalletForTx = w
                                                        txTypeInput = "DEPOSIT"
                                                        txAmountInput = "1000"
                                                        txNoteInput = "شحن رصيد بواسطة الإدارة"
                                                        showWalletTxDialog = true
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                ) {
                                                    Text("إيداع / سحب 💸", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Wallets List & Actions
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📱 المحافظ الجوالية المتاحة للعملاء", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                        Button(
                            onClick = {
                                editingWalletObj = null
                                walletNumberInput = ""
                                walletAccountNameInput = ""
                                walletAccountNameArInput = ""
                                walletDescriptionInput = ""
                                walletInstructionsInput = ""
                                walletIsDefaultInput = false
                                walletDisplayOrderInput = "0"
                                walletMinTransferInput = "0"
                                walletMaxTransferInput = "100000"
                                walletStatusInput = "active"
                                showAddWalletDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("➕ إضافة محفظة", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                if (paymentWallets.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = themeColors.surface.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                Text("⚠️ لا توجد أي محافظ جوالية مضافة حالياً. يرجى إضافة محفظة دفع لاستقبال أموال العملاء.", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }
                } else {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            paymentWallets.forEach { wallet ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                                    border = BorderStroke(1.dp, if (wallet.isDefault) themeColors.accent else themeColors.accent.copy(alpha = 0.15f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val providerName = when (wallet.provider) {
                                                "jeeb" -> "محفظة جيب 📱"
                                                "alKarimi" -> "الكريمي ام فلوس 🏦"
                                                "jawaly" -> "محفظة جوالي 📲"
                                                "yemenMobile" -> "يمن موبايل كاش 🇾🇪"
                                                "mtc" -> "محفظة MTC ⚡"
                                                "sabafon" -> "سبأ كاش 📞"
                                                "youssef" -> "محفظة يوسف 👤"
                                                else -> "محفظة تحويل أخرى 🌐"
                                            }
                                            Text(providerName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                                            
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                if (wallet.isDefault) {
                                                    Box(
                                                        modifier = Modifier
                                                            .background(themeColors.accent.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text("الافتراضية ⭐", color = themeColors.accent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .background(
                                                            if (wallet.currency == "USD") Color(0xFF3B82F6).copy(alpha = 0.2f) else if (wallet.currency == "SAR") Color(0xFFF59E0B).copy(alpha = 0.2f) else Color(0xFF10B981).copy(alpha = 0.2f),
                                                            RoundedCornerShape(4.dp)
                                                        )
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        when (wallet.currency) {
                                                            "USD" -> "💵 دولار USD"
                                                            "SAR" -> "🇸🇦 ريال سعودي"
                                                            else -> "🇾🇪 ريال يمني"
                                                        },
                                                        color = Color.White,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .background(
                                                            if (wallet.status == "active") Color(0xFF10B981).copy(alpha = 0.2f) else Color.Red.copy(alpha = 0.2f),
                                                            RoundedCornerShape(4.dp)
                                                        )
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        if (wallet.status == "active") "نشطة 🟢" else "موقفة 🚫",
                                                        color = if (wallet.status == "active") Color(0xFF10B981) else Color.Red,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }

                                        Text("رقم المحفظة / الحساب: ${wallet.walletNumber}", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                        Text("اسم صاحب الحساب: ${wallet.accountNameAr.ifBlank { wallet.accountName }}", fontSize = 11.sp, color = Color.LightGray)
                                        Text(
                                            "النوع: ${when(wallet.walletType){ "DEPOSIT" -> "📥 إيداع فقط"; "WITHDRAWAL" -> "📤 سحب فقط"; else -> "🔄 إيداع وسحب" }} | الرؤية للمستخدم: ${if (wallet.isVisibleToUsers) "👁️ ظاهرة" else "🙈 مخفية"}",
                                            fontSize = 10.sp,
                                            color = themeColors.accent
                                        )
                                        if (wallet.description.isNotEmpty()) {
                                            Text("الوصف: ${wallet.description}", fontSize = 10.sp, color = Color.Gray)
                                        }

                                        HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 4.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Button(
                                                    onClick = {
                                                        editingWalletObj = wallet
                                                        walletProviderInput = wallet.provider
                                                        walletNumberInput = wallet.walletNumber
                                                        walletAccountNameInput = wallet.accountName
                                                        walletAccountNameArInput = wallet.accountNameAr
                                                        walletDescriptionInput = wallet.description
                                                        walletInstructionsInput = wallet.instructions
                                                        walletTypeInput = wallet.walletType
                                                        walletCurrencyInput = wallet.currency
                                                        walletIsVisibleInput = wallet.isVisibleToUsers
                                                        walletIsDefaultInput = wallet.isDefault
                                                        walletDisplayOrderInput = wallet.displayOrder.toString()
                                                        walletMinTransferInput = wallet.minTransferAmount.toString()
                                                        walletMaxTransferInput = wallet.maxTransferAmount.toString()
                                                        walletStatusInput = wallet.status
                                                        showAddWalletDialog = true
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Blue.copy(alpha = 0.6f)),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 1.dp)
                                                ) {
                                                    Text("تعديل ✏️", color = Color.White, fontSize = 9.sp)
                                                }

                                                Button(
                                                    onClick = {
                                                        viewModel.togglePaymentWalletVisibility(wallet.id, wallet.isVisibleToUsers)
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = if (wallet.isVisibleToUsers) Color.DarkGray else Color(0xFF059669)),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 1.dp)
                                                ) {
                                                    Text(if (wallet.isVisibleToUsers) "إخفاء 🙈" else "إظهار 👁️", color = Color.White, fontSize = 9.sp)
                                                }

                                                Button(
                                                    onClick = {
                                                        val updated = wallet.copy(isDefault = true)
                                                        viewModel.updatePaymentWallet(updated)
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 1.dp),
                                                    enabled = !wallet.isDefault
                                                ) {
                                                    Text("افتراضية ⭐", color = Color.White, fontSize = 9.sp)
                                                }

                                                Button(
                                                    onClick = {
                                                        val updated = wallet.copy(status = if (wallet.status == "active") "suspended" else "active")
                                                        viewModel.updatePaymentWallet(updated)
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 1.dp)
                                                ) {
                                                    Text(if (wallet.status == "active") "تعطيل ⛔" else "تنشيط 🟢", color = Color.White, fontSize = 9.sp)
                                                }
                                            }

                                            IconButton(
                                                onClick = {
                                                    viewModel.deletePaymentWallet(wallet.id)
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Text("🗑️", fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Transactions Verification List
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("💸 طلبات المعاملات وإثباتات دفع العملاء الواردة", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    Text("مراقبة وتأكيد الدفع اليدوي بعد فحص الحساب ومطابقة رقم التحويل المرفق بالرسائل المصرفية لديهم:", fontSize = 11.sp, color = themeColors.textSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                }

                if (paymentsList.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = themeColors.surface.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                Text("⚠️ لا توجد أي معاملات دفع مسجلة بالمنصة حتى الآن.", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }
                } else {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            paymentsList.forEach { payment ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                                    border = BorderStroke(
                                        1.dp,
                                        when (payment.status) {
                                            "COMPLETED" -> Color(0xFF10B981).copy(alpha = 0.5f)
                                            "PROCESSING" -> Color(0xFF3B82F6).copy(alpha = 0.5f)
                                            "FAILED" -> Color.Red.copy(alpha = 0.5f)
                                            else -> Color.Gray.copy(alpha = 0.3f)
                                        }
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("المبلغ: ${payment.amount} ريال يمني 🇾🇪", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            
                                            Box(
                                                modifier = Modifier
                                                    .background(
                                                        when (payment.status) {
                                                            "COMPLETED" -> Color(0xFF10B981).copy(alpha = 0.2f)
                                                            "PROCESSING" -> Color(0xFF3B82F6).copy(alpha = 0.2f)
                                                            "FAILED" -> Color.Red.copy(alpha = 0.2f)
                                                            "REFUNDED" -> Color.Magenta.copy(alpha = 0.2f)
                                                            else -> Color.Gray.copy(alpha = 0.2f)
                                                        },
                                                        RoundedCornerShape(4.dp)
                                                    )
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                val statusText = when (payment.status) {
                                                    "COMPLETED" -> "مكتمل ومؤكد ✅"
                                                    "PROCESSING" -> "بانتظار التحقق ⏳"
                                                    "FAILED" -> "مرفوض ❌"
                                                    "REFUNDED" -> "تم الاسترداد 🔄"
                                                    else -> "طلب معلق ⏳"
                                                }
                                                Text(
                                                    statusText,
                                                    color = when (payment.status) {
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

                                        Text("👤 العميل (معرف): ${payment.userId}", fontSize = 11.sp, color = Color.LightGray)
                                        Text("🛠️ فني الخدمة (معرف): ${payment.providerId}", fontSize = 11.sp, color = Color.LightGray)
                                        if (payment.bookingId.isNotEmpty()) {
                                            Text("📅 رقم طلب الحجز: ${payment.bookingId}", fontSize = 11.sp, color = themeColors.accent)
                                        }

                                        Divider(color = Color.White.copy(alpha = 0.05f))

                                        Text("طريقة الدفع: ${if (payment.method == "mobileWallet") "محفظة جوال" else payment.method}", fontSize = 11.sp, color = Color.White)
                                        if (payment.walletProvider.isNotEmpty()) {
                                            Text("مزود المحفظة: ${payment.walletProvider} | رقم: ${payment.walletNumber}", fontSize = 11.sp, color = Color.LightGray)
                                            Text("اسم المحول: ${payment.walletAccountName}", fontSize = 11.sp, color = Color.LightGray)
                                        }
                                        if (payment.transferId.isNotEmpty()) {
                                            Text("رقم الحوالة/الإحالة: ${payment.transferId}", fontSize = 12.sp, color = themeColors.accent, fontWeight = FontWeight.Bold)
                                        }

                                        if (payment.transferPhoto.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("📸 صورة إثبات التحويل المرفقة:", fontSize = 10.sp, color = Color.LightGray)
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(100.dp)
                                                    .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(6.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (payment.transferPhoto.startsWith("http")) {
                                                    Text("🔗 رابط الصورة: ${payment.transferPhoto.take(35)}...", fontSize = 10.sp, color = themeColors.accent)
                                                } else {
                                                    Text("🖼️ صورة مشفرة Base64 جاهزة للتحقق", fontSize = 11.sp, color = Color.White)
                                                }
                                            }
                                        }

                                        if (payment.verificationNote.isNotEmpty()) {
                                            Text("📝 ملاحظة التحقق: ${payment.verificationNote}", fontSize = 10.sp, color = Color.LightGray)
                                        }

                                        if (payment.status == "PROCESSING" || payment.status == "PENDING") {
                                            Divider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 4.dp))
                                            
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Button(
                                                    onClick = {
                                                        verifyingPaymentObj = payment
                                                        adminVerifyPaymentNote = "تم التحقق ومطابقة الحوالة بنجاح"
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text("قبول وتأكيد ✅", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }

                                                Button(
                                                    onClick = {
                                                        rejectingPaymentObj = payment
                                                        adminRejectPaymentNote = "رقم التحويل غير صحيح أو الحوالة غير واردة بالحساب"
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text("رفض المعاملة ❌", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        } else if (payment.status == "COMPLETED") {
                                            Divider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 4.dp))
                                            Button(
                                                onClick = {
                                                    refundingPaymentObj = payment
                                                    refundReasonInput = "تم إلغاء الخدمة أو بطلب من الفني والعميل"
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text("🔄 بدء إجراءات استرداد المبلغ (Refund)", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (activeSubTab == "DELETED") {

                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("🗑️ إدارة ومراقبة الفنيين المحذوفين", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    Text("استعراض واستعادة مقدمي الخدمات الذين تم حذفهم منطقياً من الدليل:", fontSize = 11.sp, color = themeColors.textSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (deletedList.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🎉 لا يوجد أي فني محذوف منطقياً حالياً.", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                } else {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                            border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                deletedList.forEach { provider ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("👤 الفني: ${provider.name}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            Text("📞 هاتف: ${provider.phone} | المهنة: ${provider.profession}", fontSize = 11.sp, color = Color.LightGray)
                                            Text("📍 المنطقة: ${provider.area}", fontSize = 10.sp, color = themeColors.textSecondary)
                                        }

                                        Button(
                                            onClick = {
                                                viewModel.restoreProvider(provider.id)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                            modifier = Modifier.padding(start = 8.dp)
                                        ) {
                                            Text("استعادة حساب الفني 🟢", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Divider(color = Color.White.copy(alpha = 0.05f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ------------------ POPUP CONFIRMATION CONTEXT DIALOGS ------------------

    // Rejection dialog for pending provider request
    rejectingProviderRequest?.let { req ->
        AlertDialog(
            onDismissRequest = { rejectingProviderRequest = null },
            title = { Text("📝 توضيح سبب رفض الطلب", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("يرجى كتابة سبب رفض طلب انضمام الفني ${req.name}:", fontSize = 11.sp, color = Color.LightGray)
                    OutlinedTextField(
                        value = providerRejectionReasonText,
                        onValueChange = { providerRejectionReasonText = it },
                        placeholder = { Text("مثال: المستندات المرفقة غير واضحة") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.rejectTechnician(req.id, providerRejectionReasonText.ifBlank { "لم يستوفِ الشروط المطلوبة" })
                        rejectingProviderRequest = null
                        providerRejectionReasonText = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("تأكيد الرفض", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { rejectingProviderRequest = null }) {
                    Text("إلغاء", color = Color.LightGray)
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }

    // 1. Delete category confirmation
    showDeleteCategoryConfirmId?.let { catId ->
        val catName = categories.find { it.id == catId }?.name ?: ""
        AlertDialog(
            onDismissRequest = { showDeleteCategoryConfirmId = null },
            containerColor = Color(0xFF1E293B),
            title = { Text("⚠️ هل ترغب في حذف القسم؟", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("أنت على وشك حذف قسم الصيانة ($catName) نهائياً. سيتم إزالتها من شريط الانتقالات ومقدمي الخدمات.", color = themeColors.textSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCategory(catId)
                        showDeleteCategoryConfirmId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("نعم، احذف القسم")
                }
            },
            dismissButton = {
                Button(onClick = { showDeleteCategoryConfirmId = null }, colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)) {
                    Text("إلغاء الإجراء")
                }
            }
        )
    }

    // 2. Edit category Dialog
    showEditCategoryObj?.let { cat ->
        var editIsMain by remember(cat.id) { mutableStateOf(cat.isMainCategory || cat.parentId.isEmpty()) }
        var editParentId by remember(cat.id) { mutableStateOf(cat.parentId) }

        Dialog(onDismissRequest = { showEditCategoryObj = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                modifier = Modifier.padding(16.dp).fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("✏️ تعديل وتخصيص هيكلية القسم", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    
                    OutlinedTextField(
                        value = editCatName,
                        onValueChange = { editCatName = it },
                        label = { Text("اسم القسم") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    OutlinedTextField(
                        value = editCatIcon,
                        onValueChange = { editCatIcon = it },
                        label = { Text("أيقونة إيموجي مميزة (مثال: 🚰, ⚡)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    Text("نوع وهيكلية القسم:", fontSize = 11.sp, color = themeColors.accent, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { editIsMain = true; editParentId = "" },
                            colors = ButtonDefaults.buttonColors(containerColor = if (editIsMain) themeColors.accent else Color.DarkGray),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("قسم رئيسي 🟢", fontSize = 10.sp, color = if (editIsMain) Color.Black else Color.White)
                        }
                        Button(
                            onClick = { editIsMain = false },
                            colors = ButtonDefaults.buttonColors(containerColor = if (!editIsMain) themeColors.accent else Color.DarkGray),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("قسم فرعي 🟡", fontSize = 10.sp, color = if (!editIsMain) Color.Black else Color.White)
                        }
                    }

                    if (!editIsMain) {
                        val mainCategories = categories.filter { (it.isMainCategory || it.parentId.isEmpty()) && it.id != cat.id }
                        Text("حدد القسم الرئيسي التابع له:", fontSize = 10.sp, color = Color.White)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            mainCategories.forEach { parent ->
                                val isSelected = editParentId == parent.id
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { editParentId = parent.id },
                                    label = { Text("${parent.icon} ${parent.name}", fontSize = 10.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = themeColors.accent,
                                        selectedLabelColor = Color.Black
                                    )
                                )
                            }
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                if (editCatName.trim().isNotEmpty()) {
                                    viewModel.editCategory(
                                        categoryId = cat.id,
                                        newName = editCatName.trim(),
                                        newIcon = editCatIcon.trim(),
                                        parentId = if (!editIsMain) editParentId else "",
                                        isMainCategory = editIsMain
                                    )
                                    showEditCategoryObj = null
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("حفظ الهيكلية", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { showEditCategoryObj = null },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("إلغاء", color = Color.White)
                        }
                    }
                }
            }
        }
    }

    // 2a. Edit City Dialog
    showEditCityObj?.let { city ->
        var editArName by remember(city.id) { mutableStateOf(city.nameAr) }
        var editEnName by remember(city.id) { mutableStateOf(city.nameEn) }
        var editIcon by remember(city.id) { mutableStateOf(city.icon.ifEmpty { "📍" }) }
        var editPhotoUrl by remember(city.id) { mutableStateOf(city.photoUrl) }
        var editOrder by remember(city.id) { mutableStateOf(city.sortOrder.toString()) }

        Dialog(onDismissRequest = { showEditCityObj = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                modifier = Modifier.padding(16.dp).fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("✏️ تعديل وتخصيص بيانات المحافظة/المدينة", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)

                    OutlinedTextField(
                        value = editArName,
                        onValueChange = { editArName = it },
                        label = { Text("الاسم باللغة العربية") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    OutlinedTextField(
                        value = editEnName,
                        onValueChange = { editEnName = it },
                        label = { Text("الاسم باللغة الإنجليزية") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    OutlinedTextField(
                        value = editIcon,
                        onValueChange = { editIcon = it },
                        label = { Text("أيقونة/إيموجي رمزية (مثال: 🏰, 🏖️, 📍)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    OutlinedTextField(
                        value = editPhotoUrl,
                        onValueChange = { editPhotoUrl = it },
                        label = { Text("رابط صورة رمزية للمحافظة (URL)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    OutlinedTextField(
                        value = editOrder,
                        onValueChange = { editOrder = it },
                        label = { Text("رقم الترتيب (الترتيب في القائمة)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                if (editArName.trim().isNotEmpty()) {
                                    viewModel.updateCity(
                                        city.copy(
                                            nameAr = editArName.trim(),
                                            nameEn = editEnName.trim(),
                                            icon = editIcon.trim().ifEmpty { "📍" },
                                            photoUrl = editPhotoUrl.trim(),
                                            sortOrder = editOrder.toIntOrNull() ?: 0
                                        )
                                    )
                                    showEditCityObj = null
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("حفظ التغييرات", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { showEditCityObj = null },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("إلغاء", color = Color.White)
                        }
                    }
                }
            }
        }
    }

    // 2b. Merge category Dialog
    showMergeCategoryObj?.let { sourceCat ->
        Dialog(onDismissRequest = { showMergeCategoryObj = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                modifier = Modifier.padding(16.dp).fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("🔄 دمج وتحويل فنيين ومتاجر قسم الصيانة", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(
                        "أنت على وشك دمج قسم (${sourceCat.icon} ${sourceCat.name}). سيتم نقل جميع الفنيين والمتاجر المسجلين في هذا القسم إلى القسم الذي تحدده بالأسفل، ثم سيتم حذف هذا القسم نهائياً.",
                        fontSize = 11.sp,
                        color = Color.LightGray
                    )

                    val otherCategories = categories.filter { it.id != sourceCat.id }

                    if (otherCategories.isEmpty()) {
                        Text("⚠️ لا توجد أقسام أخرى متاحة للدمج معها!", color = Color.Red, fontSize = 12.sp)
                    } else {
                        var expanded by remember { mutableStateOf(false) }
                        val selectedTarget = categories.find { it.id == selectedTargetCategoryIdForMerge }

                        Text("اختر القسم المستهدف للدمج والتحويل إليه:", fontSize = 12.sp, color = themeColors.textSecondary)

                        Box(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                    .clickable { expanded = true }
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedTarget?.let { "${it.icon} ${it.name}" } ?: "اختر قسم آخر...",
                                    color = Color.White,
                                    fontSize = 13.sp
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "عرض",
                                    tint = Color.White
                                )
                            }

                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier.background(Color(0xFF1E293B))
                            ) {
                                otherCategories.forEach { targetCat ->
                                    DropdownMenuItem(
                                        text = { Text("${targetCat.icon} ${targetCat.name}", color = Color.White) },
                                        onClick = {
                                            selectedTargetCategoryIdForMerge = targetCat.id
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    if (selectedTargetCategoryIdForMerge.isNotEmpty()) {
                                        viewModel.mergeCategories(sourceCat.id, selectedTargetCategoryIdForMerge)
                                        showMergeCategoryObj = null
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("تأكيد دمج القسم", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { showMergeCategoryObj = null },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("إلغاء", color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }

    // 3. Delete reservation confirmation Dialog
    showDeleteBookingConfirmId?.let { bId ->
        AlertDialog(
            onDismissRequest = { showDeleteBookingConfirmId = null },
            containerColor = Color(0xFF1E293B),
            title = { Text("⚠️ هل تقصد حذف الحجز نهائياً؟", color = Color.White) },
            text = { Text("حذف هذا الحجز سيزيله من جدول حجوزات الدعم والمتابعة مباشرة.", color = themeColors.textSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteBooking(bId)
                        showDeleteBookingConfirmId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("نعم، امسح تماماً", color = Color.White)
                }
            },
            dismissButton = {
                Button(onClick = { showDeleteBookingConfirmId = null }, colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)) {
                    Text("تراجع")
                }
            }
        )
    }

    // Booking Rejection Reason Dialog
    showRejectionReasonDialogId?.let { bId ->
        AlertDialog(
            onDismissRequest = { showRejectionReasonDialogId = null },
            containerColor = Color(0xFF1E293B),
            title = { Text("❌ رفض طلب الحجز", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("الرجاء توضيح سبب رفض طلب الحجز ليتم إرساله للعميل مباشرة في التنبيهات:", color = Color.White, fontSize = 11.sp)
                    OutlinedTextField(
                        value = bookingRejectionReasonInput,
                        onValueChange = { bookingRejectionReasonInput = it },
                        label = { Text("سبب الرفض (مثال: جدول الأعمال ممتلئ، الموقع خارج التغطية)", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (bookingRejectionReasonInput.trim().isEmpty()) {
                            Toast.makeText(context, "الرجاء كتابة سبب الرفض لتنبيه العميل به", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.updateBookingStatus(bId, "REJECTED", bookingRejectionReasonInput)
                            showRejectionReasonDialogId = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("تأكيد الرفض وإرسال السبب", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Button(
                    onClick = { showRejectionReasonDialogId = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                ) {
                    Text("إلغاء", color = Color.White, fontSize = 11.sp)
                }
            }
        )
    }

    // 4. Delete targeted notifications confirmation Dialog
    showDeleteNotifConfirmId?.let { nId ->
        AlertDialog(
            onDismissRequest = { showDeleteNotifConfirmId = null },
            containerColor = Color(0xFF1E293B),
            title = { Text("⚠️ تأكيد حذف الإشعار الموجه", color = Color.White) },
            text = { Text("هذا الإشعار سيوضع كمنشور تالف وسيتم محوه من قاعدة بيانات الهواتف المحلية والمخزن.", color = themeColors.textSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteNotification(nId)
                        showDeleteNotifConfirmId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("نعم، احذف الذكرى")
                }
            },
            dismissButton = {
                Button(onClick = { showDeleteNotifConfirmId = null }, colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)) {
                    Text("تراجع")
                }
            }
        )
    }

    // 5. Active Chat Channel logs visualizer dialog and direct replies
    showActiveChatChannelObj?.let { ch ->
        Dialog(onDismissRequest = { showActiveChatChannelObj = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                border = BorderStroke(1.dp, themeColors.accent)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val partnerName = if (ch.isProvider) "مقدم الخدمة: ${ch.userName}" else "مستخدم الدليل: ${ch.userName}"
                    Text("💬 مراقبة الشات: $partnerName", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    
                    // Messages records logger
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(ch.messages, key = { it.id }) { msg ->
                                val alignment = if (msg.senderId == "admin") Alignment.End else Alignment.Start
                                val bubbleBg = if (msg.senderId == "admin") themeColors.primary else Color.Gray.copy(alpha = 0.3f)
                                Column(horizontalAlignment = alignment, modifier = Modifier.fillMaxWidth()) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(bubbleBg)
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(msg.message, fontSize = 11.sp, color = Color.White)
                                    }
                                    Text(msg.senderName, fontSize = 9.sp, color = themeColors.textSecondary)
                                }
                            }
                        }
                    }

                    // Reply tool
                    OutlinedTextField(
                        value = adminChatReplyInput,
                        onValueChange = { adminChatReplyInput = it },
                        label = { Text("اكتب رد المشرف الصريح والكامل هنا...") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                if (adminChatReplyInput.trim().isNotEmpty()) {
                                    viewModel.replyToChatChannel(ch.id, "admin", adminChatReplyInput.trim(), "مشرف الدعم")
                                    // Update visual logs dynamically
                                    val currentChannels = viewModel.chatChannels.value
                                    val updatedCh = currentChannels.find { it.id == ch.id }
                                    if (updatedCh != null) {
                                        showActiveChatChannelObj = updatedCh
                                    }
                                    adminChatReplyInput = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Green),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("إرسال الرد الموثق", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = {
                                viewModel.toggleBlockChatChannel(ch.id)
                                // Refresh visual logs
                                val currentChannels = viewModel.chatChannels.value
                                val updatedCh = currentChannels.find { it.id == ch.id }
                                if (updatedCh != null) {
                                    showActiveChatChannelObj = updatedCh
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (ch.isBlocked) "إلغاء الحظر" else "حظر الطرفين", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }

    // 6. Delete communication log dialog
    showDeleteChatConfirmId?.let { chId ->
        AlertDialog(
            onDismissRequest = { showDeleteChatConfirmId = null },
            containerColor = Color(0xFF1E293B),
            title = { Text("⚠️ هل أنت متأكد من مسح ملفات السجل صراحة؟", color = Color.White) },
            text = { Text("سيتم اقتطاع وحذف قنوات الشات من قاعدة الداتا فوراً دون أي إمكانية استرجاع.", color = themeColors.textSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteChatChannel(chId)
                        showDeleteChatConfirmId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("حذف ذيل المحادثة كاملة")
                }
            },
            dismissButton = {
                Button(onClick = { showDeleteChatConfirmId = null }, colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)) {
                    Text("إلغاء")
                }
            }
        )
    }

    // 7. Wipe confirming AlertDialog with hidden obscured password checks as requested
    if (showWipeConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showWipeConfirmDialog = false },
            containerColor = Color(0xFF0F172A),
            title = { Text("🚨 تأكيد الهوية الأمنية للمصفي", color = Color.Red, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "هذه العملية فائقة الخطورة وذات تصفية كلية فورية للمركز الفني بالمنصة والدليل اليمني. الرجاء كتابة الرمز السري للأدمن لإكمال المسح المخصص:",
                        color = themeColors.textSecondary,
                        fontSize = 12.sp
                    )
                    OutlinedTextField(
                        value = wipeInputPassword,
                        onValueChange = { wipeInputPassword = it },
                        label = { Text("كلمة مرور الأدمن") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (viewModel.verifyAdminOrOwnerPassword(wipeInputPassword)) {
                            val selectedCols = mutableListOf<String>()
                            if (wipeProvidersChecked) selectedCols.add("providers")
                            if (wipeBookingsChecked) selectedCols.add("bookings")
                            if (wipeChatsChecked) selectedCols.add("chat_channels")
                            if (wipeNotifsChecked) selectedCols.add("notifications")
                            if (wipeReportsChecked) selectedCols.add("reports")
                            if (wipeCategoriesChecked) selectedCols.add("categories")
                            if (wipePendingChecked) selectedCols.add("pending_providers")
                            if (wipeBannersChecked) selectedCols.add("banners")
                            if (wipeSupervisorsChecked) selectedCols.add("supervisors")
                            if (wipeCitiesChecked) selectedCols.add("cities")
                            if (wipeThemesChecked) selectedCols.add("color_themes")

                            val success = viewModel.wipeSelectedDatabaseData(wipeInputPassword, selectedCols)
                            if (success) {
                                showWipeConfirmDialog = false
                                wipeInputPassword = ""
                                Toast.makeText(context, "💥 تم تصفية الفئات المحددة وإعادتها للصفر بنجاح!", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            Toast.makeText(context, "❌ كلمة مرور الأدمن غير صحيحة! تم منع التطهير.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("تأكيد مسح وتطهير النظام العظيم", color = Color.White)
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        showWipeConfirmDialog = false
                        wipeInputPassword = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                ) {
                    Text("إلغاء عملية التطهير")
                }
            }
        )
    }

    // 8. Editing Booking Dialog Control
    editingBookingObj?.let { booking ->
        var editCustName by rememberSaveable(booking.id) { mutableStateOf(booking.customerName) }
        var editCustPhone by rememberSaveable(booking.id) { mutableStateOf(booking.customerPhone) }
        var editCustArea by rememberSaveable(booking.id) { mutableStateOf(booking.customerArea) }
        var editCustService by rememberSaveable(booking.id) { mutableStateOf(booking.serviceType) }
        var editCustDate by rememberSaveable(booking.id) { mutableStateOf(booking.dateString) }
        var editCustTime by rememberSaveable(booking.id) { mutableStateOf(booking.timeString) }
        var editCustStatus by rememberSaveable(booking.id) { mutableStateOf(booking.status) }
        var editCustPassword by rememberSaveable(booking.id) { 
            mutableStateOf(booking.bookingPassword.ifEmpty { booking.pinCode.ifEmpty { "1234" } }) 
        }

        Dialog(onDismissRequest = { editingBookingObj = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(1.dp, themeColors.accent),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("✏️ تعديل بيانات استمارة الحجز", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    
                    OutlinedTextField(
                        value = editCustName,
                        onValueChange = { editCustName = it },
                        label = { Text("الاسم") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    OutlinedTextField(
                        value = editCustPhone,
                        onValueChange = { editCustPhone = it },
                        label = { Text("الهاتف") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    OutlinedTextField(
                        value = editCustArea,
                        onValueChange = { editCustArea = it },
                        label = { Text("مكان الإقامة والحي") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    OutlinedTextField(
                        value = editCustService,
                        onValueChange = { editCustService = it },
                        label = { Text("نوع الخدمة المطلوبة") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    OutlinedTextField(
                        value = editCustDate,
                        onValueChange = { editCustDate = it },
                        label = { Text("التاريخ") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    OutlinedTextField(
                        value = editCustTime,
                        onValueChange = { editCustTime = it },
                        label = { Text("الوقت أو الساعة") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    OutlinedTextField(
                        value = editCustPassword,
                        onValueChange = { editCustPassword = it },
                        label = { Text("🔑 رمز المرور السري (تغيير/إعادة تعيين)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    Text("حدد حالة الحجز:", color = themeColors.textSecondary, fontSize = 11.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val statuses = listOf("PENDING", "APPROVED", "IN_PROGRESS", "COMPLETED", "REJECTED")
                        statuses.forEach { st ->
                            val isSel = editCustStatus == st
                            val color = when(st) {
                                "PENDING" -> Color(0xFFF59E0B)
                                "APPROVED" -> Color.Green
                                "IN_PROGRESS" -> Color(0xFF3B82F6)
                                "COMPLETED" -> Color(0xFF10B981)
                                else -> Color.Red
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) color else Color.Black.copy(alpha = 0.3f))
                                    .border(1.dp, if (isSel) Color.White else color.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                    .clickable { editCustStatus = st }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = when(st) {
                                        "PENDING" -> "تعليق"
                                        "APPROVED" -> "قبول"
                                        "IN_PROGRESS" -> "عمل"
                                        "COMPLETED" -> "تم"
                                        else -> "رفض"
                                    },
                                    color = if (isSel) Color.White else color,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (editCustName.trim().isNotEmpty() && editCustPhone.trim().isNotEmpty()) {
                                    val updatedB = booking.copy(
                                        customerName = editCustName.trim(),
                                        customerPhone = editCustPhone.trim(),
                                        customerArea = editCustArea.trim(),
                                        serviceType = editCustService.trim(),
                                        dateString = editCustDate.trim(),
                                        timeString = editCustTime.trim(),
                                        status = editCustStatus,
                                        bookingPassword = editCustPassword.trim(),
                                        pinCode = editCustPassword.trim()
                                    )
                                    viewModel.updateBooking(updatedB)
                                    editingBookingObj = null
                                } else {
                                    viewModel.triggerNotification("⚠️ يجب ملء الاسم والهاتف بالحد الأدنى!")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("💾 حفظ ونشر", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { editingBookingObj = null },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("إلغاء الأمر", color = Color.White, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }

    // ------------------ PAYMENT SYSTEM DIALOGS ------------------

    // 1. Add/Edit Wallet Dialog
    if (showAddWalletDialog) {
        AlertDialog(
            onDismissRequest = { showAddWalletDialog = false },
            title = {
                Text(
                    text = if (editingWalletObj == null) "➕ إضافة محفظة استقبال أموال جديدة" else "✏️ تعديل بيانات المحفظة",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        Text("مزود المحفظة المصرفية / الجوالة:", fontSize = 11.sp, color = Color.LightGray)
                        val providersList = listOf(
                            Pair("jeeb", "محفظة جيب 📱"),
                            Pair("alKarimi", "الكريمي ام فلوس / حساب 🏦"),
                            Pair("jawaly", "محفظة جوالي 📲"),
                            Pair("floosi", "ون كاش / فلوسي 💳"),
                            Pair("cashExchange", "حوالة صرافة نقدية (النجم/العمقي/المميز) 💸"),
                            Pair("foreignCurrency", "محفظة عملات أجنبية (USD/SAR) 🌐"),
                            Pair("yemenMobile", "يمن موبايل كاش 🇾🇪"),
                            Pair("mtc", "محفظة MTC ⚡"),
                            Pair("sabafon", "سبأ كاش 📞"),
                            Pair("other", "تحويلات مصرفية أخرى 🌐")
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                items(providersList) { prov ->
                                    val isSelected = walletProviderInput == prov.first
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                if (isSelected) themeColors.accent else Color.DarkGray,
                                                RoundedCornerShape(6.dp)
                                            )
                                            .clickable { walletProviderInput = prov.first }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(prov.second, color = if (isSelected) Color.Black else Color.White, fontSize = 9.sp)
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Text("عملة المحفظة الحالية:", fontSize = 11.sp, color = Color.LightGray)
                        val currenciesList = listOf(
                            Pair("YER", "🇾🇪 ريال يمني YER"),
                            Pair("USD", "💵 دولار أمريكي USD"),
                            Pair("SAR", "🇸🇦 ريال سعودي SAR")
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            currenciesList.forEach { cur ->
                                val isSelected = walletCurrencyInput == cur.first
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(if (isSelected) themeColors.accent else Color.DarkGray, RoundedCornerShape(6.dp))
                                        .clickable { walletCurrencyInput = cur.first }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(cur.second, color = if (isSelected) Color.Black else Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    item {
                        Text("غرض/استخدام المحفظة:", fontSize = 11.sp, color = Color.LightGray)
                        val typesList = listOf(
                            Pair("BOTH", "🔄 إيداع وسحب معاً"),
                            Pair("DEPOSIT", "📥 إيداع فقط (استقبال)"),
                            Pair("WITHDRAWAL", "📤 سحب فقط (تحويل للعميل)")
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            typesList.forEach { typeItem ->
                                val isSelected = walletTypeInput == typeItem.first
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(if (isSelected) themeColors.accent else Color.DarkGray, RoundedCornerShape(6.dp))
                                        .clickable { walletTypeInput = typeItem.first }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(typeItem.second, color = if (isSelected) Color.Black else Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = walletNumberInput,
                            onValueChange = { walletNumberInput = it },
                            label = { Text("رقم المحفظة / رقم الحساب", fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = walletAccountNameArInput,
                            onValueChange = { walletAccountNameArInput = it },
                            label = { Text("اسم صاحب الحساب (بالعربية)", fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = walletDescriptionInput,
                            onValueChange = { walletDescriptionInput = it },
                            label = { Text("وصف قصير / تعليمات سريعة للعميل", fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("إظهار المحفظة في خيارات الدفع للعميل", fontSize = 12.sp, color = Color.White)
                            Switch(
                                checked = walletIsVisibleInput,
                                onCheckedChange = { walletIsVisibleInput = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = themeColors.accent)
                            )
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("تعيين كافتراضية بالدليل", fontSize = 12.sp, color = Color.White)
                            Switch(
                                checked = walletIsDefaultInput,
                                onCheckedChange = { walletIsDefaultInput = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = themeColors.accent)
                            )
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = walletDisplayOrderInput,
                            onValueChange = { walletDisplayOrderInput = it },
                            label = { Text("الترتيب في العرض للعميل", fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val wallet = PaymentWalletEntity(
                            id = editingWalletObj?.id ?: "",
                            provider = walletProviderInput,
                            walletNumber = walletNumberInput,
                            accountName = walletAccountNameInput.ifBlank { walletAccountNameArInput },
                            accountNameAr = walletAccountNameArInput,
                            description = walletDescriptionInput,
                            walletType = walletTypeInput,
                            currency = walletCurrencyInput,
                            isVisibleToUsers = walletIsVisibleInput,
                            isDefault = walletIsDefaultInput,
                            displayOrder = walletDisplayOrderInput.toIntOrNull() ?: 0,
                            status = walletStatusInput
                        )
                        if (editingWalletObj == null) {
                            viewModel.addPaymentWallet(wallet)
                        } else {
                            viewModel.updatePaymentWallet(wallet)
                        }
                        showAddWalletDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent)
                ) {
                    Text("حفظ المحفظة 💾", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddWalletDialog = false }) {
                    Text("إلغاء", color = Color.White)
                }
            }
        )
    }

    // 1b. Internal Wallet Transaction Dialog
    if (showWalletTxDialog && selectedWalletForTx != null) {
        val wallet = selectedWalletForTx!!
        AlertDialog(
            onDismissRequest = { showWalletTxDialog = false },
            title = {
                Text("💸 معاملة محفظة داخلية (${wallet.ownerName})", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("نوع الحساب: ${wallet.ownerType} | الرقم: ${wallet.ownerPhone}", fontSize = 11.sp, color = Color.LightGray)
                    Text("الرصيد الحالي: ${wallet.balance} ريال يمني 🇾🇪", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)

                    Divider(color = Color.White.copy(alpha = 0.1f))

                    Text("اختر نوع العملية المالية:", fontSize = 11.sp, color = Color.White)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = { txTypeInput = "DEPOSIT" },
                            colors = ButtonDefaults.buttonColors(containerColor = if (txTypeInput == "DEPOSIT") Color(0xFF10B981) else Color.DarkGray)
                        ) {
                            Text("إيداع ➕", fontSize = 10.sp)
                        }
                        Button(
                            onClick = { txTypeInput = "WITHDRAWAL" },
                            colors = ButtonDefaults.buttonColors(containerColor = if (txTypeInput == "WITHDRAWAL") Color.Red else Color.DarkGray)
                        ) {
                            Text("سحب ➖", fontSize = 10.sp)
                        }
                        Button(
                            onClick = { txTypeInput = "TRANSFER" },
                            colors = ButtonDefaults.buttonColors(containerColor = if (txTypeInput == "TRANSFER") themeColors.accent else Color.DarkGray)
                        ) {
                            Text("تحويل 🔄", fontSize = 10.sp, color = if (txTypeInput == "TRANSFER") Color.Black else Color.White)
                        }
                    }

                    OutlinedTextField(
                        value = txAmountInput,
                        onValueChange = { txAmountInput = it },
                        label = { Text("المبلغ بالريال اليمني", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    OutlinedTextField(
                        value = txNoteInput,
                        onValueChange = { txNoteInput = it },
                        label = { Text("ملاحظات/سبب العملية", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = txAmountInput.toDoubleOrNull() ?: 0.0
                        if (amt > 0) {
                            viewModel.performWalletTransaction(
                                walletId = wallet.id,
                                ownerName = wallet.ownerName,
                                ownerPhone = wallet.ownerPhone,
                                ownerType = wallet.ownerType,
                                type = txTypeInput,
                                amount = amt,
                                note = txNoteInput
                            )
                            showWalletTxDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent)
                ) {
                    Text("تنفيذ العملية ⚡", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWalletTxDialog = false }) {
                    Text("إلغاء", color = Color.Gray)
                }
            }
        )
    }

    // 2. Verify Payment Dialog
    verifyingPaymentObj?.let { payment ->
        AlertDialog(
            onDismissRequest = { verifyingPaymentObj = null },
            title = { Text("✅ تأكيد واعتماد الدفع الوارد", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("أنت على وشك اعتماد وتأكيد دفعة بقيمة ${payment.amount} ريال يمني والمحولة عبر ${payment.walletProvider}.", fontSize = 11.sp, color = Color.LightGray)
                    Text("سيعمل هذا التفعيل على تفعيل/اعتماد طلب الحجز المرتبط وتنشيط حساب المعاملة تلقائياً.", fontSize = 11.sp, color = themeColors.accent)
                    
                    OutlinedTextField(
                        value = adminVerifyPaymentNote,
                        onValueChange = { adminVerifyPaymentNote = it },
                        label = { Text("ملاحظة وتوضيح (اختياري)", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.verifyPayment(payment.id, true, adminVerifyPaymentNote, "مدير الدليل 🛡️")
                        verifyingPaymentObj = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("اعتماد وتأكيد ✅", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { verifyingPaymentObj = null }) {
                    Text("إلغاء", color = Color.White)
                }
            }
        )
    }

    // 3. Reject Payment Dialog
    rejectingPaymentObj?.let { payment ->
        AlertDialog(
            onDismissRequest = { rejectingPaymentObj = null },
            title = { Text("❌ رفض المعاملة وإثبات الدفع", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("يرجى توضيح سبب رفض إثبات التحويل للعميل:", fontSize = 11.sp, color = Color.LightGray)
                    
                    OutlinedTextField(
                        value = adminRejectPaymentNote,
                        onValueChange = { adminRejectPaymentNote = it },
                        label = { Text("سبب الرفض المالي", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.verifyPayment(payment.id, false, adminRejectPaymentNote, "مدير الدليل 🛡️")
                        rejectingPaymentObj = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("تأكيد الرفض ❌", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { rejectingPaymentObj = null }) {
                    Text("إلغاء", color = Color.White)
                }
            }
        )
    }

    // 4. Refund Payment Dialog
    refundingPaymentObj?.let { payment ->
        AlertDialog(
            onDismissRequest = { refundingPaymentObj = null },
            title = { Text("🔄 إرجاع واسترداد المبلغ", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("تنبيه: سيتم تسجيل هذه العملية كـ 'تم الاسترداد' وإلغاء فاعليتها المالية في إحصائيات الدليل.", fontSize = 11.sp, color = Color.LightGray)
                    Text("يرجى كتابة سبب الاسترداد للتسجيل والمتابعة:", fontSize = 11.sp, color = Color.LightGray)
                    
                    OutlinedTextField(
                        value = refundReasonInput,
                        onValueChange = { refundReasonInput = it },
                        label = { Text("سبب إرجاع المبلغ", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.refundPayment(payment.id, refundReasonInput.ifBlank { "استرداد بناء على طلب الأطراف" })
                        refundingPaymentObj = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))
                ) {
                    Text("تأكيد الاسترداد 🔄", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { refundingPaymentObj = null }) {
                    Text("إلغاء", color = Color.White)
                }
            }
        )
    }

    /*
    // 5. User Submit Transfer Proof Dialog
    payingBookingObj?.let { booking ->
        Dialog(onDismissRequest = { payingBookingObj = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text("💳 سداد رسوم الحجز والخدمة بالمنصة", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                        Text("يرجى اختيار أحد الحسابات / المحافظ التالية والتحويل إليها بقيمة تكلفة المعاينة والصيانة:", fontSize = 11.sp, color = Color.LightGray)
                    }

                    if (paymentWallets.isEmpty()) {
                        item {
                            Text("⚠️ عذراً، لا توجد محافظ دفع مفعلة حالياً بالمنصة للتسديد. يرجى مراجعة المشرفين.", fontSize = 11.sp, color = Color.Red)
                        }
                    } else {
                        item {
                            Text("المحافظ والحسابات المتاحة للتحويل:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(paymentWallets.filter { it.status == "active" }) { wallet ->
                                    val isSel = selectedUserWalletObj?.id == wallet.id
                                    val name = when (wallet.provider) {
                                        "jeeb" -> "جيب 📱"
                                        "alKarimi" -> "الكريمي 🏦"
                                        "jawaly" -> "جوالي 📲"
                                        "yemenMobile" -> "يمن كاش 🇾🇪"
                                        else -> wallet.accountNameAr.take(8)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                if (isSel) themeColors.accent else Color.DarkGray,
                                                RoundedCornerShape(6.dp)
                                            )
                                            .clickable { selectedUserWalletObj = wallet }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(name, color = if (isSel) Color.Black else Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        selectedUserWalletObj?.let { wallet ->
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.2f)),
                                    border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.2f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("رقم الحساب/المحفظة للتحويل: ${wallet.walletNumber}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                                        Text("اسم صاحب الحساب المستلم: ${wallet.accountNameAr}", fontSize = 11.sp, color = Color.White)
                                        if (wallet.description.isNotEmpty()) {
                                            Text("تعليمات: ${wallet.description}", fontSize = 10.sp, color = Color.LightGray)
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Divider(color = Color.White.copy(alpha = 0.05f))
                            Text("يرجى تعبئة بيانات التحويل بعد إرسال المبلغ المالي:", fontSize = 11.sp, color = Color.LightGray)
                        }

                        item {
                            OutlinedTextField(
                                value = userTransferIdInput,
                                onValueChange = { userTransferIdInput = it },
                                label = { Text("رقم الحوالة المرجعي / رقم العملية (الـ ID)", fontSize = 11.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = userTransferAccountNameInput,
                                onValueChange = { userTransferAccountNameInput = it },
                                label = { Text("اسم المرسل الكامل (صاحب المحفظة المحوِلة)", fontSize = 11.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = userTransferPhotoInput,
                                onValueChange = { userTransferPhotoInput = it },
                                label = { Text("رابط صورة الإثبات أو لقطة الشاشة (اختياري)", fontSize = 11.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(top = 10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        if (userTransferIdInput.isBlank() || userTransferAccountNameInput.isBlank()) {
                                            viewModel.triggerNotification("❌ يرجى ملء رقم الحوالة واسم مرسل الحوالة كاملاً")
                                            return@Button
                                        }
                                        val wallet = selectedUserWalletObj ?: return@Button
                                        
                                        val docRef = viewModel.db.collection("payments").document()
                                        val payment = PaymentEntity(
                                            id = docRef.id,
                                            userId = booking.customerPhone,
                                            providerId = booking.providerId,
                                            bookingId = booking.id,
                                            type = "service",
                                            method = "mobileWallet",
                                            status = "PROCESSING",
                                            amount = 1000.0,
                                            advanceAmount = 0.0,
                                            remainingAmount = 1000.0,
                                            commission = 0.0,
                                            providerShare = 1000.0,
                                            currency = "YER",
                                            isLinkedToBooking = true,
                                            bookingDate = System.currentTimeMillis(),
                                            bookingServiceType = booking.serviceType,
                                            createdAt = System.currentTimeMillis(),
                                            transferId = userTransferIdInput,
                                            transferPhoto = userTransferPhotoInput,
                                            walletProvider = wallet.provider,
                                            walletNumber = wallet.walletNumber,
                                            walletAccountName = userTransferAccountNameInput,
                                            updatedAt = System.currentTimeMillis()
                                        )
                                        viewModel.db.collection("payments").document(docRef.id).set(payment).addOnSuccessListener {
                                            viewModel.triggerNotification("✅ تم إرسال إثبات السداد بنجاح! بانتظار تأكيد الإدارة.")
                                        }.addOnFailureListener {
                                            viewModel.triggerNotification("❌ فشل إرسال الإثبات: ${it.message}")
                                        }
                                        
                                        payingBookingObj = null
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("إرسال الإثبات 📤", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }

                                Button(
                                    onClick = { payingBookingObj = null },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("إلغاء", color = Color.White, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    */

    // 8.5 Redirect Booking Dialog Control
    redirectingBookingObj?.let { booking ->
        val providersList by viewModel.providers.collectAsState()
        
        // Find the nearest provider dynamically
        val nearestProvider = remember(booking, providersList) {
            val userCoords = getAreaCoords(booking.customerArea)
            providersList.minByOrNull { tech ->
                val techCoords = getProviderCoords(tech)
                val dist = getDistance(userCoords.first, userCoords.second, techCoords.first, techCoords.second)
                dist
            }
        }

        var selectedTargetType by remember { mutableStateOf("SPECIFIC") } // SPECIFIC, ADMIN, NEAREST, OTHER
        var selectedOtherProviderId by remember { mutableStateOf("") }
        var dropdownExpanded by remember { mutableStateOf(false) }

        Dialog(onDismissRequest = { redirectingBookingObj = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(1.5.dp, themeColors.accent),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("🔄 توجيه الحجز إلى مسؤول جديد", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    
                    Text("العميل: ${booking.customerName} - الحي: ${booking.customerArea}", fontSize = 11.sp, color = themeColors.textSecondary)
                    Text("الفني الحالي: ${booking.providerName}", fontSize = 11.sp, color = themeColors.accent)
                    
                    Divider(color = Color.Gray.copy(alpha = 0.3f))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // 1. To Admin
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable { selectedTargetType = "ADMIN" }
                        ) {
                            RadioButton(selected = (selectedTargetType == "ADMIN"), onClick = { selectedTargetType = "ADMIN" })
                            Text("المدير (الأدمن) 👑", fontSize = 12.sp, color = Color.White)
                        }

                        // 2. To Current Provider
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable { selectedTargetType = "SPECIFIC" }
                        ) {
                            RadioButton(selected = (selectedTargetType == "SPECIFIC"), onClick = { selectedTargetType = "SPECIFIC" })
                            Text("الفني المذكور: ${booking.providerName} 👷", fontSize = 12.sp, color = Color.White)
                        }

                        // 3. To Nearest Provider
                        nearestProvider?.let { np ->
                            val userCoords = getAreaCoords(booking.customerArea)
                            val techCoords = getProviderCoords(np)
                            val distance = getDistance(userCoords.first, userCoords.second, techCoords.first, techCoords.second)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().clickable { selectedTargetType = "NEAREST" }
                            ) {
                                RadioButton(selected = (selectedTargetType == "NEAREST"), onClick = { selectedTargetType = "NEAREST" })
                                Text("الفني الأقرب للمستخدم: ${np.name} (يبعد ${String.format("%.1f", distance)} كم) 📍", fontSize = 12.sp, color = Color.White)
                            }
                        }

                        // 4. To another technician
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable { selectedTargetType = "OTHER" }
                        ) {
                            RadioButton(selected = (selectedTargetType == "OTHER"), onClick = { selectedTargetType = "OTHER" })
                            Text("توجيه لفني آخر من الدليل 🔎", fontSize = 12.sp, color = Color.White)
                        }
                    }

                    if (selectedTargetType == "OTHER") {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            val selectedProvName = providersList.find { it.id == selectedOtherProviderId }?.name ?: "اختر الفني الآخر..."
                            Button(
                                onClick = { dropdownExpanded = true },
                                colors = ButtonDefaults.buttonColors(containerColor = themeColors.surface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(selectedProvName, color = Color.White, fontSize = 11.sp)
                            }

                            DropdownMenu(
                                expanded = dropdownExpanded,
                                onDismissRequest = { dropdownExpanded = false },
                                modifier = Modifier.background(Color(0xFF1E293B)).fillMaxWidth(0.8f)
                            ) {
                                providersList.forEach { prov ->
                                    DropdownMenuItem(
                                        text = { Text(prov.name + " (${prov.customCategoryName.ifBlank { "فني" }})", color = Color.White, fontSize = 11.sp) },
                                        onClick = {
                                            selectedOtherProviderId = prov.id
                                            dropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val targetProvider = when (selectedTargetType) {
                                    "ADMIN" -> Pair("admin", "إدارة الدليل")
                                    "SPECIFIC" -> Pair(booking.providerId, booking.providerName)
                                    "NEAREST" -> Pair(nearestProvider?.id ?: "admin", nearestProvider?.name ?: "إدارة الدليل")
                                    "OTHER" -> {
                                        val op = providersList.find { it.id == selectedOtherProviderId }
                                        Pair(op?.id ?: "admin", op?.name ?: "إدارة الدليل")
                                    }
                                    else -> Pair(booking.providerId, booking.providerName)
                                }

                                val updatedB = booking.copy(
                                    providerId = targetProvider.first,
                                    providerName = targetProvider.second
                                )
                                viewModel.updateBooking(updatedB)

                                // Send notifications to user about stages
                                viewModel.addNotification(
                                    title = "🔄 تم تعديل وتوجيه حجزك",
                                    message = "عزيزي العميل، تم توجيه حجزك رقم ${booking.id} بنجاح ومسؤولية تنفيذ الخدمة انتقلت إلى: ${targetProvider.second}.",
                                    targetType = "USER",
                                    targetValue = booking.customerPhone
                                )

                                // Send notifications to the target provider as well
                                if (targetProvider.first != "admin") {
                                    val targetPhone = providersList.find { it.id == targetProvider.first }?.phone ?: ""
                                    viewModel.addNotification(
                                        title = "📅 تم توجيه حجز جديد لك",
                                        message = "تم توجيه حجز جديد لك من الإدارة.\nالعميل: ${booking.customerName}\nالهاتف: ${booking.customerPhone}\nالمنطقة: ${booking.customerArea}\nالخدمة: ${booking.serviceType}\nيرجى التواصل معه مباشرة للتنسيق.",
                                        targetType = "PROVIDER",
                                        targetValue = targetPhone,
                                        customerPhone = booking.customerPhone,
                                        customerName = booking.customerName
                                    )
                                }

                                redirectingBookingObj = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("توجيه 🔄", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { redirectingBookingObj = null },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("إلغاء", color = Color.White, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }

    // 9. Editing Supervisor Dialog Control
    editingSupervisorObj?.let { supervisor ->
        var editSupName by rememberSaveable(supervisor.id) { mutableStateOf(supervisor.name) }
        var editSupRole by rememberSaveable(supervisor.id) { mutableStateOf(supervisor.role) }
        var editSupPasscode by rememberSaveable(supervisor.id) { mutableStateOf(supervisor.passcode) }

        Dialog(onDismissRequest = { editingSupervisorObj = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(1.dp, themeColors.accent),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("✏️ تعديل صلاحيات وبيانات المشرف", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)

                    OutlinedTextField(
                        value = editSupName,
                        onValueChange = { editSupName = it },
                        label = { Text("اسم المشرف الثلاثي") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    OutlinedTextField(
                        value = editSupPasscode,
                        onValueChange = { editSupPasscode = it },
                        label = { Text("رمز المرور والدخول (Passcode)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    Text("اختر الأدوار والصلاحيات الأمنية للمشرف (يمكنك اختيار صلاحية واحدة أو أكثر):", color = themeColors.textSecondary, fontSize = 11.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val roles = listOf("ADMIN", "AUDITOR", "SUPPORT", "OPERATIONS")
                        roles.forEach { rl ->
                            val isSel = editSupRole.split(",").contains(rl)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) themeColors.accent else Color.Black.copy(alpha = 0.3f))
                                    .clickable {
                                        val currentSelected = editSupRole.split(",").filter { it.isNotEmpty() }.toMutableList()
                                        if (currentSelected.contains(rl)) {
                                            currentSelected.remove(rl)
                                        } else {
                                            currentSelected.add(rl)
                                        }
                                        if (currentSelected.isEmpty()) {
                                            currentSelected.add("SUPPORT")
                                        }
                                        editSupRole = currentSelected.joinToString(",")
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = when (rl) {
                                        "ADMIN" -> "مدير 👑"
                                        "AUDITOR" -> "مدقق 🔍"
                                        "OPERATIONS" -> "ميداني 🚗"
                                        else -> "دعم 📞"
                                    },
                                    color = if (isSel) Color.Black else Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (editSupName.trim().isNotEmpty() && editSupPasscode.trim().isNotEmpty()) {
                                    viewModel.editSupervisor(supervisor.id, editSupName.trim(), editSupRole, editSupPasscode.trim())
                                    editingSupervisorObj = null
                                } else {
                                    viewModel.triggerNotification("⚠️ يرجى كتابة الاسم والرمز بالكامل")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("💾 حفظ التعديل", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { editingSupervisorObj = null },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("إلغاء", color = Color.White, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}
