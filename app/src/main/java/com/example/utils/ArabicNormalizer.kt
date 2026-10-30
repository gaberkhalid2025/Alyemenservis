package com.example.utils

import java.util.Locale

object ArabicNormalizer {
    private val DIACRITICS = Regex("[\\u064B-\\u0652\\u0640]")      // تشكيل + تطويل
    private val ALEF = Regex("[إأآٱ]")
    private val SPACES = Regex("\\s+")

    fun normalize(input: String): String = input.trim().lowercase(Locale.ROOT)
        .replace(DIACRITICS, "")
        .replace(ALEF, "ا")
        .replace('ى', 'ي').replace('ئ', 'ي')
        .replace('ؤ', 'و')
        .replace('ة', 'ه')
        .replace(SPACES, " ")

    /** كلمات مفتاحية للتخزين في Firestore: كل كلمة + بادئاتها (2..10) ليعمل البحث الجزئي. */
    fun buildKeywords(vararg texts: String): List<String> {
        val out = linkedSetOf<String>()
        texts.forEach { t ->
            normalize(t).split(" ").filter { it.length >= 2 }.forEach { w ->
                out.add(w)
                for (len in 2..minOf(10, w.length)) out.add(w.substring(0, len))
            }
        }
        return out.take(100).toList()                       // حد Firestore معقول لحجم المستند
    }
}
