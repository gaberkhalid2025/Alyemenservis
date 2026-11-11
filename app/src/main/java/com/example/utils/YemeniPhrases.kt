package com.example.utils

import kotlin.math.roundToInt

object YemeniPhrases {
    private val HUNDREDS = mapOf(
        1 to "مية", 2 to "ميتين", 3 to "ثلاثمية", 4 to "أربعمية", 5 to "خمسمية",
        6 to "ستمية", 7 to "سبعمية", 8 to "ثمانمية", 9 to "تسعمية"
    )
    private val TENS = mapOf(
        3 to "ثلاثين", 4 to "أربعين", 5 to "خمسين", 6 to "ستين", 7 to "سبعين", 8 to "ثمانين", 9 to "تسعين"
    )

    /** مسافة منطوقة: <100 م لأقرب 10، حتى كيلو لأقرب 50، وفوقه بنصف كيلو. */
    fun distance(meters: Double): String {
        val m = if (meters.isNaN() || meters < 0) 0.0 else meters
        if (m < 30.0) return "قدّامك"
        if (m < 100.0) return "${TENS[(m / 10).roundToInt().coerceIn(3, 9)]} متر"
        if (m < 1000.0) {
            val r = (m / 50).roundToInt() * 50
            if (r >= 1000) return "كيلو"
            val base = HUNDREDS[r / 100] ?: return "$r متر"
            return if (r % 100 == 0) "$base متر" else "$base وخمسين متر"
        }
        val half = (m / 500.0).roundToInt() / 2.0
        val whole = half.toInt()
        val head = when (whole) { 0, 1 -> "كيلو"; 2 -> "كيلوين"; else -> "$whole كيلو" }
        return if (half - whole >= 0.5 && whole >= 1) "$head ونص" else head
    }

    fun maneuver(m: ManeuverType, street: String?): String {
        val st = street?.takeIf { it.isNotBlank() && it != "unnamed" }?.let { " على $it" } ?: ""
        return when (m) {
            ManeuverType.LEFT -> "لف يسار$st"
            ManeuverType.RIGHT -> "لف يمين$st"
            ManeuverType.SLIGHT_LEFT -> "خذ يسار شوية$st"
            ManeuverType.SLIGHT_RIGHT -> "خذ يمين شوية$st"
            ManeuverType.SHARP_LEFT -> "لف يسار بقوة$st"
            ManeuverType.SHARP_RIGHT -> "لف يمين بقوة$st"
            ManeuverType.UTURN -> "ارجع دوّر"
            ManeuverType.ROUNDABOUT -> "عند الجولة كمّل"
            ManeuverType.STRAIGHT -> "واصل على طول$st"
            ManeuverType.DESTINATION -> "وصلت"
            else -> "واصل على طول$st"
        }
    }

    fun upcoming(m: ManeuverType, street: String?, meters: Double): String =
        if (m == ManeuverType.DESTINATION) "بعد ${distance(meters)} توصل" else "بعد ${distance(meters)}، ${maneuver(m, street)}"
}
