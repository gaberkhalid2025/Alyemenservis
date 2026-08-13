package com.example.util

import android.content.Context
import android.net.Uri
import android.util.Log

object ImageUploader {
    fun compressAndUpload(
        context: Context,
        imageUri: Uri,
        onProgress: (Float) -> Unit,
        onComplete: (String?) -> Unit
    ) {
        Log.d("ImageUploader", "Compressing image: $imageUri using settings (Quality: ${ImageSettings.maxQualityPercent}%)")
        // Mock progress updates
        onProgress(0.2f)
        onProgress(0.6f)
        onProgress(1.0f)
        onComplete("https://firebasestorage.googleapis.com/v0/b/mock/o/uploaded_compressed_image.jpg")
    }

    fun compressAndUploadFromUrl(
        url: String,
        onComplete: (String?) -> Unit
    ) {
        Log.d("ImageUploader", "Uploading image from remote URL with proxy: $url")
        onComplete(url)
    }
}
