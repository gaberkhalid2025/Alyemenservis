package com.example.ui.screens.register.forms

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.utils.VisualThemePalette

/**
 * 🏥 MedicalForm (استمارة تسجيل المنشآت الطبية المحدثة والمبسطة)
 * نموذج صفحة واحدة مدمج مع التحقق الفوري للمتطلبات الإجبارية.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicalForm(
    themeColors: VisualThemePalette,
    onSubmit: (Map<String, Any>) -> Unit,
    modifier: Modifier = Modifier
) {
    // Mandatory fields
    var medicalCenterName by remember { mutableStateOf("") }
    var medicalDirectorName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("صنعاء") }

    // Optional fields
    var medicalLicenseNumber by remember { mutableStateOf("") }
    var medicalSpecialties by remember { mutableStateOf("عام وتخصصي") }
    var doctorsCount by remember { mutableStateOf("") }
    var acceptedInsurance by remember { mutableStateOf("") }
    var workingHours by remember { mutableStateOf("") }
    var emergencyServicesAvailable by remember { mutableStateOf(true) }

    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    // Validation (Center Name, Triple Director Name, Phone >= 9, matching password >= 6, and city)
    val directorNamePartsCount = medicalDirectorName.trim().split(" ").filter { it.isNotBlank() }.size
    val isFormValid = medicalCenterName.isNotBlank() &&
            directorNamePartsCount >= 3 &&
            phone.trim().length >= 9 &&
            password.length >= 6 &&
            password == confirmPassword &&
            city.isNotBlank()

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "يرجى ملء البيانات (الحقول بعلامة * إجبارية):",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = themeColors.accent
            )

            // 1. Center Name
            OutlinedTextField(
                value = medicalCenterName,
                onValueChange = { medicalCenterName = it },
                label = { Text("اسم المركز الطبي / العيادة / المستشفى *") },
                placeholder = { Text("مثال: مركز العاصمة الطبي") },
                leadingIcon = { Icon(Icons.Default.Star, contentDescription = null, tint = themeColors.accent, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
            )

            // 2. Director Name (Triple)
            OutlinedTextField(
                value = medicalDirectorName,
                onValueChange = { medicalDirectorName = it },
                label = { Text("اسم المدير الطبي أو الطبيب المسؤول * (ثلاثي)") },
                placeholder = { Text("مثال: د. علي أحمد السعدي") },
                isError = medicalDirectorName.isNotEmpty() && directorNamePartsCount < 3,
                supportingText = {
                    if (medicalDirectorName.isNotEmpty() && directorNamePartsCount < 3) {
                        Text("يجب إدخال الاسم ثلاثياً على الأقل", color = Color.Red, fontSize = 10.sp)
                    }
                },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = themeColors.accent, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
            )

            // 3. Phone Number
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("هاتف حجز المواعيد والاستقبال * (9 أرقام)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                isError = phone.isNotEmpty() && phone.trim().length < 9,
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = themeColors.accent, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
            )

            // 4. Password
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("كلمة المرور * (6 خانات على الأقل)") },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = themeColors.accent, modifier = Modifier.size(16.dp)) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
            )

            // 5. Confirm Password
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("تأكيد كلمة المرور *") },
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                isError = confirmPassword.isNotEmpty() && confirmPassword != password,
                supportingText = {
                    if (confirmPassword.isNotEmpty() && confirmPassword != password) {
                        Text("كلمتا المرور غير متطابقتين", color = Color.Red, fontSize = 10.sp)
                    }
                },
                leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = themeColors.accent, modifier = Modifier.size(16.dp)) },
                trailingIcon = {
                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                        Icon(
                            imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
            )

            // 6. City Dropdown
            val cities = listOf("صنعاء", "عدن", "تعز", "الحديدة", "إب", "حضرموت")
            var expandedCity by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expandedCity,
                onExpandedChange = { expandedCity = !expandedCity }
            ) {
                OutlinedTextField(
                    value = city,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("المدينة / المحافظة *") },
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = themeColors.accent, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                )
                ExposedDropdownMenu(
                    expanded = expandedCity,
                    onDismissRequest = { expandedCity = false }
                ) {
                    cities.forEach { c ->
                        DropdownMenuItem(
                            text = { Text(c, fontSize = 12.sp) },
                            onClick = {
                                city = c
                                expandedCity = false
                            }
                        )
                    }
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))

            Text(
                text = "بيانات طبية اختيارية:",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.LightGray
            )

            // 7. Medical License (Optional)
            OutlinedTextField(
                value = medicalLicenseNumber,
                onValueChange = { medicalLicenseNumber = it },
                label = { Text("رقم ترخيص وزارة الصحة (اختياري)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
            )

            // 8. Specialties (Optional)
            OutlinedTextField(
                value = medicalSpecialties,
                onValueChange = { medicalSpecialties = it },
                label = { Text("العيادات والتخصصات المتوفرة (اختياري)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
            )

            // 9. Doctors Count (Optional)
            OutlinedTextField(
                value = doctorsCount,
                onValueChange = { doctorsCount = it },
                label = { Text("عدد الأطباء وطاقم الاستشاريين (اختياري)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
            )

            // 10. Insurance (Optional)
            OutlinedTextField(
                value = acceptedInsurance,
                onValueChange = { acceptedInsurance = it },
                label = { Text("شركات التأمين المقبولة إن وجدت (اختياري)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
            )

            // 11. Working Hours (Optional)
            OutlinedTextField(
                value = workingHours,
                onValueChange = { workingHours = it },
                label = { Text("ساعات الدوام (اختياري)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("توفر طوارئ 24 ساعة (اختياري)", color = Color.White, fontSize = 12.sp)
                Switch(
                    checked = emergencyServicesAvailable,
                    onCheckedChange = { emergencyServicesAvailable = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = themeColors.accent)
                )
            }

            Button(
                onClick = {
                    val data = mapOf(
                        "medicalCenterName" to medicalCenterName,
                        "medicalDirectorName" to medicalDirectorName,
                        "phone" to phone,
                        "password" to password,
                        "medicalLicenseNumber" to medicalLicenseNumber,
                        "medicalSpecialties" to medicalSpecialties.ifBlank { "عيادات عامة" },
                        "doctorsCount" to doctorsCount.ifBlank { "1" },
                        "city" to city,
                        "acceptedInsurance" to acceptedInsurance,
                        "workingHours" to workingHours.ifBlank { "8:00 ص - 8:00 م" },
                        "emergencyServicesAvailable" to emergencyServicesAvailable,
                        "role" to "MEDICAL"
                    )
                    onSubmit(data)
                },
                enabled = isFormValid,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().height(42.dp),
                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent, contentColor = Color.Black)
            ) {
                Text("إرسال طلب تسجيل المنشأة الطبية 🚀", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}
