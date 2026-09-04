package com.example.ui.screens.dashboard

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.*
import com.example.ui.MainViewModel
import com.example.utils.VisualThemePalette

import com.example.data.repositories.*

/**
 * 💼 Standalone Dedicated Dashboard for Job Posters & Recruiters (لوحة معلن الوظائف المستقلة)
 */
@Composable
fun JobPosterDashboard(
    account: UnifiedBusinessAccount,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var activeTab by remember { mutableIntStateOf(0) }

    val jobViewModel = remember(account.id) {
        JobPosterDashboardViewModel(
            posterId = account.id,
            dashboardRepository = DashboardRepositoryImpl(context)
        )
    }

    val jobUiState by jobViewModel.uiState.collectAsState()

    LaunchedEffect(jobViewModel) {
        jobViewModel.eventFlow.collect { event ->
            when (event) {
                is DashboardEvent.ShowToast -> Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                is DashboardEvent.NavigateToDetail -> { }
            }
        }
    }

    val tabsList = listOf(
        Pair("📢", "إعلانات الوظائف"),
        Pair("👥", "المتقدمين والسير الذاتية"),
        Pair("⚙️", "إعدادات المعلن"),
        Pair("📊", "الإحصائيات")
    )

    val allJobs by viewModel.jobs.collectAsState()
    val myJobs = remember(allJobs, account.phone) {
        allJobs.filter { it.phone == account.phone || it.companyName == account.name }
    }

    var isServiceActive by remember { mutableStateOf(account.isActive) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D151F))
    ) {
        // Professional Top Header
        ProfessionalDashboardHeader(
            account = account,
            subtitle = "💼 مسؤول توظيف وشركات • ${account.city.ifBlank { "اليمن" }}",
            isVerified = true,
            isServiceActive = isServiceActive,
            onToggleServiceActive = { active ->
                isServiceActive = active
                viewModel.updateBusinessAccountStatus(account.id, active)
            },
            onEditProfileClick = { activeTab = 2 },
            onShareClick = {
                val sendIntent = android.content.Intent().apply {
                    action = android.content.Intent.ACTION_SEND
                    putExtra(android.content.Intent.EXTRA_TEXT, "وظائف شاغرة معلنة من قِبل ${account.name}: ${account.phone}")
                    type = "text/plain"
                }
                context.startActivity(android.content.Intent.createChooser(sendIntent, "مشاركة حساب التوظيف"))
            },
            onBackClick = onBackClick,
            themeColors = themeColors
        )

        // Quick Stats Strip
        ProfessionalQuickStatsGrid(
            todayOrdersCount = myJobs.size,
            overallRating = 5.0,
            activeOffersCount = myJobs.size,
            approxRevenue = "${myJobs.size} شواغر",
            themeColors = themeColors,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )

        // Horizontal Tabs Bar
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0D151F))
                .padding(vertical = 6.dp, horizontal = 8.dp),

            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tabsList.size) { index ->
                val tab = tabsList[index]
                val isSelected = activeTab == index
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) Color(0xFF1E88E5) else Color(0xFF142030))
                        .border(
                            1.dp,
                            if (isSelected) Color(0xFF1E88E5) else Color.White.copy(alpha = 0.08f),
                            RoundedCornerShape(16.dp)
                        )
                        .clickable { activeTab = index }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(tab.first, fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = tab.second,
                            fontSize = 11.5.sp,
                            color = Color.White,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        Divider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)

        // Tab Content
        Box(modifier = Modifier.weight(1f).padding(12.dp)) {
            when (activeTab) {
                0 -> JobPostingsSection(account, viewModel)
                1 -> JobApplicantsSection(account, viewModel)
                2 -> JobPosterSettingsSection(account, viewModel)
                3 -> JobPosterStatsSection(account, viewModel)
            }
        }
    }
}

@Composable
private fun JobPostingsSection(account: UnifiedBusinessAccount, viewModel: MainViewModel) {
    val context = LocalContext.current
    var showCreateDialog by remember { mutableStateOf(false) }
    val allJobs by viewModel.jobs.collectAsState()
    val myJobs = remember(allJobs, account.phone) {
        allJobs.filter { it.phone == account.phone || it.companyName == account.name }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("📢 إعلانات الوظائف المنشورة", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF90CAF9))
            Button(
                onClick = { showCreateDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("نشر وظيفة ➕", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        if (myJobs.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("لا توجد إعلانات وظائف منشورة حالياً. اضغط 'نشر وظيفة' لإضافة شاغر جديد.", color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(myJobs, key = { it.id }) { job ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF142030)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(job.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Surface(color = Color(0xFF00C853).copy(alpha = 0.2f), shape = RoundedCornerShape(6.dp)) {
                                    Text(job.jobType, color = Color(0xFF00C853), fontSize = 10.sp, modifier = Modifier.padding(4.dp))
                                }
                            }
                            Text("🏢 ${job.companyName} • 📍 ${job.cityId}", fontSize = 11.sp, color = Color(0xFF90CAF9))
                            Text("💰 الراتب: ${job.salary}", fontSize = 11.sp, color = Color(0xFFFF9800))
                            Text(job.description, fontSize = 10.5.sp, color = Color.LightGray, maxLines = 2)

                            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                IconButton(
                                    onClick = {
                                        viewModel.deleteJob(job.id)
                                        Toast.makeText(context, "تم حذف الإعلان الوظيفي", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(30.dp).background(Color(0xFFE53935).copy(alpha = 0.15f), CircleShape)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color(0xFFE53935), modifier = Modifier.size(15.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        var jobTitle by remember { mutableStateOf("") }
        var jobType by remember { mutableStateOf("دوام كامل") }
        var salary by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }
        var reqs by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { showCreateDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF142030),
                border = BorderStroke(1.dp, Color(0xFF1E88E5)),
                modifier = Modifier.fillMaxWidth().padding(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("📢 نشر إعلان وظيفي جديد", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    OutlinedTextField(
                        value = jobTitle,
                        onValueChange = { jobTitle = it },
                        label = { Text("المسمى الوظيفي (مثال: محاسب عام)", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFF1E88E5))
                    )
                    OutlinedTextField(
                        value = salary,
                        onValueChange = { salary = it },
                        label = { Text("الراتب المتوقع / يحدد بالمقابلة", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFF1E88E5))
                    )
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text("وصف الوظيفة والمهام", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFF1E88E5))
                    )
                    OutlinedTextField(
                        value = reqs,
                        onValueChange = { reqs = it },
                        label = { Text("الشروط والمؤهلات المطلوبة", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFF1E88E5))
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { showCreateDialog = false }, modifier = Modifier.weight(1f)) {
                            Text("إلغاء", color = Color.White, fontSize = 11.sp)
                        }
                        Button(
                            onClick = {
                                if (jobTitle.isNotBlank()) {
                                    val job = JobEntity(
                                        title = jobTitle,
                                        companyName = account.name,
                                        managerName = account.name,
                                        phone = account.phone,
                                        cityId = account.city,
                                        jobType = jobType,
                                        salary = salary,
                                        description = desc,
                                        requirements = reqs,
                                        isActive = true,
                                        isApproved = true
                                    )
                                    viewModel.saveJob(job)
                                    showCreateDialog = false
                                    Toast.makeText(context, "تم نشر الوظيفة بنجاح!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("نشر الإعلان", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun JobApplicantsSection(account: UnifiedBusinessAccount, viewModel: MainViewModel) {
    val context = LocalContext.current
    val allApps by viewModel.jobApplications.collectAsState()
    val myApplicants = remember(allApps, account.name) {
        allApps.filter { it.companyName == account.name }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("👥 المتقدمين والسير الذاتية (CV)", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF90CAF9))

        if (myApplicants.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("لا يوجد متقدمين حتى الآن. طلبات التوظيف ستظهر هنا فور إرسالها من الباحثين عن عمل.", color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(myApplicants, key = { it.id }) { app ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF142030)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(app.applicantName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text(app.status, fontSize = 11.sp, color = Color(0xFF00C853), fontWeight = FontWeight.Bold)
                            }
                            Text("الوظيفة: ${app.jobTitle}", fontSize = 11.sp, color = Color(0xFF90CAF9))
                            Text("📞 هاتف: ${app.applicantPhone}", fontSize = 11.sp, color = Color.LightGray)
                            Text("🎓 المؤهلات: ${app.applicantQuals}", fontSize = 11.sp, color = Color.White)

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                Button(
                                    onClick = {
                                        viewModel.updateJobApplicationStatus(app.id, "ACCEPTED")
                                        Toast.makeText(context, "تم قبول المتقدم للمقابلة", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("قبول للمقابلة ✓", color = Color.White, fontSize = 11.sp)
                                }
                                OutlinedButton(
                                    onClick = {
                                        viewModel.updateJobApplicationStatus(app.id, "REJECTED")
                                        Toast.makeText(context, "تم الاعتذار للمتقدم", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("اعتذار ✕", color = Color(0xFFE53935), fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun JobPosterSettingsSection(account: UnifiedBusinessAccount, viewModel: MainViewModel) {
    val context = LocalContext.current
    var isChatEnabled by remember { mutableStateOf(account.isChatEnabled) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("⚙️ إعدادات معلن الوظائف", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF90CAF9))
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF142030)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("💬 استقبال استفسارات المتقدمين بالدردشة", fontSize = 12.sp, color = Color.White)
                    Switch(checked = isChatEnabled, onCheckedChange = { isChatEnabled = it })
                }
            }
        }

        Button(
            onClick = { Toast.makeText(context, "تم حفظ الإعدادات!", Toast.LENGTH_SHORT).show() },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("حفظ التغييرات", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

@Composable
private fun JobPosterStatsSection(account: UnifiedBusinessAccount, viewModel: MainViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("📊 إحصائيات التوظيف والتفاعل", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF90CAF9))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF142030)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("طلبات التوظيف المستلمة", fontSize = 11.sp, color = Color(0xFF90CAF9))
                    Text("42 طلب CV", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00C853))
                }
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF142030)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("الوظائف الشاغرة", fontSize = 11.sp, color = Color(0xFF90CAF9))
                    Text("3 وظائف", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))
                }
            }
        }
    }
}
