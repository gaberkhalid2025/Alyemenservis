package com.example.ui.screens.map.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MapDistanceCalculatorTest {
    @Test fun formatDistance() {
        assertEquals("999 م", MapDistanceCalculator.formatDistance(999.4))
        assertEquals("1.5 كم", MapDistanceCalculator.formatDistance(1500.0))
        assertEquals("—", MapDistanceCalculator.formatDistance(Double.NaN))
    }
    @Test fun formatDistanceSpoken() {
        assertEquals("50 متراً", MapDistanceCalculator.formatDistanceSpoken(47.0))
        assertEquals("750 متراً", MapDistanceCalculator.formatDistanceSpoken(730.0))
        assertEquals("1.5 كيلومتر", MapDistanceCalculator.formatDistanceSpoken(1500.0))
        assertEquals("2 كيلومتر", MapDistanceCalculator.formatDistanceSpoken(2000.0))
    }
    @Test fun formatDuration() {
        assertEquals("أقل من دقيقة", MapDistanceCalculator.formatDuration(20.0))
        assertEquals("2 دقيقة", MapDistanceCalculator.formatDuration(125.0))
        assertEquals("1 س", MapDistanceCalculator.formatDuration(3600.0))
        assertEquals("1 س 20 د", MapDistanceCalculator.formatDuration(4800.0))
    }
    @Test fun etaIsAlwaysLabeledApproximate() {
        assertTrue(MapDistanceCalculator.computeEta(500.0).contains("(تقريبي)"))
        assertTrue(MapDistanceCalculator.computeEta(5000.0).contains("(تقريبي)"))
    }
    @Test fun zeroCoordinatesGiveZeroDistance() {
        assertEquals(0.0, MapDistanceCalculator.calculateDistanceMeters(0.0, 0.0, 15.0, 44.0), 0.0)
        assertEquals(0.0, MapDistanceCalculator.calculateDistanceMeters(15.0, 44.0, 0.0, 0.0), 0.0)
    }
    @Test fun oneDegreeOfLatitudeIsAbout111km() {
        assertEquals(111_195.0, MapDistanceCalculator.calculateDistanceMeters(15.0, 44.0, 16.0, 44.0), 150.0)
    }
}
