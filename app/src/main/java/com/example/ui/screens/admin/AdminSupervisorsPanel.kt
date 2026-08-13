package com.example.ui.screens.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
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
fun AdminSupervisorsPanel(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    state: AdminPanelState
) {
    val currentRole = RoleManager.fromRoleString(viewModel.adminRole.value)
    if (!PermissionGuard.hasPermission(currentRole, "MANAGE_SUPERVISORS")) {
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

    Column(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("🛡️ الشكاوى والمشرفين والصلاحيات الإدارية", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
        
        val reports by viewModel.reports.collectAsState()
        var selectedReportFilter by remember { mutableStateOf("ALL") }
        
        Text("⚠️ شكاوى وبلاغات الأعضاء والمستخدمين:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
        
        // Filter row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val filters = listOf(
                Pair("ALL", "الكل"),
                Pair("SERVICES", "🛠️ الفنيين"),
                Pair("STORES", "🏪 المحلات"),
                Pair("RESTAURANTS", "🍔 المطاعم"),
                Pair("MEDICAL", "🏥 طبية"),
                Pair("PROPERTIES", "🏠 عقارات"),
                Pair("JOBS", "💼 وظائف")
            )
            filters.forEach { item ->
                val isSelected = selectedReportFilter == item.first
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) themeColors.accent else Color.Transparent)
                        .clickable { selectedReportFilter = item.first }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = item.second,
                        color = if (isSelected) Color.Black else Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        
        val filteredReports = remember(reports, selectedReportFilter) {
            if (selectedReportFilter == "ALL") reports else {
                reports.filter { it.targetType.uppercase() == selectedReportFilter || (selectedReportFilter == "SERVICES" && it.targetType.isEmpty()) }
            }
        }
        
        if (filteredReports.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                Text("لا توجد بلاغات مسجلة في هذا القسم", color = Color.Gray, fontSize = 11.sp)
            }
        } else {
            filteredReports.forEach { rep ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                    border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("بلاغ عن: ${rep.providerName}", fontWeight = FontWeight.Bold, color = themeColors.accent, fontSize = 12.sp)
                            Text(
                                text = when(rep.targetType) {
                                    "STORES" -> "🏪 متجر"
                                    "RESTAURANTS" -> "🍔 مطعم"
                                    "MEDICAL" -> "🏥 منشأة طبية"
                                    "PROPERTIES" -> "🏠 عقار"
                                    "JOBS" -> "💼 وظيفة"
                                    else -> "🛠️ فني"
                                },
                                color = Color.LightGray,
                                fontSize = 9.sp,
                                modifier = Modifier
                                    .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text("بواسطة: ${rep.reporterName}", color = Color.White, fontSize = 10.sp)
                        Text("محتوى الشكوى: ${rep.content}", color = Color.White, fontSize = 11.sp)
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(
                                onClick = { viewModel.deleteReport(rep.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier = Modifier.height(26.dp),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text("حذف البلاغ 🗑️", fontSize = 9.sp, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
        
        HorizontalDivider(color = Color.Gray.copy(alpha = 0.15f))
        
        Text("📋 إضافة وإدارة المشرفين:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
        
        val supName = state.supervisorInputNameState.value
        val supPasscode = state.supervisorInputPasscodeState.value
        val supRole = state.supervisorInputRoleState.value.ifBlank { "SUPERVISOR" }

        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = themeColors.surface)) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("إضافة مشرف جديد:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                OutlinedTextField(
                    value = supName,
                    onValueChange = { state.supervisorInputNameState.value = it },
                    label = { Text("اسم المشرف") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = supPasscode,
                    onValueChange = { state.supervisorInputPasscodeState.value = it },
                    label = { Text("رمز الدخول (Passcode)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        if (supName.isNotBlank() && supPasscode.isNotBlank()) {
                            viewModel.addSupervisor(supName, supRole, supPasscode)
                            state.supervisorInputNameState.value = ""
                            state.supervisorInputPasscodeState.value = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary)
                ) {
                    Text("إضافة المشرف 🛡️", color = Color.White)
                }
            }
        }

        val supervisors by viewModel.supervisors.collectAsState()
        supervisors.forEach { sup ->
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = themeColors.surface)) {
                Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("المشرف: ${sup.name} (${sup.role})", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                        Text("رمز الدخول: ${sup.passcode}", color = Color.LightGray, fontSize = 10.sp)
                    }
                    IconButton(onClick = { viewModel.deleteSupervisor(sup.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color.Red)
                    }
                }
            }
        }
    }
}
