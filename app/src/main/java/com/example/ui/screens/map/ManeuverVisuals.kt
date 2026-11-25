package com.example.ui.screens.map

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.utils.ManeuverType

fun maneuverVector(m: ManeuverType): ImageVector = when (m) {
    ManeuverType.LEFT -> Icons.Filled.TurnLeft
    ManeuverType.RIGHT -> Icons.Filled.TurnRight
    ManeuverType.SLIGHT_LEFT -> Icons.Filled.TurnSlightLeft
    ManeuverType.SLIGHT_RIGHT -> Icons.Filled.TurnSlightRight
    ManeuverType.SHARP_LEFT -> Icons.Filled.TurnSharpLeft
    ManeuverType.SHARP_RIGHT -> Icons.Filled.TurnSharpRight
    ManeuverType.UTURN -> Icons.Filled.UTurnLeft
    ManeuverType.ROUNDABOUT -> Icons.Filled.RoundaboutLeft
    ManeuverType.DESTINATION -> Icons.Filled.Place
    else -> Icons.Filled.Straight
}

/** يفصل المسافة إلى (رقم، وحدة) للعرض الكبير. 54 → ("54","م") و1530 → ("1.5","كم") */
fun splitDistance(meters: Double): Pair<String, String> {
    val m = if (meters.isNaN() || meters < 0) 0.0 else meters
    return when {
        m < 100 -> (((m / 5).toInt()) * 5).coerceAtLeast(0).toString() to "م"
        m < 1000 -> (((m / 10).toInt()) * 10).toString() to "م"
        else -> java.lang.String.format(java.util.Locale.US, "%.1f", m / 1000.0).removeSuffix(".0") to "كم"
    }
}
