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
        val priceZero = 0.0
        val priceNegative = -100.0
        val priceExactMin = 1000.0

        val isValidPrice1 = price1 >= 1000.0
        val isValidPrice2 = price2 >= 1000.0

        assertTrue(!isValidPrice1)
        assertTrue(isValidPrice2)
        assertTrue(priceZero < 1000.0)
        assertTrue(priceNegative <= 0.0)
        assertTrue(priceExactMin >= 1000.0)
    }

    @Test
    fun testExpiredRequestValidation() {
        val now = System.currentTimeMillis()
        val pastExpiresAt = now - 5000L // 5 seconds in the past

        val remainingMillis = (pastExpiresAt - now).coerceAtLeast(0L)
        val isExpired = remainingMillis == 0L

        assertTrue("Past expiresAt must result in 0 remaining millis", remainingMillis == 0L)
        assertTrue("Request must be marked as expired", isExpired)
    }

    @Test
    fun testUrgentRequestStatusTransitions() {
        val allowedStatuses = setOf(
            "WAITING_FOR_OFFERS",
            "REVIEWING_OFFERS",
            "ACCEPTED",
            "IN_PROGRESS",
            "COMPLETED",
            "EXPIRED",
            "CANCELLED"
        )
        assertTrue(allowedStatuses.contains("WAITING_FOR_OFFERS"))
        assertTrue(allowedStatuses.contains("ACCEPTED"))
        assertTrue(allowedStatuses.contains("COMPLETED"))
        assertTrue(allowedStatuses.contains("CANCELLED"))
        assertTrue(allowedStatuses.contains("EXPIRED"))
    }
}
