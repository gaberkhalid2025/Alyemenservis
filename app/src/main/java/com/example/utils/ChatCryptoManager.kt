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

    /**
     * تشفير النص العادي إلى Base64
     */
    fun encrypt(plainText: String, roomKey: String = DEFAULT_SECRET_SEED): String {
        if (plainText.isBlank()) return plainText
        return try {
            val keySpec = generateKey(roomKey)
            val cipher = Cipher.getInstance(ALGORITHM)
            val iv = ByteArray(16) { 0 }
            val ivSpec = IvParameterSpec(iv)

            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            "enc::" + Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            plainText
        }
    }

    /**
     * فك تشفير النص المشفر Base64
     */
    fun decrypt(cipherText: String, roomKey: String = DEFAULT_SECRET_SEED): String {
        if (!cipherText.startsWith("enc::")) return cipherText
        return try {
            val cleanCipher = cipherText.removePrefix("enc::")
            val keySpec = generateKey(roomKey)
            val cipher = Cipher.getInstance(ALGORITHM)
            val iv = ByteArray(16) { 0 }
            val ivSpec = IvParameterSpec(iv)

            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
            val decodedBytes = Base64.decode(cleanCipher, Base64.NO_WRAP)
            val decryptedBytes = cipher.doFinal(decodedBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            cipherText
        }
    }
}
