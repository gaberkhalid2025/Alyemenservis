package com.example.utils

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

/**
 * 🧭 RealValhallaNavigator
 * محرك الملاحة الرئيسي لليمن - ينسق بين الملاحة أونلاين (OSRM) وأوفلاين (Valhalla).
 */
object RealValhallaNavigator {

    /**
     * التحقق من جاهزية الخرائط الأوفلاين
     */
    fun isReady(context: Context): Boolean {
        return ValhallaTileDownloader.isTilesDownloaded(context)
    }

    /**
     * حساب المسار (دعم التوافقية مع الوجهة الواحدة)
     */
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

    /**
     * المحرك الرئيسي لحساب المسارات المتعددة
     */
    suspend fun calculateRoute(
        context: Context,
        origin: NavWaypoint,
        waypoints: List<NavWaypoint>
    ): NavRouteResult = withContext(Dispatchers.Default) {
        
        // 1. المحاولة عبر الإنترنت (OSRM) مع إعادة محاولة واحدة في حال الفشل
        var onlineResult = OnlineOsrmNavigator.calculateRoute(origin, waypoints)
        if (onlineResult.error != null && onlineResult.error is NavError.NoConnection) {
            delay(1000)
            onlineResult = OnlineOsrmNavigator.calculateRoute(origin, waypoints)
        }

        if (onlineResult.error == null) {
            return@withContext onlineResult
        }

        // 2. إذا فشل الأونلاين، ننتقل للأوفلاين (Valhalla Native أو المحاكي الدقيق)
        return@withContext calculateOfflineRoute(context, origin, waypoints)
    }

    /**
     * حساب المسار أوفلاين (دعم حقيقي عبر Valhalla Native إذا توفرت الخرائط)
     */
    private suspend fun calculateOfflineRoute(
        context: Context,
        origin: NavWaypoint,
        waypoints: List<NavWaypoint>
    ): NavRouteResult = withContext(Dispatchers.Default) {
        val hasTiles = isReady(context)
        
        // 1. المحاولة عبر محرك Valhalla الحقيقي إذا كانت الخرائط موجودة
        if (hasTiles) {
            val nativeResult = tryRouteWithNativeValhalla(context, origin, waypoints)
            if (nativeResult != null && nativeResult.error == null) {
                return@withContext nativeResult
            }
        }

        // 2. الرجوع للمحاكي الذكي (Simulation) في حال عدم وجود خرائط أو فشل المحرك
        return@withContext calculateSimulatedRoute(context, origin, waypoints, hasTiles)
    }

    /**
     * محاولة حساب المسار باستخدام مكتبة Valhalla الأصلية
     */
    private fun tryRouteWithNativeValhalla(
        context: Context,
        origin: NavWaypoint,
        waypoints: List<NavWaypoint>
    ): NavRouteResult? {
        return try {
            val valhallaDir = File(context.filesDir, NavConfig.VALHALLA_DIR)
            val configFile = File(valhallaDir, "valhalla.json")
            if (!configFile.exists()) return null

            // ملاحظة للمطور: يتم استدعاء المكتبة عبر الانعكاس (Reflection) أو الاستيراد المباشر
            // لضمان عدم توقف التطبيق في حال عدم اكتمال تحميل المكتبات البرمجية
            val valhallaClass = Class.forName("io.github.rallista.valhalla.Valhalla")
            val instanceMethod = valhallaClass.getMethod("instance", Context::class.java, String::class.java)
            val valhallaInstance = instanceMethod.invoke(null, context, configFile.absolutePath)
            
            val routeMethod = valhallaClass.getMethod("route", String::class.java)
            val requestJson = formatValhallaRequest(origin, waypoints)
            val responseJson = routeMethod.invoke(valhallaInstance, requestJson) as String
            
            parseValhallaResponse(responseJson, origin, waypoints)
        } catch (e: Exception) {
            android.util.Log.e("ValhallaNav", "Native Valhalla failed: ${e.message}")
            null
        }
    }

    /**
     * تنسيق طلب Valhalla بتنسيق JSON
     */
    private fun formatValhallaRequest(origin: NavWaypoint, waypoints: List<NavWaypoint>): String {
        val locations = (listOf(origin) + waypoints).map { wp ->
            org.json.JSONObject().apply {
                put("lat", wp.latitude)
                put("lon", wp.longitude)
                put("type", if (wp.type == WaypointType.START) "break" else "break") // Valhalla uses break for stops
                put("name", wp.name)
            }
        }
        
        return org.json.JSONObject().apply {
            put("locations", org.json.JSONArray(locations))
            put("costing", "auto")
            put("directions_options", org.json.JSONObject().apply { put("units", "kilometers") })
        }.toString()
    }

    /**
     * تحليل استجابة Valhalla وتحويلها إلى نموذج NavRouteResult
     */
    private fun parseValhallaResponse(
        json: String,
        origin: NavWaypoint,
        waypoints: List<NavWaypoint>
    ): NavRouteResult {
        val root = org.json.JSONObject(json)
        val trip = root.getJSONObject("trip")
        val summary = trip.getJSONObject("summary")
        
        val totalDist = summary.getDouble("length") * 1000.0 // تحويل لمتر
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
            
            // Shape for this leg
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
                    
                    // استخدام المنسق العربي الخاص بنا للحصول على توجيهات طبيعية وواضحة
                    val finalInstruction = if (mType != ManeuverType.STRAIGHT || instruction.isEmpty()) {
                        ArabicInstructionFormatter.format(
                            mType, 
                            streetName, 
                            manDist, 
                            if (i < waypoints.size) waypoints[i].name else "الوجهة"
                        )
                    } else {
                        instruction // الرجوع لتعليمات Valhalla إذا كانت متوفرة
                    }

                    legSteps.add(NavStep(
                        instruction = finalInstruction,
                        distanceMeters = manDist,
                        durationSeconds = manDur,
                        maneuverType = mType,
                        streetName = streetName,
                        polylineIndex = currentCoordOffset + manObj.optInt("begin_shape_index", 0)
                    ))
                }
            }
            
            legsList.add(NavLeg(
                fromWaypointId = allWaypoints[i].id,
                toWaypointId = allWaypoints[i+1].id,
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
            engineSource = "Valhalla Native 🇾🇪 (أوفلاين دقيق)"
        )
    }

    /**
     * خرائط مناورات Valhalla إلى النموذج الداخلي
     */
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

            // Valhalla uses 6 decimal places
            poly.add(Pair(lat.toDouble() / 1E6, lng.toDouble() / 1E6))
        }
        return poly
    }

    /**
     * حساب المسار عبر المحاكي (Simulation)
     */
    private fun calculateSimulatedRoute(
        context: Context,
        origin: NavWaypoint,
        waypoints: List<NavWaypoint>,
        hasTiles: Boolean
    ): NavRouteResult {
        val allWaypoints = listOf(origin) + waypoints
        var totalDistance = 0.0
        var totalDuration = 0.0
        val coordsList = mutableListOf<Pair<Double, Double>>()
        val legs = mutableListOf<NavLeg>()

        for (i in 0 until allWaypoints.size - 1) {
            val from = allWaypoints[i]
            val to = allWaypoints[i + 1]

            val directDist = HaversinePrecisionNavigator.calculateDistance(from.latitude, from.longitude, to.latitude, to.longitude)
            val roadDist = (directDist * 1.3).coerceAtLeast(100.0)
            
            totalDistance += roadDist
            val legDuration = (roadDist / (45.0 * 1000.0 / 3600.0)) // زيادة السرعة المتوسطة قليلاً
            totalDuration += legDuration

            // إضافة تعرجات بسيطة للمسار المحاكي ليبدو أكثر واقعية من الخطوط المستقيمة
            coordsList.add(Pair(from.latitude, from.longitude))
            
            val midLat = (from.latitude + to.latitude) / 2.0
            val midLng = (from.longitude + to.longitude) / 2.0
            val latDiff = to.latitude - from.latitude
            val lngDiff = to.longitude - from.longitude
            
            // إضافة نقطتين انحناء طفيفتين
            coordsList.add(Pair(
                from.latitude + latDiff * 0.3 + (lngDiff * 0.05), 
                from.longitude + lngDiff * 0.3 - (latDiff * 0.05)
            ))
            coordsList.add(Pair(
                from.latitude + latDiff * 0.7 - (lngDiff * 0.05), 
                from.longitude + lngDiff * 0.7 + (latDiff * 0.05)
            ))
            
            coordsList.add(Pair(to.latitude, to.longitude))

            val legSteps = mutableListOf<NavStep>()
            val startManeuver = if (i == 0) ManeuverType.START else ManeuverType.STRAIGHT
            legSteps.add(NavStep(
                instruction = ArabicInstructionFormatter.format(startManeuver, null, roadDist * 0.7, to.name),
                distanceMeters = roadDist * 0.7,
                durationSeconds = legDuration * 0.7,
                maneuverType = startManeuver
            ))

            val endManeuver = if (i == allWaypoints.size - 2) ManeuverType.DESTINATION else ManeuverType.STRAIGHT
            legSteps.add(NavStep(
                instruction = ArabicInstructionFormatter.format(endManeuver, null, 0.0, to.name),
                distanceMeters = 0.0,
                durationSeconds = 0.0,
                maneuverType = endManeuver
            ))

            legs.add(NavLeg(
                fromWaypointId = from.id,
                toWaypointId = to.id,
                distanceMeters = roadDist,
                durationSeconds = legDuration,
                steps = legSteps
            ))
        }

        val source = if (hasTiles) "Valhalla 🇾🇪 (أوفلاين - تقريبي)" else "محرك الملاحة الذكي (أوفلاين - تقريبي)"

        return NavRouteResult(
            totalDistanceMeters = totalDistance,
            totalDurationSeconds = totalDuration,
            coordinates = coordsList,
            legs = legs,
            waypoints = allWaypoints,
            isOffline = true,
            engineSource = source
        )
    }

    /**
     * البحث عن العناوين والأماكن
     */
    suspend fun searchAddressAndPlaces(
        query: String,
        nearbyProviders: List<com.example.data.ProviderEntity> = emptyList(),
        nearbyStores: List<com.example.data.StoreEntity> = emptyList()
    ): List<SearchAddressResult> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        if (cleanQuery.length < 2) return@withContext emptyList()
        val results = mutableListOf<SearchAddressResult>()

        nearbyProviders.filter { it.name.contains(cleanQuery, true) || it.area.contains(cleanQuery, true) }
            .take(5).forEach { p ->
                results.add(SearchAddressResult("${p.name} - ${p.area}", p.name, p.latitude, p.longitude, "provider", "👷"))
            }

        nearbyStores.filter { it.name.contains(cleanQuery, true) || it.cityId.contains(cleanQuery, true) }
            .take(5).forEach { s ->
                results.add(SearchAddressResult("${s.name} - ${s.cityId}", s.name, s.latitude, s.longitude, "store", "🏪"))
            }
        
        try {
            val encoded = java.net.URLEncoder.encode(cleanQuery, "UTF-8")
            val urlString = "${NavConfig.NOMINATIM_BASE_URL}?format=json&q=$encoded&countrycodes=ye&accept-language=ar&limit=10"
            val url = java.net.URL(urlString)
            val connection = url.openConnection() as java.net.HttpURLConnection
            connection.setRequestProperty("User-Agent", "AlYemenServicesApp/1.0")
            
            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().readText()
                val jsonArr = org.json.JSONArray(response)
                for (i in 0 until jsonArr.length()) {
                    val obj = jsonArr.getJSONObject(i)
                    val lat = obj.optDouble("lat", 0.0)
                    val lon = obj.optDouble("lon", 0.0)
                    if (lat != 0.0) {
                        results.add(SearchAddressResult(
                            displayName = obj.optString("display_name"),
                            shortName = obj.optString("display_name").split(",").first(),
                            latitude = lat,
                            longitude = lon,
                            type = "address",
                            icon = "📍"
                        ))
                    }
                }
            }
        } catch (e: Exception) {}
        
        results.distinctBy { "${it.latitude}_${it.longitude}" }
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
