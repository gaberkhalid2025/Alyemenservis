package com.example.utils

import org.junit.Assert.*
import org.junit.Test

class HolidayManagerTest {

    @Test
    fun testOfficialYemeniHolidays() {
        val (isHolidayUnity, nameUnity) = HolidayManager.isDateHoliday("2026-05-22")
        assertTrue("May 22 is Yemeni Unity Day", isHolidayUnity)
        assertNotNull(nameUnity)
        assertTrue(nameUnity!!.contains("عيد الوحدة"))

        val (isHolidaySep, nameSep) = HolidayManager.isDateHoliday("2026-09-26")
        assertTrue("Sept 26 is Yemeni Revolution Day", isHolidaySep)
        assertNotNull(nameSep)
        assertTrue(nameSep!!.contains("26 سبتمبر"))
    }

    @Test
    fun testFridayWeekendHoliday() {
        // 2026-09-25 is a Friday
        val (isFriday, fridayName) = HolidayManager.isDateHoliday("2026-09-25")
        assertTrue("Friday should be recognized as weekly holiday", isFriday)
        assertTrue(fridayName?.contains("الجمعة") == true)
    }

    @Test
    fun testCustomProviderHoliday() {
        val providerId = "prov_vacation_1"
        val vacationDate = "2026-10-15"

        HolidayManager.addCustomProviderHoliday(providerId, vacationDate)
        val (isVacation, vacationMsg) = HolidayManager.isDateHoliday(vacationDate, providerId)
        assertTrue("Provider custom vacation should be a holiday", isVacation)
        assertTrue(vacationMsg?.contains("إجازة خاصة") == true)

        HolidayManager.removeCustomProviderHoliday(providerId, vacationDate)
    }

    @Test
    fun testRegularWorkdayIsNotHoliday() {
        // 2026-10-21 is a Wednesday and not a fixed holiday
        val (isHoliday, _) = HolidayManager.isDateHoliday("2026-10-21")
        assertFalse("Normal Wednesday is not a holiday", isHoliday)
    }
}
