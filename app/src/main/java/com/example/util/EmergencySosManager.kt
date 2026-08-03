package com.example.util

import android.content.Context
import android.util.Log
import com.example.data.NotificationEntity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class EmergencyAlert(
    val id: String = java.util.UUID.randomUUID().toString(),
    val userId: String = "",
    val userName: String = "",
    val userPhone: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val note: String = "طلب نجدة وطوارئ إلكتروني عاجل",
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "ACTIVE" // ACTIVE, RESOLVED, DISMISSED
)

/**
 * 🆘 EmergencySosManager:
 * Full implementation for SOS emergency button logic, geofence safety alerts, and anti-spam controls.
 */
object EmergencySosManager {
    private const val TAG = "EmergencySosManager"
    private const val MIN_INTERVAL_MS = 30_000L // 30 seconds rate limit between SOS requests
    private var lastSosTimestamp = 0L

    private val _activeAlerts = MutableStateFlow<List<EmergencyAlert>>(emptyList())
    val activeAlerts: StateFlow<List<EmergencyAlert>> = _activeAlerts

    /**
     * Trigger Emergency SOS request with rate-limiting guard
     */
    fun triggerSosAlert(
        context: Context,
        userId: String,
        userName: String,
        userPhone: String,
        lat: Double,
        lng: Double,
        note: String = "طلب نجدة وطوارئ عاجل من مستخدم التطبيق",
        onResult: (Boolean, String) -> Unit
    ) {
        val now = System.currentTimeMillis()
        if (now - lastSosTimestamp < MIN_INTERVAL_MS) {
            val remainingSec = ((MIN_INTERVAL_MS - (now - lastSosTimestamp)) / 1000).toInt()
            onResult(false, "⚠️ يرجى الانتظار $remainingSec ثانية قبل إرسال نداء طوارئ جديد لمنع البلاغات العشوائية.")
            return
        }

        lastSosTimestamp = now

        val alert = EmergencyAlert(
            userId = userId.ifEmpty { "guest_device" },
            userName = userName.ifEmpty { "مستخدم طوارئ" },
            userPhone = userPhone.ifEmpty { "غير محدد" },
            latitude = lat,
            longitude = lng,
            note = note,
            timestamp = now
        )

        try {
            val db = FirebaseFirestore.getInstance()
            db.collection("emergency_sos_alerts")
                .document(alert.id)
                .set(alert)
                .addOnSuccessListener {
                    Log.d(TAG, "SOS alert logged successfully: ${alert.id}")

                    // Send high-priority notification to Admin panel & nearby emergency responders
                    val notif = NotificationEntity(
                        id = java.util.UUID.randomUUID().toString(),
                        title = "🚨 نداء طوارئ SOS عاجل!",
                        message = "تم تسجيل طلب نجدة من $userName ($userPhone). موقع الإحداثيات: $lat, $lng",
                        targetType = "ALL",
                        notificationType = "IMPORTANT",
                        timestamp = now
                    )
                    db.collection("notifications").document(notif.id).set(notif)

                    onResult(true, "✅ تم إرسال نداء الطوارئ بنجاح وإبلاغ فريق العمليات والمشرفين فورياً!")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to send SOS alert", e)
                    onResult(false, "❌ فشل إرسال نداء الطوارئ: ${e.localizedMessage}")
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error in EmergencySosManager", e)
            onResult(false, "❌ حدث خطأ غير متوقع: ${e.localizedMessage}")
        }
    }

    /**
     * Geofencing Safety Alert Check:
     * Evaluates if user location is inside registered unsafe zones or high-risk areas.
     */
    fun checkGeofenceSafetyAlert(
        userLat: Double,
        userLng: Double,
        onSafetyAlert: (String) -> Unit
    ) {
        if (userLat == 0.0 || userLng == 0.0) return

        // Example geofence check for Sana'a / Aden mountain passes or risk points
        val riskZones = listOf(
            Triple(15.3500, 44.2000, "منطقة أعمال صيانة حفر ومشاريع طرق جارية"),
            Triple(12.8000, 45.0300, "تنبيه أمني: ازدحام مروري وتوقف سير بري")
        )

        for (zone in riskZones) {
            val distMeters = calculateDistanceMeters(userLat, userLng, zone.first, zone.second)
            if (distMeters < 1000f) { // Within 1 km radius
                onSafetyAlert("⚠️ تنبيه أمان سياج جغرافي: أنت قريب من ${zone.third}")
                break
            }
        }
    }

    private fun calculateDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        try {
            android.location.Location.distanceBetween(lat1, lon1, lat2, lon2, results)
            return results[0]
        } catch (e: Exception) {
            return 5000f
        }
    }
}
