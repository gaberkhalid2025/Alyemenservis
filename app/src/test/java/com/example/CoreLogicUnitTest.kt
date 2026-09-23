package com.example

import com.example.utils.BookingStateMachine
import com.example.utils.PaymentSecurityGuard
import com.example.utils.SecurityCryptoUtils
import org.junit.Assert.*
import org.junit.Test

/**
 * 🧪 Core Business Logic Unit Tests Suite
 * Solves Problem 9: Unit testing covering password policy validation, booking state transitions,
 * encryption/decryption integrity, and payment security checks.
 */
class CoreLogicUnitTest {

    @Test
    fun testPasswordPolicyValidation() {
        val shortResult = SecurityCryptoUtils.validatePasswordPolicy("123")
        assertFalse("Short passwords should fail validation", shortResult.first)

        val weakResult = SecurityCryptoUtils.validatePasswordPolicy("12345678")
        assertFalse("Common weak passwords should fail validation", weakResult.first)

        val strongResult = SecurityCryptoUtils.validatePasswordPolicy("StrongPass#2026!")
        assertTrue("Strong passwords should pass validation", strongResult.first)
    }

    @Test
    fun testEncryptionDecryptionIntegrity() {
        val originalText = "Yemen_Services_Token_77777777"
        val encrypted = SecurityCryptoUtils.encrypt(originalText)
        val decrypted = SecurityCryptoUtils.decrypt(encrypted)

        assertNotEquals("Encrypted text should not equal plain text", originalText, encrypted)
        assertEquals("Decrypted text must match original plain text", originalText, decrypted)
    }

    @Test
    fun testBookingStateEngineTransitions() {
        // Pending -> Accepted should be valid
        assertTrue(BookingStateMachine.canTransition("PENDING", "ACCEPTED"))

        // Accepted -> In Progress should be allowed
        assertTrue(BookingStateMachine.canTransition("ACCEPTED", "IN_PROGRESS"))

        // Completed is terminal state
        assertFalse(BookingStateMachine.canTransition("COMPLETED", "ACCEPTED"))
        assertFalse(BookingStateMachine.canTransition("REJECTED", "ACCEPTED"))
    }

    @Test
    fun testPaymentSecurityGuardValidation() {
        val invalidResult = PaymentSecurityGuard.verifyTransactionDetails(
            bookingId = "",
            amount = 0.0,
            beneficiaryId = "",
            receiptNumber = "12"
        )
        assertFalse("Invalid transaction details must be rejected", invalidResult.isValid)

        val validResult = PaymentSecurityGuard.verifyTransactionDetails(
            bookingId = "BOOKING_1001",
            amount = 15000.0,
            beneficiaryId = "STORE_500",
            receiptNumber = "REC-998877"
        )
        assertTrue("Valid transaction details must pass validation", validResult.isValid)
    }
}
