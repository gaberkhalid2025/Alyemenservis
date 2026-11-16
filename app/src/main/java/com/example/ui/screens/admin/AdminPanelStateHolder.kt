package com.example.ui.screens.admin

import android.content.Context
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.example.data.*
import com.example.data.models.InstantRequestEntity
import com.example.data.models.RequestOfferEntity
import com.example.ui.*
import kotlinx.coroutines.CoroutineScope

@Stable
class AdminPanelState(
    val context: Context,
    val scope: CoroutineScope,
    val snackbarHostState: SnackbarHostState,
    private val pendingProvidersState: State<List<PendingProviderEntity>>,
    private val reportsState: State<List<ReportEntity>>,
    private val adminRoleState: State<String>,
    private val supervisorPermissionsState: State<List<String>>,
    private val categoriesState: State<List<CategoryEntity>>,
    private val activatedProvidersState: State<List<ProviderEntity>>,
    private val bookingsState: State<List<BookingEntity>>,
    private val notificationsState: State<List<NotificationEntity>>,
    private val chatChannelsState: State<List<ChatChannelEntity>>,
    private val settingsStateHolder: State<AdminSettingsEntity>,
    private val bannersListState: State<List<BannerEntity>>,
    private val supervisorsListState: State<List<SupervisorEntity>>,
    private val colorPalettesListState: State<List<ColorPaletteEntity>>,
    private val citiesListState: State<List<CityEntity>>,
    private val storesState: State<List<StoreEntity>>,
    private val propertiesState: State<List<PropertyEntity>>,
    private val jobsState: State<List<JobEntity>>,
    private val jobApplicationsState: State<List<JobApplicationEntity>>,
    private val registeredUsersListState: State<List<Map<String, Any>>>,
    private val instantRequestsState: State<List<InstantRequestEntity>>,
    private val requestOffersState: State<List<RequestOfferEntity>>,
    private val passwordRecoveryRequestsState: State<List<Map<String, Any>>>,
    private val callsLogState: State<List<CallEntity>>,
    private val couponsListState: State<List<CouponEntity>>,
    private val internalWalletsState: State<List<InternalWalletEntity>>,
    private val walletTransactionsState: State<List<WalletTransactionEntity>>,
    private val paymentWalletsState: State<List<PaymentWalletEntity>>,
    private val paymentsListState: State<List<PaymentEntity>>,
    private val deletedListState: State<List<ProviderEntity>>,
    private val customTabsListStateHolder: State<List<CustomProfileTabEntity>>,
    initialSettings: AdminSettingsEntity,
    initialAdminRole: String
) {
    val pendingProviders: List<PendingProviderEntity> get() = pendingProvidersState.value
    val reports: List<ReportEntity> get() = reportsState.value
    val adminRole: String get() = adminRoleState.value
    val supervisorPermissions: List<String> get() = supervisorPermissionsState.value
    val categories: List<CategoryEntity> get() = categoriesState.value
    val activatedProviders: List<ProviderEntity> get() = activatedProvidersState.value
    val bookings: List<BookingEntity> get() = bookingsState.value
    val notifications: List<NotificationEntity> get() = notificationsState.value
    val chatChannels: List<ChatChannelEntity> get() = chatChannelsState.value
    val settingsState: AdminSettingsEntity get() = settingsStateHolder.value
    val bannersList: List<BannerEntity> get() = bannersListState.value
    val supervisorsList: List<SupervisorEntity> get() = supervisorsListState.value
    val colorPalettesList: List<ColorPaletteEntity> get() = colorPalettesListState.value
    val citiesList: List<CityEntity> get() = citiesListState.value
    val stores: List<StoreEntity> get() = storesState.value
    val properties: List<PropertyEntity> get() = propertiesState.value
    val jobs: List<JobEntity> get() = jobsState.value
    val jobApplications: List<JobApplicationEntity> get() = jobApplicationsState.value
    val registeredUsersList: List<Map<String, Any>> get() = registeredUsersListState.value
    val instantRequests: List<InstantRequestEntity> get() = instantRequestsState.value
    val requestOffers: List<RequestOfferEntity> get() = requestOffersState.value
    val passwordRecoveryRequests: List<Map<String, Any>> get() = passwordRecoveryRequestsState.value
    val callsLog: List<CallEntity> get() = callsLogState.value
    val couponsList: List<CouponEntity> get() = couponsListState.value
    val internalWallets: List<InternalWalletEntity> get() = internalWalletsState.value
    val walletTransactions: List<WalletTransactionEntity> get() = walletTransactionsState.value
    val paymentWallets: List<PaymentWalletEntity> get() = paymentWalletsState.value
    val paymentsList: List<PaymentEntity> get() = paymentsListState.value
    val deletedList: List<ProviderEntity> get() = deletedListState.value
    val customTabsListState: List<CustomProfileTabEntity> get() = customTabsListStateHolder.value

    // Auth & Navigation
    var inputPasscode by mutableStateOf("")
    var isAuthorized by mutableStateOf(initialAdminRole != "GUEST")
    var activeSubTab by mutableStateOf(if (initialAdminRole == "OWNER") "BACKDOOR" else "REG_REQ")
    var selectedGroupFilter by mutableStateOf("ALL")

    // Section Sub-Tabs
    var adminReqSubTab by mutableStateOf("SERVICES")
    var adminBookingSubTab by mutableStateOf("SERVICES")
    var adminChatSubTab by mutableStateOf("SERVICES")
    var adminAddSubTab by mutableStateOf("SERVICES")
    var adminReviewSubTab by mutableStateOf("SERVICES")
    var adminNotifSubTab by mutableStateOf("SERVICES")
    var adminVipSubTab by mutableStateOf("SERVICES")
    var adminBannerSubTab by mutableStateOf("SERVICES")
    var adminPasswordSubTab by mutableStateOf("REQUESTS")

    // Category / City / Provider Rejection Dialogs
    var showDeleteCategoryConfirmId by mutableStateOf<String?>(null)
    var showEditCategoryObj by mutableStateOf<CategoryEntity?>(null)
    var showEditCityObj by mutableStateOf<CityEntity?>(null)
    var rejectingProviderRequest by mutableStateOf<PendingProviderEntity?>(null)
    var providerRejectionReasonText by mutableStateOf("")
    var editCatName by mutableStateOf("")
    var editCatIcon by mutableStateOf("")
    var newCatName by mutableStateOf("")
    var newCatIcon by mutableStateOf("")

    // Bookings & Supervisor Dialogs
    var showDeleteBookingConfirmId by mutableStateOf<String?>(null)
    var showRejectionReasonDialogId by mutableStateOf<String?>(null)
    var bookingRejectionReasonInput by mutableStateOf("")
    var editingBookingObj by mutableStateOf<BookingEntity?>(null)
    var redirectingBookingObj by mutableStateOf<BookingEntity?>(null)
    var editingSupervisorObj by mutableStateOf<SupervisorEntity?>(null)

    // Notifications Dialogs
    var showDeleteNotifConfirmId by mutableStateOf<String?>(null)
    var selectedRecoveryNotif by mutableStateOf<NotificationEntity?>(null)

    // Chat & Backup Dialogs / States
    var showActiveChatChannelObj by mutableStateOf<ChatChannelEntity?>(null)
    var adminChatReplyInput by mutableStateOf("")
    var showDeleteChatConfirmId by mutableStateOf<String?>(null)
    var backupJsonStringState by mutableStateOf("")
    var restoreJsonInputState by mutableStateOf("")

    // Targeted Notifications Form
    var notifTitleInput by mutableStateOf("")
    var notifMsgInput by mutableStateOf("")
    var notifTargetType by mutableStateOf("ALL")
    var notifTargetValue by mutableStateOf("")
    var notifDelayHours by mutableStateOf("")
    var notifValidityHours by mutableStateOf("")

    // Section 10 / Customization Inputs
    var editPrimaryHex by mutableStateOf(initialSettings.customPrimaryHex)
    var editSecondaryHex by mutableStateOf(initialSettings.customSecondaryHex)
    var editCardBgHex by mutableStateOf(initialSettings.cardBackgroundHex)
    var editProviderNameHex by mutableStateOf(initialSettings.providerNameColorHex)
    var editLocationHex by mutableStateOf(initialSettings.locationColorHex)
    var editRatingHex by mutableStateOf(initialSettings.ratingColorHex)
    var editVipBadgeHex by mutableStateOf(initialSettings.vipBadgeColorHex)
    var editVerifiedHex by mutableStateOf(initialSettings.verifiedBadgeColorHex)
    var editRecommendedHex by mutableStateOf(initialSettings.recommendedBadgeColorHex)
    var editFontSelected by mutableStateOf(initialSettings.activeFontFamily)

    var editChatIconSize by mutableStateOf(initialSettings.chatSize.toFloat())
    var editChatIconX by mutableStateOf(initialSettings.chatXOffset.toFloat())
    var editChatIconY by mutableStateOf(initialSettings.chatYOffset.toFloat())

    var editAssistantIconSize by mutableStateOf(initialSettings.assistantSize.toFloat())
    var editAssistantIconX by mutableStateOf(initialSettings.assistantXOffset.toFloat())
    var editAssistantIconY by mutableStateOf(initialSettings.assistantYOffset.toFloat())

    var requirementItemInput by mutableStateOf("")
    var isNewRequirementMandatory by mutableStateOf(true)
    var requirementsListState by mutableStateOf(
        initialSettings.registrationRequirements.split(",").filter { it.isNotBlank() }
    )

    var editCoverHeight by mutableStateOf(initialSettings.coverHeight.toFloat())
    var editAvatarSize by mutableStateOf(initialSettings.avatarSize.toFloat())
    var editElementSpacing by mutableStateOf(initialSettings.elementSpacing.toFloat())
    var editCardPadding by mutableStateOf(initialSettings.cardPadding.toFloat())

    var editShowVipBadge by mutableStateOf(initialSettings.showVipBadge)
    var editShowVerifiedBadge by mutableStateOf(initialSettings.showVerifiedBadge)
    var editShowRecommendedBadge by mutableStateOf(initialSettings.showRecommendedBadge)

    var editShowCallButton by mutableStateOf(initialSettings.showCallButton)
    var editShowWhatsappButton by mutableStateOf(initialSettings.showWhatsappButton)
    var editShowDetailsButton by mutableStateOf(initialSettings.showDetailsButton)
    var editShowBookButton by mutableStateOf(initialSettings.showBookButton)

    var categoryManagementMode by mutableStateOf("MAINTENANCE")
    var editCallButtonColorHex by mutableStateOf(initialSettings.callButtonColorHex)
    var editWhatsappButtonColorHex by mutableStateOf(initialSettings.whatsappButtonColorHex)
    var editDetailsButtonColorHex by mutableStateOf(initialSettings.detailsButtonColorHex)
    var editBookButtonColorHex by mutableStateOf(initialSettings.bookButtonColorHex)

    var editShowLoyaltyBanner by mutableStateOf(initialSettings.showLoyaltyBanner)
    var editMaxWorkPhotos by mutableStateOf(initialSettings.maxWorkPhotos.toFloat())

    // Wipe States
    var showWipeConfirmDialog by mutableStateOf(false)
    var wipeInputPassword by mutableStateOf("")
    var wipeProvidersChecked by mutableStateOf(true)
    var wipeBookingsChecked by mutableStateOf(true)
    var wipeChatsChecked by mutableStateOf(true)
    var wipeNotifsChecked by mutableStateOf(true)
    var wipeReportsChecked by mutableStateOf(true)
    var wipeCategoriesChecked by mutableStateOf(false)
    var wipePendingChecked by mutableStateOf(true)
    var wipeBannersChecked by mutableStateOf(true)
    var wipeSupervisorsChecked by mutableStateOf(false)
    var wipeCitiesChecked by mutableStateOf(false)
    var wipeThemesChecked by mutableStateOf(false)

    // Manual Creation States
    var manualName by mutableStateOf("")
    var manualPhone by mutableStateOf("")
    var manualCategoryId by mutableStateOf("")
    var manualStreet by mutableStateOf("")
    var manualCityId by mutableStateOf("")
    var manualPhotoUrl by mutableStateOf("")
    var manualIdCardUrl by mutableStateOf("")
    var manualForensicUrl by mutableStateOf("")
    var manualPriceValue by mutableStateOf("1500")
    var manualIsVipGolden by mutableStateOf(false)

    // Cities Form States
    var newCityArName by mutableStateOf("")
    var newCityEnName by mutableStateOf("")
    var newCityIcon by mutableStateOf("📍")

    // Search & Metadata States
    var complaintsSearchQuery by mutableStateOf("")
    var registeredUsersSearchQuery by mutableStateOf("")
    var activeProvidersSearchQuery by mutableStateOf("")
    var activeJobsSearchQuery by mutableStateOf("")
    var storesSearchQuery by mutableStateOf("")
    var restaurantsSearchQuery by mutableStateOf("")
    var medicalSearchQuery by mutableStateOf("")
    var propertiesSearchQuery by mutableStateOf("")
    var applicantsSearchQuery by mutableStateOf("")
    var showEditProviderMetadataObj by mutableStateOf<ProviderEntity?>(null)
    var editProviderPhone by mutableStateOf("")
    var editProviderCategoryId by mutableStateOf("")

    // Supervisors Form States
    var supervisorInputName by mutableStateOf("")
    var supervisorInputRole by mutableStateOf("SUPPORT")
    var supervisorInputPasscode by mutableStateOf("")
    var supervisorInputPermissions by mutableStateOf<List<String>>(emptyList())

    // Density & Merge/Export Dialogs
    var elementSpacingPadding by mutableStateOf(12f)
    var containerCardPadding by mutableStateOf(14f)
    var showExportReportPasswordDialog by mutableStateOf(false)
    var exportReportPasswordInput by mutableStateOf("")
    var showMergeCategoryObj by mutableStateOf<CategoryEntity?>(null)
    var selectedTargetCategoryIdForMerge by mutableStateOf("")

    // Coupons Form States
    var couponCodeInput by mutableStateOf("")
    var couponPointsInput by mutableStateOf("100")
    var couponExpiryDaysInput by mutableStateOf("30")
    var couponDiscountInput by mutableStateOf("10")
    var couponMaxUsageInput by mutableStateOf("100")

    // Payments & Voice Calls Settings States
    var isPaymentEnabledInput by mutableStateOf(initialSettings.isPaymentEnabled)
    var isBookingPaymentRequiredInput by mutableStateOf(initialSettings.isBookingPaymentRequired)
    var requireAdvancePaymentInput by mutableStateOf(initialSettings.requireAdvancePayment)
    var advancePaymentPercentInput by mutableStateOf(initialSettings.advancePaymentPercent.toString())
    var minAdvanceAmountInput by mutableStateOf(initialSettings.minAdvanceAmount.toString())
    var maxAdvanceAmountInput by mutableStateOf(initialSettings.maxAdvanceAmount.toString())
    var isCommissionEnabledInput by mutableStateOf(initialSettings.isCommissionEnabled)
    var paymentCommissionRateInput by mutableStateOf(initialSettings.paymentCommissionRate.toString())

    var linkedCategoriesForInstantBookingInput by mutableStateOf(initialSettings.linkedCategoriesForInstantBooking)
    var linkedProvidersForDepositInput by mutableStateOf(initialSettings.linkedProvidersForDeposit)
    var exemptUsersFromDepositInput by mutableStateOf(initialSettings.exemptUsersFromDeposit)
    var showWalletInProfileInput by mutableStateOf(initialSettings.showWalletInProfile)
    var voiceCallsEnabledInput by mutableStateOf(initialSettings.voiceCallsEnabled)
    var voiceCallsAllowedCategoriesInput by mutableStateOf(initialSettings.voiceCallsAllowedCategories)
    var voiceCallsAllowedProvidersInput by mutableStateOf(initialSettings.voiceCallsAllowedProviders)
    var voiceCallsAllowedUsersInput by mutableStateOf(initialSettings.voiceCallsAllowedUsers)
    var disableVoiceCallsInput by mutableStateOf(initialSettings.disableVoiceCalls)
    var hideTopHeaderBarInput by mutableStateOf(initialSettings.hideTopHeaderBar)
    var customAppNameInput by mutableStateOf(initialSettings.customAppName)

    // Wallets & Payments Dialogs
    var showWalletTxDialog by mutableStateOf(false)
    var selectedWalletForTx by mutableStateOf<InternalWalletEntity?>(null)
    var txTypeInput by mutableStateOf("DEPOSIT")
    var txAmountInput by mutableStateOf("1000")
    var txNoteInput by mutableStateOf("شحن رصيد بواسطة الإدارة")

    var editingWalletObj by mutableStateOf<PaymentWalletEntity?>(null)
    var walletProviderInput by mutableStateOf("jeeb")
    var walletNumberInput by mutableStateOf("")
    var walletAccountNameInput by mutableStateOf("")
    var walletAccountNameArInput by mutableStateOf("")
    var walletDescriptionInput by mutableStateOf("")
    var walletInstructionsInput by mutableStateOf("")
    var walletTypeInput by mutableStateOf("BOTH")
    var walletCurrencyInput by mutableStateOf("YER")
    var walletIsVisibleInput by mutableStateOf(true)
    var walletIsDefaultInput by mutableStateOf(false)
    var walletDisplayOrderInput by mutableStateOf("0")
    var walletMinTransferInput by mutableStateOf("100")
    var walletMaxTransferInput by mutableStateOf("1000000")
    var walletStatusInput by mutableStateOf("active")
    var showAddWalletDialog by mutableStateOf(false)

    var verifyingPaymentObj by mutableStateOf<PaymentEntity?>(null)
    var adminVerifyPaymentNote by mutableStateOf("")
    var rejectingPaymentObj by mutableStateOf<PaymentEntity?>(null)
    var adminRejectPaymentNote by mutableStateOf("")
    var refundingPaymentObj by mutableStateOf<PaymentEntity?>(null)
    var refundReasonInput by mutableStateOf("")
}

@Composable
fun rememberAdminPanelState(viewModel: MainViewModel): AdminPanelState {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val pendingProvidersState = viewModel.pendingProviders.collectAsState()
    val reportsState = viewModel.reports.collectAsState()
    val adminRoleState = viewModel.adminRole.collectAsState()
    val supervisorPermissionsState = viewModel.supervisorPermissions.collectAsState()
    val categoriesState = viewModel.categories.collectAsState()
    val activatedProvidersState = viewModel.providers.collectAsState()
    val bookingsState = viewModel.bookings.collectAsState()
    val notificationsState = viewModel.notifications.collectAsState()
    val chatChannelsState = viewModel.chatChannels.collectAsState()
    val settingsStateHolder = viewModel.settings.collectAsState()
    val bannersListState = viewModel.banners.collectAsState()
    val supervisorsListState = viewModel.supervisors.collectAsState()
    val colorPalettesListState = viewModel.colorPalettes.collectAsState()
    val citiesListState = viewModel.cities.collectAsState()
    val storesState = viewModel.stores.collectAsState()
    val propertiesState = viewModel.properties.collectAsState()
    val jobsState = viewModel.jobs.collectAsState()
    val jobApplicationsState = viewModel.jobApplications.collectAsState()
    val registeredUsersListState = viewModel.registeredUsersList.collectAsState()
    val instantRequestsState = viewModel.instantRequests.collectAsState()
    val requestOffersState = viewModel.requestOffers.collectAsState()
    val passwordRecoveryRequestsState = viewModel.passwordRecoveryRequests.collectAsState()
    val callsLogState = viewModel.callsLog.collectAsState()
    val couponsListState = viewModel.coupons.collectAsState()
    val internalWalletsState = viewModel.internalWallets.collectAsState()
    val walletTransactionsState = viewModel.walletTransactions.collectAsState()
    val paymentWalletsState = viewModel.paymentWallets.collectAsState()
    val paymentsListState = viewModel.payments.collectAsState()
    val deletedListState = viewModel.deletedProviders.collectAsState()
    val customTabsListStateHolder = viewModel.customProfileTabs.collectAsState()

    val state = remember(viewModel) {
        AdminPanelState(
            context = context,
            scope = scope,
            snackbarHostState = snackbarHostState,
            pendingProvidersState = pendingProvidersState,
            reportsState = reportsState,
            adminRoleState = adminRoleState,
            supervisorPermissionsState = supervisorPermissionsState,
            categoriesState = categoriesState,
            activatedProvidersState = activatedProvidersState,
            bookingsState = bookingsState,
            notificationsState = notificationsState,
            chatChannelsState = chatChannelsState,
            settingsStateHolder = settingsStateHolder,
            bannersListState = bannersListState,
            supervisorsListState = supervisorsListState,
            colorPalettesListState = colorPalettesListState,
            citiesListState = citiesListState,
            storesState = storesState,
            propertiesState = propertiesState,
            jobsState = jobsState,
            jobApplicationsState = jobApplicationsState,
            registeredUsersListState = registeredUsersListState,
            instantRequestsState = instantRequestsState,
            requestOffersState = requestOffersState,
            passwordRecoveryRequestsState = passwordRecoveryRequestsState,
            callsLogState = callsLogState,
            couponsListState = couponsListState,
            internalWalletsState = internalWalletsState,
            walletTransactionsState = walletTransactionsState,
            paymentWalletsState = paymentWalletsState,
            paymentsListState = paymentsListState,
            deletedListState = deletedListState,
            customTabsListStateHolder = customTabsListStateHolder,
            initialSettings = settingsStateHolder.value,
            initialAdminRole = adminRoleState.value
        )
    }

    val currentRole = adminRoleState.value
    val currentSettings = settingsStateHolder.value

    LaunchedEffect(currentRole) {
        state.isAuthorized = currentRole != "GUEST"
        if (currentRole == "OWNER") {
            state.activeSubTab = "BACKDOOR"
        } else if (state.activeSubTab == "BACKDOOR") {
            state.activeSubTab = "REG_REQ"
        }
    }

    LaunchedEffect(currentSettings.chatSize) { state.editChatIconSize = currentSettings.chatSize.toFloat() }
    LaunchedEffect(currentSettings.chatXOffset) { state.editChatIconX = currentSettings.chatXOffset.toFloat() }
    LaunchedEffect(currentSettings.chatYOffset) { state.editChatIconY = currentSettings.chatYOffset.toFloat() }
    LaunchedEffect(currentSettings.assistantSize) { state.editAssistantIconSize = currentSettings.assistantSize.toFloat() }
    LaunchedEffect(currentSettings.assistantXOffset) { state.editAssistantIconX = currentSettings.assistantXOffset.toFloat() }
    LaunchedEffect(currentSettings.assistantYOffset) { state.editAssistantIconY = currentSettings.assistantYOffset.toFloat() }
    LaunchedEffect(currentSettings.coverHeight) { state.editCoverHeight = currentSettings.coverHeight.toFloat() }
    LaunchedEffect(currentSettings.avatarSize) { state.editAvatarSize = currentSettings.avatarSize.toFloat() }
    LaunchedEffect(currentSettings.elementSpacing) { state.editElementSpacing = currentSettings.elementSpacing.toFloat() }
    LaunchedEffect(currentSettings.cardPadding) { state.editCardPadding = currentSettings.cardPadding.toFloat() }
    LaunchedEffect(currentSettings.showVipBadge) { state.editShowVipBadge = currentSettings.showVipBadge }
    LaunchedEffect(currentSettings.showVerifiedBadge) { state.editShowVerifiedBadge = currentSettings.showVerifiedBadge }
    LaunchedEffect(currentSettings.showRecommendedBadge) { state.editShowRecommendedBadge = currentSettings.showRecommendedBadge }
    LaunchedEffect(currentSettings.showCallButton) { state.editShowCallButton = currentSettings.showCallButton }
    LaunchedEffect(currentSettings.showWhatsappButton) { state.editShowWhatsappButton = currentSettings.showWhatsappButton }
    LaunchedEffect(currentSettings.showDetailsButton) { state.editShowDetailsButton = currentSettings.showDetailsButton }
    LaunchedEffect(currentSettings.showBookButton) { state.editShowBookButton = currentSettings.showBookButton }
    LaunchedEffect(currentSettings.callButtonColorHex) { state.editCallButtonColorHex = currentSettings.callButtonColorHex }
    LaunchedEffect(currentSettings.whatsappButtonColorHex) { state.editWhatsappButtonColorHex = currentSettings.whatsappButtonColorHex }
    LaunchedEffect(currentSettings.detailsButtonColorHex) { state.editDetailsButtonColorHex = currentSettings.detailsButtonColorHex }
    LaunchedEffect(currentSettings.bookButtonColorHex) { state.editBookButtonColorHex = currentSettings.bookButtonColorHex }
    LaunchedEffect(currentSettings.showLoyaltyBanner) { state.editShowLoyaltyBanner = currentSettings.showLoyaltyBanner }
    LaunchedEffect(currentSettings.maxWorkPhotos) { state.editMaxWorkPhotos = currentSettings.maxWorkPhotos.toFloat() }

    LaunchedEffect(currentSettings.isPaymentEnabled) { state.isPaymentEnabledInput = currentSettings.isPaymentEnabled }
    LaunchedEffect(currentSettings.isBookingPaymentRequired) { state.isBookingPaymentRequiredInput = currentSettings.isBookingPaymentRequired }
    LaunchedEffect(currentSettings.requireAdvancePayment) { state.requireAdvancePaymentInput = currentSettings.requireAdvancePayment }
    LaunchedEffect(currentSettings.advancePaymentPercent) { state.advancePaymentPercentInput = currentSettings.advancePaymentPercent.toString() }
    LaunchedEffect(currentSettings.minAdvanceAmount) { state.minAdvanceAmountInput = currentSettings.minAdvanceAmount.toString() }
    LaunchedEffect(currentSettings.maxAdvanceAmount) { state.maxAdvanceAmountInput = currentSettings.maxAdvanceAmount.toString() }
    LaunchedEffect(currentSettings.isCommissionEnabled) { state.isCommissionEnabledInput = currentSettings.isCommissionEnabled }
    LaunchedEffect(currentSettings.paymentCommissionRate) { state.paymentCommissionRateInput = currentSettings.paymentCommissionRate.toString() }
    LaunchedEffect(currentSettings.linkedCategoriesForInstantBooking) { state.linkedCategoriesForInstantBookingInput = currentSettings.linkedCategoriesForInstantBooking }
    LaunchedEffect(currentSettings.linkedProvidersForDeposit) { state.linkedProvidersForDepositInput = currentSettings.linkedProvidersForDeposit }
    LaunchedEffect(currentSettings.exemptUsersFromDeposit) { state.exemptUsersFromDepositInput = currentSettings.exemptUsersFromDeposit }
    LaunchedEffect(currentSettings.showWalletInProfile) { state.showWalletInProfileInput = currentSettings.showWalletInProfile }
    LaunchedEffect(currentSettings.voiceCallsEnabled) { state.voiceCallsEnabledInput = currentSettings.voiceCallsEnabled }
    LaunchedEffect(currentSettings.voiceCallsAllowedCategories) { state.voiceCallsAllowedCategoriesInput = currentSettings.voiceCallsAllowedCategories }
    LaunchedEffect(currentSettings.voiceCallsAllowedProviders) { state.voiceCallsAllowedProvidersInput = currentSettings.voiceCallsAllowedProviders }
    LaunchedEffect(currentSettings.voiceCallsAllowedUsers) { state.voiceCallsAllowedUsersInput = currentSettings.voiceCallsAllowedUsers }
    LaunchedEffect(currentSettings.disableVoiceCalls) { state.disableVoiceCallsInput = currentSettings.disableVoiceCalls }
    LaunchedEffect(currentSettings.hideTopHeaderBar) { state.hideTopHeaderBarInput = currentSettings.hideTopHeaderBar }
    LaunchedEffect(currentSettings.customAppName) { state.customAppNameInput = currentSettings.customAppName }

    return state
}
