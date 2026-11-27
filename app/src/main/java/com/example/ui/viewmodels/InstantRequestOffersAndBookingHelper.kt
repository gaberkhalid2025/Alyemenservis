package com.example.ui.viewmodels

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.example.data.BookingEntity
import com.example.data.NotificationEntity
import com.example.data.models.ChannelType
import com.example.data.models.InstantRequestEntity
import com.example.data.models.MediaType
import com.example.data.models.RequestOfferEntity
import com.example.security.BookingSecurityHelper
import com.example.utils.AppResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

internal fun InstantRequestViewModel.submitOfferForRequestHelper(
    requestId: String,
    requestCode: String,
    technicianId: String,
    technicianName: String,
    technicianPhone: String,
    technicianAvatar: String,
    technicianRating: Float,
    price: Double,
    estimatedArrivalTime: String = "خلال 30 دقيقة",
    estimatedDuration: String = "ساعتان",
    notes: String = "",
    onResult: (Boolean, String) -> Unit = { _, _ -> }
) {
    if (price <= 0.0 || price.isNaN() || price.isInfinite()) {
        val errMsg = "⚠️ السعر المقترح يجب أن يكون أكبر من صفر"
        _uiState.value = InstantUiState.Error(errMsg)
        triggerNotification?.invoke(errMsg)
        onResult(false, errMsg)
        return
    }
    val existingOffers = _requestOffers.value.filter { it.requestId == requestId }
    if (existingOffers.any { it.technicianId == technicianId }) {
        val dupMsg = "⚠️ قمت بتقديم عرض سابق لهذا الطلب ولا يمكن تقديم عرض مكرر!"
        _uiState.value = InstantUiState.Error(dupMsg)
        triggerNotification?.invoke(dupMsg)
        onResult(false, dupMsg)
        return
    }

    _uiState.value = InstantUiState.Loading
    val offerId = UUID.randomUUID().toString()
    val offer = RequestOfferEntity(
        id = offerId,
        requestId = requestId,
        requestCode = requestCode,
        technicianId = technicianId,
        technicianName = technicianName.ifBlank { "مقدم خدمة معتمد" },
        technicianPhone = technicianPhone,
        technicianAvatar = technicianAvatar,
        technicianRating = technicianRating,
        price = price,
        estimatedArrivalTime = estimatedArrivalTime,
        estimatedDuration = estimatedDuration,
        notes = if (notes.startsWith("🚨")) notes else "🚨 استجابة طوارئ: $notes",
        status = "PENDING",
        createdAt = System.currentTimeMillis()
    )

    _requestOffers.value = _requestOffers.value + offer
    repository.submitOffer(
        offer = offer,
        onSuccess = {
            _uiState.value = InstantUiState.Success("تم تقديم عرض السعر بنجاح")
            triggerNotification?.invoke("💰 تم تقديم عرض السعر ($price ر.ي) بنجاح للطلب $requestCode!")
            val targetReq = _instantRequests.value.find { it.id == requestId }
            if (targetReq != null && targetReq.userPhone.isNotBlank()) {
                addNotification?.invoke(
                    "💰 عرض جديد من $technicianName على طلبك $requestCode",
                    "قدم $technicianName عرض سعر قدره $price ر.ي بوقت وصول $estimatedArrivalTime. افتح العروض لمقارنة الخيارات والاختيار.",
                    "USER",
                    targetReq.userPhone
                )
            }
            onResult(true, "تم تقديم عرض السعر بنجاح")
        },
        onError = { err ->
            _requestOffers.value = _requestOffers.value.filterNot { it.id == offerId }
            val failMsg = "تعذر إرسال العرض: $err"
            _uiState.value = InstantUiState.Error(failMsg)
            triggerNotification?.invoke("❌ $failMsg")
            onResult(false, failMsg)
        }
    )
}

internal fun InstantRequestViewModel.acceptOfferHelper(
    requestId: String,
    offerId: String,
    providerPhone: String,
    onResult: (Boolean, String?) -> Unit
) {
    viewModelScope.launch(Dispatchers.IO) {
        _uiState.value = InstantUiState.Loading
        try {
            val requestSnapshot = firestore.collection("instant_requests").document(requestId).get().await()
            val req = requestSnapshot.toObject(InstantRequestEntity::class.java)

            val offerSnapshot = firestore.collection("instant_requests").document(requestId)
                .collection("offers").document(offerId).get().await()
            val offer = offerSnapshot.toObject(RequestOfferEntity::class.java)

            val techId = offer?.technicianId ?: providerPhone
            val techName = offer?.technicianName ?: "فني الخدمة"
            val techAvatar = offer?.technicianAvatar ?: ""
            val price = offer?.price ?: 0.0

            repository.acceptOffer(
                requestId = requestId,
                offerId = offerId,
                providerId = techId,
                providerName = techName,
                providerPhone = providerPhone,
                acceptedPrice = price,
                onSuccess = {
                    viewModelScope.launch(Dispatchers.IO) {
                        try {
                            val customerId = req?.userId?.ifEmpty { req.userPhone } ?: "customer"
                            val customerName = req?.userName?.ifEmpty { "عميل" } ?: "عميل"

                            val chatResult = chatRepo.getOrCreateChannel(
                                currentUserId = customerId,
                                currentUserName = customerName,
                                currentUserPhoto = "",
                                otherUserId = techId,
                                otherUserName = techName,
                                otherUserPhoto = techAvatar,
                                type = ChannelType.PRIVATE,
                                relatedEntityId = requestId,
                                relatedEntityType = "URGENT_REQUEST"
                            )

                            if (chatResult is AppResult.Success) {
                                chatRepo.sendMessage(
                                    channelId = chatResult.data.id,
                                    senderId = customerId,
                                    senderName = "إشعار النظام ($customerName)",
                                    messageText = "[نظام] تم قبول عرضك للطلب العاجل رقم ${requestId.take(6)}. تم فتح هذه المحادثة لتنسيق العمل.",
                                    mediaType = MediaType.TEXT
                                )
                            }
                        } catch (_: Exception) {}
                    }
                    _uiState.value = InstantUiState.Success("تم قبول العرض بنجاح!")
                    onResult(true, "تم قبول العرض بنجاح")
                },
                onError = { err ->
                    _uiState.value = InstantUiState.Error(err)
                    onResult(false, err)
                }
            )
        } catch (e: Exception) {
            val err = e.localizedMessage ?: "فشل قبول العرض"
            _uiState.value = InstantUiState.Error(err)
            onResult(false, err)
        }
    }
}

internal fun InstantRequestViewModel.acceptRequestOfferHelper(
    req: InstantRequestEntity,
    offer: RequestOfferEntity
) {
    acceptOffer(req.id, offer.id, offer.technicianPhone) { success, _ ->
        if (success) {
            triggerNotification?.invoke("🎉 تم قبول عرض ${offer.technicianName} بنجاح وتحويل الطلب إلى حجز مؤكد!")
            addNotification?.invoke(
                "🎉 تم اختيار عرضك للطلب ${req.requestCode}",
                "تهانينا ${offer.technicianName}! اختار العميل ${req.userName} عرضك بسعر ${offer.price} ر.ي للطلب ${req.requestCode}. يمكنك البدء في التواصل والمباشرة الآن.",
                "PROVIDER",
                offer.technicianPhone
            )
            if (req.userPhone.isNotBlank()) {
                addNotification?.invoke(
                    "🎉 تم قبول عرض الفني ${offer.technicianName}",
                    "تم قبول العرض بنجاح وبدء المحادثة المباشرة مع الفني لتنسيق العمل للطلب ${req.requestCode}.",
                    "USER",
                    req.userPhone
                )
            }
            addNotification?.invoke(
                "📢 قبول عرض طلب عاجل",
                "تم قبول عرض الفني ${offer.technicianName} للطلب ${req.requestCode} بقيمة ${offer.price} ر.ي للعميل ${req.userName}.",
                "ADMIN_ONLY",
                "ALL"
            )
        }
    }
}

internal fun InstantRequestViewModel.cancelInstantRequestHelper(
    requestId: String,
    userPin: String = "",
    context: Context? = null,
    cancelReason: String = "",
    onResult: (Boolean, String?) -> Unit = { _, _ -> }
) {
    if (context != null && BookingSecurityHelper.isBookingLocked(context, requestId)) {
        val remainingSecs = BookingSecurityHelper.getRemainingLockoutSeconds(context, requestId)
        val msg = "الحساب مقفل مؤقتاً لأسباب أمنية بسبب إدخال PIN خاطئ 3 مرات. انتظر $remainingSecs ثانية."
        _uiState.value = InstantUiState.Error(msg)
        onResult(false, msg)
        return
    }

    if (userPin.isBlank()) {
        if (context != null) {
            BookingSecurityHelper.recordFailedAttempt(context, requestId)
        }
        val msg = "يرجى إدخال رمز الحماية (PIN) لإلغاء الطلب"
        _uiState.value = InstantUiState.Error(msg)
        onResult(false, msg)
        return
    }

    _uiState.value = InstantUiState.Loading

    repository.cancelInstantRequest(
        requestId = requestId,
        userPin = userPin.trim(),
        cancelReason = cancelReason,
        onSuccess = {
            if (context != null) {
                BookingSecurityHelper.resetAttempts(context, requestId)
            }
            _uiState.value = InstantUiState.Success("تم إلغاء الطلب الفوري بنجاح")
            triggerNotification?.invoke("🚫 تم إلغاء الطلب الفوري بنجاح.")
            onResult(true, "تم إلغاء الطلب بنجاح")
        },
        onError = { err ->
            if (context != null) {
                BookingSecurityHelper.recordFailedAttempt(context, requestId)
            }
            _uiState.value = InstantUiState.Error(err)
            triggerNotification?.invoke("❌ خطأ: $err")
            onResult(false, err)
        }
    )
}

internal fun InstantRequestViewModel.loadOfferAndRequestHelper(
    offerId: String,
    onResult: (RequestOfferEntity?, InstantRequestEntity?) -> Unit = { _, _ -> }
) {
    if (offerId.isBlank()) return
    viewModelScope.launch(Dispatchers.IO) {
        try {
            var fetchedOffer: RequestOfferEntity? = _requestOffers.value.find { it.id == offerId }
            var fetchedReq: InstantRequestEntity? = _selectedRequest.value

            if (fetchedOffer == null) {
                val groupSnap = firestore.collectionGroup("offers").whereEqualTo("id", offerId).limit(1).get().await()
                if (!groupSnap.isEmpty) {
                    val doc = groupSnap.documents.first()
                    fetchedOffer = doc.toObject(RequestOfferEntity::class.java)?.copy(id = doc.id)
                }
            }

            if (fetchedOffer != null && (fetchedReq == null || fetchedReq.id != fetchedOffer.requestId)) {
                val reqSnap = firestore.collection("instant_requests").document(fetchedOffer.requestId).get().await()
                if (reqSnap.exists()) {
                    fetchedReq = reqSnap.toObject(InstantRequestEntity::class.java)?.copy(id = reqSnap.id)
                }
            }
            _selectedOffer.value = fetchedOffer
            _selectedRequest.value = fetchedReq
            withContext(Dispatchers.Main) {
                onResult(fetchedOffer, fetchedReq)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onResult(null, null)
            }
        }
    }
}

internal fun InstantRequestViewModel.confirmOfferSelectionHelper(
    booking: BookingEntity,
    curOffer: RequestOfferEntity,
    curReq: InstantRequestEntity,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    viewModelScope.launch(Dispatchers.IO) {
        if (curOffer.requestId.isNotBlank() && curReq.id.isNotBlank() && curOffer.requestId != curReq.id) {
            withContext(Dispatchers.Main) {
                onError("خطأ: العرض المحدد لا ينتمي لهذا الطلب.")
            }
            return@launch
        }

        val nonAcceptableRequestStatuses = listOf("COMPLETED", "CANCELLED", "CLOSED", "EXPIRED")
        if (curReq.status.uppercase() in nonAcceptableRequestStatuses) {
            withContext(Dispatchers.Main) {
                onError("لا يمكن قبول العرض: حالة الطلب الحالية غير قابلة للقبول (${curReq.status}).")
            }
            return@launch
        }

        val nonAcceptableOfferStatuses = listOf("REJECTED", "CANCELLED", "ACCEPTED")
        if (curOffer.status.uppercase() in nonAcceptableOfferStatuses) {
            withContext(Dispatchers.Main) {
                onError("لا يمكن قبول العرض: حالة العرض غير صالحة للقبول (${curOffer.status}).")
            }
            return@launch
        }

        try {
            val now = System.currentTimeMillis()
            firestore.collection("bookings").document(booking.id).set(booking).await()
            firestore.collection("instant_requests").document(curReq.id)
                .collection("offers").document(curOffer.id).update("status", "ACCEPTED").await()
            runCatching {
                firestore.collection("request_offers").document(curOffer.id).update("status", "ACCEPTED").await()
            }
            firestore.collection("instant_requests").document(curReq.id).update(
                mapOf(
                    "status" to "ACCEPTED",
                    "selectedOfferId" to curOffer.id,
                    "acceptedOfferId" to curOffer.id,
                    "acceptedBookingId" to booking.id,
                    "acceptedTechnicianId" to curOffer.technicianId,
                    "acceptedTechnicianName" to curOffer.technicianName,
                    "acceptedTechnicianPhone" to curOffer.technicianPhone,
                    "acceptedPrice" to curOffer.price,
                    "acceptedAt" to now,
                    "updatedAt" to now
                )
            ).await()

            val provNotifId = UUID.randomUUID().toString()
            val provNotif = NotificationEntity(
                id = provNotifId,
                title = "🎉 تم اختيار عرضك وتأكيد الحجز!",
                message = "تم قبول عرضك لطلب ${curReq.requestCode} بمبلغ ${curOffer.price} ر.ي. رقم الحجز: ${booking.bookingNumber}",
                customerPhone = curOffer.technicianPhone,
                targetType = "PROVIDER",
                targetValue = curOffer.technicianPhone,
                notificationType = "OFFER_ACCEPTED",
                timestamp = System.currentTimeMillis()
            )
            firestore.collection("notifications").document(provNotifId).set(provNotif).await()

            if (curReq.userPhone.isNotBlank()) {
                val userNotifId = UUID.randomUUID().toString()
                val userNotif = NotificationEntity(
                    id = userNotifId,
                    title = "🎉 تأكيد حجز العرض للطلب ${curReq.requestCode}",
                    message = "تم تأكيد اختيار عرض الفني ${curOffer.technicianName} بنجاح. رقم الحجز: ${booking.bookingNumber}",
                    customerPhone = curReq.userPhone,
                    targetType = "USER",
                    targetValue = curReq.userPhone,
                    notificationType = "BOOKING_CONFIRMED",
                    timestamp = System.currentTimeMillis()
                )
                firestore.collection("notifications").document(userNotifId).set(userNotif).await()
            }

            val adminNotifId = UUID.randomUUID().toString()
            val adminNotif = NotificationEntity(
                id = adminNotifId,
                title = "📢 تأكيد حجز طلب عاجل",
                message = "تم تأكيد حجز الطلب ${curReq.requestCode} للعميل ${curReq.userName} مع الفني ${curOffer.technicianName} بمبلغ ${curOffer.price} ر.ي",
                targetType = "ADMIN_ONLY",
                targetValue = "ALL",
                timestamp = System.currentTimeMillis()
            )
            firestore.collection("notifications").document(adminNotifId).set(adminNotif).await()

            withContext(Dispatchers.Main) {
                onSuccess()
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onError(e.localizedMessage ?: "فشل تأكيد الحجز")
            }
        }
    }
}
