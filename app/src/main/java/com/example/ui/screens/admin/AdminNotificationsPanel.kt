package com.example.ui.screens.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.MainViewModel
import com.example.utils.VisualThemePalette
import com.example.util.PermissionGuard
import com.example.util.RoleManager
import com.example.util.UserRole

@Composable
fun AdminNotificationsPanel(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    state: AdminPanelState
) {
    val currentRole = RoleManager.fromRoleString(viewModel.adminRole.value)
    if (!PermissionGuard.hasPermission(currentRole, "MANAGE_NOTIFICATIONS")) {
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("🔔 بث الإشعارات الفورية الموجهة للأقسام والأعضاء", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
        Text("بث إشعارات فورية مخصصة وموجهة حسب الفئة لضمان وصول التنبيهات للشرائح المناسبة.", color = Color.LightGray, fontSize = 11.sp)
        
        val notifTitle = state.notifTitleInputState.value
        val notifMsg = state.notifMsgInputState.value
        val targetTypeSelected = state.notifTargetTypeState.value
        val notificationsList by viewModel.notifications.collectAsState()
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = notifTitle,
                    onValueChange = { state.notifTitleInputState.value = it },
                    label = { Text("عنوان الإشعار", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = Color.White)
                )
                
                OutlinedTextField(
                    value = notifMsg,
                    onValueChange = { state.notifMsgInputState.value = it },
                    label = { Text("نص ومحتوى الإشعار", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = Color.White)
                )
                
                // Target Type Selector Grid
                Text("🎯 الفئة المستهدفة بالإشعار:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                val targets = listOf(
                    Pair("ALL", "الكل 👥"),
                    Pair("PROVIDER", "الفنيين 🛠️"),
                    Pair("STORE", "المحلات 🏪"),
                    Pair("RESTAURANT", "المطاعم 🍔"),
                    Pair("MEDICAL", "المنشآت الطبية 🏥"),
                    Pair("PROPERTY", "العقارات 🏠"),
                    Pair("JOB", "الوظائف 💼")
                )
                
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    targets.chunked(2).forEach { rowTargets ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            rowTargets.forEach { item ->
                                val isSel = targetTypeSelected == item.first
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSel) themeColors.accent else Color.White.copy(alpha = 0.05f))
                                        .clickable { state.notifTargetTypeState.value = item.first }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = item.second,
                                        fontSize = 10.sp,
                                        color = if (isSel) Color.Black else Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            if (rowTargets.size < 2) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
                
                Button(
                    onClick = {
                        if (notifTitle.isNotEmpty() && notifMsg.isNotEmpty()) {
                            viewModel.addNotification(
                                title = notifTitle.trim(),
                                message = notifMsg.trim(),
                                targetType = targetTypeSelected,
                                targetValue = state.notifTargetValueState.value.ifBlank { "all" }
                            )
                            state.notifTitleInputState.value = ""
                            state.notifMsgInputState.value = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("إرسال الإشعار فوراً 🚀", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(6.dp))
        
        // Notifications History
        Text("📋 سجل الإشعارات المرسلة إدارياً (${notificationsList.size}):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
        
        if (notificationsList.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                Text("لا توجد إشعارات مرسلة بعد", color = Color.Gray, fontSize = 11.sp)
            }
        } else {
            notificationsList.take(20).forEach { notif ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = themeColors.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(notif.title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text(
                                    text = when (notif.targetType) {
                                        "ALL" -> "الكل 👥"
                                        "PROVIDER" -> "فني 🛠️"
                                        "STORE" -> "متجر 🏪"
                                        "RESTAURANT" -> "مطعم 🍔"
                                        "MEDICAL" -> "طبي 🏥"
                                        "PROPERTY" -> "عقار 🏠"
                                        "JOB" -> "وظيفة 💼"
                                        else -> notif.targetType
                                    },
                                    fontSize = 8.sp,
                                    color = themeColors.accent,
                                    modifier = Modifier
                                        .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                            Text(notif.message, fontSize = 10.sp, color = Color.LightGray)
                        }
                        
                        IconButton(onClick = { viewModel.deleteNotification(notif.id) }) {
                            Text("🗑️", fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}
