package com.example.data.repositories

import android.content.Context
import android.util.Log
import com.example.data.LocalAppCacheManager
import com.example.data.models.JoinRequestEntity
import com.example.data.NotificationEntity
import com.example.domain.entities.JoinStatusEntity
import com.example.domain.entities.RegistrationEntity
import com.example.security.BookingSecurityHelper
import com.example.utils.NotificationDeduplicator
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * 📦 RegistrationRepositoryImpl
 * Production implementation of IRegistrationRepository with strict Firestore schema,
 * phone deduplication, admin notifications, and real-time status tracking.
 */
class RegistrationRepositoryImpl(
    private val context: Context
) : IRegistrationRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val cacheManager = LocalAppCacheManager(context)
    private val deduplicator = NotificationDeduplicator(context)

    private suspend fun checkExistingPendingRequest(phone: String): Boolean {
        val cleanPhone = phone.trim().replace(" ", "").replace("+", "")
        if (cleanPhone.isBlank()) return false
        val snap = firestore.collection("join_requests")
            .whereEqualTo("phone", cleanPhone)
            .whereEqualTo("status", "PENDING")
            .get()
            .await()
        return !snap.isEmpty
    }

    private suspend fun sendAdminJoinNotification(requestId: String, applicantName: String, phone: String, type: String) {
        try {
            if (deduplicator.isJoinNotificationDuplicate(requestId, "JOIN_REQUEST")) {
                return
            }
            val notifId = UUID.randomUUID().toString()
            val typeTitle = when (type) {
                "PROVIDER" -> "مهني / فني"
                "STORE" -> "متجر / محل تجاري"
                "RESTAURANT" -> "مطعم / كافيه"
                "MEDICAL" -> "مركز طبي / دكتور"
                "PROPERTY" -> "عقار / مكتب عقاري"
                "JOB" -> "إعلان توظيف / صاحب عمل"
                "CLIENT" -> "عميل جديد"
                else -> type
            }
            val notification = NotificationEntity(
                id = notifId,
                title = "📥 طلب انضمام جديد ($typeTitle)",
                message = "قدم $applicantName ($phone) طلب انضمام جديد. يرجى مراجعة بيانات الطلب والموافقة عليه.",
                targetType = "ADMIN",
                targetValue = "",
                notificationType = "JOIN_REQUEST",
                relatedRequestId = requestId,
                isRead = false,
                fcmSent = false,
                timestamp = System.currentTimeMillis(),
                createdAt = System.currentTimeMillis()
            )

            firestore.collection("notifications").document(notifId).set(notification).await()
            deduplicator.markJoinNotificationSent(requestId, "JOIN_REQUEST")
        } catch (e: Exception) {
            Log.e("RegistrationRepository", "Failed to send admin notification", e)
        }
    }

    override suspend fun registerClient(client: RegistrationEntity.Client): Result<String> {
        return try {
            val cleanPhone = client.phone.trim().replace(" ", "").replace("+", "")
            if (checkExistingPendingRequest(cleanPhone)) {
                return Result.failure(Exception("يوجد طلب تسجيل قيد المراجعة بالفعل لرقم الهاتف هذا"))
            }

            val id = UUID.randomUUID().toString()
            val hashedPassword = com.example.utils.PasswordHasher.hash(client.passwordHash)
            val request = JoinRequestEntity(
                id = id,
                type = "CLIENT",
                status = "PENDING",
                fullName = client.fullName.trim(),
                phone = cleanPhone,
                passwordHash = hashedPassword,
                city = client.city.trim(),
                profileImage = client.profileImageUrl,
                approvalStatus = "PENDING",
                submittedAt = System.currentTimeMillis(),
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            // Store request in join_requests
            firestore.collection("join_requests").document(id).set(request).await()

            // Store client data in "users" collection
            val userMap = mapOf(
                "id" to id,
                "name" to client.fullName.trim(),
                "phone" to cleanPhone,
                "city" to client.city.trim(),
                "role" to "CLIENT",
                "password" to hashedPassword,
                "isBlocked" to false,
                "createdAt" to System.currentTimeMillis()
            )
            firestore.collection("users").document(cleanPhone).set(userMap).await()

            sendAdminJoinNotification(id, request.fullName, cleanPhone, "CLIENT")
            Result.success(id)
        } catch (e: Exception) {
            Log.e("RegistrationRepository", "Error registering client", e)
            Result.failure(e)
        }
    }

    override suspend fun registerProvider(provider: RegistrationEntity.Provider): Result<String> {
        return try {
            val cleanPhone = provider.phone.trim().replace(" ", "").replace("+", "")
            if (checkExistingPendingRequest(cleanPhone)) {
                return Result.failure(Exception("يوجد طلب انضمام مهني قيد المراجعة بالفعل لرقم الهاتف هذا"))
            }

            val id = UUID.randomUUID().toString()
            val request = JoinRequestEntity(
                id = id,
                type = "PROVIDER",
                status = "PENDING",
                fullName = provider.fullName.trim(),
                phone = cleanPhone,
                passwordHash = com.example.utils.PasswordHasher.hash(provider.passwordHash),
                city = provider.city.trim(),
                categoryId = provider.professionCategory.trim(),
                categoryName = provider.professionCategory.trim(),
                idCardImage = provider.identityDocumentUrl,
                workImages = provider.workImages,
                businessName = provider.fullName.trim(),
                approvalStatus = "PENDING",
                submittedAt = System.currentTimeMillis(),
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            firestore.collection("join_requests").document(id).set(request).await()
            sendAdminJoinNotification(id, request.fullName, cleanPhone, "PROVIDER")
            Result.success(id)
        } catch (e: Exception) {
            Log.e("RegistrationRepository", "Error registering provider", e)
            Result.failure(e)
        }
    }

    override suspend fun registerStore(store: RegistrationEntity.Store): Result<String> {
        return try {
            val cleanPhone = store.phone.trim().replace(" ", "").replace("+", "")
            if (checkExistingPendingRequest(cleanPhone)) {
                return Result.failure(Exception("يوجد طلب انضمام متجر قيد المراجعة بالفعل لرقم الهاتف هذا"))
            }

            val id = UUID.randomUUID().toString()
            val request = JoinRequestEntity(
                id = id,
                type = "STORE",
                status = "PENDING",
                businessName = store.storeName.trim(),
                ownerName = store.ownerName.trim(),
                fullName = store.ownerName.trim(),
                phone = cleanPhone,
                passwordHash = com.example.utils.PasswordHasher.hash(store.passwordHash),
                categoryId = store.storeCategory.trim(),
                categoryName = store.storeCategory.trim(),
                city = store.city.trim(),
                area = store.addressDetails.trim(),
                logoImage = store.logoUrl,
                workImages = store.storeImages,
                approvalStatus = "PENDING",
                submittedAt = System.currentTimeMillis(),
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            firestore.collection("join_requests").document(id).set(request).await()
            sendAdminJoinNotification(id, request.businessName, cleanPhone, "STORE")
            Result.success(id)
        } catch (e: Exception) {
            Log.e("RegistrationRepository", "Error registering store", e)
            Result.failure(e)
        }
    }

    override suspend fun registerRestaurant(restaurant: RegistrationEntity.Restaurant): Result<String> {
        return try {
            val cleanPhone = restaurant.phone.trim().replace(" ", "").replace("+", "")
            if (checkExistingPendingRequest(cleanPhone)) {
                return Result.failure(Exception("يوجد طلب انضمام مطعم قيد المراجعة بالفعل لرقم الهاتف هذا"))
            }

            val id = UUID.randomUUID().toString()
            val request = JoinRequestEntity(
                id = id,
                type = "RESTAURANT",
                status = "PENDING",
                businessName = restaurant.restaurantName.trim(),
                ownerName = restaurant.ownerName.trim(),
                fullName = restaurant.ownerName.trim(),
                phone = cleanPhone,
                passwordHash = com.example.utils.PasswordHasher.hash(restaurant.passwordHash),
                categoryId = restaurant.cuisineType.trim(),
                categoryName = restaurant.cuisineType.trim(),
                city = restaurant.city.trim(),
                area = restaurant.addressDetails.trim(),
                logoImage = restaurant.logoUrl,
                workImages = restaurant.menuImageUrls,
                approvalStatus = "PENDING",
                submittedAt = System.currentTimeMillis(),
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            firestore.collection("join_requests").document(id).set(request).await()
            sendAdminJoinNotification(id, request.businessName, cleanPhone, "RESTAURANT")
            Result.success(id)
        } catch (e: Exception) {
            Log.e("RegistrationRepository", "Error registering restaurant", e)
            Result.failure(e)
        }
    }

    override suspend fun registerMedicalCenter(medical: RegistrationEntity.MedicalCenter): Result<String> {
        return try {
            val cleanPhone = medical.phone.trim().replace(" ", "").replace("+", "")
            if (checkExistingPendingRequest(cleanPhone)) {
                return Result.failure(Exception("يوجد طلب انضمام مركز طبي قيد المراجعة بالفعل لرقم الهاتف هذا"))
            }

            val id = UUID.randomUUID().toString()
            val request = JoinRequestEntity(
                id = id,
                type = "MEDICAL",
                status = "PENDING",
                businessName = medical.centerName.trim(),
                ownerName = medical.doctorName.trim(),
                fullName = medical.doctorName.trim(),
                phone = cleanPhone,
                passwordHash = com.example.utils.PasswordHasher.hash(medical.passwordHash),
                categoryId = medical.specialtyCategory.trim(),
                categoryName = medical.specialtyCategory.trim(),
                city = medical.city.trim(),
                area = medical.addressDetails.trim(),
                logoImage = medical.logoUrl,
                approvalStatus = "PENDING",
                submittedAt = System.currentTimeMillis(),
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            firestore.collection("join_requests").document(id).set(request).await()
            sendAdminJoinNotification(id, request.businessName, cleanPhone, "MEDICAL")
            Result.success(id)
        } catch (e: Exception) {
            Log.e("RegistrationRepository", "Error registering medical center", e)
            Result.failure(e)
        }
    }

    override suspend fun registerProperty(property: RegistrationEntity.Property): Result<String> {
        return try {
            val cleanPhone = property.phone.trim().replace(" ", "").replace("+", "")
            if (checkExistingPendingRequest(cleanPhone)) {
                return Result.failure(Exception("يوجد طلب إضافة عقار قيد المراجعة بالفعل لرقم الهاتف هذا"))
            }

            val id = UUID.randomUUID().toString()
            val request = JoinRequestEntity(
                id = id,
                type = "PROPERTY",
                status = "PENDING",
                propertyTitle = property.title.trim(),
                propertyType = property.propertyType.trim(),
                categoryId = property.category.trim(),
                categoryName = property.category.trim(),
                ownerName = property.ownerName.trim(),
                fullName = property.ownerName.trim(),
                phone = cleanPhone,
                passwordHash = com.example.utils.PasswordHasher.hash(property.passwordHash),
                city = property.city.trim(),
                area = property.areaDetails.trim(),
                price = property.priceYer,
                workImages = property.imageUrls,
                approvalStatus = "PENDING",
                submittedAt = System.currentTimeMillis(),
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            firestore.collection("join_requests").document(id).set(request).await()
            sendAdminJoinNotification(id, request.propertyTitle, cleanPhone, "PROPERTY")
            Result.success(id)
        } catch (e: Exception) {
            Log.e("RegistrationRepository", "Error registering property", e)
            Result.failure(e)
        }
    }

    override suspend fun registerJob(job: RegistrationEntity.Job): Result<String> {
        return try {
            val cleanPhone = job.contactPhone.trim().replace(" ", "").replace("+", "")
            if (checkExistingPendingRequest(cleanPhone)) {
                return Result.failure(Exception("يوجد إعلان توظيف قيد المراجعة بالفعل لرقم الهاتف هذا"))
            }

            val id = UUID.randomUUID().toString()
            val request = JoinRequestEntity(
                id = id,
                type = "JOB",
                status = "PENDING",
                jobTitle = job.jobTitle.trim(),
                companyName = job.companyName.trim(),
                businessName = job.companyName.trim(),
                categoryId = job.category.trim(),
                categoryName = job.category.trim(),
                phone = cleanPhone,
                passwordHash = com.example.utils.PasswordHasher.hash(job.passwordHash),
                city = job.city.trim(),
                approvalStatus = "PENDING",
                submittedAt = System.currentTimeMillis(),
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            firestore.collection("join_requests").document(id).set(request).await()
            sendAdminJoinNotification(id, request.jobTitle, cleanPhone, "JOB")
            Result.success(id)
        } catch (e: Exception) {
            Log.e("RegistrationRepository", "Error registering job", e)
            Result.failure(e)
        }
    }

    override fun getJoinStatusFlow(phoneNumber: String): Flow<JoinStatusEntity?> = callbackFlow {
        val cleanPhone = phoneNumber.trim().replace(" ", "").replace("+", "")
        if (cleanPhone.isBlank()) {
            trySend(null)
            return@callbackFlow
        }

        val listener: ListenerRegistration = firestore.collection("join_requests")
            .whereEqualTo("phone", cleanPhone)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(null)
                    return@addSnapshotListener
                }
                if (snapshot != null && !snapshot.isEmpty) {
                    // Get latest request by submittedAt/createdAt
                    val doc = snapshot.documents.maxByOrNull { it.getLong("submittedAt") ?: it.getLong("createdAt") ?: 0L }
                    if (doc != null) {
                        val name = doc.getString("fullName") 
                            ?: doc.getString("businessName") 
                            ?: doc.getString("propertyTitle") 
                            ?: doc.getString("jobTitle") 
                            ?: doc.getString("name") 
                            ?: ""
                        val entity = JoinStatusEntity(
                            requestId = doc.id,
                            applicantName = name,
                            registrationType = doc.getString("type") ?: doc.getString("role") ?: "PROVIDER",
                            status = doc.getString("status") ?: doc.getString("approvalStatus") ?: "PENDING",
                            rejectionReason = doc.getString("rejectionReason") ?: "",
                            createdAt = doc.getLong("submittedAt") ?: doc.getLong("createdAt") ?: 0L,
                            updatedAt = doc.getLong("updatedAt") ?: 0L
                        )
                        trySend(entity)
                    } else {
                        trySend(null)
                    }
                } else {
                    trySend(null)
                }
            }
        awaitClose { listener.remove() }
    }
}
