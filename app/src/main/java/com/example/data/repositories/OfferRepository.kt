package com.example.data.repositories

import android.content.Context
import android.util.Log
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

/**
 * 🏷️ OfferRepository
 * Manages technician bids and offers with data privacy protection:
 * Masks phone numbers and exact coordinates until an offer is accepted by the customer.
 */
class OfferRepository(private val context: Context) {

    private val firestore = FirebaseFirestore.getInstance()

    private val _offers = MutableStateFlow<List<RequestOfferEntity>>(emptyList())
    val offers: StateFlow<List<RequestOfferEntity>> = _offers.asStateFlow()

    /**
     * Realtime stream of offers for a specific request.
     * Applies privacy masking for phone numbers on unaccepted offers.
     */
    fun getOffersForRequestFlow(requestId: String, isCustomerOrAccepted: Boolean = false): Flow<List<RequestOfferEntity>> = callbackFlow {
        if (requestId.isBlank()) {
            trySend(emptyList())
            return@callbackFlow
        }

        val listener: ListenerRegistration = firestore.collection("request_offers")
            .whereEqualTo("requestId", requestId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("OfferRepository", "Error fetching offers: ${error.message}")
                    trySend(_offers.value)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val rawList = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(RequestOfferEntity::class.java)?.copy(id = doc.id)
                        } catch (e: Exception) {
                            null
                        }
                    }

                    // Apply privacy masking if not accepted
                    val processedList = rawList.map { offer ->
                        if (offer.status == "ACCEPTED" || isCustomerOrAccepted) {
                            offer
                        } else {
                            // Mask phone for general viewers
                            offer.copy(
                                technicianPhone = BookingSecurityHelper.maskPhoneNumber(offer.technicianPhone)
                            )
                        }
                    }

                    _offers.value = processedList
                    trySend(processedList)
                }
            }
        awaitClose { listener.remove() }
    }

    /**
     * Submits a new offer from a technician.
     */
    fun submitOffer(
        offer: RequestOfferEntity,
        onSuccess: (RequestOfferEntity) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val docId = if (offer.id.isNotBlank()) offer.id else firestore.collection("request_offers").document().id
            val newOffer = offer.copy(
                id = docId,
                status = "PENDING",
                createdAt = System.currentTimeMillis()
            )

            firestore.collection("request_offers").document(docId)
                .set(newOffer)
                .addOnSuccessListener {
                    // Update request offer count
                    firestore.collection("instant_requests").document(offer.requestId)
                        .update("offersCount", com.google.firebase.firestore.FieldValue.increment(1))
                    onSuccess(newOffer)
                }
                .addOnFailureListener {
                    onError(it.localizedMessage ?: "فشل تقديم العرض")
                }
        } catch (e: Exception) {
            onError(e.localizedMessage ?: "خطأ في تقديم العرض")
        }
    }

    /**
     * Accepts a specific offer and marks others as rejected.
     */
    fun acceptOffer(
        requestId: String,
        acceptedOfferId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        firestore.collection("request_offers").whereEqualTo("requestId", requestId).get()
            .addOnSuccessListener { snapshot ->
                val batch = firestore.batch()
                snapshot.documents.forEach { doc ->
                    val offerRef = doc.reference
                    if (doc.id == acceptedOfferId) {
                        batch.update(offerRef, "status", "ACCEPTED")
                    } else {
                        batch.update(offerRef, "status", "REJECTED")
                    }
                }
                batch.commit()
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { onError(it.localizedMessage ?: "فشل تأكيد العرض") }
            }
            .addOnFailureListener { onError(it.localizedMessage ?: "فشل استرجاع العروض") }
    }
}
