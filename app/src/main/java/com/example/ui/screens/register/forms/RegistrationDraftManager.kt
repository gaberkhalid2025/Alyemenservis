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
        val appCtx = context.applicationContext ?: context
        fun createEncrypted(): SharedPreferences {
            val masterKey = MasterKey.Builder(appCtx)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            return EncryptedSharedPreferences.create(
                appCtx,
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
                appCtx.deleteSharedPreferences(prefsName)
                createEncrypted()
            } catch (e2: Exception) {
                appCtx.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            }
        }
    }

    private val ignoredKeys = setOf("password", "confirmPassword", "rawPassword")

    fun saveDraft(role: String, data: Map<String, String>) {
        val editor = prefs.edit()
        val prefix = "${role}_"
        // Clear stale keys for this role not present or emptied
        prefs.all.keys.filter { it.startsWith(prefix) }.forEach { existingKey ->
            val shortKey = existingKey.removePrefix(prefix)
            if (shortKey !in data || shortKey in ignoredKeys) {
                editor.remove(existingKey)
            }
        }
        data.forEach { (key, value) ->
            if (key in ignoredKeys) return@forEach
            editor.putString("$prefix$key", value)
        }
        editor.apply()
    }

    fun getDraft(role: String): Map<String, String> {
        val prefix = "${role}_"
        val map = mutableMapOf<String, String>()
        prefs.all.forEach { (fullKey, rawVal) ->
            if (fullKey.startsWith(prefix) && rawVal is String) {
                val shortKey = fullKey.removePrefix(prefix)
                if (shortKey !in ignoredKeys) {
                    map[shortKey] = rawVal
                }
            }
        }
        return map
    }

    fun clearDraft(role: String) {
        val prefix = "${role}_"
        val editor = prefs.edit()
        prefs.all.keys.filter { it.startsWith(prefix) }.forEach { fullKey ->
            editor.remove(fullKey)
        }
        editor.apply()
    }
}
