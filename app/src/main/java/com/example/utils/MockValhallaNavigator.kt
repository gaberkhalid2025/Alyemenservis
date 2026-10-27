package com.example.utils

import android.content.Context
import kotlin.math.*

/**
 * 🧭 MockValhallaNavigator
 * محاكي الملاحة الذكي مع دعم محطات التوقف المتعددة (Multi-Stop Navigation) ومطابقة الطرق.
 */
object MockValhallaNavigator {

    fun snapToRoad(lat: Double, lng: Double): Pair<Double, Double> {
        if (lat == 0.0 && lng == 0.0) return Pair(15.3694, 44.1910)
        val snappedLat = round(lat * 10000.0) / 10000.0
        val snappedLng = round(lng * 10000.0) / 10000.0
        return Pair(snappedLat, snappedLng)
    }

    /**
     * حساب المسار (توافقية الوجهة الواحدة)
     */
    fun calculateRoute(
        context: Context,
        startLat: Double,
        startLng: Double,
        destLat: Double,
        destLng: Double,
        destName: String
    ): NavRouteResult {
        val origin = NavWaypoint(name = "موقعي الحالي", latitude = startLat, longitude = startLng, type = WaypointType.START)
        val destination = NavWaypoint(name = destName, latitude = destLat, longitude = destLng, type = WaypointType.BREAK)
        return calculateRoute(context, origin, listOf(destination))
    }

    /**
     * حساب المسارات المتعددة (محاكي)
     */
    fun calculateRoute(
        context: Context,
        origin: NavWaypoint,
        waypoints: List<NavWaypoint>
    ): NavRouteResult {
        val allWaypoints = listOf(origin) + waypoints
        if (allWaypoints.size < 2) {
            return NavRouteResult(
                totalDistanceMeters = 0.0,
                totalDurationSeconds = 0.0,
                coordinates = emptyList(),
                legs = emptyList(),
                waypoints = allWaypoints,
                isOffline = true,
                engineSource = "المحاكي",
                error = NavError.Unknown("Waypoints size < 2")
            )
        }

        var totalDistance = 0.0
        var totalDuration = 0.0
        val coordsList = mutableListOf<Pair<Double, Double>>()
        val legsList = mutableListOf<NavLeg>()

        for (i in 0 until allWaypoints.size - 1) {
            val from = allWaypoints[i]
            val to = allWaypoints[i + 1]

            val (sLat, sLng) = snapToRoad(from.latitude, from.longitude)
            val (dLat, dLng) = snapToRoad(to.latitude, to.longitude)

            val directDistance = HaversinePrecisionNavigator.calculateDistance(sLat, sLng, dLat, dLng)
            val roadDist = (directDistance * 1.25).coerceAtLeast(350.0)
            totalDistance += roadDist

            coordsList.add(Pair(sLat, sLng))
            coordsList.add(Pair((sLat + dLat) / 2, (sLng + dLng) / 2))
            coordsList.add(Pair(dLat, dLng))

            val legDuration = (roadDist / (35.0 * 1000.0 / 3600.0))
            totalDuration += legDuration

            val legSteps = mutableListOf<NavStep>()
            val startManeuver = if (i == 0) ManeuverType.START else ManeuverType.STRAIGHT
            legSteps.add(NavStep(
                instruction = ArabicInstructionFormatter.format(startManeuver, null, roadDist * 0.7, to.name),
                distanceMeters = roadDist * 0.7,
                durationSeconds = legDuration * 0.7,
                maneuverType = startManeuver
            ))

            val arrivalManeuver = if (i == allWaypoints.size - 2) ManeuverType.DESTINATION else ManeuverType.STRAIGHT
            legSteps.add(NavStep(
                instruction = ArabicInstructionFormatter.format(arrivalManeuver, null, 0.0, to.name),
                distanceMeters = 0.0,
                durationSeconds = 0.0,
                maneuverType = arrivalManeuver
            ))

            legsList.add(NavLeg(
                fromWaypointId = from.id,
                toWaypointId = to.id,
                distanceMeters = roadDist,
                durationSeconds = legDuration,
                steps = legSteps
            ))
        }

        return NavRouteResult(
            totalDistanceMeters = totalDistance,
            totalDurationSeconds = totalDuration,
            coordinates = coordsList,
            legs = legsList,
            waypoints = allWaypoints,
            isOffline = true,
            engineSource = "محاكي الطرق اليمني 🇾🇪"
        )
    }

    /**
     * توافقية مع قائمة نقاط التنقل
     */
    fun calculateRoute(
        context: Context,
        waypoints: List<NavWaypoint>
    ): NavRouteResult {
        if (waypoints.isEmpty()) {
            return NavRouteResult(
                totalDistanceMeters = 0.0,
                totalDurationSeconds = 0.0,
                coordinates = emptyList(),
                legs = emptyList(),
                waypoints = waypoints,
                isOffline = true,
                engineSource = "المحاكي",
                error = NavError.Unknown("Empty waypoints")
            )
        }
        val origin = waypoints.first()
        val rest = waypoints.drop(1)
        return calculateRoute(context, origin, rest)
    }
}
