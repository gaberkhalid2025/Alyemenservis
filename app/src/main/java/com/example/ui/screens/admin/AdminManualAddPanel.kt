package com.example.ui.screens.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.utils.VisualThemePalette
import com.example.util.PermissionGuard
import com.example.util.RoleManager

@Composable
fun AdminManualAddPanel(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    state: AdminPanelState
) {
    if (!PermissionGuard.hasPermission(RoleManager.fromRoleString(viewModel.adminRole.value), "MANAGE_PROVIDERS")) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Red, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("🔒 ليس لديك صلاحية للوصول إلى هذه اللوحة", color = Color.White, fontSize = 14.sp)
                Text("يرجى التواصل مع المالك أو المدير الرئيسي", color = Color.Gray, fontSize = 12.sp)
            }
        }
        return
    }

    with(state) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("➕ الإضافة اليدوية السريعة من الإدارة", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)

            var addCategoryType by remember { mutableStateOf("PROVIDER") }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    "PROVIDER" to "👨‍🔧 فني",
                    "STORE" to "🏪 محل",
                    "RESTAURANT" to "🍔 مطعم",
                    "MEDICAL" to "🏥 عيادة",
                    "PROPERTY" to "🏠 عقار",
                    "JOB" to "💼 وظيفة"
                ).forEach { (key, label) ->
                    val isSel = addCategoryType == key
                    Button(
                        onClick = { addCategoryType = key },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isSel) themeColors.accent else themeColors.surface),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)
                    ) {
                        Text(label, fontSize = 10.sp, color = if (isSel) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = themeColors.surface)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    when (addCategoryType) {
                        "PROVIDER" -> {
                            Text("➕ إضافة فني / مقدم خدمة جديد يدوياً:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            OutlinedTextField(value = manualNameState.value, onValueChange = { manualNameState.value = it }, label = { Text("اسم الفني الكامل") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = manualPhoneState.value, onValueChange = { manualPhoneState.value = it }, label = { Text("رقم الهاتف") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = manualCategoryIdState.value, onValueChange = { manualCategoryIdState.value = it }, label = { Text("التخصص / القسم") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = manualCityIdState.value, onValueChange = { manualCityIdState.value = it }, label = { Text("المنطقة / المدينة") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = manualPriceValueState.value, onValueChange = { manualPriceValueState.value = it }, label = { Text("سعر المعاينة (ر.ي)") }, modifier = Modifier.fillMaxWidth())

                            Button(
                                onClick = {
                                    if (manualNameState.value.isNotBlank() && manualPhoneState.value.isNotBlank()) {
                                        val p = manualPriceValueState.value.toDoubleOrNull() ?: 0.0
                                        viewModel.addNewProvider(manualNameState.value, manualPhoneState.value, manualCategoryIdState.value, manualCityIdState.value, p, true)
                                        manualNameState.value = ""
                                        manualPhoneState.value = ""
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary)
                            ) {
                                Text("إضافة واعتماد الفني فوراً 👨‍🔧", color = Color.White)
                            }
                        }
                        "STORE" -> {
                            Text("➕ إضافة متجر / محل تجاري جديد:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            OutlinedTextField(value = manualNameState.value, onValueChange = { manualNameState.value = it }, label = { Text("اسم المحل التجاري") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = manualPhoneState.value, onValueChange = { manualPhoneState.value = it }, label = { Text("رقم الهاتف / الواتساب") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = manualStreetState.value, onValueChange = { manualStreetState.value = it }, label = { Text("الوصف والتفاصيل") }, modifier = Modifier.fillMaxWidth())

                            Button(
                                onClick = {
                                    if (manualNameState.value.isNotBlank()) {
                                        viewModel.addStore(manualNameState.value, manualStreetState.value, manualPhoneState.value)
                                        manualNameState.value = ""
                                        manualPhoneState.value = ""
                                        manualStreetState.value = ""
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary)
                            ) {
                                Text("إضافة المحل التجاري 🏪", color = Color.White)
                            }
                        }
                        "RESTAURANT" -> {
                            Text("➕ إضافة مطعم / كافيه جديد:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            OutlinedTextField(value = manualNameState.value, onValueChange = { manualNameState.value = it }, label = { Text("اسم المطعم / الكافيه") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = manualPhoneState.value, onValueChange = { manualPhoneState.value = it }, label = { Text("رقم الطلبات والخدمة") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = manualStreetState.value, onValueChange = { manualStreetState.value = it }, label = { Text("نوع الوجبات وساعات العمل") }, modifier = Modifier.fillMaxWidth())

                            Button(
                                onClick = {
                                    if (manualNameState.value.isNotBlank()) {
                                        viewModel.addStore(manualNameState.value, "مطعم: ${manualStreetState.value}", manualPhoneState.value)
                                        manualNameState.value = ""
                                        manualPhoneState.value = ""
                                        manualStreetState.value = ""
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary)
                            ) {
                                Text("إضافة المطعم 🍔", color = Color.White)
                            }
                        }
                        "MEDICAL" -> {
                            Text("➕ إضافة مركز طبي / عيادة جديدة:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            OutlinedTextField(value = manualNameState.value, onValueChange = { manualNameState.value = it }, label = { Text("اسم المركز الطبي / العيادة") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = manualPhoneState.value, onValueChange = { manualPhoneState.value = it }, label = { Text("رقم الهاتف والطوارئ") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = manualStreetState.value, onValueChange = { manualStreetState.value = it }, label = { Text("التخصصات الطبية والدوام") }, modifier = Modifier.fillMaxWidth())

                            Button(
                                onClick = {
                                    if (manualNameState.value.isNotBlank()) {
                                        viewModel.addStore(manualNameState.value, "طبي: ${manualStreetState.value}", manualPhoneState.value)
                                        manualNameState.value = ""
                                        manualPhoneState.value = ""
                                        manualStreetState.value = ""
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary)
                            ) {
                                Text("إضافة المركز الطبي 🏥", color = Color.White)
                            }
                        }
                        "PROPERTY" -> {
                            Text("➕ إضافة إعلان عقار جديد:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            OutlinedTextField(value = manualNameState.value, onValueChange = { manualNameState.value = it }, label = { Text("عنوان العقار والنوع") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = manualPhoneState.value, onValueChange = { manualPhoneState.value = it }, label = { Text("رقم للتواصل") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = manualStreetState.value, onValueChange = { manualStreetState.value = it }, label = { Text("التفاصيل والسعر") }, modifier = Modifier.fillMaxWidth())

                            Button(
                                onClick = {
                                    if (manualNameState.value.isNotBlank()) {
                                        viewModel.triggerNotification("✅ تم إضافة العقار بنجاح")
                                        manualNameState.value = ""
                                        manualPhoneState.value = ""
                                        manualStreetState.value = ""
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary)
                            ) {
                                Text("إضافة العقار 🏠", color = Color.White)
                            }
                        }
                        "JOB" -> {
                            Text("➕ إضافة فرصة عمل جديدة:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            OutlinedTextField(value = manualNameState.value, onValueChange = { manualNameState.value = it }, label = { Text("المسمى الوظيفي والشركة") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = manualPhoneState.value, onValueChange = { manualPhoneState.value = it }, label = { Text("رقم التواصل") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = manualStreetState.value, onValueChange = { manualStreetState.value = it }, label = { Text("تفاصيل الوظيفة والراتب") }, modifier = Modifier.fillMaxWidth())

                            Button(
                                onClick = {
                                    if (manualNameState.value.isNotBlank()) {
                                        viewModel.triggerNotification("✅ تم إضافة الوظيفة بنجاح")
                                        manualNameState.value = ""
                                        manualPhoneState.value = ""
                                        manualStreetState.value = ""
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary)
                            ) {
                                Text("إضافة الوظيفة 💼", color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}
