package com.example.utils

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.zip.ZipInputStream

/**
 * 📦 ValhallaTileDownloader
 * Manager for downloading and validating offline navigation map packages and tiles for Yemen
 */
object ValhallaTileDownloader {

    const val DEFAULT_DOWNLOAD_URL = "https://github.com/maher202021/Vela2026/releases/download/yemen-valhalla-1/yemen_valhalla_tiles.zip"
    private const val MARKER_FILE_NAME = "is_valhalla_downloaded.txt"
    private const val VALHALLA_DIR_NAME = "valhalla"
    private const val TILE_FILE_NAME = "valhalla_tiles.tar"

    fun isTilesDownloaded(context: Context): Boolean {
        val markerFile = File(context.filesDir, MARKER_FILE_NAME)
        return markerFile.exists()
    }

    suspend fun downloadAndExtractTiles(
        context: Context,
        downloadUrl: String = DEFAULT_DOWNLOAD_URL,
        onProgress: (Float, String) -> Unit
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            onProgress(0.1f, "الاتصال بخادم الخرائط...")
            val valhallaDir = File(context.filesDir, VALHALLA_DIR_NAME)
            if (!valhallaDir.exists()) valhallaDir.mkdirs()

            val url = URL(downloadUrl)
            val connection = url.openConnection()
            connection.connect()

            val zipInputStream = ZipInputStream(BufferedInputStream(connection.getInputStream()))
            
            var entry = zipInputStream.nextEntry
            while (entry != null) {
                if (entry.name == TILE_FILE_NAME) {
                    val outputFile = File(valhallaDir, TILE_FILE_NAME)
                    FileOutputStream(outputFile).use { fos ->
                        val buffer = ByteArray(8192)
                        var length: Int
                        while (zipInputStream.read(buffer).also { length = it } != -1) {
                            fos.write(buffer, 0, length)
                        }
                    }
                    break
                }
                zipInputStream.closeEntry()
                entry = zipInputStream.nextEntry
            }
            zipInputStream.close()

            val markerFile = File(context.filesDir, MARKER_FILE_NAME)
            markerFile.writeText("downloaded")
            
            onProgress(1.0f, "تم اكتمال التثبيت بنجاح ✓")
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
