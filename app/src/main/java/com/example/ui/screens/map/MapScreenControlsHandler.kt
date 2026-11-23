@file:OptIn(
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.example.ui.screens.map

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.MainViewModel
import com.example.ui.screens.map.components.MapControls
import com.example.ui.screens.map.components.PrimaryLayersBottomSheet
import com.example.ui.screens.map.components.SecondaryLayersBottomSheet
import com.example.ui.screens.map.components.UnifiedLayersSheet
import com.example.utils.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 🎮 MapScreenControlsHandler
 * ربط لوحة التحكم الجانبية (MapControls) وقوائم الطبقات الأساسية والثانوية وخدمات الطريق
 */
@Composable
internal fun BoxScope.MapControlsDockHost(
    context: Context,
    coroutineScope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
    viewModel: MainViewModel,
    state: MapScreenState,
    themeColors: VisualThemePalette,
    hasRealLocation: Boolean,
    heading: Float,
    isDrivingMode: Boolean,
    onToggleDrivingMode: () -> Unit,
    isEcoMode: Boolean,
    isPinPickMode: Boolean,
    onSetPinPickMode: (Boolean) -> Unit,
    onClearPinnedAndSearched: () -> Unit,
    searchedPlace: RealValhallaNavigator.SearchAddressResult?,
    onClearSearchedOnly: () -> Unit,
    showZones: Boolean,
    onSetShowZones: (Boolean) -> Unit,
    locationPermissionLauncher: ActivityResultLauncher<Array<String>>,
    onShowDestinationPicker: () -> Unit,
    onShowGnss: () -> Unit,
    onShowCoordinateDialog: () -> Unit,
    onOpenMoreLayersSheet: () -> Unit,
    onCloseMoreLayersSheet: () -> Unit,
    onShowGroupTracking: () -> Unit
) {
    MapControls(
        isRadarMode = state.isRadarMode,
        onToggleRadarMode = { state.isRadarMode = !state.isRadarMode },
        isHeatmapActive = state.isHeatmapActive,
        onToggleHeatmap = { state.isHeatmapActive = !state.isHeatmapActive },
        onZoomIn = { state.zoomInTick++ },
        onZoomOut = { state.zoomOutTick++ },
        onRecenterLocation = {
            state.panOffset = Offset.Zero
            state.zoomScale = 1.0f
            state.recenterTick++
            val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            if (hasFine || hasCoarse) {
                viewModel.startLocationUpdates()
            } else {
                locationPermissionLauncher.launch(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                )
            }
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    if (hasRealLocation) "تمت إعادة التمركز إلى موقعك 📍" else "جارٍ تحديد موقعك… تأكد من تشغيل GPS"
                )
            }
        },
        onOpenLayerDialog = {
            onCloseMoreLayersSheet()
            state.showLayerDialog = true
        },
        isNavigationActive = state.isNavigating,
        onToggleNavigation = {
            val searchedWp = searchedPlace?.let {
                NavWaypoint(name = it.shortName, latitude = it.latitude, longitude = it.longitude)
            }
            when {
                !hasRealLocation -> {
                    viewModel.startLocationUpdates()
                    coroutineScope.launch { snackbarHostState.showSnackbar("فعّل GPS وانتظر تحديد موقعك ثم أعد المحاولة") }
                }
                state.startNavigation(state.selectedEntity ?: searchedWp) -> {
                    onClearSearchedOnly()
                }
                else -> {
                    onShowDestinationPicker()
                }
            }
        },
        isGpsActive = viewModel.isGpsTrackingActive.collectAsState().value,
        themeColors = themeColors,
        currentMapMode = state.currentMapMode,
        onSelectMapMode = { mode, _ ->
            state.toggleMapMode(mode)
            state.isRadarMode = false
            val label = when (state.currentMapMode) {
                MapMode.SATELLITE -> "أقمار صناعية (Esri)"
                MapMode.SATELLITE_CLARITY -> "أقمار فائقة الوضوح (Clarity)"
                MapMode.VELA -> "خرائط VELA المجانية"
                MapMode.VELA_3D -> "خرائط 3D التفاعلية"
                MapMode.HOT -> "شوارع (HOT)"
                MapMode.TOPO -> "تضاريس (Topo)"
                else -> "خريطة قياسية (OSM)"
            }
            coroutineScope.launch {
                snackbarHostState.showSnackbar("النمط: $label")
            }
        },
        isNightMode = state.nightMode,
        onToggleNight = {
            state.nightMode = !state.nightMode
            coroutineScope.launch {
                snackbarHostState.showSnackbar(if (state.nightMode) "🌙 تم تفعيل الوضع الليلي للخريطة" else "☀️ تم تفعيل الوضع النهاري للخريطة")
            }
        },
        isGibsActive = state.gibsEnabled,
        onToggleGibs = {
            state.gibsEnabled = !state.gibsEnabled
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    if (state.gibsEnabled) "🛰️ تم تفعيل طبقة ناسا اليومية (${if (state.gibsProduct == "landtemp") "حرارة السطح" else "الألوان الحقيقية"})"
                    else "تم إيقاف طبقة ناسا"
                )
            }
        },
        onGibsLongPress = {
            state.gibsProduct = if (state.gibsProduct == "truecolor") "landtemp" else "truecolor"
            state.gibsEnabled = true
            val prodName = if (state.gibsProduct == "landtemp") "حرارة السطح النهاري" else "الألوان الحقيقية (TrueColor)"
            coroutineScope.launch {
                snackbarHostState.showSnackbar("🛰️ تم التبديل إلى منتج ناسا: $prodName")
            }
        },
        isDrivingMode = isDrivingMode,
        onToggleDrivingMode = {
            onToggleDrivingMode()
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    if (!isDrivingMode) "🚘 تم تفعيل وضع القيادة والملاحة المباشرة"
                    else "🚶 تم تفعيل وضع المشي المريح"
                )
            }
        },
        compassBearing = -heading,
        onResetCompass = {
            coroutineScope.launch {
                val deg = ((heading % 360f) + 360f) % 360f
                val names = listOf("شمال", "شمال شرق", "شرق", "جنوب شرق", "جنوب", "جنوب غرب", "غرب", "شمال غرب")
                val dir = names[(((deg + 22.5f) / 45f).toInt()) % 8]
                snackbarHostState.showSnackbar("اتجاه الجهاز: ${deg.toInt()}° ($dir)")
            }
        },
        onShowGnss = onShowGnss,
        isZonesActive = showZones || state.reliabilityEnabled,
        onToggleZones = {
            val next = !(showZones || state.reliabilityEnabled)
            onSetShowZones(next)
            state.reliabilityEnabled = next
            coroutineScope.launch {
                snackbarHostState.showSnackbar(if (next) "🟢 تم تفعيل طبقة توفر وموثوقية الخدمات" else "تم إخفاء طبقة توفر الخدمات")
            }
        },
        onToggleArVela = {
            state.currentMapMode = if (state.currentMapMode == MapMode.AR_VPS) MapMode.SATELLITE else MapMode.AR_VPS
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    if (state.currentMapMode == MapMode.AR_VPS) "📷 تم تشغيل كاميرا الواقع المعزز (VELA AR)"
                    else "🗺️ تم العودة لخريطة الأقمار الصناعية"
                )
            }
        },
        isPinPickMode = isPinPickMode,
        onTogglePinPickMode = {
            val next = !isPinPickMode
            onSetPinPickMode(next)
            if (next) {
                onClearPinnedAndSearched()
            }
        },
        isEcoMode = isEcoMode,
        onToggleEcoMode = {
            val next = !isEcoMode
            MapPowerDataPrefs.setEcoMode(context, next)
            viewModel.startLocationUpdates()
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    if (next) "🔋 تم تفعيل وضع توفير البطارية والنت (كاش ذكي وتحديث GPS اقتصادي)"
                    else "⚡ تم العودة للوضع القياسي للخرائط والموقع"
                )
            }
        },
        onOpenCoordinateEntry = onShowCoordinateDialog,
        hasMoreLayersActive = state.eumetEnabled || state.gibsEnabled || state.eoxEnabled || state.realTimeSatEnabled ||
            state.quakesEnabled || state.rainEnabled ||
            state.peakEnabled || state.coverageEnabled || state.hazardEnabled ||
            state.reliabilityEnabled || state.nightMode,
        hasLayerOverlayActive = state.is3DTerrainEnabled || state.mapLayer == MapLayer.VALHALLA_OFFLINE ||
            state.currentMapMode == MapMode.VELA || state.currentMapMode == MapMode.SATELLITE_CLARITY ||
            state.currentMapMode == MapMode.OSM || state.currentMapMode == MapMode.TOPO,
        onOpenMoreLayers = {
            state.showLayerDialog = false
            onOpenMoreLayersSheet()
        },
        onOpenGroupTracking = onShowGroupTracking,
        modifier = Modifier
            .align(Alignment.CenterEnd)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(end = 16.dp, top = 74.dp, bottom = 92.dp)
    )
}

@Composable
internal fun MapLayersAndServicesSheetsHost(
    coroutineScope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
    state: MapScreenState,
    showServicesSheet: Boolean,
    onDismissServicesSheet: () -> Unit,
    companionFilter: String?,
    onSelectCompanionFilter: (String?) -> Unit,
    isTilesDownloaded: Boolean,
    valhallaSizeMb: Int,
    onClearTilesFailing: () -> Unit,
    onOpenDownloadDialog: () -> Unit,
    is3DAvailable: Boolean,
    onPrepare3DToggle: (Boolean) -> Unit,
    showMoreLayersSheet: Boolean,
    onDismissMoreLayersSheet: () -> Unit,
    showZones: Boolean,
    onSetShowZones: (Boolean) -> Unit,
    onCoverageCleared: () -> Unit,
    adminRole: String = "GUEST",
    isRegisteredUser: Boolean = false,
    isProvider: Boolean = false,
    enableLiveTrafficByAdmin: Boolean = true
) {
    if (showServicesSheet) {
        ModalBottomSheet(
            onDismissRequest = onDismissServicesSheet,
            containerColor = Color(0xFF0F172A),
            contentColor = Color.White,
            dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.3f)) }
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(bottom = 48.dp, start = 24.dp, end = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("خدمات الطريق السريع", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))

                val items = listOf(
                    Triple("وقود", "⛽ محطات وقود", "محطة"),
                    Triple("استراحة", "☕ استراحات ومطاعم", "استراحة"),
                    Triple("ميكانيك", "🔧 بنشر وصيانة", "صيانة")
                )

                items.forEach { (_, label, filterQuery) ->
                    val isActive = companionFilter == filterQuery
                    Surface(
                        onClick = {
                            onSelectCompanionFilter(if (isActive) null else filterQuery)
                            onDismissServicesSheet()
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isActive) Color(0xFF10B981).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f),
                        border = if (isActive) BorderStroke(1.dp, Color(0xFF10B981)) else null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(label, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            if (isActive) Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF10B981))
                        }
                    }
                }
            }
        }
    }

    val isAnySheetOpen = state.showLayerDialog || showMoreLayersSheet
    if (isAnySheetOpen) {
        val initialTab = if (showMoreLayersSheet && !state.showLayerDialog) 1 else 0
        UnifiedLayersSheet(
            mapLayer = state.mapLayer,
            onToggleMapLayer = { next ->
                state.mapLayer = next
                onClearTilesFailing()
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        if (next == MapLayer.VALHALLA_OFFLINE) {
                            if (isTilesDownloaded) "📦 تم تفعيل وضع أوفلاين Valhalla (${valhallaSizeMb} MB)"
                            else "📦 تم تفعيل وضع الكاش الأوفلاين"
                        } else {
                            "🌐 تم العودة إلى وضع الخريطة القياسي"
                        }
                    )
                }
            },
            isValhallaDownloaded = isTilesDownloaded,
            valhallaSizeMb = valhallaSizeMb,
            onOpenValhallaDownload = onOpenDownloadDialog,
            currentMapMode = state.currentMapMode,
            onSelectMapMode = { mode ->
                state.currentMapMode = mode
                state.isRadarMode = false
                val modeLabel = when (mode) {
                    MapMode.VELA -> "✨ تم تفعيل خرائط VELA المجانية"
                    MapMode.SATELLITE -> "🛰️ تم تفعيل أقمار Esri القياسية"
                    MapMode.SATELLITE_CLARITY -> "💎 تم تفعيل أقمار Esri Clarity عالية الوضوح"
                    MapMode.OSM -> "🌐 تم تفعيل خريطة OSM القياسية"
                    MapMode.HOT -> "🗺️ تم تفعيل خريطة شوارع HOT"
                    MapMode.TOPO -> "⛰️ تم تفعيل خريطة التضاريس"
                    else -> "تم تفعيل الطبقة بنجاح ✓"
                }
                coroutineScope.launch { snackbarHostState.showSnackbar(modeLabel) }
            },
            is3DAvailable = is3DAvailable,
            is3DTerrainEnabled = state.is3DTerrainEnabled,
            onToggle3DTerrain = { enabled ->
                onPrepare3DToggle(enabled)
                state.is3DTerrainEnabled = enabled
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        if (enabled) "⛰️ تم تفعيل التضاريس ثلاثية الأبعاد وزاوية الكاميرا (62°)"
                        else "🗺️ تم العودة إلى العرض ثنائي الأبعاد (2D)"
                    )
                }
            },
            gibsEnabled = state.gibsEnabled,
            gibsProduct = state.gibsProduct,
            onToggleGibs = { enabled ->
                state.gibsEnabled = enabled
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        if (enabled) "📡 تم تشغيل بث ناسا اليومي المباشر (NASA GIBS)"
                        else "تم إيقاف بث ناسا المباشر"
                    )
                }
            },
            onSelectGibsProduct = { productKey ->
                state.gibsProduct = productKey
                state.gibsEnabled = true
                val prodName = when (productKey) {
                    "noaa20" -> "VIIRS NOAA-20 المباشر"
                    "modis" -> "MODIS الألوان الحقيقية"
                    "landtemp" -> "حرارة السطح النهاري"
                    else -> "VIIRS SNPP المباشر"
                }
                coroutineScope.launch { snackbarHostState.showSnackbar("📡 تم التبديل إلى بث ناسا: $prodName") }
            },
            realTimeSatEnabled = state.realTimeSatEnabled,
            realTimeSatFeed = state.realTimeSatFeed,
            onToggleRealTimeSat = { enabled ->
                state.realTimeSatEnabled = enabled
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        if (enabled) "🛰️ تم تشغيل البث الفضائي الحي في الوقت الفعلي (RealEarth)"
                        else "تم إيقاف البث الفضائي المباشر"
                    )
                }
            },
            onSelectRealTimeSatFeed = { feed ->
                state.realTimeSatFeed = feed
                state.realTimeSatEnabled = true
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        if (feed == "globalir") "🛰️ بث الأشعة تحت الحمراء المباشر (ليل/نهار)"
                        else "🛰️ بث الأقمار المرئية المباشر (RealEarth)"
                    )
                }
            },
            eoxEnabled = state.eoxEnabled,
            onToggleEox = { enabled ->
                state.eoxEnabled = enabled
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        if (enabled) "🌍 تم تفعيل صور EOX Sentinel-2 (بدون سحب)"
                        else "تم إيقاف طبقة EOX Sentinel-2"
                    )
                }
            },
            nightMode = state.nightMode,
            onToggleNight = { state.nightMode = it },
            quakesEnabled = state.quakesEnabled,
            onToggleQuakes = { state.quakesEnabled = it },
            rainEnabled = state.rainEnabled,
            onToggleRain = { state.rainEnabled = it },
            reliabilityEnabled = showZones || state.reliabilityEnabled,
            onToggleReliability = {
                state.reliabilityEnabled = it
                onSetShowZones(it)
            },
            peakEnabled = state.peakEnabled,
            onTogglePeak = { state.peakEnabled = it },
            coverageEnabled = state.coverageEnabled,
            onToggleCoverage = { state.coverageEnabled = it },
            hazardEnabled = state.hazardEnabled,
            onToggleHazard = { state.hazardEnabled = it },
            eumetEnabled = state.eumetEnabled,
            onToggleEumet = { state.eumetEnabled = it },
            eumetLayerName = state.eumetLayerName,
            onSelectEumetLayer = { state.eumetLayerName = it },
            onCoverageCleared = {
                onCoverageCleared()
                coroutineScope.launch { snackbarHostState.showSnackbar("✓ تم مسح سجل التغطية المحلي") }
            },
            initialTab = initialTab,
            adminRole = adminRole,
            isRegisteredUser = isRegisteredUser,
            isProvider = isProvider,
            liveTrafficEnabled = state.liveTrafficEnabled,
            onToggleLiveTraffic = { state.liveTrafficEnabled = it },
            enableLiveTrafficByAdmin = enableLiveTrafficByAdmin,
            onDismiss = {
                state.showLayerDialog = false
                onDismissMoreLayersSheet()
            }
        )
    }
}
