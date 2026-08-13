package com.example.util

import androidx.compose.runtime.Composable

object PermissionGuard {
    fun hasPermission(role: UserRole, requiredPermission: String): Boolean {
        return when (role) {
            UserRole.OWNER -> true
            UserRole.ADMIN -> !requiredPermission.startsWith("OWNER_")
            UserRole.SUPERVISOR -> requiredPermission in listOf("VIEW_REPORTS", "VIEW_STATS", "ADMIN_PANEL", "SUPERVISOR")
            else -> false
        }
    }

    fun checkAccess(role: UserRole, requiredPermission: String, onGranted: () -> Unit, onDenied: () -> Unit) {
        if (hasPermission(role, requiredPermission)) {
            onGranted()
        } else {
            onDenied()
        }
    }

    @Composable
    fun GuardContent(
        role: UserRole,
        requiredPermission: String,
        onGranted: @Composable () -> Unit,
        onDenied: @Composable () -> Unit = {}
    ) {
        if (hasPermission(role, requiredPermission)) {
            onGranted()
        } else {
            onDenied()
        }
    }
}
