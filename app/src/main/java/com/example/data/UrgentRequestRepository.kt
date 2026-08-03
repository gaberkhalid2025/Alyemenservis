package com.example.data

import androidx.annotation.Keep
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.UUID

@Keep
data class UrgentRequestEntity(
    val id: String = UUID.randomUUID().toString(),
    val customerId: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val category: String = "", // e.g., "سباكة", "كهرباء", "تكييف", "سيارات", "قطع غيار"
    val description: String = "",
    val cityId: String = "صنعاء",
    val area: String = "",
    val localNeighborhood: String = "",
    val latitude: Double = 15.3694,
    val longitude: Double = 44.1910,
    val imageUrl: String = "",
    val audioNoteUrl: String = "",
    val audioDurationSeconds: Int = 0,
    val status: String = "OPEN", // OPEN, HAS_OFFERS, ACCEPTED, IN_PROGRESS, COMPLETED, CANCELLED
    val winningProviderId: String = "",
    val winningOfferId: String = "",
    val winningProviderName: String = "",
    val winningProviderPhone: String = "",
    val agreedPrice: Double = 0.0,
    val verificationOtp: String = "", // e.g., "YEM-8942"
    val cancellationReason: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Keep
data class OfferEntity(
    val id: String = UUID.randomUUID().toString(),
    val requestId: String = "",
    val providerId: String = "",
    val providerName: String = "",
    val providerPhone: String = "",
    val providerRating: Float = 5.0f,
    val price: Double = 0.0,
    val estimatedEtaMinutes: Int = 30,
    val notes: String = "",
    val status: String = "PENDING", // PENDING, ACCEPTED, REJECTED
    val createdAt: Long = System.currentTimeMillis()
)

class UrgentRequestRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val requestsCollection = firestore.collection("urgent_requests")
    private val offersCollection = firestore.collection("urgent_offers")

    /**
     * Create a new urgent request ("اطلب خدمتك الآن")
     */
    fun createUrgentRequest(
        request: UrgentRequestEntity,
        onResult: (Boolean, String?) -> Unit
    ) {
        val reqId = if (request.id.isBlank()) UUID.randomUUID().toString() else request.id
        val finalReq = request.copy(
            id = reqId,
            status = "OPEN",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        requestsCollection.document(reqId)
            .set(finalReq)
            .addOnSuccessListener {
                onResult(true, reqId)
            }
            .addOnFailureListener { e ->
                onResult(false, e.localizedMessage)
            }
    }

    /**
     * Submit a provider bidding offer for a request
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

        offersCollection.document(offerId)
            .set(finalOffer)
            .addOnSuccessListener {
                // Update parent request status to HAS_OFFERS if it's currently OPEN
                requestsCollection.document(offer.requestId).get()
                    .addOnSuccessListener { doc ->
                        if (doc.exists() && doc.getString("status") == "OPEN") {
                            requestsCollection.document(offer.requestId).update(
                                mapOf(
                                    "status" to "HAS_OFFERS",
                                    "updatedAt" to System.currentTimeMillis()
                                )
                            )
                        }
                    }
                onResult(true, offerId)
            }
            .addOnFailureListener { e ->
                onResult(false, e.localizedMessage)
            }
    }

    /**
     * Accept a provider offer, generates verification OTP (e.g., YEM-8942)
     */
    fun acceptOffer(
        requestId: String,
        offer: OfferEntity,
        onResult: (Boolean, String?) -> Unit
    ) {
        val otpCode = "YEM-${(1000..9999).random()}"
        
        firestore.runBatch { batch ->
            // 1. Update winning offer
            batch.update(
                offersCollection.document(offer.id),
                mapOf("status" to "ACCEPTED")
            )

            // 2. Update request state
            batch.update(
                requestsCollection.document(requestId),
                mapOf(
                    "status" to "ACCEPTED",
                    "winningProviderId" to offer.providerId,
                    "winningOfferId" to offer.id,
                    "winningProviderName" to offer.providerName,
                    "winningProviderPhone" to offer.providerPhone,
                    "agreedPrice" to offer.price,
                    "verificationOtp" to otpCode,
                    "updatedAt" to System.currentTimeMillis()
                )
            )
        }.addOnSuccessListener {
            onResult(true, otpCode)
        }.addOnFailureListener { e ->
            onResult(false, e.localizedMessage)
        }
    }

    /**
     * Verify OTP upon completion of work
     */
    fun verifyAndCompleteRequest(
        requestId: String,
        otpInput: String,
        expectedOtp: String,
        onResult: (Boolean, String) -> Unit
    ) {
        if (otpInput.trim().uppercase() != expectedOtp.trim().uppercase()) {
            onResult(false, "رمز الضامن السري غير صحيح. يرجى التحقق من الزبون.")
            return
        }

        requestsCollection.document(requestId).update(
            mapOf(
                "status" to "COMPLETED",
                "updatedAt" to System.currentTimeMillis()
            )
        ).addOnSuccessListener {
            onResult(true, "تم توثيق إنجاز الطلب وإغلاقه بنجاح!")
        }.addOnFailureListener { e ->
            onResult(false, e.localizedMessage ?: "حدث خطأ في الحفظ")
        }
    }

    /**
     * Cancel urgent request
     */
    fun cancelRequest(
        requestId: String,
        reason: String,
        onResult: (Boolean) -> Unit
    ) {
        requestsCollection.document(requestId).update(
            mapOf(
                "status" to "CANCELLED",
                "cancellationReason" to reason,
                "updatedAt" to System.currentTimeMillis()
            )
        ).addOnSuccessListener {
            onResult(true)
        }.addOnFailureListener {
            onResult(false)
        }
    }

    /**
     * Flow of open/active urgent requests by city & category
     */
    fun getUrgentRequestsFlow(cityId: String = "", category: String = ""): Flow<List<UrgentRequestEntity>> = callbackFlow {
        var query: Query = requestsCollection

        if (cityId.isNotBlank()) {
            query = query.whereEqualTo("cityId", cityId)
        }

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(UrgentRequestEntity::class.java)
                }.filter { req ->
                    req.status in listOf("OPEN", "HAS_OFFERS") &&
                            (category.isBlank() || req.category.contains(category, ignoreCase = true))
                }.sortedByDescending { it.createdAt }

                trySend(list)
            }
        }

        awaitClose { listener.remove() }
    }

    /**
     * Flow of user's own urgent requests
     */
    fun getUserRequestsFlow(customerId: String): Flow<List<UrgentRequestEntity>> = callbackFlow {
        if (customerId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = requestsCollection
            .whereEqualTo("customerId", customerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(UrgentRequestEntity::class.java)
                    }.sortedByDescending { it.createdAt }

                    trySend(list)
                }
            }

        awaitClose { listener.remove() }
    }

    /**
     * Flow of incoming offers for a request
     */
    fun getOffersForRequestFlow(requestId: String): Flow<List<OfferEntity>> = callbackFlow {
        if (requestId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = offersCollection
            .whereEqualTo("requestId", requestId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(OfferEntity::class.java)
                    }.sortedBy { it.price }

                    trySend(list)
                }
            }

        awaitClose { listener.remove() }
    }
}
