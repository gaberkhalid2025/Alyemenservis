package com.example.data.repositories

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * ⚡ Firebase Optimization Architecture
 * Optimization strategy to stay strictly within Firebase Spark Plan limits:
 * - 50,000 reads/day
 * - 20,000 writes/day
 * - 1,000 deletes/day
 * 
 * Features:
 * 1. Pagination: 15-20 msgs/batch, 10-15 bookings/batch, 10 urgent requests/batch.
 * 2. TTL Caching: Channels (5m), Messages (30s), Bookings (5m), Urgent Requests (30s).
 * 3. Delta Sync & Batch Writes.
 * 4. Image Compression: WebP format under 150KB.
 */
object FirebaseOptimizationManager {

    // Pagination constants
    const val CHAT_PAGE_SIZE = 15
    const val BOOKING_PAGE_SIZE = 10
    const val URGENT_REQUEST_PAGE_SIZE = 10

    // TTL Cache durations (milliseconds)
    const val TTL_CHANNELS_MS = 5 * 60 * 1000L      // 5 minutes
    const val TTL_MESSAGES_MS = 30 * 1000L          // 30 seconds
    const val TTL_BOOKINGS_MS = 5 * 60 * 1000L      // 5 minutes
    const val TTL_URGENT_MS = 30 * 1000L            // 30 seconds

    /**
     * Helper to verify if cache is still valid based on TTL
     */
    fun isCacheValid(lastSyncedAt: Long, ttlMillis: Long): Boolean {
        return (System.currentTimeMillis() - lastSyncedAt) < ttlMillis
    }

    /**
     * Compress bitmap to target WebP format (< 150KB)
     */
    suspend fun compressImageToWebP(bitmap: Bitmap, maxSizeBytes: Long = 150 * 1024L): String = withContext(Dispatchers.IO) {
        var quality = 80
        var outputStream = ByteArrayOutputStream()
        
        val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Bitmap.CompressFormat.WEBP_LOSSY
        } else {
            @Suppress("DEPRECATION")
            Bitmap.CompressFormat.WEBP
        }

        bitmap.compress(format, quality, outputStream)
        
        while (outputStream.toByteArray().size > maxSizeBytes && quality > 20) {
            outputStream.reset()
            quality -= 15
            bitmap.compress(format, quality, outputStream)
        }

        val byteArray = outputStream.toByteArray()
        Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    /**
     * Compress Base64 image string if needed to keep under 150KB WebP
     */
    suspend fun compressBase64IfNeeded(base64Str: String): String = withContext(Dispatchers.IO) {
        if (base64Str.isBlank() || base64Str.startsWith("http")) return@withContext base64Str
        try {
            val cleanStr = if (base64Str.contains(",")) base64Str.substringAfter(",") else base64Str
            val bytes = Base64.decode(cleanStr, Base64.DEFAULT)
            if (bytes.size > 150 * 1024) { // Larger than 150KB
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return@withContext base64Str
                compressImageToWebP(bitmap)
            } else {
                base64Str
            }
        } catch (e: Exception) {
            base64Str
        }
    }

    /**
     * Generic pagination helper for lists
     */
    fun <T> paginateList(sourceList: List<T>, pageIndex: Int, pageSize: Int = CHAT_PAGE_SIZE): List<T> {
        val fromIndex = pageIndex * pageSize
        if (fromIndex >= sourceList.size) return emptyList()
        val toIndex = kotlin.math.min(fromIndex + pageSize, sourceList.size)
        return sourceList.subList(fromIndex, toIndex)
    }
}
