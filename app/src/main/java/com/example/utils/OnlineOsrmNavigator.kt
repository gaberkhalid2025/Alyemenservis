package com.example.utils

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * 🌐 OnlineOsrmNavigator
 * محرك الملاحة عبر الإنترنت باستخدام OSRM مع مهام أمان وخووارزميات وقت المهلة (Timeouts).
 */
object OnlineOsrmNavigator {

    private const val TAG = "OnlineOsrmNavigator"

    suspend fun calculateRoute(
        origin: NavWaypoint,
        waypoints: List<NavWaypoint>
    ): NavRouteResult = withContext(Dispatchers.IO) {
        val allWaypoints = listOf(origin) + waypoints
        if (allWaypoints.size < 2) {
            return@withContext NavRouteResult(
                totalDistanceMeters = 0.0,
                totalDurationSeconds = 0.0,
                coordinates = emptyList(),
                legs = emptyList(),
                waypoints = allWaypoints,
                isOffline = false,
                engineSource = "OSRM 🌐",
                error = NavError.Unknown("Waypoints size < 2")
            )
        }

        try {
            val coordsParam = allWaypoints.joinToString(";") { "${it.longitude},${it.latitude}" }
            val urlString = "${NavConfig.OSRM_BASE_URL}$coordsParam?overview=full&geometries=geojson&steps=true"
            val url = URL(urlString)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 15000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "AlYemenServicesApp/1.0")
            }

            if (connection.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.readText()
                reader.close()
                connection.disconnect()

                val json = JSONObject(response)
                if (json.optString("code") == "Ok") {
                    val routes = json.getJSONArray("routes")
                    if (routes.length() > 0) {
                        val route = routes.getJSONObject(0)
                        val totalDistance = route.optDouble("distance", 0.0)
                        val totalDuration = route.optDouble("duration", 0.0)

                        val coordsList = mutableListOf<Pair<Double, Double>>()
                        val geometry = route.optJSONObject("geometry")
                        if (geometry != null) {
                            val coordsArr = geometry.optJSONArray("coordinates")
                            if (coordsArr != null) {
                                for (i in 0 until coordsArr.length()) {
                                    val pt = coordsArr.getJSONArray(i)
                                    coordsList.add(Pair(pt.getDouble(1), pt.getDouble(0)))
                                }
                            }
                        }

                        val legsList = mutableListOf<NavLeg>()
                        val legs = route.optJSONArray("legs")
                        
                        if (legs != null) {
                            for (legIdx in 0 until legs.length()) {
                                val leg = legs.getJSONObject(legIdx)
                                val fromWp = allWaypoints[legIdx]
                                val toWp = allWaypoints[legIdx + 1]
                                val legDist = leg.optDouble("distance", 0.0)
                                val legDur = leg.optDouble("duration", 0.0)

                                val legSteps = mutableListOf<NavStep>()
                                val steps = leg.optJSONArray("steps")
                                if (steps != null) {
                                    for (stepIdx in 0 until steps.length()) {
                                        val stepObj = steps.getJSONObject(stepIdx)
                                        val stepDist = stepObj.optDouble("distance", 0.0)
                                        val stepDur = stepObj.optDouble("duration", 0.0)
                                        val streetName = stepObj.optString("name", "").trim()
                                        val maneuver = stepObj.optJSONObject("maneuver")
                                        val type = maneuver?.optString("type", "") ?: ""
                                        val modifier = maneuver?.optString("modifier", "") ?: ""

                                        val mType = mapManeuver(type, modifier)
                                        val instruction = ArabicInstructionFormatter.format(
                                            mType, streetName, stepDist, toWp.name
                                        )

                                        legSteps.add(NavStep(
                                            instruction = instruction,
                                            distanceMeters = stepDist,
                                            durationSeconds = stepDur,
                                            maneuverType = mType,
                                            streetName = streetName
                                        ))
                                    }
                                }
                                legsList.add(NavLeg(
                                    fromWaypointId = fromWp.id,
                                    toWaypointId = toWp.id,
                                    distanceMeters = legDist,
                                    durationSeconds = legDur,
                                    steps = legSteps
                                ))
                            }
                        }

                        return@withContext NavRouteResult(
                            totalDistanceMeters = totalDistance,
                            totalDurationSeconds = totalDuration,
                            coordinates = coordsList,
                            legs = legsList,
                            waypoints = allWaypoints,
                            isOffline = false,
                            engineSource = "OSRM 🌐"
                        )
                    }
                }
            }
            return@withContext NavRouteResult(
                totalDistanceMeters = 0.0,
                totalDurationSeconds = 0.0,
                coordinates = emptyList(),
                legs = emptyList(),
                waypoints = allWaypoints,
                isOffline = false,
                engineSource = "OSRM 🌐",
                error = NavError.RouteNotFound
            )
        } catch (e: Exception) {
            Log.e(TAG, "OSRM exception: ${e.message}", e)
            return@withContext NavRouteResult(
                totalDistanceMeters = 0.0,
                totalDurationSeconds = 0.0,
                coordinates = emptyList(),
                legs = emptyList(),
                waypoints = allWaypoints,
                isOffline = false,
                engineSource = "OSRM 🌐",
                error = NavError.NoConnection
            )
        }
    }

    private fun mapManeuver(type: String, modifier: String): ManeuverType {
        return when (type) {
            "depart" -> ManeuverType.START
            "turn" -> when (modifier) {
                "left" -> ManeuverType.LEFT
                "right" -> ManeuverType.RIGHT
                "sharp left" -> ManeuverType.SHARP_LEFT
                "sharp right" -> ManeuverType.SHARP_RIGHT
                "slight left" -> ManeuverType.SLIGHT_LEFT
                "slight right" -> ManeuverType.SLIGHT_RIGHT
                "uturn" -> ManeuverType.UTURN
                else -> ManeuverType.STRAIGHT
            }
            "continue", "new name" -> ManeuverType.STRAIGHT
            "roundabout", "rotary" -> ManeuverType.ROUNDABOUT
            "on ramp", "merge" -> ManeuverType.HIGHWAY_ENTER
            "off ramp" -> ManeuverType.HIGHWAY_EXIT
            "arrive" -> ManeuverType.DESTINATION
            else -> ManeuverType.STRAIGHT
        }
    }
}
