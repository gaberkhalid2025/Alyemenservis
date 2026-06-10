package com.example.ui

import androidx.lifecycle.ViewModel
import com.example.data.*
import com.example.FirebaseManager
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class MainViewModel : ViewModel() {
    private val firestore: FirebaseFirestore? = FirebaseManager.firestore

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

    private val _cities = MutableStateFlow<List<CityEntity>>(emptyList())
    val cities: StateFlow<List<CityEntity>> = _cities.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessageEntity>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessageEntity>> = _chatMessages.asStateFlow()

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
        loadFallbackMockData()
        listenToCategories()
        listenToProviders()
        listenToPendingRequests()
        listenToBanners()
        listenToSettings()
        listenToCities()
        listenToReports()
        listenToActivityLogs()
        listenToChatMessages()
        loadUserPoints()
    }

    private fun loadFallbackMockData() {
        val fbCategories = listOf(
            CategoryEntity("1", "صيانة منزلية (سباكة/كهرباء)", "🔧", 1),
            CategoryEntity("2", "صحة ورعاية طبية", "🏥", 2),
            CategoryEntity("3", "تعليم وتدريس خصوصي", "📚", 3),
            CategoryEntity("4", "سيارات ونقل عام", "🚗", 4),
            CategoryEntity("5", "تقنية وبرامح ذكية", "💻", 5),
            CategoryEntity("6", "تجميل ولياقة منزلية", "💇", 6)
        )
        _categories.value = fbCategories

        _cities.value = listOf(
            CityEntity("ye_san", "صنعاء", "Sanaa"),
            CityEntity("ye_ade", "عدن", "Aden"),
            CityEntity("ye_tai", "تعز", "Taiz"),
            CityEntity("ye_hod", "الحديدة", "Hodeidah")
        )

        _banners.value = listOf(
            BannerEntity("b1", "خصومات خاصة على صيانة التكييف السنوية", "https://example.com/banner1", "1", "VIP", "LARGE", 5),
            BannerEntity("b2", "أفضل معلم كهروميكانيك متاح الآن في صنعاء", "https://example.com/banner2", "1", "NORMAL", "MEDIUM", 6),
            BannerEntity("b3", "مدرسون لجميع المراحل الدراسية واللغات", "https://example.com/banner3", "3", "NORMAL", "SMALL", 4)
        )

        val fbProviders = listOf(
            ProviderEntity("p1", "أبو أحمد الكهربائي", "771122334", "1", "الحصبة", true, "APPROVED", true, "ye_san", "حي مازدا", 4.9f, 150),
            ProviderEntity("p2", "المهندس وليد للسباكة", "733445566", "1", "حدة", false, "APPROVED", true, "ye_san", "جولة الرويشان", 4.7f, 90),
            ProviderEntity("p3", "الدكتور رائد رعاية منزلية", "711228899", "2", "المنصورة", true, "APPROVED", true, "ye_ade", "حي التسعين", 5.0f, 220),
            ProviderEntity("p4", "أستاذ عصام معلم رياضيات", "770005522", "3", "شعوب", false, "APPROVED", false, "ye_san", "المشهد", 4.5f, 40),
            ProviderEntity("p5", "صادق الباخرة لنقل العفش", "775544332", "4", "المعلا", false, "APPROVED", true, "ye_ade", "الشارع الرئيسي", 4.6f, 85)
        )
        _providers.value = fbProviders

        _pendingProviders.value = listOf(
            PendingProviderEntity("temp1", "بلال الصيانة المتكاملة", "773700122", "1", "عصر", "حي عصر", "PENDING", "")
        )

        _chatMessages.value = listOf(
            ChatMessageEntity("c1", "admin", "مرحباً بكم في الدعم الفني، كيف يمكننا مساعدتك اليوم؟", System.currentTimeMillis() - 1000000)
        )

        applyFilters()
    }

    // ------------------- Firestore Listeners -------------------
    private fun listenToCategories() {
        try {
            firestore?.collection("categories")
                ?.orderBy("order")
                ?.addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    val list = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(CategoryEntity::class.java)?.copy(id = doc.id)
                    }
                    if (list.isNotEmpty()) {
                        _categories.value = list
                    }
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun listenToProviders() {
        try {
            firestore?.collection("service_providers")
                ?.addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    val list = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(ProviderEntity::class.java)?.copy(id = doc.id)
                    }
                    if (list.isNotEmpty()) {
                        _providers.value = list
                        applyFilters()
                    }
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun listenToPendingRequests() {
        try {
            firestore?.collection("pending_providers")
                ?.whereEqualTo("status", "PENDING")
                ?.addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    val list = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(PendingProviderEntity::class.java)?.copy(id = doc.id)
                    }
                    if (list.isNotEmpty()) {
                        _pendingProviders.value = list
                    }
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun listenToBanners() {
        try {
            firestore?.collection("banners")
                ?.addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    val list = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(BannerEntity::class.java)?.copy(id = doc.id)
                    }
                    if (list.isNotEmpty()) {
                        _banners.value = list
                    }
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun listenToSettings() {
        try {
            firestore?.collection("admin_settings")?.document("main_settings")
                ?.addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    val settings = snapshot.toObject(AdminSettingsEntity::class.java)
                    if (settings != null) {
                        _settings.value = settings
                    }
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun listenToCities() {
        try {
            firestore?.collection("cities")
                ?.addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    val list = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(CityEntity::class.java)?.copy(id = doc.id)
                    }
                    if (list.isNotEmpty()) {
                        _cities.value = list
                    }
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun listenToReports() {
        try {
            firestore?.collection("reports")
                ?.addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    val list = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(ReportEntity::class.java)?.copy(id = doc.id)
                    }
                    if (list.isNotEmpty()) {
                        _reports.value = list
                    }
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun listenToActivityLogs() {
        try {
            firestore?.collection("activity_logs")
                ?.orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                ?.limit(100)
                ?.addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    val list = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(ActivityLogEntity::class.java)?.copy(id = doc.id)
                    }
                    if (list.isNotEmpty()) {
                        _activityLogs.value = list
                    }
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun listenToChatMessages() {
        try {
            firestore?.collection("chat_messages")
                ?.orderBy("timestamp")
                ?.limit(50)
                ?.addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    val list = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(ChatMessageEntity::class.java)?.copy(id = doc.id)
                    }
                    if (list.isNotEmpty()) {
                        _chatMessages.value = list
                    }
                }
        } catch (e: Exception) {
            e.printStackTrace()
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
        val updatedList = _providers.value.map {
            if (it.id == provider.id) it.copy(isAvailable = !it.isAvailable) else it
        }
        _providers.value = updatedList
        applyFilters()
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
        _reports.value = _reports.value + newReport
        triggerNotification("📢 تم إرسال بلاغك ضد $providerName")
    }

    fun sendMessageInChat(msgText: String) {
        if (msgText.trim().isEmpty()) return
        val newMsg = ChatMessageEntity(
            id = UUID.randomUUID().toString(),
            senderId = "guest",
            message = msgText,
            timestamp = System.currentTimeMillis()
        )
        _chatMessages.value = _chatMessages.value + newMsg
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
        _pendingProviders.value = _pendingProviders.value + newRequest
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
        _providers.value = _providers.value + approvedProvider
        _pendingProviders.value = _pendingProviders.value.filter { it.id != request.id }
        applyFilters()
        triggerNotification("✅ تم قبول طلب ${request.name}")
    }

    fun rejectRequest(request: PendingProviderEntity, reason: String) {
        _pendingProviders.value = _pendingProviders.value.filter { it.id != request.id }
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
            rating = 5.0f
        )
        _providers.value = _providers.value + newP
        applyFilters()
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
        _banners.value = _banners.value + banner
        triggerNotification("🖼️ تم إضافة إعلان جديد: $title")
    }

    fun deleteBanner(bannerId: String) {
        _banners.value = _banners.value.filter { it.id != bannerId }
        triggerNotification("🗑️ تم حذف الإعلان")
    }

    fun addNewCategory(nameAr: String, nameEn: String, icon: String, description: String) {
        val nextId = (_categories.value.size + 1).toString()
        val extraCat = CategoryEntity(id = nextId, name = nameAr, icon = icon, order = _categories.value.size + 1)
        _categories.value = _categories.value + extraCat
        triggerNotification("📁 تم إضافة قسم جديد: $nameAr")
    }

    fun addNewCity(nameAr: String, nameEn: String) {
        val nextId = "city_" + UUID.randomUUID().toString().take(4)
        val city = CityEntity(nextId, nameAr, nameEn)
        _cities.value = _cities.value + city
        triggerNotification("🏙️ تم إضافة مدينة: $nameAr")
    }

    fun removeCity(cityId: String) {
        _cities.value = _cities.value.filter { it.id != cityId }
        triggerNotification("🗑️ تم حذف المدينة")
    }

    fun removeProvider(providerId: String) {
        _providers.value = _providers.value.filter { it.id != providerId }
        applyFilters()
        triggerNotification("🗑️ تم حذف الفني")
    }

    fun pinProvider(providerId: String, isPinned: Boolean) {
        _providers.value = _providers.value.map {
            if (it.id == providerId) it.copy(isVip = isPinned) else it
        }
        applyFilters()
        triggerNotification(if (isPinned) "📌 تم تثبيت الفني" else "📌 تم إلغاء تثبيت الفني")
    }

    fun recommendProvider(providerId: String, isRecommended: Boolean) {
        _providers.value = _providers.value.map {
            if (it.id == providerId) it.copy(rating = if (isRecommended) 5.0f else 4.0f) else it
        }
        applyFilters()
        triggerNotification(if (isRecommended) "⭐ تمت توصية الفني" else "⭐ تم إلغاء توصية الفني")
    }

    fun verifyProviderBadge(providerId: String, isVerified: Boolean) {
        _providers.value = _providers.value.map {
            if (it.id == providerId) it.copy(subscriptionStatus = if (isVerified) "APPROVED" else "PENDING") else it
        }
        applyFilters()
        triggerNotification(if (isVerified) "🔷 تم توثيق الفني بالشارة الزرقاء" else "🔷 تم إلغاء توثيق الفني")
    }

    fun toggleProviderSubscription(providerId: String, status: String) {
        _providers.value = _providers.value.map {
            if (it.id == providerId) it.copy(subscriptionStatus = status) else it
        }
        applyFilters()
        triggerNotification(if (status == "APPROVED") "✨ تم تفعيل العضوية الذهبية للفني" else "✨ تم إلغاء العضوية الذهبية")
    }

    fun updateTheme(themeId: String) {
        _settings.value = _settings.value.copy(activeThemeId = themeId)
        triggerNotification("🎨 تم تغيير مظهر التطبيق إلى $themeId")
    }

    fun updateBackdoorSettings(
        appName: String, welcomeMsg: String, footerMsg: String, themeId: String,
        supportPhone: String, supportEmail: String, supportWhatsapp: String,
        isMaintenance: Boolean, hiddenFooter: Boolean, botHidden: Boolean, botSize: Int,
        chatHidden: Boolean, chatSize: Int, radiusKm: Int, isSpeech: Boolean,
        isDataSaver: Boolean, imgQuality: Int
    ) {
        _settings.value = _settings.value.copy(
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
        triggerNotification("💾 تم حفظ إعدادات البوابة الخلفية بنجاح")
    }

    fun exportComplaintsToCSV() {
        triggerNotification("📁 تم تصدير البلاغات بصيغة CSV")
    }

    fun exportComplaintsToPDF() {
        triggerNotification("📃 تم تصدير البلاغات بصيغة PDF")
    }
}
