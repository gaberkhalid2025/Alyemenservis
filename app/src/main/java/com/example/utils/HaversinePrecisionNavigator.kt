package com.example.utils

import kotlin.math.*

/**
 * 📏 HaversinePrecisionNavigator
 * حسابات المسافات والزوايا والإسقاطات بدقة جيوديسية عالية.
 */
object HaversinePrecisionNavigator {

    private const val EARTH_RADIUS_METERS = 6371008.8 // IUGG Mean Radius

    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_METERS * c
    }

    fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val lon1Rad = Math.toRadians(lon1)
        val lon2Rad = Math.toRadians(lon2)
        val lat1Rad = Math.toRadians(lat1)
        val lat2Rad = Math.toRadians(lat2)

        val y = sin(lon2Rad - lon1Rad) * cos(lat2Rad)
        val x = cos(lat1Rad) * sin(lat2Rad) - sin(lat1Rad) * cos(lat2Rad) * cos(lon2Rad - lon1Rad)
        
        return ((Math.toDegrees(atan2(y, x)) + 360) % 360).toFloat()
    }

    /**
     * حساب المسافة العمودية (Cross-track distance) من نقطة إلى خط مسار.
     * مفيد جداً للكشف عن الخروج عن المسار.
     */
    fun crossTrackDistance(
        pointLat: Double, pointLon: Double,
        startLat: Double, startLon: Double,
        endLat: Double, endLon: Double
    ): Double {
        val d13 = calculateDistance(startLat, startLon, pointLat, pointLon) / EARTH_RADIUS_METERS
        val b13 = Math.toRadians(calculateBearing(startLat, startLon, pointLat, pointLon).toDouble())
        val b12 = Math.toRadians(calculateBearing(startLat, startLon, endLat, endLon).toDouble())
        
        return asin(sin(d13) * sin(b13 - b12)) * EARTH_RADIUS_METERS
    }
}
