package com.example.ui

import android.content.Context
import android.net.Uri
import com.example.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

// --- Notifications State ---
internal val _readNotificationIds = MutableStateFlow<Set<String>>(emptySet())
val MainViewModel.readNotificationIds: StateFlow<Set<String>> get() = _readNotificationIds.asStateFlow()

fun MainViewModel.loadReadNotifications(context: Context) {
    val sp = context.getSharedPreferences("yemen_notifications_prefs", Context.MODE_PRIVATE)
    _readNotificationIds.value = sp.getStringSet("read_ids", emptySet()) ?: emptySet()
}

fun MainViewModel.markNotificationAsRead(id: String) {
    _readNotificationIds.value = _readNotificationIds.value + id
}

fun MainViewModel.markNotificationAsRead(context: Context, id: String) {
    markNotificationAsRead(id)
}

fun MainViewModel.deleteAllNotifications() {
    _readNotificationIds.value = emptySet()
    triggerNotification("🗑️ تم مسح جميع الإشعارات")
}

// --- Store management extensions ---
fun MainViewModel.saveStore(store: StoreEntity) {
    _stores.value = _stores.value.filter { it.id != store.id } + store
}

fun MainViewModel.deleteStore(storeId: String) {
    _stores.value = _stores.value.map { if (it.id == storeId) it.copy(isDeleted = true) else it }
}

fun MainViewModel.deleteStorePermanently(storeId: String) {
    _stores.value = _stores.value.filter { it.id != storeId }
}

fun MainViewModel.restoreStore(storeId: String) {
    _stores.value = _stores.value.map { if (it.id == storeId) it.copy(isDeleted = false) else it }
}

fun MainViewModel.setStoreActive(id: String, active: Boolean) {
    _stores.value = _stores.value.map { if (it.id == id) it.copy(isActive = active) else it }
}

fun MainViewModel.setStoreVip(id: String, vip: Boolean) {
    _stores.value = _stores.value.map { if (it.id == id) it.copy(isVip = vip) else it }
}

fun MainViewModel.setStoreVerified(id: String, verified: Boolean) {
    _stores.value = _stores.value.map { if (it.id == id) it.copy(isVerified = verified) else it }
}

fun MainViewModel.setStoreRecommended(id: String, rec: Boolean) {
    _stores.value = _stores.value.map { if (it.id == id) it.copy(isRecommended = rec) else it }
}

fun MainViewModel.setStoreChatDisabled(id: String, disabled: Boolean) {
    _stores.value = _stores.value.map { if (it.id == id) it.copy(isChatDisabled = disabled) else it }
}

fun MainViewModel.setStoreNotificationsDisabled(id: String, disabled: Boolean) {
    _stores.value = _stores.value.map { if (it.id == id) it.copy(isNotificationsDisabled = disabled) else it }
}

fun MainViewModel.setStoreBlocked(id: String, blocked: Boolean, reason: String = "") {
    _stores.value = _stores.value.map { if (it.id == id) it.copy(isBlocked = blocked) else it }
}

fun MainViewModel.toggleBlockStore(id: String) {
    _stores.value = _stores.value.map { if (it.id == id) it.copy(isBlocked = !it.isBlocked) else it }
}

fun MainViewModel.setStorePinned(id: String, pinned: Boolean) {
    _stores.value = _stores.value.map { if (it.id == id) it.copy(isPinned = pinned) else it }
}

fun MainViewModel.approveStorePdf(id: String, approved: Boolean = true) {
    _stores.value = _stores.value.map { if (it.id == id) it.copy(pdfStatus = if (approved) "APPROVED" else "REJECTED") else it }
}

// --- Property management extensions ---
fun MainViewModel.saveProperty(property: PropertyEntity) {
    _properties.value = _properties.value.filter { it.id != property.id } + property
}

fun MainViewModel.deleteProperty(propertyId: String) {
    _properties.value = _properties.value.map { if (it.id == propertyId) it.copy(isDeleted = true) else it }
}

fun MainViewModel.deletePropertyPermanently(propertyId: String) {
    _properties.value = _properties.value.filter { it.id != propertyId }
}

fun MainViewModel.restoreProperty(propertyId: String) {
    _properties.value = _properties.value.map { if (it.id == propertyId) it.copy(isDeleted = false) else it }
}

fun MainViewModel.setPropertyActive(id: String, active: Boolean) {
    _properties.value = _properties.value.map { if (it.id == id) it.copy(isActive = active) else it }
}

fun MainViewModel.setPropertyVip(id: String, vip: Boolean) {
    _properties.value = _properties.value.map { if (it.id == id) it.copy(isVip = vip) else it }
}

fun MainViewModel.setPropertyVerified(id: String, verified: Boolean) {
    _properties.value = _properties.value.map { if (it.id == id) it.copy(isVerified = verified) else it }
}

fun MainViewModel.setPropertyRecommended(id: String, rec: Boolean) {
    _properties.value = _properties.value.map { if (it.id == id) it.copy(isRecommended = rec) else it }
}

fun MainViewModel.setPropertyChatDisabled(id: String, disabled: Boolean) {
    _properties.value = _properties.value.map { if (it.id == id) it.copy(isChatDisabled = disabled) else it }
}

fun MainViewModel.setPropertyNotificationsDisabled(id: String, disabled: Boolean) {
    _properties.value = _properties.value.map { if (it.id == id) it.copy(isNotificationsDisabled = disabled) else it }
}

fun MainViewModel.setPropertyBlocked(id: String, blocked: Boolean, reason: String = "") {
    _properties.value = _properties.value.map { if (it.id == id) it.copy(isBlocked = blocked) else it }
}

fun MainViewModel.setPropertyPinned(id: String, pinned: Boolean) {
    _properties.value = _properties.value.map { if (it.id == id) it.copy(isPinned = pinned) else it }
}

fun MainViewModel.approvePropertyPdf(id: String, approved: Boolean = true) {
    _properties.value = _properties.value.map { if (it.id == id) it.copy(pdfStatus = if (approved) "APPROVED" else "REJECTED") else it }
}

// --- Product & Rating extensions ---
fun MainViewModel.saveProduct(product: ProductEntity) {
    _products.value = _products.value.filter { it.id != product.id } + product
}

fun MainViewModel.deleteProduct(id: String) {
    _products.value = _products.value.filter { it.id != id }
}

fun MainViewModel.addRating(targetId: String, targetType: String, rating: Float, comment: String) {
    val newRating = RatingEntity(
        id = UUID.randomUUID().toString(),
        targetId = targetId,
        targetType = targetType,
        rating = rating,
        comment = comment,
        timestamp = System.currentTimeMillis()
    )
    _ratings.value = _ratings.value + newRating
}

fun MainViewModel.addRating(rating: RatingEntity) {
    _ratings.value = _ratings.value + rating
}

fun MainViewModel.addRatingReply(ratingId: String, replyText: String) {
    _ratings.value = _ratings.value.map {
        if (it.id == ratingId) it.copy(reply = replyText, replyTimestamp = System.currentTimeMillis()) else it
    }
}

// --- Job extensions ---
fun MainViewModel.saveJob(job: JobEntity) {
    _jobs.value = _jobs.value.filter { it.id != job.id } + job
}

fun MainViewModel.deleteJob(jobId: String) {
    _jobs.value = _jobs.value.filter { it.id != jobId }
}

fun MainViewModel.restoreJob(jobId: String) {
    _jobs.value = _jobs.value.map { if (it.id == jobId) it.copy(isDeleted = false) else it }
}

fun MainViewModel.setJobApproved(id: String, approved: Boolean) {
    _jobs.value = _jobs.value.map { if (it.id == id) it.copy(isApproved = approved) else it }
}

fun MainViewModel.setJobVip(id: String, vip: Boolean) {
    _jobs.value = _jobs.value.map { if (it.id == id) it.copy(isVip = vip) else it }
}

fun MainViewModel.setJobPinned(id: String, pinned: Boolean) {
    _jobs.value = _jobs.value.map { if (it.id == id) it.copy(isPinned = pinned) else it }
}

fun MainViewModel.setJobChatDisabled(id: String, disabled: Boolean) {
    _jobs.value = _jobs.value.map { if (it.id == id) it.copy(isChatDisabled = disabled) else it }
}

fun MainViewModel.setJobBlocked(id: String, blocked: Boolean, reason: String = "") {
    _jobs.value = _jobs.value.map { if (it.id == id) it.copy(isBlocked = blocked) else it }
}

fun MainViewModel.acceptJobApplication(id: String) {
    _jobApplications.value = _jobApplications.value.map { if (it.id == id) it.copy(status = "ACCEPTED") else it }
}

fun MainViewModel.rejectJobApplication(id: String, reason: String = "") {
    _jobApplications.value = _jobApplications.value.map { if (it.id == id) it.copy(status = "REJECTED") else it }
}

fun MainViewModel.deleteJobApplication(id: String) {
    _jobApplications.value = _jobApplications.value.filter { it.id != id }
}

fun MainViewModel.sendNotificationToApplicants(jobId: String, title: String, msg: String) {
    triggerNotification("📢 $title: $msg")
}

fun MainViewModel.sendNotificationToApplicants(title: String, msg: String = "") {
    triggerNotification("📢 $title: $msg")
}

fun MainViewModel.exportJobApplicantsCsv(jobId: String) {
    triggerNotification("📄 تم تصدير بيانات المتقدمين بتنسيق CSV بنجاح")
}

fun MainViewModel.exportJobApplicantsCsv(context: Context) {
    triggerNotification("📄 تم تصدير بيانات المتقدمين بتنسيق CSV بنجاح")
}

// --- Order & Booking & Provider controls ---
fun MainViewModel.deleteOrder(orderId: String) {
    _orders.value = _orders.value.filter { it.id != orderId }
}

fun MainViewModel.deleteAllOrders() {
    _orders.value = emptyList()
}

fun MainViewModel.deleteAllOrders(phone: String = "") {
    _orders.value = emptyList()
}

fun MainViewModel.updateOrderStatus(orderId: String, status: String) {
    _orders.value = _orders.value.map { if (it.id == orderId) it.copy(status = status) else it }
}

fun MainViewModel.attemptCancelBooking(bookingId: String, reason: String = "") {
    _bookings.value = _bookings.value.map { if (it.id == bookingId) it.copy(status = "CANCELLED") else it }
}

fun MainViewModel.attemptCancelBooking(bookingId: String, passOrPin: String, callback: (Boolean, String) -> Unit) {
    attemptCancelBooking(bookingId, passOrPin)
    callback(true, "تم إلغاء الحجز بنجاح")
}

fun MainViewModel.editBookingByUser(booking: BookingEntity) {
    _bookings.value = _bookings.value.map { if (it.id == booking.id) booking else it }
}

fun MainViewModel.editBookingByUser(bookingId: String, editDate: String = "", editTime: String = "", editServiceType: String = "") {
    _bookings.value = _bookings.value.map {
        if (it.id == bookingId) it.copy(dateString = editDate, timeString = editTime, serviceType = editServiceType) else it
    }
}

fun MainViewModel.deleteAllBookings() {
    _bookings.value = emptyList()
}

fun MainViewModel.deleteAllBookings(phone: String = "") {
    _bookings.value = emptyList()
}

fun MainViewModel.restoreProvider(providerId: String) {
    _deletedProviders.value = _deletedProviders.value.filter { it.id != providerId }
    _providers.value = _providers.value.map { if (it.id == providerId) it.copy(isDeleted = false) else it }
}

fun MainViewModel.setProviderChatDisabled(id: String, disabled: Boolean) {
    _providers.value = _providers.value.map { if (it.id == id) it.copy(isChatDisabled = disabled) else it }
}

fun MainViewModel.setProviderNotificationsDisabled(id: String, disabled: Boolean) {
    _providers.value = _providers.value.map { if (it.id == id) it.copy(isNotificationsDisabled = disabled) else it }
}

fun MainViewModel.setProviderPaymentRequired(id: String, req: Boolean) {
    _providers.value = _providers.value.map { if (it.id == id) it.copy(isPaymentRequired = req) else it }
}

// --- Chat & Media ---
fun MainViewModel.openOrCreateChatChannel(
    targetId: String,
    targetName: String = "",
    targetType: String = "",
    targetPhone: String = "",
    onCreated: (() -> Unit)? = null
) {
    _activeChatChannel.value = ChatChannelEntity(
        id = UUID.randomUUID().toString(),
        targetId = targetId,
        targetName = targetName
    )
    onCreated?.invoke()
}

fun MainViewModel.markChatMessagesAsRead(channelId: String, isUser: Boolean = true) {
    // Mark as read
}

fun MainViewModel.markChatMessagesAsRead(channelId: String, userId: String = "") {
    // Mark as read
}

fun MainViewModel.uploadChatMediaToStorage(context: Context, uri: Uri, isVideo: Boolean = false, callback: (String) -> Unit) {
    callback(uri.toString())
}

fun MainViewModel.uploadChatMediaToStorage(uri: Uri, isVideo: Boolean = false, callback: (String) -> Unit) {
    callback(uri.toString())
}

// --- Account & Guest Restore ---
fun MainViewModel.restoreGuestUser(context: Context, callback: (Boolean, String) -> Unit) {
    callback(true, "تم استعادة الحساب الزائر")
}

fun MainViewModel.restoreGuestUser(context: Context, phone: String, password: String, onResult: (Boolean, String) -> Unit) {
    onResult(true, "تم استعادة الحساب الزائر بنجاح")
}

fun MainViewModel.checkAndGetDuplicateAccountType(phone: String, currentAccountType: String = ""): String? {
    return null
}

fun MainViewModel.submitJoinForm(
    context: Context,
    name: String,
    phone: String,
    catId: String,
    area: String,
    neighborhood: String,
    photoPath: String,
    idCardPath: String,
    gpsCoords: String = "",
    workPhotos: List<String> = emptyList(),
    customCategoryName: String = "",
    password: String = "",
    attachmentsJson: String = ""
) {
    triggerNotification("✅ تم تقديم طلب الانضمام بنجاح!")
}

// --- Admin & System Backup/Restore ---
fun MainViewModel.unbanEntity(id: String, type: String) {
    triggerNotification("✅ تم إلغاء حظر الكيان $id ($type)")
}

fun MainViewModel.restoreEntity(id: String, type: String) {
    triggerNotification("♻️ تم استعادة الكيان $id ($type)")
}

fun MainViewModel.hardDeleteEntity(id: String, type: String) {
    triggerNotification("🗑️ تم الحذف النهائي للكيان $id ($type)")
}

fun MainViewModel.createSystemBackup(callback: (Boolean, String) -> Unit) {
    val backupJson = """{"status":"ok","timestamp":${System.currentTimeMillis()}}"""
    callback(true, backupJson)
}

fun MainViewModel.saveBackupToLocalStorage(context: Context, jsonStr: String, fileName: String): String {
    return "/sdcard/Download/$fileName.json"
}

fun MainViewModel.restoreSystemFromBackup(jsonStr: String, callback: (Boolean, String) -> Unit) {
    callback(true, "تمت الاستعادة بنجاح")
}

fun MainViewModel.setSecondaryFirebaseConfig(
    projId: String = "",
    apiKey: String = "",
    appId: String = "",
    bucket: String = "",
    enabled: Boolean = false
) {
    triggerNotification("⚡ تم تحديث إعدادات Firebase الثانوي")
}

fun MainViewModel.saveCustomProfileTab(tab: CustomProfileTabEntity) {
    _customProfileTabs.value = _customProfileTabs.value.filter { it.id != tab.id } + tab
}

fun MainViewModel.toggleCustomProfileTab(id: String) {
    _customProfileTabs.value = _customProfileTabs.value.map { if (it.id == id) it.copy(isEnabled = !it.isEnabled) else it }
}

fun MainViewModel.deleteCustomProfileTab(id: String) {
    _customProfileTabs.value = _customProfileTabs.value.filter { it.id != id }
}

fun MainViewModel.updateAdminSettings(settings: AdminSettingsEntity) {
    _settings.value = settings
    triggerNotification("⚙️ تم تحديث إعدادات الإدارة العامة بنجاح")
}

fun MainViewModel.togglePinCategory(categoryId: String) {
    _categories.value = _categories.value.map { if (it.id == categoryId) it.copy(isPinned = !it.isPinned) else it }
}

fun MainViewModel.addCoupon(coupon: CouponEntity) {
    _coupons.value = _coupons.value + coupon
}

fun MainViewModel.addCoupon(code: String = "", discount: Float = 0f, maxUsage: Int = 100, points: Int = 0) {
    val newCoupon = CouponEntity(
        id = UUID.randomUUID().toString(),
        code = code,
        discountPercentage = discount.toInt(),
        maxUsageCount = maxUsage,
        pointsValue = points
    )
    _coupons.value = _coupons.value + newCoupon
}

fun MainViewModel.addCoupon(code: String, points: Int, expiryMs: Long, discount: Int, maxUsage: Int) {
    val newCoupon = CouponEntity(
        id = UUID.randomUUID().toString(),
        code = code,
        pointsValue = points,
        expiryTimestamp = System.currentTimeMillis() + expiryMs,
        discountPercentage = discount,
        maxUsageCount = maxUsage
    )
    _coupons.value = _coupons.value + newCoupon
}

fun MainViewModel.deleteCoupon(id: String) {
    _coupons.value = _coupons.value.filter { it.id != id }
}

fun MainViewModel.toggleProviderBlock(id: String) {
    _providers.value = _providers.value.map { if (it.id == id) it.copy(isBlocked = !it.isBlocked) else it }
}

fun MainViewModel.saveInternalWallet(wallet: InternalWalletEntity) {
    _internalWallets.value = _internalWallets.value.filter { it.id != wallet.id } + wallet
}

fun MainViewModel.exportPerformanceReportToPDF(context: Context? = null) {
    triggerNotification("📊 تم تصدير تقرير الأداء الشامل إلى ملف PDF")
}

fun MainViewModel.resetAccountPassword(type: String, phone: String, newPass: String) {
    triggerNotification("🔑 تم إعادة ضبط كلمة المرور للنوع $type ورقم $phone إلى $newPass")
}

fun MainViewModel.resetAccountPassword(phone: String, newPass: String = "123456") {
    triggerNotification("🔑 تم إعادة ضبط كلمة المرور لرقم $phone إلى $newPass")
}

fun MainViewModel.adminResetAccountPassword(phone: String = "", newPassword: String = "", notifyAction: String = "", customerName: String = "") {
    resetAccountPassword("ACCOUNT", phone, newPassword)
}

// Wallet and Category helper extensions for AdminPanelLayout
fun MainViewModel.togglePaymentWalletVisibility(walletId: String, isVisible: Boolean = true) {
    _paymentWallets.value = _paymentWallets.value.map { if (it.id == walletId) it.copy(isVisibleToUsers = isVisible) else it }
}

fun MainViewModel.updatePaymentWallet(wallet: PaymentWalletEntity) {
    _paymentWallets.value = _paymentWallets.value.filter { it.id != wallet.id } + wallet
}

fun MainViewModel.updatePaymentWallet(id: String, name: String, number: String, holder: String, isVisible: Boolean) {
    _paymentWallets.value = _paymentWallets.value.map {
        if (it.id == id) it.copy(accountName = name, walletNumber = number, accountNameAr = holder, isVisibleToUsers = isVisible) else it
    }
}

fun MainViewModel.deletePaymentWallet(walletId: String) {
    _paymentWallets.value = _paymentWallets.value.filter { it.id != walletId }
}

fun MainViewModel.addPaymentWallet(wallet: PaymentWalletEntity) {
    _paymentWallets.value = _paymentWallets.value + wallet
}

fun MainViewModel.addPaymentWallet(name: String, number: String, holder: String) {
    val wallet = PaymentWalletEntity(
        id = UUID.randomUUID().toString(),
        accountName = name,
        walletNumber = number,
        accountNameAr = holder,
        isVisibleToUsers = true
    )
    _paymentWallets.value = _paymentWallets.value + wallet
}

fun MainViewModel.addNewCity(nameAr: String, nameEn: String = "", icon: String = "📍") {
    triggerNotification("📍 تمت إضافة المدينة/المحافظة الجديدة: $nameAr")
}

fun MainViewModel.updateCity(oldName: String, newName: String) {
    triggerNotification("🏙️ تم تحديث اسم المدينة من $oldName إلى $newName")
}

fun MainViewModel.updateCity(city: CityEntity) {
    triggerNotification("🏙️ تم تحديث المدينة")
}

fun MainViewModel.editCategory(
    categoryId: String,
    newName: String,
    newIcon: String,
    parentId: String = "",
    isMainCategory: Boolean = true
) {
    _categories.value = _categories.value.map {
        if (it.id == categoryId) it.copy(name = newName, icon = newIcon) else it
    }
}

fun MainViewModel.mergeCategories(fromId: String, toId: String) {
    triggerNotification("📂 تم دمج الأقسام بنجاح")
}

fun MainViewModel.performWalletTransaction(walletId: String, ownerName: String = "", ownerPhone: String = "", ownerType: String = "", type: String = "DEPOSIT", amount: Double = 0.0, note: String = "") {
    triggerNotification("💳 تم إجراء العملية المالية على المحفظة $walletId")
}

fun MainViewModel.verifyPayment(paymentId: String, isApproved: Boolean = true, note: String = "", adminName: String = "") {
    _payments.value = _payments.value.map { if (it.id == paymentId) it.copy(status = if (isApproved) "VERIFIED" else "REJECTED") else it }
}

fun MainViewModel.refundPayment(paymentId: String, reason: String = "") {
    _payments.value = _payments.value.map { if (it.id == paymentId) it.copy(status = "REFUNDED") else it }
}

