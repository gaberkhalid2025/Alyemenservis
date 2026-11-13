package com.example.utils

import com.example.data.ProviderEntity
import com.example.ui.screens.map.MapZone
import com.example.ui.screens.map.utils.MapDistanceCalculator

data class HazardZonePoint(
    val id: String,
    val title: String,
    val lat: Double,
    val lng: Double,
    val radiusM: Int = 450
)

/**
 * ⚠️ تنبيهات وعورة الطريق والنقاط الحرجة قبل الدخول إليها (100% أوفلاين).
 */
object HazardAlerts {
    /** نقاط جبلية ومنحدرات وممرات سيول معروفة في شبكة طرق اليمن (بيانات مفتوحة محلية). */
    val knownMountainAndWadiHazards = listOf(
        HazardZonePoint("yaslah", "نقيل يسلح (منحدرات ومنعطفات حادة)", 15.1060, 44.2740, 650),
        HazardZonePoint("sumarah", "نقيل سمارة (طريق جبلي شديد الانحدار)", 14.1550, 44.2870, 700),
        HazardZonePoint("manakha", "منحدرات مناخة - الحديدة الجبلية", 15.0720, 43.7390, 600),
        HazardZonePoint("thirah", "عقبة ثرة (منعطفات جبلية حادة)", 13.8610, 45.8020, 650),
        HazardZonePoint("dhabah", "نقيل ضبة / طريق تعز - عدن الجبلي", 13.4890, 44.3010, 550),
        HazardZonePoint("al_mahweet", "طريق شبام كوكبان - المحويت الجبلي", 15.4980, 43.6580, 600),
        HazardZonePoint("abdullah_gharib", "عقبة عبدالله غريب - حضرموت", 14.9220, 49.0620, 600),
        HazardZonePoint("al_arqoub", "عقبة العرقوب - أبين", 13.5240, 46.1210, 600)
    )

    fun roadDifficultyZones(providers: List<ProviderEntity> = emptyList()): List<MapZone> {
        val list = knownMountainAndWadiHazards.map {
            MapZone(lat = it.lat, lng = it.lng, radiusM = it.radiusM, colorHex = "#EF4444")
        }.toMutableList()

        // إبراز النقاط المعزولة خارج التجمعات الخدمية كتنبيه احترازي إضافي للسائقين
        val valid = providers.filter { LandmarkAddress.isValid(it.latitude, it.longitude) }
        valid.forEach { p ->
            val neighbors = valid.count { other ->
                other.id != p.id && MapDistanceCalculator.calculateDistanceMeters(
                    p.latitude, p.longitude, other.latitude, other.longitude
                ) < 4000.0
            }
            if (neighbors == 0 && list.none {
                    MapDistanceCalculator.calculateDistanceMeters(p.latitude, p.longitude, it.lat, it.lng) < 800.0
                }
            ) {
                list.add(MapZone(lat = p.latitude, lng = p.longitude, radiusM = 350, colorHex = "#F59E0B"))
            }
        }
        return list
    }

    fun nearest(
        lat: Double,
        lng: Double,
        zones: List<MapZone>,
        thresholdMeters: Double = 300.0
    ): MapZone? {
        if (!LandmarkAddress.isValid(lat, lng) || zones.isEmpty()) return null
        var best: MapZone? = null
        var bestDist = Double.MAX_VALUE
        for (z in zones) {
            val d = HaversinePrecisionNavigator.calculateDistance(lat, lng, z.lat, z.lng)
            if (d <= thresholdMeters && d < bestDist) {
                bestDist = d
                best = z
            }
        }
        return best
    }

    fun describeZone(zone: MapZone): String {
        val known = knownMountainAndWadiHazards.minByOrNull {
            HaversinePrecisionNavigator.calculateDistance(zone.lat, zone.lng, it.lat, it.lng)
        }
        return if (known != null &&
            HaversinePrecisionNavigator.calculateDistance(zone.lat, zone.lng, known.lat, known.lng) < 1200.0
        ) {
            "تنبيه: تقترب من ${known.title}، خفّف السرعة"
        } else {
            "تنبيه: تقترب من منطقة طريق وعر أو ضعيف الخدمات، خفّف السرعة"
        }
    }
}
