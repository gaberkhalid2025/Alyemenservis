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
 * 🍽️ RestaurantForm (استمارة تسجيل المطاعم والمقاهي)
 * الحقول المطلوبة: نوع المأكولات، عدد الموظفين، ساعات العمل، خدمات التوصيل
 */
@Composable
fun RestaurantForm(
    themeColors: VisualThemePalette,
    onSubmit: (Map<String, Any>) -> Unit,
    modifier: Modifier = Modifier
) {
    var step by remember { mutableIntStateOf(1) }

    // Step 1: Restaurant Identity & Auth
    var restaurantName by remember { mutableStateOf("") }
    var managerName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    // Step 2: Food specialties & Capacity
    var cuisineType by remember { mutableStateOf("مأكولات يمنية وشعبية / مشويات") }
    var staffCount by remember { mutableStateOf("10") }
    var seatingCapacity by remember { mutableStateOf("عائلات وأفراد") }
    var city by remember { mutableStateOf("صنعاء") }

    // Step 3: Working Hours & Delivery services
    var workingHours by remember { mutableStateOf("11:00 ص - 1:00 ص") }
    var hasDeliveryService by remember { mutableStateOf(true) }
    var deliveryCoverage by remember { mutableStateOf("توصيل لكافة أحياء المدينة") }

    val isStep1Valid = restaurantName.isNotBlank() && phone.length >= 9 && password.length >= 6 && password == confirmPassword
    val isStep2Valid = cuisineType.isNotBlank() && staffCount.isNotBlank()
    val isStep3Valid = workingHours.isNotBlank()

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
                    text = "تسجيل مطعم أو كافيه",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFF59E0B).copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "المرحلة $step من 3",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF59E0B),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            LinearProgressIndicator(
                progress = { step / 3f },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = Color(0xFFF59E0B),
                trackColor = Color(0xFF334155)
            )

            AnimatedContent(targetState = step, label = "rest_wizard") { targetStep ->
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    when (targetStep) {
                        1 -> {
                            OutlinedTextField(
                                value = restaurantName,
                                onValueChange = { restaurantName = it },
                                label = { Text("اسم المطعم أو المقهى") },
                                leadingIcon = { Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFF59E0B)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = managerName,
                                onValueChange = { managerName = it },
                                label = { Text("اسم المدير المسؤول") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFFF59E0B)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = phone,
                                onValueChange = { phone = it },
                                label = { Text("رقم هاتف الحجوزات والطلبات") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFFF59E0B)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("كلمة المرور") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFF59E0B)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it },
                                label = { Text("تأكيد كلمة المرور") },
                                isError = confirmPassword.isNotEmpty() && confirmPassword != password,
                                leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFFF59E0B)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        2 -> {
                            OutlinedTextField(
                                value = cuisineType,
                                onValueChange = { cuisineType = it },
                                label = { Text("نوع وقائمة المأكولات (الأطباق الرئيسية)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = staffCount,
                                onValueChange = { staffCount = it },
                                label = { Text("عدد الموظفين وطاقم العمل") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFFF59E0B)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = seatingCapacity,
                                onValueChange = { seatingCapacity = it },
                                label = { Text("أقسام الجلوس (عوائل / أفراد / قاعات خاصة)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = city,
                                onValueChange = { city = it },
                                label = { Text("المدينة والفرع") },
                                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFFF59E0B)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        3 -> {
                            OutlinedTextField(
                                value = workingHours,
                                onValueChange = { workingHours = it },
                                label = { Text("ساعات العمل (فترات الغداء والعشاء)") },
                                leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null, tint = Color(0xFFF59E0B)) },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("توفر خدمة التوصيل السريع للمنازل", color = Color.White, fontSize = 13.sp)
                                Switch(
                                    checked = hasDeliveryService,
                                    onCheckedChange = { hasDeliveryService = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFF59E0B))
                                )
                            }

                            if (hasDeliveryService) {
                                OutlinedTextField(
                                    value = deliveryCoverage,
                                    onValueChange = { deliveryCoverage = it },
                                    label = { Text("نطاق وخدمات التوصيل المتاحة") },
                                    leadingIcon = { Icon(Icons.Default.Send, contentDescription = null, tint = Color(0xFFF59E0B)) },
                                    modifier = Modifier.fillMaxWidth()
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
                                "restaurantName" to restaurantName,
                                "managerName" to managerName,
                                "phone" to phone,
                                "password" to password,
                                "cuisineType" to cuisineType,
                                "staffCount" to staffCount,
                                "seatingCapacity" to seatingCapacity,
                                "city" to city,
                                "workingHours" to workingHours,
                                "hasDeliveryService" to hasDeliveryService,
                                "deliveryCoverage" to deliveryCoverage,
                                "role" to "RESTAURANT"
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
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B), contentColor = Color(0xFF0F172A))
                ) {
                    Text(text = if (step == 3) "إتمام تسجيل المطعم" else "التالي", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
