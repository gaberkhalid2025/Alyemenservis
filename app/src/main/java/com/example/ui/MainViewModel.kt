package com.example.ui
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.example.utils.*
import com.example.ui.viewmodels.BaseViewModel
import com.example.ui.viewmodels.BookingDistributionMode
import com.example.ui.viewmodels.BookingFormFields
import com.example.ui.viewmodels.BookingStatus
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.data.models.*
import com.example.ui.viewmodels.SettingsViewModel.CardSettings
import com.example.ui.viewmodels.SettingsViewModel.ChatParticipantType
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

@HiltViewModel
class MainViewModel @Inject constructor(
    val authViewModel: com.example.ui.viewmodels.AuthViewModel,
    val homeViewModel: com.example.ui.viewmodels.HomeViewModel,
    val bookingViewModel: com.example.ui.viewmodels.BookingViewModel,
    val adminViewModel: com.example.ui.viewmodels.AdminViewModel,
    val settingsViewModel: com.example.ui.viewmodels.SettingsViewModel,
    val instantRequestViewModel: com.example.ui.viewmodels.InstantRequestViewModel,
    val chatRepo: com.example.data.repositories.ChatRepository
) : BaseViewModel() {

    override val db by lazy {
        val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        try {
            val settings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .setCacheSizeBytes(104857600L)
                .build()
            firestore.firestoreSettings = settings
        } catch (e: Exception) {
            e.printStackTrace()
        }
        firestore
    }
    val preferenceHelper = com.example.ui.helpers.AppPreferenceHelper()
    val firestoreSeedHelper by lazy { com.example.ui.helpers.FirestoreSeedHelper(db) }
    val realtimeSyncHelper by lazy { com.example.ui.helpers.RealtimeSyncHelper(db) }
    val registrationHelper by lazy { com.example.ui.helpers.RegistrationHelper(db, auth, preferenceHelper) }
    val accountRecoveryHelper by lazy { com.example.ui.helpers.AccountRecoveryHelper(db, preferenceHelper) }

    override val firestoreListeners = mutableListOf<com.google.firebase.firestore.ListenerRegistration>()
    override fun onCleared() {
        super.onCleared()
        try {
            realtimeSyncHelper.clearListeners()
            firestoreListeners.forEach { it.remove() }
            firestoreListeners.clear()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    private val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
    // ------------------- Local StateFlows -------------------
    private val _currentLanguage = MutableStateFlow("ar")
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()
    private val _favoriteIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteIds: StateFlow<Set<String>> = _favoriteIds.asStateFlow()
    internal val _notifications = MutableStateFlow<List<NotificationEntity>>(emptyList())
    val notifications: StateFlow<List<NotificationEntity>> = _notifications.asStateFlow()
    internal val _readNotificationIds = MutableStateFlow<Set<String>>(emptySet())
    val readNotificationIds: StateFlow<Set<String>> = _readNotificationIds.asStateFlow()
    internal val _supervisors = MutableStateFlow<List<SupervisorEntity>>(emptyList())
    val supervisors: StateFlow<List<SupervisorEntity>> = _supervisors.asStateFlow()
    internal val _colorPalettes = MutableStateFlow<List<ColorPaletteEntity>>(emptyList())
    val colorPalettes: StateFlow<List<ColorPaletteEntity>> = _colorPalettes.asStateFlow()
    internal val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()
    internal val _currentUserPoints = MutableStateFlow(0)
    val currentUserPoints: StateFlow<Int> = _currentUserPoints.asStateFlow()
    private val _screenBackStack = MutableStateFlow<List<String>>(listOf("USER_BROWSE"))
    val screenBackStack: StateFlow<List<String>> = _screenBackStack.asStateFlow()
    // ------------------- ViewModels -------------------
    val notificationViewModel = com.example.ui.screens.notifications.NotificationViewModel(this)
    // ------------------- Delegated StateFlows -------------------
    // Auth
    val _currentUserId get() = authViewModel._currentUserId
    val currentUserId get() = authViewModel.currentUserId
    val _currentUserName get() = authViewModel._currentUserName
    val currentUserName get() = authViewModel.currentUserName
    val _currentUserPhone get() = authViewModel._currentUserPhone
    val currentUserPhone get() = authViewModel.currentUserPhone
    val _currentUserResidence get() = authViewModel._currentUserResidence
    val currentUserResidence get() = authViewModel.currentUserResidence
    val _adminRole get() = authViewModel._adminRole
    val adminRole get() = authViewModel.adminRole
    val _passwordRecoveryWaitingPhone get() = authViewModel._passwordRecoveryWaitingPhone
    val passwordRecoveryWaitingPhone get() = authViewModel.passwordRecoveryWaitingPhone
    val _joinRequestPhone get() = authViewModel._joinRequestPhone
    val joinRequestPhone get() = authViewModel.joinRequestPhone
    val showBackdoorDialog get() = authViewModel.showBackdoorDialog
    // Home
    val _categories get() = homeViewModel._categories
    val categories get() = homeViewModel.categories
    val _providers get() = homeViewModel._providers
    val providers get() = homeViewModel.providers
    val _filteredProviders get() = homeViewModel._filteredProviders
    val filteredProviders get() = homeViewModel.filteredProviders
    val _banners get() = homeViewModel._banners
    val banners get() = homeViewModel.banners
    val _selectedCategoryId get() = homeViewModel._selectedCategoryId
    val selectedCategoryId get() = homeViewModel.selectedCategoryId
    val _searchQuery get() = homeViewModel._searchQuery
    val searchQuery get() = homeViewModel.searchQuery
    val _filterVipOnly get() = homeViewModel._filterVipOnly
    val filterVipOnly get() = homeViewModel.filterVipOnly
    val _filterAvailableOnly get() = homeViewModel._filterAvailableOnly
    val filterAvailableOnly get() = homeViewModel.filterAvailableOnly
    val _filterCityId get() = homeViewModel._filterCityId
    val filterCityId get() = homeViewModel.filterCityId
    val _filterNeighborhoodName get() = homeViewModel._filterNeighborhoodName
    val filterNeighborhoodName get() = homeViewModel.filterNeighborhoodName
    val _phoneOrNameFilter get() = homeViewModel._phoneOrNameFilter
    val phoneOrNameFilter get() = homeViewModel.phoneOrNameFilter
    // Booking
    val _bookings get() = bookingViewModel._bookings
    val bookings get() = bookingViewModel.bookings
    val _bookingFormFields = bookingViewModel._bookingFormFields
    val bookingFormFields = bookingViewModel.bookingFormFields
    val _distributionMode = bookingViewModel._distributionMode
    val distributionMode = bookingViewModel.distributionMode
    // Chat
    internal val _chatMessages = MutableStateFlow<List<com.example.data.ChatMessageEntity>>(emptyList())
    val chatMessages: StateFlow<List<com.example.data.ChatMessageEntity>> = _chatMessages.asStateFlow()
    internal val _chatChannels = MutableStateFlow<List<com.example.data.ChatChannelEntity>>(emptyList())
    val chatChannels: StateFlow<List<com.example.data.ChatChannelEntity>> = _chatChannels.asStateFlow()
    internal val _activeChatChannel = MutableStateFlow<com.example.data.ChatChannelEntity?>(null)
    val activeChatChannel: StateFlow<com.example.data.ChatChannelEntity?> = _activeChatChannel.asStateFlow()
    // Admin
    val _pendingProviders get() = adminViewModel._pendingProviders
    val pendingProviders get() = adminViewModel.pendingProviders
    val _pendingTechnicians get() = adminViewModel._pendingTechnicians
    val pendingTechnicians get() = adminViewModel.pendingTechnicians
    val _registeredUsersList get() = adminViewModel._registeredUsersList
    val registeredUsersList get() = adminViewModel.registeredUsersList
    val _registeredUsersCount get() = adminViewModel._registeredUsersCount
    val registeredUsersCount get() = adminViewModel.registeredUsersCount
    val _reports get() = adminViewModel._reports
    val reports get() = adminViewModel.reports
    val _activityLogs get() = adminViewModel._activityLogs
    val activityLogs get() = adminViewModel.activityLogs
    val _callsLog get() = adminViewModel._callsLog
    val callsLog get() = adminViewModel.callsLog
    val _coupons get() = adminViewModel._coupons
    val coupons get() = adminViewModel.coupons
    val _internalWallets get() = adminViewModel._internalWallets
    val internalWallets get() = adminViewModel.internalWallets
    val _walletTransactions get() = adminViewModel._walletTransactions
    val walletTransactions get() = adminViewModel.walletTransactions
    val _paymentWallets get() = adminViewModel._paymentWallets
    val paymentWallets get() = adminViewModel.paymentWallets
    val _payments get() = adminViewModel._payments
    val payments get() = adminViewModel.payments
    val _orders get() = adminViewModel._orders
    val orders get() = adminViewModel.orders
    val _ratings get() = adminViewModel._ratings
    val ratings get() = adminViewModel.ratings
    val _customProfileTabs get() = adminViewModel._customProfileTabs
    val customProfileTabs get() = adminViewModel.customProfileTabs
    val _stores get() = adminViewModel._stores
    val stores get() = adminViewModel.stores
    val _products get() = adminViewModel._products
    val products get() = adminViewModel.products
    val _properties get() = adminViewModel._properties
    val properties get() = adminViewModel.properties
    val _jobs get() = adminViewModel._jobs
    val jobs get() = adminViewModel.jobs
    val _jobApplications get() = adminViewModel._jobApplications
    val jobApplications get() = adminViewModel.jobApplications
    // Settings
    val _settings get() = settingsViewModel._settings
    val settings get() = settingsViewModel.settings
    val _cardSettings get() = settingsViewModel._cardSettings
    val cardSettings get() = settingsViewModel.cardSettings
    val _colorScheme get() = settingsViewModel._colorScheme
    val colorScheme get() = settingsViewModel.colorScheme
    val _personalColors get() = settingsViewModel._personalColors
    val personalColors get() = settingsViewModel.personalColors
    val _colorSyncStatus get() = settingsViewModel._colorSyncStatus
    val colorSyncStatus get() = settingsViewModel.colorSyncStatus
    val _colorSyncLogs get() = settingsViewModel._colorSyncLogs
    val colorSyncLogs get() = settingsViewModel.colorSyncLogs
    val _pendingConflictScheme get() = settingsViewModel._pendingConflictScheme
    val pendingConflictScheme get() = settingsViewModel.pendingConflictScheme
    val _blockedChatParticipants get() = settingsViewModel._blockedChatParticipants
    val blockedChatParticipants get() = settingsViewModel.blockedChatParticipants
    val _activeVoiceCall get() = settingsViewModel._activeVoiceCall
    val activeVoiceCall get() = settingsViewModel.activeVoiceCall
    // Instant Request
    val _instantRequests get() = instantRequestViewModel._instantRequests
    val instantRequests get() = instantRequestViewModel.instantRequests
    val _requestOffers get() = instantRequestViewModel._requestOffers
    val requestOffers get() = instantRequestViewModel.requestOffers
    val _offers get() = instantRequestViewModel._offers
    val offers get() = instantRequestViewModel.offers
    // ------------------- Navigation / Shared UI State -------------------
    private val _currentScreen = MutableStateFlow("USER_BROWSE")
    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()
    private val _navigationStack = mutableListOf<String>()
    
    var selectedProvider: com.example.data.ProviderEntity? = null
    var selectedStore: com.example.data.StoreEntity? = null
    var selectedProperty: com.example.data.PropertyEntity? = null
    var selectedJob: com.example.data.JobEntity? = null
    var selectedOfferId by androidx.compose.runtime.mutableStateOf("")
    var selectedRequestId by androidx.compose.runtime.mutableStateOf("")
    var showQuickServiceDialog by androidx.compose.runtime.mutableStateOf(false)
    internal val _userLatitude = MutableStateFlow(15.3694)
    val userLatitude: StateFlow<Double> = _userLatitude.asStateFlow()
    internal val _userLongitude = MutableStateFlow(44.1910)
    val userLongitude: StateFlow<Double> = _userLongitude.asStateFlow()
    internal val _isGpsTrackingActive = MutableStateFlow(false)
    val isGpsTrackingActive: StateFlow<Boolean> = _isGpsTrackingActive.asStateFlow()
    internal val _isProvidersLoading = MutableStateFlow(true)
    val isProvidersLoading: StateFlow<Boolean> = _isProvidersLoading.asStateFlow()
    internal val _isChatChannelsLoading = MutableStateFlow(true)
    val isChatChannelsLoading: StateFlow<Boolean> = _isChatChannelsLoading.asStateFlow()
    internal val _cities = MutableStateFlow<List<CityEntity>>(emptyList())
    val cities: StateFlow<List<CityEntity>> = _cities.asStateFlow()
    internal val _deletedProviders = MutableStateFlow<List<ProviderEntity>>(emptyList())
    val deletedProviders: StateFlow<List<ProviderEntity>> = _deletedProviders.asStateFlow()
    internal val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()
    internal val _maxKmRadius = MutableStateFlow(10)
    val maxKmRadius: StateFlow<Int> = _maxKmRadius.asStateFlow()
    init {
        _stores.value = getDefaultStoresList()
        _properties.value = getDefaultPropertiesList()
    }
    private fun checkAndTriggerFavoriteOffersNotifications() {
        // Placeholder implementation for backward compatibility
    }
fun updateUserLocation(lat: Double, lng: Double) {
        _userLatitude.value = lat
        _userLongitude.value = lng
    }
fun startLocationUpdates() {
        _isGpsTrackingActive.value = true
        appContext?.let { ctx ->
            try {
                val lm = ctx.getSystemService(android.content.Context.LOCATION_SERVICE) as? android.location.LocationManager
                val loc = lm?.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
                    ?: lm?.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)
                loc?.let {
                    updateUserLocation(it.latitude, it.longitude)
                }
            } catch (e: SecurityException) {
                e.printStackTrace()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
fun refreshData(showNotification: Boolean = false) {
        viewModelScope.launch {
            _isRefreshing.value = true
            _uiErrorMessage.value = null
            try {
                firestoreListeners.forEach { it.remove() }
                firestoreListeners.clear()
                setupRealtimeFirestoreListeners()
                if (showNotification) {
                    triggerNotification("🔄 تم تحديث البيانات بنجاح!")
                }
            } catch (e: Exception) {
                _uiErrorMessage.value = "تعذر تحديث البيانات: ${e.localizedMessage}"
            } finally {
                kotlinx.coroutines.delay(600)
                _isRefreshing.value = false
            }
        }
    }
fun updateOnlineStatus(online: Boolean) {
        _isOnline.value = online
    }
fun retryConnection(context: android.content.Context) {
        val cm = context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
        if (cm != null) {
            val activeNetwork = cm.activeNetwork
            val capabilities = cm.getNetworkCapabilities(activeNetwork)
            val online = capabilities?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
            _isOnline.value = online
            if (online) {
                triggerNotification("🟢 تم استعادة الاتصال بالشبكة بنجاح وجاري مزامنة البيانات!")
                setupRealtimeFirestoreListeners()
            } else {
                triggerNotification("❌ فشل الاتصال: يرجى التحقق من باقة الإنترنت أو شبكة الواي فاي.")
            }
        }
    }
fun updateUserFcmToken(userId: String, token: String) {
        if (userId.isEmpty() || userId == "guest") return
        try {
            db.collection("registered_users").document(userId).update("fcmToken", token)
            val cleanPhone = _currentUserPhone.value.trim().replace(" ", "").replace("+", "")
            if (cleanPhone.isNotEmpty()) {
                db.collection("providers").document(cleanPhone).update("fcmToken", token)
                db.collection("stores").document(cleanPhone).update("fcmToken", token)
                db.collection("properties").document(cleanPhone).update("fcmToken", token)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
fun initializeFirestoreCollections() {
        android.util.Log.d("MainViewModel", "📤 بدء تهيئة المجموعات في Firestore")
        val collections = listOf(
            "users", "pending_providers", "providers", "stores",
            "restaurants", "medical", "properties", "jobs",
            "bookings", "instant_requests", "instant_offers",
            "notifications", "password_reset_requests", "join_requests", "fcm_tokens"
        )
        viewModelScope.launch {
            collections.forEach { collection ->
                try {
                    db.collection(collection).document("_init_")
                        .set(mapOf("initialized" to true))
                        .addOnSuccessListener {
                            db.collection(collection).document("_init_").delete()
                            android.util.Log.d("MainViewModel", "✅ تم تهيئة المجموعة بنجاح: $collection")
                        }
                } catch (e: Exception) {
                    android.util.Log.e("MainViewModel", "❌ خطأ أثناء تهيئة المجموعة $collection: ${e.message}")
                }
            }
        }
    }
fun initializeUserIdentity(context: android.content.Context) {
        android.util.Log.d("MainViewModel", "🚀 [START] initializeUserIdentity")
        appContext = context.applicationContext
        
        // Propagate appContext to all other sub-ViewModels
        authViewModel.appContext = appContext
        homeViewModel.appContext = appContext
        bookingViewModel.appContext = appContext
        adminViewModel.appContext = appContext
        settingsViewModel.appContext = appContext
        instantRequestViewModel.appContext = appContext
        // Wire up bookingViewModel decoupled delegates
        bookingViewModel.getCoupons = { _coupons.value }
        bookingViewModel.getProviders = { _providers.value }
        bookingViewModel.getCurrentUserPhone = { _currentUserPhone.value }
        bookingViewModel.getCurrentUserName = { _currentUserName.value }
        bookingViewModel.getCurrentUserResidence = { _currentUserResidence.value }
        bookingViewModel.setCurrentUserPhone = { _currentUserPhone.value = it }
        bookingViewModel.setCurrentUserName = { _currentUserName.value = it }
        bookingViewModel.setCurrentUserResidence = { _currentUserResidence.value = it }
        bookingViewModel.onAddNotification = { title, message, targetType, targetValue ->
            addNotification(title, message, targetType, targetValue)
        }
        bookingViewModel.triggerNotificationCallback = { msg ->
            triggerNotification(msg)
        }
        bookingViewModel.onOpenOrCreateChatChannel = { targetId, targetType, targetName, targetPhone, targetCategory, relatedEntityId, relatedEntityType, onComplete ->
            openOrCreateChatChannel(
                targetId = targetId,
                targetType = targetType,
                targetName = targetName,
                targetPhone = targetPhone,
                targetCategory = targetCategory,
                relatedEntityId = relatedEntityId,
                relatedEntityType = relatedEntityType.ifEmpty { "BOOKING" },
                onCreated = onComplete
            )
        }
        // Wire up adminViewModel decoupled delegates
        adminViewModel.getHomeViewModel = { homeViewModel }
        adminViewModel.getSettingsViewModel = { settingsViewModel }
        adminViewModel.getBookingViewModel = { bookingViewModel }
        adminViewModel.getInstantRequestViewModel = { instantRequestViewModel }
        adminViewModel.getNotifications = { _notifications }
        adminViewModel.onAddNotification = { title, message, targetType, targetValue ->
            addNotification(title, message, targetType, targetValue)
        }
        adminViewModel.onTriggerNotificationFull = { title, message, targetType, targetValue ->
            triggerNotification(title, message, targetType, targetValue)
        }
        adminViewModel.onTriggerNotification = { msg ->
            triggerNotification(msg)
        }
        adminViewModel.onApplyFilters = { applyFilters() }
        // Wire up settingsViewModel decoupled delegates
        settingsViewModel.getAuthViewModel = { authViewModel }
        settingsViewModel.getHomeViewModel = { homeViewModel }
        settingsViewModel.getBookingViewModel = { bookingViewModel }
        settingsViewModel.getAdminViewModel = { adminViewModel }
        settingsViewModel.getProviders = { _providers }
        settingsViewModel.getBookings = { _bookings }
        settingsViewModel.getCategories = { _categories }
        settingsViewModel.getStores = { _stores }
        settingsViewModel.getProperties = { _properties }
        settingsViewModel.getPasswordRecoveryWaitingPhone = { _passwordRecoveryWaitingPhone }
        settingsViewModel.setPasswordRecoveryWaitingPhone = { setPasswordRecoveryWaitingPhone(it) }
        settingsViewModel.verifyAdminOrOwnerPassword = { verifyAdminOrOwnerPassword(it) }
        settingsViewModel.triggerNotification = { triggerNotification(it) }
        // Wire up instantRequestViewModel decoupled delegates
        instantRequestViewModel.triggerNotification = { triggerNotification(it) }
        instantRequestViewModel.addNotification = { title, message, targetType, targetValue ->
            addNotification(title, message, targetType, targetValue)
        }
        instantRequestViewModel.getOrCreateChatChannel = { providerId, providerName, customerPhone, customerName ->
            openOrCreateChatChannel(
                targetId = providerId,
                targetType = "TECHNICIAN",
                targetName = providerName,
                targetPhone = "",
                targetCategory = "",
                relatedEntityId = "",
                relatedEntityType = "URGENT_REQUEST",
                onCreated = {}
            )
        }
        try {
            // 1. Initialize collections
            initializeFirestoreCollections()
        } catch (e: Exception) {
            android.util.Log.e("MainViewModel", "❌ Error in initializeFirestoreCollections", e)
        }
        try {
            // 2. Load and initialize identity in AuthViewModel
            authViewModel.initializeUserIdentity(context) { savedFavs ->
                _favoriteIds.value = savedFavs
                checkAndTriggerFavoriteOffersNotifications()
            }
        } catch (e: Exception) {
            android.util.Log.e("MainViewModel", "❌ Error in AuthViewModel initialization", e)
        }
        try {
            // 3. Start realtime firestore listeners
            setupRealtimeFirestoreListeners()
        } catch (e: Exception) {
            android.util.Log.e("MainViewModel", "❌ Error in setting up realtime listeners", e)
        }
        try {
            // 4. Load card settings
            settingsViewModel.loadCardSettings()
        } catch (e: Exception) {
            android.util.Log.e("MainViewModel", "❌ Error in SettingsViewModel loadCardSettings", e)
        }
        try {
            // 5. Load pending technicians
            adminViewModel.loadPendingTechnicians()
        } catch (e: Exception) {
            android.util.Log.e("MainViewModel", "❌ Error in AdminViewModel loadPendingTechnicians", e)
        }
        try {
            // 6. Seed Firestore if empty
            seedFirestoreIfEmpty()
        } catch (e: Exception) {
            android.util.Log.e("MainViewModel", "❌ Error in seedFirestoreIfEmpty", e)
        }
        // 7. Ensure _isInitialized is set to true to prevent screen freezing
        viewModelScope.launch {
            kotlinx.coroutines.delay(2200)
            _isInitialized.value = true
            android.util.Log.d("MainViewModel", "✅ [FINISH] App initialized successfully, _isInitialized = true")
        }
    }
    fun setupRealtimeFirestoreListeners() {
        realtimeSyncHelper.clearListeners()
        realtimeSyncHelper.setupRealtimeFirestoreListeners(this)
    }

    fun seedFirestoreIfEmpty() {
        firestoreSeedHelper.seedFirestoreIfEmpty()
    }

    override fun getDefaultStoresList(): List<com.example.data.StoreEntity> {
        return emptyList()
    }

    override fun getDefaultPropertiesList(): List<com.example.data.PropertyEntity> {
        return emptyList()
    }

fun applyFilters() {
        homeViewModel.applyFilters(_currentUserResidence.value)
    }
fun selectCategory(categoryId: String?) {
        homeViewModel.selectCategory(categoryId, _currentUserResidence.value)
    }
fun updateSearchQuery(query: String) {
        homeViewModel.updateSearchQuery(query, _currentUserResidence.value)
    }
fun toggleVipFilter() {
        homeViewModel.toggleVipFilter(_currentUserResidence.value)
    }
fun toggleAvailableFilter() {
        homeViewModel.toggleAvailableFilter(_currentUserResidence.value)
    }
fun setCityFilter(cityId: String?) {
        homeViewModel.setCityFilter(cityId, _currentUserResidence.value)
    }
fun setNeighborhoodFilter(neighborhood: String) {
        homeViewModel.setNeighborhoodFilter(neighborhood, _currentUserResidence.value)
    }
fun setPhoneOrNameFilter(text: String) {
        homeViewModel.setPhoneOrNameFilter(text, _currentUserResidence.value)
    }
fun setRadiusKm(km: Int) {
        homeViewModel.setRadiusKm(km, _currentUserResidence.value)
    }
fun registerBackdoorInteraction() {
        authViewModel.registerBackdoorInteraction()
    }
fun changeAdminCredentials(username: String, password: String) {
        triggerNotification("🔐 تم تغيير بيانات المدير الرئيسي")
    }
    fun authenticateAdmin(context: android.content.Context, role: String, remember: Boolean) {
        authViewModel.authenticateAdmin(context, role, remember)
        navigateTo("ADMIN_PANEL")
    }
    fun authenticateAdmin(role: String) {
        authViewModel.authenticateAdmin(role)
        navigateTo("ADMIN_PANEL")
    }
    fun logout(context: android.content.Context) {
        authViewModel.logout(context)
        navigateTo("USER_BROWSE")
    }
    fun navigateToScreen(screen: String) = navigateTo(screen)
    fun navigateAndRemoveScreens(targetScreen: String, screensToRemove: List<String>) {
        val updated = _screenBackStack.value.toMutableList()
        updated.removeAll(screensToRemove.toSet())
        if (targetScreen == "USER_BROWSE") {
            updated.clear()
            updated.add("USER_BROWSE")
        } else if (!updated.contains(targetScreen)) {
            updated.add(targetScreen)
        }
        _screenBackStack.value = updated
        _currentScreen.value = targetScreen
    }
    fun navigateTo(screen: String) {
        if (_currentScreen.value != screen) {
            val updated = _screenBackStack.value.toMutableList()
            if (screen == "USER_BROWSE") {
                updated.clear()
                updated.add("USER_BROWSE")
            } else {
                updated.add(screen)
            }
            _screenBackStack.value = updated
            _currentScreen.value = screen
        }
    }
fun goBack(): Boolean {
        val stack = _screenBackStack.value.toMutableList()
        if (stack.size > 1) {
            stack.removeAt(stack.size - 1)
            val prev = stack.last()
            _screenBackStack.value = stack
            _currentScreen.value = prev
            return true
        } else if (_currentScreen.value != "USER_BROWSE") {
            _screenBackStack.value = listOf("USER_BROWSE")
            _currentScreen.value = "USER_BROWSE"
            return true
        }
        return false
    }
fun switchLanguage() {
        val ctx = appContext
        if (ctx != null) {
            val newLang = LocaleManager.toggleLanguage(ctx)
            _currentLanguage.value = newLang
        } else {
            val newLang = if (_currentLanguage.value == "ar") "en" else "ar"
            _currentLanguage.value = newLang
        }
    }
fun toggleLanguage(context: android.content.Context) {
        appContext = context.applicationContext
        val newLang = LocaleManager.toggleLanguage(context)
        _currentLanguage.value = newLang
    }
fun setLanguage(lang: String) {
        _currentLanguage.value = lang
        val ctx = appContext
        if (ctx != null) {
            LocaleManager.setLanguage(ctx, lang)
        }
        val newSettings = _settings.value.copy(appLanguage = lang)
        _settings.value = newSettings
        try {
            db.collection("settings").document("main_settings").update("appLanguage", lang)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
fun setLanguage(context: android.content.Context, lang: String) {
        appContext = context.applicationContext
        setLanguage(lang)
    }
fun triggerNotification(
        title: String,
        message: String,
        targetType: String = "ALL",
        targetValue: String = "",
        context: android.content.Context? = null
    ) {
        val newNotif = com.example.data.NotificationEntity(
            id = java.util.UUID.randomUUID().toString(),
            title = title,
            message = message,
            targetType = targetType,
            targetValue = targetValue,
            timestamp = System.currentTimeMillis()
        )
        val currentList = _notifications.value.toMutableList()
        currentList.add(0, newNotif)
        _notifications.value = currentList
        try {
            db.collection("notifications").document(newNotif.id).set(newNotif)
        } catch (e: Exception) {}
        triggerNotification("$title: $message", context)
    }
    private var lastNotifMsg: String = ""
    private var lastNotifTime: Long = 0L
    fun triggerNotification(msg: String, context: android.content.Context? = null) {
        val now = System.currentTimeMillis()
        if (msg == lastNotifMsg && (now - lastNotifTime) < 3000L) {
            return
        }
        lastNotifMsg = msg
        lastNotifTime = now
        _toastMessage.value = msg
        val ctx = context ?: appContext
        if (ctx != null) {
            try {
                val channelId = "yemen_services_alerts"
                val nm = ctx.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    val channel = android.app.NotificationChannel(
                        channelId,
                        "إشعارات الخدمة والحجوزات والمحادثات",
                        android.app.NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = "إشعارات التطبيق الفورية"
                        enableVibration(true)
                    }
                    nm?.createNotificationChannel(channel)
                }
                val intent = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)?.apply {
                    flags = android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                val pendingIntent = android.app.PendingIntent.getActivity(
                    ctx, (System.currentTimeMillis() % 1000).toInt(), intent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                )
                val builder = androidx.core.app.NotificationCompat.Builder(ctx, channelId)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle("دليل خدمات اليمن 🔔")
                    .setContentText(msg)
                    .setStyle(androidx.core.app.NotificationCompat.BigTextStyle().bigText(msg))
                    .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                nm?.notify((System.currentTimeMillis() % 10000).toInt(), builder.build())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
fun triggerOpenChatForRequest(requestId: String, customerPhone: String, serviceType: String) {
        val targetPhone = customerPhone.ifBlank { _currentUserPhone.value }
        if (targetPhone.isNotBlank()) {
            getOrCreateChatChannel(
                providerId = "request_$requestId",
                providerName = "صاحب الطلب ($targetPhone)",
                customerId = _currentUserPhone.value.ifBlank { "guest" },
                customerName = _currentUserName.value.ifBlank { "مستخدم الدليل" }
            )
        } else {
            triggerNotification("💬 يمكنك التحدث مع مقدمي العروض عبر شاشة المحادثات")
        }
    }
fun clearNotification() {
        _toastMessage.value = null
    }
fun loadUserPoints() {
        _currentUserPoints.value = (100..500).random()
    }
fun redeemLoyaltyPoints() {
        triggerNotification("🎉 تم استبدال نقاطك بنجاح! تم الخصم بنجاح.")
    }
fun rewardSharePoints() {
        _currentUserPoints.value = _currentUserPoints.value + 20
        triggerNotification("🎁 حصلت على 20 نقطة مشاركة!")
    }
fun clearSmartAssistantChatHistory() {
        _currentUserPoints.value = 0
        triggerNotification("🧹 تم تصفية وحذف سجل المحادثة الذكية بنجاح!")
    }
    override suspend fun uploadImageStringOrUri(
        context: android.content.Context,
        input: String,
        storagePath: String,
        maxSizeBytes: Long
    ): String {
        return registrationHelper.uploadImageStringOrUri(context, input, storagePath, maxSizeBytes)
    }

    fun submitJoinForm(
        context: android.content.Context,
        name: String, phone: String, catId: String, area: String,
        neighborhood: String, photoPath: String, idCardPath: String, gpsCoords: String,
        workPhotos: List<String> = emptyList(),
        customCategoryName: String = "",
        password: String = "",
        productAttachmentsJson: String = ""
    ) {
        registrationHelper.submitJoinForm(
            context = context,
            scope = viewModelScope,
            name = name,
            phone = phone,
            catId = catId,
            area = area,
            neighborhood = neighborhood,
            photoPath = photoPath,
            idCardPath = idCardPath,
            gpsCoords = gpsCoords,
            workPhotos = workPhotos,
            customCategoryName = customCategoryName,
            password = password,
            productAttachmentsJson = productAttachmentsJson,
            checkDuplicate = { checkAndGetDuplicateAccountType(it, "") },
            logAdminActivity = { logAdminActivity(it) },
            triggerNotification = { triggerNotification(it) },
            addApplicantNotification = { title, msg, type, targetVal ->
                addNotification(
                    title = title,
                    message = msg,
                    targetType = type,
                    targetValue = targetVal
                )
            },
            onPendingAdded = { newReq ->
                val currentPending = _pendingProviders.value.filter { it.id != newReq.id }.toMutableList()
                currentPending.add(newReq)
                _pendingProviders.value = currentPending
                val currentTechs = _pendingTechnicians.value.filter { it.id != newReq.id }.toMutableList()
                currentTechs.add(newReq)
                _pendingTechnicians.value = currentTechs
            },
            onStoreAdded = { newStore ->
                val sList = _stores.value.toMutableList()
                sList.removeAll { it.id == newStore.id }
                sList.add(newStore)
                _stores.value = sList
            },
            onPropertyAdded = { newProp ->
                val pList = _properties.value.toMutableList()
                pList.removeAll { it.id == newProp.id }
                pList.add(newProp)
                _properties.value = pList
            },
            onJobAdded = { newJob ->
                val jList = _jobs.value.toMutableList()
                jList.removeAll { it.id == newJob.id }
                jList.add(newJob)
                _jobs.value = jList
            },
            onClientAdded = { userMap ->
                val uList = _registeredUsersList.value.toMutableList()
                uList.removeAll { it["phone"] == userMap["phone"] }
                uList.add(userMap)
                _registeredUsersList.value = uList
            },
            onJoinRequestPhoneUpdated = { _joinRequestPhone.value = it },
            onNavigateToScreen = { _currentScreen.value = it }
        )
    }

    fun registerClientUser(name: String, phone: String, residence: String, password: String = "") {
        registrationHelper.registerClientUser(name, phone, residence, password) { userMap ->
            val uList = _registeredUsersList.value.toMutableList()
            uList.removeAll { it["phone"] == userMap["phone"] }
            uList.add(userMap)
            _registeredUsersList.value = uList
        }
    }

    fun cancelOrResetJoinRequest(context: android.content.Context) {
        registrationHelper.cancelOrResetJoinRequest(
            context = context,
            phone = _joinRequestPhone.value,
            pendingProviders = _pendingProviders.value,
            onPendingRemoved = { id ->
                _pendingProviders.value = _pendingProviders.value.filter { it.id != id }
            },
            onPhoneCleared = { _joinRequestPhone.value = "" },
            onGoBack = { goBack() }
        )
    }

    fun setJoinRequestPhone(context: android.content.Context, phone: String) {
        registrationHelper.setJoinRequestPhone(context, phone) {
            _joinRequestPhone.value = it
        }
    }

fun addNotification(
        title: String,
        message: String,
        targetType: String = "ALL",
        targetValue: String = "",
        targetAudience: String = "ALL",
        targetRoles: List<String> = emptyList(),
        targetUserIds: List<String> = emptyList(),
        senderId: String = "SYSTEM",
        senderName: String = "النظام",
        dedupKey: String = "",
        expiryTimestamp: Long = 0L,
        scheduledTime: Long = 0L,
        customerPhone: String = "",
        customerName: String = "",
        notificationType: String = "NORMAL",
        channel: String = "IN_APP"
    ) {
        // 1. Strict Validation Check: Reject empty or dummy notifications
        if (title.trim().isEmpty() || message.trim().isEmpty()) {
            return
        }
        val providerByPhone = _providers.value.find { it.phone.trim() == targetValue.trim() }
        val providerById = _providers.value.find { it.id == targetValue }
        val isNotifDisabled = (providerByPhone?.isNotificationsDisabled == true) || (providerById?.isNotificationsDisabled == true)
        if (isNotifDisabled) {
            triggerNotification("⚠️ تم حجب إرسال هذا الإشعار لأن الإدارة قامت بتعطيل إشعارات الفني: ${providerByPhone?.name ?: providerById?.name ?: ""}")
            return
        }
        val finalDedupKey = if (dedupKey.isNotBlank()) dedupKey else "${notificationType}_${targetValue}_${title}_${System.currentTimeMillis() / (30 * 1000L)}"
        // 2. Prevent duplicate notifications
        val isDuplicate = _notifications.value.any { it.dedupKey == finalDedupKey || (it.title == title && it.targetValue == targetValue && Math.abs(it.timestamp - System.currentTimeMillis()) < 15000L) }
        if (isDuplicate) {
            return
        }
        val newNotif = NotificationEntity(
            id = "n_" + UUID.randomUUID().toString().take(8),
            title = title.trim(),
            message = message.trim(),
            targetType = targetType,
            targetValue = targetValue,
            targetAudience = targetAudience,
            targetRoles = targetRoles,
            targetUserIds = targetUserIds,
            senderId = senderId,
            senderName = senderName,
            dedupKey = finalDedupKey,
            timestamp = System.currentTimeMillis(),
            expiryTimestamp = expiryTimestamp,
            scheduledTime = scheduledTime,
            customerPhone = customerPhone,
            customerName = customerName,
            notificationType = notificationType,
            channel = channel
        )
        // Optimistic instant state update
        _notifications.value = listOf(newNotif) + _notifications.value.filter { it.id != newNotif.id }
        try {
            db.collection("notifications").document(newNotif.id).set(newNotif)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        triggerNotification("🔔 تم إرسال الإشعار الموثوق بنجاح!")
    }
    // =------------------- Delegated Functions -------------------=
    fun approveRequest(request: PendingProviderEntity) = adminViewModel.approveRequest(request)
    fun rejectRequest(request: PendingProviderEntity, reason: String) = adminViewModel.rejectRequest(request, reason)
    fun approveTechnician(providerId: String) = adminViewModel.approveTechnician(providerId)
    fun loadPendingTechnicians() = adminViewModel.loadPendingTechnicians()
    fun approvePendingProvider(pending: PendingProviderEntity) = adminViewModel.approvePendingProvider(pending)
    fun saveStore(store: com.example.data.StoreEntity) = adminViewModel.saveStore(store)
    fun deleteStore(storeId: String) = adminViewModel.deleteStore(storeId)
    fun restoreStore(storeId: String) = adminViewModel.restoreStore(storeId)
    fun deleteStorePermanently(storeId: String) = adminViewModel.deleteStorePermanently(storeId)
    fun setStoreActive(storeId: String, isActive: Boolean) = adminViewModel.setStoreActive(storeId, isActive)
    fun setStorePinned(storeId: String, isPinned: Boolean) = adminViewModel.setStorePinned(storeId, isPinned)
    fun setStoreVip(storeId: String, isVip: Boolean) = adminViewModel.setStoreVip(storeId, isVip)
    fun setStoreVerified(storeId: String, isVerified: Boolean) = adminViewModel.setStoreVerified(storeId, isVerified)
    fun setStoreRecommended(storeId: String, isRecommended: Boolean) = adminViewModel.setStoreRecommended(storeId, isRecommended)
    fun setStoreChatDisabled(storeId: String, isDisabled: Boolean) = adminViewModel.setStoreChatDisabled(storeId, isDisabled)
    fun setStoreNotificationsDisabled(storeId: String, isDisabled: Boolean) = adminViewModel.setStoreNotificationsDisabled(storeId, isDisabled)
    fun setStorePaymentEnabled(storeId: String, isEnabled: Boolean) = adminViewModel.setStorePaymentEnabled(storeId, isEnabled)
    fun toggleStoreBlocked(storeId: String, isBlocked: Boolean) = adminViewModel.toggleStoreBlocked(storeId, isBlocked)
    fun toggleStoreActive(storeId: String) = adminViewModel.toggleStoreActive(storeId)
    fun toggleStorePinned(storeId: String) = adminViewModel.toggleStorePinned(storeId)
    fun toggleStoreChatDisabled(storeId: String) = adminViewModel.toggleStoreChatDisabled(storeId)
    fun approveStorePdf(storeId: String, approve: Boolean) = adminViewModel.approveStorePdf(storeId, approve)
    fun saveProperty(property: com.example.data.PropertyEntity) = adminViewModel.saveProperty(property)
    fun deleteProperty(propertyId: String) = adminViewModel.deleteProperty(propertyId)
    fun restoreProperty(propertyId: String) = adminViewModel.restoreProperty(propertyId)
    fun deletePropertyPermanently(propertyId: String) = adminViewModel.deletePropertyPermanently(propertyId)
    fun setPropertyActive(propertyId: String, isActive: Boolean) = adminViewModel.setPropertyActive(propertyId, isActive)
    fun setPropertyPinned(propertyId: String, isPinned: Boolean) = adminViewModel.setPropertyPinned(propertyId, isPinned)
    fun setPropertyVip(propertyId: String, isVip: Boolean) = adminViewModel.setPropertyVip(propertyId, isVip)
    fun setPropertyVerified(propertyId: String, isVerified: Boolean) = adminViewModel.setPropertyVerified(propertyId, isVerified)
    fun setPropertyRecommended(propertyId: String, isRecommended: Boolean) = adminViewModel.setPropertyRecommended(propertyId, isRecommended)
    fun setPropertyChatDisabled(propertyId: String, isDisabled: Boolean) = adminViewModel.setPropertyChatDisabled(propertyId, isDisabled)
    fun setPropertyNotificationsDisabled(propertyId: String, isDisabled: Boolean) = adminViewModel.setPropertyNotificationsDisabled(propertyId, isDisabled)
    fun setPropertyPaymentEnabled(propertyId: String, isEnabled: Boolean) = adminViewModel.setPropertyPaymentEnabled(propertyId, isEnabled)
    fun togglePropertyBlocked(propertyId: String, isBlocked: Boolean) = adminViewModel.togglePropertyBlocked(propertyId, isBlocked)
    fun approvePropertyPdf(propertyId: String, approve: Boolean) = adminViewModel.approvePropertyPdf(propertyId, approve)
    fun saveJob(job: com.example.data.JobEntity) = adminViewModel.saveJob(job)
    fun deleteJob(jobId: String) = adminViewModel.deleteJob(jobId)
    fun restoreJob(jobId: String) = adminViewModel.restoreJob(jobId)
    fun deleteJobPermanently(jobId: String) = adminViewModel.deleteJobPermanently(jobId)
    fun setJobApproved(jobId: String, isApproved: Boolean) = adminViewModel.setJobApproved(jobId, isApproved)
    fun setJobPinned(jobId: String, isPinned: Boolean) = adminViewModel.setJobPinned(jobId, isPinned)
    fun setJobVip(jobId: String, isVip: Boolean) = adminViewModel.setJobVip(jobId, isVip)
    fun setJobChatDisabled(jobId: String, isDisabled: Boolean) = adminViewModel.setJobChatDisabled(jobId, isDisabled)
    fun submitJobApplication(application: com.example.data.JobApplicationEntity) = adminViewModel.submitJobApplication(application)
    fun updateJobApplicationStatus(appId: String, status: String) = adminViewModel.updateJobApplicationStatus(appId, status)
    fun acceptJobApplication(appId: String) = adminViewModel.acceptJobApplication(appId)
    fun rejectJobApplication(appId: String, reason: String) = adminViewModel.rejectJobApplication(appId, reason)
    fun deleteJobApplication(appId: String) = adminViewModel.deleteJobApplication(appId)
    fun deleteReport(reportId: String) = adminViewModel.deleteReport(reportId)
    fun sendReport(providerId: String, providerName: String, reporterName: String, content: String) = adminViewModel.sendReport(providerId, providerName, reporterName, content)
    fun saveCoupon(coupon: CouponEntity) = adminViewModel.saveCoupon(coupon)
    fun deleteCoupon(couponId: String) = adminViewModel.deleteCoupon(couponId)
    fun saveInternalWallet(wallet: com.example.data.InternalWalletEntity) = adminViewModel.saveInternalWallet(wallet)
    fun performWalletTransaction(
        walletId: String,
        ownerName: String,
        ownerPhone: String,
        ownerType: String,
        type: String, // DEPOSIT, WITHDRAWAL, TRANSFER
        amount: Double,
        note: String
    ) = adminViewModel.performWalletTransaction(walletId, ownerName, ownerPhone, ownerType, type, amount, note)
    fun addPaymentWallet(wallet: PaymentWalletEntity) = adminViewModel.addPaymentWallet(wallet)
    fun updatePaymentWallet(wallet: PaymentWalletEntity) = adminViewModel.updatePaymentWallet(wallet)
    fun deletePaymentWallet(walletId: String) = adminViewModel.deletePaymentWallet(walletId)
    fun togglePaymentWalletVisibility(walletId: String, currentVisible: Boolean) = adminViewModel.togglePaymentWalletVisibility(walletId, currentVisible)
    fun confirmPayment(
        paymentId: String,
        transferId: String,
        transferPhoto: String,
        walletProvider: String,
        walletNumber: String,
        walletAccountName: String
    ) = adminViewModel.confirmPayment(paymentId, transferId, transferPhoto, walletProvider, walletNumber, walletAccountName)
    fun verifyPayment(paymentId: String, isVerified: Boolean, note: String, adminName: String) = adminViewModel.verifyPayment(paymentId, isVerified, note, adminName)
    fun refundPayment(paymentId: String, reason: String) = adminViewModel.refundPayment(paymentId, reason)
    fun saveProduct(product: com.example.data.ProductEntity) = adminViewModel.saveProduct(product)
    fun deleteProduct(productId: String) = adminViewModel.deleteProduct(productId)
    fun updateProductPrice(productId: String, newPrice: Double) = adminViewModel.updateProductPrice(productId, newPrice)
    fun saveOffer(offer: com.example.data.models.Offer) = adminViewModel.saveOffer(offer)
    fun deleteOffer(offerId: String) = adminViewModel.deleteOffer(offerId)
    fun toggleOfferStatus(offerId: String, isActive: Boolean) = adminViewModel.toggleOfferStatus(offerId, isActive)
    fun listenToOffersForEntity(
        entityId: String,
        onResult: (List<com.example.data.models.Offer>) -> Unit
    ): com.google.firebase.firestore.ListenerRegistration = adminViewModel.listenToOffersForEntity(entityId, onResult)
    fun listenToProductsForStore(
        storeId: String,
        onResult: (List<com.example.data.ProductEntity>) -> Unit
    ): com.google.firebase.firestore.ListenerRegistration = adminViewModel.listenToProductsForStore(storeId, onResult)
    fun saveCustomProfileTab(tab: com.example.data.CustomProfileTabEntity) = adminViewModel.saveCustomProfileTab(tab)
    fun deleteCustomProfileTab(tabId: String) = adminViewModel.deleteCustomProfileTab(tabId)
    fun toggleCustomProfileTab(tabId: String) = adminViewModel.toggleCustomProfileTab(tabId)
    fun deleteCategory(categoryId: String) = adminViewModel.deleteCategory(categoryId)
    fun togglePinCategory(categoryId: String) = adminViewModel.togglePinCategory(categoryId)
    fun mergeCategories(sourceCategoryId: String, targetCategoryId: String) = adminViewModel.mergeCategories(sourceCategoryId, targetCategoryId)
    fun saveCategoryEntity(cat: CategoryEntity) = adminViewModel.saveCategoryEntity(cat)
    fun addSubCategory(parentId: String, nameAr: String, icon: String) = adminViewModel.addSubCategory(parentId, nameAr, icon)
    fun convertCategoryType(catId: String, newParentId: String, isMain: Boolean) = adminViewModel.convertCategoryType(catId, newParentId, isMain)
    fun reorderCategories(newOrderedList: List<CategoryEntity>) = adminViewModel.reorderCategories(newOrderedList)
    fun updateCity(city: CityEntity) = adminViewModel.updateCity(city)
    fun removeCity(cityId: String) = adminViewModel.removeCity(cityId)
    fun removeProvider(providerId: String) = adminViewModel.removeProvider(providerId)
    fun removeProviderPermanently(providerId: String) = adminViewModel.removeProviderPermanently(providerId)
    fun restoreProvider(providerId: String) = adminViewModel.restoreProvider(providerId)
    fun pinProvider(providerId: String, isPinned: Boolean) = adminViewModel.pinProvider(providerId, isPinned)
    fun recommendProvider(providerId: String, isRecommended: Boolean) = adminViewModel.recommendProvider(providerId, isRecommended)
    fun verifyProviderBadge(providerId: String, isVerified: Boolean) = adminViewModel.verifyProviderBadge(providerId, isVerified)
    fun toggleProviderSubscription(providerId: String, status: String) = adminViewModel.toggleProviderSubscription(providerId, status)
    fun setProviderChatDisabled(providerId: String, disabled: Boolean) = adminViewModel.setProviderChatDisabled(providerId, disabled)
    fun setProviderNotificationsDisabled(providerId: String, disabled: Boolean) = adminViewModel.setProviderNotificationsDisabled(providerId, disabled)
    fun setProviderPaymentRequired(providerId: String, required: Boolean) = adminViewModel.setProviderPaymentRequired(providerId, required)
    fun extendProviderSubscription(providerId: String, extraMs: Long) = adminViewModel.extendProviderSubscription(providerId, extraMs)
    fun toggleProviderBlock(providerId: String) = adminViewModel.toggleProviderBlock(providerId)
    fun toggleProviderStatus(provider: ProviderEntity) = adminViewModel.toggleProviderStatus(provider)
    fun toggleProviderPin(providerId: String) = adminViewModel.toggleProviderPin(providerId)
    fun toggleProviderVerification(providerId: String) = adminViewModel.toggleProviderVerification(providerId)
    fun toggleProviderRecommendation(providerId: String) = adminViewModel.toggleProviderRecommendation(providerId)
    fun updateProviderEntity(provider: ProviderEntity) = adminViewModel.updateProviderEntity(provider)
    fun updateStoreEntity(store: StoreEntity) {
        db.collection("stores").document(store.id).set(store)
        triggerNotification("✅ تم تحديث بيانات المتجر بنجاح")
    }
    fun updatePropertyEntity(property: PropertyEntity) {
        db.collection("properties").document(property.id).set(property)
        triggerNotification("✅ تم تحديث بيانات العقار بنجاح")
    }
    fun updateBusinessAccountStatus(accountId: String, isActive: Boolean) {
        viewModelScope.launch {
            try {
                db.collection("stores").document(accountId).update("isActive", isActive)
            } catch (e: Exception) {}
            try {
                db.collection("providers").document(accountId).update("isAvailable", isActive)
            } catch (e: Exception) {}
        }
    }
    fun updateEntityImages(collection: String, id: String, profileImg: String, coverImg: String) {
        val updates = mutableMapOf<String, Any>()
        if (profileImg.isNotBlank()) {
            updates["profileImage"] = profileImg
            updates["logoImage"] = profileImg
        }
        if (coverImg.isNotBlank()) {
            updates["coverImage"] = coverImg
        }
        if (updates.isNotEmpty()) {
            db.collection(collection).document(id).update(updates)
            triggerNotification("📸 تم تحديث الصور بنجاح")
        }
    }
    fun editProviderPhoneAndCategory(providerId: String, newPhone: String, newCategoryId: String) = adminViewModel.editProviderPhoneAndCategory(providerId, newPhone, newCategoryId)
    fun addNewProvider(name: String, phone: String, catId: String, area: String, price: Double, isVip: Boolean) = adminViewModel.addNewProvider(name, phone, catId, area, price, isVip)
    fun addNewProviderCustom(
        name: String,
        phone: String,
        catId: String,
        street: String,
        cityId: String,
        profileImage: String,
        idCardImage: String,
        forensicImage: String,
        price: Double,
        isVip: Boolean
    ) = adminViewModel.addNewProviderCustom(name, phone, catId, street, cityId, profileImage, idCardImage, forensicImage, price, isVip)
    fun deleteBanner(bannerId: String) = adminViewModel.deleteBanner(bannerId)
    fun reorderBanners(newOrderedList: List<BannerEntity>) = adminViewModel.reorderBanners(newOrderedList)
    fun placeOrder(order: com.example.data.OrderEntity) = adminViewModel.placeOrder(order)
    fun updateOrderStatus(orderId: String, status: String) = adminViewModel.updateOrderStatus(orderId, status)
    fun deleteOrder(orderId: String) = adminViewModel.deleteOrder(orderId)
    fun deleteAllOrders(customerPhone: String) = adminViewModel.deleteAllOrders(customerPhone)
    fun addRating(rating: com.example.data.RatingEntity) = adminViewModel.addRating(rating)
    fun addRatingReply(ratingId: String, replyText: String) = adminViewModel.addRatingReply(ratingId, replyText)
    fun deleteRating(ratingId: String) = adminViewModel.deleteRating(ratingId)
    fun approveRating(ratingId: String, isApproved: Boolean) = adminViewModel.approveRating(ratingId, isApproved)
    fun submitRating(providerId: String, rating: Int) = adminViewModel.submitRating(providerId, rating)
    fun recalculateTargetRating(targetId: String, targetType: String) = adminViewModel.recalculateTargetRating(targetId, targetType)
    fun logAdminActivity(action: String) = adminViewModel.logAdminActivity(action)
    fun logCall(providerId: String, providerName: String) = adminViewModel.logCall(providerId, providerName)
    fun checkAndGetDuplicateAccountType(phone: String, excludeId: String): String? = adminViewModel.checkAndGetDuplicateAccountType(phone, excludeId)
    fun updateProviderPortfolio(providerId: String, images: List<String>) = adminViewModel.updateProviderPortfolio(providerId, images)
    fun addPortfolioImage(providerId: String, imageBase64: String) = adminViewModel.addPortfolioImage(providerId, imageBase64)
    fun removePortfolioImage(providerId: String, index: Int) = adminViewModel.removePortfolioImage(providerId, index)
    fun clearPortfolio(providerId: String) = adminViewModel.clearPortfolio(providerId)
    fun redirectBookingToEntity(bookingId: String, targetEntityId: String, targetEntityName: String, targetPhone: String) = adminViewModel.redirectBookingToEntity(bookingId, targetEntityId, targetEntityName, targetPhone)
    fun unbanEntity(entityType: String, entityId: String) = adminViewModel.unbanEntity(entityType, entityId)
    fun restoreEntity(entityType: String, entityId: String) = adminViewModel.restoreEntity(entityType, entityId)
    fun hardDeleteEntity(entityType: String, entityId: String) = adminViewModel.hardDeleteEntity(entityType, entityId)
    fun exportJobApplicantsCsv(context: android.content.Context) = adminViewModel.exportJobApplicantsCsv(context)
    fun loadCardSettings() = settingsViewModel.loadCardSettings()
    fun updateCardSettings(settings: com.example.ui.viewmodels.SettingsViewModel.CardSettings) = settingsViewModel.updateCardSettings(settings)
    fun updateTheme(themeId: String) = settingsViewModel.updateTheme(themeId)
    fun saveCustomSettingsState(newSettings: AdminSettingsEntity) = settingsViewModel.saveCustomSettingsState(newSettings)
    fun updateAdminSettings(newSettings: AdminSettingsEntity) = settingsViewModel.updateAdminSettings(newSettings)
    fun initColorSync(context: android.content.Context) = settingsViewModel.initColorSync(context)
    fun updateCloudColorScheme(context: android.content.Context, newScheme: com.example.data.ColorSchemeEntity) = settingsViewModel.updateCloudColorScheme(context, newScheme)
    fun updatePersonalColors(context: android.content.Context, personal: com.example.data.PersonalColors) = settingsViewModel.updatePersonalColors(context, personal)
    fun triggerManualSync(context: android.content.Context) = settingsViewModel.triggerManualSync(context)
    fun resolveConflict(context: android.content.Context, useCloud: Boolean) = settingsViewModel.resolveConflict(context, useCloud)
    fun getCurrentTimestampString(): String = settingsViewModel.getCurrentTimestampString()
    fun addNewSyncLog(
        context: android.content.Context,
        type: String,
        status: String,
        changes: List<String>,
        versionFrom: Int,
        versionTo: Int
    ) = settingsViewModel.addNewSyncLog(context, type, status, changes, versionFrom, versionTo)
    fun toggleChatParticipant(participantType: ChatParticipantType) = settingsViewModel.toggleChatParticipant(participantType)
    fun isChatBlockedFor(participantType: ChatParticipantType): Boolean = settingsViewModel.isChatBlockedFor(participantType)
    fun canParticipateInChat(participantType: ChatParticipantType): Boolean = settingsViewModel.canParticipateInChat(participantType)
    fun startVoiceCall(name: String, role: String) = settingsViewModel.startVoiceCall(name, role)
    fun endVoiceCall() = settingsViewModel.endVoiceCall()
    fun exportComplaintsToCSV() = settingsViewModel.exportComplaintsToCSV()
    fun exportComplaintsToPDF() = settingsViewModel.exportComplaintsToPDF()
    fun exportPerformanceReportToPDF() = settingsViewModel.exportPerformanceReportToPDF()
    fun createSystemBackup(onComplete: (Boolean, String) -> Unit) = settingsViewModel.createSystemBackup(onComplete)
    fun restoreSystemFromBackup(jsonStr: String, onComplete: (Boolean, String) -> Unit) = settingsViewModel.restoreSystemFromBackup(jsonStr, onComplete)
    fun exportSelectedCollectionsAsJson(selectedCollections: List<String>, onResult: (String) -> Unit) = settingsViewModel.exportSelectedCollectionsAsJson(selectedCollections, onResult)
    fun saveBackupToLocalStorage(context: android.content.Context, jsonStr: String, fileName: String): String = settingsViewModel.saveBackupToLocalStorage(context, jsonStr, fileName)
    fun setSecondaryFirebaseConfig(projectId: String, apiKey: String, appId: String, storageBucket: String, isEnabled: Boolean) = settingsViewModel.setSecondaryFirebaseConfig(projectId, apiKey, appId, storageBucket, isEnabled)
    fun saveCustomPermissionsMatrixToFirestore(permissions: List<String>) = settingsViewModel.saveCustomPermissionsMatrixToFirestore(permissions)
    fun deleteColorPalette(id: String) = settingsViewModel.deleteColorPalette(id)
    fun resetAccountPassword(entityType: String, phoneOrId: String, newPass: String) = settingsViewModel.resetAccountPassword(entityType, phoneOrId, newPass)
    fun requestAdminPasswordReset(phone: String) = settingsViewModel.requestAdminPasswordReset(phone)
    fun requestPasswordReset(phone: String, onResult: (Boolean, String) -> Unit) = settingsViewModel.requestPasswordReset(phone, onResult)
    fun approvePasswordReset(phone: String, onResult: (Boolean, String) -> Unit) = settingsViewModel.approvePasswordReset(phone, onResult)
    fun adminResetAccountPassword(phone: String, newPassword: String, notifyAction: String, customerName: String) = settingsViewModel.adminResetAccountPassword(phone, newPassword, notifyAction, customerName)
    fun requestPasswordRecoveryForStore(name: String, phone: String, password: String) = settingsViewModel.requestPasswordRecoveryForStore(name, phone, password)
    fun requestPasswordRecoveryForProperty(title: String, phone: String, password: String) = settingsViewModel.requestPasswordRecoveryForProperty(title, phone, password)
    fun requestPasswordRecoveryGeneral(accountName: String, phone: String, accountType: String, currentPassword: String) = settingsViewModel.requestPasswordRecoveryGeneral(accountName, phone, accountType, currentPassword)
    fun wipeAllDatabaseData(password: String): Boolean = settingsViewModel.wipeAllDatabaseData(password)
    fun wipeSelectedDatabaseData(password: String, selectedCollections: List<String>): Boolean = settingsViewModel.wipeSelectedDatabaseData(password, selectedCollections)
    fun wipeAllMockAndTemporaryData() = settingsViewModel.wipeAllMockAndTemporaryData()
    fun acceptRequestOffer(
        req: com.example.data.models.InstantRequestEntity,
        offer: com.example.data.models.RequestOfferEntity
    ) = instantRequestViewModel.acceptRequestOffer(req, offer)
    fun completeInstantRequest(requestId: String) = instantRequestViewModel.completeInstantRequest(requestId)
    fun setPasswordRecoveryWaitingPhone(phone: String) = authViewModel.setPasswordRecoveryWaitingPhone(phone)
    fun resetRegistrationState() = authViewModel.resetRegistrationState()
    data class RestoreAccountMatch(
        val type: String, // "PROVIDER", "STORE", "PROPERTY", "CLIENT"
        val name: String,
        val provider: ProviderEntity? = null,
        val store: StoreEntity? = null,
        val property: PropertyEntity? = null,
        val savedPassword: String = ""
    )

    fun searchAccountForRestore(cleanPhone: String, onResult: (RestoreAccountMatch?) -> Unit) {
        accountRecoveryHelper.searchAccountForRestore(cleanPhone, onResult)
    }

    fun requestPasswordReset(
        context: android.content.Context,
        phone: String,
        name: String,
        accountType: String,
        onResult: (Boolean) -> Unit
    ) {
        accountRecoveryHelper.requestPasswordReset(
            context = context,
            phone = phone,
            name = name,
            accountType = accountType,
            onPasswordWaitingPhoneSet = { setPasswordRecoveryWaitingPhone(it) },
            triggerNotification = { triggerNotification(it) },
            onResult = onResult
        )
    }

    fun adminResolvePasswordReset(
        context: android.content.Context,
        phone: String,
        newPassword: String,
        onResult: (Boolean) -> Unit
    ) {
        accountRecoveryHelper.adminResolvePasswordReset(
            context = context,
            phone = phone,
            newPassword = newPassword,
            onResult = onResult
        )
    }

    fun isUserLoggedIn(context: android.content.Context): Boolean {
        val isLoggedIn = preferenceHelper.isAccountLoggedIn(context)
        val phone = currentUserPhone.value
        return isLoggedIn || (phone.isNotBlank() && currentUserId.value != "guest" && currentUserId.value.isNotBlank())
    }
    fun restoreUserAccountByPhoneAndPassword(
        context: android.content.Context,
        phone: String,
        password: String,
        onResult: (Boolean, String) -> Unit
    ) {
        val email = getAuthEmailForPhone(phone)
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val userId = authResult.user?.uid ?: ""
                db.collection("users").document(userId).update("isDeleted", false)
                db.collection("providers").document(phone).update("isDeleted", false)
                db.collection("stores").document(phone).update("isDeleted", false)
                db.collection("properties").document(phone).update("isDeleted", false)
                onResult(true, "تم استعادة الحساب بنجاح!")
            }
            .addOnFailureListener { e ->
                onResult(false, e.message ?: "فشل في تسجيل الدخول")
            }
    }
    fun restoreGuestUser(context: android.content.Context, phone: String, password: String, onResult: (Boolean, String) -> Unit) {
        restoreUserAccountByPhoneAndPassword(context, phone, password, onResult)
    }
    fun loginUserDirectly(context: android.content.Context, phone: String) = authViewModel.loginUserDirectly(context, phone)
    override fun getAuthEmailForPhone(phone: String): String = authViewModel.getAuthEmailForPhone(phone)
    fun showBackdoorDialog() = authViewModel.showBackdoorDialog()
    fun dismissBackdoorDialog() = authViewModel.dismissBackdoorDialog()
    fun setSupervisorSession(sup: SupervisorEntity) = authViewModel.setSupervisorSession(sup)
    fun hasAdminPermission(permissionKey: String): Boolean = authViewModel.hasAdminPermission(permissionKey)
    fun updateSupervisorPermissions(id: String, permissions: List<String>) = authViewModel.updateSupervisorPermissions(id, permissions)
    fun removeSupervisor(id: String) = authViewModel.removeSupervisor(id)
    fun getOrCreateChatChannel(providerId: String, providerName: String, customerId: String, customerName: String) {
        viewModelScope.launch {
            chatRepo.getOrCreateChannel(
                currentUserId = providerId,
                currentUserName = providerName,
                currentUserPhoto = "",
                otherUserId = customerId,
                otherUserName = customerName,
                otherUserPhoto = "",
                type = com.example.data.models.ChannelType.PRIVATE,
                relatedEntityId = null,
                relatedEntityType = null
            )
        }
    }
    fun deleteChatChannel(channelId: String) {
        viewModelScope.launch {
            chatRepo.deleteChannel(channelId)
            triggerToast("تم حذف المحادثة")
        }
    }
    fun toggleBlockChatChannel(channelId: String) {
        viewModelScope.launch {
            val ch = _chatChannels.value.find { it.id == channelId } ?: return@launch
            blockChatChannel(channelId, !ch.isBlocked)
        }
    }
    fun blockChatChannel(channelId: String, blocked: Boolean) {
        db.collection("chat_channels").document(channelId).update("isBlocked", blocked)
        _activeChatChannel.value = _activeChatChannel.value?.copy(isBlocked = blocked)
    }
    fun wipeOldChatChannels(days: Int) {
        triggerToast("🧹 تم تصفية وحذف سجل المحادثات الأقدم من $days أيام بنجاح!")
    }
    fun updateBookingFormFields(fields: BookingFormFields) = bookingViewModel.updateBookingFormFields(fields)
    fun updateDistributionMode(mode: BookingDistributionMode) = bookingViewModel.updateDistributionMode(mode)
    fun cancelBookingByUser(bookingId: String) = bookingViewModel.cancelBookingByUser(bookingId)
    fun getBookingStatusColor(status: String): String = bookingViewModel.getBookingStatusColor(status)
    fun getBookingStatusLabel(status: String): String = bookingViewModel.getBookingStatusLabel(status)
    fun getBookingProgress(status: String): Float = bookingViewModel.getBookingProgress(status)
    fun markNotificationAsRead(context: android.content.Context, notifId: String) {
        val currentRead = _readNotificationIds.value.toMutableSet()
        val updated = preferenceHelper.markNotificationAsRead(context, notifId, currentRead)
        _readNotificationIds.value = updated
    }
    fun loadReadNotifications(context: android.content.Context) {
        _readNotificationIds.value = preferenceHelper.getReadNotificationIds(context)
    }
    fun markAllNotificationsAsRead(context: android.content.Context) {
        val allIds = _notifications.value.map { it.id }.toSet()
        preferenceHelper.markAllNotificationsAsRead(context, allIds)
        _readNotificationIds.value = allIds
    }
    fun deleteNotification(notifId: String) {
        db.collection("notifications").document(notifId).delete()
        _notifications.value = _notifications.value.filter { it.id != notifId }
        val currentRead = _readNotificationIds.value.toMutableSet()
        currentRead.remove(notifId)
        _readNotificationIds.value = currentRead
    }
    fun deleteAllNotifications() {
        val allNotifs = _notifications.value
        _notifications.value = emptyList()
        _readNotificationIds.value = emptySet()
        allNotifs.forEach { notif ->
            db.collection("notifications").document(notif.id).delete()
        }
    }
    fun clearAllNotifications() {
        deleteAllNotifications()
    }
    // Additional Delegations & Helpers
    val isProviderUser: Boolean get() = adminRole.value == "PROVIDER" || adminRole.value == "TECHNICIAN"
    val _currentSupervisorPermissions get() = authViewModel._currentSupervisorPermissions
    val currentSupervisorPermissions get() = authViewModel.currentSupervisorPermissions
    val triggerRestoreAccountDialog = MutableStateFlow(false)
    var targetChatChannelId by mutableStateOf<String?>(null)
    fun openSupportChat() {
        val currentUserId = authViewModel.getOrGenerateUserId()
        val currentUserName = authViewModel.currentUserName.value.ifBlank { "العميل" }
        val currentUserPhoto = ""
        
        viewModelScope.launch {
            val result = chatRepo.getOrCreateChannel(
                currentUserId = currentUserId,
                currentUserName = currentUserName,
                currentUserPhoto = currentUserPhoto,
                otherUserId = com.example.data.repositories.ChatRepository.SUPPORT_ADMIN_ID,
                otherUserName = com.example.data.repositories.ChatRepository.SUPPORT_ADMIN_NAME,
                otherUserPhoto = "",
                type = com.example.data.models.ChannelType.SUPPORT,
                relatedEntityId = null,
                relatedEntityType = null
            )
            if (result is AppResult.Success) {
                targetChatChannelId = result.data.id
                navigateToScreen("CHAT_DIRECT")
            }
        }
    }
    fun openChatChannel(channel: ChatChannelEntity?) {
        if (channel == null) return
        val currentUserId = authViewModel.getOrGenerateUserId()
        val currentUserName = authViewModel.currentUserName.value.ifBlank { "العميل" }
        val currentUserPhoto = ""
        
        val otherUserId = if (channel.customerId == currentUserId) channel.targetId else channel.customerId
        val otherUserName = if (channel.customerId == currentUserId) channel.targetName else channel.customerName
        
        viewModelScope.launch {
            val result = chatRepo.getOrCreateChannel(
                currentUserId = currentUserId,
                currentUserName = currentUserName,
                currentUserPhoto = currentUserPhoto,
                otherUserId = otherUserId,
                otherUserName = otherUserName,
                otherUserPhoto = "",
                type = com.example.data.models.ChannelType.PRIVATE,
                relatedEntityId = null,
                relatedEntityType = null
            )
            if (result is AppResult.Success) {
                targetChatChannelId = result.data.id
                // Note: The caller usually navigates, so we don't navigate here.
            }
        }
    }
    fun verifyAdminOrOwnerPassword(password: String, adminPass: String = "", ownerPass: String = ""): Boolean {
        return authViewModel.verifyAdminOrOwnerPassword(password, adminPass, ownerPass)
    }
    fun setUserSessionDetails(context: android.content.Context, name: String, phone: String, residence: String = "اليمن") {
        authViewModel.setUserSessionDetails(context, name, phone, residence)
    }
    fun registerGuestUser(context: android.content.Context, name: String, phone: String, residence: String, password: String = "") {
        authViewModel.registerGuestUser(context, name, phone, residence, password)
    }
    fun toggleFavorite(id: String) {
        val current = _favoriteIds.value.toMutableSet()
        if (current.contains(id)) {
            current.remove(id)
            triggerNotification("💔 تم إزالة العنصر من المفضلة")
        } else {
            current.add(id)
            triggerNotification("💖 تم إضافة العنصر إلى المفضلة")
        }
        _favoriteIds.value = current
    }
    fun toggleBlockStore(storeId: String) {
        val store = _stores.value.find { it.id == storeId }
        if (store != null) {
            adminViewModel.toggleStoreBlocked(storeId, !store.isBlocked)
        }
    }
    fun addBooking(
        name: String,
        phone: String,
        area: String,
        serviceType: String,
        providerId: String,
        providerName: String,
        dateString: String,
        timeString: String,
        couponCode: String = "",
        pinCode: String = "",
        customBookingId: String = "",
        customPassword: String = ""
    ) = bookingViewModel.addBooking(
        name = name,
        phone = phone,
        area = area,
        serviceType = serviceType,
        providerId = providerId,
        providerName = providerName,
        dateString = dateString,
        timeString = timeString,
        couponCode = couponCode,
        pinCode = pinCode,
        customBookingId = customBookingId,
        customPassword = customPassword
    )
    fun addNewStore(
        name: String,
        phone: String,
        cityId: String,
        localNeighborhood: String,
        categoryId: String,
        coverImage: String,
        workingHours: String
    ) {
        val newStore = StoreEntity(
            id = UUID.randomUUID().toString(),
            name = name,
            phone = phone,
            cityId = cityId,
            localNeighborhood = localNeighborhood,
            categoryId = categoryId,
            coverImage = coverImage,
            workingHours = workingHours,
            isApproved = true,
            createdAt = System.currentTimeMillis()
        )
        adminViewModel.saveStore(newStore)
    }
    fun openOrCreateChatChannel(
        targetId: String,
        targetType: String,
        targetName: String,
        targetPhone: String = "",
        targetCategory: String = "",
        relatedEntityId: String = "",
        relatedEntityType: String = "",
        onCreated: (ChatChannelEntity?) -> Unit
    ) {
        val currentUserId = authViewModel.getOrGenerateUserId()
        val currentUserName = authViewModel.currentUserName.value.ifBlank { "العميل" }
        val currentUserPhoto = ""
        
        viewModelScope.launch {
            val result = chatRepo.getOrCreateChannel(
                currentUserId = currentUserId,
                currentUserName = currentUserName,
                currentUserPhoto = currentUserPhoto,
                otherUserId = targetId,
                otherUserName = targetName,
                otherUserPhoto = "",
                type = com.example.data.models.ChannelType.PRIVATE,
                relatedEntityId = relatedEntityId.takeIf { it.isNotBlank() },
                relatedEntityType = relatedEntityType.takeIf { it.isNotBlank() }
            )
            
            if (result is AppResult.Success) {
                targetChatChannelId = result.data.id
                // We pass a dummy ChatChannelEntity since the callers expect it.
                // Callers only use createdCh.id.
                val dummy = ChatChannelEntity(id = result.data.id)
                onCreated(dummy)
            } else {
                onCreated(null)
            }
        }
    }
    fun sendNotificationToApplicants(title: String, message: String, jobId: String = "") {
        adminViewModel.sendNotificationToApplicants(title, message, jobId)
    }
    fun createInstantRequest(
        userId: String,
        userName: String,
        userPhone: String,
        userCity: String,
        userNeighborhood: String,
        categoryId: String,
        categoryName: String,
        serviceTitle: String,
        description: String,
        images: List<String> = emptyList(),
        urgencyTime: String = "فوراً (خلال 30 دقيقة)",
        deliveryMethod: String = "",
        customPin: String = "",
        onResult: (Boolean, String, String) -> Unit = { _, _, _ -> }
    ) {
        instantRequestViewModel.createInstantRequest(
            userId, userName, userPhone, userCity, userNeighborhood, categoryId, categoryName, serviceTitle, description, images, urgencyTime, deliveryMethod, customPin, onResult
        )
    }
    fun submitOfferForRequest(
        requestId: String,
        requestCode: String,
        technicianId: String,
        technicianName: String,
        technicianPhone: String,
        technicianAvatar: String,
        technicianRating: Float,
        price: Double,
        estimatedArrivalTime: String = "خلال 30 دقيقة",
        estimatedDuration: String = "ساعتان",
        notes: String = ""
    ) {
        instantRequestViewModel.submitOfferForRequest(
            requestId, requestCode, technicianId, technicianName, technicianPhone, technicianAvatar, technicianRating, price, estimatedArrivalTime, estimatedDuration, notes
        )
    }
    fun cancelInstantRequest(requestId: String, userPin: String = "") {
        instantRequestViewModel.cancelInstantRequest(requestId = requestId, userPin = userPin)
    }
    // Additional Delegation and Facade Functions for UI Components
    fun addNewCategory(nameAr: String, nameEn: String, icon: String, description: String, parentId: String = "", isMainCategory: Boolean = true) =
        adminViewModel.addNewCategory(nameAr, nameEn, icon, description, parentId, isMainCategory)
    fun editCategory(categoryId: String, newName: String, newIcon: String, parentId: String = "", isMainCategory: Boolean = true) =
        adminViewModel.editCategory(categoryId, newName, newIcon, parentId, isMainCategory)
    fun setStoreBlocked(storeId: String, isBlocked: Boolean, reason: String = "") =
        adminViewModel.setStoreBlocked(storeId, isBlocked, reason)
    fun setPropertyBlocked(propertyId: String, isBlocked: Boolean, reason: String = "") =
        adminViewModel.setPropertyBlocked(propertyId, isBlocked, reason)
    fun setJobBlocked(jobId: String, isBlocked: Boolean, reason: String = "") =
        adminViewModel.setJobBlocked(jobId, isBlocked, reason)
    fun approveRegisteredUser(userId: String, userName: String = "") =
        adminViewModel.approveRegisteredUser(userId, userName)
    fun toggleBlockRegisteredUser(userId: String, currentBlocked: Boolean, userName: String = "") =
        adminViewModel.toggleBlockRegisteredUser(userId, currentBlocked, userName)
    fun deleteRegisteredUser(userId: String, userName: String = "") =
        adminViewModel.deleteRegisteredUser(userId, userName)
    fun addNewBanner(title: String, url: String, redirect: String, type: String, size: String, duration: Int, displayTime: String = "طوال اليوم") =
        adminViewModel.addNewBanner(title, url, redirect, type, size, duration, displayTime)
    fun addBanner(title: String, url: String, redirect: String, type: String, size: String, duration: Int, displayTime: String = "طوال اليوم") =
        adminViewModel.addBanner(title, url, redirect, type, size, duration, displayTime)
    fun createPayment(
        userId: String,
        providerId: String,
        amount: Double,
        method: String,
        bookingId: String = "",
        isLinkedToBooking: Boolean = false,
        bookingServiceType: String = ""
    ) = adminViewModel.createPayment(userId, providerId, amount, method, bookingId, isLinkedToBooking, bookingServiceType)
    fun updateBookingStatus(bookingId: String, newStatus: String, rejectionReason: String = "") =
        bookingViewModel.updateBookingStatus(bookingId, newStatus, rejectionReason)
    fun updateBookingStatus(bookingId: String, newStatus: BookingStatus) =
        bookingViewModel.updateBookingStatus(bookingId, newStatus)
    fun replyToChatChannel(channelId: String, senderId: String, msgText: String, senderName: String, imageUrl: String = "") {
        if (msgText.trim().isEmpty() && imageUrl.isEmpty()) return
        viewModelScope.launch {
            chatRepo.sendMessage(channelId, senderId, senderName, msgText, if (imageUrl.isNotBlank()) com.example.data.models.MediaType.IMAGE else com.example.data.models.MediaType.TEXT, imageUrl, null, null, null)
            triggerToast("تم إرسال الرد بنجاح")
        }
    }
    fun updateBackdoorSettings(
        appName: String, welcomeMsg: String, footerMsg: String, themeId: String,
        supportPhone: String, supportEmail: String, supportWhatsapp: String,
        isMaintenance: Boolean, hiddenFooter: Boolean, botHidden: Boolean, botSize: Int,
        chatHidden: Boolean, chatSize: Int, radiusKm: Int, isSpeech: Boolean,
        isDataSaver: Boolean, imgQuality: Int,
        bookingTerms: String = "يرجى الالتزام التام بالمواعيد المحددة والتسعيرة المتفق عليها مع الفني.",
        bookingLabelName: String = "الاسم الكامل للعميل",
        bookingLabelPhone: String = "رقم هاتف العميل للتواصل (مثال: 777000111)",
        bookingLabelArea: String = "المنطقة والحي السكني",
        bookingLabelService: String = "تفاصيل ونوع الخدمة المطلوبة",
        adminUsername: String = "mah73646@gmail.com",
        adminPassword: String = "Maher@@--@@736462##",
        customPrimaryHex: String = "#059669",
        customSecondaryHex: String = "#115E59",
        customBackgroundHex: String = "#0A0F0D",
        customSurfaceHex: String = "#121D18"
    ) = settingsViewModel.updateBackdoorSettings(
        appName, welcomeMsg, footerMsg, themeId, supportPhone, supportEmail, supportWhatsapp,
        isMaintenance, hiddenFooter, botHidden, botSize, chatHidden, chatSize, radiusKm, isSpeech,
        isDataSaver, imgQuality, bookingTerms, bookingLabelName, bookingLabelPhone, bookingLabelArea,
        bookingLabelService, adminUsername, adminPassword, customPrimaryHex, customSecondaryHex,
        customBackgroundHex, customSurfaceHex
    )
    fun addSupervisor(name: String, role: String, passcode: String, permissions: List<String> = emptyList()) =
        authViewModel.addSupervisor(name, role, passcode, permissions)
    fun editSupervisor(id: String, name: String, role: String, passcode: String, permissions: List<String> = emptyList()) =
        authViewModel.editSupervisor(id, name, role, passcode, permissions)
    fun addColorPalette(name: String, primaryHex: String, secondaryHex: String, backgroundHex: String = "#0A0F0D", surfaceHex: String = "#121D18") =
        settingsViewModel.addColorPalette(name, primaryHex, secondaryHex, backgroundHex, surfaceHex)
    fun addNewCity(nameAr: String, nameEn: String, icon: String = "📍", photoUrl: String = "", sortOrder: Int = 0) =
        adminViewModel.addNewCity(nameAr, nameEn, icon, photoUrl, sortOrder)
    fun addCoupon(code: String, pointsValue: Int, expiryMs: Long, discountPercentage: Int = 0, maxUsageCount: Int = 100) =
        adminViewModel.addCoupon(code, pointsValue, expiryMs, discountPercentage, maxUsageCount)
    fun rejectTechnician(providerId: String, reason: String = "لم يستوفِ الشروط") =
        adminViewModel.rejectTechnician(providerId, reason)
    fun sendMessageInChat(msgText: String, imageUrl: String = "") {
        if (msgText.trim().isEmpty() && imageUrl.isEmpty()) return
        val currentUserId = authViewModel.getOrGenerateUserId()
        val currentName = authViewModel.currentUserName.value.ifEmpty { "العميل" }
        viewModelScope.launch {
            val result = chatRepo.getOrCreateChannel(
                currentUserId = currentUserId,
                currentUserName = currentName,
                currentUserPhoto = "",
                otherUserId = "ADMIN",
                otherUserName = "الدعم الفني",
                otherUserPhoto = "",
                type = com.example.data.models.ChannelType.SUPPORT
            )
            if (result is AppResult.Success) {
                chatRepo.sendMessage(result.data.id, currentUserId, currentName, msgText, if (imageUrl.isNotBlank()) com.example.data.models.MediaType.IMAGE else com.example.data.models.MediaType.TEXT, imageUrl, null, null, null)
                addNotification(
                    "💬 رسالة جديدة في الدعم الفني المباشر",
                    "من العميل $currentName: ${msgText.ifEmpty { "📷 [صورة]" }}",
                    "SUPERVISOR",
                    currentUserId
                )
            }
        }
    }
    fun submitReport(report: com.example.data.ReportEntity, onComplete: () -> Unit = {}) =
        adminViewModel.submitReport(report, onComplete)
    fun deleteBooking(bookingId: String) =
        bookingViewModel.deleteBooking(bookingId)
    fun updateBooking(booking: com.example.data.BookingEntity) =
        bookingViewModel.updateBooking(booking)
    fun submitRating(ratingEntity: com.example.data.RatingEntity, onComplete: () -> Unit = {}) =
        adminViewModel.submitRating(ratingEntity, onComplete)
}
