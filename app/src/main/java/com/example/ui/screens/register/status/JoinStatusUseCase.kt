package com.example.ui.screens.register.status

import com.example.data.*

/**
 * 🎯 الحالات المحددة لطلب الانضمام أو الدخول للوحة التحكم
 */
sealed class JoinStatus {
    object NoRequest : JoinStatus()
    data class ActiveStore(val store: StoreEntity, val businessType: String) : JoinStatus()
    data class ActiveProperty(val property: PropertyEntity) : JoinStatus()
    data class ApprovedTechnician(val provider: ProviderEntity, val categoryName: String) : JoinStatus()
    data class ActiveJobPoster(val job: JobEntity) : JoinStatus()
    data class ActiveClient(val userMap: Map<String, Any>) : JoinStatus()
    data class Rejected(val reason: String) : JoinStatus()
    data class PendingStore(val store: StoreEntity) : JoinStatus()
    data class PendingProperty(val property: PropertyEntity) : JoinStatus()
    data class PendingTechnician(val provider: PendingProviderEntity) : JoinStatus()
    data class PendingGeneric(val phone: String) : JoinStatus()
}

/**
 * 💼 حالات واجهة مستخدم متابعة حالة الطلب
 */
sealed class JoinStatusUiState {
    object Loading : JoinStatusUiState()
    data class Ready(val status: JoinStatus) : JoinStatusUiState()
    data class Error(val message: String) : JoinStatusUiState()
}

/**
 * 🔍 UseCase لتحديد وتصنيف حالة طلب الانضمام للمستخدم
 */
class JoinStatusUseCase {

    fun determineStatus(
        joinPhone: String,
        pendingProviders: List<PendingProviderEntity>,
        providers: List<ProviderEntity>,
        stores: List<StoreEntity>,
        properties: List<PropertyEntity>,
        categories: List<CategoryEntity>,
        notifications: List<NotificationEntity>,
        jobs: List<JobEntity> = emptyList(),
        registeredUsersList: List<Map<String, Any>> = emptyList()
    ): JoinStatus {
        val cleanPhone = joinPhone.trim().replace(" ", "").replace("+", "")
        if (cleanPhone.isEmpty()) {
            return JoinStatus.NoRequest
        }

        // 1. Check Active Store / Restaurant / Medical
        val matchingStore = stores.find {
            (it.ownerId.trim().replace(" ", "").replace("+", "") == cleanPhone ||
                    it.phone.trim().replace(" ", "").replace("+", "") == cleanPhone) && !it.isDeleted
        }
        if (matchingStore != null && matchingStore.isActive) {
            val isRest = matchingStore.sectionId.contains("restaurant") || matchingStore.name.contains("مطعم")
            val isMed = matchingStore.sectionId.contains("medical") || matchingStore.name.contains("عيادة")
            val businessType = if (isRest) "restaurants" else if (isMed) "medical" else "stores"
            return JoinStatus.ActiveStore(matchingStore, businessType)
        }

        // 2. Check Active Property
        val matchingProperty = properties.find {
            (it.ownerId.trim().replace(" ", "").replace("+", "") == cleanPhone ||
                    it.phone.trim().replace(" ", "").replace("+", "") == cleanPhone) && !it.isDeleted
        }
        if (matchingProperty != null && matchingProperty.isActive) {
            return JoinStatus.ActiveProperty(matchingProperty)
        }

        // 3. Check Approved Provider / Technician
        val matchingApproved = providers.find { 
            it.phone.trim().replace(" ", "").replace("+", "").replace("-", "") == cleanPhone 
        }
        if (matchingApproved != null) {
            val catName = categories.find { it.id == matchingApproved.categoryId }?.name ?: "صيانة فنية"
            return JoinStatus.ApprovedTechnician(matchingApproved, catName)
        }

        // 4. Check Active Job Poster
        val matchingJob = jobs.find {
            it.phone.trim().replace(" ", "").replace("+", "") == cleanPhone && it.isActive
        }
        if (matchingJob != null) {
            return JoinStatus.ActiveJobPoster(matchingJob)
        }

        // 5. Check Active Client
        val matchingClient = registeredUsersList.find {
            val p = (it["phone"] as? String)?.trim()?.replace(" ", "")?.replace("+", "") ?: ""
            p == cleanPhone && (it["isApproved"] == true || it["status"] == "APPROVED")
        }
        if (matchingClient != null) {
            return JoinStatus.ActiveClient(matchingClient)
        }

        // 6. Check Rejection Notifications or Pending Provider Rejection Status
        val matchingPending = pendingProviders.find { 
            it.phone.trim().replace(" ", "").replace("+", "").replace("-", "") == cleanPhone 
        }
        if (matchingPending != null && (matchingPending.status == "REJECTED" || matchingPending.reason.isNotBlank())) {
            return JoinStatus.Rejected(matchingPending.reason.ifBlank { "تم رفض طلب الانضمام من قبل الإدارة لعدم استيفاء الشروط." })
        }

        val rejectionNotif = notifications.find {
            val cleanTarget = it.targetValue.trim().replace(" ", "").replace("+", "").replace("-", "")
            cleanTarget == cleanPhone && (it.title.contains("رفض") || it.message.contains("رفض"))
        }
        if (rejectionNotif != null) {
            return JoinStatus.Rejected(rejectionNotif.message)
        }

        // 7. Check Pending entities
        if (matchingStore != null && !matchingStore.isActive) {
            return JoinStatus.PendingStore(matchingStore)
        }
        if (matchingProperty != null && !matchingProperty.isActive) {
            return JoinStatus.PendingProperty(matchingProperty)
        }

        if (matchingPending != null) {
            return JoinStatus.PendingTechnician(matchingPending)
        }

        return JoinStatus.PendingGeneric(joinPhone)
    }
}
