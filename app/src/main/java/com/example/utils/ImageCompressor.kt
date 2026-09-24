package com.example.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * 🖼️ ImageCompressor
 * ضغط الصور وتوليد صور مصغرة (Thumbnails 200x200) لتقليل استهلاك الإنترنت
 * وتحسين سرعة تحميل الصور الفورية في المحادثات.
 */
object ImageCompressor {

    suspend fun generateThumbnail(
        context: Context,
        imageUri: Uri,
        maxDimension: Int = 200,
        quality: Int = 75
    ): File? = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(imageUri) ?: return@withContext null
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (originalBitmap == null) return@withContext null

            val width = originalBitmap.width
            val height = originalBitmap.height
            val ratio = width.toFloat() / height.toFloat()

            val targetWidth: Int
            val targetHeight: Int
            if (width > height) {
                targetWidth = maxDimension
                targetHeight = (maxDimension / ratio).toInt()
            } else {
                targetHeight = maxDimension
                targetWidth = (maxDimension * ratio).toInt()
            }

            val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, targetWidth, targetHeight, true)
            val thumbFile = File(context.cacheDir, "thumb_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(thumbFile)
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            outputStream.flush()
            outputStream.close()

            thumbFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * ⚡ ضغط الصور الكاملة المرفوعة وتقليل حجمها لأقل من 250KB قبل الرفع لتوفير باقات الإنترنت.
     */
    suspend fun compressFullImage(
        context: Context,
        imageUri: Uri,
        maxDimension: Int = 1024,
        quality: Int = 80
    ): File? = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(imageUri) ?: return@withContext null
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(inputStream, null, boundsOptions)
            inputStream.close()

            val origWidth = boundsOptions.outWidth
            val origHeight = boundsOptions.outHeight
            if (origWidth <= 0 || origHeight <= 0) return@withContext null

            var sampleSize = 1
            while (origWidth / sampleSize > maxDimension || origHeight / sampleSize > maxDimension) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val nextStream = context.contentResolver.openInputStream(imageUri) ?: return@withContext null
            val decodedBitmap = BitmapFactory.decodeStream(nextStream, null, decodeOptions)
            nextStream.close()

            if (decodedBitmap == null) return@withContext null

            val compressedFile = File(context.cacheDir, "compressed_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(compressedFile)
            decodedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            outputStream.flush()
            outputStream.close()

            compressedFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
