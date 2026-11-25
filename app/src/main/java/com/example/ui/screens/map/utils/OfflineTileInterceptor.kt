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

// [نظام خرائط وملاحة عالمي - ملكنا]
object OfflineTileInterceptor {

    private const val TAG = "TileInterceptor"
    private const val MBTILES_PATH = "maps/yemen.mbtiles"      // داخل filesDir
    private const val USER_AGENT = "AlYemenServicesApp/1.0 (Android)"
    private const val MAX_TILE_BYTES = 2 * 1024 * 1024
    private const val CACHE_TTL_MS = 30L * 24L * 3600L * 1000L // 30 يوماً لتوفير الباقة
    private const val MAX_DISK_CACHE_BYTES = 120L * 1024L * 1024L
    private const val CACHE_VERSION = 4

    private val CACHEABLE_HOSTS = setOf(
        "tile.openstreetmap.org",
        "tile.openstreetmap.fr",
        "tile.opentopomap.org",
        "server.arcgisonline.com",
        "clarity.maptiles.arcgis.com",
        "tiles.maps.eox.at",
        "s3.amazonaws.com"
    )

    fun migrateCacheIfNeeded(context: Context) {
        val prefs = context.getSharedPreferences("tile_cache_meta", Context.MODE_PRIVATE)
        if (prefs.getInt("v", 0) < CACHE_VERSION) {
            try {
                OfflineMapManager.getTileCacheDir(context).deleteRecursively()
            } catch (_: Exception) {}
            prefs.edit().putInt("v", CACHE_VERSION).apply()
        }
    }

    private fun isCacheable(host: String, contentType: String?, bytes: ByteArray): Boolean =
        (host in CACHEABLE_HOSTS || host.contains("openstreetmap") || host.contains("arcgisonline.com")) &&
            bytes.size in 300..600_000 && (contentType == null || contentType.startsWith("image/"))

    private data class CachedTile(val bytes: ByteArray, val mime: String)
    private val memoryCache = object : android.util.LruCache<String, CachedTile>(12 * 1024 * 1024) {
        override fun sizeOf(key: String, value: CachedTile): Int = value.bytes.size
    }
    @Volatile private var lastPruneAt = 0L
    private val pruneRunning = java.util.concurrent.atomic.AtomicBoolean(false)
    private val legacyCartoPurged = java.util.concurrent.atomic.AtomicBoolean(false)

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
    private val SAT = Regex("^https://server\\.arcgisonline\\.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/(\\d+)/(\\d+)/(\\d+)$")
    private val SAT_CLARITY = Regex("^https://clarity\\.maptiles\\.arcgis\\.com/arcgis/rest/services/World_Imagery/MapServer/tile/(\\d+)/(\\d+)/(\\d+)$")
    private val VELA = Regex("^https://server\\.arcgisonline\\.com/ArcGIS/rest/services/World_Street_Map/MapServer/tile/(\\d+)/(\\d+)/(\\d+)$")
    private val ESRI_LABELS = Regex("^https://server\\.arcgisonline\\.com/ArcGIS/rest/services/Reference/World_Boundaries_and_Places/MapServer/tile/(\\d+)/(\\d+)/(\\d+)$")
    private val ESRI_TRANS = Regex("^https://server\\.arcgisonline\\.com/ArcGIS/rest/services/Reference/World_Transportation/MapServer/tile/(\\d+)/(\\d+)/(\\d+)$")
    private val EOX = Regex("^https://tiles\\.maps\\.eox\\.at/wmts/1\\.0\\.0/s2cloudless_3857/default/GoogleMapsCompatible/(\\d+)/(\\d+)/(\\d+)\\.jpg$")
    private val DEM_TERRARIUM = Regex("^https://s3\\.amazonaws\\.com/elevation-tiles-prod/terrarium/(\\d+)/(\\d+)/(\\d+)\\.png$")

    private data class Tile(val source: String, val z: Int, val x: Int, val y: Int, val ext: String, val mime: String) {
        val key: String get() = "$source/$z/$x/$y"
        val isSatelliteFamily: Boolean get() = source == "sat" || source == "sat_clarity"
        val isOverlay: Boolean get() = source in setOf("dem", "eox16", "esri_labels", "esri_trans")
    }

    private fun parse(url: String): Tile? {
        OSM.matchEntire(url)?.let { return Tile("osm", it.groupValues[1].toInt(), it.groupValues[2].toInt(), it.groupValues[3].toInt(), "png", "image/png") }
        OSM_DE.matchEntire(url)?.let { return Tile("osm_de", it.groupValues[1].toInt(), it.groupValues[2].toInt(), it.groupValues[3].toInt(), "png", "image/png") }
        HOT.matchEntire(url)?.let { return Tile("hot", it.groupValues[1].toInt(), it.groupValues[2].toInt(), it.groupValues[3].toInt(), "png", "image/png") }
        TOPO.matchEntire(url)?.let { return Tile("topo", it.groupValues[1].toInt(), it.groupValues[2].toInt(), it.groupValues[3].toInt(), "png", "image/png") }
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
            "https://server.arcgisonline.com/ArcGIS/rest/services/World_Topo_Map/MapServer/tile/${t.z}/${t.y}/${t.x}" to "image/jpeg"
        )
        "osm", "osm_de", "hot" -> listOf(
            "https://server.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer/tile/${t.z}/${t.y}/${t.x}" to "image/jpeg"
        )
        "sat" -> listOf(
            "https://clarity.maptiles.arcgis.com/arcgis/rest/services/World_Imagery/MapServer/tile/${t.z}/${t.y}/${t.x}" to "image/jpeg"
        )
        "sat_clarity" -> listOf(
            "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/${t.z}/${t.y}/${t.x}" to "image/jpeg"
        )
        "vela_esri" -> listOf(
            "https://tile.openstreetmap.org/${t.z}/${t.x}/${t.y}.png" to "image/png"
        )
        else -> emptyList()
    }

    private fun isValidImageBytes(bytes: ByteArray): Boolean {
        if (bytes.size < 64) return false
        val isPng = bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() && bytes[2] == 0x4E.toByte() && bytes[3] == 0x47.toByte()
        val isJpg = bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte()
        val isWebp = bytes.size >= 12 && bytes[0] == 'R'.code.toByte() && bytes[1] == 'I'.code.toByte() && bytes[2] == 'F'.code.toByte() && bytes[3] == 'F'.code.toByte()
        return isPng || isJpg || isWebp
    }

    private fun findSameFamilyCachedTile(context: Context, requested: Tile): Pair<ByteArray, String>? {
        if (requested.isOverlay) return null
        val fallbackOrder = if (requested.isSatelliteFamily) {
            listOf(
                "sat" to ("jpg" to "image/jpeg"),
                "sat_clarity" to ("jpg" to "image/jpeg")
            )
        } else {
            listOf(
                "osm" to ("png" to "image/png"),
                "vela_esri" to ("jpg" to "image/jpeg"),
                "esri_topo" to ("jpg" to "image/jpeg"),
                "hot" to ("png" to "image/png"),
                "topo" to ("png" to "image/png")
            )
        }

        // 1) نفس المستوى من نفس العائلة البصرية فقط (منع خلط صور الأقمار مع الشوارع)
        for ((src, extMime) in fallbackOrder) {
            if (src == requested.source) continue
            if (src == "osm") {
                readMbtile(context, requested.copy(source = "osm", ext = "png", mime = "image/png"))?.let {
                    if (isValidImageBytes(it)) return it to "image/png"
                }
            }
            val f = File(OfflineMapManager.getTileCacheDir(context), "$src/${requested.z}/${requested.x}/${requested.y}.${extMime.first}")
            if (f.exists() && f.length() > 0) {
                val b = f.readBytes()
                if (isValidImageBytes(b)) return b to extMime.second
            }
        }

        // 2) قص البلاطة الأم (z-1 إلى z-4) من نفس العائلة لمنع المربعات السوداء نهائياً
        val sourcesToCheck = listOf(requested.source to (requested.ext to requested.mime)) + fallbackOrder
        for (dz in 1..4) {
            if (requested.z - dz < 2) break
            val pz = requested.z - dz
            val scale = 1 shl dz
            val px = requested.x ushr dz
            val py = requested.y ushr dz
            val subX = requested.x and (scale - 1)
            val subY = requested.y and (scale - 1)
            for ((src, extMime) in sourcesToCheck) {
                val parentBytes = if (src == "osm") {
                    readMbtile(context, Tile("osm", pz, px, py, "png", "image/png"))
                        ?: File(OfflineMapManager.getTileCacheDir(context), "osm/$pz/$px/$py.png").takeIf { it.exists() && it.length() > 0 }?.readBytes()
                } else {
                    File(OfflineMapManager.getTileCacheDir(context), "$src/$pz/$px/$py.${extMime.first}").takeIf { it.exists() && it.length() > 0 }?.readBytes()
                }
                if (parentBytes != null && isValidImageBytes(parentBytes)) {
                    cropTileQuadrant(parentBytes, scale, subX, subY)?.let { cropped ->
                        return cropped to "image/jpeg"
                    }
                }
            }
        }
        return null
    }

    private fun cropTileQuadrant(parentBytes: ByteArray, scale: Int, subX: Int, subY: Int): ByteArray? {
        return try {
            val bmp = android.graphics.BitmapFactory.decodeByteArray(parentBytes, 0, parentBytes.size) ?: return null
            val qW = (bmp.width / scale).coerceAtLeast(1)
            val qH = (bmp.height / scale).coerceAtLeast(1)
            val left = (subX * qW).coerceIn(0, bmp.width - qW)
            val top = (subY * qH).coerceIn(0, bmp.height - qH)
            val sub = android.graphics.Bitmap.createBitmap(bmp, left, top, qW, qH)
            val scaled = if (qW != 256 || qH != 256) android.graphics.Bitmap.createScaledBitmap(sub, 256, 256, true) else sub
            val out = java.io.ByteArrayOutputStream()
            scaled.compress(android.graphics.Bitmap.CompressFormat.JPEG, 88, out)
            if (scaled !== sub) scaled.recycle()
            if (sub !== bmp) sub.recycle()
            bmp.recycle()
            out.toByteArray()
        } catch (_: Throwable) {
            null
        }
    }

    private fun purgeLegacyCartoCacheOnce(context: Context) {
        if (!legacyCartoPurged.compareAndSet(false, true)) return
        Thread {
            try {
                val dir = OfflineMapManager.getTileCacheDir(context)
                listOf("carto_voyager", "carto_labels").forEach { sub ->
                    val f = File(dir, sub)
                    if (f.exists()) f.deleteRecursively()
                }
            } catch (_: Throwable) {}
        }.apply { isDaemon = true; start() }
    }

    /** يعيد استجابة البلاطة من الذاكرة أو الكاش المحلي أو الشبكة (مع وضع أوفلاين صارم لـ MapLayer.VALHALLA_OFFLINE). */
    fun intercept(context: Context, url: String, offlineOnly: Boolean = false): WebResourceResponse? {
        migrateCacheIfNeeded(context)
        purgeLegacyCartoCacheOnce(context)
        if (url.contains("cartocdn.com", ignoreCase = true) || url.contains("carto.com/basemaps", ignoreCase = true)) {
            return WebResourceResponse("text/plain", "utf-8", 404, "Blocked", CORS_HEADERS, ByteArrayInputStream(ByteArray(0)))
        }
        val tile = parse(url) ?: return null

        memoryCache.get(tile.key)?.let { cached ->
            return tileResponse(cached.bytes, cached.mime)
        }

        if (tile.source == "osm") {
            readMbtile(context, tile)?.let { bytes ->
                if (isValidImageBytes(bytes)) {
                    memoryCache.put(tile.key, CachedTile(bytes, "image/png"))
                    return tileResponse(bytes, "image/png")
                }
            }
        }

        val cache = cacheFile(context, tile)
        val eco = com.example.utils.MapPowerDataPrefs.isEcoMode(context)
        if (cache.exists() && cache.length() > 0) {
            val age = System.currentTimeMillis() - cache.lastModified()
            if (offlineOnly || eco || age <= CACHE_TTL_MS) {
                val bytes = cache.readBytes()
                if (isValidImageBytes(bytes)) {
                    memoryCache.put(tile.key, CachedTile(bytes, tile.mime))
                    return tileResponse(bytes, tile.mime)
                } else {
                    cache.delete()
                }
            }
        }

        if (offlineOnly) {
            findSameFamilyCachedTile(context, tile)?.let { (bytes, mime) ->
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
            if (cache.exists() && cache.length() > 0) {
                val old = cache.readBytes()
                if (isValidImageBytes(old)) {
                    memoryCache.put(tile.key, CachedTile(old, tile.mime))
                    return tileResponse(old, tile.mime)
                }
            }
            findSameFamilyCachedTile(context, tile)?.let { (fallbackBytes, mime) ->
                memoryCache.put(tile.key, CachedTile(fallbackBytes, mime))
                return tileResponse(fallbackBytes, mime)
            }
            return WebResourceResponse("text/plain", "utf-8", 504, "Offline", CORS_HEADERS, ByteArrayInputStream(ByteArray(0)))
        }

        val (bytes, mime) = downloaded
        memoryCache.put(tile.key, CachedTile(bytes, mime))
        val host = try { Uri.parse(url).host.orEmpty() } catch (_: Throwable) { "" }
        if (mime == tile.mime && isCacheable(host, mime, bytes)) {
            try {
                writeAtomically(cache, bytes)
                maybePruneDiskCacheAsync(context)
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

    private fun maybePruneDiskCacheAsync(context: Context) {
        val now = System.currentTimeMillis()
        if (now - lastPruneAt < 15 * 60_000L) return
        if (!pruneRunning.compareAndSet(false, true)) return
        lastPruneAt = now
        val appCtx = context.applicationContext
        Thread {
            try {
                val dir = OfflineMapManager.getTileCacheDir(appCtx)
                val files = dir.walkTopDown().filter { it.isFile && !it.name.endsWith(".tmp") }.toList()
                var total = files.sumOf { it.length() }
                if (total > MAX_DISK_CACHE_BYTES) {
                    for (f in files.sortedBy { it.lastModified() }) {
                        if (total <= MAX_DISK_CACHE_BYTES * 8 / 10) break
                        val len = f.length()
                        if (f.delete()) total -= len
                    }
                }
            } catch (_: Throwable) {
            } finally {
                pruneRunning.set(false)
            }
        }.apply { isDaemon = true; start() }
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
                connectTimeout = 6_000; readTimeout = 9_000
                instanceFollowRedirects = true
                useCaches = true
                setRequestProperty("User-Agent", USER_AGENT)
            }
            if (c.responseCode != 200) return null
            val data = c.inputStream.use { it.readBytes() }
            if (data.isEmpty() || data.size > MAX_TILE_BYTES || !isValidImageBytes(data)) null else data
        } catch (_: Throwable) {
            null
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
