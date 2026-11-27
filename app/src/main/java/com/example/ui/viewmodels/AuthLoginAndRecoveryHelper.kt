package com.example.ui.viewmodels

import android.content.Context
import androidx.lifecycle.viewModelScope
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.data.ActivityLogEntity
import com.example.data.isValid
import com.example.data.repositories.RegistrationRepositoryImpl
import com.example.domain.entities.RegistrationEntity
import com.example.domain.usecases.ValidatePhoneUseCase
import com.example.utils.PasswordHasher
import com.example.utils.SecureAdminStorage
import com.example.utils.SecureHasher
import com.example.utils.SecurityCryptoUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

internal fun AuthViewModel.registerGuestUserHelper(
    context: Context,
    name: String,
    phone: String,
    residence: String,
    password: String = "",
    confirmPassword: String = password
) {
    cachedAppContext = context.applicationContext
    val cleanPhone = ValidatePhoneUseCase.normalizePhone(phone).filter { it.isDigit() }
    val cleanPassword = password.trim()
    val cleanConfirm = confirmPassword.trim()
    if (cleanPhone.length < 7) {
        triggerToast("⚠️ يرجى إدخال رقم هاتف صحيح")
        return
    }
    if (cleanPassword.isBlank()) {
        triggerToast("⚠️ لا يمكن التسجيل بدون كلمة مرور، يرجى إدخال كلمة مرور صريحة لتأمين حسابك")
        return
    }
    if (cleanPassword != cleanConfirm) {
        triggerToast("⚠️ كلمة المرور وتأكيد كلمة المرور غير متطابقين")
        return
    }

    val valResult = SecurityCryptoUtils.validatePasswordPolicy(cleanPassword)
    if (!valResult.first) {
        triggerToast("⚠️ ${valResult.second ?: "يرجى إدخال كلمة مرور قوية لحماية حسابك"}")
        return
    }

    viewModelScope.launch {
        try {
            val client = RegistrationEntity.Client(
                fullName = name.trim(),
                phone = cleanPhone,
                city = residence.trim(),
                rawPassword = cleanPassword
            )

            val repository = RegistrationRepositoryImpl(context)
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
                    putString("user_id", SecurityCryptoUtils.encrypt(userId))
                    putString("user_name", SecurityCryptoUtils.encrypt(name.trim()))
                    putString("user_phone", SecurityCryptoUtils.encrypt(cleanPhone))
                    putString("user_residence", SecurityCryptoUtils.encrypt(residence.trim()))
                    putString("join_request_phone", SecurityCryptoUtils.encrypt(cleanPhone))
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

internal fun AuthViewModel.loginUserDirectlyHelper(
    context: Context,
    phone: String,
    password: String
) {
    val finalPhone = ValidatePhoneUseCase.normalizePhone(phone).filter { it.isDigit() }
    val cleanPassword = password.trim()
    if (finalPhone.length < 7 || cleanPassword.isBlank()) {
        triggerToast("❌ يرجى إدخال رقم الهاتف وكلمة المرور بشكل صحيح")
        return
    }
    val phoneVariants = listOf(finalPhone, "0$finalPhone", "+967$finalPhone", "967$finalPhone", "00967$finalPhone").distinct()

    viewModelScope.launch {
        try {
            var matchedDoc: DocumentSnapshot? = null
            for (col in listOf("registered_users", "users")) {
                for (ph in phoneVariants) {
                    val snap = runCatching {
                        db.collection(col).whereEqualTo("phone", ph).limit(1).get().await()
                    }.getOrNull()
                    val docs = snap?.documents.orEmpty().toMutableList()
                    runCatching {
                        val direct = db.collection(col).document(ph).get().await()
                        if (direct.exists() && docs.none { it.id == direct.id }) {
                            docs.add(direct)
                        }
                    }
                    for (doc in docs) {
                        val hasCredential = !doc.getString("passwordHash").isNullOrBlank() || !doc.getString("password").isNullOrBlank()
                        if (hasCredential) {
                            matchedDoc = doc
                            break
                        }
                    }
                    if (matchedDoc != null) break
                }
                if (matchedDoc != null) break
            }
            val targetDoc = matchedDoc

            if (targetDoc != null) {
                val storedHash = targetDoc.getString("passwordHash")?.trim().orEmpty()
                    .ifBlank { targetDoc.getString("password")?.trim().orEmpty() }
                val fieldUsed = if (!targetDoc.getString("passwordHash").isNullOrBlank()) "passwordHash" else "password"
                val isVerified = SecureHasher.verifyPassword(cleanPassword, storedHash) ||
                    PasswordHasher.verifyPassword(cleanPassword, storedHash) ||
                    SecureAdminStorage.verifyAndMigrate(
                        docRef = targetDoc.reference,
                        inputPassword = cleanPassword,
                        storedPassOrHash = storedHash,
                        fieldName = fieldUsed
                    )
                if (isVerified) {
                    val userName = targetDoc.getString("name")?.takeIf { it.isNotBlank() }
                        ?: targetDoc.getString("fullName")?.takeIf { it.isNotBlank() }
                        ?: "عميل"
                    val userCity = targetDoc.getString("city")?.takeIf { it.isNotBlank() }
                        ?: targetDoc.getString("area")?.takeIf { it.isNotBlank() }
                        ?: "اليمن"
                    setUserSessionDetails(context, userName, finalPhone, userCity)
                    val sp = context.getSharedPreferences("yemen_service_prefs", Context.MODE_PRIVATE)
                    sp.edit().apply {
                        putBoolean("is_account_logged_in", true)
                        putString("user_account_type", "CLIENT")
                        putString("logged_account_id", targetDoc.id)
                        apply()
                    }
                    runCatching {
                        val masterKey = MasterKey.Builder(context)
                            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                            .build()
                        val secureSp = EncryptedSharedPreferences.create(
                            context,
                            "yemen_service_secure_prefs",
                            masterKey,
                            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                        )
                        secureSp.edit()
                            .putBoolean("is_account_logged_in", true)
                            .putString("user_account_type", "CLIENT")
                            .putString("logged_account_id", targetDoc.id)
                            .apply()
                    }
                    runCatching {
                        val auth = FirebaseAuth.getInstance()
                        if (auth.currentUser == null) {
                            val virtualEmail = "usr_${finalPhone}@yemen-services.app"
                            auth.signInWithEmailAndPassword(virtualEmail, cleanPassword)
                                .addOnFailureListener {
                                    auth.createUserWithEmailAndPassword(virtualEmail, cleanPassword)
                                        .addOnFailureListener {
                                            auth.signInAnonymously()
                                        }
                                }
                        }
                    }
                    if (_adminRole.value !in listOf("OWNER", "ADMIN", "SUPERVISOR")) {
                        _adminRole.value = "GUEST"
                    }
                    triggerToast("✅ تم تسجيل الدخول بنجاح")
                } else {
                    triggerToast("❌ كلمة المرور غير صحيحة")
                }
            } else {
                triggerToast("❌ الحساب غير موجود")
            }
        } catch (e: Exception) {
            triggerToast("❌ حدث خطأ أثناء التحقق: ${e.message}")
        }
    }
}

private fun decryptRecoveryTempPassword(rawPass: String): String {
    return when {
        rawPass.isEmpty() -> ""
        rawPass.startsWith("gcmx:") -> SecurityCryptoUtils.decryptCrossDevice(rawPass)
        rawPass.startsWith("gcm:") || rawPass.startsWith("enc::") ->
            SecurityCryptoUtils.decrypt(rawPass).ifBlank {
                SecurityCryptoUtils.decryptCrossDevice(rawPass)
            }
        SecureHasher.isValidHash(rawPass) -> ""
        else -> rawPass
    }
}

internal fun AuthViewModel.submitPasswordRecoveryRequestHelper(
    phone: String,
    channel: String,
    note: String,
    onComplete: (Boolean) -> Unit = {}
) {
    val cleanPhone = ValidatePhoneUseCase.normalizePhone(phone).filter { it.isDigit() }
    if (cleanPhone.length < 7) {
        onComplete(false)
        return
    }
    val currentTime = System.currentTimeMillis()
    val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    val resetRequest = mapOf(
        "id" to cleanPhone,
        "uid" to currentUid,
        "phone" to cleanPhone,
        "channel" to channel,
        "note" to note,
        "status" to "PENDING",
        "newPassword" to "",
        "passwordHash" to "",
        "createdAt" to currentTime,
        "requestedAt" to currentTime,
        "timestamp" to currentTime
    )
    db.collection("password_resets").document(cleanPhone).set(resetRequest)

    val adminRecoveryRequest = mapOf(
        "id" to cleanPhone,
        "uid" to currentUid,
        "phone" to cleanPhone,
        "name" to "طلب استعادة ($cleanPhone)",
        "accountType" to "مسترجع",
        "status" to "PENDING",
        "createdAt" to currentTime,
        "requestedAt" to currentTime,
        "timestamp" to currentTime,
        "newPassword" to "",
        "passwordHash" to "",
        "adminNotes" to "القناة: $channel | ملاحظة: $note"
    )
    db.collection("password_recovery_requests").document(cleanPhone)
        .set(adminRecoveryRequest)
        .addOnSuccessListener {
            val adminNotifId = "PWD_RESET_ADMIN_$cleanPhone"
            val adminNotif = mapOf(
                "id" to adminNotifId,
                "title" to "🔑 طلب استعادة حساب جديد",
                "message" to "ورد طلب استعادة حساب للرقم: $cleanPhone عبر قناة $channel",
                "targetType" to "ADMIN_ONLY",
                "targetValue" to "ALL",
                "timestamp" to currentTime,
                "dedupKey" to "PWD_RESET_$cleanPhone"
            )
            if (adminNotif.isValid()) {
                db.collection("notifications").document(adminNotifId).set(adminNotif)
            }
            onComplete(true)
        }
        .addOnFailureListener {
            onComplete(false)
        }
}

internal fun AuthViewModel.listenToPasswordRecoveryStatusHelper(phone: String) {
    passwordRecoveryStatusListener?.remove()
    val cleanPhone = ValidatePhoneUseCase.normalizePhone(phone).filter { it.isDigit() }
    if (cleanPhone.length < 7) return
    passwordRecoveryStatusListener = db.collection("password_recovery_requests").document(cleanPhone)
        .addSnapshotListener { snapshot, e ->
            if (e != null) return@addSnapshotListener
            if (snapshot != null && snapshot.exists()) {
                val status = snapshot.getString("status") ?: "PENDING"
                val rawPass = (snapshot.getString("newPassword") ?: snapshot.getString("tempPassword") ?: "").trim()
                val temp = decryptRecoveryTempPassword(rawPass)
                _passwordRecoveryStatus.value = AuthViewModel.PasswordRecoveryStatus(status, temp)
            }
        }
}

internal fun AuthViewModel.observePasswordRecoveryStatusHelper(
    phone: String,
    onUpdate: (status: String, newPassword: String, accountName: String, accountType: String) -> Unit
): ListenerRegistration? {
    if (phone.isBlank()) return null
    val cleanPhone = ValidatePhoneUseCase.normalizePhone(phone).filter { it.isDigit() }
    if (cleanPhone.length < 7) return null
    return try {
        db.collection("password_recovery_requests")
            .document(cleanPhone)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null && snapshot.exists()) {
                    val status = snapshot.getString("status") ?: "PENDING"
                    val rawPass = (snapshot.getString("newPassword") ?: snapshot.getString("tempPassword") ?: "").trim()
                    val newPassword = decryptRecoveryTempPassword(rawPass)
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

internal fun AuthViewModel.requestPasswordRecoveryHelper(
    phone: String,
    name: String,
    accountType: String
) {
    viewModelScope.launch {
        try {
            val cleanPhone = ValidatePhoneUseCase.normalizePhone(phone).filter { it.isDigit() }
            if (cleanPhone.length < 7) {
                triggerToast("❌ رقم الهاتف غير صالح")
                return@launch
            }
            val currentTime = System.currentTimeMillis()
            val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
            val requestData = mapOf(
                "id" to cleanPhone,
                "uid" to currentUid,
                "phone" to cleanPhone,
                "name" to name.ifBlank { "صاحب الحساب ($cleanPhone)" },
                "accountType" to accountType,
                "status" to "PENDING",
                "createdAt" to currentTime,
                "requestedAt" to currentTime,
                "timestamp" to currentTime,
                "newPassword" to "",
                "passwordHash" to "",
                "adminNotes" to ""
            )

            db.collection("password_recovery_requests")
                .document(cleanPhone)
                .set(requestData, SetOptions.merge())
                .await()

            runCatching {
                db.collection("password_resets").document(cleanPhone).set(
                    mapOf(
                        "id" to cleanPhone,
                        "uid" to currentUid,
                        "phone" to cleanPhone,
                        "status" to "PENDING",
                        "newPassword" to "",
                        "passwordHash" to "",
                        "createdAt" to currentTime,
                        "requestedAt" to currentTime,
                        "timestamp" to currentTime
                    ),
                    SetOptions.merge()
                )
            }

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
            if (adminNotif.isValid()) db.collection("notifications").document(notifDocId).set(adminNotif)

            val logId = db.collection("activity_logs").document().id
            val log = ActivityLogEntity(
                id = logId,
                action = "🔑 طلب استعادة كلمة المرور للحساب: $name ($cleanPhone - $accountType)",
                timestamp = currentTime
            )
            db.collection("activity_logs").document(logId).set(log)

            triggerToast("✅ تم إرسال طلبك للإدارة. سيتم مراجعته والتواصل معك قريباً")
        } catch (e: Exception) {
            triggerToast("❌ فشل إرسال الطلب: ${e.message}")
        }
    }
}
