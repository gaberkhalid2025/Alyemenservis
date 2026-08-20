package com.example.ui.screens.admin

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.viewmodels.AdminViewModel

data class JobOfferItem(
    val id: String,
    val title: String,
    val company: String,
    val location: String,
    val salaryRange: String,
    val applicantsCount: Int = 0,
    val isActive: Boolean = true
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminJobsPanel(
    onBack: () -> Unit = {},
    adminViewModel: AdminViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var company by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("صنعاء") }
    var salary by remember { mutableStateOf("") }

    var jobsList by remember {
        mutableStateOf(
            listOf(
                JobOfferItem("JOB-1", "فني تمديدات كهربائية وطاقة", "شركة السعيد للطاقة", "صنعاء - حدة", "150,000 - 250,000 ر.ي", 12),
                JobOfferItem("JOB-2", "صيدلي / مسوق أدوية", "مجموعة الشفاء الدوائية", "عدن - المنصورة", "200,000 - 300,000 ر.ي", 8),
                JobOfferItem("JOB-3", "معلم تبريد وتكييف مركزي", "ورشة الخليج الهندسية", "تعز - شارع جمال", "180,000 - 280,000 ر.ي", 5)
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("إدارة الوظائف وفرص العمل", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "إضافة وظيفة", tint = Color(0xFF00668B))
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier.fillMaxSize().padding(paddingValues).background(Color(0xFFF8FAFC)),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(jobsList, key = { it.id }) { job ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(job.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("${job.company} • ${job.location}", fontSize = 12.sp, color = Color.Gray)
                            }
                            Switch(
                                checked = job.isActive,
                                onCheckedChange = { active ->
                                    jobsList = jobsList.map { if (it.id == job.id) it.copy(isActive = active) else it }
                                    Toast.makeText(context, if (active) "تم تفعيل الوظيفة" else "تم تعطيل الوظيفة", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("الراتب المتوقع: ${job.salaryRange} | المتقدمين: ${job.applicantsCount}", fontSize = 12.sp, color = Color(0xFF00668B))
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("نشر فرصة عمل جديدة") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("المسمى الوظيفي") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = company, onValueChange = { company = it }, label = { Text("الجهة / الشركة") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("الموقع والمدينة") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = salary, onValueChange = { salary = it }, label = { Text("الراتب التقديري") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (title.isNotBlank() && company.isNotBlank()) {
                        jobsList = listOf(JobOfferItem("JOB-${System.currentTimeMillis() % 1000}", title, company, location, salary.ifEmpty { "حسب الاتفاق" })) + jobsList
                        showAddDialog = false
                        title = ""; company = ""; salary = ""
                        Toast.makeText(context, "تم نشر الوظيفة بنجاح", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("نشر") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("إلغاء") } }
        )
    }
}
