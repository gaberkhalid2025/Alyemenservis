package com.example.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.*
import com.example.utils.VisualThemePalette
import com.example.data.*

fun LazyListScope.adminRequestsPanel(viewModel: MainViewModel, themeColors: VisualThemePalette, state: AdminPanelState) {
    with(state) {
        if (activeSubTabState.value == "REG_REQ" || activeSubTabState.value == "MANUAL_ADD") {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("⌛ طلبات الانضمام والاعتماد والإضافة اليدوية", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    val pendingList by viewModel.pendingProviders.collectAsState()
                    if (pendingList.isEmpty()) {
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = themeColors.surface)) {
                            Box(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                                Text("لا توجد طلبات انضمام معلقة حالياً ✅", color = Color.LightGray, fontSize = 12.sp)
                            }
                        }
                    } else {
                        pendingList.forEach { req ->
                            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = themeColors.surface)) {
                                Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("الاسم: ${req.name}", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                    Text("الهاتف: ${req.phone}", color = Color.LightGray, fontSize = 11.sp)
                                    Text("القسم: ${req.categoryId}", color = Color.LightGray, fontSize = 11.sp)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(onClick = { viewModel.approveTechnician(req.id) }, colors = ButtonDefaults.buttonColors(containerColor = Color.Green)) {
                                            Text("قبول الاعتماد", color = Color.Black, fontSize = 10.sp)
                                        }
                                        Button(onClick = { rejectingProviderRequestState.value = req }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                                            Text("رفض", color = Color.White, fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun LazyListScope.adminProvidersPanel(viewModel: MainViewModel, themeColors: VisualThemePalette, state: AdminPanelState) {
    with(state) {
        val tab = activeSubTabState.value
        if (tab in listOf("STORES", "RESTAURANTS", "MEDICAL", "PROPERTIES", "JOBS", "APPLICANTS", "PROVIDERS", "PASSWORDS_RESET")) {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("👥 إدارة أعضاء الدليل والتميز والتصنيفات", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    val providers by viewModel.providers.collectAsState()
                    providers.forEach { p ->
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = themeColors.surface)) {
                            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text(p.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                    Text("الهاتف: ${p.phone} | القسم: ${p.categoryId}", color = Color.LightGray, fontSize = 10.sp)
                                }
                                Button(onClick = { viewModel.removeProvider(p.id) }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f))) {
                                    Text("حذف", fontSize = 10.sp, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun LazyListScope.adminBookingsPanel(viewModel: MainViewModel, themeColors: VisualThemePalette, state: AdminPanelState) {
    with(state) {
        if (activeSubTabState.value == "BOOKINGS") {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("📅 الحجوزات والطلبات الميدانية", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    val bookings by viewModel.bookings.collectAsState()
                    if (bookings.isEmpty()) {
                        Text("لا توجد حجوزات مسجلة حالياً.", color = Color.LightGray, fontSize = 11.sp)
                    } else {
                        bookings.forEach { b ->
                            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = themeColors.surface)) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("حجز للعميل: ${b.clientName} (${b.clientPhone})", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                                    Text("الحالة: ${b.status} | الموعد: ${b.date}", color = Color.LightGray, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun LazyListScope.adminNotificationsPanel(viewModel: MainViewModel, themeColors: VisualThemePalette, state: AdminPanelState) {
    with(state) {
        if (activeSubTabState.value == "NOTIFICATIONS") {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🔔 بث الإشعارات الفورية للعملاء والفنيين", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    OutlinedTextField(
                        value = notifTitleInputState.value,
                        onValueChange = { notifTitleInputState.value = it },
                        label = { Text("عنوان الإشعار") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = notifMsgInputState.value,
                        onValueChange = { notifMsgInputState.value = it },
                        label = { Text("نص الإشعار") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(onClick = {
                        viewModel.broadcastAdminWarning("ALL", "${notifTitleInputState.value}: ${notifMsgInputState.value}")
                        notifTitleInputState.value = ""
                        notifMsgInputState.value = ""
                    }, colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary)) {
                        Text("إرسال الإشعار فوراً 🚀", color = Color.White)
                    }
                }
            }
        }
    }
}

fun LazyListScope.adminChatPanel(viewModel: MainViewModel, themeColors: VisualThemePalette, state: AdminPanelState) {
    with(state) {
        if (activeSubTabState.value in listOf("CHATS", "ADVANCED_CHAT")) {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("💬 رقابة وصلاحيات الدردشات المباشرة", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    Text("مراقبة محادثات الدعم الفني والعملاء والفنيين لضمان الجودة.", color = Color.LightGray, fontSize = 11.sp)
                }
            }
        }
    }
}

fun LazyListScope.adminBannersPanel(viewModel: MainViewModel, themeColors: VisualThemePalette, state: AdminPanelState) {
    with(state) {
        if (activeSubTabState.value == "BANNERS") {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("📢 البنرات الترويجية والتوجيه الإعلاني", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    Text("إدارة البنرات الإعلانية المتحركة في أعلى الشاشة الرئيسية.", color = Color.LightGray, fontSize = 11.sp)
                }
            }
        }
    }
}

fun LazyListScope.adminCategoriesPanel(viewModel: MainViewModel, themeColors: VisualThemePalette, state: AdminPanelState) {
    with(state) {
        if (activeSubTabState.value in listOf("CATEGORIES", "CITIES", "CUSTOM_TABS")) {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🗂️ تحكم الأقسام والخدمات والمدن", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    val categories by viewModel.categories.collectAsState()
                    categories.forEach { cat ->
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = themeColors.surface)) {
                            Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${cat.icon} ${cat.name}", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

fun LazyListScope.adminPaymentsPanel(viewModel: MainViewModel, themeColors: VisualThemePalette, state: AdminPanelState) {
    with(state) {
        if (activeSubTabState.value in listOf("PAYMENTS", "VIP", "COUPONS")) {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("💳 نظام الدفع والتحقق والمحافظ وترقيات VIP", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    Text("إدارة المحافظ الإلكترونية (جيب، جوال، وغيرها) وترقيات التميز وربط الحسابات.", color = Color.LightGray, fontSize = 11.sp)
                }
            }
        }
    }
}

fun LazyListScope.adminSettingsPanel(viewModel: MainViewModel, themeColors: VisualThemePalette, state: AdminPanelState) {
    with(state) {
        if (activeSubTabState.value in listOf("STATS", "COLORS", "REVIEWS", "CALLS", "GOLDEN_ICONS", "CARD_CUSTOMIZER", "NEW_SECTION_CREATOR", "REG_FORMS_MANAGER")) {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("⚙️ الإحصائيات والهوية وتخصيص الواجهات", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    Text("تخصيص ألوان الهوية، أزرار البطائق، مراقبة المكالمات، وإدارة استمارات التسجيل.", color = Color.LightGray, fontSize = 11.sp)
                }
            }
        }
    }
}

fun LazyListScope.adminBackupPanel(viewModel: MainViewModel, themeColors: VisualThemePalette, state: AdminPanelState) {
    with(state) {
        if (activeSubTabState.value in listOf("BACKUP", "CLEAN", "BLOCKED", "DELETED")) {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("💾 النسخ الاحتياطي والمزامنة وتهيئة البيانات", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    Button(onClick = { viewModel.refreshData() }, colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary)) {
                        Text("مزامنة وتحديث البيانات 🔄", color = Color.White)
                    }
                }
            }
        }
    }
}

fun LazyListScope.adminSupervisorsPanel(viewModel: MainViewModel, themeColors: VisualThemePalette, state: AdminPanelState) {
    with(state) {
        if (activeSubTabState.value in listOf("COMPLAINTS", "SUPERVISORS")) {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🛡️ الشكاوى والمشرفين والصلاحيات الإدارية", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    val supervisors by viewModel.supervisors.collectAsState()
                    supervisors.forEach { sup ->
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = themeColors.surface)) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("المشرف: ${sup.name} (${sup.role})", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                                Text("رمز الدخول: ${sup.passcode}", color = Color.LightGray, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
