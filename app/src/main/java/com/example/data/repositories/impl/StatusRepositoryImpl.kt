package com.example.data.repositories.impl

import android.content.Context
import android.util.Log
import com.example.data.BookingEntity
import com.example.data.NotificationEntity
import com.example.data.PendingProviderEntity
import com.example.data.models.InstantRequestEntity
import com.example.data.models.SystemStatusMetrics
import com.example.data.repositories.contracts.IStatusRepository
import com.example.data.utils.AppError
import com.example.data.utils.AppResult
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class StatusRepositoryImpl(
    private val context: Context,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : IStatusRepository {

    companion object {
        private const val TAG = "StatusRepositoryImpl"
    }

    override fun getSystemMetricsFlow(): Flow<SystemStatusMetrics> = callbackFlow {
        val listener = firestore.collection("admin_settings")
            .document("system_metrics")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to system metrics", error)
                    return@addSnapshotListener
                }
                val metrics = if (snapshot != null && snapshot.exists()) {
                    snapshot.toObject(SystemStatusMetrics::class.java) ?: SystemStatusMetrics()
                } else {
                    SystemStatusMetrics()
                }
                trySend(metrics)
            }
        awaitClose { listener.remove() }
    }.flowOn(Dispatchers.IO)

    override fun getPendingJoinRequestsFlow(): Flow<List<PendingProviderEntity>> = callbackFlow {
        val listener = firestore.collection("join_requests")
            .whereEqualTo("status", "PENDING")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to pending join requests", error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(PendingProviderEntity::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }.flowOn(Dispatchers.IO)

    override fun getSystemBookingsFlow(): Flow<List<BookingEntity>> = callbackFlow {
        val listener = firestore.collection("bookings")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(100)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to bookings", error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(BookingEntity::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }.flowOn(Dispatchers.IO)

    override fun getInstantRequestsFlow(): Flow<List<InstantRequestEntity>> = callbackFlow {
        val listener = firestore.collection("urgent_requests")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(100)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to urgent requests", error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(InstantRequestEntity::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }.flowOn(Dispatchers.IO)

    override fun getNotificationsFlow(): Flow<List<NotificationEntity>> = callbackFlow {
        val listener = firestore.collection("notifications")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(100)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to notifications", error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(NotificationEntity::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }.flowOn(Dispatchers.IO)

    override suspend fun refreshSystemStatus(): AppResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val providers = firestore.collection("providers").get().await().size()
            val stores = firestore.collection("stores").get().await().size()
            val properties = firestore.collection("properties").get().await().size()
            val instantReqs = firestore.collection("urgent_requests")
                .whereEqualTo("status", "WAITING_FOR_OFFERS")
                .get().await().size()
            val bookings = firestore.collection("bookings").get().await().size()
            val pendingRequests = firestore.collection("join_requests")
                .whereEqualTo("status", "PENDING")
                .get().await().size()
            val unreadNotifs = firestore.collection("notifications")
                .whereEqualTo("isRead", false)
                .get().await().size()

            val metrics = SystemStatusMetrics(
                providersCount = providers,
                storesCount = stores,
                propertiesCount = properties,
                instantRequestsCount = instantReqs,
                bookingsCount = bookings,
                pendingJoinRequestsCount = pendingRequests,
                unreadNotificationsCount = unreadNotifs,
                lastUpdatedTimestamp = System.currentTimeMillis()
            )

            firestore.collection("admin_settings")
                .document("system_metrics")
                .set(metrics)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing system status metrics", e)
            Result.failure(AppError.DatabaseError(e.localizedMessage ?: "فشل تحديث حالات وبيانات المنصة"))
        }
    }

    override suspend fun approveJoinRequest(request: PendingProviderEntity): AppResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val updates = mapOf(
                "status" to "APPROVED",
                "approvalStatus" to "APPROVED",
                "updatedAt" to System.currentTimeMillis()
            )
            firestore.collection("join_requests").document(request.id).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error approving request", e)
            Result.failure(AppError.DatabaseError(e.localizedMessage ?: "فشل قبول طلب الانضمام"))
        }
    }

    override suspend fun rejectJoinRequest(request: PendingProviderEntity, reason: String): AppResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val updates = mapOf(
                "status" to "REJECTED",
                "approvalStatus" to "REJECTED",
                "rejectionReason" to reason,
                "updatedAt" to System.currentTimeMillis()
            )
            firestore.collection("join_requests").document(request.id).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error rejecting request", e)
            Result.failure(AppError.DatabaseError(e.localizedMessage ?: "فشل رفض طلب الانضمام"))
        }
    }

    override suspend fun clearNotifications(): AppResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val snapshot = firestore.collection("notifications").get().await()
            val batch = firestore.batch()
            for (doc in snapshot.documents) {
                batch.delete(doc.reference)
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing notifications", e)
            Result.failure(AppError.DatabaseError(e.localizedMessage ?: "فشل مسح الإشعارات"))
        }
    }
}
