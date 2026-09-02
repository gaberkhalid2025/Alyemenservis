package com.example.ui.viewmodels

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.example.data.BookingEntity
import com.example.data.repositories.BookingRepository
import com.example.data.*
import com.example.data.models.*
import com.example.utils.*
import java.util.UUID
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
                    triggerToast("🎉 تم إنشاء حجزك بنجاح برقم: ${createdBooking.bookingNumber}")
                },
                onError = { errorMsg ->
                    _createBookingStatus.value = Result.failure(Exception(errorMsg))
                    triggerToast("❌ فشل إنشاء الحجز: $errorMsg")
                }
            )
        }
    }

    fun createBooking(booking: BookingEntity, onResult: (Boolean) -> Unit = {}) {
        val bId = booking.id.ifEmpty { java.util.UUID.randomUUID().toString() }
        val bNum = booking.bookingNumber.ifEmpty { "YEM-${(10000..99999).random()}" }
        val bPass = booking.bookingPassword.ifEmpty { "${(1000..9999).random()}" }
        val finalized = booking.copy(
            id = bId,
            bookingNumber = bNum,
            bookingPassword = bPass,
            createdAt = if (booking.createdAt == 0L) System.currentTimeMillis() else booking.createdAt,
            updatedAt = System.currentTimeMillis()
        )
        try {
            db.collection("bookings").document(bId).set(finalized)
                .addOnSuccessListener {
                    _bookings.value = _bookings.value + finalized
                    // Notification to Customer
                    onAddNotification?.invoke(
                        "📅 حجز جديد رقم $bNum",
                        "تم تسجيل طلب حجز موعد لدى ${finalized.providerName} بتاريخ ${finalized.dateString} الساعة ${finalized.timeString}.",
                        "USER",
                        finalized.customerPhone.ifEmpty { finalized.clientPhone }
                    )
                    // Notification to Provider
                    if (finalized.providerPhone.isNotEmpty()) {
                        onAddNotification?.invoke(
                            "📅 حجز جديد رقم $bNum",
                            "مرحباً يا غالي، تم تسجيل طلب حجز جديد لديك من قبل العميل ${finalized.customerName} بتاريخ ${finalized.dateString} الساعة ${finalized.timeString}.",
                            "USER",
                            finalized.providerPhone
                        )
                    }
                    onResult(true)
                }
                .addOnFailureListener {
                    _bookings.value = _bookings.value + finalized
                    onResult(true)
                }
        } catch (e: Exception) {
            _bookings.value = _bookings.value + finalized
            onResult(true)
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
        }

        // 2. Duplication & Overlap prevention scan
        val isTimeSlotTaken = _bookings.value.any {
            it.providerId == providerId &&
            it.dateString.trim() == dateString.trim() &&
            it.timeString.trim() == timeString.trim() &&
            (it.status == "PENDING" || it.status == "APPROVED" || it.status == "IN_PROGRESS")
        }
        if (isTimeSlotTaken) {
            triggerNotificationCallback?.invoke("⚠️ عذراً، هذا الموعد ($dateString في $timeString) محجوز مسبقاً لدى هذا الفني. يرجى اختيار وقت آخر!")
            return
        }

        // Determine specific pricing via admin settings or coupon logic
        var discountPercent = 0.0
        var finalPrice = 0.0

        if (couponCode.isNotBlank()) {
            val couponsList = getCoupons?.invoke() ?: emptyList()
            val coupon = couponsList.find { it.code.trim().uppercase() == couponCode.trim().uppercase() && it.status == "ACTIVE" }
            if (coupon != null) {
                val now = System.currentTimeMillis()
                if (now <= coupon.expiryTimestamp) {
                    discountPercent = coupon.discountPercentage.toDouble()
                    // Valid coupon! Increment used count in Firestore
                    val updatedCount = coupon.usedCount + 1
                    db.collection("coupons").document(coupon.id).update("usedCount", updatedCount)
                    // Apply discount or points
                    triggerNotificationCallback?.invoke("🎫 تم تطبيق كوبون الخصم بنجاح! خصم بقيمة ${coupon.discountPercentage}%")
                } else {
                    triggerNotificationCallback?.invoke("⚠️ الكوبون المستخدم منتهي الصلاحية")
                }
            } else {
                triggerNotificationCallback?.invoke("⚠️ الكوبون غير صحيح أو غير مفعل")
            }
        }

        // Retrieve provider details to ensure accuracy
        val providersList = getProviders?.invoke() ?: emptyList()
        val prov = providersList.find { it.id == providerId }
        val basePrice = prov?.previewPrice ?: 0.0
        finalPrice = if (discountPercent > 0.0) {
            basePrice * (1.0 - (discountPercent / 100.0))
        } else {
            basePrice
        }

        val finalBookingId = if (customBookingId.isNotBlank()) customBookingId else java.util.UUID.randomUUID().toString()
        val finalBookingNumber = "B-${(100000..999999).random()}"
        val generatedPass = if (customPassword.isNotBlank()) customPassword else "${(1000..9999).random()}"

        val newBooking = BookingEntity(
            id = finalBookingId,
            customerName = cleanName,
            customerPhone = cleanPhone,
            customerArea = area,
            serviceType = serviceType,
            providerId = providerId,
            providerName = providerName,
            providerPhone = prov?.phone ?: "",
            dateString = dateString,
            timeString = timeString,
            status = "PENDING",
            bookingNumber = finalBookingNumber,
            bookingPassword = generatedPass,
            pinCode = pinCode,
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
                        }
                        com.example.MyApplication.logFirebaseEvent("submit_booking", bundle)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    // Auto-save user identity in memory if empty to ensure they can track notifications immediately
                    if (getCurrentUserPhone?.invoke()?.isEmpty() == true) {
                        setCurrentUserPhone?.invoke(cleanPhone)
                        setCurrentUserName?.invoke(cleanName)
                        setCurrentUserResidence?.invoke(area)
                    }

                    // Notify the customer (user) that their booking was successfully submitted with booking number and password
                    onAddNotification?.invoke(
                        "📅 تم تسجيل طلب حجزك رقم $finalBookingNumber",
                        "مرحباً بك $cleanName، تم استقبال طلب الحجز لدى الفني $providerName بنجاح. رقم الحجز السري هو: $finalBookingNumber ورمز المرور لإلغاء وتعديل الحجز هو: $generatedPass. يرجى الاحتفاظ بهما للتحكم بالحجز وإثبات الهوية عند إنجاز الخدمة.",
                        "USER",
                        cleanPhone
                    )

                    // Compile a highly detailed notification containing customer's name, phone, and area of residence
                    onAddNotification?.invoke(
                        "⚡ حجز عاجل جديد رقم $finalBookingNumber",
                        "العميل $cleanName ($cleanPhone) من ($area) حجز خدمة ($serviceType) لدى الفني $providerName بموعد $dateString $timeString. السعر المتوقع: $finalPrice ريال يمني.",
                        "PROVIDER",
                        prov?.phone ?: ""
                    )

                    // 1. Always notify the Admin/Supervisor
                    onAddNotification?.invoke(
                        "📢 حجز جديد مسجل في النظام",
                        "العميل $cleanName حجز لدى $providerName في مدينة $area. رقم الحجز: $finalBookingNumber والرمز السري: $generatedPass.",
                        "ADMIN_ONLY",
                        ""
                    )
                }
                .addOnFailureListener { e ->
                    triggerNotificationCallback?.invoke("❌ فشل الحجز: ${e.message}")
                }

            triggerNotificationCallback?.invoke("تم إرسال طلب الحجز، سيتم مراجعته")
        } catch (e: Exception) {
            e.printStackTrace()
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
                    val otherId = b.providerId.ifEmpty { b.providerPhone.ifEmpty { "ADMIN" } }
                    val otherName = b.providerName.ifEmpty { "مقدم الخدمة" }
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
                        }
                    }
                }
                
                val arabicStatusMsg = when(newStatus) {
                    "APPROVED", "ACCEPTED", "IN_PROGRESS" -> "قبول وتأكيد حجزك بنجاح وسيتواصل معك الفني قريباً"
                    "PENDING", "UNDER_REVIEW" -> "وضع حجزك قيد المراجعة والتدقيق الإداري"
                    "REJECTED" -> "رفض وإلغاء حجزك" + (if (rejectionReason.isNotBlank()) " لسبب: $rejectionReason" else "")
                    "COMPLETED" -> "إكمال وإنجاز الخدمة بنجاح وتقييم العمل"
                    else -> "تعديل حالة طلب حجزك إلى: $newStatus"
                }

                // Always send critical user notifications for booking transitions so they can track progress
                onAddNotification?.invoke(
                    "📅 تحديث حالة الحجز (رقم ${b.bookingCode.ifBlank { b.bookingNumber.ifBlank { b.id } }})",
                    "عزيزي العميل، تم $arabicStatusMsg للخدمة المقدمة من ${b.providerName}.",
                    "USER",
                    b.customerPhone.ifBlank { b.clientPhone }
                )
            }
        }
        val toastMsg = when(newStatus) {
            "APPROVED", "ACCEPTED", "IN_PROGRESS" -> "⚡ تم قبول وتأكيد الحجز بنجاح"
            "PENDING", "UNDER_REVIEW" -> "⏳ تم وضع الحجز قيد المراجعة"
            "REJECTED" -> "❌ تم رفض الحجز وإلغائه"
            "COMPLETED" -> "🎉 تم إكمال الخدمة بنجاح وتوثيق الإنجاز"
            else -> "تم تحديث حالة الحجز بنجاح"
        }
        triggerNotificationCallback?.invoke(toastMsg)
    }

    fun deleteBooking(bookingId: String) = deleteBookingImpl(bookingId)

    fun deleteBookingImpl(bookingId: String) {
        val b = _bookings.value.find { it.id == bookingId }
        _bookings.value = _bookings.value.filter { it.id != bookingId }
        db.collection("bookings").document(bookingId).delete()
        triggerNotificationCallback?.invoke("🗑️ تم حذف الحجز من السجلات")

        val bkCode = b?.bookingCode?.ifBlank { b.bookingNumber.ifBlank { bookingId } } ?: bookingId
        val custName = b?.fullName?.ifBlank { b.clientName.ifBlank { b.customerName.ifBlank { "عميل" } } } ?: "عميل"
        onAddNotification?.invoke(
            "🗑️ إشعار إداري: حذف حجز",
            "نوع العملية: (حذف) | رقم الحجز: $bkCode | اسم العميل: $custName",
            "ADMIN_ONLY",
            ""
        )
    }

    fun deleteAllBookings(customerPhone: String) = deleteAllBookingsImpl(customerPhone)

    fun deleteAllBookingsImpl(customerPhone: String) {
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
                    triggerNotificationCallback?.invoke("🗑️ تم تصفية وحذف سجل جميع الحجوزات بنجاح.")
                }
            }
    }

    fun updateBooking(booking: BookingEntity) = updateBookingImpl(booking)

    fun updateBookingImpl(booking: BookingEntity) {
        db.collection("bookings").document(booking.id).set(booking)
        _bookings.value = _bookings.value.map { if (it.id == booking.id) booking else it }
        triggerNotificationCallback?.invoke("💾 تم تحديث بيانات الحجز بنجاح")

        val bkCode = booking.bookingCode.ifBlank { booking.bookingNumber.ifBlank { booking.id } }
        val custName = booking.fullName.ifBlank { booking.clientName.ifBlank { booking.customerName.ifBlank { "عميل" } } }
        onAddNotification?.invoke(
            "✏️ إشعار إداري: تعديل حجز",
            "نوع العملية: (تعديل) | رقم الحجز: $bkCode | اسم العميل: $custName",
            "ADMIN_ONLY",
            ""
        )
    }

    fun cancelBookingByUser(bookingId: String) = cancelBookingByUserImpl(bookingId)

    fun cancelBookingByUserImpl(bookingId: String) {
        val b = _bookings.value.find { it.id == bookingId }
        _bookings.value = _bookings.value.map { booking ->
            if (booking.id == bookingId) {
                booking.copy(status = "CANCELLED")
            } else booking
        }
        try {
            db.collection("bookings").document(bookingId).update("status", "CANCELLED")
                .addOnSuccessListener {
                    triggerNotificationCallback?.invoke("✅ تم إلغاء الحجز وإرسال إشعار للإدارة والفني")
                    val bkCode = b?.bookingCode?.ifBlank { b?.bookingNumber?.ifBlank { bookingId } } ?: bookingId
                    val custName = b?.fullName?.ifBlank { b.clientName.ifBlank { b.customerName.ifBlank { "العميل" } } } ?: "العميل"
                    val custPhone = b?.customerPhone?.ifBlank { b.clientPhone } ?: ""
                    val provName = b?.providerName ?: ""
                    val srvName = b?.serviceType?.ifBlank { "خدمة" } ?: "خدمة"
                    
                    // 1. Notify Admin
                    onAddNotification?.invoke(
                        "❌ إشعار إداري: إلغاء حجز",
                        "نوع العملية: (إلغاء) | رقم الحجز: $bkCode | اسم العميل: $custName ($custPhone) | الخدمة: $srvName لدى $provName",
                        "ADMIN_ONLY",
                        ""
                    )
                    
                    // 2. Notify Provider
                    if (b != null && b.providerPhone.isNotBlank()) {
                        onAddNotification?.invoke(
                            "❌ إلغاء حجز من العميل",
                            "قام $custName ($custPhone) بإلغاء حجز الخدمة ($srvName).",
                            "PROVIDER",
                            b.providerPhone
                        )
                    }
                }
                .addOnFailureListener {
                    triggerNotificationCallback?.invoke("❌ فشل إلغاء الحجز، حاول مجدداً")
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun attemptCancelBooking(bookingId: String, input: String, reason: String = "ملغي بطلب العميل", onResult: (Boolean, String) -> Unit) =
        attemptCancelBookingImpl(bookingId, input, reason, onResult)

    fun attemptCancelBookingImpl(bookingId: String, input: String, reason: String = "ملغي بطلب العميل", onResult: (Boolean, String) -> Unit) {
        db.collection("bookings").document(bookingId).get().addOnSuccessListener { snapshot ->
            val b = snapshot.toObject(BookingEntity::class.java)
            if (b == null) {
                onResult(false, "❌ الحجز غير موجود في قاعدة البيانات")
                return@addOnSuccessListener
            }

            // Check if locked
            if (b.isLocked) {
                val until = b.lockedUntil ?: 0L
                if (System.currentTimeMillis() < until) {
                    val remainingSeconds = (until - System.currentTimeMillis()) / 1000
                    onResult(false, "🔒 هذا الحجز مقفل حالياً ومحمي بسبب تكرار المحاولات الخاطئة. يرجى المحاولة مجدداً بعد $remainingSeconds ثانية أو التواصل مع الإدارة.")
                    return@addOnSuccessListener
                }
            }

            // Check 8-hour cancellation restriction rule
            val canCancel = com.example.utils.BookingUtils.canModifyOrCancelBooking(
                scheduledAtTimestamp = b.scheduledAt,
                dateString = b.dateString.ifBlank { b.date },
                timeString = b.timeString.ifBlank { b.time }
            )
            if (!canCancel) {
                onResult(false, "⚠️ لا يمكن إلغاء الحجز؛ التعديل والإلغاء مسموح فقط قبل 8 ساعات من الموعد المحدد حرصاً على وقت مقدم الخدمة.")
                return@addOnSuccessListener
            }

            val cleanInput = input.trim()
            val isPassCorrect = cleanInput == b.bookingPassword && b.bookingPassword.isNotEmpty()
            val isNumCorrect = cleanInput == b.bookingNumber && b.bookingNumber.isNotEmpty()
            val isPinCorrect = cleanInput == b.pinCode && b.pinCode.isNotEmpty()

            if (isPassCorrect || isNumCorrect || isPinCorrect) {
                // Correct input! Do the cancellation
                val updated = b.copy(
                    status = "CANCELLED",
                    cancellationReason = reason,
                    cancelledAt = System.currentTimeMillis(),
                    cancelledBy = "USER",
                    cancellationAttempts = 0,
                    isLocked = false,
                    lockedUntil = 0L,
                    updatedAt = System.currentTimeMillis()
                )
                val bkCode = b.bookingCode.ifBlank { b.bookingNumber.ifBlank { b.id } }
                val custName = b.fullName.ifBlank { b.clientName.ifBlank { b.customerName.ifBlank { "العميل" } } }

                db.collection("bookings").document(bookingId).set(updated).addOnSuccessListener {
                    _bookings.value = _bookings.value.map { if (it.id == bookingId) updated else it }
                    
                    // Trigger in-app notifications
                    onAddNotification?.invoke(
                        "❌ تم إلغاء حجزك بنجاح",
                        "عزيزي العميل، تم إلغاء حجز الخدمة بنجاح بطلب منك. رقم الحجز: $bkCode",
                        "USER",
                        b.customerPhone.ifBlank { b.clientPhone }
                    )
                    
                    if (b.providerId.isNotEmpty()) {
                        onAddNotification?.invoke(
                            "❌ تم إلغاء حجز قائم لديك",
                            "الفني العزيز ${b.providerName}، نود إبلاغك بأن العميل قد ألغى الحجز رقم $bkCode والمحدد في تاريخ ${b.dateString} ${b.timeString}.",
                            "PROVIDER",
                            b.providerPhone.ifEmpty { b.customerPhone }
                        )
                    }

                    // Add Admin notification containing: booking code, customer name, and operation type (إلغاء)
                    onAddNotification?.invoke(
                        "❌ إشعار إداري: إلغاء حجز",
                        "نوع العملية: (إلغاء) | رقم الحجز: $bkCode | اسم العميل: $custName | السبب: $reason",
                        "ADMIN_ONLY",
                        ""
                    )
                    onResult(true, "✅ تم إلغاء الحجز بنجاح")
                }.addOnFailureListener {
                    onResult(false, "❌ فشل تحديث حالة الحجز في الخادم")
                }
            } else {
                // Wrong input!
                val newAttempts = b.cancellationAttempts + 1
                val maxAttempts = 3
                val shouldLock = newAttempts >= maxAttempts
                val lockTime = if (shouldLock) System.currentTimeMillis() + 5 * 60 * 1000 else 0L // 5 minutes lock
                
                val updated = b.copy(
                    cancellationAttempts = newAttempts,
                    isLocked = shouldLock,
                    lockedUntil = if (shouldLock) lockTime else null
                )
                
                db.collection("bookings").document(bookingId).set(updated).addOnSuccessListener {
                    _bookings.value = _bookings.value.map { if (it.id == bookingId) updated else it }
                    if (shouldLock) {
                        onResult(false, "🔒 تم قفل عمليات إلغاء هذا الحجز مؤقتاً لمدة 5 دقائق لحماية مقدم الخدمة من الإلغاءات غير المصرح بها.")
                    } else {
                        onResult(false, "❌ كلمة المرور أو رقم الحجز غير صحيح! المحاولات المتبقية: ${maxAttempts - newAttempts}")
                    }
                }.addOnFailureListener {
                    onResult(false, "❌ إدخال خاطئ وفشل حفظ محاولة التحقق")
                }
            }
        }.addOnFailureListener {
            onResult(false, "❌ فشل الاتصال بقاعدة البيانات")
        }
    }

    fun cancelBookingByTechnician(bookingId: String, reason: String, onComplete: () -> Unit = {}) =
        cancelBookingByTechnicianImpl(bookingId, reason, onComplete)

    fun cancelBookingByTechnicianImpl(bookingId: String, reason: String, onComplete: () -> Unit = {}) {
        db.collection("bookings").document(bookingId).get().addOnSuccessListener { snapshot ->
            val b = snapshot.toObject(BookingEntity::class.java)
            if (b != null) {
                val updated = b.copy(
                    status = "CANCELLED",
                    cancellationReason = "إلغاء من قبل الفني: $reason",
                    cancelledAt = System.currentTimeMillis(),
                    cancelledBy = "PROVIDER",
                    updatedAt = System.currentTimeMillis()
                )
                db.collection("bookings").document(bookingId).set(updated).addOnSuccessListener {
                    _bookings.value = _bookings.value.map { if (it.id == bookingId) updated else it }
                    
                    // Notify Customer
                    onAddNotification?.invoke(
                        "🚫 قام الفني بإلغاء حجزك",
                        "عزيزي العميل، اعتذر الفني ${b.providerName} عن إتمام الحجز رقم ${b.bookingNumber.ifEmpty { b.id }}. السبب: $reason",
                        "USER",
                        b.customerPhone
                    )
                    
                    // Notify Admin
                    onAddNotification?.invoke(
                        "🚨 قام الفني بإلغاء حجز",
                        "قام الفني ${b.providerName} بإلغاء حجز العميل ${b.customerName} (${b.customerPhone}). السبب: $reason",
                        "ADMIN_ONLY",
                        ""
                    )
                    triggerNotificationCallback?.invoke("❌ تم إلغاء الحجز وإشعار العميل والإدارة")
                    onComplete()
                }
            }
        }
    }

    fun cancelBookingByAdmin(bookingId: String, reason: String, onComplete: () -> Unit = {}) =
        cancelBookingByAdminImpl(bookingId, reason, onComplete)

    fun cancelBookingByAdminImpl(bookingId: String, reason: String, onComplete: () -> Unit = {}) {
        db.collection("bookings").document(bookingId).get().addOnSuccessListener { snapshot ->
            val b = snapshot.toObject(BookingEntity::class.java)
            if (b != null) {
                val updated = b.copy(
                    status = "CANCELLED",
                    cancellationReason = "إلغاء من قبل الإدارة: $reason",
                    cancelledAt = System.currentTimeMillis(),
                    cancelledBy = "ADMIN",
                    updatedAt = System.currentTimeMillis()
                )
                db.collection("bookings").document(bookingId).set(updated).addOnSuccessListener {
                    _bookings.value = _bookings.value.map { if (it.id == bookingId) updated else it }
                    
                    // Notify Customer
                    onAddNotification?.invoke(
                        "🚫 تم إلغاء حجزك من قبل الإدارة",
                        "عزيزي العميل، تم إلغاء حجزك رقم ${b.bookingNumber.ifEmpty { b.id }} بواسطة إدارة المنصة. السبب: $reason",
                        "USER",
                        b.customerPhone
                    )
                    
                    // Notify Technician
                    if (b.providerPhone.isNotBlank()) {
                        onAddNotification?.invoke(
                            "🚫 تم إلغاء حجزك من قبل الإدارة",
                            "الفني العزيز ${b.providerName}، تم إلغاء حجز العميل ${b.customerName} من قبل الإدارة. السبب: $reason",
                            "PROVIDER",
                            b.providerPhone
                        )
                    }
                    triggerNotificationCallback?.invoke("❌ تم إلغاء الحجز وإشعار جميع الأطراف")
                    onComplete()
                }
            }
        }
    }

    fun getBookingStatusColor(status: String): String = getBookingStatusColorImpl(status)

    fun getBookingStatusColorImpl(status: String): String {
        return when (status.uppercase()) {
            "PENDING", "UNDER_REVIEW" -> "#F97316" // Orange
            "IN_PROGRESS", "ACCEPTED", "APPROVED" -> "#3B82F6" // Blue
            "COMPLETED" -> "#10B981" // Green
            "REJECTED", "CANCELLED" -> "#EF4444" // Red
            else -> "#9E9E9E"
        }
    }

    fun getBookingStatusLabel(status: String): String = getBookingStatusLabelImpl(status)

    fun getBookingStatusLabelImpl(status: String): String {
        return when (status.uppercase()) {
            "PENDING", "UNDER_REVIEW" -> "🔍 قيد المراجعة والتدقيق (33%)"
            "IN_PROGRESS", "ACCEPTED", "APPROVED" -> "⚡ جاري تنفيذ الخدمة (66%)"
            "COMPLETED" -> "🎉 مكتملة بنجاح (100%)"
            "REJECTED" -> "❌ مرفوضة من الإدارة"
            "CANCELLED" -> "❌ ملغية"
            else -> status
        }
    }

    fun getBookingProgress(status: String): Float = getBookingProgressImpl(status)

    fun getBookingProgressImpl(status: String): Float {
        return when (status.uppercase()) {
            "PENDING", "UNDER_REVIEW" -> 0.33f
            "IN_PROGRESS", "ACCEPTED", "APPROVED" -> 0.66f
            "COMPLETED" -> 1.00f
            else -> 0.0f
        }
    }

    fun createBookingDirectly(
        provider: ProviderEntity,
        notes: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val custName = getCurrentUserName?.invoke()?.ifBlank { "عميل التطبيق" } ?: "عميل التطبيق"
        val custPhone = getCurrentUserPhone?.invoke()?.ifBlank { "770000000" } ?: "770000000"
        val newBooking = BookingEntity(
            id = java.util.UUID.randomUUID().toString(),
            customerName = custName,
            customerPhone = custPhone,
            clientName = custName,
            clientPhone = custPhone,
            customerArea = getCurrentUserResidence?.invoke()?.ifBlank { provider.area } ?: provider.area,
            serviceType = provider.profession,
            providerId = provider.id,
            providerName = provider.name,
            providerPhone = provider.phone,
            serviceDetails = notes,
            dateString = "2026-08-25",
            timeString = "12:00 م",
            status = "PENDING",
            bookingNumber = "MAP-${(10000..99999).random()}"
        )
        createBooking(newBooking) { success ->
            if (success) onSuccess() else onError("تعذر إتمام الحجز")
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
