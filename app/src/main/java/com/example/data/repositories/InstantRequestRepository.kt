package com.example.data.repositories

import android.content.Context
import android.util.Log
import com.example.data.models.InstantRequestEntity
import com.example.data.models.RequestOfferEntity
import com.example.security.BookingSecurityHelper
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import java.util.UUID
import kotlin.random.Random

/**
 * ⚡ InstantRequestRepository
 * مستودع إدارة الطلبات الفورية والعاجلة مع دعم التدفق الحي (Flow)،
 * المؤقت التنازلي للطلب (30 دقيقة)، تقديم العروض، والمصادقة الأمنية.
 */
class InstantRequestRepository(private val context: Context? = null) {

    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    private val _requests = MutableStateFlow<List<InstantRequestEntity>>(emptyList())
    val requests: StateFlow<List<InstantRequestEntity>> = _requests.asStateFlow()

    /**
     * 1. إنشاء طلب فوري جديد مع مؤقت زمني مدته 30 دقيقة
     */
    fun createInstantRequest(
        request: InstantRequestEntity,
        onSuccess: (InstantRequestEntity) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        try {
            val docId = if (request.id.isNotBlank()) request.id else firestore.collection("instant_requests").document().id
            val requestCode = if (request.requestCode.isNotBlank()) request.requestCode else "URG-${Random.nextInt(100000, 999999)}"
            val pin = if (request.secretPin.isNotBlank()) request.secretPin else "${Random.nextInt(1000, 9999)}"
            val cancelPass = if (request.cancellationPassword.isNotBlank()) request.cancellationPassword else "${Random.nextInt(1000, 9999)}"
            val now = System.currentTimeMillis()
            val expiresAt = if (request.expiresAt > now) request.expiresAt else now + (30 * 60 * 1000L) // 30 mins

            val newEntity = request.copy(
                id = docId,
                requestCode = requestCode,
                secretPin = pin,
                cancellationPassword = cancelPass,
                status = if (request.status.isBlank()) "WAITING_FOR_OFFERS" else request.status,
                createdAt = if (request.createdAt > 0) request.createdAt else now,
                expiresAt = expiresAt
            )

            firestore.collection("instant_requests").document(docId).set(newEntity)
                .addOnSuccessListener {
                    val current = _requests.value.toMutableList()
                    current.removeAll { it.id == docId }
                    current.add(0, newEntity)
                    _requests.value = current
                    onSuccess(newEntity)
                }
                .addOnFailureListener {
                    onError(it.localizedMessage ?: "فشل إنشاء الطلب الفوري")
                }
        } catch (e: Exception) {
            Log.e("InstantRequestRepo", "Error creating instant request", e)
            onError(e.localizedMessage ?: "خطأ غير متوقع أثناء معالجة الطلب")
        }
    }

    /**
     * 2. تدفق حي لطلبات مستخدم معين (العميل)
     */
    fun getUserInstantRequests(userId: String): Flow<List<InstantRequestEntity>> = callbackFlow {
        val listener: ListenerRegistration = firestore.collection("instant_requests")
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(_requests.value.filter { it.userId == userId })
                    return@addSnapshotListener
                }

                val list = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(InstantRequestEntity::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        null
                    }
                }
                trySend(list)
            }

        awaitClose { listener.remove() }
    }

    /**
     * 3. تدفق حي للطلبات المتاحة للفنيين في مدينة/تصنيف معين
     */
    fun getAvailableInstantRequests(category: String = "", city: String = ""): Flow<List<InstantRequestEntity>> = callbackFlow {
        val query = firestore.collection("instant_requests")
            .whereEqualTo("status", "WAITING_FOR_OFFERS")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(100)

        val listener: ListenerRegistration = query.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) {
                trySend(emptyList())
                return@addSnapshotListener
            }

            val list = snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(InstantRequestEntity::class.java)?.copy(id = doc.id)
                } catch (e: Exception) {
                    null
                }
            }.filter { item ->
                (category.isBlank() || item.categoryName.contains(category, ignoreCase = true) || item.serviceTitle.contains(category, ignoreCase = true)) &&
                (city.isBlank() || item.userCity.contains(city, ignoreCase = true)) &&
                (item.expiresAt > System.currentTimeMillis())
            }

            trySend(list)
        }

        awaitClose { listener.remove() }
    }

    /**
     * 4. تقديم عرض سعر من مزود خدمة على طلب فوري
     */
    fun submitOffer(
        offer: RequestOfferEntity,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val offerId = if (offer.id.isNotBlank()) offer.id else UUID.randomUUID().toString()
        val finalOffer = offer.copy(id = offerId, createdAt = System.currentTimeMillis())

        val offerRef = firestore.collection("instant_requests")
            .document(offer.requestId)
            .collection("offers")
            .document(offerId)

        val requestRef = firestore.collection("instant_requests").document(offer.requestId)

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(requestRef)
            val currentOffers = snapshot.getLong("offersCount") ?: 0L
            transaction.set(offerRef, finalOffer)
            transaction.update(requestRef, "offersCount", currentOffers + 1)
        }.addOnSuccessListener {
            onSuccess()
        }.addOnFailureListener {
            onError(it.localizedMessage ?: "فشل تقديم العرض")
        }
    }

    /**
     * 5. قبول عرض السعر وبدء الخدمة
     */
    fun acceptOffer(
        requestId: String,
        offerId: String,
        providerId: String,
        providerName: String,
        providerPhone: String,
        acceptedPrice: Double,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val updates = mapOf(
            "status" to "ACCEPTED",
            "acceptedOfferId" to offerId,
            "acceptedTechnicianId" to providerId,
            "acceptedTechnicianName" to providerName,
            "acceptedTechnicianPhone" to providerPhone,
            "acceptedPrice" to acceptedPrice,
            "acceptedAt" to System.currentTimeMillis(),
            "updatedAt" to System.currentTimeMillis()
        )

        firestore.collection("instant_requests").document(requestId)
            .update(updates)
            .addOnSuccessListener {
                firestore.collection("instant_requests").document(requestId)
                    .collection("offers").document(offerId)
                    .update("status", "ACCEPTED")
                _requests.value = _requests.value.map {
                    if (it.id == requestId) it.copy(
                        status = "ACCEPTED",
                        acceptedOfferId = offerId,
                        acceptedTechnicianId = providerId,
                        acceptedTechnicianName = providerName,
                        acceptedTechnicianPhone = providerPhone,
                        acceptedPrice = acceptedPrice
                    ) else it
                }
                onSuccess()
            }
            .addOnFailureListener { onError(it.localizedMessage ?: "فشل قبول العرض") }
    }

    /**
     * 6. إلغاء الطلب الفوري مع التحقق الأمني
     */
    fun cancelInstantRequest(
        requestId: String,
        userPin: String = "",
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        firestore.collection("instant_requests").document(requestId).get()
            .addOnSuccessListener { doc ->
                val request = doc.toObject(InstantRequestEntity::class.java)
                if (request == null) {
                    onError("الطلب غير موجود")
                    return@addOnSuccessListener
                }

                if (userPin.isNotBlank()) {
                    val expectedPass = request.cancellationPassword.ifBlank { request.secretPin }
                    if (expectedPass.isNotBlank() && !BookingSecurityHelper.verifyPassword(userPin, expectedPass)) {
                        onError("رمز PIN للإلغاء غير صحيح")
                        return@addOnSuccessListener
                    }
                }

                val updates = mapOf(
                    "status" to "CANCELLED",
                    "cancelledAt" to System.currentTimeMillis(),
                    "updatedAt" to System.currentTimeMillis()
                )

                firestore.collection("instant_requests").document(requestId).update(updates)
                    .addOnSuccessListener {
                        _requests.value = _requests.value.map {
                            if (it.id == requestId) it.copy(status = "CANCELLED") else it
                        }
                        onSuccess()
                    }
                    .addOnFailureListener { onError(it.localizedMessage ?: "فشل إلغاء الطلب") }
            }
            .addOnFailureListener { onError(it.localizedMessage ?: "فشل الوصول لبيانات الطلب") }
    }

    /**
     * 7. إكمال الطلب الفوري بنجاح
     */
    fun completeInstantRequest(
        requestId: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val updates = mapOf(
            "status" to "COMPLETED",
            "completedAt" to System.currentTimeMillis(),
            "updatedAt" to System.currentTimeMillis()
        )

        firestore.collection("instant_requests").document(requestId).update(updates)
            .addOnSuccessListener {
                _requests.value = _requests.value.map {
                    if (it.id == requestId) it.copy(status = "COMPLETED") else it
                }
                onSuccess()
            }
            .addOnFailureListener { onError(it.localizedMessage ?: "فشل إكمال الطلب") }
    }
}
