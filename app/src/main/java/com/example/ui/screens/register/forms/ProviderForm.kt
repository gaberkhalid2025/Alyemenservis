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
4:  * 🛠️ ProviderForm (استمارة تسجيل الفني والمهني المحدثة والمبسطة)
5:  * نموذج صفحة واحدة مدمج وموفر للمساحة مع التحقق الفوري للمتطلبات الإجبارية.
6:  */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderForm(
    themeColors: VisualThemePalette,
    onSubmit: (Map<String, Any>) -> Unit,
    modifier: Modifier = Modifier
) {
    // Mandatory fields
    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("صنعاء") }

    // Optional fields
    var craftType by remember { mutableStateOf("") }
    var yearsOfExperience by remember { mutableStateOf("") }
    var certifications by remember { mutableStateOf("") }
    var languages by remember { mutableStateOf("العربية") }
    var geographicalScope by remember { mutableStateOf("") }
    var availabilityHours by remember { mutableStateOf("") }
    var baseServicePrice by remember { mutableStateOf("") }

    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    // Validation logic (Mandatory fields only)
    val isFormValid = fullName.trim().split(" ").filter { it.isNotBlank() }.size >= 3 &&
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

            // 1. Full Name (Triple)
            val namePartsCount = fullName.trim().split(" ").filter { it.isNotBlank() }.size
            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text("الاسم الثلاثي واللقب * (مطلوب ثلاثي)") },
                placeholder = { Text("مثال: علي محمد صالح") },
                isError = fullName.isNotEmpty() && namePartsCount < 3,
                supportingText = {
                    if (fullName.isNotEmpty() && namePartsCount < 3) {
                        Text("يجب إدخال الاسم ثلاثياً على الأقل", color = Color.Red, fontSize = 10.sp)
                    }
                },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = themeColors.accent, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
            )

            // 2. Phone Number
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("رقم الهاتف * (9 أرقام)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                isError = phone.isNotEmpty() && phone.trim().length < 9,
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = themeColors.accent, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
            )

            // 3. Password
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

            // 4. Confirm Password
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

            // 5. City Dropdown
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
                text = "بيانات مهنية اختيارية (لتسهيل اختيارك من قبل العملاء):",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.LightGray
            )

            // 6. Craft Type (Optional)
            OutlinedTextField(
                value = craftType,
                onValueChange = { craftType = it },
                label = { Text("التخصص المهني (اختياري)") },
                placeholder = { Text("مثال: سباك، كهربائي منازل") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
            )

            // 7. Years of Experience (Optional)
            OutlinedTextField(
                value = yearsOfExperience,
                onValueChange = { yearsOfExperience = it },
                label = { Text("سنوات الخبرة (اختياري)") },
                placeholder = { Text("مثال: 5 سنوات") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
            )

            // 8. Base Price (Optional)
            OutlinedTextField(
                value = baseServicePrice,
                onValueChange = { baseServicePrice = it },
                label = { Text("سعر معاينة الخدمة التقديري - ريال (اختياري)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
            )

            // 9. Working Hours (Optional)
            OutlinedTextField(
                value = availabilityHours,
                onValueChange = { availabilityHours = it },
                label = { Text("أوقات التوفر والعمل (اختياري)") },
                placeholder = { Text("مثال: 8:00 ص - 8:00 م") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
            )

            // 10. Scope (Optional)
            OutlinedTextField(
                value = geographicalScope,
                onValueChange = { geographicalScope = it },
                label = { Text("النطاق الجغرافي / الحارات المشمولة (اختياري)") },
                placeholder = { Text("مثال: حي الروضة وتفرعاته") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
            )

            Button(
                onClick = {
                    val data = mapOf(
                        "fullName" to fullName,
                        "phone" to phone,
                        "password" to password,
                        "city" to city,
                        "craftType" to craftType.ifBlank { "صيانة عامة" },
                        "yearsOfExperience" to yearsOfExperience.ifBlank { "1" },
                        "certifications" to certifications,
                        "languages" to languages,
                        "geographicalScope" to geographicalScope.ifBlank { city },
                        "availabilityHours" to availabilityHours.ifBlank { "على مدار الساعة" },
                        "baseServicePrice" to baseServicePrice.ifBlank { "0" },
                        "verificationDocLevel" to 1,
                        "role" to "PROVIDER"
                    )
                    onSubmit(data)
                },
                enabled = isFormValid,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().height(42.dp),
                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent, contentColor = Color.Black)
            ) {
                Text("إرسال طلب الانضمام والتسجيل 🚀", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}
