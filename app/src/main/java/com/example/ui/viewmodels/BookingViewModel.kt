package com.example.ui.viewmodels

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.example.data.BookingEntity
import com.example.data.ChatChannelEntity
import com.example.data.CouponEntity
import com.example.data.ProviderEntity
import com.example.data.repositories.BookingRepository
import com.example.ui.helpers.AppState
import com.example.utils.BookingStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 📌 Architectural Note: UI-focused BookingStatus enum providing UI labels and hex colors
 * for state rendering in Compose views.
 */
data class BookingFormFields(
    val tripleName: Boolean = true,
    val phoneNumber: Boolean = true,
    val serviceType: Boolean = true,
    val residenceArea: Boolean = true,
    val preferredTime: Boolean = true,
    val description: Boolean = false,
    val tripleNameRequired: Boolean = true,
    val phoneNumberRequired: Boolean = true,
    val serviceTypeRequired: Boolean = true,
    val residenceAreaRequired: Boolean = true,
    val preferredTimeRequired: Boolean = true,
    val descriptionRequired: Boolean = false
)

enum class BookingDistributionMode(val label: String) {
    CATEGORY_SUPERVISOR("لمشرف القسم أولاً"),
    NEAREST_PROVIDER("لأقرب فني جغرافياً"),
    ALL_PROVIDERS("لكل فنيي القسم"),
    SPECIFIC_PROVIDER("لفني محدد مسبقاً"),
    ADMIN_ONLY("للأدمن أولاً")
}

open class BookingViewModel @Inject constructor(
    private val injectedRepository: BookingRepository,
    val appState: AppState
) : BaseViewModel() {

    internal val _bookings get() = appState._bookings
    val bookings: StateFlow<List<BookingEntity>> = _bookings.asStateFlow()

    internal val _createBookingStatus = MutableStateFlow<Result<BookingEntity>?>(null)
    val createBookingStatus: StateFlow<Result<BookingEntity>?> = _createBookingStatus.asStateFlow()

    internal val _bookingFormFields get() = appState._bookingFormFields
    val bookingFormFields: StateFlow<BookingFormFields> = _bookingFormFields.asStateFlow()

    internal val _distributionMode get() = appState._distributionMode
    val distributionMode: StateFlow<BookingDistributionMode> = _distributionMode.asStateFlow()

    val bookingRepository: BookingRepository
        get() = injectedRepository

    // --- Callback/Lambda Properties for decoupling ---
    var getCoupons: (() -> List<CouponEntity>)? = null
    var getProviders: (() -> List<ProviderEntity>)? = null
    var getCurrentUserPhone: (() -> String)? = null
    var getCurrentUserName: (() -> String)? = null
    var getCurrentUserResidence: (() -> String)? = null
    var setCurrentUserPhone: ((String) -> Unit)? = null
    var setCurrentUserName: ((String) -> Unit)? = null
    var setCurrentUserResidence: ((String) -> Unit)? = null
    var onAddNotification: ((title: String, message: String, targetType: String, targetValue: String) -> Unit)? = null
    var triggerNotificationCallback: ((String) -> Unit)? = null
    var onOpenOrCreateChatChannel: ((targetId: String, targetType: String, targetName: String, targetPhone: String, targetCategory: String, relatedEntityId: String, relatedEntityType: String, onComplete: (ChatChannelEntity?) -> Unit) -> Unit)? = null

    internal fun safeBookingFirestoreCall(
        operation: (onSuccess: () -> Unit, onFailure: (Exception) -> Unit) -> Unit,
        onSuccess: () -> Unit = {},
        onError: (Exception) -> Unit = {},
        errorMessage: String = "حدث خطأ أثناء تنفيذ العملية"
    ) = safeFirestoreCallWithCallback(operation, onSuccess, onError, errorMessage)

    fun createBooking(context: Context, booking: BookingEntity, rawPasswordPin: String = "") {
        appContext = context.applicationContext
        viewModelScope.launch {
            bookingRepository.createBooking(
                booking = booking,
                rawPasswordPin = rawPasswordPin,
                onSuccess = { createdBooking ->
                    _createBookingStatus.value = Result.success(createdBooking)
                    triggerToast("🎉 تم إنشاء حجزك بنجاح برقم: ${createdBooking.bookingNumber}")
                },
                onError = { errorMsg ->
                    _createBookingStatus.value = Result.failure(Exception(errorMsg))
                    triggerToast("❌ فشل إنشاء الحجز: $errorMsg")
                }
            )
        }
    }

    fun createBooking(booking: BookingEntity, onResult: (Boolean) -> Unit = {}) =
        createBookingHelper(booking, onResult)

    fun updateBookingFormFields(fields: BookingFormFields) {
        _bookingFormFields.value = fields
        try {
            db.collection("settings").document("booking_fields").set(fields)
        } catch (_: Exception) {}
    }

    fun updateDistributionMode(mode: BookingDistributionMode) {
        _distributionMode.value = mode
        try {
            db.collection("settings").document("distribution_mode").set(mapOf("mode" to mode.name))
        } catch (_: Exception) {}
    }

    fun addBooking(
        name: String,
        phone: String,
        area: String,
        serviceType: String,
        providerId: String,
        providerName: String,
        dateString: String = "2026-06-20",
        timeString: String = "12:00 م",
        couponCode: String = "",
        pinCode: String = "",
        customBookingId: String = "",
        customPassword: String = ""
    ) = addBookingImpl(name, phone, area, serviceType, providerId, providerName, dateString, timeString, couponCode, pinCode, customBookingId, customPassword)

    fun addBookingImpl(
        name: String,
        phone: String,
        area: String,
        serviceType: String,
        providerId: String,
        providerName: String,
        dateString: String = "2026-06-20",
        timeString: String = "12:00 م",
        couponCode: String = "",
        pinCode: String = "",
        customBookingId: String = "",
        customPassword: String = ""
    ) = addBookingImplHelper(name, phone, area, serviceType, providerId, providerName, dateString, timeString, couponCode, pinCode, customBookingId, customPassword)

    fun updateBookingStatus(bookingId: String, newStatus: BookingStatus) =
        updateBookingStatusImpl(bookingId, newStatus.name)

    fun updateBookingStatus(bookingId: String, newStatus: String, rejectionReason: String = "") =
        updateBookingStatusImpl(bookingId, newStatus, rejectionReason)

    fun updateBookingStatusImpl(bookingId: String, newStatus: String, rejectionReason: String = "") =
        updateBookingStatusImplHelper(bookingId, newStatus, rejectionReason)

    fun deleteBooking(bookingId: String) = deleteBookingImpl(bookingId)

    fun deleteBookingImpl(bookingId: String) = deleteBookingImplHelper(bookingId)

    fun deleteAllBookings(customerPhone: String) = deleteAllBookingsImpl(customerPhone)

    fun deleteAllBookingsImpl(customerPhone: String) = deleteAllBookingsImplHelper(customerPhone)

    fun updateBooking(booking: BookingEntity) = updateBookingImpl(booking)

    fun updateBookingImpl(booking: BookingEntity) = updateBookingImplHelper(booking)

    fun attemptCancelBooking(bookingId: String, input: String, reason: String = "ملغي بطلب العميل", cancelledByParam: String = "USER", onResult: (Boolean, String) -> Unit) =
        attemptCancelBookingImpl(bookingId, input, reason, cancelledByParam, onResult)

    fun attemptCancelBookingImpl(bookingId: String, input: String, reason: String = "ملغي بطلب العميل", cancelledByParam: String = "USER", onResult: (Boolean, String) -> Unit) =
        attemptCancelBookingImplHelper(bookingId, input, reason, cancelledByParam, onResult)

    fun editBookingByUser(bookingId: String, newDate: String, newTime: String, newServiceType: String, providerId: String = "", providerName: String = "") =
        editBookingByUserHelper(bookingId, newDate, newTime, newServiceType, providerId, providerName)

    fun getBookingStatusColor(status: String): String {
        return when (status.uppercase()) {
            "PENDING", "UNDER_REVIEW" -> "#FF9800"
            "APPROVED", "ACCEPTED" -> "#2196F3"
            "IN_PROGRESS" -> "#9C27B0"
            "COMPLETED" -> "#4CAF50"
            "CANCELLED", "REJECTED" -> "#F44336"
            else -> "#757575"
        }
    }

    fun getBookingStatusLabel(status: String): String {
        return when (status.uppercase()) {
            "PENDING", "UNDER_REVIEW" -> "قيد الانتظار"
            "APPROVED", "ACCEPTED" -> "مقبول"
            "IN_PROGRESS" -> "جاري التنفيذ"
            "COMPLETED" -> "مكتمل"
            "CANCELLED" -> "ملغي"
            "REJECTED" -> "مرفوض"
            else -> "غير معروف"
        }
    }

    fun getBookingProgress(status: String): Float {
        return when (status.uppercase()) {
            "PENDING", "UNDER_REVIEW" -> 0.25f
            "APPROVED", "ACCEPTED" -> 0.50f
            "IN_PROGRESS" -> 0.75f
            "COMPLETED", "CANCELLED", "REJECTED" -> 1.0f
            else -> 0.0f
        }
    }

    fun createBookingDirectly(
        provider: ProviderEntity,
        notes: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) = createBookingDirectlyHelper(provider, notes, onSuccess, onError)

    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

    fun updateBookingRelatedChatChannel(bookingId: String, channelId: String) {
        if (bookingId.isBlank() || channelId.isBlank()) return
        viewModelScope.launch {
            try {
                db.collection("bookings").document(bookingId).update("relatedChatChannelId", channelId)
            } catch (_: Exception) {}
        }
    }
}
