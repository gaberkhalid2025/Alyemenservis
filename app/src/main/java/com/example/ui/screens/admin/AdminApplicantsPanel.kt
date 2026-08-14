package com.example.ui.screens.admin

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
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

@Composable
fun AdminApplicantsPanel(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    state: AdminPanelState
) {
    if (!PermissionGuard.hasPermission(RoleManager.fromRoleString(viewModel.adminRole.value), "MANAGE_JOB_APPLICANTS")) {
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

    with(state) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("📄 إدارة طلبات المتقدمين للوظائف والسير الذاتية", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)

            OutlinedTextField(
                value = applicantsSearchQueryState.value,
                onValueChange = { applicantsSearchQueryState.value = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("ابحث في المتقدمين...", color = Color.Gray, fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) }
            )

            val appsList by viewModel.jobApplications.collectAsState()
            val filteredApps = appsList.filter { it.applicantName.contains(applicantsSearchQueryState.value, ignoreCase = true) || it.jobTitle.contains(applicantsSearchQueryState.value, ignoreCase = true) }
            if (filteredApps.isEmpty()) {
                Text("لا توجد طلبات تقديم حالياً.", color = Color.LightGray, fontSize = 11.sp)
            } else {
                filteredApps.forEach { app ->
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = themeColors.surface)) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("المتقدم: ${app.applicantName} (${app.applicantPhone})", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                            Text("الوظيفة: ${app.jobTitle} | الحالة: ${app.status}", color = Color.LightGray, fontSize = 10.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { viewModel.updateJobApplicationStatus(app.id, "ACCEPTED") }, colors = ButtonDefaults.buttonColors(containerColor = Color.Green)) {
                                    Text("قبول ✅", fontSize = 9.sp, color = Color.Black)
                                }
                                Button(onClick = { viewModel.updateJobApplicationStatus(app.id, "REJECTED") }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                                    Text("رفض ❌", fontSize = 9.sp, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
