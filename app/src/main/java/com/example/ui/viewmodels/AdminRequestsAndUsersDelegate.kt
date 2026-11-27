package com.example.ui.viewmodels

import androidx.lifecycle.viewModelScope
import com.example.data.PendingProviderEntity
import com.example.domain.YemenCities
import com.example.domain.usecases.ValidatePhoneUseCase
import com.example.utils.ApprovalValidator
import com.example.utils.ArabicNormalizer
import com.example.utils.CityIdResolver
import com.example.utils.LandmarkAddress
import com.example.utils.PasswordHasher
import com.example.utils.SecurityCryptoUtils
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * 📋 AdminRequestsAndUsersDelegate
 * مفوّض مراجعة وقبول ورفض طلبات الانضمام (فنيين، متاجر، مطاعم، مراكز طبية، عقارات، وظائف، عملاء)
 * وإدارة المستخدمين المسجلين وفحص تكرار الحسابات.
 */
internal class AdminRequestsAndUsersDelegate(
    private val vm: AdminViewModel
) {
    private fun decryptAttachmentSafe(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return ""
        if (trimmed.startsWith("http://") ||
            trimmed.startsWith("https://") ||
            trimmed.startsWith("data:image") ||
            trimmed.startsWith("content://") ||
            trimmed.startsWith("file://")
        ) {
            return trimmed
        }
        return if (trimmed.startsWith("gcm:") || trimmed.startsWith("gcmx:") || trimmed.startsWith("enc::")) {
            SecurityCryptoUtils.decryptCrossDevice(trimmed).ifBlank {
                SecurityCryptoUtils.decrypt(trimmed)
            }
        } else {
            trimmed
        }
    }

    private val knownCityIds: Map<String, String> by lazy {
        YemenCities.list.associate { ArabicNormalizer.normalize(it) to YemenCities.resolveCityId(it) }
    }

    private fun parseCoordinates(request: PendingProviderEntity, resolvedCityId: String?): Pair<Double, Double>? {
        val parts = request.gpsCoords.split(",")
        val parsedLat = request.latitude.takeIf { it != 0.0 } ?: parts.getOrNull(0)?.trim()?.toDoubleOrNull()
        val parsedLng = request.longitude.takeIf { it != 0.0 } ?: parts.getOrNull(1)?.trim()?.toDoubleOrNull()
        if (parsedLat != null && parsedLng != null && LandmarkAddress.isValid(parsedLat, parsedLng)) {
            return parsedLat to parsedLng
        }
        if (!resolvedCityId.isNullOrBlank()) {
            val cityName = YemenCities.getCityNameById(resolvedCityId)
            val fallback = com.example.ui.screens.map.utils.OfflineMapManager.getCityCoordinates(cityName)
            if (LandmarkAddress.isValid(fallback.latitude, fallback.longitude)) {
                return fallback.latitude to fallback.longitude
            }
        }
        return null
    }

    private fun parseWorkingHours(json: String): String {
        if (json.isBlank() || !json.contains("\"workingHours\"")) return "9:00 AM - 10:00 PM"
        return runCatching {
            Regex("\"workingHours\":\"(.*?)\"").find(json)?.groupValues?.getOrNull(1).orEmpty()
        }.getOrDefault("").ifBlank { "9:00 AM - 10:00 PM" }
    }

    fun approveRequest(request: PendingProviderEntity) {
        val finalCityId = CityIdResolver.resolve(request.area, knownCityIds)
            ?: CityIdResolver.resolve(request.localNeighborhood, knownCityIds)
            ?: request.area.trim().takeIf { it.startsWith("ye_") }
        val coords = parseCoordinates(request, finalCityId)
        val cleanPhone = ValidatePhoneUseCase.normalizePhone(request.phone)
            .ifBlank { request.phone.trim().replace(" ", "").replace("+", "") }

        val problems = ApprovalValidator.validate(
            lat = coords?.first,
            lng = coords?.second,
            cityId = finalCityId,
            name = request.name,
            phone = cleanPhone.ifBlank { request.phone }
        )
        if (problems.isNotEmpty()) {
            vm.mainViewModel.triggerNotification(
                "❌ تعذر اعتماد ${request.name.ifBlank { "الطلب" }}: ${problems.joinToString(" • ") { it.message }}"
            )
            return
        }

        val resolvedPasswordHash = request.passwordHash.ifBlank { request.password }.let { rawOrHash ->
            val trimmed = rawOrHash.trim()
            if (trimmed.isNotBlank() && !trimmed.contains(":")) PasswordHasher.hash(trimmed) else trimmed
        }

        val ctx = ApprovalContext(
            request = request,
            cleanPhone = cleanPhone,
            resolvedCityId = finalCityId!!,
            resolvedLat = coords!!.first,
            resolvedLng = coords.second,
            resolvedWorkingHours = parseWorkingHours(request.productAttachmentsJson),
            decryptedSelfie = decryptAttachmentSafe(request.selfiePhotoBase64),
            decryptedIdCard = decryptAttachmentSafe(request.idPhotoBase64),
            decryptedWorkPhotos = request.workPhotosBase64.map { decryptAttachmentSafe(it) }.filter { it.isNotBlank() },
            resolvedPasswordHash = resolvedPasswordHash
        )

        val catUpper = request.categoryId.uppercase()
        val profUpper = request.profession.uppercase()
        when {
            profUpper == "STORE_OWNER" ||
                profUpper == "MALL_OWNER" ||
                catUpper in setOf("STORE", "MALL", "RESTAURANT", "MEDICAL") ->
                approveStoreOrMallRequest(vm, ctx)
            request.profession == "PROPERTY_OWNER" || catUpper == "PROPERTY" ->
                approvePropertyRequest(vm, ctx)
            request.profession == "JOB_POSTER" || catUpper == "JOB" ->
                approveJobPosterRequest(vm, ctx)
            request.profession == "JOB_SEEKER" || catUpper == "JOB_SEEKER" ->
                approveJobSeekerRequest(vm, ctx)
            request.profession == "CLIENT" || catUpper == "CLIENT" ->
                approveClientRequest(vm, ctx)
            else ->
                approveTechnicianProviderRequest(vm, ctx)
        }
    }

    fun rejectRequest(request: PendingProviderEntity, reason: String) {
        val cleanPhone = ValidatePhoneUseCase.normalizePhone(request.phone)
            .ifBlank { request.phone.trim().replace(" ", "").replace("+", "") }
        val now = System.currentTimeMillis()
        val finalReason = reason.ifBlank { "لم تستوفِ المستندات أو الشروط المطلوبة" }

        val rejectUpdates = mapOf(
            "status" to "REJECTED",
            "approvalStatus" to "REJECTED",
            "isActive" to false,
            "rejectionReason" to finalReason,
            "rejectedAt" to now,
            "rejectedBy" to "ADMIN",
            "updatedAt" to now
        )
        vm._pendingProviders.value = vm._pendingProviders.value.filter { it.id != request.id }

        vm.viewModelScope.launch {
            try {
                vm.db.collection("join_requests").document(request.id).update(rejectUpdates).await()
            } catch (_: Exception) {
            }
            if (cleanPhone.isNotEmpty() && cleanPhone != request.id) {
                try {
                    vm.db.collection("join_requests").document(cleanPhone).update(rejectUpdates).await()
                } catch (_: Exception) {
                }
            }
            try {
                vm.db.collection("pending_providers").document(request.id).delete().await()
            } catch (_: Exception) {
            }
            if (cleanPhone.isNotEmpty() && cleanPhone != request.id) {
                try {
                    vm.db.collection("pending_providers").document(cleanPhone).delete().await()
                } catch (_: Exception) {
                }
            }
            vm.mainViewModel.addNotification(
                title = "❌ تنويه حول طلب انضمامك",
                message = "للأسف لم يتم قبول طلب انضمامك للأسباب التالية: $finalReason. يمكنك تعديل البيانات وإعادة تقديم الطلب.",
                targetType = "USER",
                targetValue = request.phone
            )
            vm.mainViewModel.triggerNotification("❌ تم رفض طلب ${request.name} بسبب: $finalReason")
        }
    }

    fun approveTechnician(providerId: String) {
        val technician = vm._pendingProviders.value.find { it.id == providerId } ?: return
        approveRequest(technician)
    }

    fun rejectTechnician(providerId: String, reason: String = "لم يستوفِ الشروط") {
        val technician = vm._pendingProviders.value.find { it.id == providerId } ?: return
        rejectRequest(technician, reason)
    }

    private var pendingTechListener: com.google.firebase.firestore.ListenerRegistration? = null

    fun loadPendingTechnicians() {
        val role = vm._adminRole.value.uppercase()
        val isStaff = role in setOf("OWNER", "ADMIN", "SUPER_ADMIN", "MAIN_ADMIN", "SUPERVISOR", "AUDITOR", "OPERATIONS", "SUPPORT")
        if (!isStaff) return
        try {
            pendingTechListener?.remove()
            pendingTechListener = vm.db.collection("pending_providers")
                .limit(60)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    if (snapshot != null) {
                        val fetched = snapshot.documents.mapNotNull { doc ->
                            try {
                                doc.toObject(PendingProviderEntity::class.java)?.copy(id = doc.id)
                            } catch (_: Exception) {
                                try {
                                    PendingProviderEntity(
                                        id = doc.id,
                                        name = doc.getString("name") ?: "",
                                        phone = doc.getString("phone") ?: "",
                                        categoryId = doc.getString("categoryId") ?: "",
                                        area = doc.getString("area") ?: doc.getString("localArea") ?: "",
                                        localNeighborhood = doc.getString("localNeighborhood") ?: "",
                                        status = doc.getString("status") ?: "PENDING",
                                        reason = doc.getString("reason") ?: "",
                                        idPhotoBase64 = doc.getString("idPhotoBase64") ?: "",
                                        selfiePhotoBase64 = doc.getString("selfiePhotoBase64") ?: "",
                                        workPhotosBase64 = (doc.get("workPhotosBase64") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                                        gpsCoords = doc.getString("gpsCoords") ?: "",
                                        latitude = doc.getDouble("latitude") ?: 0.0,
                                        longitude = doc.getDouble("longitude") ?: 0.0,
                                        experienceYears = doc.getString("experienceYears") ?: "",
                                        qualification = doc.getString("qualification") ?: "",
                                        description = doc.getString("description") ?: ""
                                    )
                                } catch (_: Exception) {
                                    null
                                }
                            }
                        }
                        vm._pendingTechnicians.value = fetched
                    }
                }
        } catch (_: Exception) {
        }
    }

    fun approvePendingProvider(pending: PendingProviderEntity) = approveRequest(pending)

    fun rejectPendingProvider(pending: PendingProviderEntity, reason: String = "تم رفض الطلب") =
        rejectRequest(pending, reason)

    fun approveRegisteredUser(userId: String, userName: String = "") =
        approveRegisteredUserOp(vm, userId, userName)

    fun toggleBlockRegisteredUser(userId: String, currentBlocked: Boolean, userName: String = "") =
        toggleBlockRegisteredUserOp(vm, userId, currentBlocked, userName)

    fun deleteRegisteredUser(userId: String, userName: String = "") =
        deleteRegisteredUserOp(vm, userId, userName)

    fun checkAndGetDuplicateAccountType(phone: String, excludeId: String): String? =
        checkAndGetDuplicateAccountTypeOp(vm, phone, excludeId)

    fun loadPendingRequests() = loadPendingRequestsOp(vm)

    fun approveProviderRequest(requestId: String, onResult: ((Boolean) -> Unit)? = null) =
        approveProviderRequestOp(vm, this, requestId, onResult)

    fun rejectProviderRequest(requestId: String, reason: String = "", onResult: ((Boolean) -> Unit)? = null) =
        rejectProviderRequestOp(vm, requestId, reason, onResult)

    fun blockUser(userId: String, onResult: ((Boolean) -> Unit)? = null) = blockUserOp(vm, userId, onResult)
    fun unblockUser(userId: String, onResult: ((Boolean) -> Unit)? = null) = unblockUserOp(vm, userId, onResult)
    fun deleteUser(userId: String, onResult: ((Boolean) -> Unit)? = null) = deleteUserOp(vm, userId, onResult)
}
