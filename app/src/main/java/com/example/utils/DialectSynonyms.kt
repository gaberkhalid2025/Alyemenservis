package com.example.utils

object DialectSynonyms {
    /** مفتاح: كما يكتبه الناس (قبل التطبيع) ← بدائل مسجلة. أضف مصطلحاتك بعد التحقق من معناها. */
    @Volatile private var entries: Map<String, List<String>> = mapOf(
        "صرافة" to listOf("صراف", "تحويلات", "حوالات", "بنك", "كريمي", "نجم", "قطيبي", "سويد"),
        "كريمي" to listOf("الكريمي", "شركة الكريمي للصرافة", "مصرف الكريمي", "صرافة"),
        "نجم" to listOf("النجم", "شركة النجم للحوالات", "صرافة"),
        "صالة افراح" to listOf("صالة", "قاعة", "قاعة افراح", "صالة اعراس", "قاعة اعراس", "مناسبات", "قصر افراح"),
        "قاعة افراح" to listOf("قاعة", "صالة", "صالة افراح", "مناسبات", "اعراس", "قصر افراح"),
        "صالة" to listOf("قاعة", "صالة افراح", "قاعة افراح", "مناسبات", "قصر"),
        "قاعة" to listOf("صالة", "قاعة افراح", "صالة افراح", "مناسبات", "قصر"),
        "جولة" to listOf("دوار", "ميدان", "تقاطع"),
        "دوار" to listOf("جولة", "ميدان"),
        "شارع" to listOf("خط", "طريق", "جاده", "الدائري", "شارع عام"),
        "جامع" to listOf("مسجد", "مصلى"),
        "مسجد" to listOf("جامع", "مصلى"),
        "مدرسة" to listOf("مدارس", "ثانوية", "اساسية", "مجمع تربوي", "معهد", "اهلية", "روضة"),
        "مدارس" to listOf("مدرسة", "ثانوية", "معهد"),
        "ثانوية" to listOf("مدرسة", "مدارس", "مجمع"),
        "بيت" to listOf("منزل", "دار", "شقة", "فيلا", "عمارة", "سكن"),
        "منزل" to listOf("بيت", "دار", "شقة", "فيلا"),
        "حارة" to listOf("حي", "منطقة", "مربع"),
        "حي" to listOf("حارة", "منطقة", "مربع"),
        "محل" to listOf("متجر", "دكان", "بقالة", "سوق", "مركز", "مول"),
        "متجر" to listOf("محل", "سوق", "مول", "سوبر ماركت", "هايبر"),
        "مول" to listOf("مركز تجاري", "سنتر", "سوق", "متجر"),
        "سوق" to listOf("مول", "مركز تجاري", "ماركت", "سوبر ماركت"),
        "مطعم" to listOf("مطاعم", "بوفيه", "كافتيريا", "بروست", "مندي", "مظبي", "كافيه", "وجبات"),
        "كافيه" to listOf("مقهى", "كافتيريا", "مطعم"),
        "مستشفى" to listOf("مستوصف", "عيادة", "مركز صحي", "طبي"),
        "صيدلية" to listOf("دواء", "علاج"),
        "ماطور ماء" to listOf("مضخة مياه", "طلمبة مياه"),
        "ماطور" to listOf("مضخة", "موتور"),
        "بنشر" to listOf("إصلاح إطارات", "تصليح كفرات", "كفرات"),
        "بوفيه" to listOf("مطعم", "كافتيريا", "وجبات سريعة")
    )
    private var normalized: List<Pair<String, List<String>>> = build(entries)

    private fun build(m: Map<String, List<String>>) =
        m.map { (k, v) -> ArabicNormalizer.normalize(k) to v.map { ArabicNormalizer.normalize(it) } }
            .sortedByDescending { it.first.length }                     // الأطول أولاً (ماطور ماء قبل ماطور)

    /** لتحديث القاموس من Firestore لاحقاً. */
    fun replace(newEntries: Map<String, List<String>>) { entries = newEntries; normalized = build(newEntries) }

    /** الاستعلام الأصلي (مطبَّعاً) + كل التراكيب المكافئة. */
    fun expand(query: String): List<String> {
        val q = ArabicNormalizer.normalize(query)
        if (q.isEmpty()) return emptyList()
        val out = linkedSetOf(q)
        for ((k, alts) in normalized) {
            if (q.contains(k)) alts.forEach { a -> out.add(a); out.add(q.replace(k, a).trim()) }
        }
        return out.take(12).toList()
    }
}
