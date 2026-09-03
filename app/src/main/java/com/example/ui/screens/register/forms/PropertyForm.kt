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
 * 🏢 PropertyForm (استمارة تسجيل المكاتب العقارية وملاك العقارات)
 * الحقول المطلوبة: عدد الغرف، عدد الحمامات، الدور، تاريخ البناء، وسائل الراحة
 */
@Composable
fun PropertyForm(
    themeColors: VisualThemePalette,
    onSubmit: (Map<String, Any>) -> Unit,
    modifier: Modifier = Modifier
) {
    var step by remember { mutableIntStateOf(1) }

    // Step 1: Office/Owner & Account
    var officeName by remember { mutableStateOf("") }
    var ownerName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    // Step 2: Property Core Specs
    var propertyType by remember { mutableStateOf("شقق سكنية / فلل / محلات تجارية") }
    var roomsCount by remember { mutableStateOf("4") }
    var bathroomsCount by remember { mutableStateOf("2") }
    var floorNumber by remember { mutableStateOf("الدور الثاني") }
    var constructionYear by remember { mutableStateOf("2022") }

    // Step 3: Location, Amenities & Rent/Price
    var city by remember { mutableStateOf("صنعاء - حدة") }
    var amenities by remember { mutableStateOf("مصعد، موقف سيارات، حراسة أمنية، خزان مياه مستقل") }
    var priceRange by remember { mutableStateOf("إيجار شهري / بيع مباشر") }

    val isStep1Valid = officeName.isNotBlank() && ownerName.isNotBlank() && phone.trim().length >= 9 && password.length >= 6 && password == confirmPassword && city.isNotBlank()
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
                    text = "تسجيل مكتب عقاري / مالك عقار",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF8B5CF6).copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "المرحلة $step من 3",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF8B5CF6),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            LinearProgressIndicator(
                progress = { step / 3f },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = Color(0xFF8B5CF6),
                trackColor = Color(0xFF334155)
            )

            AnimatedContent(targetState = step, label = "prop_wizard") { targetStep ->
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    when (targetStep) {
                        1 -> {
                            OutlinedTextField(
                                value = officeName,
                                onValueChange = { officeName = it },
                                label = { Text("اسم المكتب العقاري أو صفة المالك") },
                                leadingIcon = { Icon(Icons.Default.Home, contentDescription = null, tint = Color(0xFF8B5CF6)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = ownerName,
                                onValueChange = { ownerName = it },
                                label = { Text("اسم المسؤول المباشر") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF8B5CF6)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = phone,
                                onValueChange = { phone = it },
                                label = { Text("رقم الهاتف للتواصل المباشر") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF8B5CF6)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("كلمة المرور") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF8B5CF6)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it },
                                label = { Text("تأكيد كلمة المرور") },
                                isError = confirmPassword.isNotEmpty() && confirmPassword != password,
                                leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF8B5CF6)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        2 -> {
                            OutlinedTextField(
                                value = propertyType,
                                onValueChange = { propertyType = it },
                                label = { Text("نوع العقارات المتوفرة (شقق / فلل / أراضي)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = roomsCount,
                                    onValueChange = { roomsCount = it },
                                    label = { Text("عدد الغرف") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = bathroomsCount,
                                    onValueChange = { bathroomsCount = it },
                                    label = { Text("عدد الحمامات") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = floorNumber,
                                    onValueChange = { floorNumber = it },
                                    label = { Text("الدور / الطابق") },
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = constructionYear,
                                    onValueChange = { constructionYear = it },
                                    label = { Text("تاريخ / سنة البناء") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        3 -> {
                            OutlinedTextField(
                                value = city,
                                onValueChange = { city = it },
                                label = { Text("المدينة والحي السكني") },
                                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF8B5CF6)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = amenities,
                                onValueChange = { amenities = it },
                                label = { Text("وسائل الراحة والخدمات المتاحة") },
                                leadingIcon = { Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFF8B5CF6)) },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2
                            )
                            OutlinedTextField(
                                value = priceRange,
                                onValueChange = { priceRange = it },
                                label = { Text("متوسط الأسعار أو نوع العرض (بيع / إيجار)") },
                                leadingIcon = { Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = Color(0xFF8B5CF6)) },
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
                                "officeName" to officeName,
                                "ownerName" to ownerName,
                                "phone" to phone,
                                "password" to password,
                                "propertyType" to propertyType,
                                "roomsCount" to roomsCount,
                                "bathroomsCount" to bathroomsCount,
                                "floorNumber" to floorNumber,
                                "constructionYear" to constructionYear,
                                "city" to city,
                                "amenities" to amenities,
                                "priceRange" to priceRange,
                                "role" to "PROPERTY"
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
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6), contentColor = Color.White)
                ) {
                    Text(text = if (step == 3) "إتمام تسجيل العقارات" else "التالي", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
