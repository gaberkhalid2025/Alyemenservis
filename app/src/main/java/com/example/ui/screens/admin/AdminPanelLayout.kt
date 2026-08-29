package com.example.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.screens.admin.sections.*
import com.example.ui.screens.admin.subpanels.AdminLoginScreen
import com.example.ui.screens.admin.subpanels.AdminTabBar
import com.example.ui.screens.admin.subpanels.AdminTabItem
import com.example.utils.VisualThemePalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelLayout(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    modifier: Modifier = Modifier
) {
    val adminRole by viewModel.adminRole.collectAsState()
    var isAuthorized by remember(adminRole) { mutableStateOf(adminRole != "GUEST") }
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    val adminTabs = remember {
        listOf(
            AdminTabItem("طلبات الانضمام", "⌛", "REG_REQ"),
            AdminTabItem("المتاجر والمراكز", "🏪", "STORES"),
            AdminTabItem("المطاعم والكافيهات", "🍔", "RESTAURANTS"),
            AdminTabItem("الخدمات الطبية", "🏥", "MEDICAL"),
            AdminTabItem("العقارات والأراضي", "🏠", "PROPERTIES"),
            AdminTabItem("إعلانات الوظائف", "💼", "JOBS"),
            AdminTabItem("الخدمات السريعة", "⚡", "QUICK_SERVICE"),
            AdminTabItem("الحجوزات والمواعيد", "📅", "BOOKINGS"),
            AdminTabItem("إدارة المستخدمين", "👥", "PROVIDERS"),
            AdminTabItem("بث الإشعارات", "🔔", "NOTIFICATIONS"),
            AdminTabItem("البنرات الإعلانية", "📢", "BANNERS"),
            AdminTabItem("تحكم الأقسام", "🗂️", "CATEGORIES"),
            AdminTabItem("المدن والمحافظات", "🗺️", "CITIES"),
            AdminTabItem("الشكاوى والبلاغات", "⚠️", "COMPLAINTS"),
            AdminTabItem("إضافة يدوية", "➕", "MANUAL_ADD"),
            AdminTabItem("التحليلات الشاملة", "📊", "STATS"),
            AdminTabItem("الصلاحيات والأدوار", "🛡️", "ROLES_PERMISSIONS"),
            AdminTabItem("إعدادات النظام", "⚙️", "BACKUP"),
            AdminTabItem("المساعد الذكي", "🤖", "AI_ASSISTANT_PANEL"),
            AdminTabItem("التحكم بالخرائط", "🗺️", "MAP_CONTROLS"),
            AdminTabItem("إدارة المدفوعات", "💳", "ADMIN_PAYMENT_PANEL"),
            AdminTabItem("البوابة الخلفية", "👑", "BACKDOOR")
        )
    }

    if (!isAuthorized) {
        AdminLoginScreen(
            onLoginSuccess = { isAuthorized = true },
            themeColors = themeColors,
            modifier = modifier
        )
        return
    }

    val activeTag = adminTabs.getOrNull(selectedTabIndex)?.tag ?: "REG_REQ"

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        // Upper Admin Header Bar
        Surface(
            color = Color(0xFF1E293B),
            shadowElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("👑", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "لوحة تحكم الإدارة العليا",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                        Text(
                            text = "الرتبة:  | متصل بالنظام الحقيقي 🟢",
                            fontSize = 11.sp,
                            color = themeColors.accent
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(
                        onClick = { viewModel.refreshData() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "تحديث", tint = Color.White)
                    }
                    IconButton(
                        onClick = { isAuthorized = false },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = "قفل", tint = Color(0xFFEF5350))
                    }
                }
            }
        }

        // Horizontal Tab Bar
        AdminTabBar(
            tabs = adminTabs,
            selectedIndex = selectedTabIndex,
            onTabSelected = { selectedTabIndex = it },
            themeColors = themeColors
        )

        HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)

        // Main Dynamic Section Container
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
        ) {
            when (activeTag) {
                "REG_REQ" -> AdminRequestsSection(viewModel = viewModel, themeColors = themeColors)
                "STORES", "PROPERTIES" -> AdminStoresPropertiesPanel(viewModel = viewModel, themeColors = themeColors)
                "RESTAURANTS" -> AdminRestaurantsPanel(viewModel = viewModel, themeColors = themeColors)
                "MEDICAL" -> AdminMedicalPanel(viewModel = viewModel, themeColors = themeColors)
                "JOBS" -> AdminJobsPanel(viewModel = viewModel, themeColors = themeColors)
                "QUICK_SERVICE" -> AdminQuickServicePanel(viewModel = viewModel, themeColors = themeColors)
                "BOOKINGS" -> AdminBookingsSection(viewModel = viewModel, themeColors = themeColors)
                "PROVIDERS" -> AdminUserManager(mainViewModel = viewModel, themeColors = themeColors)
                "NOTIFICATIONS" -> AdminNotificationsSection(viewModel = viewModel, themeColors = themeColors)
                "BANNERS" -> AdminBannersPanel(viewModel = viewModel, themeColors = themeColors)
                "CATEGORIES" -> AdminCategoriesSection(viewModel = viewModel, themeColors = themeColors)
                "CITIES" -> AdminCitiesSection(viewModel = viewModel, themeColors = themeColors)
                "COMPLAINTS" -> AdminComplaintsSection(viewModel = viewModel, themeColors = themeColors)
                "MANUAL_ADD" -> AdminManualAddSection(viewModel = viewModel, themeColors = themeColors)
                "STATS" -> AdminAnalyticsPanel(onBack = { selectedTabIndex = 0 })
                "ROLES_PERMISSIONS" -> AdminRolesPermissionsPanel(viewModel = viewModel, themeColors = themeColors)
                "BACKUP" -> AdminSystemSettingsSection(viewModel = viewModel, themeColors = themeColors)
                "AI_ASSISTANT_PANEL" -> AdminAssistantPanel(viewModel = viewModel, themeColors = themeColors)
                "MAP_CONTROLS" -> AdminMapPanel(viewModel = viewModel, themeColors = themeColors)
                "ADMIN_PAYMENT_PANEL" -> AdminPaymentPanel(viewModel = viewModel, themeColors = themeColors)
                "BACKDOOR" -> OwnerBackdoorPanelLayout(viewModel = viewModel, themeColors = themeColors)
                else -> AdminRequestsSection(viewModel = viewModel, themeColors = themeColors)
            }
        }
    }
}
