package com.example.data.repositories

import android.content.Context
import android.util.Log
import com.example.data.BookingEntity
import com.example.data.NotificationEntity
import com.example.data.PendingProviderEntity
import com.example.data.models.InstantRequestEntity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * 📦 StatusRepositoryImpl
 * Implements IStatusRepository for system stats, pending join requests, notifications, and system bookings.
 */
class StatusRepositoryImpl(
    private val context: Context
) : IStatusRepository {

    private val firestore = FirebaseFirestore.getInstance()

    override fun getSystemMetricsFlow(): Flow<SystemStatusMetrics> = callbackFlow {
        val listener: ListenerRegistration = firestore.collection("join_requests")
            .whereEqualTo("status", "PENDING")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(SystemStatusMetrics())
                    return@addSnapshotListener
                }

                val pendingCount = snapshot?.size() ?: 0

                firestore.collection("users").get().addOnSuccessListener { usersSnap ->
                    val providersCount = usersSnap.documents.count { it.getString("role") == "PROVIDER" }
                    val instantRequestsCount = usersSnap.documents.sumOf { (it.getLong("instantRequestsCount") ?: 0L).toInt() }

                    firestore.collection("stores").get().addOnSuccessListener { storesSnap ->
                        val storesCount = storesSnap.size()
                        firestore.collection("properties").get().addOnSuccessListener { propsSnap ->
                            val propertiesCount = propsSnap.size()
                            firestore.collection("bookings").get().addOnSuccessListener { bookingsSnap ->
                                val bookingsCount = bookingsSnap.size()

                                trySend(
                                    SystemStatusMetrics(
                                        providersCount = providersCount,
                                        storesCount = storesCount,
                                        propertiesCount = propertiesCount,
                                        instantRequestsCount = instantRequestsCount,
                                        bookingsCount = bookingsCount,
                                        pendingJoinRequestsCount = pendingCount,
                                        lastUpdatedTimestamp = System.currentTimeMillis()
                                    )
                                )
                            }
                        }
                    }
                }
            }

        awaitClose { listener.remove() }
    }

    override fun getPendingJoinRequestsFlow(): Flow<List<PendingProviderEntity>> = callbackFlow {
        val listener = firestore.collection("join_requests")
            .whereEqualTo("status", "PENDING")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val list = snapshot.documents.mapNotNull { doc ->
                    PendingProviderEntity(
                        id = doc.id,
                        name = doc.getString("fullName") ?: doc.getString("name") ?: "",
                        phone = doc.getString("phone") ?: "",
                        categoryId = doc.getString("professionCategory") ?: doc.getString("category") ?: "",
                        area = doc.getString("city") ?: "",
                        localNeighborhood = doc.getString("localNeighborhood") ?: "",
                        status = doc.getString("status") ?: "PENDING",
                        reason = doc.getString("reason") ?: ""
                    )
                }
                trySend(list)
            }

        awaitClose { listener.remove() }
    }

    override fun getSystemBookingsFlow(): Flow<List<BookingEntity>> = callbackFlow {
        val listener = firestore.collection("bookings")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val list = snapshot.documents.mapNotNull { doc ->
                    BookingEntity(
                        id = doc.id,
                        bookingCode = doc.getString("bookingCode") ?: doc.id.take(8).uppercase(),
                        serviceType = doc.getString("serviceTitle") ?: doc.getString("serviceType") ?: "خدمة عامة",
                        providerName = doc.getString("providerName") ?: "",
                        clientName = doc.getString("clientName") ?: doc.getString("customerName") ?: "",
                        status = doc.getString("status") ?: "PENDING",
                        dateString = doc.getString("scheduledDate") ?: doc.getString("dateString") ?: "",
                        totalAmount = doc.getDouble("priceYer") ?: doc.getDouble("totalAmount") ?: 0.0
                    )
                }
                trySend(list)
            }

        awaitClose { listener.remove() }
    }

    override fun getInstantRequestsFlow(): Flow<List<InstantRequestEntity>> = callbackFlow {
        val listener = firestore.collection("urgent_requests")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val list = snapshot.documents.mapNotNull { doc ->
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
                        status = doc.getString("status") ?: "WAITING_FOR_OFFERS",
                        offersCount = (doc.getLong("offersCount") ?: 0L).toInt(),
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                        expiresAt = doc.getLong("expiresAt") ?: (System.currentTimeMillis() + 30 * 60 * 1000L)
                    )
                }
                trySend(list)
            }

        awaitClose { listener.remove() }
    }

    override fun getNotificationsFlow(): Flow<List<NotificationEntity>> = callbackFlow {
        val listener = firestore.collection("notifications")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val list = snapshot.documents.mapNotNull { doc ->
                    NotificationEntity(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        message = doc.getString("message") ?: doc.getString("body") ?: "",
                        timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                        notificationType = doc.getString("notificationType") ?: "SYSTEM"
                    )
                }
                trySend(list)
            }

        awaitClose { listener.remove() }
    }

    override suspend fun approveJoinRequest(request: PendingProviderEntity): Result<Unit> {
        return try {
            val batch = firestore.batch()
            val requestRef = firestore.collection("join_requests").document(request.id)
            val userRef = firestore.collection("users").document(request.id)

            batch.update(requestRef, mapOf("status" to "APPROVED", "updatedAt" to System.currentTimeMillis()))
            batch.update(userRef, mapOf("status" to "APPROVED", "role" to "PROVIDER", "updatedAt" to System.currentTimeMillis()))

            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("StatusRepositoryImpl", "Error approving join request", e)
            Result.failure(e)
        }
    }

    override suspend fun rejectJoinRequest(request: PendingProviderEntity, reason: String): Result<Unit> {
        return try {
            val batch = firestore.batch()
            val requestRef = firestore.collection("join_requests").document(request.id)
            val userRef = firestore.collection("users").document(request.id)

            batch.update(requestRef, mapOf(
                "status" to "REJECTED",
                "rejectionReason" to reason.ifBlank { "لم تستوفِ المستندات أو الشروط المطلوبة" },
                "updatedAt" to System.currentTimeMillis()
            ))
            batch.update(userRef, mapOf("status" to "REJECTED", "updatedAt" to System.currentTimeMillis()))

            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("StatusRepositoryImpl", "Error rejecting join request", e)
            Result.failure(e)
        }
    }

    override suspend fun clearNotifications(): Result<Unit> {
        return try {
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun refreshSystemStatus(): Result<Unit> {
        return Result.success(Unit)
    }
}
