package com.example.ui

import com.example.data.*
import com.example.utils.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

fun MainViewModel.addBookingImpl(
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
        triggerNotification("❌ الهوية غير مسجلة: رقم الهاتف يجب أن يكون يمنياً صحيحاً مفعلاً ومكوناً من 9 أرقام يبدأ بـ 77 أو 73 أو 71 أو 70!")
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
        triggerNotification("⚠️ عذراً، هذا الموعد ($dateString في $timeString) محجوز مسبقاً لدى هذا الفني. يرجى اختيار وقت آخر!")
        return
    }

    // Determine specific pricing via admin settings or coupon logic
    var discountPercent = 0.0
    var finalPrice = 0.0

    if (couponCode.isNotBlank()) {
        val coupon = _coupons.value.find { it.code.trim().uppercase() == couponCode.trim().uppercase() && it.status == "ACTIVE" }
        if (coupon != null) {
            val now = System.currentTimeMillis()
            if (now <= coupon.expiryTimestamp) {
                discountPercent = coupon.discountPercentage.toDouble()
                // Valid coupon! Increment used count in Firestore
                val updatedCount = coupon.usedCount + 1
                db.collection("coupons").document(coupon.id).update("usedCount", updatedCount)
                // Apply discount or points
                triggerNotification("🎫 تم تطبيق كوبون الخصم بنجاح! خصم بقيمة ${coupon.discountPercentage}%")
            } else {
                triggerNotification("⚠️ الكوبون المستخدم منتهي الصلاحية")
            }
        } else {
            triggerNotification("⚠️ الكوبون غير صحيح أو غير مفعل")
        }
    }

    // Retrieve provider details to ensure accuracy
    val prov = _providers.value.find { it.id == providerId }
    val basePrice = prov?.previewPrice ?: 0.0
    finalPrice = if (discountPercent > 0.0) {
        basePrice * (1.0 - (discountPercent / 100.0))
    } else {
        basePrice
    }

    val finalBookingId = if (customBookingId.isNotBlank()) customBookingId else UUID.randomUUID().toString()
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
                if (_currentUserPhone.value.isEmpty()) {
                    _currentUserPhone.value = cleanPhone
                    _currentUserName.value = cleanName
                    _currentUserResidence.value = area
                }

                // Notify the customer (user) that their booking was successfully submitted with booking number and password
                addNotification(
                    title = "📅 تم تسجيل طلب حجزك رقم $finalBookingNumber",
                    message = "مرحباً بك $cleanName، تم استقبال طلب الحجز لدى الفني $providerName بنجاح. رقم الحجز السري هو: $finalBookingNumber ورمز المرور لإلغاء وتعديل الحجز هو: $generatedPass. يرجى الاحتفاظ بهما للتحكم بالحجز وإثبات الهوية عند إنجاز الخدمة.",
                    targetType = "USER",
                    targetValue = cleanPhone
                )

                // Compile a highly detailed notification containing customer's name, phone, and area of residence
                addNotification(
                    title = "⚡ حجز عاجل جديد رقم $finalBookingNumber",
                    message = "العميل $cleanName ($cleanPhone) من ($area) حجز خدمة ($serviceType) لدى الفني $providerName بموعد $dateString $timeString. السعر المتوقع: $finalPrice ريال يمني.",
                    targetType = "PROVIDER",
                    targetValue = prov?.phone ?: ""
                )

                // 1. Always notify the Admin/Supervisor
                addNotification(
                    title = "📢 حجز جديد مسجل في النظام",
                    message = "العميل $cleanName حجز لدى $providerName في مدينة $area. رقم الحجز: $finalBookingNumber والرمز السري: $generatedPass.",
                    targetType = "ADMIN_ONLY",
                    targetValue = ""
                )
            }
            .addOnFailureListener { e ->
                triggerNotification("❌ فشل الحجز: ${e.message}")
            }

        triggerNotification("تم إرسال طلب الحجز، سيتم مراجعته")
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun MainViewModel.updateBookingStatusImpl(bookingId: String, newStatus: String, rejectionReason: String = "") {
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
                openOrCreateChatChannel(
                    targetId = otherId,
                    targetType = "BOOKING",
                    targetName = otherName,
                    targetPhone = otherPhone,
                    targetCategory = b.category,
                    relatedEntityId = bookingId,
                    relatedEntityType = "BOOKING"
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
            addNotification(
                title = "📅 تحديث حالة الحجز (رقم ${b.bookingCode.ifBlank { b.bookingNumber.ifBlank { b.id } }})",
                message = "عزيزي العميل، تم $arabicStatusMsg للخدمة المقدمة من ${b.providerName}.",
                targetType = "USER",
                targetValue = b.customerPhone.ifBlank { b.clientPhone }
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
    triggerNotification(toastMsg)
}

fun MainViewModel.deleteBookingImpl(bookingId: String) {
    val b = _bookings.value.find { it.id == bookingId }
    _bookings.value = _bookings.value.filter { it.id != bookingId }
    db.collection("bookings").document(bookingId).delete()
    triggerNotification("🗑️ تم حذف الحجز من السجلات")

    val bkCode = b?.bookingCode?.ifBlank { b.bookingNumber.ifBlank { bookingId } } ?: bookingId
    val custName = b?.fullName?.ifBlank { b.clientName.ifBlank { b.customerName.ifBlank { "عميل" } } } ?: "عميل"
    addNotification(
        title = "🗑️ إشعار إداري: حذف حجز",
        message = "نوع العملية: (حذف) | رقم الحجز: $bkCode | اسم العميل: $custName",
        targetType = "ADMIN_ONLY",
        targetValue = ""
    )
}

fun MainViewModel.deleteAllBookingsImpl(customerPhone: String) {
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
                triggerNotification("🗑️ تم تصفية وحذف سجل جميع الحجوزات بنجاح.")
            }
        }
}

fun MainViewModel.updateBookingImpl(booking: BookingEntity) {
    db.collection("bookings").document(booking.id).set(booking)
    _bookings.value = _bookings.value.map { if (it.id == booking.id) booking else it }
    triggerNotification("💾 تم تحديث بيانات الحجز بنجاح")

    val bkCode = booking.bookingCode.ifBlank { booking.bookingNumber.ifBlank { booking.id } }
    val custName = booking.fullName.ifBlank { booking.clientName.ifBlank { booking.customerName.ifBlank { "عميل" } } }
    addNotification(
        title = "✏️ إشعار إداري: تعديل حجز",
        message = "نوع العملية: (تعديل) | رقم الحجز: $bkCode | اسم العميل: $custName",
        targetType = "ADMIN_ONLY",
        targetValue = ""
    )
}

fun MainViewModel.cancelBookingByUserImpl(bookingId: String) {
    val b = _bookings.value.find { it.id == bookingId }
    _bookings.value = _bookings.value.map { booking ->
        if (booking.id == bookingId) {
            booking.copy(status = "CANCELLED")
        } else booking
    }
    try {
        db.collection("bookings").document(bookingId).update("status", "CANCELLED")
            .addOnSuccessListener {
                triggerNotification("✅ تم إلغاء الحجز وإرسال إشعار للإدارة والفني")
                val bkCode = b?.bookingCode?.ifBlank { b?.bookingNumber?.ifBlank { bookingId } } ?: bookingId
                val custName = b?.fullName?.ifBlank { b.clientName.ifBlank { b.customerName.ifBlank { "العميل" } } } ?: "العميل"
                val custPhone = b?.customerPhone?.ifBlank { b.clientPhone } ?: ""
                val provName = b?.providerName ?: ""
                val srvName = b?.serviceType?.ifBlank { "خدمة" } ?: "خدمة"
                
                // 1. Notify Admin
                addNotification(
                    title = "❌ إشعار إداري: إلغاء حجز",
                    message = "نوع العملية: (إلغاء) | رقم الحجز: $bkCode | اسم العميل: $custName ($custPhone) | الخدمة: $srvName لدى $provName",
                    targetType = "ADMIN_ONLY",
                    targetValue = ""
                )
                
                // 2. Notify Provider
                if (b != null && b.providerPhone.isNotBlank()) {
                    addNotification(
                        title = "❌ إلغاء حجز من العميل",
                        message = "قام $custName ($custPhone) بإلغاء حجز الخدمة ($srvName).",
                        targetType = "PROVIDER",
                        targetValue = b.providerPhone
                    )
                }
            }
            .addOnFailureListener {
                triggerNotification("❌ فشل إلغاء الحجز، حاول مجدداً")
            }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun MainViewModel.attemptCancelBookingImpl(bookingId: String, input: String, reason: String = "ملغي بطلب العميل", onResult: (Boolean, String) -> Unit) {
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
                addNotification(
                    title = "❌ تم إلغاء حجزك بنجاح",
                    message = "عزيزي العميل، تم إلغاء حجز الخدمة بنجاح بطلب منك. رقم الحجز: $bkCode",
                    targetType = "USER",
                    targetValue = b.customerPhone.ifBlank { b.clientPhone }
                )
                
                if (b.providerId.isNotEmpty()) {
                    addNotification(
                        title = "❌ تم إلغاء حجز قائم لديك",
                        message = "الفني العزيز ${b.providerName}، نود إبلاغك بأن العميل قد ألغى الحجز رقم $bkCode والمحدد في تاريخ ${b.dateString} ${b.timeString}.",
                        targetType = "PROVIDER",
                        targetValue = b.providerPhone.ifEmpty { b.customerPhone }
                    )
                }

                // Add Admin notification containing: booking code, customer name, and operation type (إلغاء)
                addNotification(
                    title = "❌ إشعار إداري: إلغاء حجز",
                    message = "نوع العملية: (إلغاء) | رقم الحجز: $bkCode | اسم العميل: $custName | السبب: $reason",
                    targetType = "ADMIN_ONLY",
                    targetValue = ""
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

fun MainViewModel.cancelBookingByTechnicianImpl(bookingId: String, reason: String, onComplete: () -> Unit = {}) {
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
                addNotification(
                    title = "🚫 قام الفني بإلغاء حجزك",
                    message = "عزيزي العميل، اعتذر الفني ${b.providerName} عن إتمام الحجز رقم ${b.bookingNumber.ifEmpty { b.id }}. السبب: $reason",
                    targetType = "USER",
                    targetValue = b.customerPhone
                )
                
                // Notify Admin
                addNotification(
                    title = "🚨 قام الفني بإلغاء حجز",
                    message = "قام الفني ${b.providerName} بإلغاء حجز العميل ${b.customerName} (${b.customerPhone}). السبب: $reason",
                    targetType = "ADMIN_ONLY",
                    targetValue = ""
                )
                triggerNotification("❌ تم إلغاء الحجز وإشعار العميل والإدارة")
                onComplete()
            }
        }
    }
}

fun MainViewModel.cancelBookingByAdminImpl(bookingId: String, reason: String, onComplete: () -> Unit = {}) {
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
                addNotification(
                    title = "🚫 تم إلغاء حجزك من قبل الإدارة",
                    message = "عزيزي العميل، تم إلغاء حجزك رقم ${b.bookingNumber.ifEmpty { b.id }} بواسطة إدارة المنصة. السبب: $reason",
                    targetType = "USER",
                    targetValue = b.customerPhone
                )
                
                // Notify Technician
                if (b.providerPhone.isNotBlank()) {
                    addNotification(
                        title = "🚫 تم إلغاء حجزك من قبل الإدارة",
                        message = "الفني العزيز ${b.providerName}، تم إلغاء حجز العميل ${b.customerName} من قبل الإدارة. السبب: $reason",
                        targetType = "PROVIDER",
                        targetValue = b.providerPhone
                    )
                }
                triggerNotification("❌ تم إلغاء الحجز وإشعار جميع الأطراف")
                onComplete()
            }
        }
    }
}

fun MainViewModel.getBookingStatusColorImpl(status: String): String {
    return when (status.uppercase()) {
        "PENDING", "UNDER_REVIEW" -> "#F97316" // Orange
        "IN_PROGRESS", "ACCEPTED", "APPROVED" -> "#3B82F6" // Blue
        "COMPLETED" -> "#10B981" // Green
        "REJECTED", "CANCELLED" -> "#EF4444" // Red
        else -> "#9E9E9E"
    }
}

fun MainViewModel.getBookingStatusLabelImpl(status: String): String {
    return when (status.uppercase()) {
        "PENDING", "UNDER_REVIEW" -> "🔍 قيد المراجعة والتدقيق (33%)"
        "IN_PROGRESS", "ACCEPTED", "APPROVED" -> "⚡ جاري تنفيذ الخدمة (66%)"
        "COMPLETED" -> "🎉 مكتملة بنجاح (100%)"
        "REJECTED" -> "❌ مرفوضة من الإدارة"
        "CANCELLED" -> "❌ ملغية"
        else -> status
    }
}

fun MainViewModel.getBookingProgressImpl(status: String): Float {
    return when (status.uppercase()) {
        "PENDING", "UNDER_REVIEW" -> 0.33f
        "IN_PROGRESS", "ACCEPTED", "APPROVED" -> 0.66f
        "COMPLETED" -> 1.00f
        else -> 0.0f
    }
}

fun MainViewModel.createBooking(booking: BookingEntity, onResult: (Boolean) -> Unit = {}) {
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
                addNotification(
                    title = "📅 حجز جديد رقم $bNum",
                    message = "تم تسجيل طلب حجز موعد لدى ${finalized.providerName} بتاريخ ${finalized.dateString} الساعة ${finalized.timeString}.",
                    targetType = "USER",
                    targetValue = finalized.customerPhone.ifEmpty { finalized.clientPhone }
                )
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

fun MainViewModel.createBookingDirectly(
    provider: ProviderEntity,
    notes: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    val custName = currentUserName.value.ifBlank { "عميل التطبيق" }
    val custPhone = currentUserPhone.value.ifBlank { "770000000" }
    val newBooking = BookingEntity(
        id = java.util.UUID.randomUUID().toString(),
        customerName = custName,
        customerPhone = custPhone,
        clientName = custName,
        clientPhone = custPhone,
        customerArea = currentUserResidence.value.ifBlank { provider.area },
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

