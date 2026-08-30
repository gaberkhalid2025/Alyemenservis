package com.example.ui.screens.register.forms

import android.content.Context
import android.content.SharedPreferences

/**
 * Manager for auto-saving and restoring registration drafts.
 */
class RegistrationDraftManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("RegistrationDrafts", Context.MODE_PRIVATE)

    fun saveDraft(role: String, data: Map<String, String>) {
        val editor = prefs.edit()
        data.forEach { (key, value) ->
            editor.putString("${role}_$key", value)
        }
        editor.apply()
    }

    fun getDraft(role: String): Map<String, String> {
        val keys = listOf("entityName", "managerName", "phone", "password", "confirmPassword", "city", "specialization")
        val map = mutableMapOf<String, String>()
        keys.forEach { key ->
            prefs.getString("${role}_$key", null)?.let { value ->
                map[key] = value
            }
        }
        return map
    }

    fun clearDraft(role: String) {
        val keys = listOf("entityName", "managerName", "phone", "password", "confirmPassword", "city", "specialization")
        val editor = prefs.edit()
        keys.forEach { key ->
            editor.remove("${role}_$key")
        }
        editor.apply()
    }
}
