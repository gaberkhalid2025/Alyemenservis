@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.ui.screens.admin

import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import com.example.ui.MainViewModel
import com.example.utils.VisualThemePalette
import com.example.util.PermissionGuard
import com.example.util.RoleManager
import com.example.data.CustomProfileTabEntity

@Composable
fun AdminCustomTabsPanel(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    state: AdminPanelState
) {
    if (!PermissionGuard.hasPermission(RoleManager.fromRoleString(viewModel.adminRole.value), "MANAGE_CUSTOM_TABS")) {
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

    val context = LocalContext.current
    val customTabsListState by viewModel.customProfileTabs.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("📑 تخصيص وتبويبات ملفات المشتركين والمحلات والعقارات", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
        Text("يمكن للأدمن إنشاء وتخصيص تبويبات ديناميكية جديدة تظهر في ملفات مقدمي الخدمة والمحلات والعقارات:", fontSize = 11.sp, color = themeColors.textSecondary)
        Spacer(modifier = Modifier.height(4.dp))

        // Create new tab card
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("إنشاء تبويب مخصص جديد ➕", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)

                var newTabTitle by remember { mutableStateOf("") }
                var newTabIcon by remember { mutableStateOf("📑") }
                var newTabTarget by remember { mutableStateOf("ALL") } // "ALL", "PROVIDERS", "STORES", "PROPERTIES"
                var newTabContent by remember { mutableStateOf("") }

                OutlinedTextField(
                    value = newTabTitle,
                    onValueChange = { newTabTitle = it },
                    label = { Text("عنوان التبويب (مثال: 📜 الآراء والشهادات، 💬 التقييمات، 💼 الأعمال)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                OutlinedTextField(
                    value = newTabIcon,
                    onValueChange = { newTabIcon = it },
                    label = { Text("رمز / أيقونة التبويب (إيموجي)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                Text("النطاق المستهدف للتبويب:", fontSize = 11.sp, color = Color.White)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        "ALL" to "الكل 🌐",
                        "PROVIDERS" to "الفنيين 👷",
                        "STORES" to "المحلات 🏪",
                        "PROPERTIES" to "العقارات 🏠"
                    ).forEach { (targetVal, label) ->
                        FilterChip(
                            selected = newTabTarget == targetVal,
                            onClick = { newTabTarget = targetVal },
                            label = { Text(label, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = themeColors.accent,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = newTabContent,
                    onValueChange = { newTabContent = it },
                    label = { Text("وصف أو نص التبويب الافتراضي (اختياري)") },
                    modifier = Modifier.fillMaxWidth().height(80.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                Button(
                    onClick = {
                        if (newTabTitle.trim().isEmpty()) {
                            Toast.makeText(context, "الرجاء كتابة عنوان التبويب", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.saveCustomProfileTab(
                                CustomProfileTabEntity(
                                    title = newTabTitle.trim(),
                                    icon = newTabIcon.trim(),
                                    targetType = newTabTarget,
                                    contentHtmlOrText = newTabContent.trim(),
                                    isEnabled = true
                                )
                            )
                            newTabTitle = ""
                            newTabContent = ""
                            Toast.makeText(context, "تم حفظ التبويب بنجاح 📑", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("إضافة التبويب فوراً", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Render saved tabs
        Text("التبويبات المخصصة المضافة (${customTabsListState.size}):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
        if (customTabsListState.isEmpty()) {
            Text("لا توجد تبويبات مخصصة مضافة حالياً.", fontSize = 11.sp, color = Color.Gray)
        } else {
            customTabsListState.forEach { tab ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("${tab.icon} ${tab.title}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("المستهدف: ${when(tab.targetType) { "PROVIDERS" -> "الفنيين 👷"; "STORES" -> "المحلات 🏪"; "PROPERTIES" -> "العقارات 🏠"; else -> "الكل 🌐" }}", fontSize = 10.sp, color = themeColors.textSecondary)
                            if (tab.contentHtmlOrText.isNotEmpty()) {
                                Text(tab.contentHtmlOrText, fontSize = 9.sp, color = Color.Gray)
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Switch(
                                checked = tab.isEnabled,
                                onCheckedChange = { viewModel.toggleCustomProfileTab(tab.id) }
                            )
                            IconButton(onClick = { viewModel.deleteCustomProfileTab(tab.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "حذف التبويب", tint = Color.Red)
                            }
                        }
                    }
                }
            }
        }
    }
}
