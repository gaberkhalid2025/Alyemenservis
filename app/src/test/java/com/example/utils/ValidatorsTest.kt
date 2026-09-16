package com.example.utils

import org.junit.Assert.*
import org.junit.Test

class ValidatorsTest {
    
    // ============================================
    // 1. اختبارات validateYemenPhone
    // ============================================
    
    @Test
    fun `valid Yemeni phone passes`() {
        assertTrue(Validators.validateYemenPhone("771234567").isValid)
    }
    
    @Test
    fun `empty phone fails`() {
        val result = Validators.validateYemenPhone("")
        assertFalse(result.isValid)
    }
    
    @Test
    fun `null phone fails`() {
        assertFalse(Validators.validateYemenPhone(null).isValid)
    }
    
    @Test
    fun `phone with wrong length fails`() {
        assertFalse(Validators.validateYemenPhone("77123").isValid)
        assertFalse(Validators.validateYemenPhone("7712345678").isValid)
    }
    
    @Test
    fun `phone with wrong prefix fails`() {
        assertFalse(Validators.validateYemenPhone("991234567").isValid)
        assertFalse(Validators.validateYemenPhone("721234567").isValid)
    }
    
    // ============================================
    // 2. اختبارات validatePassword
    // ============================================
    
    @Test
    fun `valid password passes`() {
        assertTrue(Validators.validatePassword("SecurePass123").isValid)
    }
    
    @Test
    fun `short password fails`() {
        assertFalse(Validators.validatePassword("Short1").isValid)
    }
    
    @Test
    fun `password without numbers fails`() {
        assertFalse(Validators.validatePassword("OnlyLetters").isValid)
    }
    
    @Test
    fun `password without letters fails`() {
        assertFalse(Validators.validatePassword("12345678").isValid)
    }
    
    @Test
    fun `weak password fails`() {
        assertFalse(Validators.validatePassword("123456").isValid)
        assertFalse(Validators.validatePassword("password").isValid)
    }
    
    // ============================================
    // 3. اختبارات validateName
    // ============================================
    
    @Test
    fun `valid name passes`() {
        assertTrue(Validators.validateName("علي محمد").isValid)
    }
    
    @Test
    fun `short name fails`() {
        assertFalse(Validators.validateName("عل").isValid)
    }
    
    @Test
    fun `empty name fails`() {
        assertFalse(Validators.validateName("").isValid)
    }
}

class ProviderValidatorTest {
    
    @Test
    fun `valid provider passes`() {
        val result = ProviderValidator.validate("علي محمد", "771234567", "سباكة")
        assertTrue(result.isValid)
    }
    
    @Test
    fun `provider without name fails`() {
        assertFalse(ProviderValidator.validate("", "771234567", "سباكة").isValid)
    }
    
    @Test
    fun `provider without phone fails`() {
        assertFalse(ProviderValidator.validate("علي", "", "سباكة").isValid)
    }
    
    @Test
    fun `provider without category fails`() {
        assertFalse(ProviderValidator.validate("علي", "771234567", "").isValid)
    }
}
