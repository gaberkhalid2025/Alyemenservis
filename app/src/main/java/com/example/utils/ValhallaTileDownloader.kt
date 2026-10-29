package com.example.utils

import android.content.Context
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

/**
 * 📦 ValhallaTileDownloader
 * [إصلاح ملاحة نهائي] إدارة تحميل وتثبيت حزمة خرائط اليمن (Yemen Valhalla Tiles)
 * - تحميل مباشر مع دعم تتبع تحويلات GitHub Releases (302/307 Redirects)
 * - استخراج موثوق لحزمة valhalla_tiles.tar وملف valhalla.json
 * - علامة دائمة تمنع طلب التحميل مرة أخرى بعد النجاح
 */
object ValhallaTileDownloader {

    private const val TAG = "ValhallaDownloader"
    const val DEFAULT_DOWNLOAD_URL = "https://github.com/maher202021/Vela2026/releases/download/yemen-valhalla-1/yemen_valhalla_tiles.zip"
    private const val PREFS_NAME = "valhalla_prefs"
    private const val PREF_KEY_DOWNLOADED = "is_valhalla_downloaded"
    private const val MARKER_FILE_NAME = NavConfig.VALHALLA_MARKER_FILE
    private const val VALHALLA_DIR_NAME = NavConfig.VALHALLA_DIR
    private const val TILE_FILE_NAME = NavConfig.VALHALLA_TILES_FILE
    private const val LOCAL_ZIP_NAME = "yemen_valhalla_tiles.zip"

    /**
     * الحصول على مجلد التخزين الداخلي الخاص بالخرائط
     */
    fun getValhallaDir(context: Context): File {
        val dir = File(context.filesDir, VALHALLA_DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * [إصلاح ملاحة نهائي] التحقق الموثوق من تحميل وتثبيت الحزمة بنجاح
     */
    fun isTilesDownloaded(context: Context): Boolean {
        try {
            val valhallaDir = getValhallaDir(context)
            val markerFile = File(valhallaDir, MARKER_FILE_NAME)
            val tileFile = File(valhallaDir, TILE_FILE_NAME)

            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val isPrefMarked = prefs.getBoolean(PREF_KEY_DOWNLOADED, false)

            // فحص وجود ملف tar وحجمه أكبر من 1 ميجابايت على الأقل
            val hasValidTileFile = tileFile.exists() && tileFile.length() > 1024 * 1024
            val hasAnyValidTar = valhallaDir.listFiles()?.any { 
                it.name.endsWith(".tar") && it.length() > 1024 * 1024 
            } == true

            val isTilePresent = hasValidTileFile || hasAnyValidTar
            val isMarkerPresent = markerFile.exists() || isPrefMarked

            return isTilePresent && isMarkerPresent
        } catch (e: Exception) {
            Log.e(TAG, "isTilesDownloaded error: ${e.message}")
            return false
        }
    }

    /**
     * [إصلاح ملاحة نهائي] تحميل الحزمة وفك ضغطها مع دعم كامل لتحويلات الروابط (Redirects)
     */
    suspend fun downloadAndExtractTiles(
        context: Context,
        downloadUrl: String = DEFAULT_DOWNLOAD_URL,
        onProgress: (Float, String) -> Unit
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            onProgress(0.05f, "جاري الاتصال بخادم الخرائط...")
            val valhallaDir = getValhallaDir(context)

            // تتبع التحويلات (Redirects) من GitHub إلى خوادم التخزين السحابي
            var currentUrl = downloadUrl
            var redirectsCount = 0
            var finalConnection: HttpURLConnection? = null

            while (redirectsCount < 6) {
                val url = URL(currentUrl)
                connection = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15000
                    readTimeout = 45000
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "AlYemenServicesApp/1.0 (Android; MapTiles)")
                }

                val status = connection.responseCode
                if (status == HttpURLConnection.HTTP_MOVED_TEMP || 
                    status == HttpURLConnection.HTTP_MOVED_PERM || 
                    status == 307 || status == 308) {
                    val newLocation = connection.getHeaderField("Location")
                    if (!newLocation.isNullOrBlank()) {
                        currentUrl = newLocation
                        redirectsCount++
                        continue
                    }
                }

                if (status == HttpURLConnection.HTTP_OK) {
                    finalConnection = connection
                    break
                } else {
                    return@withContext Result.failure(Exception("خطأ في الاتصال بالخادم: رمز $status"))
                }
            }

            if (finalConnection == null) {
                return@withContext Result.failure(Exception("تعذر متابعة رابط التحميل بعد عدة محاولات"))
            }

            val totalContentLength = finalConnection.contentLength.toLong()
            onProgress(0.12f, "بدء فك ضغط وتثبيت خرائط اليمن...")

            val targetTarFile = File(valhallaDir, TILE_FILE_NAME)
            val tempTarFile = File(valhallaDir, "temp_$TILE_FILE_NAME")
            var extractedTar = false

            finalConnection.inputStream.use { rawInput ->
                ZipInputStream(BufferedInputStream(rawInput, 32768)).use { zipIn ->
                    var entry = zipIn.nextEntry
                    while (entry != null) {
                        val entryName = entry.name
                        if (entryName.endsWith(".tar") || entryName.contains("valhalla_tiles")) {
                            FileOutputStream(tempTarFile).use { fos ->
                                val buffer = ByteArray(32768)
                                var readBytes: Int
                                var totalExtracted = 0L

                                while (zipIn.read(buffer).also { readBytes = it } != -1) {
                                    fos.write(buffer, 0, readBytes)
                                    totalExtracted += readBytes

                                    val progressFraction = if (totalContentLength > 0) {
                                        (0.15f + (totalExtracted.toFloat() / (totalContentLength * 2.5f)) * 0.8f).coerceAtMost(0.96f)
                                    } else {
                                        (0.15f + (totalExtracted.toFloat() / (65 * 1024 * 1024)) * 0.8f).coerceAtMost(0.96f)
                                    }
                                    val percent = (progressFraction * 100).toInt()
                                    val mb = totalExtracted / (1024 * 1024)
                                    onProgress(progressFraction, "جاري تثبيت الخرائط ($mb ميجابايت) $percent%...")
                                }
                            }
                            extractedTar = true
                        } else if (entryName.endsWith("valhalla.json")) {
                            val configFile = File(valhallaDir, "valhalla.json")
                            FileOutputStream(configFile).use { fos ->
                                val buffer = ByteArray(8192)
                                var r: Int
                                while (zipIn.read(buffer).also { r = it } != -1) {
                                    fos.write(buffer, 0, r)
                                }
                            }
                        }
                        zipIn.closeEntry()
                        entry = zipIn.nextEntry
                    }
                }
            }

            if (extractedTar && tempTarFile.exists() && tempTarFile.length() > 1024 * 1024) {
                if (targetTarFile.exists()) {
                    targetTarFile.delete()
                }
                tempTarFile.renameTo(targetTarFile)

                // إنشاء ملف الإعدادات valhalla.json بالمسارات المطلقة الصحيحة
                createValhallaConfig(context)

                // كتابة ملف العلامة وحفظ الحالة في SharedPreferences
                val markerFile = File(valhallaDir, MARKER_FILE_NAME)
                markerFile.writeText("valhalla_installed_${System.currentTimeMillis()}")

                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit().putBoolean(PREF_KEY_DOWNLOADED, true).apply()

                onProgress(1.0f, "تم اكتمال تثبيت خرائط اليمن بنجاح ✓")
                Log.d(TAG, "Valhalla tiles successfully downloaded and installed. Size: ${targetTarFile.length()} bytes")
                Result.success(true)
            } else {
                tempTarFile.delete()
                Result.failure(Exception("لم يتم العثور على حزمة الخرائط بصيغة صالحة داخل الأرشيف"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Download and extract error: ${e.message}", e)
            Result.failure(e)
        } finally {
            try {
                connection?.disconnect()
            } catch (_: Exception) {}
        }
    }

    /**
     * [إصلاح ملاحة نهائي] إنشاء ملف الإعدادات (valhalla.json) للإشارة للمسار الداخلي الصحيح
     */
    fun createValhallaConfig(context: Context) {
        try {
            val valhallaDir = getValhallaDir(context)
            val tilesFile = File(valhallaDir, TILE_FILE_NAME)
            val configFile = File(valhallaDir, "valhalla.json")

            val configJson = """
                {
                  "mjolnir": {
                    "tile_dir": "${valhallaDir.absolutePath}",
                    "tile_extract": "${tilesFile.absolutePath}",
                    "admin": "${valhallaDir.absolutePath}/admin.sqlite",
                    "timezone": "${valhallaDir.absolutePath}/tz.sqlite"
                  },
                  "loki": {
                    "actions": ["route", "locate", "sources_to_targets"]
                  },
                  "thor": {
                    "source_to_target_algorithm": "select_optimal"
                  },
                  "routing": {
                    "results_namespace": "valhalla"
                  }
                }
            """.trimIndent()

            configFile.writeText(configJson)
        } catch (e: Exception) {
            Log.e(TAG, "createValhallaConfig error: ${e.message}")
        }
    }

    /**
     * [إصلاح ملاحة نهائي] استيراد يدوي للحزمة من مجلد التنزيلات
     */
    suspend fun importLocalTiles(
        context: Context,
        onProgress: (Float, String) -> Unit = { _, _ -> }
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            onProgress(0.1f, "البحث عن ملف الخرائط في مجلد التنزيلات...")
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val zipFile = File(downloadsDir, LOCAL_ZIP_NAME)

            if (!zipFile.exists()) {
                return@withContext Result.failure(Exception("الملف $LOCAL_ZIP_NAME غير موجود في التنزيلات."))
            }

            val valhallaDir = getValhallaDir(context)
            val targetTarFile = File(valhallaDir, TILE_FILE_NAME)
            val tempTarFile = File(valhallaDir, "temp_$TILE_FILE_NAME")
            var extractedTar = false

            onProgress(0.3f, "جاري فك ضغط الحزمة المحلية...")

            ZipInputStream(BufferedInputStream(zipFile.inputStream(), 32768)).use { zipIn ->
                var entry = zipIn.nextEntry
                while (entry != null) {
                    val entryName = entry.name
                    if (entryName.endsWith(".tar") || entryName.contains("valhalla_tiles")) {
                        FileOutputStream(tempTarFile).use { fos ->
                            val buffer = ByteArray(32768)
                            var readBytes: Int
                            while (zipIn.read(buffer).also { readBytes = it } != -1) {
                                fos.write(buffer, 0, readBytes)
                            }
                        }
                        extractedTar = true
                    }
                    zipIn.closeEntry()
                    entry = zipIn.nextEntry
                }
            }

            if (extractedTar && tempTarFile.exists() && tempTarFile.length() > 1024 * 1024) {
                if (targetTarFile.exists()) targetTarFile.delete()
                tempTarFile.renameTo(targetTarFile)

                createValhallaConfig(context)

                File(valhallaDir, MARKER_FILE_NAME).writeText("imported_${System.currentTimeMillis()}")
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit().putBoolean(PREF_KEY_DOWNLOADED, true).apply()

                onProgress(1.0f, "تم استيراد خرائط اليمن بنجاح ✓")
                Result.success(true)
            } else {
                tempTarFile.delete()
                Result.failure(Exception("الملف المضغوط لا يحتوي على أرشيف خرائط صالح"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "importLocalTiles error: ${e.message}", e)
            Result.failure(e)
        }
    }
}
