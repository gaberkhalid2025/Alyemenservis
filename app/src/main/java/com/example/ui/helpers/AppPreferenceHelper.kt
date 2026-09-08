package com.example.ui.helpers

import android.content.Context
import com.example.ui.*
import android.content.SharedPreferences

/**
 * Helper class to centralize and safely handle SharedPreferences operations across the application.
 */
class AppPreferenceHelper {

    companion object {
        private const val PREFS_YEMEN_SERVICE = "yemen_service_prefs"
        private const val PREFS_APP = "app_prefs"

        private const val KEY_JOIN_REQUEST_PHONE = "join_request_phone"
        private const val KEY_PASSWORD_RECOVERY_WAITING_PHONE = "password_recovery_waiting_phone"
        private const val KEY_IS_ACCOUNT_LOGGED_IN = "is_account_logged_in"
        private const val KEY_READ_NOTIFICATIONS = "read_notifications"
    }

    private fun getYemenServicePrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_YEMEN_SERVICE, Context.MODE_PRIVATE)
    }

    private fun getAppPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_APP, Context.MODE_PRIVATE)
    }

    // --- Join Request Phone ---
    fun getJoinRequestPhone(context: Context): String {
        return getYemenServicePrefs(context).getString(KEY_JOIN_REQUEST_PHONE, "") ?: ""
    }

    fun setJoinRequestPhone(context: Context, phone: String) {
        getYemenServicePrefs(context).edit().putString(KEY_JOIN_REQUEST_PHONE, phone).apply()
    }

    fun clearJoinRequestPhone(context: Context) {
        getYemenServicePrefs(context).edit().remove(KEY_JOIN_REQUEST_PHONE).apply()
    }

    // --- Password Recovery Waiting Phone ---
    fun getPasswordRecoveryWaitingPhone(context: Context): String {
        return getYemenServicePrefs(context).getString(KEY_PASSWORD_RECOVERY_WAITING_PHONE, "") ?: ""
    }

    fun setPasswordRecoveryWaitingPhone(context: Context, phone: String) {
        getYemenServicePrefs(context).edit().putString(KEY_PASSWORD_RECOVERY_WAITING_PHONE, phone).apply()
    }

    // --- Account Login Status ---
    fun isAccountLoggedIn(context: Context): Boolean {
        return getYemenServicePrefs(context).getBoolean(KEY_IS_ACCOUNT_LOGGED_IN, false)
    }

    fun setAccountLoggedIn(context: Context, isLoggedIn: Boolean) {
        getYemenServicePrefs(context).edit().putBoolean(KEY_IS_ACCOUNT_LOGGED_IN, isLoggedIn).apply()
    }

    // --- Notifications Read Status ---
    fun getReadNotificationIds(context: Context): Set<String> {
        return getAppPrefs(context).getStringSet(KEY_READ_NOTIFICATIONS, emptySet()) ?: emptySet()
    }

    fun markNotificationAsRead(context: Context, notifId: String, currentRead: MutableSet<String>): Set<String> {
        currentRead.add(notifId)
        getAppPrefs(context).edit().putStringSet(KEY_READ_NOTIFICATIONS, currentRead).apply()
        return currentRead
    }

    fun markAllNotificationsAsRead(context: Context, allIds: Set<String>): Set<String> {
        getAppPrefs(context).edit().putStringSet(KEY_READ_NOTIFICATIONS, allIds).apply()
        return allIds
    }
}
