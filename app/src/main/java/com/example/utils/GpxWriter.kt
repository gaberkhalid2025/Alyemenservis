package com.example.utils

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class TrackPoint(
    val lat: Double,
    val lng: Double,
    val eleM: Double? = null,
    val timeMs: Long = 0L
)

/**
 * 🛰️ تصدير المسار أو الرحلة المسجلة بصيغة GPX 1.1 قياسية ومفتوحة (100% أوفلاين).
 */
object GpxWriter {
    fun track(name: String, pts: List<TrackPoint>): String = buildString {
        appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
        appendLine("""<gpx version="1.1" creator="AlYemenServices" xmlns="http://www.topografix.com/GPX/1/1">""")
        appendLine("  <trk>")
        appendLine("    <name>${escapeXml(name.ifBlank { "رحلة مسجلة" })}</name>")
        appendLine("    <trkseg>")
        for (p in pts) {
            append("""      <trkpt lat="${formatCoord(p.lat)}" lon="${formatCoord(p.lng)}">""")
            if (p.eleM != null && !p.eleM.isNaN()) {
                append("<ele>${String.format(Locale.US, "%.1f", p.eleM)}</ele>")
            }
            if (p.timeMs > 0L) {
                append("<time>${isoUtc(p.timeMs)}</time>")
            }
            appendLine("</trkpt>")
        }
        appendLine("    </trkseg>")
        appendLine("  </trk>")
        appendLine("</gpx>")
    }

    fun route(name: String, pts: List<Pair<Double, Double>>): String = buildString {
        appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
        appendLine("""<gpx version="1.1" creator="AlYemenServices" xmlns="http://www.topografix.com/GPX/1/1">""")
        appendLine("  <rte>")
        appendLine("    <name>${escapeXml(name.ifBlank { "مسار ملاحي" })}</name>")
        for (p in pts) {
            appendLine("""    <rtept lat="${formatCoord(p.first)}" lon="${formatCoord(p.second)}" />""")
        }
        appendLine("  </rte>")
        appendLine("</gpx>")
    }

    fun saveAndUri(context: Context, fileName: String, content: String): Uri? {
        return try {
            val dir = File(context.cacheDir, "exports").apply { mkdirs() }
            val safeName = fileName.replace(Regex("[^A-Za-z0-9._-]"), "_")
                .let { if (it.endsWith(".gpx", ignoreCase = true)) it else "$it.gpx" }
            val file = File(dir, safeName)
            file.writeText(content, Charsets.UTF_8)
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (_: Throwable) {
            null
        }
    }

    private fun formatCoord(v: Double): String = String.format(Locale.US, "%.6f", v)

    private fun isoUtc(ms: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date(ms))
    }

    private fun escapeXml(s: String): String = s
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")
}

/**
 * ⏺️ مسجل نقاط الرحلة الاختياري في الذاكرة (يتجاهل النقاط المتقاربة < 8م لتوفير الذاكرة).
 */
class TripRecorder(private val minSpacingMeters: Double = 8.0) {
    private val points = mutableListOf<TrackPoint>()

    val size: Int
        @Synchronized get() = points.size

    @Synchronized
    fun record(lat: Double, lng: Double, timeMs: Long = System.currentTimeMillis(), eleM: Double? = null) {
        if (!LandmarkAddress.isValid(lat, lng)) return
        val last = points.lastOrNull()
        if (last != null) {
            val d = HaversinePrecisionNavigator.calculateDistance(last.lat, last.lng, lat, lng)
            if (d < minSpacingMeters) return
        }
        points.add(TrackPoint(lat = lat, lng = lng, eleM = eleM, timeMs = timeMs))
    }

    @Synchronized
    fun snapshot(): List<TrackPoint> = points.toList()

    @Synchronized
    fun clear() {
        points.clear()
    }
}
