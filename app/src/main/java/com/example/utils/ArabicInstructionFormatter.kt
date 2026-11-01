package com.example.utils

import java.util.Locale
import kotlin.math.roundToInt

/**
 * محوّل المناورات إلى جمل عربية.
 * قاعدة: نص المنعطف لا يحمل مسافة (المسافة إلى المنعطف تأتي من التتبع الحي).
 * المسافة تُذكر فقط في «واصل السير» لأن معناها طول المقطع التالي.
 */
object ArabicInstructionFormatter {

    fun format(
        maneuver: ManeuverType,
        streetName: String?,
        distanceMeters: Double,
        destinationName: String?,
        roundaboutExit: Int? = null,
        isIntermediateStop: Boolean = false
    ): String {
        val street = if (!streetName.isNullOrBlank() && streetName != "unnamed") " في $streetName" else ""
        val dest = destinationName?.takeIf { it.isNotBlank() } ?: "الوجهة"

        return when (maneuver) {
            ManeuverType.START -> "انطلق باتجاه $dest$street"
            ManeuverType.STRAIGHT -> "واصل السير مباشرة لمسافة ${spoken(distanceMeters)}$street"
            ManeuverType.LEFT -> "انعطف يساراً$street"
            ManeuverType.RIGHT -> "انعطف يميناً$street"
            ManeuverType.SHARP_LEFT -> "انعطف يساراً بشكل حاد$street"
            ManeuverType.SHARP_RIGHT -> "انعطف يميناً بشكل حاد$street"
            ManeuverType.SLIGHT_LEFT -> "انحرف يساراً قليلاً$street"
            ManeuverType.SLIGHT_RIGHT -> "انحرف يميناً قليلاً$street"
            ManeuverType.UTURN -> "قم بالدوران للخلف"
            ManeuverType.ROUNDABOUT -> {
                val exitTxt = roundaboutExit?.let { ordinal(it) }
                if (exitTxt != null) "ادخل الدوار واسلك المخرج $exitTxt$street"
                else "ادخل الدوار ثم اسلك المخرج المناسب"
            }
            ManeuverType.HIGHWAY_ENTER -> "اسلك المسار السريع$street"
            ManeuverType.HIGHWAY_EXIT -> "اخرج من المسار السريع$street"
            ManeuverType.DESTINATION ->
                if (isIntermediateStop) "وصلت إلى المحطة: $dest" else "وصلت إلى وجهتك: $dest"
            ManeuverType.MERGE -> "اندمج مع حركة السير$street"
        }
    }

    /** صياغة «قبل المنعطف»: تحافظ على المعنى الصحيح مع الوصول (مستقبل) بدل الماضي. */
    fun upcomingText(instruction: String, maneuver: ManeuverType, distanceMeters: Double): String {
        val d = spoken(distanceMeters)
        return if (maneuver == ManeuverType.DESTINATION) {
            "بعد $d ستصل إلى " + instruction.removePrefix("وصلت إلى ")
        } else "بعد $d، $instruction"
    }

    private val ORDINALS = listOf("الأول", "الثاني", "الثالث", "الرابع", "الخامس", "السادس", "السابع", "الثامن", "التاسع", "العاشر")
    private fun ordinal(n: Int): String? = when {
        n in 1..10 -> ORDINALS[n - 1]
        n > 10 -> "رقم $n"
        else -> null
    }

    /** نطق المسافة: <100 م تقريب لـ10، حتى 1 كم لـ50، وما فوق بالكيلومتر (Locale.US). */
    fun spoken(meters: Double): String {
        val m = if (meters.isNaN() || meters < 0) 0.0 else meters
        return when {
            m < 30 -> "أقل من 30 متراً"
            m < 100 -> "${(m / 10).roundToInt() * 10} متراً"
            m < 1000 -> "${(m / 50).roundToInt() * 50} متراً"
            else -> {
                val km = String.format(Locale.US, "%.1f", m / 1000.0).removeSuffix(".0")
                "$km كيلومتر"
            }
        }
    }
}
