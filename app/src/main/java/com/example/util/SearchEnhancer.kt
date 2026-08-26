package com.example.util

import java.util.Locale

/**
 * 🔍 SearchEnhancer - محرك التعزيز الصوتي والمرادفات والبحث السياقي
 * 
 * الميزات:
 * 1. خوارزمية Soundex المتقدمة والمكيفة للأصوات العربية والإنجليزية.
 * 2. معجم المرادفات المهنية اليمنية (Synonyms Dictionary) لربط المصطلحات الشائعة بالفئات الرسمية.
 * 3. تحليل السياق واستخراج الكلمات المفتاحية مع حساب درجات التطابق (Relevance Scoring).
 */
object SearchEnhancer {

    // قاموس المرادفات المهنية والخدمية باليمن
    private val SYNONYMS_MAP: Map<String, List<String>> = mapOf(
        "سباك" to listOf("سباكة", "مواسير", "تسريب", "خزان", "حنفيات", "مغاسل", "سيفون", "تهريب مياه", "دينمو"),
        "كهربائي" to listOf("كهرباء", "طاقة شمسية", "إنفرتر", "انفرتر", "بطاريات", "قواطع", "تسليك", "لمبات", "مولد"),
        "مكيف" to listOf("تكييف", "تبريد", "فريون", "سبليت", "سبلت", "كمبروسر", "ثلاجة", "غسالة"),
        "بنشر" to listOf("كفرات", "إطارات", "تيوبلس", "رقعة", "ميزان كفرات", "تغيير زيت"),
        "ميكانيكي" to listOf("صيانة سيارات", "محرك", "فرامل", "فحمات", "جير", "فحص كمبيوتر"),
        "نجار" to listOf("أثاث", "خشب", "أبواب", "شبابيك", "مطابخ", "غرف نوم", "تفصيل"),
        "حداد" to listOf("حدادة", "شباك حماية", "أبواب حديد", "مظلات", "هناجر", "لحام"),
        "نقاش" to listOf("دهانات", "بوية", "ديكورات", "جبس", "ورق حائط", "تعتيق"),
        "تنظيف" to listOf("مكافحة حشرات", "رش مبيدات", "جلي بلاط", "تنظيف فلل", "تنظيف خزانات", "مغسلة سجاد"),
        "طبيب" to listOf("دكتور", "عيادة", "مستشفى", "مستوصف", "أخصائي", "استشاري", "تحاليل", "أشعة"),
        "صيدلية" to listOf("أدوية", "علاج", "مستلزمات طبية", "فيتامينات", "حليب أطفال"),
        "مبرمج" to listOf("صيانة جوالات", "تلفونات", "سوفت وير", "فورمات", "برمجة شاشات", "كمبيوتر", "شبكات")
    )

    /**
     * خوارزمية Soundex لتوليد كود التمثيل الصوتي للكلمات
     * تدعم الحروف الإنجليزية مع تطبيع الحروف العربية المتشابهة صوتياً
     * 
     * @param word الكلمة المراد تحويلها
     * @return كود Soundex من 4 خانات (مثل: S120)
     */
    fun soundex(word: String): String {
        if (word.isBlank()) return "0000"
        val normalized = normalizeArabicPhonetics(word.trim())
        if (normalized.isEmpty()) return "0000"

        val firstChar = normalized[0].uppercaseChar()
        val encoded = StringBuilder().append(firstChar)

        var lastCode = getSoundexCode(firstChar)

        for (i in 1 until normalized.length) {
            val code = getSoundexCode(normalized[i])
            if (code != '0' && code != lastCode) {
                encoded.append(code)
                lastCode = code
            } else if (code == '0') {
                lastCode = '0'
            }
            if (encoded.length == 4) break
        }

        while (encoded.length < 4) {
            encoded.append('0')
        }

        return encoded.toString()
    }

    private fun getSoundexCode(c: Char): Char {
        return when (c.uppercaseChar()) {
            'B', 'F', 'P', 'V', 'ف', 'ب' -> '1'
            'C', 'G', 'J', 'K', 'Q', 'S', 'X', 'Z', 'ج', 'ك', 'ق', 'س', 'ص', 'ز', 'ش' -> '2'
            'D', 'T', 'د', 'ت', 'ط', 'ض', 'ظ', 'ذ', 'ث' -> '3'
            'L', 'ل' -> '4'
            'M', 'N', 'م', 'ن' -> '5'
            'R', 'ر' -> '6'
            'H', 'W', 'Y', 'ح', 'ه', 'و', 'ي', 'ع', 'خ', 'غ' -> '7'
            else -> '0'
        }
    }

    /**
     * تطبيع الأصوات المتشابهة في اللهجة واللغة العربية
     */
    fun normalizeArabicPhonetics(text: String): String {
        return text
            .replace("أ", "ا")
            .replace("إ", "ا")
            .replace("آ", "ا")
            .replace("ى", "ي")
            .replace("ة", "ه")
            .replace("ئ", "ي")
            .replace("ؤ", "و")
            .replace("گ", "ج")
            .replace(Regex("[\\u064B-\\u065F]"), "") // إزالة التشكيل
            .lowercase(Locale.ROOT)
    }

    /**
     * الحصول على قائمة المرادفات والكلمات ذات الصلة بالكلمة المدخلة
     * 
     * @param word الكلمة المفتاحية
     * @return قائمة بالمرادفات
     */
    fun getSynonyms(word: String): List<String> {
        val clean = normalizeArabicPhonetics(word)
        val results = mutableSetOf<String>()
        results.add(clean)

        for ((key, synonyms) in SYNONYMS_MAP) {
            val normKey = normalizeArabicPhonetics(key)
            val normSynonyms = synonyms.map { normalizeArabicPhonetics(it) }

            if (normKey.contains(clean) || clean.contains(normKey)) {
                results.add(normKey)
                results.addAll(normSynonyms)
            } else if (normSynonyms.any { it.contains(clean) || clean.contains(it) }) {
                results.add(normKey)
                results.addAll(normSynonyms)
            }
        }

        return results.toList()
    }

    /**
     * حساب درجة التطابق السياقي بين نص الاستعلام والنص المستهدف
     * يجمع بين التطابق التام، المرادفات، التشابه الصوتي (Soundex)، والكلمات المفتاحية
     * 
     * @param query نص البحث
     * @param target النص المراد فحصه
     * @return درجة تطابق بين 0.0 و 1.0
     */
    fun calculateContextualScore(query: String, target: String): Double {
        if (query.isBlank() || target.isBlank()) return 0.0
        val normQuery = normalizeArabicPhonetics(query)
        val normTarget = normalizeArabicPhonetics(target)

        // 1. التطابق التام أو الاحتواء المباشر
        if (normTarget.contains(normQuery)) return 1.0

        val queryWords = normQuery.split(Regex("\\s+")).filter { it.length > 1 }
        val targetWords = normTarget.split(Regex("\\s+")).filter { it.length > 1 }
        if (queryWords.isEmpty() || targetWords.isEmpty()) return 0.0

        var matchedScore = 0.0

        for (qWord in queryWords) {
            val qSoundex = soundex(qWord)
            val qSynonyms = getSynonyms(qWord)

            var bestWordScore = 0.0

            for (tWord in targetWords) {
                // فحص المرادفات
                if (qSynonyms.contains(tWord) || qSynonyms.any { tWord.contains(it) }) {
                    bestWordScore = maxOf(bestWordScore, 0.85)
                }

                // فحص Soundex
                val tSoundex = soundex(tWord)
                if (qSoundex == tSoundex) {
                    bestWordScore = maxOf(bestWordScore, 0.70)
                }
            }

            matchedScore += bestWordScore
        }

        return (matchedScore / queryWords.size).coerceIn(0.0, 1.0)
    }
}
