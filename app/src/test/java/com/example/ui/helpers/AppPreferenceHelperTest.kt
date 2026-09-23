package com.example.ui.helpers

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 🧪 اختبارات تطبيع أرقام الهواتف اليمنية (normalizePhoneNumber)
 * وفق متطلبات TESTING_PLAN.md وقواعد المشروع الصارمة.
 */
class AppPreferenceHelperTest {

    @Test
    fun `test standard 9 digits phone number remains unchanged`() {
        val input = "771234567"
        val output = AppPreferenceHelper.normalizePhoneNumber(input)
        assertEquals("771234567", output)
    }

    @Test
    fun `test international prefix 00967 is stripped`() {
        val input = "00967771234567"
        val output = AppPreferenceHelper.normalizePhoneNumber(input)
        assertEquals("771234567", output)
    }

    @Test
    fun `test international prefix plus 967 is stripped`() {
        val input = "+967771234567"
        val output = AppPreferenceHelper.normalizePhoneNumber(input)
        assertEquals("771234567", output)
    }

    @Test
    fun `test direct country code 967 without plus is stripped`() {
        val input = "967771234567"
        val output = AppPreferenceHelper.normalizePhoneNumber(input)
        assertEquals("771234567", output)
    }

    @Test
    fun `test local 10 digits with leading zero strips zero`() {
        val input = "0771234567"
        val output = AppPreferenceHelper.normalizePhoneNumber(input)
        assertEquals("771234567", output)
    }

    @Test
    fun `test spaces and formatting characters are filtered out`() {
        val input = "771 234 567"
        val output = AppPreferenceHelper.normalizePhoneNumber(input)
        assertEquals("771234567", output)

        val dashedInput = "+967-73-123-4567"
        val dashedOutput = AppPreferenceHelper.normalizePhoneNumber(dashedInput)
        assertEquals("731234567", dashedOutput)
    }

    @Test
    fun `test empty and blank input returns as is`() {
        assertEquals("", AppPreferenceHelper.normalizePhoneNumber(""))
        assertEquals("   ", AppPreferenceHelper.normalizePhoneNumber("   "))
    }

    @Test
    fun `test short input less than 7 digits returns original`() {
        assertEquals("1234", AppPreferenceHelper.normalizePhoneNumber("1234"))
        assertEquals("999", AppPreferenceHelper.normalizePhoneNumber("999"))
    }

    @Test
    fun `test longer numbers extract last 9 digits`() {
        val longInput = "96700771234567"
        val output = AppPreferenceHelper.normalizePhoneNumber(longInput)
        assertEquals("771234567", output)
    }
}
