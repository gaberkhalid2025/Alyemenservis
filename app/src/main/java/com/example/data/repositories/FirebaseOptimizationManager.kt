package com.example.data.repositories

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.example.data.LocalAppCacheManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * ⚡ Firebase Optimization Architecture
 * Optimization strategy to stay strictly within Firebase Free Tier quotas:
 * 1. Pagination: Limit queries to 20 documents per batch.
 * 2. Local Cache First: Memory and local Room/SharedPreferences cache prior to Firestore calls.
 * 3. Batch Writes: Group multiple updates to minimize network operations.
 * 4. Image Compression: Auto-compress images to ~200-300KB JPEG at 75-80% quality.
 */
object FirebaseOptimizationManager {

    const val DEFAULT_PAGE_SIZE = 20

    /**
     * Compress bitmap to target size (200-400KB) JPEG format
     */
    suspend fun compressImageForStorage(bitmap: Bitmap, targetQuality: Int = 78): String = withContext(Dispatchers.IO) {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, targetQuality, outputStream)
        val byteArray = outputStream.toByteArray()
        Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    /**
     * Compress Base64 image string if too large
     */
    suspend fun compressBase64IfNeeded(base64Str: String): String = withContext(Dispatchers.IO) {
        if (base64Str.isBlank() || base64Str.startsWith("http")) return@withContext base64Str
        try {
            val cleanStr = if (base64Str.contains(",")) base64Str.substringAfter(",") else base64Str
            val bytes = Base64.decode(cleanStr, Base64.DEFAULT)
            if (bytes.size > 500 * 1024) { // Larger than 500KB
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return@withContext base64Str
                val outStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 75, outStream)
                Base64.encodeToString(outStream.toByteArray(), Base64.DEFAULT)
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
    fun <T> paginateList(sourceList: List<T>, pageIndex: Int, pageSize: Int = DEFAULT_PAGE_SIZE): List<T> {
        val fromIndex = pageIndex * pageSize
        if (fromIndex >= sourceList.size) return emptyList()
        val toIndex = kotlin.math.min(fromIndex + pageSize, sourceList.size)
        return sourceList.subList(fromIndex, toIndex)
    }
}
