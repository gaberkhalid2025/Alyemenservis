package com.example.utils

/**
 * 🗣️ ArabicInstructionFormatter
 * محول أنواع المناورات والمسافات إلى جمل عربية احترافية للملاحة.
 */
object ArabicInstructionFormatter {

    fun format(
        maneuver: ManeuverType,
        streetName: String?,
        distanceMeters: Double,
        destinationName: String?
    ): String {
        val street = if (!streetName.isNullOrBlank() && streetName != "unnamed") " في $streetName" else ""
        val distText = formatDistance(distanceMeters)

        return when (maneuver) {
            ManeuverType.START -> "انطلق باتجاه $destinationName$street"
            ManeuverType.STRAIGHT -> "واصل السير مباشرة لمسافة $distText$street"
            ManeuverType.LEFT -> "انعطف يساراً بعد $distText$street"
            ManeuverType.RIGHT -> "انعطف يميناً بعد $distText$street"
            ManeuverType.SHARP_LEFT -> "انعطف يساراً بشكل حاد بعد $distText$street"
            ManeuverType.SHARP_RIGHT -> "انعطف يميناً بشكل حاد بعد $distText$street"
            ManeuverType.SLIGHT_LEFT -> "انحرف يساراً قليلاً بعد $distText$street"
            ManeuverType.SLIGHT_RIGHT -> "انحرف يميناً قليلاً بعد $distText$street"
            ManeuverType.UTURN -> "قم بالدوران للخلف (U-turn) بعد $distText"
            ManeuverType.ROUNDABOUT -> "ادخل الدوار لمسافة $distText ثم اسلك المخرج"
            ManeuverType.HIGHWAY_ENTER -> "اسلك المسار السريع بعد $distText$street"
            ManeuverType.HIGHWAY_EXIT -> "اخرج من المسار السريع بعد $distText$street"
            ManeuverType.DESTINATION -> "وصلت إلى وجهتك: $destinationName"
            ManeuverType.MERGE -> "اندمج مع حركة السير$street"
        }
    }

    private fun formatDistance(meters: Double): String {
        return when {
            meters < 50 -> "أقل من 50 متر"
            meters < 100 -> "100 متر"
            meters < 1000 -> "${(meters / 50).toInt() * 50} متر"
            else -> "%.1f كيلومتر".format(meters / 1000.0)
        }
    }
}
