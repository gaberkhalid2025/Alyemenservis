package com.example

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore

object FirebaseManager {
    private const val TAG = "FirebaseManager"
    private var isInitialized = false

    fun initialize(context: Context) {
        if (isInitialized) return
        try {
            Log.d(TAG, "Initializing Firebase programmatically...")
            val options = FirebaseOptions.Builder()
                .setApiKey("AIzaSyBfVDnH4FfA_lR6HQFwOWSSBRcN__InczE")
                .setApplicationId("1:1083437554902:android:6dc687a70a84eabc")
                .setProjectId("al-yemen-services")
                .build()

            FirebaseApp.initializeApp(context, options)
            isInitialized = true
            Log.d(TAG, "Firebase initialized successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing programmatically: ${e.message}", e)
        }
    }

    val firestore: FirebaseFirestore?
        get() = if (isInitialized) {
            try {
                FirebaseFirestore.getInstance()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get Firestore instance: ${e.message}")
                null
            }
        } else {
            Log.w(TAG, "Firebase not yet initialized when accessing Firestore")
            null
        }
}
