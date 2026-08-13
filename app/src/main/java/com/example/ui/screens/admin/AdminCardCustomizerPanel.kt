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
fun AdminCardCustomizerPanel(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    state: AdminPanelState
) {
    if (!PermissionGuard.hasPermission(RoleManager.fromRoleString(viewModel.adminRole.value), "MANAGE_CARD_CUSTOMIZER")) {
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
            Text("🎛️ تخصيص أزرار وأشكال البطائق والعرض", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)

            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = themeColors.surface)) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("أبعاد وتصميم بطاقات مقدمي الخدمات:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)

                    Text("ارتفاع الغلاف: ${editCoverHeightState.value.toInt()} dp", fontSize = 11.sp, color = Color.LightGray)
                    Slider(value = editCoverHeightState.value, onValueChange = { editCoverHeightState.value = it }, valueRange = 80f..250f)

                    Text("حجم الصورة الشخصية (الآفاتار): ${editAvatarSizeState.value.toInt()} dp", fontSize = 11.sp, color = Color.LightGray)
                    Slider(value = editAvatarSizeState.value, onValueChange = { editAvatarSizeState.value = it }, valueRange = 40f..120f)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("إظهار شارة VIP", fontSize = 11.sp, color = Color.White)
                        Switch(checked = editShowVipBadgeState.value, onCheckedChange = { editShowVipBadgeState.value = it })
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("إظهار الشارة الموثقة", fontSize = 11.sp, color = Color.White)
                        Switch(checked = editShowVerifiedBadgeState.value, onCheckedChange = { editShowVerifiedBadgeState.value = it })
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("إظهار زر الاتصال", fontSize = 11.sp, color = Color.White)
                        Switch(checked = editShowCallButtonState.value, onCheckedChange = { editShowCallButtonState.value = it })
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("إظهار زر الواتساب", fontSize = 11.sp, color = Color.White)
                        Switch(checked = editShowWhatsappButtonState.value, onCheckedChange = { editShowWhatsappButtonState.value = it })
                    }

                    Button(
                        onClick = {
                            val currentSettings = viewModel.settings.value
                            viewModel.saveCustomSettingsState(currentSettings)
                            viewModel.triggerNotification("✅ تم حفظ تخصيص البطاقات بنجاح")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("حفظ التغييرات 💾", color = Color.White)
                    }
                }
            }
        }
    }
}
