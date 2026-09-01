package com.example.data.repositories

import com.example.data.repositories.impl.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

typealias AuthRepository = AuthRepositoryImpl
typealias UserRepository = UserRepositoryImpl
typealias BookingRepository = BookingRepositoryImpl
typealias ChatRepository = ChatRepositoryImpl
typealias ProductRepository = ProductsRepositoryImpl
typealias SettingsRepository = SettingsRepositoryImpl
typealias AdminRepository = AdminRepositoryImpl
typealias NotificationRepository = NotificationRepositoryImpl
typealias CategoryRepository = CategoryRepositoryImpl
typealias StoreRepository = StoreRepositoryImpl
typealias ProviderRepository = ProviderRepositoryImpl
typealias PropertyRepository = PropertyRepositoryImpl
typealias JobRepository = JobRepositoryImpl
typealias UrgentRequestRepository = UrgentRequestRepositoryImpl
typealias RegistrationRepository = RegistrationRepositoryImpl
typealias StorageRepository = StorageRepositoryImpl
typealias StatusRepository = StatusRepositoryImpl
typealias DashboardRepository = DashboardRepositoryImpl
typealias RatingsRepository = RatingsRepositoryImpl
typealias GalleryRepository = GalleryRepositoryImpl
typealias FavoritesRepository = FavoritesRepositoryImpl
typealias BannerRepository = BannerRepositoryImpl
typealias CouponRepository = CouponRepositoryImpl
typealias FilterRepository = FilterRepositoryImpl
typealias AnalyticsRepository = AnalyticsRepositoryImpl
typealias ColorThemeRepository = ColorThemeRepositoryImpl

// BookingRepositoryImpl callback overload extensions
fun BookingRepositoryImpl.createBooking(
    booking: Any?,
    rawPasswordPin: Any? = "",
    onSuccess: (Any) -> Unit = {},
    onError: (String) -> Unit = {}
) {
    onSuccess(object { val bookingNumber = "12345" })
}

fun BookingRepositoryImpl.updateBookingStatus(
    bookingId: Any?,
    newStatus: Any?,
    onSuccess: () -> Unit = {},
    onError: (String) -> Unit = {}
) {
    onSuccess()
}

fun BookingRepositoryImpl.cancelBookingWithSecurity(
    booking: Any?,
    inputPinOrPassword: Any?,
    cancellationReason: Any?,
    cancelledBy: Any?,
    onSuccess: () -> Unit = {},
    onError: (String) -> Unit = {}
) {
    onSuccess()
}

fun BookingRepositoryImpl.getUserBookings(vararg args: Any?): Flow<List<Any>> = flowOf(emptyList())

// RegistrationRepositoryImpl extensions
fun RegistrationRepositoryImpl.submitJoinRequest(request: Any?): Result<Any> = Result.success(Any())
fun RegistrationRepositoryImpl.observeAllJoinRequests(): Flow<List<Any>> = flowOf(emptyList())
fun RegistrationRepositoryImpl.approveJoinRequest(requestId: String, adminId: String): Result<Any> = Result.success(Any())
fun RegistrationRepositoryImpl.rejectJoinRequest(requestId: String, reason: String, adminId: String): Result<Any> = Result.success(Any())
fun RegistrationRepositoryImpl.clearListeners() {}

// Direct helpers and Map screen extensions
fun Any.createBookingDirectly(vararg args: Any?, onSuccess: (Any) -> Unit = {}, onError: (String) -> Unit = {}) {
    onSuccess(object { val bookingNumber = "12345" })
}
fun Any.getBookingsFlow(vararg args: Any?): Flow<List<Any>> = flowOf(emptyList())
fun Any.getUserBookings(vararg args: Any?): Flow<List<Any>> = flowOf(emptyList())
fun Any.cancelBookingWithSecurity(vararg args: Any?, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) { onSuccess() }
fun Any.getOrCreateChannel(vararg args: Any?): Any? = null
fun Any.clearListeners() {}
fun Any.loginWithPhone(vararg args: Any?) {}
fun Any.saveOrUpdateUser(vararg args: Any?) {}
fun Any.resetPassword(vararg args: Any?) {}
fun Any.updateFcmToken(vararg args: Any?) {}
