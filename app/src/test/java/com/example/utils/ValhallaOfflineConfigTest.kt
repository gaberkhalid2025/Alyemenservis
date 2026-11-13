package com.example.utils

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.RandomAccessFile
import java.nio.file.Files

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ValhallaOfflineConfigTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        ValhallaTileDownloader.getValhallaDir(context).deleteRecursively()
    }

    @Test
    fun `buildConfigJsonString preserves all required service_limits nodes and clears docker paths`() {
        val tileDir = "/data/user/0/com.alyemenservices/files/valhalla"
        val tileExtract = "$tileDir/valhalla_tiles.tar"

        val jsonText = ValhallaTileDownloader.buildConfigJsonString(
            tileDirPath = tileDir,
            tileExtractPath = tileExtract
        )

        assertTrue(
            "Generated valhalla.json must pass strict validator",
            ValhallaTileDownloader.isValhallaConfigJsonValid(jsonText, tileDir, tileExtract)
        )

        val root = JSONObject(jsonText)
        val sl = root.getJSONObject("service_limits")
        val iso = sl.getJSONObject("isochrone")
        assertEquals(4, iso.getInt("max_contours"))
        assertEquals(200, iso.getInt("max_distance_contour"))
        assertEquals(120, iso.getInt("max_time_contour"))
        assertTrue(sl.getInt("max_radius") >= 5000)

        val ped = sl.getJSONObject("pedestrian")
        assertTrue(ped.has("max_multimodal_walking_distance"))
        assertTrue(ped.has("max_transit_walking_distance"))

        val mjolnir = root.getJSONObject("mjolnir")
        assertEquals(tileDir, mjolnir.getString("tile_dir"))
        assertEquals(tileExtract, mjolnir.getString("tile_extract"))
        assertFalse(mjolnir.getJSONObject("data_processing").getBoolean("use_admin_db"))
        assertFalse(jsonText.contains("/custom_files/"))
        assertFalse(jsonText.contains("/data/valhalla"))
    }

    @Test
    fun `isValhallaConfigJsonValid rejects broken camelCase or missing isochrone configs`() {
        val tileDir = "/data/user/0/com.alyemenservices/files/valhalla"
        val tileExtract = "$tileDir/valhalla_tiles.tar"

        val brokenCamelCase = ValhallaTileDownloader.buildConfigJsonString(tileDir, tileExtract)
            .replace("\"max_contours\": 4", "\"maxContours\": 4")
        assertFalse(ValhallaTileDownloader.isValhallaConfigJsonValid(brokenCamelCase, tileDir, tileExtract))

        val root = JSONObject(ValhallaTileDownloader.buildConfigJsonString(tileDir, tileExtract))
        root.getJSONObject("service_limits").remove("isochrone")
        assertFalse(ValhallaTileDownloader.isValhallaConfigJsonValid(root.toString(), tileDir, tileExtract))

        assertFalse(
            ValhallaTileDownloader.isValhallaConfigJsonValid(
                ValhallaTileDownloader.buildConfigJsonString(tileDir, "/custom_files/valhalla_tiles.tar"),
                tileDir,
                tileExtract
            )
        )
    }

    @Test
    fun `repairConfig heals broken valhalla json on existing device without re-downloading tar`() = runBlocking {
        val dir = ValhallaTileDownloader.getValhallaDir(context)
        val tar = File(dir, NavConfig.VALHALLA_TILES_FILE)
        RandomAccessFile(tar, "rw").use { raf ->
            raf.setLength(11L * 1024L * 1024L)
            val header = ByteArray(512)
            "index.bin".toByteArray(Charsets.US_ASCII).copyInto(header, 0)
            "ustar".toByteArray(Charsets.US_ASCII).copyInto(header, 257)
            raf.seek(0)
            raf.write(header)
        }
        // محاكاة ملف valhalla.json قديم وتالف ينقصه service_limits.isochrone.max_contours
        val configFile = File(dir, "valhalla.json")
        configFile.writeText("""{"mjolnir":{"tile_extract":"${tar.absolutePath}"},"service_limits":{"isochrone":{}}}""")

        assertFalse(ValhallaTileDownloader.isValhallaConfigValid(context))
        assertTrue(ValhallaTileDownloader.repairConfig(context))
        assertTrue(ValhallaTileDownloader.isValhallaConfigValid(context))

        val repairedJson = JSONObject(configFile.readText())
        assertEquals(4, repairedJson.getJSONObject("service_limits").getJSONObject("isochrone").getInt("max_contours"))
        assertEquals(tar.absolutePath, repairedJson.getJSONObject("mjolnir").getString("tile_extract"))
    }

    @Test
    fun `isValidTarFile rejects html or tiny files and accepts valid ustar tar`() {
        val tmpDir = Files.createTempDirectory("valhalla_tar_test").toFile()
        try {
            val htmlFile = File(tmpDir, "bad.tar").apply { writeText("<html>404 Not Found</html>") }
            assertFalse(ValhallaTileDownloader.isValidTarFile(htmlFile))

            val validTar = File(tmpDir, "valhalla_tiles.tar")
            RandomAccessFile(validTar, "rw").use { raf ->
                raf.setLength(11L * 1024L * 1024L)
                val header = ByteArray(512)
                "index.bin".toByteArray(Charsets.US_ASCII).copyInto(header, 0)
                "ustar".toByteArray(Charsets.US_ASCII).copyInto(header, 257)
                raf.seek(0)
                raf.write(header)
            }
            assertTrue(ValhallaTileDownloader.isValidTarFile(validTar))
        } finally {
            tmpDir.deleteRecursively()
        }
    }
}
