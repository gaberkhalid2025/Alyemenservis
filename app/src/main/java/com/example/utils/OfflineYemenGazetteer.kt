package com.example.utils

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Locale

/**
 * 🇾🇪 OfflineYemenGazetteer
 * قاعدة بيانات جغرافية محلية شاملة لأشهر المدن والشوارع والجولات والمعالم والمستشفيات والجامعات والمطارات في اليمن.
 * تعمل 100% بدون إنترنت وتدعم:
 * - تطبيع الحروف العربية ومرادفات اللهجة اليمنية
 * - تحليل الإحداثيات المباشرة (مثل: 15.3694, 44.1910)
 * - كاش محلي دائم لنتائج البحث السابقة على القرص
 */
object OfflineYemenGazetteer {

    data class OfflinePlace(
        val name: String,
        val city: String,
        val category: String,
        val icon: String,
        val latitude: Double,
        val longitude: Double,
        val aliases: List<String> = emptyList()
    )

    private const val CACHE_FILE_NAME = "offline_search_cache.json"
    private const val MAX_PERSISTENT_CACHE = 300
    private val cacheLock = Any()

    val PLACES: List<OfflinePlace> = listOf(
        // ───────── صنعاء (العاصمة والمحافظة) ─────────
        OfflinePlace("صنعاء", "صنعاء", "مدينة", "🏙️", 15.3694, 44.1910, listOf("امانة العاصمة", "العاصمة", "sanaa")),
        OfflinePlace("ميدان التحرير", "صنعاء", "معلم", "📍", 15.3547, 44.2067, listOf("التحرير", "وسط البلد")),
        OfflinePlace("باب اليمن", "صنعاء", "معلم", "🏛️", 15.3536, 44.2156, listOf("صنعاء القديمة", "سوق الملح")),
        OfflinePlace("سوق الملح", "صنعاء", "سوق", "🛒", 15.3550, 44.2153, listOf("صنعاء القديمة")),
        OfflinePlace("شارع الزبيري", "صنعاء", "شارع", "🛣️", 15.3440, 44.2020, listOf("الزبيري")),
        OfflinePlace("شارع حدة", "صنعاء", "شارع", "🛣️", 15.3245, 44.1920, listOf("حدة", "حي حدة", "منطقة حدة")),
        OfflinePlace("جولة حدة", "صنعاء", "جولة", "🔄", 15.3185, 44.1912, listOf("حدة", "ميدان حدة")),
        OfflinePlace("جولة الرويشان", "صنعاء", "جولة", "🔄", 15.3358, 44.1965, listOf("الرويشان")),
        OfflinePlace("جولة المصباحي", "صنعاء", "جولة", "🔄", 15.3125, 44.1905, listOf("المصباحي")),
        OfflinePlace("جولة عصر", "صنعاء", "جولة", "🔄", 15.3490, 44.1715, listOf("عصر", "جولة عصر")),
        OfflinePlace("جولة مذبح", "صنعاء", "جولة", "🔄", 15.3680, 44.1720, listOf("مذبح", "حي مذبح")),
        OfflinePlace("جولة الحصبة", "صنعاء", "جولة", "🔄", 15.3820, 44.2060, listOf("الحصبة", "حي الحصبة", "سوق الحصبة")),
        OfflinePlace("جولة عمران", "صنعاء", "جولة", "🔄", 15.3940, 44.1850, listOf("عمران", "خط عمران")),
        OfflinePlace("جولة آية", "صنعاء", "جولة", "🔄", 15.3675, 44.2420, listOf("آية", "خط مأرب")),
        OfflinePlace("جولة سبأ", "صنعاء", "جولة", "🔄", 15.3642, 44.2072, listOf("سبأ", "ميدان سبأ")),
        OfflinePlace("جولة دار سلم", "صنعاء", "جولة", "🔄", 15.2760, 44.2120, listOf("دار سلم", "سواد حنش")),
        OfflinePlace("جولة الخمسين", "صنعاء", "جولة", "🔄", 15.2865, 44.1945, listOf("شارع الخمسين", "بيت بوس")),
        OfflinePlace("شارع الستين الشمالي", "صنعاء", "شارع", "🛣️", 15.3850, 44.1725, listOf("الستين", "الستين الشمالي")),
        OfflinePlace("شارع الستين الجنوبي", "صنعاء", "شارع", "🛣️", 15.3200, 44.1725, listOf("الستين", "الستين الجنوبي", "فج عطان")),
        OfflinePlace("شارع تعز", "صنعاء", "شارع", "🛣️", 15.3180, 44.2160, listOf("خط تعز", "شميلة")),
        OfflinePlace("سوق شميلة", "صنعاء", "سوق", "🛒", 15.3085, 44.2145, listOf("شميلة", "جولة شميلة")),
        OfflinePlace("شارع هائل", "صنعاء", "شارع", "🛣️", 15.3490, 44.1920, listOf("هايل", "سوق هائل")),
        OfflinePlace("شارع عشرين", "صنعاء", "شارع", "🛣️", 15.3435, 44.1865, listOf("عشرين")),
        OfflinePlace("شارع الجزائر", "صنعاء", "شارع", "🛣️", 15.3390, 44.1980, listOf("الجزائر", "المركز الليبي")),
        OfflinePlace("شارع بغداد", "صنعاء", "شارع", "🛣️", 15.3420, 44.1950, listOf("بغداد")),
        OfflinePlace("شارع جمال", "صنعاء", "شارع", "🛣️", 15.3525, 44.2050, listOf("جمال عبدالناصر")),
        OfflinePlace("شارع القيادة", "صنعاء", "شارع", "🛣️", 15.3600, 44.2080, listOf("القيادة")),
        OfflinePlace("شارع المطار", "صنعاء", "شارع", "🛣️", 15.4150, 44.2130, listOf("خط المطار", "الجراف")),
        OfflinePlace("حي الجراف", "صنعاء", "حي", "🏘️", 15.3980, 44.2120, listOf("الجراف")),
        OfflinePlace("الروضة", "صنعاء", "حي", "🏘️", 15.4250, 44.2210, listOf("حي الروضة")),
        OfflinePlace("مطار صنعاء الدولي", "صنعاء", "مطار", "✈️", 15.4763, 44.2197, listOf("مطار صنعاء", "المطار")),
        OfflinePlace("جامعة صنعاء", "صنعاء", "جامعة", "🎓", 15.3645, 44.1865, listOf("الجامعة الجديدة", "كلية الطب")),
        OfflinePlace("مستشفى الثورة العام", "صنعاء", "مستشفى", "🏥", 15.3398, 44.2102, listOf("مستشفى الثورة", "الثورة")),
        OfflinePlace("المستشفى الجمهوري", "صنعاء", "مستشفى", "🏥", 15.3468, 44.2015, listOf("الجمهوري")),
        OfflinePlace("مستشفى الكويت الجامعي", "صنعاء", "مستشفى", "🏥", 15.3598, 44.1965, listOf("مستشفى الكويت")),
        OfflinePlace("مستشفى جامعة العلوم والتكنولوجيا", "صنعاء", "مستشفى", "🏥", 15.3822, 44.1735, listOf("العلوم والتكنولوجيا", "مستشفى العلوم")),
        OfflinePlace("المستشفى السعودي الألماني", "صنعاء", "مستشفى", "🏥", 15.4105, 44.1985, listOf("السعودي الالماني")),
        OfflinePlace("حديقة السبعين", "صنعاء", "معلم", "🌳", 15.3272, 44.2044, listOf("ميدان السبعين", "السبعين", "جامع الصالح")),
        OfflinePlace("الأصبحي", "صنعاء", "حي", "🏘️", 15.2980, 44.1980, listOf("الاصبحي", "شارع المقالح")),
        OfflinePlace("بيت بوس", "صنعاء", "حي", "🏘️", 15.2800, 44.1930, listOf("حي بيت بوس")),
        OfflinePlace("شملان", "صنعاء", "حي", "🏘️", 15.3980, 44.1560, listOf("وادي ظهر", "ضلاع همدان")),
        OfflinePlace("سعوان", "صنعاء", "حي", "🏘️", 15.3620, 44.2480, listOf("حي سعوان")),
        OfflinePlace("الصافية", "صنعاء", "حي", "🏘️", 15.3400, 44.2120, listOf("باب السلام")),
        OfflinePlace("شعوب", "صنعاء", "حي", "🏘️", 15.3680, 44.2220, listOf("مسيك", "سعوان")),

        // ───────── عدن ─────────
        OfflinePlace("عدن", "عدن", "مدينة", "🏙️", 12.7855, 45.0187, listOf("محافظة عدن", "aden")),
        OfflinePlace("كريتر", "عدن", "حي", "🏘️", 12.7794, 45.0367, listOf("صيرة", "عدن القديمة")),
        OfflinePlace("المعلا", "عدن", "حي", "🏘️", 12.7885, 45.0080, listOf("الشارع الرئيسي المعلا")),
        OfflinePlace("التواهي", "عدن", "حي", "🏘️", 12.7845, 44.9830, listOf("جولد مور", "ساحل جولد مور")),
        OfflinePlace("خور مكسر", "عدن", "حي", "🏘️", 12.8100, 45.0300, listOf("ساحل ابين", "العريش")),
        OfflinePlace("الشيخ عثمان", "عدن", "حي", "🏘️", 12.8670, 44.9850, listOf("الشيخ", "فرزة الشيخ عثمان")),
        OfflinePlace("المنصورة", "عدن", "حي", "🏘️", 12.8530, 44.9960, listOf("حي المنصورة", "شارع التسعين")),
        OfflinePlace("دار سعد", "عدن", "حي", "🏘️", 12.9010, 44.9800, listOf("جولة دار سعد", "البساتين")),
        OfflinePlace("البريقة", "عدن", "حي", "🏘️", 12.7610, 44.8910, listOf("عدن الصغرى", "الشعب")),
        OfflinePlace("مطار عدن الدولي", "عدن", "مطار", "✈️", 12.8295, 45.0288, listOf("مطار عدن")),
        OfflinePlace("عدن مول", "عدن", "سوق", "🛒", 12.7805, 45.0320, listOf("سيرة", "كريتر مول")),
        OfflinePlace("جولة كالتكس", "عدن", "جولة", "🔄", 12.8380, 44.9950, listOf("كالتكس")),
        OfflinePlace("جولة السفينة", "عدن", "جولة", "🔄", 12.8745, 44.9855, listOf("السفينة")),
        OfflinePlace("مستشفى الجمهورية عدن", "عدن", "مستشفى", "🏥", 12.8045, 45.0335, listOf("الجمهوري خور مكسر")),
        OfflinePlace("جامعة عدن", "عدن", "جامعة", "🎓", 12.8170, 45.0285, listOf("كلية الهندسة عدن", "مدينة الشعب")),

        // ───────── تعز ─────────
        OfflinePlace("تعز", "تعز", "مدينة", "🏙️", 13.5789, 44.0219, listOf("محافظة تعز", "taiz")),
        OfflinePlace("شارع جمال تعز", "تعز", "شارع", "🛣️", 13.5765, 44.0170, listOf("شارع جمال", "التحرير الاسفل")),
        OfflinePlace("الحوبان", "تعز", "حي", "🏘️", 13.6060, 44.0750, listOf("جولة القصر", "خط الحوبان")),
        OfflinePlace("المسبح", "تعز", "حي", "🏘️", 13.5795, 44.0105, listOf("حي المسبح")),
        OfflinePlace("عصيفرة", "تعز", "حي", "🏘️", 13.5910, 44.0245, listOf("حي عصيفرة")),
        OfflinePlace("الضباب", "تعز", "حي", "🏘️", 13.5590, 43.9680, listOf("نقطة الضباب", "خط التربة")),
        OfflinePlace("قلعة القاهرة تعز", "تعز", "معلم", "🏛️", 13.5715, 44.0175, listOf("القاهرة", "باب موسى")),
        OfflinePlace("مستشفى الثورة تعز", "تعز", "مستشفى", "🏥", 13.5845, 44.0310, listOf("مستشفى الثورة")),
        OfflinePlace("جامعة تعز", "تعز", "جامعة", "🎓", 13.5760, 43.9920, listOf("حبيل سلمان")),
        OfflinePlace("التربة", "تعز", "مدينة", "🏙️", 13.2145, 44.1290, listOf("الحجرية")),

        // ───────── إب ─────────
        OfflinePlace("إب", "إب", "مدينة", "🏙️", 13.9667, 44.1667, listOf("اب", "محافظة اب", "ibb")),
        OfflinePlace("شارع العدين", "إب", "شارع", "🛣️", 13.9720, 44.1610, listOf("جولة العدين", "العدين")),
        OfflinePlace("شارع تعز إب", "إب", "شارع", "🛣️", 13.9580, 44.1720, listOf("خط تعز اب", "مفرق جبلة")),
        OfflinePlace("مفرق جبلة", "إب", "جولة", "🔄", 13.9370, 44.1630, listOf("جبلة")),
        OfflinePlace("مستشفى الثورة إب", "إب", "مستشفى", "🏥", 13.9690, 44.1745, listOf("الثورة اب")),
        OfflinePlace("جامعة إب", "إب", "جامعة", "🎓", 13.9510, 44.1780, listOf("الجامعة اب")),
        OfflinePlace("يريم", "إب", "مدينة", "🏙️", 14.2980, 44.3780, listOf("منطقة يريم")),

        // ───────── الحديدة ─────────
        OfflinePlace("الحديدة", "الحديدة", "مدينة", "🏙️", 14.7978, 42.9545, listOf("محافظة الحديدة", "hodeidah")),
        OfflinePlace("شارع صنعاء الحديدة", "الحديدة", "شارع", "🛣️", 14.8010, 42.9630, listOf("شارع صنعاء")),
        OfflinePlace("شارع الميناء الحديدة", "الحديدة", "شارع", "🛣️", 14.8090, 42.9430, listOf("ميناء الحديدة", "الميناء")),
        OfflinePlace("الكورنيش الحديدة", "الحديدة", "معلم", "🌊", 14.7920, 42.9390, listOf("كورنيش الحديدة")),
        OfflinePlace("مستشفى الثورة الحديدة", "الحديدة", "مستشفى", "🏥", 14.7985, 42.9600, listOf("الثورة الحديدة")),
        OfflinePlace("جامعة الحديدة", "الحديدة", "جامعة", "🎓", 14.7730, 42.9670, listOf("الجامعة الحديدة")),
        OfflinePlace("باجل", "الحديدة", "مدينة", "🏙️", 15.0580, 43.2850, listOf("خط باجل")),

        // ───────── حضرموت (المكلا وسيئون) ─────────
        OfflinePlace("المكلا", "حضرموت (المكلا)", "مدينة", "🏙️", 14.5417, 49.1242, listOf("حضرموت", "mukalla")),
        OfflinePlace("الديس المكلا", "حضرموت (المكلا)", "حي", "🏘️", 14.5480, 49.1360, listOf("حي الديس", "الديس")),
        OfflinePlace("فوة المكلا", "حضرموت (المكلا)", "حي", "🏘️", 14.5050, 49.0620, listOf("فوة", "جامعة حضرموت")),
        OfflinePlace("الشرج المكلا", "حضرموت (المكلا)", "حي", "🏘️", 14.5370, 49.1150, listOf("حي الشرج")),
        OfflinePlace("مطار الريان الدولي", "حضرموت (المكلا)", "مطار", "✈️", 14.6626, 49.3750, listOf("مطار المكلا", "الريان")),
        OfflinePlace("سيئون", "سيئون", "مدينة", "🏙️", 15.9432, 48.7871, listOf("وادي حضرموت", "قصر سيئون")),
        OfflinePlace("مطار سيئون الدولي", "سيئون", "مطار", "✈️", 15.9661, 48.7881, listOf("مطار سيئون")),
        OfflinePlace("تريم", "سيئون", "مدينة", "🏙️", 16.0539, 49.0005, listOf("مدينة تريم")),
        OfflinePlace("شبام حضرموت", "سيئون", "مدينة", "🏛️", 15.9267, 48.6261, listOf("شبام")),

        // ───────── باقي المدن والمحافظات اليمنية ─────────
        OfflinePlace("مأرب", "مأرب", "مدينة", "🏙️", 15.4619, 45.3242, listOf("مارب", "سد مارب", "محافظة مارب")),
        OfflinePlace("سد مأرب", "مأرب", "معلم", "🏛️", 15.4025, 45.2650, listOf("سد مارب")),
        OfflinePlace("ذمار", "ذمار", "مدينة", "🏙️", 14.5427, 44.4051, listOf("محافظة ذمار", "جامعة ذمار")),
        OfflinePlace("جامعة ذمار", "ذمار", "جامعة", "🎓", 14.5680, 44.4040, listOf("الجامعة ذمار")),
        OfflinePlace("صعدة", "صعدة", "مدينة", "🏙️", 16.9402, 43.7639, listOf("محافظة صعدة")),
        OfflinePlace("حجة", "حجة", "مدينة", "🏙️", 15.6917, 43.6028, listOf("محافظة حجة")),
        OfflinePlace("عتق", "عتق (شبوة)", "مدينة", "🏙️", 14.5377, 46.8319, listOf("شبوة", "محافظة شبوة")),
        OfflinePlace("الغيضة", "الغيضة (المهرة)", "مدينة", "🏙️", 16.2079, 52.1760, listOf("المهرة", "محافظة المهرة")),
        OfflinePlace("زنجبار", "زنجبار (أبين)", "مدينة", "🏙️", 13.1287, 45.3806, listOf("أبين", "ابين", "محافظة ابين")),
        OfflinePlace("الحوطة", "الحوطة (لحج)", "مدينة", "🏙️", 13.0583, 44.8822, listOf("لحج", "محافظة لحج")),
        OfflinePlace("الضالع", "الضالع", "مدينة", "🏙️", 13.6958, 44.7314, listOf("محافظة الضالع")),
        OfflinePlace("البيضاء", "البيضاء", "مدينة", "🏙️", 13.9852, 45.5727, listOf("محافظة البيضاء", "رداع")),
        OfflinePlace("رداع", "البيضاء", "مدينة", "🏙️", 14.4167, 44.8333, listOf("مدينة رداع")),
        OfflinePlace("عمران", "عمران", "مدينة", "🏙️", 15.6594, 43.9439, listOf("محافظة عمران")),
        OfflinePlace("المحويت", "المحويت", "مدينة", "🏙️", 15.4700, 43.5447, listOf("محافظة المحويت")),
        OfflinePlace("ريمة", "ريمة", "مدينة", "🏙️", 14.6300, 43.7100, listOf("محافظة ريمة", "الجبين")),
        OfflinePlace("الجوف", "الجوف", "مدينة", "🏙️", 16.1400, 44.7700, listOf("الحزم", "محافظة الجوف")),
        OfflinePlace("سقطرى", "سقطرى", "مدينة", "🏝️", 12.6517, 54.0194, listOf("حديبو", "جزيرة سقطرى"))
    )

    fun search(
        query: String,
        userLat: Double = 0.0,
        userLng: Double = 0.0,
        limit: Int = 12
    ): List<RealValhallaNavigator.SearchAddressResult> {
        val clean = query.trim()
        if (clean.length < 2) return emptyList()

        // 1) فحص الإحداثيات المباشرة (مثل 15.3694, 44.1910)
        parseCoordinates(clean)?.let { (lat, lng) ->
            return listOf(
                RealValhallaNavigator.SearchAddressResult(
                    displayName = String.format(Locale.US, "إحداثيات GPS (%.5f, %.5f)", lat, lng),
                    shortName = String.format(Locale.US, "%.4f, %.4f", lat, lng),
                    latitude = lat,
                    longitude = lng,
                    type = "coords",
                    icon = "🎯"
                )
            )
        }

        val expansions = DialectSynonyms.expand(clean).ifEmpty { listOf(ArabicNormalizer.normalize(clean)) }
        val tokens = ArabicNormalizer.normalize(clean).split(" ").filter { it.length >= 2 }

        val scored = PLACES.mapNotNull { place ->
            val normName = ArabicNormalizer.normalize(place.name)
            val normCity = ArabicNormalizer.normalize(place.city)
            val normCat = ArabicNormalizer.normalize(place.category)
            val normAliases = place.aliases.map { ArabicNormalizer.normalize(it) }
            val searchable = listOf(normName, normCity, normCat) + normAliases

            var score = 0
            for (exp in expansions) {
                when {
                    normName == exp || normAliases.any { it == exp } -> score = maxOf(score, 100)
                    normName.startsWith(exp) || normAliases.any { it.startsWith(exp) } -> score = maxOf(score, 85)
                    normName.contains(exp) || normAliases.any { it.contains(exp) } -> score = maxOf(score, 70)
                    normCity.contains(exp) -> score = maxOf(score, 45)
                }
            }
            if (score == 0 && tokens.size > 1) {
                val allMatch = tokens.all { tok -> searchable.any { it.contains(tok) } }
                if (allMatch) score = 60
            }
            if (score == 0) return@mapNotNull null
            place to score
        }

        val hasUser = !(userLat == 0.0 && userLng == 0.0)
        return scored
            .sortedWith(
                compareByDescending<Pair<OfflinePlace, Int>> { it.second }
                    .thenBy { (p, _) ->
                        if (hasUser) HaversinePrecisionNavigator.calculateDistance(userLat, userLng, p.latitude, p.longitude)
                        else 0.0
                    }
            )
            .take(limit)
            .map { (p, _) ->
                RealValhallaNavigator.SearchAddressResult(
                    displayName = "${p.name} — ${p.city} (${p.category} · بحث محلي)",
                    shortName = p.name,
                    latitude = p.latitude,
                    longitude = p.longitude,
                    type = "offline_gazetteer",
                    icon = p.icon
                )
            }
    }

    /** اقتراحات قريبة سريعة عند فتح البحث دون اتصال أو قبل الكتابة. */
    fun suggestionsNear(
        userLat: Double,
        userLng: Double,
        limit: Int = 8
    ): List<RealValhallaNavigator.SearchAddressResult> {
        val hasUser = !(userLat == 0.0 && userLng == 0.0)
        val sorted = if (hasUser) {
            PLACES.sortedBy { HaversinePrecisionNavigator.calculateDistance(userLat, userLng, it.latitude, it.longitude) }
        } else {
            PLACES
        }
        return sorted.take(limit).map { p ->
            RealValhallaNavigator.SearchAddressResult(
                displayName = "${p.name} — ${p.city} (${p.category})",
                shortName = p.name,
                latitude = p.latitude,
                longitude = p.longitude,
                type = "offline_gazetteer",
                icon = p.icon
            )
        }
    }

    private fun parseCoordinates(raw: String): Pair<Double, Double>? {
        val parts = raw.replace("،", ",").split(",", " ").map { it.trim() }.filter { it.isNotEmpty() }
        if (parts.size != 2) return null
        val lat = parts[0].toDoubleOrNull() ?: return null
        val lng = parts[1].toDoubleOrNull() ?: return null
        if (lat in -90.0..90.0 && lng in -180.0..180.0 && !(lat == 0.0 && lng == 0.0)) {
            return lat to lng
        }
        return null
    }

    fun cacheOnlineResults(context: Context, results: List<RealValhallaNavigator.SearchAddressResult>) {
        if (results.isEmpty()) return
        synchronized(cacheLock) {
            try {
                val file = File(context.filesDir, CACHE_FILE_NAME)
                val existing = loadCachedInternal(file).toMutableList()
                for (r in results) {
                    val key = String.format(Locale.US, "%.5f_%.5f", r.latitude, r.longitude)
                    existing.removeAll { String.format(Locale.US, "%.5f_%.5f", it.latitude, it.longitude) == key }
                    existing.add(0, r)
                }
                val trimmed = existing.take(MAX_PERSISTENT_CACHE)
                val arr = JSONArray()
                for (item in trimmed) {
                    arr.put(JSONObject().apply {
                        put("displayName", item.displayName)
                        put("shortName", item.shortName)
                        put("latitude", item.latitude)
                        put("longitude", item.longitude)
                        put("type", item.type)
                        put("icon", item.icon)
                    })
                }
                file.writeText(arr.toString())
            } catch (_: Throwable) {}
        }
    }

    fun searchCachedOnlineResults(context: Context, query: String): List<RealValhallaNavigator.SearchAddressResult> {
        val nq = ArabicNormalizer.normalize(query)
        if (nq.length < 2) return emptyList()
        synchronized(cacheLock) {
            return try {
                val file = File(context.filesDir, CACHE_FILE_NAME)
                loadCachedInternal(file).filter {
                    ArabicNormalizer.normalize(it.shortName).contains(nq) ||
                        ArabicNormalizer.normalize(it.displayName).contains(nq)
                }.take(10)
            } catch (_: Throwable) {
                emptyList()
            }
        }
    }

    private fun loadCachedInternal(file: File): List<RealValhallaNavigator.SearchAddressResult> {
        if (!file.exists()) return emptyList()
        val arr = JSONArray(file.readText())
        val out = ArrayList<RealValhallaNavigator.SearchAddressResult>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val lat = o.optDouble("latitude", 0.0)
            val lng = o.optDouble("longitude", 0.0)
            if (lat == 0.0 && lng == 0.0) continue
            out.add(
                RealValhallaNavigator.SearchAddressResult(
                    displayName = o.optString("displayName"),
                    shortName = o.optString("shortName"),
                    latitude = lat,
                    longitude = lng,
                    type = o.optString("type", "cached"),
                    icon = o.optString("icon", "📍")
                )
            )
        }
        return out
    }
}
