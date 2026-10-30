package com.example.utils

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.zip.ZipInputStream

/**
 * 📦 ValhallaTileDownloader
 * [إصلاح ملاحة نهائي] إدارة تحميل وتثبيت حزمة خرائط اليمن (Yemen Valhalla Tiles)
 * - تحميل مباشر مع دعم تتبع تحويلات GitHub Releases (302/307 Redirects)
 * - استخراج ذري إلى مجلد مؤقت وفحص مساحة التخزين والـ SHA256
 * - علامة دائمة بصيغة JSON والحفاظ على الخرائط القديمة في حال انقطاع التنزيل
 */
object ValhallaTileDownloader {

    const val DEFAULT_DOWNLOAD_URL = "https://github.com/maher202021/Vela2026/releases/download/yemen-valhalla-1/yemen_valhalla_tiles.zip"
    private const val TAG = "ValhallaDownloader"
    private const val PREFS_NAME = "valhalla_prefs"
    private const val PREF_KEY_DOWNLOADED = "is_valhalla_downloaded"
    private const val MARKER_FILE_NAME = NavConfig.VALHALLA_MARKER_FILE
    private const val VALHALLA_DIR_NAME = NavConfig.VALHALLA_DIR
    private const val TILE_FILE_NAME = NavConfig.VALHALLA_TILES_FILE
    private const val LOCAL_ZIP_NAME = "yemen_valhalla_tiles.zip"
    private const val TMP_DIR_NAME = "valhalla_tmp"
    private const val OLD_DIR_NAME = "valhalla_old"
    private const val MIN_TILE_BYTES = 1024L * 1024L
    private const val MIN_FREE_BYTES = 100L * 1024L * 1024L

    fun getValhallaDir(context: Context): File {
        val dir = File(context.filesDir, VALHALLA_DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private fun valhallaDir(context: Context) = File(context.filesDir, VALHALLA_DIR_NAME)

    fun isTilesDownloaded(context: Context): Boolean {
        return try {
            val dir = valhallaDir(context)
            val marker = File(dir, MARKER_FILE_NAME)
            val tile = File(dir, TILE_FILE_NAME)
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val isPrefMarked = prefs.getBoolean(PREF_KEY_DOWNLOADED, false)

            if (!marker.exists() && !isPrefMarked) return false
            if (!tile.exists() || tile.length() < MIN_TILE_BYTES) {
                val hasAnyTar = dir.listFiles()?.any { it.name.endsWith(".tar") && it.length() >= MIN_TILE_BYTES } == true
                if (!hasAnyTar) return false
            }

            if (marker.exists()) {
                val text = marker.readText().trim()
                if (text.startsWith("{")) {
                    val expected = JSONObject(text).optLong("sizeBytes", -1L)
                    if (expected > 0 && tile.exists() && expected != tile.length()) return false
                }
            }
            true
        } catch (t: Throwable) {
            false
        }
    }

    suspend fun downloadAndExtractTiles(
        context: Context,
        downloadUrl: String = DEFAULT_DOWNLOAD_URL,
        onProgress: (Float, String) -> Unit
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            if (context.filesDir.usableSpace < MIN_FREE_BYTES) {
                return@withContext Result.failure(IOException("مساحة التخزين غير كافية (يلزم نحو 100 ميجابايت)"))
            }
            onProgress(0.05f, "جاري الاتصال بخادم الخرائط...")

            var currentUrl = downloadUrl
            var redirectsCount = 0
            var finalConnection: HttpURLConnection? = null

            while (redirectsCount < 6) {
                val url = URL(currentUrl)
                connection = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15_000
                    readTimeout = 45_000
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "AlYemenServicesApp/1.0 (Android; MapTiles)")
                }

                val status = connection.responseCode
                if (status == HttpURLConnection.HTTP_MOVED_TEMP ||
                    status == HttpURLConnection.HTTP_MOVED_PERM ||
                    status == 307 || status == 308
                ) {
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
                    return@withContext Result.failure(IOException("رد الخادم: $status"))
                }
            }

            if (finalConnection == null) {
                return@withContext Result.failure(IOException("تعذر متابعة رابط التحميل بعد عدة محاولات"))
            }

            val totalHint = finalConnection.contentLengthLong
            finalConnection.inputStream.use { installFromZip(context, it, totalHint, downloadUrl, onProgress) }
        } catch (e: CancellationException) {
            File(context.filesDir, TMP_DIR_NAME).deleteRecursively()
            throw e
        } catch (t: Throwable) {
            Log.e(TAG, "Download failed: ${t.message}", t)
            File(context.filesDir, TMP_DIR_NAME).deleteRecursively()
            Result.failure(t)
        } finally {
            try { connection?.disconnect() } catch (_: Exception) {}
        }
    }

    suspend fun importFromUri(
        context: Context,
        uri: Uri,
        onProgress: (Float, String) -> Unit = { _, _ -> }
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val input = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(IOException("تعذر فتح الملف"))
            input.use { installFromZip(context, it, -1L, uri.toString(), onProgress) }
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            File(context.filesDir, TMP_DIR_NAME).deleteRecursively()
            Result.failure(t)
        }
    }

    @Deprecated("استخدم importFromUri")
    suspend fun importLocalTiles(context: Context, onProgress: (Float, String) -> Unit = { _, _ -> }): Result<Boolean> =
        withContext(Dispatchers.IO) {
            val zipFile = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), LOCAL_ZIP_NAME)
            if (!zipFile.exists()) {
                return@withContext Result.failure(IOException("الملف $LOCAL_ZIP_NAME غير موجود."))
            }
            try {
                zipFile.inputStream().use { installFromZip(context, it, zipFile.length(), zipFile.absolutePath, onProgress) }
            } catch (t: Throwable) {
                Result.failure(t)
            }
        }

    private suspend fun installFromZip(
        context: Context,
        input: InputStream,
        totalHint: Long,
        sourceUrl: String,
        onProgress: (Float, String) -> Unit
    ): Result<Boolean> {
        val tmpDir = File(context.filesDir, TMP_DIR_NAME)
        tmpDir.deleteRecursively()
        tmpDir.mkdirs()
        val tmpTar = File(tmpDir, TILE_FILE_NAME)
        val digest = MessageDigest.getInstance("SHA-256")
        var bytes = 0L
        var found = false

        ZipInputStream(BufferedInputStream(input, 32768)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val entryName = entry.name
                if (!entry.isDirectory && (entryName.endsWith(".tar") || entryName.contains("valhalla_tiles"))) {
                    FileOutputStream(tmpTar).use { fos ->
                        val buf = ByteArray(32768)
                        val est = if (totalHint > 0) totalHint * 2 else 65L * 1024 * 1024
                        var n: Int
                        while (zip.read(buf).also { n = it } != -1) {
                            fos.write(buf, 0, n)
                            digest.update(buf, 0, n)
                            bytes += n
                            val p = 0.1f + (bytes.toFloat() / est).coerceAtMost(0.85f)
                            val mb = bytes / (1024 * 1024)
                            val percent = (p * 100).toInt()
                            onProgress(p, "جاري تثبيت الخرائط ($mb ميجابايت) $percent%...")
                        }
                        fos.fd.sync()
                    }
                    found = true
                } else if (!entry.isDirectory && entryName.endsWith("valhalla.json")) {
                    val configFile = File(tmpDir, "valhalla.json")
                    FileOutputStream(configFile).use { fos ->
                        val buf = ByteArray(8192)
                        var r: Int
                        while (zip.read(buf).also { r = it } != -1) {
                            fos.write(buf, 0, r)
                        }
                        fos.fd.sync()
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }

        if (!found) {
            tmpDir.deleteRecursively()
            return Result.failure(IOException("لم يتم العثور على $TILE_FILE_NAME داخل الأرشيف"))
        }
        if (bytes < MIN_TILE_BYTES) {
            tmpDir.deleteRecursively()
            return Result.failure(IOException("الملف المستخرج صغير جداً أو تالف"))
        }

        // [إصلاح 20] تحرير محرك Valhalla النشط قبل تبديل المجلدات
        try {
            RealValhallaNavigator.releaseEngine()
        } catch (_: Throwable) {}

        // تبديل ذري: لا نحذف القديم إلا بعد التأكد من نقل الجديد
        val dir = valhallaDir(context)
        val old = File(context.filesDir, OLD_DIR_NAME)
        old.deleteRecursively()
        if (dir.exists() && !dir.renameTo(old)) {
            tmpDir.deleteRecursively()
            return Result.failure(IOException("تعذر استبدال الملفات القديمة"))
        }
        if (!tmpDir.renameTo(dir)) {
            if (old.exists()) old.renameTo(dir)
            tmpDir.deleteRecursively()
            return Result.failure(IOException("تعذر نقل الملفات الجديدة"))
        }

        createValhallaConfig(context)
        val sha = digest.digest().joinToString("") { "%02x".format(it) }
        File(dir, MARKER_FILE_NAME).writeText(
            JSONObject().put("version", 1)
                .put("sha256", sha)
                .put("sizeBytes", bytes)
                .put("downloadedAt", System.currentTimeMillis())
                .put("sourceUrl", sourceUrl).toString()
        )

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(PREF_KEY_DOWNLOADED, true).apply()

        old.deleteRecursively()
        onProgress(1.0f, "تم اكتمال تثبيت خرائط اليمن بنجاح ✓")
        return Result.success(true)
    }

    fun createValhallaConfig(context: Context) {
        try {
            val dir = valhallaDir(context)
            val tiles = File(dir, TILE_FILE_NAME)
            val configFile = File(dir, "valhalla.json")
            configFile.writeText(
                """
                {
                  "mjolnir": {
                    "tile_dir": "${dir.absolutePath}",
                    "tile_extract": "${tiles.absolutePath}"
                  },
                  "loki": {
                    "actions": ["route", "locate", "sources_to_targets"]
                  },
                  "thor": {
                    "source_to_target_algorithm": "select_optimal"
                  },
                  "routing": { "results_namespace": "valhalla" }
                }
                """.trimIndent()
            )
        } catch (e: Exception) {
            Log.e(TAG, "createValhallaConfig error: ${e.message}")
        }
    }
}
