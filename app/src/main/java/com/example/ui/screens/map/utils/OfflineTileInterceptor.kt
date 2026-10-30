package com.example.ui.screens.map.utils

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.util.Log
import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

object OfflineTileInterceptor {

    private const val TAG = "TileInterceptor"
    private const val MBTILES_PATH = "maps/yemen.mbtiles"      // داخل filesDir
    private const val USER_AGENT = "AlYemenServicesApp/1.0 (Android)"
    private const val MAX_TILE_BYTES = 2 * 1024 * 1024

    private val OSM = Regex("^https://[a-c]\\.tile\\.openstreetmap\\.org/(\\d+)/(\\d+)/(\\d+)\\.png$")
    private val HOT = Regex("^https://[a-c]\\.tile\\.openstreetmap\\.fr/hot/(\\d+)/(\\d+)/(\\d+)\\.png$")
    private val TOPO = Regex("^https://[a-c]\\.tile\\.opentopomap\\.org/(\\d+)/(\\d+)/(\\d+)\\.png$")
    private val SAT = Regex("^https://server\\.arcgisonline\\.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/(\\d+)/(\\d+)/(\\d+)$")

    private data class Tile(val source: String, val z: Int, val x: Int, val y: Int)

    private fun parse(url: String): Tile? {
        OSM.matchEntire(url)?.let { return Tile("osm", it.groupValues[1].toInt(), it.groupValues[2].toInt(), it.groupValues[3].toInt()) }
        HOT.matchEntire(url)?.let { return Tile("hot", it.groupValues[1].toInt(), it.groupValues[2].toInt(), it.groupValues[3].toInt()) }
        TOPO.matchEntire(url)?.let { return Tile("topo", it.groupValues[1].toInt(), it.groupValues[2].toInt(), it.groupValues[3].toInt()) }
        // Esri: {z}/{y}/{x}
        SAT.matchEntire(url)?.let { return Tile("sat", it.groupValues[1].toInt(), it.groupValues[3].toInt(), it.groupValues[2].toInt()) }
        return null
    }

    /** يعيد استجابة البلاطة من المصدر المحلي، أو null ليتولى WebView الطلب بنفسه. */
    fun intercept(context: Context, url: String): WebResourceResponse? {
        val tile = parse(url) ?: return null

        if (tile.source == "osm") readMbtile(context, tile)?.let { return png(it) }

        val cache = cacheFile(context, tile)
        if (cache.exists() && cache.length() > 0) return png(cache.readBytes())

        if (tile.source == "sat") return null          // لا تخزين لصور Esri
        val bytes = download(url) ?: return null
        try { writeAtomically(cache, bytes) } catch (t: Throwable) { Log.w(TAG, "cache write failed: ${t.message}") }
        return png(bytes)
    }

    private fun png(bytes: ByteArray) = WebResourceResponse("image/png", null, ByteArrayInputStream(bytes))

    private fun cacheFile(context: Context, t: Tile) =
        File(OfflineMapManager.getTileCacheDir(context), "${t.source}/${t.z}/${t.x}/${t.y}.png")

    private fun writeAtomically(target: File, bytes: ByteArray) {
        target.parentFile?.mkdirs()
        val tmp = File(target.parentFile, target.name + ".tmp")
        tmp.writeBytes(bytes)
        if (!tmp.renameTo(target)) { tmp.delete() }
    }

    private fun download(url: String): ByteArray? {
        var c: HttpURLConnection? = null
        return try {
            c = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 4_000; readTimeout = 6_000
                setRequestProperty("User-Agent", USER_AGENT)
            }
            if (c.responseCode != 200) return null
            val data = c.inputStream.use { it.readBytes() }
            if (data.isEmpty() || data.size > MAX_TILE_BYTES) null else data
        } catch (t: Throwable) {
            null            // لا اتصال: لا بلاطة (الخريطة تعرض ما هو مخزّن فقط)
        } finally {
            c?.disconnect()
        }
    }

    // ───────── MBTiles (raster png/jpg، ترميز TMS) ─────────
    @Volatile private var mbtiles: SQLiteDatabase? = null

    fun hasMbtiles(context: Context): Boolean = File(context.filesDir, MBTILES_PATH).exists()

    private fun db(context: Context): SQLiteDatabase? {
        mbtiles?.let { return it }
        synchronized(this) {
            mbtiles?.let { return it }
            val f = File(context.filesDir, MBTILES_PATH)
            if (!f.exists()) return null
            return try {
                SQLiteDatabase.openDatabase(f.absolutePath, null, SQLiteDatabase.OPEN_READONLY).also { mbtiles = it }
            } catch (t: Throwable) {
                Log.e(TAG, "open mbtiles failed: ${t.message}"); null
            }
        }
    }

    private fun readMbtile(context: Context, t: Tile): ByteArray? {
        val database = db(context) ?: return null
        val tmsY = (1 shl t.z) - 1 - t.y
        return try {
            database.rawQuery(
                "SELECT tile_data FROM tiles WHERE zoom_level=? AND tile_column=? AND tile_row=?",
                arrayOf(t.z.toString(), t.x.toString(), tmsY.toString())
            ).use { c -> if (c.moveToFirst()) c.getBlob(0) else null }
        } catch (t2: Throwable) { null }
    }

    /** استيراد ملف MBTiles يختاره المستخدم (OpenDocument). يُنسخ إلى ملف مؤقت ثم يُستبدل ذرياً. */
    fun importMbtiles(context: Context, uri: Uri): Result<Boolean> {
        return try {
            val target = File(context.filesDir, MBTILES_PATH)
            target.parentFile?.mkdirs()
            val tmp = File(target.parentFile, "yemen.mbtiles.tmp")
            context.contentResolver.openInputStream(uri)?.use { input -> tmp.outputStream().use { input.copyTo(it) } }
                ?: return Result.failure(IOException("تعذر فتح الملف"))
            if (tmp.length() < 1024) { tmp.delete(); return Result.failure(IOException("الملف صغير جداً")) }
            synchronized(this) { mbtiles?.close(); mbtiles = null }
            target.delete()
            if (!tmp.renameTo(target)) return Result.failure(IOException("تعذر حفظ الملف"))
            Result.success(true)
        } catch (t: Throwable) { Result.failure(t) }
    }
}
