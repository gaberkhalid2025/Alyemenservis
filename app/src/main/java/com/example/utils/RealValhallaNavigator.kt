package com.example.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

/**
 * حالة محرك Valhalla الأصلي
 */
object NativeValhallaStatus {
    @Volatile var lastError: String? = null
    @Volatile var lastSuccessAt: Long = 0L
}

/**
 * 🧭 RealValhallaNavigator
 * [إصلاح ملاحة نهائي] محرك الملاحة الرئيسي لليمن
 * - ترتيب الأولويات: Valhalla أوفلاين (إن توفرت الحزمة) -> OSRM أونلاين -> Fallback مباشر تقريبي
 * - قفل متزامن خاص بالمحرك الأصلي فقط (nativeMutex) لمنع أي Deadlock
 * - بحث عناوين وشوارع ذكي مع تطبيع الحروف العربية وتتبع أخطاء البحث
 */
object RealValhallaNavigator {

    private val nativeMutex = Mutex()
    private const val TAG = "RealValhallaNavigator"

    @Volatile var lastSearchError: String? = null

    private val searchMutex = Mutex()
    private var lastNominatimAt = 0L
    private val searchCache = object : LinkedHashMap<String, List<SearchAddressResult>>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<SearchAddressResult>>?) = size > 40
    }

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
    ): NavRouteResult = withContext(Dispatchers.IO) {
        validateRequest(origin, waypoints)?.let { return@withContext it }
        try {
            computeRoute(context, origin, waypoints)
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            Log.e(TAG, "Route calculation failed: ${t.message}", t)
            calculateDirectFallbackRoute(origin, waypoints)
        }
    }

    private fun isValidCoordinate(lat: Double, lng: Double): Boolean =
        !lat.isNaN() && !lng.isNaN() && lat in -90.0..90.0 && lng in -180.0..180.0 && !(lat == 0.0 && lng == 0.0)

    private fun validateRequest(origin: NavWaypoint, waypoints: List<NavWaypoint>): NavRouteResult? {
        val all = listOf(origin) + waypoints
        val problem = when {
            waypoints.isEmpty() -> "لا توجد وجهة"
            all.any { !isValidCoordinate(it.latitude, it.longitude) } -> "إحداثيات غير صالحة"
            else -> null
        } ?: return null
        return NavRouteResult(
            totalDistanceMeters = 0.0, totalDurationSeconds = 0.0,
            coordinates = emptyList(), legs = emptyList(), waypoints = all,
            isOffline = true, engineSource = "تقريبي - $problem",
            error = NavError.Unknown(problem)
        )
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val net = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(net) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private suspend fun computeRoute(context: Context, origin: NavWaypoint, waypoints: List<NavWaypoint>): NavRouteResult {
        // 1) أوفلاين أولاً إذا كانت الحزمة سليمة (القفل هنا فقط لأن المحرك الأصلي لا يُستدعى بالتوازي)
        if (isReady(context)) {
            val nativeResult = nativeMutex.withLock { tryRouteWithNativeValhalla(context, origin, waypoints) }
            if (nativeResult != null && nativeResult.error == null && nativeResult.coordinates.size >= 2) {
                return nativeResult
            }
        }
        // 2) أونلاين (OSRM) مع مهلة 20 ثانية
        if (isNetworkAvailable(context)) {
            val online = try {
                withTimeoutOrNull(20_000L) { OnlineOsrmNavigator.calculateRoute(origin, waypoints) }
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                Log.e(TAG, "OSRM failed: ${t.message}", t)
                null
            }
            if (online != null && online.error == null && online.coordinates.size >= 2) {
                return online
            }
        }
        // 3) تقريبي - مسافة مباشرة بلا تعليمات منعطفات
        return calculateDirectFallbackRoute(origin, waypoints)
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
            if (!configFile.exists()) {
                NativeValhallaStatus.lastError = "valhalla.json غير موجود"
                return null
            }

            val requestJson = formatValhallaRequest(origin, waypoints)

            // محاولة إنشاء واستدعاء Valhalla مباشرة أو عبر Reflection
            val responseJson = try {
                val valhalla = com.valhalla.valhalla.Valhalla(configFile.absolutePath)
                val res = valhalla.routeRaw(requestJson)
                try { valhalla.close() } catch (_: Throwable) {}
                res
            } catch (t1: Throwable) {
                val clazz = Class.forName("com.valhalla.valhalla.Valhalla")
                val ctor = clazz.getConstructor(String::class.java)
                val instance = ctor.newInstance(configFile.absolutePath)
                val method = clazz.getMethod("routeRaw", String::class.java)
                val res = method.invoke(instance, requestJson) as String
                try { (instance as? java.io.Closeable)?.close() } catch (_: Throwable) {}
                res
            }

            if (!responseJson.isNullOrBlank()) {
                val parsed = parseValhallaResponse(responseJson, origin, waypoints)
                NativeValhallaStatus.lastError = null
                NativeValhallaStatus.lastSuccessAt = System.currentTimeMillis()
                parsed
            } else {
                NativeValhallaStatus.lastError = "استجابة Valhalla فارغة"
                null
            }
        } catch (t: Throwable) {
            val cause = (t as? java.lang.reflect.InvocationTargetException)?.targetException ?: t
            NativeValhallaStatus.lastError = "${cause.javaClass.simpleName}: ${cause.message}"
            Log.e(TAG, "Native Valhalla failed: ${NativeValhallaStatus.lastError}", cause)
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
                    val streetNames = manObj.optJSONArray("street_names")
                    val streetName = if (streetNames != null && streetNames.length() > 0) streetNames.getString(0) else ""
                    val mType = mapValhallaManeuver(manTypeInt)

                    legSteps.add(NavStep(
                        instruction = ArabicInstructionFormatter.format(
                            mType,
                            streetName,
                            manDist,
                            waypoints.getOrNull(i)?.name ?: "الوجهة"
                        ),
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

    private fun mapValhallaManeuver(type: Int): ManeuverType = when (type) {
        1, 2, 3 -> ManeuverType.START
        4, 5, 6 -> ManeuverType.DESTINATION
        7, 8, 17, 22, 23, 24 -> ManeuverType.STRAIGHT
        9 -> ManeuverType.SLIGHT_RIGHT
        10 -> ManeuverType.RIGHT
        11 -> ManeuverType.SHARP_RIGHT
        12, 13 -> ManeuverType.UTURN
        14 -> ManeuverType.SHARP_LEFT
        15 -> ManeuverType.LEFT
        16 -> ManeuverType.SLIGHT_LEFT
        18, 19, 20, 21 -> ManeuverType.HIGHWAY_EXIT
        25, 37, 38 -> ManeuverType.MERGE
        26, 27 -> ManeuverType.ROUNDABOUT
        else -> ManeuverType.STRAIGHT
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
                    instruction = "تقريبي - مسافة مباشرة إلى ${to.name}",
                    distanceMeters = directDist,
                    durationSeconds = (directDist / 10.0).coerceAtLeast(60.0), // ~36 km/h
                    maneuverType = if (i == allWaypoints.size - 2) ManeuverType.DESTINATION else ManeuverType.STRAIGHT
                )
            )

            legs.add(NavLeg(
                fromWaypointId = from.id,
                toWaypointId = to.id,
                distanceMeters = directDist,
                durationSeconds = (directDist / 10.0).coerceAtLeast(60.0),
                steps = legSteps
            ))
        }

        return NavRouteResult(
            totalDistanceMeters = totalDistance,
            totalDurationSeconds = (totalDistance / 10.0).coerceAtLeast(60.0),
            coordinates = coordsList,
            legs = legs,
            waypoints = allWaypoints,
            isOffline = true,
            engineSource = "تقريبي - مسافة مباشرة",
            error = NavError.RouteNotFound
        )
    }

    /**
     * [إصلاح ملاحة نهائي] البحث الشامل عن الأماكن والشوارع والمتاجر
     */
    suspend fun searchAddressAndPlaces(
        query: String,
        nearbyProviders: List<com.example.data.ProviderEntity> = emptyList(),
        nearbyStores: List<com.example.data.StoreEntity> = emptyList(),
        userLat: Double = 0.0,
        userLng: Double = 0.0
    ): List<SearchAddressResult> = withContext(Dispatchers.IO) {
        lastSearchError = null
        val clean = query.trim()
        if (clean.length < 3) return@withContext emptyList()
        val nq = normalizeArabic(clean)
        val hasUser = !(userLat == 0.0 && userLng == 0.0)

        // 1) محلي أولاً (فقط ما له إحداثيات صالحة)
        val local = mutableListOf<SearchAddressResult>()
        nearbyProviders.filter { it.latitude != 0.0 && it.longitude != 0.0 &&
            (normalizeArabic(it.name).contains(nq) || normalizeArabic(it.area).contains(nq)) }
            .take(6).forEach { p -> local.add(SearchAddressResult("${p.name} - ${p.area}", p.name, p.latitude, p.longitude, "provider", "👷")) }
        nearbyStores.filter { it.latitude != 0.0 && it.longitude != 0.0 &&
            (normalizeArabic(it.name).contains(nq) || normalizeArabic(it.cityId).contains(nq)) }
            .take(6).forEach { s -> local.add(SearchAddressResult("${s.name} - ${s.cityId}", s.name, s.latitude, s.longitude, "store", "🏪")) }

        // 2) Nominatim: حد 1 طلب/ثانية + كاش + رسالة خطأ واضحة
        val remote: List<SearchAddressResult> = searchMutex.withLock {
            searchCache[nq]?.let { return@withLock it }
            val wait = 1_100L - (System.currentTimeMillis() - lastNominatimAt)
            if (wait > 0) delay(wait)
            var r = fetchNominatim(clean, userLat, userLng, hasUser)
            if (r != null && r.isEmpty() && clean.contains("اي")) { // هايل ↔ هائل
                delay(1_100L)
                r = fetchNominatim(clean.replace("اي", "ائ"), userLat, userLng, hasUser)
            }
            lastNominatimAt = System.currentTimeMillis()
            if (r != null) searchCache[nq] = r
            r ?: emptyList()
        }

        val sortedRemote = if (hasUser) remote.sortedBy {
            HaversinePrecisionNavigator.calculateDistance(userLat, userLng, it.latitude, it.longitude)
        } else remote
        (local + sortedRemote).distinctBy { String.format(Locale.US, "%.5f_%.5f", it.latitude, it.longitude) }
    }

    /** null = فشل (يُسجَّل السبب في lastSearchError)، قائمة فارغة = لا نتائج. */
    private fun fetchNominatim(q: String, lat: Double, lng: Double, hasUser: Boolean): List<SearchAddressResult>? {
        var c: HttpURLConnection? = null
        return try {
            val encoded = URLEncoder.encode(q, "UTF-8")
            val viewbox = if (hasUser) "&viewbox=${lng - 1.0},${lat + 1.0},${lng + 1.0},${lat - 1.0}&bounded=0" else ""
            val url = URL("${NavConfig.NOMINATIM_BASE_URL}?format=jsonv2&q=$encoded&countrycodes=ye&accept-language=ar&limit=10$viewbox")
            c = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8_000; readTimeout = 12_000; requestMethod = "GET"
                setRequestProperty("User-Agent", "AlYemenServicesApp/1.0 (Android; Nominatim)")
            }
            when (val code = c.responseCode) {
                200 -> {
                    val arr = JSONArray(c.inputStream.bufferedReader().use { it.readText() })
                    val out = mutableListOf<SearchAddressResult>()
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        val la = o.optString("lat").toDoubleOrNull() ?: continue
                        val lo = o.optString("lon").toDoubleOrNull() ?: continue
                        if (la == 0.0 && lo == 0.0) continue
                        val display = o.optString("display_name")
                        out.add(SearchAddressResult(display, display.split(",").first().trim(), la, lo, "address", "📍"))
                    }
                    out
                }
                429 -> { lastSearchError = "تم تجاوز حد الطلبات، حاول بعد قليل"; null }
                else -> { lastSearchError = "خدمة البحث غير متاحة حالياً (رمز $code)"; null }
            }
        } catch (e: java.net.UnknownHostException) {
            lastSearchError = "لا يوجد اتصال بالإنترنت — البحث متاح فقط في الأماكن المحلية"; null
        } catch (e: java.net.SocketTimeoutException) {
            lastSearchError = "انتهت مهلة الاتصال بخدمة البحث"; null
        } catch (t: Throwable) {
            Log.e(TAG, "Nominatim failed: ${t.message}"); lastSearchError = "تعذر البحث الآن"; null
        } finally { c?.disconnect() }
    }

    private fun normalizeArabic(input: String): String = input.trim().lowercase(Locale.ROOT)
        .replace(Regex("[\\u064B-\\u0652\\u0640]"), "") // تشكيل + تطويل
        .replace(Regex("[إأآٱ]"), "ا")
        .replace('ى', 'ي').replace('ئ', 'ي')
        .replace('ؤ', 'و')
        .replace('ة', 'ه')
        .replace(Regex("\\s+"), " ")

    data class SearchAddressResult(
        val displayName: String,
        val shortName: String,
        val latitude: Double,
        val longitude: Double,
        val type: String,
        val icon: String
    )
}
