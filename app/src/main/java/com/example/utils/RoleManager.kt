package com.example.utils

import androidx.annotation.Keep

/**
 * 📌 Architectural Note: Utility-level AdminRole enum used for security check evaluations
 * and permission mapping via AdminSecurityManager.
 */
@Keep
enum class AdminRole {
    OWNER,
    SUPER_ADMIN,
    ADMIN,
    SUPERVISOR,
    GUEST
}

/**
 * Delegated to AdminSecurityManager for unified security management.
 */
object RoleManager {
    suspend fun getCustomRoles(): List<String> = AdminSecurityManager.getCustomRoles()
    fun fromRoleString(roleStr: String): AdminRole = AdminSecurityManager.fromRoleString(roleStr)
}
