package com.example.utils

import com.example.data.AdminSettingsEntity
import com.example.data.SupervisorEntity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.example.data.models.AdminRole

object AdminSecurityManager {

    /**
     * // ✨ إصلاح المرحلة 1.5: التحقق الآمن عبر Cloud Functions و Firebase Auth
     * يتحقق من صحة بيانات الدخول (المالك، المدير، أو المشرف)
     * باستخدام التشفير الآمن والتحقق السحابي عبر Cloud Functions / Firestore
     */
    suspend fun verifyCredentials(
        username: String,
        passwordAttempt: String,
        settings: AdminSettingsEntity? = null,
        context: android.content.Context? = null,
        supervisors: List<SupervisorEntity> = emptyList(),
        preferredRole: String? = null
    ): String? {
        val trimmedUser = username.trim()
        val trimmedPass = passwordAttempt.trim()
        if (trimmedUser.isBlank() || trimmedPass.isBlank()) return null

        // 1. المصادقة الآمنة عبر Firebase Auth المباشر والـ Custom Claims
        try {
            if (trimmedUser.contains("@")) {
                val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
                val authResult = auth.signInWithEmailAndPassword(trimmedUser, trimmedPass).await()
                val user = authResult.user
                if (user != null) {
                    // بمجرد نجاح مصادقة Firebase Auth، نضع الفحوصات الإضافية في try-catch داخلي
                    // حتى لا يتسبب خطأ PERMISSION_DENIED من Firestore في إلغاء تسجيل الدخول الناجح
                    try {
                        val tokenResult = user.getIdToken(true).await()
                        val claims = tokenResult.claims

                        val isOwnerClaim = claims["isSuperAdmin"] == true ||
                                claims["isOwner"] == true ||
                                claims["role"]?.toString()?.uppercase() in listOf("OWNER", "SUPER_ADMIN")
                        val isAdminClaim = claims["isAdmin"] == true ||
                                claims["admin"] == true ||
                                claims["role"]?.toString()?.uppercase() == "ADMIN"

                        if (isOwnerClaim) return "OWNER"
                        if (isAdminClaim && preferredRole != "OWNER") return "ADMIN"
                    } catch (_: Exception) {}

                    try {
                        val db = FirebaseFirestore.getInstance()
                        val adminDoc = db.collection("admin_users").document(user.uid).get().await()
                        if (adminDoc.exists()) {
                            val role = adminDoc.getString("role")?.uppercase() ?: "ADMIN"
                            return if (role == "OWNER" || role == "SUPER_ADMIN") "OWNER" else "ADMIN"
                        }
                    } catch (_: Exception) {}

                    try {
                        val db = FirebaseFirestore.getInstance()
                        val adminDoc = db.collection("admins").document(user.uid).get().await()
                        if (adminDoc.exists()) {
                            val role = adminDoc.getString("role")?.uppercase() ?: "ADMIN"
                            return if (role == "OWNER" || role == "SUPER_ADMIN") "OWNER" else "ADMIN"
                        }
                    } catch (_: Exception) {}

                    val isExplicitOwnerEmail = trimmedUser.equals("mah73646@gmail.com", ignoreCase = true) ||
                            (settings?.ownerEmail?.isNotBlank() == true && trimmedUser.equals(settings.ownerEmail.trim(), ignoreCase = true))
                    if (isExplicitOwnerEmail) return "OWNER"

                    val isExplicitAdminEmail = settings?.adminUsername?.isNotBlank() == true &&
                            trimmedUser.equals(settings.adminUsername.trim(), ignoreCase = true)
                    if (isExplicitAdminEmail && preferredRole != "OWNER") return "ADMIN"

                    return when (preferredRole?.uppercase()) {
                        "OWNER" -> "OWNER"
                        "ADMIN" -> "ADMIN"
                        else -> "OWNER"
                    }
                }
            }
        } catch (_: Exception) {
            // الاستمرار في طبقات التحقق التالية عند عدم تطابق Firebase Auth أو عدم توفر اتصال
        }

        // 2. التحقق من بيانات الإعدادات (AdminSettingsEntity) المحملة أو السحابية المباشرة
        try {
            val snap = try {
                FirebaseFirestore.getInstance().collection("settings").document("main_settings").get().await()
            } catch (_: Exception) {
                null
            }
            val snapObj = snap?.toObject(AdminSettingsEntity::class.java)
            val effectiveSettings = settings ?: snapObj

            val docOwnerPass = snap?.getString("ownerPassword") ?: snap?.getString("owner_password") ?: effectiveSettings?.ownerPassword ?: ""
            val docOwnerEmail = snap?.getString("ownerEmail") ?: snap?.getString("owner_email") ?: effectiveSettings?.ownerEmail ?: ""
            val docAdminPass = snap?.getString("adminPassword") ?: snap?.getString("admin_password") ?: effectiveSettings?.adminPassword ?: ""
            val docAdminUser = snap?.getString("adminUsername") ?: snap?.getString("admin_username") ?: effectiveSettings?.adminUsername ?: ""

            if (docOwnerPass.isNotBlank()) {
                val ownerUserMatches = docOwnerEmail.isBlank() ||
                        trimmedUser.equals(docOwnerEmail.trim(), ignoreCase = true) ||
                        trimmedUser.equals("mah73646@gmail.com", ignoreCase = true) ||
                        trimmedUser.equals("owner", ignoreCase = true) ||
                        trimmedUser.equals("admin", ignoreCase = true) ||
                        preferredRole == "OWNER"
                if (ownerUserMatches && SecurityCryptoUtils.verifyAdminPassword(trimmedPass, docOwnerPass)) {
                    return "OWNER"
                }
            }

            if (docAdminPass.isNotBlank()) {
                val adminUserMatches = docAdminUser.isBlank() ||
                        trimmedUser.equals(docAdminUser.trim(), ignoreCase = true) ||
                        trimmedUser.equals("admin", ignoreCase = true) ||
                        preferredRole == "ADMIN"
                if (adminUserMatches && SecurityCryptoUtils.verifyAdminPassword(trimmedPass, docAdminPass)) {
                    return "ADMIN"
                }
            }
        } catch (_: Exception) {}

        // 3. التحقق من الخزنة المشفرة المحلية (SecureAdminStorage) للوصول الطارئ أو بدون إنترنت
        if (context != null) {
            try {
                if (SecureAdminStorage.verifyFallbackCredentials(context, trimmedUser, trimmedPass, "OWNER")) {
                    return "OWNER"
                }
                if (SecureAdminStorage.verifyFallbackCredentials(context, trimmedUser, trimmedPass, "ADMIN")) {
                    return "ADMIN"
                }
            } catch (_: Exception) {}
        }

        // 4. التحقق من قائمة المشرفين المحملة في الذاكرة
        try {
            if (supervisors.isNotEmpty()) {
                val matchingSup = supervisors.find {
                    it.id.equals(trimmedUser, ignoreCase = true) ||
                            it.name.trim().equals(trimmedUser, ignoreCase = true)
                }
                if (matchingSup != null && SecurityCryptoUtils.verifyAdminPassword(trimmedPass, matchingSup.passcode)) {
                    val r = matchingSup.role.uppercase().trim()
                    return when {
                        r.contains("OWNER") || r == "MAIN_ADMIN" || r == "SUPER_ADMIN" -> "OWNER"
                        r == "ADMIN" -> "ADMIN"
                        else -> "SUPERVISOR"
                    }
                }
            }
        } catch (_: Exception) {}

        // 5. محاولة التحقق عبر Cloud Function "verifyAdminLogin" كطبقة إضافية
        try {
            if (trimmedUser.contains("@")) {
                val functions = com.google.firebase.functions.FirebaseFunctions.getInstance()
                val result = functions.getHttpsCallable("verifyAdminLogin")
                    .call(mapOf("email" to trimmedUser, "password" to trimmedPass))
                    .await()
                val data = result.data as? Map<*, *>
                if (data != null && data["success"] == true) {
                    val isSuperAdmin = data["isSuperAdmin"] == true
                    val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                    val token = try { user?.getIdToken(false)?.await() } catch (_: Exception) { null }
                    val claims = token?.claims
                    return when {
                        isSuperAdmin || claims?.get("isSuperAdmin") == true || claims?.get("isOwner") == true -> "OWNER"
                        preferredRole == "OWNER" -> "OWNER"
                        claims?.get("isAdmin") == true -> "ADMIN"
                        else -> "ADMIN"
                    }
                }
            }
        } catch (_: Exception) {}

        // 6. التحقق السحابي المباشر من Firestore للمشرفين والمستخدمين الإداريين (كل استعلام مستقل)
        val db = try { FirebaseFirestore.getInstance() } catch (_: Exception) { return null }

        // 6.أ: فحص المشرفين عبر المعرف المباشر أو الاسم أو البريد
        try {
            var supDoc = db.collection("supervisors").document(trimmedUser).get().await()
            if (!supDoc.exists()) {
                val byEmail = db.collection("supervisors").whereEqualTo("email", trimmedUser).limit(1).get().await()
                if (!byEmail.isEmpty) {
                    supDoc = byEmail.documents[0]
                } else {
                    val byName = db.collection("supervisors").whereEqualTo("name", trimmedUser).limit(1).get().await()
                    if (!byName.isEmpty) {
                        supDoc = byName.documents[0]
                    }
                }
            }
            if (supDoc.exists()) {
                val storedPass = supDoc.getString("passcode") ?: ""
                if (AdminCredentialsVault.verifyAndMigrate(supDoc.reference, trimmedPass, storedPass, "passcode")) {
                    val r = (supDoc.getString("role") ?: "SUPERVISOR").uppercase().trim()
                    return when {
                        r.contains("OWNER") || r == "MAIN_ADMIN" || r == "SUPER_ADMIN" -> "OWNER"
                        r == "ADMIN" -> "ADMIN"
                        else -> "SUPERVISOR"
                    }
                }
            }
        } catch (_: Exception) {}

        // 6.ب: فحص جدول admin_users
        try {
            var adminQuery = db.collection("admin_users").whereEqualTo("email", trimmedUser).limit(1).get().await()
            if (adminQuery.isEmpty) {
                adminQuery = db.collection("admin_users").whereEqualTo("username", trimmedUser).limit(1).get().await()
            }
            if (!adminQuery.isEmpty) {
                val doc = adminQuery.documents[0]
                val storedPass = doc.getString("passwordHash") ?: doc.getString("password") ?: doc.getString("passcode") ?: ""
                val role = (doc.getString("role") ?: "ADMIN").uppercase().trim()
                val fieldName = if (doc.contains("passwordHash")) "passwordHash" else "password"
                if (AdminCredentialsVault.verifyAndMigrate(doc.reference, trimmedPass, storedPass, fieldName)) {
                    return if (role.contains("OWNER") || role == "SUPER_ADMIN") "OWNER" else "ADMIN"
                }
            }
        } catch (_: Exception) {}

        // 6.ج: فحص جدول admins
        try {
            var adminsQuery = db.collection("admins").whereEqualTo("email", trimmedUser).limit(1).get().await()
            if (adminsQuery.isEmpty) {
                adminsQuery = db.collection("admins").whereEqualTo("username", trimmedUser).limit(1).get().await()
            }
            if (!adminsQuery.isEmpty) {
                val doc = adminsQuery.documents[0]
                val storedPass = doc.getString("passwordHash") ?: doc.getString("password") ?: doc.getString("passcode") ?: ""
                val role = (doc.getString("role") ?: "ADMIN").uppercase().trim()
                val fieldName = if (doc.contains("passwordHash")) "passwordHash" else "password"
                if (AdminCredentialsVault.verifyAndMigrate(doc.reference, trimmedPass, storedPass, fieldName)) {
                    return if (role.contains("OWNER") || role == "SUPER_ADMIN") "OWNER" else "ADMIN"
                }
            }
        } catch (_: Exception) {}

        return null
    }

    suspend fun isOwner(username: String, passwordAttempt: String, settings: AdminSettingsEntity? = null): Boolean {
        return verifyCredentials(username, passwordAttempt, settings, preferredRole = "OWNER") == "OWNER"
    }

    suspend fun isAdmin(username: String, passwordAttempt: String, settings: AdminSettingsEntity? = null): Boolean {
        val role = verifyCredentials(username, passwordAttempt, settings, preferredRole = "ADMIN")
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
