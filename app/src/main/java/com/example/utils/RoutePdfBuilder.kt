package com.example.utils

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.example.ui.screens.map.utils.MapDistanceCalculator
import java.io.File
import java.io.FileOutputStream
import java.text.DateFormat
import java.util.Date
import java.util.Locale

/**
 * 📄 مولّد خطة المسار الملاحية بصيغة PDF مجانية وأوفلاين بالكامل (مع رسم تخطيطي للمسار وتعليمات عربية RTL).
 */
object RoutePdfBuilder {
    private const val PAGE_W = 595
    private const val PAGE_H = 842
    private const val MARGIN = 36f

    fun buildAndUri(
        context: Context,
        title: String,
        coords: List<Pair<Double, Double>>,
        waypoints: List<NavWaypoint>,
        steps: List<NavStep>,
        totalDistanceMeters: Double,
        totalDurationSeconds: Double
    ): Uri? {
        val doc = PdfDocument()
        return try {
            val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#0F172A")
                textSize = 18f
                isFakeBoldText = true
            }
            val subPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#334155")
                textSize = 11.5f
            }
            val stepPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#0F172A")
                textSize = 11f
            }
            val mutedPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#64748B")
                textSize = 10f
            }

            var pageNum = 1
            var page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNum).create())
            var canvas = page.canvas
            var y = MARGIN

            val contentW = (PAGE_W - MARGIN * 2).toInt()

            y = drawRtlText(canvas, title.ifBlank { "خطة مسار — خدمات اليمن" }, titlePaint, MARGIN, y, contentW) + 6f
            val dateStr = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale("ar")).format(Date())
            val summary = "التاريخ: $dateStr  |  المسافة: ${MapDistanceCalculator.formatDistance(totalDistanceMeters)}  |  المدة التقديرية: ${MapDistanceCalculator.formatDuration(totalDurationSeconds)}  |  المحطات: ${waypoints.size}"
            y = drawRtlText(canvas, summary, subPaint, MARGIN, y, contentW) + 12f

            // رسم المسار التخطيطي (Schematic Vector Box)
            if (coords.size >= 2) {
                val boxRect = RectF(MARGIN, y, PAGE_W - MARGIN, y + 190f)
                drawSchematicMap(canvas, boxRect, coords, waypoints)
                y = boxRect.bottom + 14f
            }

            // جدول المحطات المختارة
            if (waypoints.isNotEmpty()) {
                y = drawRtlText(canvas, "المحطات المحددة:", titlePaint.apply { textSize = 13.5f }, MARGIN, y, contentW) + 4f
                waypoints.forEachIndexed { i, wp ->
                    val line = "${i + 1}. ${wp.name}  (${String.format(Locale.US, "%.5f, %.5f", wp.latitude, wp.longitude)})"
                    if (y > PAGE_H - MARGIN - 40f) {
                        doc.finishPage(page)
                        pageNum++
                        page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNum).create())
                        canvas = page.canvas
                        y = MARGIN
                    }
                    y = drawRtlText(canvas, line, subPaint, MARGIN, y, contentW) + 4f
                }
                y += 10f
            }

            // تعليمات المنعطفات (تُقسّم على صفحات إذا طالت)
            if (steps.isNotEmpty()) {
                if (y > PAGE_H - MARGIN - 60f) {
                    doc.finishPage(page)
                    pageNum++
                    page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNum).create())
                    canvas = page.canvas
                    y = MARGIN
                }
                y = drawRtlText(canvas, "تفاصيل خطوات التوجيه (${steps.size}):", titlePaint.apply { textSize = 13.5f }, MARGIN, y, contentW) + 6f
                steps.forEachIndexed { idx, step ->
                    val distLabel = MapDistanceCalculator.formatDistance(step.distanceMeters)
                    val line = "${idx + 1}. ${step.instruction}  ($distLabel)"
                    val needed = measureRtlHeight(line, stepPaint, contentW) + 8f
                    if (y + needed > PAGE_H - MARGIN) {
                        doc.finishPage(page)
                        pageNum++
                        page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNum).create())
                        canvas = page.canvas
                        y = MARGIN
                    }
                    y = drawRtlText(canvas, line, stepPaint, MARGIN, y, contentW) + 6f
                }
            } else {
                y = drawRtlText(canvas, "لا توجد منعطفات مفصلة — مسار مباشر بين المحطات.", mutedPaint, MARGIN, y, contentW)
            }

            doc.finishPage(page)

            val dir = File(context.cacheDir, "exports").apply { mkdirs() }
            val outFile = File(dir, "route_${System.currentTimeMillis()}.pdf")
            FileOutputStream(outFile).use { doc.writeTo(it) }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outFile)
        } catch (_: Throwable) {
            null
        } finally {
            try { doc.close() } catch (_: Throwable) {}
        }
    }

    private fun drawSchematicMap(
        canvas: android.graphics.Canvas,
        rect: RectF,
        coords: List<Pair<Double, Double>>,
        waypoints: List<NavWaypoint>
    ) {
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            style = Paint.Style.FILL
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#334155")
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }
        canvas.drawRoundRect(rect, 12f, 12f, bgPaint)
        canvas.drawRoundRect(rect, 12f, 12f, borderPaint)

        val minLat = coords.minOf { it.first }
        val maxLat = coords.maxOf { it.first }
        val minLng = coords.minOf { it.second }
        val maxLng = coords.maxOf { it.second }
        val spanLat = (maxLat - minLat).coerceAtLeast(0.0005)
        val spanLng = (maxLng - minLng).coerceAtLeast(0.0005)
        val pad = 20f
        val drawW = rect.width() - pad * 2
        val drawH = rect.height() - pad * 2

        fun toX(lng: Double) = (rect.left + pad + ((lng - minLng) / spanLng) * drawW).toFloat()
        fun toY(lat: Double) = (rect.bottom - pad - ((lat - minLat) / spanLat) * drawH).toFloat()

        val path = Path()
        coords.forEachIndexed { idx, (lat, lng) ->
            val px = toX(lng)
            val py = toY(lat)
            if (idx == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        val routePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#00E5FF")
            style = Paint.Style.STROKE
            strokeWidth = 3.5f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        canvas.drawPath(path, routePaint)

        val wpPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#F59E0B") }
        waypoints.dropLast(1).forEach { wp ->
            canvas.drawCircle(toX(wp.longitude), toY(wp.latitude), 4.5f, wpPaint)
        }

        val start = coords.first()
        val end = coords.last()
        canvas.drawCircle(
            toX(start.second),
            toY(start.first),
            6f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#10B981") }
        )
        canvas.drawCircle(
            toX(end.second),
            toY(end.first),
            6f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#EF4444") }
        )
    }

    private fun buildLayout(text: String, paint: TextPaint, width: Int): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setTextDirection(TextDirectionHeuristics.RTL)
            .setLineSpacing(2f, 1.1f)
            .build()

    private fun measureRtlHeight(text: String, paint: TextPaint, width: Int): Float =
        buildLayout(text, paint, width).height.toFloat()

    private fun drawRtlText(
        canvas: android.graphics.Canvas,
        text: String,
        paint: TextPaint,
        x: Float,
        y: Float,
        width: Int
    ): Float {
        val layout = buildLayout(text, paint, width)
        canvas.save()
        canvas.translate(x, y)
        layout.draw(canvas)
        canvas.restore()
        return y + layout.height
    }
}
