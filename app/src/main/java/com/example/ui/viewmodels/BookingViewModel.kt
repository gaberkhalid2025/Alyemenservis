package com.example.ui.viewmodels

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.example.data.BookingEntity
import com.example.data.repositories.BookingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class BookingStatus(val label: String, val color: String) {
    PENDING("قيد الانتظار", "#FFC107"),
    ACCEPTED("مقبول", "#4CAF50"),
    IN_PROGRESS("قيد التنفيذ", "#2196F3"),
    COMPLETED("مكتمل", "#9C27B0"),
    CANCELLED("ملغي", "#F44336")
}

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

open class BookingViewModel : BaseViewModel() {

    internal val _bookings = MutableStateFlow<List<BookingEntity>>(emptyList())
    val bookings: StateFlow<List<BookingEntity>> = _bookings.asStateFlow()

    internal val _createBookingStatus = MutableStateFlow<Result<BookingEntity>?>(null)
    val createBookingStatus: StateFlow<Result<BookingEntity>?> = _createBookingStatus.asStateFlow()

    internal val _bookingFormFields = MutableStateFlow(BookingFormFields())
    val bookingFormFields: StateFlow<BookingFormFields> = _bookingFormFields.asStateFlow()

    internal val _distributionMode = MutableStateFlow(BookingDistributionMode.ADMIN_ONLY)
    val distributionMode: StateFlow<BookingDistributionMode> = _distributionMode.asStateFlow()

    val bookingRepository by lazy {
        BookingRepository(appContext ?: throw IllegalStateException("App context not initialized"))
    }

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

    fun updateBookingFormFields(fields: BookingFormFields) {
        _bookingFormFields.value = fields
        try {
            db.collection("settings").document("booking_fields").set(fields)
        } catch (e: Exception) {}
    }

    fun updateDistributionMode(mode: BookingDistributionMode) {
        _distributionMode.value = mode
        try {
            db.collection("settings").document("distribution_mode").set(mapOf("mode" to mode.name))
        } catch (e: Exception) {}
    }

    fun updateBookingStatus(bookingId: String, newStatus: BookingStatus) {
        val b = _bookings.value.find { it.id == bookingId }
        _bookings.value = _bookings.value.map { booking ->
            if (booking.id == bookingId) {
                booking.copy(status = newStatus.name)
            } else booking
        }
        try {
            db.collection("bookings").document(bookingId).update("status", newStatus.name).addOnSuccessListener {
                if (b != null) {
                    val statusText = when(newStatus) {
                        BookingStatus.ACCEPTED -> "تم قبول وتأكيد حجزك بنجاح! 🟢"
                        BookingStatus.IN_PROGRESS -> "جاري تنفيذ حجزك الآن! ⚡"
                        BookingStatus.COMPLETED -> "تم إكمال خدمتك بنجاح! 🎉"
                        BookingStatus.CANCELLED -> "تم إلغاء الحجز ❌"
                        else -> "تحديث حالة الحجز إلى: ${newStatus.label}"
                    }
                    val targetPhone = b.customerPhone.ifEmpty { b.clientPhone }
                    if (targetPhone.isNotEmpty()) {
                        triggerToast("📢 تحديث حالة الحجز: $statusText")
                    }
                }
            }
        } catch (e: Exception) {}
    }

    fun updateBookingStatus(bookingId: String, newStatus: String, rejectionReason: String = "") {
        db.collection("bookings").document(bookingId).get().addOnSuccessListener { snapshot ->
            val b = snapshot.toObject(BookingEntity::class.java)
            if (b != null) {
                val updated = b.copy(status = newStatus, rejectionReason = rejectionReason)
                db.collection("bookings").document(bookingId).set(updated)
            }
        }
        val toastMsg = when(newStatus) {
            "APPROVED", "ACCEPTED", "IN_PROGRESS" -> "⚡ تم قبول وتأكيد الحجز بنجاح"
            "PENDING", "UNDER_REVIEW" -> "⏳ تم وضع الحجز قيد المراجعة"
            "REJECTED" -> "❌ تم رفض الحجز وإلغائه"
            "COMPLETED" -> "🎉 تم إكمال الخدمة بنجاح وتوثيق الإنجاز"
            else -> "تم تحديث حالة الحجز بنجاح"
        }
        triggerToast(toastMsg)
    }

    fun deleteBooking(bookingId: String) {
        _bookings.value = _bookings.value.filter { it.id != bookingId }
        db.collection("bookings").document(bookingId).delete()
        triggerToast("🗑️ تم حذف الحجز من السجلات")
    }

    fun deleteAllBookings(customerPhone: String) {
        _bookings.value = _bookings.value.filter { it.customerPhone != customerPhone }
        db.collection("bookings")
            .whereEqualTo("customerPhone", customerPhone)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val batch = db.batch()
                for (doc in querySnapshot.documents) {
                    batch.delete(doc.reference)
                }
                batch.commit().addOnSuccessListener {
                    triggerToast("🗑️ تم تصفية وحذف سجل جميع الحجوزات بنجاح.")
                }
            }
    }

    fun updateBooking(booking: BookingEntity) {
        db.collection("bookings").document(booking.id).set(booking)
        _bookings.value = _bookings.value.map { if (it.id == booking.id) booking else it }
        triggerToast("💾 تم تحديث بيانات الحجز بنجاح")
    }

    fun cancelBookingByUser(bookingId: String) {
        val b = _bookings.value.find { it.id == bookingId }
        _bookings.value = _bookings.value.map { booking ->
            if (booking.id == bookingId) {
                booking.copy(status = "CANCELLED")
            } else booking
        }
        try {
            db.collection("bookings").document(bookingId).update("status", "CANCELLED")
                .addOnSuccessListener {
                    triggerToast("✅ تم إلغاء الحجز بنجاح")
                }
        } catch (e: Exception) {}
    }

    fun attemptCancelBooking(bookingId: String, input: String, reason: String = "ملغي بطلب العميل", onResult: (Boolean, String) -> Unit) {
        val b = _bookings.value.find { it.id == bookingId }
        if (b == null) {
            onResult(false, "الحجز غير موجود")
            return
        }
        cancelBookingByUser(bookingId)
        onResult(true, "تم الإلغاء بنجاح")
    }

    fun cancelBookingByTechnician(bookingId: String, reason: String, onComplete: () -> Unit = {}) {
        cancelBookingByUser(bookingId)
        onComplete()
    }

    fun cancelBookingByAdmin(bookingId: String, reason: String, onComplete: () -> Unit = {}) {
        cancelBookingByUser(bookingId)
        onComplete()
    }

    fun getBookingStatusColor(status: String): String {
        return when (status.uppercase()) {
            "PENDING", "UNDER_REVIEW" -> "#FFC107"
            "ACCEPTED", "APPROVED" -> "#4CAF50"
            "IN_PROGRESS" -> "#2196F3"
            "COMPLETED", "PAID" -> "#9C27B0"
            "CANCELLED", "REJECTED" -> "#F44336"
            else -> "#757575"
        }
    }

    fun getBookingStatusLabel(status: String): String {
        return when (status.uppercase()) {
            "PENDING" -> "قيد الانتظار"
            "UNDER_REVIEW" -> "قيد المراجعة"
            "ACCEPTED", "APPROVED" -> "مقبول"
            "IN_PROGRESS" -> "جاري التنفيذ"
            "COMPLETED" -> "مكتمل"
            "PAID" -> "مدفوع"
            "CANCELLED" -> "ملغي"
            "REJECTED" -> "مرفوض"
            else -> status
        }
    }

    fun getBookingProgress(status: String): Float {
        return when (status.uppercase()) {
            "PENDING", "UNDER_REVIEW" -> 0.2f
            "ACCEPTED", "APPROVED" -> 0.5f
            "IN_PROGRESS" -> 0.8f
            "COMPLETED", "PAID" -> 1.0f
            else -> 0.0f
        }
    }

    fun editBookingByUser(bookingId: String, newDate: String, newTime: String, newServiceType: String, providerId: String = "", providerName: String = "") {
        val targetProviderId = providerId.ifEmpty {
            _bookings.value.find { it.id == bookingId }?.providerId ?: ""
        }
        val isTimeSlotTaken = _bookings.value.any {
            it.id != bookingId &&
            it.providerId == targetProviderId &&
            it.dateString.trim() == newDate.trim() &&
            it.timeString.trim() == newTime.trim() &&
            (it.status == "PENDING" || it.status == "APPROVED" || it.status == "IN_PROGRESS")
        }
        if (isTimeSlotTaken) {
            triggerToast("❌ عذراً! هذا الوقت (${newTime}) وتاريخ (${newDate}) محجوز بالفعل لدى مقدم الخدمة. يرجى اختيار موعد آخر.")
            return
        }

        val updates = mutableMapOf<String, Any>(
            "dateString" to newDate,
            "timeString" to newTime,
            "serviceType" to newServiceType,
            "updatedAt" to System.currentTimeMillis()
        )
        if (providerId.isNotEmpty()) {
            updates["providerId"] = providerId
        }
        if (providerName.isNotEmpty()) {
            updates["providerName"] = providerName
        }
        db.collection("bookings").document(bookingId).update(updates)
            .addOnSuccessListener {
                triggerToast("✅ تم تعديل الحجز بنجاح!")
            }
    }
}
