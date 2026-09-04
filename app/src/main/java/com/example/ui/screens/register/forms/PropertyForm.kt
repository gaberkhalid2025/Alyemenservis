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
 * 🏢 PropertyForm (استمارة تسجيل العقارات المحدثة والمبسطة)
 * نموذج صفحة واحدة مدمج مع التحقق الفوري للمتطلبات الإجبارية.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertyForm(
    themeColors: VisualThemePalette,
    onSubmit: (Map<String, Any>) -> Unit,
    modifier: Modifier = Modifier
) {
    // Mandatory fields
    var officeName by remember { mutableStateOf("") }
    var ownerName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("صنعاء") }

    // Optional fields
    var propertyType by remember { mutableStateOf("شقق سكنية / فلل / أراضي") }
    var roomsCount by remember { mutableStateOf("") }
    var bathroomsCount by remember { mutableStateOf("") }
    var floorNumber by remember { mutableStateOf("") }
    var constructionYear by remember { mutableStateOf("") }
    var amenities by remember { mutableStateOf("") }
    var priceRange by remember { mutableStateOf("") }

    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    // Validation (Office/Owner Name, Triple Responsible Name, Phone >= 9, matching password >= 6, and city)
    val ownerNamePartsCount = ownerName.trim().split(" ").filter { it.isNotBlank() }.size
    val isFormValid = officeName.isNotBlank() &&
            ownerNamePartsCount >= 3 &&
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
                text = "يرجى ملء البيانات (الحقول بعلافة * إجبارية):",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = themeColors.accent
            )

            // 1. Office Name
            OutlinedTextField(
                value = officeName,
                onValueChange = { officeName = it },
                label = { Text("اسم المكتب العقاري أو صفة المالك *") },
                placeholder = { Text("مثال: عقارات اليمن السعيد / مالك عقار") },
                leadingIcon = { Icon(Icons.Default.Home, contentDescription = null, tint = themeColors.accent, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
            )

            // 2. Owner Name (Triple)
            OutlinedTextField(
                value = ownerName,
                onValueChange = { ownerName = it },
                label = { Text("اسم المسؤول المباشر * (ثلاثي)") },
                placeholder = { Text("مثال: صالح محمد اليماني") },
                isError = ownerName.isNotEmpty() && ownerNamePartsCount < 3,
                supportingText = {
                    if (ownerName.isNotEmpty() && ownerNamePartsCount < 3) {
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
                label = { Text("رقم الهاتف للتواصل * (9 أرقام)") },
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
                text = "بيانات عقارية اختيارية:",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.LightGray
            )

            // 7. Property Type (Optional)
            OutlinedTextField(
                value = propertyType,
                onValueChange = { propertyType = it },
                label = { Text("نوع العقارات الموفرة (اختياري)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
            )

            // 8. Rooms & Bathrooms (Optional)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = roomsCount,
                    onValueChange = { roomsCount = it },
                    label = { Text("عدد الغرف") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                )
                OutlinedTextField(
                    value = bathroomsCount,
                    onValueChange = { bathroomsCount = it },
                    label = { Text("الحمامات") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                )
            }

            // 9. Floor & Construction Year (Optional)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = floorNumber,
                    onValueChange = { floorNumber = it },
                    label = { Text("الدور/الطابق") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                )
                OutlinedTextField(
                    value = constructionYear,
                    onValueChange = { constructionYear = it },
                    label = { Text("سنة البناء") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                )
            }

            // 10. Amenities (Optional)
            OutlinedTextField(
                value = amenities,
                onValueChange = { amenities = it },
                label = { Text("وسائل الراحة المتوفرة (اختياري)") },
                placeholder = { Text("مثال: مصعد، حارس، موقف سيارات") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
            )

            Button(
                onClick = {
                    val data = mapOf(
                        "officeName" to officeName,
                        "ownerName" to ownerName,
                        "phone" to phone,
                        "password" to password,
                        "propertyType" to propertyType,
                        "roomsCount" to roomsCount.ifBlank { "0" },
                        "bathroomsCount" to bathroomsCount.ifBlank { "0" },
                        "floorNumber" to floorNumber,
                        "constructionYear" to constructionYear,
                        "city" to city,
                        "amenities" to amenities,
                        "priceRange" to priceRange.ifBlank { "غير محدد" },
                        "role" to "PROPERTY"
                    )
                    onSubmit(data)
                },
                enabled = isFormValid,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().height(42.dp),
                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent, contentColor = Color.Black)
            ) {
                Text("إرسال طلب تسجيل العقارات 🚀", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}
