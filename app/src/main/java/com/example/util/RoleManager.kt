package com.example.util

enum class AdminRole {
    SUPER_ADMIN,
    ADMIN,
    SUPERVISOR,
    GUEST
}

object RoleManager {
    fun fromRoleString(roleStr: String): AdminRole {
        return when (roleStr.uppercase()) {
            "SUPER_ADMIN" -> AdminRole.SUPER_ADMIN
            "ADMIN" -> AdminRole.ADMIN
            "SUPERVISOR" -> AdminRole.SUPERVISOR
            else -> AdminRole.GUEST
        }
    }
}
