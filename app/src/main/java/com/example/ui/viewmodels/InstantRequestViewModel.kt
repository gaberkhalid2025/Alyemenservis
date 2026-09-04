package com.example.ui.viewmodels

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.example.data.NotificationEntity
import com.example.data.models.ChannelType
import com.example.data.models.InstantRequestEntity
import com.example.data.models.MediaType
import com.example.data.models.Offer
import com.example.data.models.RequestOfferEntity
import com.example.data.repositories.ChatRepository
import com.example.data.repositories.InstantRequestRepository
import com.example.security.BookingSecurityHelper
import com.example.utils.AppResult
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
  * ⚡ InstantUiState
  */
sealed class InstantUiState {
    object Idle : InstantUiState()
    object Loading : InstantUiState()
    data class Success(val message: String? = null) : InstantUiState()
    data class Error(val message: String) : InstantUiState()
    object Empty : InstantUiState()
}

sealed class InstantEvent {
    data class ShowToast(val message: String) : InstantEvent()
    data class ShowSnackbar(val message: String) : InstantEvent()
}

/**
 * ⚡ InstantRequestViewModel
 * The unified official ViewModel for 30-minute instant / urgent requests.
 * Uses InstantRequestRepository and Firestore collection "instant_requests".
 */
class InstantRequestViewModel : BaseViewModel() {
    var triggerNotification: ((String) -> Unit)? = null
    var addNotification: ((String, String, String, String) -> Unit)? = null
    var getOrCreateChatChannel: ((String, String, String, String) -> Unit)? = null

    private val repository = InstantRequestRepository()
    private val chatRepo by lazy { ChatRepository() }
    private val firestore by lazy { FirebaseFirestore.getInstance() }

    private val _uiState = MutableStateFlow<InstantUiState>(InstantUiState.Idle)
    val uiState: StateFlow<InstantUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<InstantEvent>()
    val eventFlow: SharedFlow<InstantEvent> = _eventFlow.asSharedFlow()

    internal val _instantRequests = MutableStateFlow<List<InstantRequestEntity>>(emptyList())
    val instantRequests: StateFlow<List<InstantRequestEntity>> = _instantRequests.asStateFlow()

    private val _selectedRequest = MutableStateFlow<InstantRequestEntity?>(null)
    val selectedRequest: StateFlow<InstantRequestEntity?> = _selectedRequest.asStateFlow()

    internal val _requestOffers = MutableStateFlow<List<RequestOfferEntity>>(emptyList())
    val requestOffers: StateFlow<List<RequestOfferEntity>> = _requestOffers.asStateFlow()

    internal val _offers = MutableStateFlow<List<Offer>>(emptyList())
    val offers: StateFlow<List<Offer>> = _offers.asStateFlow()

    private var requestsListener: ListenerRegistration? = null
    private var detailsListener: ListenerRegistration? = null
    private var offersListener: ListenerRegistration? = null

    fun observeInstantRequests(currentUserId: String, isProvider: Boolean) {
        requestsListener?.remove()

        var query: Query = firestore.collection("instant_requests")
        if (isProvider) {
            query = query.whereIn("status", listOf("WAITING_FOR_OFFERS", "REVIEWING_OFFERS", "PENDING"))
        } else if (currentUserId.isNotBlank()) {
            query = query.whereEqualTo("userId", currentUserId)
        }

        requestsListener = query.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) {
                _instantRequests.value = emptyList()
                return@addSnapshotListener
            }

            val list = snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(InstantRequestEntity::class.java)?.copy(id = doc.id)
                } catch (e: Exception) {
                    null
                }
            }
            _instantRequests.value = list
        }
    }

    fun observeRequestDetails(requestId: String) {
        if (requestId.isBlank()) return

        detailsListener?.remove()
        detailsListener = firestore.collection("instant_requests").document(requestId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) {
                    _selectedRequest.value = null
                    return@addSnapshotListener
                }
                _selectedRequest.value = snapshot.toObject(InstantRequestEntity::class.java)?.copy(id = snapshot.id)
            }

        offersListener?.remove()
        offersListener = firestore.collection("instant_requests").document(requestId)
            .collection("offers")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    _requestOffers.value = emptyList()
                    return@addSnapshotListener
                }
                val list = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(RequestOfferEntity::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        null
                    }
                }
                _requestOffers.value = list
            }
    }

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
        _uiState.value = InstantUiState.Loading
        val reqId = UUID.randomUUID().toString()
        val code = "URG-${(1000..9999).random()}"
        val req = InstantRequestEntity(
            id = reqId,
            requestCode = code,
            secretPin = customPin,
            cancellationPassword = customPin,
            userId = if (userId.isNotBlank()) userId else userPhone,
            userName = userName.ifBlank { "عميل" },
            userPhone = userPhone,
            userCity = userCity,
            userNeighborhood = userNeighborhood,
            categoryId = categoryId,
            categoryName = categoryName,
            serviceTitle = if (serviceTitle.startsWith("🚨")) serviceTitle else "🚨 عاجل: $serviceTitle",
            description = description,
            images = images,
            urgencyTime = urgencyTime,
            deliveryMethod = deliveryMethod,
            status = "WAITING_FOR_OFFERS",
            createdAt = System.currentTimeMillis()
        )
        repository.createInstantRequest(
            request = req,
            onSuccess = { createdReq ->
                _uiState.value = InstantUiState.Success("تم تقديم الطلب الفوري بنجاح بنظام الكود: ${createdReq.requestCode}")
                onResult(true, "تم تقديم الطلب الفوري بنجاح بنظام الكود: ${createdReq.requestCode}", createdReq.id)
            },
            onError = { err ->
                _uiState.value = InstantUiState.Error(err)
                onResult(false, err, "")
            }
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
        _uiState.value = InstantUiState.Loading
        val offerId = UUID.randomUUID().toString()
        val offer = RequestOfferEntity(
            id = offerId,
            requestId = requestId,
            requestCode = requestCode,
            technicianId = technicianId,
            technicianName = technicianName.ifBlank { "فني طوارئ معتمد" },
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

        repository.submitOffer(
            offer = offer,
            onSuccess = {
                _uiState.value = InstantUiState.Success("تم تقديم عرض السعر بنجاح")
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
                _uiState.value = InstantUiState.Error(err)
                triggerNotification?.invoke("❌ تعذر تقديم العرض: $err")
            }
        )
    }

    fun acceptOffer(
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
                                        senderId = "system",
                                        senderName = "النظام",
                                        messageText = "مرحباً! تم قبول عرضك للطلب العاجل رقم ${requestId.take(6)}. تم فتح هذه المحادثة لتنسيق العمل.",
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

    fun acceptRequestOffer(
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
            }
        }
    }

    fun completeInstantRequest(requestId: String) {
        repository.completeInstantRequest(
            requestId = requestId,
            onSuccess = { triggerNotification?.invoke("✅ تم إكمال وتنفيذ الطلب الفوري بنجاح!") },
            onError = { err -> triggerNotification?.invoke("❌ خطأ: $err") }
        )
    }

    fun cancelInstantRequest(
        requestId: String,
        userPin: String = "",
        context: Context? = null,
        onResult: (Boolean, String?) -> Unit = { _, _ -> }
    ) {
        if (context != null && BookingSecurityHelper.isBookingLocked(context, requestId)) {
            val remainingSecs = BookingSecurityHelper.getRemainingLockoutSeconds(context, requestId)
            val msg = "الحساب مقفل مؤقتاً لأسباب أمنية بسبب إدخال PIN خاطئ 3 مرات. انتظر $remainingSecs ثانية."
            _uiState.value = InstantUiState.Error(msg)
            onResult(false, msg)
            return
        }

        _uiState.value = InstantUiState.Loading
        if (context != null) {
            BookingSecurityHelper.resetAttempts(context, requestId)
        }

        repository.cancelInstantRequest(
            requestId = requestId,
            userPin = userPin,
            onSuccess = {
                _uiState.value = InstantUiState.Success("تم إلغاء الطلب الفوري بنجاح")
                triggerNotification?.invoke("🚫 تم إلغاء الطلب الفوري بنجاح.")
                onResult(true, "تم إلغاء الطلب بنجاح")
            },
            onError = { err ->
                _uiState.value = InstantUiState.Error(err)
                triggerNotification?.invoke("❌ خطأ: $err")
                onResult(false, err)
            }
        )
    }

    fun clearUiState() {
        _uiState.value = InstantUiState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        requestsListener?.remove()
        detailsListener?.remove()
        offersListener?.remove()
    }
}
