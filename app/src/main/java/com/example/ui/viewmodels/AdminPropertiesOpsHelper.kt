package com.example.ui.viewmodels

import androidx.lifecycle.viewModelScope
import com.example.data.NotificationEntity
import com.example.data.PropertyEntity
import com.example.data.isValid
import com.example.domain.usecases.ValidatePhoneUseCase
import com.example.utils.FirebaseStorageUploader
import com.example.utils.SecureHasher
import kotlinx.coroutines.launch
import java.util.UUID

internal fun savePropertyOp(vm: AdminViewModel, property: PropertyEntity) {
    val cleanPhone = ValidatePhoneUseCase.normalizePhone(property.phone)
        .ifBlank { property.phone.trim().replace(" ", "").replace("+", "") }
    val duplicateType = vm.checkAndGetDuplicateAccountType(cleanPhone, property.id)
    if (duplicateType != null) {
        vm.mainViewModel.triggerNotification("❌ عذراً! رقم الهاتف (${property.phone}) مسجل بالفعل كـ ($duplicateType). لا يُسمح بتكرار الحسابات.")
        vm.logAdminActivity("محاولة تسجيل عقار مكرر محجوبة لرقم: $cleanPhone - نوع التكرار: $duplicateType")
        return
    }
    val targetId = if (property.id.isEmpty()) vm.db.collection("properties").document().id else property.id
    val rawPassword = property.passwordHash.trim()
    val resolvedPasswordHash = when {
        rawPassword.isNotBlank() && SecureHasher.isValidHash(rawPassword) -> rawPassword
        rawPassword.isNotBlank() -> SecureHasher.hashPassword(rawPassword)
        else -> ""
    }
    val finalProp = property.copy(
        id = targetId,
        phone = cleanPhone,
        ownerId = if (property.ownerId.isEmpty()) cleanPhone else property.ownerId,
        passwordHash = resolvedPasswordHash
    )

    val updatedProps = vm._properties.value.filter { it.id != targetId }.toMutableList()
    updatedProps.add(finalProp)
    vm._properties.value = updatedProps

    if (rawPassword.isNotEmpty() && !SecureHasher.isValidHash(rawPassword)) {
        createSecondaryAuthUserHelper(vm, vm.resolveAuthEmailForPhone(cleanPhone), rawPassword)
    }

    vm.viewModelScope.launch {
        val ctx = vm.appContext
        val finalImages = finalProp.images.mapIndexed { idx, img ->
            val photoPath = FirebaseStorageUploader.getPropertyPhotoPath(targetId, idx)
            if (ctx != null) {
                vm.resolveUploadImage(ctx, img, photoPath)
            } else {
                FirebaseStorageUploader.resolveBase64ToStorageUrl(img, photoPath)
            }
        }
        val finalPdfUrl = if (finalProp.pdfFileBase64.isNotBlank()) {
            FirebaseStorageUploader.resolveBase64ToStorageUrl(
                finalProp.pdfFileBase64,
                "properties/$targetId/deed.pdf"
            )
        } else ""

        val uploadedProp = finalProp.copy(
            images = finalImages,
            pdfFileBase64 = finalPdfUrl
        )
        val currentList = vm._properties.value.filter { it.id != targetId }.toMutableList()
        currentList.add(uploadedProp)
        vm._properties.value = currentList

        vm.db.collection("properties").document(targetId).set(uploadedProp)
            .addOnSuccessListener {
                if (!uploadedProp.isActive || !uploadedProp.isApproved) {
                    val adminNotif = NotificationEntity(
                        id = UUID.randomUUID().toString(),
                        title = "🏡 طلب انضمام عقار/مكتب عقاري جديد",
                        message = "قدم المكتب/العقار '${uploadedProp.title}' طلب انضمام جديد بمنطقة ${uploadedProp.cityId}.",
                        targetType = "SUPERVISOR",
                        targetValue = "ALL",
                        timestamp = System.currentTimeMillis()
                    )
                    try {
                        if (adminNotif.isValid()) {
                            vm.db.collection("notifications").document(adminNotif.id).set(adminNotif)
                        }
                    } catch (_: Exception) {
                    }
                }
                vm.mainViewModel.triggerNotification("✅ تم تسجيل بيانات العقار بنجاح!")
            }
            .addOnFailureListener {
                vm.mainViewModel.triggerNotification("⚠️ تم الحفظ محلياً وبانتظار مزامنة السحابة")
            }
    }
}

internal fun deletePropertyOp(vm: AdminViewModel, propertyId: String) {
    vm._properties.value = vm._properties.value.map {
        if (it.id == propertyId) it.copy(isDeleted = true, deletedAt = System.currentTimeMillis()) else it
    }
    val updates = mapOf(
        "isDeleted" to true,
        "deleted" to true,
        "deletedAt" to System.currentTimeMillis()
    )
    vm.db.collection("properties").document(propertyId).update(updates)
        .addOnSuccessListener {
            vm.mainViewModel.triggerNotification("🗑️ تم حذف العقار بنجاح")
        }
        .addOnFailureListener {
            vm.db.collection("properties").document(propertyId).get().addOnSuccessListener { snapshot ->
                val property = snapshot.toObject(PropertyEntity::class.java)
                if (property != null) {
                    vm.db.collection("properties").document(propertyId)
                        .set(property.copy(isDeleted = true, deletedAt = System.currentTimeMillis()))
                        .addOnSuccessListener {
                            vm.mainViewModel.triggerNotification("🗑️ تم حذف العقار بنجاح")
                        }
                }
            }
        }
}

internal fun restorePropertyOp(vm: AdminViewModel, propertyId: String) {
    vm._properties.value = vm._properties.value.map {
        if (it.id == propertyId) it.copy(isDeleted = false, deletedAt = null) else it
    }
    val updates = hashMapOf<String, Any?>(
        "isDeleted" to false,
        "deleted" to false,
        "deletedAt" to null
    )
    vm.db.collection("properties").document(propertyId).update(updates)
        .addOnSuccessListener {
            vm.mainViewModel.triggerNotification("♻️ تم استعادة العقار بنجاح")
        }
}

internal fun deletePropertyPermanentlyOp(vm: AdminViewModel, propertyId: String) {
    vm._properties.value = vm._properties.value.filter { it.id != propertyId }
    vm.db.collection("properties").document(propertyId).delete()
        .addOnSuccessListener {
            vm.mainViewModel.triggerNotification("🗑️ تم حذف العقار نهائياً من النظام")
        }
}

internal fun setPropertyActiveOp(vm: AdminViewModel, propertyId: String, isActive: Boolean) {
    val cleanTargetId = propertyId.trim()
    val targetProp = vm._properties.value.find {
        it.id == cleanTargetId ||
            it.phone.trim().replace(" ", "").replace("+", "") == cleanTargetId.replace(" ", "").replace("+", "")
    }
    val docId = targetProp?.id ?: cleanTargetId

    vm._properties.value = vm._properties.value.map {
        if (it.id == docId || it.id == cleanTargetId) it.copy(isActive = isActive, isApproved = isActive) else it
    }

    val updates = mapOf(
        "isActive" to isActive,
        "isApproved" to isActive,
        "active" to isActive
    )

    if (targetProp != null && isActive) {
        val cleanPhone = targetProp.phone.trim().replace(" ", "").replace("+", "")
        if (cleanPhone.isNotEmpty()) {
            vm.db.collection("pending_providers").whereEqualTo("phone", targetProp.phone).get()
                .addOnSuccessListener { qs ->
                    qs?.documents?.forEach { doc ->
                        vm.db.collection("pending_providers").document(doc.id).delete()
                    }
                }
            vm._pendingProviders.value = vm._pendingProviders.value.filter {
                it.phone.trim().replace(" ", "").replace("+", "") != cleanPhone
            }
        }

        val notification = NotificationEntity(
            id = UUID.randomUUID().toString(),
            title = "🎉 تم قبول وإعتماد إعلان عقارك",
            message = "تهانينا! تم قبول ونشر إعلان العقار (${targetProp.title}) للجميع!",
            targetType = "USER",
            targetValue = targetProp.phone,
            timestamp = System.currentTimeMillis()
        )
        vm.mainViewModel._notifications.value = listOf(notification) + vm.mainViewModel._notifications.value
        try {
            if (notification.isValid()) {
                vm.db.collection("notifications").document(notification.id).set(notification)
            }
        } catch (_: Exception) {
        }
    }

    vm.db.collection("properties").document(docId).update(updates)
        .addOnSuccessListener {
            vm.mainViewModel.triggerNotification(
                if (isActive) "✅ تم تفعيل ونشر العقار للجميع" else "🔒 تم إلغاء تفعيل ونشر العقار"
            )
        }
        .addOnFailureListener {
            if (targetProp != null) {
                vm.db.collection("properties").document(docId)
                    .set(targetProp.copy(isActive = isActive, isApproved = isActive))
                    .addOnSuccessListener {
                        vm.mainViewModel.triggerNotification(
                            if (isActive) "✅ تم تفعيل ونشر العقار للجميع" else "🔒 تم إلغاء تفعيل ونشر العقار"
                        )
                    }
            }
        }
}

internal fun setPropertyPinnedOp(vm: AdminViewModel, propertyId: String, isPinned: Boolean) {
    vm._properties.value = vm._properties.value.map {
        if (it.id == propertyId) it.copy(isPinned = isPinned) else it
    }
    val updates = mapOf("isPinned" to isPinned, "pinned" to isPinned)
    vm.db.collection("properties").document(propertyId).update(updates)
        .addOnSuccessListener {
            vm.mainViewModel.triggerNotification(
                if (isPinned) "📌 تم تمييز وتثبيت العقار في الصدارة" else "📌 تم إلغاء تثبيت العقار"
            )
        }
}

internal fun setPropertyVipOp(vm: AdminViewModel, propertyId: String, isVip: Boolean) {
    vm._properties.value = vm._properties.value.map {
        if (it.id == propertyId) it.copy(isVip = isVip) else it
    }
    vm.db.collection("properties").document(propertyId).update("isVip", isVip)
        .addOnSuccessListener {
            vm.mainViewModel.triggerNotification(
                if (isVip) "🏆 تم تمييز العقار بشارة VIP" else "🔒 تم إلغاء شارة VIP عن العقار"
            )
        }
}

internal fun setPropertyVerifiedOp(vm: AdminViewModel, propertyId: String, isVerified: Boolean) {
    vm._properties.value = vm._properties.value.map {
        if (it.id == propertyId) it.copy(isVerified = isVerified) else it
    }
    vm.db.collection("properties").document(propertyId).update("isVerified", isVerified)
        .addOnSuccessListener {
            vm.mainViewModel.triggerNotification(
                if (isVerified) "🛡️ تم توثيق إعلان العقار" else "🔒 تم إلغاء التوثيق عن العقار"
            )
        }
}

internal fun setPropertyRecommendedOp(vm: AdminViewModel, propertyId: String, isRecommended: Boolean) {
    vm._properties.value = vm._properties.value.map {
        if (it.id == propertyId) it.copy(isRecommended = isRecommended) else it
    }
    vm.db.collection("properties").document(propertyId).update("isRecommended", isRecommended)
        .addOnSuccessListener {
            vm.mainViewModel.triggerNotification(
                if (isRecommended) "💖 تم ترشيح العقار كموصى به" else "🔒 تم إلغاء ترشيح العقار"
            )
        }
}

internal fun setPropertyBlockedOp(vm: AdminViewModel, propertyId: String, isBlocked: Boolean, reason: String) {
    vm._properties.value = vm._properties.value.map {
        if (it.id == propertyId) it.copy(isBlocked = isBlocked, blockReason = reason) else it
    }
    val updates = mapOf("isBlocked" to isBlocked, "blockReason" to reason)
    vm.db.collection("properties").document(propertyId).update(updates)
        .addOnSuccessListener {
            vm.mainViewModel.triggerNotification(
                if (isBlocked) "🚫 تم حظر إعلان العقار بنجاح ($reason)" else "✅ تم إلغاء حظر إعلان العقار"
            )
        }
}

internal fun setPropertyChatDisabledOp(vm: AdminViewModel, propertyId: String, isDisabled: Boolean) {
    vm._properties.value = vm._properties.value.map {
        if (it.id == propertyId) it.copy(isChatDisabled = isDisabled) else it
    }
    vm.db.collection("properties").document(propertyId).update("isChatDisabled", isDisabled)
        .addOnSuccessListener {
            vm.mainViewModel.triggerNotification(
                if (isDisabled) "🔇 تم إيقاف الدردشة للمعلن" else "💬 تم تفعيل الدردشة للمعلن"
            )
        }
}

internal fun setPropertyNotificationsDisabledOp(vm: AdminViewModel, propertyId: String, isDisabled: Boolean) {
    vm._properties.value = vm._properties.value.map {
        if (it.id == propertyId) it.copy(isNotificationsDisabled = isDisabled) else it
    }
    vm.db.collection("properties").document(propertyId).update("isNotificationsDisabled", isDisabled)
        .addOnSuccessListener {
            vm.mainViewModel.triggerNotification(
                if (isDisabled) "🔕 تم كتم الإشعارات للمعلن" else "🔔 تم تفعيل الإشعارات للمعلن"
            )
        }
}

internal fun setPropertyPaymentEnabledOp(vm: AdminViewModel, propertyId: String, isEnabled: Boolean) {
    vm.db.collection("properties").document(propertyId).update("paymentEnabled", isEnabled)
        .addOnSuccessListener {
            vm.mainViewModel.triggerNotification(
                if (isEnabled) "💳 تم تفعيل نظام الدفع للعقار" else "🔒 تم تعطيل نظام الدفع للعقار"
            )
        }
}

internal fun togglePropertyBlockedOp(vm: AdminViewModel, propertyId: String, isBlocked: Boolean) {
    vm.db.collection("properties").document(propertyId).update("isBlocked", isBlocked)
        .addOnSuccessListener {
            vm.mainViewModel.triggerNotification(
                if (isBlocked) "🚫 تم حظر العقار" else "✅ تم إلغاء حظر العقار"
            )
        }
}

internal fun approvePropertyPdfOp(vm: AdminViewModel, propertyId: String, approve: Boolean) {
    vm.db.collection("properties").document(propertyId).get().addOnSuccessListener { snapshot ->
        val prop = snapshot.toObject(PropertyEntity::class.java)
        if (prop != null) {
            vm.db.collection("properties").document(propertyId)
                .set(prop.copy(pdfStatus = if (approve) "APPROVED" else "REJECTED"))
                .addOnSuccessListener {
                    vm.mainViewModel.triggerNotification(
                        if (approve) "✅ تم قبول ملف الـ PDF للعقار بنجاح!" else "❌ تم رفض ملف الـ PDF للعقار."
                    )
                }
        }
    }
}
