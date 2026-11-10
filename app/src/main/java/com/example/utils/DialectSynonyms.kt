package com.example.utils

object DialectSynonyms {
    /** مفتاح: كما يكتبه الناس (قبل التطبيع) ← بدائل مسجلة. أضف مصطلحاتك بعد التحقق من معناها. */
    @Volatile private var entries: Map<String, List<String>> = mapOf(
        "ماطور ماء" to listOf("مضخة مياه", "طلمبة مياه"),
        "ماطور" to listOf("مضخة", "موتور"),
        "بنشر" to listOf("إصلاح إطارات", "تصليح كفرات", "كفرات"),
        "بوفيه" to listOf("مطعم", "كافتيريا", "وجبات سريعة")
        // "كنداكتر" to listOf("...")   ← أضفها بمعناها المحلي
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
        return out.take(8).toList()
    }
}
