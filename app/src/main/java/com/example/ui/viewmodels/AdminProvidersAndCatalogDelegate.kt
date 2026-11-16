package com.example.ui.viewmodels

import androidx.lifecycle.viewModelScope
import com.example.data.CategoryEntity
import com.example.data.CityEntity
import com.example.data.CustomProfileTabEntity
import com.example.data.ProviderEntity
import com.example.utils.FirebaseStorageUploader
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * 🧑‍🔧 AdminProvidersAndCatalogDelegate
 * مفوّض إدارة مقدمي الخدمات (الفنيين)، معرض الأعمال، التصنيفات والأقسام،
 * المدن والمحافظات، والتبويبات المخصصة للملفات.
 */
internal class AdminProvidersAndCatalogDelegate(
    private val vm: AdminViewModel
) {
    // ==========================================
    // 🗂️ الأقسام والتصنيفات (Categories)
    // ==========================================

    fun addNewCategory(
        nameAr: String,
        nameEn: String,
        icon: String,
        description: String,
        parentId: String = "",
        isMainCategory: Boolean = true
    ) {
        vm.mainViewModel.homeViewModel.addNewCategory(
            nameAr,
            nameEn,
            icon,
            description,
            parentId,
            isMainCategory
        )
    }

    fun editCategory(
        categoryId: String,
        newName: String,
        newIcon: String,
        parentId: String = "",
        isMainCategory: Boolean = true
    ) {
        vm.db.collection("categories").document(categoryId).get().addOnSuccessListener { snapshot ->
            val cat = snapshot.toObject(CategoryEntity::class.java)
            if (cat != null) {
                vm.db.collection("categories").document(categoryId).set(
                    cat.copy(
                        name = newName,
                        icon = newIcon,
                        parentId = parentId,
                        isMainCategory = isMainCategory
                    )
                )
            }
        }
        vm.mainViewModel.triggerNotification("✏️ تم تعديل القسم وتحديث هيكلته بنجاح: $newName")
    }

    fun deleteCategory(categoryId: String) {
        vm.mainViewModel.homeViewModel.deleteCategory(categoryId)
    }

    fun togglePinCategory(categoryId: String) {
        vm.db.collection("categories").document(categoryId).get().addOnSuccessListener { snapshot ->
            val cat = snapshot.toObject(CategoryEntity::class.java)
            if (cat != null) {
                val updated = cat.copy(isPinned = !cat.isPinned)
                vm.db.collection("categories").document(categoryId).set(updated)
                vm.mainViewModel.triggerNotification(
                    if (updated.isPinned) "📌 تم تثبيت القسم في البداية" else "🔓 تم إلغاء تثبيت القسم"
                )
            }
        }
    }

    fun mergeCategories(sourceCategoryId: String, targetCategoryId: String) {
        if (sourceCategoryId == targetCategoryId) {
            vm.mainViewModel.triggerNotification("⚠️ لا يمكن دمج القسم مع نفسه!")
            return
        }

        vm.db.collection("providers").whereEqualTo("categoryId", sourceCategoryId).get()
            .addOnSuccessListener { snapshot ->
                for (doc in snapshot.documents) {
                    doc.reference.update("categoryId", targetCategoryId)
                }
            }

        vm.db.collection("pending_providers").whereEqualTo("categoryId", sourceCategoryId).get()
            .addOnSuccessListener { snapshot ->
                for (doc in snapshot.documents) {
                    doc.reference.update("categoryId", targetCategoryId)
                }
            }

        vm.db.collection("stores").whereEqualTo("categoryId", sourceCategoryId).get()
            .addOnSuccessListener { snapshot ->
                for (doc in snapshot.documents) {
                    doc.reference.update("categoryId", targetCategoryId)
                }
            }

        vm.db.collection("categories").document(sourceCategoryId).delete().addOnSuccessListener {
            vm.mainViewModel.triggerNotification("✅ تم دمج القسمين وتحويل كافة الفنيين والمتاجر بنجاح!")
        }
    }

    fun saveCategoryEntity(cat: CategoryEntity) {
        val catId = cat.id.ifEmpty { UUID.randomUUID().toString().take(6) }
        val updated = cat.copy(id = catId)
        vm.db.collection("categories").document(catId).set(updated)
        vm.mainViewModel.triggerNotification("📁 تم حفظ وتحديث بيانات القسم: ${updated.name}")
    }

    fun addSubCategory(parentId: String, nameAr: String, icon: String) {
        val nextId = UUID.randomUUID().toString().take(6)
        val subCat = CategoryEntity(
            id = nextId,
            name = nameAr,
            icon = icon,
            parentId = parentId,
            isMainCategory = false,
            order = vm.mainViewModel.homeViewModel._categories.value.size + 1
        )
        vm.db.collection("categories").document(nextId).set(subCat)
        vm.mainViewModel.triggerNotification("📂 تم إضافة قسم فرعي جديد: $nameAr")
    }

    fun convertCategoryType(catId: String, newParentId: String, isMain: Boolean) {
        val cat = vm.mainViewModel.homeViewModel._categories.value.find { it.id == catId }
        if (cat != null) {
            val updated = cat.copy(parentId = newParentId, isMainCategory = isMain)
            vm.db.collection("categories").document(catId).set(updated)
            vm.mainViewModel.triggerNotification("🔄 تم تغيير تصنيف القسم بنجاح")
        }
    }

    fun reorderCategories(newOrderedList: List<CategoryEntity>) {
        newOrderedList.forEachIndexed { index, cat ->
            val updated = cat.copy(order = index + 1)
            vm.db.collection("categories").document(cat.id).set(updated)
        }
    }

    // ==========================================
    // 🏙️ المدن والمحافظات (Cities)
    // ==========================================

    fun addNewCity(
        nameAr: String,
        nameEn: String,
        icon: String = "📍",
        photoUrl: String = "",
        sortOrder: Int = 0
    ) {
        val nextId = "city_" + UUID.randomUUID().toString().take(4)
        val city = CityEntity(nextId, nameAr, nameEn, icon.ifEmpty { "📍" }, photoUrl, sortOrder)
        vm.db.collection("cities").document(nextId).set(city)
        vm.mainViewModel.triggerNotification("🏙️ تم إضافة مدينة/محافظة: $nameAr")
    }

    fun updateCity(city: CityEntity) {
        if (city.id.isEmpty()) return
        vm.db.collection("cities").document(city.id).set(city)
        vm.mainViewModel.triggerNotification("💾 تم تحديث بيانات المدينة/المحافظة: ${city.nameAr}")
    }

    fun removeCity(cityId: String) {
        vm.db.collection("cities").document(cityId).delete()
        vm.mainViewModel.triggerNotification("🗑️ تم حذف المدينة")
    }

    // ==========================================
    // 📑 تبويبات الملف الشخصي المخصصة
    // ==========================================

    fun saveCustomProfileTab(tab: CustomProfileTabEntity) {
        val targetId = if (tab.id.isEmpty()) UUID.randomUUID().toString().take(6) else tab.id
        val finalTab = tab.copy(id = targetId)
        vm.db.collection("custom_profile_tabs").document(targetId).set(finalTab)
        vm.mainViewModel.triggerNotification("📑 تم حفظ التبويب المخصص بنجاح: ${tab.title}")
    }

    fun deleteCustomProfileTab(tabId: String) {
        vm.db.collection("custom_profile_tabs").document(tabId).delete()
        vm.mainViewModel.triggerNotification("🗑️ تم حذف التبويب المخصص")
    }

    fun toggleCustomProfileTab(tabId: String) {
        val current = vm._customProfileTabs.value.find { it.id == tabId }
        if (current != null) {
            val updated = current.copy(isEnabled = !current.isEnabled)
            vm.db.collection("custom_profile_tabs").document(tabId).set(updated)
            vm.mainViewModel.triggerNotification(
                if (updated.isEnabled) "🟢 تم تفعيل التبويب" else "🔴 تم إيقاف التبويب"
            )
        }
    }

    // ==========================================
    // 🛠️ إدارة الفنيين ومقدمي الخدمات (Providers)
    // ==========================================

    fun removeProvider(providerId: String) {
        vm.viewModelScope.launch {
            vm.runSafeFirestoreCall(
                operation = {
                    vm.crud.deleteEntity("providers", providerId, true)
                },
                onSuccess = {
                    vm.mainViewModel.triggerNotification("🗑️ تم حذف حساب الفني بنجاح")
                },
                onError = {
                    vm.mainViewModel.triggerNotification("⚠️ تم حذف الفني محلياً، سيتم المزامنة تلقائياً")
                }
            )
        }
    }

    fun removeProviderPermanently(providerId: String) {
        vm.viewModelScope.launch {
            vm.runSafeFirestoreCall(
                operation = {
                    vm.crud.deleteEntity("providers", providerId, false)
                },
                onSuccess = {
                    vm.mainViewModel.triggerNotification("🗑️ تم حذف حساب الفني نهائياً وبشكل كامل من خوادم الدليل")
                },
                onError = { e ->
                    vm.mainViewModel.triggerNotification("❌ فشل حذف الفني نهائياً: ${e.message}")
                }
            )
        }
    }

    fun restoreProvider(providerId: String) {
        vm.viewModelScope.launch {
            vm.runSafeFirestoreCall(
                operation = {
                    vm.crud.updateFields(
                        "providers",
                        providerId,
                        mapOf("isDeleted" to false, "deletedAt" to null)
                    )
                },
                onSuccess = {
                    vm.mainViewModel.triggerNotification("🟢 تم استعادة وتفعيل حساب الفني بنجاح!")
                },
                onError = {
                    vm.mainViewModel.triggerNotification("⚠️ تم استعادة الفني محلياً، سيتم المزامنة تلقائياً")
                }
            )
        }
    }

    fun pinProvider(providerId: String, isPinned: Boolean) {
        vm.db.collection("providers").document(providerId).get().addOnSuccessListener { snapshot ->
            val p = snapshot.toObject(ProviderEntity::class.java)
            if (p != null) {
                vm.db.collection("providers").document(providerId).set(p.copy(isVip = isPinned))
            }
        }
        vm.mainViewModel.triggerNotification(if (isPinned) "📌 تم تثبيت الفني" else "📌 تم إلغاء تثبيت الفني")
    }

    fun recommendProvider(providerId: String, isRecommended: Boolean) {
        vm.db.collection("providers").document(providerId).get().addOnSuccessListener { snapshot ->
            val p = snapshot.toObject(ProviderEntity::class.java)
            if (p != null) {
                vm.db.collection("providers").document(providerId).set(
                    p.copy(isRecommended = isRecommended, rating = if (isRecommended) 5.0f else 4.2f)
                )
            }
        }
        vm.mainViewModel.triggerNotification(if (isRecommended) "⭐ تمت توصية الفني" else "⭐ تم إلغاء توصية الفني")
    }

    fun verifyProviderBadge(providerId: String, isVerified: Boolean) {
        vm.db.collection("providers").document(providerId).get().addOnSuccessListener { snapshot ->
            val p = snapshot.toObject(ProviderEntity::class.java)
            if (p != null) {
                vm.db.collection("providers").document(providerId).set(
                    p.copy(
                        isVerified = isVerified,
                        subscriptionStatus = if (isVerified) "APPROVED" else "PENDING"
                    )
                )
            }
        }
        vm.mainViewModel.triggerNotification(
            if (isVerified) "🔷 تم توثيق الفني بالشارة الزرقاء" else "🔷 تم إلغاء توثيق الفني"
        )
    }

    fun toggleProviderSubscription(providerId: String, status: String) {
        vm.db.collection("providers").document(providerId).get().addOnSuccessListener { snapshot ->
            val p = snapshot.toObject(ProviderEntity::class.java)
            if (p != null) {
                vm.db.collection("providers").document(providerId).set(p.copy(subscriptionStatus = status))
            }
        }
        vm.mainViewModel.triggerNotification(
            if (status == "APPROVED") "✨ تم تفعيل العضوية الذهبية للفني" else "✨ تم إلغاء العضوية الذهبية"
        )
    }

    fun toggleProviderSubscription(providerId: String) {
        val provider = vm.mainViewModel.homeViewModel._providers.value.find { it.id == providerId }
        provider?.let {
            val updated = it.copy(
                subscriptionStatus = if (it.subscriptionStatus == "APPROVED") "EXPIRED" else "APPROVED"
            )
            vm.mainViewModel.homeViewModel._providers.value =
                vm.mainViewModel.homeViewModel._providers.value.map { item ->
                    if (item.id == providerId) updated else item
                }
            vm.db.collection("providers").document(providerId).set(updated)
        }
    }

    fun setProviderChatDisabled(providerId: String, disabled: Boolean) {
        vm.db.collection("providers").document(providerId).get().addOnSuccessListener { snapshot ->
            val p = snapshot.toObject(ProviderEntity::class.java)
            if (p != null) {
                vm.db.collection("providers").document(providerId).set(p.copy(isChatDisabled = disabled))
            }
        }
        vm.mainViewModel.triggerNotification(
            if (disabled) "🔇 تم إيقاف دردشة الفني إدارياً" else "🔊 تم تفعيل دردشة الفني"
        )
    }

    fun setProviderNotificationsDisabled(providerId: String, disabled: Boolean) {
        vm.db.collection("providers").document(providerId).get().addOnSuccessListener { snapshot ->
            val p = snapshot.toObject(ProviderEntity::class.java)
            if (p != null) {
                vm.db.collection("providers").document(providerId)
                    .set(p.copy(isNotificationsDisabled = disabled))
            }
        }
        vm.mainViewModel.triggerNotification(
            if (disabled) "🔕 تم تعطيل إشعارات الفني إدارياً" else "🔔 تم تفعيل إشعارات الفني"
        )
    }

    fun setProviderPaymentRequired(providerId: String, required: Boolean) {
        vm.db.collection("providers").document(providerId).get().addOnSuccessListener { snapshot ->
            val p = snapshot.toObject(ProviderEntity::class.java)
            if (p != null) {
                vm.db.collection("providers").document(providerId)
                    .set(p.copy(isPaymentRequired = required))
            }
        }
        vm.mainViewModel.triggerNotification(
            if (required) {
                "💳 تم ربط حساب الفني بنظام الدفع والعمولة الإلزامية"
            } else {
                "🔓 تم استثناء الفني من شروط الدفع المسبق والعمولة"
            }
        )
    }

    fun extendProviderSubscription(providerId: String, extraMs: Long) {
        vm.db.collection("providers").document(providerId).get().addOnSuccessListener { snapshot ->
            val p = snapshot.toObject(ProviderEntity::class.java)
            if (p != null) {
                val currentExpiry = if (p.subscriptionExpiry > System.currentTimeMillis()) {
                    p.subscriptionExpiry
                } else {
                    System.currentTimeMillis()
                }
                vm.db.collection("providers").document(providerId).set(
                    p.copy(subscriptionExpiry = currentExpiry + extraMs, subscriptionStatus = "APPROVED")
                )
            }
        }
        vm.mainViewModel.triggerNotification("💸 تم تجديد وتمديد اشتراك فني بنجاح!")
    }

    fun toggleProviderBlock(providerId: String) {
        vm.viewModelScope.launch {
            val provider = vm.mainViewModel.homeViewModel._providers.value.find { it.id == providerId }
            if (provider != null) {
                val newBlockedStatus = !provider.isBlocked
                vm.runSafeFirestoreCall(
                    operation = {
                        vm.crud.toggleEntityStatus("providers", providerId, "isBlocked", newBlockedStatus)
                        vm.mainViewModel.homeViewModel._providers.value =
                            vm.mainViewModel.homeViewModel._providers.value.map { item ->
                                if (item.id == providerId) item.copy(isBlocked = newBlockedStatus) else item
                            }
                    },
                    onSuccess = {
                        val statusText = if (newBlockedStatus) "حظر" else "إلغاء حظر"
                        vm.mainViewModel.triggerNotification("🛡️ تم $statusText الفني ${provider.name}")
                    },
                    onError = {
                        vm.mainViewModel.triggerNotification("⚠️ تم تحديث الحالة محلياً، سيتم المزامنة تلقائياً")
                    }
                )
            }
        }
    }

    fun toggleProviderStatus(provider: ProviderEntity) {
        val updated = provider.copy(isAvailable = !provider.isAvailable)
        vm.db.collection("providers").document(provider.id).set(updated)
        vm.mainViewModel.triggerNotification("🔄 تم تغيير حالة التوفر لـ ${provider.name}")
    }

    fun toggleProviderPin(providerId: String) {
        val provider = vm.mainViewModel.homeViewModel._providers.value.find { it.id == providerId }
        provider?.let {
            val updated = it.copy(isVip = !it.isVip)
            vm.mainViewModel.homeViewModel._providers.value =
                vm.mainViewModel.homeViewModel._providers.value.map { item ->
                    if (item.id == providerId) updated else item
                }
            vm.db.collection("providers").document(providerId).set(updated)
        }
    }

    fun toggleProviderVerification(providerId: String) {
        val provider = vm.mainViewModel.homeViewModel._providers.value.find { it.id == providerId }
        provider?.let {
            val updated = it.copy(isVerified = !it.isVerified)
            vm.mainViewModel.homeViewModel._providers.value =
                vm.mainViewModel.homeViewModel._providers.value.map { item ->
                    if (item.id == providerId) updated else item
                }
            vm.db.collection("providers").document(providerId).set(updated)
        }
    }

    fun toggleProviderRecommendation(providerId: String) {
        val provider = vm.mainViewModel.homeViewModel._providers.value.find { it.id == providerId }
        provider?.let {
            val updated = it.copy(isRecommended = !it.isRecommended)
            vm.mainViewModel.homeViewModel._providers.value =
                vm.mainViewModel.homeViewModel._providers.value.map { item ->
                    if (item.id == providerId) updated else item
                }
            vm.db.collection("providers").document(providerId).set(updated)
        }
    }

    fun updateProviderEntity(provider: ProviderEntity) {
        vm.mainViewModel.homeViewModel._providers.value =
            vm.mainViewModel.homeViewModel._providers.value.map { item ->
                if (item.id == provider.id) provider else item
            }
        vm.db.collection("providers").document(provider.id).set(provider)
        vm.mainViewModel.triggerNotification("💾 تم تحديث بيانات مقدم الخدمة ${provider.name} بنجاح")
    }

    fun editProviderPhoneAndCategory(providerId: String, newPhone: String, newCategoryId: String) {
        vm.db.collection("providers").document(providerId).get().addOnSuccessListener { snapshot ->
            val p = snapshot.toObject(ProviderEntity::class.java)
            if (p != null) {
                vm.db.collection("providers").document(providerId)
                    .set(p.copy(phone = newPhone, categoryId = newCategoryId))
            }
        }
        vm.mainViewModel.triggerNotification("✏️ تم تعديل بيانات اتصال وتصنيف الفني")
    }

    fun addNewProvider(
        name: String,
        phone: String,
        catId: String,
        area: String,
        price: Double,
        isVip: Boolean
    ) {
        val providerId = "prov_" + phone.trim().replace(" ", "").replace("+", "")
        val newP = ProviderEntity(
            id = providerId,
            name = name,
            phone = phone,
            categoryId = catId,
            area = area,
            isVip = isVip,
            subscriptionStatus = "APPROVED",
            isAvailable = true,
            cityId = "ye_san",
            localNeighborhood = area,
            previewPrice = price,
            rating = 5.0f
        )
        vm.db.collection("providers").document(newP.id).set(newP)
        vm.mainViewModel.triggerNotification("➕ تم إضافة الفني $name يدوياً")
    }

    fun addNewProviderCustom(
        name: String,
        phone: String,
        catId: String,
        street: String,
        cityId: String,
        profileImage: String,
        idCardImage: String,
        @Suppress("UNUSED_PARAMETER") forensicImage: String,
        price: Double,
        isVip: Boolean
    ) {
        val newP = ProviderEntity(
            id = "prov_" + UUID.randomUUID().toString().take(6),
            name = name,
            phone = phone,
            categoryId = catId,
            area = street,
            localNeighborhood = street,
            cityId = cityId,
            profileImage = profileImage.ifEmpty { "https://cdn-icons-png.flaticon.com/512/147/147144.png" },
            coverImage = idCardImage.ifEmpty { "https://img.freepik.com/free-photo/view-of-yemen_1150-12349.jpg" },
            previewPrice = price,
            isVip = isVip,
            isVerified = true,
            isRecommended = true,
            subscriptionStatus = "APPROVED",
            isAvailable = true,
            rating = 5.0f,
            subscriptionExpiry = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000)
        )
        vm.db.collection("providers").document(newP.id).set(newP)
        vm.mainViewModel.triggerNotification("✨ تم إضافة الفني $name يدوياً بالدليل اليمني بنجاح")
    }

    // ==========================================
    // 🖼️ معرض أعمال الفني (Portfolio)
    // ==========================================

    fun updateProviderPortfolio(providerId: String, images: List<String>) {
        try {
            vm.db.collection("providers").document(providerId).update("portfolioImages", images)
        } catch (_: Exception) {
        }
    }

    fun addPortfolioImage(providerId: String, imageBase64: String) {
        vm.viewModelScope.launch {
            try {
                val uploadedUrl = FirebaseStorageUploader.resolveBase64ToStorageUrl(
                    imageBase64,
                    FirebaseStorageUploader.getPortfolioImagePath(providerId),
                    maxSizeBytes = 250 * 1024L
                )
                if (uploadedUrl.isBlank()) return@launch
                vm.db.collection("providers").document(providerId).get().addOnSuccessListener { snapshot ->
                    if (snapshot != null && snapshot.exists()) {
                        @Suppress("UNCHECKED_CAST")
                        val list = snapshot.get("portfolioImages") as? List<String> ?: emptyList()
                        val updated = list + uploadedUrl
                        vm.db.collection("providers").document(providerId).update("portfolioImages", updated)
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    fun removePortfolioImage(providerId: String, index: Int) {
        try {
            vm.db.collection("providers").document(providerId).get().addOnSuccessListener { snapshot ->
                if (snapshot != null && snapshot.exists()) {
                    @Suppress("UNCHECKED_CAST")
                    val list = (snapshot.get("portfolioImages") as? List<String>)?.toMutableList() ?: mutableListOf()
                    if (index < list.size) {
                        list.removeAt(index)
                        vm.db.collection("providers").document(providerId).update("portfolioImages", list)
                    }
                }
            }
        } catch (_: Exception) {
        }
    }

    fun clearPortfolio(providerId: String) {
        try {
            vm.db.collection("providers").document(providerId).update("portfolioImages", emptyList<String>())
        } catch (_: Exception) {
        }
    }
}
