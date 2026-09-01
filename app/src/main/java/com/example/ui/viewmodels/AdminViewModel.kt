package com.example.ui.viewmodels

import com.example.ui.MainViewModel

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.data.models.*
import com.example.utils.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions

class AdminViewModel : BaseViewModel() {
    // --- Callback/Lambda Properties for decoupling ---
    var getHomeViewModel: (() -> HomeViewModel)? = null
    var getSettingsViewModel: (() -> SettingsViewModel)? = null
    var getBookingViewModel: (() -> BookingViewModel)? = null
    var getInstantRequestViewModel: (() -> InstantRequestViewModel)? = null
    var getNotifications: (() -> MutableStateFlow<List<NotificationEntity>>)? = null
    var onAddNotification: ((String, String, String, String) -> Unit)? = null
    var onTriggerNotificationFull: ((String, String, String, String) -> Unit)? = null
    var onTriggerNotification: ((String) -> Unit)? = null
    var onApplyFilters: (() -> Unit)? = null

    inner class MainViewModelDelegate {
        val homeViewModel get() = this@AdminViewModel.homeViewModel
        val settingsViewModel get() = this@AdminViewModel.settingsViewModel
        val bookingViewModel get() = this@AdminViewModel.bookingViewModel
        val instantRequestViewModel get() = this@AdminViewModel.instantRequestViewModel
        val _notifications get() = this@AdminViewModel._notifications
        
        fun addNotification(title: String, message: String, targetType: String = "", targetValue: String = "") {
            this@AdminViewModel.addNotification(title, message, targetType, targetValue)
        }
        
        fun triggerNotification(title: String, message: String, targetType: String = "ALL", targetValue: String = "") {
            this@AdminViewModel.triggerNotification(title, message, targetType, targetValue)
        }
        
        fun triggerNotification(msg: String) {
            this@AdminViewModel.onTriggerNotification?.invoke(msg)
        }
        
        fun applyFilters() {
            this@AdminViewModel.applyFilters()
        }
    }
    
    val mainViewModel = MainViewModelDelegate()

    private val auth get() = com.google.firebase.auth.FirebaseAuth.getInstance()
    private val homeViewModel get() = getHomeViewModel?.invoke() ?: throw IllegalStateException("homeViewModel not provided")
    private val settingsViewModel get() = getSettingsViewModel?.invoke() ?: throw IllegalStateException("settingsViewModel not provided")
    private val bookingViewModel get() = getBookingViewModel?.invoke() ?: throw IllegalStateException("bookingViewModel not provided")
    private val instantRequestViewModel get() = getInstantRequestViewModel?.invoke() ?: throw IllegalStateException("instantRequestViewModel not provided")
    private val _notifications get() = getNotifications?.invoke() ?: throw IllegalStateException("_notifications not provided")

    private fun addNotification(title: String, message: String, targetType: String = "", targetValue: String = "") {
        onAddNotification?.invoke(title, message, targetType, targetValue)
    }

    private fun triggerNotification(title: String, message: String, targetType: String = "", targetValue: String = "") {
        onTriggerNotificationFull?.invoke(title, message, targetType, targetValue)
    }

    private fun applyFilters() {
        onApplyFilters?.invoke()
    }

    fun logAdminActivity(adminName: String, action: String, details: String) {
        logAdminActivity("Admin: $adminName - Action: $action - Details: $details")
    }

    private fun deleteBooking(bookingId: String) {
        mainViewModel.bookingViewModel.deleteBooking(bookingId)
    }

    private fun updateBooking(booking: com.example.data.BookingEntity) {
        mainViewModel.bookingViewModel.updateBooking(booking)
    }

    internal val _pendingProviders = MutableStateFlow<List<PendingProviderEntity>>(emptyList())
    val pendingProviders: StateFlow<List<PendingProviderEntity>> = _pendingProviders.asStateFlow()
    internal val _pendingTechnicians = MutableStateFlow<List<PendingProviderEntity>>(emptyList())
    val pendingTechnicians: StateFlow<List<PendingProviderEntity>> = _pendingTechnicians.asStateFlow()
    internal val _registeredUsersList = MutableStateFlow<List<Map<String, Any>>>(emptyList())
    val registeredUsersList: StateFlow<List<Map<String, Any>>> = _registeredUsersList.asStateFlow()
    internal val _registeredUsersCount = MutableStateFlow(0)
    val registeredUsersCount: StateFlow<Int> = _registeredUsersCount.asStateFlow()
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
    internal val _paymentWallets = MutableStateFlow<List<PaymentWalletEntity>>(emptyList())
    val paymentWallets: StateFlow<List<PaymentWalletEntity>> = _paymentWallets.asStateFlow()
    internal val _payments = MutableStateFlow<List<PaymentEntity>>(emptyList())
    val payments: StateFlow<List<PaymentEntity>> = _payments.asStateFlow()
    internal val _orders = MutableStateFlow<List<com.example.data.OrderEntity>>(emptyList())
    val orders: StateFlow<List<com.example.data.OrderEntity>> = _orders.asStateFlow()
    internal val _ratings = MutableStateFlow<List<com.example.data.RatingEntity>>(emptyList())
    val ratings: StateFlow<List<com.example.data.RatingEntity>> = _ratings.asStateFlow()
    internal val _customProfileTabs = MutableStateFlow<List<com.example.data.CustomProfileTabEntity>>(emptyList())
    val customProfileTabs: StateFlow<List<com.example.data.CustomProfileTabEntity>> = _customProfileTabs.asStateFlow()
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

            mainViewModel.addNotification(
                title = "🎉 تهانينا! تم تفعيل متجرك بنجاح",
                message = "مرحباً بك يا غالي، لقد تم مراجعة وتفعيل متجرك/محلك '${request.name}' بنجاح في التطبيق! يمكنك الآن إضافة منتجاتك وإدارة متجرك مباشرة من شاشة الانضمام.",
                targetType = "USER",
                targetValue = request.phone
            )
            mainViewModel.triggerNotification("✅ تم تفعيل متجر ${request.name}")
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

            mainViewModel.addNotification(
                title = "🎉 تهانينا! تم تفعيل إعلان عقارك بنجاح",
                message = "مرحباً بك، لقد تم مراجعة وتفعيل عقارك '${request.name}' بنجاح في دليل العقارات المعتمد! يمكنك تعديله وإدارته ورؤية تعليقات العملاء مباشرة من شاشة الانضمام.",
                targetType = "USER",
                targetValue = request.phone
            )
            mainViewModel.triggerNotification("✅ تم تفعيل عقار ${request.name}")
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
            val currentProviders = mainViewModel.homeViewModel._providers.value.filter { it.id != approvedProvider.id && it.phone.trim().replace(" ", "").replace("+", "") != cleanPhone }.toMutableList()
            currentProviders.add(approvedProvider)
            mainViewModel.homeViewModel._providers.value = currentProviders
            applyFilters()

            // Add accepted notification!
            mainViewModel.addNotification(
                title = "🎉 تهانينا! تم قبول انضمامك كفني معتمد",
                message = "مرحباً بك يا غالي، لقد تم قبول طلب انضمامك كمهني معتمد وأصبحت الآن نشطاً في دليل كل خدمات اليمن! حسابك يظهر الآن لجميع العملاء.",
                targetType = "USER",
                targetValue = request.phone
            )
            
            mainViewModel.triggerNotification("✅ تم قبول طلب ${request.name}")
        }
    }

fun rejectRequest(request: PendingProviderEntity, reason: String) {
        db.collection("pending_providers").document(request.id).delete()
        
        // Add rejected notification!
        mainViewModel.addNotification(
            title = "❌ تنويه حول طلب انضمامك",
            message = "للأسف لم يتم قبول طلب انضمامك للأسباب التالية: $reason. يرجى تعديل البيانات وإعادة تقديم الطلب.",
            targetType = "USER",
            targetValue = request.phone
        )
        
        mainViewModel.triggerNotification("❌ تم رفض طلب ${request.name} بسبب: $reason")
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
            val currentProviders = mainViewModel.homeViewModel._providers.value.filter { it.id != finalId }.toMutableList()
            currentProviders.add(p)
            mainViewModel.homeViewModel._providers.value = currentProviders
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
        mainViewModel.triggerNotification("✅ تم قبول وتعميم الفني: ${pending.name}")
    }

fun rejectPendingProvider(pending: PendingProviderEntity, reason: String = "تم رفض الطلب") {
        db.collection("pending_providers").document(pending.id).delete()
        mainViewModel.triggerNotification("❌ تم رفض طلب انضمام: ${pending.name}")
    }

fun approveRegisteredUser(userId: String, userName: String = "") {
        _registeredUsersList.value = _registeredUsersList.value.map { u ->
            if (u["id"]?.toString() == userId) {
                u.toMutableMap().apply { put("isApproved", true) }
            } else u
        }
        db.collection("registered_users").document(userId).update("isApproved", true)
            .addOnSuccessListener {
                mainViewModel.triggerNotification("✅ تم قبول وتأكيد طلب انضمام المستخدم $userName بنجاح من قبل الأدمن!")
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
                mainViewModel.triggerNotification("🛡️ تم $actionText حساب المستخدم $userName بنجاح.")
            }
    }

fun deleteRegisteredUser(userId: String, userName: String = "") {
        _registeredUsersList.value = _registeredUsersList.value.filter { u -> u["id"]?.toString() != userId }
        db.collection("registered_users").document(userId).delete()
            .addOnSuccessListener {
                mainViewModel.triggerNotification("🗑️ تم حذف حساب المستخدم $userName من القاعدة بنجاح.")
            }
    }

fun saveStore(store: com.example.data.StoreEntity) {
        val cleanPhone = store.phone.trim().replace(" ", "").replace("+", "")
        val duplicateType = checkAndGetDuplicateAccountType(cleanPhone, store.id)
        if (duplicateType != null) {
            mainViewModel.triggerNotification("❌ عذراً! رقم الهاتف (${store.phone}) مسجل بالفعل كـ ($duplicateType). لا يُسمح بتكرار الحسابات.")
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
                    mainViewModel.triggerNotification("✅ تم حفظ وتأكيد بيانات الطلب بنجاح!")
                }
                .addOnFailureListener {
                    mainViewModel.triggerNotification("⚠️ تم حفظ الطلب محلياً وفي انتظار مزامنة الشبكة")
                }
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
                mainViewModel.triggerNotification("🗑️ تم حذف المتجر ونقله للمحذوفات")
            }
            .addOnFailureListener { e ->
                db.collection("stores").document(storeId).get().addOnSuccessListener { snapshot ->
                    val store = snapshot.toObject(com.example.data.StoreEntity::class.java)
                    if (store != null) {
                        db.collection("stores").document(storeId).set(store.copy(isDeleted = true, deletedAt = System.currentTimeMillis()))
                            .addOnSuccessListener {
                                mainViewModel.triggerNotification("🗑️ تم حذف المتجر ونقله للمحذوفات")
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
                mainViewModel.triggerNotification("♻️ تم استعادة المتجر بنجاح")
            }
    }

fun deleteStorePermanently(storeId: String) {
        _stores.value = _stores.value.filter { it.id != storeId }
        db.collection("stores").document(storeId).delete()
            .addOnSuccessListener {
                mainViewModel.triggerNotification("🗑️ تم حذف المتجر نهائياً من النظام")
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
                mainViewModel.triggerNotification(if (isActive) "✅ تم تفعيل المتجر والموافقة عليه" else "🔒 تم إلغاء تفعيل المتجر")
            }
            .addOnFailureListener {
                if (targetStore != null) {
                    db.collection("stores").document(docId).set(targetStore.copy(isActive = isActive, isApproved = isActive))
                        .addOnSuccessListener {
                            mainViewModel.triggerNotification(if (isActive) "✅ تم تفعيل المتجر والموافقة عليه" else "🔒 تم إلغاء تفعيل المتجر")
                        }
                }
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
                mainViewModel.triggerNotification(if (isPinned) "📌 تم تثبيت المتجر في الشاشة الرئيسية" else "📌 تم إلغاء تثبيت المتجر")
            }
    }

fun setStoreVip(storeId: String, isVip: Boolean) {
        _stores.value = _stores.value.map {
            if (it.id == storeId) it.copy(isVip = isVip) else it
        }
        db.collection("stores").document(storeId).update("isVip", isVip)
            .addOnSuccessListener {
                mainViewModel.triggerNotification(if (isVip) "🏆 تم تمييز المتجر بشارة VIP" else "🔒 تم إلغاء شارة VIP عن المتجر")
            }
    }

fun setStoreVerified(storeId: String, isVerified: Boolean) {
        _stores.value = _stores.value.map {
            if (it.id == storeId) it.copy(isVerified = isVerified) else it
        }
        db.collection("stores").document(storeId).update("isVerified", isVerified)
            .addOnSuccessListener {
                mainViewModel.triggerNotification(if (isVerified) "🛡️ تم توثيق حساب المتجر" else "🔒 تم إلغاء التوثيق عن المتجر")
            }
    }

fun setStoreRecommended(storeId: String, isRecommended: Boolean) {
        _stores.value = _stores.value.map {
            if (it.id == storeId) it.copy(isRecommended = isRecommended) else it
        }
        db.collection("stores").document(storeId).update("isRecommended", isRecommended)
            .addOnSuccessListener {
                mainViewModel.triggerNotification(if (isRecommended) "💖 تم ترشيح المتجر كموصى به" else "🔒 تم إلغاء ترشيح المتجر")
            }
    }

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
                mainViewModel.triggerNotification(if (isBlocked) "🚫 تم حظر المحل/المركز بنجاح ($reason)" else "✅ تم إلغاء حظر المحل/المركز")
            }
    }

fun setStoreChatDisabled(storeId: String, isDisabled: Boolean) {
        _stores.value = _stores.value.map {
            if (it.id == storeId) it.copy(isChatDisabled = isDisabled) else it
        }
        db.collection("stores").document(storeId).update("isChatDisabled", isDisabled)
            .addOnSuccessListener {
                mainViewModel.triggerNotification(if (isDisabled) "🔇 تم إيقاف الدردشة للمتجر" else "💬 تم تفعيل الدردشة للمتجر")
            }
    }

fun setStoreNotificationsDisabled(storeId: String, isDisabled: Boolean) {
        _stores.value = _stores.value.map {
            if (it.id == storeId) it.copy(isNotificationsDisabled = isDisabled) else it
        }
        db.collection("stores").document(storeId).update("isNotificationsDisabled", isDisabled)
            .addOnSuccessListener {
                mainViewModel.triggerNotification(if (isDisabled) "🔕 تم كتم الإشعارات للمتجر" else "🔔 تم تفعيل الإشعارات للمتجر")
            }
    }

fun setStorePaymentEnabled(storeId: String, isEnabled: Boolean) {
        db.collection("stores").document(storeId).update("paymentEnabled", isEnabled)
            .addOnSuccessListener {
                mainViewModel.triggerNotification(if (isEnabled) "💳 تم تفعيل نظام الدفع والمحفظة للمتجر" else "🔒 تم تعطيل نظام الدفع للمتجر")
            }
    }

fun toggleStoreBlocked(storeId: String, isBlocked: Boolean) {
        db.collection("stores").document(storeId).update("isBlocked", isBlocked)
            .addOnSuccessListener {
                mainViewModel.triggerNotification(if (isBlocked) "🚫 تم حظر المتجر" else "✅ تم إلغاء حظر المتجر")
            }
    }

fun toggleStoreActive(storeId: String) {
        val store = _stores.value.find { it.id == storeId }
        if (store != null) {
            val updated = store.copy(isActive = !store.isActive)
            saveStore(updated)
            mainViewModel.triggerNotification(if (!updated.isActive) "🔒 تم حظر/إيقاف المحل/المركز مؤقتاً" else "🟢 تم تفعيل المحل/المركز")
        }
    }

fun toggleStorePinned(storeId: String) {
        val store = _stores.value.find { it.id == storeId }
        if (store != null) {
            val updated = store.copy(isPinned = !store.isPinned)
            saveStore(updated)
            mainViewModel.triggerNotification(if (updated.isPinned) "📌 تم تثبيت المحل في البداية" else "🔓 تم إلغاء تثبيت المحل")
        }
    }

fun toggleStoreChatDisabled(storeId: String) {
        val store = _stores.value.find { it.id == storeId }
        if (store != null) {
            val updated = store.copy(isChatDisabled = !store.isChatDisabled)
            saveStore(updated)
            mainViewModel.triggerNotification(if (updated.isChatDisabled) "🚫 تم إيقاف المحادثات للمحل/المركز" else "💬 تم تفعيل المحادثات للمحل/المركز")
        }
    }

fun approveStorePdf(storeId: String, approve: Boolean) {
        db.collection("stores").document(storeId).get().addOnSuccessListener { snapshot ->
            val store = snapshot.toObject(com.example.data.StoreEntity::class.java)
            if (store != null) {
                db.collection("stores").document(storeId).set(store.copy(pdfStatus = if (approve) "APPROVED" else "REJECTED"))
                    .addOnSuccessListener {
                        mainViewModel.triggerNotification(if (approve) "✅ تم قبول ملف الـ PDF للمحل بنجاح!" else "❌ تم رفض ملف الـ PDF للمحل.")
                    }
            }
        }
    }

fun saveProperty(property: com.example.data.PropertyEntity) {
        val cleanPhone = property.phone.trim().replace(" ", "").replace("+", "")
        val duplicateType = checkAndGetDuplicateAccountType(cleanPhone, property.id)
        if (duplicateType != null) {
            mainViewModel.triggerNotification("❌ عذراً! رقم الهاتف (${property.phone}) مسجل بالفعل كـ ($duplicateType). لا يُسمح بتكرار الحسابات.")
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
                    mainViewModel.triggerNotification("✅ تم تسجيل بيانات العقار بنجاح!")
                }
                .addOnFailureListener {
                    mainViewModel.triggerNotification("⚠️ تم الحفظ محلياً وبانتظار مزامنة السحابة")
                }
        }
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
                mainViewModel.triggerNotification("🗑️ تم حذف العقار بنجاح")
            }
            .addOnFailureListener { e ->
                db.collection("properties").document(propertyId).get().addOnSuccessListener { snapshot ->
                    val property = snapshot.toObject(com.example.data.PropertyEntity::class.java)
                    if (property != null) {
                        db.collection("properties").document(propertyId).set(property.copy(isDeleted = true, deletedAt = System.currentTimeMillis()))
                            .addOnSuccessListener {
                                mainViewModel.triggerNotification("🗑️ تم حذف العقار بنجاح")
                            }
                    }
                }
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
                mainViewModel.triggerNotification("♻️ تم استعادة العقار بنجاح")
            }
    }

fun deletePropertyPermanently(propertyId: String) {
        _properties.value = _properties.value.filter { it.id != propertyId }
        db.collection("properties").document(propertyId).delete()
            .addOnSuccessListener {
                mainViewModel.triggerNotification("🗑️ تم حذف العقار نهائياً من النظام")
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
                mainViewModel.triggerNotification(if (isActive) "✅ تم تفعيل ونشر العقار للجميع" else "🔒 تم إلغاء تفعيل ونشر العقار")
            }
            .addOnFailureListener {
                if (targetProp != null) {
                    db.collection("properties").document(docId).set(targetProp.copy(isActive = isActive, isApproved = isActive))
                        .addOnSuccessListener {
                            mainViewModel.triggerNotification(if (isActive) "✅ تم تفعيل ونشر العقار للجميع" else "🔒 تم إلغاء تفعيل ونشر العقار")
                        }
                }
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
                mainViewModel.triggerNotification(if (isPinned) "📌 تم تمييز وتثبيت العقار في الصدارة" else "📌 تم إلغاء تثبيت العقار")
            }
    }

fun setPropertyVip(propertyId: String, isVip: Boolean) {
        _properties.value = _properties.value.map {
            if (it.id == propertyId) it.copy(isVip = isVip) else it
        }
        db.collection("properties").document(propertyId).update("isVip", isVip)
            .addOnSuccessListener {
                mainViewModel.triggerNotification(if (isVip) "🏆 تم تمييز العقار بشارة VIP" else "🔒 تم إلغاء شارة VIP عن العقار")
            }
    }

fun setPropertyVerified(propertyId: String, isVerified: Boolean) {
        _properties.value = _properties.value.map {
            if (it.id == propertyId) it.copy(isVerified = isVerified) else it
        }
        db.collection("properties").document(propertyId).update("isVerified", isVerified)
            .addOnSuccessListener {
                mainViewModel.triggerNotification(if (isVerified) "🛡️ تم توثيق إعلان العقار" else "🔒 تم إلغاء التوثيق عن العقار")
            }
    }

fun setPropertyRecommended(propertyId: String, isRecommended: Boolean) {
        _properties.value = _properties.value.map {
            if (it.id == propertyId) it.copy(isRecommended = isRecommended) else it
        }
        db.collection("properties").document(propertyId).update("isRecommended", isRecommended)
            .addOnSuccessListener {
                mainViewModel.triggerNotification(if (isRecommended) "💖 تم ترشيح العقار كموصى به" else "🔒 تم إلغاء ترشيح العقار")
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
                mainViewModel.triggerNotification(if (isBlocked) "🚫 تم حظر إعلان العقار بنجاح ($reason)" else "✅ تم إلغاء حظر إعلان العقار")
            }
    }

fun setPropertyChatDisabled(propertyId: String, isDisabled: Boolean) {
        _properties.value = _properties.value.map {
            if (it.id == propertyId) it.copy(isChatDisabled = isDisabled) else it
        }
        db.collection("properties").document(propertyId).update("isChatDisabled", isDisabled)
            .addOnSuccessListener {
                mainViewModel.triggerNotification(if (isDisabled) "🔇 تم إيقاف الدردشة للمعلن" else "💬 تم تفعيل الدردشة للمعلن")
            }
    }

fun setPropertyNotificationsDisabled(propertyId: String, isDisabled: Boolean) {
        _properties.value = _properties.value.map {
            if (it.id == propertyId) it.copy(isNotificationsDisabled = isDisabled) else it
        }
        db.collection("properties").document(propertyId).update("isNotificationsDisabled", isDisabled)
            .addOnSuccessListener {
                mainViewModel.triggerNotification(if (isDisabled) "🔕 تم كتم الإشعارات للمعلن" else "🔔 تم تفعيل الإشعارات للمعلن")
            }
    }

fun setPropertyPaymentEnabled(propertyId: String, isEnabled: Boolean) {
        db.collection("properties").document(propertyId).update("paymentEnabled", isEnabled)
            .addOnSuccessListener {
                mainViewModel.triggerNotification(if (isEnabled) "💳 تم تفعيل نظام الدفع للعقار" else "🔒 تم تعطيل نظام الدفع للعقار")
            }
    }

fun togglePropertyBlocked(propertyId: String, isBlocked: Boolean) {
        db.collection("properties").document(propertyId).update("isBlocked", isBlocked)
            .addOnSuccessListener {
                mainViewModel.triggerNotification(if (isBlocked) "🚫 تم حظر العقار" else "✅ تم إلغاء حظر العقار")
            }
    }

fun approvePropertyPdf(propertyId: String, approve: Boolean) {
        db.collection("properties").document(propertyId).get().addOnSuccessListener { snapshot ->
            val prop = snapshot.toObject(com.example.data.PropertyEntity::class.java)
            if (prop != null) {
                db.collection("properties").document(propertyId).set(prop.copy(pdfStatus = if (approve) "APPROVED" else "REJECTED"))
                    .addOnSuccessListener {
                        mainViewModel.triggerNotification(if (approve) "✅ تم قبول ملف الـ PDF للعقار بنجاح!" else "❌ تم رفض ملف الـ PDF للعقار.")
                    }
            }
        }
    }

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
                mainViewModel.triggerNotification(if (finalJob.isApproved) "✅ تم حفظ ونشر الإعلان الوظيفي بنجاح!" else "📨 تم تقديم إعلان الوظيفة للمراجع من قبل الأدمن!")
            }
            .addOnFailureListener {
                mainViewModel.triggerNotification("⚠️ تم إدراج الوظيفة محلياً وفي انتظار المزامنة")
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
                mainViewModel.triggerNotification("🗑️ تم نقل الإعلان الوظيفي لسلة المحذوفات")
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
                mainViewModel.triggerNotification("♻️ تم استعادة الإعلان الوظيفي بنجاح")
            }
    }

fun deleteJobPermanently(jobId: String) {
        db.collection("jobs").document(jobId).delete()
            .addOnSuccessListener {
                mainViewModel.triggerNotification("🗑️ تم حذف الإعلان الوظيفي نهائياً من النظام")
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
                mainViewModel.triggerNotification(if (isApproved) "✅ تم قبول ونشر إعلان الوظيفة بنجاح!" else "❌ تم رفض إعلان الوظيفة")
            }
    }

fun setJobBlocked(jobId: String, isBlocked: Boolean, reason: String = "") {
        val updates = mapOf(
            "isBlocked" to isBlocked,
            "blockReason" to reason
        )
        db.collection("jobs").document(jobId).update(updates)
            .addOnSuccessListener {
                mainViewModel.triggerNotification(if (isBlocked) "🚫 تم حظر الإعلان الوظيفي ($reason)" else "✅ تم إلغاء حظر الإعلان الوظيفي")
            }
    }

fun setJobPinned(jobId: String, isPinned: Boolean) {
        db.collection("jobs").document(jobId).update("isPinned", isPinned)
            .addOnSuccessListener {
                mainViewModel.triggerNotification(if (isPinned) "📌 تم تثبيت الوظيفة في الصدارة" else "📌 تم إلغاء تثبيت الوظيفة")
            }
    }

fun setJobVip(jobId: String, isVip: Boolean) {
        db.collection("jobs").document(jobId).update("isVip", isVip)
            .addOnSuccessListener {
                mainViewModel.triggerNotification(if (isVip) "🏆 تم تمييز الإعلان الوظيفي كـ VIP" else "🔒 تم إلغاء شارة VIP عن الوظيفة")
            }
    }

fun setJobChatDisabled(jobId: String, isDisabled: Boolean) {
        db.collection("jobs").document(jobId).update("isChatDisabled", isDisabled)
            .addOnSuccessListener {
                mainViewModel.triggerNotification(if (isDisabled) "🔇 تم إيقاف الدردشة للإعلان الوظيفي" else "💬 تم تفعيل الدردشة للإعلان الوظيفي")
            }
    }

fun submitJobApplication(application: com.example.data.JobApplicationEntity) {
        val targetId = db.collection("job_applications").document().id
        val finalApp = application.copy(id = targetId)
        db.collection("job_applications").document(targetId).set(finalApp)
            .addOnSuccessListener {
                mainViewModel.triggerNotification("📨 تم إرسال طلب التقديم على الوظيفة بنجاح!")
            }
            .addOnFailureListener {
                mainViewModel.triggerNotification("❌ فشل تقديم الطلب: ${it.message}")
            }
    }

fun updateJobApplicationStatus(appId: String, status: String) {
        db.collection("job_applications").document(appId).update("status", status)
            .addOnSuccessListener {
                mainViewModel.triggerNotification("✅ تم تحديث حالة طلب التقديم إلى: $status")
            }
    }

fun acceptJobApplication(appId: String) {
        updateJobApplicationStatus(appId, "ACCEPTED")
    }

fun rejectJobApplication(appId: String, reason: String) {
        db.collection("job_applications").document(appId).update("status", "REJECTED", "rejectionReason", reason)
            .addOnSuccessListener {
                mainViewModel.triggerNotification("❌ تم رفض طلب التقديم للوظيفة مع إرسال السبب: $reason")
            }
    }

fun deleteJobApplication(appId: String) {
        db.collection("job_applications").document(appId).delete()
            .addOnSuccessListener {
                mainViewModel.triggerNotification("🗑️ تم حذف طلب التقديم من النظام بنجاح")
            }
    }

fun submitReport(report: com.example.data.ReportEntity, onComplete: () -> Unit = {}) {
        db.collection("reports").document(report.id).set(report)
            .addOnSuccessListener {
                onComplete()
            }
    }

fun deleteReport(reportId: String) {
        db.collection("reports").document(reportId).delete()
        mainViewModel.triggerNotification("🗑️ تم حذف البلاغ من النظام")
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
        mainViewModel.triggerNotification("📢 تم إرسال بلاغك ضد $providerName")
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
        mainViewModel.triggerNotification("🎫 تم إضافة كوبون جديد بنجاح: $code")
    }

fun saveCoupon(coupon: CouponEntity) {
        val couponId = if (coupon.id.isBlank()) UUID.randomUUID().toString() else coupon.id
        val updated = coupon.copy(id = couponId)
        db.collection("coupons").document(couponId).set(updated)
        mainViewModel.triggerNotification("🎫 تم حفظ وتحديث الكوبون بنجاح: ${updated.code}")
    }

fun deleteCoupon(couponId: String) {
        db.collection("coupons").document(couponId).delete()
        mainViewModel.triggerNotification("🗑️ تم حذف الكوبون")
    }

fun saveInternalWallet(wallet: com.example.data.InternalWalletEntity) {
        val targetId = if (wallet.id.isEmpty()) db.collection("internal_wallets").document().id else wallet.id
        val finalW = wallet.copy(id = targetId, updatedAt = System.currentTimeMillis())
        db.collection("internal_wallets").document(targetId).set(finalW)
            .addOnSuccessListener {
                mainViewModel.triggerNotification("✅ تم حفظ بيانات المحفظة الرقمية الداخلية (${finalW.ownerName})")
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
        mainViewModel.triggerNotification("💸 تم تنفيذ عملية ($type) بمبلغ $amount ريال للمحفظة ${currentW.ownerName}. الرصيد الجديد: $newBalance ريال")
    }

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
            mainViewModel.triggerNotification("✅ تم إضافة المحفظة بنجاح!")
        }.addOnFailureListener {
            mainViewModel.triggerNotification("❌ فشل إضافة المحفظة: ${it.message}")
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
            mainViewModel.triggerNotification("✅ تم تحديث المحفظة بنجاح!")
        }.addOnFailureListener {
            mainViewModel.triggerNotification("❌ فشل تحديث المحفظة: ${it.message}")
        }
    }

fun deletePaymentWallet(walletId: String) {
        db.collection("payment_wallets").document(walletId).delete().addOnSuccessListener {
            mainViewModel.triggerNotification("🗑️ تم حذف المحفظة بنجاح!")
        }.addOnFailureListener {
            mainViewModel.triggerNotification("❌ فشل حذف المحفظة: ${it.message}")
        }
    }

fun togglePaymentWalletVisibility(walletId: String, currentVisible: Boolean) {
        val newStatus = !currentVisible
        db.collection("payment_wallets").document(walletId).update("isVisibleToUsers", newStatus).addOnSuccessListener {
            mainViewModel.triggerNotification(if (newStatus) "👁️ تم إظهار المحفظة للمستخدمين" else "🙈 تم إخفاء المحفظة عن المستخدمين")
        }.addOnFailureListener {
            mainViewModel.triggerNotification("❌ فشل تغيير حالة إظهار المحفظة")
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
        
        val settingsVal = mainViewModel.settingsViewModel._settings.value
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
            mainViewModel.triggerNotification("✅ تم إنشاء طلب الدفع بنجاح!")
        }.addOnFailureListener {
            mainViewModel.triggerNotification("❌ فشل إنشاء طلب الدفع: ${it.message}")
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
            mainViewModel.triggerNotification("✅ تم تقديم إثبات التحويل بنجاح! بانتظار مراجعة الإدارة.")
        }.addOnFailureListener {
            mainViewModel.triggerNotification("❌ فشل تأكيد الدفع: ${it.message}")
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
            mainViewModel.triggerNotification(if (isVerified) "✅ تم قبول وتأكيد عملية الدفع بنجاح!" else "❌ تم رفض عملية الدفع.")
            
            db.collection("payments").document(paymentId).get().addOnSuccessListener { snapshot ->
                val payment = snapshot.toObject(PaymentEntity::class.java)
                if (payment != null) {
                    if (payment.isLinkedToBooking && payment.bookingId.isNotEmpty()) {
                        db.collection("bookings").document(payment.bookingId).update("status", if (isVerified) "APPROVED" else "PENDING")
                    }
                }
            }
        }.addOnFailureListener {
            mainViewModel.triggerNotification("❌ فشل التحقق من الدفع: ${it.message}")
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
            mainViewModel.triggerNotification("🔄 تم استرداد المبلغ بنجاح.")
        }.addOnFailureListener {
            mainViewModel.triggerNotification("❌ فشل استرداد الدفع: ${it.message}")
        }
    }

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
                    mainViewModel.triggerNotification("✅ تم حفظ المنتج بنجاح!")
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

fun updateProductPrice(productId: String, newPrice: Double) {
        val existing = _products.value.find { it.id == productId }
        if (existing != null) {
            val updated = existing.copy(price = newPrice, oldPrice = if (existing.price != newPrice) existing.price else existing.oldPrice)
            _products.value = _products.value.map { if (it.id == productId) updated else it }
            db.collection("products").document(productId).update("price", newPrice, "oldPrice", updated.oldPrice)
                .addOnSuccessListener {
                    mainViewModel.triggerNotification("⚡ تم تحديث السعر فورياً لجميع العملاء!")
                }
        } else {
            db.collection("products").document(productId).update("price", newPrice)
        }
    }

fun saveOffer(offer: com.example.data.models.Offer) {
        val targetId = if (offer.id.isEmpty()) db.collection("offers").document().id else offer.id
        val finalOffer = offer.copy(id = targetId, updatedAt = System.currentTimeMillis())

        val currentList = mainViewModel.instantRequestViewModel._offers.value.filter { it.id != targetId }.toMutableList()
        currentList.add(finalOffer)
        mainViewModel.instantRequestViewModel._offers.value = currentList

        db.collection("offers").document(targetId).set(finalOffer)
            .addOnSuccessListener {
                mainViewModel.triggerNotification("🎁 تم نشر العرض وتحديث الأسعار فورياً!")
            }
            .addOnFailureListener {
                mainViewModel.triggerNotification("⚠️ فشل حفظ العرض: ${it.localizedMessage}")
            }
    }

fun deleteOffer(offerId: String) {
        mainViewModel.instantRequestViewModel._offers.value = mainViewModel.instantRequestViewModel._offers.value.filter { it.id != offerId }
        db.collection("offers").document(offerId).delete()
            .addOnSuccessListener {
                mainViewModel.triggerNotification("🗑️ تم حذف العرض بنجاح!")
            }
    }

fun toggleOfferStatus(offerId: String, isActive: Boolean) {
        mainViewModel.instantRequestViewModel._offers.value = mainViewModel.instantRequestViewModel._offers.value.map { if (it.id == offerId) it.copy(isActive = isActive) else it }
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

fun saveCustomProfileTab(tab: com.example.data.CustomProfileTabEntity) {
        val targetId = if (tab.id.isEmpty()) java.util.UUID.randomUUID().toString().take(6) else tab.id
        val finalTab = tab.copy(id = targetId)
        db.collection("custom_profile_tabs").document(targetId).set(finalTab)
        mainViewModel.triggerNotification("📑 تم حفظ التبويب المخصص بنجاح: ${tab.title}")
    }

fun deleteCustomProfileTab(tabId: String) {
        db.collection("custom_profile_tabs").document(tabId).delete()
        mainViewModel.triggerNotification("🗑️ تم حذف التبويب المخصص")
    }

fun toggleCustomProfileTab(tabId: String) {
        val current = _customProfileTabs.value.find { it.id == tabId }
        if (current != null) {
            val updated = current.copy(isEnabled = !current.isEnabled)
            db.collection("custom_profile_tabs").document(tabId).set(updated)
            mainViewModel.triggerNotification(if (updated.isEnabled) "🟢 تم تفعيل التبويب" else "🔴 تم إيقاف التبويب")
        }
    }

fun addNewCategory(nameAr: String, nameEn: String, icon: String, description: String, parentId: String = "", isMainCategory: Boolean = true) {
        homeViewModel.addNewCategory(nameAr, nameEn, icon, description, parentId, isMainCategory)
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
        mainViewModel.triggerNotification("✏️ تم تعديل القسم وتحديث هيكلته بنجاح: $newName")
    }

fun deleteCategory(categoryId: String) {
        homeViewModel.deleteCategory(categoryId)
    }

fun togglePinCategory(categoryId: String) {
        db.collection("categories").document(categoryId).get().addOnSuccessListener { snapshot ->
            val cat = snapshot.toObject(com.example.data.CategoryEntity::class.java)
            if (cat != null) {
                val updated = cat.copy(isPinned = !cat.isPinned)
                db.collection("categories").document(categoryId).set(updated)
                mainViewModel.triggerNotification(if (updated.isPinned) "📌 تم تثبيت القسم في البداية" else "🔓 تم إلغاء تثبيت القسم")
            }
        }
    }

fun mergeCategories(sourceCategoryId: String, targetCategoryId: String) {
        if (sourceCategoryId == targetCategoryId) {
            mainViewModel.triggerNotification("⚠️ لا يمكن دمج القسم مع نفسه!")
            return
        }

        // 1. Move approved mainViewModel.homeViewModel.providers of sourceCategory to targetCategory
        db.collection("providers").whereEqualTo("categoryId", sourceCategoryId).get().addOnSuccessListener { snapshot ->
            for (doc in snapshot.documents) {
                doc.reference.update("categoryId", targetCategoryId)
            }
        }

        // 2. Move pending mainViewModel.homeViewModel.providers of sourceCategory to targetCategory
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
            mainViewModel.triggerNotification("✅ تم دمج القسمين وتحويل كافة الفنيين والمتاجر بنجاح!")
        }
    }

fun saveCategoryEntity(cat: CategoryEntity) {
        val catId = cat.id.ifEmpty { UUID.randomUUID().toString().take(6) }
        val updated = cat.copy(id = catId)
        db.collection("categories").document(catId).set(updated)
        mainViewModel.triggerNotification("📁 تم حفظ وتحديث بيانات القسم: ${updated.name}")
    }

fun addSubCategory(parentId: String, nameAr: String, icon: String) {
        val nextId = UUID.randomUUID().toString().take(6)
        val subCat = CategoryEntity(
            id = nextId,
            name = nameAr,
            icon = icon,
            parentId = parentId,
            isMainCategory = false,
            order = mainViewModel.homeViewModel._categories.value.size + 1
        )
        db.collection("categories").document(nextId).set(subCat)
        mainViewModel.triggerNotification("📂 تم إضافة قسم فرعي جديد: $nameAr")
    }

fun convertCategoryType(catId: String, newParentId: String, isMain: Boolean) {
        val cat = mainViewModel.homeViewModel._categories.value.find { it.id == catId }
        if (cat != null) {
            val updated = cat.copy(parentId = newParentId, isMainCategory = isMain)
            db.collection("categories").document(catId).set(updated)
            mainViewModel.triggerNotification("🔄 تم تغيير تصنيف القسم بنجاح")
        }
    }

fun reorderCategories(newOrderedList: List<CategoryEntity>) {
        newOrderedList.forEachIndexed { index, cat ->
            val updated = cat.copy(order = index + 1)
            db.collection("categories").document(cat.id).set(updated)
        }
    }

fun addNewCity(nameAr: String, nameEn: String, icon: String = "📍", photoUrl: String = "", sortOrder: Int = 0) {
        val nextId = "city_" + UUID.randomUUID().toString().take(4)
        val city = CityEntity(nextId, nameAr, nameEn, icon.ifEmpty { "📍" }, photoUrl, sortOrder)
        db.collection("cities").document(nextId).set(city)
        mainViewModel.triggerNotification("🏙️ تم إضافة مدينة/محافظة: $nameAr")
    }

fun updateCity(city: CityEntity) {
        if (city.id.isEmpty()) return
        db.collection("cities").document(city.id).set(city)
        mainViewModel.triggerNotification("💾 تم تحديث بيانات المدينة/المحافظة: ${city.nameAr}")
    }

fun removeCity(cityId: String) {
        db.collection("cities").document(cityId).delete()
        mainViewModel.triggerNotification("🗑️ تم حذف المدينة")
    }

fun removeProvider(providerId: String) {
        db.collection("providers").document(providerId).get().addOnSuccessListener { snapshot ->
            val p = snapshot.toObject(com.example.data.ProviderEntity::class.java)
            if (p != null) {
                db.collection("providers").document(providerId).set(
                    p.copy(isDeleted = true, deletedAt = System.currentTimeMillis())
                ).addOnSuccessListener {
                    mainViewModel.triggerNotification("🗑️ تم حذف حساب الفني منطقياً بنجاح (حذف مؤقت) ويمكنك استعادته من لوحة التحكم في أي وقت")
                }
            } else {
                db.collection("providers").document(providerId).delete().addOnSuccessListener {
                    mainViewModel.triggerNotification("🗑️ تم حذف حساب الفني نهائياً من الدليل")
                }
            }
        }.addOnFailureListener {
            db.collection("providers").document(providerId).delete().addOnSuccessListener {
                mainViewModel.triggerNotification("🗑️ تم حذف حساب الفني")
            }
        }
    }

fun removeProviderPermanently(providerId: String) {
        db.collection("providers").document(providerId).delete().addOnSuccessListener {
            mainViewModel.triggerNotification("🗑️ تم حذف حساب الفني نهائياً وبشكل كامل من خوادم الدليل")
        }.addOnFailureListener { e ->
            mainViewModel.triggerNotification("❌ فشل حذف الفني نهائياً: ${e.message}")
        }
    }

fun restoreProvider(providerId: String) {
        db.collection("providers").document(providerId).get().addOnSuccessListener { snapshot ->
            val p = snapshot.toObject(ProviderEntity::class.java)
            if (p != null) {
                db.collection("providers").document(providerId).set(
                    p.copy(isDeleted = false, deletedAt = null)
                )
                mainViewModel.triggerNotification("🟢 تم استعادة وتفعيل حساب الفني ${p.name} بنجاح!")
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
        mainViewModel.triggerNotification(if (isPinned) "📌 تم تثبيت الفني" else "📌 تم إلغاء تثبيت الفني")
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
        mainViewModel.triggerNotification(if (isRecommended) "⭐ تمت توصية الفني" else "⭐ تم إلغاء توصية الفني")
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
        mainViewModel.triggerNotification(if (isVerified) "🔷 تم توثيق الفني بالشارة الزرقاء" else "🔷 تم إلغاء توثيق الفني")
    }

fun toggleProviderSubscription(providerId: String, status: String) {
        db.collection("providers").document(providerId).get().addOnSuccessListener { snapshot ->
            val p = snapshot.toObject(ProviderEntity::class.java)
            if (p != null) {
                db.collection("providers").document(providerId).set(p.copy(subscriptionStatus = status))
            }
        }
        mainViewModel.triggerNotification(if (status == "APPROVED") "✨ تم تفعيل العضوية الذهبية للفني" else "✨ تم إلغاء العضوية الذهبية")
    }

fun toggleProviderSubscription(providerId: String) {
        val provider = mainViewModel.homeViewModel._providers.value.find { it.id == providerId }
        provider?.let {
            val updated = it.copy(subscriptionStatus = if (it.subscriptionStatus == "APPROVED") "EXPIRED" else "APPROVED")
            mainViewModel.homeViewModel._providers.value = mainViewModel.homeViewModel._providers.value.map { item -> if (item.id == providerId) updated else item }
            db.collection("providers").document(providerId).set(updated)
        }
    }

fun setProviderChatDisabled(providerId: String, disabled: Boolean) {
        db.collection("providers").document(providerId).get().addOnSuccessListener { snapshot ->
            val p = snapshot.toObject(ProviderEntity::class.java)
            if (p != null) {
                db.collection("providers").document(providerId).set(p.copy(isChatDisabled = disabled))
            }
        }
        mainViewModel.triggerNotification(if (disabled) "🔇 تم إيقاف دردشة الفني إدارياً" else "🔊 تم تفعيل دردشة الفني")
    }

fun setProviderNotificationsDisabled(providerId: String, disabled: Boolean) {
        db.collection("providers").document(providerId).get().addOnSuccessListener { snapshot ->
            val p = snapshot.toObject(ProviderEntity::class.java)
            if (p != null) {
                db.collection("providers").document(providerId).set(p.copy(isNotificationsDisabled = disabled))
            }
        }
        mainViewModel.triggerNotification(if (disabled) "🔕 تم تعطيل إشعارات الفني إدارياً" else "🔔 تم تفعيل إشعارات الفني")
    }

fun setProviderPaymentRequired(providerId: String, required: Boolean) {
        db.collection("providers").document(providerId).get().addOnSuccessListener { snapshot ->
            val p = snapshot.toObject(ProviderEntity::class.java)
            if (p != null) {
                db.collection("providers").document(providerId).set(p.copy(isPaymentRequired = required))
            }
        }
        mainViewModel.triggerNotification(if (required) "💳 تم ربط حساب الفني بنظام الدفع والعمولة الإلزامية" else "🔓 تم استثناء الفني من شروط الدفع المسبق والعمولة")
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
        mainViewModel.triggerNotification("💸 تم تجديد وتمديد اشتراك فني بنجاح!")
    }

fun toggleProviderBlock(providerId: String) {
        val provider = mainViewModel.homeViewModel._providers.value.find { it.id == providerId }
        provider?.let {
            val updated = it.copy(isBlocked = !it.isBlocked)
            mainViewModel.homeViewModel._providers.value = mainViewModel.homeViewModel._providers.value.map { item -> if (item.id == providerId) updated else item }
            db.collection("providers").document(providerId).set(updated)
            if (updated.isBlocked) {
                mainViewModel.triggerNotification("🚫 تم حظر الفني ${it.name} بنجاح")
            } else {
                mainViewModel.triggerNotification("🟢 تم إلغاء حظر الفني ${it.name}")
            }
        }
    }

fun toggleProviderStatus(provider: ProviderEntity) {
        val updated = provider.copy(isAvailable = !provider.isAvailable)
        db.collection("providers").document(provider.id).set(updated)
        mainViewModel.triggerNotification("🔄 تم تغيير حالة التوفر لـ ${provider.name}")
    }

fun toggleProviderPin(providerId: String) {
        val provider = mainViewModel.homeViewModel._providers.value.find { it.id == providerId }
        provider?.let {
            val updated = it.copy(isVip = !it.isVip)
            mainViewModel.homeViewModel._providers.value = mainViewModel.homeViewModel._providers.value.map { item -> if (item.id == providerId) updated else item }
            db.collection("providers").document(providerId).set(updated)
        }
    }

fun toggleProviderVerification(providerId: String) {
        val provider = mainViewModel.homeViewModel._providers.value.find { it.id == providerId }
        provider?.let {
            val updated = it.copy(isVerified = !it.isVerified)
            mainViewModel.homeViewModel._providers.value = mainViewModel.homeViewModel._providers.value.map { item -> if (item.id == providerId) updated else item }
            db.collection("providers").document(providerId).set(updated)
        }
    }

fun toggleProviderRecommendation(providerId: String) {
        val provider = mainViewModel.homeViewModel._providers.value.find { it.id == providerId }
        provider?.let {
            val updated = it.copy(isRecommended = !it.isRecommended)
            mainViewModel.homeViewModel._providers.value = mainViewModel.homeViewModel._providers.value.map { item -> if (item.id == providerId) updated else item }
            db.collection("providers").document(providerId).set(updated)
        }
    }

fun updateProviderEntity(provider: ProviderEntity) {
        mainViewModel.homeViewModel._providers.value = mainViewModel.homeViewModel._providers.value.map { item -> if (item.id == provider.id) provider else item }
        db.collection("providers").document(provider.id).set(provider)
        mainViewModel.triggerNotification("💾 تم تحديث بيانات مقدم الخدمة ${provider.name} بنجاح")
    }

fun editProviderPhoneAndCategory(providerId: String, newPhone: String, newCategoryId: String) {
        db.collection("providers").document(providerId).get().addOnSuccessListener { snapshot ->
            val p = snapshot.toObject(ProviderEntity::class.java)
            if (p != null) {
                db.collection("providers").document(providerId).set(p.copy(phone = newPhone, categoryId = newCategoryId))
            }
        }
        mainViewModel.triggerNotification("✏️ تم تعديل بيانات اتصال وتصنيف الفني")
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
        mainViewModel.triggerNotification("➕ تم إضافة الفني $name يدوياً")
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
        mainViewModel.triggerNotification("✨ تم إضافة الفني $name يدوياً بالدليل اليمني بنجاح")
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
            order = mainViewModel.homeViewModel._banners.value.size + 1
        )
        db.collection("banners").document(banner.id).set(banner)
        mainViewModel.triggerNotification("🖼️ تم إضافة إعلان جديد: $title")
    }

fun addBanner(title: String, url: String, redirect: String, type: String, size: String, duration: Int, displayTime: String = "طوال اليوم") {
        addNewBanner(title, url, redirect, type, size, duration, displayTime)
    }

fun deleteBanner(bannerId: String) {
        homeViewModel.deleteBanner(bannerId)
    }

fun reorderBanners(newOrderedList: List<BannerEntity>) {
        newOrderedList.forEachIndexed { index, banner ->
            val updated = banner.copy(order = index + 1)
            db.collection("banners").document(banner.id).set(updated)
        }
    }

fun placeOrder(order: com.example.data.OrderEntity) {
        val targetId = db.collection("orders").document().id
        val finalOrder = order.copy(id = targetId)
        db.collection("orders").document(targetId).set(finalOrder).addOnSuccessListener {
            mainViewModel.triggerNotification("🛍️ تم تسجيل طلبك بنجاح! رقم الطلب: ${targetId.take(6)}")
        }
    }

fun updateOrderStatus(orderId: String, status: String) {
        db.collection("orders").document(orderId).get().addOnSuccessListener { snap ->
            val order = snap.toObject(com.example.data.OrderEntity::class.java)
            if (order != null) {
                db.collection("orders").document(orderId).set(order.copy(status = status)).addOnSuccessListener {
                    mainViewModel.triggerNotification("📦 تم تحديث حالة الطلب إلى $status")
                }
            }
        }
    }

fun deleteOrder(orderId: String) {
        db.collection("orders").document(orderId).delete().addOnSuccessListener {
            mainViewModel.triggerNotification("🗑️ تم حذف الطلب بنجاح.")
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
                    mainViewModel.triggerNotification("🗑️ تم حذف جميع الطلبات بنجاح.")
                }
            }
    }

fun addRating(rating: com.example.data.RatingEntity) {
        val targetId = db.collection("ratings").document().id
        val finalRating = rating.copy(id = targetId)
        db.collection("ratings").document(targetId).set(finalRating).addOnSuccessListener {
            mainViewModel.triggerNotification("⭐ شكراً لتقييمك! تم إرسال تقييمك بنجاح.")
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
                mainViewModel.triggerNotification("✅ تم إضافة الرد على التعليق بنجاح!")
            }
            .addOnFailureListener {
                mainViewModel.triggerNotification("❌ فشل إضافة الرد: ${it.message}")
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

fun submitRating(providerId: String, rating: Int) {
        mainViewModel.triggerNotification("⭐ شكراً لتقييمك $rating نجوم!")
    }

fun submitRating(ratingEntity: com.example.data.RatingEntity, onComplete: () -> Unit = {}) {
        db.collection("ratings").document(ratingEntity.id).set(ratingEntity)
            .addOnSuccessListener {
                recalculateTargetRating(ratingEntity.targetId, ratingEntity.targetType)
                onComplete()
            }
    }

fun recalculateTargetRating(targetId: String, targetType: String) {
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

fun logAdminActivity(action: String) {
        val id = db.collection("activity_logs").document().id
        val log = com.example.data.ActivityLogEntity(id = id, action = action, timestamp = System.currentTimeMillis())
        db.collection("activity_logs").document(id).set(log)
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

fun checkAndGetDuplicateAccountType(phone: String, excludeId: String): String? {
        val cleanInput = phone.trim().replace(" ", "").replace("+", "")
        if (cleanInput.isEmpty()) return null
        
        // 1. Check in active mainViewModel.homeViewModel.providers
        val dupProvider = mainViewModel.homeViewModel._providers.value.any { !it.isDeleted && it.phone.trim().replace(" ", "").replace("+", "") == cleanInput && it.id != excludeId }
        if (dupProvider) return "مقدم خدمة نشط (فني)"
        
        // 2. Check in pending mainViewModel.homeViewModel.providers
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

fun redirectBookingToEntity(bookingId: String, targetEntityId: String, targetEntityName: String, targetPhone: String) {
        val updates = mapOf(
            "providerId" to targetEntityId,
            "providerName" to targetEntityName,
            "providerPhone" to targetPhone,
            "status" to "PENDING"
        )
        db.collection("bookings").document(bookingId).update(updates)
            .addOnSuccessListener {
                mainViewModel.triggerNotification("🔄 تم توجيه الحجز للجهة/الفني ($targetEntityName) بنجاح!")
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
        mainViewModel.triggerNotification("✅ تم إلغاء حظر الكيان بنجاح!")
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
                mainViewModel.triggerNotification("🔔 تم إرسال الإشعار لجميع المتقدمين للوظائف بنجاح!")
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
            mainViewModel.triggerNotification("📋 تم نسخ بيانات المتقدمين للوظائف بصيغة CSV إلى الحافظة بنجاح (${apps.size} متقدم)!")
        } catch (e: Exception) {
            mainViewModel.triggerNotification("❌ فشل تصدير البيانات: ${e.message}")
        }
    }

}