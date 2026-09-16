package com.example.utils

import org.junit.Assert.*
import org.junit.Test

class SecureHasherTest {
    
    // ============================================
    // 1. اختبارات hashPassword
    // ============================================
    
    @Test
    fun `hashPassword returns non-empty string`() {
        val hash = SecureHasher.hashPassword("password123")
        assertNotNull(hash)
        assertTrue(hash.isNotEmpty())
    }
    
    @Test
    fun `hashPassword contains salt and hash separator`() {
        val hash = SecureHasher.hashPassword("password123")
        assertTrue(hash.contains(":"))
    }
    
    @Test
    fun `hashPassword produces different hashes for same input`() {
        val hash1 = SecureHasher.hashPassword("password123")
        val hash2 = SecureHasher.hashPassword("password123")
        // لأن الـ salt عشوائي
        assertNotEquals(hash1, hash2)
    }
    
    @Test
    fun `hashPassword produces different hashes for different inputs`() {
        val hash1 = SecureHasher.hashPassword("password1")
        val hash2 = SecureHasher.hashPassword("password2")
        assertNotEquals(hash1, hash2)
    }
    
    // ============================================
    // 2. اختبارات verifyPassword
    // ============================================
    
    @Test
    fun `verifyPassword returns true for correct password`() {
        val hash = SecureHasher.hashPassword("password123")
        assertTrue(SecureHasher.verifyPassword("password123", hash))
    }
    
    @Test
    fun `verifyPassword returns false for wrong password`() {
        val hash = SecureHasher.hashPassword("password123")
        assertFalse(SecureHasher.verifyPassword("wrongpassword", hash))
    }
    
    @Test
    fun `verifyPassword is case sensitive`() {
        val hash = SecureHasher.hashPassword("Password123")
        assertFalse(SecureHasher.verifyPassword("password123", hash))
    }
    
    @Test
    fun `verifyPassword with empty inputs returns false`() {
        assertFalse(SecureHasher.verifyPassword("", "somehash"))
        assertFalse(SecureHasher.verifyPassword("password", ""))
    }
    
    // ============================================
    // 3. اختبارات hashPin
    // ============================================
    
    @Test
    fun `hashPin returns non-empty string`() {
        val hash = SecureHasher.hashPin("1234")
        assertNotNull(hash)
        assertTrue(hash.isNotEmpty())
    }
    
    @Test
    fun `verifyPin returns true for correct PIN`() {
        val hash = SecureHasher.hashPin("1234")
        assertTrue(SecureHasher.verifyPin("1234", hash))
    }
    
    @Test
    fun `verifyPin returns false for wrong PIN`() {
        val hash = SecureHasher.hashPin("1234")
        assertFalse(SecureHasher.verifyPin("5678", hash))
    }
    
    // ============================================
    // 4. اختبارات Backward Compatibility
    // ============================================
    
    @Test
    fun `verifyPassword supports legacy plain text`() {
        // إذا كانت كلمة المرور مخزنة كـ plain text (قديم)
        assertTrue(SecureHasher.verifyPassword("plainpass", "plainpass"))
    }
    
    @Test
    fun `verifyPassword supports salt-hash format`() {
        val hash = SecureHasher.hashPassword("test123")
        assertTrue(SecureHasher.verifyPassword("test123", hash))
    }
}
