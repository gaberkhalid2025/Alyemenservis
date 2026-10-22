package com.example.ui.screens.map.utils

import org.junit.Assert.*
import org.junit.Test

class MapDistanceCalculatorTest {
    
    @Test
    fun `calculateDistanceMeters - Sanaa to Aden is ~300km`() {
        val sanaa = 15.3694 to 44.1910
        val aden = 12.7855 to 45.0187
        
        val distance = MapDistanceCalculator.calculateDistanceMeters(
            sanaa.first, sanaa.second, aden.first, aden.second
        )
        
        // ~300 km
        assertTrue(distance > 280_000 && distance < 320_000)
    }
    
    @Test
    fun `calculateDistanceMeters - same point returns 0`() {
        val distance = MapDistanceCalculator.calculateDistanceMeters(
            15.3694, 44.1910, 15.3694, 44.1910
        )
        assertEquals(0.0, distance, 1.0)
    }
    
    @Test
    fun `calculateDistanceMeters - zero coordinates returns 0`() {
        val distance = MapDistanceCalculator.calculateDistanceMeters(
            0.0, 0.0, 15.3694, 44.1910
        )
        assertEquals(0.0, distance, 1.0)
    }
    
    @Test
    fun `formatDistance - less than 1km shows meters`() {
        val result = MapDistanceCalculator.formatDistance(500.0)
        assertTrue(result.contains("500") && result.contains("م"))
    }
    
    @Test
    fun `formatDistance - more than 1km shows km`() {
        val result = MapDistanceCalculator.formatDistance(2500.0)
        assertTrue(result.contains("2.5") && result.contains("كم"))
    }
    
    @Test
    fun `computeEta - short distance uses walking`() {
        val result = MapDistanceCalculator.computeEta(500.0)
        assertTrue(result.contains("سيراً"))
    }
    
    @Test
    fun `computeEta - long distance uses car`() {
        val result = MapDistanceCalculator.computeEta(15_000.0)
        assertTrue(result.contains("بالسيارة"))
    }
}
