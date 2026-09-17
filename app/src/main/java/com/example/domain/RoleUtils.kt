package com.example.domain

enum class AdminRole(val roleId: String) {
    OWNER("OWNER"),
    ADMIN("ADMIN"),
    SUPER_ADMIN("SUPER_ADMIN"),
    MAIN_ADMIN("MAIN_ADMIN"),
    SUPERVISOR("SUPERVISOR"),
    AUDITOR("AUDITOR"),
    SUPPORT("SUPPORT"),
    OPERATIONS("OPERATIONS"),
    GUEST("GUEST");

    companion object {
        fun fromString(value: String): AdminRole =
            entries.firstOrNull { it.roleId.equals(value, ignoreCase = true) } ?: GUEST
    }
}

fun String.toAdminRole(): AdminRole = AdminRole.fromString(this)

fun String.isAdmin(): Boolean {
    val role = toAdminRole()
    return role == AdminRole.OWNER ||
           role == AdminRole.ADMIN ||
           role == AdminRole.SUPER_ADMIN ||
           role == AdminRole.MAIN_ADMIN ||
           this == "ADMIN" || this == "SUPER_ADMIN" || this == "MAIN_ADMIN" || this == "OWNER"
}

fun String.isSupervisor(): Boolean = toAdminRole() == AdminRole.SUPERVISOR
fun String.isOwner(): Boolean = toAdminRole() == AdminRole.OWNER
fun String.canManageContent(): Boolean = isAdmin() || isSupervisor()
