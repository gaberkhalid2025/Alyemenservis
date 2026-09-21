package com.example.ui.screens.register.forms

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
import com.example.domain.entities.RegistrationEntity
import com.example.ui.MainViewModel
import com.example.ui.registerJobPoster
import com.example.utils.VisualThemePalette
import kotlinx.coroutines.launch

/**
 * 👨‍💼 JobSeekerForm
 * Embedded form for Job Seekers / Job Applicants (استمارة متقدم لوظيفة / باحث عن عمل)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobSeekerForm(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onSuccess: () -> Unit = {}
) {
    var applicantName by remember { mutableStateOf("") }
    var applicantPhone by remember { mutableStateOf("") }
    var desiredCategory by remember { mutableStateOf("") }
    var applicantQualification by remember { mutableStateOf("") }
    var applicantExperience by remember { mutableStateOf("") }
    var selectedCity by remember { mutableStateOf("صنعاء") }
    var notes by remember { mutableStateOf("") }

    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val yemeniCities = listOf("صنعاء", "عدن", "تعز", "الحديدة", "إب", "ذمار", "حضرموت", "مأرب", "صعدة", "عمران")

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(18.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "📝 استمارة التقديم كباحث عن عمل / متقدم لوظيفة",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = themeColors.accent
            )
            Text(
                text = "يرجى تعبئة مؤهلاتك ورقم تواصلك لإدراج اسمك في قاعدة بيانات المتقدمين للوظائف وتسهيل وصول أصحاب العمل والشركات إليك.",
                fontSize = 11.sp,
                color = Color.LightGray,
                lineHeight = 16.sp
            )

            Divider(color = Color.White.copy(alpha = 0.1f))

            if (successMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF065F46)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "✅ تم تسجيل استمارتك بنجاح!",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            successMessage ?: "",
                            fontSize = 12.sp,
                            color = Color.LightGray
                        )
                    }
                }
                Button(
                    onClick = onSuccess,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("تم العودة لخيارات الانضمام", color = Color.White, fontWeight = FontWeight.Bold)
                }
            } else {
                errorMessage?.let { err ->
                    Text(
                        text = "⚠️ $err",
                        fontSize = 11.sp,
                        color = Color(0xFFEF4444),
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedTextField(
                    value = applicantName,
                    onValueChange = { applicantName = it },
                    label = { Text("الاسم الرباعي الكامل", fontSize = 12.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = themeColors.accent,
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = themeColors.accent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = applicantPhone,
                    onValueChange = { applicantPhone = it },
                    label = { Text("رقم الهاتف / الواتساب للتواصل", fontSize = 12.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = themeColors.accent,
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = themeColors.accent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = desiredCategory,
                    onValueChange = { desiredCategory = it },
                    label = { Text("المجال / الوظيفة المطلوبة (مثلاً: محاسب، مهندس، سائق...)", fontSize = 12.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = themeColors.accent,
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = themeColors.accent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = applicantQualification,
                    onValueChange = { applicantQualification = it },
                    label = { Text("المؤهل العلمي / الشهادة الدراسية", fontSize = 12.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = themeColors.accent,
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = themeColors.accent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = applicantExperience,
                    onValueChange = { applicantExperience = it },
                    label = { Text("سنوات الخبرة والمهارات العملية", fontSize = 12.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = themeColors.accent,
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = themeColors.accent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // City Selector
                var expandedCity by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expandedCity,
                    onExpandedChange = { expandedCity = !expandedCity },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedCity,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("المحافظة / المدينة الحالية", fontSize = 12.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = themeColors.accent,
                            unfocusedBorderColor = Color.Gray,
                            focusedLabelColor = themeColors.accent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCity) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedCity,
                        onDismissRequest = { expandedCity = false }
                    ) {
                        yemeniCities.forEach { city ->
                            DropdownMenuItem(
                                text = { Text(city) },
                                onClick = {
                                    selectedCity = city
                                    expandedCity = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات إضافية / رابط السيرة الذاتية (CV)", fontSize = 12.sp) },
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = themeColors.accent,
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = themeColors.accent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        if (applicantName.isBlank() || applicantPhone.isBlank()) {
                            errorMessage = "يرجى كتابة الاسم ورقم الهاتف على الأقل"
                            return@Button
                        }
                        isSubmitting = true
                        errorMessage = null
                        scope.launch {
                            try {
                                val jobObj = RegistrationEntity.Job(
                                    jobTitle = if (desiredCategory.isNotBlank()) "طلب وظيفة: $desiredCategory" else "باحث عن عمل: $applicantName",
                                    companyName = applicantName,
                                    category = if (desiredCategory.isNotBlank()) desiredCategory else applicantQualification,
                                    contactPhone = applicantPhone,
                                    contactEmail = "",
                                    city = selectedCity,
                                    requirements = "مؤهل: $applicantQualification | خبرة: $applicantExperience | $notes",
                                    passwordHash = "NO_PASS"
                                )
                                val res = viewModel.registerJobPoster(jobObj)
                                isSubmitting = false
                                if (res.isSuccess) {
                                    successMessage = "تم حفظ بيانات استمارتك بنجاح وتقديمها لمسؤول التوظيف!"
                                } else {
                                    errorMessage = res.exceptionOrNull()?.message ?: "حدث خطأ أثناء حفظ الاستمارة"
                                }
                            } catch (e: Exception) {
                                isSubmitting = false
                                errorMessage = e.message ?: "فشلت العملية"
                            }
                        }
                    },
                    enabled = !isSubmitting,
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.Black,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            "إرسال استمارة طلب الوظيفة 📩",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp
                        )
                    }
                }
            }
        }
    }
}
