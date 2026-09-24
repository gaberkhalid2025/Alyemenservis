package com.example.utils

import com.example.data.BookingEntity
import org.junit.Assert.*
import org.junit.Test

class ScheduleManagerTest {

    @Test
    fun testProviderWorkingHoursConfiguration() {
        val customHours = ScheduleManager.ProviderWorkingHours(
            startHour = 9,
            endHour = 17,
            breakStartHour = 12,
            breakEndHour = 13,
            slotDurationHours = 1
        )
        ScheduleManager.setProviderWorkingHours("p_test_1", customHours)
        val retrieved = ScheduleManager.getProviderWorkingHours("p_test_1")

        assertEquals(9, retrieved.startHour)
        assertEquals(17, retrieved.endHour)
        assertEquals(12, retrieved.breakStartHour)
        assertEquals(13, retrieved.breakEndHour)
    }

    @Test
    fun testGenerateAvailableSlotsWithBookings() {
        // Date on a non-holiday weekday (e.g. Wednesday 2026-10-21)
        val testDate = "2026-10-21"
        val providerId = "p_schedule_test"

        val customHours = ScheduleManager.ProviderWorkingHours(
            startHour = 9,
            endHour = 12,
            breakStartHour = 14,
            breakEndHour = 15,
            slotDurationHours = 1
        )
        ScheduleManager.setProviderWorkingHours(providerId, customHours)

        val existingBooking = BookingEntity(
            id = "b1",
            providerId = providerId,
            date = testDate,
            time = "09:00 ص",
            status = "CONFIRMED"
        )

        val slots = ScheduleManager.generateAvailableSlots(
            dateString = testDate,
            providerId = providerId,
            existingBookings = listOf(existingBooking)
        )

        assertTrue(slots.isNotEmpty())
        val slot9 = slots.find { it.hour24 == 9 }
        assertNotNull(slot9)
        assertFalse("Slot 9:00 AM should be marked booked", slot9!!.isAvailable)
        assertEquals("محجوز مسبقاً", slot9.reasonIfNotAvailable)

        val slot10 = slots.find { it.hour24 == 10 }
        assertNotNull(slot10)
        assertTrue("Slot 10:00 AM should be available", slot10!!.isAvailable)
    }

    @Test
    fun testFormatHour12() {
        assertEquals("12:00 ص", ScheduleManager.formatHour12(0))
        assertEquals("08:00 ص", ScheduleManager.formatHour12(8))
        assertEquals("12:00 م", ScheduleManager.formatHour12(12))
        assertEquals("04:00 م", ScheduleManager.formatHour12(16))
    }
}
