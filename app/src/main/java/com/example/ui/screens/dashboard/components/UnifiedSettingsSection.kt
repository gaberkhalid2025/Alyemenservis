package com.example.ui.screens.dashboard.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import com.example.ui.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.InteractiveMapLocationPickerDialog
import com.example.utils.LandmarkAddress
import com.example.utils.OfflineYemenGazetteer
import com.example.utils.VisualThemePalette

@Composable
fun UnifiedSettingsSection(
    name: String,
    phone: String,
    cityArea: String,
    description: String,
    workingHours: String = "",
    isAvailable: Boolean = true,
    latitude: Double = 0.0,
    longitude: Double = 0.0,
    themeColors: VisualThemePalette,
    onSaveProfile: (name: String, phone: String, cityArea: String, description: String, workingHours: String, isAvailable: Boolean) -> Unit,
    onSaveLocation: ((lat: Double, lng: Double) -> Unit)? = null,
    onSaveProfileWithLocation: ((name: String, phone: String, cityArea: String, description: String, workingHours: String, isAvailable: Boolean, lat: Double?, lng: Double?) -> Unit)? = null,
    onChangePassword: (oldPass: String, newPass: String) -> Unit
) {
    var nameInput by remember(name) { mutableStateOf(name) }
    var phoneInput by remember(phone) { mutableStateOf(phone) }
    var cityAreaInput by remember(cityArea) { mutableStateOf(cityArea) }
    var descInput by remember(description) { mutableStateOf(description) }
    var hoursInput by remember(workingHours) { mutableStateOf(workingHours) }
    var availableState by remember(isAvailable) { mutableStateOf(isAvailable) }
    var phoneError by remember { mutableStateOf<String?>(null) }

    var pickedLat by remember(latitude) { mutableDoubleStateOf(latitude) }
    var pickedLng by remember(longitude) { mutableDoubleStateOf(longitude) }
    var showMapPicker by remember { mutableStateOf(false) }

    var oldPasswordInput by remember { mutableStateOf("") }
    var newPasswordInput by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Business / Profile Details Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, themeColors.border)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(text = "📝 تعديل البيانات العامة والمهنية", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)

                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("الاسم / اسم المنشأة", color = themeColors.textSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = themeColors.accent,
                        unfocusedBorderColor = themeColors.border,
                        focusedTextColor = themeColors.textPrimary,
                        unfocusedTextColor = themeColors.textPrimary
                    )
                )

                OutlinedTextField(
                    value = phoneInput,
                    onValueChange = {
                        phoneInput = it
                        phoneError = null
                    },
                    label = { Text("رقم الهاتف للتواصل 📞", color = themeColors.textSecondary) },
                    isError = phoneError != null,
                    supportingText = phoneError?.let { err ->
                        { Text(err, color = MaterialTheme.colorScheme.error, fontSize = 11.sp) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = themeColors.accent,
                        unfocusedBorderColor = themeColors.border,
                        focusedTextColor = themeColors.textPrimary,
                        unfocusedTextColor = themeColors.textPrimary
                    )
                )

                OutlinedTextField(
                    value = cityAreaInput,
                    onValueChange = { cityAreaInput = it },
                    label = { Text("المدينة والحي", color = themeColors.textSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = themeColors.accent,
                        unfocusedBorderColor = themeColors.border,
                        focusedTextColor = themeColors.textPrimary,
                        unfocusedTextColor = themeColors.textPrimary
                    )
                )

                if (onSaveLocation != null || onSaveProfileWithLocation != null) {
                    Surface(
                        color = themeColors.background,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("📍 إحداثيات الموقع على الخريطة", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = themeColors.textPrimary)
                                Text(
                                    text = if (LandmarkAddress.isValid(pickedLat, pickedLng)) {
                                        OfflineYemenGazetteer.formatCoordinates(pickedLat, pickedLng)
                                    } else {
                                        "لم يتم تحديد الموقع الجغرافي بعد"
                                    },
                                    fontSize = 11.sp,
                                    color = themeColors.accent
                                )
                            }
                            OutlinedButton(
                                onClick = { showMapPicker = true },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFF10B981))
                            ) {
                                Text("🗺️ تحديد", fontSize = 11.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = descInput,
                    onValueChange = { descInput = it },
                    label = { Text("الوصف التعريفي / التخصص", color = themeColors.textSecondary) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = themeColors.accent,
                        unfocusedBorderColor = themeColors.border,
                        focusedTextColor = themeColors.textPrimary,
                        unfocusedTextColor = themeColors.textPrimary
                    )
                )

                OutlinedTextField(
                    value = hoursInput,
                    onValueChange = { hoursInput = it },
                    label = { Text("ساعات أوقات العمل (مثال: 8 ص - 10 م)", color = themeColors.textSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = themeColors.accent,
                        unfocusedBorderColor = themeColors.border,
                        focusedTextColor = themeColors.textPrimary,
                        unfocusedTextColor = themeColors.textPrimary
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (availableState) "حالة التوفر: متاح لاستقبال الطلبات 🟢" else "حالة التوفر: مشغول / غير متاح 🔴",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (availableState) Color(0xFF10B981) else Color(0xFFEF4444)
                    )
                    Switch(
                        checked = availableState,
                        onCheckedChange = { availableState = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF10B981))
                    )
                }

                Button(
                    onClick = {
                        val normalizedInputPhone = com.example.domain.usecases.ValidatePhoneUseCase.normalizePhone(phoneInput)
                        val normalizedInitialPhone = com.example.domain.usecases.ValidatePhoneUseCase.normalizePhone(phone)
                        if (phoneInput.isNotBlank() &&
                            normalizedInputPhone != normalizedInitialPhone &&
                            !Regex("^(77|78|73|71|70)\\d{7}$").matches(normalizedInputPhone)
                        ) {
                            phoneError = "يرجى إدخال رقم هاتف يمني صحيح من 9 أرقام (77/78/73/71/70)"
                            return@Button
                        }
                        val validLat = pickedLat.takeIf { LandmarkAddress.isValid(pickedLat, pickedLng) }
                        val validLng = pickedLng.takeIf { LandmarkAddress.isValid(pickedLat, pickedLng) }
                        if (onSaveProfileWithLocation != null) {
                            onSaveProfileWithLocation(
                                nameInput,
                                phoneInput,
                                cityAreaInput,
                                descInput,
                                hoursInput,
                                availableState,
                                validLat,
                                validLng
                            )
                        } else {
                            onSaveProfile(nameInput, phoneInput, cityAreaInput, descInput, hoursInput, availableState)
                            if (validLat != null && validLng != null) {
                                onSaveLocation?.invoke(validLat, validLng)
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("حفظ التغيرات 💾", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }
        }

        // Change Password Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, themeColors.border)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(text = "🔒 تغيير كلمة المرور", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)

                OutlinedTextField(
                    value = oldPasswordInput,
                    onValueChange = { oldPasswordInput = it },
                    label = { Text("كلمة المرور الحالية", color = themeColors.textSecondary) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = themeColors.accent,
                        unfocusedBorderColor = themeColors.border,
                        focusedTextColor = themeColors.textPrimary,
                        unfocusedTextColor = themeColors.textPrimary
                    )
                )

                OutlinedTextField(
                    value = newPasswordInput,
                    onValueChange = { newPasswordInput = it },
                    label = { Text("كلمة المرور الجديدة", color = themeColors.textSecondary) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = themeColors.accent,
                        unfocusedBorderColor = themeColors.border,
                        focusedTextColor = themeColors.textPrimary,
                        unfocusedTextColor = themeColors.textPrimary
                    )
                )

                Button(
                    onClick = {
                        if (oldPasswordInput.isNotBlank() && newPasswordInput.isNotBlank()) {
                            onChangePassword(oldPasswordInput, newPasswordInput)
                            oldPasswordInput = ""
                            newPasswordInput = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = oldPasswordInput.isNotBlank() && newPasswordInput.isNotBlank()
                ) {
                    Text("تحديث كلمة المرور 🔑", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }

    if (showMapPicker) {
        val initLat = if (LandmarkAddress.isValid(pickedLat, pickedLng)) pickedLat else 15.3694
        val initLng = if (LandmarkAddress.isValid(pickedLat, pickedLng)) pickedLng else 44.1910
        InteractiveMapLocationPickerDialog(
            initialLat = initLat,
            initialLng = initLng,
            hasGps = LandmarkAddress.isValid(pickedLat, pickedLng),
            gpsLat = initLat,
            gpsLng = initLng,
            title = "📍 حدد موقعك الجغرافي على الخريطة",
            onDismiss = { showMapPicker = false },
            onLocationPicked = { res ->
                pickedLat = res.latitude
                pickedLng = res.longitude
                if (cityAreaInput.isBlank() && res.formattedAddress.isNotBlank()) {
                    cityAreaInput = res.formattedAddress
                }
                if (onSaveProfileWithLocation != null) {
                    onSaveProfileWithLocation(
                        nameInput,
                        phoneInput,
                        cityAreaInput,
                        descInput,
                        hoursInput,
                        availableState,
                        res.latitude,
                        res.longitude
                    )
                } else {
                    onSaveLocation?.invoke(res.latitude, res.longitude)
                }
                showMapPicker = false
            }
        )
    }
}
