package com.example.data.repositories

import android.content.Context
import android.util.Log
import com.example.data.LocalAppCacheManager
import com.example.domain.entities.JoinStatusEntity
import com.example.domain.entities.RegistrationEntity
import com.example.security.BookingSecurityHelper
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * 📦 RegistrationRepositoryImpl
 * Production implementation of IRegistrationRepository with offline-first support.
 */
class RegistrationRepositoryImpl(
    private val context: Context
) : IRegistrationRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val cacheManager = LocalAppCacheManager(context)

    override suspend fun registerClient(client: RegistrationEntity.Client): Result<String> {
        return try {
            val id = UUID.randomUUID().toString()
            val docData = mapOf(
                "id" to id,
                "fullName" to client.fullName,
                "name" to client.fullName,
                "phone" to client.phone,
                "city" to client.city,
                "passwordPin" to BookingSecurityHelper.hashPin(client.passwordHash),
                "profileImageUrl" to client.profileImageUrl,
                "role" to "CLIENT",
                "createdAt" to System.currentTimeMillis()
            )

            firestore.collection("users").document(id).set(docData).await()
            Result.success(id)
        } catch (e: Exception) {
            Log.e("RegistrationRepository", "Error registering client", e)
            Result.failure(e)
        }
    }

    override suspend fun registerProvider(provider: RegistrationEntity.Provider): Result<String> {
        return try {
            val id = UUID.randomUUID().toString()
            val docData = mapOf(
                "id" to id,
                "fullName" to provider.fullName,
                "name" to provider.fullName,
                "phone" to provider.phone,
                "professionCategory" to provider.professionCategory,
                "category" to provider.professionCategory,
                "city" to provider.city,
                "experienceYears" to provider.experienceYears,
                "bio" to provider.bio,
                "identityDocumentUrl" to provider.identityDocumentUrl,
                "licenseNumber" to provider.licenseNumber,
                "workImages" to provider.workImages,
                "passwordPin" to BookingSecurityHelper.hashPin(provider.passwordHash),
                "role" to "PROVIDER",
                "status" to "PENDING",
                "createdAt" to System.currentTimeMillis()
            )

            firestore.collection("join_requests").document(id).set(docData).await()
            firestore.collection("users").document(id).set(docData).await()
            Result.success(id)
        } catch (e: Exception) {
            Log.e("RegistrationRepository", "Error registering provider", e)
            Result.failure(e)
        }
    }

    override suspend fun registerStore(store: RegistrationEntity.Store): Result<String> {
        return try {
            val id = UUID.randomUUID().toString()
            val docData = mapOf(
                "id" to id,
                "storeName" to store.storeName,
                "name" to store.storeName,
                "ownerName" to store.ownerName,
                "phone" to store.phone,
                "category" to store.storeCategory,
                "city" to store.city,
                "addressDetails" to store.addressDetails,
                "commercialRegisterNumber" to store.commercialRegisterNumber,
                "logoUrl" to store.logoUrl,
                "storeImages" to store.storeImages,
                "passwordPin" to BookingSecurityHelper.hashPin(store.passwordHash),
                "type" to "STORE",
                "status" to "PENDING",
                "createdAt" to System.currentTimeMillis()
            )

            firestore.collection("stores").document(id).set(docData).await()
            firestore.collection("join_requests").document(id).set(docData).await()
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun registerRestaurant(restaurant: RegistrationEntity.Restaurant): Result<String> {
        return try {
            val id = UUID.randomUUID().toString()
            val docData = mapOf(
                "id" to id,
                "restaurantName" to restaurant.restaurantName,
                "name" to restaurant.restaurantName,
                "ownerName" to restaurant.ownerName,
                "phone" to restaurant.phone,
                "cuisineType" to restaurant.cuisineType,
                "city" to restaurant.city,
                "addressDetails" to restaurant.addressDetails,
                "logoUrl" to restaurant.logoUrl,
                "menuImageUrls" to restaurant.menuImageUrls,
                "passwordPin" to BookingSecurityHelper.hashPin(restaurant.passwordHash),
                "type" to "RESTAURANT",
                "status" to "PENDING",
                "createdAt" to System.currentTimeMillis()
            )

            firestore.collection("restaurants").document(id).set(docData).await()
            firestore.collection("join_requests").document(id).set(docData).await()
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun registerMedicalCenter(medical: RegistrationEntity.MedicalCenter): Result<String> {
        return try {
            val id = UUID.randomUUID().toString()
            val docData = mapOf(
                "id" to id,
                "centerName" to medical.centerName,
                "name" to medical.centerName,
                "specialtyCategory" to medical.specialtyCategory,
                "doctorName" to medical.doctorName,
                "phone" to medical.phone,
                "city" to medical.city,
                "addressDetails" to medical.addressDetails,
                "licenseNumber" to medical.licenseNumber,
                "logoUrl" to medical.logoUrl,
                "passwordPin" to BookingSecurityHelper.hashPin(medical.passwordHash),
                "type" to "MEDICAL",
                "status" to "PENDING",
                "createdAt" to System.currentTimeMillis()
            )

            firestore.collection("medical_centers").document(id).set(docData).await()
            firestore.collection("join_requests").document(id).set(docData).await()
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun registerProperty(property: RegistrationEntity.Property): Result<String> {
        return try {
            val id = UUID.randomUUID().toString()
            val docData = mapOf(
                "id" to id,
                "title" to property.title,
                "propertyType" to property.propertyType,
                "category" to property.category,
                "ownerName" to property.ownerName,
                "phone" to property.phone,
                "city" to property.city,
                "areaDetails" to property.areaDetails,
                "priceYer" to property.priceYer,
                "description" to property.description,
                "imageUrls" to property.imageUrls,
                "passwordPin" to BookingSecurityHelper.hashPin(property.passwordHash),
                "type" to "PROPERTY",
                "status" to "PENDING",
                "createdAt" to System.currentTimeMillis()
            )

            firestore.collection("properties").document(id).set(docData).await()
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun registerJob(job: RegistrationEntity.Job): Result<String> {
        return try {
            val id = UUID.randomUUID().toString()
            val docData = mapOf(
                "id" to id,
                "jobTitle" to job.jobTitle,
                "companyName" to job.companyName,
                "category" to job.category,
                "contactPhone" to job.contactPhone,
                "contactEmail" to job.contactEmail,
                "city" to job.city,
                "requirements" to job.requirements,
                "salaryRange" to job.salaryRange,
                "passwordPin" to BookingSecurityHelper.hashPin(job.passwordHash),
                "type" to "JOB",
                "status" to "PENDING",
                "createdAt" to System.currentTimeMillis()
            )

            firestore.collection("job_listings").document(id).set(docData).await()
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getJoinStatusFlow(phoneNumber: String): Flow<JoinStatusEntity?> = callbackFlow {
        if (phoneNumber.isBlank()) {
            trySend(null)
            return@callbackFlow
        }

        val listener: ListenerRegistration = firestore.collection("join_requests")
            .whereEqualTo("phone", phoneNumber.trim())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(null)
                    return@addSnapshotListener
                }
                if (snapshot != null && !snapshot.isEmpty) {
                    val doc = snapshot.documents.first()
                    val entity = JoinStatusEntity(
                        requestId = doc.id,
                        applicantName = doc.getString("fullName") ?: doc.getString("name") ?: "",
                        registrationType = doc.getString("type") ?: doc.getString("role") ?: "PROVIDER",
                        status = doc.getString("status") ?: "PENDING",
                        rejectionReason = doc.getString("rejectionReason") ?: "",
                        createdAt = doc.getLong("createdAt") ?: 0L,
                        updatedAt = doc.getLong("updatedAt") ?: 0L
                    )
                    trySend(entity)
                } else {
                    trySend(null)
                }
            }
        awaitClose { listener.remove() }
    }
}
