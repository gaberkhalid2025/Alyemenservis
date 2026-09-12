package com.example.utils

import com.example.data.AdminSettingsEntity
import com.example.data.SupervisorEntity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object AdminSecurityManager {

    /**
     * يتحقق من صحة بيانات الدخول (المالك، المدير، أو المشرف)
     * باستخدام التشفير الآمن والتحقق السحابي عبر Firestore
     * دون أي كلمات مرور ثابتة أو أبواب خلفية.
     */
    suspend fun verifyCredentials(
        username: String,
        passwordAttempt: String,
        settings: AdminSettingsEntity? = null
    ): String? {
        val trimmedUser = username.trim()
        val trimmedPass = passwordAttempt.trim()
        if (trimmedUser.isBlank() || trimmedPass.isBlank()) return null
        
        // 1. التحقق من إعدادات المالك والمدير الممررة
        if (settings != null) {
            // المالك
            if (settings.ownerEmail.isNotBlank() && trimmedUser.equals(settings.ownerEmail.trim(), ignoreCase = true)) {
                if (SecurityCryptoUtils.verifyAdminPassword(trimmedPass, settings.ownerPassword)) {
                    return "OWNER"
                }
            }
            // المدير
            if (settings.adminUsername.isNotBlank() && trimmedUser.equals(settings.adminUsername.trim(), ignoreCase = true)) {
                if (SecurityCryptoUtils.verifyAdminPassword(trimmedPass, settings.adminPassword)) {
                    return "ADMIN"
                }
            }
        }
        
        // 2. التحقق السحابي المباشر من Firestore
        return try {
            val db = FirebaseFirestore.getInstance()
            
            // تحقق من المشرفين
            val supDoc = db.collection("supervisors").document(trimmedUser).get().await()
            if (supDoc.exists()) {
                val storedPass = supDoc.getString("passcode") ?: ""
                if (SecurityCryptoUtils.verifyAdminPassword(trimmedPass, storedPass)) {
                    return supDoc.getString("role") ?: "SUPERVISOR"
                }
            }
            
            // تحقق من admin_users
            val adminQuery = db.collection("admin_users")
                .whereEqualTo("email", trimmedUser)
                .limit(1)
                .get()
                .await()
            if (!adminQuery.isEmpty) {
                val doc = adminQuery.documents[0]
                val storedPass = doc.getString("passwordHash") ?: doc.getString("password") ?: ""
                val role = doc.getString("role") ?: "ADMIN"
                if (SecurityCryptoUtils.verifyAdminPassword(trimmedPass, storedPass)) {
                    return role
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun isOwner(username: String, passwordAttempt: String, settings: AdminSettingsEntity? = null): Boolean {
        return verifyCredentials(username, passwordAttempt, settings) == "OWNER"
    }

    suspend fun isAdmin(username: String, passwordAttempt: String, settings: AdminSettingsEntity? = null): Boolean {
        val role = verifyCredentials(username, passwordAttempt, settings)
        return role == "ADMIN" || role == "OWNER"
    }

    suspend fun isSupervisor(username: String, passwordAttempt: String, settings: AdminSettingsEntity? = null): Boolean {
        val role = verifyCredentials(username, passwordAttempt, settings)
        return role == "SUPERVISOR" || role == "ADMIN" || role == "OWNER"
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
            val snapshot = FirebaseFirestore.getInstance()
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
