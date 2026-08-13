package com.example.ui.screens.admin

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.utils.VisualThemePalette
import com.example.util.PermissionGuard
import com.example.util.RoleManager

@Composable
fun AdminPropertiesPanel(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    state: AdminPanelState
) {
    if (!PermissionGuard.hasPermission(RoleManager.fromRoleString(viewModel.adminRole.value), "MANAGE_PROPERTIES")) {
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("🏠 إدارة العقارات والأراضي والإيجار والبيع", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
            
            var propTitle by remember { mutableStateOf("") }
            var propPrice by remember { mutableStateOf("") }
            var propPhone by remember { mutableStateOf("") }
            var propType by remember { mutableStateOf("sale") }

            OutlinedTextField(
                value = propertiesSearchQueryState.value,
                onValueChange = { propertiesSearchQueryState.value = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("ابحث في العقارات...", color = Color.Gray, fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) }
            )

            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = themeColors.surface)) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("➕ إضافة عقار جديد يدوياً:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    OutlinedTextField(value = propTitle, onValueChange = { propTitle = it }, label = { Text("عنوان العقار") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = propPrice, onValueChange = { propPrice = it }, label = { Text("السعر") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    OutlinedTextField(value = propPhone, onValueChange = { propPhone = it }, label = { Text("رقم المالك") }, modifier = Modifier.fillMaxWidth())
                    Button(
                        onClick = {
                            if (propTitle.isNotBlank()) {
                                val p = propPrice.toDoubleOrNull() ?: 0.0
                                viewModel.addProperty(propTitle, "عقار ممتاز", p, propType, "apartment", propPhone)
                                propTitle = ""
                                propPrice = ""
                                propPhone = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary)
                    ) {
                        Text("إضافة العقار 🏠", color = Color.White)
                    }
                }
            }

            Text("📋 قائمة العقارات المتاحة:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            val propertiesList by viewModel.properties.collectAsState()
            val filteredProps = propertiesList.filter { it.title.contains(propertiesSearchQueryState.value, ignoreCase = true) }
            if (filteredProps.isEmpty()) {
                Text("لا توجد عقارات مسجلة حالياً.", color = Color.LightGray, fontSize = 11.sp)
            } else {
                filteredProps.forEach { prop ->
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = themeColors.surface)) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(prop.title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                            Text("السعر: ${prop.price} | النوع: ${prop.type}", color = Color.LightGray, fontSize = 10.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { viewModel.togglePropertyBlock(prop.id) }, colors = ButtonDefaults.buttonColors(containerColor = if (prop.isBlocked) Color.Green else Color.Red)) {
                                    Text(if (prop.isBlocked) "إلغاء الحظر" else "حظر", fontSize = 9.sp, color = Color.White)
                                }
                                Button(onClick = { viewModel.deleteProperty(prop.id) }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f))) {
                                    Text("حذف 🗑️", fontSize = 9.sp, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
