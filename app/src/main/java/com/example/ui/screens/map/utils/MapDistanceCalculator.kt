package com.example.ui.screens.map.utils

import kotlin.math.*

/**
 * 🧭 MapDistanceCalculator
 * Precise Haversine distance calculator and ETA estimation (walking: 5 km/h, driving: 35 km/h)
 */
object MapDistanceCalculator {

    private const val EARTH_RADIUS_METERS = 6371000.0

    /**
     * Compute Haversine distance in meters between two lat/lng points
     */
    fun calculateDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        if (lat1 == 0.0 && lon1 == 0.0) return 0.0
        if (lat2 == 0.0 && lon2 == 0.0) return 0.0

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_METERS * c
    }

    /**
     * Format distance in meters to a clean Arabic string ("500 م" or "2.4 كم")
     */
    fun formatDistance(meters: Double): String {
        return if (meters < 1000) {
            "${meters.roundToInt()} م"
        } else {
            String.format(java.util.Locale.US, "%.1f كم", meters / 1000.0)
        }
    }

    /**
     * Compute Estimated Time of Arrival (ETA) text:
     * - Under 1 km: walking speed ~ 5 km/h
     * - 1 km and above: driving speed ~ 35 km/h
     */
    fun computeEta(distanceMeters: Double): String {
        val distKm = distanceMeters / 1000.0
        return if (distKm < 1.0) {
            val minutes = max(1, (distKm / 5.0 * 60).roundToInt())
            "⏱️ ~ $minutes دقيقة سيراً (${distanceMeters.roundToInt()} م)"
        } else {
            val minutes = max(1, (distKm / 35.0 * 60).roundToInt())
            "🚘 ~ $minutes دقيقة بالسيارة (${String.format(java.util.Locale.US, "%.1f", distKm)} كم)"
        }
    }
}
