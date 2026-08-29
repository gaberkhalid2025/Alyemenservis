package com.example.ui.screens.map.utils

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 📦 OfflineMapManager
 * Manages OSM Tiles Caching safely off the main thread:
 * - 7 days TTL
 * - 50MB maximum cache capacity
 * - Automatic cache purge when exceeding 45MB
 * - Metadata & coordinates for major Yemeni cities (Sana'a, Aden, Taiz, Hodeidah, etc.)
 */
object OfflineMapManager {

    private const val TAG = "OfflineMapManager"
    private const val MAX_CACHE_SIZE_BYTES = 50 * 1024 * 1024L // 50 MB
    private const val PURGE_THRESHOLD_BYTES = 45 * 1024 * 1024L // 45 MB
    private const val CACHE_TTL_MILLIS = 7 * 24 * 60 * 60 * 1000L // 7 days

    data class CityCoordinates(
        val nameAr: String,
        val latitude: Double,
        val longitude: Double,
        val defaultZoom: Int = 13
    )

    val MAJOR_YEMENI_CITIES = listOf(
        CityCoordinates("صنعاء", 15.3694, 44.1910, 13),
        CityCoordinates("عدن", 12.7855, 45.0187, 13),
        CityCoordinates("تعز", 13.5789, 44.0219, 13),
        CityCoordinates("الحديدة", 14.7978, 42.9545, 13),
        CityCoordinates("إب", 13.9667, 44.1667, 13),
        CityCoordinates("حضرموت (المكلا)", 14.5417, 49.1242, 13),
        CityCoordinates("مأرب", 15.4619, 45.3242, 13)
    )

    fun getCityCoordinates(cityName: String): CityCoordinates {
        return MAJOR_YEMENI_CITIES.find { it.nameAr.contains(cityName) || cityName.contains(it.nameAr) }
            ?: CityCoordinates("صنعاء", 15.3694, 44.1910, 13)
    }

    /**
     * Get or create offline map tiles directory
     */
    fun getTileCacheDir(context: Context): File {
        return try {
            val dir = File(context.cacheDir, "osm_tiles_cache")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            dir
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get tile cache directory", e)
            context.cacheDir
        }
    }

    /**
     * Calculate current cache size in MB
     */
    suspend fun getCacheSizeMb(context: Context): Double = withContext(Dispatchers.IO) {
        try {
            val dir = getTileCacheDir(context)
            val sizeBytes = getDirectorySize(dir)
            sizeBytes / (1024.0 * 1024.0)
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating cache size", e)
            0.0
        }
    }

    /**
     * Inspect cache and purge expired tiles (> 7 days) or when size > 45MB
     */
    suspend fun purgeCacheIfNeeded(context: Context) = withContext(Dispatchers.IO) {
        try {
            val dir = getTileCacheDir(context)
            if (!dir.exists()) return@withContext

            val now = System.currentTimeMillis()
            val files = dir.listFiles() ?: return@withContext

            // 1. Delete files older than 7 days
            for (file in files) {
                if (now - file.lastModified() > CACHE_TTL_MILLIS) {
                    file.delete()
                }
            }

            // 2. Check remaining size; if > 45MB, remove oldest files until < 35MB
            var currentSize = getDirectorySize(dir)
            if (currentSize > PURGE_THRESHOLD_BYTES) {
                val sortedFiles = files.filter { it.exists() }.sortedBy { it.lastModified() }
                for (file in sortedFiles) {
                    if (currentSize <= 35 * 1024 * 1024L) break
                    val len = file.length()
                    if (file.delete()) {
                        currentSize -= len
                    }
                }
            }
            Log.d(TAG, "Cache purge completed successfully. Final size: ${currentSize / (1024 * 1024)} MB")
        } catch (e: Exception) {
            Log.e(TAG, "Error purging offline map cache", e)
        }
    }

    private fun getDirectorySize(dir: File): Long {
        var size: Long = 0
        val files = dir.listFiles() ?: return 0
        for (f in files) {
            size += if (f.isDirectory) getDirectorySize(f) else f.length()
        }
        return size
    }
}
