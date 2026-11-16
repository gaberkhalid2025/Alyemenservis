package com.example.ui.viewmodels

import androidx.lifecycle.viewModelScope
import com.example.data.JobEntity
import com.example.data.NotificationEntity
import com.example.data.PendingProviderEntity
import com.example.data.PropertyEntity
import com.example.data.ProviderEntity
import com.example.data.StoreEntity
import com.example.data.isValid
import com.example.data.models.JoinRequestEntity
import com.example.domain.usecases.ValidatePhoneUseCase
import com.example.utils.FirebaseStorageUploader
import com.example.utils.PasswordHasher
import com.example.utils.SecureHasher
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * 📋 AdminRequestsAndUsersDelegate
 * مفوّض مراجعة وقبول ورفض طلبات الانضمام (فنيين، متاجر، مطاعم، مراكز طبية، عقارات، وظائف، عملاء)
 * وإدارة المستخدمين المسجلين وفحص تكرار الحسابات.
 */
internal class AdminRequestsAndUsersDelegate(
    private val vm: AdminViewModel
) {
    fun approveRequest(request: PendingProviderEntity) {
        val lowerArea = request.area.lowercase()
        val finalCityId = when {
            lowerArea.contains("عدن") || lowerArea.contains("aden") -> "ye_ade"
            lowerArea.contains("تعز") || lowerArea.contains("taiz") -> "ye_tai"
            lowerArea.contains("الحديدة") || lowerArea.contains("hodeidah") -> "ye_hod"
            else -> "ye_san"
        }

        val cleanPhone = ValidatePhoneUseCase.normalizePhone(request.phone)
            .ifBlank { request.phone.trim().replace(" ", "").replace("+", "") }
        val resolvedPasswordHash = request.passwordHash.ifBlank { request.password }.let { rawOrHash ->
            val trimmed = rawOrHash.trim()
            if (trimmed.isNotBlank() && !trimmed.contains(":")) {
                PasswordHasher.hash(trimmed)
            } else {
                trimmed
            }
        }

        val clearPendingFromDbAndState = {
            vm.db.collection("pending_providers").document(request.id).delete()
            if (cleanPhone.isNotEmpty()) {
                vm.db.collection("pending_providers").document(cleanPhone).delete()
                vm.db.collection("pending_providers").whereEqualTo("phone", request.phone).get()
                    .addOnSuccessListener { qs ->
                        qs?.documents?.forEach { doc ->
                            vm.db.collection("pending_providers").document(doc.id).delete()
                        }
                    }
            }
            vm._pendingProviders.value = vm._pendingProviders.value.filter {
                it.id != request.id && it.id != cleanPhone && (
                    cleanPhone.isEmpty() || ValidatePhoneUseCase.normalizePhone(it.phone)
                        .ifBlank { it.phone.trim().replace(" ", "").replace("+", "") } != cleanPhone
                    )
            }
        }

        clearPendingFromDbAndState()

        val now = System.currentTimeMillis()
        val joinReqUpdates = mapOf(
            "status" to "APPROVED",
            "approvalStatus" to "APPROVED",
            "isActive" to true,
            "approvedAt" to now,
            "updatedAt" to now
        )
        vm.db.collection("join_requests").document(request.id).update(joinReqUpdates)
        if (cleanPhone.isNotEmpty() && cleanPhone != request.id) {
            vm.db.collection("join_requests").document(cleanPhone).update(joinReqUpdates)
        }

        if (request.profession == "STORE_OWNER" ||
            request.categoryId.uppercase() == "STORE" ||
            request.categoryId.uppercase() == "RESTAURANT" ||
            request.categoryId.uppercase() == "MEDICAL"
        ) {
            val storeId = "store_" + cleanPhone
            val resolvedCategory = when (request.categoryId.uppercase()) {
                "RESTAURANT" -> "مطاعم وكافيهات"
                "MEDICAL" -> "مراكز طبية وعيادات"
                else -> "محلات ومراكز تجارية"
            }
            val resolvedSectionId = when (request.categoryId.uppercase()) {
                "RESTAURANT" -> "restaurants"
                "MEDICAL" -> "medical"
                else -> "stores"
            }
            vm.viewModelScope.launch {
                val resolvedStoreDocUrl = if (request.idPhotoBase64.isNotBlank()) {
                    FirebaseStorageUploader.resolveBase64ToStorageUrl(
                        request.idPhotoBase64,
                        "stores/$storeId/verification_doc.pdf"
                    )
                } else ""
                val newStore = StoreEntity(
                    id = storeId,
                    name = request.name,
                    description = request.specialization.ifBlank { "محل تجاري معتمد وموثق" },
                    ownerId = cleanPhone.ifBlank { request.phone },
                    ownerName = request.name,
                    phone = cleanPhone.ifBlank { request.phone },
                    localNeighborhood = request.localNeighborhood,
                    cityId = finalCityId,
                    sectionId = resolvedSectionId,
                    categoryId = resolvedCategory,
                    isActive = true,
                    isApproved = true,
                    isPinned = false,
                    isDeleted = false,
                    passwordHash = resolvedPasswordHash,
                    pdfFileBase64 = resolvedStoreDocUrl
                )
                if (request.id != storeId) {
                    vm.db.collection("stores").document(request.id).delete()
                }
                if (cleanPhone.isNotEmpty() && cleanPhone != storeId) {
                    vm.db.collection("stores").document(cleanPhone).delete()
                }
                vm.db.collection("stores").document(storeId).set(newStore)

                vm._stores.value = vm._stores.value.filter {
                    it.id != storeId && it.id != request.id && it.phone.trim().replace(" ", "").replace("+", "") != cleanPhone
                } + newStore
            }

            val (approvalTitle, approvalMsg, toastMsg) = when (request.categoryId.uppercase()) {
                "RESTAURANT" -> Triple(
                    "🎉 تهانينا! تم تفعيل مطعمك / كافيهك بنجاح",
                    "مرحباً بك، لقد تم مراجعة وتفعيل مطعمك '${request.name}' بنجاح في دليل المطاعم والكافيهات! يمكنك الآن إدارة قائمتك وحجوزاتك مباشرة من شاشة الانضمام.",
                    "✅ تم تفعيل مطعم ${request.name}"
                )
                "MEDICAL" -> Triple(
                    "🎉 تهانينا! تم تفعيل مركزك الطبي / عيادتك بنجاح",
                    "مرحباً بك، لقد تم مراجعة وتفعيل مركزك الطبي/عيادتك '${request.name}' بنجاح في الدليل الطبي! يمكنك الآن استقبال حجوزات المواعيد وإدارتها مباشرة من شاشة الانضمام.",
                    "✅ تم تفعيل المركز الطبي ${request.name}"
                )
                else -> Triple(
                    "🎉 تهانينا! تم تفعيل متجرك بنجاح",
                    "مرحباً بك يا غالي، لقد تم مراجعة وتفعيل متجرك/محلك '${request.name}' بنجاح في التطبيق! يمكنك الآن إضافة منتجاتك وإدارة متجرك مباشرة من شاشة الانضمام.",
                    "✅ تم تفعيل متجر ${request.name}"
                )
            }

            vm.mainViewModel.addNotification(
                title = approvalTitle,
                message = approvalMsg,
                targetType = "USER",
                targetValue = request.phone
            )
            vm.mainViewModel.triggerNotification(toastMsg)
        } else if (request.profession == "PROPERTY_OWNER" || request.categoryId.uppercase() == "PROPERTY") {
            val propId = "prop_" + cleanPhone
            val propPrice = try {
                request.chatRecipientId.toDouble()
            } catch (e: Exception) {
                0.0
            }
            vm.viewModelScope.launch {
                val resolvedPropDocUrl = if (request.idPhotoBase64.isNotBlank()) {
                    FirebaseStorageUploader.resolveBase64ToStorageUrl(
                        request.idPhotoBase64,
                        "properties/$propId/verification_doc.pdf"
                    )
                } else ""
                val newProp = PropertyEntity(
                    id = propId,
                    title = request.name,
                    description = request.specialization.ifBlank { "عقار معلن وموثق" },
                    phone = cleanPhone.ifBlank { request.phone },
                    ownerId = cleanPhone.ifBlank { request.phone },
                    localNeighborhood = request.localNeighborhood,
                    cityId = finalCityId,
                    isActive = true,
                    isApproved = true,
                    isPinned = false,
                    isDeleted = false,
                    passwordHash = resolvedPasswordHash,
                    price = propPrice,
                    pdfFileBase64 = resolvedPropDocUrl
                )
                if (request.id != propId) {
                    vm.db.collection("properties").document(request.id).delete()
                }
                if (cleanPhone.isNotEmpty() && cleanPhone != propId) {
                    vm.db.collection("properties").document(cleanPhone).delete()
                }
                vm.db.collection("properties").document(propId).set(newProp)

                vm._properties.value = vm._properties.value.filter {
                    it.id != propId && it.id != request.id && it.phone.trim().replace(" ", "").replace("+", "") != cleanPhone
                } + newProp
            }

            vm.mainViewModel.addNotification(
                title = "🎉 تهانينا! تم تفعيل إعلان عقارك بنجاح",
                message = "مرحباً بك، لقد تم مراجعة وتفعيل عقارك '${request.name}' بنجاح في دليل العقارات المعتمد! يمكنك تعديله وإدارته ورؤية تعليقات العملاء مباشرة من شاشة الانضمام.",
                targetType = "USER",
                targetValue = request.phone
            )
            vm.mainViewModel.triggerNotification("✅ تم تفعيل عقار ${request.name}")
        } else if (request.profession == "JOB_POSTER" || request.categoryId.uppercase() == "JOB") {
            val jobId = "job_" + cleanPhone
            val newJob = JobEntity(
                id = jobId,
                title = request.customCategoryName.ifBlank { "وظيفة - " + request.name },
                companyName = request.name,
                phone = cleanPhone.ifBlank { request.phone },
                cityId = finalCityId,
                address = request.localNeighborhood,
                description = request.specialization.ifBlank { "إعلان وظيفة معتمد وموثق" },
                isActive = true,
                isApproved = true
            )
            if (request.id != jobId) {
                vm.db.collection("jobs").document(request.id).delete()
            }
            if (cleanPhone.isNotEmpty() && cleanPhone != jobId) {
                vm.db.collection("jobs").document(cleanPhone).delete()
            }
            vm.db.collection("jobs").document(jobId).set(newJob)

            vm._jobs.value = vm._jobs.value.filter {
                it.id != jobId && it.id != request.id && it.phone.trim().replace(" ", "").replace("+", "") != cleanPhone
            } + newJob

            vm.mainViewModel.addNotification(
                title = "🎉 تهانينا! تم تفعيل إعلان وظيفتك بنجاح",
                message = "مرحباً بك، لقد تم مراجعة ونشر إعلان وظيفتك '${request.name}' بنجاح! يمكنك إدارتها الآن مباشرة من شاشة الانضمام.",
                targetType = "USER",
                targetValue = request.phone
            )
            vm.mainViewModel.triggerNotification("✅ تم تفعيل إعلان وظيفة ${request.name}")
        } else if (request.profession == "JOB_SEEKER" || request.categoryId.uppercase() == "JOB_SEEKER") {
            val seekerMap = mapOf(
                "id" to request.id,
                "name" to request.name,
                "phone" to cleanPhone,
                "profession" to request.customCategoryName.ifBlank { "باحث عن عمل" },
                "city" to request.area,
                "isApproved" to true,
                "status" to "APPROVED",
                "createdAt" to System.currentTimeMillis()
            )
            vm.db.collection("job_seekers").document(request.id).set(seekerMap)
            if (cleanPhone.isNotEmpty() && cleanPhone != request.id) {
                vm.db.collection("job_seekers").document(cleanPhone).set(seekerMap)
            }
            vm.mainViewModel.addNotification(
                title = "🎉 تهانينا! تم اعتماد ملفك كباحث عن عمل",
                message = "مرحباً بك يا غالي، تم اعتماد ملفك في قسم التوظيف والبحث عن عمل بنجاح!",
                targetType = "USER",
                targetValue = request.phone
            )
            vm.mainViewModel.triggerNotification("✅ تم اعتماد ملف المتقدم ${request.name}")
        } else if (request.profession == "CLIENT" || request.categoryId.uppercase() == "CLIENT") {
            val userMap = mutableMapOf<String, Any>(
                "id" to cleanPhone.ifBlank { request.id },
                "name" to request.name,
                "phone" to cleanPhone,
                "residence" to request.area,
                "isApproved" to true,
                "createdAt" to System.currentTimeMillis()
            )
            if (resolvedPasswordHash.isNotBlank()) {
                userMap["passwordHash"] = resolvedPasswordHash
            }
            vm.db.collection("users").document(request.id).set(userMap, SetOptions.merge())
            if (cleanPhone.isNotEmpty() && cleanPhone != request.id) {
                vm.db.collection("users").document(cleanPhone).set(userMap, SetOptions.merge())
            }

            vm.mainViewModel.addNotification(
                title = "🎉 تهانينا! تم تفعيل حسابك بنجاح",
                message = "مرحباً بك يا غالي، تم تفعيل حسابك كعميل مسجل بنجاح!",
                targetType = "USER",
                targetValue = request.phone
            )
            vm.mainViewModel.triggerNotification("✅ تم تفعيل حساب المستخدم ${request.name}")
        } else {
            val providerId = "prov_" + cleanPhone
            val approvedProvider = ProviderEntity(
                id = providerId,
                name = request.name,
                phone = cleanPhone.ifBlank { request.phone },
                categoryId = request.categoryId,
                area = request.area,
                isVip = false,
                subscriptionStatus = "APPROVED",
                isAvailable = true,
                cityId = finalCityId,
                localNeighborhood = request.localNeighborhood,
                rating = 5.0f,
                isBlocked = false,
                customCategoryName = request.customCategoryName,
                passwordHash = resolvedPasswordHash,
                isDeleted = false,
                deletedAt = null
            )
            vm.db.collection("providers").document(approvedProvider.id).set(approvedProvider)

            val currentProviders = vm.mainViewModel.homeViewModel._providers.value.filter {
                it.id != approvedProvider.id && it.phone.trim().replace(" ", "").replace("+", "") != cleanPhone
            }.toMutableList()
            currentProviders.add(approvedProvider)
            vm.mainViewModel.homeViewModel._providers.value = currentProviders
            vm.mainViewModel.applyFilters()

            vm.mainViewModel.addNotification(
                title = "🎉 تهانينا! تم قبول انضمامك كفني معتمد",
                message = "مرحباً بك يا غالي، لقد تم قبول طلب انضمامك كمهني معتمد وأصبحت الآن نشطاً في دليل كل خدمات اليمن! حسابك يظهر الآن لجميع العملاء.",
                targetType = "USER",
                targetValue = request.phone
            )

            vm.mainViewModel.triggerNotification("✅ تم قبول طلب ${request.name}")
        }
    }

    fun rejectRequest(request: PendingProviderEntity, reason: String) {
        val cleanPhone = request.phone.trim().replace(" ", "").replace("+", "")
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
        vm.db.collection("join_requests").document(request.id).update(rejectUpdates)
        if (cleanPhone.isNotEmpty() && cleanPhone != request.id) {
            vm.db.collection("join_requests").document(cleanPhone).update(rejectUpdates)
        }

        vm.db.collection("pending_providers").document(request.id).delete()
        if (cleanPhone.isNotEmpty()) {
            vm.db.collection("pending_providers").document(cleanPhone).delete()
        }
        vm._pendingProviders.value = vm._pendingProviders.value.filter {
            it.id != request.id && it.id != cleanPhone && (cleanPhone.isEmpty() || it.phone.trim().replace(" ", "").replace("+", "") != cleanPhone)
        }

        vm.mainViewModel.addNotification(
            title = "❌ تنويه حول طلب انضمامك",
            message = "للأسف لم يتم قبول طلب انضمامك للأسباب التالية: $finalReason. يمكنك تعديل البيانات وإعادة تقديم الطلب.",
            targetType = "USER",
            targetValue = request.phone
        )

        vm.mainViewModel.triggerNotification("❌ تم رفض طلب ${request.name} بسبب: $finalReason")
    }

    fun approveTechnician(providerId: String) {
        val technician = vm._pendingProviders.value.find { it.id == providerId }
        technician?.let {
            vm._pendingProviders.value = vm._pendingProviders.value.filter { item -> item.id != providerId }
            val lowerArea = it.area.lowercase()
            val finalCityId = when {
                lowerArea.contains("عدن") || lowerArea.contains("aden") -> "ye_ade"
                lowerArea.contains("تعز") || lowerArea.contains("taiz") -> "ye_tai"
                lowerArea.contains("الحديدة") || lowerArea.contains("hodeidah") -> "ye_hod"
                else -> "ye_san"
            }
            val cleanTechPhone = ValidatePhoneUseCase.normalizePhone(it.phone)
                .ifBlank { it.phone.trim().replace(" ", "").replace("+", "") }
            val finalId = "prov_" + cleanTechPhone
            val resolvedTechHash = it.passwordHash.ifBlank { it.password }.let { rawOrHash ->
                val trimmed = rawOrHash.trim()
                if (trimmed.isNotBlank() && !trimmed.contains(":")) {
                    PasswordHasher.hash(trimmed)
                } else {
                    trimmed
                }
            }
            val p = ProviderEntity(
                id = finalId,
                name = it.name,
                phone = cleanTechPhone.ifBlank { it.phone },
                categoryId = it.categoryId,
                area = it.area,
                localNeighborhood = it.localNeighborhood,
                cityId = finalCityId,
                isVerified = true,
                isRecommended = false,
                subscriptionStatus = "APPROVED",
                isVip = false,
                isAvailable = true,
                isBlocked = false,
                rating = 5.0f,
                subscriptionExpiry = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000),
                workPhotosBase64 = it.workPhotosBase64,
                passwordHash = resolvedTechHash,
                isDeleted = false,
                deletedAt = null
            )

            val notification = NotificationEntity(
                id = UUID.randomUUID().toString(),
                title = "🎉 تم قبول طلبك",
                message = "تم قبول طلب انضمامك كـ ${it.name} بنجاح بالدليل اليمني",
                targetType = "USER",
                targetValue = it.phone,
                timestamp = System.currentTimeMillis()
            )
            vm.mainViewModel._notifications.value = listOf(notification) + vm.mainViewModel._notifications.value

            val currentProviders = vm.mainViewModel.homeViewModel._providers.value.filter { item -> item.id != finalId }.toMutableList()
            currentProviders.add(p)
            vm.mainViewModel.homeViewModel._providers.value = currentProviders
            vm.mainViewModel.applyFilters()

            try {
                vm.db.collection("pending_providers").document(providerId).delete()
                vm.db.collection("providers").document(finalId).set(p)
                if (notification.isValid()) vm.db.collection("notifications").document(notification.id).set(notification)
            } catch (e: Exception) {
            }
        }
    }

    fun rejectTechnician(providerId: String, reason: String = "لم يستوفِ الشروط") {
        val technician = vm._pendingProviders.value.find { it.id == providerId }
        technician?.let {
            val updated = it.copy(status = "REJECTED", reason = reason)
            vm._pendingProviders.value = vm._pendingProviders.value.map { item -> if (item.id == providerId) updated else item }

            val notification = NotificationEntity(
                id = UUID.randomUUID().toString(),
                title = "❌ تم رفض طلبك",
                message = "تم رفض طلب انضمامك بسبب: $reason",
                targetType = "USER",
                targetValue = it.phone,
                timestamp = System.currentTimeMillis()
            )
            vm.mainViewModel._notifications.value = listOf(notification) + vm.mainViewModel._notifications.value

            try {
                vm.db.collection("pending_providers").document(providerId).set(updated)
                if (notification.isValid()) vm.db.collection("notifications").document(notification.id).set(notification)
            } catch (e: Exception) {
            }
        }
    }

    fun loadPendingTechnicians() {
        try {
            vm.db.collection("pending_providers")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    if (snapshot != null) {
                        val fetched = snapshot.documents.mapNotNull { doc ->
                            try {
                                val parsed = doc.toObject(PendingProviderEntity::class.java)
                                parsed?.copy(id = doc.id)
                            } catch (e: Exception) {
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
                                        workPhotosBase64 = (doc.get("workPhotosBase64") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                                    )
                                } catch (e2: Exception) {
                                    null
                                }
                            }
                        }
                        vm._pendingTechnicians.value = fetched
                    }
                }
        } catch (e: Exception) {
        }
    }

    fun approvePendingProvider(pending: PendingProviderEntity) {
        vm.db.collection("pending_providers").document(pending.id).delete()
        val cleanPhone = ValidatePhoneUseCase.normalizePhone(pending.phone)
            .ifBlank { pending.phone.trim().replace(" ", "").replace("+", "") }
        val resolvedHash = when {
            pending.passwordHash.isNotBlank() && SecureHasher.isValidHash(pending.passwordHash) -> pending.passwordHash
            pending.passwordHash.isNotBlank() -> SecureHasher.hashPassword(pending.passwordHash.trim())
            else -> ""
        }
        val provider = ProviderEntity(
            id = if (pending.id.isNotBlank()) pending.id else "p_" + cleanPhone.ifBlank { pending.phone },
            name = pending.name,
            phone = cleanPhone.ifBlank { pending.phone },
            categoryId = pending.categoryId,
            area = pending.area,
            localNeighborhood = pending.localNeighborhood,
            subscriptionStatus = "APPROVED",
            isAvailable = true,
            profession = pending.profession,
            specialization = pending.specialization,
            customCategoryName = pending.customCategoryName,
            passwordHash = resolvedHash,
            providerType = pending.providerType
        )
        vm.db.collection("providers").document(provider.id).set(provider)
        vm.mainViewModel.triggerNotification("✅ تم قبول وتعميم الفني: ${pending.name}")
    }

    fun rejectPendingProvider(pending: PendingProviderEntity, @Suppress("UNUSED_PARAMETER") reason: String = "تم رفض الطلب") {
        vm.db.collection("pending_providers").document(pending.id).delete()
        vm.mainViewModel.triggerNotification("❌ تم رفض طلب انضمام: ${pending.name}")
    }

    fun approveRegisteredUser(userId: String, userName: String = "") {
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

    fun toggleBlockRegisteredUser(userId: String, currentBlocked: Boolean, userName: String = "") {
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

    fun deleteRegisteredUser(userId: String, userName: String = "") {
        vm._registeredUsersList.value = vm._registeredUsersList.value.filter { u -> u["id"]?.toString() != userId }
        vm.db.collection("registered_users").document(userId).delete()
            .addOnSuccessListener {
                vm.mainViewModel.triggerNotification("🗑️ تم حذف حساب المستخدم $userName من القاعدة بنجاح.")
            }
        vm.db.collection("users").document(userId).delete()
    }

    fun checkAndGetDuplicateAccountType(phone: String, excludeId: String): String? {
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

    fun loadPendingRequests() {
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
                    } catch (e: Exception) {
                        null
                    }
                }
                vm._pendingRequestsState.value = list
            }
    }

    fun approveProviderRequest(requestId: String, onResult: ((Boolean) -> Unit)? = null) {
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
                    providerType = requestProfession
                )
                approveRequest(pendingProvider)
                loadPendingRequests()
                onResult?.invoke(true)
            } else {
                onResult?.invoke(false)
            }
        }.addOnFailureListener {
            onResult?.invoke(false)
        }
    }

    fun rejectProviderRequest(requestId: String, reason: String = "", onResult: ((Boolean) -> Unit)? = null) {
        vm.db.collection("join_requests").document(requestId).update(
            mapOf(
                "status" to "REJECTED",
                "approvalStatus" to "REJECTED",
                "rejectionReason" to reason
            )
        ).addOnSuccessListener {
            vm.db.collection("pending_providers").document(requestId).delete()
            vm.recordAuditLog("REJECT_REQUEST", "تم رفض طلب الانضمام $requestId. السبب: $reason")
            loadPendingRequests()
            vm.mainViewModel.triggerNotification("🚫 تم رفض طلب الانضمام.")
            onResult?.invoke(true)
        }.addOnFailureListener {
            onResult?.invoke(false)
        }
    }

    fun blockUser(userId: String, onResult: ((Boolean) -> Unit)? = null) {
        vm.db.collection("users").document(userId).update("isBlocked", true)
            .addOnSuccessListener {
                vm.recordAuditLog("BLOCK_USER", "حظر المستخدم $userId")
                vm.mainViewModel.triggerNotification("🚫 تم حظر المستخدم بنجاح.")
                onResult?.invoke(true)
            }
            .addOnFailureListener {
                onResult?.invoke(false)
            }
    }

    fun unblockUser(userId: String, onResult: ((Boolean) -> Unit)? = null) {
        vm.db.collection("users").document(userId).update("isBlocked", false)
            .addOnSuccessListener {
                vm.recordAuditLog("UNBLOCK_USER", "إلغاء حظر المستخدم $userId")
                vm.mainViewModel.triggerNotification("✅ تم إلغاء حظر المستخدم.")
                onResult?.invoke(true)
            }
            .addOnFailureListener {
                onResult?.invoke(false)
            }
    }

    fun deleteUser(userId: String, onResult: ((Boolean) -> Unit)? = null) {
        vm.db.collection("users").document(userId).delete()
            .addOnSuccessListener {
                vm.recordAuditLog("DELETE_USER", "حذف المستخدم $userId")
                vm.mainViewModel.triggerNotification("🗑️ تم حذف حساب المستخدم بنجاح.")
                onResult?.invoke(true)
            }
            .addOnFailureListener {
                onResult?.invoke(false)
            }
    }
}
