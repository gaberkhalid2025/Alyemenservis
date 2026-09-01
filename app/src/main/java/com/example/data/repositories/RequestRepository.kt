package com.example.data.repositories

import android.content.Context
import android.util.Log
import com.example.data.models.InstantRequestEntity
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
import kotlin.random.Random

/**
 * ⚡ RequestRepository
 * Manages instant service requests with real-time Firestore sync and local state management.
 */
class RequestRepository(private val context: Context) {

    private val firestore = FirebaseFirestore.getInstance()

    private val _requests = MutableStateFlow<List<InstantRequestEntity>>(emptyList())
    val requests: StateFlow<List<InstantRequestEntity>> = _requests.asStateFlow()

    /**
     * Real-time stream of all open and active instant requests.
     */
    fun getInstantRequestsFlow(): Flow<List<InstantRequestEntity>> = callbackFlow {
        val listener: ListenerRegistration = firestore.collection("instant_requests")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(100)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("RequestRepository", "Error fetching requests: ${error.message}")
                    trySend(_requests.value)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(InstantRequestEntity::class.java)?.copy(id = doc.id)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    _requests.value = list
                    trySend(list)
                }
            }
        awaitClose { listener.remove() }
    }

    /**
     * Creates a new instant service request.
     */
    fun createInstantRequest(
        request: InstantRequestEntity,
        onSuccess: (InstantRequestEntity) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val docId = if (request.id.isNotBlank()) request.id else firestore.collection("instant_requests").document().id
            val requestCode = if (request.requestCode.isNotBlank()) request.requestCode else "R-${Random.nextInt(100000, 999999)}"
            val pin = if (request.secretPin.isNotBlank()) request.secretPin else "${Random.nextInt(1000, 9999)}"
            val cancelPass = if (request.cancellationPassword.isNotBlank()) request.cancellationPassword else "${Random.nextInt(1000, 9999)}"

            val newEntity = request.copy(
                id = docId,
                requestCode = requestCode,
                secretPin = pin,
                cancellationPassword = cancelPass,
                status = "WAITING_FOR_OFFERS",
                createdAt = System.currentTimeMillis(),
                expiresAt = System.currentTimeMillis() + 45 * 60 * 1000L // 45 mins
            )

            firestore.collection("instant_requests").document(docId)
                .set(newEntity)
                .addOnSuccessListener {
                    val current = _requests.value.toMutableList()
                    current.removeAll { it.id == docId }
                    current.add(0, newEntity)
                    _requests.value = current
                    onSuccess(newEntity)
                }
                .addOnFailureListener {
                    onError(it.localizedMessage ?: "فشل إنشاء الطلب")
                }
        } catch (e: Exception) {
            onError(e.localizedMessage ?: "خطأ في معالجة الطلب")
        }
    }

    /**
     * Cancels an instant request with PIN verification.
     */
    fun cancelInstantRequest(
        requestId: String,
        inputPin: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        firestore.collection("instant_requests").document(requestId).get()
            .addOnSuccessListener { doc ->
                val request = doc.toObject(InstantRequestEntity::class.java)
                if (request == null) {
                    onError("الطلب غير موجود")
                    return@addOnSuccessListener
                }

                val expectedPass = request.cancellationPassword.ifBlank { request.secretPin }
                val isMatch = BookingSecurityHelper.verifyPassword(inputPin, expectedPass)

                if (!isMatch) {
                    onError("رمز PIN للإلغاء غير صحيح")
                    return@addOnSuccessListener
                }

                firestore.collection("instant_requests").document(requestId)
                    .update("status", "CANCELLED")
                    .addOnSuccessListener {
                        _requests.value = _requests.value.map {
                            if (it.id == requestId) it.copy(status = "CANCELLED") else it
                        }
                        onSuccess()
                    }
                    .addOnFailureListener { onError(it.localizedMessage ?: "فشل إلغاء الطلب") }
            }
            .addOnFailureListener { onError(it.localizedMessage ?: "فشل الوصول للطلب") }
    }

    /**
     * Sets request as accepted when user confirms an offer.
     */
    fun acceptRequestOffer(
        requestId: String,
        offerId: String,
        technicianId: String,
        technicianName: String,
        technicianPhone: String,
        acceptedPrice: Double,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val updates = mapOf(
            "status" to "ACCEPTED",
            "acceptedOfferId" to offerId,
            "acceptedTechnicianId" to technicianId,
            "acceptedTechnicianName" to technicianName,
            "acceptedTechnicianPhone" to technicianPhone,
            "acceptedPrice" to acceptedPrice
        )

        firestore.collection("instant_requests").document(requestId)
            .update(updates)
            .addOnSuccessListener {
                _requests.value = _requests.value.map {
                    if (it.id == requestId) it.copy(
                        status = "ACCEPTED",
                        acceptedOfferId = offerId,
                        acceptedTechnicianId = technicianId,
                        acceptedTechnicianName = technicianName,
                        acceptedTechnicianPhone = technicianPhone,
                        acceptedPrice = acceptedPrice
                    ) else it
                }
                onSuccess()
            }
            .addOnFailureListener { onError(it.localizedMessage ?: "فشل قبول العرض") }
    }
}
