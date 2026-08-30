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
 * 🛍️ StoreForm (استمارة تسجيل المتجر والمورد)
 * تشمل الحقول المطلوبة: السجل التجاري، رقم الضريبة، وسائل التواصل، ساعات العمل، سياسة الاسترجاع
 */
@Composable
fun StoreForm(
    themeColors: VisualThemePalette,
    onSubmit: (Map<String, Any>) -> Unit,
    modifier: Modifier = Modifier
) {
    var step by remember { mutableIntStateOf(1) }

    // Step 1: Store & Owner identity
    var storeName by remember { mutableStateOf("") }
    var ownerName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    // Step 2: Commercial & Legal Info
    var commercialRegister by remember { mutableStateOf("") }
    var taxNumber by remember { mutableStateOf("") }
    var storeCategory by remember { mutableStateOf("إلكترونيات / أجهزة") }
    var city by remember { mutableStateOf("صنعاء") }

    // Step 3: Social, Working Hours & Policies
    var socialWhatsapp by remember { mutableStateOf("") }
    var socialFacebook by remember { mutableStateOf("") }
    var socialInstagram by remember { mutableStateOf("") }
    var workingHours by remember { mutableStateOf("9:00 ص - 10:00 م") }
    var returnPolicy by remember { mutableStateOf("استرجاع واستبدال خلال 3 أيام مع الفاتورة") }

    val isStep1Valid = storeName.isNotBlank() && phone.length >= 9 && password.length >= 6 && password == confirmPassword
    val isStep2Valid = commercialRegister.isNotBlank() || taxNumber.isNotBlank()
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
                    text = "تسجيل متجر أو نشاط تجاري",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "المرحلة $step من 3",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            LinearProgressIndicator(
                progress = { step / 3f },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = Color(0xFF10B981),
                trackColor = Color(0xFF334155)
            )

            AnimatedContent(targetState = step, label = "store_wizard") { targetStep ->
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    when (targetStep) {
                        1 -> {
                            OutlinedTextField(
                                value = storeName,
                                onValueChange = { storeName = it },
                                label = { Text("اسم المتجر / النشاط التجاري") },
                                leadingIcon = { Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = Color(0xFF10B981)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = ownerName,
                                onValueChange = { ownerName = it },
                                label = { Text("اسم المالك أو المدير المسؤول") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF10B981)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = phone,
                                onValueChange = { phone = it },
                                label = { Text("رقم هاتف المتجر المعتمد") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF10B981)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("كلمة المرور") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF10B981)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it },
                                label = { Text("تأكيد كلمة المرور") },
                                isError = confirmPassword.isNotEmpty() && confirmPassword != password,
                                leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        2 -> {
                            OutlinedTextField(
                                value = commercialRegister,
                                onValueChange = { commercialRegister = it },
                                label = { Text("رقم السجل التجاري") },
                                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF10B981)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = taxNumber,
                                onValueChange = { taxNumber = it },
                                label = { Text("الرقم الضريبي (إن وجد)") },
                                leadingIcon = { Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFF10B981)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = storeCategory,
                                onValueChange = { storeCategory = it },
                                label = { Text("تصنيف المتجر والبضائع") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = city,
                                onValueChange = { city = it },
                                label = { Text("المدينة والمقر الرئيسي") },
                                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF10B981)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        3 -> {
                            OutlinedTextField(
                                value = socialWhatsapp,
                                onValueChange = { socialWhatsapp = it },
                                label = { Text("واتساب خدمة العملاء والطلبات") },
                                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF10B981)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = socialFacebook,
                                onValueChange = { socialFacebook = it },
                                label = { Text("رابط صفحة فيسبوك أو إنستغرام") },
                                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = Color(0xFF10B981)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = workingHours,
                                onValueChange = { workingHours = it },
                                label = { Text("ساعات العمل وأيام الفتح") },
                                leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null, tint = Color(0xFF10B981)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = returnPolicy,
                                onValueChange = { returnPolicy = it },
                                label = { Text("سياسة الاسترجاع والضمان") },
                                leadingIcon = { Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF10B981)) },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2
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
                                "storeName" to storeName,
                                "ownerName" to ownerName,
                                "phone" to phone,
                                "password" to password,
                                "commercialRegister" to commercialRegister,
                                "taxNumber" to taxNumber,
                                "storeCategory" to storeCategory,
                                "city" to city,
                                "socialWhatsapp" to socialWhatsapp,
                                "socialFacebook" to socialFacebook,
                                "workingHours" to workingHours,
                                "returnPolicy" to returnPolicy,
                                "role" to "STORE"
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
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981), contentColor = Color.White)
                ) {
                    Text(text = if (step == 3) "إتمام تسجيل المتجر" else "التالي", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
