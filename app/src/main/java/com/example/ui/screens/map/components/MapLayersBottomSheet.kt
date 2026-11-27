package com.example.ui.screens.map.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.map.MapLayer
import com.example.ui.screens.map.MapMode
import com.example.utils.CoverageLog
import com.example.utils.EumetViewCatalog
import com.example.utils.ValhallaTileDownloader
import com.example.utils.WmsLayerInfo

/**
 * 🗺️ UnifiedLayersSheet
 * نافذة موحدة بـ 3 تبويبات واضحة ومهنية:
 * [الخريطة] • [أقمار حية] • [تحليل]
 * (مفككة إلى بطاقات مستقلة: PrimaryMapLayersSheet، SatelliteWeatherLayersSheet، AnalysisRadarLayersSheet)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedLayersSheet(
    mapLayer: MapLayer,
    onToggleMapLayer: (MapLayer) -> Unit,
    isValhallaDownloaded: Boolean,
    valhallaSizeMb: Int,
    onOpenValhallaDownload: () -> Unit,
    currentMapMode: MapMode,
    onSelectMapMode: (MapMode) -> Unit,
    is3DAvailable: Boolean,
    is3DTerrainEnabled: Boolean,
    onToggle3DTerrain: (Boolean) -> Unit,
    gibsEnabled: Boolean,
    gibsProduct: String,
    onToggleGibs: (Boolean) -> Unit,
    onSelectGibsProduct: (String) -> Unit,
    realTimeSatEnabled: Boolean,
    realTimeSatFeed: String,
    onToggleRealTimeSat: (Boolean) -> Unit,
    onSelectRealTimeSatFeed: (String) -> Unit,
    eoxEnabled: Boolean,
    onToggleEox: (Boolean) -> Unit,
    nightMode: Boolean,
    onToggleNight: (Boolean) -> Unit,
    quakesEnabled: Boolean,
    onToggleQuakes: (Boolean) -> Unit,
    rainEnabled: Boolean,
    onToggleRain: (Boolean) -> Unit,
    reliabilityEnabled: Boolean,
    onToggleReliability: (Boolean) -> Unit,
    peakEnabled: Boolean,
    onTogglePeak: (Boolean) -> Unit,
    coverageEnabled: Boolean,
    onToggleCoverage: (Boolean) -> Unit,
    hazardEnabled: Boolean,
    onToggleHazard: (Boolean) -> Unit,
    eumetEnabled: Boolean = false,
    onToggleEumet: (Boolean) -> Unit = {},
    eumetLayerName: String = "",
    onSelectEumetLayer: (String) -> Unit = {},
    onCoverageCleared: () -> Unit = {},
    initialTab: Int = 0,
    adminRole: String = "GUEST",
    isRegisteredUser: Boolean = false,
    isProvider: Boolean = false,
    liveTrafficEnabled: Boolean = true,
    onToggleLiveTraffic: (Boolean) -> Unit = {},
    enableLiveTrafficByAdmin: Boolean = true,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(initialTab) }
    val valhallaMb = if (valhallaSizeMb > 0) valhallaSizeMb else ValhallaTileDownloader.EXPECTED_INSTALLED_MB
    var cellCount by remember { mutableIntStateOf(CoverageLog.cellCount(context)) }
    var showClearConfirm by remember { mutableStateOf(false) }

    fun isVisible(id: String): Boolean = true
    fun orderOf(id: String, fallback: Int): Int = fallback
    fun labelOf(id: String, fallback: String): String = fallback

    var eumetLayers by remember { mutableStateOf<List<WmsLayerInfo>?>(null) }
    var eumetMenu by remember { mutableStateOf(false) }
    LaunchedEffect(eumetEnabled) {
        if (eumetEnabled && eumetLayers == null) {
            eumetLayers = EumetViewCatalog.iodcLayers() ?: emptyList()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        contentColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.3f)) },
        modifier = Modifier.testTag("unified_layers_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Layers,
                    contentDescription = null,
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(26.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "طبقات الخريطة والمراقبة الحية",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "خرائط الأساس، الأقمار المباشرة، وأدوات التحليل",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
                Surface(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E293B),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier.heightIn(min = 40.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "تصغير", tint = Color(0xFFCBD5E1), modifier = Modifier.size(18.dp))
                        Text("إغلاق", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFCBD5E1))
                    }
                }
            }

            SecondaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF1E293B),
                contentColor = Color(0xFF10B981)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("الخريطة 🗺️", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp) },
                    selectedContentColor = Color(0xFF10B981),
                    unselectedContentColor = Color(0xFF94A3B8)
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("أقمار حية 🛰️", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp) },
                    selectedContentColor = Color(0xFF10B981),
                    unselectedContentColor = Color(0xFF94A3B8)
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("تحليل 📊", fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp) },
                    selectedContentColor = Color(0xFF10B981),
                    unselectedContentColor = Color(0xFF94A3B8)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                when (selectedTab) {
                    0 -> UnifiedMapBaseTabContent(
                        currentMapMode = currentMapMode,
                        onSelectMapMode = onSelectMapMode,
                        mapLayer = mapLayer,
                        onToggleMapLayer = onToggleMapLayer,
                        isValhallaDownloaded = isValhallaDownloaded,
                        valhallaMb = valhallaMb,
                        onOpenValhallaDownload = onOpenValhallaDownload,
                        is3DAvailable = is3DAvailable,
                        is3DTerrainEnabled = is3DTerrainEnabled,
                        onToggle3DTerrain = onToggle3DTerrain,
                        nightMode = nightMode,
                        onToggleNight = onToggleNight,
                        adminRole = adminRole,
                        isRegisteredUser = isRegisteredUser,
                        isProvider = isProvider,
                        isVisible = ::isVisible,
                        orderOf = ::orderOf,
                        labelOf = ::labelOf,
                        onDismiss = onDismiss
                    )

                    1 -> UnifiedLiveSatTabContent(
                        eumetEnabled = eumetEnabled,
                        onToggleEumet = onToggleEumet,
                        eumetLayers = eumetLayers,
                        eumetLayerName = eumetLayerName,
                        onSelectEumetLayer = onSelectEumetLayer,
                        eumetMenu = eumetMenu,
                        onEumetMenuChange = { eumetMenu = it },
                        realTimeSatEnabled = realTimeSatEnabled,
                        onToggleRealTimeSat = onToggleRealTimeSat,
                        gibsEnabled = gibsEnabled,
                        onToggleGibs = onToggleGibs,
                        eoxEnabled = eoxEnabled,
                        onToggleEox = onToggleEox,
                        quakesEnabled = quakesEnabled,
                        onToggleQuakes = onToggleQuakes,
                        isVisible = ::isVisible,
                        orderOf = ::orderOf,
                        labelOf = ::labelOf,
                        onDismiss = onDismiss
                    )

                    2 -> UnifiedAnalysisRadarTabContent(
                        enableLiveTrafficByAdmin = enableLiveTrafficByAdmin,
                        liveTrafficEnabled = liveTrafficEnabled,
                        onToggleLiveTraffic = onToggleLiveTraffic,
                        rainEnabled = rainEnabled,
                        onToggleRain = onToggleRain,
                        reliabilityEnabled = reliabilityEnabled,
                        onToggleReliability = onToggleReliability,
                        peakEnabled = peakEnabled,
                        onTogglePeak = onTogglePeak,
                        coverageEnabled = coverageEnabled,
                        onToggleCoverage = onToggleCoverage,
                        hazardEnabled = hazardEnabled,
                        onToggleHazard = onToggleHazard,
                        cellCount = cellCount,
                        onRequestClearCoverage = { showClearConfirm = true },
                        isVisible = ::isVisible,
                        orderOf = ::orderOf,
                        labelOf = ::labelOf,
                        onDismiss = onDismiss
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (showClearConfirm) {
        CoverageClearConfirmDialog(
            onConfirm = {
                CoverageLog.clear(context)
                cellCount = 0
                showClearConfirm = false
                onCoverageCleared()
            },
            onDismiss = { showClearConfirm = false }
        )
    }
}

@Composable
internal fun SectionTitle(text: String, color: Color) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = Modifier.padding(top = 2.dp)
    )
}

@Composable
internal fun LayerRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onChange: (Boolean) -> Unit,
    testTag: String = ""
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .padding(vertical = 4.dp)
            .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                color = if (enabled) Color.White else Color(0xFF64748B),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                color = Color(0xFF94A3B8),
                fontSize = 11.sp,
                lineHeight = 15.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = if (enabled) onChange else null,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF10B981),
                uncheckedThumbColor = Color(0xFF94A3B8),
                uncheckedTrackColor = Color(0xFF1E293B),
                uncheckedBorderColor = Color(0xFF475569)
            )
        )
    }
    HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
}

@Composable
internal fun SheetSwitchCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    accentColor: Color,
    testTag: String,
    enabled: Boolean = true,
    extraContent: @Composable ColumnScope.() -> Unit = {}
) {
    Surface(
        onClick = { if (enabled) onCheckedChange(!checked) },
        shape = RoundedCornerShape(14.dp),
        color = if (checked) accentColor.copy(alpha = 0.14f) else Color(0xFF1E293B).copy(alpha = 0.5f),
        border = if (checked) BorderStroke(1.2.dp, accentColor.copy(alpha = 0.65f)) else BorderStroke(1.dp, Color(0xFF334155)),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .testTag(testTag)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (!enabled) Color(0xFF64748B) else if (checked) accentColor else Color(0xFFCBD5E1),
                    modifier = Modifier.size(22.dp)
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        title,
                        color = if (!enabled) Color(0xFF64748B) else if (checked) accentColor else Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(subtitle, color = Color(0xFF94A3B8), fontSize = 11.sp)
                }
                Switch(
                    checked = checked,
                    onCheckedChange = if (enabled) onCheckedChange else null,
                    enabled = enabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF10B981),
                        uncheckedThumbColor = Color(0xFF94A3B8),
                        uncheckedTrackColor = Color(0xFF1E293B),
                        uncheckedBorderColor = Color(0xFF475569)
                    )
                )
            }
            extraContent()
        }
    }
}
