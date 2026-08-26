package com.example.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BookingEntity
import com.example.data.InstantRequestEntity
import com.example.data.NotificationEntity
import com.example.data.RequestOfferEntity
import com.example.data.UrgentRequestEntity
import com.example.util.OfflineQueueManager
import com.example.util.OfflineRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ⚡ RequestViewModel
 * إدارة طلبات "اطلب خدمتك الآن" والطلبات المستعجلة، العروض والمزايدات الفورية والقبول والرفض.
 */
class RequestViewModel(application: Application) : AndroidViewModel(application) {

    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val offlineQueueManager: OfflineQueueManager by lazy { OfflineQueueManager(getApplication()) }

    private val _requests = MutableStateFlow<List<InstantRequestEntity>>(emptyList())
    val requests: StateFlow<List<InstantRequestEntity>> = _requests.asStateFlow()

    private val _urgentRequests = MutableStateFlow<List<UrgentRequestEntity>>(emptyList())
    val urgentRequests: StateFlow<List<UrgentRequestEntity>> = _urgentRequests.asStateFlow()

    private val _offers = MutableStateFlow<Map<String, List<RequestOfferEntity>>>(emptyMap())
    val offers: StateFlow<Map<String, List<RequestOfferEntity>>> = _offers.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private var requestsListener: ListenerRegistration? = null
    private var urgentListener: ListenerRegistration? = null
    private var offersListener: ListenerRegistration? = null

    init {
        loadAllOpenRequests()
    }

    /**
     * تحميل جميع الطلبات المفتوحة في الوقت الفعلي
     */
    fun loadAllOpenRequests() {
        requestsListener?.remove()
        requestsListener = firestore.collection("instant_requests")
            .whereIn("status", listOf("OPEN", "PENDING", "ACTIVE"))
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { it.toObject(InstantRequestEntity::class.java) }
                    _requests.value = list.sortedByDescending { it.createdAt }
                }
            }

        urgentListener?.remove()
        urgentListener = firestore.collection("urgent_requests")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { it.toObject(UrgentRequestEntity::class.java) }
                    _urgentRequests.value = list.sortedByDescending { it.createdAt }
                }
            }
    }

    /**
     * تحميل طلبات مستخدم معين
     */
    fun loadUserRequests(userPhone: String) {
        if (userPhone.isBlank()) return
        firestore.collection("instant_requests")
            .whereEqualTo("userPhone", userPhone.trim())
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { it.toObject(InstantRequestEntity::class.java) }
                    _requests.value = list.sortedByDescending { it.createdAt }
                }
            }
    }

    /**
     * تحميل العروض لطلب محدد
     */
    fun loadOffersForRequest(requestId: String) {
        if (requestId.isBlank()) return
        firestore.collection("instant_offers")
            .whereEqualTo("requestId", requestId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { it.toObject(RequestOfferEntity::class.java) }
                    _offers.value = _offers.value + (requestId to list)
                }
            }
    }

    /**
     * نشر طلب خدمة فوري جديد
     */
    fun submitRequest(req: InstantRequestEntity, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val id = if (req.id.isNotBlank()) req.id else UUID.randomUUID().toString()
            val finalReq = req.copy(
                id = id,
                requestCode = if (req.requestCode.isNotBlank()) req.requestCode else "REQ-${System.currentTimeMillis().toString().takeLast(6)}",
                createdAt = if (req.createdAt == 0L) System.currentTimeMillis() else req.createdAt
            )

            firestore.collection("instant_requests").document(id).set(finalReq)
                .addOnSuccessListener {
                    _isLoading.value = false
                    _requests.value = listOf(finalReq) + _requests.value
                    _message.value = "تم نشر طلبك بنجاح!"
                    onComplete(true)
                }
                .addOnFailureListener {
                    _isLoading.value = false
                    offlineQueueManager.addToQueue(
                        OfflineRequest(
                            id = id,
                            type = "REQUEST",
                            data = mapOf(
                                "id" to id,
                                "userPhone" to finalReq.userPhone,
                                "categoryId" to finalReq.categoryId,
                                "description" to finalReq.description
                            )
                        )
                    )
                    _message.value = "تم حفظ الطلب محلياً وسيتم إرساله عند توفر الإنترنت."
                    onComplete(true)
                }
        }
    }

    /**
     * تقديم عرض سعر من الفني
     */
    fun submitOffer(offer: RequestOfferEntity, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val offerId = if (offer.id.isNotBlank()) offer.id else UUID.randomUUID().toString()
            val finalOffer = offer.copy(id = offerId, createdAt = System.currentTimeMillis())

            val batch = firestore.batch()
            val offerRef = firestore.collection("instant_offers").document(offerId)
            batch.set(offerRef, finalOffer)

            val reqRef = firestore.collection("instant_requests").document(finalOffer.requestId)
            batch.update(reqRef, "offersCount", FieldValue.increment(1))

            batch.commit()
                .addOnSuccessListener {
                    _isLoading.value = false
                    _message.value = "تم إرسال عرضك بنجاح!"
                    onComplete(true)
                }
                .addOnFailureListener {
                    _isLoading.value = false
                    _message.value = "فشل إرسال العرض"
                    onComplete(false)
                }
        }
    }

    /**
     * قبول عرض معين من قبل العميل
     */
    fun acceptOffer(requestId: String, offerId: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val batch = firestore.batch()

            val reqRef = firestore.collection("instant_requests").document(requestId)
            batch.update(reqRef, mapOf("status" to "ACCEPTED", "acceptedOfferId" to offerId))

            val offerRef = firestore.collection("instant_offers").document(offerId)
            batch.update(offerRef, "status", "ACCEPTED")

            batch.commit()
                .addOnSuccessListener {
                    _isLoading.value = false
                    _message.value = "تم قبول العرض بنجاح!"
                    onComplete(true)
                }
                .addOnFailureListener {
                    _isLoading.value = false
                    _message.value = "فشل قبول العرض"
                    onComplete(false)
                }
        }
    }

    /**
     * رفض عرض
     */
    fun rejectOffer(offerId: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            firestore.collection("instant_offers").document(offerId)
                .update("status", "REJECTED")
                .addOnSuccessListener { onComplete(true) }
                .addOnFailureListener { onComplete(false) }
        }
    }

    /**
     * إلغاء طلب
     */
    fun cancelRequest(requestId: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            firestore.collection("instant_requests").document(requestId)
                .update("status", "CANCELLED")
                .addOnSuccessListener { onComplete(true) }
                .addOnFailureListener { onComplete(false) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        requestsListener?.remove()
        urgentListener?.remove()
        offersListener?.remove()
    }
}
