package com.example.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
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

    private var cachedEngine: com.valhalla.valhalla.Valhalla? = null
    private var cachedConfigPath: String? = null

    /** تُستدعى فقط داخل nativeMutex. */
    private fun engineFor(configPath: String): com.valhalla.valhalla.Valhalla {
        val existing = cachedEngine
        if (existing != null && cachedConfigPath == configPath) return existing
        try { existing?.close() } catch (_: Throwable) {}
        return com.valhalla.valhalla.Valhalla(configPath).also {
            cachedEngine = it
            cachedConfigPath = configPath
        }
    }

    /** يجب استدعاؤها قبل استبدال ملفات الخرائط (من المنزِّل) وعند أي خطأ أصلي. */
    suspend fun releaseEngine() = nativeMutex.withLock {
        try { cachedEngine?.close() } catch (_: Throwable) {}
        cachedEngine = null
        cachedConfigPath = null
    }

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
        destName: String,
        driving: Boolean = true
    ): NavRouteResult {
        val origin = NavWaypoint(name = "موقعي الحالي", latitude = startLat, longitude = startLng, type = WaypointType.START)
        val destination = NavWaypoint(name = destName, latitude = destLat, longitude = destLng, type = WaypointType.BREAK)
        return calculateRoute(context, origin, listOf(destination), driving)
    }

    suspend fun calculateRoute(
        context: Context,
        origin: NavWaypoint,
        waypoints: List<NavWaypoint>,
        driving: Boolean = true
    ): NavRouteResult = withContext(Dispatchers.IO) {
        validateRequest(origin, waypoints)?.let { return@withContext it }
        try {
            computeRoute(context, origin, waypoints, driving)
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            Log.e(TAG, "Route calculation failed: ${t.message}", t)
            calculateDirectFallbackRoute(origin, waypoints, if (driving) 10.0 else 1.4)
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

    private suspend fun computeRoute(context: Context, origin: NavWaypoint, waypoints: List<NavWaypoint>, driving: Boolean): NavRouteResult {
        // 1) أوفلاين أولاً إذا كانت الحزمة سليمة (القفل هنا فقط لأن المحرك الأصلي لا يُستدعى بالتوازي)
        if (isReady(context)) {
            val nativeResult = nativeMutex.withLock { tryRouteWithNativeValhalla(context, origin, waypoints, driving) }
            if (nativeResult != null && nativeResult.error == null && nativeResult.coordinates.size >= 2) {
                return nativeResult
            }
        }
        // 2) أونلاين (OSRM) مع مهلة 20 ثانية (يدعم القيادة والمشي)
        if (isNetworkAvailable(context)) {
            val online = try {
                withTimeoutOrNull(20_000L) { OnlineOsrmNavigator.calculateRoute(origin, waypoints, driving) }
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
        return calculateDirectFallbackRoute(origin, waypoints, if (driving) 10.0 else 1.4)
    }

    private fun tryRouteWithNativeValhalla(
        context: Context,
        origin: NavWaypoint,
        waypoints: List<NavWaypoint>,
        driving: Boolean
    ): NavRouteResult? {
        return try {
            val valhallaDir = ValhallaTileDownloader.getValhallaDir(context)
            val tilesFile = ValhallaTileDownloader.resolveTilesFile(valhallaDir)
            val configFile = File(valhallaDir, "valhalla.json")
            val needsRebuild = !configFile.exists() ||
                !tilesFile.exists() ||
                !ValhallaTileDownloader.isValhallaConfigValid(context)
            if (needsRebuild) {
                try { cachedEngine?.close() } catch (_: Throwable) {}
                cachedEngine = null
                cachedConfigPath = null
                ValhallaTileDownloader.createValhallaConfig(context)
            }
            if (!configFile.exists()) {
                NativeValhallaStatus.lastError = "valhalla.json غير موجود"
                return null
            }

            val requestJson = formatValhallaRequest(origin, waypoints, driving, snapRadiusMeters = 50)

            // [إصلاح 20] استخدام نسخة محرك Valhalla واحدة محتفظ بها لتسريع الملاحة 10 أضعاف
            val responseJson = try {
                engineFor(configFile.absolutePath).routeRaw(requestJson)
            } catch (t: Throwable) {
                val msg = ((t as? java.lang.reflect.InvocationTargetException)?.targetException ?: t).message ?: ""
                when {
                    msg.contains("No such node", ignoreCase = true) ||
                        msg.contains("Exceeded max radius", ignoreCase = true) -> {
                        // إصلاح فوري لملف الإعدادات وإعادة تهيئة المحرك مرة واحدة
                        try { cachedEngine?.close() } catch (_: Throwable) {}
                        cachedEngine = null
                        cachedConfigPath = null
                        ValhallaTileDownloader.createValhallaConfig(context)
                        engineFor(configFile.absolutePath).routeRaw(requestJson)
                    }
                    msg.contains("No suitable edges", ignoreCase = true) || msg.contains("171") -> {
                        // إعادة المحاولة بنطاق التقاط طريق أوسع للنقاط البعيدة عن الطريق المرسوم
                        val wideRequest = formatValhallaRequest(origin, waypoints, driving, snapRadiusMeters = 800)
                        engineFor(configFile.absolutePath).routeRaw(wideRequest)
                    }
                    else -> {
                        try { cachedEngine?.close() } catch (_: Throwable) {}
                        cachedEngine = null
                        cachedConfigPath = null
                        throw t
                    }
                }
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
            AppErrorLogManager.logApiError("Valhalla", "فشل المحرك الأصلي: ${cause.javaClass.simpleName}", cause)
            null
        }
    }

    private fun formatValhallaRequest(
        origin: NavWaypoint,
        waypoints: List<NavWaypoint>,
        driving: Boolean,
        snapRadiusMeters: Int = 50
    ): String {
        val locations = (listOf(origin) + waypoints).map { wp ->
            org.json.JSONObject().apply {
                put("lat", wp.latitude)
                put("lon", wp.longitude)
                put("type", "break")
                put("name", wp.name)
                put("radius", snapRadiusMeters)
            }
        }
        
        return org.json.JSONObject().apply {
            put("locations", org.json.JSONArray(locations))
            put("costing", if (driving) "auto" else "pedestrian")
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
            val isLastLeg = i == legsArr.length() - 1
            // تجنّب تكرار نقطة نهاية الساق السابقة (هي نفسها بداية هذه الساق)
            val toAdd = if (i > 0 && legCoords.isNotEmpty()) legCoords.drop(1) else legCoords
            val currentCoordOffset = allCoords.size - (if (i > 0) 1 else 0)
            allCoords.addAll(toAdd)
            
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
                    val roundaboutExit = if (manObj.has("roundabout_exit_count")) manObj.optInt("roundabout_exit_count") else null

                    legSteps.add(NavStep(
                        instruction = ArabicInstructionFormatter.format(
                            mType,
                            streetName,
                            manDist,
                            waypoints.getOrNull(i)?.name ?: "الوجهة",
                            roundaboutExit = roundaboutExit,
                            isIntermediateStop = !isLastLeg
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
        waypoints: List<NavWaypoint>,
        speedMps: Double = 10.0
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
                    durationSeconds = (directDist / speedMps).coerceAtLeast(30.0),
                    maneuverType = if (i == allWaypoints.size - 2) ManeuverType.DESTINATION else ManeuverType.STRAIGHT
                )
            )

            legs.add(NavLeg(
                fromWaypointId = from.id,
                toWaypointId = to.id,
                distanceMeters = directDist,
                durationSeconds = (directDist / speedMps).coerceAtLeast(30.0),
                steps = legSteps
            ))
        }

        return NavRouteResult(
            totalDistanceMeters = totalDistance,
            totalDurationSeconds = (totalDistance / speedMps).coerceAtLeast(60.0),
            coordinates = coordsList,
            legs = legs,
            waypoints = allWaypoints,
            isOffline = true,
            engineSource = "تقريبي - مسافة مباشرة",
            error = NavError.RouteNotFound
        )
    }

    /**
     * [إصلاح ملاحة نهائي] البحث الشامل عن الأماكن والشوارع والمتاجر (أونلاين وأوفلاين)
     */
    suspend fun searchAddressAndPlaces(
        query: String,
        nearbyProviders: List<com.example.data.ProviderEntity> = emptyList(),
        nearbyStores: List<com.example.data.StoreEntity> = emptyList(),
        userLat: Double = 0.0,
        userLng: Double = 0.0,
        context: Context? = null,
        nearbyProperties: List<com.example.data.PropertyEntity> = emptyList(),
        biasCenter: Pair<Double, Double>? = null
    ): List<SearchAddressResult> = withContext(Dispatchers.IO) {
        lastSearchError = null
        val clean = query.trim()
        if (clean.length < 2) return@withContext emptyList()
        val nq = normalizeArabic(clean)
        val hasUser = !(userLat == 0.0 && userLng == 0.0)
        val hasCenter = biasCenter != null && LandmarkAddress.isValid(biasCenter.first, biasCenter.second)
        val hasRef = hasUser || hasCenter
        val refLat = when {
            hasUser -> userLat
            hasCenter -> biasCenter!!.first
            else -> 15.3694
        }
        val refLng = when {
            hasUser -> userLng
            hasCenter -> biasCenter!!.second
            else -> 44.1910
        }

        // 1) محلي أولاً: العناوين المحفوظة + الفنيون والمتاجر والعقارات + القاموس الجغرافي اليمني + الكاش الدائم
        val local = mutableListOf<SearchAddressResult>()
        val expansions = DialectSynonyms.expand(clean).ifEmpty { listOf(nq) }
        val tokens = nq.split(" ").filter { it.length >= 2 }
        fun hit(text: String): Boolean {
            val t = normalizeArabic(text)
            if (t.isBlank()) return false
            return expansions.any { t.contains(it) } ||
                (tokens.isNotEmpty() && tokens.all { t.contains(it) })
        }

        if (context != null) {
            runCatching {
                LandmarkAddressStore(context.applicationContext).getAll()
                    .filter { hit(it.label) || hit(it.landmarkName) || hit(it.description) }
                    .take(6)
                    .forEach { addr ->
                        local.add(
                            SearchAddressResult(
                                displayName = "${addr.label} — ${addr.describe()}",
                                shortName = addr.label,
                                latitude = addr.pointLat,
                                longitude = addr.pointLng,
                                type = "saved_landmark",
                                icon = "🏠"
                            )
                        )
                    }
            }
        }

        nearbyProviders.filter { it.latitude != 0.0 && it.longitude != 0.0 &&
            (hit(it.name) || hit(it.area) || hit(it.profession) || hit(it.specialization)) }
            .take(8).forEach { p -> local.add(SearchAddressResult("${p.name} - ${p.area}", p.name, p.latitude, p.longitude, "provider", "👷")) }
        nearbyStores.filter { it.latitude != 0.0 && it.longitude != 0.0 &&
            (hit(it.name) || hit(it.cityId) || hit(it.localNeighborhood) || hit(it.categoryId) || hit(it.description)) }
            .take(8).forEach { s ->
                val loc = s.localNeighborhood.ifBlank { s.cityId }
                local.add(SearchAddressResult("${s.name} - $loc", s.name, s.latitude, s.longitude, "store", "🏪"))
            }
        nearbyProperties.filter { !it.isDeleted && it.latitude != 0.0 && it.longitude != 0.0 &&
            (hit(it.title) || hit(it.cityId) || hit(it.localNeighborhood) || hit(it.propertyType) || hit(it.description)) }
            .take(8).forEach { pr ->
                val loc = pr.localNeighborhood.ifBlank { pr.cityId }
                local.add(SearchAddressResult("${pr.title} - $loc", pr.title, pr.latitude, pr.longitude, "property", "🏠"))
            }

        // القاموس الجغرافي اليمني الشامل (مدن، شوارع، جولات، صالات، مساجد، مدارس، مستشفيات، جامعات، مطارات)
        local.addAll(OfflineYemenGazetteer.search(clean, refLat, refLng, limit = 60))

        // المعالم المخصصة المضافة يدوياً + كاش نتائج البحث السابقة المحفوظ على القرص
        if (context != null) {
            local.addAll(CustomLandmarkStore.search(context.applicationContext, clean))
            local.addAll(OfflineYemenGazetteer.searchCachedOnlineResults(context.applicationContext, clean))
        }

        // 2) بحث أونلاين متكامل (Nominatim + Photon + Overpass للمساجد والمدارس وصالات الأفراح والشوارع) بدون قيد local.size < 6
        val onlineAvailable = context == null || isNetworkAvailable(context)
        val cacheKey = String.format(Locale.US, "%s_%.2f_%.2f", nq, refLat, refLng)
        val remote: List<SearchAddressResult> = if (onlineAvailable && clean.length >= 2) {
            searchMutex.withLock {
                searchCache[cacheKey]?.let { return@withLock it }
                val fetched = coroutineScope {
                    val wait = 900L - (System.currentTimeMillis() - lastNominatimAt)
                    val nomDef = async {
                        withTimeoutOrNull(4_000L) {
                            if (wait > 0 && local.isEmpty()) delay(wait)
                            fetchNominatim(clean, refLat, refLng, hasRef)
                        } ?: emptyList()
                    }
                    val photonDef = async {
                        withTimeoutOrNull(4_000L) {
                            fetchPhoton(clean, refLat, refLng, hasRef)
                        } ?: emptyList()
                    }
                    val overpassDef = async {
                        withTimeoutOrNull(4_500L) {
                            LandmarkFinder.searchByNameOrCategory(clean, refLat, refLng).map { c ->
                                SearchAddressResult(
                                    displayName = "${c.name} (${c.kind})",
                                    shortName = c.name,
                                    latitude = c.lat,
                                    longitude = c.lng,
                                    type = "overpass",
                                    icon = iconForKind(c.kind)
                                )
                            }
                        } ?: emptyList()
                    }
                    val nomList = nomDef.await().orEmpty()
                    lastNominatimAt = System.currentTimeMillis()
                    (overpassDef.await() + photonDef.await() + nomList)
                }
                if (fetched.isNotEmpty()) {
                    searchCache[cacheKey] = fetched
                    if (context != null) OfflineYemenGazetteer.cacheOnlineResults(context.applicationContext, fetched)
                }
                fetched
            }
        } else {
            emptyList()
        }

        val explicitOtherCity = listOf(
            "صنعاء", "عدن", "تعز", "الحديده", "اب", "المكلا", "حضرموت", "مارب",
            "ذمار", "سيئون", "صعده", "حجه", "شبوه", "المهره", "ابين", "لحج",
            "الضالع", "البيضاء", "عمران", "المحويت", "ريمه", "الجوف", "سقطري"
        ).any { nq.contains(it) }

        fun relevanceScore(r: SearchAddressResult): Int {
            val sn = normalizeArabic(r.shortName)
            val dn = normalizeArabic(r.displayName)
            var s = when {
                sn == nq -> 120
                sn.startsWith(nq) -> 105
                sn.contains(nq) -> 95
                expansions.any { sn.contains(it) } -> 85
                dn.contains(nq) -> 75
                tokens.isNotEmpty() && tokens.all { sn.contains(it) || dn.contains(it) } -> 70
                expansions.any { dn.contains(it) } -> 60
                else -> 40
            }
            if (r.type in setOf("saved_landmark", "provider", "store", "property")) s += 10
            if (hasRef && !explicitOtherCity) {
                val distKm = HaversinePrecisionNavigator.calculateDistance(refLat, refLng, r.latitude, r.longitude) / 1000.0
                s += when {
                    distKm <= 10.0 -> 38
                    distKm <= 30.0 -> 24
                    distKm <= 75.0 -> 10
                    distKm > 150.0 -> -35
                    else -> 0
                }
            }
            return s
        }

        val combined = (local + remote)
            .distinctBy { "${normalizeArabic(it.shortName).take(20)}_${String.format(Locale.US, "%.4f_%.4f", it.latitude, it.longitude)}" }
            .sortedWith(
                compareByDescending<SearchAddressResult> { relevanceScore(it) }
                    .thenBy { HaversinePrecisionNavigator.calculateDistance(refLat, refLng, it.latitude, it.longitude) }
            )
            .take(60)

        if (combined.isNotEmpty()) {
            lastSearchError = null
        } else if (!onlineAvailable) {
            lastSearchError = "لا توجد نتائج محلية مطابقة لـ «$clean» — جرب اسم الشارع الرئيسي أو المدينة أو اختر النقطة من الخريطة"
        }
        combined
    }

    private fun iconForKind(kind: String): String = when {
        kind.contains("مسجد") || kind.contains("جامع") -> "🕌"
        kind.contains("صالة") || kind.contains("قاعة") || kind.contains("أفراح") -> "💒"
        kind.contains("مدرسة") || kind.contains("ثانوية") -> "🏫"
        kind.contains("جامعة") || kind.contains("معهد") -> "🎓"
        kind.contains("مستشفى") || kind.contains("عيادة") -> "🏥"
        kind.contains("صيدلية") -> "💊"
        kind.contains("مطعم") -> "🍴"
        kind.contains("كافيه") -> "☕"
        kind.contains("شارع") -> "🛣️"
        kind.contains("جولة") -> "🔄"
        kind.contains("فندق") -> "🏨"
        kind.contains("سوق") -> "🛒"
        kind.contains("بنك") -> "🏦"
        kind.contains("وقود") -> "⛽"
        kind.contains("حي") -> "🏘️"
        else -> "📍"
    }

    private fun iconForOsmKeyVal(osmKey: String, osmVal: String): String = when {
        osmVal == "place_of_worship" -> "🕌"
        osmVal in listOf("events_venue", "community_centre", "conference_centre") -> "💒"
        osmVal in listOf("school", "kindergarten") -> "🏫"
        osmVal in listOf("university", "college") -> "🎓"
        osmVal in listOf("hospital", "clinic") -> "🏥"
        osmVal == "pharmacy" -> "💊"
        osmVal in listOf("restaurant", "fast_food") -> "🍴"
        osmVal == "cafe" -> "☕"
        osmKey == "highway" -> "🛣️"
        osmKey == "shop" || osmVal in listOf("supermarket", "mall", "marketplace") -> "🛒"
        osmVal in listOf("hotel", "guest_house", "motel") -> "🏨"
        osmKey == "building" || osmVal in listOf("residential", "house", "apartments") -> "🏠"
        else -> "📍"
    }

    private fun fetchPhoton(q: String, lat: Double, lng: Double, hasBias: Boolean): List<SearchAddressResult> {
        var c: HttpURLConnection? = null
        return try {
            val encoded = URLEncoder.encode(q, "UTF-8")
            val bias = if (hasBias) "&lat=$lat&lon=$lng" else ""
            val url = URL("https://photon.komoot.io/api/?q=$encoded&limit=20$bias")
            c = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 2_500; readTimeout = 3_500; requestMethod = "GET"
                setRequestProperty("User-Agent", "AlYemenServicesApp/1.0 (Android; Photon)")
            }
            if (c.responseCode != 200) return emptyList()
            val root = JSONObject(c.inputStream.bufferedReader().use { it.readText() })
            val features = root.optJSONArray("features") ?: return emptyList()
            val out = mutableListOf<SearchAddressResult>()
            for (i in 0 until features.length()) {
                val f = features.optJSONObject(i) ?: continue
                val coords = f.optJSONObject("geometry")?.optJSONArray("coordinates") ?: continue
                if (coords.length() < 2) continue
                val lo = coords.optDouble(0, Double.NaN)
                val la = coords.optDouble(1, Double.NaN)
                if (la.isNaN() || lo.isNaN()) continue
                // نطاق اليمن الجغرافي التقريبي لتفادي نتائج من دول أخرى
                if (la !in 11.8..19.2 || lo !in 41.5..55.0) continue
                val props = f.optJSONObject("properties") ?: JSONObject()
                val name = props.optString("name").ifBlank { props.optString("street") }.ifBlank { continue }
                val city = props.optString("city").ifBlank { props.optString("state") }
                val district = props.optString("district").ifBlank { props.optString("suburb") }
                val parts = listOf(name, district, city).filter { it.isNotBlank() }.distinct()
                val osmKey = props.optString("osm_key")
                val osmVal = props.optString("osm_value")
                out.add(
                    SearchAddressResult(
                        displayName = parts.joinToString(" — "),
                        shortName = name,
                        latitude = la,
                        longitude = lo,
                        type = osmVal.ifBlank { osmKey.ifBlank { "address" } },
                        icon = iconForOsmKeyVal(osmKey, osmVal)
                    )
                )
            }
            out
        } catch (_: Throwable) {
            emptyList()
        } finally {
            c?.disconnect()
        }
    }

    /** null = فشل (يُسجَّل السبب في lastSearchError)، قائمة فارغة = لا نتائج. */
    private fun fetchNominatim(q: String, lat: Double, lng: Double, hasUser: Boolean): List<SearchAddressResult>? {
        var c: HttpURLConnection? = null
        return try {
            val encoded = URLEncoder.encode(q, "UTF-8")
            val viewbox = if (hasUser) "&viewbox=${lng - 1.5},${lat + 1.5},${lng + 1.5},${lat - 1.5}&bounded=0" else ""
            val url = URL("${NavConfig.NOMINATIM_BASE_URL}?format=jsonv2&q=$encoded&countrycodes=ye&accept-language=ar&limit=25$viewbox")
            c = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 2_500; readTimeout = 3_500; requestMethod = "GET"
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
                        val cls = o.optString("class")
                        val typ = o.optString("type")
                        out.add(
                            SearchAddressResult(
                                displayName = display,
                                shortName = display.split(",").first().trim(),
                                latitude = la,
                                longitude = lo,
                                type = typ.ifBlank { "address" },
                                icon = iconForOsmKeyVal(cls, typ)
                            )
                        )
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
