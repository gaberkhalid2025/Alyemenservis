package com.example.utils

/**
 * 💬 ChatTypeResolver
 * تحديد نوع قناة الدردشة حسب نوع المتجر أو النشاط (Document 75)
 */
object ChatTypeResolver {
    fun forStore(sectionId: String, categoryId: String, name: String): String {
        val t = "${sectionId.lowercase()} ${categoryId.lowercase()} $name"
        return when {
            listOf("medical", "pharm", "clinic", "طبي", "صيدلية", "مستشفى").any { it in t } -> "MEDICAL"
            listOf("restaurant", "cafe", "مطعم", "كافيه", "بوفيه").any { it in t } -> "RESTAURANT"
            listOf("mall", "مول", "مركز تجاري").any { it in t } -> "MALL"
            else -> "STORE"
        }
    }
}
