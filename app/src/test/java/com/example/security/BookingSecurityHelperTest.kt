package com.example.security

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 🧪 اختبارات مساعد أمان الحجوزات (BookingSecurityHelper)
 * تغطي تشفير الـ PIN عبر SHA-256، قفل المحاولات بعد 3 مرات، وحجب أرقام الهواتف.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BookingSecurityHelperTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        // Clear prefs for isolation
        context.getSharedPreferences("booking_security_prefs", Context.MODE_PRIVATE).edit().clear().apply()
    }

    @Test
    fun `test hashPin returns consistent sha256 output`() {
        val pin = "1234"
        val hash1 = BookingSecurityHelper.hashPin(pin)

        assertNotNull(hash1)
        assertTrue(hash1.length > 50) // PBKDF2 hash with base64 encoded salt + hash
        assertTrue(hash1.contains(":"))
        assertTrue(BookingSecurityHelper.verifyPassword(pin, hash1))

        // Trailing whitespace is trimmed
        val hashWithSpace = BookingSecurityHelper.hashPin("1234 ")
        assertTrue(BookingSecurityHelper.verifyPassword(pin, hashWithSpace))

        // Blank returns empty
        assertEquals("", BookingSecurityHelper.hashPin(""))
    }

    @Test
    fun `test verifyPassword with plain text and hash`() {
        val pin = "5678"
        val hash = BookingSecurityHelper.hashPin(pin)

        // Matches plain text
        assertTrue(BookingSecurityHelper.verifyPassword(pin, "5678"))
        // Matches hash
        assertTrue(BookingSecurityHelper.verifyPassword(pin, hash))
        // Rejects wrong input
        assertFalse(BookingSecurityHelper.verifyPassword("9999", hash))
        // Rejects empty
        assertFalse(BookingSecurityHelper.verifyPassword("", hash))
    }

    @Test
    fun `test recordFailedAttempt locks booking after 3 failures`() {
        val bookingId = "bk_sec_test_1"
        BookingSecurityHelper.resetAttempts(context, bookingId)

        // Initially not locked
        assertFalse(BookingSecurityHelper.isBookingLocked(context, bookingId))

        // Attempt 1: 2 remaining
        val remaining1 = BookingSecurityHelper.recordFailedAttempt(context, bookingId)
        assertEquals(2, remaining1)
        assertFalse(BookingSecurityHelper.isBookingLocked(context, bookingId))

        // Attempt 2: 1 remaining
        val remaining2 = BookingSecurityHelper.recordFailedAttempt(context, bookingId)
        assertEquals(1, remaining2)
        assertFalse(BookingSecurityHelper.isBookingLocked(context, bookingId))

        // Attempt 3: 0 remaining -> LOCKOUT!
        val remaining3 = BookingSecurityHelper.recordFailedAttempt(context, bookingId)
        assertEquals(0, remaining3)
        assertTrue(BookingSecurityHelper.isBookingLocked(context, bookingId))

        // Remaining lockout seconds should be greater than 0
        val remainingSecs = BookingSecurityHelper.getRemainingLockoutSeconds(context, bookingId)
        assertTrue(remainingSecs > 0)
    }

    @Test
    fun `test resetAttempts unlocks booking and clears failure count`() {
        val bookingId = "bk_sec_test_2"
        BookingSecurityHelper.recordFailedAttempt(context, bookingId)
        BookingSecurityHelper.recordFailedAttempt(context, bookingId)
        BookingSecurityHelper.recordFailedAttempt(context, bookingId)
        assertTrue(BookingSecurityHelper.isBookingLocked(context, bookingId))

        // Reset
        BookingSecurityHelper.resetAttempts(context, bookingId)
        assertFalse(BookingSecurityHelper.isBookingLocked(context, bookingId))
    }

    @Test
    fun `test maskPhoneNumber masks middle digits`() {
        val phone = "771234567"
        val masked = BookingSecurityHelper.maskPhoneNumber(phone)
        assertEquals("77****567", masked)

        val shortPhone = "123"
        assertEquals("***", BookingSecurityHelper.maskPhoneNumber(shortPhone))
    }
}
