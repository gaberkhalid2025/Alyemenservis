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
