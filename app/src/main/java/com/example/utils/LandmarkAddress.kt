package com.example.utils

import org.json.JSONObject
import java.util.Locale
import java.util.UUID
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * عنوان مرئي: معلم مرجعي + نقطة المستخدم + وصف نصي.
 * لا يعتمد على GPS؛ النقطة يحددها المستخدم على الخريطة، والإزاحة تُحسب منها.
 */
data class LandmarkAddress(
    val id: String = UUID.randomUUID().toString(),
    val label: String,                 // «منزلي»، «عملي»
    val landmarkName: String,          // «مسجد النور»
    val landmarkLat: Double,
    val landmarkLng: Double,
    val pointLat: Double,              // موقع المنزل كما حدده المستخدم
    val pointLng: Double,
    val description: String,           // «البوابة الحمراء» (إلزامي)
    val createdAt: Long = System.currentTimeMillis()
) {
    val northMeters: Double get() = (pointLat - landmarkLat) * METERS_PER_DEG
    val eastMeters: Double get() = (pointLng - landmarkLng) * METERS_PER_DEG * cos(Math.toRadians(landmarkLat))
    val distanceMeters: Double get() = sqrt(northMeters * northMeters + eastMeters * eastMeters)

    /** أحد الاتجاهات الثمانية بالعربية (من المعلم إلى النقطة). */
    fun directionArabic(): String {
        val bearing = (Math.toDegrees(atan2(eastMeters, northMeters)) + 360.0) % 360.0
        return DIRECTIONS[(((bearing + 22.5) / 45.0).toInt()) % 8]
    }

    /** «على بعد 80 م شمال مسجد النور، البوابة الحمراء» */
    fun describe(): String {
        val d = distanceMeters
        val where = if (d < 15.0) "عند $landmarkName" else "على بعد ${roundMeters(d)} ${directionArabic()} $landmarkName"
        val desc = description.trim()
        return if (desc.isEmpty()) where else "$where، $desc"
    }

    /** null = صالح، وإلا رسالة الخطأ بالعربية. */
    fun validate(): String? = when {
        label.isBlank() -> "اسم العنوان مطلوب"
        landmarkName.isBlank() -> "اسم المعلم مطلوب"
        !isValid(landmarkLat, landmarkLng) -> "موقع المعلم غير صالح"
        !isValid(pointLat, pointLng) -> "موقع النقطة غير صالح"
        description.trim().length < 3 -> "الوصف النصي مطلوب (مثل: البوابة الحمراء، الدور الثاني)"
        distanceMeters > MAX_OFFSET_METERS -> "النقطة بعيدة عن المعلم أكثر من ${MAX_OFFSET_METERS.toInt()} م؛ اختر معلماً أقرب"
        else -> null
    }

    fun toJson(): JSONObject = JSONObject()
        .put("id", id).put("label", label).put("landmarkName", landmarkName)
        .put("landmarkLat", landmarkLat).put("landmarkLng", landmarkLng)
        .put("pointLat", pointLat).put("pointLng", pointLng)
        .put("description", description).put("createdAt", createdAt)

    /** للإرسال مع الحجز (Firestore): يحمل الإحداثيات والوصف الجاهز للعرض. */
    fun toMap(): Map<String, Any> = mapOf(
        "id" to id, "label" to label, "landmarkName" to landmarkName,
        "landmarkLat" to landmarkLat, "landmarkLng" to landmarkLng,
        "pointLat" to pointLat, "pointLng" to pointLng,
        "description" to description.trim(),
        "northM" to northMeters.roundToInt(), "eastM" to eastMeters.roundToInt(),
        "text" to describe(), "createdAt" to createdAt
    )

    companion object {
        const val METERS_PER_DEG = 111_195.0
        const val MAX_OFFSET_METERS = 1500.0
        private val DIRECTIONS = listOf("شمال", "شمال شرق", "شرق", "جنوب شرق", "جنوب", "جنوب غرب", "غرب", "شمال غرب")

        fun isValid(lat: Double, lng: Double) =
            !lat.isNaN() && !lng.isNaN() && lat in -90.0..90.0 && lng in -180.0..180.0 && !(lat == 0.0 && lng == 0.0)

        fun roundMeters(d: Double): String = when {
            d < 100.0 -> "${((d / 5.0).roundToInt() * 5).coerceAtLeast(5)} م"
            d < 1000.0 -> "${(d / 10.0).roundToInt() * 10} م"
            else -> String.format(Locale.US, "%.1f", d / 1000.0).removeSuffix(".0") + " كم"
        }

        fun fromJson(o: JSONObject): LandmarkAddress? = try {
            LandmarkAddress(
                id = o.optString("id").ifBlank { UUID.randomUUID().toString() },
                label = o.getString("label"), landmarkName = o.getString("landmarkName"),
                landmarkLat = o.getDouble("landmarkLat"), landmarkLng = o.getDouble("landmarkLng"),
                pointLat = o.getDouble("pointLat"), pointLng = o.getDouble("pointLng"),
                description = o.optString("description"), createdAt = o.optLong("createdAt", System.currentTimeMillis())
            )
        } catch (e: Exception) { null }

        /** قراءة عنوان وصل من Firestore (حقل landmarkAddress في الحجز). */
        fun fromMap(m: Map<String, Any?>): LandmarkAddress? {
            fun d(k: String) = (m[k] as? Number)?.toDouble()
            return try {
                LandmarkAddress(
                    id = (m["id"] as? String) ?: UUID.randomUUID().toString(),
                    label = (m["label"] as? String) ?: "العنوان",
                    landmarkName = m["landmarkName"] as? String ?: return null,
                    landmarkLat = d("landmarkLat") ?: return null, landmarkLng = d("landmarkLng") ?: return null,
                    pointLat = d("pointLat") ?: return null, pointLng = d("pointLng") ?: return null,
                    description = (m["description"] as? String).orEmpty(),
                    createdAt = (m["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
                )
            } catch (e: Exception) { null }
        }
    }
}
