package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UrgentViewModelTest {

    @Test
    fun testCountdownCalculation() {
        val now = System.currentTimeMillis()
        val expiresAt = now + 30 * 60 * 1000L // after 30 mins
        val totalDuration = 30 * 60 * 1000L

        val remainingMillis = (expiresAt - now).coerceAtLeast(0L)
        val remainingSeconds = remainingMillis / 1000L
        val minutes = remainingSeconds / 60
        val seconds = remainingSeconds % 60

        assertEquals(30, minutes)
        assertEquals(0, seconds)
        assertTrue(remainingMillis > 0)
    }

    @Test
    fun testOfferPriceConstraintValidation() {
        val price1 = 500.0
        val price2 = 1200.0

        val isValidPrice1 = price1 >= 1000.0
        val isValidPrice2 = price2 >= 1000.0

        assertTrue(!isValidPrice1)
        assertTrue(isValidPrice2)
    }
}
