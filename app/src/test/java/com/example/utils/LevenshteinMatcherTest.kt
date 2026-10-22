package com.example.utils

import org.junit.Assert.*
import org.junit.Test

class LevenshteinMatcherTest {
    
    @Test
    fun `identical strings have distance 0`() {
        assertEquals(0, LevenshteinMatcher.calculateDistance("hello", "hello"))
    }
    
    @Test
    fun `one char difference has distance 1`() {
        assertEquals(1, LevenshteinMatcher.calculateDistance("hello", "hallo"))
    }
    
    @Test
    fun `Arabic similar words match`() {
        assertTrue(LevenshteinMatcher.isFuzzyMatch("سباك", "سباكة"))
    }
    
    @Test
    fun `Arabic typo matches`() {
        assertTrue(LevenshteinMatcher.isFuzzyMatch("سباك", "سابك"))
    }
    
    @Test
    fun `empty query matches everything`() {
        assertTrue(LevenshteinMatcher.isFuzzyMatch("", "anything"))
    }
    
    @Test
    fun `very different strings do not match`() {
        assertFalse(LevenshteinMatcher.isFuzzyMatch("سباك", "كهربائي"))
    }
    
    @Test
    fun `substring match returns true`() {
        assertTrue(LevenshteinMatcher.isFuzzyMatch("سبا", "سباكة"))
    }
    
    @Test
    fun `maxDistance parameter respected`() {
        assertEquals(3, LevenshteinMatcher.calculateDistance("abc", "xyz"))
        assertFalse(LevenshteinMatcher.isFuzzyMatch("abcdef", "uvwxyz", maxDistance = 2))
    }
}
