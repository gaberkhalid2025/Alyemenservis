package com.example.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.NotificationEntity
import com.example.data.InstantRequestEntity
import com.example.data.RequestOfferEntity
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * 🔒 UrgentUiState
 * يمثل الحالات المختلفة لواجهة المستخدم الخاصة بالطلبات العاجلة.
 */
sealed class UrgentUiState {
    object Idle : UrgentUiState()
    object Loading : UrgentUiState()
    data class Success(val message: String? = null) : UrgentUiState()
    data class Error(val message: String) : UrgentUiState()
    object Empty : UrgentUiState()
}

/**
 * 🚨 UrgentViewModel
 * إدارة المنطق البرمجي والبيانات للطلبات العاجلة خلال 30 دقيقة.
 * يدعم التزامن المباشر مع Firebase Firestore واستمرار البيانات للعمل بدون إنترنت (Offline persistence).
 */
class UrgentViewModel : ViewModel() {

    private val firestore by lazy { FirebaseFirestore.getInstance() }

    private val _uiState = MutableStateFlow<UrgentUiState>(UrgentUiState.Idle)
    val uiState: StateFlow<UrgentUiState> = _uiState.asStateFlow()

    private val _urgentRequests = MutableStateFlow<List<InstantRequestEntity>>(emptyList())
    val urgentRequests: StateFlow<List<InstantRequestEntity>> = _urgentRequests.asStateFlow()

    private val _selectedRequest = MutableStateFlow<InstantRequestEntity?>(null)
    val selectedRequest: StateFlow<InstantRequestEntity?> = _selectedRequest.asStateFlow()

    private val _offersForRequest = MutableStateFlow<List<RequestOfferEntity>>(emptyList())
    val offersForRequest: StateFlow<List<RequestOfferEntity>> = _offersForRequest.asStateFlow()

    private var requestsListener: ListenerRegistration? = null
    private var detailsListener: ListenerRegistration? = null
    private var offersListener: ListenerRegistration? = null

    /**
     * تحميل واستماع للطلبات العاجلة النشطة.
     */
    fun observeUrgentRequests(currentUserId: String, isProvider: Boolean) {
        _uiState.value = UrgentUiState.Loading
        requestsListener?.remove()

        var query: Query = firestore.collection("instant_requests")
        if (!isProvider && currentUserId.isNotBlank() && currentUserId != "guest") {
            query = query.whereEqualTo("userId", currentUserId)
        }

        requestsListener = query.orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _uiState.value = UrgentUiState.Error("خطأ في الاتصال: ${error.localizedMessage}")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { it.toObject(InstantRequestEntity::class.java) }
                    val filtered = list.filter {
                        it.urgencyTime.contains("30") || it.requestCode.startsWith("URG") || it.serviceTitle.contains("عاجل")
                    }
                    _urgentRequests.value = filtered
                    _uiState.value = if (filtered.isEmpty()) UrgentUiState.Empty else UrgentUiState.Idle
                } else {
                    _urgentRequests.value = emptyList()
                    _uiState.value = UrgentUiState.Empty
                }
            }
    }

    /**
     * تحميل تفاصيل طلب عاجل محدد والعروض المرتبطة به.
     */
    fun observeRequestDetails(requestId: String) {
        if (requestId.isBlank()) return
        _uiState.value = UrgentUiState.Loading
        detailsListener?.remove()
        offersListener?.remove()

        detailsListener = firestore.collection("instant_requests").document(requestId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _uiState.value = UrgentUiState.Error("فشل تحميل الطلب: ${error.localizedMessage}")
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    _selectedRequest.value = snapshot.toObject(InstantRequestEntity::class.java)
                } else {
                    _selectedRequest.value = null
                }
                _uiState.value = UrgentUiState.Idle
            }

        offersListener = firestore.collection("instant_offers")
            .whereEqualTo("requestId", requestId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { it.toObject(RequestOfferEntity::class.java) }
                    _offersForRequest.value = list
                } else {
                    _offersForRequest.value = emptyList()
                }
            }
    }

    /**
     * إنشاء طلب عاجل جديد لمدة 30 دقيقة.
     */
    fun createUrgentRequest(
        customerName: String,
        customerPhone: String,
        selectedCity: String,
        selectedArea: String,
        selectedDepartment: String,
        selectedCategory: String,
        serviceTitle: String,
        serviceDetails: String,
        pinCode: String,
        currentUserId: String,
        onSuccess: (requestCode: String) -> Unit,
        onError: (message: String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = UrgentUiState.Loading
            try {
                val uniqueCode = "URG-${(100000..999999).random()}"
                val reqId = UUID.randomUUID().toString()
                val now = System.currentTimeMillis()
                val urgentReq = InstantRequestEntity(
                    id = reqId,
                    requestCode = uniqueCode,
                    secretPin = pinCode,
                    cancellationPassword = pinCode,
                    userId = if (currentUserId.isNotBlank()) currentUserId else customerPhone,
                    userName = customerName.ifBlank { "عميل" },
                    userPhone = customerPhone,
                    userCity = selectedCity,
                    userNeighborhood = selectedArea,
                    categoryId = selectedDepartment,
                    categoryName = selectedCategory,
                    serviceTitle = "🚨 عاجل: $serviceTitle",
                    description = serviceDetails,
                    status = "WAITING_FOR_OFFERS",
                    urgencyTime = "فوراً (خلال 30 دقيقة)",
                    createdAt = now,
                    expiresAt = now + 30 * 60 * 1000L
                )

                firestore.collection("instant_requests").document(reqId).set(urgentReq).await()
                _uiState.value = UrgentUiState.Success("تم إرسال الطلب العاجل بنجاح")
                onSuccess(uniqueCode)
            } catch (e: Exception) {
                val err = e.localizedMessage ?: "حدث خطأ أثناء إنشاء الطلب"
                _uiState.value = UrgentUiState.Error(err)
                onError(err)
            }
        }
    }

    /**
     * تقديم عرض عاجل سري للطلب.
     */
    fun submitUrgentOffer(
        currentReq: InstantRequestEntity,
        price: Double,
        estimatedArrival: String,
        estimatedDuration: String,
        notesText: String,
        currentUserId: String,
        onSuccess: () -> Unit,
        onError: (message: String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = UrgentUiState.Loading
            try {
                val offerId = UUID.randomUUID().toString()
                val newOffer = RequestOfferEntity(
                    id = offerId,
                    requestId = currentReq.id,
                    requestCode = currentReq.requestCode,
                    technicianId = currentUserId,
                    technicianName = "فني طوارئ معتمد",
                    technicianPhone = currentUserId,
                    technicianAvatar = "",
                    technicianRating = 5.0f,
                    price = price,
                    estimatedArrivalTime = estimatedArrival,
                    estimatedDuration = estimatedDuration,
                    notes = "🚨 استجابة طوارئ: $notesText",
                    status = "PENDING",
                    createdAt = System.currentTimeMillis()
                )

                firestore.collection("instant_offers").document(offerId).set(newOffer).await()
                firestore.collection("instant_requests").document(currentReq.id)
                    .update("offersCount", FieldValue.increment(1)).await()

                val notifId = UUID.randomUUID().toString()
                val notif = NotificationEntity(
                    id = notifId,
                    title = "🚨 عرض طارئ لطلبك ${currentReq.requestCode}",
                    message = "وصلك عرض فوري من ${newOffer.technicianName} بسعر ${newOffer.price} ر.ي ووصول ${newOffer.estimatedArrivalTime}",
                    customerPhone = currentReq.userPhone,
                    targetType = "USER",
                    targetValue = currentReq.userPhone,
                    notificationType = "URGENT_OFFER",
                    timestamp = System.currentTimeMillis()
                )
                firestore.collection("notifications").document(notifId).set(notif).await()

                _uiState.value = UrgentUiState.Success("تم إرسال عرضك الفوري بنجاح")
                onSuccess()
            } catch (e: Exception) {
                val err = e.localizedMessage ?: "فشل تقديم العرض"
                _uiState.value = UrgentUiState.Error(err)
                onError(err)
            }
        }
    }

    /**
     * إلغاء الطلب العاجل بالرمز السري PIN.
     */
    fun cancelUrgentRequest(
        requestId: String,
        enteredPin: String,
        expectedPin: String,
        onSuccess: () -> Unit,
        onError: (message: String) -> Unit
    ) {
        if (enteredPin != expectedPin) {
            onError("رمز PIN غير صحيح!")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = UrgentUiState.Loading
            try {
                firestore.collection("instant_requests").document(requestId)
                    .update("status", "CANCELLED").await()
                _uiState.value = UrgentUiState.Success("تم إلغاء الطلب العاجل بنجاح")
                onSuccess()
            } catch (e: Exception) {
                val err = e.localizedMessage ?: "فشل إلغاء الطلب"
                _uiState.value = UrgentUiState.Error(err)
                onError(err)
            }
        }
    }

    /**
     * قبول عرض محدد للطلب العاجل.
     */
    fun acceptOffer(
        requestId: String,
        offerId: String,
        offer: RequestOfferEntity,
        onSuccess: () -> Unit,
        onError: (message: String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = UrgentUiState.Loading
            try {
                firestore.collection("instant_requests").document(requestId).update(
                    mapOf(
                        "status" to "ACCEPTED",
                        "acceptedOfferId" to offerId,
                        "acceptedTechnicianId" to offer.technicianId,
                        "acceptedTechnicianName" to offer.technicianName,
                        "acceptedTechnicianPhone" to offer.technicianPhone,
                        "acceptedPrice" to offer.price
                    )
                ).await()

                firestore.collection("instant_offers").document(offerId)
                    .update("status", "ACCEPTED").await()

                _uiState.value = UrgentUiState.Success("تم قبول العرض بنجاح!")
                onSuccess()
            } catch (e: Exception) {
                val err = e.localizedMessage ?: "فشل قبول العرض"
                _uiState.value = UrgentUiState.Error(err)
                onError(err)
            }
        }
    }

    fun clearUiState() {
        _uiState.value = UrgentUiState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        requestsListener?.remove()
        detailsListener?.remove()
        offersListener?.remove()
    }
}
