package com.example.ui.screens.map.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 🧪 MapDistanceCalculatorTest
 * Unit tests verifying Haversine calculations, distance formatting, and ETA logic.
 */
class MapDistanceCalculatorTest {

    @Test
    fun calculateDistance_samePoint_returnsZero() {
        val dist = MapDistanceCalculator.calculateDistanceMeters(15.3694, 44.1910, 15.3694, 44.1910)
        assertEquals(0.0, dist, 0.001)
    }

    @Test
    fun calculateDistance_zeroCoords_returnsZero() {
        val dist = MapDistanceCalculator.calculateDistanceMeters(0.0, 0.0, 15.3694, 44.1910)
        assertEquals(0.0, dist, 0.001)
    }

    @Test
    fun calculateDistance_sanaaToAden_isApproxCorrect() {
        // Distance between Sana'a (~15.3694, 44.1910) and Aden (~12.7855, 45.0187) is roughly 300km
        val distMeters = MapDistanceCalculator.calculateDistanceMeters(15.3694, 44.1910, 12.7855, 45.0187)
        val distKm = distMeters / 1000.0
        assertTrue("Distance should be between 280km and 320km", distKm in 280.0..320.0)
    }

    @Test
    fun formatDistance_underOneKm_formatsMeters() {
        val formatted = MapDistanceCalculator.formatDistance(450.0)
        assertEquals("450 م", formatted)
    }

    @Test
    fun formatDistance_overOneKm_formatsKilometers() {
        val formatted = MapDistanceCalculator.formatDistance(3500.0)
        assertEquals("3.5 كم", formatted)
    }

    @Test
    fun computeEta_shortDistance_returnsWalkingEta() {
        val eta = MapDistanceCalculator.computeEta(500.0)
        assertTrue(eta.contains("سيراً"))
    }

    @Test
    fun computeEta_longDistance_returnsDrivingEta() {
        val eta = MapDistanceCalculator.computeEta(15000.0)
        assertTrue(eta.contains("بالسيارة"))
    }

    @Test
    fun getCityCoordinates_returnsSanaaByDefault() {
        val coords = OfflineMapManager.getCityCoordinates("مدينة غير معروفة")
        assertEquals("صنعاء", coords.nameAr)
        assertEquals(15.3694, coords.latitude, 0.001)
    }

    @Test
    fun getCityCoordinates_findsAdenCorrectly() {
        val coords = OfflineMapManager.getCityCoordinates("عدن")
        assertEquals("عدن", coords.nameAr)
        assertEquals(12.7855, coords.latitude, 0.001)
    }
}
