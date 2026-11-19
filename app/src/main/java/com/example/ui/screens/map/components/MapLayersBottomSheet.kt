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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.map.MapLayer
import com.example.ui.screens.map.MapMode
import com.example.utils.CoverageLog
import com.example.utils.NavConfig
import com.example.utils.ValhallaTileDownloader

/**
 * 🗺️ PrimaryLayersBottomSheet
 * قائمة مخصصة وحصرية للطبقات الأساسية فقط (بدون أي تبويبات أو طبقات ثانوية):
 * - وضع أوفلاين Valhalla
 * - عرض التضاريس ثلاثية الأبعاد (3D Terrain)
 * - خرائط الأساس (VELA المجانية، Esri Clarity، Esri القياسية، OSM، HOT، Topo)
 * - زر "تصغير" وإغلاق فوري + إخفاء تلقائي عند اختيار أي خريطة
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrimaryLayersBottomSheet(
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
    onDismiss: () -> Unit
) {
    val valhallaMb = if (valhallaSizeMb > 0) valhallaSizeMb else ValhallaTileDownloader.EXPECTED_INSTALLED_MB

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        contentColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.3f)) },
        modifier = Modifier.testTag("primary_layers_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // الرأس مع زر تصغير
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Layers,
                    contentDescription = null,
                    tint = Color(0xFFFBBF24),
                    modifier = Modifier.size(26.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "الطبقات الأساسية للخريطة",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "خرائط الأساس، وضع أوفلاين، والتضاريس 3D",
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
                        .testTag("primary_layers_minimize_btn")
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

            // 1. وضع التشغيل والمنظور ثلاثي الأبعاد
            SectionTitle("وضع التشغيل والمنظور ثلاثي الأبعاد", Color(0xFF10B981))

            SheetSwitchCard(
                icon = Icons.Default.OfflinePin,
                title = "خرائط بلا إنترنت",
                subtitle = if (isValhallaDownloaded) {
                    "حزمة اليمن جاهزة محلياً (~$valhallaMb ميجابايت) — توفير 100% للبيانات"
                } else {
                    "استعمل الخريطة المحفوظة على الجهاز (حزم المناطق)"
                },
                checked = mapLayer == MapLayer.VALHALLA_OFFLINE,
                onCheckedChange = { checked ->
                    onToggleMapLayer(if (checked) MapLayer.VALHALLA_OFFLINE else MapLayer.STANDARD)
                    onDismiss()
                },
                accentColor = Color(0xFF10B981),
                testTag = "sheet_valhalla_offline_switch"
            ) {
                if (!isValhallaDownloaded) {
                    Spacer(Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onOpenValhallaDownload()
                        },
                        border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("sheet_download_valhalla_btn")
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "حزمة التوجيه بلا إنترنت (${ValhallaTileDownloader.EXPECTED_DOWNLOAD_MB} ميجابايت)",
                            color = Color(0xFFFBBF24),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            SheetSwitchCard(
                icon = Icons.Default.Terrain,
                title = "عرض التضاريس ثلاثية الأبعاد (3D Terrain)",
                subtitle = if (is3DAvailable) {
                    "ارتفاعات الجبال وإمالة الكاميرا (62°) مع حماية تلقائية ضد الشاشة السوداء"
                } else {
                    "وضع المنظور المائل 3D التوافقي للخرائط"
                },
                checked = is3DTerrainEnabled,
                enabled = true,
                onCheckedChange = { enabled ->
                    onToggle3DTerrain(enabled)
                    onDismiss()
                },
                accentColor = Color(0xFFA78BFA),
                testTag = "sheet_3d_terrain_switch"
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

            // 2. خرائط الأساس والأقمار عالية الدقة
            SectionTitle("🗺️ خرائط الأساس والأقمار عالية الدقة", Color(0xFFFBBF24))

            val baseModes = listOf(
                Triple(MapMode.VELA, "✨ خرائط VELA", "خريطة شوارع حديثة وسريعة بتفاصيل واضحة"),
                Triple(MapMode.SATELLITE_CLARITY, "💎 أقمار فائقة الوضوح (Esri Clarity)", "صور فضائية عالية الحدة للمدن والطرق الجبلية"),
                Triple(MapMode.SATELLITE, "🛰️ أقمار صناعية (Esri القياسية)", "صور جوية عالمية عالية الدقة من Esri"),
                Triple(MapMode.OSM, "🌐 OpenStreetMap القياسية", "خريطة OSM الكاملة بأسماء الشوارع والمعالم"),
                Triple(MapMode.HOT, "🗺️ شوارع HOT الميدانية", "نمط ميداني واضح للطرق والأحياء اليمنية"),
                Triple(MapMode.TOPO, "⛰️ تضاريس OpenTopoMap", "خريطة جغرافية بخطوط الكنتور والارتفاعات")
            )

            baseModes.forEach { (mode, title, desc) ->
                val selected = currentMapMode == mode || (currentMapMode == MapMode.VELA_3D && mode == MapMode.SATELLITE)
                Surface(
                    onClick = {
                        onSelectMapMode(mode)
                        onDismiss()
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = if (selected) Color(0xFF10B981).copy(alpha = 0.18f) else Color(0xFF1E293B).copy(alpha = 0.55f),
                    border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) Color(0xFF10B981) else Color(0xFF334155)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 54.dp)
                        .testTag("sheet_base_${mode.name.lowercase()}_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                title,
                                color = if (selected) Color(0xFF10B981) else Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(desc, color = Color(0xFF94A3B8), fontSize = 11.sp)
                        }
                        if (selected) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "مفعّل", tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
        }
    }
}

/**
 * 🛰️ SecondaryLayersBottomSheet
 * قائمة مخصصة وحصرية للطبقات الثانوية فقط (بدون أي تبويبات أو طبقات أساسية):
 * - الأقمار الصناعية الحية المباشرة (UW RealEarth, NASA GIBS, EOX Sentinel-2)
 * - الوضع الليلي، الزلازل، الرادار، التحليل الأوفلاين، ومسح بيانات التغطية
 * - زر "تصغير" وإغلاق فوري + إخفاء تلقائي عند تفعيل/تبديل أي طبقة
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
            // الرأس مع زر تصغير
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

            // 1. أقمار صناعية ومناخ
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

            // 🛰️ قمر صناعي مباشر مجاني (Meteosat عبر EUMETSAT)
            var eumetLayers by remember { mutableStateOf<List<com.example.utils.WmsLayerInfo>?>(null) }
            var eumetMenu by remember { mutableStateOf(false) }
            LaunchedEffect(eumetEnabled) {
                if (eumetEnabled && eumetLayers == null) {
                    eumetLayers = com.example.utils.EumetViewCatalog.iodcLayers() ?: emptyList()
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

            // 2. طبقات الطقس والمخاطر والتحليل الأوفلاين
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
            SheetSwitchCard(
                icon = Icons.Default.Cloud,
                title = "رادار الأمطار (RainViewer)",
                subtitle = if (NavConfig.ENABLE_RAINVIEWER) "تغطية السحب والأمطار الأخيرة" else "معطّلة افتراضياً (بدون مفاتيح مدفوعة)",
                checked = rainEnabled,
                enabled = NavConfig.ENABLE_RAINVIEWER,
                onCheckedChange = { onToggleRain(it); onDismiss() },
                accentColor = Color(0xFF38BDF8),
                testTag = "layer_rain_switch"
            )
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
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            containerColor = Color(0xFF0F172A),
            title = { Text("مسح بيانات التغطية؟", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("سيتم حذف سجل مناطق ضعف الشبكة المحفوظ محلياً على جهازك.", color = Color(0xFFCBD5E1)) },
            confirmButton = {
                TextButton(onClick = {
                    CoverageLog.clear(context)
                    cellCount = 0
                    showClearConfirm = false
                    onCoverageCleared()
                }) { Text("مسح", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("إلغاء", color = Color(0xFF94A3B8)) }
            }
        )
    }
}

/**
 * 🗺️ UnifiedLayersSheet
 * نافذة موحدة بـ 3 تبويبات واضحة ومهنية:
 * [الخريطة] • [أقمار حية] • [تحليل]
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
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(initialTab) }
    val valhallaMb = if (valhallaSizeMb > 0) valhallaSizeMb else ValhallaTileDownloader.EXPECTED_INSTALLED_MB
    var cellCount by remember { mutableIntStateOf(CoverageLog.cellCount(context)) }
    var showClearConfirm by remember { mutableStateOf(false) }

    var eumetLayers by remember { mutableStateOf<List<com.example.utils.WmsLayerInfo>?>(null) }
    var eumetMenu by remember { mutableStateOf(false) }
    LaunchedEffect(eumetEnabled) {
        if (eumetEnabled && eumetLayers == null) {
            eumetLayers = com.example.utils.EumetViewCatalog.iodcLayers() ?: emptyList()
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
            // الرأس مع زر تصغير
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

            // 🌟 3 تبويبات رئيسية
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
                    0 -> {
                        // 1. الخريطة (الأساسية) — شرائح للأساس + صفوف مسطّحة
                        SectionTitle("خريطة الأساس", Color(0xFF10B981))

                        val baseModes = listOf(
                            Triple(MapMode.HOT, "🗺️ شوارع", "HOT الميدانية"),
                            Triple(MapMode.SATELLITE, "🛰️ أقمار", "Esri القياسية"),
                            Triple(MapMode.SATELLITE_CLARITY, "💎 وضوح", "Esri Clarity"),
                            Triple(MapMode.VELA, "✨ VELA", "خريطة الشوارع"),
                            Triple(MapMode.OSM, "🌐 قياسي", "OpenStreetMap"),
                            Triple(MapMode.TOPO, "⛰️ تضاريس", "OpenTopoMap")
                        )

                        @OptIn(ExperimentalLayoutApi::class)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            baseModes.forEach { (mode, label, _) ->
                                val selected = currentMapMode == mode
                                FilterChip(
                                    selected = selected,
                                    onClick = { onSelectMapMode(mode); onDismiss() },
                                    label = {
                                        Text(
                                            label,
                                            fontSize = 12.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = Color(0xFF1E293B),
                                        labelColor = Color(0xFFCBD5E1),
                                        selectedContainerColor = Color(0xFF10B981).copy(alpha = 0.22f),
                                        selectedLabelColor = Color(0xFF10B981)
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = selected,
                                        borderColor = Color(0xFF334155),
                                        selectedBorderColor = Color(0xFF10B981)
                                    )
                                )
                            }
                        }

                        HorizontalDivider(color = Color.White.copy(alpha = 0.06f))

                        LayerRow(
                            title = "خرائط بلا إنترنت",
                            subtitle = if (isValhallaDownloaded) "حزمة اليمن جاهزة محلياً (~$valhallaMb ميجابايت)" else "استعمل الخريطة المحفوظة على الجهاز (حزم المناطق)",
                            checked = mapLayer == MapLayer.VALHALLA_OFFLINE,
                            onChange = { checked ->
                                onToggleMapLayer(if (checked) MapLayer.VALHALLA_OFFLINE else MapLayer.STANDARD)
                                onDismiss()
                            },
                            testTag = "unified_valhalla_switch"
                        )

                        if (!isValhallaDownloaded) {
                            OutlinedButton(
                                onClick = { onDismiss(); onOpenValhallaDownload() },
                                border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.6f)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).heightIn(min = 44.dp)
                            ) {
                                Icon(Icons.Default.CloudDownload, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "حزمة التوجيه بلا إنترنت (${ValhallaTileDownloader.EXPECTED_DOWNLOAD_MB} ميجابايت)",
                                    color = Color(0xFFFBBF24),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        LayerRow(
                            title = "التضاريس ثلاثية الأبعاد (3D)",
                            subtitle = if (is3DAvailable) "ارتفاعات الجبال وإمالة الكاميرا (62°)" else "وضع المنظور المائل 3D للخرائط",
                            checked = is3DTerrainEnabled,
                            onChange = { enabled ->
                                onToggle3DTerrain(enabled)
                                onDismiss()
                            },
                            testTag = "unified_3d_terrain_switch"
                        )

                        LayerRow(
                            title = "الوضع الليلي للخريطة",
                            subtitle = "تعتيم مريح للعين أثناء القيادة ليلاً",
                            checked = nightMode,
                            onChange = { onToggleNight(it); onDismiss() },
                            testTag = "unified_night_switch"
                        )
                    }

                    1 -> {
                        // 2. أقمار حية — صفوف مسطّحة
                        SectionTitle("🛰️ أقمار صناعية مباشرة ويومية", Color(0xFF10B981))

                        LayerRow(
                            title = "قمر Meteosat مباشر (EUMETSAT)",
                            subtitle = "يتجدد كل ~15 دقيقة • سحب وغبار فوق اليمن • دقة ≈ 3 كم",
                            checked = eumetEnabled,
                            onChange = { onToggleEumet(it) },
                            testTag = "unified_eumet_switch"
                        )

                        if (eumetEnabled) {
                            val list = eumetLayers
                            when {
                                list == null -> LinearProgressIndicator(Modifier.fillMaxWidth().padding(vertical = 4.dp), color = Color(0xFF10B981))
                                list.isNotEmpty() -> {
                                    if (eumetLayerName.isEmpty()) {
                                        val defaultLayer = (list.firstOrNull { it.title.contains("dust", true) } ?: list.first()).name
                                        onSelectEumetLayer(defaultLayer)
                                    }
                                    Box(Modifier.padding(vertical = 4.dp)) {
                                        OutlinedButton(
                                            onClick = { eumetMenu = true },
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF10B981)),
                                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f))
                                        ) {
                                            Text(list.firstOrNull { it.name == eumetLayerName }?.arabicLabel() ?: "اختر طبقة", fontWeight = FontWeight.Bold)
                                            Spacer(Modifier.width(6.dp))
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                        }
                                        DropdownMenu(expanded = eumetMenu, onDismissRequest = { eumetMenu = false }, containerColor = Color(0xFF0F172A)) {
                                            list.forEach { l ->
                                                DropdownMenuItem(
                                                    text = { Text(l.arabicLabel(), color = Color.White) },
                                                    onClick = { onSelectEumetLayer(l.name); eumetMenu = false }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (NavConfig.ENABLE_REALEARTH) {
                            LayerRow(
                                title = "أقمار الطقس والأرض (UW RealEarth)",
                                subtitle = "بث فضائي محدث كل 15-30 دقيقة",
                                checked = realTimeSatEnabled,
                                onChange = { onToggleRealTimeSat(it); onDismiss() },
                                testTag = "unified_realearth_switch"
                            )
                        }

                        LayerRow(
                            title = "صور ناسا اليومية (NASA GIBS)",
                            subtitle = "صور NASA EOSDIS اليومية (VIIRS 375م و MODIS)",
                            checked = gibsEnabled,
                            onChange = { onToggleGibs(it); onDismiss() },
                            testTag = "unified_gibs_switch"
                        )

                        LayerRow(
                            title = "صور EOX Sentinel-2 (2016)",
                            subtitle = "طبقة فضائية أوروبية صافية تماماً (CC BY 4.0)",
                            checked = eoxEnabled,
                            onChange = { onToggleEox(it); onDismiss() },
                            testTag = "unified_eox_switch"
                        )

                        LayerRow(
                            title = "زلازل USGS (≥ 2.5 آخر أسبوع)",
                            subtitle = "عرض الهزات المسجلة رسمياً مع تفاصيل النقطة",
                            checked = quakesEnabled,
                            onChange = { onToggleQuakes(it); onDismiss() },
                            testTag = "unified_quakes_switch"
                        )
                    }

                    2 -> {
                        // 3. تحليل — صفوف مسطّحة
                        SectionTitle("📊 أدوات المراقبة وتحليل الطريق", Color(0xFF10B981))

                        LayerRow(
                            title = "رادار الأمطار (RainViewer)",
                            subtitle = if (NavConfig.ENABLE_RAINVIEWER) "تغطية السحب والأمطار الأخيرة" else "معطّلة افتراضياً",
                            checked = rainEnabled,
                            enabled = NavConfig.ENABLE_RAINVIEWER,
                            onChange = { onToggleRain(it); onDismiss() },
                            testTag = "unified_rain_switch"
                        )

                        LayerRow(
                            title = "مناطق توفر وموثوقية الخدمات",
                            subtitle = "توزيع الفنيين والمتاجر المتاحين حولك",
                            checked = reliabilityEnabled,
                            onChange = { onToggleReliability(it); onDismiss() },
                            testTag = "unified_reliability_switch"
                        )

                        LayerRow(
                            title = "التنبؤ بساعات الذروة والطلب",
                            subtitle = "تحليل محلي لسجل الحجوزات حسب الوقت",
                            checked = peakEnabled,
                            onChange = { onTogglePeak(it); onDismiss() },
                            testTag = "unified_peak_switch"
                        )

                        LayerRow(
                            title = "مناطق ضعف الشبكة المتعلمة محلياً",
                            subtitle = "نقاط الانقطاع المرصودة على جهازك ($cellCount خلية)",
                            checked = coverageEnabled,
                            onChange = { onToggleCoverage(it); onDismiss() },
                            testTag = "unified_coverage_switch"
                        )

                        LayerRow(
                            title = "مناطق وعورة الطريق والمنحدرات",
                            subtitle = "إبراز العقبات والنقائل الصعبة مع تنبيه مسبق",
                            checked = hazardEnabled,
                            onChange = { onToggleHazard(it); onDismiss() },
                            testTag = "unified_hazard_switch"
                        )

                        Spacer(Modifier.height(6.dp))
                        OutlinedButton(
                            onClick = { showClearConfirm = true },
                            enabled = cellCount > 0,
                            border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth().height(46.dp)
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = Color(0xFFFCA5A5), modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("مسح بيانات التغطية ($cellCount)", color = Color(0xFFFCA5A5), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            containerColor = Color(0xFF0F172A),
            title = { Text("مسح بيانات التغطية؟", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("سيتم حذف سجل مناطق ضعف الشبكة المحفوظ محلياً على جهازك.", color = Color(0xFFCBD5E1)) },
            confirmButton = {
                TextButton(onClick = {
                    CoverageLog.clear(context)
                    cellCount = 0
                    showClearConfirm = false
                    onCoverageCleared()
                }) { Text("مسح", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("إلغاء", color = Color(0xFF94A3B8)) }
            }
        )
    }
}

@Composable
private fun SectionTitle(text: String, color: Color) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = Modifier.padding(top = 2.dp)
    )
}

@Composable
private fun LayerRow(
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
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                color = Color(0xFF94A3B8),
                fontSize = 11.sp,
                lineHeight = 15.sp,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
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
private fun SheetSwitchCard(
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
