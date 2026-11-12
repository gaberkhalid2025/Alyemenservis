package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.screens.map.MapMode
import com.example.ui.screens.map.RealLeafletMapView
import com.example.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

data class PickedLocationResult(
    val latitude: Double,
    val longitude: Double,
    val label: String,
    val landmarkName: String = "",
    val landmarkLat: Double = 0.0,
    val landmarkLng: Double = 0.0,
    val note: String = "",
    val formattedAddress: String = ""
)

fun buildFormattedLocationAddress(
    latitude: Double,
    longitude: Double,
    nearestSummary: String = "",
    landmarkName: String = "",
    landmarkLat: Double = 0.0,
    landmarkLng: Double = 0.0,
    note: String = ""
): String {
    val coords = OfflineYemenGazetteer.formatCoordinates(latitude, longitude)
    val base = when {
        landmarkName.isNotBlank() && LandmarkAddress.isValid(landmarkLat, landmarkLng) -> {
            LandmarkAddress(
                label = "",
                landmarkName = landmarkName,
                landmarkLat = landmarkLat,
                landmarkLng = landmarkLng,
                pointLat = latitude,
                pointLng = longitude,
                description = note.trim()
            ).describe()
        }
        nearestSummary.isNotBlank() && note.isNotBlank() -> "$nearestSummary، ${note.trim()}"
        nearestSummary.isNotBlank() -> nearestSummary
        note.isNotBlank() -> note.trim()
        else -> "موقع محدد"
    }
    return "$base ($coords)"
}

/**
 * 🔢 نافذة إدخال أو لصق الإحداثيات يدوياً:
 * تدعم الأرقام العشرية، الأرقام العربية، روابط خرائط جوجل/واتساب/OSM، وصيغة الدرجات والدقائق.
 */
@Composable
fun ManualCoordinateDialog(
    initialLat: Double = 0.0,
    initialLng: Double = 0.0,
    gpsLat: Double = 0.0,
    gpsLng: Double = 0.0,
    hasGps: Boolean = false,
    title: String = "🔢 إدخال الإحداثيات يدوياً",
    subtitle: String = "أدخل خط العرض والطول أو الصق رابط موقع من واتساب / خرائط جوجل",
    confirmLabel: String = "اعتماد الموقع",
    onDismiss: () -> Unit,
    onConfirm: (lat: Double, lng: Double, summary: String) -> Unit,
    onNavigate: ((lat: Double, lng: Double, summary: String) -> Unit)? = null,
    onSetAsMyLocation: ((lat: Double, lng: Double) -> Unit)? = null
) {
    val context = LocalContext.current
    var pasteInput by remember { mutableStateOf("") }
    var latInput by remember {
        mutableStateOf(
            if (LandmarkAddress.isValid(initialLat, initialLng))
                String.format(Locale.US, "%.6f", initialLat)
            else ""
        )
    }
    var lngInput by remember {
        mutableStateOf(
            if (LandmarkAddress.isValid(initialLat, initialLng))
                String.format(Locale.US, "%.6f", initialLng)
            else ""
        )
    }
    var parseStatus by remember { mutableStateOf<String?>(null) }

    val parsedLat = remember(latInput) {
        OfflineYemenGazetteer.parseCoordinates("${latInput.trim()}, 44.0")?.first
            ?: latInput.trim().replace("،", ".").replace(",", ".").toDoubleOrNull()
    }
    val parsedLng = remember(lngInput) {
        OfflineYemenGazetteer.parseCoordinates("15.0, ${lngInput.trim()}")?.second
            ?: lngInput.trim().replace("،", ".").replace(",", ".").toDoubleOrNull()
    }
    val isValid = parsedLat != null && parsedLng != null && LandmarkAddress.isValid(parsedLat, parsedLng)

    var nearestSummary by remember { mutableStateOf("") }
    LaunchedEffect(parsedLat, parsedLng, isValid) {
        nearestSummary = if (isValid && parsedLat != null && parsedLng != null) {
            withContext(Dispatchers.IO) {
                OfflineYemenGazetteer.nearestReferenceSummary(context, parsedLat, parsedLng)
            }
        } else ""
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        titleContentColor = Color.White,
        textContentColor = Color.White,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Text(subtitle, fontSize = 11.5.sp, color = Color(0xFFCBD5E1))

                OutlinedTextField(
                    value = pasteInput,
                    onValueChange = { raw ->
                        pasteInput = raw
                        val parsed = OfflineYemenGazetteer.parseCoordinates(raw)
                        if (parsed != null) {
                            latInput = String.format(Locale.US, "%.6f", parsed.first)
                            lngInput = String.format(Locale.US, "%.6f", parsed.second)
                            parseStatus = "✓ تم استخراج الإحداثيات تلقائياً"
                        } else if (raw.trim().length >= 5) {
                            parseStatus = "لم يُتعرف على صيغة الرابط بعد — أدخل الأرقام أدناه"
                        } else {
                            parseStatus = null
                        }
                    },
                    label = { Text("لصق رابط موقع أو إحداثيات (اختياري)", fontSize = 11.sp) },
                    placeholder = { Text("مثال: 15.3694, 44.1910 أو رابط خرائط", fontSize = 11.sp, color = Color(0xFF64748B)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF00E5FF),
                        unfocusedBorderColor = Color(0xFF334155)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("coord_smart_paste_input")
                )

                parseStatus?.let { msg ->
                    Text(
                        msg,
                        fontSize = 11.sp,
                        color = if (msg.startsWith("✓")) Color(0xFF10B981) else Color(0xFFF59E0B)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = latInput,
                        onValueChange = { latInput = it },
                        label = { Text("خط العرض (Lat)", fontSize = 10.5.sp) },
                        placeholder = { Text("15.3694", fontSize = 11.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF10B981),
                            unfocusedBorderColor = Color(0xFF334155)
                        ),
                        modifier = Modifier.weight(1f).testTag("coord_lat_input")
                    )
                    OutlinedTextField(
                        value = lngInput,
                        onValueChange = { lngInput = it },
                        label = { Text("خط الطول (Lng)", fontSize = 10.5.sp) },
                        placeholder = { Text("44.1910", fontSize = 11.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF10B981),
                            unfocusedBorderColor = Color(0xFF334155)
                        ),
                        modifier = Modifier.weight(1f).testTag("coord_lng_input")
                    )
                }

                if (hasGps && LandmarkAddress.isValid(gpsLat, gpsLng)) {
                    OutlinedButton(
                        onClick = {
                            latInput = String.format(Locale.US, "%.6f", gpsLat)
                            lngInput = String.format(Locale.US, "%.6f", gpsLng)
                            parseStatus = "✓ تم تعبئة إحداثيات GPS الحالية"
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("تعبئة بموقع GPS الحالي", color = Color(0xFF10B981), fontSize = 12.sp)
                    }
                }

                if (isValid && parsedLat != null && parsedLng != null) {
                    Surface(
                        color = Color(0xFF1E293B),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f))
                    ) {
                        Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                "🎯 ${OfflineYemenGazetteer.formatCoordinates(parsedLat, parsedLng)}",
                                color = Color(0xFF10B981),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (nearestSummary.isNotBlank()) {
                                Text("أقرب مرجع: $nearestSummary", color = Color(0xFFCBD5E1), fontSize = 11.sp)
                            }
                        }
                    }
                } else if (latInput.isNotBlank() || lngInput.isNotBlank()) {
                    Text("⚠️ الإحداثيات غير مكتملة أو خارج النطاق الصحيح", color = Color(0xFFF59E0B), fontSize = 11.sp)
                }

                if (onNavigate != null || onSetAsMyLocation != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        onNavigate?.let { nav ->
                            Button(
                                enabled = isValid,
                                onClick = {
                                    val la = parsedLat ?: return@Button
                                    val lo = parsedLng ?: return@Button
                                    nav(la, lo, nearestSummary)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                modifier = Modifier.weight(1f).height(48.dp).testTag("coord_navigate_btn")
                            ) {
                                Text("🧭 ملاحة", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        onSetAsMyLocation?.let { setLoc ->
                            OutlinedButton(
                                enabled = isValid,
                                onClick = {
                                    val la = parsedLat ?: return@OutlinedButton
                                    val lo = parsedLng ?: return@OutlinedButton
                                    setLoc(la, lo)
                                },
                                modifier = Modifier.weight(1f).height(48.dp).testTag("coord_set_my_loc_btn")
                            ) {
                                Text("📍 كموقعي", fontSize = 12.sp, color = Color(0xFF00E5FF))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = isValid,
                onClick = {
                    val la = parsedLat ?: return@Button
                    val lo = parsedLng ?: return@Button
                    onConfirm(la, lo, nearestSummary)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF), contentColor = Color.Black),
                modifier = Modifier.height(48.dp).testTag("coord_confirm_btn")
            ) {
                Text(confirmLabel, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.height(48.dp)) {
                Text("إغلاق", color = Color(0xFF94A3B8))
            }
        }
    )
}

/**
 * 🎯 منتقي الموقع التفاعلي بالخريطة + الإحداثيات + أقرب معلم:
 * يُستخدم لتحديد موقع توصيل العميل بدقة، أو موقع الفني/المتجر/العقار، أو أي نقطة غير مسماة على الخريطة.
 */
@Composable
fun InteractiveMapLocationPickerDialog(
    initialLat: Double = 15.3694,
    initialLng: Double = 44.1910,
    hasGps: Boolean = false,
    gpsLat: Double = 0.0,
    gpsLng: Double = 0.0,
    title: String = "📍 حدد الموقع بدقة على الخريطة",
    requireNote: Boolean = false,
    onDismiss: () -> Unit,
    onLocationPicked: (PickedLocationResult) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val startLat = remember {
        when {
            LandmarkAddress.isValid(initialLat, initialLng) -> initialLat
            hasGps && LandmarkAddress.isValid(gpsLat, gpsLng) -> gpsLat
            else -> 15.3694
        }
    }
    val startLng = remember {
        when {
            LandmarkAddress.isValid(initialLat, initialLng) -> initialLng
            hasGps && LandmarkAddress.isValid(gpsLat, gpsLng) -> gpsLng
            else -> 44.1910
        }
    }

    var activeLat by remember { mutableDoubleStateOf(startLat) }
    var activeLng by remember { mutableDoubleStateOf(startLng) }
    var mapMode by remember { mutableStateOf(MapMode.SATELLITE) }
    var hybridLabels by remember { mutableStateOf(true) }
    var focusPoint by remember { mutableStateOf<Triple<String, Double, Double>?>(Triple("الموقع المحدد", startLat, startLng)) }

    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<RealValhallaNavigator.SearchAddressResult>>(emptyList()) }
    var searchBusy by remember { mutableStateOf(false) }

    var noteInput by remember { mutableStateOf("") }
    var nearestSummary by remember { mutableStateOf("") }
    var selectedLandmark by remember { mutableStateOf<LandmarkCandidate?>(null) }

    var showCoordDialog by remember { mutableStateOf(false) }
    var showLandmarkSheet by remember { mutableStateOf(false) }
    var nearbyLandmarks by remember { mutableStateOf<List<LandmarkCandidate>>(emptyList()) }
    var loadingLandmarks by remember { mutableStateOf(false) }
    var showAddCustomLandmark by remember { mutableStateOf(false) }

    LaunchedEffect(activeLat, activeLng, selectedLandmark) {
        delay(220)
        nearestSummary = withContext(Dispatchers.IO) {
            selectedLandmark?.let { lm ->
                LandmarkAddress(
                    label = "",
                    landmarkName = lm.name,
                    landmarkLat = lm.lat,
                    landmarkLng = lm.lng,
                    pointLat = activeLat,
                    pointLng = activeLng,
                    description = ""
                ).describe()
            } ?: OfflineYemenGazetteer.nearestReferenceSummary(context, activeLat, activeLng)
        }
    }

    LaunchedEffect(searchQuery) {
        val q = searchQuery.trim()
        if (q.length < 2) {
            searchResults = emptyList()
            searchBusy = false
            return@LaunchedEffect
        }
        delay(280)
        searchBusy = true
        searchResults = RealValhallaNavigator.searchAddressAndPlaces(
            query = q,
            nearbyProviders = emptyList(),
            nearbyStores = emptyList(),
            userLat = activeLat,
            userLng = activeLng,
            context = context
        )
        searchBusy = false
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0B0F19))) {
            RealLeafletMapView(
                userCoords = if (hasGps && LandmarkAddress.isValid(gpsLat, gpsLng)) gpsLat to gpsLng else 0.0 to 0.0,
                nearbyProviders = emptyList(),
                nearbyStores = emptyList(),
                nearbyProperties = emptyList(),
                dynamicOffsets = emptyMap(),
                currentMapMode = mapMode,
                selectedCity = "الكل",
                selectedEntity = null,
                onProviderSelected = {},
                onStoreSelected = {},
                onPropertySelected = {},
                onDeselect = {},
                waypointMarkers = listOfNotNull(
                    selectedLandmark?.let { Triple("🏛️ ${it.name}", it.lat, it.lng) }
                ),
                focusPoint = focusPoint,
                pickMode = true,
                hybridLabels = hybridLabels,
                ecoMode = MapPowerDataPrefs.isEcoMode(context),
                onMapPicked = { lat, lng ->
                    activeLat = lat
                    activeLng = lng
                    focusPoint = Triple("الموقع المختار", lat, lng)
                },
                onMapLongPressed = { lat, lng ->
                    activeLat = lat
                    activeLng = lng
                    focusPoint = Triple("الموقع المختار", lat, lng)
                },
                onMapCenterChanged = { lat, lng ->
                    activeLat = lat
                    activeLng = lng
                },
                modifier = Modifier.fillMaxSize()
            )

            // 🎯 مؤشر التثبيت في المنتصف (Crosshair)
            Box(
                modifier = Modifier.align(Alignment.Center),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0x3300E5FF))
                )
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "مؤشر الموقع المختار",
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(38.dp).offset(y = (-14).dp)
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00E5FF))
                )
            }

            // الشريط العلوي
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF0F172A).copy(alpha = 0.95f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.White)
                            }
                            Column(Modifier.weight(1f)) {
                                Text(title, color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    "حرّك الخريطة أو اضغط على أي مكان حتى بدون اسم",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 10.5.sp
                                )
                            }
                            TextButton(
                                onClick = {
                                    mapMode = if (mapMode == MapMode.SATELLITE) MapMode.OSM else MapMode.SATELLITE
                                },
                                modifier = Modifier.height(48.dp)
                            ) {
                                Text(
                                    if (mapMode == MapMode.SATELLITE) "🗺️ شوارع" else "🛰️ أقمار",
                                    color = Color(0xFF00E5FF),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                singleLine = true,
                                placeholder = { Text("ابحث عن منطقة، فندق، جولة أو الصق إحداثيات…", fontSize = 11.5.sp, color = Color(0xFF94A3B8)) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF00E5FF),
                                    unfocusedBorderColor = Color(0xFF334155)
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            FilledTonalButton(
                                onClick = { showCoordDialog = true },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Color(0xFF1E293B),
                                    contentColor = Color(0xFF00E5FF)
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp),
                                modifier = Modifier.height(48.dp).testTag("picker_open_coords_btn")
                            ) {
                                Text("🔢 إحداثيات", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (searchBusy) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Color(0xFF10B981))
                    }
                }

                if (searchResults.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF0F172A).copy(alpha = 0.97f),
                        modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp)
                    ) {
                        LazyColumn {
                            items(searchResults) { res ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            activeLat = res.latitude
                                            activeLng = res.longitude
                                            focusPoint = Triple(res.shortName, res.latitude, res.longitude)
                                            searchResults = emptyList()
                                            searchQuery = ""
                                        }
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(res.icon, fontSize = 16.sp)
                                    Column(Modifier.weight(1f)) {
                                        Text(res.shortName, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(res.displayName, color = Color(0xFF94A3B8), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                                HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
                            }
                        }
                    }
                }
            }

            // اللوحة السفلية لتأكيد الموقع وتفاصيل المعلم
            Surface(
                shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
                color = Color(0xFF0F172A).copy(alpha = 0.97f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "🎯 ${OfflineYemenGazetteer.formatCoordinates(activeLat, activeLng)}",
                                color = Color(0xFF00E5FF),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (nearestSummary.isNotBlank()) {
                                Text(
                                    nearestSummary,
                                    color = Color(0xFF10B981),
                                    fontSize = 11.5.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        if (hasGps && LandmarkAddress.isValid(gpsLat, gpsLng)) {
                            IconButton(
                                onClick = {
                                    activeLat = gpsLat
                                    activeLng = gpsLng
                                    focusPoint = Triple("موقعي الحالي", gpsLat, gpsLng)
                                },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Default.MyLocation, contentDescription = "العودة لموقعي GPS", tint = Color(0xFF10B981))
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = {
                                showLandmarkSheet = true
                                scope.launch {
                                    loadingLandmarks = true
                                    nearbyLandmarks = LandmarkFinder.nearbyWithOfflineFallback(context, activeLat, activeLng)
                                    loadingLandmarks = false
                                }
                            },
                            modifier = Modifier.weight(1f).height(48.dp).testTag("picker_nearby_landmark_btn")
                        ) {
                            Text(
                                selectedLandmark?.let { "🏛️ ${it.name}" } ?: "🏛️ معلم قريب (فندق/جولة/شارع)",
                                color = Color.White,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        OutlinedButton(
                            onClick = { showAddCustomLandmark = true },
                            modifier = Modifier.height(48.dp).testTag("picker_add_landmark_btn")
                        ) {
                            Text("➕ إضافة معلم", color = Color(0xFFFBBF24), fontSize = 11.sp)
                        }
                    }

                    OutlinedTextField(
                        value = noteInput,
                        onValueChange = { noteInput = it },
                        singleLine = true,
                        label = {
                            Text(
                                if (requireNote) "وصف علامة مميزة (الباب الأحمر، الدور الثاني…)" else "علامة إضافية للمندوب أو الفني (اختياري)",
                                fontSize = 11.sp
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF10B981),
                            unfocusedBorderColor = Color(0xFF334155)
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("picker_note_input")
                    )

                    Button(
                        enabled = !requireNote || noteInput.trim().length >= 3,
                        onClick = {
                            val lm = selectedLandmark
                            val formatted = buildFormattedLocationAddress(
                                latitude = activeLat,
                                longitude = activeLng,
                                nearestSummary = nearestSummary,
                                landmarkName = lm?.name.orEmpty(),
                                landmarkLat = lm?.lat ?: 0.0,
                                landmarkLng = lm?.lng ?: 0.0,
                                note = noteInput.trim()
                            )
                            onLocationPicked(
                                PickedLocationResult(
                                    latitude = activeLat,
                                    longitude = activeLng,
                                    label = lm?.name ?: "موقع محدد",
                                    landmarkName = lm?.name.orEmpty(),
                                    landmarkLat = lm?.lat ?: 0.0,
                                    landmarkLng = lm?.lng ?: 0.0,
                                    note = noteInput.trim(),
                                    formattedAddress = formatted
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("confirm_picked_location_btn")
                    ) {
                        Text("اعتماد هذا الموقع ✓", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showCoordDialog) {
        ManualCoordinateDialog(
            initialLat = activeLat,
            initialLng = activeLng,
            gpsLat = gpsLat,
            gpsLng = gpsLng,
            hasGps = hasGps,
            onDismiss = { showCoordDialog = false },
            onConfirm = { lat, lng, _ ->
                activeLat = lat
                activeLng = lng
                focusPoint = Triple("إحداثيات", lat, lng)
                showCoordDialog = false
            }
        )
    }

    if (showLandmarkSheet) {
        AlertDialog(
            onDismissRequest = { showLandmarkSheet = false },
            containerColor = Color(0xFF0F172A),
            titleContentColor = Color.White,
            title = { Text("🏛️ اختر أقرب معلم معروف", fontWeight = FontWeight.Bold, fontSize = 15.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (loadingLandmarks) {
                        LinearProgressIndicator(Modifier.fillMaxWidth(), color = Color(0xFF00E5FF))
                    } else if (nearbyLandmarks.isEmpty()) {
                        Text("لا توجد معالم مسجلة قريبة من هذه النقطة. يمكنك إضافة معلم جديد باسم الفندق أو الشارع أو الجولة.", color = Color(0xFFCBD5E1), fontSize = 12.sp)
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                            items(nearbyLandmarks) { lm ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedLandmark = lm
                                            showLandmarkSheet = false
                                        }
                                        .padding(vertical = 10.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text("${lm.kind}: ${lm.name}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        Text("المسافة: ${LandmarkAddress.roundMeters(lm.distanceM)}", color = Color(0xFF10B981), fontSize = 11.sp)
                                    }
                                    Icon(Icons.Default.CheckCircleOutline, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(18.dp))
                                }
                                HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLandmarkSheet = false; showAddCustomLandmark = true }) {
                    Text("➕ إضافة معلم جديد", color = Color(0xFFFBBF24))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLandmarkSheet = false }) {
                    Text("إغلاق", color = Color(0xFF94A3B8))
                }
            }
        )
    }

    if (showAddCustomLandmark) {
        AddCustomLandmarkDialog(
            latitude = activeLat,
            longitude = activeLng,
            onDismiss = { showAddCustomLandmark = false },
            onAdded = { saved ->
                selectedLandmark = LandmarkCandidate(saved.name, saved.latitude, saved.longitude, saved.kind, 0.0)
                showAddCustomLandmark = false
            }
        )
    }
}

@Composable
fun AddCustomLandmarkDialog(
    latitude: Double,
    longitude: Double,
    onDismiss: () -> Unit,
    onAdded: (CustomLandmark) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    val kinds = listOf("فندق", "جولة", "شارع", "مسجد", "محطة وقود", "سوق", "مستشفى", "معلم")
    var selectedKind by remember { mutableStateOf("معلم") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        titleContentColor = Color.White,
        title = { Text("➕ إضافة معلم في هذه النقطة", fontWeight = FontWeight.Bold, fontSize = 15.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "سيُحفظ هذا المعلم محلياً على جهازك (${OfflineYemenGazetteer.formatCoordinates(latitude, longitude)}) ليظهر في البحث وعناوين التوصيل.",
                    color = Color(0xFFCBD5E1),
                    fontSize = 11.5.sp
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(kinds) { kind ->
                        FilterChip(
                            selected = selectedKind == kind,
                            onClick = { selectedKind = kind },
                            label = { Text(kind, fontSize = 11.sp) }
                        )
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text("اسم المعلم *") },
                    placeholder = { Text("مثال: فندق السعيد، جولة القادسية، بقالة الأمل") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF00E5FF),
                        unfocusedBorderColor = Color(0xFF334155)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = name.trim().length >= 2,
                onClick = {
                    scope.launch {
                        val saved = withContext(Dispatchers.IO) {
                            CustomLandmarkStore.add(context, name.trim(), selectedKind, latitude, longitude)
                        }
                        if (saved != null) onAdded(saved) else onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
            ) {
                Text("حفظ واعتماد", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء", color = Color(0xFF94A3B8)) }
        }
    )
}
