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
 * 🏥 MedicalForm (استمارة تسجيل المراكز الطبية والعيادات)
 * الحقول المطلوبة: الترخيص الطبي، التخصصات الطبية، عدد الأطباء، التأمين الصحي المقبول
 */
@Composable
fun MedicalForm(
    themeColors: VisualThemePalette,
    onSubmit: (Map<String, Any>) -> Unit,
    modifier: Modifier = Modifier
) {
    var step by remember { mutableIntStateOf(1) }

    // Step 1: Facility & Admin Info
    var medicalCenterName by remember { mutableStateOf("") }
    var medicalDirectorName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    // Step 2: License & Specialties
    var medicalLicenseNumber by remember { mutableStateOf("") }
    var medicalSpecialties by remember { mutableStateOf("طب عام / باطنية / أسنان / أطفال") }
    var doctorsCount by remember { mutableStateOf("5") }
    var city by remember { mutableStateOf("صنعاء") }

    // Step 3: Insurances, Emergency & Working Hours
    var acceptedInsurance by remember { mutableStateOf("جميع شركات التأمين المعتمدة") }
    var workingHours by remember { mutableStateOf("24 ساعة (طوارئ متواصلة)") }
    var emergencyServicesAvailable by remember { mutableStateOf(true) }

    val isStep1Valid = medicalCenterName.isNotBlank() && medicalDirectorName.isNotBlank() && phone.trim().length >= 9 && password.length >= 6 && password == confirmPassword && city.isNotBlank()
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
                    text = "تسجيل منشأة طبية / عيادة",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF06B6D4).copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "المرحلة $step من 3",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF06B6D4),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            LinearProgressIndicator(
                progress = { step / 3f },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = Color(0xFF06B6D4),
                trackColor = Color(0xFF334155)
            )

            AnimatedContent(targetState = step, label = "med_wizard") { targetStep ->
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    when (targetStep) {
                        1 -> {
                            OutlinedTextField(
                                value = medicalCenterName,
                                onValueChange = { medicalCenterName = it },
                                label = { Text("اسم المركز الطبي / المستشفى / العيادة") },
                                leadingIcon = { Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFF06B6D4)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = medicalDirectorName,
                                onValueChange = { medicalDirectorName = it },
                                label = { Text("اسم المدير الطبي أو الطبيب المسؤول") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF06B6D4)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = phone,
                                onValueChange = { phone = it },
                                label = { Text("هاتف الاستقبال وحجز المواعيد") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF06B6D4)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("كلمة المرور") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF06B6D4)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it },
                                label = { Text("تأكيد كلمة المرور") },
                                isError = confirmPassword.isNotEmpty() && confirmPassword != password,
                                leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF06B6D4)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        2 -> {
                            OutlinedTextField(
                                value = medicalLicenseNumber,
                                onValueChange = { medicalLicenseNumber = it },
                                label = { Text("رقم ترخيص وزارة الصحة والمهن الطبية") },
                                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF06B6D4)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = medicalSpecialties,
                                onValueChange = { medicalSpecialties = it },
                                label = { Text("التخصصات والعيادات الطبية المتاحة") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = doctorsCount,
                                onValueChange = { doctorsCount = it },
                                label = { Text("عدد الأطباء والاستشاريين") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF06B6D4)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = city,
                                onValueChange = { city = it },
                                label = { Text("المدينة والموقع") },
                                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF06B6D4)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        3 -> {
                            OutlinedTextField(
                                value = acceptedInsurance,
                                onValueChange = { acceptedInsurance = it },
                                label = { Text("شركات التأمين الصحي المقبولة") },
                                leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF06B6D4)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = workingHours,
                                onValueChange = { workingHours = it },
                                label = { Text("أوقات الدوام وساعات استقبال المرضى") },
                                leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null, tint = Color(0xFF06B6D4)) },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("توفر قسم طوارئ واستقبال الحالات العاجلة", color = Color.White, fontSize = 13.sp)
                                Switch(
                                    checked = emergencyServicesAvailable,
                                    onCheckedChange = { emergencyServicesAvailable = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF06B6D4))
                                )
                            }
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
                                "medicalCenterName" to medicalCenterName,
                                "medicalDirectorName" to medicalDirectorName,
                                "phone" to phone,
                                "password" to password,
                                "medicalLicenseNumber" to medicalLicenseNumber,
                                "medicalSpecialties" to medicalSpecialties,
                                "doctorsCount" to doctorsCount,
                                "city" to city,
                                "acceptedInsurance" to acceptedInsurance,
                                "workingHours" to workingHours,
                                "emergencyServicesAvailable" to emergencyServicesAvailable,
                                "role" to "MEDICAL"
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
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF06B6D4), contentColor = Color.White)
                ) {
                    Text(text = if (step == 3) "إتمام تسجيل المنشأة الطبية" else "التالي", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
