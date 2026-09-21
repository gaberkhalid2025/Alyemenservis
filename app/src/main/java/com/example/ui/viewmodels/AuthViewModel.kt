package com.example.ui.viewmodels
import kotlinx.coroutines.tasks.await

import android.content.Context
import com.example.ui.*
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

import javax.inject.Inject

open class AuthViewModel @Inject constructor() : BaseViewModel() {

    fun updateUserFcmToken(userId: String, token: String, currentUserPhone: String) {
        if (userId.isEmpty() || userId == "guest") return
        viewModelScope.launch {
            try {
                db.collection("registered_users").document(userId).update("fcmToken", token)
                val cleanPhone = currentUserPhone.trim().replace(" ", "").replace("+", "")
                if (cleanPhone.isNotEmpty()) {
                    db.collection("providers").document(cleanPhone).update("fcmToken", token)
                    db.collection("stores").document(cleanPhone).update("fcmToken", token)
                    db.collection("properties").document(cleanPhone).update("fcmToken", token)
                }
            } catch (e: Exception) {
                android.util.Log.e("AuthViewModel", "Error updating fcmToken: ", e)
            }
        }
    }

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

    fun getOrGenerateUserId(): String {
        var current = _currentUserId.value
        if (current.isBlank() || current == "guest") {
            current = "USR_GUEST_" + (100000..999999).random().toString()
            _currentUserId.value = current
        }
        return current
    }

    fun setPasswordRecoveryWaitingPhone(phone: String) {
        _passwordRecoveryWaitingPhone.value = phone
    }

    fun setJoinRequestPhone(context: Context, phone: String) {
        val cleanPhone = phone.trim().replace(" ", "").replace("+967", "").removePrefix("0")
        val finalPhone = if (cleanPhone.length == 9) cleanPhone else phone
        _joinRequestPhone.value = finalPhone
        val sp = context.getSharedPreferences("yemen_service_prefs", Context.MODE_PRIVATE)
        sp.edit().putString("join_request_phone", com.example.utils.SecurityCryptoUtils.encrypt(finalPhone)).apply()
    }

    fun initializeUserIdentity(context: Context, onFavoritesLoaded: ((Set<String>) -> Unit)? = null) {
        com.example.ui.LocaleManager.init(context)
        val sp = context.getSharedPreferences("yemen_service_prefs", Context.MODE_PRIVATE)
        
        val rawId = sp.getString("user_id", "guest") ?: "guest"
        var savedId = if (rawId != "guest" && rawId.isNotEmpty()) com.example.utils.SecurityCryptoUtils.decrypt(rawId) else rawId
        val savedName = com.example.utils.SecurityCryptoUtils.decrypt(sp.getString("user_name", "") ?: "")
        val savedPhone = com.example.utils.SecurityCryptoUtils.decrypt(sp.getString("user_phone", "") ?: "")
        val savedResidence = com.example.utils.SecurityCryptoUtils.decrypt(sp.getString("user_residence", "") ?: "")

        if (savedId == "guest" || savedId.isEmpty()) {
            if (savedPhone.isNotEmpty()) {
                val cleanP = savedPhone.filter { it.isDigit() }
                savedId = "USR-" + (if (cleanP.length >= 6) cleanP.takeLast(6) else (100000..999999).random().toString())
            } else {
                val persistentGuest = sp.getString("persistent_guest_id", "") ?: ""
                if (persistentGuest.isNotEmpty()) {
                    savedId = com.example.utils.SecurityCryptoUtils.decrypt(persistentGuest)
                }
                if (savedId.isEmpty() || savedId == "guest") {
                    savedId = "USR_GUEST_" + (100000..999999).random().toString()
                    val enc = com.example.utils.SecurityCryptoUtils.encrypt(savedId)
                    sp.edit().putString("persistent_guest_id", enc).apply()
                }
            }
            sp.edit().putString("user_id", com.example.utils.SecurityCryptoUtils.encrypt(savedId)).apply()
        }

        _currentUserId.value = savedId
        _currentUserName.value = savedName
        _currentUserPhone.value = savedPhone
        _currentUserResidence.value = savedResidence
        
        val savedJoinPhone = com.example.utils.SecurityCryptoUtils.decrypt(sp.getString("join_request_phone", "") ?: "")
        _joinRequestPhone.value = savedJoinPhone
        
        val secureStorage = com.example.utils.SecureStorage(context)
        val session = secureStorage.getAdminSession()
        if (session != null) {
            val isExpired = System.currentTimeMillis() - session.loginTime > 30L * 24 * 60 * 60 * 1000
            if (!isExpired) {
                _adminRole.value = session.role
                if (session.role == "SUPERVISOR") {
                    // إذا كان مشرفاً، نحاول جلب صلاحياته المخزنة أو الافتراضية
                    viewModelScope.launch {
                        try {
                            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            val doc = db.collection("supervisors").document(session.uid).get().await()
                            if (doc.exists()) {
                                @Suppress("UNCHECKED_CAST")
                                val perms = doc.get("permissions") as? List<String> ?: emptyList()
                                _currentSupervisorPermissions.value = perms
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            } else {
                secureStorage.clearAdminSession()
                _adminRole.value = "GUEST"
            }
        } else {
            val savedRole = sp.getString("saved_admin_role", "GUEST") ?: "GUEST"
            if (savedRole != "GUEST") {
                _adminRole.value = savedRole
            }
        }

        try {
            val savedFavs = sp.getStringSet("favorite_ids_set", emptySet()) ?: emptySet()
            if (savedFavs.isNotEmpty()) {
                onFavoritesLoaded?.invoke(savedFavs)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        autoSyncProviderCredentials(context, savedJoinPhone, savedId, sp)
    }

    private fun autoSyncProviderCredentials(context: Context, savedJoinPhone: String, savedId: String, sp: android.content.SharedPreferences) {
        val phoneToLookup = savedJoinPhone.ifEmpty { _currentUserPhone.value }
        if (phoneToLookup.isNotEmpty()) {
            db.collection("providers").whereEqualTo("phone", phoneToLookup).get().addOnSuccessListener { snapshot ->
                if (snapshot != null && !snapshot.isEmpty) {
                    val prov = snapshot.documents.first().toObject(ProviderEntity::class.java)
                    if (prov != null) {
                        _currentUserId.value = prov.id
                        _currentUserName.value = prov.name
                        _currentUserPhone.value = prov.phone
                        _currentUserResidence.value = prov.area
                        _adminRole.value = "PROVIDER"
                        
                        sp.edit().apply {
                            putString("user_id", com.example.utils.SecurityCryptoUtils.encrypt(prov.id))
                            putString("user_name", com.example.utils.SecurityCryptoUtils.encrypt(prov.name))
                            putString("user_phone", com.example.utils.SecurityCryptoUtils.encrypt(prov.phone))
                            putString("user_residence", com.example.utils.SecurityCryptoUtils.encrypt(prov.area))
                            putString("saved_admin_role", "PROVIDER")
                            apply()
                        }
                    }
                } else {
                    db.collection("stores").whereEqualTo("phone", phoneToLookup).get().addOnSuccessListener { sSnap ->
                        if (sSnap != null && !sSnap.isEmpty) {
                            val st = sSnap.documents.first().toObject(com.example.data.StoreEntity::class.java)
                            if (st != null) {
                                _currentUserId.value = st.id
                                _currentUserName.value = st.name
                                _currentUserPhone.value = st.phone
                                _currentUserResidence.value = st.cityId
                                _adminRole.value = "STORE_OWNER"
                                sp.edit().apply {
                                    putString("user_id", com.example.utils.SecurityCryptoUtils.encrypt(st.id))
                                    putString("user_name", com.example.utils.SecurityCryptoUtils.encrypt(st.name))
                                    putString("user_phone", com.example.utils.SecurityCryptoUtils.encrypt(st.phone))
                                    putString("saved_admin_role", "STORE_OWNER")
                                    apply()
                                }
                            }
                        } else {
                            db.collection("pending_providers").whereEqualTo("phone", phoneToLookup).get().addOnSuccessListener { pSnapshot ->
                                if (pSnapshot != null && !pSnapshot.isEmpty) {
                                    val pend = pSnapshot.documents.first().toObject(PendingProviderEntity::class.java)
                                    if (pend != null) {
                                        val pendId = "user_" + pend.phone
                                        _currentUserId.value = pendId
                                        _currentUserName.value = pend.name
                                        _currentUserPhone.value = pend.phone
                                        _currentUserResidence.value = pend.area
                                        
                                        sp.edit().apply {
                                            putString("user_id", com.example.utils.SecurityCryptoUtils.encrypt(pendId))
                                            putString("user_name", com.example.utils.SecurityCryptoUtils.encrypt(pend.name))
                                            putString("user_phone", com.example.utils.SecurityCryptoUtils.encrypt(pend.phone))
                                            putString("user_residence", com.example.utils.SecurityCryptoUtils.encrypt(pend.area))
                                            apply()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun registerGuestUser(context: Context, name: String, phone: String, residence: String, password: String = "") {
        val cleanPhone = phone.trim().replace(" ", "").replace("+", "").replace("-", "")
        val effectivePassword = if (password.isBlank()) "yemen_${cleanPhone.takeLast(6)}" else password.trim()

        if (password.isNotBlank()) {
            val valResult = com.example.utils.SecurityCryptoUtils.validatePasswordPolicy(password)
            if (!valResult.first) {
                triggerToast("⚠️ ${valResult.second}")
                return
            }
        }

        viewModelScope.launch {
            try {
                // ✨ فرض تجزئة SHA-256 / PBKDF2 لكلمة المرور ومنع تخزين نصوص صريحة
                val secureHash = com.example.utils.PasswordHasher.createSaltedHash(effectivePassword)

                val client = com.example.domain.entities.RegistrationEntity.Client(
                    fullName = name.trim(),
                    phone = cleanPhone,
                    city = residence.trim(),
                    passwordHash = secureHash
                )

                val repository = com.example.data.repositories.RegistrationRepositoryImpl(context)
                val result = repository.registerClient(client)

                if (result.isSuccess) {
                    val userId = result.getOrNull() ?: ("usr_" + cleanPhone)

                    _currentUserId.value = userId
                    _currentUserName.value = name.trim()
                    _currentUserPhone.value = cleanPhone
                    _currentUserResidence.value = residence.trim()
                    _joinRequestPhone.value = cleanPhone

                    val sp = context.getSharedPreferences("yemen_service_prefs", Context.MODE_PRIVATE)
                    sp.edit().apply {
                        putString("user_id", com.example.utils.SecurityCryptoUtils.encrypt(userId))
                        putString("user_name", com.example.utils.SecurityCryptoUtils.encrypt(name.trim()))
                        putString("user_phone", com.example.utils.SecurityCryptoUtils.encrypt(cleanPhone))
                        putString("user_residence", com.example.utils.SecurityCryptoUtils.encrypt(residence.trim()))
                        putString("join_request_phone", com.example.utils.SecurityCryptoUtils.encrypt(cleanPhone))
                        apply()
                    }

                    triggerToast("🎉 أهلاً بك في الدليل $name، تم تسجيل وحماية حسابك آمنياً بنجاح!")
                } else {
                    val errorMsg = result.exceptionOrNull()?.localizedMessage ?: "فشل تسجيل حساب العميل"
                    triggerToast("❌ $errorMsg")
                }
            } catch (e: Exception) {
                triggerToast("❌ حدث خطأ أثناء التسجيل: ${e.localizedMessage}")
            }
        }
    }

    fun setUserSessionDetails(context: Context, name: String, phone: String, residence: String = "اليمن") {
        val cleanPhone = phone.trim().replace(" ", "").replace("+967", "").removePrefix("0")
        val finalPhone = if (cleanPhone.length == 9) cleanPhone else phone
        _currentUserName.value = name.ifBlank { "عميل" }
        _currentUserPhone.value = finalPhone
        _currentUserResidence.value = residence.ifBlank { "اليمن" }
        if (_currentUserId.value.isEmpty() || _currentUserId.value == "guest") {
            _currentUserId.value = "user_" + (if (finalPhone.length >= 6) finalPhone.takeLast(6) else (100000..999999).random().toString())
        }
        _joinRequestPhone.value = finalPhone
        val sp = context.getSharedPreferences("yemen_service_prefs", Context.MODE_PRIVATE)
        sp.edit().apply {
            putString("user_name", com.example.utils.SecurityCryptoUtils.encrypt(_currentUserName.value))
            putString("user_phone", com.example.utils.SecurityCryptoUtils.encrypt(finalPhone))
            putString("user_residence", com.example.utils.SecurityCryptoUtils.encrypt(_currentUserResidence.value))
            putString("user_id", com.example.utils.SecurityCryptoUtils.encrypt(_currentUserId.value))
            putString("join_request_phone", com.example.utils.SecurityCryptoUtils.encrypt(finalPhone))
            apply()
        }
        try {
            com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().setUserId(_currentUserId.value)
            com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().log("Session updated for user ${_currentUserId.value}")
        } catch (e: Exception) {
            // تجاهل في بيئات الاختبار
        }
    }

    fun loginUserDirectly(context: Context, phone: String, password: String) {
        val cleanPhone = phone.trim().replace(" ", "").replace("+967", "").removePrefix("0")
        val finalPhone = if (cleanPhone.length == 9) cleanPhone else phone
        
        viewModelScope.launch {
            try {
                db.collection("registered_users")
                    .whereEqualTo("phone", finalPhone)
                    .limit(1)
                    .get()
                    .addOnSuccessListener { snapshot ->
                        if (!snapshot.isEmpty) {
                            val doc = snapshot.documents.first()
                            val storedHash = doc.getString("passwordHash") ?: doc.getString("password") ?: ""
                            if (com.example.utils.PasswordHasher.verifyPassword(password, storedHash)) {
                                _currentUserPhone.value = finalPhone
                                _joinRequestPhone.value = finalPhone
                                val sp = context.getSharedPreferences("yemen_service_prefs", Context.MODE_PRIVATE)
                                sp.edit().apply {
                                    putBoolean("is_account_logged_in", true)
                                    putString("user_phone", com.example.utils.SecurityCryptoUtils.encrypt(finalPhone))
                                    putString("join_request_phone", com.example.utils.SecurityCryptoUtils.encrypt(finalPhone))
                                    apply()
                                }
                                triggerToast("✅ تم تسجيل الدخول بنجاح")
                            } else {
                                triggerToast("❌ كلمة المرور غير صحيحة")
                            }
                        } else {
                            triggerToast("❌ الحساب غير موجود")
                        }
                    }
                    .addOnFailureListener { e ->
                        triggerToast("❌ حدث خطأ أثناء التحقق: ${e.message}")
                    }
            } catch (e: Exception) {
                triggerToast("❌ حدث خطأ أثناء التحقق: ${e.message}")
            }
        }
    }

    fun authenticateAdmin(role: String) {
        _adminRole.value = role
        triggerToast("🔓 تم تسجيل الدخول بنجاح بصلاحية: $role")
    }

    fun authenticateAdmin(context: Context, role: String, remember: Boolean) {
        _adminRole.value = role
        if (remember) {
            val sp = context.getSharedPreferences("yemen_service_prefs", Context.MODE_PRIVATE)
            sp.edit().putString("saved_admin_role", role).apply()
        }
        triggerToast("🔓 تم تسجيل الدخول بنجاح بصلاحية: $role")
    }

    fun logout(context: Context) {
        _adminRole.value = "GUEST"
        val sp = context.getSharedPreferences("yemen_service_prefs", Context.MODE_PRIVATE)
        sp.edit().putString("saved_admin_role", "GUEST").apply()
        triggerToast("🔒 تم تسجيل الخروج بنجاح")
    }

    fun verifyAdminOrOwnerPassword(password: String, adminPass: String = "", ownerPass: String = ""): Boolean {
        val trimmed = password.trim()
        if (trimmed.isEmpty()) return false
        
        if (com.example.utils.SecurityCryptoUtils.verifyAdminPassword(trimmed, adminPass) ||
            com.example.utils.SecurityCryptoUtils.verifyAdminPassword(trimmed, ownerPass)) {
            return true
        }
        val matchSup = _supervisors.value.find {
            it.passcode.isNotBlank() && com.example.utils.SecurityCryptoUtils.verifyAdminPassword(trimmed, it.passcode)
        }
        return matchSup != null
    }

    fun registerBackdoorInteraction() {
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
        _showBackdoorDialog.value = true
    }

    fun dismissBackdoorDialog() {
        _showBackdoorDialog.value = false
    }

    fun setSupervisorSession(sup: SupervisorEntity) {
        _adminRole.value = "SUPERVISOR"
        _currentSupervisorPermissions.value = sup.permissions
    }

    fun hasAdminPermission(permissionKey: String): Boolean {
        return com.example.utils.PermissionGuard.hasPermission(
            role = com.example.utils.RoleManager.fromRoleString(_adminRole.value),
            permission = permissionKey,
            supervisorGrantedPermissions = _currentSupervisorPermissions.value
        )
    }

    fun addSupervisor(name: String, role: String, passcode: String, permissions: List<String> = emptyList()) {
        val nextId = "sup_" + UUID.randomUUID().toString().take(6)
        // ✨ م2: تشفير كلمة المرور (Hashing) باستخدام PasswordHasher قبل التخزين لحماية المشرفين
        val hashedPass = com.example.utils.PasswordHasher.createSaltedHash(passcode.trim())
        val newSup = SupervisorEntity(nextId, name, role, hashedPass, permissions)
        db.collection("supervisors").document(nextId).set(newSup)
        com.example.utils.ActivityLogManager.logAdminOperation(
            action = "👤 إضافة مشرف جديد: $name بصلاحية $role",
            performedBy = "SUPER_ADMIN",
            target = nextId
        )
        triggerToast("🔑 تم إضافة المشرف $name وتعيين ${permissions.size} صلاحية بنجاح")
    }

    fun editSupervisor(id: String, name: String, role: String, passcode: String, permissions: List<String> = emptyList()) {
        // ✨ م2: تشفير كلمة المرور في حال التعديل لضمان الأمان
        val finalPass = if (passcode.contains(":")) passcode else com.example.utils.PasswordHasher.createSaltedHash(passcode.trim())
        val updatedSup = SupervisorEntity(id, name, role, finalPass, permissions)
        db.collection("supervisors").document(id).set(updatedSup)
        com.example.utils.ActivityLogManager.logAdminOperation(
            action = "✏️ تعديل بيانات وصلاحيات المشرف: $name ($id)",
            performedBy = "SUPER_ADMIN",
            target = id
        )
        triggerToast("✏️ تم تعديل بيانات وصلاحيات المشرف $name (${permissions.size} صلاحية) بنجاح")
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
    ) {
        val cleanPhone = phone.trim().replace(" ", "")
        val currentTime = System.currentTimeMillis()
        val currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""

        val resetRequest = mapOf(
            "phone" to cleanPhone,
            "channel" to channel,
            "note" to note,
            "status" to "PENDING",
            "createdAt" to currentTime
        )
        db.collection("password_resets").document(cleanPhone).set(resetRequest)

        val adminRecoveryRequest = mapOf(
            "id" to cleanPhone,
            "uid" to currentUid,
            "phone" to cleanPhone,
            "name" to "طلب استعادة ($cleanPhone)",
            "accountType" to "مسترجع",
            "status" to "PENDING",
            "timestamp" to currentTime,
            "newPassword" to "",
            "adminNotes" to "القناة: $channel | ملاحظة: $note"
        )
        db.collection("password_recovery_requests").document(cleanPhone).set(adminRecoveryRequest)

        val adminNotifId = java.util.UUID.randomUUID().toString()
        val adminNotif = mapOf(
            "id" to adminNotifId,
            "title" to "🔑 طلب استعادة حساب جديد",
            "message" to "ورد طلب استعادة حساب للرقم: $cleanPhone عبر قناة $channel",
            "targetType" to "ADMIN_ONLY",
            "targetValue" to "ALL",
            "timestamp" to currentTime
        )
        db.collection("notifications").document(adminNotifId).set(adminNotif)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    data class PasswordRecoveryStatus(val status: String = "", val tempPassword: String = "")

    private val _passwordRecoveryStatus = MutableStateFlow(PasswordRecoveryStatus())
    val passwordRecoveryStatus: StateFlow<PasswordRecoveryStatus> = _passwordRecoveryStatus.asStateFlow()

    private var passwordRecoveryStatusListener: com.google.firebase.firestore.ListenerRegistration? = null

    fun listenToPasswordRecoveryStatus(phone: String) {
        passwordRecoveryStatusListener?.remove()
        val cleanPhone = phone.trim().replace(" ", "")
        passwordRecoveryStatusListener = db.collection("password_resets").document(cleanPhone)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null && snapshot.exists()) {
                    val status = snapshot.getString("status") ?: "PENDING"
                    val temp = snapshot.getString("tempPassword") ?: snapshot.getString("newPassword") ?: ""
                    _passwordRecoveryStatus.value = PasswordRecoveryStatus(status, temp)
                }
            }
    }

    override fun onCleared() {
        super.onCleared()
        passwordRecoveryStatusListener?.remove()
    }

    fun observePasswordRecoveryStatus(
        phone: String,
        onUpdate: (status: String, newPassword: String, accountName: String, accountType: String) -> Unit
    ): com.google.firebase.firestore.ListenerRegistration? {
        if (phone.isBlank()) return null
        val cleanPhone = phone.trim().replace(" ", "").replace("+967", "").replace("967", "").replace("+", "")
        return try {
            db.collection("password_recovery_requests")
                .document(cleanPhone)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null && snapshot.exists()) {
                        val status = snapshot.getString("status") ?: "PENDING"
                        val newPassword = snapshot.getString("newPassword") ?: snapshot.getString("tempPassword") ?: ""
                        val accountName = snapshot.getString("name") ?: "صاحب الحساب"
                        val accountType = snapshot.getString("accountType") ?: "حساب معتمد"
                        onUpdate(status, newPassword, accountName, accountType)
                    }
                }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun requestPasswordRecovery(
        phone: String,
        name: String,
        accountType: String
    ) {
        viewModelScope.launch {
            try {
                val cleanPhone = phone.trim().replace(" ", "").replace("+967", "").replace("967", "").replace("+", "")
                val currentTime = System.currentTimeMillis()
                val requestData = mapOf(
                    "id" to cleanPhone,
                    "phone" to cleanPhone,
                    "name" to name.ifBlank { "صاحب الحساب ($cleanPhone)" },
                    "accountType" to accountType,
                    "status" to "PENDING",
                    "requestedAt" to currentTime,
                    "newPassword" to "",
                    "adminNotes" to "",
                    "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )
                
                db.collection("password_recovery_requests")
                    .document(cleanPhone)
                    .set(requestData, com.google.firebase.firestore.SetOptions.merge())
                    .await()

                val notifDocId = "PWD_RESET_NOTIF_$cleanPhone"
                val adminNotif = mapOf(
                    "id" to notifDocId,
                    "title" to "🔑 طلب استعادة كلمة مرور ($name)",
                    "message" to "ورد طلب استعادة وتعيين كلمة مرور للحساب: $name ($accountType) - الهاتف: $cleanPhone",
                    "targetType" to "ADMIN_ONLY",
                    "targetValue" to "ALL",
                    "timestamp" to currentTime,
                    "dedupKey" to "PWD_RESET_$cleanPhone"
                )
                db.collection("notifications").document(notifDocId).set(adminNotif)
                
                // Record in activity_logs centrally
                com.example.utils.ActivityLogManager.logPasswordRecoveryRequest(
                    phone = cleanPhone,
                    name = name.ifBlank { "صاحب الحساب ($cleanPhone)" },
                    accountType = accountType
                )

                triggerToast("✅ تم إرسال طلبك للإدارة. سيتم مراجعته والتواصل معك قريباً")
            } catch (e: Exception) {
                triggerToast("❌ فشل إرسال الطلب: ${e.message}")
            }
        }
    }

    /**
     * 🔐 المسار الآمن لإعادة تعيين كلمة المرور للمستخدمين ومختلف الحسابات
     * يضمن تشفير كافة كلمات المرور عبر PasswordHasher وتوثيق العملية في ActivityLogManager
     */
    fun executeSecurePasswordReset(
        entityType: String,
        phoneOrId: String,
        newPass: String,
        performedBy: String = "ADMIN",
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        val cleanPhone = phoneOrId.trim().replace(" ", "").replace("+967", "").replace("967", "").replace("+", "")
        if (cleanPhone.length < 9) {
            onResult(false, "رقم الهاتف غير صالح")
            return
        }
        val hashedPass = com.example.utils.PasswordHasher.createSaltedHash(newPass.trim())
        val passUpdate = mapOf("password" to hashedPass, "passwordHash" to hashedPass)

        val collections = when (entityType.uppercase()) {
            "PROVIDER", "TECHNICIAN", "TECH" -> listOf("providers", "pending_providers")
            "STORE", "RESTAURANT", "MEDICAL", "CENTER" -> listOf("stores")
            "JOB" -> listOf("jobs")
            "USER", "CLIENT" -> listOf("registered_users", "users")
            else -> listOf("registered_users", "providers", "stores", "properties")
        }

        var updateCount = 0
        viewModelScope.launch {
            try {
                for (col in collections) {
                    val qs = db.collection(col).get().await()
                    for (doc in qs.documents) {
                        val p = doc.getString("phone") ?: ""
                        if (p.contains(cleanPhone)) {
                            db.collection(col).document(doc.id).update(passUpdate).await()
                            updateCount++
                        }
                    }
                }

                // تحديث حالة طلب الاستعادة إذا كان موجوداً
                try {
                    db.collection("password_recovery_requests").document(cleanPhone).update(
                        mapOf(
                            "status" to "RESOLVED",
                            "newPassword" to hashedPass,
                            "resolvedAt" to System.currentTimeMillis()
                        )
                    )
                } catch (_: Exception) {}

                com.example.utils.ActivityLogManager.logPasswordReset(
                    targetPhone = cleanPhone,
                    entityType = entityType,
                    performedBy = performedBy,
                    isApproval = false,
                    details = "تم تحديث كلمات المرور بنجاح لـ $updateCount سجل"
                )

                onResult(true, "تم إعادة تعيين وتأمين كلمة المرور بنجاح")
            } catch (e: Exception) {
                onResult(false, "فشل حفظ التحديثات: ${e.localizedMessage}")
            }
        }
    }

    /**
     * 🔐 الموافقة الآمنة على طلب إعادة التعيين مع توليد كلمة مرور مؤقتة وتشفيرها
     */
    fun approveSecurePasswordReset(
        phone: String,
        onResult: (Boolean, String) -> Unit
    ) {
        val cleanPhone = phone.trim().replace(" ", "").replace("+967", "").replace("967", "").replace("+", "").replace("-", "")
        if (cleanPhone.length < 9) {
            onResult(false, "رقم الهاتف غير صالح")
            return
        }

        val chars = "23456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
        val tempPassword = (1..8).map { chars.random() }.joinToString("")
        val hashedPassword = com.example.utils.PasswordHasher.createSaltedHash(tempPassword)

        viewModelScope.launch {
            try {
                val batch = db.batch()
                val reqRef = db.collection("password_recovery_requests").document(cleanPhone)
                batch.update(reqRef, mapOf("status" to "APPROVED", "newPassword" to hashedPassword))

                val resetRef = db.collection("password_resets").document(cleanPhone)
                batch.update(resetRef, mapOf("status" to "APPROVED", "tempPassword" to tempPassword))

                val userQs = db.collection("registered_users").whereEqualTo("phone", cleanPhone).get().await()
                userQs.documents.firstOrNull()?.let { doc ->
                    batch.update(doc.reference, "password", hashedPassword)
                    batch.update(doc.reference, "passwordHash", hashedPassword)
                }

                val provQs = db.collection("providers").whereEqualTo("phone", cleanPhone).get().await()
                provQs.documents.firstOrNull()?.let { pDoc ->
                    batch.update(pDoc.reference, "password", hashedPassword)
                    batch.update(pDoc.reference, "passwordHash", hashedPassword)
                }

                val notifId = UUID.randomUUID().toString()
                val notif = NotificationEntity(
                    id = notifId,
                    title = "🔑 تم إعادة تعيين كلمة المرور",
                    message = "تمت الموافقة على طلبك لإعادة تعيين كلمة المرور. كلمة المرور المؤقتة الجديدة هي: $tempPassword يرجى تغييرها بعد تسجيل الدخول.",
                    targetType = "USER",
                    targetValue = cleanPhone,
                    notificationType = "PASSWORD_RESET_APPROVED",
                    timestamp = System.currentTimeMillis(),
                    dedupKey = "PWD_RESET_APPROVED_$cleanPhone"
                )
                batch.set(db.collection("notifications").document(notifId), notif)

                batch.commit().await()

                com.example.utils.ActivityLogManager.logPasswordReset(
                    targetPhone = cleanPhone,
                    entityType = "USER",
                    performedBy = "ADMIN",
                    isApproval = true,
                    details = "تم إنشاء كلمة مرور مؤقتة واعتماد الطلب"
                )

                onResult(true, "تمت الموافقة بنجاح. كلمة المرور المؤقتة هي: $tempPassword")
            } catch (e: Exception) {
                onResult(false, "فشل اعتماد الطلب: ${e.localizedMessage}")
            }
        }
    }

    /**
     * 🔐 إعادة تعيين من الإدارة مع إجراء إشعار مخصص
     */
    fun adminResetPasswordWithAction(
        phone: String,
        newPassword: String,
        notifyAction: String,
        customerName: String,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        val cleanPhone = phone.trim().replace(" ", "").replace("+967", "").replace("967", "").replace("+", "")
        val hashedPassword = com.example.utils.PasswordHasher.createSaltedHash(newPassword.trim())
        val passUpdate = mapOf("password" to hashedPassword, "passwordHash" to hashedPassword)

        viewModelScope.launch {
            try {
                val collections = listOf("providers", "pending_providers", "stores", "properties", "registered_users", "users", "join_requests")
                for (col in collections) {
                    val snap = db.collection(col).get().await()
                    for (doc in snap.documents) {
                        val p = doc.getString("phone") ?: ""
                        if (p.contains(cleanPhone)) {
                            db.collection(col).document(doc.id).update(passUpdate).await()
                        }
                    }
                }

                try {
                    db.collection("password_recovery_requests").document(cleanPhone).set(
                        mapOf(
                            "status" to "RESOLVED",
                            "newPassword" to hashedPassword,
                            "resolvedAt" to System.currentTimeMillis()
                        ),
                        com.google.firebase.firestore.SetOptions.merge()
                    ).await()
                } catch (_: Exception) {}

                com.example.utils.ActivityLogManager.logPasswordReset(
                    targetPhone = cleanPhone,
                    entityType = "GENERAL_ACCOUNT",
                    performedBy = "ADMIN",
                    isApproval = false,
                    details = "إعادة تعيين للحساب: $customerName - الإجراء: $notifyAction"
                )

                val notifId = UUID.randomUUID().toString()
                val (title, message) = when (notifyAction) {
                    "DIRECT_PASSWORD" -> Pair(
                        "🔑 إعادة تعيين كلمة المرور بنجاح",
                        "تم إعادة تعيين كلمة مرور حسابك. يرجى التواصل مع الدعم لاستلام كلمة المرور الجديدة"
                    )
                    "VERIFICATION_WHATSAPP" -> Pair(
                        "🔐 التحقق من الهوية - استعادة الحساب",
                        "عزيزي المشترك، يرجى التواصل عبر الواتساب أو التليجرام أو المحادثة الفورية مع الإدارة للتحقق من هويتك وتأكيد ملكيتك للحساب واستلام كلمة المرور."
                    )
                    "INSTANT_CHAT" -> Pair(
                        "💬 محادثة فورية لاستعادة الحساب",
                        "تم فتح قناة دعم فورية لك. يرجى التوجه للمحادثة المباشرة مع الإدارة للتحقق من هويتك واسترجاع حسابك فوراً."
                    )
                    else -> Pair(
                        "🔑 تحديث كلمة المرور",
                        "قامت الإدارة بتحديث ومعالجة طلب استعادة كلمة المرور الخاصة بحسابك."
                    )
                }

                val userNotif = NotificationEntity(
                    id = notifId,
                    title = title,
                    message = message,
                    targetType = "USER",
                    targetValue = cleanPhone,
                    timestamp = System.currentTimeMillis(),
                    dedupKey = "PWD_RESET_${cleanPhone}"
                )
                db.collection("notifications").document(notifId).set(userNotif).await()

                onResult(true, "تم تحديث كلمة المرور وإرسال الإشعار للمستخدم بنجاح")
            } catch (e: Exception) {
                onResult(false, "حدث خطأ أثناء التحديث: ${e.localizedMessage}")
            }
        }
    }
}
