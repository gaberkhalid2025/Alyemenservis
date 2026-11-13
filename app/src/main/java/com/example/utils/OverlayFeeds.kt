package com.example.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class Quake(
    val lat: Double,
    val lng: Double,
    val mag: Double,
    val place: String,
    val timeMs: Long
)

/**
 * 🌋 طبقات التغذية الحية المجانية (بدون أي مفتاح API):
 * - USGS M2.5+ (آخر أسبوع)
 * - RainViewer (معطّلة افتراضياً عبر NavConfig.ENABLE_RAINVIEWER)
 */
object OverlayFeeds {
    private const val QUAKE_TTL_MS = 10 * 60_000L
    private const val RAIN_TTL_MS = 5 * 60_000L

    @Volatile private var cachedQuakes: List<Quake>? = null
    @Volatile private var cachedQuakesAt: Long = 0L

    @Volatile private var cachedRainUrl: String? = null
    @Volatile private var cachedRainAt: Long = 0L

    suspend fun quakes(forceRefresh: Boolean = false): Result<List<Quake>> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val cur = cachedQuakes
        if (!forceRefresh && cur != null && now - cachedQuakesAt < QUAKE_TTL_MS) {
            return@withContext Result.success(cur)
        }
        return@withContext try {
            val raw = httpGet(NavConfig.QUAKES_FEED_URL)
            val list = parseQuakesGeoJson(raw)
            cachedQuakes = list
            cachedQuakesAt = now
            Result.success(list)
        } catch (t: Throwable) {
            if (cur != null) Result.success(cur) else Result.failure(t)
        }
    }

    fun parseQuakesGeoJson(raw: String): List<Quake> {
        val root = JSONObject(raw)
        val features = root.optJSONArray("features") ?: return emptyList()
        val out = ArrayList<Quake>(features.length())
        for (i in 0 until features.length()) {
            val f = features.optJSONObject(i) ?: continue
            val geom = f.optJSONObject("geometry") ?: continue
            val coords = geom.optJSONArray("coordinates") ?: continue
            if (coords.length() < 2) continue
            val lng = coords.optDouble(0, Double.NaN)
            val lat = coords.optDouble(1, Double.NaN)
            if (lat.isNaN() || lng.isNaN() || !LandmarkAddress.isValid(lat, lng)) continue
            val props = f.optJSONObject("properties") ?: continue
            val mag = props.optDouble("mag", 0.0)
            val place = props.optString("place", "").ifBlank { "منطقة غير مسماة" }
            val time = props.optLong("time", 0L)
            out.add(Quake(lat = lat, lng = lng, mag = mag, place = place, timeMs = time))
        }
        return out
    }

    fun quakesToJson(list: List<Quake>): String {
        val arr = JSONArray()
        list.take(300).forEach { q ->
            arr.put(
                JSONObject()
                    .put("lat", q.lat)
                    .put("lng", q.lng)
                    .put("mag", q.mag)
                    .put("place", q.place)
                    .put("time", q.timeMs)
            )
        }
        return arr.toString()
    }

    suspend fun rainTemplate(): Result<String?> = withContext(Dispatchers.IO) {
        if (!NavConfig.ENABLE_RAINVIEWER) {
            return@withContext Result.failure(
                IllegalStateException("طبقة رادار الأمطار معطّلة افتراضياً (بدون مفاتيح مدفوعة)")
            )
        }
        val now = System.currentTimeMillis()
        val cached = cachedRainUrl
        if (cached != null && now - cachedRainAt < RAIN_TTL_MS) {
            return@withContext Result.success(cached)
        }
        return@withContext try {
            val raw = httpGet(NavConfig.RAINVIEWER_FEED_URL)
            val url = parseRainViewerTemplate(raw)
                ?: return@withContext Result.failure(IllegalStateException("لا تتوفر بيانات رادار جوي حالياً"))
            cachedRainUrl = url
            cachedRainAt = now
            Result.success(url)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    fun parseRainViewerTemplate(raw: String): String? {
        val root = JSONObject(raw)
        val host = root.optString("host").takeIf { it.startsWith("https://") } ?: return null
        val radar = root.optJSONObject("radar") ?: return null
        val past = radar.optJSONArray("past") ?: return null
        if (past.length() == 0) return null
        val latest = past.optJSONObject(past.length() - 1) ?: return null
        val path = latest.optString("path").takeIf { it.startsWith("/") } ?: return null
        return "$host$path/256/{z}/{x}/{y}/2/1_1.png"
    }

    private fun httpGet(url: String): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8000
            readTimeout = 8000
            requestMethod = "GET"
            setRequestProperty("User-Agent", "AlYemenServicesApp/1.0 (Android)")
        }
        return try {
            val code = conn.responseCode
            if (code !in 200..299) throw IllegalStateException("HTTP $code")
            conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }
}
