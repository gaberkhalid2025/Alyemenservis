package com.example.ui.screens.register.forms

import androidx.compose.animation.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.utils.VisualThemePalette

/**
 * 🛠️ ProviderForm (استمارة تسجيل الفني والمهني)
 * نموذج تسجيل تفاعلي من 3 مراحل (Wizard) مع تحقق فوري وحفظ تلقائي:
 * - المرحلة 1: البيانات الأساسية وحساب الدخول
 * - المرحلة 2: الخبرات، الشهادات، واللغات
 * - المرحلة 3: نطاق العمل الجغرافي، أوقات التوفر، وسعر الخدمة الأساسي
 */
@Composable
fun ProviderForm(
    themeColors: VisualThemePalette,
    onSubmit: (Map<String, Any>) -> Unit,
    modifier: Modifier = Modifier
) {
    var step by remember { mutableIntStateOf(1) }

    // Phase 1 Fields
    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    // Phase 2 Fields (Required in Prompt)
    var yearsOfExperience by remember { mutableStateOf("") }
    var certifications by remember { mutableStateOf("") }
    var languages by remember { mutableStateOf("العربية") }
    var craftType by remember { mutableStateOf("كهرباء / سباكة / تكييف") }

    // Phase 3 Fields (Required in Prompt)
    var geographicalScope by remember { mutableStateOf("صنعاء وضواحيها") }
    var availabilityHours by remember { mutableStateOf("8:00 ص - 8:00 م") }
    var baseServicePrice by remember { mutableStateOf("") }
    var verificationDocLevel by remember { mutableIntStateOf(1) }

    // Live validation states
    val isStep1Valid = fullName.trim().length >= 3 && phone.trim().length >= 9 && password.length >= 6 && password == confirmPassword
    val isStep2Valid = yearsOfExperience.isNotBlank() && craftType.isNotBlank()
    val isStep3Valid = geographicalScope.isNotBlank() && baseServicePrice.isNotBlank()

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
            // Wizard Header Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "تسجيل فني / مهني جديد",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF00E5FF).copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "المرحلة $step من 3",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00E5FF),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            LinearProgressIndicator(
                progress = { step / 3f },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = Color(0xFF00E5FF),
                trackColor = Color(0xFF334155)
            )

            AnimatedContent(targetState = step, label = "wizard_step") { targetStep ->
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    when (targetStep) {
                        1 -> {
                            // Step 1: Basic Info & Authentication
                            OutlinedTextField(
                                value = fullName,
                                onValueChange = { fullName = it },
                                label = { Text("الاسم الكامل واللقب") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF00E5FF)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = phone,
                                onValueChange = { phone = it },
                                label = { Text("رقم الهاتف (واتساب والمكالمات)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF00E5FF)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("كلمة المرور (6 خانات على الأقل)") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF00E5FF)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it },
                                label = { Text("تأكيد كلمة المرور") },
                                isError = confirmPassword.isNotEmpty() && confirmPassword != password,
                                supportingText = {
                                    if (confirmPassword.isNotEmpty() && confirmPassword != password) {
                                        Text("كلمتا المرور غير متطابقتين", color = Color(0xFFEF4444))
                                    }
                                },
                                leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF00E5FF)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }

                        2 -> {
                            // Step 2: Experience & Qualifications
                            OutlinedTextField(
                                value = craftType,
                                onValueChange = { craftType = it },
                                label = { Text("التخصص المهني الأساسي") },
                                leadingIcon = { Icon(Icons.Default.Build, contentDescription = null, tint = Color(0xFF00E5FF)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = yearsOfExperience,
                                onValueChange = { yearsOfExperience = it },
                                label = { Text("سنوات الخبرة العملية (مثال: 5 سنوات)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                leadingIcon = { Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFF00E5FF)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = certifications,
                                onValueChange = { certifications = it },
                                label = { Text("الشهادات المهنية والدورات التدريبية") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = languages,
                                onValueChange = { languages = it },
                                label = { Text("اللغات المتقنة (مثال: العربية، الإنجليزية)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        3 -> {
                            // Step 3: Location, Pricing & Working Hours
                            OutlinedTextField(
                                value = geographicalScope,
                                onValueChange = { geographicalScope = it },
                                label = { Text("نطاق العمل الجغرافي والمدن المشمولة") },
                                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF00E5FF)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = availabilityHours,
                                onValueChange = { availabilityHours = it },
                                label = { Text("أوقات التوفر وأيام العمل") },
                                leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null, tint = Color(0xFF00E5FF)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = baseServicePrice,
                                onValueChange = { baseServicePrice = it },
                                label = { Text("سعر الخدمة الأساسي / كشف المعاينة (ريال)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                leadingIcon = { Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = Color(0xFF00E5FF)) },
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Document Verification Level Selector (3 Levels)
                            Text(
                                text = "مستوى التوثيق والاعتماد:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF94A3B8)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(1 to "أساسي", 2 to "موثق بالهوية", 3 to "معتمد مهنياً").forEach { (lvl, title) ->
                                    FilterChip(
                                        selected = verificationDocLevel == lvl,
                                        onClick = { verificationDocLevel = lvl },
                                        label = { Text(title, fontSize = 11.sp) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Navigation Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (step > 1) {
                    OutlinedButton(
                        onClick = { step-- },
                        shape = RoundedCornerShape(10.dp)
                    ) {
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
                                "fullName" to fullName,
                                "phone" to phone,
                                "password" to password,
                                "yearsOfExperience" to yearsOfExperience,
                                "certifications" to certifications,
                                "languages" to languages,
                                "craftType" to craftType,
                                "geographicalScope" to geographicalScope,
                                "availabilityHours" to availabilityHours,
                                "baseServicePrice" to baseServicePrice,
                                "verificationDocLevel" to verificationDocLevel,
                                "role" to "PROVIDER"
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
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF), contentColor = Color(0xFF0F172A))
                ) {
                    Text(
                        text = if (step == 3) "إتمام التسجيل" else "التالي",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
