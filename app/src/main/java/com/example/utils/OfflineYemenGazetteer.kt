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
        OfflinePlace("جولة كنتاكي", "صنعاء", "جولة", "🔄", 15.3445, 44.2045, listOf("كنتاكي", "تقاطع الزبيري وشارع حدة", "جولة كينتاكي")),
        OfflinePlace("جولة ريماس", "صنعاء", "جولة", "🔄", 15.3290, 44.1935, listOf("ريماس", "حدة ريماس")),
        OfflinePlace("جولة الرويشان", "صنعاء", "جولة", "🔄", 15.3358, 44.1965, listOf("الرويشان")),
        OfflinePlace("جولة المصباحي", "صنعاء", "جولة", "🔄", 15.3125, 44.1905, listOf("المصباحي")),
        OfflinePlace("جولة عصر", "صنعاء", "جولة", "🔄", 15.3490, 44.1715, listOf("عصر", "جولة عصر")),
        OfflinePlace("جولة مذبح", "صنعاء", "جولة", "🔄", 15.3680, 44.1720, listOf("مذبح", "حي مذبح", "مذبح القديمة")),
        OfflinePlace("جولة الحصبة", "صنعاء", "جولة", "🔄", 15.3820, 44.2060, listOf("الحصبة", "حي الحصبة", "سوق الحصبة")),
        OfflinePlace("جولة عمران", "صنعاء", "جولة", "🔄", 15.3940, 44.1850, listOf("عمران", "خط عمران")),
        OfflinePlace("جولة آية", "صنعاء", "جولة", "🔄", 15.3675, 44.2420, listOf("آية", "خط مأرب")),
        OfflinePlace("جولة سبأ", "صنعاء", "جولة", "🔄", 15.3642, 44.2072, listOf("سبأ", "ميدان سبأ")),
        OfflinePlace("جولة دار سلم", "صنعاء", "جولة", "🔄", 15.2760, 44.2120, listOf("دار سلم", "سواد حنش")),
        OfflinePlace("جولة الخمسين", "صنعاء", "جولة", "🔄", 15.2865, 44.1945, listOf("شارع الخمسين", "بيت بوس")),
        OfflinePlace("جولة 45", "صنعاء", "جولة", "🔄", 15.3210, 44.2100, listOf("شارع 45", "جولة خمسة واربعين")),
        OfflinePlace("جولة المرور", "صنعاء", "جولة", "🔄", 15.3315, 44.1850, listOf("المرور", "ادارة المرور")),
        OfflinePlace("جولة الجمنة", "صنعاء", "جولة", "🔄", 15.4210, 44.2140, listOf("الجمنة", "خط المطار")),
        OfflinePlace("جولة شيراتون", "صنعاء", "جولة", "🔄", 15.3650, 44.2310, listOf("شيراتون", "فندق شيراتون")),
        OfflinePlace("جولة الرقاص", "صنعاء", "جولة", "🔄", 15.3520, 44.1880, listOf("الرقاص", "شارع الرقاص")),
        OfflinePlace("جولة يمن موبايل", "صنعاء", "جولة", "🔄", 15.3780, 44.2010, listOf("يمن موبايل", "تلفزيون")),
        OfflinePlace("جولة الحباري", "صنعاء", "جولة", "🔄", 15.3880, 44.2090, listOf("الحباري", "سوق الحباري")),
        OfflinePlace("جولة الجامعة", "صنعاء", "جولة", "🔄", 15.3650, 44.1830, listOf("جولة الجامعة القديمة", "جولة جامعة صنعاء")),
        OfflinePlace("جولة النصر", "صنعاء", "جولة", "🔄", 15.3710, 44.2380, listOf("شارع النصر", "حي النصر")),
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
        OfflinePlace("شارع صخر", "صنعاء", "شارع", "🛣️", 15.3510, 44.1970, listOf("صخر", "شارع مجاهد")),
        OfflinePlace("شارع مجاهد", "صنعاء", "شارع", "🛣️", 15.3380, 44.1930, listOf("مجاهد")),
        OfflinePlace("شارع عمان", "صنعاء", "شارع", "🛣️", 15.3340, 44.1910, listOf("عمان")),
        OfflinePlace("شارع تونس", "صنعاء", "شارع", "🛣️", 15.3630, 44.1990, listOf("تونس")),
        OfflinePlace("شارع الدائري الغربي", "صنعاء", "شارع", "🛣️", 15.3560, 44.1870, listOf("الدائري", "شارع الدائري")),
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

        // ───────── شركات ومحلات الصرافة والبنوك (صنعاء، عدن، تعز، إب، حضرموت) ─────────
        OfflinePlace("شركة الكريمي للصرافة - فرع حدة", "صنعاء", "صرافة", "💰", 15.3240, 44.1925, listOf("الكريمي", "صرافة الكريمي", "بنك الكريمي", "حوالات")),
        OfflinePlace("شركة الكريمي للصرافة - فرع الزبيري", "صنعاء", "صرافة", "💰", 15.3435, 44.2015, listOf("الكريمي", "صرافة الكريمي", "الكريمي الزبيري")),
        OfflinePlace("شركة الكريمي للصرافة - فرع التحرير", "صنعاء", "صرافة", "💰", 15.3540, 44.2060, listOf("الكريمي", "الكريمي التحرير")),
        OfflinePlace("شركة الكريمي للصرافة - فرع شميلة", "صنعاء", "صرافة", "💰", 15.3090, 44.2150, listOf("الكريمي شميلة")),
        OfflinePlace("شركة الكريمي للصرافة - فرع الحصبة", "صنعاء", "صرافة", "💰", 15.3815, 44.2055, listOf("الكريمي الحصبة")),
        OfflinePlace("شركة النجم للحوالات والصرافة", "صنعاء", "صرافة", "💰", 15.3190, 44.2155, listOf("النجم", "صرافة النجم", "شبكة النجم", "حوالات النجم")),
        OfflinePlace("شركة سويد وإخوانه للصرافة", "صنعاء", "صرافة", "💰", 15.3450, 44.2030, listOf("سويد", "صرافة سويد", "سويد للصرافة")),
        OfflinePlace("شركة الحظاء للصرافة", "صنعاء", "صرافة", "💰", 15.3530, 44.2140, listOf("الحظاء", "صرافة الحظاء")),
        OfflinePlace("شركة دادية للصرافة", "صنعاء", "صرافة", "💰", 15.3460, 44.1990, listOf("دادية", "صرافة دادية")),
        OfflinePlace("شركة الامتياز للصرافة", "صنعاء", "صرافة", "💰", 15.3485, 44.1925, listOf("الامتياز", "شبكة الامتياز")),
        OfflinePlace("شركة الصيفي للصرافة", "صنعاء", "صرافة", "💰", 15.3555, 44.2145, listOf("الصيفي", "صرافة الصيفي")),
        OfflinePlace("بنك التضامن الإسلامي الدولي", "صنعاء", "بنك", "🏦", 15.3440, 44.2025, listOf("التضامن", "بنك التضامن")),
        OfflinePlace("بنك اليمن والكويت", "صنعاء", "بنك", "🏦", 15.3260, 44.1930, listOf("اليمن والكويت", "بنك الكويت")),

        // ───────── الجوامع والمساجد الكبرى ─────────
        OfflinePlace("جامع الصالح", "صنعاء", "جامع", "🕌", 15.3268, 44.2052, listOf("جامع الصالح السبعين", "مسجد الصالح")),
        OfflinePlace("الجامع الكبير بصنعاء", "صنعاء", "جامع", "🕌", 15.3532, 44.2145, listOf("الجامع الكبير", "مسجد صنعاء القديمة")),
        OfflinePlace("جامع الخير", "صنعاء", "جامع", "🕌", 15.3670, 44.1730, listOf("مسجد الخير", "مذبح الخير")),
        OfflinePlace("جامع الشوكاني", "صنعاء", "جامع", "🕌", 15.3510, 44.2010, listOf("مسجد الشوكاني")),
        OfflinePlace("جامع قبة البكيرية", "صنعاء", "جامع", "🕌", 15.3560, 44.2180, listOf("البكيرية")),
        OfflinePlace("جامع الرقاص", "صنعاء", "جامع", "🕌", 15.3515, 44.1875, listOf("مسجد الرقاص")),
        OfflinePlace("جامع الحاشدي", "صنعاء", "جامع", "🕌", 15.3220, 44.1940, listOf("مسجد الحاشدي")),

        // ───────── المدارس والثانويات والمعاهد ─────────
        OfflinePlace("ثانوية جمال عبد الناصر للمتفوقين", "صنعاء", "مدرسة", "🏫", 15.3540, 44.2045, listOf("عبد الناصر", "مدرسة عبد الناصر", "ثانوية عبد الناصر")),
        OfflinePlace("مدرسة الكويت بصنعاء", "صنعاء", "مدرسة", "🏫", 15.3425, 44.1955, listOf("مدرسة الكويت", "ثانوية الكويت")),
        OfflinePlace("مدرسة سيف بن ذي يزن", "صنعاء", "مدرسة", "🏫", 15.3520, 44.2170, listOf("سيف بن ذي يزن", "مدرسة سيف")),
        OfflinePlace("مدرسة بغداد", "صنعاء", "مدرسة", "🏫", 15.3415, 44.1940, listOf("مدرسة بغداد")),
        OfflinePlace("مدرسة رابعة العدوية", "صنعاء", "مدرسة", "🏫", 15.3570, 44.1880, listOf("رابعة العدوية", "مدرسة رابعة")),
        OfflinePlace("مدرسة عائشة", "صنعاء", "مدرسة", "🏫", 15.3390, 44.2110, listOf("مدرسة عائشة للبنات")),
        OfflinePlace("مدرسة ابن ماجد", "صنعاء", "مدرسة", "🏫", 15.3830, 44.2050, listOf("ابن ماجد")),
        OfflinePlace("مدرسة بلقيس", "صنعاء", "مدرسة", "🏫", 15.3450, 44.2010, listOf("مدرسة بلقيس للبنات")),
        OfflinePlace("المعهد التقني الصناعي", "صنعاء", "معهد", "🎓", 15.4200, 44.1750, listOf("المعهد الصناعي", "ذهبان")),
        OfflinePlace("المعهد العالي للعلوم الصحية", "صنعاء", "معهد", "🎓", 15.3410, 44.2090, listOf("معهد العلوم الصحية")),

        // ───────── عدن ─────────
        OfflinePlace("عدن", "عدن", "مدينة", "🏙️", 12.7855, 45.0187, listOf("محافظة عدن", "aden")),
        OfflinePlace("كريتر", "عدن", "حي", "🏘️", 12.7794, 45.0367, listOf("صيرة", "عدن القديمة")),
        OfflinePlace("المعلا", "عدن", "حي", "🏘️", 12.7885, 45.0080, listOf("الشارع الرئيسي المعلا", "شارع مدرم")),
        OfflinePlace("التواهي", "عدن", "حي", "🏘️", 12.7845, 44.9830, listOf("جولد مور", "ساحل جولد مور")),
        OfflinePlace("خور مكسر", "عدن", "حي", "🏘️", 12.8100, 45.0300, listOf("ساحل ابين", "العريش")),
        OfflinePlace("الشيخ عثمان", "عدن", "حي", "🏘️", 12.8670, 44.9850, listOf("الشيخ", "فرزة الشيخ عثمان")),
        OfflinePlace("المنصورة", "عدن", "حي", "🏘️", 12.8530, 44.9960, listOf("حي المنصورة", "شارع التسعين")),
        OfflinePlace("دار سعد", "عدن", "حي", "🏘️", 12.9010, 44.9800, listOf("جولة دار سعد", "البساتين")),
        OfflinePlace("البريقة", "عدن", "حي", "🏘️", 12.7610, 44.8910, listOf("عدن الصغرى", "الشعب", "ساحل الغدير")),
        OfflinePlace("مطار عدن الدولي", "عدن", "مطار", "✈️", 12.8295, 45.0288, listOf("مطار عدن")),
        OfflinePlace("عدن مول", "عدن", "سوق", "🛒", 12.7805, 45.0320, listOf("سيرة", "كريتر مول")),
        OfflinePlace("جولة كالتكس", "عدن", "جولة", "🔄", 12.8380, 44.9950, listOf("كالتكس", "دوار كالتكس")),
        OfflinePlace("جولة السفينة", "عدن", "جولة", "🔄", 12.8745, 44.9855, listOf("السفينة", "دوار السفينة")),
        OfflinePlace("جولة الكراع", "عدن", "جولة", "🔄", 12.8980, 44.9810, listOf("الكراع", "دوار الكراع")),
        OfflinePlace("جولة الغزل والنسيج", "عدن", "جولة", "🔄", 12.8610, 44.9920, listOf("الغزل والنسيج")),
        OfflinePlace("جولة سوزوكي", "عدن", "جولة", "🔄", 12.8250, 45.0110, listOf("سوزوكي")),
        OfflinePlace("جولة العاقل", "عدن", "جولة", "🔄", 12.7950, 45.0230, listOf("العاقل")),
        OfflinePlace("جولة زكو", "عدن", "جولة", "🔄", 12.7810, 45.0350, listOf("زكو كريتر")),
        OfflinePlace("جولة الفل", "عدن", "جولة", "🔄", 12.7785, 45.0360, listOf("الفل كريتر")),
        OfflinePlace("شركة القطيبي للصرافة - المنصورة", "عدن", "صرافة", "💰", 12.8540, 44.9970, listOf("القطيبي", "صرافة القطيبي", "بنك القطيبي")),
        OfflinePlace("شركة الكريمي للصرافة - فرع كريتر", "عدن", "صرافة", "💰", 12.7810, 45.0340, listOf("الكريمي كريتر", "الكريمي عدن")),
        OfflinePlace("شركة الكريمي للصرافة - فرع الشيخ عثمان", "عدن", "صرافة", "💰", 12.8660, 44.9860, listOf("الكريمي الشيخ عثمان")),
        OfflinePlace("شركة العمقي وإخوانه للصرافة - عدن", "عدن", "صرافة", "💰", 12.7890, 45.0090, listOf("العمقي عدن", "صرافة العمقي")),
        OfflinePlace("جامع أبان التاريخي", "عدن", "جامع", "🕌", 12.7790, 45.0355, listOf("جامع ابان", "مسجد ابان")),
        OfflinePlace("جامع العيدروس", "عدن", "جامع", "🕌", 12.7760, 45.0330, listOf("العيدروس", "مسجد العيدروس")),
        OfflinePlace("جامع الهاشمي", "عدن", "جامع", "🕌", 12.8680, 44.9840, listOf("مسجد الهاشمي", "فرزة الهاشمي")),
        OfflinePlace("ثانوية البيحاني النموذجية", "عدن", "مدرسة", "🏫", 12.7800, 45.0380, listOf("البيحاني", "مدرسة البيحاني")),
        OfflinePlace("ثانوية لطفي جعفر أمان", "عدن", "مدرسة", "🏫", 12.7770, 45.0350, listOf("لطفي جعفر امان")),
        OfflinePlace("مستشفى الجمهورية عدن", "عدن", "مستشفى", "🏥", 12.8045, 45.0335, listOf("الجمهوري خور مكسر")),
        OfflinePlace("جامعة عدن", "عدن", "جامعة", "🎓", 12.8170, 45.0285, listOf("كلية الهندسة عدن", "مدينة الشعب")),

        // ───────── تعز ─────────
        OfflinePlace("تعز", "تعز", "مدينة", "🏙️", 13.5789, 44.0219, listOf("محافظة تعز", "taiz")),
        OfflinePlace("شارع جمال تعز", "تعز", "شارع", "🛣️", 13.5765, 44.0170, listOf("شارع جمال", "التحرير الاسفل")),
        OfflinePlace("الحوبان", "تعز", "حي", "🏘️", 13.6060, 44.0750, listOf("جولة القصر", "خط الحوبان")),
        OfflinePlace("جولة القصر تعز", "تعز", "جولة", "🔄", 13.6010, 44.0680, listOf("جولة القصر", "فرزة الحوبان")),
        OfflinePlace("جولة وادي القاضي", "تعز", "جولة", "🔄", 13.5850, 44.0090, listOf("وادي القاضي")),
        OfflinePlace("جولة الصقر", "تعز", "جولة", "🔄", 13.5680, 43.9980, listOf("نادي الصقر", "بير باشا")),
        OfflinePlace("جولة سنان", "تعز", "جولة", "🔄", 13.5790, 44.0190, listOf("باب موسى", "سنان")),
        OfflinePlace("شركة الكريمي للصرافة - فرع شارع جمال تعز", "تعز", "صرافة", "💰", 13.5770, 44.0165, listOf("الكريمي تعز")),
        OfflinePlace("شركة النجم للصرافة - تعز", "تعز", "صرافة", "💰", 13.5780, 44.0180, listOf("النجم تعز")),
        OfflinePlace("جامع الجند التاريخي", "تعز", "جامع", "🕌", 13.6700, 44.1100, listOf("مسجد الجند", "الجند")),
        OfflinePlace("جامع المظفر", "تعز", "جامع", "🕌", 13.5720, 44.0150, listOf("المظفر تعز القديمة")),
        OfflinePlace("جامع الأشرفية", "تعز", "جامع", "🕌", 13.5700, 44.0160, listOf("الاشرفية تعز")),
        OfflinePlace("ثانوية تعز الكبرى", "تعز", "مدرسة", "🏫", 13.5750, 44.0180, listOf("مدرسة تعز الكبرى", "ثانوية تعز")),
        OfflinePlace("مدرسة الشهيد الدرة", "تعز", "مدرسة", "🏫", 13.5810, 44.0120, listOf("محمد الدرة")),
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
        OfflinePlace("جولة العدين إب", "إب", "جولة", "🔄", 13.9715, 44.1605, listOf("جولة العدين", "فرزة العدين")),
        OfflinePlace("مفرق جبلة", "إب", "جولة", "🔄", 13.9370, 44.1630, listOf("جبلة", "دوار جبلة")),
        OfflinePlace("شركة الكريمي للصرافة - فرع إب", "إب", "صرافة", "💰", 13.9700, 44.1630, listOf("الكريمي اب")),
        OfflinePlace("جامع قيدان التاريخي", "إب", "جامع", "🕌", 13.9650, 44.1680, listOf("مسجد قيدان", "قيدان")),
        OfflinePlace("ثانوية النهضة إب", "إب", "مدرسة", "🏫", 13.9680, 44.1650, listOf("مدرسة النهضة اب")),
        OfflinePlace("شارع تعز إب", "إب", "شارع", "🛣️", 13.9580, 44.1720, listOf("خط تعز اب", "مفرق جبلة")),
        OfflinePlace("مستشفى الثورة إب", "إب", "مستشفى", "🏥", 13.9690, 44.1745, listOf("الثورة اب")),
        OfflinePlace("جامعة إب", "إب", "جامعة", "🎓", 13.9510, 44.1780, listOf("الجامعة اب")),
        OfflinePlace("يريم", "إب", "مدينة", "🏙️", 14.2980, 44.3780, listOf("منطقة يريم", "جولة يريم")),

        // ───────── الحديدة ─────────
        OfflinePlace("الحديدة", "الحديدة", "مدينة", "🏙️", 14.7978, 42.9545, listOf("محافظة الحديدة", "hodeidah")),
        OfflinePlace("شارع صنعاء الحديدة", "الحديدة", "شارع", "🛣️", 14.8010, 42.9630, listOf("شارع صنعاء")),
        OfflinePlace("شارع الميناء الحديدة", "الحديدة", "شارع", "🛣️", 14.8090, 42.9430, listOf("ميناء الحديدة", "الميناء")),
        OfflinePlace("جولة المحافظة الحديدة", "الحديدة", "جولة", "🔄", 14.7990, 42.9580, listOf("المحافظة الحديدة")),
        OfflinePlace("جولة الساعة الحديدة", "الحديدة", "جولة", "🔄", 14.8020, 42.9520, listOf("الساعة الحديدة")),
        OfflinePlace("الكورنيش الحديدة", "الحديدة", "معلم", "🌊", 14.7920, 42.9390, listOf("كورنيش الحديدة")),
        OfflinePlace("مستشفى الثورة الحديدة", "الحديدة", "مستشفى", "🏥", 14.7985, 42.9600, listOf("الثورة الحديدة")),
        OfflinePlace("جامعة الحديدة", "الحديدة", "جامعة", "🎓", 14.7730, 42.9670, listOf("الجامعة الحديدة")),
        OfflinePlace("باجل", "الحديدة", "مدينة", "🏙️", 15.0580, 43.2850, listOf("خط باجل")),

        // ───────── حضرموت (المكلا وسيئون) ─────────
        OfflinePlace("المكلا", "حضرموت (المكلا)", "مدينة", "🏙️", 14.5417, 49.1242, listOf("حضرموت", "mukalla")),
        OfflinePlace("الديس المكلا", "حضرموت (المكلا)", "حي", "🏘️", 14.5480, 49.1360, listOf("حي الديس", "الديس")),
        OfflinePlace("فوة المكلا", "حضرموت (المكلا)", "حي", "🏘️", 14.5050, 49.0620, listOf("فوة", "جامعة حضرموت")),
        OfflinePlace("الشرج المكلا", "حضرموت (المكلا)", "حي", "🏘️", 14.5370, 49.1150, listOf("حي الشرج")),
        OfflinePlace("شركة العمقي وإخوانه للصرافة - المكلا", "حضرموت (المكلا)", "صرافة", "💰", 14.5380, 49.1160, listOf("العمقي المكلا", "صرافة العمقي")),
        OfflinePlace("شركة البسيري للصرافة", "حضرموت (المكلا)", "صرافة", "💰", 14.5400, 49.1200, listOf("البسيري", "صرافة البسيري")),
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
        OfflinePlace("جامع الإمام الهادي التاريخي", "صعدة", "جامع", "🕌", 16.9420, 43.7650, listOf("جامع الهادي", "مسجد الهادي")),
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
        limit: Int = 16
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
                    normName.startsWith(exp) || normAliases.any { it.startsWith(exp) } -> score = maxOf(score, 90)
                    normName.contains(exp) || normAliases.any { it.contains(exp) } -> score = maxOf(score, 80)
                    normCat == exp -> score = maxOf(score, 75)
                    normCat.contains(exp) -> score = maxOf(score, 65)
                    normCity.contains(exp) -> score = maxOf(score, 50)
                }
            }
            if (tokens.isNotEmpty()) {
                var tokenHits = 0
                for (tok in tokens) {
                    if (normName.contains(tok) || normAliases.any { it.contains(tok) }) {
                        tokenHits += 25
                    } else if (normCat.contains(tok)) {
                        tokenHits += 20
                    } else if (normCity.contains(tok)) {
                        tokenHits += 15
                    }
                }
                score = maxOf(score, tokenHits)
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

    /**
     * يحلّل الإحداثيات بأي صيغة شائعة:
     * - 15.3694, 44.1910 (أو بأرقام عربية ١٥.٣٦٩٤، ٤٤.١٩١٠)
     * - روابط خرائط جوجل أو واتساب: ?q=lat,lng أو @lat,lng أو ll=lat,lng
     * - روابط geo:lat,lng أو OpenStreetMap #map=zoom/lat/lng
     * - درجات ودقائق: 15°22'10"N 44°11'28"E
     */
    fun parseCoordinates(raw: String): Pair<Double, Double>? {
        val normalized = normalizeDigits(raw).trim()
        if (normalized.isBlank()) return null

        // 1) روابط التطبيق الداخلية أو خرائط جوجل / واتساب / OSM / geo
        val urlPatterns = listOf(
            Regex("[?&]lat=([+-]?\\d+(?:\\.\\d+)?)[^\\s]*?&lng=([+-]?\\d+(?:\\.\\d+)?)"),
            Regex("[?&]lng=([+-]?\\d+(?:\\.\\d+)?)[^\\s]*?&lat=([+-]?\\d+(?:\\.\\d+)?)"),
            Regex("(?:q|ll|center)=([+-]?\\d+(?:\\.\\d+)?),\\s*([+-]?\\d+(?:\\.\\d+)?)"),
            Regex("@([+-]?\\d+(?:\\.\\d+)?),([+-]?\\d+(?:\\.\\d+)?)"),
            Regex("geo:([+-]?\\d+(?:\\.\\d+)?),([+-]?\\d+(?:\\.\\d+)?)"),
            Regex("#map=\\d+/([+-]?\\d+(?:\\.\\d+)?)/([+-]?\\d+(?:\\.\\d+)?)")
        )
        for ((index, p) in urlPatterns.withIndex()) {
            p.find(normalized)?.let { m ->
                val rawFirst = m.groupValues[1].toDoubleOrNull()
                val rawSecond = m.groupValues[2].toDoubleOrNull()
                val lat = if (index == 1) rawSecond else rawFirst
                val lng = if (index == 1) rawFirst else rawSecond
                if (lat != null && lng != null && LandmarkAddress.isValid(lat, lng)) return lat to lng
            }
        }

        // 2) درجات ودقائق وثواني: 15°22'10.5"N 44°11'28"E
        val dmsRegex = Regex("(\\d{1,3})°\\s*(\\d{1,2})['′]\\s*(\\d{1,2}(?:\\.\\d+)?)[\"″]?\\s*([NSns]).*?(\\d{1,3})°\\s*(\\d{1,2})['′]\\s*(\\d{1,2}(?:\\.\\d+)?)[\"″]?\\s*([EWew])")
        dmsRegex.find(normalized)?.let { m ->
            val latDeg = m.groupValues[1].toDouble() + m.groupValues[2].toDouble() / 60.0 + m.groupValues[3].toDouble() / 3600.0
            val latSign = if (m.groupValues[4].equals("S", true)) -1.0 else 1.0
            val lngDeg = m.groupValues[5].toDouble() + m.groupValues[6].toDouble() / 60.0 + m.groupValues[7].toDouble() / 3600.0
            val lngSign = if (m.groupValues[8].equals("W", true)) -1.0 else 1.0
            val lat = latDeg * latSign
            val lng = lngDeg * lngSign
            if (LandmarkAddress.isValid(lat, lng)) return lat to lng
        }

        // 3) إحداثيات عشرية مباشرة (lat, lng أو lat lng)
        val cleaned = normalized
            .replace("،", ",")
            .replace("؛", ",")
            .replace(";", ",")
            .replace("|", ",")
        val decimalPair = Regex("^\\s*([+-]?\\d+(?:\\.\\d+)?)\\s*[,\\s]+\\s*([+-]?\\d+(?:\\.\\d+)?)\\s*$")
        decimalPair.matchEntire(cleaned)?.let { m ->
            val lat = m.groupValues[1].toDoubleOrNull()
            val lng = m.groupValues[2].toDoubleOrNull()
            if (lat != null && lng != null && LandmarkAddress.isValid(lat, lng)) return lat to lng
        }

        // 4) استخراج زوج إحداثيات من أي نص مشاركة (مثل رسالة واتساب)
        val embedded = Regex("([+-]?\\d{1,2}\\.\\d{3,})\\s*[,،\\s]+\\s*([+-]?\\d{1,3}\\.\\d{3,})")
        embedded.find(normalized)?.let { m ->
            val lat = m.groupValues[1].toDoubleOrNull()
            val lng = m.groupValues[2].toDoubleOrNull()
            if (lat != null && lng != null && LandmarkAddress.isValid(lat, lng)) return lat to lng
        }

        return null
    }

    fun buildAppLocationUri(lat: Double, lng: Double, label: String = "موقع محدد"): String {
        val latStr = String.format(Locale.US, "%.6f", lat)
        val lngStr = String.format(Locale.US, "%.6f", lng)
        val encodedLabel = android.net.Uri.encode(label.ifBlank { "موقع محدد" })
        return "yemenserv://map?lat=$latStr&lng=$lngStr&label=$encodedLabel"
    }

    fun extractLocationLabel(raw: String): String? {
        return try {
            val uri = android.net.Uri.parse(raw)
            uri.getQueryParameter("label")?.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }

    fun formatCoordinates(lat: Double, lng: Double): String =
        String.format(Locale.US, "%.5f, %.5f", lat, lng)

    fun nearestReferenceSummary(context: Context?, lat: Double, lng: Double): String {
        if (!LandmarkAddress.isValid(lat, lng)) return ""
        val custom = if (context != null) CustomLandmarkStore.nearby(context, lat, lng, 25_000.0) else emptyList()
        val gaz = PLACES.map { p ->
            LandmarkCandidate(
                name = p.name,
                lat = p.latitude,
                lng = p.longitude,
                kind = p.category,
                distanceM = HaversinePrecisionNavigator.calculateDistance(lat, lng, p.latitude, p.longitude)
            )
        }
        val nearest = (custom + gaz).minByOrNull { it.distanceM } ?: return formatCoordinates(lat, lng)
        val helperAddr = LandmarkAddress(
            label = "",
            landmarkName = nearest.name,
            landmarkLat = nearest.lat,
            landmarkLng = nearest.lng,
            pointLat = lat,
            pointLng = lng,
            description = ""
        )
        return helperAddr.describe()
    }

    private fun normalizeDigits(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            when (ch) {
                in '٠'..'٩' -> sb.append(('0'.code + (ch.code - '٠'.code)).toChar())
                in '۰'..'۹' -> sb.append(('0'.code + (ch.code - '۰'.code)).toChar())
                '٫' -> sb.append('.')
                else -> sb.append(ch)
            }
        }
        return sb.toString()
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
