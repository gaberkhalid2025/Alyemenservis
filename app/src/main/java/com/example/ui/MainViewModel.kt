package com.example.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.data.models.*
import com.example.data.repositories.ChatRepository
import com.example.data.repositories.NotificationRepository
import com.example.ui.helpers.*
import com.example.ui.screens.notifications.NotificationViewModel
import com.example.ui.viewmodels.*
import com.example.utils.LocationService
import com.example.utils.NavWaypoint
import com.example.utils.OfflineQueueManager
import com.example.utils.WalletManager
import com.google.android.gms.location.LocationCallback
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    val authViewModel: AuthViewModel,
    val homeViewModel: HomeViewModel,
    val bookingViewModel: BookingViewModel,
    val adminViewModel: AdminViewModel,
    val settingsViewModel: SettingsViewModel,
    val instantRequestViewModel: InstantRequestViewModel,
    val chatRepo: ChatRepository,
    val appState: AppState,
    val walletManager: WalletManager,
    val storeManagementViewModel: StoreManagementViewModel = StoreManagementViewModel(appState),
    val propertyManagementViewModel: PropertyManagementViewModel = PropertyManagementViewModel(appState),
    val jobManagementViewModel: JobManagementViewModel = JobManagementViewModel(appState),
    val paymentManagementViewModel: PaymentManagementViewModel = PaymentManagementViewModel(appState)
) : BaseViewModel() {

    val preferenceHelper = AppPreferenceHelper()
    val notificationRepository = NotificationRepository()
    val firestoreSeedHelper by lazy { FirestoreSeedHelper(db) }
    val realtimeSyncHelper by lazy { RealtimeSyncHelper(db) }
    val registrationHelper by lazy { RegistrationHelper(db, auth, preferenceHelper) }
    val accountRecoveryHelper by lazy { AccountRecoveryHelper(db, preferenceHelper) }

    internal var lastSyncedAuthUid: String? = null
    internal var authStateListener: FirebaseAuth.AuthStateListener? = null

    override fun onCleared() {
        super.onCleared()
        try {
            authStateListener?.let { auth.removeAuthStateListener(it) }
            authStateListener = null
            realtimeSyncHelper.clearListeners()
            stopLocationUpdates()
            stopNavigationTracking()
            freshnessJob?.cancel()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val auth: FirebaseAuth = FirebaseAuth.getInstance()
    internal val _currentLanguage = MutableStateFlow("ar")
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()
    internal val _favoriteIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteIds: StateFlow<Set<String>> = _favoriteIds.asStateFlow()
    internal val _notifications get() = appState._notifications
    val notifications: StateFlow<List<NotificationEntity>> = _notifications.asStateFlow()
    internal val _readNotificationIds = MutableStateFlow<Set<String>>(emptySet())
    val readNotificationIds: StateFlow<Set<String>> = _readNotificationIds.asStateFlow()
    internal val _supervisors get() = appState._supervisors
    val supervisors: StateFlow<List<SupervisorEntity>> = _supervisors.asStateFlow()
    internal val _colorPalettes get() = appState._colorPalettes
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

    val pendingTrackingToken = MutableStateFlow<String?>(null)

    val _currentUserId get() = authViewModel._currentUserId
    val currentUserId get() = authViewModel.currentUserId
    val _currentUserName get() = authViewModel._currentUserName
    val currentUserName get() = authViewModel.currentUserName
    val _currentUserPhone get() = authViewModel._currentUserPhone
    val currentUserPhone get() = authViewModel.currentUserPhone
    val _currentUserResidence get() = authViewModel._currentUserResidence
    val currentUserResidence get() = authViewModel.currentUserResidence
    val _adminRole get() = adminViewModel._adminRole
    val adminRole get() = adminViewModel.adminRole
    val supervisorPermissions get() = adminViewModel.supervisorPermissions
    val _passwordRecoveryWaitingPhone get() = authViewModel._passwordRecoveryWaitingPhone
    val passwordRecoveryWaitingPhone get() = authViewModel.passwordRecoveryWaitingPhone
    val _joinRequestPhone get() = authViewModel._joinRequestPhone
    val joinRequestPhone get() = authViewModel.joinRequestPhone
    val showBackdoorDialog get() = authViewModel.showBackdoorDialog
    val _categories get() = homeViewModel._categories
    val categories get() = homeViewModel.categories

    fun updateAdminSettings(settings: AdminSettingsEntity) = settingsViewModel.updateAdminSettings(settings)
    fun saveCustomProfileTab(tab: CustomProfileTabEntity) = adminViewModel.saveCustomProfileTab(tab)
    fun deleteCustomProfileTab(tabId: String) = adminViewModel.deleteCustomProfileTab(tabId)
    fun toggleCustomProfileTab(tabId: String) = adminViewModel.toggleCustomProfileTab(tabId)
    fun addNewCategory(nameAr: String, nameEn: String, icon: String, description: String, parentId: String = "", isMainCategory: Boolean = true) =
        adminViewModel.addNewCategory(nameAr, nameEn, icon, description, parentId, isMainCategory)

    val _providers get() = homeViewModel._providers
    val providers get() = homeViewModel.providers
    val _filteredProviders get() = homeViewModel._filteredProviders
    val filteredProviders get() = homeViewModel.filteredProviders
    val _filteredStores get() = homeViewModel._filteredStores
    val filteredStores: StateFlow<List<StoreEntity>> get() = homeViewModel.filteredStores
    val _filteredProperties get() = homeViewModel._filteredProperties
    val filteredProperties: StateFlow<List<PropertyEntity>> get() = homeViewModel.filteredProperties
    val _banners get() = homeViewModel._banners
    val banners get() = homeViewModel.banners
    val activeBrowserTab get() = homeViewModel.activeBrowserTab
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
    val chatCoordinator = ChatCoordinator()

    val _chatMessages get() = chatCoordinator._chatMessages
    val chatMessages get() = chatCoordinator.chatMessages
    val _chatChannels get() = chatCoordinator._chatChannels
    val chatChannels get() = chatCoordinator.chatChannels
    val _activeChatChannel get() = chatCoordinator._activeChatChannel
    val activeChatChannel get() = chatCoordinator.activeChatChannel
    val _pendingProviders get() = adminViewModel._pendingProviders
    val pendingProviders get() = adminViewModel.pendingProviders
    val _pendingTechnicians get() = adminViewModel._pendingTechnicians
    val pendingTechnicians get() = adminViewModel.pendingTechnicians
    val _registeredUsersList get() = adminViewModel._registeredUsersList
    val registeredUsersList get() = adminViewModel.registeredUsersList
    val _registeredUsersCount get() = adminViewModel._registeredUsersCount
    val registeredUsersCount get() = adminViewModel.registeredUsersCount
    val _passwordRecoveryRequests get() = adminViewModel.passwordRecoveryRequests
    val passwordRecoveryRequests get() = adminViewModel.passwordRecoveryRequests
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

    var selectedProvider: ProviderEntity? = null
    var selectedStore: StoreEntity? = null
    var selectedProperty: PropertyEntity? = null
    var selectedJob: JobEntity? = null
    var comingSoonServiceName by mutableStateOf("التوصيل")

    fun openComingSoon(serviceName: String) {
        comingSoonServiceName = serviceName
        navigateToScreen(AppScreens.COMING_SOON_SERVICES)
    }

    var selectedOfferId by mutableStateOf("")
    var selectedRequestId by mutableStateOf("")
    var showQuickServiceDialog by mutableStateOf(false)
    internal val _lastFixAtMs = MutableStateFlow(0L)
    internal var freshnessJob: Job? = null

    internal val _userLatitude = MutableStateFlow(15.3694)
    val userLatitude: StateFlow<Double> = _userLatitude.asStateFlow()
    internal val _userLongitude = MutableStateFlow(44.1910)
    val userLongitude: StateFlow<Double> = _userLongitude.asStateFlow()
    internal val _userLocationAccuracyM = MutableStateFlow(Float.MAX_VALUE)
    val userLocationAccuracyM: StateFlow<Float> = _userLocationAccuracyM.asStateFlow()
    internal val _isUserLocationReal = MutableStateFlow(false)
    val isUserLocationReal: StateFlow<Boolean> = _isUserLocationReal.asStateFlow()
    internal val _isManualLocationSet = MutableStateFlow(false)
    val isManualLocationSet: StateFlow<Boolean> = _isManualLocationSet.asStateFlow()
    internal val _isGpsTrackingActive = MutableStateFlow(false)
    val isGpsTrackingActive: StateFlow<Boolean> = _isGpsTrackingActive.asStateFlow()
    internal var locationCallback: LocationCallback? = null
    internal var navTrackingJob: Job? = null

    val locationService by lazy {
        LocationService(appContext ?: com.example.MyApplication.instance ?: throw IllegalStateException("Context not ready"))
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface QueueEntryPoint {
        fun offlineQueueManager(): OfflineQueueManager
    }

    val offlineQueueManager: OfflineQueueManager by lazy {
        val ctx = appContext ?: com.example.MyApplication.instance ?: throw IllegalStateException("Context not ready")
        EntryPointAccessors.fromApplication(ctx.applicationContext, QueueEntryPoint::class.java).offlineQueueManager()
    }

    internal val _isProvidersLoading get() = appState._isProvidersLoading
    val isProvidersLoading: StateFlow<Boolean> = _isProvidersLoading.asStateFlow()
    internal val _isChatChannelsLoading get() = appState._isChatChannelsLoading
    val isChatChannelsLoading: StateFlow<Boolean> = _isChatChannelsLoading.asStateFlow()
    internal val _cities get() = appState._cities
    val cities: StateFlow<List<CityEntity>> = _cities.asStateFlow()
    internal val _deletedProviders get() = appState._deletedProviders
    val deletedProviders: StateFlow<List<ProviderEntity>> = _deletedProviders.asStateFlow()
    private val _showNotificationsDialog = MutableStateFlow(false)
    val showNotificationsDialog: StateFlow<Boolean> = _showNotificationsDialog.asStateFlow()

    private val _showRestoreAccountDialog = MutableStateFlow(false)
    val showRestoreAccountDialog: StateFlow<Boolean> = _showRestoreAccountDialog.asStateFlow()

    fun toggleNotificationsDialog(show: Boolean) {
        _showNotificationsDialog.value = show
    }

    fun toggleRestoreAccountDialog(show: Boolean) {
        _showRestoreAccountDialog.value = show
    }

    internal var isSettingsLoaded = false
    internal var isCategoriesLoaded = false
    internal var isCitiesLoaded = false
    internal var isProvidersLoaded = false

    internal val _isInitialized get() = appState._isInitialized
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()
    internal val _maxKmRadius get() = appState._maxKmRadius
    val maxKmRadius: StateFlow<Int> = _maxKmRadius.asStateFlow()
    var lastNotifMsg: String = ""
    var lastNotifTime: Long = 0L
    var targetChatChannelId by mutableStateOf<String?>(null)
    var targetChatUserId by mutableStateOf<String?>(null)
    var targetChatUserName by mutableStateOf<String?>(null)
    var targetChatUserPhoto by mutableStateOf<String?>(null)
    var targetChatRelatedEntityId by mutableStateOf<String?>(null)
    var targetChatRelatedEntityType by mutableStateOf<String?>(null)
    var pendingMapFocusPoint by mutableStateOf<NavWaypoint?>(null)
    var pendingMapAutoNavigate by mutableStateOf(false)
    val triggerRestoreAccountDialog = MutableStateFlow(false)

    fun openInAppMap(latitude: Double, longitude: Double, label: String = "موقع مشارك", startNavigation: Boolean = false) =
        openInAppMapHelper(latitude, longitude, label, startNavigation)

    fun openDirectChat(
        targetUserId: String,
        targetUserName: String,
        targetUserPhoto: String = "",
        relatedEntityId: String? = null,
        relatedEntityType: String? = null
    ) {
        targetChatChannelId = null
        targetChatUserId = targetUserId
        targetChatUserName = targetUserName
        targetChatUserPhoto = targetUserPhoto
        targetChatRelatedEntityId = relatedEntityId
        targetChatRelatedEntityType = relatedEntityType
        navigateToScreen(AppScreens.CHAT_DIRECT)
    }

    val notificationViewModel = NotificationViewModel(this)

    init {
        _stores.value = getDefaultStoresList()
        _properties.value = getDefaultPropertiesList()
        viewModelScope.launch {
            authViewModel.adminRole.collect { role ->
                if (adminViewModel._adminRole.value != role) {
                    adminViewModel._adminRole.value = role
                }
            }
        }
        viewModelScope.launch {
            adminViewModel.adminRole.collect { role ->
                if (authViewModel._adminRole.value != role) {
                    authViewModel._adminRole.value = role
                }
            }
        }
        viewModelScope.launch {
            authViewModel.currentSupervisorPermissions.collect { perms ->
                if (adminViewModel.supervisorPermissions.value != perms) {
                    adminViewModel._supervisorPermissions.value = perms
                }
            }
        }
        viewModelScope.launch {
            appState._supervisors.collect { sups ->
                if (authViewModel._supervisors.value != sups) {
                    authViewModel._supervisors.value = sups
                }
            }
        }
    }

    fun updateUserLocation(lat: Double, lng: Double) = updateUserLocationHelper(lat, lng)
    fun setManualUserLocation(lat: Double, lng: Double) = setManualUserLocationHelper(lat, lng)
    fun clearManualUserLocation() = clearManualUserLocationHelper()
    fun startLocationUpdates() = startLocationUpdatesHelper()
    fun stopLocationUpdates() = stopLocationUpdatesHelper()
    fun startNavigationTracking() = startNavigationTrackingHelper()
    fun stopNavigationTracking() = stopNavigationTrackingHelper()
    fun enqueueSos(lat: Double, lng: Double, accuracyM: Float?, message: String): Boolean =
        enqueueSosHelper(lat, lng, accuracyM, message)

    fun refreshData(@Suppress("UNUSED_PARAMETER") showNotification: Boolean = false) = refreshDataHelper()
    fun updateOnlineStatus(online: Boolean) {
        _isOnline.value = online
    }
    fun saveCustomSettingsState(newSettings: AdminSettingsEntity) =
        settingsViewModel.saveCustomSettingsState(newSettings)
    fun retryConnection(context: Context) = retryConnectionHelper(context)
    fun initializeUserIdentity(context: Context) = initializeUserIdentityHelper(context)

    fun setupRealtimeFirestoreListeners() {
        realtimeSyncHelper.clearListeners()
        realtimeSyncHelper.setupRealtimeFirestoreListeners(appState)
        viewModelScope.launch {
            realtimeSyncHelper.loadOnDemandInitialData(appState)
        }
    }

    fun seedFirestoreIfEmpty() {
        firestoreSeedHelper.seedFirestoreIfEmpty()
    }

    override fun getDefaultStoresList(): List<StoreEntity> = emptyList()
    override fun getDefaultPropertiesList(): List<PropertyEntity> = emptyList()

    override suspend fun uploadImageStringOrUri(
        context: Context,
        input: String,
        storagePath: String,
        maxSizeBytes: Long
    ): String {
        return registrationHelper.uploadImageStringOrUri(context, input, storagePath, maxSizeBytes)
    }

    override fun getAuthEmailForPhone(phone: String): String = authViewModel.getAuthEmailForPhone(phone)

    data class RestoreAccountMatch(
        val type: String,
        val name: String,
        val provider: ProviderEntity? = null,
        val store: StoreEntity? = null,
        val property: PropertyEntity? = null
    )

    fun observePasswordRecoveryStatus(
        phone: String,
        onUpdate: (status: String, newPassword: String, accountName: String, accountType: String) -> Unit
    ): ListenerRegistration? = authViewModel.observePasswordRecoveryStatus(phone, onUpdate)

    fun requestPasswordRecovery(phone: String, name: String, accountType: String) =
        authViewModel.requestPasswordRecovery(phone, name, accountType)

    suspend fun searchAll(query: String): Map<String, List<Any>> {
        val providers = realtimeSyncHelper.searchProviders(query)
        val stores = realtimeSyncHelper.searchStores(query)
        val properties = realtimeSyncHelper.searchProperties(query)
        val jobs = realtimeSyncHelper.searchJobs(query)

        return mapOf(
            "providers" to providers,
            "stores" to stores,
            "properties" to properties,
            "jobs" to jobs
        )
    }
}
