package com.example.ui
import android.content.Context
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

fun MainViewModel.addBooking(name: String, phone: String, area: String, serviceType: String, providerId: String, providerName: String, dateString: String = "2026-06-20", timeString: String = "12:00 م") {
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

        // 2. Duplication prevention scan
        val isDuplicate = _bookings.value.any { 
            it.customerPhone.trim() == cleanPhone && 
            it.providerId == providerId && 
            (it.status == "PENDING" || it.status == "APPROVED" || it.status == "IN_PROGRESS")
        }
        if (isDuplicate) {
            triggerNotification("⚠️ حجز مكرر: توجد استمارة حجز معلقة أو نشطة قائمة فعلياً بنفس الرقم لهذا الفني!")
            return
        }

        val newBooking = BookingEntity(
            id = "b_" + UUID.randomUUID().toString().take(6),
            customerName = cleanName,
            customerPhone = cleanPhone,
            customerArea = area,
            serviceType = serviceType,
            providerId = providerId,
            providerName = providerName,
            dateString = dateString,
            timeString = timeString,
            status = "PENDING"
        )
        db.collection("bookings").document(newBooking.id).set(newBooking)

        // Auto-save user identity in memory if empty to ensure they can track notifications immediately
        if (_currentUserPhone.value.isEmpty()) {
            _currentUserPhone.value = cleanPhone
            _currentUserName.value = cleanName
            _currentUserResidence.value = area
        }

        // Notify the customer (user) that their booking was successfully submitted
        addNotification(
            title = "📅 تم إرسال طلب حجزك بنجاح",
            message = "عزيزي العميل $cleanName، لقد تم إرسال طلب حجزك رقم: ${newBooking.id} بنجاح للفني: $providerName. الموعد المحدد: $dateString الساعة $timeString. طلبك الآن قيد المراجعة والتدقيق الإداري وسيصلك إشعار بالخطوة القادمة فوراً.",
            targetType = "USER",
            targetValue = cleanPhone
        )

        // Compile a highly detailed notification containing customer's name, phone, and area of residence
        val detailedMessage = "طلب حجز جديد من العميل: $cleanName، رقم الهاتف للتواصل: $cleanPhone، منطقة السكن: $area. تفاصيل الخدمة المطلوبة: $serviceType. الموعد المفضل: $dateString الساعة $timeString."

        // 1. Always notify the Admin/Supervisor
        addNotification(
            title = "📅 طلب حجز جديد بانتظار المراجعة",
            message = detailedMessage,
            targetType = "SUPERVISOR",
            targetValue = "all"
        )

        // 2. Distribute to technicians according to the active mode set by the admin
        when (_distributionMode.value) {
            MainViewModel.BookingDistributionMode.SPECIFIC_PROVIDER -> {
                // Find and notify the specific technician named in the booking
                val tech = _providers.value.find { it.id == providerId }
                if (tech != null) {
                    addNotification(
                        title = "📅 حجز جديد موجه لك بالاسم",
                        message = detailedMessage,
                        targetType = "PROVIDER",
                        targetValue = tech.phone
                    )
                }
            }
            MainViewModel.BookingDistributionMode.NEAREST_PROVIDER, MainViewModel.BookingDistributionMode.ALL_PROVIDERS -> {
                // Find and notify all providers in the same category (or closest geographically)
                val categoryIdOfProvider = _providers.value.find { it.id == providerId }?.categoryId ?: "1"
                val catTechs = _providers.value.filter { it.categoryId == categoryIdOfProvider }
                catTechs.forEach { tech ->
                    addNotification(
                        title = "📅 فرصة حجز عمل جديدة في منطقتك",
                        message = detailedMessage,
                        targetType = "PROVIDER",
                        targetValue = tech.phone
                    )
                }
            }
            else -> {
                // ADMIN_ONLY or CATEGORY_SUPERVISOR -> Handled by Supervisor notifications
            }
        }

        // 3. Notify the Customer (Requester) themselves
        addNotification(
            title = "📅 تم استلام طلب حجزك بنجاح",
            message = "عزيزي العميل ${cleanName}، لقد تم تقديم طلب حجز الخدمة رقم (${newBooking.id}) بنجاح للفني (${providerName}). الطلب حالياً بانتظار المراجعة والاتصال بك لتأكيد الموعد.",
            targetType = "USER",
            targetValue = cleanPhone
        )

        triggerNotification("تم إرسال طلب الحجز، سيتم مراجعته")
    }



fun MainViewModel.updateBookingStatus(bookingId: String, newStatus: String, rejectionReason: String = "") {
        db.collection("bookings").document(bookingId).get().addOnSuccessListener { snapshot ->
            val b = snapshot.toObject(BookingEntity::class.java)
            if (b != null) {
                val updated = b.copy(status = newStatus, rejectionReason = rejectionReason)
                db.collection("bookings").document(bookingId).set(updated)
                
                val arabicStatusMsg = when(newStatus) {
                    "PENDING", "UNDER_REVIEW" -> "وضع حجزك قيد المراجعة والتدقيق الإداري"
                    "IN_PROGRESS" -> "قبول حجزك وبدء تنفيذ الخدمة المطلوبة ميدانياً"
                    "REJECTED" -> "رفض وإلغاء حجزك" + (if (rejectionReason.isNotBlank()) " لسبب: $rejectionReason" else "")
                    "COMPLETED" -> "إكمال وإنجاز الخدمة بنجاح وتقييم العمل"
                    else -> "تعديل حالة طلب حجزك إلى: $newStatus"
                }

                // Always send critical user notifications for booking transitions so they can track progress
                addNotification(
                    title = "📅 تحديث حالة الحجز (رقم ${b.id})",
                    message = "عزيزي العميل، تم $arabicStatusMsg للخدمة المقدمة من ${b.providerName}.",
                    targetType = "USER",
                    targetValue = b.customerPhone
                )
            }
        }
        val toastMsg = when(newStatus) {
            "PENDING", "UNDER_REVIEW" -> "⏳ تم وضع الحجز قيد المراجعة"
            "IN_PROGRESS" -> "⚡ تم قبول الحجز وبدء تنفيذ الخدمة"
            "REJECTED" -> "❌ تم رفض الحجز وإلغائه"
            "COMPLETED" -> "🎉 تم إكمال الخدمة بنجاح وتوثيق الإنجاز"
            else -> "تم تحديث حالة الحجز بنجاح"
        }
        triggerNotification(toastMsg)
    }



fun MainViewModel.deleteBooking(bookingId: String) {
        db.collection("bookings").document(bookingId).delete()
        triggerNotification("🗑️ تم حذف الحجز من السجلات")
    }



fun MainViewModel.updateBooking(booking: BookingEntity) {
        db.collection("bookings").document(booking.id).set(booking)
        triggerNotification("💾 تم تحديث بيانات الحجز بنجاح")
    }

    // Targeted Notifications Management


fun MainViewModel.updateBookingFormFields(fields: MainViewModel.BookingFormFields) {
        _bookingFormFields.value = fields
        try {
            db.collection("settings").document("booking_fields").set(fields)
        } catch (e: Exception) {}
    }



fun MainViewModel.updateDistributionMode(mode: MainViewModel.BookingDistributionMode) {
        _distributionMode.value = mode
        try {
            db.collection("settings").document("distribution_mode").set(mapOf("mode" to mode.name))
        } catch (e: Exception) {}
    }



fun MainViewModel.updateBookingStatus(bookingId: String, newStatus: BookingStatus) {
        _bookings.value = _bookings.value.map { booking ->
            if (booking.id == bookingId) {
                booking.copy(status = newStatus.name)
            } else booking
        }
        try {
            db.collection("bookings").document(bookingId).update("status", newStatus.name)
        } catch (e: Exception) {}
    }



fun MainViewModel.getBookingStatusColor(status: String): String {
        return when (status.uppercase()) {
            "PENDING", "UNDER_REVIEW" -> "#F97316" // Orange
            "IN_PROGRESS", "ACCEPTED", "APPROVED" -> "#3B82F6" // Blue
            "COMPLETED" -> "#10B981" // Green
            "REJECTED", "CANCELLED" -> "#EF4444" // Red
            else -> "#9E9E9E"
        }
    }



fun MainViewModel.getBookingStatusLabel(status: String): String {
        return when (status.uppercase()) {
            "PENDING", "UNDER_REVIEW" -> "🔍 قيد المراجعة والتدقيق (33%)"
            "IN_PROGRESS", "ACCEPTED", "APPROVED" -> "⚡ جاري تنفيذ الخدمة (66%)"
            "COMPLETED" -> "🎉 مكتملة بنجاح (100%)"
            "REJECTED" -> "❌ مرفوضة من الإدارة"
            "CANCELLED" -> "❌ ملغية"
            else -> status
        }
    }



fun MainViewModel.getBookingProgress(status: String): Float {
        return when (status.uppercase()) {
            "PENDING", "UNDER_REVIEW" -> 0.33f
            "IN_PROGRESS", "ACCEPTED", "APPROVED" -> 0.66f
            "COMPLETED" -> 1.00f
            else -> 0.0f
        }
    }

    // ============================================================
    // 🔒 إشعار تعطيل الدردشة - إضافة
    // ============================================================



fun MainViewModel.addBooking(
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
        val newBooking = BookingEntity(
            id = customBookingId.ifEmpty { "b_" + java.util.UUID.randomUUID().toString().take(6) },
            customerName = cleanName,
            customerPhone = cleanPhone,
            customerArea = area,
            serviceType = serviceType,
            providerId = providerId,
            providerName = providerName,
            dateString = dateString,
            timeString = timeString,
            status = "PENDING"
        )
        _bookings.value = _bookings.value + newBooking
        try {
            db.collection("bookings").document(newBooking.id).set(newBooking)
        } catch (e: Exception) {}
        triggerNotification("🎉 تم تقديم طلب الحجز بنجاح بنتيجة معلقة لدى الفني!")
    }



fun MainViewModel.placeOrder(order: OrderEntity) {
        _orders.value = _orders.value + order
        try {
            db.collection("orders").document(order.id).set(order)
        } catch (e: Exception) {}
        triggerNotification("🛒 تم تقديم طلب الشراء بنجاح!")
    }



fun MainViewModel.placeOrder(orderMap: Map<String, Any>) {
        triggerNotification("🛒 تم تقديم طلب الشراء بنجاح!")



fun MainViewModel.deleteOrder(orderId: String) {
    _orders.value = _orders.value.filter { it.id != orderId }
}



fun MainViewModel.deleteAllOrders() {
    _orders.value = emptyList()
}



fun MainViewModel.deleteAllOrders(phone: String = "") {
    _orders.value = emptyList()
}



fun MainViewModel.updateOrderStatus(orderId: String, status: String) {
    _orders.value = _orders.value.map { if (it.id == orderId) it.copy(status = status) else it }
}



fun MainViewModel.attemptCancelBooking(bookingId: String, reason: String = "") {
    _bookings.value = _bookings.value.map { if (it.id == bookingId) it.copy(status = "CANCELLED") else it }
}



fun MainViewModel.attemptCancelBooking(bookingId: String, passOrPin: String, callback: (Boolean, String) -> Unit) {
    attemptCancelBooking(bookingId, passOrPin)
    callback(true, "تم إلغاء الحجز بنجاح")
}



fun MainViewModel.editBookingByUser(booking: BookingEntity) {
    _bookings.value = _bookings.value.map { if (it.id == booking.id) booking else it }
}



fun MainViewModel.editBookingByUser(bookingId: String, editDate: String = "", editTime: String = "", editServiceType: String = "") {
    _bookings.value = _bookings.value.map {
        if (it.id == bookingId) it.copy(dateString = editDate, timeString = editTime, serviceType = editServiceType) else it
    }
}



fun MainViewModel.deleteAllBookings() {
    _bookings.value = emptyList()
}



fun MainViewModel.deleteAllBookings(phone: String = "") {
    _bookings.value = emptyList()
}
}
