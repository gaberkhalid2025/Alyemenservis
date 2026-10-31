package com.example.utils

/**
 * ⚙️ NavConfig
 * الإعدادات المركزية لنظام الملاحة والخرائط لليمن.
 */
object NavConfig {
    // Endpoints
    const val OSRM_BASE_URL = "https://router.project-osrm.org/route/v1/driving/"
    const val NOMINATIM_BASE_URL = "https://nominatim.openstreetmap.org/search"
    
    // Valhalla Local Config
    const val VALHALLA_DIR = "valhalla"
    const val VALHALLA_TILES_FILE = "valhalla_tiles.tar"
    const val VALHALLA_MARKER_FILE = "is_valhalla_downloaded.txt"
    
    // Thresholds
    const val OFF_ROUTE_THRESHOLD_METERS = 45.0 // المسافة التي يعتبر عندها المستخدم خارج المسار
    const val ARRIVAL_THRESHOLD_METERS = 25.0    // المسافة لاعتبار الوصول للمحطة
    const val MIN_SPEED_FOR_DIRECTION_KMH = 5.0   // الحد الأدنى للسرعة لتحديد الاتجاه
    
    // Default Yemen Location (Sana'a)
    const val DEFAULT_YEMEN_LAT = 15.3694
    const val DEFAULT_YEMEN_LNG = 44.1910
    const val EMERGENCY_NUMBER = "" // الرقم الرسمي للطوارئ إن وجد
    
    // Yemen Cities Data
    data class CityCoords(val name: String, val lat: Double, val lng: Double)
    val YEMEN_CITIES = listOf(
        CityCoords("صنعاء", 15.3694, 44.1910),
        CityCoords("عدن", 12.7855, 45.0186),
        CityCoords("تعز", 13.5783, 44.0135),
        CityCoords("الحديدة", 14.7979, 42.9530),
        CityCoords("المكلا", 14.5422, 49.1242),
        CityCoords("إب", 13.9742, 44.1725),
        CityCoords("ذمار", 14.5414, 44.4079),
        CityCoords("مأرب", 15.4600, 45.3200)
    )
}
