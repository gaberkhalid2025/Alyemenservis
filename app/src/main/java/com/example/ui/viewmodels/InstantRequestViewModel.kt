package com.example.ui.viewmodels

import android.content.Context
import com.example.data.BookingEntity
import com.example.data.models.ChannelType
import com.example.data.models.ChatChannel
import com.example.data.models.CreateInstantRequestParams
import com.example.data.models.InstantRequestEntity
import com.example.data.models.Offer
import com.example.data.models.RequestOfferEntity
import com.example.data.repositories.ChatRepository
import com.example.data.repositories.InstantRequestRepository
import com.example.ui.helpers.AppState
import com.example.utils.AppResult
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.SecureRandom
import java.util.UUID
import javax.inject.Inject

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
class InstantRequestViewModel @Inject constructor(
    internal val repository: InstantRequestRepository,
    internal val chatRepo: ChatRepository,
    val appState: AppState
) : BaseViewModel() {

    var triggerNotification: ((String) -> Unit)? = null
    var addNotification: ((String, String, String, String) -> Unit)? = null
    var getOrCreateChatChannel: ((String, String, String, String) -> Unit)? = null

    internal val firestore by lazy { FirebaseFirestore.getInstance() }

    internal val _uiState = MutableStateFlow<InstantUiState>(InstantUiState.Idle)
    val uiState: StateFlow<InstantUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<InstantEvent>()
    val eventFlow: SharedFlow<InstantEvent> = _eventFlow.asSharedFlow()

    internal val _instantRequests get() = appState._instantRequests
    val instantRequests: StateFlow<List<InstantRequestEntity>> = _instantRequests.asStateFlow()

    internal val _selectedRequest = MutableStateFlow<InstantRequestEntity?>(null)
    val selectedRequest: StateFlow<InstantRequestEntity?> = _selectedRequest.asStateFlow()

    internal val _selectedOffer = MutableStateFlow<RequestOfferEntity?>(null)
    val selectedOffer: StateFlow<RequestOfferEntity?> = _selectedOffer.asStateFlow()

    internal val _requestOffers get() = appState._requestOffers
    val requestOffers: StateFlow<List<RequestOfferEntity>> = _requestOffers.asStateFlow()

    internal val _offers get() = appState._offers
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
        offersListener = firestore.collection("instant_requests")
            .document(requestId)
            .collection("offers")
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    return@addSnapshotListener
                }
                val list = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(RequestOfferEntity::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        null
                    }
                }
                val others = _requestOffers.value.filter { it.requestId != requestId && it.id !in list.map { o -> o.id } }
                _requestOffers.value = (others + list).distinctBy { it.id }
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
        latitude: Double = 0.0,
        longitude: Double = 0.0,
        sectionType: String = "",
        onResult: (Boolean, String, String) -> Unit = { _, _, _ -> }
    ) {
        _uiState.value = InstantUiState.Loading
        val reqId = UUID.randomUUID().toString()
        val secureRandom = SecureRandom()
        val code = "URG-${100000 + secureRandom.nextInt(900000)}"
        val safePin = customPin.trim().ifBlank { (1000 + secureRandom.nextInt(9000)).toString() }
        val resolvedSection = normalizeInstantSectionCode(sectionType.ifBlank { categoryId })
        val now = System.currentTimeMillis()
        val req = InstantRequestEntity(
            id = reqId,
            requestCode = code,
            rawPin = safePin,
            cancellationPassword = safePin,
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
            createdAt = now,
            latitude = latitude,
            longitude = longitude,
            sectionType = resolvedSection,
            updatedAt = now
        )
        repository.createInstantRequest(
            request = req,
            onSuccess = { createdReq ->
                val resolvedPin = createdReq.rawPin.ifBlank { safePin }
                if (resolvedPin.isBlank()) {
                    _uiState.value = InstantUiState.Error("فشل توليد رمز الحماية للطلب")
                    onResult(false, "", "")
                    return@createInstantRequest
                }
                _instantRequests.value = (_instantRequests.value.filter { it.id != createdReq.id } + createdReq)
                _uiState.value = InstantUiState.Success("تم تقديم الطلب الفوري بنجاح بنظام الكود: ${createdReq.requestCode}")
                val configuredRadius = appState._maxKmRadius.value.coerceIn(5, 200)
                sendUrgentRequestNotificationToNearbyProvidersHelper(createdReq, configuredRadius)
                onResult(true, createdReq.requestCode, resolvedPin)
            },
            onError = { err ->
                _instantRequests.value = _instantRequests.value.filter { it.id != reqId }
                _uiState.value = InstantUiState.Error("فشل حفظ الطلب الفوري: $err")
                triggerNotification?.invoke("❌ فشل إرسال الطلب الفوري، يرجى التحقق من الاتصال بالإنترنت: $err")
                onResult(false, "", "")
            }
        )
    }

    fun createInstantRequest(
        params: CreateInstantRequestParams,
        onResult: (Boolean, String, String) -> Unit = { _, _, _ -> }
    ) {
        createInstantRequest(
            userId = params.userId,
            userName = params.userName,
            userPhone = params.userPhone,
            userCity = params.userCity,
            userNeighborhood = params.userNeighborhood,
            categoryId = params.categoryId,
            categoryName = params.categoryName,
            serviceTitle = params.serviceTitle,
            description = params.description,
            images = params.images,
            urgencyTime = params.urgencyTime,
            deliveryMethod = params.deliveryMethod,
            customPin = params.customPin,
            latitude = params.latitude,
            longitude = params.longitude,
            sectionType = params.sectionType,
            onResult = onResult
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
        notes: String = "",
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) = submitOfferForRequestHelper(
        requestId, requestCode, technicianId, technicianName, technicianPhone,
        technicianAvatar, technicianRating, price, estimatedArrivalTime,
        estimatedDuration, notes, onResult
    )

    fun acceptOffer(
        requestId: String,
        offerId: String,
        providerPhone: String,
        onResult: (Boolean, String?) -> Unit
    ) = acceptOfferHelper(requestId, offerId, providerPhone, onResult)

    fun acceptRequestOffer(req: InstantRequestEntity, offer: RequestOfferEntity) =
        acceptRequestOfferHelper(req, offer)

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
        cancelReason: String = "",
        onResult: (Boolean, String?) -> Unit = { _, _ -> }
    ) = cancelInstantRequestHelper(requestId, userPin, context, cancelReason, onResult)

    fun loadOfferAndRequest(
        offerId: String,
        onResult: (RequestOfferEntity?, InstantRequestEntity?) -> Unit = { _, _ -> }
    ) = loadOfferAndRequestHelper(offerId, onResult)

    fun confirmOfferSelection(
        booking: BookingEntity,
        curOffer: RequestOfferEntity,
        curReq: InstantRequestEntity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) = confirmOfferSelectionHelper(booking, curOffer, curReq, onSuccess, onError)

    fun clearUiState() {
        _uiState.value = InstantUiState.Idle
    }

    suspend fun getOrCreateChatChannel(
        currentUserId: String,
        currentUserName: String,
        currentUserPhoto: String = "",
        otherUserId: String,
        otherUserName: String,
        otherUserPhoto: String = "",
        type: ChannelType = ChannelType.PRIVATE,
        relatedEntityId: String? = null,
        relatedEntityType: String? = null
    ): AppResult<ChatChannel> {
        return chatRepo.getOrCreateChannel(
            currentUserId = currentUserId,
            currentUserName = currentUserName,
            currentUserPhoto = currentUserPhoto,
            otherUserId = otherUserId,
            otherUserName = otherUserName,
            otherUserPhoto = otherUserPhoto,
            type = type,
            relatedEntityId = relatedEntityId,
            relatedEntityType = relatedEntityType
        )
    }

    override fun onCleared() {
        super.onCleared()
        requestsListener?.remove()
        detailsListener?.remove()
        offersListener?.remove()
    }
}
