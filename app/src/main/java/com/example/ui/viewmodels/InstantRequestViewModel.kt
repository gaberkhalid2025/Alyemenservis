package com.example.ui.viewmodels

import com.example.ui.MainViewModel

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.data.models.*
import com.example.utils.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions

class InstantRequestViewModel : BaseViewModel() {
    lateinit var mainViewModel: MainViewModel

    internal val _instantRequests = MutableStateFlow<List<com.example.data.models.InstantRequestEntity>>(emptyList())
    val instantRequests: StateFlow<List<com.example.data.models.InstantRequestEntity>> = _instantRequests.asStateFlow()
    internal val _requestOffers = MutableStateFlow<List<com.example.data.models.RequestOfferEntity>>(emptyList())
    val requestOffers: StateFlow<List<com.example.data.models.RequestOfferEntity>> = _requestOffers.asStateFlow()
    internal val _offers = MutableStateFlow<List<com.example.data.models.Offer>>(emptyList())
    val offers: StateFlow<List<com.example.data.models.Offer>> = _offers.asStateFlow()

    fun createInstantRequest(
        userId: String,
        userName: String,
        userPhone: String,
        userCity: String,
        userNeighborhood: String,
        categoryId: String,
        categoryName: String,
        serviceTitle: String,
        description: String,
        images: List<String> = emptyList(),
        urgencyTime: String = "فوراً (خلال 30 دقيقة)",
        deliveryMethod: String = "",
        customPin: String = "",
        onResult: (Boolean, String, String) -> Unit = { _, _, _ -> }
    ) {
        val reqId = java.util.UUID.randomUUID().toString()
        val code = "REQ-${System.currentTimeMillis() % 1000000}"
        val req = com.example.data.models.InstantRequestEntity(
            id = reqId,
            requestCode = code,
            userId = userId,
            userName = userName,
            userPhone = userPhone,
            userCity = userCity,
            userNeighborhood = userNeighborhood,
            categoryId = categoryId,
            categoryName = categoryName,
            serviceTitle = serviceTitle,
            description = description,
            images = images,
            urgencyTime = urgencyTime,
            deliveryMethod = deliveryMethod,
            status = "PENDING",
            createdAt = System.currentTimeMillis()
        )
        db.collection("instant_requests").document(reqId).set(req)
            .addOnSuccessListener {
                onResult(true, "تم تقديم الطلب الفوري بنجاح بنظام الكود: $code", reqId)
            }
            .addOnFailureListener { e ->
                onResult(false, e.message ?: "فشل تقديم الطلب", "")
            }
    }

fun submitOfferForRequest(
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
        notes: String = ""
    ) {
        val offerId = java.util.UUID.randomUUID().toString()
        val offer = com.example.data.models.RequestOfferEntity(
            id = offerId,
            requestId = requestId,
            requestCode = requestCode,
            technicianId = technicianId,
            technicianName = technicianName,
            technicianPhone = technicianPhone,
            technicianAvatar = technicianAvatar,
            technicianRating = technicianRating,
            price = price,
            estimatedArrivalTime = estimatedArrivalTime,
            estimatedDuration = estimatedDuration,
            notes = notes,
            status = "PENDING",
            createdAt = System.currentTimeMillis()
        )

        db.collection("request_offers").document(offerId).set(offer)
            .addOnSuccessListener {
                val reqRef = db.collection("instant_requests").document(requestId)
                db.runTransaction { transaction ->
                    val snapshot = transaction.get(reqRef)
                    val currentCount = snapshot.getLong("offersCount")?.toInt() ?: 0
                    transaction.update(reqRef, mapOf(
                        "offersCount" to currentCount + 1,
                        "status" to "REVIEWING_OFFERS"
                    ))
                }
                mainViewModel.triggerNotification("💰 تم تقديم عرض السعر ($price ر.ي) بنجاح للطلب $requestCode!")

                // Notify customer about new offer
                val targetReq = _instantRequests.value.find { it.id == requestId }
                if (targetReq != null && targetReq.userPhone.isNotBlank()) {
                    mainViewModel.addNotification(
                        title = "💰 عرض جديد من $technicianName على طلبك $requestCode",
                        message = "قدم الفني $technicianName عرض سعر قدره $price ر.ي بوقت وصول $estimatedArrivalTime. افتح العروض لمقارنة الخيارات والاختيار.",
                        targetType = "USER",
                        targetValue = targetReq.userPhone
                    )
                }
            }
            .addOnFailureListener {
                mainViewModel.triggerNotification("❌ تعذر تقديم العرض: ${it.localizedMessage}")
            }
    }

fun acceptRequestOffer(
        req: com.example.data.models.InstantRequestEntity,
        offer: com.example.data.models.RequestOfferEntity
    ) {
        db.collection("request_offers").document(offer.id).update("status", "ACCEPTED")
        
        db.collection("instant_requests").document(req.id).update(mapOf(
            "status" to "ACCEPTED",
            "acceptedOfferId" to offer.id,
            "acceptedTechnicianId" to offer.technicianId,
            "acceptedTechnicianName" to offer.technicianName,
            "acceptedTechnicianPhone" to offer.technicianPhone,
            "acceptedPrice" to offer.price
        ))

        val bookingId = java.util.UUID.randomUUID().toString()
        val booking = com.example.data.BookingEntity(
            id = bookingId,
            bookingNumber = req.requestCode,
            bookingPassword = req.cancellationPassword,
            pinCode = req.secretPin,
            clientId = req.userId,
            clientName = req.userName,
            clientPhone = req.userPhone,
            clientAddress = "${req.userCity} - ${req.userNeighborhood}",
            customerName = req.userName,
            customerPhone = req.userPhone,
            customerArea = req.userCity,
            providerId = offer.technicianId,
            providerName = offer.technicianName,
            providerPhone = offer.technicianPhone,
            category = req.categoryName,
            serviceType = req.serviceTitle,
            serviceDetails = req.description,
            date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ENGLISH).format(java.util.Date()),
            time = java.text.SimpleDateFormat("HH:mm", java.util.Locale.ENGLISH).format(java.util.Date()),
            dateString = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ENGLISH).format(java.util.Date()),
            timeString = java.text.SimpleDateFormat("HH:mm", java.util.Locale.ENGLISH).format(java.util.Date()),
            status = "APPROVED",
            totalAmount = offer.price,
            createdAt = System.currentTimeMillis()
        )

        db.collection("bookings").document(bookingId).set(booking)
        mainViewModel.triggerNotification("🎉 تم قبول عرض ${offer.technicianName} بنجاح وتحويل الطلب إلى حجز مؤكد!")

        // Notify winning provider
        mainViewModel.addNotification(
            title = "🎉 تم اختيار عرضك للطلب ${req.requestCode}",
            message = "تهانينا $offer.technicianName! اختار العميل $req.userName عرضك بسعر $offer.price ر.ي للطلب $req.requestCode. يمكنك البدء في التواصل والمباشرة الآن.",
            targetType = "PROVIDER",
            targetValue = offer.technicianPhone
        )

        // Notify other bidders
        val otherOffers = _requestOffers.value.filter { it.requestId == req.id && it.id != offer.id }
        otherOffers.forEach { otherOffer ->
            db.collection("request_offers").document(otherOffer.id).update("status", "REJECTED")
            mainViewModel.addNotification(
                title = "📢 تم اختيار عرض آخر للطلب ${req.requestCode}",
                message = "شكراً لمشاركتك. تم اختيار عرض أسعار آخر من قبل العميل للطلب ${req.requestCode}.",
                targetType = "PROVIDER",
                targetValue = otherOffer.technicianPhone
            )
        }

        // Create active chat channel between customer & winning provider
        mainViewModel.getOrCreateChatChannel(offer.technicianId, offer.technicianName, req.userPhone, req.userName)
    }

fun completeInstantRequest(requestId: String) {
        db.collection("instant_requests").document(requestId).update("status", "COMPLETED")
            .addOnSuccessListener {
                mainViewModel.triggerNotification("✅ تم إكمال وتنفيذ الطلب الفوري بنجاح!")
            }
    }

fun cancelInstantRequest(requestId: String, passwordInput: String = "", isCustomer: Boolean = true, reqPass: String = "") {
        if (isCustomer && reqPass.isNotEmpty() && passwordInput != reqPass) {
            mainViewModel.triggerNotification("❌ رمز إلقاء/إلغاء الطلب غير صحيح!")
            return
        }
        db.collection("instant_requests").document(requestId).update("status", "CANCELLED")
            .addOnSuccessListener {
                mainViewModel.triggerNotification("🚫 تم إلغاء الطلب الفوري بنجاح.")
            }
    }

}