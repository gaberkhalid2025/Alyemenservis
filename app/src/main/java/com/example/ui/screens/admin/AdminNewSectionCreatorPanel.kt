@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.ui.screens.admin

import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import com.example.ui.MainViewModel
import com.example.utils.VisualThemePalette
import com.example.util.PermissionGuard
import com.example.util.RoleManager

@Composable
fun AdminNewSectionCreatorPanel(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    state: AdminPanelState
) {
    if (!PermissionGuard.hasPermission(RoleManager.fromRoleString(viewModel.adminRole.value), "MANAGE_NEW_SECTIONS")) {
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

    var newSecName by remember { mutableStateOf("") }
    var newSecType by remember { mutableStateOf("DELIVERY") } // DELIVERY, WALLET, CUSTOM
    var newSecIcon by remember { mutableStateOf("🚀") }
    var linkToMap by remember { mutableStateOf(true) }
    var linkToOrders by remember { mutableStateOf(true) }
    var linkToPayment by remember { mutableStateOf(true) }
    var secNotifications by remember { mutableStateOf(true) }
    var secBookings by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("➕ إضافة قسم جديد ومحافظ رقمية وتوصيل شامل", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                Text("إنشاء قسم جديد بالكامل (مثل التوصيل، المحافظ الرقمية الداخلية، أو أي قسم خدمي) مع ربطه بالشاشات والطلبات والمحافظ:", fontSize = 11.sp, color = themeColors.textSecondary)

                OutlinedTextField(
                    value = newSecName,
                    onValueChange = { newSecName = it },
                    label = { Text("اسم القسم الجديد (مثال: خدمة التوصيل السريع، المحفظة الرقمية)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                Text("تصنيف وطبيعة القسم:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("DELIVERY" to "قسم التوصيل 🚚", "WALLET" to "محفظة رقمية 💳", "CUSTOM" to "قسم خدمي عام 🌟").forEach { (t, lbl) ->
                        FilterChip(
                            selected = newSecType == t,
                            onClick = { newSecType = t },
                            label = { Text(lbl, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = themeColors.accent, selectedLabelColor = Color.Black)
                        )
                    }
                }

                OutlinedTextField(
                    value = newSecIcon,
                    onValueChange = { newSecIcon = it },
                    label = { Text("أيقونة أو إيموجي القسم الجديد") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                Text("الربط والتحكم بالخدمات المرتبطة:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) { Switch(checked = linkToMap, onCheckedChange = { linkToMap = it }); Spacer(modifier = Modifier.width(8.dp)); Text("ربط القسم بشاشة الخرائط والمواقع 🗺️", color = Color.White, fontSize = 11.sp) }
                    Row(verticalAlignment = Alignment.CenterVertically) { Switch(checked = linkToOrders, onCheckedChange = { linkToOrders = it }); Spacer(modifier = Modifier.width(8.dp)); Text("ربط القسم بشاشة الطلبات (طلباتي) 📦", color = Color.White, fontSize = 11.sp) }
                    Row(verticalAlignment = Alignment.CenterVertically) { Switch(checked = linkToPayment, onCheckedChange = { linkToPayment = it }); Spacer(modifier = Modifier.width(8.dp)); Text("ربط القسم بشاشة الدفع والمحافظ والتحقق 💳", color = Color.White, fontSize = 11.sp) }
                    Row(verticalAlignment = Alignment.CenterVertically) { Switch(checked = secNotifications, onCheckedChange = { secNotifications = it }); Spacer(modifier = Modifier.width(8.dp)); Text("تمكين إشعارات التنبيه المباشرة للقسم 🔔", color = Color.White, fontSize = 11.sp) }
                    Row(verticalAlignment = Alignment.CenterVertically) { Switch(checked = secBookings, onCheckedChange = { secBookings = it }); Spacer(modifier = Modifier.width(8.dp)); Text("تمكين نظام الحجوزات وإدارة الطلبات للقسم 📅", color = Color.White, fontSize = 11.sp) }
                }

                Button(
                    onClick = {
                        if (newSecName.isNotBlank()) {
                            viewModel.addNewCategory(
                                nameAr = newSecName.trim(),
                                nameEn = newSecName.trim(),
                                icon = newSecIcon.trim(),
                                description = "",
                                parentId = "",
                                isMainCategory = true
                            )
                            Toast.makeText(context, "🚀 تمت إضافة القسم الجديد ومزامنته بنجاح تام!", Toast.LENGTH_LONG).show()
                            newSecName = ""
                        } else {
                            Toast.makeText(context, "⚠️ الرجاء إدخال اسم القسم الجديد", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("إنشاء وتفعيل القسم الجديد فورياً 🚀", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
