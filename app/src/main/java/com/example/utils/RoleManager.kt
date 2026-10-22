package com.example.utils

import com.example.data.models.AdminRole

/**
 * Delegated to AdminSecurityManager for unified security management.
 */
object RoleManager {
    suspend fun getCustomRoles(): List<String> = AdminSecurityManager.getCustomRoles()
    fun fromRoleString(roleStr: String): AdminRole = AdminSecurityManager.fromRoleString(roleStr)
}
