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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
                            "MAPLIBRE" to "الخريطة\nالمدمجة 🗺️",
                            "MAPBOX" to "شوارع\nاليمن 🌐",
                            "SATELLITE" to "الأقمار\nالصناعية 🛰️"
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
                                        .heightIn(min = 48.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSel) themeColors.accent else Color.Black.copy(alpha = 0.3f))
                                        .clickable {
                                            viewModel.saveCustomSettingsState(settingsState.copy(mapProvider = provKey))
                                            Toast.makeText(context, "تم تعيين مزود الخريطة: ${provLabel.replace("\n", " ")}", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(vertical = 8.dp, horizontal = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = provLabel,
                                        fontSize = 10.sp,
                                        lineHeight = 13.sp,
                                        color = if (isSel) Color.Black else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        maxLines = 2
                                    )
                                }
                            }
                        }

                        var isEcoLocal by remember(settingsState.isEcoMapDefault) {
                            mutableStateOf(settingsState.isEcoMapDefault || com.example.utils.MapPowerDataPrefs.isEcoMode(context))
                        }

                        // ⚡ وضع توفير بيانات الإنترنت للخرائط
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text("وضع التوفير الفائق لبيانات الإنترنت ⚡", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                Text("تخفيض استهلاك البيانات بنسبة 60%+ عبر منع تحميل البلاطات الإضافية", fontSize = 10.sp, color = Color(0xFF10B981))
                            }
                            Switch(
                                checked = isEcoLocal,
                                onCheckedChange = { eco ->
                                    isEcoLocal = eco
                                    com.example.utils.MapPowerDataPrefs.setEcoMode(context, eco)
                                    viewModel.saveCustomSettingsState(settingsState.copy(isEcoMapDefault = eco))
                                    Toast.makeText(context, if (eco) "تم تفعيل توفير البيانات الفائق ⚡ (توفير 60%+)" else "تم إيقاف وضع توفير البيانات", Toast.LENGTH_SHORT).show()
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFF10B981),
                                    checkedTrackColor = Color(0xFF10B981).copy(alpha = 0.4f),
                                    uncheckedThumbColor = Color.LightGray,
                                    uncheckedTrackColor = Color.White.copy(alpha = 0.2f)
                                )
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("تفعيل ميزة الخرائط في المنصة بالكامل", fontSize = 12.sp, color = Color.White, modifier = Modifier.weight(1f).padding(end = 8.dp))
                            Switch(
                                checked = settingsState.isMapFeatureEnabled,
                                onCheckedChange = { enabled ->
                                    viewModel.saveCustomSettingsState(settingsState.copy(isMapFeatureEnabled = enabled))
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = themeColors.accent,
                                    checkedTrackColor = themeColors.accent.copy(alpha = 0.4f),
                                    uncheckedThumbColor = Color.LightGray,
                                    uncheckedTrackColor = Color.White.copy(alpha = 0.2f)
                                )
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("إظهار أيقونة الخرائط للجميع (الزوار والعملاء)", fontSize = 12.sp, color = Color.White, modifier = Modifier.weight(1f).padding(end = 8.dp))
                            Switch(
                                checked = settingsState.isMapsIconVisible,
                                onCheckedChange = { visible ->
                                    viewModel.saveCustomSettingsState(settingsState.copy(isMapsIconVisible = visible))
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = themeColors.accent,
                                    checkedTrackColor = themeColors.accent.copy(alpha = 0.4f),
                                    uncheckedThumbColor = Color.LightGray,
                                    uncheckedTrackColor = Color.White.copy(alpha = 0.2f)
                                )
                            )
                        }

                        Button(
                            onClick = {
                                viewModel.saveCustomSettingsState(settingsState.copy(isEcoMapDefault = isEcoLocal))
                                Toast.makeText(context, "🗺️ تم حفظ ومزامنة إعدادات الخرائط في Firestore بنجاح", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        ) {
                            Text("حفظ ومزامنة إعدادات الخرائط 💾", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                AdminMapPanel(
                    viewModel = viewModel,
                    themeColors = themeColors,
                    onOpenMap = { viewModel.navigateToScreen(AppScreens.MAP_VIEW) }
                )
            }
        }

        "FINANCIAL_REPORTS" -> {
            item {
                val revStats = androidx.compose.runtime.remember(bookings.size) {
                    viewModel.adminViewModel.getRevenueStats("ALL")
                }
                val bookStats = androidx.compose.runtime.remember(bookings.size) {
                    viewModel.adminViewModel.getBookingStats("ALL")
                }
                Card(colors = CardDefaults.cardColors(containerColor = themeColors.surface), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("📈 تقارير الأرباح والإيرادات الفعلية", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                        Text("إجمالي الحجوزات: ${bookStats.total} | مكتملة: ${bookStats.completed} | جارية: ${bookStats.inProgress}", fontSize = 12.sp, color = Color.White)
                        Text("إجمالي الإيرادات المسجلة: ${String.format(java.util.Locale.US, "%.0f", revStats.totalRevenue)} ريال يمني", fontSize = 12.sp, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                        Text("صافي عمولة المنصة: ${String.format(java.util.Locale.US, "%.0f", revStats.platformCommission)} ريال يمني", fontSize = 12.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                        Text("مستحقات مقدمي الخدمات: ${String.format(java.util.Locale.US, "%.0f", revStats.providerPayouts)} ريال يمني", fontSize = 11.5.sp, color = Color.LightGray)
                        Button(
                            onClick = {
                                val csv = buildString {
                                    appendLine("Metric,Value,Currency")
                                    appendLine("TotalBookings,${bookStats.total},COUNT")
                                    appendLine("CompletedBookings,${bookStats.completed},COUNT")
                                    appendLine("TotalRevenue,${revStats.totalRevenue},${revStats.currency}")
                                    appendLine("PlatformCommission,${revStats.platformCommission},${revStats.currency}")
                                    appendLine("ProviderPayouts,${revStats.providerPayouts},${revStats.currency}")
                                }
                                viewModel.adminViewModel.exportReport("FINANCIAL_CSV")
                                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "text/csv"
                                    putExtra(android.content.Intent.EXTRA_SUBJECT, "تقرير الأرباح والعمولات")
                                    putExtra(android.content.Intent.EXTRA_TEXT, csv)
                                }
                                context.startActivity(android.content.Intent.createChooser(shareIntent, "تصدير ومشاركة تقرير الأرباح"))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        ) {
                            Text("📤 تصدير ومشاركة التقرير المالي (CSV)", color = Color.Black, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
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
