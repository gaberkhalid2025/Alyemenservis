package com.example.ui.screens.map.utils

import kotlin.math.*
import com.example.utils.HaversinePrecisionNavigator

/**
 * 🚲 TransportMode - وسيلة التنقل لحساب وقت الوصول المتوقع
 */
enum class TransportMode(val speedKmH: Double, val icon: String, val labelArabic: String) {
    WALKING(3.5, "⏱️", "سيراً"),
    BICYCLE(15.0, "🚲", "بالدراجة"),
    CAR(35.0, "🚘", "بالسيارة")
}

/**
 * 🧭 MapDistanceCalculator
 * محول المسافات ومقدر وقت الوصول المتوقع (ETA) باستخدام المحرك الجيوديسي عالي الدقة.
 */
object MapDistanceCalculator {

    /**
     * حساب المسافة بالامتار بين نقطتين (Haversine)
     */
    fun calculateDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        if (lat1 == 0.0 && lon1 == 0.0) return 0.0
        if (lat2 == 0.0 && lon2 == 0.0) return 0.0
        return HaversinePrecisionNavigator.calculateDistance(lat1, lon1, lat2, lon2)
    }

    /**
     * تنسيق المسافة إلى نص عربي (مثلاً "500 م" أو "2.4 كم")
     */
    fun formatDistance(meters: Double): String {
        return if (meters < 1000) {
            "${meters.roundToInt()} م"
        } else {
            String.format(java.util.Locale.US, "%.1f كم", meters / 1000.0)
        }
    }

    /**
     * حساب وقت الوصول المتوقع (ETA)
     */
    fun computeEta(distanceMeters: Double, mode: TransportMode? = null): String {
        val distKm = distanceMeters / 1000.0
        val selectedMode = mode ?: if (distKm < 1.0) TransportMode.WALKING else TransportMode.CAR
        val minutes = max(1, (distKm / selectedMode.speedKmH * 60).roundToInt())
        val distFormatted = if (distKm < 1.0) "${distanceMeters.roundToInt()} م" else "${String.format(java.util.Locale.US, "%.1f", distKm)} كم"
        return "${selectedMode.icon} ~ $minutes دقيقة ${selectedMode.labelArabic} ($distFormatted)"
    }
}
