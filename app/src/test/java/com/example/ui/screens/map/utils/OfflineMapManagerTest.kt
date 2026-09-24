package com.example.ui.screens.map.utils

import org.junit.Assert.*
import org.junit.Test

class OfflineMapManagerTest {

    @Test
    fun testMajorYemeniCitiesList() {
        val cities = OfflineMapManager.MAJOR_YEMENI_CITIES
        assertTrue("Cities list should contain major Yemeni capitals", cities.size >= 5)

        val cityNames = cities.map { it.nameAr }
        assertTrue(cityNames.any { it.contains("صنعاء") })
        assertTrue(cityNames.any { it.contains("عدن") })
        assertTrue(cityNames.any { it.contains("تعز") })
        assertTrue(cityNames.any { it.contains("الحديدة") })
    }

    @Test
    fun testGetCityCoordinatesSanaa() {
        val sanaa = OfflineMapManager.getCityCoordinates("صنعاء")
        assertEquals(15.3694, sanaa.latitude, 0.001)
        assertEquals(44.1910, sanaa.longitude, 0.001)
    }

    @Test
    fun testGetCityCoordinatesAden() {
        val aden = OfflineMapManager.getCityCoordinates("عدن")
        assertEquals(12.7855, aden.latitude, 0.001)
        assertEquals(45.0187, aden.longitude, 0.001)
    }

    @Test
    fun testGetCityCoordinatesFallback() {
        val fallback = OfflineMapManager.getCityCoordinates("مدينة غير معروفة")
        // Default fallback should be Sana'a
        assertEquals(15.3694, fallback.latitude, 0.001)
        assertEquals(44.1910, fallback.longitude, 0.001)
    }
}
