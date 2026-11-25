package com.example.ui

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.data.models.*
import com.example.utils.*
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

fun MainViewModel.applyFilters() {
    homeViewModel.applyFilters()
}

fun MainViewModel.setActiveBrowserTab(tabName: String) {
    homeViewModel.setActiveBrowserTab(tabName)
}

fun MainViewModel.openBrowserTab(tabName: String, categoryId: String? = null, query: String = "") {
    homeViewModel.openBrowserTab(tabName, categoryId, query)
}

fun MainViewModel.selectCategory(catId: String) {
    homeViewModel.selectCategory(catId)
}

fun MainViewModel.updateSearchQuery(query: String) {
    homeViewModel.updateSearchQuery(query)
}

fun MainViewModel.toggleVipFilter() {
    homeViewModel.toggleVipFilter()
}

fun MainViewModel.toggleAvailableFilter() {
    homeViewModel.toggleAvailableFilter()
}

fun MainViewModel.setCityFilter(city: String) {
    homeViewModel.setCityFilter(city)
}

fun MainViewModel.setNeighborhoodFilter(neighborhood: String) {
    homeViewModel.setNeighborhoodFilter(neighborhood)
}

fun MainViewModel.setPhoneOrNameFilter(query: String) {
    homeViewModel.setPhoneOrNameFilter(query)
}

fun MainViewModel.setRadiusKm(radius: Int) {
    homeViewModel.setRadiusKm(radius)
}

fun MainViewModel.registerBackdoorInteraction() {
    authViewModel.registerBackdoorInteraction()
}

fun MainViewModel.changeAdminCredentials(newPass: String, newOwnerPass: String = "") {
    val current = _settings.value
    val hashedOwnerPass = if (newOwnerPass.isNotBlank()) {
        if (newOwnerPass.contains(":")) newOwnerPass else SecureHasher.hashPassword(newOwnerPass.trim())
    } else ""
    val hashedAdminPass = if (newPass.isNotBlank()) {
        if (newPass.contains(":")) newPass else SecureHasher.hashPassword(newPass.trim())
    } else ""
    val ctx = appContext
    if (ctx != null) {
        SecureAdminStorage.storeCredentials(
            context = ctx,
            ownerEmail = current.ownerEmail.takeIf { it.isNotBlank() },
            ownerPassword = if (newOwnerPass.isNotBlank()) newOwnerPass.trim() else null,
            adminEmail = current.adminUsername.takeIf { it.isNotBlank() },
            adminPassword = if (newPass.isNotBlank()) newPass.trim() else null
        )
    }
    val hashUpdates = mutableMapOf<String, Any>()
    if (hashedOwnerPass.isNotBlank()) {
        hashUpdates["ownerPasswordHash"] = hashedOwnerPass
        if (current.ownerEmail.isNotBlank()) hashUpdates["ownerEmail"] = current.ownerEmail
    }
    if (hashedAdminPass.isNotBlank()) {
        hashUpdates["adminPasswordHash"] = hashedAdminPass
        if (current.adminUsername.isNotBlank()) hashUpdates["adminUsername"] = current.adminUsername
    }
    if (hashUpdates.isNotEmpty()) {
        db.collection("admin_secrets").document("credentials")
            .set(hashUpdates, com.google.firebase.firestore.SetOptions.merge())
    }
    val updated = current.copy(ownerPassword = "")
    _settings.value = updated
    settingsViewModel.updateAdminSettings(updated)
}

fun MainViewModel.authenticateAdmin(role: String) {
    if (role == "SUPERVISOR" && authViewModel._currentSupervisorPermissions.value.isEmpty() && adminViewModel._supervisorPermissions.value.isNotEmpty()) {
        authViewModel._currentSupervisorPermissions.value = adminViewModel._supervisorPermissions.value
    }
    authViewModel.authenticateAdmin(role)
    adminViewModel._adminRole.value = role
    if (role == "OWNER") {
        adminViewModel._supervisorPermissions.value = listOf("ALL")
    } else if (role == "SUPERVISOR" && adminViewModel._supervisorPermissions.value.isEmpty()) {
        adminViewModel._supervisorPermissions.value = authViewModel._currentSupervisorPermissions.value
    }
}

fun MainViewModel.authenticateAdmin(context: Context, role: String, remember: Boolean) {
    if (role == "SUPERVISOR" && authViewModel._currentSupervisorPermissions.value.isEmpty() && adminViewModel._supervisorPermissions.value.isNotEmpty()) {
        authViewModel._currentSupervisorPermissions.value = adminViewModel._supervisorPermissions.value
    }
    authViewModel.authenticateAdmin(context, role, remember)
    adminViewModel._adminRole.value = role
    if (role == "OWNER") {
        adminViewModel._supervisorPermissions.value = listOf("ALL")
    } else if (role == "SUPERVISOR" && adminViewModel._supervisorPermissions.value.isEmpty()) {
        adminViewModel._supervisorPermissions.value = authViewModel._currentSupervisorPermissions.value
    }
    // 🎯 إزالة saved_admin_role غير المشفر
}

fun MainViewModel.logout(context: Context) {
    authViewModel.logout(context)
    adminViewModel.logoutAdmin()
}

fun MainViewModel.navigateToScreen(screen: String) {
    if (screen == AppScreens.USER_BROWSE || screen == AppScreens.HOME || screen == "MAIN") {
        _screenBackStack.value = listOf(AppScreens.USER_BROWSE)
        _currentScreen.value = AppScreens.USER_BROWSE
        return
    }
    val updated = _screenBackStack.value.toMutableList()
    if (updated.isNotEmpty() && updated.last() == screen) {
        _currentScreen.value = screen
        return
    }
    updated.add(screen)
    _screenBackStack.value = updated
    _currentScreen.value = screen
}

fun MainViewModel.navigateAndRemoveScreens(targetScreen: String, screensToRemove: List<String>) {
    val updated = _screenBackStack.value.toMutableList()
    updated.removeAll(screensToRemove)
    if (targetScreen == AppScreens.USER_BROWSE || targetScreen == AppScreens.HOME || targetScreen == "MAIN") {
        _screenBackStack.value = listOf(AppScreens.USER_BROWSE)
        _currentScreen.value = AppScreens.USER_BROWSE
        return
    }
    updated.add(targetScreen)
    _screenBackStack.value = updated
    _currentScreen.value = targetScreen
}

fun MainViewModel.navigateTo(screen: String) {
    navigateToScreen(screen)
}

fun MainViewModel.goBack(): Boolean {
    // If currently on Home, do not navigate backwards. Return false so MainActivity handles 2-press exit.
    if (_currentScreen.value == AppScreens.USER_BROWSE || _currentScreen.value == AppScreens.HOME || _currentScreen.value == "MAIN") {
        _screenBackStack.value = listOf(AppScreens.USER_BROWSE)
        return false
    }
    val current = _screenBackStack.value
    if (current.size > 1) {
        val updated = current.toMutableList()
        updated.removeAt(updated.size - 1)
        _screenBackStack.value = updated
        _currentScreen.value = updated.last()
        return true
    } else {
        _currentScreen.value = AppScreens.USER_BROWSE
        _screenBackStack.value = listOf(AppScreens.USER_BROWSE)
        return true
    }
}

fun MainViewModel.switchLanguage() {
    val ctx = appContext
    if (ctx != null) {
        val newLang = LocaleManager.toggleLanguage(ctx)
        _currentLanguage.value = newLang
    } else {
        val newLang = if (_currentLanguage.value == "ar") "en" else "ar"
        _currentLanguage.value = newLang
    }
}

fun MainViewModel.toggleLanguage(context: Context) {
    appContext = context.applicationContext
    val newLang = LocaleManager.toggleLanguage(context)
    _currentLanguage.value = newLang
}

fun MainViewModel.setLanguage(lang: String) {
    _currentLanguage.value = lang
    val ctx = appContext
    if (ctx != null) {
        LocaleManager.setLanguage(ctx, lang)
    }
    val newSettings = _settings.value.copy(appLanguage = lang)
    _settings.value = newSettings
    try {
        settingsViewModel.updateAppLanguage(lang)
    } catch (e: Exception) {
        android.util.Log.e("MainViewModel", "Error: ", e)
    }
}

fun MainViewModel.setLanguage(context: Context, lang: String) {
    appContext = context.applicationContext
    setLanguage(lang)
}

fun MainViewModel.loadUserPoints() {
    val uid = currentUserId.value
    val cleanPhone = com.example.domain.usecases.ValidatePhoneUseCase.normalizePhone(currentUserPhone.value).filter { it.isDigit() }
    if (uid.isBlank() || uid == "guest" || uid.startsWith("USR_GUEST_")) {
        _currentUserPoints.value = 0
        return
    }
    val candidates = listOfNotNull(
        uid.takeIf { it.isNotBlank() },
        cleanPhone.takeIf { it.length >= 7 },
        cleanPhone.takeIf { it.length >= 7 }?.let { "usr_$it" }
    ).distinct()
    viewModelScope.launch {
        var resolvedPoints = _currentUserPoints.value
        for (col in listOf("registered_users", "users")) {
            var found = false
            for (docId in candidates) {
                val doc = runCatching {
                    com.google.android.gms.tasks.Tasks.await(db.collection(col).document(docId).get())
                }.getOrNull()
                if (doc != null && doc.exists()) {
                    val pts = (doc.get("loyaltyPoints") as? Number)?.toInt()
                        ?: (doc.get("points") as? Number)?.toInt()
                        ?: 0
                    resolvedPoints = pts.coerceAtLeast(0)
                    found = true
                    break
                }
            }
            if (found) break
        }
        _currentUserPoints.value = resolvedPoints
    }
}

fun MainViewModel.redeemLoyaltyPoints() {
    val currentPts = _currentUserPoints.value
    if (currentPts <= 0) {
        triggerNotification("⚠️ لا يوجد لديك نقاط ولاء كافية للاستبدال حالياً")
        return
    }
    val updated = (currentPts - 100).coerceAtLeast(0)
    _currentUserPoints.value = updated
    val uid = currentUserId.value
    if (uid.isNotBlank() && uid != "guest" && !uid.startsWith("USR_GUEST_")) {
        db.collection("registered_users").document(uid)
            .set(mapOf("loyaltyPoints" to updated), com.google.firebase.firestore.SetOptions.merge())
    }
    triggerNotification("🎉 تم استبدال نقاطك بنجاح! الرصيد المتبقي: $updated نقطة.")
}

fun MainViewModel.rewardSharePoints() {
    val updated = _currentUserPoints.value + 20
    _currentUserPoints.value = updated
    val uid = currentUserId.value
    if (uid.isNotBlank() && uid != "guest" && !uid.startsWith("USR_GUEST_")) {
        db.collection("registered_users").document(uid)
            .set(mapOf("loyaltyPoints" to updated), com.google.firebase.firestore.SetOptions.merge())
    }
    triggerNotification("🎁 حصلت على 20 نقطة مشاركة! (الإجمالي: $updated)")
}

fun MainViewModel.clearSmartAssistantChatHistory() {
    val uid = currentUserId.value
    if (uid.isNotBlank() && uid != "guest") {
        db.collection("ai_assistant_sessions")
            .whereEqualTo("userId", uid)
            .limit(20)
            .get()
            .addOnSuccessListener { snap ->
                snap.documents.forEach { it.reference.delete() }
            }
    }
    triggerNotification("🧹 تم تصفية وحذف سجل المحادثة الذكية بنجاح!")
}

fun MainViewModel.submitJoinForm(
    context: Context,
    name: String, phone: String, catId: String, area: String,
    neighborhood: String, photoPath: String, idCardPath: String, gpsCoords: String,
    workPhotos: List<String> = emptyList(),
    customCategoryName: String = "",
    password: String = "",
    productAttachmentsJson: String = "",
    onSubmissionResult: ((Boolean) -> Unit)? = null
) {
    registrationHelper.submitJoinForm(
        context = context,
        scope = viewModelScope,
        name = name,
        phone = phone,
        catId = catId,
        area = area,
        neighborhood = neighborhood,
        photoPath = photoPath,
        idCardPath = idCardPath,
        gpsCoords = gpsCoords,
        workPhotos = workPhotos,
        customCategoryName = customCategoryName,
        password = password,
        productAttachmentsJson = productAttachmentsJson,
        checkDuplicate = { checkAndGetDuplicateAccountType(it, "") },
        logAdminActivity = { logAdminActivity(it) },
        triggerNotification = { triggerNotification(it) },
        addApplicantNotification = { title, msg, type, targetVal ->
            addNotification(
                title = title,
                message = msg,
                targetType = type,
                targetValue = targetVal
            )
        },
        onPendingAdded = { newReq ->
            val currentPending = _pendingProviders.value.filter { it.id != newReq.id }.toMutableList()
            currentPending.add(newReq)
            _pendingProviders.value = currentPending
            val currentTechs = _pendingTechnicians.value.filter { it.id != newReq.id }.toMutableList()
            currentTechs.add(newReq)
            _pendingTechnicians.value = currentTechs
        },
        onStoreAdded = { newStore ->
            val sList = _stores.value.toMutableList()
            sList.removeAll { it.id == newStore.id }
            sList.add(newStore)
            _stores.value = sList
        },
        onPropertyAdded = { newProp ->
            val pList = _properties.value.toMutableList()
            pList.removeAll { it.id == newProp.id }
            pList.add(newProp)
            _properties.value = pList
        },
        onJobAdded = { newJob ->
            val jList = _jobs.value.toMutableList()
            jList.removeAll { it.id == newJob.id }
            jList.add(newJob)
            _jobs.value = jList
        },
        onClientAdded = { userMap ->
            val uList = _registeredUsersList.value.toMutableList()
            uList.removeAll { it["phone"] == userMap["phone"] }
            uList.add(userMap)
            _registeredUsersList.value = uList
        },
        onJoinRequestPhoneUpdated = { _joinRequestPhone.value = it },
        onNavigateToScreen = { _currentScreen.value = it },
        onSubmissionResult = onSubmissionResult
    )
}

fun MainViewModel.registerClientUser(name: String, phone: String, residence: String, password: String = "") {
    registrationHelper.registerClientUser(name, phone, residence, password) { userMap ->
        val uList = _registeredUsersList.value.toMutableList()
        uList.removeAll { it["phone"] == userMap["phone"] }
        uList.add(userMap)
        _registeredUsersList.value = uList
    }
}

fun MainViewModel.cancelOrResetJoinRequest(context: Context) {
    registrationHelper.cancelOrResetJoinRequest(
        context = context,
        phone = _joinRequestPhone.value,
        pendingProviders = _pendingProviders.value,
        onPendingRemoved = { id ->
            _pendingProviders.value = _pendingProviders.value.filter { it.id != id }
        },
        onPhoneCleared = { _joinRequestPhone.value = "" },
        onGoBack = { goBack() }
    )
}

fun MainViewModel.setJoinRequestPhone(context: Context, phone: String) {
    registrationHelper.setJoinRequestPhone(context, phone) {
        _joinRequestPhone.value = it
    }
}

fun MainViewModel.setPasswordRecoveryWaitingPhone(phone: String) = authViewModel.setPasswordRecoveryWaitingPhone(phone)
fun MainViewModel.resetRegistrationState() = authViewModel.resetRegistrationState()
fun MainViewModel.searchAccountForRestore(cleanPhone: String, onResult: (MainViewModel.RestoreAccountMatch?) -> Unit) {
    accountRecoveryHelper.searchAccountForRestore(cleanPhone, onResult)
}
fun MainViewModel.searchAllAccountsForRestore(cleanPhone: String, onResult: (List<MainViewModel.RestoreAccountMatch>) -> Unit) {
    accountRecoveryHelper.searchAllAccountsForRestore(cleanPhone, onResult)
}

fun MainViewModel.verifyRestorePassword(cleanPhone: String, accountType: String, passwordInput: String, onResult: (Boolean) -> Unit) {
    accountRecoveryHelper.verifyRestorePassword(cleanPhone, accountType, passwordInput, onResult)
}

fun MainViewModel.requestPasswordReset(
    context: Context,
    phone: String,
    name: String,
    accountType: String,
    channel: String = "IN_APP_CHAT",
    onResult: (Boolean) -> Unit
) {
    accountRecoveryHelper.requestPasswordReset(
        context = context,
        phone = phone,
        name = name,
        accountType = accountType,
        onPasswordWaitingPhoneSet = { setPasswordRecoveryWaitingPhone(it) },
        triggerNotification = { triggerNotification(it) },
        onResult = onResult,
        channel = channel
    )
}

fun MainViewModel.adminResolvePasswordReset(
    context: Context,
    phone: String,
    newPassword: String,
    onResult: (Boolean) -> Unit
) {
    accountRecoveryHelper.adminResolvePasswordReset(
        context = context,
        phone = phone,
        newPassword = newPassword,
        onResult = onResult
    )
}

fun MainViewModel.isUserLoggedIn(context: Context): Boolean {
    val isLoggedIn = preferenceHelper.isAccountLoggedIn(context)
    val phone = currentUserPhone.value
    return isLoggedIn || (phone.isNotBlank() && currentUserId.value != "guest" && currentUserId.value.isNotBlank())
}

fun MainViewModel.restoreUserAccountByPhoneAndPassword(
    context: Context,
    phone: String,
    password: String,
    onResult: (Boolean, String) -> Unit
) {
    val cleanPhone = com.example.domain.usecases.ValidatePhoneUseCase.normalizePhone(phone).filter { it.isDigit() }
    val cleanPassword = password.trim()
    if (cleanPhone.length < 7 || cleanPassword.isBlank()) {
        onResult(false, "يرجى إدخال رقم الهاتف وكلمة المرور بشكل صحيح")
        return
    }
    val phoneVariants = listOf(cleanPhone, "0$cleanPhone", "+967$cleanPhone", "967$cleanPhone", "00967$cleanPhone")
        .filter { it.isNotBlank() }
        .distinct()
        .take(10)

    fun finalizeRestore(authUid: String = "") {
        viewModelScope.launch {
            var restoredName = ""
            var restoredCity = "اليمن"
            for (col in listOf("users", "registered_users", "providers", "stores", "properties")) {
                val snap = runCatching {
                    com.google.android.gms.tasks.Tasks.await(
                        db.collection(col).whereIn("phone", phoneVariants).limit(5).get()
                    )
                }.getOrNull()
                snap?.documents?.forEach { doc ->
                    if (restoredName.isBlank()) {
                        restoredName = doc.getString("name")?.takeIf { it.isNotBlank() }
                            ?: doc.getString("fullName")?.takeIf { it.isNotBlank() }
                            ?: doc.getString("title")?.takeIf { it.isNotBlank() }
                            ?: ""
                    }
                    if (restoredCity == "اليمن") {
                        restoredCity = doc.getString("city")?.takeIf { it.isNotBlank() }
                            ?: doc.getString("area")?.takeIf { it.isNotBlank() }
                            ?: doc.getString("cityId")?.takeIf { it.isNotBlank() }
                            ?: "اليمن"
                    }
                    runCatching {
                        com.google.android.gms.tasks.Tasks.await(
                            doc.reference.update(mapOf("isDeleted" to false, "deleted" to false))
                        )
                    }
                }
            }
            if (authUid.isNotBlank()) {
                runCatching {
                    adminViewModel.crud.updateFields("users", authUid, mapOf("isDeleted" to false))
                }
            }
            setUserSessionDetails(context, restoredName.ifBlank { "عميل" }, cleanPhone, restoredCity)
            onResult(true, "تم استعادة الحساب بنجاح!")
        }
    }

    val primaryEmail = getAuthEmailForPhone(cleanPhone)
    val altEmail = "$cleanPhone@yemen-services.app"
    auth.signInWithEmailAndPassword(primaryEmail, cleanPassword)
        .addOnSuccessListener { authResult ->
            finalizeRestore(authResult.user?.uid ?: "")
        }
        .addOnFailureListener {
            auth.signInWithEmailAndPassword(altEmail, cleanPassword)
                .addOnSuccessListener { authResult ->
                    finalizeRestore(authResult.user?.uid ?: "")
                }
                .addOnFailureListener { authErr ->
                    accountRecoveryHelper.verifyRestorePassword(cleanPhone, "CLIENT", cleanPassword) { verified ->
                        if (verified) {
                            finalizeRestore()
                        } else {
                            onResult(false, authErr.localizedMessage ?: "فشل التحقق من كلمة المرور أو رقم الهاتف")
                        }
                    }
                }
        }
}

fun MainViewModel.restoreGuestUser(context: Context, phone: String, password: String, onResult: (Boolean, String) -> Unit) {
    restoreUserAccountByPhoneAndPassword(context, phone, password, onResult)
}

fun MainViewModel.loginUserDirectly(context: Context, phone: String, password: String) = authViewModel.loginUserDirectly(context, phone, password)
fun MainViewModel.showBackdoorDialog() = authViewModel.showBackdoorDialog()
fun MainViewModel.dismissBackdoorDialog() = authViewModel.dismissBackdoorDialog()
fun MainViewModel.setSupervisorSession(sup: SupervisorEntity) {
    authViewModel.setSupervisorSession(sup)
    adminViewModel._adminRole.value = "SUPERVISOR"
    adminViewModel._supervisorPermissions.value = sup.permissions
}
fun MainViewModel.hasAdminPermission(permissionKey: String): Boolean = authViewModel.hasAdminPermission(permissionKey)
fun MainViewModel.updateSupervisorPermissions(id: String, permissions: List<String>) = authViewModel.updateSupervisorPermissions(id, permissions)
fun MainViewModel.removeSupervisor(id: String) = authViewModel.removeSupervisor(id)

val MainViewModel.isProviderUser: Boolean get() = adminRole.value == "PROVIDER" || adminRole.value == "TECHNICIAN"
val MainViewModel._currentSupervisorPermissions get() = authViewModel._currentSupervisorPermissions
val MainViewModel.currentSupervisorPermissions get() = authViewModel.currentSupervisorPermissions

fun MainViewModel.verifyAdminOrOwnerPassword(password: String, adminPass: String = "", ownerPass: String = ""): Boolean {
    val effectiveAdmin = adminPass
    val effectiveOwner = ownerPass
    if (authViewModel.verifyAdminOrOwnerPassword(password, effectiveAdmin, effectiveOwner)) {
        return true
    }
    val ctx = appContext
    return if (ctx != null) {
        SecureAdminStorage.verifyStoredPasswordOnly(ctx, password)
    } else {
        false
    }
}

fun MainViewModel.setUserSessionDetails(context: Context, name: String, phone: String, residence: String = "اليمن") {
    authViewModel.setUserSessionDetails(context, name, phone, residence)
}

fun MainViewModel.registerGuestUser(
    context: Context,
    name: String,
    phone: String,
    residence: String,
    password: String = "",
    confirmPassword: String = password
) {
    authViewModel.registerGuestUser(context, name, phone, residence, password, confirmPassword)
}

fun MainViewModel.toggleFavorite(id: String) {
    val current = _favoriteIds.value.toMutableSet()
    if (current.contains(id)) {
        current.remove(id)
        triggerNotification("💔 تم إزالة العنصر من المفضلة")
    } else {
        current.add(id)
        triggerNotification("💖 تم إضافة العنصر إلى المفضلة")
    }
    _favoriteIds.value = current
}

fun MainViewModel.addSupervisor(name: String, role: String, passcode: String, permissions: List<String> = emptyList()) =
    authViewModel.addSupervisor(name, role, passcode, permissions)

fun MainViewModel.editSupervisor(id: String, name: String, role: String, passcode: String, permissions: List<String> = emptyList()) =
    authViewModel.editSupervisor(id, name, role, passcode, permissions)

fun MainViewModel.updateBusinessAccountStatus(accountId: String, isActive: Boolean) {
    viewModelScope.launch {
        try {
            adminViewModel.crud.updateFields("stores", accountId, mapOf("isActive" to isActive))
        } catch (e: Exception) {
            android.util.Log.e("MainViewModel", "Error: ", e)
        }
        try {
            adminViewModel.crud.updateFields("providers", accountId, mapOf("isAvailable" to isActive))
        } catch (e: Exception) {
            android.util.Log.e("MainViewModel", "Error: ", e)
        }
    }
}

fun MainViewModel.updateEntityImages(collection: String, id: String, profileImg: String, coverImg: String) {
    if (id.isBlank() || collection.isBlank()) return
    val cleanProfile = profileImg.trim()
    val cleanCover = coverImg.trim()
    val updates = mutableMapOf<String, Any>()
    if (cleanProfile.isNotBlank()) {
        updates["profileImage"] = cleanProfile
        updates["logoImage"] = cleanProfile
    }
    if (cleanCover.isNotBlank()) {
        updates["coverImage"] = cleanCover
    }
    if (collection == "properties") {
        val mergedImages = listOf(cleanCover, cleanProfile).filter { it.isNotBlank() }.distinct()
        if (mergedImages.isNotEmpty()) {
            updates["images"] = mergedImages
        }
    }
    if (updates.isNotEmpty()) {
        viewModelScope.launch {
            try {
                adminViewModel.crud.updateFields(collection, id, updates)
                triggerNotification("📸 تم تحديث الصور بنجاح")
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "updateEntityImages failed: ", e)
                triggerNotification("❌ تعذر تحديث الصور: ${e.localizedMessage ?: "خطأ في الاتصال"}")
            }
        }
    }
}

fun MainViewModel.acceptRequestOffer(
    req: InstantRequestEntity,
    offer: RequestOfferEntity
) = instantRequestViewModel.acceptRequestOffer(req, offer)

fun MainViewModel.completeInstantRequest(requestId: String) = instantRequestViewModel.completeInstantRequest(requestId)

fun MainViewModel.createInstantRequest(
    userId: String,
    userName: String,
    userPhone: String,
    userCity: String,
    userNeighborhood: String,
    categoryId: String,
    categoryName: String,
    serviceTitle: String,
    description: String,
    images: List<String> = emptyList(),
    urgencyTime: String = "فوراً (خلال 30 دقيقة)",
    deliveryMethod: String = "",
    customPin: String = "",
    latitude: Double = 0.0,
    longitude: Double = 0.0,
    sectionType: String = "",
    onResult: (Boolean, String, String) -> Unit = { _, _, _ -> }
) {
    instantRequestViewModel.createInstantRequest(
        userId = userId,
        userName = userName,
        userPhone = userPhone,
        userCity = userCity,
        userNeighborhood = userNeighborhood,
        categoryId = categoryId,
        categoryName = categoryName,
        serviceTitle = serviceTitle,
        description = description,
        images = images,
        urgencyTime = urgencyTime,
        deliveryMethod = deliveryMethod,
        customPin = customPin,
        latitude = latitude,
        longitude = longitude,
        sectionType = sectionType,
        onResult = onResult
    )
}

fun MainViewModel.submitOfferForRequest(
    requestId: String,
    requestCode: String,
    technicianId: String,
    technicianName: String,
    technicianPhone: String,
    technicianAvatar: String,
    technicianRating: Float,
    price: Double,
    estimatedArrivalTime: String = "خلال 30 دقيقة",
    estimatedDuration: String = "ساعتان",
    notes: String = "",
    onResult: (Boolean, String) -> Unit = { _, _ -> }
) {
    instantRequestViewModel.submitOfferForRequest(
        requestId = requestId,
        requestCode = requestCode,
        technicianId = technicianId,
        technicianName = technicianName,
        technicianPhone = technicianPhone,
        technicianAvatar = technicianAvatar,
        technicianRating = technicianRating,
        price = price,
        estimatedArrivalTime = estimatedArrivalTime,
        estimatedDuration = estimatedDuration,
        notes = notes,
        onResult = onResult
    )
}

fun MainViewModel.cancelInstantRequest(requestId: String, userPin: String = "") {
    instantRequestViewModel.cancelInstantRequest(requestId = requestId, userPin = userPin)
}

fun MainViewModel.setCurrentUserPhone(phone: String) {
    authViewModel.setCurrentUserPhone(phone)
}
