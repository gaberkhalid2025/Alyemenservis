package com.example.utils

import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.TimeUnit

class BookingUtilsTest {
    
    // ============================================
    // 1. اختبارات generateBookingNumber
    // ============================================
    
    @Test
    fun `generateBookingNumber starts with BK`() {
        val number = BookingUtils.generateBookingNumber()
        assertTrue(number.startsWith("BK-"))
    }
    
    @Test
    fun `generateBookingNumber has correct format`() {
        val number = BookingUtils.generateBookingNumber()
        val parts = number.split("-")
        assertEquals(3, parts.size)
        assertEquals("BK", parts[0])
        assertEquals(12, parts[1].length)
        assertEquals(4, parts[2].length)
    }
    
    @Test
    fun `generated numbers are unique`() {
        val numbers = (1..50).map { BookingUtils.generateBookingNumber() }
        assertEquals(50, numbers.distinct().size)
    }
    
    // ============================================
    // 2. اختبارات generateBookingPassword
    // ============================================
    
    @Test
    fun `generateBookingPassword returns 4 digits by default`() {
        val password = BookingUtils.generateBookingPassword()
        assertEquals(4, password.length)
        assertTrue(password.all { it.isDigit() })
    }
    
    @Test
    fun `generateBookingPassword with length 6`() {
        val password = BookingUtils.generateBookingPassword(6)
        assertEquals(6, password.length)
    }
    
    // ============================================
    // 3. اختبارات canModifyOrCancelBooking (8-hour rule)
    // ============================================
    
    @Test
    fun `can modify booking with more than 8 hours`() {
        val futureTime = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(10)
        assertTrue(BookingUtils.canModifyOrCancelBooking(futureTime))
    }
    
    @Test
    fun `cannot modify booking with less than 8 hours`() {
        val nearFuture = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(5)
        assertFalse(BookingUtils.canModifyOrCancelBooking(nearFuture))
    }
    
    @Test
    fun `cannot modify past booking`() {
        val pastTime = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1)
        assertFalse(BookingUtils.canModifyOrCancelBooking(pastTime))
    }
    
    // ============================================
    // 4. اختبارات isValidYemeniPhone
    // ============================================
    
    @Test
    fun `valid Yemeni phone passes`() {
        assertTrue(BookingUtils.isValidYemeniPhone("771234567"))
        assertTrue(BookingUtils.isValidYemeniPhone("731234567"))
    }
    
    @Test
    fun `invalid phone fails`() {
        assertFalse(BookingUtils.isValidYemeniPhone("123"))
        assertFalse(BookingUtils.isValidYemeniPhone(""))
    }
    
    // ============================================
    // 5. اختبارات formatRemainingCancellationTime
    // ============================================
    
    @Test
    fun `formatRemainingCancellationTime returns correct text`() {
        val futureTime = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(10)
        val text = BookingUtils.formatRemainingCancellationTime(futureTime)
        assertTrue(text.contains("ساعة") || text.contains("دقيقة"))
    }
    
    @Test
    fun `formatRemainingCancellationTime for expired returns expired text`() {
        val pastTime = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1)
        val text = BookingUtils.formatRemainingCancellationTime(pastTime)
        assertTrue(text.contains("انتهت"))
    }
    
    // ============================================
    // 6. اختبارات validateBookingForm
    // ============================================
    
    @Test
    fun `valid form passes`() {
        val result = BookingUtils.validateBookingForm(
            clientName = "علي محمد",
            phone = "771234567",
            service = "سباكة",
            date = "2026-12-31"
        )
        assertTrue(result.first)
    }
    
    @Test
    fun `empty name fails`() {
        val result = BookingUtils.validateBookingForm("", "771234567", "سباكة", "2026-12-31")
        assertFalse(result.first)
    }
    
    @Test
    fun `validateBookingForm with invalid phone fails`() {
        val result = BookingUtils.validateBookingForm("علي", "123", "سباكة", "2026-12-31")
        assertFalse(result.first)
    }
}
