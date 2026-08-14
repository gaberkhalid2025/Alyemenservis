package com.example

import com.example.utils.BookingUtils
import com.example.util.SecurityCryptoUtils
import org.junit.Assert.*
import org.junit.Test

class CoreBusinessUnitTests {

    @Test
    fun testBookingNumberGeneration() {
        val bookingNum = BookingUtils.generateBookingNumber("YEMEN")
        assertTrue(bookingNum.startsWith("YEMEN-"))
        assertEquals(3, bookingNum.split("-").size)
    }

    @Test
    fun testBookingPasswordGeneration() {
        val pass = BookingUtils.generateBookingPassword(6)
        assertEquals(6, pass.length)
        assertTrue(pass.all { it.isDigit() })
    }

    @Test
    fun testPasswordPolicyValidation() {
        val (shortValid, shortError) = SecurityCryptoUtils.validatePasswordPolicy("123")
        assertFalse(shortValid)
        assertNotNull(shortError)

        val (valid, error) = SecurityCryptoUtils.validatePasswordPolicy("secret123")
        assertTrue(valid)
        assertNull(error)
    }

    @Test
    fun testCategoryMapSerialization() {
        val cat = Category(1, "صيانة كهرباء", "⚡", 2)
        val map = cat.toMap()
        assertEquals(1, map["id"])
        assertEquals("صيانة كهرباء", map["name"])
        assertEquals("⚡", map["icon"])

        val reconstructed = Category.fromMap(map)
        assertEquals(cat.id, reconstructed.id)
        assertEquals(cat.name, reconstructed.name)
        assertEquals(cat.icon, reconstructed.icon)
    }
}
