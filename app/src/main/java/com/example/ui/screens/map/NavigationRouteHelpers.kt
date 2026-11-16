package com.example.ui.screens.map

import android.content.Context
import android.content.Intent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.data.PropertyEntity
import com.example.data.ProviderEntity
import com.example.data.StoreEntity
import com.example.utils.GpxWriter
import com.example.utils.ManeuverType
import com.example.utils.NavStep
import com.example.utils.NavWaypoint
import com.example.utils.RoutePdfBuilder
import com.example.utils.SosDispatcher
import com.example.utils.TrackPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal fun isValidCoordinate(lat: Double, lng: Double): Boolean =
    !lat.isNaN() && !lng.isNaN() && lat in -90.0..90.0 && lng in -180.0..180.0 && !(lat == 0.0 && lng == 0.0)

/** توافق مع الاستدعاء القديم الذي يمرر كياناً واحداً. */
internal fun legacyTargetWaypoint(entity: Any?): NavWaypoint? {
    val (name, lat, lng) = when (entity) {
        is ProviderEntity -> Triple(entity.name, entity.latitude, entity.longitude)
        is StoreEntity -> Triple(entity.name, entity.latitude, entity.longitude)
        is PropertyEntity -> Triple(entity.title, entity.latitude, entity.longitude)
        else -> return null
    }
    return if (isValidCoordinate(lat, lng)) NavWaypoint(name = name, latitude = lat, longitude = lng) else null
}

internal fun maneuverIcon(type: ManeuverType?): ImageVector = when (type) {
    ManeuverType.LEFT, ManeuverType.SLIGHT_LEFT, ManeuverType.SHARP_LEFT -> Icons.AutoMirrored.Filled.ArrowBack
    ManeuverType.RIGHT, ManeuverType.SLIGHT_RIGHT, ManeuverType.SHARP_RIGHT -> Icons.AutoMirrored.Filled.ArrowForward
    ManeuverType.UTURN, ManeuverType.ROUNDABOUT -> Icons.Default.Refresh
    ManeuverType.DESTINATION -> Icons.Default.Place
    else -> Icons.Default.Navigation
}

internal fun shareNavigationGpx(
    context: Context,
    scope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
    title: String,
    recordedPoints: List<TrackPoint>,
    routeCoordinates: List<Pair<Double, Double>>
) {
    val gpxContent = when {
        recordedPoints.size >= 2 -> GpxWriter.track(title, recordedPoints)
        routeCoordinates.size >= 2 -> GpxWriter.route(title, routeCoordinates)
        else -> null
    }
    if (gpxContent == null) {
        scope.launch { snackbarHostState.showSnackbar("لا توجد نقاط مسار كافية لتصدير GPX") }
        return
    }
    val uri = GpxWriter.saveAndUri(context, "route_${System.currentTimeMillis()}.gpx", gpxContent)
    if (uri != null) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/gpx+xml"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        SosDispatcher.safeStart(context, Intent.createChooser(intent, "مشاركة ملف GPX"))
    } else {
        scope.launch { snackbarHostState.showSnackbar("تعذر حفظ ملف GPX") }
    }
}

internal fun shareNavigationPdf(
    context: Context,
    scope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
    targetName: String,
    coords: List<Pair<Double, Double>>,
    waypoints: List<NavWaypoint>,
    steps: List<NavStep>,
    totalDistanceMeters: Double,
    totalDurationSeconds: Double
) {
    val uri = RoutePdfBuilder.buildAndUri(
        context = context,
        title = "خطة المسار: $targetName",
        coords = coords,
        waypoints = waypoints,
        steps = steps,
        totalDistanceMeters = totalDistanceMeters,
        totalDurationSeconds = totalDurationSeconds
    )
    if (uri != null) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        SosDispatcher.safeStart(context, Intent.createChooser(intent, "مشاركة خطة المسار PDF"))
    } else {
        scope.launch { snackbarHostState.showSnackbar("تعذر إنشاء ملف PDF") }
    }
}
