package com.example.util

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * 📈 AnalyticsManager - المدير الموحد للتحليلات وإصدار التقارير
 * 
 * الميزات:
 * 1. تتبع الأحداث اليومية وإحصائيات الاستخدام عبر Firebase Analytics.
 * 2. تخزين التقارير التحليلية المفصلة والتقارير المجمعة في Firestore ("analytics_reports").
 * 3. تتبع الشاشات وتفضيلات المستخدم وتقديم إحصائيات StateFlow سريعة في التطبيق.
 */
class AnalyticsManager(private val context: Context) {

    private val firebaseAnalytics: FirebaseAnalytics by lazy { FirebaseAnalytics.getInstance(context) }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _eventCount = MutableStateFlow(0)
    val eventCount: StateFlow<Int> = _eventCount.asStateFlow()

    /**
     * تسجيل حدث تفاعلي في Firebase Analytics
     */
    fun trackEvent(eventName: String, params: Map<String, Any?> = emptyMap()) {
        try {
            val bundle = Bundle()
            params.forEach { (key, value) ->
                when (value) {
                    is String -> bundle.putString(key, value)
                    is Int -> bundle.putInt(key, value)
                    is Long -> bundle.putLong(key, value)
                    is Double -> bundle.putDouble(key, value)
                    is Boolean -> bundle.putBoolean(key, value)
                    else -> bundle.putString(key, value?.toString() ?: "")
                }
            }
            firebaseAnalytics.logEvent(eventName, bundle)
            _eventCount.value += 1
        } catch (e: Exception) {
            // صامت
        }
    }

    /**
     * تتبع زيارة شاشة
     */
    fun trackScreenView(screenName: String, screenClass: String = screenName) {
        trackEvent(
            FirebaseAnalytics.Event.SCREEN_VIEW,
            mapOf(
                FirebaseAnalytics.Param.SCREEN_NAME to screenName,
                FirebaseAnalytics.Param.SCREEN_CLASS to screenClass
            )
        )
    }

    /**
     * حفظ تقرير تحليلي مخصص في Firestore
     */
    fun saveReportToCloud(
        reportType: String, // "DAILY_SUMMARY", "REVENUE", "USER_ACTIVITY", "ERROR_LOG"
        data: Map<String, Any?>,
        userId: String = ""
    ) {
        scope.launch {
            try {
                val reportId = "REP_${UUID.randomUUID().toString().take(8)}"
                val reportPayload = hashMapOf<String, Any?>(
                    "id" to reportId,
                    "type" to reportType,
                    "userId" to userId,
                    "timestamp" to System.currentTimeMillis(),
                    "data" to data
                )
                firestore.collection("analytics_reports").document(reportId).set(reportPayload)
            } catch (e: Exception) {
                // صامت
            }
        }
    }

    /**
     * تعيين خاصية لمستخدم
     */
    fun setUserProperty(name: String, value: String) {
        try {
            firebaseAnalytics.setUserProperty(name, value)
        } catch (e: Exception) {
            // صامت
        }
    }
}
