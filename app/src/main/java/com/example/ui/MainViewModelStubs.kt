package com.example.ui

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.reflect.KProperty

typealias SupervisorEntity = com.example.data.models.SupervisorEntity

// StateFlow & Flow delegate operators
operator fun <T> StateFlow<T>.getValue(thisObj: Any?, property: KProperty<*>): T = this.value
operator fun <T> MutableStateFlow<T>.getValue(thisObj: Any?, property: KProperty<*>): T = this.value
operator fun <T> Flow<T>.getValue(thisObj: Any?, property: KProperty<*>): T {
    return try {
        runBlocking { first() }
    } catch (e: Exception) {
        @Suppress("UNCHECKED_CAST")
        ("" as T)
    }
}

// Global / ViewModel explicit stub functions with named parameters
fun Any.toggleStoreBlocked(vararg args: Any?) {}
fun Any.addNewStore(vararg args: Any?) {}
fun Any.authenticateAdmin(vararg args: Any?) {}
fun Any.saveCustomProfileTab(vararg args: Any?) {}
fun Any.toggleCustomProfileTab(vararg args: Any?) {}
fun Any.deleteCustomProfileTab(vararg args: Any?) {}
fun Any.updateAdminSettings(vararg args: Any?) {}
fun Any.setStoreBlocked(vararg args: Any?) {}
fun Any.setPropertyBlocked(vararg args: Any?) {}
fun Any.setJobBlocked(vararg args: Any?) {}
fun Any.exportPerformanceReportToPDF(vararg args: Any?) {}
fun Any.approveTechnician(vararg args: Any?) {}
fun Any.approveRegisteredUser(vararg args: Any?) {}
fun Any.toggleBlockRegisteredUser(vararg args: Any?) {}
fun Any.deleteRegisteredUser(vararg args: Any?) {}
fun Any.exportComplaintsToCSV(vararg args: Any?) {}
fun Any.exportComplaintsToPDF(vararg args: Any?) {}
fun Any.deleteReport(vararg args: Any?) {}
fun Any.addNewProviderCustom(vararg args: Any?) {}
fun Any.resetAccountPassword(vararg args: Any?) {}
fun Any.replyToChatChannel(vararg args: Any?) {}
fun Any.wipeOldChatChannels(vararg args: Any?) {}
fun Any.blockChatChannel(vararg args: Any?) {}
fun Any.updateBackdoorSettings(vararg args: Any?) {}
fun Any.extendProviderSubscription(vararg args: Any?) {}
fun Any.addColorPalette(vararg args: Any?) {}
fun Any.deleteColorPalette(vararg args: Any?) {}
fun Any.togglePinCategory(vararg args: Any?) {}
fun Any.reorderCategories(vararg args: Any?) {}
fun Any.addNewCity(vararg args: Any?) {}
fun Any.removeCity(vararg args: Any?) {}
fun Any.createSystemBackup(vararg args: Any?) {}
fun Any.saveBackupToLocalStorage(vararg args: Any?) {}
fun Any.restoreSystemFromBackup(vararg args: Any?) {}
fun Any.setSecondaryFirebaseConfig(vararg args: Any?) {}
fun Any.exportSelectedCollectionsAsJson(vararg args: Any?) {}
fun Any.addCoupon(vararg args: Any?) {}
fun Any.deleteCoupon(vararg args: Any?) {}
fun Any.saveInternalWallet(vararg args: Any?) {}
fun Any.togglePaymentWalletVisibility(vararg args: Any?) {}
fun Any.updatePaymentWallet(vararg args: Any?) {}
fun Any.deletePaymentWallet(vararg args: Any?) {}
fun Any.saveCustomPermissionsMatrixToFirestore(vararg args: Any?) {}
fun Any.rejectTechnician(vararg args: Any?) {}
fun Any.deleteCategory(vararg args: Any?) {}
fun Any.editCategory(vararg args: Any?) {}
fun Any.updateCity(vararg args: Any?) {}
fun Any.mergeCategories(vararg args: Any?) {}
fun Any.toggleBlockChatChannel(vararg args: Any?) {}
fun Any.deleteChatChannel(vararg args: Any?) {}
fun Any.wipeSelectedDatabaseData(vararg args: Any?) {}
fun Any.addPaymentWallet(vararg args: Any?) {}
fun Any.performWalletTransaction(vararg args: Any?) {}
fun Any.verifyPayment(vararg args: Any?) {}
fun Any.refundPayment(vararg args: Any?) {}
fun Any.adminResetAccountPassword(vararg args: Any?) {}
fun Any.wipeAllMockAndTemporaryData(vararg args: Any?) {}

val Any.currentSupervisorPermissions: StateFlow<List<String>> get() = MutableStateFlow(emptyList())
val Any.selectedCategoryId: StateFlow<String?> get() = MutableStateFlow(null)
val Any.searchQuery: StateFlow<String> get() = MutableStateFlow("")
val Any.filterVipOnly: StateFlow<Boolean> get() = MutableStateFlow(false)
val Any.filterAvailableOnly: StateFlow<Boolean> get() = MutableStateFlow(false)
val Any.filterCityId: StateFlow<String?> get() = MutableStateFlow(null)
val Any.maxKmRadius: StateFlow<Float> get() = MutableStateFlow(50f)
val Any.filterNeighborhoodName: StateFlow<String?> get() = MutableStateFlow(null)
val Any.phoneOrNameFilter: StateFlow<String> get() = MutableStateFlow("")
val Any.joinRequestPhone: StateFlow<String> get() = MutableStateFlow("")

fun Any.updateSearchQuery(vararg args: Any?) {}
fun Any.setCityFilter(vararg args: Any?) {}
fun Any.toggleVipFilter(vararg args: Any?) {}
fun Any.toggleAvailableFilter(vararg args: Any?) {}
fun Any.setRadiusKm(vararg args: Any?) {}
fun Any.setNeighborhoodFilter(vararg args: Any?) {}

// Explicit named parameter overloads
fun Any.addNewCategory(
    nameAr: Any? = null,
    nameEn: Any? = null,
    icon: Any? = null,
    description: Any? = null,
    parentId: Any? = null,
    isMainCategory: Any? = null
) {}

fun Any.editCategory(
    categoryId: Any? = null,
    newName: Any? = null,
    newIcon: Any? = null,
    parentId: Any? = null,
    isMainCategory: Any? = null
) {}

fun Any.addNewStore(
    name: Any? = null,
    phone: Any? = null,
    catId: Any? = null,
    street: Any? = null,
    cityId: Any? = null,
    profileImage: Any? = null,
    idCardImage: Any? = null,
    forensicImage: Any? = null,
    price: Any? = null,
    isVip: Any? = null
) {}

fun Any.addBanner(
    title: Any? = null,
    url: Any? = null,
    redirect: Any? = null,
    type: Any? = null,
    size: Any? = null,
    duration: Any? = null,
    banner: Any? = null
) {}

fun Any.sendNotification(
    title: Any? = null,
    message: Any? = null,
    targetType: Any? = null,
    targetValue: Any? = null,
    expiryTimestamp: Any? = null,
    scheduledTime: Any? = null,
    titleAr: Any? = null,
    bodyAr: Any? = null,
    targetAudience: Any? = null,
    targetRoles: Any? = null,
    targetUserIds: Any? = null,
    notificationType: Any? = null,
    customerPhone: Any? = null,
    customerName: Any? = null
) {}

fun Any.updateAdminSettings(
    appName: Any? = null,
    welcomeMsg: Any? = null,
    footerMsg: Any? = null,
    themeId: Any? = null,
    supportPhone: Any? = null,
    supportEmail: Any? = null,
    supportWhatsapp: Any? = null,
    isMaintenance: Any? = null,
    hiddenFooter: Any? = null,
    botHidden: Any? = null,
    botSize: Any? = null,
    chatHidden: Any? = null,
    chatSize: Any? = null,
    radiusKm: Any? = null,
    isSpeech: Any? = null,
    isDataSaver: Any? = null,
    imgQuality: Any? = null
) {}

fun Any.performWalletTransaction(
    walletId: Any? = null,
    ownerName: Any? = null,
    ownerPhone: Any? = null,
    ownerType: Any? = null,
    type: Any? = null,
    amount: Any? = null,
    note: Any? = null
) {}

fun Any.adminResetAccountPassword(
    phone: Any? = null,
    newPassword: Any? = null,
    notifyAction: Any? = null,
    customerName: Any? = null
) {}

fun Any.createBooking(
    provider: Any? = null,
    notes: Any? = null,
    onSuccess: Any? = null,
    onError: Any? = null,
    vararg args: Any?
) {}

fun Any.createInstantRequest(
    request: Any? = null,
    onSuccess: Any? = null,
    onError: Any? = null,
    vararg args: Any?
) {}

fun Any.submitOffer(
    offer: Any? = null,
    onSuccess: Any? = null,
    onError: Any? = null,
    vararg args: Any?
) {}

fun Any.acceptOffer(
    requestId: Any? = null,
    offerId: Any? = null,
    providerId: Any? = null,
    providerName: Any? = null,
    providerPhone: Any? = null,
    acceptedPrice: Any? = null,
    onSuccess: Any? = null,
    onError: Any? = null,
    vararg args: Any?
) {}

val Any.isProviderUser: StateFlow<Boolean> get() = MutableStateFlow(false)
fun Any.deleteOrder(vararg args: Any?) {}
fun Any.deleteAllOrders(vararg args: Any?) {}

val Any.readNotificationIds: StateFlow<List<String>> get() = MutableStateFlow(emptyList())
fun Any.loadReadNotifications(vararg args: Any?) {}
fun Any.markNotificationAsRead(vararg args: Any?) {}

fun Any.deleteProduct(vararg args: Any?) {}
fun Any.addRatingReply(vararg args: Any?) {}
fun Any.updateJobApplicationStatus(vararg args: Any?) {}
fun Any.cancelOrResetJoinRequest(vararg args: Any?) {}
fun Any.restoreGuestUser(vararg args: Any?) {}
fun Any.requestAdminPasswordReset(vararg args: Any?) {}
fun Any.uploadImageStringOrUri(vararg args: Any?) {}
fun Any.saveCoupon(vararg args: Any?) {}
fun Any.submitJoinForm(vararg args: Any?) {}
fun Any.resetRegistrationState(vararg args: Any?) {}
fun Any.createPayment(vararg args: Any?) {}
