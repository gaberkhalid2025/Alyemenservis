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
 * 💼 JobForm (استمارة تسجيل التوظيف المحدثة والمبسطة)
 * نموذج صفحة واحدة مدمج مع التحقق الفوري للمتطلبات الإجبارية.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobForm(
    themeColors: VisualThemePalette,
    onSubmit: (Map<String, Any>) -> Unit,
    modifier: Modifier = Modifier
) {
    // Mandatory fields
    var companyName by remember { mutableStateOf("") }
    var recruiterName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("صنعاء") }

    // Optional fields
    var jobTitle by remember { mutableStateOf("") }
    var jobType by remember { mutableStateOf("دوام كامل") }
    var requiredQualifications by remember { mutableStateOf("") }
    var requiredExperience by remember { mutableStateOf("") }
    var requiredSkills by remember { mutableStateOf("") }
    var workStartDate by remember { mutableStateOf("") }
    var salaryRange by remember { mutableStateOf("") }

    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    // Validation (Company Name, Triple Recruiter Name, Phone >= 9, matching password >= 6, and city)
    val recruiterNamePartsCount = recruiterName.trim().split(" ").filter { it.isNotBlank() }.size
    val isFormValid = companyName.isNotBlank() &&
            recruiterNamePartsCount >= 3 &&
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

            // 1. Company Name
            OutlinedTextField(
                value = companyName,
                onValueChange = { companyName = it },
                label = { Text("اسم جهة العمل / الشركة / المؤسسة *") },
                placeholder = { Text("مثال: شركة الحلول البرمجية الذكية") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = themeColors.accent, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
            )

            // 2. Recruiter Name (Triple)
            OutlinedTextField(
                value = recruiterName,
                onValueChange = { recruiterName = it },
                label = { Text("اسم مسؤول التوظيف المعتمد * (ثلاثي)") },
                placeholder = { Text("مثال: مروان عبدالله العتمي") },
                isError = recruiterName.isNotEmpty() && recruiterNamePartsCount < 3,
                supportingText = {
                    if (recruiterName.isNotEmpty() && recruiterNamePartsCount < 3) {
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
                label = { Text("رقم هاتف التواصل والتقديم * (9 أرقام)") },
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
                    label = { Text("المدينة / المقر الرئيسي *") },
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
                text = "تفاصيل الوظيفة الشاغرة (اختياري):",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.LightGray
            )

            // 7. Job Title (Optional)
            OutlinedTextField(
                value = jobTitle,
                onValueChange = { jobTitle = it },
                label = { Text("المسمى الوظيفي المطلوب (اختياري)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
            )

            // 8. Job Type (Optional)
            Text(
                text = "نوع العمل الشاغر (اختياري):",
                fontSize = 11.sp,
                color = Color.LightGray
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("دوام كامل", "دوام جزئي", "عن بعد").forEach { t ->
                    FilterChip(
                        selected = jobType == t,
                        onClick = { jobType = t },
                        label = { Text(t, fontSize = 11.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 9. Required Experience (Optional)
            OutlinedTextField(
                value = requiredExperience,
                onValueChange = { requiredExperience = it },
                label = { Text("الخبرات المطلوبة للوظيفة (اختياري)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
            )

            // 10. Required Skills (Optional)
            OutlinedTextField(
                value = requiredSkills,
                onValueChange = { requiredSkills = it },
                label = { Text("المهارات الأساسية (اختياري)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
            )

            Button(
                onClick = {
                    val data = mapOf(
                        "companyName" to companyName,
                        "recruiterName" to recruiterName,
                        "phone" to phone,
                        "password" to password,
                        "jobTitle" to jobTitle.ifBlank { "وظيفة عامة" },
                        "jobType" to jobType,
                        "requiredQualifications" to requiredQualifications.ifBlank { "غير محدد" },
                        "requiredExperience" to requiredExperience.ifBlank { "بدون خبرة" },
                        "requiredSkills" to requiredSkills,
                        "city" to city,
                        "workStartDate" to workStartDate.ifBlank { "فوري" },
                        "salaryRange" to salaryRange.ifBlank { "يحدد لاحقاً" },
                        "role" to "JOB"
                    )
                    onSubmit(data)
                },
                enabled = isFormValid,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().height(42.dp),
                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent, contentColor = Color.Black)
            ) {
                Text("إرسال طلب تسجيل جهة التوظيف 🚀", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}
