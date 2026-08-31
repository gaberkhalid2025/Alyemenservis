package com.example.data

import com.example.utils.*

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * 📦 Local App Cache Manager & Offline Sync Storage Engine
 * Solves Problem 6: Offline caching, fast local rendering, queued sync operations,
 * and automatic cache pruning for stale records older than 30 days.
 */
class LocalAppCacheManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("YS_Local_App_Cache_v2026", Context.MODE_PRIVATE)

    // 1. Save & Load Cached Providers
    fun saveProvidersCache(rawJsonString: String) {
        prefs.edit().putString("KEY_PROVIDERS_CACHE", rawJsonString).putLong("KEY_PROVIDERS_TIME", System.currentTimeMillis()).apply()
    }

    fun getProvidersCacheRaw(): String {
        return prefs.getString("KEY_PROVIDERS_CACHE", "[]") ?: "[]"
    }

    // 2. Save & Load Cached Stores
    fun saveStoresCache(rawJsonString: String) {
        prefs.edit().putString("KEY_STORES_CACHE", rawJsonString).putLong("KEY_STORES_TIME", System.currentTimeMillis()).apply()
    }

    fun getStoresCacheRaw(): String {
        return prefs.getString("KEY_STORES_CACHE", "[]") ?: "[]"
    }

    // 3. Save & Load Cached Bookings
    fun saveBookingsCache(rawJsonString: String) {
        prefs.edit().putString("KEY_BOOKINGS_CACHE", rawJsonString).putLong("KEY_BOOKINGS_TIME", System.currentTimeMillis()).apply()
    }

    fun getBookingsCacheRaw(): String {
        return prefs.getString("KEY_BOOKINGS_CACHE", "[]") ?: "[]"
    }

    // 4. Save & Load Cached Categories
    fun saveCategoriesCache(rawJsonString: String) {
        prefs.edit().putString("KEY_CATEGORIES_CACHE", rawJsonString).apply()
    }

    fun getCategoriesCacheRaw(): String {
        return prefs.getString("KEY_CATEGORIES_CACHE", "[]") ?: "[]"
    }

    // 5. Offline Queue Operations (When user creates booking or sends message offline)
    data class OfflineSyncAction(
        val id: String = java.util.UUID.randomUUID().toString(),
        val type: String, // "CREATE_BOOKING", "UPDATE_PROFILE", "SEND_MESSAGE"
        val payloadJson: String,
        val createdAt: Long = System.currentTimeMillis()
    )

    fun queueOfflineAction(action: OfflineSyncAction) {
        val currentQueue = getOfflineQueueRaw()
        try {
            val jsonArray = JSONArray(currentQueue)
            val obj = JSONObject().apply {
                put("id", action.id)
                put("type", action.type)
                put("payloadJson", action.payloadJson)
                put("createdAt", action.createdAt)
            }
            jsonArray.put(obj)
            prefs.edit().putString("KEY_OFFLINE_QUEUE", jsonArray.toString()).apply()
        } catch (e: Exception) {
            // Safe fallback
        }
    }

    fun getOfflineQueueRaw(): String {
        return prefs.getString("KEY_OFFLINE_QUEUE", "[]") ?: "[]"
    }

    fun clearOfflineQueue() {
        prefs.edit().remove("KEY_OFFLINE_QUEUE").apply()
    }

    // 6. Automatic Cache Pruning (Removes cached data older than 30 days)
    fun pruneStaleCache() {
        val now = System.currentTimeMillis()
        val thirtyDaysMs = 30L * 24 * 60 * 60 * 1000

        val providersTime = prefs.getLong("KEY_PROVIDERS_TIME", 0)
        if (now - providersTime > thirtyDaysMs) {
            prefs.edit().remove("KEY_PROVIDERS_CACHE").remove("KEY_PROVIDERS_TIME").apply()
        }

        val storesTime = prefs.getLong("KEY_STORES_TIME", 0)
        if (now - storesTime > thirtyDaysMs) {
            prefs.edit().remove("KEY_STORES_CACHE").remove("KEY_STORES_TIME").apply()
        }
    }
}
