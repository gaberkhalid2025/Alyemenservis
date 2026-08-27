package com.example.security

import android.content.Context
import android.content.SharedPreferences

/**
 * 🛡️ SecurityManager - إدارة الأمان والحماية والحد من محاولات الدخول الخاطئة
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

    fun savePinCode(pin: String) {
        prefs.edit().putString("local_pin", pin).apply()
    }

    fun verifyPinCode(inputPin: String): Boolean {
        val savedPin = prefs.getString("local_pin", null)
        return savedPin == inputPin
    }

    fun hasPinCode(): Boolean {
        return !prefs.getString("local_pin", null).isNullOrEmpty()
    }
}
