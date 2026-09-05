package com.example.utils

import com.example.data.SupervisorEntity
import com.example.data.AdminSettingsEntity

object AdminSecurityManager {

    const val OWNER_EMAIL = "mah73646@gmail.com"
    const val OWNER_PASSWORD = "Maher@@--@@736462##"
    const val ADMIN_EMAIL = "meh777644@gmail.com"
    const val ADMIN_PASSWORD = "Meh@@@@777644##"

    fun verifyCredentials(username: String, passwordAttempt: String, settings: AdminSettingsEntity? = null): String? {
        val trimmedUser = username.trim()
        val trimmedPass = passwordAttempt.trim()
        if (trimmedUser.isBlank() || trimmedPass.isBlank()) return null

        // 1. Check Owner
        if (trimmedUser.equals(OWNER_EMAIL, ignoreCase = true) || trimmedUser == "WAM2026" || (settings != null && trimmedUser.equals(settings.ownerEmail, ignoreCase = true))) {
            if (trimmedPass == OWNER_PASSWORD ||
                (settings != null && settings.ownerPassword.isNotBlank() && (
                    trimmedPass == settings.ownerPassword ||
                    PasswordHasher.verifyPassword(trimmedPass, settings.ownerPassword) ||
                    SecurityCryptoUtils.verifyAdminPassword(trimmedPass, settings.ownerPassword)
                ))
            ) {
                return "OWNER"
            }
        }

        // 2. Check Admin
        if (trimmedUser.equals(ADMIN_EMAIL, ignoreCase = true) || (settings != null && trimmedUser.equals(settings.adminUsername, ignoreCase = true))) {
            if (trimmedPass == ADMIN_PASSWORD || trimmedPass == OWNER_PASSWORD ||
                (settings != null && settings.adminPassword.isNotBlank() && (
                    trimmedPass == settings.adminPassword ||
                    PasswordHasher.verifyPassword(trimmedPass, settings.adminPassword) ||
                    SecurityCryptoUtils.verifyAdminPassword(trimmedPass, settings.adminPassword)
                ))
            ) {
                return "ADMIN"
            }
        }

        return null
    }

    fun isOwner(username: String, passwordAttempt: String, settings: AdminSettingsEntity): Boolean {
        return verifyCredentials(username, passwordAttempt, settings) == "OWNER"
    }

    fun isAdmin(username: String, passwordAttempt: String, settings: AdminSettingsEntity): Boolean {
        val role = verifyCredentials(username, passwordAttempt, settings)
        return role == "ADMIN" || role == "OWNER"
    }

    fun isSupervisor(username: String, passwordAttempt: String, supervisorList: List<SupervisorEntity>): SupervisorEntity? {
        val trimmedUser = username.trim()
        val trimmedPass = passwordAttempt.trim()
        if (trimmedUser.isBlank() || trimmedPass.isBlank()) return null

        return supervisorList.find { s ->
            val matchUser = s.name.trim().equals(trimmedUser, ignoreCase = true) || s.id.equals(trimmedUser, ignoreCase = true)
            val matchPass = s.passcode.isNotBlank() && (
                s.passcode.trim() == trimmedPass ||
                PasswordHasher.verifyPassword(trimmedPass, s.passcode) ||
                SecurityCryptoUtils.verifyAdminPassword(trimmedPass, s.passcode)
            )
            matchUser && matchPass
        }
    }
}

