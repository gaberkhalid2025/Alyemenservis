package com.example.ui.screens.admin

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.data.AdminSettingsEntity

@Composable
fun AdminRegFormsManagerPanel(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    state: AdminPanelState
) {
    if (!PermissionGuard.hasPermission(RoleManager.fromRoleString(viewModel.adminRole.value), "MANAGE_REG_FORMS")) {
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
            Text("📋 تخصيص استمارات التسجيل وطلبات الانضمام", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)

            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = themeColors.surface)) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("إضافة متطلب جديد لاستمارة التسجيل:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    OutlinedTextField(
                        value = requirementItemInputState.value,
                        onValueChange = { requirementItemInputState.value = it },
                        label = { Text("اسم الحقل أو المتطلب") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("حقل إلزامي", fontSize = 11.sp, color = Color.White)
                        Switch(
                            checked = isNewRequirementMandatoryState.value,
                            onCheckedChange = { isNewRequirementMandatoryState.value = it }
                        )
                    }

                    Button(
                        onClick = {
                            if (requirementItemInputState.value.isNotBlank()) {
                                requirementsListStateState.value = requirementsListStateState.value + requirementItemInputState.value
                                requirementItemInputState.value = ""
                                val currentSettings = viewModel.settings.value
                                viewModel.saveCustomSettingsState(currentSettings)
                                viewModel.triggerNotification("✅ تم إضافة المتطلب بنجاح")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("إضافة الحقل للاستمارة ➕", color = Color.White)
                    }
                }
            }
        }
    }
}
