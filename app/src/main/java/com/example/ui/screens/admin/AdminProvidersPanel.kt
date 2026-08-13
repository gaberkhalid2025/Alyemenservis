package com.example.ui.screens.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.MainViewModel
import com.example.utils.VisualThemePalette
import com.example.util.PermissionGuard
import com.example.util.RoleManager
import com.example.util.UserRole

@Composable
fun AdminProvidersPanel(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    state: AdminPanelState
) {
    val currentRole = RoleManager.fromRoleString(viewModel.adminRole.value)
    if (!PermissionGuard.hasPermission(currentRole, "MANAGE_PROVIDERS")) {
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

    val providers by viewModel.providers.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val editProviderObj = state.showEditProviderMetadataObjState.value

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("👥 إدارة أعضاء الدليل والتميز والتصنيفات", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
        Text("تعديل وحذف بيانات الفنيين ومقدمي الخدمات المسجلين وتصنيف أقسامهم بدقة.", color = Color.LightGray, fontSize = 11.sp)

        if (providers.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                Text("لا يوجد أعضاء في الدليل حالياً.", color = Color.LightGray, fontSize = 11.sp)
            }
        } else {
            providers.forEach { p ->
                val matchedCat = categories.find { it.id == p.categoryId }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = themeColors.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(p.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                            Text(
                                text = "الهاتف: ${p.phone} | القسم: ${matchedCat?.icon ?: "🔧"} ${matchedCat?.name ?: p.categoryId}",
                                color = Color.LightGray,
                                fontSize = 10.sp
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            IconButton(
                                onClick = {
                                    state.showEditProviderMetadataObjState.value = p
                                    state.editProviderPhoneState.value = p.phone
                                    state.editProviderCategoryIdState.value = p.categoryId
                                }
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = themeColors.accent, modifier = Modifier.size(18.dp))
                            }
                            Button(
                                onClick = { viewModel.removeProvider(p.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text("حذف 🗑️", fontSize = 9.sp, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }

    // Edit Provider Metadata Dialog
    if (editProviderObj != null) {
        val editPhone = state.editProviderPhoneState.value
        val editCategory = state.editProviderCategoryIdState.value

        AlertDialog(
            onDismissRequest = { state.showEditProviderMetadataObjState.value = null },
            title = { Text("📝 تعديل بيانات العضو", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("الاسم: ${editProviderObj.name}", fontSize = 12.sp, color = Color.LightGray)
                    
                    OutlinedTextField(
                        value = editPhone,
                        onValueChange = { state.editProviderPhoneState.value = it },
                        label = { Text("رقم الهاتف") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("اختر القسم الجديد:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .border(1.dp, Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .padding(6.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            categories.forEach { cat ->
                                val isSelected = editCategory == cat.id
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (isSelected) themeColors.accent.copy(alpha = 0.15f) else Color.Transparent)
                                        .clickable { state.editProviderCategoryIdState.value = cat.id }
                                        .padding(6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { state.editProviderCategoryIdState.value = cat.id },
                                        colors = RadioButtonDefaults.colors(selectedColor = themeColors.accent),
                                        modifier = Modifier.scale(0.8f)
                                    )
                                    Text("${cat.icon} ${cat.name}", fontSize = 11.sp, color = if (isSelected) themeColors.accent else Color.White)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editPhone.isNotBlank() && editCategory.isNotBlank()) {
                            viewModel.editProviderPhoneAndCategory(editProviderObj.id, editPhone.trim(), editCategory)
                            state.showEditProviderMetadataObjState.value = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent)
                ) {
                    Text("حفظ التعديلات ✔️", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { state.showEditProviderMetadataObjState.value = null }) {
                    Text("إلغاء ❌", color = Color.White, fontSize = 11.sp)
                }
            },
            containerColor = themeColors.surface
        )
    }
}
