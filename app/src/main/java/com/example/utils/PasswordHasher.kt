package com.example.utils

/**
 * Delegated to SecureHasher to ensure backward compatibility across the app.
 */
object PasswordHasher {
    fun generateSalt(): ByteArray = SecureHasher.generateSalt()
    fun hashPassword(password: String, salt: ByteArray = generateSalt()): String = SecureHasher.hashPassword(password, salt)
    fun hash(password: String): String = SecureHasher.hashPassword(password)
    fun createSaltedHash(password: String): String = SecureHasher.hashPassword(password)
    fun verifyPassword(password: String, storedHash: String): Boolean = SecureHasher.verifyPassword(password, storedHash)
}
