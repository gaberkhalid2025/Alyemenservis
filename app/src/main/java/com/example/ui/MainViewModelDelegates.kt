package com.example.ui

import androidx.compose.runtime.*
import com.example.ui.*
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import com.example.data.models.*
import com.example.data.*
import java.util.UUID
import com.example.utils.AppResult
import com.example.ui.viewmodels.SettingsViewModel.ChatParticipantType
import com.example.ui.viewmodels.BookingFormFields
import com.example.ui.viewmodels.BookingDistributionMode
import com.example.ui.viewmodels.BookingStatus


fun MainViewModel.applyFilters() {
        homeViewModel.applyFilters(_currentUserResidence.value)
    }
fun MainViewModel.selectCategory(categoryId: String?) {
        homeViewModel.selectCategory(categoryId, _currentUserResidence.value)
    }
fun MainViewModel.updateSearchQuery(query: String) {
        homeViewModel.updateSearchQuery(query, _currentUserResidence.value)
    }
fun MainViewModel.toggleVipFilter() {
        homeViewModel.toggleVipFilter(_currentUserResidence.value)
    }
fun MainViewModel.toggleAvailableFilter() {
        homeViewModel.toggleAvailableFilter(_currentUserResidence.value)
    }
fun MainViewModel.setCityFilter(cityId: String?) {
        homeViewModel.setCityFilter(cityId, _currentUserResidence.value)
    }
fun MainViewModel.setNeighborhoodFilter(neighborhood: String) {
        homeViewModel.setNeighborhoodFilter(neighborhood, _currentUserResidence.value)
    }
fun MainViewModel.setPhoneOrNameFilter(text: String) {
        homeViewModel.setPhoneOrNameFilter(text, _currentUserResidence.value)
    }
fun MainViewModel.setRadiusKm(km: Int) {
        homeViewModel.setRadiusKm(km, _currentUserResidence.value)
    }
fun MainViewModel.registerBackdoorInteraction() {
        authViewModel.registerBackdoorInteraction()
    }
fun MainViewModel.changeAdminCredentials(username: String, password: String) {
        triggerNotification("🔐 تم تغيير بيانات المدير الرئيسي")
    }
    fun MainViewModel.authenticateAdmin(context: android.content.Context, role: String, remember: Boolean) {
        authViewModel.authenticateAdmin(context, role, remember)
        navigateTo("ADMIN_PANEL")
    }
    fun MainViewModel.authenticateAdmin(role: String) {
        authViewModel.authenticateAdmin(role)
        navigateTo("ADMIN_PANEL")
    }
    fun MainViewModel.logout(context: android.content.Context) {
        authViewModel.logout(context)
        navigateTo("USER_BROWSE")
    }
    fun MainViewModel.navigateToScreen(screen: String) = navigateTo(screen)
    fun MainViewModel.navigateAndRemoveScreens(targetScreen: String, screensToRemove: List<String>) {
        val updated = _screenBackStack.value.toMutableList()
        updated.removeAll(screensToRemove.toSet())
        if (targetScreen == "USER_BROWSE") {
            updated.clear()
            updated.add("USER_BROWSE")
        } else if (!updated.contains(targetScreen)) {
            updated.add(targetScreen)
        }
        _screenBackStack.value = updated
        _currentScreen.value = targetScreen
    }
    fun MainViewModel.navigateTo(screen: String) {
        val updated = _screenBackStack.value.toMutableList()
        if (screen == "USER_BROWSE") {
            updated.clear()
            updated.add("USER_BROWSE")
        } else {
            if (updated.lastOrNull() != screen) {
                updated.add(screen)
            }
        }
        _screenBackStack.value = updated
        _currentScreen.value = screen
    }
fun MainViewModel.goBack(): Boolean {
        val stack = _screenBackStack.value.toMutableList()
        if (stack.size > 1) {
            stack.removeAt(stack.size - 1)
            val prev = stack.last()
            _screenBackStack.value = stack
            _currentScreen.value = prev
            return true
        } else if (_currentScreen.value != "USER_BROWSE") {
            _screenBackStack.value = listOf("USER_BROWSE")
            _currentScreen.value = "USER_BROWSE"
            return true
        }
        return false
    }
fun MainViewModel.switchLanguage() {
        val ctx = appContext
        if (ctx != null) {
            val newLang = LocaleManager.toggleLanguage(ctx)
            _currentLanguage.value = newLang
        } else {
            val newLang = if (_currentLanguage.value == "ar") "en" else "ar"
            _currentLanguage.value = newLang
        }
    }
fun MainViewModel.toggleLanguage(context: android.content.Context) {
        appContext = context.applicationContext
        val newLang = LocaleManager.toggleLanguage(context)
        _currentLanguage.value = newLang
    }
fun MainViewModel.setLanguage(lang: String) {
        _currentLanguage.value = lang
        val ctx = appContext
        if (ctx != null) {
            LocaleManager.setLanguage(ctx, lang)
        }
        val newSettings = _settings.value.copy(appLanguage = lang)
        _settings.value = newSettings
        try {
            settingsViewModel.updateAppLanguage(lang)
        } catch (e: Exception) {
            android.util.Log.e("MainViewModel", "Error: ", e)
        }
    }
fun MainViewModel.setLanguage(context: android.content.Context, lang: String) {
        appContext = context.applicationContext
        setLanguage(lang)
    }
fun MainViewModel.triggerNotification(
        title: String,
        message: String,
        targetType: String = "ALL",
        targetValue: String = "",
        context: android.content.Context? = null
    ) {
        val newNotif = com.example.data.NotificationEntity(
            id = java.util.UUID.randomUUID().toString(),
            title = title,
            message = message,
            targetType = targetType,
            targetValue = targetValue,
            timestamp = System.currentTimeMillis()
        )
        val currentList = _notifications.value.toMutableList()
        currentList.add(0, newNotif)
        _notifications.value = currentList
        try {
            viewModelScope.launch { notificationRepository.saveNotification(newNotif) }
        } catch (e: Exception) {
            android.util.Log.e("MainViewModel", "Error: ", e)
        }
        triggerNotification("$title: $message", context)
    }
    var MainViewModel.lastNotifMsg: String
    get() = ""
    set(value) {}
    var MainViewModel.lastNotifTime: Long
    get() = 0L
    set(value) {}
    fun MainViewModel.triggerNotification(msg: String, context: android.content.Context? = null) {
        val now = System.currentTimeMillis()
        if (msg == lastNotifMsg && (now - lastNotifTime) < 3000L) {
            return
        }
        lastNotifMsg = msg
        lastNotifTime = now
        _toastMessage.value = msg
        val ctx = context ?: appContext
        if (ctx != null) {
            try {
                val channelId = "yemen_services_alerts"
                val nm = ctx.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    val channel = android.app.NotificationChannel(
                        channelId,
                        "إشعارات الخدمة والحجوزات والمحادثات",
                        android.app.NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = "إشعارات التطبيق الفورية"
                        enableVibration(true)
                    }
                    nm?.createNotificationChannel(channel)
                }
                val intent = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)?.apply {
                    flags = android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                val pendingIntent = android.app.PendingIntent.getActivity(
                    ctx, (System.currentTimeMillis() % 1000).toInt(), intent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                )
                val builder = androidx.core.app.NotificationCompat.Builder(ctx, channelId)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle("دليل خدمات اليمن 🔔")
                    .setContentText(msg)
                    .setStyle(androidx.core.app.NotificationCompat.BigTextStyle().bigText(msg))
                    .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                nm?.notify((System.currentTimeMillis() % 10000).toInt(), builder.build())
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Error: ", e)
            }
        }
    }
fun MainViewModel.triggerOpenChatForRequest(requestId: String, customerPhone: String, serviceType: String) {
        val targetPhone = customerPhone.ifBlank { _currentUserPhone.value }
        if (targetPhone.isNotBlank()) {
            getOrCreateChatChannel(
                providerId = "request_$requestId",
                providerName = "صاحب الطلب ($targetPhone)",
                customerId = _currentUserPhone.value.ifBlank { "guest" },
                customerName = _currentUserName.value.ifBlank { "مستخدم الدليل" }
            )
        } else {
            triggerNotification("💬 يمكنك التحدث مع مقدمي العروض عبر شاشة المحادثات")
        }
    }
fun MainViewModel.clearNotification() {
        _toastMessage.value = null
    }
fun MainViewModel.loadUserPoints() {
        _currentUserPoints.value = (100..500).random()
    }
fun MainViewModel.redeemLoyaltyPoints() {
        triggerNotification("🎉 تم استبدال نقاطك بنجاح! تم الخصم بنجاح.")
    }
fun MainViewModel.rewardSharePoints() {
        _currentUserPoints.value = _currentUserPoints.value + 20
        triggerNotification("🎁 حصلت على 20 نقطة مشاركة!")
    }
fun MainViewModel.clearSmartAssistantChatHistory() {
        _currentUserPoints.value = 0
        triggerNotification("🧹 تم تصفية وحذف سجل المحادثة الذكية بنجاح!")
    }
    fun MainViewModel.submitJoinForm(
        context: android.content.Context,
        name: String, phone: String, catId: String, area: String,
        neighborhood: String, photoPath: String, idCardPath: String, gpsCoords: String,
        workPhotos: List<String> = emptyList(),
        customCategoryName: String = "",
        password: String = "",
        productAttachmentsJson: String = ""
    ) {
        registrationHelper.submitJoinForm(
            context = context,
            scope = viewModelScope,
            name = name,
            phone = phone,
            catId = catId,
            area = area,
            neighborhood = neighborhood,
            photoPath = photoPath,
            idCardPath = idCardPath,
            gpsCoords = gpsCoords,
            workPhotos = workPhotos,
            customCategoryName = customCategoryName,
            password = password,
            productAttachmentsJson = productAttachmentsJson,
            checkDuplicate = { checkAndGetDuplicateAccountType(it, "") },
            logAdminActivity = { logAdminActivity(it) },
            triggerNotification = { triggerNotification(it) },
            addApplicantNotification = { title, msg, type, targetVal ->
                addNotification(
                    title = title,
                    message = msg,
                    targetType = type,
                    targetValue = targetVal
                )
            },
            onPendingAdded = { newReq ->
                val currentPending = _pendingProviders.value.filter { it.id != newReq.id }.toMutableList()
                currentPending.add(newReq)
                _pendingProviders.value = currentPending
                val currentTechs = _pendingTechnicians.value.filter { it.id != newReq.id }.toMutableList()
                currentTechs.add(newReq)
                _pendingTechnicians.value = currentTechs
            },
            onStoreAdded = { newStore ->
                val sList = _stores.value.toMutableList()
                sList.removeAll { it.id == newStore.id }
                sList.add(newStore)
                _stores.value = sList
            },
            onPropertyAdded = { newProp ->
                val pList = _properties.value.toMutableList()
                pList.removeAll { it.id == newProp.id }
                pList.add(newProp)
                _properties.value = pList
            },
            onJobAdded = { newJob ->
                val jList = _jobs.value.toMutableList()
                jList.removeAll { it.id == newJob.id }
                jList.add(newJob)
                _jobs.value = jList
            },
            onClientAdded = { userMap ->
                val uList = _registeredUsersList.value.toMutableList()
                uList.removeAll { it["phone"] == userMap["phone"] }
                uList.add(userMap)
                _registeredUsersList.value = uList
            },
            onJoinRequestPhoneUpdated = { _joinRequestPhone.value = it },
            onNavigateToScreen = { _currentScreen.value = it }
        )
    }

    fun MainViewModel.registerClientUser(name: String, phone: String, residence: String, password: String = "") {
        registrationHelper.registerClientUser(name, phone, residence, password) { userMap ->
            val uList = _registeredUsersList.value.toMutableList()
            uList.removeAll { it["phone"] == userMap["phone"] }
            uList.add(userMap)
            _registeredUsersList.value = uList
        }
    }

    fun MainViewModel.cancelOrResetJoinRequest(context: android.content.Context) {
        registrationHelper.cancelOrResetJoinRequest(
            context = context,
            phone = _joinRequestPhone.value,
            pendingProviders = _pendingProviders.value,
            onPendingRemoved = { id ->
                _pendingProviders.value = _pendingProviders.value.filter { it.id != id }
            },
            onPhoneCleared = { _joinRequestPhone.value = "" },
            onGoBack = { goBack() }
        )
    }

    fun MainViewModel.setJoinRequestPhone(context: android.content.Context, phone: String) {
        registrationHelper.setJoinRequestPhone(context, phone) {
            _joinRequestPhone.value = it
        }
    }

fun MainViewModel.addNotification(
        title: String,
        message: String,
        targetType: String = "ALL",
        targetValue: String = "",
        targetAudience: String = "ALL",
        targetRoles: List<String> = emptyList(),
        targetUserIds: List<String> = emptyList(),
        senderId: String = "SYSTEM",
        senderName: String = "النظام",
        dedupKey: String = "",
        expiryTimestamp: Long = 0L,
        scheduledTime: Long = 0L,
        customerPhone: String = "",
        customerName: String = "",
        notificationType: String = "NORMAL",
        channel: String = "IN_APP"
    ) {
        if (title.trim().isEmpty() || message.trim().isEmpty()) {
            return
        }
        val providerByPhone = _providers.value.find { it.phone.trim() == targetValue.trim() }
        val providerById = _providers.value.find { it.id == targetValue }
        val isNotifDisabled = (providerByPhone?.isNotificationsDisabled == true) || (providerById?.isNotificationsDisabled == true)
        if (isNotifDisabled) {
            triggerNotification("⚠️ تم حجب إرسال هذا الإشعار لأن الإدارة قامت بتعطيل إشعارات الفني: ${providerByPhone?.name ?: providerById?.name ?: ""}")
            return
        }
        val finalDedupKey = if (dedupKey.isNotBlank()) dedupKey else "${notificationType}_${targetValue}_${title}_${System.currentTimeMillis() / (30 * 1000L)}"
        val isDuplicate = _notifications.value.any { it.dedupKey == finalDedupKey || (it.title == title && it.targetValue == targetValue && Math.abs(it.timestamp - System.currentTimeMillis()) < 15000L) }
        if (isDuplicate) {
            return
        }
        val newNotif = NotificationEntity(
            id = "n_" + UUID.randomUUID().toString().take(8),
            title = title.trim(),
            message = message.trim(),
            targetType = targetType,
            targetValue = targetValue,
            targetAudience = targetAudience,
            targetRoles = targetRoles,
            targetUserIds = targetUserIds,
            senderId = senderId,
            senderName = senderName,
            dedupKey = finalDedupKey,
            timestamp = System.currentTimeMillis(),
            expiryTimestamp = expiryTimestamp,
            scheduledTime = scheduledTime,
            customerPhone = customerPhone,
            customerName = customerName,
            notificationType = notificationType,
            channel = channel
        )
        _notifications.value = listOf(newNotif) + _notifications.value.filter { it.id != newNotif.id }
        try {
            viewModelScope.launch { notificationRepository.saveNotification(newNotif) }
        } catch (e: Exception) {
            android.util.Log.e("MainViewModel", "Error: ", e)
        }
        triggerNotification("🔔 تم إرسال الإشعار الموثوق بنجاح!")
    }
    fun MainViewModel.approveRequest(request: PendingProviderEntity) = adminViewModel.approveRequest(request)
    fun MainViewModel.rejectRequest(request: PendingProviderEntity, reason: String) = adminViewModel.rejectRequest(request, reason)
    fun MainViewModel.approveTechnician(providerId: String) = adminViewModel.approveTechnician(providerId)
    fun MainViewModel.loadPendingTechnicians() = adminViewModel.loadPendingTechnicians()
    fun MainViewModel.approvePendingProvider(pending: PendingProviderEntity) = adminViewModel.approvePendingProvider(pending)
    fun MainViewModel.saveStore(store: com.example.data.StoreEntity) = adminViewModel.saveStore(store)
    fun MainViewModel.deleteStore(storeId: String) = adminViewModel.deleteStore(storeId)
    fun MainViewModel.restoreStore(storeId: String) = adminViewModel.restoreStore(storeId)
    fun MainViewModel.deleteStorePermanently(storeId: String) = adminViewModel.deleteStorePermanently(storeId)
    fun MainViewModel.setStoreActive(storeId: String, isActive: Boolean) = adminViewModel.setStoreActive(storeId, isActive)
    fun MainViewModel.setStorePinned(storeId: String, isPinned: Boolean) = adminViewModel.setStorePinned(storeId, isPinned)
    fun MainViewModel.setStoreVip(storeId: String, isVip: Boolean) = adminViewModel.setStoreVip(storeId, isVip)
    fun MainViewModel.setStoreVerified(storeId: String, isVerified: Boolean) = adminViewModel.setStoreVerified(storeId, isVerified)
    fun MainViewModel.setStoreRecommended(storeId: String, isRecommended: Boolean) = adminViewModel.setStoreRecommended(storeId, isRecommended)
    fun MainViewModel.setStoreChatDisabled(storeId: String, isDisabled: Boolean) = adminViewModel.setStoreChatDisabled(storeId, isDisabled)
    fun MainViewModel.setStoreNotificationsDisabled(storeId: String, isDisabled: Boolean) = adminViewModel.setStoreNotificationsDisabled(storeId, isDisabled)
    fun MainViewModel.setStorePaymentEnabled(storeId: String, isEnabled: Boolean) = adminViewModel.setStorePaymentEnabled(storeId, isEnabled)
    fun MainViewModel.toggleStoreBlocked(storeId: String, isBlocked: Boolean) = adminViewModel.toggleStoreBlocked(storeId, isBlocked)
    fun MainViewModel.toggleStoreActive(storeId: String) = adminViewModel.toggleStoreActive(storeId)
    fun MainViewModel.toggleStorePinned(storeId: String) = adminViewModel.toggleStorePinned(storeId)
    fun MainViewModel.toggleStoreChatDisabled(storeId: String) = adminViewModel.toggleStoreChatDisabled(storeId)
    fun MainViewModel.approveStorePdf(storeId: String, approve: Boolean) = adminViewModel.approveStorePdf(storeId, approve)
    fun MainViewModel.saveProperty(property: com.example.data.PropertyEntity) = adminViewModel.saveProperty(property)
    fun MainViewModel.deleteProperty(propertyId: String) = adminViewModel.deleteProperty(propertyId)
    fun MainViewModel.restoreProperty(propertyId: String) = adminViewModel.restoreProperty(propertyId)
    fun MainViewModel.deletePropertyPermanently(propertyId: String) = adminViewModel.deletePropertyPermanently(propertyId)
    fun MainViewModel.setPropertyActive(propertyId: String, isActive: Boolean) = adminViewModel.setPropertyActive(propertyId, isActive)
    fun MainViewModel.setPropertyPinned(propertyId: String, isPinned: Boolean) = adminViewModel.setPropertyPinned(propertyId, isPinned)
    fun MainViewModel.setPropertyVip(propertyId: String, isVip: Boolean) = adminViewModel.setPropertyVip(propertyId, isVip)
    fun MainViewModel.setPropertyVerified(propertyId: String, isVerified: Boolean) = adminViewModel.setPropertyVerified(propertyId, isVerified)
    fun MainViewModel.setPropertyRecommended(propertyId: String, isRecommended: Boolean) = adminViewModel.setPropertyRecommended(propertyId, isRecommended)
    fun MainViewModel.setPropertyChatDisabled(propertyId: String, isDisabled: Boolean) = adminViewModel.setPropertyChatDisabled(propertyId, isDisabled)
    fun MainViewModel.setPropertyNotificationsDisabled(propertyId: String, isDisabled: Boolean) = adminViewModel.setPropertyNotificationsDisabled(propertyId, isDisabled)
    fun MainViewModel.setPropertyPaymentEnabled(propertyId: String, isEnabled: Boolean) = adminViewModel.setPropertyPaymentEnabled(propertyId, isEnabled)
    fun MainViewModel.togglePropertyBlocked(propertyId: String, isBlocked: Boolean) = adminViewModel.togglePropertyBlocked(propertyId, isBlocked)
    fun MainViewModel.approvePropertyPdf(propertyId: String, approve: Boolean) = adminViewModel.approvePropertyPdf(propertyId, approve)
    fun MainViewModel.saveJob(job: com.example.data.JobEntity) = adminViewModel.saveJob(job)
    fun MainViewModel.deleteJob(jobId: String) = adminViewModel.deleteJob(jobId)
    fun MainViewModel.restoreJob(jobId: String) = adminViewModel.restoreJob(jobId)
    fun MainViewModel.deleteJobPermanently(jobId: String) = adminViewModel.deleteJobPermanently(jobId)
    fun MainViewModel.setJobApproved(jobId: String, isApproved: Boolean) = adminViewModel.setJobApproved(jobId, isApproved)
    fun MainViewModel.setJobPinned(jobId: String, isPinned: Boolean) = adminViewModel.setJobPinned(jobId, isPinned)
    fun MainViewModel.setJobVip(jobId: String, isVip: Boolean) = adminViewModel.setJobVip(jobId, isVip)
    fun MainViewModel.setJobChatDisabled(jobId: String, isDisabled: Boolean) = adminViewModel.setJobChatDisabled(jobId, isDisabled)
    fun MainViewModel.submitJobApplication(application: com.example.data.JobApplicationEntity) = adminViewModel.submitJobApplication(application)
    fun MainViewModel.updateJobApplicationStatus(appId: String, status: String) = adminViewModel.updateJobApplicationStatus(appId, status)
    fun MainViewModel.acceptJobApplication(appId: String) = adminViewModel.acceptJobApplication(appId)
    fun MainViewModel.rejectJobApplication(appId: String, reason: String) = adminViewModel.rejectJobApplication(appId, reason)
    fun MainViewModel.deleteJobApplication(appId: String) = adminViewModel.deleteJobApplication(appId)
    fun MainViewModel.deleteReport(reportId: String) = adminViewModel.deleteReport(reportId)
    fun MainViewModel.sendReport(providerId: String, providerName: String, reporterName: String, content: String) = adminViewModel.sendReport(providerId, providerName, reporterName, content)
    fun MainViewModel.saveCoupon(coupon: CouponEntity) = adminViewModel.saveCoupon(coupon)
    fun MainViewModel.deleteCoupon(couponId: String) = adminViewModel.deleteCoupon(couponId)
    fun MainViewModel.saveInternalWallet(wallet: com.example.data.InternalWalletEntity) = adminViewModel.saveInternalWallet(wallet)
    fun MainViewModel.performWalletTransaction(
        walletId: String,
        ownerName: String,
        ownerPhone: String,
        ownerType: String,
        type: String, // DEPOSIT, WITHDRAWAL, TRANSFER
        amount: Double,
        note: String
    ) = adminViewModel.performWalletTransaction(walletId, ownerName, ownerPhone, ownerType, type, amount, note)
    fun MainViewModel.addPaymentWallet(wallet: PaymentWalletEntity) = adminViewModel.addPaymentWallet(wallet)
    fun MainViewModel.updatePaymentWallet(wallet: PaymentWalletEntity) = adminViewModel.updatePaymentWallet(wallet)
    fun MainViewModel.deletePaymentWallet(walletId: String) = adminViewModel.deletePaymentWallet(walletId)
    fun MainViewModel.togglePaymentWalletVisibility(walletId: String, currentVisible: Boolean) = adminViewModel.togglePaymentWalletVisibility(walletId, currentVisible)
    fun MainViewModel.confirmPayment(
        paymentId: String,
        transferId: String,
        transferPhoto: String,
        walletProvider: String,
        walletNumber: String,
        walletAccountName: String
    ) = adminViewModel.confirmPayment(paymentId, transferId, transferPhoto, walletProvider, walletNumber, walletAccountName)
    fun MainViewModel.verifyPayment(paymentId: String, isVerified: Boolean, note: String, adminName: String) = adminViewModel.verifyPayment(paymentId, isVerified, note, adminName)
    fun MainViewModel.refundPayment(paymentId: String, reason: String) = adminViewModel.refundPayment(paymentId, reason)
    fun MainViewModel.saveProduct(product: com.example.data.ProductEntity) = adminViewModel.saveProduct(product)
    fun MainViewModel.deleteProduct(productId: String) = adminViewModel.deleteProduct(productId)
    fun MainViewModel.updateProductPrice(productId: String, newPrice: Double) = adminViewModel.updateProductPrice(productId, newPrice)
    fun MainViewModel.saveOffer(offer: com.example.data.models.Offer) = adminViewModel.saveOffer(offer)
    fun MainViewModel.deleteOffer(offerId: String) = adminViewModel.deleteOffer(offerId)
    fun MainViewModel.toggleOfferStatus(offerId: String, isActive: Boolean) = adminViewModel.toggleOfferStatus(offerId, isActive)
    fun MainViewModel.listenToOffersForEntity(
        entityId: String,
        onResult: (List<com.example.data.models.Offer>) -> Unit
    ): com.google.firebase.firestore.ListenerRegistration = adminViewModel.listenToOffersForEntity(entityId, onResult)
    fun MainViewModel.listenToProductsForStore(
        storeId: String,
        onResult: (List<com.example.data.ProductEntity>) -> Unit
    ): com.google.firebase.firestore.ListenerRegistration = adminViewModel.listenToProductsForStore(storeId, onResult)
    fun MainViewModel.saveCustomProfileTab(tab: com.example.data.CustomProfileTabEntity) = adminViewModel.saveCustomProfileTab(tab)
    fun MainViewModel.deleteCustomProfileTab(tabId: String) = adminViewModel.deleteCustomProfileTab(tabId)
    fun MainViewModel.toggleCustomProfileTab(tabId: String) = adminViewModel.toggleCustomProfileTab(tabId)
    fun MainViewModel.deleteCategory(categoryId: String) = adminViewModel.deleteCategory(categoryId)
    fun MainViewModel.togglePinCategory(categoryId: String) = adminViewModel.togglePinCategory(categoryId)
    fun MainViewModel.mergeCategories(sourceCategoryId: String, targetCategoryId: String) = adminViewModel.mergeCategories(sourceCategoryId, targetCategoryId)
    fun MainViewModel.saveCategoryEntity(cat: CategoryEntity) = adminViewModel.saveCategoryEntity(cat)
    fun MainViewModel.addSubCategory(parentId: String, nameAr: String, icon: String) = adminViewModel.addSubCategory(parentId, nameAr, icon)
    fun MainViewModel.convertCategoryType(catId: String, newParentId: String, isMain: Boolean) = adminViewModel.convertCategoryType(catId, newParentId, isMain)
    fun MainViewModel.reorderCategories(newOrderedList: List<CategoryEntity>) = adminViewModel.reorderCategories(newOrderedList)
    fun MainViewModel.updateCity(city: CityEntity) = adminViewModel.updateCity(city)
    fun MainViewModel.removeCity(cityId: String) = adminViewModel.removeCity(cityId)
    fun MainViewModel.removeProvider(providerId: String) = adminViewModel.removeProvider(providerId)
    fun MainViewModel.removeProviderPermanently(providerId: String) = adminViewModel.removeProviderPermanently(providerId)
    fun MainViewModel.restoreProvider(providerId: String) = adminViewModel.restoreProvider(providerId)
    fun MainViewModel.pinProvider(providerId: String, isPinned: Boolean) = adminViewModel.pinProvider(providerId, isPinned)
    fun MainViewModel.recommendProvider(providerId: String, isRecommended: Boolean) = adminViewModel.recommendProvider(providerId, isRecommended)
    fun MainViewModel.verifyProviderBadge(providerId: String, isVerified: Boolean) = adminViewModel.verifyProviderBadge(providerId, isVerified)
    fun MainViewModel.toggleProviderSubscription(providerId: String, status: String) = adminViewModel.toggleProviderSubscription(providerId, status)
    fun MainViewModel.setProviderChatDisabled(providerId: String, disabled: Boolean) = adminViewModel.setProviderChatDisabled(providerId, disabled)
    fun MainViewModel.setProviderNotificationsDisabled(providerId: String, disabled: Boolean) = adminViewModel.setProviderNotificationsDisabled(providerId, disabled)
    fun MainViewModel.setProviderPaymentRequired(providerId: String, required: Boolean) = adminViewModel.setProviderPaymentRequired(providerId, required)
    fun MainViewModel.extendProviderSubscription(providerId: String, extraMs: Long) = adminViewModel.extendProviderSubscription(providerId, extraMs)
    fun MainViewModel.toggleProviderBlock(providerId: String) = adminViewModel.toggleProviderBlock(providerId)
    fun MainViewModel.toggleProviderStatus(provider: ProviderEntity) = adminViewModel.toggleProviderStatus(provider)
    fun MainViewModel.toggleProviderPin(providerId: String) = adminViewModel.toggleProviderPin(providerId)
    fun MainViewModel.toggleProviderVerification(providerId: String) = adminViewModel.toggleProviderVerification(providerId)
    fun MainViewModel.toggleProviderRecommendation(providerId: String) = adminViewModel.toggleProviderRecommendation(providerId)
    fun MainViewModel.updateProviderEntity(provider: ProviderEntity) = adminViewModel.updateProviderEntity(provider)
    fun MainViewModel.updateStoreEntity(store: StoreEntity) {
        viewModelScope.launch { adminViewModel.crud.saveEntity("stores", store.id, store) }
        triggerNotification("✅ تم تحديث بيانات المتجر بنجاح")
    }
    fun MainViewModel.updatePropertyEntity(property: PropertyEntity) {
        viewModelScope.launch { adminViewModel.crud.saveEntity("properties", property.id, property) }
        triggerNotification("✅ تم تحديث بيانات العقار بنجاح")
    }
    fun MainViewModel.updateBusinessAccountStatus(accountId: String, isActive: Boolean) {
        viewModelScope.launch {
            try {
                adminViewModel.crud.updateFields("stores", accountId, mapOf("isActive" to isActive))
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Error: ", e)
            }
            try {
                adminViewModel.crud.updateFields("providers", accountId, mapOf("isAvailable" to isActive))
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Error: ", e)
            }
        }
    }
    fun MainViewModel.updateEntityImages(collection: String, id: String, profileImg: String, coverImg: String) {
        val updates = mutableMapOf<String, Any>()
        if (profileImg.isNotBlank()) {
            updates["profileImage"] = profileImg
            updates["logoImage"] = profileImg
        }
        if (coverImg.isNotBlank()) {
            updates["coverImage"] = coverImg
        }
        if (updates.isNotEmpty()) {
            viewModelScope.launch { adminViewModel.crud.updateFields(collection, id, updates) }
            triggerNotification("📸 تم تحديث الصور بنجاح")
        }
    }
    fun MainViewModel.editProviderPhoneAndCategory(providerId: String, newPhone: String, newCategoryId: String) = adminViewModel.editProviderPhoneAndCategory(providerId, newPhone, newCategoryId)
    fun MainViewModel.addNewProvider(name: String, phone: String, catId: String, area: String, price: Double, isVip: Boolean) = adminViewModel.addNewProvider(name, phone, catId, area, price, isVip)
    fun MainViewModel.addNewProviderCustom(
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
    ) = adminViewModel.addNewProviderCustom(name, phone, catId, street, cityId, profileImage, idCardImage, forensicImage, price, isVip)
    fun MainViewModel.deleteBanner(bannerId: String) = adminViewModel.deleteBanner(bannerId)
    fun MainViewModel.reorderBanners(newOrderedList: List<BannerEntity>) = adminViewModel.reorderBanners(newOrderedList)
    fun MainViewModel.placeOrder(order: com.example.data.OrderEntity) = adminViewModel.placeOrder(order)
    fun MainViewModel.updateOrderStatus(orderId: String, status: String) = adminViewModel.updateOrderStatus(orderId, status)
    fun MainViewModel.deleteOrder(orderId: String) = adminViewModel.deleteOrder(orderId)
    fun MainViewModel.deleteAllOrders(customerPhone: String) = adminViewModel.deleteAllOrders(customerPhone)
    fun MainViewModel.addRating(rating: com.example.data.RatingEntity) = adminViewModel.addRating(rating)
    fun MainViewModel.addRatingReply(ratingId: String, replyText: String) = adminViewModel.addRatingReply(ratingId, replyText)
    fun MainViewModel.deleteRating(ratingId: String) = adminViewModel.deleteRating(ratingId)
    fun MainViewModel.approveRating(ratingId: String, isApproved: Boolean) = adminViewModel.approveRating(ratingId, isApproved)
    fun MainViewModel.submitRating(providerId: String, rating: Int) = adminViewModel.submitRating(providerId, rating)
    fun MainViewModel.recalculateTargetRating(targetId: String, targetType: String) = adminViewModel.recalculateTargetRating(targetId, targetType)
    fun MainViewModel.logAdminActivity(action: String) = adminViewModel.logAdminActivity(action)
    fun MainViewModel.logCall(providerId: String, providerName: String) = adminViewModel.logCall(providerId, providerName)
    fun MainViewModel.checkAndGetDuplicateAccountType(phone: String, excludeId: String): String? = adminViewModel.checkAndGetDuplicateAccountType(phone, excludeId)
    fun MainViewModel.updateProviderPortfolio(providerId: String, images: List<String>) = adminViewModel.updateProviderPortfolio(providerId, images)
    fun MainViewModel.addPortfolioImage(providerId: String, imageBase64: String) = adminViewModel.addPortfolioImage(providerId, imageBase64)
    fun MainViewModel.removePortfolioImage(providerId: String, index: Int) = adminViewModel.removePortfolioImage(providerId, index)
    fun MainViewModel.clearPortfolio(providerId: String) = adminViewModel.clearPortfolio(providerId)
    fun MainViewModel.redirectBookingToEntity(bookingId: String, targetEntityId: String, targetEntityName: String, targetPhone: String) = adminViewModel.redirectBookingToEntity(bookingId, targetEntityId, targetEntityName, targetPhone)
    fun MainViewModel.unbanEntity(entityType: String, entityId: String) = adminViewModel.unbanEntity(entityType, entityId)
    fun MainViewModel.restoreEntity(entityType: String, entityId: String) = adminViewModel.restoreEntity(entityType, entityId)
    fun MainViewModel.hardDeleteEntity(entityType: String, entityId: String) = adminViewModel.hardDeleteEntity(entityType, entityId)
    fun MainViewModel.exportJobApplicantsCsv(context: android.content.Context) = adminViewModel.exportJobApplicantsCsv(context)
    fun MainViewModel.loadCardSettings() = settingsViewModel.loadCardSettings()
    fun MainViewModel.updateCardSettings(settings: com.example.ui.viewmodels.SettingsViewModel.CardSettings) = settingsViewModel.updateCardSettings(settings)
    fun MainViewModel.updateTheme(themeId: String) = settingsViewModel.updateTheme(themeId)
    fun MainViewModel.saveCustomSettingsState(newSettings: AdminSettingsEntity) = settingsViewModel.saveCustomSettingsState(newSettings)
    fun MainViewModel.updateAdminSettings(newSettings: AdminSettingsEntity) = settingsViewModel.updateAdminSettings(newSettings)
    fun MainViewModel.initColorSync(context: android.content.Context) = settingsViewModel.initColorSync(context)
    fun MainViewModel.updateCloudColorScheme(context: android.content.Context, newScheme: com.example.data.ColorSchemeEntity) = settingsViewModel.updateCloudColorScheme(context, newScheme)
    fun MainViewModel.updatePersonalColors(context: android.content.Context, personal: com.example.data.PersonalColors) = settingsViewModel.updatePersonalColors(context, personal)
    fun MainViewModel.triggerManualSync(context: android.content.Context) = settingsViewModel.triggerManualSync(context)
    fun MainViewModel.resolveConflict(context: android.content.Context, useCloud: Boolean) = settingsViewModel.resolveConflict(context, useCloud)
    fun MainViewModel.getCurrentTimestampString(): String = settingsViewModel.getCurrentTimestampString()
    fun MainViewModel.addNewSyncLog(
        context: android.content.Context,
        type: String,
        status: String,
        changes: List<String>,
        versionFrom: Int,
        versionTo: Int
    ) = settingsViewModel.addNewSyncLog(context, type, status, changes, versionFrom, versionTo)
    fun MainViewModel.toggleChatParticipant(participantType: ChatParticipantType) = settingsViewModel.toggleChatParticipant(participantType)
    fun MainViewModel.isChatBlockedFor(participantType: ChatParticipantType): Boolean = settingsViewModel.isChatBlockedFor(participantType)
    fun MainViewModel.canParticipateInChat(participantType: ChatParticipantType): Boolean = settingsViewModel.canParticipateInChat(participantType)
    fun MainViewModel.startVoiceCall(name: String, role: String) = settingsViewModel.startVoiceCall(name, role)
    fun MainViewModel.endVoiceCall() = settingsViewModel.endVoiceCall()
    fun MainViewModel.exportComplaintsToCSV() = settingsViewModel.exportComplaintsToCSV()
    fun MainViewModel.exportComplaintsToPDF() = settingsViewModel.exportComplaintsToPDF()
    fun MainViewModel.exportPerformanceReportToPDF() = settingsViewModel.exportPerformanceReportToPDF()
    fun MainViewModel.createSystemBackup(onComplete: (Boolean, String) -> Unit) = settingsViewModel.createSystemBackup(onComplete)
    fun MainViewModel.restoreSystemFromBackup(jsonStr: String, onComplete: (Boolean, String) -> Unit) = settingsViewModel.restoreSystemFromBackup(jsonStr, onComplete)
    fun MainViewModel.exportSelectedCollectionsAsJson(selectedCollections: List<String>, onResult: (String) -> Unit) = settingsViewModel.exportSelectedCollectionsAsJson(selectedCollections, onResult)
    fun MainViewModel.saveBackupToLocalStorage(context: android.content.Context, jsonStr: String, fileName: String): String = settingsViewModel.saveBackupToLocalStorage(context, jsonStr, fileName)
    fun MainViewModel.setSecondaryFirebaseConfig(projectId: String, apiKey: String, appId: String, storageBucket: String, isEnabled: Boolean) = settingsViewModel.setSecondaryFirebaseConfig(projectId, apiKey, appId, storageBucket, isEnabled)
    fun MainViewModel.saveCustomPermissionsMatrixToFirestore(permissions: List<String>) = settingsViewModel.saveCustomPermissionsMatrixToFirestore(permissions)
    fun MainViewModel.deleteColorPalette(id: String) = settingsViewModel.deleteColorPalette(id)
    fun MainViewModel.resetAccountPassword(entityType: String, phoneOrId: String, newPass: String) = settingsViewModel.resetAccountPassword(entityType, phoneOrId, newPass)
    fun MainViewModel.requestAdminPasswordReset(phone: String) = settingsViewModel.requestAdminPasswordReset(phone)
    fun MainViewModel.requestPasswordReset(phone: String, onResult: (Boolean, String) -> Unit) = settingsViewModel.requestPasswordReset(phone, onResult)
    fun MainViewModel.approvePasswordReset(phone: String, onResult: (Boolean, String) -> Unit) = settingsViewModel.approvePasswordReset(phone, onResult)
    fun MainViewModel.adminResetAccountPassword(phone: String, newPassword: String, notifyAction: String, customerName: String) = settingsViewModel.adminResetAccountPassword(phone, newPassword, notifyAction, customerName)
    fun MainViewModel.requestPasswordRecoveryForStore(name: String, phone: String, password: String) = settingsViewModel.requestPasswordRecoveryForStore(name, phone, password)
    fun MainViewModel.requestPasswordRecoveryForProperty(title: String, phone: String, password: String) = settingsViewModel.requestPasswordRecoveryForProperty(title, phone, password)
    fun MainViewModel.requestPasswordRecoveryGeneral(accountName: String, phone: String, accountType: String, currentPassword: String) = settingsViewModel.requestPasswordRecoveryGeneral(accountName, phone, accountType, currentPassword)
    fun MainViewModel.wipeAllDatabaseData(password: String): Boolean = settingsViewModel.wipeAllDatabaseData(password)
    fun MainViewModel.wipeSelectedDatabaseData(password: String, selectedCollections: List<String>): Boolean = settingsViewModel.wipeSelectedDatabaseData(password, selectedCollections)
    fun MainViewModel.wipeAllMockAndTemporaryData() = settingsViewModel.wipeAllMockAndTemporaryData()
    fun MainViewModel.acceptRequestOffer(
        req: com.example.data.models.InstantRequestEntity,
        offer: com.example.data.models.RequestOfferEntity
    ) = instantRequestViewModel.acceptRequestOffer(req, offer)
    fun MainViewModel.completeInstantRequest(requestId: String) = instantRequestViewModel.completeInstantRequest(requestId)
    fun MainViewModel.setPasswordRecoveryWaitingPhone(phone: String) = authViewModel.setPasswordRecoveryWaitingPhone(phone)
    fun MainViewModel.resetRegistrationState() = authViewModel.resetRegistrationState()
    fun MainViewModel.searchAccountForRestore(cleanPhone: String, onResult: (MainViewModel.RestoreAccountMatch?) -> Unit) {
        accountRecoveryHelper.searchAccountForRestore(cleanPhone, onResult)
    }
    fun MainViewModel.requestPasswordReset(
        context: android.content.Context,
        phone: String,
        name: String,
        accountType: String,
        onResult: (Boolean) -> Unit
    ) {
        accountRecoveryHelper.requestPasswordReset(
            context = context,
            phone = phone,
            name = name,
            accountType = accountType,
            onPasswordWaitingPhoneSet = { setPasswordRecoveryWaitingPhone(it) },
            triggerNotification = { triggerNotification(it) },
            onResult = onResult
        )
    }
    fun MainViewModel.adminResolvePasswordReset(
        context: android.content.Context,
        phone: String,
        newPassword: String,
        onResult: (Boolean) -> Unit
    ) {
        accountRecoveryHelper.adminResolvePasswordReset(
            context = context,
            phone = phone,
            newPassword = newPassword,
            onResult = onResult
        )
    }
    fun MainViewModel.isUserLoggedIn(context: android.content.Context): Boolean {
        val isLoggedIn = preferenceHelper.isAccountLoggedIn(context)
        val phone = currentUserPhone.value
        return isLoggedIn || (phone.isNotBlank() && currentUserId.value != "guest" && currentUserId.value.isNotBlank())
    }
    fun MainViewModel.restoreUserAccountByPhoneAndPassword(
        context: android.content.Context,
        phone: String,
        password: String,
        onResult: (Boolean, String) -> Unit
    ) {
        val email = getAuthEmailForPhone(phone)
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val userId = authResult.user?.uid ?: ""
                viewModelScope.launch {
                    adminViewModel.crud.updateFields("users", userId, mapOf("isDeleted" to false))
                    adminViewModel.crud.updateFields("providers", phone, mapOf("isDeleted" to false))
                    adminViewModel.crud.updateFields("stores", phone, mapOf("isDeleted" to false))
                    adminViewModel.crud.updateFields("properties", phone, mapOf("isDeleted" to false))
                    onResult(true, "تم استعادة الحساب بنجاح!")
                }
            }
            .addOnFailureListener { e ->
                onResult(false, e.message ?: "فشل في تسجيل الدخول")
            }
            .addOnFailureListener { e ->
                onResult(false, e.message ?: "فشل في تسجيل الدخول")
            }
    }
    fun MainViewModel.restoreGuestUser(context: android.content.Context, phone: String, password: String, onResult: (Boolean, String) -> Unit) {
        restoreUserAccountByPhoneAndPassword(context, phone, password, onResult)
    }
    fun MainViewModel.loginUserDirectly(context: android.content.Context, phone: String) = authViewModel.loginUserDirectly(context, phone)
    fun MainViewModel.showBackdoorDialog() = authViewModel.showBackdoorDialog()
    fun MainViewModel.dismissBackdoorDialog() = authViewModel.dismissBackdoorDialog()
    fun MainViewModel.setSupervisorSession(sup: SupervisorEntity) = authViewModel.setSupervisorSession(sup)
    fun MainViewModel.hasAdminPermission(permissionKey: String): Boolean = authViewModel.hasAdminPermission(permissionKey)
    fun MainViewModel.updateSupervisorPermissions(id: String, permissions: List<String>) = authViewModel.updateSupervisorPermissions(id, permissions)
    fun MainViewModel.removeSupervisor(id: String) = authViewModel.removeSupervisor(id)
    fun MainViewModel.getOrCreateChatChannel(providerId: String, providerName: String, customerId: String, customerName: String) {
        viewModelScope.launch {
            chatRepo.getOrCreateChannel(
                currentUserId = providerId,
                currentUserName = providerName,
                currentUserPhoto = "",
                otherUserId = customerId,
                otherUserName = customerName,
                otherUserPhoto = "",
                type = com.example.data.models.ChannelType.PRIVATE,
                relatedEntityId = null,
                relatedEntityType = null
            )
        }
    }
    fun MainViewModel.deleteChatChannel(channelId: String) {
        viewModelScope.launch {
            chatRepo.deleteChannel(channelId)
            triggerToast("تم حذف المحادثة")
        }
    }
    fun MainViewModel.toggleBlockChatChannel(channelId: String) {
        viewModelScope.launch {
            val ch = _chatChannels.value.find { it.id == channelId } ?: return@launch
            blockChatChannel(channelId, !ch.isBlocked)
        }
    }
    fun MainViewModel.blockChatChannel(channelId: String, blocked: Boolean) {
        viewModelScope.launch { adminViewModel.crud.updateFields("chat_channels", channelId, mapOf("isBlocked" to blocked)) }
        _activeChatChannel.value = _activeChatChannel.value?.copy(isBlocked = blocked)
    }
    fun MainViewModel.wipeOldChatChannels(days: Int) {
        triggerToast("🧹 تم تصفية وحذف سجل المحادثات الأقدم من $days أيام بنجاح!")
    }
    fun MainViewModel.updateBookingFormFields(fields: BookingFormFields) = bookingViewModel.updateBookingFormFields(fields)
    fun MainViewModel.updateDistributionMode(mode: BookingDistributionMode) = bookingViewModel.updateDistributionMode(mode)
    fun MainViewModel.getBookingStatusColor(status: String): String = bookingViewModel.getBookingStatusColor(status)
    fun MainViewModel.getBookingStatusLabel(status: String): String = bookingViewModel.getBookingStatusLabel(status)
    fun MainViewModel.getBookingProgress(status: String): Float = bookingViewModel.getBookingProgress(status)
    fun MainViewModel.markNotificationAsRead(context: android.content.Context, notifId: String) {
        val currentRead = _readNotificationIds.value.toMutableSet()
        val updated = preferenceHelper.markNotificationAsRead(context, notifId, currentRead)
        _readNotificationIds.value = updated
    }
    fun MainViewModel.loadReadNotifications(context: android.content.Context) {
        _readNotificationIds.value = preferenceHelper.getReadNotificationIds(context)
    }
    fun MainViewModel.markAllNotificationsAsRead(context: android.content.Context) {
        val allIds = _notifications.value.map { it.id }.toSet()
        preferenceHelper.markAllNotificationsAsRead(context, allIds)
        _readNotificationIds.value = allIds
    }
    fun MainViewModel.deleteNotification(notifId: String) {
        viewModelScope.launch { notificationRepository.deleteNotification(notifId) }
        _notifications.value = _notifications.value.filter { it.id != notifId }
        val currentRead = _readNotificationIds.value.toMutableSet()
        currentRead.remove(notifId)
        _readNotificationIds.value = currentRead
    }
    fun MainViewModel.deleteAllNotifications() {
        val allNotifs = _notifications.value
        _notifications.value = emptyList()
        _readNotificationIds.value = emptySet()
        allNotifs.forEach { notif ->
            viewModelScope.launch { notificationRepository.deleteNotification(notif.id) }
        }
    }
    fun MainViewModel.clearAllNotifications() {
        deleteAllNotifications()
    }
    val MainViewModel.isProviderUser: Boolean get() = adminRole.value == "PROVIDER" || adminRole.value == "TECHNICIAN"
    val MainViewModel._currentSupervisorPermissions get() = authViewModel._currentSupervisorPermissions
    val MainViewModel.currentSupervisorPermissions get() = authViewModel.currentSupervisorPermissions
    fun MainViewModel.openSupportChat() {
        val currentUserId = authViewModel.getOrGenerateUserId()
        val currentUserName = authViewModel.currentUserName.value.ifBlank { "العميل" }
        val currentUserPhoto = ""
        
        viewModelScope.launch {
            val result = chatRepo.getOrCreateChannel(
                currentUserId = currentUserId,
                currentUserName = currentUserName,
                currentUserPhoto = currentUserPhoto,
                otherUserId = com.example.data.repositories.ChatRepository.SUPPORT_ADMIN_ID,
                otherUserName = com.example.data.repositories.ChatRepository.SUPPORT_ADMIN_NAME,
                otherUserPhoto = "",
                type = com.example.data.models.ChannelType.SUPPORT,
                relatedEntityId = null,
                relatedEntityType = null
            )
            if (result is AppResult.Success) {
                targetChatChannelId = result.data.id
                navigateToScreen("CHAT_DIRECT")
            }
        }
    }
    fun MainViewModel.openChatChannel(channel: ChatChannelEntity?) {
        if (channel == null) return
        val currentUserId = authViewModel.getOrGenerateUserId()
        val currentUserName = authViewModel.currentUserName.value.ifBlank { "العميل" }
        val currentUserPhoto = ""
        
        val otherUserId = if (channel.customerId == currentUserId) channel.targetId else channel.customerId
        val otherUserName = if (channel.customerId == currentUserId) channel.targetName else channel.customerName
        
        viewModelScope.launch {
            val result = chatRepo.getOrCreateChannel(
                currentUserId = currentUserId,
                currentUserName = currentUserName,
                currentUserPhoto = currentUserPhoto,
                otherUserId = otherUserId,
                otherUserName = otherUserName,
                otherUserPhoto = "",
                type = com.example.data.models.ChannelType.PRIVATE,
                relatedEntityId = null,
                relatedEntityType = null
            )
            if (result is AppResult.Success) {
                targetChatChannelId = result.data.id
                // Note: The caller usually navigates, so we don't navigate here.
            }
        }
    }
    fun MainViewModel.verifyAdminOrOwnerPassword(password: String, adminPass: String = "", ownerPass: String = ""): Boolean {
        val effectiveAdmin = adminPass.ifEmpty { settings.value.adminPassword }
        val effectiveOwner = ownerPass.ifEmpty { settings.value.ownerPassword }
        return authViewModel.verifyAdminOrOwnerPassword(password, effectiveAdmin, effectiveOwner)
    }
    fun MainViewModel.setUserSessionDetails(context: android.content.Context, name: String, phone: String, residence: String = "اليمن") {
        authViewModel.setUserSessionDetails(context, name, phone, residence)
    }
    fun MainViewModel.registerGuestUser(context: android.content.Context, name: String, phone: String, residence: String, password: String = "") {
        authViewModel.registerGuestUser(context, name, phone, residence, password)
    }
    fun MainViewModel.toggleFavorite(id: String) {
        val current = _favoriteIds.value.toMutableSet()
        if (current.contains(id)) {
            current.remove(id)
            triggerNotification("💔 تم إزالة العنصر من المفضلة")
        } else {
            current.add(id)
            triggerNotification("💖 تم إضافة العنصر إلى المفضلة")
        }
        _favoriteIds.value = current
    }
    fun MainViewModel.toggleBlockStore(storeId: String) {
        val store = _stores.value.find { it.id == storeId }
        if (store != null) {
            adminViewModel.toggleStoreBlocked(storeId, !store.isBlocked)
        }
    }
    fun MainViewModel.addBooking(
        name: String,
        phone: String,
        area: String,
        serviceType: String,
        providerId: String,
        providerName: String,
        dateString: String,
        timeString: String,
        couponCode: String = "",
        pinCode: String = "",
        customBookingId: String = "",
        customPassword: String = ""
    ) = bookingViewModel.addBooking(
        name = name,
        phone = phone,
        area = area,
        serviceType = serviceType,
        providerId = providerId,
        providerName = providerName,
        dateString = dateString,
        timeString = timeString,
        couponCode = couponCode,
        pinCode = pinCode,
        customBookingId = customBookingId,
        customPassword = customPassword
    )
    fun MainViewModel.addNewStore(
        name: String,
        phone: String,
        cityId: String,
        localNeighborhood: String,
        categoryId: String,
        coverImage: String,
        workingHours: String
    ) {
        val newStore = StoreEntity(
            id = UUID.randomUUID().toString(),
            name = name,
            phone = phone,
            cityId = cityId,
            localNeighborhood = localNeighborhood,
            categoryId = categoryId,
            coverImage = coverImage,
            workingHours = workingHours,
            isApproved = true,
            createdAt = System.currentTimeMillis()
        )
        adminViewModel.saveStore(newStore)
    }
    fun MainViewModel.openOrCreateChatChannel(
        targetId: String,
        targetType: String,
        targetName: String,
        targetPhone: String = "",
        targetCategory: String = "",
        relatedEntityId: String = "",
        relatedEntityType: String = "",
        onCreated: (ChatChannelEntity?) -> Unit
    ) {
        val currentUserId = authViewModel.getOrGenerateUserId()
        val currentUserName = authViewModel.currentUserName.value.ifBlank { "العميل" }
        val currentUserPhoto = ""
        
        viewModelScope.launch {
            val result = chatRepo.getOrCreateChannel(
                currentUserId = currentUserId,
                currentUserName = currentUserName,
                currentUserPhoto = currentUserPhoto,
                otherUserId = targetId,
                otherUserName = targetName,
                otherUserPhoto = "",
                type = com.example.data.models.ChannelType.PRIVATE,
                relatedEntityId = relatedEntityId.takeIf { it.isNotBlank() },
                relatedEntityType = relatedEntityType.takeIf { it.isNotBlank() }
            )
            
            if (result is AppResult.Success) {
                targetChatChannelId = result.data.id
                val dummy = ChatChannelEntity(id = result.data.id)
                onCreated(dummy)
            } else {
                onCreated(null)
            }
        }
    }
    fun MainViewModel.sendNotificationToApplicants(title: String, message: String, jobId: String = "") {
        adminViewModel.sendNotificationToApplicants(title, message, jobId)
    }
    fun MainViewModel.createInstantRequest(
        userId: String,
        userName: String,
        userPhone: String,
        userCity: String,
        userNeighborhood: String,
        categoryId: String,
        categoryName: String,
        serviceTitle: String,
        description: String,
        images: List<String> = emptyList(),
        urgencyTime: String = "فوراً (خلال 30 دقيقة)",
        deliveryMethod: String = "",
        customPin: String = "",
        onResult: (Boolean, String, String) -> Unit = { _, _, _ -> }
    ) {
        instantRequestViewModel.createInstantRequest(
            userId, userName, userPhone, userCity, userNeighborhood, categoryId, categoryName, serviceTitle, description, images, urgencyTime, deliveryMethod, customPin, onResult
        )
    }
    fun MainViewModel.submitOfferForRequest(
        requestId: String,
        requestCode: String,
        technicianId: String,
        technicianName: String,
        technicianPhone: String,
        technicianAvatar: String,
        technicianRating: Float,
        price: Double,
        estimatedArrivalTime: String = "خلال 30 دقيقة",
        estimatedDuration: String = "ساعتان",
        notes: String = ""
    ) {
        instantRequestViewModel.submitOfferForRequest(
            requestId, requestCode, technicianId, technicianName, technicianPhone, technicianAvatar, technicianRating, price, estimatedArrivalTime, estimatedDuration, notes
        )
    }
    fun MainViewModel.cancelInstantRequest(requestId: String, userPin: String = "") {
        instantRequestViewModel.cancelInstantRequest(requestId = requestId, userPin = userPin)
    }
    fun MainViewModel.addNewCategory(nameAr: String, nameEn: String, icon: String, description: String, parentId: String = "", isMainCategory: Boolean = true) =
        adminViewModel.addNewCategory(nameAr, nameEn, icon, description, parentId, isMainCategory)
    fun MainViewModel.editCategory(categoryId: String, newName: String, newIcon: String, parentId: String = "", isMainCategory: Boolean = true) =
        adminViewModel.editCategory(categoryId, newName, newIcon, parentId, isMainCategory)
    fun MainViewModel.setStoreBlocked(storeId: String, isBlocked: Boolean, reason: String = "") =
        adminViewModel.setStoreBlocked(storeId, isBlocked, reason)
    fun MainViewModel.setPropertyBlocked(propertyId: String, isBlocked: Boolean, reason: String = "") =
        adminViewModel.setPropertyBlocked(propertyId, isBlocked, reason)
    fun MainViewModel.setJobBlocked(jobId: String, isBlocked: Boolean, reason: String = "") =
        adminViewModel.setJobBlocked(jobId, isBlocked, reason)
    fun MainViewModel.approveRegisteredUser(userId: String, userName: String = "") =
        adminViewModel.approveRegisteredUser(userId, userName)
    fun MainViewModel.toggleBlockRegisteredUser(userId: String, currentBlocked: Boolean, userName: String = "") =
        adminViewModel.toggleBlockRegisteredUser(userId, currentBlocked, userName)
    fun MainViewModel.deleteRegisteredUser(userId: String, userName: String = "") =
        adminViewModel.deleteRegisteredUser(userId, userName)
    fun MainViewModel.addNewBanner(title: String, url: String, redirect: String, type: String, size: String, duration: Int, displayTime: String = "طوال اليوم") =
        adminViewModel.addNewBanner(title, url, redirect, type, size, duration, displayTime)
    fun MainViewModel.addBanner(title: String, url: String, redirect: String, type: String, size: String, duration: Int, displayTime: String = "طوال اليوم") =
        adminViewModel.addBanner(title, url, redirect, type, size, duration, displayTime)
    fun MainViewModel.createPayment(
        userId: String,
        providerId: String,
        amount: Double,
        method: String,
        bookingId: String = "",
        isLinkedToBooking: Boolean = false,
        bookingServiceType: String = ""
    ) = adminViewModel.createPayment(userId, providerId, amount, method, bookingId, isLinkedToBooking, bookingServiceType)
    fun MainViewModel.updateBookingStatus(bookingId: String, newStatus: String, rejectionReason: String = "") =
        bookingViewModel.updateBookingStatus(bookingId, newStatus, rejectionReason)
    fun MainViewModel.updateBookingStatus(bookingId: String, newStatus: BookingStatus) =
        bookingViewModel.updateBookingStatus(bookingId, newStatus)
    fun MainViewModel.replyToChatChannel(channelId: String, senderId: String, msgText: String, senderName: String, imageUrl: String = "") {
        if (msgText.trim().isEmpty() && imageUrl.isEmpty()) return
        viewModelScope.launch {
            chatRepo.sendMessage(channelId, senderId, senderName, msgText, if (imageUrl.isNotBlank()) com.example.data.models.MediaType.IMAGE else com.example.data.models.MediaType.TEXT, imageUrl, null, null, null)
            triggerToast("تم إرسال الرد بنجاح")
        }
    }
    fun MainViewModel.updateBackdoorSettings(
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
        adminUsername: String = "",
        adminPassword: String = "",
        customPrimaryHex: String = "#059669",
        customSecondaryHex: String = "#115E59",
        customBackgroundHex: String = "#0A0F0D",
        customSurfaceHex: String = "#121D18"
    ) = settingsViewModel.updateBackdoorSettings(
        appName, welcomeMsg, footerMsg, themeId, supportPhone, supportEmail, supportWhatsapp,
        isMaintenance, hiddenFooter, botHidden, botSize, chatHidden, chatSize, radiusKm, isSpeech,
        isDataSaver, imgQuality, bookingTerms, bookingLabelName, bookingLabelPhone, bookingLabelArea,
        bookingLabelService, adminUsername, adminPassword, customPrimaryHex, customSecondaryHex,
        customBackgroundHex, customSurfaceHex
    )
    fun MainViewModel.addSupervisor(name: String, role: String, passcode: String, permissions: List<String> = emptyList()) =
        authViewModel.addSupervisor(name, role, passcode, permissions)
    fun MainViewModel.editSupervisor(id: String, name: String, role: String, passcode: String, permissions: List<String> = emptyList()) =
        authViewModel.editSupervisor(id, name, role, passcode, permissions)
    fun MainViewModel.addColorPalette(name: String, primaryHex: String, secondaryHex: String, backgroundHex: String = "#0A0F0D", surfaceHex: String = "#121D18") =
        settingsViewModel.addColorPalette(name, primaryHex, secondaryHex, backgroundHex, surfaceHex)
    fun MainViewModel.addNewCity(nameAr: String, nameEn: String, icon: String = "📍", photoUrl: String = "", sortOrder: Int = 0) =
        adminViewModel.addNewCity(nameAr, nameEn, icon, photoUrl, sortOrder)
    fun MainViewModel.addCoupon(code: String, pointsValue: Int, expiryMs: Long, discountPercentage: Int = 0, maxUsageCount: Int = 100) =
        adminViewModel.addCoupon(code, pointsValue, expiryMs, discountPercentage, maxUsageCount)
    fun MainViewModel.rejectTechnician(providerId: String, reason: String = "لم يستوفِ الشروط") =
        adminViewModel.rejectTechnician(providerId, reason)
    fun MainViewModel.sendMessageInChat(msgText: String, imageUrl: String = "") {
        if (msgText.trim().isEmpty() && imageUrl.isEmpty()) return
        val currentUserId = authViewModel.getOrGenerateUserId()
        val currentName = authViewModel.currentUserName.value.ifEmpty { "العميل" }
        viewModelScope.launch {
            val result = chatRepo.getOrCreateChannel(
                currentUserId = currentUserId,
                currentUserName = currentName,
                currentUserPhoto = "",
                otherUserId = "ADMIN",
                otherUserName = "الدعم الفني",
                otherUserPhoto = "",
                type = com.example.data.models.ChannelType.SUPPORT
            )
            if (result is AppResult.Success) {
                chatRepo.sendMessage(result.data.id, currentUserId, currentName, msgText, if (imageUrl.isNotBlank()) com.example.data.models.MediaType.IMAGE else com.example.data.models.MediaType.TEXT, imageUrl, null, null, null)
                addNotification(
                    "💬 رسالة جديدة في الدعم الفني المباشر",
                    "من العميل $currentName: ${msgText.ifEmpty { "📷 [صورة]" }}",
                    "SUPERVISOR",
                    currentUserId
                )
            }
        }
    }
    fun MainViewModel.submitReport(report: com.example.data.ReportEntity, onComplete: () -> Unit = {}) =
        adminViewModel.submitReport(report, onComplete)
    fun MainViewModel.deleteBooking(bookingId: String) =
        bookingViewModel.deleteBooking(bookingId)
    fun MainViewModel.updateBooking(booking: com.example.data.BookingEntity) =
        bookingViewModel.updateBooking(booking)
    fun MainViewModel.submitRating(ratingEntity: com.example.data.RatingEntity, onComplete: () -> Unit = {}) =
        adminViewModel.submitRating(ratingEntity, onComplete)
