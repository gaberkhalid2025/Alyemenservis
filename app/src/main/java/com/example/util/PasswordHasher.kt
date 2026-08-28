package com.example.util

import com.example.utils.*

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Secure PBKDF2 with HmacSHA256 and unique random salts for hashing admin/owner/user passwords.
 * Prevents plain-text password storage and protects against reverse engineering dictionary attacks.
 */
object PasswordHasher {

    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ITERATIONS = 12000
    private const val KEY_LENGTH = 256
    private const val SALT_SIZE = 16

    /**
     * Generates a unique 16-byte random salt.
     */
    fun generateSalt(): ByteArray {
        val random = SecureRandom()
        val salt = ByteArray(SALT_SIZE)
        random.nextBytes(salt)
        return salt
    }

    /**
     * Hashes a password string using PBKDF2 and the provided salt.
     * Returns a Base64-encoded hash string.
     */
    fun hashPassword(password: String, salt: ByteArray): String {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        val skf = SecretKeyFactory.getInstance(ALGORITHM)
        val hash = skf.generateSecret(spec).encoded
        return Base64.encodeToString(hash, Base64.NO_WRAP)
    }

    /**
     * Helper to format salt and hash into a single stored string format: "salt:hash"
     */
    fun createSaltedHash(password: String): String {
        val salt = generateSalt()
        val saltBase64 = Base64.encodeToString(salt, Base64.NO_WRAP)
        val hashBase64 = hashPassword(password, salt)
        return "$saltBase64:$hashBase64"
    }

    private fun sha256(input: String): String {
        return try {
            val md = java.security.MessageDigest.getInstance("SHA-256")
            val bytes = md.digest(input.toByteArray(Charsets.UTF_8))
            bytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Verifies if an input password matches the stored "salt:hash" string, raw text, or sha256.
     */
    fun verifyPassword(inputPassword: String, storedSaltedHash: String): Boolean {
        if (inputPassword.isBlank() || storedSaltedHash.isBlank()) return false
        val trimmedInput = inputPassword.trim()
        val trimmedStored = storedSaltedHash.trim()
        if (trimmedInput == trimmedStored) return true
        if (sha256(trimmedInput).equals(trimmedStored, ignoreCase = true)) return true
        return try {
            if (trimmedStored.contains(":")) {
                val parts = trimmedStored.split(":")
                if (parts.size == 2) {
                    val salt = Base64.decode(parts[0], Base64.NO_WRAP)
                    val expectedHash = parts[1]
                    val computedHash = hashPassword(trimmedInput, salt)
                    return computedHash == expectedHash
                }
            }
            false
        } catch (e: Exception) {
            false
        }
    }
}
