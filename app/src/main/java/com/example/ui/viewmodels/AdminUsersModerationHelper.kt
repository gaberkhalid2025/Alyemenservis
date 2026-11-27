package com.example.ui.viewmodels

import com.example.data.PendingProviderEntity
import com.example.data.models.JoinRequestEntity

internal fun approveRegisteredUserOp(vm: AdminViewModel, userId: String, userName: String) {
    vm._registeredUsersList.value = vm._registeredUsersList.value.map { u ->
        if (u["id"]?.toString() == userId) {
            u.toMutableMap().apply { put("isApproved", true) }
        } else u
    }
    vm.db.collection("registered_users").document(userId).update("isApproved", true)
        .addOnSuccessListener {
            vm.mainViewModel.triggerNotification("✅ تم قبول وتأكيد طلب انضمام المستخدم $userName بنجاح من قبل الأدمن!")
        }
    vm.db.collection("users").document(userId).update("isApproved", true)
}

internal fun toggleBlockRegisteredUserOp(
    vm: AdminViewModel,
    userId: String,
    currentBlocked: Boolean,
    userName: String
) {
    val newBlockedState = !currentBlocked
    vm._registeredUsersList.value = vm._registeredUsersList.value.map { u ->
        if (u["id"]?.toString() == userId) {
            u.toMutableMap().apply { put("isBlocked", newBlockedState) }
        } else u
    }
    vm.db.collection("registered_users").document(userId).update("isBlocked", newBlockedState)
        .addOnSuccessListener {
            val actionText = if (newBlockedState) "حظر" else "إلغاء حظر"
            vm.mainViewModel.triggerNotification("🛡️ تم $actionText حساب المستخدم $userName بنجاح.")
        }
    vm.db.collection("users").document(userId).update("isBlocked", newBlockedState)
}

internal fun deleteRegisteredUserOp(vm: AdminViewModel, userId: String, userName: String) {
    vm._registeredUsersList.value = vm._registeredUsersList.value.filter { u -> u["id"]?.toString() != userId }
    vm.db.collection("registered_users").document(userId).delete()
        .addOnSuccessListener {
            vm.mainViewModel.triggerNotification("🗑️ تم حذف حساب المستخدم $userName من القاعدة بنجاح.")
        }
    vm.db.collection("users").document(userId).delete()
}

internal fun checkAndGetDuplicateAccountTypeOp(
    vm: AdminViewModel,
    phone: String,
    excludeId: String
): String? {
    val cleanInput = phone.trim().replace(" ", "").replace("+", "")
    if (cleanInput.isEmpty()) return null

    val dupProvider = vm.mainViewModel.homeViewModel._providers.value.any {
        !it.isDeleted && it.phone.trim().replace(" ", "").replace("+", "") == cleanInput && it.id != excludeId
    }
    if (dupProvider) return "مقدم خدمة نشط (فني)"

    val dupPendingProvider = vm._pendingProviders.value.any {
        it.status == "PENDING" && it.phone.trim().replace(" ", "").replace("+", "") == cleanInput && it.id != excludeId
    }
    if (dupPendingProvider) return "طلب انضمام فني قيد المراجعة"

    val dupStore = vm._stores.value.any {
        !it.isDeleted && it.phone.trim().replace(" ", "").replace("+", "") == cleanInput && it.id != excludeId
    }
    if (dupStore) return "محل أو متجر تجاري مسجل"

    val dupProp = vm._properties.value.any {
        !it.isDeleted && it.phone.trim().replace(" ", "").replace("+", "") == cleanInput && it.id != excludeId
    }
    if (dupProp) return "إعلان عقاري مدرج"

    return null
}

internal fun loadPendingRequestsOp(vm: AdminViewModel) {
    vm.db.collection("join_requests")
        .get()
        .addOnSuccessListener { snap ->
            val list = snap.documents.mapNotNull { doc ->
                try {
                    val req = doc.toObject(JoinRequestEntity::class.java)
                    if (req != null) {
                        val sectionName = when (req.type.uppercase()) {
                            "PROVIDER" -> "فني / مهني"
                            "STORE" -> "متجر / معرض"
                            "RESTAURANT" -> "مطعم / كافيه"
                            "MEDICAL" -> "مركز طبي / عيادة"
                            "PROPERTY" -> "عقار / أرض"
                            "JOB" -> "وظيفة / شاغر"
                            "CLIENT" -> "مستخدم عادي"
                            else -> req.type
                        }
                        PendingRequest(
                            id = req.id,
                            name = req.fullName,
                            phone = req.phone,
                            section = sectionName,
                            city = req.city,
                            details = "الفئة: ${req.categoryName} • الحي: ${req.neighborhood}",
                            status = req.status,
                            createdAt = req.submittedAt
                        )
                    } else null
                } catch (_: Exception) {
                    null
                }
            }
            vm._pendingRequestsState.value = list
        }
}

internal fun approveProviderRequestOp(
    vm: AdminViewModel,
    delegate: AdminRequestsAndUsersDelegate,
    requestId: String,
    onResult: ((Boolean) -> Unit)?
) {
    vm.db.collection("join_requests").document(requestId).get().addOnSuccessListener { snapshot ->
        val req = snapshot.toObject(JoinRequestEntity::class.java)
        if (req != null) {
            val requestProfession = when (req.type.uppercase()) {
                "STORE", "RESTAURANT", "MEDICAL" -> "STORE_OWNER"
                "PROPERTY" -> "PROPERTY_OWNER"
                "JOB" -> "JOB_POSTER"
                "CLIENT" -> "CLIENT"
                else -> "PROVIDER"
            }
            val pendingProvider = PendingProviderEntity(
                id = req.id,
                name = req.fullName,
                phone = req.phone,
                categoryId = req.categoryId,
                area = req.city,
                localNeighborhood = req.neighborhood,
                status = "APPROVED",
                idPhotoBase64 = req.idCardImage,
                selfiePhotoBase64 = req.profileImage,
                workPhotosBase64 = req.workImages,
                customCategoryName = req.categoryName,
                profession = requestProfession,
                passwordHash = req.passwordHash,
                providerType = requestProfession,
                gpsCoords = snapshot.getString("gpsCoords") ?: "",
                latitude = snapshot.getDouble("latitude") ?: 0.0,
                longitude = snapshot.getDouble("longitude") ?: 0.0,
                experienceYears = snapshot.getString("experienceYears") ?: "",
                qualification = snapshot.getString("qualification") ?: "",
                description = snapshot.getString("description") ?: "",
                productAttachmentsJson = snapshot.getString("productAttachmentsJson") ?: ""
            )
            delegate.approveRequest(pendingProvider)
            loadPendingRequestsOp(vm)
            onResult?.invoke(true)
        } else {
            onResult?.invoke(false)
        }
    }.addOnFailureListener {
        onResult?.invoke(false)
    }
}

internal fun rejectProviderRequestOp(
    vm: AdminViewModel,
    requestId: String,
    reason: String,
    onResult: ((Boolean) -> Unit)?
) {
    vm.db.collection("join_requests").document(requestId).update(
        mapOf(
            "status" to "REJECTED",
            "approvalStatus" to "REJECTED",
            "rejectionReason" to reason
        )
    ).addOnSuccessListener {
        vm.db.collection("pending_providers").document(requestId).delete()
        vm.recordAuditLog("REJECT_REQUEST", "تم رفض طلب الانضمام $requestId. السبب: $reason")
        loadPendingRequestsOp(vm)
        vm.mainViewModel.triggerNotification("🚫 تم رفض طلب الانضمام.")
        onResult?.invoke(true)
    }.addOnFailureListener {
        onResult?.invoke(false)
    }
}

internal fun blockUserOp(vm: AdminViewModel, userId: String, onResult: ((Boolean) -> Unit)?) {
    vm.db.collection("users").document(userId).update("isBlocked", true)
        .addOnSuccessListener {
            vm.recordAuditLog("BLOCK_USER", "حظر المستخدم $userId")
            vm.mainViewModel.triggerNotification("🚫 تم حظر المستخدم بنجاح.")
            onResult?.invoke(true)
        }
        .addOnFailureListener { onResult?.invoke(false) }
}

internal fun unblockUserOp(vm: AdminViewModel, userId: String, onResult: ((Boolean) -> Unit)?) {
    vm.db.collection("users").document(userId).update("isBlocked", false)
        .addOnSuccessListener {
            vm.recordAuditLog("UNBLOCK_USER", "إلغاء حظر المستخدم $userId")
            vm.mainViewModel.triggerNotification("✅ تم إلغاء حظر المستخدم.")
            onResult?.invoke(true)
        }
        .addOnFailureListener { onResult?.invoke(false) }
}

internal fun deleteUserOp(vm: AdminViewModel, userId: String, onResult: ((Boolean) -> Unit)?) {
    vm.db.collection("users").document(userId).delete()
        .addOnSuccessListener {
            vm.recordAuditLog("DELETE_USER", "حذف المستخدم $userId")
            vm.mainViewModel.triggerNotification("🗑️ تم حذف حساب المستخدم بنجاح.")
            onResult?.invoke(true)
        }
        .addOnFailureListener { onResult?.invoke(false) }
}
