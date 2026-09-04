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
 * 🍽️ RestaurantForm (استمارة تسجيل المطاعم والمقاهي المحدثة والمبسطة)
 * نموذج صفحة واحدة مدمج مع التحقق الفوري للمتطلبات الإجبارية.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestaurantForm(
    themeColors: VisualThemePalette,
    onSubmit: (Map<String, Any>) -> Unit,
    modifier: Modifier = Modifier
) {
    // Mandatory fields
    var restaurantName by remember { mutableStateOf("") }
    var managerName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("صنعاء") }

    // Optional fields
    var cuisineType by remember { mutableStateOf("مأكولات شعبية وحديثة") }
    var staffCount by remember { mutableStateOf("") }
    var seatingCapacity by remember { mutableStateOf("") }
    var workingHours by remember { mutableStateOf("") }
    var hasDeliveryService by remember { mutableStateOf(true) }
    var deliveryCoverage by remember { mutableStateOf("") }

    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    // Validation (Restaurant Name, Triple Manager Name, Phone >= 9, matching password >= 6, and city)
    val managerNamePartsCount = managerName.trim().split(" ").filter { it.isNotBlank() }.size
    val isFormValid = restaurantName.isNotBlank() &&
            managerNamePartsCount >= 3 &&
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

            // 1. Restaurant Name
            OutlinedTextField(
                value = restaurantName,
                onValueChange = { restaurantName = it },
                label = { Text("اسم المطعم / المقهى / المنشأة *") },
                placeholder = { Text("مثال: مطعم شواطئ عدن السياحي") },
                leadingIcon = { Icon(Icons.Default.Favorite, contentDescription = null, tint = themeColors.accent, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
            )

            // 2. Manager Name (Triple)
            OutlinedTextField(
                value = managerName,
                onValueChange = { managerName = it },
                label = { Text("اسم المدير المسؤول * (ثلاثي)") },
                placeholder = { Text("مثال: علي عبدالله الصنعاني") },
                isError = managerName.isNotEmpty() && managerNamePartsCount < 3,
                supportingText = {
                    if (managerName.isNotEmpty() && managerNamePartsCount < 3) {
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
                label = { Text("رقم الهاتف للحجوزات والطلبات * (9 أرقام)") },
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
                text = "بيانات إضافية اختيارية:",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.LightGray
            )

            // 7. Cuisine Type (Optional)
            OutlinedTextField(
                value = cuisineType,
                onValueChange = { cuisineType = it },
                label = { Text("نوع المأكولات / التصنيف (اختياري)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
            )

            // 8. Staff Count (Optional)
            OutlinedTextField(
                value = staffCount,
                onValueChange = { staffCount = it },
                label = { Text("عدد الموظفين وطاقم العمل (اختياري)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
            )

            // 9. Seating Capacity (Optional)
            OutlinedTextField(
                value = seatingCapacity,
                onValueChange = { seatingCapacity = it },
                label = { Text("أقسام الجلوس - عائلات/أفراد (اختياري)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
            )

            // 10. Working Hours (Optional)
            OutlinedTextField(
                value = workingHours,
                onValueChange = { workingHours = it },
                label = { Text("ساعات العمل والخدمة (اختياري)") },
                placeholder = { Text("مثال: 12:00 م - 12:00 ص") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("توفر خدمة التوصيل السريع (اختياري)", color = Color.White, fontSize = 12.sp)
                Switch(
                    checked = hasDeliveryService,
                    onCheckedChange = { hasDeliveryService = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = themeColors.accent)
                )
            }

            if (hasDeliveryService) {
                OutlinedTextField(
                    value = deliveryCoverage,
                    onValueChange = { deliveryCoverage = it },
                    label = { Text("نطاق التوصيل والمناطق المشمولة (اختياري)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                )
            }

            Button(
                onClick = {
                    val data = mapOf(
                        "restaurantName" to restaurantName,
                        "managerName" to managerName,
                        "phone" to phone,
                        "password" to password,
                        "cuisineType" to cuisineType.ifBlank { "مأكولات متنوعة" },
                        "staffCount" to staffCount.ifBlank { "5" },
                        "seatingCapacity" to seatingCapacity,
                        "city" to city,
                        "workingHours" to workingHours.ifBlank { "11:00 ص - 11:30 م" },
                        "hasDeliveryService" to hasDeliveryService,
                        "deliveryCoverage" to deliveryCoverage,
                        "role" to "RESTAURANT"
                    )
                    onSubmit(data)
                },
                enabled = isFormValid,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().height(42.dp),
                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent, contentColor = Color.Black)
            ) {
                Text("إرسال طلب تسجيل المطعم 🚀", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}
