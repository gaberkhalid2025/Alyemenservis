package com.example.ui.helpers

import com.example.data.*
import com.example.data.models.InstantRequestEntity
import com.example.data.models.Offer
import com.example.data.models.RequestOfferEntity
import com.example.ui.viewmodels.BookingDistributionMode
import com.example.ui.viewmodels.BookingFormFields
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

internal suspend fun loadOnDemandInitialDataHelper(
    helper: RealtimeSyncHelper,
    db: FirebaseFirestore,
    appState: AppState
) {
    try {
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

        val limit = RealtimeSyncHelper.ON_DEMAND_FETCH_LIMIT
        appState._banners.value = helper.loadBanners(limit)
        appState._customProfileTabs.value = helper.loadCustomProfileTabs()
        appState._colorPalettes.value = helper.loadColorThemes()
        appState._products.value = helper.loadProducts(limit)
        appState._jobListings.value = helper.loadJobListings(limit)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

internal suspend fun loadRegisteredUsersHelper(db: FirebaseFirestore, limit: Long): List<Map<String, Any>> =
    try {
        db.collection("registered_users").limit(limit).get().await().documents.mapNotNull { doc ->
            val data = doc.data?.toMutableMap() ?: mutableMapOf()
            data["id"] = doc.id
            data
        }
    } catch (_: Exception) { emptyList() }

internal suspend fun loadInternalWalletsHelper(
    db: FirebaseFirestore,
    limit: Long,
    userId: String,
    customerPhone: String
): List<InternalWalletEntity> = try {
    val cleanPhone = AppPreferenceHelper.normalizePhoneNumber(customerPhone)
    val all = db.collection("internal_wallets").limit(limit).get().await().documents
        .mapNotNull { it.toObject(InternalWalletEntity::class.java) }
    if (userId.isBlank() && cleanPhone.isBlank()) all
    else all.filter { w ->
        (userId.isNotBlank() && w.id == userId) ||
            (cleanPhone.isNotBlank() && (AppPreferenceHelper.normalizePhoneNumber(w.ownerPhone) == cleanPhone || AppPreferenceHelper.normalizePhoneNumber(w.id) == cleanPhone))
    }
} catch (_: Exception) { emptyList() }

internal suspend fun loadWalletTransactionsHelper(
    db: FirebaseFirestore,
    limit: Long,
    userId: String,
    customerPhone: String
): List<WalletTransactionEntity> = try {
    val cleanPhone = AppPreferenceHelper.normalizePhoneNumber(customerPhone)
    val all = db.collection("wallet_transactions").orderBy("timestamp", Query.Direction.DESCENDING)
        .limit(limit).get().await().documents.mapNotNull { it.toObject(WalletTransactionEntity::class.java) }
    if (userId.isBlank() && cleanPhone.isBlank()) all
    else all.filter { tx ->
        (userId.isNotBlank() && tx.walletId == userId) ||
            (cleanPhone.isNotBlank() && AppPreferenceHelper.normalizePhoneNumber(tx.walletId) == cleanPhone)
    }
} catch (_: Exception) { emptyList() }

internal suspend fun loadBannersHelper(db: FirebaseFirestore, limit: Long): List<BannerEntity> = try {
    db.collection("banners").limit(limit).get().await().documents.mapNotNull { doc ->
        try { doc.toObject(BannerEntity::class.java) } catch (_: Exception) { null }
    }.sortedBy { it.order }
} catch (_: Exception) { emptyList() }

internal suspend fun loadPendingProvidersHelper(db: FirebaseFirestore, limit: Long): List<PendingProviderEntity> = try {
    db.collection("pending_providers").limit(limit).get().await().documents.mapNotNull { doc ->
        try { doc.toObject(PendingProviderEntity::class.java)?.copy(id = doc.id) } catch (_: Exception) { null }
    }
} catch (_: Exception) { emptyList() }

internal suspend fun loadBookingsHelper(db: FirebaseFirestore, limit: Long): List<BookingEntity> = try {
    db.collection("bookings").orderBy("createdAt", Query.Direction.DESCENDING).limit(limit)
        .get().await().documents.mapNotNull { it.toObject(BookingEntity::class.java) }
} catch (_: Exception) { emptyList() }

internal suspend fun loadReportsHelper(db: FirebaseFirestore, limit: Long): List<ReportEntity> = try {
    db.collection("reports").limit(limit).get().await().documents.mapNotNull { it.toObject(ReportEntity::class.java) }
} catch (_: Exception) { emptyList() }

internal suspend fun loadSupervisorsHelper(db: FirebaseFirestore, limit: Long): List<SupervisorEntity> = try {
    db.collection("supervisors").limit(limit).get().await().documents.mapNotNull { it.toObject(SupervisorEntity::class.java) }
} catch (_: Exception) { emptyList() }

internal suspend fun loadColorThemesHelper(db: FirebaseFirestore): List<ColorPaletteEntity> = try {
    db.collection("color_themes").get().await().documents.mapNotNull { it.toObject(ColorPaletteEntity::class.java) }
} catch (_: Exception) { emptyList() }

internal suspend fun loadCustomProfileTabsHelper(db: FirebaseFirestore): List<CustomProfileTabEntity> = try {
    db.collection("custom_profile_tabs").get().await().documents
        .mapNotNull { it.toObject(CustomProfileTabEntity::class.java) }.sortedBy { it.displayOrder }
} catch (_: Exception) { emptyList() }

internal suspend fun loadJobsHelper(db: FirebaseFirestore, limit: Long): List<JobEntity> = try {
    db.collection("jobs").limit(limit).get().await().documents.mapNotNull { doc ->
        try {
            val obj = doc.toObject(JobEntity::class.java)
            if (obj != null) {
                val isDel = doc.getBoolean("isDeleted") == true || doc.getBoolean("deleted") == true
                val act = doc.getBoolean("isActive") ?: doc.getBoolean("active") ?: true
                val pin = doc.getBoolean("isPinned") == true || doc.getBoolean("pinned") == true
                obj.copy(id = doc.id, isDeleted = isDel, isActive = act, isPinned = pin)
            } else null
        } catch (_: Exception) { null }
    }
} catch (_: Exception) { emptyList() }

internal suspend fun loadJobListingsHelper(db: FirebaseFirestore, limit: Long): List<JobEntity> = try {
    db.collection("job_listings").limit(limit).get().await().documents.mapNotNull { doc ->
        try {
            val obj = doc.toObject(JobEntity::class.java)
            if (obj != null) {
                val isDel = doc.getBoolean("isDeleted") == true || doc.getBoolean("deleted") == true
                val act = doc.getBoolean("isActive") ?: doc.getBoolean("active") ?: true
                val pin = doc.getBoolean("isPinned") == true || doc.getBoolean("pinned") == true
                obj.copy(id = doc.id, isDeleted = isDel, isActive = act, isPinned = pin)
            } else null
        } catch (_: Exception) { null }
    }
} catch (_: Exception) { emptyList() }

internal suspend fun loadJobApplicationsHelper(db: FirebaseFirestore, limit: Long): List<JobApplicationEntity> = try {
    db.collection("job_applications").limit(limit).get().await().documents
        .mapNotNull { it.toObject(JobApplicationEntity::class.java)?.copy(id = it.id) }
} catch (_: Exception) { emptyList() }

internal suspend fun loadRatingsHelper(db: FirebaseFirestore, limit: Long): List<RatingEntity> = try {
    db.collection("ratings").orderBy("timestamp", Query.Direction.DESCENDING).limit(limit)
        .get().await().documents.mapNotNull { it.toObject(RatingEntity::class.java)?.copy(id = it.id) }
} catch (_: Exception) { emptyList() }

internal suspend fun loadOrdersHelper(db: FirebaseFirestore, limit: Long): List<OrderEntity> = try {
    db.collection("orders").orderBy("timestamp", Query.Direction.DESCENDING).limit(limit)
        .get().await().documents.mapNotNull { it.toObject(OrderEntity::class.java)?.copy(id = it.id) }
} catch (_: Exception) { emptyList() }

internal suspend fun loadOffersHelper(db: FirebaseFirestore, limit: Long): List<Offer> = try {
    val primary = db.collection("offers").limit(limit).get().await().documents.mapNotNull { doc ->
        try { doc.toObject(Offer::class.java)?.copy(id = doc.id) } catch (_: Exception) { null }
    }
    if (primary.isNotEmpty()) primary
    else db.collection("special_offers").limit(limit).get().await().documents.mapNotNull { doc ->
        try { doc.toObject(Offer::class.java)?.copy(id = doc.id) } catch (_: Exception) { null }
    }
} catch (_: Exception) { emptyList() }

internal suspend fun loadRequestOffersHelper(db: FirebaseFirestore, limit: Long): List<RequestOfferEntity> = try {
    val topLevel = db.collection("request_offers").orderBy("createdAt", Query.Direction.DESCENDING)
        .limit(limit).get().await().documents.mapNotNull { it.toObject(RequestOfferEntity::class.java)?.copy(id = it.id) }
    if (topLevel.isNotEmpty()) topLevel
    else db.collectionGroup("offers").limit(limit).get().await().documents
        .mapNotNull { it.toObject(RequestOfferEntity::class.java)?.copy(id = it.id) }
        .sortedByDescending { it.createdAt }
} catch (_: Exception) { emptyList() }

internal suspend fun loadInstantRequestsHelper(db: FirebaseFirestore, limit: Long): List<InstantRequestEntity> = try {
    val docs = db.collection("instant_requests").orderBy("createdAt", Query.Direction.DESCENDING)
        .limit(limit).get().await().documents.mapNotNull { it.toObject(InstantRequestEntity::class.java)?.copy(id = it.id) }
    val now = System.currentTimeMillis()
    docs.map { req ->
        if ((req.status == "WAITING_FOR_OFFERS" || req.status == "REVIEWING_OFFERS") && now > req.expiresAt) {
            req.copy(status = "EXPIRED")
        } else req
    }
} catch (_: Exception) { emptyList() }

internal suspend fun loadPaymentsHelper(
    db: FirebaseFirestore,
    limit: Long,
    userId: String,
    customerPhone: String
): List<PaymentEntity> = try {
    val cleanPhone = AppPreferenceHelper.normalizePhoneNumber(customerPhone)
    val all = db.collection("payments").orderBy("createdAt", Query.Direction.DESCENDING)
        .limit(limit).get().await().documents.mapNotNull { it.toObject(PaymentEntity::class.java)?.copy(id = it.id) }
    if (userId.isBlank() && cleanPhone.isBlank()) all
    else all.filter { p ->
        (userId.isNotBlank() && (p.userId == userId || p.providerId == userId)) ||
            (cleanPhone.isNotBlank() && (AppPreferenceHelper.normalizePhoneNumber(p.userId) == cleanPhone || AppPreferenceHelper.normalizePhoneNumber(p.walletNumber) == cleanPhone))
    }
} catch (_: Exception) { emptyList() }

internal suspend fun loadPaymentWalletsHelper(db: FirebaseFirestore, limit: Long): List<PaymentWalletEntity> = try {
    db.collection("payment_wallets").limit(limit).get().await().documents
        .mapNotNull { it.toObject(PaymentWalletEntity::class.java)?.copy(id = it.id) }
        .sortedBy { it.displayOrder }
} catch (_: Exception) { emptyList() }

internal suspend fun loadCouponsHelper(db: FirebaseFirestore, limit: Long): List<CouponEntity> = try {
    db.collection("coupons").limit(limit).get().await().documents.mapNotNull { it.toObject(CouponEntity::class.java) }
} catch (_: Exception) { emptyList() }

internal suspend fun loadCallsHelper(db: FirebaseFirestore, limit: Long): List<CallEntity> = try {
    db.collection("calls").orderBy("timestamp", Query.Direction.DESCENDING).limit(limit)
        .get().await().documents.mapNotNull { it.toObject(CallEntity::class.java) }.sortedByDescending { it.timestamp }
} catch (_: Exception) { emptyList() }

internal suspend fun loadProductsHelper(db: FirebaseFirestore, limit: Long): List<ProductEntity> = try {
    db.collection("products").limit(limit).get().await().documents.mapNotNull { doc ->
        try {
            val obj = doc.toObject(ProductEntity::class.java)
            if (obj != null) {
                val isDel = doc.getBoolean("isDeleted") == true || doc.getBoolean("deleted") == true
                obj.copy(id = doc.id, isDeleted = isDel)
            } else null
        } catch (_: Exception) { null }
    }.filter { !it.isDeleted }
} catch (_: Exception) { emptyList() }

internal suspend fun loadActivityLogsHelper(db: FirebaseFirestore, limit: Long): List<ActivityLogEntity> = try {
    db.collection("activity_logs").orderBy("timestamp", Query.Direction.DESCENDING).limit(limit)
        .get().await().documents.mapNotNull { it.toObject(ActivityLogEntity::class.java)?.copy(id = it.id) }
        .sortedByDescending { it.timestamp }
} catch (_: Exception) { emptyList() }
