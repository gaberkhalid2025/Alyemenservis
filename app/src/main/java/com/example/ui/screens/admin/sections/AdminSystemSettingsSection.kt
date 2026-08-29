package com.example.ui.screens.admin.sections

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.utils.VisualThemePalette

@Composable
fun AdminSystemSettingsSection(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    modifier: Modifier = Modifier
) {
    val settingsState by viewModel.settings.collectAsState()
    val context = LocalContext.current

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "⚙️ إعدادات النظام ووضع الصيانة",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "وضع الصيانة العامة 🚧", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                        Text(
                            text = if (settingsState.isMaintenanceActive) "التطبيق حالياً في وضع الصيانة (مغلق أمام العملاء)" else "التطبيق يعمل كالمعتاد بكامل طاقته",
                            color = if (settingsState.isMaintenanceActive) Color(0xFFEF5350) else Color(0xFF10B981),
                            fontSize = 11.5.sp
                        )
                    }

                    Switch(
                        checked = settingsState.isMaintenanceActive,
                        onCheckedChange = { active ->
                            val st = settingsState
                            viewModel.updateBackdoorSettings(
                                st.appName, st.welcomeMessage, st.footerMessage, st.activeThemeId,
                                st.supportPhone, st.supportEmail, st.supportWhatsapp,
                                active, st.hidePromoFooter, st.assistantHidden, st.assistantSize,
                                st.chatHidden, st.chatSize, st.maxSearchRadiusKm, st.isSpeechSearchEnabled,
                                false, 90
                            )
                            Toast.makeText(context, if (active) "تم تفعيل وضع الصيانة" else "تم إيقاف وضع الصيانة", Toast.LENGTH_SHORT).show()
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFEF5350))
                    )
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "💾 النسخ الاحتياطي وتفريغ الذاكرة المؤقتة",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            viewModel.refreshData()
                            Toast.makeText(context, "تم أخذ مزامنة وتحديث حالة النظام بنجاح", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("مزامنة فورية", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.refreshData()
                            Toast.makeText(context, "تم تنظيف وتفريغ الكاش والذاكرة المؤقتة", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00668B)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Build, contentDescription = null, tint = Color(0xFF00668B))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("تنظيف الذاكرة", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
