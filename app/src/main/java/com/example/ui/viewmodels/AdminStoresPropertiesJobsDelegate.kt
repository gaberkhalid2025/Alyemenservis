package com.example.ui.viewmodels

import android.content.Context
import com.example.data.JobApplicationEntity
import com.example.data.JobEntity
import com.example.data.PropertyEntity
import com.example.data.StoreEntity

/**
 * 🏪 AdminStoresPropertiesJobsDelegate
 * مفوّض إدارة المتاجر والمراكز، العقارات، الوظائف وطلبات التوظيف،
 * بالإضافة إلى عمليات الحظر/الاستعادة/الحذف العابرة للكيانات وتوجيه الحجوزات.
 */
internal class AdminStoresPropertiesJobsDelegate(
    private val vm: AdminViewModel
) {
    // ==========================================
    // 🏪 المتاجر والمطاعم والمراكز الطبية (Stores)
    // ==========================================

    fun saveStore(store: StoreEntity) = saveStoreOp(vm, store)
    fun deleteStore(storeId: String) = deleteStoreOp(vm, storeId)
    fun restoreStore(storeId: String) = restoreStoreOp(vm, storeId)
    fun deleteStorePermanently(storeId: String) = deleteStorePermanentlyOp(vm, storeId)
    fun setStoreActive(storeId: String, isActive: Boolean) = setStoreActiveOp(vm, storeId, isActive)
    fun setStorePinned(storeId: String, isPinned: Boolean) = setStorePinnedOp(vm, storeId, isPinned)
    fun setStoreVip(storeId: String, isVip: Boolean) = setStoreVipOp(vm, storeId, isVip)
    fun setStoreVerified(storeId: String, isVerified: Boolean) = setStoreVerifiedOp(vm, storeId, isVerified)
    fun setStoreRecommended(storeId: String, isRecommended: Boolean) = setStoreRecommendedOp(vm, storeId, isRecommended)
    fun setStoreBlocked(storeId: String, isBlocked: Boolean, reason: String = "") =
        setStoreBlockedOp(vm, storeId, isBlocked, reason)
    fun setStoreChatDisabled(storeId: String, isDisabled: Boolean) = setStoreChatDisabledOp(vm, storeId, isDisabled)
    fun setStoreNotificationsDisabled(storeId: String, isDisabled: Boolean) =
        setStoreNotificationsDisabledOp(vm, storeId, isDisabled)
    fun setStorePaymentEnabled(storeId: String, isEnabled: Boolean) = setStorePaymentEnabledOp(vm, storeId, isEnabled)
    fun toggleStoreBlocked(storeId: String, isBlocked: Boolean) = toggleStoreBlockedOp(vm, storeId, isBlocked)
    fun toggleStoreActive(storeId: String) = toggleStoreActiveOp(vm, storeId)
    fun toggleStorePinned(storeId: String) = toggleStorePinnedOp(vm, storeId)
    fun toggleStoreChatDisabled(storeId: String) = toggleStoreChatDisabledOp(vm, storeId)
    fun approveStorePdf(storeId: String, approve: Boolean) = approveStorePdfOp(vm, storeId, approve)

    // ==========================================
    // 🏡 العقارات والأراضي (Properties)
    // ==========================================

    fun saveProperty(property: PropertyEntity) = savePropertyOp(vm, property)
    fun deleteProperty(propertyId: String) = deletePropertyOp(vm, propertyId)
    fun restoreProperty(propertyId: String) = restorePropertyOp(vm, propertyId)
    fun deletePropertyPermanently(propertyId: String) = deletePropertyPermanentlyOp(vm, propertyId)
    fun setPropertyActive(propertyId: String, isActive: Boolean) = setPropertyActiveOp(vm, propertyId, isActive)
    fun setPropertyPinned(propertyId: String, isPinned: Boolean) = setPropertyPinnedOp(vm, propertyId, isPinned)
    fun setPropertyVip(propertyId: String, isVip: Boolean) = setPropertyVipOp(vm, propertyId, isVip)
    fun setPropertyVerified(propertyId: String, isVerified: Boolean) = setPropertyVerifiedOp(vm, propertyId, isVerified)
    fun setPropertyRecommended(propertyId: String, isRecommended: Boolean) =
        setPropertyRecommendedOp(vm, propertyId, isRecommended)
    fun setPropertyBlocked(propertyId: String, isBlocked: Boolean, reason: String = "") =
        setPropertyBlockedOp(vm, propertyId, isBlocked, reason)
    fun setPropertyChatDisabled(propertyId: String, isDisabled: Boolean) =
        setPropertyChatDisabledOp(vm, propertyId, isDisabled)
    fun setPropertyNotificationsDisabled(propertyId: String, isDisabled: Boolean) =
        setPropertyNotificationsDisabledOp(vm, propertyId, isDisabled)
    fun setPropertyPaymentEnabled(propertyId: String, isEnabled: Boolean) =
        setPropertyPaymentEnabledOp(vm, propertyId, isEnabled)
    fun togglePropertyBlocked(propertyId: String, isBlocked: Boolean) = togglePropertyBlockedOp(vm, propertyId, isBlocked)
    fun approvePropertyPdf(propertyId: String, approve: Boolean) = approvePropertyPdfOp(vm, propertyId, approve)

    // ==========================================
    // 💼 الوظائف وطلبات التوظيف (Jobs & Applications)
    // ==========================================

    fun saveJob(job: JobEntity) = saveJobOp(vm, job)
    fun deleteJob(jobId: String) = deleteJobOp(vm, jobId)
    fun restoreJob(jobId: String) = restoreJobOp(vm, jobId)
    fun deleteJobPermanently(jobId: String) = deleteJobPermanentlyOp(vm, jobId)
    fun setJobApproved(jobId: String, isApproved: Boolean) = setJobApprovedOp(vm, jobId, isApproved)
    fun setJobBlocked(jobId: String, isBlocked: Boolean, reason: String = "") =
        setJobBlockedOp(vm, jobId, isBlocked, reason)
    fun setJobPinned(jobId: String, isPinned: Boolean) = setJobPinnedOp(vm, jobId, isPinned)
    fun setJobVip(jobId: String, isVip: Boolean) = setJobVipOp(vm, jobId, isVip)
    fun setJobChatDisabled(jobId: String, isDisabled: Boolean) = setJobChatDisabledOp(vm, jobId, isDisabled)
    fun submitJobApplication(application: JobApplicationEntity) = submitJobApplicationOp(vm, application)
    fun updateJobApplicationStatus(appId: String, status: String, reason: String = "") =
        updateJobApplicationStatusOp(vm, appId, status, reason)
    fun acceptJobApplication(appId: String) = updateJobApplicationStatus(appId, "ACCEPTED")
    fun rejectJobApplication(appId: String, reason: String) = updateJobApplicationStatus(appId, "REJECTED", reason)
    fun deleteJobApplication(appId: String) = deleteJobApplicationOp(vm, appId)
    fun sendNotificationToApplicants(title: String, message: String, jobId: String = "") =
        sendNotificationToApplicantsOp(vm, title, message, jobId)
    fun exportJobApplicantsCsv(context: Context) = exportJobApplicantsCsvOp(vm, context)

    // ==========================================
    // 🔄 عمليات عابرة للكيانات وتوجيه الحجوزات
    // ==========================================

    fun redirectBookingToEntity(
        bookingId: String,
        targetEntityId: String,
        targetEntityName: String,
        targetPhone: String
    ) = redirectBookingToEntityOp(vm, bookingId, targetEntityId, targetEntityName, targetPhone)

    fun unbanEntity(entityType: String, entityId: String) = unbanEntityOp(vm, entityType, entityId)
    fun restoreEntity(entityType: String, entityId: String) = restoreEntityOp(vm, entityType, entityId)
    fun hardDeleteEntity(entityType: String, entityId: String, reason: String = "") =
        hardDeleteEntityOp(vm, entityType, entityId, reason)
}
