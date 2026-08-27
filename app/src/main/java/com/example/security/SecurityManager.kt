package com.example.security

import android.content.Context
import android.content.SharedPreferences
import com.example.util.SecurityCryptoUtils

/**
 * 🛡️ SecurityManager - إدارة الأمان والحماية والحد من محاولات الدخول الخاطئة والتشفير الآمن للمصادقة
 * Encrypts and decrypts all sensitive local credentials (PIN code, secure auth tokens) using SecurityCryptoUtils.
 */
class SecurityManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_security_prefs", Context.MODE_PRIVATE)

    fun registerFailedAttempt(): Boolean {
        val attempts = prefs.getInt("failed_attempts", 0) + 1
        prefs.edit().putInt("failed_attempts", attempts).apply()

        if (attempts >= 5) {
            val lockTime = System.currentTimeMillis() + (30 * 60 * 1000) // قفل لمدة 30 دقيقة
            prefs.edit().putLong("lockout_timestamp", lockTime).apply()
            return true // تم القفل
        }
        return false
    }

    fun isLockedOut(): Boolean {
        val lockTime = prefs.getLong("lockout_timestamp", 0L)
        if (System.currentTimeMillis() < lockTime) {
            return true
        } else if (lockTime != 0L) {
            // انقضى وقت القفل
            resetAttempts()
        }
        return false
    }

    fun resetAttempts() {
        prefs.edit()
            .putInt("failed_attempts", 0)
            .putLong("lockout_timestamp", 0L)
            .apply()
    }

    /**
     * 🔒 Saves PIN code with high-grade AES-256 GCM encryption
     */
    fun savePinCode(pin: String) {
        val encryptedPin = SecurityCryptoUtils.encrypt(pin)
        prefs.edit().putString("local_pin", encryptedPin).apply()
    }

    /**
     * 🔓 Decrypts and verifies entered PIN code securely
     */
    fun verifyPinCode(inputPin: String): Boolean {
        val savedPinEncrypted = prefs.getString("local_pin", null) ?: return false
        val decryptedPin = SecurityCryptoUtils.decrypt(savedPinEncrypted)
        return decryptedPin == inputPin
    }

    fun hasPinCode(): Boolean {
        return !prefs.getString("local_pin", null).isNullOrEmpty()
    }

    /**
     * 🔑 Saves sensitive session token securely
     */
    fun saveSecureToken(token: String) {
        val encryptedToken = SecurityCryptoUtils.encrypt(token)
        prefs.edit().putString("secure_token", encryptedToken).apply()
    }

    /**
     * 🔓 Retrieves and decrypts the session token
     */
    fun getSecureToken(): String {
        val encryptedToken = prefs.getString("secure_token", null) ?: return ""
        return SecurityCryptoUtils.decrypt(encryptedToken)
    }
}
