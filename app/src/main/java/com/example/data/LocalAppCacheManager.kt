package com.example.data

import com.example.utils.*

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONArray
import org.json.JSONObject

/**
 * 📦 Local App Cache Manager & Offline Sync Storage Engine
 * Solves Problem 6: Offline caching, fast local rendering, queued sync operations,
 * and automatic cache pruning for stale records older than 30 days.
 */
@Singleton
class LocalAppCacheManager @Inject constructor(
    @ApplicationContext context: Context
) {

    private val prefs: SharedPreferences = context.getSharedPreferences("YS_Local_App_Cache_v2026", Context.MODE_PRIVATE)

    companion object {
        private const val CACHE_TTL_MS = 12L * 60L * 60L * 1000L // 12 hours TTL
    }

    private fun saveWithTimestamp(dataKey: String, timeKey: String, rawJsonString: String) {
        if (rawJsonString.isBlank()) return
        prefs.edit()
            .putString(dataKey, rawJsonString)
            .putLong(timeKey, System.currentTimeMillis())
            .apply()
    }

    private fun readIfFresh(dataKey: String, timeKey: String): String {
        val savedAt = prefs.getLong(timeKey, 0L)
        if (savedAt <= 0L || System.currentTimeMillis() - savedAt > CACHE_TTL_MS) {
            return "[]"
        }
        return prefs.getString(dataKey, "[]") ?: "[]"
    }

    // 1. Save & Load Cached Providers (12h TTL)
    fun saveProvidersCache(rawJsonString: String) {
        saveWithTimestamp("KEY_PROVIDERS_CACHE", "KEY_PROVIDERS_TIME", rawJsonString)
    }

    fun getProvidersCacheRaw(): String {
        return readIfFresh("KEY_PROVIDERS_CACHE", "KEY_PROVIDERS_TIME")
    }

    // 2. Save & Load Cached Stores (12h TTL)
    fun saveStoresCache(rawJsonString: String) {
        saveWithTimestamp("KEY_STORES_CACHE", "KEY_STORES_TIME", rawJsonString)
    }

    fun getStoresCacheRaw(): String {
        return readIfFresh("KEY_STORES_CACHE", "KEY_STORES_TIME")
    }

    // 3. Bookings Cache -> Delegated exclusively to Room Database (AppDatabase.bookingDao())
    init {
        if (prefs.contains("KEY_BOOKINGS_CACHE") || prefs.contains("KEY_BOOKINGS_TIME")) {
            prefs.edit().remove("KEY_BOOKINGS_CACHE").remove("KEY_BOOKINGS_TIME").apply()
        }
    }

    fun saveOffersCache(rawJsonString: String) {
        saveWithTimestamp("KEY_OFFERS_CACHE", "KEY_OFFERS_TIME", rawJsonString)
    }

    fun getOffersCacheRaw(): String {
        return readIfFresh("KEY_OFFERS_CACHE", "KEY_OFFERS_TIME")
    }

    fun getOffersCacheTime(): Long {
        return prefs.getLong("KEY_OFFERS_TIME", 0L)
    }

    // 4. Save & Load Cached Categories (12h TTL)
    fun saveCategoriesCache(rawJsonString: String) {
        saveWithTimestamp("KEY_CATEGORIES_CACHE", "KEY_CATEGORIES_TIME", rawJsonString)
    }

    fun getCategoriesCacheRaw(): String {
        return readIfFresh("KEY_CATEGORIES_CACHE", "KEY_CATEGORIES_TIME")
    }

    // 4.1 Save & Load Cached Jobs (12h TTL)
    fun saveJobsCache(ownerId: String, rawJsonString: String) {
        val key = ownerId.trim()
        if (key.isBlank()) return
        saveWithTimestamp("KEY_JOBS_CACHE_$key", "KEY_JOBS_TIME_$key", rawJsonString)
    }

    fun getJobsCacheRaw(ownerId: String): String {
        val key = ownerId.trim()
        if (key.isBlank()) return "[]"
        return readIfFresh("KEY_JOBS_CACHE_$key", "KEY_JOBS_TIME_$key")
    }

    // 4.2 Save & Load Cached Doctors (12h TTL)
    fun saveDoctorsCache(ownerId: String, rawJsonString: String) {
        val key = ownerId.trim()
        if (key.isBlank()) return
        saveWithTimestamp("KEY_DOCTORS_CACHE_$key", "KEY_DOCTORS_TIME_$key", rawJsonString)
    }

    fun getDoctorsCacheRaw(ownerId: String): String {
        val key = ownerId.trim()
        if (key.isBlank()) return "[]"
        return readIfFresh("KEY_DOCTORS_CACHE_$key", "KEY_DOCTORS_TIME_$key")
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
            val jsonArray = try {
                JSONArray(currentQueue)
            } catch (_: Exception) {
                JSONArray()
            }
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
        val editor = prefs.edit()
        var modified = false

        val cachePairs = listOf(
            "KEY_PROVIDERS_CACHE" to "KEY_PROVIDERS_TIME",
            "KEY_STORES_CACHE" to "KEY_STORES_TIME",
            "KEY_OFFERS_CACHE" to "KEY_OFFERS_TIME",
            "KEY_CATEGORIES_CACHE" to "KEY_CATEGORIES_TIME"
        )

        for ((cacheKey, timeKey) in cachePairs) {
            val savedTime = prefs.getLong(timeKey, 0L)
            if (savedTime > 0L && now - savedTime > thirtyDaysMs) {
                editor.remove(cacheKey).remove(timeKey)
                modified = true
            }
        }

        if (modified) {
            editor.apply()
        }
    }
}
