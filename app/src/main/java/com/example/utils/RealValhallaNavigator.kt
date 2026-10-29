package com.example.utils

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

/**
 * 🧭 RealValhallaNavigator
 * [إصلاح ملاحة نهائي] محرك الملاحة الرئيسي لليمن
 * - ترتيب الأولويات: OSRM أونلاين -> Valhalla أوفلاين -> Fallback مباشر تقريبي
 * - قفل متزامن بدون Deadlock
 * - بحث عناوين وشوارع ذكي مع تطبيع الحروف العربية
 */
object RealValhallaNavigator {

    private val mutex = Mutex()
    private const val TAG = "RealValhallaNavigator"

    fun isReady(context: Context): Boolean {
        return ValhallaTileDownloader.isTilesDownloaded(context)
    }

    suspend fun calculateRoute(
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

    suspend fun calculateRoute(
        context: Context,
        origin: NavWaypoint,
        waypoints: List<NavWaypoint>
    ): NavRouteResult = mutex.withLock {
        withContext(Dispatchers.IO) {
            try {
                // 1. [إصلاح ملاحة نهائي] الأولوية الأولى: تجربة OSRM عبر الإنترنت
                val onlineResult = OnlineOsrmNavigator.calculateRoute(origin, waypoints)
                if (onlineResult.error == null && onlineResult.coordinates.isNotEmpty()) {
                    return@withContext onlineResult
                }

                // 2. [إصلاح ملاحة نهائي] الأولوية الثانية: تجربة Valhalla أوفلاين إذا توفرت الحزمة المحملة
                if (isReady(context)) {
                    val nativeResult = tryRouteWithNativeValhalla(context, origin, waypoints)
                    if (nativeResult != null && nativeResult.error == null && nativeResult.coordinates.isNotEmpty()) {
                        return@withContext nativeResult
                    }
                }

                // 3. [إصلاح ملاحة نهائي] الأولوية الثالثة: مسار بديل مباشر
                return@withContext calculateDirectFallbackRoute(origin, waypoints)
            } catch (e: Exception) {
                Log.e(TAG, "Route calculation exception: ${e.message}", e)
                return@withContext calculateDirectFallbackRoute(origin, waypoints)
            }
        }
    }

    private fun tryRouteWithNativeValhalla(
        context: Context,
        origin: NavWaypoint,
        waypoints: List<NavWaypoint>
    ): NavRouteResult? {
        return try {
            val valhallaDir = ValhallaTileDownloader.getValhallaDir(context)
            val configFile = File(valhallaDir, "valhalla.json")
            if (!configFile.exists()) {
                ValhallaTileDownloader.createValhallaConfig(context)
            }
            if (!configFile.exists()) return null

            val valhalla = try {
                com.valhalla.valhalla.Valhalla(configFile.absolutePath)
            } catch (_: Throwable) {
                val clazz = Class.forName("com.valhalla.valhalla.Valhalla")
                val ctor = clazz.getConstructor(String::class.java)
                ctor.newInstance(configFile.absolutePath) as java.io.Closeable
            }

            val requestJson = formatValhallaRequest(origin, waypoints)
            val responseJson = if (valhalla is com.valhalla.valhalla.Valhalla) {
                val r = valhalla.routeRaw(requestJson)
                try { valhalla.close() } catch (_: Exception) {}
                r
            } else {
                val m = valhalla.javaClass.getMethod("routeRaw", String::class.java)
                val r = m.invoke(valhalla, requestJson) as String
                try { valhalla.close() } catch (_: Exception) {}
                r
            }

            if (!responseJson.isNullOrBlank()) {
                parseValhallaResponse(responseJson, origin, waypoints)
            } else null
        } catch (e: Throwable) {
            Log.w(TAG, "Native Valhalla execution failed or tile archive missing: ${e.message}")
            null
        }
    }

    private fun formatValhallaRequest(origin: NavWaypoint, waypoints: List<NavWaypoint>): String {
        val locations = (listOf(origin) + waypoints).map { wp ->
            org.json.JSONObject().apply {
                put("lat", wp.latitude)
                put("lon", wp.longitude)
                put("type", "break")
                put("name", wp.name)
            }
        }
        
        return org.json.JSONObject().apply {
            put("locations", org.json.JSONArray(locations))
            put("costing", "auto")
            put("directions_options", org.json.JSONObject().apply { put("units", "kilometers") })
        }.toString()
    }

    private fun parseValhallaResponse(
        json: String,
        origin: NavWaypoint,
        waypoints: List<NavWaypoint>
    ): NavRouteResult {
        val root = org.json.JSONObject(json)
        val trip = root.getJSONObject("trip")
        val summary = trip.getJSONObject("summary")
        
        val totalDist = summary.getDouble("length") * 1000.0
        val totalDur = summary.getDouble("time")
        
        val allWaypoints = listOf(origin) + waypoints
        val legsList = mutableListOf<NavLeg>()
        val allCoords = mutableListOf<Pair<Double, Double>>()
        
        val legsArr = trip.getJSONArray("legs")
        for (i in 0 until legsArr.length()) {
            val legObj = legsArr.getJSONObject(i)
            val legSummary = legObj.optJSONObject("summary")
            val legDist = (legSummary?.optDouble("length", 0.0) ?: 0.0) * 1000.0
            val legDur = legSummary?.optDouble("time", 0.0) ?: 0.0
            
            val legShapeEncoded = legObj.getString("shape")
            val legCoords = decodePolyline(legShapeEncoded)
            val currentCoordOffset = allCoords.size
            allCoords.addAll(legCoords)
            
            val legSteps = mutableListOf<NavStep>()
            val maneuversArr = legObj.optJSONArray("maneuvers")
            if (maneuversArr != null) {
                for (j in 0 until maneuversArr.length()) {
                    val manObj = maneuversArr.getJSONObject(j)
                    val manTypeInt = manObj.optInt("type", 0)
                    val manDist = manObj.optDouble("length", 0.0) * 1000.0
                    val manDur = manObj.optDouble("time", 0.0)
                    val instruction = manObj.optString("instruction", "")
                    val streetNames = manObj.optJSONArray("street_names")
                    val streetName = if (streetNames != null && streetNames.length() > 0) streetNames.getString(0) else ""
                    val mType = mapValhallaManeuver(manTypeInt)

                    legSteps.add(NavStep(
                        instruction = if (instruction.isNotEmpty()) instruction else ArabicInstructionFormatter.format(mType, streetName, manDist, waypoints.getOrNull(i)?.name ?: "الوجهة"),
                        distanceMeters = manDist,
                        durationSeconds = manDur,
                        maneuverType = mType,
                        streetName = streetName,
                        polylineIndex = currentCoordOffset + manObj.optInt("begin_shape_index", 0)
                    ))
                }
            }
            
            legsList.add(NavLeg(
                fromWaypointId = allWaypoints.getOrNull(i)?.id ?: "start",
                toWaypointId = allWaypoints.getOrNull(i + 1)?.id ?: "end",
                distanceMeters = legDist,
                durationSeconds = legDur,
                steps = legSteps
            ))
        }

        return NavRouteResult(
            totalDistanceMeters = totalDist,
            totalDurationSeconds = totalDur,
            coordinates = allCoords,
            legs = legsList,
            waypoints = allWaypoints,
            isOffline = true,
            engineSource = "Valhalla 🇾🇪 (أوفلاين حقيقي)"
        )
    }

    private fun mapValhallaManeuver(type: Int): ManeuverType {
        return when (type) {
            1, 2, 3 -> ManeuverType.START
            4, 5, 6 -> ManeuverType.DESTINATION
            8, 22 -> ManeuverType.STRAIGHT
            9 -> ManeuverType.SLIGHT_RIGHT
            10 -> ManeuverType.RIGHT
            11 -> ManeuverType.SHARP_RIGHT
            12, 13 -> ManeuverType.UTURN
            14 -> ManeuverType.SLIGHT_LEFT
            15 -> ManeuverType.LEFT
            16 -> ManeuverType.SHARP_LEFT
            18, 20 -> ManeuverType.HIGHWAY_EXIT
            25 -> ManeuverType.MERGE
            in 26..34 -> ManeuverType.ROUNDABOUT
            else -> ManeuverType.STRAIGHT
        }
    }

    private fun decodePolyline(encoded: String): List<Pair<Double, Double>> {
        val poly = mutableListOf<Pair<Double, Double>>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            poly.add(Pair(lat.toDouble() / 1E6, lng.toDouble() / 1E6))
        }
        return poly
    }

    private fun calculateDirectFallbackRoute(
        origin: NavWaypoint,
        waypoints: List<NavWaypoint>
    ): NavRouteResult {
        val allWaypoints = listOf(origin) + waypoints
        var totalDistance = 0.0
        val coordsList = mutableListOf<Pair<Double, Double>>()
        val legs = mutableListOf<NavLeg>()

        for (i in 0 until allWaypoints.size - 1) {
            val from = allWaypoints[i]
            val to = allWaypoints[i + 1]
            val directDist = HaversinePrecisionNavigator.calculateDistance(from.latitude, from.longitude, to.latitude, to.longitude)
            totalDistance += directDist

            // إضافة نقاط استيفاء لرسم خط واضح على الخريطة
            coordsList.add(Pair(from.latitude, from.longitude))
            val stepsCount = 10
            for (step in 1 until stepsCount) {
                val fraction = step.toDouble() / stepsCount
                val interpLat = from.latitude + fraction * (to.latitude - from.latitude)
                val interpLng = from.longitude + fraction * (to.longitude - from.longitude)
                coordsList.add(Pair(interpLat, interpLng))
            }
            coordsList.add(Pair(to.latitude, to.longitude))

            val legSteps = listOf(
                NavStep(
                    instruction = "اتجه مباشرة نحو ${to.name}",
                    distanceMeters = directDist,
                    durationSeconds = (directDist / 11.0).coerceAtLeast(60.0), // ~40 km/h
                    maneuverType = ManeuverType.STRAIGHT
                )
            )

            legs.add(NavLeg(
                fromWaypointId = from.id,
                toWaypointId = to.id,
                distanceMeters = directDist,
                durationSeconds = (directDist / 11.0).coerceAtLeast(60.0),
                steps = legSteps
            ))
        }

        return NavRouteResult(
            totalDistanceMeters = totalDistance,
            totalDurationSeconds = (totalDistance / 11.0).coerceAtLeast(60.0),
            coordinates = coordsList,
            legs = legs,
            waypoints = allWaypoints,
            isOffline = true,
            engineSource = "مسار مباشر (تقريبي)",
            error = null
        )
    }

    /**
     * [إصلاح ملاحة نهائي] البحث الشامل عن الأماكن والشوارع والمتاجر
     */
    suspend fun searchAddressAndPlaces(
        query: String,
        nearbyProviders: List<com.example.data.ProviderEntity> = emptyList(),
        nearbyStores: List<com.example.data.StoreEntity> = emptyList()
    ): List<SearchAddressResult> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        if (cleanQuery.length < 2) return@withContext emptyList()
        val results = mutableListOf<SearchAddressResult>()

        val normalizedQuery = normalizeArabic(cleanQuery)

        // 1. البحث المحلي في الفنيين
        nearbyProviders.filter {
            normalizeArabic(it.name).contains(normalizedQuery) ||
            normalizeArabic(it.area).contains(normalizedQuery) ||
            normalizeArabic(it.profession).contains(normalizedQuery)
        }.take(5).forEach { p ->
            results.add(SearchAddressResult("${p.name} (${p.profession}) - ${p.area}", p.name, p.latitude, p.longitude, "provider", "👷"))
        }

        // 2. البحث المحلي في المتاجر والمراكز
        nearbyStores.filter {
            normalizeArabic(it.name).contains(normalizedQuery) ||
            normalizeArabic(it.cityId).contains(normalizedQuery) ||
            normalizeArabic(it.description).contains(normalizedQuery)
        }.take(5).forEach { s ->
            results.add(SearchAddressResult("${s.name} - ${s.cityId}", s.name, s.latitude, s.longitude, "store", "🏪"))
        }
        
        // 3. البحث العالمي في الشوارع والمدن عبر Nominatim (OpenStreetMap)
        var connection: HttpURLConnection? = null
        try {
            val encoded = URLEncoder.encode(cleanQuery, "UTF-8")
            val urlString = "${NavConfig.NOMINATIM_BASE_URL}?format=json&q=$encoded&countrycodes=ye&accept-language=ar&limit=10"
            val url = URL(urlString)
            connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 12000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "AlYemenServicesApp/1.0 (Android; Nominatim)")
            }
            
            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonArr = org.json.JSONArray(response)
                for (i in 0 until jsonArr.length()) {
                    val obj = jsonArr.optJSONObject(i) ?: continue
                    val latStr = obj.optString("lat", "")
                    val lonStr = obj.optString("lon", "")
                    val lat = latStr.toDoubleOrNull() ?: obj.optDouble("lat", 0.0)
                    val lon = lonStr.toDoubleOrNull() ?: obj.optDouble("lon", 0.0)
                    if (lat != 0.0 && lon != 0.0) {
                        val displayName = obj.optString("display_name", "")
                        val shortName = displayName.split(",").firstOrNull()?.trim() ?: displayName
                        results.add(SearchAddressResult(
                            displayName = displayName,
                            shortName = shortName,
                            latitude = lat,
                            longitude = lon,
                            type = "address",
                            icon = "📍"
                        ))
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Nominatim search exception: ${e.message}")
        } finally {
            try {
                connection?.disconnect()
            } catch (e: Exception) {}
        }
        
        results.distinctBy { "${it.latitude}_${it.longitude}" }
    }

    private fun normalizeArabic(input: String): String {
        return input.lowercase(Locale("ar"))
            .replace(Regex("[إأآا]"), "ا")
            .replace(Regex("[ىي]"), "ي")
            .replace(Regex("[ؤو]"), "و")
            .replace(Regex("[ةه]"), "ه")
            .replace(Regex("[\u064B-\u0652]"), "")
    }

    data class SearchAddressResult(
        val displayName: String,
        val shortName: String,
        val latitude: Double,
        val longitude: Double,
        val type: String,
        val icon: String
    )
}
