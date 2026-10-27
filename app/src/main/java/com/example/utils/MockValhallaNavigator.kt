package com.example.utils

import android.content.Context
import com.example.ui.screens.map.utils.MapDistanceCalculator
import kotlin.math.*

/**
 * 🧭 MockValhallaNavigator
 * محاكي الملاحة الذكي مع دعم محطات التوقف المتعددة (Multi-Stop Navigation) ومطابقة الطرق.
 */
object MockValhallaNavigator {

    data class Waypoint(
        val name: String,
        val lat: Double,
        val lng: Double,
        val id: String = java.util.UUID.randomUUID().toString()
    )

    data class NavigationWaypoint(
        val id: String = java.util.UUID.randomUUID().toString(),
        val name: String,
        val latitude: Double,
        val longitude: Double,
        var isReached: Boolean = false
    ) {
        fun toWaypoint(): Waypoint = Waypoint(name, latitude, longitude, id)
    }

    data class NavigationStep(
        val instruction: String,
        val distanceText: String,
        val distanceMeters: Double,
        val iconType: String,
        val streetName: String = "",
        val targetWaypointName: String = ""
    )

    data class RouteLeg(
        val fromName: String,
        val toName: String,
        val distanceKm: Double,
        val timeMinutes: Double,
        val instructions: List<NavigationStep>
    )

    sealed class RouteError {
        object EmptyWaypoints : RouteError()
        object InvalidCoordinates : RouteError()
    }

    data class RouteResult(
        val totalDistanceMeters: Double,
        val totalDurationSeconds: Double,
        val steps: List<NavigationStep>,
        val coordinates: List<Pair<Double, Double>> = emptyList(),
        val routeTitle: String = "مسار معبد مباشر",
        val formattedDistance: String = MapDistanceCalculator.formatDistance(totalDistanceMeters),
        val formattedDuration: String = "${(totalDurationSeconds / 60).toInt().coerceAtLeast(1)} دقيقة",
        val totalDistanceKm: Double = totalDistanceMeters / 1000.0,
        val totalTimeMinutes: Double = totalDurationSeconds / 60.0,
        val legs: List<RouteLeg> = emptyList(),
        val waypoints: List<NavigationWaypoint> = emptyList(),
        val error: RouteError? = null
    )

    fun snapToRoad(lat: Double, lng: Double): Pair<Double, Double> {
        if (lat == 0.0 && lng == 0.0) return Pair(15.3694, 44.1910)
        val snappedLat = round(lat * 10000.0) / 10000.0
        val snappedLng = round(lng * 10000.0) / 10000.0
        return Pair(snappedLat, snappedLng)
    }

    // Backward compatibility for single destination
    fun calculateRoute(
        context: Context,
        startLat: Double,
        startLng: Double,
        destLat: Double,
        destLng: Double,
        destName: String
    ): RouteResult {
        val origin = Waypoint("موقعي الحالي", startLat, startLng)
        val destination = Waypoint(destName, destLat, destLng)
        return calculateRoute(context, origin, listOf(destination))
    }

    // Multi-stop calculateRoute receiving origin and list of waypoints
    fun calculateRoute(
        context: Context,
        origin: Waypoint,
        waypoints: List<Waypoint>
    ): RouteResult {
        val allWaypoints = listOf(origin) + waypoints
        if (allWaypoints.size < 2) {
            return RouteResult(
                totalDistanceMeters = 0.0,
                totalDurationSeconds = 0.0,
                steps = listOf(NavigationStep("حدد وجهتك لبدء الملاحة", "0 م", 0.0, "STRAIGHT")),
                coordinates = emptyList(),
                waypoints = allWaypoints.map { NavigationWaypoint(it.id, it.name, it.lat, it.lng) },
                error = RouteError.EmptyWaypoints
            )
        }

        var totalDistance = 0.0
        val coordsList = mutableListOf<Pair<Double, Double>>()
        val stepsList = mutableListOf<NavigationStep>()
        val legsList = mutableListOf<RouteLeg>()

        for (i in 0 until allWaypoints.size - 1) {
            val from = allWaypoints[i]
            val to = allWaypoints[i + 1]

            val (sLat, sLng) = snapToRoad(from.lat, from.lng)
            val (dLat, dLng) = snapToRoad(to.lat, to.lng)

            val directDistance = MapDistanceCalculator.calculateDistanceMeters(sLat, sLng, dLat, dLng)
            val actualRoadDistance = (directDistance * 1.25).coerceAtLeast(350.0)
            totalDistance += actualRoadDistance

            coordsList.add(Pair(sLat, sLng))
            coordsList.add(Pair((sLat + dLat) / 2, (sLng + dLng) / 2))
            coordsList.add(Pair(dLat, dLng))

            val legSteps = mutableListOf<NavigationStep>()
            if (actualRoadDistance < 1000) {
                val step1 = NavigationStep("اتجه مباشرة في الشارع الرئيسي نحو ${to.name}", MapDistanceCalculator.formatDistance(actualRoadDistance * 0.7), actualRoadDistance * 0.7, "STRAIGHT", targetWaypointName = to.name)
                val step2 = NavigationStep("انعطف يميناً نحو ${to.name}", MapDistanceCalculator.formatDistance(actualRoadDistance * 0.3), actualRoadDistance * 0.3, "RIGHT", targetWaypointName = to.name)
                val step3 = NavigationStep("لقد وصلت إلى وجهتك: ${to.name}", "0 م", 0.0, "DESTINATION", targetWaypointName = to.name)
                legSteps.addAll(listOf(step1, step2, step3))
                stepsList.addAll(listOf(step1, step2, step3))
            } else {
                val step1 = NavigationStep("انطلق على المسار الرئيسي نحو ${to.name}", "400 م", 400.0, "STRAIGHT", targetWaypointName = to.name)
                val step2 = NavigationStep("ادخل الجولة واستمر في المخرج الثاني", "800 م", 800.0, "ROUNDABOUT", targetWaypointName = to.name)
                val step3 = NavigationStep("لقد وصلت إلى وجهتك: ${to.name}", "0 م", 0.0, "DESTINATION", targetWaypointName = to.name)
                legSteps.addAll(listOf(step1, step2, step3))
                stepsList.addAll(listOf(step1, step2, step3))
            }

            legsList.add(
                RouteLeg(
                    fromName = from.name,
                    toName = to.name,
                    distanceKm = actualRoadDistance / 1000.0,
                    timeMinutes = (actualRoadDistance / 1000.0) / 35.0 * 60.0,
                    instructions = legSteps
                )
            )
        }

        val averageSpeedMps = 35.0 * 1000.0 / 3600.0
        val durationSeconds = totalDistance / averageSpeedMps
        val mins = (durationSeconds / 60).toInt().coerceAtLeast(1)
        val formattedDur = if (mins >= 60) "${mins / 60} س و ${mins % 60} د" else "$mins دقيقة"

        return RouteResult(
            totalDistanceMeters = totalDistance,
            totalDurationSeconds = durationSeconds,
            steps = stepsList,
            coordinates = coordsList,
            routeTitle = "مسار معبد متعدد المحطات",
            formattedDistance = MapDistanceCalculator.formatDistance(totalDistance),
            formattedDuration = formattedDur,
            totalDistanceKm = totalDistance / 1000.0,
            totalTimeMinutes = durationSeconds / 60.0,
            legs = legsList,
            waypoints = allWaypoints.map { NavigationWaypoint(it.id, it.name, it.lat, it.lng) }
        )
    }

    // Overload for NavigationWaypoint list
    fun calculateRoute(
        context: Context,
        waypoints: List<NavigationWaypoint>
    ): RouteResult {
        if (waypoints.isEmpty()) {
            return RouteResult(
                totalDistanceMeters = 0.0,
                totalDurationSeconds = 0.0,
                steps = listOf(NavigationStep("حدد وجهتك لبدء الملاحة", "0 م", 0.0, "STRAIGHT")),
                coordinates = emptyList(),
                waypoints = waypoints,
                error = RouteError.EmptyWaypoints
            )
        }
        val origin = waypoints.first().toWaypoint()
        val rest = waypoints.drop(1).map { it.toWaypoint() }
        return calculateRoute(context, origin, rest)
    }
}
