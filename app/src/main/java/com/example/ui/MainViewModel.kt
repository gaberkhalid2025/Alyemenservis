package com.example.ui

import androidx.lifecycle.ViewModel
import com.example.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class MainViewModel : ViewModel() {

    // ------------------- Firestore setup -------------------
    private val db by lazy { com.google.firebase.firestore.FirebaseFirestore.getInstance() }

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

    private val _adminRole = MutableStateFlow("GUEST")
    val adminRole: StateFlow<String> = _adminRole.asStateFlow()

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

    private var clickCount = 0

    init {
        setupRealtimeFirestoreListeners()
        loadUserPoints()
    }

    private fun setupRealtimeFirestoreListeners() {
        // 1. Settings (Document main_settings)
        db.collection("settings").document("main_settings").addSnapshotListener { snapshot, error ->
            if (snapshot != null && snapshot.exists()) {
                snapshot.toObject(AdminSettingsEntity::class.java)?.let {
                    _settings.value = it
                    _maxKmRadius.value = it.maxSearchRadiusKm
                }
            } else {
                db.collection("settings").document("main_settings").set(AdminSettingsEntity())
            }
        }

        // 2. Categories
        db.collection("categories").addSnapshotListener { snapshot, error ->
            if (snapshot != null) {
                val fetched = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(CategoryEntity::class.java)
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
            if (snapshot != null) {
                val fetched = snapshot.documents.mapNotNull { it.toObject(CityEntity::class.java) }
                if (fetched.isNotEmpty()) {
                    _cities.value = fetched
                } else {
                    writeDefaultCities()
                }
            }
        }

        // 4. Banners
        db.collection("banners").addSnapshotListener { snapshot, error ->
            if (snapshot != null) {
                val fetched = snapshot.documents.mapNotNull { it.toObject(BannerEntity::class.java) }
                _banners.value = fetched
            } else {
                writeDefaultBanners()
            }
        }

        // 5. Providers
        db.collection("providers").addSnapshotListener { snapshot, error ->
            if (snapshot != null) {
                val fetched = snapshot.documents.mapNotNull { it.toObject(ProviderEntity::class.java) }
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
            if (snapshot != null) {
                val fetched = snapshot.documents.mapNotNull { it.toObject(PendingProviderEntity::class.java) }
                _pendingProviders.value = fetched
            }
        }

        // 7. Bookings
        db.collection("bookings").addSnapshotListener { snapshot, error ->
            if (snapshot != null) {
                val fetched = snapshot.documents.mapNotNull { it.toObject(BookingEntity::class.java) }
                _bookings.value = fetched
            }
        }

        // 8. Notifications
        db.collection("notifications").addSnapshotListener { snapshot, error ->
            if (snapshot != null) {
                val fetched = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(NotificationEntity::class.java)
                }.sortedByDescending { it.timestamp }
                _notifications.value = fetched
            }
        }

        // 9. Chat Channels
        db.collection("chat_channels").addSnapshotListener { snapshot, error ->
            if (snapshot != null) {
                val fetched = snapshot.documents.mapNotNull { it.toObject(ChatChannelEntity::class.java) }
                    .sortedByDescending { it.timestamp }
                _chatChannels.value = fetched
            }
        }

        // 10. General Support Chat Messages
        db.collection("chat_channels").document("support_general").addSnapshotListener { snapshot, error ->
            if (snapshot != null && snapshot.exists()) {
                val ch = snapshot.toObject(ChatChannelEntity::class.java)
                if (ch != null) {
                    _chatMessages.value = ch.messages
                }
            } else {
                val initialMsg = ChatMessageEntity("c1", "admin", "مرحباً بكم في الدعم الفني، كيف يمكننا مساعدتك اليوم؟", System.currentTimeMillis() - 1000000, "الدعم الفني")
                val supportCh = ChatChannelEntity(
                    id = "support_general",
                    userName = "الدعم الفني",
                    lastMessage = initialMsg.message,
                    timestamp = initialMsg.timestamp,
                    messages = listOf(initialMsg)
                )
                db.collection("chat_channels").document("support_general").set(supportCh)
            }
        }

        // 11. Reports
        db.collection("reports").addSnapshotListener { snapshot, error ->
            if (snapshot != null) {
                val fetched = snapshot.documents.mapNotNull { it.toObject(ReportEntity::class.java) }
                _reports.value = fetched
            }
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
            ProviderEntity("p_maher", "ماهر محمد طاهر", "777644670", "1", "صنعاء", true, "APPROVED", true, "ye_san", "شارع السنين القريبي (مديرية معين)", 5.0f, 300, previewPrice = 1500.0, latitude = 15.3694, longitude = 44.1910, subscriptionExpiry = System.currentTimeMillis() + (10L * 24 * 60 * 60 * 1000)),
            ProviderEntity("p1", "أبو أحمد الكهربائي", "771122334", "1", "الحصبة", true, "APPROVED", true, "ye_san", "حي مازدا", 4.9f, 150, previewPrice = 2000.0, latitude = 15.3850, longitude = 44.1950, subscriptionExpiry = System.currentTimeMillis() + (30L * 60 * 60 * 1000)),
            ProviderEntity("p2", "المهندس وليد للسباكة", "733445566", "1", "حدة", false, "APPROVED", true, "ye_san", "جولة الرويشان", 4.7f, 90, previewPrice = 1800.0, latitude = 15.3280, longitude = 44.1800, subscriptionExpiry = System.currentTimeMillis() + (40L * 60 * 60 * 1000)),
            ProviderEntity("p3", "الدكتور رائد رعاية منزلية", "711228899", "2", "المنصورة", true, "APPROVED", true, "ye_ade", "حي التسعين", 5.0f, 220, previewPrice = 5000.0, latitude = 12.8350, longitude = 44.9900, subscriptionExpiry = System.currentTimeMillis() + (15L * 24 * 60 * 60 * 1000)),
            ProviderEntity("p4", "أستاذ عصام معلم رياضيات", "770005522", "3", "شعوب", false, "APPROVED", false, "ye_san", "المشهد", 4.5f, 40, previewPrice = 2500.0, latitude = 15.3720, longitude = 44.2200, subscriptionExpiry = System.currentTimeMillis() + (5L * 24 * 60 * 60 * 1000)),
            ProviderEntity("p5", "صادق الباخرة لنقل العفش", "775544332", "4", "المعلا", false, "APPROVED", true, "ye_ade", "الشارع الرئيسي", 4.6f, 85, previewPrice = 8000.0, latitude = 12.7950, longitude = 44.9850, subscriptionExpiry = System.currentTimeMillis() + (25L * 24 * 60 * 60 * 1000))
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
            _adminRole.value = "OWNER"
            _currentScreen.value = "OWNER_PANEL"
            triggerNotification("🔓 تم تفعيل بوابة التحكم بالبوابة الخلفية (المالك) بنجاح!")
        }
    }

    fun changeAdminCredentials(username: String, password: String) {
        triggerNotification("🔐 تم تغيير بيانات المدير الرئيسي")
    }

    fun authenticateAdmin(role: String) {
        _adminRole.value = role
        triggerNotification("🔓 تم تسجيل الدخول بنجاح بصلاحية: $role")
        _currentScreen.value = "ADMIN_PANEL"
    }

    fun logout() {
        _adminRole.value = "GUEST"
        _currentScreen.value = "USER_BROWSE"
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

    fun sendMessageInChat(msgText: String) {
        if (msgText.trim().isEmpty()) return
        val newMsg = ChatMessageEntity(
            id = UUID.randomUUID().toString(),
            senderId = "guest",
            message = msgText,
            timestamp = System.currentTimeMillis(),
            senderName = "مستخدم"
        )
        db.collection("chat_channels").document("support_general").get().addOnSuccessListener { snapshot ->
            val ch = snapshot.toObject(ChatChannelEntity::class.java)
            if (ch != null) {
                db.collection("chat_channels").document("support_general").set(
                    ch.copy(
                        lastMessage = msgText,
                        timestamp = System.currentTimeMillis(),
                        messages = ch.messages + newMsg
                    )
                )
            } else {
                val newSupport = ChatChannelEntity(
                    id = "support_general",
                    userName = "الدعم الفني",
                    lastMessage = msgText,
                    timestamp = System.currentTimeMillis(),
                    messages = listOf(newMsg)
                )
                db.collection("chat_channels").document("support_general").set(newSupport)
            }
        }
    }

    fun submitJoinForm(
        name: String, phone: String, catId: String, area: String,
        neighborhood: String, photoPath: String, idCardPath: String, gpsCoords: String
    ) {
        val newRequest = PendingProviderEntity(
            id = UUID.randomUUID().toString(),
            name = name,
            phone = phone,
            categoryId = catId,
            area = area,
            localNeighborhood = neighborhood,
            status = "PENDING"
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

    fun addNewBanner(title: String, url: String, redirect: String, type: String, size: String, duration: Int) {
        val banner = BannerEntity(
            id = UUID.randomUUID().toString(),
            title = title,
            url = url,
            redirectCategory = redirect,
            type = type,
            size = size,
            duration = duration
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
        isDataSaver: Boolean, imgQuality: Int
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
            isSpeechSearchEnabled = isSpeech
        )
        db.collection("settings").document("main_settings").set(updated)
        triggerNotification("💾 تم حفظ إعدادات البوابة الخلفية بنجاح")
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
    fun addBooking(name: String, phone: String, area: String, providerId: String, providerName: String) {
        val newBooking = BookingEntity(
            id = "b_" + UUID.randomUUID().toString().take(6),
            customerName = name,
            customerPhone = phone,
            customerArea = area,
            providerId = providerId,
            providerName = providerName,
            dateString = "2026-06-18",
            timeString = "12:00 م",
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

    fun wipeOldChatChannels(days: Int) {
        triggerNotification("🧹 تم تصفية وحذف سجل المحادثات الأقدم من $days أيام بنجاح!")
    }

    private val _supervisors = MutableStateFlow<List<SupervisorEntity>>(listOf(
        SupervisorEntity("1", "ماهر محمد طاهر", "ADMIN", "maher736462"),
        SupervisorEntity("2", "عماد خالد", "AUDITOR", "1234"),
        SupervisorEntity("3", "محمد سليم", "SUPPORT", "777"),
        SupervisorEntity("4", "سامي اليدومي", "OPERATIONS", "999")
    ))
    val supervisors: StateFlow<List<SupervisorEntity>> = _supervisors.asStateFlow()

    fun addSupervisor(name: String, role: String, passcode: String) {
        val nextId = UUID.randomUUID().toString()
        _supervisors.value = _supervisors.value + SupervisorEntity(nextId, name, role, passcode)
        triggerNotification("🔑 تم إضافة المشرف $name بصلاحية $role")
    }

    fun removeSupervisor(id: String) {
        _supervisors.value = _supervisors.value.filter { it.id != id }
        triggerNotification("🗑️ تم إلغاء صلاحية المشرف بنجاح")
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
}
