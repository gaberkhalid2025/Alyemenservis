package com.example.ui.utils

import androidx.compose.ui.graphics.Color

fun convertGenericUriToBase64(context: android.content.Context, uri: android.net.Uri): String {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return ""
        val bytes = inputStream.readBytes()
        inputStream.close()
        android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT)
    } catch (e: Exception) { "" }
}

fun convertBitmapToBase64(bitmap: android.graphics.Bitmap): String {
    return try {
        val reqWidth = 220
        val reqHeight = 220
        val scaledBitmap = if (bitmap.width > reqWidth || bitmap.height > reqHeight) {
            val ratio = Math.min(reqWidth.toFloat() / bitmap.width, reqHeight.toFloat() / bitmap.height)
            android.graphics.Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * ratio).toInt(),
                (bitmap.height * ratio).toInt(),
                true
            )
        } else {
            bitmap
        }
        val outputStream = java.io.ByteArrayOutputStream()
        scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 55, outputStream)
        val bytes = outputStream.toByteArray()
        if (scaledBitmap != bitmap) {
            scaledBitmap.recycle()
        }
        android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT)
    } catch (e: Exception) { "" }
}

fun compressAndResizeImageUri(context: android.content.Context, uri: android.net.Uri, maxDimension: Int = 800, quality: Int = 70): String {
    return try {
        val contentResolver = context.contentResolver
        val inputStreamForBounds = contentResolver.openInputStream(uri) ?: return ""
        val options = android.graphics.BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        android.graphics.BitmapFactory.decodeStream(inputStreamForBounds, null, options)
        inputStreamForBounds.close()

        var inSampleSize = 1
        if (options.outHeight > maxDimension || options.outWidth > maxDimension) {
            val halfHeight = options.outHeight / 2
            val halfWidth = options.outWidth / 2
            while ((halfHeight / inSampleSize) >= maxDimension && (halfWidth / inSampleSize) >= maxDimension) {
                inSampleSize *= 2
            }
        }

        val finalOptions = android.graphics.BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize
        }
        val inputStreamForDecode = contentResolver.openInputStream(uri) ?: return ""
        val decodedBitmap = android.graphics.BitmapFactory.decodeStream(inputStreamForDecode, null, finalOptions)
        inputStreamForDecode.close()

        if (decodedBitmap != null) {
            val scaledBitmap = if (decodedBitmap.width > maxDimension || decodedBitmap.height > maxDimension) {
                val ratio = Math.min(maxDimension.toFloat() / decodedBitmap.width, maxDimension.toFloat() / decodedBitmap.height)
                android.graphics.Bitmap.createScaledBitmap(
                    decodedBitmap,
                    (decodedBitmap.width * ratio).toInt(),
                    (decodedBitmap.height * ratio).toInt(),
                    true
                )
            } else {
                decodedBitmap
            }

            val outputStream = java.io.ByteArrayOutputStream()
            scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, outputStream)
            val bytes = outputStream.toByteArray()
            if (scaledBitmap != decodedBitmap) {
                scaledBitmap.recycle()
            }
            decodedBitmap.recycle()
            android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        } else ""
    } catch (e: Exception) {
        e.printStackTrace()
        ""
    }
}

fun Color.luminance(): Float {
    return (0.2126f * this.red + 0.7152f * this.green + 0.0722f * this.blue)
}

fun isMoreThan8HoursBefore(dateStr: String, timeStr: String): Boolean {
    return try {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US)
        val bookingDate = sdf.parse("$dateStr $timeStr")
        if (bookingDate != null) {
            val diffMs = bookingDate.time - System.currentTimeMillis()
            val diffHours = diffMs / (1000 * 60 * 60)
            diffHours >= 8
        } else {
            true
        }
    } catch (e: Exception) {
        true
    }
}
