package com.example.utils

import android.content.Context
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.zip.ZipInputStream

/**
 * 📦 ValhallaTileDownloader
 * إدارة تحميل وتثبيت حزمة خرائط الملاحة (Yemen Valhalla Tiles)
 * - تحميل، فك ضغط، وحفظ الملف في الذاكرة الداخلية.
 * - منع التحميل المتكرر عبر ملف علامة (Marker).
 * - دعم الاستيراد اليدوي من مجلد التنزيلات.
 */
object ValhallaTileDownloader {

    const val DEFAULT_DOWNLOAD_URL = "https://github.com/maher202021/Vela2026/releases/download/yemen-valhalla-1/yemen_valhalla_tiles.zip"
    private const val MARKER_FILE_NAME = NavConfig.VALHALLA_MARKER_FILE
    private const val VALHALLA_DIR_NAME = NavConfig.VALHALLA_DIR
    private const val TILE_FILE_NAME = NavConfig.VALHALLA_TILES_FILE
    private const val LOCAL_ZIP_NAME = "yemen_valhalla_tiles.zip"

    /**
     * الحصول على مجلد التخزين الداخلي الخاص بالخرائط
     */
    private fun getValhallaDir(context: Context): File {
        val dir = File(context.filesDir, VALHALLA_DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * التحقق مما إذا كانت الخرائط محملة ومثبتة بنجاح
     */
    fun isTilesDownloaded(context: Context): Boolean {
        val valhallaDir = getValhallaDir(context)
        val markerFile = File(valhallaDir, MARKER_FILE_NAME)
        val tileFile = File(valhallaDir, TILE_FILE_NAME)
        return markerFile.exists() && tileFile.exists() && tileFile.length() > 1024 * 1024 // أكبر من 1 ميجا
    }

    /**
     * تحميل الحزمة من الإنترنت وفك ضغطها
     */
    suspend fun downloadAndExtractTiles(
        context: Context,
        downloadUrl: String = DEFAULT_DOWNLOAD_URL,
        onProgress: (Float, String) -> Unit
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            onProgress(0.05f, "بدء الاتصال بخادم الخرائط...")
            val valhallaDir = getValhallaDir(context)
            
            // تنظيف المجلد قبل البدء لضمان التثبيت النظيف
            if (valhallaDir.exists()) {
                valhallaDir.deleteRecursively()
                valhallaDir.mkdirs()
            }

            val url = URL(downloadUrl)
            val connection = url.openConnection()
            connection.connectTimeout = 10000
            connection.readTimeout = 30000
            connection.connect()

            val zipInputStream = ZipInputStream(BufferedInputStream(connection.getInputStream()))
            var entry = zipInputStream.nextEntry
            var extracted = false

            while (entry != null) {
                // البحث عن ملف tar الخاص بـ Valhalla داخل الـ zip
                if (entry.name.endsWith(TILE_FILE_NAME)) {
                    val outputFile = File(valhallaDir, TILE_FILE_NAME)
                    FileOutputStream(outputFile).use { fos ->
                        val buffer = ByteArray(16384)
                        var length: Int
                        var totalRead = 0L
                        val totalSize = entry.size

                        while (zipInputStream.read(buffer).also { length = it } != -1) {
                            fos.write(buffer, 0, length)
                            totalRead += length
                            if (totalSize > 0) {
                                val progress = 0.1f + (totalRead.toFloat() / totalSize) * 0.85f
                                onProgress(progress, "جاري تثبيت الخرائط... ${(progress * 100).toInt()}%")
                            } else {
                                // في حال كان الحجم غير معروف، نعرض تقدم تقديري
                                val estimatedProgress = (0.1f + (totalRead.toFloat() / (25 * 1024 * 1024)) * 0.85f).coerceAtMost(0.95f)
                                onProgress(estimatedProgress, "جاري التحميل... ${(totalRead / 1024 / 1024)}MB")
                            }
                        }
                    }
                    extracted = true
                }
                zipInputStream.closeEntry()
                entry = zipInputStream.nextEntry
            }
            zipInputStream.close()

            if (extracted) {
                createValhallaConfig(context)
                val markerFile = File(valhallaDir, MARKER_FILE_NAME)
                markerFile.writeText("downloaded_${System.currentTimeMillis()}")
                onProgress(1.0f, "تم اكتمال تثبيت خرائط اليمن بنجاح ✓")
                Result.success(true)
            } else {
                Result.failure(Exception("لم يتم العثور على حزمة الخرائط ($TILE_FILE_NAME) داخل الأرشيف."))
            }
        } catch (e: Exception) {
            android.util.Log.e("ValhallaDownloader", "Download failed: ${e.message}")
            Result.failure(e)
        }
    }


    /**
     * إنشاء ملف الإعدادات (valhalla.json) لمحرك الملاحة
     */
    private fun createValhallaConfig(context: Context) {
        val valhallaDir = File(context.filesDir, NavConfig.VALHALLA_DIR)
        val tilesFile = File(valhallaDir, NavConfig.VALHALLA_TILES_FILE)
        val configFile = File(valhallaDir, "valhalla.json")

        val configJson = """
            {
              "mjolnir": {
                "tile_dir": "${valhallaDir.absolutePath}",
                "tile_extract": "${tilesFile.absolutePath}"
              },
              "routing": {
                "results_namespace": "valhalla"
              }
            }
        """.trimIndent()

        configFile.writeText(configJson)
    }

    /**
     * استيراد يدوي للحزمة من مجلد التنزيلات
     */
    suspend fun importLocalTiles(
        context: Context,
        onProgress: (Float, String) -> Unit = { _, _ -> }
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            onProgress(0.1f, "البحث عن الملف في التنزيلات...")
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val zipFile = File(downloadsDir, LOCAL_ZIP_NAME)

            if (!zipFile.exists()) {
                return@withContext Result.failure(Exception("الملف $LOCAL_ZIP_NAME غير موجود في التنزيلات."))
            }

            val valhallaDir = getValhallaDir(context)
            onProgress(0.3f, "جاري فك الضغط...")

            val zipInputStream = ZipInputStream(BufferedInputStream(zipFile.inputStream()))
            var entry = zipInputStream.nextEntry
            var extracted = false

            while (entry != null) {
                if (entry.name.endsWith(TILE_FILE_NAME)) {
                    val outputFile = File(valhallaDir, TILE_FILE_NAME)
                    FileOutputStream(outputFile).use { fos ->
                        val buffer = ByteArray(8192)
                        var length: Int
                        while (zipInputStream.read(buffer).also { length = it } != -1) {
                            fos.write(buffer, 0, length)
                        }
                    }
                    extracted = true
                    break
                }
                zipInputStream.closeEntry()
                entry = zipInputStream.nextEntry
            }
            zipInputStream.close()

            if (extracted) {
                createValhallaConfig(context)
                File(valhallaDir, MARKER_FILE_NAME).writeText("imported_${System.currentTimeMillis()}")
                onProgress(1.0f, "تم الاستيراد بنجاح ✓")
                Result.success(true)
            } else {
                Result.failure(Exception("الملف المضغوط لا يحتوي على الخرائط."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
