package com.example.ui.screens.map

import android.Manifest
import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ProviderEntity
import com.example.data.StoreEntity
import com.example.ui.MainViewModel
import com.example.ui.components.AddCustomLandmarkDialog
import com.example.ui.components.ManualCoordinateDialog
import com.example.ui.dialogs.BookingDialog
import com.example.ui.screens.map.components.GroupTrackingDialog
import com.example.ui.screens.map.utils.MapDistanceCalculator
import com.example.utils.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 🗨️ MapScreenDialogsHost
 * استضافة جميع النوافذ والحوارات المنبثقة لشاشة الخريطة الرئيسية:
 * أذونات الموقع، حزم Valhalla، نداء SOS، تشخيص الخرائط، GNSS، تتبع المجموعة، الحجز، العناوين المحفوظة، اختيار الوجهة، والإحداثيات اليدوية
 */
@Composable
internal fun MapScreenDialogsHost(
    context: Context,
    coroutineScope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
    viewModel: MainViewModel,
    state: MapScreenState,
    themeColors: VisualThemePalette,
    providers: List<ProviderEntity>,
    stores: List<StoreEntity>,
    hasRealLocation: Boolean,
    safeUserLat: Double,
    safeUserLng: Double,
    accuracyM: Float,
    showLocationDisclosure: Boolean,
    onDismissLocationDisclosure: () -> Unit,
    locationPermissionLauncher: ActivityResultLauncher<Array<String>>,
    showGnss: Boolean,
    onDismissGnss: () -> Unit,
    onOpenLandmarkAddress: () -> Unit,
    showGroupTracking: Boolean,
    onDismissGroupTracking: () -> Unit,
    onGroupMembersUpdated: (List<GroupMember>) -> Unit,
    onFocusGroupMember: (RealValhallaNavigator.SearchAddressResult) -> Unit,
    showDownloadDialog: Boolean,
    onDismissDownloadDialog: () -> Unit,
    onTilesDownloaded: () -> Unit,
    showSosDialog: Boolean,
    onDismissSosDialog: () -> Unit,
    showDiagnostics: Boolean,
    onDismissDiagnostics: () -> Unit,
    onDiagnosticsTilesUpdated: () -> Unit,
    showSavedAddresses: Boolean,
    savedAddresses: List<LandmarkAddress>,
    onSavedAddressesUpdated: (List<LandmarkAddress>) -> Unit,
    addressStore: LandmarkAddressStore,
    onDismissSavedAddresses: () -> Unit,
    showDestinationPicker: Boolean,
    onDismissDestinationPicker: () -> Unit,
    onEnablePinPickMode: () -> Unit,
    onOpenCoordinateDialog: () -> Unit,
    showCoordinateDialog: Boolean,
    onDismissCoordinateDialog: () -> Unit,
    pinnedPoint: Pair<Double, Double>?,
    onPinConfirmedFromCoords: (Double, Double) -> Unit,
    showAddLandmarkForPin: Pair<Double, Double>?,
    onDismissAddLandmarkForPin: () -> Unit
) {
    if (showLocationDisclosure) {
        AlertDialog(
            onDismissRequest = onDismissLocationDisclosure,
            title = { Text("لماذا نطلب موقعك؟", fontWeight = FontWeight.Bold) },
            text = { Text(PermissionManager.getRationaleForPermission(PermissionManager.PERMISSION_FINE_LOCATION)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDismissLocationDisclosure()
                        locationPermissionLauncher.launch(
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                        )
                    }
                ) { Text("متابعة", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = onDismissLocationDisclosure) { Text("ليس الآن") }
            }
        )
    }

    if (showGnss) {
        GnssPanelDialog(
            accuracyM = accuracyM.takeIf { it < Float.MAX_VALUE },
            onOpenLandmarkAddress = onOpenLandmarkAddress,
            onDismiss = onDismissGnss
        )
    }

    if (showGroupTracking) {
        GroupTrackingDialog(
            onMembersUpdated = onGroupMembersUpdated,
            onFocusMember = { member ->
                val lat = member.lastLat
                val lng = member.lastLng
                if (lat != null && lng != null) {
                    onFocusGroupMember(
                        RealValhallaNavigator.SearchAddressResult(
                            displayName = "موقع ${member.label} في المجموعة",
                            shortName = "👥 ${member.label}",
                            latitude = lat,
                            longitude = lng,
                            type = "group",
                            icon = "👥"
                        )
                    )
                    onDismissGroupTracking()
                }
            },
            onDismiss = onDismissGroupTracking
        )
    }

    // 📥 Download Dialog for Offline Maps
    if (showDownloadDialog) {
        var isDownloading by remember { mutableStateOf(false) }
        var downloadProgress by remember { mutableFloatStateOf(0f) }
        var downloadStatusText by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { if (!isDownloading) onDismissDownloadDialog() },
            containerColor = Color(0xFF0B0F19),
            titleContentColor = Color.White,
            textContentColor = Color.White,
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null, tint = Color(0xFF38BDF8))
                    Text("تحميل خرائط الملاحة أوفلاين", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "للحصول على أدق النتائج والملاحة في اليمن بدون إنترنت، يرجى تحميل حزمة خرائط اليمن.\nحجم التنزيل: ~${ValhallaTileDownloader.EXPECTED_DOWNLOAD_MB} ميجابايت مضغوطة (تُفكّ إلى ~${ValhallaTileDownloader.EXPECTED_INSTALLED_MB} ميجابايت على الجهاز).",
                        color = Color.White,
                        fontSize = 13.sp
                    )
                    if (isDownloading) {
                        LinearProgressIndicator(
                            progress = { downloadProgress },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = Color(0xFF10B981),
                            trackColor = Color(0xFF1E293B)
                        )
                        Text(downloadStatusText, color = Color(0xFF10B981), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            confirmButton = {
                if (!isDownloading) {
                    Button(
                        onClick = {
                            isDownloading = true
                            coroutineScope.launch {
                                val result = ValhallaTileDownloader.downloadAndExtractTiles(context) { p, status ->
                                    downloadProgress = p
                                    downloadStatusText = status
                                }
                                if (result.isSuccess) {
                                    onTilesDownloaded()
                                    onDismissDownloadDialog()
                                    snackbarHostState.showSnackbar("✅ تم تثبيت خرائط اليمن (~${ValhallaTileDownloader.EXPECTED_INSTALLED_MB} MB) بنجاح! استمتع بالملاحة أوفلاين.")
                                } else {
                                    isDownloading = false
                                    snackbarHostState.showSnackbar("❌ فشل التحميل: ${result.exceptionOrNull()?.message ?: "يرجى التحقق من الاتصال."}")
                                }
                            }
                        },
                        modifier = Modifier.heightIn(min = 48.dp).testTag("map_download_offline_confirm_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Text("تحميل الآن (~${ValhallaTileDownloader.EXPECTED_DOWNLOAD_MB} MB)", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                if (!isDownloading) {
                    TextButton(onClick = onDismissDownloadDialog) {
                        Text("لاحقاً", color = Color(0xFF94A3B8))
                    }
                }
            }
        )
    }

    // 🚨 10. SOS / الفزعة Confirmation Dialog
    if (showSosDialog) {
        val nearest = remember(providers, hasRealLocation, safeUserLat, safeUserLng) {
            if (!hasRealLocation) null
            else providers
                .filter { it.phone.isNotBlank() && it.latitude != 0.0 && it.longitude != 0.0 }
                .minByOrNull { MapDistanceCalculator.calculateDistanceMeters(safeUserLat, safeUserLng, it.latitude, it.longitude) }
        }
        val sosText = SosDispatcher.buildMessage(safeUserLat, safeUserLng, null)

        AlertDialog(
            onDismissRequest = onDismissSosDialog,
            containerColor = Color(0xFF0F172A),
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Emergency, contentDescription = null, tint = Color(0xFFEF4444))
                    Text("🚨 نداء فزعة طوارئ", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (!hasRealLocation) {
                        Text("لم يتم تحديد موقعك الحقيقي بعد، فلن يُرسَل موقع. يمكنك الاتصال مباشرة.", color = Color(0xFFF59E0B), fontSize = 13.sp)
                    } else {
                        Text("سيتم استخدام موقعك الحالي. لا يُعتبر النداء مرسلاً إلا بعد تأكيد الإرسال أدناه.", color = Color(0xFFCBD5E1), fontSize = 13.sp)
                    }
                    if (NavConfig.EMERGENCY_NUMBER.isNotBlank()) {
                        Button(
                            onClick = { SosDispatcher.safeStart(context, SosDispatcher.dialIntent(NavConfig.EMERGENCY_NUMBER)) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("📞 اتصال بالطوارئ ${NavConfig.EMERGENCY_NUMBER}", fontWeight = FontWeight.Bold) }
                    }
                    nearest?.let { p ->
                        Text("أقرب فني: ${p.name}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Button(
                            onClick = { SosDispatcher.safeStart(context, SosDispatcher.dialIntent(p.phone)) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("📞 اتصل بـ ${p.name}")
                        }
                        if (hasRealLocation) {
                            OutlinedButton(
                                onClick = {
                                    if (!SosDispatcher.safeStart(context, SosDispatcher.smsIntent(p.phone, sosText))) {
                                        coroutineScope.launch { snackbarHostState.showSnackbar("تعذر فتح تطبيق الرسائل") }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("✉️ أرسل رسالة SMS بموقعي (تعمل بلا إنترنت)")
                            }
                        }
                    }
                    if (hasRealLocation) {
                        OutlinedButton(
                            onClick = { SosDispatcher.safeStart(context, SosDispatcher.shareIntent(sosText)) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("📤 مشاركة موقعي (واتساب وغيره)")
                        }
                        Button(
                            onClick = {
                                val ok = viewModel.enqueueSos(
                                    safeUserLat,
                                    safeUserLng,
                                    accuracyM.takeIf { it < Float.MAX_VALUE },
                                    sosText
                                )
                                onDismissSosDialog()
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(
                                        if (ok) "تم حفظ النداء وسيُرسل للخادم فور توفر الإنترنت. للأمان اتصل أيضاً بأقرب فني."
                                        else "تعذر حفظ النداء! اتصل بأقرب فني أو أرسل رسالة SMS الآن."
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("🚨 إرسال نداء عبر التطبيق", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = onDismissSosDialog) {
                    Text("إغلاق", color = Color(0xFF94A3B8))
                }
            }
        )
    }

    if (showDiagnostics) {
        MapDiagnosticsDialog(
            hasRealLocation = hasRealLocation,
            accuracyM = accuracyM.takeIf { it < Float.MAX_VALUE },
            onTilesUpdated = onDiagnosticsTilesUpdated,
            onDismiss = onDismissDiagnostics
        )
    }

    // Provider Booking Dialog
    state.bookingProviderTarget?.let { provider ->
        BookingDialog(
            provider = provider,
            viewModel = viewModel,
            themeColors = themeColors,
            onDismiss = { state.bookingProviderTarget = null }
        )
    }

    if (showSavedAddresses) {
        SavedLandmarkAddressesDialog(
            addresses = savedAddresses,
            onNavigate = { a ->
                onDismissSavedAddresses()
                if (!hasRealLocation) {
                    viewModel.startLocationUpdates()
                    coroutineScope.launch { snackbarHostState.showSnackbar("فعّل GPS وانتظر تحديد موقعك") }
                } else {
                    state.startNavigationViaLandmark(a)
                }
            },
            onDelete = { a ->
                coroutineScope.launch {
                    withContext(Dispatchers.IO) { addressStore.delete(a.id) }
                    onSavedAddressesUpdated(withContext(Dispatchers.IO) { addressStore.getAll() })
                }
            },
            onAddNew = {
                onDismissSavedAddresses()
                onOpenLandmarkAddress()
            },
            onDismiss = onDismissSavedAddresses
        )
    }

    if (showDestinationPicker) {
        var pickerQuery by remember { mutableStateOf("") }
        var pickerResults by remember { mutableStateOf<List<RealValhallaNavigator.SearchAddressResult>>(emptyList()) }
        var pickerBusy by remember { mutableStateOf(false) }
        val defaultSuggestions = remember(safeUserLat, safeUserLng, savedAddresses) {
            val saved = savedAddresses.map {
                RealValhallaNavigator.SearchAddressResult(
                    displayName = "عنوان محفوظ • ${it.landmarkName}",
                    shortName = it.label,
                    latitude = it.pointLat,
                    longitude = it.pointLng,
                    type = "saved",
                    icon = "🏠"
                )
            }
            (saved + OfflineYemenGazetteer.suggestionsNear(safeUserLat, safeUserLng, limit = 12))
                .distinctBy { "${it.shortName}_${it.latitude}" }
        }

        LaunchedEffect(pickerQuery) {
            val q = pickerQuery.trim()
            if (q.length < 2) {
                pickerResults = emptyList()
                pickerBusy = false
                return@LaunchedEffect
            }
            delay(250)
            pickerBusy = true
            pickerResults = RealValhallaNavigator.searchAddressAndPlaces(
                query = q,
                nearbyProviders = providers,
                nearbyStores = stores,
                userLat = if (hasRealLocation) safeUserLat else 0.0,
                userLng = if (hasRealLocation) safeUserLng else 0.0,
                context = context
            )
            pickerBusy = false
        }

        val visibleList = if (pickerQuery.trim().length >= 2) pickerResults else defaultSuggestions

        AlertDialog(
            onDismissRequest = onDismissDestinationPicker,
            containerColor = Color(0xFF0F172A),
            titleContentColor = Color.White,
            title = { Text("🧭 اختر وجهتك للملاحة", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = pickerQuery,
                        onValueChange = { pickerQuery = it },
                        singleLine = true,
                        placeholder = { Text("ابحث عن شارع، جولة، فندق، معلم أو الصق إحداثيات…", fontSize = 12.sp, color = Color(0xFF94A3B8)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF00E5FF),
                            unfocusedBorderColor = Color(0xFF334155)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = {
                                onDismissDestinationPicker()
                                onEnablePinPickMode()
                            },
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Text("📍 دبوس الخريطة", fontSize = 11.sp, color = Color(0xFF10B981), maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = {
                                onDismissDestinationPicker()
                                onOpenCoordinateDialog()
                            },
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Text("🔢 إحداثيات يدوياً", fontSize = 11.sp, color = Color(0xFF00E5FF), maxLines = 1)
                        }
                    }
                    if (pickerBusy) {
                        LinearProgressIndicator(Modifier.fillMaxWidth(), color = Color(0xFF10B981))
                    }
                    if (visibleList.isEmpty() && pickerQuery.trim().length >= 2 && !pickerBusy) {
                        Text(
                            text = "لم يتم العثور على «${pickerQuery.trim()}» — حدد المكان بدبوس الخريطة أو بالإحداثيات أعلاه",
                            color = Color(0xFFF59E0B),
                            fontSize = 12.sp
                        )
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 260.dp)) {
                            items(visibleList) { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val wp = NavWaypoint(
                                                name = item.shortName,
                                                latitude = item.latitude,
                                                longitude = item.longitude
                                            )
                                            onDismissDestinationPicker()
                                            state.startNavigation(wp)
                                        }
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(item.icon, fontSize = 16.sp)
                                    Column(Modifier.weight(1f)) {
                                        Text(item.shortName, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(item.displayName, color = Color(0xFF94A3B8), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                                HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = onDismissDestinationPicker) {
                    Text("إغلاق", color = Color(0xFF94A3B8))
                }
            }
        )
    }

    if (showCoordinateDialog) {
        ManualCoordinateDialog(
            initialLat = pinnedPoint?.first ?: safeUserLat,
            initialLng = pinnedPoint?.second ?: safeUserLng,
            gpsLat = safeUserLat,
            gpsLng = safeUserLng,
            hasGps = hasRealLocation,
            confirmLabel = "📌 عرض على الخريطة",
            onDismiss = onDismissCoordinateDialog,
            onConfirm = { lat, lng, _ ->
                onPinConfirmedFromCoords(lat, lng)
                onDismissCoordinateDialog()
            },
            onNavigate = { lat, lng, _ ->
                val wp = NavWaypoint(
                    name = "إحداثيات (${OfflineYemenGazetteer.formatCoordinates(lat, lng)})",
                    latitude = lat,
                    longitude = lng
                )
                onDismissCoordinateDialog()
                if (!hasRealLocation) {
                    coroutineScope.launch { snackbarHostState.showSnackbar("حدد موقع انطلاقك أولاً عبر GPS أو «📍 كموقعي»") }
                } else {
                    state.startNavigation(wp)
                }
            },
            onSetAsMyLocation = { lat, lng ->
                viewModel.setManualUserLocation(lat, lng)
                onDismissCoordinateDialog()
                coroutineScope.launch { snackbarHostState.showSnackbar("✓ تم اعتماد الإحداثيات كموقع انطلاقك اليدوي") }
            }
        )
    }

    showAddLandmarkForPin?.let { (lat, lng) ->
        AddCustomLandmarkDialog(
            latitude = lat,
            longitude = lng,
            onDismiss = onDismissAddLandmarkForPin,
            onAdded = { saved ->
                onDismissAddLandmarkForPin()
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("✓ تم حفظ المعلم «${saved.name}» وسيظهر في البحث أوفلاين")
                }
            }
        )
    }
}
