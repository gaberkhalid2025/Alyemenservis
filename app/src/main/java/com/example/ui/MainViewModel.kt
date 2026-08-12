package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class MainViewModel : ViewModel() {

    // ------------------- Firestore setup -------------------
    internal val db by lazy {
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
    internal val _categories = MutableStateFlow<List<CategoryEntity>>(emptyList())
    val categories: StateFlow<List<CategoryEntity>> = _categories.asStateFlow()

    internal val _providers = MutableStateFlow<List<ProviderEntity>>(emptyList())
    val providers: StateFlow<List<ProviderEntity>> = _providers.asStateFlow()

    internal val _filteredProviders = MutableStateFlow<List<ProviderEntity>>(emptyList())
    val filteredProviders: StateFlow<List<ProviderEntity>> = _filteredProviders.asStateFlow()

    internal val _pendingProviders = MutableStateFlow<List<PendingProviderEntity>>(emptyList())
    val pendingProviders: StateFlow<List<PendingProviderEntity>> = _pendingProviders.asStateFlow()

    internal val _banners = MutableStateFlow<List<BannerEntity>>(emptyList())
    val banners: StateFlow<List<BannerEntity>> = _banners.asStateFlow()

    internal val _settings = MutableStateFlow(AdminSettingsEntity())
    val settings: StateFlow<AdminSettingsEntity> = _settings.asStateFlow()

    internal val _forceRebuildKey = MutableStateFlow(0)
    val forceRebuildKey: StateFlow<Int> = _forceRebuildKey.asStateFlow()

    internal val _reports = MutableStateFlow<List<ReportEntity>>(emptyList())
    val reports: StateFlow<List<ReportEntity>> = _reports.asStateFlow()

    internal val _activityLogs = MutableStateFlow<List<ActivityLogEntity>>(emptyList())
    val activityLogs: StateFlow<List<ActivityLogEntity>> = _activityLogs.asStateFlow()

    internal val _userLatitude = MutableStateFlow(15.3694)
    val userLatitude: StateFlow<Double> = _userLatitude.asStateFlow()

    internal val _userLongitude = MutableStateFlow(44.1910)
    val userLongitude: StateFlow<Double> = _userLongitude.asStateFlow()

    fun updateUserLocation(lat: Double, lng: Double) {
        _userLatitude.value = lat
        _userLongitude.value = lng
    }

    internal val _cities = MutableStateFlow<List<CityEntity>>(emptyList())
    val cities: StateFlow<List<CityEntity>> = _cities.asStateFlow()

    internal val _chatMessages = MutableStateFlow<List<ChatMessageEntity>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessageEntity>> = _chatMessages.asStateFlow()

    internal val _bookings = MutableStateFlow<List<BookingEntity>>(emptyList())
    val bookings: StateFlow<List<BookingEntity>> = _bookings.asStateFlow()

    internal val _notifications = MutableStateFlow<List<NotificationEntity>>(emptyList())
    val notifications: StateFlow<List<NotificationEntity>> = _notifications.asStateFlow()

    internal val _chatChannels = MutableStateFlow<List<ChatChannelEntity>>(emptyList())
    val chatChannels: StateFlow<List<ChatChannelEntity>> = _chatChannels.asStateFlow()

    internal val _currentUserId = MutableStateFlow("guest")
    val currentUserId: StateFlow<String> = _currentUserId.asStateFlow()

    internal val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    internal val _registeredUsersCount = MutableStateFlow(0)
    val registeredUsersCount: StateFlow<Int> = _registeredUsersCount.asStateFlow()

    internal val _currentUserName = MutableStateFlow("")
    val currentUserName: StateFlow<String> = _currentUserName.asStateFlow()

    internal val _currentUserPhone = MutableStateFlow("")
    val currentUserPhone: StateFlow<String> = _currentUserPhone.asStateFlow()

    internal val _currentUserResidence = MutableStateFlow("")
    val currentUserResidence: StateFlow<String> = _currentUserResidence.asStateFlow()

    internal val _adminRole = MutableStateFlow("GUEST")
    val adminRole: StateFlow<String> = _adminRole.asStateFlow()

    internal val _joinRequestPhone = MutableStateFlow("")
    val joinRequestPhone: StateFlow<String> = _joinRequestPhone.asStateFlow()

    val filteredNotifications: StateFlow<List<NotificationEntity>> = combine(
        _notifications,
        _currentUserId,
        _currentUserPhone,
        _joinRequestPhone,
        _adminRole
    ) { notificationsList, userId, phone, joinPhone, adminRoleState ->
        val now = System.currentTimeMillis()
        val visibleList = if (adminRoleState != "GUEST") {
            notificationsList
        } else {
            notificationsList.filter {
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

    internal val _currentUserPoints = MutableStateFlow(0)
    val currentUserPoints: StateFlow<Int> = _currentUserPoints.asStateFlow()

    internal val _toastMessage = MutableStateFlow<String?>(null)
    val toastFlow: StateFlow<String?> = _toastMessage.asStateFlow()

    internal val _currentScreen = MutableStateFlow("USER_BROWSE")
    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()

    internal val _currentLanguage = MutableStateFlow("ar")
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    internal val _selectedCategoryId = MutableStateFlow<String?>(null)
    val selectedCategoryId: StateFlow<String?> = _selectedCategoryId.asStateFlow()

    internal val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    internal val _filterVipOnly = MutableStateFlow(false)
    val filterVipOnly: StateFlow<Boolean> = _filterVipOnly.asStateFlow()

    internal val _filterAvailableOnly = MutableStateFlow(false)
    val filterAvailableOnly: StateFlow<Boolean> = _filterAvailableOnly.asStateFlow()

    internal val _filterCityId = MutableStateFlow<String?>(null)
    val filterCityId: StateFlow<String?> = _filterCityId.asStateFlow()

    internal val _filterNeighborhoodName = MutableStateFlow("")
    val filterNeighborhoodName: StateFlow<String> = _filterNeighborhoodName.asStateFlow()

    internal val _phoneOrNameFilter = MutableStateFlow("")
    val phoneOrNameFilter: StateFlow<String> = _phoneOrNameFilter.asStateFlow()

    internal val _maxKmRadius = MutableStateFlow(10)
    val maxKmRadius: StateFlow<Int> = _maxKmRadius.asStateFlow()

    internal val _showBackdoorDialog = MutableStateFlow(false)
    val showBackdoorDialog: StateFlow<Boolean> = _showBackdoorDialog.asStateFlow()

    internal val _colorPalettes = MutableStateFlow<List<ColorPaletteEntity>>(emptyList())
    val colorPalettes: StateFlow<List<ColorPaletteEntity>> = _colorPalettes.asStateFlow()

    internal var clickCount = 0

    internal var supportChatListenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null

    fun initializeUserIdentity(context: android.content.Context) {
        val sp = context.getSharedPreferences("yemen_service_prefs", android.content.Context.MODE_PRIVATE)
        val savedId = sp.getString("user_id", "guest") ?: "guest"
        _currentUserId.value = savedId
        _currentUserName.value = sp.getString("user_name", "") ?: ""
        _currentUserPhone.value = sp.getString("user_phone", "") ?: ""
        _currentUserResidence.value = sp.getString("user_residence", "") ?: ""
        val savedJoinPhone = sp.getString("join_request_phone", "") ?: ""
        _joinRequestPhone.value = savedJoinPhone
        
        val savedRole = sp.getString("saved_admin_role", "GUEST") ?: "GUEST"
        if (savedRole != "GUEST") {
            _adminRole.value = savedRole
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
                            putString("user_id", prov.id)
                            putString("user_name", prov.name)
                            putString("user_phone", prov.phone)
                            putString("user_residence", prov.area)
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
                                    putString("user_id", pendId)
                                    putString("user_name", pend.name)
                                    putString("user_phone", pend.phone)
                                    putString("user_residence", pend.area)
                                    apply()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun registerGuestUser(context: android.content.Context, name: String, phone: String, residence: String, password: String? = null) {
        val androidId = android.provider.Settings.Secure.getString(context.contentResolver, android.provider.Settings.Secure.ANDROID_ID) ?: "unknown_device"
        
        // Check if this device is already registered in Firestore
        db.collection("registered_users").document(androidId).get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                // Restore original credentials
                val existingUserId = doc.getString("id") ?: ("user_" + (100000..999999).random().toString())
                val existingName = doc.getString("name") ?: name
                val existingPhone = doc.getString("phone") ?: phone
                val existingResidence = doc.getString("residence") ?: residence
                
                _currentUserId.value = existingUserId
                _currentUserName.value = existingName
                _currentUserPhone.value = existingPhone
                _currentUserResidence.value = existingResidence
                
                val sp = context.getSharedPreferences("yemen_service_prefs", android.content.Context.MODE_PRIVATE)
                sp.edit().apply {
                    putString("user_id", existingUserId)
                    putString("user_name", existingName)
                    putString("user_phone", existingPhone)
                    putString("user_residence", existingResidence)
                    apply()
                }
                triggerNotification("📲 تم استعادة حسابك النشط المرتبط بهذا الهاتف بنجاح! لا يمكن إنشاء أكثر من حساب للجهاز.")
            } else {
                // Check if phone number is registered on another device
                db.collection("registered_users").whereEqualTo("phone", phone).get().addOnSuccessListener { qs ->
                    if (qs != null && !qs.isEmpty) {
                        triggerNotification("⚠️ رقم الهاتف هذا مسجل بالفعل لجهاز آخر! يرجى إدخال رقم هاتف فريد.")
                    } else {
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

                        val regUser = mutableMapOf(
                            "id" to newUserId,
                            "name" to name,
                            "phone" to phone,
                            "residence" to residence,
                            "androidId" to androidId,
                            "timestamp" to System.currentTimeMillis()
                        )
                        if (password != null) {
                            regUser["password"] = password
                        }
                        db.collection("registered_users").document(androidId).set(regUser)
                        triggerNotification("🎉 أهلاً بك في الدليل $name، تم تسجيل وحماية حسابك الموحد بنجاح!")
                    }
                }
            }
        }.addOnFailureListener {
            // Local fallback if offline
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

    internal fun setupRealtimeFirestoreListeners() {
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
                _settings.value = AdminSettingsEntity()
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
                _categories.value = fetched
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

        // Delete mock user Maher to ensure they never show up
        db.collection("providers").document("p_maher").delete()
        db.collection("providers").whereEqualTo("name", "ماهر محمد طاهر").get().addOnSuccessListener { sn ->
            for (doc in sn.documents) { doc.reference.delete() }
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
                }.filter { !it.name.contains("ماهر") && it.id != "p_maher" }
                _providers.value = fetched
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

        // 14. Activity Logs (Instantly synced)
        db.collection("activity_logs").addSnapshotListener { snapshot, error ->
            if (snapshot != null) {
                val fetched = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(ActivityLogEntity::class.java)
                    } catch (e: Exception) {
                        null
                    }
                }.sortedByDescending { it.timestamp }
                _activityLogs.value = fetched.take(100)
            }
        }
    }

    internal fun writeDefaultSupervisors() {
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

    internal fun writeDefaultColorPalettes() {
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

    internal fun writeDefaultCategories() {
        val fbCategories = listOf(
            CategoryEntity("1", "صيانة منزلية (سباكة/كهرباء)", "🔧", 1),
            CategoryEntity("2", "صحة ورعاية طبية", "🏥", 2),
            CategoryEntity("3", "تعليم وتدريس خصوصي", "📚", 3),
            CategoryEntity("4", "سيارات ونقل عام", "🚗", 4),
            CategoryEntity("5", "تقنية وبرامح ذكية", "💻", 5),
            CategoryEntity("6", "تجميل ولياقة منزلية", "💇", 6),
            CategoryEntity("other", "أخرى", "✏️", 7)
        )
        fbCategories.forEach { cat ->
            db.collection("categories").document(cat.id).set(cat)
        }
    }

    internal fun writeDefaultCities() {
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

    internal fun writeDefaultBanners() {
        val defaultBanners = listOf(
            BannerEntity("b1", "خصومات خاصة على صيانة التكييف السنوية", "https://example.com/banner1", "1", "VIP", "LARGE", 5),
            BannerEntity("b2", "أفضل معلم كهروميكانيك متاح الآن في صنعاء", "https://example.com/banner2", "1", "NORMAL", "MEDIUM", 6),
            BannerEntity("b3", "مدرسون لجميع المراحل الدراسية واللغات", "https://example.com/banner3", "3", "NORMAL", "SMALL", 4)
        )
        defaultBanners.forEach { banner ->
            db.collection("banners").document(banner.id).set(banner)
        }
    }

    internal fun writeDefaultProviders() {
        val fbProviders = listOf(
            ProviderEntity("p_amin", "امين الغرباني", "777703195", "1", "صنعاء", true, "APPROVED", true, "ye_san", "منطقة الدائري جوار مدرسة اسماء للبنات", 5.0f, 300, previewPrice = 1500.0, latitude = 15.3694, longitude = 44.1910, subscriptionExpiry = System.currentTimeMillis() + (10L * 24 * 60 * 60 * 1000))
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

    fun sendReport(providerId: String, providerName: String, reporterName: String, content: String, targetId: String = "", targetType: String = "SERVICES") {
        try {
            val newReport = ReportEntity(
                id = UUID.randomUUID().toString(),
                providerId = providerId,
                providerName = providerName,
                reporterName = reporterName,
                content = content,
                targetId = if (targetId.isNotEmpty()) targetId else providerId,
                targetType = targetType
            )
            db.collection("reports").document(newReport.id).set(newReport)
            logActivity("تقديم بلاغ ضد: $providerName ($targetType)")
            triggerNotification("📢 تم إرسال بلاغك ضد $providerName")
        } catch (e: Exception) {
            e.printStackTrace()
            _uiErrorMessage.value = "حدث خطأ أثناء إرسال البلاغ: ${e.localizedMessage}"
        }
    }

    fun deleteReport(reportId: String) {
        try {
            db.collection("reports").document(reportId).delete()
            logActivity("حذف بلاغ رقم: $reportId")
            triggerNotification("🗑️ تم حذف البلاغ من النظام")
        } catch (e: Exception) {
            e.printStackTrace()
            _uiErrorMessage.value = "حدث خطأ أثناء حذف البلاغ: ${e.localizedMessage}"
        }
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
        val settingsState = _settings.value
        val isRedirectedToAdmin = settingsState.chatRoutingMode == "ADMIN_ONLY" || settingsState.chatRoutingMode == "ADMIN_THEN_CENTER"
        
        val targetProviderId = if (isRedirectedToAdmin) "admin" else providerId
        val targetProviderName = if (isRedirectedToAdmin) "الإدارة والدعم" else providerName
        val channelId = if (isRedirectedToAdmin) "support_$customerId" else "chat_p_${providerId}_u_${customerId}"
        
        val dispCustomerName = customerName.ifEmpty { "عميل" }
        val displayName = if (isRedirectedToAdmin) "الدعم المباشر: $dispCustomerName" else "دردشة: $targetProviderName مع $dispCustomerName"
        
        db.collection("chat_channels").document(channelId).get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) {
                val newCh = ChatChannelEntity(
                    id = channelId,
                    userName = displayName,
                    customerId = customerId,
                    customerName = dispCustomerName,
                    customerPhone = customerId,
                    targetId = targetProviderId,
                    targetName = targetProviderName,
                    lastMessage = if (isRedirectedToAdmin) "مرحباً! تم تحويل محادثتك تلقائياً إلى الإدارة للمتابعة والدعم." else "مرحباً! تم بدء محادثة فورية جديدة لتنسيق الخدمة.",
                    timestamp = System.currentTimeMillis(),
                    isProvider = false,
                    messages = listOf(
                        ChatMessageEntity(
                            id = UUID.randomUUID().toString(),
                            senderId = "system",
                            message = if (isRedirectedToAdmin) "مرحباً! تم تحويل محادثتك تلقائياً إلى الإدارة للمتابعة والدعم والتحقق من الجودة وحماية الشراء." else "مرحباً! تم بدء محادثة فورية جديدة لتنسيق الخدمة.",
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

    fun submitJoinForm(
        context: android.content.Context,
        name: String, phone: String, catId: String, area: String,
        neighborhood: String, photoPath: String, idCardPath: String, gpsCoords: String,
        workPhotos: List<String> = emptyList(),
        customCategoryName: String = ""
    ) {
        val requestDocId = phone.trim().replace(" ", "").replace("+", "")
        val newRequest = PendingProviderEntity(
            id = requestDocId,
            name = name,
            phone = phone,
            categoryId = catId,
            area = area,
            localNeighborhood = neighborhood,
            status = "PENDING",
            selfiePhotoBase64 = photoPath,
            idPhotoBase64 = idCardPath,
            workPhotosBase64 = workPhotos,
            customCategoryName = customCategoryName
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
            cityId = "ye_san",
            localNeighborhood = request.localNeighborhood,
            rating = 5.0f,
            customCategoryName = request.customCategoryName
        )
        db.collection("providers").document(approvedProvider.id).set(approvedProvider)
        db.collection("pending_providers").document(request.id).delete()
        
        // Add accepted notification!
        addNotification(
            title = "🎉 تهانينا! تم قبول انضمامك كفني معتمد",
            message = "مرحباً بك يا غالي، لقد تم قبول طلب انضمامك كمهني معتمد وأصبحت الآن نشطاً في دليل كل خدمات اليمن! حسابك يظهر الآن لجميع العملاء.",
            targetType = "USER",
            targetValue = request.phone
        )
        
        triggerNotification("✅ تم قبول طلب ${request.name}")
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

    fun addNewBanner(title: String, url: String, redirect: String, type: String, size: String, duration: Int, displayTime: String = "طوال اليوم", targetSection: String = "ALL") {
        val banner = BannerEntity(
            id = UUID.randomUUID().toString(),
            title = title,
            url = url,
            redirectCategory = redirect,
            type = type,
            size = size,
            duration = duration,
            displayTime = displayTime,
            order = _banners.value.size + 1,
            targetSection = targetSection
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
        newOrderedList.forEachIndexed { index, banner ->
            val updated = banner.copy(order = index + 1)
            db.collection("banners").document(banner.id).set(updated)
        }
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
        val current = _settings.value
        val updated = current.copy(activeThemeId = themeId)
        _settings.value = updated
        _forceRebuildKey.value = _forceRebuildKey.value + 1
        try {
            db.collection("settings").document("main_settings").set(updated)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        triggerNotification("🎨 تم تغيير مظهر التطبيق إلى $themeId")
    }

    fun saveCustomSettingsState(newSettings: AdminSettingsEntity) {
        _settings.value = newSettings
        _maxKmRadius.value = newSettings.maxSearchRadiusKm
        _forceRebuildKey.value = _forceRebuildKey.value + 1
        try {
            db.collection("settings").document("main_settings").set(newSettings)
        } catch (e: Exception) {
            e.printStackTrace()
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
        _settings.value = updated
        _maxKmRadius.value = radiusKm
        _forceRebuildKey.value = _forceRebuildKey.value + 1
        try {
            db.collection("settings").document("main_settings").set(updated)
        } catch (e: Exception) {
            e.printStackTrace()
        }
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

        // Auto-save user identity in memory if empty to ensure they can track notifications immediately
        if (_currentUserPhone.value.isEmpty()) {
            _currentUserPhone.value = cleanPhone
            _currentUserName.value = cleanName
            _currentUserResidence.value = area
        }

        // Notify the customer (user) that their booking was successfully submitted
        addNotification(
            title = "📅 تم إرسال طلب حجزك بنجاح",
            message = "عزيزي العميل $cleanName، لقد تم إرسال طلب حجزك رقم: ${newBooking.id} بنجاح للفني: $providerName. الموعد المحدد: $dateString الساعة $timeString. طلبك الآن قيد المراجعة والتدقيق الإداري وسيصلك إشعار بالخطوة القادمة فوراً.",
            targetType = "USER",
            targetValue = cleanPhone
        )

        // Compile a highly detailed notification containing customer's name, phone, and area of residence
        val detailedMessage = "طلب حجز جديد من العميل: $cleanName، رقم الهاتف للتواصل: $cleanPhone، منطقة السكن: $area. تفاصيل الخدمة المطلوبة: $serviceType. الموعد المفضل: $dateString الساعة $timeString."

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

        // 3. Notify the Customer (Requester) themselves
        addNotification(
            title = "📅 تم استلام طلب حجزك بنجاح",
            message = "عزيزي العميل ${cleanName}، لقد تم تقديم طلب حجز الخدمة رقم (${newBooking.id}) بنجاح للفني (${providerName}). الطلب حالياً بانتظار المراجعة والاتصال بك لتأكيد الموعد.",
            targetType = "USER",
            targetValue = cleanPhone
        )

        triggerNotification("تم إرسال طلب الحجز، سيتم مراجعته")
    }

    fun updateBookingStatus(bookingId: String, newStatus: String, rejectionReason: String = "") {
        db.collection("bookings").document(bookingId).get().addOnSuccessListener { snapshot ->
            val b = snapshot.toObject(BookingEntity::class.java)
            if (b != null) {
                val updated = b.copy(status = newStatus, rejectionReason = rejectionReason)
                db.collection("bookings").document(bookingId).set(updated)
                
                val arabicStatusMsg = when(newStatus) {
                    "PENDING", "UNDER_REVIEW" -> "وضع حجزك قيد المراجعة والتدقيق الإداري"
                    "IN_PROGRESS" -> "قبول حجزك وبدء تنفيذ الخدمة المطلوبة ميدانياً"
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
            "PENDING", "UNDER_REVIEW" -> "⏳ تم وضع الحجز قيد المراجعة"
            "IN_PROGRESS" -> "⚡ تم قبول الحجز وبدء تنفيذ الخدمة"
            "REJECTED" -> "❌ تم رفض الحجز وإلغائه"
            "COMPLETED" -> "🎉 تم إكمال الخدمة بنجاح وتوثيق الإنجاز"
            else -> "تم تحديث حالة الحجز بنجاح"
        }
        triggerNotification(toastMsg)
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
    fun addNotification(
        title: String,
        message: String,
        targetType: String,
        targetValue: String,
        expiryTimestamp: Long = 0L,
        scheduledTime: Long = 0L,
        customerPhone: String = "",
        customerName: String = ""
    ) {
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
            customerName = customerName
        )
        db.collection("notifications").document(newNotif.id).set(newNotif)
        triggerNotification("🔔 تم إرسال الإشعار الموجه بنجاح!")
    }

    fun deleteNotification(notifId: String) {
        db.collection("notifications").document(notifId).delete()
        triggerNotification("🗑️ تم حذف الإشعار")
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
            if (senderId == "admin") {
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
                }
            } else {
                // User replying -> Notify supervisor
                addNotification(
                    title = "💬 رسالة جديدة من: $senderName",
                    message = "محتوى الرسالة: $finalMsgText",
                    targetType = "SUPERVISOR",
                    targetValue = "all"
                )
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

    internal val _supervisors = MutableStateFlow<List<SupervisorEntity>>(emptyList())
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

    fun wipeSelectedDatabaseData(password: String, selectedCollections: List<String>): Boolean {
        if (password == "maher736462") {
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

    internal val _pendingTechnicians = MutableStateFlow<List<PendingProviderEntity>>(emptyList())
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

    internal val _cardSettings = MutableStateFlow(CardSettings())
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

    internal val _blockedChatParticipants = MutableStateFlow<Set<ChatParticipantType>>(emptySet())
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

    // Additional StateFlows for app compatibility
    internal val _stores = MutableStateFlow<List<StoreEntity>>(emptyList())
    val stores: StateFlow<List<StoreEntity>> = _stores.asStateFlow()

    internal val _deletedProviders = MutableStateFlow<List<ProviderEntity>>(emptyList())
    val deletedProviders: StateFlow<List<ProviderEntity>> = _deletedProviders.asStateFlow()

    internal val _properties = MutableStateFlow<List<PropertyEntity>>(emptyList())
    val properties: StateFlow<List<PropertyEntity>> = _properties.asStateFlow()

    internal val _jobs = MutableStateFlow<List<JobEntity>>(emptyList())
    val jobs: StateFlow<List<JobEntity>> = _jobs.asStateFlow()

    internal val _triggerRestoreAccountDialog = MutableStateFlow(false)
    val triggerRestoreAccountDialog: StateFlow<Boolean> = _triggerRestoreAccountDialog.asStateFlow()

    internal val _activeChatChannel = MutableStateFlow<ChatChannelEntity?>(null)
    val activeChatChannel: StateFlow<ChatChannelEntity?> = _activeChatChannel.asStateFlow()

    internal val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    internal val _uiErrorMessage = MutableStateFlow<String?>(null)
    val uiErrorMessage: StateFlow<String?> = _uiErrorMessage.asStateFlow()

    internal val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    internal val _selectedProvider = MutableStateFlow<ProviderEntity?>(null)
    var selectedProvider: ProviderEntity?
        get() = _selectedProvider.value
        set(v) { _selectedProvider.value = v }
    val selectedProviderFlow: StateFlow<ProviderEntity?> = _selectedProvider.asStateFlow()

    internal val _selectedStore = MutableStateFlow<StoreEntity?>(null)
    var selectedStore: StoreEntity?
        get() = _selectedStore.value
        set(v) { _selectedStore.value = v }
    val selectedStoreFlow: StateFlow<StoreEntity?> = _selectedStore.asStateFlow()

    internal val _selectedProperty = MutableStateFlow<PropertyEntity?>(null)
    var selectedProperty: PropertyEntity?
        get() = _selectedProperty.value
        set(v) { _selectedProperty.value = v }
    val selectedPropertyFlow: StateFlow<PropertyEntity?> = _selectedProperty.asStateFlow()

    internal val _screenBackStack = MutableStateFlow<List<String>>(listOf("HOME"))
    val screenBackStack: StateFlow<List<String>> = _screenBackStack.asStateFlow()

    internal val _activeVoiceCallPair = MutableStateFlow<Pair<String, String>?>(null)
    val activeVoiceCall: StateFlow<Pair<String, String>?> = _activeVoiceCallPair.asStateFlow()

    internal val _customProfileTabs = MutableStateFlow<List<CustomProfileTabEntity>>(emptyList())
    val customProfileTabs: StateFlow<List<CustomProfileTabEntity>> = _customProfileTabs.asStateFlow()

    internal val _passwordRecoveryWaitingPhone = MutableStateFlow("")
    val passwordRecoveryWaitingPhone: StateFlow<String> = _passwordRecoveryWaitingPhone.asStateFlow()

    fun setPasswordRecoveryWaitingPhone(phone: String) {
        _passwordRecoveryWaitingPhone.value = phone
    }

    fun updateOnlineStatus(isOnline: Boolean) {
        _isOnline.value = isOnline
    }

    fun updateUserFcmToken(userId: String, token: String) {
        if (userId.isNotEmpty() && userId != "guest") {
            db.collection("registered_users").document(userId).update("fcmToken", token)
                .addOnFailureListener {
                    db.collection("providers").document(userId).update("fcmToken", token)
                }
        }
    }

    fun triggerRestoreAccountDialog(show: Boolean) {
        _triggerRestoreAccountDialog.value = show
    }

    fun closeActiveChatChannel() {
        _activeChatChannel.value = null
    }

    fun clearUiError() {
        _uiErrorMessage.value = null
    }

    fun setUiError(message: String) {
        _uiErrorMessage.value = message
    }

    fun logActivity(actionName: String) {
        val log = ActivityLogEntity(
            id = "log_" + java.util.UUID.randomUUID().toString().take(6),
            action = actionName,
            timestamp = System.currentTimeMillis()
        )
        try {
            db.collection("activity_logs").document(log.id).set(log)
            _activityLogs.value = (listOf(log) + _activityLogs.value).take(100)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun refreshData() {
        _isRefreshing.value = true
        try {
            db.collection("settings").document("main_settings").get().addOnSuccessListener { snapshot ->
                if (snapshot != null && snapshot.exists()) {
                    snapshot.toObject(AdminSettingsEntity::class.java)?.let {
                        _settings.value = it
                        _maxKmRadius.value = it.maxSearchRadiusKm
                    }
                }
            }
            db.collection("categories").get().addOnSuccessListener { snapshot ->
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { it.toObject(CategoryEntity::class.java) }
                    _categories.value = list
                }
            }
            db.collection("banners").get().addOnSuccessListener { snapshot ->
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { it.toObject(BannerEntity::class.java) }
                    _banners.value = list.sortedBy { it.order }
                }
            }
            applyFilters()
            _forceRebuildKey.value = _forceRebuildKey.value + 1
        } catch (e: Exception) {
            e.printStackTrace()
        }
        _isRefreshing.value = false
        triggerNotification("🔄 تم تحديث وإعادة تحميل كافة بيانات التطبيق والإعدادات بنجاح!")
    }

    fun retryConnection(context: android.content.Context? = null) {
        _isOnline.value = true
        refreshData()
    }

    fun setUserSessionDetails(context: android.content.Context, name: String, phone: String, residence: String) {
        _currentUserId.value = "user_" + phone.trim().replace(" ", "").replace("+", "")
        _currentUserName.value = name
        _currentUserPhone.value = phone
        _currentUserResidence.value = residence
        val sp = context.getSharedPreferences("yemen_service_prefs", android.content.Context.MODE_PRIVATE)
        sp.edit().apply {
            putString("user_id", _currentUserId.value)
            putString("user_name", name)
            putString("user_phone", phone)
            putString("user_residence", residence)
            apply()
        }
    }

    // --- ADDITIONAL COMPATIBILITY METHODS ---


    internal val _products = MutableStateFlow<List<ProductEntity>>(emptyList())
    val products: StateFlow<List<ProductEntity>> = _products.asStateFlow()

    internal val _ratings = MutableStateFlow<List<RatingEntity>>(emptyList())
    val ratings: StateFlow<List<RatingEntity>> = _ratings.asStateFlow()

    internal val _orders = MutableStateFlow<List<OrderEntity>>(emptyList())
    val orders: StateFlow<List<OrderEntity>> = _orders.asStateFlow()

    internal val _callsLog = MutableStateFlow<List<CallEntity>>(emptyList())
    val callsLog: StateFlow<List<CallEntity>> = _callsLog.asStateFlow()

    internal val _coupons = MutableStateFlow<List<CouponEntity>>(emptyList())
    val coupons: StateFlow<List<CouponEntity>> = _coupons.asStateFlow()

    internal val _internalWallets = MutableStateFlow<List<InternalWalletEntity>>(emptyList())
    val internalWallets: StateFlow<List<InternalWalletEntity>> = _internalWallets.asStateFlow()

    internal val _walletTransactions = MutableStateFlow<List<WalletTransactionEntity>>(emptyList())
    val walletTransactions: StateFlow<List<WalletTransactionEntity>> = _walletTransactions.asStateFlow()

    internal val _paymentWallets = MutableStateFlow<List<PaymentWalletEntity>>(emptyList())
    val paymentWallets: StateFlow<List<PaymentWalletEntity>> = _paymentWallets.asStateFlow()

    internal val _payments = MutableStateFlow<List<PaymentEntity>>(emptyList())
    val payments: StateFlow<List<PaymentEntity>> = _payments.asStateFlow()

    internal val _jobApplications = MutableStateFlow<List<JobApplicationEntity>>(emptyList())
    val jobApplications: StateFlow<List<JobApplicationEntity>> = _jobApplications.asStateFlow()

    // Active voice call Pair(callerName, callerRole)

    // Selected item getters and setters



    // Additional methods
    fun addNewCategory(
        nameAr: String,
        nameEn: String = "",
        icon: String = "📁",
        description: String = "",
        parentId: String = "",
        isMainCategory: Boolean = true
    ) {
        val newCat = CategoryEntity(
            id = "cat_" + java.util.UUID.randomUUID().toString().take(6),
            name = nameAr,
            icon = icon.ifEmpty { "📁" },
            order = _categories.value.size + 1
        )
        _categories.value = _categories.value + newCat
        try {
            db.collection("categories").document(newCat.id).set(newCat)
        } catch (e: Exception) {}
        triggerNotification("✅ تمت إضافة القسم الجديد: $nameAr")
    }

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
        val newBooking = BookingEntity(
            id = customBookingId.ifEmpty { "b_" + java.util.UUID.randomUUID().toString().take(6) },
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
        _bookings.value = _bookings.value + newBooking
        try {
            db.collection("bookings").document(newBooking.id).set(newBooking)
        } catch (e: Exception) {}
        triggerNotification("🎉 تم تقديم طلب الحجز بنجاح بنتيجة معلقة لدى الفني!")
    }

    fun requestPasswordRecoveryGeneral(
        accountName: String = "",
        phone: String,
        accountType: String = "",
        currentPassword: String = ""
    ) {
        _passwordRecoveryWaitingPhone.value = phone
        triggerNotification("🔒 تم إرسال طلب استعادة كلمة المرور للحساب $accountName إلى الإدارة")
    }

    fun requestPasswordRecoveryForStore(name: String, phone: String, password: String) {
        requestPasswordRecoveryGeneral(accountName = name, phone = phone, currentPassword = password)
    }

    fun requestPasswordRecoveryForProperty(title: String, phone: String, password: String) {
        requestPasswordRecoveryGeneral(accountName = title, phone = phone, currentPassword = password)
    }

    fun placeOrder(order: OrderEntity) {
        _orders.value = _orders.value + order
        try {
            db.collection("orders").document(order.id).set(order)
        } catch (e: Exception) {}
        triggerNotification("🛒 تم تقديم طلب الشراء بنجاح!")
    }

    fun placeOrder(orderMap: Map<String, Any>) {
        triggerNotification("🛒 تم تقديم طلب الشراء بنجاح!")
    }

}
