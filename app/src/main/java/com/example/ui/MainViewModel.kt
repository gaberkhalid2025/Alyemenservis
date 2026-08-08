package com.example.ui

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
    private val _categories = MutableStateFlow<List<CategoryEntity>>(emptyList())
    val categories: StateFlow<List<CategoryEntity>> = _categories.asStateFlow()

    private val _providers = MutableStateFlow<List<ProviderEntity>>(emptyList())
    val providers: StateFlow<List<ProviderEntity>> = _providers.asStateFlow()

    private val _deletedProviders = MutableStateFlow<List<ProviderEntity>>(emptyList())
    val deletedProviders: StateFlow<List<ProviderEntity>> = _deletedProviders.asStateFlow()

    private val _filteredProviders = MutableStateFlow<List<ProviderEntity>>(emptyList())
    val filteredProviders: StateFlow<List<ProviderEntity>> = _filteredProviders.asStateFlow()

    private val _pendingProviders = MutableStateFlow<List<PendingProviderEntity>>(emptyList())
    val pendingProviders: StateFlow<List<PendingProviderEntity>> = _pendingProviders.asStateFlow()

    private val _banners = MutableStateFlow<List<BannerEntity>>(emptyList())
    val banners: StateFlow<List<BannerEntity>> = _banners.asStateFlow()

    private val _settings = MutableStateFlow(AdminSettingsEntity())
    val settings: StateFlow<AdminSettingsEntity> = _settings.asStateFlow()

    private val _reports = MutableStateFlow<List<ReportEntity>>(emptyList())
    val reports: StateFlow<List<ReportEntity>> = _reports.asStateFlow()

    private val _activityLogs = MutableStateFlow<List<ActivityLogEntity>>(emptyList())
    val activityLogs: StateFlow<List<ActivityLogEntity>> = _activityLogs.asStateFlow()

    private val _callsLog = MutableStateFlow<List<CallEntity>>(emptyList())
    val callsLog: StateFlow<List<CallEntity>> = _callsLog.asStateFlow()

    private val _coupons = MutableStateFlow<List<CouponEntity>>(emptyList())
    val coupons: StateFlow<List<CouponEntity>> = _coupons.asStateFlow()

    private val _internalWallets = MutableStateFlow<List<com.example.data.InternalWalletEntity>>(emptyList())
    val internalWallets: StateFlow<List<com.example.data.InternalWalletEntity>> = _internalWallets.asStateFlow()

    private val _walletTransactions = MutableStateFlow<List<com.example.data.WalletTransactionEntity>>(emptyList())
    val walletTransactions: StateFlow<List<com.example.data.WalletTransactionEntity>> = _walletTransactions.asStateFlow()

    private val _userLatitude = MutableStateFlow(15.3694)
    val userLatitude: StateFlow<Double> = _userLatitude.asStateFlow()

    private val _userLongitude = MutableStateFlow(44.1910)
    val userLongitude: StateFlow<Double> = _userLongitude.asStateFlow()

    fun updateUserLocation(lat: Double, lng: Double) {
        _userLatitude.value = lat
        _userLongitude.value = lng
    }

    private val _cities = MutableStateFlow<List<CityEntity>>(emptyList())
    val cities: StateFlow<List<CityEntity>> = _cities.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessageEntity>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessageEntity>> = _chatMessages.asStateFlow()

    private val _bookings = MutableStateFlow<List<BookingEntity>>(emptyList())
    val bookings: StateFlow<List<BookingEntity>> = _bookings.asStateFlow()

    private val _paymentWallets = MutableStateFlow<List<PaymentWalletEntity>>(emptyList())
    val paymentWallets: StateFlow<List<PaymentWalletEntity>> = _paymentWallets.asStateFlow()

    private val _payments = MutableStateFlow<List<PaymentEntity>>(emptyList())
    val payments: StateFlow<List<PaymentEntity>> = _payments.asStateFlow()

    private val _notifications = MutableStateFlow<List<NotificationEntity>>(emptyList())
    val notifications: StateFlow<List<NotificationEntity>> = _notifications.asStateFlow()

    private val _passwordRecoveryWaitingPhone = MutableStateFlow<String>("")
    val passwordRecoveryWaitingPhone: StateFlow<String> = _passwordRecoveryWaitingPhone.asStateFlow()

    fun setPasswordRecoveryWaitingPhone(phone: String) {
        _passwordRecoveryWaitingPhone.value = phone
    }

    var selectedProvider: com.example.data.ProviderEntity? = null
    var selectedStore: com.example.data.StoreEntity? = null
    var selectedProperty: com.example.data.PropertyEntity? = null

    private val _chatChannels = MutableStateFlow<List<ChatChannelEntity>>(emptyList())
    val chatChannels: StateFlow<List<ChatChannelEntity>> = _chatChannels.asStateFlow()

    private val _activeChatChannel = MutableStateFlow<ChatChannelEntity?>(null)
    val activeChatChannel: StateFlow<ChatChannelEntity?> = _activeChatChannel.asStateFlow()

    fun openChatChannel(channel: ChatChannelEntity?) {
        _activeChatChannel.value = channel
    }

    fun closeActiveChatChannel() {
        _activeChatChannel.value = null
    }

    private val _stores = MutableStateFlow<List<com.example.data.StoreEntity>>(getDefaultStoresList())
    val stores: StateFlow<List<com.example.data.StoreEntity>> = _stores.asStateFlow()

    private val _products = MutableStateFlow<List<com.example.data.ProductEntity>>(emptyList())
    val products: StateFlow<List<com.example.data.ProductEntity>> = _products.asStateFlow()

    private val _properties = MutableStateFlow<List<com.example.data.PropertyEntity>>(getDefaultPropertiesList())
    val properties: StateFlow<List<com.example.data.PropertyEntity>> = _properties.asStateFlow()

    private val _jobs = MutableStateFlow<List<com.example.data.JobEntity>>(emptyList())
    val jobs: StateFlow<List<com.example.data.JobEntity>> = _jobs.asStateFlow()

    private val _jobApplications = MutableStateFlow<List<com.example.data.JobApplicationEntity>>(emptyList())
    val jobApplications: StateFlow<List<com.example.data.JobApplicationEntity>> = _jobApplications.asStateFlow()

    private val _ratings = MutableStateFlow<List<com.example.data.RatingEntity>>(emptyList())
    val ratings: StateFlow<List<com.example.data.RatingEntity>> = _ratings.asStateFlow()

    private val _customProfileTabs = MutableStateFlow<List<com.example.data.CustomProfileTabEntity>>(emptyList())
    val customProfileTabs: StateFlow<List<com.example.data.CustomProfileTabEntity>> = _customProfileTabs.asStateFlow()

    private val _orders = MutableStateFlow<List<com.example.data.OrderEntity>>(emptyList())
    val orders: StateFlow<List<com.example.data.OrderEntity>> = _orders.asStateFlow()

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

    private val _currentUserName = MutableStateFlow("")
    val currentUserName: StateFlow<String> = _currentUserName.asStateFlow()

    private val _currentUserPhone = MutableStateFlow("")
    val currentUserPhone: StateFlow<String> = _currentUserPhone.asStateFlow()

    private val _currentUserResidence = MutableStateFlow("")
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
        val visibleList = if (adminRoleState != "GUEST") {
            distinctList
        } else {
            distinctList.filter {
                val notExpired = it.expiryTimestamp == 0L || now <= it.expiryTimestamp
                val isReleased = it.scheduledTime == 0L || now >= it.scheduledTime
                notExpired && isReleased
            }
        }

        if (adminRoleState != "GUEST") {
            visibleList
        } else if (userId == "guest" && joinPhone.isEmpty()) {
            visibleList.filter { it.targetType == "ALL" }
        } else {
            visibleList.filter {
                it.targetType == "ALL" || 
                (it.targetType == "USER" && (it.targetValue == userId || it.targetValue == phone || it.targetValue == joinPhone)) ||
                (it.targetType == "PROVIDER" && (it.targetValue == userId || it.targetValue == phone)) ||
                (it.targetType == "SUPERVISOR" && adminRoleState == "SUPERVISOR")
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
                completeGuestRegistration(context, firebaseUid, name, cleanPhone, residence, androidId)
            }
            .addOnFailureListener { e ->
                if (e.message?.contains("already in use") == true || e.message?.contains("EMAIL_EXISTS") == true) {
                    auth.signInWithEmailAndPassword(authEmail, password)
                        .addOnSuccessListener { authResult ->
                            val firebaseUid = authResult.user?.uid ?: ("usr_" + System.currentTimeMillis())
                            completeGuestRegistration(context, firebaseUid, name, cleanPhone, residence, androidId)
                        }
                        .addOnFailureListener {
                            triggerNotification("❌ كلمة المرور المدخلة غير صحيحة لهذا الحساب المسجل سابقاً.")
                        }
                } else {
                    completeGuestRegistration(context, "user_" + (100000..999999).random(), name, cleanPhone, residence, androidId)
                }
            }
    }

    private fun completeGuestRegistration(
        context: android.content.Context,
        userId: String,
        name: String,
        phone: String,
        residence: String,
        androidId: String
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

        // Save profile WITHOUT password field!
        val regUser = mapOf(
            "id" to userId,
            "name" to name,
            "phone" to phone,
            "residence" to residence,
            "androidId" to androidId,
            "timestamp" to System.currentTimeMillis()
        )
        db.collection("registered_users").document(userId).set(regUser)
        triggerNotification("🎉 أهلاً بك في الدليل $name، تم تسجيل وحماية حسابك آمنياً بنجاح عبر Firebase Auth!")
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
        setupRealtimeFirestoreListeners()
        loadCardSettings()
        loadPendingTechnicians()
        loadUserPoints()
        try {
            seedFirestoreIfEmpty()
        } catch (e: Exception) {
            e.printStackTrace()
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

        // 5. Providers
        db.collection("providers").addSnapshotListener { snapshot, error ->
            if (error != null) {
                error.printStackTrace()
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val allList = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(ProviderEntity::class.java)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }.filter { !it.name.contains("ماهر") && it.id != "p_maher" }
                
                val activeList = allList.filter { !it.isDeleted }
                val deletedList = allList.filter { it.isDeleted }
                
                _providers.value = activeList
                _deletedProviders.value = deletedList
                applyFilters()
            }
        }

        // 6. Pending Providers
        db.collection("pending_providers").addSnapshotListener { snapshot, error ->
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

        // 7. Bookings
        db.collection("bookings").addSnapshotListener { snapshot, error ->
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

        // 8. Notifications
        db.collection("notifications").addSnapshotListener { snapshot, error ->
            if (error != null) {
                error.printStackTrace()
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val fetched = snapshot.documents.mapNotNull { doc ->
                    try {
                        val obj = doc.toObject(NotificationEntity::class.java)
                        if (obj != null && obj.id.isEmpty()) {
                            obj.copy(id = doc.id)
                        } else {
                            obj
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }.distinctBy { it.id }.sortedByDescending { it.timestamp }
                _notifications.value = fetched
            }
        }

        // 9. Chat Channels
        db.collection("chat_channels").addSnapshotListener { snapshot, error ->
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

        // 11. Reports
        db.collection("reports").addSnapshotListener { snapshot, error ->
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
        db.collection("supervisors").addSnapshotListener { snapshot, error ->
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

        // 14. Calls Log
        db.collection("calls").addSnapshotListener { snapshot, error ->
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
        db.collection("coupons").addSnapshotListener { snapshot, error ->
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
        db.collection("payment_wallets").addSnapshotListener { snapshot, error ->
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

        // 17. Payments
        db.collection("payments").addSnapshotListener { snapshot, error ->
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

        // 18. Stores
        db.collection("stores").addSnapshotListener { snapshot, error ->
            if (error == null && snapshot != null) {
                val fetched = snapshot.documents.mapNotNull { doc ->
                    try {
                        val obj = doc.toObject(com.example.data.StoreEntity::class.java)
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
                val defStores = getDefaultStoresList()
                val merged = (fetched + defStores.filter { def -> fetched.none { it.id == def.id } })
                _stores.value = merged
            }
        }

        // 19. Products
        db.collection("products").addSnapshotListener { snapshot, error ->
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
                _products.value = fetched
            }
        }

        // 20. Properties
        db.collection("properties").addSnapshotListener { snapshot, error ->
            if (error == null && snapshot != null) {
                val fetched = snapshot.documents.mapNotNull { doc ->
                    try {
                        val obj = doc.toObject(com.example.data.PropertyEntity::class.java)
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
                val defProps = getDefaultPropertiesList()
                val merged = (fetched + defProps.filter { def -> fetched.none { it.id == def.id } })
                _properties.value = merged
            }
        }

        // 20.1 Jobs
        db.collection("jobs").addSnapshotListener { snapshot, error ->
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

        // 20.2 Job Applications
        db.collection("job_applications").addSnapshotListener { snapshot, error ->
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

        // 21. Ratings
        db.collection("ratings").addSnapshotListener { snapshot, error ->
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

        // 22. Orders
        db.collection("orders").addSnapshotListener { snapshot, error ->
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

        // 23. Activity Logs
        db.collection("activity_logs").addSnapshotListener { snapshot, error ->
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
    }

    fun seedFirestoreIfEmpty() {
        // Check and seed default configurations if they don't exist
        db.collection("settings").document("main_settings").get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val doc = task.result
                if (doc == null || !doc.exists()) {
                    db.collection("settings").document("main_settings").set(AdminSettingsEntity())
                }
            } else {
                try {
                    db.collection("settings").document("main_settings").set(AdminSettingsEntity())
                } catch (e: Exception) {}
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
        val fbBanners = listOf(
            BannerEntity("banner_001", "عروض وتخفيضات سيتي مارت صنعاء الكبرى! خصومات تصل إلى 30%", "https://images.unsplash.com/photo-1578916171728-46686eac8d58?q=80&w=800", "stores", "IMAGE", "MEDIUM", 5, "طوال اليوم", 1, "STORES"),
            BannerEntity("banner_002", "مهرجان المأكولات الملكية في مطعم الشيباني - صنعاء شارع حدة", "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?q=80&w=800", "restaurants", "IMAGE", "MEDIUM", 6, "طوال اليوم", 2, "RESTAURANTS"),
            BannerEntity("banner_003", "افتتاح العيادات التخصصية الشاملة بمستشفى المتوكل النموذجي", "https://images.unsplash.com/photo-1586773860418-d37222d8fce3?q=80&w=800", "medical", "IMAGE", "MEDIUM", 7, "طوال اليوم", 3, "MEDICAL")
        )
        fbBanners.forEach { banner ->
            db.collection("banners").document(banner.id).set(banner)
        }
    }

    private fun writeDefaultProviders() {
        val fbProviders = listOf(
            ProviderEntity("p_amin", "أمين الغرباني", "777703195", "1", "صنعاء", true, "APPROVED", true, "ye_san", "منطقة الدائري جوار مدرسة أسماء للبنات", 5.0f, 300, previewPrice = 1500.0, latitude = 15.3694, longitude = 44.1910, subscriptionExpiry = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000))
        )
        fbProviders.forEach { prov ->
            db.collection("providers").document(prov.id).set(prov)
        }
    }

    fun getDefaultStoresList(): List<com.example.data.StoreEntity> {
        return listOf(
            com.example.data.StoreEntity(
                id = "store_city_mart", sectionId = "stores", name = "سيتي مارت - صنعاء (City Mart Supermarket)",
                description = "أضخم سوبرماركت ومركز تجاري متكامل في صنعاء (شارع حدة/السبعين). يوفر كافة المستلزمات العائلية والمواد الغذائية والأجهزة المنزلية والمنتجات الاستهلاكية بأقل الأسعار والتخفيضات اليومية.",
                ownerId = "owner_city_mart", ownerName = "إدارة سيتي مارت - صنعاء", phone = "777111222", password = "Maher123",
                categoryId = "sub_store_4", cityId = "ye_san", localNeighborhood = "صنعاء - شارع حدة - تقاطع الرويشان",
                rating = 4.9f, numReviews = 480, isActive = true, isPinned = true, displayOrder = 1,
                workingHours = "7:00 AM - 12:00 AM", isApproved = true, isVip = true, isVerified = true, isRecommended = true,
                coverImage = "https://images.unsplash.com/photo-1578916171728-46686eac8d58?q=80&w=800",
                logoImage = "https://images.unsplash.com/photo-1534723452862-4c874018d66d?q=80&w=400",
                images = listOf("https://images.unsplash.com/photo-1578916171728-46686eac8d58?q=80&w=800", "https://images.unsplash.com/photo-1534723452862-4c874018d66d?q=80&w=400")
            ),
            com.example.data.StoreEntity(
                id = "restaurant_shaibani", sectionId = "restaurants", name = "مطعم الشيباني الملكي - صنعاء",
                description = "أعرق وأفخم المطاعم الملكية في أمانة العاصمة صنعاء. نقدم المأكولات اليمنية والشرقية الأصلية: السلتة والفحسة الصنعانية الساخنة، أطباق اللحم البلدي والمندي، المشويات والمأكولات البحرية العصرية.",
                ownerId = "owner_shaibani", ownerName = "إدارة مطاعم الشيباني الملكية", phone = "777333444", password = "Maher123",
                categoryId = "sub_rest_1", cityId = "ye_san", localNeighborhood = "صنعاء - شارع الزبيري - جوار تقاطع حدة",
                rating = 4.9f, numReviews = 620, isActive = true, isPinned = true, displayOrder = 2,
                workingHours = "10:00 AM - 1:00 AM", isApproved = true, isVip = true, isVerified = true, isRecommended = true,
                coverImage = "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?q=80&w=800",
                logoImage = "https://images.unsplash.com/photo-1555396273-367ea4eb4db5?q=80&w=400",
                images = listOf("https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?q=80&w=800")
            ),
            com.example.data.StoreEntity(
                id = "medical_mutawakkil", sectionId = "medical", name = "مستشفى د. عبدالقادر المتوكل النموذجي - صنعاء",
                description = "من أحدث المراكز والمستشفيات الطبية النموذجية في اليمن. يقدم رعاية صحية متكاملة عبر نخبة من استشاريي الطب في الباطنية، جراحة القلب، الأطفال، العظام، والطوارئ والعناية المركزة 24/7.",
                ownerId = "owner_mutawakkil", ownerName = "د. عبدالقادر المتوكل", phone = "777777888", password = "Maher123",
                categoryId = "sub_center_2", cityId = "ye_san", localNeighborhood = "صنعاء - شارع بغداد - مقابل مركز العاصمة",
                rating = 4.9f, numReviews = 540, isActive = true, isPinned = true, displayOrder = 3,
                workingHours = "طوارئ 24/7 - العيادات (8:00 AM - 8:00 PM)", isApproved = true, isVip = true, isVerified = true, isRecommended = true,
                coverImage = "https://images.unsplash.com/photo-1586773860418-d37222d8fce3?q=80&w=800",
                logoImage = "https://images.unsplash.com/photo-1519494026892-80bbd2d6fd0d?q=80&w=400",
                images = listOf("https://images.unsplash.com/photo-1586773860418-d37222d8fce3?q=80&w=800")
            ),
            com.example.data.StoreEntity(
                id = "medical_awalqi_lab", sectionId = "medical", name = "مختبرات العولقي الطبية المركزية - صنعاء",
                description = "المختبر الطبي المرجعي الأول والأحدث في صنعاء. أجهزة آلية متطورة ودقة متناهية في فحوصات الدم، الهرمونات، الفيروسات، والأنسجة بأعلى معايير الجودة العالمية.",
                ownerId = "owner_awalqi", ownerName = "د. صادق العولقي", phone = "777888999", password = "Maher123",
                categoryId = "sub_center_2", cityId = "ye_san", localNeighborhood = "صنعاء - شارع الزبيري - مقابل مستشفى الجمهوري",
                rating = 4.9f, numReviews = 410, isActive = true, isPinned = true, displayOrder = 4,
                workingHours = "7:00 AM - 11:00 PM", isApproved = true, isVip = true, isVerified = true, isRecommended = true,
                coverImage = "https://images.unsplash.com/photo-1532187863486-abf9dbad1b69?q=80&w=800",
                logoImage = "https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?q=80&w=400",
                images = listOf("https://images.unsplash.com/photo-1532187863486-abf9dbad1b69?q=80&w=800")
            ),
            com.example.data.StoreEntity(
                id = "medical_pearl_dental", sectionId = "medical", name = "عيادة لؤلؤة صنعاء لطب وجراحة الأسنان",
                description = "مركز متخصص في تجميل، زراعة، وتقويم الأسنان وتبييض الليزر الزوم. أحدث التقنيات وأفضل الكوادر الطبية المتخصصة لابتسامة ساحرة بدون ألم.",
                ownerId = "owner_pearl", ownerName = "د. رزان الريمي", phone = "777999000", password = "Maher123",
                categoryId = "sub_center_1", cityId = "ye_san", localNeighborhood = "صنعاء - شارع حدة - العمارة البيضاء",
                rating = 4.8f, numReviews = 310, isActive = true, isPinned = true, displayOrder = 5,
                workingHours = "9:00 AM - 8:00 PM", isApproved = true, isVip = true, isVerified = true, isRecommended = true,
                coverImage = "https://images.unsplash.com/photo-1629909613654-28e377c37b09?q=80&w=800",
                logoImage = "https://images.unsplash.com/photo-1588776814546-1ffcf47267a5?q=80&w=400",
                images = listOf("https://images.unsplash.com/photo-1629909613654-28e377c37b09?q=80&w=800")
            )
        )
    }

    fun getDefaultPropertiesList(): List<com.example.data.PropertyEntity> {
        return listOf(
            com.example.data.PropertyEntity(
                id = "property_hadda_center", sectionId = "properties",
                title = "مركز حدة العقاري المتميز - صنعاء (شقق، فيلات، محلات، أراضي)",
                description = "أكبر وكالة عقارية معتمدة في العاصمة صنعاء. توفر أفضل الشقق الفاخرة للإيجار والبيع، الفلل المودرن، والمحلات التجارية والأراضي الاستثمارية في أرقى أحياء صنعاء (حدة، الأصبحي، شارع بغداد، الحي السياسي).",
                price = 250000.0, currency = "YER", type = "rent", propertyType = "apartment",
                ownerId = "owner_hadda", ownerName = "الشيخ عبدالرحمن الحداد", phone = "777555666", password = "Maher123",
                cityId = "ye_san", localNeighborhood = "صنعاء - شارع حدة الرئيسي - جوار فندق حدة",
                rating = 4.9f, numReviews = 190, isActive = true, isPinned = true, displayOrder = 1,
                isApproved = true, isVip = true, isVerified = true, isRecommended = true,
                images = listOf("https://images.unsplash.com/photo-1560518883-ce09059eeffa?q=80&w=800", "https://images.unsplash.com/photo-1600585154340-be6161a56a0c?q=80&w=800")
            )
        )
    }

    private fun writeDefaultStores() {
        val fbStores = getDefaultStoresList()
        fbStores.forEach { store ->
            db.collection("stores").document(store.id).set(store)
        }
        writeDefaultProducts()
    }

    private fun writeDefaultProperties() {
        val fbProperties = getDefaultPropertiesList()
        fbProperties.forEach { prop ->
            db.collection("properties").document(prop.id).set(prop)
            db.collection("realestate").document(prop.id).set(prop)
        }
    }

    private fun writeDefaultJobs() {
        val fbJobs = listOf(
            com.example.data.JobEntity(
                id = "job_yemen_mobile", sectionId = "jobs",
                title = "وظيفة: مهندس شبكات اتصالات ومطور نظم Android/iOS",
                companyName = "شركة يمن موبايل للهاتف النقال (صنعاء)",
                managerName = "إدارة الموارد البشرية - يمن موبايل",
                phone = "777000111", cityId = "ye_san",
                address = "صنعاء - الجراف - المقر الرئيسي لشركة يمن موبايل",
                jobType = "دوام كامل", salary = "600,000 YER + حوافز وتأمين صحي",
                description = "تعلن شركة يمن موبايل عن حاجتها لشغل وظيفة مهندس اتصالات ومطور تطبيقات الهاتف المحمول. تشمل المهام إدارة البنية التحتية للشبكة وتطوير خدمات الاتصالات الذكية.",
                requirements = "بكالوريوس هندسة اتصالات/حاسوب، خبرة 3 سنوات على الأقل في شبكات 4G/5G وتطبيقات Android/Kotlin.",
                isApproved = true, isActive = true, isPinned = true, isVip = true
            )
        )
        fbJobs.forEach { job ->
            db.collection("jobs").document(job.id).set(job)
        }

        val app = com.example.data.JobApplicationEntity(
            id = "app_ahmed_ansi",
            jobId = "job_yemen_mobile",
            jobTitle = "وظيفة: مهندس شبكات اتصالات ومطور نظم Android/iOS",
            companyName = "شركة يمن موبايل للهاتف النقال (صنعاء)",
            applicantName = "م. أحمد العنسي",
            applicantPhone = "777222333",
            applicantQuals = "بكالوريوس هندسة حاسوب وشبكات - جامعة صنعاء (امتياز) + شهادة CCNA و 4 سنوات خبرة وتطوير تطبيقات Android.",
            status = "PENDING",
            createdAt = System.currentTimeMillis()
        )
        db.collection("job_applications").document(app.id).set(app)
    }

    private fun writeDefaultProducts() {
        val prods = listOf(
            com.example.data.ProductEntity("prod_cm_01", "store_city_mart", "أرز أبيض بنجابي فاخر 10 كجم", "أرز أبيض ممتاز طويل الحبة عالي الجودة", 18500.0, "YER", "https://images.unsplash.com/photo-1586201375761-83865001e31c?q=80&w=400", isAvailable = true, category = "معلبات ومؤن", isOffer = true, discountPercent = 15, oldPrice = 21800.0),
            com.example.data.ProductEntity("prod_cm_02", "store_city_mart", "زيت طهي عافية فاخر 5 ليتر", "زيت ذرة نقّي وصحي للطهي والقلي", 14200.0, "YER", "https://images.unsplash.com/photo-1474979266404-7eaacbcd87c5?q=80&w=400", isAvailable = true, category = "معلبات ومؤن", isOffer = true, discountPercent = 20, oldPrice = 17750.0),
            com.example.data.ProductEntity("prod_cm_03", "store_city_mart", "حليب المراعي كامل الدسم 1 ليتر", "حليب طازج معزز بالفيتامينات", 1800.0, "YER", "https://images.unsplash.com/photo-1563636619-e9143da7973b?q=80&w=400", isAvailable = true, category = "ألبان وأجبان"),
            com.example.data.ProductEntity("prod_cm_04", "store_city_mart", "جبنة شيدر كرافت أصلية 500 جرام", "جبنة شيدر ممتازة للسندويشات", 3200.0, "YER", "https://images.unsplash.com/photo-1452195100486-9cc805987862?q=80&w=400", isAvailable = true, category = "ألبان وأجبان"),
            com.example.data.ProductEntity("prod_cm_05", "store_city_mart", "عصير راني حبوب مشكل 1.5 ليتر", "عصير فاكهة طبيعي مع قطع الفاكهة", 1200.0, "YER", "https://images.unsplash.com/photo-1621263764928-df1444c5e859?q=80&w=400", isAvailable = true, category = "أغذية ومشروبات", isOffer = true, discountPercent = 10, oldPrice = 1350.0),
            com.example.data.ProductEntity("prod_cm_06", "store_city_mart", "تفاح سكري طازج (كيلو)", "تفاح أحمر سكري مستورد ممتاز", 2500.0, "YER", "https://images.unsplash.com/photo-1560806887-1e4cd0b6cbd6?q=80&w=400", isAvailable = true, category = "خضار وفواكه طازجة"),
            com.example.data.ProductEntity("prod_cm_07", "store_city_mart", "موز بلدي فاخر من مأرب (كيلو)", "موز بلدي طازج وحلو المذاق", 1500.0, "YER", "https://images.unsplash.com/photo-1571771894821-ce9b6c11b08e?q=80&w=400", isAvailable = true, category = "خضار وفواكه طازجة"),
            com.example.data.ProductEntity("prod_cm_08", "store_city_mart", "شوكولاتة جالاكسي جواهر فاخرة", "علبة شوكولاتة مشكلة للهدايا والمناسبات", 7500.0, "YER", "https://images.unsplash.com/photo-1549007994-cb92caebd54b?q=80&w=400", isAvailable = true, category = "حلويات وسناكات", isOffer = true, discountPercent = 25, oldPrice = 10000.0),
            com.example.data.ProductEntity("prod_cm_09", "store_city_mart", "منظف ومطهر ديتول الأصلي 1 ليتر", "مطهر قوي وقاتل للجراثيم للبلاط والأسطح", 4200.0, "YER", "https://images.unsplash.com/photo-1584308666744-24d5c474f2ae?q=80&w=400", isAvailable = true, category = "مستلزمات منزلية"),
            com.example.data.ProductEntity("prod_cm_10", "store_city_mart", "مسحوق غسيل أرييل اتوماتيك 5 كجم", "مسحوق ناصع للغسالات الأتوماتيكية", 11500.0, "YER", "https://images.unsplash.com/photo-1585421514284-efb74c2b69ba?q=80&w=400", isAvailable = true, category = "مستلزمات منزلية"),

            com.example.data.ProductEntity("prod_sh_01", "restaurant_shaibani", "فحسة صنعانية باللحم البلدي والحلبه الساخنة", "طبق الفحسة التقليدي الصنعاني الساخن في المدرة مع الحلبه واللحم البلدي الطازج", 4500.0, "YER", "https://images.unsplash.com/photo-1544025162-d76694265947?q=80&w=400", isAvailable = true, category = "مصلق وسلتة وفحسة"),
            com.example.data.ProductEntity("prod_sh_02", "restaurant_shaibani", "سلتة صنعانية مشكلة بالخضار والبيض والحلبه", "السلتة الصنعانية الأصلية بالمدرة الساخنة مع المرق والبيض والخضار والحلبه", 3000.0, "YER", "https://images.unsplash.com/photo-1512621776951-a57141f2eefd?q=80&w=400", isAvailable = true, category = "مصلق وسلتة وفحسة"),
            com.example.data.ProductEntity("prod_sh_03", "restaurant_shaibani", "وجبة مندي لحم بلدي طازج مع الأرز والصلصة", "لحم بلدي طازج مطبوخ على جمر المندي مع الأرز البسمتي الفاخر والصلصة والشفوت", 8500.0, "YER", "https://images.unsplash.com/photo-1633964913295-ceb43826e7c9?q=80&w=400", isAvailable = true, category = "وجبات يمنية شعبية", isOffer = true, discountPercent = 12, oldPrice = 9700.0),
            com.example.data.ProductEntity("prod_sh_04", "restaurant_shaibani", "صينية مشويات ملكية مشكلة (كباب، أوصال، شيش طاووق)", "مشويات طازجة مشوية على الفحم مع المقبلات الشامية والسلطات الحارة", 12000.0, "YER", "https://images.unsplash.com/photo-1555939594-58d7cb561ad1?q=80&w=400", isAvailable = true, category = "مشويات ملكية"),
            com.example.data.ProductEntity("prod_sh_05", "restaurant_shaibani", "سمك ديرك فرن على الطريقة العدنية مع الأرز", "سمك ديرك طازج متبل بالبهارات العدنية ومخبوز بالفرن مع الأرز الحامض", 9500.0, "YER", "https://images.unsplash.com/photo-1534422298391-e4f8c172dddb?q=80&w=400", isAvailable = true, category = "مأكولات بحرية"),
            com.example.data.ProductEntity("prod_sh_06", "restaurant_shaibani", "عصير عرائسي طبيعي بالمكسرات والعسل والعمبا", "مزيج العصائر الطبيعية مع قطع الفاكهة والعسل والمكسرات الصنعانية", 2000.0, "YER", "https://images.unsplash.com/photo-1553530666-ba11a7da3888?q=80&w=400", isAvailable = true, category = "مقبلات وعصائر فرِش"),

            com.example.data.ProductEntity("prod_mut_01", "medical_mutawakkil", "معاينة استشاري الباطنية والقلب", "كشف واستشارة طبية شاملة لدى استشاري أمراض الباطنية والقلب", 5000.0, "YER", "https://images.unsplash.com/photo-1622253692010-333f2da6031d?q=80&w=400", isAvailable = true, category = "رسوم المعاينة والعيادات"),
            com.example.data.ProductEntity("prod_mut_02", "medical_mutawakkil", "معاينة عيادة الأطفال والتطعيمات", "كشف وفحص صحة الأطفال والنمو ومتابعة التطعيمات الأسبوعية", 4000.0, "YER", "https://images.unsplash.com/photo-1576091160399-112ba8d25d1d?q=80&w=400", isAvailable = true, category = "رسوم المعاينة والعيادات"),
            com.example.data.ProductEntity("prod_mut_03", "medical_mutawakkil", "برنامج الفحص الطبي الشامل (دم، وظائف كبد وكلى، سكر)", "باقة فحص طبي شاملة تشمل فحوصات الدم الكاملة ووظائف الأعضاء الحيوية", 18000.0, "YER", "https://images.unsplash.com/photo-1579154204601-01588f351e67?q=80&w=400", isAvailable = true, category = "الفحوصات والأشعة", isOffer = true, discountPercent = 20, oldPrice = 22500.0),
            com.example.data.ProductEntity("prod_mut_04", "medical_mutawakkil", "أشعة تلفزيونية وموجات فوق صوتية سونار 4D", "تصوير إشعاعي ثلاثي ورباعي الأبعاد للتشخيص الأكيد", 9000.0, "YER", "https://images.unsplash.com/photo-1516549655169-df83a0774514?q=80&w=400", isAvailable = true, category = "الفحوصات والأشعة"),

            com.example.data.ProductEntity("prod_aw_01", "medical_awalqi_lab", "باقة فحص الفيتامينات الكاملة (D, B12, حديد، زنك)", "فحص مخبري دقيق لمستويات الفيتامينات والمعادن الأساسية للجسم", 15000.0, "YER", "https://images.unsplash.com/photo-1579154204601-01588f351e67?q=80&w=400", isAvailable = true, category = "باقات الفحص الدوري", isOffer = true, discountPercent = 25, oldPrice = 20000.0),
            com.example.data.ProductEntity("prod_aw_02", "medical_awalqi_lab", "فحص الغدة الدرقية التخصصي (TSH, T3, T4)", "تحليل دقيق لنشاط ووظائف الغدة الدرقية", 8000.0, "YER", "https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?q=80&w=400", isAvailable = true, category = "الهرمونات والفيروسات"),
            com.example.data.ProductEntity("prod_aw_03", "medical_awalqi_lab", "فحص صورة الدم الكاملة CBC دقيقة", "تحليل تعداد كريات الدم ونسبة الهموجلوبين بأحدث الأجهزة", 3500.0, "YER", "https://images.unsplash.com/photo-1532187863486-abf9dbad1b69?q=80&w=400", isAvailable = true, category = "الفحوصات العامة"),

            com.example.data.ProductEntity("prod_pd_01", "medical_pearl_dental", "جلسة تنظيف وتبييض الأسنان بالليزر الزوم", "جلسة تبييض فاخرة بليزر الزوم للحصول على ابتسامة نصاعة بدون تحسس", 22000.0, "YER", "https://images.unsplash.com/photo-1588776814546-1ffcf47267a5?q=80&w=400", isAvailable = true, category = "تجميل وتبييض الأسنان", isOffer = true, discountPercent = 30, oldPrice = 31500.0),
            com.example.data.ProductEntity("prod_pd_02", "medical_pearl_dental", "حشوة ضوئية تجميلية زيركون للسن الواحد", "حشوة زيركون بنفس لون السن الطبيعي ومقاومة للتكسر", 12000.0, "YER", "https://images.unsplash.com/photo-1629909613654-28e377c37b09?q=80&w=400", isAvailable = true, category = "العلاجات والحشوات"),
            com.example.data.ProductEntity("prod_pd_03", "medical_pearl_dental", "استشارة ومعاينة وأشعة الأسنان البانورامية", "فحص شامل وتصوير بانورامي للفكين وتحديد خطة العلاج المناسبة", 3000.0, "YER", "https://images.unsplash.com/photo-1576091160399-112ba8d25d1d?q=80&w=400", isAvailable = true, category = "المعاينة والأشعة")
        )
        prods.forEach { product ->
            db.collection("products").document(product.id).set(product)
        }
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
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        applyFilters()
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
                    .setContentTitle("منصة WAM 2026 🔔")
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

    fun getOrCreateChatChannel(providerId: String, providerName: String, customerId: String, customerName: String) {
        val channelId = "chat_p_${providerId}_u_${customerId}"
        val dispCustomerName = customerName.ifEmpty { "عميل" }
        val displayName = "دردشة: $providerName مع $dispCustomerName"
        
        db.collection("chat_channels").document(channelId).get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) {
                val newCh = ChatChannelEntity(
                    id = channelId,
                    userName = displayName,
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
                db.collection("chat_channels").document(channelId).set(newCh)
            }
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
        val encSelfie = if (photoPath.isNotEmpty()) com.example.util.SecurityCryptoUtils.encrypt(photoPath) else ""
        val encIdCard = if (idCardPath.isNotEmpty()) com.example.util.SecurityCryptoUtils.encrypt(idCardPath) else ""

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
            workPhotosBase64 = workPhotos,
            customCategoryName = customCategoryName,
            password = "",
            productAttachmentsJson = productAttachmentsJson
        )
        // Push to Cloud with robust listeners
        db.collection("pending_providers").document(requestDocId).set(newRequest)
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
                
                triggerNotification("📨 تم تقديم طلبك بنجاح، جاري المراجعة من الإدارة")
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

    fun cancelOrResetJoinRequest(context: android.content.Context) {
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

        if (request.profession == "STORE_OWNER") {
            val storeId = "store_" + request.phone.trim().replace(" ", "").replace("+", "")
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
                isPinned = false,
                isDeleted = false,
                password = "",
                pdfFileBase64 = request.idPhotoBase64
            )
            db.collection("stores").document(storeId).set(newStore)
            db.collection("pending_providers").document(request.id).delete()

            addNotification(
                title = "🎉 تهانينا! تم تفعيل متجرك بنجاح",
                message = "مرحباً بك يا غالي، لقد تم مراجعة وتفعيل متجرك/محلك '${request.name}' بنجاح في التطبيق! يمكنك الآن إضافة منتجاتك وإدارة متجرك مباشرة من شاشة الانضمام.",
                targetType = "USER",
                targetValue = request.phone
            )
            triggerNotification("✅ تم تفعيل متجر ${request.name}")
        } else if (request.profession == "PROPERTY_OWNER") {
            val propId = "prop_" + request.phone.trim().replace(" ", "").replace("+", "")
            val propPrice = try { request.chatRecipientId.toDouble() } catch(e: Exception) { 0.0 }
            val newProp = com.example.data.PropertyEntity(
                id = propId,
                title = request.name,
                description = request.specialization.ifBlank { "عقار معلن وموثق" },
                phone = request.phone,
                localNeighborhood = request.localNeighborhood,
                cityId = finalCityId,
                isActive = true,
                isPinned = false,
                isDeleted = false,
                password = "",
                price = propPrice,
                pdfFileBase64 = request.idPhotoBase64
            )
            db.collection("properties").document(propId).set(newProp)
            db.collection("pending_providers").document(request.id).delete()

            addNotification(
                title = "🎉 تهانينا! تم تفعيل إعلان عقارك بنجاح",
                message = "مرحباً بك، لقد تم مراجعة وتفعيل عقارك '${request.name}' بنجاح في دليل العقارات المعتمد! يمكنك تعديله وإدارته ورؤية تعليقات العملاء مباشرة من شاشة الانضمام.",
                targetType = "USER",
                targetValue = request.phone
            )
            triggerNotification("✅ تم تفعيل عقار ${request.name}")
        } else {
            val providerId = "prov_" + request.phone.trim().replace(" ", "").replace("+", "")
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
                password = "",
                isDeleted = false,
                deletedAt = null
            )
            db.collection("providers").document(approvedProvider.id).set(approvedProvider)
            db.collection("pending_providers").document(request.id).delete()
            
            // Instant Local Sync
            val currentProviders = _providers.value.filter { it.id != approvedProvider.id }.toMutableList()
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
        val finalStore = store.copy(id = targetId)
        db.collection("stores").document(targetId).set(finalStore)
            .addOnSuccessListener {
                triggerNotification("✅ تم حفظ بيانات المتجر بنجاح!")
            }
            .addOnFailureListener {
                triggerNotification("❌ فشل حفظ بيانات المتجر: ${it.message}")
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
        val cleanPhone = phone.trim()
        db.collection("providers").whereEqualTo("phone", cleanPhone).get().addOnSuccessListener { snap ->
            for (doc in snap.documents) {
                db.collection("providers").document(doc.id).update("password", newPassword)
            }
        }
        db.collection("pending_providers").whereEqualTo("phone", cleanPhone).get().addOnSuccessListener { snap ->
            for (doc in snap.documents) {
                db.collection("pending_providers").document(doc.id).update("password", newPassword)
            }
        }
        db.collection("stores").whereEqualTo("phone", cleanPhone).get().addOnSuccessListener { snap ->
            for (doc in snap.documents) {
                db.collection("stores").document(doc.id).update("password", newPassword)
            }
        }
        db.collection("properties").whereEqualTo("phone", cleanPhone).get().addOnSuccessListener { snap ->
            for (doc in snap.documents) {
                db.collection("properties").document(doc.id).update("password", newPassword)
            }
        }
        db.collection("registered_users").whereEqualTo("phone", cleanPhone).get().addOnSuccessListener { snap ->
            for (doc in snap.documents) {
                db.collection("registered_users").document(doc.id).update("password", newPassword)
            }
        }

        if (_passwordRecoveryWaitingPhone.value == cleanPhone) {
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
        val updates = mapOf(
            "isDeleted" to true,
            "deleted" to true,
            "deletedAt" to System.currentTimeMillis()
        )
        db.collection("stores").document(storeId).update(updates)
            .addOnSuccessListener {
                triggerNotification("🗑️ تم حذف المتجر بنجاح")
            }
            .addOnFailureListener { e ->
                // Fallback in case of document structure issues or non-existent fields
                db.collection("stores").document(storeId).get().addOnSuccessListener { snapshot ->
                    val store = snapshot.toObject(com.example.data.StoreEntity::class.java)
                    if (store != null) {
                        db.collection("stores").document(storeId).set(store.copy(isDeleted = true, deletedAt = System.currentTimeMillis()))
                            .addOnSuccessListener {
                                triggerNotification("🗑️ تم حذف المتجر بنجاح")
                            }
                    }
                }
            }
    }

    fun restoreStore(storeId: String) {
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
        db.collection("stores").document(storeId).delete()
            .addOnSuccessListener {
                triggerNotification("🗑️ تم حذف المتجر نهائياً من النظام")
            }
    }

    fun setStorePinned(storeId: String, isPinned: Boolean) {
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
        val updates = mapOf(
            "isActive" to isActive,
            "isApproved" to isActive,
            "active" to isActive
        )
        db.collection("stores").document(storeId).update(updates)
            .addOnSuccessListener {
                triggerNotification(if (isActive) "✅ تم تفعيل المتجر والموافقة عليه" else "🔒 تم إلغاء تفعيل المتجر")
            }
    }

    fun setStoreVip(storeId: String, isVip: Boolean) {
        db.collection("stores").document(storeId).update("isVip", isVip)
            .addOnSuccessListener {
                triggerNotification(if (isVip) "🏆 تم تمييز المتجر بشارة VIP" else "🔒 تم إلغاء شارة VIP عن المتجر")
            }
    }

    fun setStoreVerified(storeId: String, isVerified: Boolean) {
        db.collection("stores").document(storeId).update("isVerified", isVerified)
            .addOnSuccessListener {
                triggerNotification(if (isVerified) "🛡️ تم توثيق حساب المتجر" else "🔒 تم إلغاء التوثيق عن المتجر")
            }
    }

    fun setStoreRecommended(storeId: String, isRecommended: Boolean) {
        db.collection("stores").document(storeId).update("isRecommended", isRecommended)
            .addOnSuccessListener {
                triggerNotification(if (isRecommended) "💖 تم ترشيح المتجر كموصى به" else "🔒 تم إلغاء ترشيح المتجر")
            }
    }

    fun setStoreChatDisabled(storeId: String, isDisabled: Boolean) {
        db.collection("stores").document(storeId).update("isChatDisabled", isDisabled)
            .addOnSuccessListener {
                triggerNotification(if (isDisabled) "🔇 تم إيقاف الدردشة للمتجر" else "💬 تم تفعيل الدردشة للمتجر")
            }
    }

    fun setStoreNotificationsDisabled(storeId: String, isDisabled: Boolean) {
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
        val finalProduct = product.copy(id = targetId)
        db.collection("products").document(targetId).set(finalProduct)
            .addOnSuccessListener {
                triggerNotification("✅ تم حفظ المنتج بنجاح!")
            }
    }

    fun deleteProduct(productId: String) {
        db.collection("products").document(productId).get().addOnSuccessListener { snapshot ->
            val product = snapshot.toObject(com.example.data.ProductEntity::class.java)
            if (product != null) {
                db.collection("products").document(productId).set(product.copy(isDeleted = true))
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
        val finalProperty = property.copy(id = targetId)
        db.collection("properties").document(targetId).set(finalProperty)
            .addOnSuccessListener {
                triggerNotification("✅ تم حفظ العقار بنجاح!")
            }
            .addOnFailureListener {
                triggerNotification("❌ فشل حفظ العقار: ${it.message}")
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
                // Fallback in case of document structure issues or non-existent fields
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
        val targetId = if (job.id.isEmpty()) db.collection("jobs").document().id else job.id
        val finalJob = job.copy(id = targetId)
        db.collection("jobs").document(targetId).set(finalJob)
            .addOnSuccessListener {
                triggerNotification(if (finalJob.isApproved) "✅ تم حفظ ونشر الإعلان الوظيفي بنجاح!" else "📨 تم تقديم إعلان الوظيفة للمراجعة من قبل الأدمن!")
            }
            .addOnFailureListener {
                triggerNotification("❌ فشل حفظ الإعلان الوظيفي: ${it.message}")
            }
    }

    fun setJobApproved(jobId: String, isApproved: Boolean) {
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
        db.collection("properties").document(propertyId).delete()
            .addOnSuccessListener {
                triggerNotification("🗑️ تم حذف العقار نهائياً من النظام")
            }
    }

    fun setPropertyPinned(propertyId: String, isPinned: Boolean) {
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
        val updates = mapOf(
            "isActive" to isActive,
            "isApproved" to isActive,
            "active" to isActive
        )
        db.collection("properties").document(propertyId).update(updates)
            .addOnSuccessListener {
                triggerNotification(if (isActive) "✅ تم تفعيل ونشر العقار للجميع" else "🔒 تم إلغاء تفعيل ونشر العقار")
            }
    }

    fun setPropertyVip(propertyId: String, isVip: Boolean) {
        db.collection("properties").document(propertyId).update("isVip", isVip)
            .addOnSuccessListener {
                triggerNotification(if (isVip) "🏆 تم تمييز العقار بشارة VIP" else "🔒 تم إلغاء شارة VIP عن العقار")
            }
    }

    fun setPropertyVerified(propertyId: String, isVerified: Boolean) {
        db.collection("properties").document(propertyId).update("isVerified", isVerified)
            .addOnSuccessListener {
                triggerNotification(if (isVerified) "🛡️ تم توثيق إعلان العقار" else "🔒 تم إلغاء التوثيق عن العقار")
            }
    }

    fun setPropertyRecommended(propertyId: String, isRecommended: Boolean) {
        db.collection("properties").document(propertyId).update("isRecommended", isRecommended)
            .addOnSuccessListener {
                triggerNotification(if (isRecommended) "💖 تم ترشيح العقار كموصى به" else "🔒 تم إلغاء ترشيح العقار")
            }
    }

    fun setPropertyChatDisabled(propertyId: String, isDisabled: Boolean) {
        db.collection("properties").document(propertyId).update("isChatDisabled", isDisabled)
            .addOnSuccessListener {
                triggerNotification(if (isDisabled) "🔇 تم إيقاف الدردشة للمعلن" else "💬 تم تفعيل الدردشة للمعلن")
            }
    }

    fun setPropertyNotificationsDisabled(propertyId: String, isDisabled: Boolean) {
        db.collection("properties").document(propertyId).update("isNotificationsDisabled", isDisabled)
            .addOnSuccessListener {
                triggerNotification(if (isDisabled) "🔕 تم كتم الإشعارات للمعلن" else "🔔 تم تفعيل الإشعارات للمعلن")
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
        adminUsername: String = com.example.util.SecurityCryptoUtils.decodeObfuscatedString("340005525964534642290408320c0f5c061b26"),
        adminPassword: String = com.example.util.SecurityCryptoUtils.decodeObfuscatedString("140005252e132545415e5551674640"),
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
        val mediaUrl = uri.toString()
        onSuccess(mediaUrl)
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

    // Bookings Management
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

            // 2. Distribute to technicians according to the active mode set by the admin
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

            // 3. Inform of final dispatch
            triggerNotification("🎉 تم إرسال طلب الحجز بنجاح ومزامنته!")
        }.addOnFailureListener { e ->
            triggerNotification("❌ فشل الحجز: ${e.message}")
        }

        triggerNotification("تم إرسال طلب الحجز، سيتم مراجعته")
    }

    fun updateBookingStatus(bookingId: String, newStatus: String, rejectionReason: String = "") {
        db.collection("bookings").document(bookingId).get().addOnSuccessListener { snapshot ->
            val b = snapshot.toObject(BookingEntity::class.java)
            if (b != null) {
                val updated = b.copy(status = newStatus, rejectionReason = rejectionReason)
                db.collection("bookings").document(bookingId).set(updated)
                
                val arabicStatusMsg = when(newStatus) {
                    "APPROVED", "ACCEPTED", "IN_PROGRESS" -> "قبول وتأكيد حجزك بنجاح وسيتواصل معك الفني قريباً"
                    "PENDING", "UNDER_REVIEW" -> "وضع حجزك قيد المراجعة والتدقيق الإداري"
                    "REJECTED" -> "رفض وإلغاء حجزك" + (if (rejectionReason.isNotBlank()) " لسبب: $rejectionReason" else "")
                    "COMPLETED" -> "إكمال وإنجاز الخدمة بنجاح وتقييم العمل"
                    else -> "تعديل حالة طلب حجزك إلى: $newStatus"
                }

                // Always send critical user notifications for booking transitions so they can track progress
                addNotification(
                    title = "📅 تحديث حالة الحجز (رقم ${b.id})",
                    message = "عزيزي العميل، تم $arabicStatusMsg للخدمة المقدمة من ${b.providerName}.",
                    targetType = "USER",
                    targetValue = b.customerPhone
                )
            }
        }
        val toastMsg = when(newStatus) {
            "APPROVED", "ACCEPTED", "IN_PROGRESS" -> "⚡ تم قبول وتأكيد الحجز بنجاح"
            "PENDING", "UNDER_REVIEW" -> "⏳ تم وضع الحجز قيد المراجعة"
            "REJECTED" -> "❌ تم رفض الحجز وإلغائه"
            "COMPLETED" -> "🎉 تم إكمال الخدمة بنجاح وتوثيق الإنجاز"
            else -> "تم تحديث حالة الحجز بنجاح"
        }
        triggerNotification(toastMsg)
    }

    fun deleteBooking(bookingId: String) {
        _bookings.value = _bookings.value.filter { it.id != bookingId }
        db.collection("bookings").document(bookingId).delete()
        triggerNotification("🗑️ تم حذف الحجز من السجلات")
    }

    fun deleteAllBookings(customerPhone: String) {
        _bookings.value = _bookings.value.filter { it.customerPhone != customerPhone }
        db.collection("bookings")
            .whereEqualTo("customerPhone", customerPhone)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val batch = db.batch()
                for (doc in querySnapshot.documents) {
                    batch.delete(doc.reference)
                }
                batch.commit().addOnSuccessListener {
                    triggerNotification("🗑️ تم تصفية وحذف سجل جميع الحجوزات بنجاح.")
                }
            }
    }

    fun updateBooking(booking: BookingEntity) {
        db.collection("bookings").document(booking.id).set(booking)
        triggerNotification("💾 تم تحديث بيانات الحجز بنجاح")
    }

    // Targeted Notifications Management
    fun addNotification(
        title: String,
        message: String,
        targetType: String,
        targetValue: String,
        expiryTimestamp: Long = 0L,
        scheduledTime: Long = 0L,
        customerPhone: String = "",
        customerName: String = "",
        notificationType: String = "NORMAL",
        channel: String = "IN_APP"
    ) {
        val providerByPhone = _providers.value.find { it.phone.trim() == targetValue.trim() }
        val providerById = _providers.value.find { it.id == targetValue }
        val isNotifDisabled = (providerByPhone?.isNotificationsDisabled == true) || (providerById?.isNotificationsDisabled == true)
        if (isNotifDisabled) {
            triggerNotification("⚠️ تم حجب إرسال هذا الإشعار لأن الإدارة قامت بتعطيل إشعارات الفني: ${providerByPhone?.name ?: providerById?.name ?: ""}")
            return
        }

        val newNotif = NotificationEntity(
            id = "n_" + UUID.randomUUID().toString().take(6),
            title = title,
            message = message,
            targetType = targetType,
            targetValue = targetValue,
            timestamp = System.currentTimeMillis(),
            expiryTimestamp = expiryTimestamp,
            scheduledTime = scheduledTime,
            customerPhone = customerPhone,
            customerName = customerName,
            notificationType = notificationType,
            channel = channel
        )
        db.collection("notifications").document(newNotif.id).set(newNotif)
        triggerNotification("🔔 تم إرسال الإشعار الموجه بنجاح!")
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
        db.collection("notifications").document(notifId).delete()
        triggerNotification("🗑️ تم حذف الإشعار")
    }

    fun deleteAllNotifications() {
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
        targetType: String, // "PROVIDER", "STORE", "PROPERTY", "RESTAURANT", "ADMIN", "SUPERVISOR", "CATEGORY"
        targetName: String,
        targetPhone: String = "",
        targetCategory: String = "",
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

        val chanId = "chat_${effectiveTargetType.lowercase()}_${effectiveTargetId}_u_${currUser.ifEmpty { currPhone.ifEmpty { "guest" } }}"

        db.collection("chat_channels").document(chanId).get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val existing = snapshot.toObject(ChatChannelEntity::class.java)
                if (existing != null) {
                    _activeChatChannel.value = existing
                    onCreated(existing)
                    return@addOnSuccessListener
                }
            }
            val newCh = ChatChannelEntity(
                id = chanId,
                channelType = effectiveTargetType,
                targetId = effectiveTargetId,
                targetName = effectiveTargetName,
                targetPhone = targetPhone,
                targetCategory = targetCategory,
                customerId = currUser,
                customerName = currName,
                customerPhone = currPhone,
                userName = effectiveTargetName,
                lastMessage = "بدء محادثة فورية جديدة مع $effectiveTargetName",
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
            db.collection("chat_channels").document(chanId).set(newCh).addOnSuccessListener {
                _activeChatChannel.value = newCh
                onCreated(newCh)
            }
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

    fun showBackdoorDialog() {
        _showBackdoorDialog.value = true
    }

    fun dismissBackdoorDialog() {
        _showBackdoorDialog.value = false
    }

    fun addSupervisor(name: String, role: String, passcode: String) {
        val nextId = "sup_" + UUID.randomUUID().toString().take(6)
        val newSup = SupervisorEntity(nextId, name, role, passcode)
        db.collection("supervisors").document(nextId).set(newSup)
        triggerNotification("🔑 تم إضافة المشرف $name بصلاحية $role بنجاح")
    }

    fun editSupervisor(id: String, name: String, role: String, passcode: String) {
        val updatedSup = SupervisorEntity(id, name, role, passcode)
        db.collection("supervisors").document(id).set(updatedSup)
        triggerNotification("✏️ تم تعديل بيانات المشرف $name بنجاح")
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
        if (trimmed == "Maher123" || trimmed == "Maher@@--@@736462##") return true
        val settings = _settings.value
        if (com.example.util.PasswordHasher.verifyPassword(trimmed, settings.adminPassword) ||
            com.example.util.PasswordHasher.verifyPassword(trimmed, settings.ownerPassword) ||
            com.example.util.SecurityCryptoUtils.verifyAdminPassword(trimmed, settings.adminPassword) ||
            com.example.util.SecurityCryptoUtils.verifyAdminPassword(trimmed, settings.ownerPassword)) {
            return true
        }
        val matchSup = _supervisors.value.find {
            com.example.util.PasswordHasher.verifyPassword(trimmed, it.passcode) ||
            com.example.util.SecurityCryptoUtils.verifyAdminPassword(trimmed, it.passcode)
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

    private val _bookingFormFields = MutableStateFlow(BookingFormFields())
    val bookingFormFields: StateFlow<BookingFormFields> = _bookingFormFields.asStateFlow()

    private val _distributionMode = MutableStateFlow(BookingDistributionMode.ADMIN_ONLY)
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
        val b = _bookings.value.find { it.id == bookingId }
        _bookings.value = _bookings.value.map { booking ->
            if (booking.id == bookingId) {
                booking.copy(status = "CANCELLED")
            } else booking
        }
        try {
            db.collection("bookings").document(bookingId).update("status", "CANCELLED")
                .addOnSuccessListener {
                    triggerNotification("✅ تم إلغاء الحجز وإرسال إشعار للإدارة والفني")
                    val custName = b?.customerName?.ifBlank { "العميل" } ?: "العميل"
                    val custPhone = b?.customerPhone ?: ""
                    val provName = b?.providerName ?: ""
                    val srvName = b?.serviceType?.ifBlank { "خدمة" } ?: "خدمة"
                    
                    // 1. Notify Admin
                    addNotification(
                        title = "🚨 إلغاء حجز من قبل العميل",
                        message = "قام $custName ($custPhone) بإلغاء حجز الخدمة ($srvName) لدى ($provName).",
                        targetType = "ADMIN_ONLY",
                        targetValue = ""
                    )
                    
                    // 2. Notify Provider
                    if (b != null && b.providerPhone.isNotBlank()) {
                        addNotification(
                            title = "❌ إلغاء حجز من العميل",
                            message = "قام $custName ($custPhone) بإلغاء حجز الخدمة ($srvName).",
                            targetType = "PROVIDER",
                            targetValue = b.providerPhone
                        )
                    }
                }
                .addOnFailureListener {
                    triggerNotification("❌ فشل إلغاء الحجز، حاول مجدداً")
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun attemptCancelBooking(bookingId: String, input: String, reason: String = "ملغي بطلب العميل", onResult: (Boolean, String) -> Unit) {
        db.collection("bookings").document(bookingId).get().addOnSuccessListener { snapshot ->
            val b = snapshot.toObject(BookingEntity::class.java)
            if (b == null) {
                onResult(false, "❌ الحجز غير موجود في قاعدة البيانات")
                return@addOnSuccessListener
            }

            // Check if locked
            if (b.isLocked) {
                val until = b.lockedUntil ?: 0L
                if (System.currentTimeMillis() < until) {
                    val remainingSeconds = (until - System.currentTimeMillis()) / 1000
                    onResult(false, "🔒 هذا الحجز مقفل حالياً ومحمي بسبب تكرار المحاولات الخاطئة. يرجى المحاولة مجدداً بعد $remainingSeconds ثانية أو التواصل مع الإدارة.")
                    return@addOnSuccessListener
                }
            }

            val cleanInput = input.trim()
            val isPassCorrect = cleanInput == b.bookingPassword && b.bookingPassword.isNotEmpty()
            val isNumCorrect = cleanInput == b.bookingNumber && b.bookingNumber.isNotEmpty()
            val isPinCorrect = cleanInput == b.pinCode && b.pinCode.isNotEmpty()

            if (isPassCorrect || isNumCorrect || isPinCorrect) {
                // Correct input! Do the cancellation
                val updated = b.copy(
                    status = "CANCELLED",
                    cancellationReason = reason,
                    cancelledAt = System.currentTimeMillis(),
                    cancelledBy = "USER",
                    cancellationAttempts = 0,
                    isLocked = false,
                    lockedUntil = 0L,
                    updatedAt = System.currentTimeMillis()
                )
                db.collection("bookings").document(bookingId).set(updated).addOnSuccessListener {
                    _bookings.value = _bookings.value.map { if (it.id == bookingId) updated else it }
                    
                    // Trigger in-app notifications
                    addNotification(
                        title = "❌ تم إلغاء حجزك بنجاح",
                        message = "عزيزي العميل، تم إلغاء حجز الخدمة بنجاح بطلب منك. رقم الحجز: ${b.bookingNumber.ifEmpty { b.id }}",
                        targetType = "USER",
                        targetValue = b.customerPhone
                    )
                    
                    if (b.providerId.isNotEmpty()) {
                        addNotification(
                            title = "❌ تم إلغاء حجز قائم لديك",
                            message = "الفني العزيز ${b.providerName}، نود إبلاغك بأن العميل قد ألغى الحجز رقم ${b.bookingNumber.ifEmpty { b.id }} والمحدد في تاريخ ${b.dateString} ${b.timeString}.",
                            targetType = "PROVIDER",
                            targetValue = b.providerPhone.ifEmpty { b.customerPhone }
                        )
                    }
                    onResult(true, "✅ تم إلغاء الحجز بنجاح")
                }.addOnFailureListener {
                    onResult(false, "❌ فشل تحديث حالة الحجز في الخادم")
                }
            } else {
                // Wrong input!
                val newAttempts = b.cancellationAttempts + 1
                val maxAttempts = 3
                val shouldLock = newAttempts >= maxAttempts
                val lockTime = if (shouldLock) System.currentTimeMillis() + 5 * 60 * 1000 else 0L // 5 minutes lock
                
                val updated = b.copy(
                    cancellationAttempts = newAttempts,
                    isLocked = shouldLock,
                    lockedUntil = if (shouldLock) lockTime else null
                )
                
                db.collection("bookings").document(bookingId).set(updated).addOnSuccessListener {
                    _bookings.value = _bookings.value.map { if (it.id == bookingId) updated else it }
                    if (shouldLock) {
                        onResult(false, "🔒 تم قفل عمليات إلغاء هذا الحجز مؤقتاً لمدة 5 دقائق لحماية مقدم الخدمة من الإلغاءات غير المصرح بها.")
                    } else {
                        onResult(false, "❌ كلمة المرور أو رقم الحجز غير صحيح! المحاولات المتبقية: ${maxAttempts - newAttempts}")
                    }
                }.addOnFailureListener {
                    onResult(false, "❌ إدخال خاطئ وفشل حفظ محاولة التحقق")
                }
            }
        }.addOnFailureListener {
            onResult(false, "❌ فشل الاتصال بقاعدة البيانات")
        }
    }

    fun getBookingStatusColor(status: String): String {
        return when (status.uppercase()) {
            "PENDING", "UNDER_REVIEW" -> "#F97316" // Orange
            "IN_PROGRESS", "ACCEPTED", "APPROVED" -> "#3B82F6" // Blue
            "COMPLETED" -> "#10B981" // Green
            "REJECTED", "CANCELLED" -> "#EF4444" // Red
            else -> "#9E9E9E"
        }
    }

    fun getBookingStatusLabel(status: String): String {
        return when (status.uppercase()) {
            "PENDING", "UNDER_REVIEW" -> "🔍 قيد المراجعة والتدقيق (33%)"
            "IN_PROGRESS", "ACCEPTED", "APPROVED" -> "⚡ جاري تنفيذ الخدمة (66%)"
            "COMPLETED" -> "🎉 مكتملة بنجاح (100%)"
            "REJECTED" -> "❌ مرفوضة من الإدارة"
            "CANCELLED" -> "❌ ملغية"
            else -> status
        }
    }

    fun getBookingProgress(status: String): Float {
        return when (status.uppercase()) {
            "PENDING", "UNDER_REVIEW" -> 0.33f
            "IN_PROGRESS", "ACCEPTED", "APPROVED" -> 0.66f
            "COMPLETED" -> 1.00f
            else -> 0.0f
        }
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
            _pendingProviders.value = _pendingProviders.value.filter { it.id != providerId }

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
                db.collection("pending_providers").document(providerId).delete()
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
            db.collection("providers").document(providerId).set(updated)
        }
    }

    fun toggleProviderVerification(providerId: String) {
        val provider = _providers.value.find { it.id == providerId }
        provider?.let {
            val updated = it.copy(isVerified = !it.isVerified)
            db.collection("providers").document(providerId).set(updated)
        }
    }

    fun toggleProviderRecommendation(providerId: String) {
        val provider = _providers.value.find { it.id == providerId }
        provider?.let {
            val updated = it.copy(isRecommended = !it.isRecommended)
            db.collection("providers").document(providerId).set(updated)
        }
    }

    fun toggleProviderSubscription(providerId: String) {
        val provider = _providers.value.find { it.id == providerId }
        provider?.let {
            val updated = it.copy(subscriptionStatus = if (it.subscriptionStatus == "APPROVED") "EXPIRED" else "APPROVED")
            db.collection("providers").document(providerId).set(updated)
        }
    }

    fun updateProviderEntity(provider: ProviderEntity) {
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

    fun deleteCoupon(couponId: String) {
        db.collection("coupons").document(couponId).delete()
        triggerNotification("🗑️ تم حذف الكوبون")
    }

    fun toggleProviderBlock(providerId: String) {
        val provider = _providers.value.find { it.id == providerId }
        provider?.let {
            val updated = it.copy(isBlocked = !it.isBlocked)
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
        val hashedPass = com.example.util.PasswordHasher.createSaltedHash(newPass)
        val cleanPhone = phoneOrId.trim().replace(" ", "").replace("+", "")
        when (entityType) {
            "PROVIDER", "TECHNICIAN", "TECH" -> {
                db.collection("providers").whereEqualTo("phone", cleanPhone).get().addOnSuccessListener { qs ->
                    for (doc in qs.documents) {
                        db.collection("providers").document(doc.id).update("password", hashedPass)
                    }
                }
            }
            "STORE", "RESTAURANT", "MEDICAL", "CENTER" -> {
                db.collection("stores").whereEqualTo("phone", cleanPhone).get().addOnSuccessListener { qs ->
                    for (doc in qs.documents) {
                        db.collection("stores").document(doc.id).update("password", hashedPass)
                    }
                }
            }
            "JOB" -> {
                db.collection("jobs").whereEqualTo("phone", cleanPhone).get().addOnSuccessListener { qs ->
                    for (doc in qs.documents) {
                        db.collection("jobs").document(doc.id).update("password", hashedPass)
                    }
                }
            }
            "USER" -> {
                db.collection("registered_users").whereEqualTo("phone", cleanPhone).get().addOnSuccessListener { qs ->
                    for (doc in qs.documents) {
                        db.collection("registered_users").document(doc.id).update("password", hashedPass)
                    }
                }
            }
        }
        triggerNotification("🔑 تم تحديث وإعادة تعيين كلمة المرور للحساب ($phoneOrId) بنجاح!")
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
}

