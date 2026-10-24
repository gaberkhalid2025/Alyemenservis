package com.example.utils

import android.content.Context
import com.example.ui.screens.map.utils.MapDistanceCalculator
import org.json.JSONObject
import kotlin.math.*

/**
 * 🇾🇪 MockValhallaNavigator
 * محاكي شبكة الملاحة والمسارات السريعة بين المدن والشوارع اليمنية
 * - يقرأ mock_valhalla_network.json من assets
 * - يحسب المسار الدقيق، المسافة الحقيقية، والتعليمات التفصيلية خطوة بخطوة
 * - يدعم الملاحة الداخلية للشوارع والملاحة الدولية بين المحافظات
 */
object MockValhallaNavigator {

    data class CityNode(
        val id: String,
        val name: String,
        val lat: Double,
        val lng: Double,
        val streetNodes: List<StreetNode> = emptyList()
    )

    data class StreetNode(
        val name: String,
        val lat: Double,
        val lng: Double
    )

    data class HighwayEdge(
        val from: String,
        val to: String,
        val highwayName: String,
        val distanceKm: Double,
        val durationMins: Int,
        val maneuvers: List<Maneuver>
    )

    data class Maneuver(
        val instruction: String,
        val km: Double
    )

    data class NavigationStep(
        val instruction: String,
        val distanceText: String,
        val distanceMeters: Double,
        val iconType: String // "STRAIGHT", "LEFT", "RIGHT", "DESTINATION", "HIGHWAY"
    )

    data class NavigationRouteResult(
        val totalDistanceMeters: Double,
        val totalDurationSeconds: Int,
        val formattedDistance: String,
        val formattedDuration: String,
        val steps: List<NavigationStep>,
        val coordinates: List<Pair<Double, Double>>,
        val routeTitle: String
    )

    private var cachedCities: List<CityNode>? = null
    private var cachedHighways: List<HighwayEdge>? = null

    @Synchronized
    private fun loadNetwork(context: Context) {
        if (cachedCities != null && cachedHighways != null) return

        try {
            val jsonString = context.assets.open("mock_valhalla_network.json").bufferedReader().use { it.readText() }
            val root = JSONObject(jsonString)

            val citiesArray = root.optJSONArray("cities")
            val citiesList = mutableListOf<CityNode>()
            if (citiesArray != null) {
                for (i in 0 until citiesArray.length()) {
                    val cObj = citiesArray.getJSONObject(i)
                    val streetsArray = cObj.optJSONArray("streetNodes")
                    val streetsList = mutableListOf<StreetNode>()
                    if (streetsArray != null) {
                        for (j in 0 until streetsArray.length()) {
                            val sObj = streetsArray.getJSONObject(j)
                            streetsList.add(
                                StreetNode(
                                    name = sObj.optString("name", "شارع محلي"),
                                    lat = sObj.optDouble("lat", 0.0),
                                    lng = sObj.optDouble("lng", 0.0)
                                )
                            )
                        }
                    }
                    citiesList.add(
                        CityNode(
                            id = cObj.optString("id", ""),
                            name = cObj.optString("name", ""),
                            lat = cObj.optDouble("lat", 0.0),
                            lng = cObj.optDouble("lng", 0.0),
                            streetNodes = streetsList
                        )
                    )
                }
            }

            val highwaysArray = root.optJSONArray("highways")
            val highwaysList = mutableListOf<HighwayEdge>()
            if (highwaysArray != null) {
                for (i in 0 until highwaysArray.length()) {
                    val hObj = highwaysArray.getJSONObject(i)
                    val maneuversArray = hObj.optJSONArray("maneuvers")
                    val maneuversList = mutableListOf<Maneuver>()
                    if (maneuversArray != null) {
                        for (j in 0 until maneuversArray.length()) {
                            val mObj = maneuversArray.getJSONObject(j)
                            maneuversList.add(
                                Maneuver(
                                    instruction = mObj.optString("instruction", ""),
                                    km = mObj.optDouble("km", 1.0)
                                )
                            )
                        }
                    }
                    val from = hObj.optString("from", "")
                    val to = hObj.optString("to", "")
                    val name = hObj.optString("highwayName", "طريق دولي")
                    val dist = hObj.optDouble("distanceKm", 10.0)
                    val dur = hObj.optInt("durationMins", 15)

                    // إضافة الاتجاهين
                    highwaysList.add(HighwayEdge(from, to, name, dist, dur, maneuversList))
                    highwaysList.add(
                        HighwayEdge(
                            to,
                            from,
                            name,
                            dist,
                            dur,
                            maneuversList.reversed().map {
                                Maneuver(it.instruction.replace("جنوباً", "شمالاً").replace("شمالاً", "جنوباً"), it.km)
                            }
                        )
                    )
                }
            }

            cachedCities = citiesList
            cachedHighways = highwaysList
        } catch (_: Exception) {
            cachedCities = emptyList()
            cachedHighways = emptyList()
        }
    }

    /**
     * حساب المسار بين نقطتين في اليمن
     */
    fun calculateRoute(
        context: Context,
        startLat: Double,
        startLng: Double,
        destLat: Double,
        destLng: Double,
        destName: String = "الوجهة المحددة"
    ): NavigationRouteResult {
        loadNetwork(context)

        val directDistanceMeters = MapDistanceCalculator.calculateDistanceMeters(startLat, startLng, destLat, destLng)
        val cities = cachedCities.orEmpty()
        val highways = cachedHighways.orEmpty()

        // 1. إذا كانت المسافة قصيرة جداً (أقل من 35 كم) -> ملاحة داخل المدينة / الشوارع
        if (directDistanceMeters < 35_000.0 || cities.isEmpty()) {
            return generateLocalStreetRoute(startLat, startLng, destLat, destLng, destName, directDistanceMeters, cities)
        }

        // 2. ملاحة بين المدن والمحافظات
        val nearestStartCity = findNearestCity(startLat, startLng, cities)
        val nearestDestCity = findNearestCity(destLat, destLng, cities)

        if (nearestStartCity == null || nearestDestCity == null || nearestStartCity.id == nearestDestCity.id) {
            return generateLocalStreetRoute(startLat, startLng, destLat, destLng, destName, directDistanceMeters, cities)
        }

        // البحث عن أفضل مسار في شبكة الطرق
        val highwayPath = findHighwayPath(nearestStartCity.id, nearestDestCity.id, highways)
        if (highwayPath.isEmpty()) {
            return generateLocalStreetRoute(startLat, startLng, destLat, destLng, destName, directDistanceMeters, cities)
        }

        // بناء تفاصيل المسار بين المحافظات
        val steps = mutableListOf<NavigationStep>()
        val polyline = mutableListOf<Pair<Double, Double>>()
        polyline.add(Pair(startLat, startLng))

        var totalHighwayKm = 0.0
        var totalHighwayMins = 0

        // خطوة البدء
        steps.add(
            NavigationStep(
                instruction = "انطلق من موقعك الحالي باتجاه المخرج الرئيسي لـ ${nearestStartCity.name}",
                distanceText = "1.5 كم",
                distanceMeters = 1500.0,
                iconType = "STRAIGHT"
            )
        )

        for (edge in highwayPath) {
            totalHighwayKm += edge.distanceKm
            totalHighwayMins += edge.durationMins

            steps.add(
                NavigationStep(
                    instruction = "واصل السير على ${edge.highwayName}",
                    distanceText = "${edge.distanceKm.roundToInt()} كم",
                    distanceMeters = edge.distanceKm * 1000.0,
                    iconType = "HIGHWAY"
                )
            )

            for (m in edge.maneuvers) {
                steps.add(
                    NavigationStep(
                        instruction = m.instruction,
                        distanceText = if (m.km >= 1.0) "${m.km.roundToInt()} كم" else "${(m.km * 1000).roundToInt()} م",
                        distanceMeters = m.km * 1000.0,
                        iconType = if (m.instruction.contains("انعطف") || m.instruction.contains("منعرجات")) "LEFT" else "STRAIGHT"
                    )
                )
            }

            val targetCityObj = cities.find { it.id == edge.to }
            if (targetCityObj != null) {
                polyline.add(Pair(targetCityObj.lat, targetCityObj.lng))
            }
        }

        // خطوة الوصول النهائية
        steps.add(
            NavigationStep(
                instruction = "انعطف نحو الشارع المؤدي إلى $destName",
                distanceText = "400 م",
                distanceMeters = 400.0,
                iconType = "RIGHT"
            )
        )
        steps.add(
            NavigationStep(
                instruction = "لقد وصلت إلى وجهتك في $destName",
                distanceText = "0 م",
                distanceMeters = 0.0,
                iconType = "DESTINATION"
            )
        )
        polyline.add(Pair(destLat, destLng))

        val finalTotalMeters = (totalHighwayKm * 1000.0) + 3000.0
        val finalTotalSeconds = (totalHighwayMins * 60) + 300

        return NavigationRouteResult(
            totalDistanceMeters = finalTotalMeters,
            totalDurationSeconds = finalTotalSeconds,
            formattedDistance = MapDistanceCalculator.formatDistance(finalTotalMeters),
            formattedDuration = formatDuration(finalTotalSeconds),
            steps = steps,
            coordinates = polyline,
            routeTitle = "مسار: ${nearestStartCity.name} ⬅️ ${nearestDestCity.name}"
        )
    }

    private fun generateLocalStreetRoute(
        startLat: Double,
        startLng: Double,
        destLat: Double,
        destLng: Double,
        destName: String,
        directDistanceMeters: Double,
        cities: List<CityNode>
    ): NavigationRouteResult {
        // المسافة على الطريق تكون عادة 1.25x إلى 1.35x المسافة الخطية بسبب الشوارع والدوارات
        val realisticRoadMeters = (directDistanceMeters * 1.3).coerceAtLeast(350.0)
        val speedMps = 11.1 // ~40 كم/س في شوارع المدن
        val durationSeconds = (realisticRoadMeters / speedMps).roundToInt().coerceAtLeast(90)

        val steps = mutableListOf<NavigationStep>()
        val polyline = mutableListOf<Pair<Double, Double>>()
        polyline.add(Pair(startLat, startLng))

        // البحث عما إذا كان هناك شارع رئيسي قريب
        val nearestCity = findNearestCity(startLat, startLng, cities)
        val street1 = nearestCity?.streetNodes?.getOrNull(0)?.name ?: "الشارع الرئيسي"
        val street2 = nearestCity?.streetNodes?.getOrNull(1)?.name ?: "الشارع التجاري"

        if (realisticRoadMeters > 3000.0) {
            steps.add(
                NavigationStep(
                    instruction = "اتجه للأمام مباشرة نحو $street1",
                    distanceText = "450 م",
                    distanceMeters = 450.0,
                    iconType = "STRAIGHT"
                )
            )
            steps.add(
                NavigationStep(
                    instruction = "انعطف يميناً عند التقاطع باتجاه $street2",
                    distanceText = "850 م",
                    distanceMeters = 850.0,
                    iconType = "RIGHT"
                )
            )
            steps.add(
                NavigationStep(
                    instruction = "تابع السير على الطريق الرئيسي",
                    distanceText = MapDistanceCalculator.formatDistance(realisticRoadMeters - 1800.0),
                    distanceMeters = realisticRoadMeters - 1800.0,
                    iconType = "STRAIGHT"
                )
            )
            steps.add(
                NavigationStep(
                    instruction = "انعطف يساراً نحو مدخل $destName",
                    distanceText = "300 م",
                    distanceMeters = 300.0,
                    iconType = "LEFT"
                )
            )
        } else {
            steps.add(
                NavigationStep(
                    instruction = "اتجه للأمام في الشارع الحالي",
                    distanceText = "200 م",
                    distanceMeters = 200.0,
                    iconType = "STRAIGHT"
                )
            )
            steps.add(
                NavigationStep(
                    instruction = "انعطف يميناً نحو $destName",
                    distanceText = MapDistanceCalculator.formatDistance(realisticRoadMeters - 200.0),
                    distanceMeters = realisticRoadMeters - 200.0,
                    iconType = "RIGHT"
                )
            )
        }

        steps.add(
            NavigationStep(
                instruction = "لقد وصلت إلى وجهتك في $destName",
                distanceText = "0 م",
                distanceMeters = 0.0,
                iconType = "DESTINATION"
            )
        )
        polyline.add(Pair(destLat, destLng))

        return NavigationRouteResult(
            totalDistanceMeters = realisticRoadMeters,
            totalDurationSeconds = durationSeconds,
            formattedDistance = MapDistanceCalculator.formatDistance(realisticRoadMeters),
            formattedDuration = formatDuration(durationSeconds),
            steps = steps,
            coordinates = polyline,
            routeTitle = "ملاحة شوارع محلية نحو $destName"
        )
    }

    private fun findNearestCity(lat: Double, lng: Double, cities: List<CityNode>): CityNode? {
        return cities.minByOrNull {
            MapDistanceCalculator.calculateDistanceMeters(lat, lng, it.lat, it.lng)
        }
    }

    /**
     * خوارزمية BFS للعثور على أقصر مسار بين عقد المدن
     */
    private fun findHighwayPath(startCityId: String, destCityId: String, highways: List<HighwayEdge>): List<HighwayEdge> {
        val queue = ArrayDeque<Pair<String, List<HighwayEdge>>>()
        queue.add(Pair(startCityId, emptyList()))
        val visited = mutableSetOf<String>()
        visited.add(startCityId)

        while (queue.isNotEmpty()) {
            val (currentCity, currentPath) = queue.removeFirst()
            if (currentCity == destCityId) {
                return currentPath
            }

            val outgoing = highways.filter { it.from == currentCity }
            for (edge in outgoing) {
                if (!visited.contains(edge.to)) {
                    visited.add(edge.to)
                    val newPath = currentPath + edge
                    queue.add(Pair(edge.to, newPath))
                }
            }
        }
        return emptyList()
    }

    private fun formatDuration(seconds: Int): String {
        val hours = seconds / 3600
        val mins = (seconds % 3600) / 60
        return when {
            hours > 0 && mins > 0 -> "$hours ساعة و $mins دقيقة"
            hours > 0 -> "$hours ساعة"
            mins > 0 -> "$mins دقيقة"
            else -> "أقل من دقيقة"
        }
    }
}
