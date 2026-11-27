package com.example.ui.viewmodels

import android.content.Context
import com.example.BuildConfig
import com.example.data.SupervisorEntity
import com.example.domain.usecases.ValidatePhoneUseCase
import com.example.utils.AdminSession
import com.example.utils.PermissionGuard
import com.example.utils.RoleManager
import com.example.utils.SecureHasher
import com.example.utils.SecureStorage
import com.example.utils.SecurityCryptoUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import javax.inject.Inject

open class AuthViewModel @Inject constructor() : BaseViewModel() {

    fun updateUserFcmToken(userId: String, token: String, currentUserPhone: String) =
        updateUserFcmTokenHelper(userId, token, currentUserPhone)

    internal val _currentUserId = MutableStateFlow("guest")
    val currentUserId: StateFlow<String> = _currentUserId.asStateFlow()

    internal val _currentUserName = MutableStateFlow("")
    val currentUserName: StateFlow<String> = _currentUserName.asStateFlow()

    internal val _currentUserPhone = MutableStateFlow("")
    val currentUserPhone: StateFlow<String> = _currentUserPhone.asStateFlow()

    fun setCurrentUserPhone(phone: String) {
        _currentUserPhone.value = phone
    }

    internal val _currentUserResidence = MutableStateFlow("")
    val currentUserResidence: StateFlow<String> = _currentUserResidence.asStateFlow()

    internal val _joinRequestPhone = MutableStateFlow("")
    val joinRequestPhone: StateFlow<String> = _joinRequestPhone.asStateFlow()

    internal val _adminRole = MutableStateFlow("GUEST")
    val adminRole: StateFlow<String> = _adminRole.asStateFlow()

    internal val _passwordRecoveryWaitingPhone = MutableStateFlow("")
    val passwordRecoveryWaitingPhone: StateFlow<String> = _passwordRecoveryWaitingPhone.asStateFlow()

    internal val _showBackdoorDialog = MutableStateFlow(false)
    val showBackdoorDialog: StateFlow<Boolean> = _showBackdoorDialog.asStateFlow()

    internal val _supervisors = MutableStateFlow<List<SupervisorEntity>>(emptyList())
    val supervisors: StateFlow<List<SupervisorEntity>> = _supervisors.asStateFlow()

    internal val _currentSupervisorPermissions = MutableStateFlow<List<String>>(emptyList())
    val currentSupervisorPermissions: StateFlow<List<String>> = _currentSupervisorPermissions.asStateFlow()

    val auth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    fun resetRegistrationState() {
        _joinRequestPhone.value = ""
        _passwordRecoveryWaitingPhone.value = ""
    }

    private var clickCount = 0
    private var lastBackdoorClickTime = 0L
    @Volatile
    private var failedAdminVerifyAttempts = 0
    @Volatile
    private var adminVerifyLockoutUntilMs = 0L
    @Volatile
    internal var cachedAppContext: Context? = null

    fun getOrGenerateUserId(): String = getOrGenerateUserIdHelper()

    fun setPasswordRecoveryWaitingPhone(phone: String) {
        _passwordRecoveryWaitingPhone.value = phone
    }

    fun setJoinRequestPhone(context: Context, phone: String) {
        cachedAppContext = context.applicationContext
        val normalized = ValidatePhoneUseCase.normalizePhone(phone).filter { it.isDigit() }
        val finalPhone = normalized.ifBlank { phone.trim() }
        _joinRequestPhone.value = finalPhone
        val sp = context.getSharedPreferences("yemen_service_prefs", Context.MODE_PRIVATE)
        sp.edit().putString("join_request_phone", SecurityCryptoUtils.encrypt(finalPhone)).apply()
    }

    fun initializeUserIdentity(context: Context, onFavoritesLoaded: ((Set<String>) -> Unit)? = null) =
        initializeUserIdentityHelper(context, onFavoritesLoaded)

    fun registerGuestUser(
        context: Context,
        name: String,
        phone: String,
        residence: String,
        password: String = "",
        confirmPassword: String = password
    ) = registerGuestUserHelper(context, name, phone, residence, password, confirmPassword)

    fun setUserSessionDetails(context: Context, name: String, phone: String, residence: String = "اليمن") =
        setUserSessionDetailsHelper(context, name, phone, residence)

    fun loginUserDirectly(context: Context, phone: String, password: String) =
        loginUserDirectlyHelper(context, phone, password)

    internal var currentSupervisorId: String = ""

    fun setAdminRole(role: String) {
        _adminRole.value = role
        if (role == "OWNER") {
            _currentSupervisorPermissions.value = listOf("ALL")
        }
    }

    fun authenticateAdmin(role: String) {
        _adminRole.value = role
        if (role == "OWNER") {
            _currentSupervisorPermissions.value = listOf("ALL")
        }
        triggerToast("🔓 تم تسجيل الدخول بنجاح بصلاحية: $role")
    }

    fun authenticateAdmin(context: Context, role: String, remember: Boolean) {
        _adminRole.value = role
        if (role == "OWNER") {
            _currentSupervisorPermissions.value = listOf("ALL")
        }
        try {
            val secureStorage = SecureStorage(context)
            val existingSession = secureStorage.getAdminSession()
            if (role == "SUPERVISOR" && _currentSupervisorPermissions.value.isEmpty() && existingSession?.role == "SUPERVISOR" && existingSession.permissions.isNotEmpty()) {
                _currentSupervisorPermissions.value = existingSession.permissions
            }
            if (remember) {
                val perms = if (role == "OWNER") listOf("ALL") else _currentSupervisorPermissions.value
                val resolvedUid = if (role == "SUPERVISOR") {
                    currentSupervisorId.ifBlank {
                        existingSession?.uid?.takeIf { it.isNotBlank() && !it.startsWith("supervisor_") }
                            ?: "${role.lowercase()}_${System.currentTimeMillis()}"
                    }
                } else {
                    "${role.lowercase()}_${System.currentTimeMillis()}"
                }
                secureStorage.saveAdminSession(
                    AdminSession(
                        uid = resolvedUid,
                        email = existingSession?.email?.takeIf { it.isNotBlank() } ?: role,
                        loginTime = System.currentTimeMillis(),
                        refreshToken = "${role}_SESSION",
                        role = role,
                        permissions = perms
                    )
                )
            } else {
                secureStorage.clearAdminSession()
            }
        } catch (_: Exception) {}
        triggerToast("🔓 تم تسجيل الدخول بنجاح بصلاحية: $role")
    }

    fun logout(context: Context) {
        _adminRole.value = "GUEST"
        _currentSupervisorPermissions.value = emptyList()
        currentSupervisorId = ""
        try {
            SecureStorage(context).clearAdminSession()
        } catch (_: Exception) {}
        triggerToast("🔒 تم تسجيل الخروج بنجاح")
    }

    fun verifyAdminOrOwnerPassword(password: String, adminPass: String = "", ownerPass: String = ""): Boolean {
        val now = System.currentTimeMillis()
        if (now < adminVerifyLockoutUntilMs) {
            val remainingSec = ((adminVerifyLockoutUntilMs - now) / 1000L).coerceAtLeast(1L)
            triggerToast("⏳ محاولات كثيرة خاطئة. يرجى الانتظار $remainingSec ثانية")
            return false
        }

        val trimmed = password.trim()
        if (trimmed.isEmpty() || SecureHasher.isHashFormat(trimmed)) return false

        val isStaffMatch = SecurityCryptoUtils.verifyAdminPassword(trimmed, adminPass) ||
            SecurityCryptoUtils.verifyAdminPassword(trimmed, ownerPass)
        val matchSup = if (!isStaffMatch) {
            _supervisors.value.find {
                it.passcodeHash.isNotBlank() && SecurityCryptoUtils.verifyAdminPassword(trimmed, it.passcodeHash)
            }
        } else null

        if (isStaffMatch || matchSup != null) {
            failedAdminVerifyAttempts = 0
            adminVerifyLockoutUntilMs = 0L
            return true
        }

        failedAdminVerifyAttempts++
        if (failedAdminVerifyAttempts >= 5) {
            adminVerifyLockoutUntilMs = now + 30_000L
            failedAdminVerifyAttempts = 0
            triggerToast("🔒 تم قفل التحقق لمدة 30 ثانية بعد 5 محاولات خاطئة")
        }
        return false
    }

    fun registerBackdoorInteraction() {
        if (!BuildConfig.DEBUG) return
        val now = System.currentTimeMillis()
        if (now - lastBackdoorClickTime > 3000L) {
            clickCount = 0
        }
        lastBackdoorClickTime = now
        clickCount++
        if (clickCount >= 3) {
            clickCount = 0
            _showBackdoorDialog.value = true
        }
    }

    fun showBackdoorDialog() {
        if (!BuildConfig.DEBUG) return
        _showBackdoorDialog.value = true
    }

    fun dismissBackdoorDialog() {
        _showBackdoorDialog.value = false
    }

    fun setSupervisorSession(sup: SupervisorEntity) {
        currentSupervisorId = sup.id
        _adminRole.value = "SUPERVISOR"
        _currentSupervisorPermissions.value = sup.permissions
    }

    fun hasAdminPermission(permissionKey: String): Boolean {
        return PermissionGuard.hasPermission(
            role = RoleManager.fromRoleString(_adminRole.value),
            permission = permissionKey,
            supervisorGrantedPermissions = _currentSupervisorPermissions.value
        )
    }

    fun addSupervisor(name: String, role: String, passcode: String, permissions: List<String> = emptyList()) {
        val cleanName = name.trim()
        val cleanPasscode = passcode.trim()
        if (cleanName.isBlank() || cleanPasscode.isBlank()) {
            triggerToast("❌ يرجى إدخال اسم المشرف والرمز السري")
            return
        }
        val nextId = "sup_" + UUID.randomUUID().toString().take(6)
        val hashedPass = if (SecureHasher.isValidHash(cleanPasscode)) {
            cleanPasscode
        } else {
            SecureHasher.hashPassword(cleanPasscode)
        }
        val newSup = SupervisorEntity(id = nextId, name = cleanName, role = role, passcodeHash = hashedPass, permissions = permissions)
        db.collection("supervisors").document(nextId).set(newSup)
        triggerToast("🔑 تم إضافة المشرف $cleanName وتعيين ${permissions.size} صلاحية بنجاح")
    }

    fun editSupervisor(id: String, name: String, role: String, passcode: String, permissions: List<String> = emptyList()) {
        val cleanName = name.trim()
        val cleanPasscode = passcode.trim()
        if (id.isBlank() || cleanName.isBlank() || cleanPasscode.isBlank()) {
            triggerToast("❌ بيانات المشرف غير مكتملة")
            return
        }
        val finalPass = if (SecureHasher.isValidHash(cleanPasscode)) {
            cleanPasscode
        } else {
            SecureHasher.hashPassword(cleanPasscode)
        }
        val updatedSup = SupervisorEntity(id = id, name = cleanName, role = role, passcodeHash = finalPass, permissions = permissions)
        db.collection("supervisors").document(id).set(updatedSup)
        triggerToast("✏️ تم تعديل بيانات وصلاحيات المشرف $cleanName (${permissions.size} صلاحية) بنجاح")
    }

    fun updateSupervisorPermissions(id: String, permissions: List<String>) {
        db.collection("supervisors").document(id).update("permissions", permissions)
        triggerToast("🛡️ تم تحديث الصلاحيات الممنوحة للمشرف (${permissions.size} صلاحية)")
    }

    fun removeSupervisor(id: String) {
        db.collection("supervisors").document(id).delete()
        triggerToast("🗑️ تم إلغاء صلاحية المشرف بنجاح")
    }

    fun submitPasswordRecoveryRequest(
        phone: String,
        channel: String,
        note: String,
        onComplete: (Boolean) -> Unit = {}
    ) = submitPasswordRecoveryRequestHelper(phone, channel, note, onComplete)

    data class PasswordRecoveryStatus(val status: String = "", val tempPassword: String = "")

    internal val _passwordRecoveryStatus = MutableStateFlow(PasswordRecoveryStatus())
    val passwordRecoveryStatus: StateFlow<PasswordRecoveryStatus> = _passwordRecoveryStatus.asStateFlow()

    internal var passwordRecoveryStatusListener: ListenerRegistration? = null

    fun listenToPasswordRecoveryStatus(phone: String) =
        listenToPasswordRecoveryStatusHelper(phone)

    override fun onCleared() {
        super.onCleared()
        passwordRecoveryStatusListener?.remove()
    }

    fun observePasswordRecoveryStatus(
        phone: String,
        onUpdate: (status: String, newPassword: String, accountName: String, accountType: String) -> Unit
    ): ListenerRegistration? = observePasswordRecoveryStatusHelper(phone, onUpdate)

    fun requestPasswordRecovery(
        phone: String,
        name: String,
        accountType: String
    ) = requestPasswordRecoveryHelper(phone, name, accountType)
}
