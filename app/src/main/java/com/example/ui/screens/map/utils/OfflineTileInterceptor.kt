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
    private const val CACHE_TTL_MS = 30L * 24L * 3600L * 1000L // 30 يوماً لتوفير الباقة
    private const val MAX_DISK_CACHE_BYTES = 120L * 1024L * 1024L

    private data class CachedTile(val bytes: ByteArray, val mime: String)
    private val memoryCache = object : android.util.LruCache<String, CachedTile>(10 * 1024 * 1024) {
        override fun sizeOf(key: String, value: CachedTile): Int = value.bytes.size
    }
    @Volatile private var lastPruneAt = 0L

    private val CORS_HEADERS = mapOf(
        "Access-Control-Allow-Origin" to "*",
        "Access-Control-Allow-Methods" to "GET, HEAD, OPTIONS",
        "Access-Control-Allow-Headers" to "*",
        "Cache-Control" to "public, max-age=2592000"
    )

    private val OSM = Regex("^https://(?:[a-c]\\.)?tile\\.openstreetmap\\.org/(\\d+)/(\\d+)/(\\d+)\\.png$")
    private val OSM_DE = Regex("^https://tile\\.openstreetmap\\.de/(\\d+)/(\\d+)/(\\d+)\\.png$")
    private val HOT = Regex("^https://[a-c]\\.tile\\.openstreetmap\\.fr/hot/(\\d+)/(\\d+)/(\\d+)\\.png$")
    private val TOPO = Regex("^https://[a-c]\\.tile\\.opentopomap\\.org/(\\d+)/(\\d+)/(\\d+)\\.png$")
    private val ESRI_TOPO = Regex("^https://server\\.arcgisonline\\.com/ArcGIS/rest/services/World_Topo_Map/MapServer/tile/(\\d+)/(\\d+)/(\\d+)$")
    private val CARTO_VOYAGER = Regex("^https://[a-d]\\.basemaps\\.cartocdn\\.com/rastertiles/voyager/(\\d+)/(\\d+)/(\\d+)(?:@2x)?\\.png$")
    private val SAT = Regex("^https://server\\.arcgisonline\\.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/(\\d+)/(\\d+)/(\\d+)$")
    private val SAT_CLARITY = Regex("^https://clarity\\.maptiles\\.arcgis\\.com/arcgis/rest/services/World_Imagery/MapServer/tile/(\\d+)/(\\d+)/(\\d+)$")
    private val VELA = Regex("^https://server\\.arcgisonline\\.com/ArcGIS/rest/services/World_Street_Map/MapServer/tile/(\\d+)/(\\d+)/(\\d+)$")
    private val ESRI_LABELS = Regex("^https://server\\.arcgisonline\\.com/ArcGIS/rest/services/Reference/World_Boundaries_and_Places/MapServer/tile/(\\d+)/(\\d+)/(\\d+)$")
    private val ESRI_TRANS = Regex("^https://server\\.arcgisonline\\.com/ArcGIS/rest/services/Reference/World_Transportation/MapServer/tile/(\\d+)/(\\d+)/(\\d+)$")
    private val CARTO_LABELS = Regex("^https://[a-d]\\.basemaps\\.cartocdn\\.com/rastertiles/voyager_only_labels/(\\d+)/(\\d+)/(\\d+)(?:@2x)?\\.png$")
    private val EOX = Regex("^https://tiles\\.maps\\.eox\\.at/wmts/1\\.0\\.0/s2cloudless_3857/default/GoogleMapsCompatible/(\\d+)/(\\d+)/(\\d+)\\.jpg$")
    private val DEM_TERRARIUM = Regex("^https://s3\\.amazonaws\\.com/elevation-tiles-prod/terrarium/(\\d+)/(\\d+)/(\\d+)\\.png$")

    private data class Tile(val source: String, val z: Int, val x: Int, val y: Int, val ext: String, val mime: String) {
        val key: String get() = "$source/$z/$x/$y"
    }

    private fun parse(url: String): Tile? {
        OSM.matchEntire(url)?.let { return Tile("osm", it.groupValues[1].toInt(), it.groupValues[2].toInt(), it.groupValues[3].toInt(), "png", "image/png") }
        OSM_DE.matchEntire(url)?.let { return Tile("osm_de", it.groupValues[1].toInt(), it.groupValues[2].toInt(), it.groupValues[3].toInt(), "png", "image/png") }
        HOT.matchEntire(url)?.let { return Tile("hot", it.groupValues[1].toInt(), it.groupValues[2].toInt(), it.groupValues[3].toInt(), "png", "image/png") }
        TOPO.matchEntire(url)?.let { return Tile("topo", it.groupValues[1].toInt(), it.groupValues[2].toInt(), it.groupValues[3].toInt(), "png", "image/png") }
        CARTO_VOYAGER.matchEntire(url)?.let { return Tile("carto_voyager", it.groupValues[1].toInt(), it.groupValues[2].toInt(), it.groupValues[3].toInt(), "png", "image/png") }
        CARTO_LABELS.matchEntire(url)?.let { return Tile("carto_labels", it.groupValues[1].toInt(), it.groupValues[2].toInt(), it.groupValues[3].toInt(), "png", "image/png") }
        DEM_TERRARIUM.matchEntire(url)?.let { return Tile("dem", it.groupValues[1].toInt(), it.groupValues[2].toInt(), it.groupValues[3].toInt(), "png", "image/png") }
        // Esri & EOX: {z}/{y}/{x}
        ESRI_TOPO.matchEntire(url)?.let { return Tile("esri_topo", it.groupValues[1].toInt(), it.groupValues[3].toInt(), it.groupValues[2].toInt(), "jpg", "image/jpeg") }
        VELA.matchEntire(url)?.let { return Tile("vela_esri", it.groupValues[1].toInt(), it.groupValues[3].toInt(), it.groupValues[2].toInt(), "jpg", "image/jpeg") }
        SAT.matchEntire(url)?.let { return Tile("sat", it.groupValues[1].toInt(), it.groupValues[3].toInt(), it.groupValues[2].toInt(), "jpg", "image/jpeg") }
        SAT_CLARITY.matchEntire(url)?.let { return Tile("sat_clarity", it.groupValues[1].toInt(), it.groupValues[3].toInt(), it.groupValues[2].toInt(), "jpg", "image/jpeg") }
        ESRI_LABELS.matchEntire(url)?.let { return Tile("esri_labels", it.groupValues[1].toInt(), it.groupValues[3].toInt(), it.groupValues[2].toInt(), "png", "image/png") }
        ESRI_TRANS.matchEntire(url)?.let { return Tile("esri_trans", it.groupValues[1].toInt(), it.groupValues[3].toInt(), it.groupValues[2].toInt(), "png", "image/png") }
        EOX.matchEntire(url)?.let { return Tile("eox16", it.groupValues[1].toInt(), it.groupValues[3].toInt(), it.groupValues[2].toInt(), "jpg", "image/jpeg") }
        return null
    }

    private fun mirrorUrls(t: Tile): List<Pair<String, String>> = when (t.source) {
        "topo" -> listOf(
            "https://server.arcgisonline.com/ArcGIS/rest/services/World_Topo_Map/MapServer/tile/${t.z}/${t.y}/${t.x}" to "image/jpeg",
            "https://a.basemaps.cartocdn.com/rastertiles/voyager/${t.z}/${t.x}/${t.y}.png" to "image/png"
        )
        "osm", "osm_de", "hot" -> listOf(
            "https://a.basemaps.cartocdn.com/rastertiles/voyager/${t.z}/${t.x}/${t.y}.png" to "image/png",
            "https://server.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer/tile/${t.z}/${t.y}/${t.x}" to "image/jpeg"
        )
        "sat_clarity" -> listOf(
            "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/${t.z}/${t.y}/${t.x}" to "image/jpeg"
        )
        "vela_esri", "carto_voyager" -> listOf(
            "https://server.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer/tile/${t.z}/${t.y}/${t.x}" to "image/jpeg",
            "https://tile.openstreetmap.org/${t.z}/${t.x}/${t.y}.png" to "image/png"
        )
        else -> emptyList()
    }

    private fun findFallbackCachedTile(context: Context, requested: Tile): Pair<ByteArray, String>? {
        if (requested.source in setOf("dem", "eox16", "esri_labels", "esri_trans", "carto_labels")) return null
        val fallbackOrder = listOf(
            "sat" to ("jpg" to "image/jpeg"),
            "sat_clarity" to ("jpg" to "image/jpeg"),
            "vela_esri" to ("jpg" to "image/jpeg"),
            "carto_voyager" to ("png" to "image/png"),
            "esri_topo" to ("jpg" to "image/jpeg"),
            "osm" to ("png" to "image/png"),
            "hot" to ("png" to "image/png"),
            "topo" to ("png" to "image/png")
        )
        for ((src, extMime) in fallbackOrder) {
            if (src == requested.source) continue
            if (src == "osm") {
                readMbtile(context, requested.copy(source = "osm", ext = "png", mime = "image/png"))?.let {
                    return it to "image/png"
                }
            }
            val f = File(OfflineMapManager.getTileCacheDir(context), "$src/${requested.z}/${requested.x}/${requested.y}.${extMime.first}")
            if (f.exists() && f.length() > 0) {
                return f.readBytes() to extMime.second
            }
        }
        return null
    }

    /** يعيد استجابة البلاطة من الذاكرة أو الكاش المحلي أو الشبكة (مع وضع أوفلاين صارم لـ MapLayer.VALHALLA_OFFLINE). */
    fun intercept(context: Context, url: String, offlineOnly: Boolean = false): WebResourceResponse? {
        val tile = parse(url) ?: return null

        memoryCache.get(tile.key)?.let { cached ->
            return tileResponse(cached.bytes, cached.mime)
        }

        if (tile.source == "osm") {
            readMbtile(context, tile)?.let { bytes ->
                memoryCache.put(tile.key, CachedTile(bytes, "image/png"))
                return tileResponse(bytes, "image/png")
            }
        }

        val cache = cacheFile(context, tile)
        val eco = com.example.utils.MapPowerDataPrefs.isEcoMode(context)
        if (cache.exists() && cache.length() > 0) {
            val age = System.currentTimeMillis() - cache.lastModified()
            if (offlineOnly || eco || age <= CACHE_TTL_MS) {
                val bytes = cache.readBytes()
                memoryCache.put(tile.key, CachedTile(bytes, tile.mime))
                return tileResponse(bytes, tile.mime)
            }
        }

        if (offlineOnly) {
            findFallbackCachedTile(context, tile)?.let { (bytes, mime) ->
                memoryCache.put(tile.key, CachedTile(bytes, mime))
                return tileResponse(bytes, mime)
            }
            return WebResourceResponse("text/plain", "utf-8", 404, "Not Cached", CORS_HEADERS, ByteArrayInputStream(ByteArray(0)))
        }

        val downloaded = download(url)?.let { it to tile.mime } ?: run {
            var mirrorResult: Pair<ByteArray, String>? = null
            for ((mirrorUrl, mirrorMime) in mirrorUrls(tile)) {
                val mBytes = download(mirrorUrl)
                if (mBytes != null) {
                    mirrorResult = mBytes to mirrorMime
                    break
                }
            }
            mirrorResult
        }

        if (downloaded == null) {
            // إذا فشل الاتصال نعيد النسخة المخزنة حتى لو قديمة أو بديل متوفر محلياً
            if (cache.exists() && cache.length() > 0) {
                val old = cache.readBytes()
                memoryCache.put(tile.key, CachedTile(old, tile.mime))
                return tileResponse(old, tile.mime)
            }
            findFallbackCachedTile(context, tile)?.let { (fallbackBytes, mime) ->
                memoryCache.put(tile.key, CachedTile(fallbackBytes, mime))
                return tileResponse(fallbackBytes, mime)
            }
            return WebResourceResponse("text/plain", "utf-8", 504, "Offline", CORS_HEADERS, ByteArrayInputStream(ByteArray(0)))
        }

        val (bytes, mime) = downloaded
        memoryCache.put(tile.key, CachedTile(bytes, mime))
        if (mime == tile.mime) {
            try {
                writeAtomically(cache, bytes)
                maybePruneDiskCache(context)
            } catch (t: Throwable) {
                Log.w(TAG, "cache write failed: ${t.message}")
            }
        }
        return tileResponse(bytes, mime)
    }

    private fun tileResponse(bytes: ByteArray, mime: String) =
        WebResourceResponse(mime, null, 200, "OK", CORS_HEADERS, ByteArrayInputStream(bytes))

    private fun cacheFile(context: Context, t: Tile) =
        File(OfflineMapManager.getTileCacheDir(context), "${t.source}/${t.z}/${t.x}/${t.y}.${t.ext}")

    private fun maybePruneDiskCache(context: Context) {
        val now = System.currentTimeMillis()
        if (now - lastPruneAt < 10 * 60_000L) return
        lastPruneAt = now
        try {
            val dir = OfflineMapManager.getTileCacheDir(context)
            val files = dir.walkTopDown().filter { it.isFile && !it.name.endsWith(".tmp") }.toList()
            var total = files.sumOf { it.length() }
            if (total <= MAX_DISK_CACHE_BYTES) return
            for (f in files.sortedBy { it.lastModified() }) {
                if (total <= MAX_DISK_CACHE_BYTES * 8 / 10) break
                val len = f.length()
                if (f.delete()) total -= len
            }
        } catch (_: Throwable) {}
    }

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
                connectTimeout = 3_000; readTimeout = 4_500
                useCaches = true
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
