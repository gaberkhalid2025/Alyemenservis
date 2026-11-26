package com.example.ui.dialogs

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.ui.components.InteractiveMapLocationPickerDialog
import com.example.ui.components.ManualCoordinateDialog
import com.example.ui.components.buildFormattedLocationAddress
import com.example.ui.screens.map.LandmarkAddressScreen
import com.example.utils.LandmarkAddress
import com.example.utils.LandmarkAddressStore
import com.example.utils.OfflineYemenGazetteer
import com.example.utils.VisualThemePalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun Step2LocationAndDetails(
    citiesList: List<String>,
    selectedCity: String,
    onCitySelect: (String) -> Unit,
    neighborhood: String,
    onNeighborhoodChange: (String) -> Unit,
    pickedLat: Double,
    pickedLng: Double,
    onCoordsPicked: (Double, Double) -> Unit,
    attachedImageUri: Uri?,
    onImageSelected: (Uri?) -> Unit,
    serviceTitle: String,
    onServiceTitleChange: (String) -> Unit,
    orderDetails: String,
    onOrderDetailsChange: (String) -> Unit,
    urgencyTime: String,
    onUrgencyChange: (String) -> Unit,
    deliveryMethod: String,
    onDeliveryMethodChange: (String) -> Unit,
    selectedSection: String,
    gpsLat: Double,
    gpsLng: Double,
    hasGps: Boolean,
    themeColors: VisualThemePalette
) {
    val context = LocalContext.current
    val landmarkStore = remember { LandmarkAddressStore(context.applicationContext) }
    var savedAddresses by remember { mutableStateOf<List<LandmarkAddress>>(emptyList()) }
    var showMapPicker by remember { mutableStateOf(false) }
    var showCoordDialog by remember { mutableStateOf(false) }
    var showLandmarkWizard by remember { mutableStateOf(false) }
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) onImageSelected(uri)
    }
    LaunchedEffect(showLandmarkWizard) {
        savedAddresses = withContext(Dispatchers.IO) { landmarkStore.getAll() }
    }
    val hasPickedCoords = LandmarkAddress.isValid(pickedLat, pickedLng)
    val effectiveLat = if (hasPickedCoords) pickedLat else gpsLat
    val effectiveLng = if (hasPickedCoords) pickedLng else gpsLng

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "2️⃣ حدد المحافظة والمنطقة:",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        // Quick City Pills
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(citiesList) { city ->
                val isSelected = selectedCity == city
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) Color(0xFF10B981) else Color(0xFF1E293B))
                        .border(
                            1.dp,
                            if (isSelected) Color(0xFF10B981) else Color.White.copy(alpha = 0.1f),
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { onCitySelect(city) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "📍 $city",
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = Color.White
                    )
                }
            }
        }

        // Neighborhood text field
        OutlinedTextField(
            value = neighborhood,
            onValueChange = onNeighborhoodChange,
            label = { Text("الحي / الشارع / المعلم أو الموقع الدقيق") },
            placeholder = { Text("اكتب موقعك أو حدده على الخريطة ليصل المندوب/الفني بالضبط") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 2,
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF10B981),
                unfocusedBorderColor = Color.DarkGray,
                focusedContainerColor = Color(0xFF1E293B),
                unfocusedContainerColor = Color(0xFF1E293B)
            )
        )

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { showMapPicker = true },
                contentPadding = PaddingValues(horizontal = 8.dp),
                modifier = Modifier.weight(1f).height(48.dp)
            ) {
                Text("📍 من الخريطة", fontSize = 11.sp, color = Color(0xFF10B981), maxLines = 1)
            }
            OutlinedButton(
                onClick = { showCoordDialog = true },
                contentPadding = PaddingValues(horizontal = 8.dp),
                modifier = Modifier.weight(1f).height(48.dp)
            ) {
                Text("🔢 إحداثيات", fontSize = 11.sp, color = Color(0xFF00E5FF), maxLines = 1)
            }
            OutlinedButton(
                onClick = { showLandmarkWizard = true },
                contentPadding = PaddingValues(horizontal = 8.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Text("🏠 بالمعلم", fontSize = 11.sp, color = Color(0xFFFBBF24), maxLines = 1)
            }
        }

        if (hasPickedCoords || hasGps) {
            Surface(
                color = Color(0xFF064E3B).copy(alpha = 0.7f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (hasPickedCoords) {
                        "✅ تم تثبيت إحداثيات الطلب: ${OfflineYemenGazetteer.formatCoordinates(pickedLat, pickedLng)}"
                    } else {
                        "📡 سيتم إرفاق إحداثيات GPS الحالية تلقائياً (${OfflineYemenGazetteer.formatCoordinates(gpsLat, gpsLng)})"
                    },
                    color = Color(0xFFA7F3D0),
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }

        if (savedAddresses.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(savedAddresses) { addr ->
                    SuggestionChip(
                        onClick = {
                            onCoordsPicked(addr.pointLat, addr.pointLng)
                            onNeighborhoodChange("${addr.describe()} (${OfflineYemenGazetteer.formatCoordinates(addr.pointLat, addr.pointLng)})")
                        },
                        label = { Text("🏠 ${addr.label} (${addr.landmarkName})", fontSize = 10.sp) }
                    )
                }
            }
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

        Text(
            text = "📝 تفاصيل ما تحتاجه بدقة:",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        // Service Title
        OutlinedTextField(
            value = serviceTitle,
            onValueChange = onServiceTitleChange,
            label = { Text("عنوان الطلب / السلعة / الخدمة (مطلوب) *") },
            placeholder = { Text("مثال: صيانة تسريب مياه / طلب مواد غذائية / شقة للإيجار") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFFEF4444),
                unfocusedBorderColor = Color.DarkGray,
                focusedContainerColor = Color(0xFF1E293B),
                unfocusedContainerColor = Color(0xFF1E293B)
            )
        )

        // Details / Description
        OutlinedTextField(
            value = orderDetails,
            onValueChange = onOrderDetailsChange,
            label = { Text("شرح إضافي / ملاحظات ومواصفات") },
            placeholder = { Text("اكتب الكميات أو المواصفات أو التفاصيل المطلوبة لتصلك عروض أسعار دقيقة...") },
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFFEF4444),
                unfocusedBorderColor = Color.DarkGray,
                focusedContainerColor = Color(0xFF1E293B),
                unfocusedContainerColor = Color(0xFF1E293B)
            )
        )

        // Optional Photo Attachment
        Surface(
            color = Color(0xFF1E293B),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("📷 إرفاق صورة توضيحية (اختياري)", fontSize = 11.5.sp, color = Color.White, fontWeight = FontWeight.Medium)
                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(if (attachedImageUri != null) "تغيير الصورة" else "اختيار صورة", fontSize = 11.sp, color = themeColors.accent)
                    }
                }
                if (attachedImageUri != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        AsyncImage(
                            model = attachedImageUri,
                            contentDescription = "معاينة الصورة المرفقة",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        IconButton(
                            onClick = { onImageSelected(null) },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(32.dp)
                                .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "إزالة الصورة", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        // Delivery / Inspection Method for Stores, Restaurants, Medical, Real Estate
        if (selectedSection != "SERVICES") {
            val deliveryOptions = if (selectedSection == "PROPERTIES") {
                listOf("معاينة ميدانية", "تواصل واتفاق مباشر")
            } else {
                listOf("توصيل للموقع", "استلام من المحل")
            }
            Text(
                text = if (selectedSection == "PROPERTIES") "🏠 طريقة المتابعة والمعاينة:" else "🛵 طريقة التوصيل والاستلام:",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = themeColors.accent
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                deliveryOptions.forEach { method ->
                    val isSel = deliveryMethod == method
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSel) Color(0xFF10B981) else Color(0xFF1E293B))
                            .border(1.dp, if (isSel) Color(0xFF10B981) else Color.DarkGray, RoundedCornerShape(8.dp))
                            .clickable { onDeliveryMethodChange(method) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = method,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Urgency selector
        Text(
            text = "⏱️ وقت التنفيذ المطلوب:",
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            color = themeColors.accent
        )
        val urgencies = listOf("فوراً (خلال 30 دقيقة)", "اليوم خلال ساعات", "تحديد موعد لاحق")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            urgencies.forEach { opt ->
                val isSel = urgencyTime == opt
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSel) Color(0xFFEF4444) else Color(0xFF1E293B))
                        .clickable { onUrgencyChange(opt) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = opt,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    if (showMapPicker) {
        InteractiveMapLocationPickerDialog(
            initialLat = if (LandmarkAddress.isValid(effectiveLat, effectiveLng)) effectiveLat else 15.3694,
            initialLng = if (LandmarkAddress.isValid(effectiveLat, effectiveLng)) effectiveLng else 44.1910,
            hasGps = hasGps,
            gpsLat = gpsLat,
            gpsLng = gpsLng,
            title = "📍 حدد موقع التوصيل / الخدمة على الخريطة",
            onDismiss = { showMapPicker = false },
            onLocationPicked = { res ->
                onCoordsPicked(res.latitude, res.longitude)
                onNeighborhoodChange(res.formattedAddress)
                if (res.note.isNotBlank() && orderDetails.isBlank()) onOrderDetailsChange(res.note)
                showMapPicker = false
            }
        )
    }

    if (showCoordDialog) {
        ManualCoordinateDialog(
            initialLat = if (LandmarkAddress.isValid(effectiveLat, effectiveLng)) effectiveLat else 15.3694,
            initialLng = if (LandmarkAddress.isValid(effectiveLat, effectiveLng)) effectiveLng else 44.1910,
            gpsLat = gpsLat,
            gpsLng = gpsLng,
            hasGps = hasGps,
            title = "🔢 إحداثيات موقع الطلب",
            onDismiss = { showCoordDialog = false },
            onConfirm = { lat, lng, summary ->
                onCoordsPicked(lat, lng)
                onNeighborhoodChange(buildFormattedLocationAddress(lat, lng, nearestSummary = summary))
                showCoordDialog = false
            }
        )
    }

    if (showLandmarkWizard) {
        Dialog(
            onDismissRequest = { showLandmarkWizard = false },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            LandmarkAddressScreen(
                centerLat = if (LandmarkAddress.isValid(effectiveLat, effectiveLng)) effectiveLat else 15.3694,
                centerLng = if (LandmarkAddress.isValid(effectiveLat, effectiveLng)) effectiveLng else 44.1910,
                hasRealLocation = hasGps,
                selectedCity = selectedCity,
                onSaved = { addr ->
                    onCoordsPicked(addr.pointLat, addr.pointLng)
                    onNeighborhoodChange("${addr.describe()} (${OfflineYemenGazetteer.formatCoordinates(addr.pointLat, addr.pointLng)})")
                },
                onClose = { showLandmarkWizard = false }
            )
        }
    }
}
