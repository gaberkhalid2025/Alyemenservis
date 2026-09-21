package com.example

import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MyApplication : Application() {

    companion object {
        var instance: MyApplication? = null
            private set
        private var firebaseAnalytics: FirebaseAnalytics? = null
        private var isCrashlyticsReady = false

        @JvmStatic
        fun logFirebaseEvent(name: String, params: Bundle) {
            try {
                firebaseAnalytics?.logEvent(name, params)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // ===================== الخطوة 1: تهيئة Firebase بأمان =====================
        try {
            FirebaseApp.initializeApp(this)
            Log.d("MyApplication", "✅ Firebase initialized successfully")

            // 🔐 Firebase App Check - حماية من Abuse
            val firebaseAppCheck = FirebaseAppCheck.getInstance()
            firebaseAppCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            )
            // تفعيل الـ Debug Provider في وضع التطوير
            if (BuildConfig.DEBUG) {
                firebaseAppCheck.installAppCheckProviderFactory(
                    DebugAppCheckProviderFactory.getInstance()
                )
            }

            // محاولة تسجيل الدخول كمجهول بأمان دون تعطيل مسار التطبيق
            try {
                val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
                if (auth.currentUser == null) {
                    auth.signInAnonymously()
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Log.d("AnonymousAuth", "✅ UID: ${task.result?.user?.uid}")
                            } else {
                                Log.w("AnonymousAuth", "⚠️ Anonymous Auth note: ${task.exception?.message}")
                            }
                        }
                } else {
                    Log.d("AnonymousAuth", "✅ Existing UID: ${auth.currentUser?.uid}")
                }
            } catch (authEx: Throwable) {
                Log.w("AnonymousAuth", "⚠️ Auth init skipped: ${authEx.message}")
            }
        } catch (e: Exception) {
            Log.e("MyApplication", "❌ Firebase initialization failed: ${e.message}")
            e.printStackTrace()
        }

        // ===================== تهيئة Firestore الموحدة =====================
        try {
            val firestore = FirebaseFirestore.getInstance()
            val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                .build()
            firestore.firestoreSettings = settings
            Log.d("MyApplication", "✅ FirebaseFirestore settings initialized successfully")
        } catch (e: Exception) {
            // تجاهل — قد تكون مهيأة مسبقاً
            try {
                val firestore = FirebaseFirestore.getInstance()
                val settings = FirebaseFirestoreSettings.Builder()
                    .setPersistenceEnabled(true)
                    .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                    .build()
                firestore.firestoreSettings = settings
            } catch (e2: Exception) {
                e2.printStackTrace()
            }
        }

        // ===================== الخطوة 2: تهيئة Analytics بأمان =====================
        try {
            firebaseAnalytics = FirebaseAnalytics.getInstance(this)
            Log.d("MyApplication", "✅ FirebaseAnalytics initialized successfully")
        } catch (e: Exception) {
            Log.e("MyApplication", "❌ FirebaseAnalytics initialization failed: ${e.message}")
            e.printStackTrace()
        }

        // ===================== الخطوة 3: تهيئة Crashlytics بأمان (مع تأخير) =====================
        initializeCrashlyticsSafely()

        // ===================== الخطوة 4: تهيئة قنوات الإشعارات الموحدة =====================
        try {
            com.example.utils.NotificationChannels.createAll(this)
            Log.d("MyApplication", "✅ Unified notification channels created successfully")
        } catch (e: Exception) {
            Log.e("MyApplication", "❌ Failed to create notification channels: ${e.message}")
        }

        try {
            com.example.utils.NotificationHelper.createNotificationChannels(this)
            Log.d("MyApplication", "✅ Admin notification channels created successfully")
        } catch (e: Exception) {
            Log.e("MyApplication", "❌ Failed to create admin notification channels: ${e.message}")
        }

        try {
            com.example.utils.ChatNotificationHelper.createNotificationChannels(this)
            Log.d("MyApplication", "✅ Chat notification channels created successfully")
        } catch (e: Exception) {
            Log.e("MyApplication", "❌ Failed to create chat notification channels: ${e.message}")
        }

        // ===================== الخطوة 5: جدولة المزامنة الدورية في الخلفية =====================
        try {
            com.example.sync.BackgroundSyncScheduler(this).schedulePeriodicSync()
            Log.d("MyApplication", "✅ Background periodic sync scheduled successfully")
        } catch (e: Exception) {
            Log.e("MyApplication", "❌ Failed to schedule periodic sync: ${e.message}")
        }
    }

    /**
     * تهيئة Crashlytics بأمان مع:
     * 1. تأخير 3 ثوانٍ لضمان اكتمال تهيئة Firebase
     * 2. try-catch شامل لمنع إغلاق التطبيق
     * 3. حفظ حالة التهيئة لمنع المحاولات المتكررة الفاشلة
     */
    private fun initializeCrashlyticsSafely() {
        // التحقق من عدم التهيئة مسبقاً
        if (isCrashlyticsReady) {
            Log.d("MyApplication", "✅ Crashlytics already initialized")
            return
        }

        // استخدام Handler للتأخير
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                val crashlytics = FirebaseCrashlytics.getInstance()
                crashlytics.setCrashlyticsCollectionEnabled(true)
                isCrashlyticsReady = true
                Log.d("MyApplication", "✅ Firebase Crashlytics initialized successfully")
                
                try {
                    crashlytics.log("Crashlytics initialized successfully")
                } catch (e: Exception) { /* تجاهل */ }
                
            } catch (e: Exception) {
                Log.e("MyApplication", "❌ Firebase Crashlytics initialization failed: ${e.message}")
                e.printStackTrace()
                // محاولة بديلة
                tryAlternativeCrashlyticsInit()
            }
        }, 3000)
    }

    /**
     * محاولة بديلة لتهيئة Crashlytics (كحل أخير)
     */
    private fun tryAlternativeCrashlyticsInit() {
        try {
            val crashlytics = FirebaseCrashlytics.getInstance()
            crashlytics.setCrashlyticsCollectionEnabled(true)
            isCrashlyticsReady = true
            Log.d("MyApplication", "✅ Firebase Crashlytics initialized (alternative method)")
        } catch (e: Exception) {
            Log.e("MyApplication", "❌ Firebase Crashlytics alternative initialization failed: ${e.message}")
            // التطبيق يستمر في العمل بدون Crashlytics
        }
    }

    /**
     * دالة آمنة لتسجيل الاستثناءات في Crashlytics
     */
    fun recordExceptionSafely(throwable: Throwable) {
        if (isCrashlyticsReady) {
            try {
                FirebaseCrashlytics.getInstance().recordException(throwable)
            } catch (e: Exception) {
                Log.e("MyApplication", "Failed to record exception: ${e.message}")
            }
        } else {
            Log.e("MyApplication", "Crashlytics not ready: ${throwable.message}")
        }
    }
}
