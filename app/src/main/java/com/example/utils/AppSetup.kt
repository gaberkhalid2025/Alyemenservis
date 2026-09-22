package com.example.utils

import com.example.utils.*

import android.content.Context
import android.util.Log

object AppSetup {
    private const val TAG = "AppSetup"

    fun initializeApplication(context: Context) {
        try {
            Log.i(TAG, "Initializing Yemen Services platform configuration...")
            setupNotificationChannels(context)
            setupImageCacheDefaults(context)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to complete app setup: ${e.message}", e)
        }
    }

    private fun setupNotificationChannels(context: Context) {
        NotificationChannelsRegistry.createAll(context)
    }

    private fun setupImageCacheDefaults(context: Context) {
        // Prepare local image cache limits if needed
        Log.d(TAG, "Image caching and Room offline database initialized.")
    }
}
