package com.example.ui.screens.map.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.utils.CoverageLog
import com.example.utils.EumetViewCatalog
import com.example.utils.NavConfig
import com.example.utils.WmsLayerInfo

/**
 * 🛰️ SecondaryLayersBottomSheet
 * قائمة مخصصة وحصرية للطبقات الثانوية فقط (بدون أي تبويبات أو طبقات أساسية):
 * - الأقمار الصناعية الحية المباشرة (UW RealEarth, NASA GIBS, EOX Sentinel-2, EUMETSAT)
 * - الوضع الليلي، الزلازل، الرادار، التحليل الأوفلاين، ومسح بيانات التغطية
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecondaryLayersBottomSheet(
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
    onCoverageCleared: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var showClearConfirm by remember { mutableStateOf(false) }
    var cellCount by remember { mutableIntStateOf(CoverageLog.cellCount(context)) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        contentColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.3f)) },
        modifier = Modifier.testTag("secondary_layers_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SatelliteAlt,
                    contentDescription = null,
                    tint = Color(0xFF00E5FF),
                    modifier = Modifier.size(26.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "الطبقات الثانوية والأقمار الحية",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "بث الأقمار الحية، الطقس، والتحليل الأوفلاين",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
                Surface(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E293B),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("secondary_layers_minimize_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "تصغير وإغلاق",
                            tint = Color(0xFFCBD5E1),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "تصغير",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFCBD5E1)
                        )
                    }
                }
            }

            SectionTitle("🛰️ صور الأقمار الصناعية", Color(0xFF00E5FF))

            if (NavConfig.ENABLE_REALEARTH) {
                SheetSwitchCard(
                    icon = Icons.Default.Public,
                    title = "أقمار الطقس والأرض الحية (UW RealEarth)",
                    subtitle = "تغطية فضائية محدّثة كل 15-30 دقيقة",
                    checked = realTimeSatEnabled,
                    onCheckedChange = { enabled ->
                        onToggleRealTimeSat(enabled)
                        onDismiss()
                    },
                    accentColor = Color(0xFF00E5FF),
                    testTag = "sheet_realtime_sat_switch"
                ) {
                    if (realTimeSatEnabled) {
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                "globalvis" to "مرئي مباشر (سحب/أرض)",
                                "globalir" to "أشعة تحت حمراء (ليل/نهار)"
                            ).forEach { (key, label) ->
                                val selected = realTimeSatFeed == key
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        onSelectRealTimeSatFeed(key)
                                        onDismiss()
                                    },
                                    label = { Text(label, fontSize = 10.5.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = Color(0xFF0F172A),
                                        labelColor = Color(0xFFCBD5E1),
                                        selectedContainerColor = Color(0xFF00E5FF).copy(alpha = 0.2f),
                                        selectedLabelColor = Color(0xFF00E5FF)
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            SheetSwitchCard(
                icon = Icons.Default.SatelliteAlt,
                title = "صور ناسا اليومية (NASA GIBS)",
                subtitle = "صور NASA EOSDIS اليومية (VIIRS 375م و MODIS وحرارة السطح)",
                checked = gibsEnabled,
                onCheckedChange = { enabled ->
                    onToggleGibs(enabled)
                    onDismiss()
                },
                accentColor = Color(0xFF38BDF8),
                testTag = "sheet_gibs_switch"
            ) {
                if (gibsEnabled) {
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "truecolor" to "VIIRS SNPP",
                            "noaa20" to "NOAA-20",
                            "modis" to "MODIS",
                            "landtemp" to "حرارة"
                        ).forEach { (key, label) ->
                            val selected = gibsProduct == key
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    onSelectGibsProduct(key)
                                    onDismiss()
                                },
                                label = { Text(label, fontSize = 10.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium) },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = Color(0xFF0F172A),
                                    labelColor = Color(0xFFCBD5E1),
                                    selectedContainerColor = Color(0xFF38BDF8).copy(alpha = 0.2f),
                                    selectedLabelColor = Color(0xFF38BDF8)
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            SheetSwitchCard(
                icon = Icons.Default.Brightness7,
                title = "صور EOX Sentinel-2 (2016 بدون سحب)",
                subtitle = "طبقة فضائية أوروبية CC BY 4.0 مع تخزين محلي تلقائي",
                checked = eoxEnabled,
                onCheckedChange = { enabled ->
                    onToggleEox(enabled)
                    onDismiss()
                },
                accentColor = Color(0xFF38BDF8),
                testTag = "sheet_eox_switch"
            )

            var eumetLayers by remember { mutableStateOf<List<WmsLayerInfo>?>(null) }
            var eumetMenu by remember { mutableStateOf(false) }
            LaunchedEffect(eumetEnabled) {
                if (eumetEnabled && eumetLayers == null) {
                    eumetLayers = EumetViewCatalog.iodcLayers() ?: emptyList()
                }
            }

            SheetSwitchCard(
                icon = Icons.Default.Satellite,
                title = "قمر Meteosat مباشر (EUMETSAT)",
                subtitle = "يتجدد كل ~15 دقيقة • سحب وغبار فوق اليمن • دقة ≈ 3 كم",
                checked = eumetEnabled,
                onCheckedChange = { onToggleEumet(it) },
                accentColor = Color(0xFF10B981),
                testTag = "sheet_eumet_switch"
            )

            if (eumetEnabled) {
                val list = eumetLayers
                when {
                    list == null -> {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            color = Color(0xFF10B981)
                        )
                    }
                    list.isEmpty() -> {
                        Text(
                            "تعذر جلب قائمة الطبقات (تحتاج إنترنت).",
                            color = Color(0xFFF59E0B),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                    else -> {
                        if (eumetLayerName.isEmpty()) {
                            val defaultLayer = (list.firstOrNull { it.title.contains("dust", true) } ?: list.first()).name
                            onSelectEumetLayer(defaultLayer)
                        }
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                            OutlinedButton(
                                onClick = { eumetMenu = true },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF10B981)),
                                border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f))
                            ) {
                                Text(
                                    list.firstOrNull { it.name == eumetLayerName }?.arabicLabel() ?: "اختر طبقة",
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.width(6.dp))
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                            DropdownMenu(
                                expanded = eumetMenu,
                                onDismissRequest = { eumetMenu = false },
                                containerColor = Color(0xFF0F172A)
                            ) {
                                list.forEach { l ->
                                    DropdownMenuItem(
                                        text = { Text(l.arabicLabel(), color = Color.White) },
                                        onClick = {
                                            onSelectEumetLayer(l.name)
                                            eumetMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

            SectionTitle("طبقات الطقس والمخاطر والتحليل الأوفلاين", Color(0xFF94A3B8))

            SheetSwitchCard(
                icon = Icons.Default.Bedtime,
                title = "الوضع الليلي للخريطة",
                subtitle = "حماية العين ليلاً مع عكس مريح لخرائط الشوارع",
                checked = nightMode,
                onCheckedChange = { onToggleNight(it); onDismiss() },
                accentColor = Color(0xFFA78BFA),
                testTag = "layer_night_switch"
            )
            SheetSwitchCard(
                icon = Icons.Default.WarningAmber,
                title = "زلازل USGS (≥ 2.5 آخر أسبوع)",
                subtitle = "عرض الهزات المسجلة عالمياً مجاناً مع تفاصيل كل نقطة",
                checked = quakesEnabled,
                onCheckedChange = { onToggleQuakes(it); onDismiss() },
                accentColor = Color(0xFFF59E0B),
                testTag = "layer_quakes_switch"
            )
            if (NavConfig.ENABLE_RAINVIEWER) {
                SheetSwitchCard(
                    icon = Icons.Default.Cloud,
                    title = "رادار الأمطار (RainViewer)",
                    subtitle = "تغطية السحب والأمطار الأخيرة",
                    checked = rainEnabled,
                    enabled = true,
                    onCheckedChange = { onToggleRain(it); onDismiss() },
                    accentColor = Color(0xFF38BDF8),
                    testTag = "layer_rain_switch"
                )
            }
            SheetSwitchCard(
                icon = Icons.Default.PieChart,
                title = "مناطق توفر وموثوقية الخدمات",
                subtitle = "توزيع الفنيين والمتاجر المتاحين حولك",
                checked = reliabilityEnabled,
                onCheckedChange = { onToggleReliability(it); onDismiss() },
                accentColor = Color(0xFF10B981),
                testTag = "layer_reliability_switch"
            )
            SheetSwitchCard(
                icon = Icons.Default.TrendingUp,
                title = "التنبؤ بساعات الذروة والطلب",
                subtitle = "تحليل محلي لسجل الحجوزات حسب الوقت",
                checked = peakEnabled,
                onCheckedChange = { onTogglePeak(it); onDismiss() },
                accentColor = Color(0xFF00E5FF),
                testTag = "layer_peak_switch"
            )
            SheetSwitchCard(
                icon = Icons.Default.SignalCellularOff,
                title = "مناطق ضعف الشبكة المتعلمة محلياً",
                subtitle = "نقاط الانقطاع المرصودة ذاتياً على جهازك ($cellCount خلية)",
                checked = coverageEnabled,
                onCheckedChange = { onToggleCoverage(it); onDismiss() },
                accentColor = Color(0xFFF59E0B),
                testTag = "layer_coverage_switch"
            )
            SheetSwitchCard(
                icon = Icons.Default.Warning,
                title = "مناطق وعورة الطريق والمنحدرات",
                subtitle = "إبراز العقبات والنقائل الصعبة مع تنبيه صوتي مسبق",
                checked = hazardEnabled,
                onCheckedChange = { onToggleHazard(it); onDismiss() },
                accentColor = Color(0xFFEF4444),
                testTag = "layer_hazard_switch"
            )

            OutlinedButton(
                onClick = { showClearConfirm = true },
                enabled = cellCount > 0,
                border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("clear_coverage_data_btn")
            ) {
                Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = Color(0xFFFCA5A5), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("مسح بيانات التغطية ($cellCount)", color = Color(0xFFFCA5A5), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(10.dp))
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
internal fun UnifiedLiveSatTabContent(
    eumetEnabled: Boolean,
    onToggleEumet: (Boolean) -> Unit,
    eumetLayers: List<WmsLayerInfo>?,
    eumetLayerName: String,
    onSelectEumetLayer: (String) -> Unit,
    eumetMenu: Boolean,
    onEumetMenuChange: (Boolean) -> Unit,
    realTimeSatEnabled: Boolean,
    onToggleRealTimeSat: (Boolean) -> Unit,
    gibsEnabled: Boolean,
    onToggleGibs: (Boolean) -> Unit,
    eoxEnabled: Boolean,
    onToggleEox: (Boolean) -> Unit,
    quakesEnabled: Boolean,
    onToggleQuakes: (Boolean) -> Unit,
    isVisible: (String) -> Boolean,
    orderOf: (String, Int) -> Int,
    labelOf: (String, String) -> String,
    onDismiss: () -> Unit
) {
    SectionTitle("🛰️ أقمار صناعية مباشرة ويومية", Color(0xFF10B981))

    val satRows = listOf("EUMETSAT", "REALEARTH", "NASA_GIBS", "EOX_SENTINEL", "USGS_QUAKES")
        .filter { isVisible(it) }
        .sortedBy { orderOf(it, 99) }

    satRows.forEach { layerId ->
        when (layerId) {
            "EUMETSAT" -> {
                LayerRow(
                    title = labelOf("EUMETSAT", "قمر Meteosat مباشر (EUMETSAT)"),
                    subtitle = "يتجدد كل ~15 دقيقة • سحب وغبار فوق اليمن • دقة ≈ 3 كم",
                    checked = eumetEnabled,
                    onChange = { onToggleEumet(it) },
                    testTag = "unified_eumet_switch"
                )

                if (eumetEnabled) {
                    when {
                        eumetLayers == null -> LinearProgressIndicator(Modifier.fillMaxWidth().padding(vertical = 4.dp), color = Color(0xFF10B981))
                        eumetLayers.isNotEmpty() -> {
                            if (eumetLayerName.isEmpty()) {
                                val defaultLayer = (eumetLayers.firstOrNull { it.title.contains("dust", true) } ?: eumetLayers.first()).name
                                onSelectEumetLayer(defaultLayer)
                            }
                            Box(Modifier.padding(vertical = 4.dp)) {
                                OutlinedButton(
                                    onClick = { onEumetMenuChange(true) },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF10B981)),
                                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f))
                                ) {
                                    Text(eumetLayers.firstOrNull { it.name == eumetLayerName }?.arabicLabel() ?: "اختر طبقة", fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.width(6.dp))
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                                DropdownMenu(expanded = eumetMenu, onDismissRequest = { onEumetMenuChange(false) }, containerColor = Color(0xFF0F172A)) {
                                    eumetLayers.forEach { l ->
                                        DropdownMenuItem(
                                            text = { Text(l.arabicLabel(), color = Color.White) },
                                            onClick = {
                                                onSelectEumetLayer(l.name)
                                                onEumetMenuChange(false)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            "REALEARTH" -> if (NavConfig.ENABLE_REALEARTH) {
                LayerRow(
                    title = labelOf("REALEARTH", "أقمار الطقس والأرض (UW RealEarth)"),
                    subtitle = "بث فضائي محدث كل 15-30 دقيقة",
                    checked = realTimeSatEnabled,
                    onChange = { onToggleRealTimeSat(it); onDismiss() },
                    testTag = "unified_realearth_switch"
                )
            }
            "NASA_GIBS" -> {
                LayerRow(
                    title = labelOf("NASA_GIBS", "صور ناسا اليومية (NASA GIBS)"),
                    subtitle = "صور NASA EOSDIS اليومية (VIIRS 375م و MODIS)",
                    checked = gibsEnabled,
                    onChange = { onToggleGibs(it); onDismiss() },
                    testTag = "unified_gibs_switch"
                )
            }
            "EOX_SENTINEL" -> {
                LayerRow(
                    title = labelOf("EOX_SENTINEL", "صور EOX Sentinel-2 (2016)"),
                    subtitle = "طبقة فضائية أوروبية صافية تماماً (CC BY 4.0)",
                    checked = eoxEnabled,
                    onChange = { onToggleEox(it); onDismiss() },
                    testTag = "unified_eox_switch"
                )
            }
            "USGS_QUAKES" -> {
                LayerRow(
                    title = labelOf("USGS_QUAKES", "زلازل USGS (≥ 2.5 آخر أسبوع)"),
                    subtitle = "عرض الهزات المسجلة رسمياً مع تفاصيل النقطة",
                    checked = quakesEnabled,
                    onChange = { onToggleQuakes(it); onDismiss() },
                    testTag = "unified_quakes_switch"
                )
            }
        }
    }
}
