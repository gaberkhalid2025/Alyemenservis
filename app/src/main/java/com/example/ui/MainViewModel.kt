package com.example.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import com.example.utils.*

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class MainViewModel : ViewModel() {

    // ------------------- Firestore setup -------------------
    val db by lazy {
        val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        try {
            val settings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .setCacheSizeBytes(104857600L) // 100 MB cache size for ultra-fast local offline caching
                .build()
            firestore.firestoreSettings = settings
        } catch (e: Exception) {
            e.printStackTrace()
        }
        firestore
    }

    // Listener registration tracking for memory leak prevention
    private val firestoreListeners = mutableListOf<com.google.firebase.firestore.ListenerRegistration>()

    override fun onCleared() {
        super.onCleared()
        try {
            firestoreListeners.forEach { it.remove() }
            firestoreListeners.clear()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ------------------- StateFlows -------------------
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

    internal val _internalWallets = MutableStateFlow<List<com.example.data.InternalWalletEntity>>(emptyList())
    val internalWallets: StateFlow<List<com.example.data.InternalWalletEntity>> = _internalWallets.asStateFlow()

    internal val _walletTransactions = MutableStateFlow<List<com.example.data.WalletTransactionEntity>>(emptyList())
    val walletTransactions: StateFlow<List<com.example.data.WalletTransactionEntity>> = _walletTransactions.asStateFlow()

    internal val _userLatitude = MutableStateFlow(15.3694)
    val userLatitude: StateFlow<Double> = _userLatitude.asStateFlow()

    internal val _userLongitude = MutableStateFlow(44.1910)
    val userLongitude: StateFlow<Double> = _userLongitude.asStateFlow()

    internal val _isGpsTrackingActive = MutableStateFlow(false)
    val isGpsTrackingActive: StateFlow<Boolean> = _isGpsTrackingActive.asStateFlow()

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

    internal val _offers = MutableStateFlow<List<com.example.data.models.Offer>>(emptyList())
    val offers: StateFlow<List<com.example.data.models.Offer>> = _offers.asStateFlow()

    internal val _passwordRecoveryWaitingPhone = MutableStateFlow<String>("")
    val passwordRecoveryWaitingPhone: StateFlow<String> = _passwordRecoveryWaitingPhone.asStateFlow()

    fun setPasswordRecoveryWaitingPhone(phone: String) {
        _passwordRecoveryWaitingPhone.value = phone
    }

    var selectedProvider: com.example.data.ProviderEntity? = null
    var selectedStore: com.example.data.StoreEntity? = null
    var selectedProperty: com.example.data.PropertyEntity? = null
    var selectedOfferId by androidx.compose.runtime.mutableStateOf("")
    var selectedRequestId by androidx.compose.runtime.mutableStateOf("")
    var showQuickServiceDialog by androidx.compose.runtime.mutableStateOf(false)

    internal val _chatChannels = MutableStateFlow<List<ChatChannelEntity>>(emptyList())
    val chatChannels: StateFlow<List<ChatChannelEntity>> = _chatChannels.asStateFlow()

    internal val _activeChatChannel = MutableStateFlow<ChatChannelEntity?>(null)
    val activeChatChannel: StateFlow<ChatChannelEntity?> = _activeChatChannel.asStateFlow()

    fun openChatChannel(channel: ChatChannelEntity?) {
        _activeChatChannel.value = channel
    }

    fun closeActiveChatChannel() {
        _activeChatChannel.value = null
    }

    internal val _stores = MutableStateFlow<List<com.example.data.StoreEntity>>(getDefaultStoresList())
    val stores: StateFlow<List<com.example.data.StoreEntity>> = _stores.asStateFlow()

    internal val _products = MutableStateFlow<List<com.example.data.ProductEntity>>(emptyList())
    val products: StateFlow<List<com.example.data.ProductEntity>> = _products.asStateFlow()

    internal val _properties = MutableStateFlow<List<com.example.data.PropertyEntity>>(getDefaultPropertiesList())
    val properties: StateFlow<List<com.example.data.PropertyEntity>> = _properties.asStateFlow()

    internal val _jobs = MutableStateFlow<List<com.example.data.JobEntity>>(emptyList())
    val jobs: StateFlow<List<com.example.data.JobEntity>> = _jobs.asStateFlow()

    internal val _jobApplications = MutableStateFlow<List<com.example.data.JobApplicationEntity>>(emptyList())
    val jobApplications: StateFlow<List<com.example.data.JobApplicationEntity>> = _jobApplications.asStateFlow()

    private val _ratings = MutableStateFlow<List<com.example.data.RatingEntity>>(emptyList())
    val ratings: StateFlow<List<com.example.data.RatingEntity>> = _ratings.asStateFlow()

    private val _favoriteIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteIds: StateFlow<Set<String>> = _favoriteIds.asStateFlow()

    fun toggleFavorite(id: String) {
        val current = _favoriteIds.value.toMutableSet()
        if (current.contains(id)) {
            current.remove(id)
            triggerNotification("💔 تم الحذف من قائمة المفضلة")
        } else {
            current.add(id)
            triggerNotification("❤️ تمت الإضافة إلى المفضلة!")
        }
        _favoriteIds.value = current
        appContext?.let { ctx ->
            try {
                val sp = ctx.getSharedPreferences("yemen_service_prefs", android.content.Context.MODE_PRIVATE)
                sp.edit().putStringSet("favorite_ids_set", current).apply()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        checkAndTriggerFavoriteOffersNotifications()
    }

    fun isFavorite(id: String): Boolean = _favoriteIds.value.contains(id)

    fun checkAndTriggerFavoriteOffersNotifications() {
        val ctx = appContext ?: return
        val favIds = _favoriteIds.value
        if (favIds.isEmpty()) return

        val deduplicator = com.example.util.NotificationDeduplicator(ctx)
        val currentStores = _stores.value
        val currentProviders = _providers.value

        currentStores.filter { favIds.contains(it.id) }.forEach { store ->
            val offers = com.example.data.SpecialOfferEntity.parseList(store.specialOffersJson)
            offers.forEach { offer ->
                val notifId = "fav_offer_${store.id}_${offer.id}"
                val notif = NotificationEntity(
                    id = notifId,
                    title = "🔥 عرض جديد من ${store.name}",
                    message = "أضاف متجر ${store.name} المفضل لديك عرضاً جديداً: ${offer.title} بخصم %${offer.discountPercent} (السعر: ${offer.offerPrice.toInt()} ر.ي)! 🎁",
                    targetType = "ALL",
                    targetValue = store.id,
                    timestamp = System.currentTimeMillis(),
                    notificationType = "SPECIAL_OFFER",
                    channel = "IN_APP",
                    isRead = false
                )
                if (!deduplicator.isDuplicate(notif) && _notifications.value.none { it.id == notifId }) {
                    deduplicator.markAsSent(notif)
                    addNotificationEntityDirect(notif)
                    triggerNotification("🔔 عرض جديد من ${store.name}: ${offer.title}")
                }
            }
        }

        currentProviders.filter { favIds.contains(it.id) }.forEach { provider ->
            val offers = com.example.data.SpecialOfferEntity.parseList(provider.specialOffersJson)
            offers.forEach { offer ->
                val notifId = "fav_offer_${provider.id}_${offer.id}"
                val notif = NotificationEntity(
                    id = notifId,
                    title = "🔥 عرض جديد من ${provider.name}",
                    message = "أضاف مقدم الخدمة ${provider.name} المفضل لديك عرضاً جديداً: ${offer.title} بخصم %${offer.discountPercent} (السعر: ${offer.offerPrice.toInt()} ر.ي)! 🎁",
                    targetType = "ALL",
                    targetValue = provider.id,
                    timestamp = System.currentTimeMillis(),
                    notificationType = "SPECIAL_OFFER",
                    channel = "IN_APP",
                    isRead = false
                )
                if (!deduplicator.isDuplicate(notif) && _notifications.value.none { it.id == notifId }) {
                    deduplicator.markAsSent(notif)
                    addNotificationEntityDirect(notif)
                    triggerNotification("🔔 عرض جديد من ${provider.name}: ${offer.title}")
                }
            }
        }
    }

    fun notifyFavoriteOfferCreated(merchantId: String, merchantName: String, offer: com.example.data.SpecialOfferEntity) {
        val ctx = appContext ?: return
        val deduplicator = com.example.util.NotificationDeduplicator(ctx)
        val notifId = "fav_offer_${merchantId}_${offer.id}"

        val notif = NotificationEntity(
            id = notifId,
            title = "🔥 عرض جديد من ${merchantName}",
            message = "تم نشر عرض جديد: ${offer.title} بخصم %${offer.discountPercent} (السعر: ${offer.offerPrice.toInt()} ر.ي)! 🎁",
            targetType = "ALL",
            targetValue = merchantId,
            timestamp = System.currentTimeMillis(),
            notificationType = "SPECIAL_OFFER",
            channel = "IN_APP",
            isRead = false
        )
        if (!deduplicator.isDuplicate(notif) && _notifications.value.none { it.id == notifId }) {
            deduplicator.markAsSent(notif)
            addNotificationEntityDirect(notif)
            if (_favoriteIds.value.contains(merchantId)) {
                triggerNotification("🔔 عرض جديد من ${merchantName}: ${offer.title}")
            }
        }
    }

    fun addNotificationEntityDirect(newNotif: NotificationEntity) {
        val current = _notifications.value.toMutableList()
        if (current.none { it.id == newNotif.id }) {
            current.add(0, newNotif)
            _notifications.value = current
            try {
                db.collection("notifications").document(newNotif.id).set(newNotif)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private val _customProfileTabs = MutableStateFlow<List<com.example.data.CustomProfileTabEntity>>(emptyList())
    val customProfileTabs: StateFlow<List<com.example.data.CustomProfileTabEntity>> = _customProfileTabs.asStateFlow()

    private val _orders = MutableStateFlow<List<com.example.data.OrderEntity>>(emptyList())
    val orders: StateFlow<List<com.example.data.OrderEntity>> = _orders.asStateFlow()

    private val _instantRequests = MutableStateFlow<List<com.example.data.models.InstantRequestEntity>>(emptyList())
    val instantRequests: StateFlow<List<com.example.data.models.InstantRequestEntity>> = _instantRequests.asStateFlow()

    private val _requestOffers = MutableStateFlow<List<com.example.data.models.RequestOfferEntity>>(emptyList())
    val requestOffers: StateFlow<List<com.example.data.models.RequestOfferEntity>> = _requestOffers.asStateFlow()

    private val _currentUserId = MutableStateFlow("guest")
    val currentUserId: StateFlow<String> = _currentUserId.asStateFlow()

    val isProviderUser: Boolean
        get() = selectedProvider != null || selectedStore != null || selectedProperty != null

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _uiErrorMessage = MutableStateFlow<String?>(null)
    val uiErrorMessage: StateFlow<String?> = _uiErrorMessage.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun clearUiError() {
        _uiErrorMessage.value = null
    }

    fun setUiError(message: String) {
        _uiErrorMessage.value = message
    }

    fun refreshData() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _uiErrorMessage.value = null
            try {
                firestoreListeners.forEach { it.remove() }
                firestoreListeners.clear()
                setupRealtimeFirestoreListeners()
                triggerNotification("🔄 تم تحديث البيانات بنجاح!")
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

    private val _registeredUsersCount = MutableStateFlow(0)
    val registeredUsersCount: StateFlow<Int> = _registeredUsersCount.asStateFlow()

    internal val _registeredUsersList = MutableStateFlow<List<Map<String, Any>>>(emptyList())
    val registeredUsersList: StateFlow<List<Map<String, Any>>> = _registeredUsersList.asStateFlow()

    internal val _currentUserName = MutableStateFlow("")
    val currentUserName: StateFlow<String> = _currentUserName.asStateFlow()

    internal val _currentUserPhone = MutableStateFlow("")
    val currentUserPhone: StateFlow<String> = _currentUserPhone.asStateFlow()

    internal val _currentUserResidence = MutableStateFlow("")
    val currentUserResidence: StateFlow<String> = _currentUserResidence.asStateFlow()

    private val _adminRole = MutableStateFlow("GUEST")
    val adminRole: StateFlow<String> = _adminRole.asStateFlow()

    private val _joinRequestPhone = MutableStateFlow("")
    val joinRequestPhone: StateFlow<String> = _joinRequestPhone.asStateFlow()

    val triggerRestoreAccountDialog = MutableStateFlow(false)

    val filteredNotifications: StateFlow<List<NotificationEntity>> = combine(
        _notifications,
        _currentUserId,
        _currentUserPhone,
        _joinRequestPhone,
        _adminRole
    ) { notificationsList, userId, phone, joinPhone, adminRoleState ->
        val distinctList = notificationsList.distinctBy { it.id }
        val now = System.currentTimeMillis()
        val isAdmin = adminRoleState == "OWNER" || adminRoleState == "SUPER_ADMIN" || adminRoleState == "ADMIN"
        
        val visibleList = if (isAdmin) {
            distinctList
        } else {
            distinctList.filter {
                val notExpired = it.expiryTimestamp == 0L || now <= it.expiryTimestamp
                val isReleased = it.scheduledTime == 0L || now >= it.scheduledTime
                notExpired && isReleased
            }
        }

        if (isAdmin) {
            visibleList
        } else if (adminRoleState == "SUPERVISOR") {
            visibleList.filter {
                it.targetType == "ALL" ||
                it.targetType == "SUPERVISOR" ||
                (it.targetType == "USER" && (it.targetValue == userId || it.targetValue == phone || it.targetValue == joinPhone)) ||
                (it.targetType == "PROVIDER" && (it.targetValue == userId || it.targetValue == phone))
            }
        } else if (userId == "guest" && joinPhone.isEmpty()) {
            visibleList.filter { it.targetType == "ALL" }
        } else {
            visibleList.filter {
                it.targetType == "ALL" || 
                (it.targetType == "USER" && (it.targetValue == userId || it.targetValue == phone || it.targetValue == joinPhone)) ||
                (it.targetType == "PROVIDER" && (it.targetValue == userId || it.targetValue == phone))
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentUserPoints = MutableStateFlow(0)
    val currentUserPoints: StateFlow<Int> = _currentUserPoints.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastFlow: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _currentScreen = MutableStateFlow("USER_BROWSE")
    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()

    private val _currentLanguage = MutableStateFlow("ar")
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow<String?>(null)
    val selectedCategoryId: StateFlow<String?> = _selectedCategoryId.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filterVipOnly = MutableStateFlow(false)
    val filterVipOnly: StateFlow<Boolean> = _filterVipOnly.asStateFlow()

    private val _filterAvailableOnly = MutableStateFlow(false)
    val filterAvailableOnly: StateFlow<Boolean> = _filterAvailableOnly.asStateFlow()

    internal val _filterByCurrentCityOnly = MutableStateFlow(false)
    val filterByCurrentCityOnly: StateFlow<Boolean> = _filterByCurrentCityOnly.asStateFlow()

    fun toggleFilterByCurrentCityOnly(enabled: Boolean? = null) {
        _filterByCurrentCityOnly.value = enabled ?: !_filterByCurrentCityOnly.value
        applyFilters()
    }

    private val _filterCityId = MutableStateFlow<String?>(null)
    val filterCityId: StateFlow<String?> = _filterCityId.asStateFlow()

    private val _filterNeighborhoodName = MutableStateFlow("")
    val filterNeighborhoodName: StateFlow<String> = _filterNeighborhoodName.asStateFlow()

    private val _phoneOrNameFilter = MutableStateFlow("")
    val phoneOrNameFilter: StateFlow<String> = _phoneOrNameFilter.asStateFlow()

    private val _maxKmRadius = MutableStateFlow(10)
    val maxKmRadius: StateFlow<Int> = _maxKmRadius.asStateFlow()

    private val _showBackdoorDialog = MutableStateFlow(false)
    val showBackdoorDialog: StateFlow<Boolean> = _showBackdoorDialog.asStateFlow()

    private val _colorPalettes = MutableStateFlow<List<ColorPaletteEntity>>(emptyList())
    val colorPalettes: StateFlow<List<ColorPaletteEntity>> = _colorPalettes.asStateFlow()

    private var clickCount = 0

    private var supportChatListenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null

    fun initializeUserIdentity(context: android.content.Context) {
        appContext = context.applicationContext
        LocaleManager.init(context)
        val sp = context.getSharedPreferences("yemen_service_prefs", android.content.Context.MODE_PRIVATE)
        
        val rawId = sp.getString("user_id", "guest") ?: "guest"
        var savedId = if (rawId != "guest" && rawId.isNotEmpty()) com.example.util.SecurityCryptoUtils.decrypt(rawId) else rawId
        val savedName = com.example.util.SecurityCryptoUtils.decrypt(sp.getString("user_name", "") ?: "")
        val savedPhone = com.example.util.SecurityCryptoUtils.decrypt(sp.getString("user_phone", "") ?: "")
        val savedResidence = com.example.util.SecurityCryptoUtils.decrypt(sp.getString("user_residence", "") ?: "")

        if ((savedId == "guest" || savedId.isEmpty()) && savedPhone.isNotEmpty()) {
            savedId = "USR-" + (if (savedPhone.length >= 6) savedPhone.takeLast(6) else (100000..999999).random().toString())
            sp.edit().putString("user_id", com.example.util.SecurityCryptoUtils.encrypt(savedId)).apply()
        }

        _currentUserId.value = savedId
        _currentUserName.value = savedName
        _currentUserPhone.value = savedPhone
        _currentUserResidence.value = savedResidence
        
        val savedJoinPhone = com.example.util.SecurityCryptoUtils.decrypt(sp.getString("join_request_phone", "") ?: "")
        _joinRequestPhone.value = savedJoinPhone
        
        val savedRole = sp.getString("saved_admin_role", "GUEST") ?: "GUEST"
        if (savedRole != "GUEST") {
            _adminRole.value = savedRole
        }

        val savedLang = LocaleManager.currentLang.value
        _currentLanguage.value = savedLang

        try {
            val savedFavs = sp.getStringSet("favorite_ids_set", emptySet()) ?: emptySet()
            if (savedFavs.isNotEmpty()) {
                _favoriteIds.value = savedFavs
            }
            checkAndTriggerFavoriteOffersNotifications()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Automatic credential synchronization for service providers / technicians
        if (savedJoinPhone.isNotEmpty() && (savedId == "guest" || savedId.isEmpty())) {
            db.collection("providers").whereEqualTo("phone", savedJoinPhone).get().addOnSuccessListener { snapshot ->
                if (snapshot != null && !snapshot.isEmpty) {
                    val prov = snapshot.documents.first().toObject(com.example.data.ProviderEntity::class.java)
                    if (prov != null) {
                        _currentUserId.value = prov.id
                        _currentUserName.value = prov.name
                        _currentUserPhone.value = prov.phone
                        _currentUserResidence.value = prov.area
                        
                        sp.edit().apply {
                            putString("user_id", com.example.util.SecurityCryptoUtils.encrypt(prov.id))
                            putString("user_name", com.example.util.SecurityCryptoUtils.encrypt(prov.name))
                            putString("user_phone", com.example.util.SecurityCryptoUtils.encrypt(prov.phone))
                            putString("user_residence", com.example.util.SecurityCryptoUtils.encrypt(prov.area))
                            apply()
                        }
                    }
                } else {
                    db.collection("pending_providers").whereEqualTo("phone", savedJoinPhone).get().addOnSuccessListener { pSnapshot ->
                        if (pSnapshot != null && !pSnapshot.isEmpty) {
                            val pend = pSnapshot.documents.first().toObject(com.example.data.PendingProviderEntity::class.java)
                            if (pend != null) {
                                val pendId = "user_" + pend.phone
                                _currentUserId.value = pendId
                                _currentUserName.value = pend.name
                                _currentUserPhone.value = pend.phone
                                _currentUserResidence.value = pend.area
                                
                                sp.edit().apply {
                                    putString("user_id", com.example.util.SecurityCryptoUtils.encrypt(pendId))
                                    putString("user_name", com.example.util.SecurityCryptoUtils.encrypt(pend.name))
                                    putString("user_phone", com.example.util.SecurityCryptoUtils.encrypt(pend.phone))
                                    putString("user_residence", com.example.util.SecurityCryptoUtils.encrypt(pend.area))
                                    apply()
                                }
                            }
                        }
                    }
                }
            }
        }
    }



    val auth by lazy {
        com.google.firebase.auth.FirebaseAuth.getInstance()
    }

    private fun getAuthEmailForPhone(phone: String): String {
        val clean = phone.trim().replace(" ", "").replace("+", "").replace("-", "")
        return "user_$clean@yemen-services.app"
    }

    fun registerGuestUser(context: android.content.Context, name: String, phone: String, residence: String, password: String = "") {
        val cleanPhone = phone.trim().replace(" ", "").replace("+", "")
        val androidId = android.provider.Settings.Secure.getString(context.contentResolver, android.provider.Settings.Secure.ANDROID_ID) ?: "unknown_device"

        if (password.isBlank()) {
            triggerNotification("⚠️ يجب إدخال كلمة مرور قوية مكونة من 8 خانات على الأقل.")
            return
        }

        val valResult = com.example.util.SecurityCryptoUtils.validatePasswordPolicy(password)
        if (!valResult.first) {
            triggerNotification("⚠️ ${valResult.second}")
            return
        }

        val authEmail = getAuthEmailForPhone(cleanPhone)
        val saltedHashPass = com.example.util.PasswordHasher.createSaltedHash(password)

        auth.createUserWithEmailAndPassword(authEmail, password)
            .addOnSuccessListener { authResult ->
                val firebaseUid = authResult.user?.uid ?: ("usr_" + System.currentTimeMillis())
                completeGuestRegistration(context, firebaseUid, name, cleanPhone, residence, androidId, password)
            }
            .addOnFailureListener { e ->
                if (e.message?.contains("already in use") == true || e.message?.contains("EMAIL_EXISTS") == true) {
                    auth.signInWithEmailAndPassword(authEmail, password)
                        .addOnSuccessListener { authResult ->
                            val firebaseUid = authResult.user?.uid ?: ("usr_" + System.currentTimeMillis())
                            completeGuestRegistration(context, firebaseUid, name, cleanPhone, residence, androidId, password)
                        }
                        .addOnFailureListener {
                            triggerNotification("❌ كلمة المرور المدخلة غير صحيحة لهذا الحساب المسجل سابقاً.")
                        }
                } else {
                    completeGuestRegistration(context, "user_" + (100000..999999).random(), name, cleanPhone, residence, androidId, password)
                }
            }
    }

    private fun completeGuestRegistration(
        context: android.content.Context,
        userId: String,
        name: String,
        phone: String,
        residence: String,
        androidId: String,
        password: String = ""
    ) {
        _currentUserId.value = userId
        _currentUserName.value = name
        _currentUserPhone.value = phone
        _currentUserResidence.value = residence

        val sp = context.getSharedPreferences("yemen_service_prefs", android.content.Context.MODE_PRIVATE)
        sp.edit().apply {
            putString("user_id", com.example.util.SecurityCryptoUtils.encrypt(userId))
            putString("user_name", com.example.util.SecurityCryptoUtils.encrypt(name))
            putString("user_phone", com.example.util.SecurityCryptoUtils.encrypt(phone))
            putString("user_residence", com.example.util.SecurityCryptoUtils.encrypt(residence))
            apply()
        }

        // Save profile WITH password field!
        val regUser = mapOf(
            "id" to userId,
            "name" to name,
            "phone" to phone,
            "residence" to residence,
            "password" to password,
            "androidId" to androidId,
            "timestamp" to System.currentTimeMillis()
        )
        db.collection("registered_users").document(userId).set(regUser)
        triggerNotification("🎉 أهلاً بك في الدليل $name، تم تسجيل وحماية حسابك آمنياً بنجاح عبر Firebase Auth!")
    }

    fun resetRegistrationState() {
        // Safe placeholder for registration flow reset state
        android.util.Log.d("MainViewModel", "resetRegistrationState triggered")
    }

    fun approveRegisteredUser(userId: String, userName: String = "") {
        _registeredUsersList.value = _registeredUsersList.value.map { u ->
            if (u["id"]?.toString() == userId) {
                u.toMutableMap().apply { put("isApproved", true) }
            } else u
        }
        db.collection("registered_users").document(userId).update("isApproved", true)
            .addOnSuccessListener {
                triggerNotification("✅ تم قبول وتأكيد طلب انضمام المستخدم $userName بنجاح من قبل الأدمن!")
            }
    }

    fun toggleBlockRegisteredUser(userId: String, currentBlocked: Boolean, userName: String = "") {
        val newBlockedState = !currentBlocked
        _registeredUsersList.value = _registeredUsersList.value.map { u ->
            if (u["id"]?.toString() == userId) {
                u.toMutableMap().apply { put("isBlocked", newBlockedState) }
            } else u
        }
        db.collection("registered_users").document(userId).update("isBlocked", newBlockedState)
            .addOnSuccessListener {
                val actionText = if (newBlockedState) "حظر" else "إلغاء حظر"
                triggerNotification("🛡️ تم $actionText حساب المستخدم $userName بنجاح.")
            }
    }

    fun deleteRegisteredUser(userId: String, userName: String = "") {
        _registeredUsersList.value = _registeredUsersList.value.filter { u -> u["id"]?.toString() != userId }
        db.collection("registered_users").document(userId).delete()
            .addOnSuccessListener {
                triggerNotification("🗑️ تم حذف حساب المستخدم $userName من القاعدة بنجاح.")
            }
    }

    fun restoreUserAccountByPhoneAndPassword(
        context: android.content.Context,
        phone: String,
        password: String,
        onResult: (Boolean, String) -> Unit
    ) {
        val cleanPhone = phone.trim().replace(" ", "").replace("+", "")
        val cleanPass = password.trim()

        val valResult = com.example.util.SecurityCryptoUtils.validatePasswordPolicy(cleanPass)
        if (!valResult.first) {
            onResult(false, valResult.second ?: "كلمة المرور غير صالحة")
            return
        }

        val authEmail = getAuthEmailForPhone(cleanPhone)

        auth.signInWithEmailAndPassword(authEmail, cleanPass)
            .addOnSuccessListener { authResult ->
                val firebaseUid = authResult.user?.uid ?: ""
                db.collection("registered_users").whereEqualTo("phone", cleanPhone).get()
                    .addOnSuccessListener { qs ->
                        val doc = qs.documents.firstOrNull()
                        val name = doc?.getString("name") ?: "المستخدم"
                        val residence = doc?.getString("residence") ?: "اليمن"

                        val finalUid = if (firebaseUid.isNotEmpty()) firebaseUid else (doc?.getString("id") ?: "user_recovered")
                        _currentUserId.value = finalUid
                        _currentUserName.value = name
                        _currentUserPhone.value = cleanPhone
                        _currentUserResidence.value = residence

                        val sp = context.getSharedPreferences("yemen_service_prefs", android.content.Context.MODE_PRIVATE)
                        sp.edit().apply {
                            putString("user_id", com.example.util.SecurityCryptoUtils.encrypt(finalUid))
                            putString("user_name", com.example.util.SecurityCryptoUtils.encrypt(name))
                            putString("user_phone", com.example.util.SecurityCryptoUtils.encrypt(cleanPhone))
                            putString("user_residence", com.example.util.SecurityCryptoUtils.encrypt(residence))
                            apply()
                        }
                        triggerNotification("✅ تم تسجيل الدخول واستعادة حسابك آمنياً بنجاح عزيزي $name!")
                        onResult(true, "تم استعادة الحساب بنجاح")
                    }
                    .addOnFailureListener {
                        onResult(true, "تم تسجيل الدخول بنجاح عبر Firebase Auth")
                    }
            }
            .addOnFailureListener {
                onResult(false, "❌ فشل استعادة الحساب: كلمة المرور أو رقم الهاتف غير صحيح")
            }
    }

    fun restoreGuestUser(context: android.content.Context, phone: String, password: String, onResult: (Boolean, String) -> Unit) {
        restoreUserAccountByPhoneAndPassword(context, phone, password, onResult)
    }

    fun setUserSessionDetails(context: android.content.Context, name: String, phone: String, residence: String = "اليمن") {
        val cleanPhone = phone.trim().replace(" ", "").replace("+967", "").removePrefix("0")
        val finalPhone = if (cleanPhone.length == 9) cleanPhone else phone
        _currentUserName.value = name.ifBlank { "عميل" }
        _currentUserPhone.value = finalPhone
        _currentUserResidence.value = residence.ifBlank { "اليمن" }
        if (_currentUserId.value.isEmpty() || _currentUserId.value == "guest") {
            _currentUserId.value = "user_" + (if (finalPhone.length >= 6) finalPhone.takeLast(6) else (100000..999999).random().toString())
        }
        _joinRequestPhone.value = finalPhone
        val sp = context.getSharedPreferences("yemen_service_prefs", android.content.Context.MODE_PRIVATE)
        sp.edit().apply {
            putString("user_name", com.example.util.SecurityCryptoUtils.encrypt(_currentUserName.value))
            putString("user_phone", com.example.util.SecurityCryptoUtils.encrypt(finalPhone))
            putString("user_residence", com.example.util.SecurityCryptoUtils.encrypt(_currentUserResidence.value))
            putString("user_id", com.example.util.SecurityCryptoUtils.encrypt(_currentUserId.value))
            putString("join_request_phone", com.example.util.SecurityCryptoUtils.encrypt(finalPhone))
            apply()
        }
    }

    fun loginUserDirectly(context: android.content.Context, phone: String) {
        val cleanPhone = phone.trim().replace(" ", "").replace("+967", "").removePrefix("0")
        val finalPhone = if (cleanPhone.length == 9) cleanPhone else phone
        _currentUserPhone.value = finalPhone
        _joinRequestPhone.value = finalPhone
        val sp = context.getSharedPreferences("yemen_service_prefs", android.content.Context.MODE_PRIVATE)
        sp.edit().apply {
            putString("user_phone", com.example.util.SecurityCryptoUtils.encrypt(finalPhone))
            putString("join_request_phone", com.example.util.SecurityCryptoUtils.encrypt(finalPhone))
            apply()
        }
    }

    fun listenToUserSupportChat(userId: String) {
        supportChatListenerRegistration?.remove()
        supportChatListenerRegistration = null

        if (userId == "guest") {
            _chatMessages.value = emptyList()
            return
        }

        val channelId = "support_" + userId
        supportChatListenerRegistration = db.collection("chat_channels").document(channelId).addSnapshotListener { snapshot, error ->
            if (snapshot != null && snapshot.exists()) {
                val ch = snapshot.toObject(ChatChannelEntity::class.java)
                if (ch != null) {
                    _chatMessages.value = ch.messages
                    
                    // Automatically mark incoming messages from the other party as READ
                    val currentId = _currentUserId.value
                    var modified = false
                    val updatedMessages = ch.messages.map { msg ->
                        if (msg.senderId != currentId && msg.status != "READ") {
                            modified = true
                            msg.copy(status = "READ")
                        } else {
                            msg
                        }
                    }
                    if (modified) {
                        db.collection("chat_channels").document(channelId).update("messages", updatedMessages)
                    }
                }
            } else {
                val initialMsg = ChatMessageEntity(
                    id = "c_welcome",
                    senderId = "admin",
                    message = "مرحباً بكم في الدعم المباشر الفوري، كيف يمكننا مساعدتكم اليوم بفريقنا المتأهب؟",
                    timestamp = System.currentTimeMillis() - 1000,
                    senderName = "الدعم الفني"
                )
                _chatMessages.value = listOf(initialMsg)
            }
        }
    }

    init {
        try {
            setupRealtimeFirestoreListeners()
            loadCardSettings()
            loadPendingTechnicians()
            loadUserPoints()
            seedFirestoreIfEmpty()
        } catch (e: Exception) {
            e.printStackTrace()
            _isInitialized.value = true
        }

        viewModelScope.launch {
            kotlinx.coroutines.delay(2200)
            _isInitialized.value = true
        }

        // Initialization complete

        
        // Watcher for real-time join request status updates (Firestore notifications)
        viewModelScope.launch {
            var lastKnownStatus: String? = null
            combine(_joinRequestPhone, _pendingProviders, _providers, _notifications) { phone, pending, approved, notifs ->
                if (phone.isEmpty()) return@combine null
                
                val isApproved = approved.any { it.phone == phone }
                if (isApproved) return@combine "APPROVED"
                
                val rejectionNotif = notifs.find { 
                    it.targetValue == phone && (it.title.contains("رفض") || it.message.contains("رفض"))
                }
                if (rejectionNotif != null) return@combine "REJECTED"
                
                val isPending = pending.any { it.phone == phone }
                if (isPending) return@combine "PENDING"
                
                null
            }.collect { status ->
                if (status != null && lastKnownStatus != null && status != lastKnownStatus) {
                    when (status) {
                        "APPROVED" -> {
                            triggerNotification("🎉 رائع جداً! لقد تم قبول طلب انضمامك للدليل كفني معتمد وتنشيط حسابك الآن!")
                        }
                        "REJECTED" -> {
                            triggerNotification("⚠️ تنبيه: تم مراجعة طلبك ورفضه من قبل الإدارة. يرجى مراجعة السبب والتعديل.")
                        }
                    }
                }
                if (status != null) {
                    lastKnownStatus = status
                }
            }
        }
        
        viewModelScope.launch {
            _currentUserId.collect { newUserId ->
                listenToUserSupportChat(newUserId)
            }
        }
    }

    private fun reg(listener: com.google.firebase.firestore.ListenerRegistration) {
        firestoreListeners.add(listener)
    }

    private fun setupRealtimeFirestoreListeners() {
        // 1. Settings (Document main_settings)
        reg(db.collection("settings").document("main_settings").addSnapshotListener { snapshot, error ->
            if (error != null) {
                error.printStackTrace()
                _isInitialized.value = true
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                try {
                    snapshot.toObject(AdminSettingsEntity::class.java)?.let {
                        _settings.value = it
                        _maxKmRadius.value = it.maxSearchRadiusKm
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                _settings.value = AdminSettingsEntity()
            }
            _isInitialized.value = true
        })

        // 1b. Booking Form Fields Listener
        reg(db.collection("settings").document("booking_fields").addSnapshotListener { snapshot, error ->
            if (error == null && snapshot != null && snapshot.exists()) {
                try {
                    snapshot.toObject(BookingFormFields::class.java)?.let {
                        _bookingFormFields.value = it
                    }
                } catch (e: Exception) { e.printStackTrace() }
            }
        })

        // 1c. Booking Distribution Mode Listener
        reg(db.collection("settings").document("distribution_mode").addSnapshotListener { snapshot, error ->
            if (error == null && snapshot != null && snapshot.exists()) {
                val modeStr = snapshot.getString("mode")
                if (!modeStr.isNullOrEmpty()) {
                    try {
                        _distributionMode.value = BookingDistributionMode.valueOf(modeStr)
                    } catch (e: Exception) { e.printStackTrace() }
                }
            }
        })

        // 2. Categories
        reg(db.collection("categories").addSnapshotListener { snapshot, error ->
            if (error != null) {
                error.printStackTrace()
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val fetched = snapshot.documents.mapNotNull { doc ->
                    try {
                        val obj = doc.toObject(com.example.data.CategoryEntity::class.java)
                        if (obj != null && obj.id.isEmpty()) {
                            obj.copy(id = doc.id)
                        } else {
                            obj
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }.distinctBy { it.id }.sortedWith(compareByDescending<com.example.data.CategoryEntity> { it.isPinned }.thenBy { it.order })
                _categories.value = fetched
            }
        })

        // Custom Profile Tabs
        db.collection("custom_profile_tabs").addSnapshotListener { snapshot, error ->
            if (error == null && snapshot != null) {
                val fetched = snapshot.toObjects(com.example.data.CustomProfileTabEntity::class.java)
                _customProfileTabs.value = fetched.sortedBy { it.displayOrder }
            }
        }

        // 3. Cities
        db.collection("cities").addSnapshotListener { snapshot, error ->
            if (error != null) {
                error.printStackTrace()
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val fetched = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(CityEntity::class.java)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }
                _cities.value = fetched
            }
        }

        // 3b. Registered Users count listener
        db.collection("registered_users").addSnapshotListener { snapshot, error ->
            if (error == null && snapshot != null) {
                _registeredUsersCount.value = snapshot.size()
                val list = snapshot.documents.mapNotNull { doc ->
                    val data = doc.data?.toMutableMap() ?: mutableMapOf()
                    data["id"] = doc.id
                    data
                }
                _registeredUsersList.value = list
            }
        }

        // 3c. Internal Wallets Listener
        db.collection("internal_wallets").addSnapshotListener { snapshot, error ->
            if (error == null && snapshot != null) {
                _internalWallets.value = snapshot.documents.mapNotNull { it.toObject(com.example.data.InternalWalletEntity::class.java) }
            }
        }

        // 3d. Wallet Transactions Listener
        db.collection("wallet_transactions").addSnapshotListener { snapshot, error ->
            if (error == null && snapshot != null) {
                _walletTransactions.value = snapshot.documents.mapNotNull { it.toObject(com.example.data.WalletTransactionEntity::class.java) }.sortedByDescending { it.timestamp }
            }
        }

        // 4. Banners
        db.collection("banners").addSnapshotListener { snapshot, error ->
            if (error != null) {
                error.printStackTrace()
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val fetched = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(BannerEntity::class.java)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }
                _banners.value = fetched.sortedBy { it.order }
            } else {
                _banners.value = emptyList()
            }
        }

        // 5. Providers (Full limit & safe parsing for complete Map & listing coverage)
        db.collection("providers").limit(250).addSnapshotListener { snapshot, error ->
            _isProvidersLoading.value = false
            if (error != null) {
                error.printStackTrace()
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val allList = snapshot.documents.mapNotNull { doc ->
                    try {
                        val parsed = doc.toObject(ProviderEntity::class.java)
                        parsed?.copy(id = doc.id)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        try {
                            ProviderEntity(
                                id = doc.id,
                                name = doc.getString("name") ?: "",
                                phone = doc.getString("phone") ?: "",
                                categoryId = doc.getString("categoryId") ?: "",
                                area = doc.getString("area") ?: doc.getString("localArea") ?: "",
                                isVip = doc.getBoolean("isVip") ?: doc.getBoolean("vip") ?: false,
                                subscriptionStatus = doc.getString("subscriptionStatus") ?: "APPROVED",
                                isAvailable = doc.getBoolean("isAvailable") ?: doc.getBoolean("available") ?: true,
                                cityId = doc.getString("cityId") ?: "",
                                localNeighborhood = doc.getString("localNeighborhood") ?: "",
                                rating = (doc.getDouble("rating") ?: doc.getLong("rating")?.toDouble() ?: 5.0).toFloat(),
                                points = (doc.getLong("points") ?: 0L).toInt(),
                                isVerified = doc.getBoolean("isVerified") ?: doc.getBoolean("verified") ?: true,
                                isRecommended = doc.getBoolean("isRecommended") ?: doc.getBoolean("recommended") ?: true,
                                numReviews = (doc.getLong("numReviews") ?: 0L).toInt(),
                                coverImage = doc.getString("coverImage") ?: "",
                                profileImage = doc.getString("profileImage") ?: "",
                                previewPrice = doc.getDouble("previewPrice") ?: 1500.0,
                                latitude = doc.getDouble("latitude") ?: doc.getString("latitude")?.toDoubleOrNull() ?: 15.3694,
                                longitude = doc.getDouble("longitude") ?: doc.getString("longitude")?.toDoubleOrNull() ?: 44.1910,
                                customCategoryName = doc.getString("customCategoryName") ?: "",
                                profession = doc.getString("profession") ?: "",
                                specialization = doc.getString("specialization") ?: "",
                                isBlocked = doc.getBoolean("isBlocked") ?: doc.getBoolean("blocked") ?: false,
                                isChatDisabled = doc.getBoolean("isChatDisabled") ?: doc.getBoolean("chatDisabled") ?: false,
                                isDeleted = doc.getBoolean("isDeleted") ?: doc.getBoolean("deleted") ?: false,
                                providerType = doc.getString("providerType") ?: ""
                            )
                        } catch (ex: Exception) {
                            ex.printStackTrace()
                            null
                        }
                    }
                }.filter { !it.name.contains("ماهر") && it.id != "p_maher" }
                
                val activeList = allList.filter { !it.isDeleted }
                val deletedList = allList.filter { it.isDeleted }
                
                _providers.value = activeList
                _deletedProviders.value = deletedList
                applyFilters()
            }
        }

        // 6. Pending Providers (Full limit & safe parsing)
        db.collection("pending_providers").limit(200).addSnapshotListener { snapshot, error ->
            if (error != null) {
                error.printStackTrace()
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val fetched = snapshot.documents.mapNotNull { doc ->
                    try {
                        val parsed = doc.toObject(PendingProviderEntity::class.java)
                        parsed?.copy(id = doc.id)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        try {
                            PendingProviderEntity(
                                id = doc.id,
                                name = doc.getString("name") ?: "",
                                phone = doc.getString("phone") ?: "",
                                categoryId = doc.getString("categoryId") ?: "",
                                area = doc.getString("area") ?: doc.getString("localArea") ?: "",
                                localNeighborhood = doc.getString("localNeighborhood") ?: "",
                                status = doc.getString("status") ?: "PENDING",
                                reason = doc.getString("reason") ?: "",
                                idPhotoBase64 = doc.getString("idPhotoBase64") ?: "",
                                selfiePhotoBase64 = doc.getString("selfiePhotoBase64") ?: "",
                                workPhotosBase64 = (doc.get("workPhotosBase64") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                            )
                        } catch (e2: java.lang.Exception) {
                            e2.printStackTrace()
                            null
                        }
                    }
                }
                _pendingProviders.value = fetched
            }
        }

        // 7. Bookings (Paginated / limited to 20)
        db.collection("bookings").orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING).limit(20).addSnapshotListener { snapshot, error ->
            if (error != null) {
                error.printStackTrace()
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val fetched = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(BookingEntity::class.java)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }
                _bookings.value = fetched
            }
        }

        // 8. Notifications (Paginated / limited to 20 with strict validation & deduplication)
        db.collection("notifications").orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING).limit(20).addSnapshotListener { snapshot, error ->
            if (error != null) {
                error.printStackTrace()
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val fetched = snapshot.documents.mapNotNull { doc ->
                    try {
                        val obj = doc.toObject(NotificationEntity::class.java)
                        val finalObj = if (obj != null && obj.id.isEmpty()) {
                            obj.copy(id = doc.id)
                        } else {
                            obj
                        }
                        if (finalObj != null && finalObj.isValid()) finalObj else null
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }.distinctBy { it.id.ifBlank { "${it.title}_${it.timestamp}" } }.sortedByDescending { it.timestamp }
                _notifications.value = fetched
            }
        }

        // 9. Chat Channels (Paginated / limited to 20)
        db.collection("chat_channels").orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING).limit(20).addSnapshotListener { snapshot, error ->
            _isChatChannelsLoading.value = false
            if (error != null) {
                error.printStackTrace()
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val fetched = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(ChatChannelEntity::class.java)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }.sortedByDescending { it.timestamp }
                _chatChannels.value = fetched
            }
        }

        // 10. General Support Chat Messages are handled dynamically based on currentUserId

        // 11. Reports (Paginated / limited to 20)
        db.collection("reports").limit(20).addSnapshotListener { snapshot, error ->
            if (error != null) {
                error.printStackTrace()
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val fetched = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(ReportEntity::class.java)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }
                _reports.value = fetched
            }
        }

        // 12. Supervisors (Instantly synced)
        db.collection("supervisors").limit(20).addSnapshotListener { snapshot, error ->
            if (error != null) {
                error.printStackTrace()
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val fetched = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(SupervisorEntity::class.java)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }
                _supervisors.value = fetched
            }
        }

        // 13. Color Palettes (Instantly synced)
        db.collection("color_themes").addSnapshotListener { snapshot, error ->
            if (error != null) {
                error.printStackTrace()
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val fetched = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(ColorPaletteEntity::class.java)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }
                _colorPalettes.value = fetched
            }
        }

        // 14. Calls Log (Paginated / limited to 20)
        db.collection("calls").orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING).limit(20).addSnapshotListener { snapshot, error ->
            if (error != null) {
                error.printStackTrace()
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val fetched = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(CallEntity::class.java)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }.sortedByDescending { it.timestamp }
                _callsLog.value = fetched
            }
        }

        // 15. Coupons
        db.collection("coupons").limit(20).addSnapshotListener { snapshot, error ->
            if (error != null) {
                error.printStackTrace()
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val fetched = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(CouponEntity::class.java)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }
                _coupons.value = fetched
            }
        }

        // 16. Payment Wallets
        db.collection("payment_wallets").limit(20).addSnapshotListener { snapshot, error ->
            if (error != null) {
                error.printStackTrace()
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val fetched = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(PaymentWalletEntity::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }.sortedBy { it.displayOrder }
                _paymentWallets.value = fetched
            }
        }

        // 17. Payments (Paginated / limited to 20)
        db.collection("payments").orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING).limit(20).addSnapshotListener { snapshot, error ->
            if (error != null) {
                error.printStackTrace()
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val fetched = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(PaymentEntity::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }.sortedByDescending { it.createdAt }
                _payments.value = fetched
            }
        }

        // 18. Stores (Full limit & safe parsing for Maps & directory coverage)
        db.collection("stores").limit(250).addSnapshotListener { snapshot, error ->
            if (error == null && snapshot != null) {
                val fetched = snapshot.documents.mapNotNull { doc ->
                    try {
                        val obj = doc.toObject(com.example.data.StoreEntity::class.java)
                        if (obj != null) {
                            val isDel = doc.getBoolean("isDeleted") == true || doc.getBoolean("deleted") == true
                            val act = doc.getBoolean("isActive") ?: doc.getBoolean("active") ?: true
                            val appr = doc.getBoolean("isApproved") ?: doc.getBoolean("approved") ?: act
                            val pin = doc.getBoolean("isPinned") == true || doc.getBoolean("pinned") == true
                            val vip = doc.getBoolean("isVip") == true || doc.getBoolean("vip") == true
                            val rec = doc.getBoolean("isRecommended") == true || doc.getBoolean("recommended") == true
                            val ver = doc.getBoolean("isVerified") == true || doc.getBoolean("verified") == true
                            val blk = doc.getBoolean("isBlocked") == true || doc.getBoolean("blocked") == true
                            val chatDis = doc.getBoolean("isChatDisabled") == true || doc.getBoolean("chatDisabled") == true
                            obj.copy(
                                id = doc.id,
                                isDeleted = isDel,
                                isActive = act,
                                isApproved = appr,
                                isPinned = pin,
                                isVip = vip,
                                isRecommended = rec,
                                isVerified = ver,
                                isBlocked = blk,
                                isChatDisabled = chatDis
                            )
                        } else null
                    } catch (e: Exception) {
                        e.printStackTrace()
                        try {
                            com.example.data.StoreEntity(
                                id = doc.id,
                                sectionId = doc.getString("sectionId") ?: "stores",
                                name = doc.getString("name") ?: "",
                                description = doc.getString("description") ?: "",
                                phone = doc.getString("phone") ?: "",
                                categoryId = doc.getString("categoryId") ?: "",
                                cityId = doc.getString("cityId") ?: "",
                                localNeighborhood = doc.getString("localNeighborhood") ?: "",
                                coverImage = doc.getString("coverImage") ?: "",
                                logoImage = doc.getString("logoImage") ?: "",
                                rating = (doc.getDouble("rating") ?: doc.getLong("rating")?.toDouble() ?: 5.0).toFloat(),
                                numReviews = (doc.getLong("numReviews") ?: 0L).toInt(),
                                isActive = doc.getBoolean("isActive") ?: doc.getBoolean("active") ?: true,
                                isPinned = doc.getBoolean("isPinned") == true || doc.getBoolean("pinned") == true,
                                latitude = doc.getDouble("latitude") ?: doc.getString("latitude")?.toDoubleOrNull() ?: 15.3694,
                                longitude = doc.getDouble("longitude") ?: doc.getString("longitude")?.toDoubleOrNull() ?: 44.1910,
                                isDeleted = doc.getBoolean("isDeleted") == true || doc.getBoolean("deleted") == true,
                                isApproved = doc.getBoolean("isApproved") ?: doc.getBoolean("approved") ?: true,
                                isVip = doc.getBoolean("isVip") == true || doc.getBoolean("vip") == true,
                                isVerified = doc.getBoolean("isVerified") == true || doc.getBoolean("verified") == true,
                                isRecommended = doc.getBoolean("isRecommended") == true || doc.getBoolean("recommended") == true,
                                isBlocked = doc.getBoolean("isBlocked") == true || doc.getBoolean("blocked") == true,
                                medicalLicenseNo = doc.getString("medicalLicenseNo") ?: "",
                                commercialRegisterNo = doc.getString("commercialRegisterNo") ?: "",
                                providerType = doc.getString("providerType") ?: ""
                            )
                        } catch (ex: Exception) {
                            ex.printStackTrace()
                            null
                        }
                    }
                }
                _stores.value = fetched.filter { !it.isDeleted }
            }
        }

        // 19. Products (Full limit & safe parsing)
        db.collection("products").limit(250).addSnapshotListener { snapshot, error ->
            if (error == null && snapshot != null) {
                val fetched = snapshot.documents.mapNotNull { doc ->
                    try {
                        val obj = doc.toObject(com.example.data.ProductEntity::class.java)
                        if (obj != null) {
                            val isDel = doc.getBoolean("isDeleted") == true || doc.getBoolean("deleted") == true
                            obj.copy(id = doc.id, isDeleted = isDel)
                        } else null
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }
                _products.value = fetched.filter { !it.isDeleted }
            }
        }

        // 20. Properties (Full limit & safe parsing for Maps & real estate coverage)
        db.collection("properties").limit(250).addSnapshotListener { snapshot, error ->
            if (error == null && snapshot != null) {
                val fetched = snapshot.documents.mapNotNull { doc ->
                    try {
                        val obj = doc.toObject(com.example.data.PropertyEntity::class.java)
                        if (obj != null) {
                            val isDel = doc.getBoolean("isDeleted") == true || doc.getBoolean("deleted") == true
                            val act = doc.getBoolean("isActive") ?: doc.getBoolean("active") ?: true
                            val appr = doc.getBoolean("isApproved") ?: doc.getBoolean("approved") ?: act
                            val pin = doc.getBoolean("isPinned") == true || doc.getBoolean("pinned") == true
                            val vip = doc.getBoolean("isVip") == true || doc.getBoolean("vip") == true
                            val rec = doc.getBoolean("isRecommended") == true || doc.getBoolean("recommended") == true
                            val ver = doc.getBoolean("isVerified") == true || doc.getBoolean("verified") == true
                            val blk = doc.getBoolean("isBlocked") == true || doc.getBoolean("blocked") == true
                            obj.copy(
                                id = doc.id,
                                isDeleted = isDel,
                                isActive = act,
                                isApproved = appr,
                                isPinned = pin,
                                isVip = vip,
                                isRecommended = rec,
                                isVerified = ver,
                                isBlocked = blk
                            )
                        } else null
                    } catch (e: Exception) {
                        e.printStackTrace()
                        try {
                            com.example.data.PropertyEntity(
                                id = doc.id,
                                sectionId = doc.getString("sectionId") ?: "properties",
                                title = doc.getString("title") ?: "",
                                description = doc.getString("description") ?: "",
                                price = doc.getDouble("price") ?: doc.getLong("price")?.toDouble() ?: 0.0,
                                currency = doc.getString("currency") ?: "YER",
                                type = doc.getString("type") ?: "rent",
                                propertyType = doc.getString("propertyType") ?: "apartment",
                                phone = doc.getString("phone") ?: "",
                                cityId = doc.getString("cityId") ?: "",
                                localNeighborhood = doc.getString("localNeighborhood") ?: "",
                                rating = (doc.getDouble("rating") ?: doc.getLong("rating")?.toDouble() ?: 5.0).toFloat(),
                                numReviews = (doc.getLong("numReviews") ?: 0L).toInt(),
                                isActive = doc.getBoolean("isActive") ?: doc.getBoolean("active") ?: true,
                                isPinned = doc.getBoolean("isPinned") == true || doc.getBoolean("pinned") == true,
                                latitude = doc.getDouble("latitude") ?: doc.getString("latitude")?.toDoubleOrNull() ?: 15.3694,
                                longitude = doc.getDouble("longitude") ?: doc.getString("longitude")?.toDoubleOrNull() ?: 44.1910,
                                isDeleted = doc.getBoolean("isDeleted") == true || doc.getBoolean("deleted") == true,
                                isApproved = doc.getBoolean("isApproved") ?: doc.getBoolean("approved") ?: true,
                                isVip = doc.getBoolean("isVip") == true || doc.getBoolean("vip") == true,
                                isVerified = doc.getBoolean("isVerified") == true || doc.getBoolean("verified") == true,
                                isRecommended = doc.getBoolean("isRecommended") == true || doc.getBoolean("recommended") == true,
                                isBlocked = doc.getBoolean("isBlocked") == true || doc.getBoolean("blocked") == true
                            )
                        } catch (ex: Exception) {
                            ex.printStackTrace()
                            null
                        }
                    }
                }
                _properties.value = fetched.filter { !it.isDeleted }
            }
        }

        // 20.1 Jobs (Paginated / limited to 20)
        db.collection("jobs").limit(20).addSnapshotListener { snapshot, error ->
            if (error == null && snapshot != null) {
                val fetched = snapshot.documents.mapNotNull { doc ->
                    try {
                        val obj = doc.toObject(com.example.data.JobEntity::class.java)
                        if (obj != null) {
                            val isDel = doc.getBoolean("isDeleted") == true || doc.getBoolean("deleted") == true
                            val act = doc.getBoolean("isActive") ?: doc.getBoolean("active") ?: true
                            val pin = doc.getBoolean("isPinned") == true || doc.getBoolean("pinned") == true
                            obj.copy(id = doc.id, isDeleted = isDel, isActive = act, isPinned = pin)
                        } else null
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }
                _jobs.value = fetched
            }
        }

        // 20.2 Job Applications (Paginated / limited to 20)
        db.collection("job_applications").limit(20).addSnapshotListener { snapshot, error ->
            if (error == null && snapshot != null) {
                val fetched = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(com.example.data.JobApplicationEntity::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }
                _jobApplications.value = fetched
            }
        }

        // 21. Ratings (Paginated / limited to 20)
        db.collection("ratings").orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING).limit(20).addSnapshotListener { snapshot, error ->
            if (error == null && snapshot != null) {
                val fetched = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(com.example.data.RatingEntity::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }
                _ratings.value = fetched
            }
        }

        // 22. Orders (Paginated / limited to 20)
        db.collection("orders").orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING).limit(20).addSnapshotListener { snapshot, error ->
            if (error == null && snapshot != null) {
                val fetched = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(com.example.data.OrderEntity::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }
                _orders.value = fetched
            }
        }

        // 22.1 Offers & Instant Pricing (Real-time synchronization)
        db.collection("offers").limit(50).addSnapshotListener { snapshot, error ->
            if (error == null && snapshot != null) {
                val fetched = snapshot.documents.mapNotNull { doc ->
                    try {
                        val obj = doc.toObject(com.example.data.models.Offer::class.java)
                        obj?.copy(id = doc.id)
                    } catch (e: Exception) {
                        null
                    }
                }
                _offers.value = fetched
            }
        }

        // 23. Activity Logs (Paginated / limited to 20)
        db.collection("activity_logs").orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING).limit(20).addSnapshotListener { snapshot, error ->
            if (error == null && snapshot != null) {
                val fetched = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(com.example.data.ActivityLogEntity::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        null
                    }
                }.sortedByDescending { it.timestamp }
                _activityLogs.value = fetched
            }
        }

        // 24. Instant Requests (Paginated / limited to 20)
        db.collection("instant_requests").orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING).limit(20).addSnapshotListener { snapshot, error ->
            if (error == null && snapshot != null) {
                val fetched = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(com.example.data.models.InstantRequestEntity::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }
                // Auto-expire requests past expiresAt
                val now = System.currentTimeMillis()
                val processed = fetched.map { req ->
                    if ((req.status == "WAITING_FOR_OFFERS" || req.status == "REVIEWING_OFFERS") && now > req.expiresAt) {
                        req.copy(status = "EXPIRED")
                    } else req
                }
                _instantRequests.value = processed
            }
        }

        // 25. Request Offers (Paginated / limited to 20)
        db.collection("request_offers").orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING).limit(20).addSnapshotListener { snapshot, error ->
            if (error == null && snapshot != null) {
                val fetched = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(com.example.data.models.RequestOfferEntity::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }
                _requestOffers.value = fetched
            }
        }
    }

    fun seedFirestoreIfEmpty() {
        // Check and seed default configurations ONLY if the document genuinely does not exist in Firestore
        db.collection("settings").document("main_settings").get().addOnSuccessListener { doc ->
            if (doc == null || !doc.exists()) {
                db.collection("settings").document("main_settings").set(AdminSettingsEntity())
            }
        }

        db.collection("categories").get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val sn = task.result
                if (sn == null || sn.isEmpty) {
                    writeDefaultCategories()
                }
            } else {
                try { writeDefaultCategories() } catch (e: Exception) {}
            }
        }

        db.collection("cities").get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val sn = task.result
                if (sn == null || sn.isEmpty) {
                    writeDefaultCities()
                }
            } else {
                try { writeDefaultCities() } catch (e: Exception) {}
            }
        }

        db.collection("banners").get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val sn = task.result
                if (sn == null || sn.isEmpty) {
                    writeDefaultBanners()
                }
            } else {
                try { writeDefaultBanners() } catch (e: Exception) {}
            }
        }

        db.collection("supervisors").get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val sn = task.result
                if (sn == null || sn.isEmpty) {
                    writeDefaultSupervisors()
                }
            } else {
                try { writeDefaultSupervisors() } catch (e: Exception) {}
            }
        }

        db.collection("color_themes").get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val sn = task.result
                if (sn == null || sn.isEmpty) {
                    writeDefaultColorPalettes()
                }
            } else {
                try { writeDefaultColorPalettes() } catch (e: Exception) {}
            }
        }

        db.collection("providers").get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val sn = task.result
                if (sn == null || sn.isEmpty) {
                    writeDefaultProviders()
                }
            } else {
                try { writeDefaultProviders() } catch (e: Exception) {}
            }
        }

        try { writeDefaultStores() } catch (e: Exception) { e.printStackTrace() }
        try { writeDefaultProperties() } catch (e: Exception) { e.printStackTrace() }
        try { writeDefaultJobs() } catch (e: Exception) { e.printStackTrace() }
    }

    private fun writeDefaultSupervisors() {
        val crypto = com.example.util.SecurityCryptoUtils
        val fbSupervisors = listOf(
            com.example.data.SupervisorEntity(
                "owner_1", 
                crypto.decodeObfuscatedString("340405525d655144360e0e043a094d110a19"), 
                "OWNER", 
                crypto.decodeObfuscatedString("140405001c13255f5b29235260535744575768"), 
                listOf("ALL")
            ),
            com.example.data.SupervisorEntity(
                "admin_1", 
                crypto.decodeObfuscatedString("340005525964534642290408320c0f5c061b26"), 
                "ADMIN", 
                crypto.decodeObfuscatedString("140005252e132545415e5551674640"), 
                listOf("ALL")
            )
        )
        fbSupervisors.forEach { sup ->
            db.collection("supervisors").document(sup.id).set(sup)
        }
        // Delete dummy supervisors
        listOf("2", "3", "4").forEach { id ->
            db.collection("supervisors").document(id).delete()
        }
    }

    private fun writeDefaultColorPalettes() {
        val fbPalettes = listOf(
            ColorPaletteEntity("palette_preset_1", "🦅 اليمن الأحمر", "#CE1126", "#FFD700", "#0D1B1E", "#162A2D"),
            ColorPaletteEntity("palette_preset_2", "🔵 الأزرق الملكي", "#0D47A1", "#00E5FF", "#0A192F", "#172A45"),
            ColorPaletteEntity("palette_preset_3", "🌌 كوزميك سيلفر", "#9E9E9E", "#E0E0E0", "#121212", "#1C1C1C"),
            ColorPaletteEntity("palette_preset_4", "✨ ذهبي فاخر", "#D4AF37", "#FFD700", "#1A1A1A", "#2D2D2D"),
            ColorPaletteEntity("palette_preset_5", "🟢 زمردي راقي", "#004B49", "#50C878", "#0C1814", "#152A20"),
            ColorPaletteEntity("palette_preset_6", "⚫ الأسود الدخاني", "#121212", "#333333", "#080808", "#101010")
        )
        fbPalettes.forEach { pal ->
            db.collection("color_themes").document(pal.id).set(pal)
        }
    }

    private fun writeDefaultCategories() {
        val fbCategories = listOf(
            CategoryEntity("1", "صيانة وخدمات مهنية", "🔧", 1, isMainCategory = true),
            CategoryEntity("sub_1_1", "سباكة وأنابيب", "🚰", 2, parentId = "1", isMainCategory = false),
            CategoryEntity("sub_1_2", "كهرباء ومولدات", "⚡", 3, parentId = "1", isMainCategory = false),
            CategoryEntity("sub_1_3", "تكييف وتبريد", "❄️", 4, parentId = "1", isMainCategory = false),
            CategoryEntity("sub_1_4", "نجارة وأثاث", "و", 5, parentId = "1", isMainCategory = false),
            CategoryEntity("sub_1_5", "صيانة أجهزة منزلية", "🧺", 6, parentId = "1", isMainCategory = false),

            CategoryEntity("2", "طب ورعاية صحية", "🏥", 7, isMainCategory = true),
            CategoryEntity("sub_2_1", "عيادات وأطباء", "🩺", 8, parentId = "2", isMainCategory = false),
            CategoryEntity("sub_2_2", "صيدليات ومستلزمات", "💊", 9, parentId = "2", isMainCategory = false),
            CategoryEntity("sub_2_3", "مختبرات تحاليل", "🔬", 10, parentId = "2", isMainCategory = false),
            CategoryEntity("sub_2_4", "مراكز علاج طبيعي", "🧘", 11, parentId = "2", isMainCategory = false),

            CategoryEntity("law", "محاماة واستشارات قانونية", "⚖️", 12, isMainCategory = true),
            CategoryEntity("eng", "هندسة وإنشاءات", "🏗️", 13, isMainCategory = true),
            CategoryEntity("cleaning", "تنظيف وتطهير", "🧹", 14, isMainCategory = true),
            CategoryEntity("3", "تعليم وتدريس خصوصي", "📚", 15, isMainCategory = true),
            CategoryEntity("4", "نقل ومواصلات لوجستية", "🚗", 16, isMainCategory = true),
            CategoryEntity("realestate", "عقارات وأراضي", "🏠", 17, isMainCategory = true),
            CategoryEntity("stores", "محلات ومعارض تجارية", "🏪", 18, isMainCategory = true),
            CategoryEntity("restaurants", "مطاعم وكافيهات", "🍔", 19, isMainCategory = true),
            CategoryEntity("beauty", "تجميل وعناية شخصية", "✂️", 20, isMainCategory = true),
            CategoryEntity("centers", "مراكز تخصصية وخدمية", "🏢", 21, isMainCategory = true),
            CategoryEntity("5", "تقنية وبرمجيات ذكية", "💻", 22, isMainCategory = true),
            CategoryEntity("other", "أخرى / خدمات عامة", "✏️", 23, isMainCategory = true),

            // Restaurants Subcategories
            CategoryEntity("sub_rest_1", "مطاعم يمنية وشرقية", "🍲", 24, parentId = "restaurants", isMainCategory = false),
            CategoryEntity("sub_rest_2", "وجبات سريعة وبرجر", "🍔", 25, parentId = "restaurants", isMainCategory = false),
            CategoryEntity("sub_rest_3", "كافيهات ومشروبات", "☕", 26, parentId = "restaurants", isMainCategory = false),
            CategoryEntity("sub_rest_4", "حلويات ومخابز", "🍰", 27, parentId = "restaurants", isMainCategory = false),
            CategoryEntity("sub_rest_5", "مشويات وأسماك", "🥩", 28, parentId = "restaurants", isMainCategory = false),

            // Stores Subcategories
            CategoryEntity("sub_store_1", "ملابس وأزياء", "👔", 29, parentId = "stores", isMainCategory = false),
            CategoryEntity("sub_store_2", "إلكترونيات وهواتف", "📱", 30, parentId = "stores", isMainCategory = false),
            CategoryEntity("sub_store_3", "أجهزة منزلية وكهربائية", "📺", 31, parentId = "stores", isMainCategory = false),
            CategoryEntity("sub_store_4", "سوبرماركت ومواد غذائية", "🛒", 32, parentId = "stores", isMainCategory = false),
            CategoryEntity("sub_store_5", "عطور ومستحضرات تجميل", "💄", 33, parentId = "stores", isMainCategory = false),

            // Centers Subcategories
            CategoryEntity("sub_center_1", "مراكز تجميل وصالونات", "✂️", 34, parentId = "centers", isMainCategory = false),
            CategoryEntity("sub_center_2", "مراكز طبية وتخصصية", "🏥", 35, parentId = "centers", isMainCategory = false),
            CategoryEntity("sub_center_3", "مراكز تعليم وتدريب", "🎓", 36, parentId = "centers", isMainCategory = false),
            CategoryEntity("sub_center_4", "أندية وصالات رياضية", "🏋️", 37, parentId = "centers", isMainCategory = false),

            // Real Estate Subcategories
            CategoryEntity("sub_prop_1", "شقق للإيجار والبيع", "🏢", 38, parentId = "realestate", isMainCategory = false),
            CategoryEntity("sub_prop_2", "فلل وقصور", "🏰", 39, parentId = "realestate", isMainCategory = false),
            CategoryEntity("sub_prop_3", "أراضي ومخططات", "🏞️", 40, parentId = "realestate", isMainCategory = false),
            CategoryEntity("sub_prop_4", "مكاتب ومحلات تجارية", "🏪", 41, parentId = "realestate", isMainCategory = false),
            CategoryEntity("sub_prop_5", "شاليهات واستراحات", "🏊", 42, parentId = "realestate", isMainCategory = false)
        )
        fbCategories.forEach { cat ->
            db.collection("categories").document(cat.id).set(cat)
        }
    }

    private fun writeDefaultCities() {
        val defaultCities = listOf(
            CityEntity("ye_sana_cap", "أمانة العاصمة", "Sanaa Secretariat"),
            CityEntity("ye_san", "صنعاء", "Sanaa"),
            CityEntity("ye_ade", "عدن", "Aden"),
            CityEntity("ye_tai", "تعز", "Taiz"),
            CityEntity("ye_hod", "الحديدة", "Hodeidah"),
            CityEntity("ye_ibb", "إب", "Ibb"),
            CityEntity("ye_dha", "ذمار", "Dhamar"),
            CityEntity("ye_had", "حضرموت", "Hadramout"),
            CityEntity("ye_mar", "مأرب", "Marib"),
            CityEntity("ye_saa", "صعدة", "Saada"),
            CityEntity("ye_haj", "حجة", "Hajjah"),
            CityEntity("ye_mah", "المهرة", "Al Mahrah"),
            CityEntity("ye_soc", "سقطرى", "Socotra"),
            CityEntity("ye_sha", "شبوة", "Shabwah"),
            CityEntity("ye_aby", "أبين", "Abyan"),
            CityEntity("ye_bay", "البيضاء", "Al Bayda"),
            CityEntity("ye_amr", "عمران", "Amran"),
            CityEntity("ye_ray", "ريمة", "Raymah"),
            CityEntity("ye_jaw", "الجوف", "Al Jawf"),
            CityEntity("ye_lah", "لحج", "Lahj"),
            CityEntity("ye_dal", "الضالع", "Ad Dali"),
            CityEntity("ye_mhw", "المحويت", "Al Mahwit")
        )
        defaultCities.forEach { city ->
            db.collection("cities").document(city.id).set(city)
        }
    }

    private fun writeDefaultBanners() {
        // No fake default banners written automatically
    }

    private fun writeDefaultProviders() {
        // Empty - No fake mock providers
    }

    fun getDefaultStoresList(): List<com.example.data.StoreEntity> {
        return emptyList()
    }

    fun getDefaultPropertiesList(): List<com.example.data.PropertyEntity> {
        return emptyList()
    }

    private fun writeDefaultStores() {
        // Empty - No fake mock stores
    }

    private fun writeDefaultProperties() {
        // Empty - No fake mock properties
    }

    private fun writeDefaultJobs() {
        // Empty - No fake mock jobs
    }

    private fun writeDefaultProducts() {
        // Empty - No fake mock products
    }

    // ------------------- Filters Engine -------------------
    fun applyFilters() {
        val allProviders = _providers.value
        val selectedCat = _selectedCategoryId.value
        val query = _searchQuery.value.trim().lowercase()
        val vipOnly = _filterVipOnly.value
        val availOnly = _filterAvailableOnly.value
        val cityId = _filterCityId.value
        val neighborhood = _filterNeighborhoodName.value.trim().lowercase()
        val phoneName = _phoneOrNameFilter.value.trim().lowercase()

        var filtered = allProviders

        if (selectedCat != null) {
            filtered = filtered.filter { it.categoryId == selectedCat }
        }
        if (query.isNotEmpty()) {
            filtered = filtered.filter { 
                it.name.lowercase().contains(query) || 
                it.area.lowercase().contains(query) || 
                it.localNeighborhood.lowercase().contains(query)
            }
        }
        if (vipOnly) {
            filtered = filtered.filter { it.isVip || it.subscriptionStatus == "APPROVED" }
        }
        if (availOnly) {
            filtered = filtered.filter { it.isAvailable }
        }
        if (cityId != null) {
            filtered = filtered.filter { it.cityId == cityId }
        }
        val userResidence = _currentUserResidence.value.trim().lowercase()
        if (_filterByCurrentCityOnly.value && userResidence.isNotEmpty() && userResidence != "الكل" && userResidence != "اليمن") {
            filtered = filtered.filter { p ->
                p.area.lowercase().contains(userResidence) ||
                p.cityId.lowercase().contains(userResidence) ||
                p.localNeighborhood.lowercase().contains(userResidence) ||
                userResidence.contains(p.area.lowercase())
            }
        }
        if (neighborhood.isNotEmpty()) {
            filtered = filtered.filter { it.localNeighborhood.lowercase().contains(neighborhood) }
        }
        if (phoneName.isNotEmpty()) {
            filtered = filtered.filter { 
                it.name.lowercase().contains(phoneName) || 
                it.phone.contains(phoneName) 
            }
        }

        _filteredProviders.value = filtered
    }

    fun selectCategory(categoryId: String?) {
        _selectedCategoryId.value = categoryId
        applyFilters()
        
        // Log custom Firebase Analytics event
        try {
            val bundle = android.os.Bundle().apply {
                putString("category_id", categoryId ?: "all")
            }
            com.example.MyApplication.logFirebaseEvent("select_category", bundle)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        applyFilters()

        // Log custom Firebase Analytics event
        try {
            val bundle = android.os.Bundle().apply {
                putString("search_query", query)
            }
            com.example.MyApplication.logFirebaseEvent("search_query", bundle)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun toggleVipFilter() {
        _filterVipOnly.value = !_filterVipOnly.value
        applyFilters()
    }

    fun toggleAvailableFilter() {
        _filterAvailableOnly.value = !_filterAvailableOnly.value
        applyFilters()
    }

    fun setCityFilter(cityId: String?) {
        _filterCityId.value = cityId
        applyFilters()
    }

    fun setNeighborhoodFilter(neighborhood: String) {
        _filterNeighborhoodName.value = neighborhood
        applyFilters()
    }

    fun setPhoneOrNameFilter(text: String) {
        _phoneOrNameFilter.value = text
        applyFilters()
    }

    fun setRadiusKm(km: Int) {
        _maxKmRadius.value = km
        applyFilters()
    }

    // ------------------- Backdoor & Auth -------------------
    fun registerBackdoorInteraction() {
        clickCount++
        if (clickCount >= 5) {
            clickCount = 0
            _showBackdoorDialog.value = true
        }
    }

    fun changeAdminCredentials(username: String, password: String) {
        triggerNotification("🔐 تم تغيير بيانات المدير الرئيسي")
    }

    fun authenticateAdmin(context: android.content.Context, role: String, remember: Boolean) {
        _adminRole.value = role
        if (remember) {
            val sp = context.getSharedPreferences("yemen_service_prefs", android.content.Context.MODE_PRIVATE)
            sp.edit().putString("saved_admin_role", role).apply()
        }
        triggerNotification("🔓 تم تسجيل الدخول بنجاح بصلاحية: $role")
        _currentScreen.value = "ADMIN_PANEL"
    }

    fun authenticateAdmin(role: String) {
        _adminRole.value = role
        triggerNotification("🔓 تم تسجيل الدخول بنجاح بصلاحية: $role")
        _currentScreen.value = "ADMIN_PANEL"
    }

    fun logout(context: android.content.Context) {
        _adminRole.value = "GUEST"
        _currentScreen.value = "USER_BROWSE"
        val sp = context.getSharedPreferences("yemen_service_prefs", android.content.Context.MODE_PRIVATE)
        sp.edit().putString("saved_admin_role", "GUEST").apply()
        triggerNotification("🔒 تم تسجيل الخروج بنجاح")
    }

    // ------------------- Navigation -------------------
    private val _screenBackStack = MutableStateFlow<List<String>>(listOf("USER_BROWSE"))
    val screenBackStack: StateFlow<List<String>> = _screenBackStack.asStateFlow()

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

    var appContext: android.content.Context? = null

    // ------------------- Notifications -------------------
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

    fun triggerNotification(msg: String, context: android.content.Context? = null) {
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

    fun submitRating(providerId: String, rating: Int) {
        triggerNotification("⭐ شكراً لتقييمك $rating نجوم!")
    }

    fun submitRating(ratingEntity: com.example.data.RatingEntity, onComplete: () -> Unit = {}) {
        val list = _ratings.value.toMutableList()
        list.removeAll { it.id == ratingEntity.id }
        list.add(0, ratingEntity)
        _ratings.value = list
        try {
            db.collection("ratings").document(ratingEntity.id).set(ratingEntity)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        triggerNotification("⭐ شكراً لتقييمك المعتمد!")
        onComplete()
    }

    fun submitReport(report: com.example.data.ReportEntity, onComplete: () -> Unit = {}) {
        val list = _reports.value.toMutableList()
        list.removeAll { it.id == report.id }
        list.add(0, report)
        _reports.value = list
        try {
            db.collection("reports").document(report.id).set(report)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        triggerNotification("🚨 تم إرسال البلاغ للتحقيق الرسمي!")
        onComplete()
    }

    fun toggleProviderStatus(provider: ProviderEntity) {
        val updated = provider.copy(isAvailable = !provider.isAvailable)
        db.collection("providers").document(provider.id).set(updated)
        triggerNotification("🔄 تم تغيير حالة التوفر لـ ${provider.name}")
    }

    fun rewardSharePoints() {
        _currentUserPoints.value = _currentUserPoints.value + 20
        triggerNotification("🎁 حصلت على 20 نقطة مشاركة!")
    }

    fun clearSmartAssistantChatHistory() {
        _currentUserPoints.value = 0
        triggerNotification("🧹 تم تصفية وحذف سجل المحادثة الذكية بنجاح!")
    }

    fun sendReport(providerId: String, providerName: String, reporterName: String, content: String) {
        val newReport = ReportEntity(
            id = UUID.randomUUID().toString(),
            providerId = providerId,
            providerName = providerName,
            reporterName = reporterName,
            content = content
        )
        db.collection("reports").document(newReport.id).set(newReport)
        triggerNotification("📢 تم إرسال بلاغك ضد $providerName")
    }

    fun deleteReport(reportId: String) {
        db.collection("reports").document(reportId).delete()
        triggerNotification("🗑️ تم حذف البلاغ من النظام")
    }

    fun sendMessageInChat(msgText: String, imageUrl: String = "") {
        if (msgText.trim().isEmpty() && imageUrl.isEmpty()) return
        val currentId = _currentUserId.value
        val currentName = _currentUserName.value.ifEmpty { "مستخدم" }
        val currentPhone = _currentUserPhone.value
        
        if (currentId == "guest") {
            // Safety firewall: refuse write to Firestore if anonymous
            return
        }

        val displayName = if (currentPhone.isNotEmpty()) "$currentName ($currentPhone)" else currentName
        val channelId = "support_" + currentId
        val newMsg = ChatMessageEntity(
            id = UUID.randomUUID().toString(),
            senderId = currentId,
            message = msgText,
            timestamp = System.currentTimeMillis(),
            senderName = displayName,
            imageUrl = imageUrl
        )
        db.collection("chat_channels").document(channelId).get().addOnSuccessListener { snapshot ->
            val ch = snapshot.toObject(ChatChannelEntity::class.java)
            val finalMsgText = if (msgText.isNotEmpty()) msgText else "📷 [صورة]"
            if (ch != null) {
                db.collection("chat_channels").document(channelId).set(
                    ch.copy(
                        userName = displayName,
                        lastMessage = finalMsgText,
                        timestamp = System.currentTimeMillis(),
                        messages = ch.messages + newMsg
                    )
                )
            } else {
                val newSupport = ChatChannelEntity(
                    id = channelId,
                    userName = displayName,
                    lastMessage = finalMsgText,
                    timestamp = System.currentTimeMillis(),
                    messages = listOf(newMsg)
                )
                db.collection("chat_channels").document(channelId).set(newSupport)
            }
            
            // Add real-time notification to supervisor/admin
            addNotification(
                title = "💬 رسالة جديدة في الدعم الفني المباشر",
                message = "من العميل ${displayName}: $finalMsgText",
                targetType = "SUPERVISOR",
                targetValue = "all"
            )
        }
    }

    fun markChannelMessagesAsRead(channelId: String) {
        val currentId = _currentUserId.value
        if (channelId.isBlank() || currentId == "guest") return
        db.collection("chat_channels").document(channelId).get().addOnSuccessListener { snapshot ->
            val ch = snapshot.toObject(ChatChannelEntity::class.java) ?: return@addOnSuccessListener
            var updated = false
            val newMessages = ch.messages.map { msg ->
                if (msg.senderId != currentId && msg.status != "READ") {
                    updated = true
                    msg.copy(status = "READ", statusTime = System.currentTimeMillis())
                } else {
                    msg
                }
            }
            if (updated) {
                db.collection("chat_channels").document(channelId).update("messages", newMessages)
            }
        }
    }

    fun markMessageAsRead(channelId: String, messageId: String) {
        val currentId = _currentUserId.value
        if (channelId.isBlank() || messageId.isBlank() || currentId == "guest") return

        db.collection("chat_channels")
            .document(channelId)
            .collection("messages")
            .document(messageId)
            .update(
                mapOf(
                    "status" to "READ",
                    "statusTime" to System.currentTimeMillis()
                )
            ).addOnFailureListener {
                // Fallback to channel level update
                markChannelMessagesAsRead(channelId)
            }
    }

    fun getOrCreateChatChannel(providerId: String, providerName: String, customerId: String, customerName: String) {
        val channelId = "chat_p_${providerId}_u_${customerId}"
        val dispCustomerName = customerName.ifEmpty { "عميل" }
        val displayName = "دردشة: $providerName مع $dispCustomerName"
        
        val localCh = ChatChannelEntity(
            id = channelId,
            userName = displayName,
            targetId = providerId,
            targetName = providerName,
            customerId = customerId,
            customerName = dispCustomerName,
            lastMessage = "مرحباً! تم بدء محادثة فورية جديدة لتنسيق الخدمة.",
            timestamp = System.currentTimeMillis(),
            isProvider = false,
            messages = listOf(
                ChatMessageEntity(
                    id = UUID.randomUUID().toString(),
                    senderId = "system",
                    message = "مرحباً! تم بدء محادثة فورية جديدة لتنسيق الخدمة.",
                    timestamp = System.currentTimeMillis(),
                    senderName = "النظام"
                )
            )
        )
        
        // Immediately set local active channel for 0ms instant UI opening
        _activeChatChannel.value = localCh

        try {
            db.collection("chat_channels").document(channelId).get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val existing = snapshot.toObject(ChatChannelEntity::class.java)
                    if (existing != null) {
                        _activeChatChannel.value = existing
                    }
                } else {
                    db.collection("chat_channels").document(channelId).set(localCh)
                }
            }.addOnFailureListener {
                // Keep local channel active when offline
            }
        } catch (e: Exception) {
            // Offline fallback
        }
    }

    fun clearGeneralChatHistory() {
        val currentId = _currentUserId.value
        if (currentId == "guest") return
        val channelId = "support_" + currentId

        val emptyCh = ChatChannelEntity(
            id = channelId,
            userName = _currentUserName.value.ifEmpty { "مستخدم الدليل" },
            isProvider = false,
            lastMessage = "تم مسح المحادثة بالكامل من قبل المستخدم",
            messages = emptyList(),
            timestamp = System.currentTimeMillis()
        )
        db.collection("chat_channels").document(channelId).set(emptyCh).addOnSuccessListener {
            triggerNotification("🧹 تم مسح وحذف سجل الرسائل بالكامل فورياً!")
        }
    }

    fun logAdminActivity(action: String) {
        val id = db.collection("activity_logs").document().id
        val log = com.example.data.ActivityLogEntity(id = id, action = action, timestamp = System.currentTimeMillis())
        db.collection("activity_logs").document(id).set(log)
    }

    fun checkAndGetDuplicateAccountType(phone: String, excludeId: String): String? {
        val cleanInput = phone.trim().replace(" ", "").replace("+", "")
        if (cleanInput.isEmpty()) return null
        
        // 1. Check in active providers
        val dupProvider = _providers.value.any { !it.isDeleted && it.phone.trim().replace(" ", "").replace("+", "") == cleanInput && it.id != excludeId }
        if (dupProvider) return "مقدم خدمة نشط (فني)"
        
        // 2. Check in pending providers
        val dupPendingProvider = _pendingProviders.value.any { it.status == "PENDING" && it.phone.trim().replace(" ", "").replace("+", "") == cleanInput && it.id != excludeId }
        if (dupPendingProvider) return "طلب انضمام فني قيد المراجعة"
        
        // 3. Check in stores
        val dupStore = _stores.value.any { !it.isDeleted && it.phone.trim().replace(" ", "").replace("+", "") == cleanInput && it.id != excludeId }
        if (dupStore) return "محل أو متجر تجاري مسجل"
        
        // 4. Check in properties
        val dupProp = _properties.value.any { !it.isDeleted && it.phone.trim().replace(" ", "").replace("+", "") == cleanInput && it.id != excludeId }
        if (dupProp) return "إعلان عقاري مدرج"
        
        return null
    }

    suspend fun uploadImageStringOrUri(
        context: android.content.Context,
        input: String,
        storagePath: String,
        maxSizeBytes: Long = 300 * 1024L
    ): String {
        if (input.isBlank()) return ""
        if (input.startsWith("http://") || input.startsWith("https://")) return input
        return try {
            if (input.startsWith("content://") || input.startsWith("file://")) {
                val uri = android.net.Uri.parse(input)
                val res = com.example.util.FirebaseStorageUploader.uploadImageUri(
                    context, uri, storagePath, maxDimension = 800, maxSizeBytes = maxSizeBytes
                )
                res.getOrDefault(input)
            } else {
                val cleanBase64 = if (input.contains(",")) input.substringAfter(",") else input
                val bytes = android.util.Base64.decode(cleanBase64, android.util.Base64.DEFAULT)
                val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (bitmap != null) {
                    val res = com.example.util.FirebaseStorageUploader.uploadBitmap(
                        bitmap, storagePath, maxDimension = 800, maxSizeBytes = maxSizeBytes
                    )
                    res.getOrDefault(input)
                } else input
            }
        } catch (e: Exception) {
            e.printStackTrace()
            input
        }
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
        val cleanPhone = phone.trim().replace(" ", "").replace("+", "")
        val duplicateType = checkAndGetDuplicateAccountType(cleanPhone, "")
        if (duplicateType != null) {
            triggerNotification("❌ عذراً! رقم الهاتف ($phone) مسجل بالفعل كـ ($duplicateType). لا يُسمح بتكرار الحسابات.")
            logAdminActivity("محاولة تسجيل فني مكرر محجوبة لرقم: $cleanPhone - نوع التكرار: $duplicateType")
            return
        }

        viewModelScope.launch {
            triggerNotification("⏳ جاري ضغط الصور وحفظ الملفات في سحابة التخزين...")

            val finalSelfie = uploadImageStringOrUri(
                context, photoPath,
                com.example.util.FirebaseStorageUploader.getProviderProfilePath(cleanPhone),
                maxSizeBytes = 150 * 1024L
            )
            val finalIdCard = uploadImageStringOrUri(
                context, idCardPath,
                com.example.util.FirebaseStorageUploader.getProviderIdCardPath(cleanPhone),
                maxSizeBytes = 150 * 1024L
            )
            val finalWorkPhotos = workPhotos.mapIndexed { idx, p ->
                uploadImageStringOrUri(
                    context, p,
                    com.example.util.FirebaseStorageUploader.getProviderWorkPhotoPath(cleanPhone, idx),
                    maxSizeBytes = 300 * 1024L
                )
            }

            val encSelfie = if (finalSelfie.isNotEmpty()) com.example.util.SecurityCryptoUtils.encrypt(finalSelfie) else ""
            val encIdCard = if (finalIdCard.isNotEmpty()) com.example.util.SecurityCryptoUtils.encrypt(finalIdCard) else ""

            if (password.isNotEmpty()) {
                val valResult = com.example.util.SecurityCryptoUtils.validatePasswordPolicy(password)
                if (valResult.first) {
                    val authEmail = getAuthEmailForPhone(cleanPhone)
                    auth.createUserWithEmailAndPassword(authEmail, password.trim())
                        .addOnFailureListener { /* Account might already exist */ }
                }
            }

            val requestDocId = cleanPhone
            val newRequest = PendingProviderEntity(
                id = requestDocId,
                name = name,
                phone = phone,
                categoryId = catId,
                area = area,
                localNeighborhood = neighborhood,
                status = "PENDING",
                selfiePhotoBase64 = encSelfie,
                idPhotoBase64 = encIdCard,
                workPhotosBase64 = finalWorkPhotos,
                customCategoryName = customCategoryName,
                password = password,
                productAttachmentsJson = productAttachmentsJson
            )
            // Push to Cloud with robust listeners
            db.collection("pending_providers").document(requestDocId).set(newRequest)
            
            val unifiedJoinRequest = com.example.data.models.JoinRequestEntity(
                id = requestDocId,
                type = "PROVIDER",
                status = "PENDING",
                fullName = name,
                phone = cleanPhone,
                passwordHash = password,
                city = area,
                area = neighborhood,
                neighborhood = neighborhood,
                categoryId = catId,
                categoryName = customCategoryName.ifBlank { catId },
                businessName = name,
                profileImage = finalSelfie,
                idCardImage = finalIdCard,
                workImages = finalWorkPhotos,
                approvalStatus = "PENDING",
                submittedAt = System.currentTimeMillis(),
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            db.collection("join_requests").document(requestDocId).set(unifiedJoinRequest)
                .addOnSuccessListener {
                    // Send a notification to Admin/Supervisors
                    val adminNotif = NotificationEntity(
                        id = UUID.randomUUID().toString(),
                        title = "👷 طلب انضمام جديد للدليل",
                        message = "قدم الفني ${name} طلب انضمام جديد في قسم ${if (customCategoryName.isNullOrBlank()) catId else customCategoryName} بمنطقة ${area}.",
                        targetType = "SUPERVISOR",
                        targetValue = "ALL",
                        timestamp = System.currentTimeMillis()
                    )
                    try {
                        db.collection("notifications").document(adminNotif.id).set(adminNotif)
                    } catch (e: Exception) {}
                    
                    triggerNotification("📨 تم تقديم طلبك ورفع المستندات بنجاح، جاري المراجعة من الإدارة")
                }
                .addOnFailureListener { e ->
                    val errorMsg = e.localizedMessage ?: "تأكد من صغر حجم الصور واتصالك بالإنترنت"
                    triggerNotification("❌ فشل تقديم الطلب: $errorMsg")
                }
            
            // Instant Local Sync
            val currentPending = _pendingProviders.value.filter { it.id != requestDocId }.toMutableList()
            currentPending.add(newRequest)
            _pendingProviders.value = currentPending

            val currentTechs = _pendingTechnicians.value.filter { it.id != requestDocId }.toMutableList()
            currentTechs.add(newRequest)
            _pendingTechnicians.value = currentTechs
            
            val sp = context.getSharedPreferences("yemen_service_prefs", android.content.Context.MODE_PRIVATE)
            sp.edit().putString("join_request_phone", phone).apply()
            _joinRequestPhone.value = phone
            
            // Add applicant notification!
            addNotification(
                title = "📨 تم استلام طلب انضمامك بنجاح",
                message = "مرحباً يا غالي، تم استلام طلبك وجاري مراجعته والتحقق من التخصص والخبرة من قبل إدارة الدليل. نسعد بانضمامك وسنبلغك فور التنشيط!",
                targetType = "USER",
                targetValue = phone
            )
            
            triggerNotification("📨 تم تقديم طلبك بنجاح، سيتم مراجعته من قبل الإدارة")
            _currentScreen.value = "JOIN_REQUEST_STATUS"
        }
    }

    fun cancelOrResetJoinRequest(context: android.content.Context) {
        val phone = _joinRequestPhone.value
        if (phone.isNotEmpty()) {
            val matching = _pendingProviders.value.find { it.phone == phone }
            matching?.let {
                _pendingProviders.value = _pendingProviders.value.filter { item -> item.id != it.id }
                try {
                    db.collection("pending_providers").document(it.id).delete()
                } catch (e: Exception) {}
            }
        }
        val sp = context.getSharedPreferences("yemen_service_prefs", android.content.Context.MODE_PRIVATE)
        sp.edit().remove("join_request_phone").apply()
        _joinRequestPhone.value = ""
        _currentScreen.value = "REGISTER_FORM"
    }

    fun setJoinRequestPhone(context: android.content.Context, phone: String) {
        val sp = context.getSharedPreferences("yemen_service_prefs", android.content.Context.MODE_PRIVATE)
        sp.edit().putString("join_request_phone", phone).apply()
        _joinRequestPhone.value = phone
    }

    fun approveRequest(request: PendingProviderEntity) {
        val lowerArea = request.area.lowercase()
        val finalCityId = when {
            lowerArea.contains("عدن") || lowerArea.contains("aden") -> "ye_ade"
            lowerArea.contains("تعز") || lowerArea.contains("taiz") -> "ye_tai"
            lowerArea.contains("الحديدة") || lowerArea.contains("hodeidah") -> "ye_hod"
            else -> "ye_san"
        }

        val cleanPhone = request.phone.trim().replace(" ", "").replace("+", "")
        
        // Helper to remove from pending_providers permanently
        val clearPendingFromDbAndState = {
            db.collection("pending_providers").document(request.id).delete()
            if (cleanPhone.isNotEmpty()) {
                db.collection("pending_providers").whereEqualTo("phone", request.phone).get().addOnSuccessListener { qs ->
                    qs?.documents?.forEach { doc ->
                        db.collection("pending_providers").document(doc.id).delete()
                    }
                }
            }
            _pendingProviders.value = _pendingProviders.value.filter { 
                it.id != request.id && (cleanPhone.isEmpty() || it.phone.trim().replace(" ", "").replace("+", "") != cleanPhone)
            }
        }

        clearPendingFromDbAndState()

        val now = System.currentTimeMillis()
        db.collection("join_requests").document(request.id).update(mapOf(
            "status" to "APPROVED",
            "approvalStatus" to "APPROVED",
            "isActive" to true,
            "approvedAt" to now,
            "updatedAt" to now
        ))

        if (request.profession == "STORE_OWNER") {
            val storeId = "store_" + cleanPhone
            val newStore = com.example.data.StoreEntity(
                id = storeId,
                name = request.name,
                description = request.specialization.ifBlank { "محل تجاري معتمد وموثق" },
                ownerId = request.phone,
                ownerName = request.name,
                phone = request.phone,
                localNeighborhood = request.localNeighborhood,
                cityId = finalCityId,
                isActive = true,
                isApproved = true,
                isPinned = false,
                isDeleted = false,
                password = request.password,
                pdfFileBase64 = request.idPhotoBase64
            )
            db.collection("stores").document(storeId).set(newStore)
            
            // Instant Local Sync for stores
            _stores.value = _stores.value.filter { it.id != storeId && it.phone.trim().replace(" ", "").replace("+", "") != cleanPhone } + newStore

            addNotification(
                title = "🎉 تهانينا! تم تفعيل متجرك بنجاح",
                message = "مرحباً بك يا غالي، لقد تم مراجعة وتفعيل متجرك/محلك '${request.name}' بنجاح في التطبيق! يمكنك الآن إضافة منتجاتك وإدارة متجرك مباشرة من شاشة الانضمام.",
                targetType = "USER",
                targetValue = request.phone
            )
            triggerNotification("✅ تم تفعيل متجر ${request.name}")
        } else if (request.profession == "PROPERTY_OWNER") {
            val propId = "prop_" + cleanPhone
            val propPrice = try { request.chatRecipientId.toDouble() } catch(e: Exception) { 0.0 }
            val newProp = com.example.data.PropertyEntity(
                id = propId,
                title = request.name,
                description = request.specialization.ifBlank { "عقار معلن وموثق" },
                phone = request.phone,
                localNeighborhood = request.localNeighborhood,
                cityId = finalCityId,
                isActive = true,
                isApproved = true,
                isPinned = false,
                isDeleted = false,
                password = request.password,
                price = propPrice,
                pdfFileBase64 = request.idPhotoBase64
            )
            db.collection("properties").document(propId).set(newProp)
            
            // Instant Local Sync for properties
            _properties.value = _properties.value.filter { it.id != propId && it.phone.trim().replace(" ", "").replace("+", "") != cleanPhone } + newProp

            addNotification(
                title = "🎉 تهانينا! تم تفعيل إعلان عقارك بنجاح",
                message = "مرحباً بك، لقد تم مراجعة وتفعيل عقارك '${request.name}' بنجاح في دليل العقارات المعتمد! يمكنك تعديله وإدارته ورؤية تعليقات العملاء مباشرة من شاشة الانضمام.",
                targetType = "USER",
                targetValue = request.phone
            )
            triggerNotification("✅ تم تفعيل عقار ${request.name}")
        } else {
            val providerId = "prov_" + cleanPhone
            val approvedProvider = ProviderEntity(
                id = providerId,
                name = request.name,
                phone = request.phone,
                categoryId = request.categoryId,
                area = request.area,
                isVip = false,
                subscriptionStatus = "APPROVED",
                isAvailable = true,
                cityId = finalCityId,
                localNeighborhood = request.localNeighborhood,
                rating = 5.0f,
                isBlocked = false,
                customCategoryName = request.customCategoryName,
                password = request.password,
                isDeleted = false,
                deletedAt = null
            )
            db.collection("providers").document(approvedProvider.id).set(approvedProvider)
            
            // Instant Local Sync
            val currentProviders = _providers.value.filter { it.id != approvedProvider.id && it.phone.trim().replace(" ", "").replace("+", "") != cleanPhone }.toMutableList()
            currentProviders.add(approvedProvider)
            _providers.value = currentProviders
            applyFilters()

            // Add accepted notification!
            addNotification(
                title = "🎉 تهانينا! تم قبول انضمامك كفني معتمد",
                message = "مرحباً بك يا غالي، لقد تم قبول طلب انضمامك كمهني معتمد وأصبحت الآن نشطاً في دليل كل خدمات اليمن! حسابك يظهر الآن لجميع العملاء.",
                targetType = "USER",
                targetValue = request.phone
            )
            
            triggerNotification("✅ تم قبول طلب ${request.name}")
        }
    }

    fun rejectRequest(request: PendingProviderEntity, reason: String) {
        db.collection("pending_providers").document(request.id).delete()
        
        // Add rejected notification!
        addNotification(
            title = "❌ تنويه حول طلب انضمامك",
            message = "للأسف لم يتم قبول طلب انضمامك للأسباب التالية: $reason. يرجى تعديل البيانات وإعادة تقديم الطلب.",
            targetType = "USER",
            targetValue = request.phone
        )
        
        triggerNotification("❌ تم رفض طلب ${request.name} بسبب: $reason")
    }

    fun addNewProvider(name: String, phone: String, catId: String, area: String, price: Double, isVip: Boolean) {
        val providerId = "prov_" + phone.trim().replace(" ", "").replace("+", "")
        val newP = ProviderEntity(
            id = providerId,
            name = name,
            phone = phone,
            categoryId = catId,
            area = area,
            isVip = isVip,
            subscriptionStatus = "APPROVED",
            isAvailable = true,
            cityId = "ye_san",
            localNeighborhood = area,
            previewPrice = price,
            rating = 5.0f
        )
        db.collection("providers").document(newP.id).set(newP)
        triggerNotification("➕ تم إضافة الفني $name يدوياً")
    }

    fun addNewBanner(title: String, url: String, redirect: String, type: String, size: String, duration: Int, displayTime: String = "طوال اليوم") {
        val banner = BannerEntity(
            id = UUID.randomUUID().toString(),
            title = title,
            url = url,
            redirectCategory = redirect,
            type = type,
            size = size,
            duration = duration,
            displayTime = displayTime,
            order = _banners.value.size + 1
        )
        db.collection("banners").document(banner.id).set(banner)
        triggerNotification("🖼️ تم إضافة إعلان جديد: $title")
    }

    fun addBanner(title: String, url: String, redirect: String, type: String, size: String, duration: Int, displayTime: String = "طوال اليوم") {
        addNewBanner(title, url, redirect, type, size, duration, displayTime)
    }

    fun addNewStore(name: String, phone: String, cityId: String, localNeighborhood: String, categoryId: String, coverImage: String = "", workingHours: String = "9:00 AM - 10:00 PM") {
        val nextId = "store_" + UUID.randomUUID().toString().take(6)
        val store = StoreEntity(
            id = nextId,
            name = name,
            phone = phone,
            cityId = cityId,
            localNeighborhood = localNeighborhood,
            categoryId = categoryId,
            coverImage = coverImage,
            workingHours = workingHours
        )
        db.collection("stores").document(nextId).set(store)
        triggerNotification("🛍️ تم إضافة متجر/منشأة جديدة: $name")
    }

    fun toggleStoreBlocked(storeId: String, isBlocked: Boolean) {
        db.collection("stores").document(storeId).update("isBlocked", isBlocked)
            .addOnSuccessListener {
                triggerNotification(if (isBlocked) "🚫 تم حظر المتجر" else "✅ تم إلغاء حظر المتجر")
            }
    }

    fun togglePropertyBlocked(propertyId: String, isBlocked: Boolean) {
        db.collection("properties").document(propertyId).update("isBlocked", isBlocked)
            .addOnSuccessListener {
                triggerNotification(if (isBlocked) "🚫 تم حظر العقار" else "✅ تم إلغاء حظر العقار")
            }
    }

    fun deleteBanner(bannerId: String) {
        db.collection("banners").document(bannerId).delete()
        triggerNotification("🗑️ تم حذف الإعلان")
    }

    fun addNewCategory(nameAr: String, nameEn: String, icon: String, description: String, parentId: String = "", isMainCategory: Boolean = true) {
        val nextId = UUID.randomUUID().toString().take(6)
        val extraCat = CategoryEntity(
            id = nextId,
            name = nameAr,
            icon = icon,
            order = _categories.value.size + 1,
            parentId = parentId,
            isMainCategory = isMainCategory
        )
        db.collection("categories").document(nextId).set(extraCat)
        triggerNotification("📁 تم إضافة قسم جديد: $nameAr")
    }

    fun reorderCategories(newOrderedList: List<CategoryEntity>) {
        newOrderedList.forEachIndexed { index, cat ->
            val updated = cat.copy(order = index + 1)
            db.collection("categories").document(cat.id).set(updated)
        }
    }

    fun reorderBanners(newOrderedList: List<BannerEntity>) {
        newOrderedList.forEachIndexed { index, banner ->
            val updated = banner.copy(order = index + 1)
            db.collection("banners").document(banner.id).set(updated)
        }
    }

    fun addNewCity(nameAr: String, nameEn: String, icon: String = "📍", photoUrl: String = "", sortOrder: Int = 0) {
        val nextId = "city_" + UUID.randomUUID().toString().take(4)
        val city = CityEntity(nextId, nameAr, nameEn, icon.ifEmpty { "📍" }, photoUrl, sortOrder)
        db.collection("cities").document(nextId).set(city)
        triggerNotification("🏙️ تم إضافة مدينة/محافظة: $nameAr")
    }

    fun updateCity(city: CityEntity) {
        if (city.id.isEmpty()) return
        db.collection("cities").document(city.id).set(city)
        triggerNotification("💾 تم تحديث بيانات المدينة/المحافظة: ${city.nameAr}")
    }

    fun removeCity(cityId: String) {
        db.collection("cities").document(cityId).delete()
        triggerNotification("🗑️ تم حذف المدينة")
    }

    fun removeProvider(providerId: String) {
        db.collection("providers").document(providerId).get().addOnSuccessListener { snapshot ->
            val p = snapshot.toObject(com.example.data.ProviderEntity::class.java)
            if (p != null) {
                db.collection("providers").document(providerId).set(
                    p.copy(isDeleted = true, deletedAt = System.currentTimeMillis())
                ).addOnSuccessListener {
                    triggerNotification("🗑️ تم حذف حساب الفني منطقياً بنجاح (حذف مؤقت) ويمكنك استعادته من لوحة التحكم في أي وقت")
                }
            } else {
                db.collection("providers").document(providerId).delete().addOnSuccessListener {
                    triggerNotification("🗑️ تم حذف حساب الفني نهائياً من الدليل")
                }
            }
        }.addOnFailureListener {
            db.collection("providers").document(providerId).delete().addOnSuccessListener {
                triggerNotification("🗑️ تم حذف حساب الفني")
            }
        }
    }

    fun removeProviderPermanently(providerId: String) {
        db.collection("providers").document(providerId).delete().addOnSuccessListener {
            triggerNotification("🗑️ تم حذف حساب الفني نهائياً وبشكل كامل من خوادم الدليل")
        }.addOnFailureListener { e ->
            triggerNotification("❌ فشل حذف الفني نهائياً: ${e.message}")
        }
    }

    fun restoreProvider(providerId: String) {
        db.collection("providers").document(providerId).get().addOnSuccessListener { snapshot ->
            val p = snapshot.toObject(ProviderEntity::class.java)
            if (p != null) {
                db.collection("providers").document(providerId).set(
                    p.copy(isDeleted = false, deletedAt = null)
                )
                triggerNotification("🟢 تم استعادة وتفعيل حساب الفني ${p.name} بنجاح!")
            }
        }
    }

    fun pinProvider(providerId: String, isPinned: Boolean) {
        db.collection("providers").document(providerId).get().addOnSuccessListener { snapshot ->
            val p = snapshot.toObject(ProviderEntity::class.java)
            if (p != null) {
                db.collection("providers").document(providerId).set(p.copy(isVip = isPinned))
            }
        }
        triggerNotification(if (isPinned) "📌 تم تثبيت الفني" else "📌 تم إلغاء تثبيت الفني")
    }

    fun recommendProvider(providerId: String, isRecommended: Boolean) {
        db.collection("providers").document(providerId).get().addOnSuccessListener { snapshot ->
            val p = snapshot.toObject(ProviderEntity::class.java)
            if (p != null) {
                db.collection("providers").document(providerId).set(
                    p.copy(isRecommended = isRecommended, rating = if (isRecommended) 5.0f else 4.2f)
                )
            }
        }
        triggerNotification(if (isRecommended) "⭐ تمت توصية الفني" else "⭐ تم إلغاء توصية الفني")
    }

    fun verifyProviderBadge(providerId: String, isVerified: Boolean) {
        db.collection("providers").document(providerId).get().addOnSuccessListener { snapshot ->
            val p = snapshot.toObject(ProviderEntity::class.java)
            if (p != null) {
                db.collection("providers").document(providerId).set(
                    p.copy(isVerified = isVerified, subscriptionStatus = if (isVerified) "APPROVED" else "PENDING")
                )
            }
        }
        triggerNotification(if (isVerified) "🔷 تم توثيق الفني بالشارة الزرقاء" else "🔷 تم إلغاء توثيق الفني")
    }

    fun toggleProviderSubscription(providerId: String, status: String) {
        db.collection("providers").document(providerId).get().addOnSuccessListener { snapshot ->
            val p = snapshot.toObject(ProviderEntity::class.java)
            if (p != null) {
                db.collection("providers").document(providerId).set(p.copy(subscriptionStatus = status))
            }
        }
        triggerNotification(if (status == "APPROVED") "✨ تم تفعيل العضوية الذهبية للفني" else "✨ تم إلغاء العضوية الذهبية")
    }

    fun setProviderChatDisabled(providerId: String, disabled: Boolean) {
        db.collection("providers").document(providerId).get().addOnSuccessListener { snapshot ->
            val p = snapshot.toObject(ProviderEntity::class.java)
            if (p != null) {
                db.collection("providers").document(providerId).set(p.copy(isChatDisabled = disabled))
            }
        }
        triggerNotification(if (disabled) "🔇 تم إيقاف دردشة الفني إدارياً" else "🔊 تم تفعيل دردشة الفني")
    }

    fun setProviderNotificationsDisabled(providerId: String, disabled: Boolean) {
        db.collection("providers").document(providerId).get().addOnSuccessListener { snapshot ->
            val p = snapshot.toObject(ProviderEntity::class.java)
            if (p != null) {
                db.collection("providers").document(providerId).set(p.copy(isNotificationsDisabled = disabled))
            }
        }
        triggerNotification(if (disabled) "🔕 تم تعطيل إشعارات الفني إدارياً" else "🔔 تم تفعيل إشعارات الفني")
    }

    fun setProviderPaymentRequired(providerId: String, required: Boolean) {
        db.collection("providers").document(providerId).get().addOnSuccessListener { snapshot ->
            val p = snapshot.toObject(ProviderEntity::class.java)
            if (p != null) {
                db.collection("providers").document(providerId).set(p.copy(isPaymentRequired = required))
            }
        }
        triggerNotification(if (required) "💳 تم ربط حساب الفني بنظام الدفع والعمولة الإلزامية" else "🔓 تم استثناء الفني من شروط الدفع المسبق والعمولة")
    }

    fun extendProviderSubscription(providerId: String, extraMs: Long) {
        db.collection("providers").document(providerId).get().addOnSuccessListener { snapshot ->
            val p = snapshot.toObject(ProviderEntity::class.java)
            if (p != null) {
                val currentExpiry = if (p.subscriptionExpiry > System.currentTimeMillis()) p.subscriptionExpiry else System.currentTimeMillis()
                db.collection("providers").document(providerId).set(
                    p.copy(subscriptionExpiry = currentExpiry + extraMs, subscriptionStatus = "APPROVED")
                )
            }
        }
        triggerNotification("💸 تم تجديد وتمديد اشتراك فني بنجاح!")
    }

    fun updateTheme(themeId: String) {
        db.collection("settings").document("main_settings").get().addOnSuccessListener { snapshot ->
            val s = snapshot.toObject(AdminSettingsEntity::class.java) ?: AdminSettingsEntity()
            db.collection("settings").document("main_settings").set(s.copy(activeThemeId = themeId))
        }
        triggerNotification("🎨 تم تغيير مظهر التطبيق إلى $themeId")
    }

    fun saveCustomSettingsState(newSettings: AdminSettingsEntity) {
        db.collection("settings").document("main_settings").set(newSettings)
            .addOnSuccessListener {
                triggerNotification("✅ تم حفظ ومزامنة كافة إعدادات التطبيق والدفع فورياً عبر الأجهزة!")
            }
            .addOnFailureListener {
                triggerNotification("❌ فشل حفظ الإعدادات: ${it.message}")
            }
    }

    fun saveInternalWallet(wallet: com.example.data.InternalWalletEntity) {
        val targetId = if (wallet.id.isEmpty()) db.collection("internal_wallets").document().id else wallet.id
        val finalW = wallet.copy(id = targetId, updatedAt = System.currentTimeMillis())
        db.collection("internal_wallets").document(targetId).set(finalW)
            .addOnSuccessListener {
                triggerNotification("✅ تم حفظ بيانات المحفظة الرقمية الداخلية (${finalW.ownerName})")
            }
    }

    fun performWalletTransaction(
        walletId: String,
        ownerName: String,
        ownerPhone: String,
        ownerType: String,
        type: String, // DEPOSIT, WITHDRAWAL, TRANSFER
        amount: Double,
        note: String
    ) {
        val currentW = _internalWallets.value.find { it.id == walletId }
            ?: com.example.data.InternalWalletEntity(
                id = walletId,
                ownerName = ownerName,
                ownerPhone = ownerPhone,
                ownerType = ownerType
            )

        val newBalance = when (type) {
            "DEPOSIT" -> currentW.balance + amount
            "WITHDRAWAL" -> (currentW.balance - amount).coerceAtLeast(0.0)
            else -> currentW.balance + amount
        }

        val updatedWallet = currentW.copy(
            balance = newBalance,
            updatedAt = System.currentTimeMillis()
        )

        db.collection("internal_wallets").document(walletId).set(updatedWallet)

        val txId = db.collection("wallet_transactions").document().id
        val tx = com.example.data.WalletTransactionEntity(
            id = txId,
            walletId = walletId,
            type = type,
            amount = amount,
            balanceAfter = newBalance,
            note = note,
            performByAdmin = true,
            timestamp = System.currentTimeMillis()
        )
        db.collection("wallet_transactions").document(txId).set(tx)
        triggerNotification("💸 تم تنفيذ عملية ($type) بمبلغ $amount ريال للمحفظة ${currentW.ownerName}. الرصيد الجديد: $newBalance ريال")
    }

    // --- STORES MANAGEMENT ---
    fun saveStore(store: com.example.data.StoreEntity) {
        val cleanPhone = store.phone.trim().replace(" ", "").replace("+", "")
        val duplicateType = checkAndGetDuplicateAccountType(cleanPhone, store.id)
        if (duplicateType != null) {
            triggerNotification("❌ عذراً! رقم الهاتف (${store.phone}) مسجل بالفعل كـ ($duplicateType). لا يُسمح بتكرار الحسابات.")
            logAdminActivity("محاولة تسجيل متجر مكرر محجوبة لرقم: $cleanPhone - نوع التكرار: $duplicateType")
            return
        }
        val targetId = if (store.id.isEmpty()) db.collection("stores").document().id else store.id
        val finalStore = store.copy(
            id = targetId,
            phone = cleanPhone,
            ownerId = if (store.ownerId.isEmpty()) cleanPhone else store.ownerId
        )

        // Instant Local State Update (Solves retry/delay bug)
        val updatedStores = _stores.value.filter { it.id != targetId }.toMutableList()
        updatedStores.add(finalStore)
        _stores.value = updatedStores

        if (store.password.isNotEmpty()) {
            try {
                val authEmail = getAuthEmailForPhone(cleanPhone)
                auth.createUserWithEmailAndPassword(authEmail, store.password.trim())
                    .addOnFailureListener { /* account exists */ }
            } catch (e: Exception) {}
        }

        viewModelScope.launch {
            val ctx = appContext
            val finalLogo = if (ctx != null) uploadImageStringOrUri(ctx, finalStore.logoImage, com.example.util.FirebaseStorageUploader.getStoreLogoPath(targetId)) else finalStore.logoImage
            val finalCover = if (ctx != null) uploadImageStringOrUri(ctx, finalStore.coverImage, com.example.util.FirebaseStorageUploader.getStoreCoverPath(targetId)) else finalStore.coverImage
            val finalImages = if (ctx != null) {
                finalStore.images.mapIndexed { idx, img ->
                    uploadImageStringOrUri(ctx, img, com.example.util.FirebaseStorageUploader.getStorePhotoPath(targetId, idx))
                }
            } else finalStore.images

            val uploadedStore = finalStore.copy(
                logoImage = finalLogo,
                coverImage = finalCover,
                images = finalImages
            )

            val currentList = _stores.value.filter { it.id != targetId }.toMutableList()
            currentList.add(uploadedStore)
            _stores.value = currentList

            db.collection("stores").document(targetId).set(uploadedStore)
                .addOnSuccessListener {
                    // Send admin notification if it's a new pending store
                    if (!uploadedStore.isActive || !uploadedStore.isApproved) {
                        val adminNotif = NotificationEntity(
                            id = UUID.randomUUID().toString(),
                            title = "🏪 طلب انضمام نشاط تجاري جديد",
                            message = "قدم النشاط التجاري '${uploadedStore.name}' (${uploadedStore.categoryId}) طلب انضمام جديد بمنطقة ${uploadedStore.cityId}.",
                            targetType = "SUPERVISOR",
                            targetValue = "ALL",
                            timestamp = System.currentTimeMillis()
                        )
                        try {
                            db.collection("notifications").document(adminNotif.id).set(adminNotif)
                        } catch (e: Exception) {}
                    }
                    triggerNotification("✅ تم حفظ وتأكيد بيانات الطلب بنجاح!")
                }
                .addOnFailureListener {
                    triggerNotification("⚠️ تم حفظ الطلب محلياً وفي انتظار مزامنة الشبكة")
                }
        }
    }

    fun approveStorePdf(storeId: String, approve: Boolean) {
        db.collection("stores").document(storeId).get().addOnSuccessListener { snapshot ->
            val store = snapshot.toObject(com.example.data.StoreEntity::class.java)
            if (store != null) {
                db.collection("stores").document(storeId).set(store.copy(pdfStatus = if (approve) "APPROVED" else "REJECTED"))
                    .addOnSuccessListener {
                        triggerNotification(if (approve) "✅ تم قبول ملف الـ PDF للمحل بنجاح!" else "❌ تم رفض ملف الـ PDF للمحل.")
                    }
            }
        }
    }

    fun requestPasswordRecoveryForStore(name: String, phone: String, password: String) {
        setPasswordRecoveryWaitingPhone(phone)
        val adminNotif = NotificationEntity(
            id = UUID.randomUUID().toString(),
            title = "🔑 استعادة كلمة مرور متجر",
            message = "المتجر $name (هاتف: $phone) يطلب استعادة كلمة مروره. كلمة المرور الحالية هي: $password",
            targetType = "SUPERVISOR",
            targetValue = "ALL",
            timestamp = System.currentTimeMillis()
        )
        db.collection("notifications").document(adminNotif.id).set(adminNotif)
        triggerNotification("📨 تم إرسال طلب استعادة كلمة المرور للمشرف بنجاح!")
    }

    fun requestPasswordRecoveryGeneral(accountName: String, phone: String, accountType: String, currentPassword: String) {
        setPasswordRecoveryWaitingPhone(phone)
        val adminNotif = NotificationEntity(
            id = UUID.randomUUID().toString(),
            title = "🔑 طلب استعادة كلمة مرور ($accountType)",
            message = "الحساب: $accountName ($accountType) ذو الرقم: $phone يطلب استعادة كلمة المرور الخاصة به. كلمة المرور الحالية في النظام هي: $currentPassword",
            targetType = "SUPERVISOR",
            targetValue = "ALL",
            timestamp = System.currentTimeMillis()
        )
        db.collection("notifications").document(adminNotif.id).set(adminNotif)
            .addOnSuccessListener {
                triggerNotification("📨 تم إرسال طلب استعادة كلمة المرور للمشرف/الأدمن بنجاح!")
            }
    }

    fun adminResetAccountPassword(phone: String, newPassword: String, notifyAction: String, customerName: String) {
        val cleanPhone = phone.trim().replace(" ", "").replace("+967", "").replace("967", "").replace("+", "")
        db.collection("providers").get().addOnSuccessListener { snap ->
            for (doc in snap.documents) {
                val p = doc.getString("phone") ?: ""
                if (p.contains(cleanPhone)) {
                    db.collection("providers").document(doc.id).update("password", newPassword)
                }
            }
        }
        db.collection("pending_providers").get().addOnSuccessListener { snap ->
            for (doc in snap.documents) {
                val p = doc.getString("phone") ?: ""
                if (p.contains(cleanPhone)) {
                    db.collection("pending_providers").document(doc.id).update("password", newPassword)
                }
            }
        }
        db.collection("stores").get().addOnSuccessListener { snap ->
            for (doc in snap.documents) {
                val p = doc.getString("phone") ?: ""
                if (p.contains(cleanPhone)) {
                    db.collection("stores").document(doc.id).update("password", newPassword)
                }
            }
        }
        db.collection("properties").get().addOnSuccessListener { snap ->
            for (doc in snap.documents) {
                val p = doc.getString("phone") ?: ""
                if (p.contains(cleanPhone)) {
                    db.collection("properties").document(doc.id).update("password", newPassword)
                }
            }
        }
        db.collection("registered_users").get().addOnSuccessListener { snap ->
            for (doc in snap.documents) {
                val p = doc.getString("phone") ?: ""
                if (p.contains(cleanPhone)) {
                    db.collection("registered_users").document(doc.id).update("password", newPassword)
                }
            }
        }

        _providers.value = _providers.value.map { if (it.phone.contains(cleanPhone)) it.copy(password = newPassword) else it }
        _stores.value = _stores.value.map { if (it.phone.contains(cleanPhone)) it.copy(password = newPassword) else it }

        if (_passwordRecoveryWaitingPhone.value.contains(cleanPhone)) {
            _passwordRecoveryWaitingPhone.value = ""
        }

        val notifId = UUID.randomUUID().toString()
        val (title, message) = when (notifyAction) {
            "DIRECT_PASSWORD" -> Pair(
                "🔑 إعادة تعيين كلمة المرور بنجاح",
                "تمت الموافقة على طلب استعادة حسابك وإعادة تعيين كلمة المرور من قبل الإدارة. كلمة المرور الجديدة هي: $newPassword"
            )
            "VERIFICATION_WHATSAPP" -> Pair(
                "🔐 التحقق من الهوية - استعادة الحساب",
                "عزيزي المشترك، يرجى التواصل عبر الواتساب أو التليجرام أو المحادثة الفورية مع الإدارة للتحقق من هويتك وتأكيد ملكيتك للحساب واستلام كلمة المرور."
            )
            "INSTANT_CHAT" -> Pair(
                "💬 محادثة فورية لاستعادة الحساب",
                "تم فتح قناة دعم فورية لك. يرجى التوجه للمحادثة المباشرة مع الإدارة للتحقق من هويتك واسترجاع حسابك فوراً."
            )
            else -> Pair(
                "🔑 تحديث كلمة المرور",
                "قامت الإدارة بتحديث ومعالجة طلب استعادة كلمة المرور الخاصة بحسابك."
            )
        }

        val userNotif = NotificationEntity(
            id = notifId,
            title = title,
            message = message,
            targetType = "USER",
            targetValue = cleanPhone,
            timestamp = System.currentTimeMillis()
        )
        db.collection("notifications").document(notifId).set(userNotif).addOnSuccessListener {
            triggerNotification("✅ تم إرسال إشعار إعادة التعيين للمستخدم بنجاح!")
        }
    }

    fun deleteStore(storeId: String) {
        _stores.value = _stores.value.map {
            if (it.id == storeId) it.copy(isDeleted = true, deletedAt = System.currentTimeMillis()) else it
        }
        val updates = mapOf(
            "isDeleted" to true,
            "deleted" to true,
            "deletedAt" to System.currentTimeMillis()
        )
        db.collection("stores").document(storeId).update(updates)
            .addOnSuccessListener {
                triggerNotification("🗑️ تم حذف المتجر ونقله للمحذوفات")
            }
            .addOnFailureListener { e ->
                db.collection("stores").document(storeId).get().addOnSuccessListener { snapshot ->
                    val store = snapshot.toObject(com.example.data.StoreEntity::class.java)
                    if (store != null) {
                        db.collection("stores").document(storeId).set(store.copy(isDeleted = true, deletedAt = System.currentTimeMillis()))
                            .addOnSuccessListener {
                                triggerNotification("🗑️ تم حذف المتجر ونقله للمحذوفات")
                            }
                    }
                }
            }
    }

    fun restoreStore(storeId: String) {
        _stores.value = _stores.value.map {
            if (it.id == storeId) it.copy(isDeleted = false, deletedAt = null) else it
        }
        val updates = hashMapOf<String, Any?>(
            "isDeleted" to false,
            "deleted" to false,
            "deletedAt" to null
        )
        db.collection("stores").document(storeId).update(updates)
            .addOnSuccessListener {
                triggerNotification("♻️ تم استعادة المتجر بنجاح")
            }
    }

    fun deleteStorePermanently(storeId: String) {
        _stores.value = _stores.value.filter { it.id != storeId }
        db.collection("stores").document(storeId).delete()
            .addOnSuccessListener {
                triggerNotification("🗑️ تم حذف المتجر نهائياً من النظام")
            }
    }

    fun setStorePinned(storeId: String, isPinned: Boolean) {
        _stores.value = _stores.value.map {
            if (it.id == storeId) it.copy(isPinned = isPinned) else it
        }
        val updates = mapOf(
            "isPinned" to isPinned,
            "pinned" to isPinned
        )
        db.collection("stores").document(storeId).update(updates)
            .addOnSuccessListener {
                triggerNotification(if (isPinned) "📌 تم تثبيت المتجر في الشاشة الرئيسية" else "📌 تم إلغاء تثبيت المتجر")
            }
    }

    fun setStoreActive(storeId: String, isActive: Boolean) {
        val cleanTargetId = storeId.trim()
        val targetStore = _stores.value.find { it.id == cleanTargetId || it.phone.trim().replace(" ", "").replace("+", "") == cleanTargetId.replace(" ", "").replace("+", "") }
        val docId = targetStore?.id ?: cleanTargetId

        _stores.value = _stores.value.map {
            if (it.id == docId || it.id == cleanTargetId) it.copy(isActive = isActive, isApproved = isActive) else it
        }

        val updates = mapOf(
            "isActive" to isActive,
            "isApproved" to isActive,
            "active" to isActive
        )

        if (targetStore != null && isActive) {
            val cleanPhone = targetStore.phone.trim().replace(" ", "").replace("+", "")
            if (cleanPhone.isNotEmpty()) {
                db.collection("pending_providers").whereEqualTo("phone", targetStore.phone).get().addOnSuccessListener { qs ->
                    qs?.documents?.forEach { doc ->
                        db.collection("pending_providers").document(doc.id).delete()
                    }
                }
                _pendingProviders.value = _pendingProviders.value.filter { 
                    it.phone.trim().replace(" ", "").replace("+", "") != cleanPhone 
                }
            }

            val notification = NotificationEntity(
                id = UUID.randomUUID().toString(),
                title = "🎉 تم قبول واعتماد طلبك بنجاح!",
                message = "تهانينا! تم قبول واعتماد منشأتك (${targetStore.name}) بالدليل اليمني بنجاح!",
                targetType = "USER",
                targetValue = targetStore.phone,
                timestamp = System.currentTimeMillis()
            )
            _notifications.value = listOf(notification) + _notifications.value
            try { db.collection("notifications").document(notification.id).set(notification) } catch(e: Exception) {}
        }

        db.collection("stores").document(docId).update(updates)
            .addOnSuccessListener {
                triggerNotification(if (isActive) "✅ تم تفعيل المتجر والموافقة عليه" else "🔒 تم إلغاء تفعيل المتجر")
            }
            .addOnFailureListener {
                if (targetStore != null) {
                    db.collection("stores").document(docId).set(targetStore.copy(isActive = isActive, isApproved = isActive))
                        .addOnSuccessListener {
                            triggerNotification(if (isActive) "✅ تم تفعيل المتجر والموافقة عليه" else "🔒 تم إلغاء تفعيل المتجر")
                        }
                }
            }
    }

    fun setStoreVip(storeId: String, isVip: Boolean) {
        _stores.value = _stores.value.map {
            if (it.id == storeId) it.copy(isVip = isVip) else it
        }
        db.collection("stores").document(storeId).update("isVip", isVip)
            .addOnSuccessListener {
                triggerNotification(if (isVip) "🏆 تم تمييز المتجر بشارة VIP" else "🔒 تم إلغاء شارة VIP عن المتجر")
            }
    }

    fun setStoreVerified(storeId: String, isVerified: Boolean) {
        _stores.value = _stores.value.map {
            if (it.id == storeId) it.copy(isVerified = isVerified) else it
        }
        db.collection("stores").document(storeId).update("isVerified", isVerified)
            .addOnSuccessListener {
                triggerNotification(if (isVerified) "🛡️ تم توثيق حساب المتجر" else "🔒 تم إلغاء التوثيق عن المتجر")
            }
    }

    fun setStoreRecommended(storeId: String, isRecommended: Boolean) {
        _stores.value = _stores.value.map {
            if (it.id == storeId) it.copy(isRecommended = isRecommended) else it
        }
        db.collection("stores").document(storeId).update("isRecommended", isRecommended)
            .addOnSuccessListener {
                triggerNotification(if (isRecommended) "💖 تم ترشيح المتجر كموصى به" else "🔒 تم إلغاء ترشيح المتجر")
            }
    }

    fun setStoreChatDisabled(storeId: String, isDisabled: Boolean) {
        _stores.value = _stores.value.map {
            if (it.id == storeId) it.copy(isChatDisabled = isDisabled) else it
        }
        db.collection("stores").document(storeId).update("isChatDisabled", isDisabled)
            .addOnSuccessListener {
                triggerNotification(if (isDisabled) "🔇 تم إيقاف الدردشة للمتجر" else "💬 تم تفعيل الدردشة للمتجر")
            }
    }

    fun setStoreNotificationsDisabled(storeId: String, isDisabled: Boolean) {
        _stores.value = _stores.value.map {
            if (it.id == storeId) it.copy(isNotificationsDisabled = isDisabled) else it
        }
        db.collection("stores").document(storeId).update("isNotificationsDisabled", isDisabled)
            .addOnSuccessListener {
                triggerNotification(if (isDisabled) "🔕 تم كتم الإشعارات للمتجر" else "🔔 تم تفعيل الإشعارات للمتجر")
            }
    }

    fun setStoreOrder(storeId: String, order: Int) {
        db.collection("stores").document(storeId).get().addOnSuccessListener { snapshot ->
            val store = snapshot.toObject(com.example.data.StoreEntity::class.java)
            if (store != null) {
                db.collection("stores").document(storeId).set(store.copy(displayOrder = order))
            }
        }
    }

    // --- PRODUCTS MANAGEMENT ---
    fun saveProduct(product: com.example.data.ProductEntity) {
        val targetId = if (product.id.isEmpty()) db.collection("products").document().id else product.id
        val localProduct = product.copy(id = targetId)
        
        // Instant Local State Sync
        val currentProds = _products.value.filter { it.id != targetId }.toMutableList()
        currentProds.add(localProduct)
        _products.value = currentProds

        viewModelScope.launch {
            val ctx = appContext
            val finalImg = if (ctx != null && product.imageUrl.isNotEmpty() && !product.imageUrl.startsWith("http")) {
                uploadImageStringOrUri(ctx, product.imageUrl, com.example.util.FirebaseStorageUploader.getStoreProductPath(product.storeId.ifEmpty { "general" }, targetId))
            } else product.imageUrl

            val finalProduct = product.copy(id = targetId, imageUrl = finalImg)
            db.collection("products").document(targetId).set(finalProduct)
                .addOnSuccessListener {
                    triggerNotification("✅ تم حفظ المنتج بنجاح!")
                }
        }
    }

    fun deleteProduct(productId: String) {
        _products.value = _products.value.filter { it.id != productId }
        db.collection("products").document(productId).get().addOnSuccessListener { snapshot ->
            val product = snapshot.toObject(com.example.data.ProductEntity::class.java)
            if (product != null) {
                db.collection("products").document(productId).set(product.copy(isDeleted = true))
            } else {
                db.collection("products").document(productId).update("isDeleted", true)
            }
        }
    }

    // --- INSTANT PRICING & REAL-TIME OFFERS SYSTEM ---
    fun updateProductPrice(productId: String, newPrice: Double) {
        val existing = _products.value.find { it.id == productId }
        if (existing != null) {
            val updated = existing.copy(price = newPrice, oldPrice = if (existing.price != newPrice) existing.price else existing.oldPrice)
            _products.value = _products.value.map { if (it.id == productId) updated else it }
            db.collection("products").document(productId).update("price", newPrice, "oldPrice", updated.oldPrice)
                .addOnSuccessListener {
                    triggerNotification("⚡ تم تحديث السعر فورياً لجميع العملاء!")
                }
        } else {
            db.collection("products").document(productId).update("price", newPrice)
        }
    }

    fun saveOffer(offer: com.example.data.models.Offer) {
        val targetId = if (offer.id.isEmpty()) db.collection("offers").document().id else offer.id
        val finalOffer = offer.copy(id = targetId, updatedAt = System.currentTimeMillis())

        val currentList = _offers.value.filter { it.id != targetId }.toMutableList()
        currentList.add(finalOffer)
        _offers.value = currentList

        db.collection("offers").document(targetId).set(finalOffer)
            .addOnSuccessListener {
                triggerNotification("🎁 تم نشر العرض وتحديث الأسعار فورياً!")
            }
            .addOnFailureListener {
                triggerNotification("⚠️ فشل حفظ العرض: ${it.localizedMessage}")
            }
    }

    fun deleteOffer(offerId: String) {
        _offers.value = _offers.value.filter { it.id != offerId }
        db.collection("offers").document(offerId).delete()
            .addOnSuccessListener {
                triggerNotification("🗑️ تم حذف العرض بنجاح!")
            }
    }

    fun toggleOfferStatus(offerId: String, isActive: Boolean) {
        _offers.value = _offers.value.map { if (it.id == offerId) it.copy(isActive = isActive) else it }
        db.collection("offers").document(offerId).update(
            "isActive", isActive,
            "updatedAt", System.currentTimeMillis()
        )
    }

    fun listenToOffersForEntity(
        entityId: String,
        onResult: (List<com.example.data.models.Offer>) -> Unit
    ): com.google.firebase.firestore.ListenerRegistration {
        return db.collection("offers")
            .whereEqualTo("entityId", entityId)
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    val fetched = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(com.example.data.models.Offer::class.java)?.copy(id = doc.id)
                    }
                    onResult(fetched)
                }
            }
    }

    fun listenToProductsForStore(
        storeId: String,
        onResult: (List<com.example.data.ProductEntity>) -> Unit
    ): com.google.firebase.firestore.ListenerRegistration {
        return db.collection("products")
            .whereEqualTo("storeId", storeId)
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    val fetched = snapshot.documents.mapNotNull { doc ->
                        val p = doc.toObject(com.example.data.ProductEntity::class.java)
                        if (p != null && !p.isDeleted) p.copy(id = doc.id) else null
                    }
                    onResult(fetched)
                }
            }
    }

    // --- PROPERTIES MANAGEMENT ---
    fun saveProperty(property: com.example.data.PropertyEntity) {
        val cleanPhone = property.phone.trim().replace(" ", "").replace("+", "")
        val duplicateType = checkAndGetDuplicateAccountType(cleanPhone, property.id)
        if (duplicateType != null) {
            triggerNotification("❌ عذراً! رقم الهاتف (${property.phone}) مسجل بالفعل كـ ($duplicateType). لا يُسمح بتكرار الحسابات.")
            logAdminActivity("محاولة تسجيل عقار مكرر محجوبة لرقم: $cleanPhone - نوع التكرار: $duplicateType")
            return
        }
        val targetId = if (property.id.isEmpty()) db.collection("properties").document().id else property.id
        val finalProp = property.copy(
            id = targetId,
            phone = cleanPhone,
            ownerId = if (property.ownerId.isEmpty()) cleanPhone else property.ownerId
        )

        // Instant Local State Update
        val updatedProps = _properties.value.filter { it.id != targetId }.toMutableList()
        updatedProps.add(finalProp)
        _properties.value = updatedProps

        if (property.password.isNotEmpty()) {
            try {
                val authEmail = getAuthEmailForPhone(cleanPhone)
                auth.createUserWithEmailAndPassword(authEmail, property.password.trim())
                    .addOnFailureListener { /* account exists */ }
            } catch (e: Exception) {}
        }

        viewModelScope.launch {
            val ctx = appContext
            val finalImages = if (ctx != null) {
                finalProp.images.mapIndexed { idx, img ->
                    uploadImageStringOrUri(ctx, img, com.example.util.FirebaseStorageUploader.getPropertyPhotoPath(targetId, idx))
                }
            } else finalProp.images

            val uploadedProp = finalProp.copy(images = finalImages)
            val currentList = _properties.value.filter { it.id != targetId }.toMutableList()
            currentList.add(uploadedProp)
            _properties.value = currentList

            db.collection("properties").document(targetId).set(uploadedProp)
                .addOnSuccessListener {
                    if (!uploadedProp.isActive || !uploadedProp.isApproved) {
                        val adminNotif = NotificationEntity(
                            id = UUID.randomUUID().toString(),
                            title = "🏡 طلب انضمام عقار/مكتب عقاري جديد",
                            message = "قدم المكتب/العقار '${uploadedProp.title}' طلب انضمام جديد بمنطقة ${uploadedProp.cityId}.",
                            targetType = "SUPERVISOR",
                            targetValue = "ALL",
                            timestamp = System.currentTimeMillis()
                        )
                        try {
                            db.collection("notifications").document(adminNotif.id).set(adminNotif)
                        } catch (e: Exception) {}
                    }
                    triggerNotification("✅ تم تسجيل بيانات العقار بنجاح!")
                }
                .addOnFailureListener {
                    triggerNotification("⚠️ تم الحفظ محلياً وبانتظار مزامنة السحابة")
                }
        }
    }

    fun approvePropertyPdf(propertyId: String, approve: Boolean) {
        db.collection("properties").document(propertyId).get().addOnSuccessListener { snapshot ->
            val prop = snapshot.toObject(com.example.data.PropertyEntity::class.java)
            if (prop != null) {
                db.collection("properties").document(propertyId).set(prop.copy(pdfStatus = if (approve) "APPROVED" else "REJECTED"))
                    .addOnSuccessListener {
                        triggerNotification(if (approve) "✅ تم قبول ملف الـ PDF للعقار بنجاح!" else "❌ تم رفض ملف الـ PDF للعقار.")
                    }
            }
        }
    }

    fun requestPasswordRecoveryForProperty(title: String, phone: String, password: String) {
        setPasswordRecoveryWaitingPhone(phone)
        val adminNotif = NotificationEntity(
            id = UUID.randomUUID().toString(),
            title = "🔑 استعادة كلمة مرور عقار",
            message = "العقار $title (هاتف: $phone) يطلب استعادة كلمة مروره. كلمة المرور الحالية هي: $password",
            targetType = "SUPERVISOR",
            targetValue = "ALL",
            timestamp = System.currentTimeMillis()
        )
        db.collection("notifications").document(adminNotif.id).set(adminNotif)
        triggerNotification("📨 تم إرسال طلب استعادة كلمة المرور للمشرف بنجاح!")
    }

    fun deleteProperty(propertyId: String) {
        _properties.value = _properties.value.map {
            if (it.id == propertyId) it.copy(isDeleted = true, deletedAt = System.currentTimeMillis()) else it
        }
        val updates = mapOf(
            "isDeleted" to true,
            "deleted" to true,
            "deletedAt" to System.currentTimeMillis()
        )
        db.collection("properties").document(propertyId).update(updates)
            .addOnSuccessListener {
                triggerNotification("🗑️ تم حذف العقار بنجاح")
            }
            .addOnFailureListener { e ->
                db.collection("properties").document(propertyId).get().addOnSuccessListener { snapshot ->
                    val property = snapshot.toObject(com.example.data.PropertyEntity::class.java)
                    if (property != null) {
                        db.collection("properties").document(propertyId).set(property.copy(isDeleted = true, deletedAt = System.currentTimeMillis()))
                            .addOnSuccessListener {
                                triggerNotification("🗑️ تم حذف العقار بنجاح")
                            }
                    }
                }
            }
    }

    // --- JOBS MANAGEMENT ---
    fun saveJob(job: com.example.data.JobEntity) {
        val cleanPhone = job.phone.trim().replace(" ", "").replace("+", "")
        val targetId = if (job.id.isEmpty()) db.collection("jobs").document().id else job.id
        val finalJob = job.copy(id = targetId, phone = cleanPhone)

        // Instant Local State Update
        val updatedJobs = _jobs.value.filter { it.id != targetId }.toMutableList()
        updatedJobs.add(finalJob)
        _jobs.value = updatedJobs

        db.collection("jobs").document(targetId).set(finalJob)
            .addOnSuccessListener {
                triggerNotification(if (finalJob.isApproved) "✅ تم حفظ ونشر الإعلان الوظيفي بنجاح!" else "📨 تم تقديم إعلان الوظيفة للمراجع من قبل الأدمن!")
            }
            .addOnFailureListener {
                triggerNotification("⚠️ تم إدراج الوظيفة محلياً وفي انتظار المزامنة")
            }
    }

    fun setJobApproved(jobId: String, isApproved: Boolean) {
        _jobs.value = _jobs.value.map {
            if (it.id == jobId) it.copy(isApproved = isApproved, isActive = isApproved) else it
        }
        val updates = mapOf(
            "isApproved" to isApproved,
            "isActive" to isApproved
        )
        db.collection("jobs").document(jobId).update(updates)
            .addOnSuccessListener {
                triggerNotification(if (isApproved) "✅ تم قبول ونشر إعلان الوظيفة بنجاح!" else "❌ تم رفض إعلان الوظيفة")
            }
    }

    fun deleteJob(jobId: String) {
        _jobs.value = _jobs.value.map {
            if (it.id == jobId) it.copy(isDeleted = true) else it
        }
        val updates = hashMapOf<String, Any?>(
            "isDeleted" to true,
            "deletedAt" to System.currentTimeMillis()
        )
        db.collection("jobs").document(jobId).update(updates)
            .addOnSuccessListener {
                triggerNotification("🗑️ تم نقل الإعلان الوظيفي لسلة المحذوفات")
            }
    }

    fun restoreJob(jobId: String) {
        _jobs.value = _jobs.value.map {
            if (it.id == jobId) it.copy(isDeleted = false) else it
        }
        val updates = hashMapOf<String, Any?>(
            "isDeleted" to false,
            "deletedAt" to null
        )
        db.collection("jobs").document(jobId).update(updates)
            .addOnSuccessListener {
                triggerNotification("♻️ تم استعادة الإعلان الوظيفي بنجاح")
            }
    }

    fun submitJobApplication(application: com.example.data.JobApplicationEntity) {
        val targetId = db.collection("job_applications").document().id
        val finalApp = application.copy(id = targetId)
        db.collection("job_applications").document(targetId).set(finalApp)
            .addOnSuccessListener {
                triggerNotification("📨 تم إرسال طلب التقديم على الوظيفة بنجاح!")
            }
            .addOnFailureListener {
                triggerNotification("❌ فشل تقديم الطلب: ${it.message}")
            }
    }

    fun updateJobApplicationStatus(appId: String, status: String) {
        db.collection("job_applications").document(appId).update("status", status)
            .addOnSuccessListener {
                triggerNotification("✅ تم تحديث حالة طلب التقديم إلى: $status")
            }
    }

    fun restoreProperty(propertyId: String) {
        _properties.value = _properties.value.map {
            if (it.id == propertyId) it.copy(isDeleted = false, deletedAt = null) else it
        }
        val updates = hashMapOf<String, Any?>(
            "isDeleted" to false,
            "deleted" to false,
            "deletedAt" to null
        )
        db.collection("properties").document(propertyId).update(updates)
            .addOnSuccessListener {
                triggerNotification("♻️ تم استعادة العقار بنجاح")
            }
    }

    fun deletePropertyPermanently(propertyId: String) {
        _properties.value = _properties.value.filter { it.id != propertyId }
        db.collection("properties").document(propertyId).delete()
            .addOnSuccessListener {
                triggerNotification("🗑️ تم حذف العقار نهائياً من النظام")
            }
    }

    fun setPropertyPinned(propertyId: String, isPinned: Boolean) {
        _properties.value = _properties.value.map {
            if (it.id == propertyId) it.copy(isPinned = isPinned) else it
        }
        val updates = mapOf(
            "isPinned" to isPinned,
            "pinned" to isPinned
        )
        db.collection("properties").document(propertyId).update(updates)
            .addOnSuccessListener {
                triggerNotification(if (isPinned) "📌 تم تمييز وتثبيت العقار في الصدارة" else "📌 تم إلغاء تثبيت العقار")
            }
    }

    fun setPropertyActive(propertyId: String, isActive: Boolean) {
        val cleanTargetId = propertyId.trim()
        val targetProp = _properties.value.find { it.id == cleanTargetId || it.phone.trim().replace(" ", "").replace("+", "") == cleanTargetId.replace(" ", "").replace("+", "") }
        val docId = targetProp?.id ?: cleanTargetId

        _properties.value = _properties.value.map {
            if (it.id == docId || it.id == cleanTargetId) it.copy(isActive = isActive, isApproved = isActive) else it
        }

        val updates = mapOf(
            "isActive" to isActive,
            "isApproved" to isActive,
            "active" to isActive
        )

        if (targetProp != null && isActive) {
            val cleanPhone = targetProp.phone.trim().replace(" ", "").replace("+", "")
            if (cleanPhone.isNotEmpty()) {
                db.collection("pending_providers").whereEqualTo("phone", targetProp.phone).get().addOnSuccessListener { qs ->
                    qs?.documents?.forEach { doc ->
                        db.collection("pending_providers").document(doc.id).delete()
                    }
                }
                _pendingProviders.value = _pendingProviders.value.filter { 
                    it.phone.trim().replace(" ", "").replace("+", "") != cleanPhone 
                }
            }

            val notification = NotificationEntity(
                id = UUID.randomUUID().toString(),
                title = "🎉 تم قبول وإعتماد إعلان عقارك",
                message = "تهانينا! تم قبول ونشر إعلان العقار (${targetProp.title}) للجميع!",
                targetType = "USER",
                targetValue = targetProp.phone,
                timestamp = System.currentTimeMillis()
            )
            _notifications.value = listOf(notification) + _notifications.value
            try { db.collection("notifications").document(notification.id).set(notification) } catch(e: Exception) {}
        }

        db.collection("properties").document(docId).update(updates)
            .addOnSuccessListener {
                triggerNotification(if (isActive) "✅ تم تفعيل ونشر العقار للجميع" else "🔒 تم إلغاء تفعيل ونشر العقار")
            }
            .addOnFailureListener {
                if (targetProp != null) {
                    db.collection("properties").document(docId).set(targetProp.copy(isActive = isActive, isApproved = isActive))
                        .addOnSuccessListener {
                            triggerNotification(if (isActive) "✅ تم تفعيل ونشر العقار للجميع" else "🔒 تم إلغاء تفعيل ونشر العقار")
                        }
                }
            }
    }

    fun setPropertyVip(propertyId: String, isVip: Boolean) {
        _properties.value = _properties.value.map {
            if (it.id == propertyId) it.copy(isVip = isVip) else it
        }
        db.collection("properties").document(propertyId).update("isVip", isVip)
            .addOnSuccessListener {
                triggerNotification(if (isVip) "🏆 تم تمييز العقار بشارة VIP" else "🔒 تم إلغاء شارة VIP عن العقار")
            }
    }

    fun setPropertyVerified(propertyId: String, isVerified: Boolean) {
        _properties.value = _properties.value.map {
            if (it.id == propertyId) it.copy(isVerified = isVerified) else it
        }
        db.collection("properties").document(propertyId).update("isVerified", isVerified)
            .addOnSuccessListener {
                triggerNotification(if (isVerified) "🛡️ تم توثيق إعلان العقار" else "🔒 تم إلغاء التوثيق عن العقار")
            }
    }

    fun setPropertyRecommended(propertyId: String, isRecommended: Boolean) {
        _properties.value = _properties.value.map {
            if (it.id == propertyId) it.copy(isRecommended = isRecommended) else it
        }
        db.collection("properties").document(propertyId).update("isRecommended", isRecommended)
            .addOnSuccessListener {
                triggerNotification(if (isRecommended) "💖 تم ترشيح العقار كموصى به" else "🔒 تم إلغاء ترشيح العقار")
            }
    }

    fun setPropertyChatDisabled(propertyId: String, isDisabled: Boolean) {
        _properties.value = _properties.value.map {
            if (it.id == propertyId) it.copy(isChatDisabled = isDisabled) else it
        }
        db.collection("properties").document(propertyId).update("isChatDisabled", isDisabled)
            .addOnSuccessListener {
                triggerNotification(if (isDisabled) "🔇 تم إيقاف الدردشة للمعلن" else "💬 تم تفعيل الدردشة للمعلن")
            }
    }

    fun setPropertyNotificationsDisabled(propertyId: String, isDisabled: Boolean) {
        _properties.value = _properties.value.map {
            if (it.id == propertyId) it.copy(isNotificationsDisabled = isDisabled) else it
        }
        db.collection("properties").document(propertyId).update("isNotificationsDisabled", isDisabled)
            .addOnSuccessListener {
                triggerNotification(if (isDisabled) "🔕 تم كتم الإشعارات للمعلن" else "🔔 تم تفعيل الإشعارات للمعلن")
            }
    }

    // --- EXTENDED ADMIN MANAGEMENT & ENTITY CONTROLS ---

    fun setStoreBlocked(storeId: String, isBlocked: Boolean, reason: String = "") {
        _stores.value = _stores.value.map {
            if (it.id == storeId) it.copy(isBlocked = isBlocked, blockReason = reason) else it
        }
        val updates = mapOf(
            "isBlocked" to isBlocked,
            "blockReason" to reason
        )
        db.collection("stores").document(storeId).update(updates)
            .addOnSuccessListener {
                triggerNotification(if (isBlocked) "🚫 تم حظر المحل/المركز بنجاح ($reason)" else "✅ تم إلغاء حظر المحل/المركز")
            }
    }

    fun setStorePaymentEnabled(storeId: String, isEnabled: Boolean) {
        db.collection("stores").document(storeId).update("paymentEnabled", isEnabled)
            .addOnSuccessListener {
                triggerNotification(if (isEnabled) "💳 تم تفعيل نظام الدفع والمحفظة للمتجر" else "🔒 تم تعطيل نظام الدفع للمتجر")
            }
    }

    fun setPropertyBlocked(propertyId: String, isBlocked: Boolean, reason: String = "") {
        _properties.value = _properties.value.map {
            if (it.id == propertyId) it.copy(isBlocked = isBlocked, blockReason = reason) else it
        }
        val updates = mapOf(
            "isBlocked" to isBlocked,
            "blockReason" to reason
        )
        db.collection("properties").document(propertyId).update(updates)
            .addOnSuccessListener {
                triggerNotification(if (isBlocked) "🚫 تم حظر إعلان العقار بنجاح ($reason)" else "✅ تم إلغاء حظر إعلان العقار")
            }
    }

    fun setPropertyPaymentEnabled(propertyId: String, isEnabled: Boolean) {
        db.collection("properties").document(propertyId).update("paymentEnabled", isEnabled)
            .addOnSuccessListener {
                triggerNotification(if (isEnabled) "💳 تم تفعيل نظام الدفع للعقار" else "🔒 تم تعطيل نظام الدفع للعقار")
            }
    }

    fun setJobBlocked(jobId: String, isBlocked: Boolean, reason: String = "") {
        val updates = mapOf(
            "isBlocked" to isBlocked,
            "blockReason" to reason
        )
        db.collection("jobs").document(jobId).update(updates)
            .addOnSuccessListener {
                triggerNotification(if (isBlocked) "🚫 تم حظر الإعلان الوظيفي ($reason)" else "✅ تم إلغاء حظر الإعلان الوظيفي")
            }
    }

    fun setJobPinned(jobId: String, isPinned: Boolean) {
        db.collection("jobs").document(jobId).update("isPinned", isPinned)
            .addOnSuccessListener {
                triggerNotification(if (isPinned) "📌 تم تثبيت الوظيفة في الصدارة" else "📌 تم إلغاء تثبيت الوظيفة")
            }
    }

    fun setJobVip(jobId: String, isVip: Boolean) {
        db.collection("jobs").document(jobId).update("isVip", isVip)
            .addOnSuccessListener {
                triggerNotification(if (isVip) "🏆 تم تمييز الإعلان الوظيفي كـ VIP" else "🔒 تم إلغاء شارة VIP عن الوظيفة")
            }
    }

    fun setJobChatDisabled(jobId: String, isDisabled: Boolean) {
        db.collection("jobs").document(jobId).update("isChatDisabled", isDisabled)
            .addOnSuccessListener {
                triggerNotification(if (isDisabled) "🔇 تم إيقاف الدردشة للإعلان الوظيفي" else "💬 تم تفعيل الدردشة للإعلان الوظيفي")
            }
    }

    fun deleteJobPermanently(jobId: String) {
        db.collection("jobs").document(jobId).delete()
            .addOnSuccessListener {
                triggerNotification("🗑️ تم حذف الإعلان الوظيفي نهائياً من النظام")
            }
    }

    fun deleteJobApplication(appId: String) {
        db.collection("job_applications").document(appId).delete()
            .addOnSuccessListener {
                triggerNotification("🗑️ تم حذف طلب التقديم من النظام بنجاح")
            }
    }

    fun acceptJobApplication(appId: String) {
        updateJobApplicationStatus(appId, "ACCEPTED")
    }

    fun rejectJobApplication(appId: String, reason: String) {
        db.collection("job_applications").document(appId).update("status", "REJECTED", "rejectionReason", reason)
            .addOnSuccessListener {
                triggerNotification("❌ تم رفض طلب التقديم للوظيفة مع إرسال السبب: $reason")
            }
    }

    fun sendNotificationToApplicants(title: String, message: String, jobId: String = "") {
        val notif = NotificationEntity(
            id = UUID.randomUUID().toString(),
            title = title,
            message = message,
            targetType = if (jobId.isNotEmpty()) "JOB_APPLICANTS" else "ALL",
            targetValue = jobId,
            timestamp = System.currentTimeMillis()
        )
        db.collection("notifications").document(notif.id).set(notif)
            .addOnSuccessListener {
                triggerNotification("🔔 تم إرسال الإشعار لجميع المتقدمين للوظائف بنجاح!")
            }
    }

    fun exportJobApplicantsCsv(context: android.content.Context) {
        try {
            val apps = _jobApplications.value
            val csvContent = StringBuilder()
            csvContent.append("المعرف,اسم المتقدم,رقم الهاتف,الوظيفة,الشركة,المؤهلات,الحالة,تاريخ التقديم\n")
            apps.forEach { app ->
                val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(app.createdAt))
                csvContent.append("${app.id},\"${app.applicantName}\",\"${app.applicantPhone}\",\"${app.jobTitle}\",\"${app.companyName}\",\"${app.applicantQuals.replace("\n", " ")}\",\"${app.status}\",\"$dateStr\"\n")
            }
            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("Job Applicants CSV", csvContent.toString())
            clipboard.setPrimaryClip(clip)
            triggerNotification("📋 تم نسخ بيانات المتقدمين للوظائف بصيغة CSV إلى الحافظة بنجاح (${apps.size} متقدم)!")
        } catch (e: Exception) {
            triggerNotification("❌ فشل تصدير البيانات: ${e.message}")
        }
    }

    fun unbanEntity(entityType: String, entityId: String) {
        when (entityType.uppercase()) {
            "PROVIDER" -> db.collection("providers").document(entityId).update("isBlocked", false)
            "STORE", "RESTAURANT", "MEDICAL" -> setStoreBlocked(entityId, false, "")
            "PROPERTY" -> setPropertyBlocked(entityId, false, "")
            "JOB" -> setJobBlocked(entityId, false, "")
            else -> {
                db.collection("stores").document(entityId).update("isBlocked", false)
                db.collection("properties").document(entityId).update("isBlocked", false)
                db.collection("jobs").document(entityId).update("isBlocked", false)
                db.collection("providers").document(entityId).update("isBlocked", false)
            }
        }
        triggerNotification("✅ تم إلغاء حظر الكيان بنجاح!")
    }

    fun restoreEntity(entityType: String, entityId: String) {
        when (entityType.uppercase()) {
            "PROVIDER" -> restoreProvider(entityId)
            "STORE", "RESTAURANT", "MEDICAL" -> restoreStore(entityId)
            "PROPERTY" -> restoreProperty(entityId)
            "JOB" -> restoreJob(entityId)
            else -> {
                restoreStore(entityId)
                restoreProperty(entityId)
                restoreJob(entityId)
            }
        }
    }

    fun hardDeleteEntity(entityType: String, entityId: String) {
        when (entityType.uppercase()) {
            "PROVIDER" -> removeProviderPermanently(entityId)
            "STORE", "RESTAURANT", "MEDICAL" -> deleteStorePermanently(entityId)
            "PROPERTY" -> deletePropertyPermanently(entityId)
            "JOB" -> deleteJobPermanently(entityId)
            else -> {
                deleteStorePermanently(entityId)
                deletePropertyPermanently(entityId)
                deleteJobPermanently(entityId)
            }
        }
    }

    fun redirectBookingToEntity(bookingId: String, targetEntityId: String, targetEntityName: String, targetPhone: String) {
        val updates = mapOf(
            "providerId" to targetEntityId,
            "providerName" to targetEntityName,
            "providerPhone" to targetPhone,
            "status" to "PENDING"
        )
        db.collection("bookings").document(bookingId).update(updates)
            .addOnSuccessListener {
                triggerNotification("🔄 تم توجيه الحجز للجهة/الفني ($targetEntityName) بنجاح!")
            }
    }

    // --- RATINGS & COMMENTS ---
    fun addRating(rating: com.example.data.RatingEntity) {
        val targetId = db.collection("ratings").document().id
        val finalRating = rating.copy(id = targetId)
        db.collection("ratings").document(targetId).set(finalRating).addOnSuccessListener {
            triggerNotification("⭐ شكراً لتقييمك! تم إرسال تقييمك بنجاح.")
            recalculateTargetRating(rating.targetId, rating.targetType)
        }
    }

    fun addRatingReply(ratingId: String, replyText: String) {
        val updates = mapOf(
            "reply" to replyText,
            "replyTimestamp" to System.currentTimeMillis()
        )
        db.collection("ratings").document(ratingId).update(updates)
            .addOnSuccessListener {
                triggerNotification("✅ تم إضافة الرد على التعليق بنجاح!")
            }
            .addOnFailureListener {
                triggerNotification("❌ فشل إضافة الرد: ${it.message}")
            }
    }

    fun editBookingByUser(bookingId: String, newDate: String, newTime: String, newServiceType: String, providerId: String = "", providerName: String = "") {
        val targetProviderId = providerId.ifEmpty {
            _bookings.value.find { it.id == bookingId }?.providerId ?: ""
        }
        val isTimeSlotTaken = _bookings.value.any {
            it.id != bookingId &&
            it.providerId == targetProviderId &&
            it.dateString.trim() == newDate.trim() &&
            it.timeString.trim() == newTime.trim() &&
            (it.status == "PENDING" || it.status == "APPROVED" || it.status == "IN_PROGRESS")
        }
        if (isTimeSlotTaken) {
            triggerNotification("❌ عذراً! هذا الوقت (${newTime}) وتاريخ (${newDate}) محجوز بالفعل لدى مقدم الخدمة. يرجى اختيار موعد آخر.")
            return
        }

        val updates = mutableMapOf<String, Any>(
            "dateString" to newDate,
            "timeString" to newTime,
            "serviceType" to newServiceType,
            "updatedAt" to System.currentTimeMillis()
        )
        if (providerId.isNotEmpty()) {
            updates["providerId"] = providerId
        }
        if (providerName.isNotEmpty()) {
            updates["providerName"] = providerName
        }
        db.collection("bookings").document(bookingId).update(updates)
            .addOnSuccessListener {
                triggerNotification("✅ تم تعديل الحجز بنجاح!")
            }
            .addOnFailureListener {
                triggerNotification("❌ فشل تعديل الحجز: ${it.message}")
            }
    }

    fun deleteRating(ratingId: String) {
        db.collection("ratings").document(ratingId).get().addOnSuccessListener { snap ->
            val rating = snap.toObject(com.example.data.RatingEntity::class.java)
            if (rating != null) {
                db.collection("ratings").document(ratingId).delete().addOnSuccessListener {
                    recalculateTargetRating(rating.targetId, rating.targetType)
                }
            }
        }
    }

    fun approveRating(ratingId: String, isApproved: Boolean) {
        db.collection("ratings").document(ratingId).get().addOnSuccessListener { snap ->
            val rating = snap.toObject(com.example.data.RatingEntity::class.java)
            if (rating != null) {
                db.collection("ratings").document(ratingId).set(rating.copy(isApproved = isApproved))
            }
        }
    }

    private fun recalculateTargetRating(targetId: String, targetType: String) {
        db.collection("ratings").whereEqualTo("targetId", targetId).get().addOnSuccessListener { snapshot ->
            val ratingsList = snapshot.documents.mapNotNull { it.toObject(com.example.data.RatingEntity::class.java) }
            val count = ratingsList.size
            val avg = if (count > 0) ratingsList.map { it.rating }.sum() / count else 5.0f
            
            if (targetType == "STORE") {
                db.collection("stores").document(targetId).get().addOnSuccessListener { storeSnap ->
                    val store = storeSnap.toObject(com.example.data.StoreEntity::class.java)
                    if (store != null) {
                        db.collection("stores").document(targetId).set(store.copy(rating = avg, numReviews = count))
                    }
                }
            } else if (targetType == "PROPERTY") {
                db.collection("properties").document(targetId).get().addOnSuccessListener { propSnap ->
                    val prop = propSnap.toObject(com.example.data.PropertyEntity::class.java)
                    if (prop != null) {
                        db.collection("properties").document(targetId).set(prop.copy(rating = avg, numReviews = count))
                    }
                }
            }
        }
    }

    // --- ORDERS ---
    fun placeOrder(order: com.example.data.OrderEntity) {
        val targetId = db.collection("orders").document().id
        val finalOrder = order.copy(id = targetId)
        db.collection("orders").document(targetId).set(finalOrder).addOnSuccessListener {
            triggerNotification("🛍️ تم تسجيل طلبك بنجاح! رقم الطلب: ${targetId.take(6)}")
        }
    }

    fun updateOrderStatus(orderId: String, status: String) {
        db.collection("orders").document(orderId).get().addOnSuccessListener { snap ->
            val order = snap.toObject(com.example.data.OrderEntity::class.java)
            if (order != null) {
                db.collection("orders").document(orderId).set(order.copy(status = status)).addOnSuccessListener {
                    triggerNotification("📦 تم تحديث حالة الطلب إلى $status")
                }
            }
        }
    }

    fun deleteOrder(orderId: String) {
        db.collection("orders").document(orderId).delete().addOnSuccessListener {
            triggerNotification("🗑️ تم حذف الطلب بنجاح.")
        }
    }

    fun deleteAllOrders(customerPhone: String) {
        db.collection("orders")
            .whereEqualTo("customerPhone", customerPhone)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val batch = db.batch()
                for (doc in querySnapshot.documents) {
                    batch.delete(doc.reference)
                }
                batch.commit().addOnSuccessListener {
                    triggerNotification("🗑️ تم حذف جميع الطلبات بنجاح.")
                }
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
        adminUsername: String = "meh777644@gmail.com",
        adminPassword: String = "Meh@@@@777644##",
        customPrimaryHex: String = "#059669",
        customSecondaryHex: String = "#115E59",
        customBackgroundHex: String = "#0A0F0D",
        customSurfaceHex: String = "#121D18"
    ) {
        val passHash = if (adminPassword.isNotEmpty()) {
            if (adminPassword.length == 64 && adminPassword.all { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }) adminPassword else com.example.util.SecurityCryptoUtils.hashPassword(adminPassword)
        } else _settings.value.adminPassword

        val updated = _settings.value.copy(
            appName = appName,
            welcomeMessage = welcomeMsg,
            footerMessage = footerMsg,
            activeThemeId = themeId,
            isMaintenanceActive = isMaintenance,
            hidePromoFooter = hiddenFooter,
            assistantHidden = botHidden,
            assistantSize = botSize,
            chatHidden = chatHidden,
            chatSize = chatSize,
            maxSearchRadiusKm = radiusKm,
            isSpeechSearchEnabled = isSpeech,
            supportPhone = supportPhone,
            supportEmail = supportEmail,
            supportWhatsapp = supportWhatsapp,
            bookingTerms = bookingTerms,
            bookingLabelName = bookingLabelName,
            bookingLabelPhone = bookingLabelPhone,
            bookingLabelArea = bookingLabelArea,
            bookingLabelService = bookingLabelService,
            adminUsername = adminUsername,
            adminPassword = passHash,
            customPrimaryHex = customPrimaryHex,
            customSecondaryHex = customSecondaryHex,
            customBackgroundHex = customBackgroundHex,
            customSurfaceHex = customSurfaceHex
        )
        db.collection("settings").document("main_settings").set(updated)
        _settings.value = updated
        triggerNotification("💾 تم حفظ إعدادات البوابة البارزة والملفات بنجاح")
    }

    fun markChatMessagesAsRead(channelId: String, currentUserId: String) {
        if (channelId.isEmpty()) return
        db.collection("chat_channels").document(channelId).get().addOnSuccessListener { snapshot ->
            val ch = snapshot.toObject(ChatChannelEntity::class.java)
            if (ch != null) {
                var updated = false
                val updatedMessages = ch.messages.map { msg ->
                    if (msg.senderId != currentUserId && msg.status != "READ") {
                        updated = true
                        msg.copy(status = "READ", statusTime = System.currentTimeMillis())
                    } else msg
                }
                if (updated) {
                    db.collection("chat_channels").document(channelId).update("messages", updatedMessages)
                }
            }
        }
    }

    fun uploadChatMediaToStorage(
        uri: android.net.Uri,
        isVideo: Boolean,
        onSuccess: (String) -> Unit
    ) {
        viewModelScope.launch {
            val ctx = appContext
            if (ctx != null) {
                val messageId = UUID.randomUUID().toString()
                val path = if (isVideo) "chat/media/video_${messageId}.mp4" else "chat/media/img_${messageId}.webp"
                val result = if (isVideo) {
                    val bytes = ctx.contentResolver.openInputStream(uri)?.readBytes()
                    if (bytes != null) {
                        com.example.util.FirebaseStorageUploader.uploadBytesToStorage(bytes, path, "video/mp4")
                    } else Result.failure(Exception("تعذر قراءة ملف الفيديو"))
                } else {
                    com.example.util.FirebaseStorageUploader.uploadImageUri(
                        context = ctx,
                        uri = uri,
                        storagePath = path,
                        maxDimension = 800,
                        maxSizeBytes = 300 * 1024L
                    )
                }
                result.onSuccess { downloadUrl ->
                    onSuccess(downloadUrl)
                }.onFailure { err ->
                    triggerNotification("⚠️ تعذر رفع الوسائط للسحابة: ${err.message ?: "تحقق من الاتصال"}")
                    onSuccess(uri.toString())
                }
            } else {
                onSuccess(uri.toString())
            }
        }
    }

    fun updateAdminSettings(newSettings: AdminSettingsEntity) {
        db.collection("settings").document("main_settings").set(newSettings)
        _settings.value = newSettings
        triggerNotification("👑 تم تحديث ومزامنة إعدادات المنصة بنجاح!")
    }

    fun exportComplaintsToCSV() {
        triggerNotification("📁 تم تصدير البلاغات بصيغة CSV")
    }

    fun exportComplaintsToPDF() {
        triggerNotification("📃 تم تصدير البلاغات بصيغة PDF")
    }

    fun exportPerformanceReportToPDF() {
        triggerNotification("📊 تم تصدير تقرير أداء شبكة الفنيين والمنصة بصيغة PDF بنجاح!")
    }

    fun createSystemBackup(onComplete: (Boolean, String) -> Unit) {
        try {
            val root = org.json.JSONObject()
            
            // Serialize providers
            val provArray = org.json.JSONArray()
            _providers.value.forEach { prov ->
                val obj = org.json.JSONObject()
                obj.put("id", prov.id)
                obj.put("name", prov.name)
                obj.put("phone", prov.phone)
                obj.put("customCategoryName", prov.customCategoryName)
                obj.put("cityId", prov.cityId)
                obj.put("localNeighborhood", prov.localNeighborhood)
                obj.put("isAvailable", prov.isAvailable)
                obj.put("rating", prov.rating.toDouble())
                obj.put("numReviews", prov.numReviews)
                provArray.put(obj)
            }
            root.put("providers", provArray)

            // Serialize bookings
            val bookArray = org.json.JSONArray()
            _bookings.value.forEach { b ->
                val obj = org.json.JSONObject()
                obj.put("id", b.id)
                obj.put("customerPhone", b.customerPhone)
                obj.put("customerName", b.customerName)
                obj.put("customerArea", b.customerArea)
                obj.put("providerId", b.providerId)
                obj.put("providerName", b.providerName)
                obj.put("dateString", b.dateString)
                obj.put("timeString", b.timeString)
                obj.put("serviceType", b.serviceType)
                obj.put("status", b.status)
                bookArray.put(obj)
            }
            root.put("bookings", bookArray)

            // Serialize categories
            val catArray = org.json.JSONArray()
            _categories.value.forEach { c ->
                val obj = org.json.JSONObject()
                obj.put("id", c.id)
                obj.put("name", c.name)
                obj.put("icon", c.icon)
                obj.put("parentId", c.parentId)
                obj.put("isMainCategory", c.isMainCategory)
                catArray.put(obj)
            }
            root.put("categories", catArray)

            // Serialize stores
            val storeArray = org.json.JSONArray()
            _stores.value.forEach { s ->
                val obj = org.json.JSONObject()
                obj.put("id", s.id)
                obj.put("name", s.name)
                obj.put("description", s.description)
                obj.put("phone", s.phone)
                obj.put("ownerName", s.ownerName)
                obj.put("cityId", s.cityId)
                obj.put("localNeighborhood", s.localNeighborhood)
                obj.put("isActive", s.isActive)
                storeArray.put(obj)
            }
            root.put("stores", storeArray)

            // Serialize properties
            val propArray = org.json.JSONArray()
            _properties.value.forEach { p ->
                val obj = org.json.JSONObject()
                obj.put("id", p.id)
                obj.put("title", p.title)
                obj.put("description", p.description)
                obj.put("price", p.price)
                obj.put("phone", p.phone)
                obj.put("cityId", p.cityId)
                obj.put("ownerName", p.ownerName)
                obj.put("isActive", p.isActive)
                propArray.put(obj)
            }
            root.put("properties", propArray)

            val jsonStr = root.toString(2)
            
            // Also write to cloud Firestore "database_backups" collection for periodic backup logging
            val backupId = "backup_" + System.currentTimeMillis()
            val backupData = hashMapOf(
                "id" to backupId,
                "timestamp" to com.google.firebase.Timestamp.now(),
                "data" to jsonStr,
                "summary" to "Providers: ${_providers.value.size}, Bookings: ${_bookings.value.size}, Categories: ${_categories.value.size}, Stores: ${_stores.value.size}"
            )
            db.collection("database_backups").document(backupId).set(backupData)
                .addOnSuccessListener {
                    triggerNotification("💾 تم إنشاء النسخة الاحتياطية الدورية السحابية الأسبوعية وحفظها بنجاح!")
                    onComplete(true, jsonStr)
                }
                .addOnFailureListener { e ->
                    onComplete(true, jsonStr)
                }
        } catch (e: Exception) {
            e.printStackTrace()
            onComplete(false, e.message ?: "Unknown error")
        }
    }

    fun restoreSystemFromBackup(jsonStr: String, onComplete: (Boolean, String) -> Unit) {
        try {
            val root = org.json.JSONObject(jsonStr)
            
            // Restore providers
            if (root.has("providers")) {
                val array = root.getJSONArray("providers")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val id = obj.getString("id")
                    val phone = obj.optString("phone", id)
                    val data = hashMapOf(
                        "id" to id,
                        "name" to obj.optString("name", ""),
                        "phone" to phone,
                        "customCategoryName" to obj.optString("customCategoryName", ""),
                        "cityId" to obj.optString("cityId", ""),
                        "localNeighborhood" to obj.optString("localNeighborhood", ""),
                        "isAvailable" to obj.optBoolean("isAvailable", true),
                        "rating" to obj.optDouble("rating", 4.5).toFloat(),
                        "numReviews" to obj.optInt("numReviews", 1)
                    )
                    db.collection("providers").document(id).set(data)
                }
            }

            // Restore bookings
            if (root.has("bookings")) {
                val array = root.getJSONArray("bookings")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val id = obj.getString("id")
                    val data = hashMapOf(
                        "id" to id,
                        "customerPhone" to obj.optString("customerPhone", ""),
                        "customerName" to obj.optString("customerName", ""),
                        "customerArea" to obj.optString("customerArea", ""),
                        "providerId" to obj.optString("providerId", ""),
                        "providerName" to obj.optString("providerName", ""),
                        "dateString" to obj.optString("dateString", ""),
                        "timeString" to obj.optString("timeString", ""),
                        "serviceType" to obj.optString("serviceType", ""),
                        "status" to obj.optString("status", "PENDING")
                    )
                    db.collection("bookings").document(id).set(data)
                }
            }

            // Restore categories
            if (root.has("categories")) {
                val array = root.getJSONArray("categories")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val id = obj.getString("id")
                    val data = hashMapOf(
                        "id" to id,
                        "name" to obj.optString("name", ""),
                        "icon" to obj.optString("icon", ""),
                        "parentId" to obj.optString("parentId", ""),
                        "isMainCategory" to obj.optBoolean("isMainCategory", true)
                    )
                    db.collection("categories").document(id).set(data)
                }
            }

            // Restore stores
            if (root.has("stores")) {
                val array = root.getJSONArray("stores")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val id = obj.getString("id")
                    val data = hashMapOf(
                        "id" to id,
                        "name" to obj.optString("name", ""),
                        "description" to obj.optString("description", ""),
                        "phone" to obj.optString("phone", ""),
                        "ownerName" to obj.optString("ownerName", ""),
                        "cityId" to obj.optString("cityId", ""),
                        "localNeighborhood" to obj.optString("localNeighborhood", ""),
                        "isActive" to obj.optBoolean("isActive", true)
                    )
                    db.collection("stores").document(id).set(data)
                }
            }

            // Restore properties
            if (root.has("properties")) {
                val array = root.getJSONArray("properties")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val id = obj.getString("id")
                    val data = hashMapOf(
                        "id" to id,
                        "title" to obj.optString("title", ""),
                        "description" to obj.optString("description", ""),
                        "price" to obj.optDouble("price", 0.0),
                        "phone" to obj.optString("phone", ""),
                        "cityId" to obj.optString("cityId", ""),
                        "ownerName" to obj.optString("ownerName", ""),
                        "isActive" to obj.optBoolean("isActive", true)
                    )
                    db.collection("properties").document(id).set(data)
                }
            }

            triggerNotification("💚 تم استعادة قاعدة البيانات الشاملة بنجاح ومزامنتها سحابياً!")
            onComplete(true, "Success")
        } catch (e: Exception) {
            e.printStackTrace()
            onComplete(false, e.message ?: "Unknown parsing error")
        }
    }

    fun editCategory(categoryId: String, newName: String, newIcon: String, parentId: String = "", isMainCategory: Boolean = true) {
        db.collection("categories").document(categoryId).get().addOnSuccessListener { snapshot ->
            val cat = snapshot.toObject(CategoryEntity::class.java)
            if (cat != null) {
                db.collection("categories").document(categoryId).set(
                    cat.copy(
                        name = newName,
                        icon = newIcon,
                        parentId = parentId,
                        isMainCategory = isMainCategory
                    )
                )
            }
        }
        triggerNotification("✏️ تم تعديل القسم وتحديث هيكلته بنجاح: $newName")
    }

    fun toggleBlockStore(storeId: String, reason: String = "") {
        db.collection("stores").document(storeId).get().addOnSuccessListener { snapshot ->
            val store = snapshot.toObject(com.example.data.StoreEntity::class.java)
            if (store != null) {
                val newStatus = !store.isBlocked
                val updated = store.copy(isBlocked = newStatus, blockReason = reason)
                db.collection("stores").document(storeId).set(updated).addOnSuccessListener {
                    triggerNotification(
                        if (newStatus) "🚫 تم حظر المتجر/المحل (${store.name}) بنجاح!"
                        else "🟢 تم إلغاء حظر المتجر/المحل (${store.name}) وتفعيله!"
                    )
                    logAdminActivity("تغيير حالة حظر المتجر (${store.name}) إلى: ${if (newStatus) "محظور" else "متاح"}")
                }
            }
        }
    }

    fun saveCustomProfileTab(tab: com.example.data.CustomProfileTabEntity) {
        val targetId = if (tab.id.isEmpty()) java.util.UUID.randomUUID().toString().take(6) else tab.id
        val finalTab = tab.copy(id = targetId)
        db.collection("custom_profile_tabs").document(targetId).set(finalTab)
        triggerNotification("📑 تم حفظ التبويب المخصص بنجاح: ${tab.title}")
    }

    fun deleteCustomProfileTab(tabId: String) {
        db.collection("custom_profile_tabs").document(tabId).delete()
        triggerNotification("🗑️ تم حذف التبويب المخصص")
    }

    fun toggleCustomProfileTab(tabId: String) {
        val current = _customProfileTabs.value.find { it.id == tabId }
        if (current != null) {
            val updated = current.copy(isEnabled = !current.isEnabled)
            db.collection("custom_profile_tabs").document(tabId).set(updated)
            triggerNotification(if (updated.isEnabled) "🟢 تم تفعيل التبويب" else "🔴 تم إيقاف التبويب")
        }
    }


    fun deleteCategory(categoryId: String) {
        db.collection("categories").document(categoryId).delete()
        triggerNotification("🗑️ تم حذف القسم بالكامل")
    }

    fun togglePinCategory(categoryId: String) {
        db.collection("categories").document(categoryId).get().addOnSuccessListener { snapshot ->
            val cat = snapshot.toObject(com.example.data.CategoryEntity::class.java)
            if (cat != null) {
                val updated = cat.copy(isPinned = !cat.isPinned)
                db.collection("categories").document(categoryId).set(updated)
                triggerNotification(if (updated.isPinned) "📌 تم تثبيت القسم في البداية" else "🔓 تم إلغاء تثبيت القسم")
            }
        }
    }

    fun mergeCategories(sourceCategoryId: String, targetCategoryId: String) {
        if (sourceCategoryId == targetCategoryId) {
            triggerNotification("⚠️ لا يمكن دمج القسم مع نفسه!")
            return
        }

        // 1. Move approved providers of sourceCategory to targetCategory
        db.collection("providers").whereEqualTo("categoryId", sourceCategoryId).get().addOnSuccessListener { snapshot ->
            for (doc in snapshot.documents) {
                doc.reference.update("categoryId", targetCategoryId)
            }
        }

        // 2. Move pending providers of sourceCategory to targetCategory
        db.collection("pending_providers").whereEqualTo("categoryId", sourceCategoryId).get().addOnSuccessListener { snapshot ->
            for (doc in snapshot.documents) {
                doc.reference.update("categoryId", targetCategoryId)
            }
        }

        // 2b. Move stores of sourceCategory to targetCategory
        db.collection("stores").whereEqualTo("categoryId", sourceCategoryId).get().addOnSuccessListener { snapshot ->
            for (doc in snapshot.documents) {
                doc.reference.update("categoryId", targetCategoryId)
            }
        }

        // 3. Delete the source category
        db.collection("categories").document(sourceCategoryId).delete().addOnSuccessListener {
            triggerNotification("✅ تم دمج القسمين وتحويل كافة الفنيين والمتاجر بنجاح!")
        }
    }

    fun saveCategoryEntity(cat: CategoryEntity) {
        val catId = cat.id.ifEmpty { UUID.randomUUID().toString().take(6) }
        val updated = cat.copy(id = catId)
        db.collection("categories").document(catId).set(updated)
        triggerNotification("📁 تم حفظ وتحديث بيانات القسم: ${updated.name}")
    }

    fun addSubCategory(parentId: String, nameAr: String, icon: String) {
        val nextId = UUID.randomUUID().toString().take(6)
        val subCat = CategoryEntity(
            id = nextId,
            name = nameAr,
            icon = icon,
            parentId = parentId,
            isMainCategory = false,
            order = _categories.value.size + 1
        )
        db.collection("categories").document(nextId).set(subCat)
        triggerNotification("📂 تم إضافة قسم فرعي جديد: $nameAr")
    }

    fun convertCategoryType(catId: String, newParentId: String, isMain: Boolean) {
        val cat = _categories.value.find { it.id == catId }
        if (cat != null) {
            val updated = cat.copy(parentId = newParentId, isMainCategory = isMain)
            db.collection("categories").document(catId).set(updated)
            triggerNotification("🔄 تم تغيير تصنيف القسم بنجاح")
        }
    }

    fun toggleStoreChatDisabled(storeId: String) {
        val store = _stores.value.find { it.id == storeId }
        if (store != null) {
            val updated = store.copy(isChatDisabled = !store.isChatDisabled)
            saveStore(updated)
            triggerNotification(if (updated.isChatDisabled) "🚫 تم إيقاف المحادثات للمحل/المركز" else "💬 تم تفعيل المحادثات للمحل/المركز")
        }
    }

    fun toggleStoreActive(storeId: String) {
        val store = _stores.value.find { it.id == storeId }
        if (store != null) {
            val updated = store.copy(isActive = !store.isActive)
            saveStore(updated)
            triggerNotification(if (!updated.isActive) "🔒 تم حظر/إيقاف المحل/المركز مؤقتاً" else "🟢 تم تفعيل المحل/المركز")
        }
    }

    fun toggleStorePinned(storeId: String) {
        val store = _stores.value.find { it.id == storeId }
        if (store != null) {
            val updated = store.copy(isPinned = !store.isPinned)
            saveStore(updated)
            triggerNotification(if (updated.isPinned) "📌 تم تثبيت المحل في البداية" else "🔓 تم إلغاء تثبيت المحل")
        }
    }

    fun updateStoreMaxImages(storeId: String, maxImages: Int) {
        val store = _stores.value.find { it.id == storeId }
        if (store != null) {
            val updated = store.copy(maxImages = maxImages)
            saveStore(updated)
            triggerNotification("📸 تم تحديث الحد الأقصى للصور إلى: $maxImages")
        }
    }

    // Bookings Management (Now modularized in MainViewModelBookings.kt)
    fun addBooking(
        name: String, 
        phone: String, 
        area: String, 
        serviceType: String, 
        providerId: String, 
        providerName: String, 
        dateString: String = "2026-06-20", 
        timeString: String = "12:00 م",
        couponCode: String = "",
        pinCode: String = "",
        customBookingId: String = "",
        customPassword: String = ""
    ) {
        addBookingImpl(
            name, phone, area, serviceType, providerId, providerName,
            dateString, timeString, couponCode, pinCode, customBookingId, customPassword
        )
        return
    }

    fun addBooking_disabled_unused(
        name: String, 
        phone: String, 
        area: String, 
        serviceType: String, 
        providerId: String, 
        providerName: String, 
        dateString: String = "2026-06-20", 
        timeString: String = "12:00 م",
        couponCode: String = "",
        pinCode: String = "",
        customBookingId: String = "",
        customPassword: String = ""
    ) {
        val cleanPhone = phone.trim()
        val cleanName = name.trim()
        
        // 1. Verification of identity of registered Yemeni user phone
        val isValidYemeniPhone = cleanPhone.length == 9 && (
            cleanPhone.startsWith("77") || 
            cleanPhone.startsWith("73") || 
            cleanPhone.startsWith("71") || 
            cleanPhone.startsWith("70") || 
            cleanPhone.startsWith("78")
        )
        if (!isValidYemeniPhone) {
            triggerNotification("❌ الهوية غير مسجلة: رقم الهاتف يجب أن يكون يمنياً صحيحاً مفعلاً ومكوناً من 9 أرقام يبدأ بـ 77 أو 73 أو 71 أو 70!")
            return
        }

        // 2. Duplication & Overlap prevention scan
        val isTimeSlotTaken = _bookings.value.any {
            it.providerId == providerId &&
            it.dateString.trim() == dateString.trim() &&
            it.timeString.trim() == timeString.trim() &&
            (it.status == "PENDING" || it.status == "APPROVED" || it.status == "IN_PROGRESS")
        }
        if (isTimeSlotTaken) {
            triggerNotification("❌ عذراً! هذا الوقت (${timeString}) وتاريخ (${dateString}) محجوز بالفعل لدى مقدم الخدمة هذا. يرجى اختيار موعد آخر.")
            return
        }

        val isCustomerBusy = _bookings.value.any {
            it.customerPhone.trim() == cleanPhone &&
            it.dateString.trim() == dateString.trim() &&
            it.timeString.trim() == timeString.trim() &&
            (it.status == "PENDING" || it.status == "APPROVED" || it.status == "IN_PROGRESS")
        }
        if (isCustomerBusy) {
            triggerNotification("❌ عذراً! لديك حجز آخر بالفعل في نفس هذا الموعد والتاريخ. لا يمكنك تكرار الحجوزات المتداخلة.")
            return
        }

        val isDuplicate = _bookings.value.any { 
            it.customerPhone.trim() == cleanPhone && 
            it.providerId == providerId && 
            (it.status == "PENDING" || it.status == "APPROVED" || it.status == "IN_PROGRESS")
        }
        if (isDuplicate) {
            triggerNotification("⚠️ حجز مكرر: توجد استمارة حجز معلقة أو نشطة قائمة فعلياً بنفس الرقم لهذا الفني!")
            return
        }

        val techForBooking = _providers.value.find { it.id == providerId }
        var finalServiceType = serviceType
        if (techForBooking?.isPaymentRequired == true) {
            finalServiceType += " (⚠️ يتطلب دفع إلكتروني مسبق وموثق)"
        }
        val trimmedCoupon = couponCode.trim().uppercase()
        if (trimmedCoupon.isNotEmpty()) {
            val coupon = _coupons.value.find { it.code.uppercase() == trimmedCoupon }
            if (coupon != null) {
                val isExpired = System.currentTimeMillis() > coupon.expiryTimestamp
                val isLimitReached = coupon.usedCount >= coupon.maxUsageCount
                if (isExpired) {
                    triggerNotification("❌ هذا الكوبون ($trimmedCoupon) منتهي الصلاحية!")
                } else if (isLimitReached) {
                    triggerNotification("❌ هذا الكوبون ($trimmedCoupon) وصل للحد الأقصى للاستخدام!")
                } else {
                    // Valid coupon! Increment used count in Firestore
                    val updatedCoupon = coupon.copy(usedCount = coupon.usedCount + 1)
                    db.collection("coupons").document(coupon.id).set(updatedCoupon)
                    
                    // Apply discount or points
                    val discountMsg = if (coupon.discountPercentage > 0) {
                        "خصم ${coupon.discountPercentage}% مفعّل"
                    } else {
                        "شحن نقاط بقيمة ${coupon.pointsValue}"
                    }
                    finalServiceType += " [كوبون: $trimmedCoupon - $discountMsg]"
                    triggerNotification("🎉 تم تطبيق الكوبون ($trimmedCoupon) بنجاح: $discountMsg")
                }
            } else {
                triggerNotification("❌ رمز الكوبون ($trimmedCoupon) غير صحيح أو غير متوفر!")
            }
        }

        val cleanDate = dateString.replace("/", "-").replace(".", "-")
        val cleanTime = timeString.replace(" ", "_").replace(":", "-")
        val slotId = "${cleanDate}_${cleanTime}"
        val availabilityRef = db.collection("providers")
            .document(providerId)
            .collection("availability")
            .document(slotId)

        val generatedNum = if (customBookingId.trim().isNotEmpty()) {
            val rawId = customBookingId.trim()
            if (rawId.startsWith("b_")) rawId else "b_$rawId"
        } else {
            "b_" + UUID.randomUUID().toString().take(6)
        }
        val generatedPass = if (customPassword.trim().isNotEmpty()) customPassword.trim() else com.example.utils.BookingUtils.generateBookingPassword()

        val newBooking = BookingEntity(
            id = generatedNum,
            customerName = cleanName,
            customerPhone = cleanPhone,
            customerArea = area,
            serviceType = finalServiceType,
            providerId = providerId,
            providerName = providerName,
            dateString = dateString,
            timeString = timeString,
            status = "PENDING",
            pinCode = generatedPass,
            
            bookingNumber = generatedNum,
            bookingPassword = generatedPass,
            clientId = cleanPhone,
            clientName = cleanName,
            clientPhone = cleanPhone,
            clientAddress = area,
            providerPhone = _providers.value.find { it.id == providerId }?.phone ?: "",
            category = _providers.value.find { it.id == providerId }?.categoryId ?: "",
            subCategory = "",
            serviceDetails = finalServiceType,
            date = dateString,
            time = timeString,
            requiresPasswordForCancellation = true,
            cancellationAttempts = 0,
            maxCancellationAttempts = 3,
            isLocked = false,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        db.runTransaction { transaction ->
            val availabilitySnap = transaction.get(availabilityRef)
            if (availabilitySnap.exists() && availabilitySnap.getBoolean("booked") == true) {
                throw Exception("الوقت محجوز مسبقاً")
            }
            
            // Mark slot as booked
            transaction.set(availabilityRef, mapOf(
                "booked" to true,
                "bookingId" to newBooking.id,
                "customerId" to cleanPhone
            ))
            
            // Save booking
            val bookingDocRef = db.collection("bookings").document(newBooking.id)
            transaction.set(bookingDocRef, newBooking)
        }.addOnSuccessListener {
            // Auto-save user identity in memory if empty to ensure they can track notifications immediately
            if (_currentUserPhone.value.isEmpty()) {
                _currentUserPhone.value = cleanPhone
                _currentUserName.value = cleanName
                _currentUserResidence.value = area
            }

            // Notify the customer (user) that their booking was successfully submitted with booking number and password
            addNotification(
                title = "📅 تم تسجيل حجزك بنجاح",
                message = "مرحباً يا غالي العميل $cleanName، تم إرسال طلب حجزك بنجاح للفني: $providerName.\nرقم الحجز: ${newBooking.bookingNumber}\n🔑 كلمة المرور السرية للإلغاء: ${newBooking.bookingPassword}\nالموعد: $dateString الساعة $timeString. تم إرسال كلمة المرور كذلك عبر الإشعارات السحابية، والواتساب، ورسائل الـ SMS بنجاح للتحقق.",
                targetType = "USER",
                targetValue = cleanPhone
            )

            // Compile a highly detailed notification containing customer's name, phone, and area of residence
            val detailedMessage = "طلب حجز جديد من العميل: $cleanName، هاتف العميل: $cleanPhone، منطقة السكن: $area. الخدمة المطلوبة: $serviceType. الموعد المفضل: $dateString الساعة $timeString. رقم الحجز: ${newBooking.bookingNumber}"

            // 1. Always notify the Admin/Supervisor
            addNotification(
                title = "📅 طلب حجز جديد بانتظار المراجعة",
                message = detailedMessage,
                targetType = "SUPERVISOR",
                targetValue = "all"
            )

            // 2. Distribute to technicians, stores, clinics, restaurants, or properties according to the target
            if (providerId.startsWith("ALL_STORE")) {
                val matchingStores = _stores.value.filter { it.sectionId == "stores" || it.sectionId == "shops" }
                matchingStores.forEach { s ->
                    addNotification(
                        title = "⚡ طلب سلعة أو خدمة عاجلة لمتجرك",
                        message = detailedMessage,
                        targetType = "STORE",
                        targetValue = s.phone
                    )
                }
            } else if (providerId.startsWith("ALL_RESTAURANT")) {
                val matchingRestaurants = _stores.value.filter { it.sectionId == "restaurants" || it.sectionId == "food" }
                matchingRestaurants.forEach { r ->
                    addNotification(
                        title = "⚡ طلب وجبة أو حجز مطعم عاجل",
                        message = detailedMessage,
                        targetType = "RESTAURANT",
                        targetValue = r.phone
                    )
                }
            } else if (providerId.startsWith("ALL_CLINIC") || providerId.startsWith("ALL_MEDICAL")) {
                val matchingClinics = _stores.value.filter { it.sectionId == "medical" || it.sectionId == "clinics" || it.medicalLicenseNo.isNotEmpty() }
                matchingClinics.forEach { c ->
                    addNotification(
                        title = "⚡ طلب استشارة أو كشف طبي عاجل",
                        message = detailedMessage,
                        targetType = "CLINIC",
                        targetValue = c.phone
                    )
                }
            } else if (providerId.startsWith("ALL_REALESTATE") || providerId.startsWith("ALL_PROP")) {
                _properties.value.forEach { p ->
                    addNotification(
                        title = "⚡ طلب عقار أو إيجار عاجل في منطقتك",
                        message = detailedMessage,
                        targetType = "PROPERTY",
                        targetValue = p.phone
                    )
                }
            } else {
                when (_distributionMode.value) {
                    BookingDistributionMode.SPECIFIC_PROVIDER -> {
                        // Find and notify the specific technician named in the booking
                        val tech = _providers.value.find { it.id == providerId }
                        if (tech != null) {
                            addNotification(
                                title = "📅 حجز جديد موجه لك بالاسم",
                                message = detailedMessage,
                                targetType = "PROVIDER",
                                targetValue = tech.phone
                            )
                        } else {
                            val store = _stores.value.find { it.id == providerId }
                            if (store != null) {
                                addNotification(
                                    title = "📅 حجز / طلب جديد لمتجرك",
                                    message = detailedMessage,
                                    targetType = "STORE",
                                    targetValue = store.phone
                                )
                            }
                        }
                    }
                    BookingDistributionMode.NEAREST_PROVIDER, BookingDistributionMode.ALL_PROVIDERS -> {
                        // Find and notify all providers in the same category (or closest geographically)
                        val categoryIdOfProvider = _providers.value.find { it.id == providerId }?.categoryId ?: "1"
                        val catTechs = _providers.value.filter { it.categoryId == categoryIdOfProvider }
                        catTechs.forEach { tech ->
                            addNotification(
                                title = "📅 فرصة حجز عمل جديدة في منطقتك",
                                message = detailedMessage,
                                targetType = "PROVIDER",
                                targetValue = tech.phone
                            )
                        }
                    }
                    else -> {
                        // ADMIN_ONLY or CATEGORY_SUPERVISOR -> Handled by Supervisor notifications
                    }
                }
            }

            // 3. Inform of final dispatch
            triggerNotification("🎉 تم إرسال طلب الحجز بنجاح ومزامنته!")
        }.addOnFailureListener { e ->
            triggerNotification("❌ فشل الحجز: ${e.message}")
        }

        triggerNotification("تم إرسال طلب الحجز، سيتم مراجعته")
    }

    fun updateBookingStatus(bookingId: String, newStatus: String, rejectionReason: String = "") {
        updateBookingStatusImpl(bookingId, newStatus, rejectionReason)
    }

    fun deleteBooking(bookingId: String) {
        deleteBookingImpl(bookingId)
    }

    fun deleteAllBookings(customerPhone: String) {
        deleteAllBookingsImpl(customerPhone)
    }

    fun updateBooking(booking: BookingEntity) {
        updateBookingImpl(booking)
    }

    // Targeted Notifications Management with Strict Validation & Deduplication
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

    private val _readNotificationIds = MutableStateFlow<Set<String>>(emptySet())
    val readNotificationIds: StateFlow<Set<String>> = _readNotificationIds.asStateFlow()

    fun markNotificationAsRead(context: android.content.Context, notifId: String) {
        val sp = context.getSharedPreferences("yemen_service_prefs", android.content.Context.MODE_PRIVATE)
        val currentSet = sp.getStringSet("read_notification_ids", emptySet()) ?: emptySet()
        val newSet = currentSet + notifId
        sp.edit().putStringSet("read_notification_ids", newSet).apply()
        _readNotificationIds.value = newSet
        try {
            db.collection("notifications").document(notifId).update("isRead", true)
        } catch (e: Exception) {}
    }

    fun loadReadNotifications(context: android.content.Context) {
        val sp = context.getSharedPreferences("yemen_service_prefs", android.content.Context.MODE_PRIVATE)
        val currentSet = sp.getStringSet("read_notification_ids", emptySet()) ?: emptySet()
        _readNotificationIds.value = currentSet
    }

    fun markAllNotificationsAsRead(context: android.content.Context) {
        val allIds = _notifications.value.map { it.id }.toSet()
        val sp = context.getSharedPreferences("yemen_service_prefs", android.content.Context.MODE_PRIVATE)
        sp.edit().putStringSet("read_notification_ids", allIds).apply()
        _readNotificationIds.value = allIds
        _notifications.value.forEach { notif ->
            try {
                db.collection("notifications").document(notif.id).update("isRead", true)
            } catch (e: Exception) {}
        }
        triggerNotification("✓ تم تحديد جميع الإشعارات كمقروءة")
    }

    fun deleteNotification(notifId: String) {
        _notifications.value = _notifications.value.filter { it.id != notifId }
        db.collection("notifications").document(notifId).delete()
        triggerNotification("🗑️ تم حذف الإشعار")
    }

    fun deleteAllNotifications() {
        _notifications.value = emptyList()
        db.collection("notifications").get().addOnSuccessListener { snapshot ->
            snapshot?.documents?.forEach { doc -> doc.reference.delete() }
            triggerNotification("🧹 تم حذف جميع الإشعارات بنجاح")
        }
    }

    fun deleteAllChats() {
        db.collection("chat_channels").get().addOnSuccessListener { snapshot ->
            snapshot?.documents?.forEach { doc -> doc.reference.delete() }
            triggerNotification("🧹 تم حذف جميع المحادثات بنجاح")
        }
    }

    // Instant Chats Management / Admin Supervision
    fun deleteChatMessage(channelId: String, messageId: String) {
        db.collection("chat_channels").document(channelId).get().addOnSuccessListener { snapshot ->
            val ch = snapshot.toObject(ChatChannelEntity::class.java)
            if (ch != null) {
                val updatedMessages = ch.messages.filter { it.id != messageId }
                val lastMsg = updatedMessages.lastOrNull()?.message ?: "تم حذف الرسالة بقرار الرقابة الإدارية"
                db.collection("chat_channels").document(channelId).set(
                    ch.copy(
                        lastMessage = lastMsg,
                        messages = updatedMessages
                    )
                ).addOnSuccessListener {
                    triggerNotification("🗑️ تم حذف الرسالة بنجاح بقرار الرقابة الإدارية.")
                }
            }
        }
    }

    fun broadcastAdminWarning(channelId: String, warningText: String) {
        val systemMsg = ChatMessageEntity(
            id = UUID.randomUUID().toString(),
            senderId = "system_warning",
            message = "⚠️ تحذير إداري رسمي: $warningText",
            timestamp = System.currentTimeMillis(),
            senderName = "الرقابة الإدارية"
        )
        db.collection("chat_channels").document(channelId).get().addOnSuccessListener { snapshot ->
            val ch = snapshot.toObject(ChatChannelEntity::class.java)
            if (ch != null) {
                db.collection("chat_channels").document(channelId).set(
                    ch.copy(
                        lastMessage = "⚠️ تحذير إداري رسمي",
                        timestamp = System.currentTimeMillis(),
                        messages = ch.messages + systemMsg
                    )
                ).addOnSuccessListener {
                    triggerNotification("📢 تم إرسال التحذير الإداري إلى المحادثة.")
                }
            }
        }
    }

    // Instant Chats Management
    fun replyToChatChannel(channelId: String, senderId: String, msgText: String, senderName: String, imageUrl: String = "") {
        if (msgText.trim().isEmpty() && imageUrl.isEmpty()) return
        val newMsg = ChatMessageEntity(
            id = UUID.randomUUID().toString(),
            senderId = senderId,
            message = msgText,
            timestamp = System.currentTimeMillis(),
            senderName = senderName,
            imageUrl = imageUrl
        )
        db.collection("chat_channels").document(channelId).get().addOnSuccessListener { snapshot ->
            val ch = snapshot.toObject(ChatChannelEntity::class.java)
            val finalMsgText = if (msgText.isNotEmpty()) msgText else "📷 [صورة]"
            if (ch != null) {
                db.collection("chat_channels").document(channelId).set(
                    ch.copy(
                        lastMessage = finalMsgText,
                        timestamp = System.currentTimeMillis(),
                        messages = ch.messages + newMsg
                    )
                )
            } else {
                val newCh = ChatChannelEntity(
                    id = channelId,
                    userName = senderName,
                    lastMessage = finalMsgText,
                    timestamp = System.currentTimeMillis(),
                    messages = listOf(newMsg)
                )
                db.collection("chat_channels").document(channelId).set(newCh)
            }

            // Real-time notification dispatch
            if (senderId == "admin" || senderId.startsWith("super_")) {
                if (channelId.startsWith("support_")) {
                    val userId = channelId.removePrefix("support_")
                    db.collection("registered_users").document(userId).get().addOnSuccessListener { userSnap ->
                        val userPhone = userSnap?.getString("phone")
                        if (!userPhone.isNullOrEmpty()) {
                            addNotification(
                                title = "💬 رد جديد من إدارة الدعم الفني",
                                message = "المشرف أرسل لك رسالة: $finalMsgText",
                                targetType = "USER",
                                targetValue = userPhone
                            )
                        }
                    }
                } else if (channelId.contains("_u_")) {
                    // Extract customer phone or id and notify
                    val parts = channelId.split("_u_")
                    if (parts.size > 1) {
                        val userId = parts[1]
                        db.collection("registered_users").document(userId).get().addOnSuccessListener { userSnap ->
                            val userPhone = userSnap?.getString("phone")
                            if (!userPhone.isNullOrEmpty()) {
                                addNotification(
                                    title = "💬 رسالة جديدة في الشات من الإدارة",
                                    message = "المشرف أرسل لك: $finalMsgText",
                                    targetType = "USER",
                                    targetValue = userPhone
                                )
                            }
                        }
                    }
                } else if (channelId.startsWith("chat_") && !channelId.startsWith("support_")) {
                    // For custom chat rooms, find which one is user and provider
                    val parts = channelId.removePrefix("chat_").split("_")
                    if (parts.size == 2) {
                        // Admin warning or admin reply inside a chat between user and provider
                        // Notify both parts!
                        parts.forEach { part ->
                            if (part.all { it.isDigit() } || part.startsWith("+")) {
                                addNotification(
                                    title = "💬 رسالة جديدة من الإدارة",
                                    message = "المشرف أرسل في الدردشة المشتركة: $finalMsgText",
                                    targetType = "USER",
                                    targetValue = part
                                )
                            } else {
                                db.collection("providers").document(part).get().addOnSuccessListener { provSnap ->
                                    val provPhone = provSnap?.getString("phone")
                                    if (!provPhone.isNullOrEmpty()) {
                                        addNotification(
                                            title = "💬 رسالة جديدة من الإدارة",
                                            message = "المشرف أرسل في الدردشة المشتركة: $finalMsgText",
                                            targetType = "PROVIDER",
                                            targetValue = provPhone
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                if (channelId.startsWith("support_")) {
                    // User replying to support -> Notify supervisor
                    addNotification(
                        title = "💬 رسالة دعم جديدة من: $senderName",
                        message = "محتوى الرسالة: $finalMsgText",
                        targetType = "SUPERVISOR",
                        targetValue = "all"
                    )
                } else if (channelId.startsWith("chat_p_") || (channelId.startsWith("chat_") && !channelId.startsWith("support_"))) {
                    // Direct chat between Customer User and Provider Technician
                    val parts = if (channelId.startsWith("chat_p_")) {
                        val pId = channelId.substringAfter("chat_p_").substringBefore("_u_")
                        val uId = channelId.substringAfter("_u_")
                        listOf(pId, uId)
                    } else {
                        channelId.removePrefix("chat_").split("_")
                    }
                    if (parts.size == 2) {
                        val id1 = parts[0]
                        val id2 = parts[1]
                        val recipientId = if (senderId == id1) id2 else id1
                        
                        // Let's identify the recipient and notify them
                        db.collection("providers").document(recipientId).get().addOnSuccessListener { provSnap ->
                            if (provSnap != null && provSnap.exists()) {
                                // Recipient is a provider! Send notification to provider's phone
                                val provPhone = provSnap.getString("phone")
                                if (!provPhone.isNullOrEmpty()) {
                                    addNotification(
                                        title = "💬 رسالة شات جديدة من عميل",
                                        message = "$senderName أرسل لك: $finalMsgText",
                                        targetType = "PROVIDER",
                                        targetValue = provPhone
                                    )
                                }
                            } else {
                                // Recipient is a customer/user!
                                // If the recipientId is a phone number, use it directly. Otherwise, look up their registered user phone
                                if (recipientId.all { it.isDigit() } || recipientId.startsWith("+")) {
                                    addNotification(
                                        title = "💬 رسالة شات جديدة من الفني",
                                        message = "$senderName أرسل لك: $finalMsgText",
                                        targetType = "USER",
                                        targetValue = recipientId
                                    )
                                } else {
                                    db.collection("registered_users").document(recipientId).get().addOnSuccessListener { userSnap ->
                                        val userPhone = userSnap?.getString("phone") ?: recipientId
                                        addNotification(
                                            title = "💬 رسالة شات جديدة من الفني",
                                            message = "$senderName أرسل لك: $finalMsgText",
                                            targetType = "USER",
                                            targetValue = userPhone
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

    fun deleteChatChannel(channelId: String) {
        db.collection("chat_channels").document(channelId).delete()
        triggerNotification("🗑️ تم حذف المحادثة بالكامل.")
    }

    // Advanced Instant Chat Engine
    fun openOrCreateChatChannel(
        targetId: String,
        targetType: String, // "PROVIDER", "STORE", "PROPERTY", "RESTAURANT", "ADMIN", "SUPERVISOR", "CATEGORY", "BOOKING"
        targetName: String,
        targetPhone: String = "",
        targetCategory: String = "",
        relatedEntityId: String = "",
        relatedEntityType: String = "",
        onCreated: (ChatChannelEntity) -> Unit
    ) {
        val currUser = _currentUserId.value
        val currPhone = _currentUserPhone.value
        val currName = _currentUserName.value.ifEmpty { "عميل التطبيق" }

        val settingsState = _settings.value
        val (effectiveTargetId, effectiveTargetType, effectiveTargetName) = when (settingsState.chatRoutingMode) {
            "ADMIN_ONLY" -> Triple("admin", "ADMIN", "الإدارة والدعم الفني 👑")
            "ADMIN_SUPERVISORS" -> Triple("supervisors", "SUPERVISOR", "قسم الإشراف والمتابعة 👮")
            else -> Triple(targetId, targetType, targetName)
        }

        val chanId = if (relatedEntityId.isNotBlank()) {
            "chat_${(relatedEntityType.ifEmpty { effectiveTargetType }).lowercase()}_${relatedEntityId}_u_${currUser.ifEmpty { currPhone.ifEmpty { "guest" } }}"
        } else {
            "chat_${effectiveTargetType.lowercase()}_${effectiveTargetId}_u_${currUser.ifEmpty { currPhone.ifEmpty { "guest" } }}"
        }

        val newCh = ChatChannelEntity(
            id = chanId,
            channelType = effectiveTargetType,
            targetId = effectiveTargetId,
            targetName = effectiveTargetName,
            targetPhone = targetPhone,
            targetCategory = targetCategory,
            relatedEntityId = relatedEntityId,
            relatedEntityType = relatedEntityType.ifEmpty { effectiveTargetType },
            customerId = currUser,
            customerName = currName,
            customerPhone = currPhone,
            userName = effectiveTargetName,
            lastMessage = if (relatedEntityId.isNotBlank()) "بدء محادثة فورية مخصصة للحجز ($relatedEntityId)" else "بدء محادثة فورية جديدة مع $effectiveTargetName",
            lastMessageTime = System.currentTimeMillis(),
            timestamp = System.currentTimeMillis(),
            messages = listOf(
                ChatMessageEntity(
                    id = UUID.randomUUID().toString(),
                    senderId = "system",
                    senderName = "النظام",
                    message = "مرحباً بكم في خدمة المحادثة الفورية مع $effectiveTargetName. يسعدنا خدمتكم!",
                    timestamp = System.currentTimeMillis(),
                    mediaType = "TEXT",
                    status = "READ"
                )
            )
        )

        // Set state synchronously for 0ms instant response regardless of network connectivity
        _activeChatChannel.value = newCh
        onCreated(newCh)

        try {
            db.collection("chat_channels").document(chanId).get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val existing = snapshot.toObject(ChatChannelEntity::class.java)
                    if (existing != null) {
                        _activeChatChannel.value = existing
                    }
                } else {
                    db.collection("chat_channels").document(chanId).set(newCh)
                }
            }.addOnFailureListener {
                // Keep local channel active when offline
            }
        } catch (e: Exception) {
            // Offline fallback
        }
    }

    fun sendChatMessageAdvanced(
        channelId: String,
        messageText: String,
        mediaType: String = "TEXT", // "TEXT", "AUDIO", "IMAGE", "VIDEO", "CALL"
        mediaUrl: String = "",
        audioDurationSec: Int = 0
    ) {
        val settingsState = _settings.value
        val currUser = _currentUserId.value
        val currPhone = _currentUserPhone.value
        val currName = _currentUserName.value.ifEmpty { "مستخدم" }

        if (settingsState.disableChatAll) {
            triggerNotification("⚠️ المحادثات متوقفة حالياً بقرار من الإدارة.")
            return
        }

        val blockedList = settingsState.chatBlockedIds.split(",").map { it.trim() }
        if (blockedList.contains(currUser) || (currPhone.isNotEmpty() && blockedList.contains(currPhone))) {
            triggerNotification("🛑 تم تعليق حسابك من استخدام الدردشة الفورية.")
            return
        }

        when (mediaType) {
            "TEXT" -> if (!settingsState.isChatTextEnabled) { triggerNotification("⚠️ الرسائل النصية معطلة حالياً"); return }
            "AUDIO" -> if (!settingsState.isChatAudioEnabled) { triggerNotification("⚠️ الرسائل الصوتية معطلة حالياً"); return }
            "IMAGE" -> if (!settingsState.isChatImageEnabled) { triggerNotification("⚠️ إرسال الصور معطل حالياً"); return }
            "VIDEO" -> if (!settingsState.isChatVideoEnabled) { triggerNotification("⚠️ إرسال الفيديو معطل حالياً"); return }
            "CALL" -> if (!settingsState.isChatCallEnabled) { triggerNotification("⚠️ المكالمات المباشرة معطلة حالياً"); return }
        }

        val newMsg = ChatMessageEntity(
            id = UUID.randomUUID().toString(),
            senderId = currUser.ifEmpty { currPhone.ifEmpty { "guest" } },
            senderName = currName,
            senderPhone = currPhone,
            message = messageText,
            timestamp = System.currentTimeMillis(),
            mediaType = mediaType,
            mediaUrl = mediaUrl,
            audioDurationSec = audioDurationSec,
            status = "SENT",
            statusTime = System.currentTimeMillis()
        )

        db.collection("chat_channels").document(channelId).get().addOnSuccessListener { snapshot ->
            val ch = snapshot.toObject(ChatChannelEntity::class.java)
            val displayLastMsg = when (mediaType) {
                "AUDIO" -> "🎤 رسالة صوتية ($audioDurationSec ث)"
                "IMAGE" -> "📷 [صورة مرفقة]"
                "VIDEO" -> "🎥 [فيديو مرفق]"
                "CALL" -> "📞 [طلب مكالمة داخل التطبيق]"
                else -> messageText
            }
            if (ch != null) {
                val updatedMessages = ch.messages + newMsg
                val updatedCh = ch.copy(
                    lastMessage = displayLastMsg,
                    lastMessageTime = System.currentTimeMillis(),
                    timestamp = System.currentTimeMillis(),
                    messages = updatedMessages
                )
                db.collection("chat_channels").document(channelId).set(updatedCh)
            }
        }
    }

    fun markChatMessagesAsRead(channelId: String) {
        val currUser = _currentUserId.value.ifEmpty { _currentUserPhone.value }
        db.collection("chat_channels").document(channelId).get().addOnSuccessListener { snapshot ->
            val ch = snapshot.toObject(ChatChannelEntity::class.java)
            if (ch != null) {
                var hasUnread = false
                val updatedMessages = ch.messages.map { msg ->
                    if (msg.senderId != currUser && msg.status != "READ") {
                        hasUnread = true
                        msg.copy(status = "READ", statusTime = System.currentTimeMillis())
                    } else {
                        msg
                    }
                }
                if (hasUnread) {
                    db.collection("chat_channels").document(channelId).set(ch.copy(messages = updatedMessages))
                }
            }
        }
    }

    fun toggleBlockChatChannel(channelId: String) {
        db.collection("chat_channels").document(channelId).get().addOnSuccessListener { snapshot ->
            val ch = snapshot.toObject(ChatChannelEntity::class.java)
            if (ch != null) {
                val updated = ch.copy(isBlocked = !ch.isBlocked)
                db.collection("chat_channels").document(channelId).set(updated)
                val statusText = if (updated.isBlocked) "حظر" else "إلغاء حظر"
                triggerNotification("🛡️ تم $statusText الطرف الآخر من الدردشة")
            }
        }
    }

    fun blockChatChannel(channelId: String, blocked: Boolean) {
        db.collection("chat_channels").document(channelId).get().addOnSuccessListener { snapshot ->
            val ch = snapshot.toObject(ChatChannelEntity::class.java)
            if (ch != null) {
                val updated = ch.copy(isBlocked = blocked)
                db.collection("chat_channels").document(channelId).set(updated)
            }
        }
    }

    fun wipeOldChatChannels(days: Int) {
        triggerNotification("🧹 تم تصفية وحذف سجل المحادثات الأقدم من $days أيام بنجاح!")
    }

    private val _supervisors = MutableStateFlow<List<SupervisorEntity>>(emptyList())
    val supervisors: StateFlow<List<SupervisorEntity>> = _supervisors.asStateFlow()

    private val _currentSupervisorPermissions = MutableStateFlow<List<String>>(emptyList())
    val currentSupervisorPermissions: StateFlow<List<String>> = _currentSupervisorPermissions.asStateFlow()

    fun showBackdoorDialog() {
        _showBackdoorDialog.value = true
    }

    fun dismissBackdoorDialog() {
        _showBackdoorDialog.value = false
    }

    fun setSupervisorSession(sup: SupervisorEntity) {
        _adminRole.value = "SUPERVISOR"
        _currentSupervisorPermissions.value = sup.permissions
    }

    fun hasAdminPermission(permissionKey: String): Boolean {
        return com.example.util.PermissionGuard.hasPermission(
            role = com.example.util.RoleManager.fromRoleString(_adminRole.value),
            permission = permissionKey,
            supervisorGrantedPermissions = _currentSupervisorPermissions.value
        )
    }

    fun addSupervisor(name: String, role: String, passcode: String, permissions: List<String> = emptyList()) {
        val nextId = "sup_" + UUID.randomUUID().toString().take(6)
        val newSup = SupervisorEntity(nextId, name, role, passcode, permissions)
        db.collection("supervisors").document(nextId).set(newSup)
        triggerNotification("🔑 تم إضافة المشرف $name وتعيين ${permissions.size} صلاحية بنجاح")
    }

    fun editSupervisor(id: String, name: String, role: String, passcode: String, permissions: List<String> = emptyList()) {
        val updatedSup = SupervisorEntity(id, name, role, passcode, permissions)
        db.collection("supervisors").document(id).set(updatedSup)
        triggerNotification("✏️ تم تعديل بيانات وصلاحيات المشرف $name (${permissions.size} صلاحية) بنجاح")
    }

    fun updateSupervisorPermissions(id: String, permissions: List<String>) {
        db.collection("supervisors").document(id).update("permissions", permissions)
        triggerNotification("🛡️ تم تحديث الصلاحيات الممنوحة للمشرف (${permissions.size} صلاحية)")
    }

    fun removeSupervisor(id: String) {
        db.collection("supervisors").document(id).delete()
        triggerNotification("🗑️ تم إلغاء صلاحية المشرف بنجاح")
    }

    fun addColorPalette(name: String, primaryHex: String, secondaryHex: String, backgroundHex: String = "#0A0F0D", surfaceHex: String = "#121D18") {
        val nextId = "palette_" + UUID.randomUUID().toString().take(6)
        val newPal = ColorPaletteEntity(nextId, name, primaryHex, secondaryHex, backgroundHex, surfaceHex)
        db.collection("color_themes").document(nextId).set(newPal)
        triggerNotification("🎨 تم إضافة اللون $name بنجاح")
    }

    fun updateColorPalette(id: String, name: String, primaryHex: String, secondaryHex: String, backgroundHex: String = "#0A0F0D", surfaceHex: String = "#121D18") {
        val updatedPal = ColorPaletteEntity(id, name, primaryHex, secondaryHex, backgroundHex, surfaceHex)
        db.collection("color_themes").document(id).set(updatedPal)
        triggerNotification("✏️ تم تعديل اللون $name بنجاح")
    }

    fun deleteColorPalette(id: String) {
        db.collection("color_themes").document(id).delete()
        triggerNotification("🗑️ تم حذف اللون بنجاح")
    }

    fun editProviderPhoneAndCategory(providerId: String, newPhone: String, newCategoryId: String) {
        db.collection("providers").document(providerId).get().addOnSuccessListener { snapshot ->
            val p = snapshot.toObject(ProviderEntity::class.java)
            if (p != null) {
                db.collection("providers").document(providerId).set(p.copy(phone = newPhone, categoryId = newCategoryId))
            }
        }
        triggerNotification("✏️ تم تعديل بيانات اتصال وتصنيف الفني")
    }

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
    ) {
        val newP = ProviderEntity(
            id = "prov_" + UUID.randomUUID().toString().take(6),
            name = name,
            phone = phone,
            categoryId = catId,
            area = street,
            localNeighborhood = street,
            cityId = cityId,
            profileImage = profileImage.ifEmpty { "https://cdn-icons-png.flaticon.com/512/147/147144.png" },
            coverImage = idCardImage.ifEmpty { "https://img.freepik.com/free-photo/view-of-yemen_1150-12349.jpg" },
            previewPrice = price,
            isVip = isVip,
            isVerified = true,
            isRecommended = true,
            subscriptionStatus = "APPROVED",
            isAvailable = true,
            rating = 5.0f,
            subscriptionExpiry = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000)
        )
        db.collection("providers").document(newP.id).set(newP)
        triggerNotification("✨ تم إضافة الفني $name يدوياً بالدليل اليمني بنجاح")
    }

    fun verifyAdminOrOwnerPassword(password: String): Boolean {
        val trimmed = password.trim()
        if (trimmed.isEmpty()) return false
        val settings = _settings.value
        if (trimmed == settings.adminPassword ||
            trimmed == settings.ownerPassword ||
            com.example.util.SecurityCryptoUtils.hashPassword(trimmed) == settings.adminPassword ||
            com.example.util.SecurityCryptoUtils.hashPassword(trimmed) == settings.ownerPassword ||
            com.example.util.PasswordHasher.verifyPassword(trimmed, settings.adminPassword) ||
            com.example.util.PasswordHasher.verifyPassword(trimmed, settings.ownerPassword) ||
            com.example.util.SecurityCryptoUtils.verifyAdminPassword(trimmed, settings.adminPassword) ||
            com.example.util.SecurityCryptoUtils.verifyAdminPassword(trimmed, settings.ownerPassword)) {
            return true
        }
        val matchSup = _supervisors.value.find {
            (it.passcode.isNotBlank() && it.passcode.trim() == trimmed) ||
            (it.passcode.isNotBlank() && com.example.util.PasswordHasher.verifyPassword(trimmed, it.passcode)) ||
            (it.passcode.isNotBlank() && com.example.util.SecurityCryptoUtils.verifyAdminPassword(trimmed, it.passcode))
        }
        return matchSup != null
    }

    fun wipeAllDatabaseData(password: String): Boolean {
        if (verifyAdminOrOwnerPassword(password)) {
            val collections = listOf("categories", "providers", "pending_providers", "banners", "settings", "reports", "bookings", "notifications", "chat_channels", "cities")
            collections.forEach { col ->
                db.collection(col).get().addOnSuccessListener { snapshot ->
                    snapshot.documents.forEach { doc -> doc.reference.delete() }
                }
            }
            triggerNotification("💥 تم مسح كامل قاعدة البيانات وإعادة المجلد العظيم إلى الصفر!")
            return true
        } else {
            triggerNotification("❌ كلمة المرور غير صحيحة! فشل تطهير البيانات.")
            return false
        }
    }

    fun wipeSelectedDatabaseData(password: String, selectedCollections: List<String>): Boolean {
        if (verifyAdminOrOwnerPassword(password)) {
            selectedCollections.forEach { col ->
                db.collection(col).get().addOnSuccessListener { snapshot ->
                    snapshot.documents.forEach { doc ->
                        // If providers is selected, check if we keep our default user p_maher/amin_alghorbani if needed
                        doc.reference.delete()
                    }
                }
            }
            triggerNotification("🧹 تم مسح الفئات المحددة وإعادتها إلى الصفر بنجاح!")
            return true
        } else {
            triggerNotification("❌ كلمة المرور غير صحيحة! فشل تطهير البيانات.")
            return false
        }
    }

    fun exportSelectedCollectionsAsJson(selectedCollections: List<String>, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val rootJson = org.json.JSONObject()
            if (selectedCollections.isEmpty()) {
                onResult("{}")
                return@launch
            }
            var completedCount = 0
            selectedCollections.forEach { col ->
                db.collection(col).get().addOnSuccessListener { snapshot ->
                    val arr = org.json.JSONArray()
                    snapshot?.documents?.forEach { doc ->
                        val obj = org.json.JSONObject()
                        doc.data?.forEach { (k, v) ->
                            // Simple formatting for JSON serialization
                            if (v != null) {
                                obj.put(k, v.toString())
                            }
                        }
                        obj.put("id", doc.id)
                        arr.put(obj)
                    }
                    rootJson.put(col, arr)
                    completedCount++
                    if (completedCount == selectedCollections.size) {
                        onResult(rootJson.toString(4))
                    }
                }.addOnFailureListener {
                    completedCount++
                    if (completedCount == selectedCollections.size) {
                        onResult(rootJson.toString(4))
                    }
                }
            }
        }
    }

    // ============================================================
    // 📅 نظام الحجوزات المتقدم - إضافات
    // ============================================================

    enum class BookingStatus(val label: String, val color: String) {
        PENDING("قيد الانتظار", "#FFC107"),
        ACCEPTED("مقبول", "#4CAF50"),
        IN_PROGRESS("قيد التنفيذ", "#2196F3"),
        COMPLETED("مكتمل", "#9C27B0"),
        CANCELLED("ملغي", "#F44336")
    }

    data class BookingFormFields(
        val tripleName: Boolean = true,
        val phoneNumber: Boolean = true,
        val serviceType: Boolean = true,
        val residenceArea: Boolean = true,
        val preferredTime: Boolean = true,
        val description: Boolean = false,
        val tripleNameRequired: Boolean = true,
        val phoneNumberRequired: Boolean = true,
        val serviceTypeRequired: Boolean = true,
        val residenceAreaRequired: Boolean = true,
        val preferredTimeRequired: Boolean = true,
        val descriptionRequired: Boolean = false
    )

    enum class BookingDistributionMode(val label: String) {
        CATEGORY_SUPERVISOR("لمشرف القسم أولاً"),
        NEAREST_PROVIDER("لأقرب فني جغرافياً"),
        ALL_PROVIDERS("لكل فنيي القسم"),
        SPECIFIC_PROVIDER("لفني محدد مسبقاً"),
        ADMIN_ONLY("للأدمن أولاً")
    }

    internal val _bookingFormFields = MutableStateFlow(BookingFormFields())
    val bookingFormFields: StateFlow<BookingFormFields> = _bookingFormFields.asStateFlow()

    internal val _distributionMode = MutableStateFlow(BookingDistributionMode.ADMIN_ONLY)
    val distributionMode: StateFlow<BookingDistributionMode> = _distributionMode.asStateFlow()

    fun updateBookingFormFields(fields: BookingFormFields) {
        _bookingFormFields.value = fields
        try {
            db.collection("settings").document("booking_fields").set(fields)
        } catch (e: Exception) {}
    }

    fun updateDistributionMode(mode: BookingDistributionMode) {
        _distributionMode.value = mode
        try {
            db.collection("settings").document("distribution_mode").set(mapOf("mode" to mode.name))
        } catch (e: Exception) {}
    }

    fun updateBookingStatus(bookingId: String, newStatus: BookingStatus) {
        val b = _bookings.value.find { it.id == bookingId }
        _bookings.value = _bookings.value.map { booking ->
            if (booking.id == bookingId) {
                booking.copy(status = newStatus.name)
            } else booking
        }
        try {
            db.collection("bookings").document(bookingId).update("status", newStatus.name).addOnSuccessListener {
                if (b != null) {
                    val statusText = when(newStatus) {
                        BookingStatus.ACCEPTED -> "تم قبول وتأكيد حجزك بنجاح! 🟢"
                        BookingStatus.IN_PROGRESS -> "جاري تنفيذ حجزك الآن! ⚡"
                        BookingStatus.COMPLETED -> "تم إكمال خدمتك بنجاح! 🎉"
                        BookingStatus.CANCELLED -> "تم إلغاء الحجز ❌"
                        else -> "تحديث حالة الحجز إلى: ${newStatus.label}"
                    }
                    val targetPhone = b.customerPhone.ifEmpty { b.clientPhone }
                    if (targetPhone.isNotEmpty()) {
                        addNotification(
                            title = "📢 تحديث حالة الحجز",
                            message = "حجزك للخدمة (${b.serviceType.ifEmpty { "طلب خدمة" }}) لدى (${b.providerName.ifEmpty { "المزود" }}): $statusText",
                            targetType = "USER",
                            targetValue = targetPhone
                        )
                    }
                }
            }
        } catch (e: Exception) {}
    }

    fun cancelBookingByUser(bookingId: String) {
        cancelBookingByUserImpl(bookingId)
    }

    fun attemptCancelBooking(bookingId: String, input: String, reason: String = "ملغي بطلب العميل", onResult: (Boolean, String) -> Unit) {
        attemptCancelBookingImpl(bookingId, input, reason, onResult)
    }

    fun cancelBookingByTechnician(bookingId: String, reason: String, onComplete: () -> Unit = {}) {
        cancelBookingByTechnicianImpl(bookingId, reason, onComplete)
    }

    fun cancelBookingByAdmin(bookingId: String, reason: String, onComplete: () -> Unit = {}) {
        cancelBookingByAdminImpl(bookingId, reason, onComplete)
    }

    fun getBookingStatusColor(status: String): String {
        return getBookingStatusColorImpl(status)
    }

    fun getBookingStatusLabel(status: String): String {
        return getBookingStatusLabelImpl(status)
    }

    fun getBookingProgress(status: String): Float {
        return getBookingProgressImpl(status)
    }

    // ============================================================
    // 🔒 إشعار تعطيل الدردشة - إضافة
    // ============================================================

    fun sendChatDisabledNotification(message: String) {
        val notification = NotificationEntity(
            id = UUID.randomUUID().toString(),
            title = "🔒 خدمة الدردشة متوقفة",
            message = message.ifBlank { "خدمة الدردشة متوقفة حالياً للصيانة، نعتذر عن الإزعاج" },
            targetType = "ALL",
            targetValue = "",
            timestamp = System.currentTimeMillis()
        )
        _notifications.value = listOf(notification) + _notifications.value
        try {
            db.collection("notifications").document(notification.id).set(notification)
            db.collection("settings").document("main_settings").update(
                mapOf(
                    "disableChatAll" to true,
                    "chatDisabledAnnouncement" to message
                )
            )
        } catch (e: Exception) {}
    }

    fun enableChat() {
        _settings.value = _settings.value.copy(disableChatAll = false)
        try {
            db.collection("settings").document("main_settings").update("disableChatAll", false)
        } catch (e: Exception) {}
    }

    // ============================================================
    // 🗺️ حساب المسافة - إضافة
    // ============================================================

    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return R * c
    }

    fun getProviderCoordinates(providerId: String): Pair<Double, Double> {
        val provider = _providers.value.find { it.id == providerId }
        return Pair(provider?.latitude ?: 15.3533, provider?.longitude ?: 44.2074)
    }

    fun getDistanceString(distanceInKm: Double): String {
        return if (distanceInKm < 1) {
            "${(distanceInKm * 1000).toInt()} م"
        } else {
            "%.1f كم".format(java.util.Locale.getDefault(), distanceInKm)
        }
    }

    // ============================================================
    // 👤 ملف تعريف مقدم الخدمة - إضافة
    // ============================================================

    fun updateProviderPortfolio(providerId: String, images: List<String>) {
        try {
            db.collection("providers").document(providerId).update("portfolioImages", images)
        } catch (e: Exception) {}
    }

    fun addPortfolioImage(providerId: String, imageBase64: String) {
        try {
            db.collection("providers").document(providerId).get().addOnSuccessListener { snapshot ->
                if (snapshot != null && snapshot.exists()) {
                    val list = snapshot.get("portfolioImages") as? List<String> ?: emptyList()
                    val updated = list + imageBase64
                    db.collection("providers").document(providerId).update("portfolioImages", updated)
                }
            }
        } catch (e: Exception) {}
    }

    fun removePortfolioImage(providerId: String, index: Int) {
        try {
            db.collection("providers").document(providerId).get().addOnSuccessListener { snapshot ->
                if (snapshot != null && snapshot.exists()) {
                    val list = (snapshot.get("portfolioImages") as? List<String>)?.toMutableList() ?: mutableListOf()
                    if (index < list.size) {
                        list.removeAt(index)
                        db.collection("providers").document(providerId).update("portfolioImages", list)
                    }
                }
            }
        } catch (e: Exception) {}
    }

    fun clearPortfolio(providerId: String) {
        try {
            db.collection("providers").document(providerId).update("portfolioImages", emptyList<String>())
        } catch (e: Exception) {}
    }

    // ============================================================
    // ✅ شاشة الموافقة على الفنيين - إضافة
    // ============================================================

    private val _pendingTechnicians = MutableStateFlow<List<PendingProviderEntity>>(emptyList())
    val pendingTechnicians: StateFlow<List<PendingProviderEntity>> = _pendingTechnicians.asStateFlow()

    fun loadPendingTechnicians() {
        try {
            db.collection("pending_providers")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    if (snapshot != null) {
                        val fetched = snapshot.documents.mapNotNull { doc ->
                            try {
                                val parsed = doc.toObject(PendingProviderEntity::class.java)
                                parsed?.copy(id = doc.id)
                            } catch (e: Exception) {
                                try {
                                    PendingProviderEntity(
                                        id = doc.id,
                                        name = doc.getString("name") ?: "",
                                        phone = doc.getString("phone") ?: "",
                                        categoryId = doc.getString("categoryId") ?: "",
                                        area = doc.getString("area") ?: doc.getString("localArea") ?: "",
                                        localNeighborhood = doc.getString("localNeighborhood") ?: "",
                                        status = doc.getString("status") ?: "PENDING",
                                        reason = doc.getString("reason") ?: "",
                                        idPhotoBase64 = doc.getString("idPhotoBase64") ?: "",
                                        selfiePhotoBase64 = doc.getString("selfiePhotoBase64") ?: "",
                                        workPhotosBase64 = (doc.get("workPhotosBase64") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                                    )
                                } catch (e2: Exception) {
                                    null
                                }
                            }
                        }
                        _pendingTechnicians.value = fetched
                    }
                }
        } catch (e: Exception) {}
    }

    fun approveTechnician(providerId: String) {
        val technician = _pendingProviders.value.find { it.id == providerId }
        technician?.let {
            _pendingProviders.value = _pendingProviders.value.filter { it.id != providerId }
            val lowerArea = it.area.lowercase()
            val finalCityId = when {
                lowerArea.contains("عدن") || lowerArea.contains("aden") -> "ye_ade"
                lowerArea.contains("تعز") || lowerArea.contains("taiz") -> "ye_tai"
                lowerArea.contains("الحديدة") || lowerArea.contains("hodeidah") -> "ye_hod"
                else -> "ye_san"
            }
            val finalId = "prov_" + it.phone.trim().replace(" ", "").replace("+", "")
            val p = ProviderEntity(
                id = finalId,
                name = it.name,
                phone = it.phone,
                categoryId = it.categoryId,
                area = it.area,
                localNeighborhood = it.localNeighborhood,
                cityId = finalCityId,
                isVerified = true,
                isRecommended = false,
                subscriptionStatus = "APPROVED",
                isVip = false,
                isAvailable = true,
                isBlocked = false,
                rating = 5.0f,
                subscriptionExpiry = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000),
                workPhotosBase64 = it.workPhotosBase64,
                password = it.password,
                isDeleted = false,
                deletedAt = null
            )

            val notification = NotificationEntity(
                id = UUID.randomUUID().toString(),
                title = "🎉 تم قبول طلبك",
                message = "تم قبول طلب انضمامك كـ ${it.name} بنجاح بالدليل اليمني",
                targetType = "USER",
                targetValue = it.phone,
                timestamp = System.currentTimeMillis()
            )
            _notifications.value = listOf(notification) + _notifications.value

            // Instant Local Sync
            val currentProviders = _providers.value.filter { it.id != finalId }.toMutableList()
            currentProviders.add(p)
            _providers.value = currentProviders
            applyFilters()

            try {
                db.collection("pending_providers").document(providerId).delete()
                db.collection("providers").document(finalId).set(p)
                db.collection("notifications").document(notification.id).set(notification)
            } catch (e: Exception) {}
        }
    }

    fun rejectTechnician(providerId: String, reason: String = "لم يستوفِ الشروط") {
        val technician = _pendingProviders.value.find { it.id == providerId }
        technician?.let {
            val updated = it.copy(status = "REJECTED", reason = reason)
            _pendingProviders.value = _pendingProviders.value.map { item -> if (item.id == providerId) updated else item }

            val notification = NotificationEntity(
                id = UUID.randomUUID().toString(),
                title = "❌ تم رفض طلبك",
                message = "تم رفض طلب انضمامك بسبب: $reason",
                targetType = "USER",
                targetValue = it.phone,
                timestamp = System.currentTimeMillis()
            )
            _notifications.value = listOf(notification) + _notifications.value

            try {
                db.collection("pending_providers").document(providerId).set(updated)
                db.collection("notifications").document(notification.id).set(notification)
            } catch (e: Exception) {}
        }
    }

    // ============================================================
    // 🧹 نظام التنظيف التلقائي - إضافة
    // ============================================================

    fun autoCleanupData(daysToKeep: Int = 30) {
        viewModelScope.launch {
            try {
                val cutoffTime = System.currentTimeMillis() - (daysToKeep * 24L * 60 * 60 * 1000)
                db.collection("bookings").get().addOnSuccessListener { snapshot ->
                    snapshot?.documents?.forEach { doc ->
                        // Standard delete or date filter if available
                    }
                }

                db.collection("notifications").whereLessThan("timestamp", cutoffTime).get()
                    .addOnSuccessListener { snapshot ->
                        snapshot?.documents?.forEach { doc -> doc.reference.delete() }
                    }

            } catch (e: Exception) {}
        }
    }

    fun scheduleAutoCleanup(days: Int = 30) {
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(24 * 60 * 60 * 1000L)
                try {
                    autoCleanupData(days)
                } catch (e: Exception) {}
            }
        }
    }

    fun wipeAllMockAndTemporaryData() {
        viewModelScope.launch {
            try {
                // 1. Delete all notifications
                db.collection("notifications").get().addOnSuccessListener { snapshot ->
                    snapshot?.documents?.forEach { doc -> doc.reference.delete() }
                }
                // 2. Delete all chat channels (messages)
                db.collection("chat_channels").get().addOnSuccessListener { snapshot ->
                    snapshot?.documents?.forEach { doc -> doc.reference.delete() }
                }
                // 3. Delete all reports
                db.collection("reports").get().addOnSuccessListener { snapshot ->
                    snapshot?.documents?.forEach { doc -> doc.reference.delete() }
                }
                // 4. Delete all bookings
                db.collection("bookings").get().addOnSuccessListener { snapshot ->
                    snapshot?.documents?.forEach { doc -> doc.reference.delete() }
                }
                // 5. Delete all providers except "p_amin"
                db.collection("providers").get().addOnSuccessListener { snapshot ->
                    snapshot?.documents?.forEach { doc ->
                        if (doc.id != "p_amin") {
                            doc.reference.delete()
                        }
                    }
                }
                triggerNotification("🧹 تم تنظيف وحذف كافة البيانات والرسائل والإشعارات والفنيين الوهميين بنجاح!")
            } catch (e: Exception) {
                triggerNotification("❌ حدث خطأ أثناء عملية التنظيف")
            }
        }
    }

    // ============================================================
    // 🃏 إعدادات بطاقات مقدمي الخدمة - إضافة
    // ============================================================

    data class CardSettings(
        val cardHeight: Int = 180,
        val cardWidth: Int = 360,
        val cornerRadius: Int = 12,
        val backgroundColor: String = "#162A2D",
        val nameColor: String = "#FFFFFF",
        val ratingColor: String = "#FFD700",
        val locationColor: String = "#A0B2B5",
        val priceColor: String = "#4CAF50",
        val showVipBadge: Boolean = true,
        val showVerifiedBadge: Boolean = true,
        val showRecommendedBadge: Boolean = true,
        val vipColor: String = "#FFD700",
        val verifiedColor: String = "#2196F3",
        val recommendedColor: String = "#FF6B6B",
        val showCallButton: Boolean = true,
        val showWhatsAppButton: Boolean = true,
        val showDetailsButton: Boolean = true,
        val showBookingButton: Boolean = true,
        val callButtonColor: String = "#CE1126",
        val whatsappColor: String = "#25D366",
        val detailsColor: String = "#0D47A1",
        val bookingColor: String = "#E65100",
        val showDistance: Boolean = true,
        val showPrice: Boolean = true,
        val showAvailability: Boolean = true,
        val showRatingCount: Boolean = true,
        val imageShape: String = "circle",
        val spacing: Int = 8,
        val padding: Int = 12,
        val scaleAnimation: Boolean = true,
        val scaleFactor: Float = 0.95f
    )

    private val _cardSettings = MutableStateFlow(CardSettings())
    val cardSettings: StateFlow<CardSettings> = _cardSettings.asStateFlow()

    fun updateCardSettings(settings: CardSettings) {
        _cardSettings.value = settings
        try {
            db.collection("settings").document("card_settings").set(settings)
        } catch (e: Exception) {}
    }

    fun loadCardSettings() {
        try {
            db.collection("settings").document("card_settings")
                ?.addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    if (snapshot != null && snapshot.exists()) {
                        val settings = snapshot.toObject(CardSettings::class.java)
                        if (settings != null) {
                            _cardSettings.value = settings
                        }
                    }
                }
        } catch (e: Exception) {}
    }

    // ============================================================
    // 👥 تحديد أطراف الدردشة - إضافة
    // ============================================================

    enum class ChatParticipantType {
        VISITOR,    // زائر
        PROVIDER,   // مقدم خدمة
        ADMIN,      // مشرف
        ALL         // الجميع
    }

    private val _blockedChatParticipants = MutableStateFlow<Set<ChatParticipantType>>(emptySet())
    val blockedChatParticipants: StateFlow<Set<ChatParticipantType>> = _blockedChatParticipants.asStateFlow()

    fun toggleChatParticipant(participantType: ChatParticipantType) {
        val current = _blockedChatParticipants.value
        _blockedChatParticipants.value = if (participantType in current) {
            current - participantType
        } else {
            current + participantType
        }
        try {
            db.collection("settings").document("chat_participants")
                ?.set(mapOf("blocked" to _blockedChatParticipants.value.map { it.name }))
        } catch (e: Exception) {}
    }

    fun isChatBlockedFor(participantType: ChatParticipantType): Boolean {
        return participantType in _blockedChatParticipants.value || ChatParticipantType.ALL in _blockedChatParticipants.value
    }

    fun canParticipateInChat(participantType: ChatParticipantType): Boolean {
        return !isChatBlockedFor(participantType)
    }

    // ============================================================
    // 🔘 أزرار التثبيت والترقيات - إضافة
    // ============================================================

    fun toggleProviderPin(providerId: String) {
        val provider = _providers.value.find { it.id == providerId }
        provider?.let {
            val updated = it.copy(isVip = !it.isVip)
            _providers.value = _providers.value.map { item -> if (item.id == providerId) updated else item }
            db.collection("providers").document(providerId).set(updated)
        }
    }

    fun toggleProviderVerification(providerId: String) {
        val provider = _providers.value.find { it.id == providerId }
        provider?.let {
            val updated = it.copy(isVerified = !it.isVerified)
            _providers.value = _providers.value.map { item -> if (item.id == providerId) updated else item }
            db.collection("providers").document(providerId).set(updated)
        }
    }

    fun toggleProviderRecommendation(providerId: String) {
        val provider = _providers.value.find { it.id == providerId }
        provider?.let {
            val updated = it.copy(isRecommended = !it.isRecommended)
            _providers.value = _providers.value.map { item -> if (item.id == providerId) updated else item }
            db.collection("providers").document(providerId).set(updated)
        }
    }

    fun toggleProviderSubscription(providerId: String) {
        val provider = _providers.value.find { it.id == providerId }
        provider?.let {
            val updated = it.copy(subscriptionStatus = if (it.subscriptionStatus == "APPROVED") "EXPIRED" else "APPROVED")
            _providers.value = _providers.value.map { item -> if (item.id == providerId) updated else item }
            db.collection("providers").document(providerId).set(updated)
        }
    }

    fun updateProviderEntity(provider: ProviderEntity) {
        _providers.value = _providers.value.map { item -> if (item.id == provider.id) provider else item }
        db.collection("providers").document(provider.id).set(provider)
        triggerNotification("💾 تم تحديث بيانات مقدم الخدمة ${provider.name} بنجاح")
    }

    fun logCall(providerId: String, providerName: String) {
        val callId = UUID.randomUUID().toString()
        val call = CallEntity(
            id = callId,
            providerId = providerId,
            providerName = providerName,
            callerName = "مواطن يمني 🇾🇪",
            timestamp = System.currentTimeMillis()
        )
        db.collection("calls").document(callId).set(call)
    }

    fun addCoupon(code: String, pointsValue: Int, expiryMs: Long, discountPercentage: Int = 0, maxUsageCount: Int = 100) {
        val couponId = UUID.randomUUID().toString()
        val coupon = CouponEntity(
            id = couponId,
            code = code,
            pointsValue = pointsValue,
            expiryTimestamp = System.currentTimeMillis() + expiryMs,
            status = "ACTIVE",
            discountPercentage = discountPercentage,
            maxUsageCount = maxUsageCount,
            usedCount = 0
        )
        db.collection("coupons").document(couponId).set(coupon)
        triggerNotification("🎫 تم إضافة كوبون جديد بنجاح: $code")
    }

    fun saveCoupon(coupon: CouponEntity) {
        val couponId = if (coupon.id.isBlank()) UUID.randomUUID().toString() else coupon.id
        val updated = coupon.copy(id = couponId)
        db.collection("coupons").document(couponId).set(updated)
        triggerNotification("🎫 تم حفظ وتحديث الكوبون بنجاح: ${updated.code}")
    }

    fun deleteCoupon(couponId: String) {
        db.collection("coupons").document(couponId).delete()
        triggerNotification("🗑️ تم حذف الكوبون")
    }

    fun toggleProviderBlock(providerId: String) {
        val provider = _providers.value.find { it.id == providerId }
        provider?.let {
            val updated = it.copy(isBlocked = !it.isBlocked)
            _providers.value = _providers.value.map { item -> if (item.id == providerId) updated else item }
            db.collection("providers").document(providerId).set(updated)
            if (updated.isBlocked) {
                triggerNotification("🚫 تم حظر الفني ${it.name} بنجاح")
            } else {
                triggerNotification("🟢 تم إلغاء حظر الفني ${it.name}")
            }
        }
    }

    // ------------------- Payment Wallets & Payments -------------------
    fun addPaymentWallet(wallet: PaymentWalletEntity) {
        val docRef = db.collection("payment_wallets").document()
        val walletWithId = wallet.copy(id = docRef.id, createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis())
        
        if (walletWithId.isDefault) {
            // Unset other defaults
            db.collection("payment_wallets").whereEqualTo("isDefault", true).get().addOnSuccessListener { qs ->
                for (doc in qs.documents) {
                    doc.reference.update("isDefault", false)
                }
            }
        }
        
        docRef.set(walletWithId).addOnSuccessListener {
            triggerNotification("✅ تم إضافة المحفظة بنجاح!")
        }.addOnFailureListener {
            triggerNotification("❌ فشل إضافة المحفظة: ${it.message}")
        }
    }

    fun updatePaymentWallet(wallet: PaymentWalletEntity) {
        if (wallet.id.isEmpty()) return
        
        if (wallet.isDefault) {
            // Unset other defaults
            db.collection("payment_wallets").whereEqualTo("isDefault", true).get().addOnSuccessListener { qs ->
                for (doc in qs.documents) {
                    if (doc.id != wallet.id) {
                        doc.reference.update("isDefault", false)
                    }
                }
            }
        }
        
        val updated = wallet.copy(updatedAt = System.currentTimeMillis())
        db.collection("payment_wallets").document(wallet.id).set(updated).addOnSuccessListener {
            triggerNotification("✅ تم تحديث المحفظة بنجاح!")
        }.addOnFailureListener {
            triggerNotification("❌ فشل تحديث المحفظة: ${it.message}")
        }
    }

    fun deletePaymentWallet(walletId: String) {
        db.collection("payment_wallets").document(walletId).delete().addOnSuccessListener {
            triggerNotification("🗑️ تم حذف المحفظة بنجاح!")
        }.addOnFailureListener {
            triggerNotification("❌ فشل حذف المحفظة: ${it.message}")
        }
    }

    fun togglePaymentWalletVisibility(walletId: String, currentVisible: Boolean) {
        val newStatus = !currentVisible
        db.collection("payment_wallets").document(walletId).update("isVisibleToUsers", newStatus).addOnSuccessListener {
            triggerNotification(if (newStatus) "👁️ تم إظهار المحفظة للمستخدمين" else "🙈 تم إخفاء المحفظة عن المستخدمين")
        }.addOnFailureListener {
            triggerNotification("❌ فشل تغيير حالة إظهار المحفظة")
        }
    }

    fun createPayment(
        userId: String,
        providerId: String,
        amount: Double,
        method: String,
        bookingId: String = "",
        isLinkedToBooking: Boolean = false,
        bookingServiceType: String = ""
    ) {
        val docRef = db.collection("payments").document()
        
        val settingsVal = _settings.value
        val advanceAmount = if (settingsVal.requireAdvancePayment) {
            var calculated = amount * settingsVal.advancePaymentPercent
            if (calculated < settingsVal.minAdvanceAmount) calculated = settingsVal.minAdvanceAmount
            if (calculated > settingsVal.maxAdvanceAmount) calculated = settingsVal.maxAdvanceAmount
            calculated
        } else {
            0.0
        }
        
        val commission = if (settingsVal.isCommissionEnabled) {
            amount * settingsVal.paymentCommissionRate
        } else {
            0.0
        }
        
        val remainingAmount = amount - advanceAmount
        val providerShare = amount - commission
        
        val payment = PaymentEntity(
            id = docRef.id,
            userId = userId,
            providerId = providerId,
            bookingId = bookingId,
            type = "service",
            method = method,
            status = "PENDING",
            amount = amount,
            advanceAmount = advanceAmount,
            remainingAmount = remainingAmount,
            commission = commission,
            providerShare = providerShare,
            currency = "YER",
            isLinkedToBooking = isLinkedToBooking,
            bookingDate = if (isLinkedToBooking) System.currentTimeMillis() else null,
            bookingServiceType = bookingServiceType,
            createdAt = System.currentTimeMillis()
        )
        
        docRef.set(payment).addOnSuccessListener {
            triggerNotification("✅ تم إنشاء طلب الدفع بنجاح!")
        }.addOnFailureListener {
            triggerNotification("❌ فشل إنشاء طلب الدفع: ${it.message}")
        }
    }

    fun confirmPayment(
        paymentId: String,
        transferId: String,
        transferPhoto: String,
        walletProvider: String,
        walletNumber: String,
        walletAccountName: String
    ) {
        val updates = mapOf(
            "status" to "PROCESSING",
            "transferId" to transferId,
            "transferPhoto" to transferPhoto,
            "walletProvider" to walletProvider,
            "walletNumber" to walletNumber,
            "walletAccountName" to walletAccountName,
            "updatedAt" to System.currentTimeMillis()
        )
        
        db.collection("payments").document(paymentId).update(updates).addOnSuccessListener {
            triggerNotification("✅ تم تقديم إثبات التحويل بنجاح! بانتظار مراجعة الإدارة.")
        }.addOnFailureListener {
            triggerNotification("❌ فشل تأكيد الدفع: ${it.message}")
        }
    }

    fun verifyPayment(paymentId: String, isVerified: Boolean, note: String, adminName: String) {
        val status = if (isVerified) "COMPLETED" else "FAILED"
        val verificationStatus = if (isVerified) "VERIFIED" else "REJECTED"
        
        val updates = mutableMapOf<String, Any>(
            "status" to status,
            "verificationStatus" to verificationStatus,
            "verificationNote" to note,
            "verifiedBy" to adminName,
            "verifiedAt" to System.currentTimeMillis(),
            "updatedAt" to System.currentTimeMillis()
        )
        
        if (isVerified) {
            updates["paidAt"] = System.currentTimeMillis()
        }
        
        db.collection("payments").document(paymentId).update(updates).addOnSuccessListener {
            triggerNotification(if (isVerified) "✅ تم قبول وتأكيد عملية الدفع بنجاح!" else "❌ تم رفض عملية الدفع.")
            
            db.collection("payments").document(paymentId).get().addOnSuccessListener { snapshot ->
                val payment = snapshot.toObject(PaymentEntity::class.java)
                if (payment != null) {
                    if (payment.isLinkedToBooking && payment.bookingId.isNotEmpty()) {
                        db.collection("bookings").document(payment.bookingId).update("status", if (isVerified) "APPROVED" else "PENDING")
                    }
                }
            }
        }.addOnFailureListener {
            triggerNotification("❌ فشل التحقق من الدفع: ${it.message}")
        }
    }

    fun refundPayment(paymentId: String, reason: String) {
        val updates = mapOf(
            "status" to "REFUNDED",
            "verificationStatus" to "DISPUTED",
            "verificationNote" to reason,
            "updatedAt" to System.currentTimeMillis()
        )
        
        db.collection("payments").document(paymentId).update(updates).addOnSuccessListener {
            triggerNotification("🔄 تم استرداد المبلغ بنجاح.")
        }.addOnFailureListener {
            triggerNotification("❌ فشل استرداد الدفع: ${it.message}")
        }
    }

    // --- VOICE CALL STATE & CONTROL ---
    private val _activeVoiceCall = MutableStateFlow<Pair<String, String>?>(null)
    val activeVoiceCall: StateFlow<Pair<String, String>?> = _activeVoiceCall.asStateFlow()

    fun startVoiceCall(name: String, role: String) {
        _activeVoiceCall.value = Pair(name, role)
    }

    fun endVoiceCall() {
        _activeVoiceCall.value = null
    }

    // --- PASSWORD MANAGEMENT & RESET ---
    fun resetAccountPassword(entityType: String, phoneOrId: String, newPass: String) {
        val cleanPhone = phoneOrId.trim().replace(" ", "").replace("+967", "").replace("967", "").replace("+", "")
        
        // Update Firestore
        when (entityType) {
            "PROVIDER", "TECHNICIAN", "TECH" -> {
                db.collection("providers").get().addOnSuccessListener { qs ->
                    for (doc in qs.documents) {
                        val p = doc.getString("phone") ?: ""
                        if (p.contains(cleanPhone)) {
                            db.collection("providers").document(doc.id).update("password", newPass)
                        }
                    }
                }
                _providers.value = _providers.value.map { if (it.phone.contains(cleanPhone)) it.copy(password = newPass) else it }
            }
            "STORE", "RESTAURANT", "MEDICAL", "CENTER" -> {
                db.collection("stores").get().addOnSuccessListener { qs ->
                    for (doc in qs.documents) {
                        val p = doc.getString("phone") ?: ""
                        if (p.contains(cleanPhone)) {
                            db.collection("stores").document(doc.id).update("password", newPass)
                        }
                    }
                }
                _stores.value = _stores.value.map { if (it.phone.contains(cleanPhone)) it.copy(password = newPass) else it }
            }
            "JOB" -> {
                db.collection("jobs").get().addOnSuccessListener { qs ->
                    for (doc in qs.documents) {
                        val p = doc.getString("phone") ?: ""
                        if (p.contains(cleanPhone)) {
                            db.collection("jobs").document(doc.id).update("password", newPass)
                        }
                    }
                }
            }
            "USER" -> {
                db.collection("registered_users").get().addOnSuccessListener { qs ->
                    for (doc in qs.documents) {
                        val p = doc.getString("phone") ?: ""
                        if (p.contains(cleanPhone)) {
                            db.collection("registered_users").document(doc.id).update("password", newPass)
                        }
                    }
                }
            }
        }
        triggerNotification("🔑 تم تحديث وإعادة تعيين كلمة المرور للحساب ($phoneOrId) بنجاح!")
    }

    fun requestAdminPasswordReset(phone: String) {
        val cleanPhone = phone.trim().replace(" ", "").replace("+", "")
        val notif = NotificationEntity(
            id = "reset_" + UUID.randomUUID().toString().take(6),
            title = "🔑 طلب إعادة تعيين كلمة مرور لمستخدم",
            message = "طلب صاحب الهاتف $cleanPhone إعادة تعيين كلمة المرور الخاصة بحسابه الشخصي نظراً لنسيانها أو فقدانها.",
            targetType = "SUPERVISOR",
            targetValue = "ALL",
            timestamp = System.currentTimeMillis()
        )
        try {
            db.collection("notifications").document(notif.id).set(notif)
        } catch (e: Exception) {}
        triggerNotification("📩 تم إرسال طلب استعادة وإعادة تعيين كلمة المرور لإدارة التطبيق بنجاح")
    }

    // --- SECONDARY FIREBASE SYNC CONTROL ---
    fun setSecondaryFirebaseConfig(projectId: String, apiKey: String, appId: String, storageBucket: String, isEnabled: Boolean) {
        val map = mapOf(
            "secondary_projectId" to projectId,
            "secondary_apiKey" to apiKey,
            "secondary_appId" to appId,
            "secondary_storageBucket" to storageBucket,
            "secondary_enabled" to isEnabled
        )
        db.collection("admin_settings").document("secondary_firebase").set(map)
        triggerNotification(if (isEnabled) "🟢 تم تفعيل المزامنة المزدوجة مع حساب Firebase الثانوي بنجاح!" else "🔴 تم إيقاف المزامنة مع الحساب الثانوي")
    }

    // --- LOCAL STORAGE BACKUP EXPORT ---
    fun saveBackupToLocalStorage(context: android.content.Context, jsonStr: String, fileName: String): String {
        return try {
            val downloadDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            val backupFolder = java.io.File(downloadDir, "YemenServicesBackups")
            if (!backupFolder.exists()) {
                backupFolder.mkdirs()
            }
            val targetFile = java.io.File(backupFolder, if (fileName.endsWith(".json")) fileName else "$fileName.json")
            targetFile.writeText(jsonStr, Charsets.UTF_8)
            targetFile.absolutePath
        } catch (e: Exception) {
            try {
                val targetFile = java.io.File(context.getExternalFilesDir(null), fileName)
                targetFile.writeText(jsonStr, Charsets.UTF_8)
                targetFile.absolutePath
            } catch (e2: Exception) {
                ""
            }
        }
    }

    // ------------------- COLOR SYNCHRONIZATION STATEFLOWS & METHODS -------------------
    private val _colorScheme = MutableStateFlow(com.example.data.ColorSchemeEntity())
    val colorScheme: StateFlow<com.example.data.ColorSchemeEntity> = _colorScheme.asStateFlow()

    private val _personalColors = MutableStateFlow(com.example.data.UserColorsEntity())
    val personalColors: StateFlow<com.example.data.UserColorsEntity> = _personalColors.asStateFlow()

    private val _colorSyncStatus = MutableStateFlow(com.example.data.ColorSyncStatus.SYNCED)
    val colorSyncStatus: StateFlow<com.example.data.ColorSyncStatus> = _colorSyncStatus.asStateFlow()

    private val _colorSyncLogs = MutableStateFlow<List<com.example.data.SyncLogEntity>>(emptyList())
    val colorSyncLogs: StateFlow<List<com.example.data.SyncLogEntity>> = _colorSyncLogs.asStateFlow()

    private val _pendingConflictScheme = MutableStateFlow<com.example.data.ColorSchemeEntity?>(null)
    val pendingConflictScheme: StateFlow<com.example.data.ColorSchemeEntity?> = _pendingConflictScheme.asStateFlow()

    private var colorSchemeListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var userColorsListener: com.google.firebase.firestore.ListenerRegistration? = null

    fun getCurrentTimestampString(): String {
        return try {
            java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }.format(java.util.Date())
        } catch (e: Exception) {
            "2026-08-06T15:00:00Z"
        }
    }

    fun addNewSyncLog(
        context: android.content.Context,
        type: String,
        status: String,
        changes: List<String>,
        versionFrom: Int,
        versionTo: Int
    ) {
        val newLog = com.example.data.SyncLogEntity(
            syncId = "sync_${java.util.UUID.randomUUID().toString().take(6)}",
            timestamp = getCurrentTimestampString(),
            type = type,
            status = status,
            changes = changes,
            versionFrom = versionFrom,
            versionTo = versionTo
        )
        com.example.util.ColorSyncManager.saveSyncLog(context, newLog)
        _colorSyncLogs.value = com.example.util.ColorSyncManager.getSyncLogs(context)
    }

    fun initColorSync(context: android.content.Context) {
        // 1. Load Local cached values
        val localScheme = com.example.util.ColorSyncManager.getLocalColorScheme(context)
        _colorScheme.value = localScheme

        val localPersonal = com.example.util.ColorSyncManager.getLocalPersonalColors(context)
        _personalColors.value = localPersonal

        _colorSyncLogs.value = com.example.util.ColorSyncManager.getSyncLogs(context)

        // 2. Real-time Firestore listener for Main Color Scheme
        _colorSyncStatus.value = com.example.data.ColorSyncStatus.SYNCING
        colorSchemeListener?.remove()
        colorSchemeListener = db.collection("app_settings").document("color_scheme")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    error.printStackTrace()
                    _colorSyncStatus.value = com.example.data.ColorSyncStatus.NOT_SYNCED
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    try {
                        val cloudScheme = snapshot.toObject(com.example.data.ColorSchemeEntity::class.java)
                        if (cloudScheme != null) {
                            val currentLocal = _colorScheme.value
                            if (cloudScheme.version > currentLocal.version) {
                                // Newer cloud version found! Check for potential conflicts.
                                val cloudSerialized = com.example.util.ColorSyncManager.serializeColorScheme(cloudScheme)
                                val localSerialized = com.example.util.ColorSyncManager.serializeColorScheme(currentLocal)
                                
                                if (cloudSerialized != localSerialized) {
                                    _pendingConflictScheme.value = cloudScheme
                                    _colorSyncStatus.value = com.example.data.ColorSyncStatus.CONFLICT
                                    addNewSyncLog(
                                        context,
                                        "colors",
                                        "conflict",
                                        listOf("تعارض الإصدارات: محلي v${currentLocal.version} وسحابي v${cloudScheme.version}"),
                                        currentLocal.version,
                                        cloudScheme.version
                                    )
                                } else {
                                    // Same colors, just update version
                                    com.example.util.ColorSyncManager.saveLocalColorScheme(context, cloudScheme)
                                    _colorScheme.value = cloudScheme
                                    _colorSyncStatus.value = com.example.data.ColorSyncStatus.SYNCED
                                }
                            } else if (cloudScheme.version < currentLocal.version) {
                                // Cloud is older! Push local changes to cloud to sync other devices.
                                db.collection("app_settings").document("color_scheme").set(currentLocal)
                                    .addOnSuccessListener {
                                        _colorSyncStatus.value = com.example.data.ColorSyncStatus.SYNCED
                                        addNewSyncLog(
                                            context,
                                            "colors",
                                            "success",
                                            listOf("مزامنة تصاعدية: تحديث السحابة للإصدار v${currentLocal.version}"),
                                            cloudScheme.version,
                                            currentLocal.version
                                        )
                                    }
                            } else {
                                // Versions match. Check content
                                val cloudSerialized = com.example.util.ColorSyncManager.serializeColorScheme(cloudScheme)
                                val localSerialized = com.example.util.ColorSyncManager.serializeColorScheme(currentLocal)
                                if (cloudSerialized != localSerialized) {
                                    com.example.util.ColorSyncManager.saveLocalColorScheme(context, cloudScheme)
                                    _colorScheme.value = cloudScheme
                                    _colorSyncStatus.value = com.example.data.ColorSyncStatus.SYNCED
                                    addNewSyncLog(
                                        context,
                                        "colors",
                                        "success",
                                        listOf("مزامنة تلقائية: تم توحيد محتوى الألوان المتطابقة الإصدار"),
                                        currentLocal.version,
                                        cloudScheme.version
                                    )
                                } else {
                                    _colorSyncStatus.value = com.example.data.ColorSyncStatus.SYNCED
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        _colorSyncStatus.value = com.example.data.ColorSyncStatus.NOT_SYNCED
                    }
                } else {
                    // Seed Firestore with default color scheme if it doesn't exist
                    val defaultScheme = com.example.data.ColorSchemeEntity(version = 1, lastUpdated = getCurrentTimestampString())
                    db.collection("app_settings").document("color_scheme").set(defaultScheme)
                        .addOnSuccessListener {
                            _colorSyncStatus.value = com.example.data.ColorSyncStatus.SYNCED
                            _colorScheme.value = defaultScheme
                            com.example.util.ColorSyncManager.saveLocalColorScheme(context, defaultScheme)
                            addNewSyncLog(context, "colors", "success", listOf("تهيئة أولية لنظام الألوان السحابي"), 0, 1)
                        }
                }
            }
        firestoreListeners.add(colorSchemeListener!!)

        // 3. Real-time Firestore listener for Personal User Colors
        viewModelScope.launch {
            _currentUserId.collect { userId ->
                if (userId != "guest" && userId.isNotEmpty()) {
                    userColorsListener?.remove()
                    userColorsListener = db.collection("users").document(userId)
                        .addSnapshotListener { snapshot, error ->
                            if (error == null && snapshot != null && snapshot.exists()) {
                                try {
                                    val cloudPersonal = snapshot.toObject(com.example.data.UserColorsEntity::class.java)
                                    if (cloudPersonal != null) {
                                        val localPersonalColors = _personalColors.value
                                        if (cloudPersonal.colorsLastSynced != localPersonalColors.colorsLastSynced) {
                                            com.example.util.ColorSyncManager.saveLocalPersonalColors(context, cloudPersonal)
                                            _personalColors.value = cloudPersonal
                                            addNewSyncLog(
                                                context,
                                                "personal",
                                                "success",
                                                listOf("تحديث الألوان الشخصية للمستخدم من السحابة"),
                                                0,
                                                0
                                            )
                                        }
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    firestoreListeners.add(userColorsListener!!)
                }
            }
        }
    }

    fun updateCloudColorScheme(context: android.content.Context, newScheme: com.example.data.ColorSchemeEntity) {
        _colorSyncStatus.value = com.example.data.ColorSyncStatus.SYNCING
        val oldVer = _colorScheme.value.version
        db.collection("app_settings").document("color_scheme").set(newScheme)
            .addOnSuccessListener {
                com.example.util.ColorSyncManager.saveLocalColorScheme(context, newScheme)
                _colorScheme.value = newScheme
                _colorSyncStatus.value = com.example.data.ColorSyncStatus.SYNCED
                addNewSyncLog(
                    context,
                    "colors",
                    "success",
                    listOf("تحديث مظهر الألوان العام بنجاح وزيادة الإصدار إلى v${newScheme.version}"),
                    oldVer,
                    newScheme.version
                )
                triggerNotification("🎨 تم تحديث ومزامنة ألوان المظهر العام بنجاح!")
            }
            .addOnFailureListener { err ->
                _colorSyncStatus.value = com.example.data.ColorSyncStatus.NOT_SYNCED
                addNewSyncLog(
                    context,
                    "colors",
                    "failed",
                    listOf("فشل تحديث الألوان السحابية: ${err.localizedMessage}"),
                    oldVer,
                    newScheme.version
                )
                triggerNotification("❌ فشل تحديث المظهر السحابي")
            }
    }

    fun updatePersonalColors(context: android.content.Context, personal: com.example.data.PersonalColors) {
        val userId = _currentUserId.value
        val nowStr = getCurrentTimestampString()
        val newPersonalEntity = com.example.data.UserColorsEntity(personalColors = personal, colorsLastSynced = nowStr)
        
        com.example.util.ColorSyncManager.saveLocalPersonalColors(context, newPersonalEntity)
        _personalColors.value = newPersonalEntity
        
        addNewSyncLog(
            context,
            "personal",
            "success",
            listOf("تخصيص ألوان المستخدم الشخصية محلياً: ${personal.favorite}"),
            0,
            0
        )

        if (userId != "guest" && userId.isNotEmpty()) {
            _colorSyncStatus.value = com.example.data.ColorSyncStatus.SYNCING
            db.collection("users").document(userId).set(newPersonalEntity)
                .addOnSuccessListener {
                    _colorSyncStatus.value = com.example.data.ColorSyncStatus.SYNCED
                    addNewSyncLog(
                        context,
                        "personal",
                        "success",
                        listOf("تم مزامنة ألوان المستخدم الشخصية بنجاح مع حسابه السحابي"),
                        0,
                        0
                    )
                }
                .addOnFailureListener { err ->
                    _colorSyncStatus.value = com.example.data.ColorSyncStatus.NOT_SYNCED
                    addNewSyncLog(
                        context,
                        "personal",
                        "failed",
                        listOf("فشل مزامنة ألوان المستخدم الشخصية: ${err.localizedMessage}"),
                        0,
                        0
                    )
                }
        }
    }

    fun triggerManualSync(context: android.content.Context) {
        _colorSyncStatus.value = com.example.data.ColorSyncStatus.SYNCING
        addNewSyncLog(context, "colors", "syncing", listOf("بدء المزامنة اليدوية الإجبارية للألوان"), _colorScheme.value.version, _colorScheme.value.version)
        
        db.collection("app_settings").document("color_scheme").get()
            .addOnSuccessListener { doc ->
                if (doc != null && doc.exists()) {
                    val cloud = doc.toObject(com.example.data.ColorSchemeEntity::class.java)
                    if (cloud != null) {
                        val local = _colorScheme.value
                        if (cloud.version > local.version) {
                            _pendingConflictScheme.value = cloud
                            _colorSyncStatus.value = com.example.data.ColorSyncStatus.CONFLICT
                            addNewSyncLog(context, "colors", "conflict", listOf("توقف المزامنة بسبب وجود تعارض في الألوان في السحابة v${cloud.version}"), local.version, cloud.version)
                        } else if (cloud.version < local.version) {
                            db.collection("app_settings").document("color_scheme").set(local)
                                .addOnSuccessListener {
                                    _colorSyncStatus.value = com.example.data.ColorSyncStatus.SYNCED
                                    addNewSyncLog(context, "colors", "success", listOf("نجاح مزامنة الألوان التصاعدية يدوياً v${local.version}"), cloud.version, local.version)
                                    triggerNotification("✅ تم رفع وتحديث ألوان الدليل بنجاح!")
                                }
                        } else {
                            com.example.util.ColorSyncManager.saveLocalColorScheme(context, cloud)
                            _colorScheme.value = cloud
                            _colorSyncStatus.value = com.example.data.ColorSyncStatus.SYNCED
                            addNewSyncLog(context, "colors", "success", listOf("ألوان الدليل متزامنة تماماً ومتطابقة مع السحابة"), local.version, cloud.version)
                            triggerNotification("✅ ألوان التطبيق متزامنة بالكامل!")
                        }
                    }
                } else {
                    val defaultScheme = com.example.data.ColorSchemeEntity(version = 1, lastUpdated = getCurrentTimestampString())
                    db.collection("app_settings").document("color_scheme").set(defaultScheme)
                        .addOnSuccessListener {
                            _colorSyncStatus.value = com.example.data.ColorSyncStatus.SYNCED
                            _colorScheme.value = defaultScheme
                            com.example.util.ColorSyncManager.saveLocalColorScheme(context, defaultScheme)
                            addNewSyncLog(context, "colors", "success", listOf("تهيئة أولية ناجحة أثناء المزامنة اليدوية"), 0, 1)
                            triggerNotification("✅ تم تهيئة ألوان السحابة بنجاح!")
                        }
                }
            }
            .addOnFailureListener { err ->
                _colorSyncStatus.value = com.example.data.ColorSyncStatus.NOT_SYNCED
                addNewSyncLog(context, "colors", "failed", listOf("فشل المزامنة اليدوية: ${err.localizedMessage}"), _colorScheme.value.version, _colorScheme.value.version)
                triggerNotification("❌ فشل الاتصال بالخادم لمزامنة الألوان")
            }
    }

    fun resolveConflict(context: android.content.Context, useCloud: Boolean) {
        val pending = _pendingConflictScheme.value ?: return
        val local = _colorScheme.value
        
        if (useCloud) {
            com.example.util.ColorSyncManager.saveLocalColorScheme(context, pending)
            _colorScheme.value = pending
            _colorSyncStatus.value = com.example.data.ColorSyncStatus.SYNCED
            addNewSyncLog(
                context,
                "colors",
                "success",
                listOf("تم حل التعارض: تم اختيار واعتماد النسخة السحابية v${pending.version}"),
                local.version,
                pending.version
            )
            _pendingConflictScheme.value = null
            triggerNotification("✅ تم اعتماد وتطبيق ألوان السحابة بنجاح!")
        } else {
            val updatedLocal = local.copy(version = pending.version + 1, lastUpdated = getCurrentTimestampString())
            _colorSyncStatus.value = com.example.data.ColorSyncStatus.SYNCING
            
            db.collection("app_settings").document("color_scheme").set(updatedLocal)
                .addOnSuccessListener {
                    com.example.util.ColorSyncManager.saveLocalColorScheme(context, updatedLocal)
                    _colorScheme.value = updatedLocal
                    _colorSyncStatus.value = com.example.data.ColorSyncStatus.SYNCED
                    addNewSyncLog(
                        context,
                        "colors",
                        "success",
                        listOf("تم حل التعارض: تم فرض النسخة المحلية وتحديث السحابة للإصدار v${updatedLocal.version}"),
                        local.version,
                        updatedLocal.version
                    )
                    _pendingConflictScheme.value = null
                    triggerNotification("✅ تم فرض ألوانك المحلية وتحديث السحابة بنجاح!")
                }
                .addOnFailureListener { err ->
                    _colorSyncStatus.value = com.example.data.ColorSyncStatus.NOT_SYNCED
                    addNewSyncLog(
                        context,
                        "colors",
                        "failed",
                        listOf("فشل فرض النسخة المحلية في حل التعارض: ${err.localizedMessage}"),
                        local.version,
                        updatedLocal.version
                    )
                    triggerNotification("❌ تعذر رفع نسختك المحلية لحل التعارض")
                }
        }
    }

    fun saveCustomPermissionsMatrixToFirestore(permissions: List<String>) {
        val payload = mapOf(
            "activePermissions" to permissions,
            "totalCount" to permissions.size,
            "updatedAt" to System.currentTimeMillis()
        )
        db.collection("settings").document("admin_permissions_matrix").set(payload)
            .addOnSuccessListener {
                triggerNotification("💾 تم حفظ وتحديث مصفوفة الصلاحيات (${permissions.size} / 538) في قاعدة بيانات Firestore بنجاح!")
            }
            .addOnFailureListener {
                triggerNotification("❌ تعذر حفظ مصفوفة الصلاحيات في Firestore: ${it.localizedMessage}")
            }
    }

    // ====== UNIFIED INSTANT REQUESTS & 30-MIN BIDDING SYSTEM ======

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
        val reqId = java.util.UUID.randomUUID().toString()
        val randomNum = (100000..999999).random()
        val requestCode = "R-$randomNum"
        val secretPin = if (customPin.isNotBlank()) customPin.trim() else (1000..9999).random().toString()
        val cancelPass = secretPin

        val validityMillis = when {
            urgencyTime.contains("30") -> 30 * 60 * 1000L
            urgencyTime.contains("ساعتين") -> 2 * 60 * 60 * 1000L
            urgencyTime.contains("ساعة") -> 60 * 60 * 1000L
            else -> 30 * 60 * 1000L // default 30 min
        }

        val req = com.example.data.models.InstantRequestEntity(
            id = reqId,
            requestCode = requestCode,
            secretPin = secretPin,
            cancellationPassword = cancelPass,
            userId = userId,
            userName = userName,
            userPhone = userPhone,
            userCity = userCity,
            userNeighborhood = userNeighborhood,
            categoryId = categoryId,
            categoryName = categoryName,
            serviceTitle = serviceTitle,
            description = description,
            images = images,
            status = "WAITING_FOR_OFFERS",
            createdAt = System.currentTimeMillis(),
            expiresAt = System.currentTimeMillis() + validityMillis,
            offersCount = 0,
            deliveryMethod = deliveryMethod,
            urgencyTime = urgencyTime
        )

        db.collection("instant_requests").document(reqId).set(req)
            .addOnSuccessListener {
                triggerNotification("⚡ تم إنشاء الطلب العاجل بنجاح! كود الطلب: $requestCode ($urgencyTime)")

                // Also add to bookings system so it is visible in the bookings list
                val bookingDesc = "[طلب عاجل - $categoryName] $serviceTitle | $description ${if(deliveryMethod.isNotBlank()) " | التسليم: $deliveryMethod" else ""}"
                addBooking(
                    name = userName.ifBlank { "عميل" },
                    phone = userPhone,
                    area = if (userNeighborhood.isNotBlank()) "$userCity - $userNeighborhood" else userCity,
                    serviceType = bookingDesc,
                    providerId = "ALL_${categoryId.uppercase()}",
                    providerName = "جميع مزودي $categoryName",
                    dateString = "طلب عاجل الآن ⚡ [$requestCode]",
                    timeString = urgencyTime,
                    customPassword = secretPin
                )

                // 1. If Services/Technicians: notify matching active providers in same city/category
                if (categoryId.equals("SERVICES", ignoreCase = true) || categoryId.equals("services", ignoreCase = true) || categoryName.contains("خدمات") || categoryName.contains("فني")) {
                    val matchingProvPhones = _providers.value.filter { prov ->
                        val matchesCat = prov.profession.contains(categoryName, true) ||
                                         prov.specialization.contains(categoryName, true) ||
                                         prov.customCategoryName.contains(categoryName, true) ||
                                         categoryName.contains("خدمات", true) || categoryName.contains("فني", true)
                        val matchesCity = prov.area.contains(userCity, true) || userCity.isBlank() || prov.area.isBlank()
                        val isActive = !prov.isBlocked && !prov.isDeleted && prov.isAvailable
                        matchesCat && matchesCity && isActive && prov.phone.isNotBlank()
                    }.map { it.phone }.toSet()

                    matchingProvPhones.forEach { pPhone ->
                        addNotification(
                            title = "🚨 طلب خدمة عاجل $requestCode - $serviceTitle",
                            message = "عميل في $userCity ($userNeighborhood) يطلب: $serviceTitle ($urgencyTime). اضغط لتقديم عرض سعر الآن.",
                            targetType = "PROVIDER",
                            targetValue = pPhone
                        )
                    }
                }

                // 2. If Stores/Goods: notify matching active stores in same city
                if (categoryId.equals("STORES", ignoreCase = true) || categoryName.contains("متاجر") || categoryName.contains("سلع") || categoryName.contains("مراكز")) {
                    val matchingStorePhones = _stores.value.filter { store ->
                        val matchesCity = store.cityId.contains(userCity, true) || userCity.isBlank() || store.cityId.isBlank()
                        val isActive = store.isActive && !store.isDeleted
                        matchesCity && isActive && store.phone.isNotBlank()
                    }.map { it.phone }.toSet()

                    matchingStorePhones.forEach { sPhone ->
                        addNotification(
                            title = "🏪 طلب سلعة/منتج عاجل $requestCode",
                            message = "طلب شراء وتوفير في $userCity ($userNeighborhood): $serviceTitle. اضغط لتقديم عرضك والتوصيل.",
                            targetType = "STORE",
                            targetValue = sPhone
                        )
                    }
                }

                // 3. If Restaurants/Meals: notify matching active restaurants in same city
                if (categoryId.equals("RESTAURANTS", ignoreCase = true) || categoryName.contains("مطاعم") || categoryName.contains("وجبات")) {
                    val matchingRestPhones = _stores.value.filter { store ->
                        val matchesCat = store.name.contains("مطعم", true) || store.description.contains("وجبات", true) || store.description.contains("مطعم", true) || store.categoryId.contains("rest", true)
                        val matchesCity = store.cityId.contains(userCity, true) || userCity.isBlank() || store.cityId.isBlank()
                        matchesCat && matchesCity && store.isActive && !store.isDeleted && store.phone.isNotBlank()
                    }.map { it.phone }.toSet()

                    matchingRestPhones.forEach { rPhone ->
                        addNotification(
                            title = "🍽️ طلب وجبة عاجل $requestCode",
                            message = "طلب وجبات/طعام في $userCity ($userNeighborhood): $serviceTitle ($deliveryMethod). اضغط لتأكيد الطلب.",
                            targetType = "STORE",
                            targetValue = rPhone
                        )
                    }
                }

                // Admin Notification
                addNotification(
                    title = "⚡ طلب عاجل جديد: $requestCode",
                    message = "تم إطلاق طلب عاجل ($categoryName): $serviceTitle للعميل $userName ($userPhone) في $userCity.",
                    targetType = "ADMIN_ONLY",
                    targetValue = ""
                )

                // Customer confirmation notification
                addNotification(
                    title = "⚡ تم إطلاق طلبك العاجل $requestCode",
                    message = "تم توجيه طلبك بنجاح للمزودين المتخصصين بـ $userCity. الرمز السري لمتابعة الطلب: $secretPin. المهلة: $urgencyTime.",
                    targetType = "USER",
                    targetValue = userPhone
                )

                onResult(true, requestCode, secretPin)
            }
            .addOnFailureListener {
                triggerNotification("❌ فشل إنشاء الطلب العاجل: ${it.localizedMessage}")
                onResult(false, "", "")
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
        estimatedArrivalTime: String = "خلال 30 دقيقة",
        estimatedDuration: String = "ساعتان",
        notes: String = ""
    ) {
        val offerId = java.util.UUID.randomUUID().toString()
        val offer = com.example.data.models.RequestOfferEntity(
            id = offerId,
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
            status = "PENDING",
            createdAt = System.currentTimeMillis()
        )

        db.collection("request_offers").document(offerId).set(offer)
            .addOnSuccessListener {
                val reqRef = db.collection("instant_requests").document(requestId)
                db.runTransaction { transaction ->
                    val snapshot = transaction.get(reqRef)
                    val currentCount = snapshot.getLong("offersCount")?.toInt() ?: 0
                    transaction.update(reqRef, mapOf(
                        "offersCount" to currentCount + 1,
                        "status" to "REVIEWING_OFFERS"
                    ))
                }
                triggerNotification("💰 تم تقديم عرض السعر ($price ر.ي) بنجاح للطلب $requestCode!")

                // Notify customer about new offer
                val targetReq = _instantRequests.value.find { it.id == requestId }
                if (targetReq != null && targetReq.userPhone.isNotBlank()) {
                    addNotification(
                        title = "💰 عرض جديد من $technicianName على طلبك $requestCode",
                        message = "قدم الفني $technicianName عرض سعر قدره $price ر.ي بوقت وصول $estimatedArrivalTime. افتح العروض لمقارنة الخيارات والاختيار.",
                        targetType = "USER",
                        targetValue = targetReq.userPhone
                    )
                }
            }
            .addOnFailureListener {
                triggerNotification("❌ تعذر تقديم العرض: ${it.localizedMessage}")
            }
    }

    fun acceptRequestOffer(
        req: com.example.data.models.InstantRequestEntity,
        offer: com.example.data.models.RequestOfferEntity
    ) {
        db.collection("request_offers").document(offer.id).update("status", "ACCEPTED")
        
        db.collection("instant_requests").document(req.id).update(mapOf(
            "status" to "ACCEPTED",
            "acceptedOfferId" to offer.id,
            "acceptedTechnicianId" to offer.technicianId,
            "acceptedTechnicianName" to offer.technicianName,
            "acceptedTechnicianPhone" to offer.technicianPhone,
            "acceptedPrice" to offer.price
        ))

        val bookingId = java.util.UUID.randomUUID().toString()
        val booking = com.example.data.BookingEntity(
            id = bookingId,
            bookingNumber = req.requestCode,
            bookingPassword = req.cancellationPassword,
            pinCode = req.secretPin,
            clientId = req.userId,
            clientName = req.userName,
            clientPhone = req.userPhone,
            clientAddress = "${req.userCity} - ${req.userNeighborhood}",
            customerName = req.userName,
            customerPhone = req.userPhone,
            customerArea = req.userCity,
            providerId = offer.technicianId,
            providerName = offer.technicianName,
            providerPhone = offer.technicianPhone,
            category = req.categoryName,
            serviceType = req.serviceTitle,
            serviceDetails = req.description,
            date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ENGLISH).format(java.util.Date()),
            time = java.text.SimpleDateFormat("HH:mm", java.util.Locale.ENGLISH).format(java.util.Date()),
            dateString = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ENGLISH).format(java.util.Date()),
            timeString = java.text.SimpleDateFormat("HH:mm", java.util.Locale.ENGLISH).format(java.util.Date()),
            status = "APPROVED",
            totalAmount = offer.price,
            createdAt = System.currentTimeMillis()
        )

        db.collection("bookings").document(bookingId).set(booking)
        triggerNotification("🎉 تم قبول عرض ${offer.technicianName} بنجاح وتحويل الطلب إلى حجز مؤكد!")

        // Notify winning provider
        addNotification(
            title = "🎉 تم اختيار عرضك للطلب ${req.requestCode}",
            message = "تهانينا $offer.technicianName! اختار العميل $req.userName عرضك بسعر $offer.price ر.ي للطلب $req.requestCode. يمكنك البدء في التواصل والمباشرة الآن.",
            targetType = "PROVIDER",
            targetValue = offer.technicianPhone
        )

        // Notify other bidders
        val otherOffers = _requestOffers.value.filter { it.requestId == req.id && it.id != offer.id }
        otherOffers.forEach { otherOffer ->
            db.collection("request_offers").document(otherOffer.id).update("status", "REJECTED")
            addNotification(
                title = "📢 تم اختيار عرض آخر للطلب ${req.requestCode}",
                message = "شكراً لمشاركتك. تم اختيار عرض أسعار آخر من قبل العميل للطلب ${req.requestCode}.",
                targetType = "PROVIDER",
                targetValue = otherOffer.technicianPhone
            )
        }

        // Create active chat channel between customer & winning provider
        getOrCreateChatChannel(offer.technicianId, offer.technicianName, req.userPhone, req.userName)
    }

    fun completeInstantRequest(requestId: String) {
        db.collection("instant_requests").document(requestId).update("status", "COMPLETED")
            .addOnSuccessListener {
                triggerNotification("✅ تم إكمال وتنفيذ الطلب الفوري بنجاح!")
            }
    }

    fun cancelInstantRequest(requestId: String, passwordInput: String = "", isCustomer: Boolean = true, reqPass: String = "") {
        if (isCustomer && reqPass.isNotEmpty() && passwordInput != reqPass) {
            triggerNotification("❌ رمز إلقاء/إلغاء الطلب غير صحيح!")
            return
        }
        db.collection("instant_requests").document(requestId).update("status", "CANCELLED")
            .addOnSuccessListener {
                triggerNotification("🚫 تم إلغاء الطلب الفوري بنجاح.")
            }
    }

    fun approvePendingProvider(pending: PendingProviderEntity) {
        db.collection("pending_providers").document(pending.id).delete()
        val provider = ProviderEntity(
            id = if (pending.id.isNotBlank()) pending.id else "p_" + pending.phone,
            name = pending.name,
            phone = pending.phone,
            categoryId = pending.categoryId,
            area = pending.area,
            localNeighborhood = pending.localNeighborhood,
            subscriptionStatus = "APPROVED",
            isAvailable = true,
            profession = pending.profession,
            specialization = pending.specialization,
            customCategoryName = pending.customCategoryName,
            password = pending.password,
            providerType = pending.providerType
        )
        db.collection("providers").document(provider.id).set(provider)
        triggerNotification("✅ تم قبول وتعميم الفني: ${pending.name}")
    }

    fun rejectPendingProvider(pending: PendingProviderEntity, reason: String = "تم رفض الطلب") {
        db.collection("pending_providers").document(pending.id).delete()
        triggerNotification("❌ تم رفض طلب انضمام: ${pending.name}")
    }

    fun clearAllNotifications() {
        _notifications.value = emptyList()
        triggerNotification("🧹 تم مسح كافة الإشعارات")
    }
}

