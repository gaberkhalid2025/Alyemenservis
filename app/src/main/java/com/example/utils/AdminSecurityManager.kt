package com.example.utils

import android.content.Context
import com.example.data.AdminSettingsEntity
import com.example.data.SupervisorEntity
import com.example.data.models.AdminRole
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

/**
 * 🔒 AdminSecurityManager
 * المحرك الموحد للتحقق من أوراق اعتماد الإدارة (المالك، الأدمن، المشرفين).
 * يدعم:
 * 1. Firebase Authentication + Custom Claims
 * 2. التحقق الآمن عبر SecureAdminStorage
 * 3. التوافق الكامل مع كلمات المرور الحالية للمالك (mah73646@gmail.com) والأدمن (meh777644@gmail.com)
 */
object AdminSecurityManager {

    /**
     * التحقق الشامل من بيانات دخول الإدارة وتحديد الدور بدقة وأمان.
     */
    suspend fun verifyCredentials(
        username: String,
        passwordAttempt: String,
        settings: AdminSettingsEntity? = null,
        context: Context? = null,
        supervisors: List<SupervisorEntity> = emptyList(),
        preferredRole: String? = null
    ): String? {
        val cleanUser = username.trim()
        val cleanPass = passwordAttempt.trim()
        if (cleanUser.isBlank() || cleanPass.isBlank()) return null

        // 1️⃣ محاولة التحقق عبر Firebase Auth إذا كان المدخل بريداً إلكترونياً
        try {
            val auth = FirebaseAuth.getInstance()
            val authResult = auth.signInWithEmailAndPassword(cleanUser, cleanPass).await()
            val user = authResult.user
            if (user != null) {
                val tokenResult = user.getIdToken(true).await()
                val claims = tokenResult.claims

                val roleClaim = (claims["role"] as? String)?.uppercase()
                val isOwnerClaim = claims["isSuperAdmin"] == true ||
                        claims["isOwner"] == true ||
                        roleClaim == "OWNER" ||
                        roleClaim == "SUPER_ADMIN"
                val isAdminClaim = claims["isAdmin"] == true ||
                        claims["admin"] == true ||
                        roleClaim == "ADMIN"
                val isSupervisorClaim = roleClaim == "SUPERVISOR"

                val isExplicitOwnerEmail = cleanUser.equals("mah73646@gmail.com", ignoreCase = true) ||
                        (settings?.ownerEmail?.isNotBlank() == true && cleanUser.equals(settings.ownerEmail.trim(), ignoreCase = true))
                val isExplicitAdminEmail = cleanUser.equals("meh777644@gmail.com", ignoreCase = true) ||
                        (settings?.adminEmail?.isNotBlank() == true && cleanUser.equals(settings.adminEmail.trim(), ignoreCase = true))

                return when {
                    isOwnerClaim || isExplicitOwnerEmail -> "OWNER"
                    isAdminClaim || isExplicitAdminEmail -> "ADMIN"
                    isSupervisorClaim -> "SUPERVISOR"
                    preferredRole == "OWNER" && isExplicitOwnerEmail -> "OWNER"
                    preferredRole == "ADMIN" && isExplicitAdminEmail -> "ADMIN"
                    else -> "ADMIN"
                }
            }
        } catch (_: Exception) {
            // الاستمرار للتحقق المحلي الآمن في حال عدم توفر اتصال بالشبكة أو خطأ Auth
        }

        // 2️⃣ التحقق من SecureAdminStorage المحلي المشفر
        if (context != null) {
            if (SecureAdminStorage.verifyFallbackCredentials(context, cleanUser, cleanPass, "OWNER")) {
                return "OWNER"
            }
            if (SecureAdminStorage.verifyFallbackCredentials(context, cleanUser, cleanPass, "ADMIN")) {
                return "ADMIN"
            }
        }

        // 3️⃣ التحقق من إعدادات المالك (Owner)
        val ownerEmail = settings?.ownerEmail?.trim() ?: ""
        val ownerPass = settings?.ownerPassword?.trim() ?: ""
        val isOwnerUser = cleanUser.equals("mah73646@gmail.com", ignoreCase = true) ||
                (ownerEmail.isNotBlank() && cleanUser.equals(ownerEmail, ignoreCase = true))

        if (isOwnerUser && ownerPass.isNotBlank()) {
            val passMatch = SecureHasher.verifyPassword(cleanPass, ownerPass) ||
                    SecurityCryptoUtils.verifyAdminPassword(cleanPass, ownerPass) ||
                    cleanPass == ownerPass
            if (passMatch) return "OWNER"
        }

        // 4️⃣ التحقق من إعدادات الأدمن (Admin)
        val adminEmail = settings?.adminEmail?.trim() ?: ""
        val adminPass = settings?.adminPassword?.trim() ?: ""
        val isAdminUser = cleanUser.equals("meh777644@gmail.com", ignoreCase = true) ||
                (adminEmail.isNotBlank() && cleanUser.equals(adminEmail, ignoreCase = true))

        if (isAdminUser && adminPass.isNotBlank()) {
            val passMatch = SecureHasher.verifyPassword(cleanPass, adminPass) ||
                    SecurityCryptoUtils.verifyAdminPassword(cleanPass, adminPass) ||
                    cleanPass == adminPass
            if (passMatch) return "ADMIN"
        }

        // 5️⃣ التحقق من المشرفين (Supervisors)
        val matchingSup = supervisors.find {
            it.id.equals(cleanUser, ignoreCase = true) ||
                    it.name.trim().equals(cleanUser, ignoreCase = true)
        }
        if (matchingSup != null && matchingSup.passcode.isNotBlank()) {
            val storedPass = matchingSup.passcode.trim()
            val passMatch = SecureHasher.verifyPassword(cleanPass, storedPass) ||
                    SecurityCryptoUtils.verifyAdminPassword(cleanPass, storedPass) ||
                    cleanPass == storedPass
            if (passMatch) {
                val supRole = matchingSup.role.uppercase().trim()
                return if (supRole.isNotBlank()) supRole else "SUPERVISOR"
            }
        }

        return null
    }

    /**
     * واجهة التحقق المباشرة عبر Result<String> للنداءات المباشرة.
     */
    suspend fun verifyCredentials(
        email: String,
        password: String
    ): Result<String> {
        val role = verifyCredentials(
            username = email,
            passwordAttempt = password,
            settings = null,
            context = null,
            supervisors = emptyList(),
            preferredRole = null
        )
        return if (role != null) {
            Result.success(role)
        } else {
            Result.failure(SecurityException("بيانات الدخول غير صحيحة أو غير مصرح لك"))
        }
    }

    suspend fun isOwner(email: String, password: String): Boolean {
        return verifyCredentials(email, password).getOrNull() == "OWNER"
    }

    suspend fun isAdmin(email: String, password: String): Boolean {
        return verifyCredentials(email, password).getOrNull()?.let {
            it == "ADMIN" || it == "OWNER"
        } ?: false
    }

    suspend fun isSupervisor(email: String, password: String): Boolean {
        return verifyCredentials(email, password).isSuccess
    }

    fun hasOwnerPermission(role: String): Boolean {
        return role == "OWNER"
    }

    fun hasAdminPermission(role: String): Boolean {
        return role == "OWNER" || role == "ADMIN"
    }

    fun hasSupervisorPermission(role: String): Boolean {
        return role == "OWNER" || role == "ADMIN" || role == "SUPERVISOR"
    }

    suspend fun getCustomRoles(): List<String> {
        return try {
            val snapshot = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("settings")
                .document("roles")
                .get()
                .await()
            @Suppress("UNCHECKED_CAST")
            val roles = snapshot.get("roles") as? List<String> ?: emptyList()
            if (roles.isNotEmpty()) roles else listOf("OWNER", "SUPER_ADMIN", "ADMIN", "SUPERVISOR", "GUEST")
        } catch (e: Exception) {
            listOf("OWNER", "SUPER_ADMIN", "ADMIN", "SUPERVISOR", "GUEST")
        }
    }

    fun fromRoleString(roleStr: String): AdminRole {
        return when (roleStr.uppercase().trim()) {
            "OWNER", "MAIN_ADMIN" -> AdminRole.OWNER
            "SUPER_ADMIN" -> AdminRole.SUPER_ADMIN
            "ADMIN" -> AdminRole.ADMIN
            "SUPERVISOR", "SUPPORT", "AUDITOR", "OPERATIONS" -> AdminRole.SUPERVISOR
            else -> AdminRole.GUEST
        }
    }
}
