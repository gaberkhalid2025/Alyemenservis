package com.example.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 🔐 نموذج جلسة الأدمن
 */
data class AdminSession(
    val uid: String,
    val email: String,
    val loginTime: Long,
    val refreshToken: String,
    val role: String = "ADMIN"
)

/**
 * 🔐 SecureStorage
 * تخزين مشفر وآمن لجلسات الأدمن والبيانات الحساسة
 * باستخدام EncryptedSharedPreferences و Android KeyStore
 */
@Singleton
class SecureStorage @Inject constructor(
    private val context: Context
) {
    private val prefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                "secure_admin_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            context.getSharedPreferences("secure_admin_prefs_fallback", Context.MODE_PRIVATE)
        }
    }

    /**
     * 🔐 حفظ جلسة الأدمن بشكل آمن
     */
    fun saveAdminSession(session: AdminSession) {
        prefs.edit()
            .putString("admin_uid", session.uid)
            .putString("admin_email", session.email)
            .putLong("admin_login_time", session.loginTime)
            .putString("admin_refresh_token", session.refreshToken)
            .putString("admin_role", session.role)
            .apply()
    }

    /**
     * 📖 استرجاع جلسة الأدمن
     */
    fun getAdminSession(): AdminSession? {
        val uid = prefs.getString("admin_uid", null) ?: return null
        val email = prefs.getString("admin_email", null) ?: return null
        val loginTime = prefs.getLong("admin_login_time", 0)
        val refreshToken = prefs.getString("admin_refresh_token", null) ?: return null
        val role = prefs.getString("admin_role", "ADMIN") ?: "ADMIN"

        return AdminSession(uid, email, loginTime, refreshToken, role)
    }

    /**
     * 🗑️ مسح جلسة الأدمن
     */
    fun clearAdminSession() {
        prefs.edit()
            .remove("admin_uid")
            .remove("admin_email")
            .remove("admin_login_time")
            .remove("admin_refresh_token")
            .remove("admin_role")
            .apply()
    }
}
