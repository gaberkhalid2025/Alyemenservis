package com.example.ui.helpers

import com.example.data.*
import com.example.data.models.*
import com.example.ui.viewmodels.BookingDistributionMode
import com.example.ui.viewmodels.BookingFormFields
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppState @Inject constructor() {
    val _activityLogs = MutableStateFlow<List<ActivityLogEntity>>(emptyList())
    val _banners = MutableStateFlow<List<BannerEntity>>(emptyList())
    val _bookingFormFields = MutableStateFlow<BookingFormFields>(BookingFormFields())
    val _bookings = MutableStateFlow<List<BookingEntity>>(emptyList())
    val _callsLog = MutableStateFlow<List<CallEntity>>(emptyList())
    val _categories = MutableStateFlow<List<CategoryEntity>>(emptyList())
    val _chatChannels = MutableStateFlow<List<ChatChannelEntity>>(emptyList())
    val _cities = MutableStateFlow<List<CityEntity>>(emptyList())
    val _colorPalettes = MutableStateFlow<List<ColorPaletteEntity>>(emptyList())
    val _coupons = MutableStateFlow<List<CouponEntity>>(emptyList())
    val _customProfileTabs = MutableStateFlow<List<CustomProfileTabEntity>>(emptyList())
    val _deletedProviders = MutableStateFlow<List<ProviderEntity>>(emptyList())
    val _distributionMode = MutableStateFlow(BookingDistributionMode.ADMIN_ONLY)
    val _instantRequests = MutableStateFlow<List<InstantRequestEntity>>(emptyList())
    val _internalWallets = MutableStateFlow<List<InternalWalletEntity>>(emptyList())
    val _isChatChannelsLoading = MutableStateFlow(false)
    val _isInitialized = MutableStateFlow(false)
    val _isProvidersLoading = MutableStateFlow(false)
    val _jobApplications = MutableStateFlow<List<JobApplicationEntity>>(emptyList())
    val _jobs = MutableStateFlow<List<JobEntity>>(emptyList())
    val _maxKmRadius = MutableStateFlow(50)
    val _notifications = MutableStateFlow<List<NotificationEntity>>(emptyList())
    val _offers = MutableStateFlow<List<Offer>>(emptyList())
    val _orders = MutableStateFlow<List<OrderEntity>>(emptyList())
    val _payments = MutableStateFlow<List<PaymentEntity>>(emptyList())
    val _paymentWallets = MutableStateFlow<List<PaymentWalletEntity>>(emptyList())
    val _pendingProviders = MutableStateFlow<List<PendingProviderEntity>>(emptyList())
    val _products = MutableStateFlow<List<ProductEntity>>(emptyList())
    val _properties = MutableStateFlow<List<PropertyEntity>>(emptyList())
    val _providers = MutableStateFlow<List<ProviderEntity>>(emptyList())
    val _ratings = MutableStateFlow<List<RatingEntity>>(emptyList())
    val _registeredUsersCount = MutableStateFlow(0)
    val _registeredUsersList = MutableStateFlow<List<Map<String, Any>>>(emptyList())
    val _reports = MutableStateFlow<List<ReportEntity>>(emptyList())
    val _requestOffers = MutableStateFlow<List<RequestOfferEntity>>(emptyList())
    val _settings = MutableStateFlow(AdminSettingsEntity())
    val _stores = MutableStateFlow<List<StoreEntity>>(emptyList())
    val _supervisors = MutableStateFlow<List<SupervisorEntity>>(emptyList())
    val _walletTransactions = MutableStateFlow<List<WalletTransactionEntity>>(emptyList())
    val _pendingTechnicians = MutableStateFlow<List<PendingProviderEntity>>(emptyList())
}
