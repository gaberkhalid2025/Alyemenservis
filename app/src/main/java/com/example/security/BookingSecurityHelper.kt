package com.example.security

import android.content.Context
import android.content.SharedPreferences
import java.security.MessageDigest

/**
 * 🔒 BookingSecurityHelper
 * Manages PIN encryption (SHA-256), 3-attempt failure tracking,
 * and 5-minute security lockouts for booking cancellations and modifications.
 */
object BookingSecurityHelper {

    private const val PREFS_NAME = "booking_security_vault"
    private const val KEY_ATTEMPTS_PREFIX = "attempts_"
    private const val KEY_LOCKOUT_PREFIX = "lockout_"
    private const val MAX_ATTEMPTS = 3
    private const val LOCKOUT_DURATION_MS = 5 * 60 * 1000L // 5 minutes lockout

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Hashes a PIN or password using SHA-256 for secure comparison.
     */
    fun hashPin(pin: String): String {
        if (pin.isBlank()) return ""
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(pin.trim().toByteArray(Charsets.UTF_8))
            hashBytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            pin.trim()
        }
    }

    /**
     * Checks if a booking is currently locked out from cancellation/modification attempts.
     */
    fun isBookingLocked(context: Context, bookingId: String): Boolean {
        val prefs = getPrefs(context)
        val lockTime = prefs.getLong(KEY_LOCKOUT_PREFIX + bookingId, 0L)
        val now = System.currentTimeMillis()
        if (now < lockTime) {
            return true
        } else if (lockTime != 0L) {
            // Lockout expired, reset
            resetAttempts(context, bookingId)
        }
        return false
    }

    /**
     * Returns remaining lockout time in seconds.
     */
    fun getRemainingLockoutSeconds(context: Context, bookingId: String): Long {
        val prefs = getPrefs(context)
        val lockTime = prefs.getLong(KEY_LOCKOUT_PREFIX + bookingId, 0L)
        val diff = lockTime - System.currentTimeMillis()
        return if (diff > 0) diff / 1000 else 0L
    }

    /**
     * Records a failed PIN attempt. If attempts reach 3, locks out for 5 minutes.
     * Returns the number of remaining attempts before lockout.
     */
    fun recordFailedAttempt(context: Context, bookingId: String): Int {
        val prefs = getPrefs(context)
        val currentAttempts = prefs.getInt(KEY_ATTEMPTS_PREFIX + bookingId, 0) + 1
        val editor = prefs.edit()
        editor.putInt(KEY_ATTEMPTS_PREFIX + bookingId, currentAttempts)

        if (currentAttempts >= MAX_ATTEMPTS) {
            val lockoutTime = System.currentTimeMillis() + LOCKOUT_DURATION_MS
            editor.putLong(KEY_LOCKOUT_PREFIX + bookingId, lockoutTime)
            editor.apply()
            return 0
        }
        editor.apply()
        return (MAX_ATTEMPTS - currentAttempts).coerceAtLeast(0)
    }

    /**
     * Resets failed attempts and lockout upon successful verification.
     */
    fun resetAttempts(context: Context, bookingId: String) {
        val prefs = getPrefs(context)
        prefs.edit()
            .remove(KEY_ATTEMPTS_PREFIX + bookingId)
            .remove(KEY_LOCKOUT_PREFIX + bookingId)
            .apply()
    }

    /**
     * Verifies raw input password against target (handles both plain and hashed comparison).
     */
    fun verifyPassword(rawInput: String, targetPasswordOrHash: String): Boolean {
        val cleanInput = rawInput.trim()
        val cleanTarget = targetPasswordOrHash.trim()
        if (cleanInput.isEmpty() || cleanTarget.isEmpty()) return false
        
        // Direct match
        if (cleanInput.equals(cleanTarget, ignoreCase = true)) return true
        
        // Hash match
        val inputHash = hashPin(cleanInput)
        return inputHash.equals(cleanTarget, ignoreCase = true)
    }

    /**
     * Masks sensitive phone number (e.g. 771234567 -> 77***4567 or ***4567) until accepted.
     */
    fun maskPhoneNumber(phone: String): String {
        val clean = phone.trim()
        if (clean.length < 6) return "***"
        val prefix = clean.take(2)
        val suffix = clean.takeLast(3)
        return "$prefix****$suffix"
    }
}
