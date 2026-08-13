package com.example.ui.screens.admin

import androidx.compose.foundation.BorderStroke
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

@Composable
fun AdminCleanPanel(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    state: AdminPanelState
) {
    if (!PermissionGuard.hasPermission(RoleManager.fromRoleString(viewModel.adminRole.value), "CLEAN_DATABASE")) {
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
            Text("🧹 تهيئة وتنظيف بيانات النظام وإعادة الضبط الشامل", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("🚨 تحذير: هذه العملية مسؤولة عن حذف وتنظيف قواعد البيانات على خوادم Firestore", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Red)
                    Text("اختر الأقسام والفئات المراد تنظيفها:", fontSize = 11.sp, color = Color.LightGray)

                    val cleanOptions = listOf(
                        Triple("الفنيين ومزودي الخدمات", wipeProvidersCheckedState) { b: Boolean -> wipeProvidersCheckedState.value = b },
                        Triple("سجل الحجوزات", wipeBookingsCheckedState) { b: Boolean -> wipeBookingsCheckedState.value = b },
                        Triple("المحادثات والرسائل", wipeChatsCheckedState) { b: Boolean -> wipeChatsCheckedState.value = b },
                        Triple("الإشعارات والتنبيهات", wipeNotifsCheckedState) { b: Boolean -> wipeNotifsCheckedState.value = b },
                        Triple("البلاغات والشكاوى", wipeReportsCheckedState) { b: Boolean -> wipeReportsCheckedState.value = b },
                        Triple("التصنيفات والأقسام", wipeCategoriesCheckedState) { b: Boolean -> wipeCategoriesCheckedState.value = b },
                        Triple("الطلبات المعلقة", wipePendingCheckedState) { b: Boolean -> wipePendingCheckedState.value = b },
                        Triple("البنرات والشرائح", wipeBannersCheckedState) { b: Boolean -> wipeBannersCheckedState.value = b },
                        Triple("طاقم المشرفين", wipeSupervisorsCheckedState) { b: Boolean -> wipeSupervisorsCheckedState.value = b },
                        Triple("قائمة المدن", wipeCitiesCheckedState) { b: Boolean -> wipeCitiesCheckedState.value = b },
                        Triple("إعدادات الثيمات", wipeThemesCheckedState) { b: Boolean -> wipeThemesCheckedState.value = b }
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        cleanOptions.forEach { (label, stateVal, onChg) ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = stateVal.value, onCheckedChange = onChg, colors = CheckboxDefaults.colors(checkedColor = Color.Red))
                                Text(label, fontSize = 11.sp, color = Color.White)
                            }
                        }
                    }

                    Button(
                        onClick = {
                            if (wipeProvidersCheckedState.value || wipeBookingsCheckedState.value || wipeChatsCheckedState.value || wipeNotifsCheckedState.value || wipeReportsCheckedState.value || wipeCategoriesCheckedState.value || wipePendingCheckedState.value || wipeBannersCheckedState.value || wipeSupervisorsCheckedState.value || wipeCitiesCheckedState.value || wipeThemesCheckedState.value) {
                                showWipeConfirmDialogState.value = true
                            } else {
                                viewModel.triggerNotification("⚠️ يرجى تحديد فئة واحدة على الأقل للتنظيف")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("بدء عملية التنظيف الشامل 🧹", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            if (showWipeConfirmDialogState.value) {
                AlertDialog(
                    onDismissRequest = { showWipeConfirmDialogState.value = false },
                    title = { Text("🔒 تأكيد كلمة المرور للمسح", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("أدخل كلمة المرور الإدارية لتأكيد عملية الحذف والتنظيف النهائي:", fontSize = 11.sp, color = Color.LightGray)
                            OutlinedTextField(
                                value = wipeInputPasswordState.value,
                                onValueChange = { wipeInputPasswordState.value = it },
                                label = { Text("كلمة المرور") },
                                modifier = Modifier.fillMaxWidth(),
                                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White)
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (wipeInputPasswordState.value.isNotBlank()) {
                                    viewModel.wipeAllDatabaseData(wipeInputPasswordState.value)
                                    viewModel.triggerNotification("✅ تم تنفيذ عملية التنظيف بنجاح للبنود المحددة")
                                    showWipeConfirmDialogState.value = false
                                    wipeInputPasswordState.value = ""
                                } else {
                                    viewModel.triggerNotification("❌ يرجى إدخال كلمة المرور")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                        ) {
                            Text("تأكيد وحذف نهائي", color = Color.White)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showWipeConfirmDialogState.value = false }) {
                            Text("إلغاء", color = Color.Gray)
                        }
                    },
                    containerColor = themeColors.surface
                )
            }
        }
    }
}
