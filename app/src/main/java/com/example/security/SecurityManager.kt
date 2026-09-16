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

    companion object {
        private const val TAG = "SecurityManager"

        /**
         * Checks if the device is rooted or running in a compromised environment.
         */
        fun isDeviceRooted(): Boolean {
            return try {
                val rootFiles = listOf(
                    "/system/app/Superuser.apk",
                    "/sbin/su",
                    "/system/bin/su",
                    "/system/xbin/su",
                    "/data/local/xbin/su",
                    "/data/local/bin/su",
                    "/system/sd/xbin/su",
                    "/system/bin/failsafe/su",
                    "/data/local/su"
                )
                for (file in rootFiles) {
                    if (java.io.File(file).exists()) return true
                }

                if (android.os.Build.TAGS?.contains("test-keys") == true) return true

                false
            } catch (e: Exception) {
                false
            }
        }

        /**
         * Checks for known hooking tools such as Frida or Xposed.
         */
        fun isHookingFrameworkDetected(): Boolean {
            return try {
                val checkPaths = listOf(
                    "/data/local/tmp/frida-server",
                    "/system/framework/XposedBridge.jar"
                )
                for (p in checkPaths) {
                    if (java.io.File(p).exists()) return true
                }
                false
            } catch (e: Exception) {
                false
            }
        }

        /**
         * Verifies application signature SHA-256 digest against expected hashes to detect illegal repackaging.
         */
        fun verifyAppSignature(context: Context): Boolean {
            return try {
                val expectedHash = com.example.BuildConfig.SIGNATURE_HASH
                if (expectedHash.isEmpty()) {
                    return com.example.BuildConfig.DEBUG
                }
                
                val pm = context.packageManager
                val pkg = context.packageName
                val signatures = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    val signingInfo = pm.getPackageInfo(pkg, android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES).signingInfo
                    if (signingInfo != null) {
                        if (signingInfo.hasMultipleSigners()) {
                            signingInfo.apkContentsSigners
                        } else {
                            signingInfo.signingCertificateHistory
                        }
                    } else null
                } else {
                    @Suppress("DEPRECATION")
                    pm.getPackageInfo(pkg, android.content.pm.PackageManager.GET_SIGNATURES).signatures
                }

                if (signatures.isNullOrEmpty()) {
                    return false
                }

                val md = java.security.MessageDigest.getInstance("SHA-256")
                for (sig in signatures) {
                    val digest = md.digest(sig.toByteArray())
                    val hash = digest.joinToString("") { "%02x".format(it) }
                    if (hash.equals(expectedHash, ignoreCase = true)) {
                        return true
                    }
                }
                android.util.Log.e(TAG, "Signature mismatch!")
                false
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Exception during signature verification: ${e.message}")
                false
            }
        }
    }
}
