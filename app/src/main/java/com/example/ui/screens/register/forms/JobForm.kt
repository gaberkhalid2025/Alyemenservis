package com.example.ui.screens.register.forms

import androidx.compose.animation.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.utils.VisualThemePalette

/**
 * 💼 JobForm (استمارة تسجيل جهات التوظيف وعروض العمل)
 * الحقول المطلوبة: نوع الوظيفة، المؤهلات، الخبرات، المهارات، تاريخ بدء العمل
 */
@Composable
fun JobForm(
    themeColors: VisualThemePalette,
    onSubmit: (Map<String, Any>) -> Unit,
    modifier: Modifier = Modifier
) {
    var step by remember { mutableIntStateOf(1) }

    // Step 1: Employer Identity & Auth
    var companyName by remember { mutableStateOf("") }
    var recruiterName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    // Step 2: Job Specs & Requirements
    var jobTitle by remember { mutableStateOf("") }
    var jobType by remember { mutableStateOf("دوام كامل") } // دوام كامل / دوام جزئي / عن بعد
    var requiredQualifications by remember { mutableStateOf("بكالوريوس أو ما يعادلها") }
    var requiredExperience by remember { mutableStateOf("سنتان على الأقل") }
    var requiredSkills by remember { mutableStateOf("إجادة استخدام الحاسوب، العمل الجماعي، حل المشكلات") }

    // Step 3: Location, Salary & Start Date
    var city by remember { mutableStateOf("صنعاء") }
    var workStartDate by remember { mutableStateOf("خلال أسبوعين من تاريخ القبول") }
    var salaryRange by remember { mutableStateOf("يحدد بعد المقابلة بناء على الكفاءة") }

    val isStep1Valid = companyName.isNotBlank() && recruiterName.isNotBlank() && phone.trim().length >= 9 && password.length >= 6 && password == confirmPassword && city.isNotBlank()
    val isStep2Valid = true // Optional fields
    val isStep3Valid = true // Optional fields

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "تسجيل جهة توظيف / إعلان وظيفة",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFEC4899).copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "المرحلة $step من 3",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEC4899),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            LinearProgressIndicator(
                progress = { step / 3f },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = Color(0xFFEC4899),
                trackColor = Color(0xFF334155)
            )

            AnimatedContent(targetState = step, label = "job_wizard") { targetStep ->
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    when (targetStep) {
                        1 -> {
                            OutlinedTextField(
                                value = companyName,
                                onValueChange = { companyName = it },
                                label = { Text("اسم الشركة / المؤسسة / جهة العمل") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFFEC4899)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = recruiterName,
                                onValueChange = { recruiterName = it },
                                label = { Text("اسم مسؤول التوظيف أو الموارد البشرية") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFFEC4899)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = phone,
                                onValueChange = { phone = it },
                                label = { Text("رقم هاتف التواصل واستقبال السير الذاتية") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFFEC4899)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("كلمة المرور") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFEC4899)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it },
                                label = { Text("تأكيد كلمة المرور") },
                                isError = confirmPassword.isNotEmpty() && confirmPassword != password,
                                leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFFEC4899)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        2 -> {
                            OutlinedTextField(
                                value = jobTitle,
                                onValueChange = { jobTitle = it },
                                label = { Text("المسمى الوظيفي المطلوب") },
                                leadingIcon = { Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFEC4899)) },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Text(
                                text = "نوع الوظيفة:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF94A3B8)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("دوام كامل", "دوام جزئي", "عن بعد").forEach { type ->
                                    FilterChip(
                                        selected = jobType == type,
                                        onClick = { jobType = type },
                                        label = { Text(type, fontSize = 11.sp) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = requiredQualifications,
                                onValueChange = { requiredQualifications = it },
                                label = { Text("المؤهلات الأكاديمية المطلوبة") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = requiredExperience,
                                onValueChange = { requiredExperience = it },
                                label = { Text("الخبرات المهنية وسنوات العمل المطلوبة") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = requiredSkills,
                                onValueChange = { requiredSkills = it },
                                label = { Text("المهارات والقدرات المطلوبة") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2
                            )
                        }

                        3 -> {
                            OutlinedTextField(
                                value = city,
                                onValueChange = { city = it },
                                label = { Text("المدينة ومقر العمل") },
                                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFFEC4899)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = workStartDate,
                                onValueChange = { workStartDate = it },
                                label = { Text("تاريخ بدء العمل المتوقع") },
                                leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null, tint = Color(0xFFEC4899)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = salaryRange,
                                onValueChange = { salaryRange = it },
                                label = { Text("المميزات والراتب المتوقع") },
                                leadingIcon = { Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = Color(0xFFEC4899)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (step > 1) {
                    OutlinedButton(onClick = { step-- }, shape = RoundedCornerShape(10.dp)) {
                        Text("السابق", color = Color.White)
                    }
                } else {
                    Spacer(modifier = Modifier.width(10.dp))
                }

                Button(
                    onClick = {
                        if (step < 3) {
                            step++
                        } else {
                            val data = mapOf(
                                "companyName" to companyName,
                                "recruiterName" to recruiterName,
                                "phone" to phone,
                                "password" to password,
                                "jobTitle" to jobTitle,
                                "jobType" to jobType,
                                "requiredQualifications" to requiredQualifications,
                                "requiredExperience" to requiredExperience,
                                "requiredSkills" to requiredSkills,
                                "city" to city,
                                "workStartDate" to workStartDate,
                                "salaryRange" to salaryRange,
                                "role" to "JOB"
                            )
                            onSubmit(data)
                        }
                    },
                    enabled = when (step) {
                        1 -> isStep1Valid
                        2 -> isStep2Valid
                        3 -> isStep3Valid
                        else -> false
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEC4899), contentColor = Color.White)
                ) {
                    Text(text = if (step == 3) "إتمام تسجيل جهة التوظيف" else "التالي", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
