package com.example.utils

import androidx.annotation.Keep

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
