import re
import os

# Create a temporary file or read original from main source if backup exists.
# Wait, since MainViewModel.kt is currently our modified one, let's see if we can find the core functions implementations.
# Yes, they are still in MainViewModel.kt!
# So we can read MainViewModel.kt and extract the core functions implementations from it!

with open('app/src/main/java/com/example/ui/MainViewModel.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# Core functions to keep (implementations)
core_funcs = [
    "updateUserLocation", "startLocationUpdates", "refreshData", "updateOnlineStatus", "retryConnection",
    "updateUserFcmToken", "initializeFirestoreCollections", "initializeUserIdentity", "setupRealtimeFirestoreListeners",
    "seedFirestoreIfEmpty", "writeDefaultSupervisors", "writeDefaultColorPalettes", "writeDefaultCategories",
    "writeDefaultCities", "writeDefaultBanners", "writeDefaultProviders", "getDefaultStoresList",
    "getDefaultPropertiesList", "writeDefaultStores", "writeDefaultProperties", "writeDefaultJobs",
    "writeDefaultProducts", "applyFilters", "selectCategory", "updateSearchQuery", "toggleVipFilter",
    "toggleAvailableFilter", "setCityFilter", "setNeighborhoodFilter", "setPhoneOrNameFilter", "setRadiusKm",
    "registerBackdoorInteraction", "changeAdminCredentials", "authenticateAdmin", "logout", "navigateTo",
    "goBack", "switchLanguage", "toggleLanguage", "setLanguage", "triggerNotification", "triggerOpenChatForRequest",
    "clearNotification", "loadUserPoints", "redeemLoyaltyPoints", "rewardSharePoints",
    "clearSmartAssistantChatHistory", "uploadImageStringOrUri", "submitJoinForm", "cancelOrResetJoinRequest",
    "setJoinRequestPhone", "addNotification"
]

# Extracted functions
extracted_core = {}

def extract_function(file_content, func_name):
    pattern = r'(?:override\s+|private\s+|internal\s+|suspend\s+)*fun\s+' + func_name + r'\b'
    matches = list(re.finditer(pattern, file_content))
    if not matches:
        return []
    results = []
    for match in matches:
        start_pos = match.start()
        # Find parenthesis boundaries
        paren_start = file_content.find('(', start_pos)
        if paren_start == -1:
            continue
            
        depth = 0
        paren_end = -1
        for pos in range(paren_start, len(file_content)):
            char = file_content[pos]
            if char == '(':
                depth += 1
            elif char == ')':
                depth -= 1
                if depth == 0:
                    paren_end = pos
                    break
                    
        if paren_end == -1:
            continue
            
        brace_match = re.search(r'\{', file_content[paren_end:])
        if not brace_match:
            eq_match = re.search(r'=', file_content[paren_end:])
            if eq_match:
                line_end = file_content.find('\n', paren_end + eq_match.start())
                results.append(file_content[start_pos:line_end])
            continue
        
        brace_start = paren_end + brace_match.start()
        count = 1
        pos = brace_start + 1
        while count > 0 and pos < len(file_content):
            char = file_content[pos]
            if char == '{':
                count += 1
            elif char == '}':
                count -= 1
            pos += 1
        results.append(file_content[start_pos:pos])
    return results

for f in core_funcs:
    funcs = extract_function(content, f)
    if funcs:
        extracted_core[f] = funcs

# Other ViewModels list of functions to delegate
admin_funcs = [
    "approveRequest", "rejectRequest", "approveTechnician", "rejectTechnician", "loadPendingTechnicians",
    "approvePendingProvider", "rejectPendingProvider", "approveRegisteredUser", "toggleBlockRegisteredUser",
    "deleteRegisteredUser", "saveStore", "deleteStore", "restoreStore", "deleteStorePermanently",
    "setStoreActive", "setStorePinned", "setStoreVip", "setStoreVerified", "setStoreRecommended",
    "setStoreBlocked", "setStoreChatDisabled", "setStoreNotificationsDisabled", "setStorePaymentEnabled",
    "toggleStoreBlocked", "toggleStoreActive", "toggleStorePinned", "toggleStoreChatDisabled", "approveStorePdf",
    "saveProperty", "deleteProperty", "restoreProperty", "deletePropertyPermanently", "setPropertyActive",
    "setPropertyPinned", "setPropertyVip", "setPropertyVerified", "setPropertyRecommended", "setPropertyBlocked",
    "setPropertyChatDisabled", "setPropertyNotificationsDisabled", "setPropertyPaymentEnabled", "togglePropertyBlocked",
    "approvePropertyPdf", "saveJob", "deleteJob", "restoreJob", "deleteJobPermanently", "setJobApproved",
    "setJobBlocked", "setJobPinned", "setJobVip", "setJobChatDisabled", "submitJobApplication",
    "updateJobApplicationStatus", "acceptJobApplication", "rejectJobApplication", "deleteJobApplication",
    "submitReport", "deleteReport", "sendReport", "addCoupon", "saveCoupon", "deleteCoupon",
    "saveInternalWallet", "performWalletTransaction", "addPaymentWallet", "updatePaymentWallet",
    "deletePaymentWallet", "togglePaymentWalletVisibility", "createPayment", "confirmPayment", "verifyPayment",
    "refundPayment", "saveProduct", "deleteProduct", "updateProductPrice", "saveOffer", "deleteOffer",
    "toggleOfferStatus", "listenToOffersForEntity", "listenToProductsForStore", "saveCustomProfileTab",
    "deleteCustomProfileTab", "toggleCustomProfileTab", "addNewCategory", "editCategory", "deleteCategory",
    "togglePinCategory", "mergeCategories", "saveCategoryEntity", "addSubCategory", "convertCategoryType",
    "reorderCategories", "addNewCity", "updateCity", "removeCity", "removeProvider", "removeProviderPermanently",
    "restoreProvider", "pinProvider", "recommendProvider", "verifyProviderBadge", "toggleProviderSubscription",
    "setProviderChatDisabled", "setProviderNotificationsDisabled", "setProviderPaymentRequired",
    "extendProviderSubscription", "toggleProviderBlock", "toggleProviderStatus", "toggleProviderPin",
    "toggleProviderVerification", "toggleProviderRecommendation", "updateProviderEntity",
    "editProviderPhoneAndCategory", "addNewProvider", "addNewProviderCustom", "addNewBanner", "addBanner",
    "deleteBanner", "reorderBanners", "placeOrder", "updateOrderStatus", "deleteOrder", "deleteAllOrders",
    "addRating", "addRatingReply", "deleteRating", "approveRating", "submitRating", "recalculateTargetRating",
    "logAdminActivity", "logCall", "checkAndGetDuplicateAccountType", "updateProviderPortfolio",
    "addPortfolioImage", "removePortfolioImage", "clearPortfolio", "redirectBookingToEntity", "unbanEntity",
    "restoreEntity", "hardDeleteEntity", "sendNotificationToApplicants", "exportJobApplicantsCsv"
]

settings_funcs = [
    "loadCardSettings", "updateCardSettings", "updateTheme", "saveCustomSettingsState", "updateBackdoorSettings",
    "updateAdminSettings", "initColorSync", "updateCloudColorScheme", "updatePersonalColors", "triggerManualSync",
    "resolveConflict", "getCurrentTimestampString", "addNewSyncLog", "toggleChatParticipant", "isChatBlockedFor",
    "canParticipateInChat", "startVoiceCall", "endVoiceCall", "exportComplaintsToCSV", "exportComplaintsToPDF",
    "exportPerformanceReportToPDF", "createSystemBackup", "restoreSystemFromBackup", "exportSelectedCollectionsAsJson",
    "saveBackupToLocalStorage", "setSecondaryFirebaseConfig", "saveCustomPermissionsMatrixToFirestore",
    "addColorPalette", "updateColorPalette", "deleteColorPalette", "resetAccountPassword", "requestAdminPasswordReset",
    "requestPasswordReset", "approvePasswordReset", "adminResetAccountPassword", "requestPasswordRecoveryForStore",
    "requestPasswordRecoveryForProperty", "requestPasswordRecoveryGeneral", "wipeAllDatabaseData",
    "wipeSelectedDatabaseData", "wipeAllMockAndTemporaryData", "autoCleanupData", "scheduleAutoCleanup"
]

instant_funcs = [
    "createInstantRequest", "submitOfferForRequest", "acceptRequestOffer", "completeInstantRequest", "cancelInstantRequest"
]

auth_delegates = [
    "setPasswordRecoveryWaitingPhone", "registerGuestUser", "resetRegistrationState", "searchAccountForRestore",
    "restoreUserAccountByPhoneAndPassword", "restoreGuestUser", "setUserSessionDetails", "loginUserDirectly",
    "getAuthEmailForPhone", "showBackdoorDialog", "dismissBackdoorDialog", "setSupervisorSession",
    "hasAdminPermission", "addSupervisor", "editSupervisor", "updateSupervisorPermissions", "removeSupervisor"
]

chat_delegates = [
    "listenToUserSupportChat", "sendMessageInChat", "markChannelMessagesAsRead", "markMessageAsRead",
    "getOrCreateChatChannel", "clearGeneralChatHistory", "deleteAllChats", "deleteChatChannel",
    "deleteChatMessage", "broadcastAdminWarning", "replyToChatChannel", "openOrCreateChatChannel",
    "sendChatMessageAdvanced", "markChatMessagesAsRead", "toggleBlockChatChannel", "blockChatChannel",
    "wipeOldChatChannels"
]

booking_delegates = [
    "createBooking", "updateBookingFormFields", "updateDistributionMode", "updateBookingStatus",
    "cancelBookingByUser", "attemptCancelBooking", "cancelBookingByTechnician", "cancelBookingByAdmin",
    "getBookingStatusColor", "getBookingStatusLabel", "getBookingProgress"
]

notification_delegates = [
    "markNotificationAsRead", "loadReadNotifications", "markAllNotificationsAsRead", "deleteNotification",
    "deleteAllNotifications", "clearAllNotifications"
]

def get_signature_and_delegate(file_content, func_name, target_vm):
    pattern = r'(?:override\s+|private\s+|internal\s+|suspend\s+)*fun\s+' + func_name + r'\b'
    match = re.search(pattern, file_content)
    if not match:
        return None
    start_pos = match.start()
    
    # Find parenthesis boundaries
    paren_start = file_content.find('(', start_pos)
    if paren_start == -1:
        return None
        
    depth = 0
    paren_end = -1
    for pos in range(paren_start, len(file_content)):
        char = file_content[pos]
        if char == '(':
            depth += 1
        elif char == ')':
            depth -= 1
            if depth == 0:
                paren_end = pos
                break
                
    if paren_end == -1:
        return None
        
    brace_match = re.search(r'\{', file_content[paren_end:])
    eq_match = re.search(r'=', file_content[paren_end:])
    
    end_sig_pos = len(file_content)
    if brace_match and eq_match:
        end_sig_pos = paren_end + min(brace_match.start(), eq_match.start())
    elif brace_match:
        end_sig_pos = paren_end + brace_match.start()
    elif eq_match:
        end_sig_pos = paren_end + eq_match.start()
        
    sig = file_content[start_pos:end_sig_pos].strip()
    
    # Extract params
    paren_match = re.search(r'\((.*)\)', sig, re.DOTALL)
    args = []
    if paren_match:
        params_str = paren_match.group(1)
        # Strip all comments
        params_str = re.sub(r'//.*', '', params_str)
        params_str = re.sub(r'/\*.*?\*/', '', params_str, flags=re.DOTALL)
        
        # Split params by comma, ignoring nested templates/brackets
        depth = 0
        current_arg = ""
        for char in params_str:
            if char == '<': depth += 1
            elif char == '>': depth -= 1
            elif char == ',' and depth == 0:
                parts = current_arg.split(':')
                if parts:
                    name_part = parts[0].strip().split()
                    if name_part:
                        args.append(name_part[-1])
                current_arg = ""
                continue
            current_arg += char
        if current_arg:
            parts = current_arg.split(':')
            if parts:
                name_part = parts[0].strip().split()
                if name_part:
                    args.append(name_part[-1])
                    
    args_str = ", ".join(args)
    return f"    {sig} = {target_vm}.{func_name}({args_str})"

delegations = []

# Admin
for f in admin_funcs:
    del_func = get_signature_and_delegate(content, f, "adminViewModel")
    if del_func:
        delegations.append(del_func)

# Settings
for f in settings_funcs:
    del_func = get_signature_and_delegate(content, f, "settingsViewModel")
    if del_func:
        delegations.append(del_func)

# Instant request
for f in instant_funcs:
    del_func = get_signature_and_delegate(content, f, "instantRequestViewModel")
    if del_func:
        delegations.append(del_func)

# Auth
for f in auth_delegates:
    del_func = get_signature_and_delegate(content, f, "authViewModel")
    if del_func:
        delegations.append(del_func)

# Chat
for f in chat_delegates:
    del_func = get_signature_and_delegate(content, f, "chatViewModel")
    if del_func:
        delegations.append(del_func)

# Booking
for f in booking_delegates:
    del_func = get_signature_and_delegate(content, f, "bookingViewModel")
    if del_func:
        delegations.append(del_func)

# Notification
for f in notification_delegates:
    del_func = get_signature_and_delegate(content, f, "notificationViewModel")
    if del_func:
        delegations.append(del_func)

# Build MainViewModel.kt
new_main = [
    "package com.example.ui",
    "",
    "import androidx.compose.runtime.getValue",
    "import androidx.compose.runtime.setValue",
    "import androidx.compose.runtime.mutableStateOf",
    "import com.example.utils.*",
    "import com.example.ui.viewmodels.BaseViewModel",
    "import com.example.ui.viewmodels.BookingDistributionMode",
    "import com.example.ui.viewmodels.BookingFormFields",
    "import com.example.ui.viewmodels.BookingStatus",
    "import androidx.lifecycle.ViewModel",
    "import androidx.lifecycle.viewModelScope",
    "import com.example.data.*",
    "import com.example.data.models.*",
    "import kotlinx.coroutines.flow.*",
    "import kotlinx.coroutines.launch",
    "import java.util.UUID",
    "",
    "class MainViewModel : BaseViewModel() {",
    "",
    "    override val db by lazy {",
    "        val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()",
    "        try {",
    "            val settings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()",
    "                .setPersistenceEnabled(true)",
    "                .setCacheSizeBytes(104857600L)",
    "                .build()",
    "            firestore.firestoreSettings = settings",
    "        } catch (e: Exception) {",
    "            e.printStackTrace()",
    "        }",
    "        firestore",
    "    }",
    "",
    "    override val firestoreListeners = mutableListOf<com.google.firebase.firestore.ListenerRegistration>()",
    "",
    "    override fun onCleared() {",
    "        super.onCleared()",
    "        try {",
    "            firestoreListeners.forEach { it.remove() }",
    "            firestoreListeners.clear()",
    "        } catch (e: Exception) {",
    "            e.printStackTrace()",
    "        }",
    "    }",
    "",
    "    private val auth = com.google.firebase.auth.FirebaseAuth.getInstance()",
    "",
    "    // ------------------- Local StateFlows -------------------",
    "    private val _currentLanguage = MutableStateFlow(\"ar\")",
    "    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()",
    "",
    "    private val _favoriteIds = MutableStateFlow<Set<String>>(emptySet())",
    "    val favoriteIds: StateFlow<Set<String>> = _favoriteIds.asStateFlow()",
    "",
    "    internal val _notifications = MutableStateFlow<List<NotificationEntity>>(emptyList())",
    "    val notifications: StateFlow<List<NotificationEntity>> = _notifications.asStateFlow()",
    "",
    "    internal val _readNotificationIds = MutableStateFlow<Set<String>>(emptySet())",
    "    val readNotificationIds: StateFlow<Set<String>> = _readNotificationIds.asStateFlow()",
    "",
    "    internal val _supervisors = MutableStateFlow<List<SupervisorEntity>>(emptyList())",
    "    val supervisors: StateFlow<List<SupervisorEntity>> = _supervisors.asStateFlow()",
    "",
    "    internal val _colorPalettes = MutableStateFlow<List<ColorPaletteEntity>>(emptyList())",
    "    val colorPalettes: StateFlow<List<ColorPaletteEntity>> = _colorPalettes.asStateFlow()",
    "",
    "    internal val _isOnline = MutableStateFlow(true)",
    "    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()",
    "",
    "    internal val _currentUserPoints = MutableStateFlow(0)",
    "    val currentUserPoints: StateFlow<Int> = _currentUserPoints.asStateFlow()",
    "",
    "    private val _screenBackStack = androidx.compose.runtime.mutableStateListOf<String>()",
    "",
    "    // ------------------- ViewModels -------------------",
    "    val authViewModel = com.example.ui.viewmodels.AuthViewModel()",
    "    val homeViewModel = com.example.ui.viewmodels.HomeViewModel()",
    "    val bookingViewModel = com.example.ui.viewmodels.BookingViewModel()",
    "    val chatViewModel = com.example.ui.viewmodels.ChatViewModel().apply {",
    "        currentUserIdProvider = { _currentUserId.value }",
    "        currentUserNameProvider = { _currentUserName.value }",
    "        currentUserPhoneProvider = { _currentUserPhone.value }",
    "        systemSettingsProvider = { _settings.value }",
    "        addNotificationHandler = { title, message, targetType, targetValue ->",
    "            addNotification(title, message, targetType, targetValue)",
    "        }",
    "    }",
    "    val notificationViewModel = com.example.ui.screens.notifications.NotificationViewModel(this)",
    "    val adminViewModel = com.example.ui.viewmodels.AdminViewModel().apply { mainViewModel = this@MainViewModel }",
    "    val settingsViewModel = com.example.ui.viewmodels.SettingsViewModel().apply { mainViewModel = this@MainViewModel }",
    "    val instantRequestViewModel = com.example.ui.viewmodels.InstantRequestViewModel().apply { mainViewModel = this@MainViewModel }",
    "",
    "    // ------------------- Delegated StateFlows -------------------",
    "    // Auth",
    "    val _currentUserId get() = authViewModel._currentUserId",
    "    val currentUserId get() = authViewModel.currentUserId",
    "    val _currentUserName get() = authViewModel._currentUserName",
    "    val currentUserName get() = authViewModel.currentUserName",
    "    val _currentUserPhone get() = authViewModel._currentUserPhone",
    "    val currentUserPhone get() = authViewModel.currentUserPhone",
    "    val _currentUserResidence get() = authViewModel._currentUserResidence",
    "    val currentUserResidence get() = authViewModel.currentUserResidence",
    "    val _adminRole get() = authViewModel._adminRole",
    "    val adminRole get() = authViewModel.adminRole",
    "    val _passwordRecoveryWaitingPhone get() = authViewModel._passwordRecoveryWaitingPhone",
    "    val passwordRecoveryWaitingPhone get() = authViewModel.passwordRecoveryWaitingPhone",
    "    val _joinRequestPhone get() = authViewModel._joinRequestPhone",
    "    val joinRequestPhone get() = authViewModel.joinRequestPhone",
    "",
    "    // Home",
    "    val _categories get() = homeViewModel._categories",
    "    val categories get() = homeViewModel.categories",
    "    val _providers get() = homeViewModel._providers",
    "    val providers get() = homeViewModel.providers",
    "    val _filteredProviders get() = homeViewModel._filteredProviders",
    "    val filteredProviders get() = homeViewModel.filteredProviders",
    "    val _banners get() = homeViewModel._banners",
    "    val banners get() = homeViewModel.banners",
    "",
    "    // Booking",
    "    val _bookings get() = bookingViewModel._bookings",
    "    val bookings get() = bookingViewModel.bookings",
    "    val _bookingFormFields = bookingViewModel._bookingFormFields",
    "    val bookingFormFields = bookingViewModel.bookingFormFields",
    "    val _distributionMode = bookingViewModel._distributionMode",
    "    val distributionMode = bookingViewModel.distributionMode",
    "",
    "    // Chat",
    "    val _chatMessages get() = chatViewModel._chatMessages",
    "    val chatMessages get() = chatViewModel.chatMessages",
    "    val _chatChannels get() = chatViewModel._chatChannels",
    "    val chatChannels get() = chatViewModel.chatChannels",
    "    val _activeChatChannel get() = chatViewModel._activeChatChannel",
    "    val activeChatChannel get() = chatViewModel.activeChatChannel",
    "",
    "    // Admin",
    "    val _pendingProviders get() = adminViewModel._pendingProviders",
    "    val pendingProviders get() = adminViewModel.pendingProviders",
    "    val _pendingTechnicians get() = adminViewModel._pendingTechnicians",
    "    val pendingTechnicians get() = adminViewModel.pendingTechnicians",
    "    val _registeredUsersList get() = adminViewModel._registeredUsersList",
    "    val registeredUsersList get() = adminViewModel.registeredUsersList",
    "    val _registeredUsersCount get() = adminViewModel._registeredUsersCount",
    "    val registeredUsersCount get() = adminViewModel.registeredUsersCount",
    "    val _reports get() = adminViewModel._reports",
    "    val reports get() = adminViewModel.reports",
    "    val _activityLogs get() = adminViewModel._activityLogs",
    "    val activityLogs get() = adminViewModel.activityLogs",
    "    val _callsLog get() = adminViewModel._callsLog",
    "    val callsLog get() = adminViewModel.callsLog",
    "    val _coupons get() = adminViewModel._coupons",
    "    val coupons get() = adminViewModel.coupons",
    "    val _internalWallets get() = adminViewModel._internalWallets",
    "    val internalWallets get() = adminViewModel.internalWallets",
    "    val _walletTransactions get() = adminViewModel._walletTransactions",
    "    val walletTransactions get() = adminViewModel.walletTransactions",
    "    val _paymentWallets get() = adminViewModel._paymentWallets",
    "    val paymentWallets get() = adminViewModel.paymentWallets",
    "    val _payments get() = adminViewModel._payments",
    "    val payments get() = adminViewModel.payments",
    "    val _orders get() = adminViewModel._orders",
    "    val orders get() = adminViewModel.orders",
    "    val _ratings get() = adminViewModel._ratings",
    "    val ratings get() = adminViewModel.ratings",
    "    val _customProfileTabs get() = adminViewModel._customProfileTabs",
    "    val customProfileTabs get() = adminViewModel.customProfileTabs",
    "    val _stores get() = adminViewModel._stores",
    "    val stores get() = adminViewModel.stores",
    "    val _products get() = adminViewModel._products",
    "    val products get() = adminViewModel.products",
    "    val _properties get() = adminViewModel._properties",
    "    val properties get() = adminViewModel.properties",
    "    val _jobs get() = adminViewModel._jobs",
    "    val jobs get() = adminViewModel.jobs",
    "    val _jobApplications get() = adminViewModel._jobApplications",
    "    val jobApplications get() = adminViewModel.jobApplications",
    "",
    "    // Settings",
    "    val _settings get() = settingsViewModel._settings",
    "    val settings get() = settingsViewModel.settings",
    "    val _cardSettings get() = settingsViewModel._cardSettings",
    "    val cardSettings get() = settingsViewModel.cardSettings",
    "    val _colorScheme get() = settingsViewModel._colorScheme",
    "    val colorScheme get() = settingsViewModel.colorScheme",
    "    val _personalColors get() = settingsViewModel._personalColors",
    "    val personalColors get() = settingsViewModel.personalColors",
    "    val _colorSyncStatus get() = settingsViewModel._colorSyncStatus",
    "    val colorSyncStatus get() = settingsViewModel.colorSyncStatus",
    "    val _colorSyncLogs get() = settingsViewModel._colorSyncLogs",
    "    val colorSyncLogs get() = settingsViewModel.colorSyncLogs",
    "    val _pendingConflictScheme get() = settingsViewModel._pendingConflictScheme",
    "    val pendingConflictScheme get() = settingsViewModel.pendingConflictScheme",
    "    val _blockedChatParticipants get() = settingsViewModel._blockedChatParticipants",
    "    val blockedChatParticipants get() = settingsViewModel.blockedChatParticipants",
    "    val _activeVoiceCall get() = settingsViewModel._activeVoiceCall",
    "    val activeVoiceCall get() = settingsViewModel.activeVoiceCall",
    "",
    "    // Instant Request",
    "    val _instantRequests get() = instantRequestViewModel._instantRequests",
    "    val instantRequests get() = instantRequestViewModel.instantRequests",
    "    val _requestOffers get() = instantRequestViewModel._requestOffers",
    "    val requestOffers get() = instantRequestViewModel.requestOffers",
    "    val _offers get() = instantRequestViewModel._offers",
    "    val offers get() = instantRequestViewModel.offers",
    "",
    "    // ------------------- Navigation / Shared UI State -------------------",
    "    private val _currentScreen = MutableStateFlow(\"USER_BROWSE\")",
    "    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()",
    "",
    "    private val _navigationStack = mutableListOf<String>()",
    "    ",
    "    var selectedProvider: com.example.data.ProviderEntity? = null",
    "    var selectedStore: com.example.data.StoreEntity? = null",
    "    var selectedProperty: com.example.data.PropertyEntity? = null",
    "    var selectedOfferId by androidx.compose.runtime.mutableStateOf(\"\")",
    "    var selectedRequestId by androidx.compose.runtime.mutableStateOf(\"\")",
    "    var showQuickServiceDialog by androidx.compose.runtime.mutableStateOf(false)",
    "",
    "    internal val _userLatitude = MutableStateFlow(15.3694)",
    "    val userLatitude: StateFlow<Double> = _userLatitude.asStateFlow()",
    "",
    "    internal val _userLongitude = MutableStateFlow(44.1910)",
    "    val userLongitude: StateFlow<Double> = _userLongitude.asStateFlow()",
    "",
    "    internal val _isGpsTrackingActive = MutableStateFlow(false)",
    "    val isGpsTrackingActive: StateFlow<Boolean> = _isGpsTrackingActive.asStateFlow()",
    "",
    "    internal val _isProvidersLoading = MutableStateFlow(true)",
    "    val isProvidersLoading: StateFlow<Boolean> = _isProvidersLoading.asStateFlow()",
    "",
    "    internal val _isChatChannelsLoading = MutableStateFlow(true)",
    "    val isChatChannelsLoading: StateFlow<Boolean> = _isChatChannelsLoading.asStateFlow()",
    "",
    "    internal val _cities = MutableStateFlow<List<CityEntity>>(emptyList())",
    "    val cities: StateFlow<List<CityEntity>> = _cities.asStateFlow()",
    "",
    "    internal val _deletedProviders = MutableStateFlow<List<ProviderEntity>>(emptyList())",
    "    val deletedProviders: StateFlow<List<ProviderEntity>> = _deletedProviders.asStateFlow()",
    "",
    "    internal val _isInitialized = MutableStateFlow(false)",
    "    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()",
    "",
    "    internal val _maxKmRadius = MutableStateFlow(10)",
    "    val maxKmRadius: StateFlow<Int> = _maxKmRadius.asStateFlow()",
    "",
    "    init {",
    "        _stores.value = getDefaultStoresList()",
    "        _properties.value = getDefaultPropertiesList()",
    "    }",
    "",
    "    private fun checkAndTriggerFavoriteOffersNotifications() {",
    "        // Placeholder implementation for backward compatibility",
    "    }",
    ""
]

# Add core functions implementation
for f in core_funcs:
    if f in extracted_core:
        for code in extracted_core[f]:
            new_main.append(code)
            new_main.append("")

# Add delegated functions
new_main.append("    // =------------------- Delegated Functions -------------------=")
for dlg in delegations:
    new_main.append(dlg)

new_main.append("}")

with open('app/src/main/java/com/example/ui/MainViewModel.kt', 'w', encoding='utf-8') as f:
    f.write("\n".join(new_main))

print("Rewrote MainViewModel.kt with correct signatures and stripped comments!")
