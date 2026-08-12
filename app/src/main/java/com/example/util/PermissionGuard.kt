package com.example.util

object PermissionGuard {
    fun checkAccess(role: UserRole, requiredPermission: String, onGranted: () -> Unit, onDenied: () -> Unit) {
        val hasAccess = when (role) {
            UserRole.OWNER -> true
            UserRole.ADMIN -> !requiredPermission.startsWith("OWNER_")
            UserRole.SUPERVISOR -> requiredPermission == "VIEW_REPORTS" || requiredPermission == "VIEW_STATS"
            else -> false
        }

        if (hasAccess) {
            onGranted()
        } else {
            onDenied()
        }
    }
}
