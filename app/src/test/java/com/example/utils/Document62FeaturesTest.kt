package com.example.utils

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.GregorianCalendar
import java.util.TimeZone

class PeakPredictorTest {
    private val tz = TimeZone.getTimeZone("Asia/Aden")
    private fun ms(y: Int, m: Int, d: Int, h: Int): Long =
        GregorianCalendar(tz).apply { set(y, m - 1, d, h, 30, 0) }.timeInMillis

    @Test
    fun insufficientDataIsUnknown() {
        val z = PeakPredictor.predict(
            listOf(BookingStamp(15.35, 44.2, ms(2026, 11, 10, 10))),
            ms(2026, 11, 10, 10),
            tz
        )
        assertEquals(PeakLevel.UNKNOWN, z.single().level)
    }

    @Test
    fun busyHourIsHigh() {
        val tuesdays = listOf(
            ms(2026, 11, 10, 10),
            ms(2026, 11, 3, 10),
            ms(2026, 10, 27, 10),
            ms(2026, 10, 20, 10)
        )
        val stamps = tuesdays.flatMap { t -> (0 until 6).map { BookingStamp(15.351, 44.201, t) } } +
                (0 until 6).map { BookingStamp(15.351, 44.201, ms(2026, 11, 10, 20)) }
        val z = PeakPredictor.predict(stamps, ms(2026, 11, 10, 10), tz)
        assertEquals(PeakLevel.HIGH, z.single().level)
    }
}
