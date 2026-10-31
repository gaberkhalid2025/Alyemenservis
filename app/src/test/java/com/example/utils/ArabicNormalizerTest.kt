package com.example.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArabicNormalizerTest {
    @Test fun hamzaOnYaMatchesPlainYa() = assertEquals(ArabicNormalizer.normalize("هائل"), ArabicNormalizer.normalize("هايل"))
    @Test fun alefTaMarbutaAndDiacritics() = assertEquals("مدرسه الاميل", ArabicNormalizer.normalize("مَدرسةُ الأميل"))
    @Test fun keywordsHavePrefixes() = assertTrue(ArabicNormalizer.buildKeywords("صنعاء").contains("صن"))
}
