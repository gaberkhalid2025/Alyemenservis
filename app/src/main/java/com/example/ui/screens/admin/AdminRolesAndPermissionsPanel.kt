@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.ui.screens.admin

import androidx.compose.foundation.*
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
import com.example.util.UserRole

@Composable
fun AdminRolesAndPermissionsPanel(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    state: AdminPanelState
) {
    if (!PermissionGuard.hasPermission(RoleManager.fromRoleString(viewModel.adminRole.value), "MANAGE_ROLES")) {
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

    val permissionsList = listOf(
        Pair("طلبات الانضمام والاعتماد", "MANAGE_PROVIDERS"),
        Pair("الإضافة اليدوية للإدارة", "MANAGE_PROVIDERS"),
        Pair("أعضاء الدليل والتميز", "MANAGE_PROVIDERS"),
        Pair("المحلات التجارية والمراكز", "MANAGE_STORES"),
        Pair("المطاعم والكافيهات", "MANAGE_RESTAURANTS"),
        Pair("المراكز الطبية والعيادات", "MANAGE_MEDICAL"),
        Pair("العقارات والأراضي", "MANAGE_PROPERTIES"),
        Pair("المعلنين عن الوظائف", "MANAGE_JOBS"),
        Pair("المتقدمين للوظائف", "MANAGE_JOB_APPLICANTS"),
        Pair("الإحصائيات الشاملة", "VIEW_STATS"),
        Pair("الحجوزات والطلبات", "MANAGE_BOOKINGS"),
        Pair("رقابة وصلاحيات الدردشات", "MANAGE_CHAT"),
        Pair("بث الإشعارات الموجهة", "MANAGE_NOTIFICATIONS"),
        Pair("البنرات الترويجية والتوجيه", "MANAGE_BANNERS"),
        Pair("تحكم الأقسام وتصنيف المهن", "MANAGE_CATEGORIES"),
        Pair("تحكم المحافظات والمدن", "MANAGE_CITIES"),
        Pair("الشكاوى والبلاغات", "VIEW_REPORTS"),
        Pair("المشرفين والصلاحيات", "MANAGE_SUPERVISORS"),
        Pair("الهوية والألوان والتخصيص", "MANAGE_THEMES"),
        Pair("النسخ الاحتياطي والمزامنة", "MANAGE_BACKUP"),
        Pair("تهيئة وتطهير البيانات", "CLEAN_DATABASE"),
        Pair("إدارة التقييمات والتعليقات", "MANAGE_REVIEWS"),
        Pair("مراقبة المكالمات", "VIEW_CALLS"),
        Pair("إدارة الكوبونات", "MANAGE_COUPONS"),
        Pair("القائمة المحظورة المركزية", "MANAGE_BLOCKED"),
        Pair("سلة المحذوفات المركزية", "MANAGE_DELETED"),
        Pair("نظام الدفع والتحقق والمحافظ", "MANAGE_PAYMENTS"),
        Pair("تخصيص تبويبات الملفات", "MANAGE_CUSTOM_TABS"),
        Pair("الأيقونات الذهبية والخط", "MANAGE_THEMES"),
        Pair("صلاحيات وتوجيه الدردشات", "MANAGE_ADVANCED_CHAT"),
        Pair("توجيه الحجوزات الآلي", "MANAGE_BOOKING_ROUTING"),
        Pair("تخصيص أشكال وأزرار البطائق", "MANAGE_CARD_CUSTOMIZER"),
        Pair("إضافة وإدارة الأقسام الجديدة", "MANAGE_NEW_SECTIONS"),
        Pair("تخصيص استمارات التسجيل", "MANAGE_REG_FORMS"),
        Pair("الصلاحيات والأدوار", "MANAGE_ROLES"),
        Pair("إعادة تعيين كلمات المرور", "MANAGE_USERS"),
        Pair("إعدادات البوابة الخلفية", "OWNER_PANEL")
    )

    var searchQuery by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("🛡️ جدول الصلاحيات والأدوار الإدارية الشاملة", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
        Text("توزيع الصلاحيات المطلوبة لكل لوحة تحكم في النظام لتطبيق الأمان والمصادقة الصارمة:", fontSize = 11.sp, color = themeColors.textSecondary)

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("بحث عن لوحة أو كود صلاحية...") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
            trailingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = themeColors.accent) }
        )

        val filtered = permissionsList.filter {
            it.first.contains(searchQuery, true) || it.second.contains(searchQuery, true)
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                filtered.forEachIndexed { index, (panelName, permCode) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(panelName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("كود الصلاحية: $permCode", fontSize = 10.sp, color = themeColors.accent)
                        }
                        Box(
                            modifier = Modifier
                                .background(themeColors.accent.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("مفعلة 🔐", fontSize = 9.sp, color = themeColors.accent, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (index < filtered.size - 1) {
                        HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                    }
                }
            }
        }
    }
}
