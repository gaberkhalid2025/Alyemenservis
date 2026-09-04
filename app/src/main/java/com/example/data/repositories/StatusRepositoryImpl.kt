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

    override fun getSystemMetrics(): Flow<SystemStatusMetrics> = callbackFlow {
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

    override fun getSystemMetricsFlow(): Flow<SystemStatusMetrics> = getSystemMetrics()

    override fun getPendingJoinRequests(): Flow<List<PendingProviderEntity>> = callbackFlow {
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

    override fun getPendingJoinRequestsFlow(): Flow<List<PendingProviderEntity>> = getPendingJoinRequests()

    override fun getSystemBookings(): Flow<List<BookingEntity>> = callbackFlow {
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

    override fun getSystemBookingsFlow(): Flow<List<BookingEntity>> = getSystemBookings()

    override fun getInstantRequests(): Flow<List<InstantRequestEntity>> = callbackFlow {
        val listener = firestore.collection("instant_requests")
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

    override fun getInstantRequestsFlow(): Flow<List<InstantRequestEntity>> = getInstantRequests()

    override fun getNotifications(): Flow<List<NotificationEntity>> = callbackFlow {
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

    override fun getNotificationsFlow(): Flow<List<NotificationEntity>> = getNotifications()

    override suspend fun approveJoinRequest(request: PendingProviderEntity): Result<Unit> {
        return try {
            val requestDoc = firestore.collection("join_requests").document(request.id).get().await()
            val type = requestDoc.getString("type") ?: "PROVIDER"
            val cleanPhone = request.phone.trim().replace(" ", "").replace("+", "")
            val now = System.currentTimeMillis()

            val batch = firestore.batch()
            val requestRef = firestore.collection("join_requests").document(request.id)
            batch.update(requestRef, mapOf(
                "status" to "APPROVED",
                "approvalStatus" to "APPROVED",
                "isActive" to true,
                "approvedAt" to now,
                "approvedBy" to "ADMIN",
                "updatedAt" to now
            ))

            // Create Entity in appropriate collection
            when (type.uppercase()) {
                "PROVIDER" -> {
                    val provRef = firestore.collection("providers").document(request.id)
                    val provData = mapOf(
                        "id" to request.id,
                        "name" to request.name,
                        "phone" to cleanPhone,
                        "categoryId" to request.categoryId,
                        "area" to request.area,
                        "isAvailable" to true,
                        "subscriptionStatus" to "APPROVED",
                        "rating" to 5.0f,
                        "isBlocked" to false,
                        "createdAt" to now
                    )
                    batch.set(provRef, provData)
                }
                "STORE", "RESTAURANT", "MEDICAL" -> {
                    val storeRef = firestore.collection("stores").document(request.id)
                    val storeData = mapOf(
                        "id" to request.id,
                        "name" to (requestDoc.getString("businessName") ?: request.name),
                        "ownerName" to request.name,
                        "phone" to cleanPhone,
                        "category" to request.categoryId,
                        "city" to request.area,
                        "isActive" to true,
                        "isApproved" to true,
                        "type" to type,
                        "createdAt" to now
                    )
                    batch.set(storeRef, storeData)
                }
                "PROPERTY" -> {
                    val propRef = firestore.collection("properties").document(request.id)
                    val propData = mapOf(
                        "id" to request.id,
                        "title" to (requestDoc.getString("propertyTitle") ?: request.name),
                        "ownerName" to request.name,
                        "phone" to cleanPhone,
                        "category" to request.categoryId,
                        "city" to request.area,
                        "isActive" to true,
                        "isApproved" to true,
                        "createdAt" to now
                    )
                    batch.set(propRef, propData)
                }
                "JOB" -> {
                    val jobRef = firestore.collection("jobs").document(request.id)
                    val jobData = mapOf(
                        "id" to request.id,
                        "jobTitle" to (requestDoc.getString("jobTitle") ?: request.name),
                        "companyName" to (requestDoc.getString("companyName") ?: request.name),
                        "phone" to cleanPhone,
                        "city" to request.area,
                        "isActive" to true,
                        "isApproved" to true,
                        "createdAt" to now
                    )
                    batch.set(jobRef, jobData)
                }
                else -> {
                    val userRef = firestore.collection("users").document(request.id)
                    batch.set(userRef, mapOf(
                        "id" to request.id,
                        "name" to request.name,
                        "phone" to cleanPhone,
                        "role" to "CLIENT",
                        "status" to "APPROVED",
                        "createdAt" to now
                    ))
                }
            }

            // Create notification for user
            val notifId = java.util.UUID.randomUUID().toString()
            val notifRef = firestore.collection("notifications").document(notifId)
            val notif = NotificationEntity(
                id = notifId,
                title = "🎉 تم قبول وتوثيق طلب الانضمام!",
                message = "تهانينا! تمت مراجعة والموافقة على طلب انضمامك ($type). حسابك أصبح نشطاً ومتاحاً الآن.",
                targetType = "USER",
                targetValue = cleanPhone,
                notificationType = "JOIN_APPROVED",
                relatedRequestId = request.id,
                isRead = false,
                fcmSent = false,
                timestamp = now,
                createdAt = now
            )
            batch.set(notifRef, notif)

            // Delete from pending_providers so that it is removed from waiting list
            val pendingRef = firestore.collection("pending_providers").document(request.id)
            batch.delete(pendingRef)

            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("StatusRepositoryImpl", "Error approving join request", e)
            Result.failure(e)
        }
    }

    override suspend fun rejectJoinRequest(request: PendingProviderEntity, reason: String): Result<Unit> {
        return try {
            val cleanPhone = request.phone.trim().replace(" ", "").replace("+", "")
            val now = System.currentTimeMillis()
            val finalReason = reason.ifBlank { "لم تستوفِ المستندات أو الشروط المطلوبة" }

            val batch = firestore.batch()
            val requestRef = firestore.collection("join_requests").document(request.id)

            batch.update(requestRef, mapOf(
                "status" to "REJECTED",
                "approvalStatus" to "REJECTED",
                "isActive" to false,
                "rejectionReason" to finalReason,
                "rejectedAt" to now,
                "rejectedBy" to "ADMIN",
                "updatedAt" to now
            ))

            // Update status in pending_providers as well
            val pendingRef = firestore.collection("pending_providers").document(request.id)
            batch.update(pendingRef, mapOf(
                "status" to "REJECTED",
                "reason" to finalReason
            ))

            // Create notification for user
            val notifId = java.util.UUID.randomUUID().toString()
            val notifRef = firestore.collection("notifications").document(notifId)
            val notif = NotificationEntity(
                id = notifId,
                title = "❌ حالة طلب الانضمام",
                message = "نأسف لإبلاغك بأنه تم رفض طلب الانضمام للسبب التالي: $finalReason",
                targetType = "USER",
                targetValue = cleanPhone,
                notificationType = "JOIN_REJECTED",
                relatedRequestId = request.id,
                isRead = false,
                fcmSent = false,
                timestamp = now,
                createdAt = now
            )
            batch.set(notifRef, notif)

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
