package com.example.ui.viewmodels

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.viewModelScope
import com.example.data.PendingProviderEntity
import com.example.data.ProviderEntity
import com.example.data.StoreEntity
import com.example.domain.usecases.ValidatePhoneUseCase
import com.example.ui.LocaleManager
import com.example.utils.SecureStorage
import com.example.utils.SecurityCryptoUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.security.SecureRandom

internal fun AuthViewModel.updateUserFcmTokenHelper(
    userId: String,
    token: String,
    currentUserPhone: String
) {
    if (userId.isEmpty() || userId == "guest" || token.isBlank()) return
    viewModelScope.launch {
        try {
            val now = System.currentTimeMillis()
            val cleanPhone = ValidatePhoneUseCase.normalizePhone(currentUserPhone).filter { it.isDigit() }
            val authUid = FirebaseAuth.getInstance().currentUser?.uid
            val targetTokenDocId = authUid?.takeIf { it.isNotBlank() } ?: userId
            val tokenPayload = buildMap<String, Any> {
                put("token", token)
                put("userId", targetTokenDocId)
                put("appUserId", userId)
                put("isActive", true)
                put("role", _adminRole.value.ifBlank { "CLIENT" })
                put("updatedAt", now)
                if (cleanPhone.isNotBlank()) put("phone", cleanPhone)
            }
            db.collection("fcm_tokens").document(targetTokenDocId)
                .set(tokenPayload, SetOptions.merge())
            runCatching {
                val tokenHash = MessageDigest.getInstance("SHA-256")
                    .digest(token.toByteArray(Charsets.UTF_8))
                    .joinToString("") { "%02x".format(it) }
                    .take(32)
                db.collection("fcm_tokens").document(targetTokenDocId)
                    .collection("devices").document(tokenHash)
                    .set(tokenPayload, SetOptions.merge())
            }

            db.collection("registered_users").document(userId).get().addOnSuccessListener { doc ->
                if (doc != null && doc.exists()) {
                    doc.reference.update("fcmToken", token)
                }
            }
            if (cleanPhone.length >= 7) {
                val phoneVariants = listOf(cleanPhone, "0$cleanPhone", "+967$cleanPhone", "967$cleanPhone", "00967$cleanPhone")
                    .distinct()
                    .take(10)
                for (col in listOf("providers", "stores", "properties", "users", "registered_users")) {
                    db.collection(col)
                        .whereIn("phone", phoneVariants)
                        .limit(5)
                        .get()
                        .addOnSuccessListener { snaps ->
                            for (doc in snaps.documents) {
                                doc.reference.update("fcmToken", token)
                            }
                        }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AuthViewModel", "Error updating fcmToken: ", e)
        }
    }
}

internal fun AuthViewModel.getOrGenerateUserIdHelper(): String {
    var current = _currentUserId.value
    if (current.isBlank() || current == "guest") {
        val sp = cachedAppContext?.getSharedPreferences("yemen_service_prefs", Context.MODE_PRIVATE)
        val savedGuest = sp?.getString("persistent_guest_id", "")?.takeIf { it.isNotBlank() }?.let {
            SecurityCryptoUtils.decrypt(it).ifBlank {
                if (!it.startsWith("gcm:") && !it.startsWith("gcmx:") && !it.startsWith("enc::")) it else ""
            }
        }.orEmpty()
        current = if (savedGuest.isNotBlank() && savedGuest != "guest") {
            savedGuest
        } else {
            "USR_GUEST_" + (SecureRandom().nextInt(900_000) + 100_000).toString()
        }
        _currentUserId.value = current
        if (sp != null) {
            val enc = SecurityCryptoUtils.encrypt(current)
            sp.edit()
                .putString("persistent_guest_id", enc)
                .putString("user_id", enc)
                .apply()
        }
    }
    return current
}

private fun decryptPrefOrFallback(raw: String): String {
    if (raw.isEmpty()) return ""
    return try {
        SecurityCryptoUtils.decrypt(raw).ifBlank {
            if (!raw.startsWith("gcm:") && !raw.startsWith("gcmx:") && !raw.startsWith("enc::")) raw else ""
        }
    } catch (_: Throwable) {
        raw
    }
}

internal fun AuthViewModel.initializeUserIdentityHelper(
    context: Context,
    onFavoritesLoaded: ((Set<String>) -> Unit)? = null
) {
    cachedAppContext = context.applicationContext
    LocaleManager.init(context)
    val sp = context.getSharedPreferences("yemen_service_prefs", Context.MODE_PRIVATE)

    val rawId = sp.getString("user_id", "guest") ?: "guest"
    var savedId = if (rawId != "guest" && rawId.isNotEmpty()) decryptPrefOrFallback(rawId) else rawId
    val savedName = decryptPrefOrFallback(sp.getString("user_name", "") ?: "")
    val savedPhone = decryptPrefOrFallback(sp.getString("user_phone", "") ?: "")
    val savedResidence = decryptPrefOrFallback(sp.getString("user_residence", "") ?: "")

    if (savedId == "guest" || savedId.isEmpty()) {
        if (savedPhone.isNotEmpty()) {
            val cleanP = savedPhone.filter { it.isDigit() }
            savedId = "USR-" + (if (cleanP.length >= 6) cleanP.takeLast(6) else (SecureRandom().nextInt(900_000) + 100_000).toString())
        } else {
            val persistentGuest = sp.getString("persistent_guest_id", "") ?: ""
            if (persistentGuest.isNotEmpty()) {
                savedId = SecurityCryptoUtils.decrypt(persistentGuest)
            }
            if (savedId.isEmpty() || savedId == "guest") {
                savedId = "USR_GUEST_" + (SecureRandom().nextInt(900_000) + 100_000).toString()
                val enc = SecurityCryptoUtils.encrypt(savedId)
                sp.edit().putString("persistent_guest_id", enc).apply()
            }
        }
        sp.edit().putString("user_id", SecurityCryptoUtils.encrypt(savedId)).apply()
    }

    _currentUserId.value = savedId
    _currentUserName.value = savedName
    _currentUserPhone.value = savedPhone
    _currentUserResidence.value = savedResidence

    val rawJoinPhone = sp.getString("join_request_phone", "") ?: ""
    val savedJoinPhone = if (rawJoinPhone.isNotBlank()) decryptPrefOrFallback(rawJoinPhone) else ""
    _joinRequestPhone.value = savedJoinPhone

    val rawWaitingPhone = sp.getString("password_recovery_waiting_phone", "") ?: ""
    if (rawWaitingPhone.isNotBlank()) {
        val decryptedWaiting = decryptPrefOrFallback(rawWaitingPhone)
        if (decryptedWaiting.isNotBlank()) {
            _passwordRecoveryWaitingPhone.value = decryptedWaiting
        }
    }

    val secureStorage = SecureStorage(context)
    val session = secureStorage.getAdminSession()
    if (session != null) {
        val isExpired = System.currentTimeMillis() - session.loginTime > 30L * 24 * 60 * 60 * 1000
        if (!isExpired) {
            _adminRole.value = session.role
            if (session.role == "OWNER") {
                _currentSupervisorPermissions.value = listOf("ALL")
            } else if (session.role == "SUPERVISOR") {
                if (session.permissions.isNotEmpty()) {
                    _currentSupervisorPermissions.value = session.permissions
                }
                viewModelScope.launch {
                    try {
                        val firestoreDb = FirebaseFirestore.getInstance()
                        val doc = firestoreDb.collection("supervisors").document(session.uid).get().await()
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
        _adminRole.value = "GUEST"
    }

    try {
        val savedFavs = sp.getStringSet("favorite_ids_set", emptySet()) ?: emptySet()
        if (savedFavs.isNotEmpty()) {
            onFavoritesLoaded?.invoke(savedFavs)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    autoSyncProviderCredentialsHelper(savedJoinPhone, sp)
}

internal fun AuthViewModel.autoSyncProviderCredentialsHelper(
    savedJoinPhone: String,
    sp: SharedPreferences
) {
    val phoneToLookup = ValidatePhoneUseCase.normalizePhone(
        savedJoinPhone.ifEmpty { _currentUserPhone.value }
    ).filter { it.isDigit() }
    if (phoneToLookup.length < 7) return

    db.collection("providers").whereEqualTo("phone", phoneToLookup).get().addOnSuccessListener { snapshot ->
        if (snapshot != null && !snapshot.isEmpty) {
            val candidates = snapshot.documents.mapNotNull { doc ->
                doc.toObject(ProviderEntity::class.java)?.let { entity ->
                    if (entity.id.isBlank()) entity.copy(id = doc.id) else entity
                }
            }
            val prov = candidates.firstOrNull { !it.isDeleted } ?: candidates.firstOrNull()
            if (prov != null) {
                _currentUserId.value = prov.id
                _currentUserName.value = prov.name
                _currentUserPhone.value = prov.phone
                _currentUserResidence.value = prov.area
                if (_adminRole.value !in listOf("OWNER", "ADMIN", "SUPERVISOR")) {
                    _adminRole.value = "PROVIDER"
                }
                sp.edit().apply {
                    putString("user_id", SecurityCryptoUtils.encrypt(prov.id))
                    putString("user_name", SecurityCryptoUtils.encrypt(prov.name))
                    putString("user_phone", SecurityCryptoUtils.encrypt(prov.phone))
                    putString("user_residence", SecurityCryptoUtils.encrypt(prov.area))
                    apply()
                }
            }
        } else {
            db.collection("stores").whereEqualTo("phone", phoneToLookup).get().addOnSuccessListener { sSnap ->
                if (sSnap != null && !sSnap.isEmpty) {
                    val candidates = sSnap.documents.mapNotNull { doc ->
                        doc.toObject(StoreEntity::class.java)?.let { entity ->
                            if (entity.id.isBlank()) entity.copy(id = doc.id) else entity
                        }
                    }
                    val st = candidates.firstOrNull { !it.isDeleted } ?: candidates.firstOrNull()
                    if (st != null) {
                        _currentUserId.value = st.id
                        _currentUserName.value = st.name
                        _currentUserPhone.value = st.phone
                        _currentUserResidence.value = st.cityId
                        if (_adminRole.value !in listOf("OWNER", "ADMIN", "SUPERVISOR")) {
                            _adminRole.value = "STORE_OWNER"
                        }
                        sp.edit().apply {
                            putString("user_id", SecurityCryptoUtils.encrypt(st.id))
                            putString("user_name", SecurityCryptoUtils.encrypt(st.name))
                            putString("user_phone", SecurityCryptoUtils.encrypt(st.phone))
                            putString("user_residence", SecurityCryptoUtils.encrypt(st.cityId))
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
                                    putString("user_id", SecurityCryptoUtils.encrypt(pendId))
                                    putString("user_name", SecurityCryptoUtils.encrypt(pend.name))
                                    putString("user_phone", SecurityCryptoUtils.encrypt(pend.phone))
                                    putString("user_residence", SecurityCryptoUtils.encrypt(pend.area))
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

internal fun AuthViewModel.setUserSessionDetailsHelper(
    context: Context,
    name: String,
    phone: String,
    residence: String = "اليمن"
) {
    val normalized = ValidatePhoneUseCase.normalizePhone(phone).filter { it.isDigit() }
    val finalPhone = normalized.ifBlank { phone.trim() }
    _currentUserName.value = name.ifBlank { "عميل" }
    _currentUserPhone.value = finalPhone
    _currentUserResidence.value = residence.ifBlank { "اليمن" }
    if (_currentUserId.value.isEmpty() || _currentUserId.value == "guest" || _currentUserId.value.startsWith("USR_GUEST_")) {
        _currentUserId.value = "user_" + (if (finalPhone.length >= 6) finalPhone.takeLast(6) else (SecureRandom().nextInt(900_000) + 100_000).toString())
    }
    _joinRequestPhone.value = finalPhone
    val sp = context.getSharedPreferences("yemen_service_prefs", Context.MODE_PRIVATE)
    sp.edit().apply {
        putString("user_name", SecurityCryptoUtils.encrypt(_currentUserName.value))
        putString("user_phone", SecurityCryptoUtils.encrypt(finalPhone))
        putString("user_residence", SecurityCryptoUtils.encrypt(_currentUserResidence.value))
        putString("user_id", SecurityCryptoUtils.encrypt(_currentUserId.value))
        putString("join_request_phone", SecurityCryptoUtils.encrypt(finalPhone))
        apply()
    }
    try {
        FirebaseCrashlytics.getInstance().setUserId(_currentUserId.value)
        FirebaseCrashlytics.getInstance().log("Session updated for user ${_currentUserId.value}")
    } catch (_: Exception) {
    }
}
