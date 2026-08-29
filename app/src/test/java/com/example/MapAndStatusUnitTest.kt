package com.example

import com.example.data.PropertyEntity
import com.example.data.ProviderEntity
import com.example.data.StoreEntity
import com.example.ui.screens.map.CityAliases
import com.example.ui.screens.map.MapScreenFilters
import com.example.ui.screens.map.utils.MapDistanceCalculator
import com.example.ui.screens.map.utils.TransportMode
import com.example.util.ErrorHandler
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * 🧪 MapAndStatusUnitTest
 * Unit tests covering Map Distance Calculator, Transport ETA, City Aliases Filters, and Error Handling.
 */
class MapAndStatusUnitTest {

    @Test
    fun testMapDistanceCalculator_HaversineAndFormat() {
        // Sana'a coordinates
        val lat1 = 15.3694
        val lon1 = 44.1910
        val lat2 = 15.3700
        val lon2 = 44.1920

        val distanceMeters = MapDistanceCalculator.calculateDistanceMeters(lat1, lon1, lat2, lon2)
        assertTrue("Distance should be positive", distanceMeters > 0)

        val formattedDist = MapDistanceCalculator.formatDistance(distanceMeters)
        assertNotNull(formattedDist)
        assertTrue(formattedDist.contains("م") || formattedDist.contains("كم"))
    }

    @Test
    fun testTransportMode_EtaCalculation() {
        val distance500m = 500.0
        val etaWalking = MapDistanceCalculator.computeEta(distance500m, TransportMode.WALKING)
        assertTrue(etaWalking.contains("سيراً"))

        val etaBicycle = MapDistanceCalculator.computeEta(distance500m, TransportMode.BICYCLE)
        assertTrue(etaBicycle.contains("بالدراجة"))

        val etaCar = MapDistanceCalculator.computeEta(2000.0, TransportMode.CAR)
        assertTrue(etaCar.contains("بالسيارة"))
    }

    @Test
    fun testCityAliases_FilterMatching() {
        val sanaaAliases = CityAliases.ALIASES["صنعاء"]
        assertNotNull(sanaaAliases)
        assertTrue(sanaaAliases!!.contains("sanaa"))
        assertTrue(sanaaAliases.contains("حدة"))

        val isMatched = MapScreenFilters.matchesCityFilter(
            cityFilter = "صنعاء",
            entityCityId = "1",
            entityArea = "حدة",
            entityNeighborhood = "السبعين"
        )
        assertTrue("Sanaa neighborhood should match city filter", isMatched)
    }

    @Test
    fun testFilterProviders_BySearchQuery() {
        val providers = listOf(
            ProviderEntity(id = "p1", name = "علي الكهربائي", profession = "كهربائي", cityId = "1"),
            ProviderEntity(id = "p2", name = "محمد السباك", profession = "سباك", cityId = "1")
        )

        val filtered = MapScreenFilters.filterProviders(
            providers = providers,
            selectedCategory = "ALL",
            selectedCity = "الكل",
            searchQuery = "كهربائي"
        )

        assertEquals(1, filtered.size)
        assertEquals("p1", filtered.first().id)
    }

    @Test
    fun testErrorHandler_TranslatesExceptionsCorrectly() {
        val unknownHostMsg = ErrorHandler.handle(UnknownHostException("Host not found"))
        assertTrue(unknownHostMsg.contains("الإنترنت"))

        val timeoutMsg = ErrorHandler.handle(SocketTimeoutException("Timeout"))
        assertTrue(timeoutMsg.contains("انتهت مهلة الاتصال"))

        val ioMsg = ErrorHandler.handle(IOException("Disk error"))
        assertTrue(ioMsg.contains("حدث خطأ"))
    }
}
