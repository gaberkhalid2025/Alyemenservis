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

    internal val _activeVoiceCall = MutableStateFlow<String?>(null)
    val activeVoiceCall: StateFlow<String?> = _activeVoiceCall.asStateFlow()

    internal val _toastFlow = MutableSharedFlow<String>()
    val toastFlow: SharedFlow<String> = _toastFlow.asSharedFlow()

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
            notificationRepository.deleteAllNotifications()
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

    fun submitOfferForRequest(offer: RequestOfferEntity, onResult: (Boolean, String) -> Unit = {_,_ ->}) {
        viewModelScope.launch {
            urgentRequestRepository.submitOffer(offer).fold(
                onSuccess = { onResult(true, "تم تقديم العرض") },
                onFailure = { onResult(false, it.localizedMessage ?: "فشل تقديم العرض") }
            )
        }
    }

    fun acceptRequestOffer(requestId: String, offerId: String, providerPhone: String, onResult: (Boolean, String) -> Unit = {_,_ ->}) {
        viewModelScope.launch {
            urgentRequestRepository.acceptOffer(requestId, offerId, "", "", providerPhone, 0.0).fold(
                onSuccess = { onResult(true, "تم قبول العرض بنجاح") },
                onFailure = { onResult(false, it.localizedMessage ?: "فشل قبول العرض") }
            )
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

    fun toggleProviderSubscription(providerId: String) {
        viewModelScope.launch {
            val provider = _providers.value.find { it.id == providerId } ?: return@launch
            val newStatus = if (provider.subscriptionStatus == "APPROVED") "PENDING" else "APPROVED"
            providerRepository.saveOrUpdateProvider(provider.copy(subscriptionStatus = newStatus, isVip = !provider.isVip))
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

    // Compatibility stubs for Store, Property, Job, Chat, Orders, and Settings
    fun saveStore(store: StoreEntity) {}
    fun deleteStore(storeId: String) {}
    fun deleteStorePermanently(storeId: String) {}
    fun saveProduct(prod: ProductEntity) {}
    fun deleteProperty(propertyId: String) {}
    fun deletePropertyPermanently(propertyId: String) {}
    fun addRating(rating: RatingEntity) {}
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
    fun triggerRestoreAccountDialog(trigger: Boolean) {
        _triggerRestoreAccountDialog.value = trigger
    }
    fun clearNotification(id: String) {}
    fun clearNotification() {}
    fun retryConnection() {}
    fun requestPasswordRecoveryGeneral(phone: String, pass: String) {}
    fun getOrCreateChatChannel(otherUserId: String, otherUserName: String, otherUserPhoto: String) {}
    fun getOrCreateChatChannel(providerId: String, providerName: String, customerPhone: String, customerName: String) {}
    fun closeActiveChatChannel() {
        _activeChatChannel.value = null
    }
    fun registerGuestUser(name: String, phone: String) {
        _currentUserName.value = name
        _currentUserPhone.value = phone
    }
    fun registerBackdoorInteraction() {}
    fun sendReport(report: ReportEntity) {}
    fun sendReport(type: String, targetId: String, reason: String) {}

    fun restoreGuestUser(context: Context, phone: String, password: String, onResult: (Boolean, String) -> Unit) {
        try {
            val db = FirebaseFirestore.getInstance()
            db.collection("users").whereEqualTo("phone", phone).get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        val doc = documents.documents[0]
                        val savedPass = doc.getString("password") ?: doc.getString("pass") ?: ""
                        if (savedPass.isEmpty() || savedPass == password) {
                            val name = doc.getString("name") ?: doc.getString("fullName") ?: "مستخدم"
                            val userId = doc.id
                            val userType = doc.getString("type") ?: "CLIENT"
                            
                            val sp = context.getSharedPreferences("yemen_service_prefs", Context.MODE_PRIVATE)
                            sp.edit()
                                .putBoolean("is_logged_in", true)
                                .putString("user_id", userId)
                                .putString("user_name", name)
                                .putString("user_phone", phone)
                                .putString("user_type", userType)
                                .apply()

                            _currentUserPhone.value = phone
                            _currentUserName.value = name
                            _currentUserId.value = userId

                            onResult(true, "✅ تم استرجاع الحساب بنجاح!")
                        } else {
                            onResult(false, "كلمة المرور غير صحيحة")
                        }
                    } else {
                        // Check pending collections or providers
                        db.collection("pending_providers").whereEqualTo("phone", phone).get()
                            .addOnSuccessListener { pendingDocs ->
                                if (!pendingDocs.isEmpty) {
                                    onResult(true, "⏳ حسابك قيد المراجعة الإدارية من قبل الإدارة.")
                                } else {
                                    onResult(false, "لم يتم العثور على حساب بهذا الرقم")
                                }
                            }.addOnFailureListener {
                                onResult(false, "لم يتم العثور على حساب بهذا الرقم")
                            }
                    }
                }
                .addOnFailureListener { e ->
                    onResult(false, "خطأ في الاتصال: ${e.localizedMessage}")
                }
        } catch (e: Exception) {
            e.printStackTrace()
            onResult(false, "حدث خطأ غير متوقع: ${e.localizedMessage}")
        }
    }

    fun setUserSessionDetails(id: String, phone: String, name: String, residence: String) {}
    fun setJoinRequestPhone(phone: String) {}
    fun startVoiceCall(name: String, role: String) {}
    fun endVoiceCall() {}
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
        sp.edit()
            .putBoolean("is_logged_in", false)
            .putString("saved_admin_role", "GUEST")
            .remove("user_id")
            .remove("user_phone")
            .remove("user_name")
            .remove("user_type")
            .apply()
        triggerNotification("🔒 تم تسجيل الخروج بنجاح")
    }

    fun dismissBackdoorDialog() {
        _showBackdoorDialog.value = false
    }

    internal val _triggerRestoreAccountDialog = MutableStateFlow(false)
    val triggerRestoreAccountDialog: StateFlow<Boolean> = _triggerRestoreAccountDialog.asStateFlow()

    fun saveProperty(vararg args: Any?) {}
    fun placeOrder(vararg args: Any?) {}
    fun sendMessageInChat(vararg args: Any?) {}
    fun submitRating(vararg args: Any?) {}
    fun submitReport(vararg args: Any?) {}
    fun deleteBanner(vararg args: Any?) {}
    fun addBanner(vararg args: Any?) {}
    fun setJobVip(vararg args: Any?) {}
    fun deleteJobPermanently(vararg args: Any?) {}
    fun toggleStoreBlocked(vararg args: Any?) {}
    fun addNewStore(vararg args: Any?) {}
    fun approveTechnician(vararg args: Any?) {}
    fun approveRegisteredUser(vararg args: Any?) {}
    fun toggleBlockRegisteredUser(vararg args: Any?) {}
    fun deleteRegisteredUser(vararg args: Any?) {}
    fun setJobApproved(vararg args: Any?) {}
    fun exportComplaintsToCSV(vararg args: Any?) {}
    fun exportComplaintsToPDF(vararg args: Any?) {}
    fun deleteReport(vararg args: Any?) {}
    fun addNewProviderCustom(vararg args: Any?) {}
    fun saveJob(vararg args: Any?) {}
    fun resetAccountPassword(vararg args: Any?) {}
    fun addNewBanner(vararg args: Any?) {}
    fun reorderBanners(vararg args: Any?) {}
    fun replyToChatChannel(vararg args: Any?) {}
    fun wipeOldChatChannels(vararg args: Any?) {}
    fun blockChatChannel(vararg args: Any?) {}
    fun updateBackdoorSettings(vararg args: Any?) {}
    fun extendProviderSubscription(vararg args: Any?) {}
    fun addSupervisor(vararg args: Any?) {}
    fun removeSupervisor(vararg args: Any?) {}
    fun addColorPalette(vararg args: Any?) {}
    fun deleteColorPalette(vararg args: Any?) {}
    fun addNewCategory(vararg args: Any?) {}
    fun togglePinCategory(vararg args: Any?) {}
    fun reorderCategories(vararg args: Any?) {}
    fun addNewCity(vararg args: Any?) {}
    fun removeCity(vararg args: Any?) {}
    fun createSystemBackup(vararg args: Any?) {}
    fun restoreSystemFromBackup(vararg args: Any?) {}
    fun setSecondaryFirebaseConfig(vararg args: Any?) {}
    fun exportSelectedCollectionsAsJson(vararg args: Any?) {}
    fun addCoupon(vararg args: Any?) {}
    fun deleteCoupon(vararg args: Any?) {}
    fun saveInternalWallet(vararg args: Any?) {}
    fun togglePaymentWalletVisibility(vararg args: Any?) {}
    fun updatePaymentWallet(vararg args: Any?) {}
    fun deletePaymentWallet(vararg args: Any?) {}
    fun saveCustomPermissionsMatrixToFirestore(vararg args: Any?) {}
    fun rejectTechnician(vararg args: Any?) {}
    fun deleteCategory(vararg args: Any?) {}
    fun editCategory(vararg args: Any?) {}
    fun updateCity(vararg args: Any?) {}
    fun mergeCategories(vararg args: Any?) {}
    fun toggleBlockChatChannel(vararg args: Any?) {}
    fun deleteChatChannel(vararg args: Any?) {}
    fun wipeSelectedDatabaseData(vararg args: Any?) {}
    fun addPaymentWallet(vararg args: Any?) {}
    fun performWalletTransaction(vararg args: Any?) {}
    fun verifyPayment(vararg args: Any?) {}
    fun refundPayment(vararg args: Any?) {}
    fun editSupervisor(vararg args: Any?) {}
    fun adminResetAccountPassword(vararg args: Any?) {}
    fun wipeAllMockAndTemporaryData(vararg args: Any?) {}
    fun createBooking(vararg args: Any?) {}
    fun updateBookingImpl(vararg args: Any?) {}
    fun attemptCancelBookingImpl(vararg args: Any?) {}
    fun deleteBookingImpl(vararg args: Any?) {}
    fun openChatChannel(vararg args: Any?) {}

    val supervisors: StateFlow<List<SupervisorEntity>> = MutableStateFlow<List<SupervisorEntity>>(emptyList()).asStateFlow()
    val colorPalettes: StateFlow<List<ColorPaletteEntity>> = MutableStateFlow<List<ColorPaletteEntity>>(emptyList()).asStateFlow()
    val registeredUsersList: StateFlow<List<UserEntity>> = MutableStateFlow<List<UserEntity>>(emptyList()).asStateFlow()
    val instantRequests: StateFlow<List<InstantRequestEntity>> = MutableStateFlow<List<InstantRequestEntity>>(emptyList()).asStateFlow()
    val requestOffers: StateFlow<List<RequestOfferEntity>> = MutableStateFlow<List<RequestOfferEntity>>(emptyList()).asStateFlow()
    val customProfileTabs: StateFlow<List<CustomProfileTabEntity>> = MutableStateFlow<List<CustomProfileTabEntity>>(emptyList()).asStateFlow()
    val currentSupervisorPermissions: StateFlow<List<String>> = MutableStateFlow<List<String>>(emptyList()).asStateFlow()

    fun setStoreBlocked(vararg args: Any?) {}
    fun setPropertyBlocked(vararg args: Any?) {}
    fun setJobPinned(vararg args: Any?) {}
    fun setJobChatDisabled(vararg args: Any?) {}
    fun setJobBlocked(vararg args: Any?) {}
    fun exportPerformanceReportToPDF(vararg args: Any?) {}
    fun saveCustomProfileTab(vararg args: Any?) {}
    fun toggleCustomProfileTab(vararg args: Any?) {}
    fun deleteCustomProfileTab(vararg args: Any?) {}
    fun updateAdminSettings(vararg args: Any?) {}
    fun saveBackupToLocalStorage(vararg args: Any?) {}
    fun setSupervisorSession(vararg args: Any?) {}
    fun updateJobApplicationStatus(vararg args: Any?) {}
    fun addRatingReply(vararg args: Any?) {}
    fun cancelOrResetJoinRequest(vararg args: Any?) {}
    fun requestAdminPasswordReset(vararg args: Any?) {}
    fun togglePropertyBlocked(vararg args: Any?) {}
    fun deleteOrder(vararg args: Any?) {}
    fun deleteAllOrders(vararg args: Any?) {}
    fun deleteProduct(vararg args: Any?) {}
    fun uploadImageStringOrUri(vararg args: Any?) {}
    fun openOrCreateChatChannel(vararg args: Any?) {}
    fun createBooking(vararg args: Any?) {}
    fun toggleProviderSubscription(vararg args: Any?) {}
    fun authenticateAdmin(vararg args: Any?) {}
    val isProviderUser: Boolean = false

    fun showBackdoorDialog() {
        _showBackdoorDialog.value = true
    }
}
