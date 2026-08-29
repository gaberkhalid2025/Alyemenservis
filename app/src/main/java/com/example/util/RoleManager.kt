package com.example.util

enum class AdminRole {
    OWNER,
    SUPER_ADMIN,
    ADMIN,
    SUPERVISOR,
    GUEST
}

object RoleManager {
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
