package com.example.ui.viewmodels

import androidx.lifecycle.viewModelScope
import com.example.data.JobEntity
import com.example.data.PendingProviderEntity
import com.example.data.PropertyEntity
import com.example.data.ProviderEntity
import com.example.data.StoreEntity
import com.example.data.repositories.MallRepository
import com.example.utils.FirebaseStorageUploader
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

internal data class ApprovalContext(
    val request: PendingProviderEntity,
    val cleanPhone: String,
    val resolvedCityId: String,
    val resolvedLat: Double,
    val resolvedLng: Double,
    val resolvedWorkingHours: String,
    val decryptedSelfie: String,
    val decryptedIdCard: String,
    val decryptedWorkPhotos: List<String>,
    val resolvedPasswordHash: String
)

internal suspend fun finalizeApprovalCleanupOp(
    vm: AdminViewModel,
    requestId: String,
    cleanPhone: String
) {
    val now = System.currentTimeMillis()
    val joinReqUpdates = mapOf(
        "status" to "APPROVED",
        "approvalStatus" to "APPROVED",
        "isActive" to true,
        "approvedAt" to now,
        "updatedAt" to now
    )
    try {
        vm.db.collection("join_requests").document(requestId).update(joinReqUpdates).await()
    } catch (_: Exception) {
    }
    if (cleanPhone.isNotEmpty() && cleanPhone != requestId) {
        try {
            vm.db.collection("join_requests").document(cleanPhone).update(joinReqUpdates).await()
        } catch (_: Exception) {
        }
    }
    try {
        vm.db.collection("pending_providers").document(requestId).delete().await()
    } catch (_: Exception) {
    }
    if (cleanPhone.isNotEmpty() && cleanPhone != requestId) {
        try {
            vm.db.collection("pending_providers").document(cleanPhone).delete().await()
        } catch (_: Exception) {
        }
    }
    vm._pendingProviders.value = vm._pendingProviders.value.filter { it.id != requestId }
}

internal fun approveStoreOrMallRequest(vm: AdminViewModel, ctx: ApprovalContext) {
    val request = ctx.request
    val cleanPhone = ctx.cleanPhone
    val catUpper = request.categoryId.uppercase()
    val profUpper = request.profession.uppercase()
    val isMallReq = catUpper == "MALL" || profUpper == "MALL_OWNER"
    val storeId = "store_" + cleanPhone
    val resolvedCategory = when {
        isMallReq -> "مراكز تجارية ومولات"
        catUpper == "RESTAURANT" -> "مطاعم وكافيهات"
        catUpper == "MEDICAL" -> "مراكز طبية وعيادات"
        else -> "محلات ومراكز تجارية"
    }
    val resolvedSectionId = when {
        isMallReq -> "malls"
        catUpper == "RESTAURANT" -> "restaurants"
        catUpper == "MEDICAL" -> "medical"
        else -> "stores"
    }
    vm.viewModelScope.launch {
        try {
            val resolvedStoreDocUrl = if (ctx.decryptedIdCard.isNotBlank()) {
                FirebaseStorageUploader.resolveBase64ToStorageUrl(
                    ctx.decryptedIdCard,
                    "stores/$storeId/verification_doc.pdf"
                )
            } else ""
            val resolvedLogoUrl = if (ctx.decryptedSelfie.isNotBlank()) {
                FirebaseStorageUploader.resolveBase64ToStorageUrl(
                    ctx.decryptedSelfie,
                    FirebaseStorageUploader.getStoreLogoPath(storeId)
                )
            } else ""
            val resolvedGallery = ctx.decryptedWorkPhotos.mapIndexed { idx, img ->
                FirebaseStorageUploader.resolveBase64ToStorageUrl(
                    img,
                    FirebaseStorageUploader.getStorePhotoPath(storeId, idx)
                )
            }.filter { it.isNotBlank() }

            val newStore = StoreEntity(
                id = storeId,
                name = request.name,
                description = request.description.ifBlank { request.specialization.ifBlank { "محل تجاري معتمد وموثق" } },
                ownerId = cleanPhone.ifBlank { request.phone },
                ownerName = request.name,
                phone = cleanPhone.ifBlank { request.phone },
                localNeighborhood = request.localNeighborhood,
                cityId = ctx.resolvedCityId,
                sectionId = resolvedSectionId,
                categoryId = resolvedCategory,
                coverImage = resolvedGallery.firstOrNull() ?: resolvedLogoUrl,
                logoImage = resolvedLogoUrl,
                workingHours = ctx.resolvedWorkingHours,
                latitude = ctx.resolvedLat,
                longitude = ctx.resolvedLng,
                images = resolvedGallery,
                isActive = true,
                isApproved = true,
                isVerified = true,
                isPinned = false,
                isDeleted = false,
                passwordHash = ctx.resolvedPasswordHash,
                pdfFileBase64 = resolvedStoreDocUrl,
                productAttachmentsJson = request.productAttachmentsJson
            )
            vm.db.collection("stores").document(storeId).set(newStore).await()
            if (isMallReq) {
                val initialMall = MallRepository.defaultMallFromStore(newStore)
                vm.db.collection("malls").document(storeId).set(initialMall, SetOptions.merge()).await()
            }
            if (request.id != storeId) {
                try { vm.db.collection("stores").document(request.id).delete().await() } catch (_: Exception) {}
            }
            if (cleanPhone.isNotEmpty() && cleanPhone != storeId) {
                try { vm.db.collection("stores").document(cleanPhone).delete().await() } catch (_: Exception) {}
            }

            vm._stores.value = vm._stores.value.filter {
                it.id != storeId && it.id != request.id && it.phone.trim().replace(" ", "").replace("+", "") != cleanPhone
            } + newStore

            finalizeApprovalCleanupOp(vm, request.id, cleanPhone)

            val (approvalTitle, approvalMsg, toastMsg) = when {
                isMallReq -> Triple(
                    "🎉 تهانينا! تم تفعيل المركز التجاري بنجاح",
                    "مرحباً بك، تم اعتماد وتفعيل المركز التجاري '${request.name}' بنجاح! يمكنك الآن إدارة الطوابق ودليل المحلات المستأجرة مباشرة من لوحة تحكم المول.",
                    "✅ تم تفعيل المركز التجاري ${request.name}"
                )
                catUpper == "RESTAURANT" -> Triple(
                    "🎉 تهانينا! تم تفعيل مطعمك / كافيهك بنجاح",
                    "مرحباً بك، لقد تم مراجعة وتفعيل مطعمك '${request.name}' بنجاح في دليل المطاعم والكافيهات! يمكنك الآن إدارة قائمتك وحجوزاتك مباشرة من شاشة الانضمام.",
                    "✅ تم تفعيل مطعم ${request.name}"
                )
                catUpper == "MEDICAL" -> Triple(
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
        } catch (e: Exception) {
            vm.mainViewModel.triggerNotification("❌ تعذر اعتماد طلب ${request.name}: ${e.localizedMessage ?: "يرجى المحاولة مجدداً"}")
        }
    }
}

internal fun approvePropertyRequest(vm: AdminViewModel, ctx: ApprovalContext) {
    val request = ctx.request
    val cleanPhone = ctx.cleanPhone
    val propId = "prop_" + cleanPhone
    val propPrice = try {
        request.chatRecipientId.toDouble()
    } catch (_: Exception) {
        0.0
    }
    vm.viewModelScope.launch {
        try {
            val resolvedPropDocUrl = if (ctx.decryptedIdCard.isNotBlank()) {
                FirebaseStorageUploader.resolveBase64ToStorageUrl(
                    ctx.decryptedIdCard,
                    "properties/$propId/verification_doc.pdf"
                )
            } else ""
            val resolvedMainPhoto = if (ctx.decryptedSelfie.isNotBlank()) {
                FirebaseStorageUploader.resolveBase64ToStorageUrl(
                    ctx.decryptedSelfie,
                    FirebaseStorageUploader.getPropertyPhotoPath(propId, 0)
                )
            } else ""
            val resolvedPropGallery = (listOf(resolvedMainPhoto) + ctx.decryptedWorkPhotos.mapIndexed { idx, img ->
                FirebaseStorageUploader.resolveBase64ToStorageUrl(
                    img,
                    FirebaseStorageUploader.getPropertyPhotoPath(propId, idx + 1)
                )
            }).filter { it.isNotBlank() }.distinct()

            val newProp = PropertyEntity(
                id = propId,
                title = request.name,
                description = request.description.ifBlank { request.specialization.ifBlank { "عقار معلن وموثق" } },
                phone = cleanPhone.ifBlank { request.phone },
                ownerId = cleanPhone.ifBlank { request.phone },
                ownerName = request.name,
                localNeighborhood = request.localNeighborhood,
                cityId = ctx.resolvedCityId,
                latitude = ctx.resolvedLat,
                longitude = ctx.resolvedLng,
                images = resolvedPropGallery,
                isActive = true,
                isApproved = true,
                isVerified = true,
                isPinned = false,
                isDeleted = false,
                passwordHash = ctx.resolvedPasswordHash,
                price = propPrice,
                pdfFileBase64 = resolvedPropDocUrl,
                productAttachmentsJson = request.productAttachmentsJson
            )
            vm.db.collection("properties").document(propId).set(newProp).await()
            if (request.id != propId) {
                try { vm.db.collection("properties").document(request.id).delete().await() } catch (_: Exception) {}
            }
            if (cleanPhone.isNotEmpty() && cleanPhone != propId) {
                try { vm.db.collection("properties").document(cleanPhone).delete().await() } catch (_: Exception) {}
            }

            vm._properties.value = vm._properties.value.filter {
                it.id != propId && it.id != request.id && it.phone.trim().replace(" ", "").replace("+", "") != cleanPhone
            } + newProp

            finalizeApprovalCleanupOp(vm, request.id, cleanPhone)

            vm.mainViewModel.addNotification(
                title = "🎉 تهانينا! تم تفعيل إعلان عقارك بنجاح",
                message = "مرحباً بك، لقد تم مراجعة وتفعيل عقارك '${request.name}' بنجاح في دليل العقارات المعتمد! يمكنك تعديله وإدارته ورؤية تعليقات العملاء مباشرة من شاشة الانضمام.",
                targetType = "USER",
                targetValue = request.phone
            )
            vm.mainViewModel.triggerNotification("✅ تم تفعيل عقار ${request.name}")
        } catch (e: Exception) {
            vm.mainViewModel.triggerNotification("❌ تعذر اعتماد عقار ${request.name}: ${e.localizedMessage ?: "يرجى المحاولة مجدداً"}")
        }
    }
}

internal fun approveJobPosterRequest(vm: AdminViewModel, ctx: ApprovalContext) {
    val request = ctx.request
    val cleanPhone = ctx.cleanPhone
    val jobId = "job_" + cleanPhone
    val newJob = JobEntity(
        id = jobId,
        title = request.customCategoryName.ifBlank { "وظيفة - " + request.name },
        companyName = request.name,
        phone = cleanPhone.ifBlank { request.phone },
        cityId = ctx.resolvedCityId,
        address = request.localNeighborhood,
        description = request.description.ifBlank { request.specialization.ifBlank { "إعلان وظيفة معتمد وموثق" } },
        requirements = request.qualification,
        isActive = true,
        isApproved = true
    )
    vm.viewModelScope.launch {
        try {
            vm.db.collection("jobs").document(jobId).set(newJob).await()
            if (request.id != jobId) {
                try { vm.db.collection("jobs").document(request.id).delete().await() } catch (_: Exception) {}
            }
            if (cleanPhone.isNotEmpty() && cleanPhone != jobId) {
                try { vm.db.collection("jobs").document(cleanPhone).delete().await() } catch (_: Exception) {}
            }

            vm._jobs.value = vm._jobs.value.filter {
                it.id != jobId && it.id != request.id && it.phone.trim().replace(" ", "").replace("+", "") != cleanPhone
            } + newJob

            finalizeApprovalCleanupOp(vm, request.id, cleanPhone)

            vm.mainViewModel.addNotification(
                title = "🎉 تهانينا! تم تفعيل إعلان وظيفتك بنجاح",
                message = "مرحباً بك، لقد تم مراجعة ونشر إعلان وظيفتك '${request.name}' بنجاح! يمكنك إدارتها الآن مباشرة من شاشة الانضمام.",
                targetType = "USER",
                targetValue = request.phone
            )
            vm.mainViewModel.triggerNotification("✅ تم تفعيل إعلان وظيفة ${request.name}")
        } catch (e: Exception) {
            vm.mainViewModel.triggerNotification("❌ تعذر اعتماد إعلان وظيفة ${request.name}: ${e.localizedMessage ?: "يرجى المحاولة مجدداً"}")
        }
    }
}

internal fun approveJobSeekerRequest(vm: AdminViewModel, ctx: ApprovalContext) {
    val request = ctx.request
    val cleanPhone = ctx.cleanPhone
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
    vm.viewModelScope.launch {
        try {
            vm.db.collection("job_seekers").document(request.id).set(seekerMap).await()
            if (cleanPhone.isNotEmpty() && cleanPhone != request.id) {
                vm.db.collection("job_seekers").document(cleanPhone).set(seekerMap).await()
            }
            finalizeApprovalCleanupOp(vm, request.id, cleanPhone)
            vm.mainViewModel.addNotification(
                title = "🎉 تهانينا! تم اعتماد ملفك كباحث عن عمل",
                message = "مرحباً بك يا غالي، تم اعتماد ملفك في قسم التوظيف والبحث عن عمل بنجاح!",
                targetType = "USER",
                targetValue = request.phone
            )
            vm.mainViewModel.triggerNotification("✅ تم اعتماد ملف المتقدم ${request.name}")
        } catch (e: Exception) {
            vm.mainViewModel.triggerNotification("❌ تعذر اعتماد ملف المتقدم ${request.name}: ${e.localizedMessage ?: "يرجى المحاولة مجدداً"}")
        }
    }
}

internal fun approveClientRequest(vm: AdminViewModel, ctx: ApprovalContext) {
    val request = ctx.request
    val cleanPhone = ctx.cleanPhone
    val userMap = mutableMapOf<String, Any>(
        "id" to cleanPhone.ifBlank { request.id },
        "name" to request.name,
        "phone" to cleanPhone,
        "residence" to request.area,
        "isApproved" to true,
        "createdAt" to System.currentTimeMillis()
    )
    if (ctx.resolvedPasswordHash.isNotBlank()) {
        userMap["passwordHash"] = ctx.resolvedPasswordHash
    }
    vm.viewModelScope.launch {
        try {
            vm.db.collection("users").document(request.id).set(userMap, SetOptions.merge()).await()
            if (cleanPhone.isNotEmpty() && cleanPhone != request.id) {
                vm.db.collection("users").document(cleanPhone).set(userMap, SetOptions.merge()).await()
            }
            finalizeApprovalCleanupOp(vm, request.id, cleanPhone)
            vm.mainViewModel.addNotification(
                title = "🎉 تهانينا! تم تفعيل حسابك بنجاح",
                message = "مرحباً بك يا غالي، تم تفعيل حسابك كعميل مسجل بنجاح!",
                targetType = "USER",
                targetValue = request.phone
            )
            vm.mainViewModel.triggerNotification("✅ تم تفعيل حساب المستخدم ${request.name}")
        } catch (e: Exception) {
            vm.mainViewModel.triggerNotification("❌ تعذر تفعيل حساب ${request.name}: ${e.localizedMessage ?: "يرجى المحاولة مجدداً"}")
        }
    }
}

internal fun approveTechnicianProviderRequest(vm: AdminViewModel, ctx: ApprovalContext) {
    val request = ctx.request
    val cleanPhone = ctx.cleanPhone
    val providerId = "prov_" + cleanPhone
    vm.viewModelScope.launch {
        try {
            val resolvedProfileUrl = if (ctx.decryptedSelfie.isNotBlank()) {
                FirebaseStorageUploader.resolveBase64ToStorageUrl(
                    ctx.decryptedSelfie,
                    FirebaseStorageUploader.getProviderProfilePath(providerId)
                )
            } else ""
            val resolvedWorkGallery = ctx.decryptedWorkPhotos.mapIndexed { idx, img ->
                FirebaseStorageUploader.resolveBase64ToStorageUrl(
                    img,
                    FirebaseStorageUploader.getProviderWorkPhotoPath(providerId, idx)
                )
            }.filter { it.isNotBlank() }

            val approvedProvider = ProviderEntity(
                id = providerId,
                name = request.name,
                phone = cleanPhone.ifBlank { request.phone },
                categoryId = request.categoryId,
                area = request.area,
                isVip = false,
                subscriptionStatus = "APPROVED",
                isAvailable = true,
                cityId = ctx.resolvedCityId,
                localNeighborhood = request.localNeighborhood,
                rating = 5.0f,
                coverImage = resolvedWorkGallery.firstOrNull() ?: resolvedProfileUrl,
                profileImage = resolvedProfileUrl,
                latitude = ctx.resolvedLat,
                longitude = ctx.resolvedLng,
                workPhotosBase64 = resolvedWorkGallery,
                productAttachmentsJson = request.productAttachmentsJson,
                specialization = request.specialization.ifBlank { request.description },
                isBlocked = false,
                customCategoryName = request.customCategoryName,
                passwordHash = ctx.resolvedPasswordHash,
                isDeleted = false,
                deletedAt = null
            )
            vm.db.collection("providers").document(approvedProvider.id).set(approvedProvider).await()

            val currentProviders = vm.mainViewModel.homeViewModel._providers.value.filter {
                it.id != approvedProvider.id && it.phone.trim().replace(" ", "").replace("+", "") != cleanPhone
            }.toMutableList()
            currentProviders.add(approvedProvider)
            vm.mainViewModel.homeViewModel._providers.value = currentProviders
            vm.mainViewModel.applyFilters()

            finalizeApprovalCleanupOp(vm, request.id, cleanPhone)

            vm.mainViewModel.addNotification(
                title = "🎉 تهانينا! تم قبول انضمامك كفني معتمد",
                message = "مرحباً بك يا غالي، لقد تم قبول طلب انضمامك كمهني معتمد وأصبحت الآن نشطاً في دليل كل خدمات اليمن! حسابك يظهر الآن لجميع العملاء.",
                targetType = "USER",
                targetValue = request.phone
            )

            vm.mainViewModel.triggerNotification("✅ تم قبول طلب ${request.name}")
        } catch (e: Exception) {
            vm.mainViewModel.triggerNotification("❌ تعذر اعتماد الفني ${request.name}: ${e.localizedMessage ?: "يرجى المحاولة مجدداً"}")
        }
    }
}
