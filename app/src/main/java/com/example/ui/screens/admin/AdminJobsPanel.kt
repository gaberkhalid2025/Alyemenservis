package com.example.ui.screens.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.JobEntity
import com.example.data.JobApplicationEntity
import com.example.ui.MainViewModel
import com.example.ui.screens.admin.components.AdminEntityCard
import com.example.ui.screens.admin.components.AdminFilterChips
import com.example.utils.VisualThemePalette
import kotlinx.coroutines.launch

/**
 * 💼 Admin Panel: Jobs Management (إدارة التوظيف وإعلانات الوظائف)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminJobsPanel(
    onBack: () -> Unit = {},
    viewModel: MainViewModel = viewModel(),
    themeColors: VisualThemePalette,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val jobs by viewModel.jobs.collectAsState()
    val applications by viewModel.jobApplications.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("الكل") }
    val filters = listOf("الكل", "مميز VIP", "نشط")

    // Add / Edit Job Dialog States
    var showAddEditDialog by remember { mutableStateOf(false) }
    var editingJob by remember { mutableStateOf<JobEntity?>(null) }
    var jobTitleState by remember { mutableStateOf("") }
    var jobCompanyState by remember { mutableStateOf("") }
    var jobManagerState by remember { mutableStateOf("") }
    var jobPhoneState by remember { mutableStateOf("") }
    var jobCityState by remember { mutableStateOf("") }
    var jobSalaryState by remember { mutableStateOf("") }
    var jobDescState by remember { mutableStateOf("") }
    var jobReqsState by remember { mutableStateOf("") }

    // Applicants Dialog States
    var showApplicantsDialog by remember { mutableStateOf(false) }
    var selectedJobForApplicants by remember { mutableStateOf<JobEntity?>(null) }

    val filteredJobs = remember(jobs, selectedFilter, searchQuery) {
        jobs.filter { job ->
            val matchesFilter = when (selectedFilter) {
                "مميز VIP" -> job.isVip
                "نشط" -> !job.isDeleted
                else -> true
            }
            val matchesSearch = searchQuery.isBlank() || 
                    job.title.contains(searchQuery, ignoreCase = true) ||
                    job.companyName.contains(searchQuery, ignoreCase = true) ||
                    job.managerName.contains(searchQuery, ignoreCase = true) ||
                    job.phone.contains(searchQuery)
            
            matchesFilter && matchesSearch
        }.sortedByDescending { it.createdAt }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("💼 إدارة فرص العمل والوظائف", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        editingJob = null
                        jobTitleState = ""
                        jobCompanyState = ""
                        jobManagerState = ""
                        jobPhoneState = ""
                        jobCityState = ""
                        jobSalaryState = ""
                        jobDescState = ""
                        jobReqsState = ""
                        showAddEditDialog = true
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "إضافة وظيفة", tint = themeColors.accent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        containerColor = Color(0xFF0F172A)
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("بحث باسم الوظيفة، الشركة أو رقم الهاتف...", color = Color.Gray, fontSize = 13.sp) },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "مسح", tint = Color.Gray)
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = themeColors.accent,
                    unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
                )
            )

            AdminFilterChips(
                categories = filters,
                selectedCategory = selectedFilter,
                onSelectCategory = { selectedFilter = it },
                themeColors = themeColors
            )

            if (filteredJobs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("لا توجد إعلانات وظائف تطابق البحث حالياً", color = Color.Gray, fontSize = 13.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredJobs, key = { it.id }) { job ->
                        val applicantCount = applications.count { it.jobId == job.id }
                        AdminEntityCard(
                            title = job.title.ifBlank { "فرصة عمل" },
                            subtitle = "🏢 ${job.companyName} • 📍 ${job.cityId} • 💰 ${job.salary}",
                            details = "📝 ${job.description.take(60)}...",
                            statusText = if (job.isVip) "VIP ⭐" else "عادي",
                            statusColor = if (job.isVip) Color(0xFFF59E0B) else Color(0xFF10B981),
                            isVip = job.isVip,
                            themeColors = themeColors,
                            actions = {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                viewModel.setJobVip(job.id, !job.isVip)
                                                scope.launch { snackbarHostState.showSnackbar(if (job.isVip) "تم إلغاء شارة VIP" else "تمت إضافة شارة VIP") }
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, if (job.isVip) Color(0xFFF59E0B) else themeColors.accent),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(if (job.isVip) "إلغاء VIP" else "ترقية VIP", fontSize = 10.5.sp, color = if (job.isVip) Color(0xFFF59E0B) else themeColors.accent)
                                        }

                                        Button(
                                            onClick = {
                                                selectedJobForApplicants = job
                                                showApplicantsDialog = true
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1.2f)
                                        ) {
                                            Text("📋 المتقدمين ($applicantCount)", fontSize = 10.5.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                editingJob = job
                                                jobTitleState = job.title
                                                jobCompanyState = job.companyName
                                                jobManagerState = job.managerName
                                                jobPhoneState = job.phone
                                                jobCityState = job.cityId
                                                jobSalaryState = job.salary
                                                jobDescState = job.description
                                                jobReqsState = job.requirements
                                                showAddEditDialog = true
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("تعديل 📝", fontSize = 11.sp, color = Color.White)
                                        }

                                        IconButton(
                                            onClick = {
                                                viewModel.deleteJobPermanently(job.id)
                                                scope.launch { snackbarHostState.showSnackbar("🗑️ تم حذف الإعلان الوظيفي") }
                                            },
                                            modifier = Modifier.background(Color(0xFFEF5350).copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color(0xFFEF5350), modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Add / Edit Job Dialog
    if (showAddEditDialog) {
        AlertDialog(
            onDismissRequest = { showAddEditDialog = false },
            containerColor = Color(0xFF1E293B),
            title = { Text(if (editingJob == null) "💼 إضافة إعلان وظيفي جديد" else "📝 تعديل الإعلان الوظيفي", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        OutlinedTextField(
                            value = jobTitleState,
                            onValueChange = { jobTitleState = it },
                            label = { Text("المسمى الوظيفي") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = jobCompanyState,
                            onValueChange = { jobCompanyState = it },
                            label = { Text("اسم الشركة") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = jobManagerState,
                            onValueChange = { jobManagerState = it },
                            label = { Text("المسؤول") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = jobPhoneState,
                            onValueChange = { jobPhoneState = it },
                            label = { Text("رقم الهاتف") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = jobCityState,
                            onValueChange = { jobCityState = it },
                            label = { Text("المحافظة") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = jobSalaryState,
                            onValueChange = { jobSalaryState = it },
                            label = { Text("الراتب المتوقع") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = jobDescState,
                            onValueChange = { jobDescState = it },
                            label = { Text("وصف الوظيفة") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 4,
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = jobReqsState,
                            onValueChange = { jobReqsState = it },
                            label = { Text("الشروط والمؤهلات") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 4,
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val baseJob = editingJob ?: JobEntity()
                        val finalJob = baseJob.copy(
                            title = jobTitleState,
                            companyName = jobCompanyState,
                            managerName = jobManagerState,
                            phone = jobPhoneState,
                            cityId = jobCityState,
                            salary = jobSalaryState,
                            description = jobDescState,
                            requirements = jobReqsState,
                            isApproved = true,
                            isActive = true
                        )
                        viewModel.saveJob(finalJob)
                        showAddEditDialog = false
                        scope.launch { snackbarHostState.showSnackbar("💾 تم حفظ الإعلان الوظيفي بنجاح") }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent)
                ) {
                    Text("حفظ", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddEditDialog = false }) {
                    Text("إلغاء", color = Color.Gray)
                }
            }
        )
    }

    // Applicants Viewer Dialog
    if (showApplicantsDialog && selectedJobForApplicants != null) {
        val job = selectedJobForApplicants!!
        val jobApps = applications.filter { it.jobId == job.id }

        AlertDialog(
            onDismissRequest = { showApplicantsDialog = false },
            containerColor = Color(0xFF1E293B),
            title = {
                Text("📋 المتقدمين لوظيفة: ${job.title}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (jobApps.isEmpty()) {
                        Text("لا يوجد متقدمين لهذه الوظيفة بعد.", color = Color.Gray, fontSize = 12.sp)
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.heightIn(max = 400.dp)
                        ) {
                            items(jobApps) { app ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.2f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(app.applicantName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            IconButton(
                                                onClick = {
                                                    viewModel.deleteJobApplication(app.id)
                                                    scope.launch { snackbarHostState.showSnackbar("🗑️ تم حذف طلب التقديم") }
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "حذف المتقدم", tint = Color(0xFFEF5350), modifier = Modifier.size(16.dp))
                                            }
                                        }
                                        Text("📞 رقم الهاتف: ${app.applicantPhone}", color = Color.LightGray, fontSize = 11.sp)
                                        Text("🎓 المؤهلات: ${app.applicantQuals}", color = Color.LightGray, fontSize = 11.sp)
                                        Text("📊 الحالة: ${app.status}", color = themeColors.accent, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showApplicantsDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary)
                ) {
                    Text("إغلاق", color = Color.White)
                }
            }
        )
    }
}
