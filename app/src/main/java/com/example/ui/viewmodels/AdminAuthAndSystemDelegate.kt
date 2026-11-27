package com.example.ui.viewmodels

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.example.security.AdminPermissionResolver
import com.example.ui.screens.admin.permissions.AdminPermissionsViewModel
import com.example.utils.AdminSecurityManager
import com.example.utils.AdminSession
import com.example.utils.EntityIdGenerator
import com.example.utils.SecureAdminStorage
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
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

    private val fullAccessRoles = setOf("OWNER", "SUPER_ADMIN", "MAIN_ADMIN")
    private val staffRoles = setOf("OWNER", "SUPER_ADMIN", "MAIN_ADMIN", "ADMIN", "SUPERVISOR", "AUDITOR", "SUPPORT", "OPERATIONS")

    @Volatile private var failedLoginCount = 0
    @Volatile private var lockoutUntilMs = 0L

    private fun checkLockoutRemainingSec(): Long {
        val now = System.currentTimeMillis()
        return if (now < lockoutUntilMs) ((lockoutUntilMs - now) / 1000L).coerceAtLeast(1L) else 0L
    }

    private fun recordLoginFailure(): Long {
        failedLoginCount++
        if (failedLoginCount >= 5) {
            val factor = (failedLoginCount - 4).coerceIn(1, 5)
            val delaySec = 30L * factor
            lockoutUntilMs = System.currentTimeMillis() + delaySec * 1000L
            return delaySec
        }
        return 0L
    }

    private fun resetLoginLockout() {
        failedLoginCount = 0
        lockoutUntilMs = 0L
    }

    fun initSessionAndRecoveryListener() {
        try {
            val session = vm.secureStorage.getAdminSession()
            if (session != null) {
                val isExpired = System.currentTimeMillis() - session.loginTime > 30L * 24 * 60 * 60 * 1000
                val cleanRole = session.role.trim().uppercase()
                if (!isExpired && cleanRole in staffRoles) {
                    vm._adminRole.value = cleanRole
                    val initialPerms = resolveCachedRolePermissions(cleanRole, session.permissions)
                    vm._supervisorPermissions.value = initialPerms
                    ensurePasswordRecoveryListenerIfStaff(cleanRole)
                    if (cleanRole !in fullAccessRoles) {
                        refreshAdminMatrixPermissionsFromFirestore(cleanRole, session.permissions)
                    }
                }
            } else {
                val user = FirebaseAuth.getInstance().currentUser
                if (user != null) {
                    user.getIdToken(false).addOnSuccessListener { token ->
                        val role = (token?.claims?.get("role") as? String)?.trim()?.uppercase()
                        if (role != null && role in staffRoles) {
                            vm._adminRole.value = role
                            vm._supervisorPermissions.value = resolveCachedRolePermissions(role, emptyList())
                            ensurePasswordRecoveryListenerIfStaff(role)
                            if (role !in fullAccessRoles) {
                                refreshAdminMatrixPermissionsFromFirestore(role, emptyList())
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {
        }
    }

    private fun ensurePasswordRecoveryListenerIfStaff(role: String = vm._adminRole.value) {
        val cleanRole = role.trim().uppercase()
        if (cleanRole in staffRoles) {
            if (passwordRecoveryListenerRegistration == null) {
                listenToPasswordRecoveryRequests()
            }
        } else {
            cleanup()
        }
    }

    private fun resolveCachedRolePermissions(role: String, sessionPerms: List<String>): List<String> {
        val cleanRole = role.trim().uppercase()
        if (cleanRole in fullAccessRoles) return listOf("ALL")
        return try {
            val prefs = vm.application.getSharedPreferences("admin_rbac_cache", Context.MODE_PRIVATE)
            val cachedSet = if (prefs.getBoolean("role_configured_$cleanRole", false)) {
                prefs.getStringSet("role_perms_$cleanRole", emptySet())?.toList().orEmpty()
            } else emptyList()
            val matrixMap = if (cachedSet.isNotEmpty()) mapOf(cleanRole to cachedSet) else emptyMap()
            AdminPermissionResolver.resolve(
                role = cleanRole,
                explicit = sessionPerms,
                matrix = matrixMap
            ).toList()
        } catch (_: Exception) {
            if (cleanRole == "ADMIN" && sessionPerms.isEmpty()) listOf("ALL") else sessionPerms
        }
    }

    private fun refreshAdminMatrixPermissionsFromFirestore(
        targetRole: String = vm._adminRole.value,
        explicitPerms: List<String> = emptyList()
    ) {
        val cleanRole = targetRole.trim().uppercase()
        if (cleanRole in fullAccessRoles || cleanRole !in staffRoles) return
        try {
            vm.db.collection("settings").document("admin_permissions_matrix").get()
                .addOnSuccessListener { doc ->
                    if (doc == null || !doc.exists()) return@addOnSuccessListener
                    val currentRole = vm._adminRole.value.trim().uppercase()
                    if (!currentRole.equals(cleanRole, ignoreCase = true)) return@addOnSuccessListener
                    val rolesRaw = doc.get("roles") as? Map<*, *> ?: return@addOnSuccessListener
                    val roleMap = rolesRaw[cleanRole] as? Map<*, *> ?: return@addOnSuccessListener
                    val perms = (roleMap["activePermissions"] as? List<*>)
                        ?.mapNotNull { it?.toString()?.takeIf(String::isNotBlank) }
                        ?: return@addOnSuccessListener
                    try {
                        vm.application.getSharedPreferences("admin_rbac_cache", Context.MODE_PRIVATE)
                            .edit()
                            .putStringSet("role_perms_$cleanRole", perms.toSet())
                            .putBoolean("role_configured_$cleanRole", true)
                            .apply()
                        AdminPermissionsViewModel.updateCachedRoleOverride(cleanRole, perms.toSet())
                    } catch (_: Exception) {}
                    val resolved = AdminPermissionResolver.resolve(
                        role = cleanRole,
                        explicit = explicitPerms,
                        matrix = mapOf(cleanRole to perms)
                    ).toList()
                    vm._supervisorPermissions.value = resolved
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
            val remaining = checkLockoutRemainingSec()
            if (remaining > 0L) {
                onResult(false, "تم تقييد المحاولات مؤقتاً لأمان الحساب. انتظر $remaining ثانية")
                return@launch
            }
            try {
                vm._isLoadingState.value = true
                val trimmedEmail = email.trim()
                val trimmedPass = password.trim()

                val verifiedRole = AdminSecurityManager.verifyCredentials(
                    username = trimmedEmail,
                    passwordAttempt = trimmedPass,
                    settings = vm.appState._settings.value,
                    context = vm.application,
                    supervisors = vm.appState._supervisors.value,
                    preferredRole = "ADMIN"
                )

                if (verifiedRole != null && (verifiedRole == "ADMIN" || verifiedRole in fullAccessRoles)) {
                    resetLoginLockout()
                    val assignedRole: String = verifiedRole
                    val perms = resolveCachedRolePermissions(assignedRole, emptyList())

                    SecureAdminStorage.storeCredentials(
                        context = vm.application,
                        ownerEmail = if (assignedRole in fullAccessRoles) trimmedEmail else null,
                        ownerPassword = if (assignedRole in fullAccessRoles) trimmedPass else null,
                        adminEmail = if (assignedRole == "ADMIN") trimmedEmail else null,
                        adminPassword = if (assignedRole == "ADMIN") trimmedPass else null
                    )

                    if (rememberMe) {
                        vm.secureStorage.saveAdminSession(
                            AdminSession(
                                uid = EntityIdGenerator.generate("ADM"),
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
                    ensurePasswordRecoveryListenerIfStaff(assignedRole)
                    if (assignedRole !in fullAccessRoles) {
                        refreshAdminMatrixPermissionsFromFirestore(assignedRole, emptyList())
                    }
                    vm._isLoadingState.value = false
                    onResult(true, null)
                    return@launch
                }

                val lockSec = recordLoginFailure()
                vm._isLoadingState.value = false
                val msg = if (lockSec > 0L) "محاولات خاطئة متكررة — تم القفل مؤقتاً لـ $lockSec ثانية" else "بيانات الدخول غير صحيحة"
                onResult(false, msg)
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
            val remaining = checkLockoutRemainingSec()
            if (remaining > 0L) {
                onResult(false, "تم تقييد المحاولات مؤقتاً لأمان الحساب. انتظر $remaining ثانية")
                return@launch
            }
            try {
                vm._isLoadingState.value = true
                val trimmedEmail = email.trim()
                val trimmedPass = password.trim()

                val verifiedRole = AdminSecurityManager.verifyCredentials(
                    username = trimmedEmail,
                    passwordAttempt = trimmedPass,
                    settings = vm.appState._settings.value,
                    context = vm.application,
                    supervisors = vm.appState._supervisors.value,
                    preferredRole = "OWNER"
                )

                if (verifiedRole == "OWNER") {
                    resetLoginLockout()
                    SecureAdminStorage.storeCredentials(
                        context = vm.application,
                        ownerEmail = trimmedEmail,
                        ownerPassword = trimmedPass
                    )

                    if (rememberMe) {
                        vm.secureStorage.saveAdminSession(
                            AdminSession(
                                uid = EntityIdGenerator.generate("OWN"),
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
                    ensurePasswordRecoveryListenerIfStaff("OWNER")
                    vm._isLoadingState.value = false
                    onResult(true, null)
                    return@launch
                }

                val lockSec = recordLoginFailure()
                vm._isLoadingState.value = false
                val msg = if (lockSec > 0L) "محاولات خاطئة متكررة — تم القفل مؤقتاً لـ $lockSec ثانية" else "بيانات دخول المالك غير صحيحة"
                onResult(false, msg)
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
            val remaining = checkLockoutRemainingSec()
            if (remaining > 0L) {
                onResult(false, "تم تقييد المحاولات مؤقتاً لأمان الحساب. انتظر $remaining ثانية")
                return@launch
            }
            try {
                vm._isLoadingState.value = true
                val trimmedUser = usernameOrEmail.trim()
                val trimmedPass = passcode.trim()

                val supervisorsList = vm.appState._supervisors.value
                val verifiedRole = AdminSecurityManager.verifyCredentials(
                    username = trimmedUser,
                    passwordAttempt = trimmedPass,
                    settings = vm.appState._settings.value,
                    context = vm.application,
                    supervisors = supervisorsList,
                    preferredRole = "SUPERVISOR"
                )

                if (verifiedRole != null && verifiedRole != "GUEST") {
                    resetLoginLockout()
                    val matchingSup = supervisorsList.find {
                        it.id.equals(trimmedUser, ignoreCase = true) ||
                            it.name.trim().equals(trimmedUser, ignoreCase = true)
                    }
                    val perms = resolveCachedRolePermissions(
                        role = verifiedRole,
                        sessionPerms = matchingSup?.permissions.orEmpty()
                    )

                    if (rememberMe) {
                        vm.secureStorage.saveAdminSession(
                            AdminSession(
                                uid = matchingSup?.id ?: EntityIdGenerator.generate("SUP"),
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
                    ensurePasswordRecoveryListenerIfStaff(verifiedRole)
                    if (verifiedRole.uppercase() !in fullAccessRoles) {
                        refreshAdminMatrixPermissionsFromFirestore(verifiedRole, matchingSup?.permissions.orEmpty())
                    }
                    vm._isLoadingState.value = false
                    onResult(true, null)
                    return@launch
                }

                val lockSec = recordLoginFailure()
                vm._isLoadingState.value = false
                val msg = if (lockSec > 0L) "محاولات خاطئة متكررة — تم القفل مؤقتاً لـ $lockSec ثانية" else "بيانات دخول المشرف غير صحيحة"
                onResult(false, msg)
            } catch (e: Exception) {
                vm._isLoadingState.value = false
                onResult(false, e.message ?: "فشل تسجيل الدخول")
            }
        }
    }

    fun logoutAdmin() {
        vm.viewModelScope.launch {
            try {
                cleanup()
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
            val isExpired = System.currentTimeMillis() - session.loginTime > 30L * 24 * 60 * 60 * 1000
            if (isExpired) {
                vm.secureStorage.clearAdminSession()
                vm._adminRole.value = "GUEST"
                vm._supervisorPermissions.value = emptyList()
                return false
            }

            val cleanRole = session.role.trim().uppercase()
            if (cleanRole in staffRoles) {
                val user = FirebaseAuth.getInstance().currentUser
                if (user != null) {
                    val tokenResult = runCatching { user.getIdToken(false).await() }.getOrNull()
                    val claims = tokenResult?.claims
                    val claimedRole = (claims?.get("role") as? String)?.trim()?.uppercase()
                    if (claimedRole != null && claimedRole !in staffRoles &&
                        claims["isAdmin"] != true && claims["admin"] != true &&
                        claims["isOwner"] != true && claims["isSuperAdmin"] != true
                    ) {
                        // Ignore non-staff claims when staff session is stored locally on Spark plan
                    }
                }
                vm._adminRole.value = cleanRole
                vm._supervisorPermissions.value = resolveCachedRolePermissions(cleanRole, session.permissions)
                return true
            }
            false
        } catch (_: Exception) {
            false
        }
    }

    fun saveSettingsAtomically(appName: String, welcomeMessage: String, footerMessage: String, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) =
        saveSettingsAtomicallyOp(vm, appName, welcomeMessage, footerMessage, onSuccess, onError)

    fun createSettingsBackup(onResult: (Boolean) -> Unit = {}) = createSettingsBackupOp(vm, onResult)
    fun logAdminActivity(action: String) = logAdminActivityOp(vm, action)
    fun logCall(providerId: String, providerName: String) = logCallOp(vm, providerId, providerName)
    fun loadSystemStats() = loadSystemStatsOp(vm)

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

    fun exportReport(type: String): String = exportReportOp(this, type)
    fun syncAllData(context: Context, onComplete: ((Boolean) -> Unit)? = null) = syncAllDataOp(vm, context, onComplete)
    fun clearCache(context: Context) = clearCacheOp(vm, context)
    fun getSystemHealth(): SystemHealth = SystemHealth()
    fun getStorageUsage(): StorageUsage = StorageUsage()
    fun getBookingStats(period: String = "ALL"): BookingStats = getBookingStatsOp(vm, period)
    fun getRevenueStats(period: String = "ALL"): RevenueStats = getRevenueStatsOp(vm, period)
    fun getProviderStats(): ProviderStats = getProviderStatsOp(vm)
    fun getCategoryStats(): CategoryStats = getCategoryStatsOp(vm)
    fun getCityStats(): CityStats = getCityStatsOp(vm)

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

    fun exportAuditLogs(): String = exportAuditLogsOp(this, vm)
    fun getSystemLogs(): List<SystemLog> = vm._systemLogsState.value
    fun clearSystemLogs() {
        vm._systemLogsState.value = emptyList()
    }

    private fun listenToPasswordRecoveryRequests() {
        passwordRecoveryListenerRegistration?.remove()
        passwordRecoveryListenerRegistration = vm.db.collection("password_recovery_requests")
            .limit(50)
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
