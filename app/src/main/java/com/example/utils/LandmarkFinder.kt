package com.example.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

data class LandmarkCandidate(val name: String, val lat: Double, val lng: Double, val kind: String, val distanceM: Double)

object LandmarkFinder {
    @Volatile var lastError: String? = null
    private val cache = ConcurrentHashMap<String, Pair<Long, List<LandmarkCandidate>>>()
    private val mutex = Mutex()
    private const val ENDPOINT = "https://overpass-api.de/api/interpreter"     // يمكن تغييره لخادم Overpass آخر مجاني

    suspend fun nearby(lat: Double, lng: Double, radiusM: Int = 1200): List<LandmarkCandidate> = withContext(Dispatchers.IO) {
        lastError = null
        if (!LandmarkAddress.isValid(lat, lng)) return@withContext emptyList()
        val key = String.format(Locale.US, "%.3f_%.3f_%d", lat, lng, radiusM)
        cache[key]?.let { (t, v) -> if (System.currentTimeMillis() - t < 5 * 60_000L) return@withContext v }

        val la = String.format(Locale.US, "%.6f", lat)
        val lo = String.format(Locale.US, "%.6f", lng)
        val q = "[out:json][timeout:15];(" +
            "nwr(around:$radiusM,$la,$lo)[\"amenity\"~\"^(place_of_worship|school|marketplace|hospital|pharmacy|bank|fuel|restaurant|cafe)\$\"][\"name\"];" +
            "nwr(around:$radiusM,$la,$lo)[\"tourism\"~\"^(hotel|guest_house|motel)\$\"][\"name\"];" +
            "nwr(around:$radiusM,$la,$lo)[\"junction\"=\"roundabout\"][\"name\"];" +
            "way(around:$radiusM,$la,$lo)[\"highway\"~\"^(primary|secondary|tertiary|residential|trunk)\$\"][\"name\"];" +
            ");out center 45;"

        val result = postOverpass(q, lat, lng) { tags ->
            when {
                tags.optString("tourism") in listOf("hotel", "guest_house", "motel") -> "فندق"
                tags.optString("junction") == "roundabout" -> "جولة"
                tags.has("highway") && !tags.has("amenity") -> "شارع"
                else -> when (tags.optString("amenity")) {
                    "place_of_worship" -> if (tags.optString("religion") == "muslim" || tags.optString("religion").isBlank()) "مسجد" else "دار عبادة"
                    "school" -> "مدرسة"
                    "marketplace" -> "سوق"
                    "hospital" -> "مستشفى"
                    "pharmacy" -> "صيدلية"
                    "bank" -> "بنك"
                    "fuel" -> "محطة وقود"
                    "restaurant", "cafe" -> "مطعم"
                    else -> "معلم"
                }
            }
        }
        if (result.isNotEmpty()) {
            cache[key] = System.currentTimeMillis() to result
        }
        result
    }

    /**
     * يجلب أقرب المعالم (مساجد، فنادق، شوارع، جولات، محطات) أونلاين مع دمج تلقائي للمعالم المضافة محلياً
     * وقاعدة البيانات الأوفلاين حتى لا يفشل أبداً عند انقطاع الإنترنت.
     */
    suspend fun nearbyWithOfflineFallback(
        context: android.content.Context,
        lat: Double,
        lng: Double,
        radiusM: Int = 1500
    ): List<LandmarkCandidate> = withContext(Dispatchers.IO) {
        if (!LandmarkAddress.isValid(lat, lng)) return@withContext emptyList()
        val cm = context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
        val net = cm?.activeNetwork
        val caps = cm?.getNetworkCapabilities(net)
        val hasNet = caps?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

        val remote = if (hasNet) nearby(lat, lng, radiusM) else emptyList()
        val custom = CustomLandmarkStore.nearby(context, lat, lng, maxDistanceM = 15_000.0)
        val offlineGaz = OfflineYemenGazetteer.PLACES.mapNotNull { p ->
            val d = HaversinePrecisionNavigator.calculateDistance(lat, lng, p.latitude, p.longitude)
            if (d <= 20_000.0) LandmarkCandidate(p.name, p.latitude, p.longitude, p.category, d) else null
        }

        val combined = (custom + remote + offlineGaz)
            .distinctBy { "${it.name}_${String.format(Locale.US, "%.4f", it.lat)}" }
            .sortedBy { it.distanceM }
            .take(30)

        if (combined.isNotEmpty()) lastError = null
        combined
    }

    suspend fun nearbyWalkingAid(lat: Double, lng: Double, radiusM: Int = 800): List<LandmarkCandidate> = withContext(Dispatchers.IO) {
        val la = String.format(Locale.US, "%.6f", lat)
        val lo = String.format(Locale.US, "%.6f", lng)
        val q = "[out:json][timeout:15];(" +
            "nwr(around:$radiusM,$la,$lo)[\"amenity\"~\"^(place_of_worship|drinking_water|pharmacy)\$\"];" +
            "nwr(around:$radiusM,$la,$lo)[\"shop\"~\"^(convenience|supermarket|grocery)\$\"];" +
            ");out center 40;"
        postOverpass(q, lat, lng) { tags ->
            when {
                tags.optString("amenity") == "drinking_water" -> "مصدر مياه"
                tags.optString("amenity") == "pharmacy" -> "صيدلية"
                tags.optString("amenity") == "place_of_worship" -> "مسجد"
                else -> "بقالة"
            }
        }
    }

    private suspend fun postOverpass(
        query: String,
        originLat: Double,
        originLng: Double,
        kindOf: (JSONObject) -> String
    ): List<LandmarkCandidate> = mutex.withLock {
        var c: HttpURLConnection? = null
        try {
            c = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"; doOutput = true
                connectTimeout = 10_000; readTimeout = 20_000
                setRequestProperty("User-Agent", "AlYemenServicesApp/1.0")
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            }
            c.outputStream.use { it.write(("data=" + URLEncoder.encode(query, "UTF-8")).toByteArray(Charsets.UTF_8)) }
            when (val code = c.responseCode) {
                200 -> {
                    val root = JSONObject(c.inputStream.bufferedReader().use { it.readText() })
                    val els = root.optJSONArray("elements")
                    val out = mutableListOf<LandmarkCandidate>()
                    if (els != null) for (i in 0 until els.length()) {
                        val e = els.getJSONObject(i)
                        val tags = e.optJSONObject("tags") ?: JSONObject()
                        val kind = kindOf(tags)
                        val rawName = tags.optString("name:ar").ifBlank { tags.optString("name") }
                        val name = rawName.ifBlank { kind }
                        val center = e.optJSONObject("center")
                        val la2 = if (e.has("lat")) e.optDouble("lat") else center?.optDouble("lat") ?: Double.NaN
                        val lo2 = if (e.has("lon")) e.optDouble("lon") else center?.optDouble("lon") ?: Double.NaN
                        if (!LandmarkAddress.isValid(la2, lo2)) continue
                        out.add(LandmarkCandidate(name, la2, lo2, kind, HaversinePrecisionNavigator.calculateDistance(originLat, originLng, la2, lo2)))
                    }
                    out.distinctBy { "${it.name}_${"%.4f".format(Locale.US, it.lat)}" }.sortedBy { it.distanceM }.take(25)
                }
                429, 504 -> { lastError = "خدمة المعالم مشغولة، حاول بعد قليل"; emptyList() }
                else -> { lastError = "تعذر جلب المعالم (رمز $code)"; emptyList() }
            }
        } catch (e: java.net.UnknownHostException) { lastError = "لا يوجد اتصال بالإنترنت"; emptyList() }
        catch (e: java.net.SocketTimeoutException) { lastError = "انتهت مهلة جلب المعالم"; emptyList() }
        catch (e: kotlinx.coroutines.CancellationException) { throw e }
        catch (e: Exception) { lastError = "تعذر جلب المعالم"; emptyList() }
        finally { c?.disconnect() }
    }
}
