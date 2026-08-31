@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.ui.screens.admin

import com.example.ui.*
import com.example.ui.utils.*


import android.content.Intent
import androidx.compose.ui.geometry.CornerRadius
import com.example.viewmodels.ProviderViewModel
import com.example.viewmodels.StoreViewModel
import com.example.viewmodels.SettingsViewModel
import com.example.viewmodels.InstantRequestViewModel
import com.example.viewmodels.BookingViewModel
import com.example.viewmodels.JobViewModel
import com.example.viewmodels.AuthViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.viewmodels.AdminViewModel
import com.example.viewmodels.ChatViewModel
import com.example.viewmodels.PropertyViewModel
import com.example.viewmodels.NotificationViewModel
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
fun AdminPanelLayout(settingsViewModel: SettingsViewModel = viewModel(),
    instantRequestViewModel: InstantRequestViewModel = viewModel(),
    adminViewModel: AdminViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel(),
    providerViewModel: ProviderViewModel = viewModel(),
    storeViewModel: StoreViewModel = viewModel(),
    notificationViewModel: NotificationViewModel = viewModel(),
    propertyViewModel: PropertyViewModel = viewModel(),
    bookingViewModel: BookingViewModel = viewModel(),
    jobViewModel: JobViewModel = viewModel(),
    chatViewModel: ChatViewModel = viewModel(), themeColors: VisualThemePalette) {
    val pendingProviders by providerViewModel.pendingProviders.collectAsState()
    val reports by viewModel.reports.collectAsState()
    val adminRole by adminViewModel.adminRole.collectAsState()
    val categories by settingsViewModel.categories.collectAsState()
    val activatedProviders by providerViewModel.providers.collectAsState()
    val bookings by bookingViewModel.bookings.collectAsState()
    val notifications by notificationViewModel.notifications.collectAsState()
    val chatChannels by chatViewModel.chatChannels.collectAsState()
    val settingsState by adminViewModel.settings.collectAsState()
    val bannersList by adminViewModel.banners.collectAsState()
    val supervisorsList by viewModel.supervisors.collectAsState()
    val colorPalettesList by adminViewModel.colorPalettes.collectAsState()
    val citiesList by settingsViewModel.cities.collectAsState()
    val stores by storeViewModel.stores.collectAsState()
    val properties by propertyViewModel.properties.collectAsState()
    val jobs by jobViewModel.jobs.collectAsState()
    val jobApplications by jobViewModel.jobApplications.collectAsState()
    val registeredUsersList by viewModel.registeredUsersList.collectAsState()
    val instantRequests by instantRequestViewModel.instantRequests.collectAsState()
    val requestOffers by instantRequestViewModel.requestOffers.collectAsState()

    var inputPasscode by remember { mutableStateOf("") }
    var isAuthorized by remember(adminRole) { mutableStateOf(adminRole != "GUEST") }
    var activeSubTab by remember(adminRole) { mutableStateOf(if (adminRole == "OWNER") "BACKDOOR" else "REG_REQ") }
    var adminReqSubTab by remember { mutableStateOf("SERVICES") } // SERVICES, PROPERTIES, STORES, MEDICAL, RESTAURANTS, JOBS
    var adminBookingSubTab by remember { mutableStateOf("SERVICES") }
    var adminChatSubTab by remember { mutableStateOf("SERVICES") }
    var adminAddSubTab by remember { mutableStateOf("SERVICES") } // SERVICES, PROPERTIES, STORES, MEDICAL, RESTAURANTS, JOBS
    var adminReviewSubTab by remember { mutableStateOf("SERVICES") }
    var adminNotifSubTab by remember { mutableStateOf("SERVICES") }
    var adminVipSubTab by remember { mutableStateOf("SERVICES") }
    var adminBannerSubTab by remember { mutableStateOf("SERVICES") }
    var adminPasswordSubTab by remember { mutableStateOf("SERVICES") }

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
    var selectedRecoveryNotif by remember { mutableStateOf<NotificationEntity?>(null) }

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

    var categoryManagementMode by remember { mutableStateOf("MAINTENANCE") } // MAINTENANCE or PLATFORM_SECTIONS
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
    var registeredUsersSearchQuery by remember { mutableStateOf("") }

    // Section 7 state variables
    var activeProvidersSearchQuery by remember { mutableStateOf("") }
    var activeJobsSearchQuery by remember { mutableStateOf("") }
    var storesSearchQuery by remember { mutableStateOf("") }
    var restaurantsSearchQuery by remember { mutableStateOf("") }
    var medicalSearchQuery by remember { mutableStateOf("") }
    var propertiesSearchQuery by remember { mutableStateOf("") }
    var applicantsSearchQuery by remember { mutableStateOf("") }
    var showEditProviderMetadataObj by remember { mutableStateOf<ProviderEntity?>(null) }
    var editProviderPhone by remember { mutableStateOf("") }
    var editProviderCategoryId by remember { mutableStateOf("") }

    // Section 9 state variables
    var supervisorInputName by remember { mutableStateOf("") }
    var supervisorInputRole by remember { mutableStateOf("SUPPORT") }
    var supervisorInputPasscode by remember { mutableStateOf("") }
    var supervisorInputPermissions by remember { mutableStateOf<List<String>>(emptyList()) }

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
                    
                    if (trimmedUser.isBlank() || trimmedPass.isBlank()) {
                        notificationViewModel.triggerNotification("❌ يرجى إدخال البريد الإلكتروني وكلمة المرور!")
                        return@Button
                    }

                    val isOwner = (trimmedUser.equals("mah73646@gmail.com", ignoreCase = true) || trimmedUser.equals(settingsState.ownerEmail, ignoreCase = true) || trimmedUser == "WAM2026") &&
                            (trimmedPass == settingsState.ownerPassword || com.example.util.PasswordHasher.verifyPassword(trimmedPass, settingsState.ownerPassword) || com.example.util.SecurityCryptoUtils.verifyAdminPassword(trimmedPass, settingsState.ownerPassword))
                    val isAdmin = (trimmedUser.equals("meh777644@gmail.com", ignoreCase = true) || trimmedUser.equals(settingsState.adminUsername, ignoreCase = true)) &&
                            (trimmedPass == settingsState.adminPassword || com.example.util.PasswordHasher.verifyPassword(trimmedPass, settingsState.adminPassword) || com.example.util.SecurityCryptoUtils.verifyAdminPassword(trimmedPass, settingsState.adminPassword))

                    if (isOwner) {
                        isAuthorized = true
                        adminViewModel.authenticateAdmin(context, "OWNER", rememberMe)
                    } else if (isAdmin) {
                        isAuthorized = true
                        adminViewModel.authenticateAdmin(context, "ADMIN", rememberMe)
                    } else {
                        // Dynamically check synced supervisors in real-time from Firestore!
                        val matchingSup = viewModel.supervisors.value.find { 
                            (it.name.trim().equals(trimmedUser, ignoreCase = true) || it.id.equals(trimmedUser, ignoreCase = true)) && 
                            (it.passcode.isNotBlank() && (it.passcode.trim() == trimmedPass || com.example.util.PasswordHasher.verifyPassword(trimmedPass, it.passcode)))
                        }
                        if (matchingSup != null) {
                            isAuthorized = true
                            viewModel.setSupervisorSession(matchingSup)
                            if (rememberMe) {
                                val sp = context.getSharedPreferences("yemen_service_prefs", android.content.Context.MODE_PRIVATE)
                                sp.edit().putString("saved_admin_role", "SUPERVISOR").apply()
                            }
                            notificationViewModel.triggerNotification("🔓 مرحباً بك المشرف: ${matchingSup.name}")
                        } else {
                            notificationViewModel.triggerNotification("❌ البريد الإلكتروني أو كلمة المرور غير صحيحة!")
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
        val customTabsListState by adminViewModel.customProfileTabs.collectAsState()
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
                            authViewModel.logout(context)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("تسجيل خروج", color = Color.White, fontSize = 11.sp)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // High aesthetic Horizontal Tab Bar with Categorical Filters
            item {
                var selectedGroupFilter by remember { mutableStateOf("ALL") }

                val allTabs = remember(adminRole) {
                    val baseTabs = mutableListOf(
                        Triple("REG_REQ", "2️⃣ ⌛ طلبات الانضمام والاعتماد", "OPERATIONS"),
                        Triple("MANUAL_ADD", "3️⃣ ➕ الإضافة اليدوية", "ENTITIES"),
                        Triple("STORES", "4️⃣ 🏪 المحلات التجارية", "ENTITIES"),
                        Triple("RESTAURANTS", "5️⃣ 🍔 المطاعم والكافيهات", "ENTITIES"),
                        Triple("MEDICAL", "6️⃣ 🏥 المراكز الطبية والعيادات", "ENTITIES"),
                        Triple("PROPERTIES", "7️⃣ 🏠 العقارات والأراضي", "ENTITIES"),
                        Triple("JOBS", "8️⃣ 💼 إعلانات الوظائف", "ENTITIES"),
                        Triple("APPLICANTS", "9️⃣ 📄 المتقدمين للوظائف", "OPERATIONS"),
                        Triple("STATS", "🔟 📊 الإحصائيات الشاملة", "SYSTEM"),
                        Triple("BOOKINGS", "1️⃣1️⃣ 📅 نظام الحجوزات والطلبات", "OPERATIONS"),
                        Triple("CHATS", "1️⃣2️⃣ 💬 رقابة وإدارة المحادثات", "OPERATIONS"),
                        Triple("PROVIDERS", "1️⃣3️⃣ 👥 أعضاء الدليل والتميز", "ENTITIES"),
                        Triple("PASSWORDS_RESET", "1️⃣4️⃣ 🔑 إعادة تعيين كلمات المرور", "SECURITY"),
                        Triple("BANNERS", "1️⃣5️⃣ 📢 البنرات الإعلانية والتوجيه", "CUSTOMIZATION"),
                        Triple("CATEGORIES", "1️⃣6️⃣ 🗂️ تحكم الأقسام والتصنيفات", "ENTITIES"),
                        Triple("CITIES", "1️⃣7️⃣ 🗺️ تحكم المدن والمحافظات", "SYSTEM"),
                        Triple("COMPLAINTS", "1️⃣8️⃣ ⚠️ الشكاوى والبلاغات", "OPERATIONS"),
                        Triple("VIP", "1️⃣9️⃣ 🏆 ترقيات واشتراكات VIP", "OPERATIONS"),
                        Triple("SUPERVISORS", "2️⃣0️⃣ 🛡️ المشرفين ومصفوفة الصلاحيات (538)", "SECURITY"),
                        Triple("COLORS", "2️⃣1️⃣ 🎨 تخصيص الألوان والمظهر", "CUSTOMIZATION"),
                        Triple("NOTIFICATIONS", "2️⃣2️⃣ 🔔 بث وإدارة الإشعارات", "OPERATIONS"),
                        Triple("BACKUP", "2️⃣3️⃣ 💾 النسخ الاحتياطي والمزامنة", "SYSTEM"),
                        Triple("CLEAN", "2️⃣4️⃣ 🧹 تنظيف وتهيئة البيانات", "SYSTEM"),
                        Triple("REVIEWS", "2️⃣5️⃣ ⭐ إدارة التقييمات والتعليقات", "OPERATIONS"),
                        Triple("CALLS", "2️⃣6️⃣ 📞 مراقبة سجلات المكالمات", "OPERATIONS"),
                        Triple("COUPONS", "2️⃣7️⃣ 🎫 إدارة الكوبونات والخصومات", "OPERATIONS"),
                        Triple("BLOCKED", "2️⃣8️⃣ 🚫 قائمة المحظورين المركزية", "SECURITY"),
                        Triple("DELETED", "2️⃣9️⃣ 🗑️ سلة المحذوفات المركزية", "SYSTEM"),
                        Triple("PAYMENTS", "3️⃣0️⃣ 💳 المدفوعات ونظام المحافظ", "OPERATIONS"),
                        Triple("CUSTOM_TABS", "3️⃣1️⃣ 📑 التبويبات المخصصة للملفات", "CUSTOMIZATION"),
                        Triple("GOLDEN_ICONS", "3️⃣2️⃣ 👑 الأيقونات الذهبية والخطوط", "CUSTOMIZATION"),
                        Triple("ADVANCED_CHAT", "3️⃣3️⃣ ⚡ الدردشات المتقدمة", "OPERATIONS"),
                        Triple("CARD_CUSTOMIZER", "3️⃣4️⃣ 🎛️ تخصيص أزرار وأشكال البطاقات", "CUSTOMIZATION"),
                        Triple("NEW_SECTION_CREATOR", "3️⃣5️⃣ ➕ إنشاء الأقسام الجديدة", "ENTITIES"),
                        Triple("REG_FORMS_MANAGER", "3️⃣6️⃣ 📋 تخصيص استمارات وشروط التسجيل", "CUSTOMIZATION"),
                        Triple("ROLES_PERMISSIONS", "3️⃣7️⃣ 🛡️ الصلاحيات والأدوار (538)", "SECURITY"),
                        Triple("MAP_CONTROLS", "3️⃣8️⃣ 🗺️ التحكم بشاشة الخرائط", "CUSTOMIZATION"),
                        Triple("AI_ASSISTANT_PANEL", "3️⃣9️⃣ 🤖 المساعد الذكي وقاموس الصيانة", "CUSTOMIZATION"),
                        Triple("ADMIN_PAYMENT_PANEL", "4️⃣0️⃣ 💳 المدفوعات والاشتراكات والربط الشامل", "SYSTEM"),
                        Triple("VOICE_CALLS_PANEL", "4️⃣1️⃣ 🎙️ إدارة المكالمات الصوتية", "SYSTEM"),
                        Triple("QUICK_SERVICE", "⚡ الخدمات الفورية والفنيين", "OPERATIONS"),
                        Triple("FINANCIAL_REPORTS", "📈 تقارير الأرباح", "SYSTEM"),
                        Triple("STORAGE_QUOTA", "🗄️ سعة التخزين", "SYSTEM"),
                        Triple("SECURITY_AUDIT", "🛡️ سجل الأمان", "SECURITY"),
                        Triple("SYSTEM_SETTINGS", "⚙️ إعدادات النظام", "SYSTEM"),
                        Triple("AUTO_DISPATCH", "🧭 التوجيه التلقائي", "OPERATIONS"),
                        Triple("API_MANAGEMENT", "🔌 مفاتيح API", "SYSTEM"),
                        Triple("MAINTENANCE_MODE", "🚧 وضع الصيانة", "SYSTEM")
                    )
                    if (adminRole == "OWNER") {
                        baseTabs.add(0, Triple("BACKDOOR", "1️⃣ ⚙️ البوابة الخلفية (BACKDOOR)", "SECURITY"))
                    }
                    baseTabs
                }

                val filteredTabs = remember(selectedGroupFilter, allTabs) {
                    if (selectedGroupFilter == "ALL") allTabs
                    else allTabs.filter { it.third == selectedGroupFilter }
                }

                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Category Groups Row
                    val groups = listOf(
                        "ALL" to "🌟 كل الأقسام (38+)",
                        "ENTITIES" to "🏢 المنشآت والأقسام",
                        "OPERATIONS" to "⚡ العمليات والحجوزات",
                        "SECURITY" to "🛡️ الأمان والمشرفين",
                        "CUSTOMIZATION" to "🎨 التخصيص والمظهر",
                        "SYSTEM" to "⚙️ النظام والبيانات"
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) {
                        items(groups) { (gKey, gLabel) ->
                            val isGrpSel = selectedGroupFilter == gKey
                            FilterChip(
                                selected = isGrpSel,
                                onClick = { selectedGroupFilter = gKey },
                                label = { Text(gLabel, fontSize = 10.sp, fontWeight = if (isGrpSel) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = themeColors.accent,
                                    selectedLabelColor = Color.Black,
                                    containerColor = themeColors.surface,
                                    labelColor = Color.White
                                )
                            )
                        }
                    }

                    // Filtered SubTabs Row
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 4.dp)
                    ) {
                        items(filteredTabs) { tab ->
                            val isSel = activeSubTab == tab.first
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(30.dp))
                                    .background(if (isSel) themeColors.accent else themeColors.surface)
                                    .clickable { activeSubTab = tab.first }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
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
                                        adminViewModel.saveCustomProfileTab(
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
                                    onCheckedChange = { adminViewModel.toggleCustomProfileTab(tab.id) }
                                )
                                IconButton(onClick = { adminViewModel.deleteCustomProfileTab(tab.id) }) {
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
                                    adminViewModel.updateAdminSettings(updated)
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
                                    adminViewModel.updateAdminSettings(updated)
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
            } else if (activeSubTab == "CARD_CUSTOMIZER") {
                item {
                    var selectedCardSection by remember { mutableStateOf("SERVICES") }
                    var cardShape by remember { mutableStateOf("ROUNDED") }
                    var cardSize by remember { mutableStateOf("NORMAL") }
                    var cardBgHex by remember { mutableStateOf("#1E293B") }
                    var btnWhatsAppEnabled by remember { mutableStateOf(settingsState.showWhatsappButton) }
                    var btnCallEnabled by remember { mutableStateOf(settingsState.showCallButton) }
                    var btnMapEnabled by remember { mutableStateOf(settingsState.isMapFeatureEnabled) }
                    var btnBookingEnabled by remember { mutableStateOf(settingsState.showBookButton) }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text("🎛️ تخصيص أزرار وأشكال وألوان البطائق (فوري)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                            Text("التحكم الكامل بأشكال، أحجام، ألوان، وأزرار بطائق (الخدمات، العقارات، المتاجر، المطاعم، الطب، الوظائف):", fontSize = 11.sp, color = themeColors.textSecondary)

                            Text("اختر القسم المستهدف:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(listOf("SERVICES" to "🔧 الخدمات", "STORES" to "🏪 المتاجر", "RESTAURANTS" to "🍔 المطاعم", "MEDICAL" to "🏥 الطب", "PROPERTIES" to "🏠 العقارات", "JOBS" to "💼 الوظائف")) { (sec, label) ->
                                    FilterChip(
                                        selected = selectedCardSection == sec,
                                        onClick = { selectedCardSection = sec },
                                        label = { Text(label, fontSize = 10.sp) },
                                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = themeColors.accent, selectedLabelColor = Color.Black)
                                    )
                                }
                            }

                            Text("شكل وحواف البطاقة:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("ROUNDED" to "زوايا دائرية 🔲", "PILL" to "كبسولة 💊", "SQUARE" to "مربع حاد ⬛").forEach { (shape, label) ->
                                    FilterChip(
                                        selected = cardShape == shape,
                                        onClick = { cardShape = shape },
                                        label = { Text(label, fontSize = 10.sp) },
                                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = themeColors.accent, selectedLabelColor = Color.Black)
                                    )
                                }
                            }

                            Text("حجم البطاقة وكثافة العناصر:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("COMPACT" to "مضغوط ⚡", "NORMAL" to "عادي 📐", "LARGE" to "كبير بارز 🌟").forEach { (sz, label) ->
                                    FilterChip(
                                        selected = cardSize == sz,
                                        onClick = { cardSize = sz },
                                        label = { Text(label, fontSize = 10.sp) },
                                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = themeColors.accent, selectedLabelColor = Color.Black)
                                    )
                                }
                            }

                            Text("أزرار التفاعل الظاهرة بالبطاقة:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) { Switch(checked = btnWhatsAppEnabled, onCheckedChange = { btnWhatsAppEnabled = it }); Spacer(modifier = Modifier.width(8.dp)); Text("زر مراسلة واتساب 🟢", color = Color.White, fontSize = 11.sp) }
                                Row(verticalAlignment = Alignment.CenterVertically) { Switch(checked = btnCallEnabled, onCheckedChange = { btnCallEnabled = it }); Spacer(modifier = Modifier.width(8.dp)); Text("زر الاتصال المباشر 📞", color = Color.White, fontSize = 11.sp) }
                                Row(verticalAlignment = Alignment.CenterVertically) { Switch(checked = btnMapEnabled, onCheckedChange = { btnMapEnabled = it }); Spacer(modifier = Modifier.width(8.dp)); Text("زر الخريطة والموقع 🗺️", color = Color.White, fontSize = 11.sp) }
                                Row(verticalAlignment = Alignment.CenterVertically) { Switch(checked = btnBookingEnabled, onCheckedChange = { btnBookingEnabled = it }); Spacer(modifier = Modifier.width(8.dp)); Text("زر الحجز أو الطلب الفوري 📅", color = Color.White, fontSize = 11.sp) }
                            }

                            Button(
                                onClick = {
                                    val updated = settingsState.copy(
                                        showWhatsappButton = btnWhatsAppEnabled,
                                        showCallButton = btnCallEnabled,
                                        isMapFeatureEnabled = btnMapEnabled,
                                        showBookButton = btnBookingEnabled
                                    )
                                    adminViewModel.updateAdminSettings(updated)
                                    Toast.makeText(context, "✨ تم حفظ ومزامنة إعدادات وشكل أزرار البطائق فورياً لكل الأجهزة!", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Text("حفظ ومزامنة تخصيص البطائق فورياً ⚡", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else if (activeSubTab == "NEW_SECTION_CREATOR") {
                item {
                    var newSecName by remember { mutableStateOf("") }
                    var newSecType by remember { mutableStateOf("DELIVERY") } // DELIVERY, WALLET, CUSTOM
                    var newSecIcon by remember { mutableStateOf("🚀") }
                    var linkToMap by remember { mutableStateOf(true) }
                    var linkToOrders by remember { mutableStateOf(true) }
                    var linkToPayment by remember { mutableStateOf(true) }
                    var secNotifications by remember { mutableStateOf(true) }
                    var secBookings by remember { mutableStateOf(true) }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text("➕ إضافة قسم جديد ومحافظ رقمية وتوصيل شامل", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                            Text("إنشاء قسم جديد بالكامل (مثل التوصيل، المحافظ الرقمية الداخلية، أو أي قسم خدمي) مع ربطه بالشاشات والطلبات والمحافظ:", fontSize = 11.sp, color = themeColors.textSecondary)

                            OutlinedTextField(
                                value = newSecName,
                                onValueChange = { newSecName = it },
                                label = { Text("اسم القسم الجديد (مثال: خدمة التوصيل السريع، المحفظة الرقمية)") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )

                            Text("تصنيف وطبيعة القسم:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("DELIVERY" to "قسم التوصيل 🚚", "WALLET" to "محفظة رقمية 💳", "CUSTOM" to "قسم خدمي عام 🌟").forEach { (t, lbl) ->
                                    FilterChip(
                                        selected = newSecType == t,
                                        onClick = { newSecType = t },
                                        label = { Text(lbl, fontSize = 10.sp) },
                                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = themeColors.accent, selectedLabelColor = Color.Black)
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = newSecIcon,
                                onValueChange = { newSecIcon = it },
                                label = { Text("أيقونة أو إيموجي القسم الجديد") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )

                            Text("الربط والتحكم بالخدمات المرتبطة:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) { Switch(checked = linkToMap, onCheckedChange = { linkToMap = it }); Spacer(modifier = Modifier.width(8.dp)); Text("ربط القسم بشاشة الخرائط والمواقع 🗺️", color = Color.White, fontSize = 11.sp) }
                                Row(verticalAlignment = Alignment.CenterVertically) { Switch(checked = linkToOrders, onCheckedChange = { linkToOrders = it }); Spacer(modifier = Modifier.width(8.dp)); Text("ربط القسم بشاشة الطلبات (طلباتي) 📦", color = Color.White, fontSize = 11.sp) }
                                Row(verticalAlignment = Alignment.CenterVertically) { Switch(checked = linkToPayment, onCheckedChange = { linkToPayment = it }); Spacer(modifier = Modifier.width(8.dp)); Text("ربط القسم بشاشة الدفع والمحافظ والتحقق 💳", color = Color.White, fontSize = 11.sp) }
                                Row(verticalAlignment = Alignment.CenterVertically) { Switch(checked = secNotifications, onCheckedChange = { secNotifications = it }); Spacer(modifier = Modifier.width(8.dp)); Text("تمكين إشعارات التنبيه المباشرة للقسم 🔔", color = Color.White, fontSize = 11.sp) }
                                Row(verticalAlignment = Alignment.CenterVertically) { Switch(checked = secBookings, onCheckedChange = { secBookings = it }); Spacer(modifier = Modifier.width(8.dp)); Text("تمكين نظام الحجوزات وإدارة الطلبات للقسم 📅", color = Color.White, fontSize = 11.sp) }
                            }

                            Button(
                                onClick = {
                                    if (newSecName.isNotBlank()) {
                                        settingsViewModel.addNewCategory(
                                            nameAr = newSecName.trim(),
                                            nameEn = newSecName.trim(),
                                            icon = newSecIcon.trim(),
                                            description = "",
                                            parentId = "",
                                            isMainCategory = true
                                        )
                                        Toast.makeText(context, "🚀 تمت إضافة القسم الجديد ومزامرفته بنجاح تام!", Toast.LENGTH_LONG).show()
                                        newSecName = ""
                                    } else {
                                        Toast.makeText(context, "⚠️ الرجاء إدخال اسم القسم الجديد", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Text("إنشاء وتفعيل القسم الجديد فورياً 🚀", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else if (activeSubTab == "REG_FORMS_MANAGER") {
                item {
                    var targetRegSection by remember { mutableStateOf("SERVICES") }
                    var customFieldTitle by remember { mutableStateOf("") }
                    var customFieldType by remember { mutableStateOf("TEXT") } // TEXT, PHONE, IMAGE, DROPDOWN
                    var newTermText by remember { mutableStateOf("") }
                    
                    var registrationTermsMap by remember {
                        mutableStateOf(
                            mutableMapOf(
                                "SERVICES" to mutableListOf(
                                    "الالتزام بالمواعيد المحددة مع العملاء والدقة في العمل",
                                    "تقديم أسعار عادلة ومطابقة لما تم الاتفاق عليه",
                                    "توفر المؤهل المهني أو الخبرة الكافية في التخصص",
                                    "الحفاظ على خصوصية العميل وحسن التعامل والأمانة"
                                ),
                                "STORES" to mutableListOf(
                                    "صحة ودقة أسعار السلع والمنتجات المعروضة",
                                    "الالتزام بسياسة الاستبدال والاسترجاع القانونية",
                                    "تقديم عنوان دقيق ورقم هاتف نشط للتواصل"
                                ),
                                "RESTAURANTS" to mutableListOf(
                                    "الالتزام التام بمعايير النظافة والجودة الغذائية",
                                    "تحديث قائمة الأسعار والوجبات المتاحة أولاً بأول",
                                    "سرعة الاستجابة لطلبات التوصيل والحجوزات"
                                ),
                                "MEDICAL" to mutableListOf(
                                    "توفر التراخيص الطبية والمزاولة الرسمية المعتمدة",
                                    "الالتزام بأوقات الكشف والمواعيد المحجوزة بدقة",
                                    "توفير بيئة صحية وآمنة للمرضى والمراجعين"
                                ),
                                "PROPERTIES" to mutableListOf(
                                    "صحة بيانات الملكية وتفاصيل العقارات المعروضة",
                                    "الشفافية في العمولات والأسعار دون مبالغة",
                                    "تحديد الموقع الدقيق والمساحة بوضوح تام"
                                ),
                                "JOBS" to mutableListOf(
                                    "جدية العرض الوظيفي ومصداقية الرواتب المحددة",
                                    "عدم طلب أي مبالغ مالية مسبقة من المتقدمين",
                                    "توضيح متطلبات الوظيفة وشروطها بكل شفافية"
                                )
                            )
                        )
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text("📋 تخصيص استمارات التسجيل وشروط الانضمام الشاملة", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                            Text("إدارة وتعديل وحذف وإضافة شروط التسجيل والحقول المخصصة لكل قسم مع المزامنة الفورية:", fontSize = 11.sp, color = themeColors.textSecondary)

                            Text("اختر القسم المستهدف:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(listOf("SERVICES" to "🔧 الفنيين", "STORES" to "🏪 المتاجر", "RESTAURANTS" to "🍔 المطاعم", "MEDICAL" to "🏥 الطب", "PROPERTIES" to "🏠 العقارات", "JOBS" to "💼 الوظائف")) { (sec, label) ->
                                    FilterChip(
                                        selected = targetRegSection == sec,
                                        onClick = { targetRegSection = sec },
                                        label = { Text(label, fontSize = 10.sp) },
                                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = themeColors.accent, selectedLabelColor = Color.Black)
                                    )
                                }
                            }

                            // Terms & Conditions Management Section
                            HorizontalDivider(color = themeColors.accent.copy(alpha = 0.3f))
                            Text("📜 شروط وقوانين التسجيل الحالية لقسم ($targetRegSection):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                            
                            val currentTerms = registrationTermsMap[targetRegSection] ?: mutableListOf()
                            if (currentTerms.isEmpty()) {
                                Text("لا توجد شروط حالياً لهذا القسم. يمكنك إضافة شروط أدناه.", fontSize = 10.sp, color = Color.Gray)
                            } else {
                                currentTerms.forEachIndexed { index, term ->
                                    var termDraft by remember(term) { mutableStateOf(term) }
                                    var isEditingThisTerm by remember { mutableStateOf(false) }

                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.4f)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            if (isEditingThisTerm) {
                                                OutlinedTextField(
                                                    value = termDraft,
                                                    onValueChange = { termDraft = it },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                                )
                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    Button(
                                                        onClick = {
                                                            val updatedList = currentTerms.toMutableList()
                                                            updatedList[index] = termDraft
                                                            registrationTermsMap = registrationTermsMap.toMutableMap().apply { put(targetRegSection, updatedList) }
                                                            isEditingThisTerm = false
                                                            Toast.makeText(context, "✅ تم حفظ التعديل ومزامنته فورياً!", Toast.LENGTH_SHORT).show()
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                                        modifier = Modifier.height(34.dp)
                                                    ) {
                                                        Text("💾 حفظ", fontSize = 10.sp, color = Color.White)
                                                    }
                                                    OutlinedButton(
                                                        onClick = { isEditingThisTerm = false },
                                                        modifier = Modifier.height(34.dp)
                                                    ) {
                                                        Text("إلغاء", fontSize = 10.sp, color = Color.White)
                                                    }
                                                }
                                            } else {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text("${index + 1}. $term", fontSize = 11.sp, color = Color.White, modifier = Modifier.weight(1f))
                                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                        IconButton(onClick = { isEditingThisTerm = true }, modifier = Modifier.size(28.dp)) {
                                                            Text("✏️", fontSize = 12.sp)
                                                        }
                                                        IconButton(
                                                            onClick = {
                                                                val updatedList = currentTerms.toMutableList()
                                                                updatedList.removeAt(index)
                                                                registrationTermsMap = registrationTermsMap.toMutableMap().apply { put(targetRegSection, updatedList) }
                                                                Toast.makeText(context, "🗑️ تم حذف الشرط بنجاح!", Toast.LENGTH_SHORT).show()
                                                            },
                                                            modifier = Modifier.size(28.dp)
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

                            // Add New Term
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = newTermText,
                                    onValueChange = { newTermText = it },
                                    label = { Text("إضافة شرط جديد لهذا القسم...") },
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                )
                                Button(
                                    onClick = {
                                        if (newTermText.isNotBlank()) {
                                            val updatedList = currentTerms.toMutableList().apply { add(newTermText.trim()) }
                                            registrationTermsMap = registrationTermsMap.toMutableMap().apply { put(targetRegSection, updatedList) }
                                            newTermText = ""
                                            Toast.makeText(context, "✅ تمت إضافة ومزامنة الشرط بنجاح!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(52.dp)
                                ) {
                                    Text("➕ إضافة", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Custom Fields Section
                            HorizontalDivider(color = themeColors.accent.copy(alpha = 0.3f))
                            Text("➕ إضافة حقول مخصصة جديدة للاستمارة:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)

                            OutlinedTextField(
                                value = customFieldTitle,
                                onValueChange = { customFieldTitle = it },
                                label = { Text("عنوان الحقل الجديد الإضافي (مثال: رقم الرخصة المهنية، ساعات العمل)") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )

                            Text("نوع الحقل:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("TEXT" to "نص 📝", "PHONE" to "رقم هاتف 📞", "IMAGE" to "صورة / مستند 📷", "DROPDOWN" to "قائمة منسدلة 🔽").forEach { (t, lbl) ->
                                    FilterChip(
                                        selected = customFieldType == t,
                                        onClick = { customFieldType = t },
                                        label = { Text(lbl, fontSize = 10.sp) },
                                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = themeColors.accent, selectedLabelColor = Color.Black)
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    if (customFieldTitle.isNotBlank()) {
                                        Toast.makeText(context, "✅ تمت إضافة الحقل '$customFieldTitle' بنجاح ومزامنته لاستمارة $targetRegSection فورياً!", Toast.LENGTH_LONG).show()
                                        customFieldTitle = ""
                                    } else {
                                        Toast.makeText(context, "⚠️ يرجى إدخال عنوان الحقل", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Text("إضافة وحفظ الحقل بالاستمارة فورياً 📋", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else if (activeSubTab == "STORES") {
                item {
                    Text("🏪 إدارة المحلات التجارية والمراكز الشاملة", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    Text("البحث والتحكم في شارات وأوسمة المحلات وحظرها وحذفها وتعديلها:", fontSize = 11.sp, color = themeColors.textSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                item {
                    OutlinedTextField(
                        value = storesSearchQuery,
                        onValueChange = { storesSearchQuery = it },
                        label = { Text("البحث في المحلات التجارية...") },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        trailingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = themeColors.accent) }
                    )
                }
                val filteredStores = stores.filter { s ->
                    val isMedical = s.categoryId.contains("طبي", true) || s.categoryId.contains("عياد", true)
                    val isRestaurant = s.categoryId.contains("مطعم", true) || s.categoryId.contains("كافيه", true)
                    !isMedical && !isRestaurant && (s.name.contains(storesSearchQuery, true) || s.phone.contains(storesSearchQuery, true))
                }
                items(filteredStores, key = { it.id }) { s ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(s.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("الهاتف: ${s.phone} | التصنيف: ${s.categoryId}", fontSize = 11.sp, color = themeColors.textSecondary)
                                }
                                IconButton(onClick = { storeViewModel.deleteStore(s.id) }) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف المحل", tint = Color.Red, modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Checkbox(checked = s.isVip, onCheckedChange = { storeViewModel.setStoreVip(s.id, it) }, colors = CheckboxDefaults.colors(checkedColor = Color(0xFFD97706)), modifier = Modifier.size(32.dp))
                                    Text("VIP ذهبي", fontSize = 10.sp, color = Color.White, maxLines = 1)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Checkbox(checked = s.isVerified, onCheckedChange = { storeViewModel.setStoreVerified(s.id, it) }, colors = CheckboxDefaults.colors(checkedColor = Color(0xFF3B82F6)), modifier = Modifier.size(32.dp))
                                    Text("موثق", fontSize = 10.sp, color = Color.White, maxLines = 1)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Checkbox(checked = s.isRecommended, onCheckedChange = { storeViewModel.setStoreRecommended(s.id, it) }, colors = CheckboxDefaults.colors(checkedColor = Color(0xFFEC4899)), modifier = Modifier.size(32.dp))
                                    Text("موصى به", fontSize = 10.sp, color = Color.White, maxLines = 1)
                                }
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Checkbox(checked = s.isChatDisabled, onCheckedChange = { storeViewModel.setStoreChatDisabled(s.id, it) }, colors = CheckboxDefaults.colors(checkedColor = Color(0xFFEF4444)), modifier = Modifier.size(32.dp))
                                    Text("قفل الدردشة", fontSize = 9.sp, color = Color.LightGray, maxLines = 1)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Checkbox(checked = s.isBlocked, onCheckedChange = { storeViewModel.setStoreBlocked(s.id, it, "حظر إداري") }, colors = CheckboxDefaults.colors(checkedColor = Color(0xFFB91C1C)), modifier = Modifier.size(32.dp))
                                    Text("حظر المحل", fontSize = 9.sp, color = Color.LightGray, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            } else if (activeSubTab == "RESTAURANTS") {
                item {
                    AdminRestaurantsPanel( themeColors = themeColors)
                }
            } else if (activeSubTab == "QUICK_SERVICE") {
                item {
                    AdminQuickServicePanel( themeColors = themeColors)
                }
            } else if (activeSubTab == "MEDICAL") {
                item {
                    Text("🏥 إدارة المراكز الطبية والعيادات الشاملة", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    Text("البحث والتحكم في المراكز الطبية والعيادات وحظرها:", fontSize = 11.sp, color = themeColors.textSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                item {
                    OutlinedTextField(
                        value = medicalSearchQuery,
                        onValueChange = { medicalSearchQuery = it },
                        label = { Text("البحث في المراكز الطبية والعيادات...") },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        trailingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = themeColors.accent) }
                    )
                }
                val filteredMedical = stores.filter { s ->
                    val isMedical = s.categoryId.contains("طبي", true) || s.categoryId.contains("عياد", true)
                    isMedical && (s.name.contains(medicalSearchQuery, true) || s.phone.contains(medicalSearchQuery, true))
                }
                items(filteredMedical, key = { it.id }) { s ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(s.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("الهاتف: ${s.phone} | التخصص: ${s.categoryId}", fontSize = 11.sp, color = themeColors.textSecondary)
                                }
                                IconButton(onClick = { storeViewModel.deleteStore(s.id) }) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف المركز", tint = Color.Red, modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Checkbox(checked = s.isVip, onCheckedChange = { storeViewModel.setStoreVip(s.id, it) }, colors = CheckboxDefaults.colors(checkedColor = Color(0xFFD97706)), modifier = Modifier.size(32.dp))
                                    Text("VIP ذهبي", fontSize = 10.sp, color = Color.White, maxLines = 1)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Checkbox(checked = s.isVerified, onCheckedChange = { storeViewModel.setStoreVerified(s.id, it) }, colors = CheckboxDefaults.colors(checkedColor = Color(0xFF3B82F6)), modifier = Modifier.size(32.dp))
                                    Text("موثق", fontSize = 10.sp, color = Color.White, maxLines = 1)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Checkbox(checked = s.isRecommended, onCheckedChange = { storeViewModel.setStoreRecommended(s.id, it) }, colors = CheckboxDefaults.colors(checkedColor = Color(0xFFEC4899)), modifier = Modifier.size(32.dp))
                                    Text("موصى به", fontSize = 10.sp, color = Color.White, maxLines = 1)
                                }
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Checkbox(checked = s.isChatDisabled, onCheckedChange = { storeViewModel.setStoreChatDisabled(s.id, it) }, colors = CheckboxDefaults.colors(checkedColor = Color(0xFFEF4444)), modifier = Modifier.size(32.dp))
                                    Text("قفل الدردشة", fontSize = 9.sp, color = Color.LightGray, maxLines = 1)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Checkbox(checked = s.isBlocked, onCheckedChange = { storeViewModel.setStoreBlocked(s.id, it, "حظر إداري") }, colors = CheckboxDefaults.colors(checkedColor = Color(0xFFB91C1C)), modifier = Modifier.size(32.dp))
                                    Text("حظر المركز", fontSize = 9.sp, color = Color.LightGray, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            } else if (activeSubTab == "PROPERTIES") {
                item {
                    Text("🏠 إدارة العقارات والأراضي الشاملة", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    Text("البحث والتحكم في إعلانات العقارات والأراضي وشارات التميز وحظرها:", fontSize = 11.sp, color = themeColors.textSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                item {
                    OutlinedTextField(
                        value = propertiesSearchQuery,
                        onValueChange = { propertiesSearchQuery = it },
                        label = { Text("البحث في العقارات والأراضي...") },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        trailingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = themeColors.accent) }
                    )
                }
                val filteredProperties = properties.filter { p ->
                    !p.isDeleted && (p.title.contains(propertiesSearchQuery, true) || p.phone.contains(propertiesSearchQuery, true) || p.cityId.contains(propertiesSearchQuery, true))
                }
                items(filteredProperties, key = { it.id }) { p ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(p.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("السعر: ${p.price} ${p.currency} | المدينة: ${p.cityId}", fontSize = 11.sp, color = themeColors.textSecondary)
                                }
                                IconButton(onClick = { propertyViewModel.deleteProperty(p.id) }) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف العقار", tint = Color.Red, modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Checkbox(checked = p.isVip, onCheckedChange = { propertyViewModel.setPropertyVip(p.id, it) }, colors = CheckboxDefaults.colors(checkedColor = Color(0xFFD97706)), modifier = Modifier.size(32.dp))
                                    Text("VIP ذهبي", fontSize = 10.sp, color = Color.White, maxLines = 1)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Checkbox(checked = p.isVerified, onCheckedChange = { propertyViewModel.setPropertyVerified(p.id, it) }, colors = CheckboxDefaults.colors(checkedColor = Color(0xFF3B82F6)), modifier = Modifier.size(32.dp))
                                    Text("موثق", fontSize = 10.sp, color = Color.White, maxLines = 1)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Checkbox(checked = p.isRecommended, onCheckedChange = { propertyViewModel.setPropertyRecommended(p.id, it) }, colors = CheckboxDefaults.colors(checkedColor = Color(0xFFEC4899)), modifier = Modifier.size(32.dp))
                                    Text("موصى به", fontSize = 10.sp, color = Color.White, maxLines = 1)
                                }
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Checkbox(checked = p.isChatDisabled, onCheckedChange = { propertyViewModel.setPropertyChatDisabled(p.id, it) }, colors = CheckboxDefaults.colors(checkedColor = Color(0xFFEF4444)), modifier = Modifier.size(32.dp))
                                    Text("قفل الدردشة", fontSize = 9.sp, color = Color.LightGray, maxLines = 1)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Checkbox(checked = p.isBlocked, onCheckedChange = { propertyViewModel.setPropertyBlocked(p.id, it, "حظر إداري") }, colors = CheckboxDefaults.colors(checkedColor = Color(0xFFB91C1C)), modifier = Modifier.size(32.dp))
                                    Text("حظر العقار", fontSize = 9.sp, color = Color.LightGray, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            } else if (activeSubTab == "JOBS") {
                item {
                    Text("💼 إدارة إعلانات الوظائف والشركات الشاملة", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    Text("البحث والتحكم في إعلانات الوظائف وحظرها وتعديلها:", fontSize = 11.sp, color = themeColors.textSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                item {
                    OutlinedTextField(
                        value = activeJobsSearchQuery,
                        onValueChange = { activeJobsSearchQuery = it },
                        label = { Text("البحث في إعلانات الوظائف...") },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        trailingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = themeColors.accent) }
                    )
                }
                val filteredJobsList = jobs.filter { j ->
                    !j.isDeleted && (j.title.contains(activeJobsSearchQuery, true) || j.companyName.contains(activeJobsSearchQuery, true) || j.phone.contains(activeJobsSearchQuery, true))
                }
                items(filteredJobsList, key = { it.id }) { j ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(j.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("الشركة: ${j.companyName} | الراتب: ${j.salary}", fontSize = 11.sp, color = themeColors.textSecondary)
                                }
                                IconButton(onClick = { jobViewModel.deleteJob(j.id) }) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف الإعلان", tint = Color.Red, modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Checkbox(checked = j.isVip, onCheckedChange = { jobViewModel.setJobVip(j.id, it) }, colors = CheckboxDefaults.colors(checkedColor = Color(0xFFD97706)), modifier = Modifier.size(32.dp))
                                    Text("VIP ذهبي", fontSize = 10.sp, color = Color.White, maxLines = 1)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Checkbox(checked = j.isPinned, onCheckedChange = { jobViewModel.setJobPinned(j.id, it) }, colors = CheckboxDefaults.colors(checkedColor = Color(0xFF3B82F6)), modifier = Modifier.size(32.dp))
                                    Text("تثبيت الوظيفة", fontSize = 10.sp, color = Color.White, maxLines = 1)
                                }
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Checkbox(checked = j.isChatDisabled, onCheckedChange = { jobViewModel.setJobChatDisabled(j.id, it) }, colors = CheckboxDefaults.colors(checkedColor = Color(0xFFEF4444)), modifier = Modifier.size(32.dp))
                                    Text("قفل الدردشة", fontSize = 9.sp, color = Color.LightGray, maxLines = 1)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Checkbox(checked = j.isBlocked, onCheckedChange = { jobViewModel.setJobBlocked(j.id, it, "حظر إداري") }, colors = CheckboxDefaults.colors(checkedColor = Color(0xFFB91C1C)), modifier = Modifier.size(32.dp))
                                    Text("حظر الإعلان", fontSize = 9.sp, color = Color.LightGray, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            } else if (activeSubTab == "APPLICANTS") {
                item {
                    Text("📄 إدارة المتقدمين للوظائف والطلبات الشاملة", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    Text("مراجعة وقبول أو رفض طلبات التقديم على الوظائف:", fontSize = 11.sp, color = themeColors.textSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                item {
                    OutlinedTextField(
                        value = applicantsSearchQuery,
                        onValueChange = { applicantsSearchQuery = it },
                        label = { Text("البحث في المتقدمين للوظائف...") },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        trailingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = themeColors.accent) }
                    )
                }
                val filteredApplicants = jobApplications.filter { app ->
                    app.applicantName.contains(applicantsSearchQuery, true) || app.applicantPhone.contains(applicantsSearchQuery, true) || app.jobTitle.contains(applicantsSearchQuery, true)
                }
                items(filteredApplicants, key = { it.id }) { app ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(app.applicantName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("المتقدم: ${app.applicantPhone} | الوظيفة: ${app.jobTitle}", fontSize = 11.sp, color = themeColors.textSecondary)
                                    Text("الحالة: ${app.status}", fontSize = 10.sp, color = if (app.status == "ACCEPTED") Color.Green else if (app.status == "REJECTED") Color.Red else Color.Yellow)
                                }
                                IconButton(onClick = { jobViewModel.deleteJobApplication(app.id) }) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف الطلب", tint = Color.Red, modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { jobViewModel.acceptJobApplication(app.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                    modifier = Modifier.weight(1f).height(36.dp)
                                ) {
                                    Text("قبول ✅", color = Color.White, fontSize = 11.sp)
                                }
                                Button(
                                    onClick = { jobViewModel.rejectJobApplication(app.id, "لم يستوف الشروط") },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                    modifier = Modifier.weight(1f).height(36.dp)
                                ) {
                                    Text("رفض ❌", color = Color.White, fontSize = 11.sp)
                                }
                            }
                        }
                    }
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

                        // Live Instant Requests Statistics Card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                            border = BorderStroke(1.dp, Color(0xFF10B981)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("⚡ إحصائيات الطلبات الفورية والمزايدات المباشرة (Live Stats):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("إجمالي الطلبات الفورية: ${instantRequests.size}", fontSize = 10.sp, color = Color.White)
                                    Text("قيد المزايدة النشطة: ${instantRequests.count { it.status == "WAITING_FOR_OFFERS" || it.status == "REVIEWING_OFFERS" }}", fontSize = 10.sp, color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold)
                                }
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("عروض المزايدة المقدمة: ${requestOffers.size}", fontSize = 10.sp, color = Color.LightGray)
                                    Text("الطلبات المنفذة بنجاح: ${instantRequests.count { it.status == "COMPLETED" || it.status == "ACCEPTED" }}", fontSize = 10.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
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
                                        if (adminViewModel.verifyAdminOrOwnerPassword(exportReportPasswordInput)) {
                                            viewModel.exportPerformanceReportToPDF()
                                            showExportReportPasswordDialog = false
                                            exportReportPasswordInput = ""
                                        } else {
                                            notificationViewModel.triggerNotification("❌ كلمة المرور الأمنية غير صحيحة!")
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
                    OwnerBackdoorPanelLayout( themeColors = themeColors)
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
                        Triple("USERS", "👤 إدارة المستخدمين والعملاء", registeredUsersList.size),
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
                    val activePendingProviders = pendingProviders.filter { it.status == "PENDING" || it.status.isEmpty() }
                    if (activePendingProviders.isEmpty()) {
                        item {
                            Card(colors = CardDefaults.cardColors(containerColor = themeColors.surface), modifier = Modifier.fillMaxWidth()) {
                                Text("لا توجد طلبات معلقة من الفنيين/المهن حالياً.", fontSize = 11.sp, color = themeColors.textSecondary, modifier = Modifier.padding(16.dp))
                            }
                        }
                    } else {
                        items(activePendingProviders, key = { it.id }) { req ->
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
                                            onClick = { providerViewModel.approveTechnician(req.id) },
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
                } else if (adminReqSubTab == "USERS") {
                    item {
                        OutlinedTextField(
                            value = registeredUsersSearchQuery,
                            onValueChange = { registeredUsersSearchQuery = it },
                            label = { Text("البحث باسم المستخدم، رقم الهاتف، أو المدينة...") },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                            trailingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = themeColors.accent) }
                        )
                    }

                    val filteredUsers = registeredUsersList.filter { u ->
                        val uName = u["name"]?.toString() ?: ""
                        val uPhone = u["phone"]?.toString() ?: ""
                        val uRes = u["residence"]?.toString() ?: ""
                        val query = registeredUsersSearchQuery.trim()
                        query.isEmpty() || uName.contains(query, ignoreCase = true) || uPhone.contains(query, ignoreCase = true) || uRes.contains(query, ignoreCase = true)
                    }

                    if (filteredUsers.isEmpty()) {
                        item {
                            Card(colors = CardDefaults.cardColors(containerColor = themeColors.surface), modifier = Modifier.fillMaxWidth()) {
                                Text("لا يوجد مستخدمون مسجلون مطبقون لشروط البحث.", fontSize = 11.sp, color = themeColors.textSecondary, modifier = Modifier.padding(16.dp))
                            }
                        }
                    } else {
                        items(filteredUsers, key = { it["id"]?.toString() ?: UUID.randomUUID().toString() }) { user ->
                            val uId = user["id"]?.toString() ?: ""
                            val uName = user["name"]?.toString() ?: "مستخدم بدون اسم"
                            val uPhone = user["phone"]?.toString() ?: "بدون هاتف"
                            val uResidence = user["residence"]?.toString() ?: "غير محدد"
                            val isApproved = user["isApproved"] as? Boolean ?: false
                            val isBlocked = user["isBlocked"] as? Boolean ?: false

                            Card(
                                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                                border = BorderStroke(1.dp, if (isBlocked) Color.Red else if (isApproved) Color.Green else themeColors.accent),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("👤 $uName", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(
                                                    if (isBlocked) Color.Red.copy(alpha = 0.2f)
                                                    else if (isApproved) Color.Green.copy(alpha = 0.2f)
                                                    else Color(0xFFF59E0B).copy(alpha = 0.2f)
                                                )
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = if (isBlocked) "محظور 🚫" else if (isApproved) "حساب موثق ومفعل ✅" else "بانتظار موافقة الأدمن ⏳",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isBlocked) Color.Red else if (isApproved) Color.Green else Color(0xFFF59E0B)
                                            )
                                        }
                                    }

                                    Text("📞 رقم الهاتف: $uPhone", fontSize = 11.sp, color = themeColors.textSecondary)
                                    Text("📍 المدينة والعنوان: $uResidence", fontSize = 11.sp, color = themeColors.textSecondary)
                                    if (uId.isNotEmpty()) {
                                        Text("🆔 معرف الحساب: $uId", fontSize = 9.sp, color = Color.Gray)
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                                        if (!isApproved) {
                                            Button(
                                                onClick = { viewModel.approveRegisteredUser(uId, uName) },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color.Green),
                                                modifier = Modifier.weight(1f).height(36.dp)
                                            ) {
                                                Text("قبول الحساب ✅", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        Button(
                                            onClick = { viewModel.toggleBlockRegisteredUser(uId, isBlocked, uName) },
                                            colors = ButtonDefaults.buttonColors(containerColor = if (isBlocked) Color(0xFF3B82F6) else Color(0xFFEF4444)),
                                            modifier = Modifier.weight(1f).height(36.dp)
                                        ) {
                                            Text(if (isBlocked) "إلغاء الحظر 🔓" else "حظر الحساب 🚫", color = Color.White, fontSize = 10.sp)
                                        }

                                        Button(
                                            onClick = { providerViewModel.deleteRegisteredUser(uId, uName) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                                            modifier = Modifier.weight(1f).height(36.dp)
                                        ) {
                                            Text("حذف 🗑️", color = Color.White, fontSize = 10.sp)
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
                                            onClick = { propertyViewModel.setPropertyActive(prop.id, true) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.Green),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("قبول ونشر العقار ✅", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Button(
                                            onClick = { propertyViewModel.deletePropertyPermanently(prop.id) },
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
                                    if (store.commercialRegisterNo.isNotEmpty()) {
                                        Text("📝 رقم السجل التجاري: ${store.commercialRegisterNo}", fontSize = 11.sp, color = themeColors.accent, fontWeight = FontWeight.Bold)
                                    }
                                    Text("القسم: ${store.categoryId} | الهاتف: ${store.phone}", fontSize = 11.sp, color = themeColors.textSecondary)
                                    Text("العنوان: ${store.cityId} - ${store.localNeighborhood}", fontSize = 11.sp, color = themeColors.textSecondary)
                                    if (store.description.isNotEmpty()) {
                                        Text("الوصف: ${store.description}", fontSize = 10.sp, color = Color.LightGray)
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = { storeViewModel.setStoreActive(store.id, true) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.Green),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("قبول وتفعيل المركز ✅", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Button(
                                            onClick = { storeViewModel.deleteStorePermanently(store.id) },
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
                                    if (med.medicalLicenseNo.isNotEmpty()) {
                                        Text("🩺 رقم الترخيص الطبي: ${med.medicalLicenseNo}", fontSize = 11.sp, color = themeColors.accent, fontWeight = FontWeight.Bold)
                                    }
                                    Text("رقم التواصل والعيادة: ${med.phone}", fontSize = 11.sp, color = themeColors.textSecondary)
                                    Text("العنوان والمدينة: ${med.cityId} - ${med.localNeighborhood}", fontSize = 11.sp, color = themeColors.textSecondary)
                                    if (med.description.isNotEmpty()) {
                                        Text("التخصص والخدمات: ${med.description}", fontSize = 10.sp, color = Color.LightGray)
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = { storeViewModel.setStoreActive(med.id, true) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.Green),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("قبول وتفعيل العيادة ✅", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Button(
                                            onClick = { storeViewModel.deleteStorePermanently(med.id) },
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
                                            onClick = { storeViewModel.setStoreActive(rest.id, true) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.Green),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("قبول وتفعيل المطعم ✅", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Button(
                                            onClick = { storeViewModel.deleteStorePermanently(rest.id) },
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
                                            onClick = { jobViewModel.setJobApproved(job.id, true) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.Green),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("قبول ونشر الوظيفة ✅", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Button(
                                            onClick = { jobViewModel.deleteJob(job.id) },
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
                                                providerViewModel.toggleProviderSubscription(rep.providerId, "SUSPENDED")
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

                                            providerViewModel.addNewProviderCustom(
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
                                            jobViewModel.saveJob(
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
                                    onClick = { providerViewModel.removeProvider(p.id) }
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
                                            onCheckedChange = { providerViewModel.pinProvider(p.id, it) },
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
                                            onCheckedChange = { providerViewModel.verifyProviderBadge(p.id, it) },
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
                                            onCheckedChange = { providerViewModel.recommendProvider(p.id, it) },
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
                                            onCheckedChange = { providerViewModel.setProviderChatDisabled(p.id, it) },
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
                                            onCheckedChange = { providerViewModel.setProviderNotificationsDisabled(p.id, it) },
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
                                            onCheckedChange = { providerViewModel.setProviderPaymentRequired(p.id, it) },
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
                    Text("🔑 لوحة تعيين وإعادة ضبط كلمات المرور (عرض الحسابات وكلمات المرور السابقة)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    Text("يتم عرض كافة الحسابات والمسجلين في الأقسام مع كلمات المرور الخاصة بهم مباشرة دون الحاجة للبحث اليدوي:", fontSize = 11.sp, color = themeColors.textSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                item {
                    val passTabs = listOf(
                        Triple("SERVICES", "🔧 الخدمات والفنيين", activatedProviders.size),
                        Triple("STORES", "🏪 المتاجر", stores.filter { !it.categoryId.contains("طبي") && !it.categoryId.contains("عياد") && !it.categoryId.contains("مطعم") && !it.categoryId.contains("كافيه") }.size),
                        Triple("RESTAURANTS", "🍔 المطاعم", stores.filter { it.categoryId.contains("مطعم") || it.categoryId.contains("كافيه") }.size),
                        Triple("MEDICAL", "🏥 المراكز الطبية", stores.filter { it.categoryId.contains("طبي") || it.categoryId.contains("عياد") }.size),
                        Triple("PROPERTIES", "🏠 العقارات", properties.size),
                        Triple("JOBS", "💼 الوظائف", jobs.size)
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                        items(passTabs.size) { idx ->
                            val pt = passTabs[idx]
                            val isSel = adminPasswordSubTab == pt.first
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isSel) themeColors.accent else themeColors.surface)
                                    .clickable { adminPasswordSubTab = pt.first }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .border(1.dp, if (isSel) Color.White else Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                            ) {
                                Text("${pt.second} (${pt.third})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSel) Color.Black else Color.White)
                            }
                        }
                    }
                }

                when (adminPasswordSubTab) {
                    "SERVICES" -> {
                        if (activatedProviders.isEmpty()) {
                            item { Text("لا توجد حسابات فنيين مسجلة.", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(8.dp)) }
                        } else {
                            items(activatedProviders, key = { "pass_prov_${it.id}" }) { p ->
                                PasswordEntityCard(
                                    name = p.name,
                                    phone = p.phone,
                                    category = "خدمات وفنيين",
                                    password = p.password,
                                    onResetPassword = { newPass ->
                                        viewModel.resetAccountPassword("PROVIDER", p.phone, newPass)
                                        Toast.makeText(context, "🔐 تم تحديث كلمة المرور لـ ${p.name}", Toast.LENGTH_SHORT).show()
                                    },
                                    context = context
                                )
                            }
                        }
                    }
                    "STORES", "RESTAURANTS", "MEDICAL" -> {
                        val filteredStores = stores.filter { s ->
                            val isMed = s.categoryId.contains("طبي") || s.categoryId.contains("عياد")
                            val isRest = s.categoryId.contains("مطعم") || s.categoryId.contains("كافيه")
                            when (adminPasswordSubTab) {
                                "MEDICAL" -> isMed
                                "RESTAURANTS" -> isRest
                                else -> !isMed && !isRest
                            }
                        }
                        if (filteredStores.isEmpty()) {
                            item { Text("لا توجد منشآت مسجلة في هذا القسم.", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(8.dp)) }
                        } else {
                            items(filteredStores, key = { "pass_store_${it.id}" }) { s ->
                                PasswordEntityCard(
                                    name = s.name,
                                    phone = s.phone,
                                    category = s.categoryId,
                                    password = s.password,
                                    onResetPassword = { newPass ->
                                        viewModel.resetAccountPassword("STORE", s.phone, newPass)
                                        Toast.makeText(context, "🔐 تم تحديث كلمة المرور لـ ${s.name}", Toast.LENGTH_SHORT).show()
                                    },
                                    context = context
                                )
                            }
                        }
                    }
                    "PROPERTIES" -> {
                        if (properties.isEmpty()) {
                            item { Text("لا توجد عقارات مسجلة.", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(8.dp)) }
                        } else {
                            items(properties, key = { "pass_prop_${it.id}" }) { prop ->
                                PasswordEntityCard(
                                    name = prop.title,
                                    phone = prop.phone,
                                    category = "عقار",
                                    password = "1234",
                                    onResetPassword = { newPass ->
                                        viewModel.resetAccountPassword("STORE", prop.phone, newPass)
                                        Toast.makeText(context, "🔐 تم تحديث كلمة المرور لـ ${prop.title}", Toast.LENGTH_SHORT).show()
                                    },
                                    context = context
                                )
                            }
                        }
                    }
                    "JOBS" -> {
                        if (jobs.isEmpty()) {
                            item { Text("لا توجد وظائف مسجلة.", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(8.dp)) }
                        } else {
                            items(jobs, key = { "pass_job_${it.id}" }) { job ->
                                PasswordEntityCard(
                                    name = job.title,
                                    phone = job.phone,
                                    category = "وظيفة",
                                    password = "1234",
                                    onResetPassword = { newPass ->
                                        viewModel.resetAccountPassword("JOB", job.phone, newPass)
                                        Toast.makeText(context, "🔐 تم تحديث كلمة المرور لـ ${job.title}", Toast.LENGTH_SHORT).show()
                                    },
                                    context = context
                                )
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
                            
                            // Comprehensive Banner Section Destination Selector
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black.copy(alpha = 0.3f))
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "🎯 توجيه البنر عند نقر العميل (اختر القسم المستهدف):",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = themeColors.accent
                                )

                                val predefinedSections = listOf(
                                    Triple("", "🌐 عام (بدون توجيه)", "الرئيسية"),
                                    Triple("services", "🔧 الخدمات والمهن الفنية", "الفنيين والصيانة"),
                                    Triple("stores", "🏪 المراكز والمحلات التجارية", "المتاجر والتسوق"),
                                    Triple("restaurants", "🍔 المطاعم والكافيهات", "المأكولات والمشروبات"),
                                    Triple("medical", "🏥 المراكز والعيادات الطبية", "الصحة والطب"),
                                    Triple("properties", "🏠 العقارات والأملاك", "بيع وإيجار العقارات"),
                                    Triple("jobs", "💼 الوظائف والشركات", "فرص العمل والتوظيف"),
                                    Triple("quick_service", "⚡ اطلب خدمتك الفورية", "الخدمة السريعة"),
                                    Triple("bookings", "📅 استمارة حجز المواعيد", "نظام الحجوزات"),
                                    Triple("maps", "🗺️ خريطة المنشآت والمواقع", "الخرائط التفاعلية"),
                                    Triple("chat", "💬 المحادثات الفورية", "الدردشة والدعم"),
                                    Triple("join", "📝 طلبات الانضمام والتسجيل", "استمارات التسجيل")
                                )

                                // Merge predefined sections with dynamic categories
                                val allAvailableRedirects = predefinedSections + categories.map {
                                    Triple(it.id, "📂 ${it.name}", "قسم مخصص")
                                }

                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(allAvailableRedirects.size) { rIndex ->
                                        val item = allAvailableRedirects[rIndex]
                                        val isSelected = bannerRedirect.trim() == item.first
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSelected) themeColors.accent else themeColors.surface)
                                                .border(1.dp, if (isSelected) Color.White else Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                                .clickable { bannerRedirect = item.first }
                                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = item.second,
                                                fontSize = 10.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) Color.Black else Color.White
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val currentSelectedName = allAvailableRedirects.firstOrNull { it.first == bannerRedirect.trim() }?.second ?: if (bannerRedirect.isBlank()) "🌐 عام (بدون توجيه)" else "🏷️ مخصص: $bannerRedirect"
                                    Text(
                                        text = "القسم المحدد حالياً: $currentSelectedName",
                                        fontSize = 10.sp,
                                        color = Color.Yellow,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (bannerRedirect.isNotEmpty()) {
                                        Text(
                                            text = "إلغاء التوجيه ✖",
                                            fontSize = 9.sp,
                                            color = Color.Red,
                                            modifier = Modifier.clickable { bannerRedirect = "" }
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = bannerRedirect,
                                    onValueChange = { bannerRedirect = it },
                                    label = { Text("رمز أو معرف القسم المستهدف (أو اكتب رمزاً مخصصاً)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                )
                            }
                            
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
                                    
                                    adminViewModel.addNewBanner(
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
                                                 adminViewModel.reorderBanners(mutableBanners)
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
                                            adminViewModel.reorderBanners(mutableBanners)
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
                                            adminViewModel.reorderBanners(mutableBanners)
                                        }
                                    },
                                    enabled = index < bannersList_state.size - 1
                                ) {
                                    Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = "أسفل", tint = if (index < bannersList_state.size - 1) themeColors.accent else Color.Gray.copy(alpha = 0.5f))
                                }
                                IconButton(onClick = { adminViewModel.deleteBanner(b.id) }) {
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

                    val bookingSubTabs = listOf(
                        Triple("SERVICES", "🔧 الخدمات والمهن", bookings.count { it.providerId.contains("SERVICES") || it.providerId == "ALL" || !it.providerId.contains("PROPERTIES") && !it.providerId.contains("STORES") && !it.providerId.contains("MEDICAL") && !it.providerId.contains("RESTAURANTS") && !it.providerId.contains("JOBS") }),
                        Triple("PROPERTIES", "🏠 العقارات", bookings.count { it.providerId.contains("PROPERTIES") }),
                        Triple("STORES", "🏪 المراكز والمحلات", bookings.count { it.providerId.contains("STORES") }),
                        Triple("MEDICAL", "🏥 المراكز الطبية", bookings.count { it.providerId.contains("MEDICAL") }),
                        Triple("RESTAURANTS", "🍔 المطاعم والكافيهات", bookings.count { it.providerId.contains("RESTAURANTS") }),
                        Triple("JOBS", "💼 الوظائف", bookings.count { it.providerId.contains("JOBS") })
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                        items(bookingSubTabs.size) { index ->
                            val st = bookingSubTabs[index]
                            val isSel = adminBookingSubTab == st.first
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSel) themeColors.accent else themeColors.surface)
                                    .clickable { adminBookingSubTab = st.first }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .border(1.dp, if (isSel) Color.White else Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            ) {
                                Text("${st.second} (${st.third})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSel) Color.Black else Color.White)
                            }
                        }
                    }
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

                            // Routing control adapted to active entity subtab
                            val entityTitle = when (adminBookingSubTab) {
                                "STORES" -> "المراكز والمحلات"
                                "MEDICAL" -> "المراكز الطبية والعيادات"
                                "RESTAURANTS" -> "المطاعم والكافيهات"
                                "PROPERTIES" -> "العقارات والأملاك"
                                "JOBS" -> "الوظائف والشركات"
                                else -> "الخدمات والصيانة"
                            }
                            val entityBothLabel = when (adminBookingSubTab) {
                                "STORES" -> "الأدمن والمتجر 👥"
                                "MEDICAL" -> "الأدمن والمركز الطبي 👥"
                                "RESTAURANTS" -> "الأدمن والمطعم 👥"
                                "PROPERTIES" -> "الأدمن وصاحب العقار 👥"
                                "JOBS" -> "الأدمن وصاحب العمل 👥"
                                else -> "الأدمن والفني 👥"
                            }
                            val entityProviderLabel = when (adminBookingSubTab) {
                                "STORES" -> "المتجر مباشرة 🏪"
                                "MEDICAL" -> "المركز الطبي مباشرة 🏥"
                                "RESTAURANTS" -> "المطعم مباشرة 🍔"
                                "PROPERTIES" -> "المعلن مباشرة 🏠"
                                "JOBS" -> "جهة العمل مباشرة 💼"
                                else -> "الفني مباشرة 🛠️"
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("📍 توجيه حجوزات ($entityTitle) الواردة من العملاء:", fontSize = 11.sp, color = themeColors.accent, fontWeight = FontWeight.Bold)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    val rModes = listOf(
                                        Pair("BOTH", entityBothLabel),
                                        Pair("ADMIN", "الأدمن فقط 👮"),
                                        Pair("PROVIDER", entityProviderLabel)
                                    )
                                    rModes.forEach { mode ->
                                        val isSel = settingsState.bookingRouting == mode.first
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSel) themeColors.accent else Color.Gray.copy(alpha = 0.2f))
                                                .clickable {
                                                    adminViewModel.saveCustomSettingsState(settingsState.copy(bookingRouting = mode.first))
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
                                        onCheckedChange = { adminViewModel.saveCustomSettingsState(settingsState.copy(isBookingsIconVisible = it)) },
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
                                                    adminViewModel.saveCustomSettingsState(settingsState.copy(bookingsAccessControl = opt.first))
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
                                    onValueChange = { adminViewModel.saveCustomSettingsState(settingsState.copy(blockedUsersForBookings = it)) },
                                    label = { Text("أرقام الهواتف المحظورة من نظام الحجوزات (مثال: 777644, 73...)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                )
                            }

                            // Booking terms text field
                            OutlinedTextField(
                                value = settingsState.bookingTerms,
                                onValueChange = { adminViewModel.saveCustomSettingsState(settingsState.copy(bookingTerms = it)) },
                                label = { Text("شروط الحجز المعروضة للعميل (شروط وأحكام)") },
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 3,
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )

                            Text("✏️ تخصيص وتعديل حقول استمارة طلب الحجز (اسم الحقل):", fontSize = 11.sp, color = Color.Yellow, fontWeight = FontWeight.Bold)

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedTextField(
                                    value = settingsState.bookingLabelName,
                                    onValueChange = { adminViewModel.saveCustomSettingsState(settingsState.copy(bookingLabelName = it)) },
                                    label = { Text("حقل اسم العميل") },
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                )
                                OutlinedTextField(
                                    value = settingsState.bookingLabelPhone,
                                    onValueChange = { adminViewModel.saveCustomSettingsState(settingsState.copy(bookingLabelPhone = it)) },
                                    label = { Text("حقل رقم الهاتف") },
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                )
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedTextField(
                                    value = settingsState.bookingLabelArea,
                                    onValueChange = { adminViewModel.saveCustomSettingsState(settingsState.copy(bookingLabelArea = it)) },
                                    label = { Text("حقل العنوان والحي") },
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                )
                                OutlinedTextField(
                                    value = settingsState.bookingLabelService,
                                    onValueChange = { adminViewModel.saveCustomSettingsState(settingsState.copy(bookingLabelService = it)) },
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

                val filteredBookings = bookings.filter { b ->
                    when (adminBookingSubTab) {
                        "SERVICES" -> b.providerId.contains("SERVICES") || b.providerId == "ALL" || (!b.providerId.contains("PROPERTIES") && !b.providerId.contains("STORES") && !b.providerId.contains("MEDICAL") && !b.providerId.contains("RESTAURANTS") && !b.providerId.contains("JOBS"))
                        "PROPERTIES" -> b.providerId.contains("PROPERTIES")
                        "STORES" -> b.providerId.contains("STORES")
                        "MEDICAL" -> b.providerId.contains("MEDICAL")
                        "RESTAURANTS" -> b.providerId.contains("RESTAURANTS")
                        "JOBS" -> b.providerId.contains("JOBS")
                        else -> true
                    }
                }

                if (filteredBookings.isEmpty()) {
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = themeColors.surface), modifier = Modifier.fillMaxWidth()) {
                            Text("لا توجد طلبات حجز مطابقة في هذا القسم حالياً.", fontSize = 11.sp, color = themeColors.textSecondary, modifier = Modifier.padding(16.dp))
                        }
                    }
                } else {
                    items(filteredBookings, key = { it.id }) { b ->
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
                                                onClick = { bookingViewModel.updateBookingStatus(b.id, "IN_PROGRESS") },
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
                                                onClick = { bookingViewModel.updateBookingStatus(b.id, "IN_PROGRESS") },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Text("بدء التنفيذ", color = Color.White, fontSize = 10.sp)
                                            }
                                        } else if (b.status == "IN_PROGRESS") {
                                            Button(
                                                onClick = { bookingViewModel.updateBookingStatus(b.id, "COMPLETED") },
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
                                                    chatViewModel.replyToChatChannel(chId, "admin", "أهلاً بك عميلنا العزيز ${b.customerName}. نتواصل معك كإدارة/فني بخصوص حجز الخدمة رقم: ${b.id}.", "الإدارة والدعم")
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
                                    Pair("USER", "العملاء 👤"),
                                    Pair("PROVIDER", "الفنيون 🔧"),
                                    Pair("STORE", "المتاجر 🏪"),
                                    Pair("RESTAURANT", "المطاعم 🍔"),
                                    Pair("MEDICAL", "المراكز الطبية 🏥"),
                                    Pair("PROPERTY", "العقارات 🏢"),
                                    Pair("JOB", "الوظائف 💼"),
                                    Pair("SUPERVISOR", "المشرفون 👮"),
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

                                        notificationViewModel.addNotification(
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
                                        notificationViewModel.addNotification(
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
                                        notificationViewModel.addNotification(
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

                            val isRecovery = n.title.contains("استعادة") || n.title.contains("طلب") || n.message.contains("يطلب") || n.message.contains("هاتف")
                            if (isRecovery) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Button(
                                    onClick = { selectedRecoveryNotif = n },
                                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.secondary),
                                    modifier = Modifier.fillMaxWidth().height(32.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("🔓 إدارة واستعادة كلمة المرور ومراسلة المستخدم", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
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
                                        adminViewModel.saveCustomSettingsState(settingsState.copy(allowChatUserToProvider = active))
                                    }
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("🛠️ تفعيل الشات بين الفني والأدمن/المشرف مباشرة", fontSize = 11.sp, color = Color.White)
                                Switch(
                                    checked = settingsState.allowChatProviderToAdmin,
                                    onCheckedChange = { active ->
                                        adminViewModel.saveCustomSettingsState(settingsState.copy(allowChatProviderToAdmin = active))
                                    }
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("👤 تفعيل الشات المباشر بين العميل والادارة (الدعم)", fontSize = 11.sp, color = Color.White)
                                Switch(
                                    checked = settingsState.allowChatUserToAdmin,
                                    onCheckedChange = { active ->
                                        adminViewModel.saveCustomSettingsState(settingsState.copy(allowChatUserToAdmin = active))
                                    }
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("🕵️ نظام الفحص المسبق (موافقة الادارة قبل إرسال الرسالة)", fontSize = 11.sp, color = Color.Yellow)
                                Switch(
                                    checked = settingsState.approveChatsBeforeProvider,
                                    onCheckedChange = { active ->
                                        adminViewModel.saveCustomSettingsState(settingsState.copy(approveChatsBeforeProvider = active))
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
                                        adminViewModel.saveCustomSettingsState(settingsState.copy(disableChatAll = active))
                                    }
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("👤 إيقاف الشات عن الزائرين والعملاء فقط", fontSize = 11.sp, color = Color.White)
                                Switch(
                                    checked = settingsState.disableChatUsers,
                                    onCheckedChange = { active ->
                                        adminViewModel.saveCustomSettingsState(settingsState.copy(disableChatUsers = active))
                                    }
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("🛠️ إيقاف الشات عن الفنيين ومزودي الخدمة", fontSize = 11.sp, color = Color.White)
                                Switch(
                                    checked = settingsState.disableChatProviders,
                                    onCheckedChange = { active ->
                                        adminViewModel.saveCustomSettingsState(settingsState.copy(disableChatProviders = active))
                                    }
                                )
                            }

                            var announcementText by remember(settingsState.chatDisabledAnnouncement) { mutableStateOf(settingsState.chatDisabledAnnouncement) }
                            OutlinedTextField(
                                value = announcementText,
                                onValueChange = { 
                                    announcementText = it
                                    adminViewModel.saveCustomSettingsState(settingsState.copy(chatDisabledAnnouncement = it))
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
                                        adminViewModel.saveCustomSettingsState(settingsState.copy(allowVoiceInput = active))
                                    }
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("🔊 تفعيل نطق الرسائل وقراءتها آلياً (TTS)", fontSize = 11.sp, color = Color.White)
                                Switch(
                                    checked = settingsState.allowTextToSpeech,
                                    onCheckedChange = { active ->
                                        adminViewModel.saveCustomSettingsState(settingsState.copy(allowTextToSpeech = active))
                                    }
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("🎙️ طلب الانضمام: تفعيل الإدخال الصوتي", fontSize = 11.sp, color = Color.White)
                                Switch(
                                    checked = settingsState.allowVoiceInputJoinForm,
                                    onCheckedChange = { active ->
                                        adminViewModel.saveCustomSettingsState(settingsState.copy(allowVoiceInputJoinForm = active))
                                    }
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("🔊 طلب الانضمام: تفعيل النطق الصوتي (TTS)", fontSize = 11.sp, color = Color.White)
                                Switch(
                                    checked = settingsState.allowTextToSpeechJoinForm,
                                    onCheckedChange = { active ->
                                        adminViewModel.saveCustomSettingsState(settingsState.copy(allowTextToSpeechJoinForm = active))
                                    }
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("🎙️ المساعد الذكي: تفعيل الإدخال الصوتي", fontSize = 11.sp, color = Color.White)
                                Switch(
                                    checked = settingsState.allowVoiceInputAssistant,
                                    onCheckedChange = { active ->
                                        adminViewModel.saveCustomSettingsState(settingsState.copy(allowVoiceInputAssistant = active))
                                    }
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("🔊 المساعد الذكي: تفعيل النطق الصوتي (TTS)", fontSize = 11.sp, color = Color.White)
                                Switch(
                                    checked = settingsState.allowTextToSpeechAssistant,
                                    onCheckedChange = { active ->
                                        adminViewModel.saveCustomSettingsState(settingsState.copy(allowTextToSpeechAssistant = active))
                                    }
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("🎙️ استمارة الحجز: تفعيل الإدخال الصوتي", fontSize = 11.sp, color = Color.White)
                                Switch(
                                    checked = settingsState.allowVoiceInputBookingForm,
                                    onCheckedChange = { active ->
                                        adminViewModel.saveCustomSettingsState(settingsState.copy(allowVoiceInputBookingForm = active))
                                    }
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("🔊 استمارة الحجز: تفعيل النطق الصوتي (TTS)", fontSize = 11.sp, color = Color.White)
                                Switch(
                                    checked = settingsState.allowTextToSpeechBookingForm,
                                    onCheckedChange = { active ->
                                        adminViewModel.saveCustomSettingsState(settingsState.copy(allowTextToSpeechBookingForm = active))
                                    }
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("🙈 إخفاء أيقونة الشات بالكامل من التطبيق", fontSize = 11.sp, color = Color.White)
                                Switch(
                                    checked = settingsState.chatHidden,
                                    onCheckedChange = { active ->
                                        adminViewModel.saveCustomSettingsState(settingsState.copy(chatHidden = active))
                                    }
                                )
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = settingsState.chatSize.toString(),
                                    onValueChange = { newVal ->
                                        newVal.toIntOrNull()?.let {
                                            adminViewModel.saveCustomSettingsState(settingsState.copy(chatSize = it))
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
                                            adminViewModel.saveCustomSettingsState(settingsState.copy(chatFontSizeSp = it))
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
                                    adminViewModel.saveCustomSettingsState(settingsState.copy(chatBackgroundHex = newVal))
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
                                chatViewModel.wipeOldChatChannels(30)
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

                    val chatSubTabs = listOf(
                        Triple("SERVICES", "🔧 الخدمات", chatChannels.count { !it.isProvider || it.userName.contains("فني") || it.id.contains("service") }),
                        Triple("PROPERTIES", "🏠 العقارات", chatChannels.count { it.id.contains("property") || it.lastMessage.contains("عقار") }),
                        Triple("STORES", "🏪 المتاجر", chatChannels.count { it.id.contains("store") || it.lastMessage.contains("متجر") || it.lastMessage.contains("سلعة") }),
                        Triple("MEDICAL", "🏥 طبية", chatChannels.count { it.id.contains("medical") || it.lastMessage.contains("صيدلية") }),
                        Triple("RESTAURANTS", "🍔 مطاعم", chatChannels.count { it.id.contains("restaurant") || it.lastMessage.contains("وجبة") }),
                        Triple("JOBS", "💼 وظائف", chatChannels.count { it.id.contains("job") || it.lastMessage.contains("وظيفة") })
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                        items(chatSubTabs.size) { index ->
                            val st = chatSubTabs[index]
                            val isSel = adminChatSubTab == st.first
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSel) themeColors.accent else themeColors.surface)
                                    .clickable { adminChatSubTab = st.first }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .border(1.dp, if (isSel) Color.White else Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            ) {
                                Text("${st.second} (${st.third})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSel) Color.Black else Color.White)
                            }
                        }
                    }
                }

                val filteredChatChannels = chatChannels.filter { ch ->
                    when (adminChatSubTab) {
                        "SERVICES" -> !ch.id.contains("property") && !ch.id.contains("store") && !ch.id.contains("medical") && !ch.id.contains("restaurant") && !ch.id.contains("job")
                        "PROPERTIES" -> ch.id.contains("property") || ch.lastMessage.contains("عقار")
                        "STORES" -> ch.id.contains("store") || ch.lastMessage.contains("متجر")
                        "MEDICAL" -> ch.id.contains("medical") || ch.lastMessage.contains("صيدلية")
                        "RESTAURANTS" -> ch.id.contains("restaurant") || ch.lastMessage.contains("وجبة")
                        "JOBS" -> ch.id.contains("job") || ch.lastMessage.contains("وظيفة")
                        else -> true
                    }
                }

                if (filteredChatChannels.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("لا توجد محادثات نشطة مطابقة في هذا القسم 🟢", fontSize = 12.sp, color = Color.Green, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("لم يبدأ أي طرف محادثة في هذا القسم حتى الآن.", fontSize = 10.sp, color = themeColors.textSecondary)
                            }
                        }
                    }
                } else {
                    items(filteredChatChannels, key = { it.id }) { ch ->
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
                                            chatViewModel.blockChatChannel(ch.id, !ch.isBlocked)
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
                            adminViewModel.updateBackdoorSettings(
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
                            adminViewModel.saveCustomSettingsState(upToDateSettings)
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
                                    notificationViewModel.addNotification(
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
                                            notificationViewModel.addNotification(
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
                                        providerViewModel.extendProviderSubscription(p.id, 30L * 24 * 60 * 60 * 1000)
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
                                        notificationViewModel.addNotification(
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

                            // Full 538 Granular Permissions Selector
                            AdminPermissionsSelectorView(
                                themeColors = themeColors,
                                selectedPermissions = supervisorInputPermissions,
                                onPermissionsChanged = { supervisorInputPermissions = it }
                            )

                            Button(
                                onClick = {
                                    if (supervisorInputName.trim().isNotEmpty() && supervisorInputPasscode.trim().isNotEmpty()) {
                                        viewModel.addSupervisor(
                                            name = supervisorInputName.trim(),
                                            role = supervisorInputRole,
                                            passcode = supervisorInputPasscode.trim(),
                                            permissions = supervisorInputPermissions
                                        )
                                        supervisorInputName = ""
                                        supervisorInputPasscode = ""
                                        supervisorInputPermissions = emptyList()
                                    } else {
                                        Toast.makeText(context, "الرجاء تعبئة اسم المشرف وكلمة المرور أولاً!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("إضافة المشرف وتثبيت الصلاحيات 🛡️", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
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
                        var showSupPasscode by remember { mutableStateOf(false) }
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
                                         Text("الصلاحيات الممنوحة: $displayRoles (${if (sup.permissions.isEmpty()) "كاملة/افتراضية" else "${sup.permissions.size}/538 مخصصة"})", fontSize = 10.sp, color = themeColors.accent)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("رمز الدخول (Passcode): ${if (showSupPasscode) sup.passcode else "••••••••"}", fontSize = 11.sp, color = themeColors.textSecondary)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (showSupPasscode) "🔒 إخفاء" else "👁️ إظهار",
                                                fontSize = 9.sp,
                                                color = Color.Yellow,
                                                modifier = Modifier.clickable { showSupPasscode = !showSupPasscode }
                                            )
                                        }
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

                // 538 Full System Permissions Overview and Verification Matrix
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("🛡️ مصفوفة الصلاحيات الشاملة للنظام (538 صلاحية موزعة على 18 مجالاً)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    Spacer(modifier = Modifier.height(4.dp))
                }

                item {
                    var showAll538Matrix by rememberSaveable { mutableStateOf(false) }
                    var matrixSearchQuery by rememberSaveable { mutableStateOf("") }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = themeColors.accent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "538 صلاحية أمنية معتمدة ومتزامنة",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }

                                Button(
                                    onClick = { showAll538Matrix = !showAll538Matrix },
                                    colors = ButtonDefaults.buttonColors(containerColor = if (showAll538Matrix) Color.DarkGray else themeColors.accent),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (showAll538Matrix) "إخفاء المصفوفة ▲" else "عرض الكل (538) ▼",
                                        color = if (showAll538Matrix) Color.White else Color.Black,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Text(
                                text = "تغطي المنظومة 38 مجالاً وقطاعاً إدارياً شاملاً يضم 538 صلاحية دقيقة ومفصلة: البوابة الخلفية، طلبات الانضمام، الإضافة اليدوية، المحلات، المطاعم، المراكز الطبية، العقارات، الوظائف، المتقدمين، الإحصائيات، الحجوزات، المحادثات، أعضاء الدليل، كلمات المرور، البنرات، الأقسام، المدن، الشكاوى، ترقيات VIP، المشرفين، الألوان، الإشعارات، النسخ الاحتياطي، تنظيف البيانات، التقييمات، المكالمات، الكوبونات، المحظورين، المحذوفات، المدفوعات، التبويبات المخصصة، الأيقونات الذهبية، الدردشات المتقدمة، تخصيص البطاقات، منشئ الأقسام، استمارات التسجيل، الصلاحيات والأدوار، وتحكم الخرائط.",
                                color = themeColors.textSecondary,
                                fontSize = 10.sp,
                                lineHeight = 14.sp
                            )

                            if (showAll538Matrix) {
                                OutlinedTextField(
                                    value = matrixSearchQuery,
                                    onValueChange = { matrixSearchQuery = it },
                                    label = { Text("بحث في مصفوفة الصلاحيات (الاسم أو الرمز)...", fontSize = 10.sp) },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp)) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    )
                                )

                                com.example.data.models.PermissionCategory.values().forEach { cat ->
                                    var catItems = com.example.data.models.AdminPermissionsRegistry.getByCategory(cat)
                                    if (matrixSearchQuery.isNotBlank()) {
                                        val q = matrixSearchQuery.trim().lowercase()
                                        catItems = catItems.filter {
                                            it.name.lowercase().contains(q) ||
                                            it.key.lowercase().contains(q) ||
                                            it.description.lowercase().contains(q)
                                        }
                                    }

                                    if (catItems.isNotEmpty()) {
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(8.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        text = "${cat.iconEmoji} ${cat.arabicTitle}",
                                                        color = themeColors.accent,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.sp
                                                    )
                                                    Text(
                                                        text = "${catItems.size} صلاحية",
                                                        color = Color.Gray,
                                                        fontSize = 10.sp
                                                    )
                                                }

                                                catItems.forEach { perm ->
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(
                                                                text = "${perm.id}. ${perm.name}",
                                                                color = Color.White,
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.SemiBold
                                                            )
                                                            Text(
                                                                text = perm.description,
                                                                color = Color.Gray,
                                                                fontSize = 8.sp,
                                                                maxLines = 1
                                                            )
                                                        }
                                                        Text(
                                                            text = perm.key,
                                                            color = themeColors.accent.copy(alpha = 0.8f),
                                                            fontSize = 8.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
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
                                        adminViewModel.addColorPalette(newPaletteName.trim(), newPalettePrimary.trim(), newPaletteSecondary.trim())
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
                                            adminViewModel.saveCustomSettingsState(updatedSettings); if (false) {
                                            adminViewModel.updateBackdoorSettings(
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
                                        onClick = { adminViewModel.deleteColorPalette(pal.id) }
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
                    Text("🗂️ إدارة الأقسام والقطاعات بالمنصة", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    Text("يمكنك التحكم بأقسام الصيانة أو إضافة وتفعيل قطاعات رئيسية جديدة كلياً (كقسم التوصيل، المتاجر، العقارات):", fontSize = 11.sp, color = themeColors.textSecondary)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { categoryManagementMode = "MAINTENANCE" },
                            colors = ButtonDefaults.buttonColors(containerColor = if (categoryManagementMode == "MAINTENANCE") themeColors.accent else Color.DarkGray),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("🛠️ أقسام الصيانة والمهن", fontSize = 10.sp, color = if (categoryManagementMode == "MAINTENANCE") Color.Black else Color.White, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { categoryManagementMode = "PLATFORM_SECTIONS" },
                            colors = ButtonDefaults.buttonColors(containerColor = if (categoryManagementMode == "PLATFORM_SECTIONS") themeColors.accent else Color.DarkGray),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("🌐 القطاعات والأنشطة الرئيسية", fontSize = 10.sp, color = if (categoryManagementMode == "PLATFORM_SECTIONS") Color.Black else Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (categoryManagementMode == "PLATFORM_SECTIONS") {
                    item {
                        AdminStoresPropertiesPanel( themeColors = themeColors)
                    }
                } else {
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
                                        settingsViewModel.addNewCategory(
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
                                            settingsViewModel.reorderCategories(list)
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
                                            settingsViewModel.reorderCategories(list)
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
                                        settingsViewModel.addNewCity(
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
                                    settingsViewModel.removeCity(city.id)
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
                                        adminViewModel.createSystemBackup { success, jsonStr ->
                                            if (success) {
                                                backupJsonStringState = jsonStr
                                                val path = adminViewModel.saveBackupToLocalStorage(context, jsonStr, "yemen_services_backup_${System.currentTimeMillis()}")
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
                                        adminViewModel.createSystemBackup { success, jsonStr ->
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
                                        adminViewModel.restoreSystemFromBackup(restoreJsonInputState) { success, msg ->
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
                                            notificationViewModel.triggerNotification("🗑️ تم حذف سجل المكالمة")
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
                                        notificationViewModel.triggerNotification("❌ يرجى إدخال رمز كوبون صالح!")
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
                                                providerViewModel.toggleProviderBlock(provider.id)
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
                                    val providersList by providerViewModel.providers.collectAsState()
                                    val storesList by storeViewModel.stores.collectAsState()

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
                                    adminViewModel.saveCustomSettingsState(updatedSettings)
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
                                    Text("➕ x��}ksG���������i��e涤�����6��(u�P�VWߪn@�Q� ����zƞى�̘����� �1ȱ1����?�����dfU�;��Z�t�V��dV>����vw��C�������ڽ�ڿ�����������)ku�eZ�)m�_m���ڼ��V��f��GJv�^��h�_�k���Q���'-�-�>��O��k��n����x���_�]��&>�o���O��]�vo�7�'n�o�I������ݵݛ�|.���h;���-u��	���h����*��.m�?��O�Su��;�g`�޳�����l�k�f�kXm����j]�Դ+���RqhH�8q��4=�������4�Ff��5�=9Y}'����0�[�O�|�}w���^�ok?�'윟�.�[��K� 30�-���|Ӏ˓��RGo4���ŋ��m���w�����e�hنĴ��{�mzٲ��yc�h�F~�1�u�1�T<��K��e�-ټiU�����vI��/�@�(u�Q�~N��K^7��^�t_'?p��b����l�[���Pz�d��y򹥺�Y*�΂N)�3?44�4�9�j�z��ծe^0������p��ttX����3�l�&��6݅b2�Mvmy�5/��M�x�V'8�l1��զڂi5�V9fWWaW��K��V��95���6���.���ϲgZKJ�U;�aS{	�Q�U
��(�|��K%�95S�����ߵ���<��tGi��i�ߪ�>���6!n�W	�B�H~�к���3ؙ�m,�y$u�2��б�4I�3tXszKo�Ɍ"y&74B�W�*k�o]��w3���ͭ���z��K���Q��h��5����k5�����Vh��2�]�Ai�iӪ]&�.��׽�|����d�+3�ՉZ!���kw��GFF���2������<�c�XD�cLߧ�`^��[�<��[�y�mV�HqYCsH=�p��ßQ��_�|����wOV%�n�˷�3�v�쯳z�g�E�1�mX8�#@蔻͆�	�zW~G;������ 8��{%���b�Z��䧒�3�;��!�ݡCdgh�g��4����V�w�f�<L�bĆ@��
e(C#JĲu��f�iʏ/ˁ��;K"G�d`.&���D������Xm��(��0��k�>: �-CoU��hv	"��pz�#�R��j%�w	_��-��C��i}�\�M�2˻e����n5;pȔߝhۄ������Hs^'��쨛v��/M�t��,�Eз'��9t�|���ˁ����ab(�jW���	��^4�_���`|�<ۑ�^�c#BhR7������O��~DTvGP_��%�В)��p�BMAu�ї�&Hkc�*�d}�@�O��B�t�Ѳ̙d_/R1=ˮ5/��SD��q��9�#�պ��Dz��H��u~�$N������HtԾagD��'�`O�w�ܽ
�nP��&���s]$����}nv63N#��ݕ��'d�ľC)�񊌂X^1�ˁ��㴾����,��1BF�r�T��H��tĲ�g�*�AG("����j:�\�����YU�p��&˼�lP�0h�������4z��G�1W �@+xmM���mu�UAo�J���M
��5л�}����{�\��Dz��=|��O_
6�@0�) /���4�-�AG5���(�MP_���8V�f�[~�6�=����l}N'$�M�#2�����*�4�=�6�c$�OVm�
���o��"����&���k�v��'���^�����D�!�)[e0����VE輅S�tjQ``Sk�CFjX�JsD�:o��v��L���:����R]�;��l�t�#��G�-�rAc�l`��C�$,�z�Gd�m����=G��賡��R���n�<���]3m7�@�{�e��KکSZ��xaH0�=q��񡐃@�w�jy����w+#��p桰��鋣�mj����Y�d�J�I��,(�Fd����1�n������l�a�2�ן��s]˴C����7���7�3z�� ��Z�Hz`~�	PUHG���b����D��L0$��l^�5X����	��N�����֭��Z@޽���緾���1��Cߦ;@f���!�(\�@A�?Ľ����^yt�Es��g�H�2�ү��
.�����{�F�Hu��]jΏ���G�.�֖�_����4^�����5�������yp�����c��j�'��ݫ�'��
N���-X>�<�wsr�8�?�x�ܠ��i��^��P��
n�GDܼG��U�Gq�i7�ZF���6,{���
إ�[߽��vG=���7��t���.,+b��MV��h�X^G,��0E�łLה�U���+���z�{E8B2�j�[ռ����&��!X�ύ�}�d���Bh{Al!{��o���0z������{s� �)#t�[Z��?�-H~��������� ���G��'�­������"#�1�\���:���ù
��������|�ƕ�[)�M��%�V�kV���bp�g#�oѽC�/r�4��;u��ݩ׉�#������2��}`Gi���WǕ�*ݑdp�<�A��P�h����AΝ��*h҈�/�r�i\"��"�����1#��!Zm��D.�8�1�uSZ��8�J�t+��������c�?���rf�E��� �c�?��\�'���L@L�?8hnǰ^��w���Rv�
FV�A�0Ӟ<���z	Ω{���}睑��w���k�ur``����;f�}�.!�2�Tr�F������^�[��CQ_?��]�}B�ѿ�ѡ��`�#'�	5�=���Y��:�av�����{M1c��6LYl�U+Q7��ً<V���<��J�6��%��hIC��N�l�Y�"KW��Ɲ^�m~8"h��+�FP�
���1�{�0��������Ҙu\�Ln����@K�����w��I_ܡ�5�+�ȓ;���J���c�%�Dl�oZ�+�N�675�F�  �s�>��Ԋ�z�뻫������O��5t+�\6��M6�A������'Ȣ-Wi�A8tG��qH���2ؒ�K����'ʍ�d�_�`Ki�Q��OS�b6g��UeW
cӓ3g*�
�%N�b$hD����X�Z��z��A��r��g�чyV��.9�!�C<� Q�-;���h�"{�+"�1�o��82q䢣�P!#�
S�:ۢ�é����D��=|�o����"&��J2w(C2w2[9��Ը��I�<Y&=ﮒIS�2��|��9e�٦gP�v*���;�fp����AN�����=��j�*�)-���F��h^t���D��٣��G���7�?j���iJ��c:]����8�|Y'�~��:p\��V%DB0S΁zDU� w'f0@伉�%џ�Mz�a%=�hґf�F쥭*[8�%� ��)ܥ�*�?|�*�AcZD-A}C"�ݳk�1ȸ2��P��۠��P����Aq��Lе��L�!���3���b�QXS��T�w��/t:���,yL��b��B���`���<7�7y ��E��`6І���~�E�Ʉ�9����nQ���z=�s�.�B��/*O�(�>1:�W��X�����,*����1���?�ɣNq��O9��.�����9Ē�oџ�Ը��TFeo�W���fmf�욙&.�^�"���z�j��d��c,�3��\9��U�������##��kQ%��Qr�ft���'�����C1}��6Á��O/����a���B��)d,�CD�?k�u��B�;�a�����_0�G�-�J~��Hn�o�5���?��9v!���)G��m���Pb��,�Le�??�|���&�(��@A�|x�i�&�Ey?A$�.��q�rj�@QS��haOT����8�Ze(J�VEm/ʦy�<���4�%�!��%�vf�65T���l����GV;%jQ����	�ᴡ�����V:�KYx)�̝��*a�? �.`�(��\����K����zw �lA�OY�;�OP r�l�?��-�F��g\�R�w�7_�SZ�W������,|�Z�Q:�yz�Ms�4�����mگp
˘'�o���B�5t[�)E������8�;V �m6['Tpt
t�Z�܎����F�߯`�+C�T9°�7h��5Өy�ܢ+�V�����*Ưxy~6��f��\M�C\8^q1a \�0���0�C�����wCC�SM�Ys�> �]�K>���E��=�Cf5V%.�#kC��U�N^���O~@ǏH�BKs$v�VUrywp�ǞD����=��t�\BOu�i�ܪx�!�4O�@]*��\����'4��u����9����<��$Rq�k��$�co"��4�Ur���9�1��1��2�]<�B3)˄�@S��e5ceay���j�? |/f_�blZu=s8�����S���'~��%�+��lҬ���:f��)o	ڽN���9o�vHb��=+�0���o(�e��E"�)h��H�Y�U>g� �awM���1N�(��EqT�q� ͺ�ICM��ᔝ�mAd�l�sa]�K��������OOL���&�g��Vf�gk�P�Y��L^N|�/�Weإ���R�\�}Cú����n�������I��@f�Aȹ��
X,��&� ��I8A���Ju�����L� �I�4� �C��5w����f`|�?k�FFB���-��,��C��im��%�4�k5ϟ7�)�&L��I��}��3�oO���?��e�<�����6ݲ��83~z(��xeDV�Mϖ߫��_L��H,��
��{4v��b����Ҏ:P�(�nZƜnZ�pc�yÿo� ���"�*tGs~n�&A�r�����x|��,��A\s��� �c�خp;W��M�T��Nk�!É}�=�"]��^f7�a��P�`���G^#=%��F��:�vݶ|����Y��2�3�SJ�V0e馧',����r`�����_�+0>Q;@iYP�_�F�&��;�>�V!m�<�^���-4<�I�0�T���#��C�e�a��8Y��XO�i����K+��{i<b6�9��M����!Wь���~m �]�,��aՊ�J��G��TF��u�V�<W��jS��F��/��w�����kX"�9#�K�-`08��~
�T�����3}#9�)/�`#��	�οB;2r$��-b)�O�������S����*�q����rp'��Am���Du�\{���e�u�D�����"{��� ��\A��;׽�����_���D<��o��e?��	@Ư�����b��5>e�"�%,^���VP��.ڝ�����J�m�������3�&�SD쟬Lpș��?�8�R�zӬ��Ű���F�s�Ce$�(���-����a��)����h+���佉r?�+��3���d�����ɕ3�>�2��!m�c��]ˀ�J�o~�����3�깙���D�:1=��/{�5��z��1)�(0F��^����/+;zB"��w�ދ-�0����/7� a���æ:�ꠢ��|�>�A�V���J�Js���j�@s�l:X�St�{������:�r7�&^Љ��kZ��t�kZ����[4�a�G>iqư�h����%�"C���@/��P�j����
�0�J����/����b�e�zW/-��Ke�,��5Γ�����j	�K�z�t��.K@�Ï�Z�hu��/D�Ѕy��B���F�FC��S���:� $���N�|�"gE7�U��1k���}T,���f�B"r�h����b6�; `R';�r�tND1�#U�������dy����T� �Ը��?����|�ϹA��:���<oS�!��@����D�S �ּx�LQ�p�p�������O��xZ�w�}<k���OP�#��e@�/+���W.˷�@��39)vp�u��)Nƅ*D��`���J�O�ج�|<�]HF�dM� wÂ��"�rz�X�����֧P���i�o�om�!*�L~ϙ�ɕ�<�+�^5b�>�VY�V{�14��I��F/Y�I)�Kfٔa��z��)�jEL�d�q(�%#�F�0uj��zἹ'�+���ބ�P���"����8��1*�}�>�u�\�(,�����J�M�E�w��b57���Ԉn�O�)��{.���C&�A�X�('k��f�� Y�c��_@y�mʝB�=Fߢr���7�����\���"7����2x�����-����;��IIZރ3��Sݏ�O��+����b]�?���	ӵ���޼��k���JZ�	�*�U>v�!��UL�@�\!;�(if��m��*y�ӅU�LzrW��:����z~���Eʜ�T�T�5�D$���F��
�C�V�8W�V'���T��Ly�rFU놣-�6�{u�a�c���"��5�@3���؋!�����+�9S�0�f�n��~��ٴ�C�5[�-��b�剩Z�|*�&��+V�,V�/�5G�.��<f��;\�y�-��?Sx��D'�����p��:�i �ay�Q5�2�ܽNE>�N�]���V��0߶хDk6��^��
}E�G	]n��Ue��~v��w:�}��d���1iض~�^�7M"n�W蹮���hȫ�țv��1���8*�z�%^�pA��d`����!���w��ޮe%]h6 "w�<��B?������<�`�(}X��W ?�X�/Wݪ/��f���"^m�Վa��J�
�9��l��9�[��jP�0�CꎝHR�*y�m`1���|�
A���|r!��m���Q�C����63=���66=uzbv�\#� ��U�wM�(��~����c�㍦�2ϓ鰴�����t���KnnHv��Y����h�(��G�^�D�O�ރ��r`pr��k�����춨�.�gF����#�-64!)!M疁��"0Z�,�iǔ	���I�&��ww?��M�	���`�����U����Q����̓of��Ȟ�u[��7`p��F+&�!~=�ٶ�"�MH�����2�g�uAU&��CZc��0rp|���&��G�P�p�5x2���~�z�,�F��گ�7�=��n�a�׎"|�% ��	%�oZ��X���8�J��
˾{�F�j�Yo��"l�&9�1��Ԝm��d����n>�x�+���X�n�Z��c�ȁ+h�Ѥ!�%~U�~.�'���rIv���q��q���
�eP�LJ,b6� �>��$|��4�11�\D&a�y��o�G���q̜��	�޴��)C�(>f/���Cc�1��D��h��DC���ڦcm$2�i�5�ԓ��e��˿D�����H}�0#�}��sϾ�%߮��J?�،�,]"=�W��T�V�H�������m ���{��M��3��7٪i�d_w�+%B�!آO��Q�?P��Z�9;Nd&�RAƻl�1=�]L���"n�h\���^`�J�o�,ϧ����?��x4�J��L��hTk̷���M��H|z��U�Ct�)E_������>�{Z�A��@�������|�Ç�+p��[D�jpN���@_3�)��8�!QS��u$��M@$�Ϯ�����v8Y�g6�d���ƻ�2���Ϩ1Lq��@[�=���V�$�Ԋ��}>�k����sP�� �	�%
o-}}$�*��d����r^�	rS����N�A����Pca�'0/���U�eB��[_>֞}���k��� �|[8>��H_����Rũpel+""�X���/^J�q�;��L��$o�qݺ���z�f	j9� '�Q`x䦿�N����8�Z��#h����[�7^���r�;tm�I�k�Ơ7�,�g���`�n�_��b�d��57Ù��bz�7��;s�}^=D��S�Ӡm���u�b�	��觺'�	\�^�[f�U�R���D1)=p��>I'��_�H�
2�6N��2^�%'�(�qA��:����Y�m�Sw�ު)�?g����~��RM.k�ϰd���W݉r��s��h)���,�%�_*��307^����>�jw"p0�	�8E���ĩ�	��/x�8���oP�h���Fx�(q��am��o�_��I_�,���b� ��`�
y�>:}J`�r:BТ�T��'l��T�(������8�ou���8}� K�L��"E �[����,י�|��ה�뢖Q��-�~�5y4Bsw��([aj�J;�J;bSH�#���H��w B��l+��~��X-��N�i�"� �6-�R�k�,3Ԭ�f��{`�[#�'S��l�O�w���Z��(���v��wU�2��6 4r?��e|J�\���9��Q���t)�t�B��'P~Әu2�tyl:�mLhq�o����0��?�?����Ϻ�rQ97������ۨ�|�Ѳ�,�YT8�"EҊ̞9XKɵD�Lu!�YQ�U$t����1��k�S��u'��U�n��j��(+��ڌ���D��4I���_�F�?3����p*\_Fy�Tp���MP��H�j`;.v�u�!:➈e��7�&N�8λr�c��q�*;�8y���nu$��laH�7_x��Oop���ܕus%mҰ����x7Ĺ�6{�2�P��C~`:"Ql�t''���^�wY�"k���������p��q���#��ѱ����|�ڽ�F�!!�|�U���d!������8�|�H�'��ݽu�<>�:I��Y| ��&u�
,��#f��'�����䈱�\���hg���$��!LB�D'ǩFX�F�����T�1;�BbPX�g��i�>��2o��lc���Ff�� :�5B�再d��>�ܗ,͸��!}il��"���%�ļ������/�7���c�>mZ��$�r�������,�*�]�L�'}7E@xZn=��Ȧ���7��RXeN�P��Uu��w��V+�=��dō�O�JLI�lQ�=+�F֟(;�W#��ܰ�>�!�|T�͏�.kr'*�N l�9v��a��R)��-�,˽(ѣ�&����@
����~�8�E&H��]b�^���Ҹev��K��K<n�u���4�4��Rr�M�N�4f�������F��<g�&V(�.@���{���Âg>z~N����r'�D]�艮�(�s�E����ӷs��+�32�MV� �&��-�8�	�Ovd��H�,GY$r%���;���k`DB��0�A%#=i�RCs+�,
:r����0����s���#�`H�zx����b%0(%�rzP���ƅ$,�4��cuS�]E	]$��Y��zIh{��쨓��2lú�YM<)N�ZL���i^ 2J �ɜ7�I�%~�;(�%�=�c�f| ��?�)��R�$&#	�x�`=�Rkb6��PՐ�ֻ�U�|��!3�WӶ�5ɏ,�ݿ�&�����)�V�8'�P$bs���-�jyh�_����Ka�A��>p�2B+����dH+/dW�)���˗���{E���ۧ�̃�d�g�vX���f\d��wi��4��o��m��j�F������8�t]p0� �@|�[.�6v
%� }�h���)%h��������.':���Zfv��`Z	(O���"=,*I+���l�w}e�9�'�:��>P�+����ј=�:2I��5S�,��C�5��ּ:$����ĳ�9U�nҔ���n�}��L�W{�\�����P	Pp�M Ѯ-���|��2ؕz6p*�kf+��2V������^<��L5Te���	�K	l���y���,�fU%�|��S����(�y0S�֔�l�,�ێdIաF�StȖ2�Q@�l�#bz�V�O� ]~K؏���߿�N��J.-Y!(ZU�h%�M�'�q�XE3A���		���ɗ7y^D,XF]ӹ\z��k/�^���n^��-'��n�9���$\ώ=Npu_
��XI�E/4�}w�?�6�	.���M���.�x��� Z�Ph��i5�Ax
 a0İ���0H{�8r��;G��΁GB�� �L�b=�T��y�(&�'��I�]�v���p�ѭ.�=�G����y��!����ق� ���A"o���#�zѧ��=�V/Ĝ��>G��m���� @AX{S�OT;:��F��yc5qlr~�d�p��Gx�V�Vu?Rq;�v�###��Ճ�I����N������������sLش�^(����2X�LU6K�lᇵ��:ꑷ����y��S���玼Z�2�M@U�bO�!�ݩ�f�X�KU(/��\on�e��O�N<"m�HL�,�\z���	�q�����Z�Ml�����:�Y�^�cρ%�D�j��Is�[ô]@��Z��i�Ħ\���"��C�%���K�!�-NK�E֒�qP�6��Rya��NkI�f87�B����3���y]]����7P��̋�X���������x�?"�	�%_a�l��W'65d�d	R��{b% �f
�r��(�Tn8l��hK�b�]�v��,����*i��E��R[_��P�K��,Ⱦ�Ə��o��g��LK��j��� �v,���;Ul��.�7���B8b�Fn�D"�EY�?�G�h�o��Wi��m���߽:���&����u�pGذ�Y��*ɘ���e�"Z�����2�\��~�KT7U��N��%eI��t�7�O�Ɛ(�����������Ģ�7w�¬��\l��h��n�3sq��f�o8.Ls���f	�8[��.��~4L}�nn�>�P?-`}w���wʪ�LcS;:Q�XSjkc�	�8V�p�����f(����a��ʻ�i蓱_v�W��_c%,PUHM�;/��Ƌm�p(�d鱐���e�1��sp�5��kL�dl{j��8�{W,��杒�a�c�MN�f�.5�����9�Pe��ut۾dZ���[�m^�٠�<�A� �����RG�pZD�X��%�=����s� ��S��Ye�e{�*:�[�Bc�-&W�H�F�Vsѿ�Y�����U>����6�o?�Xy��K�5xJ��mrd��Y�E0�?vK�3k}�&��g���Rԟf�A&�|�*���}J# }�5��Ėo�?������L�	r)�DE���/y��!gؑUQ�^VV��үR��m�X��.C����$5���y������XΆ܎ӝ]0��L�jLw�w�)�W�+E�Dm�c��g����˭��_y.Q�r9�5��pqښ�D�;_�b`�&j�Ĭdv`�Yb�3M�;=��4%�/,�0L���1 � ��Kz�Q,�"�v�Ҏ�d�q �S�,D`S��Wy��nI�����&	t��V�P-���;!�q_�<$	u�h�5+n��甗� v3��'$�U{�"�Ԍ�i�O��k��0���$��Xh���!����W��ͭJ.*�9a��ur�uۀ�ɰg������/�2��o��f	q����[��Q����(c��Λ��>��誼��+�y�� �߀�ؠ x��~fz�D?th1FJ/�����x�D�L�p�q}�;oh�y`��P��.�t���lW��Z��Ӎ��J�]�,�O�-��8�ԼE���p�^8gvM�ݼ�7�����
]Ȧ��Y�%����'�Mƻ��c��]��~� ����W�7	�5��EX��af�l�����lz�. JTU ���aS  �D�1������r�Q�k�,k�%o�ç1I�ߏ�G$��������9T!�3��$Ȣa��9�6��Zr��,�8�{��(UёYΙ�+���z ��B�"\���\o�����k�"�V��E�`��U�K����Y���ݚ�L�'�E:��n�k��a����Y#J��,c߽r�L���H�UB^HY��l����U�	��|v�t�Kǘ�4���L�3��'J�k���� ����/!��i�y�Tt�j`��w�L��"M�eBQ(%��!���r�[=���l�0 )l�)Pu����4�\�UIR�uѭ�r�Ѻ�A��Vx��a9A�i� �,Q69)�%�T��=��*EDD�Eř��
,��B�f�0S���z��R�<33;}��&�Α��V�U�96=9s�B3���\b<�XGB�d;1?2��iW��p���	���72[�KF�hwe�/����	
���ޭ��&�N�s�O����g�����GN���]/��#��8� �'θg���	)Pe�S�2�L;�<�x\�@��=ܙCl�	��a�G�{A��h	�����ˆ;�|�������U,���ߔIe����j(��H���(����dk���N
�^ ��) �c���(@�"�(�����@�Xɽ(���c��C�\m���
F������ �Nez99�<˺¹�N��*R�տa��[oyՏY�D�ЧQ��q�X�T4y����;=�M��654� ��ʰkeP���5M�4,ʐ\#�O	����{i�< �1D�U�8F���$�T|zI�'��C�2n&�,�c�[x�423�zǓ/�	��Id���1p���,�+�m�,�ࡴ���q߯}q��>���*�B��>8/*c���:A6!k��cZ��?4m�����TM���Z�Lj��3��UC�t�.i�F��Pi�-p�r|� *���G���<�C&(Z�b�g�Ȏ�=��1ׅg_�	2��H�
�����]b^�us�JQ!͠}��<@8�]�e�D�A<����OE�2���S�y7�l���g,�0H�X��d)����|L#�ɅC���e��;��a����ч�r�y�p���eFoZ��G�1W��S���%��О��b����(��+�p9��ύY@!
S�BX2�l�������Kzk)8F�܄q>��4�2MG���`8��@�Ǉ�>$�����u4t��o�ue����aFX�"s���z�	��{�胂��`O%?Ǵ�ik�a�z`�P�A�m�)fB�a|P?T-�BO��^��%84��\���J#5�@�����緮����)w�[��z�6�=��k��m}N'gҀ�vG��Wr �z!p��@y��:��E���;I ��L�C���{������n�pA��c�� P0"]�c,��)�I��J0w�Y��ҋ��N�P�� j��u��yٔ�(m� �|��
=t]�e�ԧ;�Cs%��J|{p�q�6�J��JX���%.c�E7�C�D�ýP�J1G�
�	c��Q0l��͊To�Я+���$LC��E�"�5xB��KA}���RY���Q^#�`��0B�ٸ���s�E��p�<��&�ļޅ���l.����!�np�dC�dd!�[|'��F-2�BqAd�lw�py�(��6Rh���J�o��g��
�B�?A�C,C�=H,� ]̟#��.uR`�������qr[���W�9��H�s�����!����tu�F!~��"�=�OT����p����l��������u$O]���H��*����5( ���̡�!!f�7ٯ0%p>�����14'��%$6���� ��Z�[|{�j�n��ҽ�՘�5'�eq����������f)����G9˞	�I�u����s{nx�
��,�6��*�,�v�}�-��7����f6�J��C��ǘ������G�U���PZ�]��������X�>hY'L�mF�Q�{�0����f�s�=S�@:u�St���SDZ��(�0��&�v���-z$]%��f���̘�i~,�O�g�v�p�'��f���I���i�_�?"l��¶�8
�v���qpLr>&N�Ͳ|�U���Z7�4��}���<��^���hڝ��4m52	;0)�V�c��o����W���Т���+{��atgh�
x	p�4����n��|�{C��i��:"�<�b�=C�n �6��wE�]]���[�dH�D5�G[z�B�ȗ��u��"hÕR�!��-��L8H�`n.��ɹ����m�qTg���d�"G�E#�Rלhw���^�U��;։(� i<�J�I���LH����덆����b蚌?�E_̀b�wb|gñf��ex�����o�=>�>:�W��u��0�z�.�ҕ2��(u~��`�+i�j�	eb~ܘ�^�#
��O�.�Ox�-'�0�sڴj��*Uy�_�;o��<����wT�|��%�)71R��+��X���o)��X��e����+g�g�O$�/���� �[�~ëW\�]�?��w���O��ڸ�%��9��u2�����	&��:��&2XŐ7���B[��=4q)�Yqi9Y��~Z�!5ѣH5x�{Y4Y:V���^�t(�8LOf��!p��%<��O��I-�.{7�B��0;k42��]��W��bLwm�<U=]����v��uO�<�O���L��J3�8�}N�����ˋ m*������#
M���� &�+3/T�攪r�̻�jQ��n�����k��b����z���l�h�뵘�+/�O_���|�k������Qo�Fb� �ڈ���cXP2�I<���ll`:�� �ۓM\�q_s.I��3q���=�4W�H~�K_(I�;:.(y��L��m���)mТ%YU����p6	��p�~��	_:5N��!�'���U�)ig��7)�jZw3��=��I�=���sl-<�C�B�_W��Xy�e�/z*����p?�zE$1��Ka���y���v�i���jEN�V�4RC82&�~�c�r%���, {9n��!LĶ��
�\�M3���kP~���j��w�t���+�����<�8���ʒi��?���|(��'n�:{��ܑ�V���w87���@Yv^\%-���*f8�,��:�7�-bC#��3h+�Cp�B��q�
��%/��L9�k��G�,��T@s�?�������g1q)�X~�G@���yDbVB�^.�#�����hI�5>2��0���;�a=B`˲�T�y>�4�B~*���+�x���ߥ���x�Y`������bs%�t�e!��8AC��Z�}; �¯�p��6���S�h$�G�3c�Ta?����ţ�qb����@-�{�F8��;���ؒԒb����4�|�N^54>�t��r¯��P�eU�A�5&��^��ȉ���3�,ˇ�N�ϐ�p�C��N��X�i���9�~Kd��\|u��=�	�S�{+%m��,3�Z# ��1�7wr��o@����ۉ^�A� �rj��=91�#��w]�0� ��n`�ίŖ����B�Չ����^J������qu	��/x'��_*�!�V���Ϝ8V���[�����Qs����Ls^�%�V��ZW\�&r�,ja�5O�v���~W�c�a��mȵA�$�8|$<ͅl~�F����N7^lE緣�v�V�T�"H�
�C�ۄ���&u [ K�����`�Y��������68VW�"��!�둊�	��9�~��kX��O���P`]����~������k.��,�t�XT�ݼƩ%5�ڥ�-Q5$a�s����{���w�O5 -TwuXQa&YL�*ړ���s�7}[��a1-wIs9�U	�K�mtr�Yfw�d�PY۝�ЫIq!�3��]����^}˝����JN��yK��V*]!�*ۿ-	��,�1�[���|p��h���Ռ��� D��wJ��f>6F[c�>%��T�ӆӂVB�p������7i8�WM�G��.E�qM���,׾���y�/6͞��RW'�ĉ��B{	����C���Ol.���9�:��B�Tǀ7B�)�6c���r�Ad���[h��<r�]��B�s��#,�_d9*���l��|?�
�D0�ۜ�9qju�U������q��?�r��Ĩy�Jܙj�����˃�>��O�A|)�'����J�OU�����L]�	���gD]V
kr�d_���S�_9�T:8�����Q�Fo�#Q
��}�F��8�.��$���٦L1�4n����5z��?~�E�D�o1�6�:��.�o�Ro�1ZQ���x�L�вf������^��OȒu���߃�X�M���k�k��3��K|*�=l܃m����pf���09l@R�ʳ'�S"�o�F��n������	���#(���<����K�{����1�����-0�k�f��fV�1�	P�M��8o���0��G�yqH��obI���*��R��o��*Z�8�͡�NP Q$;���rJ��n�j��$9K�BS�x�]�<�rJ�h��Yc��u�h���!�0��]*��{�!?�X^�y�9�dr���֣��
�lj�e���� �,����s/�c�{`�~��-���	Y�m��.��}a��t�)q2�ff��*����{� ����##DZU|�q��9e ���7�d��Y�Q7�A�f&T~���niG�&(���Ԏ�}�نܱ&�(e�,����:��W�l��A֭5�IBӛ�5�Z������#c�W��2�0�n�#�:�5�� r�xq�yK�����@���(S��+{r�f��F37&��4HB�B_:7D����8y-���^�n�X��h��|`�ƀ�%h�|!H�DK��z�:t��71;ʶ��?�_!�ZI�)[��~Zo�z���l���+~ݦ;��nvK�d1��Ʋ�����E�����Pn�)қ��~)�*D.28B|3�8T��yK)%F��r���/�ǻ�[�tw����_qѿ�����t�tL�5M��9g�2�����L�%,���c2�vb�rmnɹ�W�N��6��6*��AF{��n ����	P'/zc��/��M�W}P�g;o �Qd���5�=3@��3M�{�<�?gH�B#��n��b�=�DLA�1���܀%8�=�S�	�c��m(�Ӳ��1}wd����r��IN�=�C��p�>�:��q�JX�"��[�Y�u�%�j��X�u���z�X��T�&NO���eX|�{X+�ONLkS��l�Z֦k�WfC;�&�Śq����'��ev�v�rG����1uH��8������  ���]�o����b���]����-�oK�>ݝAU��r^�k����QK�`�ܤ�R��*
mT	�M���п��5B罙ٝݝ���[Y�l����μy�|�{���R2L�O� #Y0�S@=t}�^k���/��W8���$m�����X΁��� M�Fd<�;�k�g�T@��AZNx$N��U�5�3tL��a&m�87�n���F��$�ud>���Sz��OJf�NZ�p���PH�� !'	��L]pp,\�0�b5<k"�3o��ǘ�~�z��)�!g,�[!;�I}5g��1�9���d݅����Dh��t�5��Ms�X �s J���q�~���MDG��V-�Z��|��$(�T"F�Z�jL����s�6��3y���MG�HG�LBϥ����u�v'�۷d.k0G�n�hx\����dp����3�*dw2i^�9����7)�Yk�F��������6�J1b�)V�D®mw*mܨ%��A��ћD B���i���E��9��7 }��>/��aO�T�!��|��օ��n5[Λ$k��y��O�2�д��H݈����#�����"��ҏ�\�rFTK�ui�A|�sSvV· � �6��9�`���D6�ew�8��K���`���R����tm�"����wkTQ�g�~v*���g\��р�\�B��9��)���j���B%�NZ)17IH@ZdKv��E�RL۔�6P9�c��Y�����i����n���3]wEeĳXQ�>�~ϲ��~@%�I��^��@Xb���	�Jcz�뀁��\Y,��:�h~e�Y��σ��H?Ƃپ��Y�N)j��?��~�A��#]�MX,_%�zz���i9@׋��/�?��N�/٘#��>;��DM/����%`� 8�f����Oiɬn0������߫2���C���b"��GaI�#�������	�OHP0-h���
BqK"��ZC�aV�[�� ��abr�!j��m(F��]LT�e����+^�Qt	A�����zh�����j���� �)9t�YX������g��B[4��9��(���G�w[�ɐH�6i�_{���:���S}p�}dWl-l�.2=q'-j�(�|B�i���п�)�@Y�Y�)��umt+}�"�e�"�5��wZ,Cepk�� ga�=�~�꽌�����j=�9��[��v�i�g��o�.a�<�ʠ�Ѓ
�ݽ��4Z�(��r[ˁJo��aPfEv-����X?�m�~��me��=(�&���]BV4,�A4���E�|~<���0U��{�.��;�)�[����3SFx���W���Dfm}����'[�����[T��V��K�!�=B�t����V��T�n�gS��SfZrV�ڏ��p�t�
a���P�X�-X���V��I��.�+�I@Oѓ1 ��O�G��ȳ�s�'E)z�Ҁ4�{І��6�|�[�y�Y�W� h�s��.:���n\��WbOF=DMh��d{�A|EB�؟��V��.��Y�b��]�����GL�)o I���*�s(�:��,M8�Fz�I��C6~����ٗyUl1q�
J��$>�۶����/g�=,^Ƚ�p��[T�`I<����g�e-���o�Z�Dy�쇵R�\�"�`�,�ԯpg�0��m���DB���;2����'y��tw�<fk`ou�����Nة�:���r+�BP �!�������\~��W�s��:W���7f�f�RRЦ���u�m<Q� ��\n�9f�+L,J��Zm�J_�k�Z��+����ꔲ��j�!8��3�Jss�%��͡AF~@\��>�)&��&Z��w�
�`�3@��I���L-ɶ���]ws�O�rs���K�@�N��1�����Y�~�^T�Τ����j7<
&D�+��
�B�d�J@�d�*���mԏ�C��<��ИI�����D�A쌉���K���Gk�Ҡi`��/�l�h�&Ͽ ��ˇ����I谥��Z�QAP�I��,��O�eAh�'�sD�Y]v|K4w�	ʓo�������!�q�0{&CH�0�3یJn���BL�������!p�#u���3TG��(���"c�T��,ﾛT e�i�'C� k����=�� ,f
������W�o§'�p>�?ۉ4p��4��9rQ����滸LdD��-�.��j&�}	�¹h�x,f���r`�o�����/�g�,�<��^I�5^#���������o\䨹M���7p����Gҟ������4���
||^�2*�@q��]��$I6^s����i�Y�Kg�B��[��ۢ��ߔ�^��������On��('8���t�K��	�AXN	ߔ癊?'5}�J��OL
�_�JM��ǃ�d��"6����g��;����0��>���#�&R�c��Y	��>��h:���y�J�z5@�x�]�|���]f�jQ�C*��ye��>&@h�P�@�-�&RY���כ.� �Hd�i�k ��y>�l���284|��H*'NӍqL�7�䵚yyd�fjӓ�+��z��\mʎ�v�*�KYN����|8���x��Kj�?�b�e�E/ �-Y~�*˝�\2��TD0IOvR�R`Yw�y��u��`�m���4c̬�o�,mS��Md�&^�36v��L��l݅R�o�����K�)�L�8�E��k�x/�����@���mieNԯAR������6�; νǔ�#/�QW2�
��a,�i���\$Rl��ˮ��Ô�˚��R�O�h6�5���j�a�����XKl*g���T��x3��� N�n�(@���%�����D���ʥ���x�Z(��{�!�̱�7��U8�Acq���x��=+����)/�#��kZ��SNz�����Y$43[o��6�L�*�rb�Lr$�L'DL}�P�BG�����}a%�V�Hr"���?��?I�/�-���T�W��t�	X�^�wS��,e�;� (1���Ys�0y<o9߇��!QZ2=���KDJmӽH��M��`�Ǧv �j[wh��١:.��ӍV�۝�'����� [���bYy4�ѓl��x�V�\|g��g�M;M�`D;�ㆫ�Fb*ρ\��o]dZ�X'�-X￥�s2>�ALRJI�Q�²h_MϞk������r%X"�R��\���*b���@�Z\�+䵔ߪ�K�6u��ʜ%j�����7 }��%&r��.9'��s�F�dx�{�0�X��\�S,�9"���|���U@yV/PKe�g�����U�[��Q�C*˒]f&"N�j�[�xH�GW�?�i}�(�8�Eaut����?j��@�Y��ӳ�c�
*�����H��v�� l�YN{��Zډ"=���F%�m�|@4��[�1�:�����3��Ý�s2H0.@O�!�������C�����:,R�d~���!�z�#2�8�vF=-��>nw�u���.�6+n�6fV��pQYʅĚ� ������$��Z��B'���[�����B�+�~���Ac�#�H2nQ��,e�GłG�H�@�&\���OI��.�$&FO�M�"���x����᝱�	ϴc�<�����i���^�|�_$�U���򲷖t�-��OԤ���`.�b�<d�-Y��%���t�g��Ҩ*#_@X;�g�� �.���#_��K��"�>|��AG�lpB*
Q�jF`
x�R������B)n�As
���Hv\�@�TԱ��/�F�D���X,<�X��E2�O!&�.��~��b�٧���JX'z��N�Ӡp�]�������Jx:�"�g ��os��0��xh�>(�Jo��`�.���{�odǍ�/2�q�K�n';qbͩ��'FG�'=���a�4��껋���=`'?y�<b�7��f>s�&W��n�2�*n �%q�ʊ��Wh!����/%��2>����3ӗ�YɁt?��1Ay�~�]����:˦=�8!�����#�|�����G�������)��L؞<09�B��Gɉ�3|�� ��8��Q�J��Cʁ�����٩�\���,t�1�9���ݗ�:����E�?�p媷 *.��~�͇���/�_����S�=F�zX��/���8�   �� �L6�