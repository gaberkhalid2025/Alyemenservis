package com.example.utils

import java.util.UUID

/**
 * 📦 NavigationModels
 * نماذج البيانات الموحدة لنظام الملاحة.
 */

data class NavWaypoint(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val latitude: Double,
    val longitude: Double,
    var isReached: Boolean = false,
    val type: WaypointType = WaypointType.BREAK
)

enum class WaypointType {
    START,  // نقطة الانطلاق
    BREAK,  // محطة توقف (يجب التوقف عندها)
    THROUGH // نقطة مرور (توجيه فقط بدون توقف)
}

data class NavStep(
    val instruction: String,
    val distanceMeters: Double,
    val durationSeconds: Double,
    val maneuverType: ManeuverType,
    val streetName: String = "",
    val polylineIndex: Int = 0 // فهرس البداية في مصفوفة الإحداثيات
) {
    val distanceText: String get() = if (distanceMeters >= 1000) "%.1f كم".format(distanceMeters / 1000.0) else "${distanceMeters.toInt()} م"
    
    val iconType: String get() = when (maneuverType) {
        ManeuverType.START -> "STRAIGHT"
        ManeuverType.STRAIGHT -> "STRAIGHT"
        ManeuverType.LEFT, ManeuverType.SHARP_LEFT, ManeuverType.SLIGHT_LEFT -> "LEFT"
        ManeuverType.RIGHT, ManeuverType.SHARP_RIGHT, ManeuverType.SLIGHT_RIGHT -> "RIGHT"
        ManeuverType.ROUNDABOUT -> "ROUNDABOUT"
        ManeuverType.UTURN -> "ROUNDABOUT"
        ManeuverType.HIGHWAY_ENTER, ManeuverType.HIGHWAY_EXIT, ManeuverType.MERGE -> "HIGHWAY"
        ManeuverType.DESTINATION -> "DESTINATION"
    }
}

enum class ManeuverType {
    START, STRAIGHT, LEFT, RIGHT, SHARP_LEFT, SHARP_RIGHT, SLIGHT_LEFT, SLIGHT_RIGHT,
    UTURN, ROUNDABOUT, HIGHWAY_ENTER, HIGHWAY_EXIT, DESTINATION, MERGE
}

data class NavLeg(
    val fromWaypointId: String,
    val toWaypointId: String,
    val distanceMeters: Double,
    val durationSeconds: Double,
    val steps: List<NavStep>
)

data class NavRouteResult(
    val totalDistanceMeters: Double,
    val totalDurationSeconds: Double,
    val coordinates: List<Pair<Double, Double>>, // قائمة خطوط الطول والعرض للمسار الكامل
    val legs: List<NavLeg>,
    val waypoints: List<NavWaypoint>,
    val isOffline: Boolean,
    val engineSource: String,
    val error: NavError? = null
) {
    fun getAllSteps(): List<NavStep> = legs.flatMap { it.steps }
    val formattedDistance: String get() = formatDistance(totalDistanceMeters)
    val formattedDuration: String get() = formatDuration(totalDurationSeconds)

    private fun formatDistance(meters: Double): String {
        return if (meters >= 1000) "%.1f كم".format(meters / 1000.0) else "${meters.toInt()} م"
    }

    private fun formatDuration(seconds: Double): String {
        val mins = (seconds / 60).toInt()
        return if (mins >= 60) "${mins / 60} س و ${mins % 60} د" else "$mins دقيقة"
    }
}

sealed class NavError {
    object NoConnection : NavError()
    object NoTiles : NavError()
    object RouteNotFound : NavError()
    data class Unknown(val message: String) : NavError()
}
