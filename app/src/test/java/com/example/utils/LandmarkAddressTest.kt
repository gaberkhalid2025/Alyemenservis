package com.example.utils

import org.junit.Assert.*
import org.junit.Test

class LandmarkAddressTest {
    private fun addr(dLat: Double = 0.0, dLng: Double = 0.0, desc: String = "البوابة الحمراء") = LandmarkAddress(
        label = "منزلي", landmarkName = "مسجد النور", landmarkLat = 15.3694, landmarkLng = 44.1910,
        pointLat = 15.3694 + dLat, pointLng = 44.1910 + dLng, description = desc
    )
    @Test fun northOffsetDescribed() =
        assertEquals("على بعد 80 م شمال مسجد النور، البوابة الحمراء", addr(dLat = 0.0007).describe())
    @Test fun eastDirection() = assertTrue(addr(dLng = 0.0003).describe().contains("شرق"))
    @Test fun southWestDirection() = assertTrue(addr(dLat = -0.0005, dLng = -0.0005).describe().contains("جنوب غرب"))
    @Test fun veryCloseIsAtLandmark() = assertTrue(addr(dLat = 0.00005).describe().startsWith("عند مسجد النور"))
    @Test fun descriptionRequired() = assertNotNull(addr(desc = "ab").validate())
    @Test fun tooFarRejected() = assertNotNull(addr(dLat = 0.02).validate())      // ≈ 2.2 كم
    @Test fun validPasses() = assertNull(addr(dLat = 0.0007).validate())
    @Test fun roundTripMap() {
        val a = addr(dLat = 0.0007)
        val b = LandmarkAddress.fromMap(a.toMap())!!
        assertEquals(a.describe(), b.describe())
    }
}
