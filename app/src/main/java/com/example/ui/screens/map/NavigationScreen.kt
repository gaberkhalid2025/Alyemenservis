package com.example.ui.screens.map

import android.content.Context
import android.os.SystemClock
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.example.data.ProviderEntity
import com.example.data.StoreEntity
import com.example.ui.screens.map.components.HudMirrorOverlay
import com.example.ui.screens.map.utils.MapDistanceCalculator
import com.example.utils.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

private const val NAV_TAG = "NavigationScreen"

/**
 * 🧭 NavigationScreen
 * المنسّق الرئيسي للملاحة خطوة بخطوة:
 * يدير محرك الصوت والتنبيهات، المسار وإعادة التوجيه، التقدير المؤقت (Dead Reckoning)، الخريطة،
 * ويفوّض واجهة القيادة إلى [NavigationTopOverlay] و [NavigationBottomOverlay]
 * والحوارات إلى [NavigationDialogsHost].
 */
@Composable
fun NavigationScreen(
    targetEntity: Any? = null,
    userLat: Double,
    userLng: Double,
    onFinishNavigation: () -> Unit,
    downloadUrl: String = ValhallaTileDownloader.DEFAULT_DOWNLOAD_URL,
    nearbyProviders: List<ProviderEntity> = emptyList(),
    nearbyStores: List<StoreEntity> = emptyList(),
    initialWaypoints: List<NavWaypoint> = emptyList(),
    hasRealLocation: Boolean = true,
    isDrivingMode: Boolean = true,
    bookingId: String? = null,
    chatBridge: BookingChatBridge? = null,
    hazardZones: List<MapZone> = emptyList(),
    onArrivedAtWaypoint: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val latestLat by rememberUpdatedState(userLat)
    val latestLng by rememberUpdatedState(userLng)

    // ───────── الصوت (محرك متعدد + نغمات أوفلاين احتياطية + إعداد ذكي) ─────────
    var voiceEnabled by remember { mutableStateOf(true) }
    var voiceState by remember {
        mutableStateOf(
            ArabicNavigationVoiceEngine.VoiceEngineState(
                googleTtsInstalled = ArabicNavigationVoiceEngine.isPackageInstalled(
                    context,
                    ArabicNavigationVoiceEngine.GOOGLE_TTS_PACKAGE
                )
            )
        )
    }
    var showVoiceSetupDialog by remember { mutableStateOf(false) }
    var voiceHintShown by remember { mutableStateOf(false) }

    val voiceEngine = remember(context) {
        ArabicNavigationVoiceEngine(context) { newState ->
            voiceState = newState
        }
    }
    DisposableEffect(voiceEngine) {
        voiceEngine.initialize()
        onDispose { voiceEngine.shutdown() }
    }

    LaunchedEffect(voiceState.isInitializing, voiceState.usingAudioCueFallback) {
        if (!voiceState.isInitializing && voiceState.usingAudioCueFallback && !voiceHintShown) {
            voiceHintShown = true
            val res = snackbarHostState.showSnackbar(
                message = "تعمل نغمات التوجيه المدمجة — لتفعيل النطق العربي اضغط إعداد",
                actionLabel = "إعداد الصوت",
                duration = SnackbarDuration.Long
            )
            if (res == SnackbarResult.ActionPerformed) {
                showVoiceSetupDialog = true
            }
        }
    }

    val speak: (String, ManeuverType?, Boolean) -> Unit = { text, maneuver, urgent ->
        if (voiceEnabled) {
            voiceEngine.speak(text, maneuver, urgent)
        }
    }

    // ───────── المحطات ─────────
    val waypoints = remember {
        mutableStateListOf<NavWaypoint>().apply {
            val fromParams = initialWaypoints.filter { isValidCoordinate(it.latitude, it.longitude) }
            if (fromParams.isNotEmpty()) addAll(fromParams) else legacyTargetWaypoint(targetEntity)?.let { add(it) }
        }
    }
    if (waypoints.isEmpty()) {
        LaunchedEffect(Unit) { onFinishNavigation() }
        return
    }

    // [إصلاح 19] إبقاء الشاشة مضاءة طوال فترة الملاحة
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    // [إصلاح 19] تأكيد الخروج بزر الرجوع لمنع الإنهاء العرضي
    var showExitConfirm by remember { mutableStateOf(false) }
    BackHandler(enabled = true) { showExitConfirm = true }

    var currentWaypointIndex by remember { mutableIntStateOf(0) }
    var routeForIndex by remember { mutableIntStateOf(0) }
    var lastArrivalAt by remember { mutableLongStateOf(0L) }
    var routeVersion by remember { mutableIntStateOf(0) }
    var routeResult by remember { mutableStateOf<NavRouteResult?>(null) }
    var isLoadingRoute by remember { mutableStateOf(true) }
    var tracker by remember { mutableStateOf<RouteTracker?>(null) }
    var trackerState by remember { mutableStateOf<TrackerResult?>(null) }
    var isTilesDownloaded by remember { mutableStateOf(ValhallaTileDownloader.isTilesDownloaded(context)) }
    var navigationFinished by remember { mutableStateOf(false) }
    var followUser by remember { mutableStateOf(true) }
    var mapMode by remember { mutableStateOf(MapMode.OSM) }
    var pendingStop by remember { mutableStateOf<NavWaypoint?>(null) }
    var showAddStopDialog by remember { mutableStateOf(false) }
    var showDownloadDialog by remember { mutableStateOf(false) }

    val speedEstimator = remember { SpeedEstimator() }
    val offRoute = remember { OffRouteDetector(NavConfig.OFF_ROUTE_THRESHOLD_METERS) }
    val arrival = remember { ArrivalDetector(NavConfig.ARRIVAL_THRESHOLD_METERS) }
    var currentSpeedKmh by remember { mutableIntStateOf(0) }
    var lastRerouteAt by remember { mutableLongStateOf(0L) }
    val announced = remember { mutableSetOf<String>() }
    var routeFailures by remember { mutableIntStateOf(0) }

    val voicePrefs = remember { context.getSharedPreferences("voice_prefs", Context.MODE_PRIVATE) }
    var dialectVoice by remember { mutableStateOf(voicePrefs.getBoolean("dialect", false)) }

    var isTopCardCollapsed by remember { mutableStateOf(false) }
    var isBottomCardCollapsed by remember { mutableStateOf(false) }
    var showHud by remember { mutableStateOf(false) }
    var isHudMirrored by remember { mutableStateOf(true) }

    val tripRecorder = remember { TripRecorder() }
    var isRecordingTrip by remember { mutableStateOf(false) }
    var recordedPointCount by remember { mutableIntStateOf(0) }
    val warnedHazards = remember { mutableSetOf<String>() }
    var activeHazardBanner by remember { mutableStateOf<String?>(null) }

    var drActive by remember { mutableStateOf(false) }
    var lastAlong by remember { mutableDoubleStateOf(0.0) }
    var lastSpeedMps by remember { mutableDoubleStateOf(0.0) }

    // تنبيه صوتي ومرئي مسبق قبل الدخول لمناطق وعورة الطريق
    LaunchedEffect(userLat, userLng, hazardZones, hasRealLocation) {
        if (!hasRealLocation || hazardZones.isEmpty()) {
            activeHazardBanner = null
            return@LaunchedEffect
        }
        val z = HazardAlerts.nearest(userLat, userLng, hazardZones, thresholdMeters = 300.0)
        if (z != null) {
            val msg = HazardAlerts.describeZone(z)
            activeHazardBanner = msg
            val key = "${String.format(Locale.US, "%.4f", z.lat)},${String.format(Locale.US, "%.4f", z.lng)}"
            if (warnedHazards.add(key)) {
                speak(msg, ManeuverType.STRAIGHT, true)
            }
        } else {
            activeHazardBanner = null
        }
    }

    // تسجيل مسار الرحلة عند التفعيل
    LaunchedEffect(userLat, userLng, isRecordingTrip, hasRealLocation) {
        if (isRecordingTrip && hasRealLocation) {
            tripRecorder.record(userLat, userLng)
            recordedPointCount = tripRecorder.size
        }
    }

    // تذكّر آخر وضع موثوق للـ Dead Reckoning
    LaunchedEffect(trackerState, hasRealLocation) {
        val ts = trackerState
        if (hasRealLocation && ts != null) {
            lastAlong = ts.alongMeters
            lastSpeedMps = currentSpeedKmh / 3.6
        }
    }

    // تقدير محدود عند ضياع الإشارة (Dead Reckoning ≤ 10 ثوانٍ)
    LaunchedEffect(hasRealLocation, tracker) {
        drActive = false
        if (hasRealLocation) return@LaunchedEffect
        val t = tracker ?: return@LaunchedEffect
        val v = lastSpeedMps.coerceIn(0.0, 25.0)
        if (v < NavConfig.DEAD_RECKONING_MIN_SPEED_MPS || routeResult == null || routeResult?.error != null) return@LaunchedEffect
        val startAlong = lastAlong
        val begin = SystemClock.elapsedRealtime()
        drActive = true
        while (true) {
            delay(1000L)
            val dt = (SystemClock.elapsedRealtime() - begin) / 1000.0
            if (dt > NavConfig.DEAD_RECKONING_MAX_SECONDS) break
            val alongM = (startAlong + v * dt).coerceAtMost(t.totalMeters)
            trackerState = TrackerResult(
                alongMeters = alongM,
                remainingMeters = (t.totalMeters - alongM).coerceAtLeast(0.0),
                offRouteMeters = 0.0,
                segmentIndex = trackerState?.segmentIndex ?: 0,
                progress = (alongM / t.totalMeters).toFloat().coerceIn(0f, 1f)
            )
        }
        drActive = false
    }

    // ───────── حساب المسار (من الموقع الحالي عبر المحطات المتبقية) ─────────
    val needsFirstRoute = hasRealLocation && routeResult == null
    LaunchedEffect(routeVersion, waypoints.size, isTilesDownloaded, needsFirstRoute, isDrivingMode) {
        val remaining = waypoints.drop(currentWaypointIndex)
        if (remaining.isEmpty()) return@LaunchedEffect
        if (!hasRealLocation) {
            isLoadingRoute = false
            return@LaunchedEffect
        }
        if (routeResult == null) isLoadingRoute = true
        try {
            val origin = NavWaypoint(name = "موقعي الحالي", latitude = latestLat, longitude = latestLng, type = WaypointType.START)
            val result = RealValhallaNavigator.calculateRoute(context, origin, remaining, driving = isDrivingMode)
            routeResult = result
            routeFailures = 0
            routeForIndex = currentWaypointIndex
            tracker = RouteTracker(result.coordinates)
            trackerState = null
            offRoute.reset()
            announced.clear()
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            Log.e(NAV_TAG, "Route calculation failed: ${t.message}", t)
            routeFailures += 1
            if (routeFailures <= 3) {
                scope.launch {
                    delay(5_000L * routeFailures)
                    routeVersion += 1
                }
            } else {
                scope.launch {
                    snackbarHostState.showSnackbar("تعذر حساب المسار. تحقق من الاتصال أو حمّل الخرائط الأوفلاين")
                }
            }
        } finally {
            isLoadingRoute = false
        }
    }

    val result = routeResult
    val isApproximate = result == null || result.error != null
    val target = waypoints.getOrNull(currentWaypointIndex)

    LaunchedEffect(isApproximate, hasRealLocation) {
        if (!isApproximate || !hasRealLocation) return@LaunchedEffect
        repeat(6) {
            delay(15_000L)
            routeVersion += 1
        }
    }

    // ───────── الوصول إلى محطة ─────────
    val handleArrival: () -> Unit = {
        val now = System.currentTimeMillis()
        lastArrivalAt = now
        tracker = null
        trackerState = null
        val arrived = waypoints.getOrNull(currentWaypointIndex)
        val next = waypoints.getOrNull(currentWaypointIndex + 1)
        if (arrived != null) {
            if (next == null) {
                speak("لقد وصلت إلى ${arrived.name}. اكتملت الرحلة", ManeuverType.DESTINATION, false)
                navigationFinished = true
            } else {
                val legMeters = routeResult?.legs?.getOrNull(1)?.distanceMeters
                    ?: HaversinePrecisionNavigator.calculateDistance(arrived.latitude, arrived.longitude, next.latitude, next.longitude)
                val km = String.format(Locale.US, "%.1f", legMeters / 1000.0)
                speak("لقد وصلت إلى ${arrived.name}. الوجهة التالية هي ${next.name}، وتبعد $km كم", ManeuverType.DESTINATION, false)
                currentWaypointIndex += 1
                routeVersion += 1
            }
            onArrivedAtWaypoint?.invoke(currentWaypointIndex)
        }
    }

    // ───────── تتبع الموقع الحقيقي ─────────
    LaunchedEffect(userLat, userLng, tracker) {
        val now = System.currentTimeMillis()
        if (!hasRealLocation) return@LaunchedEffect
        currentSpeedKmh = speedEstimator.update(userLat, userLng, now)
        val t = tracker
        if (t != null) trackerState = t.update(userLat, userLng)
        val tgt = waypoints.getOrNull(currentWaypointIndex) ?: return@LaunchedEffect
        val d = HaversinePrecisionNavigator.calculateDistance(userLat, userLng, tgt.latitude, tgt.longitude)

        val ts = trackerState
        val legEnd = routeResult?.legs?.firstOrNull()?.distanceMeters ?: 0.0
        val isCurrentLegRoute = (routeForIndex == currentWaypointIndex)
        val reachedByRoute = !isApproximate && isCurrentLegRoute && ts != null && legEnd > 0.0 &&
            (legEnd - ts.alongMeters) < 20.0 && ts.offRouteMeters < 40.0

        val canCheckArrival = (now - lastArrivalAt > 4000L)
        if (canCheckArrival && isCurrentLegRoute && arrival.check(currentWaypointIndex, if (reachedByRoute) 0.0 else d)) {
            handleArrival()
        } else if (!isApproximate && isCurrentLegRoute && ts != null && !isLoadingRoute &&
            offRoute.update(ts.offRouteMeters, now) && now - lastRerouteAt > 10_000L
        ) {
            lastRerouteAt = now
            offRoute.reset()
            speak("جارٍ إعادة حساب المسار", ManeuverType.STRAIGHT, true)
            routeVersion += 1
        }
    }

    LaunchedEffect(navigationFinished) {
        if (navigationFinished) {
            delay(5_000L)
            onFinishNavigation()
        }
    }
    // العودة التلقائية لوضع التتبع بعد 8 ثوانٍ من سحب الخريطة
    LaunchedEffect(followUser) {
        if (!followUser) {
            delay(8_000L)
            followUser = true
        }
    }

    // ───────── مشتقات العرض ─────────
    val steps = remember(result) { result?.getAllSteps() ?: emptyList() }
    val stepStarts = remember(steps) {
        DoubleArray(steps.size).also { arr -> var acc = 0.0; steps.forEachIndexed { i, s -> arr[i] = acc; acc += s.distanceMeters } }
    }
    val along = trackerState?.alongMeters ?: 0.0
    val totalMeters = tracker?.totalMeters ?: result?.totalDistanceMeters ?: 0.0
    val remainingMeters = trackerState?.remainingMeters ?: totalMeters
    val progress = trackerState?.progress ?: 0f
    val baseSeconds = if (totalMeters > 1.0) (result?.totalDurationSeconds ?: 0.0) * (remainingMeters / totalMeters) else 0.0
    val expectedKmh = if ((result?.totalDurationSeconds ?: 0.0) > 0) totalMeters / result!!.totalDurationSeconds * 3.6 else 0.0
    val speedFactor = if (!isApproximate && currentSpeedKmh >= 15 && expectedKmh > 0) (expectedKmh / currentSpeedKmh).coerceIn(0.7, 1.5) else 1.0
    val remainingSeconds = baseSeconds * speedFactor
    val currentStepIndex = run {
        var idx = 0
        for (i in steps.indices) {
            if (stepStarts[i] <= along + 1.0) idx = i else break
        }
        idx
    }
    val upcoming = steps.getOrNull(currentStepIndex + 1)
    val distToManeuver = if (upcoming != null) max(0.0, stepStarts[currentStepIndex + 1] - along) else remainingMeters
    val distToTarget = if (target != null)
        HaversinePrecisionNavigator.calculateDistance(userLat, userLng, target.latitude, target.longitude) else 0.0

    // ───────── النطق حسب المسافة والسرعة ─────────
    LaunchedEffect(currentStepIndex, along, routeVersion, drActive) {
        if (isApproximate || upcoming == null || !(hasRealLocation || drActive)) return@LaunchedEffect
        val v = max(currentSpeedKmh / 3.6, 5.0)
        val far = max(250.0, v * 20)
        val mid = max(100.0, v * 8)
        val near = 30.0
        val level = when {
            distToManeuver <= near -> "N"
            distToManeuver <= mid -> "M"
            distToManeuver <= far -> "F"
            else -> return@LaunchedEffect
        }
        val key = "$routeVersion:${currentStepIndex + 1}:$level"
        if (announced.add(key)) {
            val isDest = upcoming.maneuverType == ManeuverType.DESTINATION
            if (!(isDest && level == "N")) {
                val text = if (dialectVoice) {
                    if (level == "N") YemeniPhrases.maneuver(upcoming.maneuverType, upcoming.streetName)
                    else YemeniPhrases.upcoming(upcoming.maneuverType, upcoming.streetName, distToManeuver)
                } else if (level == "N") upcoming.instruction
                else ArabicInstructionFormatter.upcomingText(upcoming.instruction, upcoming.maneuverType, distToManeuver)
                speak(text, upcoming.maneuverType, false)
            }
        }
    }

    val engineLabel = when {
        result == null -> "جارٍ حساب المسار..."
        isApproximate -> "تقريبي - مسافة مباشرة"
        result.isOffline -> "🗺️ وضع غير متصل · ${result.engineSource}"
        else -> "🛰️ وضع متصل · ${result.engineSource}"
    }
    val nativeError = if (isTilesDownloaded && result != null && !result.isOffline) NativeValhallaStatus.lastError else null
    val instructionText = when {
        drActive -> "إشارة GPS ضعيفة — تقدير مؤقت للموقع"
        !hasRealLocation -> if (result != null) "إشارة GPS ضعيفة — سنواصل التوجيه عند عودتها" else "لم يتم تحديد موقعك بعد — فعّل GPS"
        result == null -> "جارٍ حساب المسار..."
        isApproximate -> "تقريبي: ${MapDistanceCalculator.formatDistance(distToTarget)} مباشرة إلى ${target?.name ?: ""} (بدون تعليمات منعطفات)"
        upcoming != null -> ArabicInstructionFormatter.upcomingText(upcoming.instruction, upcoming.maneuverType, distToManeuver)
        else -> "واصل حتى الوصول إلى ${target?.name ?: "الوجهة"}"
    }
    val etaClock = remember(remainingSeconds) {
        DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(System.currentTimeMillis() + (remainingSeconds * 1000).toLong()))
    }

    val smartZoom: Int? = when {
        isApproximate || upcoming == null || !hasRealLocation -> null
        distToManeuver < NavConfig.SMART_ZOOM_NEAR_METERS -> NavConfig.SMART_ZOOM_NEAR_LEVEL
        else -> NavConfig.SMART_ZOOM_CRUISE_LEVEL
    }

    Box(modifier = modifier.fillMaxSize().background(Color(0xFF0B1120))) {

        // ───────── الخريطة ─────────
        RealLeafletMapView(
            userCoords = Pair(userLat, userLng),
            nearbyProviders = nearbyProviders,
            nearbyStores = nearbyStores,
            nearbyProperties = emptyList(),
            dynamicOffsets = emptyMap(),
            currentMapMode = mapMode,
            selectedCity = "الكل",
            selectedEntity = null,
            onProviderSelected = { p ->
                if (isValidCoordinate(p.latitude, p.longitude)) pendingStop = NavWaypoint(name = p.name, latitude = p.latitude, longitude = p.longitude)
            },
            onStoreSelected = { s ->
                if (isValidCoordinate(s.latitude, s.longitude)) pendingStop = NavWaypoint(name = s.name, latitude = s.latitude, longitude = s.longitude)
            },
            onPropertySelected = {},
            onDeselect = {},
            routeCoordinates = result?.coordinates ?: emptyList(),
            routeKey = routeVersion,
            waypointMarkers = waypoints.mapIndexed { i, w -> Triple("${i + 1}. ${w.name}", w.latitude, w.longitude) },
            followUser = followUser,
            onUserPanned = { followUser = false },
            navZoom = smartZoom,
            modifier = Modifier.fillMaxSize()
        )

        NavigationTopOverlay(
            engineLabel = engineLabel,
            isApproximate = isApproximate,
            isTilesDownloaded = isTilesDownloaded,
            nativeError = nativeError,
            waypoints = waypoints.toList(),
            currentWaypointIndex = currentWaypointIndex,
            isTopCardCollapsed = isTopCardCollapsed,
            onToggleTopCardCollapsed = { isTopCardCollapsed = it },
            upcoming = upcoming,
            isLoadingRoute = isLoadingRoute,
            hasResult = result != null,
            voiceEnabled = voiceEnabled,
            onToggleVoice = { voiceEnabled = !voiceEnabled },
            dialectVoice = dialectVoice,
            voiceUsingFallback = voiceState.usingAudioCueFallback,
            currentSpeedKmh = currentSpeedKmh,
            distToManeuver = distToManeuver,
            instructionText = instructionText,
            activeHazardBanner = activeHazardBanner,
            onToggleMapMode = { mapMode = if (mapMode == MapMode.OSM) MapMode.SATELLITE else MapMode.OSM },
            onOpenAddStop = { showAddStopDialog = true },
            onOpenDownloadDialog = { showDownloadDialog = true },
            onRemoveWaypoint = { index ->
                waypoints.removeAt(index)
                if (index < currentWaypointIndex) currentWaypointIndex -= 1
                if (currentWaypointIndex >= waypoints.size) navigationFinished = true else routeVersion += 1
            },
            onOpenVoiceSetup = { showVoiceSetupDialog = true },
            modifier = Modifier.align(Alignment.TopCenter)
        )

        if (!followUser) {
            FloatingActionButton(
                onClick = { followUser = true },
                containerColor = Color(0xFF0F172A),
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp).size(46.dp)
            ) { Icon(Icons.Default.MyLocation, contentDescription = "موقعي", tint = Color(0xFF00E5FF)) }
        }

        SnackbarHost(
            snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 240.dp)
        )

        NavigationBottomOverlay(
            context = context,
            scope = scope,
            snackbarHostState = snackbarHostState,
            bookingId = bookingId,
            chatBridge = chatBridge,
            etaClock = etaClock,
            result = result,
            isApproximate = isApproximate,
            remainingSeconds = remainingSeconds,
            remainingMeters = remainingMeters,
            totalMeters = totalMeters,
            progress = progress,
            isBottomCardCollapsed = isBottomCardCollapsed,
            onToggleBottomCardCollapsed = { isBottomCardCollapsed = it },
            onRequestExit = { showExitConfirm = true },
            onOpenHud = { showHud = true },
            isRecordingTrip = isRecordingTrip,
            recordedPointCount = recordedPointCount,
            onToggleRecordTrip = {
                isRecordingTrip = !isRecordingTrip
                if (isRecordingTrip && hasRealLocation) {
                    tripRecorder.record(userLat, userLng)
                    recordedPointCount = tripRecorder.size
                }
                scope.launch {
                    snackbarHostState.showSnackbar(
                        if (isRecordingTrip) "⏺️ بدأ تسجيل مسار الرحلة"
                        else "⏸️ توقف التسجيل ($recordedPointCount نقطة محفوظة)"
                    )
                }
            },
            tripRecorder = tripRecorder,
            target = target,
            waypoints = waypoints.toList(),
            steps = steps,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        if (showHud) {
            BackHandler(enabled = true) { showHud = false }
            HudMirrorOverlay(
                speedKmh = currentSpeedKmh,
                distToManeuverText = MapDistanceCalculator.formatDistance(distToManeuver),
                instruction = instructionText,
                maneuverType = if (isApproximate) null else upcoming?.maneuverType,
                mirrored = isHudMirrored,
                onToggleMirror = { isHudMirrored = !isHudMirrored },
                onClose = { showHud = false }
            )
        }

        NavigationDialogsHost(
            context = context,
            scope = scope,
            snackbarHostState = snackbarHostState,
            userLat = userLat,
            userLng = userLng,
            nearbyProviders = nearbyProviders,
            nearbyStores = nearbyStores,
            downloadUrl = downloadUrl,
            showExitConfirm = showExitConfirm,
            onDismissExitConfirm = { showExitConfirm = false },
            onConfirmFinishNavigation = onFinishNavigation,
            pendingStop = pendingStop,
            onDismissPendingStop = { pendingStop = null },
            onAddWaypoint = { waypoints.add(it) },
            showAddStopDialog = showAddStopDialog,
            onDismissAddStopDialog = { showAddStopDialog = false },
            showVoiceSetupDialog = showVoiceSetupDialog,
            onDismissVoiceSetupDialog = { showVoiceSetupDialog = false },
            voiceState = voiceState,
            dialectVoice = dialectVoice,
            onToggleDialect = { dialectVoice = it },
            voicePrefs = voicePrefs,
            voiceEngine = voiceEngine,
            showDownloadDialog = showDownloadDialog,
            onDismissDownloadDialog = { showDownloadDialog = false },
            onTilesDownloaded = {
                isTilesDownloaded = true
                routeVersion++
            }
        )
    }
}
