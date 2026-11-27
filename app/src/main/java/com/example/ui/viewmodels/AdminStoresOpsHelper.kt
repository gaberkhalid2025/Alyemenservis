package com.example.ui.viewmodels

import androidx.lifecycle.viewModelScope
import com.example.data.NotificationEntity
import com.example.data.StoreEntity
import com.example.data.isValid
import com.example.domain.usecases.ValidatePhoneUseCase
import com.example.utils.FirebaseStorageUploader
import com.example.utils.SecureHasher
import kotlinx.coroutines.launch
import java.util.UUID

internal fun createSecondaryAuthUserHelper(vm: AdminViewModel, authEmail: String, rawPassword: String) {
    try {
        val primaryApp = com.google.firebase.FirebaseApp.getInstance()
        val secondaryApp = runCatching {
            com.google.firebase.FirebaseApp.getInstance("SecondaryUserCreation")
        }.getOrElse {
            com.google.firebase.FirebaseApp.initializeApp(
                vm.application,
                primaryApp.options,
                "SecondaryUserCreation"
            )
        }
        val secondaryAuth = com.google.firebase.auth.FirebaseAuth.getInstance(secondaryApp)
        secondaryAuth.createUserWithEmailAndPassword(authEmail, rawPassword)
            .addOnCompleteListener {
                runCatching { secondaryAuth.signOut() }
            }
    } catch (_: Exception) {
    }
}

internal fun saveStoreOp(vm: AdminViewModel, store: StoreEntity) {
    val cleanPhone = ValidatePhoneUseCase.normalizePhone(store.phone)
        .ifBlank { store.phone.trim().replace(" ", "").replace("+", "") }
    val duplicateType = vm.checkAndGetDuplicateAccountType(cleanPhone, store.id)
    if (duplicateType != null) {
        vm.mainViewModel.triggerNotification("❌ عذراً! رقم الهاتف (${store.phone}) مسجل بالفعل كـ ($duplicateType). لا يُسمح بتكرار الحسابات.")
        vm.logAdminActivity("محاولة تسجيل متجر مكرر محجوبة لرقم: $cleanPhone - نوع التكرار: $duplicateType")
        return
    }
    val targetId = if (store.id.isEmpty()) vm.db.collection("stores").document().id else store.id
    val rawPassword = store.passwordHash.trim()
    val resolvedPasswordHash = when {
        rawPassword.isNotBlank() && SecureHasher.isValidHash(rawPassword) -> rawPassword
        rawPassword.isNotBlank() -> SecureHasher.hashPassword(rawPassword)
        else -> ""
    }
    val finalStore = store.copy(
        id = targetId,
        phone = cleanPhone,
        ownerId = if (store.ownerId.isEmpty()) cleanPhone else store.ownerId,
        passwordHash = resolvedPasswordHash
    )

    val updatedStores = vm._stores.value.filter { it.id != targetId }.toMutableList()
    updatedStores.add(finalStore)
    vm._stores.value = updatedStores

    if (rawPassword.isNotEmpty() && !SecureHasher.isValidHash(rawPassword)) {
        createSecondaryAuthUserHelper(vm, vm.resolveAuthEmailForPhone(cleanPhone), rawPassword)
    }

    vm.viewModelScope.launch {
        val ctx = vm.appContext
        val logoPath = FirebaseStorageUploader.getStoreLogoPath(targetId)
        val coverPath = FirebaseStorageUploader.getStoreCoverPath(targetId)
        val finalLogo = if (ctx != null) {
            vm.resolveUploadImage(ctx, finalStore.logoImage, logoPath)
        } else {
            FirebaseStorageUploader.resolveBase64ToStorageUrl(finalStore.logoImage, logoPath)
        }
        val finalCover = if (ctx != null) {
            vm.resolveUploadImage(ctx, finalStore.coverImage, coverPath)
        } else {
            FirebaseStorageUploader.resolveBase64ToStorageUrl(finalStore.coverImage, coverPath)
        }
        val finalImages = finalStore.images.mapIndexed { idx, img ->
            val photoPath = FirebaseStorageUploader.getStorePhotoPath(targetId, idx)
            if (ctx != null) {
                vm.resolveUploadImage(ctx, img, photoPath)
            } else {
                FirebaseStorageUploader.resolveBase64ToStorageUrl(img, photoPath)
            }
        }
        val finalPdfUrl = if (finalStore.pdfFileBase64.isNotBlank()) {
            FirebaseStorageUploader.resolveBase64ToStorageUrl(
                finalStore.pdfFileBase64,
                "stores/$targetId/catalog.pdf"
            )
        } else ""

        val uploadedStore = finalStore.copy(
            logoImage = finalLogo,
            coverImage = finalCover,
            images = finalImages,
            pdfFileBase64 = finalPdfUrl
        )

        val currentList = vm._stores.value.filter { it.id != targetId }.toMutableList()
        currentList.add(uploadedStore)
        vm._stores.value = currentList

        vm.db.collection("stores").document(targetId).set(uploadedStore)
            .addOnSuccessListener {
                if (!uploadedStore.isActive || !uploadedStore.isApproved) {
                    val adminNotif = NotificationEntity(
                        id = UUID.randomUUID().toString(),
                        title = "🏪 طلب انضمام نشاط تجاري جديد",
                        message = "قدم النشاط التجاري '${uploadedStore.name}' (${uploadedStore.categoryId}) طلب انضمام جديد بمنطقة ${uploadedStore.cityId}.",
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
                vm.mainViewModel.triggerNotification("✅ تم حفظ وتأكيد بيانات الطلب بنجاح!")
            }
            .addOnFailureListener {
                vm.mainViewModel.triggerNotification("⚠️ تم حفظ الطلب محلياً وفي انتظار مزامنة الشبكة")
            }
    }
}

internal fun deleteStoreOp(vm: AdminViewModel, storeId: String) {
    vm._stores.value = vm._stores.value.map {
        if (it.id == storeId) it.copy(isDeleted = true, deletedAt = System.currentTimeMillis()) else it
    }
    val updates = mapOf(
        "isDeleted" to true,
        "deleted" to true,
        "deletedAt" to System.currentTimeMillis()
    )
    vm.db.collection("stores").document(storeId).update(updates)
        .addOnSuccessListener {
            vm.mainViewModel.triggerNotification("🗑️ تم حذف المتجر ونقله للمحذوفات")
        }
        .addOnFailureListener {
            vm.db.collection("stores").document(storeId).get().addOnSuccessListener { snapshot ->
                val store = snapshot.toObject(StoreEntity::class.java)
                if (store != null) {
                    vm.db.collection("stores").document(storeId)
                        .set(store.copy(isDeleted = true, deletedAt = System.currentTimeMillis()))
                        .addOnSuccessListener {
                            vm.mainViewModel.triggerNotification("🗑️ تم حذف المتجر ونقله للمحذوفات")
                        }
                }
            }
        }
}

internal fun restoreStoreOp(vm: AdminViewModel, storeId: String) {
    vm._stores.value = vm._stores.value.map {
        if (it.id == storeId) it.copy(isDeleted = false, deletedAt = null) else it
    }
    val updates = hashMapOf<String, Any?>(
        "isDeleted" to false,
        "deleted" to false,
        "deletedAt" to null
    )
    vm.db.collection("stores").document(storeId).update(updates)
        .addOnSuccessListener {
            vm.mainViewModel.triggerNotification("♻️ تم استعادة المتجر بنجاح")
        }
}

internal fun deleteStorePermanentlyOp(vm: AdminViewModel, storeId: String) {
    vm._stores.value = vm._stores.value.filter { it.id != storeId }
    vm.db.collection("stores").document(storeId).delete()
        .addOnSuccessListener {
            vm.mainViewModel.triggerNotification("🗑️ تم حذف المتجر نهائياً من النظام")
        }
}

internal fun setStoreActiveOp(vm: AdminViewModel, storeId: String, isActive: Boolean) {
    val cleanTargetId = storeId.trim()
    val targetStore = vm._stores.value.find {
        it.id == cleanTargetId ||
            it.phone.trim().replace(" ", "").replace("+", "") == cleanTargetId.replace(" ", "").replace("+", "")
    }
    val docId = targetStore?.id ?: cleanTargetId

    vm._stores.value = vm._stores.value.map {
        if (it.id == docId || it.id == cleanTargetId) it.copy(isActive = isActive, isApproved = isActive) else it
    }

    val updates = mapOf(
        "isActive" to isActive,
        "isApproved" to isActive,
        "active" to isActive
    )

    if (targetStore != null && isActive) {
        val cleanPhone = targetStore.phone.trim().replace(" ", "").replace("+", "")
        if (cleanPhone.isNotEmpty()) {
            vm.db.collection("pending_providers").whereEqualTo("phone", targetStore.phone).get()
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
            title = "🎉 تم قبول واعتماد طلبك بنجاح!",
            message = "تهانينا! تم قبول واعتماد منشأتك (${targetStore.name}) بالدليل اليمني بنجاح!",
            targetType = "USER",
            targetValue = targetStore.phone,
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

    vm.db.collection("stores").document(docId).update(updates)
        .addOnSuccessListener {
            vm.mainViewModel.triggerNotification(
                if (isActive) "✅ تم تفعيل المتجر والموافقة عليه" else "🔒 تم إلغاء تفعيل المتجر"
            )
        }
        .addOnFailureListener {
            if (targetStore != null) {
                vm.db.collection("stores").document(docId)
                    .set(targetStore.copy(isActive = isActive, isApproved = isActive))
                    .addOnSuccessListener {
                        vm.mainViewModel.triggerNotification(
                            if (isActive) "✅ تم تفعيل المتجر والموافقة عليه" else "🔒 تم إلغاء تفعيل المتجر"
                        )
                    }
            }
        }
}

internal fun setStorePinnedOp(vm: AdminViewModel, storeId: String, isPinned: Boolean) {
    vm._stores.value = vm._stores.value.map {
        if (it.id == storeId) it.copy(isPinned = isPinned) else it
    }
    val updates = mapOf("isPinned" to isPinned, "pinned" to isPinned)
    vm.db.collection("stores").document(storeId).update(updates)
        .addOnSuccessListener {
            vm.mainViewModel.triggerNotification(
                if (isPinned) "📌 تم تثبيت المتجر في الشاشة الرئيسية" else "📌 تم إلغاء تثبيت المتجر"
            )
        }
}

internal fun setStoreVipOp(vm: AdminViewModel, storeId: String, isVip: Boolean) {
    vm._stores.value = vm._stores.value.map {
        if (it.id == storeId) it.copy(isVip = isVip) else it
    }
    vm.db.collection("stores").document(storeId).update("isVip", isVip)
        .addOnSuccessListener {
            vm.mainViewModel.triggerNotification(
                if (isVip) "🏆 تم تمييز المتجر بشارة VIP" else "🔒 تم إلغاء شارة VIP عن المتجر"
            )
        }
}

internal fun setStoreVerifiedOp(vm: AdminViewModel, storeId: String, isVerified: Boolean) {
    vm._stores.value = vm._stores.value.map {
        if (it.id == storeId) it.copy(isVerified = isVerified) else it
    }
    vm.db.collection("stores").document(storeId).update("isVerified", isVerified)
        .addOnSuccessListener {
            vm.mainViewModel.triggerNotification(
                if (isVerified) "🛡️ تم توثيق حساب المتجر" else "🔒 تم إلغاء التوثيق عن المتجر"
            )
        }
}

internal fun setStoreRecommendedOp(vm: AdminViewModel, storeId: String, isRecommended: Boolean) {
    vm._stores.value = vm._stores.value.map {
        if (it.id == storeId) it.copy(isRecommended = isRecommended) else it
    }
    vm.db.collection("stores").document(storeId).update("isRecommended", isRecommended)
        .addOnSuccessListener {
            vm.mainViewModel.triggerNotification(
                if (isRecommended) "💖 تم ترشيح المتجر كموصى به" else "🔒 تم إلغاء ترشيح المتجر"
            )
        }
}

internal fun setStoreBlockedOp(vm: AdminViewModel, storeId: String, isBlocked: Boolean, reason: String) {
    vm._stores.value = vm._stores.value.map {
        if (it.id == storeId) it.copy(isBlocked = isBlocked, blockReason = reason) else it
    }
    val updates = mapOf("isBlocked" to isBlocked, "blockReason" to reason)
    vm.db.collection("stores").document(storeId).update(updates)
        .addOnSuccessListener {
            vm.mainViewModel.triggerNotification(
                if (isBlocked) "🚫 تم حظر المحل/المركز بنجاح ($reason)" else "✅ تم إلغاء حظر المحل/المركز"
            )
        }
}

internal fun setStoreChatDisabledOp(vm: AdminViewModel, storeId: String, isDisabled: Boolean) {
    vm._stores.value = vm._stores.value.map {
        if (it.id == storeId) it.copy(isChatDisabled = isDisabled) else it
    }
    vm.db.collection("stores").document(storeId).update("isChatDisabled", isDisabled)
        .addOnSuccessListener {
            vm.mainViewModel.triggerNotification(
                if (isDisabled) "🔇 تم إيقاف الدردشة للمتجر" else "💬 تم تفعيل الدردشة للمتجر"
            )
        }
}

internal fun setStoreNotificationsDisabledOp(vm: AdminViewModel, storeId: String, isDisabled: Boolean) {
    vm._stores.value = vm._stores.value.map {
        if (it.id == storeId) it.copy(isNotificationsDisabled = isDisabled) else it
    }
    vm.db.collection("stores").document(storeId).update("isNotificationsDisabled", isDisabled)
        .addOnSuccessListener {
            vm.mainViewModel.triggerNotification(
                if (isDisabled) "🔕 تم كتم الإشعارات للمتجر" else "🔔 تم تفعيل الإشعارات للمتجر"
            )
        }
}

internal fun setStorePaymentEnabledOp(vm: AdminViewModel, storeId: String, isEnabled: Boolean) {
    vm.db.collection("stores").document(storeId).update("paymentEnabled", isEnabled)
        .addOnSuccessListener {
            vm.mainViewModel.triggerNotification(
                if (isEnabled) "💳 تم تفعيل نظام الدفع والمحفظة للمتجر" else "🔒 تم تعطيل نظام الدفع للمتجر"
            )
        }
}

internal fun toggleStoreBlockedOp(vm: AdminViewModel, storeId: String, isBlocked: Boolean) {
    vm.db.collection("stores").document(storeId).update("isBlocked", isBlocked)
        .addOnSuccessListener {
            vm.mainViewModel.triggerNotification(
                if (isBlocked) "🚫 تم حظر المتجر" else "✅ تم إلغاء حظر المتجر"
            )
        }
}

internal fun toggleStoreActiveOp(vm: AdminViewModel, storeId: String) {
    val store = vm._stores.value.find { it.id == storeId }
    if (store != null) {
        val updated = store.copy(isActive = !store.isActive)
        saveStoreOp(vm, updated)
        vm.mainViewModel.triggerNotification(
            if (!updated.isActive) "🔒 تم حظر/إيقاف المحل/المركز مؤقتاً" else "🟢 تم تفعيل المحل/المركز"
        )
    }
}

internal fun toggleStorePinnedOp(vm: AdminViewModel, storeId: String) {
    val store = vm._stores.value.find { it.id == storeId }
    if (store != null) {
        val updated = store.copy(isPinned = !store.isPinned)
        saveStoreOp(vm, updated)
        vm.mainViewModel.triggerNotification(
            if (updated.isPinned) "📌 تم تثبيت المحل في البداية" else "🔓 تم إلغاء تثبيت المحل"
        )
    }
}

internal fun toggleStoreChatDisabledOp(vm: AdminViewModel, storeId: String) {
    val store = vm._stores.value.find { it.id == storeId }
    if (store != null) {
        val updated = store.copy(isChatDisabled = !store.isChatDisabled)
        saveStoreOp(vm, updated)
        vm.mainViewModel.triggerNotification(
            if (updated.isChatDisabled) "🚫 تم إيقاف المحادثات للمحل/المركز" else "💬 تم تفعيل المحادثات للمحل/المركز"
        )
    }
}

internal fun approveStorePdfOp(vm: AdminViewModel, storeId: String, approve: Boolean) {
    vm.db.collection("stores").document(storeId).get().addOnSuccessListener { snapshot ->
        val store = snapshot.toObject(StoreEntity::class.java)
        if (store != null) {
            vm.db.collection("stores").document(storeId)
                .set(store.copy(pdfStatus = if (approve) "APPROVED" else "REJECTED"))
                .addOnSuccessListener {
                    vm.mainViewModel.triggerNotification(
                        if (approve) "✅ تم قبول ملف الـ PDF للمحل بنجاح!" else "❌ تم رفض ملف الـ PDF للمحل."
                    )
                }
        }
    }
}
