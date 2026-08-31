package com.example

import android.app.Application
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        try {
            firebaseAnalytics = FirebaseAnalytics.getInstance(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        private var instance: MyApplication? = null
        private var firebaseAnalytics: FirebaseAnalytics? = null

        @JvmStatic
        fun logFirebaseEvent(name: String, params: Bundle) {
            try {
                firebaseAnalytics?.logEvent(name, params)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
