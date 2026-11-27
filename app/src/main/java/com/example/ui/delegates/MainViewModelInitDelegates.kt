package com.example.ui

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal fun MainViewModel.checkInitializationCompleteHelper() {
    if (isSettingsLoaded && isCategoriesLoaded && isCitiesLoaded && isProvidersLoaded) {
        _isInitialized.value = true
    }
}

internal fun MainViewModel.refreshDataHelper() {
    viewModelScope.launch {
        _isRefreshing.value = true
        _uiErrorMessage.value = null
        try {
            setupRealtimeFirestoreListeners()
        } catch (e: Exception) {
            _uiErrorMessage.value = "تعذر تحديث البيانات: ${e.localizedMessage}"
        } finally {
            delay(600)
            _isRefreshing.value = false
        }
    }
}

internal fun MainViewModel.retryConnectionHelper(context: Context) {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    if (cm != null) {
        val activeNetwork = cm.activeNetwork
        val capabilities = cm.getNetworkCapabilities(activeNetwork)
        val online = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        _isOnline.value = online
        if (online) {
            setupRealtimeFirestoreListeners()
        }
    }
}

internal fun MainViewModel.initializeUserIdentityHelper(context: Context) {
    appContext = context.applicationContext
    authViewModel.appContext = appContext
    homeViewModel.appContext = appContext
    bookingViewModel.appContext = appContext
    adminViewModel.appContext = appContext
    settingsViewModel.appContext = appContext
    instantRequestViewModel.appContext = appContext
    bookingViewModel.getCoupons = { _coupons.value }
    bookingViewModel.getProviders = { _providers.value }
    bookingViewModel.getCurrentUserPhone = { _currentUserPhone.value }
    bookingViewModel.getCurrentUserName = { _currentUserName.value }
    bookingViewModel.getCurrentUserResidence = { _currentUserResidence.value }
    bookingViewModel.setCurrentUserPhone = { _currentUserPhone.value = it }
    bookingViewModel.setCurrentUserName = { _currentUserName.value = it }
    bookingViewModel.setCurrentUserResidence = { _currentUserResidence.value = it }
    bookingViewModel.onAddNotification = { title, message, targetType, targetValue ->
        addNotification(title = title, message = message, targetType = targetType, targetValue = targetValue)
    }
    bookingViewModel.triggerNotificationCallback = { msg ->
        triggerNotification(msg)
    }
    bookingViewModel.onOpenOrCreateChatChannel = { targetId, targetType, targetName, targetPhone, targetCategory, relatedEntityId, relatedEntityType, onComplete ->
        openOrCreateChatChannel(
            targetId = targetId,
            targetType = targetType,
            targetName = targetName,
            targetPhone = targetPhone,
            targetCategory = targetCategory,
            relatedEntityId = relatedEntityId,
            relatedEntityType = relatedEntityType,
            onCreated = onComplete
        )
    }
    adminViewModel.getHomeViewModel = { homeViewModel }
    adminViewModel.getSettingsViewModel = { settingsViewModel }
    adminViewModel.getBookingViewModel = { bookingViewModel }
    adminViewModel.getInstantRequestViewModel = { instantRequestViewModel }
    adminViewModel.getNotifications = { _notifications }
    adminViewModel.onAddNotification = { title, message, targetType, targetValue ->
        addNotification(title = title, message = message, targetType = targetType, targetValue = targetValue)
    }
    adminViewModel.onTriggerNotificationFull = { title, message, targetType, targetValue ->
        triggerNotification(title = title, message = message, targetType = targetType, targetValue = targetValue)
    }
    adminViewModel.onTriggerNotification = { msg ->
        triggerNotification(msg)
    }
    adminViewModel.onApplyFilters = {
        homeViewModel.applyFilters(_currentUserResidence.value)
    }
    storeManagementViewModel.onTriggerNotification = { msg -> triggerNotification(msg) }
    propertyManagementViewModel.onTriggerNotification = { msg -> triggerNotification(msg) }
    jobManagementViewModel.onTriggerNotification = { msg -> triggerNotification(msg) }
    paymentManagementViewModel.onTriggerNotification = { msg -> triggerNotification(msg) }
    settingsViewModel.getAuthViewModel = { authViewModel }
    settingsViewModel.getHomeViewModel = { homeViewModel }
    settingsViewModel.getBookingViewModel = { bookingViewModel }
    settingsViewModel.getAdminViewModel = { adminViewModel }
    settingsViewModel.getProviders = { _providers }
    settingsViewModel.getBookings = { _bookings }
    settingsViewModel.getCategories = { _categories }
    settingsViewModel.getStores = { _stores }
    settingsViewModel.getProperties = { _properties }
    settingsViewModel.getPasswordRecoveryWaitingPhone = { _passwordRecoveryWaitingPhone }
    settingsViewModel.setPasswordRecoveryWaitingPhone = { _passwordRecoveryWaitingPhone.value = it }
    settingsViewModel.verifyAdminOrOwnerPassword = { password ->
        verifyAdminOrOwnerPassword(password)
    }
    settingsViewModel.triggerNotification = { msg ->
        triggerNotification(msg)
    }
    instantRequestViewModel.triggerNotification = { msg ->
        triggerNotification(msg)
    }
    instantRequestViewModel.addNotification = { title, message, targetType, targetValue ->
        addNotification(title = title, message = message, targetType = targetType, targetValue = targetValue)
    }
    instantRequestViewModel.getOrCreateChatChannel = { providerId, providerName, customerPhone, customerName ->
        openOrCreateChatChannel(
            targetId = providerId,
            targetType = "URGENT_REQUEST",
            targetName = providerName,
            targetPhone = customerPhone,
            targetCategory = "",
            relatedEntityId = "",
            relatedEntityType = "URGENT_REQUEST",
            onCreated = { }
        )
    }
    try {
        authViewModel.initializeUserIdentity(context) { savedFavs ->
            _favoriteIds.value = savedFavs
        }
    } catch (e: Exception) {
        android.util.Log.e("MainViewModel", "❌ Error in AuthViewModel initialization", e)
    }
    try {
        if (authStateListener == null) {
            val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
                val newUid = firebaseAuth.currentUser?.uid
                if (newUid != null && newUid != lastSyncedAuthUid) {
                    lastSyncedAuthUid = newUid
                    setupRealtimeFirestoreListeners()
                    seedFirestoreIfEmpty()
                }
            }
            authStateListener = listener
            auth.addAuthStateListener(listener)
        }
        lastSyncedAuthUid = auth.currentUser?.uid
        setupRealtimeFirestoreListeners()
    } catch (e: Exception) {
        android.util.Log.e("MainViewModel", "❌ Error in setting up realtime listeners", e)
    }
    try {
        settingsViewModel.loadCardSettings()
    } catch (e: Exception) {
        android.util.Log.e("MainViewModel", "❌ Error in SettingsViewModel loadCardSettings", e)
    }
    try {
        adminViewModel.loadPendingTechnicians()
    } catch (e: Exception) {
        android.util.Log.e("MainViewModel", "❌ Error in AdminViewModel loadPendingTechnicians", e)
    }
    try {
        seedFirestoreIfEmpty()
    } catch (e: Exception) {
        android.util.Log.e("MainViewModel", "❌ Error in seedFirestoreIfEmpty", e)
    }
    viewModelScope.launch {
        settings.collect {
            isSettingsLoaded = true
            checkInitializationCompleteHelper()
        }
    }
    viewModelScope.launch {
        categories.collect { cats ->
            if (cats.isNotEmpty()) {
                isCategoriesLoaded = true
                checkInitializationCompleteHelper()
            }
        }
    }
    viewModelScope.launch {
        cities.collect { cityList ->
            if (cityList.isNotEmpty()) {
                isCitiesLoaded = true
                checkInitializationCompleteHelper()
            }
        }
    }
    viewModelScope.launch {
        providers.collect { provList ->
            if (provList.isNotEmpty()) {
                isProvidersLoaded = true
                checkInitializationCompleteHelper()
            }
        }
    }
    viewModelScope.launch {
        delay(2800)
        if (!_isInitialized.value) {
            _isInitialized.value = true
        }
    }
}
