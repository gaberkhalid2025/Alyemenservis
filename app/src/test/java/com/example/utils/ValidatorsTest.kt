package com.example.utils

import org.junit.Assert.*
import org.junit.Test

class ValidatorsTest {
    
    @Test
    fun `validateYemenPhone - valid 9 digit number starting with 77`() {
        val result = Validators.validateYemenPhone("771234567")
        assertTrue(result.isValid)
    }
    
    @Test
    fun `validateYemenPhone - invalid short number`() {
        val result = Validators.validateYemenPhone("123456")
        assertFalse(result.isValid)
    }
    
    @Test
    fun `validateYemenPhone - valid prefixes 73, 71, 70, 78`() {
        assertTrue(Validators.validateYemenPhone("731234567").isValid)
        assertTrue(Validators.validateYemenPhone("711234567").isValid)
        assertTrue(Validators.validateYemenPhone("701234567").isValid)
        assertTrue(Validators.validateYemenPhone("781234567").isValid)
    }
    
    @Test
    fun `validateYemenPhone - invalid prefix 72`() {
        val result = Validators.validateYemenPhone("721234567")
        assertFalse(result.isValid)
    }
    
    @Test
    fun `validateEmail - valid email`() {
        val result = Validators.validateEmail("test@example.com")
        assertTrue(result.isValid)
    }
    
    @Test
    fun `validateEmail - invalid email without @`() {
        val result = Validators.validateEmail("testexample.com")
        assertFalse(result.isValid)
    }
}
