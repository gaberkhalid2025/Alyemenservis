
package com.example.ui
import androidx.compose.runtime.getValue
import com.example.ui.*
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

    val preferenceHelper = com.example.ui.helpers.AppPreferenceHelper()
    val notificationRepository = com.example.data.repositories.NotificationRepository()
    val firestoreSeedHelper by lazy { com.example.ui.helpers.FirestoreSeedHelper(db) }
    val realtimeSyncHelper by lazy { com.example.ui.helpers.RealtimeSyncHelper(db) }
    val registrationHelper by lazy { com.example.ui.helpers.RegistrationHelper(db, auth, preferenceHelper) }
    val accountRecoveryHelper by lazy { com.example.ui.helpers.AccountRecoveryHelper(db, preferenceHelper) }

    override fun onCleared() {
        super.onCleared()
        try {
            realtimeSyncHelper.clearListeners()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
    internal val _currentLanguage = MutableStateFlow("ar")
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()
    internal val _favoriteIds = MutableStateFlow<Set<String>>(emptySet())
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
    internal val _targetRegistrationType = MutableStateFlow<String?>(null)
    val targetRegistrationType: StateFlow<String?> = _targetRegistrationType.asStateFlow()

    fun setTargetRegistrationType(type: String?) {
        _targetRegistrationType.value = type
    }
    internal val _screenBackStack = MutableStateFlow<List<String>>(listOf("USER_BROWSE"))
    val screenBackStack: StateFlow<List<String>> = _screenBackStack.asStateFlow()
    val notificationViewModel = com.example.ui.screens.notifications.NotificationViewModel(this)
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
    val _bookings get() = bookingViewModel._bookings
    val bookings get() = bookingViewModel.bookings
    val _bookingFormFields = bookingViewModel._bookingFormFields
    val bookingFormFields = bookingViewModel.bookingFormFields
    val _distributionMode = bookingViewModel._distributionMode
    val distributionMode = bookingViewModel.distributionMode
    internal val _chatMessages = MutableStateFlow<List<com.example.data.ChatMessageEntity>>(emptyList())
    val chatMessages: StateFlow<List<com.example.data.ChatMessageEntity>> = _chatMessages.asStateFlow()
    internal val _chatChannels = MutableStateFlow<List<com.example.data.ChatChannelEntity>>(emptyList())
    val chatChannels: StateFlow<List<com.example.data.ChatChannelEntity>> = _chatChannels.asStateFlow()
    internal val _activeChatChannel = MutableStateFlow<com.example.data.ChatChannelEntity?>(null)
    val activeChatChannel: StateFlow<com.example.data.ChatChannelEntity?> = _activeChatChannel.asStateFlow()
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
    val _instantRequests get() = instantRequestViewModel._instantRequests
    val instantRequests get() = instantRequestViewModel.instantRequests
    val _requestOffers get() = instantRequestViewModel._requestOffers
    val requestOffers get() = instantRequestViewModel.requestOffers
    val _offers get() = instantRequestViewModel._offers
    val offers get() = instantRequestViewModel.offers
    internal val _currentScreen = MutableStateFlow("USER_BROWSE")
    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()
    internal val _navigationStack = mutableListOf<String>()
    
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
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Error: ", e)
            }
        }
    }
    fun refreshData(showNotification: Boolean = false) {
        viewModelScope.launch {
            _isRefreshing.value = true
            _uiErrorMessage.value = null
            try {
                setupRealtimeFirestoreListeners()
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
                setupRealtimeFirestoreListeners()
            }
        }
    }
    fun initializeUserIdentity(context: android.content.Context) {
        appContext = context.applicationContext
        authViewModel.appContext = appContext
        homeViewModel.appContext = appContext
        bookingViewModel.appContext = appContext
        adminViewModel.appContext = appContext
        settingsViewModel.appContext = appContext
        instantRequestViewModel.appContext = appContext
        bookingViewModel.getCoupons = { _coupons.value }
        bookingViewModel.getProviders = { _providers.value }
        bookingViewModel.getCurrentUserPhone = { _currentUserPhone.value }
        bookingViewModel.getCurrentUserName = { _currentUserName.value }
        bookingViewModel.getCurrentUserResidence = { _currentUserResidence.value }
        bookingViewModel.setCurrentUserPhone = { _currentUserPhone.value = it }
        bookingViewModel.setCurrentUserName = { _currentUserName.value = it }
        bookingViewModel.setCurrentUserResidence = { _currentUserResidence.value = it }
        bookingViewModel.onAddNotification = { title, message, targetType, targetValue ->
            addNotification(title = title, message = message, targetType = targetType, targetValue = targetValue)
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
                relatedEntityType = relatedEntityType,
                onCreated = onComplete
            )
        }
        adminViewModel.getHomeViewModel = { homeViewModel }
        adminViewModel.getSettingsViewModel = { settingsViewModel }
        adminViewModel.getBookingViewModel = { bookingViewModel }
        adminViewModel.getInstantRequestViewModel = { instantRequestViewModel }
        adminViewModel.getNotifications = { _notifications }
        adminViewModel.onAddNotification = { title, message, targetType, targetValue ->
            addNotification(title = title, message = message, targetType = targetType, targetValue = targetValue)
        }
        adminViewModel.onTriggerNotificationFull = { title, message, targetType, targetValue ->
            triggerNotification(title = title, message = message, targetType = targetType, targetValue = targetValue)
        }
        adminViewModel.onTriggerNotification = { msg ->
            triggerNotification(msg)
        }
        adminViewModel.onApplyFilters = {  }
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
        settingsViewModel.setPasswordRecoveryWaitingPhone = { _passwordRecoveryWaitingPhone.value = it }
        settingsViewModel.verifyAdminOrOwnerPassword = { password ->
            verifyAdminOrOwnerPassword(password)
        }
        settingsViewModel.triggerNotification = { msg ->
            triggerNotification(msg)
        }
        instantRequestViewModel.triggerNotification = { msg ->
            triggerNotification(msg)
        }
        instantRequestViewModel.addNotification = { title, message, targetType, targetValue ->
            addNotification(title = title, message = message, targetType = targetType, targetValue = targetValue)
        }
        instantRequestViewModel.getOrCreateChatChannel = { providerId, providerName, customerPhone, customerName ->
            openOrCreateChatChannel(
                targetId = providerId,
                targetType = "INSTANT_REQUEST",
                targetName = providerName,
                targetPhone = customerPhone,
                targetCategory = "",
                relatedEntityId = "",
                relatedEntityType = "INSTANT_REQUEST",
                onCreated = { }
            )
        }
        try {
            com.example.ui.helpers.FirestoreSeedHelper(db).seedFirestoreIfEmpty()
        } catch (e: Exception) {
            android.util.Log.e("MainViewModel", "❌ Error in initializeFirestoreCollections", e)
        }
        try {
            authViewModel.initializeUserIdentity(context) { savedFavs ->
                _favoriteIds.value = savedFavs
                checkAndTriggerFavoriteOffersNotifications()
            }
        } catch (e: Exception) {
            android.util.Log.e("MainViewModel", "❌ Error in AuthViewModel initialization", e)
        }
        try {
            setupRealtimeFirestoreListeners()
        } catch (e: Exception) {
            android.util.Log.e("MainViewModel", "❌ Error in setting up realtime listeners", e)
        }
        try {
            settingsViewModel.loadCardSettings()
        } catch (e: Exception) {
            android.util.Log.e("MainViewModel", "❌ Error in SettingsViewModel loadCardSettings", e)
        }
        try {
            adminViewModel.loadPendingTechnicians()
        } catch (e: Exception) {
            android.util.Log.e("MainViewModel", "❌ Error in AdminViewModel loadPendingTechnicians", e)
        }
        try {
            seedFirestoreIfEmpty()
        } catch (e: Exception) {
            android.util.Log.e("MainViewModel", "❌ Error in seedFirestoreIfEmpty", e)
        }
        viewModelScope.launch {
            kotlinx.coroutines.delay(2200)
            _isInitialized.value = true
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
    override suspend fun uploadImageStringOrUri(
        context: android.content.Context,
        input: String,
        storagePath: String,
        maxSizeBytes: Long
    ): String {
        return registrationHelper.uploadImageStringOrUri(context, input, storagePath, maxSizeBytes)
    }
    override fun getAuthEmailForPhone(phone: String): String = authViewModel.getAuthEmailForPhone(phone)

    var lastNotifMsg: String = ""
    var lastNotifTime: Long = 0L
    val triggerRestoreAccountDialog = MutableStateFlow(false)
    var targetChatChannelId by mutableStateOf<String?>(null)
    data class RestoreAccountMatch(
        val type: String,
        val name: String,
        val provider: com.example.data.ProviderEntity? = null,
        val store: com.example.data.StoreEntity? = null,
        val property: com.example.data.PropertyEntity? = null,
        val savedPassword: String = ""
    )
}
