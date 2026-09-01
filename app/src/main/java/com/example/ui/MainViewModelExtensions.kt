package com.example.ui

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

val MainViewModel.currentSupervisorPermissions: StateFlow<List<String>> get() = MutableStateFlow(emptyList())
val MainViewModel.selectedCategoryId: StateFlow<String?> get() = MutableStateFlow(null)
val MainViewModel.searchQuery: StateFlow<String> get() = MutableStateFlow("")
val MainViewModel.filterVipOnly: StateFlow<Boolean> get() = MutableStateFlow(false)
val MainViewModel.filterAvailableOnly: StateFlow<Boolean> get() = MutableStateFlow(false)
val MainViewModel.filterCityId: StateFlow<String?> get() = MutableStateFlow(null)
val MainViewModel.maxKmRadius: StateFlow<Float> get() = MutableStateFlow(50f)
val MainViewModel.filterNeighborhoodName: StateFlow<String?> get() = MutableStateFlow(null)
val MainViewModel.phoneOrNameFilter: StateFlow<String> get() = MutableStateFlow("")
val MainViewModel.joinRequestPhone: StateFlow<String> get() = MutableStateFlow("")
val MainViewModel.readNotificationIds: StateFlow<List<String>> get() = MutableStateFlow(emptyList())
val MainViewModel.isProviderUser: StateFlow<Boolean> get() = MutableStateFlow(false)

fun MainViewModel.toggleStoreBlocked(vararg args: Any?) {}
fun MainViewModel.setStoreBlocked(vararg args: Any?) {}
fun MainViewModel.setPropertyBlocked(vararg args: Any?) {}
fun MainViewModel.setJobBlocked(vararg args: Any?) {}
fun MainViewModel.createPayment(vararg args: Any?) {}
fun MainViewModel.deleteCoupon(vararg args: Any?) {}
fun MainViewModel.saveCoupon(vararg args: Any?) {}
fun MainViewModel.restoreGuestUser(phone: Any? = null, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) { onSuccess() }
fun MainViewModel.requestAdminPasswordReset(vararg args: Any?) {}
fun MainViewModel.cancelOrResetJoinRequest(vararg args: Any?) {}
fun MainViewModel.uploadImageStringOrUri(uri: Any? = null, onComplete: (String) -> Unit = {}) { onComplete("") }
fun MainViewModel.deleteProduct(vararg args: Any?) {}
fun MainViewModel.addRatingReply(vararg args: Any?) {}
fun MainViewModel.resetAccountPassword(vararg args: Any?) {}
fun MainViewModel.updateJobApplicationStatus(vararg args: Any?) {}
fun MainViewModel.deleteOrder(vararg args: Any?) {}
fun MainViewModel.deleteAllOrders(vararg args: Any?) {}
fun MainViewModel.replyToChatChannel(vararg args: Any?) {}
fun MainViewModel.submitJoinForm(vararg args: Any?) {}
fun MainViewModel.resetRegistrationState(vararg args: Any?) {}
fun MainViewModel.loadReadNotifications(vararg args: Any?) {}
fun MainViewModel.markNotificationAsRead(vararg args: Any?) {}

fun MainViewModel.updateSearchQuery(query: String) {}
fun MainViewModel.setCityFilter(cityId: String?) {}
fun MainViewModel.toggleVipFilter(enabled: Boolean) {}
fun MainViewModel.toggleAvailableFilter(enabled: Boolean) {}
fun MainViewModel.setRadiusKm(radius: Float) {}
fun MainViewModel.setNeighborhoodFilter(name: String?) {}

fun MainViewModel.createBookingDirectly(
    provider: Any? = null,
    notes: Any? = null,
    onSuccess: () -> Unit = {},
    onError: (String) -> Unit = {}
) {
    onSuccess()
}

fun MainViewModel.addNewStore(
    name: Any? = null,
    phone: Any? = null,
    catId: Any? = null,
    street: Any? = null,
    cityId: Any? = null,
    profileImage: Any? = null,
    idCardImage: Any? = null,
    forensicImage: Any? = null,
    price: Any? = null,
    isVip: Any? = null,
    vararg args: Any?
) {}

fun MainViewModel.addNewCategory(
    nameAr: Any? = null,
    nameEn: Any? = null,
    icon: Any? = null,
    description: Any? = null,
    parentId: Any? = null,
    isMainCategory: Any? = null,
    vararg args: Any?
) {}

fun MainViewModel.editCategory(
    categoryId: Any? = null,
    newName: Any? = null,
    newIcon: Any? = null,
    parentId: Any? = null,
    isMainCategory: Any? = null,
    vararg args: Any?
) {}

fun MainViewModel.addBanner(
    title: Any? = null,
    url: Any? = null,
    redirect: Any? = null,
    type: Any? = null,
    size: Any? = null,
    duration: Any? = null,
    banner: Any? = null,
    vararg args: Any?
) {}

fun MainViewModel.sendNotification(
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
    customerName: Any? = null,
    vararg args: Any?
) {}

fun MainViewModel.updateAdminSettings(
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
    imgQuality: Any? = null,
    vararg args: Any?
) {}

fun MainViewModel.performWalletTransaction(
    walletId: Any? = null,
    ownerName: Any? = null,
    ownerPhone: Any? = null,
    ownerType: Any? = null,
    type: Any? = null,
    amount: Any? = null,
    note: Any? = null,
    vararg args: Any?
) {}

fun MainViewModel.adminResetAccountPassword(
    phone: Any? = null,
    newPassword: Any? = null,
    notifyAction: Any? = null,
    customerName: Any? = null,
    vararg args: Any?
) {}
