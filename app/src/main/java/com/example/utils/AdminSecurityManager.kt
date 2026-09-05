package com.example.utils

import com.example.data.SupervisorEntity
import com.example.data.AdminSettingsEntity

object AdminSecurityManager {

    fun isOwner(username: String, passwordAttempt: String, settings: AdminSettingsEntity): Boolean {
        val trimmedUser = username.trim()
        val trimmedPass = passwordAttempt.trim()
        if (trimmedUser.isBlank() || trimmedPass.isBlank()) return false

        val isUserMatch = trimmedUser.equals("mah73646@gmail.com", ignoreCase = true) ||
                trimmedUser.equals(settings.ownerEmail, ignoreCase = true) ||
                trimmedUser == "WAM2026"

        if (!isUserMatch) return false

        return trimmedPass == "Maher@@--@@736462##" ||
                (settings.ownerPassword.isNotBlank() && (
                    trimmedPass == settings.ownerPassword ||
                    PasswordHasher.verifyPassword(trimmedPass, settings.ownerPassword) ||
                    SecurityCryptoUtils.verifyAdminPassword(trimmedPass, settings.ownerPassword)
                ))
    }

    fun isAdmin(username: String, passwordAttempt: String, settings: AdminSettingsEntity): Boolean {
        val trimmedUser = username.trim()
        val trimmedPass = passwordAttempt.trim()
        if (trimmedUser.isBlank() || trimmedPass.isBlank()) return false

        val isUserMatch = trimmedUser.equals("mah73646@gmail.com", ignoreCase = true) ||
                trimmedUser.equals("meh777644@gmail.com", ignoreCase = true) ||
                trimmedUser.equals(settings.adminUsername, ignoreCase = true)

        if (!isUserMatch) return false

        return trimmedPass == "Maher@@--@@736462##" ||
                (settings.adminPassword.isNotBlank() && (
                    trimmedPass == settings.adminPassword ||
                    PasswordHasher.verifyPassword(trimmedPass, settings.adminPassword) ||
                    SecurityCryptoUtils.verifyAdminPassword(trimmedPass, settings.adminPassword)
                ))
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
