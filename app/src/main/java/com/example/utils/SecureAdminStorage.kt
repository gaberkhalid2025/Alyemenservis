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
            
            EncryptedSharedPreferences.create(
                context.applicationContext,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            ) as EncryptedSharedPreferences
        } catch (e: Exception) {
            android.util.Log.e("SecureAdminStorage", "Failed to init secure storage", e)
            null
        }
    }
    
    /**
     * حفظ credentials بشكل آمن (تشفير Hash)
     * لا نحفظ password نفسه - فقط Hash
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
                editor.putString(KEY_OWNER_HASH, hashValue(it, SALT_PREFIX + "pass"))
            }
            adminEmail?.let {
                editor.putString(KEY_ADMIN_EMAIL, hashValue(it, SALT_PREFIX + "email"))
            }
            adminPassword?.let {
                editor.putString(KEY_ADMIN_HASH, hashValue(it, SALT_PREFIX + "pass"))
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
     * التحقق من Credentials المخزنة محلياً
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
            
            when (role.uppercase()) {
                "OWNER" -> {
                    val storedEmail = prefs.getString(KEY_OWNER_EMAIL, null) ?: return false
                    val storedPass = prefs.getString(KEY_OWNER_HASH, null) ?: return false
                    constantTimeEquals(storedEmail, emailHash) && 
                        constantTimeEquals(storedPass, passHash)
                }
                "ADMIN" -> {
                    val storedEmail = prefs.getString(KEY_ADMIN_EMAIL, null) ?: return false
                    val storedPass = prefs.getString(KEY_ADMIN_HASH, null) ?: return false
                    constantTimeEquals(storedEmail, emailHash) && 
                        constantTimeEquals(storedPass, passHash)
                }
                else -> false
            }
        } catch (e: Exception) {
            android.util.Log.e("SecureAdminStorage", "Failed to verify credentials", e)
            false
        }
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
