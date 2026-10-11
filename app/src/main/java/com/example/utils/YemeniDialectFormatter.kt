package com.example.utils

object YemeniDialectFormatter {
    fun format(text: String): String {
        if (text.contains("777644") || text.contains("طوارئ") || text.contains("دفاع مدني")) return text
        var formatted = text.trim()
        if (!formatted.startsWith("حياك") && !formatted.startsWith("أبشر") && !formatted.startsWith("ابشر")) {
            formatted = "حياك الله يا غالي، $formatted"
        }
        return formatted
    }
}
