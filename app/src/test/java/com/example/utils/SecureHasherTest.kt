package com.example.utils

import org.junit.Assert.*
import org.junit.Test

class SecureHasherTest {
    
    @Test
    fun `hashPin - produces consistent hash`() {
        val pin = "1234"
        val salt = "FixedSaltForTest".toByteArray()
        val hash1 = SecureHasher.hashPin(pin, salt)
        val hash2 = SecureHasher.hashPin(pin, salt)
        assertEquals(hash1, hash2)
    }
    
    @Test
    fun `verifyPin - correct pin returns true`() {
        val pin = "1234"
        val hash = SecureHasher.hashPin(pin)
        assertTrue(SecureHasher.verifyPin(pin, hash))
    }
    
    @Test
    fun `verifyPin - wrong pin returns false`() {
        val pin = "1234"
        val hash = SecureHasher.hashPin(pin)
        assertFalse(SecureHasher.verifyPin("5678", hash))
    }
    
    @Test
    fun `verifyPassword - correct password returns true`() {
        val password = "MySecurePass123"
        val hash = SecureHasher.hashPassword(password)
        assertTrue(SecureHasher.verifyPassword(password, hash))
    }
}
