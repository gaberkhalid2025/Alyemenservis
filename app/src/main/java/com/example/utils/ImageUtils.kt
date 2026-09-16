package com.example.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

object ImageUtils {
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
    fun uriToBase64(
        context: Context,
        uri: Uri,
        maxWidth: Int = 800,
        quality: Int = 75
    ): String {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return ""
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            if (originalBitmap == null) return ""

            val ratio = originalBitmap.width.toFloat() / originalBitmap.height.toFloat()
            val targetWidth = minOf(maxWidth, originalBitmap.width)
            val targetHeight = (targetWidth / ratio).toInt()

            val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, targetWidth, targetHeight, true)

            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)

            if (scaledBitmap != originalBitmap) originalBitmap.recycle()
            originalBitmap.recycle()

            Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    fun uriToCompressedBase64(context: Context, uri: Uri, maxWidth: Int = 800, maxHeight: Int = 800, quality: Int = 75): String {
        return uriToBase64(context, uri, maxWidth, quality)
    }

    suspend fun compressAndOptimizeImage(
        context: Context,
        uri: Uri,
        maxWidth: Int = 800,
        maxHeight: Int = 800,
        quality: Int = 80,
        maxSizeKB: Int = 200
    ): String {
        return withContext(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext ""
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream.close()
                if (bitmap == null) return@withContext ""
                
                val scaledBitmap = if (bitmap.width > maxWidth || bitmap.height > maxHeight) {
                    val ratio = minOf(maxWidth.toFloat() / bitmap.width, maxHeight.toFloat() / bitmap.height)
                    Bitmap.createScaledBitmap(
                        bitmap,
                        (bitmap.width * ratio).toInt(),
                        (bitmap.height * ratio).toInt(),
                        true
                    )
                } else bitmap
                
                var currentQuality = quality
                var compressedBytes: ByteArray
                var outputStream: ByteArrayOutputStream
                
                do {
                    outputStream = ByteArrayOutputStream()
                    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, currentQuality, outputStream)
                    compressedBytes = outputStream.toByteArray()
                    currentQuality -= 10
                } while (compressedBytes.size > maxSizeKB * 1024 && currentQuality >= 20)
                
                Base64.encodeToString(compressedBytes, Base64.DEFAULT)
            } catch (e: Exception) {
                e.printStackTrace()
                ""
            }
        }
    }
}

object ImageOptimizationUtils {
    suspend fun compressAndOptimizeImage(
        context: Context,
        uri: Uri,
        maxWidth: Int = 800,
        maxHeight: Int = 800,
        quality: Int = 80,
        maxSizeKB: Int = 200
    ): String {
        return ImageUtils.compressAndOptimizeImage(context, uri, maxWidth, maxHeight, quality, maxSizeKB)
    }
}

fun convertGenericUriToBase64(context: Context, uri: Uri): String {
    return ImageUtils.uriToBase64(context, uri)
}

fun convertBitmapToBase64(bitmap: Bitmap): String {
    return try {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
        Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    } catch (e: Exception) {
        ""
    }
}

fun compressAndResizeImageUri(context: Context, uri: Uri, maxDimension: Int = 800, quality: Int = 70): String {
    return ImageUtils.uriToBase64(context, uri, maxDimension, quality)
}
