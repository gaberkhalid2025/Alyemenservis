package com.example.ui.screens.entities

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.*
import com.example.ui.*
import com.example.ui.components.InteractiveMapLocationPickerDialog
import com.example.ui.components.ManualCoordinateDialog
import com.example.utils.LandmarkAddress
import com.example.utils.OfflineYemenGazetteer
import com.example.utils.VisualThemePalette
import kotlinx.coroutines.launch

@Composable
internal fun ProfileEditPhotosDialog(
    entityId: String,
    collectionName: String,
    provider: ProviderEntity?,
    store: StoreEntity?,
    property: PropertyEntity?,
    context: Context,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            modifier = Modifier.fillMaxWidth().padding(12.dp)
        ) {
            var avatarUrl by remember {
                mutableStateOf(
                    provider?.profileImage ?: store?.logoImage ?: ""
                )
            }
            var coverUrl by remember {
                mutableStateOf(
                    provider?.coverImage ?: store?.coverImage ?: property?.images?.firstOrNull() ?: ""
                )
            }
            var isUploadingAvatar by remember { mutableStateOf(false) }
            var isUploadingCover by remember { mutableStateOf(false) }
            val coroutineScope = rememberCoroutineScope()

            val avatarPicker = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.PickVisualMedia()
            ) { uri: android.net.Uri? ->
                if (uri != null) {
                    coroutineScope.launch {
                        isUploadingAvatar = true
                        val path = "avatars/${System.currentTimeMillis()}_$entityId.webp"
                        val result = com.example.utils.FirebaseStorageUploader.uploadImageToStorage(context, uri, path)
                        isUploadingAvatar = false
                        result.onSuccess { url ->
                            avatarUrl = url
                            Toast.makeText(context, "تم رفع الصورة الشخصية بنجاح ✅", Toast.LENGTH_SHORT).show()
                        }.onFailure { err ->
                            Toast.makeText(context, "فشل الرفع: ${err.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            val coverPicker = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.PickVisualMedia()
            ) { uri: android.net.Uri? ->
                if (uri != null) {
                    coroutineScope.launch {
                        isUploadingCover = true
                        val path = "covers/${System.currentTimeMillis()}_$entityId.webp"
                        val result = com.example.utils.FirebaseStorageUploader.uploadImageToStorage(context, uri, path)
                        isUploadingCover = false
                        result.onSuccess { url ->
                            coverUrl = url
                            Toast.makeText(context, "تم رفع صورة الغلاف بنجاح ✅", Toast.LENGTH_SHORT).show()
                        }.onFailure { err ->
                            Toast.makeText(context, "فشل الرفع: ${err.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("📸 تغيير الصورة الشخصية وصورة الغلاف", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = avatarUrl,
                        onValueChange = { avatarUrl = it },
                        label = { Text("الصورة الشخصية / الشعار", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = themeColors.textPrimary,
                            unfocusedTextColor = themeColors.textPrimary,
                            focusedLabelColor = themeColors.accent,
                            unfocusedLabelColor = themeColors.textSecondary
                        )
                    )
                    Button(
                        onClick = { avatarPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        enabled = !isUploadingAvatar,
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(54.dp)
                    ) {
                        if (isUploadingAvatar) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black)
                        } else {
                            Text("اختر 📷", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = coverUrl,
                        onValueChange = { coverUrl = it },
                        label = { Text("صورة الغلاف (Banner)", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = themeColors.textPrimary,
                            unfocusedTextColor = themeColors.textPrimary,
                            focusedLabelColor = themeColors.accent,
                            unfocusedLabelColor = themeColors.textSecondary
                        )
                    )
                    Button(
                        onClick = { coverPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        enabled = !isUploadingCover,
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(54.dp)
                    ) {
                        if (isUploadingCover) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black)
                        } else {
                            Text("اختر 🖼️", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            if (isUploadingAvatar || isUploadingCover) {
                                Toast.makeText(context, "يرجى الانتظار حتى يكتمل رفع الصورة", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            viewModel.updateEntityImages(collectionName, entityId, avatarUrl.trim(), coverUrl.trim())
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("حفظ التغييرات 💾", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("إلغاء", color = themeColors.textPrimary)
                    }
                }
            }
        }
    }
}

@Composable
internal fun ProfileEditDetailsDialog(
    provider: ProviderEntity?,
    store: StoreEntity?,
    property: PropertyEntity?,
    context: Context,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onDismiss: () -> Unit
) {
    val userLat by viewModel.userLatitude.collectAsState()
    val userLng by viewModel.userLongitude.collectAsState()
    val isRealLoc by viewModel.isUserLocationReal.collectAsState()
    val hasGps = isRealLoc && LandmarkAddress.isValid(userLat, userLng)

    var editName by remember { mutableStateOf(provider?.name ?: store?.name ?: property?.title ?: "") }
    var editPhone by remember { mutableStateOf(provider?.phone ?: store?.phone ?: property?.phone ?: "") }
    var editArea by remember {
        mutableStateOf(
            provider?.localNeighborhood?.ifBlank {
                provider.area.takeUnless { it.startsWith("ye_") }
                    ?: com.example.domain.YemenCities.getCityNameById(provider.cityId.ifBlank { provider.area })
            }
                ?: store?.localNeighborhood?.ifBlank {
                    if (store.cityId.isNotBlank()) com.example.domain.YemenCities.getCityNameById(store.cityId) else ""
                }
                ?: property?.localNeighborhood?.ifBlank {
                    if (property.cityId.isNotBlank()) com.example.domain.YemenCities.getCityNameById(property.cityId) else ""
                }
                ?: ""
        )
    }
    var editDesc by remember { mutableStateOf(provider?.profession ?: store?.description ?: property?.description ?: "") }
    var editLat by remember { mutableDoubleStateOf(provider?.latitude ?: store?.latitude ?: property?.latitude ?: 15.3694) }
    var editLng by remember { mutableDoubleStateOf(provider?.longitude ?: store?.longitude ?: property?.longitude ?: 44.1910) }
    var showEntityMapPicker by remember { mutableStateOf(false) }
    var showEntityCoordDialog by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("📝 تعديل البيانات الأساسية وموقع الخريطة", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)

                OutlinedTextField(
                    value = editName,
                    onValueChange = { editName = it },
                    label = { Text("اسم النشاط / صاحب العمل") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = themeColors.textPrimary,
                        unfocusedTextColor = themeColors.textPrimary,
                        focusedLabelColor = themeColors.accent,
                        unfocusedLabelColor = themeColors.textSecondary
                    )
                )

                OutlinedTextField(
                    value = editPhone,
                    onValueChange = { editPhone = it },
                    label = { Text("رقم الهاتف") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = themeColors.textPrimary,
                        unfocusedTextColor = themeColors.textPrimary,
                        focusedLabelColor = themeColors.accent,
                        unfocusedLabelColor = themeColors.textSecondary
                    )
                )

                OutlinedTextField(
                    value = editArea,
                    onValueChange = { editArea = it },
                    label = { Text("المدينة / المحافظة / الحي أو أقرب معلم") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = themeColors.textPrimary,
                        unfocusedTextColor = themeColors.textPrimary,
                        focusedLabelColor = themeColors.accent,
                        unfocusedLabelColor = themeColors.textSecondary
                    )
                )

                Surface(
                    color = themeColors.background,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.35f))
                ) {
                    Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "📍 إحداثيات الموقع: ${OfflineYemenGazetteer.formatCoordinates(editLat, editLng)}",
                            color = themeColors.accent,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { showEntityMapPicker = true },
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) {
                                Text("🗺️ من الخريطة", fontSize = 10.5.sp, color = Color(0xFF10B981), maxLines = 1)
                            }
                            OutlinedButton(
                                onClick = { showEntityCoordDialog = true },
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) {
                                Text("🔢 إحداثيات", fontSize = 10.5.sp, color = themeColors.accent, maxLines = 1)
                            }
                            if (hasGps) {
                                OutlinedButton(
                                    onClick = {
                                        editLat = userLat
                                        editLng = userLng
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    Text("📡 GPS", fontSize = 10.5.sp, color = Color(0xFFFBBF24), maxLines = 1)
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = editDesc,
                    onValueChange = { editDesc = it },
                    label = { Text("الوصف / التخصص / الخدمات") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = themeColors.textPrimary,
                        unfocusedTextColor = themeColors.textPrimary,
                        focusedLabelColor = themeColors.accent,
                        unfocusedLabelColor = themeColors.textSecondary
                    )
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val trimmedName = editName.trim()
                            val trimmedPhone = editPhone.trim()
                            val trimmedArea = editArea.trim()
                            val trimmedDesc = editDesc.trim()
                            if (trimmedName.isBlank()) {
                                Toast.makeText(context, "يرجى إدخال اسم النشاط أو صاحب العمل", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (trimmedPhone.isBlank()) {
                                Toast.makeText(context, "يرجى إدخال رقم الهاتف", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val knownCityIds = com.example.domain.YemenCities.list.associate {
                                com.example.utils.ArabicNormalizer.normalize(it) to com.example.domain.YemenCities.resolveCityId(it)
                            }
                            val detectedCityId = com.example.utils.CityIdResolver.resolve(trimmedArea, knownCityIds)
                            if (provider != null) {
                                val updated = provider.copy(
                                    name = trimmedName,
                                    phone = trimmedPhone,
                                    cityId = detectedCityId ?: provider.cityId,
                                    area = if (provider.area.startsWith("ye_")) (detectedCityId ?: provider.area) else trimmedArea.ifBlank { provider.area },
                                    localNeighborhood = trimmedArea,
                                    profession = trimmedDesc,
                                    latitude = editLat,
                                    longitude = editLng
                                )
                                viewModel.updateProviderEntity(updated)
                            } else if (store != null) {
                                val updated = store.copy(
                                    name = trimmedName,
                                    phone = trimmedPhone,
                                    cityId = detectedCityId ?: store.cityId,
                                    localNeighborhood = trimmedArea,
                                    description = trimmedDesc,
                                    latitude = editLat,
                                    longitude = editLng
                                )
                                viewModel.updateStoreEntity(updated)
                            } else if (property != null) {
                                val updated = property.copy(
                                    title = trimmedName,
                                    phone = trimmedPhone,
                                    cityId = detectedCityId ?: property.cityId,
                                    localNeighborhood = trimmedArea,
                                    description = trimmedDesc,
                                    latitude = editLat,
                                    longitude = editLng
                                )
                                viewModel.updatePropertyEntity(updated)
                            }
                            Toast.makeText(context, "✅ تم إرسال التعديلات والموقع للحفظ!", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("حفظ التعديلات 💾", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("إلغاء", color = themeColors.textPrimary)
                    }
                }
            }
        }
    }

    if (showEntityMapPicker) {
        InteractiveMapLocationPickerDialog(
            initialLat = editLat,
            initialLng = editLng,
            hasGps = hasGps,
            gpsLat = userLat,
            gpsLng = userLng,
            title = "📍 حدد موقع النشاط / الفني على الخريطة",
            onDismiss = { showEntityMapPicker = false },
            onLocationPicked = { res ->
                editLat = res.latitude
                editLng = res.longitude
                if (editArea.isBlank()) editArea = res.formattedAddress
                showEntityMapPicker = false
            }
        )
    }

    if (showEntityCoordDialog) {
        ManualCoordinateDialog(
            initialLat = editLat,
            initialLng = editLng,
            gpsLat = userLat,
            gpsLng = userLng,
            hasGps = hasGps,
            title = "🔢 تعديل إحداثيات النشاط",
            onDismiss = { showEntityCoordDialog = false },
            onConfirm = { lat, lng, summary ->
                editLat = lat
                editLng = lng
                if (editArea.isBlank() && summary.isNotBlank()) editArea = summary
                showEntityCoordDialog = false
            }
        )
    }
}
