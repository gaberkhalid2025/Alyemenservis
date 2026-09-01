package com.example.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.data.models.*
import com.example.data.repositories.contracts.*
import com.example.data.repositories.impl.*
import com.example.utils.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class MainViewModel(
    private val categoryRepository: ICategoryRepository = CategoryRepositoryImpl(FirebaseFirestore.getInstance()),
    private val providerRepository: IProviderRepository = ProviderRepositoryImpl(appContext),
    private val bookingRepository: IBookingRepository = BookingRepositoryImpl(appContext ?: com.google.firebase.FirebaseApp.getInstance().applicationContext),
    private val notificationRepository: INotificationRepository = NotificationRepositoryImpl(appContext),
    private val settingsRepository: ISettingsRepository = SettingsRepositoryImpl(appContext),
    private val urgentRequestRepository: IUrgentRequestRepository = UrgentRequestRepositoryImpl(appContext),
    private val authRepository: IAuthRepository = AuthRepositoryImpl(appContext),
    private val storageRepository: IStorageRepository = StorageRepositoryImpl(),
    private val chatRepository: IChatRepository = ChatRepositoryImpl(appContext)
) : ViewModel() {

    // Legacy Firestore compatibility
    val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    companion object {
        var appContext: Context? = null
    }

    // Navigation and Language StateFlows
    private val _currentScreen = MutableStateFlow("USER_BROWSE")
    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()

    private val _screenBackStack = MutableStateFlow<List<String>>(listOf("USER_BROWSE"))
    val screenBackStack: StateFlow<List<String>> = _screenBackStack.asStateFlow()

    private val _adminRole = MutableStateFlow("GUEST")
    val adminRole: StateFlow<String> = _adminRole.asStateFlow()

    private val _currentLanguage = MutableStateFlow("ar")
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    private val _showBackdoorDialog = MutableStateFlow(false)
    val showBackdoorDialog: StateFlow<Boolean> = _showBackdoorDialog.asStateFlow()

    // Domain StateFlows
    internal val _categories = MutableStateFlow<List<CategoryEntity>>(emptyList())
    val categories: StateFlow<List<CategoryEntity>> = _categories.asStateFlow()

    internal val _providers = MutableStateFlow<List<ProviderEntity>>(emptyList())
    val providers: StateFlow<List<ProviderEntity>> = _providers.asStateFlow()

    internal val _deletedProviders = MutableStateFlow<List<ProviderEntity>>(emptyList())
    val deletedProviders: StateFlow<List<ProviderEntity>> = _deletedProviders.asStateFlow()

    internal val _filteredProviders = MutableStateFlow<List<ProviderEntity>>(emptyList())
    val filteredProviders: StateFlow<List<ProviderEntity>> = _filteredProviders.asStateFlow()

    internal val _pendingProviders = MutableStateFlow<List<PendingProviderEntity>>(emptyList())
    val pendingProviders: StateFlow<List<PendingProviderEntity>> = _pendingProviders.asStateFlow()

    internal val _banners = MutableStateFlow<List<BannerEntity>>(emptyList())
    val banners: StateFlow<List<BannerEntity>> = _banners.asStateFlow()

    internal val _settings = MutableStateFlow(AdminSettingsEntity())
    val settings: StateFlow<AdminSettingsEntity> = _settings.asStateFlow()

    internal val _reports = MutableStateFlow<List<ReportEntity>>(emptyList())
    val reports: StateFlow<List<ReportEntity>> = _reports.asStateFlow()

    internal val _activityLogs = MutableStateFlow<List<ActivityLogEntity>>(emptyList())
    val activityLogs: StateFlow<List<ActivityLogEntity>> = _activityLogs.asStateFlow()

    internal val _callsLog = MutableStateFlow<List<CallEntity>>(emptyList())
    val callsLog: StateFlow<List<CallEntity>> = _callsLog.asStateFlow()

    internal val _coupons = MutableStateFlow<List<CouponEntity>>(emptyList())
    val coupons: StateFlow<List<CouponEntity>> = _coupons.asStateFlow()

    internal val _internalWallets = MutableStateFlow<List<InternalWalletEntity>>(emptyList())
    val internalWallets: StateFlow<List<InternalWalletEntity>> = _internalWallets.asStateFlow()

    internal val _walletTransactions = MutableStateFlow<List<WalletTransactionEntity>>(emptyList())
    val walletTransactions: StateFlow<List<WalletTransactionEntity>> = _walletTransactions.asStateFlow()

    internal val _userLatitude = MutableStateFlow(15.3694)
    val userLatitude: StateFlow<Double> = _userLatitude.asStateFlow()

    internal val _userLongitude = MutableStateFlow(44.1910)
    val userLongitude: StateFlow<Double> = _userLongitude.asStateFlow()

    internal val _supervisors = MutableStateFlow<List<com.example.data.models.SupervisorEntity>>(emptyList())
    val supervisors: StateFlow<List<com.example.data.models.SupervisorEntity>> = _supervisors.asStateFlow()

    internal val _registeredUsersList = MutableStateFlow<List<Map<String, Any>>>(emptyList())
    val registeredUsersList: StateFlow<List<Map<String, Any>>> = _registeredUsersList.asStateFlow()

    internal val _instantRequests = MutableStateFlow<List<InstantRequestEntity>>(emptyList())
    val instantRequests: StateFlow<List<InstantRequestEntity>> = _instantRequests.asStateFlow()

    internal val _requestOffers = MutableStateFlow<List<RequestOfferEntity>>(emptyList())
    val requestOffers: StateFlow<List<RequestOfferEntity>> = _requestOffers.asStateFlow()

    internal val _isGpsTrackingActive = MutableStateFlow(false)
    val isGpsTrackingActive: StateFlow<Boolean> = _isGpsTrackingActive.asStateFlow()

    internal val _isProvidersLoading = MutableStateFlow(true)
    val isProvidersLoading: StateFlow<Boolean> = _isProvidersLoading.asStateFlow()

    internal val _isChatChannelsLoading = MutableStateFlow(true)
    val isChatChannelsLoading: StateFlow<Boolean> = _isChatChannelsLoading.asStateFlow()

    internal val _cities = MutableStateFlow<List<CityEntity>>(emptyList())
    val cities: StateFlow<List<CityEntity>> = _cities.asStateFlow()

    internal val _chatMessages = MutableStateFlow<List<ChatMessageEntity>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessageEntity>> = _chatMessages.asStateFlow()

    internal val _bookings = MutableStateFlow<List<BookingEntity>>(emptyList())
    val bookings: StateFlow<List<BookingEntity>> = _bookings.asStateFlow()

    internal val _paymentWallets = MutableStateFlow<List<PaymentWalletEntity>>(emptyList())
    val paymentWallets: StateFlow<List<PaymentWalletEntity>> = _paymentWallets.asStateFlow()

    internal val _payments = MutableStateFlow<List<PaymentEntity>>(emptyList())
    val payments: StateFlow<List<PaymentEntity>> = _payments.asStateFlow()

    internal val _notifications = MutableStateFlow<List<NotificationEntity>>(emptyList())
    val notifications: StateFlow<List<NotificationEntity>> = _notifications.asStateFlow()

    internal val _offers = MutableStateFlow<List<Offer>>(emptyList())
    val offers: StateFlow<List<Offer>> = _offers.asStateFlow()

    internal val _passwordRecoveryWaitingPhone = MutableStateFlow("")
    val passwordRecoveryWaitingPhone: StateFlow<String> = _passwordRecoveryWaitingPhone.asStateFlow()

    internal val _orders = MutableStateFlow<List<OrderEntity>>(emptyList())
    val orders: StateFlow<List<OrderEntity>> = _orders.asStateFlow()

    // Screen State Variables
    var selectedProvider: ProviderEntity? = null
    var selectedStore: StoreEntity? = null
    var selectedProperty: PropertyEntity? = null
    var selectedOfferId by mutableStateOf("")
    var selectedRequestId by mutableStateOf("")
    var showQuickServiceDialog by mutableStateOf(false)

    // Chat Channels & Favorites
    internal val _chatChannels = MutableStateFlow<List<ChatChannelEntity>>(emptyList())
    val chatChannels: StateFlow<List<ChatChannelEntity>> = _chatChannels.asStateFlow()

    internal val _activeChatChannel = MutableStateFlow<ChatChannelEntity?>(null)
    val activeChatChannel: StateFlow<ChatChannelEntity?> = _activeChatChannel.asStateFlow()

    internal val _stores = MutableStateFlow<List<StoreEntity>>(emptyList())
    val stores: StateFlow<List<StoreEntity>> = _stores.asStateFlow()

    internal val _products = MutableStateFlow<List<ProductEntity>>(emptyList())
    val products: StateFlow<List<ProductEntity>> = _products.asStateFlow()

    internal val _properties = MutableStateFlow<List<PropertyEntity>>(emptyList())
    val properties: StateFlow<List<PropertyEntity>> = _properties.asStateFlow()

    internal val _jobs = MutableStateFlow<List<JobEntity>>(emptyList())
    val jobs: StateFlow<List<JobEntity>> = _jobs.asStateFlow()

    internal val _jobApplications = MutableStateFlow<List<JobApplicationEntity>>(emptyList())
    val jobApplications: StateFlow<List<JobApplicationEntity>> = _jobApplications.asStateFlow()

    private val _ratings = MutableStateFlow<List<RatingEntity>>(emptyList())
    val ratings: StateFlow<List<RatingEntity>> = _ratings.asStateFlow()

    private val _favoriteIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteIds: StateFlow<Set<String>> = _favoriteIds.asStateFlow()

    // Filters properties
    internal val _selectedCategoryId = MutableStateFlow<String?>("ALL")
    internal val _searchQuery = MutableStateFlow("")
    internal val _filterVipOnly = MutableStateFlow(false)
    internal val _filterAvailableOnly = MutableStateFlow(false)
    internal val _filterCityId = MutableStateFlow<String?>("ALL")
    internal val _filterNeighborhoodName = MutableStateFlow("")
    internal val _phoneOrNameFilter = MutableStateFlow("")

    // Network & Loading Compatibility StateFlows
    internal val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    internal val _isInitialized = MutableStateFlow(true)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    internal val _uiErrorMessage = MutableStateFlow<String?>(null)
    val uiErrorMessage: StateFlow<String?> = _uiErrorMessage.asStateFlow()

    internal val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // User authentication compatibility StateFlows
    internal val _currentUserId = MutableStateFlow("guest_user")
    val currentUserId: StateFlow<String> = _currentUserId.asStateFlow()

    internal val _currentUserPhone = MutableStateFlow("")
    val currentUserPhone: StateFlow<String> = _currentUserPhone.asStateFlow()

    internal val _currentUserName = MutableStateFlow("زائر")
    val currentUserName: StateFlow<String> = _currentUserName.asStateFlow()

    internal val _currentUserResidence = MutableStateFlow("صنعاء")
    val currentUserResidence: StateFlow<String> = _currentUserResidence.asStateFlow()

    // Interactive Dialogs & Dynamic Custom States
    internal val _activeVoiceCall = MutableStateFlow<Pair<String, String>?>(null)
    val activeVoiceCall: StateFlow<Pair<String, String>?> = _activeVoiceCall.asStateFlow()

    internal val _toastFlow = MutableStateFlow<String?>(null)
    val toastFlow: StateFlow<String?> = _toastFlow.asStateFlow()

    val triggerRestoreAccountDialog = MutableStateFlow(false)

    private val _customProfileTabs = MutableStateFlow<List<CustomProfileTabEntity>>(emptyList())
    val customProfileTabs: StateFlow<List<CustomProfileTabEntity>> = _customProfileTabs.asStateFlow()

    private val _colorPalettes = MutableStateFlow<List<ColorPaletteEntity>>(emptyList())
    val colorPalettes: StateFlow<List<ColorPaletteEntity>> = _colorPalettes.asStateFlow()

    init {
        // Collect Categories
        viewModelScope.launch {
            categoryRepository.observeCategories().collect { result ->
                result.onSuccess { _categories.value = it }
            }
        }
        // Collect Providers
        viewModelScope.launch {
            providerRepository.observeApprovedProviders().collect { list ->
                _providers.value = list
                _filteredProviders.value = list
                _isProvidersLoading.value = false
            }
        }
        // Collect Bookings
        viewModelScope.launch {
            bookingRepository.observeBookings().collect { list ->
                _bookings.value = list
            }
        }
        // Collect Notifications
        viewModelScope.launch {
            notificationRepository.observeAdminNotifications().collect { list ->
                _notifications.value = list
            }
        }
        // Collect Settings
        viewModelScope.launch {
            settingsRepository.observeSettings().collect { entity ->
                _settings.value = entity
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        chatRepository.clearListeners()
    }

    // Navigation and Language implementation
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
            _currentLanguage.value = if (_currentLanguage.value == "ar") "en" else "ar"
        }
    }

    fun toggleLanguage(context: Context) {
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
    }

    fun setLanguage(context: Context, lang: String) {
        appContext = context.applicationContext
        setLanguage(lang)
    }

    // Helper functions
    fun updateUserLocation(lat: Double, lng: Double) {
        _userLatitude.value = lat
        _userLongitude.value = lng
    }

    fun startLocationUpdates() {
        _isGpsTrackingActive.value = true
        appContext?.let { ctx ->
            try {
                val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager
                val loc = lm?.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
                    ?: lm?.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)
                loc?.let {
                    updateUserLocation(it.latitude, it.longitude)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setPasswordRecoveryWaitingPhone(phone: String) {
        _passwordRecoveryWaitingPhone.value = phone
    }

    fun toggleFavorite(id: String) {
        val current = _favoriteIds.value.toMutableSet()
        if (current.contains(id)) {
            current.remove(id)
            triggerNotification("💔 تم الحذف من قائمة المفضلة")
        } else {
            current.add(id)
            triggerNotification("❤️ تم الإضافة إلى قائمة المفضلة")
        }
        _favoriteIds.value = current
    }

    fun triggerNotification(message: String) {
        addNotification("تنبيه", message, "system", "عام")
    }

    fun applyFilters() {
        val allProviders = _providers.value
        val selectedCat = _selectedCategoryId.value
        val query = _searchQuery.value.trim().lowercase()
        val vipOnly = _filterVipOnly.value
        val availOnly = _filterAvailableOnly.value

        var filtered = allProviders
        if (!selectedCat.isNullOrBlank() && selectedCat != "ALL" && selectedCat != "الكل") {
            filtered = filtered.filter { it.categoryId == selectedCat }
        }
        if (query.isNotEmpty()) {
            filtered = filtered.filter {
                it.name.lowercase().contains(query) ||
                it.profession.lowercase().contains(query) ||
                it.specialization.lowercase().contains(query) ||
                it.customCategoryName.lowercase().contains(query)
            }
        }
        if (vipOnly) {
            filtered = filtered.filter { it.isVip }
        }
        if (availOnly) {
            filtered = filtered.filter { it.isAvailable }
        }
        _filteredProviders.value = filtered
    }

    fun setFilterCategoryId(catId: String?) {
        _selectedCategoryId.value = catId
        applyFilters()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        applyFilters()
    }

    fun selectCategory(catId: String) {
        _selectedCategoryId.value = catId
        applyFilters()
    }

    fun updateOnlineStatus(online: Boolean) {
        _isOnline.value = online
    }

    fun refreshData() {
        viewModelScope.launch {
            _isRefreshing.value = true
            kotlinx.coroutines.delay(1000)
            _isRefreshing.value = false
        }
    }

    fun clearUiError() {
        _uiErrorMessage.value = null
    }

    // Booking actions
    fun addBooking(booking: BookingEntity, onResult: (Boolean, String) -> Unit = {_,_ ->}) {
        viewModelScope.launch {
            bookingRepository.createDirectBooking(booking).fold(
                onSuccess = { onResult(true, "تم إضافة الحجز بنجاح") },
                onFailure = { onResult(false, it.localizedMessage ?: "فشل إضافة الحجز") }
            )
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
        couponCode: String,
        pinCode: String,
        customBookingId: String,
        customPassword: String
    ) {
        val booking = BookingEntity(
            id = customBookingId.ifBlank { UUID.randomUUID().toString() },
            customerName = name,
            customerPhone = phone,
            customerArea = area,
            serviceType = serviceType,
            providerId = providerId,
            providerName = providerName,
            dateString = dateString,
            timeString = timeString,
            pinCode = pinCode,
            bookingPassword = customPassword
        )
        addBooking(booking)
    }

    fun updateBookingStatus(bookingId: String, newStatus: String, rejectionReason: String = "") {
        viewModelScope.launch {
            bookingRepository.updateBookingStatus(bookingId, newStatus)
        }
    }

    fun deleteBooking(bookingId: String) {
        viewModelScope.launch {
            bookingRepository.deleteBooking(bookingId)
        }
    }

    fun updateBooking(booking: BookingEntity) {
        viewModelScope.launch {
            bookingRepository.updateBooking(booking)
        }
    }

    fun attemptCancelBooking(bookingId: String, input: String, reason: String = "ملغي بطلب العميل", onResult: (Boolean, String) -> Unit = {_,_ ->}) {
        viewModelScope.launch {
            bookingRepository.updateBookingStatus(bookingId, "CANCELLED").fold(
                onSuccess = { onResult(true, "تم إلغاء الحجز بنجاح") },
                onFailure = { onResult(false, it.localizedMessage ?: "فشل إلغاء الحجز") }
            )
        }
    }

    // Notification actions
    fun addNotification(titleAr: String, bodyAr: String, type: String = "general", categoryAr: String = "عام", targetUserPhone: String? = null) {
        viewModelScope.launch {
            val entity = NotificationEntity(
                id = UUID.randomUUID().toString(),
                title = titleAr,
                message = bodyAr,
                notificationType = type,
                createdAt = System.currentTimeMillis()
            )
            notificationRepository.sendNotification(entity)
        }
    }

    fun deleteNotification(notifId: String) {
        viewModelScope.launch {
            notificationRepository.deleteNotification(notifId)
        }
    }

    fun deleteAllNotifications() {
        viewModelScope.launch {
            _notifications.value.forEach { notif ->
                notificationRepository.deleteNotification(notif.id)
            }
        }
    }

    // Urgent request actions
    fun createInstantRequest(request: InstantRequestEntity, onResult: (Boolean, String) -> Unit = {_,_ ->}) {
        viewModelScope.launch {
            urgentRequestRepository.createInstantRequest(request).fold(
                onSuccess = { onResult(true, it.id) },
                onFailure = { onResult(false, it.localizedMessage ?: "فشل إنشاء الطلب") }
            )
        }
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
        images: List<String>,
        urgencyTime: String,
        deliveryMethod: String = "DELIVERY",
        customPin: String = "",
        onResult: (Boolean, String, String) -> Unit
    ) {
        val reqCode = "REQ-${System.currentTimeMillis() % 1000000}"
        val secretPin = customPin.ifBlank { (1000..9999).random().toString() }
        val entity = InstantRequestEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            userName = userName,
            userPhone = userPhone,
            userCity = userCity,
            userNeighborhood = userNeighborhood,
            categoryId = categoryId,
            categoryName = categoryName,
            serviceTitle = serviceTitle,
            description = description,
            urgencyTime = urgencyTime,
            deliveryMethod = deliveryMethod,
            requestCode = reqCode,
            secretPin = secretPin,
            createdAt = System.currentTimeMillis()
        )
        viewModelScope.launch {
            urgentRequestRepository.createInstantRequest(entity).fold(
                onSuccess = { onResult(true, reqCode, secretPin) },
                onFailure = { onResult(false, "", "") }
            )
        }
    }

    fun submitOfferForRequest(offer: RequestOfferEntity, onResult: (Boolean, String) -> Unit = {_,_ ->}) {
        viewModelScope.launch {
            urgentRequestRepository.submitOffer(offer).fold(
                onSuccess = { onResult(true, "تم تقديم العرض") },
                onFailure = { onResult(false, it.localizedMessage ?: "فشل تقديم العرض") }
            )
        }
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
        estimatedArrivalTime: String,
        estimatedDuration: String,
        notes: String
    ) {
        val entity = RequestOfferEntity(
            id = UUID.randomUUID().toString(),
            requestId = requestId,
            requestCode = requestCode,
            technicianId = technicianId,
            technicianName = technicianName,
            technicianPhone = technicianPhone,
            technicianAvatar = technicianAvatar,
            technicianRating = technicianRating,
            price = price,
            estimatedArrivalTime = estimatedArrivalTime,
            estimatedDuration = estimatedDuration,
            notes = notes,
            createdAt = System.currentTimeMillis()
        )
        submitOfferForRequest(entity)
    }

    fun acceptRequestOffer(requestId: String, offerId: String, providerPhone: String = "", onResult: (Boolean, String) -> Unit = {_,_ ->}) {
        viewModelScope.launch {
            urgentRequestRepository.acceptOffer(requestId, offerId, "", "", providerPhone, 0.0).fold(
                onSuccess = { onResult(true, "تم قبول العرض بنجاح") },
                onFailure = { onResult(false, it.localizedMessage ?: "فشل قبول العرض") }
            )
        }
    }

    fun acceptRequestOffer(request: InstantRequestEntity, offer: RequestOfferEntity) {
        acceptRequestOffer(request.id, offer.id, offer.technicianPhone) { success, msg ->
            triggerNotification(msg)
        }
    }

    fun cancelInstantRequest(requestId: String, passwordInput: String = "", isCustomer: Boolean = true, reqPass: String = "") {
        viewModelScope.launch {
            urgentRequestRepository.cancelInstantRequest(requestId, passwordInput)
        }
    }

    // Provider actions
    fun approvePendingProvider(pending: PendingProviderEntity) {
        viewModelScope.launch {
            providerRepository.saveOrUpdateProvider(
                ProviderEntity(id = pending.id, name = pending.name, phone = pending.phone)
            )
        }
    }

    fun rejectPendingProvider(pending: PendingProviderEntity, reason: String = "تم رفض الطلب") {
        viewModelScope.launch {
            providerRepository.deleteProvider(pending.id)
        }
    }

    fun toggleProviderBlock(providerId: String) {
        viewModelScope.launch {
            val provider = _providers.value.find { it.id == providerId } ?: return@launch
            providerRepository.saveOrUpdateProvider(provider.copy(isBlocked = !provider.isBlocked))
        }
    }

    fun toggleProviderPin(providerId: String) {
        viewModelScope.launch {
            val provider = _providers.value.find { it.id == providerId } ?: return@launch
            providerRepository.saveOrUpdateProvider(provider.copy(isVip = !provider.isVip))
        }
    }

    fun toggleProviderVerification(providerId: String) {
        viewModelScope.launch {
            val provider = _providers.value.find { it.id == providerId } ?: return@launch
            providerRepository.saveOrUpdateProvider(provider.copy(isVerified = !provider.isVerified))
        }
    }

    fun toggleProviderRecommendation(providerId: String) {
        viewModelScope.launch {
            val provider = _providers.value.find { it.id == providerId } ?: return@launch
            providerRepository.saveOrUpdateProvider(provider.copy(isRecommended = !provider.isRecommended))
        }
    }

    fun toggleProviderSubscription(providerId: String, status: String = "") {
        viewModelScope.launch {
            val provider = _providers.value.find { it.id == providerId } ?: return@launch
            val nextStatus = if (status.isNotBlank()) status else {
                if (provider.subscriptionStatus == "APPROVED") "PENDING" else "APPROVED"
            }
            providerRepository.saveOrUpdateProvider(provider.copy(subscriptionStatus = nextStatus))
        }
    }

    fun updateProviderEntity(provider: ProviderEntity) {
        viewModelScope.launch {
            providerRepository.saveOrUpdateProvider(provider)
        }
    }

    fun logCall(providerId: String, providerName: String) {
        // Log call compatibility stub
    }

    // Compatibility stubs and implementations for Store, Property, Job, Chat, Orders, and Settings
    fun saveStore(store: StoreEntity) {
        val list = _stores.value.toMutableList()
        val index = list.indexOfFirst { it.id == store.id }
        if (index >= 0) {
            list[index] = store
        } else {
            list.add(store)
        }
        _stores.value = list
        triggerNotification("🏪 تم حفظ المتجر بنجاح")
    }
    fun deleteStore(storeId: String) {
        val list = _stores.value.toMutableList()
        list.removeAll { it.id == storeId }
        _stores.value = list
    }
    fun deleteStorePermanently(storeId: String) {
        deleteStore(storeId)
    }
    fun saveProduct(prod: ProductEntity) {
        val list = _products.value.toMutableList()
        val index = list.indexOfFirst { it.id == prod.id }
        if (index >= 0) {
            list[index] = prod
        } else {
            list.add(prod)
        }
        _products.value = list
    }
    fun saveProperty(property: PropertyEntity) {
        val list = _properties.value.toMutableList()
        val index = list.indexOfFirst { it.id == property.id }
        if (index >= 0) {
            list[index] = property
        } else {
            list.add(property)
        }
        _properties.value = list
        triggerNotification("🏢 تم حفظ العقار بنجاح")
    }
    fun deleteProperty(propertyId: String) {
        val list = _properties.value.toMutableList()
        list.removeAll { it.id == propertyId }
        _properties.value = list
    }
    fun deletePropertyPermanently(propertyId: String) {
        deleteProperty(propertyId)
    }
    fun addRating(rating: RatingEntity) {
        val list = _ratings.value.toMutableList()
        list.add(rating)
        _ratings.value = list
    }
    fun requestPasswordRecoveryForStore(name: String, phone: String, pass: String) {}
    fun requestPasswordRecoveryForProperty(name: String, phone: String, pass: String) {}
    fun saveCustomSettingsState(settings: AdminSettingsEntity) {
        _settings.value = settings
    }
    fun approveStorePdf(storeId: String, approved: Boolean) {}
    fun setStoreVip(storeId: String, vip: Boolean) {}
    fun setStoreVerified(storeId: String, verified: Boolean) {}
    fun setStoreRecommended(storeId: String, recommended: Boolean) {}
    fun setStoreChatDisabled(storeId: String, disabled: Boolean) {}
    fun setStoreNotificationsDisabled(storeId: String, disabled: Boolean) {}
    fun toggleBlockStore(storeId: String) {}
    fun setStorePinned(storeId: String, pinned: Boolean) {}
    fun approvePropertyPdf(propId: String, approved: Boolean) {}
    fun setPropertyVip(propId: String, vip: Boolean) {}
    fun setPropertyVerified(propId: String, verified: Boolean) {}
    fun setPropertyRecommended(propId: String, recommended: Boolean) {}
    fun setPropertyChatDisabled(propId: String, disabled: Boolean) {}
    fun setPropertyNotificationsDisabled(propId: String, disabled: Boolean) {}
    fun setPropertyPinned(propId: String, pinned: Boolean) {}
    fun restoreStore(storeId: String) {}
    fun restoreProperty(propId: String) {}
    fun updateOrderStatus(orderId: String, status: String) {}
    fun setStoreActive(storeId: String, active: Boolean) {}
    fun setPropertyActive(propId: String, active: Boolean) {}
    fun pinProvider(providerId: String, pinned: Boolean) {
        toggleProviderPin(providerId)
    }
    fun verifyProviderBadge(providerId: String, verified: Boolean) {
        toggleProviderVerification(providerId)
    }
    fun recommendProvider(providerId: String, recommended: Boolean) {
        toggleProviderRecommendation(providerId)
    }

    // Orders, Chat and Reports
    fun placeOrder(order: OrderEntity) {
        val list = _orders.value.toMutableList()
        list.add(order)
        _orders.value = list
        triggerNotification("📦 تم تقديم طلب الشراء بنجاح")
    }

    fun sendMessageInChat(messageText: String) {
        val active = _activeChatChannel.value ?: return
        val entity = ChatMessageEntity(
            id = UUID.randomUUID().toString(),
            senderId = _currentUserId.value,
            senderName = _currentUserName.value,
            message = messageText,
            timestamp = System.currentTimeMillis()
        )
        val updated = _chatMessages.value.toMutableList()
        updated.add(entity)
        _chatMessages.value = updated
        triggerNotification("✉️ تم إرسال الرسالة بنجاح")
    }

    fun submitReport(report: ReportEntity, onResult: () -> Unit = {}) {
        val list = _reports.value.toMutableList()
        list.add(report)
        _reports.value = list
        triggerNotification("🚩 تم تقديم البلاغ بنجاح")
        onResult()
    }

    // Banners
    fun addBanner(banner: BannerEntity) {
        val list = _banners.value.toMutableList()
        list.add(banner)
        _banners.value = list
    }
    fun addNewBanner(banner: BannerEntity) {
        addBanner(banner)
    }
    fun deleteBanner(bannerId: String) {
        val list = _banners.value.toMutableList()
        list.removeAll { it.id == bannerId }
        _banners.value = list
    }
    fun reorderBanners(bannersList: List<BannerEntity>) {
        _banners.value = bannersList
    }

    // Jobs
    fun setJobVip(jobId: String, vip: Boolean) {
        val list = _jobs.value.map {
            if (it.id == jobId) it.copy(isVip = vip) else it
        }
        _jobs.value = list
    }
    fun setJobPinned(jobId: String, pinned: Boolean) {
        val list = _jobs.value.map {
            if (it.id == jobId) it.copy(isPinned = pinned) else it
        }
        _jobs.value = list
    }
    fun setJobChatDisabled(jobId: String, disabled: Boolean) {
        val list = _jobs.value.map {
            if (it.id == jobId) it.copy(isChatDisabled = disabled) else it
        }
        _jobs.value = list
    }
    fun setJobBlocked(jobId: String, blocked: Boolean) {
        val list = _jobs.value.map {
            if (it.id == jobId) it.copy(isBlocked = blocked) else it
        }
        _jobs.value = list
    }
    fun setJobApproved(jobId: String, approved: Boolean) {
        val list = _jobs.value.map {
            if (it.id == jobId) it.copy(isApproved = approved) else it
        }
        _jobs.value = list
    }
    fun deleteJobPermanently(jobId: String) {
        val list = _jobs.value.toMutableList()
        list.removeAll { it.id == jobId }
        _jobs.value = list
    }
    fun saveJob(job: JobEntity) {
        val list = _jobs.value.toMutableList()
        val index = list.indexOfFirst { it.id == job.id }
        if (index >= 0) {
            list[index] = job
        } else {
            list.add(job)
        }
        _jobs.value = list
    }

    // Supervisors
    fun addSupervisor(name: String, role: String, passcode: String, permissions: List<String>) {
        val entity = com.example.data.models.SupervisorEntity(
            id = UUID.randomUUID().toString(),
            name = name,
            role = role,
            passcode = passcode,
            permissions = permissions
        )
        val list = _supervisors.value.toMutableList()
        list.add(entity)
        _supervisors.value = list
        triggerNotification("🔑 تم إضافة المشرف $name بنجاح")
    }

    fun editSupervisor(id: String, name: String, role: String, passcode: String, permissions: List<String>) {
        val list = _supervisors.value.map {
            if (it.id == id) {
                it.copy(name = name, role = role, passcode = passcode, permissions = permissions)
            } else {
                it
            }
        }
        _supervisors.value = list
        triggerNotification("💾 تم تحديث بيانات المشرف بنجاح")
    }

    fun removeSupervisor(id: String) {
        val list = _supervisors.value.toMutableList()
        list.removeAll { it.id == id }
        _supervisors.value = list
        triggerNotification("🔑 تم إزالة المشرف بنجاح")
    }

    fun setSupervisorSession(sup: com.example.data.models.SupervisorEntity?) {}

    // Bookings implementation helpers
    fun updateBookingImpl(booking: BookingEntity, inputPin: String = "") {
        viewModelScope.launch {
            bookingRepository.updateBooking(booking, inputPin)
        }
    }

    fun attemptCancelBookingImpl(bookingId: String, input: String, reason: String, onResult: (Boolean, String) -> Unit = {_,_ ->}) {
        viewModelScope.launch {
            val booking = _bookings.value.find { it.id == bookingId }
            if (booking == null) {
                onResult(false, "الحجز غير موجود")
                return@launch
            }
            bookingRepository.attemptCancel(booking, input, reason).fold(
                onSuccess = { onResult(true, "تم إلغاء الحجز بنجاح") },
                onFailure = { onResult(false, it.localizedMessage ?: "فشل إلغاء الحجز") }
            )
        }
    }

    fun deleteBookingImpl(bookingId: String) {
        viewModelScope.launch {
            bookingRepository.deleteBooking(bookingId)
        }
    }

    // Chat routing and setup
    fun openOrCreateChatChannel(
        targetId: String,
        targetType: String,
        targetName: String,
        targetPhone: String,
        targetCategory: String = "",
        relatedEntityId: String = "",
        relatedEntityType: String = "",
        onCreated: (ChatChannelEntity?) -> Unit = {},
        onResult: (ChatChannelEntity?) -> Unit = {}
    ) {
        val channelId = "chat_${targetType.lowercase()}_${targetId}_user_${_currentUserId.value}"
        val channel = ChatChannelEntity(
            id = channelId,
            targetId = targetId,
            targetName = targetName,
            targetPhone = targetPhone,
            targetCategory = targetCategory,
            relatedEntityId = relatedEntityId,
            relatedEntityType = relatedEntityType,
            timestamp = System.currentTimeMillis()
        )
        val updatedChannels = _chatChannels.value.toMutableList()
        if (updatedChannels.none { it.id == channelId }) {
            updatedChannels.add(channel)
            _chatChannels.value = updatedChannels
        }
        _activeChatChannel.value = channel
        onCreated(channel)
        onResult(channel)
    }

    fun openChatChannel(channel: ChatChannelEntity?) {
        _activeChatChannel.value = channel
    }
    fun setProviderChatDisabled(providerId: String, disabled: Boolean) {}
    fun setProviderNotificationsDisabled(providerId: String, disabled: Boolean) {}
    fun setProviderPaymentRequired(providerId: String, required: Boolean) {}
    fun exportJobApplicantsCsv(context: Context) {}
    fun acceptJobApplication(applicationId: String) {}
    fun deleteJobApplication(applicationId: String) {}
    fun sendNotificationToApplicants(title: String, message: String) {}
    fun rejectJobApplication(applicationId: String, reason: String) {}
    fun unbanEntity(type: String, id: String) {}
    fun restoreEntity(type: String, id: String) {}
    fun hardDeleteEntity(type: String, id: String) {}
    fun restoreJob(jobId: String) {}
    fun restoreProvider(providerId: String) {}
    fun deleteJob(jobId: String) {}
    fun removeProvider(providerId: String) {
        viewModelScope.launch { providerRepository.deleteProvider(providerId) }
    }
    fun initializeUserIdentity(context: Context) {}
    fun updateUserFcmToken(token: String) {}
    fun updateUserFcmToken(userId: String, token: String) {}
    fun clearNotification() {
        _toastFlow.value = null
    }
    fun clearNotification(id: String) {
        _toastFlow.value = null
    }
    fun retryConnection() {}
    fun retryConnection(context: Context) {}
    fun requestPasswordRecoveryGeneral(phone: String, pass: String) {}
    fun requestPasswordRecoveryGeneral(
        accountName: String,
        phone: String,
        accountType: String,
        currentPassword: String
    ) {
        // compatibility stub
    }

    fun getOrCreateChatChannel(otherUserId: String, otherUserName: String, otherUserPhoto: String) {}
    fun getOrCreateChatChannel(
        providerId: String = "",
        providerName: String = "",
        customerId: String = "",
        customerName: String = ""
    ) {
        val chId = "chat_p_${providerId}_u_${customerId}"
        _activeChatChannel.value = ChatChannelEntity(
            id = chId,
            providerId = providerId,
            customerId = customerId,
            targetId = providerId,
            targetName = providerName,
            customerName = customerName,
            timestamp = System.currentTimeMillis()
        )
    }

    fun setUserSessionDetails(id: String, phone: String, name: String, residence: String) {}
    fun setUserSessionDetails(context: Context, name: String, phone: String, residence: String) {
        _currentUserId.value = phone
        _currentUserPhone.value = phone
        _currentUserName.value = name
        _currentUserResidence.value = residence
        val sp = context.getSharedPreferences("yemen_service_prefs", Context.MODE_PRIVATE)
        sp.edit()
            .putString("user_id", phone)
            .putString("user_phone", phone)
            .putString("user_name", name)
            .putString("user_residence", residence)
            .apply()
    }

    fun setJoinRequestPhone(phone: String) {}
    fun setJoinRequestPhone(context: Context, phone: String) {}

    fun startVoiceCall(name: String, role: String) {
        _activeVoiceCall.value = Pair(name, role)
    }
    fun endVoiceCall() {
        _activeVoiceCall.value = null
    }

    fun verifyAdminOrOwnerPassword(password: String): Boolean = true

    // Admin backdoor authentication helper
    fun authenticateAdmin(role: String) {
        _adminRole.value = role
        triggerNotification("🔓 تم تسجيل الدخول بنجاح بصلاحية: $role")
        _currentScreen.value = "ADMIN_PANEL"
    }

    fun logout(context: Context) {
        _adminRole.value = "GUEST"
        _currentScreen.value = "USER_BROWSE"
        val sp = context.getSharedPreferences("yemen_service_prefs", Context.MODE_PRIVATE)
        sp.edit().putString("saved_admin_role", "GUEST").apply()
        triggerNotification("🔒 تم تسجيل الخروج بنجاح")
    }

    fun dismissBackdoorDialog() {
        _showBackdoorDialog.value = false
    }

    fun showBackdoorDialog() {
        _showBackdoorDialog.value = true
    }

    fun closeActiveChatChannel() {
        _activeChatChannel.value = null
    }

    fun registerGuestUser(context: Context, name: String, phone: String, residence: String, password: String) {
        _currentUserId.value = phone
        _currentUserPhone.value = phone
        _currentUserName.value = name
        _currentUserResidence.value = residence
        val sp = context.getSharedPreferences("yemen_service_prefs", Context.MODE_PRIVATE)
        sp.edit()
            .putString("user_id", phone)
            .putString("user_phone", phone)
            .putString("user_name", name)
            .putString("user_residence", residence)
            .apply()
    }

    fun registerBackdoorInteraction() {}

    fun sendReport(targetId: String, targetName: String, reporterName: String, reason: String) {}

    fun submitRating(rating: RatingEntity, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            // locally add rating to flow or trigger notification
            val list = _ratings.value.toMutableList()
            list.add(rating)
            _ratings.value = list
            onSuccess()
        }
    }
}
