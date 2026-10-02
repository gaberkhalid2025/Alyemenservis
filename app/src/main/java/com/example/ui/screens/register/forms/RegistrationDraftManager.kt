package com.example.ui.screens.register.forms

import android.content.Context
import com.example.ui.*
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Manager for auto-saving and restoring registration drafts securely using EncryptedSharedPreferences.
 */
class RegistrationDraftManager(context: Context) {
    private val prefs: SharedPreferences = run {
        val prefsName = "RegistrationDraftsSecure"
        fun createEncrypted(): SharedPreferences {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            return EncryptedSharedPreferences.create(
                context,
                prefsName,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }
        try {
            createEncrypted()
        } catch (e: Exception) {
            try {
                createEncrypted()
            } catch (e2: Exception) {
                throw SecurityException("❌ خطأ أمني حرج: تعذر إنشاء مساحة تخزين آمنة للمسودات. يرجى مسح بيانات التطبيق.")
            }
        }
    }

    fun saveDraft(role: String, data: Map<String, String>) {
        val editor = prefs.edit()
        data.forEach { (key, value) ->
            if (key == "password" || key == "confirmPassword") return@forEach
            editor.putString("${role}_$key", value)
        }
        editor.apply()
    }

    fun getDraft(role: String): Map<String, String> {
        val keys = listOf("entityName", "managerName", "phone", "city", "specialization")
        val map = mutableMapOf<String, String>()
        keys.forEach { key ->
            prefs.getString("${role}_$key", null)?.let { value ->
                map[key] = value
            }
        }
        return map
    }

    fun clearDraft(role: String) {
        val keys = listOf("entityName", "managerName", "phone", "city", "specialization")
        val editor = prefs.edit()
        keys.forEach { key ->
            editor.remove("${role}_$key")
        }
        editor.apply()
    }
}
