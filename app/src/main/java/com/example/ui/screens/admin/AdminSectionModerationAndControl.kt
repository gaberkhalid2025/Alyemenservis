@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package com.example.ui.screens.admin

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.*
import com.example.utils.VisualThemePalette

internal fun LazyListScope.adminModerationAndControlSection(
    activeSubTab: String,
    state: AdminPanelState,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette
) {
    val context = state.context
    val activatedProviders = state.activatedProviders
    val bookings = state.bookings
    val settingsState = state.settingsState
    val deletedList = state.deletedList

    when (activeSubTab) {
        "BACKDOOR" -> {
            if (state.adminRole == "OWNER") {
                item {
                    OwnerBackdoorPanelLayout(viewModel = viewModel, themeColors = themeColors)
                }
            }
        }

        "STORES" -> {
            item {
                AdminStoresPropertiesPanel(viewModel = viewModel, themeColors = themeColors, initialSector = "STORES")
            }
        }

        "PROPERTIES" -> {
            item {
                AdminStoresPropertiesPanel(viewModel = viewModel, themeColors = themeColors, initialSector = "PROPERTIES")
            }
        }

        "RESTAURANTS" -> {
            item {
                AdminRestaurantsPanel(viewModel = viewModel, themeColors = themeColors)
            }
        }

        "QUICK_SERVICE" -> {
            item {
                AdminQuickServicePanel(viewModel = viewModel, themeColors = themeColors)
            }
        }

        "MEDICAL" -> {
            item {
                AdminMedicalPanel(viewModel = viewModel, themeColors = themeColors)
            }
        }

        "JOBS", "APPLICANTS" -> {
            item {
                AdminJobsPanel(viewModel = viewModel, themeColors = themeColors, initialTab = activeSubTab)
            }
        }

        "STATS" -> {
            item {
                AdminDashboardPanel(viewModel = viewModel, themeColors = themeColors)
            }
        }

        "REG_REQ" -> {
            item {
                AdminRequestsPanel(viewModel = viewModel, themeColors = themeColors)
            }
        }

        "COMPLAINTS" -> {
            item {
                AdminReportsPanel(viewModel = viewModel, themeColors = themeColors)
            }
        }

        "BLOCKED" -> {
            val blockedProviders = activatedProviders.filter { it.isBlocked }

            item {
                Spacer(modifier = Modifier.height(10.dp))
                Text("🚫 إدارة ومراقبة الفنيين المحظورين", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                Text("استعراض وإلغاء حظر مقدمي الخدمات الموقوفين لأسباب إدارية أو بلاغات:", fontSize = 11.sp, color = themeColors.textSecondary)
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (blockedProviders.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🎉 لا يوجد أي فني محظور حالياً بالمنصة.", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            } else {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            blockedProviders.forEach { provider ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("👤 الفني: ${provider.name}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Text("📞 هاتف: ${provider.phone} | المهنة: ${provider.profession}", fontSize = 11.sp, color = Color.LightGray)
                                        Text("📍 المنطقة: ${provider.area}", fontSize = 10.sp, color = themeColors.textSecondary)
                                    }

                                    Button(
                                        onClick = { viewModel.toggleProviderBlock(provider.id) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                                    ) {
                                        Text("إلغاء الحظر ✅", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                            }
                        }
                    }
                }
            }
        }

        "DELETED" -> {
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Text("🗑️ إدارة ومراقبة الفنيين المحذوفين", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                Text("استعراض واستعادة مقدمي الخدمات الذين تم حذفهم منطقياً من الدليل:", fontSize = 11.sp, color = themeColors.textSecondary)
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (deletedList.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🎉 لا يوجد أي فني محذوف منطقياً حالياً.", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            } else {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            deletedList.forEach { provider ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("👤 الفني: ${provider.name}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Text("📞 هاتف: ${provider.phone} | المهنة: ${provider.profession}", fontSize = 11.sp, color = Color.LightGray)
                                        Text("📍 المنطقة: ${provider.area}", fontSize = 10.sp, color = themeColors.textSecondary)
                                    }

                                    Button(
                                        onClick = { viewModel.restoreProvider(provider.id) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                        modifier = Modifier.padding(start = 8.dp)
                                    ) {
                                        Text("استعادة حساب الفني 🟢", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                            }
                        }
                    }
                }
            }
        }

        "MAP_CONTROLS" -> {
            item {
                Text("🗺️ التحكم بشاشة الخرائط والمواقع الجغرافية", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                Text("إدارة ظهور الخرائط والمنشآت وتحديد مزودي الخرائط ونطاقات التغطية مع المزامنة الفورية:", fontSize = 11.sp, color = themeColors.textSecondary)
                Spacer(modifier = Modifier.height(4.dp))
            }

            item {
                Card(colors = CardDefaults.cardColors(containerColor = themeColors.surface), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("📍 إعدادات مزود ونظام الخرائط:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)

                        val mapProviders = listOf(
                            "MAPLIBRE" to "الخريطة المدمجة 🗺️",
                            "GOOGLE" to "خرائط جوجل 📍",
                            "MAPBOX" to "خريطة شوارع اليمن 🌐"
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            mapProviders.forEach { (provKey, provLabel) ->
                                val isSel = settingsState.mapProvider.equals(provKey, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSel) themeColors.accent else Color.Black.copy(alpha = 0.3f))
                                        .clickable {
                                            viewModel.saveCustomSettingsState(settingsState.copy(mapProvider = provKey))
                                            Toast.makeText(context, "تم تعيين مزود الخريطة: $provLabel", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        provLabel,
                                        fontSize = 9.sp,
                                        color = if (isSel) Color.Black else Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("تفعيل ميزة الخرائط في المنصة بالكامل", fontSize = 12.sp, color = Color.White)
                            Switch(
                                checked = settingsState.isMapFeatureEnabled,
                                onCheckedChange = { enabled ->
                                    viewModel.saveCustomSettingsState(settingsState.copy(isMapFeatureEnabled = enabled))
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = themeColors.accent)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("إظهار أيقونة الخرائط للجميع (الزوار والعملاء)", fontSize = 12.sp, color = Color.White)
                            Switch(
                                checked = settingsState.isMapsIconVisible,
                                onCheckedChange = { visible ->
                                    viewModel.saveCustomSettingsState(settingsState.copy(isMapsIconVisible = visible))
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = themeColors.accent)
                            )
                        }

                        Button(
                            onClick = {
                                viewModel.saveCustomSettingsState(settingsState)
                                Toast.makeText(context, "🗺️ تم حفظ ومزامنة إعدادات الخرائط في Firestore بنجاح", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("حفظ ومزامنة إعدادات الخرائط 💾", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                AdminMapPanel(viewModel = viewModel, themeColors = themeColors)
            }
        }

        "FINANCIAL_REPORTS" -> {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = themeColors.surface), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("📈 تقارير الأرباح المتقدمة", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                        Text("إجمالي العمليات المنفذة: ${bookings.size}", fontSize = 12.sp, color = Color.White)
                        Text("إجمالي الأرباح المتوقعة من العمولات: ${bookings.size * 500} ريال يمني", fontSize = 12.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                        Button(
                            onClick = { },
                            enabled = false,
                            colors = ButtonDefaults.buttonColors(disabledContainerColor = Color.White.copy(alpha = 0.1f), disabledContentColor = Color.Gray)
                        ) {
                            Text("تصدير تقرير الأرباح (PDF) - قيد التطوير ⏳", fontSize = 11.sp)
                        }
                        Text("ميزة تصدير التقارير بصيغة PDF قيد التطوير وستتوفر في التحديث القادم.", fontSize = 10.sp, color = Color.Gray)
                    }
                }
            }
        }

        "SYSTEM_SETTINGS" -> {
            item {
                AdminSystemSettingsScreenContent(viewModel = viewModel, themeColors = themeColors)
            }
        }

        "AUTO_DISPATCH" -> {
            item {
                AdminAutoRoutingScreenContent(viewModel = viewModel, themeColors = themeColors)
            }
        }

        "API_MANAGEMENT" -> {
            item {
                AdminApiKeysScreenContent(viewModel = viewModel, themeColors = themeColors)
            }
        }

        "AI_ASSISTANT_PANEL" -> {
            item {
                AdminAssistantPanel(viewModel = viewModel, themeColors = themeColors)
            }
        }

        "ADMIN_PAYMENT_PANEL" -> {
            item {
                AdminPaymentPanel(viewModel = viewModel, themeColors = themeColors)
            }
        }

        "VOICE_CALLS_PANEL" -> {
            item {
                AdminVoiceCallPanel(viewModel = viewModel, themeColors = themeColors)
            }
        }

        "MAINTENANCE_MODE" -> {
            item {
                AdminMaintenanceScreenContent(viewModel = viewModel, themeColors = themeColors)
            }
        }
    }
}
