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

    /**
     * استيراد حزمة خرائط الملاحة المحلية من مجلد التنزيلات (Downloads) أو مسار مخصص
     */
    suspend fun importLocalTiles(
        context: Context,
        customPathOrUri: Any? = null,
        onProgress: (Float, String) -> Unit = { _, _ -> }
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val valhallaDir = getValhallaDir(context)
            onProgress(0.1f, "البحث عن حزمة الخرائط في ذاكرة الجهاز...")

            var candidateFile: File? = null

            // 1. فحص إذا تم تمرير مسار أو ملف مباشر
            when (customPathOrUri) {
                is File -> if (customPathOrUri.exists() && customPathOrUri.length() > 0) candidateFile = customPathOrUri
                is String -> {
                    val f = File(customPathOrUri)
                    if (f.exists() && f.length() > 0) candidateFile = f
                }
            }

            // 2. فحص المجلدات الشائعة للتنزيلات
            if (candidateFile == null) {
                val downloadDirs = listOfNotNull(
                    android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS),
                    context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS),
                    context.filesDir
                )

                val candidateNames = listOf(
                    "valhalla_tiles.tar",
                    "yemen_valhalla_tiles.zip",
                    "valhalla_tiles.zip",
                    "valhalla.tar",
                    "yemen_valhalla.zip"
                )

                for (dir in downloadDirs) {
                    if (dir.exists() && dir.isDirectory) {
                        for (name in candidateNames) {
                            val f = File(dir, name)
                            if (f.exists() && f.length() > 1024 * 100) {
                                candidateFile = f
                                break
                            }
                        }
                        if (candidateFile != null) break

                        // بحث مرن عن أي ملف يحوي valhalla
                        val matched = dir.listFiles { file ->
                            file.isFile && (file.name.contains("valhalla", ignoreCase = true) || file.name.contains("yemen_tiles", ignoreCase = true))
                        }?.firstOrNull()
                        if (matched != null && matched.length() > 1024 * 100) {
                            candidateFile = matched
                            break
                        }
                    }
                }
            }

            if (candidateFile == null || !candidateFile.exists()) {
                return@withContext Result.failure(
                    FileNotFoundException("لم يتم العثور على ملف خرائط (valhalla_tiles.tar أو yemen_valhalla_tiles.zip) في مجلد التنزيلات (Downloads). يُرجى وضع الملف في التنزيلات وإعادة المحاولة.")
                )
            }

            onProgress(0.3f, "تم العثور على الملف: ${candidateFile.name} (${candidateFile.length() / (1024 * 1024)} MB)")

            if (candidateFile.name.endsWith(".tar", ignoreCase = true)) {
                // نسخ ملف .tar مباشرة
                val destFile = File(valhallaDir, TILES_TAR_NAME)
                onProgress(0.5f, "جاري استيراد ونسخ ملف المسارات إلى ذاكرة التطبيق...")
                candidateFile.inputStream().use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            } else if (candidateFile.name.endsWith(".zip", ignoreCase = true)) {
                // فك ضغط ملف .zip
                onProgress(0.5f, "جاري فك ضغط حزمة الخرائط إلى الذاكرة الداخلية...")
                unzipFile(candidateFile, valhallaDir) { ratio ->
                    val p = 0.5f + (ratio * 0.45f)
                    onProgress(p, "تثبيت ملفات المسارات (${(p * 100).toInt()}%)...")
                }
            } else {
                // نسخ عام
                val destFile = File(valhallaDir, TILES_TAR_NAME)
                candidateFile.inputStream().use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }

            // كتابة علامة التثبيت
            val markerFile = File(valhallaDir, MARKER_FILE_NAME)
            markerFile.writeText("IMPORTED_AT=${System.currentTimeMillis()}\nENGINE=Valhalla\nSOURCE=LocalImport")

            onProgress(1.0f, "اكتمل استيراد وتفعيل حزمة الملاحة بنجاح! 🇾🇪")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
