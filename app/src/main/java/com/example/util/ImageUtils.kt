package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream

object ImageUtils {
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
}

