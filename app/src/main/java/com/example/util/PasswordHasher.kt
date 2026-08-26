package com.example.util

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * 🔑 PasswordHasher - نظام التجزئة الآمن لكلمات المرور (PBKDF2-HMAC-SHA256)
 * 
 * الميزات:
 * 1. استخدام خوارزمية PBKDF2 المعتمدة مع HMAC-SHA256 و 12,000 دورة تكرار.
 * 2. توليد تمليح عشوائي مشفر (Cryptographically Secure Salt) بطول 16 بايت لكل كلمة مرور.
 * 3. حماية ضد هجمات التوقيت (Timing Attacks) باستخدام المقارنة ذات الوقت الثابت (Constant-time).
 * 4. التوافق العكسي مع كلمات المرور المحفوظة بصيغة SHA-256 أو التنسيق المشفر.
 */
object PasswordHasher {

    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ITERATIONS = 12000
    private const val KEY_LENGTH = 256
    private const val SALT_SIZE = 16

    private val secureRandom by lazy { SecureRandom() }

    /**
     * توليد تمليح عشوائي فريد بطول 16 بايت
     * @return مصفوفة بايتات التمليح
     */
    fun generateSalt(): ByteArray {
        val salt = ByteArray(SALT_SIZE)
        secureRandom.nextBytes(salt)
        return salt
    }

    /**
     * تجزئة كلمة المرور باستخدام PBKDF2 والتمليح المعطى
     * @param password كلمة المرور النصية
     * @param salt مصفوفة التمليح
     * @return الهاش المشفر بتنسيق Base64
     */
    fun hashPassword(password: String, salt: ByteArray): String {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        val skf = SecretKeyFactory.getInstance(ALGORITHM)
        val hash = skf.generateSecret(spec).encoded
        return Base64.encodeToString(hash, Base64.NO_WRAP)
    }

    /**
     * إنشاء هاش مدمج بالتمليح بصيغة "saltBase64:hashBase64" للتخزين الآمن
     * @param password كلمة المرور
     * @return السلسلة المركبة الجاهزة للحفظ
     */
    fun createSaltedHash(password: String): String {
        val salt = generateSalt()
        val saltBase64 = Base64.encodeToString(salt, Base64.NO_WRAP)
        val hashBase64 = hashPassword(password, salt)
        return "$saltBase64:$hashBase64"
    }

    /**
     * تجزئة سريعة باستخدام SHA-256 للتوافق
     * @param input النص المراد تجزئته
     * @return تمثيل Hex للهاش
     */
    fun sha256(input: String): String {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            val bytes = md.digest(input.toByteArray(Charsets.UTF_8))
            bytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * التحقق من صحة كلمة المرور مقارنة بالهاش المخزن
     * يستخدم المقارنة بالوقت الثابت لمنع هجمات التوقيت (Timing Attacks)
     * 
     * @param inputPassword كلمة المرور المدخلة
     * @param storedSaltedHash الهاش المخزن (إما salt:hash أو SHA-256 أو نص قديم)
     * @return true إذا كانت كلمة المرور مطابقة
     */
    fun verifyPassword(inputPassword: String, storedSaltedHash: String): Boolean {
        if (inputPassword.isBlank() || storedSaltedHash.isBlank()) return false
        val trimmedInput = inputPassword.trim()
        val trimmedStored = storedSaltedHash.trim()

        // 1. فحص المطابقة المباشرة إذا كانت القيمة مخزنة كنص صريح (Legacy)
        if (constantTimeEquals(trimmedInput, trimmedStored)) return true

        // 2. فحص SHA-256
        val computedSha256 = sha256(trimmedInput)
        if (constantTimeEquals(computedSha256.lowercase(), trimmedStored.lowercase())) return true

        // 3. فحص صيغة PBKDF2 (salt:hash)
        return try {
            if (trimmedStored.contains(":")) {
                val parts = trimmedStored.split(":")
                if (parts.size == 2) {
                    val salt = Base64.decode(parts[0], Base64.NO_WRAP)
                    val expectedHash = parts[1]
                    val computedHash = hashPassword(trimmedInput, salt)
                    return constantTimeEquals(computedHash, expectedHash)
                }
            }
            false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * مقارنة نصين في وقت ثابت لمنع هجمات التوقيت
     */
    private fun constantTimeEquals(a: String, b: String): Boolean {
        val aBytes = a.toByteArray(Charsets.UTF_8)
        val bBytes = b.toByteArray(Charsets.UTF_8)
        return MessageDigest.isEqual(aBytes, bBytes)
    }
}
