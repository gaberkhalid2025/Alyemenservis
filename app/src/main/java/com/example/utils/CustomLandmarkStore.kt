package com.example.utils

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/**
 * 🕌 CustomLandmarkStore
 * مخزن محلي للمعالم المخصصة التي يضيفها المستخدم بنفسه (فندق، جولة، شارع، مسجد، بقالة...)
 * في المناطق غير المسماة على الخريطة، لتظهر له مستقبلاً في البحث وفي اقتراحات «أقرب معلم».
 */
data class CustomLandmark(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val kind: String = "معلم",
    val latitude: Double,
    val longitude: Double,
    val createdAt: Long = System.currentTimeMillis()
)

object CustomLandmarkStore {
    private const val FILE_NAME = "custom_landmarks.json"
    private val lock = Any()

    fun getAll(context: Context): List<CustomLandmark> {
        synchronized(lock) {
            val file = File(context.applicationContext.filesDir, FILE_NAME)
            if (!file.exists()) return emptyList()
            return try {
                val arr = JSONArray(file.readText())
                val out = ArrayList<CustomLandmark>(arr.length())
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val name = o.optString("name").trim()
                    val lat = o.optDouble("latitude", 0.0)
                    val lng = o.optDouble("longitude", 0.0)
                    if (name.isBlank() || !LandmarkAddress.isValid(lat, lng)) continue
                    out.add(
                        CustomLandmark(
                            id = o.optString("id").ifBlank { UUID.randomUUID().toString() },
                            name = name,
                            kind = o.optString("kind").ifBlank { "معلم" },
                            latitude = lat,
                            longitude = lng,
                            createdAt = o.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
                out
            } catch (_: Throwable) {
                emptyList()
            }
        }
    }

    fun add(context: Context, name: String, kind: String, latitude: Double, longitude: Double): CustomLandmark? {
        val cleanName = name.trim()
        if (cleanName.length < 2 || !LandmarkAddress.isValid(latitude, longitude)) return null
        val item = CustomLandmark(
            name = cleanName,
            kind = kind.trim().ifBlank { "معلم" },
            latitude = latitude,
            longitude = longitude
        )
        synchronized(lock) {
            val file = File(context.applicationContext.filesDir, FILE_NAME)
            val existing = getAll(context).filterNot {
                it.name == cleanName &&
                    HaversinePrecisionNavigator.calculateDistance(it.latitude, it.longitude, latitude, longitude) < 40.0
            }.toMutableList()
            existing.add(0, item)
            val arr = JSONArray()
            existing.take(200).forEach { lm ->
                arr.put(
                    JSONObject()
                        .put("id", lm.id)
                        .put("name", lm.name)
                        .put("kind", lm.kind)
                        .put("latitude", lm.latitude)
                        .put("longitude", lm.longitude)
                        .put("createdAt", lm.createdAt)
                )
            }
            file.writeText(arr.toString())
        }
        return item
    }

    fun nearby(context: Context, lat: Double, lng: Double, maxDistanceM: Double = 15_000.0): List<LandmarkCandidate> {
        if (!LandmarkAddress.isValid(lat, lng)) return emptyList()
        return getAll(context).mapNotNull { lm ->
            val d = HaversinePrecisionNavigator.calculateDistance(lat, lng, lm.latitude, lm.longitude)
            if (d <= maxDistanceM) LandmarkCandidate(lm.name, lm.latitude, lm.longitude, lm.kind, d) else null
        }.sortedBy { it.distanceM }
    }

    fun search(context: Context, query: String): List<RealValhallaNavigator.SearchAddressResult> {
        val nq = OfflineYemenGazetteer.let { q -> query.trim() }
        if (nq.length < 2) return emptyList()
        return getAll(context).filter {
            it.name.contains(nq, ignoreCase = true) || it.kind.contains(nq, ignoreCase = true)
        }.take(10).map { lm ->
            RealValhallaNavigator.SearchAddressResult(
                displayName = "${lm.name} (${lm.kind} · معلم مضاف يدوياً)",
                shortName = lm.name,
                latitude = lm.latitude,
                longitude = lm.longitude,
                type = "custom_landmark",
                icon = "📌"
            )
        }
    }
}
