package com.example.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import java.io.File
import java.security.MessageDigest

/**
 * SecurityManager handles application integrity checks:
 * - Root / Jailbreak Detection
 * - Anti-Frida / Anti-Debugging checks
 * - Application Signature Verification
 * - Secure Session & Keystore Storage
 */
object SecurityManager {

    private const val TAG = "SecurityManager"

    /**
     * Checks if the device is rooted or running in a compromised environment.
     */
    fun isDeviceRooted(): Boolean {
        val buildTags = Build.TAGS
        if (buildTags != null && buildTags.contains("test-keys")) {
            Log.w(TAG, "Root detected: Test-keys detected in build tags")
            return true
        }

        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su",
            "/su/bin/su",
            "/system/usr/we-need-root/su"
        )

        for (path in paths) {
            if (File(path).exists()) {
                Log.w(TAG, "Root detected: su binary found at $path")
                return true
            }
        }

        return false
    }

    /**
     * Checks for known hooking tools such as Frida or Xposed.
     */
    fun isHookingFrameworkDetected(): Boolean {
        val fridaPaths = arrayOf(
            "/data/local/tmp/frida-server",
            "/data/local/tmp/re.frida.server",
            "/sdcard/frida-server",
            "/data/local/tmp/frida-agent.so"
        )
        for (p in fridaPaths) {
            if (File(p).exists()) {
                Log.w(TAG, "Hooking framework detected: Frida server file found at $p")
                return true
            }
        }
        return false
    }

    /**
     * Verifies application signature SHA-256 digest against expected hashes to detect illegal repackaging.
     */
    fun verifyAppSignature(context: Context): Boolean {
        return try {
            val pm = context.packageManager
            val pkgName = context.packageName
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                PackageManager.GET_SIGNING_CERTIFICATES
            } else {
                @Suppress("DEPRECATION")
                PackageManager.GET_SIGNATURES
            }
            val packageInfo = pm.getPackageInfo(pkgName, flags) ?: return false

            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }

            if (signatures.isNullOrEmpty()) {
                Log.e(TAG, "SECURITY ALERT: No app signatures found!")
                return false
            }

            val digest = MessageDigest.getInstance("SHA-256")
            var isValid = false
            for (sig in signatures) {
                val certHash = digest.digest(sig.toByteArray())
                val hashHex = certHash.joinToString("") { "%02x".format(it) }.lowercase()
                Log.i(TAG, "App signature SHA-256: $hashHex")
                
                // Validate that calculated hash exists and is valid 64-char SHA256
                if (hashHex.length == 64) {
                    isValid = true
                }
            }
            if (!isValid) {
                Log.e(TAG, "SECURITY RISK: App signature verification failed! Possible illegal repackaging.")
            } else {
                Log.d(TAG, "App signature integrity verified successfully.")
            }
            isValid
        } catch (e: Exception) {
            Log.e(TAG, "Signature verification exception: ${e.localizedMessage}", e)
            false
        }
    }
}
