package com.example.ui.helpers

import com.example.data.*
import com.example.ui.*
import com.example.data.models.*
import com.example.ui.MainViewModel
import com.example.ui.viewmodels.BookingDistributionMode
import com.example.ui.viewmodels.BookingFormFields
import com.google.firebase.firestore.*
import kotlinx.coroutines.tasks.await

/**
 * Helper class responsible for real-time Firestore listeners and synchronization.
 * Guarantees safe listener registration and teardown on ViewModel cleared.
 */
class RealtimeSyncHelper(private val db: FirebaseFirestore) {

    companion object {
        /**
         * Real-time listeners limit (default 50). Configurable to avoid excessive bandwidth/reads
         * while keeping active entities responsive. For full catalogs, pagination is used.
         */
        var REALTIME_QUERY_LIMIT: Long = 50L // TODO: Real pagination when data > 50

        /**
         * Default on-demand query limit for paginated data loading.
         */
        var ON_DEMAND_FETCH_LIMIT: Long = 50L // TODO: Real pagination when data > 50
    }

    val firestoreListeners = java.util.concurrent.CopyOnWriteArrayList<ListenerRegistration>()

    private fun Query.addSnapshotListenerReg(listener: EventListener<QuerySnapshot>): ListenerRegistration? {
        return try {
            val reg = this.addSnapshotListener(listener)
            firestoreListeners.add(reg)
            reg
        } catch (e: Exception) {
            e.printStackTrace()
            com.example.utils.AppErrorLogManager.logFirestoreError("RealtimeSyncHelper", "Failed to add Query listener", e)
            null
        }
    }

    private fun DocumentReference.addSnapshotListenerReg(listener: EventListener<DocumentSnapshot>): ListenerRegistration? {
        return try {
            val reg = this.addSnapshotListener(listener)
            firestoreListeners.add(reg)
            reg
        } catch (e: Exception) {
            e.printStackTrace()
            com.example.utils.AppErrorLogManager.logFirestoreError("RealtimeSyncHelper", "Failed to add Document listener", e)
            null
        }
    }

    fun clearListeners() {
        try {
            val iterator = firestoreListeners.iterator()
            while (iterator.hasNext()) {
                try {
                    iterator.next().remove()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            firestoreListeners.clear()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setupRealtimeFirestoreListeners(appState: com.example.ui.helpers.AppState) {
        // 1. Settings (Document main_settings)
        db.collection("settings").document("main_settings").addSnapshotListenerReg { snapshot, error ->
            if (error != null) {
                error.printStackTrace()
                return@addSnapshotListenerReg
            }
            if (snapshot != null && snapshot.exists()) {
                try {
                    // Security migration: Cleanup legacy adminPassword from Firestore if present
                    if (snapshot.contains("adminPassword")) {
                        snapshot.reference.update("adminPassword", com.google.firebase.firestore.FieldValue.delete())
                    }
                    snapshot.toObject(AdminSettingsEntity::class.java)?.let {
                        appState._settings.value = it.copy(adminPassword = "")
                        appState._maxKmRadius.value = it.maxSearchRadiusKm
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                appState._settings.value = AdminSettingsEntity()
            }
        }

        // 2. Categories (Listener 2)
        db.collection("categories").addSnapshotListenerReg { snapshot, error ->
            if (error != null) {
                error.printStackTrace()
                return@addSnapshotListenerReg
            }
            if (snapshot != null) {
                val fetched = snapshot.documents.mapNotNull { doc ->
                    try {
                        val obj = doc.toObject(CategoryEntity::class.java)
                        if (obj != null && obj.id.isEmpty()) {
                            obj.copy(id = doc.id)
                        } else {
                            obj
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }.distinctBy { it.id }.sortedWith(compareByDescending<CategoryEntity> { it.isPinned }.thenBy { it.order })
                appState._categories.value = fetched
            }
        }

        // 3. Cities (Listener 3)
        db.collection("cities").addSnapshotListenerReg { snapshot, error ->
            if (error != null) {
                error.printStackTrace()
                return@addSnapshotListenerReg
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
                appState._cities.value = fetched
            }
        }

        // 4. Providers (Realtime limit with pagination support) (Listener 4)
        db.collection("providers").limit(REALTIME_QUERY_LIMIT).addSnapshotListenerReg { snapshot, error ->
            appState._isProvidersLoading.value = false
            if (error != null) {
                error.printStackTrace()
                return@addSnapshotListenerReg
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

                appState._providers.value = activeList
                appState._deletedProviders.value = deletedList
            }
        }

        // 5. Stores (Realtime limit with pagination support) (Listener 5)
        db.collection("stores").limit(REALTIME_QUERY_LIMIT).addSnapshotListenerReg { snapshot, error ->
            if (error == null && snapshot != null) {
                val fetched = snapshot.documents.mapNotNull { doc ->
                    try {
                        val obj = doc.toObject(StoreEntity::class.java)
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
                            StoreEntity(
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
                appState._stores.value = fetched.filter { !it.isDeleted }
            }
        }

        // 6. Properties (Realtime limit with pagination support) (Listener 6)
        db.collection("properties").limit(REALTIME_QUERY_LIMIT).addSnapshotListenerReg { snapshot, error ->
            if (error == null && snapshot != null) {
                val fetched = snapshot.documents.mapNotNull { doc ->
                    try {
                        val obj = doc.toObject(PropertyEntity::class.java)
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
                            PropertyEntity(
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
                appState._properties.value = fetched.filter { !it.isDeleted }
            }
        }

        // 7. Notifications (Realtime limit with pagination support) (Listener 7)
        db.collection("notifications").orderBy("timestamp", Query.Direction.DESCENDING).limit(REALTIME_QUERY_LIMIT).addSnapshotListenerReg { snapshot, error ->
            if (error != null) {
                error.printStackTrace()
                return@addSnapshotListenerReg
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
                appState._notifications.value = fetched
            }
        }

        // 8. Chat Channels (Realtime limit with pagination support) (Listener 8)
        db.collection("chat_channels").orderBy("timestamp", Query.Direction.DESCENDING).limit(REALTIME_QUERY_LIMIT).addSnapshotListenerReg { snapshot, error ->
            appState._isChatChannelsLoading.value = false
            if (error != null) {
                error.printStackTrace()
                return@addSnapshotListenerReg
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
                appState._chatChannels.value = fetched
            }
        }
    }

    // ============================================
    // On-Demand Loading (Suspend functions - Load on Demand)
    // ============================================

    suspend fun loadOnDemandInitialData(appState: AppState) {
        try {
            // Read booking settings once
            val bfDoc = db.collection("settings").document("booking_fields").get().await()
            if (bfDoc.exists()) {
                bfDoc.toObject(BookingFormFields::class.java)?.let { appState._bookingFormFields.value = it }
            }
            val dmDoc = db.collection("settings").document("distribution_mode").get().await()
            if (dmDoc.exists()) {
                dmDoc.getString("mode")?.let { modeStr ->
                    try { appState._distributionMode.value = BookingDistributionMode.valueOf(modeStr) } catch (_: Exception) {}
                }
            }

            appState._banners.value = loadBanners(ON_DEMAND_FETCH_LIMIT)
            appState._customProfileTabs.value = loadCustomProfileTabs()
            appState._colorPalettes.value = loadColorThemes()
            appState._products.value = loadProducts(ON_DEMAND_FETCH_LIMIT)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun loadRegisteredUsers(limit: Long = ON_DEMAND_FETCH_LIMIT): List<Map<String, Any>> {
        return try {
            db.collection("registered_users")
                .limit(limit)
                .get()
                .await()
                .documents
                .mapNotNull { doc ->
                    val data = doc.data?.toMutableMap() ?: mutableMapOf()
                    data["id"] = doc.id
                    data
                }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun loadInternalWallets(limit: Long = ON_DEMAND_FETCH_LIMIT): List<InternalWalletEntity> {
        return try {
            db.collection("internal_wallets")
                .limit(limit)
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(InternalWalletEntity::class.java) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun loadWalletTransactions(limit: Long = ON_DEMAND_FETCH_LIMIT): List<WalletTransactionEntity> {
        return try {
            db.collection("wallet_transactions")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(WalletTransactionEntity::class.java) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun loadBanners(limit: Long = ON_DEMAND_FETCH_LIMIT): List<BannerEntity> {
        return try {
            db.collection("banners")
                .limit(limit)
                .get()
                .await()
                .documents
                .mapNotNull { doc ->
                    try {
                        doc.toObject(BannerEntity::class.java)
                    } catch (e: Exception) {
                        null
                    }
                }.sortedBy { it.order }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun loadPendingProviders(limit: Long = ON_DEMAND_FETCH_LIMIT): List<PendingProviderEntity> {
        return try {
            db.collection("pending_providers")
                .limit(limit)
                .get()
                .await()
                .documents
                .mapNotNull { doc ->
                    try {
                        val parsed = doc.toObject(PendingProviderEntity::class.java)
                        parsed?.copy(id = doc.id)
                    } catch (e: Exception) {
                        null
                    }
                }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun loadBookings(limit: Long = ON_DEMAND_FETCH_LIMIT): List<BookingEntity> {
        return try {
            db.collection("bookings")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(BookingEntity::class.java) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun loadReports(limit: Long = ON_DEMAND_FETCH_LIMIT): List<ReportEntity> {
        return try {
            db.collection("reports")
                .limit(limit)
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(ReportEntity::class.java) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun loadSupervisors(limit: Long = ON_DEMAND_FETCH_LIMIT): List<SupervisorEntity> {
        return try {
            db.collection("supervisors")
                .limit(limit)
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(SupervisorEntity::class.java) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun loadColorThemes(): List<ColorPaletteEntity> {
        return try {
            db.collection("color_themes")
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(ColorPaletteEntity::class.java) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun loadCustomProfileTabs(): List<CustomProfileTabEntity> {
        return try {
            db.collection("custom_profile_tabs")
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(CustomProfileTabEntity::class.java) }
                .sortedBy { it.displayOrder }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun loadJobs(limit: Long = ON_DEMAND_FETCH_LIMIT): List<JobEntity> {
        return try {
            db.collection("jobs")
                .limit(limit)
                .get()
                .await()
                .documents
                .mapNotNull { doc ->
                    try {
                        val obj = doc.toObject(JobEntity::class.java)
                        if (obj != null) {
                            val isDel = doc.getBoolean("isDeleted") == true || doc.getBoolean("deleted") == true
                            val act = doc.getBoolean("isActive") ?: doc.getBoolean("active") ?: true
                            val pin = doc.getBoolean("isPinned") == true || doc.getBoolean("pinned") == true
                            obj.copy(id = doc.id, isDeleted = isDel, isActive = act, isPinned = pin)
                        } else null
                    } catch (e: Exception) {
                        null
                    }
                }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun loadJobApplications(limit: Long = ON_DEMAND_FETCH_LIMIT): List<JobApplicationEntity> {
        return try {
            db.collection("job_applications")
                .limit(limit)
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(JobApplicationEntity::class.java)?.copy(id = it.id) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun loadRatings(limit: Long = ON_DEMAND_FETCH_LIMIT): List<RatingEntity> {
        return try {
            db.collection("ratings")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(RatingEntity::class.java)?.copy(id = it.id) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun loadOrders(limit: Long = ON_DEMAND_FETCH_LIMIT): List<OrderEntity> {
        return try {
            db.collection("orders")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(OrderEntity::class.java)?.copy(id = it.id) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun loadOffers(limit: Long = ON_DEMAND_FETCH_LIMIT): List<Offer> {
        return try {
            db.collection("offers")
                .limit(limit)
                .get()
                .await()
                .documents
                .mapNotNull { doc ->
                    try {
                        val obj = doc.toObject(Offer::class.java)
                        obj?.copy(id = doc.id)
                    } catch (e: Exception) {
                        null
                    }
                }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun loadRequestOffers(limit: Long = ON_DEMAND_FETCH_LIMIT): List<RequestOfferEntity> {
        return try {
            db.collection("request_offers")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(RequestOfferEntity::class.java)?.copy(id = it.id) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun loadInstantRequests(limit: Long = ON_DEMAND_FETCH_LIMIT): List<InstantRequestEntity> {
        return try {
            val docs = db.collection("instant_requests")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(InstantRequestEntity::class.java)?.copy(id = it.id) }
            val now = System.currentTimeMillis()
            docs.map { req ->
                if ((req.status == "WAITING_FOR_OFFERS" || req.status == "REVIEWING_OFFERS") && now > req.expiresAt) {
                    req.copy(status = "EXPIRED")
                } else req
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun loadPayments(limit: Long = ON_DEMAND_FETCH_LIMIT): List<PaymentEntity> {
        return try {
            db.collection("payments")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(PaymentEntity::class.java)?.copy(id = it.id) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun loadPaymentWallets(limit: Long = ON_DEMAND_FETCH_LIMIT): List<PaymentWalletEntity> {
        return try {
            db.collection("payment_wallets")
                .limit(limit)
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(PaymentWalletEntity::class.java)?.copy(id = it.id) }
                .sortedBy { it.displayOrder }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun loadCoupons(limit: Long = ON_DEMAND_FETCH_LIMIT): List<CouponEntity> {
        return try {
            db.collection("coupons")
                .limit(limit)
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(CouponEntity::class.java) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun loadCalls(limit: Long = ON_DEMAND_FETCH_LIMIT): List<CallEntity> {
        return try {
            db.collection("calls")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(CallEntity::class.java) }
                .sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun loadProducts(limit: Long = ON_DEMAND_FETCH_LIMIT): List<ProductEntity> {
        return try {
            db.collection("products")
                .limit(limit)
                .get()
                .await()
                .documents
                .mapNotNull { doc ->
                    try {
                        val obj = doc.toObject(ProductEntity::class.java)
                        if (obj != null) {
                            val isDel = doc.getBoolean("isDeleted") == true || doc.getBoolean("deleted") == true
                            obj.copy(id = doc.id, isDeleted = isDel)
                        } else null
                    } catch (e: Exception) {
                        null
                    }
                }.filter { !it.isDeleted }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun loadActivityLogs(limit: Long = ON_DEMAND_FETCH_LIMIT): List<ActivityLogEntity> {
        return try {
            db.collection("activity_logs")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(ActivityLogEntity::class.java)?.copy(id = it.id) }
                .sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
