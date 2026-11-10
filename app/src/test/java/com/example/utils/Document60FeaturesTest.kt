package com.example.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GnssClassifyTest {
    @Test
    fun good() = assertEquals(GnssQuality.GOOD, GnssMonitor.classify(9, 34f))

    @Test
    fun fair() = assertEquals(GnssQuality.FAIR, GnssMonitor.classify(5, 24f))

    @Test
    fun poor() = assertEquals(GnssQuality.POOR, GnssMonitor.classify(2, 15f))

    @Test
    fun none() = assertEquals(GnssQuality.NONE, GnssMonitor.classify(0, 0f))
}

class DialectSynonymsTest {
    @Test
    fun motorWater() = assertTrue(
        DialectSynonyms.expand("ماطور ماء").contains(ArabicNormalizer.normalize("مضخة مياه"))
    )

    @Test
    fun punctureShop() = assertTrue(
        DialectSynonyms.expand("بنشر").contains(ArabicNormalizer.normalize("كفرات"))
    )

    @Test
    fun noExpansionForUnknown() = assertEquals(
        listOf(ArabicNormalizer.normalize("سباك")),
        DialectSynonyms.expand("سباك")
    )
}

class WaypointOptimizerTest {
    private fun wp(lat: Double, lng: Double) = NavWaypoint(name = "$lat", latitude = lat, longitude = lng)

    @Test
    fun reordersToNearestFirst() {
        val origin = 15.0 to 44.0
        val far = wp(15.10, 44.0)
        val near = wp(15.01, 44.0)
        val mid = wp(15.05, 44.0)
        val result = WaypointOptimizer.optimize(origin, listOf(far, near, mid))
        assertEquals(listOf(near, mid, far), result)
    }

    @Test
    fun neverWorse() {
        val origin = 15.0 to 44.0
        val pts = listOf(
            wp(15.02, 44.03),
            wp(15.01, 44.0),
            wp(15.04, 44.01),
            wp(15.03, 43.99)
        )
        assertTrue(
            WaypointOptimizer.pathLength(origin, WaypointOptimizer.optimize(origin, pts)) <=
                    WaypointOptimizer.pathLength(origin, pts) + 1e-6
        )
    }
}
