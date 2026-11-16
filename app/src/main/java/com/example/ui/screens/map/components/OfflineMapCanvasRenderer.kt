package com.example.ui.screens.map.components

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.example.data.PropertyEntity
import com.example.data.ProviderEntity
import com.example.data.StoreEntity
import com.example.utils.NavRouteResult
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

// [إصلاح 21] كائنات Paint ثابتة على مستوى الملف لإلغاء أي تخصيصات أثناء رسم الإطارات (60 FPS)
private val sRoadTextPaint = android.graphics.Paint().apply {
    color = android.graphics.Color.WHITE
    textAlign = android.graphics.Paint.Align.CENTER
    isFakeBoldText = true
    isAntiAlias = true
    setShadowLayer(5f, 1f, 1f, android.graphics.Color.BLACK)
}
private val sRoundPaint = android.graphics.Paint().apply {
    color = android.graphics.Color.WHITE
    textAlign = android.graphics.Paint.Align.CENTER
    isFakeBoldText = true
    isAntiAlias = true
    setShadowLayer(5f, 1f, 1f, android.graphics.Color.BLACK)
}
private val sDistPaint = android.graphics.Paint().apply {
    color = android.graphics.Color.WHITE
    textAlign = android.graphics.Paint.Align.CENTER
    isFakeBoldText = true
    isAntiAlias = true
    setShadowLayer(6f, 1f, 1f, android.graphics.Color.BLACK)
}
private val sPillPaint = android.graphics.Paint().apply {
    color = android.graphics.Color.parseColor("#B30F172A")
    isAntiAlias = true
    style = android.graphics.Paint.Style.FILL
}
private val sBorderPaint = android.graphics.Paint().apply {
    color = android.graphics.Color.parseColor("#475569")
    isAntiAlias = true
    style = android.graphics.Paint.Style.STROKE
    strokeWidth = 1.2f
}

internal class OfflineMapPaintCache {
    val emojiPaint = android.graphics.Paint().apply {
        textAlign = android.graphics.Paint.Align.CENTER
        isAntiAlias = true
    }
    val titlePaint = android.graphics.Paint().apply {
        color = android.graphics.Color.WHITE
        textAlign = android.graphics.Paint.Align.CENTER
        isFakeBoldText = true
        isAntiAlias = true
        setShadowLayer(6f, 2f, 2f, android.graphics.Color.parseColor("#E6000000"))
    }
    val subtitlePaint = android.graphics.Paint().apply {
        color = android.graphics.Color.parseColor("#38BDF8")
        textAlign = android.graphics.Paint.Align.CENTER
        isFakeBoldText = true
        isAntiAlias = true
        setShadowLayer(4f, 1f, 1f, android.graphics.Color.parseColor("#E6000000"))
    }
    val labelBgPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.parseColor("#CC0F172A")
        isAntiAlias = true
        style = android.graphics.Paint.Style.FILL
    }
    val uTitlePaint = android.graphics.Paint().apply {
        textSize = 21f
        color = android.graphics.Color.WHITE
        textAlign = android.graphics.Paint.Align.CENTER
        isFakeBoldText = true
        isAntiAlias = true
        setShadowLayer(4f, 1f, 1f, android.graphics.Color.BLACK)
    }
    val uSubPaint = android.graphics.Paint().apply {
        textSize = 15f
        color = android.graphics.Color.parseColor("#38BDF8")
        textAlign = android.graphics.Paint.Align.CENTER
        isFakeBoldText = true
        isAntiAlias = true
    }
    val uBgPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.parseColor("#E60F172A")
        style = android.graphics.Paint.Style.FILL
        isAntiAlias = true
    }
    val uBorderPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.parseColor("#00E5FF")
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = 1.8f
        isAntiAlias = true
    }
}

/**
 * 🎨 OfflineMapCanvasRenderer
 * دوال رسم شبكة الشوارع، الحدائق، المسار الملاحي، حلقات الرادار التكتيكية، الدبابيس، وموقع المستخدم النابض
 */
internal fun DrawScope.renderOfflineMapScene(
    safeCX: Float,
    safeCY: Float,
    safeMeters: Float,
    safePan: Offset,
    safeZoom: Float,
    selectedCity: String,
    originLat: Double,
    originLng: Double,
    safeUserLat: Double,
    safeUserLng: Double,
    hasUserLocation: Boolean,
    metersPerDegreeLat: Double,
    metersPerDegreeLng: Double,
    route: NavRouteResult?,
    screenPoints: List<InteractiveMapPoint>,
    selectedEntity: Any?,
    pulseProgress: Float,
    paints: OfflineMapPaintCache
) {
    // 1. Draw city roads, avenues, ring-roads and parks
    drawCityRoadGrid(
        centerX = safeCX,
        centerY = safeCY,
        metersPerPx = safeMeters,
        widthPx = size.width,
        heightPx = size.height,
        panOffset = safePan,
        zoomScale = safeZoom,
        selectedCity = selectedCity,
        originLat = originLat,
        originLng = originLng,
        metersPerDegreeLat = metersPerDegreeLat,
        metersPerDegreeLng = metersPerDegreeLng
    )

    // 1.5. Draw Navigation Route Polyline
    drawNavigationRoute(
        route = route,
        safeCX = safeCX,
        safeCY = safeCY,
        safeMeters = safeMeters,
        safeZoom = safeZoom,
        originLat = originLat,
        originLng = originLng,
        metersPerDegreeLat = metersPerDegreeLat,
        metersPerDegreeLng = metersPerDegreeLng
    )

    // 2. User GPS coordinates projection
    val userXMeters = ((safeUserLng - originLng) * metersPerDegreeLng).toFloat()
    val userYMeters = (-(safeUserLat - originLat) * metersPerDegreeLat).toFloat()
    val userScreenX = safeCX + (userXMeters / safeMeters)
    val userScreenY = safeCY + (userYMeters / safeMeters)
    val userPin = Offset(userScreenX, userScreenY)

    val visiblePadding = 90f
    val minX = -visiblePadding
    val maxX = size.width + visiblePadding
    val minY = -visiblePadding
    val maxY = size.height + visiblePadding

    // 3. Proximity distance rings around user GPS (Tactical Radar overlay)
    if (userPin.x in minX..maxX && userPin.y in minY..maxY) {
        val ring500Px = 500f / safeMeters
        val ring1000Px = 1000f / safeMeters
        val ring2000Px = 2000f / safeMeters

        val dashRingStroke = Stroke(
            width = 1.2f,
            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
        )

        if (ring500Px in 35f..size.width) {
            drawCircle(Color(0xFF00E5FF).copy(alpha = 0.15f), ring500Px, userPin, style = dashRingStroke)
        }
        if (ring1000Px in 35f..size.width) {
            drawCircle(Color(0xFF00E5FF).copy(alpha = 0.10f), ring1000Px, userPin, style = dashRingStroke)
        }
        if (ring2000Px in 35f..size.width) {
            drawCircle(Color(0xFF00E5FF).copy(alpha = 0.06f), ring2000Px, userPin, style = dashRingStroke)
        }
    }

    // 4. Dynamic paints scaled with zoom for sharp crisp rendering
    val fontScale = (safeZoom.coerceIn(0.6f, 2.5f)).pow(0.35f)
    val emojiSize = (28f * fontScale).coerceIn(20f, 38f)
    val titleLabelSize = (18f * fontScale).coerceIn(13f, 25f)
    val subLabelSize = (14f * fontScale).coerceIn(11f, 19f)

    paints.emojiPaint.textSize = emojiSize
    paints.titlePaint.textSize = titleLabelSize
    paints.subtitlePaint.textSize = subLabelSize

    // 5. Render service & entity markers with ground needles, shadows and distance tags
    screenPoints.forEach { pt ->
        val groundPoint = Offset(pt.screenX, pt.screenY)
        if (groundPoint.x in minX..maxX && groundPoint.y in minY..maxY) {
            val isSelected = when (val ent = selectedEntity) {
                is ProviderEntity -> ent.id == pt.id
                is StoreEntity -> ent.id == pt.id
                is PropertyEntity -> ent.id == pt.id
                else -> false
            }

            val baseRadius = 15f * fontScale.coerceIn(0.85f, 1.4f)
            val pinHeadCenter = Offset(groundPoint.x, groundPoint.y - baseRadius - 5f)

            // Ground contact shadow
            drawOval(
                color = Color.Black.copy(alpha = 0.40f),
                topLeft = Offset(groundPoint.x - baseRadius * 0.75f, groundPoint.y - 3f),
                size = Size(baseRadius * 1.5f, 6f)
            )

            // Ground pointer needle
            val needlePath = Path().apply {
                moveTo(groundPoint.x, groundPoint.y)
                lineTo(groundPoint.x - baseRadius * 0.55f, pinHeadCenter.y + baseRadius * 0.5f)
                lineTo(groundPoint.x + baseRadius * 0.55f, pinHeadCenter.y + baseRadius * 0.5f)
                close()
            }
            drawPath(needlePath, color = if (isSelected) Color.White else pt.color)

            // Outer glowing aura / halo
            val pointAlpha = if (pt.isApprox) 0.55f else 1.0f
            if (isSelected) {
                drawCircle(pt.color.copy(alpha = 0.30f * pointAlpha), baseRadius * 2.8f, pinHeadCenter)
                drawCircle(pt.color.copy(alpha = 0.65f * pointAlpha), baseRadius * 1.9f, pinHeadCenter)
                drawCircle(Color.White.copy(alpha = pointAlpha), baseRadius * 1.35f, pinHeadCenter, style = Stroke(4f))
            } else {
                drawCircle(pt.color.copy(alpha = 0.35f * pointAlpha), baseRadius * 1.7f, pinHeadCenter)
            }

            // Badge core
            drawCircle(Color(0xFF0F172A).copy(alpha = pointAlpha), baseRadius, pinHeadCenter)
            drawCircle(
                color = if (isSelected) Color.White.copy(alpha = pointAlpha) else pt.color.copy(alpha = pointAlpha),
                radius = baseRadius,
                center = pinHeadCenter,
                style = Stroke(if (isSelected) 3.5f else 2.5f)
            )

            // Emoji icon inside badge
            drawContext.canvas.nativeCanvas.drawText(
                pt.emoji,
                pinHeadCenter.x,
                pinHeadCenter.y + (emojiSize * 0.35f),
                paints.emojiPaint
            )

            // Calculate distance from user GPS
            val distMeters = sqrt((pt.xMeters - userXMeters).pow(2) + (pt.yMeters - userYMeters).pow(2))
            val distString = if (distMeters < 1000f) {
                "${distMeters.roundToInt()} م"
            } else {
                "${String.format(java.util.Locale.US, "%.1f", distMeters / 1000f)} كم"
            }

            // Legible multi-attribute label with dark pill background
            if (safeZoom >= 0.50f) {
                val baseName = if (pt.name.length > 20) pt.name.take(19) + "…" else pt.name
                val displayName = if (pt.isApprox) "$baseName (موقع تقريبي)" else baseName
                val titleWidth = paints.titlePaint.measureText(displayName)
                val subText = "${pt.category} • $distString"
                val subWidth = paints.subtitlePaint.measureText(subText)
                val maxTextWidth = max(titleWidth, subWidth)

                val labelTop = pinHeadCenter.y + baseRadius + 4f
                val labelBottom = labelTop + titleLabelSize + subLabelSize + 8f

                val bgRect = android.graphics.RectF(
                    pinHeadCenter.x - (maxTextWidth / 2f) - 10f,
                    labelTop - 2f,
                    pinHeadCenter.x + (maxTextWidth / 2f) + 10f,
                    labelBottom
                )

                drawContext.canvas.nativeCanvas.drawRoundRect(
                    bgRect,
                    12f,
                    12f,
                    paints.labelBgPaint
                )

                // Title line
                drawContext.canvas.nativeCanvas.drawText(
                    displayName,
                    pinHeadCenter.x,
                    labelTop + titleLabelSize - 1f,
                    paints.titlePaint
                )

                // Subtitle line (category & distance)
                drawContext.canvas.nativeCanvas.drawText(
                    subText,
                    pinHeadCenter.x,
                    labelTop + titleLabelSize + subLabelSize + 3f,
                    paints.subtitlePaint
                )
            }
        }
    }

    // 6. User GPS location with pulsating neon cyan animation & halo
    if (hasUserLocation && userPin.x in minX..maxX && userPin.y in minY..maxY) {
        val pulseRadiusMax = 44f
        val currentPulse = pulseProgress * pulseRadiusMax
        val pulseAlpha = (1f - pulseProgress).coerceIn(0f, 1f) * 0.55f

        drawCircle(
            color = Color(0xFF00E5FF).copy(alpha = pulseAlpha),
            radius = 14f + currentPulse,
            center = userPin
        )
        drawCircle(
            color = Color(0xFF00E5FF).copy(alpha = 0.35f),
            radius = 20f,
            center = userPin
        )
        drawCircle(
            color = Color(0xFF0F172A),
            radius = 13f,
            center = userPin
        )
        drawCircle(
            color = Color(0xFF00E5FF),
            radius = 11f,
            center = userPin
        )
        drawCircle(
            color = Color.White,
            radius = 5f,
            center = userPin
        )

        // User location title badge
        val uLabel = "موقعك المباشر 📍"
        val uSubLabel = if (selectedCity != "الكل") "محافظة $selectedCity" else if (hasUserLocation) "نظام تحديد المواقع GPS نشط" else ""
        val uW = paints.uTitlePaint.measureText(uLabel).coerceAtLeast(paints.uSubPaint.measureText(uSubLabel))
        val uRect = android.graphics.RectF(
            userPin.x - (uW / 2f) - 14f,
            userPin.y - 68f,
            userPin.x + (uW / 2f) + 14f,
            userPin.y - 18f
        )
        drawContext.canvas.nativeCanvas.drawRoundRect(uRect, 10f, 10f, paints.uBgPaint)
        drawContext.canvas.nativeCanvas.drawRoundRect(uRect, 10f, 10f, paints.uBorderPaint)
        drawContext.canvas.nativeCanvas.drawText(uLabel, userPin.x, userPin.y - 42f, paints.uTitlePaint)
        drawContext.canvas.nativeCanvas.drawText(uSubLabel, userPin.x, userPin.y - 24f, paints.uSubPaint)
    }
}

private fun DrawScope.drawNavigationRoute(
    route: NavRouteResult?,
    safeCX: Float,
    safeCY: Float,
    safeMeters: Float,
    safeZoom: Float,
    originLat: Double,
    originLng: Double,
    metersPerDegreeLat: Double,
    metersPerDegreeLng: Double
) {
    route?.let { res ->
        if (res.coordinates.size >= 2) {
            val routePath = Path()
            val startCoords = res.coordinates[0]
            val startX = safeCX + (((startCoords.second - originLng) * metersPerDegreeLng).toFloat() / safeMeters)
            val startY = safeCY + (((-(startCoords.first - originLat) * metersPerDegreeLat)).toFloat() / safeMeters)
            routePath.moveTo(startX, startY)

            for (i in 1 until res.coordinates.size) {
                val pt = res.coordinates[i]
                val px = safeCX + (((pt.second - originLng) * metersPerDegreeLng).toFloat() / safeMeters)
                val py = safeCY + (((-(pt.first - originLat) * metersPerDegreeLat)).toFloat() / safeMeters)
                routePath.lineTo(px, py)
            }

            // Outer glow for the route
            drawPath(
                path = routePath,
                color = Color(0xFF00E5FF).copy(alpha = 0.35f),
                style = Stroke(width = 14.dp.toPx() / safeZoom, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            // Second layer glow
            drawPath(
                path = routePath,
                color = Color(0xFF00E5FF).copy(alpha = 0.20f),
                style = Stroke(width = 24.dp.toPx() / safeZoom, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            // Main neon route line
            drawPath(
                path = routePath,
                color = Color(0xFF00E5FF),
                style = Stroke(width = 7.dp.toPx() / safeZoom, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Draw stop markers along the route with premium styling
            res.waypoints.forEachIndexed { index, wp ->
                val wx = safeCX + (((wp.longitude - originLng) * metersPerDegreeLng).toFloat() / safeMeters)
                val wy = safeCY + (((-(wp.latitude - originLat) * metersPerDegreeLat)).toFloat() / safeMeters)
                val wpCenter = Offset(wx, wy)

                if (index == 0) {
                    drawCircle(Color(0xFF10B981).copy(alpha = 0.3f), 18.dp.toPx() / safeZoom, wpCenter)
                    drawCircle(Color(0xFF10B981), 10.dp.toPx() / safeZoom, wpCenter)
                    drawCircle(Color.White, 4.dp.toPx() / safeZoom, wpCenter)
                } else if (index == res.waypoints.size - 1) {
                    drawCircle(Color(0xFFEF4444).copy(alpha = 0.3f), 18.dp.toPx() / safeZoom, wpCenter)
                    drawCircle(Color(0xFFEF4444), 10.dp.toPx() / safeZoom, wpCenter)
                    drawCircle(Color.White, 4.dp.toPx() / safeZoom, wpCenter)
                } else {
                    drawCircle(Color(0xFF0F172A), 10.dp.toPx() / safeZoom, wpCenter)
                    drawCircle(Color(0xFF00E5FF), 8.dp.toPx() / safeZoom, wpCenter, style = Stroke(2.5.dp.toPx()))
                    drawCircle(Color.White, 3.dp.toPx() / safeZoom, wpCenter)
                }
            }
        }
    }
}

/**
 * Draws structured city roads, ring-roads, blocks and parks in high-contrast neon cartographic style
 */
internal fun DrawScope.drawCityRoadGrid(
    centerX: Float,
    centerY: Float,
    metersPerPx: Float,
    widthPx: Float,
    heightPx: Float,
    panOffset: Offset,
    zoomScale: Float,
    selectedCity: String,
    originLat: Double,
    originLng: Double,
    metersPerDegreeLat: Double,
    metersPerDegreeLng: Double
) {
    fun geoToScreen(lat: Double, lng: Double): Offset {
        val xm = ((lng - originLng) * metersPerDegreeLng).toFloat()
        val ym = (-(lat - originLat) * metersPerDegreeLat).toFloat()
        return Offset(centerX + (xm / metersPerPx), centerY + (ym / metersPerPx))
    }

    // 1. Urban Cartographic Background Blocks
    val gridSize = (140f * zoomScale.coerceIn(0.5f, 2.5f))
    val startX = (panOffset.x % gridSize) - gridSize
    val startY = (panOffset.y % gridSize) - gridSize

    val blockColor = Color(0xFF131D31)
    val blockBorderColor = Color(0xFF1E2C44)

    var currentY = startY
    while (currentY < heightPx + gridSize) {
        var currentX = startX
        while (currentX < widthPx + gridSize) {
            drawRoundRect(
                color = blockColor,
                topLeft = Offset(currentX + 8f, currentY + 8f),
                size = Size(gridSize - 16f, gridSize - 16f),
                cornerRadius = CornerRadius(8f, 8f)
            )
            drawRoundRect(
                color = blockBorderColor,
                topLeft = Offset(currentX + 8f, currentY + 8f),
                size = Size(gridSize - 16f, gridSize - 16f),
                cornerRadius = CornerRadius(8f, 8f),
                style = Stroke(width = 1f)
            )
            currentX += gridSize
        }
        currentY += gridSize
    }

    // 2. Real Public Parks and Waterfronts
    getCityParks(selectedCity).forEach { p ->
        val centerPt = geoToScreen(p.lat, p.lng)
        val wPx = p.wM / metersPerPx
        val hPx = p.hM / metersPerPx
        val rectTopLeft = Offset(centerPt.x - wPx / 2f, centerPt.y - hPx / 2f)
        if (rectTopLeft.x < widthPx + 200f && rectTopLeft.x + wPx > -200f &&
            rectTopLeft.y < heightPx + 200f && rectTopLeft.y + hPx > -200f
        ) {
            drawRoundRect(
                color = Color(0xFF10B981).copy(alpha = 0.40f),
                topLeft = rectTopLeft,
                size = Size(wPx, hPx),
                cornerRadius = CornerRadius(14f, 14f)
            )
            drawRoundRect(
                color = Color(0xFF059669).copy(alpha = 0.70f),
                topLeft = rectTopLeft,
                size = Size(wPx, hPx),
                cornerRadius = CornerRadius(14f, 14f),
                style = Stroke(width = 1.5f)
            )
        }
    }

    // 3. Real Geodesic Roads
    val highwayOuterColor = Color(0xFFF59E0B)
    val highwayInnerColor = Color(0xFFFEF08A)
    val streetOuterColor = Color(0xFF0284C7)
    val streetInnerColor = Color(0xFFE0F2FE)

    val roadTextPaint = sRoadTextPaint.apply {
        textSize = (20f * zoomScale.coerceIn(0.7f, 1.6f)).coerceIn(16f, 28f)
    }

    getCityRoads(selectedCity).forEach { r ->
        val shouldDrawSegment = if (r.isHighway) true else zoomScale >= 0.55f
        if (shouldDrawSegment) {
            val pts = r.points.map { geoToScreen(it.first, it.second) }
            val zoomFactor = zoomScale.coerceIn(0.4f, 4.0f)
            val outerW = if (r.isHighway) (8.0f * zoomFactor).coerceIn(4f, 24f) else (5.0f * zoomFactor).coerceIn(2.5f, 16f)
            val innerW = if (r.isHighway) (outerW * 0.55f) else (outerW * 0.45f)
            val oCol = if (r.isHighway) highwayOuterColor else streetOuterColor
            val iCol = if (r.isHighway) highwayInnerColor else streetInnerColor

            for (i in 0 until pts.size - 1) {
                val p1 = pts[i]
                val p2 = pts[i + 1]
                drawLine(color = oCol, start = p1, end = p2, strokeWidth = outerW)
                drawLine(color = iCol, start = p1, end = p2, strokeWidth = innerW)
            }

            val shouldShowLabel = if (r.isHighway) zoomScale >= 0.45f else zoomScale >= 0.85f
            if (shouldShowLabel && pts.size >= 2) {
                val midIdx = (pts.size - 1) / 2
                val midX = (pts[midIdx].x + pts[midIdx + 1].x) / 2f
                val midY = (pts[midIdx].y + pts[midIdx + 1].y) / 2f
                if (midX in -80f..(widthPx + 80f) && midY in -80f..(heightPx + 80f)) {
                    drawContext.canvas.nativeCanvas.drawText(r.name + " 🛣️", midX, midY - 8f, roadTextPaint)
                }
            }
        }
    }

    // 4. Real Roundabouts & Landmarks
    val roundPaint = sRoundPaint.apply {
        textSize = (20f * zoomScale.coerceIn(0.7f, 1.5f)).coerceIn(15f, 26f)
    }

    getCityRoundabouts(selectedCity).forEach { rb ->
        val shouldShowRb = rb.isHighlight || zoomScale >= 0.75f
        if (shouldShowRb) {
            val pt = geoToScreen(rb.lat, rb.lng)
            if (pt.x in -120f..(widthPx + 120f) && pt.y in -120f..(heightPx + 120f)) {
                val ringColor = if (rb.isHighlight) Color(0xFF00E5FF) else Color(0xFFFBBF24)
                drawCircle(color = ringColor.copy(alpha = 0.35f), radius = 16f, center = pt)
                drawCircle(color = ringColor, radius = 9f, center = pt)
                drawCircle(color = Color(0xFF0F172A), radius = 5f, center = pt)
                drawContext.canvas.nativeCanvas.drawText(rb.name, pt.x, pt.y + 24f, roundPaint)
            }
        }
    }

    // 5. Real Neighborhood Badges
    val distPaint = sDistPaint.apply {
        textSize = (22f * zoomScale.coerceIn(0.7f, 1.5f)).coerceIn(16f, 28f)
    }

    getCityDistricts(selectedCity).forEach { d ->
        val pt = geoToScreen(d.lat, d.lng)
        if (pt.x in -120f..(widthPx + 120f) && pt.y in -120f..(heightPx + 120f)) {
            val label = "${d.emoji} ${d.name}"
            val textW = distPaint.measureText(label)
            val bgRect = android.graphics.RectF(pt.x - textW / 2f - 10f, pt.y - 20f, pt.x + textW / 2f + 10f, pt.y + 8f)
            drawContext.canvas.nativeCanvas.drawRoundRect(bgRect, 8f, 8f, sPillPaint)
            drawContext.canvas.nativeCanvas.drawRoundRect(bgRect, 8f, 8f, sBorderPaint)
            drawContext.canvas.nativeCanvas.drawText(label, pt.x, pt.y, distPaint)
        }
    }
}
