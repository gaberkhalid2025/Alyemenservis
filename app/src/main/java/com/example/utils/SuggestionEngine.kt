package com.example.utils

object SuggestionEngine {
    fun suggest(hour: Int, recent: List<String>, topCategories: List<String>): List<String> {
        val out = linkedSetOf<String>()
        recent.takeLast(3).reversed().forEach { out.add(it) }
        when (hour) {
            in 5..9 -> out.addAll(listOf("مخبز", "بقالة"))
            in 11..14 -> out.addAll(listOf("مطعم", "بوفيه"))
            in 18..22 -> out.addAll(listOf("صيدلية", "مطعم"))
            else -> out.add("صيدلية")
        }
        topCategories.take(2).forEach { out.add(it) }
        return out.take(6).toList()
    }
}
