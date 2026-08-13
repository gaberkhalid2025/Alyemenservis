package com.example.util

object AdminPermissions {
    val ALL_PERMISSIONS = mapOf(
        "DASHBOARD" to listOf("VIEW_STATS", "VIEW_EARNINGS", "MANAGE_WIDGETS"),
        "USERS" to listOf("VIEW_USERS", "BLOCK_USERS", "EDIT_ROLES", "DELETE_USERS"),
        "PROVIDERS" to listOf("APPROVE_PROVIDERS", "REJECT_PROVIDERS", "VERIFY_PROVIDERS", "DELETE_PROVIDERS"),
        "STORES" to listOf("MANAGE_STORES", "APPROVE_STORES", "BLOCK_STORES", "DELETE_STORES"),
        "PROPERTIES" to listOf("MANAGE_PROPERTIES", "APPROVE_PROPERTIES", "DELETE_PROPERTIES"),
        "JOBS" to listOf("MANAGE_JOBS", "APPROVE_JOBS", "DELETE_JOBS"),
        "REPORTS" to listOf("VIEW_REPORTS", "DELETE_REPORTS", "EXPORT_DATA"),
        "BACKUPS" to listOf("RESTORE_DATA", "CLEAN_DB", "AUTO_BACKUP")
    )

    fun hasPermission(section: String, action: String): Boolean {
        return ALL_PERMISSIONS[section]?.contains(action) ?: false
    }
}
