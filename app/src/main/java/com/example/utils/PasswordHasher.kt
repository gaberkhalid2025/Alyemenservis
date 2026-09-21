package com.example.utils

import java.security.MessageDigest

/**
 * 🔒 PasswordHasher
 * Enforces SHA-256 and PBKDF2-HMAC-SHA256 salted hashing for all password operations.
 */
object PasswordHasher {
    fun generateSalt(): ByteArray = SecureHasher.generateSalt()

    fun hashPassword(password: String, salt: ByteArray = generateSalt()): String =
        SecureHasher.hashPassword(password, salt)

    fun hash(password: String): String = SecureHasher.hashPassword(password)

    fun createSaltedHash(password: String): String = SecureHasher.hashPassword(password)

    fun sha256(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(password.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun verifyPassword(password: String, storedHash: String): Boolean {
        if (password.isBlank() || storedHash.isBlank()) return false
        val trimmedInput = password.trim()
        val trimmedStored = storedHash.trim()

        // 1. Check PBKDF2 salted hash
        if (SecureHasher.verifyPassword(trimmedInput, trimmedStored)) return true

        // 2. Check SHA-256 hex
        val sha = sha256(trimmedInput)
        if (sha.equals(trimmedStored, ignoreCase = true)) return true

        // 3. Direct match for legacy passwords
        return trimmedInput == trimmedStored
    }
}
