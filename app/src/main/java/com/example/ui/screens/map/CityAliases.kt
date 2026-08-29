package com.example.ui.screens.map

/**
 * 🏙️ CityAliases - Map of Yemeni cities and their associated neighborhoods and regional aliases
 */
object CityAliases {
    val ALIASES = mapOf(
        "صنعاء" to listOf("صنعاء", "sanaa", "ye_san", "1", "حدة", "السبعين", "التحرير", "الصافية", "معين"),
        "عدن" to listOf("عدن", "aden", "ye_ade", "2", "كريتر", "المعلا", "المنصورة", "الشيخ عثمان", "خور مكسر"),
        "تعز" to listOf("تعز", "taiz", "ye_tai", "3", "الحوبان", "صالة", "المظفر"),
        "الحديدة" to listOf("الحديدة", "hodeidah", "ye_hud", "الحالي", "الميناء", "الحوك"),
        "إب" to listOf("إب", "ibb", "ye_ibb", "يريم", "العدين", "المشنة"),
        "حضرموت" to listOf("حضرموت", "المكلا", "mukalla", "سيئون", "ye_had", "تريم"),
        "المكلا" to listOf("حضرموت", "المكلا", "mukalla", "سيئون", "ye_had", "تريم"),
        "مأرب" to listOf("مأرب", "marib", "ye_mar"),
        "ذمار" to listOf("ذمار", "dhamar", "ye_dha")
    )
}
