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
fun AdminJobsPanel(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    state: AdminPanelState
) {
    if (!PermissionGuard.hasPermission(RoleManager.fromRoleString(viewModel.adminRole.value), "MANAGE_JOBS")) {
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
            Text("💼 إدارة إعلانات الوظائف والشركات المعلنة", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
            
            var jobTitle by remember { mutableStateOf("") }
            var companyName by remember { mutableStateOf("") }
            var jobSalary by remember { mutableStateOf("") }
            var jobPhone by remember { mutableStateOf("") }

            OutlinedTextField(
                value = activeJobsSearchQueryState.value,
                onValueChange = { activeJobsSearchQueryState.value = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("ابحث في الوظائف...", color = Color.Gray, fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) }
            )

            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = themeColors.surface)) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("➕ إضافة وظيفة جديدة يدوياً:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    OutlinedTextField(value = jobTitle, onValueChange = { jobTitle = it }, label = { Text("المسمى الوظيفي") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = companyName, onValueChange = { companyName = it }, label = { Text("اسم الشركة") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = jobSalary, onValueChange = { jobSalary = it }, label = { Text("الراتب") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = jobPhone, onValueChange = { jobPhone = it }, label = { Text("هاتف التواصل") }, modifier = Modifier.fillMaxWidth())
                    Button(
                        onClick = {
                            if (jobTitle.isNotBlank()) {
                                viewModel.addJob(jobTitle, companyName, "تفاصيل الوظيفة", jobPhone, jobSalary)
                                jobTitle = ""
                                companyName = ""
                                jobSalary = ""
                                jobPhone = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary)
                    ) {
                        Text("إضافة الوظيفة 💼", color = Color.White)
                    }
                }
            }

            val jobsList by viewModel.jobs.collectAsState()
            val filteredJobs = jobsList.filter { it.title.contains(activeJobsSearchQueryState.value, ignoreCase = true) }
            filteredJobs.forEach { job ->
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = themeColors.surface)) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("${job.title} - ${job.companyName}", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                        Text("الراتب: ${job.salary}", color = Color.LightGray, fontSize = 10.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { viewModel.toggleJobPin(job.id) }, colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)) {
                                Text(if (job.isPinned) "إلغاء التثبيت" else "تثبيت 📌", fontSize = 9.sp, color = Color.White)
                            }
                            Button(onClick = { viewModel.deleteJob(job.id) }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f))) {
                                Text("حذف 🗑️", fontSize = 9.sp, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}
