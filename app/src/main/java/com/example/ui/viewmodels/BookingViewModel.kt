package com.example.ui.viewmodels
import com.example.ui.helpers.AppState

import android.content.Context
import com.example.ui.*
import androidx.lifecycle.viewModelScope
import com.example.data.BookingEntity
import com.example.data.repositories.BookingRepository
import com.example.data.*
import com.example.data.models.*
import com.example.utils.*
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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

    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

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
    var getCoupons: (() -> List<com.example.data.CouponEntity>)? = null
    var getProviders: (() -> List<com.example.data.ProviderEntity>)? = null
    var getCurrentUserPhone: (() -> String)? = null
    var getCurrentUserName: (() -> String)? = null
    var getCurrentUserResidence: (() -> String)? = null
    var setCurrentUserPhone: ((String) -> Unit)? = null
    var setCurrentUserName: ((String) -> Unit)? = null
    var setCurrentUserResidence: ((String) -> Unit)? = null
    var onAddNotification: ((title: String, message: String, targetType: String, targetValue: String) -> Unit)? = null
    var triggerNotificationCallback: ((String) -> Unit)? = null
    var onOpenOrCreateChatChannel: ((targetId: String, targetType: String, targetName: String, targetPhone: String, targetCategory: String, relatedEntityId: String, relatedEntityType: String, onComplete: (com.example.data.ChatChannelEntity?) -> Unit) -> Unit)? = null

    fun createBooking(context: Context, booking: BookingEntity, rawPasswordPin: String = "") {
        appContext = context.applicationContext
        viewModelScope.launch {
            bookingRepository.createBooking(
                booking = booking,
                rawPasswordPin = rawPasswordPin,
                onSuccess = { createdBooking ->
                    _createBookingStatus.value = Result.success(createdBooking)
                    triggerToast("🎉 تم إنشاء حجزك بنجاح برقم: ${createdBooking.bookingNumber
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}")
                
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

},
                onError = { errorMsg ->
                    _createBookingStatus.value = Result.failure(Exception(errorMsg))
                    triggerToast("❌ فشل إنشاء الحجز: $errorMsg")
                
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}
            )
        
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}
    
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}

    fun createBooking(booking: BookingEntity, onResult: (Boolean) -> Unit = {
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}) {
        if (booking.isRecurring && booking.recurrenceRule != "NONE") {
            createRecurringBookings(booking, onResult)
            return
        
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}
        val bId = booking.id.ifEmpty { java.util.UUID.randomUUID().toString() 
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}
        val bNum = booking.bookingNumber.ifEmpty { "YEM-${(10000..99999).random()
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}" 
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}
        val bPass = booking.bookingPassword.ifEmpty { "${(1000..9999).random()
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}" 
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}
        val finalized = booking.copy(
            id = bId,
            bookingNumber = bNum,
            bookingPassword = "",
            pinCode = if (booking.pinCode.isNotBlank()) booking.pinCode else com.example.utils.SecureHasher.hashPin(bPass),
            createdAt = if (booking.createdAt == 0L) System.currentTimeMillis() else booking.createdAt,
            updatedAt = System.currentTimeMillis()
        )
        safeFirestoreCallWithCallback(
            operation = { onSuccess, onFailure ->
                db.collection("bookings").document(bId).set(finalized)
                    .addOnSuccessListener {
                        _bookings.value = _bookings.value + finalized
                        val custPhone = finalized.customerPhone.ifEmpty { finalized.clientPhone 
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}
                        val custName = finalized.customerName.ifEmpty { finalized.clientName.ifEmpty { "العميل" 
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

} 
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}
                        val provPhone = finalized.providerPhone.ifEmpty {
                            getProviders?.invoke()?.find { it.id == finalized.providerId || it.name.trim() == finalized.providerName.trim() 
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}?.phone?.trim() ?: finalized.providerId
                        
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}
                        
                        // 1. User notification
                        onAddNotification?.invoke(
                            "📅 حجز جديد رقم $bNum",
                            "تم تسجيل طلب حجز موعد لدى ${finalized.providerName
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

} بنجاح. رقم الحجز: $bNum ورمز المرور: $bPass.",
                            "USER",
                            custPhone
                        )
                        
                        // 2. Provider notification
                        if (provPhone.isNotBlank()) {
                            onAddNotification?.invoke(
                                "⚡ حجز جديد وارد رقم $bNum",
                                "العميل $custName ($custPhone) قام بحجز خدمة (${finalized.serviceType
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}) لديك.",
                                "PROVIDER",
                                provPhone
                            )
                        
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}
                        
                        // 3. Admin notification
                        onAddNotification?.invoke(
                            "📢 حجز جديد مسجل في النظام",
                            "نوع العملية: (إنشاء حجز) | رقم الحجز: $bNum | العميل: $custName ($custPhone) لدى: ${finalized.providerName
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}",
                            "ADMIN_ONLY",
                            ""
                        )
                        onSuccess()
                        onResult(true)
                    
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}
                    .addOnFailureListener { e ->
                        _bookings.value = _bookings.value + finalized
                        onFailure(e)
                        onResult(true)
                    
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}
            
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

},
            onSuccess = { triggerToast("✅ تم إنشاء الحجز بنجاح") 
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

},
            onError = { triggerToast("⚠️ تم حفظ الحجز محلياً، سيتم المزامنة تلقائياً") 
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

},
            errorMessage = "فشل إنشاء الحجز"
        )
    
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}

    private fun createRecurringBookings(baseBooking: BookingEntity, onResult: (Boolean) -> Unit) {
        val parentId = java.util.UUID.randomUUID().toString()
        val dates = com.example.utils.ScheduleManager.calculateRecurringDates(baseBooking.dateString, baseBooking.recurrenceRule)
        
        val batch = db.batch()
        val newBookings = mutableListOf<BookingEntity>()
        
        dates.forEachIndexed { index, dateStr ->
            val bId = "${parentId
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}_${index
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}"
            val bNum = "YEM-${(10000..99999).random()
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}"
            val bPass = "${(1000..9999).random()
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}"
            val b = baseBooking.copy(
                id = bId,
                parentId = parentId,
                date = dateStr,
                dateString = dateStr,
                bookingNumber = bNum,
                bookingPassword = "",
                pinCode = com.example.utils.SecureHasher.hashPin(bPass),
                isRecurring = true,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            newBookings.add(b)
            val docRef = db.collection("bookings").document(bId)
            batch.set(docRef, b)
        
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}
        
        safeFirestoreCallWithCallback(
            operation = { onSuccess, onFailure ->
                batch.commit().addOnSuccessListener {
                    _bookings.value = _bookings.value + newBookings
                    
                    val custPhone = baseBooking.customerPhone.ifEmpty { baseBooking.clientPhone 
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}
                    val custName = baseBooking.customerName.ifEmpty { baseBooking.clientName.ifEmpty { "العميل" 
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

} 
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}
                    val provPhone = baseBooking.providerPhone.ifEmpty {
                        getProviders?.invoke()?.find { it.id == baseBooking.providerId || it.name.trim() == baseBooking.providerName.trim() 
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}?.phone?.trim() ?: baseBooking.providerId
                    
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}
                    
                    // Notify provider about the batch
                    val msg = "لديك سلسلة حجوزات جديدة متكررة (${newBookings.size
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

} مواعيد) من $custName برقم $custPhone"
                    if (provPhone.isNotBlank()) {
                        onAddNotification?.invoke("سلسلة حجوزات جديدة", msg, "PROVIDER", provPhone)
                    
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}
                    
                    // Log to admin
                    val auditLog = mapOf(
                        "id" to java.util.UUID.randomUUID().toString(),
                        "action" to "CREATE_RECURRING_BOOKING",
                        "details" to "تم إنشاء ${newBookings.size
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

} حجوزات للمزود ${baseBooking.providerName
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}",
                        "timestamp" to System.currentTimeMillis()
                    )
                    db.collection("security_audit_logs").document(auditLog["id"].toString()).set(auditLog)
                    
                    onSuccess()
                    onResult(true)
                
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}.addOnFailureListener { e ->
                    onFailure(e)
                    onResult(false)
                
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}
            
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

},
            onSuccess = { triggerToast("✅ تم إنشاء السلسلة المتكررة بنجاح") 
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

},
            onError = { triggerToast("⚠️ خطأ في المزامنة، يرجى التحقق من اتصالك") 
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

},
            errorMessage = "فشل إنشاء الحجوزات المتكررة"
        )
    
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}

    fun updateBookingFormFields(fields: BookingFormFields) {
        _bookingFormFields.value = fields
        try {
            db.collection("settings").document("booking_fields").set(fields)
        
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

} catch (e: Exception) {
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}
    
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}

    fun updateDistributionMode(mode: BookingDistributionMode) {
        _distributionMode.value = mode
        try {
            db.collection("settings").document("distribution_mode").set(mapOf("mode" to mode.name))
        
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

} catch (e: Exception) {
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}
    
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

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
    ) {
        val cleanPhone = phone.trim()
        val cleanName = name.trim()
        
        // 1. Verification of identity of registered Yemeni user phone
        val isValidYemeniPhone = cleanPhone.length == 9 && (
            cleanPhone.startsWith("77") || 
            cleanPhone.startsWith("73") || 
            cleanPhone.startsWith("71") || 
            cleanPhone.startsWith("70") || 
            cleanPhone.startsWith("78")
        )
        if (!isValidYemeniPhone) {
            triggerNotificationCallback?.invoke("❌ الهوية غير مسجلة: رقم الهاتف يجب أن يكون يمنياً صحيحاً مفعلاً ومكوناً من 9 أرقام يبدأ بـ 77 أو 73 أو 71 أو 70!")
            return
        
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}

        // 2. Duplication & Overlap prevention scan
        val isTimeSlotTaken = _bookings.value.any {
            it.providerId == providerId &&
            it.dateString.trim() == dateString.trim() &&
            it.timeString.trim() == timeString.trim() &&
            (it.status == "PENDING" || it.status == "APPROVED" || it.status == "IN_PROGRESS")
        
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}
        if (isTimeSlotTaken) {
            triggerNotificationCallback?.invoke("⚠️ عذراً، هذا الموعد ($dateString في $timeString) محجوز مسبقاً لدى هذا الفني. يرجى اختيار وقت آخر!")
            return
        
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}

        // Determine specific pricing via admin settings or coupon logic
        var discountPercent = 0.0
        var finalPrice = 0.0

        if (couponCode.isNotBlank()) {
            val couponsList = getCoupons?.invoke() ?: emptyList()
            val coupon = couponsList.find { it.code.trim().uppercase() == couponCode.trim().uppercase() && it.status == "ACTIVE" 
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}
            if (coupon != null) {
                val now = System.currentTimeMillis()
                if (now <= coupon.expiryTimestamp) {
                    discountPercent = coupon.discountPercentage.toDouble()
                    // Valid coupon! Increment used count in Firestore
                    val updatedCount = coupon.usedCount + 1
                    db.collection("coupons").document(coupon.id).update("usedCount", updatedCount)
                    // Apply discount or points
                    triggerNotificationCallback?.invoke("🎫 تم تطبيق كوبون الخصم بنجاح! خصم بقيمة ${coupon.discountPercentage
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}%")
                
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

} else {
                    triggerNotificationCallback?.invoke("⚠️ الكوبون المستخدم منتهي الصلاحية")
                
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}
            
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

} else {
                triggerNotificationCallback?.invoke("⚠️ الكوبون غير صحيح أو غير مفعل")
            
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}
        
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}

        // Retrieve provider details to ensure accuracy
        val providersList = getProviders?.invoke() ?: emptyList()
        val prov = providersList.find { it.id == providerId || (providerId.isBlank() && it.name.trim() == providerName.trim()) 
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}
        val basePrice = prov?.previewPrice ?: 0.0
        finalPrice = if (discountPercent > 0.0) {
            basePrice * (1.0 - (discountPercent / 100.0))
        
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

} else {
            basePrice
        
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}

        val effectiveProviderPhone = prov?.phone?.trim()?.ifBlank {
            providersList.find { it.name.trim() == providerName.trim() 
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}?.phone?.trim() ?: ""
        
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

} ?: ""

        val finalBookingId = if (customBookingId.isNotBlank()) customBookingId else java.util.UUID.randomUUID().toString()
        val finalBookingNumber = "B-${(100000..999999).random()
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}"
        val generatedPass = if (customPassword.isNotBlank()) customPassword else "${(1000..9999).random()
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}"

        val newBooking = BookingEntity(
            id = finalBookingId,
            customerName = cleanName,
            customerPhone = cleanPhone,
            customerArea = area,
            serviceType = serviceType,
            providerId = providerId,
            providerName = providerName,
            providerPhone = effectiveProviderPhone,
            dateString = dateString,
            timeString = timeString,
            status = "PENDING",
            bookingNumber = finalBookingNumber,
            bookingPassword = "",
            pinCode = if (pinCode.isNotBlank()) pinCode else com.example.utils.SecureHasher.hashPin(generatedPass),
            totalAmount = finalPrice,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        try {
            // Mark slot as booked
            db.collection("bookings").document(finalBookingId).set(newBooking)
                .addOnSuccessListener {
                    _bookings.value = _bookings.value + newBooking
                    
                    // Log custom Firebase Analytics event
                    try {
                        val bundle = android.os.Bundle().apply {
                            putString("service_type", serviceType)
                            putString("provider_name", providerName)
                            putDouble("price", finalPrice)
                            putString("booking_number", finalBookingNumber)
                        
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}
                        com.example.MyApplication.logFirebaseEvent("submit_booking", bundle)
                    
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

} catch (e: Exception) {
                        e.printStackTrace()
                    
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}

                    // Auto-save user identity in memory if empty to ensure they can track notifications immediately
                    if (getCurrentUserPhone?.invoke()?.isEmpty() == true) {
                        setCurrentUserPhone?.invoke(cleanPhone)
                        setCurrentUserName?.invoke(cleanName)
                        setCurrentUserResidence?.invoke(area)
                    
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}

                    // Check Admin bookingRouting setting to control notification distribution
                    val bookingRouting = appState._settings.value.bookingRouting
                    val sendUserNotif = bookingRouting == "BOTH" || bookingRouting == "PROVIDER" || bookingRouting == "USER" || bookingRouting.isBlank()
                    val sendProviderNotif = bookingRouting == "BOTH" || bookingRouting == "PROVIDER" || bookingRouting.isBlank()
                    val sendAdminNotif = bookingRouting == "BOTH" || bookingRouting == "ADMIN" || bookingRouting.isBlank()

                    // 1. Notify the customer (user) with booking number and password
                    if (sendUserNotif) {
                        onAddNotification?.invoke(
                            "📅 تم تسجيل طلب حجزك رقم $finalBookingNumber",
                            "مرحباً بك $cleanName، تم استقبال طلب الحجز لدى الفني $providerName بنجاح. رقم الحجز السري هو: $finalBookingNumber ورمز المرور لإلغاء وتعديل الحجز هو: $generatedPass. يرجى الاحتفاظ بهما للتحكم بالحجز وإثبات الهوية عند إنجاز الخدمة.",
                            "USER",
                            cleanPhone
                        )
                    
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}

                    // 2. Notify the Technician (PROVIDER) with actual phone or ID
                    if (sendProviderNotif) {
                        val technicianTarget = effectiveProviderPhone.ifBlank { providerId.ifBlank { "PROVIDER" 
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

} 
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}
                        onAddNotification?.invoke(
                            "⚡ حجز عاجل جديد رقم $finalBookingNumber",
                            "العميل $cleanName ($cleanPhone) من ($area) حجز خدمة ($serviceType) لدى الفني $providerName بموعد $dateString $timeString. السعر المتوقع: $finalPrice ريال يمني.",
                            "PROVIDER",
                            technicianTarget
                        )
                    
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}

                    // 3. Notify the Admin/Supervisor according to settings
                    if (sendAdminNotif) {
                        onAddNotification?.invoke(
                            "📢 حجز جديد مسجل في النظام",
                            "العميل $cleanName حجز لدى $providerName في مدينة $area. رقم الحجز: $finalBookingNumber والرمز السري: $generatedPass.",
                            "ADMIN_ONLY",
                            ""
                        )
                    
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}
                
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}
                .addOnFailureListener { e ->
                    triggerNotificationCallback?.invoke("❌ فشل الحجز: ${e.message
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}")
                
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}

            triggerNotificationCallback?.invoke("تم إرسال طلب الحجز، سيتم مراجعته")
        
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

} catch (e: Exception) {
            e.printStackTrace()
        
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}
    
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}

    fun updateBookingStatus(bookingId: String, newStatus: BookingStatus) =
        updateBookingStatusImpl(bookingId, newStatus.name)

    fun updateBookingStatus(bookingId: String, newStatus: String, rejectionReason: String = "") =
        updateBookingStatusImpl(bookingId, newStatus, rejectionReason)

    fun updateBookingStatusImpl(bookingId: String, newStatus: String, rejectionReason: String = "") {
        db.collection("bookings").document(bookingId).get().addOnSuccessListener { snapshot ->
            val b = snapshot.toObject(BookingEntity::class.java)
            if (b != null) {
                val updated = b.copy(status = newStatus, rejectionReason = rejectionReason)
                db.collection("bookings").document(bookingId).set(updated)
                
                // Automatically trigger getOrCreateChannel with relatedEntityId & relatedEntityType = "BOOKING" upon acceptance/approval
                if (newStatus == "APPROVED" || newStatus == "ACCEPTED" || newStatus == "IN_PROGRESS") {
                    val otherId = b.providerId.ifEmpty { b.providerPhone.ifEmpty { "ADMIN" 
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

} 
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}
                    val otherName = b.providerName.ifEmpty { "مقدم الخدمة" 
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}
                    val otherPhone = b.providerPhone
                    onOpenOrCreateChatChannel?.invoke(
                        otherId,
                        "BOOKING",
                        otherName,
                        otherPhone,
                        b.category,
                        bookingId,
                        "BOOKING"
                    ) { createdCh ->
                        // Channel auto-provisioned upon booking approval with relatedChatChannelId stored
                        if (createdCh != null && createdCh.id.isNotEmpty()) {
                            db.collection("bookings").document(bookingId).update("relatedChatChannelId", createdCh.id)
                        
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}
                    
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}
                
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}
                
                val arabicStatusMsg = when(newStatus) {
                    "APPROVED", "ACCEPTED", "IN_PROGRESS" -> "قبول وتأكيد حجزك بنجاح وسيتواصل معك الفني قريباً"
                    "PENDING", "UNDER_REVIEW" -> "وضع حجزك قيد المراجعة والتدقيق الإداري"
                    "REJECTED" -> "رفض وإلغاء حجزك" + (if (rejectionReason.isNotBlank()) " لسبب: $rejectionReason" else "")
                    "COMPLETED" -> "إكمال وإنجاز الخدمة بنجاح وتقييم العمل"
                    else -> "تعديل حالة طلب حجزك إلى: $newStatus"
                
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}

                // Always send critical user notifications for booking transitions so they can track progress
                onAddNotification?.invoke(
                    "📅 تحديث حالة الحجز (رقم ${b.bookingCode.ifBlank { b.bookingNumber.ifBlank { b.id 
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

} 
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

})",
                    "عزيزي العميل، تم $arabicStatusMsg للخدمة المقدمة من ${b.providerName
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}.",
                    "USER",
                    b.customerPhone.ifBlank { b.clientPhone 
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}
                )

                // Also notify the technician (provider)
                val provTarget = b.providerPhone.ifBlank {
                    val provObj = getProviders?.invoke()?.find { it.id == b.providerId || it.name.trim() == b.providerName.trim() 
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}
                    provObj?.phone?.trim()?.ifBlank { b.providerId 
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

} ?: b.providerId
                
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}
                if (provTarget.isNotBlank()) {
                    onAddNotification?.invoke(
                        "📅 تحديث حالة الحجز (رقم ${b.bookingCode.ifBlank { b.bookingNumber.ifBlank { b.id 
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

} 
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

})",
                        "تم تغيير حالة الحجز للعميل ${b.customerName
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

} إلى: $arabicStatusMsg",
                        "PROVIDER",
                        provTarget
                    )
                
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}

                // Notify admin
                onAddNotification?.invoke(
                    "📢 تحديث حالة حجز",
                    "تم تحديث حالة الحجز رقم ${b.bookingCode.ifBlank { b.bookingNumber.ifBlank { b.id 
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

} 
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

} إلى $arabicStatusMsg للعميل ${b.customerName
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

} والفني ${b.providerName
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}.",
                    "ADMIN_ONLY",
                    ""
                )
            
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}
        
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}
        val toastMsg = when(newStatus) {
            "APPROVED", "ACCEPTED", "IN_PROGRESS" -> "⚡ تم قبول وتأكيد الحجز بنجاح"
            "PENDING", "UNDER_REVIEW" -> "⏳ تم وضع الحجز قيد المراجعة"
            "REJECTED" -> "❌ تم رفض الحجز وإلغائه"
            "COMPLETED" -> "🎉 تم إكمال الخدمة بنجاح وتوثيق الإنجاز"
            else -> "تم تحديث حالة الحجز بنجاح"
        
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}
        triggerNotificationCallback?.invoke(toastMsg)
    
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}

    fun deleteBooking(bookingId: String) = deleteBookingImpl(bookingId)

    fun deleteBookingImpl(bookingId: String) {
        val b = _bookings.value.find { it.id == bookingId 
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}
        val status = b?.status?.uppercase() ?: ""
        if (b != null && status != "COMPLETED" && status != "CANCELLED") {
            triggerNotificationCallback?.invoke("⚠️ لا يمكن حذف الحجز إلا إذا كان مكتملاً أو ملغياً")
            return
        
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}
        _bookings.value = _bookings.value.filter { it.id != bookingId 
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}
        db.collection("bookings").document(bookingId).delete()
        bookingRepository.deleteBooking(bookingId, {
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}, {
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

})
        triggerNotificationCallback?.invoke("🗑️ تم حذف الحجز من السجلات")

        val bkCode = b?.bookingCode?.ifBlank { b.bookingNumber.ifBlank { bookingId 
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

} 
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

} ?: bookingId
        val custName = b?.fullName?.ifBlank { b.clientName.ifBlank { b.customerName.ifBlank { "عميل" 
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

} 
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

} 
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

} ?: "عميل"
        onAddNotification?.invoke(
            "🗑️ إشعار إداري: حذف حجز",
            "نوع العملية: (حذف) | رقم الحجز: $bkCode | اسم العميل: $custName",
            "ADMIN_ONLY",
            ""
        )
    
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}

    fun deleteAllBookings(customerPhone: String) = deleteAllBookingsImpl(customerPhone)

    fun deleteAllBookingsImpl(customerPhone: String) {
        _bookings.value = _bookings.value.filter { it.customerPhone != customerPhone 
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}
        db.collection("bookings")
            .whereEqualTo("customerPhone", customerPhone)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val batch = db.batch()
                for (doc in querySnapshot.documents) {
                    batch.delete(doc.reference)
                
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}
                batch.commit().addOnSuccessListener {
                    triggerNotificationCallback?.invoke("🗑️ تم تصفية وحذف سجل جميع الحجوزات بنجاح.")
                
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}
            
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}
    
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}

    fun updateBooking(booking: BookingEntity) = updateBookingImpl(booking)

    fun updateBookingImpl(booking: BookingEntity) {
        db.collection("bookings").document(booking.id).set(booking)
        _bookings.value = _bookings.value.map { if (it.id == booking.id) booking else it 
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}
        triggerNotificationCallback?.invoke("💾 تم تحديث بيانات الحجز بنجاح")

        val bkCode = booking.bookingCode.ifBlank { booking.bookingNumber.ifBlank { booking.id 
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

} 
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}
        val custName = booking.fullName.ifBlank { booking.clientName.ifBlank { booking.customerName.ifBlank { "عميل" 
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

} 
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

} 
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}
        onAddNotification?.invoke(
            "✏️ إشعار إداري: تعديل حجز",
            "نوع العملية: (تعديل) | رقم الحجز: $bkCode | اسم العميل: $custName",
            "ADMIN_ONLY",
            ""
        )
    
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}




    fun attemptCancelBooking(bookingId: String, input: String, reason: String = "ملغي بطلب العميل", cancelledByParam: String = "USER", onResult: (Boolean, String) -> Unit) =
        attemptCancelBookingImpl(bookingId, input, reason, cancelledByParam, onResult)

        fun attemptCancelBookingImpl(bookingId: String, input: String, reason: String = "ملغي بطلب العميل", cancelledByParam: String = "USER", onResult: (Boolean, String) -> Unit) {
        val b = _bookings.value.find { it.id == bookingId 
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}
        if (b == null) {
            onResult(false, "❌ الحجز غير موجود")
            return
        
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}
        
        injectedRepository.cancelBookingWithSecurity(
            booking = b,
            inputPinOrPassword = input,
            cancellationReason = reason,
            cancelledBy = cancelledByParam,
            onSuccess = {
                onResult(true, "✅ تم إلغاء الحجز بنجاح")
                triggerNotificationCallback?.invoke("✅ تم إلغاء الحجز بنجاح")
            
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

},
            onError = { msg ->
                onResult(false, msg)
            
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}
        )
    
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}

    fun editBookingByUser(bookingId: String, newDate: String, newTime: String, newServiceType: String, providerId: String = "", providerName: String = "") {
        val targetProviderId = providerId.ifEmpty {
            _bookings.value.find { it.id == bookingId 
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}?.providerId ?: ""
        
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}
        val isTimeSlotTaken = _bookings.value.any {
            it.id != bookingId &&
            it.providerId == targetProviderId &&
            it.dateString.trim() == newDate.trim() &&
            it.timeString.trim() == newTime.trim() &&
            (it.status == "PENDING" || it.status == "APPROVED" || it.status == "IN_PROGRESS")
        
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}
        if (isTimeSlotTaken) {
            triggerToast("❌ عذراً! هذا الوقت (${newTime
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}) وتاريخ (${newDate
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}) محجوز بالفعل لدى مقدم الخدمة. يرجى اختيار موعد آخر.")
            return
        
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}

        val updates = mutableMapOf<String, Any>(
            "dateString" to newDate,
            "timeString" to newTime,
            "serviceType" to newServiceType,
            "updatedAt" to System.currentTimeMillis()
        )
        if (providerId.isNotEmpty()) {
            updates["providerId"] = providerId
        
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}
        if (providerName.isNotEmpty()) {
            updates["providerName"] = providerName
        
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}
        db.collection("bookings").document(bookingId).update(updates)
            .addOnSuccessListener {
                triggerToast("✅ تم تعديل الحجز بنجاح!")
            
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}
    
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}

    fun getBookingStatusColor(status: String): String {
        return when (status) {
            "PENDING" -> "#FF9800" // Orange
            "APPROVED" -> "#2196F3" // Blue
            "IN_PROGRESS" -> "#9C27B0" // Purple
            "COMPLETED" -> "#4CAF50" // Green
            "CANCELLED" -> "#F44336" // Red
            else -> "#757575" // Grey
        
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}
    
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}

    fun getBookingStatusLabel(status: String): String {
        return when (status) {
            "PENDING" -> "قيد الانتظار"
            "APPROVED" -> "مقبول"
            "IN_PROGRESS" -> "جاري التنفيذ"
            "COMPLETED" -> "مكتمل"
            "CANCELLED" -> "ملغي"
            else -> "غير معروف"
        
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}
    
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}

    fun getBookingProgress(status: String): Float {
        return when (status) {
            "PENDING" -> 0.25f
            "APPROVED" -> 0.50f
            "IN_PROGRESS" -> 0.75f
            "COMPLETED" -> 1.0f
            "CANCELLED" -> 1.0f
            else -> 0.0f
        
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}
    
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}
    
    fun createBookingDirectly(provider: com.example.data.ProviderEntity, notes: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        onError("Not implemented")
    
    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}

    fun lockBookingAfterFailedAttempts(bookingId: String, lockDurationMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = injectedRepository.lockBooking(bookingId, lockDurationMs)
            onResult(result.isSuccess)
        }
    }

}
