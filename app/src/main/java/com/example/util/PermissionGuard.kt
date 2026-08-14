package com.example.util

import androidx.compose.runtime.Composable

object PermissionGuard {
    fun hasPermission(role: UserRole, requiredPermission: String): Boolean {
        return when (role) {
            UserRole.OWNER -> true
            UserRole.ADMIN -> !requiredPermission.startsWith("OWNER_")
            UserRole.SUPERVISOR -> requiredPermission in listOf(
                "VIEW_REPORTS", "VIEW_STATS", "ADMIN_PANEL", "SUPERVISOR",
                "MANAGE_PROVIDERS", "MANAGE_STORES", "MANAGE_RESTAURANTS", "MANAGE_MEDICAL",
                "MANAGE_PROPERTIES", "MANAGE_JOBS", "MANAGE_JOB_APPLICANTS", "MANAGE_BOOKINGS",
                "MANAGE_CHAT", "MANAGE_NOTIFICATIONS", "MANAGE_BANNERS", "MANAGE_CATEGORIES",
                "MANAGE_CITIES", "MANAGE_REVIEWS", "VIEW_CALLS", "MANAGE_COUPONS", "MANAGE_BLOCKED",
                "MANAGE_DELETED", "MANAGE_CUSTOM_TABS", "MANAGE_THEMES", "MANAGE_ADVANCED_CHAT",
                "MANAGE_BOOKING_ROUTING", "MANAGE_CARD_CUSTOMIZER", "MANAGE_NEW_SECTIONS",
                "MANAGE_REG_FORMS", "MANAGE_VIP", "MANAGE_SUPERVISORS", "MANAGE_PAYMENTS",
                "MANAGE_BACKUP", "CLEAN_DATABASE", "MANAGE_USERS", "MANAGE_ROLES"
            )
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
