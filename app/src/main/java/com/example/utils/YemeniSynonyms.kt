package com.example.utils

object YemeniSynonyms {
    private val synonymMap = mapOf(
        "مواسيرجي" to "سباك",
        "سباكه" to "سباك",
        "صحي" to "سباك",
        "كهربائي" to "كهرباء",
        "وايرات" to "كهرباء",
        "طاقة شمسية" to "طاقة",
        "المنظومه" to "طاقة",
        "دكتور" to "طبيب",
        "عيادة" to "طبيب",
        "مستشفى" to "طبيب",
        "تكييف" to "صيانة",
        "مكيف" to "صيانة",
        "غسالة" to "صيانة",
        "عقار" to "بيت",
        "شقة" to "بيت",
        "إيجار" to "بيت",
        "مغترب" to "خدمة"
    )

    fun expandQuery(query: String): List<String> {
        val trimmed = query.trim().lowercase()
        val results = mutableSetOf(trimmed)
        synonymMap.forEach { (key, value) ->
            if (trimmed.contains(key)) {
                results.add(value)
            }
            if (trimmed.contains(value)) {
                results.add(key)
            }
        }
        return results.toList()
    }
}
