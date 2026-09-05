package com.example.utils

import com.example.data.AdminSettingsEntity
import com.example.data.SupervisorEntity

object AdminSecurityManager {
    // ========== الحسابات الثابتة ==========
    const val OWNER_EMAIL = "mah73646@gmail.com"
    const val OWNER_PASSWORD = "Maher@@--@@736462##"

    const val ADMIN_EMAIL = "meh777644@gmail.com"
    const val ADMIN_PASSWORD = "Meh@@@@777644##"

    // ========== دوال التحقق ==========
    fun verifyCredentials(
        username: String,
        passwordAttempt: String,
        settings: AdminSettingsEntity? = null
    ): String? {
        val trimmedUser = username.trim()
        val trimmedPass = passwordAttempt.trim()
        if (trimmedUser.isBlank() || trimmedPass.isBlank()) return null

        // 1. التحقق من المالك (OWNER)
        if (trimmedUser.equals(OWNER_EMAIL, ignoreCase = true) ||
            trimmedUser == "WAM2026" ||
            (settings != null && trimmedUser.equals(settings.ownerEmail, ignoreCase = true))
        ) {
            if (trimmedPass == OWNER_PASSWORD ||
                (settings != null && settings.ownerPassword.isNotBlank() && (
                    trimmedPass == settings.ownerPassword ||
                    PasswordHasher.verifyPassword(trimmedPass, settings.ownerPassword) ||
                    SecurityCryptoUtils.verifyAdminPassword(trimmedPass, settings.ownerPassword)
                )) ||
                SecurityCryptoUtils.verifyAdminPassword(trimmedPass, hashOwnerPassword()) ||
                PasswordHasher.verifyPassword(trimmedPass, hashOwnerPassword())
            ) {
                return "OWNER"
            }
        }

        // 2. التحقق من المدير (ADMIN)
        if (trimmedUser.equals(ADMIN_EMAIL, ignoreCase = true) ||
            (settings != null && trimmedUser.equals(settings.adminUsername, ignoreCase = true))
        ) {
            if (trimmedPass == ADMIN_PASSWORD ||
                trimmedPass == OWNER_PASSWORD ||
                (settings != null && settings.adminPassword.isNotBlank() && (
                    trimmedPass == settings.adminPassword ||
                    PasswordHasher.verifyPassword(trimmedPass, settings.adminPassword) ||
                    SecurityCryptoUtils.verifyAdminPassword(trimmedPass, settings.adminPassword)
                )) ||
                SecurityCryptoUtils.verifyAdminPassword(trimmedPass, hashAdminPassword()) ||
                PasswordHasher.verifyPassword(trimmedPass, hashAdminPassword())
            ) {
                return "ADMIN"
            }
        }

        return null
    }

    fun isOwner(username: String, passwordAttempt: String, settings: AdminSettingsEntity? = null): Boolean {
        return verifyCredentials(username, passwordAttempt, settings) == "OWNER"
    }

    fun isAdmin(username: String, passwordAttempt: String, settings: AdminSettingsEntity? = null): Boolean {
        val role = verifyCredentials(username, passwordAttempt, settings)
        return role == "ADMIN" || role == "OWNER"
    }

    fun isSupervisor(username: String, passwordAttempt: String, supervisors: List<SupervisorEntity>): Boolean {
        return getSupervisor(username, passwordAttempt, supervisors) != null
    }

    fun getSupervisor(username: String, passwordAttempt: String, supervisors: List<SupervisorEntity>): SupervisorEntity? {
        val trimmedUser = username.trim()
        val trimmedPass = passwordAttempt.trim()
        if (trimmedUser.isBlank() || trimmedPass.isBlank()) return null

        return supervisors.find { sup ->
            val matchUser = sup.name.trim().equals(trimmedUser, ignoreCase = true) || sup.id.equals(trimmedUser, ignoreCase = true)
            val matchPass = sup.passcode.isNotBlank() && (
                sup.passcode.trim() == trimmedPass ||
                PasswordHasher.verifyPassword(trimmedPass, sup.passcode) ||
                SecurityCryptoUtils.verifyAdminPassword(trimmedPass, sup.passcode)
            )
            matchUser && matchPass
        }
    }

    // ========== دوال التجزئة ==========
    private fun hashOwnerPassword(): String {
        return SecurityCryptoUtils.hashPassword(OWNER_PASSWORD)
    }

    private fun hashAdminPassword(): String {
        return SecurityCryptoUtils.hashPassword(ADMIN_PASSWORD)
    }

    // ========== دوال التحقق من الصلاحيات ==========
    fun hasOwnerPermission(role: String): Boolean {
        return role == "OWNER"
    }

    fun hasAdminPermission(role: String): Boolean {
        return role == "OWNER" || role == "ADMIN"
    }

    fun hasSupervisorPermission(role: String): Boolean {
        return role == "OWNER" || role == "ADMIN" || role == "SUPERVISOR"
    }
}
