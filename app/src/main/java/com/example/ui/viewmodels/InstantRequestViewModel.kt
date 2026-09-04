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
    var triggerNotification: ((String) -> Unit)? = null
    var addNotification: ((String, String, String, String) -> Unit)? = null
    var getOrCreateChatChannel: ((String, String, String, String) -> Unit)? = null

    internal val _instantRequests = MutableStateFlow<List<com.example.data.models.InstantRequestEntity>>(emptyList())
    val instantRequests: StateFlow<List<com.example.data.models.InstantRequestEntity>> = _instantRequests.asStateFlow()
    internal val _requestOffers = MutableStateFlow<List<com.example.data.models.RequestOfferEntity>>(emptyList())
    val requestOffers: StateFlow<List<com.example.data.models.RequestOfferEntity>> = _requestOffers.asStateFlow()
    internal val _offers = MutableStateFlow<List<com.example.data.models.Offer>>(emptyList())
    val offers: StateFlow<List<com.example.data.models.Offer>> = _offers.asStateFlow()

    private val repository = com.example.data.repositories.InstantRequestRepository()

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
        repository.createInstantRequest(
            request = req,
            onSuccess = { createdReq -> onResult(true, "تم تقديم الطلب الفوري بنجاح بنظام الكود: $code", createdReq.id) },
            onError = { err -> onResult(false, err, "") }
        )
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

        repository.submitOffer(
            offer = offer,
            onSuccess = {
                triggerNotification?.invoke("💰 تم تقديم عرض السعر ($price ر.ي) بنجاح للطلب $requestCode!")
                val targetReq = _instantRequests.value.find { it.id == requestId }
                if (targetReq != null && targetReq.userPhone.isNotBlank()) {
                    addNotification?.invoke(
                        "💰 عرض جديد من $technicianName على طلبك $requestCode",
                        "قدم الفني $technicianName عرض سعر قدره $price ر.ي بوقت وصول $estimatedArrivalTime. افتح العروض لمقارنة الخيارات والاختيار.",
                        "USER",
                        targetReq.userPhone
                    )
                }
            },
            onError = { err ->
                triggerNotification?.invoke("❌ تعذر تقديم العرض: $err")
            }
        )
    }

    fun acceptRequestOffer(
        req: com.example.data.models.InstantRequestEntity,
        offer: com.example.data.models.RequestOfferEntity
    ) {
        repository.acceptOffer(
            requestId = req.id,
            offerId = offer.id,
            providerId = offer.technicianId,
            providerName = offer.technicianName,
            providerPhone = offer.technicianPhone,
            acceptedPrice = offer.price,
            onSuccess = {
                triggerNotification?.invoke("🎉 تم قبول عرض ${offer.technicianName} بنجاح وتحويل الطلب إلى حجز مؤكد!")
                addNotification?.invoke(
                    "🎉 تم اختيار عرضك للطلب ${req.requestCode}",
                    "تهانينا ${offer.technicianName}! اختار العميل ${req.userName} عرضك بسعر ${offer.price} ر.ي للطلب ${req.requestCode}. يمكنك البدء في التواصل والمباشرة الآن.",
                    "PROVIDER",
                    offer.technicianPhone
                )
                
                val otherOffers = _requestOffers.value.filter { it.requestId == req.id && it.id != offer.id }
                otherOffers.forEach { otherOffer ->
                    addNotification?.invoke(
                        "📢 تم اختيار عرض آخر للطلب ${req.requestCode}",
                        "شكراً لمشاركتك. تم اختيار عرض أسعار آخر من قبل العميل للطلب ${req.requestCode}.",
                        "PROVIDER",
                        otherOffer.technicianPhone
                    )
                }
                
                getOrCreateChatChannel?.invoke(offer.technicianId, offer.technicianName, req.userPhone, req.userName)
            },
            onError = { err ->
                triggerNotification?.invoke("❌ خطأ: $err")
            }
        )
    }

    fun completeInstantRequest(requestId: String) {
        repository.completeInstantRequest(
            requestId = requestId,
            onSuccess = { triggerNotification?.invoke("✅ تم إكمال وتنفيذ الطلب الفوري بنجاح!") },
            onError = { err -> triggerNotification?.invoke("❌ خطأ: $err") }
        )
    }

    fun cancelInstantRequest(requestId: String, passwordInput: String = "", isCustomer: Boolean = true, reqPass: String = "") {
        if (isCustomer && reqPass.isNotEmpty() && passwordInput != reqPass) {
            triggerNotification?.invoke("❌ رمز إلقاء/إلغاء الطلب غير صحيح!")
            return
        }
        repository.cancelInstantRequest(
            requestId = requestId,
            onSuccess = { triggerNotification?.invoke("🚫 تم إلغاء الطلب الفوري بنجاح.") },
            onError = { err -> triggerNotification?.invoke("❌ خطأ: $err") }
        )
    }
}
