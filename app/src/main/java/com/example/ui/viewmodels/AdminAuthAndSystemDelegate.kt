package com.example.ui.viewmodels

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.example.data.ActivityLogEntity
import com.example.data.CallEntity
import com.example.utils.AdminSecurityManager
import com.example.utils.AdminSession
import com.example.utils.DateFormatter
import com.example.utils.FullSyncManager
import com.example.utils.SecureAdminStorage
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * 🔐 AdminAuthAndSystemDelegate
 * مفوّض مصادقة الإدارة والمالك والمشرفين، جلسات الأمان، الإعدادات الذرية والنسخ الاحتياطي،
 * إحصائيات النظام والأرباح، سجلات التدقيق، ومستمع طلبات استعادة كلمات المرور.
 */
internal class AdminAuthAndSystemDelegate(
    private val vm: AdminViewModel
) {
    private var passwordRecoveryListenerRegistration: ListenerRegistration? = null

    fun initSessionAndRecoveryListener() {
        listenToPasswordRecoveryRequests()
        try {
            val session = vm.secureStorage.getAdminSession()
            if (session != null) {
                val isExpired = System.currentTimeMillis() - session.loginTime > 30L * 24 * 60 * 60 * 1000
                if (!isExpired && session.role in listOf("OWNER", "ADMIN", "SUPERVISOR")) {
                    vm._adminRole.value = session.role
                    val initialPerms = resolveCachedRolePermissions(session.role, session.permissions)
                    vm._supervisorPermissions.value = initialPerms
                    if (session.role == "ADMIN") {
                        refreshAdminMatrixPermissionsFromFirestore()
                    }
                }
            } else {
                val user = FirebaseAuth.getInstance().currentUser
                if (user != null) {
                    user.getIdToken(false).addOnSuccessListener { token ->
                        val role = token?.claims?.get("role") as? String
                        if (role != null && role.uppercase() in listOf("OWNER", "ADMIN", "SUPERVISOR")) {
                            val upper = role.uppercase()
                            vm._adminRole.value = upper
                            vm._supervisorPermissions.value = resolveCachedRolePermissions(upper, emptyList())
                            if (upper == "ADMIN") {
                                refreshAdminMatrixPermissionsFromFirestore()
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {
        }
    }

    private fun resolveCachedRolePermissions(role: String, sessionPerms: List<String>): List<String> {
        val cleanRole = role.trim().uppercase()
        if (cleanRole == "OWNER") return listOf("ALL")
        return try {
            val prefs = vm.application.getSharedPreferences("admin_rbac_cache", Context.MODE_PRIVATE)
            if (prefs.getBoolean("role_configured_$cleanRole", false)) {
                prefs.getStringSet("role_perms_$cleanRole", emptySet())?.toList() ?: emptyList()
            } else if (sessionPerms.isNotEmpty()) {
                sessionPerms
            } else if (cleanRole == "ADMIN") {
                listOf("ALL")
            } else {
                sessionPerms
            }
        } catch (_: Exception) {
            if (cleanRole == "ADMIN" && sessionPerms.isEmpty()) listOf("ALL") else sessionPerms
        }
    }

    private fun refreshAdminMatrixPermissionsFromFirestore() {
        try {
            vm.db.collection("settings").document("admin_permissions_matrix").get()
                .addOnSuccessListener { doc ->
                    if (doc != null && doc.exists() && vm._adminRole.value.equals("ADMIN", ignoreCase = true)) {
                        val rolesRaw = doc.get("roles") as? Map<*, *> ?: return@addOnSuccessListener
                        val adminMap = rolesRaw["ADMIN"] as? Map<*, *> ?: return@addOnSuccessListener
                        val perms = (adminMap["activePermissions"] as? List<*>)
                            ?.mapNotNull { it?.toString() }
                            ?: return@addOnSuccessListener
                        vm._supervisorPermissions.value = perms
                        try {
                            vm.application.getSharedPreferences("admin_rbac_cache", Context.MODE_PRIVATE)
                                .edit()
                                .putStringSet("role_perms_ADMIN", perms.toSet())
                                .putBoolean("role_configured_ADMIN", true)
                                .apply()
                        } catch (_: Exception) {}
                    }
                }
        } catch (_: Exception) {}
    }

    fun cleanup() {
        passwordRecoveryListenerRegistration?.remove()
        passwordRecoveryListenerRegistration = null
    }

    fun loginAdminSecure(
        email: String,
        password: String,
        rememberMe: Boolean = true,
        onResult: (success: Boolean, errorMessage: String?) -> Unit
    ) {
        vm.viewModelScope.launch {
            try {
                vm._isLoadingState.value = true
                val trimmedEmail = email.trim()
                val trimmedPass = password.trim()

                val currentSettings = vm.appState._settings.value
                val supervisorsList = vm.appState._supervisors.value
                val verifiedRole = AdminSecurityManager.verifyCredentials(
                    username = trimmedEmail,
                    passwordAttempt = trimmedPass,
                    settings = currentSettings,
                    context = vm.application,
                    supervisors = supervisorsList,
                    preferredRole = "ADMIN"
                )

                if (verifiedRole == "ADMIN" || verifiedRole == "OWNER") {
                    val assignedRole = verifiedRole
                    val perms = resolveCachedRolePermissions(assignedRole, emptyList())
                    if (assignedRole == "ADMIN") {
                        refreshAdminMatrixPermissionsFromFirestore()
                    }

                    SecureAdminStorage.storeCredentials(
                        context = vm.application,
                        ownerEmail = if (assignedRole == "OWNER") trimmedEmail else null,
                        ownerPassword = if (assignedRole == "OWNER") trimmedPass else null,
                        adminEmail = if (assignedRole == "ADMIN") trimmedEmail else null,
                        adminPassword = if (assignedRole == "ADMIN") trimmedPass else null
                    )

                    if (rememberMe) {
                        vm.secureStorage.saveAdminSession(
                            AdminSession(
                                uid = com.example.utils.EntityIdGenerator.generate("ADM"),
                                email = trimmedEmail,
                                loginTime = System.currentTimeMillis(),
                                refreshToken = "ADMIN_SESSION",
                                role = assignedRole,
                                permissions = perms
                            )
                        )
                    }
                    vm._adminRole.value = assignedRole
                    vm._supervisorPermissions.value = perms
                    vm._isLoadingState.value = false
                    onResult(true, null)
                    return@launch
                }

                vm._isLoadingState.value = false
                onResult(false, "بيانات الدخول غير صحيحة")
            } catch (e: Exception) {
                vm._isLoadingState.value = false
                onResult(false, e.message ?: "فشل تسجيل الدخول")
            }
        }
    }

    fun loginOwnerSecure(
        email: String,
        password: String,
        rememberMe: Boolean = true,
        onResult: (success: Boolean, errorMessage: String?) -> Unit
    ) {
        vm.viewModelScope.launch {
            try {
                vm._isLoadingState.value = true
                val trimmedEmail = email.trim()
                val trimmedPass = password.trim()

                val currentSettings = vm.appState._settings.value
                val supervisorsList = vm.appState._supervisors.value
                val verifiedRole = AdminSecurityManager.verifyCredentials(
                    username = trimmedEmail,
                    passwordAttempt = trimmedPass,
                    settings = currentSettings,
                    context = vm.application,
                    supervisors = supervisorsList,
                    preferredRole = "OWNER"
                )

                if (verifiedRole == "OWNER") {
                    SecureAdminStorage.storeCredentials(
                        context = vm.application,
                        ownerEmail = trimmedEmail,
                        ownerPassword = trimmedPass
                    )

                    if (rememberMe) {
                        vm.secureStorage.saveAdminSession(
                            AdminSession(
                                uid = com.example.utils.EntityIdGenerator.generate("OWN"),
                                email = trimmedEmail,
                                loginTime = System.currentTimeMillis(),
                                refreshToken = "OWNER_SESSION",
                                role = "OWNER",
                                permissions = listOf("ALL")
                            )
                        )
                    }
                    vm._adminRole.value = "OWNER"
                    vm._supervisorPermissions.value = listOf("ALL")
                    vm._isLoadingState.value = false
                    onResult(true, null)
                    return@launch
                }

                vm._isLoadingState.value = false
                onResult(false, "بيانات دخول المالك غير صحيحة")
            } catch (e: Exception) {
                vm._isLoadingState.value = false
                onResult(false, e.message ?: "فشل تسجيل الدخول")
            }
        }
    }

    fun loginSupervisorSecure(
        usernameOrEmail: String,
        passcode: String,
        rememberMe: Boolean = true,
        onResult: (success: Boolean, errorMessage: String?) -> Unit
    ) {
        vm.viewModelScope.launch {
            try {
                vm._isLoadingState.value = true
                val trimmedUser = usernameOrEmail.trim()
                val trimmedPass = passcode.trim()

                val currentSettings = vm.appState._settings.value
                val supervisorsList = vm.appState._supervisors.value
                val verifiedRole = AdminSecurityManager.verifyCredentials(
                    username = trimmedUser,
                    passwordAttempt = trimmedPass,
                    settings = currentSettings,
                    context = vm.application,
                    supervisors = supervisorsList,
                    preferredRole = "SUPERVISOR"
                )

                if (verifiedRole != null && verifiedRole != "GUEST") {
                    val matchingSup = supervisorsList.find {
                        it.id.equals(trimmedUser, ignoreCase = true) ||
                            it.name.trim().equals(trimmedUser, ignoreCase = true)
                    }
                    val perms = matchingSup?.permissions?.ifEmpty {
                        resolveCachedRolePermissions(verifiedRole, listOf("MANAGE_PROVIDERS", "MANAGE_BOOKINGS", "MANAGE_REQUESTS"))
                    } ?: resolveCachedRolePermissions(verifiedRole, emptyList())

                    if (rememberMe) {
                        vm.secureStorage.saveAdminSession(
                            AdminSession(
                                uid = matchingSup?.id ?: com.example.utils.EntityIdGenerator.generate("SUP"),
                                email = trimmedUser,
                                loginTime = System.currentTimeMillis(),
                                refreshToken = "SUPERVISOR_SESSION",
                                role = verifiedRole,
                                permissions = perms
                            )
                        )
                    }

                    vm._adminRole.value = verifiedRole
                    vm._supervisorPermissions.value = perms
                    vm._isLoadingState.value = false
                    onResult(true, null)
                    return@launch
                }

                vm._isLoadingState.value = false
                onResult(false, "بيانات دخول المشرف غير صحيحة")
            } catch (e: Exception) {
                vm._isLoadingState.value = false
                onResult(false, e.message ?: "فشل تسجيل الدخول")
            }
        }
    }

    fun logoutAdmin() {
        vm.viewModelScope.launch {
            try {
                vm.secureStorage.clearAdminSession()
                FirebaseAuth.getInstance().signOut()
                vm._adminRole.value = "GUEST"
                vm._supervisorPermissions.value = emptyList()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun isAdminSessionValid(): Boolean {
        return try {
            val session = vm.secureStorage.getAdminSession() ?: return false

            if (session.role == "OWNER" || session.role == "ADMIN" || session.role == "SUPERVISOR") {
                val user = FirebaseAuth.getInstance().currentUser
                if (user != null) {
                    val tokenResult = user.getIdToken(false).await()
                    val isAdmin = (tokenResult.claims["isAdmin"] as? Boolean == true) ||
                        (tokenResult.claims["isSuperAdmin"] as? Boolean == true)
                    @Suppress("UNUSED_VARIABLE")
                    val _unused = isAdmin
                }
                vm._adminRole.value = session.role
                vm._supervisorPermissions.value = session.permissions
                return true
            }
            false
        } catch (e: Exception) {
            false
        }
    }

    fun saveSettingsAtomically(
        appName: String,
        welcomeMessage: String,
        footerMessage: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        vm.viewModelScope.launch {
            try {
                val settingsRef = vm.db.collection("settings").document("main_settings")
                vm.db.runTransaction { transaction ->
                    val snapshot = transaction.get(settingsRef)
                    val currentVersion = snapshot.getLong("version") ?: 0L
                    val updatedData = mapOf(
                        "appName" to appName,
                        "welcomeMessage" to welcomeMessage,
                        "footerMessage" to footerMessage,
                        "version" to (currentVersion + 1),
                        "updatedAt" to System.currentTimeMillis(),
                        "updatedBy" to FirebaseAuth.getInstance().currentUser?.uid
                    )
                    transaction.set(settingsRef, updatedData, SetOptions.merge())
                    val auditRef = vm.db.collection("audit_logs").document()
                    transaction.set(
                        auditRef,
                        mapOf(
                            "action" to "SETTINGS_UPDATED",
                            "userId" to (FirebaseAuth.getInstance().currentUser?.uid ?: "unknown"),
                            "timestamp" to System.currentTimeMillis(),
                            "version" to (currentVersion + 1)
                        )
                    )
                    currentVersion + 1
                }.await()
                vm.appState._settings.value = vm.appState._settings.value.copy(
                    appName = appName,
                    welcomeMessage = welcomeMessage,
                    footerMessage = footerMessage
                )
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "فشل حفظ الإعدادات سحابياً")
            }
        }
    }

    fun createSettingsBackup(onResult: (Boolean) -> Unit = {}) {
        vm.viewModelScope.launch {
            try {
                val settingsDoc = vm.db.collection("settings")
                    .document("main_settings")
                    .get()
                    .await()
                if (!settingsDoc.exists()) {
                    onResult(false)
                    return@launch
                }
                val settingsData = settingsDoc.data ?: emptyMap<String, Any>()
                val backupData: Map<String, Any?> = mapOf(
                    "timestamp" to System.currentTimeMillis(),
                    "version" to (settingsData["version"] ?: 0L),
                    "data" to settingsData,
                    "createdBy" to FirebaseAuth.getInstance().currentUser?.uid
                )
                vm.db.collection("settings_backups").add(backupData).await()
                onResult(true)
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }

    fun logAdminActivity(action: String) {
        val id = vm.db.collection("activity_logs").document().id
        val log = ActivityLogEntity(id = id, action = action, timestamp = System.currentTimeMillis())
        vm.db.collection("activity_logs").document(id).set(log)
    }

    fun logCall(providerId: String, providerName: String) {
        val callId = UUID.randomUUID().toString()
        val call = CallEntity(
            id = callId,
            providerId = providerId,
            providerName = providerName,
            callerName = "مواطن يمني 🇾🇪",
            timestamp = System.currentTimeMillis()
        )
        vm.db.collection("calls").document(callId).set(call)
    }

    fun loadSystemStats() {
        val totalProvs = vm.appState._providers.value.size
        val usersTotal = maxOf(vm._registeredUsersList.value.size, vm._registeredUsersCount.value)
        val allBookings = vm.appState._bookings.value
        val activeCount = allBookings.count { b ->
            val st = b.status.uppercase()
            st == "PENDING" || st == "ACCEPTED" || st == "APPROVED" || st == "IN_PROGRESS"
        }
        val completedCount = allBookings.count { it.status.equals("COMPLETED", ignoreCase = true) }
        val pendingCount = vm._pendingRequestsState.value.count { it.status.equals("PENDING", ignoreCase = true) } +
            vm._pendingProviders.value.count { it.status.equals("PENDING", ignoreCase = true) }
        vm._systemStatsState.value = SystemStats(
            totalProviders = totalProvs,
            totalUsers = usersTotal,
            activeBookings = activeCount,
            completedOrders = completedCount,
            pendingApprovals = pendingCount
        )
    }

    fun getSystemStats(): SystemStats {
        loadSystemStats()
        return vm._systemStatsState.value
    }

    fun recordAuditLog(action: String, details: String) {
        val entry = AuditLog(action = action, details = details)
        vm._auditLogsState.value = listOf(entry) + vm._auditLogsState.value
        try {
            vm.db.collection("admin_audit_logs").document(entry.id).set(entry)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun exportReport(type: String): String {
        recordAuditLog("EXPORT_REPORT", "تصدير تقرير من نوع $type")
        val dateStr = DateFormatter.formatDateDash(System.currentTimeMillis())
        return "تقرير شامل للـ $type - التاريخ: $dateStr"
    }

    fun syncAllData(context: Context, onComplete: ((Boolean) -> Unit)? = null) {
        vm.viewModelScope.launch {
            try {
                val syncMgr = FullSyncManager(context)
                val res = syncMgr.syncAllSettings()
                onComplete?.invoke(res)
            } catch (e: Exception) {
                onComplete?.invoke(false)
            }
        }
    }

    fun clearCache(context: Context) {
        try {
            FullSyncManager(context).clearLocalCache()
            vm.mainViewModel.triggerNotification("🧹 تم مسح الذاكرة المؤقتة بنجاح")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun periodStartMillis(period: String): Long {
        val now = System.currentTimeMillis()
        val dayMs = 24L * 60L * 60L * 1000L
        return when (period.uppercase()) {
            "TODAY" -> now - dayMs
            "WEEK" -> now - 7L * dayMs
            "MONTH" -> now - 30L * dayMs
            "YEAR" -> now - 365L * dayMs
            else -> 0L
        }
    }

    fun getSystemHealth(): SystemHealth = SystemHealth()
    fun getStorageUsage(): StorageUsage = StorageUsage()

    fun getBookingStats(period: String = "ALL"): BookingStats {
        val now = System.currentTimeMillis()
        val dayMs = 24L * 60L * 60L * 1000L
        val cutoff = periodStartMillis(period)
        val allBookings = vm.appState._bookings.value
        val filtered = if (cutoff == 0L) allBookings else allBookings.filter { it.createdAt == 0L || it.createdAt >= cutoff }
        return BookingStats(
            total = filtered.size,
            pending = filtered.count { it.status.equals("PENDING", ignoreCase = true) },
            inProgress = filtered.count {
                val st = it.status.uppercase()
                st == "ACCEPTED" || st == "APPROVED" || st == "IN_PROGRESS"
            },
            completed = filtered.count { it.status.equals("COMPLETED", ignoreCase = true) },
            cancelled = filtered.count {
                val st = it.status.uppercase()
                st == "CANCELLED" || st == "REJECTED"
            },
            today = allBookings.count { it.createdAt >= now - dayMs },
            thisWeek = allBookings.count { it.createdAt >= now - 7L * dayMs },
            thisMonth = allBookings.count { it.createdAt >= now - 30L * dayMs }
        )
    }

    fun getRevenueStats(period: String = "ALL"): RevenueStats {
        val now = System.currentTimeMillis()
        val dayMs = 24L * 60L * 60L * 1000L
        val cutoff = periodStartMillis(period)
        val completedPayments = vm._payments.value.filter { it.status.equals("COMPLETED", ignoreCase = true) }
        val filteredPayments = if (cutoff == 0L) completedPayments else completedPayments.filter { it.createdAt == 0L || it.createdAt >= cutoff }

        val paidFromPayments = filteredPayments.sumOf { it.amount }
        val paidFromBookings = vm.appState._bookings.value
            .filter {
                val st = it.status.uppercase()
                (st == "COMPLETED" || st == "ACCEPTED" || st == "APPROVED") &&
                    (cutoff == 0L || it.createdAt == 0L || it.createdAt >= cutoff)
            }
            .sumOf { if (it.totalAmount > 0.0) it.totalAmount else it.price }
        val paidFromOrders = vm._orders.value
            .filter {
                it.status.equals("COMPLETED", ignoreCase = true) &&
                    (cutoff == 0L || it.timestamp == 0L || it.timestamp >= cutoff)
            }
            .sumOf { it.totalAmount }

        val totalRev = if (paidFromPayments > 0.0) paidFromPayments else (paidFromBookings + paidFromOrders)
        val commissionSum = filteredPayments.sumOf { it.commission }.takeIf { it > 0.0 } ?: (totalRev * 0.10)
        val payoutsSum = filteredPayments.sumOf { it.providerShare }.takeIf { it > 0.0 } ?: (totalRev - commissionSum).coerceAtLeast(0.0)
        val todayRev = completedPayments.filter { it.createdAt >= now - dayMs }.sumOf { it.amount }
        val monthRev = completedPayments.filter { it.createdAt >= now - 30L * dayMs }.sumOf { it.amount }

        return RevenueStats(
            totalRevenue = totalRev,
            platformCommission = commissionSum,
            providerPayouts = payoutsSum,
            currency = "YER",
            todayRevenue = todayRev,
            monthRevenue = monthRev
        )
    }

    fun getProviderStats(): ProviderStats {
        val provs = vm.appState._providers.value
        val pendingCount = vm._pendingRequestsState.value.count { it.status.equals("PENDING", ignoreCase = true) } +
            vm._pendingProviders.value.count { it.status.equals("PENDING", ignoreCase = true) }
        return ProviderStats(
            totalVerified = provs.count { it.isVerified },
            pendingApproval = pendingCount,
            topRated = provs.count { it.rating >= 4.5f && it.numReviews > 0 },
            suspended = provs.count { !it.isAvailable }
        )
    }

    fun getCategoryStats(): CategoryStats {
        val provs = vm.appState._providers.value
        val storesList = vm._stores.value
        val medicalProvs = provs.count {
            it.categoryId.contains("med", ignoreCase = true) ||
                it.categoryId.contains("health", ignoreCase = true) ||
                it.categoryId.contains("clinic", ignoreCase = true) ||
                it.categoryId.contains("طب")
        }
        val medicalStores = storesList.count { it.sectionId.equals("medical", ignoreCase = true) }
        val restaurantCount = storesList.count {
            it.sectionId.equals("restaurants", ignoreCase = true) ||
                it.categoryId.contains("rest", ignoreCase = true) ||
                it.categoryId.contains("مطعم")
        }
        val retailStoreCount = storesList.count {
            !it.sectionId.equals("restaurants", ignoreCase = true) &&
                !it.sectionId.equals("medical", ignoreCase = true)
        }
        val maintenanceCount = (provs.size - medicalProvs).coerceAtLeast(0)
        return CategoryStats(
            maintenanceCount = maintenanceCount,
            storeCount = retailStoreCount,
            restaurantCount = restaurantCount,
            medicalCount = medicalProvs + medicalStores
        )
    }

    fun getCityStats(): CityStats {
        val cityStrings = buildList {
            vm.appState._providers.value.forEach { add("${it.cityId} ${it.area}") }
            vm._stores.value.forEach { add(it.cityId) }
            vm._properties.value.forEach { add(it.cityId) }
            vm.appState._bookings.value.forEach { add("${it.userCity} ${it.customerArea}") }
        }
        fun countCity(vararg keys: String): Int = cityStrings.count { text ->
            keys.any { k -> text.contains(k, ignoreCase = true) }
        }
        return CityStats(
            sanaaCount = countCity("ye_san", "صنعاء", "sanaa", "أمانة"),
            adenCount = countCity("ye_ade", "عدن", "aden"),
            taizCount = countCity("ye_tai", "تعز", "taiz"),
            ibbCount = countCity("ye_ibb", "إب", "اب", "ibb"),
            mukallaCount = countCity("ye_had", "ye_muk", "حضرموت", "المكلا", "سيئون", "mukalla", "hadramout")
        )
    }

    fun sendAdminNotification(title: String, message: String, target: String) {
        val notif = AdminNotification(
            id = UUID.randomUUID().toString(),
            title = title,
            message = "$message (الهدف: $target)",
            type = "BROADCAST",
            isRead = false,
            timestamp = System.currentTimeMillis()
        )
        vm._adminNotificationsListState.value = listOf(notif) + vm._adminNotificationsListState.value
        recordAuditLog("SEND_NOTIFICATION", "إرسال إشعار: $title إلى $target")
    }

    fun getAuditLogs(filter: String = "ALL"): List<AuditLog> {
        if (filter.isBlank() || filter == "ALL") return vm._auditLogsState.value
        return vm._auditLogsState.value.filter {
            it.action.contains(filter, ignoreCase = true) || it.details.contains(filter, ignoreCase = true)
        }
    }

    fun exportAuditLogs(): String {
        recordAuditLog("EXPORT_AUDIT", "تصدير سجل التدقيق والأمان")
        return buildString {
            appendLine("ID,Admin,Action,Details,Timestamp")
            vm._auditLogsState.value.forEach {
                appendLine("${it.id},${it.adminEmail},${it.action},\"${it.details}\",${it.timestamp}")
            }
        }
    }

    fun getSystemLogs(): List<SystemLog> = vm._systemLogsState.value

    fun clearSystemLogs() {
        vm._systemLogsState.value = emptyList()
    }

    private fun listenToPasswordRecoveryRequests() {
        passwordRecoveryListenerRegistration?.remove()
        passwordRecoveryListenerRegistration = vm.db.collection("password_recovery_requests")
            .addSnapshotListener { snap, err ->
                if (err == null && snap != null) {
                    try {
                        val requests = snap.documents.mapNotNull { doc ->
                            val m = doc.data?.toMutableMap() ?: return@mapNotNull null
                            m["id"] = doc.id
                            m
                        }.sortedByDescending {
                            val rawTs = it["timestamp"] ?: it["requestedAt"] ?: it["createdAt"]
                            when (rawTs) {
                                is Number -> rawTs.toLong()
                                is Timestamp -> rawTs.toDate().time
                                else -> (it["requestedAt"] as? Number)?.toLong()
                                    ?: (it["createdAt"] as? Number)?.toLong()
                                    ?: 0L
                            }
                        }
                        vm._passwordRecoveryRequestsState.value = requests
                    } catch (e: Throwable) {
                        e.printStackTrace()
                    }
                }
            }
    }

    @Suppress("unused")
    private suspend fun attemptEmergencyFallback(
        email: String,
        password: String,
        rememberMe: Boolean
    ): Boolean {
        val secureAdminStorage = SecureAdminStorage

        if (secureAdminStorage.verifyFallbackCredentials(vm.application, email, password, "OWNER")) {
            handleEmergencyFallbackSuccess("OWNER", email, rememberMe)
            return true
        }

        if (secureAdminStorage.verifyFallbackCredentials(vm.application, email, password, "ADMIN")) {
            handleEmergencyFallbackSuccess("ADMIN", email, rememberMe)
            return true
        }

        return false
    }

    private fun handleEmergencyFallbackSuccess(role: String, email: String, rememberMe: Boolean) {
        vm._adminRole.value = role
        if (rememberMe) {
            vm.secureStorage.saveAdminSession(
                AdminSession(
                    uid = "emergency_${role.lowercase()}",
                    email = email,
                    loginTime = System.currentTimeMillis(),
                    refreshToken = "EMERGENCY_SESSION",
                    role = role
                )
            )
        }
    }
}
