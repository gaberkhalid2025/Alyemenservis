package com.example.utils

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * 🔒 SecureHasher
 * المحرك الموحد الآمن لتشفير والتحقق من كلمة المرور والرموز السرية (PBKDF2WithHmacSHA256)
 * محمي ضد هجمات التوقيت (Timing Attacks) عبر المقارنة الثابتة زمنياً (MessageDigest.isEqual)
 */
object SecureHasher {

    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ITERATIONS_PASSWORD = 12000
    private const val ITERATIONS_PIN = 10000
    private const val KEY_LENGTH = 256
    private const val SALT_SIZE = 16

    fun generateSalt(): ByteArray {
        val random = SecureRandom()
        val salt = ByteArray(SALT_SIZE)
        random.nextBytes(salt)
        return salt
    }

    private fun base64Encode(bytes: ByteArray): String {
        return try {
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Throwable) {
            java.util.Base64.getEncoder().encodeToString(bytes)
        }
    }

    private fun base64Decode(str: String): ByteArray {
        return try {
            Base64.decode(str, Base64.NO_WRAP)
        } catch (e: Throwable) {
            java.util.Base64.getDecoder().decode(str)
        }
    }

    private fun constantTimeEquals(a: String, b: String): Boolean {
        return MessageDigest.isEqual(
            a.toByteArray(Charsets.UTF_8),
            b.toByteArray(Charsets.UTF_8)
        )
    }

    fun hashPassword(password: String, salt: ByteArray = generateSalt()): String {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS_PASSWORD, KEY_LENGTH)
        return try {
            val skf = SecretKeyFactory.getInstance(ALGORITHM)
            val hash = skf.generateSecret(spec).encoded
            val saltBase64 = base64Encode(salt)
            val hashBase64 = base64Encode(hash)
            "$saltBase64:$hashBase64"
        } finally {
            spec.clearPassword()
        }
    }

    fun verifyPassword(password: String, storedHash: String): Boolean {
        if (password.isBlank() || storedHash.isBlank()) return false
        val trimmedInput = password.trim()
        val trimmedStored = storedHash.trim()

        // 🎯 أمان: لا نقبل النص الصريح أبداً.
        // إذا كانت القيمة المخزنة لا تحتوي على الفاصل ":" → رفض.
        if (!trimmedStored.contains(":")) {
            return false
        }

        return try {
            val parts = trimmedStored.split(":")
            if (parts.size != 2) return false
            val salt = base64Decode(parts[0])
            val expectedHashBytes = base64Decode(parts[1])
            val spec = PBEKeySpec(trimmedInput.toCharArray(), salt, ITERATIONS_PASSWORD, KEY_LENGTH)
            try {
                val skf = SecretKeyFactory.getInstance(ALGORITHM)
                val actualHashBytes = skf.generateSecret(spec).encoded
                MessageDigest.isEqual(actualHashBytes, expectedHashBytes)
            } finally {
                spec.clearPassword()
            }
        } catch (e: Exception) {
            false
        }
    }

    fun hashPin(pin: String, salt: ByteArray = generateSalt()): String {
        val saltBase64 = base64Encode(salt)
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS_PIN, KEY_LENGTH)
        return try {
            val skf = SecretKeyFactory.getInstance(ALGORITHM)
            val hash = skf.generateSecret(spec).encoded
            val hashBase64 = base64Encode(hash)
            "$saltBase64:$hashBase64"
        } finally {
            spec.clearPassword()
        }
    }

    fun verifyPin(pin: String, storedHash: String): Boolean {
        if (pin.isBlank() || storedHash.isBlank()) return false
        val trimmedInput = pin.trim()
        val trimmedStored = storedHash.trim()

        // 🎯 أمان: لا نقبل النص الصريح أبداً للـ PIN.
        // إذا كانت القيمة المخزنة لا تحتوي على الفاصل ":" → رفض.
        if (!trimmedStored.contains(":")) {
            return false
        }

        return try {
            val parts = trimmedStored.split(":")
            if (parts.size != 2) return false
            val salt = base64Decode(parts[0])
            val expectedHashBytes = base64Decode(parts[1])
            val spec = PBEKeySpec(trimmedInput.toCharArray(), salt, ITERATIONS_PIN, KEY_LENGTH)
            try {
                val skf = SecretKeyFactory.getInstance(ALGORITHM)
                val actualHashBytes = skf.generateSecret(spec).encoded
                MessageDigest.isEqual(actualHashBytes, expectedHashBytes)
            } finally {
                spec.clearPassword()
            }
        } catch (e: Exception) {
            false
        }
    }
}
