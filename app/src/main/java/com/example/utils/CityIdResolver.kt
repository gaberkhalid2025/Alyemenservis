package com.example.utils

/**
 * 🗺️ CityIdResolver
 * محلل معرّفات المدن والمحافظات اليمنية دون فرض قسري لصنعاء (Document 74)
 */
object CityIdResolver {
    /** يعيد معرّف المدينة النظامي من اسم المحافظة/المدينة، أو null إن لم يُعرف (لا افتراض صنعاء أبداً). */
    fun resolve(rawName: String?, knownIds: Map<String, String>): String? {
        val n = ArabicNormalizer.normalize(rawName.orEmpty())
        if (n.length < 2) return null
        knownIds[n]?.let { return it }
        return knownIds.entries.firstOrNull { n.contains(it.key) || it.key.contains(n) }?.value
    }
}
