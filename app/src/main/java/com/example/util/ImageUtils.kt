package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream

object ImageUtils {
    fun uriToBase64(context: Context, uri: Uri, maxWidth: Int = 800, quality: Int = 75): String {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return ""
            val bitmap = BitmapFactory.decodeStream(inputStream) ?: return ""
            val scaledBitmap = if (bitmap.width > maxWidth) {
                val ratio = bitmap.height.toFloat() / bitmap.width.toFloat()
                val targetHeight = (maxWidth * ratio).toInt()
                Bitmap.createScaledBitmap(bitmap, maxWidth, targetHeight, true)
            } else {
                bitmap
            }
            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            val bytes = outputStream.toByteArray()
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            ""
        }
    }

    fun uriToCompressedBase64(context: Context, uri: Uri, maxWidth: Int = 800, maxHeight: Int = 800, quality: Int = 75): String {
        return uriToBase64(context, uri, maxWidth, quality)
    }
}
