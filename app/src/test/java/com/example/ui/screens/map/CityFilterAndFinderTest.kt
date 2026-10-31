package com.example.ui.screens.map

import com.example.ui.screens.map.utils.OfflineMapManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CityFilterAndFinderTest {
    @Test fun numericCodeIsExactToken() {
        assertTrue(MapScreenFilters.matchesCityFilter("صنعاء", "1", "", ""))
        assertFalse(MapScreenFilters.matchesCityFilter("صنعاء", "10", "شارع 21", ""))
    }
    @Test fun nameContains() =
        assertTrue(MapScreenFilters.matchesCityFilter("عدن", "", "كريتر - عدن", ""))
    @Test fun genericWordDoesNotMatchMarib() =
        assertFalse(MapScreenFilters.matchesCityFilter("مأرب", "", "المدينة المنورة", ""))
    @Test fun findCityRejectsDigitsAndEmpty() {
        assertNull(OfflineMapManager.findCity("1"))
        assertNull(OfflineMapManager.findCity(""))
        assertNull(OfflineMapManager.findCity("المدينة"))
    }
    @Test fun findCityFindsSanaa() = assertNotNull(OfflineMapManager.findCity("صنعاء"))
}
