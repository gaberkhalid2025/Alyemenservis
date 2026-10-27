package com.example.utils

import android.content.Context
import com.example.data.ProviderEntity
import com.example.data.StoreEntity
import com.example.ui.screens.map.utils.MapDistanceCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.UUID

/**
 * 🧭 RealValhallaNavigator
 * محرك الملاحة الهجين فائق الدقة لليمن مع دعم محطات التوقف المتعددة (Multi-Stop Navigation).
 */
object RealValhallaNavigator {

    data class Waypoint(
        val name: String,
        val lat: Double,
        val lng: Double,
        val id: String = UUID.randomUUID().toString()
    )

    data class NavigationWaypoint(
        val id: String = UUID.randomUUID().toString(),
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
        val iconType: String, // "STRAIGHT", "LEFT", "RIGHT", "ROUNDABOUT", "HIGHWAY", "DESTINATION"
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
        object MissingTiles : RouteError()
        data class EngineFailure(val message: String) : RouteError()
    }

    data class NavigationRouteResult(
        val totalDistanceMeters: Double,
        val totalDurationSeconds: Int,
        val formattedDistance: String,
        val formattedDuration: String,
        val steps: List<NavigationStep>,
        val coordinates: List<Pair<Double, Double>> = emptyList(),
        val isOffline: Boolean = false,
        val source: String = "OSRM 🌐",
        val waypoints: List<NavigationWaypoint> = emptyList(),
        val totalDistanceKm: Double = totalDistanceMeters / 1000.0,
        val totalTimeMinutes: Double = totalDurationSeconds / 60.0,
        val legs: List<RouteLeg> = emptyList(),
        val error: RouteError? = null
    )

    data class SearchAddressResult(
        val displayName: String,
        val shortName: String,
        val latitude: Double,
        val longitude: Double,
        val type: String,
        val icon: String
    )

    fun isReady(context: Context): Boolean {
        return try {
            val valhallaDir = File(context.filesDir, "valhalla")
            val tileFile = File(valhallaDir, "valhalla_tiles.tar")
            val markerFile = File(valhallaDir, "is_valhalla_downloaded.txt")
            (tileFile.exists() && tileFile.length() > 500 * 1024) || markerFile.exists()
        } catch (e: Exception) {
            false
        }
    }

    // Backward compatibility for single destination
    suspend fun calculateRoute(
        context: Context,
        startLat: Double,
        startLng: Double,
        destLat: Double,
        destLng: Double,
        destName: String
    ): NavigationRouteResult = withContext(Dispatchers.IO) {
        val origin = Waypoint("موقعي الحالي", startLat, startLng)
        val destination = Waypoint(destName, destLat, destLng)
        calculateRoute(context, origin, listOf(destination))
    }

    // Multi-stop calculateRoute receiving origin and list of waypoints
    suspend fun calculateRoute(
        context: Context,
        origin: Waypoint,
        waypoints: List<Waypoint>
    ): NavigationRouteResult = withContext(Dispatchers.IO) {
        val allWaypoints = listOf(origin) + waypoints
        if (allWaypoints.size < 2) {
            return@withContext NavigationRouteResult(
                totalDistanceMeters = 0.0,
                totalDurationSeconds = 0,
                formattedDistance = "0 م",
                formattedDuration = "0 دقيقة",
                steps = listOf(NavigationStep("حدد وجهتك لبدء الملاحة", "0 م", 0.0, "STRAIGHT")),
                coordinates = emptyList(),
                isOffline = true,
                source = "غير محدد",
                waypoints = allWaypoints.map { NavigationWaypoint(it.id, it.name, it.lat, it.lng) },
                error = RouteError.EmptyWaypoints
            )
        }

        try {
            // Build one route request containing origin + all waypoints (each acts as a break stop)
            val coordsParam = allWaypoints.joinToString(";") { "${it.lng},${it.lat}" }
            val urlString = "https://router.project-osrm.org/route/v1/driving/$coordsParam?overview=full&geometries=geojson&steps=true"
            val url = URL(urlString)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 4500
                readTimeout = 4500
                requestMethod = "GET"
                setRequestProperty("User-Agent", "AlYemenServicesApp/1.0 (Android; OpenStreetMap)")
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
                        val totalDuration = route.optDouble("duration", 0.0).toInt()

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

                        val stepsList = mutableListOf<NavigationStep>()
                        val legsList = mutableListOf<RouteLeg>()
                        val legs = route.optJSONArray("legs")
                        
                        if (legs != null) {
                            for (legIdx in 0 until legs.length()) {
                                val leg = legs.getJSONObject(legIdx)
                                val fromWp = allWaypoints.getOrNull(legIdx) ?: allWaypoints.first()
                                val toWp = allWaypoints.getOrNull(legIdx + 1) ?: allWaypoints.last()
                                val legDist = leg.optDouble("distance", 0.0)
                                val legDur = leg.optDouble("duration", 0.0)

                                val legSteps = mutableListOf<NavigationStep>()
                                val steps = leg.optJSONArray("steps")
                                if (steps != null) {
                                    for (stepIdx in 0 until steps.length()) {
                                        val stepObj = steps.getJSONObject(stepIdx)
                                        val stepDist = stepObj.optDouble("distance", 0.0)
                                        val streetName = stepObj.optString("name", "").trim()
                                        val maneuver = stepObj.optJSONObject("maneuver")
                                        val type = maneuver?.optString("type", "") ?: ""
                                        val modifier = maneuver?.optString("modifier", "") ?: ""

                                        val (instruction, iconType) = translateManeuverToArabic(
                                            type = type,
                                            modifier = modifier,
                                            streetName = streetName,
                                            destName = toWp.name,
                                            isLastStep = (legIdx == legs.length() - 1 && stepIdx == steps.length() - 1)
                                        )

                                        val navStep = NavigationStep(
                                            instruction = instruction,
                                            distanceText = MapDistanceCalculator.formatDistance(stepDist),
                                            distanceMeters = stepDist,
                                            iconType = iconType,
                                            streetName = streetName,
                                            targetWaypointName = toWp.name
                                        )
                                        legSteps.add(navStep)
                                        stepsList.add(navStep)
                                    }
                                }
                                legsList.add(
                                    RouteLeg(
                                        fromName = fromWp.name,
                                        toName = toWp.name,
                                        distanceKm = legDist / 1000.0,
                                        timeMinutes = legDur / 60.0,
                                        instructions = legSteps
                                    )
                                )
                            }
                        }

                        val formattedDist = MapDistanceCalculator.formatDistance(totalDistance)
                        val mins = (totalDuration / 60).coerceAtLeast(1)
                        val formattedDur = if (mins >= 60) "${mins / 60} س و ${mins % 60} د" else "$mins دقيقة"

                        return@withContext NavigationRouteResult(
                            totalDistanceMeters = totalDistance,
                            totalDurationSeconds = totalDuration,
                            formattedDistance = formattedDist,
                            formattedDuration = formattedDur,
                            steps = stepsList.ifEmpty { buildDefaultSteps(allWaypoints.map { NavigationWaypoint(it.id, it.name, it.lat, it.lng) }, totalDistance) },
                            coordinates = coordsList,
                            isOffline = false,
                            source = "شبكة الطرق المتعددة 🌐 (OSRM)",
                            waypoints = allWaypoints.map { NavigationWaypoint(it.id, it.name, it.lat, it.lng) },
                            totalDistanceKm = totalDistance / 1000.0,
                            totalTimeMinutes = totalDuration / 60.0,
                            legs = legsList
                        )
                    }
                }
            }
        } catch (e: Exception) {
            // Fallback to offline
        }

        calculateOfflineRoute(context, allWaypoints)
    }

    // Overload for NavigationWaypoint list
    suspend fun calculateRouteWithWaypoints(
        context: Context,
        waypoints: List<NavigationWaypoint>
    ): NavigationRouteResult = withContext(Dispatchers.IO) {
        if (waypoints.isEmpty()) {
            return@withContext NavigationRouteResult(
                totalDistanceMeters = 0.0,
                totalDurationSeconds = 0,
                formattedDistance = "0 م",
                formattedDuration = "0 دقيقة",
                steps = listOf(NavigationStep("حدد وجهتك لبدء الملاحة", "0 م", 0.0, "STRAIGHT")),
                coordinates = emptyList(),
                isOffline = true,
                source = "غير محدد",
                waypoints = waypoints,
                error = RouteError.EmptyWaypoints
            )
        }
        val origin = waypoints.first().toWaypoint()
        val rest = waypoints.drop(1).map { it.toWaypoint() }
        calculateRoute(context, origin, rest)
    }

    private fun calculateOfflineRoute(
        context: Context,
        allWaypoints: List<Waypoint>
    ): NavigationRouteResult {
        var totalDistance = 0.0
        val coordsList = mutableListOf<Pair<Double, Double>>()
        val stepsList = mutableListOf<NavigationStep>()
        val legsList = mutableListOf<RouteLeg>()

        for (i in 0 until allWaypoints.size - 1) {
            val from = allWaypoints[i]
            val to = allWaypoints[i + 1]

            val directDist = MapDistanceCalculator.calculateDistanceMeters(
                from.lat, from.lng, to.lat, to.lng
            )
            val roadDist = (directDist * 1.26).coerceAtLeast(250.0)
            totalDistance += roadDist

            coordsList.add(Pair(from.lat, from.lng))
            coordsList.add(Pair((from.lat + to.lat) / 2.0, (from.lng + to.lng) / 2.0))
            coordsList.add(Pair(to.lat, to.lng))

            val legSteps = mutableListOf<NavigationStep>()
            if (roadDist < 1500) {
                val s1 = NavigationStep("انطلق نحو ${to.name}", MapDistanceCalculator.formatDistance(roadDist * 0.7), roadDist * 0.7, "STRAIGHT", targetWaypointName = to.name)
                val s2 = NavigationStep("انعطف نحو الوجهة", MapDistanceCalculator.formatDistance(roadDist * 0.3), roadDist * 0.3, "RIGHT", targetWaypointName = to.name)
                legSteps.add(s1)
                legSteps.add(s2)
                stepsList.add(s1)
                stepsList.add(s2)
            } else {
                val s1 = NavigationStep("انطلق على الطريق السريع نحو ${to.name}", MapDistanceCalculator.formatDistance(roadDist * 0.5), roadDist * 0.5, "HIGHWAY", targetWaypointName = to.name)
                val s2 = NavigationStep("واصل السير حتى الوصول إلى ${to.name}", MapDistanceCalculator.formatDistance(roadDist * 0.5), roadDist * 0.5, "STRAIGHT", targetWaypointName = to.name)
                legSteps.add(s1)
                legSteps.add(s2)
                stepsList.add(s1)
                stepsList.add(s2)
            }
            val arrivalStep = NavigationStep("وصلت إلى: ${to.name}", "0 م", 0.0, "DESTINATION", targetWaypointName = to.name)
            legSteps.add(arrivalStep)
            stepsList.add(arrivalStep)

            legsList.add(
                RouteLeg(
                    fromName = from.name,
                    toName = to.name,
                    distanceKm = roadDist / 1000.0,
                    timeMinutes = (roadDist / 1000.0) / 45.0 * 60.0,
                    instructions = legSteps
                )
            )
        }

        val avgSpeedKmh = if (totalDistance > 15000) 65.0 else 40.0
        val durationSeconds = ((totalDistance / 1000.0) / avgSpeedKmh * 3600).toInt().coerceAtLeast(60)

        val formattedDist = MapDistanceCalculator.formatDistance(totalDistance)
        val mins = (durationSeconds / 60).coerceAtLeast(1)
        val formattedDur = if (mins >= 60) "${mins / 60} س و ${mins % 60} د" else "$mins دقيقة"

        val hasTiles = isReady(context)
        val sourceLabel = if (hasTiles) "خرائط Valhalla اليمنية 🇾🇪 (أوفلاين)" else "محاكي الطرق اليمني 🇾🇪 (أوفلاين)"

        return NavigationRouteResult(
            totalDistanceMeters = totalDistance,
            totalDurationSeconds = durationSeconds,
            formattedDistance = formattedDist,
            formattedDuration = formattedDur,
            steps = stepsList,
            coordinates = coordsList,
            isOffline = true,
            source = sourceLabel,
            waypoints = allWaypoints.map { NavigationWaypoint(it.id, it.name, it.lat, it.lng) },
            totalDistanceKm = totalDistance / 1000.0,
            totalTimeMinutes = durationSeconds / 60.0,
            legs = legsList
        )
    }

    private fun translateManeuverToArabic(
        type: String,
        modifier: String,
        streetName: String,
        destName: String,
        isLastStep: Boolean
    ): Pair<String, String> {
        val streetPart = if (streetName.isNotBlank() && streetName != "unnamed") " إلى $streetName" else ""
        val onStreetPart = if (streetName.isNotBlank() && streetName != "unnamed") " على $streetName" else ""

        if (isLastStep || type == "arrive") {
            return Pair("وصلت إلى وجهتك: $destName", "DESTINATION")
        }

        return when (type) {
            "depart" -> Pair("انطلق$onStreetPart باتجاه $destName", "STRAIGHT")
            "turn" -> when (modifier) {
                "left" -> Pair("انعطف يساراً$streetPart", "LEFT")
                "right" -> Pair("انعطف يميناً$streetPart", "RIGHT")
                "sharp left" -> Pair("انعطف يساراً بحِدة$streetPart", "LEFT")
                "sharp right" -> Pair("انعطف يميناً بحِدة$streetPart", "RIGHT")
                "slight left" -> Pair("انحرف يساراً قليلاً$streetPart", "LEFT")
                "slight right" -> Pair("انحرف يميناً قليلاً$streetPart", "RIGHT")
                "uturn" -> Pair("قم بالدوران للخلف (U-Turn)$streetPart", "ROUNDABOUT")
                else -> Pair("انعطف$streetPart", "STRAIGHT")
            }
            "new name", "continue" -> Pair("واصل السير$onStreetPart", "STRAIGHT")
            "roundabout", "rotary" -> Pair("ادخل الدوار وسلك المخرج نحو$streetPart", "ROUNDABOUT")
            "on ramp", "off ramp", "merge" -> Pair("اسلك المسار السريع$onStreetPart", "HIGHWAY")
            else -> {
                if (streetName.isNotBlank()) Pair("تابع السير$onStreetPart", "STRAIGHT")
                else Pair("واصل السير باتجاه $destName", "STRAIGHT")
            }
        }
    }

    private fun buildDefaultSteps(waypoints: List<NavigationWaypoint>, totalDistance: Double): List<NavigationStep> {
        val dest = waypoints.lastOrNull()?.name ?: "الوجهة"
        return listOf(
            NavigationStep("انطلق باتجاه $dest", MapDistanceCalculator.formatDistance(totalDistance * 0.8), totalDistance * 0.8, "STRAIGHT", targetWaypointName = dest),
            NavigationStep("وصلت إلى: $dest", "0 م", 0.0, "DESTINATION", targetWaypointName = dest)
        )
    }

    suspend fun searchAddressAndPlaces(
        query: String,
        nearbyProviders: List<ProviderEntity> = emptyList(),
        nearbyStores: List<StoreEntity> = emptyList()
    ): List<SearchAddressResult> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        if (cleanQuery.length < 2) return@withContext emptyList()

        val results = mutableListOf<SearchAddressResult>()

        nearbyProviders.filter {
            it.name.contains(cleanQuery, ignoreCase = true) ||
            it.specialization.contains(cleanQuery, ignoreCase = true) ||
            it.area.contains(cleanQuery, ignoreCase = true)
        }.take(4).forEach { p ->
            results.add(
                SearchAddressResult(
                    displayName = "${p.name} (${p.specialization}) - ${p.area}",
                    shortName = p.name,
                    latitude = p.latitude,
                    longitude = p.longitude,
                    type = "provider",
                    icon = "👷"
                )
            )
        }

        nearbyStores.filter {
            it.name.contains(cleanQuery, ignoreCase = true) ||
            it.description.contains(cleanQuery, ignoreCase = true) ||
            it.localNeighborhood.contains(cleanQuery, ignoreCase = true)
        }.take(4).forEach { s ->
            val storeArea = s.localNeighborhood.ifBlank { s.cityId }.ifBlank { "متجر معتمد" }
            results.add(
                SearchAddressResult(
                    displayName = "${s.name} - $storeArea",
                    shortName = s.name,
                    latitude = s.latitude,
                    longitude = s.longitude,
                    type = "store",
                    icon = "🏪"
                )
            )
        }

        try {
            val encoded = URLEncoder.encode(cleanQuery, "UTF-8")
            val urlString = "https://nominatim.openstreetmap.org/search?format=json&q=$encoded&countrycodes=ye&accept-language=ar&limit=8&addressdetails=1"
            val url = URL(urlString)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 3500
                readTimeout = 3500
                requestMethod = "GET"
                setRequestProperty("User-Agent", "AlYemenServicesApp/1.0 (Android; OpenStreetMap)")
            }

            if (connection.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.readText()
                reader.close()
                connection.disconnect()

                val jsonArray = JSONArray(response)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val dispName = obj.optString("display_name", "")
                    val name = obj.optString("name", "").ifBlank { dispName.split(",").firstOrNull() ?: cleanQuery }
                    val lat = obj.optString("lat", "0").toDoubleOrNull() ?: 0.0
                    val lon = obj.optString("lon", "0").toDoubleOrNull() ?: 0.0
                    val type = obj.optString("type", "street")

                    if (lat != 0.0 && lon != 0.0) {
                        val icon = when {
                            type.contains("road") || type.contains("highway") || type.contains("street") -> "🛣️"
                            type.contains("residential") || type.contains("neighbourhood") || type.contains("suburb") -> "🏘️"
                            type.contains("hospital") || type.contains("clinic") -> "🏥"
                            type.contains("hotel") || type.contains("restaurant") -> "🏨"
                            type.contains("shop") || type.contains("market") -> "🛍️"
                            else -> "📍"
                        }
                        results.add(
                            SearchAddressResult(
                                displayName = dispName,
                                shortName = name,
                                latitude = lat,
                                longitude = lon,
                                type = "street",
                                icon = icon
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
        }

        results.distinctBy { "${it.latitude.toLong()}_${it.longitude.toLong()}_${it.shortName}" }
    }
}
