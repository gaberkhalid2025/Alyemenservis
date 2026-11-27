package com.example.ui.helpers

import com.example.data.*
import com.example.data.models.InstantRequestEntity
import com.example.data.models.Offer
import com.example.data.models.RequestOfferEntity
import com.google.firebase.firestore.*

/**
 * Helper class responsible for real-time Firestore listeners and synchronization.
 * Guarantees safe listener registration and teardown on ViewModel cleared.
 */
class RealtimeSyncHelper(private val db: FirebaseFirestore) {

    companion object {
        /** Real-time listeners limit (optimized to 80 to conserve Firestore Spark quota). */
        var REALTIME_QUERY_LIMIT: Long = 80L

        /** Default on-demand query limit (optimized to 80 to conserve Firestore Spark quota). */
        var ON_DEMAND_FETCH_LIMIT: Long = 80L
    }

    val firestoreListeners = java.util.concurrent.CopyOnWriteArrayList<ListenerRegistration>()

    internal fun registerQueryListener(
        query: Query,
        listener: EventListener<QuerySnapshot>
    ): ListenerRegistration? = try {
        val reg = query.addSnapshotListener(listener)
        firestoreListeners.add(reg)
        reg
    } catch (e: Exception) {
        e.printStackTrace()
        com.example.utils.AppErrorLogManager.logFirestoreError("RealtimeSyncHelper", "Failed to add Query listener", e)
        null
    }

    internal fun registerDocListener(
        docRef: DocumentReference,
        listener: EventListener<DocumentSnapshot>
    ): ListenerRegistration? = try {
        val reg = docRef.addSnapshotListener(listener)
        firestoreListeners.add(reg)
        reg
    } catch (e: Exception) {
        e.printStackTrace()
        com.example.utils.AppErrorLogManager.logFirestoreError("RealtimeSyncHelper", "Failed to add Document listener", e)
        null
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

    fun setupRealtimeFirestoreListeners(appState: AppState) {
        registerCoreRealtimeListeners(db, appState)
        registerCatalogAndOpsRealtimeListeners(db, appState)
    }

    suspend fun loadOnDemandInitialData(appState: AppState) =
        loadOnDemandInitialDataHelper(this, db, appState)

    suspend fun loadRegisteredUsers(limit: Long = ON_DEMAND_FETCH_LIMIT): List<Map<String, Any>> =
        loadRegisteredUsersHelper(db, limit)

    suspend fun loadInternalWallets(
        limit: Long = ON_DEMAND_FETCH_LIMIT,
        userId: String = "",
        customerPhone: String = ""
    ): List<InternalWalletEntity> = loadInternalWalletsHelper(db, limit, userId, customerPhone)

    suspend fun loadWalletTransactions(
        limit: Long = ON_DEMAND_FETCH_LIMIT,
        userId: String = "",
        customerPhone: String = ""
    ): List<WalletTransactionEntity> = loadWalletTransactionsHelper(db, limit, userId, customerPhone)

    suspend fun loadBanners(limit: Long = ON_DEMAND_FETCH_LIMIT): List<BannerEntity> =
        loadBannersHelper(db, limit)

    suspend fun loadPendingProviders(limit: Long = ON_DEMAND_FETCH_LIMIT): List<PendingProviderEntity> =
        loadPendingProvidersHelper(db, limit)

    suspend fun loadBookings(limit: Long = ON_DEMAND_FETCH_LIMIT): List<BookingEntity> =
        loadBookingsHelper(db, limit)

    suspend fun loadReports(limit: Long = ON_DEMAND_FETCH_LIMIT): List<ReportEntity> =
        loadReportsHelper(db, limit)

    suspend fun loadSupervisors(limit: Long = ON_DEMAND_FETCH_LIMIT): List<SupervisorEntity> =
        loadSupervisorsHelper(db, limit)

    suspend fun loadColorThemes(): List<ColorPaletteEntity> =
        loadColorThemesHelper(db)

    suspend fun loadCustomProfileTabs(): List<CustomProfileTabEntity> =
        loadCustomProfileTabsHelper(db)

    suspend fun loadJobs(limit: Long = ON_DEMAND_FETCH_LIMIT): List<JobEntity> =
        loadJobsHelper(db, limit)

    suspend fun loadJobListings(limit: Long = ON_DEMAND_FETCH_LIMIT): List<JobEntity> =
        loadJobListingsHelper(db, limit)

    suspend fun loadJobApplications(limit: Long = ON_DEMAND_FETCH_LIMIT): List<JobApplicationEntity> =
        loadJobApplicationsHelper(db, limit)

    suspend fun loadRatings(limit: Long = ON_DEMAND_FETCH_LIMIT): List<RatingEntity> =
        loadRatingsHelper(db, limit)

    suspend fun loadOrders(limit: Long = ON_DEMAND_FETCH_LIMIT): List<OrderEntity> =
        loadOrdersHelper(db, limit)

    suspend fun loadOffers(limit: Long = ON_DEMAND_FETCH_LIMIT): List<Offer> =
        loadOffersHelper(db, limit)

    suspend fun loadRequestOffers(limit: Long = ON_DEMAND_FETCH_LIMIT): List<RequestOfferEntity> =
        loadRequestOffersHelper(db, limit)

    suspend fun loadInstantRequests(limit: Long = ON_DEMAND_FETCH_LIMIT): List<InstantRequestEntity> =
        loadInstantRequestsHelper(db, limit)

    suspend fun loadPayments(
        limit: Long = ON_DEMAND_FETCH_LIMIT,
        userId: String = "",
        customerPhone: String = ""
    ): List<PaymentEntity> = loadPaymentsHelper(db, limit, userId, customerPhone)

    suspend fun loadPaymentWallets(limit: Long = ON_DEMAND_FETCH_LIMIT): List<PaymentWalletEntity> =
        loadPaymentWalletsHelper(db, limit)

    suspend fun loadCoupons(limit: Long = ON_DEMAND_FETCH_LIMIT): List<CouponEntity> =
        loadCouponsHelper(db, limit)

    suspend fun loadCalls(limit: Long = ON_DEMAND_FETCH_LIMIT): List<CallEntity> =
        loadCallsHelper(db, limit)

    suspend fun loadProducts(limit: Long = ON_DEMAND_FETCH_LIMIT): List<ProductEntity> =
        loadProductsHelper(db, limit)

    suspend fun loadActivityLogs(limit: Long = ON_DEMAND_FETCH_LIMIT): List<ActivityLogEntity> =
        loadActivityLogsHelper(db, limit)

    suspend fun searchProviders(query: String, limit: Long = 30): List<ProviderEntity> =
        searchProvidersHelper(db, query, limit)

    suspend fun searchStores(query: String, limit: Long = 30): List<StoreEntity> =
        searchStoresHelper(db, query, limit)

    suspend fun searchProperties(query: String, limit: Long = 30): List<PropertyEntity> =
        searchPropertiesHelper(db, query, limit)

    suspend fun searchJobs(query: String, limit: Long = 30): List<JobEntity> =
        searchJobsHelper(db, query, limit)
}
