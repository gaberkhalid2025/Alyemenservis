package com.example.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteTrackerTest {
    // ثلاث نقاط على خط طول ثابت، كل 0.01° عرض ≈ 1112 م
    private val route = listOf(15.00 to 44.0, 15.01 to 44.0, 15.02 to 44.0)

    @Test fun totalLengthIsAbout2224m() {
        assertEquals(2224.0, RouteTracker(route).totalMeters, 15.0)
    }

    @Test fun midpointProgressAndRemaining() {
        val r = RouteTracker(route).update(15.01, 44.0)!!
        assertEquals(1112.0, r.alongMeters, 10.0)
        assertEquals(1112.0, r.remainingMeters, 10.0)
        assertEquals(0.0, r.offRouteMeters, 1.0)
        assertEquals(0.5f, r.progress, 0.02f)
    }

    @Test fun lateralOffsetIsMeasured() {
        // 0.0005° شرقاً عند خط عرض 15° ≈ 53.7 م
        val r = RouteTracker(route).update(15.01, 44.0005)!!
        assertTrue("offRoute=${r.offRouteMeters}", r.offRouteMeters in 50.0..58.0)
    }

    @Test fun loopedRouteDoesNotJumpToEnd() {
        val loop = listOf(15.0 to 44.0, 15.01 to 44.0, 15.01 to 44.01, 15.0 to 44.01, 15.0 to 44.0)
        val r = RouteTracker(loop).update(15.0, 44.0)!!
        assertEquals(0.0, r.alongMeters, 1.0)      // البداية لا النهاية
    }

    @Test fun emptyOrSinglePointIsNotUsable() {
        assertFalse(RouteTracker(emptyList()).isUsable)
        assertFalse(RouteTracker(listOf(15.0 to 44.0)).isUsable)
    }
}

class DetectorsTest {
    @Test fun offRouteNeedsSustainedDeviation() {
        val d = OffRouteDetector(thresholdM = 40.0, holdMs = 5_000L)
        assertFalse(d.update(100.0, 0L))
        assertFalse(d.update(100.0, 4_999L))
        assertTrue(d.update(100.0, 5_000L))
        assertFalse(d.update(10.0, 6_000L))       // العودة تصفّر العدّاد
        assertFalse(d.update(100.0, 6_100L))
    }

    @Test fun arrivalFiresOncePerWaypoint() {
        val a = ArrivalDetector(radiusM = 30.0)
        assertTrue(a.check(0, 10.0))
        assertFalse(a.check(0, 5.0))
        assertFalse(a.check(1, 50.0))
        assertTrue(a.check(1, 20.0))
    }

    @Test fun speedIsZeroWhenStationaryAndPositiveWhenMoving() {
        val s = SpeedEstimator()
        s.update(15.0, 44.0, 1_000L)
        assertEquals(0, s.update(15.0, 44.0, 2_000L))
        val m = SpeedEstimator()
        m.update(15.0, 44.0, 1_000L)
        val kmh = m.update(15.0, 44.0001, 2_000L)   // ≈ 10.7 م في ثانية ≈ 38 كم/س
        assertTrue("kmh=$kmh", kmh in 35..42)
    }
}
