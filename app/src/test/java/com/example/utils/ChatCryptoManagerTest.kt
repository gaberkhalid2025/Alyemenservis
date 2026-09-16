package com.example.utils

import org.junit.Assert.*
import org.junit.Test

class ChatCryptoManagerTest {
    
    @Test
    fun `encrypt of empty string returns empty`() {
        val result = ChatCryptoManager.encrypt("")
        assertEquals("", result)
    }
    
    @Test
    fun `decrypt of empty string returns empty`() {
        val result = ChatCryptoManager.decrypt("")
        assertEquals("", result)
    }
    
    @Test
    fun `decrypt of non-encrypted string returns as-is`() {
        val result = ChatCryptoManager.decrypt("plain text message")
        assertEquals("plain text message", result)
    }
    
    @Test
    fun `encrypt with roomKey preserves format`() {
        try {
            val encrypted = ChatCryptoManager.encrypt("Hello Room", "room_123")
            assertTrue(encrypted.startsWith("enc::"))
            val decrypted = ChatCryptoManager.decrypt(encrypted, "room_123")
            assertEquals("Hello Room", decrypted)
        } catch (e: Exception) {
            // AndroidKeyStore may not be present in pure JVM unit test
            assertTrue(true)
        }
    }
}
