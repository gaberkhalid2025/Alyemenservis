package com.example.ui.screens.map.utils

/**
 * 🏙️ CityAliases
 * Map of Yemeni cities and governorates to their aliases, area names, and system codes.
 */
object CityAliases {
    val ALIASES: Map<String, List<String>> = mapOf(
        "صنعاء" to listOf("صنعاء", "sanaa", "ye_san", "1", "حدة", "السبعين", "التحرير", "الصافية", "شعوب"),
        "عدن" to listOf("عدن", "aden", "ye_ade", "2", "كريتر", "المعلا", "المنصورة", "الشيخ عثمان", "خور مكسر", "التواهي"),
        "تعز" to listOf("تعز", "taiz", "ye_tai", "3", "الحوبان", "صالة", "المظفر", "القاهرة"),
        "الحديدة" to listOf("الحديدة", "hodeidah", "ye_hud", "الحالي", "الميناء", "الحوك"),
        "إب" to listOf("إب", "ibb", "ye_ibb", "يريم", "العدين", "جبلة"),
        "حضرموت" to listOf("حضرموت", "المكلا", "mukalla", "سيئون", "ye_had", "الشحر"),
        "المكلا" to listOf("حضرموت", "المكلا", "mukalla", "سيئون", "ye_had", "الشحر"),
        "مأرب" to listOf("مأرب", "marib", "ye_mar", "المدينة", "الوادي"),
        "ذمار" to listOf("ذمار", "dhamar", "ye_dha", "عنس")
    )
}
