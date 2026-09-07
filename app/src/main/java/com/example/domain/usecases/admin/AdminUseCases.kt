package com.example.domain.usecases.admin

import com.example.data.*
import com.example.data.models.*
import com.example.utils.AppResult
import com.example.utils.AppError
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

// Repository classes for AdminUseCases
class AdminRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {
    suspend fun approveRequest(request: PendingProviderEntity): AppResult<Unit> {
        return try {
            db.collection("pending_providers").document(request.id).update("status", "APPROVED").await()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.UnknownError(e.message ?: "خطأ", e))
        }
    }

    suspend fun rejectRequest(request: PendingProviderEntity, reason: String): AppResult<Unit> {
        return try {
            db.collection("pending_providers").document(request.id).update(mapOf("status" to "REJECTED", "rejectionReason" to reason)).await()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.UnknownError(e.message ?: "خطأ", e))
        }
    }

    suspend fun approveTechnician(providerId: String): AppResult<Unit> {
        return try {
            db.collection("providers").document(providerId).update("isVerified", true, "status", "APPROVED").await()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.UnknownError(e.message ?: "خطأ", e))
        }
    }

    suspend fun loadPendingProviders(): AppResult<List<PendingProviderEntity>> = AppResult.Success(emptyList())
    suspend fun loadPendingTechnicians(): AppResult<List<PendingProviderEntity>> = AppResult.Success(emptyList())
    suspend fun approveUser(userId: String): AppResult<Unit> = AppResult.Success(Unit)
    suspend fun toggleBlockUser(userId: String, isBlocked: Boolean): AppResult<Unit> = AppResult.Success(Unit)
    suspend fun deleteUser(userId: String): AppResult<Unit> = AppResult.Success(Unit)
    suspend fun getBlockedEntities(): AppResult<List<BlockedEntity>> = AppResult.Success(emptyList())
    suspend fun unbanEntity(entityType: String, entityId: String): AppResult<Unit> = AppResult.Success(Unit)
    suspend fun getDeletedEntities(): AppResult<List<DeletedEntity>> = AppResult.Success(emptyList())
    suspend fun restoreEntity(entityType: String, entityId: String): AppResult<Unit> = AppResult.Success(Unit)
    suspend fun hardDeleteEntity(entityType: String, entityId: String): AppResult<Unit> = AppResult.Success(Unit)
    suspend fun saveCoupon(coupon: CouponEntity): AppResult<Unit> = AppResult.Success(Unit)
    suspend fun deleteCoupon(couponId: String): AppResult<Unit> = AppResult.Success(Unit)
    suspend fun saveOffer(offer: Offer): AppResult<Unit> = AppResult.Success(Unit)
    suspend fun deleteOffer(offerId: String): AppResult<Unit> = AppResult.Success(Unit)
    suspend fun toggleOfferStatus(offerId: String, isActive: Boolean): AppResult<Unit> = AppResult.Success(Unit)
    suspend fun submitReport(report: ReportEntity): AppResult<Unit> = AppResult.Success(Unit)
}

class ProviderRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {
    suspend fun addProvider(provider: ProviderEntity): AppResult<Unit> = AppResult.Success(Unit)
    suspend fun updateProvider(provider: ProviderEntity): AppResult<Unit> = AppResult.Success(Unit)
    suspend fun removeProvider(providerId: String): AppResult<Unit> = AppResult.Success(Unit)
    suspend fun restoreProvider(providerId: String): AppResult<Unit> = AppResult.Success(Unit)
    suspend fun toggleBlock(providerId: String): AppResult<Unit> = AppResult.Success(Unit)
    suspend fun toggleVIP(providerId: String, isVip: Boolean): AppResult<Unit> = AppResult.Success(Unit)
    suspend fun toggleVerification(providerId: String, isVerified: Boolean): AppResult<Unit> = AppResult.Success(Unit)
    suspend fun toggleRecommendation(providerId: String, isRecommended: Boolean): AppResult<Unit> = AppResult.Success(Unit)
}

class StoreRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {
    suspend fun saveStore(store: StoreEntity): AppResult<Unit> = AppResult.Success(Unit)
    suspend fun deleteStore(storeId: String): AppResult<Unit> = AppResult.Success(Unit)
    suspend fun restoreStore(storeId: String): AppResult<Unit> = AppResult.Success(Unit)
    suspend fun deletePermanently(storeId: String): AppResult<Unit> = AppResult.Success(Unit)
    suspend fun toggleBlocked(storeId: String, isBlocked: Boolean): AppResult<Unit> = AppResult.Success(Unit)
    suspend fun toggleActive(storeId: String, isActive: Boolean): AppResult<Unit> = AppResult.Success(Unit)
    suspend fun toggleVIP(storeId: String, isVip: Boolean): AppResult<Unit> = AppResult.Success(Unit)
}

class PropertyRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {
    suspend fun saveProperty(property: PropertyEntity): AppResult<Unit> = AppResult.Success(Unit)
    suspend fun deleteProperty(propertyId: String): AppResult<Unit> = AppResult.Success(Unit)
    suspend fun restoreProperty(propertyId: String): AppResult<Unit> = AppResult.Success(Unit)
    suspend fun deletePermanently(propertyId: String): AppResult<Unit> = AppResult.Success(Unit)
    suspend fun toggleBlocked(propertyId: String, isBlocked: Boolean): AppResult<Unit> = AppResult.Success(Unit)
    suspend fun toggleActive(propertyId: String, isActive: Boolean): AppResult<Unit> = AppResult.Success(Unit)
}

class JobRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {
    suspend fun saveJob(job: JobEntity): AppResult<Unit> = AppResult.Success(Unit)
    suspend fun deleteJob(jobId: String): AppResult<Unit> = AppResult.Success(Unit)
    suspend fun restoreJob(jobId: String): AppResult<Unit> = AppResult.Success(Unit)
    suspend fun setApproved(jobId: String, isApproved: Boolean): AppResult<Unit> = AppResult.Success(Unit)
}

/**
 * ⚙️ AdminUseCases
 * UseCases موحدة لإدارة المنصة والمشرفين
 */
class AdminUseCases(
    private val adminRepository: AdminRepository = AdminRepository(),
    private val providerRepository: ProviderRepository = ProviderRepository(),
    private val storeRepository: StoreRepository = StoreRepository(),
    private val propertyRepository: PropertyRepository = PropertyRepository(),
    private val jobRepository: JobRepository = JobRepository()
) {

    // ===== إدارة الطلبات المعلقة =====

    suspend fun loadPendingProviders(): AppResult<List<PendingProviderEntity>> {
        return adminRepository.loadPendingProviders()
    }

    suspend fun approvePendingRequest(request: PendingProviderEntity): AppResult<Unit> {
        return adminRepository.approveRequest(request)
    }

    suspend fun rejectPendingRequest(request: PendingProviderEntity, reason: String): AppResult<Unit> {
        return adminRepository.rejectRequest(request, reason)
    }

    suspend fun approveTechnician(providerId: String): AppResult<Unit> {
        return adminRepository.approveTechnician(providerId)
    }

    suspend fun loadPendingTechnicians(): AppResult<List<PendingProviderEntity>> {
        return adminRepository.loadPendingTechnicians()
    }

    // ===== إدارة المزودين =====

    suspend fun addProvider(provider: ProviderEntity): AppResult<Unit> {
        return providerRepository.addProvider(provider)
    }

    suspend fun updateProvider(provider: ProviderEntity): AppResult<Unit> {
        return providerRepository.updateProvider(provider)
    }

    suspend fun removeProvider(providerId: String): AppResult<Unit> {
        return providerRepository.removeProvider(providerId)
    }

    suspend fun restoreProvider(providerId: String): AppResult<Unit> {
        return providerRepository.restoreProvider(providerId)
    }

    suspend fun toggleProviderBlock(providerId: String): AppResult<Unit> {
        return providerRepository.toggleBlock(providerId)
    }

    suspend fun toggleProviderVIP(providerId: String, isVip: Boolean): AppResult<Unit> {
        return providerRepository.toggleVIP(providerId, isVip)
    }

    suspend fun toggleProviderVerification(providerId: String, isVerified: Boolean): AppResult<Unit> {
        return providerRepository.toggleVerification(providerId, isVerified)
    }

    suspend fun toggleProviderRecommendation(providerId: String, isRecommended: Boolean): AppResult<Unit> {
        return providerRepository.toggleRecommendation(providerId, isRecommended)
    }

    // ===== إدارة المتاجر =====

    suspend fun saveStore(store: StoreEntity): AppResult<Unit> {
        return storeRepository.saveStore(store)
    }

    suspend fun deleteStore(storeId: String): AppResult<Unit> {
        return storeRepository.deleteStore(storeId)
    }

    suspend fun restoreStore(storeId: String): AppResult<Unit> {
        return storeRepository.restoreStore(storeId)
    }

    suspend fun deleteStorePermanently(storeId: String): AppResult<Unit> {
        return storeRepository.deletePermanently(storeId)
    }

    suspend fun toggleStoreBlocked(storeId: String, isBlocked: Boolean): AppResult<Unit> {
        return storeRepository.toggleBlocked(storeId, isBlocked)
    }

    suspend fun toggleStoreActive(storeId: String, isActive: Boolean): AppResult<Unit> {
        return storeRepository.toggleActive(storeId, isActive)
    }

    suspend fun toggleStoreVIP(storeId: String, isVip: Boolean): AppResult<Unit> {
        return storeRepository.toggleVIP(storeId, isVip)
    }

    // ===== إدارة العقارات =====

    suspend fun saveProperty(property: PropertyEntity): AppResult<Unit> {
        return propertyRepository.saveProperty(property)
    }

    suspend fun deleteProperty(propertyId: String): AppResult<Unit> {
        return propertyRepository.deleteProperty(propertyId)
    }

    suspend fun restoreProperty(propertyId: String): AppResult<Unit> {
        return propertyRepository.restoreProperty(propertyId)
    }

    suspend fun deletePropertyPermanently(propertyId: String): AppResult<Unit> {
        return propertyRepository.deletePermanently(propertyId)
    }

    suspend fun togglePropertyBlocked(propertyId: String, isBlocked: Boolean): AppResult<Unit> {
        return propertyRepository.toggleBlocked(propertyId, isBlocked)
    }

    suspend fun togglePropertyActive(propertyId: String, isActive: Boolean): AppResult<Unit> {
        return propertyRepository.toggleActive(propertyId, isActive)
    }

    // ===== إدارة الوظائف =====

    suspend fun saveJob(job: JobEntity): AppResult<Unit> {
        return jobRepository.saveJob(job)
    }

    suspend fun deleteJob(jobId: String): AppResult<Unit> {
        return jobRepository.deleteJob(jobId)
    }

    suspend fun restoreJob(jobId: String): AppResult<Unit> {
        return jobRepository.restoreJob(jobId)
    }

    suspend fun setJobApproved(jobId: String, isApproved: Boolean): AppResult<Unit> {
        return jobRepository.setApproved(jobId, isApproved)
    }

    // ===== إدارة المستخدمين =====

    suspend fun approveRegisteredUser(userId: String): AppResult<Unit> {
        return adminRepository.approveUser(userId)
    }

    suspend fun toggleBlockRegisteredUser(userId: String, isBlocked: Boolean): AppResult<Unit> {
        return adminRepository.toggleBlockUser(userId, isBlocked)
    }

    suspend fun deleteRegisteredUser(userId: String): AppResult<Unit> {
        return adminRepository.deleteUser(userId)
    }

    // ===== إدارة المحظورين والمحذوفين =====

    suspend fun getBlockedEntities(): AppResult<List<BlockedEntity>> {
        return adminRepository.getBlockedEntities()
    }

    suspend fun unbanEntity(entityType: String, entityId: String): AppResult<Unit> {
        return adminRepository.unbanEntity(entityType, entityId)
    }

    suspend fun getDeletedEntities(): AppResult<List<DeletedEntity>> {
        return adminRepository.getDeletedEntities()
    }

    suspend fun restoreEntity(entityType: String, entityId: String): AppResult<Unit> {
        return adminRepository.restoreEntity(entityType, entityId)
    }

    suspend fun hardDeleteEntity(entityType: String, entityId: String): AppResult<Unit> {
        return adminRepository.hardDeleteEntity(entityType, entityId)
    }

    // ===== إدارة المحتوى =====

    suspend fun saveCoupon(coupon: CouponEntity): AppResult<Unit> {
        return adminRepository.saveCoupon(coupon)
    }

    suspend fun deleteCoupon(couponId: String): AppResult<Unit> {
        return adminRepository.deleteCoupon(couponId)
    }

    suspend fun saveOffer(offer: Offer): AppResult<Unit> {
        return adminRepository.saveOffer(offer)
    }

    suspend fun deleteOffer(offerId: String): AppResult<Unit> {
        return adminRepository.deleteOffer(offerId)
    }

    suspend fun toggleOfferStatus(offerId: String, isActive: Boolean): AppResult<Unit> {
        return adminRepository.toggleOfferStatus(offerId, isActive)
    }

    suspend fun submitReport(report: ReportEntity): AppResult<Unit> {
        return adminRepository.submitReport(report)
    }
}

// ===== DATA CLASSES =====

data class BlockedEntity(
    val id: String = "",
    val type: String = "",
    val name: String = "",
    val phone: String = "",
    val blockReason: String = ""
)

data class DeletedEntity(
    val id: String = "",
    val type: String = "",
    val name: String = "",
    val phone: String = "",
    val deletedAt: Long = 0L
)
