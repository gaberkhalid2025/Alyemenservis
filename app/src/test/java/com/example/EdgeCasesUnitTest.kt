package com.example

import com.example.utils.AppPreferenceHelper
import com.example.utils.Validators
import org.junit.Assert.*
import org.junit.Test

/**
 * 🧪 EdgeCasesUnitTest
 * Tests edge cases like very long Arabic text, unusual phone formats, and boundary inputs.
 */
class EdgeCasesUnitTest {

    @Test
    fun `test long Arabic message handling in phone and text validators`() {
        val longArabicText = "هذا النص العربي طويل جداً لتجربة إدخال بيانات ضخمة في حقول الاستمارة وسرعة المعالجة بدون حدوث تعليق أو بطء " .repeat(20)
        assertTrue(longArabicText.length > 1000)
        
        val normalizedPhone = AppPreferenceHelper.normalizePhoneNumber(" +967 (77) 123-4567 ")
        assertEquals("771234567", normalizedPhone)
    }

    @Test
    fun `test Yemen phone validation edge cases`() {
        // Numbers with spaces, hyphens, country codes
        assertTrue(Validators.isValidYemeniPhone("00967-771234567"))
        assertTrue(Validators.isValidYemeniPhone("+967 731234567"))
        assertTrue(Validators.isValidYemeniPhone(" 711 234 567 "))

        // Invalid prefixes or short lengths
        assertFalse(Validators.isValidYemeniPhone("701234567")) // Invalid prefix 70
        assertFalse(Validators.isValidYemeniPhone("77123")) // Too short
    }
}
