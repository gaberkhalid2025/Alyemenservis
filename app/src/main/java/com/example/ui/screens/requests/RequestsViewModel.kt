package com.example.ui.screens.requests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BookingEntity
import com.example.data.NotificationEntity
import com.example.data.InstantRequestEntity
import com.example.data.models.LoadingState
import com.example.data.RequestOfferEntity
import com.example.data.models.ChatChannel
import com.example.data.models.ChannelType
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * 💼 RequestsViewModel
 * إدارة دورة حياة طلبات الخدمات الفورية والعروض والمحادثات المرتبطة
 */
class RequestsViewModel(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ViewModel() {

    private val _requestsState = MutableStateFlow<LoadingState<List<InstantRequestEntity>>>(LoadingState.Idle)
    val requestsState: StateFlow<LoadingState<List<InstantRequestEntity>>> = _requestsState.asStateFlow()

    private val _myRequests = MutableStateFlow<List<InstantRequestEntity>>(emptyList())
    val myRequests: StateFlow<List<InstantRequestEntity>> = _myRequests.asStateFlow()

    private val _currentRequest = MutableStateFlow<InstantRequestEntity?>(null)
    val currentRequest: StateFlow<InstantRequestEntity?> = _currentRequest.asStateFlow()

    private val _currentOffers = MutableStateFlow<List<RequestOfferEntity>>(emptyList())
    val currentOffers: StateFlow<List<RequestOfferEntity>> = _currentOffers.asStateFlow()

    private val _actionState = MutableStateFlow<LoadingState<String>>(LoadingState.Idle)
    val actionState: StateFlow<LoadingState<String>> = _actionState.asStateFlow()

    private var requestsListener: ListenerRegistration? = null
    private var requestDetailsListener: ListenerRegistration? = null
    private var offersListener: ListenerRegistration? = null

    init {
        listenToAllRequests()
    }

    /**
     * الاستماع لجميع الطلبات في الوقت الفعلي مع دعم وضع عدم الاتصال
     */
    fun listenToAllRequests() {
        _requestsState.value = LoadingState.Loading
        requestsListener?.remove()
        requestsListener = firestore.collection("instant_requests")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _requestsState.value = LoadingState.Error(error.localizedMessage ?: "فشل تحميل الطلبات", error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { it.toObject(InstantRequestEntity::class.java) }
                        .sortedByDescending { it.createdAt }
                    if (list.isEmpty()) {
                        _requestsState.value = LoadingState.Empty
                    } else {
                        _requestsState.value = LoadingState.Success(list)
                    }
                } else {
                    _requestsState.value = LoadingState.Empty
                }
            }
    }

    /**
     * الاستماع لتفاصيل طلب محدد
     */
    fun listenToRequestDetails(requestId: String) {
        if (requestId.isBlank()) return
        requestDetailsListener?.remove()
        requestDetailsListener = firestore.collection("instant_requests").document(requestId)
            .addSnapshotListener { snapshot, error ->
                if (snapshot != null && snapshot.exists()) {
                    _currentRequest.value = snapshot.toObject(InstantRequestEntity::class.java)
                }
            }

        listenToOffers(requestId)
    }

    /**
     * الاستماع للعروض المقدمة للطلب
     */
    fun listenToOffers(requestId: String) {
        if (requestId.isBlank()) return
        offersListener?.remove()
        offersListener = firestore.collection("instant_offers")
            .whereEqualTo("requestId", requestId)
            .addSnapshotListener { snapshot, error ->
                if (snapshot != null) {
                    _currentOffers.value = snapshot.documents.mapNotNull { it.toObject(RequestOfferEntity::class.java) }
                }
            }
    }

    /**
     * نشر طلب خدمة جديد
     */
    fun createRequest(
        request: InstantRequestEntity,
        onSuccess: (InstantRequestEntity) -> Unit,
        onError: (String) -> Unit
    ) {
        _actionState.value = LoadingState.Loading
        viewModelScope.launch {
            firestore.collection("instant_requests").document(request.id).set(request)
                .addOnSuccessListener {
                    _actionState.value = LoadingState.Success("تم نشر طلبك بنجاح!")
                    onSuccess(request)
                }
                .addOnFailureListener { e ->
                    val errorMsg = e.localizedMessage ?: "حدث خطأ أثناء حفظ الطلب"
                    _actionState.value = LoadingState.Error(errorMsg, e)
                    onError(errorMsg)
                }
        }
    }

    /**
     * تقديم عرض سعر من الفني
     */
    fun submitOffer(
        offer: RequestOfferEntity,
        userPhone: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        _actionState.value = LoadingState.Loading
        viewModelScope.launch {
            firestore.collection("instant_offers").document(offer.id).set(offer)
                .addOnSuccessListener {
                    // تحديث عداد العروض
                    firestore.collection("instant_requests").document(offer.requestId)
                        .update("offersCount", FieldValue.increment(1))

                    // إرسال إشعار للعميل
                    val notifId = UUID.randomUUID().toString()
                    val notif = NotificationEntity(
                        id = notifId,
                        title = "وصلك عرض جديد لطلبك",
                        message = "قدم لك ${offer.technicianName} عرضاً بسعر ${offer.price} ر.ي",
                        customerPhone = userPhone,
                        targetType = "USER",
                        targetValue = userPhone,
                        notificationType = "NEW_OFFER",
                        timestamp = System.currentTimeMillis()
                    )
                    firestore.collection("notifications").document(notifId).set(notif)

                    _actionState.value = LoadingState.Success("تم إرسال العرض بنجاح!")
                    onSuccess()
                }
                .addOnFailureListener { e ->
                    val errorMsg = e.localizedMessage ?: "فشل إرسال العرض"
                    _actionState.value = LoadingState.Error(errorMsg, e)
                    onError(errorMsg)
                }
        }
    }

    /**
     * قبول عرض معين وإنشاء الحجز والمحادثة
     */
    fun acceptOffer(
        requestId: String,
        selectedOfferId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        _actionState.value = LoadingState.Loading
        viewModelScope.launch {
            // 1. جلب العرض المختار
            firestore.collection("instant_offers").document(selectedOfferId).get()
                .addOnSuccessListener { offerDoc ->
                    val offer = offerDoc.toObject(RequestOfferEntity::class.java)
                    if (offer == null) {
                        _actionState.value = LoadingState.Error("العرض غير موجود")
                        onError("العرض غير موجود")
                        return@addOnSuccessListener
                    }

                    // 2. جلب الطلب
                    firestore.collection("instant_requests").document(requestId).get()
                        .addOnSuccessListener { reqDoc ->
                            val req = reqDoc.toObject(InstantRequestEntity::class.java)
                            if (req == null) {
                                _actionState.value = LoadingState.Error("الطلب غير موجود")
                                onError("الطلب غير موجود")
                                return@addOnSuccessListener
                            }

                            // 3. تحديث حالة العرض والطلب
                            val batch = firestore.batch()
                            val reqRef = firestore.collection("instant_requests").document(requestId)
                            batch.update(reqRef, mapOf("status" to "ACCEPTED", "acceptedOfferId" to selectedOfferId))

                            val offerRef = firestore.collection("instant_offers").document(selectedOfferId)
                            batch.update(offerRef, mapOf("status" to "ACCEPTED"))

                            // 4. إنشاء حجز جديد رسمي
                            val bookingId = UUID.randomUUID().toString()
                            val booking = BookingEntity(
                                id = bookingId,
                                clientId = req.userId.ifBlank { req.userPhone },
                                clientName = req.userName,
                                clientPhone = req.userPhone,
                                customerName = req.userName,
                                customerPhone = req.userPhone,
                                customerArea = "${req.userCity} - ${req.userNeighborhood}",
                                clientAddress = "${req.userCity} - ${req.userNeighborhood}",
                                fullAddress = "${req.userCity} - ${req.userNeighborhood}",
                                providerId = offer.technicianId,
                                providerName = offer.technicianName,
                                providerPhone = offer.technicianPhone,
                                serviceType = req.serviceTitle,
                                category = req.categoryId,
                                subCategory = req.categoryName,
                                totalAmount = offer.price,
                                status = "APPROVED",
                                date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date()),
                                time = offer.estimatedArrivalTime,
                                dateString = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date()),
                                timeString = offer.estimatedArrivalTime,
                                serviceDetails = "حجز تم تأكيده من خلال نظام العروض الفورية",
                                pinCode = req.secretPin.ifBlank { req.cancellationPassword },
                                createdAt = System.currentTimeMillis()
                            )
                            val bookingRef = firestore.collection("bookings").document(bookingId)
                            batch.set(bookingRef, booking)

                            // 5. إشعار للفني
                            val notifId = UUID.randomUUID().toString()
                            val notif = NotificationEntity(
                                id = notifId,
                                title = "🎉 تم قبول عرضك!",
                                message = "وافق العميل ${req.userName} على عرضك لطلب ${req.requestCode}",
                                customerPhone = offer.technicianPhone,
                                targetType = "PROVIDER",
                                targetValue = offer.technicianPhone,
                                notificationType = "OFFER_ACCEPTED",
                                timestamp = System.currentTimeMillis()
                            )
                            val notifRef = firestore.collection("notifications").document(notifId)
                            batch.set(notifRef, notif)

                            // تنفيذ العمليات في Batch
                            batch.commit()
                                .addOnSuccessListener {
                                    _actionState.value = LoadingState.Success("تم قبول العرض وإنشاء الحجز بنجاح!")
                                    onSuccess()
                                }
                                .addOnFailureListener { e ->
                                    val err = e.localizedMessage ?: "فشل إتمام العملية"
                                    _actionState.value = LoadingState.Error(err, e)
                                    onError(err)
                                }
                        }
                        .addOnFailureListener { e ->
                            val err = e.localizedMessage ?: "فشل جلب بيانات الطلب"
                            _actionState.value = LoadingState.Error(err, e)
                            onError(err)
                        }
                }
                .addOnFailureListener { e ->
                    val err = e.localizedMessage ?: "فشل جلب بيانات العرض"
                    _actionState.value = LoadingState.Error(err, e)
                    onError(err)
                }
        }
    }

    /**
     * إلغاء الطلب باستخدام رمز PIN السري
     */
    fun cancelRequest(
        requestId: String,
        enteredPin: String,
        expectedPin: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (enteredPin.trim() != expectedPin.trim()) {
            onError("رمز PIN السري غير صحيح!")
            return
        }

        _actionState.value = LoadingState.Loading
        viewModelScope.launch {
            firestore.collection("instant_requests").document(requestId)
                .update("status", "CANCELLED")
                .addOnSuccessListener {
                    _actionState.value = LoadingState.Success("تم إلغاء الطلب بنجاح")
                    onSuccess()
                }
                .addOnFailureListener { e ->
                    val err = e.localizedMessage ?: "فشل إلغاء الطلب"
                    _actionState.value = LoadingState.Error(err, e)
                    onError(err)
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        requestsListener?.remove()
        requestDetailsListener?.remove()
        offersListener?.remove()
    }
}
