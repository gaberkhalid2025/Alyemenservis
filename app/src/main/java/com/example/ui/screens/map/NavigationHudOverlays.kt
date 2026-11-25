package com.example.ui.screens.map

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.map.utils.MapDistanceCalculator
import com.example.utils.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 🧭 NavigationHudOverlays
 * واجهات القيادة العلوية والسفلية لشاشة الملاحة (بطاقة المنعطفات، شريط المحطات، التنبيهات، الردود السريعة، وشريط الأدوات)
 */
@Composable
internal fun NavigationTopOverlay(
    engineLabel: String,
    isApproximate: Boolean,
    isTilesDownloaded: Boolean,
    nativeError: String?,
    waypoints: List<NavWaypoint>,
    currentWaypointIndex: Int,
    isTopCardCollapsed: Boolean,
    onToggleTopCardCollapsed: (Boolean) -> Unit,
    upcoming: NavStep?,
    isLoadingRoute: Boolean,
    hasResult: Boolean,
    voiceEnabled: Boolean,
    onToggleVoice: () -> Unit,
    dialectVoice: Boolean,
    voiceUsingFallback: Boolean,
    currentSpeedKmh: Int,
    distToManeuver: Double,
    instructionText: String,
    activeHazardBanner: String?,
    onToggleMapMode: () -> Unit,
    onOpenAddStop: () -> Unit,
    onOpenDownloadDialog: () -> Unit,
    onRemoveWaypoint: (Int) -> Unit,
    onOpenVoiceSetup: () -> Unit,
    vehicleProfile: NavVehicleProfile = NavVehicleProfile.SEDAN,
    onCycleVehicleProfile: () -> Unit = {},
    streetCount: Int = 0,
    onOpenStreets: () -> Unit = {},
    onOpenLayersSheet: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // شارة الحالة
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF0F172A).copy(alpha = 0.95f),
            border = BorderStroke(1.dp, if (isApproximate) Color(0xFFF59E0B) else Color(0xFF10B981)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(if (isApproximate) Color(0xFFF59E0B) else Color(0xFF10B981)))
                    Text(engineLabel, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Surface(
                        onClick = onCycleVehicleProfile,
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1E293B),
                        border = BorderStroke(1.dp, Color(0xFF334155)),
                        modifier = Modifier.testTag("nav_vehicle_profile_btn")
                    ) {
                        Text(
                            text = "${vehicleProfile.emoji} ${vehicleProfile.labelAr}",
                            color = Color(0xFF00E5FF),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                    }
                    if (streetCount > 0) {
                        Surface(
                            onClick = onOpenStreets,
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF0284C7).copy(alpha = 0.85f),
                            border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                            modifier = Modifier.testTag("nav_streets_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text("🛣️", fontSize = 10.sp)
                                Text(
                                    text = "$streetCount",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    IconButton(onClick = onOpenLayersSheet, modifier = Modifier.size(30.dp).testTag("nav_layers_btn")) {
                        Icon(Icons.Default.Layers, contentDescription = "الطبقات", tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onOpenAddStop, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Add, contentDescription = "إضافة محطة", tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
                    }
                    if (!isTilesDownloaded || nativeError != null) {
                        IconButton(
                            onClick = onOpenDownloadDialog,
                            modifier = Modifier.size(30.dp).testTag("nav_download_offline_button")
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = "تحميل أو إصلاح خرائط الأوفلاين", tint = Color(0xFF0284C7), modifier = Modifier.size(18.dp))
                        }
                    }
                }
                nativeError?.let {
                    Text(
                        text = "تعذر تشغيل الأوفلاين: $it",
                        color = Color(0xFFF59E0B),
                        fontSize = 10.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // شريط المحطات (قابل للتمرير، الحالية مميزة)
        LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(waypoints) { index, wp ->
                val isCurrent = index == currentWaypointIndex
                val isPassed = index < currentWaypointIndex
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E293B).copy(alpha = if (isPassed) 0.6f else 0.95f),
                    border = BorderStroke(if (isCurrent) 2.dp else 1.dp, if (isCurrent) Color(0xFF10B981) else Color(0xFF00E5FF).copy(alpha = 0.4f))
                ) {
                    Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("${index + 1}", color = Color(0xFF00E5FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(wp.name, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 140.dp))
                        if (waypoints.size > 1 && !isPassed) {
                            IconButton(
                                onClick = { onRemoveWaypoint(index) },
                                modifier = Modifier.size(18.dp)
                            ) { Icon(Icons.Default.Close, contentDescription = "حذف", tint = Color(0xFFEF4444), modifier = Modifier.size(12.dp)) }
                        }
                    }
                }
            }
        }

        // بطاقة التعليمات (قابلة للطي)
        AnimatedContent(
            targetState = isTopCardCollapsed,
            transitionSpec = { fadeIn() + expandVertically() togetherWith fadeOut() + shrinkVertically() },
            label = "top_card_anim"
        ) { collapsed ->
            if (collapsed) {
                Surface(
                    onClick = { onToggleTopCardCollapsed(false) },
                    shape = CircleShape,
                    color = Color(0xFF0F172A).copy(alpha = 0.95f),
                    border = BorderStroke(1.5.dp, Color(0xFF00E5FF)),
                    shadowElevation = 8.dp,
                    modifier = Modifier.size(56.dp).align(Alignment.CenterHorizontally)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        androidx.compose.runtime.CompositionLocalProvider(
                            androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr
                        ) {
                            Icon(
                                maneuverIcon(if (isApproximate) null else upcoming?.maneuverType),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF0F172A).copy(alpha = 0.95f),
                    border = BorderStroke(1.5.dp, Color(0xFF00E5FF)),
                    shadowElevation = 12.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(shape = CircleShape, color = Color(0xFF0284C7), modifier = Modifier.size(44.dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (isLoadingRoute && !hasResult) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                                    else {
                                        androidx.compose.runtime.CompositionLocalProvider(
                                            androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr
                                        ) {
                                            Icon(maneuverIcon(if (isApproximate) null else upcoming?.maneuverType), contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                                        }
                                    }
                                }
                            }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "${if (voiceEnabled) "🔊" else "🔇"} $currentSpeedKmh كم/س",
                                        color = Color(0xFF00E5FF),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    if (!isApproximate && upcoming != null) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFF0284C7).copy(alpha = 0.35f),
                                            border = BorderStroke(0.5.dp, Color(0xFF38BDF8).copy(alpha = 0.7f))
                                        ) {
                                            Text(
                                                text = "بعد ${MapDistanceCalculator.formatDistance(distToManeuver)}",
                                                color = Color(0xFF38BDF8),
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                softWrap = false,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(instructionText, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                @OptIn(ExperimentalFoundationApi::class)
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .combinedClickable(
                                            onClick = onToggleVoice,
                                            onLongClick = onOpenVoiceSetup
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        if (voiceEnabled) (if (dialectVoice) Icons.Default.RecordVoiceOver else Icons.Default.VolumeUp) else Icons.Default.VolumeOff,
                                        contentDescription = "الصوت (اضغط مطولاً لإعداد الصوت واللهجة)",
                                        tint = when {
                                            !voiceEnabled -> Color(0xFF94A3B8)
                                            voiceUsingFallback -> Color(0xFFF59E0B)
                                            dialectVoice -> Color(0xFF00E5FF)
                                            else -> Color(0xFF10B981)
                                        }
                                    )
                                }
                                IconButton(
                                    onClick = onOpenVoiceSetup,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Tune, contentDescription = "إعدادات الصوت واللهجة", tint = Color(0xFFCBD5E1), modifier = Modifier.size(18.dp))
                                }
                                IconButton(onClick = { onToggleTopCardCollapsed(true) }, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "طي", tint = Color(0xFF94A3B8))
                                }
                            }
                        }

                        // مؤشر المسارب البصري للمنعطف القادم
                        if (!isApproximate && upcoming != null) {
                            val activeLane = when (upcoming.maneuverType) {
                                ManeuverType.LEFT, ManeuverType.SHARP_LEFT, ManeuverType.SLIGHT_LEFT -> 0
                                ManeuverType.RIGHT, ManeuverType.SHARP_RIGHT, ManeuverType.SLIGHT_RIGHT -> 2
                                else -> 1
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF1E293B).copy(alpha = 0.7f), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                val laneLabels = listOf("⬅️ يسار", "⬆️ مستقيم", "➡️ يمين")
                                laneLabels.forEachIndexed { idx, label ->
                                    val isAct = idx == activeLane
                                    Text(
                                        text = label,
                                        color = if (isAct) Color(0xFF10B981) else Color(0xFF64748B),
                                        fontSize = 11.sp,
                                        fontWeight = if (isAct) FontWeight.ExtraBold else FontWeight.Normal,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        activeHazardBanner?.let { hazardText ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xDD7F1D1D),
                border = BorderStroke(1.dp, Color(0xFFEF4444)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFCA5A5), modifier = Modifier.size(18.dp))
                    Text(hazardText, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
internal fun NavigationBottomOverlay(
    context: Context,
    scope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
    bookingId: String?,
    chatBridge: BookingChatBridge?,
    etaClock: String,
    result: NavRouteResult?,
    isApproximate: Boolean,
    remainingSeconds: Double,
    remainingMeters: Double,
    totalMeters: Double,
    progress: Float,
    isBottomCardCollapsed: Boolean,
    onToggleBottomCardCollapsed: (Boolean) -> Unit,
    onRequestExit: () -> Unit,
    onOpenHud: () -> Unit,
    isRecordingTrip: Boolean,
    recordedPointCount: Int,
    onToggleRecordTrip: () -> Unit,
    tripRecorder: TripRecorder,
    target: NavWaypoint?,
    waypoints: List<NavWaypoint>,
    steps: List<NavStep>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // زر إنهاء الملاحة المنفصل (FAB أحمر) لسلامة السائق
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            FloatingActionButton(
                onClick = onRequestExit,
                containerColor = Color(0xFFDC2626),
                contentColor = Color.White,
                modifier = Modifier.size(56.dp).testTag("end_navigation_fab")
            ) {
                Icon(Icons.Default.Stop, contentDescription = "إنهاء الملاحة", modifier = Modifier.size(28.dp))
            }

            if (bookingId != null && chatBridge != null) {
                IconButton(
                    onClick = { chatBridge.open(BookingChat.channelIdFor(bookingId)) },
                    modifier = Modifier.background(Color(0xFF0F172A).copy(alpha = 0.9f), CircleShape).size(48.dp)
                ) {
                    Icon(Icons.Default.Chat, contentDescription = "دردشة الحجز", tint = Color(0xFF38BDF8))
                }
            }
        }

        if (bookingId != null && chatBridge != null) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(BookingChat.quickReplies) { msg ->
                    AssistChip(
                        onClick = {
                            chatBridge.send(BookingChat.channelIdFor(bookingId), msg)
                            scope.launch { snackbarHostState.showSnackbar("تم الإرسال: $msg") }
                        },
                        label = { Text(msg, fontSize = 11.sp, color = Color.White) },
                        colors = AssistChipDefaults.assistChipColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.9f))
                    )
                }
                item {
                    AssistChip(
                        onClick = {
                            val etaMsg = BookingChat.etaMessage((remainingSeconds / 60).toInt(), null)
                            chatBridge.send(BookingChat.channelIdFor(bookingId), etaMsg)
                            scope.launch { snackbarHostState.showSnackbar("تم إرسال وقت الوصول المتوقع") }
                        },
                        label = { Text("أرسل وقت وصولي", fontSize = 11.sp, color = Color(0xFF00E5FF)) },
                        colors = AssistChipDefaults.assistChipColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.9f))
                    )
                }
            }
        }

        AnimatedContent(
            targetState = isBottomCardCollapsed,
            transitionSpec = { fadeIn() + expandVertically() togetherWith fadeOut() + shrinkVertically() },
            label = "bottom_card_anim"
        ) { collapsed ->
            if (collapsed) {
                Surface(
                    onClick = { onToggleBottomCardCollapsed(false) },
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF0F172A).copy(alpha = 0.95f),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "الوصول: $etaClock",
                            color = Color(0xFF10B981),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            MapDistanceCalculator.formatDistance(remainingMeters),
                            color = Color.White,
                            fontSize = 13.sp
                        )
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "توسيع", tint = Color(0xFF94A3B8))
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF0F172A).copy(alpha = 0.96f),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    shadowElevation = 14.dp,
                    modifier = Modifier.fillMaxWidth().clickable { onToggleBottomCardCollapsed(true) }
                ) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(Modifier.fillMaxWidth()) {
                            InfoCell("وقت الوصول", if (result == null) "—" else (if (isApproximate) "~ " else "") + etaClock, Color(0xFF10B981), Modifier.weight(1f))
                            InfoCell("المدة المتبقية", if (result == null) "—" else (if (isApproximate) "~ " else "") + MapDistanceCalculator.formatDuration(remainingSeconds), Color.White, Modifier.weight(1f))
                            InfoCell("المسافة المتبقية", if (result == null) "—" else MapDistanceCalculator.formatDistance(remainingMeters), Color.White, Modifier.weight(1f))
                        }
                        if (isApproximate && result != null) {
                            Text("القيم تقريبية (خط مستقيم)", color = Color(0xFFF59E0B), fontSize = 10.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        }
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = Color(0xFF00E5FF),
                            trackColor = Color(0xFF334155)
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            item {
                                AssistChip(
                                    onClick = onOpenHud,
                                    label = { Text("🚘 HUD", fontSize = 11.5.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold) },
                                    colors = AssistChipDefaults.assistChipColors(containerColor = Color(0xFF1E293B)),
                                    modifier = Modifier.heightIn(min = 40.dp).testTag("nav_hud_btn")
                                )
                            }
                            item {
                                AssistChip(
                                    onClick = onToggleRecordTrip,
                                    label = {
                                        Text(
                                            if (isRecordingTrip) "⏹️ إيقاف ($recordedPointCount)" else "⏺️ تسجيل",
                                            fontSize = 11.5.sp,
                                            color = if (isRecordingTrip) Color(0xFFFCA5A5) else Color.White
                                        )
                                    },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = if (isRecordingTrip) Color(0xFF7F1D1D) else Color(0xFF1E293B)
                                    ),
                                    modifier = Modifier.heightIn(min = 40.dp).testTag("nav_record_trip_btn")
                                )
                            }
                            item {
                                AssistChip(
                                    onClick = {
                                        shareNavigationGpx(
                                            context = context,
                                            scope = scope,
                                            snackbarHostState = snackbarHostState,
                                            title = target?.name?.ifBlank { "رحلة" } ?: "رحلة",
                                            recordedPoints = tripRecorder.snapshot(),
                                            routeCoordinates = result?.coordinates.orEmpty()
                                        )
                                    },
                                    label = { Text("🛰️ GPX", fontSize = 11.5.sp, color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold) },
                                    colors = AssistChipDefaults.assistChipColors(containerColor = Color(0xFF1E293B)),
                                    modifier = Modifier.heightIn(min = 40.dp).testTag("nav_export_gpx_btn")
                                )
                            }
                            item {
                                AssistChip(
                                    onClick = {
                                        shareNavigationPdf(
                                            context = context,
                                            scope = scope,
                                            snackbarHostState = snackbarHostState,
                                            targetName = target?.name ?: "الوجهة",
                                            coords = result?.coordinates.orEmpty(),
                                            waypoints = waypoints,
                                            steps = steps,
                                            totalDistanceMeters = totalMeters,
                                            totalDurationSeconds = result?.totalDurationSeconds ?: 0.0
                                        )
                                    },
                                    label = { Text("📄 PDF", fontSize = 11.5.sp, color = Color(0xFFFBBF24), fontWeight = FontWeight.Bold) },
                                    colors = AssistChipDefaults.assistChipColors(containerColor = Color(0xFF1E293B)),
                                    modifier = Modifier.heightIn(min = 40.dp).testTag("nav_export_pdf_btn")
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoCell(label: String, value: String, valueColor: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = valueColor, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(label, color = Color(0xFF94A3B8), fontSize = 11.sp, maxLines = 1)
    }
}
