package com.example.ui.viewmodels

import com.example.data.BookingEntity
import com.example.utils.BookingStateMachine
import com.google.android.gms.tasks.Tasks

internal fun BookingViewModel.updateBookingStatusImplHelper(
    bookingId: String,
    newStatus: String,
    rejectionReason: String = ""
) {
    db.collection("bookings").document(bookingId).get().addOnSuccessListener { snapshot ->
        val b = snapshot.toObject(BookingEntity::class.java)
        if (b == null) {
            triggerNotificationCallback?.invoke("❌ تعذر العثور على الحجز المطلوب")
            return@addOnSuccessListener
        }
        if (!BookingStateMachine.canTransition(b.status, newStatus)) {
            triggerNotificationCallback?.invoke("⚠️ لا يمكن نقل حالة الحجز من (${BookingStateMachine.getStatusLabel(b.status)}) إلى (${BookingStateMachine.getStatusLabel(newStatus)})")
            return@addOnSuccessListener
        }

        val now = System.currentTimeMillis()
        val updated = b.copy(status = newStatus, rejectionReason = rejectionReason, updatedAt = now)
        db.collection("bookings").document(bookingId).set(updated)
            .addOnSuccessListener {
                _bookings.value = _bookings.value.map { if (it.id == bookingId) updated else it }

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
                        if (createdCh != null && createdCh.id.isNotEmpty()) {
                            db.collection("bookings").document(bookingId).update("relatedChatChannelId", createdCh.id)
                        }
                    }
                }

                val arabicStatusMsg = when (newStatus) {
                    "APPROVED", "ACCEPTED", "IN_PROGRESS" -> "قبول وتأكيد حجزك بنجاح وسيتواصل معك مزود الخدمة قريباً"
                    "PENDING", "UNDER_REVIEW" -> "وضع حجزك قيد المراجعة والتدقيق الإداري"
                    "REJECTED" -> "رفض وإلغاء حجزك" + (if (rejectionReason.isNotBlank()) " لسبب: $rejectionReason" else "")
                    "COMPLETED" -> "إكمال وإنجاز الخدمة بنجاح وتقييم العمل"
                    else -> "تعديل حالة طلب حجزك إلى: $newStatus"
                }

                onAddNotification?.invoke(
                    "📅 تحديث حالة الحجز (رقم ${b.bookingCode.ifBlank { b.bookingNumber.ifBlank { b.id } }})",
                    "عزيزي العميل، تم $arabicStatusMsg للخدمة المقدمة من ${b.providerName}.",
                    "USER",
                    b.customerPhone.ifBlank { b.clientPhone }
                )

                val provTarget = b.providerPhone.ifBlank {
                    val provObj = getProviders?.invoke()?.find { it.id == b.providerId || it.name.trim() == b.providerName.trim() }
                    provObj?.phone?.trim()?.ifBlank { b.providerId } ?: b.providerId
                }
                if (provTarget.isNotBlank()) {
                    onAddNotification?.invoke(
                        "📅 تحديث حالة الحجز (رقم ${b.bookingCode.ifBlank { b.bookingNumber.ifBlank { b.id } }})",
                        "تم تغيير حالة الحجز للعميل ${b.customerName} إلى: $arabicStatusMsg",
                        "PROVIDER",
                        provTarget
                    )
                }

                onAddNotification?.invoke(
                    "📢 تحديث حالة حجز",
                    "تم تحديث حالة الحجز رقم ${b.bookingCode.ifBlank { b.bookingNumber.ifBlank { b.id } }} إلى $arabicStatusMsg للعميل ${b.customerName} ولدى ${b.providerName}.",
                    "ADMIN_ONLY",
                    ""
                )

                val toastMsg = when (newStatus) {
                    "APPROVED", "ACCEPTED", "IN_PROGRESS" -> "⚡ تم قبول وتأكيد الحجز بنجاح"
                    "PENDING", "UNDER_REVIEW" -> "⏳ تم وضع الحجز قيد المراجعة"
                    "REJECTED" -> "❌ تم رفض الحجز وإلغائه"
                    "COMPLETED" -> "🎉 تم إكمال الخدمة بنجاح وتوثيق الإنجاز"
                    else -> "تم تحديث حالة الحجز بنجاح"
                }
                triggerNotificationCallback?.invoke(toastMsg)
            }
            .addOnFailureListener {
                triggerNotificationCallback?.invoke("❌ تعذر تحديث حالة الحجز على السحابة، يرجى المحاولة لاحقاً")
            }
    }.addOnFailureListener {
        triggerNotificationCallback?.invoke("❌ تعذر الاتصال بالخادم لتحديث الحجز")
    }
}

internal fun BookingViewModel.deleteBookingImplHelper(bookingId: String) {
    val b = _bookings.value.find { it.id == bookingId }
    val status = b?.status?.uppercase() ?: ""
    if (b != null && status != "COMPLETED" && status != "CANCELLED" && status != "REJECTED") {
        triggerNotificationCallback?.invoke("⚠️ لا يمكن حذف الحجز إلا إذا كان مكتملاً أو ملغياً")
        return
    }
    db.collection("bookings").document(bookingId).delete()
        .addOnSuccessListener {
            _bookings.value = _bookings.value.filter { it.id != bookingId }
            bookingRepository.deleteBooking(bookingId, {}, {})
            triggerNotificationCallback?.invoke("🗑️ تم حذف الحجز من السجلات")

            val bkCode = b?.bookingCode?.ifBlank { b.bookingNumber.ifBlank { bookingId } } ?: bookingId
            val custName = b?.effectiveCustomerName ?: "عميل"
            onAddNotification?.invoke(
                "🗑️ إشعار إداري: حذف حجز",
                "نوع العملية: (حذف) | رقم الحجز: $bkCode | اسم العميل: $custName",
                "ADMIN_ONLY",
                ""
            )
        }
        .addOnFailureListener { e ->
            val isPerm = e.message?.contains("PERMISSION_DENIED", ignoreCase = true) == true
            triggerNotificationCallback?.invoke(
                if (isPerm) "⚠️ الحذف النهائي للسجلات متاح للإدارة فقط؛ يمكنك إلغاء الحجز بدلاً من ذلك"
                else "❌ تعذر حذف الحجز: ${e.localizedMessage ?: "خطأ في الاتصال"}"
            )
        }
}

internal fun BookingViewModel.deleteAllBookingsImplHelper(customerPhone: String) {
    _bookings.value = _bookings.value.filter { it.customerPhone != customerPhone }
    db.collection("bookings")
        .whereEqualTo("customerPhone", customerPhone)
        .get()
        .addOnSuccessListener { querySnapshot ->
            if (querySnapshot.isEmpty) {
                triggerNotificationCallback?.invoke("🗑️ لا توجد حجوزات لحذفها.")
                return@addOnSuccessListener
            }
            val commitTasks = querySnapshot.documents.chunked(450).map { chunk ->
                val batch = db.batch()
                chunk.forEach { doc -> batch.delete(doc.reference) }
                batch.commit()
            }
            Tasks.whenAllComplete(commitTasks)
                .addOnSuccessListener {
                    triggerNotificationCallback?.invoke("🗑️ تم تصفية وحذف سجل جميع الحجوزات بنجاح.")
                }
        }
}

internal fun BookingViewModel.updateBookingImplHelper(booking: BookingEntity) {
    db.collection("bookings").document(booking.id).set(booking)
    _bookings.value = _bookings.value.map { if (it.id == booking.id) booking else it }
    triggerNotificationCallback?.invoke("💾 تم تحديث بيانات الحجز بنجاح")

    val bkCode = booking.bookingCode.ifBlank { booking.bookingNumber.ifBlank { booking.id } }
    val custName = booking.effectiveCustomerName
    onAddNotification?.invoke(
        "✏️ إشعار إداري: تعديل حجز",
        "نوع العملية: (تعديل) | رقم الحجز: $bkCode | اسم العميل: $custName",
        "ADMIN_ONLY",
        ""
    )
}

internal fun BookingViewModel.attemptCancelBookingImplHelper(
    bookingId: String,
    input: String,
    reason: String = "ملغي بطلب العميل",
    cancelledByParam: String = "USER",
    onResult: (Boolean, String) -> Unit
) {
    val b = _bookings.value.find { it.id == bookingId }
    if (b == null) {
        onResult(false, "❌ الحجز غير موجود")
        return
    }

    bookingRepository.cancelBookingWithSecurity(
        booking = b,
        inputPinOrPassword = input,
        cancellationReason = reason,
        cancelledBy = cancelledByParam,
        onSuccess = {
            onResult(true, "✅ تم إلغاء الحجز بنجاح")
            triggerNotificationCallback?.invoke("✅ تم إلغاء الحجز بنجاح")
        },
        onError = { msg ->
            onResult(false, msg)
        }
    )
}

internal fun BookingViewModel.editBookingByUserHelper(
    bookingId: String,
    newDate: String,
    newTime: String,
    newServiceType: String,
    providerId: String = "",
    providerName: String = ""
) {
    val targetProviderId = providerId.ifEmpty {
        _bookings.value.find { it.id == bookingId }?.providerId ?: ""
    }
    val isTimeSlotTaken = _bookings.value.any {
        it.id != bookingId &&
            it.providerId == targetProviderId &&
            it.effectiveDate.trim() == newDate.trim() &&
            it.effectiveTime.trim() == newTime.trim() &&
            (it.status == "PENDING" || it.status == "APPROVED" || it.status == "ACCEPTED" || it.status == "IN_PROGRESS")
    }
    if (isTimeSlotTaken) {
        triggerToast("❌ عذراً! هذا الوقت (${newTime}) وتاريخ (${newDate}) محجوز بالفعل لدى مقدم الخدمة. يرجى اختيار موعد آخر.")
        return
    }

    val updates = mutableMapOf<String, Any>(
        "date" to newDate,
        "time" to newTime,
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
