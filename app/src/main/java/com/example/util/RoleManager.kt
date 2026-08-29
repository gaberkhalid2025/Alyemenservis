package com.example.util

enum class AdminRole {
    OWNER,
    SUPER_ADMIN,
    ADMIN,
    SUPERVISOR,
    AUDITOR,
    SUPPORT,
    OPERATIONS,
    GUEST
}

object RoleManager {
    fun fromRoleString(roleStr: String): AdminRole {
        return when (roleStr.uppercase().trim()) {
            "OWNER", "MAIN_ADMIN" -> AdminRole.OWNER
            "SUPER_ADMIN" -> AdminRole.SUPER_ADMIN
            "ADMIN" -> AdminRole.ADMIN
            "SUPERVISOR" -> AdminRole.SUPERVISOR
            "AUDITOR" -> AdminRole.AUDITOR
            "SUPPORT" -> AdminRole.SUPPORT
            "OPERATIONS" -> AdminRole.OPERATIONS
            else -> AdminRole.GUEST
        }
    }
}
