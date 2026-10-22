package com.example.utils

import com.example.ui.screens.map.utils.MapDistanceCalculator
import com.example.ui.screens.map.utils.TransportMode
import org.junit.Assert.*
import org.junit.Test

class MapDistanceCalculatorTest {
    
    // ============================================
    // 1. اختبارات حساب المسافة (Haversine)
    // ============================================
    
    @Test
    fun `same location returns 0 meters`() {
        val distance = MapDistanceCalculator.calculateDistanceMeters(
            15.3694, 44.1910, // صنعاء
            15.3694, 44.1910  // نفس النقطة
        )
        assertEquals(0.0, distance, 0.01)
    }
    
    @Test
    fun `Sanaa to Aden is approximately 300 km`() {
        val distance = MapDistanceCalculator.calculateDistanceMeters(
            15.3694, 44.1910, // صنعاء
            12.7855, 45.0187  // عدن
        )
        val distanceKm = distance / 1000.0
        assertTrue("Expected ~300km, got $distanceKm", distanceKm in 280.0..320.0)
    }
    
    @Test
    fun `Sanaa to Taiz is approximately 200 to 250 km`() {
        val distance = MapDistanceCalculator.calculateDistanceMeters(
            15.3694, 44.1910, // صنعاء
            13.5789, 44.0219  // تعز
        )
        val distanceKm = distance / 1000.0
        assertTrue("Expected ~200-250km, got $distanceKm", distanceKm in 190.0..280.0)
    }
    
    // ============================================
    // 2. اختبارات formatDistance
    // ============================================
    
    @Test
    fun `formatDistance for small distance returns meters`() {
        val result = MapDistanceCalculator.formatDistance(500.0)
        assertTrue(result.contains("م"))
        assertFalse(result.contains("كم"))
    }
    
    @Test
    fun `formatDistance for large distance returns km`() {
        val result = MapDistanceCalculator.formatDistance(2500.0)
        assertTrue(result.contains("كم"))
    }
    
    @Test
    fun `formatDistance for exactly 1000m returns 1 km`() {
        val result = MapDistanceCalculator.formatDistance(1000.0)
        assertTrue(result.contains("كم"))
    }
    
    // ============================================
    // 3. اختبارات computeEta
    // ============================================
    
    @Test
    fun `computeEta for walking distance is reasonable`() {
        val result = MapDistanceCalculator.computeEta(500.0, TransportMode.WALKING)
        assertTrue(result.contains("سيراً") || result.contains("دقيقة"))
    }
    
    @Test
    fun `computeEta for driving distance is reasonable`() {
        val result = MapDistanceCalculator.computeEta(50000.0, TransportMode.CAR)
        assertTrue(result.contains("بالسيارة") || result.contains("دقيقة"))
    }
    
    // ============================================
    // 4. اختبارات TransportMode
    // ============================================
    
    @Test
    fun `WALKING speed is 3_5 km per hour`() {
        assertEquals(3.5, TransportMode.WALKING.speedKmH, 0.01)
    }
    
    @Test
    fun `CAR speed is 35 km per hour`() {
        assertEquals(35.0, TransportMode.CAR.speedKmH, 0.01)
    }
}
