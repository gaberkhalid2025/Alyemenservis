package com.example.data.repositories

import android.content.Context
import android.util.Log
import com.example.data.NotificationEntity
import com.example.data.ProviderEntity
import com.example.data.StoreEntity
import com.example.data.models.JoinRequestEntity
import com.example.domain.entities.JoinStatusEntity
import com.example.domain.entities.RegistrationEntity
import com.example.util.NotificationDeduplicator
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * 📝 RegistrationRepository
 * مسؤول عن: طلبات الانضمام (join_requests / pending_providers)، الموافقة والرفض، وإدارة حالات التوثيق
 */
class RegistrationRepository(
    private val context: Context? = null,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val listeners = mutableListOf<ListenerRegistration>()
    private val joinRequestsCollection = firestore.collection("join_requests")
    private val pendingProvidersCollection = firestore.collection("pending_providers")
    private val providersCollection = firestore.collection("providers")
    private val storesCollection = firestore.collection("stores")
    private val notificationsCollection = firestore.collection("notifications")

    companion object {
        private const val TAG = "RegistrationRepository"
    }

    fun clearListeners() {
        try {
            listeners.forEach { it.remove() }
            listeners.clear()
            Log.d(TAG, "All RegistrationRepository listeners cleared safely")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing listeners", e)
        }
    }

    /**
     * إرسال طلب انضمام جديد
     */
    suspend fun submitJoinRequest(request: JoinRequestEntity): Result<String> = withContext(Dispatchers.IO) {
        try {
            val docId = if (request.id.isNotBlank()) request.id else joinRequestsCollection.document().id
            val finalRequest = request.copy(id = docId, createdAt = System.currentTimeMillis())

            joinRequestsCollection.document(docId).set(finalRequest, SetOptions.merge()).await()

            // Also mirror to pending_providers if type is PROVIDER or TECHNICIAN
            if (request.type.equals("PROVIDER", ignoreCase = true) || request.type.equals("TECHNICIAN", ignoreCase = true)) {
                pendingProvidersCollection.document(docId).set(finalRequest, SetOptions.merge()).await()
            }

            // Trigger admin notification if deduplication allows
            sendAdminJoinNotification(docId, finalRequest.fullName.ifBlank { finalRequest.businessName }, finalRequest.phone, finalRequest.type)

            Result.success(docId)
        } catch (e: Exception) {
            Log.e(TAG, "Error submitting join request", e)
            Result.failure(e)
        }
    }

    /**
     * الموافقة على طلب انضمام وتحويله لكيان نشط
     */
    suspend fun approveJoinRequest(requestId: String, adminId: String = "admin"): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val doc = joinRequestsCollection.document(requestId).get().await()
            if (!doc.exists()) {
                return@withContext Result.failure(NoSuchElementException("طلب الانضمام غير موجود"))
            }

            val request = doc.toObject(JoinRequestEntity::class.java)
                ?: return@withContext Result.failure(IllegalStateException("فشل في تحويل بيانات الطلب"))

            val now = System.currentTimeMillis()

            // 1. Update JoinRequest Status
            joinRequestsCollection.document(requestId).update(
                mapOf(
                    "status" to "APPROVED",
                    "approvedAt" to now,
                    "approvedBy" to adminId,
                    "updatedAt" to now
                )
            ).await()

            // 2. Remove or update in pending_providers
            pendingProvidersCollection.document(requestId).delete().await()

            val displayName = request.fullName.ifBlank { request.businessName }

            // 3. Move to target collection
            when (request.type.uppercase()) {
                "PROVIDER", "TECHNICIAN" -> {
                    val provider = ProviderEntity(
                        id = requestId,
                        name = displayName,
                        phone = request.phone,
                        categoryId = request.categoryId,
                        cityId = request.city,
                        area = request.neighborhood.ifBlank { request.area },
                        localNeighborhood = request.neighborhood.ifBlank { request.area },
                        subscriptionStatus = "APPROVED",
                        isAvailable = true,
                        isVerified = true
                    )
                    providersCollection.document(requestId).set(provider, SetOptions.merge()).await()
                }
                "STORE", "RESTAURANT", "MEDICAL" -> {
                    val store = StoreEntity(
                        id = requestId,
                        sectionId = when (request.type.uppercase()) {
                            "RESTAURANT" -> "restaurants"
                            "MEDICAL" -> "medical"
                            else -> "stores"
                        },
                        name = request.businessName.ifBlank { displayName },
                        ownerName = request.ownerName.ifBlank { displayName },
                        phone = request.phone,
                        categoryId = request.categoryId,
                        cityId = request.city,
                        localNeighborhood = request.neighborhood.ifBlank { request.area },
                        isActive = true,
                        createdAt = now
                    )
                    storesCollection.document(requestId).set(store, SetOptions.merge()).await()
                }
            }

            // 4. Send Approval Notification to user
            sendJoinStatusNotification(request.phone, displayName, "APPROVED")

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error approving join request $requestId", e)
            Result.failure(e)
        }
    }

    /**
     * رفض طلب انضمام مع كتابة السبب
     */
    suspend fun rejectJoinRequest(requestId: String, reason: String, adminId: String = "admin"): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val doc = joinRequestsCollection.document(requestId).get().await()
            if (!doc.exists()) {
                return@withContext Result.failure(NoSuchElementException("طلب الانضمام غير موجود"))
            }

            val request = doc.toObject(JoinRequestEntity::class.java)
                ?: return@withContext Result.failure(IllegalStateException("فشل قراءة بيانات الطلب"))

            val now = System.currentTimeMillis()

            joinRequestsCollection.document(requestId).update(
                mapOf(
                    "status" to "REJECTED",
                    "rejectionReason" to reason,
                    "rejectedAt" to now,
                    "rejectedBy" to adminId,
                    "updatedAt" to now
                )
            ).await()

            // Remove from pending_providers
            pendingProvidersCollection.document(requestId).delete().await()

            val displayName = request.fullName.ifBlank { request.businessName }

            // Send Rejection Notification to user
            sendJoinStatusNotification(request.phone, displayName, "REJECTED", reason)

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error rejecting join request $requestId", e)
            Result.failure(e)
        }
    }

    /**
     * مراقبة كافة طلبات الانضمام للإدارة
     */
    fun observeAllJoinRequests(): Flow<List<JoinRequestEntity>> = callbackFlow {
        val listener = joinRequestsCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error observing join requests", error)
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(JoinRequestEntity::class.java)?.copy(id = doc.id)
            } ?: emptyList()
            trySend(list)
        }
        listeners.add(listener)

        awaitClose {
            listener.remove()
            listeners.remove(listener)
        }
    }.flowOn(Dispatchers.IO)

    /**
     * مراقبة حالة طلب الانضمام لرقم هاتف محدد
     */
    fun observeJoinStatusByPhone(phone: String): Flow<JoinRequestEntity?> = callbackFlow {
        val cleanPhone = phone.trim().replace(" ", "").replace("+", "")
        if (cleanPhone.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val listener = joinRequestsCollection
            .whereEqualTo("phone", cleanPhone)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error observing join status for phone $phone", error)
                    return@addSnapshotListener
                }
                val latest = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(JoinRequestEntity::class.java)?.copy(id = doc.id)
                }?.maxByOrNull { it.createdAt }

                trySend(latest)
            }
        listeners.add(listener)

        awaitClose {
            listener.remove()
            listeners.remove(listener)
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun sendAdminJoinNotification(requestId: String, applicantName: String, phone: String, type: String) {
        try {
            val notifId = UUID.randomUUID().toString()
            val notif = NotificationEntity(
                id = notifId,
                title = "طلب انضمام جديد",
                message = "قام $applicantName ($phone) بتقديم طلب انضمام كـ $type",
                targetType = "ADMIN",
                targetAudience = "ADMIN_ONLY",
                senderId = "SYSTEM",
                notificationType = "JOIN_REQUEST",
                relatedRequestId = requestId,
                createdAt = System.currentTimeMillis()
            )
            notificationsCollection.document(notifId).set(notif, SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send admin join notification", e)
        }
    }

    private suspend fun sendJoinStatusNotification(phone: String, name: String, status: String, reason: String = "") {
        try {
            val notifId = UUID.randomUUID().toString()
            val title = if (status == "APPROVED") "تم قبول طلب الانضمام 🎉" else "تحديث بشأن طلب الانضمام"
            val message = if (status == "APPROVED") {
                "مرحباً $name، تم قبول وتوثيق حسابك بنجاح. يمكنك الآن استقبال الطلبات وتقديم الخدمات."
            } else {
                "مرحباً $name، نعتذر عن قبول الطلب في الوقت الحالي. السبب: ${reason.ifBlank { "لم يستوفِ الشروط" }}"
            }

            val notif = NotificationEntity(
                id = notifId,
                title = title,
                message = message,
                customerPhone = phone,
                targetType = "USER",
                targetValue = phone,
                targetAudience = "SPECIFIC_USERS",
                senderId = "ADMIN",
                notificationType = if (status == "APPROVED") "JOIN_APPROVED" else "JOIN_REJECTED",
                createdAt = System.currentTimeMillis()
            )
            notificationsCollection.document(notifId).set(notif, SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send join status notification", e)
        }
    }
}
