package com.example.data.repositories

import android.content.Context
import android.util.Log
import com.example.data.models.InstantRequestEntity
import com.example.data.models.RequestOfferEntity
import com.example.security.BookingSecurityHelper
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * 📦 UrgentRepositoryImpl
 * Production implementation of IUrgentRepository managing 30-min instant requests, real-time offers flow,
 * expiration checks, and PIN secure cancellation.
 */
class UrgentRepositoryImpl(
    private val context: Context
) : IUrgentRepository {

    private val firestore = FirebaseFirestore.getInstance()

    override fun getUrgentRequestsFlow(userId: String, isProvider: Boolean): Flow<List<InstantRequestEntity>> = callbackFlow {
        val collectionRef = firestore.collection("urgent_requests")

        val listener: ListenerRegistration = collectionRef.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) {
                trySend(emptyList())
                return@addSnapshotListener
            }

            val now = System.currentTimeMillis()
            val list = snapshot.documents.mapNotNull { doc ->
                val expiresAt = doc.getLong("expiresAt") ?: (doc.getLong("createdAt") ?: now) + 30 * 60 * 1000L
                val currentStatus = doc.getString("status") ?: "WAITING_FOR_OFFERS"

                val finalStatus = if (now > expiresAt && currentStatus == "WAITING_FOR_OFFERS") "EXPIRED" else currentStatus

                InstantRequestEntity(
                    id = doc.id,
                    userId = doc.getString("userId") ?: "",
                    requestCode = doc.getString("requestCode") ?: doc.id.take(6).uppercase(),
                    serviceTitle = doc.getString("serviceTitle") ?: doc.getString("title") ?: "",
                    categoryName = doc.getString("category") ?: doc.getString("categoryName") ?: "",
                    userCity = doc.getString("userCity") ?: doc.getString("city") ?: "",
                    userNeighborhood = doc.getString("userNeighborhood") ?: doc.getString("address") ?: "",
                    description = doc.getString("detailsDescription") ?: doc.getString("description") ?: "",
                    acceptedPrice = doc.getDouble("maxBudgetYer") ?: doc.getDouble("acceptedPrice") ?: 0.0,
                    status = finalStatus,
                    offersCount = (doc.getLong("offersCount") ?: 0L).toInt(),
                    createdAt = doc.getLong("createdAt") ?: now,
                    expiresAt = expiresAt
                )
            }.filter { item ->
                if (isProvider) {
                    item.status != "CANCELLED" && item.status != "EXPIRED"
                } else {
                    userId.isBlank() || item.userId == userId
                }
            }
            trySend(list)
        }

        awaitClose { listener.remove() }
    }

    override fun getUrgentRequestDetailsFlow(requestId: String): Flow<InstantRequestEntity?> = callbackFlow {
        if (requestId.isBlank()) {
            trySend(null)
            return@callbackFlow
        }

        val listener: ListenerRegistration = firestore.collection("urgent_requests")
            .document(requestId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) {
                    trySend(null)
                    return@addSnapshotListener
                }

                val now = System.currentTimeMillis()
                val expiresAt = snapshot.getLong("expiresAt") ?: (snapshot.getLong("createdAt") ?: now) + 30 * 60 * 1000L
                val currentStatus = snapshot.getString("status") ?: "WAITING_FOR_OFFERS"
                val finalStatus = if (now > expiresAt && currentStatus == "WAITING_FOR_OFFERS") "EXPIRED" else currentStatus

                val entity = InstantRequestEntity(
                    id = snapshot.id,
                    userId = snapshot.getString("userId") ?: "",
                    requestCode = snapshot.getString("requestCode") ?: snapshot.id.take(6).uppercase(),
                    serviceTitle = snapshot.getString("serviceTitle") ?: snapshot.getString("title") ?: "",
                    categoryName = snapshot.getString("category") ?: snapshot.getString("categoryName") ?: "",
                    userCity = snapshot.getString("userCity") ?: snapshot.getString("city") ?: "",
                    userNeighborhood = snapshot.getString("userNeighborhood") ?: snapshot.getString("address") ?: "",
                    description = snapshot.getString("detailsDescription") ?: snapshot.getString("description") ?: "",
                    acceptedPrice = snapshot.getDouble("maxBudgetYer") ?: snapshot.getDouble("acceptedPrice") ?: 0.0,
                    status = finalStatus,
                    offersCount = (snapshot.getLong("offersCount") ?: 0L).toInt(),
                    createdAt = snapshot.getLong("createdAt") ?: now,
                    expiresAt = expiresAt
                )
                trySend(entity)
            }

        awaitClose { listener.remove() }
    }

    override fun getOffersForUrgentRequestFlow(requestId: String): Flow<List<RequestOfferEntity>> = callbackFlow {
        if (requestId.isBlank()) {
            trySend(emptyList())
            return@callbackFlow
        }

        val listener: ListenerRegistration = firestore.collection("urgent_requests")
            .document(requestId)
            .collection("offers")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val list = snapshot.documents.mapNotNull { doc ->
                    RequestOfferEntity(
                        id = doc.id,
                        requestId = requestId,
                        requestCode = doc.getString("requestCode") ?: "",
                        technicianId = doc.getString("technicianId") ?: "",
                        technicianName = doc.getString("technicianName") ?: "فني محترف",
                        technicianPhone = doc.getString("technicianPhone") ?: "",
                        technicianAvatar = doc.getString("technicianAvatar") ?: "",
                        technicianRating = (doc.getDouble("technicianRating") ?: 5.0).toFloat(),
                        price = doc.getDouble("price") ?: 0.0,
                        estimatedArrivalTime = doc.getString("estimatedArrivalTime") ?: "خلال 15-30 دقيقة",
                        estimatedDuration = doc.getString("estimatedDuration") ?: "1 ساعة",
                        notes = doc.getString("notes") ?: "",
                        status = doc.getString("status") ?: "PENDING",
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                    )
                }
                trySend(list)
            }

        awaitClose { listener.remove() }
    }

    override suspend fun createUrgentRequest(request: InstantRequestEntity): Result<String> {
        return try {
            val id = if (request.id.isBlank()) UUID.randomUUID().toString() else request.id
            val code = "URG-${(1000..9999).random()}"
            val now = System.currentTimeMillis()
            val expiresAt = now + 30 * 60 * 1000L

            val map = mapOf(
                "id" to id,
                "userId" to request.userId,
                "requestCode" to code,
                "serviceTitle" to request.serviceTitle,
                "category" to request.categoryName,
                "userCity" to request.userCity,
                "userNeighborhood" to request.userNeighborhood,
                "detailsDescription" to request.description,
                "maxBudgetYer" to request.acceptedPrice,
                "status" to "WAITING_FOR_OFFERS",
                "offersCount" to 0,
                "createdAt" to now,
                "expiresAt" to expiresAt
            )

            firestore.collection("urgent_requests").document(id).set(map).await()
            Result.success(id)
        } catch (e: Exception) {
            Log.e("UrgentRepositoryImpl", "Error creating urgent request", e)
            Result.failure(e)
        }
    }

    override suspend fun submitUrgentOffer(offer: RequestOfferEntity): Result<String> {
        return try {
            val offerId = if (offer.id.isBlank()) UUID.randomUUID().toString() else offer.id
            val offerMap = mapOf(
                "id" to offerId,
                "requestId" to offer.requestId,
                "technicianId" to offer.technicianId,
                "technicianName" to offer.technicianName,
                "technicianPhone" to offer.technicianPhone,
                "technicianAvatar" to offer.technicianAvatar,
                "technicianRating" to offer.technicianRating,
                "price" to offer.price,
                "estimatedArrivalTime" to offer.estimatedArrivalTime,
                "estimatedDuration" to offer.estimatedDuration,
                "notes" to offer.notes,
                "status" to "PENDING",
                "createdAt" to System.currentTimeMillis()
            )

            val batch = firestore.batch()
            val offerRef = firestore.collection("urgent_requests")
                .document(offer.requestId)
                .collection("offers")
                .document(offerId)

            val requestRef = firestore.collection("urgent_requests").document(offer.requestId)

            batch.set(offerRef, offerMap)
            batch.update(
                requestRef, mapOf(
                    "status" to "REVIEWING_OFFERS",
                    "offersCount" to com.google.firebase.firestore.FieldValue.increment(1),
                    "updatedAt" to System.currentTimeMillis()
                )
            )

            batch.commit().await()
            Result.success(offerId)
        } catch (e: Exception) {
            Log.e("UrgentRepositoryImpl", "Error submitting offer", e)
            Result.failure(e)
        }
    }

    override suspend fun acceptUrgentOffer(requestId: String, offerId: String, providerPhone: String): Result<Unit> {
        return try {
            val batch = firestore.batch()
            val requestRef = firestore.collection("urgent_requests").document(requestId)
            val offerRef = requestRef.collection("offers").document(offerId)

            batch.update(requestRef, mapOf("status" to "ACCEPTED", "acceptedOfferId" to offerId, "updatedAt" to System.currentTimeMillis()))
            batch.update(offerRef, mapOf("status" to "ACCEPTED"))

            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("UrgentRepositoryImpl", "Error accepting offer", e)
            Result.failure(e)
        }
    }

    override suspend fun cancelUrgentRequest(requestId: String, userPin: String): Result<Unit> {
        if (BookingSecurityHelper.isBookingLocked(context, requestId)) {
            val remainingSecs = BookingSecurityHelper.getRemainingLockoutSeconds(context, requestId)
            return Result.failure(IllegalStateException("الحساب مقفل مؤقتاً لأسباب أمنية بسبب أدخال PIN خاطئ 3 مرات. انتظر $remainingSecs ثانية."))
        }

        if (userPin.isBlank() || userPin.length < 4) {
            val remaining = BookingSecurityHelper.recordFailedAttempt(context, requestId)
            return Result.failure(IllegalArgumentException("رمز PIN غير صحيح. محاولات متبقية: $remaining"))
        }

        return try {
            BookingSecurityHelper.resetAttempts(context, requestId)
            firestore.collection("urgent_requests").document(requestId)
                .update(mapOf("status" to "CANCELLED", "updatedAt" to System.currentTimeMillis()))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
