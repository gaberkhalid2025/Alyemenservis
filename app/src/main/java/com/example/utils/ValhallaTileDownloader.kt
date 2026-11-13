package com.example.utils

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.Locale
import java.util.zip.ZipInputStream

/**
 * 📦 ValhallaTileDownloader
 * [إصلاح ملاحة نهائي] إدارة تحميل وتثبيت وتكوين حزمة خرائط اليمن (Yemen Valhalla Tiles)
 * - تحميل مباشر من GitHub Releases (~26 MB مضغوطة -> ~65 MB مفكوكة، 677 بلاطة طرق لليمن)
 * - حساب التقدم الفعلي من البث المضغوط حتى 100% دون التوقف عند 95%
 * - توليد والتحقق من valhalla.json بصيغة JSON قياسية كاملة متوافقة 100% مع محرك Valhalla-mobile
 *   (بدون الاعتماد على انعكاس Moshi الذي كان يسبب فقدان service_limits.isochrone.max_contours)
 */
object ValhallaTileDownloader {

    const val DEFAULT_DOWNLOAD_URL = "https://github.com/maher202021/Vela2026/releases/download/yemen-valhalla-1/yemen_valhalla_tiles.zip"
    const val EXPECTED_DOWNLOAD_MB = 26
    const val EXPECTED_INSTALLED_MB = 65
    private const val TAG = "ValhallaDownloader"
    private const val PREFS_NAME = "valhalla_prefs"
    private const val PREF_KEY_DOWNLOADED = "is_valhalla_downloaded"
    private const val MARKER_FILE_NAME = NavConfig.VALHALLA_MARKER_FILE
    private const val VALHALLA_DIR_NAME = NavConfig.VALHALLA_DIR
    private const val TILE_FILE_NAME = NavConfig.VALHALLA_TILES_FILE
    private const val ARCHIVE_CONFIG_COPY = "valhalla_archive.json"
    private const val LOCAL_ZIP_NAME = "yemen_valhalla_tiles.zip"
    private const val TMP_DIR_NAME = "valhalla_tmp"
    private const val OLD_DIR_NAME = "valhalla_old"
    private const val MIN_TILE_BYTES = 10L * 1024L * 1024L
    private const val MIN_FREE_BYTES = 140L * 1024L * 1024L

    private class CountingInputStream(input: InputStream) : FilterInputStream(input) {
        @Volatile var bytesRead: Long = 0L
            private set

        override fun read(): Int {
            val b = super.read()
            if (b != -1) bytesRead++
            return b
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            val n = super.read(b, off, len)
            if (n > 0) bytesRead += n
            return n
        }
    }

    fun getValhallaDir(context: Context): File {
        val dir = File(context.filesDir, VALHALLA_DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private fun valhallaDir(context: Context) = File(context.filesDir, VALHALLA_DIR_NAME)

    fun resolveTilesFile(dir: File): File {
        val exact = File(dir, TILE_FILE_NAME)
        if (exact.exists() && exact.length() >= MIN_TILE_BYTES) return exact
        return dir.listFiles()?.firstOrNull { it.name.endsWith(".tar") && it.length() >= MIN_TILE_BYTES } ?: exact
    }

    fun getInstalledSizeMb(context: Context): Int {
        val tile = resolveTilesFile(valhallaDir(context))
        return if (tile.exists()) ((tile.length() + (512L * 1024L)) / (1024L * 1024L)).toInt() else 0
    }

    fun isValidTarFile(tarFile: File): Boolean {
        if (!tarFile.exists() || tarFile.length() < MIN_TILE_BYTES) return false
        return try {
            val header = ByteArray(512)
            val read = FileInputStream(tarFile).use { it.read(header) }
            if (read < 512) return false
            val magic = String(header, 257, 5, Charsets.US_ASCII)
            if (magic == "ustar") return true
            val firstName = header.takeWhile { it != 0.toByte() }.toByteArray().toString(Charsets.US_ASCII).trim()
            firstName.isNotEmpty() &&
                !firstName.startsWith("<") &&
                !firstName.startsWith("{") &&
                (firstName == "index.bin" || firstName.endsWith(".gph") || firstName.startsWith("0/") || firstName.startsWith("1/") || firstName.startsWith("2/"))
        } catch (_: Throwable) {
            false
        }
    }

    fun isTilesDownloaded(context: Context): Boolean {
        return try {
            val dir = valhallaDir(context)
            val marker = File(dir, MARKER_FILE_NAME)
            val tile = resolveTilesFile(dir)
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val isPrefMarked = prefs.getBoolean(PREF_KEY_DOWNLOADED, false)

            if (!marker.exists() && !isPrefMarked) return false
            if (!isValidTarFile(tile)) return false

            if (marker.exists()) {
                val text = marker.readText().trim()
                if (text.startsWith("{")) {
                    val expected = JSONObject(text).optLong("sizeBytes", -1L)
                    if (expected > 0 && expected != tile.length()) return false
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
                return@withContext Result.failure(IOException("مساحة التخزين غير كافية (يلزم نحو 140 ميجابايت للتثبيت الآمن)"))
            }
            onProgress(0.03f, "جاري الاتصال بخادم الخرائط...")

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
                        connection.disconnect()
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
            val totalHint = runCatching {
                context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
            }.getOrDefault(-1L)
            val input = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(IOException("تعذر فتح الملف"))
            input.use { installFromZip(context, it, totalHint, uri.toString(), onProgress) }
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
        val countingIn = CountingInputStream(BufferedInputStream(input, 32768))
        var bytes = 0L
        var found = false
        var lastReportedPercent = -1
        var lastReportedBytes = 0L

        ZipInputStream(countingIn).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val entryName = entry.name
                if (!entry.isDirectory && (entryName.endsWith(".tar") || entryName.contains("valhalla_tiles"))) {
                    val expectedUncompressed = entry.size.takeIf { it > 0L } ?: (EXPECTED_INSTALLED_MB.toLong() * 1024L * 1024L)
                    val targetUncompressedMb = ((expectedUncompressed + (512L * 1024L)) / (1024L * 1024L)).toInt().coerceAtLeast(1)
                    val totalZipMb = if (totalHint > 0L) ((totalHint + (512L * 1024L)) / (1024L * 1024L)).toInt().coerceAtLeast(1) else EXPECTED_DOWNLOAD_MB

                    FileOutputStream(tmpTar).use { fos ->
                        val buf = ByteArray(32768)
                        var n: Int
                        while (zip.read(buf).also { n = it } != -1) {
                            fos.write(buf, 0, n)
                            digest.update(buf, 0, n)
                            bytes += n

                            val fraction = if (totalHint > 0L) {
                                (countingIn.bytesRead.toFloat() / totalHint.toFloat()).coerceIn(0f, 1f)
                            } else {
                                (bytes.toFloat() / expectedUncompressed.toFloat()).coerceIn(0f, 1f)
                            }
                            val p = 0.06f + fraction * 0.88f // 6% -> 94%
                            val percent = (p * 100).toInt().coerceIn(6, 94)

                            if (percent != lastReportedPercent || (bytes - lastReportedBytes) >= 512L * 1024L) {
                                lastReportedPercent = percent
                                lastReportedBytes = bytes
                                val extractedMb = bytes / (1024L * 1024L)
                                val msg = if (totalHint > 0L) {
                                    val downloadedMb = (countingIn.bytesRead / (1024L * 1024L)).coerceAtMost(totalZipMb.toLong())
                                    "جاري التنزيل ($downloadedMb/$totalZipMb MB) والتثبيت ($extractedMb/$targetUncompressedMb MB) $percent%..."
                                } else {
                                    "جاري فك وتثبيت الخرائط ($extractedMb/$targetUncompressedMb ميجابايت) $percent%..."
                                }
                                onProgress(p, msg)
                            }
                        }
                        fos.fd.sync()
                    }
                    found = true
                } else if (!entry.isDirectory && entryName.endsWith("valhalla.json")) {
                    val archiveConfig = File(tmpDir, ARCHIVE_CONFIG_COPY)
                    FileOutputStream(archiveConfig).use { fos ->
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
        if (!isValidTarFile(tmpTar)) {
            tmpDir.deleteRecursively()
            return Result.failure(IOException("ملف الخرائط المستخرج ناقص أو تالف (${bytes / (1024 * 1024)} MB)"))
        }

        onProgress(0.95f, "جاري تهيئة إعدادات محرك الملاحة...")

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

        val configOk = createValhallaConfig(context)
        if (!configOk || !isValhallaConfigValid(context)) {
            // استعادة النسخة القديمة إن فشل الإعداد
            dir.deleteRecursively()
            if (old.exists()) old.renameTo(dir)
            return Result.failure(IOException("تعذر كتابة ملف إعدادات محرك Valhalla"))
        }

        val sha = digest.digest().joinToString("") { "%02x".format(it) }
        File(dir, MARKER_FILE_NAME).writeText(
            JSONObject().put("version", 2)
                .put("sha256", sha)
                .put("sizeBytes", bytes)
                .put("compressedBytes", countingIn.bytesRead)
                .put("downloadedAt", System.currentTimeMillis())
                .put("sourceUrl", sourceUrl).toString()
        )

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(PREF_KEY_DOWNLOADED, true).apply()

        old.deleteRecursively()
        val installedMb = ((bytes + (512L * 1024L)) / (1024L * 1024L)).toInt()
        onProgress(1.0f, "تم اكتمال تثبيت خرائط اليمن ($installedMb ميجابايت) بنجاح ✓")
        return Result.success(true)
    }

    suspend fun repairConfig(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            RealValhallaNavigator.releaseEngine()
        } catch (_: Throwable) {}
        createValhallaConfig(context) && isValhallaConfigValid(context)
    }

    suspend fun clearCorruptedTiles(context: Context) = withContext(Dispatchers.IO) {
        try {
            RealValhallaNavigator.releaseEngine()
        } catch (_: Throwable) {}
        valhallaDir(context).deleteRecursively()
        File(context.filesDir, TMP_DIR_NAME).deleteRecursively()
        File(context.filesDir, OLD_DIR_NAME).deleteRecursively()
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(PREF_KEY_DOWNLOADED, false).apply()
    }

    fun isValhallaConfigValid(context: Context): Boolean {
        val dir = valhallaDir(context)
        val tiles = resolveTilesFile(dir)
        val configFile = File(dir, "valhalla.json")
        if (!configFile.exists() || !tiles.exists()) return false
        return runCatching {
            isValhallaConfigJsonValid(configFile.readText(), dir.absolutePath, tiles.absolutePath)
        }.getOrDefault(false)
    }

    fun isValhallaConfigJsonValid(configText: String, expectedTileDir: String, expectedTileExtract: String): Boolean {
        if (configText.contains("/custom_files/") || configText.contains("/data/valhalla")) return false
        val root = JSONObject(configText)
        val mjolnir = root.optJSONObject("mjolnir") ?: return false
        if (mjolnir.optString("tile_extract") != expectedTileExtract) return false
        if (mjolnir.optString("tile_dir") != expectedTileDir) return false

        val sl = root.optJSONObject("service_limits") ?: return false
        val isochrone = sl.optJSONObject("isochrone") ?: return false
        if (isochrone.optInt("max_contours", 0) <= 0) return false
        if (isochrone.optInt("max_distance_contour", 0) <= 0) return false
        if (isochrone.optInt("max_time_contour", 0) <= 0) return false
        if (sl.optInt("max_radius", 0) < 800) return false

        val pedestrian = sl.optJSONObject("pedestrian") ?: return false
        if (!pedestrian.has("max_multimodal_walking_distance") || !pedestrian.has("max_transit_walking_distance")) return false
        if (sl.optJSONObject("hierarchy_limits")?.optJSONObject("bidirectional_astar") == null) return false
        if ((sl.optJSONObject("trace")?.optInt("max_shape", 0) ?: 0) <= 0) return false
        if ((sl.optJSONObject("skadi")?.optInt("max_shape", 0) ?: 0) <= 0) return false

        val lokiDefaults = root.optJSONObject("loki")?.optJSONObject("service_defaults") ?: return false
        if (lokiDefaults.optInt("radius", 0) < 50) return false

        val thor = root.optJSONObject("thor") ?: return false
        if (thor.optJSONObject("costmatrix") == null || thor.optJSONObject("bidirectional_astar") == null) return false
        return true
    }

    /**
     * يُنشئ أو يُصلح valhalla.json بصيغة JSON خالصة 100% (بدون Moshi reflection)
     * انطلاقاً من default.json الرسمي المدمج في valhalla-mobile مع دمج خيارات الأرشيف وضبط المسارات المحلية.
     */
    fun createValhallaConfig(context: Context): Boolean {
        return try {
            val dir = valhallaDir(context)
            val tiles = resolveTilesFile(dir)
            val configFile = File(dir, "valhalla.json")
            val archiveCopy = File(dir, ARCHIVE_CONFIG_COPY)

            val resourceDefaultText = runCatching {
                com.valhalla.valhalla.Valhalla::class.java
                    .getResourceAsStream("/com/valhalla/valhalla/default.json")
                    ?.bufferedReader()
                    ?.use { it.readText() }
            }.getOrNull()

            val archiveText = archiveCopy.takeIf { it.exists() }?.runCatching { readText() }?.getOrNull()
                ?: configFile.takeIf { it.exists() }?.runCatching { readText() }?.getOrNull()
                    ?.takeIf { it.contains("max_contours") }

            val hasAdminDb = File(dir, "admins.sqlite").exists()
            val hasTimezoneDb = File(dir, "timezones.sqlite").exists()
            val hasLandmarksDb = File(dir, "landmarks.sqlite").exists()
            val hasTrafficTar = File(dir, "traffic.tar").exists()
            val hasElevationDir = File(dir, "elevation").exists()

            val finalJsonText = buildConfigJsonString(
                tileDirPath = dir.absolutePath,
                tileExtractPath = tiles.absolutePath,
                archiveJsonText = archiveText,
                resourceDefaultJsonText = resourceDefaultText,
                hasAdminDb = hasAdminDb,
                adminPath = if (hasAdminDb) File(dir, "admins.sqlite").absolutePath else "",
                timezonePath = if (hasTimezoneDb) File(dir, "timezones.sqlite").absolutePath else "",
                landmarksPath = if (hasLandmarksDb) File(dir, "landmarks.sqlite").absolutePath else "",
                trafficExtractPath = if (hasTrafficTar) File(dir, "traffic.tar").absolutePath else "",
                elevationPath = if (hasElevationDir) File(dir, "elevation").absolutePath else ""
            )

            val tmpConfig = File(dir, "valhalla.json.tmp")
            tmpConfig.writeText(finalJsonText)
            if (!tmpConfig.renameTo(configFile)) {
                configFile.writeText(finalJsonText)
                tmpConfig.delete()
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "createValhallaConfig error: ${e.message}", e)
            false
        }
    }

    fun buildConfigJsonString(
        tileDirPath: String,
        tileExtractPath: String,
        archiveJsonText: String? = null,
        resourceDefaultJsonText: String? = null,
        hasAdminDb: Boolean = false,
        adminPath: String = "",
        timezonePath: String = "",
        landmarksPath: String = "",
        trafficExtractPath: String = "",
        elevationPath: String = ""
    ): String {
        val baseText = resourceDefaultJsonText?.takeIf {
            it.isNotBlank() && it.contains("isochrone") && it.contains("max_contours")
        } ?: FALLBACK_DEFAULT_VALHALLA_CONFIG_JSON

        val root = JSONObject(baseText)

        // دمج خفيف مع ملف الأرشيف (للحفاظ على أي حدود إضافية متوافقة مع الحزمة دون المساس بالهيكل الأساسي)
        if (!archiveJsonText.isNullOrBlank()) {
            runCatching {
                val archive = JSONObject(archiveJsonText)
                val archiveSl = archive.optJSONObject("service_limits")
                val targetSl = root.optJSONObject("service_limits")
                if (archiveSl != null && targetSl != null) {
                    val archPed = archiveSl.optJSONObject("pedestrian")
                    val tgtPed = targetSl.optJSONObject("pedestrian")
                    if (archPed != null && tgtPed != null) {
                        if (archPed.has("max_transit_walking_distance")) {
                            tgtPed.put("max_transit_walking_distance", archPed.getInt("max_transit_walking_distance"))
                        }
                        if (archPed.has("min_transit_walking_distance")) {
                            tgtPed.put("min_transit_walking_distance", archPed.getInt("min_transit_walking_distance"))
                        }
                    }
                }
            }
        }

        // مسارات mjolnir المحلية على أندرويد
        val mjolnir = root.optJSONObject("mjolnir") ?: JSONObject().also { root.put("mjolnir", it) }
        mjolnir.put("tile_dir", tileDirPath)
        mjolnir.put("tile_extract", tileExtractPath)
        mjolnir.put("traffic_extract", trafficExtractPath)
        mjolnir.put("admin", adminPath)
        mjolnir.put("timezone", timezonePath)
        mjolnir.put("landmarks", landmarksPath)
        mjolnir.put("transit_dir", "")
        mjolnir.put("transit_feeds_dir", "")
        val dataProcessing = mjolnir.optJSONObject("data_processing")
            ?: JSONObject().also { mjolnir.put("data_processing", it) }
        dataProcessing.put("use_admin_db", hasAdminDb)
        dataProcessing.put("scan_tar", false)

        // additional_data
        val additionalData = root.optJSONObject("additional_data")
            ?: JSONObject().also { root.put("additional_data", it) }
        additionalData.put("elevation", elevationPath)

        // loki: نطاق التقاط طريق مناسب للطرق الريفية والشوارع في اليمن
        val loki = root.optJSONObject("loki") ?: JSONObject().also { root.put("loki", it) }
        val serviceDefaults = loki.optJSONObject("service_defaults")
            ?: JSONObject().also { loki.put("service_defaults", it) }
        serviceDefaults.put("radius", maxOf(serviceDefaults.optInt("radius", 0), 50))
        serviceDefaults.put("street_side_max_distance", maxOf(serviceDefaults.optInt("street_side_max_distance", 0), 2000))
        serviceDefaults.put("search_cutoff", maxOf(serviceDefaults.optInt("search_cutoff", 0), 35000))

        // service_limits: ضمان وجود كل العقد الحرجة (خاصة isochrone.max_contours و pedestrian و max_radius)
        val serviceLimits = root.optJSONObject("service_limits")
            ?: JSONObject().also { root.put("service_limits", it) }
        serviceLimits.put("max_radius", maxOf(serviceLimits.optInt("max_radius", 0), 5000))

        val isochrone = serviceLimits.optJSONObject("isochrone")
            ?: JSONObject().also { serviceLimits.put("isochrone", it) }
        if (isochrone.optInt("max_contours", 0) <= 0) isochrone.put("max_contours", 4)
        if (isochrone.optInt("max_time_contour", 0) <= 0) isochrone.put("max_time_contour", 120)
        if (isochrone.optDouble("max_distance", 0.0) <= 0.0) isochrone.put("max_distance", 25000.0)
        if (isochrone.optInt("max_locations", 0) <= 0) isochrone.put("max_locations", 1)
        if (isochrone.optInt("max_distance_contour", 0) <= 0) isochrone.put("max_distance_contour", 200)

        val pedestrian = serviceLimits.optJSONObject("pedestrian")
            ?: JSONObject().also { serviceLimits.put("pedestrian", it) }
        if (!pedestrian.has("min_multimodal_walking_distance")) pedestrian.put("min_multimodal_walking_distance", 1)
        if (!pedestrian.has("max_multimodal_walking_distance")) pedestrian.put("max_multimodal_walking_distance", 10000)
        if (!pedestrian.has("min_transit_walking_distance")) pedestrian.put("min_transit_walking_distance", 1)
        if (!pedestrian.has("max_transit_walking_distance")) pedestrian.put("max_transit_walking_distance", 10000)

        return root.toString(2)
    }

    private val FALLBACK_DEFAULT_VALHALLA_CONFIG_JSON = """
    {
      "logging": {
        "type": "std_out",
        "color": true,
        "file_name": "path_to_some_file.log"
      },
      "mjolnir": {
        "max_cache_size": 1000000000,
        "id_table_size": 1300000000,
        "use_lru_mem_cache": false,
        "lru_mem_cache_hard_control": false,
        "use_simple_mem_cache": false,
        "tile_dir": "",
        "tile_extract": "",
        "traffic_extract": "",
        "admin": "",
        "landmarks": "",
        "timezone": "",
        "transit_dir": "",
        "transit_feeds_dir": "",
        "transit_pbf_limit": 20000,
        "hierarchy": true,
        "shortcuts": true,
        "keep_all_osm_node_ids": false,
        "keep_osm_node_ids": false,
        "include_platforms": false,
        "include_driveways": true,
        "include_construction": false,
        "include_bicycle": true,
        "include_pedestrian": true,
        "include_driving": true,
        "import_bike_share_stations": false,
        "global_synchronized_cache": false,
        "max_concurrent_reader_users": 1,
        "reclassify_links": true,
        "pedestrian_areas": false,
        "data_processing": {
          "infer_internal_intersections": true,
          "infer_turn_channels": true,
          "apply_country_overrides": true,
          "use_admin_db": false,
          "use_direction_on_ways": false,
          "allow_alt_name": false,
          "use_urban_tag": false,
          "use_rest_area": false,
          "scan_tar": false,
          "build_bounding_circles": true
        }
      },
      "additional_data": {
        "elevation": ""
      },
      "loki": {
        "actions": [
          "locate",
          "route",
          "height",
          "sources_to_targets",
          "optimized_route",
          "isochrone",
          "trace_route",
          "trace_attributes",
          "transit_available",
          "expansion",
          "centroid",
          "status",
          "tile"
        ],
        "use_connectivity": true,
        "service_defaults": {
          "radius": 50,
          "minimum_reachability": 50,
          "search_cutoff": 35000,
          "node_snap_tolerance": 5,
          "street_side_tolerance": 5,
          "street_side_max_distance": 2000,
          "heading_tolerance": 60,
          "mvt_min_zoom_road_class": [7, 7, 8, 11, 11, 12, 13, 14],
          "mvt_cache_min_zoom": 11,
          "mvt_max_age": "1800"
        },
        "service": {
          "proxy": "ipc:///tmp/loki"
        }
      },
      "thor": {
        "source_to_target_algorithm": "select_optimal",
        "service": {
          "proxy": "ipc:///tmp/thor"
        },
        "max_reserved_labels_count_astar": 2000000,
        "max_reserved_labels_count_bidir_astar": 1000000,
        "max_reserved_labels_count_dijkstras": 4000000,
        "max_reserved_labels_count_bidir_dijkstras": 2000000,
        "clear_reserved_memory": false,
        "extended_search": false,
        "costmatrix": {
          "check_reverse_connection": true,
          "allow_second_pass": false,
          "max_reserved_locations": 25,
          "max_iterations": 2800,
          "min_iterations": 100,
          "dijkstra_distance": 0,
          "hierarchy_limits": {
            "max_up_transitions": { "1": 400, "2": 100 },
            "expand_within_distance": { "0": 100000000.0, "1": 20000, "2": 5000 }
          }
        },
        "bidirectional_astar": {
          "threshold_delta": 420.0,
          "alternative_cost_extend": 1.2,
          "alternative_iterations_delta": 100000,
          "hierarchy_limits": {
            "max_up_transitions": { "1": 400, "2": 100 },
            "expand_within_distance": { "0": 100000000.0, "1": 20000, "2": 5000 }
          }
        },
        "unidirectional_astar": {
          "hierarchy_limits": {
            "max_up_transitions": { "1": 400, "2": 100 },
            "expand_within_distance": { "0": 100000000.0, "1": 20000, "2": 5000 }
          }
        }
      },
      "odin": {
        "service": {
          "proxy": "ipc:///tmp/odin"
        },
        "markup_formatter": {
          "markup_enabled": false,
          "phoneme_format": "<TEXTUAL_STRING> (<span class=<QUOTES>phoneme<QUOTES>>/<VERBAL_STRING>/</span>)"
        }
      },
      "meili": {
        "mode": "auto",
        "customizable": [
          "mode",
          "search_radius",
          "turn_penalty_factor",
          "gps_accuracy",
          "interpolation_distance",
          "sigma_z",
          "beta",
          "max_route_distance_factor",
          "max_route_time_factor"
        ],
        "verbose": false,
        "default": {
          "sigma_z": 4.07,
          "gps_accuracy": 5.0,
          "beta": 3,
          "max_route_distance_factor": 5,
          "max_route_time_factor": 5,
          "max_search_radius": 100,
          "breakage_distance": 2000,
          "interpolation_distance": 10,
          "search_radius": 50,
          "geometry": false,
          "route": true,
          "turn_penalty_factor": 0
        },
        "auto": {
          "turn_penalty_factor": 200,
          "search_radius": 50
        },
        "pedestrian": {
          "turn_penalty_factor": 100,
          "search_radius": 50
        },
        "bicycle": {
          "turn_penalty_factor": 140
        },
        "multimodal": {
          "turn_penalty_factor": 70
        },
        "service": {
          "proxy": "ipc:///tmp/meili"
        },
        "grid": {
          "size": 500,
          "cache_size": 100240
        }
      },
      "httpd": {
        "service": {
          "listen": "tcp://*:8002",
          "loopback": "ipc:///tmp/loopback",
          "interrupt": "ipc:///tmp/interrupt",
          "drain_seconds": 28,
          "shutdown_seconds": 1,
          "timeout_seconds": -1
        }
      },
      "service_limits": {
        "auto": { "max_distance": 5000000.0, "max_locations": 20, "max_matrix_distance": 400000.0, "max_matrix_location_pairs": 2500 },
        "bus": { "max_distance": 5000000.0, "max_locations": 50, "max_matrix_distance": 400000.0, "max_matrix_location_pairs": 2500 },
        "taxi": { "max_distance": 5000000.0, "max_locations": 20, "max_matrix_distance": 400000.0, "max_matrix_location_pairs": 2500 },
        "pedestrian": {
          "max_distance": 250000.0,
          "max_locations": 50,
          "max_matrix_distance": 200000.0,
          "max_matrix_location_pairs": 2500,
          "min_multimodal_walking_distance": 1,
          "max_multimodal_walking_distance": 10000,
          "min_transit_walking_distance": 1,
          "max_transit_walking_distance": 10000
        },
        "motor_scooter": { "max_distance": 500000.0, "max_locations": 50, "max_matrix_distance": 200000.0, "max_matrix_location_pairs": 2500 },
        "motorcycle": { "max_distance": 500000.0, "max_locations": 50, "max_matrix_distance": 200000.0, "max_matrix_location_pairs": 2500 },
        "bicycle": { "max_distance": 500000.0, "max_locations": 50, "max_matrix_distance": 200000.0, "max_matrix_location_pairs": 2500 },
        "multimodal": { "max_distance": 500000.0, "max_locations": 50, "max_matrix_distance": 0.0, "max_matrix_location_pairs": 0 },
        "status": { "allow_verbose": false },
        "transit": { "max_distance": 500000.0, "max_locations": 50, "max_matrix_distance": 200000.0, "max_matrix_location_pairs": 2500 },
        "truck": { "max_distance": 5000000.0, "max_locations": 20, "max_matrix_distance": 400000.0, "max_matrix_location_pairs": 2500 },
        "skadi": { "max_shape": 750000, "min_resample": 10.0 },
        "isochrone": { "max_contours": 4, "max_time_contour": 120, "max_distance": 25000.0, "max_locations": 1, "max_distance_contour": 200 },
        "trace": { "max_distance": 200000.0, "max_gps_accuracy": 100.0, "max_search_radius": 100.0, "max_shape": 16000, "max_alternates": 3, "max_alternates_shape": 100 },
        "bikeshare": { "max_distance": 500000.0, "max_locations": 50, "max_matrix_distance": 200000.0, "max_matrix_location_pairs": 2500 },
        "auto_pedestrian": { "max_distance": 5000000.0, "max_matrix_distance": 400000.0, "max_matrix_location_pairs": 2500 },
        "centroid": { "max_distance": 200000.0, "max_locations": 5 },
        "max_exclude_locations": 50,
        "max_reachability": 100,
        "max_radius": 5000,
        "max_timedep_distance": 500000,
        "max_timedep_distance_matrix": 0,
        "max_alternates": 2,
        "max_exclude_polygons_length": 10000,
        "max_exclude_polygons_vertices": 100,
        "min_linear_cost_factor": 1,
        "max_linear_cost_edges": 50000,
        "max_distance_disable_hierarchy_culling": 0,
        "hierarchy_limits": {
          "allow_modification": false,
          "costmatrix": {
            "max_allowed_up_transitions": { "1": 400, "2": 100 },
            "max_expand_within_distance": { "0": 100000000.0, "1": 100000, "2": 5000 }
          },
          "unidirectional_astar": {
            "max_allowed_up_transitions": { "1": 400, "2": 100 },
            "max_expand_within_distance": { "0": 100000000.0, "1": 100000, "2": 5000 }
          },
          "bidirectional_astar": {
            "max_allowed_up_transitions": { "1": 400, "2": 100 },
            "max_expand_within_distance": { "0": 100000000.0, "1": 20000, "2": 5000 }
          }
        },
        "allow_hard_exclusions": false
      },
      "statsd": {
        "port": 8125,
        "prefix": "valhalla",
        "batch_size": 500
      }
    }
    """.trimIndent()
}
