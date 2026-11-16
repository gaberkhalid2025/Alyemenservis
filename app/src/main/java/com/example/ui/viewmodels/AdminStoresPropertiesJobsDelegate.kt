package com.example.ui.viewmodels

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.viewModelScope
import com.example.data.JobApplicationEntity
import com.example.data.JobEntity
import com.example.data.NotificationEntity
import com.example.data.PropertyEntity
import com.example.data.StoreEntity
import com.example.data.isValid
import com.example.domain.usecases.ValidatePhoneUseCase
import com.example.utils.DateFormatter
import com.example.utils.FirebaseStorageUploader
import com.example.utils.SecureHasher
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.launch
import java.util.UUID

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

    fun saveStore(store: StoreEntity) {
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
            try {
                val authEmail = vm.resolveAuthEmailForPhone(cleanPhone)
                vm.firebaseAuth.createUserWithEmailAndPassword(authEmail, rawPassword)
                    .addOnFailureListener { /* account exists */ }
            } catch (_: Exception) {
            }
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

    fun deleteStore(storeId: String) {
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

    fun restoreStore(storeId: String) {
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

    fun deleteStorePermanently(storeId: String) {
        vm._stores.value = vm._stores.value.filter { it.id != storeId }
        vm.db.collection("stores").document(storeId).delete()
            .addOnSuccessListener {
                vm.mainViewModel.triggerNotification("🗑️ تم حذف المتجر نهائياً من النظام")
            }
    }

    fun setStoreActive(storeId: String, isActive: Boolean) {
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

    fun setStorePinned(storeId: String, isPinned: Boolean) {
        vm._stores.value = vm._stores.value.map {
            if (it.id == storeId) it.copy(isPinned = isPinned) else it
        }
        val updates = mapOf(
            "isPinned" to isPinned,
            "pinned" to isPinned
        )
        vm.db.collection("stores").document(storeId).update(updates)
            .addOnSuccessListener {
                vm.mainViewModel.triggerNotification(
                    if (isPinned) "📌 تم تثبيت المتجر في الشاشة الرئيسية" else "📌 تم إلغاء تثبيت المتجر"
                )
            }
    }

    fun setStoreVip(storeId: String, isVip: Boolean) {
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

    fun setStoreVerified(storeId: String, isVerified: Boolean) {
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

    fun setStoreRecommended(storeId: String, isRecommended: Boolean) {
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

    fun setStoreBlocked(storeId: String, isBlocked: Boolean, reason: String = "") {
        vm._stores.value = vm._stores.value.map {
            if (it.id == storeId) it.copy(isBlocked = isBlocked, blockReason = reason) else it
        }
        val updates = mapOf(
            "isBlocked" to isBlocked,
            "blockReason" to reason
        )
        vm.db.collection("stores").document(storeId).update(updates)
            .addOnSuccessListener {
                vm.mainViewModel.triggerNotification(
                    if (isBlocked) "🚫 تم حظر المحل/المركز بنجاح ($reason)" else "✅ تم إلغاء حظر المحل/المركز"
                )
            }
    }

    fun setStoreChatDisabled(storeId: String, isDisabled: Boolean) {
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

    fun setStoreNotificationsDisabled(storeId: String, isDisabled: Boolean) {
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

    fun setStorePaymentEnabled(storeId: String, isEnabled: Boolean) {
        vm.db.collection("stores").document(storeId).update("paymentEnabled", isEnabled)
            .addOnSuccessListener {
                vm.mainViewModel.triggerNotification(
                    if (isEnabled) "💳 تم تفعيل نظام الدفع والمحفظة للمتجر" else "🔒 تم تعطيل نظام الدفع للمتجر"
                )
            }
    }

    fun toggleStoreBlocked(storeId: String, isBlocked: Boolean) {
        vm.db.collection("stores").document(storeId).update("isBlocked", isBlocked)
            .addOnSuccessListener {
                vm.mainViewModel.triggerNotification(
                    if (isBlocked) "🚫 تم حظر المتجر" else "✅ تم إلغاء حظر المتجر"
                )
            }
    }

    fun toggleStoreActive(storeId: String) {
        val store = vm._stores.value.find { it.id == storeId }
        if (store != null) {
            val updated = store.copy(isActive = !store.isActive)
            saveStore(updated)
            vm.mainViewModel.triggerNotification(
                if (!updated.isActive) "🔒 تم حظر/إيقاف المحل/المركز مؤقتاً" else "🟢 تم تفعيل المحل/المركز"
            )
        }
    }

    fun toggleStorePinned(storeId: String) {
        val store = vm._stores.value.find { it.id == storeId }
        if (store != null) {
            val updated = store.copy(isPinned = !store.isPinned)
            saveStore(updated)
            vm.mainViewModel.triggerNotification(
                if (updated.isPinned) "📌 تم تثبيت المحل في البداية" else "🔓 تم إلغاء تثبيت المحل"
            )
        }
    }

    fun toggleStoreChatDisabled(storeId: String) {
        val store = vm._stores.value.find { it.id == storeId }
        if (store != null) {
            val updated = store.copy(isChatDisabled = !store.isChatDisabled)
            saveStore(updated)
            vm.mainViewModel.triggerNotification(
                if (updated.isChatDisabled) "🚫 تم إيقاف المحادثات للمحل/المركز" else "💬 تم تفعيل المحادثات للمحل/المركز"
            )
        }
    }

    fun approveStorePdf(storeId: String, approve: Boolean) {
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

    // ==========================================
    // 🏡 العقارات والأراضي (Properties)
    // ==========================================

    fun saveProperty(property: PropertyEntity) {
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
            try {
                val authEmail = vm.resolveAuthEmailForPhone(cleanPhone)
                vm.firebaseAuth.createUserWithEmailAndPassword(authEmail, rawPassword)
                    .addOnFailureListener { /* account exists */ }
            } catch (_: Exception) {
            }
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

    fun deleteProperty(propertyId: String) {
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

    fun restoreProperty(propertyId: String) {
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

    fun deletePropertyPermanently(propertyId: String) {
        vm._properties.value = vm._properties.value.filter { it.id != propertyId }
        vm.db.collection("properties").document(propertyId).delete()
            .addOnSuccessListener {
                vm.mainViewModel.triggerNotification("🗑️ تم حذف العقار نهائياً من النظام")
            }
    }

    fun setPropertyActive(propertyId: String, isActive: Boolean) {
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

    fun setPropertyPinned(propertyId: String, isPinned: Boolean) {
        vm._properties.value = vm._properties.value.map {
            if (it.id == propertyId) it.copy(isPinned = isPinned) else it
        }
        val updates = mapOf(
            "isPinned" to isPinned,
            "pinned" to isPinned
        )
        vm.db.collection("properties").document(propertyId).update(updates)
            .addOnSuccessListener {
                vm.mainViewModel.triggerNotification(
                    if (isPinned) "📌 تم تمييز وتثبيت العقار في الصدارة" else "📌 تم إلغاء تثبيت العقار"
                )
            }
    }

    fun setPropertyVip(propertyId: String, isVip: Boolean) {
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

    fun setPropertyVerified(propertyId: String, isVerified: Boolean) {
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

    fun setPropertyRecommended(propertyId: String, isRecommended: Boolean) {
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

    fun setPropertyBlocked(propertyId: String, isBlocked: Boolean, reason: String = "") {
        vm._properties.value = vm._properties.value.map {
            if (it.id == propertyId) it.copy(isBlocked = isBlocked, blockReason = reason) else it
        }
        val updates = mapOf(
            "isBlocked" to isBlocked,
            "blockReason" to reason
        )
        vm.db.collection("properties").document(propertyId).update(updates)
            .addOnSuccessListener {
                vm.mainViewModel.triggerNotification(
                    if (isBlocked) "🚫 تم حظر إعلان العقار بنجاح ($reason)" else "✅ تم إلغاء حظر إعلان العقار"
                )
            }
    }

    fun setPropertyChatDisabled(propertyId: String, isDisabled: Boolean) {
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

    fun setPropertyNotificationsDisabled(propertyId: String, isDisabled: Boolean) {
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

    fun setPropertyPaymentEnabled(propertyId: String, isEnabled: Boolean) {
        vm.db.collection("properties").document(propertyId).update("paymentEnabled", isEnabled)
            .addOnSuccessListener {
                vm.mainViewModel.triggerNotification(
                    if (isEnabled) "💳 تم تفعيل نظام الدفع للعقار" else "🔒 تم تعطيل نظام الدفع للعقار"
                )
            }
    }

    fun togglePropertyBlocked(propertyId: String, isBlocked: Boolean) {
        vm.db.collection("properties").document(propertyId).update("isBlocked", isBlocked)
            .addOnSuccessListener {
                vm.mainViewModel.triggerNotification(
                    if (isBlocked) "🚫 تم حظر العقار" else "✅ تم إلغاء حظر العقار"
                )
            }
    }

    fun approvePropertyPdf(propertyId: String, approve: Boolean) {
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

    // ==========================================
    // 💼 الوظائف وطلبات التوظيف (Jobs & Applications)
    // ==========================================

    fun saveJob(job: JobEntity) {
        val cleanPhone = job.phone.trim().replace(" ", "").replace("+", "")
        val targetId = if (job.id.isEmpty()) vm.db.collection("jobs").document().id else job.id
        val finalJob = job.copy(id = targetId, phone = cleanPhone)

        val updatedJobs = vm._jobs.value.filter { it.id != targetId }.toMutableList()
        updatedJobs.add(finalJob)
        vm._jobs.value = updatedJobs

        vm.db.collection("jobs").document(targetId).set(finalJob)
            .addOnSuccessListener {
                vm.mainViewModel.triggerNotification(
                    if (finalJob.isApproved) "✅ تم حفظ ونشر الإعلان الوظيفي بنجاح!" else "📨 تم تقديم إعلان الوظيفة للمراجع من قبل الأدمن!"
                )
            }
            .addOnFailureListener {
                vm.mainViewModel.triggerNotification("⚠️ تم إدراج الوظيفة محلياً وفي انتظار المزامنة")
            }
    }

    fun deleteJob(jobId: String) {
        vm._jobs.value = vm._jobs.value.map {
            if (it.id == jobId) it.copy(isDeleted = true) else it
        }
        val updates = hashMapOf<String, Any?>(
            "isDeleted" to true,
            "deletedAt" to System.currentTimeMillis()
        )
        vm.db.collection("jobs").document(jobId).update(updates)
            .addOnSuccessListener {
                vm.mainViewModel.triggerNotification("🗑️ تم نقل الإعلان الوظيفي لسلة المحذوفات")
            }
    }

    fun restoreJob(jobId: String) {
        vm._jobs.value = vm._jobs.value.map {
            if (it.id == jobId) it.copy(isDeleted = false) else it
        }
        val updates = hashMapOf<String, Any?>(
            "isDeleted" to false,
            "deletedAt" to null
        )
        vm.db.collection("jobs").document(jobId).update(updates)
            .addOnSuccessListener {
                vm.mainViewModel.triggerNotification("♻️ تم استعادة الإعلان الوظيفي بنجاح")
            }
    }

    fun deleteJobPermanently(jobId: String) {
        vm.db.collection("jobs").document(jobId).delete()
            .addOnSuccessListener {
                vm.mainViewModel.triggerNotification("🗑️ تم حذف الإعلان الوظيفي نهائياً من النظام")
            }
    }

    fun setJobApproved(jobId: String, isApproved: Boolean) {
        vm._jobs.value = vm._jobs.value.map {
            if (it.id == jobId) it.copy(isApproved = isApproved, isActive = isApproved) else it
        }
        val updates = mapOf(
            "isApproved" to isApproved,
            "isActive" to isApproved
        )
        val targetJob = vm._jobs.value.find { it.id == jobId }
        if (targetJob != null && isApproved) {
            val notification = NotificationEntity(
                id = UUID.randomUUID().toString(),
                title = "🎉 تم قبول ونشر إعلان وظيفتك!",
                message = "تهانينا! تم قبول ونشر إعلان الوظيفة (${targetJob.title}) للجميع!",
                targetType = "USER",
                targetValue = targetJob.phone,
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
        vm.db.collection("jobs").document(jobId).update(updates)
            .addOnSuccessListener {
                vm.mainViewModel.triggerNotification(
                    if (isApproved) "✅ تم قبول ونشر إعلان الوظيفة بنجاح!" else "❌ تم رفض إعلان الوظيفة"
                )
            }
        vm.db.collection("pending_providers").document(jobId).delete()
        vm.db.collection("join_requests").document(jobId).update(
            mapOf(
                "status" to if (isApproved) "APPROVED" else "REJECTED",
                "approvalStatus" to if (isApproved) "APPROVED" else "REJECTED"
            )
        )
        vm._pendingProviders.value = vm._pendingProviders.value.filter { it.id != jobId }
    }

    fun setJobBlocked(jobId: String, isBlocked: Boolean, reason: String = "") {
        val updates = mapOf(
            "isBlocked" to isBlocked,
            "blockReason" to reason
        )
        vm.db.collection("jobs").document(jobId).update(updates)
            .addOnSuccessListener {
                vm.mainViewModel.triggerNotification(
                    if (isBlocked) "🚫 تم حظر الإعلان الوظيفي ($reason)" else "✅ تم إلغاء حظر الإعلان الوظيفي"
                )
            }
    }

    fun setJobPinned(jobId: String, isPinned: Boolean) {
        vm.db.collection("jobs").document(jobId).update("isPinned", isPinned)
            .addOnSuccessListener {
                vm.mainViewModel.triggerNotification(
                    if (isPinned) "📌 تم تثبيت الوظيفة في الصدارة" else "📌 تم إلغاء تثبيت الوظيفة"
                )
            }
    }

    fun setJobVip(jobId: String, isVip: Boolean) {
        vm.db.collection("jobs").document(jobId).update("isVip", isVip)
            .addOnSuccessListener {
                vm.mainViewModel.triggerNotification(
                    if (isVip) "🏆 تم تمييز الإعلان الوظيفي كـ VIP" else "🔒 تم إلغاء شارة VIP عن الوظيفة"
                )
            }
    }

    fun setJobChatDisabled(jobId: String, isDisabled: Boolean) {
        vm.db.collection("jobs").document(jobId).update("isChatDisabled", isDisabled)
            .addOnSuccessListener {
                vm.mainViewModel.triggerNotification(
                    if (isDisabled) "🔇 تم إيقاف الدردشة للإعلان الوظيفي" else "💬 تم تفعيل الدردشة للإعلان الوظيفي"
                )
            }
    }

    fun submitJobApplication(application: JobApplicationEntity) {
        val targetId = vm.db.collection("job_applications").document().id
        val finalApp = application.copy(id = targetId)
        vm._jobApplications.value = vm._jobApplications.value + finalApp
        vm.db.collection("job_applications").document(targetId).set(finalApp)
            .addOnSuccessListener {
                vm.mainViewModel.triggerNotification("📨 تم إرسال طلب التقديم على الوظيفة بنجاح!")
            }
            .addOnFailureListener {
                vm.mainViewModel.triggerNotification("❌ فشل تقديم الطلب: ${it.message}")
            }
    }

    fun updateJobApplicationStatus(appId: String, status: String, reason: String = "") {
        vm._jobApplications.value = vm._jobApplications.value.map {
            if (it.id == appId) it.copy(status = status, rejectionReason = reason) else it
        }
        val updates = mutableMapOf<String, Any>("status" to status)
        if (reason.isNotEmpty()) {
            updates["rejectionReason"] = reason
        }
        vm.db.collection("job_applications").document(appId).update(updates)
            .addOnSuccessListener {
                vm.mainViewModel.triggerNotification("✅ تم تحديث حالة طلب التقديم إلى: $status")
            }
            .addOnFailureListener {
                vm.db.collection("job_applications").document(appId).set(updates, SetOptions.merge())
            }
    }

    fun acceptJobApplication(appId: String) {
        updateJobApplicationStatus(appId, "ACCEPTED")
    }

    fun rejectJobApplication(appId: String, reason: String) {
        updateJobApplicationStatus(appId, "REJECTED", reason)
    }

    fun deleteJobApplication(appId: String) {
        vm._jobApplications.value = vm._jobApplications.value.filter { it.id != appId }
        vm.db.collection("job_applications").document(appId).delete()
            .addOnSuccessListener {
                vm.mainViewModel.triggerNotification("🗑️ تم حذف طلب التقديم من النظام بنجاح")
            }
    }

    fun sendNotificationToApplicants(title: String, message: String, jobId: String = "") {
        val notif = NotificationEntity(
            id = UUID.randomUUID().toString(),
            title = title,
            message = message,
            targetType = if (jobId.isNotEmpty()) "JOB_APPLICANTS" else "ALL",
            targetValue = jobId,
            timestamp = System.currentTimeMillis()
        )
        if (notif.isValid()) {
            vm.db.collection("notifications").document(notif.id).set(notif)
                .addOnSuccessListener {
                    vm.mainViewModel.triggerNotification("🔔 تم إرسال الإشعار لجميع المتقدمين للوظائف بنجاح!")
                }
        }
    }

    fun exportJobApplicantsCsv(context: Context) {
        try {
            val apps = vm._jobApplications.value
            val csvContent = StringBuilder()
            csvContent.append("المعرف,اسم المتقدم,رقم الهاتف,الوظيفة,الشركة,المؤهلات,الحالة,تاريخ التقديم\n")
            apps.forEach { app ->
                val dateStr = DateFormatter.formatCustom(app.createdAt, "yyyy-MM-dd HH:mm")
                csvContent.append(
                    "${app.id},\"${app.applicantName}\",\"${app.applicantPhone}\",\"${app.jobTitle}\",\"${app.companyName}\",\"${app.applicantQuals.replace("\n", " ")}\",\"${app.status}\",\"$dateStr\"\n"
                )
            }
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Job Applicants CSV", csvContent.toString())
            clipboard.setPrimaryClip(clip)
            vm.mainViewModel.triggerNotification("📋 تم نسخ بيانات المتقدمين للوظائف بصيغة CSV إلى الحافظة بنجاح (${apps.size} متقدم)!")
        } catch (e: Exception) {
            vm.mainViewModel.triggerNotification("❌ فشل تصدير البيانات: ${e.message}")
        }
    }

    // ==========================================
    // 🔄 عمليات عابرة للكيانات وتوجيه الحجوزات
    // ==========================================

    fun redirectBookingToEntity(
        bookingId: String,
        targetEntityId: String,
        targetEntityName: String,
        targetPhone: String
    ) {
        val updates = mapOf(
            "providerId" to targetEntityId,
            "providerName" to targetEntityName,
            "providerPhone" to targetPhone,
            "status" to "PENDING"
        )
        vm.db.collection("bookings").document(bookingId).update(updates)
            .addOnSuccessListener {
                vm.mainViewModel.triggerNotification("🔄 تم توجيه الحجز للجهة/الفني ($targetEntityName) بنجاح!")
            }
    }

    fun unbanEntity(entityType: String, entityId: String) {
        when (entityType.uppercase()) {
            "PROVIDER" -> vm.db.collection("providers").document(entityId).update("isBlocked", false)
            "STORE", "RESTAURANT", "MEDICAL" -> setStoreBlocked(entityId, false, "")
            "PROPERTY" -> setPropertyBlocked(entityId, false, "")
            "JOB" -> setJobBlocked(entityId, false, "")
            else -> {
                vm.db.collection("stores").document(entityId).update("isBlocked", false)
                vm.db.collection("properties").document(entityId).update("isBlocked", false)
                vm.db.collection("jobs").document(entityId).update("isBlocked", false)
                vm.db.collection("providers").document(entityId).update("isBlocked", false)
            }
        }
        vm.mainViewModel.triggerNotification("✅ تم إلغاء حظر الكيان بنجاح!")
    }

    fun restoreEntity(entityType: String, entityId: String) {
        when (entityType.uppercase()) {
            "PROVIDER" -> vm.restoreProvider(entityId)
            "STORE", "RESTAURANT", "MEDICAL" -> restoreStore(entityId)
            "PROPERTY" -> restoreProperty(entityId)
            "JOB" -> restoreJob(entityId)
            else -> {
                restoreStore(entityId)
                restoreProperty(entityId)
                restoreJob(entityId)
            }
        }
    }

    fun hardDeleteEntity(entityType: String, entityId: String, reason: String = "") {
        val cleanReason = reason.trim()
        if (cleanReason.isNotBlank()) {
            vm.logAdminActivity("🗑️ حذف نهائي للسجل ($entityType: $entityId) | السبب: $cleanReason")
        }
        when (entityType.uppercase()) {
            "PROVIDER" -> vm.removeProviderPermanently(entityId)
            "STORE", "RESTAURANT", "MEDICAL" -> deleteStorePermanently(entityId)
            "PROPERTY" -> deletePropertyPermanently(entityId)
            "JOB" -> deleteJobPermanently(entityId)
            else -> {
                deleteStorePermanently(entityId)
                deletePropertyPermanently(entityId)
                deleteJobPermanently(entityId)
            }
        }
    }
}
