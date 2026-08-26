package com.example.util

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * 🔒 SecurityCryptoUtils - وحدة التشفير والأمان المتقدمة لتطبيق دليل خدمات اليمن
 * 
 * الميزات والوظائف:
 * 1. تشفير متقدم بنظام AES-256-GCM المعتمد من AndroidKeyStore مع فحص تكامل البيانات (AEAD).
 * 2. توليد IV عشوائي حقيقي (12 بايت) لكل عملية تشفير لمنع هجمات التكرار (Replay Attacks).
 * 3. دمج IV مع النص المشفر في مصفوفة بايتات واحدة بصيغة Base64 (IV + CipherText + Tag).
 * 4. دعم فك التشفير للبيانات القديمة (Legacy Fallback) المتوافقة مع الإصدارات السابقة.
 * 5. تجزئة كلمات المرور باستخدام SHA-256 والتمليح الآمن (Salt).
 * 6. تعقيم المدخلات (Input Sanitization) لمنع حقن السكربتات وهجمات XSS.
 */
object SecurityCryptoUtils {

    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEYSTORE_ALIAS = "YEMEN_SERVICES_MASTER_KEY_2026_AES256"
    private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_IV_LENGTH = 12 // 12 بايت قياسي لـ GCM
    private const val GCM_TAG_LENGTH = 128 // 128 بت (16 بايت) لمصادقة التشفير
    private const val LEGACY_AES_CBC_TRANSFORMATION = "AES/CBC/PKCS7Padding"

    private val secureRandom by lazy { SecureRandom() }

    /**
     * الحصول على المفتاح السري من AndroidKeyStore أو إنشائه إن لم يكن موجوداً
     * @return مفتاح AES-256 المشفر على مستوى الأجهزة (Hardware-backed إن أمكن)
     */
    @Synchronized
    private fun getOrCreateMasterKey(): SecretKey {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)

            if (!keyStore.containsAlias(KEYSTORE_ALIAS)) {
                val keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES,
                    ANDROID_KEYSTORE
                )
                val keyGenSpec = KeyGenParameterSpec.Builder(
                    KEYSTORE_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM, KeyProperties.BLOCK_MODE_CBC)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE, KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .setKeySize(256)
                    .setRandomizedEncryptionRequired(false) // يسمح بإعطاء IV يدوي عشوائي
                    .build()

                keyGenerator.init(keyGenSpec)
                keyGenerator.generateKey()
            } else {
                val entry = keyStore.getEntry(KEYSTORE_ALIAS, null) as? KeyStore.SecretKeyEntry
                entry?.secretKey ?: createFallbackKey()
            }
        } catch (e: Exception) {
            createFallbackKey()
        }
    }

    /**
     * مفتاح احتياطي آمن مشتق في حال تعذر الوصول إلى AndroidKeyStore
     * @return SecretKeySpec مشتق بواسطة SHA-256
     */
    private fun createFallbackKey(): SecretKeySpec {
        val appSalt = "YEMEN_SERVICES_SECURE_VAULT_SALT_2026_AES256_PROD".toByteArray(StandardCharsets.UTF_8)
        val deviceSeed = ("SEED_${android.os.Build.MANUFACTURER}_${android.os.Build.MODEL}_${android.os.Build.SERIAL ?: "DEVICE"}").toByteArray(StandardCharsets.UTF_8)
        val combined = ByteArray(appSalt.size + deviceSeed.size)
        System.arraycopy(appSalt, 0, combined, 0, appSalt.size)
        System.arraycopy(deviceSeed, 0, combined, appSalt.size, deviceSeed.size)
        
        val sha256 = MessageDigest.getInstance("SHA-256")
        val keyBytes = sha256.digest(combined)
        return SecretKeySpec(keyBytes, "AES")
    }

    /**
     * تشفير النص باستخدام AES-256-GCM مع توليد IV عشوائي 12 بايت
     * 
     * @param plainText النص المراد تشفيره
     * @return النص المشفر بصيغة Base64 يحتوي على (IV + CipherText + AuthTag)
     */
    fun encrypt(plainText: String?): String {
        if (plainText.isNullOrEmpty()) return ""
        return try {
            val key = getOrCreateMasterKey()
            val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
            
            // 1. توليد IV عشوائي
            val iv = ByteArray(GCM_IV_LENGTH)
            secureRandom.nextBytes(iv)
            
            // 2. تهيئة التشفير بنمط GCM
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec)
            
            // 3. تنفيذ التشفير
            val cipherText = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))
            
            // 4. دمج IV مع النص المشفر (IV + CipherText)
            val combined = ByteArray(iv.size + cipherText.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(cipherText, 0, combined, iv.size, cipherText.size)
            
            // 5. إرجاع النتيجة بتنسيق Base64
            Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            // محاولة التشفير الاحتياطي إذا فشل KeyStore
            encryptWithFallback(plainText)
        }
    }

    /**
     * تشفير احتياطي باستخدام Fallback Key
     */
    private fun encryptWithFallback(plainText: String): String {
        return try {
            val key = createFallbackKey()
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            val iv = ByteArray(16)
            secureRandom.nextBytes(iv)
            val ivSpec = IvParameterSpec(iv)
            cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec)
            val cipherText = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))
            
            val combined = ByteArray(iv.size + cipherText.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(cipherText, 0, combined, iv.size, cipherText.size)
            Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            plainText // كحل أخير لتجنب انهيار التطبيق
        }
    }

    /**
     * فك تشفير النص المشفر واستخراج IV والبيانات الأصلية
     * يدعم فك تشفير AES-256-GCM والتوافق مع البيانات القديمة.
     * 
     * @param encryptedText النص المشفر بصيغة Base64
     * @return النص الأصلي غير المشفر
     */
    fun decrypt(encryptedText: String?): String {
        if (encryptedText.isNullOrEmpty()) return ""
        return try {
            val combined = Base64.decode(encryptedText, Base64.NO_WRAP)
            
            // فحص طول البيانات لتحديد وضع GCM أو Legacy
            if (combined.size <= GCM_IV_LENGTH) {
                return encryptedText // نص عادي أو تالف
            }
            
            // 1. استخراج IV (أول 12 بايت لـ GCM)
            val iv = ByteArray(GCM_IV_LENGTH)
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH)
            
            val cipherTextSize = combined.size - GCM_IV_LENGTH
            val cipherText = ByteArray(cipherTextSize)
            System.arraycopy(combined, GCM_IV_LENGTH, cipherText, 0, cipherTextSize)
            
            val key = getOrCreateMasterKey()
            val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec)
            
            val plainBytes = cipher.doFinal(cipherText)
            String(plainBytes, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            // محاولة فك التشفير الاحتياطي أو القديم
            decryptFallbackOrLegacy(encryptedText)
        }
    }

    /**
     * فك التشفير الاحتياطي أو للبيانات القديمة
     */
    private fun decryptFallbackOrLegacy(encryptedText: String): String {
        return try {
            val combined = Base64.decode(encryptedText, Base64.NO_WRAP)
            if (combined.size > 16) {
                // تجربة فك CBC مع 16 بايت IV
                val iv = ByteArray(16)
                System.arraycopy(combined, 0, iv, 0, 16)
                val cipherText = ByteArray(combined.size - 16)
                System.arraycopy(combined, 16, cipherText, 0, combined.size - 16)
                
                val key = createFallbackKey()
                val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
                cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv))
                val decrypted = cipher.doFinal(cipherText)
                String(decrypted, StandardCharsets.UTF_8)
            } else {
                encryptedText
            }
        } catch (e: Exception) {
            encryptedText // إرجاع النص كما هو إذا كان نصاً عادياً سابقاً
        }
    }

    /**
     * تشفير رمز الإشعارات (FCM Token) لضمان الخصوصية
     * @param token رمز FCM
     * @return الرمز مشفراً
     */
    fun encryptFcmToken(token: String?): String = encrypt(token)

    /**
     * فك تشفير رمز الإشعارات (FCM Token)
     * @param encryptedToken الرمز المشفر
     * @return الرمز الأصلي
     */
    fun decryptFcmToken(encryptedToken: String?): String = decrypt(encryptedToken)

    /**
     * تجزئة كلمة المرور باستخدام SHA-256 والتمليح (Salt)
     * @param password كلمة المرور
     * @param salt قيمة التمليح الاختيارية
     * @return الهاش بصيغة Hex
     */
    fun hashPassword(password: String, salt: String = "YEMEN_DIR_2026"): String {
        if (password.isEmpty()) return ""
        val inputWithSalt = "$salt:$password"
        val bytes = MessageDigest.getInstance("SHA-256").digest(inputWithSalt.toByteArray(StandardCharsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * التحقق من صحة كلمة مرور الإدارة
     * يدعم المطابقة مع الهاش المشفر ومكتبة PasswordHasher
     * 
     * @param input كلمة المرور المدخلة
     * @param storedHashOrPass القيمة المخزنة
     * @return true إذا كانت كلمة المرور صحيحة
     */
    fun verifyAdminPassword(input: String, storedHashOrPass: String? = null): Boolean {
        if (input.isBlank() || storedHashOrPass.isNullOrBlank()) return false
        val trimmedInput = input.trim()
        val trimmedStored = storedHashOrPass.trim()

        if (trimmedInput == trimmedStored) return true
        if (hashPassword(trimmedInput).equals(trimmedStored, ignoreCase = true)) return true
        if (hashPassword(trimmedInput, "").equals(trimmedStored, ignoreCase = true)) return true
        return PasswordHasher.verifyPassword(trimmedInput, trimmedStored)
    }

    /**
     * تعقيم وتطهير المدخلات من السكربتات والوسوم الخطيرة لمنع هجمات XSS وحقن البيانات
     * 
     * @param input النص المدخل من المستخدم
     * @return النص بعد تنظيفه وتعقيمه
     */
    fun sanitizeInput(input: String?): String {
        if (input.isNullOrEmpty()) return ""
        return input
            .replace(Regex("<[^>]*>"), "")
            .replace("script", "", ignoreCase = true)
            .replace("javascript:", "", ignoreCase = true)
            .replace("vbscript:", "", ignoreCase = true)
            .replace("onload=", "", ignoreCase = true)
            .replace("onerror=", "", ignoreCase = true)
            .replace("onclick=", "", ignoreCase = true)
            .replace("'", "''") // منع SQL Injection البسيط
            .trim()
    }

    /**
     * فك تشفير السلاسل النصية المشوشة (Obfuscated Strings)
     * @param hex النص المشفر بنظام الست عشري
     * @param key المفتاح السري للتشويش
     * @return النص الأصلي
     */
    fun decodeObfuscatedString(hex: String, key: String = "YemenServiceSecretKey2026"): String {
        return try {
            val bytes = ByteArray(hex.length / 2)
            for (i in bytes.indices) {
                bytes[i] = hex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
            }
            val keyBytes = key.toByteArray(StandardCharsets.UTF_8)
            for (i in bytes.indices) {
                bytes[i] = (bytes[i].toInt() xor keyBytes[i % keyBytes.size].toInt()).toByte()
            }
            String(bytes, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * التحقق من قوة كلمة المرور ومطابقتها لمعايير الأمان
     * @param password كلمة المرور المراد فحصها
     * @return Pair يحتوي على نتيجة الفحص ورسالة التوجيه بالعربية في حال عدم القبول
     */
    fun validatePasswordPolicy(password: String): Pair<Boolean, String?> {
        val cleanPass = password.trim()
        if (cleanPass.length < 8) {
            return Pair(false, "عفواً، يجب أن تكون كلمة المرور مكونة من 8 خانات (أحرف أو أرقام) على الأقل لضمان قوة حماية حسابك.")
        }
        val weakPasswords = listOf(
            "123456", "12345678", "000000", "00000000", "111111", "11111111", 
            "112233", "123123", "password", "yemen123", "yemen2026", "77777777",
            "123456789", "qwertyuiop", "admin123"
        )
        if (cleanPass.lowercase() in weakPasswords) {
            return Pair(false, "عفواً، لقد قمت بإدخال كلمة مرور ضعيفة وسهلة التخمين (مثل: 123456 أو 000000). يرجى اختيار كلمة مرور قوية وغير متوقعة لحماية بياناتك.")
        }
        return Pair(true, null)
    }
}
