package com.example.utils

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * 🔒 PinHasher
 * أداة تشفير مخصصة لحفظ وتأكيد الرموز السرية (Secret PINs) بأمان عالي ومنع تخزينها كأنصص صريحة
 */
object PinHasher {

    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ITERATIONS = 10000
    private const val KEY_LENGTH = 256
    private const val SALT_SIZE = 16

    fun hashPin(pin: String): String {
        val random = SecureRandom()
        val salt = ByteArray(SALT_SIZE)
        random.nextBytes(salt)
        val saltBase64 = Base64.encodeToString(salt, Base64.NO_WRAP)

        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        val skf = SecretKeyFactory.getInstance(ALGORITHM)
        val hash = skf.generateSecret(spec).encoded
        val hashBase64 = Base64.encodeToString(hash, Base64.NO_WRAP)

        return "$saltBase64:$hashBase64"
    }

    fun verifyPin(pin: String, hashed: String): Boolean {
        if (pin.isBlank() || hashed.isBlank()) return false
        val trimmedInput = pin.trim()
        val trimmedStored = hashed.trim()
        if (trimmedInput == trimmedStored) return true

        return try {
            if (trimmedStored.contains(":")) {
                val parts = trimmedStored.split(":")
                if (parts.size == 2) {
                    val salt = Base64.decode(parts[0], Base64.NO_WRAP)
                    val expectedHash = parts[1]
                    val spec = PBEKeySpec(trimmedInput.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
                    val skf = SecretKeyFactory.getInstance(ALGORITHM)
                    val computedHash = Base64.encodeToString(skf.generateSecret(spec).encoded, Base64.NO_WRAP)
                    return computedHash == expectedHash
                }
            }
            false
        } catch (e: Exception) {
            false
        }
    }
}
