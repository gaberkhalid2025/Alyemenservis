package com.example.domain.usecases

import org.junit.Assert.*
import org.junit.Test

class ValidatePhoneUseCaseTest {
    
    private val useCase = ValidatePhoneUseCase()
    
    // ============================================
    // 1. اختبارات الأرقام الصحيحة (Valid)
    // ============================================
    
    @Test
    fun `77 prefix is valid`() {
        val result = useCase("771234567")
        assertTrue(result.isValid)
        assertTrue(result.errorMessage.isEmpty())
    }
    
    @Test
    fun `73 prefix is valid`() {
        assertTrue(useCase("731234567").isValid)
    }
    
    @Test
    fun `71 prefix is valid`() {
        assertTrue(useCase("711234567").isValid)
    }
    
    @Test
    fun `70 prefix is valid`() {
        assertTrue(useCase("701234567").isValid)
    }
    
    @Test
    fun `78 prefix is valid`() {
        assertTrue(useCase("781234567").isValid)
    }
    
    // ============================================
    // 2. اختبارات الأرقام غير الصحيحة (Invalid)
    // ============================================
    
    @Test
    fun `empty phone is invalid`() {
        val result = useCase("")
        assertFalse(result.isValid)
        assertTrue(result.errorMessage.contains("يرجى إدخال"))
    }
    
    @Test
    fun `phone with 8 digits is invalid`() {
        assertFalse(useCase("77123456").isValid)
    }
    
    @Test
    fun `phone with 10 digits is invalid`() {
        assertFalse(useCase("7712345678").isValid)
    }
    
    @Test
    fun `phone with invalid prefix 79 is invalid`() {
        val result = useCase("791234567")
        assertFalse(result.isValid)
        assertTrue(result.errorMessage.contains("77"))
    }
    
    @Test
    fun `phone with invalid prefix 72 is invalid`() {
        assertFalse(useCase("721234567").isValid)
    }
    
    @Test
    fun `phone with letters is invalid`() {
        assertFalse(useCase("7712345ab").isValid)
    }
    
    // ============================================
    // 3. اختبارات الصيغ المختلفة
    // ============================================
    
    @Test
    fun `phone with +967 prefix is valid`() {
        assertTrue(useCase("+967771234567").isValid)
    }
    
    @Test
    fun `phone with 00967 prefix is valid`() {
        assertTrue(useCase("00967771234567").isValid)
    }
    
    @Test
    fun `phone with 967 prefix is valid`() {
        assertTrue(useCase("967771234567").isValid)
    }
    
    @Test
    fun `phone with leading 0 is valid`() {
        assertTrue(useCase("0771234567").isValid)
    }
    
    @Test
    fun `phone with spaces is valid`() {
        assertTrue(useCase("771 234 567").isValid)
    }
}
