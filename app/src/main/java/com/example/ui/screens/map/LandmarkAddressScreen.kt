package com.example.ui.screens.map

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun LandmarkAddressScreen(
    centerLat: Double,
    centerLng: Double,
    hasRealLocation: Boolean,
    selectedCity: String = "الكل",
    onSaved: (LandmarkAddress) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { LandmarkAddressStore(context.applicationContext) }
    val snackbar = remember { SnackbarHostState() }

    var step by remember { mutableIntStateOf(0) }                  // 0 معلم، 1 نقطة، 2 تأكيد
    var landmarkName by remember { mutableStateOf("") }
    var landmark by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var point by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var description by remember { mutableStateOf("") }
    var label by remember { mutableStateOf("منزلي") }
    var confirmed by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<RealValhallaNavigator.SearchAddressResult>>(emptyList()) }
    var nearby by remember { mutableStateOf<List<LandmarkCandidate>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var tapForLandmark by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var focus by remember { mutableStateOf<Triple<String, Double, Double>?>(null) }
    var manualName by remember { mutableStateOf("") }

    BackHandler { if (step > 0) { step -= 1; confirmed = false } else onClose() }

    fun chooseLandmark(name: String, lat: Double, lng: Double) {
        landmarkName = name; landmark = lat to lng; point = null
        focus = Triple(name, lat, lng); results = emptyList(); nearby = emptyList(); message = null; query = ""
        step = 1
    }

    // بحث بالاسم (Nominatim الموجود) مع debounce
    LaunchedEffect(query) {
        val q = query.trim()
        if (step != 0 || q.length < 3) { results = emptyList(); return@LaunchedEffect }
        delay(600)
        busy = true
        results = RealValhallaNavigator.searchAddressAndPlaces(
            q, emptyList(), emptyList(),
            if (hasRealLocation) centerLat else 0.0, if (hasRealLocation) centerLng else 0.0
        )
        message = if (results.isEmpty()) (RealValhallaNavigator.lastSearchError ?: "لا توجد نتائج") else null
        busy = false
    }

    val onPicked: (Double, Double) -> Unit = { lat, lng ->
        when (step) {
            0 -> tapForLandmark = lat to lng            // معلم يدوي: نطلب اسمه
            1 -> point = lat to lng
        }
    }

    val draft: LandmarkAddress? = remember(landmark, point, landmarkName, description, label) {
        val l = landmark; val p = point
        if (l != null && p != null) LandmarkAddress(
            label = label.ifBlank { "منزلي" }, landmarkName = landmarkName,
            landmarkLat = l.first, landmarkLng = l.second, pointLat = p.first, pointLng = p.second,
            description = description.trim()
        ) else null
    }
    val markers = remember(landmark, point, landmarkName) {
        listOfNotNull(
            landmark?.let { Triple("المعلم: $landmarkName", it.first, it.second) },
            point?.let { Triple("موقعي", it.first, it.second) }
        )
    }

    Box(modifier.fillMaxSize().background(Color(0xFF0B0F19))) {
        RealLeafletMapView(
            userCoords = if (hasRealLocation) Pair(centerLat, centerLng) else Pair(0.0, 0.0),
            nearbyProviders = emptyList(), nearbyStores = emptyList(), nearbyProperties = emptyList(),
            dynamicOffsets = emptyMap(),
            currentMapMode = MapMode.OSM,
            selectedCity = selectedCity,
            selectedEntity = null,
            onProviderSelected = {}, onStoreSelected = {}, onPropertySelected = {}, onDeselect = {},
            waypointMarkers = markers,
            focusPoint = focus,
            pickMode = step <= 1,
            onMapPicked = onPicked,
            modifier = Modifier.fillMaxSize()
        )

        // الشريط العلوي
        Column(Modifier.align(Alignment.TopCenter).fillMaxWidth().statusBarsPadding().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFF0F172A).copy(alpha = 0.95f), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("عنواني بالمعلم — الخطوة ${step + 1} من 3", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    IconButton(onClick = onClose, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color(0xFF94A3B8)) }
                }
            }
            if (step == 0) {
                OutlinedTextField(
                    value = query, onValueChange = { query = it }, singleLine = true,
                    placeholder = { Text("ابحث عن معلم معروف: مسجد، مدرسة، سوق…", color = Color(0xFF94A3B8), fontSize = 12.sp) },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF0F172A), unfocusedContainerColor = Color(0xFF0F172A),
                        focusedBorderColor = Color(0xFF00E5FF), unfocusedBorderColor = Color(0xFF334155)),
                    modifier = Modifier.fillMaxWidth()
                )
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Color(0xFF00E5FF))
                message?.let { Text(it, color = Color(0xFFF59E0B), fontSize = 12.sp, modifier = Modifier.background(Color(0xCC0F172A), RoundedCornerShape(8.dp)).padding(8.dp)) }
                if (results.isNotEmpty() || nearby.isNotEmpty()) {
                    Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFF0F172A).copy(alpha = 0.97f), modifier = Modifier.fillMaxWidth().heightIn(max = 260.dp)) {
                        LazyColumn {
                            items(results) { r ->
                                Text("📍 ${r.shortName}", color = Color.White, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.fillMaxWidth().clickable { chooseLandmark(r.shortName, r.latitude, r.longitude) }.padding(12.dp))
                                HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
                            }
                            items(nearby) { n ->
                                Text("${n.kind}: ${n.name} (${LandmarkAddress.roundMeters(n.distanceM)})", color = Color.White, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.fillMaxWidth().clickable { chooseLandmark(n.name, n.lat, n.lng) }.padding(12.dp))
                                HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
                            }
                        }
                    }
                }
            }
        }

        // اللوحة السفلية
        Surface(
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            color = Color(0xFF0F172A).copy(alpha = 0.97f),
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding()
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                when (step) {
                    0 -> {
                        Text("اختر معلماً قريباً من منزلك يعرفه الناس. اضغط على موقعه في الخريطة إن لم تجده في القائمة.", color = Color(0xFFCBD5E1), fontSize = 13.sp)
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    busy = true
                                    nearby = LandmarkFinder.nearby(centerLat, centerLng)
                                    message = if (nearby.isEmpty()) (LandmarkFinder.lastError ?: "لا معالم مسجلة قريباً؛ اضغط على الخريطة لإضافة معلم") else null
                                    busy = false
                                }
                            },
                            enabled = hasRealLocation && LandmarkAddress.isValid(centerLat, centerLng),
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("🕌 معالم قريبة مني") }
                        if (!hasRealLocation) Text("لإظهار المعالم القريبة منك فعّل GPS، أو ابحث بالاسم، أو اضغط على الخريطة.", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    }
                    1 -> {
                        Text("المعلم: $landmarkName", color = Color(0xFF00E5FF), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("اضغط على الخريطة في مكان منزلك بالضبط. يمكنك الضغط مجدداً لتصحيحه.", color = Color(0xFFCBD5E1), fontSize = 13.sp)
                        draft?.let { Text(it.copy(description = "").describe(), color = Color(0xFF10B981), fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { step = 0; point = null }, modifier = Modifier.weight(1f)) { Text("تغيير المعلم") }
                            Button(onClick = { step = 2 }, enabled = point != null, modifier = Modifier.weight(1f)) { Text("التالي") }
                        }
                    }
                    else -> {
                        OutlinedTextField(value = label, onValueChange = { label = it }, singleLine = true, label = { Text("اسم العنوان") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(
                            value = description, onValueChange = { description = it }, label = { Text("وصف إلزامي (البوابة الحمراء، الدور الثاني…)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        val err = draft?.validate()
                        draft?.let {
                            Text(it.describe(), color = Color(0xFF10B981), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            if (it.distanceMeters > 500) Text("المسافة عن المعلم كبيرة (${LandmarkAddress.roundMeters(it.distanceMeters)})؛ يفضّل معلم أقرب.", color = Color(0xFFF59E0B), fontSize = 11.sp)
                        }
                        if (err != null && description.isNotBlank()) Text(err, color = Color(0xFFF59E0B), fontSize = 11.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = confirmed, onCheckedChange = { confirmed = it })
                            Text("نعم، هذا هو موقع منزلي", color = Color.White, fontSize = 13.sp)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { step = 1; confirmed = false }, modifier = Modifier.weight(1f)) { Text("رجوع") }
                            Button(
                                onClick = {
                                    val d = draft ?: return@Button
                                    scope.launch {
                                        val ok = withContext(Dispatchers.IO) { store.save(d) }
                                        if (ok) { onSaved(d); onClose() } else snackbar.showSnackbar("تعذر حفظ العنوان على هذا الجهاز")
                                    }
                                },
                                enabled = draft != null && err == null && confirmed,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                modifier = Modifier.weight(1f)
                            ) { Text("حفظ العنوان", fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 220.dp))
    }

    // تسمية معلم أضافه المستخدم بالنقر على الخريطة
    tapForLandmark?.let { (lat, lng) ->
        AlertDialog(
            onDismissRequest = { tapForLandmark = null; manualName = "" },
            title = { Text("اسم هذا المعلم") },
            text = { OutlinedTextField(value = manualName, onValueChange = { manualName = it }, singleLine = true, placeholder = { Text("مثال: مسجد النور") }) },
            confirmButton = {
                TextButton(enabled = manualName.trim().length >= 2, onClick = {
                    chooseLandmark(manualName.trim(), lat, lng); tapForLandmark = null; manualName = ""
                }) { Text("اعتماد") }
            },
            dismissButton = { TextButton(onClick = { tapForLandmark = null; manualName = "" }) { Text("إلغاء") } }
        )
    }
}

/** قائمة «عناويني»: عرض، ذهاب (المعلم أولاً ثم النقطة)، حذف. */
@Composable
fun SavedLandmarkAddressesDialog(
    addresses: List<LandmarkAddress>,
    onNavigate: (LandmarkAddress) -> Unit,
    onDelete: (LandmarkAddress) -> Unit,
    onAddNew: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("عناويني بالمعلم") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(addresses) { a ->
                    Column(Modifier.fillMaxWidth().background(Color(0x14FFFFFF), RoundedCornerShape(12.dp)).padding(10.dp)) {
                        Text(a.label, fontWeight = FontWeight.Bold)
                        Text(a.describe(), fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { onNavigate(a) }) { Text("اذهب") }
                            TextButton(onClick = {
                                com.example.utils.SosDispatcher.safeStart(context, com.example.utils.SosDispatcher.shareIntent(a.shareText()))
                            }) { Text("مشاركة") }
                            TextButton(onClick = { onDelete(a) }) { Text("حذف", color = Color(0xFFEF4444)) }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onAddNew) { Text("إضافة عنوان") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إغلاق") } }
    )
}

/** بطاقة للسائق/الفني: تقرأ حقل landmarkAddress من الحجز وتعرضه مع زر الذهاب. */
@Composable
fun LandmarkAddressCard(address: LandmarkAddress, onNavigate: (LandmarkAddress) -> Unit, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFF0F172A), modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("📍 عنوان العميل بالمعلم", color = Color(0xFF00E5FF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(address.describe(), color = Color.White, fontSize = 14.sp)
            Button(onClick = { onNavigate(address) }, modifier = Modifier.fillMaxWidth()) { Text("اذهب: المعلم أولاً ثم الموقع") }
        }
    }
}
