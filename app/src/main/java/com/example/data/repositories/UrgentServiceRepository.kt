package com.example.data.repositories

import androidx.annotation.Keep
import com.example.data.UrgentRequestEntity
import com.example.data.OfferEntity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.UUID

@Keep
class UrgentServiceRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val requestsCollection = firestore.collection("urgent_requests")
    private val offersCollection = firestore.collection("urgent_offers")

    /**
     * Publish a new urgent request
     */
    fun publishRequest(
        request: UrgentRequestEntity,
        onResult: (Boolean, String?) -> Unit
    ) {
        val reqId = if (request.id.isBlank()) UUID.randomUUID().toString() else request.id
        val finalRequest = request.copy(
            id = reqId,
            status = "OPEN",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        requestsCollection.document(reqId)
            .set(finalRequest)
            .addOnSuccessListener {
                onResult(true, reqId)
            }
            .addOnFailureListener { e ->
                onResult(false, e.localizedMessage)
            }
    }

    /**
     * Submit an offer from a service provider
     */
    fun submitOffer(
        offer: OfferEntity,
        onResult: (Boolean, String?) -> Unit
    ) {
        val offerId = if (offer.id.isBlank()) UUID.randomUUID().toString() else offer.id
        val finalOffer = offer.copy(
            id = offerId,
            status = "PENDING",
            createdAt = System.currentTimeMillis()
        )

        val batch = firestore.batch()
        
        // 1. Write the offer
        batch.set(offersCollection.document(offerId), finalOffer)
        
        // 2. Increment offers count and update parent request status
        val requestDoc = requestsCollection.document(offer.requestId)
        batch.update(
            requestDoc,
            mapOf(
                "status" to "HAS_OFFERS",
                "offersCount" to com.google.firebase.firestore.FieldValue.increment(1),
                "updatedAt" to System.currentTimeMillis()
            )
        )

        batch.commit()
            .addOnSuccessListener {
                onResult(true, offerId)
            }
            .addOnFailureListener { e ->
                onResult(false, e.localizedMessage)
            }
    }

    /**
     * Accept a specific provider offer
     */
    fun acceptOffer(
        requestId: String,
        offerId: String,
        providerId: String,
        providerName: String,
        providerPhone: String,
        price: Double,
        onResult: (Boolean, String?) -> Unit
    ) {
        val otpCode = "YEM-${(1000..9999).random()}"

        val batch = firestore.batch()

        // 1. Accept the offer
        batch.update(offersCollection.document(offerId), "status", "ACCEPTED")

        // 2. Reject other offers for the same request
        // (Will update locally or let background handle. For simplicity, update request with accepted state)
        batch.update(
            requestsCollection.document(requestId),
            mapOf(
                "status" to "ACCEPTED",
                "winningProviderId" to providerId,
                "winningOfferId" to offerId,
                "winningProviderName" to providerName,
                "winningProviderPhone" to providerPhone,
                "agreedPrice" to price,
                "verificationOtp" to otpCode,
                "updatedAt" to System.currentTimeMillis()
            )
        )

        batch.commit()
            .addOnSuccessListener {
                onResult(true, otpCode)
            }
            .addOnFailureListener { e ->
                onResult(false, e.localizedMessage)
            }
    }

    /**
     * Listen to active requests flow
     */
    fun getActiveRequestsFlow(cityId: String = "", limit: Int = 50): Flow<List<UrgentRequestEntity>> = callbackFlow {
        var query: Query = requestsCollection
            .orderBy("createdAt", Query.Direction.DESCENDING)

        if (cityId.isNotBlank()) {
            query = query.whereEqualTo("cityId", cityId)
        }

        val listener = query.limit(limit.toLong()).addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(UrgentRequestEntity::class.java)?.copy(id = doc.id)
                }
                trySend(list)
            }
        }

        awaitClose { listener.remove() }
    }
}
