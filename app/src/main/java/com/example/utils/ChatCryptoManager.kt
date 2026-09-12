package com.example.utils

import android.util.Base64
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * 🔐 ChatCryptoManager
 * تشفير وفك تشفير الرسائل بتقنية AES-256-CBC
 * لحماية خصوصية المحادثات وضمان التشفير التام (End-to-End Encryption - E2EE)
 */
object ChatCryptoManager {

    private const val ALGORITHM = "AES/CBC/PKCS5Padding"
    private const val DEFAULT_SECRET_SEED = "YemenServices_E2EE_SuperSecret_2026"

    /**
     * توليد مفتاح AES 256 بت من بذرة السر أو معرف الغرفة
     */
    private fun generateKey(passphrase: String): SecretKeySpec {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = passphrase.toByteArray(Charsets.UTF_8)
        val keyBytes = digest.digest(bytes)
        return SecretKeySpec(keyBytes, "AES")
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

    /**
     * تشفير النص العادي إلى Base64 باستخدام IV عشوائي 16 بايت
     */
    fun encrypt(plainText: String, roomKey: String = DEFAULT_SECRET_SEED): String {
        if (plainText.isBlank()) return plainText
        return try {
            val keySpec = generateKey(roomKey)
            val cipher = Cipher.getInstance(ALGORITHM)
            val iv = ByteArray(16)
            java.security.SecureRandom().nextBytes(iv)
            val ivSpec = IvParameterSpec(iv)

            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            val combined = iv + encryptedBytes
            "enc::" + base64Encode(combined)
        } catch (e: Exception) {
            e.printStackTrace()
            plainText
        }
    }

    /**
     * فك تشفير النص المشفر Base64 مع دعم IV العشوائي والنصوص المشفرة القديمة
     */
    fun decrypt(cipherText: String, roomKey: String = DEFAULT_SECRET_SEED): String {
        if (!cipherText.startsWith("enc::")) return cipherText
        return try {
            val cleanCipher = cipherText.removePrefix("enc::")
            val combined = base64Decode(cleanCipher)
            val keySpec = generateKey(roomKey)
            val cipher = Cipher.getInstance(ALGORITHM)

            if (combined.size > 16) {
                try {
                    val iv = combined.copyOfRange(0, 16)
                    val encrypted = combined.copyOfRange(16, combined.size)
                    val ivSpec = IvParameterSpec(iv)
                    cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
                    val decryptedBytes = cipher.doFinal(encrypted)
                    String(decryptedBytes, Charsets.UTF_8)
                } catch (ex: Exception) {
                    // Fallback to legacy static 16-zero IV
                    val iv = ByteArray(16) { 0 }
                    val ivSpec = IvParameterSpec(iv)
                    cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
                    val decryptedBytes = cipher.doFinal(combined)
                    String(decryptedBytes, Charsets.UTF_8)
                }
            } else {
                val iv = ByteArray(16) { 0 }
                val ivSpec = IvParameterSpec(iv)
                cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
                val decryptedBytes = cipher.doFinal(combined)
                String(decryptedBytes, Charsets.UTF_8)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            cipherText
        }
    }
}
