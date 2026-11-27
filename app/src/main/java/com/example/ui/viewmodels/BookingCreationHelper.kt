package com.example.ui.viewmodels

import android.os.Bundle
import com.example.MyApplication
import com.example.data.BookingEntity
import com.example.data.ProviderEntity
import com.example.utils.DateFormatter
import com.example.utils.ScheduleManager
import com.example.utils.SecureHasher
import com.google.android.gms.tasks.Tasks
import java.security.SecureRandom
import java.text.NumberFormat
import java.util.Locale
import java.util.UUID

internal fun BookingViewModel.createBookingHelper(
    booking: BookingEntity,
    onResult: (Boolean) -> Unit = {}
) {
    if (booking.isRecurring && booking.recurrenceRule != "NONE") {
        createRecurringBookingsHelper(booking, onResult)
        return
    }
    val bId = booking.id.ifEmpty { UUID.randomUUID().toString() }
    val secureRandom = SecureRandom()
    val bNum = booking.bookingNumber.ifEmpty { "YEM-${10000 + secureRandom.nextInt(90000)}" }
    val bPass = "${1000 + secureRandom.nextInt(9000)}"
    val hasPreHashedPin = booking.pinCode.isNotBlank()
    val finalized = booking.copy(
        id = bId,
        bookingNumber = bNum,
        pinCode = if (hasPreHashedPin) booking.pinCode else SecureHasher.hashPin(bPass),
        createdAt = if (booking.createdAt == 0L) System.currentTimeMillis() else booking.createdAt,
        updatedAt = System.currentTimeMillis()
    )
    safeBookingFirestoreCall(
        operation = { onSuccess, onFailure ->
            db.collection("bookings").document(bId).set(finalized)
                .addOnSuccessListener {
                    _bookings.value = _bookings.value + finalized
                    val custPhone = finalized.customerPhone.ifEmpty { finalized.clientPhone }
                    val custName = finalized.customerName.ifEmpty { finalized.clientName.ifEmpty { "العميل" } }
                    val provPhone = finalized.providerPhone.ifEmpty {
                        getProviders?.invoke()?.find { it.id == finalized.providerId || it.name.trim() == finalized.providerName.trim() }?.phone?.trim() ?: finalized.providerId
                    }

                    onAddNotification?.invoke(
                        "📅 حجز جديد رقم $bNum",
                        "تم تسجيل طلب حجز موعد لدى ${finalized.providerName} بنجاح. رقم الحجز: $bNum.",
                        "USER",
                        custPhone
                    )

                    if (provPhone.isNotBlank()) {
                        onAddNotification?.invoke(
                            "⚡ حجز جديد وارد رقم $bNum",
                            "العميل $custName ($custPhone) قام بحجز خدمة (${finalized.serviceType}) لديك.",
                            "PROVIDER",
                            provPhone
                        )
                    }

                    onAddNotification?.invoke(
                        "📢 حجز جديد مسجل في النظام",
                        "نوع العملية: (إنشاء حجز) | رقم الحجز: $bNum | العميل: $custName ($custPhone) لدى: ${finalized.providerName}",
                        "ADMIN_ONLY",
                        ""
                    )
                    onSuccess()
                    onResult(true)
                }
                .addOnFailureListener { e ->
                    onFailure(e)
                    onResult(false)
                }
        },
        onSuccess = {
            if (hasPreHashedPin) {
                triggerToast("✅ تم إنشاء الحجز بنجاح")
            } else {
                triggerToast("✅ تم الحجز #$bNum | احتفظ برمز PIN: $bPass")
            }
        },
        onError = { triggerToast("⚠️ تعذر حفظ الحجز على السحابة، يرجى التحقق من الاتصال") },
        errorMessage = "فشل إنشاء الحجز"
    )
}

internal fun BookingViewModel.createRecurringBookingsHelper(
    baseBooking: BookingEntity,
    onResult: (Boolean) -> Unit
) {
    val parentId = UUID.randomUUID().toString()
    val secureRandom = SecureRandom()
    val dates = ScheduleManager.calculateRecurringDates(baseBooking.effectiveDate, baseBooking.recurrenceRule)
    if (dates.isEmpty()) {
        onResult(false)
        return
    }

    val newBookings = mutableListOf<BookingEntity>()
    val batches = dates.chunked(450).mapIndexed { chunkIdx, chunkDates ->
        val batch = db.batch()
        chunkDates.forEachIndexed { localIdx, dateStr ->
            val index = chunkIdx * 450 + localIdx
            val bId = "${parentId}_${index}"
            val bNum = "YEM-${10000 + secureRandom.nextInt(90000)}"
            val bPass = "${1000 + secureRandom.nextInt(9000)}"
            val b = baseBooking.copy(
                id = bId,
                parentId = parentId,
                date = dateStr,
                dateString = dateStr,
                bookingNumber = bNum,
                pinCode = SecureHasher.hashPin(bPass),
                isRecurring = true,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            newBookings.add(b)
            batch.set(db.collection("bookings").document(bId), b)
        }
        batch
    }

    safeBookingFirestoreCall(
        operation = { onSuccess, onFailure ->
            Tasks.whenAllComplete(batches.map { it.commit() })
                .addOnSuccessListener { tasks ->
                    val firstError = tasks.firstOrNull { !it.isSuccessful }?.exception
                    if (firstError != null) {
                        onFailure(firstError)
                        onResult(false)
                        return@addOnSuccessListener
                    }
                    _bookings.value = _bookings.value + newBookings

                    val custPhone = baseBooking.effectiveCustomerPhone
                    val custName = baseBooking.effectiveCustomerName
                    val provPhone = baseBooking.providerPhone.ifEmpty {
                        getProviders?.invoke()?.find { it.id == baseBooking.providerId || it.name.trim() == baseBooking.providerName.trim() }?.phone?.trim() ?: baseBooking.providerId
                    }

                    val msg = "لديك سلسلة حجوزات جديدة متكررة (${newBookings.size} مواعيد) من $custName برقم $custPhone"
                    if (provPhone.isNotBlank()) {
                        onAddNotification?.invoke("سلسلة حجوزات جديدة", msg, "PROVIDER", provPhone)
                    }

                    val auditId = UUID.randomUUID().toString()
                    val auditLog = mapOf(
                        "id" to auditId,
                        "action" to "CREATE_RECURRING_BOOKING",
                        "details" to "تم إنشاء ${newBookings.size} حجوزات للمزود ${baseBooking.providerName}",
                        "timestamp" to System.currentTimeMillis()
                    )
                    db.collection("security_audit_logs").document(auditId).set(auditLog)

                    onSuccess()
                    onResult(true)
                }
                .addOnFailureListener { e ->
                    onFailure(e)
                    onResult(false)
                }
        },
        onSuccess = { triggerToast("✅ تم إنشاء السلسلة المتكررة بنجاح") },
        onError = { triggerToast("⚠️ خطأ في المزامنة، يرجى التحقق من اتصالك") },
        errorMessage = "فشل إنشاء الحجوزات المتكررة"
    )
}

internal fun BookingViewModel.addBookingImplHelper(
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

    val isTimeSlotTaken = _bookings.value.any {
        it.providerId == providerId &&
            it.effectiveDate.trim() == dateString.trim() &&
            it.effectiveTime.trim() == timeString.trim() &&
            (it.status == "PENDING" || it.status == "APPROVED" || it.status == "ACCEPTED" || it.status == "IN_PROGRESS")
    }
    if (isTimeSlotTaken) {
        triggerNotificationCallback?.invoke("⚠️ عذراً، هذا الموعد ($dateString في $timeString) محجوز مسبقاً لدى هذا الفني. يرجى اختيار وقت آخر!")
        return
    }

    var discountPercent = 0.0
    if (couponCode.isNotBlank()) {
        val couponsList = getCoupons?.invoke() ?: emptyList()
        val coupon = couponsList.find { it.code.trim().uppercase() == couponCode.trim().uppercase() && it.status == "ACTIVE" }
        if (coupon != null) {
            val now = System.currentTimeMillis()
            if (now <= coupon.expiryTimestamp) {
                discountPercent = coupon.discountPercentage.toDouble()
                val updatedCount = coupon.usedCount + 1
                db.collection("coupons").document(coupon.id).update("usedCount", updatedCount)
                triggerNotificationCallback?.invoke("🎫 تم تطبيق كوبون الخصم بنجاح! خصم بقيمة ${coupon.discountPercentage}%")
            } else {
                triggerNotificationCallback?.invoke("⚠️ الكوبون المستخدم منتهي الصلاحية")
            }
        } else {
            triggerNotificationCallback?.invoke("⚠️ الكوبون غير صحيح أو غير مفعل")
        }
    }

    val providersList = getProviders?.invoke() ?: emptyList()
    val prov = providersList.find { it.id == providerId || (providerId.isBlank() && it.name.trim() == providerName.trim()) }
    val basePrice = prov?.previewPrice ?: 0.0
    val finalPrice = if (discountPercent > 0.0) {
        basePrice * (1.0 - (discountPercent / 100.0))
    } else {
        basePrice
    }

    val effectiveProviderPhone = prov?.phone?.trim()?.ifBlank {
        providersList.find { it.name.trim() == providerName.trim() }?.phone?.trim() ?: ""
    } ?: ""

    val secureRandom = SecureRandom()
    val finalBookingId = if (customBookingId.isNotBlank()) customBookingId else UUID.randomUUID().toString()
    val finalBookingNumber = "B-${100000 + secureRandom.nextInt(900000)}"
    val generatedPass = if (customPassword.isNotBlank()) customPassword else "${1000 + secureRandom.nextInt(9000)}"

    val newBooking = BookingEntity(
        id = finalBookingId,
        customerName = cleanName,
        customerPhone = cleanPhone,
        customerArea = area,
        serviceType = serviceType,
        providerId = providerId,
        providerName = providerName,
        providerPhone = effectiveProviderPhone,
        date = dateString,
        time = timeString,
        dateString = dateString,
        timeString = timeString,
        status = "PENDING",
        bookingNumber = finalBookingNumber,
        pinCode = if (pinCode.isNotBlank()) pinCode else SecureHasher.hashPin(generatedPass),
        totalAmount = finalPrice,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )

    try {
        db.collection("bookings").document(finalBookingId).set(newBooking)
            .addOnSuccessListener {
                _bookings.value = _bookings.value + newBooking

                try {
                    val bundle = Bundle().apply {
                        putString("service_type", serviceType)
                        putString("provider_name", providerName)
                        putDouble("price", finalPrice)
                        putString("booking_number", finalBookingNumber)
                    }
                    MyApplication.logFirebaseEvent("submit_booking", bundle)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                if (getCurrentUserPhone?.invoke()?.isEmpty() == true) {
                    setCurrentUserPhone?.invoke(cleanPhone)
                    setCurrentUserName?.invoke(cleanName)
                    setCurrentUserResidence?.invoke(area)
                }

                val bookingRouting = appState._settings.value.bookingRouting
                val sendUserNotif = bookingRouting == "BOTH" || bookingRouting == "PROVIDER" || bookingRouting == "USER" || bookingRouting.isBlank()
                val sendProviderNotif = bookingRouting == "BOTH" || bookingRouting == "PROVIDER" || bookingRouting.isBlank()
                val sendAdminNotif = bookingRouting == "BOTH" || bookingRouting == "ADMIN" || bookingRouting.isBlank()

                if (sendUserNotif) {
                    onAddNotification?.invoke(
                        "📅 تم تسجيل طلب حجزك رقم $finalBookingNumber",
                        "مرحباً بك $cleanName، تم استقبال طلب الحجز لدى $providerName بنجاح برقم #$finalBookingNumber.",
                        "USER",
                        cleanPhone
                    )
                }

                if (sendProviderNotif) {
                    val formattedPrice = NumberFormat.getNumberInstance(Locale.US).format(finalPrice)
                    val technicianTarget = effectiveProviderPhone.ifBlank { providerId.ifBlank { "PROVIDER" } }
                    onAddNotification?.invoke(
                        "⚡ حجز عاجل جديد رقم $finalBookingNumber",
                        "العميل $cleanName ($cleanPhone) من ($area) حجز خدمة ($serviceType) لدى $providerName بموعد $dateString $timeString. سعر الخدمة: $formattedPrice ريال يمني.",
                        "PROVIDER",
                        technicianTarget
                    )
                }

                if (sendAdminNotif) {
                    onAddNotification?.invoke(
                        "📢 حجز جديد مسجل في النظام",
                        "العميل $cleanName حجز لدى $providerName في مدينة $area. رقم الحجز: $finalBookingNumber.",
                        "ADMIN_ONLY",
                        ""
                    )
                }

                triggerNotificationCallback?.invoke("✅ تم إنشاء الحجز #$finalBookingNumber | احتفظ برمز الإلغاء: $generatedPass")
            }
            .addOnFailureListener { e ->
                triggerNotificationCallback?.invoke("❌ فشل الحجز: ${e.message}")
            }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

internal fun BookingViewModel.createBookingDirectlyHelper(
    provider: ProviderEntity,
    notes: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    val custPhone = getCurrentUserPhone?.invoke()?.trim().orEmpty()
    if (custPhone.isBlank()) {
        onError("⚠️ يرجى تسجيل رقم هاتفك أولاً قبل إرسال الحجز المباشر")
        return
    }
    val custName = getCurrentUserName?.invoke()?.ifBlank { "عميل ($custPhone)" } ?: "عميل ($custPhone)"
    val bookingDate = DateFormatter.formatDateDash(System.currentTimeMillis())
    val bookingTime = DateFormatter.formatTime24(System.currentTimeMillis() + 3600 * 1000L)
    val secureRandom = SecureRandom()
    val rawPin = "${1000 + secureRandom.nextInt(9000)}"
    val bNum = "YEM-${10000 + secureRandom.nextInt(90000)}"
    val bId = UUID.randomUUID().toString()

    val directBooking = BookingEntity(
        id = bId,
        bookingNumber = bNum,
        bookingCode = bNum,
        providerId = provider.id,
        providerName = provider.name,
        providerPhone = provider.phone,
        customerName = custName,
        customerPhone = custPhone,
        clientName = custName,
        clientPhone = custPhone,
        clientId = custPhone,
        serviceType = provider.profession.ifBlank { provider.customCategoryName.ifBlank { "حجز فوري من الخريطة" } },
        category = provider.customCategoryName.ifBlank { provider.profession },
        serviceDetails = notes.ifBlank { "حجز مباشر من خريطة التطبيق" },
        date = bookingDate,
        dateString = bookingDate,
        time = bookingTime,
        timeString = bookingTime,
        status = "PENDING",
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )

    bookingRepository.createBooking(
        booking = directBooking,
        rawPasswordPin = rawPin,
        onSuccess = { createdBooking ->
            _bookings.value = _bookings.value.filter { it.id != createdBooking.id } + createdBooking

            onAddNotification?.invoke(
                "📅 حجز خريطة جديد #${createdBooking.bookingNumber}",
                "تم إرسال طلب حجزك بنجاح إلى ${createdBooking.providerName} برقم #${createdBooking.bookingNumber}.",
                "USER",
                custPhone
            )
            if (createdBooking.providerPhone.isNotBlank()) {
                onAddNotification?.invoke(
                    "⚡ طلب حجز جديد من الخريطة #${createdBooking.bookingNumber}",
                    "قام العميل $custName ($custPhone) بطلب حجز مباشر من الخريطة لديك.",
                    "PROVIDER",
                    createdBooking.providerPhone
                )
            }
            onAddNotification?.invoke(
                "📢 حجز خريطة جديد",
                "حجز خريطة جديد #${createdBooking.bookingNumber} للعميل $custName لدى ${createdBooking.providerName}.",
                "ADMIN_ONLY",
                "ALL"
            )

            triggerToast("🎉 تم الحجز #${createdBooking.bookingNumber} | رمز PIN: $rawPin")
            onSuccess()
        },
        onError = { errorMsg ->
            onError(errorMsg)
        }
    )
}
