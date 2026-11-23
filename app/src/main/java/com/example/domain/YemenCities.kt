package com.example.domain

object YemenCities {
    val defaultCity: String = "صنعاء"

    val list: List<String> = listOf(
        "صنعاء",
        "أمانة العاصمة",
        "عدن",
        "تعز",
        "الحديدة",
        "إب",
        "حضرموت",
        "مأرب",
        "ذمار",
        "صعدة",
        "حجة",
        "عمران",
        "البيضاء",
        "لحج",
        "أبين",
        "الضالع",
        "شبوة",
        "المهرة",
        "سقطرى",
        "ريمة",
        "الجوف",
        "المحويت"
    )

    val MAIN_CITIES: List<String> = list

    val listWithAll: List<String> = listOf("الكل") + list

    fun resolveCityId(areaOrCity: String): String {
        val raw = areaOrCity.trim()
        if (raw.isEmpty()) return "ye_san"
        val lower = raw.lowercase()
        return when {
            lower.contains("أمانة") || lower.contains("امانة") || lower.contains("secretariat") || lower == "ye_sana_cap" -> "ye_sana_cap"
            lower.contains("صنعاء") || lower.contains("sanaa") || lower.contains("sana'a") || lower == "ye_san" -> "ye_san"
            lower.contains("عدن") || lower.contains("aden") || lower == "ye_ade" -> "ye_ade"
            lower.contains("تعز") || lower.contains("taiz") || lower.contains("taizz") || lower == "ye_tai" -> "ye_tai"
            lower.contains("الحديدة") || lower.contains("حديدة") || lower.contains("حديده") || lower.contains("hodeidah") || lower.contains("hudaydah") || lower == "ye_hod" -> "ye_hod"
            lower.contains("إب") || lower.contains("اب") && (lower == "اب" || lower.contains("محافظة اب") || lower.contains("مدينة اب")) || lower.contains("ibb") || lower == "ye_ibb" -> "ye_ibb"
            lower.contains("ذمار") || lower.contains("dhamar") || lower == "ye_dha" -> "ye_dha"
            lower.contains("حضرموت") || lower.contains("المكلا") || lower.contains("سيئون") || lower.contains("hadram") || lower.contains("mukalla") || lower == "ye_had" -> "ye_had"
            lower.contains("مأرب") || lower.contains("مارب") || lower.contains("marib") || lower == "ye_mar" -> "ye_mar"
            lower.contains("صعدة") || lower.contains("صعده") || lower.contains("saada") || lower == "ye_saa" -> "ye_saa"
            lower.contains("حجة") || lower.contains("حجه") || lower.contains("hajjah") || lower == "ye_haj" -> "ye_haj"
            lower.contains("المهرة") || lower.contains("المهره") || lower.contains("الغيضة") || lower.contains("mahrah") || lower == "ye_mah" -> "ye_mah"
            lower.contains("سقطرى") || lower.contains("سقطره") || lower.contains("socotra") || lower == "ye_soc" -> "ye_soc"
            lower.contains("شبوة") || lower.contains("شبوه") || lower.contains("عتق") || lower.contains("shabwa") || lower == "ye_sha" -> "ye_sha"
            lower.contains("أبين") || lower.contains("ابين") || lower.contains("زنجبار") || lower.contains("abyan") || lower == "ye_aby" -> "ye_aby"
            lower.contains("البيضاء") || lower.contains("بيضاء") || lower.contains("bayda") || lower == "ye_bay" -> "ye_bay"
            lower.contains("عمران") || lower.contains("amran") || lower == "ye_amr" -> "ye_amr"
            lower.contains("ريمة") || lower.contains("ريمه") || lower.contains("raymah") || lower == "ye_ray" -> "ye_ray"
            lower.contains("الجوف") || lower.contains("جوف") || lower.contains("jawf") || lower == "ye_jaw" -> "ye_jaw"
            lower.contains("لحج") || lower.contains("lahj") || lower == "ye_lah" -> "ye_lah"
            lower.contains("الضالع") || lower.contains("ضالع") || lower.contains("dali") || lower.contains("dhale") || lower == "ye_dal" -> "ye_dal"
            lower.contains("المحويت") || lower.contains("محويت") || lower.contains("mahwit") || lower == "ye_mhw" -> "ye_mhw"
            else -> "ye_san"
        }
    }

    fun getCityNameById(cityId: String): String {
        return when (cityId.trim().lowercase()) {
            "ye_sana_cap" -> "أمانة العاصمة"
            "ye_san" -> "صنعاء"
            "ye_ade" -> "عدن"
            "ye_tai" -> "تعز"
            "ye_hod" -> "الحديدة"
            "ye_ibb" -> "إب"
            "ye_dha" -> "ذمار"
            "ye_had" -> "حضرموت"
            "ye_mar" -> "مأرب"
            "ye_saa" -> "صعدة"
            "ye_haj" -> "حجة"
            "ye_mah" -> "المهرة"
            "ye_soc" -> "سقطرى"
            "ye_sha" -> "شبوة"
            "ye_aby" -> "أبين"
            "ye_bay" -> "البيضاء"
            "ye_amr" -> "عمران"
            "ye_ray" -> "ريمة"
            "ye_jaw" -> "الجوف"
            "ye_lah" -> "لحج"
            "ye_dal" -> "الضالع"
            "ye_mhw" -> "المحويت"
            else -> defaultCity
        }
    }
}
