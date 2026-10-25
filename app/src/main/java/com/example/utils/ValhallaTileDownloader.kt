package com.example.utils

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.*
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream

/**
 * 📦 ValhallaTileDownloader
 * مدير تنزيل وتثبيت حزمة خرائط الملاحة الدقيقة لليمن بدون إنترنت (25 ميجابايت)
 * - يقوم بالتنزيل التدريجي من السيرفر مع حساب النسبة المئوية
 * - يقوم بفك الضغط التلقائي إلى الذاكرة الداخلية الخاصة بالتطبيق context.filesDir/valhalla
 * - ينتج ملفات valhalla_tiles.tar و valhalla.json وملف التحقق is_valhalla_downloaded.txt
 */
object ValhallaTileDownloader {

    const val DEFAULT_DOWNLOAD_URL = "https://github.com/maher202021/Vela2026/releases/download/yemen-valhalla-1/yemen_valhalla_tiles.zip"
    private const val VALHALLA_DIR_NAME = "valhalla"
    private const val MARKER_FILE_NAME = "is_valhalla_downloaded.txt"
    private const val TILES_TAR_NAME = "valhalla_tiles.tar"

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    /**
     * التحقق مما إذا كانت الحزمة محملة ومثبتة بنجاح
     */
    fun isTilesDownloaded(context: Context): Boolean {
        val valhallaDir = File(context.filesDir, VALHALLA_DIR_NAME)
        if (!valhallaDir.exists()) return false

        val markerFile = File(valhallaDir, MARKER_FILE_NAME)
        val tarFile = File(valhallaDir, TILES_TAR_NAME)
        return markerFile.exists() || (tarFile.exists() && tarFile.length() > 1024 * 1024)
    }

    /**
     * الحصول على مجلد الحزمة
     */
    fun getValhallaDir(context: Context): File {
        val dir = File(context.filesDir, VALHALLA_DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * تنزيل وفك ضغط حزمة الملاحة مع تحديث شريط التقدم
     */
    suspend fun downloadAndExtractTiles(
        context: Context,
        downloadUrl: String = DEFAULT_DOWNLOAD_URL,
        onProgress: (Float, String) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val valhallaDir = getValhallaDir(context)
            val tempZipFile = File(valhallaDir, "temp_yemen_valhalla.zip")

            val finalUrl = if (downloadUrl.isNotBlank()) downloadUrl else DEFAULT_DOWNLOAD_URL

            onProgress(0.05f, "بدء الاتصال بخادم الخرائط...")

            val request = Request.Builder()
                .url(finalUrl)
                .addHeader("User-Agent", "YemenServices-Navigator/2.2026")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(IOException("فشل الاتصال: رمز الاستجابة ${response.code}"))
            }

            val body = response.body ?: return@withContext Result.failure(IOException("محتوى الاستجابة فارغ"))
            val contentLength = body.contentLength()

            // 1. تنزيل الملف إلى ملف مؤقت
            onProgress(0.10f, "جاري تنزيل حزمة الخرائط (25 ميجابايت)...")
            val buffer = ByteArray(8192)
            var totalBytesRead = 0L

            body.byteStream().use { inputStream ->
                FileOutputStream(tempZipFile).use { outputStream ->
                    var bytesRead: Int
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead
                        if (contentLength > 0) {
                            val downloadProgress = 0.10f + (totalBytesRead.toFloat() / contentLength.toFloat()) * 0.65f
                            val percent = (downloadProgress * 100).toInt()
                            val mbRead = totalBytesRead / (1024 * 1024)
                            val mbTotal = contentLength / (1024 * 1024)
                            onProgress(downloadProgress, "جاري التنزيل: $mbRead من $mbTotal ميجابايت ($percent%)...")
                        }
                    }
                }
            }

            // 2. فك الضغط التلقائي
            onProgress(0.80f, "جاري فك ضغط وتثبيت بيانات الملاحة...")
            unzipFile(tempZipFile, valhallaDir) { unzipRatio ->
                val totalProgress = 0.80f + (unzipRatio * 0.15f)
                onProgress(totalProgress, "تثبيت ملفات المسارات...")
            }

            // 3. حذف الملف المؤقت وكتابة علامة التثبيت
            try {
                if (tempZipFile.exists()) tempZipFile.delete()
            } catch (_: Exception) {}

            val markerFile = File(valhallaDir, MARKER_FILE_NAME)
            markerFile.writeText("INSTALLED_AT=${System.currentTimeMillis()}\nENGINE=Valhalla\nTILES_VERSION=yemen-valhalla-1")

            onProgress(1.0f, "اكتمل تثبيت حزمة الملاحة بنجاح! 🇾🇪")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun unzipFile(zipFile: File, targetDir: File, onUnzipProgress: (Float) -> Unit) {
        val totalSize = zipFile.length().coerceAtLeast(1)
        var processedBytes = 0L

        ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val outFile = File(targetDir, entry.name)
                // الحماية من Zip Slip
                if (!outFile.canonicalPath.startsWith(targetDir.canonicalPath)) {
                    throw SecurityException("مسار غير آمن في حزمة الخرائط")
                }

                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    FileOutputStream(outFile).use { fos ->
                        val buffer = ByteArray(8192)
                        var len: Int
                        while (zis.read(buffer).also { len = it } > 0) {
                            fos.write(buffer, 0, len)
                            processedBytes += len
                            val ratio = (processedBytes.toFloat() / totalSize.toFloat()).coerceIn(0f, 1f)
                            onUnzipProgress(ratio)
                        }
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }

    /**
     * حذف الحزمة لتفريغ المساحة إذا رغب المستخدم
     */
    fun deleteTiles(context: Context): Boolean {
        return try {
            val valhallaDir = getValhallaDir(context)
            valhallaDir.deleteRecursively()
        } catch (_: Exception) {
            false
        }
    }
}
