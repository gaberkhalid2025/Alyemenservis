package com.example.ui.viewmodels
import com.example.ui.helpers.AppState

import android.content.Context
import com.example.ui.*
import androidx.lifecycle.viewModelScope
import com.example.data.ProviderEntity
import com.example.data.BookingEntity
import com.example.data.NotificationEntity
import com.example.data.PropertyEntity
import com.example.data.StoreEntity
import com.example.data.models.ChannelType
import com.example.data.models.ChatChannel
import com.example.data.models.InstantRequestEntity
import com.example.data.models.MediaType
import com.example.data.models.Offer
import com.example.data.models.RequestOfferEntity
import com.example.data.repositories.ChatRepository
import com.example.data.repositories.InstantRequestRepository
import com.example.security.BookingSecurityHelper
import com.example.utils.AppConstants
import com.example.utils.AppResult
import com.example.utils.HaversinePrecisionNavigator
import com.example.utils.LandmarkAddress
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val repository: InstantRequestRepository,
    private val chatRepo: ChatRepository,
    val appState: AppState
) : BaseViewModel() {

    var triggerNotification: ((String) -> Unit)? = null
    var addNotification: ((String, String, String, String) -> Unit)? = null
    var getOrCreateChatChannel: ((String, String, String, String) -> Unit)? = null

    private val firestore by lazy { FirebaseFirestore.getInstance() }

    private val _uiState = MutableStateFlow<InstantUiState>(InstantUiState.Idle)
    val uiState: StateFlow<InstantUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<InstantEvent>()
    val eventFlow: SharedFlow<InstantEvent> = _eventFlow.asSharedFlow()

    internal val _instantRequests get() = appState._instantRequests
    val instantRequests: StateFlow<List<InstantRequestEntity>> = _instantRequests.asStateFlow()

    private val _selectedRequest = MutableStateFlow<InstantRequestEntity?>(null)
    val selectedRequest: StateFlow<InstantRequestEntity?> = _selectedRequest.asStateFlow()

    private val _selectedOffer = MutableStateFlow<RequestOfferEntity?>(null)
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

    private fun normalizeSectionCode(raw: String): String {
        val v = raw.trim().uppercase()
        return when {
            v == "SERVICES" || v.contains("خدم") || v.contains("فني") -> "SERVICES"
            v == "STORES" || v.contains("متجر") || v.contains("متاجر") || v.contains("محل") || v.contains("تجار") || v.contains("سلع") -> "STORES"
            v == "RESTAURANTS" || v.contains("مطعم") || v.contains("مطاعم") || v.contains("كافيه") -> "RESTAURANTS"
            v == "MEDICAL" || v.contains("طب") || v.contains("صيدل") || v.contains("عياد") -> "MEDICAL"
            v == "PROPERTIES" || v.contains("عقار") || v.contains("شقق") -> "PROPERTIES"
            v == "GENERAL" || v.contains("عام") -> "GENERAL"
            else -> v.ifBlank { "GENERAL" }
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
        val secureRandom = java.security.SecureRandom()
        val code = "URG-${100000 + secureRandom.nextInt(900000)}"
        val safePin = customPin.trim().ifBlank { (1000 + secureRandom.nextInt(9000)).toString() }
        val resolvedSection = normalizeSectionCode(sectionType.ifBlank { categoryId })
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
                sendUrgentRequestNotificationToNearbyProviders(createdReq, configuredRadius)
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
        params: com.example.data.models.CreateInstantRequestParams,
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

    private fun isWithinEffectiveRadius(
        requestLat: Double,
        requestLng: Double,
        targetLat: Double,
        targetLng: Double,
        radiusKm: Int,
        cityMatched: Boolean
    ): Boolean {
        if (!LandmarkAddress.isValid(requestLat, requestLng)) return true
        if (!LandmarkAddress.isValid(targetLat, targetLng)) return cityMatched
        // إذا كانت الإحداثيات الافتراضية لصنعاء على كيان في مدينة أخرى متطابقة بالمدينة لا نسقطه
        val isDefaultSanaa = kotlin.math.abs(targetLat - 15.3694) < 0.0005 && kotlin.math.abs(targetLng - 44.1910) < 0.0005
        val isRequestNearSanaa = HaversinePrecisionNavigator.calculateDistance(requestLat, requestLng, 15.3694, 44.1910) <= radiusKm * 1000.0
        if (isDefaultSanaa && !isRequestNearSanaa && cityMatched) return true
        return HaversinePrecisionNavigator.calculateDistance(requestLat, requestLng, targetLat, targetLng) <= radiusKm * 1000.0
    }

    private fun sendUrgentRequestNotificationToNearbyProviders(
        request: InstantRequestEntity,
        radiusKm: Int
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val section = normalizeSectionCode(request.sectionType.ifBlank { request.categoryId })
                val cityQuery = request.userCity.trim()
                val matchedCity = appState._cities.value.find {
                    it.nameAr.equals(cityQuery, ignoreCase = true) ||
                        it.nameEn.equals(cityQuery, ignoreCase = true) ||
                        it.id.equals(cityQuery, ignoreCase = true)
                }
                val cityKeys = listOfNotNull(
                    cityQuery.takeIf { it.isNotBlank() },
                    matchedCity?.id?.trim()?.takeIf { it.isNotBlank() },
                    matchedCity?.nameAr?.trim()?.takeIf { it.isNotBlank() },
                    matchedCity?.nameEn?.trim()?.takeIf { it.isNotBlank() }
                ).distinct().take(10)

                val targets = linkedMapOf<String, String>() // targetValue -> customerPhone

                // 1) الفنيون والخدمات (SERVICES أو GENERAL)
                if (section == "SERVICES" || section == "GENERAL") {
                    val provQuery: Query = if (cityKeys.isNotEmpty()) {
                        firestore.collection(AppConstants.COL_PROVIDERS).whereIn("cityId", cityKeys).limit(120)
                    } else {
                        firestore.collection(AppConstants.COL_PROVIDERS).limit(120)
                    }
                    val provSnap = runCatching { provQuery.get().await() }.getOrNull()
                    provSnap?.documents?.mapNotNull { doc ->
                        runCatching { doc.toObject(ProviderEntity::class.java)?.copy(id = doc.id) }.getOrNull()
                    }?.filter { provider ->
                        provider.isAvailable &&
                            !provider.isDeleted &&
                            !provider.isBlocked &&
                            !provider.isNotificationsDisabled &&
                            provider.subscriptionStatus.uppercase() in listOf("APPROVED", "ACCEPTED", "ACTIVE") &&
                            isWithinEffectiveRadius(
                                request.latitude, request.longitude,
                                provider.latitude, provider.longitude,
                                radiusKm,
                                cityKeys.isEmpty() || cityKeys.any { it.equals(provider.cityId, ignoreCase = true) }
                            )
                    }?.forEach { provider ->
                        val key = provider.phone.ifBlank { provider.id }
                        if (key.isNotBlank()) targets.putIfAbsent(key, provider.phone)
                    }
                }

                // 2) المتاجر والسلع، المطاعم، والمراكز الطبية (STORES / RESTAURANTS / MEDICAL / GENERAL)
                if (section in listOf("STORES", "RESTAURANTS", "MEDICAL", "GENERAL")) {
                    val storeQuery: Query = if (cityKeys.isNotEmpty()) {
                        firestore.collection(AppConstants.COL_STORES).whereIn("cityId", cityKeys).limit(120)
                    } else {
                        firestore.collection(AppConstants.COL_STORES).limit(120)
                    }
                    val storeSnap = runCatching { storeQuery.get().await() }.getOrNull()
                    storeSnap?.documents?.mapNotNull { doc ->
                        runCatching { doc.toObject(StoreEntity::class.java)?.copy(id = doc.id) }.getOrNull()
                    }?.filter { store ->
                        val storeSection = normalizeSectionCode(store.sectionId.ifBlank { store.providerType.ifBlank { "STORES" } })
                        val sectionMatches = section == "GENERAL" ||
                            storeSection == section ||
                            (section == "STORES" && store.sectionId.isBlank())
                        store.isActive &&
                            store.isApproved &&
                            !store.isDeleted &&
                            !store.isBlocked &&
                            !store.isNotificationsDisabled &&
                            sectionMatches &&
                            isWithinEffectiveRadius(
                                request.latitude, request.longitude,
                                store.latitude, store.longitude,
                                radiusKm,
                                cityKeys.isEmpty() || cityKeys.any { it.equals(store.cityId, ignoreCase = true) }
                            )
                    }?.forEach { store ->
                        val key = store.phone.ifBlank { store.ownerId.ifBlank { store.id } }
                        if (key.isNotBlank()) targets.putIfAbsent(key, store.phone)
                    }
                }

                // 3) العقارات والشقق والأراضي (PROPERTIES / GENERAL)
                if (section == "PROPERTIES" || section == "GENERAL") {
                    val propQuery: Query = if (cityKeys.isNotEmpty()) {
                        firestore.collection(AppConstants.COL_PROPERTIES).whereIn("cityId", cityKeys).limit(120)
                    } else {
                        firestore.collection(AppConstants.COL_PROPERTIES).limit(120)
                    }
                    val propSnap = runCatching { propQuery.get().await() }.getOrNull()
                    propSnap?.documents?.mapNotNull { doc ->
                        runCatching { doc.toObject(PropertyEntity::class.java)?.copy(id = doc.id) }.getOrNull()
                    }?.filter { prop ->
                        prop.isActive &&
                            prop.isApproved &&
                            !prop.isDeleted &&
                            !prop.isBlocked &&
                            !prop.isNotificationsDisabled &&
                            isWithinEffectiveRadius(
                                request.latitude, request.longitude,
                                prop.latitude, prop.longitude,
                                radiusKm,
                                cityKeys.isEmpty() || cityKeys.any { it.equals(prop.cityId, ignoreCase = true) }
                            )
                    }?.forEach { prop ->
                        val key = prop.phone.ifBlank { prop.ownerId.ifBlank { prop.id } }
                        if (key.isNotBlank()) targets.putIfAbsent(key, prop.phone)
                    }
                }

                // 4) إرسال إشعارات مجمعة مع احترام حد الـ Batch في Firestore
                val now = System.currentTimeMillis()
                val entries = targets.entries.toList()
                entries.chunked(400).forEach { chunk ->
                    val batch = firestore.batch()
                    chunk.forEach { (targetPhoneOrId, phone) ->
                        val safeKey = targetPhoneOrId.replace(Regex("[^A-Za-z0-9_-]"), "_")
                        val notifId = "URG_PROV_${request.id}_$safeKey"
                        val notif = NotificationEntity(
                            id = notifId,
                            title = "🚨 طلب عاجل في ${request.userCity}",
                            message = "طلب (${request.requestCode}) - ${request.categoryName.ifBlank { section }}: ${request.serviceTitle} في ${request.userNeighborhood.ifBlank { request.userCity }}. قدّم عرضك الآن!",
                            customerPhone = phone,
                            targetType = "PROVIDER",
                            targetValue = targetPhoneOrId,
                            notificationType = "URGENT_REQUEST",
                            relatedRequestId = request.id,
                            fcmSent = false,
                            timestamp = now,
                            createdAt = now,
                            dedupKey = notifId
                        )
                        batch.set(firestore.collection("notifications").document(notifId), notif)
                    }
                    runCatching { batch.commit().await() }
                }

                // 5) تسجيل الإشعار العام للقسم في Firestore
                addNotification?.invoke(
                    "🚨 طلب عاجل جديد",
                    "تم إرسال طلب عاجل في ${request.userCity} - القسم: ${request.categoryName.ifBlank { section }}",
                    "PROVIDER",
                    section
                )
            } catch (e: Exception) {
                android.util.Log.e("InstantRequestVM", "Failed to notify nearby providers", e)
            }
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

    fun loadOfferAndRequest(
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

    fun confirmOfferSelection(
        booking: BookingEntity,
        curOffer: RequestOfferEntity,
        curReq: InstantRequestEntity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            // 🛡️ التحقق القبلي الصارم لمنع إنشاء حجوزات يتيمة أو قبول عروض غير متطابقة
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

                // Triple notification: PROVIDER, USER, ADMIN_ONLY
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
