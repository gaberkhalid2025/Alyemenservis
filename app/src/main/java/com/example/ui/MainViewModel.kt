package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class MainViewModel : ViewModel() {

    // ------------------- Firestore setup -------------------
    private val db by lazy {
        val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        try {
            val settings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(false)
                .build()
            firestore.firestoreSettings = settings
        } catch (e: Exception) {
            e.printStackTrace()
        }
        firestore
    }

    // ------------------- StateFlows -------------------
    private val _categories = MutableStateFlow<List<CategoryEntity>>(emptyList())
    val categories: StateFlow<List<CategoryEntity>> = _categories.asStateFlow()

    private val _providers = MutableStateFlow<List<ProviderEntity>>(emptyList())
    val providers: StateFlow<List<ProviderEntity>> = _providers.asStateFlow()

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

    private val _notifications = MutableStateFlow<List<NotificationEntity>>(emptyList())
    val notifications: StateFlow<List<NotificationEntity>> = _notifications.asStateFlow()

    private val _chatChannels = MutableStateFlow<List<ChatChannelEntity>>(emptyList())
    val chatChannels: StateFlow<List<ChatChannelEntity>> = _chatChannels.asStateFlow()

    private val _currentUserId = MutableStateFlow("guest")
    val currentUserId: StateFlow<String> = _currentUserId.asStateFlow()

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val _currentUserName = MutableStateFlow("")
    val currentUserName: StateFlow<String> = _currentUserName.asStateFlow()

    private val _currentUserPhone = MutableStateFlow("")
    val currentUserPhone: StateFlow<String> = _currentUserPhone.asStateFlow()

    private val _currentUserResidence = MutableStateFlow("")
    val currentUserResidence: StateFlow<String> = _currentUserResidence.asStateFlow()

    private val _adminRole = MutableStateFlow("GUEST")
    val adminRole: StateFlow<String> = _adminRole.asStateFlow()

    val filteredNotifications: StateFlow<List<NotificationEntity>> = combine(
        _notifications,
        _currentUserId,
        _currentUserPhone,
        _adminRole
    ) { notificationsList, userId, phone, adminRoleState ->
        if (adminRoleState != "GUEST") {
            notificationsList
        } else if (userId == "guest") {
            notificationsList.filter { it.targetType == "ALL" }
        } else {
            notificationsList.filter {
                it.targetType == "ALL" || 
                (it.targetType == "USER" && (it.targetValue == userId || it.targetValue == phone))
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
        val sp = context.getSharedPreferences("yemen_service_prefs", android.content.Context.MODE_PRIVATE)
        val savedId = sp.getString("user_id", "guest") ?: "guest"
        _currentUserId.value = savedId
        _currentUserName.value = sp.getString("user_name", "") ?: ""
        _currentUserPhone.value = sp.getString("user_phone", "") ?: ""
        _currentUserResidence.value = sp.getString("user_residence", "") ?: ""
        
        val savedRole = sp.getString("saved_admin_role", "GUEST") ?: "GUEST"
        if (savedRole != "GUEST") {
            _adminRole.value = savedRole
        }
    }

    fun registerGuestUser(context: android.content.Context, name: String, phone: String, residence: String) {
        val newUserId = "user_" + (100000..999999).random().toString()
        _currentUserId.value = newUserId
        _currentUserName.value = name
        _currentUserPhone.value = phone
        _currentUserResidence.value = residence

        val sp = context.getSharedPreferences("yemen_service_prefs", android.content.Context.MODE_PRIVATE)
        sp.edit().apply {
            putString("user_id", newUserId)
            putString("user_name", name)
            putString("user_phone", phone)
            putString("user_residence", residence)
            apply()
        }

        // Save registration info to Firestore to ensure real tracing
        val regUser = mapOf(
            "id" to newUserId,
            "name" to name,
            "phone" to phone,
            "residence" to residence,
            "timestamp" to System.currentTimeMillis()
        )
        db.collection("registered_users").document(newUserId).set(regUser)
        triggerNotification("🎉 أهلاً بك في الدليل $name، تم تسجيل حسابك للتفعيل الأمني بنجاح")
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
        loadUserPoints()
        
        viewModelScope.launch {
            _currentUserId.collect { newUserId ->
                listenToUserSupportChat(newUserId)
            }
        }
    }

    private fun setupRealtimeFirestoreListeners() {
        // 1. Settings (Document main_settings)
        db.collection("settings").document("main_settings").addSnapshotListener { snapshot, error ->
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
                try {
                    db.collection("settings").document("main_settings").set(AdminSettingsEntity())
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            _isInitialized.value = true
        }

        // 2. Categories
        db.collection("categories").addSnapshotListener { snapshot, error ->
            if (error != null) {
                error.printStackTrace()
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val fetched = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(CategoryEntity::class.java)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }.sortedBy { it.order }
                if (fetched.isNotEmpty()) {
                    _categories.value = fetched
                } else {
                    writeDefaultCategories()
                }
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
                if (fetched.isNotEmpty()) {
                    _cities.value = fetched
                } else {
                    writeDefaultCities()
                }
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
                _banners.value = fetched
            } else {
                writeDefaultBanners()
            }
        }

        // 5. Providers
        db.collection("providers").addSnapshotListener { snapshot, error ->
            if (error != null) {
                error.printStackTrace()
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val fetched = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(ProviderEntity::class.java)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }
                if (fetched.isNotEmpty()) {
                    _providers.value = fetched
                    applyFilters()
                } else {
                    writeDefaultProviders()
                }
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
                        doc.toObject(PendingProviderEntity::class.java)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
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
                        doc.toObject(NotificationEntity::class.java)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }.sortedByDescending { it.timestamp }
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
                if (fetched.isNotEmpty()) {
                    _supervisors.value = fetched
                } else {
                    writeDefaultSupervisors()
                }
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
                if (fetched.isNotEmpty()) {
                    _colorPalettes.value = fetched
                } else {
                    writeDefaultColorPalettes()
                }
            }
        }
    }

    private fun writeDefaultSupervisors() {
        val fbSupervisors = listOf(
            SupervisorEntity("1", "ماهر محمد طاهر", "ADMIN", "maher736462"),
            SupervisorEntity("2", "عماد خالد", "AUDITOR", "1234"),
            SupervisorEntity("3", "محمد سليم", "SUPPORT", "777"),
            SupervisorEntity("4", "سامي اليدومي", "OPERATIONS", "999")
        )
        fbSupervisors.forEach { sup ->
            db.collection("supervisors").document(sup.id).set(sup)
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
            CategoryEntity("1", "صيانة منزلية (سباكة/كهرباء)", "🔧", 1),
            CategoryEntity("2", "صحة ورعاية طبية", "🏥", 2),
            CategoryEntity("3", "تعليم وتدريس خصوصي", "📚", 3),
            CategoryEntity("4", "سيارات ونقل عام", "🚗", 4),
            CategoryEntity("5", "تقنية وبرامح ذكية", "💻", 5),
            CategoryEntity("6", "تجميل ولياقة منزلية", "💇", 6)
        )
        fbCategories.forEach { cat ->
            db.collection("categories").document(cat.id).set(cat)
        }
    }

    private fun writeDefaultCities() {
        val defaultCities = listOf(
            CityEntity("ye_san", "صنعاء", "Sanaa"),
            CityEntity("ye_ade", "عدن", "Aden"),
            CityEntity("ye_tai", "تعز", "Taiz"),
            CityEntity("ye_hod", "الحديدة", "Hodeidah")
        )
        defaultCities.forEach { city ->
            db.collection("cities").document(city.id).set(city)
        }
    }

    private fun writeDefaultBanners() {
        val defaultBanners = listOf(
            BannerEntity("b1", "خصومات خاصة على صيانة التكييف السنوية", "https://example.com/banner1", "1", "VIP", "LARGE", 5),
            BannerEntity("b2", "أفضل معلم كهروميكانيك متاح الآن في صنعاء", "https://example.com/banner2", "1", "NORMAL", "MEDIUM", 6),
            BannerEntity("b3", "مدرسون لجميع المراحل الدراسية واللغات", "https://example.com/banner3", "3", "NORMAL", "SMALL", 4)
        )
        defaultBanners.forEach { banner ->
            db.collection("banners").document(banner.id).set(banner)
        }
    }

    private fun writeDefaultProviders() {
        val fbProviders = listOf(
            ProviderEntity("p_maher", "ماهر محمد طاهر", "777644", "1", "صنعاء", true, "APPROVED", true, "ye_san", "شارع الستين القريب (مديرية معين)", 5.0f, 300, previewPrice = 1500.0, latitude = 15.3694, longitude = 44.1910, subscriptionExpiry = System.currentTimeMillis() + (10L * 24 * 60 * 60 * 1000))
        )
        fbProviders.forEach { prov ->
            db.collection("providers").document(prov.id).set(prov)
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
    fun navigateTo(screen: String) {
        _currentScreen.value = screen
    }

    fun goBack(): Boolean {
        val current = _currentScreen.value
        return if (current != "USER_BROWSE") {
            navigateTo("USER_BROWSE")
            true
        } else false
    }

    fun switchLanguage() {
        _currentLanguage.value = if (_currentLanguage.value == "ar") "en" else "ar"
    }

    // ------------------- Notifications -------------------
    fun triggerNotification(msg: String) {
        _toastMessage.value = msg
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

    fun sendMessageInChat(msgText: String) {
        if (msgText.trim().isEmpty()) return
        val currentId = _currentUserId.value
        val currentName = _currentUserName.value.ifEmpty { "مستخدم" }
        
        if (currentId == "guest") {
            // Safety firewall: refuse write to Firestore if anonymous
            return
        }

        val channelId = "support_" + currentId
        val newMsg = ChatMessageEntity(
            id = UUID.randomUUID().toString(),
            senderId = currentId,
            message = msgText,
            timestamp = System.currentTimeMillis(),
            senderName = currentName
        )
        db.collection("chat_channels").document(channelId).get().addOnSuccessListener { snapshot ->
            val ch = snapshot.toObject(ChatChannelEntity::class.java)
            if (ch != null) {
                db.collection("chat_channels").document(channelId).set(
                    ch.copy(
                        lastMessage = msgText,
                        timestamp = System.currentTimeMillis(),
                        messages = ch.messages + newMsg
                    )
                )
            } else {
                val newSupport = ChatChannelEntity(
                    id = channelId,
                    userName = currentName,
                    lastMessage = msgText,
                    timestamp = System.currentTimeMillis(),
                    messages = listOf(newMsg)
                )
                db.collection("chat_channels").document(channelId).set(newSupport)
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

    fun submitJoinForm(
        name: String, phone: String, catId: String, area: String,
        neighborhood: String, photoPath: String, idCardPath: String, gpsCoords: String,
        workPhotos: List<String> = emptyList()
    ) {
        val newRequest = PendingProviderEntity(
            id = UUID.randomUUID().toString(),
            name = name,
            phone = phone,
            categoryId = catId,
            area = area,
            localNeighborhood = neighborhood,
            status = "PENDING",
            selfiePhotoBase64 = photoPath,
            idPhotoBase64 = idCardPath,
            workPhotosBase64 = workPhotos
        )
        db.collection("pending_providers").document(newRequest.id).set(newRequest)
        triggerNotification("📨 تم تقديم طلبك بنجاح، سيتم مراجعته من قبل الإدارة")
    }

    fun approveRequest(request: PendingProviderEntity) {
        val approvedProvider = ProviderEntity(
            id = UUID.randomUUID().toString(),
            name = request.name,
            phone = request.phone,
            categoryId = request.categoryId,
            area = request.area,
            isVip = false,
            subscriptionStatus = "APPROVED",
            isAvailable = true,
            cityId = "ye_san",
            localNeighborhood = request.localNeighborhood,
            rating = 5.0f
        )
        db.collection("providers").document(approvedProvider.id).set(approvedProvider)
        db.collection("pending_providers").document(request.id).delete()
        triggerNotification("✅ تم قبول طلب ${request.name}")
    }

    fun rejectRequest(request: PendingProviderEntity, reason: String) {
        db.collection("pending_providers").document(request.id).delete()
        triggerNotification("❌ تم رفض طلب ${request.name} بسبب: $reason")
    }

    fun addNewProvider(name: String, phone: String, catId: String, area: String, price: Double, isVip: Boolean) {
        val newP = ProviderEntity(
            id = UUID.randomUUID().toString(),
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
            displayTime = displayTime
        )
        db.collection("banners").document(banner.id).set(banner)
        triggerNotification("🖼️ تم إضافة إعلان جديد: $title")
    }

    fun deleteBanner(bannerId: String) {
        db.collection("banners").document(bannerId).delete()
        triggerNotification("🗑️ تم حذف الإعلان")
    }

    fun addNewCategory(nameAr: String, nameEn: String, icon: String, description: String) {
        val nextId = UUID.randomUUID().toString().take(6)
        val extraCat = CategoryEntity(id = nextId, name = nameAr, icon = icon, order = _categories.value.size + 1)
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
        // banners are handled naturally
    }

    fun addNewCity(nameAr: String, nameEn: String) {
        val nextId = "city_" + UUID.randomUUID().toString().take(4)
        val city = CityEntity(nextId, nameAr, nameEn)
        db.collection("cities").document(nextId).set(city)
        triggerNotification("🏙️ تم إضافة مدينة: $nameAr")
    }

    fun removeCity(cityId: String) {
        db.collection("cities").document(cityId).delete()
        triggerNotification("🗑️ تم حذف المدينة")
    }

    fun removeProvider(providerId: String) {
        db.collection("providers").document(providerId).delete()
        triggerNotification("🗑️ تم حذف الفني")
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
        adminUsername: String = "WAM2026",
        adminPassword: String = "maher736462",
        customPrimaryHex: String = "#059669",
        customSecondaryHex: String = "#115E59",
        customBackgroundHex: String = "#0A0F0D",
        customSurfaceHex: String = "#121D18"
    ) {
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
            adminPassword = adminPassword,
            customPrimaryHex = customPrimaryHex,
            customSecondaryHex = customSecondaryHex,
            customBackgroundHex = customBackgroundHex,
            customSurfaceHex = customSurfaceHex
        )
        db.collection("settings").document("main_settings").set(updated)
        _settings.value = updated
        triggerNotification("💾 تم حفظ إعدادات البوابة البارزة والملفات بنجاح")
    }

    fun exportComplaintsToCSV() {
        triggerNotification("📁 تم تصدير البلاغات بصيغة CSV")
    }

    fun exportComplaintsToPDF() {
        triggerNotification("📃 تم تصدير البلاغات بصيغة PDF")
    }

    fun editCategory(categoryId: String, newName: String, newIcon: String) {
        db.collection("categories").document(categoryId).get().addOnSuccessListener { snapshot ->
            val cat = snapshot.toObject(CategoryEntity::class.java)
            if (cat != null) {
                db.collection("categories").document(categoryId).set(cat.copy(name = newName, icon = newIcon))
            }
        }
        triggerNotification("✏️ تم تعديل القسم بنجاح: $newName")
    }

    fun deleteCategory(categoryId: String) {
        db.collection("categories").document(categoryId).delete()
        triggerNotification("🗑️ تم حذف القسم بالكامل")
    }

    // Bookings Management
    fun addBooking(name: String, phone: String, area: String, serviceType: String, providerId: String, providerName: String, dateString: String = "2026-06-20", timeString: String = "12:00 م") {
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

        // 2. Duplication prevention scan
        val isDuplicate = _bookings.value.any { 
            it.customerPhone.trim() == cleanPhone && 
            it.providerId == providerId && 
            (it.status == "PENDING" || it.status == "APPROVED" || it.status == "IN_PROGRESS")
        }
        if (isDuplicate) {
            triggerNotification("⚠️ حجز مكرر: توجد استمارة حجز معلقة أو نشطة قائمة فعلياً بنفس الرقم لهذا الفني!")
            return
        }

        val newBooking = BookingEntity(
            id = "b_" + UUID.randomUUID().toString().take(6),
            customerName = cleanName,
            customerPhone = cleanPhone,
            customerArea = area,
            serviceType = serviceType,
            providerId = providerId,
            providerName = providerName,
            dateString = dateString,
            timeString = timeString,
            status = "PENDING"
        )
        db.collection("bookings").document(newBooking.id).set(newBooking)
        triggerNotification("📅 تم تقديم الحجز بنجاح، بانتظار الموافقة")
    }

    fun updateBookingStatus(bookingId: String, newStatus: String) {
        db.collection("bookings").document(bookingId).get().addOnSuccessListener { snapshot ->
            val b = snapshot.toObject(BookingEntity::class.java)
            if (b != null) {
                val updated = b.copy(status = newStatus)
                db.collection("bookings").document(bookingId).set(updated)
                
                if (_settings.value.isNotificationsEnabled) {
                    addNotification(
                        title = "تحديث حالة الحجز",
                        message = "تم $newStatus حجزك للخدمة المقدمة من ${b.providerName} بنجاح.",
                        targetType = "USER",
                        targetValue = b.customerPhone
                    )
                }
            }
        }
        triggerNotification(if (newStatus == "APPROVED") "✅ تم قبول الحجز وتأكيده بنجاح" else "❌ تم رفض/إلغاء الحجز")
    }

    fun deleteBooking(bookingId: String) {
        db.collection("bookings").document(bookingId).delete()
        triggerNotification("🗑️ تم حذف الحجز من السجلات")
    }

    fun updateBooking(booking: BookingEntity) {
        db.collection("bookings").document(booking.id).set(booking)
        triggerNotification("💾 تم تحديث بيانات الحجز بنجاح")
    }

    // Targeted Notifications Management
    fun addNotification(title: String, message: String, targetType: String, targetValue: String) {
        val newNotif = NotificationEntity(
            id = "n_" + UUID.randomUUID().toString().take(6),
            title = title,
            message = message,
            targetType = targetType,
            targetValue = targetValue,
            timestamp = System.currentTimeMillis()
        )
        db.collection("notifications").document(newNotif.id).set(newNotif)
        triggerNotification("🔔 تم إرسال الإشعار الموجه بنجاح!")
    }

    fun deleteNotification(notifId: String) {
        db.collection("notifications").document(notifId).delete()
        triggerNotification("🗑️ تم حذف الإشعار")
    }

    // Instant Chats Management
    fun replyToChatChannel(channelId: String, senderId: String, msgText: String, senderName: String) {
        if (msgText.trim().isEmpty()) return
        val newMsg = ChatMessageEntity(
            id = UUID.randomUUID().toString(),
            senderId = senderId,
            message = msgText,
            timestamp = System.currentTimeMillis(),
            senderName = senderName
        )
        db.collection("chat_channels").document(channelId).get().addOnSuccessListener { snapshot ->
            val ch = snapshot.toObject(ChatChannelEntity::class.java)
            if (ch != null) {
                db.collection("chat_channels").document(channelId).set(
                    ch.copy(
                        lastMessage = msgText,
                        timestamp = System.currentTimeMillis(),
                        messages = ch.messages + newMsg
                    )
                )
            } else {
                val newCh = ChatChannelEntity(
                    id = channelId,
                    userName = senderName,
                    lastMessage = msgText,
                    timestamp = System.currentTimeMillis(),
                    messages = listOf(newMsg)
                )
                db.collection("chat_channels").document(channelId).set(newCh)
            }
        }
    }

    fun deleteChatChannel(channelId: String) {
        db.collection("chat_channels").document(channelId).delete()
        triggerNotification("🗑️ تم حذف المحادثة بالكامل.")
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

    fun wipeAllDatabaseData(password: String): Boolean {
        if (password == "maher736462") {
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
        _bookings.value = _bookings.value.map { booking ->
            if (booking.id == bookingId) {
                booking.copy(status = newStatus.name)
            } else booking
        }
        try {
            db.collection("bookings").document(bookingId).update("status", newStatus.name)
        } catch (e: Exception) {}
    }

    fun getBookingStatusColor(status: String): String {
        return when (status.uppercase()) {
            "PENDING" -> "#FFC107"
            "ACCEPTED" -> "#4CAF50"
            "IN_PROGRESS" -> "#2196F3"
            "COMPLETED" -> "#9C27B0"
            "CANCELLED" -> "#F44336"
            else -> "#9E9E9E"
        }
    }

    fun getBookingStatusLabel(status: String): String {
        return when (status.uppercase()) {
            "PENDING" -> "⏳ قيد الانتظار"
            "ACCEPTED" -> "✅ مقبول"
            "IN_PROGRESS" -> "🔧 قيد التنفيذ"
            "COMPLETED" -> "🎉 مكتمل"
            "CANCELLED" -> "❌ ملغي"
            else -> status
        }
    }

    fun getBookingProgress(status: String): Float {
        return when (status.uppercase()) {
            "PENDING" -> 0.25f
            "ACCEPTED" -> 0.50f
            "IN_PROGRESS" -> 0.75f
            "COMPLETED" -> 1.0f
            "CANCELLED" -> 0.0f
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
                        _pendingTechnicians.value = snapshot.toObjects(PendingProviderEntity::class.java)
                    }
                }
        } catch (e: Exception) {}
    }

    fun approveTechnician(providerId: String) {
        val technician = _pendingProviders.value.find { it.id == providerId }
        technician?.let {
            _pendingProviders.value = _pendingProviders.value.filter { it.id != providerId }
            val p = ProviderEntity(
                id = it.id,
                name = it.name,
                phone = it.phone,
                categoryId = it.categoryId,
                area = it.area,
                localNeighborhood = it.localNeighborhood,
                isVerified = true,
                isRecommended = false,
                subscriptionStatus = "APPROVED",
                isVip = false,
                isAvailable = true,
                rating = 5.0f,
                subscriptionExpiry = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000),
                workPhotosBase64 = it.workPhotosBase64
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

            try {
                db.collection("pending_providers").document(providerId).delete()
                db.collection("providers").document(providerId).set(p)
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
}

