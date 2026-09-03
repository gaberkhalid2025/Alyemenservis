package com.example.data.repositories.impl

import android.content.Context
import android.util.Log
import com.example.data.models.InstantRequestEntity
import com.example.data.models.RequestOfferEntity
import com.example.data.repositories.contracts.IUrgentRequestRepository
import com.example.data.utils.AppError
import com.example.data.utils.AppResult
import com.example.security.BookingSecurityHelper
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.random.Random

class UrgentRequestRepositoryImpl(
    private val context: Context?,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : IUrgentRequestRepository {

    private val activeListeners = mutableListOf<ListenerRegistration>()

    override fun clearListeners() {
        try {
            activeListeners.forEach { it.remove() }
            activeListeners.clear()
        } catch (e: Exception) {
            Log.e("UrgentRequestRepoImpl", "Error clearing listeners", e)
        }
    }

    override suspend fun createInstantRequest(request: InstantRequestEntity): AppResult<InstantRequestEntity> = withContext(Dispatchers.IO) {
        try {
            val docId = request.id.ifBlank { firestore.collection("urgent_requests").document().id }
            val requestCode = request.requestCode.ifBlank { "URG-${Random.nextInt(100000, 999999)}" }
            val pin = request.secretPin.ifBlank { "${Random.nextInt(1000, 9999)}" }
            val cancelPass = request.cancellationPassword.ifBlank { "${Random.nextInt(1000, 9999)}" }
            val now = System.currentTimeMillis()
            val expiresAt = if (request.expiresAt > now) request.expiresAt else now + (30 * 60 * 1000L) // 30 mins

            val newEntity = request.copy(
                id = docId,
                requestCode = requestCode,
                secretPin = pin,
                cancellationPassword = cancelPass,
                status = request.status.ifBlank { "WAITING_FOR_OFFERS" },
                createdAt = if (request.createdAt > 0) request.createdAt else now,
                expiresAt = expiresAt
            )

            firestore.collection("urgent_requests").document(docId).set(newEntity).await()
            firestore.collection("instant_requests").document(docId).set(newEntity).await()

            Result.success(newEntity)
        } catch (e: Exception) {
            Result.failure(AppError.DatabaseError(e.localizedMessage ?: "فشل إنشاء الطلب العاجل"))
        }
    }

    override fun getUserInstantRequests(userId: String): Flow<List<InstantRequestEntity>> = callbackFlow {
        val listener = firestore.collection("urgent_requests")
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
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
                }
                trySend(list)
            }
        activeListeners.add(listener)
        awaitClose { listener.remove() }
    }

    override fun getAvailableInstantRequests(category: String, city: String): Flow<List<InstantRequestEntity>> = callbackFlow {
        val query = firestore.collection("urgent_requests")
            .whereEqualTo("status", "WAITING_FOR_OFFERS")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(100)

        val listener = query.addSnapshotListener { snapshot, error ->
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
        activeListeners.add(listener)
        awaitClose { listener.remove() }
    }

    override suspend fun submitOffer(offer: RequestOfferEntity): AppResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val offerId = offer.id.ifBlank { UUID.randomUUID().toString() }
            val finalOffer = offer.copy(id = offerId, createdAt = System.currentTimeMillis())

            val offerRef = firestore.collection("urgent_requests")
                .document(offer.requestId)
                .collection("offers")
                .document(offerId)

            val requestRef = firestore.collection("urgent_requests").document(offer.requestId)

            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(requestRef)
                val currentOffers = snapshot.getLong("offersCount") ?: 0L
                transaction.set(offerRef, finalOffer)
                transaction.update(requestRef, "offersCount", currentOffers + 1)
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(AppError.DatabaseError(e.localizedMessage ?: "فشل تقديم عرض السعر"))
        }
    }

    override suspend fun acceptOffer(
        requestId: String,
        offerId: String,
        providerId: String,
        providerName: String,
        providerPhone: String,
        acceptedPrice: Double
    ): AppResult<Unit> = withContext(Dispatchers.IO) {
        try {
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

            firestore.collection("urgent_requests").document(requestId).update(updates).await()
            firestore.collection("instant_requests").document(requestId).update(updates).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(AppError.DatabaseError(e.localizedMessage ?: "فشل قبول عرض السعر"))
        }
    }

    override suspend fun cancelInstantRequest(requestId: String, userPin: String): AppResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val doc = firestore.collection("urgent_requests").document(requestId).get().await()
            val request = doc.toObject(InstantRequestEntity::class.java)
                ?: return@withContext Result.failure(AppError.NotFoundError("الطلب الفوري غير موجود"))

            if (userPin.isNotBlank()) {
                val expectedPass = request.cancellationPassword.ifBlank { request.secretPin }
                if (expectedPass.isNotBlank() && !BookingSecurityHelper.verifyPassword(userPin, expectedPass)) {
                    return@withContext Result.failure(AppError.ValidationError("PIN", "رمز PIN للإلغاء غير صحيح"))
                }
            }

            val updates = mapOf(
                "status" to "CANCELLED",
                "cancelledAt" to System.currentTimeMillis(),
                "updatedAt" to System.currentTimeMillis()
            )

            firestore.collection("urgent_requests").document(requestId).update(updates).await()
            firestore.collection("instant_requests").document(requestId).update(updates).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(AppError.DatabaseError(e.localizedMessage ?: "فشل إلغاء الطلب"))
        }
    }

    override suspend fun completeInstantRequest(requestId: String): AppResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val updates = mapOf(
                "status" to "COMPLETED",
                "completedAt" to System.currentTimeMillis(),
                "updatedAt" to System.currentTimeMillis()
            )

            firestore.collection("urgent_requests").document(requestId).update(updates).await()
            firestore.collection("instant_requests").document(requestId).update(updates).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(AppError.DatabaseError(e.localizedMessage ?: "فشل إكمال الطلب"))
        }
    }
}
