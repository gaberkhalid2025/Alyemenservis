package com.example.ui.viewmodels

import android.app.Application
import android.content.Context
import com.example.data.*
import com.example.data.models.Offer
import com.example.ui.*
import com.example.ui.helpers.AppState
import com.example.utils.SecureStorage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * 🛡️ AdminViewModel (Facade)
 * المنسّق الرئيسي للوحة الإدارة وواجهة الربط الموحدة بين الشاشات والمفوّضين:
 * 1. [AdminAuthAndSystemDelegate] المصادقة والجلسات والإعدادات والإحصائيات والسجلات.
 * 2. [AdminRequestsAndUsersDelegate] طلبات الانضمام والفنيون والمستخدمون ومنع التكرار.
 * 3. [AdminProvidersAndCatalogDelegate] مقدمو الخدمات ومعرض الأعمال والتصنيفات والمدن والتبويبات.
 * 4. [AdminStoresPropertiesJobsDelegate] المتاجر والعقارات والوظائف والعمليات العابرة للكيانات.
 * 5. [AdminCommerceAndEngagementDelegate] البلاغات والكوبونات والمحافظ والمدفوعات والمنتجات والعروض والبنرات والطلبات والتقييمات.
 */
class AdminViewModel @Inject constructor(
    val application: Application,
    val appState: AppState,
    val secureStorage: SecureStorage
) : BaseViewModel() {

    val crud = AdminCrudOperations(db)

    internal val _isLoadingState = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoadingState.asStateFlow()

    internal val _adminRole = MutableStateFlow("GUEST")
    val adminRole: StateFlow<String> = _adminRole.asStateFlow()

    internal val _supervisorPermissions = MutableStateFlow<List<String>>(emptyList())
    val supervisorPermissions: StateFlow<List<String>> = _supervisorPermissions.asStateFlow()

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

        fun addNotification(
            title: String,
            message: String,
            targetType: String = "",
            targetValue: String = ""
        ) {
            this@AdminViewModel.addNotification(title, message, targetType, targetValue)
        }

        fun triggerNotification(
            title: String,
            message: String,
            targetType: String = "ALL",
            targetValue: String = ""
        ) {
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

    internal val firebaseAuth: FirebaseAuth
        get() = FirebaseAuth.getInstance()

    private val homeViewModel: HomeViewModel
        get() = getHomeViewModel?.invoke() ?: throw IllegalStateException("homeViewModel not provided")
    private val settingsViewModel: SettingsViewModel
        get() = getSettingsViewModel?.invoke() ?: throw IllegalStateException("settingsViewModel not provided")
    private val bookingViewModel: BookingViewModel
        get() = getBookingViewModel?.invoke() ?: throw IllegalStateException("bookingViewModel not provided")
    private val instantRequestViewModel: InstantRequestViewModel
        get() = getInstantRequestViewModel?.invoke() ?: throw IllegalStateException("instantRequestViewModel not provided")
    private val _notifications: MutableStateFlow<List<NotificationEntity>>
        get() = getNotifications?.invoke() ?: throw IllegalStateException("_notifications not provided")

    private fun addNotification(title: String, message: String, targetType: String = "", targetValue: String = "") {
        onAddNotification?.invoke(title, message, targetType, targetValue)
    }

    private fun triggerNotification(title: String, message: String, targetType: String = "", targetValue: String = "") {
        onTriggerNotificationFull?.invoke(title, message, targetType, targetValue)
    }

    private fun applyFilters() {
        onApplyFilters?.invoke()
    }

    internal suspend fun <T> runSafeFirestoreCall(
        operation: suspend () -> T,
        onSuccess: (T) -> Unit = {},
        onError: (Exception) -> Unit = {},
        errorMessage: String = "حدث خطأ أثناء تنفيذ العملية"
    ) {
        safeFirestoreCall(operation, onSuccess, onError, errorMessage)
    }

    internal suspend fun resolveUploadImage(
        context: Context,
        input: String,
        storagePath: String,
        maxSizeBytes: Long = 300 * 1024L
    ): String = uploadImageStringOrUri(context, input, storagePath, maxSizeBytes)

    internal fun resolveAuthEmailForPhone(phone: String): String = getAuthEmailForPhone(phone)

    // --- StateFlows bound to AppState ---
    internal val _pendingProviders get() = appState._pendingProviders
    val pendingProviders: StateFlow<List<PendingProviderEntity>> = _pendingProviders.asStateFlow()
    internal val _pendingTechnicians get() = appState._pendingTechnicians
    val pendingTechnicians: StateFlow<List<PendingProviderEntity>> = _pendingTechnicians.asStateFlow()
    internal val _registeredUsersList get() = appState._registeredUsersList
    val registeredUsersList: StateFlow<List<Map<String, Any>>> = _registeredUsersList.asStateFlow()
    internal val _registeredUsersCount get() = appState._registeredUsersCount
    val registeredUsersCount: StateFlow<Int> = _registeredUsersCount.asStateFlow()
    internal val _reports get() = appState._reports
    val reports: StateFlow<List<ReportEntity>> = _reports.asStateFlow()
    internal val _activityLogs get() = appState._activityLogs
    val activityLogs: StateFlow<List<ActivityLogEntity>> = _activityLogs.asStateFlow()
    internal val _callsLog get() = appState._callsLog
    val callsLog: StateFlow<List<CallEntity>> = _callsLog.asStateFlow()
    internal val _coupons get() = appState._coupons
    val coupons: StateFlow<List<CouponEntity>> = _coupons.asStateFlow()
    internal val _internalWallets get() = appState._internalWallets
    val internalWallets: StateFlow<List<InternalWalletEntity>> = _internalWallets.asStateFlow()
    internal val _walletTransactions get() = appState._walletTransactions
    val walletTransactions: StateFlow<List<WalletTransactionEntity>> = _walletTransactions.asStateFlow()
    internal val _paymentWallets get() = appState._paymentWallets
    val paymentWallets: StateFlow<List<PaymentWalletEntity>> = _paymentWallets.asStateFlow()
    internal val _payments get() = appState._payments
    val payments: StateFlow<List<PaymentEntity>> = _payments.asStateFlow()
    internal val _orders get() = appState._orders
    val orders: StateFlow<List<OrderEntity>> = _orders.asStateFlow()
    internal val _ratings get() = appState._ratings
    val ratings: StateFlow<List<RatingEntity>> = _ratings.asStateFlow()
    internal val _customProfileTabs get() = appState._customProfileTabs
    val customProfileTabs: StateFlow<List<CustomProfileTabEntity>> = _customProfileTabs.asStateFlow()
    internal val _stores get() = appState._stores
    val stores: StateFlow<List<StoreEntity>> = _stores.asStateFlow()
    internal val _products get() = appState._products
    val products: StateFlow<List<ProductEntity>> = _products.asStateFlow()
    internal val _properties get() = appState._properties
    val properties: StateFlow<List<PropertyEntity>> = _properties.asStateFlow()
    internal val _jobs get() = appState._jobs
    val jobs: StateFlow<List<JobEntity>> = _jobs.asStateFlow()
    internal val _jobApplications get() = appState._jobApplications
    val jobApplications: StateFlow<List<JobApplicationEntity>> = _jobApplications.asStateFlow()

    // --- Admin Audit, System & Recovery StateFlows ---
    internal val _auditLogsState = MutableStateFlow<List<AuditLog>>(emptyList())
    val auditLogs: StateFlow<List<AuditLog>> = _auditLogsState.asStateFlow()

    internal val _adminNotificationsListState = MutableStateFlow<List<AdminNotification>>(emptyList())
    val adminNotificationsList: StateFlow<List<AdminNotification>> = _adminNotificationsListState.asStateFlow()

    internal val _systemLogsState = MutableStateFlow<List<SystemLog>>(emptyList())
    val systemLogs: StateFlow<List<SystemLog>> = _systemLogsState.asStateFlow()

    internal val _pendingRequestsState = MutableStateFlow<List<PendingRequest>>(emptyList())
    val pendingRequests: StateFlow<List<PendingRequest>> = _pendingRequestsState.asStateFlow()

    internal val _systemStatsState = MutableStateFlow(SystemStats())
    val systemStats: StateFlow<SystemStats> = _systemStatsState.asStateFlow()

    internal val _passwordRecoveryRequestsState = MutableStateFlow<List<Map<String, Any>>>(emptyList())
    val passwordRecoveryRequests: StateFlow<List<Map<String, Any>>> = _passwordRecoveryRequestsState.asStateFlow()

    // --- Cohesive Delegates ---
    private val authDelegate = AdminAuthAndSystemDelegate(this)
    private val requestsDelegate = AdminRequestsAndUsersDelegate(this)
    private val providersDelegate = AdminProvidersAndCatalogDelegate(this)
    private val storesPropertiesJobsDelegate = AdminStoresPropertiesJobsDelegate(this)
    private val commerceDelegate = AdminCommerceAndEngagementDelegate(this)

    init {
        authDelegate.initSessionAndRecoveryListener()
    }

    override fun onCleared() {
        super.onCleared()
        authDelegate.cleanup()
    }

    // ==========================================
    // 🔐 المصادقة والجلسات والإعدادات والإحصائيات
    // ==========================================

    fun loginAdminSecure(
        email: String,
        password: String,
        rememberMe: Boolean = true,
        onResult: (success: Boolean, errorMessage: String?) -> Unit
    ) = authDelegate.loginAdminSecure(email, password, rememberMe, onResult)

    fun loginOwnerSecure(
        email: String,
        password: String,
        rememberMe: Boolean = true,
        onResult: (success: Boolean, errorMessage: String?) -> Unit
    ) = authDelegate.loginOwnerSecure(email, password, rememberMe, onResult)

    fun loginSupervisorSecure(
        usernameOrEmail: String,
        passcode: String,
        rememberMe: Boolean = true,
        onResult: (success: Boolean, errorMessage: String?) -> Unit
    ) = authDelegate.loginSupervisorSecure(usernameOrEmail, passcode, rememberMe, onResult)

    fun logoutAdmin() = authDelegate.logoutAdmin()

    suspend fun isAdminSessionValid(): Boolean = authDelegate.isAdminSessionValid()

    fun saveSettingsAtomically(
        appName: String,
        welcomeMessage: String,
        footerMessage: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) = authDelegate.saveSettingsAtomically(appName, welcomeMessage, footerMessage, onSuccess, onError)

    fun createSettingsBackup(onResult: (Boolean) -> Unit = {}) =
        authDelegate.createSettingsBackup(onResult)

    fun logAdminActivity(adminName: String, action: String, details: String) =
        authDelegate.logAdminActivity("Admin: $adminName - Action: $action - Details: $details")

    fun logAdminActivity(action: String) = authDelegate.logAdminActivity(action)

    fun logCall(providerId: String, providerName: String) =
        authDelegate.logCall(providerId, providerName)

    fun loadSystemStats() = authDelegate.loadSystemStats()

    fun getSystemStats(): SystemStats = authDelegate.getSystemStats()

    fun recordAuditLog(action: String, details: String) =
        authDelegate.recordAuditLog(action, details)

    fun exportReport(type: String): String = authDelegate.exportReport(type)

    fun syncAllData(context: Context, onComplete: ((Boolean) -> Unit)? = null) =
        authDelegate.syncAllData(context, onComplete)

    fun clearCache(context: Context) = authDelegate.clearCache(context)

    fun getSystemHealth(): SystemHealth = authDelegate.getSystemHealth()

    fun getStorageUsage(): StorageUsage = authDelegate.getStorageUsage()

    fun getBookingStats(period: String = "ALL"): BookingStats = authDelegate.getBookingStats(period)

    fun getRevenueStats(period: String = "ALL"): RevenueStats = authDelegate.getRevenueStats(period)

    fun getProviderStats(): ProviderStats = authDelegate.getProviderStats()

    fun getCategoryStats(): CategoryStats = authDelegate.getCategoryStats()

    fun getCityStats(): CityStats = authDelegate.getCityStats()

    fun sendAdminNotification(title: String, message: String, target: String) =
        authDelegate.sendAdminNotification(title, message, target)

    fun getAuditLogs(filter: String = "ALL"): List<AuditLog> = authDelegate.getAuditLogs(filter)

    fun exportAuditLogs(): String = authDelegate.exportAuditLogs()

    fun getSystemLogs(): List<SystemLog> = authDelegate.getSystemLogs()

    fun clearSystemLogs() = authDelegate.clearSystemLogs()

    // ==========================================
    // 📋 طلبات الانضمام والمستخدمون ومنع التكرار
    // ==========================================

    fun approveRequest(request: PendingProviderEntity) = requestsDelegate.approveRequest(request)

    fun rejectRequest(request: PendingProviderEntity, reason: String) =
        requestsDelegate.rejectRequest(request, reason)

    fun approveTechnician(providerId: String) = requestsDelegate.approveTechnician(providerId)

    fun rejectTechnician(providerId: String, reason: String = "لم يستوفِ الشروط") =
        requestsDelegate.rejectTechnician(providerId, reason)

    fun loadPendingTechnicians() = requestsDelegate.loadPendingTechnicians()

    fun approvePendingProvider(pending: PendingProviderEntity) =
        requestsDelegate.approvePendingProvider(pending)

    fun rejectPendingProvider(pending: PendingProviderEntity, reason: String = "تم رفض الطلب") =
        requestsDelegate.rejectPendingProvider(pending, reason)

    fun approveRegisteredUser(userId: String, userName: String = "") =
        requestsDelegate.approveRegisteredUser(userId, userName)

    fun toggleBlockRegisteredUser(userId: String, currentBlocked: Boolean, userName: String = "") =
        requestsDelegate.toggleBlockRegisteredUser(userId, currentBlocked, userName)

    fun deleteRegisteredUser(userId: String, userName: String = "") =
        requestsDelegate.deleteRegisteredUser(userId, userName)

    fun checkAndGetDuplicateAccountType(phone: String, excludeId: String): String? =
        requestsDelegate.checkAndGetDuplicateAccountType(phone, excludeId)

    fun loadPendingRequests() = requestsDelegate.loadPendingRequests()

    fun approveProviderRequest(requestId: String, onResult: ((Boolean) -> Unit)? = null) =
        requestsDelegate.approveProviderRequest(requestId, onResult)

    fun rejectProviderRequest(
        requestId: String,
        reason: String = "",
        onResult: ((Boolean) -> Unit)? = null
    ) = requestsDelegate.rejectProviderRequest(requestId, reason, onResult)

    fun blockUser(userId: String, onResult: ((Boolean) -> Unit)? = null) =
        requestsDelegate.blockUser(userId, onResult)

    fun unblockUser(userId: String, onResult: ((Boolean) -> Unit)? = null) =
        requestsDelegate.unblockUser(userId, onResult)

    fun deleteUser(userId: String, onResult: ((Boolean) -> Unit)? = null) =
        requestsDelegate.deleteUser(userId, onResult)

    // ==========================================
    // 🏪 المتاجر والمطاعم والمراكز الطبية
    // ==========================================

    fun saveStore(store: StoreEntity) = storesPropertiesJobsDelegate.saveStore(store)

    fun deleteStore(storeId: String) = storesPropertiesJobsDelegate.deleteStore(storeId)

    fun restoreStore(storeId: String) = storesPropertiesJobsDelegate.restoreStore(storeId)

    fun deleteStorePermanently(storeId: String) =
        storesPropertiesJobsDelegate.deleteStorePermanently(storeId)

    fun setStoreActive(storeId: String, isActive: Boolean) =
        storesPropertiesJobsDelegate.setStoreActive(storeId, isActive)

    fun setStorePinned(storeId: String, isPinned: Boolean) =
        storesPropertiesJobsDelegate.setStorePinned(storeId, isPinned)

    fun setStoreVip(storeId: String, isVip: Boolean) =
        storesPropertiesJobsDelegate.setStoreVip(storeId, isVip)

    fun setStoreVerified(storeId: String, isVerified: Boolean) =
        storesPropertiesJobsDelegate.setStoreVerified(storeId, isVerified)

    fun setStoreRecommended(storeId: String, isRecommended: Boolean) =
        storesPropertiesJobsDelegate.setStoreRecommended(storeId, isRecommended)

    fun setStoreBlocked(storeId: String, isBlocked: Boolean, reason: String = "") =
        storesPropertiesJobsDelegate.setStoreBlocked(storeId, isBlocked, reason)

    fun setStoreChatDisabled(storeId: String, isDisabled: Boolean) =
        storesPropertiesJobsDelegate.setStoreChatDisabled(storeId, isDisabled)

    fun setStoreNotificationsDisabled(storeId: String, isDisabled: Boolean) =
        storesPropertiesJobsDelegate.setStoreNotificationsDisabled(storeId, isDisabled)

    fun setStorePaymentEnabled(storeId: String, isEnabled: Boolean) =
        storesPropertiesJobsDelegate.setStorePaymentEnabled(storeId, isEnabled)

    fun toggleStoreBlocked(storeId: String, isBlocked: Boolean) =
        storesPropertiesJobsDelegate.toggleStoreBlocked(storeId, isBlocked)

    fun toggleStoreActive(storeId: String) = storesPropertiesJobsDelegate.toggleStoreActive(storeId)

    fun toggleStorePinned(storeId: String) = storesPropertiesJobsDelegate.toggleStorePinned(storeId)

    fun toggleStoreChatDisabled(storeId: String) =
        storesPropertiesJobsDelegate.toggleStoreChatDisabled(storeId)

    fun approveStorePdf(storeId: String, approve: Boolean) =
        storesPropertiesJobsDelegate.approveStorePdf(storeId, approve)

    // ==========================================
    // 🏡 العقارات والأراضي
    // ==========================================

    fun saveProperty(property: PropertyEntity) = storesPropertiesJobsDelegate.saveProperty(property)

    fun deleteProperty(propertyId: String) = storesPropertiesJobsDelegate.deleteProperty(propertyId)

    fun restoreProperty(propertyId: String) = storesPropertiesJobsDelegate.restoreProperty(propertyId)

    fun deletePropertyPermanently(propertyId: String) =
        storesPropertiesJobsDelegate.deletePropertyPermanently(propertyId)

    fun setPropertyActive(propertyId: String, isActive: Boolean) =
        storesPropertiesJobsDelegate.setPropertyActive(propertyId, isActive)

    fun setPropertyPinned(propertyId: String, isPinned: Boolean) =
        storesPropertiesJobsDelegate.setPropertyPinned(propertyId, isPinned)

    fun setPropertyVip(propertyId: String, isVip: Boolean) =
        storesPropertiesJobsDelegate.setPropertyVip(propertyId, isVip)

    fun setPropertyVerified(propertyId: String, isVerified: Boolean) =
        storesPropertiesJobsDelegate.setPropertyVerified(propertyId, isVerified)

    fun setPropertyRecommended(propertyId: String, isRecommended: Boolean) =
        storesPropertiesJobsDelegate.setPropertyRecommended(propertyId, isRecommended)

    fun setPropertyBlocked(propertyId: String, isBlocked: Boolean, reason: String = "") =
        storesPropertiesJobsDelegate.setPropertyBlocked(propertyId, isBlocked, reason)

    fun setPropertyChatDisabled(propertyId: String, isDisabled: Boolean) =
        storesPropertiesJobsDelegate.setPropertyChatDisabled(propertyId, isDisabled)

    fun setPropertyNotificationsDisabled(propertyId: String, isDisabled: Boolean) =
        storesPropertiesJobsDelegate.setPropertyNotificationsDisabled(propertyId, isDisabled)

    fun setPropertyPaymentEnabled(propertyId: String, isEnabled: Boolean) =
        storesPropertiesJobsDelegate.setPropertyPaymentEnabled(propertyId, isEnabled)

    fun togglePropertyBlocked(propertyId: String, isBlocked: Boolean) =
        storesPropertiesJobsDelegate.togglePropertyBlocked(propertyId, isBlocked)

    fun approvePropertyPdf(propertyId: String, approve: Boolean) =
        storesPropertiesJobsDelegate.approvePropertyPdf(propertyId, approve)

    // ==========================================
    // 💼 الوظائف وطلبات التوظيف
    // ==========================================

    fun saveJob(job: JobEntity) = storesPropertiesJobsDelegate.saveJob(job)

    fun deleteJob(jobId: String) = storesPropertiesJobsDelegate.deleteJob(jobId)

    fun restoreJob(jobId: String) = storesPropertiesJobsDelegate.restoreJob(jobId)

    fun deleteJobPermanently(jobId: String) =
        storesPropertiesJobsDelegate.deleteJobPermanently(jobId)

    fun setJobApproved(jobId: String, isApproved: Boolean) =
        storesPropertiesJobsDelegate.setJobApproved(jobId, isApproved)

    fun setJobBlocked(jobId: String, isBlocked: Boolean, reason: String = "") =
        storesPropertiesJobsDelegate.setJobBlocked(jobId, isBlocked, reason)

    fun setJobPinned(jobId: String, isPinned: Boolean) =
        storesPropertiesJobsDelegate.setJobPinned(jobId, isPinned)

    fun setJobVip(jobId: String, isVip: Boolean) = storesPropertiesJobsDelegate.setJobVip(jobId, isVip)

    fun setJobChatDisabled(jobId: String, isDisabled: Boolean) =
        storesPropertiesJobsDelegate.setJobChatDisabled(jobId, isDisabled)

    fun submitJobApplication(application: JobApplicationEntity) =
        storesPropertiesJobsDelegate.submitJobApplication(application)

    fun updateJobApplicationStatus(appId: String, status: String, reason: String = "") =
        storesPropertiesJobsDelegate.updateJobApplicationStatus(appId, status, reason)

    fun acceptJobApplication(appId: String) = storesPropertiesJobsDelegate.acceptJobApplication(appId)

    fun rejectJobApplication(appId: String, reason: String) =
        storesPropertiesJobsDelegate.rejectJobApplication(appId, reason)

    fun deleteJobApplication(appId: String) =
        storesPropertiesJobsDelegate.deleteJobApplication(appId)

    fun sendNotificationToApplicants(title: String, message: String, jobId: String = "") =
        storesPropertiesJobsDelegate.sendNotificationToApplicants(title, message, jobId)

    fun exportJobApplicantsCsv(context: Context) =
        storesPropertiesJobsDelegate.exportJobApplicantsCsv(context)

    fun redirectBookingToEntity(
        bookingId: String,
        targetEntityId: String,
        targetEntityName: String,
        targetPhone: String
    ) = storesPropertiesJobsDelegate.redirectBookingToEntity(
        bookingId,
        targetEntityId,
        targetEntityName,
        targetPhone
    )

    fun unbanEntity(entityType: String, entityId: String) =
        storesPropertiesJobsDelegate.unbanEntity(entityType, entityId)

    fun restoreEntity(entityType: String, entityId: String) =
        storesPropertiesJobsDelegate.restoreEntity(entityType, entityId)

    fun hardDeleteEntity(entityType: String, entityId: String, reason: String = "") =
        storesPropertiesJobsDelegate.hardDeleteEntity(entityType, entityId, reason)

    // ==========================================
    // 📢 البلاغات والكوبونات والمحافظ والمدفوعات
    // ==========================================

    fun submitReport(report: ReportEntity, onComplete: () -> Unit = {}) =
        commerceDelegate.submitReport(report, onComplete)

    fun deleteReport(reportId: String) = commerceDelegate.deleteReport(reportId)

    fun sendReport(providerId: String, providerName: String, reporterName: String, content: String) =
        commerceDelegate.sendReport(providerId, providerName, reporterName, content)

    fun addCoupon(
        code: String,
        pointsValue: Int,
        expiryMs: Long,
        discountPercentage: Int = 0,
        maxUsageCount: Int = 100
    ) = commerceDelegate.addCoupon(code, pointsValue, expiryMs, discountPercentage, maxUsageCount)

    fun saveCoupon(coupon: CouponEntity) = commerceDelegate.saveCoupon(coupon)

    fun deleteCoupon(couponId: String) = commerceDelegate.deleteCoupon(couponId)

    fun saveInternalWallet(wallet: InternalWalletEntity) = commerceDelegate.saveInternalWallet(wallet)

    fun performWalletTransaction(
        walletId: String,
        ownerName: String,
        ownerPhone: String,
        ownerType: String,
        type: String,
        amount: Double,
        note: String
    ) = commerceDelegate.performWalletTransaction(
        walletId,
        ownerName,
        ownerPhone,
        ownerType,
        type,
        amount,
        note
    )

    fun addPaymentWallet(wallet: PaymentWalletEntity) = commerceDelegate.addPaymentWallet(wallet)

    fun updatePaymentWallet(wallet: PaymentWalletEntity) =
        commerceDelegate.updatePaymentWallet(wallet)

    fun deletePaymentWallet(walletId: String) = commerceDelegate.deletePaymentWallet(walletId)

    fun togglePaymentWalletVisibility(walletId: String, currentVisible: Boolean) =
        commerceDelegate.togglePaymentWalletVisibility(walletId, currentVisible)

    fun createPayment(
        userId: String,
        providerId: String,
        amount: Double,
        method: String,
        bookingId: String = "",
        isLinkedToBooking: Boolean = false,
        bookingServiceType: String = "",
        walletProvider: String = "",
        walletNumber: String = "",
        walletAccountName: String = "",
        transferId: String = "",
        transferPhoto: String = "",
        status: String = "PENDING"
    ) = commerceDelegate.createPayment(
        userId,
        providerId,
        amount,
        method,
        bookingId,
        isLinkedToBooking,
        bookingServiceType,
        walletProvider,
        walletNumber,
        walletAccountName,
        transferId,
        transferPhoto,
        status
    )

    fun confirmPayment(
        paymentId: String,
        transferId: String,
        transferPhoto: String,
        walletProvider: String,
        walletNumber: String,
        walletAccountName: String
    ) = commerceDelegate.confirmPayment(
        paymentId,
        transferId,
        transferPhoto,
        walletProvider,
        walletNumber,
        walletAccountName
    )

    fun verifyPayment(paymentId: String, isVerified: Boolean, note: String, adminName: String) =
        commerceDelegate.verifyPayment(paymentId, isVerified, note, adminName)

    fun refundPayment(paymentId: String, reason: String) =
        commerceDelegate.refundPayment(paymentId, reason)

    // ==========================================
    // 📦 المنتجات والعروض والبنرات والطلبات والتقييمات
    // ==========================================

    fun saveProduct(product: ProductEntity) = commerceDelegate.saveProduct(product)

    fun deleteProduct(productId: String) = commerceDelegate.deleteProduct(productId)

    fun updateProductPrice(productId: String, newPrice: Double) =
        commerceDelegate.updateProductPrice(productId, newPrice)

    fun saveOffer(offer: Offer) = commerceDelegate.saveOffer(offer)

    fun deleteOffer(offerId: String) = commerceDelegate.deleteOffer(offerId)

    fun toggleOfferStatus(offerId: String, isActive: Boolean) =
        commerceDelegate.toggleOfferStatus(offerId, isActive)

    fun listenToOffersForEntity(entityId: String, onResult: (List<Offer>) -> Unit): ListenerRegistration =
        commerceDelegate.listenToOffersForEntity(entityId, onResult)

    fun listenToProductsForStore(storeId: String, onResult: (List<ProductEntity>) -> Unit): ListenerRegistration =
        commerceDelegate.listenToProductsForStore(storeId, onResult)

    fun addNewBanner(
        title: String,
        url: String,
        redirect: String,
        type: String,
        size: String,
        duration: Int,
        displayTime: String = "طوال اليوم"
    ) = commerceDelegate.addNewBanner(title, url, redirect, type, size, duration, displayTime)

    fun addBanner(
        title: String,
        url: String,
        redirect: String,
        type: String,
        size: String,
        duration: Int,
        displayTime: String = "طوال اليوم"
    ) = commerceDelegate.addBanner(title, url, redirect, type, size, duration, displayTime)

    fun deleteBanner(bannerId: String) = commerceDelegate.deleteBanner(bannerId)

    fun reorderBanners(newOrderedList: List<BannerEntity>) =
        commerceDelegate.reorderBanners(newOrderedList)

    fun placeOrder(order: OrderEntity) = commerceDelegate.placeOrder(order)

    fun updateOrderStatus(orderId: String, status: String) =
        commerceDelegate.updateOrderStatus(orderId, status)

    fun deleteOrder(orderId: String) = commerceDelegate.deleteOrder(orderId)

    fun deleteAllOrders(customerPhone: String) = commerceDelegate.deleteAllOrders(customerPhone)

    fun addRating(rating: RatingEntity) = commerceDelegate.addRating(rating)

    fun addRatingReply(ratingId: String, replyText: String) =
        commerceDelegate.addRatingReply(ratingId, replyText)

    fun deleteRating(ratingId: String) = commerceDelegate.deleteRating(ratingId)

    fun approveRating(ratingId: String, isApproved: Boolean) =
        commerceDelegate.approveRating(ratingId, isApproved)

    fun submitRating(providerId: String, rating: Int) =
        commerceDelegate.submitRating(providerId, rating)

    fun submitRating(ratingEntity: RatingEntity, onComplete: () -> Unit = {}) =
        commerceDelegate.submitRating(ratingEntity, onComplete)

    fun recalculateTargetRating(targetId: String, targetType: String) =
        commerceDelegate.recalculateTargetRating(targetId, targetType)

    // ==========================================
    // 🗂️ التصنيفات والمدن والتبويبات والفنيون
    // ==========================================

    fun saveCustomProfileTab(tab: CustomProfileTabEntity) =
        providersDelegate.saveCustomProfileTab(tab)

    fun deleteCustomProfileTab(tabId: String) =
        providersDelegate.deleteCustomProfileTab(tabId)

    fun toggleCustomProfileTab(tabId: String) =
        providersDelegate.toggleCustomProfileTab(tabId)

    fun addNewCategory(
        nameAr: String,
        nameEn: String,
        icon: String,
        description: String,
        parentId: String = "",
        isMainCategory: Boolean = true
    ) = providersDelegate.addNewCategory(nameAr, nameEn, icon, description, parentId, isMainCategory)

    fun editCategory(
        categoryId: String,
        newName: String,
        newIcon: String,
        parentId: String = "",
        isMainCategory: Boolean = true
    ) = providersDelegate.editCategory(categoryId, newName, newIcon, parentId, isMainCategory)

    fun deleteCategory(categoryId: String) = providersDelegate.deleteCategory(categoryId)

    fun togglePinCategory(categoryId: String) = providersDelegate.togglePinCategory(categoryId)

    fun mergeCategories(sourceCategoryId: String, targetCategoryId: String) =
        providersDelegate.mergeCategories(sourceCategoryId, targetCategoryId)

    fun saveCategoryEntity(cat: CategoryEntity) = providersDelegate.saveCategoryEntity(cat)

    fun addSubCategory(parentId: String, nameAr: String, icon: String) =
        providersDelegate.addSubCategory(parentId, nameAr, icon)

    fun convertCategoryType(catId: String, newParentId: String, isMain: Boolean) =
        providersDelegate.convertCategoryType(catId, newParentId, isMain)

    fun reorderCategories(newOrderedList: List<CategoryEntity>) =
        providersDelegate.reorderCategories(newOrderedList)

    fun addNewCity(
        nameAr: String,
        nameEn: String,
        icon: String = "📍",
        photoUrl: String = "",
        sortOrder: Int = 0
    ) = providersDelegate.addNewCity(nameAr, nameEn, icon, photoUrl, sortOrder)

    fun updateCity(city: CityEntity) = providersDelegate.updateCity(city)

    fun removeCity(cityId: String) = providersDelegate.removeCity(cityId)

    fun removeProvider(providerId: String) = providersDelegate.removeProvider(providerId)

    fun removeProviderPermanently(providerId: String) =
        providersDelegate.removeProviderPermanently(providerId)

    fun restoreProvider(providerId: String) = providersDelegate.restoreProvider(providerId)

    fun pinProvider(providerId: String, isPinned: Boolean) =
        providersDelegate.pinProvider(providerId, isPinned)

    fun recommendProvider(providerId: String, isRecommended: Boolean) =
        providersDelegate.recommendProvider(providerId, isRecommended)

    fun verifyProviderBadge(providerId: String, isVerified: Boolean) =
        providersDelegate.verifyProviderBadge(providerId, isVerified)

    fun toggleProviderSubscription(providerId: String, status: String) =
        providersDelegate.toggleProviderSubscription(providerId, status)

    fun toggleProviderSubscription(providerId: String) =
        providersDelegate.toggleProviderSubscription(providerId)

    fun setProviderChatDisabled(providerId: String, disabled: Boolean) =
        providersDelegate.setProviderChatDisabled(providerId, disabled)

    fun setProviderNotificationsDisabled(providerId: String, disabled: Boolean) =
        providersDelegate.setProviderNotificationsDisabled(providerId, disabled)

    fun setProviderPaymentRequired(providerId: String, required: Boolean) =
        providersDelegate.setProviderPaymentRequired(providerId, required)

    fun extendProviderSubscription(providerId: String, extraMs: Long) =
        providersDelegate.extendProviderSubscription(providerId, extraMs)

    fun toggleProviderBlock(providerId: String) = providersDelegate.toggleProviderBlock(providerId)

    fun toggleProviderStatus(provider: ProviderEntity) =
        providersDelegate.toggleProviderStatus(provider)

    fun toggleProviderPin(providerId: String) = providersDelegate.toggleProviderPin(providerId)

    fun toggleProviderVerification(providerId: String) =
        providersDelegate.toggleProviderVerification(providerId)

    fun toggleProviderRecommendation(providerId: String) =
        providersDelegate.toggleProviderRecommendation(providerId)

    fun updateProviderEntity(provider: ProviderEntity) =
        providersDelegate.updateProviderEntity(provider)

    fun editProviderPhoneAndCategory(providerId: String, newPhone: String, newCategoryId: String) =
        providersDelegate.editProviderPhoneAndCategory(providerId, newPhone, newCategoryId)

    fun addNewProvider(
        name: String,
        phone: String,
        catId: String,
        area: String,
        price: Double,
        isVip: Boolean
    ) = providersDelegate.addNewProvider(name, phone, catId, area, price, isVip)

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
    ) = providersDelegate.addNewProviderCustom(
        name,
        phone,
        catId,
        street,
        cityId,
        profileImage,
        idCardImage,
        forensicImage,
        price,
        isVip
    )

    fun updateProviderPortfolio(providerId: String, images: List<String>) =
        providersDelegate.updateProviderPortfolio(providerId, images)

    fun addPortfolioImage(providerId: String, imageBase64: String) =
        providersDelegate.addPortfolioImage(providerId, imageBase64)

    fun removePortfolioImage(providerId: String, index: Int) =
        providersDelegate.removePortfolioImage(providerId, index)

    fun clearPortfolio(providerId: String) = providersDelegate.clearPortfolio(providerId)
}