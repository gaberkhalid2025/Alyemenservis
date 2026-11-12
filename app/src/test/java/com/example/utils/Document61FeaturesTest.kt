package com.example.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class YemeniPhrasesTest {
    @Test
    fun twoHundred() = assertEquals("ميتين متر", YemeniPhrases.distance(200.0))

    @Test
    fun hundredFifty() = assertEquals("مية وخمسين متر", YemeniPhrases.distance(150.0))

    @Test
    fun fifty() = assertEquals("خمسين متر", YemeniPhrases.distance(50.0))

    @Test
    fun oneAndHalfKm() = assertEquals("كيلو ونص", YemeniPhrases.distance(1500.0))

    @Test
    fun upcomingLeft() = assertEquals("بعد ميتين متر، لف يسار", YemeniPhrases.upcoming(ManeuverType.LEFT, null, 200.0))
}

class ReliabilityZonesTest {
    private fun p(lat: Double, lng: Double, av: Boolean) = com.example.data.ProviderEntity(
        id = "p1",
        name = "فني 1",
        phone = "777000000",
        profession = "كهربائي",
        area = "حدة",
        latitude = lat,
        longitude = lng,
        isAvailable = av
    )

    @Test
    fun availableCellIsGreen() {
        val z = ReliabilityZones.build(listOf(p(15.005, 44.005, true)), emptyList())
        assertEquals("#10B981", z.single().colorHex)
    }

    @Test
    fun unavailableCellIsYellow() {
        val z = ReliabilityZones.build(listOf(p(15.005, 44.005, false)), emptyList())
        assertEquals("#F59E0B", z.single().colorHex)
    }
}

class OfflineYemenGazetteerTest {
    @Test
    fun findsTwoLetterCity() {
        val res = OfflineYemenGazetteer.search("إب")
        org.junit.Assert.assertTrue(res.any { it.shortName == "إب" })
    }

    @Test
    fun findsNormalizedStreetAndCoordinates() {
        val street = OfflineYemenGazetteer.search("الزبيري")
        org.junit.Assert.assertTrue(street.any { it.shortName.contains("الزبيري") })

        val coords = OfflineYemenGazetteer.search("15.3694, 44.1910")
        org.junit.Assert.assertEquals(1, coords.size)
        org.junit.Assert.assertEquals(15.3694, coords.first().latitude, 0.0001)
    }

    @Test
    fun parsesAllCoordinateFormats() {
        val google = OfflineYemenGazetteer.parseCoordinates("https://maps.google.com/?q=15.3547,44.2067")
        org.junit.Assert.assertNotNull(google)
        assertEquals(15.3547, google!!.first, 0.0001)
        assertEquals(44.2067, google.second, 0.0001)

        val arabicDigits = OfflineYemenGazetteer.parseCoordinates("١٥٫٣٦٩٤، ٤٤٫١٩١٠")
        org.junit.Assert.assertNotNull(arabicDigits)
        assertEquals(15.3694, arabicDigits!!.first, 0.0001)
        assertEquals(44.1910, arabicDigits.second, 0.0001)

        val dms = OfflineYemenGazetteer.parseCoordinates("15°22'10\"N 44°11'28\"E")
        org.junit.Assert.assertNotNull(dms)
        assertEquals(15.3694, dms!!.first, 0.001)
        assertEquals(44.1911, dms.second, 0.001)

        val osm = OfflineYemenGazetteer.parseCoordinates("https://www.openstreetmap.org/#map=16/12.7855/45.0187")
        org.junit.Assert.assertNotNull(osm)
        assertEquals(12.7855, osm!!.first, 0.0001)

        val whatsapp = OfflineYemenGazetteer.parseCoordinates("موقعي للتوصيل هو 13.5789, 44.0219 عند الباب")
        org.junit.Assert.assertNotNull(whatsapp)
        assertEquals(13.5789, whatsapp!!.first, 0.0001)
    }

    @Test
    fun buildsNearestReferenceSummaryWithoutContext() {
        val summary = OfflineYemenGazetteer.nearestReferenceSummary(null, 15.3550, 44.2067)
        org.junit.Assert.assertTrue(summary.contains("ميدان التحرير"))
    }
}
