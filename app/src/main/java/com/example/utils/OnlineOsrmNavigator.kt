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
 * [إصلاح ملاحة نهائي] محرك الملاحة عبر الإنترنت باستخدام OSRM
 * - معالجة كاملة للاستثناءات والمهل الزمنية
 * - إغلاق الاتصالات تلقائياً في finally
 * - دعم نقاط توقف متعددة وإرشادات عربية دقيقة
 */
object OnlineOsrmNavigator {

    private const val TAG = "OnlineOsrmNavigator"

    suspend fun calculateRoute(
        origin: Pair<Double, Double>,
        waypoints: List<Pair<Double, Double>>
    ): NavRouteResult {
        val originWp = NavWaypoint(name = "موقعي", latitude = origin.first, longitude = origin.second, type = WaypointType.START)
        val listWps = waypoints.mapIndexed { idx, pt ->
            NavWaypoint(name = "نقطة ${idx + 1}", latitude = pt.first, longitude = pt.second, type = WaypointType.BREAK)
        }
        return calculateRoute(originWp, listWps)
    }

    suspend fun calculateRoute(
        origin: NavWaypoint,
        waypoints: List<NavWaypoint>
    ): NavRouteResult = withContext(Dispatchers.IO) {
        val validWaypoints = waypoints.filter { !it.latitude.isNaN() && !it.longitude.isNaN() && it.latitude != 0.0 && it.longitude != 0.0 }
        val allWaypoints = listOf(origin) + validWaypoints

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

        var connection: HttpURLConnection? = null
        try {
            // [إصلاح ملاحة نهائي] OSRM يتطلب الترتيب lon,lat
            val coordsParam = allWaypoints.joinToString(";") { "${it.longitude},${it.latitude}" }
            val urlString = "${NavConfig.OSRM_BASE_URL}$coordsParam?overview=full&geometries=geojson&steps=true&alternatives=false"
            val url = URL(urlString)

            connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 12000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "AlYemenServicesApp/1.0 (Android; OpenStreetMap)")
                instanceFollowRedirects = true
            }

            val responseCode = connection.responseCode
            if (responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)

                if (json.optString("code") == "Ok") {
                    val routes = json.optJSONArray("routes")
                    if (routes != null && routes.length() > 0) {
                        val route = routes.getJSONObject(0)
                        val totalDistance = route.optDouble("distance", 0.0)
                        val totalDuration = route.optDouble("duration", 0.0)

                        // [إصلاح ملاحة نهائي] استخراج خط المسار (Polyline Coords) بصيغة [Lat, Lng] للخرائط
                        val coordsList = mutableListOf<Pair<Double, Double>>()
                        val geometry = route.optJSONObject("geometry")
                        if (geometry != null) {
                            val coordsArr = geometry.optJSONArray("coordinates")
                            if (coordsArr != null) {
                                for (i in 0 until coordsArr.length()) {
                                    val pt = coordsArr.optJSONArray(i)
                                    if (pt != null && pt.length() >= 2) {
                                        // OSRM returns [lon, lat], we convert to Pair(lat, lon)
                                        coordsList.add(Pair(pt.getDouble(1), pt.getDouble(0)))
                                    }
                                }
                            }
                        }

                        val legsList = mutableListOf<NavLeg>()
                        val legs = route.optJSONArray("legs")
                        if (legs != null) {
                            for (legIdx in 0 until legs.length()) {
                                val leg = legs.optJSONObject(legIdx) ?: continue
                                val fromWp = allWaypoints.getOrNull(legIdx) ?: allWaypoints.first()
                                val toWp = allWaypoints.getOrNull(legIdx + 1) ?: allWaypoints.last()
                                val legDist = leg.optDouble("distance", 0.0)
                                val legDur = leg.optDouble("duration", 0.0)

                                val legSteps = mutableListOf<NavStep>()
                                val steps = leg.optJSONArray("steps")
                                if (steps != null) {
                                    for (stepIdx in 0 until steps.length()) {
                                        val stepObj = steps.optJSONObject(stepIdx) ?: continue
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

                                        legSteps.add(
                                            NavStep(
                                                instruction = instruction,
                                                distanceMeters = stepDist,
                                                durationSeconds = stepDur,
                                                maneuverType = mType,
                                                streetName = streetName
                                            )
                                        )
                                    }
                                }

                                legsList.add(
                                    NavLeg(
                                        fromWaypointId = fromWp.id,
                                        toWaypointId = toWp.id,
                                        distanceMeters = legDist,
                                        durationSeconds = legDur,
                                        steps = legSteps
                                    )
                                )
                            }
                        }

                        return@withContext NavRouteResult(
                            totalDistanceMeters = totalDistance,
                            totalDurationSeconds = totalDuration,
                            coordinates = coordsList,
                            legs = legsList,
                            waypoints = allWaypoints,
                            isOffline = false,
                            engineSource = "OSRM 🌐 (أونلاين حي)"
                        )
                    }
                }
            } else {
                Log.w(TAG, "OSRM server returned HTTP $responseCode")
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
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Throwable) {
            Log.e(TAG, "OSRM calculateRoute exception: ${e.message}", e)
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
        } finally {
            try {
                connection?.disconnect()
            } catch (_: Exception) {}
        }
    }

    private fun mapManeuver(type: String, modifier: String): ManeuverType {
        fun byModifier(): ManeuverType = when (modifier) {
            "left" -> ManeuverType.LEFT
            "right" -> ManeuverType.RIGHT
            "sharp left" -> ManeuverType.SHARP_LEFT
            "sharp right" -> ManeuverType.SHARP_RIGHT
            "slight left" -> ManeuverType.SLIGHT_LEFT
            "slight right" -> ManeuverType.SLIGHT_RIGHT
            "uturn" -> ManeuverType.UTURN
            else -> ManeuverType.STRAIGHT
        }
        return when (type) {
            "depart" -> ManeuverType.START
            "arrive" -> ManeuverType.DESTINATION
            "turn", "end of road", "fork", "continue" -> byModifier()
            "new name" -> ManeuverType.STRAIGHT
            "roundabout", "rotary", "roundabout turn", "exit roundabout", "exit rotary" -> ManeuverType.ROUNDABOUT
            "on ramp", "merge" -> ManeuverType.HIGHWAY_ENTER
            "off ramp" -> ManeuverType.HIGHWAY_EXIT
            else -> byModifier()
        }
    }
}
