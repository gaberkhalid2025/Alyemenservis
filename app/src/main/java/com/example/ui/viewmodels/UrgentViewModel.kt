package com.example.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.NotificationEntity
import com.example.data.models.InstantRequestEntity
import com.example.data.models.RequestOfferEntity
import com.example.security.BookingSecurityHelper
import com.google.firebase.firestore.FieldValue
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
 * 🔒 UrgentUiState
 */
sealed class UrgentUiState {
    object Idle : UrgentUiState()
    object Loading : UrgentUiState()
    data class Success(val message: String? = null) : UrgentUiState()
    data class Error(val message: String) : UrgentUiState()
    object Empty : UrgentUiState()
}

sealed class UrgentEvent {
    data class ShowToast(val message: String) : UrgentEvent()
    data class ShowSnackbar(val message: String) : UrgentEvent()
}

/**
 * 🚨 UrgentViewModel
 * Manages 30-min urgent requests flow, offers tracking, countdown updates, and security PIN verification.
 */
class UrgentViewModel : ViewModel() {

    private val firestore by lazy { FirebaseFirestore.getInstance() }

    private val _uiState = MutableStateFlow<UrgentUiState>(UrgentUiState.Idle)
    val uiState: StateFlow<UrgentUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<UrgentEvent>()
    val eventFlow: SharedFlow<UrgentEvent> = _eventFlow.asSharedFlow()

    private val _urgentRequests = MutableStateFlow<List<InstantRequestEntity>>(emptyList())
    val urgentRequests: StateFlow<List<InstantRequestEntity>> = _urgentRequests.asStateFlow()

    private val _selectedRequest = MutableStateFlow<InstantRequestEntity?>(null)
    val selectedRequest: StateFlow<InstantRequestEntity?> = _selectedRequest.asStateFlow()

    private val _offersForRequest = MutableStateFlow<List<RequestOfferEntity>>(emptyList())
    val offersForRequest: StateFlow<List<RequestOfferEntity>> = _offersForRequest.asStateFlow()

    private var requestsListener: ListenerRegistration? = null
    private var detailsListener: ListenerRegistration? = null
    private var offersListener: ListenerRegistration? = null

    fun observeUrgentRequests(currentUserId: String, isProvider: Boolean) {
        requestsListener?.remove()

        var query: Query = firestore.collection("urgent_requests")
        if (isProvider) {
            query = query.whereIn("status", listOf("WAITING_FOR_OFFERS", "REVIEWING_OFFERS"))
        } else if (currentUserId.isNotBlank()) {
            query = query.whereEqualTo("userId", currentUserId)
        }

        requestsListener = query.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) {
                _urgentRequests.value = emptyList()
                return@addSnapshotListener
            }

            val list = snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(InstantRequestEntity::class.java)?.copy(id = doc.id)
                } catch (e: Exception) {
                    null
                }
            }
            _urgentRequests.value = list
        }
    }

    fun observeRequestDetails(requestId: String) {
        if (requestId.isBlank()) return

        detailsListener?.remove()
        detailsListener = firestore.collection("urgent_requests").document(requestId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) {
                    _selectedRequest.value = null
                    return@addSnapshotListener
                }
                _selectedRequest.value = snapshot.toObject(InstantRequestEntity::class.java)?.copy(id = snapshot.id)
            }

        offersListener?.remove()
        offersListener = firestore.collection("urgent_requests").document(requestId)
            .collection("offers")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    _offersForRequest.value = emptyList()
                    return@addSnapshotListener
                }
                val list = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(RequestOfferEntity::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        null
                    }
                }
                _offersForRequest.value = list
            }
    }

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
        onSuccess: (code: String) -> Unit,
        onError: (message: String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = UrgentUiState.Loading
            try {
                val reqId = UUID.randomUUID().toString()
                val uniqueCode = "URG-${(1000..9999).random()}"
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

                firestore.collection("urgent_requests").document(reqId).set(urgentReq).await()
                _uiState.value = UrgentUiState.Success("تم إرسال الطلب العاجل بنجاح")
                onSuccess(uniqueCode)
            } catch (e: Exception) {
                val err = e.localizedMessage ?: "حدث خطأ أثناء إنشاء الطلب"
                _uiState.value = UrgentUiState.Error(err)
                onError(err)
            }
        }
    }

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

                firestore.collection("urgent_requests").document(currentReq.id)
                    .collection("offers").document(offerId).set(newOffer).await()

                firestore.collection("urgent_requests").document(currentReq.id)
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

    fun acceptOffer(
        requestId: String,
        offerId: String,
        providerPhone: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = UrgentUiState.Loading
            try {
                val reqDoc = firestore.collection("urgent_requests").document(requestId).get().await()
                val reqObj = reqDoc.toObject(InstantRequestEntity::class.java)

                firestore.collection("urgent_requests").document(requestId).update(
                    mapOf(
                        "status" to "ACCEPTED",
                        "acceptedOfferId" to offerId,
                        "acceptedTechnicianPhone" to providerPhone
                    )
                ).await()

                firestore.collection("urgent_requests").document(requestId)
                    .collection("offers").document(offerId)
                    .update("status", "ACCEPTED").await()

                if (reqObj != null) {
                    val bookingId = "BK-URG-" + UUID.randomUUID().toString().take(8)
                    val bookingData = mapOf(
                        "id" to bookingId,
                        "bookingNumber" to (reqObj.requestCode.ifBlank { bookingId }),
                        "bookingCode" to reqObj.requestCode,
                        "clientId" to reqObj.userId,
                        "clientName" to reqObj.userName,
                        "clientPhone" to reqObj.userPhone,
                        "customerName" to reqObj.userName,
                        "customerPhone" to reqObj.userPhone,
                        "providerPhone" to providerPhone,
                        "category" to reqObj.categoryName,
                        "serviceType" to reqObj.serviceTitle,
                        "serviceDetails" to reqObj.description,
                        "date" to "اليوم (عاجل)",
                        "time" to "فوراً (30 دقيقة)",
                        "status" to "APPROVED",
                        "city" to reqObj.userCity,
                        "address" to "${reqObj.userCity} - ${reqObj.userNeighborhood}",
                        "bookingPassword" to reqObj.secretPin,
                        "pinCode" to reqObj.secretPin,
                        "createdAt" to System.currentTimeMillis(),
                        "scheduledAt" to System.currentTimeMillis() + 30 * 60 * 1000L
                    )
                    firestore.collection("bookings").document(bookingId).set(bookingData).await()
                }

                _uiState.value = UrgentUiState.Success("تم قبول العرض بنجاح!")
                onResult(true, "تم قبول العرض بنجاح")
            } catch (e: Exception) {
                val err = e.localizedMessage ?: "فشل قبول العرض"
                _uiState.value = UrgentUiState.Error(err)
                onResult(false, err)
            }
        }
    }

    fun acceptOffer(
        requestId: String,
        offerId: String,
        offer: RequestOfferEntity,
        onSuccess: () -> Unit,
        onError: (message: String) -> Unit
    ) {
        acceptOffer(requestId, offerId, offer.technicianPhone) { success, msg ->
            if (success) onSuccess() else onError(msg ?: "حدث خطأ")
        }
    }

    fun cancelUrgentRequest(
        requestId: String,
        enteredPin: String,
        context: Context,
        onResult: (Boolean, String?) -> Unit
    ) {
        if (BookingSecurityHelper.isBookingLocked(context, requestId)) {
            val remainingSecs = BookingSecurityHelper.getRemainingLockoutSeconds(context, requestId)
            val msg = "الحساب مقفل مؤقتاً لأسباب أمنية بسبب أدخال PIN خاطئ 3 مرات. انتظر $remainingSecs ثانية."
            _uiState.value = UrgentUiState.Error(msg)
            onResult(false, msg)
            return
        }

        if (enteredPin.isBlank() || enteredPin.length < 4) {
            val remaining = BookingSecurityHelper.recordFailedAttempt(context, requestId)
            val msg = "رمز PIN غير صحيح. محاولات متبقية: $remaining"
            _uiState.value = UrgentUiState.Error(msg)
            onResult(false, msg)
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = UrgentUiState.Loading
            try {
                BookingSecurityHelper.resetAttempts(context, requestId)
                firestore.collection("urgent_requests").document(requestId)
                    .update("status", "CANCELLED").await()
                _uiState.value = UrgentUiState.Success("تم إلغاء الطلب العاجل بنجاح")
                onResult(true, "تم إلغاء الطلب بنجاح")
            } catch (e: Exception) {
                val err = e.localizedMessage ?: "فشل إلغاء الطلب"
                _uiState.value = UrgentUiState.Error(err)
                onResult(false, err)
            }
        }
    }

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
                firestore.collection("urgent_requests").document(requestId)
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
