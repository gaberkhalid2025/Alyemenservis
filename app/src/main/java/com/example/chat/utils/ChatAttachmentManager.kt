package com.example.chat.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

/**
 * 🖼️ ChatAttachmentManager
 * Optimizes media uploads (e.g., generating 200x200 thumbnails).
 */
class ChatAttachmentManager(private val context: Context) {
    
    fun compressImageForThumbnail(uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            
            // 200x200 Thumbnail generation for initial fast loading
            val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, 200, 200, true)
            
            val file = File(context.cacheDir, "thumb_${System.currentTimeMillis()}.jpg")
            val fos = FileOutputStream(file)
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 70, fos)
            fos.flush()
            fos.close()
            
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
