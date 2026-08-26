package com.example

import android.app.Application
import android.os.Bundle
import android.util.Log
import com.example.BuildConfig
import com.example.util.FirestoreLocalBackupWorker
import com.example.util.SecurityManager
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

/**
 * 🏢 MyApplication
 * فئة التطبيق المركزية لتهيئة Firebase، أدوات الأمان، محرك الصوت والنسخ الاحتياطي التلقائي.
 */
class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this

        initFirebase()
        initSecurityAndBackup()
        initVoiceEngine()
    }

    private fun initFirebase() {
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
            }
            try {
                val firebaseAppCheck = FirebaseAppCheck.getInstance()
                if (BuildConfig.DEBUG) {
                    firebaseAppCheck.installAppCheckProviderFactory(
                        DebugAppCheckProviderFactory.getInstance()
                    )
                } else {
                    firebaseAppCheck.installAppCheckProviderFactory(
                        PlayIntegrityAppCheckProviderFactory.getInstance()
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "AppCheck initialization notice: ${e.message}")
            }

            firebaseAnalytics = FirebaseAnalytics.getInstance(this)
        } catch (e: Exception) {
            Log.e(TAG, "Firebase initialization error", e)
        }
    }

    private fun initSecurityAndBackup() {
        try {
            SecurityManager.verifyAppSignature(this)
            FirestoreLocalBackupWorker.schedulePeriodicBackup(this)
        } catch (e: Exception) {
            Log.e(TAG, "Security/Backup initialization error", e)
        }
    }

    private fun initVoiceEngine() {
        try {
            VoiceManager.init(this)
        } catch (e: Exception) {
            Log.e(TAG, "VoiceManager init error", e)
        }
    }

    companion object {
        private const val TAG = "MyApplication"
        private var instance: MyApplication? = null
        private var firebaseAnalytics: FirebaseAnalytics? = null

        fun getInstance(): MyApplication? = instance

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
