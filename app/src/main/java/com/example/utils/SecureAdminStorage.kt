package com.example.utils

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * 🔐 SecureAdminStorage - تخزين آمن لبيانات المالك والأدمن
 * 
 * يستخدم:
 * - Android Keystore (Hardware-backed)
 * - AES256-GCM Encryption
 * - EncryptedSharedPreferences
 * 
 * ⚠️ البيانات لا تُخزَّن في الكود المصدري
 * ⚠️ يتم تحميلها من Cloud Functions عند أول تشغيل
 */
object SecureAdminStorage {
    
    private const val PREFS_NAME = "secure_admin_vault_v2"
    private const val KEY_OWNER_EMAIL = "owner_email_hash"
    private const val KEY_OWNER_HASH = "owner_pass_hash"
    private const val KEY_ADMIN_EMAIL = "admin_email_hash"
    private const val KEY_ADMIN_HASH = "admin_pass_hash"
    private const val KEY_VAULT_INITIALIZED = "vault_initialized"
    private const val KEY_VAULT_LAST_SYNC = "vault_last_sync"
    
    private const val SALT_PREFIX = "yemen_admin_v2_"
    
    private fun getSecurePrefs(context: Context): EncryptedSharedPreferences? {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            
            val encryptedPrefs = EncryptedSharedPreferences.create(
                context.applicationContext,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            ) as EncryptedSharedPreferences
            migrateLegacyPrefs(context, encryptedPrefs)
            encryptedPrefs
        } catch (e: Exception) {
            android.util.Log.e("SecureAdminStorage", "Failed to init secure storage", e)
            null
        }
    }

    /**
     * 🔄 ترحيل بيانات الاعتماد من SharedPreferences العادي إلى المشفر ثم حذف النسخة القديمة
     */
    private fun migrateLegacyPrefs(context: Context, encryptedPrefs: EncryptedSharedPreferences) {
        val legacySources = listOf("admin_security_prefs", "secure_admin_vault", "admin_vault_legacy")
        for (src in legacySources) {
            try {
                val oldSp = context.getSharedPreferences(src, Context.MODE_PRIVATE)
                val entries = oldSp.all
                if (entries.isNotEmpty()) {
                    val editor = encryptedPrefs.edit()
                    for ((k, v) in entries) {
                        if (!encryptedPrefs.contains(k)) {
                            when (v) {
                                is String -> editor.putString(k, v)
                                is Boolean -> editor.putBoolean(k, v)
                                is Long -> editor.putLong(k, v)
                                is Int -> editor.putInt(k, v)
                                is Float -> editor.putFloat(k, v)
                            }
                        }
                    }
                    editor.apply()
                    oldSp.edit().clear().apply()
                }
            } catch (_: Exception) {}
        }
    }
    
    /**
     * حفظ credentials بشكل آمن (تشفير Hash بـ PBKDF2)
     * لا نحفظ password نفسه في أي مكان إطلاقاً - فقط Hash
     */
    fun storeCredentials(
        context: Context,
        ownerEmail: String? = null,
        ownerPassword: String? = null,
        adminEmail: String? = null,
        adminPassword: String? = null
    ): Boolean {
        val prefs = getSecurePrefs(context) ?: return false
        
        return try {
            val editor = prefs.edit()
            
            ownerEmail?.let {
                editor.putString(KEY_OWNER_EMAIL, hashValue(it, SALT_PREFIX + "email"))
            }
            ownerPassword?.let {
                editor.putString(KEY_OWNER_HASH, SecureHasher.hashPassword(it.trim()))
            }
            adminEmail?.let {
                editor.putString(KEY_ADMIN_EMAIL, hashValue(it, SALT_PREFIX + "email"))
            }
            adminPassword?.let {
                editor.putString(KEY_ADMIN_HASH, SecureHasher.hashPassword(it.trim()))
            }
            
            editor.putBoolean(KEY_VAULT_INITIALIZED, true)
            editor.putLong(KEY_VAULT_LAST_SYNC, System.currentTimeMillis())
            editor.apply()
            true
        } catch (e: Exception) {
            android.util.Log.e("SecureAdminStorage", "Failed to store credentials", e)
            false
        }
    }
    
    /**
     * التحقق من Credentials المخزنة محلياً باستخدام PBKDF2
     */
    fun verifyFallbackCredentials(
        context: Context,
        email: String,
        password: String,
        role: String
    ): Boolean {
        val prefs = getSecurePrefs(context) ?: return false
        
        if (!prefs.getBoolean(KEY_VAULT_INITIALIZED, false)) {
            return false
        }
        
        return try {
            val emailHash = hashValue(email, SALT_PREFIX + "email")
            val passHash = hashValue(password, SALT_PREFIX + "pass")
            val cleanEmail = email.trim()
            val cleanPass = password.trim()
            
            when (role.uppercase()) {
                "OWNER" -> {
                    val storedEmail = prefs.getString(KEY_OWNER_EMAIL, null) ?: return false
                    val storedPass = prefs.getString(KEY_OWNER_HASH, null) ?: return false
                    val emailMatches = constantTimeEquals(storedEmail, emailHash) || 
                            SecureHasher.verifyPassword(cleanEmail, storedEmail) ||
                            storedEmail.equals(cleanEmail, ignoreCase = true)
                    val passMatches = constantTimeEquals(storedPass, passHash) || 
                            SecureHasher.verifyPassword(cleanPass, storedPass)
                    emailMatches && passMatches
                }
                "ADMIN" -> {
                    val storedEmail = prefs.getString(KEY_ADMIN_EMAIL, null) ?: return false
                    val storedPass = prefs.getString(KEY_ADMIN_HASH, null) ?: return false
                    val emailMatches = constantTimeEquals(storedEmail, emailHash) || 
                            SecureHasher.verifyPassword(cleanEmail, storedEmail) ||
                            storedEmail.equals(cleanEmail, ignoreCase = true)
                    val passMatches = constantTimeEquals(storedPass, passHash) || 
                            SecureHasher.verifyPassword(cleanPass, storedPass)
                    emailMatches && passMatches
                }
                else -> false
            }
        } catch (e: Exception) {
            android.util.Log.e("SecureAdminStorage", "Failed to verify credentials", e)
            false
        }
    }

    /**
     * 🛡️ التحقق من كلمة المرور عبر البصمة (PBKDF2) وترقية الحسابات القديمة تلقائياً في Firestore
     * عند أول تسجيل دخول ناجح لأي حساب قديم مخزن كنص، يتم تحويله إلى بصمة مشفرة وتحديث Firestore فوراً
     */
    fun verifyAndMigrate(
        docRef: com.google.firebase.firestore.DocumentReference?,
        inputPassword: String,
        storedPassOrHash: String,
        fieldName: String = "passcode"
    ): Boolean {
        if (inputPassword.isBlank() || storedPassOrHash.isBlank()) return false
        val cleanInput = inputPassword.trim()
        val cleanStored = storedPassOrHash.trim()
        
        val isValid = SecureHasher.verifyPassword(cleanInput, cleanStored) ||
                SecurityCryptoUtils.verifyAdminPassword(cleanInput, cleanStored)
                
        if (isValid && docRef != null) {
            // إذا كانت كلمة المرور القديمة نصاً عادياً لا يحتوي على ملوحة (salt separator ":")
            if (!cleanStored.contains(":")) {
                try {
                    val newHash = SecureHasher.hashPassword(cleanInput)
                    docRef.update(fieldName, newHash)
                } catch (e: Exception) {
                    android.util.Log.e("SecureAdminStorage", "Automatic hash migration in Firestore failed", e)
                }
            }
        }
        return isValid
    }
    
    /**
     * Hash آمن باستخدام SHA-256 + Salt
     */
    private fun hashValue(value: String, salt: String): String {
        return try {
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            val bytes = digest.digest((salt + value).toByteArray(Charsets.UTF_8))
            bytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            ""
        }
    }
    
    /**
     * Constant-Time Comparison لتجنب Timing Attacks
     */
    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var result = 0
        for (i in a.indices) {
            result = result or (a[i].code xor b[i].code)
        }
        return result == 0
    }
    
    /**
     * حذف البيانات المحفوظة (للاستخدام عند التحديثات)
     */
    fun clearVault(context: Context): Boolean {
        val prefs = getSecurePrefs(context) ?: return false
        return try {
            prefs.edit().clear().apply()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * التحقق من تهيئة الـ Vault
     */
    fun isVaultInitialized(context: Context): Boolean {
        val prefs = getSecurePrefs(context) ?: return false
        return prefs.getBoolean(KEY_VAULT_INITIALIZED, false)
    }
}

/**
 * 🔐 واجهة ومستودع أوراق اعتماد الأدمن والمشرفين (AdminCredentialsVault)
 */
typealias AdminCredentialsVault = SecureAdminStorage
