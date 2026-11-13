package com.example.utils

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.ProviderEntity
import com.example.ui.screens.map.MapZone
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Document63FeaturesTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        CoverageLog.clear(context)
    }

    @Test
    fun `kalman smooths noisy gps jitter and resets after gap`() {
        val kf = LatLngKalman(processNoiseMps = 3.0)
        val t0 = 1_700_000_000_000L
        val first = kf.update(15.3694, 44.1910, 15f, t0)
        assertEquals(15.3694, first.first, 1e-6)
        assertEquals(44.1910, first.second, 1e-6)

        // قفزة ضوضاء بمقدار +0.0005 درجة (~55 متر) مع دقة 25م: التنعيم يمنع القفز الكامل
        val noisy = kf.update(15.3699, 44.1915, 25f, t0 + 1000L)
        assertTrue(noisy.first > 15.3694 && noisy.first < 15.3699)
        assertTrue(noisy.second > 44.1910 && noisy.second < 44.1915)

        // فجوة زمنية > 10 ثوانٍ تُعيد تهيئة المرشح فوراً للنقطة الجديدة
        val resetPt = kf.update(15.4000, 44.2200, 10f, t0 + 25_000L)
        assertEquals(15.4000, resetPt.first, 1e-6)
        assertEquals(44.2200, resetPt.second, 1e-6)
    }

    @Test
    fun `hazard alerts detects zone within threshold and ignores distant ones`() {
        val zones = listOf(
            MapZone(15.1060, 44.2740, 650, "#EF4444"),
            MapZone(14.1550, 44.2870, 700, "#EF4444")
        )
        // على بعد ~110 متر فقط من نقيل يسلح
        val near = HazardAlerts.nearest(15.1070, 44.2740, zones, thresholdMeters = 300.0)
        assertNotNull(near)
        assertEquals(15.1060, near!!.lat, 1e-6)

        // نقطة بعيدة في صنعاء
        val far = HazardAlerts.nearest(15.3694, 44.1910, zones, thresholdMeters = 300.0)
        assertNull(far)

        val autoZones = HazardAlerts.roadDifficultyZones(
            listOf(ProviderEntity(id = "p_isolated", name = "فني معزول", latitude = 16.0, longitude = 45.0))
        )
        assertTrue(autoZones.size >= HazardAlerts.knownMountainAndWadiHazards.size + 1)
    }

    @Test
    fun `gpx writer and trip recorder build valid gpx and skip close duplicates`() {
        val rec = TripRecorder(minSpacingMeters = 8.0)
        rec.record(15.369400, 44.191000, 1_700_000_000_000L, 2250.0)
        // حركة أقل من 8 أمتار (حوالي 1 متر) يجب تجاهلها
        rec.record(15.369410, 44.191005, 1_700_000_002_000L, 2250.0)
        // حركة > 100 متر يجب تسجيلها
        rec.record(15.371000, 44.192000, 1_700_000_020_000L, 2255.0)
        assertEquals(2, rec.size)

        val xml = GpxWriter.track("رحلة <صنعاء> & عدن", rec.snapshot())
        assertTrue(xml.contains("""<gpx version="1.1""""))
        assertTrue(xml.contains("رحلة &lt;صنعاء&gt; &amp; عدن"))
        assertTrue(xml.contains("<ele>2250.0</ele>"))
        assertTrue(xml.contains("<trkpt lat=\"15.369400\" lon=\"44.191000\">"))

        val routeXml = GpxWriter.route("مسار", listOf(15.3694 to 44.1910, 12.7855 to 45.0186))
        assertTrue(routeXml.contains("<rtept lat=\"15.369400\" lon=\"44.191000\" />"))
    }

    @Test
    fun `overlay feeds parse usgs geojson and rainviewer is gated off by default`() {
        assertFalse(NavConfig.ENABLE_RAINVIEWER)
        val geoJson = """
            {
              "type": "FeatureCollection",
              "features": [
                {
                  "geometry": {"type": "Point", "coordinates": [43.5, 14.8, 10]},
                  "properties": {"mag": 4.2, "place": "Red Sea", "time": 1700000000000}
                }
              ]
            }
        """.trimIndent()
        val parsed = OverlayFeeds.parseQuakesGeoJson(geoJson)
        assertEquals(1, parsed.size)
        assertEquals(14.8, parsed[0].lat, 1e-6)
        assertEquals(43.5, parsed[0].lng, 1e-6)
        assertEquals(4.2, parsed[0].mag, 1e-6)

        val rainJson = """{"host":"https://tilecache.rainviewer.com","radar":{"past":[{"path":"/v2/radar/1700000000"}]}}"""
        val tpl = OverlayFeeds.parseRainViewerTemplate(rainJson)
        assertEquals("https://tilecache.rainviewer.com/v2/radar/1700000000/256/{z}/{x}/{y}/2/1_1.png", tpl)
    }

    @Test
    fun `group store enforces 5-member limit extracts deep links and handles expiry`() {
        val store = GroupStore(context)
        val t1 = "ABCDEFGHIJKLMNOPQRSTUVWX"
        assertTrue(store.add("أحمد", "alyemen://track/$t1").isSuccess)
        assertTrue(store.add("مكرر", t1).isFailure)

        for (i in 2..5) {
            val tok = "TOKEN000000000000000000$i"
            assertTrue(store.add("عضو $i", tok).isSuccess)
        }
        val sixth = store.add("السادس", "ZZZZZZZZZZZZZZZZZZZZZZZZ")
        assertTrue(sixth.isFailure)
        assertEquals(5, store.all().size)

        val updated = store.updateFromFetch(t1, null)
        assertTrue(updated.first { it.token == t1 }.expired)
        val afterRemove = store.remove(t1)
        assertEquals(4, afterRemove.size)
    }

    @Test
    fun `coverage log identifies dead zones when minimum offline ratio reached`() {
        repeat(3) {
            CoverageLog.sample(context, 15.3675, 44.1925, online = false)
        }
        repeat(3) {
            CoverageLog.sample(context, 15.3800, 44.2100, online = true)
        }
        val zones = CoverageLog.deadZones(context)
        assertEquals(1, zones.size)
        assertEquals("#7C3AED", zones.first().colorHex)
        assertTrue(abs(zones.first().lat - 15.3675) < 0.01)

        CoverageLog.clear(context)
        assertTrue(CoverageLog.deadZones(context).isEmpty())
    }
}
