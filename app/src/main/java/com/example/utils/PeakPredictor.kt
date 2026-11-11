package com.example.utils

import com.example.ui.screens.map.MapZone
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.floor

data class BookingStamp(val lat: Double, val lng: Double, val epochMs: Long)

enum class PeakLevel(val colorHex: String) {
    LOW("#10B981"),
    MEDIUM("#F59E0B"),
    HIGH("#EF4444"),
    UNKNOWN("#64748B")
}

data class PeakZone(val lat: Double, val lng: Double, val level: PeakLevel, val sample: Int)

object PeakPredictor {
    private const val CELL = 0.02
    const val MIN_SAMPLE = 20
    private const val MIN_SAME_DAY = 6
    const val WINDOW_DAYS = 56

    fun predict(history: List<BookingStamp>, nowMs: Long, tz: TimeZone = TimeZone.getTimeZone("Asia/Aden")): List<PeakZone> {
        val since = nowMs - WINDOW_DAYS * 86_400_000L
        val cal = Calendar.getInstance(tz).apply { timeInMillis = nowMs }
        val dow = cal.get(Calendar.DAY_OF_WEEK)
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val cells = HashMap<Pair<Long, Long>, MutableList<BookingStamp>>()
        history.asSequence()
            .filter { it.epochMs in since..nowMs && LandmarkAddress.isValid(it.lat, it.lng) }
            .forEach {
                cells.getOrPut(floor(it.lat / CELL).toLong() to floor(it.lng / CELL).toLong()) { mutableListOf() }.add(it)
            }

        val c = Calendar.getInstance(tz)
        return cells.map { (key, list) ->
            val lat = (key.first + 0.5) * CELL
            val lng = (key.second + 0.5) * CELL
            if (list.size < MIN_SAMPLE) return@map PeakZone(lat, lng, PeakLevel.UNKNOWN, list.size)
            val counts = IntArray(24)
            list.forEach {
                c.timeInMillis = it.epochMs
                if (c.get(Calendar.DAY_OF_WEEK) == dow) counts[c.get(Calendar.HOUR_OF_DAY)]++
            }
            val total = counts.sum()
            if (total < MIN_SAME_DAY) return@map PeakZone(lat, lng, PeakLevel.UNKNOWN, list.size)
            val mean = total / 24.0
            val cur = (counts[(hour + 23) % 24] + counts[hour] + counts[(hour + 1) % 24]) / 3.0
            val ratio = cur / mean
            PeakZone(
                lat, lng,
                when {
                    ratio >= 1.5 -> PeakLevel.HIGH
                    ratio <= 0.6 -> PeakLevel.LOW
                    else -> PeakLevel.MEDIUM
                },
                list.size
            )
        }
    }

    /** للعرض بطبقة المناطق (المستند 61: MapZone). */
    fun toMapZones(zones: List<PeakZone>): List<MapZone> = zones.map { MapZone(it.lat, it.lng, 1100, it.level.colorHex) }
}
